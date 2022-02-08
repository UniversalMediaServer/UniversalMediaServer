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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
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
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class NettyUpnpStream extends UpnpStream {
	//base the logger inside org.jupnp.transport.spi.UpnpStream to reflect old behavior
	private static final Logger LOGGER = LoggerFactory.getLogger(UpnpStream.class);

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
			try {
				ChannelBuffer content = getHttpRequest().getContent();
				bodyBytes = content.array();
			} catch (UnsupportedOperationException t) {
				bodyBytes = new byte[0];
			}

			LOGGER.debug("Reading request body bytes: {0}", bodyBytes.length);

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
				logStreamRequestMessage(requestMessage);
			}
			// Process it
			StreamResponseMessage responseMessage = process(requestMessage);
			//trace log
			if (LOGGER.isTraceEnabled()) {
				logStreamResponseMessage(responseMessage, requestMessage);
			}
			// Return the response
			if (responseMessage != null) {
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
			LOGGER.debug("Exception occured during UPnP stream processing: {}", t);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Cause: {}", Exceptions.unwrap(t));
			}
			HttpResponse response = new DefaultHttpResponse(getHttpRequest().getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
			getMessageEvent().getChannel().write(response).addListener(ChannelFutureListener.CLOSE);

			responseException(t);
		}
	}

	private static final String HTTP_REQUEST_BEGIN = "===================================== HTTP REQUEST BEGIN ========================================";
	private static final String HTTP_REQUEST_END = "===================================== HTTP REQUEST END ==========================================";
	private static final String HTTP_RESPONSE_BEGIN = "===================================== HTTP RESPONSE BEGIN =======================================";
	private static final String HTTP_RESPONSE_END = "===================================== HTTP RESPONSE END =========================================";
	private static void logStreamRequestMessage(StreamRequestMessage requestMessage) {
		StringBuilder header = new StringBuilder();
		header.append(requestMessage.getOperation().getHttpMethodName()).append(" ").append(requestMessage.getUri().getPath());
		header.append(" HTTP/1.").append(requestMessage.getOperation().getHttpMinorVersion()).append("\n\n");
		header.append("HEADER:\n");
		for (Entry<String, List<String>> entry : requestMessage.getHeaders().entrySet()) {
			if (StringUtils.isNotBlank(entry.getKey())) {
				for (String value : entry.getValue()) {
					header.append("  ").append(entry.getKey()).append(": ").append(value).append("\n");
				}
			}
		}
		String formattedContent = null;
		if (requestMessage.isBodyNonEmptyString()) {
			try {
				formattedContent = StringUtil.prettifyXML(requestMessage.getBodyString(), StandardCharsets.UTF_8, 2);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
				formattedContent += "    " + requestMessage.getBodyString().replace("\n", "\n    ") + "\n";
			}
		}
		formattedContent = StringUtils.isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : "";
		LOGGER.trace(
				"Received a request from {}:\n{}\n{}{}\n{}",
				requestMessage.getConnection().getRemoteAddress().getHostAddress(),
				HTTP_REQUEST_BEGIN,
				header,
				formattedContent,
				HTTP_REQUEST_END
				);
	}
	private static void logStreamResponseMessage(StreamResponseMessage responseMessage, StreamRequestMessage requestMessage) {
		StringBuilder header = new StringBuilder();
		for (Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
			if (StringUtils.isNotBlank(entry.getKey())) {
				for (String value : entry.getValue()) {
					header.append("  ").append(entry.getKey()).append(": ").append(value).append("\n");
				}
			}
		}
		String formattedResponse = null;
		if (responseMessage.isBodyNonEmptyString()) {
			try {
				formattedResponse = StringUtil.prettifyXML(responseMessage.getBodyString(), StandardCharsets.UTF_8, 4);
			} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
				formattedResponse = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
				formattedResponse += "    " + responseMessage.getBodyString().replace("\n", "\n    ");
			}
		}
		if (formattedResponse != null) {
			formattedResponse = "CONTENT:\n" + formattedResponse;
		}
		LOGGER.trace(
			"Send a response to {}:\n{}\nHEADER:\n  HTTP/1.{} {}\n{}{}\n{}",
			requestMessage.getConnection().getRemoteAddress(),
			HTTP_RESPONSE_BEGIN,
			responseMessage.getOperation().getHttpMinorVersion(),
			responseMessage.getOperation().getResponseDetails(),
			header,
			formattedResponse != null ? formattedResponse : "",
			HTTP_RESPONSE_END
		);
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
