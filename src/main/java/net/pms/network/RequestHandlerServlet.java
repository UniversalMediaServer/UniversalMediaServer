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
package net.pms.network;

import java.io.BufferedReader;
import java.io.FileInputStream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaOnDemandSubtitle;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.MediaType;
import net.pms.dlna.Range;
import net.pms.dlna.RealFile;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.encoders.ImagePlayer;
import net.pms.external.StartStopListenerDelegate;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.network.api.ApiHandler;
import net.pms.service.Services;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class RequestHandlerServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerServlet.class);

	private static final Pattern TIMERANGE_PATTERN = Pattern.compile(
		"timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d.:]+)?-?([\\d.:]+)?",
		Pattern.CASE_INSENSITIVE
	);

	private static final String METHOD_DELETE = "DELETE";
	private static final String METHOD_HEAD = "HEAD";
	private static final String METHOD_GET = "GET";
	private static final String METHOD_OPTIONS = "OPTIONS";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";
	private static final String METHOD_TRACE = "TRACE";
	private static final String METHOD_SUBSCRIBE = "SUBSCRIBE";
	private static final String METHOD_NOTIFY = "NOTIFY";


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
		"user-agent"
	};

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		InetAddress ia = InetAddress.getByName(req.getRemoteAddr());
		boolean isSelf = req.getRemoteHost().equals(PMS.get().getServer().getHost()) &&
				req.getHeader("User-Agent") != null &&
				req.getHeader("User-Agent").contains("UMS/");
		// Filter if required
		if (isSelf || filterIp(ia)) {
			resp.sendError(HttpURLConnection.HTTP_FORBIDDEN);
			return;
		}
		RequestServlet request = new RequestServlet(req.getMethod(), getUri(req.getRequestURI()));
		//fill headers
		List<Map.Entry<String, String>> headers = getHttpHeaders(req);

		// The handler makes a couple of attempts to recognize a renderer from its requests.
		// IP address matches from previous requests are preferred, when that fails request
		// header matches are attempted and if those fail as well we're stuck with the
		// default renderer.

		RendererConfiguration renderer = null;
		// Attempt 1: If the reguested url contains the no-transcode tag, force
		// the default streaming-only conf.
		if (request.getUri().contains(RendererConfiguration.NOTRANSCODE)) {
			renderer = RendererConfiguration.getStreamingConf();
			LOGGER.debug("Forcing streaming.");
		}

		if (renderer == null) {
			// Attempt 2: try to recognize the renderer by its socket address from previous requests
			renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);
		}

		// If the renderer exists but isn't marked as loaded it means it's unrecognized
		// by upnp and we still need to attempt http recognition here.
		if (renderer == null || !renderer.loaded) {
			// Attempt 3: try to recognize the renderer by matching headers
			renderer = RendererConfiguration.getRendererConfigurationByHeaders(headers, ia);
		}

		if (renderer != null) {
			request.setMediaRenderer(renderer);
		}
		
		String userAgentString = null;
		ArrayList<String> identifiers = new ArrayList<>();
		Iterator<String> iterator = req.getHeaderNames().asIterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			String value = req.getHeader(name);
			String headerLine = name + ": " + value;

			if (headerLine.toUpperCase().startsWith("USER-AGENT")) {
				userAgentString = headerLine.substring(headerLine.indexOf(':') + 1).trim();
			} else if (renderer != null && name.equals("X-PANASONIC-DMP-Profile")) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(value, renderer);
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
			renderer = RendererConfiguration.resolve(ia, null);
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

		if (req.getContentLength() > 0) {
			String textContent = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
			request.setTextContent(textContent);
			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(event, textContent, renderer);
			}
		}
		
		writeResponse(ctx, event, request, ia);
	}

	private void getTransfertMode() {
		
	}

	private void writeResponse(HttpServletRequest req, HttpServletResponse resp, RequestServlet request, InetAddress ia) throws HttpException {
		
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

		StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());
		// Attach it to the context so it can be invoked if connection is reset unexpectedly
		ctx.setAttachment(startStopListenerDelegate);

		try {
			request.answer(response, event, close, startStopListenerDelegate);
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

	/**
	 * Construct a proper HTTP response to a received request. After the response has been
	 * created, it is sent and the resulting {@link ChannelFuture} object is returned.
	 * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
	 * for HTTP header field definitions.
	 *
	 * @param output The {@link HttpResponse} object that will be used to construct the response.
	 * @param event The {@link MessageEvent} object used to communicate with the client that sent
	 * 			the request.
	 * @param close Set to true to close the channel after sending the response. By default the
	 * 			channel is not closed after sending.
	 * @param startStopListenerDelegate The {@link StartStopListenerDelegate} object that is used
	 * 			to notify plugins that the {@link DLNAResource} is about to start playing.
	 * @return The {@link ChannelFuture} object via which the response was sent.
	 * @throws IOException
	 */
	public ChannelFuture answer(
		HttpServletRequest req,
		HttpServletResponse resp,
		RequestServlet request,
		final boolean close,
		final StartStopListenerDelegate startStopListenerDelegate
	) throws IOException {
		long cLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit
		StringBuilder response = new StringBuilder();
		String uri = request.uri;
		String method = request.method;
		DLNAResource dlna = null;
		InputStream inputStream = null;

		// Samsung 2012 TVs have a problematic preceding slash that needs to be removed.
		if (uri.startsWith("/")) {
			LOGGER.trace("Stripping preceding slash from: " + uri);
			uri = uri.substring(1);
		}

		if (uri.startsWith("api/")) {
			ApiHandler api = new ApiHandler();
			api.handleApiRequest(null, request.content, request.output, uri.substring(4), event);
			ChannelFuture future = event.getChannel().write(output);
			if (close) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		} else if ((METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) && uri.startsWith("console/")) {
			// Request to output a page to the HTML console.
			resp.setContentType("text/html");
			response.append(HTMLConsole.servePage(uri.substring(8)));
		} else if ((METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) && uri.startsWith("get/")) {
			// Request to retrieve a file

			/**
			 * Skip the leading "get/"
			 * e.g. "get/0$1$5$3$4/Foo.mp4" -> "0$1$5$3$4/Foo.mp4"
			 *
			 * ExSport: I spotted on Android it is asking for "/get/0$2$4$2$1$3" which generates exception with response:
			 * "Http: Response, HTTP/1.1, Status: Internal server error, URL: /get/0$2$4$2$1$3"
			 * This should fix it
			 */

			// Note: we intentionally include the trailing filename here because it may
			// be used to reconstruct lost Temp items.
			String id = uri.substring(uri.indexOf("get/") + 4);

			// Some clients escape the separators in their request: unescape them.
			id = id.replace("%24", "$");

			// Retrieve the DLNAresource itself.
			if (id.startsWith(DbIdResourceLocator.DbidMediaType.GENERAL_PREFIX)) {
				try {
					dlna = dbIdResourceLocator.locateResource(id.substring(0, id.indexOf('/')));
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				dlna = PMS.get().getRootFolder(mediaRenderer).getDLNAResource(id, mediaRenderer);
			}
			String fileName = id.substring(id.indexOf('/') + 1);

			if (request.transferMode != null) {
				resp.setHeader("TransferMode.DLNA.ORG", request.transferMode);
			}

			if (dlna != null && dlna.isFolder() && !fileName.startsWith("thumbnail0000")) {
				// if we found a folder we MUST be asked for thumbnails
				// otherwise this is not allowed
				dlna = null;
			}

			if (dlna != null) {
				// DLNAresource was found.
				if (fileName.startsWith("thumbnail0000")) {
					// This is a request for a thumbnail file.
					DLNAImageProfile imageProfile = ImagesUtil.parseThumbRequest(fileName);
					resp.setHeader(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
					resp.setHeader(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
					resp.setHeader(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
					resp.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

					DLNAThumbnailInputStream thumbInputStream;
					if (!configuration.isShowCodeThumbs() && !dlna.isCodeValid(dlna)) {
						thumbInputStream = dlna.getGenericThumbnailInputStream(null);
					} else {
						dlna.checkThumbnail();
						thumbInputStream = dlna.fetchThumbnailInputStream();
					}

					BufferedImageFilterChain filterChain = null;
					if (
						(
							dlna instanceof RealFile &&
							mediaRenderer.isThumbnails() &&
							FullyPlayed.isFullyPlayedFileMark(((RealFile) dlna).getFile())
						) ||
						(
							dlna instanceof MediaLibraryFolder &&
							((MediaLibraryFolder) dlna).isTVSeries() &&
							FullyPlayed.isFullyPlayedTVSeriesMark(((MediaLibraryFolder) dlna).getName())
						)
					) {
						filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
					}
					filterChain = dlna.addFlagFilters(filterChain);
					inputStream = thumbInputStream.transcode(
						imageProfile,
						mediaRenderer != null ? mediaRenderer.isThumbnailPadding() : false,
						filterChain
					);
					if (contentFeatures != null) {
						output.headers().set(
							"ContentFeatures.DLNA.ORG",
							dlna.getDlnaContentFeatures(imageProfile, true)
						);
					}
					if (inputStream != null && (lowRange > 0 || highRange > 0)) {
						if (lowRange > 0) {
							inputStream.skip(lowRange);
						}
						inputStream = DLNAResource.wrap(inputStream, highRange, lowRange);
					}
					resp.setHeader(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
					resp.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
				} else if (dlna.getMedia() != null && dlna.getMedia().getMediaType() == MediaType.IMAGE && dlna.isCodeValid(dlna)) {
					// This is a request for an image
					Services.sleepManager().postponeSleep();
					DLNAImageProfile imageProfile = ImagesUtil.parseImageRequest(fileName, null);
					if (imageProfile == null) {
						// Parsing failed for some reason, we'll have to pick a profile
						if (dlna.getMedia().getImageInfo() != null && dlna.getMedia().getImageInfo().getFormat() != null) {
							switch (dlna.getMedia().getImageInfo().getFormat()) {
								case GIF:
									imageProfile = DLNAImageProfile.GIF_LRG;
									break;
								case PNG:
									imageProfile = DLNAImageProfile.PNG_LRG;
									break;
								default:
									imageProfile = DLNAImageProfile.JPEG_LRG;
							}
						} else {
							imageProfile = DLNAImageProfile.JPEG_LRG;
						}
					}
					resp.setHeader(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
					resp.setHeader(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
					resp.setHeader(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
					resp.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
					try {
						InputStream imageInputStream;
						if (dlna.getPlayer() instanceof ImagePlayer) {
							ProcessWrapper transcodeProcess = dlna.getPlayer().launchTranscode(
								dlna,
								dlna.getMedia(),
								new OutputParams(configuration)
							);
							imageInputStream = transcodeProcess != null ? transcodeProcess.getInputStream(0) : null;
						} else {
							imageInputStream = dlna.getInputStream();
						}
						if (imageInputStream == null) {
							LOGGER.warn("Input stream returned for \"{}\" was null, no image will be sent to renderer", fileName);
						} else {
							inputStream = DLNAImageInputStream.toImageInputStream(imageInputStream, imageProfile, false);
							if (contentFeatures != null) {
								resp.setHeader(
									"ContentFeatures.DLNA.ORG",
									dlna.getDlnaContentFeatures(imageProfile, false)
								);
							}
							if (inputStream != null && (lowRange > 0 || highRange > 0)) {
								if (lowRange > 0) {
									inputStream.skip(lowRange);
								}
								inputStream = DLNAResource.wrap(inputStream, highRange, lowRange);
							}
							resp.setHeader(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
							resp.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
						}
					} catch (IOException ie) {
						resp.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "0");
						resp.setStatus(HttpURLConnection.HTTP_UNSUPPORTED_TYPE);

						// Send the response headers to the client.
						ChannelFuture future = event.getChannel().write(output);

						if (close) {
							// Close the channel after the response is sent.
							future.addListener(ChannelFutureListener.CLOSE);
						}

						LOGGER.debug("Could not send image \"{}\": {}", dlna.getName(), ie.getMessage() != null ? ie.getMessage() : ie.getClass().getSimpleName());
						LOGGER.trace("", ie);
						return future;
					}
				} else if (dlna.getMedia() != null && fileName.contains("subtitle0000") && dlna.isCodeValid(dlna)) {
					// This is a request for a subtitles file
					resp.setContentType("text/plain");
					resp.setHeader(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
					DLNAMediaSubtitle sub = dlna.getMediaSubtitle();
					if (sub != null) {
						// XXX external file is null if the first subtitle track is embedded:
						// http://www.ps3mediaserver.org/forum/viewtopic.php?f=3&t=15805&p=75534#p75534
						if (sub.isExternal()) {
							if (sub.getExternalFile() == null && sub instanceof DLNAMediaOnDemandSubtitle) {
								// Try to fetch subtitles
								((DLNAMediaOnDemandSubtitle) sub).fetch();
							}
							if (sub.getExternalFile() == null) {
								LOGGER.error("External subtitles file \"{}\" is unavailable", sub.getName());
							} else {
								try {
									if (sub.getType() == SubtitleType.SUBRIP && mediaRenderer.isRemoveTagsFromSRTsubs()) {
										// Remove tags from .srt subtitles if the renderer doesn't support them
										inputStream = SubtitleUtils.removeSubRipTags(sub.getExternalFile());
									} else {
										inputStream = new FileInputStream(sub.getExternalFile());
									}
									LOGGER.trace("Loading external subtitles file: {}", sub.getName());
								} catch (IOException ioe) {
									LOGGER.debug("Couldn't load external subtitles file: {}\nCause: {}", sub.getName(), ioe.getMessage());
									LOGGER.trace("", ioe);
								}
							}
						} else {
							LOGGER.trace("Not sending subtitles because they are embedded: {}", sub);
						}
					} else {
						LOGGER.trace("Not sending external subtitles because dlna.getMediaSubtitle() returned null");
					}
				} else if (dlna.isCodeValid(dlna)) {
					// This is a request for a regular file.
					DLNAResource.Rendering origRendering = null;
					if (!mediaRenderer.equals(dlna.getDefaultRenderer())) {
						// Adjust rendering details for this renderer
						origRendering = dlna.updateRendering(mediaRenderer);
					}
					// If range has not been initialized yet and the DLNAResource has its
					// own start and end defined, initialize range with those values before
					// requesting the input stream.
					Range.Time splitRange = dlna.getSplitRange();

					if (range.getStart() == null && splitRange.getStart() != null) {
						range.setStart(splitRange.getStart());
					}

					if (range.getEnd() == null && splitRange.getEnd() != null) {
						range.setEnd(splitRange.getEnd());
					}

					long totalsize = dlna.length(mediaRenderer);
					boolean ignoreTranscodeByteRangeRequests = mediaRenderer.ignoreTranscodeByteRangeRequests();

					// Ignore ByteRangeRequests while media is transcoded
					if (
						!ignoreTranscodeByteRangeRequests ||
						totalsize != DLNAMediaInfo.TRANS_SIZE ||
						(
							ignoreTranscodeByteRangeRequests &&
							lowRange == 0 &&
							totalsize == DLNAMediaInfo.TRANS_SIZE
						)
					) {
						inputStream = dlna.getInputStream(Range.create(lowRange, highRange, range.getStart(), range.getEnd()), mediaRenderer);
						if (dlna.isResume()) {
							// Update range to possibly adjusted resume time
							range.setStart(dlna.getResume().getTimeOffset() / (double) 1000);
						}
					}

					Format format = dlna.getFormat();
					if (format != null && format.isVideo()) {
						MediaType mediaType = dlna.getMedia() == null ? null : dlna.getMedia().getMediaType();
						if (mediaType == MediaType.VIDEO) {
							if (
								dlna.getMedia() != null &&
								dlna.getMediaSubtitle() != null &&
								dlna.getMediaSubtitle().isExternal() &&
								!configuration.isDisableSubtitles() &&
								mediaRenderer.isExternalSubtitlesFormatSupported(dlna.getMediaSubtitle(), dlna)
							) {
								String subtitleHttpHeader = mediaRenderer.getSubtitleHttpHeader();
								if (isNotBlank(subtitleHttpHeader)  && (dlna.getPlayer() == null || mediaRenderer.streamSubsForTranscodedVideo())) {
									// Device allows a custom subtitle HTTP header; construct it
									DLNAMediaSubtitle sub = dlna.getMediaSubtitle();
									String subtitleUrl;
									String subExtension = sub.getType().getExtension();
									if (isNotBlank(subExtension)) {
										subExtension = "." + subExtension;
									}
									subtitleUrl = "http://" + PMS.get().getServer().getHost() +
										':' + PMS.get().getServer().getPort() + "/get/" +
										id.substring(0, id.indexOf('/')) + "/subtitle0000" + subExtension;

										resp.setHeader(subtitleHttpHeader, subtitleUrl);
								} else {
									LOGGER.trace(
										"Did not send subtitle headers because mediaRenderer.getSubtitleHttpHeader() returned {}",
										subtitleHttpHeader == null ? "null" : "\"" + subtitleHttpHeader + "\""
										);
								}
							} else {
								ArrayList<String> reasons = new ArrayList<>();
								if (dlna.getMedia() == null) {
									reasons.add("dlna.getMedia() is null");
								}
								if (configuration.isDisableSubtitles()) {
									reasons.add("configuration.isDisabledSubtitles() is true");
								}
								if (dlna.getMediaSubtitle() == null) {
									reasons.add("dlna.getMediaSubtitle() is null");
								} else if (!dlna.getMediaSubtitle().isExternal()) {
									reasons.add("the subtitles are internal/embedded");
								} else if (!mediaRenderer.isExternalSubtitlesFormatSupported(dlna.getMediaSubtitle(), dlna)) {
									reasons.add("the external subtitles format isn't supported by the renderer");
								}
								LOGGER.trace("Did not send subtitle headers because {}", StringUtil.createReadableCombinedString(reasons));
							}
						}
					}

					String name = dlna.getDisplayName(mediaRenderer);
					if (dlna.isNoName()) {
						name = dlna.getName() + " " + dlna.getDisplayName(mediaRenderer);
					}

					if (inputStream == null) {
						if (!ignoreTranscodeByteRangeRequests) {
							// No inputStream indicates that transcoding / remuxing probably crashed.
							LOGGER.error("There is no inputstream to return for " + name);
						}
					} else {
						// Notify plugins that the DLNAresource is about to start playing
						startStopListenerDelegate.start(dlna);

						// Try to determine the content type of the file
						String rendererMimeType = getRendererMimeType(mediaRenderer, dlna);

						if (rendererMimeType != null && !"".equals(rendererMimeType)) {
							resp.setContentType(rendererMimeType);
						}

						// Response generation:
						// We use -1 for arithmetic convenience but don't send it as a value.
						// If Content-Length < 0 we omit it, for Content-Range we use '*' to signify unspecified.
						boolean chunked = mediaRenderer.isChunkedTransfer();

						// Determine the total size. Note: when transcoding the length is
						// not known in advance, so DLNAMediaInfo.TRANS_SIZE will be returned instead.
						if (chunked && totalsize == DLNAMediaInfo.TRANS_SIZE) {
							// In chunked mode we try to avoid arbitrary values.
							totalsize = -1;
						}

						long remaining = totalsize - lowRange;
						long requested = highRange - lowRange;

						if (requested != 0) {
							// Determine the range (i.e. smaller of known or requested bytes)
							long bytes = remaining > -1 ? remaining : inputStream.available();

							if (requested > 0 && bytes > requested) {
								bytes = requested + 1;
							}

							// Calculate the corresponding highRange (this is usually redundant).
							highRange = lowRange + bytes - (bytes > 0 ? 1 : 0);

							LOGGER.trace((chunked ? "Using chunked response. " : "") + "Sending " + bytes + " bytes.");

							resp.setHeader(HttpHeaders.Names.CONTENT_RANGE, "bytes " + lowRange + "-" + (highRange > -1 ? highRange : "*") + "/" + (totalsize > -1 ? totalsize : "*"));

							// Content-Length refers to the current chunk size here, though in chunked
							// mode if the request is open-ended and totalsize is unknown we omit it.
							if (chunked && requested < 0 && totalsize < 0) {
								cLoverride = -1;
							} else {
								cLoverride = bytes;
							}
						} else {
							// Content-Length refers to the total remaining size of the stream here.
							cLoverride = remaining;
						}

						// Calculate the corresponding highRange (this is usually redundant).
						highRange = lowRange + cLoverride - (cLoverride > 0 ? 1 : 0);

						if (contentFeatures != null) {
							resp.setHeader("ContentFeatures.DLNA.ORG", dlna.getDlnaContentFeatures(mediaRenderer));
						}

						resp.setHeader(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
						resp.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
					}
					if (origRendering != null) {
						// Restore original rendering details
						dlna.updateRendering(origRendering);
					}
				}
			}
		} else if ((METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) && (uri.toLowerCase().endsWith(".png") || uri.toLowerCase().endsWith(".jpg") || uri.toLowerCase().endsWith(".jpeg"))) {
			inputStream = imageHandler(output);
		} else if ((METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) && (uri.equals("description/fetch") || uri.endsWith("1.0.xml"))) {
			resp.setContentType("text/xml; charset=\"utf-8\"");
			response.append(serverSpecHandler(output));
		} else if (METHOD_POST.equals(method) && (uri.contains("MS_MediaReceiverRegistrar_control") || uri.contains("control/x_ms_mediareceiverregistrar"))) {
			resp.setContentType("text/xml; charset=\"utf-8\"");
			response.append(msMediaReceiverRegistrarHandler());
		} else if (METHOD_POST.equals(method) && uri.endsWith("upnp/control/connection_manager")) {
			resp.setContentType("text/xml; charset=\"utf-8\"");
			if (soapaction != null && soapaction.contains("ConnectionManager:1#GetProtocolInfo")) {
				response.append(getProtocolInfoHandler());
			}
		} else if (METHOD_POST.equals(method) && uri.endsWith("upnp/control/content_directory")) {
			resp.setContentType("text/xml; charset=\"utf-8\"");
			if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSystemUpdateID")) {
				response.append(getSystemUpdateIdHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_SetBookmark")) {
				response.append(samsungSetBookmarkHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_GetFeatureList")) { // Added for Samsung 2012 TVs
				response.append(samsungGetFeaturesListHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSortCapabilities")) {
				response.append(getSortCapabilitiesHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSearchCapabilities")) {
				response.append(getSearchCapabilitiesHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Browse")) {
				response.append(browseHandler());
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Search")) {
				response.append(searchHandler());
			} else {
				LOGGER.debug("Unsupported action received: " + content);
			}
		} else if (METHOD_SUBSCRIBE.equals(method)) {
			response.append(subscribeHandler(output));
		} else if (METHOD_NOTIFY.equals(method)) {
			response.append(notifyHandler(output));
		}

		resp.setHeader(HttpHeaders.Names.SERVER, PMS.get().getServerName());

		ChannelFuture future;
		if (response.length() > 0) {
			// A response message was constructed; convert it to data ready to be sent.
			String responseString = response.toString();
			byte[] responseData = response.toString().getBytes(StandardCharsets.UTF_8);
			resp.setContentLength(responseData.length);

			// HEAD requests only require headers to be set, no need to set contents.
			if (!METHOD_HEAD.equals(method)) {
				// Not a HEAD request, so set the contents of the response.
				resp.getWriter().print(responseString);
			}
		} else if (inputStream != null) {
			// There is an input stream to send as a response.

			if (cLoverride > -2) {
				// Content-Length override has been set, send or omit as appropriate
				if (cLoverride > -1 && cLoverride != DLNAMediaInfo.TRANS_SIZE) {
					// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
					// as the PS3 will display a network error and request the last seconds of the
					// transcoded video. Better to send no Content-Length at all.
					resp.setContentLengthLong(cLoverride);
				}
			} else {
				int contentLength = inputStream.available();
				LOGGER.trace("Available Content-Length: {}", contentLength);
				resp.setContentLength(contentLength);
			}

			if (range.isStartOffsetAvailable() && dlna != null) {
				// Add timeseek information headers.
				String timeseekValue = StringUtil.formatDLNADuration(range.getStartOrZero());
				String timetotalValue = dlna.getMedia().getDurationString();
				String timeEndValue = range.isEndLimitAvailable() ? StringUtil.formatDLNADuration(range.getEnd()) : timetotalValue;
				resp.setHeader("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
				resp.setHeader("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
			}

			// Send the response headers to the client.
			future = event.getChannel().write(output);

			if (lowRange != DLNAMediaInfo.ENDFILE_POS && !METHOD_HEAD.equals(method)) {
				// Send the response body to the client in chunks.
				resp.getOutputStream().write(inputStream.readNBytes(BUFFER_SIZE));
			} else {
				// HEAD method is being used, so simply clean up after the response was sent.
				try {
					inputStream.close();
				} catch (IOException ioe) {
					LOGGER.error("Caught exception", ioe);
				}
			}
		} else {
			// No response data and no input stream. Seems we are merely serving up headers.
			resp.setContentLength(0);
			resp.setStatus(HttpURLConnection.HTTP_NO_CONTENT);
		}

		if (LOGGER.isTraceEnabled()) {
			// Log trace information
			logRequest(output, response, inputStream);
		}
	}

	/**
	 * Removes all preceding slashes from uri. Samsung 2012 TVs have a problematic (additional) preceding slash that
	 * needs to be also removed.
	 *
	 * @param rawUri requested uri
	 * @return uri without preceding slash
	 */
	private static String getUri(String rawUri) {
		String uri = rawUri;
		while (uri.startsWith("/")) {
			LOGGER.trace("Stripping preceding slash from: " + uri);
			uri = uri.substring(1);
		}
		return uri;
	}

	private static void logMessageReceived(MessageEvent event, String content, RendererConfiguration renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;
		if (event.getMessage() instanceof HttpRequest) {
			header.append(((HttpRequest) event.getMessage()).getMethod());
			header.append(" ").append(((HttpRequest) event.getMessage()).getUri());
		}
		if (event.getMessage() instanceof HttpMessage) {
			if (header.length() > 0) {
				header.append(" ");
			}
			header.append(((HttpMessage) event.getMessage()).getProtocolVersion().getText());
			header.append("\n\n");
			header.append("HEADER:\n");
			for (Entry<String, String> entry : ((HttpMessage) event.getMessage()).headers().entries()) {
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
		if (event.getChannel().getRemoteAddress() instanceof InetSocketAddress) {
			rendererName +=
				" (" + ((InetSocketAddress) event.getChannel().getRemoteAddress()).getAddress().getHostAddress() +
				":" + ((InetSocketAddress) event.getChannel().getRemoteAddress()).getPort() + ")";
		}

		if (isNotBlank(requestType)) {
			LOGGER.trace(
				"Received a {}request from {}:\n\n{}{}",
				requestType,
				rendererName,
				header,
				StringUtils.isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : ""
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

	private static List<Map.Entry<String, String>> getHttpHeaders(HttpServletRequest request) {
		List<Map.Entry<String, String>> headers = new ArrayList<>();
		for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements(); ) {
				headers.add(new AbstractMap.SimpleEntry<>(name, (String) values.nextElement()));
			}
		}
		return headers;
	}
}
