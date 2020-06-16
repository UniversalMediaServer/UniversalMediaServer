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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.newgui.StatusTab.ConnectionState;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPServer implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPServer.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private final int port;
	private String hostname;
	private ServerSocketChannel serverSocketChannel;
	private ServerSocket serverSocket;
	private boolean stop;
	private Thread runnable;
	private InetAddress iafinal;
	private ChannelFactory factory;
	private Channel channel;
	private NetworkInterface networkInterface;
	private ChannelGroup group;

	// XXX not used
	@Deprecated
	public InetAddress getIafinal() {
		return iafinal;
	}

	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	// use getNetworkInterface()
	@Deprecated
	public NetworkInterface getNi() {
		return getNetworkInterface();
	}

	public HTTPServer(int port) {
		this.port = port;
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
		hostname = configuration.getServerHostname();
		InetSocketAddress address;

		if (StringUtils.isNotBlank(hostname)) {
			LOGGER.info("Using forced address " + hostname);
			InetAddress tempIA = InetAddress.getByName(hostname);

			if (tempIA != null && networkInterface != null && networkInterface.equals(NetworkInterface.getByInetAddress(tempIA))) {
				address = new InetSocketAddress(tempIA, port);
			} else {
				address = new InetSocketAddress(hostname, port);
			}
		} else if (isAddressFromInterfaceFound(configuration.getNetworkInterface())) { // XXX sets iafinal and networkInterface
			LOGGER.info("Using address {} found on network interface: {}", iafinal, networkInterface.toString().trim().replace('\n', ' '));
			address = new InetSocketAddress(iafinal, port);
		} else {
			LOGGER.info("Using localhost address");
			address = new InetSocketAddress(port);
		}

		LOGGER.info("Created socket: {}", address);

		if (configuration.isHTTPEngineV2()) { // HTTP Engine V2
			ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);
			group = new DefaultChannelGroup("HTTPServer");
			factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(new NettyBossThreadFactory()),
				Executors.newCachedThreadPool(new NettyWorkerThreadFactory())
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
				channel = bootstrap.bind(address);

				group.add(channel);
			} catch (Exception e) {
				LOGGER.error("Another program is using port " + port + ", which UMS needs.");
				LOGGER.error("You can change the port UMS uses on the General Configuration tab.");
				LOGGER.trace("The error was: " + e);
				PMS.get().getFrame().setConnectionState(ConnectionState.BLOCKED);
			}

			if (hostname == null && iafinal != null) {
				hostname = iafinal.getHostAddress();
			} else if (hostname == null) {
				hostname = InetAddress.getLocalHost().getHostAddress();
			}
		} else { // HTTP Engine V1
			serverSocketChannel = ServerSocketChannel.open();

			serverSocket = serverSocketChannel.socket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(address);

			if (hostname == null && iafinal != null) {
				hostname = iafinal.getHostAddress();
			} else if (hostname == null) {
				hostname = InetAddress.getLocalHost().getHostAddress();
			}

			runnable = new Thread(this, "HTTPv1 Request Handler");
			runnable.setDaemon(false);
			runnable.start();
		}

		return true;
	}

	// XXX this sets iafinal and networkInterface
	private boolean isAddressFromInterfaceFound(String networkInterfaceName) {
		NetworkConfiguration.InterfaceAssociation ia = StringUtils.isNotEmpty(networkInterfaceName) ?
			NetworkConfiguration.getInstance().getAddressForNetworkInterfaceName(networkInterfaceName) :
			null;

		if (ia == null) {
			ia = NetworkConfiguration.getInstance().getDefaultNetworkInterfaceAddress();
		}

		if (ia != null) {
			iafinal = ia.getAddr();
			networkInterface = ia.getIface();
		}

		return ia != null;
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

				if (!configuration.getIpFiltering().allowed(inetAddress)) {
					ignore = true;
					socket.close();
					LOGGER.trace("Ignoring request from {}:{}" + ip, socket.getPort());
				}

				if (!ignore) {
					if (count == Integer.MAX_VALUE) {
						count = 1;
					} else
					{
						count++;
					}
					RequestHandler request = new RequestHandler(socket);
					Thread thread = new Thread(request, "HTTPv1 Request Worker " + count);
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

	/**
	 * A {@link ThreadFactory} that creates Netty worker threads.
	 */
	static class NettyWorkerThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		NettyWorkerThreadFactory() {
			group = new ThreadGroup("Netty worker group");
			group.setDaemon(false);
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(group, runnable, "HTTPv2 Request Worker " + threadNumber.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}

	/**
	 * A {@link ThreadFactory} that creates Netty boss threads.
	 */
	static class NettyBossThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);

		NettyBossThreadFactory() {
			group = new ThreadGroup("Netty boss group");
			group.setDaemon(false);
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(group, runnable, "HTTPv2 Request Handler " + threadNumber.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}
}
