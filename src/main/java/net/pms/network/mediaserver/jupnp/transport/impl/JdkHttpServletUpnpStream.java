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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import net.pms.network.HttpServletHelper;
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on Servlet 6.0 API.
 *
 * @author Surf@ceS
 */
public class JdkHttpServletUpnpStream extends UpnpStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdkHttpServletUpnpStream.class);

	private final AsyncContext asyncContext;

	public JdkHttpServletUpnpStream(ProtocolFactory protocolFactory, AsyncContext asyncContext) {
		super(protocolFactory);
		this.asyncContext = asyncContext;
	}

	@Override
	public void run() {
		try {
			StreamRequestMessage requestMessage = readRequestMessage();
			if (LOGGER.isTraceEnabled()) {
				if (requestMessage.isBodyNonEmptyString()) {
					HttpServletHelper.logHttpServletRequest(getRequest(), requestMessage.getBodyString());
				} else {
					HttpServletHelper.logHttpServletRequest(getRequest(), "");
				}
			}
			StreamResponseMessage responseMessage = process(requestMessage);

			if (responseMessage != null) {
				LOGGER.trace("Preparing HTTP response message: " + responseMessage);
				writeResponseMessage(responseMessage);
			} else {
				// If it's null, it's 404
				LOGGER.trace("Sending HTTP response status: " + HttpURLConnection.HTTP_NOT_FOUND);
				getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
				if (LOGGER.isTraceEnabled()) {
					HttpServletHelper.logHttpServletResponse(getRequest(), getResponse(), null, false);
				}
			}
		} catch (IOException t) {
			LOGGER.info("Exception occurred during UPnP stream processing: " + t);
			if (!getResponse().isCommitted()) {
				LOGGER.trace("Response hasn't been committed, returning INTERNAL SERVER ERROR to client");
				getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				if (LOGGER.isTraceEnabled()) {
					HttpServletHelper.logHttpServletResponse(getRequest(), getResponse(), null, false);
				}
			} else {
				LOGGER.info("Could not return INTERNAL SERVER ERROR to client, response was already committed");
			}
			responseException(t);
		} finally {
			complete();
		}
	}

	private StreamRequestMessage readRequestMessage() throws IOException {
		// Extract what we need from the HTTP httpRequest
		String requestMethod = getRequest().getMethod();
		String requestURI = getRequest().getRequestURI();

		LOGGER.trace("Processing HTTP request: " + requestMethod + " " + requestURI);

		StreamRequestMessage requestMessage;
		try {
			requestMessage = new StreamRequestMessage(UpnpRequest.Method.getByHttpName(requestMethod),
					URI.create(requestURI));
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Invalid request URI: " + requestURI, ex);
		}

		if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
			throw new RuntimeException("Method not supported: " + requestMethod);
		}

		// Connection wrapper
		requestMessage.setConnection(createConnection());

		// Headers
		UpnpHeaders headers = new UpnpHeaders();
		Enumeration<String> headerNames = getRequest().getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			Enumeration<String> headerValues = getRequest().getHeaders(headerName);
			while (headerValues.hasMoreElements()) {
				String headerValue = headerValues.nextElement();
				headers.add(headerName, headerValue);
			}
		}
		requestMessage.setHeaders(headers);

		// Body
		byte[] bodyBytes;
		try (InputStream is = getRequest().getInputStream()) {
			// Needed as on some bad HTTP Stack implementations the inputStream may block when trying to read a request
			// without a body (GET)
			if (UpnpRequest.Method.GET.getHttpName().equals(requestMethod)) {
				bodyBytes = new byte[]{};
			} else {
				bodyBytes = IO.readBytes(is);
			}
		}
		LOGGER.trace("Reading request body bytes: " + bodyBytes.length);

		if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {
			LOGGER.trace("Request contains textual entity body, converting then setting string on message");
			requestMessage.setBodyCharacters(bodyBytes);

		} else if (bodyBytes.length > 0) {
			LOGGER.trace("Request contains binary entity body, setting bytes on message");
			requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);
		} else {
			LOGGER.trace("Request did not contain entity body");
		}

		return requestMessage;
	}

	private void writeResponseMessage(StreamResponseMessage responseMessage) throws IOException {
		int statusCode = responseMessage.getOperation().getStatusCode();
		LOGGER.trace("Sending HTTP response status: " + statusCode);

		getResponse().setStatus(statusCode);

		// Headers
		for (Map.Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
			for (String value : entry.getValue()) {
				getResponse().addHeader(entry.getKey(), value);
			}
		}
		// The Date header is recommended in UDA
		getResponse().setDateHeader("Date", System.currentTimeMillis());

		if (responseMessage.getContentTypeHeader() != null) {
			getResponse().setContentType(responseMessage.getContentTypeHeader().getString());
		}

		// Body
		byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
		int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

		if (contentLength > 0) {
			getResponse().setContentLength(contentLength);
			if (LOGGER.isTraceEnabled()) {
				if (responseMessage.isBodyNonEmptyString()) {
					HttpServletHelper.logHttpServletResponse(getRequest(), getResponse(), responseMessage.getBodyString(), false);
				} else {
					HttpServletHelper.logHttpServletResponse(getRequest(), getResponse(), null, true);
				}
			}
			LOGGER.trace("Response message has body, writing bytes to stream...");
			IO.writeBytes(getResponse().getOutputStream(), responseBodyBytes);
		} else {
			if (LOGGER.isTraceEnabled()) {
				HttpServletHelper.logHttpServletResponse(getRequest(), getResponse(), null, false);
			}
		}
	}

	private Connection createConnection() {
		return new ServletConnection(getRequest());
	}

	private HttpServletRequest getRequest() {
		ServletRequest request = asyncContext.getRequest();
		if (request instanceof HttpServletRequest httpRequest) {
			return httpRequest;
		}
		throw new IllegalStateException("Couldn't get response from asynchronous context, already timed out");
	}

	private HttpServletResponse getResponse() {
		ServletResponse response = asyncContext.getResponse();
		if (response instanceof HttpServletResponse httpResponse) {
			return httpResponse;
		}
		throw new IllegalStateException("Couldn't get response from asynchronous context, already timed out");
	}

	private void complete() {
		try {
			asyncContext.complete();
		} catch (IllegalStateException ex) {
			// If server connection, for whatever reason, is in an illegal state, this will be thrown
			// and we can "probably" ignore it. The request is complete, no matter how it ended.
			LOGGER.info("Error calling servlet container's AsyncContext#complete() method: " + ex);
		}
	}

}
