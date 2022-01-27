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
package net.pms.network.mediaserver.socketchannelserver;

import net.pms.network.mediaserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketChannelServer extends HttpMediaServer implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelServer.class);

	private ServerSocketChannel serverSocketChannel;
	private ServerSocket serverSocket;
	private boolean shouldStop;
	private Thread runnable;

	public SocketChannelServer(InetAddress inetAddress, int port) {
		super(inetAddress, port);
	}

	@Override
	public boolean start() throws IOException {
		shouldStop = false;
		LOGGER.info("Starting HTTP server (SocketChannel) on host {} and port {}...", hostname, port);
		InetSocketAddress address = new InetSocketAddress(serverInetAddress, port);

		serverSocketChannel = ServerSocketChannel.open();

		serverSocket = serverSocketChannel.socket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(address);
		//if port == 0, it's ephemeral port, so let's MediaServer know the port.
		hostname = serverSocket.getInetAddress().getHostAddress();
		localPort = serverSocket.getLocalPort();
		MediaServer.setPort(localPort);

		runnable = new Thread(this, "HTTPv1 Request Handler");
		runnable.setDaemon(false);
		runnable.start();
		LOGGER.info("HTTP server started on host {} and port {}", hostname, localPort);
		return true;
	}

	// avoid a NPE when a) switching HTTP Engine versions and b) restarting the HTTP server
	// by cleaning up based on what's in use (not null) rather than the config state, which
	// might be inconsistent.
	//
	// NOTE: there's little in the way of cleanup to do here as PMS.reset() discards the old
	// server and creates a new one
	@Override
	public synchronized void stop() {
		LOGGER.info("Stopping HTTP server (SocketChannel) on host {} and port {}", hostname, localPort);
		shouldStop = true;

		if (runnable != null) { // HTTP Engine V1
			runnable.interrupt();
		}

		if (serverSocket != null) { // HTTP Engine V1
			try {
				serverSocket.close();
				serverSocketChannel.close();
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
		LOGGER.info("HTTP server stopped");
	}

	// XXX only used by HTTP Engine V1
	@Override
	public void run() {
		LOGGER.trace("Starting Runnable for HTTP server (SocketChannel) on host {} and port {}", hostname, localPort);
		int count = 0;
		while (!shouldStop) {
			try {
				Socket socket = serverSocket.accept();
				InetAddress inetAddress = socket.getInetAddress();
				String ip = inetAddress.getHostAddress();
				// basic IP filter: solntcev at gmail dot com
				boolean ignore = false;

				if (!CONFIGURATION.getIpFiltering().allowed(inetAddress)) {
					ignore = true;
					socket.close();
					LOGGER.trace("Ignoring request from {}:{}" + ip, socket.getPort());
				}

				if (!ignore) {
					if (count == Integer.MAX_VALUE) {
						count = 1;
					} else {
						count++;
					}
					RequestHandler request = new RequestHandler(socket);
					Thread thread = new Thread(request, "HTTPv1 Request Worker " + count);
					thread.start();
				}
			} catch (ClosedByInterruptException e) {
				shouldStop = true;
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			} finally {
				try {
					if (shouldStop && serverSocket != null) {
						serverSocket.close();
					}

					if (shouldStop && serverSocketChannel != null) {
						serverSocketChannel.close();
					}
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		}
	}

}
