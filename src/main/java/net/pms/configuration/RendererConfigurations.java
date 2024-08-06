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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import net.pms.PMS;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.util.FileWatcher;
import net.pms.util.PropertiesUtil;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class maintain all renderers models configs enabled in UMS conf.
 * This class also maintain all configurations specific to devices.
 */
public class RendererConfigurations {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererConfigurations.class);
	public static final String ALL_RENDERERS_KEY = "All renderers";

	/**
	 * {@link #ENABLED_RENDERERS_CONFS} doesn't normally need locking since
	 * modification is rare and {@link #loadRendererConfigurations(UmsConfiguration)}
	 * is only called during {@link PMS#init()} (To avoid any chance of a
	 * race condition proper locking should be implemented though). During
	 * build on the other hand the method is called repeatedly and it is random
	 * if a {@link ConcurrentModificationException} is thrown as a result.
	 *
	 * To avoid build problems, this is used to make sure that calls to
	 * {@link #loadRendererConfigurations(UmsConfiguration)} is serialized.
	 */
	private static final Object LOAD_RENDERER_CONFIGURATIONS_LOCK = new Object();
	private static final List<String> ALL_RENDERERS_NAMES = Collections.synchronizedList(new ArrayList<>());

	/**
	 * A loading priority comparator
	 */
	public static final Comparator<RendererConfiguration> RENDERER_LOADING_PRIORITY_COMPARATOR = (RendererConfiguration r1, RendererConfiguration r2) -> {
		if (r1 == null || r2 == null) {
			if (r1 == null && r2 == null) {
				return 0;
			}
			return (r1 == null) ? 1 : -1;
		}
		int p1 = r1.getLoadingPriority();
		int p2 = r2.getLoadingPriority();
		if (p1 > p2) {
			return -1;
		} else if (p1 < p2) {
			return 1;
		}
		return r1.getConfName().compareToIgnoreCase(r2.getConfName());
	};
	private static final SortedSet<RendererConfiguration> ENABLED_RENDERERS_CONFS = Collections.synchronizedSortedSet(new TreeSet<>(RENDERER_LOADING_PRIORITY_COMPARATOR));
	private static final Map<String, PropertiesConfiguration> DEVICES_CONFS = Collections.synchronizedMap(new HashMap<>());

	private static RendererConfiguration defaultConf;
	private static Renderer defaultRenderer;

	/**
	 * This class is not meant to be instantiated.
	 */
	private RendererConfigurations() {}

	//TODO : Assign this dynamically on UMS conf update.
	public static RendererConfiguration getDefaultConf() {
		return defaultConf;
	}

	//TODO : Assign this dynamically on UMS conf update.
	public static Renderer getDefaultRenderer() {
		return defaultRenderer;
	}

	/**
	 * Returns the list of enabled renderer configurations.
	 *
	 * @return The list of enabled renderers.
	 */
	public static List<RendererConfiguration> getEnabledRenderersConfigurations() {
		return new ArrayList<>(ENABLED_RENDERERS_CONFS);
	}

	private static void addRendererConfiguration(RendererConfiguration r) {
		ENABLED_RENDERERS_CONFS.add(r);
	}

	/**
	 * Tries to find a matching renderer configuration based on the name of
	 * the renderer.
	 * Returns true if the provided name is equal to or a substring of the
	 * renderer name defined in a configuration, where case does not matter.
	 *
	 * @param name The renderer name to match.
	 * @return The matching renderer configuration or <code>null</code>
	 *
	 * @since 1.50.1
	 */
	public static synchronized RendererConfiguration getRendererConfigurationByName(String name) {
		for (RendererConfiguration conf : ENABLED_RENDERERS_CONFS) {
			if (conf.getConfName().toLowerCase().contains(name.toLowerCase())) {
				return conf;
			}
		}
		return null;
	}

	public static synchronized RendererConfiguration getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders) {
		if (PMS.getConfiguration().isRendererForceDefault()) {
			// Force default renderer
			RendererConfiguration r = getDefaultConf();
			LOGGER.debug("Forcing renderer match to \"" + r.getRendererName() + "\"");
			return r;
		}
		for (RendererConfiguration r : ENABLED_RENDERERS_CONFS) {
			if (r.match(sortedHeaders)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on headers " + sortedHeaders);
				return r;
			}
		}
		return null;
	}

	public static synchronized RendererConfiguration getRendererConfigurationByUPNPDetails(String details) {
		for (RendererConfiguration r : ENABLED_RENDERERS_CONFS) {
			if (r.matchUPNPDetails(details)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on dlna details \"" + details + "\"");
				return r;
			}
		}
		return null;
	}

	public static File getRenderersDir() {
		String[] pathList = PropertiesUtil.getProjectProperties().get("project.renderers.dir").split(",");

		for (String path : pathList) {
			if (path.trim().length() > 0) {
				File file = new File(path.trim());

				if (file.isDirectory()) {
					if (file.canRead()) {
						return file;
					}
					LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
				}
			}
		}

		return null;
	}

	public static File getProfileRenderersDir() {
		File file = new File(PMS.getConfiguration().getProfileDirectory(), "renderers");
		if (file.isDirectory()) {
			if (file.canRead()) {
				return file;
			}
			LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
		}
		return null;
	}

	public static File getRenderersIconFile(String icon) {
		File file = new File(icon);
		if (!file.isAbsolute() && file.getParent() == null) {
			//try profile renderers dir
			file = new File(getProfileRenderersDir(), icon);
			if (file.isFile() && file.exists()) {
				return file;
			}
			//try renderers dir
			return new File(getRenderersDir(), icon);
		}
		return file;
	}

	public static File getWritableRenderersDir() {
		//first test the profile directory
		File file = new File(PMS.getConfiguration().getProfileDirectory(), "renderers");
		if (file.isDirectory()) {
			if (file.canWrite()) {
				return file;
			}
			LOGGER.warn("Can't write directory: {}", file.getAbsolutePath());
		}

		//then test the app directory
		String[] pathList = PropertiesUtil.getProjectProperties().get("project.renderers.dir").split(",");
		for (String path : pathList) {
			if (path.trim().length() > 0) {
				file = new File(path.trim());

				if (file.isDirectory()) {
					if (file.canWrite()) {
						return file;
					}
					LOGGER.warn("Can't write directory: {}", file.getAbsolutePath());
				}
			}
		}

		return null;
	}

	public static List<String> getAllRenderersNames() {
		return new ArrayList<>(ALL_RENDERERS_NAMES);
	}

	/**
	 * @return all renderer names as a JSON array
	 */
	public static synchronized JsonArray getAllRendererNamesAsJsonArray() {
		List<String> values = getAllRenderersNames();

		JsonArray jsonArray = new JsonArray();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", ALL_RENDERERS_KEY);
		jsonObject.addProperty("label", "i18n@AllRenderers");
		jsonArray.add(jsonObject);
		jsonObject = new JsonObject();
		jsonObject.addProperty("value", BaseConfiguration.EMPTY_LIST_VALUE);
		jsonObject.addProperty("label", "i18n@None");
		jsonArray.add(jsonObject);
		for (int i = 0; i < values.size(); i++) {
			jsonObject = new JsonObject();
			jsonObject.addProperty("value", values.get(i));
			jsonObject.addProperty("label", values.get(i));
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}

	/**
	 * This builds the dropdown for setting the default renderer.
	 *
	 * @return all default renderers as a JSON array
	 */
	public static synchronized JsonArray getEnabledRendererNamesAsJsonArray() {
		List<RendererConfiguration> values = getEnabledRenderersConfigurations();
		sortRendererConfigurationsByName(values);
		JsonArray jsonArray = new JsonArray();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", "");
		jsonObject.addProperty("label", "i18n@UnknownRenderer");
		jsonArray.add(jsonObject);
		for (int i = 0; i < values.size(); i++) {
			jsonObject = new JsonObject();
			String value = values.get(i).getConfName();
			jsonObject.addProperty("value", value);
			jsonObject.addProperty("label", value);
			jsonArray.add(jsonObject);
		}
		return jsonArray;
	}

	public static boolean hasDeviceConfiguration(String id) {
		return DEVICES_CONFS.containsKey(id);
	}

	public static boolean isDeviceConfigurationChanged(String id, PropertiesConfiguration deviceConf) {
		return DEVICES_CONFS.containsKey(id) && deviceConf != DEVICES_CONFS.get(id);
	}

	public static PropertiesConfiguration getDeviceConfiguration(String id) {
		return DEVICES_CONFS.get(id);
	}

	public static void removeDeviceConfiguration(String id) {
		DEVICES_CONFS.remove(id);
	}

	public static int getDeviceUpnpMode(String id) {
		if (DEVICES_CONFS.containsKey(id)) {
			return Renderer.getUpnpMode(DEVICES_CONFS.get(id).getString(RendererConfiguration.KEY_UPNP_ALLOW, "true"));
		}
		return Renderer.UPNP_ALLOW;
	}

	public static void createRendererFile(Renderer renderer, File file, boolean load, File refFile) {
		try {
			List<String> lines = RendererConfiguration.getRendererLines(renderer, refFile);
			FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, "\r\n");
			if (load) {
				try {
					RendererConfiguration rendererConf = new RendererConfiguration(file);
					RendererConfigurations.addRendererConfiguration(rendererConf);
					renderer.inherit(rendererConf);
				} catch (ConfigurationException ce) {
					LOGGER.debug("Error initializing renderer configuration: " + ce);
				}
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating renderer configuration file: " + ie);
		}
	}

	public static File createDeviceFile(Renderer renderer, String filename, boolean load) {
		File file = null;
		try {
			if (StringUtils.isBlank(filename)) {
				filename = renderer.getDefaultFilename();
			} else if (!filename.endsWith(".conf")) {
				filename += ".conf";
			}
			File dir = getWritableRenderersDir();
			if (dir == null) {
				LOGGER.warn("Error creating device configuration file: Can't find a writable directory");
			}
			file = new File(dir, filename);
			List<String> lines = RendererConfiguration.getDeviceLines(renderer);

			FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), lines, "\r\n");

			if (load) {
				RendererConfiguration rendererConf = new RendererConfiguration(file);
				reloadDeviceFile(file, rendererConf);
				FileWatcher.add(new FileWatcher.Watch(file.getPath(), RELOADER));
			}
		} catch (IOException | ConfigurationException ie) {
			LOGGER.debug("Error creating device configuration file: " + ie);
		}
		return file;
	}

	/**
	 * Load all renderer configuration files and set up the default renderer.
	 *
	 * TODO : Assign this dynamically on UMS conf update.
	 * For now, it need a complete app restart.
	 */
	public static synchronized void loadRendererConfigurations() {
		synchronized (LOAD_RENDERER_CONFIGURATIONS_LOCK) {
			ALL_RENDERERS_NAMES.clear();
			ENABLED_RENDERERS_CONFS.clear();
			try {
				defaultConf = new RendererConfiguration(null);
				defaultRenderer = new Renderer(defaultConf);
			} catch (ConfigurationException e) {
				LOGGER.debug("Caught exception", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			loadConfigurations(getProfileRenderersDir(), true);
			loadConfigurations(getRenderersDir(), false);
		}

		LOGGER.info("Enabled " + ENABLED_RENDERERS_CONFS.size() + " configurations, listed in order of loading priority:");
		for (RendererConfiguration rendererConf : ENABLED_RENDERERS_CONFS) {
			LOGGER.info(":   " + rendererConf);
		}

		if (!ENABLED_RENDERERS_CONFS.isEmpty()) {
			// See if a different default configuration was configured
			String rendererFallback = PMS.getConfiguration().getRendererDefault();

			if (StringUtils.isNotBlank(rendererFallback)) {
				RendererConfiguration fallbackConf = getRendererConfigurationByName(rendererFallback);

				if (fallbackConf != null) {
					// A valid fallback configuration was set, use it as default.
					defaultConf = fallbackConf;
					try {
						defaultRenderer.inherit(defaultConf);
					} catch (ConfigurationException e) {
						LOGGER.debug("Caught exception", e);
					}
				}
			}
		}
		//TODO : set collection auto sort
		Collections.sort(ALL_RENDERERS_NAMES, String.CASE_INSENSITIVE_ORDER);
	}

	private static void loadConfigurations(File renderersDir, boolean profile) {
		if (renderersDir != null) {
			LOGGER.info("Loading renderer and device configurations from " + renderersDir.getAbsolutePath());

			File[] files = renderersDir.listFiles();
			Arrays.sort(files);

			for (File file : files) {
				if (file.getName().endsWith(".conf")) {
					try {
						RendererConfiguration rendererConf = new RendererConfiguration(file);
						if (rendererConf.hasDeviceId()) {
							//device specific conf
							loadDeviceConfiguration(rendererConf);
						} else {
							//renderer specific conf
							loadRendererConfiguration(rendererConf, profile);
						}
						FileWatcher.add(new FileWatcher.Watch(file.getPath(), RELOADER));
					} catch (ConfigurationException ce) {
						LOGGER.info("Error in loading configuration of: " + file.getAbsolutePath());
					}
				}
			}
		}
	}

	private static void loadRendererConfiguration(RendererConfiguration rendererConf, boolean profile) {
		List<String> selectedRenderers = PMS.getConfiguration().getSelectedRenderers();
		String rendererName = rendererConf.getConfName();
		if (profile) {
			rendererName = rendererName + "*";
		}
		if (!ALL_RENDERERS_NAMES.contains(rendererName)) {
			ALL_RENDERERS_NAMES.add(rendererName);
		}
		String renderersGroup = null;
		if (rendererName.indexOf(' ') > 0) {
			renderersGroup = rendererName.substring(0, rendererName.indexOf(' '));
		}

		if (selectedRenderers.contains(rendererName) || selectedRenderers.contains(renderersGroup) || selectedRenderers.contains(ALL_RENDERERS_KEY)) {
			ENABLED_RENDERERS_CONFS.add(rendererConf);
		} else {
			LOGGER.debug("Ignored \"{}\" configuration", rendererName);
		}
	}

	private static List<String> loadDeviceConfiguration(RendererConfiguration rendererConf) {
		List<String> idsList = new ArrayList<>();
		String deviceId = rendererConf.getDeviceId();
		String[] ids = deviceId.split("\\s*,\\s*");
		for (String id : ids) {
			if (StringUtils.isNotBlank(id)) {
				if (!idsList.contains(deviceId)) {
					idsList.add(id);
				}
				DEVICES_CONFS.put(id, (PropertiesConfiguration) rendererConf.getConfiguration());
				LOGGER.info("Loaded device configuration {} for {}", rendererConf.getFile().getName(), id);
			}
		}
		return idsList;
	}

	/**
	 * Automatic renderer and device conf reloading
	 */
	private static final FileWatcher.Listener RELOADER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> reloadConfigurationFile(filename);

	private static void reloadConfigurationFile(String filename) {
		File file = new File(filename);
		try {
			RendererConfiguration rendererConf = new RendererConfiguration(file);
			if (rendererConf.hasDeviceId()) {
				//device specific conf
				reloadDeviceFile(file, rendererConf);
			}
		} catch (ConfigurationException ce) {
			LOGGER.info("Error in reloading configuration of: " + file.getAbsolutePath());
		}
	}

	private static void reloadDeviceFile(File file, RendererConfiguration rendererConf) {
		List<String> idsList = new ArrayList<>();
		for (Iterator<String> iterator = DEVICES_CONFS.keySet().iterator(); iterator.hasNext();) {
			String id = iterator.next();
			PropertiesConfiguration deviceConfiguration = DEVICES_CONFS.get(id);
			if (deviceConfiguration.getFile().equals(file)) {
				idsList.add(id);
				iterator.remove();
			}
		}
		idsList.addAll(loadDeviceConfiguration(rendererConf));
		if (!idsList.isEmpty()) {
			for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
				if (idsList.contains(renderer.getId())) {
					renderer.reset();
				}
			}
		}
	}

	public static void sortRendererConfigurationsByName(List<RendererConfiguration> rendererConfigurations) {
		Collections.sort(rendererConfigurations, (RendererConfiguration o1, RendererConfiguration o2) -> {
			if (o1 == null && o2 == null) {
				return 0;
			}

			if (o1 == null) {
				return 1;
			}

			if (o2 == null) {
				return -1;
			}

			return o1.getRendererName().toLowerCase().compareTo(o2.getRendererName().toLowerCase());
		});
	}

}
