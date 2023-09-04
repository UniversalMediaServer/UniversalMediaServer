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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.jupnp.http.Headers;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on org.jupnp.transport.impl.apache
 */
public class ApacheStreamClient extends AbstractStreamClient<ApacheStreamClientConfiguration, HttpUriRequestBase> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamClient.class);

	protected final ApacheStreamClientConfiguration configuration;
	protected final PoolingHttpClientConnectionManager clientConnectionManager;
	protected final CloseableHttpClient httpClient;

	public ApacheStreamClient(ApacheStreamClientConfiguration configuration) throws InitializationException {
		this.configuration = configuration;
		ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom();
		// These are some safety settings, we should never run into these timeouts as we
		// do our own expiration checking
		connectionConfigBuilder.setSocketTimeout(Timeout.ofSeconds(configuration.getTimeoutSeconds() + 5));
		connectionConfigBuilder.setConnectTimeout(Timeout.ofSeconds(configuration.getTimeoutSeconds() + 5));

		SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
		if (configuration.getSocketBufferSize() != -1) {
			socketConfigBuilder.setSndBufSize(configuration.getSocketBufferSize());
			socketConfigBuilder.setRcvBufSize(configuration.getSocketBufferSize());
		}

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setExpectContinueEnabled(false);

		// Only register 80, not 443 and SSL
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.build();

		clientConnectionManager = new PoolingHttpClientConnectionManager(registry);
		clientConnectionManager.setMaxTotal(configuration.getMaxTotalConnections());
		clientConnectionManager.setDefaultMaxPerRoute(configuration.getMaxTotalPerRoute());
		clientConnectionManager.setDefaultConnectionConfig(connectionConfigBuilder.build());
		clientConnectionManager.setDefaultSocketConfig(socketConfigBuilder.build());

		HttpRequestRetryStrategy defaultHttpRequestRetryHandler;
		if (configuration.getRequestRetryCount() != -1) {
			defaultHttpRequestRetryHandler = new DefaultHttpRequestRetryStrategy(configuration.getRequestRetryCount(), TimeValue.ofSeconds(1L));
		} else {
			defaultHttpRequestRetryHandler = new DefaultHttpRequestRetryStrategy();
		}

		httpClient = HttpClients
				.custom()
				.setConnectionManager(clientConnectionManager)
				.setDefaultRequestConfig(requestConfigBuilder.build())
				.setRetryStrategy(defaultHttpRequestRetryHandler)
				.build();
	}

	@Override
	public ApacheStreamClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	protected HttpUriRequestBase createRequest(StreamRequestMessage requestMessage) {
		UpnpRequest requestOperation = requestMessage.getOperation();
		HttpUriRequestBase request;
		switch (requestOperation.getMethod()) {
			case GET -> {
				request = new HttpGet(requestOperation.getURI());
			}
			case SUBSCRIBE -> {
				request = new HttpGet(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.SUBSCRIBE.getHttpName();
					}
				};
			}
			case UNSUBSCRIBE -> {
				request = new HttpGet(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.UNSUBSCRIBE.getHttpName();
					}
				};
			}
			case POST -> {
				request = new HttpPost(requestOperation.getURI());
				request.setEntity(createHttpRequestEntity(requestMessage));
			}
			case NOTIFY -> {
				request = new HttpPost(requestOperation.getURI()) {
					@Override
					public String getMethod() {
						return UpnpRequest.Method.NOTIFY.getHttpName();
					}
				};
				request.setEntity(createHttpRequestEntity(requestMessage));
			}
			default -> throw new RuntimeException("Unknown HTTP method: " + requestOperation.getHttpMethodName());
		}

		// Headers
		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			request.setHeader("User-Agent", getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(),
					requestMessage.getUdaMinorVersion()));
		}
		if (requestMessage.getOperation().getHttpMinorVersion() == 0) {
			request.setVersion(HttpVersion.HTTP_1_0);
		} else {
			request.setVersion(HttpVersion.HTTP_1_1);
			// This closes the http connection immediately after the call.
			request.addHeader("Connection", "close");
		}
		addHeaders(request, requestMessage.getHeaders());
		return request;
	}

	@Override
	protected Callable<StreamResponseMessage> createCallable(final StreamRequestMessage requestMessage, final HttpUriRequestBase request) {
		return () -> {
			LOGGER.trace("Sending HTTP request: " + requestMessage);
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamClientRequestMessage(requestMessage);
			}
			return httpClient.execute(request, createResponseHandler(requestMessage));
		};
	}

	@Override
	protected void abort(HttpUriRequestBase request) {
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
		try (clientConnectionManager) {
			LOGGER.trace("Shutting down HTTP client connection manager/pool");
		}
	}

	protected HttpEntity createHttpRequestEntity(UpnpMessage upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
			LOGGER.trace("Preparing HTTP request entity as byte[]");
			return new ByteArrayEntity(upnpMessage.getBodyBytes(), getContentType(upnpMessage));
		} else {
			LOGGER.trace("Preparing HTTP request entity as string");
			return new StringEntity(upnpMessage.getBodyString(), getContentType(upnpMessage));
		}
	}

	private ContentType getContentType(UpnpMessage upnpMessage) {
		ContentTypeHeader contentTypeHeader = upnpMessage.getContentTypeHeader();
		String contentTypeStr = contentTypeHeader.getValue().toStringNoParameters();
		String charsetStr = upnpMessage.getContentTypeCharset();
		Charset charset = null;
		if (charsetStr != null) {
			try {
				charset = Charset.forName(charsetStr);
			} catch (UnsupportedCharsetException e) {
				//no support is available for a requested charset.
			}
		}
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		return ContentType.create(contentTypeStr, charset);
	}

	protected HttpClientResponseHandler<StreamResponseMessage> createResponseHandler(StreamRequestMessage requestMessage) {
		return (final ClassicHttpResponse httpResponse) -> {
			StatusLine statusLine = new StatusLine(httpResponse);
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
		for (Header header : httpMessage.getHeaders()) {
			headers.add(header.getName(), header.getValue());
		}
		return headers;
	}

}
