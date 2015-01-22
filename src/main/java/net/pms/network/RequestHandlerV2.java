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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.StartStopListenerDelegate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class RequestHandlerV2 extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerV2.class);

	private static final Pattern TIMERANGE_PATTERN = Pattern.compile(
		"timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d.:]+)?-?([\\d.:]+)?",
		Pattern.CASE_INSENSITIVE
	);

	private static final CharSequence CALLBACK = HttpHeaders.newNameEntity("CallBack");
	private static final CharSequence SOAPACTION = HttpHeaders.newNameEntity("SOAPAction");

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
		"range",
		"sid",
		"soapaction",
		"timeout",
		"user-agent"
	};

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws Exception {
		InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		InetAddress ia = remoteAddress.getAddress();

		String userAgent = nettyRequest.headers().get(HttpHeaders.Names.USER_AGENT);
		boolean isSelf = isLocalClingRequest(ia, userAgent);

		// Filter if required
		if (isSelf || filterIp(ia)) {
			LOGGER.trace(isSelf ?
				("Ignoring self-originating request from " + ia + ":" + remoteAddress.getPort()) :
				("Access denied for address " + ia + " based on IP filter"));
			return;
		}

		LOGGER.trace("Opened request handler on socket " + remoteAddress);
		PMS.get().getRegistry().disableGoToSleep();
		RequestV2 requestV2 = new RequestV2(nettyRequest.getMethod().name(), nettyRequest.getUri().substring(1));
		LOGGER.trace("Request: " + nettyRequest.getProtocolVersion().text() + " : " + requestV2.getMethod() + " : " + requestV2.getArgument());

		if (nettyRequest.getProtocolVersion().minorVersion() == 0) {
			requestV2.setHttp10(true);
		}

		HttpHeaders headers = nettyRequest.headers();

		// The handler makes a couple of attempts to recognize a renderer from its requests.
		// IP address matches from previous requests are preferred, when that fails request
		// header matches are attempted and if those fail as well we're stuck with the
		// default renderer.
		RendererConfiguration renderer = getRendererConfiguration(ia, headers);
		if (renderer != null) {
			requestV2.setMediaRenderer(renderer);
		}

		String soapAction = getSoapActionOrCallback(headers);
		if (soapAction != null) {
			requestV2.setSoapaction(soapAction);
		}

		Set<String> headerNames = headers.names();
		List<String> unknownHeaders = new LinkedList<>();
		Iterator<String> iterator = headerNames.iterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			String headerLine = name + ": " + headers.get(name);
			LOGGER.trace("Received on socket: " + headerLine);

			try {
				if (headerLine.toUpperCase().contains("RANGE: BYTES=")) {
					String nums = headerLine.substring(
						headerLine.toUpperCase().indexOf(
						"RANGE: BYTES=") + 13).trim();
					StringTokenizer st = new StringTokenizer(nums, "-");
					if (!nums.startsWith("-")) {
						requestV2.setLowRange(Long.parseLong(st.nextToken()));
					}
					if (!nums.startsWith("-") && !nums.endsWith("-")) {
						requestV2.setHighRange(Long.parseLong(st.nextToken()));
					} else {
						requestV2.setHighRange(-1);
					}
				} else if (headerLine.toLowerCase().contains("transfermode.dlna.org:")) {
					requestV2.setTransferMode(headerLine.substring(headerLine.toLowerCase().indexOf("transfermode.dlna.org:") + 22).trim());
				} else if (headerLine.toLowerCase().contains("getcontentfeatures.dlna.org:")) {
					requestV2.setContentFeatures(headerLine.substring(headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") + 28).trim());
				} else {
					Matcher matcher = TIMERANGE_PATTERN.matcher(headerLine);
					if (matcher.find()) {
						String first = matcher.group(1);
						if (first != null) {
							requestV2.setTimeRangeStartString(first);
						}
						String end = matcher.group(2);
						if (end != null) {
							requestV2.setTimeRangeEndString(end);
						}
					} else {
						/** If we made it to here, none of the previous header checks matched.
						 * Unknown headers make interesting logging info when we cannot recognize
						 * the media renderer, so keep track of the truly unknown ones.
						 */
						if (!isKnownOrAdditionalHeader(renderer, headerLine)) {
							// Truly unknown header, therefore interesting. Save for later use.
							unknownHeaders.add(headerLine);
						}
					}
				}
			} catch (Exception ee) {
				LOGGER.error("Error parsing HTTP headers", ee);
			}
		}

		// Still no media renderer recognized?
		if (requestV2.getMediaRenderer() == null) {

			// Attempt 3: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			requestV2.setMediaRenderer(RendererConfiguration.resolve(ia, null));
			if (requestV2.getMediaRenderer() != null) {
				LOGGER.trace("Using default media renderer: " + requestV2.getMediaRenderer().getRendererName());

				if (userAgent != null && !userAgent.equals("FDSSDP")) {
					// We have found an unknown renderer
					LOGGER.info("Media renderer was not recognized. Possible identifying HTTP headers: " +
							"User-Agent: " + userAgent +
							(unknownHeaders.isEmpty() ? "" : ", " + StringUtils.join(unknownHeaders, ", ")));
					PMS.get().setRendererFound(requestV2.getMediaRenderer());
				}
			} else {
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				return;
			}
		}

		if (HttpHeaders.isContentLengthSet(nettyRequest) && nettyRequest.content().isReadable()) {
			requestV2.setTextContent(nettyRequest.content().toString(UTF_8));
		}

		LOGGER.trace("HTTP: " + requestV2.getArgument() + " / " + requestV2.getLowRange() + "-" + requestV2.getHighRange());

		writeResponse(ctx, nettyRequest, requestV2, ia);
	}

	private String getSoapActionOrCallback(HttpHeaders headers) {
		String headerValue = trimToEmpty(headers.get(SOAPACTION));
		if (headerValue.isEmpty()) {
			// Fall back to the callback header
			headerValue = trimToEmpty(headers.get(CALLBACK));
		}
		if (headerValue.isEmpty()){
			return null;
		} else {
			// I'm not entirely sure this is needed, but I'm leaving it in just in case.
			return new StringTokenizer(headerValue).nextToken();
		}
	}

	private boolean isKnownOrAdditionalHeader(RendererConfiguration renderer, String headerLine) {
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
		if (renderer != null) {
            String additionalHeader = renderer.getUserAgentAdditionalHttpHeader();
            if (StringUtils.isNotBlank(additionalHeader) && lowerCaseHeaderLine.startsWith(additionalHeader)) {
                isKnown = true;
            }
        }
		return isKnown;
	}

	private boolean isLocalClingRequest(InetAddress ia, String userAgent) {
		// FIXME: this would also block an external cling-based client running on the same host
		return userAgent != null && userAgent.contains("Cling/") &&
				ia.getHostAddress().equals(PMS.get().getServer().getHost());
	}

	private RendererConfiguration getRendererConfiguration(InetAddress ia, HttpHeaders headers) {
		// Attempt 1: try to recognize the renderer by its socket address from previous requests
		RendererConfiguration renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);

		// If the renderer exists but isn't marked as loaded it means it's unrecognized
		// by upnp and we still need to attempt http recognition here.
		if (renderer == null || !renderer.loaded) {
			// Attempt 2: try to recognize the renderer by matching headers
			renderer = RendererConfiguration.getRendererConfigurationByHeaders(headers.entries(), ia);
		}

		if (renderer != null) {
			String userAgent = headers.get(HttpHeaders.Names.USER_AGENT);
			if (userAgent != null) {
				LOGGER.debug("HTTP User-Agent: " + userAgent);
			}
			LOGGER.trace("Recognized media renderer: " + renderer.getRendererName());
		}

		return renderer;
	}

	/**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 *
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	private boolean filterIp(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
	}

	private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest nettyRequest, RequestV2 request, InetAddress ia) {
		// Decide whether to close the connection or not.
		boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION)) ||
			nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
			!HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION));

		// Build the response object.
		HttpResponse response;
		if (request.getLowRange() != 0 || request.getHighRange() != 0) {
			response = new DefaultHttpResponse(
				HttpVersion.HTTP_1_1,
				HttpResponseStatus.PARTIAL_CONTENT
			);
		} else {
			String soapAction = nettyRequest.headers().get("SOAPACTION");

			if (soapAction != null && soapAction.contains("X_GetFeatureList")) {
				LOGGER.debug("Invalid action in SOAPACTION: " + soapAction);
				response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
			} else {
				response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			}
		}

		StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());

		try {
			request.answer(ctx, response, nettyRequest, close, startStopListenerDelegate);
		} catch (IOException e1) {
			LOGGER.trace("HTTP request V2 IO error: " + e1.getMessage());
			// note: we don't call stop() here in a finally block as
			// answer() is non-blocking. we only (may) need to call it
			// here in the case of an exception. it's a no-op if it's
			// already been called
			startStopListenerDelegate.stop();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOGGER.trace("Caught exception", cause);
		if (cause instanceof TooLongFrameException) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		if (cause != null && !cause.getClass().equals(ClosedChannelException.class) && !cause.getClass().equals(IOException.class)) {
			LOGGER.debug("Caught exception", cause);
		}
		if (ctx.channel().isActive()) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
		ctx.close();
	}

	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(
				"Failure: " + status.toString() + "\r\n", Charset.forName("UTF-8")));
		response.headers().set(
			HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
}
