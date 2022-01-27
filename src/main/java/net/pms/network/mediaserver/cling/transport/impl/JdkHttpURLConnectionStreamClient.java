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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.StreamClient;
import org.seamless.http.Headers;
import org.seamless.util.Exceptions;
import org.seamless.util.io.IO;

public class JdkHttpURLConnectionStreamClient implements StreamClient {

	private static final Logger LOGGER = Logger.getLogger(StreamClient.class.getName());

	protected final JdkHttpURLConnectionStreamClientConfiguration configuration;

	public JdkHttpURLConnectionStreamClient(JdkHttpURLConnectionStreamClientConfiguration configuration) throws InitializationException {
		this.configuration = configuration;

		LOGGER.log(Level.FINE, "Using persistent HTTP stream client connections: {0}", configuration.isUsePersistentConnections());
		System.setProperty("http.keepAlive", Boolean.toString(configuration.isUsePersistentConnections()));
	}

	@Override
	public JdkHttpURLConnectionStreamClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {
		final UpnpRequest requestOperation = requestMessage.getOperation();
		LOGGER.log(Level.FINE, "Preparing HTTP request message with method '{0}': {1}", new Object[]{requestOperation.getHttpMethodName(), requestMessage});
		URL url;
		HttpURLConnection urlConnection = null;
		InputStream inputStream;
		try {
			url = new URL(null, requestOperation.getURI().toString(), new UpnpHttpHandler());
			urlConnection = (UpnpHttpURLConnection) url.openConnection();

			urlConnection.setRequestMethod(requestOperation.getHttpMethodName());

			// Use the built-in expiration, we can't cancel HttpURLConnection
			urlConnection.setReadTimeout(configuration.getTimeoutSeconds() * 1000);
			urlConnection.setConnectTimeout(configuration.getTimeoutSeconds() * 1000);

			applyRequestProperties(urlConnection, requestMessage);
			applyRequestBody(urlConnection, requestMessage);

			LOGGER.log(Level.FINE, "Sending HTTP request: {0}", requestMessage);
			inputStream = urlConnection.getInputStream();
			return createResponse(urlConnection, inputStream);

		} catch (ProtocolException ex) {
			LOGGER.log(Level.FINE, "HTTP request failed: {0} {1}", new Object[]{requestMessage, Exceptions.unwrap(ex)});
			return null;
		} catch (IOException ex) {

			if (urlConnection == null) {
				LOGGER.log(Level.FINE, "HTTP request failed: {0} {1}", new Object[]{requestMessage, Exceptions.unwrap(ex)});
				return null;
			}

			if (ex instanceof SocketTimeoutException) {
				LOGGER.log(Level.INFO,
						"Timeout of {0} seconds while waiting for HTTP request to complete, aborting: {1}", new Object[]{getConfiguration().getTimeoutSeconds(), requestMessage});
				return null;
			}

			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.log(Level.FINEST, "Exception occurred, trying to read the error stream: {0}", Exceptions.unwrap(ex));
			}
			try {
				inputStream = urlConnection.getErrorStream();
				return createResponse(urlConnection, inputStream);
			} catch (Exception errorEx) {
				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.log(Level.FINEST, "Could not read error stream: {0}", errorEx);
				}
				return null;
			}
		} catch (Exception ex) {
			LOGGER.log(Level.FINE, "HTTP request failed: {0} {1}" + requestMessage, Exceptions.unwrap(ex));
			return null;

		} finally {

			if (urlConnection != null) {
				// Release any idle persistent connection, or "indicate that we don't want to use this server for a while"
				urlConnection.disconnect();
			}
		}
	}

	@Override
	public void stop() {
		// NOOP
	}

	protected void applyRequestProperties(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) {

		urlConnection.setInstanceFollowRedirects(false); // Defaults to true but not needed here

		// HttpURLConnection always adds a "Host" header
		// HttpURLConnection always adds an "Accept" header (not needed but shouldn't hurt)
		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			urlConnection.setRequestProperty(
					UpnpHeader.Type.USER_AGENT.getHttpName(),
					getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(), requestMessage.getUdaMinorVersion())
			);
		}

		// Other headers
		applyHeaders(urlConnection, requestMessage.getHeaders());
	}

	protected static void applyHeaders(HttpURLConnection urlConnection, Headers headers) {
		LOGGER.log(Level.FINE, "Writing headers on HttpURLConnection: {0}", headers.size());
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			for (String v : entry.getValue()) {
				String headerName = entry.getKey();
				LOGGER.log(Level.FINE, "Setting header '{0}': {1}", new Object[]{headerName, v});
				urlConnection.setRequestProperty(headerName, v);
			}
		}
	}

	protected static void applyRequestBody(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) throws IOException {

		if (requestMessage.hasBody()) {
			urlConnection.setDoOutput(true);
		} else {
			urlConnection.setDoOutput(false);
			return;
		}

		if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
			IO.writeUTF8(urlConnection.getOutputStream(), requestMessage.getBodyString());
		} else if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
			IO.writeBytes(urlConnection.getOutputStream(), requestMessage.getBodyBytes());
		}
		urlConnection.getOutputStream().flush();
	}

	protected static StreamResponseMessage createResponse(HttpURLConnection urlConnection, InputStream inputStream) throws Exception {

		if (urlConnection.getResponseCode() == -1) {
			LOGGER.log(Level.WARNING, "Received an invalid HTTP response: {0}", urlConnection.getURL());
			LOGGER.log(Level.WARNING, "Is your JuPnP-based server sending connection heartbeats with RemoteClientInfo#isRequestCancelled?" +
					" This client can't handle heartbeats, read the manual.");
			return null;
		}

		// Status
		UpnpResponse responseOperation = new UpnpResponse(urlConnection.getResponseCode(), urlConnection.getResponseMessage());

		LOGGER.log(Level.FINE, "Received response: {}", responseOperation);

		// Message
		StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

		// Headers
		responseMessage.setHeaders(new UpnpHeaders(urlConnection.getHeaderFields()));

		// Body
		byte[] bodyBytes = null;
		try (InputStream is = inputStream) {
			if (inputStream != null) {
				bodyBytes = IO.readBytes(is);
			}
		}

		if (bodyBytes != null && bodyBytes.length > 0 && responseMessage.isContentTypeMissingOrText()) {

			LOGGER.log(Level.FINE, "Response contains textual entity body, converting then setting string on message");
			responseMessage.setBodyCharacters(bodyBytes);

		} else if (bodyBytes != null && bodyBytes.length > 0) {

			LOGGER.log(Level.FINE, "Response contains binary entity body, setting bytes on message");
			responseMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);

		} else {
			LOGGER.log(Level.FINE, "Response did not contain entity body");
		}

		LOGGER.log(Level.FINE, "Response message complete: {}", responseMessage);
		return responseMessage;
	}

	private static class UpnpHttpHandler extends sun.net.www.protocol.http.Handler {

		@Override
		protected URLConnection openConnection(URL u, Proxy p) throws IOException {
			return new UpnpHttpURLConnection(u, p, this);
		}
	}

	private static class UpnpHttpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {

		private static final String[] UPNP_METHODS = {
			"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE",
			"SUBSCRIBE", "UNSUBSCRIBE", "NOTIFY"
		};

		protected UpnpHttpURLConnection(URL u, Proxy p, sun.net.www.protocol.http.Handler handler) throws IOException {
			super(u, p, handler);
		}

		@Override
		public synchronized OutputStream getOutputStream() throws IOException {
			OutputStream os;
			String savedMethod = method;
			switch (method) {
				case "NOTIFY":
					method = "PUT";
					break;
				case "SUBSCRIBE":
				case "UNSUBSCRIBE":
					method = "GET";
					break;
			}
			os = super.getOutputStream();
			method = savedMethod;
			return os;
		}

		@Override
		public void setRequestMethod(String method) throws ProtocolException {
			if (connected) {
				throw new ProtocolException("Can't reset method: already connected");
			}
			for (String m : UPNP_METHODS) {
				if (m.equals(method)) {
					this.method = method;
					return;
				}
			}
			throw new ProtocolException("Invalid UPnP HTTP method: " + method);
		}
	}

}
