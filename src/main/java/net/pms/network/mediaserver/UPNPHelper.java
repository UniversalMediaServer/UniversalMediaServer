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
package net.pms.network.mediaserver;

import java.net.*;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.dlna.DLNAResource;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.RendererMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPNPHelper extends UPNPControl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPHelper.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final UPNPHelper INSTANCE = new UPNPHelper();

	/**
	 * This utility class is not meant to be instantiated.
	 */
	private UPNPHelper() {
		rendererMap = new RendererMap<>(Renderer.class);
	}

	@Override
	protected void rendererReady(String uuid) {
		Renderer r = ConnectedRenderers.getRendererByUUID(uuid);
		if (r != null) {
			r.getPlayer();
		}
	}

	@Override
	protected boolean isBlocked(String uuid) {
		if (uuid.startsWith("uuid:")) {
			ConnectedRenderers.addUuidAssociation(getAddress(uuid), uuid);
		}
		int mode = RendererConfigurations.getDeviceUpnpMode(uuid);
		if (mode != Renderer.UPNP_ALLOW) {
			LOGGER.debug("Upnp service is {} for {}", Renderer.getUpnpModeString(mode), uuid);
			return true;
		}
		return false;
	}

	@Override
	protected Renderer rendererFound(Device device, String uuid) {
		// Create or retrieve an instance
		try {
			InetAddress socket = InetAddress.getByName(getURL(device).getHost());
			Renderer renderer = ConnectedRenderers.getRendererBySocketAddress(socket);
			RendererConfiguration ref = CONFIGURATION.isRendererForceDefault() ? null :
				RendererConfigurations.getRendererConfigurationByUPNPDetails(getDeviceDetailsString(device));

			if (renderer != null && !renderer.isUpnpAllowed()) {
				LOGGER.debug("Upnp service is {} for \"{}\"", renderer.getUpnpModeString(), renderer);
				return null;
			}

			// FIXME: when UpnpDetailsSearch is missing from the conf a
			// upnp-advertising
			// renderer could register twice if the http server sees it first

			boolean distinct = renderer != null && StringUtils.isNotBlank(renderer.getUUID()) && !uuid.equals(renderer.getUUID());

			if (!distinct && renderer != null && (renderer.matchUPNPDetails(getDeviceDetailsString(device)) || !renderer.isLoaded())) {
				// Already seen by the http server
				if (
					ref != null &&
					!ref.getUpnpDetailsString().equals(renderer.getUpnpDetailsString()) &&
					ref.getLoadingPriority() >= renderer.getLoadingPriority()
				) {
					/*
					 * The upnp-matched reference conf is different from the
					 * previous http-matched conf and has equal or higher
					 * priority, so update.
					 */
					LOGGER.debug("Switching to preferred renderer: " + ref);
					renderer.inherit(ref);
				}

				// Update if we have a custom configuration for this uuid
				renderer.setUUID(uuid);

				// Make sure it's mapped
				rendererMap.put(uuid, "0", renderer);
				Map<String, String> details = getDeviceDetails(device);
				renderer.setDetails(details);
				// Update gui
				renderer.updateRendererGui();
				LOGGER.debug("Found upnp service for \"{}\" with dlna details: {}", renderer, details);
			} else {
				// It's brand new
				renderer = rendererMap.get(uuid, "0");
				if (ref != null) {
					renderer.inherit(ref);
				} else {
					// It's unrecognized: temporarily assign the default
					// renderer but mark it as unloaded
					// so actual recognition can happen later once the http
					// server receives a request.
					// This is to allow initiation of upnp playback before http
					// recognition has occurred.
					renderer.inheritDefault();
					LOGGER.debug("Marking upnp renderer \"{}\" at {} as unrecognized", renderer, socket);
				}
				if (renderer.associateIP(socket)) {
					Map<String, String> details = getDeviceDetails(device);
					renderer.setDetails(details);
					PMS.get().setRendererFound(renderer);
					LOGGER.debug("New renderer found: \"{}\" with dlna details: {}", renderer, details);
				}
			}
			return renderer;
		} catch (UnknownHostException | ConfigurationException e) {
			LOGGER.debug("Error initializing device " + getFriendlyName(device) + ": " + e);
		}
		return null;
	}

	//seems to be unused
	public void addRenderer(Renderer renderer) {
		if (renderer.getUUID() != null) {
			rendererMap.put(renderer.getUUID(), "0", renderer);
		}
	}

	public void removeRenderer(Renderer d) {
		if (d.getUUID() != null) {
			rendererMap.remove(d.getUUID());
		}
	}

	public static UPNPHelper getInstance() {
		return INSTANCE;
	}

	public static boolean activate(String uuid) {
		if (!rendererMap.containsKey(uuid)) {
			LOGGER.debug("Activating upnp service for {}", uuid);
			return getInstance().addRenderer(uuid);
		}
		return true;
	}


	public static List<Renderer> getRenderers(int type) {
		ArrayList<Renderer> renderers = new ArrayList<>();
		for (Map<String, Renderer> item : (Collection<Map<String, Renderer>>) rendererMap.values()) {
			Renderer r = item.get("0");
			if (r.isActive() && r.isControllable(type)) {
				renderers.add(r);
			}
		}
		return renderers;
	}

	//seems to be unused
	public static void play(String uri, String name, Renderer renderer) {
		DLNAResource d = DLNAResource.getValidResource(uri, name, renderer);
		if (d != null) {
			play(d, renderer);
		}
	}

	//seems to be unused
	public static void play(DLNAResource d, Renderer renderer) {
		DLNAResource d1 = d.getParent() == null ? DLNAResource.TEMP.add(d) : d;
		if (d1 != null) {
			Device dev = getDevice(renderer.getUUID());
			String id = renderer.getInstanceID();
			setAVTransportURI(dev, id, d1.getURL(""), renderer.isPushMetadata() ? d1.getDidlString(renderer) : null);
			play(dev, id);
		}
	}

}
