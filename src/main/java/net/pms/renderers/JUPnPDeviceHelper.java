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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.controlpoint.UmsSubscriptionCallback;
import net.pms.util.StringUtil;
import net.pms.util.XmlUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JUPnPDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(JUPnPDeviceHelper.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	public static final DeviceType[] MEDIA_RENDERER_TYPES = new DeviceType[]{
		new UDADeviceType("MediaRenderer", 1),
		// Older Sony Blurays provide only 'Basic' service
		new UDADeviceType("Basic", 1)
	};

	public static final int ACTIVE = 0;
	public static final int CONTROLS = 1;
	public static final int RENEW = 2;
	public static final int AVT = Renderer.PLAYCONTROL;
	public static final int RC = Renderer.VOLUMECONTROL;
	public static final int ANY = 0xff;

	private static final boolean DEBUG = true; // log upnp state vars

	// AVTransport
	// Play modes
	public static final String NORMAL = "NORMAL";
	public static final String REPEAT_ONE = "REPEAT_ONE";
	public static final String REPEAT_ALL = "REPEAT_ALL";
	public static final String RANDOM = "RANDOM";
	// Seek modes
	public static final String REL_BYTE = "X_DLNA_REL_BYTE";
	public static final String REL_TIME = "REL_TIME";
	public static final String TRACK_NR = "TRACK_NR";

	private static final String AV_TRANSPORT_SERVICE = "AVTransport";
	private static final String RENDERING_CONTROL_SERVICE = "RenderingControl";

	// RenderingControl
	// Audio channels
	public static final String MASTER = "Master";
	public static final String LF = "LF";
	public static final String RF = "RF";


	/**
	 * List of ignored devices (non-Renderers) from the network infrastructure
	 * e.g. gateways, routers, printers etc.
	 */
	protected static ArrayList<RemoteDevice> ignoredDevices = new ArrayList<>();

	private static DocumentBuilder db;


	public static void remoteDeviceAdded(RemoteDevice device) {
		if (isBlocked(getUUID(device)) || !addRenderer(device)) {
			LOGGER.trace("Ignoring remote device: {} {}", device.getType().getType(), device);
			addIgnoredDeviceToList(device);
		}
		// This may be unnecessary, but we might as well be thorough
		if (device.hasEmbeddedDevices()) {
			for (Device<?, RemoteDevice, ?> embedded : device.getEmbeddedDevices()) {
				if (isBlocked(getUUID(embedded)) || !addRenderer(embedded)) {
					LOGGER.trace("Ignoring embedded device: {} {}", embedded.getType(), embedded.toString());
					addIgnoredDeviceToList((RemoteDevice) embedded);
				}
			}
		}
	}

	public static void remoteDeviceUpdated(RemoteDevice d) {
		rendererUpdated(d);
	}

	public static void remoteDeviceRemoved(RemoteDevice d) {
		String uuid = getUUID(d);
		if (ConnectedRenderers.hasUpNPRenderer(uuid)) {
			ConnectedRenderers.markRenderer(uuid, ACTIVE, false);
			LOGGER.debug("Renderer {} is now offline.", getFriendlyName(d));
		}
	}

	public static void markRenderer(String uuid, int property, Object value) {
		ConnectedRenderers.markRenderer(uuid, property, value);
	}

	public static boolean isUpnpDevice(String uuid) {
		return getDevice(uuid) != null;
	}

	public static boolean isActive(String uuid) {
		if (ConnectedRenderers.hasUpNPRenderer(uuid)) {
			return ConnectedRenderers.getUpNPRenderer(uuid).isActive();
		}
		return false;
	}

	public static boolean isUpnpControllable(String uuid) {
		if (ConnectedRenderers.hasUpNPRenderer(uuid)) {
			return ConnectedRenderers.getUpNPRenderer(uuid).isControllable();
		}
		return false;
	}

	public static String getFriendlyName(String uuid) {
		return getFriendlyName(getDevice(uuid));
	}

	public static boolean activate(String uuid) {
		if (!ConnectedRenderers.hasUpNPRenderer(uuid)) {
			LOGGER.debug("Activating upnp service for {}", uuid);
			return addRenderer(uuid);
		}
		return true;
	}

	public static boolean isNonRenderer(InetAddress socket) {
		Device d = getDevice(socket);
		if (d != null && !isMediaRenderer(d)) {
			LOGGER.debug("Device at {} is {}: {}", socket, d.getType(), d.toString());
			return true;
		}
		return false;
	}

	public static ActionArgumentValue[] getPositionInfo(Renderer renderer) {
		Device dev = getDevice(renderer.getUUID());
		if (dev == null) {
			return null;
		}
		ActionInvocation invocation = send(dev, renderer, AV_TRANSPORT_SERVICE, "GetPositionInfo");
		return invocation == null ? null : invocation.getOutput();
	}

	public static InetAddress getAddress(String uuid) {
		try {
			Device device = getDevice(uuid);
			if (device != null) {
				return InetAddress.getByName(getURL(device).getHost());
			}
		} catch (UnknownHostException e) {
		}
		return null;
	}

	private static boolean addRenderer(String uuid) {
		Device device = getDevice(uuid);
		return (device != null && addRenderer(device));
	}

	private static synchronized boolean addRenderer(Device<?, RemoteDevice, ?> device) {
		if (device != null) {
			String uuid = getUUID(device);
			if (isMediaRenderer(device)) {
				Renderer renderer = rendererFound(device, uuid);
				if (renderer != null) {
					LOGGER.debug("Adding device: {} {}", device.getType(), device.toString());
					subscribeAll(device, renderer);
					getProtocolInfo(device);
					renderer.setActive(true);
					renderer.getPlayer();
					return true;
				}
			}
		}
		return false;
	}

	private static Renderer rendererFound(Device device, String uuid) {
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
				ConnectedRenderers.addUpNPRenderer(uuid, renderer);
				Map<String, String> details = getDeviceDetails(device);
				renderer.setDetails(details);
				// Update gui
				renderer.updateRendererGui();
				LOGGER.debug("Found upnp service for \"{}\" with dlna details: {}", renderer, details);
			} else {
				// It's brand new
				renderer = ConnectedRenderers.getOrCreateUpNPRenderer(uuid);
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

	private static boolean isBlocked(String uuid) {
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

	private static void subscribeAll(Device d, Renderer renderer) {
		String name = getFriendlyName(d);
		int ctrl = 0;
		for (Service s : d.getServices()) {
			String sid = s.getServiceId().getId();
			LOGGER.debug("Subscribing to " + sid + " service on " + name);
			if (sid.contains(AV_TRANSPORT_SERVICE)) {
				ctrl |= AVT;
			} else if (sid.contains(RENDERING_CONTROL_SERVICE)) {
				ctrl |= RC;
			}
			MediaServer.upnpService.getControlPoint().execute(new UmsSubscriptionCallback(s));
		}
		renderer.setRenew(false);
		renderer.setControls(ctrl);
	}

	private static void rendererUpdated(Device d) {
		String uuid = getUUID(d);
		if (ConnectedRenderers.hasUpNPRenderer(uuid)) {
			Renderer renderer = ConnectedRenderers.getUpNPRenderer(uuid);
			if (renderer.needsRenewal()) {
				LOGGER.debug("Renewing subscriptions to ", getFriendlyName(d));
				subscribeAll(d, renderer);
			}
			renderer.setActive(true);
		}
	}

	/**
	 * Returns the first renderer at the given address, if any.
	 *
	 * @param socket address of the checked remote device.
	 * @return Device
	 */
	private static Device getDevice(InetAddress socket) {
		if (MediaServer.upnpService != null) {
			for (DeviceType r : MEDIA_RENDERER_TYPES) {
				for (Device d : MediaServer.upnpService.getRegistry().getDevices(r)) {
					try {
						InetAddress devsocket = InetAddress.getByName(getURL(d).getHost());
						if (devsocket.equals(socket)) {
							return d;
						}
					} catch (UnknownHostException e) {
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get the registered device root or embedded with the requested UUID
	 *
	 * @param uuid the UUID of the device to be checked.
	 * @return the device registered in the UpnpService.Registry, null otherwise
	 */
	public static Device getDevice(String uuid) {
		return uuid != null && MediaServer.upnpService != null ? MediaServer.upnpService.getRegistry().getDevice(UDN.valueOf(uuid), false) : null;
	}

	/**
	 * Returns the first device regardless of type at the given address, if any.
	 * Seems not used.
	 *
	 * @param socket address of the checked remote device.
	 * @return Device
	 */
	public static Device getAnyDevice(InetAddress socket) {
		if (MediaServer.upnpService != null) {
			for (Device d : MediaServer.upnpService.getRegistry().getDevices()) {
				try {
					InetAddress devsocket = InetAddress.getByName(getURL(d).getHost());
					if (devsocket.equals(socket)) {
						return d;
					}
				} catch (UnknownHostException e) {
				}
			}
		}
		return null;
	}

	private static URL getURL(Device device) {
		if (device instanceof RemoteDevice remoteDevice) {
			return remoteDevice.getIdentity().getDescriptorURL();
		} else if (device != null) {
			return device.getDetails().getBaseURL();
		}
		return null;
	}

	public static List<String> getServiceNames(String uuid) {
		if (isUpnpDevice(uuid)) {
			return getServiceNames(getDevice(uuid));
		}
		return null;
	}

	private static List<String> getServiceNames(Device d) {
		ArrayList<String> services = new ArrayList<>();
		for (Service s : d.getServices()) {
			services.add(s.getServiceId().getId());
		}
		return services;
	}

	public static Map<String, String> getDeviceDetails(String uuid) {
		if (isUpnpDevice(uuid)) {
			return getDeviceDetails(getDevice(uuid));
		}
		return null;
	}

	private static Map<String, String> getDeviceDetails(Device d) {
		if (d == null) {
			return null;
		}
		DeviceDetails dev = d.getDetails();
		ManufacturerDetails man = dev.getManufacturerDetails();
		ModelDetails model = dev.getModelDetails();
		LinkedHashMap<String, String> details = new LinkedHashMap<>();
		details.put("friendlyName", dev.getFriendlyName());
		details.put("address", getURL(d).getHost());
		details.put("udn", getUUID(d));
		Object detail;
		detail = man.getManufacturer();
		if (detail != null) {
			details.put("manufacturer", (String) detail);
		}
		detail = model.getModelName();
		if (detail != null) {
			details.put("modelName", (String) detail);
		}
		detail = model.getModelNumber();
		if (detail != null) {
			details.put("modelNumber", (String) detail);
		}
		detail = model.getModelDescription();
		if (detail != null) {
			details.put("modelDescription", (String) detail);
		}
		detail = man.getManufacturerURI();
		if (detail != null) {
			details.put("manufacturerURL", detail.toString());
		}
		detail = model.getModelURI();
		if (detail != null) {
			details.put("modelURL", detail.toString());
		}
		return details;
	}

	private static String getDeviceDetailsString(Device d) {
		return StringUtils.join(getDeviceDetails(d).values(), " ");
	}

	public static String getDeviceIcon(String uuid, int maxHeight) {
		if (isUpnpDevice(uuid)) {
			return getDeviceIcon(getDevice(uuid), maxHeight);
		}
		return null;
	}

	private static String getDeviceIcon(Device d, int maxHeight) {
		URL base = getURL(d);
		Icon icon = null;
		String url = null;
		int maxH = maxHeight == 0 ? 99999 : maxHeight;
		int height = 0;
		for (Icon i : d.getIcons()) {
			int h = i.getHeight();
			if (h < maxH && h > height) {
				icon = i;
				height = h;
			}
		}
		try {
			url = icon != null ? new URL(base, icon.getUri().toString()).toString() : null;
		} catch (MalformedURLException e) {
		}
		LOGGER.debug("Device icon: " + url);
		return url;
	}

	private static boolean isMediaRenderer(Device d) {
		String t = d.getType().getType();
		for (DeviceType r : MEDIA_RENDERER_TYPES) {
			if (r.getType().equals(t)) {
				return true;
			}
		}
		return false;
	}

	private static String getFriendlyName(Device d) {
		return d != null ? d.getDetails().getFriendlyName() : null;
	}

	/**
	 * seems not used.
	 */
	public static String getUUID(InetAddress socket) {
		Device d = getDevice(socket);
		if (d != null) {
			return getUUID(d);
		}
		return null;
	}

	private static String getUUID(Device d) {
		return d.getIdentity().getUdn().toString();
	}

	/**
	 * Add device to the list of ignored devices when not exists on the list.
	 *
	 * @param device The device to add to the list.
	 */
	private static void addIgnoredDeviceToList(RemoteDevice device) {
		if (!ignoredDevices.contains(device)) {
			ignoredDevices.add(device);
//			LOGGER.trace("This device was added to the list of ignored devices.");
//		} else {
//			LOGGER.trace("This device is in the list of ignored devices so not be added.");
		}
	}

	// ConnectionManager
	private static void getProtocolInfo(Device<?, RemoteDevice, ?> device) {
		Service<RemoteDevice, RemoteService> connectionManager = device.findService(ServiceId.valueOf("ConnectionManager"));
		if (connectionManager != null) {
			Action<RemoteService> action = connectionManager.getAction("GetProtocolInfo");
			final String name = getFriendlyName(device);
			if (action != null) {
				final String uuid = getUUID(device);
				ActionInvocation<RemoteService> actionInvocation = new ActionInvocation<>(action);

				new ActionCallback(actionInvocation, MediaServer.upnpService.getControlPoint()) {
					@Override
					public void success(ActionInvocation invocation) {
						Map<String, ActionArgumentValue<RemoteService>> outputs = invocation.getOutputMap();
						ActionArgumentValue<RemoteService> sink = outputs.get("Sink");
						if (sink != null && ConnectedRenderers.hasUpNPRenderer(uuid)) {
							ConnectedRenderers.getUpNPRenderer(uuid).deviceProtocolInfo.add(DeviceProtocolInfo.GET_PROTOCOLINFO_SINK, sink.toString());
						}
						if (LOGGER.isTraceEnabled()) {
							StringBuilder sb = new StringBuilder();
							for (Map.Entry<String, ActionArgumentValue<RemoteService>> entry : outputs.entrySet()) {
								if (entry.getValue() != null) {
									String value = entry.getValue().toString();
									if (StringUtils.isNotBlank(value)) {
										sb.append("\n").append(entry.getKey()).append(":\n  ");
										sb.append(value.replace(",", "\n  "));
									}
								}
							}
							if (sb.length() > 0) {
								LOGGER.trace("Received GetProtocolInfo from \"{}\": {}", name, sb.toString());
							} else {
								LOGGER.trace("Received empty reply to GetProtocolInfo from \"{}\"", name);
							}
						}
					}

					@Override
					public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
						LOGGER.debug(
							"GetProtocolInfo from \"{}\" failed with status code {}: {} ({})",
							name,
							operation.getStatusCode(),
							operation.getStatusMessage(),
							defaultMsg
						);
					}
				}.run();
			}
		}
	}

	private static ActionInvocation send(final Device dev, String service, final String action, String... args) {
		return send(dev, null, service, action, args);
	}

	// Convenience functions for sending various upnp service requests
	private static ActionInvocation send(final Device dev, Renderer renderer, String service, final String action, String... args) {
		Service svc = dev.findService(ServiceId.valueOf("urn:upnp-org:serviceId:" + service));
		final String uuid = getUUID(dev);
		if (svc != null) {
			Action x = svc.getAction(action);
			String name = getFriendlyName(dev);
			// Don't spam the log with the GetPositionInfo because it is not important.
			// The UMS is using it only to show the current state of the media playing.
			boolean isNotGetPositionInfoRequest = !action.equals("GetPositionInfo");

			if (x != null) {
				ActionInvocation a = new ActionInvocation(x);
				a.setInput(Renderer.INSTANCE_ID, "0");
				for (int i = 0; i < args.length; i += 2) {
					a.setInput(args[i], args[i + 1]);
				}
				if (isNotGetPositionInfoRequest) {
					LOGGER.debug("Sending upnp {}.{} {} to {}", service, action, args, name);
				}

				new ActionCallback(a, MediaServer.upnpService.getControlPoint()) {
					@Override
					public void success(ActionInvocation invocation) {
						ConnectedRenderers.markRenderer(uuid, ACTIVE, true);
					}

					@Override
					public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
						if (isNotGetPositionInfoRequest) { // Don't show the error in GetPositionInfo
							LOGGER.error("Failed to send action \"{}\" to {}: {}", action, dev.getDetails().getFriendlyName(), defaultMsg);
							if (LOGGER.isTraceEnabled() && invocation != null && invocation.getFailure() != null) {
								LOGGER.trace("", invocation.getFailure());
							}

							// Mark the renderer false when there is an error except
							// the GetPositionInfo failure. It could be wrong
							// implementation in the renderer.
							ConnectedRenderers.markRenderer(uuid, ACTIVE, false);
						} else if (renderer != null && renderer.isGetPositionInfoImplemented) {
							if (invocation.getFailure().getErrorCode() == (int) 501) { // renderer returns that GetPositionInfo is not implemented.
								renderer.isGetPositionInfoImplemented = false;
								LOGGER.info("The renderer {} returns that the GetPositionInfo is not implemented. The UMS disabled this feature.", renderer);
							} else { // failure is not clear so check the renderer GetPositionInfo capability three times before disable it.
								renderer.countGetPositionRequests++;
								if (renderer.countGetPositionRequests > 2) {
									renderer.isGetPositionInfoImplemented = false;
									LOGGER.info("The GetPositionInfo seems to be not properly implemented in the {}. The UMS disabled this feature.", renderer);
								}
							}
						}
					}
				}.run();

				if (isNotGetPositionInfoRequest) {
					for (ActionArgumentValue arg : a.getOutput()) {
						LOGGER.debug("Received from {}: {}={}", name, arg.getArgument().getName(), arg.toString());
					}
				}
				return a;
			}
		} else {
			LOGGER.warn(
				"Couldn't find UPnP service {} for device {} when trying perform action {}",
				service,
				dev.getDetails().getFriendlyName(),
				action
			);
		}
		return null;
	}

	// AVTransport Service
	public static void play(Device dev) {
		send(dev, AV_TRANSPORT_SERVICE, "Play", "Speed", "1");
	}

	/**
	 * Seems not used.
	 */
	public static void play(String uri, String name, Renderer renderer) {
		DLNAResource d = DLNAResource.getValidResource(uri, name, renderer);
		if (d != null) {
			play(d, renderer);
		}
	}

	/**
	 * Seems not used.
	 */
	public static void play(DLNAResource d, Renderer renderer) {
		DLNAResource d1 = d.getParent() == null ? DLNAResource.TEMP.add(d) : d;
		if (d1 != null) {
			Device dev = getDevice(renderer.getUUID());
			setAVTransportURI(dev, d1.getURL(""), renderer.isPushMetadata() ? d1.getDidlString(renderer) : null);
			play(dev);
		}
	}

	public static void pause(Device dev) {
		send(dev, AV_TRANSPORT_SERVICE, "Pause");
	}

	public static void next(Device dev) {
		send(dev, AV_TRANSPORT_SERVICE, "Next");
	}

	public static void previous(Device dev) {
		send(dev, AV_TRANSPORT_SERVICE, "Previous");
	}

	public static void seek(Device dev, String mode, String target) {
		// REL_TIME target format is "hh:mm:ss"
		send(dev, AV_TRANSPORT_SERVICE, "Seek", "Unit", mode, "Target", target);
	}

	public static void stop(Device dev) {
		send(dev, AV_TRANSPORT_SERVICE, "Stop");
	}

	/**
	 * Seems not used.
	 */
	public static String getCurrentTransportState(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetTransportInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("CurrentTransportState");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getCurrentTransportActions(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetCurrentTransportActions");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("CurrentTransportActions");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getDeviceCapabilities(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetDeviceCapabilities");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("DeviceCapabilities");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getMediaInfo(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetMediaInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("MediaInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getTransportInfo(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetTransportInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("TransportInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getTransportSettings(Device dev) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "GetTransportSettings");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("TransportSettings");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static void setAVTransportURI(Device dev, String uri, String metaData) {
		send(dev, AV_TRANSPORT_SERVICE, "SetAVTransportURI", "CurrentURI", uri,
			"CurrentURIMetaData", metaData != null ? StringUtil.unEncodeXML(metaData) : null);
	}

	/**
	 * Seems not used.
	 */
	public static void setPlayMode(Device dev, String mode) {
		send(dev, AV_TRANSPORT_SERVICE, "SetPlayMode", "NewPlayMode", mode);
	}

	/**
	 * Seems not used.
	 */
	public static String xDlnaGetBytePositionInfo(Device dev, String trackSize) {
		ActionInvocation invocation = send(dev, AV_TRANSPORT_SERVICE, "X_DLNA_GetBytePositionInfo", "TrackSize", trackSize);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("BytePositionInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	// RenderingControl
	// Audio channels

	/**
	 * Seems not used.
	 */
	public static String getMute(Device dev) {
		return getMute(dev, MASTER);
	}

	/**
	 * Seems not used.
	 */
	public static String getMute(Device dev, String channel) {
		ActionInvocation invocation = send(dev, RENDERING_CONTROL_SERVICE, "GetMute", "Channel", channel);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("Mute");
		return argumentValue == null ? null : argumentValue.toString();
	}

	/**
	 * Seems not used.
	 */
	public static String getVolume(Device dev) {
		return getVolume(dev, MASTER);
	}

	/**
	 * Seems not used.
	 */
	public static String getVolume(Device dev, String channel) {
		ActionInvocation invocation = send(dev, RENDERING_CONTROL_SERVICE, "GetVolume", "Channel", channel);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("Volume");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static void setMute(Device dev, boolean on) {
		setMute(dev, on, MASTER);
	}

	public static void setMute(Device dev, boolean on, String channel) {
		send(dev, RENDERING_CONTROL_SERVICE, "SetMute", "DesiredMute", on ? "1" : "0", "Channel", channel);
	}

	public static void setVolume(Device dev, int volume) {
		setVolume(dev, volume, MASTER);
	}

	public static void setVolume(Device dev, int volume, String channel) {
		// volume = 1 to 100
		send(dev, RENDERING_CONTROL_SERVICE, "SetVolume", "DesiredVolume", String.valueOf(volume), "Channel", channel);
	}

	/**
	 * Check if the uuid is NOT ignored device.
	 * Seems not used : ignoredDevices only added.
	 *
	 * @param uuid The uuid to check.
	 * @return True when uuid device is NOT on the list of ignored
	 *         devices, false otherwise.
	 */
	public static boolean isNotIgnoredDevice(String uuid) {
		if (ignoredDevices != null) {
			UDN udn = UDN.valueOf(uuid);
			for (RemoteDevice rd : ignoredDevices) {
				if (rd.findDevice(udn) != null) {
					return false;
				}
			}
		}
		return true;
	}

	public static synchronized void xml2d(String uuid, String xml, Renderer item) {
		if (StringUtils.isBlank(xml)) {
			return;
		}
		try {
			if (db == null) {
				try {
					db = XmlUtils.xxeDisabledDocumentBuilderFactory().newDocumentBuilder();
				} catch (ParserConfigurationException ex) {
					LOGGER.debug("Error creating xml2d parser " + ex);
					return;
				}
			}
			Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
//			doc.getDocumentElement().normalize();
			NodeList ids = doc.getElementsByTagName(Renderer.INSTANCE_ID);
			int idsLength = ids.getLength();
			for (int i = 0; i < idsLength; i++) {
				NodeList c = ids.item(i).getChildNodes();
				String id = ((Element) ids.item(i)).getAttribute("val");
//				if (DEBUG) LOGGER.debug("InstanceID: " + id);
				if (item == null) {
					item = ConnectedRenderers.getUpNPRenderer(uuid);
				}
				item.data.put(Renderer.INSTANCE_ID, id);
				for (int n = 0; n < c.getLength(); n++) {
					if (c.item(n).getNodeType() != Node.ELEMENT_NODE) {
//						LOGGER.debug("skip this " + c.item(n));
						continue;
					}
					Element e = (Element) c.item(n);
					String name = e.getTagName();
					String val = e.getAttribute("val");
					if (DEBUG) {
						LOGGER.debug(name + ": " + val);
					}
					item.data.put(name, val);
				}
				item.alert();
			}
		} catch (IOException | SAXException e) {
			LOGGER.debug("Error parsing xml: " + e);
		}
	}

}
