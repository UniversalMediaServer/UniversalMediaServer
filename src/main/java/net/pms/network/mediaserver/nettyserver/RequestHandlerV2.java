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
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import net.pms.configuration.RendererConfigurations;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.network.mediaserver.MediaServer;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.service.StartStopListenerDelegate;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class RequestHandlerV2 extends SimpleChannelUpstreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerV2.class);

	private static final Pattern TIMERANGE_PATTERN = Pattern.compile(
		"timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d.:]+)?-?([\\d.:]+)?",
		Pattern.CASE_INSENSITIVE
	);

	private static final String HTTPSERVER_REQUEST_BEGIN =  "================================== HTTPSERVER REQUEST BEGIN =====================================";
	private static final String HTTPSERVER_REQUEST_END =    "================================== HTTPSERVER REQUEST END =======================================";

	private final ChannelGroup group;

	public RequestHandlerV2(ChannelGroup group) {
		this.group = group;
	}

	// Used to filter out known headers when the renderer is not recognized
	private static final String[] KNOWN_HEADERS = {
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
		"user-agent"
	};

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		RequestV2 request;
		Renderer renderer = null;
		String userAgentString = null;
		ArrayList<String> identifiers = new ArrayList<>();

		HttpRequest nettyRequest = (HttpRequest) event.getMessage();
		HttpHeaders headers = nettyRequest.headers();

		InetSocketAddress remoteAddress = (InetSocketAddress) event.getChannel().getRemoteAddress();
		InetAddress ia = remoteAddress.getAddress();

		// Is the request from our own JUPnP service, i.e. self-originating?
		boolean isSelf = ia.getHostAddress().equals(MediaServer.getHost()) &&
			headers.get(HttpHeaders.Names.USER_AGENT) != null &&
			headers.get(HttpHeaders.Names.USER_AGENT).contains("UMS/");

		// Filter if required
		if (isSelf || filterIp(ia)) {
			event.getChannel().close();
			/*if (isSelf && LOGGER.isTraceEnabled()) {
				LOGGER.trace("Ignoring self-originating request from {}:{}", ia, remoteAddress.getPort());
			}*/
			return;
		}

		request = new RequestV2(nettyRequest.getMethod(), getUri(nettyRequest.getUri()));


		// The handler makes a couple of attempts to recognize a renderer from its requests.
		// IP address matches from previous requests are preferred, when that fails request
		// header matches are attempted and if those fail as well we're stuck with the
		// default renderer.

		// Attempt 1: If the reguested url contains the no-transcode tag, force
		// the default streaming-only conf.
		if (request.getUri().contains(Renderer.NOTRANSCODE)) {
			renderer = RendererConfigurations.getDefaultRenderer();
			LOGGER.debug("Forcing streaming.");
		}

		if (renderer == null) {
			// Attempt 2: try to recognize the renderer by its socket address from previous requests
			renderer = ConnectedRenderers.getRendererBySocketAddress(ia);
		}

		// If the renderer exists but isn't marked as loaded it means it's unrecognized
		// by upnp and we still need to attempt http recognition here.
		if (renderer == null || !renderer.isLoaded()) {
			// Attempt 3: try to recognize the renderer by matching headers
			renderer = ConnectedRenderers.getRendererConfigurationByHeaders(headers.entries(), ia);
		}

		if (renderer != null) {
			request.setMediaRenderer(renderer);
		}

		Set<String> headerNames = headers.names();
		for (String name : headerNames) {
			String headerLine = name + ": " + headers.get(name);

			if (headerLine.toUpperCase().startsWith("USER-AGENT")) {
				userAgentString = headerLine.substring(headerLine.indexOf(':') + 1).trim();
			} else if (renderer != null && name.equals("X-PANASONIC-DMP-Profile")) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(headers.get(name), renderer);
			}

			try {
				StringTokenizer s = new StringTokenizer(headerLine);
				String temp = s.nextToken();
				if (temp.equalsIgnoreCase("SOAPACTION:")) {
					request.setSoapaction(s.nextToken());
				} else if (temp.equalsIgnoreCase("CALLBACK:")) {
					request.setSoapaction(s.nextToken());
				} else if (headerLine.toUpperCase().contains("RANGE: BYTES=")) {
					String nums = headerLine.substring(headerLine.toUpperCase().indexOf("RANGE: BYTES=") + 13).trim();
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
			} catch (NumberFormatException ee) {
				LOGGER.error("Error parsing HTTP headers: {}", ee.getMessage());
				LOGGER.trace("", ee);
			}
		}

		// Still no media renderer recognized?
		if (renderer == null) {

			// Attempt 4: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			renderer = ConnectedRenderers.resolve(ia, null);
			request.setMediaRenderer(renderer);
			if (renderer != null) {
				LOGGER.debug("Using default media renderer \"{}\"", renderer.getConfName());

				if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
					// We have found an unknown renderer
					identifiers.add(0, "User-Agent: " + userAgentString);
					renderer.setIdentifiers(identifiers);
					LOGGER.info(
						"Media renderer was not recognized. Possible identifying HTTP headers:\n{}",
						StringUtils.join(identifiers, "\n")
					);
					PMS.get().setRendererFound(renderer);
				}
			} else {
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				return;
			}
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Recognized media renderer \"{}\"", renderer.getRendererName());
		}

		if (nettyRequest.headers().contains(HttpHeaders.Names.CONTENT_LENGTH)) {
			byte[] data = new byte[(int) HttpHeaders.getContentLength(nettyRequest)];
			ChannelBuffer content = nettyRequest.getContent();
			content.readBytes(data);
			String textContent = new String(data, StandardCharsets.UTF_8);
			request.setTextContent(textContent);
			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(event, textContent, renderer);
			}
		} else if (LOGGER.isTraceEnabled()) {
			logMessageReceived(event, null, renderer);
		}

		/**
		 * LG TVs send us many "play" requests while browsing directories, in order
		 * for them to show dynamic thumbnails. That means we can skip certain things
		 * like searching for subtitles and fully played logic.
		 */
		if (renderer.isLG() && userAgentString != null && userAgentString.contains("Lavf/")) {
			request.setIsVideoThumbnailRequest(true);
		}

		writeResponse(ctx, event, request, ia);
	}

	/**
	 * Removes all preceding slashes from uri. Samsung 2012 TVs have a problematic (additional) preceding slash that
	 * needs to be also removed.
	 *
	 * @param rawUri requested uri
	 * @return uri without preceding slash
	 */
	private String getUri(String rawUri) {
		String uri = rawUri;

		while (uri.startsWith("/")) {
			LOGGER.trace("Stripping preceding slash from: " + uri);
			uri = uri.substring(1);
		}
		return uri;
	}

	private static void logMessageReceived(MessageEvent event, String content, Renderer renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;
		if (event.getMessage() instanceof HttpRequest httpRequest) {
			header.append(httpRequest.getMethod());
			header.append(" ").append(httpRequest.getUri());
		}
		if (event.getMessage() instanceof HttpMessage httpMessage) {
			if (header.length() > 0) {
				header.append(" ");
			}
			header.append(httpMessage.getProtocolVersion().getText());
			header.append("\n\n");
			header.append("HEADER:\n");
			for (Entry<String, String> entry : httpMessage.headers().entries()) {
				if (StringUtils.isNotBlank(entry.getKey())) {
					header.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
					if ("SOAPACTION".equalsIgnoreCase(entry.getKey())) {
						soapAction = entry.getValue().toUpperCase(Locale.ROOT);
					}
				}
			}
		} else {
			header.append("Unknown class: ").append(event.getClass().getSimpleName()).append("\n");
			header.append(event).append("\n");
		}
		String formattedContent = null;
		if (StringUtils.isNotBlank(content)) {
			try {
				formattedContent = StringUtil.prettifyXML(content, StandardCharsets.UTF_8, 2);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
				formattedContent += "    " + content.replace("\n", "\n    ") + "\n";
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
		} else {
			soapAction = "";
		}
		String rendererName;
		if (renderer != null) {
			if (StringUtils.isNotBlank(renderer.getRendererName())) {
				if (StringUtils.isBlank(renderer.getConfName()) || renderer.getRendererName().equals(renderer.getConfName())) {
					rendererName = renderer.getRendererName();
				} else {
					rendererName = renderer.getRendererName() + " [" + renderer.getConfName() + "]";
				}
			} else if (StringUtils.isNotBlank(renderer.getConfName())) {
				rendererName = renderer.getConfName();
			} else {
				rendererName = "Unnamed";
			}
		} else {
			rendererName = "Unknown";
		}
		if (event.getChannel().getRemoteAddress() instanceof InetSocketAddress inetSocketAddress) {
			rendererName +=
				" (" + inetSocketAddress.getAddress().getHostAddress() +
				":" + inetSocketAddress.getPort() + ")";
		}

		formattedContent = StringUtils.isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : "";
		if (StringUtils.isNotBlank(requestType)) {
			LOGGER.trace(
				"Received a {}request from {}:\n{}\n{}{}{}",
				requestType,
				rendererName,
				HTTPSERVER_REQUEST_BEGIN,
				header,
				formattedContent,
				HTTPSERVER_REQUEST_END
				);
		} else { // Trace not supported request type
			LOGGER.trace(
				"Received a {}request from {}:\n{}\n{}{}{}\nRenderer UUID={}",
				soapAction,
				rendererName,
				HTTPSERVER_REQUEST_BEGIN,
				header,
				formattedContent,
				HTTPSERVER_REQUEST_END,
				renderer != null ? renderer.getUUID() : "null"
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

	private void writeResponse(ChannelHandlerContext ctx, MessageEvent event, RequestV2 request, InetAddress ia) throws HttpException {
		HttpRequest nettyRequest = (HttpRequest) event.getMessage();
		// Decide whether to close the connection or not.
		boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION)) ||
			nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
			!HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(nettyRequest.headers().get(HttpHeaders.Names.CONNECTION));

		// Build the response object.
		HttpResponse response;
		if (request.getLowRange() != 0 || request.getHighRange() != 0) {
			response = new DefaultHttpResponse(
				nettyRequest.getProtocolVersion(),
				HttpResponseStatus.PARTIAL_CONTENT
			);
		} else {
			response = new DefaultHttpResponse(
				nettyRequest.getProtocolVersion(),
				HttpResponseStatus.OK
			);
		}

		StartStopListenerDelegate startStopListenerDelegate = null;
		if (!request.isVideoThumbnailRequest()) {
			startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());
			// Attach it to the context so it can be invoked if connection is reset unexpectedly
			ctx.setAttachment(startStopListenerDelegate);
		}

		try {
			request.answer(response, event, close, startStopListenerDelegate);
		} catch (IOException e1) {
			LOGGER.debug("HTTP request V2 IO error: " + e1.getMessage());
			LOGGER.trace("", e1);
			// note: we don't call stop() here in a finally block as
			// answer() is non-blocking. we only (may) need to call it
			// here in the case of an exception. it's a no-op if it's
			// already been called
			if (startStopListenerDelegate != null) {
				startStopListenerDelegate.stop();
			}
		}
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
				LOGGER.debug("Connection error: " + cause);
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

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
		throws Exception {
		// as seen in http://www.jboss.org/netty/community.html#nabble-td2423020
		super.channelOpen(ctx, e);
		if (group != null) {
			group.add(ctx.getChannel());
		}
	}

	/* Uncomment to see channel events in the trace logs
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
	// Log all channel events.
	LOGGER.trace("Channel upstream event: " + e);
	super.handleUpstream(ctx, e);
	}
	 */
}
