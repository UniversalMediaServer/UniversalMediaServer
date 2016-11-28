/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.NetworkConfiguration.InterfaceAssociation;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPServer implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPServer.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private final int port;
	private final String hostname;
	private final InetSocketAddress socketAddress;
	private final List<NetworkInterface> networkInterfaces = new ArrayList<>();
	private ServerSocketChannel serverSocketChannel;
	private ServerSocket serverSocket;
	private boolean stop;
	private Thread runnable;
	private ChannelFactory factory;
	private Channel channel;
	private ChannelGroup group;

	/**
	 * @return The network interface(s) used by this server instance
	 */
	public List<NetworkInterface> getNetworkInterfaces() {
		return new ArrayList<>(networkInterfaces);
	}

	/**
	 * @return The socketAddress used by this server instance
	 */
	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	public HTTPServer() {
		int tmpPort = configuration.getServerPort();
		if (tmpPort < 1 || tmpPort > 65535) {
			LOGGER.warn("Configured HTTP port outside of valid range (1-65535), using default port ({}) instead", PmsConfiguration.DEFAULT_SERVER_PORT);
			port = PmsConfiguration.DEFAULT_SERVER_PORT;
		} else {
			port = tmpPort;
		}

		String tmpHostname = configuration.getServerHostname();
		InetSocketAddress tmpSocketAddress = null;

		if (StringUtils.isNotBlank(tmpHostname)) {
			LOGGER.info("Using forced address \"{}\"", tmpHostname);
			InetAddress inetAddress = null;
			try {
				inetAddress = InetAddress.getByName(tmpHostname);
			} catch (UnknownHostException e) {
				tmpHostname = null;
				LOGGER.error("Hostname {} cannot be resolved, ignoring parameter: {}", tmpHostname, e.getMessage());
				LOGGER.trace("", e);
			}

			if (inetAddress != null) {
				LOGGER.info("Using address {} resolved from \"{}\"", inetAddress.getHostAddress(), tmpHostname);
				tmpSocketAddress = new InetSocketAddress(inetAddress, port);
				networkInterfaces.addAll(NetworkConfiguration.getInstance().getNetworkInterfaces(inetAddress));
			}
		}

		if (tmpSocketAddress == null) {
			InterfaceAssociation interfaceAssociation = null;
			if (StringUtils.isNotEmpty(configuration.getNetworkInterface())) {
				interfaceAssociation = NetworkConfiguration.getInstance().getAddressForNetworkInterfaceName(configuration.getNetworkInterface());
			}

			if (interfaceAssociation != null) {
				InetAddress inetAddress = interfaceAssociation.getAddr();
				networkInterfaces.add(interfaceAssociation.getIface());
				LOGGER.info(
					"Using address {} found on network interface: {}",
					inetAddress,
					interfaceAssociation.getIface().toString().trim().replace('\n', ' ')
				);
				socketAddress = new InetSocketAddress(inetAddress, port);
			} else {
				networkInterfaces.addAll(NetworkConfiguration.getInstance().getRelevantNetworkInterfaces());
				LOGGER.info("Using all addresses");
				socketAddress = new InetSocketAddress(port);
			}
		} else {
			socketAddress = tmpSocketAddress;
		}

		if (StringUtils.isBlank(tmpHostname) && socketAddress != null) {
			if (socketAddress.getAddress().isAnyLocalAddress()) {
				try {
					tmpHostname = InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException e) {
					tmpHostname = InetAddress.getLoopbackAddress().getHostName();
				}
			} else {
				tmpHostname = socketAddress.getHostString();
			}
		}
		hostname = tmpHostname;
	}

	public String getURL() {
		return "http://" + hostname + ":" + port;
	}

	public String getHost() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public boolean start() throws IOException {

		LOGGER.info("Creating socket: {}", socketAddress);

		if (configuration.isHTTPEngineV2()) { // HTTP Engine V2
			group = new DefaultChannelGroup("myServer");
			factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()
			);

			ServerBootstrap bootstrap = new ServerBootstrap(factory);
			HttpServerPipelineFactory pipeline = new HttpServerPipelineFactory(group);
			bootstrap.setPipelineFactory(pipeline);
			bootstrap.setOption("child.tcpNoDelay", true);
			bootstrap.setOption("child.keepAlive", true);
			bootstrap.setOption("reuseAddress", true);
			bootstrap.setOption("child.reuseAddress", true);
			bootstrap.setOption("child.sendBufferSize", 65536);
			bootstrap.setOption("child.receiveBufferSize", 65536);

			try {
				channel = bootstrap.bind(socketAddress);

				group.add(channel);
			} catch (Exception e) {
				LOGGER.error("Another program is using port " + port + ", which UMS needs.");
				LOGGER.error("You can change the port UMS uses on the General Configuration tab.");
				LOGGER.trace("The error was: " + e);
				PMS.get().getFrame().setStatusCode(0, Messages.getString("PMS.141"), "icon-status-warning.png");
			}
		} else { // HTTP Engine V1
			serverSocketChannel = ServerSocketChannel.open();

			serverSocket = serverSocketChannel.socket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(socketAddress);

			runnable = new Thread(this, "HTTP Server");
			runnable.setDaemon(false);
			runnable.start();
		}

		return true;
	}

	// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=10689&p=48811#p48811
	//
	// avoid a NPE when a) switching HTTP Engine versions and b) restarting the HTTP server
	// by cleaning up based on what's in use (not null) rather than the config state, which
	// might be inconsistent.
	//
	// NOTE: there's little in the way of cleanup to do here as PMS.reset() discards the old
	// server and creates a new one
	public void stop() {
		LOGGER.info("Stopping server on host {} and port {}...", hostname, port);

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

		if (channel != null) { // HTTP Engine V2
			if (group != null) {
				group.close().awaitUninterruptibly();
			}

			if (factory != null) {
				factory.releaseExternalResources();
			}
		}

		NetworkConfiguration.forgetConfiguration();
	}

	// XXX only used by HTTP Engine V1
	@Override
	public void run() {
		LOGGER.info("Starting DLNA Server on host {} and port {}...", hostname, port);

		int count = 0;
		while (!stop) {
			try {
				Socket socket = serverSocket.accept();
				InetAddress inetAddress = socket.getInetAddress();
				String ip = inetAddress.getHostAddress();
				// basic IP filter: solntcev at gmail dot com
				boolean ignore = false;

				if (configuration.getIpFiltering().allowed(inetAddress)) {
					LOGGER.trace("Receiving a request from: " + ip);
				} else {
					ignore = true;
					socket.close();
					LOGGER.trace("Ignoring request from: " + ip);
				}

				if (!ignore) {
					if (count == Integer.MAX_VALUE) {
						count = 1;
					} else
					{
						count++;
					}
					RequestHandler request = new RequestHandler(socket);
					Thread thread = new Thread(request, "Request Handler " + count);
					thread.start();
				}
			} catch (ClosedByInterruptException e) {
				stop = true;
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			} finally {
				try {
					if (stop && serverSocket != null) {
						serverSocket.close();
					}

					if (stop && serverSocketChannel != null) {
						serverSocketChannel.close();
					}
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		}
	}
}
