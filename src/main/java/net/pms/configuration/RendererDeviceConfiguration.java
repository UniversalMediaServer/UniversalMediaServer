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
import java.net.InetAddress;
import java.util.UUID;
import net.pms.PMS;
import net.pms.renderers.ConnectedRenderers;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for all {@link Renderer} settings.
 * This class create a CompositeConfiguration that will be used by Renderer.
 * <pre>
 * Querying properties is ordered :
 *		- Device specific configuration if any.
 *		- Renderer model configuration.
 *		- UMS main configuration.
 * </pre>
 */
public class RendererDeviceConfiguration extends RendererConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererDeviceConfiguration.class);

	private static final int DEVICE = 0;
	private static final int RENDERER = 1;

	private PropertiesConfiguration deviceConf = null;
	private RendererConfiguration ref = null;
	protected String uuid;

	public RendererDeviceConfiguration(RendererConfiguration ref, InetAddress ia, String uuid) throws ConfigurationException, InterruptedException {
		initDeviceConfiguration(ia, uuid);
		inherit(ref);
		if (uuid == null && ia != null) {
			//create a temp uuid to serve files
			uuid = UUID.nameUUIDFromBytes(ia.getAddress()).toString();
		}
		if (uuid != null) {
			this.uuid = uuid;
		}
	}

	/**
	 * Creates a composite configuration for this device consisting of a dedicated device
	 * configuration plus the given reference renderer configuration and the default UMS
	 * configuration for fallback lookup.
	 *
	 * @param ref The reference renderer configuration.
	 * @throws ConfigurationException
	 */
	public final void inherit(RendererConfiguration ref) throws ConfigurationException {
		CompositeConfiguration cconf = new CompositeConfiguration();
		cconf.setListDelimiter((char) 0);

		// Add the component configurations in order of lookup priority:

		// 1. The device configuration, marked as "in memory" (i.e. writeable)
		cconf.addConfiguration(deviceConf, true);
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
		configurationReader = new ConfigurationReader(configuration, true);

		umsConfiguration = new UmsConfiguration(configuration, configurationReader);

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
		} catch (ConfigurationException e) {
			LOGGER.debug("Error reloading device configuration {}: {}", this, e);
		}
	}

	public void setUUID(String uuid) {
		if (uuid != null && !uuid.equals(this.uuid)) {
			this.uuid = uuid;
			//advise uuid changed
			uuidChanged();
			// Switch to the custom device conf for this new uuid, if any
			if (RendererConfigurations.isDeviceConfigurationChanged(uuid, deviceConf)) {
				initDeviceConfiguration(null, uuid);
				reset();
			}
		}
	}

	/**
	 * Returns the uuid of this renderer, if known. Default value is null.
	 *
	 * @return The uuid.
	 */
	public String getUUID() {
		return uuid;
	}

	protected void uuidChanged() {
		//let renderer know uuid was changed
	}

	public final PropertiesConfiguration initDeviceConfiguration(InetAddress ia, String uuid) {
		//try first the uuid if exists then ip address
		String id = uuid != null ? uuid : ConnectedRenderers.getUuidOf(ia);
		if (ia != null && (id == null || !RendererConfigurations.hasDeviceConfiguration(id))) {
			id = ia.toString().substring(1);
		}
		if (id != null && RendererConfigurations.hasDeviceConfiguration(id)) {
			deviceConf = RendererConfigurations.getDeviceConfiguration(id);
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

	public PropertiesConfiguration getDeviceConfiguration() {
		return getConfiguration(DEVICE);
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
				RendererConfigurations.removeDeviceConfiguration(getUUID());
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

}
