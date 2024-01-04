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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import net.pms.PMS;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.UmsUpnpServiceConfiguration;
import net.pms.network.mediaserver.nettyserver.RequestHandlerV2;
import net.pms.service.StartStopListenerDelegate;
import net.pms.util.SimpleThreadFactory;
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
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyStreamServer implements StreamServer<UmsStreamServerConfiguration> {
	//base the logger inside org.jupnp.transport.spi.StreamServer to reflect old behavior
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamServer.class);

	protected final UmsStreamServerConfiguration configuration;
	private InetSocketAddress socketAddress;
	private Channel channel;
	private ChannelGroup allChannels;
	private ServerBootstrap bootstrap;

	public NettyStreamServer(UmsStreamServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public synchronized void init(InetAddress bindAddress, Router router) throws InitializationException {
		try {
			socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

			ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);
			allChannels = new DefaultChannelGroup("JUPnP-HTTPServer");
			ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(
					new SimpleThreadFactory("jupnp-netty-handler", "JUPnP Netty boss group")
				),
				Executors.newCachedThreadPool(
					new SimpleThreadFactory("jupnp-netty-worker", "JUPnP Netty worker group")
				)
			);

			bootstrap = new ServerBootstrap(factory);
			HttpServerPipelineFactory pipeline = new HttpServerPipelineFactory(router, allChannels);
			bootstrap.setPipelineFactory(pipeline);
			bootstrap.setOption("child.tcpNoDelay", true);
			bootstrap.setOption("child.keepAlive", true);
			bootstrap.setOption("reuseAddress", true);
			bootstrap.setOption("child.reuseAddress", true);
			bootstrap.setOption("child.sendBufferSize", 65536);
			bootstrap.setOption("child.receiveBufferSize", 65536);

			LOGGER.info("Created server (for receiving TCP streams) on: {}", socketAddress);

		} catch (Exception ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
		}
	}

	@Override
	public synchronized int getPort() {
		if (channel != null && channel.isBound() && channel.getLocalAddress() instanceof InetSocketAddress) {
			return ((InetSocketAddress) channel.getLocalAddress()).getPort();
		}
		return socketAddress.getPort();
	}

	@Override
	public UmsStreamServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public synchronized void run() {
		LOGGER.debug("Starting StreamServer...");
		// Starts a new thread but inherits the properties of the calling thread
		try {
			channel = bootstrap.bind(socketAddress);
			allChannels.add(channel);
			if (channel != null && channel.isBound() && channel.getLocalAddress() instanceof InetSocketAddress) {
				LOGGER.info("Started StreamServer on: {}", (InetSocketAddress) channel.getLocalAddress());
			} else {
				LOGGER.info("Started StreamServer on: {}", socketAddress);
			}
			if (configuration.getUpdateMediaServerPort()) {
				MediaServer.setPort(getPort());
			}
		} catch (Exception ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
		}
	}

	@Override
	public synchronized void stop() {
		LOGGER.debug("Stopping StreamServer...");
		if (channel != null) {
			if (allChannels != null) {
				allChannels.close().awaitUninterruptibly();
				LOGGER.trace("Confirm allChannels is empty: {}", allChannels.toString());
			}
			bootstrap.releaseExternalResources();
		}
	}

	protected static class RequestUpstreamHandler extends SimpleChannelUpstreamHandler {

		private final Router router;
		private final RequestHandlerV2 requestHandlerV2;
		private final boolean serveContentDirectory;

		public RequestUpstreamHandler(Router router, ChannelGroup allChannels) {
			this.router = router;
			requestHandlerV2 = new RequestHandlerV2(allChannels);
			if (router.getConfiguration() instanceof UmsUpnpServiceConfiguration routerConfiguration) {
				serveContentDirectory = routerConfiguration.useOwnContentDirectory();
			} else {
				serveContentDirectory = false;
			}
		}

		// This is executed in the request receiving thread!
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
			//check inetAddress allowed
			InetSocketAddress remoteAddress = (InetSocketAddress) event.getChannel().getRemoteAddress();
			if (!PMS.getConfiguration().getIpFiltering().allowed(remoteAddress.getAddress())) {
				LOGGER.trace("Ip Filtering denying address: {}", remoteAddress.getAddress().getHostAddress());
				event.getChannel().close();
				return;
			}
			if (requestHandlerV2 != null) {
				//if it's not an HttpRequest, pass it to RequestV2
				if (!(event.getMessage() instanceof HttpRequest)) {
					requestHandlerV2.messageReceived(ctx, event);
					return;
				}
				String uri = ((HttpRequest) event.getMessage()).getUri();
				//check if we want to let JUPnP handle the uri or not.

				//JUPnP use it's own uri formatter ("/dev/<udn>/", "/svc/<udn>/")
				//if not pass it to RequestV2
				if (!uri.startsWith("/dev/") && !uri.startsWith("/svc/")) {
					requestHandlerV2.messageReceived(ctx, event);
					return;
				}
				//lastly we want UMS to respond it's own service ContentDirectory.
				if (!serveContentDirectory && uri.startsWith("/dev/" + PMS.get().udn()) && uri.contains("/ContentDirectory/")) {
					requestHandlerV2.messageReceived(ctx, event);
					return;
				}
			}
			// Here everything is handle by JUPnP
			// We pass control to the service, which will (hopefully) start a new thread immediately so we can
			// continue the receiving thread ASAP

			LOGGER.debug("Received MessageEvent event: {} {}", ((HttpRequest) event.getMessage()).getMethod(), ((HttpRequest) event.getMessage()).getUri());
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
					LOGGER.debug("Connection error: {}", cause);
					StartStopListenerDelegate startStopListenerDelegate = (StartStopListenerDelegate) ctx.getAttachment();
					if (startStopListenerDelegate != null) {
						LOGGER.debug("Premature end, stopping...");
						startStopListenerDelegate.stop();
					}
				} else if (!cause.getClass().equals(ClosedChannelException.class)) {
					LOGGER.debug("Caught exception: {}", cause.getMessage());
					LOGGER.trace("", cause);
				}
			}
			Channel ch = e.getChannel();
			if (ch.isConnected()) {
				sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			}
			ch.close();
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
	}

	protected static class HttpServerPipelineFactory implements ChannelPipelineFactory {
		private final Router router;
		private final ChannelGroup allChannels;

		public HttpServerPipelineFactory(Router router, ChannelGroup allChannels) {
			this.router = router;
			this.allChannels = allChannels;
		}

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			// Create a default pipeline implementation.
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("decoder", new HttpRequestDecoder());
			pipeline.addLast("aggregator", new HttpChunkAggregator(65536)); // eliminate the need to decode http chunks from the client
			pipeline.addLast("encoder", new HttpResponseEncoder());
			pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
			pipeline.addLast("handler", new RequestUpstreamHandler(router, allChannels));
			return pipeline;
		}
	}
}
