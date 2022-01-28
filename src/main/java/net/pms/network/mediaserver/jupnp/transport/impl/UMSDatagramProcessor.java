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
package net.pms.network.mediaserver.jupnp.transport.impl;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import net.pms.PMS;
import org.jupnp.http.Headers;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.impl.DatagramProcessorImpl;
import org.jupnp.transport.spi.DatagramProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UMS impl of DatagramProcessor.
 *
 * Fix DatagramProcessorImpl trace null char as it log the DatagramPacket buffer
 * directly (check on JUPnP update if it's corrected).
 * Set our server header.
 */
public class UMSDatagramProcessor extends DatagramProcessorImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatagramProcessor.class);
	private static final String SERVER_HTTP_TOKEN = new ServerClientTokens("UMS", PMS.getVersion()).getHttpToken();

	/**
	 * Overrided as JUPnP logger show null char from the datagram data buffer.
	 * Check on JUPnP update if it's corrected, then remove the overriding method.
	 * @param receivedOnAddress
	 * @param datagram
	 * @return
	 * @throws UnsupportedDataException
	 */
	@Override
	public IncomingDatagramMessage read(InetAddress receivedOnAddress, DatagramPacket datagram) throws UnsupportedDataException {

		try {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("===================================== DATAGRAM BEGIN ============================================");
				LOGGER.trace(new String(datagram.getData()).trim());
				LOGGER.trace("-===================================== DATAGRAM END =============================================");
			}

			ByteArrayInputStream is = new ByteArrayInputStream(datagram.getData());

			String[] startLine = Headers.readLine(is).split(" ");
			if (startLine[0].startsWith("HTTP/1.")) {
				return readResponseMessage(receivedOnAddress, datagram, is, Integer.valueOf(startLine[1]), startLine[2], startLine[0]);
			} else {
				return readRequestMessage(receivedOnAddress, datagram, is, startLine[0], startLine[2]);
			}

		} catch (Exception ex) {
			throw new UnsupportedDataException("Could not parse headers: " + ex, ex, datagram.getData());
		}
	}

	/**
	 * Overrided as there is no simple way to change the product/version for datagrams message
	 * @param message
	 * @return
	 * @throws UnsupportedDataException
	 */
	@Override
	public DatagramPacket write(OutgoingDatagramMessage message) throws UnsupportedDataException {
		if (message.getHeaders().containsKey(UpnpHeader.Type.SERVER)) {
			message.getHeaders().set(UpnpHeader.Type.SERVER.getHttpName(), SERVER_HTTP_TOKEN);
		}
		return super.write(message);
	}
}
