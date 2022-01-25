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
package net.pms.network.mediaserver.cling.transport.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.pms.external.StartStopListenerDelegate;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.StreamServer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

public class NettyStreamServer implements StreamServer<StreamServerConfigurationImpl> {

	private static final Logger LOGGER = Logger.getLogger(StreamServer.class.getName());

	final protected StreamServerConfigurationImpl configuration;
	private InetSocketAddress socketAddress;
	private Channel channel;
	private ChannelGroup allChannels;
	private ServerBootstrap bootstrap;

	public NettyStreamServer(StreamServerConfigurationImpl configuration) {
		this.configuration = configuration;
	}

	@Override
	synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
		try {
			socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

			ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);
			allChannels = new DefaultChannelGroup("HTTPServer");
			ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(new NettyBossThreadFactory()),
				Executors.newCachedThreadPool(new NettyWorkerThreadFactory())
			);

			bootstrap = new ServerBootstrap(factory);
			HttpServerPipelineFactory pipeline = new HttpServerPipelineFactory(router);
			bootstrap.setPipelineFactory(pipeline);
			bootstrap.setOption("child.tcpNoDelay", true);
			bootstrap.setOption("child.keepAlive", true);
			bootstrap.setOption("reuseAddress", true);
			bootstrap.setOption("child.reuseAddress", true);
			bootstrap.setOption("child.sendBufferSize", 65536);
			bootstrap.setOption("child.receiveBufferSize", 65536);

			LOGGER.log(Level.INFO, "Created server (for receiving TCP streams) on: {0}", socketAddress);

		} catch (Exception ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
		}
	}

	@Override
	synchronized public int getPort() {
		return socketAddress.getPort();
	}

	@Override
	public StreamServerConfigurationImpl getConfiguration() {
		return configuration;
	}

	@Override
	synchronized public void run() {
		LOGGER.fine("Starting StreamServer...");
		// Starts a new thread but inherits the properties of the calling thread
		try {
			channel = bootstrap.bind(socketAddress);
			allChannels.add(channel);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Another program is using port {0}, which upnp stream server needs.", socketAddress.getPort());
			LOGGER.log(Level.FINE, "The error was: {0}", e);
		}
	}

	@Override
	synchronized public void stop() {
		LOGGER.fine("Stopping StreamServer...");
		if (channel != null) {
			if (allChannels != null) {
				allChannels.close().awaitUninterruptibly();
			}
			LOGGER.log(Level.FINE, "Confirm allChannels is empty: {0}", allChannels.toString());
			bootstrap.releaseExternalResources();
		}
	}

	protected static class RequestUpstreamHandler extends SimpleChannelUpstreamHandler {

		private final Router router;

		public RequestUpstreamHandler(Router router) {
			this.router = router;
		}

		// This is executed in the request receiving thread!
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
			// And we pass control to the service, which will (hopefully) start a new thread immediately so we can
			// continue the receiving thread ASAP

			LOGGER.log(Level.FINE, "Received MessageEvent event: {0} {1}", new Object[]{((HttpRequest) event.getMessage()).getMethod(), ((HttpRequest) event.getMessage()).getUri()});
			router.received(
				new NettyUpnpStream(router.getProtocolFactory(), event)
			);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
			Throwable cause = e.getCause();
			if (cause instanceof TooLongFrameException) {
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}
			if (cause != null) {
				if (cause.getClass().equals(IOException.class)) {
					LOGGER.log(Level.FINE, "Connection error: {0}", cause);
					StartStopListenerDelegate startStopListenerDelegate = (StartStopListenerDelegate) ctx.getAttachment();
					if (startStopListenerDelegate != null) {
						LOGGER.fine("Premature end, stopping...");
						startStopListenerDelegate.stop();
					}
				} else if (!cause.getClass().equals(ClosedChannelException.class)) {
					LOGGER.log(Level.FINE, "Caught exception: {}", cause.getMessage());
					LOGGER.log(Level.FINE, "{}", cause);
				}
			}
			Channel ch = e.getChannel();
			if (ch.isConnected()) {
				sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			}
			ch.close();
		}
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(
			HttpVersion.HTTP_1_1, status);
		response.headers().set(
			HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(
			"Failure: " + status.toString() + "\r\n", StandardCharsets.UTF_8));

		// Close the connection as soon as the error message is sent.
		ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * A {@link ThreadFactory} that creates Netty worker threads.
	 */
	protected static class NettyWorkerThreadFactory implements ThreadFactory {
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
	protected static class NettyBossThreadFactory implements ThreadFactory {
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

	protected static class HttpServerPipelineFactory implements ChannelPipelineFactory {
		private final Router router;

		public HttpServerPipelineFactory(Router router) {
			this.router = router;
		}

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			// Create a default pipeline implementation.
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("decoder", new HttpRequestDecoder());
			pipeline.addLast("aggregator", new HttpChunkAggregator(65536)); // eliminate the need to decode http chunks from the client
			pipeline.addLast("encoder", new HttpResponseEncoder());
			pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
			pipeline.addLast("handler", new RequestUpstreamHandler(router));
			return pipeline;
		}
	}
}
