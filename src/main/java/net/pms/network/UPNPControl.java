package net.pms.network;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.net.URL;
import java.net.InetAddress;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.apache.commons.lang.StringUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.DeviceTypeHeader;
import org.fourthline.cling.model.action.*;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.model.gena.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.util.BasicPlayer;

public class UPNPControl {
	// Logger ids to write messages to the logs.
	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPControl.class);

	public static final DeviceType[] mediaRendererTypes = new DeviceType[] {
		new UDADeviceType("MediaRenderer", 1),
		// Older Sony Blurays provide only 'Basic' service
		new UDADeviceType("Basic", 1)
	};

	private static UpnpService upnpService;
	private static DocumentBuilder db;

	public static final int ACTIVE = 0;
	public static final int CONTROLS = 1;
	public static final int AVT = BasicPlayer.PLAYCONTROL;
	public static final int RC = BasicPlayer.VOLUMECONTROL;
	public static final int ANY = 0xff;

	private static final boolean DEBUG = true; // log upnp state vars

	protected static Map<String, Renderer> socketMap = new HashMap<>();

	public static class DeviceMap<T extends Renderer> extends HashMap<String,HashMap<String,T>> {
		private static final long serialVersionUID = 1510675619549915489L;

		private Class<T> TClass;

		public DeviceMap(Class<T> t) {
			TClass = t;
		}

		public T get(String uuid, String id) {
			if (! containsKey(uuid)) {
				put(uuid, new HashMap<String,T>());
			}
			HashMap<String,T> m = get(uuid);
			if (!m.containsKey(id)) {
				try {
					T newitem = TClass.newInstance();
					newitem.uuid = uuid;
					m.put(id, newitem);
				} catch (Exception e) {
					LOGGER.debug("Error instantiating item " + uuid + "[" + id + "]: " + e);
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

		public HashMap<String,String> getData(String uuid, String id) {
			if (containsKey(uuid, id)) {
				return get(uuid, id).data;
			}
			return null;
		}

		public T put(String uuid, String id, T item) {
			item.uuid = uuid;
			if (! containsKey(uuid)) {
				get(uuid, "0");
			}
			return get(uuid).put(id, item);
		}

		public String put(String uuid, String id, String key, String value) {
			return get(uuid, id).data.put(key, value);
		}

		public void mark(String uuid, int property, Object value) {
			for (T i : get(uuid).values()) {
				switch (property) {
					case ACTIVE:
						i.active = (boolean)value;
						i.alert();
						break;
					case CONTROLS:
						i.controls = (int)value;
						break;
				default:
					break;
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
		public HashMap<String,String> data;
		public LinkedHashSet<ActionListener> listeners;
		private Thread monitor;
		public boolean active;

		public Renderer(String uuid) {
			this();
			this.uuid = uuid;
			controls = 0;
		}

		public Renderer() {
			data = new HashMap<String, String>();
			listeners = new LinkedHashSet<ActionListener>();
			event = new ActionEvent(this, 0, null);
			monitor = null;
			data.put("TransportState", "STOPPED");
		}

		public void alert() {
			if ((monitor == null || ! monitor.isAlive()) && ! "STOPPED".equals(data.get("TransportState"))) {
				monitor();
			}
			for (ActionListener l : listeners) {
				l.actionPerformed(event);
			}
		}

		public Map<String,String> connect(ActionListener listener) {
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
					while (active && ! "STOPPED".equals(data.get("TransportState"))) {
						UPNPHelper.sleep(1000);
						if (DEBUG) LOGGER.debug("InstanceID: " + id);
						for (ActionArgumentValue o : getPositionInfo(d, id)) {
							data.put(o.getArgument().getName(), o.toString());
							if (DEBUG) LOGGER.debug(o.getArgument().getName() + ": " + o.toString());
						}
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
	}

	public static Device getDevice(String uuid) {
		return uuid != null ? upnpService.getRegistry().getDevice(UDN.valueOf(uuid), false) : null;
	}

	public static synchronized void xml2d(String uuid, String xml, Renderer item) {
		try {
			Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
//			doc.getDocumentElement().normalize();
			NodeList ids = doc.getElementsByTagName("InstanceID");
			for (int i = 0; i < ids.getLength(); i++) {
				NodeList c = ids.item(i).getChildNodes();
				String id = ((Element)ids.item(i)).getAttribute("val");
				if (DEBUG) LOGGER.debug("InstanceID: " + id);
				if (item == null) {
					item = rendererMap.get(uuid, id);
				}
				item.data.put("InstanceID", id);
				for (int n=0; n < c.getLength(); n++) {
					if(c.item(n).getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.debug("skip this " + c.item(n));
						continue;
					}
					Element e = (Element)c.item(n);
					String name = e.getTagName();
					String val = e.getAttribute("val");
					if (DEBUG) LOGGER.debug(name + ": " + val);
					item.data.put(name, val);
				}
				item.alert();
			}
		} catch (Exception e) {
			LOGGER.debug("Error parsing xml: " + e);
		}
	}

	public UPNPControl() {
		rendererMap = new DeviceMap<Renderer>(Renderer.class);
	}

	public void init() {

		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			RegistryListener rl = new DefaultRegistryListener() {
				@Override
				public void remoteDeviceAdded(Registry registry, RemoteDevice d) {
					super.remoteDeviceAdded(registry, d);
					if (! addRenderer(d)) {
						LOGGER.debug(d.getType().getType() + " found: " + d.toString());
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
					String uuid = getUUID(d);
					if (rendererMap.containsKey(uuid)) {
						rendererMap.mark(uuid, ACTIVE, true);
						rendererUpdated(d);
					}
				}

			};
			upnpService = new UpnpServiceImpl(rl);

			// find all media renderers on the network
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

	public static boolean isMediaRenderer(DeviceType t) {
		for (DeviceType r : mediaRendererTypes) {
			if (r.equals(t)) {
				return true;
			}
		}
		return false;
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
		return d.getDetails().getFriendlyName();
	}

	public static String getUUID(Device d) {
		return d.getIdentity().getUdn().toString();
	}

	public static URL getURL(Device d) {
		return d instanceof RemoteDevice ? ((RemoteDevice)d).getIdentity().getDescriptorURL() :
			d.getDetails().getBaseURL();
	}

	public static List<String> getServiceNames(Device d) {
		ArrayList<String> services = new ArrayList<String>();
		for (Service s : d.getServices()) {
			services.add(s.getServiceId().getId());
		}
		return services;
	}

	public static Map<String,String> getDeviceDetails(Device d) {
		DeviceDetails dev = d.getDetails();
		ManufacturerDetails man = dev.getManufacturerDetails();
		ModelDetails model = dev.getModelDetails();
		LinkedHashMap<String,String> details = new LinkedHashMap<String, String>();
		details.put("friendlyName", dev.getFriendlyName());
		details.put("address", getURL(d).getHost());
		details.put("udn", getUUID(d));
		Object detail;
		if((detail = man.getManufacturer()) != null) {
			details.put("manufacturer", (String)detail);
		}
		if((detail = model.getModelName()) != null) {
			details.put("modelName", (String)detail);
		}
		if((detail = model.getModelNumber()) != null) {
			details.put("modelNumber", (String)detail);
		}
		if((detail = model.getModelDescription()) != null) {
			details.put("modelDescription", (String)detail);
		}
		if((detail = man.getManufacturerURI()) != null) {
			details.put("manufacturerURL", detail.toString());
		}
		if((detail = model.getModelURI()) != null) {
			details.put("modelURL", detail.toString());
		}
		return details;
	}

	public static String getDeviceDetailsString(Device d) {
		return StringUtils.join(getDeviceDetails(d).values(), " ");
	}

	public static String getDeviceIcon(Renderer r, int maxHeight) {
		if (r.uuid != null) {
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

	protected synchronized boolean addRenderer(Device d) {
		if (d != null) {
			String uuid = getUUID(d);
			String name = getFriendlyName(d);
			if (isMediaRenderer(d.getType())) {
				rendererFound(d, uuid);
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
				rendererMap.mark(uuid, ACTIVE, true);
				rendererMap.mark(uuid, CONTROLS, ctrl);
				rendererReady(uuid);
				return true;
			}
		}
		return false;
	}

	protected Renderer rendererFound(Device d, String uuid) {
		// Create an instance
		return rendererMap.get(uuid, "0");
	}

	protected void rendererReady(String uuid) {
	}

	protected void rendererUpdated(Device d) {
	}

	protected void rendererRemoved(Device d) {
		LOGGER.debug(getFriendlyName(d) + " is now offline.");
	}

	public static String getUUID(String addr) {
		try {
			return getUUID(InetAddress.getByName(addr));
		} catch(Exception e) {
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

	public static Device getDevice(InetAddress socket) {
		for (Device d : upnpService.getRegistry().getDevices()) {
			try {
				InetAddress devsocket = InetAddress.getByName(getURL(d).getHost());
				if (devsocket.equals(socket)) {
					return d;
				}
			} catch(Exception e) {}
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
		boolean b = (d != null && ! rendererMap.containsKey(getUUID(d)));
		if (b) {
			LOGGER.debug("Device at " + socket + " is a " + d.getType().getType() + ": " + d.toString());
		}
		return b;
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

		public void eventReceived(GENASubscription subscription) {
			if (subscription.getCurrentValues().containsKey("LastChange")) {
				xml2d(uuid, subscription.getCurrentValues().get("LastChange").toString(), null);
			}
		}

		public void established(GENASubscription sub) {
			LOGGER.debug("Subscription established: " + sub.getService().getServiceId().getId() + 
				" on " + getFriendlyName(uuid));
		}

		public void failed(GENASubscription sub, UpnpResponse response, Exception ex, String defaultMsg) {
			LOGGER.debug("Subscription failed: " + sub.getService().getServiceId().getId() +
				" on " + getFriendlyName(uuid) + ": " + defaultMsg.split(": ", 2)[1]);
		}

		public void failed(GENASubscription sub, UpnpResponse response, Exception ex) {
			LOGGER.debug("Subscription failed: " + sub.getService().getServiceId().getId() +
				" on " + getFriendlyName(uuid) + ": " + createDefaultFailureMessage(response, ex).split(": ", 2)[1]);
		}

		public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
			// Reason should be null, or it didn't end regularly
			if (reason != null) {
				LOGGER.debug("Subscription cancelled: " + sub.getService().getServiceId().getId() +
					" on " + uuid + ": " + reason);
			}
		}

		public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
			LOGGER.debug("Missed events: " + numberOfMissedEvents + " for subscription " + sub.getService().getServiceId().getId() + " on " + getFriendlyName(uuid));
		}
	}

	// Convenience functions for sending various upnp service requests

	public static ActionInvocation send(Device dev, String instanceID, String service, String action, String... args) {
		Service svc = dev.findService(ServiceId.valueOf("urn:upnp-org:serviceId:" + service));
		if (svc != null) {
			Action x = svc.getAction(action);
			if (x != null) {
				ActionInvocation a = new ActionInvocation(x);
				a.setInput("InstanceID", instanceID);
				for(int i=0; i<args.length; i+=2) {
					a.setInput(args[i], args[i+1]);
				}
//				LOGGER.debug("UPNP SEND ["+instanceID+"]: " + action);
				new ActionCallback.Default(a, upnpService.getControlPoint()).run();
//				for (ActionArgumentValue arg : a.getOutput()) {
//					LOGGER.debug("UPNP RECV: " + arg.getArgument().getName()+"="+arg.toString());
//				}
				return a;
			}
		}
		return null;
	}

	// ConnectionManager

	public static String getProtocolInfo(Device dev, String instanceID, String dir) {
		return send(dev, instanceID, "ConnectionManager", "GetProtocolInfo")
			.getOutput(dir).toString();
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
		return send(dev, instanceID, "AVTransport", "GetTransportInfo")
			.getOutput("CurrentTransportState").toString();
	}

	public static String getCurrentTransportActions(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetCurrentTransportActions")
			.getOutput("CurrentTransportActions").toString();
	}

	public static String getDeviceCapabilities(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetDeviceCapabilities")
			.getOutput("DeviceCapabilities").toString();
	}

	public static String getMediaInfo(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetMediaInfo")
			.getOutput("MediaInfo").toString();
	}

	public static ActionArgumentValue[] getPositionInfo(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetPositionInfo").getOutput();
	}

	public static String getTransportInfo(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetTransportInfo")
			.getOutput("TransportInfo").toString();
	}

	public static String getTransportSettings(Device dev, String instanceID) {
		return send(dev, instanceID, "AVTransport", "GetTransportSettings")
			.getOutput("TransportSettings").toString();
	}

	public static void setAVTransportURI(Device dev, String instanceID, String uri, String metaData) {
		send(dev, instanceID, "AVTransport", "SetAVTransportURI", "CurrentURI", uri, "CurrentURIMetaData", metaData);
	}

	public static void setPlayMode(Device dev, String instanceID, String mode) {
		send(dev, instanceID, "AVTransport", "SetPlayMode", "NewPlayMode", mode);
	}

	public static String X_DLNA_GetBytePositionInfo(Device dev, String instanceID, String trackSize) {
		return send(dev, instanceID, "AVTransport", "X_DLNA_GetBytePositionInfo", "TrackSize", trackSize)
			.getOutput("BytePositionInfo").toString();
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
		return send(dev, instanceID, "RenderingControl", "GetMute", "Channel", channel)
			.getOutput("Mute").toString();
	}

	public static String getVolume(Device dev, String instanceID) {
		return getVolume(dev, instanceID, MASTER);
	}

	public static String getVolume(Device dev, String instanceID, String channel) {
		return send(dev, instanceID, "RenderingControl", "GetVolume", "Channel", channel)
			.getOutput("Volume").toString();
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
