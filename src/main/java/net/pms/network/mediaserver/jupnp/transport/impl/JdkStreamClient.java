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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jupnp.http.Headers;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkStreamClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdkStreamClient.class);

	protected final JdkStreamClientConfiguration configuration;
	protected final HttpClient httpClient;
	protected final HttpRequest httpRequest;

	protected CompletableFuture<HttpResponse<byte[]>> httpResponse;

	public JdkStreamClient(StreamRequestMessage requestMessage, JdkStreamClientConfiguration configuration) throws InitializationException {
		this.configuration = configuration;
		httpClient = HttpClient
				.newBuilder()
				.executor(configuration.getRequestExecutorService())
				.connectTimeout(Duration.ofSeconds(configuration.getTimeoutSeconds()))
				.version(HttpClient.Version.HTTP_1_1)
				.build();
		httpRequest = createRequest(requestMessage);
	}

	private HttpRequest createRequest(StreamRequestMessage requestMessage) {
		UpnpRequest requestOperation = requestMessage.getOperation();
		HttpRequest.Builder request;
		switch (requestOperation.getMethod()) {
			case GET -> {
				request = HttpRequest.newBuilder()
						.GET()
						.uri(requestOperation.getURI());
			}
			case SUBSCRIBE, UNSUBSCRIBE -> {
				request = HttpRequest.newBuilder()
						.method(requestOperation.getMethod().getHttpName(), BodyPublishers.noBody())
						.uri(requestOperation.getURI());
			}
			case POST -> {
				request = HttpRequest.newBuilder()
						.POST(createHttpRequestBodyPublisher(requestMessage))
						.uri(requestOperation.getURI());
			}
			case NOTIFY -> {
				request = HttpRequest.newBuilder()
						.method(requestOperation.getMethod().getHttpName(), createHttpRequestBodyPublisher(requestMessage))
						.uri(requestOperation.getURI());
			}
			default ->
				throw new RuntimeException("Unknown HTTP method: " + requestOperation.getHttpMethodName());
		}

		// Headers
		// Add the default user agent if not already set on the message
		if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
			request = request.setHeader("User-Agent", configuration.getUserAgentValue(requestMessage.getUdaMajorVersion(),
					requestMessage.getUdaMinorVersion()));
		}
		addHeaders(request, requestMessage.getHeaders());
		return request.build();
	}

	protected StreamResponseMessage getResponse(StreamRequestMessage requestMessage) throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		LOGGER.trace("Sending HTTP request: " + requestMessage);
		if (LOGGER.isTraceEnabled()) {
			StreamsLoggerHelper.logStreamClientRequestMessage(requestMessage);
		}
		httpResponse = httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray());
		HttpResponse<byte[]> response = httpResponse.get();
		LOGGER.trace("Received HTTP response: " + response.statusCode());
		// Status
		UpnpResponse responseOperation = new UpnpResponse(response.statusCode(), "");
		// Message
		StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);
		// Headers
		responseMessage.setHeaders(new UpnpHeaders(getHeaders(response)));

		// Body
		byte[] body = response.body();
		if (body == null || body.length == 0) {
			LOGGER.trace("HTTP response message has no body");
			return responseMessage;
		}
		if (responseMessage.isContentTypeMissingOrText()) {
			LOGGER.trace("HTTP response message contains text entity");
			responseMessage.setBodyCharacters(body);
		} else {
			LOGGER.trace("HTTP response message contains binary entity");
			responseMessage.setBody(UpnpMessage.BodyType.BYTES, body);
		}
		if (LOGGER.isTraceEnabled()) {
			StreamsLoggerHelper.logStreamClientResponseMessage(responseMessage, requestMessage);
		}
		return responseMessage;
	}

	protected void cancel() {
		if (httpResponse != null) {
			httpResponse.cancel(true);
		}
	}

	protected BodyPublisher createHttpRequestBodyPublisher(UpnpMessage upnpMessage) {
		if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
			LOGGER.trace("Preparing HTTP request entity as byte[]");
			return HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(upnpMessage.getBodyBytes()));
		} else {
			LOGGER.trace("Preparing HTTP request entity as string");
			return HttpRequest.BodyPublishers.ofString(upnpMessage.getBodyString());
		}
	}

	private static void addHeaders(HttpRequest.Builder builder, Headers headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			for (String value : entry.getValue()) {
				builder = builder.header(entry.getKey(), value);
			}
		}
	}

	private static Headers getHeaders(HttpResponse<?> httpResponse) {
		Headers headers = new Headers();
		if (httpResponse != null && httpResponse.headers() != null) {
			headers.putAll(httpResponse.headers().map());
		}
		return headers;
	}

}
