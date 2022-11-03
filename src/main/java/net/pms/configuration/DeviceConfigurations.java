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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.pms.PMS;
import net.pms.network.mediaserver.UPNPHelper;
import net.pms.renderers.ConnectedRenderers;
import net.pms.util.FileWatcher;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keep track of runtime DeviceConfiguration founded.
 */
public class DeviceConfigurations {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConfigurations.class);
	private static final Map<String, String> XREF = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, PropertiesConfiguration> deviceConfs = Collections.synchronizedMap(new HashMap<>());

	private static File deviceDir;

	/**
	 * This class is not meant to be instantiated.
	 */
	private DeviceConfigurations() {}

	public static boolean hasDeviceConfiguration(String id) {
		return deviceConfs.containsKey(id);
	}

	public static PropertiesConfiguration getDeviceConfiguration(String id) {
		return deviceConfs.get(id);
	}

	public static void removeDeviceConfiguration(String id) {
		deviceConfs.remove(id);
	}

	public static boolean isDeviceConfigurationChanged(String id, PropertiesConfiguration deviceConf) {
		return deviceConfs.containsKey(id) && deviceConf != deviceConfs.get(id);
	}

	public static int getDeviceUpnpMode(String id) {
		if (deviceConfs.containsKey(id)) {
			return RendererConfiguration.getUpnpMode(deviceConfs.get(id).getString(RendererConfiguration.KEY_UPNP_ALLOW, "true"));
		}
		return RendererConfiguration.UPNP_ALLOW;
	}

	public static int getDeviceUpnpMode(String id, boolean store) {
		if (store && id.startsWith("uuid:")) {
			crossReference(id, UPNPHelper.getAddress(id));
		}
		return getDeviceUpnpMode(id);
	}

	public static void crossReference(String uuid, InetAddress ia) {
		// FIXME: this assumes one device per address
		String address = ia.toString().substring(1);
		XREF.put(address, uuid);
		XREF.put(uuid, address);
		if (deviceConfs.containsKey(uuid)) {
			deviceConfs.put(address, deviceConfs.get(uuid));
		}
	}

	public static String getUuidOf(InetAddress ia) {
		// FIXME: this assumes one device per address
		return ia != null ? XREF.get(ia.toString().substring(1)) : null;
	}

	public static List<RendererConfiguration> getInheritors(RendererConfiguration renderer) {
		ArrayList<RendererConfiguration> devices = new ArrayList<>();
		RendererConfiguration ref = (renderer instanceof DeviceConfiguration) ? ((DeviceConfiguration) renderer).getRef() : renderer;
		for (RendererConfiguration r : ConnectedRenderers.getConnectedRenderersConfigurations()) {
			if (r instanceof DeviceConfiguration deviceConfiguration && deviceConfiguration.getRef() == ref) {
				devices.add(r);
			}
		}
		return devices;
	}

	public static File getDeviceDir() {
		return deviceDir;
	}

	public static List<String> loadDeviceFile(File f, PropertiesConfiguration conf) {
		List<String> idsList = new ArrayList<>();
		String filename = f.getName();
		try {
			conf.load(f);
			String s = conf.getString(RendererConfiguration.KEY_DEVICE_ID, "");
			if (s.isEmpty() && conf.containsKey("device")) {
				// Backward compatibility
				s = conf.getString("device", "");
			}
			String[] ids = s.split("\\s*,\\s*");
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					idsList.add(s);
					deviceConfs.put(id, conf);
					LOGGER.info("Loaded device configuration {} for {}", filename, id);
				}
			}
		} catch (ConfigurationException ce) {
			LOGGER.info("Error loading device configuration: " + f.getAbsolutePath());
		}
		return idsList;
	}

	public static void loadDeviceConfigurations() {
		deviceConfs.clear();
		XREF.clear();
		deviceDir = new File(PMS.getConfiguration().getProfileDirectory(), "renderers");
		if (deviceDir.exists()) {
			LOGGER.info("Loading device configurations from " + deviceDir.getAbsolutePath());
			File[] files = deviceDir.listFiles();
			Arrays.sort(files);
			for (File f : files) {
				if (f.getName().endsWith(".conf")) {
					List<String> ids = loadDeviceFile(f, RendererConfiguration.createPropertiesConfiguration());
					if (!ids.isEmpty()) {
						FileWatcher.add(new FileWatcher.Watch(f.getPath(), RELOADER));
					}
				}
			}
		}
	}

	/**
	 * Automatic reloading
	 */
	private static final FileWatcher.Listener RELOADER = new FileWatcher.Listener() {
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
			if (conf != null) {
				conf.clear();
				List<String> idsList = loadDeviceFile(f, conf);
				if (!idsList.isEmpty()) {
					ids.addAll(idsList);
				}
				for (RendererConfiguration r : ConnectedRenderers.getConnectedRenderersConfigurations()) {
					if (r instanceof DeviceConfiguration d && ids.contains(d.getId())) {
						r.reset();
					}
				}
			}
		}
	};

}
