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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.pms.dlna.protocolinfo.DeviceProtocolInfo;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.util.BasicPlayer;
import net.pms.util.UMSUtils;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.meta.Device;

public class Renderer {
	private static final String TRANSPORT_STATE = "TransportState";
	private static final String STOPPED = "STOPPED";
	private static final String PLAYING = "PLAYING";
	private static final String RECORDING = "RECORDING";
	private static final String TRANSITIONING = "TRANSITIONING";
	public static final String INSTANCE_ID = "InstanceID";

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
		monitor = null;
		renew = false;
		data.put(TRANSPORT_STATE, STOPPED);
	}

	public void alert() {
		String transportState = data.get(TRANSPORT_STATE);
		if (UPNPControl.isUpnpDevice(uuid) &&
				(monitor == null || !monitor.isAlive()) &&
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
		monitor = new Thread(() -> {
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
