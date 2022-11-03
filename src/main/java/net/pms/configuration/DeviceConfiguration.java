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
package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import net.pms.PMS;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceConfiguration extends UmsConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfiguration.class);

	private static final int DEVICE = 0;
	private static final int RENDERER = 1;

	private PropertiesConfiguration deviceConf = null;
	private RendererConfiguration ref = null;

	public DeviceConfiguration() throws InterruptedException {
		super(0);
	}

	public DeviceConfiguration(File f, String uuid) throws ConfigurationException, InterruptedException {
		super(f, uuid);
		inherit(null);
	}

	public DeviceConfiguration(RendererConfiguration ref) throws ConfigurationException, InterruptedException {
		super(0);
		inherit(ref);
	}

	public DeviceConfiguration(RendererConfiguration ref, InetAddress ia) throws ConfigurationException, InterruptedException {
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
	 * @throws ConfigurationException
	 */
	public final void inherit(RendererConfiguration ref) throws ConfigurationException {
		CompositeConfiguration cconf = new CompositeConfiguration();

		// Add the component configurations in order of lookup priority:

		// 1. The device configuration, marked as "in memory" (i.e. writeable)
		cconf.addConfiguration(deviceConf != null ? deviceConf : initConfiguration(null), true);
		// 2. The reference renderer configuration (read-only)
		if (ref == null) {
			cconf.addConfiguration(getConfiguration());
		} else {
			cconf.addConfiguration(ref.getConfiguration());
		}
		// 3. The default pms configuration (read-only)
		UmsConfiguration baseConf = PMS.getConfiguration();
		cconf.addConfiguration(baseConf.getConfiguration());

		// Handle all queries (external and internal) via the composite configuration
		configuration = cconf;
		umsConfiguration = this;

		configurationReader = new ConfigurationReader(configuration, true);

		// Sync our internal UmsConfiguration vars
		// TODO: create new objects here instead?
		tempFolder = baseConf.tempFolder;
		filter = baseConf.filter;

		// Initialize our internal RendererConfiguration vars
		if (ref != null) {
			sortedHeaderMatcher = ref.sortedHeaderMatcher;
		}

		// Note: intentionally omitting 'player = null' so as to preserve player state when reloading
		loaded = true;
		if (ref == null) {
			this.ref = this;
		} else {
			this.ref = ref;
		}

		init(NOFILE);
	}

	/**
	 * Temporarily assign the default renderer but mark it as unloaded.
	 *
	 * Actual recognition can happen later once the http server receives a request.
	 * This is to allow initiation of upnp playback before http recognition has occurred.
	 */
	public final void inheritDefault() throws ConfigurationException {
		inherit(RendererConfigurations.getDefaultConf());
		loaded = false;
	}

	@Override
	public void reset() {
		try {
			inherit(ref);
			updateRendererGui();
		} catch (ConfigurationException e) {
			LOGGER.debug("Error reloading device configuration {}: {}", this, e);
		}
	}

	@Override
	public void setUUID(String uuid) {
		if (uuid != null && !uuid.equals(this.uuid)) {
			this.uuid = uuid;
			// Switch to the custom device conf for this new uuid, if any
			if (DeviceConfigurations.isDeviceConfigurationChanged(uuid, deviceConf)) {
				deviceConf = initConfiguration(null);
				reset();
			}
		}
	}

	public final PropertiesConfiguration initConfiguration(InetAddress ia) {
		String id = uuid != null ? uuid : ia != null ? ia.toString().substring(1) : null;
		if (id != null && DeviceConfigurations.hasDeviceConfiguration(id)) {
			deviceConf = DeviceConfigurations.getDeviceConfiguration(id);
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

	public RendererConfiguration getRef() {
		return ref;
	}

	public boolean isValid() {
		if (loaded) {
			File f = getConfiguration(DEVICE).getFile();
			if (f != null && !f.exists()) {
				// Reset
				getConfiguration(DEVICE).setFile(NOFILE);
				getConfiguration(DEVICE).clear();
				DeviceConfigurations.removeDeviceConfiguration(getId());
				return false;
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

	public static File createDeviceFile(DeviceConfiguration r, String filename, boolean load) {
		File file = null;
		try {
			if (StringUtils.isBlank(filename)) {
				filename = getDefaultFilename(r);
			} else if (!filename.endsWith(".conf")) {
				filename += ".conf";
			}
			file = new File(DeviceConfigurations.getDeviceDir(), filename);
			ArrayList<String> conf = new ArrayList<>();

			// Add the header and device id
			conf.add("#----------------------------------------------------------------------------");
			conf.add("# Custom Device profile");
			conf.add("# See DefaultRenderer.conf for descriptions of all possible renderer options");
			conf.add("# and UMS.conf for program options.");
			conf.add("");
			conf.add("# Options in this file override the default settings for the specific " + getSimpleName(r) + " device(s) listed below.");
			conf.add("# Specify devices by uuid (or address if no uuid), separated by commas if more than one.");
			conf.add("");
			conf.add(KEY_DEVICE_ID + " = " + r.getId());

			FileUtils.writeLines(file, "utf-8", conf, "\r\n");

			if (load) {
				DeviceConfigurations.loadDeviceFile(file, r.getConfiguration(DEVICE));
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating device configuration file: " + ie);
		}
		return file;
	}

}
