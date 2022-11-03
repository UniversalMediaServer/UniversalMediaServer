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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.network.mediaserver.UPNPControl;
import net.pms.util.UMSUtils;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.meta.Device;

public class Renderer {
	private static final String TRANSPORT_STATE = "TransportState";
	private static final String STOPPED = "STOPPED";
	private static final String PLAYING = "PLAYING";
	private static final String RECORDING = "RECORDING";
	private static final String TRANSITIONING = "TRANSITIONING";
	public static final String INSTANCE_ID = "InstanceID";

	public static final int PLAYCONTROL = 1;
	public static final int VOLUMECONTROL = 2;

	private int controls;
	protected ActionEvent event;
	protected String uuid;
	// FIXME: unclear in what precise context a media renderer's instanceID != 0
	// BTW, setInstanceID is never used, so it's always 0.
	protected String instanceID = "0";
	public final Map<String, String> data;
	protected Map<String, String> details;
	private LinkedHashSet<ActionListener> listeners;
	private Thread monitorThread;
	private volatile boolean active;
	private volatile boolean renew;
	public final DeviceProtocolInfo deviceProtocolInfo = new DeviceProtocolInfo();
	public volatile PanasonicDmpProfiles panasonicDmpProfiles;
	public boolean isGetPositionInfoImplemented = true;
	public int countGetPositionRequests = 0;

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
		monitorThread = null;
		renew = false;
		data.put(TRANSPORT_STATE, STOPPED);
	}

	public void alert() {
		String transportState = data.get(TRANSPORT_STATE);
		if (UPNPControl.isUpnpDevice(uuid) &&
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
		final Device d = UPNPControl.getDevice(uuid);
		monitorThread = new Thread(() -> {
			String id = data.get(INSTANCE_ID);
			String transportState = data.get(TRANSPORT_STATE);
			while (active &&
					(PLAYING.equals(transportState) ||
					RECORDING.equals(transportState) ||
					TRANSITIONING.equals(transportState))) {
				UMSUtils.sleep(1000);
				// if (DEBUG) LOGGER.debug("InstanceID: " + id);
				// Send the GetPositionRequest only when renderer supports it
				if (isGetPositionInfoImplemented) {
					for (ActionArgumentValue o : UPNPControl.getPositionInfo(d, id, this)) {
						data.put(o.getArgument().getName(), o.toString());
						// if (DEBUG) LOGGER.debug(o.getArgument().getName() +
						// ": " + o.toString());
					}
					alert();
				}
			}
			if (!active) {
				data.put(TRANSPORT_STATE, STOPPED);
				alert();
			}
		}, "UPNP-" + d.getDetails().getFriendlyName());
		monitorThread.start();
	}

	public int getControls() {
		return controls;
	}

	public void setControls(int value) {
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
	}

	public void setRenew(boolean b) {
		renew = b;
	}

	public boolean needsRenewal() {
		return !active || renew;
	}

	/**
	 * Sets the uuid of this renderer.
	 *
	 * @param uuid The uuid.
	 */
	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Returns the uuid of this renderer, if known. Default value is null.
	 *
	 * @return The uuid.
	 */
	public String getUUID() {
		return uuid;
	}

}
