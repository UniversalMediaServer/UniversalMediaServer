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
import java.util.ArrayList;
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
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.RendererDeviceConfiguration;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.gui.IRendererGuiListener;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.network.SpeedStats;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.PlaybackTimer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.renderers.devices.players.UPNPPlayer;
import net.pms.store.MediaStore;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
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
	private volatile boolean allowed;
	private volatile int userId;
	private volatile boolean renew;

	public volatile PanasonicDmpProfiles panasonicDmpProfiles;
	public boolean isGetPositionInfoImplemented = true;
	public int countGetPositionRequests = 0;
	protected BasicPlayer player;
	private StoreItem playingRes;
	private long buffer;
	private int maximumBitrateTotal = 0;
	private String automaticVideoQuality;

	private volatile MediaStore mediaStore;
	private List<String> sharedPath;
	protected Account account;

	private volatile int upnpMode = UPNP_NONE;

	public Renderer(String uuid) throws ConfigurationException, InterruptedException {
		this(null, null, uuid);
	}

	public Renderer(RendererConfiguration ref) throws ConfigurationException, InterruptedException {
		this(ref, null, null);
	}

	public Renderer(RendererConfiguration ref, InetAddress ia) throws ConfigurationException, InterruptedException {
		this(ref, ia, null);
	}

	public Renderer(RendererConfiguration ref, InetAddress ia, String uuid) throws ConfigurationException, InterruptedException {
		super(ref, ia, uuid);
		if (isUpnpAllowed() && uuid == null) {
			String id = getDeviceId();
			if (StringUtils.isNotBlank(id) && !id.contains(",")) {
				uuid = id;
			}
		}
		if (!isAuthenticated()) {
			allowed = RendererFilter.isAllowed(uuid);
			userId = RendererUser.getUserId(uuid);
			account = AccountService.getAccountByUserId(userId);
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
	 * interface when this renderer connects.
	 *
	 * Default value is "UnknownRenderer".
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

	/**
	 * Used to check if the renderer use it's own auth (like webplayer).
	 */
	public boolean isAuthenticated() {
		return false;
	}

	public Account getAccount() {
		return account;
	}

	public void setUserId(int value) {
		if (userId != value) {
			userId = value;
			setAccount(AccountService.getAccountByUserId(userId));
			refreshUserIdGui(value);
		}
	}

	public int getUserId() {
		return userId;
	}

	public void setAccount(Account account) {
		this.account = account;
		resetMediaStore();
	}

	public int getAccountGroupId() {
		return account != null && account.getGroup() != null && account.getGroup().getId() != Integer.MAX_VALUE ? account.getGroup().getId() : 0;
	}

	public int getAccountUserId() {
		return account != null && account.getUser() != null && account.getUser().getId() != Integer.MAX_VALUE ? account.getUser().getId() : 0;
	}

	public boolean hasShareAccess(File value) {
		return hasSameBasePath(getSharedFolders(), value.getAbsolutePath());
	}

	private synchronized void clearSharedFolders() {
		sharedPath = null;
	}

	private synchronized List<String> getSharedFolders() {
		if (sharedPath == null) {
			// Lazy initialization
			sharedPath = new ArrayList<>();
			List<File> files = SharedContentConfiguration.getSharedFolders(getAccountGroupId());
			for (File file : files) {
				sharedPath.add(file.getAbsolutePath());
			}
		}
		return sharedPath;
	}

	@Override
	public void reset() {
		super.reset();
		resetRenderer();
	}

	private void resetRenderer() {
		if (!isAuthenticated()) {
			allowed = RendererFilter.isAllowed(uuid);
			if (userId != RendererUser.getUserId(uuid)) {
				setUserId(RendererUser.getUserId(uuid));
			}
		}
		resetMediaStore();
		// update gui
		updateRendererGui();
		for (Renderer renderer : ConnectedRenderers.getInheritors(this)) {
			renderer.updateRendererGui();
		}
	}

	@Override
	protected void uuidChanged() {
		resetRenderer();
	}

	/**
	 * Returns the MediaStore.
	 *
	 * @return The MediaStore.
	 */
	public synchronized MediaStore getMediaStore() {
		if (mediaStore == null) {
			mediaStore = new MediaStore(this);
			mediaStore.discoverChildren();
		}

		return mediaStore;
	}

	public synchronized void clearMediaStore() {
		clearSharedFolders();
		if (mediaStore != null) {
			mediaStore.clearChildren();
			mediaStore.setDiscovered(false);
			mediaStore.clearWeakResources();
		}
	}

	public synchronized void resetMediaStore() {
		clearSharedFolders();
		if (mediaStore != null) {
			mediaStore.reset();
		}
	}

	public synchronized void addFolderLimit(StoreResource res) {
		if (mediaStore != null) {
			mediaStore.setFolderLim(res);
		}
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
			InetAddress address = JUPnPDeviceHelper.getInetAddress(uuid);
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

	public StoreItem getPlayingRes() {
		return playingRes;
	}

	public void setPlayingRes(StoreItem resource) {
		playingRes = resource;
		getPlayer();
		if (resource != null) {
			player.getState().setName(resource.getDisplayName());
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

	public void refreshAllowedGui(boolean b) {
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.setAllowed(b);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public void refreshUserIdGui(int userId) {
		listenersLock.readLock().lock();
		try {
			for (IRendererGuiListener gui : guiListeners) {
				gui.setUserId(userId);
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
		return (getControls() & PLAYCONTROL) != 0;
	}

	public boolean hasVolumeControls() {
		return (getControls() & VOLUMECONTROL) != 0;
	}

	public boolean isControllable() {
		return getControls() != 0;
	}

	public boolean isControllable(int type) {
		return (getControls() & type) != 0;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean b) {
		active = b;
		refreshActiveGui(b);
	}

	public boolean isAllowed() {
		return allowed;
	}

	public void setAllowed(boolean b) {
		allowed = b;
		refreshAllowedGui(b);
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
		if (PMS.getConfiguration().isAutomaticMaximumBitrate() && !PMS.isRunningTests()) {
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

	public final boolean isUpnpAllowed() {
		return getUpnpMode() > UPNP_NONE;
	}

	public boolean verify() {
		// FIXME: this is a very fallible, incomplete validity test for use only until
		// we find something better. The assumption is that renderers unable determine
		// their own address (i.e. non-UPnP/web renderers that have lost their spot in the
		// address association to a newer renderer at the same ip) are "invalid".
		return getUpnpMode() == Renderer.UPNP_BLOCK || getAddress() != null;
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

	private static boolean hasSameBasePath(List<String> paths, String filename) {
		for (String path : paths) {
			if (filename.startsWith(path)) {
				return true;
			}
		}
		return false;
	}

}
