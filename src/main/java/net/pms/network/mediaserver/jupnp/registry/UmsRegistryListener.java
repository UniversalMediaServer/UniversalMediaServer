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
package net.pms.network.mediaserver.jupnp.registry;

import net.pms.network.mediaserver.UPNPHelper;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;

public class UmsRegistryListener extends DefaultRegistryListener {

	@Override
	public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
		super.remoteDeviceAdded(registry, device);
		UPNPHelper.getInstance().remoteDeviceAdded(device);
	}

	@Override
	public void remoteDeviceRemoved(Registry registry, RemoteDevice d) {
		super.remoteDeviceRemoved(registry, d);
		UPNPHelper.getInstance().remoteDeviceRemoved(d);
	}

	@Override
	public void remoteDeviceUpdated(Registry registry, RemoteDevice d) {
		super.remoteDeviceUpdated(registry, d);
		UPNPHelper.getInstance().remoteDeviceUpdated(d);
	}
}
