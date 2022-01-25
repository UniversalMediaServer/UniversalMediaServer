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
package net.pms.network.mediaserver.cling;

import net.pms.network.mediaserver.UPNPControl;
import net.pms.network.mediaserver.cling.model.meta.UmsLocalDevice;
import net.pms.network.mediaserver.cling.registry.UmsRegistryListener;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.LocalDevice;

/**
 * HTTPMU/HTTPU only implementation.
 * Use it's own ssdp upnp server if MediaServerDevice is registered.
 * Need SocketSSDPServer to start if no device or service are registered.
 * Need external http upnp server as no service are implemented.
 */
public class UmsNoServerUpnpService extends UpnpServiceImpl {
	private final LocalDevice mediaServerDevice = UmsLocalDevice.createMediaServerDevice();

	/**
	 * @param control UPNPControl to handle the device discovery
	 * @param addDevice if set to <code>true</code>, will register UMS Device/Services on Cling, then Cling will advertise them.
	 * if set to <code>false</code>, SocketSSDPServer need to advertise for UMS Device/Services.
	 */
	public UmsNoServerUpnpService(UPNPControl control, boolean addDevice) {
		super(new UmsNoServerUpnpServiceConfiguration(), new UmsRegistryListener(control));
		if (addDevice) {
			addMediaServerDevice();
		}
	}

	public final void addMediaServerDevice() {
		if (!registry.getLocalDevices().contains(mediaServerDevice)) {
			registry.addDevice(mediaServerDevice);
		}
	}

	public final void removeMediaServerDevice() {
		if (registry.getLocalDevices().contains(mediaServerDevice)) {
			registry.removeDevice(mediaServerDevice);
		}
	}

}
