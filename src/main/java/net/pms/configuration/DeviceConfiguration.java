package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import net.pms.PMS;
import net.pms.network.UPNPHelper;
import net.pms.util.FileWatcher;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceConfiguration extends PmsConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfiguration.class);

	public static final int DEVICE = 0;
	public static final int RENDERER = 1;
	public static final int PMSCONF = 2;

	private PropertiesConfiguration deviceConf = null;
	private RendererConfiguration ref = null;
	private static HashMap<String, PropertiesConfiguration> deviceConfs;
	private static HashMap<String, String> xref;
	private static File deviceDir;

	public DeviceConfiguration() {
		super(0);
	}

	public DeviceConfiguration(File f, String uuid) throws ConfigurationException {
		super(f, uuid);
		inherit(this);
	}

	public DeviceConfiguration(RendererConfiguration ref) throws ConfigurationException {
		super(0);
		inherit(ref);
	}

	public DeviceConfiguration(RendererConfiguration ref, InetAddress ia) throws ConfigurationException {
		super(0);
		deviceConf = initConfiguration(ia);
		inherit(ref);
	}

	/**
	 * Creates a composite configuration for this device consisting of a dedicated device
	 * configuration plus the given reference renderer configuration and the default pms
	 * configuration for fallback lookup.
	 *
	 * @param ref The reference renderer configuration.
	 */
	public void inherit(RendererConfiguration ref) throws ConfigurationException {
		CompositeConfiguration cconf = new CompositeConfiguration();

		// Add the component configurations in order of lookup priority:

		// 1. The device configuration, marked as "in memory" (i.e. writeable)
		cconf.addConfiguration(deviceConf != null ? deviceConf : initConfiguration(null), true);
		// 2. The reference renderer configuration (read-only)
		cconf.addConfiguration(ref.getConfiguration());
		// 3. The default pms configuration (read-only)
		PmsConfiguration baseConf = PMS.getConfiguration();
		cconf.addConfiguration(baseConf.getConfiguration());

		// Handle all queries (external and internal) via the composite configuration
		configuration = cconf;
		pmsConfiguration = this;

		configurationReader = new ConfigurationReader(configuration, true);

		// Sync our internal PmsConfiguration vars
		// TODO: create new objects here instead?
		tempFolder = baseConf.tempFolder;
		programPaths = baseConf.programPaths;
		filter = baseConf.filter;

		// Initialize our internal RendererConfiguration vars
		sortedHeaderMatcher = ref.sortedHeaderMatcher;
		// Note: intentionally omitting 'player = null' so as to preserve player state when reloading
		loaded = true;
		this.ref = ref;

		init(NOFILE);
	}

	@Override
	public void reset() {
		try {
			inherit(ref);
			PMS.get().updateRenderer(this);
		} catch (Exception e) {
			LOGGER.debug("Error reloading device configuration {}: {}", this, e);
			e.printStackTrace();
		}
	}

	@Override
	public void setUUID(String uuid) {
		if (uuid != null && ! uuid.equals(this.uuid)) {
			this.uuid = uuid;
			// Switch to the custom device conf for this new uuid, if any
			if (deviceConfs.containsKey(uuid) && deviceConf != deviceConfs.get(uuid)) {
				deviceConf = initConfiguration(null);
				reset();
			}
		}
	}

	public PropertiesConfiguration initConfiguration(InetAddress ia) {
		String id = uuid != null ? uuid : ia != null ? ia.toString().substring(1) : null;
		if (id != null && deviceConfs.containsKey(id)) {
			deviceConf = deviceConfs.get(id);
			LOGGER.info("Using custom device configuration {} for {}", deviceConf.getFile().getName(), id);
		} else {
			deviceConf = createPropertiesConfiguration();
		}
		return deviceConf;
	}

	public PropertiesConfiguration getConfiguration(int index) {
		CompositeConfiguration c = (CompositeConfiguration) configuration;
		return (PropertiesConfiguration) c.getConfiguration(index);
	}

	@Override
	public File getFile() {
		if (loaded) {
			File f = getConfiguration(DEVICE).getFile();
			return (f != null && !f.equals(NOFILE)) ? f : getConfiguration(RENDERER).getFile();
		}
		return null;
	}

	public File getParentFile() {
		return getConfiguration(RENDERER).getFile();
	}

	public boolean isValid() {
		if (loaded) {
			File f = getConfiguration(DEVICE).getFile();
			if (f != null) {
				if (!f.exists()) {
					// Reset
					getConfiguration(DEVICE).setFile(NOFILE);
					getConfiguration(DEVICE).clear();
					deviceConfs.remove(getId());
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isCustomized() {
		if (isValid()) {
			File f = getConfiguration(DEVICE).getFile();
			return f != null && !f.equals(NOFILE);
		}
		return false;
	}

	public static File getDeviceDir() {
		return deviceDir;
	}

	public static void loadDeviceConfigurations(PmsConfiguration pmsConf) {
		deviceConfs = new HashMap<>();
		xref = new HashMap<>();
		deviceDir = new File(pmsConf.getProfileDirectory(), "renderers");
		if (deviceDir.exists()) {
			LOGGER.info("Loading device configurations from " + deviceDir.getAbsolutePath());
			File[] files = deviceDir.listFiles();
			Arrays.sort(files);
			for (File f : files) {
				if (f.getName().endsWith(".conf")) {
					loadDeviceFile(f, createPropertiesConfiguration());
					PMS.getFileWatcher().add(new FileWatcher.Watch(f.getPath(), reloader));
				}
			}
		}
	}

	public static String[] loadDeviceFile(File f, PropertiesConfiguration conf) {
		String filename = f.getName();
		try {
			conf.load(f);
			String s = conf.getString(DEVICE_ID, "");
			if (s.isEmpty() && conf.containsKey("device")) {
				// Backward compatibility
				s = conf.getString("device", "");
			}
			String[] ids = s.split("\\s*,\\s*");
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					deviceConfs.put(id, conf);
					LOGGER.info("Loaded device configuration {} for {}", filename, id);
				}
			}
			return ids;
		} catch (ConfigurationException ce) {
			LOGGER.info("Error loading device configuration: " + f.getAbsolutePath());
		}
		return null;
	}

	public static int getDeviceUpnpMode(String id) {
		return getDeviceUpnpMode(id, false);
	}

	public static int getDeviceUpnpMode(String id, boolean store) {
		int mode = deviceConfs.containsKey(id) ? getUpnpMode(deviceConfs.get(id).getString(UPNP_ALLOW, "true")) : ALLOW;
		if (store && id.startsWith("uuid:")) {
			crossReference(id, UPNPHelper.getAddress(id));
		}
		return mode;
	}

	public static void crossReference(String uuid, InetAddress ia) {
		// FIXME: this assumes one device per address
		String address = ia.toString().substring(1);
		xref.put(address, uuid);
		xref.put(uuid, address);
		if (deviceConfs.containsKey(uuid)) {
			deviceConfs.put(address, deviceConfs.get(uuid));
		}
	}

	public static String getUuidOf(InetAddress ia) {
		// FIXME: this assumes one device per address
		return ia != null ? xref.get(ia.toString().substring(1)) : null;
	}

	public static File createDeviceFile(DeviceConfiguration r, String filename, boolean load) {
		File file = null;
		try {
			if (StringUtils.isBlank(filename)) {
				filename = getDefaultFilename(r);
			} else if (!filename.endsWith(".conf")) {
				filename += ".conf";
			}
			file = new File(deviceDir, filename);
			ArrayList<String> conf = new ArrayList<>();

			// Add the header and device id
			conf.add("#----------------------------------------------------------------------------");
			conf.add("# Custom Device profile");
			conf.add("# See DefaultRenderer.conf for descriptions of all possible renderer options");
			conf.add("# and UMS.conf for program options.");
			conf.add("");
			conf.add("# Options in this file override the default settings for the specific " + r.getSimpleName(r) + " device(s) listed below.");
			conf.add("# Specify devices by uuid (or address if no uuid), separated by commas if more than one.");
			conf.add("");
			conf.add(DEVICE_ID + " = " + r.getId());

			FileUtils.writeLines(file, "utf-8", conf, "\r\n");

			if (load) {
				loadDeviceFile(file, r.getConfiguration(DEVICE));
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating device configuration file: " + ie);
		}
		return file;
	}

	public static ArrayList<RendererConfiguration> getInheritors(RendererConfiguration renderer) {
		ArrayList<RendererConfiguration> devices = new ArrayList<>();
		RendererConfiguration ref = (renderer instanceof DeviceConfiguration) ? ((DeviceConfiguration)renderer).ref : renderer;
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if ((r instanceof DeviceConfiguration) && ((DeviceConfiguration)r).ref == ref) {
				devices.add(r);
			}
		}
		return devices;
	}

	/**
	 * Automatic reloading
	 */
	public static final FileWatcher.Listener reloader = new FileWatcher.Listener() {
		@Override
		public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir) {
			File f = new File(filename);
			PropertiesConfiguration conf = null;
			HashSet<String> ids = new HashSet<>();
			for (Iterator<String> iterator = deviceConfs.keySet().iterator(); iterator.hasNext();) {
				String id = iterator.next();
				PropertiesConfiguration c = deviceConfs.get(id);
				if (c.getFile().equals(f)) {
					ids.add(id);
					conf = c;
					iterator.remove();
				}
			}
			conf.clear();
			ids.addAll(Arrays.asList(loadDeviceFile(f, conf)));
			for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
				if ((r instanceof DeviceConfiguration) && ids.contains(((DeviceConfiguration)r).getId())) {
					r.reset();
				}
			}
		}
	};
}
