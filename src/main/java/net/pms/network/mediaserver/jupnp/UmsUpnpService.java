/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.mediaserver.jupnp.model.meta.UmsLocalDevice;
import net.pms.network.mediaserver.jupnp.registry.UmsRegistryListener;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.LoggerFactory;

/**
 * HTTPMU/HTTPU only implementation. Use it's own ssdp upnp server if
 * MediaServerDevice is registered. Need SocketSSDPServer to start if no device
 * or service are registered. Need external http upnp server as no service are
 * implemented.
 */
public class UmsUpnpService extends UpnpServiceImpl {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UmsUpnpService.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final LocalDevice mediaServerDevice = UmsLocalDevice.createMediaServerDevice();

	public UmsUpnpService(boolean serveContentDirectory) {
		super(new UmsUpnpServiceConfiguration(serveContentDirectory));
		//don't log org.jupnp by default to reflext Cling not log to UMS.
		if (!LOGGER.isTraceEnabled() && !CONFIGURATION.isUpnpDebug()) {
			LOGGER.debug("Upnp set in silent log mode");
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			Logger rootLogger = loggerContext.getLogger("org.jupnp");
			rootLogger.setLevel(Level.OFF);
			rootLogger = loggerContext.getLogger("org.jupnp.support");
			rootLogger.setLevel(Level.OFF);
			SpecificationViolationReporter.disableReporting();
		}
	}

	@Override
	protected Registry createRegistry(ProtocolFactory protocolFactory) {
		Registry result = super.createRegistry(protocolFactory);
		result.addListener(new UmsRegistryListener());
		result.addDevice(mediaServerDevice);
		return result;
	}
}
