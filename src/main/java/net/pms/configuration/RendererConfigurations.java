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
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import net.pms.PMS;
import net.pms.network.SpeedStats;
import net.pms.network.mediaserver.UPNPHelper;
import net.pms.newgui.GeneralTab;
import net.pms.util.PropertiesUtil;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handle all renderers configs.
 * It should be splitted in config / ADDRESS_ASSOCIATION, upnp etc
 */
public class RendererConfigurations {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererConfigurations.class);
	public static final String ALL_RENDERERS = "All renderers";
	protected static UmsConfiguration umsConfiguration = PMS.getConfiguration();

	/**
	 * {@link #enabledRendererConfs} doesn't normally need locking since
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
	private static final ArrayList<String> ALL_RENDERERS_NAMES = new ArrayList<>();
	private static final Map<InetAddress, RendererConfiguration> ADDRESS_ASSOCIATION = new HashMap<>();
	private static RendererConfiguration defaultConf;
	private static DeviceConfiguration streamingConf;

	private static TreeSet<RendererConfiguration> enabledRendererConfs;

	/**
	 * This class is not meant to be instantiated.
	 */
	private RendererConfigurations() {}

	public static RendererConfiguration getDefaultConf() {
		return defaultConf;
	}

	public static RendererConfiguration getStreamingConf() {
		return streamingConf;
	}

	/**
	 * Returns the list of enabled renderer configurations.
	 *
	 * @return The list of enabled renderers.
	 */
	public static List<RendererConfiguration> getEnabledRenderersConfigurations() {
		return enabledRendererConfs != null ? new ArrayList<>(enabledRendererConfs) : null;
	}

	/**
	 * Returns the list of all connected renderer devices.
	 *
	 * @return The list of connected renderers.
	 */
	public static Collection<RendererConfiguration> getConnectedRenderersConfigurations() {
		// We need to check both UPnP and http sides to ensure a complete list
		HashSet<RendererConfiguration> renderers = new HashSet<>(UPNPHelper.getRenderers(UPNPHelper.ANY));
		renderers.addAll(ADDRESS_ASSOCIATION.values());
		// Ensure any remaining secondary common-ip renderers (which are no longer in address association) are added
		renderers.addAll(PMS.get().getFoundRenderers());
		return renderers;
	}

	public static boolean hasConnectedAVTransportPlayers() {
		return UPNPHelper.hasRenderer(UPNPHelper.AVT);
	}

	public static List<RendererConfiguration> getConnectedAVTransportPlayers() {
		return UPNPHelper.getRenderers(UPNPHelper.AVT);
	}

	public static boolean hasConnectedRenderer(int type) {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if (r.isControllable(type)) {
				return true;
			}
		}
		return false;
	}

	public static List<RendererConfiguration> getConnectedRenderers(int type) {
		ArrayList<RendererConfiguration> renderers = new ArrayList<>();
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if (r.isActive() && r.isControllable(type)) {
				renderers.add(r);
			}
		}
		return renderers;
	}

	public static boolean hasConnectedControlPlayers() {
		return hasConnectedRenderer(UPNPHelper.ANY);
	}

	public static List<RendererConfiguration> getConnectedControlPlayers() {
		return getConnectedRenderers(UPNPHelper.ANY);
	}

	/**
	 * Searches for an instance of this renderer connected at the given address.
	 *
	 * @param r the renderer.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static RendererConfiguration find(RendererConfiguration r, InetAddress ia) {
		return find(r.getConfName(), ia);
	}

	/**
	 * Searches for a renderer of this name connected at the given address.
	 *
	 * @param name the renderer name.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static RendererConfiguration find(String name, InetAddress ia) {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if (ia.equals(r.getAddress()) && name.equals(r.getConfName())) {
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
		File file = new File(umsConfiguration.getProfileDirectory(), "renderers");
		if (file.isDirectory()) {
			if (file.canRead()) {
				return file;
			}
			LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
		}
		return null;
	}

	/**
	 * Delete connected renderers devices.
	 */
	public static void deleteAllConnectedRenderers() {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			delete(r, 0);
		}
	}

	public static void resetAllRenderers() {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			r.setRootFolder(null);
		}
		// Resetting enabledRendererConfs isn't strictly speaking necessary any more, since
		// these are now for reference only and never actually populate their root folders.
		for (RendererConfiguration r : enabledRendererConfs) {
			r.setRootFolder(null);
		}
	}

	public static void addRendererConfigurationAssociation(InetAddress sa, RendererConfiguration r) {
		// FIXME: handle multiple clients with same ip properly, now newer overwrites older

		RendererConfiguration prev = ADDRESS_ASSOCIATION.put(sa, r);
		if (prev != null) {
			// We've displaced a previous renderer at this address, so
			// check  if it's a ghost instance that should be deleted.
			verify(prev);
		}
	}

	public static boolean hasRendererConfigurationInetAddress(RendererConfiguration r) {
		return ADDRESS_ASSOCIATION.containsValue(r);
	}

	public static InetAddress getRendererConfigurationInetAddress(RendererConfiguration r) {
		for (Entry<InetAddress, RendererConfiguration> entry : RendererConfigurations.ADDRESS_ASSOCIATION.entrySet()) {
			if (entry.getValue() == r) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static RendererConfiguration getRendererConfigurationBySocketAddress(InetAddress sa) {
		RendererConfiguration r = ADDRESS_ASSOCIATION.get(sa);
		if (r != null) {
			LOGGER.trace("Matched media renderer \"{}\" based on address {}", r.getRendererName(), sa.getHostAddress());
		}
		return r;
	}

	/**
	 * Tries to find a matching renderer configuration based on the given collection of
	 * request headers
	 *
	 * @param headers The headers.
	 * @param ia The request's origin address.
	 * @return The matching renderer configuration or <code>null</code>
	 */
	public static RendererConfiguration getRendererConfigurationByHeaders(Collection<Map.Entry<String, String>> headers, InetAddress ia) {
		return getRendererConfigurationByHeaders(new SortedHeaderMap(headers), ia);
	}

	public static RendererConfiguration getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders, InetAddress ia) {
		RendererConfiguration r = null;
		RendererConfiguration ref = getRendererConfigurationByHeaders(sortedHeaders);
		if (ref != null) {
			boolean isNew = !ADDRESS_ASSOCIATION.containsKey(ia);
			r = resolve(ia, ref);
			if (r != null) {
				LOGGER.trace(
					"Matched {}media renderer \"{}\" based on headers {}",
					isNew ? "new " : "",
					r.getRendererName(),
					sortedHeaders
				);
			}
		}
		return r;
	}

	public static RendererConfiguration getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders) {
		if (umsConfiguration.isRendererForceDefault()) {
			// Force default renderer
			LOGGER.debug("Forcing renderer match to \"" + defaultConf.getRendererName() + "\"");
			return defaultConf;
		}
		for (RendererConfiguration r : getEnabledRenderersConfigurations()) {
			if (r.match(sortedHeaders)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on headers " + sortedHeaders);
				return r;
			}
		}
		return null;
	}

	/**
	 * Tries to find a matching renderer configuration based on the name of
	 * the renderer. Returns true if the provided name is equal to or a
	 * substring of the renderer name defined in a configuration, where case
	 * does not matter.
	 *
	 * @param name The renderer name to match.
	 * @return The matching renderer configuration or <code>null</code>
	 *
	 * @since 1.50.1
	 */
	public static RendererConfiguration getRendererConfigurationByName(String name) {
		for (RendererConfiguration conf : enabledRendererConfs) {
			if (conf.getConfName().toLowerCase().contains(name.toLowerCase())) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByUUID(String uuid) {
		for (RendererConfiguration conf : getConnectedRenderersConfigurations()) {
			if (uuid.equals(conf.getUUID())) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByUPNPDetails(String details) {
		for (RendererConfiguration r : enabledRendererConfs) {
			if (r.matchUPNPDetails(details)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on dlna details \"" + details + "\"");
				return r;
			}
		}
		return null;
	}

	public static RendererConfiguration resolve(InetAddress ia, RendererConfiguration ref) {
		DeviceConfiguration r = null;
		boolean recognized = ref != null;
		if (!recognized) {
			ref = getDefaultConf();
		}
		try {
			if (ADDRESS_ASSOCIATION.containsKey(ia)) {
				// Already seen, finish configuration if required
				r = (DeviceConfiguration) ADDRESS_ASSOCIATION.get(ia);
				boolean higher = ref != null && ref.getLoadingPriority() > r.getLoadingPriority() && recognized;
				if (!r.isLoaded() || higher) {
					LOGGER.debug("Finishing configuration for {}", r);
					if (higher) {
						LOGGER.debug("Switching to higher priority renderer: {}", ref);
					}
					r.inherit(ref);
					// update gui
					r.updateRendererGui();
				}
			} else if (!UPNPHelper.isNonRenderer(ia)) {
				// It's brand new
				r = new DeviceConfiguration(ref, ia);
				if (r.associateIP(ia)) {
					PMS.get().setRendererFound(r);
				}
				r.setActive(true);
				if (r.isUpnpPostponed()) {
					r.setUpnpMode(RendererConfiguration.UPNP_ALLOW);
				}
			}
		} catch (ConfigurationException e) {
			LOGGER.error("Configuration error while resolving renderer: {}", e.getMessage());
			LOGGER.trace("", e);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted while resolving renderer \"{}\": {}", ia, e.getMessage());
			return null;
		}
		if (!recognized) {
			// Mark it as unloaded so actual recognition can happen later if UPnP sees it.
			LOGGER.trace("Marking renderer \"{}\" at {} as unrecognized", r, ia.getHostAddress());
			if (r != null) {
				r.resetLoaded();
			}
		}
		return r;
	}

	public static void verify(RendererConfiguration r) {
		// FIXME: this is a very fallible, incomplete validity test for use only until
		// we find something better. The assumption is that renderers unable determine
		// their own address (i.e. non-UPnP/web renderers that have lost their spot in the
		// address association to a newer renderer at the same ip) are "invalid".
		if (r.getUpnpMode() != RendererConfiguration.UPNP_BLOCK && r.getAddress() == null) {
			LOGGER.debug("Purging renderer {} as invalid", r);
			r.delete(0);
		}
	}

	public static void delete(final RendererConfiguration r, int delay) {
		r.setActive(false);
		// Using javax.swing.Timer because of gui (this works in headless mode too).
		javax.swing.Timer t = new javax.swing.Timer(delay, (ActionEvent event) -> {
			// Make sure we haven't been reactivated while asleep
			if (!r.isActive()) {
				LOGGER.debug("Deleting renderer " + r);
				r.deleteGuis();
				PMS.get().getFoundRenderers().remove(r);
				UPNPHelper.getInstance().removeRenderer(r);
				InetAddress ia = r.getAddress();
				if (ADDRESS_ASSOCIATION.get(ia) == r) {
					ADDRESS_ASSOCIATION.remove(ia);
				}
				// TODO: actually delete rootfolder, etc.
			}
		});
		t.setRepeats(false);
		t.start();
	}

	public static List<String> getAllRenderersNames() {
		return ALL_RENDERERS_NAMES;
	}

	/**
	 * @return all renderer names as a JSON array
	 */
	public static synchronized JsonArray getAllRendererNamesAsJsonArray() {
		List<String> values = getAllRenderersNames();

		JsonArray jsonArray = new JsonArray();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", ALL_RENDERERS);
		jsonObject.addProperty("label", "i18n@AllRenderers");
		jsonArray.add(jsonObject);
		jsonObject = new JsonObject();
		jsonObject.addProperty("value", "None");
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
		GeneralTab.sortRendererConfigurationsByName(values);
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

	public static void calculateAllSpeeds() {
		for (Entry<InetAddress, RendererConfiguration> entry : ADDRESS_ASSOCIATION.entrySet()) {
			InetAddress sa = entry.getKey();
			if (sa.isLoopbackAddress() || sa.isAnyLocalAddress()) {
				continue;
			}
			RendererConfiguration r = entry.getValue();
			if (!r.isOffline()) {
				SpeedStats.getSpeedInMBits(sa, r.getRendererName());
			}
		}
	}

	/**
	 * Load all renderer configuration files and set up the default renderer.
	 *
	 * @param umsConf
	 */
	public static void loadRendererConfigurations() {
		synchronized (LOAD_RENDERER_CONFIGURATIONS_LOCK) {
			umsConfiguration = PMS.getConfiguration();
			enabledRendererConfs = new TreeSet<>(RENDERER_LOADING_PRIORITY_COMPARATOR);

			try {
				defaultConf = new RendererConfiguration();
				streamingConf = new DeviceConfiguration();
				streamingConf.inherit(defaultConf);
			} catch (ConfigurationException | InterruptedException e) {
				LOGGER.debug("Caught exception", e);
			}

			File[] renderersDirs = new File[]{getProfileRenderersDir(), getRenderersDir()};
			for (File renderersDir : renderersDirs) {
				if (renderersDir != null) {
					LOGGER.info("Loading renderer configurations from " + renderersDir.getAbsolutePath());

					File[] confs = renderersDir.listFiles();
					Arrays.sort(confs);
					int rank = 1;

					List<String> selectedRenderers = umsConfiguration.getSelectedRenderers();
					for (File f : confs) {
						if (f.getName().endsWith(".conf")) {
							try {
								RendererConfiguration r = new RendererConfiguration(f);
								//do not add device conf
								if (r.hasDeviceId()) {
									continue;
								}
								r.setRank(rank++);
								String rendererName = r.getConfName();
								ALL_RENDERERS_NAMES.add(rendererName);
								String renderersGroup = null;
								if (rendererName.indexOf(' ') > 0) {
									renderersGroup = rendererName.substring(0, rendererName.indexOf(' '));
								}

								if (selectedRenderers.contains(rendererName) || selectedRenderers.contains(renderersGroup) || selectedRenderers.contains(ALL_RENDERERS)) {
									enabledRendererConfs.add(r);
								} else {
									LOGGER.debug("Ignored \"{}\" configuration", rendererName);
								}
							} catch (ConfigurationException ce) {
								LOGGER.info("Error in loading configuration of: " + f.getAbsolutePath());
							}
						}
					}
				}
			}
		}

		LOGGER.info("Enabled " + enabledRendererConfs.size() + " configurations, listed in order of loading priority:");
		for (RendererConfiguration r : enabledRendererConfs) {
			LOGGER.info(":   " + r);
		}

		if (!enabledRendererConfs.isEmpty()) {
			// See if a different default configuration was configured
			String rendererFallback = umsConfiguration.getRendererDefault();

			if (StringUtils.isNotBlank(rendererFallback)) {
				RendererConfiguration fallbackConf = getRendererConfigurationByName(rendererFallback);

				if (fallbackConf != null) {
					// A valid fallback configuration was set, use it as default.
					defaultConf = fallbackConf;
				}
			}
		}
		Collections.sort(ALL_RENDERERS_NAMES, String.CASE_INSENSITIVE_ORDER);
		DeviceConfigurations.loadDeviceConfigurations(umsConfiguration);
	}

	public static void addRendererConfiguration(RendererConfiguration r) {
		enabledRendererConfs.add(r);
	}

	/**
	 * A loading priority comparator
	 */
	public static final Comparator<RendererConfiguration> RENDERER_LOADING_PRIORITY_COMPARATOR = (RendererConfiguration r1, RendererConfiguration r2) -> {
		if (r1 == null || r2 == null) {
			return (r1 == null && r2 == null) ? 0 : r1 == null ? 1 : r2 == null ? -1 : 0;
		}
		int p1 = r1.getLoadingPriority();
		int p2 = r2.getLoadingPriority();
		return p1 > p2 ? -1 : p1 < p2 ? 1 : r1.getConfName().compareToIgnoreCase(r2.getConfName());
	};
}
