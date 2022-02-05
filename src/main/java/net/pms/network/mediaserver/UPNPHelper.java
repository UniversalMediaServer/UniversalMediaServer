/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.mediaserver;

import java.net.*;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPNPHelper extends UPNPControl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPHelper.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final UPNPHelper INSTANCE = new UPNPHelper();
	private static boolean httpControlHandled;



	/**
	 * This utility class is not meant to be instantiated.
	 */
	private UPNPHelper() {
		rendererMap = new DeviceMap<>(DeviceConfiguration.class);
	}

	@Override
	public void init() {
		if (CONFIGURATION.isUpnpEnabled()) {
			super.init();
		}
		getHttpControlHandler();
	}

	@Override
	protected void rendererReady(String uuid) {
		RendererConfiguration r = RendererConfiguration.getRendererConfigurationByUUID(uuid);
		if (r != null) {
			r.getPlayer();
		}
	}

	@Override
	protected boolean isBlocked(String uuid) {
		int mode = DeviceConfiguration.getDeviceUpnpMode(uuid, true);
		if (mode != RendererConfiguration.ALLOW) {
			LOGGER.debug("Upnp service is {} for {}", RendererConfiguration.getUpnpModeString(mode), uuid);
			return true;
		}
		return false;
	}

	@Override
	protected Renderer rendererFound(Device d, String uuid) {
		// Create or retrieve an instance
		try {
			InetAddress socket = InetAddress.getByName(getURL(d).getHost());
			DeviceConfiguration r = (DeviceConfiguration) RendererConfiguration.getRendererConfigurationBySocketAddress(socket);
			RendererConfiguration ref = CONFIGURATION.isRendererForceDefault() ? null :
				RendererConfiguration.getRendererConfigurationByUPNPDetails(getDeviceDetailsString(d));

			if (r != null && !r.isUpnpAllowed()) {
				LOGGER.debug("Upnp service is {} for \"{}\"", r.getUpnpModeString(), r);
				return null;
			} else if (r == null && ref != null && !ref.isUpnpAllowed()) {
				LOGGER.debug("Upnp service is {} for {} devices", ref.getUpnpModeString(), ref);
				return null;
			}

			// FIXME: when UpnpDetailsSearch is missing from the conf a
			// upnp-advertising
			// renderer could register twice if the http server sees it first

			boolean distinct = r != null && StringUtils.isNotBlank(r.getUUID()) && !uuid.equals(r.getUUID());

			if (!distinct && r != null && (r.matchUPNPDetails(getDeviceDetailsString(d)) || !r.loaded)) {
				// Already seen by the http server
				if (
					ref != null &&
					!ref.getUpnpDetailsString().equals(r.getUpnpDetailsString()) &&
					ref.getLoadingPriority() >= r.getLoadingPriority()
				) {
					/*
					 * The upnp-matched reference conf is different from the
					 * previous http-matched conf and has equal or higher
					 * priority, so update.
					 */
					LOGGER.debug("Switching to preferred renderer: " + ref);
					r.inherit(ref);
				}

				// Update if we have a custom configuration for this uuid
				r.setUUID(uuid);

				// Make sure it's mapped
				rendererMap.put(uuid, "0", r);
				r.details = getDeviceDetails(d);
				// Update gui
				PMS.get().updateRenderer(r);
				LOGGER.debug("Found upnp service for \"{}\" with dlna details: {}", r, r.details);
			} else {
				// It's brand new
				r = (DeviceConfiguration) rendererMap.get(uuid, "0");
				if (ref != null) {
					r.inherit(ref);
				} else {
					// It's unrecognized: temporarily assign the default
					// renderer but mark it as unloaded
					// so actual recognition can happen later once the http
					// server receives a request.
					// This is to allow initiation of upnp playback before http
					// recognition has occurred.
					r.inherit(RendererConfiguration.getDefaultConf());
					r.loaded = false;
					LOGGER.debug("Marking upnp renderer \"{}\" at {} as unrecognized", r, socket);
				}
				if (r.associateIP(socket)) {
					r.details = getDeviceDetails(d);
					PMS.get().setRendererFound(r);
					LOGGER.debug("New renderer found: \"{}\" with dlna details: {}", r, r.details);
				}
			}
			return r;
		} catch (UnknownHostException | ConfigurationException e) {
			LOGGER.debug("Error initializing device " + getFriendlyName(d) + ": " + e);
		}
		return null;
	}

	//seems to be unused
	public void addRenderer(DeviceConfiguration d) {
		if (d.uuid != null) {
			rendererMap.put(d.uuid, "0", d);
		}
	}

	public void removeRenderer(RendererConfiguration d) {
		if (d.uuid != null) {
			rendererMap.remove(d.uuid);
		}
	}

	public static UPNPHelper getInstance() {
		return INSTANCE;
	}

	public static void getHttpControlHandler() {
		if (
			!httpControlHandled &&
			PMS.get().getWebInterfaceServer() != null &&
			!"false".equals(CONFIGURATION.getBumpAddress().toLowerCase())
		) {
			httpControlHandled = true;
			PMS.get().getWebInterfaceServer().setPlayerControlService();
			LOGGER.debug("Attached http player control handler to web player server");
		}
	}

	public static boolean activate(String uuid) {
		if (!rendererMap.containsKey(uuid)) {
			LOGGER.debug("Activating upnp service for {}", uuid);
			return getInstance().addRenderer(uuid);
		}
		return true;
	}


	public static List<RendererConfiguration> getRenderers(int type) {
		ArrayList<RendererConfiguration> renderers = new ArrayList<>();
		for (Map<String, Renderer> item : (Collection<Map<String, Renderer>>) rendererMap.values()) {
			Renderer r = item.get("0");
			if (r.active && (r.controls & type) != 0) {
				renderers.add((RendererConfiguration) r);
			}
		}
		return renderers;
	}

	//seems to be unused
	public static void play(String uri, String name, DeviceConfiguration r) {
		DLNAResource d = DLNAResource.getValidResource(uri, name, r);
		if (d != null) {
			play(d, r);
		}
	}

	//seems to be unused
	public static void play(DLNAResource d, DeviceConfiguration r) {
		DLNAResource d1 = d.getParent() == null ? DLNAResource.TEMP.add(d) : d;
		if (d1 != null) {
			Device dev = getDevice(r.getUUID());
			String id = r.getInstanceID();
			setAVTransportURI(dev, id, d1.getURL(""), r.isPushMetadata() ? d1.getDidlString(r) : null);
			play(dev, id);
		}
	}

}
