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

import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpMessage;
import org.fourthline.cling.model.message.UpnpOperation;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.spi.AbstractStreamClient;
import org.fourthline.cling.transport.spi.InitializationException;


/**
 * Apache StreamClient as we have a dependency on it.
 */
public class ApacheStreamClient extends AbstractStreamClient<StreamClientConfigurationImpl, HttpUriRequest> {

	//using java.util.logging.Logger will not show error to UMS.
	//only here to have the same behavior than before.
	private static final Logger LOGGER = Logger.getLogger(ApacheStreamClient.class.getName());

	protected final StreamClientConfigurationImpl configuration;
	protected final CloseableHttpClient httpClient;

	public ApacheStreamClient(StreamClientConfigurationImpl configuration) throws InitializationException {
		this.configuration = configuration;
		// These are some safety settings, we should never run into these timeouts as we
		// do our own expiration checking
		int connectTimeout = (configuration.getTimeoutSeconds() + 5) * 1000;
		int cpus = Runtime.getRuntime().availableProcessors();
		int maxThreads = 5 * cpus;

		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(connectTimeout)
				.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(2);
		connectionManager.setMaxTotal(maxThreads);

		httpClient = HttpClients
				.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(defaultRequestConfig)
				.build();
	}

	@Override
	public StreamClientConfigurationImpl getConfiguration() {
		return configuration;
	}

	@Override
	protected HttpUriRequest createRequest(StreamRequestMessage requestMessage) {
		final UpnpRequest upnpRequest = requestMessage.getOperation();

		LOGGER.log(Level.FINE, "Creating HTTP request. URI: '{0}' method: '{1}'", new Object[]{upnpRequest.getURI(), upnpRequest.getMethod()});
		RequestBuilder request;
		switch (upnpRequest.getMethod()) {
			case GET:
			case POST:
				request = RequestBuilder.create(upnpRequest.getHttpMethodName());
				request.setUri(upnpRequest.getURI());
				break;
			case SUBSCRIBE:
			case UNSUBSCRIBE:
			case NOTIFY:
				request = RequestBuilder.create(upnpRequest.getHttpMethodName());
				request.setUri(upnpRequest.getURI());
				break;
			default:
				throw new RuntimeException("Unknown HTTP method: " + upnpRequest.getHttpMethodName());
		}
		switch (upnpRequest.getMethod()) {
			case POST:
			case NOTIFY:
				request.setEntity(createEntityProvider(requestMessage));
				break;
			default:
		}

		// prepare default headers
		//request.getHeaders().add(defaultHttpFields);
		// FIXME: what about HTTP2 ?
		if (requestMessage.getOperation().getHttpMinorVersion() == 0) {
			request.setVersion(HttpVersion.HTTP_1_0);
		} else {
			request.setVersion(HttpVersion.HTTP_1_1);
			// This closes the http connection immediately after the call.
			request.addHeader("Connection", "close");
		}

		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			request.setHeader("User-Agent", getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(),
					requestMessage.getUdaMinorVersion()));
		}

		// Headers
		for (Map.Entry<String, List<String>> entry : requestMessage.getHeaders().entrySet()) {
			for (final String value : entry.getValue()) {
				request.setHeader(entry.getKey(), value);
			}
		}

		return request.build();
	}

	@Override
	protected Callable<StreamResponseMessage> createCallable(final StreamRequestMessage requestMessage, HttpUriRequest request) {
		return () -> {
			LOGGER.log(Level.FINE, "Sending HTTP request: {0}", requestMessage);
			try {
				final CloseableHttpResponse httpResponse = httpClient.execute(request);

				LOGGER.log(Level.FINE, "Received HTTP response: {0}", httpResponse.getStatusLine().getReasonPhrase());

				// Status
				final UpnpResponse responseOperation = new UpnpResponse(httpResponse.getStatusLine().getStatusCode(),
						httpResponse.getStatusLine().getReasonPhrase());

				// Message
				final StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

				// Headers
				final Headers headers = new Headers();
				for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
					String key = entry.getKey();
					for (String value : entry.getValue()) {
						headers.add(key, value);
					}
				}

				responseMessage.setHeaders(new UpnpHeaders(headers));

				// Body
				byte[] bytes = null;
				HttpEntity entity = httpResponse.getEntity();
				if (entity != null && entity.getContentLength() > 0) {
					try {
						bytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
					} catch (IOException e) {
					}
				}
				if (bytes == null || 0 == bytes.length) {
					LOGGER.fine("HTTP response message has no entity");
					return responseMessage;
				}

				if (responseMessage.isContentTypeMissingOrText()) {
					LOGGER.fine("HTTP response message contains text entity");
				} else {
					LOGGER.fine("HTTP response message contains binary entity");
				}

				responseMessage.setBodyCharacters(bytes);

				return responseMessage;
			} catch (IOException | UnsupportedOperationException e) {
				if (e instanceof NoHttpResponseException) {
					LOGGER.log(Level.WARNING, "Request: {0} failed : {1}", new Object[]{request, e.getMessage()});
				} else if (e instanceof ClientProtocolException) {
					LOGGER.log(Level.WARNING, "Request: {0} failed : ClientProtocolException", request);
				} else {
					LOGGER.log(Level.WARNING, "Request: {0} failed : {1}", new Object[]{request, e});
				}
				return null;
			}
		};
	}

	@Override
	protected void abort(HttpUriRequest request) {
		request.abort();
	}

	@Override
	protected boolean logExecutionException(Throwable t) {
		if (t instanceof IllegalStateException) {
			// TODO: Document when/why this happens and why we can ignore it, violating the
			// logging rules of the StreamClient#sendRequest() method
			LOGGER.log(Level.FINE, "Illegal state: {0}", t.getMessage());
			return true;
		} else if (t instanceof ClientProtocolException) {
			return true;
		}
		return false;
	}

	@Override
	public void stop() {
		LOGGER.fine("Shutting down HTTP client connection manager/pool");
		try {
			httpClient.close();
		} catch (IOException e) {
			LOGGER.log(Level.INFO, "Shutting down of HTTP client throwed exception : {0}", e);
		}
	}

	protected <O extends UpnpOperation> HttpEntity createEntityProvider(final UpnpMessage<O> upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
			LOGGER.fine("Preparing HTTP request entity as String");
			final String charset = upnpMessage.getContentTypeCharset();
			return new StringEntity(upnpMessage.getBodyString(), charset != null ? charset : "UTF-8");
		} else {
			LOGGER.fine("Preparing HTTP request entity as byte[]");
			return new ByteArrayEntity(upnpMessage.getBodyBytes());
		}
	}

}