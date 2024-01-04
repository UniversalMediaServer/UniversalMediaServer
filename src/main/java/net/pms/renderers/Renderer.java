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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.RendererDeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.gui.IRendererGuiListener;
import net.pms.network.SpeedStats;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.PlaybackTimer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.renderers.devices.players.UPNPPlayer;
import net.pms.util.UMSUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.action.ActionArgumentValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Renderer extends RendererDeviceConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);
	public static final String NOTRANSCODE = "_NOTRANSCODE_";

	private static final String TRANSPORT_STATE = "TransportState";
	private static final String STOPPED = "STOPPED";
	private static final String PLAYING = "PLAYING";
	private static final String RECORDING = "RECORDING";
	private static final String TRANSITIONING = "TRANSITIONING";
	public static final String INSTANCE_ID = "InstanceID";

	/**
	 * Upnp service startup management
	 */
	public static final int UPNP_BLOCK = -2;
	private static final int UPNP_POSTPONE = -1;
	private static final int UPNP_NONE = 0;
	public static final int UPNP_ALLOW = 1;

	public static final int PLAYCONTROL = 1;
	public static final int VOLUMECONTROL = 2;

	private final ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();
	private final LinkedHashSet<IRendererGuiListener> guiListeners = new LinkedHashSet<>();
	public final Map<String, String> data = new HashMap<>();
	private final LinkedHashSet<ActionListener> listeners = new LinkedHashSet<>();
	public final DeviceProtocolInfo deviceProtocolInfo = new DeviceProtocolInfo();

	private int controls;
	protected ActionEvent event;

	protected Map<String, String> details;
	private Thread monitorThread;
	private volatile boolean active;
	private volatile boolean renew;

	public volatile PanasonicDmpProfiles panasonicDmpProfiles;
	public boolean isGetPositionInfoImplemented = true;
	public int countGetPositionRequests = 0;
	protected BasicPlayer player;
	private DLNAResource playingRes;
	private long buffer;
	private int maximumBitrateTotal = 0;

	private volatile RootFolder rootFolder;
	private String automaticVideoQuality;

	public Renderer(String uuid) throws ConfigurationException, InterruptedException {
		super(uuid);
		setup();
	}

	public Renderer(RendererConfiguration ref) throws ConfigurationException, InterruptedException {
		super(ref);
		setup();
	}

	public Renderer(RendererConfiguration ref, InetAddress ia) throws ConfigurationException, InterruptedException {
		super(ref, ia);
		setup();
	}

	private void setup() {
		setRootFolder(null);
		if (isUpnpAllowed() && uuid == null) {
			String id = getDeviceId();
			if (StringUtils.isNotBlank(id) && !id.contains(",")) {
				uuid = id;
			}
		}
		controls = 0;
		active = false;
		details = null;
		event = new ActionEvent(this, 0, null);
		monitorThread = null;
		renew = false;
		data.put(TRANSPORT_STATE, STOPPED);
	}

	/**
	 * RendererName: Determines the name that is displayed in the UMS user
	 * interface when this renderer connects. Default value is "Unknown
	 * renderer".
	 *
	 * @return The renderer name.
	 */
	@Override
	public String getRendererName() {
		if (details != null && details.containsKey("friendlyName")) {
			return details.get("friendlyName");
		} else if (isUpnp()) {
			return JUPnPDeviceHelper.getFriendlyName(uuid);
		}
		return getConfName();
	}

	public String getId() {
		return uuid != null ? uuid : getAddress().toString().substring(1);
	}

	@Override
	public void reset() {
		super.reset();
		// update gui
		updateRendererGui();
		for (Renderer renderer : ConnectedRenderers.getInheritors(this)) {
			renderer.updateRendererGui();
		}
	}

	public void delete(int delay) {
		ConnectedRenderers.delete(this, delay);
	}

	/**
	 * Returns the RootFolder.
	 *
	 * @return The RootFolder.
	 */
	public synchronized RootFolder getRootFolder() {
		if (rootFolder == null) {
			rootFolder = new RootFolder();
			rootFolder.setDefaultRenderer(this);
			if (umsConfiguration.getUseCache()) {
				rootFolder.discoverChildren();
			}
		}

		return rootFolder;
	}

	public void addFolderLimit(DLNAResource res) {
		if (rootFolder != null) {
			rootFolder.setFolderLim(res);
		}
	}

	public synchronized void setRootFolder(RootFolder r) {
		rootFolder = r;
	}

	/**
	 * Associate an IP address with this renderer. The association will
	 * persist between requests, allowing the renderer to be recognized
	 * by its address in later requests.
	 *
	 * @param sa The IP address to associate.
	 * @return whether the device at this address is a renderer.
	 * @see #getRendererConfigurationBySocketAddress(InetAddress)
	 */
	public boolean associateIP(InetAddress sa) {
		if (JUPnPDeviceHelper.isNonRenderer(sa)) {
			// TODO: remove it if already added unknowingly
			return false;
		}

		ConnectedRenderers.addRendererAssociation(sa, this);
		resetUpnpMode();

		if (
			(
				umsConfiguration.isAutomaticMaximumBitrate() ||
				umsConfiguration.isSpeedDbg()
			) &&
			!(
				sa.isLoopbackAddress() ||
				sa.isAnyLocalAddress()
			)
		) {
			SpeedStats.getSpeedInMBits(sa, getRendererName());
		}
		return true;
	}

	/**
	 * Returns the UPnP details of this renderer as broadcast by itself, if known.
	 * Default value is null.
	 *
	 * @return The detail map.
	 */
	public Map<String, String> getUpnpDetails() {
		return JUPnPDeviceHelper.getDeviceDetails(uuid);
	}

	public boolean isUpnp() {
		return JUPnPDeviceHelper.isUpnpDevice(uuid);
	}

	public void setDetails(Map<String, String> value) {
		details = value;
	}

	public Map<String, String> getDetails() {
		if (details == null) {
			if (isUpnp()) {
				details = getUpnpDetails();
			} else {
				details = new LinkedHashMap<>();
				details.put(Messages.getString("Name"), getRendererName());
				if (getAddress() != null) {
					details.put(Messages.getString("Address"), getAddress().getHostAddress());
				}
			}
		}
		return details;
	}

	/**
	 * Returns the icon to use for displaying this renderer in UMS as defined
	 * in the renderer configurations.
	 * Default value is UNKNOWN_ICON.
	 *
	 * @return The renderer icon.
	 */
	@Override
	public String getRendererIcon() {
		String icon = super.getRendererIcon();
		String deviceIcon = null;
		if (icon.equals(UNKNOWN_ICON) && uuid != null) {
			deviceIcon = JUPnPDeviceHelper.getDeviceIcon(uuid, 140);
		}
		return deviceIcon == null ? icon : deviceIcon;
	}

	/**
	 * Returns the current UPnP state variables of this renderer, if known. Default value is null.
	 *
	 * @return The data.
	 */
	public Map<String, String> getUPNPData() {
		return data;
	}

	/**
	 * Returns the UPnP services of this renderer.
	 * Default value is null.
	 *
	 * @return The list of service names.
	 */
	public List<String> getUpnpServices() {
		return isUpnp() ? JUPnPDeviceHelper.getServiceNames(uuid) : null;
	}

	/**
	 * Returns whether this renderer is known to be offline.
	 *
	 * @return Whether offline.
	 */
	public boolean isOffline() {
		return !isActive();
	}

	/**
	 * Returns whether this renderer is currently connected via UPnP.
	 *
	 * @return Whether connected.
	 */
	public boolean isUpnpConnected() {
		return uuid != null && JUPnPDeviceHelper.isActive(uuid);
	}

	/**
	 * Returns whether this renderer has an associated address.
	 *
	 * @return Has address.
	 */
	public boolean hasAssociatedAddress() {
		return ConnectedRenderers.hasInetAddressForRenderer(this);
	}

	/**
	 * Returns this renderer's associated address.
	 *
	 * @return The address.
	 */
	public InetAddress getAddress() {
		// If we have a uuid look up the UPnP device address, which is always
		// correct even if another device has overwritten our association
		if (uuid != null) {
			InetAddress address = JUPnPDeviceHelper.getAddress(uuid);
			if (address != null) {
				return address;
			}
		}
		// Otherwise check the address association
		return ConnectedRenderers.getRendererInetAddress(this);
	}

	/**
	 * Returns whether this renderer provides UPnP control services.
	 *
	 * @return Whether controllable.
	 */
	public boolean isUpnpControllable() {
		return JUPnPDeviceHelper.isUpnpControllable(uuid);
	}

	/**
	 * Returns a player for this renderer if control is supported.
	 *
	 * @return a player or null.
	 */
	public BasicPlayer getPlayer() {
		if (player == null) {
			player = isUpnpControllable() ? new UPNPPlayer(this) :
				new PlaybackTimer(this);
		} else if (player instanceof PlaybackTimer && isUpnpControllable()) {
			player = new UPNPPlayer(this);
		}
		return player;
	}

	/**
	 * Sets the player.
	 *
	 * @param player
	 */
	public void setPlayer(BasicPlayer player) {
		this.player = player;
	}

	public DLNAResource getPlayingRes() {
		return playingRes;
	}

	public void setPlayingRes(DLNAResource dlna) {
		playingRes = dlna;
		getPlayer();
		if (dlna != null) {
			player.getState().setName(dlna.getDisplayName());
			player.start();
		} else {
			player.reset();
		}
	}

	public void setBuffer(long mb) {
		buffer = mb < 0 ? 0 : mb;
		getPlayer().setBuffer(mb);
	}

	public long getBuffer() {
		return buffer;
	}

	public void addGuiListener(IRendererGuiListener gui) {
		listenersLock.readLock().lock();
		try {
			guiListeners.add(gui);
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void removeGuiListener(IRendererGuiListener gui) {
		listenersLock.readLock().lock();
		try {
			guiListeners.remove(gui);
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void updateRendererGui() {
		LOGGER.debug("Updating status button for {}", getRendererName());
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.updateRenderer(this);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void refreshActiveGui(boolean b) {
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.setActive(b);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void refreshPlayerStateGui(PlayerState state) {
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.refreshPlayerState(state);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void deleteGuis() {
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.delete();
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}


	/**
	 * Returns the maximum bitrate (in bits-per-second) as defined by
	 * whichever is lower out of the renderer setting or user setting.
	 *
	 * @return The maximum bitrate in bits-per-second.
	 */
	public int getMaxBandwidth() {
		if (maximumBitrateTotal > 0) {
			return maximumBitrateTotal;
		}

		int[] defaultMaxBitrates = getVideoBitrateConfig(PMS.getConfiguration().getMaximumBitrate());
		int[] rendererMaxBitrates = new int[2];

		int maxVideoBitrate = getMaxVideoBitrate();
		if (maxVideoBitrate > 0) {
			rendererMaxBitrates = getVideoBitrateConfig(Integer.toString(maxVideoBitrate));
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace(
				"Using video bitrate limit from {} configuration ({} Mb/s) because " +
				"it is lower than the general configuration bitrate limit ({} Mb/s)",
				getRendererName(),
				rendererMaxBitrates[0],
				defaultMaxBitrates[0]
			);
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (isHalveBitrate()) {
			defaultMaxBitrates[0] /= 2;
		}

		maximumBitrateTotal = defaultMaxBitrates[0] * 1000000;
		return maximumBitrateTotal;
	}

	public boolean nox264() {
		return false;
	}

	public void alert() {
		String transportState = data.get(TRANSPORT_STATE);
		if (JUPnPDeviceHelper.isUpnpDevice(uuid) &&
				(monitorThread == null || !monitorThread.isAlive()) &&
				(PLAYING.equals(transportState) ||
				RECORDING.equals(transportState) ||
				TRANSITIONING.equals(transportState))) {
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
		monitorThread = new Thread(() -> {
			String transportState = data.get(TRANSPORT_STATE);
			while (active &&
					(PLAYING.equals(transportState) ||
					RECORDING.equals(transportState) ||
					TRANSITIONING.equals(transportState))) {
				UMSUtils.sleep(1000);
				// Send the GetPositionRequest only when renderer supports it
				if (isGetPositionInfoImplemented) {
					for (ActionArgumentValue o : JUPnPDeviceHelper.getPositionInfo(this)) {
						data.put(o.getArgument().getName(), o.toString());
					}
					alert();
				}
			}
			if (!active) {
				data.put(TRANSPORT_STATE, STOPPED);
				alert();
			}
		}, "UPNP-" + getRendererName());
		monitorThread.start();
	}

	public int getControls() {
		return controls;
	}

	public final void setControls(int value) {
		controls = value;
	}

	public boolean hasPlayControls() {
		return (controls & PLAYCONTROL) != 0;
	}

	public boolean hasVolumeControls() {
		return (controls & VOLUMECONTROL) != 0;
	}

	public boolean isControllable() {
		return controls != 0;
	}

	public boolean isControllable(int type) {
		return (controls & type) != 0;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean b) {
		active = b;
		refreshActiveGui(b);
	}

	public void setRenew(boolean b) {
		renew = b;
	}

	public boolean needsRenewal() {
		return !active || renew;
	}

	/**
	 * Returns the maximum bitrate (in megabits-per-second) supported by the
	 * media renderer as defined in the renderer configuration. The default
	 * value is 0 (unlimited).
	 *
	 * @return The bitrate.
	 */
	@Override
	public int getMaxVideoBitrate() {
		if (PMS.getConfiguration().isAutomaticMaximumBitrate()) {
			try {
				int calculatedSpeed = calculatedSpeed();
				if (calculatedSpeed >= 70) { // this should be a wired connection
					setAutomaticVideoQuality("Automatic (Wired)");
				} else {
					setAutomaticVideoQuality("Automatic (Wireless)");
				}

				return calculatedSpeed;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return 0;
			} catch (ExecutionException e) {
				LOGGER.debug("Automatic maximum bitrate calculation failed with: {}", e.getCause().getMessage());
				LOGGER.trace("", e.getCause());
			}
		}
		return super.getMaxVideoBitrate();
	}

	/**
	 * Returns the actual renderer network speed in Mbits/sec calculated from
	 * the Ping response.
	 *
	 * @return the actual speed or the default MAX_VIDEO_BITRATE when the calculation fails.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public int calculatedSpeed() throws InterruptedException, ExecutionException {
		int max = super.getMaxVideoBitrate();
		InetAddress addr = ConnectedRenderers.getRendererInetAddress(this);
		if (addr != null) {
			Future<Integer> speed = SpeedStats.getSpeedInMBitsStored(addr);
			if (speed != null) {
				if (max == 0) {
					return speed.get();
				}

				if (speed.get() > max && max > 0) {
					return max;
				}

				return speed.get();
			}
		}
		return max;
	}

	private void setAutomaticVideoQuality(String value) {
		automaticVideoQuality = value;
	}

	public String getAutomaticVideoQuality() {
		return automaticVideoQuality;
	}

	public String getDefaultFilename() {
		String id = getId();
		return (getSimpleName() + "-" + (id.startsWith("uuid:") ? id.substring(5, 11) : id)).replace(" ", "") + ".conf";
	}

	public File getUsableFile() {
		File file = getFile();
		if (file == null || file.equals(NOFILE)) {
			String name = getSimpleName();
			if (name.equals(RendererConfigurations.getDefaultConf().getSimpleName())) {
				file = new File(RendererConfigurations.getWritableRenderersDir(), getDefaultFilename());
			} else {
				file = new File(RendererConfigurations.getWritableRenderersDir(), name.replace(" ", "") + ".conf");
			}
		}
		return file;
	}

	private volatile int upnpMode = UPNP_NONE;
	public int getUpnpMode() {
		if (upnpMode == UPNP_NONE) {
			upnpMode = getUpnpMode(getUpnpAllow());
		}
		return upnpMode;
	}

	public String getUpnpModeString() {
		return getUpnpModeString(upnpMode);
	}

	public void resetUpnpMode() {
		setUpnpMode(getUpnpMode(getUpnpAllow()));
	}

	public void setUpnpMode(int mode) {
		if (upnpMode != mode) {
			upnpMode = mode;
			if (upnpMode == UPNP_ALLOW) {
				String id = uuid != null ? uuid : ConnectedRenderers.getUuidOf(getAddress());
				if (id != null) {
					setUpnpAllow("true");
					JUPnPDeviceHelper.activate(id);
				}
			}
		}
	}

	public boolean isUpnpPostponed() {
		return getUpnpMode() == UPNP_POSTPONE;
	}

	public boolean isUpnpAllowed() {
		return getUpnpMode() > UPNP_NONE;
	}

	public static int getUpnpMode(String mode) {
		if (mode != null) {
			return switch (mode.trim().toLowerCase()) {
				case "false" -> UPNP_BLOCK;
				case "postpone" -> UPNP_POSTPONE;
				default -> UPNP_ALLOW;
			};
		}
		return UPNP_ALLOW;
	}

	public static String getUpnpModeString(int mode) {
		return switch (mode) {
			case UPNP_BLOCK -> "blocked";
			case UPNP_POSTPONE -> "postponed";
			case UPNP_NONE -> "unknown";
			default -> "allowed";
		};
	}

}
