/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.sun.jna.Platform;
import java.io.*;
import java.net.BindException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import net.pms.configuration.Build;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.database.MediaDatabase;
import net.pms.database.UserDatabase;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DynamicPlaylist;
import net.pms.dlna.GlobalIdRepo;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibrary;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.YoutubeDl;
import net.pms.gui.EConnectionState;
import net.pms.gui.GuiManager;
import net.pms.io.*;
import net.pms.logging.CacheLogger;
import net.pms.logging.LoggingConfig;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.WebGuiServer;
import net.pms.network.webguiserver.servlets.SseApiServlet;
import net.pms.network.webplayerserver.WebPlayerServer;
import net.pms.newgui.DbgPacker;
import net.pms.newgui.GuiUtil;
import net.pms.newgui.LanguageSelection;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.ProfileChooser;
import net.pms.newgui.Splash;
import net.pms.newgui.Wizard;
import net.pms.newgui.components.WindowProperties.WindowPropertiesConfiguration;
import net.pms.platform.PlatformUtils;
import net.pms.platform.windows.WindowsNamedPipe;
import net.pms.platform.windows.WindowsUtils;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.RendererFilter;
import net.pms.service.LibraryScanner;
import net.pms.service.Services;
import net.pms.update.AutoUpdater;
import net.pms.util.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMS {
	private static final String SCROLLBARS = "scrollbars";
	private static final String NATIVELOOK_ARG = "nativelook";
	private static final String CONSOLE_ARG = "console";
	private static final String HEADLESS_ARG = "headless";
	private static final String NOCONSOLE_ARG = "noconsole";
	private static final String PROFILES = "profiles";
	private static final String PROFILE = "^(?i)profile(?::|=)([^\"*<>?]+)$";
	private static final String TRACE = "trace";
	private static final String DBLOG = "dblog";
	private static final String DBTRACE = "dbtrace";

	public static final String NAME = "Universal Media Server";
	public static final String CROWDIN_LINK = "https://crowdin.com/project/universalmediaserver";

	private boolean ready = false;

	private GlobalIdRepo globalRepo;

	public static final String AVS_SEPARATOR = "\1";

	// (innot): The logger used for all logging.
	private static final Logger LOGGER = LoggerFactory.getLogger(PMS.class);

	// TODO(tcox):  This shouldn't be static
	private static UmsConfiguration umsConfiguration;

	/**
	 * Universally Unique Identifier used in the UPnP mediaServer.
	 */
	private String uuid;

	/**
	 * Relative location of a context sensitive help page in the documentation
	 * directory.
	 */
	private static String helpPage = "index.html";

	/**
	 * A lock to prevent heavy IO tasks from causing browsing to be less
	 * responsive.
	 *
	 * When a task has a high priority (needs to run in realtime), it should
	 * implement this lock for the duration of their operation. When a task
	 * has a lower priority, it should use this lock to wait for any
	 * realtime task to finish, and then immediately unlock, to prevent
	 * blocking the next realtime task from starting.
	 */
	public static final Lock REALTIME_LOCK = new ReentrantLock();

	/**
	 * Returns the root folder for a given renderer. There could be the case
	 * where a given media renderer needs a different root structure.
	 *
	 * @param renderer {@link Renderer}
	 * is the renderer for which to get the RootFolder structure. If <code>null</code>,
	 * then the default renderer is used.
	 * @return {@link net.pms.dlna.RootFolder} The root folder structure for a given renderer
	 */
	public RootFolder getRootFolder(Renderer renderer) {
		// something to do here for multiple directories views for each renderer
		if (renderer == null) {
			renderer = RendererConfigurations.getDefaultRenderer();
		}

		if (renderer == null) {
			return null;
		}

		return renderer.getRootFolder();
	}

	/**
	 * Pointer to a running UMS server.
	 */
	private static PMS instance = null;

	/**
	 * An array of {@link Renderer}s that have been found by UMS.
	 * <p>
	 * Important! If iteration is done on this list it's not thread safe unless
	 * the iteration loop is enclosed by a {@code synchronized} block on the <b>
	 * {@link List} itself</b>.
	 */
	private final List<Renderer> foundRenderers = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The returned <code>List</code> itself is thread safe, but the objects
	 * it's holding is not. Any looping/iterating of this <code>List</code>
	 * MUST be enclosed in:
	 * S<pre><code>
	 * synchronized (getFoundRenderers()) {
	 *      ..code..
	 * }
	 * </code></pre>
	 * @return {@link #foundRenderers}
	 */
	public List<Renderer> getFoundRenderers() {
		return foundRenderers;
	}

	/**
	 * Adds a {@link Renderer} to the list of media renderers found.
	 * The list is being used, for example, to give the user a graphical representation of the found
	 * media renderers.
	 *
	 * @param renderer {@link Renderer}
	 * @since 1.82.0
	 */
	public void setRendererFound(Renderer renderer) {
		synchronized (foundRenderers) {
			if (!foundRenderers.contains(renderer) && !renderer.isFDSSDP()) {
				LOGGER.debug("Adding status button for {}", renderer.getRendererName());
				foundRenderers.add(renderer);
				GuiManager.addRenderer(renderer);
				GuiManager.setConnectionState(EConnectionState.CONNECTED);
			}
		}
	}

	/**
	 * UPnP mediaServer that serves the XML files, media files and broadcast messages needed by UPnP Service.
	 */
	private MediaServer mediaServer;

	/**
	 * HTTP server that serves a gui.
	 */
	private WebGuiServer webGuiServer;

	/**
	 * HTTP server that serves a browser/player of media files.
	 * Should replace the WebInterfaceServer at end.
	 */
	private WebPlayerServer webPlayerServer;

	/**
	 * User friendly name for the server.
	 */
	private String serverName;

	private PMS() {
	}

	/**
	 * Used to get the database. Needed in the case of the Xbox 360, that requires a database.
	 * for its queries.
	 * @return (MediaDatabase) a reference to the mediaDatabase.
	 */
	public MediaDatabase getMediaDatabase() {
		return MediaDatabase.get();
	}

	public UserDatabase getUserDatabase() {
		return UserDatabase.get();
	}

	private void displayBanner() throws IOException {
		LOGGER.debug("");
		LOGGER.info("Starting {} {}", PropertiesUtil.getProjectProperties().get("project.name"), getVersion());
		LOGGER.info("Based on PS3 Media Server by shagrath, copyright 2008-2014");
		LOGGER.info("https://www.universalmediaserver.com");
		LOGGER.info("");

		String commitId = PropertiesUtil.getProjectProperties().get("git.commit.id");
		LOGGER.info(
			"Build: {} ({})",
			commitId.substring(0, 9),
			PropertiesUtil.getProjectProperties().get("git.commit.time")
		);

		if (PlatformUtils.isMac() && !PlatformUtils.getOSVersion().isGreaterThanOrEqualTo("10.6.0")) {
			LOGGER.warn("-----------------------------------------------------------------");
			LOGGER.warn("WARNING!");
			LOGGER.warn("UMS ships with external binaries compiled for Mac OS X 10.6 or");
			LOGGER.warn("higher. You are running an older version of Mac OS X which means");
			LOGGER.warn("that these binaries used for example for transcoding may not work!");
			LOGGER.warn("To solve this, replace the binaries found in the \"osx\"");
			LOGGER.warn("subfolder with versions compiled for your version of OS X.");
			LOGGER.warn("-----------------------------------------------------------------");
			LOGGER.warn("");
		}

		String cwd = new File("").getAbsolutePath();
		LOGGER.info("Working directory: {}", cwd);

		LOGGER.info("Temporary directory: {}", umsConfiguration.getTempFolder());

		/**
		 * Verify the java.io.tmpdir is writable; JNA requires it.
		 * Note: the configured tempFolder has already been checked, but it
		 * may differ from the java.io.tmpdir so double check to be sure.
		 */
		File javaTmpdir = new File(System.getProperty("java.io.tmpdir"));

		if (!FileUtil.getFilePermissions(javaTmpdir).isWritable()) {
			LOGGER.error("The Java temp directory \"{}\" is not writable by UMS", javaTmpdir.getAbsolutePath());
			LOGGER.error("Please make sure the directory is writable for user \"{}\"", System.getProperty("user.name"));
			throw new IOException("Cannot write to Java temp directory: " + javaTmpdir.getAbsolutePath());
		}

		LOGGER.info("Logging configuration file: {}", LoggingConfig.getConfigFilePath());

		Map<String, String> lfps = LoggingConfig.getLogFilePaths();

		// Logfile name(s) and path(s)
		if (lfps != null && !lfps.isEmpty()) {
			if (lfps.size() == 1) {
				Entry<String, String> entry = lfps.entrySet().iterator().next();
				if (entry.getKey().equalsIgnoreCase("default.log")) {
					LOGGER.info("Logfile: {}", entry.getValue());
				} else {
					LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
				}
			} else {
				LOGGER.info("Logging to multiple files:");
				Iterator<Entry<String, String>> logsIterator = lfps.entrySet().iterator();
				Entry<String, String> entry;
				while (logsIterator.hasNext()) {
					entry = logsIterator.next();
					LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
				}
			}
		}

		String profilePath = umsConfiguration.getProfilePath();
		String profileDirectoryPath = umsConfiguration.getProfileDirectory();

		LOGGER.info("");
		LOGGER.info("Profile directory: {}", profileDirectoryPath);
		try {
			// Don't use the {} syntax here as the check needs to be performed on every log level
			LOGGER.info("Profile directory permissions: " + FileUtil.getFilePermissions(profileDirectoryPath));
		} catch (FileNotFoundException e) {
			LOGGER.warn("Profile directory not found: {}", e.getMessage());
		}
		LOGGER.info("Profile configuration file: {}", profilePath);
		try {
			// Don't use the {} syntax here as the check needs to be performed on every log level
			LOGGER.info("Profile configuration file permissions: " + FileUtil.getFilePermissions(profilePath));
		} catch (FileNotFoundException e) {
			LOGGER.warn("Profile configuration file not found: {}", e.getMessage());
		}
		LOGGER.info("Profile name: {}", umsConfiguration.getProfileName());
		LOGGER.info("");

		/**
		 * Ensure the data directory is created. On Windows this is
		 * usually done by the installer
		 */
		File dDir = new File(umsConfiguration.getDataDir());
		if (!dDir.exists() && !dDir.mkdirs()) {
			LOGGER.error("Failed to create profile folder \"{}\"", umsConfiguration.getDataDir());
		}

		dbgPack = new DbgPacker();
		tfm = new TempFileMgr();

		// Start this here to let the converison work
		tfm.schedule();

	}

	/**
	 * Initialization procedure.
	 *
	 * @return <code>true</code> if the UMS server has been initialized correctly.
         <code>false</code> if initialization was aborted.
	 * @throws Exception
	 */
	private boolean init() throws Exception {
		// Gather and log system information from a separate thread
		LogSystemInformationMode logSystemInfo = umsConfiguration.getLogSystemInformation();
		if (
			logSystemInfo == LogSystemInformationMode.ALWAYS ||
			logSystemInfo == LogSystemInformationMode.TRACE_ONLY &&
			LOGGER.isTraceEnabled()
		) {
			new SystemInformation().start();
		}

		// Show the language selection dialog before displayBanner();
		if (
			!isHeadless() &&
			!isRunningTests() &&
			(
				umsConfiguration.getLanguageRawString() == null ||
				!Languages.isValid(umsConfiguration.getLanguageRawString())
			)
		) {
			LanguageSelection languageDialog = new LanguageSelection(null, PMS.getLocale(), false);
			languageDialog.show();
			if (languageDialog.isAborted()) {
				return false;
			}
		}

		// Initialize splash screen
		WindowPropertiesConfiguration windowConfiguration = null;
		Splash splash = null;
		if (!isHeadless()) {
			windowConfiguration = new WindowPropertiesConfiguration(
				Paths.get(umsConfiguration.getProfileDirectory()).resolve("UMS.dat")
			);
			splash = new Splash(umsConfiguration, windowConfiguration.getGraphicsConfiguration());
		}

		// Call this as early as possible
		displayBanner();

		// Start network scanner
		NetworkConfiguration.start();
		// Initialize databases
		MediaDatabase.init();
		UserDatabase.init();
		NetworkDeviceFilter.reset();
		RendererFilter.reset();

		/**
		 * Bump the SystemUpdateID state variable because now we will have
		 * different resource IDs than last time UMS ran. It also populates our
		 * in-memory value with the database value if the database is enabled.
		 */
		DLNAResource.bumpSystemUpdateId();

		// Log registered ImageIO plugins
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("");
			LOGGER.trace("Registered ImageIO reader classes:");
			Iterator<ImageReaderSpi> readerIterator = IIORegistry.getDefaultInstance().getServiceProviders(ImageReaderSpi.class, true);
			while (readerIterator.hasNext()) {
				ImageReaderSpi reader = readerIterator.next();
				LOGGER.trace("Reader class: {}", reader.getPluginClassName());
			}
			LOGGER.trace("");
			LOGGER.trace("Registered ImageIO writer classes:");
			Iterator<ImageWriterSpi> writerIterator = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, true);
			while (writerIterator.hasNext()) {
				ImageWriterSpi writer = writerIterator.next();
				LOGGER.trace("Writer class: {}", writer.getPluginClassName());
			}
			LOGGER.trace("");
		}

		// Wizard
		if (umsConfiguration.isRunWizard() && !isHeadless() && !isRunningTests()) {
			// Hide splash screen
			if (splash != null) {
				splash.setVisible(false);
			}

			// Run wizard
			Wizard.run(umsConfiguration);

			// Unhide splash screen
			if (splash != null) {
				splash.setVisible(true);
			}
		}

		globalRepo = new GlobalIdRepo();
		LOGGER.trace("Initialized globalRepo");

		AutoUpdater autoUpdater = null;
		if (Build.isUpdatable()) {
			String serverURL = Build.getUpdateServerURL();
			autoUpdater = new AutoUpdater(serverURL, getVersion());
		}

		// Show info that video automatic setting was improved and was not set in the wizard.
		// This must be done before the frame is initialized to accept changes.
		if (!isHeadless() && !isRunningTests() && umsConfiguration.showInfoAboutVideoAutomaticSetting()) {
			if (!umsConfiguration.isAutomaticMaximumBitrate()) {
				// Ask if user wants to use automatic maximum bitrate
				boolean useAutomaticMaximumBitrate = GuiUtil.askYesNoMessage(Messages.getString("WeImprovedAutomaticVideoQuality"), Messages.getString("ImprovedFeature"), true);
				umsConfiguration.setAutomaticMaximumBitrate(useAutomaticMaximumBitrate);
			}

			// It will be shown only once
			umsConfiguration.setShowInfoAboutVideoAutomaticSetting(false);
		}

		// Actions that happen only the first time UMS runs
		if (!umsConfiguration.hasRunOnce()) {
			/*
			 * Enable youtube-dl once, to ensure that if it is
			 * disabled, that was done by the user.
			 */
			if (!EngineFactory.isEngineActive(YoutubeDl.ID)) {
				umsConfiguration.setEngineEnabled(YoutubeDl.ID, true);
				umsConfiguration.setEnginePriorityBelow(YoutubeDl.ID, FFmpegWebVideo.ID);
			}

			// Ensure this only happens once
			umsConfiguration.setHasRunOnce();
		}

		GuiManager.setScanLibraryStatus(umsConfiguration.getUseCache(), false);
		if (!isHeadless()) {
			GuiManager.addGui(new LooksFrame(autoUpdater, umsConfiguration, windowConfiguration));
		} else {
			LOGGER.info("Graphics environment not available or headless mode is forced");
			LOGGER.info("Switching to console mode");
		}

		// Close splash screen
		if (splash != null) {
			splash.dispose();
		}

		umsConfiguration.addConfigurationListener((ConfigurationEvent event) -> {
			if (!event.isBeforeUpdate()) {
				if (UmsConfiguration.NEED_MEDIA_SERVER_RELOAD_FLAGS.contains(event.getPropertyName())) {
					GuiManager.setReloadable(true);
				} else if (UmsConfiguration.NEED_RENDERERS_RELOAD_FLAGS.contains(event.getPropertyName())) {
					GuiManager.setReloadable(true);
				} else if (UmsConfiguration.NEED_WEB_GUI_SERVER_RELOAD_FLAGS.contains(event.getPropertyName())) {
					GuiManager.setReloadable(true);
				} else if (UmsConfiguration.NEED_WEB_PLAYER_SERVER_RELOAD_FLAGS.contains(event.getPropertyName())) {
					resetWebPlayerServer();
				} else if (UmsConfiguration.NEED_MEDIA_LIBRARY_RELOAD_FLAGS.contains(event.getPropertyName())) {
					resetMediaLibrary();
				} else if (UmsConfiguration.NEED_RENDERERS_ROOT_RELOAD_FLAGS.contains(event.getPropertyName())) {
					resetRenderersRoot();
				}
				GuiManager.setConfigurationChanged(event.getPropertyName());
			}
		});

		// GUI stuff
		resetWebGuiServer();
		// Web player stuff
		resetWebPlayerServer();

		// init Credentials
		credMgr = new CredMgr(umsConfiguration.getCredFile());

		// init dbs
		codes = new CodeDb();
		masterCode = null;

		RendererConfigurations.loadRendererConfigurations();

		// Initialize MPlayer and FFmpeg to let them generate fontconfig cache/s
		if (!umsConfiguration.isDisableSubtitles()) {
			LOGGER.info("Checking the fontconfig cache in the background, this can take two minutes or so.");

			//TODO: Rewrite fontconfig generation
			ThreadedProcessWrapper.runProcessNullOutput(5, TimeUnit.MINUTES, 2000, umsConfiguration.getMPlayerPath(), "dummy");

			/**
			 * Note: Different versions of fontconfig and bitness require
			 * different caches, which is why here we ask FFmpeg (64-bit
			 * if possible) to create a cache.
			 * This should result in all of the necessary caches being built.
			 */
			if ((!PlatformUtils.isWindows() || PlatformUtils.is64Bit()) && umsConfiguration.getFFmpegPath() != null) {
				ThreadedProcessWrapper.runProcessNullOutput(5,
					TimeUnit.MINUTES,
					2000,
					umsConfiguration.getFFmpegPath(),
					"-y",
					"-f",
					"lavfi",
					"-i",
					"nullsrc=s=720x480:d=1:r=1",
					"-vf",
					"ass=DummyInput.ass",
					"-target",
					"ntsc-dvd",
					"-"
				);
			}
		}

		// Check available GPU HW decoding acceleration methods used in FFmpeg
		UMSUtils.checkGPUDecodingAccelerationMethodsForFFmpeg(umsConfiguration);

		GuiManager.setConnectionState(EConnectionState.SEARCHING);

		// Check the existence of VSFilter / DirectVobSub
		if (PlatformUtils.INSTANCE.isAviSynthAvailable() && PlatformUtils.INSTANCE.getAvsPluginsDir() != null) {
			LOGGER.debug("AviSynth plugins directory: " + PlatformUtils.INSTANCE.getAvsPluginsDir().getAbsolutePath());
			File vsFilterDLL = new File(PlatformUtils.INSTANCE.getAvsPluginsDir(), "VSFilter.dll");
			if (vsFilterDLL.exists()) {
				LOGGER.debug("VSFilter / DirectVobSub was found in the AviSynth plugins directory.");
			} else {
				File vsFilterDLL2 = new File(PlatformUtils.INSTANCE.getKLiteFiltersDir(), "vsfilter.dll");
				if (vsFilterDLL2.exists()) {
					LOGGER.debug("VSFilter / DirectVobSub was found in the K-Lite Codec Pack filters directory.");
				} else {
					LOGGER.info("VSFilter / DirectVobSub was not found. This can cause problems when trying to play subtitled videos with AviSynth.");
				}
			}
		}

		// Check if Kerio is installed
		if (PlatformUtils.INSTANCE.isKerioFirewall()) {
			LOGGER.info("Detected Kerio firewall");
		}

		// Disable jaudiotagger logging
		LogManager.getLogManager().readConfiguration(
			new ByteArrayInputStream("org.jaudiotagger.level=OFF".getBytes(StandardCharsets.US_ASCII))
		);

		// Wrap System.err
		System.setErr(new PrintStream(new SystemErrWrapper(), true, StandardCharsets.UTF_8.name()));

		// Initialize a engine factory to register all transcoding engines
		EngineFactory.initialize();

		// Any plugin-defined engines are now registered, create the gui view.
		GuiManager.addEngines();

		// Now that renderer confs are all loaded, we can start searching for renderers
		MediaServer.start();

		new Thread("Connection Checker") {
			@Override
			public void run() {
				UMSUtils.sleep(7000);

				if (foundRenderers.isEmpty()) {
					GuiManager.setConnectionState(EConnectionState.DISCONNECTED);
				} else {
					GuiManager.setConnectionState(EConnectionState.CONNECTED);
				}
			}
		}.start();

		if (webPlayerServer != null && webPlayerServer.getServer() != null) {
			GuiManager.enableWebUiButton();
			LOGGER.info("Web player is available at: " + webPlayerServer.getUrl());
		}

		// initialize the cache
		mediaLibrary = new MediaLibrary();

		// XXX: this must be called *after* mediaLibrary is initialized, if enabled (above)
		getRootFolder(null);

		// Ensure up-to-date API metadata versions
		if (umsConfiguration.getExternalNetwork() && umsConfiguration.isUseInfoFromIMDb()) {
			APIUtils.setApiMetadataVersions();
			APIUtils.setApiImageBaseURL();
		}

		GuiManager.serverReady();
		ready = true;
		if (!isHeadless() && umsConfiguration.isWebGuiOnStart() && !isRunningTests()) {
			new Thread("Web GUI browser") {
				@Override
				public void run() {
					while (!UserDatabase.isAvailable()) {
						UMSUtils.sleep(100);
					}
					LOGGER.info("Launching the graphical interface on a browser");
					if (!PlatformUtils.INSTANCE.browseURI(webGuiServer.getUrl())) {
						LOGGER.info(Messages.getString("ErrorOccurredTryingLaunchBrowser"));
					}
				}
			}.start();
		}
		Runtime.getRuntime().addShutdownHook(new Thread("UMS Shutdown") {
			@Override
			public void run() {
				shutdown();
			}
		});

		umsConfiguration.setAutoSave();

		// Initiate a library scan in case files were added to folders while UMS was closed.
		if (umsConfiguration.getUseCache() && umsConfiguration.isScanSharedFoldersOnStartup()) {
			LibraryScanner.scanLibrary();
		}

		return true;
	}

	private MediaLibrary mediaLibrary;

	/**
	 * Returns the MediaLibrary.
	 *
	 * @return The current {@link MediaLibrary}.
	 */
	public MediaLibrary getLibrary() {
		return mediaLibrary;
	}

	/**
	 * Restarts the server. The trigger is either a button on the main UMS
	 * window or via an action item.
	 */
	// XXX: don't try to optimize this by reusing the same HttpMediaServer instance.
	// see the comment above HttpMediaServer.stop()
	public void resetMediaServer() {
		TaskRunner.getInstance().submitNamed("restart", true, () -> {
			SseApiServlet.notify("server-restart", "Server is restarting", "Server status", "red", true);
			MediaServer.stop();
			resetRenderers(true);

			LOGGER.trace("Waiting 1 second...");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.trace("Caught exception", e);
			}

			// re-create the server because may happened the
			// change of the used interface
			MediaServer.start();
			GuiManager.setReloadable(false);
		});
	}

	/**
	 * Reset renderers.
	 * The trigger is configuration change.
	 * @param delete True if removal of known renderers is needed
	 */
	public void resetRenderers(boolean delete) {
		RendererConfigurations.loadRendererConfigurations();
		if (delete) {
			ConnectedRenderers.deleteAllConnectedRenderers();
		}
	}

	/**
	 * Reset the media library.
	 * The trigger is configuration change.
	 */
	public void resetMediaLibrary() {
		if (mediaLibrary != null) {
			mediaLibrary.reset();
		}
		resetRenderersRoot();
	}

	/**
	 * Reset all renderers Root Folder.
	 * The trigger is configuration change.
	 */
	public void resetRenderersRoot() {
		ConnectedRenderers.resetAllRenderers();
		DLNAResource.bumpSystemUpdateId();
	}

	/**
	 * Reset the web graphical user interface server.
	 * The trigger is init.
	 */
	public void resetWebGuiServer() {
		if (webGuiServer != null) {
			GuiManager.removeGui(webGuiServer);
			webGuiServer.stop();
		}
		try {
			webGuiServer = WebGuiServer.createServer(umsConfiguration.getWebGuiServerPort());
		} catch (BindException b) {
			try {
				LOGGER.info("Unable to bind web interface on port: " + umsConfiguration.getWebGuiServerPort() + ", because: " + b.getMessage());
				LOGGER.info("Falling back to random port.");
				webGuiServer = WebGuiServer.createServer(0);
			} catch (IOException ex) {
				LOGGER.error("FATAL ERROR: Unable to set the gui server, because: " + ex.getMessage());
				LOGGER.info("Maybe another process is running or the hostname is wrong.");
			}
		} catch (IOException ex) {
			LOGGER.error("FATAL ERROR: Unable to set the gui server, because: " + ex.getMessage());
		}
		if (webGuiServer != null && webGuiServer.getServer() != null) {
			GuiManager.addGui(webGuiServer);
			LOGGER.info("GUI is available at: " + webGuiServer.getUrl());
		}
	}

	/**
	 * Reset the web player server.
	 * The trigger is init and configuration change.
	 */
	public void resetWebPlayerServer() {
		if (webPlayerServer != null) {
			webPlayerServer.stop();
		}
		if (umsConfiguration.useWebPlayerServer()) {
			try {
				webPlayerServer = WebPlayerServer.createServer(umsConfiguration.getWebPlayerServerPort());
				GuiManager.updateServerStatus();
			} catch (BindException b) {
				LOGGER.error("FATAL ERROR: Unable to bind web player on port: " + umsConfiguration.getWebPlayerServerPort() + ", because: " + b.getMessage());
				LOGGER.info("Maybe another process is running or the hostname is wrong.");
			} catch (IOException ex) {
				LOGGER.error("FATAL ERROR: Unable to read server port value from configuration");
			}
		}
	}

	/**
	 * Creates a new random {@link #uuid}. These are used to uniquely identify the server to renderers (i.e.
	 * renderers treat multiple servers with the same UUID as the same server).
	 * @return {@link String} with an Universally Unique Identifier.
	 */
	// XXX don't use the MAC address to seed the UUID as it breaks multiple profiles
	public String usn() {
		return "uuid:" + udn();
	}

	public synchronized String udn() {
		if (uuid == null) {
			// Retrieve UUID from configuration
			uuid = umsConfiguration.getUuid();

			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
				LOGGER.info("Generated new random UUID: {}", uuid);

				// save the newly-generated UUID
				umsConfiguration.setUuid(uuid);
				saveConfiguration();
			}

			LOGGER.info("Using the following UUID configured in UMS.conf: {}", uuid);
		}

		return uuid;
	}

	/**
	 * Returns the user friendly name of the UMS server.
	 * @return {@link String} with the user friendly name.
	 */
	public String getServerName() {
		if (serverName == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.getProperty("os.name").replace(" ", "_"));
			sb.append('-');
			sb.append(System.getProperty("os.arch").replace(" ", "_"));
			sb.append('-');
			sb.append(System.getProperty("os.version").replace(" ", "_"));
			sb.append(", UPnP/1.0 DLNADOC/1.50, UMS/").append(getVersion());
			serverName = sb.toString();
		}

		return serverName;
	}

	/**
	 * Returns the PMS instance.
	 *
	 * @return {@link net.pms.PMS}
	 */
	@Nonnull
	public static PMS get() {
		// XXX when we run as an application, the instance is initialized via the createInstance call in main().
		// However, plugin tests may need access to a UMS instance without going
		// to the trouble of launching the UMS application, so we provide a fallback
		// initialization here. Either way, createInstance() should only be called once (see below)
		if (instance == null) {
			createInstance();
		}

		return instance;
	}

	@Nonnull
	public static PMS getNewInstance() {
		instance = null;
		createInstance();
		return instance;
	}

	private static synchronized void createInstance() {
		assert instance == null; // this should only be called once
		instance = new PMS();

		try {
			if (instance.init()) {
				LOGGER.info("{} is now available for renderers to find", PMS.NAME);
			} else {
				LOGGER.info("{} initialization was aborted", PMS.NAME);
			}
		} catch (Exception e) {
			LOGGER.error("A serious error occurred during {} initialization: {}", PMS.NAME, e.getMessage());
			LOGGER.debug("", e);
		}
	}

	public static void main(String[] args) {
		boolean displayProfileChooser = false;
		boolean denyHeadless = false;
		File profilePath = null;

		// This must be called before JNA is used
		configureJNA();

		// Start caching log messages until the logger is configured
		CacheLogger.startCaching();

		// Set headless options if given as a system property when launching the JVM
		if (System.getProperty(CONSOLE_ARG, "").equalsIgnoreCase(Boolean.toString(true))) {
			forceHeadless();
		}
		if (System.getProperty(NOCONSOLE_ARG, "").equalsIgnoreCase(Boolean.toString(true))) {
			denyHeadless = true;
		}

		if (args.length > 0) {
			Pattern pattern = Pattern.compile(PROFILE);
			for (String arg : args) {
				switch (arg.trim().toLowerCase(Locale.ROOT)) {
					case HEADLESS_ARG, CONSOLE_ARG -> forceHeadless();
					case NATIVELOOK_ARG -> System.setProperty(NATIVELOOK_ARG, Boolean.toString(true));
					case SCROLLBARS -> System.setProperty(SCROLLBARS, Boolean.toString(true));
					case NOCONSOLE_ARG -> {
						denyHeadless = true;
					}
					case PROFILES -> {
						displayProfileChooser = true;
					}
					case TRACE -> {
						traceMode = 2;
					}
					case DBLOG, DBTRACE -> {
						logDB = true;
					}
					default -> {
						Matcher matcher = pattern.matcher(arg);
						if (matcher.find()) {
							profilePath = new File(matcher.group(1));
						}
					}
				}
			}
		}

		if (!GuiUtil.initDefaultToolkit()) {
			forceHeadless();
		}

		if (isHeadless() && denyHeadless) {
			System.err.println(
				"Either a graphics environment isn't available or headless " +
				"mode is forced, but \"noconsole\" is specified. " + PMS.NAME +
				" can't start, exiting."
			);
			System.exit(1);
		} else if (!isHeadless()) {
			LooksFrame.initializeLookAndFeel();
		}

		if (profilePath != null) {
			if (!FileUtil.isValidFileName(profilePath.getName())) {
				LOGGER.warn("Invalid file name in profile argument - using default profile");
			} else if (!profilePath.exists()) {
				LOGGER.warn("Specified profile ({}) doesn't exist - using default profile", profilePath.getAbsolutePath());
			} else {
				LOGGER.debug("Using specified profile: {}", profilePath.getAbsolutePath());
				System.setProperty("ums.profile.path", profilePath.getAbsolutePath());
			}
		} else if (!isHeadless() && displayProfileChooser) {
			ProfileChooser.display();
		}

		try {
			umsConfiguration = new UmsConfiguration();
			assert umsConfiguration != null;

			// Log whether the service is installed as it may help with debugging and support
			if (Platform.isWindows()) {
				boolean isUmsServiceInstalled = WindowsUtils.isUmsServiceInstalled();
				if (isUmsServiceInstalled) {
					LOGGER.info("The Windows service is installed.");
				}
			}

			/* Rename previous log file to .prev
			 * Log file location is unknown at this point, it's finally decided during loadFile() below
			 * but the file is also truncated at the same time, so we'll have to try a qualified guess
			 * for the file location.
			 */

			// Set root level from configuration here so that logging is available during renameOldLogFile();
			LoggingConfig.setRootLevel(Level.toLevel(umsConfiguration.getRootLogLevel()));

			// Load the (optional) LogBack config file.
			// This has to be called after 'new UmsConfiguration'
			LoggingConfig.loadFile();

			// Check TRACE mode
			if (traceMode == 2) {
				LoggingConfig.setRootLevel(Level.TRACE);
				LOGGER.debug("Forcing debug level to TRACE");
			} else {
				// Remember whether logging level was TRACE/ALL at startup
				traceMode = LoggingConfig.getRootLevel().toInt() <= Level.TRACE_INT ? 1 : 0;
			}

			// Configure syslog unless in forced trace mode
			if (traceMode != 2 && umsConfiguration.getLoggingUseSyslog()) {
				LoggingConfig.setSyslog();
			}
			// Configure log buffering
			if (traceMode != 2 && umsConfiguration.getLoggingBuffered()) {
				LoggingConfig.setBuffered(true);
			} else if (traceMode == 2) {
				// force unbuffered regardless of logback.xml if in forced trace mode
				LOGGER.debug("Forcing unbuffered verbose logging");
				LoggingConfig.setBuffered(false);
				LoggingConfig.forceVerboseFileEncoder();
			}

			// Write buffered messages to the log now that logger is configured
			CacheLogger.stopAndFlush();

			// Create services
			Services.create();

			LOGGER.debug(new Date().toString());

			try {
				umsConfiguration.initCred();
			} catch (IOException e) {
				LOGGER.debug("Error initializing credentials file: {}", e);
			}

			if (!isRunningTests() && umsConfiguration.isRunSingleInstance()) {
				killOld();
			}

			// Create the PMS instance returned by get()
			createInstance(); // Calls new() then init()
		} catch (ConfigurationException t) {
			String errorMessage = String.format(
				"Configuration error: %s: %s",
				t.getClass().getName(),
				t.getMessage()
			);

			LOGGER.error(errorMessage);

			if (!isHeadless() && instance != null) {
				GuiManager.showErrorMessage(errorMessage, Messages.getString("ErrorWhileStartingUms"));
			}
		} catch (InterruptedException e) {
			// Interrupted during startup
		}
	}

	public MediaServer getMediaServer() {
		return mediaServer;
	}

	public WebGuiServer getGuiServer() {
		return webGuiServer;
	}

	public WebPlayerServer getWebPlayerServer() {
		return webPlayerServer;
	}

	/**
	 * Save the configuration changes immediately to the configuration
	 * file and not wait for the automatic saving.
	 */
	public void saveConfiguration() {
		try {
			umsConfiguration.save();
		} catch (ConfigurationException e) {
			LOGGER.error("Could not save configuration", e);
		}
	}

	/**
	 * Retrieves the {@link net.pms.configuration.UmsConfiguration UmsConfiguration} object
	 * that contains all configured settings. The object provides getters for all
	 * configurable settings.
	 *
	 * @return The configuration object
	 */
	public static UmsConfiguration getConfiguration() {
		return umsConfiguration;
	}

	/**
	 * Retrieves the composite {@link net.pms.configuration.RendererDeviceConfiguration DeviceConfiguration} object
	 * that applies to this device, which acts as its {@link net.pms.configuration.UmsConfiguration UmsConfiguration}.
	 *
	 * This function should be used to resolve the relevant UmsConfiguration wherever the renderer
	 * is known or can be determined.
	 *
	 * @param  renderer The renderer.
	 * @return          The UmsConfiguration from object, if any, or the global UmsConfiguration.
	 */
	public static UmsConfiguration getConfiguration(Renderer renderer) {
		return renderer != null ? renderer.getUmsConfiguration() : umsConfiguration;
	}

	public static UmsConfiguration getConfiguration(OutputParams params) {
		return getConfiguration(params != null ? params.getMediaRenderer() : null);
	}

	// Note: this should be used only when no Renderer or OutputParams is available
	public static UmsConfiguration getConfiguration(DLNAResource dlna) {
		return getConfiguration(dlna != null ? dlna.getDefaultRenderer() : null);
	}

	/**
	 * Sets the {@link net.pms.configuration.UmsConfiguration UmsConfiguration} object
	 * that contains all configured settings for UMS. The object provides getters for all
	 * configurable UMS settings.
	 *
	 * @param conf The configuration object.
	 */
	public static void setConfiguration(UmsConfiguration conf) {
		umsConfiguration = conf;
	}

	/**
	 * Returns the project version for UMS.
	 *
	 * @return The project version.
	 */
	public static String getVersion() {
		return PropertiesUtil.getProjectProperties().get("project.version");
	}

	/**
	 * Shutdown the currently running Universal Media Server.
	 */
	public static void shutdown() {
		try {
			if (PlatformUtils.isWindows()) {
				WindowsNamedPipe.setLoop(false);
			}
			//Stop network scanner
			NetworkConfiguration.stop();

			LOGGER.debug("Shutting down the media server");
			MediaServer.stop();
			Thread.sleep(500);

			LOGGER.debug("Shutting down all active processes");

			Services.stopProcessManager();
			ProcessWrapperImpl.destroyCurrentProcesses();
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupted while shutting down..");
			LOGGER.trace("", e);
		}

		// Destroy services
		Services.destroy();

		LOGGER.info("Stopping {} {}", PropertiesUtil.getProjectProperties().get("project.name"), getVersion());
		/**
		 * Stopping logging gracefully (flushing logs)
		 * No logging is available after this point
		 */
		ILoggerFactory iLoggerContext = LoggerFactory.getILoggerFactory();
		if (iLoggerContext instanceof LoggerContext loggerContext) {
			loggerContext.stop();
		} else {
			LOGGER.error("Unable to shut down logging gracefully");
			System.err.println("Unable to shut down logging gracefully");
		}

		// Shut down library scanner
		if (getConfiguration().getUseCache()) {
			if (LibraryScanner.isScanLibraryRunning()) {
				LOGGER.debug("LibraryScanner is still running, attempting to stop it");
				LibraryScanner.stopScanLibrary();
			} else {
				LOGGER.debug("LibraryScanner already stopped");
			}
		}

		if (MediaDatabase.isInstantiated()) {
			LOGGER.debug("Shutting down media database");
			MediaDatabase.shutdown();
			MediaDatabase.createDatabaseReportIfNeeded();
		}
		if (UserDatabase.isInstantiated()) {
			LOGGER.debug("Shutting down user database");
			UserDatabase.shutdown();
			UserDatabase.createDatabaseReportIfNeeded();
		}
	}

	/**
	 * Terminates the currently running Universal Media Server.
	 */
	public static void quit() {
		System.exit(0);
	}

	/**
	 * Restart handling
	 */
	public static void killOld() {
		// Note: failure here doesn't necessarily mean we need admin rights,
		// only that we lack the required permission for these specific items.
		try {
			killProc();
		} catch (SecurityException e) {
			LOGGER.error(
				"Failed to check for already running instance: " + e.getMessage() +
				(PlatformUtils.isWindows() ? "\nUMS might need to run as an administrator to access the PID file" : "")
			);
		} catch (FileNotFoundException e) {
			LOGGER.debug("PID file not found, cannot check for running process");
		} catch (IOException e) {
			LOGGER.error("Error killing old process: " + e);
		}

		try {
			dumpPid();
		} catch (FileNotFoundException e) {
			LOGGER.error(
				"Failed to write PID file: " + e.getMessage() +
				(PlatformUtils.isWindows() ? "\nUMS might need to run as an administrator to enforce single instance" : "")
			);
		} catch (IOException e) {
			LOGGER.error("Error dumping PID " + e);
		}
	}

	/*
	 * This method is only called for Windows OS'es, so specialized Windows charset handling is allowed
	 */
	private static boolean verifyPidName(String pid) throws IOException, IllegalAccessException {
		if (!Platform.isWindows()) {
			throw new IllegalAccessException("verifyPidName can only be called from Windows!");
		}
		ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "\"PID eq " + pid + "\"", "/V", "/NH", "/FO", "CSV");
		pb.redirectErrorStream(true);
		Process p = pb.start();
		String line;

		Charset charset = WindowsUtils.getOEMCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
			LOGGER.warn("Couldn't find a supported charset for {}, using default ({})", WindowsUtils.getOEMCP(), charset);
		}
		try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), charset))) {
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				return false;
			}
			line = in.readLine();
		}

		if (line == null) {
			return false;
		}

		// remove all " and convert to common case before splitting result on ,
		String[] tmp = line.toLowerCase().replace("\"", "").split(",");
		// if the line is too short we don't kill the process
		if (tmp.length < 9) {
			return false;
		}

		// check first and last, update since taskkill changed
		// also check 2nd last since we migh have ", POSSIBLY UNSTABLE" in there
		boolean ums = tmp[tmp.length - 1].contains("universal media server") ||
						tmp[tmp.length - 2].contains("universal media server");
		return tmp[0].equals("javaw.exe") && ums;
	}

	private static String pidFile() {
		return umsConfiguration.getDataFile("UMS.pid");
	}

	private static void killProc() throws SecurityException, IOException {
		ProcessBuilder pb = null;
		String pid;
		String pidFile = pidFile();
		if (!FileUtil.getFilePermissions(pidFile).isReadable()) {
			throw new SecurityException("Cannot read " + pidFile);
		}

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile), StandardCharsets.US_ASCII))) {
			pid = in.readLine();
		}

		if (pid == null) {
			return;
		}

		if (PlatformUtils.isWindows()) {
			try {
				if (verifyPidName(pid)) {
					pb = new ProcessBuilder("taskkill", "/F", "/PID", pid, "/T");
				}
			} catch (IllegalAccessException e) {
				// Impossible
			}
		} else if (Platform.isFreeBSD() || Platform.isLinux() || Platform.isOpenBSD() || Platform.isSolaris()) {
			pb = new ProcessBuilder("kill", "-9", pid);
		}

		if (pb == null) {
			return;
		}

		try {
			Process p = pb.start();
			p.waitFor();
		} catch (InterruptedException e) {
			LOGGER.trace("Got interrupted while trying to kill process by PID " + e);
			Thread.currentThread().interrupt();
		}
	}

	public static long getPID() {
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		return Long.parseLong(processName.split("@")[0]);
	}

	private static void dumpPid() throws IOException {
		try (FileOutputStream out = new FileOutputStream(pidFile())) {
			long pid = getPID();
			LOGGER.debug("Writing PID: " + pid);
			String data = String.valueOf(pid) + "\r\n";
			out.write(data.getBytes(StandardCharsets.US_ASCII));
			out.flush();
		}
	}

	private DbgPacker dbgPack;

	public DbgPacker dbgPack() {
		return dbgPack;
	}

	private TempFileMgr tfm;

	public void addTempFile(File f) {
		tfm.add(f);
	}

	public void addTempFile(File f, int cleanTime) {
		tfm.add(f, cleanTime);
	}

	private static final ReadWriteLock HEADLESS_LOCK = new ReentrantReadWriteLock();
	private static Boolean headless = null;

	/**
	 * Checks if UMS is running in headless (console) mode.
	 * @return true if UMS is running in headless mode
	 */
	public static boolean isHeadless() {
		HEADLESS_LOCK.readLock().lock();
		try {
			if (headless == null) {
				headless = GuiUtil.isHeadless();
			}
			return headless;
		} finally {
			HEADLESS_LOCK.readLock().unlock();
		}
	}

	/**
	 * Forces UMS to run in headless (console) mode whether a graphics
	 * environment is available or not.
	 */
	public static void forceHeadless() {
		HEADLESS_LOCK.writeLock().lock();
		try {
			headless = true;
		} finally {
			HEADLESS_LOCK.writeLock().unlock();
		}
	}

	private static Locale locale = null;
	private static final ReadWriteLock LOCALE_LOCK = new ReentrantReadWriteLock();

	/**
	 * Gets the current {@link Locale} to be used in any {@link Locale}
	 * sensitive operations. If <code>null</code> the default {@link Locale}
	 * is returned.
	 * @return current {@link Locale} or default {@link Locale}
	 */
	public static Locale getLocale() {
		LOCALE_LOCK.readLock().lock();
		try {
			if (locale != null) {
				return locale;
			}
			return Locale.getDefault();
		} finally {
			LOCALE_LOCK.readLock().unlock();
		}
	}

	/**
	 * Sets UMS' {@link Locale}.
	 * @param aLocale the {@link Locale} to set
	 */
	public static void setLocale(Locale aLocale) {
		LOCALE_LOCK.writeLock().lock();
		try {
			locale = (Locale) aLocale.clone();
			Messages.setLocaleBundle(locale);
		} finally {
			LOCALE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Sets UMS' {@link Locale} with the same parameters as the
	 * {@link Locale} class constructor. <code>null</code> values are
	 * treated as empty strings.
	 *
	 * @param language An ISO 639 alpha-2 or alpha-3 language code, or a
	 * language subtag up to 8 characters in length. See the
	 * <code>Locale</code> class description about valid language values.
	 * @param country An ISO 3166 alpha-2 country code or a UN M.49
	 * numeric-3 area code. See the <code>Locale</code> class description
	 * about valid country values.
	 * @param variant Any arbitrary value used to indicate a variation of a
	 * <code>Locale</code>. See the <code>Locale</code> class description
	 * for the details.
	 */
	public static void setLocale(String language, String country, String variant) {
		if (country == null) {
			country = "";
		}
		if (variant == null) {
			variant = "";
		}
		LOCALE_LOCK.writeLock().lock();
		try {
			locale = new Locale(language, country, variant);
		} finally {
			LOCALE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Sets UMS' {@link Locale} with the same parameters as the
	 * {@link Locale} class constructor. <code>null</code> values are
	 * treated as empty strings.
	 *
	 * @param language An ISO 639 alpha-2 or alpha-3 language code, or a
	 * language subtag up to 8 characters in length. See the
	 * <code>Locale</code> class description about valid language values.
	 * @param country An ISO 3166 alpha-2 country code or a UN M.49
	 * numeric-3 area code. See the <code>Locale</code> class description
	 * about valid country values.
	 */
	public static void setLocale(String language, String country) {
		setLocale(language, country, "");
	}

	/**
	 * Sets UMS' {@link Locale} with the same parameters as the {@link Locale}
	 * class constructor. <code>null</code> values are
	 * treated as empty strings.
	 *
	 * @param language An ISO 639 alpha-2 or alpha-3 language code, or a
	 * language subtag up to 8 characters in length. See the
	 * <code>Locale</code> class description about valid language values.
	 */
	public static void setLocale(String language) {
		setLocale(language, "", "");
	}

	/**
	 * Sets the relative URL of a context sensitive help page located in the
	 * documentation directory.
	 *
	 * @param page The help page.
	 */
	public static void setHelpPage(String page) {
		helpPage = page;
	}

	/**
	 * Returns the relative URL of a context sensitive help page in the
	 * documentation directory.
	 *
	 * @return The help page.
	 */
	public static String getHelpPage() {
		return helpPage;
	}

	public static boolean isReady() {
		return get().ready;
	}

	public static GlobalIdRepo getGlobalRepo() {
		return get().globalRepo;
	}

	private CodeDb codes;
	private CodeEnter masterCode;

	public CodeDb codeDb() {
		return codes;
	}

	public void setMasterCode(CodeEnter ce) {
		masterCode = ce;
	}

	public boolean masterCodeValid() {
		return (masterCode != null && masterCode.validCode(null));
	}

	private DynamicPlaylist dynamicPls;

	public Playlist getDynamicPls() {
		if (dynamicPls == null) {
			dynamicPls = new DynamicPlaylist(Messages.getString("DynamicPlaylist"),
				umsConfiguration.getDynamicPlsSavePath(),
				(umsConfiguration.isDynamicPlsAutoSave() ? UMSUtils.IOList.AUTOSAVE : 0) | UMSUtils.IOList.PERMANENT);
		}
		return dynamicPls;
	}

	private static int traceMode = 0;
	private static boolean logDB;

	/**
	 * Returns current trace mode state
	 *
	 * @return
	 *			0 = Not started in trace mode<br>
	 *			1 = Started in trace mode<br>
	 *			2 = Forced to trace mode
	 */
	public static int getTraceMode() {
		return traceMode;
	}

	/**
	 * Returns if the mediaDatabase logging is forced by command line arguments.
	 *
	 * @return {@code true} if mediaDatabase logging is forced, {@code false}
	 *         otherwise.
	 */
	public static boolean getLogDB() {
		return logDB;
	}

	private CredMgr credMgr;

	public static CredMgr.Credential getCred(String owner) {
		if (instance == null || instance.credMgr == null) {
			return null;
		}
		return instance.credMgr.getCred(owner);
	}

	public static CredMgr.Credential getCred(String owner, String tag) {
		return instance.credMgr.getCred(owner, tag);
	}

	public static String getCredTag(String owner, String username) {
		return instance.credMgr.getTag(owner, username);
	}

	public static boolean verifyCred(String owner, String tag, String user, String pwd) {
		return instance.credMgr.verify(owner, tag, user, pwd);
	}

	/**
	 * @return whether UMS is being run by Surefire or a CI environment like GitHub Actions
	 */
	public static boolean isRunningTests() {
		return System.getProperty("surefire.real.class.path") != null || System.getenv("CI").equals("true");
	}

	/**
	 * Configures JNA according to the environment. This must be called before
	 * JNA is first initialized to have any effect.
	 */
	public static void configureJNA() {
		// Set JNA "jnidispatch" resolution rules
		try {
			if (
				System.getProperty("os.name") != null &&
				System.getProperty("os.name").startsWith("Windows") &&
				StringUtils.isNotBlank(System.getProperty("os.version")) &&
				Double.parseDouble(System.getProperty("os.version")) < 5.2
			) {
				String developmentPath = "src\\main\\external-resources\\lib\\winxp";
				if (new File(developmentPath).exists()) {
					System.setProperty("jna.boot.library.path", developmentPath);
				} else {
					System.setProperty("jna.boot.library.path", "windows\\winxp");
				}
			} else {
				System.setProperty("jna.nosys", "true");
			}
		} catch (NullPointerException | NumberFormatException e) {
			System.setProperty("jna.nosys", "true");
			System.err.println(
				"Could not determine Windows version from " +
				System.getProperty("os.version") +
				". Not applying Windows XP hack"
			);
		}
	}
}
