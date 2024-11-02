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
package net.pms.network.mediaserver.jupnp;

import net.pms.network.mediaserver.jupnp.model.meta.UmsLocalDevice;
import net.pms.network.mediaserver.jupnp.registry.UmsRegistryListener;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;

/**
 * HTTPMU/HTTPU only implementation. Use it's own ssdp upnp server if
 * MediaServerDevice is registered. Need SocketSSDPServer to start if no device
 * or service are registered. Need external http upnp server as no service are
 * implemented.
 */
public class UmsUpnpService extends UpnpServiceImpl {

	private final UmsLocalDevice mediaServerDevice = UmsLocalDevice.createMediaServerDevice();

	public UmsUpnpService() {
		super(new UmsUpnpServiceConfiguration());
	}

	@Override
	protected Registry createRegistry(ProtocolFactory protocolFactory) {
		Registry result = super.createRegistry(protocolFactory);
		result.addListener(new UmsRegistryListener());
		result.addDevice(mediaServerDevice);
		return result;
	}

	public void sendAlive() {
		getProtocolFactory().createSendingNotificationAlive(mediaServerDevice).run();
	}

}
