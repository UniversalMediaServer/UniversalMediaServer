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
package net.pms.network.mediaserver.jupnp.transport.impl.jetty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.pms.network.mediaserver.jupnp.transport.impl.StreamsLoggerHelper;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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
 * Implementation based on <a href="http://www.eclipse.org/jetty/">Jetty Client
 * 12.x</a>.
 *
 * @author Surf@ceS
 */
public class JettyStreamClientImpl extends AbstractStreamClient<StreamClientConfigurationImpl, Request> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JettyStreamClientImpl.class);

	protected final StreamClientConfigurationImpl configuration;
	protected final HttpClient httpClient;

	public JettyStreamClientImpl(StreamClientConfigurationImpl configuration) throws InitializationException {
		this.configuration = configuration;

		httpClient = new HttpClient();

		// These are some safety settings, we should never run into these timeouts as we
		// do our own expiration checking
		httpClient.setConnectTimeout((configuration.getTimeoutSeconds()) * 1000);
		httpClient.setMaxConnectionsPerDestination(2);

		int cpus = Runtime.getRuntime().availableProcessors();
		int maxThreads = 5 * cpus;

		final QueuedThreadPool queuedThreadPool = createThreadPool("jupnp-stream-client", 5, maxThreads, 60000);

		httpClient.setExecutor(queuedThreadPool);

		if (configuration.getSocketBufferSize() != -1) {
			httpClient.setRequestBufferSize(configuration.getSocketBufferSize());
			httpClient.setResponseBufferSize(configuration.getSocketBufferSize());
		}

		try {
			LOGGER.info("Starting Jetty HTTP client v{} connection manager/pool", Jetty.VERSION);
			httpClient.start();
		} catch (final Exception e) {
			LOGGER.error("Failed to instantiate HTTP client", e);
			throw new InitializationException("Failed to instantiate HTTP client", e);
		}
	}

	@Override
	public StreamClientConfigurationImpl getConfiguration() {
		return configuration;
	}

	@Override
	protected Request createRequest(StreamRequestMessage requestMessage) {
		final UpnpRequest upnpRequest = requestMessage.getOperation();

		LOGGER.trace("Creating HTTP request. URI: '{}' method: '{}'", upnpRequest.getURI(), upnpRequest.getMethod());
		Request request;
		switch (upnpRequest.getMethod()) {
			case GET, SUBSCRIBE, UNSUBSCRIBE -> {
				try {
					request = httpClient.newRequest(upnpRequest.getURI()).method(upnpRequest.getHttpMethodName());
				} catch (IllegalArgumentException e) {
					LOGGER.debug("Cannot create request because URI '{}' is invalid", upnpRequest.getURI(), e);
					return null;
				}
			}
			case POST, NOTIFY -> {
				try {
					request = httpClient.newRequest(upnpRequest.getURI()).method(upnpRequest.getHttpMethodName());
					request.body(createContentProvider(requestMessage));
				} catch (IllegalArgumentException e) {
					LOGGER.debug("Cannot create request because URI '{}' is invalid", upnpRequest.getURI(), e);
					return null;
				}
			}
			default ->
				throw new RuntimeException("Unknown HTTP method: " + upnpRequest.getHttpMethodName());
		}

		// FIXME: what about HTTP2 ?
		if (requestMessage.getOperation().getHttpMinorVersion() == 0) {
			request.version(HttpVersion.HTTP_1_0);
		} else {
			request.version(HttpVersion.HTTP_1_1);
			// This closes the http connection immediately after the call.
			//
			// Even though jetty client is able to close connections properly,
			// it still takes ~30 seconds to do so. This may cause too many
			// connections for installations with many upnp devices.
			addHeader(request, HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.asString());
		}

		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			request.agent(getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(), requestMessage.getUdaMinorVersion()));
		}

		// Headers
		addHeaders(request, requestMessage.getHeaders());

		return request;
	}

	@Override
	protected Callable<StreamResponseMessage> createCallable(final StreamRequestMessage requestMessage, final Request request) {
		return () -> {
			LOGGER.trace("Sending HTTP request: {}", requestMessage);
			if (LOGGER.isTraceEnabled()) {
				StreamsLoggerHelper.logStreamClientRequestMessage(requestMessage);
			}
			try {
				final ContentResponse httpResponse = request.send();

				LOGGER.trace("Received HTTP response: {}", httpResponse.getReason());

				// Status
				final UpnpResponse responseOperation = new UpnpResponse(httpResponse.getStatus(), httpResponse.getReason());

				// Message
				final StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

				// Headers
				responseMessage.setHeaders(new UpnpHeaders(getHeaders(httpResponse)));

				// Body
				final byte[] bytes = httpResponse.getContent();
				if (bytes == null || 0 == bytes.length) {
					LOGGER.trace("HTTP response message has no entity");
					if (LOGGER.isTraceEnabled()) {
						StreamsLoggerHelper.logStreamClientResponseMessage(responseMessage, requestMessage);
					}
					return responseMessage;
				}

				if (responseMessage.isContentTypeMissingOrText()) {
					LOGGER.trace("HTTP response message contains text entity");
				} else {
					LOGGER.trace("HTTP response message contains binary entity");
				}

				responseMessage.setBodyCharacters(bytes);
				if (LOGGER.isTraceEnabled()) {
					StreamsLoggerHelper.logStreamClientResponseMessage(responseMessage, requestMessage);
				}
				return responseMessage;
			} catch (final RuntimeException e) {
				LOGGER.error("Request: {} failed", request, e);
				throw e;
			}
		};
	}

	@Override
	protected void abort(Request request) {
		request.abort(new Exception("Request aborted by API"));
	}

	@Override
	protected boolean logExecutionException(Throwable t) {
		if (t instanceof IllegalStateException) {
			// TODO: Document when/why this happens and why we can ignore it, violating the
			// logging rules of the StreamClient#sendRequest() method
			LOGGER.trace("Illegal state: {}", t.getMessage());
			return true;
		} else if (t != null && t.getMessage().contains("HTTP protocol violation")) {
			SpecificationViolationReporter.report(t.getMessage());
			return true;
		}
		return false;
	}

	@Override
	public void stop() {
		LOGGER.trace("Shutting down HTTP client connection manager/pool");
		try {
			httpClient.stop();
		} catch (Exception e) {
			LOGGER.info("Shutting down of HTTP client throwed exception", e);
		}
	}

	protected <O extends UpnpOperation> Request.Content createContentProvider(final UpnpMessage<O> upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
			LOGGER.trace("Preparing HTTP request entity as String");
			return new StringRequestContent(upnpMessage.getBodyString(), upnpMessage.getContentTypeCharset());
		} else {
			LOGGER.trace("Preparing HTTP request entity as byte[]");
			return new BytesRequestContent(upnpMessage.getBodyBytes());
		}
	}

	private QueuedThreadPool createThreadPool(String consumerName, int minThreads, int maxThreads, int keepAliveTimeout) {
		QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreads, minThreads, keepAliveTimeout);
		queuedThreadPool.setName(consumerName);
		queuedThreadPool.setDaemon(true);
		return queuedThreadPool;
	}

	/**
	 * Add all jUPnP {@link Headers} header information to {@link Request}.
	 *
	 * @param request to enrich with header information
	 * @param headers to be added to the {@link Request}
	 */
	public static void addHeaders(final Request request, final Headers headers) {
		final HttpFields httpFields = request.getHeaders();
		if (httpFields instanceof HttpFields.Mutable httpHeaders) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				for (final String value : entry.getValue()) {
					httpHeaders.add(entry.getKey(), value);
				}
			}
		}
	}

	/**
	 * Add a {@link HttpHeader} header information to {@link Request}.
	 *
	 * @param request to enrich with header information
	 * @param header to be added to the {@link Request}
	 * @param value of the header, null to delete
	 */
	public static void addHeader(final Request request, final HttpHeader header, final String value) {
		final HttpFields httpFields = request.getHeaders();
		if (httpFields instanceof HttpFields.Mutable httpHeaders) {
			httpHeaders.add(header, value);
		}
	}

	/**
	 * Get all header information from {@link Response} jUPnP {@link Headers}.
	 *
	 * @param response {@link Response}, must not be null
	 * @return {@link Headers}, never {@code null}
	 */
	public static Headers getHeaders(final Response response) {
		final Headers headers = new Headers();
		for (HttpField httpField : response.getHeaders()) {
			headers.add(httpField.getName(), httpField.getValue());
		}

		return headers;
	}

}
