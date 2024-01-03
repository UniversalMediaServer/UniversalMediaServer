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
package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.dlna.ByteRange;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.media.MediaType;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.Range;
import net.pms.dlna.RealFile;
import net.pms.dlna.TimeRange;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.ImageEngine;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.handlers.message.BrowseRequest;
import net.pms.network.mediaserver.handlers.message.BrowseSearchRequest;
import net.pms.network.mediaserver.handlers.message.SamsungBookmark;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.service.Services;
import net.pms.service.StartStopListenerDelegate;
import net.pms.service.sleep.SleepManager;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class RequestHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static final String GET = "GET";
	private static final String HEAD = "HEAD";
	private static final String POST = "POST";
	private static final String SUBSCRIBE = "SUBSCRIBE";
	private static final String NOTIFY = "NOTIFY";
	private static final String CRLF = "\r\n";
	private static final String CONTENT_TYPE_XML_UTF8 = "text/xml; charset=\"utf-8\"";
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final Pattern DIDL_PATTERN = Pattern.compile("<Result>(&lt;DIDL-Lite.*?)</Result>");

	private static final String HTTPSERVER_REQUEST_BEGIN =  "================================== HTTPSERVER REQUEST BEGIN =====================================";
	private static final String HTTPSERVER_REQUEST_END =    "================================== HTTPSERVER REQUEST END =======================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "================================== HTTPSERVER RESPONSE BEGIN ====================================";
	private static final String HTTPSERVER_RESPONSE_END =   "================================== HTTPSERVER RESPONSE END ======================================";

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		Renderer renderer = null;
		try {
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			String userAgentString = exchange.getRequestHeaders().getFirst("User-Agent");
			// Is the request from our own JUPnP service, i.e. self-originating?
			boolean isSelf = ia.getHostAddress().equals(MediaServer.getHost()) &&
					userAgentString != null &&
					userAgentString.contains("UMS/");
			// Filter if required
			if (isSelf || filterIp(ia)) {
				exchange.close();
				return;
			}

			String uri = exchange.getRequestURI().getPath();
			Collection<Map.Entry<String, String>> headers = getHeaders(exchange);
			renderer = getRenderer(uri, ia, userAgentString, headers);
			if (renderer == null) {
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				exchange.close();
				return;
			}

			if (exchange.getRequestHeaders().containsKey("X-PANASONIC-DMP-Profile")) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(exchange.getRequestHeaders().getFirst("X-PANASONIC-DMP-Profile"), renderer);
			}

			String requestBody = null;
			if (exchange.getRequestHeaders().containsKey("Content-Length")) {
				int contentLength = 0;
				try {
					contentLength = Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));
				} catch (NumberFormatException e) {
				}
				if (contentLength > 0) {
					byte[] data = new byte[contentLength];
					exchange.getRequestBody().read(data);
					requestBody = new String(data, StandardCharsets.UTF_8);
				} else {
					requestBody = "";
				}
			}
			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(exchange, requestBody, renderer);
			}

			String soapaction = null;
			if (exchange.getRequestHeaders().containsKey("SOAPACTION")) {
				soapaction = exchange.getRequestHeaders().getFirst("SOAPACTION");
			} else if (exchange.getRequestHeaders().containsKey("CALLBACK")) {
				soapaction = exchange.getRequestHeaders().getFirst("CALLBACK");
			}

			String method = exchange.getRequestMethod().toUpperCase();

			// Samsung 2012 TVs have a problematic preceding slash that needs to be removed.
			if (uri.startsWith("/")) {
				LOGGER.trace("Stripping preceding slash from: " + uri);
				uri = uri.substring(1);
			}

			if ((GET.equals(method) || HEAD.equals(method)) && uri.startsWith("get/")) {
				sendGetResponse(exchange, renderer, uri);
			} else if ((GET.equals(method) || HEAD.equals(method)) && (uri.toLowerCase().endsWith(".png") || uri.toLowerCase().endsWith(".jpg") || uri.toLowerCase().endsWith(".jpeg"))) {
				sendResponse(exchange, renderer, 200, imageHandler(exchange, uri));

			//------------------------- START ContentDirectory -------------------------
			} else if (GET.equals(method) && uri.endsWith("/ContentDirectory/desc")) {
				sendResponse(exchange, renderer, 200, contentDirectorySpec(exchange), CONTENT_TYPE_XML_UTF8);
			} else if (POST.equals(method) && uri.endsWith("/ContentDirectory/action")) {
				if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSystemUpdateID")) {
					sendResponse(exchange, renderer, 200, getSystemUpdateIdHandler(), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_SetBookmark")) {
					sendResponse(exchange, renderer, 200, samsungSetBookmarkHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_GetFeatureList")) { // Added for Samsung 2012 TVs
					sendResponse(exchange, renderer, 200, samsungGetFeaturesListHandler(renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSortCapabilities")) {
					sendResponse(exchange, renderer, 200, getSortCapabilitiesHandler(), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSearchCapabilities")) {
					sendResponse(exchange, renderer, 200, getSearchCapabilitiesHandler(renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Browse")) {
					sendResponse(exchange, renderer, 200, browseHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Search")) {
					sendResponse(exchange, renderer, 200, searchHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else {
					LOGGER.debug("Unsupported action received: " + requestBody);
					sendResponse(exchange, renderer, 200, notifyHandler(exchange), "text/xml");
				}
			} else if (SUBSCRIBE.equals(method)) {
				sendResponse(exchange, renderer, 200, subscribeHandler(exchange, uri, soapaction), CONTENT_TYPE_XML_UTF8);
			} else if (NOTIFY.equals(method)) {
				sendResponse(exchange, renderer, 200, notifyHandler(exchange), CONTENT_TYPE_XML_UTF8);
			//------------------------- END ContentDirectory -------------------------
			}
		} catch (IOException e) {
			String message = e.getMessage();
			if (message != null) {
				if (message.equals("Connection reset by peer")) {
					LOGGER.trace("Http request from {}: {}", getRendererName(exchange, renderer), message);
				}
			} else {
				LOGGER.error("Http request error:", e);
			}
		}
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, String message, String contentType) throws IOException {
		exchange.getResponseHeaders().set("Server", PMS.get().getServerName());
		exchange.getResponseHeaders().set("Content-Type", contentType);
		if (message == null || message.length() == 0) {
			// No response data. Seems we are merely serving up headers.
			exchange.sendResponseHeaders(204, 0);
			if (LOGGER.isTraceEnabled()) {
				logMessageSent(exchange, null, null, renderer);
			}
			return;
		}
		// A response message was constructed; convert it to data ready to be sent.
		byte[] responseData = message.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(code, responseData.length);
		// HEAD requests only require headers to be set, no need to set contents.
		if (!HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			// Not a HEAD request, so set the contents of the response.
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(responseData);
				os.flush();
			}
		}
		exchange.close();
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, message, null, renderer);
		}
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, InputStream inputStream) throws IOException {
		sendResponse(exchange, renderer, code, inputStream, -2, true);
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, InputStream inputStream, long cLoverride, boolean writeStream) throws IOException {
		// There is an input stream to send as a response.
		exchange.getResponseHeaders().set("Server", PMS.get().getServerName());
		if (inputStream == null) {
			// No input stream. Seems we are merely serving up headers.
			exchange.sendResponseHeaders(204, 0);
			if (LOGGER.isTraceEnabled()) {
				logMessageSent(exchange, null, null, renderer);
			}
			return;
		}
		long contentLength = 0;
		if (cLoverride > -2) {
			// Content-Length override has been set, send or omit as appropriate
			if (cLoverride == 0) {
				//mean no content, HttpExchange use the -1 value for it.
				contentLength = -1;
			} else if (cLoverride > -1 && cLoverride != MediaInfo.TRANS_SIZE) {
				// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
				// as the PS3 will display a network error and request the last seconds of the
				// transcoded video. Better to send no Content-Length at all.
				contentLength = cLoverride;
			} else if (cLoverride == -1) {
				//chunked, HttpExchange use the 0 value for it.
				contentLength = 0;
			}
		} else {
			contentLength = inputStream.available();
			LOGGER.trace("Available Content-Length: {}", contentLength);
		}
		if (contentLength > 0) {
			exchange.getResponseHeaders().set("Content-length", Long.toString(contentLength));
		}

		// Send the response headers to the client.
		exchange.sendResponseHeaders(code, contentLength);
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, null, inputStream, renderer);
		}
		// send only if no HEAD method is being used.
		if (writeStream && !HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			// Send the response body to the client in chunks.
			byte[] buf = new byte[BUFFER_SIZE];
			int length;
			try (OutputStream outputStream = exchange.getResponseBody()) {
				int lengthSent = 0;
				try {
					while ((length = inputStream.read(buf)) > 0) {
						outputStream.write(buf, 0, length);
						outputStream.flush();
						lengthSent += length;
					}
				} catch (IOException ioe) {
					//client close the connection
				}
				try {
					outputStream.close();
				} catch (IOException ioe) {
					//client close the connection and insufficient bytes written to stream
				}
				LOGGER.trace("OutputStream({}) - bytes sent: {}/{}", outputStream.getClass().getName(), lengthSent, contentLength);
			}
		}
		try {
			inputStream.close();
		} catch (IOException ioe) {
			LOGGER.error("Caught exception", ioe);
		}
		exchange.close();
	}

	private static void sendGetResponse(final HttpExchange exchange, final Renderer renderer, String uri) throws IOException {
		// Request to retrieve a file
		TimeRange timeseekrange = getTimeSeekRange(exchange.getRequestHeaders().getFirst("timeseekrange.dlna.org"));
		ByteRange range = getRange(exchange.getRequestHeaders().getFirst("Range"));
		int status = (range.getStart() != 0 || range.getEnd() != 0) ? 206 : 200;
		StartStopListenerDelegate startStopListenerDelegate = null;
		DLNAResource dlna = null;
		InputStream inputStream = null;
		long cLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit
		/**
		 * Skip the leading "get/" e.g. "get/0$1$5$3$4/Foo.mp4" ->
		 * "0$1$5$3$4/Foo.mp4"
		 *
		 * ExSport: I spotted on Android it is asking for "/get/0$2$4$2$1$3"
		 * which generates exception with response: "Http: Response, HTTP/1.1,
		 * Status: Internal server error, URL: /get/0$2$4$2$1$3" This should fix
		 * it
		 */

		// Note: we intentionally include the trailing filename here because it may
		// be used to reconstruct lost Temp items.
		String id = uri.substring(uri.indexOf("get/") + 4);

		// Some clients escape the separators in their request: unescape them.
		id = id.replace("%24", "$");

		// Retrieve the DLNAresource itself.
		if (id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
			try {
				dlna = DbIdResourceLocator.locateResource(id.substring(0, id.indexOf('/')), renderer);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		} else {
			dlna = PMS.get().getRootFolder(renderer).getDLNAResource(id, renderer);
		}
		String fileName = id.substring(id.indexOf('/') + 1);

		if (exchange.getRequestHeaders().containsKey("transfermode.dlna.org")) {
			exchange.getResponseHeaders().set("TransferMode.DLNA.ORG", exchange.getRequestHeaders().getFirst("transfermode.dlna.org"));
		}

		if (dlna != null && dlna.isFolder() && !fileName.startsWith("thumbnail0000")) {
			// if we found a folder we MUST be asked for thumbnails
			// otherwise this is not allowed
			dlna = null;
		}

		if (dlna != null) {
			String contentFeatures = null;
			if (exchange.getRequestHeaders().containsKey("getcontentfeatures.dlna.org")) {
				contentFeatures = exchange.getRequestHeaders().getFirst("getcontentfeatures.dlna.org");
			}
			// DLNAresource was found.
			if (fileName.endsWith("/chapters.vtt")) {
				sendResponse(exchange, renderer, 200, MediaChapter.getWebVtt(dlna), HTTPResource.WEBVTT_TYPEMIME);
				return;
			} else if (fileName.endsWith("/chapters.json")) {
				sendResponse(exchange, renderer, 200, MediaChapter.getHls(dlna), HTTPResource.JSON_TYPEMIME);
				return;
			} else if (fileName.startsWith("hls/")) {
				//HLS
				if (fileName.endsWith(".m3u8")) {
					//HLS rendition m3u8 file
					String rendition = fileName.replace("hls/", "").replace(".m3u8", "");
					if (HlsHelper.getByKey(rendition) != null) {
						sendResponse(exchange, renderer, 200, HlsHelper.getHLSm3u8ForRendition(dlna, renderer, "/get/", rendition), HTTPResource.HLS_TYPEMIME);
					} else {
						sendResponse(exchange, renderer, 404, null);
					}
				} else {
					//HLS stream request
					inputStream = HlsHelper.getInputStream("/" + fileName, dlna, renderer);
					if (inputStream != null) {
						if (fileName.endsWith(".ts")) {
							exchange.getResponseHeaders().set("Content-Type", HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
						} else if (fileName.endsWith(".vtt")) {
							exchange.getResponseHeaders().set("Content-Type", HTTPResource.WEBVTT_TYPEMIME);
						}
						sendResponse(exchange, renderer, 200, inputStream, MediaInfo.TRANS_SIZE, true);
					} else {
						sendResponse(exchange, renderer, 404, null);
					}
				}
				return;
			} else if (fileName.endsWith("_transcoded_to.m3u8")) {
				//HLS start m3u8 file
				if (contentFeatures != null) {
					//output.headers().set("transferMode.dlna.org", "Streaming");
					if (dlna.getMedia().getDurationInSeconds() > 0) {
						String durationStr = String.format(Locale.ENGLISH, "%.3f", dlna.getMedia().getDurationInSeconds());
						exchange.getResponseHeaders().set("TimeSeekRange.dlna.org", "npt=0-" + durationStr + "/" + durationStr);
						exchange.getResponseHeaders().set("X-AvailableSeekRange", "npt=0-" + durationStr);
						//only time seek, transcoded
						exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG", "DLNA.ORG_OP=10;DLNA.ORG_CI=01;DLNA.ORG_FLAGS=01700000000000000000000000000000");
					}
				}
				sendResponse(exchange, renderer, 200, HlsHelper.getHLSm3u8(dlna, renderer, "/get/"), HTTPResource.HLS_TYPEMIME);
				return;
			} else if (fileName.startsWith("thumbnail0000")) {
				// This is a request for a thumbnail file.
				DLNAImageProfile imageProfile = ImagesUtil.parseThumbRequest(fileName);
				exchange.getResponseHeaders().set("Content-Type", imageProfile.getMimeType().toString());
				exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
				exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
				exchange.getResponseHeaders().set("Connection", "keep-alive");

				DLNAThumbnailInputStream thumbInputStream;
				if (!CONFIGURATION.isShowCodeThumbs() && !dlna.isCodeValid(dlna)) {
					thumbInputStream = dlna.getGenericThumbnailInputStream(null);
				} else {
					dlna.checkThumbnail();
					thumbInputStream = dlna.fetchThumbnailInputStream();
				}

				BufferedImageFilterChain filterChain = null;
				if ((dlna instanceof RealFile &&
						renderer.isThumbnails() &&
						FullyPlayed.isFullyPlayedFileMark(((RealFile) dlna).getFile())) ||
						(dlna instanceof MediaLibraryFolder &&
						((MediaLibraryFolder) dlna).isTVSeries() &&
						FullyPlayed.isFullyPlayedTVSeriesMark(((MediaLibraryFolder) dlna).getName()))) {
					filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
				}
				filterChain = dlna.addFlagFilters(filterChain);
				inputStream = thumbInputStream.transcode(
						imageProfile,
						renderer != null && renderer.isThumbnailPadding(),
						filterChain
				);
				if (contentFeatures != null) {
					exchange.getResponseHeaders().set(
							"ContentFeatures.DLNA.ORG",
							dlna.getDlnaContentFeatures(imageProfile, true)
					);
				}
				if (inputStream != null && (range.getStart() > 0 || range.getEnd() > 0)) {
					if (range.getStart() > 0) {
						inputStream.skip(range.getStart());
					}
					inputStream = DLNAResource.wrap(inputStream, range.getEnd(), range.getStart());
				}
			} else if (dlna.getMedia() != null && dlna.getMedia().getMediaType() == MediaType.IMAGE && dlna.isCodeValid(dlna)) {
				// This is a request for an image
				SleepManager sleepManager = Services.sleepManager();
				if (sleepManager != null) {
					sleepManager.postponeSleep();
				}

				DLNAImageProfile imageProfile = ImagesUtil.parseImageRequest(fileName, null);
				if (imageProfile == null) {
					// Parsing failed for some reason, we'll have to pick a profile
					if (dlna.getMedia().getImageInfo() != null && dlna.getMedia().getImageInfo().getFormat() != null) {
						imageProfile = switch (dlna.getMedia().getImageInfo().getFormat()) {
							case GIF -> DLNAImageProfile.GIF_LRG;
							case PNG -> DLNAImageProfile.PNG_LRG;
							default -> DLNAImageProfile.JPEG_LRG;
						};
					} else {
						imageProfile = DLNAImageProfile.JPEG_LRG;
					}
				}
				exchange.getResponseHeaders().set("Content-Type", imageProfile.getMimeType().toString());
				exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
				exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
				exchange.getResponseHeaders().set("Connection", "keep-alive");
				try {
					InputStream imageInputStream;
					if (dlna.getEngine() instanceof ImageEngine) {
						ProcessWrapper transcodeProcess = dlna.getEngine().launchTranscode(
								dlna,
								dlna.getMedia(),
								new OutputParams(CONFIGURATION)
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
							exchange.getResponseHeaders().set(
									"ContentFeatures.DLNA.ORG",
									dlna.getDlnaContentFeatures(imageProfile, false)
							);
						}
						if (inputStream != null && (range.getStart() > 0 || range.getEnd() > 0)) {
							if (range.getStart() > 0) {
								inputStream.skip(range.getStart());
							}
							inputStream = DLNAResource.wrap(inputStream, range.getEnd(), range.getStart());
						}
					}
				} catch (IOException ie) {
					exchange.sendResponseHeaders(415, 0);
					LOGGER.debug("Could not send image \"{}\": {}", dlna.getName(), ie.getMessage() != null ? ie.getMessage() : ie.getClass().getSimpleName());
					LOGGER.trace("", ie);
					return;
				}
			} else if (dlna.getMedia() != null && fileName.contains("subtitle0000") && dlna.isCodeValid(dlna)) {
				// This is a request for a subtitles file
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
				MediaSubtitle sub = dlna.getMediaSubtitle();
				if (sub != null) {
					// XXX external file is null if the first subtitle track is embedded
					if (sub.isExternal()) {
						if (sub.getExternalFile() == null && sub instanceof MediaOnDemandSubtitle) {
							// Try to fetch subtitles
							((MediaOnDemandSubtitle) sub).fetch();
						}
						if (sub.getExternalFile() == null) {
							LOGGER.error("External subtitles file \"{}\" is unavailable", sub.getName());
						} else {
							try {
								if (sub.getType() == SubtitleType.SUBRIP && renderer.isRemoveTagsFromSRTsubs()) {
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
				if (!renderer.equals(dlna.getDefaultRenderer())) {
					// Adjust rendering details for this renderer
					origRendering = dlna.updateRendering(renderer);
				}
				// If range has not been initialized yet and the DLNAResource has its
				// own start and end defined, initialize range with those values before
				// requesting the input stream.
				TimeRange splitRange = dlna.getSplitRange();

				if (timeseekrange.getStart() == null && splitRange.getStart() != null) {
					timeseekrange.setStart(splitRange.getStart());
				}

				if (timeseekrange.getEnd() == null && splitRange.getEnd() != null) {
					timeseekrange.setEnd(splitRange.getEnd());
				}

				long totalsize = dlna.length(renderer);
				boolean ignoreTranscodeByteRangeRequests = renderer.ignoreTranscodeByteRangeRequests();

				// Ignore ByteRangeRequests while media is transcoded
				if (!ignoreTranscodeByteRangeRequests ||
						totalsize != MediaInfo.TRANS_SIZE ||
						(ignoreTranscodeByteRangeRequests &&
						range.getStart() == 0 &&
						totalsize == MediaInfo.TRANS_SIZE)) {
					inputStream = dlna.getInputStream(Range.create(range.getStart(), range.getEnd(), timeseekrange.getStart(), timeseekrange.getEnd()), renderer);
					if (dlna.isResume()) {
						// Update range to possibly adjusted resume time
						timeseekrange.setStart(dlna.getResume().getTimeOffset() / (double) 1000);
					}
				}

				/**
				 * LG TVs send us many "play" requests while browsing directories, in order
				 * for them to show dynamic thumbnails. That means we can skip certain things
				 * like searching for subtitles and fully played logic.
				 */
				String userAgentString = exchange.getRequestHeaders().getFirst("User-Agent");
				boolean isVideoThumbnailRequest = renderer.isLG() && userAgentString != null && userAgentString.contains("Lavf/");

				Format format = dlna.getFormat();
				if (!isVideoThumbnailRequest && format != null && format.isVideo()) {
					MediaType mediaType = dlna.getMedia() == null ? null : dlna.getMedia().getMediaType();
					if (mediaType == MediaType.VIDEO) {
						if (dlna.getMedia() != null &&
								dlna.getMediaSubtitle() != null &&
								dlna.getMediaSubtitle().isExternal() &&
								!CONFIGURATION.isDisableSubtitles() &&
								renderer.isExternalSubtitlesFormatSupported(dlna.getMediaSubtitle(), dlna)) {
							String subtitleHttpHeader = renderer.getSubtitleHttpHeader();
							if (StringUtils.isNotBlank(subtitleHttpHeader) && (dlna.getEngine() == null || renderer.streamSubsForTranscodedVideo())) {
								// Device allows a custom subtitle HTTP header; construct it
								MediaSubtitle sub = dlna.getMediaSubtitle();
								String subtitleUrl;
								String subExtension = sub.getType().getExtension();
								if (StringUtils.isNotBlank(subExtension)) {
									subExtension = "." + subExtension;
								}
								subtitleUrl = MediaServer.getURL() + "/get/" +
										id.substring(0, id.indexOf('/')) + "/subtitle0000" + subExtension;

								exchange.getResponseHeaders().set(subtitleHttpHeader, subtitleUrl);
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
							if (CONFIGURATION.isDisableSubtitles()) {
								reasons.add("configuration.isDisabledSubtitles() is true");
							}
							if (dlna.getMediaSubtitle() == null) {
								reasons.add("dlna.getMediaSubtitle() is null");
							} else if (!dlna.getMediaSubtitle().isExternal()) {
								reasons.add("the subtitles are internal/embedded");
							} else if (!renderer.isExternalSubtitlesFormatSupported(dlna.getMediaSubtitle(), dlna)) {
								reasons.add("the external subtitles format isn't supported by the renderer");
							}
							LOGGER.trace("Did not send subtitle headers because {}", StringUtil.createReadableCombinedString(reasons));
						}
					}
				}

				String name = dlna.getDisplayName(renderer);
				if (dlna.isNoName()) {
					name = dlna.getName() + " " + dlna.getDisplayName(renderer);
				}

				if (inputStream == null) {
					if (!ignoreTranscodeByteRangeRequests) {
						// No inputStream indicates that transcoding / remuxing probably crashed.
						LOGGER.error("There is no inputstream to return for " + name);
					}
				} else {
					if (!isVideoThumbnailRequest) {
						startStopListenerDelegate = new StartStopListenerDelegate(exchange.getRemoteAddress().getAddress().getHostAddress());
						startStopListenerDelegate.start(dlna);
					}

					// Try to determine the content type of the file
					String rendererMimeType = renderer.getMimeType(dlna);

					if (rendererMimeType != null && !"".equals(rendererMimeType)) {
						exchange.getResponseHeaders().set("Content-Type", rendererMimeType);
					}

					// Response generation:
					// We use -1 for arithmetic convenience but don't send it as a value.
					// If Content-Length < 0 we omit it, for Content-Range we use '*' to signify unspecified.
					boolean chunked = renderer.isChunkedTransfer();

					// Determine the total size. Note: when transcoding the length is
					// not known in advance, so MediaInfo.TRANS_SIZE will be returned instead.
					if (chunked && totalsize == MediaInfo.TRANS_SIZE) {
						// In chunked mode we try to avoid arbitrary values.
						totalsize = -1;
					}

					long remaining = totalsize - range.getStart();
					long requested = range.getEnd() - range.getStart();

					if (requested != 0) {
						// Determine the range (i.e. smaller of known or requested bytes)
						long bytes = remaining > -1 ? remaining : inputStream.available();

						if (requested > 0 && bytes > requested) {
							bytes = requested + 1;
						}

						// Calculate the corresponding highRange (this is usually redundant).
						range.setEnd(range.getStart() + bytes - (bytes > 0 ? 1 : 0));

						LOGGER.trace((chunked ? "Using chunked response. " : "") + "Sending " + bytes + " bytes.");

						exchange.getResponseHeaders().set("Content-Range", "bytes " + range.getStart() + "-" + (range.getEnd() > -1 ? range.getEnd() : "*") + "/" + (totalsize > -1 ? totalsize : "*"));

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
					range.setEnd(range.getStart() + cLoverride - (cLoverride > 0 ? 1 : 0));

					if (contentFeatures != null) {
						exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG", dlna.getDlnaContentFeatures(renderer));
					}

					exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
					exchange.getResponseHeaders().set("Connection", "keep-alive");
				}
				if (origRendering != null) {
					// Restore original rendering details
					dlna.updateRendering(origRendering);
				}
			}
		}
		if (timeseekrange.isStartOffsetAvailable() && dlna != null) {
			// Add timeseek information headers.
			String timeseekValue = StringUtil.formatDLNADuration(timeseekrange.getStartOrZero());
			String timetotalValue = dlna.getMedia().getDurationString();
			String timeEndValue = timeseekrange.isEndLimitAvailable() ? StringUtil.formatDLNADuration(timeseekrange.getEnd()) : timetotalValue;
			exchange.getResponseHeaders().set("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
			exchange.getResponseHeaders().set("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
		}
		try {
			sendResponse(exchange, renderer, status, inputStream, cLoverride, (range.getStart() != MediaInfo.ENDFILE_POS));
		} finally {
			if (startStopListenerDelegate != null) {
				startStopListenerDelegate.stop();
			}
		}
	}

	private static ByteRange getRange(String rangeStr) {
		ByteRange range = new ByteRange(0L, 0L);
		if (rangeStr == null || StringUtils.isEmpty(rangeStr)) {
			return range;
		}
		rangeStr = rangeStr.toLowerCase().trim();
		if (!rangeStr.startsWith("bytes=")) {
			LOGGER.warn("Range '" + rangeStr + "' does not start with 'bytes='");
			return range;
		}
		rangeStr = rangeStr.substring(6);
		int dashPos = rangeStr.indexOf('-');
		if (dashPos > 0) {
			long firstPos = Long.parseLong(rangeStr.substring(0, dashPos));
			if (dashPos < rangeStr.length() - 1) {
				Long lastPos = Long.valueOf(rangeStr.substring(dashPos + 1, rangeStr.length()));
				return new ByteRange(firstPos, lastPos);
			} else {
				return new ByteRange(firstPos, -1L);
			}
		} else if (dashPos == 0) {
			return new ByteRange(0L, Long.valueOf(rangeStr.substring(1)));
		}
		LOGGER.warn("Range '" + rangeStr + "' is not well formed");
		return range;
	}

	private static TimeRange getTimeSeekRange(String timeSeekRangeStr) {
		TimeRange timeSeekRange = new TimeRange();
		if (timeSeekRangeStr != null && timeSeekRangeStr.startsWith("npt=")) {
			String[] params = timeSeekRangeStr.substring(4).split("[-/]");
			if (params.length > 1 && params[1].length() != 0) {
				timeSeekRange.setEnd(StringUtil.convertStringToTime(params[1]));
			}
			if (params.length > 0 && params[0].length() != 0) {
				timeSeekRange.setStart(StringUtil.convertStringToTime(params[0]));
			}
		}
		return timeSeekRange;
	}

	private static Collection<Map.Entry<String, String>> getHeaders(HttpExchange exchange) {
		HashMap<String, String> headers = new HashMap<>();
		for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
			String name = header.getKey();
			for (String value : header.getValue()) {
				headers.put(name, value);
			}
		}
		return headers.entrySet();
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 *
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null
	 * otherwise.
	 */
	private static InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replace("//", "/");
		ClassLoader cll = RequestHandler.class.getClassLoader();
		InputStream is = cll.getResourceAsStream(fileName.substring(1));

		while (is == null && cll.getParent() != null) {
			cll = cll.getParent();
			is = cll.getResourceAsStream(fileName.substring(1));
		}

		return is;
	}

	private static InputStream imageHandler(HttpExchange exchange, String uri) {
		if (uri.toLowerCase().endsWith(".png")) {
			exchange.getResponseHeaders().set("Content-Type", "image/png");
		} else {
			exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
		}
		exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
		return getResourceInputStream(uri);
	}

	//------------------------- START ContentDirectory -------------------------
	private static String contentDirectorySpec(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Cache-Control", "no-cache");
		exchange.getResponseHeaders().set("Expires", "0");
		exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		InputStream iStream = getResourceInputStream("UPnP_AV_ContentDirectory_1.0.xml");

		byte[] b = new byte[iStream.available()];
		iStream.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/**
	 * Wraps the payload around soap Envelope / Body tags.
	 *
	 * @param payload Soap body as a XML String
	 * @return Soap message as a XML string
	 */
	private static StringBuilder createResponse(String payload) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.XML_HEADER).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER).append(CRLF);
		response.append(payload).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER).append(CRLF);
		return response;
	}

	private static String getSystemUpdateIdHandler() {
		StringBuilder payload = new StringBuilder();
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_HEADER).append(CRLF);
		payload.append("<Id>").append(DLNAResource.getSystemUpdateId()).append("</Id>").append(CRLF);
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_FOOTER);
		return createResponse(payload.toString()).toString();
	}

	private static String samsungSetBookmarkHandler(String requestBody, Renderer renderer) {
		LOGGER.debug("Setting bookmark");
		SamsungBookmark payload = getPayload(SamsungBookmark.class, requestBody);
		if (payload.getPosSecond() == 0) {
			// Sometimes when Samsung device is starting to play the video
			// it sends X_SetBookmark message immediately with the position=0.
			// No need to update database in such case.
			LOGGER.debug("Skipping \"set bookmark\". Position=0");
		} else {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					DLNAResource dlna = PMS.get().getRootFolder(renderer).getDLNAResource(payload.getObjectId(), renderer);
					File file = new File(dlna.getFileName());
					String path = file.getCanonicalPath();
					MediaTableFilesStatus.setBookmark(connection, path, payload.getPosSecond());
				}
			} catch (IOException e) {
				LOGGER.error("Cannot set bookmark", e);
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return createResponse(HTTPXMLHelper.SETBOOKMARK_RESPONSE).toString();
	}

	private static <T> T getPayload(Class<T> clazz, String requestBody) {
		try {
			SOAPMessage message = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(requestBody.getBytes()));
			JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Document body = message.getSOAPBody().extractContentAsDocument();
			return unmarshaller.unmarshal(body, clazz).getValue();
		} catch (JAXBException | SOAPException | IOException e) {
			LOGGER.error("Unmarshalling error", e);
			return null;
		}
	}

	private static String samsungGetFeaturesListHandler(Renderer renderer) {
		StringBuilder features = new StringBuilder();
		String rootFolderId = PMS.get().getRootFolder(renderer).getResourceId();
		features.append("<Features xmlns=\"urn:schemas-upnp-org:av:avs\"");
		features.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		features.append(" xsi:schemaLocation=\"urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd\">").append(CRLF);
		features.append("<Feature name=\"samsung.com_BASICVIEW\" version=\"1\">").append(CRLF);
		// we may use here different container IDs in the future
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.audioItem\"/>").append(CRLF);
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.videoItem\"/>").append(CRLF);
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.imageItem\"/>").append(CRLF);
		features.append("</Feature>").append(CRLF);
		features.append("</Features>").append(CRLF);

		StringBuilder response = new StringBuilder();
		response.append("<u:X_GetFeatureListResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">").append(CRLF);
		response.append("<FeatureList>").append(CRLF);

		response.append(StringEscapeUtils.escapeXml10(features.toString()));

		response.append("</FeatureList>").append(CRLF);
		response.append("</u:X_GetFeatureListResponse>");
		return createResponse(response.toString()).toString();
	}

	private static String getSortCapabilitiesHandler() {
		return createResponse(HTTPXMLHelper.SORTCAPS_RESPONSE).toString();
	}

	private static String getSearchCapabilitiesHandler(Renderer mediaRenderer) {
		if (mediaRenderer.isUpnpSearchCapsEnabled()) {
			return createResponse(HTTPXMLHelper.SEARCHCAPS_RESPONSE).toString();
		} else {
			return createResponse(HTTPXMLHelper.SEARCHCAPS_RESPONSE_SEARCH_DEACTIVATED).toString();
		}
	}

	private static String browseHandler(String requestBody, Renderer renderer) {
		BrowseRequest requestMessage = getPayload(BrowseRequest.class, requestBody);
		return browseSearchHandler(requestMessage, requestBody, renderer).toString();
	}

	private static String searchHandler(String requestBody, Renderer renderer) {
		SearchRequest requestMessage = getPayload(SearchRequest.class, requestBody);
		try {
			return new SearchRequestHandler().createSearchResponse(requestMessage, renderer).toString();
		} catch (Exception e) {
			LOGGER.trace("error transforming searchCriteria to SQL. Fallback to content browsing ...", e);
			return browseHandler(requestBody, renderer);
		}
	}

	/**
	 * Hybrid handler for Browse and Search requests.
	 *
	 * @param requestMessage parsed message
	 * @return Soap response as a XML string
	 */
	private static StringBuilder browseSearchHandler(BrowseSearchRequest requestMessage, String requestBody, Renderer renderer) {
		int startingIndex = 0;
		int requestCount = 0;
		boolean xbox360 = renderer.isXbox360();
		String objectID = requestMessage.getObjectId();
		String containerID = null;
		if ((objectID == null || objectID.length() == 0)) {
			containerID = requestMessage.getContainerId();
			if (containerID == null || (xbox360 && !containerID.contains("$"))) {
				objectID = "0";
			} else {
				objectID = containerID;
				containerID = null;
			}
		}
		Integer sI = requestMessage.getStartingIndex();
		Integer rC = requestMessage.getRequestedCount();
		String browseFlag = requestMessage.getBrowseFlag();

		if (sI != null) {
			startingIndex = sI;
		}

		if (rC != null) {
			requestCount = rC;
		}

		boolean browseDirectChildren = browseFlag != null && browseFlag.equals("BrowseDirectChildren");

		if (requestMessage instanceof SearchRequest) {
			browseDirectChildren = true;
		}

		// Xbox 360 virtual containers ... d'oh!
		String searchCriteria = null;
		if (xbox360 && CONFIGURATION.getUseCache() && PMS.get().getLibrary().isEnabled() && containerID != null) {
			if (containerID.equals("7") && PMS.get().getLibrary().getAlbumFolder() != null) {
				objectID = PMS.get().getLibrary().getAlbumFolder().getResourceId();
			} else if (containerID.equals("6") && PMS.get().getLibrary().getArtistFolder() != null) {
				objectID = PMS.get().getLibrary().getArtistFolder().getResourceId();
			} else if (containerID.equals("5") && PMS.get().getLibrary().getGenreFolder() != null) {
				objectID = PMS.get().getLibrary().getGenreFolder().getResourceId();
			} else if (containerID.equals("F") && PMS.get().getLibrary().getPlaylistFolder() != null) {
				objectID = PMS.get().getLibrary().getPlaylistFolder().getResourceId();
			} else if (containerID.equals("4") && PMS.get().getLibrary().getAllFolder() != null) {
				objectID = PMS.get().getLibrary().getAllFolder().getResourceId();
			} else if (containerID.equals("1")) {
				String artist = getEnclosingValue(requestBody, "upnp:artist = &quot;", "&quot;)");
				if (artist != null) {
					objectID = PMS.get().getLibrary().getArtistFolder().getResourceId();
					searchCriteria = artist;
				}
			}
		} else if (requestMessage instanceof SearchRequest) {
			searchCriteria = requestMessage.getSearchCriteria();
		}

		List<DLNAResource> files = PMS.get().getRootFolder(renderer).getDLNAResources(
				objectID,
				browseDirectChildren,
				startingIndex,
				requestCount,
				renderer,
				searchCriteria
		);

		if (searchCriteria != null && files != null) {
			UMSUtils.filterResourcesByName(files, searchCriteria, false, false);
			if (xbox360 && !files.isEmpty()) {
				files = files.get(0).getChildren();
			}
		}

		int minus = 0;
		StringBuilder filesData = new StringBuilder();
		if (files != null) {
			for (DLNAResource uf : files) {
				if (uf instanceof PlaylistFolder playlistFolder) {
					File f = new File(uf.getFileName());
					if (uf.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (xbox360 && containerID != null && uf != null) {
					uf.setFakeParentId(containerID);
				}

				if (uf != null && uf.isCompatible(renderer) &&
						(uf.getEngine() == null || uf.getEngine().isEngineCompatible(renderer)) ||
						// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
						// all possible combination not only those supported by renderer because the renderer setting could be wrong.
						uf != null && files.get(0).isInsideTranscodeFolder()) {
					filesData.append(uf.getDidlString(renderer));
				} else {
					minus++;
				}
			}
		}

		StringBuilder response = new StringBuilder();

		if (requestMessage instanceof SearchRequest) {
			response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
		} else {
			response.append(HTTPXMLHelper.BROWSERESPONSE_HEADER);
		}

		response.append(CRLF);
		response.append(HTTPXMLHelper.RESULT_HEADER);
		response.append(HTTPXMLHelper.DIDL_HEADER);
		response.append(filesData);
		response.append(HTTPXMLHelper.DIDL_FOOTER);
		response.append(HTTPXMLHelper.RESULT_FOOTER);
		response.append(CRLF);

		int filessize = 0;
		if (files != null) {
			filessize = files.size();
		}

		response.append("<NumberReturned>").append(filessize - minus).append("</NumberReturned>");
		response.append(CRLF);
		DLNAResource parentFolder;

		if (files != null && filessize > 0) {
			parentFolder = files.get(0).getParent();
		} else {
			parentFolder = PMS.get().getRootFolder(renderer).getDLNAResource(objectID, renderer);
		}

		if (browseDirectChildren && renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, files are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular files, plus the #transcode folder, maybe the #imdb one, also files can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			int totalCount = startingIndex + requestCount + 1; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (filessize - minus <= 0) {
				totalCount = startingIndex;
			}

			response.append("<TotalMatches>").append(totalCount).append("</TotalMatches>");
		} else if (browseDirectChildren) {
			response.append("<TotalMatches>").append(((parentFolder != null) ? parentFolder.childrenNumber() : filessize) - minus).append("</TotalMatches>");
		} else {
			// From upnp spec: If BrowseMetadata is specified in the BrowseFlags then TotalMatches = 1
			response.append("<TotalMatches>1</TotalMatches>");
		}
		response.append(CRLF);

		/**
		 * From page 95, section 5.5.8.2 of
		 * http://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service.pdf
		 *
		 * UpdateID: The value returned in the UpdateID argument shall be the
		 * SystemUpdateID state variable value at the time the Browse() response
		 * was generated. If a control point finds that the current
		 * SystemUpdateID state variable value is not equal to the value
		 * returned in the UpdateID argument, then a change within the
		 * ContentDirectory service has occurred between the time the result was
		 * generated and the time that the control point is processing the
		 * result. The control point might therefore want to re-invoke the
		 * Browse() action to ensure that it has the latest property values.
		 * Note however that the change in the value of the SystemUpdateID state
		 * variable could have been caused by a change that occurred in a
		 * location in the ContentDirectory tree hierarchy that is not part of
		 * the returned result. In this case, the re-invocation of the Browse()
		 * action will return the exact same result. Note: This definition is
		 * not backwards compatible with previous versions of this
		 * specification. However, the previous definition did not indicate
		 * changes to properties of child containers. Therefore the control
		 * point would not have been aware that it had stale data.
		 */
		response.append("<UpdateID>");
		response.append(DLNAResource.getSystemUpdateId());
		response.append("</UpdateID>");
		response.append(CRLF);

		if (requestMessage instanceof SearchRequest) {
			response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
		} else {
			response.append(HTTPXMLHelper.BROWSERESPONSE_FOOTER);
		}

		return createResponse(response.toString());
	}

	/**
	 * Returns the string value that is enclosed by the left and right tag in a
	 * content string. Only the first match of each tag is used to determine
	 * positions. If either of the tags cannot be found, null is returned.
	 *
	 * @param content The entire {@link String} that needs to be searched for
	 * the left and right tag.
	 * @param leftTag The {@link String} determining the match for the left tag.
	 * @param rightTag The {@link String} determining the match for the right
	 * tag.
	 * @return The {@link String} that was enclosed by the left and right tag.
	 */
	private static String getEnclosingValue(String content, String leftTag, String rightTag) {
		String result = null;
		int leftTagPos = content.indexOf(leftTag);
		int leftTagStop = content.indexOf('>', leftTagPos + 1);
		int rightTagPos = content.indexOf(rightTag, leftTagStop + 1);

		if (leftTagPos > -1 && rightTagPos > leftTagPos) {
			result = content.substring(leftTagStop + 1, rightTagPos);
		}

		return result;
	}

	private String subscribeHandler(HttpExchange exchange, String uri, String soapaction) throws IOException {
		exchange.getResponseHeaders().set("SID", PMS.get().usn());

		/**
		 * Requirement [7.2.22.1]: UPnP devices must send events to all properly
		 * subscribed UPnP control points. The device must enforce a
		 * subscription TIMEOUT value of 5 minutes. The UPnP device behavior of
		 * enforcing this 5 minutes TIMEOUT value is implemented by specifying
		 * "TIMEOUT: second-300" as an HTTP header/value pair.
		 */
		exchange.getResponseHeaders().set("TIMEOUT", "Second-300");

		if (soapaction != null) {
			String cb = soapaction.replace("<", "").replace(">", "");

			try {
				URL soapActionUrl = new URL(cb);
				String addr = soapActionUrl.getHost();
				int port = soapActionUrl.getPort();
				try (
					Socket sock = new Socket(addr, port);
					OutputStream out = sock.getOutputStream()) {
					out.write(("NOTIFY /" + uri + " HTTP/1.1").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("SID: " + PMS.get().usn()).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("SEQ: " + 0).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("NT: upnp:event").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("NTS: upnp:propchange").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("HOST: " + addr + ":" + port).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.flush();
				}
			} catch (MalformedURLException ex) {
				LOGGER.debug("Cannot parse address and port from soap action \"" + soapaction + "\"", ex);
			}
		} else {
			LOGGER.debug("Expected soap action in request");
		}

		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.eventHeader("urn:schemas-upnp-org:service:ContentDirectory:1"));
		response.append(HTTPXMLHelper.eventProp("TransferIDs"));
		response.append(HTTPXMLHelper.eventProp("ContainerUpdateIDs"));
		response.append(HTTPXMLHelper.eventProp("SystemUpdateID", "" + DLNAResource.getSystemUpdateId()));
		response.append(HTTPXMLHelper.EVENT_FOOTER);
		return response.toString();
	}

	private static String notifyHandler(HttpExchange exchange) {
		exchange.getResponseHeaders().set("NT", "upnp:event");
		exchange.getResponseHeaders().set("NTS", "upnp:propchange");
		exchange.getResponseHeaders().set("SID", PMS.get().usn());
		exchange.getResponseHeaders().set("SEQ", "0");
		StringBuilder response = new StringBuilder();
		response.append("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">");
		response.append("<e:property>");
		response.append("<TransferIDs></TransferIDs>");
		response.append("</e:property>");
		response.append("<e:property>");
		response.append("<ContainerUpdateIDs></ContainerUpdateIDs>");
		response.append("</e:property>");
		response.append("<e:property>");
		response.append("<SystemUpdateID>").append(DLNAResource.getSystemUpdateId()).append("</SystemUpdateID>");
		response.append("</e:property>");
		response.append("</e:propertyset>");
		return response.toString();
	}

	//------------------------- END ContentDirectory -------------------------

	/**
	 * Returns a date somewhere in the far future.
	 *
	 * @return The {@link String} containing the date
	 */
	private static String getFutureDate() {
		SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
		return SDF.format(new Date(10000000000L + System.currentTimeMillis()));
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

	private static Renderer getRenderer(String uri, InetAddress ia, String userAgentString, Collection<Map.Entry<String, String>> headers) {
		Renderer renderer = null;
		// Attempt 1: If the reguested url contains the no-transcode tag, force
		// the default streaming-only conf.
		if (uri.contains(Renderer.NOTRANSCODE)) {
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
			renderer = ConnectedRenderers.getRendererConfigurationByHeaders(headers, ia);
		}
		// Still no media renderer recognized?
		if (renderer == null) {
			// Attempt 4: Not really an attempt; all other attempts to recognize
			// the renderer have failed. The only option left is to assume the
			// default renderer.
			renderer = ConnectedRenderers.resolve(ia, null);
			// If RendererConfiguration.resolve() didn't return the default renderer
			// it means we know via upnp that it's not really a renderer.
			if (renderer != null) {
				LOGGER.debug("Using default media renderer \"{}\"", renderer.getConfName());
				if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
					// We have found an unknown renderer
					List<String> identifiers = getIdentifiers(userAgentString, headers);
					renderer.setIdentifiers(identifiers);
					LOGGER.info(
							"Media renderer was not recognized. Possible identifying HTTP headers:\n{}",
							StringUtils.join(identifiers, "\n")
					);
					PMS.get().setRendererFound(renderer);
				}
			}
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Recognized media renderer \"{}\"", renderer.getRendererName());
		}
		return renderer;
	}

	private static List<String> getIdentifiers(String userAgentString, Collection<Map.Entry<String, String>> headers) {
		List<String> identifiers = new ArrayList<>();
		identifiers.add("User-Agent: " + userAgentString);
		for (Map.Entry<String, String> header : headers) {
			boolean isKnown = false;

			// Try to match known headers.
			String headerName = header.getKey().toLowerCase();
			for (String knownHeaderString : KNOWN_HEADERS) {
				if (headerName.startsWith(knownHeaderString)) {
					isKnown = true;
					break;
				}
			}
			if (!isKnown) {
				// Truly unknown header, therefore interesting.
				identifiers.add(header.getKey() + ": " + header.getValue());
			}
		}
		return identifiers;
	}

	/**
	 * Applies the IP filter to the specified internet address. Returns true if
	 * the address is not allowed and therefore should be filtered out, false
	 * otherwise.
	 *
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
	private static boolean filterIp(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
	}

	private static void logMessageSent(HttpExchange exchange, String response, InputStream iStream, Renderer renderer) {
		StringBuilder header = new StringBuilder();
		for (Map.Entry<String, List<String>> headers : exchange.getResponseHeaders().entrySet()) {
			String name = headers.getKey();
			if (StringUtils.isNotBlank(name)) {
				for (String value : headers.getValue()) {
					header.append("  ").append(name).append(": ").append(value).append("\n");
				}
			}
		}
		if (header.length() > 0) {
			header.insert(0, "\nHEADER:\n");
		}

		String responseCode = exchange.getProtocol() + " " + exchange.getResponseCode();
		String rendererName = getRendererName(exchange, renderer);

		if (HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			LOGGER.trace(
				"HEAD only response sent to {}:\n{}\n{}\n{}{}",
				rendererName,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				header,
				HTTPSERVER_RESPONSE_END
			);
		} else {
			String formattedResponse = null;
			if (StringUtils.isNotBlank(response)) {
				try {
					formattedResponse = StringUtil.prettifyXML(response, StandardCharsets.UTF_8, 4);
				} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
					formattedResponse = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
					formattedResponse += "    " + response.replace("\n", "\n    ");
				}
			}
			if (StringUtils.isNotBlank(formattedResponse)) {
				LOGGER.trace(
					"Response sent to {}:\n{}\n{}\n{}\nCONTENT:\n{}{}",
					rendererName,
					HTTPSERVER_RESPONSE_BEGIN,
					responseCode,
					header,
					formattedResponse,
					HTTPSERVER_RESPONSE_END
				);
				Matcher matcher = DIDL_PATTERN.matcher(response);
				if (matcher.find()) {
					try {
						LOGGER.trace(
							"The unescaped <Result> sent to {} is:\n{}",
							rendererName,
							StringUtil.prettifyXML(StringEscapeUtils.unescapeXml(matcher.group(1)), StandardCharsets.UTF_8, 2)
						);
					} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
						LOGGER.warn("Failed to prettify DIDL-Lite document: {}", e.getMessage());
						LOGGER.trace("", e);
					}
				}
			} else if (iStream != null && !"0".equals(exchange.getResponseHeaders().getFirst("Content-Length"))) {
				LOGGER.trace(
					"Transfer response sent to {}:\n{}\n{} ({})\n{}{}",
					rendererName,
					HTTPSERVER_RESPONSE_BEGIN,
					responseCode,
					getResponseIsChunked(exchange) ? "chunked" : "non-chunked",
					header,
					HTTPSERVER_RESPONSE_END
				);
			} else {
				LOGGER.trace(
					"Empty response sent to {}:\n{}\n{}\n{}{}",
					rendererName,
					HTTPSERVER_RESPONSE_BEGIN,
					responseCode,
					header,
					HTTPSERVER_RESPONSE_END
				);
			}
		}
	}

	private static void logMessageReceived(HttpExchange exchange, String content, Renderer renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;
		header.append(exchange.getRequestMethod());
		header.append(" ").append(exchange.getRequestURI());
		if (header.length() > 0) {
			header.append(" ");
		}
		header.append(exchange.getProtocol());
		header.append("\n\n");
		header.append("HEADER:\n");
		for (Map.Entry<String, List<String>> headers : exchange.getRequestHeaders().entrySet()) {
			String name = headers.getKey();
			if (StringUtils.isNotBlank(name)) {
				for (String value : headers.getValue()) {
					header.append("  ").append(name).append(": ").append(value).append("\n");
					if ("SOAPACTION".equalsIgnoreCase(name)) {
						soapAction = value.toUpperCase(Locale.ROOT);
					}
				}
			}
		}
		String formattedContent = null;
		if (StringUtils.isNotBlank(content)) {
			try {
				formattedContent = StringUtil.prettifyXML(content, StandardCharsets.UTF_8, 2);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage() + "\n";
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

		String rendererName = getRendererName(exchange, renderer);
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

	private static boolean getResponseIsChunked(HttpExchange exchange) {
		return exchange.getResponseHeaders().containsKey("Transfer-Encoding") &&
				exchange.getResponseHeaders().getFirst("Transfer-Encoding").equalsIgnoreCase("chunked");
	}

	private static String getRendererName(HttpExchange exchange, Renderer renderer) {
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
		if (exchange != null && exchange.getRemoteAddress() instanceof InetSocketAddress) {
			rendererName +=
					" (" + exchange.getRemoteAddress().getAddress().getHostAddress() +
					":" + exchange.getRemoteAddress().getPort() + ")";
		}
		return rendererName;
	}
}
