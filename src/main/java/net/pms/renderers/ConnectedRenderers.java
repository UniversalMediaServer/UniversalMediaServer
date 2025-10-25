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
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.SpeedStats;
import net.pms.renderers.devices.WebGuiRenderer;
import net.pms.util.SortedHeaderMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handle all renderers and devices found.
 */
public class ConnectedRenderers {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedRenderers.class);
	private static final Map<InetAddress, Renderer> ADDRESS_RENDERER_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, InetAddress> UUID_ADDRESS_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, WebGuiRenderer> REACT_CLIENT_RENDERERS = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, Renderer> UUID_RENDERER_ASSOCIATION = Collections.synchronizedMap(new HashMap<>());
	// Used to filter out known headers when the renderer is not recognized
	private static final String[] KNOWN_HEADERS = {
		"accept",
		"accept-language",
		"accept-encoding",
		"callback",
		"connection",
		"content-length",
		"content-type",
		"date",
		"host",
		"nt",
		"sid",
		"timeout",
		"user-agent"
	};
	/**
	 * A lock to prevent multiple renderer creation.
	 */
	public static final Lock RENDERER_LOCK = new ReentrantLock();

	/**
	 * This class is not meant to be instantiated.
	 */
	private ConnectedRenderers() {
	}

	/**
	 * The handler makes a couple of attempts to recognize a renderer from its
	 * requests.
	 *
	 * IP address matches from previous requests are preferred, then upnp uuid
	 * is checked, when that fails request header matches are attempted and if
	 * those fail as well we're stuck with the default renderer.
	 *
	 * @param ia
	 * @param userAgentString
	 * @param headers
	 * @return
	 */
	public static Renderer getRenderer(InetAddress ia, String userAgentString, Collection<Map.Entry<String, String>> headers) {
		Renderer renderer = null;
		RENDERER_LOCK.lock();
		try {
			// Attempt 1: try to recognize the renderer by its socket address from previous requests
			renderer = getRendererBySocketAddress(ia);

			// If the renderer exists but isn't marked as loaded it means it's unrecognized
			// by upnp and we still need to attempt http recognition here.
			if (renderer == null || !renderer.isLoaded()) {
				// Attempt 2: try to recognize the renderer by matching headers
				renderer = getRendererConfigurationByHeaders(headers, ia);
			}

			// Still no media renderer recognized?
			if (renderer == null) {
				// Attempt 3: Not really an attempt; all other attempts to recognize
				// the renderer have failed. The only option left is to assume the
				// default renderer.
				renderer = resolve(ia, null);
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				if (renderer != null) {
					LOGGER.debug("Using default media renderer \"{}\"", renderer.getConfName());
					if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
						// We have found an unknown renderer
						List<String> identifiers = getIdentifiers(userAgentString, headers);
						renderer.setIdentifiers(identifiers);
						LOGGER.info(
								"Media renderer was not recognized. Possible identifying HTTP headers:\n{}",
								StringUtils.join(identifiers, "\n")
						);
						PMS.get().setRendererFound(renderer);
					}
				}
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Recognized media renderer \"{}\"", renderer.getRendererName());
			}
		} finally {
			RENDERER_LOCK.unlock();
		}
		return renderer;
	}

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

	private static List<String> getIdentifiers(String userAgentString, Collection<Map.Entry<String, String>> headers) {
		List<String> identifiers = new ArrayList<>();
		identifiers.add("User-Agent: " + userAgentString);
		for (Map.Entry<String, String> header : headers) {
			boolean isKnown = false;

			// Try to match known headers.
			String headerName = header.getKey().toLowerCase();
			for (String knownHeaderString : KNOWN_HEADERS) {
				if (headerName.startsWith(knownHeaderString)) {
					isKnown = true;
					break;
				}
			}
			if (!isKnown) {
				// Truly unknown header, therefore interesting.
				identifiers.add(header.getKey() + ": " + header.getValue());
			}
		}
		return identifiers;
	}

	/**
	 * Tries to find a matching renderer configuration based on the given
	 * collection of request headers
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

	public static Renderer getRendererBySocketAddress(InetAddress sa) {
		Renderer r = ADDRESS_RENDERER_ASSOCIATION.get(sa);
		if (r != null) {
			LOGGER.trace("Matched media renderer \"{}\" based on address {}", r.getRendererName(), sa.getHostAddress());
		}
		return r;
	}

	public static Renderer getRendererByUUID(String uuid) {
		for (Renderer renderer : getConnectedRenderers()) {
			if (uuid.equalsIgnoreCase(renderer.getUUID())) {
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
				if (JUPnPDeviceHelper.getDevice(ia) == null) {
					//UPnP device not yet discovered, device just started ?
					LOGGER.debug("Sending UPnP search for newly created renderer: {}", renderer);
					JUPnPDeviceHelper.searchMediaRendererDevices();
				} else if (renderer.isUpnpPostponed()) {
					renderer.setUpnpMode(Renderer.UPNP_ALLOW);
				}
			}
		} catch (ConfigurationException e) {
			LOGGER.error("Configuration error while resolving renderer: {}", e.getMessage());
			LOGGER.trace("", e);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted while resolving renderer \"{}\": {}", ia, e.getMessage());
			Thread.currentThread().interrupt();
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
		if (!r.verify()) {
			LOGGER.debug("Purging renderer {} as invalid", r);
			delete(r, 0);
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
					renderer.clearMediaStore();
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
			r.resetMediaStore();
		}
	}

	public static List<Renderer> getInheritors(Renderer renderer) {
		ArrayList<Renderer> renderers = new ArrayList<>();
		RendererConfiguration ref = renderer.getRef();
		for (Renderer connectedRenderer : getConnectedRenderers()) {
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
		if (ia != null && id != null) {
			UUID_ADDRESS_ASSOCIATION.put(id, ia);
		}
	}

	public static String getUuidOf(InetAddress ia) {
		if (ia != null) {
			Optional<Map.Entry<String, InetAddress>> res = UUID_ADDRESS_ASSOCIATION.entrySet().stream().filter(entry -> ia.equals(entry.getValue())).findFirst();
			if (res.isPresent()) {
				return res.get().getKey();
			}
		}
		return null;
	}

	private static void removeUuidOf(InetAddress ia) {
		if (ia != null) {
			while (UUID_ADDRESS_ASSOCIATION.values().remove(ia)) {
				//it will end by itself
			}
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
			delete(renderer, 0);
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
	 *
	 * @param uuid
	 * @return Renderer
	 */
	public static void markUpnpRenderer(String uuid, int property, Object value) {
		Renderer renderer = UUID_RENDERER_ASSOCIATION.get(uuid);
		switch (property) {
			case JUPnPDeviceHelper.ACTIVE ->
				renderer.setActive((boolean) value);
			case JUPnPDeviceHelper.RENEW ->
				renderer.setRenew((boolean) value);
			case JUPnPDeviceHelper.CONTROLS ->
				renderer.setControls((int) value);
			default -> {
				//not handled
			}
		}
	}

	public static boolean hasUuidRenderer(String uuid) {
		return (UUID_RENDERER_ASSOCIATION.containsKey(uuid));
	}

	public static Renderer addUuidRenderer(String uuid, Renderer renderer) {
		return UUID_RENDERER_ASSOCIATION.put(uuid, renderer);
	}

	public static Renderer getUuidRenderer(String uuid) {
		return UUID_RENDERER_ASSOCIATION.get(uuid);
	}

	/**
	 * RendererMap was creating renderer on the fly if not found.
	 *
	 * @param uuid
	 * @return Renderer
	 */
	public static Renderer getOrCreateUuidRenderer(String uuid) {
		if (!hasUuidRenderer(uuid)) {
			try {
				addUuidRenderer(uuid, new Renderer(uuid));
			} catch (ConfigurationException e) {
				LOGGER.error("Error instantiating item {}: {}", uuid, e.getMessage());
				LOGGER.trace("", e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return getUuidRenderer(uuid);
	}

	public static List<Renderer> getUPnPRenderers(int type) {
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
		return getUPnPRenderers(JUPnPDeviceHelper.AVT);
	}

	public static List<Renderer> getConnectedControlPlayers() {
		return getConnectedRenderers(JUPnPDeviceHelper.ANY);
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
