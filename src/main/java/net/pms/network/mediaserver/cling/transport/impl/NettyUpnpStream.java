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
package net.pms.network.mediaserver.cling.transport.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fourthline.cling.model.message.Connection;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.transport.spi.UpnpStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.seamless.util.Exceptions;


public class NettyUpnpStream extends UpnpStream {

	private static final Logger LOGGER = Logger.getLogger(UpnpStream.class.getName());

	private final MessageEvent event;

	public NettyUpnpStream(ProtocolFactory protocolFactory, MessageEvent event) {
		super(protocolFactory);
		this.event = event;
	}

	protected Connection createConnection() {
		return new NettyServerConnection(event);
	}

	public MessageEvent getMessageEvent() {
		return event;
	}

	public HttpRequest getHttpRequest() {
		return (HttpRequest) event.getMessage();
	}

	@Override
	public void run() {

		try {
			LOGGER.log(Level.FINE, "Processing HTTP request: {0} {1}", new Object[]{getHttpRequest().getMethod().getName(), getHttpRequest().getUri()});
			// Status
			StreamRequestMessage requestMessage =
					new StreamRequestMessage(
							UpnpRequest.Method.getByHttpName(getHttpRequest().getMethod().getName()),
							new URI(getHttpRequest().getUri())
					);

			if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
				LOGGER.log(Level.FINE, "Method not supported by UPnP stack: {0}", getHttpRequest().getMethod().getName());
				throw new RuntimeException("Method not supported: " + getHttpRequest().getMethod().getName());
			}

			// Protocol
			requestMessage.getOperation().setHttpMinorVersion(
					getHttpRequest().getProtocolVersion().getMinorVersion()
			);

			LOGGER.log(Level.FINE, "Created new request message: {0}", requestMessage);

			// Connection wrapper
			requestMessage.setConnection(createConnection());

			// Headers
			UpnpHeaders requestUpnpHeaders = new UpnpHeaders();
			for (Map.Entry<String, String> entry : getHttpRequest().headers().entries()) {
				requestUpnpHeaders.add(entry.getKey(), entry.getValue());
			}
			requestMessage.setHeaders(requestUpnpHeaders);

			// Body
			byte[] bodyBytes;
			try {
				ChannelBuffer content = getHttpRequest().getContent();
				bodyBytes = content.array();
			} catch (UnsupportedOperationException t) {
				bodyBytes = new byte[0];
			}

			LOGGER.log(Level.FINE, "Reading request body bytes: {0}", bodyBytes.length);

			if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {

				LOGGER.fine("Request contains textual entity body, converting then setting string on message");
				requestMessage.setBodyCharacters(bodyBytes);

			} else if (bodyBytes.length > 0) {

				LOGGER.fine("Request contains binary entity body, setting bytes on message");
				requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);

			} else {
				LOGGER.fine("Request did not contain entity body");
			}

			// Process it
			StreamResponseMessage responseMessage = process(requestMessage);

			// Return the response
			if (responseMessage != null) {
				LOGGER.log(Level.FINE, "Preparing HTTP response message: {0}", responseMessage);

				// Body
				byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
				int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

				LOGGER.log(Level.FINE, "Sending HTTP response message: {0} with content length: {1}", new Object[]{responseMessage, contentLength});
				HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.valueOf(responseMessage.getOperation().getStatusCode()));

				// Headers
				for (Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
					response.headers().add(entry.getKey(), entry.getValue());
				}

				if (contentLength > -1) {
					response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
				}
				ChannelFuture future = getMessageEvent().getChannel().write(response);
				if (contentLength > 0) {
					LOGGER.fine("Response message has body, writing bytes to stream...");
					future = getMessageEvent().getChannel().write(responseBodyBytes);
				}
				if (HttpHeaders.Values.CLOSE.equalsIgnoreCase(getHttpRequest().headers().get(HttpHeaders.Names.CONNECTION)) ||
						getHttpRequest().getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
						!HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(getHttpRequest().headers().get(HttpHeaders.Names.CONNECTION))) {
					future.addListener(ChannelFutureListener.CLOSE);
				}
			} else {
				// If it's null, it's 404, everything else needs a proper httpResponse
				LOGGER.log(Level.FINE, "Sending HTTP response status: {0}", HttpResponseStatus.NOT_FOUND);
				HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
				getMessageEvent().getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
			}

			responseSent(responseMessage);

		} catch (IOException | RuntimeException | URISyntaxException t) {

			// You definitely want to catch all Exceptions here, otherwise the server will
			// simply close the socket and you get an "unexpected end of file" on the client.
			// The same is true if you just rethrow an IOException - it is a mystery why it
			// is declared then on the HttpHandler interface if it isn't handled in any
			// way... so we always do error handling here.
			// TODO: We should only send an error if the problem was on our side
			// You don't have to catch Throwable unless, like we do here in unit tests,
			// you might run into Errors as well (assertions).
			LOGGER.log(Level.FINE, "Exception occured during UPnP stream processing: {0}", t);
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, "Cause: {0}", Exceptions.unwrap(t));
			}
			HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
			getMessageEvent().getChannel().write(response);

			responseException(t);
		}
	}

	protected class NettyServerConnection implements Connection {

		private final MessageEvent event;

		public NettyServerConnection(MessageEvent event) {
			this.event = event;
		}

		@Override
		public boolean isOpen() {
			return event.getChannel().isOpen();
		}

		@Override
		public InetAddress getRemoteAddress() {
			return event.getRemoteAddress() != null && event.getRemoteAddress() instanceof InetSocketAddress ?
					((InetSocketAddress) event.getRemoteAddress()).getAddress() :
					null;
		}

		@Override
		public InetAddress getLocalAddress() {
			return event.getChannel().getLocalAddress() != null && event.getChannel().getLocalAddress() instanceof InetSocketAddress ?
					((InetSocketAddress) event.getChannel().getLocalAddress()).getAddress() :
					null;
		}
	}

}
