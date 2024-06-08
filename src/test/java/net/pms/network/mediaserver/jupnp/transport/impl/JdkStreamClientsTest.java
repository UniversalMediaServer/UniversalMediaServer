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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;

/**
 * Check that JDK HttpServer and JdkStreamClient can handle special upnp methods
 */
public class JdkStreamClientsTest {

	static HttpServer server;
	static String host;

	@BeforeAll
	public static void CreateServer() throws IOException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
		server = HttpServer.create(inetSocketAddress, 0);
		server.createContext("/", new RequestHttpHandler());
		server.setExecutor(executorService);
		server.start();
		host = server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort();
	}

	@AfterAll
	public static void StopServer() throws IOException {
		server.stop(0);
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
	public void testClientConnectTimeout() throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		JdkStreamClientConfiguration configuration = new JdkStreamClientConfiguration(executorService, 1);
		JdkStreamClients clients = new JdkStreamClients(configuration);
		//non routable ip
		StreamRequestMessage requestMessage = new StreamRequestMessage(UpnpRequest.Method.GET, URI.create("http://10.253.252.251/"));

		JdkStreamClient client = clients.createRequest(requestMessage);
		//timeout to 1 second
		long start = System.currentTimeMillis();
		ExecutionException e = assertThrows(ExecutionException.class, () -> {
			client.getResponse(requestMessage);
		});
		assertEquals(e.getCause().getClass(), HttpConnectTimeoutException.class);
		long run = System.currentTimeMillis() - start;
		assertTrue(run < 1500);
	}

	@Test
	public void testClientServerError() throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		JdkStreamClientConfiguration configuration = new JdkStreamClientConfiguration(executorService, 1);
		JdkStreamClients clients = new JdkStreamClients(configuration);
		StreamRequestMessage requestMessage = new StreamRequestMessage(UpnpRequest.Method.GET, URI.create("http://" + host + "/"));
		JdkStreamClient client = clients.createRequest(requestMessage);
		StreamResponseMessage response = client.getResponse(requestMessage);
		assertTrue(response.getOperation().getStatusCode() == 500);
	}

	private void testClientMethod(UpnpRequest.Method upnpMethod, String body) throws InterruptedException, ExecutionException, UnsupportedEncodingException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		JdkStreamClientConfiguration configuration = new JdkStreamClientConfiguration(executorService);
		JdkStreamClients clients = new JdkStreamClients(configuration);
		StreamRequestMessage requestMessage = new StreamRequestMessage(upnpMethod, URI.create("http://" + host + "/"));
		if (body != null) {
			requestMessage.setBody(body);
		}
		JdkStreamClient client = clients.createRequest(requestMessage);
		StreamResponseMessage response = client.getResponse(requestMessage);
		String method = response.getHeaders().getFirstHeader("method");
		assertEquals(method, upnpMethod.getHttpName());
	}

}
