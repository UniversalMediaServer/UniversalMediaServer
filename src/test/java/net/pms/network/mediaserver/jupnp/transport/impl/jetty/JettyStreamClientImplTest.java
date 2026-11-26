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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.jetty.client.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.StreamClient;

/**
 * Check that Jetty Http Client can handle special upnp methods
 */
public class JettyStreamClientImplTest {

	static HttpServer server;
	static String host;
	static ExecutorService serverExecutor;
	private final List<StreamClient> clientsToStop = new ArrayList<>();
	private final List<ExecutorService> executorsToStop = new ArrayList<>();

	@AfterEach
	public void stopClients() {
		try {
			clientsToStop.forEach(StreamClient::stop);
		} finally {
			executorsToStop.forEach(ExecutorService::shutdownNow);
		}
	}

	private ExecutorService newExecutor() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		executorsToStop.add(executor);
		return executor;
	}

	private JettyStreamClientImpl newClient(StreamClientConfigurationImpl configuration) {
		JettyStreamClientImpl client = new JettyStreamClientImpl(configuration);
		clientsToStop.add(client);
		return client;
	}

	@BeforeAll
	public static void CreateServer() throws IOException {
		serverExecutor = Executors.newFixedThreadPool(10);
		InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
		server = HttpServer.create(inetSocketAddress, 0);
		server.createContext("/", new RequestHttpHandler());
		server.setExecutor(serverExecutor);
		server.start();
		host = server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort();
	}

	@AfterAll
	public static void StopServer() throws IOException {
		try {
			server.stop(0);
		} finally {
			serverExecutor.shutdownNow();
		}
	}

	protected static class RequestHttpHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (UpnpRequest.Method.SUBSCRIBE.getHttpName().equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("method", UpnpRequest.Method.SUBSCRIBE.getHttpName());
				exchange.sendResponseHeaders(200, -1);
			} else if (UpnpRequest.Method.UNSUBSCRIBE.getHttpName().equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("method", UpnpRequest.Method.UNSUBSCRIBE.getHttpName());
				exchange.sendResponseHeaders(200, -1);
			} else if (UpnpRequest.Method.NOTIFY.getHttpName().equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("method", UpnpRequest.Method.NOTIFY.getHttpName());
				exchange.sendResponseHeaders(200, -1);
			} else {
				exchange.sendResponseHeaders(500, -1);
			}
		}

	}

	@Test
	public void testClientSubscribeMethod() {
		assertDoesNotThrow(() -> {
			testClientMethod(UpnpRequest.Method.SUBSCRIBE, null);
		});
	}

	@Test
	public void testClientUnsubscribeMethod() {
		assertDoesNotThrow(() -> {
			testClientMethod(UpnpRequest.Method.UNSUBSCRIBE, null);
		});
	}

	@Test
	public void testClientNotifyMethod() {
		assertDoesNotThrow(() -> {
			testClientMethod(UpnpRequest.Method.NOTIFY, "test");
		});
	}

	@Test
	public void testClientRequestTimeout() {
		ExecutorService executorService = newExecutor();
		StreamClientConfigurationImpl configuration = new StreamClientConfigurationImpl(executorService, 1);
		StreamClient client = JettyTransportConfiguration.INSTANCE.createStreamClient(executorService, configuration);
		clientsToStop.add(client);
		CountDownLatch received = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		server.createContext("/timeout", exchange -> {
			received.countDown();
			try {
				release.await();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			} finally {
				exchange.close();
			}
		});
		try {
			StreamRequestMessage message = new StreamRequestMessage(UpnpRequest.Method.GET, URI.create("http://" + host + "/timeout"));
			// A connected server deliberately sends no response. The UMS request
			// deadline must end the call; five seconds is only a deadlock guard.
			assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertNull(client.sendRequest(message)));
			assertEquals(0, received.getCount(), "Request must reach the local server");
		} finally {
			release.countDown();
			server.removeContext("/timeout");
		}
	}

	@Test
	public void testJettyClientConnectTimeoutConfiguration() {
		for (int seconds : new int[]{1, 3}) {
			JettyStreamClientImpl client = newClient(new StreamClientConfigurationImpl(newExecutor(), seconds));
			assertEquals(seconds * 1000L, client.httpClient.getConnectTimeout());
		}
	}

	@Test
	public void testJettyClientPropagatesConnectTimeout() {
		JettyStreamClientImpl client = newClient(new StreamClientConfigurationImpl(newExecutor(), 1));
		StreamRequestMessage message = new StreamRequestMessage(UpnpRequest.Method.GET, URI.create("http://127.0.0.1/"));
		SocketTimeoutException timeout = new SocketTimeoutException("Controlled connect timeout");
		Request request = (Request) Proxy.newProxyInstance(Request.class.getClassLoader(), new Class<?>[]{Request.class}, (proxy, method, arguments) -> {
			if (method.getName().equals("send") && method.getParameterCount() == 0) {
				throw new ExecutionException(timeout);
			}
			throw new AssertionError("Unexpected request method: " + method);
		});
		ExecutionException exception = assertThrows(ExecutionException.class, () -> client.createCallable(message, request).call());
		assertSame(timeout, exception.getCause());
	}

	@Test
	public void testClientServerError() {
		ExecutorService executorService = newExecutor();
		StreamClientConfigurationImpl configuration = new StreamClientConfigurationImpl(executorService, 1);
		JettyStreamClientImpl clients = newClient(configuration);
		StreamRequestMessage requestMessage = new StreamRequestMessage(UpnpRequest.Method.GET, URI.create("http://" + host + "/"));
		Request request = clients.createRequest(requestMessage);
		try {
			StreamResponseMessage response = clients.createCallable(requestMessage, request).call();
			assertTrue(response.getOperation().getStatusCode() == 500);
		} catch (Exception ex) {
			fail(ex);
		}
	}

	private void testClientMethod(UpnpRequest.Method upnpMethod, String body) {
		ExecutorService executorService = newExecutor();
		StreamClientConfigurationImpl configuration = new StreamClientConfigurationImpl(executorService);
		JettyStreamClientImpl clients = newClient(configuration);
		StreamRequestMessage requestMessage = new StreamRequestMessage(upnpMethod, URI.create("http://" + host + "/"));
		if (body != null) {
			requestMessage.setBody(body);
		}
		Request request = clients.createRequest(requestMessage);
		StreamResponseMessage response;
		try {
			response = clients.createCallable(requestMessage, request).call();
			String method = response.getHeaders().getFirstHeader("method");
			assertEquals(method, upnpMethod.getHttpName());
		} catch (Exception ex) {
			fail(ex);
		}
	}

}
