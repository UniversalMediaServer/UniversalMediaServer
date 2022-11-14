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
import java.util.UUID;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.SpeedStats;
import net.pms.renderers.devices.WebGuiRenderer;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handle all renderers and devices found.
 */
public class ConnectedRenderers {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedRenderers.class);
	private static final Map<InetAddress, Renderer> ADDRESS_RENDERER_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());
	private static final Map<InetAddress, String> ADDRESS_UUID_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, WebGuiRenderer> REACT_CLIENT_RENDERERS = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, Renderer> UUID_RENDERER_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Returns the list of all connected renderer devices.
	 *
	 * @return The list of connected renderers.
	 */
	public static Collection<Renderer> getConnectedRenderers() {
		// We need to check both UPnP and http sides to ensure a complete list
		HashSet<Renderer> renderers = new HashSet<>(UUID_RENDERER_ASSOCIATION.values());
		renderers.addAll(ADDRESS_RENDERER_ASSOCIATION.values());
		renderers.addAll(REACT_CLIENT_RENDERERS.values());
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
	public static Renderer getRendererConfigurationByHeaders(Collection<Map.Entry<String, String>> headers, InetAddress ia) {
		return getRendererConfigurationByHeaders(new SortedHeaderMap(headers), ia);
	}

	public static Renderer getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders, InetAddress ia) {
		Renderer r = null;
		RendererConfiguration ref = RendererConfigurations.getRendererConfigurationByHeaders(sortedHeaders);
		if (ref != null) {
			boolean isNew = !ADDRESS_RENDERER_ASSOCIATION.containsKey(ia);
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

	public static Renderer getRendererBySocketAddress(InetAddress sa) {
		Renderer r = ADDRESS_RENDERER_ASSOCIATION.get(sa);
		if (r != null) {
			LOGGER.trace("Matched media renderer \"{}\" based on address {}", r.getRendererName(), sa.getHostAddress());
		}
		return r;
	}

	public static Renderer getRendererByUUID(String uuid) {
		for (Renderer renderer : getConnectedRenderers()) {
			if (uuid.equals(renderer.getUUID())) {
				return renderer;
			}
		}
		return null;
	}

	/**
	 * Searches for an instance of this renderer connected at the given address.
	 *
	 * @param r the renderer.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static Renderer find(Renderer r, InetAddress ia) {
		return find(r.getConfName(), ia);
	}

	/**
	 * Searches for a renderer of this name connected at the given address.
	 *
	 * @param name the renderer name.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static Renderer find(String name, InetAddress ia) {
		for (Renderer r : getConnectedRenderers()) {
			if (ia.equals(r.getAddress()) && name.equals(r.getConfName())) {
				return r;
			}
		}
		return null;
	}

	public static Renderer resolve(InetAddress ia, RendererConfiguration ref) {
		Renderer renderer = null;
		boolean recognized = ref != null;
		if (!recognized) {
			ref = RendererConfigurations.getDefaultConf();
		}
		try {
			if (ADDRESS_RENDERER_ASSOCIATION.containsKey(ia)) {
				// Already seen, finish configuration if required
				renderer = ADDRESS_RENDERER_ASSOCIATION.get(ia);
				boolean higher = ref != null && ref.getLoadingPriority() > renderer.getLoadingPriority() && recognized;
				if (!renderer.isLoaded() || higher) {
					LOGGER.debug("Finishing configuration for {}", renderer);
					if (higher) {
						LOGGER.debug("Switching to higher priority renderer: {}", ref);
					}
					renderer.inherit(ref);
					// update gui
					renderer.updateRendererGui();
				}
			} else if (!JUPnPDeviceHelper.isNonRenderer(ia)) {
				// It's brand new
				renderer = new Renderer(ref, ia);
				if (renderer.associateIP(ia)) {
					PMS.get().setRendererFound(renderer);
				}
				renderer.setActive(true);
				if (renderer.isUpnpPostponed()) {
					renderer.setUpnpMode(Renderer.UPNP_ALLOW);
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
			LOGGER.trace("Marking renderer \"{}\" at {} as unrecognized", renderer, ia.getHostAddress());
			if (renderer != null) {
				renderer.resetLoaded();
			}
		}
		return renderer;
	}

	public static void verify(Renderer r) {
		// FIXME: this is a very fallible, incomplete validity test for use only until
		// we find something better. The assumption is that renderers unable determine
		// their own address (i.e. non-UPnP/web renderers that have lost their spot in the
		// address association to a newer renderer at the same ip) are "invalid".
		if (r.getUpnpMode() != Renderer.UPNP_BLOCK && r.getAddress() == null) {
			LOGGER.debug("Purging renderer {} as invalid", r);
			r.delete(0);
		}
	}

	public static void delete(final Renderer renderer, long delay) {
		renderer.setActive(false);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				// Make sure we haven't been reactivated while asleep
				if (!renderer.isActive()) {
					LOGGER.debug("Deleting renderer " + renderer);
					renderer.deleteGuis();
					PMS.get().getFoundRenderers().remove(renderer);
					InetAddress ia = renderer.getAddress();
					if (ADDRESS_RENDERER_ASSOCIATION.get(ia) == renderer) {
						ADDRESS_RENDERER_ASSOCIATION.remove(ia);
					}
					String uuid = renderer.getUUID();
					if (uuid != null) {
						if (UUID_RENDERER_ASSOCIATION.get(uuid) == renderer) {
							UUID_RENDERER_ASSOCIATION.remove(uuid);
						}
						if (REACT_CLIENT_RENDERERS.get(uuid) == renderer) {
							REACT_CLIENT_RENDERERS.remove(uuid);
						}
						if (uuid.equals(getUuidOf(ia))) {
							removeUuidOf(ia);
						}
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
		for (Renderer r : getConnectedRenderers()) {
			delete(r, 0);
		}
	}

	public static void resetAllRenderers() {
		for (Renderer r : getConnectedRenderers()) {
			r.setRootFolder(null);
		}
	}

	public static List<Renderer> getInheritors(Renderer renderer) {
		ArrayList<Renderer> renderers = new ArrayList<>();
		RendererConfiguration ref = renderer.getRef();
		for (Renderer connectedRenderer : ConnectedRenderers.getConnectedRenderers()) {
			if (connectedRenderer.getRef() == ref) {
				renderers.add(connectedRenderer);
			}
		}
		return renderers;
	}

	public static void addRendererAssociation(InetAddress sa, Renderer r) {
		// FIXME: handle multiple clients with same ip properly, now newer overwrites older
		Renderer prev = ADDRESS_RENDERER_ASSOCIATION.put(sa, r);
		if (prev != null) {
			// We've displaced a previous renderer at this address, so
			// check  if it's a ghost instance that should be deleted.
			verify(prev);
		}
	}

	public static boolean hasInetAddressForRenderer(Renderer r) {
		return ADDRESS_RENDERER_ASSOCIATION.containsValue(r);
	}

	public static InetAddress getRendererInetAddress(Renderer r) {
		for (Entry<InetAddress, Renderer> entry : ADDRESS_RENDERER_ASSOCIATION.entrySet()) {
			if (entry.getValue() == r) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static void addUuidAssociation(InetAddress ia, String id) {
		if (ia != null && id.startsWith("uuid:")) {
			// FIXME: this assumes one uuid per address
			ADDRESS_UUID_ASSOCIATION.put(ia, id);
		}
	}

	public static String getUuidOf(InetAddress ia) {
		// FIXME: this assumes one uuid per address
		return ia != null ? ADDRESS_UUID_ASSOCIATION.get(ia) : null;
	}

	private static void removeUuidOf(InetAddress ia) {
		if (ia != null) {
			ADDRESS_UUID_ASSOCIATION.remove(ia);
		}
	}

	public static void calculateAllSpeeds() {
		Map<InetAddress, String> values = new HashMap<>();
		for (Entry<InetAddress, Renderer> entry : ADDRESS_RENDERER_ASSOCIATION.entrySet()) {
			InetAddress sa = entry.getKey();
			if (sa.isLoopbackAddress() || sa.isAnyLocalAddress()) {
				continue;
			}
			Renderer r = entry.getValue();
			if (!r.isOffline()) {
				values.put(sa, r.getRendererName());
			}
		}
		for (Map.Entry<InetAddress, String> entry : values.entrySet()) {
			SpeedStats.getSpeedInMBits(entry.getKey(), entry.getValue());
		}
	}

	public static void addWebPlayerRenderer(WebGuiRenderer renderer) {
		REACT_CLIENT_RENDERERS.put(renderer.getUUID(), renderer);
		PMS.get().setRendererFound(renderer);
	}

	public static WebGuiRenderer getWebPlayerRenderer(String uuid) {
		return REACT_CLIENT_RENDERERS.get(uuid);
	}

	public static boolean hasWebPlayerRenderer(String uuid) {
		return REACT_CLIENT_RENDERERS.containsKey(uuid);
	}

	public static void removeWebPlayerRenderer(String uuid) {
		Renderer renderer = REACT_CLIENT_RENDERERS.remove(uuid);
		if (renderer != null) {
			renderer.delete(0);
		}
	}

	public static boolean isValidUUID(String token) {
		try {
			UUID.fromString(token);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static String getRandomUUID() {
		String uuid = UUID.randomUUID().toString();
		while (getRendererByUUID(uuid) != null) {
			uuid = UUID.randomUUID().toString();
		}
		return uuid;
	}

	/**
	 * RendererMap was marking renderer via uuid.
	 * @param uuid
	 * @return Renderer
	 */
	public static void markRenderer(String uuid, int property, Object value) {
		Renderer renderer = UUID_RENDERER_ASSOCIATION.get(uuid);
		switch (property) {
			case JUPnPDeviceHelper.ACTIVE -> renderer.setActive((boolean) value);
			case JUPnPDeviceHelper.RENEW -> renderer.setRenew((boolean) value);
			case JUPnPDeviceHelper.CONTROLS -> renderer.setControls((int) value);
			default -> {
				//not handled
			}
		}
	}

	public static boolean hasUpNPRenderer(String uuid) {
		return (UUID_RENDERER_ASSOCIATION.containsKey(uuid));
	}

	public static Renderer addUpNPRenderer(String uuid, Renderer renderer) {
		return UUID_RENDERER_ASSOCIATION.put(uuid, renderer);
	}

	public static Renderer getUpNPRenderer(String uuid) {
		return UUID_RENDERER_ASSOCIATION.get(uuid);
	}

	/**
	 * RendererMap was creating renderer on the fly if not found.
	 * @param uuid
	 * @return Renderer
	 */
	public static Renderer getOrCreateUpNPRenderer(String uuid) {
		if (!hasUpNPRenderer(uuid)) {
			try {
				addUpNPRenderer(uuid, new Renderer(uuid));
			} catch (InterruptedException | ConfigurationException e) {
				LOGGER.error("Error instantiating item {}: {}", uuid, e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return getUpNPRenderer(uuid);
	}

	public static List<Renderer> getUpNPRenderers(int type) {
		ArrayList<Renderer> renderers = new ArrayList<>();
		for (Renderer r : UUID_RENDERER_ASSOCIATION.values()) {
			if (r.isActive() && r.isControllable(type)) {
				renderers.add(r);
			}
		}
		return renderers;
	}

	public static boolean hasUpNPRenderer(int type) {
		for (Renderer r : UUID_RENDERER_ASSOCIATION.values()) {
			if (r.isControllable(type)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasConnectedAVTransportPlayers() {
		return hasUpNPRenderer(JUPnPDeviceHelper.AVT);
	}

	public static List<Renderer> getConnectedAVTransportPlayers() {
		return getUpNPRenderers(JUPnPDeviceHelper.AVT);
	}

	public static List<Renderer> getConnectedControlPlayers() {
		return ConnectedRenderers.getConnectedRenderers(JUPnPDeviceHelper.ANY);
	}

	public static boolean hasConnectedControlPlayers() {
		return hasConnectedRenderer(JUPnPDeviceHelper.ANY);
	}

	public static boolean hasConnectedRenderer(int type) {
		for (Renderer r : getConnectedRenderers()) {
			if (r.isControllable(type)) {
				return true;
			}
		}
		return false;
	}

	public static List<Renderer> getConnectedRenderers(int type) {
		ArrayList<Renderer> renderers = new ArrayList<>();
		for (Renderer r : getConnectedRenderers()) {
			if (r.isActive() && r.isControllable(type)) {
				renderers.add(r);
			}
		}
		return renderers;
	}

}
