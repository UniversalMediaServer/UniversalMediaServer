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
 * Full implementation.
 * !!! Should not be used for now, all services need to be fully implemented before
 * Use it's own ssdp upnp server as device and services are registered.
 * Use it's own http server to handle upnp requests
 */
public class UmsServerUpnpService extends UpnpServiceImpl {
	private final LocalDevice mediaServerDevice = UmsLocalDevice.createMediaServerDevice();

	public UmsServerUpnpService(UPNPControl control, boolean addDevice) {
		super(new UmsServerUpnpServiceConfiguration(), new UmsRegistryListener(control));
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
