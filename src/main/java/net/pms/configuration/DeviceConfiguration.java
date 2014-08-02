package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceConfiguration extends RendererConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfiguration.class);

	public static final int DEVICE = 0;
	public static final int RENDERER = 1;

	private PropertiesConfiguration deviceConf = null;
	private static HashMap<String, PropertiesConfiguration> deviceConfs;
	private static File deviceDir;

	public DeviceConfiguration() throws ConfigurationException {
		super(false);
	}

	public DeviceConfiguration(File f, String uuid) throws ConfigurationException {
		super(f, uuid);
		inherit(this);
	}

	public DeviceConfiguration(RendererConfiguration ref) throws ConfigurationException {
		super(false);
		inherit(ref);
	}

	public DeviceConfiguration(RendererConfiguration ref, InetAddress ia) throws ConfigurationException {
		super(false);
		deviceConf =  initConfiguration(ia);
		inherit(ref);
	}

	/**
	 * Creates a composite configuration for this device consisting of a dedicated device
	 * configuration plus the given reference renderer configuration for fallback lookup.
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

		// Set up
		configuration = cconf;
		configurationReader = new ConfigurationReader(configuration, false);

		// Initialize our internal RendererConfiguration vars
		mimes = new HashMap<>();
		charMap = new HashMap<>();
		DLNAPN = new HashMap<>();
		renderCache = new HashMap<>();
		player = null;
		loaded = true;

		init(null);
	}

	public String getName() {
		return StringUtils.substringBefore(getRendererName(), "(").trim();
	}

	public String getId() {
		return uuid != null ? uuid : getAddress().toString().substring(1);
	}

	public PropertiesConfiguration initConfiguration(InetAddress ia) throws ConfigurationException {
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
		CompositeConfiguration c = (CompositeConfiguration)configuration;
		return (PropertiesConfiguration)c.getConfiguration(index);
	}

	@Override
	public File getFile(boolean force) {
		CompositeConfiguration c = (CompositeConfiguration)configuration;
		File f = getConfiguration(DEVICE).getFile();
		return (f != null && !f.equals(NOFILE)) ? f : getConfiguration(RENDERER).getFile();
	}

	public File getParentFile() {
		CompositeConfiguration c = (CompositeConfiguration)configuration;
		return getConfiguration(RENDERER).getFile();
	}

	public boolean isValid() {
		File f = getConfiguration(DEVICE).getFile();
		if (f != null) {
			if (! f.exists()) {
				// Reset
				getConfiguration(DEVICE).setFile(NOFILE);
				getConfiguration(DEVICE).clear();
				deviceConfs.remove(getId());
				return false;
			}
		}
		return true;
	}

	public boolean isCustomized() {
		if (isValid()) {
			File f = getConfiguration(DEVICE).getFile();
			return f != null && ! f.equals(NOFILE);
		}
		return false;
	}

	public static File getDeviceDir() {
		return deviceDir;
	}

	public static void loadDeviceConfigurations(PmsConfiguration pmsConf) {
		deviceConfs = new HashMap<>();
		deviceDir = new File(pmsConf.getProfileDirectory(), "renderers");
		if (deviceDir.exists()) {
			LOGGER.info("Loading device configurations from " + deviceDir.getAbsolutePath());
			File[] files = deviceDir.listFiles();
			Arrays.sort(files);
			for (File f : files) {
				if (f.getName().endsWith(".conf")) {
					loadDeviceFile(f, createPropertiesConfiguration());
				}
			}
		}
	}

	public static void loadDeviceFile(File f, PropertiesConfiguration conf) {
		String filename = f.getName();
		try {
			conf.load(f);
			for (String id : conf.getStringArray("device")) {
				if (StringUtils.isNotBlank(id)) {
					deviceConfs.put(id, conf);
					LOGGER.info("Loaded device configuration {} for {}", filename, id);
				}
			}
		} catch (ConfigurationException ce) {
			LOGGER.info("Error loading device configuration: " + f.getAbsolutePath());
		}
	}

	public static String getDefaultFilename(DeviceConfiguration r) {
		String id = r.getId();
		return r.getName() + "-" + (id.startsWith("uuid:") ? id.substring(5,11) : id) + ".conf";
	}

	public static File createDeviceFile(DeviceConfiguration r, String filename, boolean load) {
		File file = null;
		try {
			if (StringUtils.isBlank(filename)) {
				filename = getDefaultFilename(r);
			} else if (! filename.endsWith(".conf")) {
				filename += ".conf";
			}
			file = new File(deviceDir, filename);
			ArrayList<String> conf = new ArrayList<String>();

			// Add the header and device id
			conf.add("#----------------------------------------------------------------------------");
			conf.add("# Custom Device profile");
			conf.add("# See PS3.conf for a description of all possible configuration options.");
			conf.add("");
			conf.add("# Options in this file override the default settings for the specific " + r.getName() + " device(s) listed below.");
			conf.add("# Specify devices by uuid (or address if no uuid), separated by commas if more than one.");
			conf.add("");
			conf.add("device = " + r.getId());

			FileUtils.writeLines(file, "utf-8", conf, "\r\n");

			if (load) {
				loadDeviceFile(file, r.getConfiguration(DEVICE));
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating device configuration file: " + ie);
		}
		return file;
	}

}
