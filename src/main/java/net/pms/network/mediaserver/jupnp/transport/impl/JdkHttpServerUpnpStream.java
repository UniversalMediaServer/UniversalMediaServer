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
package net.pms.network.mediaserver.jupnp.transport.impl;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.Locale;
import net.pms.PMS;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.Exceptions;
import org.jupnp.util.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkHttpServerUpnpStream extends UpnpStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdkHttpServerUpnpStream.class);
	private static final String SERVER_HTTP_TOKEN = new ServerClientTokens("UMS", PMS.getVersion()).getHttpToken();

	private final HttpExchange httpExchange;

	public JdkHttpServerUpnpStream(ProtocolFactory protocolFactory, HttpExchange httpExchange) {
		super(protocolFactory);
		this.httpExchange = httpExchange;
	}

	public HttpExchange getHttpExchange() {
		return httpExchange;
	}

	@Override
	public void run() {

		try {
			LOGGER.trace("Processing HTTP request: {} {}", getHttpExchange().getRequestMethod(), getHttpExchange().getRequestURI());

			// Status
			StreamRequestMessage requestMessage	= new StreamRequestMessage(
				UpnpRequest.Method.getByHttpName(getHttpExchange().getRequestMethod()),
				getHttpExchange().getRequestURI()
			);

			if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
				LOGGER.trace("Method not supported by UPnP stack: {}", getHttpExchange().getRequestMethod());
				throw new RuntimeException("Method not supported: " + getHttpExchange().getRequestMethod());
			}

			// Protocol
			requestMessage.getOperation().setHttpMinorVersion(
					getHttpExchange().getProtocol().toUpperCase(Locale.ROOT).equals("HTTP/1.1") ? 1 : 0
			);

			LOGGER.trace("Created new request message: {}", requestMessage);

			// Connection wrapper
			requestMessage.setConnection(new JdkHttpServerConnection(httpExchange));

			// Headers
			requestMessage.setHeaders(new UpnpHeaders(getHttpExchange().getRequestHeaders()));

			// Body
			byte[] bodyBytes;
			try (InputStream is = getHttpExchange().getRequestBody()) {
				bodyBytes = IO.readBytes(is);
			}

			LOGGER.trace("Reading request body bytes: {}", bodyBytes.length);

			if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {
				LOGGER.trace("Request contains textual entity body, converting then setting string on message");
				requestMessage.setBodyCharacters(bodyBytes);
			} else if (bodyBytes.length > 0) {
				LOGGER.trace("Request contains binary entity body, setting bytes on message");
				requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
			} else {
				LOGGER.trace("Request did not contain entity body");
			}

			//trace log
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamServerRequestMessage(requestMessage);
			}
			// Process it
			StreamResponseMessage responseMessage = process(requestMessage);

			// Return the response
			if (responseMessage != null) {
				//set our own server token
				responseMessage.getHeaders().set(UpnpHeader.Type.SERVER.getHttpName(), SERVER_HTTP_TOKEN);
				//trace log
				if (LOGGER.isTraceEnabled()) {
					StreamsLoggerHelper.logStreamServerResponseMessage(responseMessage, requestMessage);
				}
				LOGGER.trace("Preparing HTTP response message: {}", responseMessage);

				// Headers
				getHttpExchange().getResponseHeaders().putAll(
						responseMessage.getHeaders()
				);

				// Body
				byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
				int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

				LOGGER.trace("Sending HTTP response message: {} with content length: {}", responseMessage, contentLength);
				getHttpExchange().sendResponseHeaders(responseMessage.getOperation().getStatusCode(), contentLength);

				if (contentLength > 0) {
					LOGGER.trace("Response message has body, writing bytes to stream...");
					try (OutputStream os = getHttpExchange().getResponseBody()) {
						IO.writeBytes(os, responseBodyBytes);
						os.flush();
					}
				}

			} else {
				// If it's null, it's 404, everything else needs a proper httpResponse
				LOGGER.trace("Sending HTTP response status: {}", HttpURLConnection.HTTP_NOT_FOUND);
				getHttpExchange().sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
			}

			responseSent(responseMessage);

		} catch (Throwable t) {

			// You definitely want to catch all Exceptions here, otherwise the server will
			// simply close the socket and you get an "unexpected end of file" on the client.
			// The same is true if you just rethrow an IOException - it is a mystery why it
			// is declared then on the HttpHandler interface if it isn't handled in any
			// way... so we always do error handling here.
			// TODO: We should only send an error if the problem was on our side
			// You don't have to catch Throwable unless, like we do here in unit tests,
			// you might run into Errors as well (assertions).
			LOGGER.trace("Exception occurred during UPnP stream processing: {}", t);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Cause: {}", Exceptions.unwrap(t), Exceptions.unwrap(t));
			}
			try {
				httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
			} catch (IOException ex) {
				LOGGER.warn("Couldn't send error response: {}", ex.getMessage(), ex);
			}

			responseException(t);
		}
	}

	protected class JdkHttpServerConnection implements Connection {

		private final HttpExchange exchange;

		public JdkHttpServerConnection(HttpExchange exchange) {
			this.exchange = exchange;
		}

		@Override
		public boolean isOpen() {
			LOGGER.trace("Can't check client connection, socket access impossible on JDK webserver!");
			return true;
		}

		@Override
		public InetAddress getRemoteAddress() {
			return exchange.getRemoteAddress() != null ? exchange.getRemoteAddress().getAddress() : null;
		}

		@Override
		public InetAddress getLocalAddress() {
			return exchange.getLocalAddress() != null ? exchange.getLocalAddress().getAddress() : null;
		}
	}
}
