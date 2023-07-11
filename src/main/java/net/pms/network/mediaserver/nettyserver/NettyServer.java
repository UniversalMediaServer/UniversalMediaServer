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
package net.pms.network.mediaserver.nettyserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import net.pms.gui.EConnectionState;
import net.pms.gui.GuiManager;
import net.pms.network.mediaserver.HttpMediaServer;
import net.pms.util.SimpleThreadFactory;
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

	private static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("HTTPServer");

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
		ChannelFactory factory = new NioServerSocketChannelFactory(
			Executors.newCachedThreadPool(
				new SimpleThreadFactory("HTTPv2 Request Handler", "Netty boss group")
			),
			Executors.newCachedThreadPool(
				new SimpleThreadFactory("HTTPv2 Request Worker", "Netty worker group")
			)
		);

		bootstrap = new ServerBootstrap(factory);
		HttpServerPipelineFactory pipeline = new HttpServerPipelineFactory(ALL_CHANNELS);
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

			ALL_CHANNELS.add(channel);
		} catch (Exception e) {
			LOGGER.error("Another program is using port " + port + ", which UMS needs.");
			LOGGER.error("You can change the port UMS uses on the General Configuration tab.");
			LOGGER.trace("The error was: " + e);
			GuiManager.setConnectionState(EConnectionState.BLOCKED);
		}

		LOGGER.info("HTTP server started on host {} and port {}", hostname, localPort);
		return true;
	}

	@Override
	public synchronized void stop() {
		LOGGER.info("Stopping HTTP server (Netty) on host {} and port {}", hostname, localPort);

		/**
		 * Netty v3 (HTTP Engine V2) shutdown approach from
		 * @see https://netty.io/3.8/guide/#start.12
		 */
		if (channel != null) {
			ALL_CHANNELS.close().awaitUninterruptibly();
			LOGGER.debug("Confirm allChannels is empty: " + ALL_CHANNELS.toString());

			bootstrap.releaseExternalResources();
		}
		LOGGER.info("HTTP server stopped");
	}

}
