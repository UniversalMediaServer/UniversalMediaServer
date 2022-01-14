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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
import org.jupnp.http.Headers;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpOperation;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache StreamClient as we have a dependency on it.
 */
public class ApacheStreamClientImpl extends AbstractStreamClient<StreamClientConfigurationImpl, HttpUriRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamClient.class);

	protected final StreamClientConfigurationImpl configuration;
	protected final CloseableHttpClient httpClient;

	public ApacheStreamClientImpl(StreamClientConfigurationImpl configuration) throws InitializationException {
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

		LOGGER.trace("Creating HTTP request. URI: '{}' method: '{}'", upnpRequest.getURI(), upnpRequest.getMethod());
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
			LOGGER.trace("Sending HTTP request: {}", requestMessage);
			try {
				final CloseableHttpResponse httpResponse = httpClient.execute(request);

				LOGGER.trace("Received HTTP response: {}", httpResponse.getStatusLine().getReasonPhrase());

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
					LOGGER.trace("HTTP response message has no entity");
					return responseMessage;
				}

				if (responseMessage.isContentTypeMissingOrText()) {
					LOGGER.trace("HTTP response message contains text entity");
				} else {
					LOGGER.trace("HTTP response message contains binary entity");
				}

				responseMessage.setBodyCharacters(bytes);

				return responseMessage;
			} catch (IOException | UnsupportedOperationException e) {
				if (e instanceof NoHttpResponseException) {
					LOGGER.error("Request: {} failed : {}", request, e.getMessage());
				} else if (e instanceof ClientProtocolException) {
					LOGGER.error("Request: {} failed : {}", request, "ClientProtocolException");
				} else {
					LOGGER.error("Request: {} failed", request, e);
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
			LOGGER.trace("Illegal state: {}", t.getMessage());
			return true;
		} else if (t instanceof ClientProtocolException) {
			SpecificationViolationReporter.report(t.getMessage());
			return true;
		}
		return false;
	}

	@Override
	public void stop() {
		LOGGER.trace("Shutting down HTTP client connection manager/pool");
		try {
			httpClient.close();
		} catch (IOException e) {
			LOGGER.info("Shutting down of HTTP client throwed exception", e);
		}
	}

	protected <O extends UpnpOperation> HttpEntity createEntityProvider(final UpnpMessage<O> upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
			LOGGER.trace("Preparing HTTP request entity as String");
			final String charset = upnpMessage.getContentTypeCharset();
			return new StringEntity(upnpMessage.getBodyString(), charset != null ? charset : "UTF-8");
		} else {
			LOGGER.trace("Preparing HTTP request entity as byte[]");
			return new ByteArrayEntity(upnpMessage.getBodyBytes());
		}
	}

}
