/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms;

import com.sun.jna.Platform;
import java.awt.*;
import java.io.*;
import java.net.BindException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.LogManager;
import javax.swing.*;
import net.pms.configuration.Build;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibrary;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.gui.DummyFrame;
import net.pms.gui.IFrame;
import net.pms.io.*;
import net.pms.logging.LoggingConfigFileLoader;
import net.pms.network.HTTPServer;
import net.pms.network.NetworkConfiguration;
import net.pms.network.ProxyServer;
import net.pms.network.UPNPHelper;
import net.pms.newgui.DbgPacker;
import net.pms.newgui.GeneralTab;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.ProfileChooser;
import net.pms.update.AutoUpdater;
import net.pms.util.ProcessUtil;
import net.pms.util.PropertiesUtil;
import net.pms.util.SystemErrWrapper;
import net.pms.util.TaskRunner;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMS {
	private static final String SCROLLBARS = "scrollbars";
	private static final String NATIVELOOK = "nativelook";
	private static final String CONSOLE = "console";
	private static final String NOCONSOLE = "noconsole";
	private static final String PROFILES = "profiles";

	/**
	 * @deprecated The version has moved to the resources/project.properties file. Use {@link #getVersion()} instead. 
	 */
	@Deprecated
	public static String VERSION;

	public static final String AVS_SEPARATOR = "\1";

	// (innot): The logger used for all logging.
	private static final Logger LOGGER = LoggerFactory.getLogger(PMS.class);

	// TODO(tcox):  This shouldn't be static
	private static PmsConfiguration configuration;

	/**Returns a pointer to the main PMS GUI.
	 * @return {@link net.pms.gui.IFrame} Main PMS window.
	 */
	public IFrame getFrame() {
		return frame;
	}

	/**getRootFolder returns the Root Folder for a given renderer. There could be the case
	 * where a given media renderer needs a different root structure.
	 * @param renderer {@link net.pms.configuration.RendererConfiguration} is the renderer for which to get the RootFolder structure. If <b>null</b>, then
	 * the default renderer is used.
	 * @return {@link net.pms.dlna.RootFolder} The root folder structure for a given renderer
	 */
	public RootFolder getRootFolder(RendererConfiguration renderer) {
		// something to do here for multiple directories views for each renderer
		if (renderer == null) {
			renderer = RendererConfiguration.getDefaultConf();
		}
		return renderer.getRootFolder();
	}

	/**
	 * Pointer to a running PMS server.
	 */
	private static PMS instance = null;

	/**
	 * @deprecated This field is not used and will be removed in the future. 
	 */
	@Deprecated
	public final static SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

	/**
	 * @deprecated This field is not used and will be removed in the future. 
	 */
	@Deprecated
	public final static SimpleDateFormat sdfHour = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	/**
	 * Array of {@link net.pms.configuration.RendererConfiguration} that have been found by PMS.
	 */
	private final ArrayList<RendererConfiguration> foundRenderers = new ArrayList<RendererConfiguration>();

	/**Adds a {@link net.pms.configuration.RendererConfiguration} to the list of media renderers found. The list is being used, for
	 * example, to give the user a graphical representation of the found media renderers.
	 * @param mediarenderer {@link net.pms.configuration.RendererConfiguration}
	 */
	public void setRendererfound(RendererConfiguration mediarenderer) {
		if (!foundRenderers.contains(mediarenderer) && !mediarenderer.isFDSSDP()) {
			foundRenderers.add(mediarenderer);
			frame.addRendererIcon(mediarenderer.getRank(), mediarenderer.getRendererName(), mediarenderer.getRendererIcon());
			frame.setStatusCode(0, Messages.getString("PMS.18"), "apply-220.png");
		}
	}

	/**
	 * HTTP server that serves the XML files needed by UPnP server and the media files.
	 */
	private HTTPServer server;

	/**
	 * User friendly name for the server.
	 */
	private String serverName;

	private ProxyServer proxyServer;

	public ProxyServer getProxy() {
		return proxyServer;
	}

	public ArrayList<Process> currentProcesses = new ArrayList<Process>();

	private PMS() {
	}

	/**
	 * {@link net.pms.gui.IFrame} object that represents PMS GUI.
	 */
	IFrame frame;

	/**
	 * @see com.sun.jna.Platform#isWindows()
	 */
	public boolean isWindows() {
		return Platform.isWindows();
	}

	private int proxy;

	/**Interface to Windows specific functions, like Windows Registry. registry is set by {@link #init()}.
	 * @see net.pms.io.WinUtils
	 */
	private SystemUtils registry;

	/**
	 * @see net.pms.io.WinUtils
	 */
	public SystemUtils getRegistry() {
		return registry;
	}

	/**Executes a new Process and creates a fork that waits for its results. 
	 * TODO:Extend explanation on where this is being used.
	 * @param name Symbolic name for the process to be launched, only used in the trace log
	 * @param error (boolean) Set to true if you want PMS to add error messages to the trace pane
	 * @param workDir (File) optional working directory to run the process in
	 * @param params (array of Strings) array containing the command to call and its arguments
	 * @return Returns true if the command exited as expected
	 * @throws Exception TODO: Check which exceptions to use
	 */
	private boolean checkProcessExistence(String name, boolean error, File workDir, String... params) throws Exception {
		LOGGER.debug("launching: " + params[0]);

		try {
			ProcessBuilder pb = new ProcessBuilder(params);
			if (workDir != null) {
				pb.directory(workDir);
			}
			final Process process = pb.start();

			OutputTextConsumer stderrConsumer = new OutputTextConsumer(process.getErrorStream(), false);
			stderrConsumer.start();

			OutputTextConsumer outConsumer = new OutputTextConsumer(process.getInputStream(), false);
			outConsumer.start();

			Runnable r = new Runnable() {
				public void run() {
					ProcessUtil.waitFor(process);
				}
			};

			Thread checkThread = new Thread(r, "PMS Checker");
			checkThread.start();
			checkThread.join(60000);
			checkThread.interrupt();
			checkThread = null;

			// XXX no longer used
			if (params[0].equals("vlc") && stderrConsumer.getResults().get(0).startsWith("VLC")) {
				return true;
			}

			// XXX no longer used
			if (params[0].equals("ffmpeg") && stderrConsumer.getResults().get(0).startsWith("FF")) {
				return true;
			}

			int exit = process.exitValue();
			if (exit != 0) {
				if (error) {
					LOGGER.info("[" + exit + "] Cannot launch " + name + " / Check the presence of " + params[0] + " ...");
				}
				return false;
			}
			return true;
		} catch (Exception e) {
			if (error) {
				LOGGER.error("Cannot launch " + name + " / Check the presence of " + params[0] + " ...", e);
			}
			return false;
		}
	}

	/**
	 * @see System#err
	 */
	@SuppressWarnings("unused")
	private final PrintStream stderr = System.err;

	/**Main resource database that supports search capabilities. Also known as media cache.
	 * @see net.pms.dlna.DLNAMediaDatabase
	 */
	private DLNAMediaDatabase database;

	private void initializeDatabase() {
		database = new DLNAMediaDatabase("medias"); // TODO: rename "medias" -> "cache"
		database.init(false);
	}

	/**Used to get the database. Needed in the case of the Xbox 360, that requires a database.
	 * for its queries.
	 * @return (DLNAMediaDatabase) a reference to the database instance or <b>null</b> if one isn't defined
	 * (e.g. if the cache is disabled).
	 */
	public synchronized DLNAMediaDatabase getDatabase() {
		if (configuration.getUseCache()) {
			if (database == null) {
				initializeDatabase();
			}
			return database;
		}
		return null;
	}

	/**Initialisation procedure for PMS.
	 * @return true if the server has been initialized correctly. false if the server could
	 * not be set to listen on the UPnP port.
	 * @throws Exception
	 */
	private boolean init() throws Exception {
		AutoUpdater autoUpdater = null;

		// Temporary fix for backwards compatibility
		VERSION = getVersion();

		if (Build.isUpdatable()) {
			String serverURL = Build.getUpdateServerURL();
			autoUpdater = new AutoUpdater(serverURL, getVersion());
		}

		registry = createSystemUtils();

		if (System.getProperty(CONSOLE) == null) {
			frame = new LooksFrame(autoUpdater, configuration);
		} else {
			LOGGER.info("GUI environment not available");
			LOGGER.info("Switching to console mode");
			frame = new DummyFrame();
		}
		configuration.addConfigurationListener(new ConfigurationListener() {
			@Override
			public void configurationChanged(ConfigurationEvent event) {
				if ((!event.isBeforeUpdate())
						&& PmsConfiguration.NEED_RELOAD_FLAGS.contains(event.getPropertyName())) {
					frame.setReloadable(true);
				}
			}
		});

		frame.setStatusCode(0, Messages.getString("PMS.130"), "connect_no-220.png");
		proxy = -1;

		LOGGER.info("Starting " + PropertiesUtil.getProjectProperties().get("project.name") + " " + getVersion());
		LOGGER.info("Based on PS3 Media Server (ps3mediaserver.org) by shagrath, copyright 2008-2012");
		LOGGER.info("http://www.universalmediaserver.com");
		LOGGER.info("");

		String commitId = PropertiesUtil.getProjectProperties().get("git.commit.id");
		String commitTime = PropertiesUtil.getProjectProperties().get("git.commit.time");
		String shortCommitId = commitId.substring(0,  9);

		LOGGER.info("Build: " + shortCommitId + " (" + commitTime + ")");

		// Log system properties
		logSystemInfo();

		String cwd = new File("").getAbsolutePath();
		LOGGER.info("Working directory: " + cwd);

		LOGGER.info("Temp folder: " + configuration.getTempFolder());
		LOGGER.info("Logging config file: " + LoggingConfigFileLoader.getConfigFilePath());

		HashMap<String, String> lfps = LoggingConfigFileLoader.getLogFilePaths();

		if (lfps != null && lfps.size() > 0) {
			if (lfps.size() == 1) {
				Entry<String, String> entry = lfps.entrySet().iterator().next();
				LOGGER.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
			} else {
				LOGGER.info("Logging to multiple files:");
				Iterator<Entry<String, String>> logsIterator = lfps.entrySet().iterator();
				Entry<String, String> entry;
				while (logsIterator.hasNext()) {
					entry = logsIterator.next();
					LOGGER.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
				}
			}
		}

		LOGGER.info("");

		LOGGER.info("Profile directory: " + configuration.getProfileDirectory());
		String profilePath = configuration.getProfilePath();
		LOGGER.info("Profile path: " + profilePath);

		File profileFile = new File(profilePath);

		if (profileFile.exists()) {
			String status = String.format("%s%s",
				profileFile.canRead()  ? "r" : "-",
				profileFile.canWrite() ? "w" : "-"
			);
			LOGGER.info("Profile status: " + status);
		} else {
			LOGGER.info("Profile status: no such file");
		}

		LOGGER.info("Profile name: " + configuration.getProfileName());
		LOGGER.info("");

		dbgPack = new DbgPacker();

		RendererConfiguration.loadRendererConfigurations(configuration);

		LOGGER.info("Checking MPlayer font cache. It can take a minute or so.");
		checkProcessExistence("MPlayer", true, null, configuration.getMplayerPath(), "dummy");
		if (isWindows()) {
			checkProcessExistence("MPlayer", true, configuration.getTempFolder(), configuration.getMplayerPath(), "dummy");
		}
		LOGGER.info("Done!");

		// check the existence of Vsfilter.dll
		if (registry.isAvis() && registry.getAvsPluginsDir() != null) {
			LOGGER.info("Found AviSynth plugins dir: " + registry.getAvsPluginsDir().getAbsolutePath());
			File vsFilterdll = new File(registry.getAvsPluginsDir(), "VSFilter.dll");
			if (!vsFilterdll.exists()) {
				LOGGER.info("VSFilter.dll is not in the AviSynth plugins directory. This can cause problems when trying to play subtitled videos with AviSynth");
			}
		}

		if (registry.getVlcv() != null && registry.getVlcp() != null) {
			LOGGER.info("Found VideoLAN version " + registry.getVlcv() + " at: " + registry.getVlcp());
		}

		//check if Kerio is installed
		if (registry.isKerioFirewall()) {
			LOGGER.info("Detected Kerio firewall");
		}

		// force use of specific dvr ms muxer when it's installed in the right place
		File dvrsMsffmpegmuxer = new File("win32/dvrms/ffmpeg_MPGMUX.exe");
		if (dvrsMsffmpegmuxer.exists()) {
			configuration.setFfmpegAlternativePath(dvrsMsffmpegmuxer.getAbsolutePath());
		}

		// disable jaudiotagger logging
		LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("org.jaudiotagger.level=OFF".getBytes()));

		// wrap System.err
		System.setErr(new PrintStream(new SystemErrWrapper(), true));

		server = new HTTPServer(configuration.getServerPort());

		/*
		 * XXX: keep this here (i.e. after registerExtensions and before registerPlayers) so that plugins
		 * can register custom players correctly (e.g. in the GUI) and/or add/replace custom formats
		 *
		 * XXX: if a plugin requires initialization/notification even earlier than
		 * this, then a new external listener implementing a new callback should be added
		 * e.g. StartupListener.registeredExtensions()
		 */
		try {
			ExternalFactory.lookup();
		} catch (Exception e) {
			LOGGER.error("Error loading plugins", e);
		}

		// Initialize a player factory to register all players
		PlayerFactory.initialize(configuration);

		// Instantiate listeners that require registered players.
		ExternalFactory.instantiateLateListeners();
		
		// a static block in Player doesn't work (i.e. is called too late).
		// this must always be called *after* the plugins have loaded.
		// here's as good a place as any
		Player.initializeFinalizeTranscoderArgsListeners();

		// Any plugin-defined players are now registered, create the gui view.
		frame.addEngines();

		boolean binding = false;

		try {
			binding = server.start();
		} catch (BindException b) {
			LOGGER.info("FATAL ERROR: Unable to bind on port: " + configuration.getServerPort() + ", because: " + b.getMessage());
			LOGGER.info("Maybe another process is running or the hostname is wrong.");
		}

		new Thread("Connection Checker") {
			@Override
			public void run() {
				try {
					Thread.sleep(7000);
				} catch (InterruptedException e) {
				}
				if (foundRenderers.isEmpty()) {
					frame.setStatusCode(0, Messages.getString("PMS.0"), "messagebox_critical-220.png");
				} else {
					frame.setStatusCode(0, Messages.getString("PMS.18"), "apply-220.png");
				}
			}
		}.start();

		if (!binding) {
			return false;
		}

		if (proxy > 0) {
			LOGGER.info("Starting HTTP Proxy Server on port: " + proxy);
			proxyServer = new ProxyServer(proxy);
		}

		// initialize the cache
		if (configuration.getUseCache()) {
			initializeDatabase(); // XXX: this must be done *before* new MediaLibrary -> new MediaLibraryFolder
			mediaLibrary = new MediaLibrary();
			LOGGER.info("A tiny cache admin interface is available at: http://" + server.getHost() + ":" + server.getPort() + "/console/home");
		}

		// XXX: this must be called:
		//     a) *after* loading plugins i.e. plugins register root folders then RootFolder.discoverChildren adds them
		//     b) *after* mediaLibrary is initialized, if enabled (above)
		getRootFolder(RendererConfiguration.getDefaultConf());

		frame.serverReady();

		//UPNPHelper.sendByeBye();
		Runtime.getRuntime().addShutdownHook(new Thread("PMS Listeners Stopper") {
			@Override
			public void run() {
				try {
					for (ExternalListener l : ExternalFactory.getExternalListeners()) {
						l.shutdown();
					}
					UPNPHelper.shutDownListener();
					UPNPHelper.sendByeBye();
					LOGGER.debug("Forcing shutdown of all active processes");
					for (Process p : currentProcesses) {
						try {
							p.exitValue();
						} catch (IllegalThreadStateException ise) {
							LOGGER.trace("Forcing shutdown of process: " + p);
							ProcessUtil.destroy(p);
						}
					}
					get().getServer().stop();
					Thread.sleep(500);
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				} catch (InterruptedException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		});

		UPNPHelper.sendAlive();
		LOGGER.trace("Waiting 250 milliseconds...");
		Thread.sleep(250);
		UPNPHelper.listen();

		return true;
	}
	
	private MediaLibrary mediaLibrary;

	/**Returns the MediaLibrary used by PMS.
	 * @return (MediaLibrary) Used mediaLibrary, if any. null if none is in use.
	 */
	public MediaLibrary getLibrary() {
		return mediaLibrary;
	}

	private SystemUtils createSystemUtils() {
		if (Platform.isWindows()) {
			return new WinUtils();
		} else {
			if (Platform.isMac()) {
				return new MacSystemUtils();
			} else {
				if (Platform.isSolaris()) {
					return new SolarisUtils();
				} else {
					return new BasicSystemUtils();
				}
			}
		}
	}

	/**Executes the needed commands in order to make PMS a Windows service that starts whenever the machine is started.
	 * This function is called from the Network tab.
	 * @return true if PMS could be installed as a Windows service.
	 * @see net.pms.newgui.GeneralTab#build()
	 */
	public boolean installWin32Service() {
		LOGGER.info(Messages.getString("PMS.41"));
		String cmdArray[] = new String[]{"win32/service/wrapper.exe", "-r", "wrapper.conf"};
		OutputParams output = new OutputParams(configuration);
		output.noexitcheck = true;
		ProcessWrapperImpl pwuninstall = new ProcessWrapperImpl(cmdArray, output);
		pwuninstall.runInSameThread();
		cmdArray = new String[]{"win32/service/wrapper.exe", "-i", "wrapper.conf"};
		ProcessWrapperImpl pwinstall = new ProcessWrapperImpl(cmdArray, new OutputParams(configuration));
		pwinstall.runInSameThread();
		return pwinstall.isSuccess();
	}

	/**Transforms a comma separated list of directory entries into an array of {@link String}.
	 * Checks that the directory exists and is a valid directory.
	 * @param log whether to output log information
	 * @return {@link java.io.File}[] Array of directories.
	 * @throws java.io.IOException
	 */

	// this is called *way* too often (e.g. a dozen times with 1 renderer and 1 shared folder),
	// so log it by default so we can fix it.
	// BUT it's also called when the GUI is initialized (to populate the list of shared folders),
	// and we don't want this message to appear *before* the PMS banner, so allow that call to suppress logging	
	public File[] getFoldersConf(boolean log) {
		String folders = getConfiguration().getFolders();
		if (folders == null || folders.length() == 0) {
			return null;
		}
		ArrayList<File> directories = new ArrayList<File>();
		String[] foldersArray = folders.split(",");
		for (String folder : foldersArray) {
			// unescape embedded commas. note: backslashing isn't safe as it conflicts with
			// Windows path separators:
			// http://ps3mediaserver.org/forum/viewtopic.php?f=14&t=8883&start=250#p43520
			folder = folder.replaceAll("&comma;", ",");
			if (log) {
				LOGGER.info("Checking shared folder: " + folder);
			}
			File file = new File(folder);
			if (file.exists()) {
				if (!file.isDirectory()) {
					LOGGER.warn("The file " + folder + " is not a directory! Please remove it from your Shared folders list on the Navigation/Share Settings tab");
				}
			} else {
				LOGGER.warn("The directory " + folder + " does not exist. Please remove it from your Shared folders list on the Navigation/Share Settings tab");
			}

			// add the file even if there are problems so that the user can update the shared folders as required.
			directories.add(file);
		}
		File f[] = new File[directories.size()];
		directories.toArray(f);
		return f;
	}

	public File[] getFoldersConf() {
		return getFoldersConf(true);
	}

	/**Restarts the server. The trigger is either a button on the main PMS window or via
	 * an action item.
	 * @throws java.io.IOException
	 */
	// XXX: don't try to optimize this by reusing the same server instance.
	// see the comment above HTTPServer.stop()
	public void reset() {
		TaskRunner.getInstance().submitNamed("restart", true, new Runnable() {
			public void run() {
				try {
					LOGGER.trace("Waiting 1 second...");
					UPNPHelper.sendByeBye();
					server.stop();
					server = null;
					RendererConfiguration.resetAllRenderers();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						LOGGER.trace("Caught exception", e);
					}
					server = new HTTPServer(configuration.getServerPort());
					server.start();
					UPNPHelper.sendAlive();
					frame.setReloadable(false);
				} catch (IOException e) {
					LOGGER.error("error during restart :" +e.getMessage(), e);
				}
			}
		});
	}

	// Cannot remove these methods because of backwards compatibility;
	// none of the PMS code uses it, but some plugins still do.
	
	/**
	 * @deprecated Use the SLF4J logging API instead.
	 * Adds a message to the debug stream, or {@link System#out} in case the
	 * debug stream has not been set up yet.
	 * @param msg {@link String} to be added to the debug stream.
	 */
	@Deprecated
	public static void debug(String msg) {
		LOGGER.trace(msg);
	}

	/**
	 * @deprecated Use the SLF4J logging API instead.
	 * Adds a message to the info stream.
	 * @param msg {@link String} to be added to the info stream.
	 */
	@Deprecated
	public static void info(String msg) {
		LOGGER.debug(msg);
	}

	/**
	 * @deprecated Use the SLF4J logging API instead.
	 * Adds a message to the minimal stream. This stream is also
	 * shown in the Trace tab.
	 * @param msg {@link String} to be added to the minimal stream.
	 */
	@Deprecated
	public static void minimal(String msg) {
		LOGGER.info(msg);
	}

	/**
	 * @deprecated Use the SLF4J logging API instead.
	 * Adds a message to the error stream. This is usually called by
	 * statements that are in a try/catch block.
	 * @param msg {@link String} to be added to the error stream
	 * @param t {@link Throwable} comes from an {@link Exception} 
	 */
	@Deprecated
	public static void error(String msg, Throwable t) {
		LOGGER.error(msg, t);
	}

	/*
	 * Universally Unique Identifier used in the UPnP server.
	 * 
	 */
	private String uuid;

	/*
	 * Creates a new {@link #uuid} for the UPnP server to use. Tries to follow the RFCs for creating the UUID based on the link MAC address.
	 * Defaults to a random one if that method is not available.
	 * @return {@link String} with an Universally Unique Identifier.
	 */
	public String usn() {
		if (uuid == null) {
			// Retrieve UUID from configuration
			uuid = getConfiguration().getUuid();

			if (uuid == null) {
				// Create a new UUID based on the MAC address of the used network adapter
				NetworkInterface ni = null;
				try {
					ni = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();
					// If no ni comes from the server host name, we should get the default.
					if (ni != null) {
						ni = get().getServer().getNi();
					}

					if (ni != null) {
						byte[] addr = getRegistry().getHardwareAddress(ni); // return null when java.net.preferIPv4Stack=true
						if (addr != null) {
							uuid = UUID.nameUUIDFromBytes(addr).toString();
							LOGGER.info(String.format("Generated new UUID based on the MAC address of the network adapter '%s'", ni.getDisplayName()));
						}
					}
				} catch (SocketException e) {
					LOGGER.debug("Caught exception", e);
				} catch (UnknownHostException e) {
					LOGGER.debug("Caught exception", e);
				}

				// Create random UUID if the generation by MAC address failed
				if (uuid == null) {
					uuid = UUID.randomUUID().toString();
					LOGGER.info("Generated new random UUID");
				}

				// Save the newly generated UUID
				getConfiguration().setUuid(uuid);
				try {
					getConfiguration().save();
				} catch (ConfigurationException e) {
					LOGGER.error("Failed to save configuration with new UUID", e);
				}
			}

			LOGGER.info("Using the following UUID configured in UMS.conf: " + uuid);
		}
		return "uuid:" + uuid;
	}

	/*
	 * Returns the user friendly name of the UPnP server. 
	 * @return {@link String} with the user friendly name.
	 */
	public String getServerName() {
		if (serverName == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.getProperty("os.name").replace(" ", "_"));
			sb.append("-");
			sb.append(System.getProperty("os.arch").replace(" ", "_"));
			sb.append("-");
			sb.append(System.getProperty("os.version").replace(" ", "_"));
			sb.append(", UPnP/1.0, UMS/").append(getVersion());
			serverName = sb.toString();
		}
		return serverName;
	}

	/*
	 * Returns the PMS instance.
	 * @return {@link net.pms.PMS}
	 */
	public static PMS get() {
		// XXX when PMS is run as an application, the instance is initialized via the createInstance call in main().
		// However, plugin tests may need access to a PMS instance without going
		// to the trouble of launching the PMS application, so we provide a fallback
		// initialization here. Either way, createInstance() should only be called once (see below)
		if (instance == null) {
			createInstance();
		}

		return instance;
	}

	private synchronized static void createInstance() {
		assert instance == null; // this should only be called once
		instance = new PMS();

		try {
			if (instance.init()) {
				LOGGER.info("The server should now appear on your renderer");
			} else {
				LOGGER.error("A serious error occurred during PMS init");
			}
		} catch (Exception e) {
			LOGGER.error("A serious error occurred during PMS init", e);
		}
	}

	/**
	 * @deprecated Use {@link net.pms.formats.FormatFactory#getAssociatedExtension(String)}
	 * instead.
	 *
	 * @param filename
	 * @return The format.
	 */
	@Deprecated
	public Format getAssociatedExtension(String filename) {
		return FormatFactory.getAssociatedExtension(filename);
	}

	public static void main(String args[]) throws IOException, ConfigurationException {
		boolean displayProfileChooser = false;
		boolean headless = true;

		if (args.length > 0) {
			for (int a = 0; a < args.length; a++) {
				if (args[a].equals(CONSOLE)) {
					System.setProperty(CONSOLE, Boolean.toString(true));
				} else if (args[a].equals(NATIVELOOK)) {
					System.setProperty(NATIVELOOK, Boolean.toString(true));
				} else if (args[a].equals(SCROLLBARS)) {
					System.setProperty(SCROLLBARS, Boolean.toString(true));
				} else if (args[a].equals(NOCONSOLE)) {
					System.setProperty(NOCONSOLE, Boolean.toString(true));
				} else if (args[a].equals(PROFILES)) {
					displayProfileChooser = true;
				}
			}
		}

		try {
			Toolkit.getDefaultToolkit();

			if (GraphicsEnvironment.isHeadless()) {
				if (System.getProperty(NOCONSOLE) == null) {
					System.setProperty(CONSOLE, Boolean.toString(true));
				}
			} else {
				headless = false;
			}
		} catch (Throwable t) {
			System.err.println("Toolkit error: " + t.getClass().getName() + ": " + t.getMessage());

			if (System.getProperty(NOCONSOLE) == null) {
				System.setProperty(CONSOLE, Boolean.toString(true));
			}
		}

		if (!headless && displayProfileChooser) {
			ProfileChooser.display();
		}

		try {
			setConfiguration(new PmsConfiguration());

			assert getConfiguration() != null;

			// Load the (optional) logback config file. This has to be called after 'new PmsConfiguration'
			// as the logging starts immediately and some filters need the PmsConfiguration.
			LoggingConfigFileLoader.load();

			killOld();
			// create the PMS instance returned by get()
			createInstance(); 
		} catch (Throwable t) {
			String errorMessage = String.format(
				"Configuration error: %s: %s",
				t.getClass().getName(),
				t.getMessage()
			);

			System.err.println(errorMessage);

			if (!headless && instance != null) {
				JOptionPane.showMessageDialog(
					((JFrame) (SwingUtilities.getWindowAncestor((Component) instance.getFrame()))),
					errorMessage,
					"Error initalizing UMS!",
					JOptionPane.ERROR_MESSAGE
				);
			}
		}
	}

	public HTTPServer getServer() {
		return server;
	}

	/**
	 * @deprecated Use {@link net.pms.formats.FormatFactory#getExtensions()} instead.
	 *
	 * @return The list of formats. 
	 */
	public ArrayList<Format> getExtensions() {
		return FormatFactory.getExtensions();
	}

	public void save() {
		try {
			configuration.save();
		} catch (ConfigurationException e) {
			LOGGER.error("Could not save configuration", e);
		}
	}

	public void storeFileInCache(File file, int formatType) {
		if (getConfiguration().getUseCache()
				&& !getDatabase().isDataExists(file.getAbsolutePath(), file.lastModified())) {

			getDatabase().insertData(file.getAbsolutePath(), file.lastModified(), formatType, null);
		}
	}

	/**
	 * Retrieves the {@link net.pms.configuration.PmsConfiguration PmsConfiguration} object
	 * that contains all configured settings for PMS. The object provides getters for all
	 * configurable PMS settings.
	 *
	 * @return The configuration object
	 */
	public static PmsConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Sets the {@link net.pms.configuration.PmsConfiguration PmsConfiguration} object
	 * that contains all configured settings for PMS. The object provides getters for all
	 * configurable PMS settings.
	 *
	 * @param conf The configuration object.
	 */
	public static void setConfiguration(PmsConfiguration conf) {
		configuration = conf;
	}

	/**
	 * Returns the project version for PMS.
	 *
	 * @return The project version.
	 */
	public static String getVersion() {
		return PropertiesUtil.getProjectProperties().get("project.version");
	}

	/**
	 * Returns whether the operating system is 64-bit or 32-bit.
	 *
	 * This will work with Windows and OS X but not necessarily with Linux
	 * because when the OS is not Windows we are using Java's os.arch which
	 * only detects the bitness of Java, not of the operating system.
	 *
	 * @return The bitness of the operating system.
	 */
	public static int getOSBitness() {
		int bitness = 32;

		if (
			(System.getProperty("os.name").contains("Windows") && System.getenv("ProgramFiles(x86)") != null) ||
			System.getProperty("os.arch").indexOf("64") != -1
		) {
			bitness = 64;
		}

		return bitness;
	}

	/**
	 * Log system properties identifying Java, the OS and encoding and log
	 * warnings where appropriate.
	 */
	private void logSystemInfo() {
		long memoryInMB = Runtime.getRuntime().maxMemory() / 1048576;

		LOGGER.info("Java: " + System.getProperty("java.version") + "-" + System.getProperty("java.vendor"));
		LOGGER.info("OS: " + System.getProperty("os.name") + " " + getOSBitness() + "-bit " + System.getProperty("os.version"));
		LOGGER.info("Encoding: " + System.getProperty("file.encoding"));
		LOGGER.info("Memory: " + memoryInMB + " " + Messages.getString("StatusTab.12"));
		LOGGER.info("");

		if (Platform.isMac()) {
			// The binaries shipped with the Mac OS X version of PMS are being
			// compiled against specific OS versions, making them incompatible
			// with older versions. Warn the user about this when necessary.
			String osVersion = System.getProperty("os.version");

			// Split takes a regular expression, so escape the dot.
			String[] versionNumbers = osVersion.split("\\.");

			if (versionNumbers.length > 1) {
				try {
					int osVersionMinor = Integer.parseInt(versionNumbers[1]);

					if (osVersionMinor < 6) {
						LOGGER.warn("-----------------------------------------------------------------");
						LOGGER.warn("WARNING!");
						LOGGER.warn("PMS ships with binaries compiled for Mac OS X 10.6 or higher.");
						LOGGER.warn("You are running an older version of Mac OS X so PMS may not work!");
						LOGGER.warn("More information in the FAQ:");
						LOGGER.warn("http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=66371#p66371");
						LOGGER.warn("-----------------------------------------------------------------");
						LOGGER.warn("");
					}
				} catch (NumberFormatException e) {
					LOGGER.debug("Cannot parse minor os.version number");
				}
			}
		}
	}

	/*
	 * Restart handling
	 */

	private static void killOld() {
		try {
			killProc();
		} catch (IOException e) {
			LOGGER.debug("error killing old proc " + e);
		}
		try {
			dumpPid();
		} catch (IOException e) {
			LOGGER.debug("error dumping pid " + e);
		}
	}

	private static boolean verifyPidName(String pid) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("tasklist","/FI","\"PID eq " + pid + "\"", "/V", "/NH", "/FO", "CSV");
		pb.redirectErrorStream(true);
		Process p = pb.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			in.close();
			return false;
		}
		String line = in.readLine();
		in.close();
		if (line == null) {
			return false;
		}
		// remove all " and convert to common case before splitting result on ,
		String[] tmp = line.toLowerCase().replaceAll("\"", "").split(",");
		// if the line is too short we don't kill the process
		if (tmp.length < 9) {
			return false;
		}
		return tmp[0].equals("javaw.exe") && tmp[8].contains("universal media server");
	}

	private static void killProc() throws IOException {
		ProcessBuilder pb = null;
		BufferedReader in = new BufferedReader(new FileReader("pms.pid"));
		String pid = in.readLine();
		in.close();
		if (Platform.isWindows()) {
			if (verifyPidName(pid)) {
				pb = new ProcessBuilder("taskkill","/F","/PID",pid,"/T");
			}
		} else if (Platform.isFreeBSD() || Platform.isLinux() || Platform.isOpenBSD() || Platform.isSolaris()) {
			pb=new ProcessBuilder("kill","-9",pid);
		}
		if (pb == null) {
			return;
		}
		try {			
			Process p = pb.start();
			p.waitFor();
		} catch (Exception e) {
			LOGGER.debug("error kill pid " + e);
		}
	}

	public static long getPID() {
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		return Long.parseLong(processName.split("@")[0]);
	}

	private static void dumpPid() throws IOException {
		FileOutputStream out = new FileOutputStream("pms.pid");
		long pid = getPID();
		LOGGER.debug("My PID is " + pid);
		String data = String.valueOf(pid) + "\r\n";
		out.write(data.getBytes());
		out.flush();
		out.close();
	}

	private DbgPacker dbgPack;

	public DbgPacker dbgPack() {
		return dbgPack;
	}
	
	@Deprecated
	public void registerPlayer(Player player) {
		PlayerFactory.registerPlayer(player);
	} 
}
