package net.pms.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.pms.PMS;
import static net.pms.network.UPNPHelper.sleep;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.ServerClientTokens;
import org.fourthline.cling.model.action.*;
import org.fourthline.cling.model.gena.*;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.DeviceTypeHeader;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UPNPControl {
	// Logger ids to write messages to the logs.
	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPControl.class);

	public static final DeviceType[] mediaRendererTypes = new DeviceType[] {
		new UDADeviceType("MediaRenderer", 1),
		// Older Sony Blurays provide only 'Basic' service
		new UDADeviceType("Basic", 1)
	};

	private static UpnpService upnpService;
	private static UpnpHeaders UMSHeaders;
	private static DocumentBuilder db;

	public static final int ACTIVE = 0;
	public static final int CONTROLS = 1;
	public static final int RENEW = 2;
	public static final int AVT = BasicPlayer.PLAYCONTROL;
	public static final int RC = BasicPlayer.VOLUMECONTROL;
	public static final int ANY = 0xff;

	private static final boolean DEBUG = true; // log upnp state vars

	protected static Map<String, Renderer> socketMap = new HashMap<>();

	public static class DeviceMap<T extends Renderer> extends HashMap<String, HashMap<String, T>> {
		private static final long serialVersionUID = 1510675619549915489L;

		private Class<T> TClass;

		public DeviceMap(Class<T> t) {
			TClass = t;
		}

		public T get(String uuid, String id) {
			if (!containsKey(uuid)) {
				put(uuid, new HashMap<String, T>());
			}
			HashMap<String, T> m = get(uuid);
			if (!m.containsKey(id)) {
				try {
					T newitem = TClass.newInstance();
					newitem.uuid = uuid;
					m.put(id, newitem);
				} catch (Exception e) {
					LOGGER.error("Error instantiating item {}[{}]: {}", uuid, id, e.getMessage());
					LOGGER.trace("", e);
				}
			}
			return m.get(id);
		}

		public String get(String uuid, String id, String key) {
			return get(uuid, id).data.get(key);
		}

		public boolean containsKey(String uuid, String id) {
			return containsKey(uuid) && get(uuid).containsKey(id);
		}

		public HashMap<String, String> getData(String uuid, String id) {
			if (containsKey(uuid, id)) {
				return get(uuid, id).data;
			}
			return null;
		}

		public T put(String uuid, String id, T item) {
			item.uuid = uuid;
			if (!containsKey(uuid)) {
				get(uuid, "0");
			}
			return get(uuid).put(id, item);
		}

		public String put(String uuid, String id, String key, String value) {
			return get(uuid, id).data.put(key, value);
		}

		public void mark(String uuid, int property, Object value) {
			HashMap<String, T> m = get(uuid);
			if (m != null) {
				for (T i : m.values()) {
					switch (property) {
						case ACTIVE:
							i.setActive((boolean) value);
							break;
						case RENEW:
							i.renew = (boolean) value;
							break;
						case CONTROLS:
							i.controls = (int) value;
							break;
						default:
							break;
					}
				}
			}
		}
	}
	protected static DeviceMap rendererMap;

	public static class Renderer {
		public int controls;
		protected ActionEvent event;
		public String uuid;
		public String instanceID = "0"; // FIXME: unclear in what precise context a media renderer's instanceID != 0
		public final HashMap<String, String> data;
		public Map<String, String> details;
		public LinkedHashSet<ActionListener> listeners;
		private Thread monitor;
		public volatile boolean active, renew;
		public final DeviceProtocolInfo deviceProtocolInfo = new DeviceProtocolInfo();
		public volatile PanasonicDmpProfiles panasonicDmpProfiles;

		public Renderer(String uuid) {
			this();
			this.uuid = uuid;
		}

		public Renderer() {
			controls = 0;
			active = false;
			data = new HashMap<>();
			details = null;
			listeners = new LinkedHashSet<>();
			event = new ActionEvent(this, 0, null);
			monitor = null;
			renew = false;
			data.put("TransportState", "STOPPED");
		}

		public void alert() {
			if (isUpnpDevice(uuid) && (monitor == null || !monitor.isAlive()) && !"STOPPED".equals(data.get("TransportState"))) {
				monitor();
			}
			for (ActionListener l : listeners) {
				l.actionPerformed(event);
			}
		}

		public Map<String, String> connect(ActionListener listener) {
			listeners.add(listener);
			return data;
		}

		public void disconnect(ActionListener listener) {
			listeners.remove(listener);
		}

		public void monitor() {
			final Device d = getDevice(uuid);
			monitor = new Thread(new Runnable() {
				@Override
				public void run() {
					String id = data.get("InstanceID");
					while (active && !"STOPPED".equals(data.get("TransportState"))) {
						sleep(1000);
//						if (DEBUG) LOGGER.debug("InstanceID: " + id);
						for (ActionArgumentValue o : getPositionInfo(d, id)) {
							data.put(o.getArgument().getName(), o.toString());
//							if (DEBUG) LOGGER.debug(o.getArgument().getName() + ": " + o.toString());
						}
						alert();
					}
					if (!active) {
						data.put("TransportState", "STOPPED");
						alert();
					}
				}
			}, "UPNP-" + d.getDetails().getFriendlyName());
			monitor.start();
		}

		public boolean hasPlayControls() {
			return (controls & BasicPlayer.PLAYCONTROL) != 0;
		}

		public boolean hasVolumeControls() {
			return (controls & BasicPlayer.VOLUMECONTROL) != 0;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean b) {
			active = b;
		}

		public boolean needsRenewal() {
			return !active || renew;
		}
	}

	public static Device getDevice(String uuid) {
		return uuid != null && upnpService != null ? upnpService.getRegistry().getDevice(UDN.valueOf(uuid), false) : null;
	}

	public static synchronized void xml2d(String uuid, String xml, Renderer item) {
		try {
			Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
//			doc.getDocumentElement().normalize();
			NodeList ids = doc.getElementsByTagName("InstanceID");
			int idsLength = ids.getLength();
			for (int i = 0; i < idsLength; i++) {
				NodeList c = ids.item(i).getChildNodes();
				String id = ((Element) ids.item(i)).getAttribute("val");
//				if (DEBUG) LOGGER.debug("InstanceID: " + id);
				if (item == null) {
					item = rendererMap.get(uuid, id);
				}
				item.data.put("InstanceID", id);
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
		} catch (Exception e) {
			LOGGER.debug("Error parsing xml: " + e);
		}
	}

	public UPNPControl() {
		rendererMap = new DeviceMap<>(Renderer.class);
	}

	public void init() {
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			UMSHeaders = new UpnpHeaders();
			UMSHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "UMS/" + PMS.getVersion() + " " + new ServerClientTokens());

			DefaultUpnpServiceConfiguration sc = new DefaultUpnpServiceConfiguration() {
				@Override
				public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
					return UMSHeaders;
				}

				@Override
				public int getAliveIntervalMillis() {
					return 10000;
				}
			};

			RegistryListener rl = new DefaultRegistryListener() {
				@Override
				public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
					super.remoteDeviceAdded(registry, device);
					if (isBlocked(getUUID(device)) || !addRenderer(device)) {
						LOGGER.trace("Ignoring remote device: {} {}", device.getType().getType(), device);
					}
					// This may be unnecessary, but we might as well be thorough
					if (device.hasEmbeddedDevices()) {
						for (Device<?, RemoteDevice, ?> embedded : device.getEmbeddedDevices()) {
							if (isBlocked(getUUID(embedded)) || !addRenderer(embedded)) {
								LOGGER.debug("Ignoring embedded device: {} {}", embedded.getType(), embedded.toString());
							}
						}
					}
				}

				@Override
				public void remoteDeviceRemoved(Registry registry, RemoteDevice d) {
					super.remoteDeviceRemoved(registry, d);
					String uuid = getUUID(d);
					if (rendererMap.containsKey(uuid)) {
						rendererMap.mark(uuid, ACTIVE, false);
						rendererRemoved(d);
					}
				}

				@Override
				public void remoteDeviceUpdated(Registry registry, RemoteDevice d) {
					super.remoteDeviceUpdated(registry, d);
					rendererUpdated(d);
				}
			};

			upnpService = new UpnpServiceImpl(sc, rl);
			for (DeviceType t : mediaRendererTypes) {
				upnpService.getControlPoint().search(new DeviceTypeHeader(t));
			}

			LOGGER.debug("UPNP Services are online, listening for media renderers");
		} catch (Exception ex) {
			LOGGER.debug("UPNP startup Error", ex);
		}
	}

	public void shutdown() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (upnpService != null) {
					LOGGER.debug("Stopping UPNP Services...");
					upnpService.shutdown();
				}
			}
		}).start();
	}

	public static boolean isMediaRenderer(Device d) {
		String t = d.getType().getType();
		for (DeviceType r : mediaRendererTypes) {
			if (r.getType().equals(t)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isUpnpDevice(String uuid) {
		return getDevice(uuid) != null;
	}

	public static boolean isActive(String uuid, String id) {
		if (rendererMap.containsKey(uuid, id)) {
			return rendererMap.get(uuid, id).active;
		}
		return false;
	}

	public static boolean isUpnpControllable(String uuid) {
		if (rendererMap.containsKey(uuid)) {
			return rendererMap.get(uuid, "0").controls != 0;
		}
		return false;
	}

	public static String getFriendlyName(String uuid) {
		return getFriendlyName(getDevice(uuid));
	}

	public static String getFriendlyName(Device d) {
		return d != null ? d.getDetails().getFriendlyName() : null;
	}

	public static String getUUID(Device d) {
		return d.getIdentity().getUdn().toString();
	}

	public static URL getURL(Device d) {
		return d instanceof RemoteDevice ? ((RemoteDevice) d).getIdentity().getDescriptorURL() :
			d.getDetails().getBaseURL();
	}

	public static List<String> getServiceNames(Device d) {
		ArrayList<String> services = new ArrayList<>();
		for (Service s : d.getServices()) {
			services.add(s.getServiceId().getId());
		}
		return services;
	}

	public static Map<String, String> getDeviceDetails(Device d) {
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
		if ((detail = man.getManufacturer()) != null) {
			details.put("manufacturer", (String) detail);
		}
		if ((detail = model.getModelName()) != null) {
			details.put("modelName", (String) detail);
		}
		if ((detail = model.getModelNumber()) != null) {
			details.put("modelNumber", (String) detail);
		}
		if ((detail = model.getModelDescription()) != null) {
			details.put("modelDescription", (String) detail);
		}
		if ((detail = man.getManufacturerURI()) != null) {
			details.put("manufacturerURL", detail.toString());
		}
		if ((detail = model.getModelURI()) != null) {
			details.put("modelURL", detail.toString());
		}
		return details;
	}

	public static String getDeviceDetailsString(Device d) {
		return StringUtils.join(getDeviceDetails(d).values(), " ");
	}

	public static String getDeviceIcon(Renderer r, int maxHeight) {
		if (isUpnpDevice(r.uuid)) {
			return getDeviceIcon(getDevice(r.uuid), maxHeight);
		}
		return null;
	}

	public static String getDeviceIcon(Device d, int maxHeight) {
		URL base = getURL(d);
		Icon icon = null;
		String url = null;
		int maxH = maxHeight == 0 ? 99999 : maxHeight, height = 0;
		for (Icon i : d.getIcons()) {
			int h = i.getHeight();
			if (h < maxH && h > height) {
				icon = i;
				height = h;
			}
		}
		try {
			url = icon != null ? new URL(base, icon.getUri().toString()).toString() : null;
		} catch (Exception e) {}
		LOGGER.debug("Device icon: " + url);
		return url;
	}

	protected boolean addRenderer(String uuid) {
		Device device = getDevice(uuid);
		return device != null ? addRenderer(device) : false;
	}

	protected synchronized boolean addRenderer(Device<?, RemoteDevice, ?> device) {
		if (device != null) {
			String uuid = getUUID(device);
			if (isMediaRenderer(device) && rendererFound(device, uuid) != null) {
				LOGGER.debug("Adding device: {} {}", device.getType(), device.toString());
				subscribeAll(device, uuid);
				getProtocolInfo(device);
				rendererMap.mark(uuid, ACTIVE, true);
				rendererReady(uuid);
				return true;
			}
		}
		return false;
	}

	protected void subscribeAll(Device d, String uuid) {
		String name = getFriendlyName(d);
		int ctrl = 0;
		for (Service s : d.getServices()) {
			String sid = s.getServiceId().getId();
			LOGGER.debug("Subscribing to " + sid + " service on " + name);
			if (sid.contains("AVTransport")) {
				ctrl |= AVT;
			} else if (sid.contains("RenderingControl")) {
				ctrl |= RC;
			}
			upnpService.getControlPoint().execute(new SubscriptionCB(s));
		}
		rendererMap.mark(uuid, RENEW, false);
		rendererMap.mark(uuid, CONTROLS, ctrl);
	}

	protected Renderer rendererFound(Device d, String uuid) {
		// Create an instance
		return rendererMap.get(uuid, "0");
	}

	protected void rendererReady(String uuid) {
	}

	protected boolean isBlocked(String uuid) {
		return false;
	}

	protected void rendererUpdated(Device d) {
		String uuid = getUUID(d);
		if (rendererMap.containsKey(uuid)) {
			if (rendererMap.get(uuid, "0").needsRenewal()) {
				LOGGER.debug("Renewing subscriptions to ", getFriendlyName(d));
				subscribeAll(d, uuid);
			}
			rendererMap.mark(uuid, ACTIVE, true);
		}
	}

	protected void rendererRemoved(Device d) {
		LOGGER.debug(getFriendlyName(d) + " is now offline.");
	}

	public static String getUUID(String addr) {
		try {
			return getUUID(InetAddress.getByName(addr));
		} catch (Exception e) {
			return null;
		}
	}

	public static String getUUID(InetAddress socket) {
		Device d = getDevice(socket);
		if (d != null) {
			return getUUID(d);
		}
		return null;
	}

	// Returns the first device regardless of type at the given address, if any
	public static Device getAnyDevice(InetAddress socket) {
		if (upnpService != null) {
			for (Device d : upnpService.getRegistry().getDevices()) {
				try {
					InetAddress devsocket = InetAddress.getByName(getURL(d).getHost());
					if (devsocket.equals(socket)) {
						return d;
					}
				} catch (Exception e) {}
			}
		}
		return null;
	}

	// Returns the first renderer at the given address, if any
	public static Device getDevice(InetAddress socket) {
		if (upnpService != null) {
			for (DeviceType r : mediaRendererTypes) {
				for (Device d : upnpService.getRegistry().getDevices(r)) {
					try {
						InetAddress devsocket = InetAddress.getByName(getURL(d).getHost());
						if (devsocket.equals(socket)) {
							return d;
						}
					} catch (Exception e) {}
				}
			}
		}
		return null;
	}

	public static Renderer getRenderer(String uuid) {
		if (rendererMap.containsKey(uuid)) {
			return rendererMap.get(uuid, "0");
		}
		return null;
	}

	public static boolean isNonRenderer(InetAddress socket) {
		Device d = getDevice(socket);
		if (d != null && !isMediaRenderer(d)) {
			LOGGER.debug("Device at {} is {}: {}", socket, d.getType(), d.toString());
			return true;
		}
		return false;
	}

	public static void connect(String uuid, String instanceID, ActionListener listener) {
		rendererMap.get(uuid, instanceID).connect(listener);
	}

	public static Map<String, String> getData(String uuid, String instanceID) {
		return rendererMap.get(uuid, instanceID).data;
	}

	public UpnpService getService() {
		return upnpService;
	}

	public static class SubscriptionCB extends SubscriptionCallback {
		private String uuid;

		public SubscriptionCB(Service s) {
			super(s);
			uuid = getUUID(s.getDevice());
		}

		@Override
		public void eventReceived(GENASubscription subscription) {
			rendererMap.mark(uuid, ACTIVE, true);
			if (subscription.getCurrentValues().containsKey("LastChange")) {
				xml2d(uuid, subscription.getCurrentValues().get("LastChange").toString(), null);
			}
		}

		@Override
		public void established(GENASubscription sub) {
			LOGGER.debug("Subscription established: " + sub.getService().getServiceId().getId() +
				" on " + getFriendlyName(uuid));
		}

		@Override
		public void failed(GENASubscription sub, UpnpResponse response, Exception ex, String defaultMsg) {
			LOGGER.debug("Subscription failed: " + sub.getService().getServiceId().getId() +
				" on " + getFriendlyName(uuid) + ": " + defaultMsg.split(": ", 2)[1]);
		}

		@Override
		public void failed(GENASubscription sub, UpnpResponse response, Exception ex) {
			LOGGER.debug("Subscription failed: " + sub.getService().getServiceId().getId() +
				" on " + getFriendlyName(uuid) + ": " + createDefaultFailureMessage(response, ex).split(": ", 2)[1]);
		}

		@Override
		public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
			// Reason should be null, or it didn't end regularly
			if (reason != null) {
				LOGGER.debug("Subscription cancelled: " + sub.getService().getServiceId().getId() +
					" on " + uuid + ": " + reason);
			}
			rendererMap.mark(uuid, RENEW, true);
		}

		@Override
		public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
			LOGGER.debug("Missed events: " + numberOfMissedEvents + " for subscription " + sub.getService().getServiceId().getId() + " on " + getFriendlyName(uuid));
		}
	}

	// Convenience functions for sending various upnp service requests
	public static ActionInvocation send(final Device dev, String instanceID, String service, final String action, String... args) {
		Service svc = dev.findService(ServiceId.valueOf("urn:upnp-org:serviceId:" + service));
		final String uuid = getUUID(dev);
		if (svc != null) {
			Action x = svc.getAction(action);
			String name = getFriendlyName(dev);
			boolean log = !action.equals("GetPositionInfo");
			if (x != null) {
				ActionInvocation a = new ActionInvocation(x);
				a.setInput("InstanceID", instanceID);
				for (int i = 0; i < args.length; i += 2) {
					a.setInput(args[i], args[i + 1]);
				}
				if (log) {
					LOGGER.debug("Sending upnp {}.{} {} to {}[{}]", service, action, args, name, instanceID);
				}
				new ActionCallback(a, upnpService.getControlPoint()) {
					@Override
					public void success(ActionInvocation invocation) {
						rendererMap.mark(uuid, ACTIVE, true);
					}

					@Override
					public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
						LOGGER.error("Failed to send action \"{}\" to {}: {}", action, dev.getDetails().getFriendlyName(), defaultMsg);
						if (LOGGER.isTraceEnabled() && invocation != null && invocation.getFailure() != null) {
							LOGGER.trace("", invocation.getFailure());
						}
						rendererMap.mark(uuid, ACTIVE, false);
					}
				}.run();

				if (log) {
					for (ActionArgumentValue arg : a.getOutput()) {
						LOGGER.debug("Received from {}[{}]: {}={}", name, instanceID, arg.getArgument().getName(), arg.toString());
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

	// ConnectionManager
	public static void getProtocolInfo(Device<?, RemoteDevice, ?> device) {
		Service<RemoteDevice, RemoteService> connectionManager = device.findService(ServiceId.valueOf("ConnectionManager"));
		if (connectionManager != null) {
			Action<RemoteService> action = connectionManager.getAction("GetProtocolInfo");
			final String name = getFriendlyName(device);
			if (action != null) {
				final String uuid = getUUID(device);
				ActionInvocation<RemoteService> actionInvocation = new ActionInvocation<>(action);

				new ActionCallback(actionInvocation, upnpService.getControlPoint()) {
					@Override
					public void success(ActionInvocation invocation) {
						Map<String, ActionArgumentValue<RemoteService>> outputs = invocation.getOutputMap();
						ActionArgumentValue<RemoteService> sink = outputs.get("Sink");
						if (sink != null) {
							rendererMap.get(uuid, "0").deviceProtocolInfo.add(DeviceProtocolInfo.GET_PROTOCOLINFO_SINK, sink.toString());
						}
						if (LOGGER.isTraceEnabled()) {
							StringBuilder sb = new StringBuilder();
							for (Entry<String, ActionArgumentValue<RemoteService>> entry : outputs.entrySet()) {
								if (entry.getValue() != null) {
									String value = entry.getValue().toString();
									if (isNotBlank(value)) {
										sb.append("\n").append(entry.getKey()).append(":\n  ");
										sb.append(value.replaceAll(",", "\n  "));
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

	// AVTransport
	// Play modes
	public final static String NORMAL = "NORMAL";
	public final static String REPEAT_ONE = "REPEAT_ONE";
	public final static String REPEAT_ALL = "REPEAT_ALL";
	public final static String RANDOM = "RANDOM";
	// Seek modes
	public final static String REL_BYTE = "X_DLNA_REL_BYTE";
	public final static String REL_TIME = "REL_TIME";
	public final static String TRACK_NR = "TRACK_NR";

	public static void play(Device dev, String instanceID) {
		send(dev, instanceID, "AVTransport", "Play", "Speed", "1");
	}

	public static void pause(Device dev, String instanceID) {
		send(dev, instanceID, "AVTransport", "Pause");
	}

	public static void next(Device dev, String instanceID) {
		send(dev, instanceID, "AVTransport", "Next");
	}

	public static void previous(Device dev, String instanceID) {
		send(dev, instanceID, "AVTransport", "Previous");
	}

	public static void seek(Device dev, String instanceID, String mode, String target) {
		// REL_TIME target format is "hh:mm:ss"
		send(dev, instanceID, "AVTransport", "Seek", "Unit", mode, "Target", target);
	}

	public static void stop(Device dev, String instanceID) {
		send(dev, instanceID, "AVTransport", "Stop");
	}

	public static String getCurrentTransportState(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetTransportInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("CurrentTransportState");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static String getCurrentTransportActions(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetCurrentTransportActions");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("CurrentTransportActions");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static String getDeviceCapabilities(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetDeviceCapabilities");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("DeviceCapabilities");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static String getMediaInfo(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetMediaInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("MediaInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static ActionArgumentValue[] getPositionInfo(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetPositionInfo");
		return invocation == null ? null : invocation.getOutput();
	}

	public static String getTransportInfo(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetTransportInfo");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("TransportInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static String getTransportSettings(Device dev, String instanceID) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "GetTransportSettings");
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("TransportSettings");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static void setAVTransportURI(Device dev, String instanceID, String uri, String metaData) {
		send(dev, instanceID, "AVTransport", "SetAVTransportURI", "CurrentURI", uri,
			"CurrentURIMetaData", metaData != null ? StringUtil.unEncodeXML(metaData) : null);
	}

	public static void setPlayMode(Device dev, String instanceID, String mode) {
		send(dev, instanceID, "AVTransport", "SetPlayMode", "NewPlayMode", mode);
	}

	public static String X_DLNA_GetBytePositionInfo(Device dev, String instanceID, String trackSize) {
		ActionInvocation invocation = send(dev, instanceID, "AVTransport", "X_DLNA_GetBytePositionInfo", "TrackSize", trackSize);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("BytePositionInfo");
		return argumentValue == null ? null : argumentValue.toString();
	}

	// RenderingControl
	// Audio channels
	public final static String MASTER = "Master";
	public final static String LF = "LF";
	public final static String RF = "RF";

	public static String getMute(Device dev, String instanceID) {
		return getMute(dev, instanceID, MASTER);
	}

	public static String getMute(Device dev, String instanceID, String channel) {
		ActionInvocation invocation = send(dev, instanceID, "RenderingControl", "GetMute", "Channel", channel);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("Mute");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static String getVolume(Device dev, String instanceID) {
		return getVolume(dev, instanceID, MASTER);
	}

	public static String getVolume(Device dev, String instanceID, String channel) {
		ActionInvocation invocation = send(dev, instanceID, "RenderingControl", "GetVolume", "Channel", channel);
		if (invocation == null) {
			return null;
		}
		ActionArgumentValue argumentValue = invocation.getOutput("Volume");
		return argumentValue == null ? null : argumentValue.toString();
	}

	public static void setMute(Device dev, String instanceID, boolean on) {
		setMute(dev, instanceID, on, MASTER);
	}

	public static void setMute(Device dev, String instanceID, boolean on, String channel) {
		send(dev, instanceID, "RenderingControl", "SetMute", "DesiredMute", on ? "1" : "0", "Channel", channel);
	}

	public static void setVolume(Device dev, String instanceID, int volume) {
		setVolume(dev, instanceID, volume, MASTER);
	}

	public static void setVolume(Device dev, String instanceID, int volume, String channel) {
		// volume = 1 to 100
		send(dev, instanceID, "RenderingControl", "SetVolume", "DesiredVolume", String.valueOf(volume), "Channel", channel);
	}

}
