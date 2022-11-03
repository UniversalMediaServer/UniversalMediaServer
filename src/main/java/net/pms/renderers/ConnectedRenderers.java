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
package net.pms.renderers;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.SpeedStats;
import net.pms.network.mediaserver.UPNPHelper;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handle all renderers and devices founded.
 * TODO : Add UPNPHelper devices founded, DeviceConfigurations devices founded
 */
public class ConnectedRenderers {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedRenderers.class);
	private static final Map<InetAddress, RendererConfiguration> ADDRESS_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());

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
		RendererConfiguration ref = RendererConfigurations.getRendererConfigurationByHeaders(sortedHeaders);
		if (ref != null) {
			boolean isNew = !ConnectedRenderers.hasRendererConfigurationForInetAddress(ia);
			r = ConnectedRenderers.resolve(ia, ref);
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

	public static RendererConfiguration getRendererConfigurationBySocketAddress(InetAddress sa) {
		RendererConfiguration r = ADDRESS_ASSOCIATION.get(sa);
		if (r != null) {
			LOGGER.trace("Matched media renderer \"{}\" based on address {}", r.getRendererName(), sa.getHostAddress());
		}
		return r;
	}

	public static RendererConfiguration getRendererConfigurationByUUID(String uuid) {
		for (RendererConfiguration conf : getConnectedRenderersConfigurations()) {
			if (uuid.equals(conf.getUUID())) {
				return conf;
			}
		}
		return null;
	}

	public static boolean hasConnectedAVTransportPlayers() {
		return UPNPHelper.hasRenderer(UPNPHelper.AVT);
	}

	public static List<RendererConfiguration> getConnectedAVTransportPlayers() {
		return UPNPHelper.getRenderers(UPNPHelper.AVT);
	}

	public static boolean hasConnectedControlPlayers() {
		return hasConnectedRenderer(UPNPHelper.ANY);
	}

	public static List<RendererConfiguration> getConnectedControlPlayers() {
		return getConnectedRenderers(UPNPHelper.ANY);
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

	public static RendererConfiguration resolve(InetAddress ia, RendererConfiguration ref) {
		DeviceConfiguration r = null;
		boolean recognized = ref != null;
		if (!recognized) {
			ref = RendererConfigurations.getDefaultConf();
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

	public static void delete(final RendererConfiguration r, long delay) {
		r.setActive(false);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
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
			}
		};
		new Timer("RendererDeletion").schedule(task, delay);
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

	public static boolean hasRendererConfigurationForInetAddress(InetAddress r) {
		return ADDRESS_ASSOCIATION.containsKey(r);
	}

	public static boolean hasInetAddressForRendererConfiguration(RendererConfiguration r) {
		return ADDRESS_ASSOCIATION.containsValue(r);
	}

	public static InetAddress getRendererConfigurationInetAddress(RendererConfiguration r) {
		for (Entry<InetAddress, RendererConfiguration> entry : ADDRESS_ASSOCIATION.entrySet()) {
			if (entry.getValue() == r) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static void calculateAllSpeeds() {
		Map<InetAddress, String> values = new HashMap<>();
		for (Entry<InetAddress, RendererConfiguration> entry : ADDRESS_ASSOCIATION.entrySet()) {
			InetAddress sa = entry.getKey();
			if (sa.isLoopbackAddress() || sa.isAnyLocalAddress()) {
				continue;
			}
			RendererConfiguration r = entry.getValue();
			if (!r.isOffline()) {
				values.put(sa, r.getRendererName());
			}
		}
		for (Map.Entry<InetAddress, String> entry : values.entrySet()) {
			SpeedStats.getSpeedInMBits(entry.getKey(), entry.getValue());
		}
	}

}
