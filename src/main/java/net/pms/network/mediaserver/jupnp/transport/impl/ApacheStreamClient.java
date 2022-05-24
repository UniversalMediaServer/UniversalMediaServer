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

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.http.NoHttpResponseException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jupnp.http.Headers;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on org.jupnp.transport.impl.apache
 */
public class ApacheStreamClient extends AbstractStreamClient<ApacheStreamClientConfiguration, HttpRequestBase> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamClient.class);

	protected final ApacheStreamClientConfiguration configuration;
	protected final PoolingHttpClientConnectionManager clientConnectionManager;
	protected final CloseableHttpClient httpClient;

	public ApacheStreamClient(ApacheStreamClientConfiguration configuration) throws InitializationException {
		this.configuration = configuration;
		ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom();
		connectionConfigBuilder.setCharset(Charset.forName(configuration.getContentCharset()));
		if (configuration.getSocketBufferSize() != -1) {
			connectionConfigBuilder.setBufferSize(configuration.getSocketBufferSize());
		}

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setExpectContinueEnabled(false);

		// These are some safety settings, we should never run into these timeouts as we
		// do our own expiration checking
		requestConfigBuilder.setConnectTimeout((configuration.getTimeoutSeconds() + 5) * 1000);
		requestConfigBuilder.setSocketTimeout((configuration.getTimeoutSeconds() + 5) * 1000);

		// Only register 80, not 443 and SSL
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.build();

		clientConnectionManager = new PoolingHttpClientConnectionManager(registry);
		clientConnectionManager.setMaxTotal(configuration.getMaxTotalConnections());
		clientConnectionManager.setDefaultMaxPerRoute(configuration.getMaxTotalPerRoute());

		DefaultHttpRequestRetryHandler defaultHttpRequestRetryHandler;
		if (configuration.getRequestRetryCount() != -1) {
			defaultHttpRequestRetryHandler = new DefaultHttpRequestRetryHandler(configuration.getRequestRetryCount(), false);
		} else {
			defaultHttpRequestRetryHandler = new DefaultHttpRequestRetryHandler();
		}

		httpClient = HttpClients
				.custom()
				.setDefaultConnectionConfig(connectionConfigBuilder.build())
				.setConnectionManager(clientConnectionManager)
				.setDefaultRequestConfig(requestConfigBuilder.build())
				.setRetryHandler(defaultHttpRequestRetryHandler)
				.build();
	}

	@Override
	public ApacheStreamClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	protected HttpRequestBase createRequest(StreamRequestMessage requestMessage) {
		UpnpRequest requestOperation = requestMessage.getOperation();
		HttpRequestBase request;
		switch (requestOperation.getMethod()) {
			case GET:
				request = new HttpGet(requestOperation.getURI());
				break;
			case SUBSCRIBE:
				request = new HttpGet(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.SUBSCRIBE.getHttpName();
					}
				};
				break;
			case UNSUBSCRIBE:
				request = new HttpGet(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.UNSUBSCRIBE.getHttpName();
					}
				};
				break;
			case POST:
				request = new HttpPost(requestOperation.getURI());
				((HttpEntityEnclosingRequestBase) request).setEntity(createHttpRequestEntity(requestMessage));
				break;
			case NOTIFY:
				request = new HttpPost(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.NOTIFY.getHttpName();
					}
				};
				((HttpEntityEnclosingRequestBase) request).setEntity(createHttpRequestEntity(requestMessage));
				break;
			default:
				throw new RuntimeException("Unknown HTTP method: " + requestOperation.getHttpMethodName());
		}

		// Headers
		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			request.setHeader("User-Agent", getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(),
					requestMessage.getUdaMinorVersion()));
		}
		if (requestMessage.getOperation().getHttpMinorVersion() == 0) {
			request.setProtocolVersion(HttpVersion.HTTP_1_0);
		} else {
			request.setProtocolVersion(HttpVersion.HTTP_1_1);
			// This closes the http connection immediately after the call.
			request.addHeader("Connection", "close");
		}
		addHeaders(request, requestMessage.getHeaders());
		return request;
	}

	@Override
	protected Callable<StreamResponseMessage> createCallable(final StreamRequestMessage requestMessage, final HttpRequestBase request) {
		return () -> {
			LOGGER.trace("Sending HTTP request: " + requestMessage);
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamClientRequestMessage(requestMessage);
			}
			return httpClient.execute(request, createResponseHandler(requestMessage));
		};
	}

	@Override
	protected void abort(HttpRequestBase request) {
		request.abort();
	}

	@Override
	protected boolean logExecutionException(Throwable t) {
		if (t instanceof IllegalStateException) {
			// TODO: Document when/why this happens and why we can ignore it, violating the
			// logging rules of the StreamClient#sendRequest() method
			LOGGER.trace("Illegal state: " + t.getMessage());
			return true;
		} else if (t instanceof NoHttpResponseException) {
			LOGGER.trace("No Http Response: " + t.getMessage());
			return true;
		}
		return false;
	}

	@Override
	public void stop() {
		LOGGER.trace("Shutting down HTTP client connection manager/pool");
		clientConnectionManager.shutdown();
	}

	protected HttpEntity createHttpRequestEntity(UpnpMessage upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
			LOGGER.trace("Preparing HTTP request entity as byte[]");
			return new ByteArrayEntity(upnpMessage.getBodyBytes());
		} else {
			LOGGER.trace("Preparing HTTP request entity as string");
			String charset = upnpMessage.getContentTypeCharset();
			if (charset == null) {
				charset = "UTF-8";
			}
			try {
				return new StringEntity(upnpMessage.getBodyString(), charset);
			} catch (UnsupportedCharsetException ex) {
				LOGGER.trace("HTTP request does not support charset: {}", charset);
				throw new RuntimeException(ex);
			}
		}
	}

	protected ResponseHandler<StreamResponseMessage> createResponseHandler(StreamRequestMessage requestMessage) {
		return (final HttpResponse httpResponse) -> {
			StatusLine statusLine = httpResponse.getStatusLine();
			LOGGER.trace("Received HTTP response: " + statusLine);

			// Status
			UpnpResponse responseOperation = new UpnpResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase());

			// Message
			StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

			// Headers
			responseMessage.setHeaders(new UpnpHeaders(getHeaders(httpResponse)));

			// Body
			HttpEntity entity = httpResponse.getEntity();
			if (entity == null || entity.getContentLength() == 0) {
				LOGGER.trace("HTTP response message has no entity");
				return responseMessage;
			}

			byte[] data = EntityUtils.toByteArray(entity);
			if (data != null) {
				if (responseMessage.isContentTypeMissingOrText()) {
					LOGGER.trace("HTTP response message contains text entity");
					responseMessage.setBodyCharacters(data);
				} else {
					LOGGER.trace("HTTP response message contains binary entity");
					responseMessage.setBody(UpnpMessage.BodyType.BYTES, data);
				}
			} else {
				LOGGER.trace("HTTP response message has no entity");
			}
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamClientResponseMessage(responseMessage, requestMessage);
			}
			return responseMessage;
		};
	}

	private static void addHeaders(HttpMessage httpMessage, Headers headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			for (String value : entry.getValue()) {
				httpMessage.addHeader(entry.getKey(), value);
			}
		}
	}

	private static Headers getHeaders(HttpMessage httpMessage) {
		Headers headers = new Headers();
		for (Header header : httpMessage.getAllHeaders()) {
			headers.add(header.getName(), header.getValue());
		}
		return headers;
	}
}
