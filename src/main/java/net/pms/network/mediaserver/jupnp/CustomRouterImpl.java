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
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Let drop own server datagram.
 */
public class CustomRouterImpl extends RouterImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomRouterImpl.class);

	public CustomRouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
		super(configuration, protocolFactory);
	}

	@Override
	public void received(IncomingDatagramMessage msg) {
		String usnHeader = msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN.getHttpName());
		String usn = PMS.get().usn();
		if (usnHeader != null && usnHeader.startsWith(usn)) {
			LOGGER.trace("local device datagram ignored");
			return;
		}
		super.received(msg);
	}
}
