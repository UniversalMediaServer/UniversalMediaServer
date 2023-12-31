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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.PMS;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyUpnpStream extends UpnpStream {
	//base the logger inside org.jupnp.transport.spi.UpnpStream to reflect old behavior
	private static final Logger LOGGER = LoggerFactory.getLogger(UpnpStream.class);
	private static final String SERVER_HTTP_TOKEN = new ServerClientTokens("UMS", PMS.getVersion()).getHttpToken();

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
			LOGGER.debug("Processing HTTP request: {} {}", getHttpRequest().getMethod().getName(), getHttpRequest().getUri());
			// Status
			StreamRequestMessage requestMessage =
					new StreamRequestMessage(
							UpnpRequest.Method.getByHttpName(getHttpRequest().getMethod().getName()),
							new URI(getHttpRequest().getUri())
					);
			if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
				LOGGER.debug("Method not supported by UPnP stack: {}", getHttpRequest().getMethod().getName());
				throw new RuntimeException("Method not supported: " + getHttpRequest().getMethod().getName());
			}

			// Protocol
			requestMessage.getOperation().setHttpMinorVersion(
					getHttpRequest().getProtocolVersion().getMinorVersion()
			);

			LOGGER.debug("Created new request message: {}", requestMessage);

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
			long requestContentLength = HttpHeaders.getContentLength(getHttpRequest(), 0);
			try {
				ChannelBuffer content = getHttpRequest().getContent();
				if (content.hasArray()) {
					bodyBytes = content.array();
				} else if (requestContentLength > 0) {
					bodyBytes = new byte[(int) requestContentLength];
					content.readBytes(bodyBytes);
				} else {
					bodyBytes = new byte[0];
				}
			} catch (UnsupportedOperationException t) {
				LOGGER.trace("Reading request body failed: {}", t);
				bodyBytes = new byte[0];
			}

			LOGGER.debug("Reading request body bytes: {}", bodyBytes.length);

			if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {

				LOGGER.debug("Request contains textual entity body, converting then setting string on message");
				requestMessage.setBodyCharacters(bodyBytes);

			} else if (bodyBytes.length > 0) {

				LOGGER.debug("Request contains binary entity body, setting bytes on message");
				requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);

			} else {
				LOGGER.debug("Request did not contain entity body");
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
				LOGGER.debug("Preparing HTTP response message: {}", responseMessage);

				// Body
				byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
				int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

				LOGGER.debug("Sending HTTP response message: {} with content length: {}", responseMessage, contentLength);
				HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.valueOf(responseMessage.getOperation().getStatusCode()));

				// Headers
				for (Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
					response.headers().add(entry.getKey(), entry.getValue());
				}

				if (contentLength > -1) {
					response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
				}
				if (contentLength > 0 && !"HEAD".equals(requestMessage.getOperation().getHttpMethodName())) {
					LOGGER.debug("Response message has body, writing bytes to stream...");
					ChannelBuffer buf = ChannelBuffers.copiedBuffer(responseBodyBytes);
					response.setContent(buf);
				}
				ChannelFuture future = getMessageEvent().getChannel().write(response);
				if (HttpHeaders.Values.CLOSE.equalsIgnoreCase(getHttpRequest().headers().get(HttpHeaders.Names.CONNECTION)) ||
						getHttpRequest().getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
						!HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(getHttpRequest().headers().get(HttpHeaders.Names.CONNECTION))) {
					future.addListener(ChannelFutureListener.CLOSE);
				}
			} else {
				// If it's null, it's 404, everything else needs a proper httpResponse
				LOGGER.debug("Sending HTTP response status: {}", HttpResponseStatus.NOT_FOUND);
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
			LOGGER.debug("Exception occurred during UPnP stream processing: {}", t);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Cause: {}", Exceptions.unwrap(t));
			}
			HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
			getMessageEvent().getChannel().write(response).addListener(ChannelFutureListener.CLOSE);

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
			return event.getRemoteAddress() instanceof InetSocketAddress ?
					((InetSocketAddress) event.getRemoteAddress()).getAddress() :
					null;
		}

		@Override
		public InetAddress getLocalAddress() {
			return event.getChannel().getLocalAddress() instanceof InetSocketAddress ?
					((InetSocketAddress) event.getChannel().getLocalAddress()).getAddress() :
					null;
		}
	}

}
