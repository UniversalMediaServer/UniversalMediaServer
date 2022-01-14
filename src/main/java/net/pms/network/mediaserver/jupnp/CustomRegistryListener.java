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
package net.pms.network.mediaserver.jupnp;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.UPNPControl;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Let UPNPControl know remote device state.
 */
public class CustomRegistryListener extends DefaultRegistryListener {

	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomRegistryListener.class);
	private final UPNPControl upnpControl;

	public CustomRegistryListener(UPNPControl upnpControl) {
		this.upnpControl = upnpControl;
	}

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		if (deviceIsLocalDevice(device)) {
			LOGGER.info("local device Added");
			return;
		}
		super.remoteDeviceAdded(registry, device);
		upnpControl.remoteDeviceAdded(device);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
		if (deviceIsLocalDevice(device)) {
			LOGGER.info("local device Updated");
			return;
		}
		super.remoteDeviceUpdated(registry, device);
		upnpControl.remoteDeviceAdded(device);
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
		if (deviceIsLocalDevice(device)) {
			LOGGER.info("local device Removed");
			return;
		}
		super.remoteDeviceRemoved(registry, device);
		upnpControl.remoteDeviceRemoved(device);
	}

	private static boolean deviceIsLocalDevice(RemoteDevice device) {
		return device.getIdentity().getDescriptorURL().getPort() == CONFIGURATION.getServerPort();
	}

}
