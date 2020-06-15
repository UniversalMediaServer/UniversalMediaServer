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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpUtil.isContentLengthSet;
import static io.netty.util.AsciiString.contentEqualsIgnoreCase;
import io.netty.util.AttributeKey;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.external.StartStopListenerDelegate;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class RequestHandlerV2 extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerV2.class);

	private static final Pattern TIMERANGE_PATTERN = Pattern.compile(
		"timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d.:]+)?-?([\\d.:]+)?",
		Pattern.CASE_INSENSITIVE
	);

	private static final CharSequence CALLBACK = "CallBack";
	private static final CharSequence DLNA_GETCONTENTFEATURES = "GetContentFeatures.DLNA.ORG";
	private static final CharSequence DLNA_TIMESEEKRANGE = "TimeSeekRange.DLNA.ORG";
	private static final CharSequence DLNA_TRANSFERMODE = "TransferMode.DLNA.ORG";
	private static final CharSequence NT = "NT";
	private static final CharSequence SOAPACTION = "SOAPACTION";
	private static final CharSequence SID = "SID";
	private static final CharSequence TIMEOUT = "Timeout";
	private final AttributeKey<StartStopListenerDelegate> startStop = AttributeKey.valueOf("startStop");

	// Used to filter out known headers when the renderer is not recognized
	private final static CharSequence[] KNOWN_HEADERS = {
		ACCEPT,
		ACCEPT_LANGUAGE,
		ACCEPT_ENCODING,
		CALLBACK,
		CONNECTION,
		CONTENT_LENGTH,
		CONTENT_TYPE,
		DATE,
		DLNA_GETCONTENTFEATURES,
		DLNA_TIMESEEKRANGE,
		DLNA_TRANSFERMODE,
		HOST,
		NT,
		RANGE,
		SID,
		SOAPACTION,
		TIMEOUT,
		USER_AGENT,
	};

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws Exception {
		InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		InetAddress ia = remoteAddress.getAddress();
		String userAgent = nettyRequest.headers().get(USER_AGENT);
		boolean isSelf = isLocalClingRequest(ia, userAgent);

		// Filter if required
		if (isSelf || filterIp(ia)) {
			ctx.channel().close();
//			if (isSelf && LOGGER.isTraceEnabled()) {
//				LOGGER.trace("Ignoring self-originating request from {}:{}", ia, remoteAddress.getPort());
//			}
			return;
		}

		LOGGER.trace("Opened request handler on socket " + remoteAddress);
		RequestV2 requestV2 = new RequestV2(nettyRequest.method(), nettyRequest.uri().substring(1));
		LOGGER.trace("Request: " + nettyRequest.protocolVersion().text() + " : " + requestV2.getMethod() + " : " + requestV2.getArgument());

		if (nettyRequest.protocolVersion().minorVersion() == 0) {
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
		} else {
			// If RendererConfiguration.resolve() didn't return the default renderer
			// it means we know via upnp that it's not really a renderer.
			return;
		}
		 
		requestV2.setSoapaction(getSoapActionOrCallback(headers));

		parseRangeHeader(requestV2, headers);
		parseTransferMode(requestV2, headers);
		parseGetContentFeatures(requestV2, headers);
		parseTimeSeekRange(requestV2, headers);

		if (isContentLengthSet(nettyRequest) && nettyRequest.content().isReadable()) {
			String textContent = nettyRequest.content().toString(UTF_8);
			requestV2.setTextContent(textContent);
			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(ctx, nettyRequest, textContent, renderer);
			}
		} else if (LOGGER.isTraceEnabled() ){
			logMessageReceived(ctx, nettyRequest, null, renderer);
		}

		String headerValue = headers.get("X-PANASONIC-DMP-Profile");
		if (headerValue != null) {
			PanasonicDmpProfiles.parsePanasonicDmpProfiles(headerValue, renderer);
		}

		writeResponse(ctx, nettyRequest, requestV2, ia);
	}

	private static void logMessageReceived(ChannelHandlerContext ctx, FullHttpRequest nettyRequest, String content, RendererConfiguration renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;
		if (nettyRequest instanceof HttpRequest) {
			header.append(((HttpRequest) nettyRequest).method());
			header.append(" ").append(((HttpRequest) nettyRequest).uri());
		}

		if (nettyRequest instanceof HttpMessage) {
			if (header.length() > 0) {
				header.append(" ");
			}

			header.append(((HttpMessage) nettyRequest).protocolVersion().text());
			header.append("\n\n");
			header.append("HEADER:\n");
			for (Entry<String, String> entry : ((HttpMessage) nettyRequest).headers().entries()) {
				if (isNotBlank(entry.getKey())) {
					header.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
					if (SOAPACTION.equals(entry.getKey())) {
						soapAction = entry.getValue().toUpperCase(Locale.ROOT);
					}
				}
			}
		} else {
			header.append("Unknown class: ").append(nettyRequest.getClass().getSimpleName()).append("\n");
			header.append(nettyRequest).append("\n");
		}

		String formattedContent = null;
		if (isNotBlank(content)) {
			try {
				formattedContent = StringUtil.prettifyXML(content, StandardCharsets.UTF_8, 2);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
				formattedContent += "    " + content.replaceAll("\n", "\n    ") + "\n";
			}
		}

		String requestType = "";
		// Map known requests to request type
		if (soapAction != null) {
			if (soapAction.contains("CONTENTDIRECTORY:1#BROWSE")) {
				requestType = "browse ";
			} else if (soapAction.contains("CONTENTDIRECTORY:1#SEARCH")) {
				requestType = "search ";
			}
		}

		String rendererName;
		if (renderer != null) {
			if (isNotBlank(renderer.getRendererName())) {
				if (isBlank(renderer.getConfName()) || renderer.getRendererName().equals(renderer.getConfName())) {
					rendererName = renderer.getRendererName();
				} else {
					rendererName = renderer.getRendererName() + " [" + renderer.getConfName() + "]";
				}

			} else if (isNotBlank(renderer.getConfName())) {
				rendererName = renderer.getConfName();
			} else {
				rendererName = "Unnamed";
			}

		} else {
			rendererName = "Unknown";
		}

		if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
			rendererName +=
				" (" + ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress() +
				":" + ((InetSocketAddress) ctx.channel().remoteAddress()).getPort() + ")";
		}

		if (isNotBlank(requestType)) {
			LOGGER.trace(
				"Received a {}request from {}:\n\n{}{}",
				requestType,
				rendererName,
				header,
				isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : ""
				);
		} else { // Trace not supported request type
			LOGGER.trace(
				"Received a {}request from {}:\n\n{}.\nRenderer UUID={}",
				soapAction,
				rendererName,
				header,
				renderer.uuid
				);
		}
	}

	/**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 *
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	private static boolean filterIp(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
	}

	private void writeResponse(ChannelHandlerContext ctx, FullHttpRequest nettyRequest, RequestV2 request, InetAddress ia) throws HttpException {
		// Decide whether to close the connection or not.
		boolean close = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(nettyRequest.headers().get(HttpHeaderNames.CONNECTION)) ||
			nettyRequest.protocolVersion().equals(HttpVersion.HTTP_1_0) &&
			!HttpHeaderValues.KEEP_ALIVE.contentEqualsIgnoreCase(nettyRequest.headers().get(HttpHeaderNames.CONNECTION));

		// Build the response object.
		HttpResponse response;
		if (request.getLowRange() != 0 || request.getHighRange() != 0) {
			response = new DefaultHttpResponse(
				request.isHttp10() ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1,
				HttpResponseStatus.PARTIAL_CONTENT
			);
		} else {
			String soapAction = nettyRequest.headers().get("SOAPACTION");

			if (soapAction != null && soapAction.contains("X_GetFeatureList")) {
				LOGGER.debug("Not implemented feature in the UMS requested in SOAPACTION: " + soapAction);
				response = new DefaultHttpResponse(
					request.isHttp10() ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1,
					HttpResponseStatus.NOT_IMPLEMENTED
				);
			} else {
				response = new DefaultHttpResponse(
					request.isHttp10() ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1,
					HttpResponseStatus.OK
				);
			}
		}

		StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());
		// Attach it to the context so it can be invoked if connection is reset unexpectedly
		ctx.channel().attr(startStop).set(startStopListenerDelegate);

		try {
			request.answer(ctx, response, nettyRequest, close, startStopListenerDelegate);
		} catch (IOException e1) {
			LOGGER.debug("HTTP request V2 IO error: " + e1.getMessage());
			LOGGER.trace("", e1);
			// note: we don't call stop() here in a finally block as
			// answer() is non-blocking. we only (may) need to call it
			// here in the case of an exception. it's a no-op if it's
			// already been called
			startStopListenerDelegate.stop();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof TooLongFrameException) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		if (cause != null) {
			if (cause.getClass().equals(IOException.class)) {
				LOGGER.debug("Connection error: " + cause);
				StartStopListenerDelegate startStopListenerDelegate = ctx.channel().attr(startStop).get();
				if (startStopListenerDelegate != null) {
					LOGGER.debug("Premature end, stopping...");
					startStopListenerDelegate.stop();
				}
			} else if (!cause.getClass().equals(ClosedChannelException.class)) {
				LOGGER.debug("Caught exception: {}", cause.getMessage());
				LOGGER.trace("", cause);
			}
		}

		if (ctx.channel().isActive()) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}

		ctx.close();
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(
			HttpVersion.HTTP_1_1, status);
		response.headers().set(
			HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private void parseTimeSeekRange(RequestV2 requestV2, HttpHeaders headers) {
		if (!headers.contains(DLNA_TIMESEEKRANGE)) return;

		String headerLine = DLNA_TIMESEEKRANGE + ": " + headers.get(DLNA_TIMESEEKRANGE);
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
		}
	}

	private void parseGetContentFeatures(RequestV2 requestV2, HttpHeaders headers) {
		if (headers.contains(DLNA_GETCONTENTFEATURES))
			requestV2.setContentFeatures(headers.get(DLNA_GETCONTENTFEATURES).trim());
	}
		 
	private void parseTransferMode(RequestV2 requestV2, HttpHeaders headers) {
		if (headers.contains(DLNA_TRANSFERMODE))
			requestV2.setTransferMode(headers.get(DLNA_TRANSFERMODE).trim());
	}

	private void parseRangeHeader(RequestV2 requestV2, HttpHeaders headers) {
		if (headers.contains(RANGE) && headers.get(RANGE).toLowerCase().startsWith("bytes=")) {
			String nums = headers.get(RANGE).substring(6).trim();
			StringTokenizer st = new StringTokenizer(nums, "-");
			if (!nums.startsWith("-")) {
				requestV2.setLowRange(Long.parseLong(st.nextToken()));
			}

			if (!nums.startsWith("-") && !nums.endsWith("-")) {
				requestV2.setHighRange(Long.parseLong(st.nextToken()));
			} else {
				requestV2.setHighRange(-1);
			}
		}
	}

	private String getSoapActionOrCallback(HttpHeaders headers) {
		String headerValue = trimToEmpty(headers.get(SOAPACTION));
		if (headerValue.isEmpty()) {
			// Fall back to the callback header
			headerValue = trimToEmpty(headers.get(CALLBACK));
		}
		if (headerValue.isEmpty()){
			return "";
		} else {
			// I'm not entirely sure this is needed, but I'm leaving it in just in case.
			return new StringTokenizer(headerValue).nextToken();
		}
	}

	private boolean isKnownHeader(String headerName) {
		for (CharSequence knownHeader : KNOWN_HEADERS) {
			if (contentEqualsIgnoreCase(headerName, knownHeader)) {
				return true;
			}
		}
		return false;
	}

	private boolean isLocalClingRequest(InetAddress ia, String userAgent) {
		return userAgent != null &&
			userAgent.contains("UMS/") &&
			userAgent.contains("Cling/") &&
			ia.getHostAddress().equals(PMS.get().getServer().getHost());
	}

	private List<String> getUnknownHeaders(HttpHeaders headers) {
		List<String> unknownHeaders = new LinkedList<>();
		Set<String> headerNames = headers.names();
		Iterator<String> iterator = headerNames.iterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			String headerLine = name + ": " + headers.get(name);
			/** Unknown headers make interesting logging info when we cannot recognize
			 * the media renderer, so keep track of the truly unknown ones.
			 */
			if (!isKnownHeader(name)) {
				unknownHeaders.add(headerLine);
			}
		}
		return unknownHeaders;
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
			String userAgent = headers.get(USER_AGENT);
			if (userAgent != null) {
				LOGGER.debug("HTTP User-Agent: " + userAgent);
			}
			LOGGER.trace("Recognized media renderer: " + renderer.getRendererName());
		} else {
			// Attempt 3: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			renderer = RendererConfiguration.resolve(ia, null);
			if (renderer != null) {
				LOGGER.trace("Using default media renderer: " + renderer.getRendererName());
				String userAgent = headers.get(USER_AGENT);
				if (userAgent != null && !userAgent.equals("FDSSDP")) {
					// We have found an unknown renderer
					List<String> unknownHeaders = getUnknownHeaders(headers);
					LOGGER.info("Media renderer was not recognized. Possible identifying HTTP headers: " +
						"User-Agent: " + userAgent +
						(unknownHeaders.isEmpty() ? "" : ", " + join(unknownHeaders, ", ")));
					PMS.get().setRendererFound(renderer);
				}
			}
		}

		return renderer;
	}
}
