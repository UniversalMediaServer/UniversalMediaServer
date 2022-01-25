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
package net.pms.network.mediaserver.nettyserver;

import net.pms.network.mediaserver.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.newgui.StatusTab.ConnectionState;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer extends HttpMediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

	private static ChannelGroup allChannels;

	private ChannelFactory factory;
	private Channel channel;
	private ServerBootstrap bootstrap;

	public NettyServer(InetAddress inetAddress, int port) {
		super(inetAddress, port);
	}

	@Override
	public boolean start() throws IOException {
		LOGGER.info("Starting HTTP server (Netty {}) on host {} and port {}", Version.ID, hostname, port);
		InetSocketAddress address = new InetSocketAddress(serverInetAddress, port);
		ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);
		allChannels = new DefaultChannelGroup("HTTPServer");
		factory = new NioServerSocketChannelFactory(
			Executors.newCachedThreadPool(new NettyBossThreadFactory()),
			Executors.newCachedThreadPool(new NettyWorkerThreadFactory())
		);

		bootstrap = new ServerBootstrap(factory);
		HttpServerPipelineFactory pipeline = new HttpServerPipelineFactory(allChannels);
		bootstrap.setPipelineFactory(pipeline);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("reuseAddress", true);
		bootstrap.setOption("child.reuseAddress", true);
		bootstrap.setOption("child.sendBufferSize", 65536);
		bootstrap.setOption("child.receiveBufferSize", 65536);

		try {
			channel = bootstrap.bind(address);
			//if port == 0, it's ephemeral port, so let's MediaServer know the port.
			hostname = ((InetSocketAddress) channel.getLocalAddress()).getAddress().getHostAddress();
			localPort = ((InetSocketAddress) channel.getLocalAddress()).getPort();

			allChannels.add(channel);
		} catch (Exception e) {
			LOGGER.error("Another program is using port " + port + ", which UMS needs.");
			LOGGER.error("You can change the port UMS uses on the General Configuration tab.");
			LOGGER.trace("The error was: " + e);
			PMS.get().getFrame().setConnectionState(ConnectionState.BLOCKED);
		}

		LOGGER.info("HTTP server started on host {} and port {}", hostname, localPort);
		return true;
	}

	@Override
	public synchronized void stop() {
		LOGGER.info("Stopping HTTP server (Netty {}) on host {} and port {}", hostname, localPort);

		/**
		 * Netty v3 (HTTP Engine V2) shutdown approach from
		 * @see https://netty.io/3.8/guide/#start.12
		 */
		if (channel != null) {
			if (allChannels != null) {
				allChannels.close().awaitUninterruptibly();
			}
			LOGGER.debug("Confirm allChannels is empty: " + allChannels.toString());

			bootstrap.releaseExternalResources();
		}
		LOGGER.info("HTTP server stopped");
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
