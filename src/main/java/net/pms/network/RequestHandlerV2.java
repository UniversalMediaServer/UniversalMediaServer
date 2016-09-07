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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.StartStopListenerDelegate;
import net.pms.remote.RemoteUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandlerV2 extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerV2.class);

	private static final Pattern TIMERANGE_PATTERN = Pattern.compile(
		"timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d.:]+)?-?([\\d.:]+)?",
		Pattern.CASE_INSENSITIVE
	);
	
	private static int BUFFER_SIZE = 8 * 1024;

	private volatile FullHttpRequest nettyRequest;
	private RendererConfiguration renderer = null;

//	private final ChannelGroup group;
//
//	public RequestHandlerV2(ChannelGroup group) {
//		this.group = group;
//	}

	// Used to filter out known headers when the renderer is not recognized
	private final static String[] KNOWN_HEADERS = {
		"accept",
		"accept-language",
		"accept-encoding",
		"callback",
		"connection",
		"content-length",
		"content-type",
		"date",
		"host",
		"nt",
		"sid",
		"timeout",
		"cache-control",
		"user-agent"
	};

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest e)
		throws Exception {
		RequestV2 request = null;
		String userAgentString = null;
		ArrayList<String> identifiers = new ArrayList<>();

		FullHttpRequest nettyRequest = this.nettyRequest = e;

		InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		InetAddress ia = remoteAddress.getAddress();

		// Is the request from our own Cling service, i.e. self-originating?
		String ua = nettyRequest.headers().get(HttpHeaderNames.USER_AGENT);
		boolean isSelf = ia.getHostAddress().equals(PMS.get().getServer().getHost()) &&
			ua != null && ua.contains("UMS/");

		// Filter if required
		if (isSelf || filterIp(ia)) {
			ctx.channel().close();
			LOGGER.trace(isSelf ?
				("Ignoring self-originating request from " + ia + ":" + remoteAddress.getPort()) :
				("Access denied for address " + ia + " based on IP filter"));
			return;
		}

		LOGGER.trace("Opened request handler on socket " + remoteAddress);
		PMS.get().getRegistry().disableGoToSleep();
		request = new RequestV2(nettyRequest.method().name(), nettyRequest.uri().substring(1));
		LOGGER.trace("Request: " + nettyRequest.protocolVersion().text() + " : " + request.getMethod() + " : " + request.getArgument());

		if (nettyRequest.protocolVersion().minorVersion() == 0) {
			request.setHttp10(true);
		}

		HttpHeaders headers = nettyRequest.headers();

		// The handler makes a couple of attempts to recognize a renderer from its requests.
		// IP address matches from previous requests are preferred, when that fails request
		// header matches are attempted and if those fail as well we're stuck with the
		// default renderer.
		String cookieString = headers.get(HttpHeaderNames.COOKIE);
		if (cookieString == null)
			cookieString = headers.get("cookies");
		renderer = RemoteUtil.matchRenderer(cookieString, ua, ia);
		if (renderer == null)
			renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);
		
		if (renderer != null) {
		// Attempt 1: try to recognize the renderer by its socket address from previous requests
			String uuid = UPNPControl.getUUID(ia);
			renderer.setUUID(uuid);
		}

		// If the renderer exists but isn't marked as loaded it means it's unrecognized
		// by upnp and we still need to attempt http recognition here.
		if (renderer == null || !renderer.loaded) {
			// Attempt 2: try to recognize the renderer by matching headers
			renderer = RendererConfiguration.getRendererConfigurationByHeaders(headers.entries(), ia);
		}

		if (renderer != null) {
			request.setMediaRenderer(renderer);
		}

		Set<String> headerNames = headers.names();
		Iterator<String> iterator = headerNames.iterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			String headerLine = name + ": " + headers.get(name);
			LOGGER.trace("Received on socket: " + headerLine);

			if (headerLine.toUpperCase().startsWith("USER-AGENT")) {
				userAgentString = headerLine.substring(headerLine.indexOf(':') + 1).trim();
			}

			try {
				StringTokenizer s = new StringTokenizer(headerLine);
				String temp = s.nextToken();
				if (temp.toUpperCase().equals("SOAPACTION:")) {
					request.setSoapaction(s.nextToken());
				} else if (temp.toUpperCase().equals("CALLBACK:")) {
					request.setSoapaction(s.nextToken());
				} else if (headerLine.toUpperCase().contains("RANGE: BYTES=")) {
					String nums = headerLine.substring(
						headerLine.toUpperCase().indexOf(
						"RANGE: BYTES=") + 13).trim();
					StringTokenizer st = new StringTokenizer(nums, "-");
					if (!nums.startsWith("-")) {
						request.setLowRange(Long.parseLong(st.nextToken()));
					}
					if (!nums.startsWith("-") && !nums.endsWith("-")) {
						request.setHighRange(Long.parseLong(st.nextToken()));
					} else {
						request.setHighRange(-1);
					}
				} else if (headerLine.toLowerCase().contains("transfermode.dlna.org:")) {
					request.setTransferMode(headerLine.substring(headerLine.toLowerCase().indexOf("transfermode.dlna.org:") + 22).trim());
				} else if (headerLine.toLowerCase().contains("getcontentfeatures.dlna.org:")) {
					request.setContentFeatures(headerLine.substring(headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") + 28).trim());
				} else {
					Matcher matcher = TIMERANGE_PATTERN.matcher(headerLine);
					if (matcher.find()) {
						String first = matcher.group(1);
						if (first != null) {
							request.setTimeRangeStartString(first);
						}
						String end = matcher.group(2);
						if (end != null) {
							request.setTimeRangeEndString(end);
						}
					} else {
						/** If we made it to here, none of the previous header checks matched.
						 * Unknown headers make interesting logging info when we cannot recognize
						 * the media renderer, so keep track of the truly unknown ones.
						 */
						boolean isKnown = false;

						// Try to match known headers.
						String lowerCaseHeaderLine = headerLine.toLowerCase();
						for (String knownHeaderString : KNOWN_HEADERS) {
							if (lowerCaseHeaderLine.startsWith(knownHeaderString)) {
								isKnown = true;
								break;
							}
						}

						// It may be unusual but already known
						if (!isKnown && renderer != null) {
							String additionalHeader = renderer.getUserAgentAdditionalHttpHeader();
							if (StringUtils.isNotBlank(additionalHeader) && lowerCaseHeaderLine.startsWith(additionalHeader)) {
								isKnown = true;
							}
						}

						if (!isKnown) {
							// Truly unknown header, therefore interesting. Save for later use.
							identifiers.add(headerLine);
						}
					}
				}
			} catch (Exception ee) {
				LOGGER.error("Error parsing HTTP headers", ee);
			}
		}

		// Still no media renderer recognized?
		if (renderer == null) {

			// Attempt 3: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			renderer = RendererConfiguration.resolve(ia, null);
			request.setMediaRenderer(renderer);
			if (renderer != null) {
				LOGGER.trace("Using default media renderer: " + renderer.getConfName());

				if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
					// We have found an unknown renderer
					identifiers.add(0, "User-Agent: " + userAgentString);
					renderer.setIdentifiers(identifiers);
					LOGGER.info("Media renderer was not recognized. Possible identifying HTTP headers:"
						+ StringUtils.join(identifiers, ", "));
					PMS.get().setRendererFound(renderer);
				}
			} else {
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				return;
			}
		} else {
			if (userAgentString != null) {
				LOGGER.trace("HTTP User-Agent: " + userAgentString);
			}

			LOGGER.trace("Recognized media renderer: " + renderer.getRendererName());
		}

		if (nettyRequest.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
			byte data[] = new byte[(int) HttpUtil.getContentLength(nettyRequest)];
			ByteBuf content = nettyRequest.content();
			content.readBytes(data);
			request.setTextContent(new String(data, "UTF-8"));
		}

		LOGGER.trace("HTTP: " + request.getArgument() + " / " + request.getLowRange() + "-" + request.getHighRange());

		writeResponse(ctx, e, request, ia);
	}

	/**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 *
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	public static boolean filterIp(InetAddress address) {
		return (!address.isLoopbackAddress() && !PMS.getConfiguration().getIpFiltering().allowed(address)) || !PMS.isReady();
	}

	private void writeResponse(ChannelHandlerContext ctx, HttpRequest e, RequestV2 request, InetAddress ia) {
		// Decide whether to close the connection or not.
		boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION)) ||
			nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
			!HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION));

		// Build the response object.
		FullHttpResponse response;
		if (request.getLowRange() != 0 || request.getHighRange() != 0) {
			// Limiting block size for better perf. doesn't work. Clients don't make follow up request
//			if (request.getHighRange() == -1)
//				request.setHighRange(request.getLowRange() + 8 * BUFFER_SIZE);
			response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1,
				HttpResponseStatus.PARTIAL_CONTENT
			);
		} else {
			String soapAction = nettyRequest.headers().get("SOAPACTION");

			if (soapAction != null && soapAction.contains("X_GetFeatureList")) {
				LOGGER.debug("Invalid action in SOAPACTION: " + soapAction);
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			} else {
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			}
		}

		final StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());
		startStopListenerDelegate.setRenderer(renderer);
		// Attach it to the context so it can be invoked if connection is reset unexpectedly
//		ctx.attr(startStopListenerDelegate);

		ChannelFuture chunkWriteFuture = null;
		try {
			StringBuilder content = request.answer(response, e, close, startStopListenerDelegate);
			if (request.getInputStream() != null || request.getFile() != null) {
				final InputStream inputStream = request.getInputStream();
				
				// Partial content aka byte range seek support
				DefaultHttpResponse response1 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				if (response.status().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
					// Smaller files need not be served as partial content
					response1 = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
				}
				response1.headers().set(response.headers());
				
				long start = 0;
				long end = 0;
				if (response.status().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
					start = request.getLowRange();
					end = request.getHighRange();
				}

				// Stream avi
				if (request.getFile() != null){// && !response.status().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
					LOGGER.trace("Serving file");
					File f = request.getFile();
					if (end <= 0)
						end = f.length() - 1;
					response1.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
					response1.headers().set(HttpHeaderNames.CONTENT_LENGTH, end - start + 1);
					if (response.status().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
						response1.headers().set(HttpHeaderNames.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, f.length()));
					}
					ctx.write(response1);
					chunkWriteFuture = ctx.write(new DefaultFileRegion(f, start, end + 1));
				} else {
					LOGGER.trace("Serving stream");
					// WMP needs content length for smaller files
					if (!request.getArgument().endsWith(".xml"))
						response1.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
					ctx.write(response1);
					chunkWriteFuture = ctx.write(new ChunkedStream(inputStream, 64 * BUFFER_SIZE));
					ctx.write(Unpooled.EMPTY_BUFFER);
				}
				ctx.write(LastHttpContent.EMPTY_LAST_CONTENT);

				LOGGER.info(e.toString());
				LOGGER.info(response1.toString());

				// Add a listener to clean up after sending the entire response body.
				chunkWriteFuture.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) {
						try {
							PMS.get().getRegistry().reenableGoToSleep();
							if (inputStream != null)
								inputStream.close();
						} catch (IOException e) {
							LOGGER.debug("Caught exception", e);
						}

						// Always close the channel after the response is sent because of
						// a freeze at the end of video when the channel is not closed.
						
						// For Denon, there's a follow-up thumbnail request which finishes first and causes the connection to close.
						future.channel().close();
						startStopListenerDelegate.stop();
					}
				});

			} else {
				ByteBuf buffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
				response.content().writeBytes(buffer);
				chunkWriteFuture = ctx.write(response);
			}
		} catch (IOException e1) {
			LOGGER.trace("HTTP request V2 IO error: " + e1.getMessage());
			// note: we don't call stop() here in a finally block as
			// answer() is non-blocking. we only (may) need to call it
			// here in the case of an exception. it's a no-op if it's
			// already been called
			startStopListenerDelegate.stop();
		}
		ctx.flush();
		// Decide whether to close the connection or not.
		if (close) {
			// Close the connection when the whole content is written out.
			chunkWriteFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
		throws Exception {
		Channel ch = ctx.channel();
		Throwable cause = e.getCause();
		if (cause instanceof TooLongFrameException) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		if (cause == null) {
			cause = e;
		}
		{
			if (cause.getClass().equals(IOException.class)) {
				LOGGER.debug("Connection error: " + cause);
//				StartStopListenerDelegate startStopListenerDelegate = (StartStopListenerDelegate)ctx.getAttachment();
//				if (startStopListenerDelegate != null) {
//					LOGGER.debug("Premature end, stopping...");
//					startStopListenerDelegate.stop();
//				}
			} else if (!cause.getClass().equals(ClosedChannelException.class)) {
				LOGGER.debug("Caught exception: {}", cause.getMessage());
				LOGGER.trace("", cause);
			}
		}
		if (ch.isOpen()) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
		ch.close();
	}

	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, status);
		response.headers().set(
			HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.content().writeBytes(Unpooled.copiedBuffer(
			"Failure: " + status.toString() + "\r\n", Charset.forName("UTF-8")));

		// Close the connection as soon as the error message is sent.
		ctx.channel().write(response).addListener(ChannelFutureListener.CLOSE);
	}

//	@Override
//	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
//		throws Exception {
//		// as seen in http://www.jboss.org/netty/community.html#nabble-td2423020
//		super.channelOpen(ctx, e);
//		if (group != null) {
//			group.add(ctx.channel());
//		}
//	}

	/* Uncomment to see channel events in the trace logs
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
	// Log all channel events.
	LOGGER.trace("Channel upstream event: " + e);
	super.handleUpstream(ctx, e);
	}
	 */
}
