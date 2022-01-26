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

import ch.qos.logback.classic.Level;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.cling.model.meta.UmsLocalDevice;
import net.pms.network.mediaserver.cling.registry.UmsRegistryListener;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.slf4j.LoggerFactory;

/**
 * HTTPMU/HTTPU only implementation. Use it's own ssdp upnp server if
 * MediaServerDevice is registered. Need SocketSSDPServer to start if no device
 * or service are registered. Need external http upnp server as no service are
 * implemented.
 */
public class UmsUpnpService extends UpnpServiceImpl {

	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final LocalDevice mediaServerDevice = UmsLocalDevice.createMediaServerDevice();

	private boolean addDevice = false;

	public UmsUpnpService(boolean addDevice, boolean ownHttpServer) {
		super(ownHttpServer ? new UmsServerUpnpServiceConfiguration() : new UmsNoServerUpnpServiceConfiguration());
		this.addDevice = addDevice;
		if (!CONFIGURATION.isUpnpDebug()) {
			ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.jupnp");
			logger.setLevel(Level.OFF);
		}
	}

	protected void setOwnHttpServer(boolean addHttpServer) {
		if (addHttpServer) {
			if (!(this.configuration instanceof UmsServerUpnpServiceConfiguration)) {
				this.configuration = new UmsServerUpnpServiceConfiguration();
				if (isRunning) {
					shutdown();
					startup();
				}
			}
		} else {
			if (!(this.configuration instanceof UmsNoServerUpnpServiceConfiguration)) {
				this.configuration = new UmsNoServerUpnpServiceConfiguration();
				if (isRunning) {
					shutdown();
					startup();
				}
			}
		}
	}

	public final void addMediaServerDevice() {
		addDevice = true;
		if (registry != null && !registry.getLocalDevices().contains(mediaServerDevice)) {
			registry.addDevice(mediaServerDevice);
		}
	}

	public final void removeMediaServerDevice() {
		addDevice = false;
		if (registry != null && registry.getLocalDevices().contains(mediaServerDevice)) {
			registry.removeDevice(mediaServerDevice);
		}
	}

	@Override
	protected Registry createRegistry(ProtocolFactory protocolFactory) {
		Registry result = super.createRegistry(protocolFactory);
		result.addListener(new UmsRegistryListener());
		if (addDevice) {
			registry.addDevice(mediaServerDevice);
		}
		return result;
	}
}
