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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DlnaHelper;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.ImageEngine;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.MediaInfo;
import net.pms.media.MediaType;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.MediaServerRequest;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.service.Services;
import net.pms.service.StartStopListenerDelegate;
import net.pms.service.sleep.SleepManager;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.util.ByteRange;
import net.pms.util.FullyPlayed;
import net.pms.util.Range;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.TimeRange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the core stream server.
 *
 * It serve media stream, thumbnails and subtitles for media items.
 */
public class MediaServerHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerHandler.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static final String GET = "GET";
	private static final String HEAD = "HEAD";
	private static final int BUFFER_SIZE = 8 * 1024;

	private static final String HTTPSERVER_REQUEST_BEGIN = "================================== MEDIASERVER REQUEST BEGIN =====================================";
	private static final String HTTPSERVER_REQUEST_END = "================================== MEDIASERVER REQUEST END =======================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "================================== MEDIASERVER RESPONSE BEGIN ====================================";
	private static final String HTTPSERVER_RESPONSE_END = "================================== MEDIASERVER RESPONSE END ======================================";

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		Renderer renderer = null;
		try {
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			// Filter if required
			if (!NetworkDeviceFilter.isAllowed(ia)) {
				exchange.close();
				return;
			}

			//here, renderer should has been registred.
			String uri = exchange.getRequestURI().getPath();
			MediaServerRequest mediaServerRequest = new MediaServerRequest(uri);
			if (mediaServerRequest.isBadRequest()) {
				//Bad Request
				sendErrorResponse(exchange, renderer, 400);
				return;
			}

			//find renderer by uuid
			renderer = ConnectedRenderers.getUuidRenderer(mediaServerRequest.getUuid());

			if (renderer == null) {
				//find renderer by uuid and ip for non registred upnp devices
				renderer = ConnectedRenderers.getRendererBySocketAddress(ia);
				if (renderer != null && !mediaServerRequest.getUuid().equals(renderer.getId())) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Recognized media renderer \"{}\" is not matching UUID \"{}\"", renderer.getRendererName(), mediaServerRequest.getUuid());
					}
					/**
					 * here, that mean the originated renderer advised is no more available.
					 * It may be a caster/proxy control point, so let change to the real renderer.
					 * fixme : non-renderer should advise a special uuid.
					 * renderer = null;
					 */
				}
			}

			if (renderer == null) {
				// If uuid not known, it mean the renderer is not registred
				//Forbidden
				sendErrorResponse(exchange, renderer, 403);
				return;
			}

			if (!renderer.isAllowed()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
				}
				//Unauthorized
				sendErrorResponse(exchange, renderer, 401);
				return;
			}

			if (exchange.getRequestHeaders().containsKey("X-PANASONIC-DMP-Profile")) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(exchange.getRequestHeaders().getFirst("X-PANASONIC-DMP-Profile"), renderer);
			}

			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(exchange, renderer);
			}

			String method = exchange.getRequestMethod().toUpperCase();

			if (GET.equals(method) || HEAD.equals(method)) {
				// Get resource
				StoreResource resource = renderer.getMediaStore().getResource(mediaServerRequest.getResourceId());
				if (resource == null) {
					//resource not founded
					sendErrorResponse(exchange, renderer, 404);
					return;
				}
				switch (mediaServerRequest.getRequestType()) {
					case MEDIA -> {
						sendMediaResponse(exchange, renderer, resource, mediaServerRequest.getOptionalPath());
					}
					case THUMBNAIL -> {
						sendThumbnailResponse(exchange, renderer, resource, mediaServerRequest.getOptionalPath());
					}
					case SUBTITLES -> {
						sendSubtitlesResponse(exchange, renderer, resource);
					}
					default -> {
						//Bad Request
						sendErrorResponse(exchange, renderer, 400);
					}
				}
			} else {
				//Method Not Allowed
				sendErrorResponse(exchange, renderer, 405);
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

	private static void sendErrorResponse(final HttpExchange exchange, final Renderer renderer, int code) throws IOException {
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
		exchange.sendResponseHeaders(code, 0);
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, null, null, renderer);
		}
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, String message, String contentType) throws IOException {
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
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
			// Not a HEAD mediaServerRequest, so set the contents of the response.
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
		try (exchange) {
			exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
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
				} else if (cLoverride > -1 && cLoverride != StoreResource.TRANS_SIZE) {
					// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
					// as the PS3 will display a network error and mediaServerRequest the last seconds of the
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
		}
	}

	private static void sendMediaResponse(final HttpExchange exchange, final Renderer renderer, StoreResource resource, String filename) throws IOException {
		// Request to retrieve a file
		if (resource instanceof StoreItem item) {
			TimeRange timeseekrange = getTimeSeekRange(exchange.getRequestHeaders().getFirst("timeseekrange.dlna.org"));
			ByteRange range = getRange(exchange.getRequestHeaders().getFirst("Range"));
			int status = (range.getStart() != 0 || range.getEnd() != 0) ? 206 : 200;
			StartStopListenerDelegate startStopListenerDelegate = null;
			InputStream inputStream = null;
			long cLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit

			if (exchange.getRequestHeaders().containsKey("transfermode.dlna.org")) {
				exchange.getResponseHeaders().set("TransferMode.DLNA.ORG", exchange.getRequestHeaders().getFirst("transfermode.dlna.org"));
			}
			String contentFeatures = null;
			if (exchange.getRequestHeaders().containsKey("getcontentfeatures.dlna.org")) {
				contentFeatures = exchange.getRequestHeaders().getFirst("getcontentfeatures.dlna.org");
			}
			String samsungMediaInfo = null;
			if (exchange.getRequestHeaders().containsKey("getmediainfo.sec")) {
				samsungMediaInfo = exchange.getRequestHeaders().getFirst("getmediainfo.sec");
			}

			// LibraryResource was found.
			if (filename.endsWith("/chapters.vtt")) {
				sendResponse(exchange, renderer, 200, HlsHelper.getChaptersWebVtt(item), HTTPResource.WEBVTT_TYPEMIME);
				return;
			} else if (filename.endsWith("/chapters.json")) {
				sendResponse(exchange, renderer, 200, HlsHelper.getChaptersHls(item), HTTPResource.JSON_TYPEMIME);
				return;
			} else if (filename.startsWith("hls/")) {
				//HLS
				if (filename.endsWith(".m3u8")) {
					//HLS rendition m3u8 file
					String rendition = filename.replace("hls/", "").replace(".m3u8", "");
					if (HlsHelper.getByKey(rendition) != null) {
						String baseUrl = MediaServerRequest.getMediaURL(renderer.getUUID()).toString();
						sendResponse(exchange, renderer, 200, HlsHelper.getHLSm3u8ForRendition(item, renderer, baseUrl, rendition), HTTPResource.HLS_TYPEMIME);
					} else {
						sendResponse(exchange, renderer, 404, null);
					}
				} else {
					//HLS stream mediaServerRequest
					inputStream = HlsHelper.getInputStream("/" + filename, item);
					if (inputStream != null) {
						if (filename.endsWith(".ts")) {
							exchange.getResponseHeaders().set("Content-Type", HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							try {
								startStopListenerDelegate = new StartStopListenerDelegate(exchange.getRemoteAddress().getAddress().getHostAddress());
								startStopListenerDelegate.start(item);
								LOGGER.trace("Sending inputstream for " + filename);
								sendResponse(exchange, renderer, 200, inputStream, StoreResource.TRANS_SIZE, true);
							} finally {
								if (startStopListenerDelegate != null) {
									startStopListenerDelegate.stop();
								}
							}
						} else if (filename.endsWith(".vtt")) {
							exchange.getResponseHeaders().set("Content-Type", HTTPResource.WEBVTT_TYPEMIME);
							LOGGER.trace("Sending inputstream for " + filename);
							sendResponse(exchange, renderer, 200, inputStream, StoreResource.TRANS_SIZE, true);
						}
					} else {
						LOGGER.error("No inputstream for " + filename);
						sendResponse(exchange, renderer, 404, null);
					}
				}
				return;
			} else if (filename.endsWith("_transcoded_to.m3u8")) {
				//HLS start m3u8 file
				if (contentFeatures != null) {
					//output.headers().set("transferMode.HlsHelper.org", "Streaming");
					//only time seek, transcoded
					exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG", "DLNA.ORG_OP=10;DLNA.ORG_CI=01;DLNA.ORG_FLAGS=01700000000000000000000000000000");

					if (item.getMediaInfo().getDurationInSeconds() > 0) {
						String durationStr = String.format(Locale.ENGLISH, "%.3f", item.getMediaInfo().getDurationInSeconds());
						exchange.getResponseHeaders().set("TimeSeekRange.dlna.org", "npt=0-" + durationStr + "/" + durationStr);
						exchange.getResponseHeaders().set("X-AvailableSeekRange", "npt=0-" + durationStr);
					}
				}
				if (samsungMediaInfo != null && item.getMediaInfo().getDurationInSeconds() > 0) {
					exchange.getResponseHeaders().set("MediaInfo.sec", "SEC_Duration=" + (long) (item.getMediaInfo().getDurationInSeconds() * 1000));
				}

				String baseUrl = MediaServerRequest.getMediaURL(renderer.getUUID()).toString();
				sendResponse(exchange, renderer, 200, HlsHelper.getHLSm3u8(item, renderer, baseUrl), HTTPResource.HLS_TYPEMIME);
				return;
			} else if (item.getMediaInfo() != null && item.getMediaInfo().getMediaType() == MediaType.IMAGE && item.isCodeValid(item)) {
				// This is a mediaServerRequest for an image
				SleepManager sleepManager = Services.sleepManager();
				if (sleepManager != null) {
					sleepManager.postponeSleep();
				}

				DLNAImageProfile imageProfile = ImagesUtil.parseImageRequest(filename, null);
				if (imageProfile == null) {
					// Parsing failed for some reason, we'll have to pick a profile
					if (item.getMediaInfo().getImageInfo() != null && item.getMediaInfo().getImageInfo().getFormat() != null) {
						imageProfile = switch (item.getMediaInfo().getImageInfo().getFormat()) {
							case GIF ->
								DLNAImageProfile.GIF_LRG;
							case PNG ->
								DLNAImageProfile.PNG_LRG;
							default ->
								DLNAImageProfile.JPEG_LRG;
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
					if (item.getEngine() instanceof ImageEngine) {
						ProcessWrapper transcodeProcess = item.getEngine().launchTranscode(item,
								item.getMediaInfo(),
								new OutputParams(CONFIGURATION)
						);
						imageInputStream = transcodeProcess != null ? transcodeProcess.getInputStream(0) : null;
					} else {
						imageInputStream = item.getInputStream();
					}
					if (imageInputStream == null) {
						LOGGER.warn("Input stream returned for \"{}\" was null, no image will be sent to renderer", filename);
					} else {
						inputStream = DLNAImageInputStream.toImageInputStream(imageInputStream, imageProfile, false);
						if (contentFeatures != null) {
							if (CONFIGURATION.isUpnpJupnpDidl()) {
								exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG",
									net.pms.network.mediaserver.jupnp.support.contentdirectory.result.DlnaHelper.getDlnaImageContentFeatures(item, imageProfile, false)
								);
							} else {
								exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG",
									DlnaHelper.getDlnaImageContentFeatures(item, imageProfile, false)
								);
							}
						}
						if (inputStream != null && (range.getStart() > 0 || range.getEnd() > 0)) {
							if (range.getStart() > 0) {
								inputStream.skip(range.getStart());
							}
							inputStream = StoreItem.wrap(inputStream, range.getEnd(), range.getStart());
						}
					}
				} catch (IOException ie) {
					exchange.sendResponseHeaders(415, 0);
					LOGGER.debug("Could not send image \"{}\": {}", item.getName(), ie.getMessage() != null ? ie.getMessage() : ie.getClass().getSimpleName());
					LOGGER.trace("", ie);
					return;
				}
			} else if (item.isCodeValid(item)) {
				// This is a mediaServerRequest for a regular file.

				// If range has not been initialized yet and the LibraryResource has its
				// own start and end defined, initialize range with those values before
				// requesting the input stream.
				TimeRange splitRange = item.getSplitRange();

				if (timeseekrange.getStart() == null && splitRange.getStart() != null) {
					timeseekrange.setStart(splitRange.getStart());
				}

				if (timeseekrange.getEnd() == null && splitRange.getEnd() != null) {
					timeseekrange.setEnd(splitRange.getEnd());
				}

				long totalsize = item.length();
				boolean ignoreTranscodeByteRangeRequests = renderer.ignoreTranscodeByteRangeRequests();

				// Ignore ByteRangeRequests while media is transcoded
				if (!ignoreTranscodeByteRangeRequests ||
						totalsize != StoreResource.TRANS_SIZE ||
						(ignoreTranscodeByteRangeRequests &&
						range.getStart() == 0 &&
						totalsize == StoreResource.TRANS_SIZE)) {
					inputStream = item.getInputStream(Range.create(range.getStart(), range.getEnd(), timeseekrange.getStart(), timeseekrange.getEnd()));
					if (item.isResume()) {
						// Update range to possibly adjusted resume time
						timeseekrange.setStart(item.getResume().getTimeOffset() / (double) 1000);
					}
				}

				/**
				 * LG TVs send us many "play" requests while browsing
				 * directories, in order for them to show dynamic thumbnails.
				 * That means we can skip certain things like searching for
				 * subtitles and fully played logic.
				 */
				String userAgentString = exchange.getRequestHeaders().getFirst("User-Agent");
				boolean isVideoThumbnailRequest = renderer.isLG() && userAgentString != null && userAgentString.contains("Lavf/");

				Format format = item.getFormat();
				if (!isVideoThumbnailRequest && format != null && format.isVideo()) {
					MediaType mediaType = item.getMediaInfo() == null ? null : item.getMediaInfo().getMediaType();
					if (mediaType == MediaType.VIDEO) {
						if (item.getMediaInfo() != null &&
								item.getMediaSubtitle() != null &&
								item.getMediaSubtitle().isExternal() &&
								!CONFIGURATION.isDisableSubtitles() &&
								renderer.isExternalSubtitlesFormatSupported(item.getMediaSubtitle(), item)) {
							String subtitleHttpHeader = renderer.getSubtitleHttpHeader();
							if (StringUtils.isNotBlank(subtitleHttpHeader) && (item.getEngine() == null || renderer.streamSubsForTranscodedVideo())) {
								// Device allows a custom subtitle HTTP header; construct it
								MediaSubtitle sub = item.getMediaSubtitle();
								String subtitleUrl = item.getSubsURL(sub);
								exchange.getResponseHeaders().set(subtitleHttpHeader, subtitleUrl);
							} else {
								LOGGER.trace(
										"Did not send subtitle headers because mediaRenderer.getSubtitleHttpHeader() returned {}",
										subtitleHttpHeader == null ? "null" : "\"" + subtitleHttpHeader + "\""
								);
							}
						} else {
							ArrayList<String> reasons = new ArrayList<>();
							if (item.getMediaInfo() == null) {
								reasons.add("item.getMedia() is null");
							}
							if (CONFIGURATION.isDisableSubtitles()) {
								reasons.add("configuration.isDisabledSubtitles() is true");
							}
							if (item.getMediaSubtitle() == null) {
								reasons.add("item.getMediaSubtitle() is null");
							} else if (!item.getMediaSubtitle().isExternal()) {
								reasons.add("the subtitles are internal/embedded");
							} else if (!renderer.isExternalSubtitlesFormatSupported(item.getMediaSubtitle(), item)) {
								reasons.add("the external subtitles format isn't supported by the renderer");
							}
							LOGGER.trace("Did not send subtitle headers because {}", StringUtil.createReadableCombinedString(reasons));
						}
					}
				}

				String name = item.getDisplayName();
				if (item.isNoName()) {
					name = item.getName() + " " + item.getDisplayName();
				}

				if (inputStream == null) {
					if (!ignoreTranscodeByteRangeRequests) {
						// No inputStream indicates that transcoding / remuxing probably crashed.
						LOGGER.error("There is no inputstream to return for " + name);
					}
				} else {
					if (!isVideoThumbnailRequest) {
						startStopListenerDelegate = new StartStopListenerDelegate(exchange.getRemoteAddress().getAddress().getHostAddress());
						startStopListenerDelegate.start(item);
					}

					// Try to determine the content type of the file
					String rendererMimeType = renderer.getMimeType(item);

					if (rendererMimeType != null && !"".equals(rendererMimeType)) {
						exchange.getResponseHeaders().set("Content-Type", rendererMimeType);
					}

					// Response generation:
					// We use -1 for arithmetic convenience but don't send it as a value.
					// If Content-Length < 0 we omit it, for Content-Range we use '*' to signify unspecified.
					boolean chunked = renderer.isChunkedTransfer();

					// Determine the total size. Note: when transcoding the length is
					// not known in advance, so MediaInfo.TRANS_SIZE will be returned instead.
					if (chunked && totalsize == StoreResource.TRANS_SIZE) {
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
						// mode if the mediaServerRequest is open-ended and totalsize is unknown we omit it.
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
						if (CONFIGURATION.isUpnpJupnpDidl()) {
							exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG", net.pms.network.mediaserver.jupnp.support.contentdirectory.result.DlnaHelper.getDlnaContentFeatures(item));
						} else {
							exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG", DlnaHelper.getDlnaContentFeatures(item));
						}
					}

					if (samsungMediaInfo != null && item.getMediaInfo().getDurationInSeconds() > 0) {
						exchange.getResponseHeaders().set("MediaInfo.sec", "SEC_Duration=" + (long) (item.getMediaInfo().getDurationInSeconds() * 1000));
					}

					exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
					exchange.getResponseHeaders().set("Connection", "keep-alive");
				}
			}

			if (timeseekrange.isStartOffsetAvailable()) {
				// Add timeseek information headers.
				String timeseekValue = StringUtil.formatDLNADuration(timeseekrange.getStartOrZero());
				String timetotalValue = item.getMediaInfo().getDurationString();
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
		} else {
			sendErrorResponse(exchange, renderer, 400);
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

	private static void sendThumbnailResponse(final HttpExchange exchange, final Renderer renderer, StoreResource resource, String filename) throws IOException {
		// Request to retrieve a thumbnail
		ByteRange range = getRange(exchange.getRequestHeaders().getFirst("Range"));
		int status = (range.getStart() != 0 || range.getEnd() != 0) ? 206 : 200;
		InputStream inputStream;

		if (exchange.getRequestHeaders().containsKey("transfermode.dlna.org")) {
			exchange.getResponseHeaders().set("TransferMode.DLNA.ORG", exchange.getRequestHeaders().getFirst("transfermode.dlna.org"));
		}
		String contentFeatures = null;
		if (exchange.getRequestHeaders().containsKey("getcontentfeatures.dlna.org")) {
			contentFeatures = exchange.getRequestHeaders().getFirst("getcontentfeatures.dlna.org");
		}

		// This is a mediaServerRequest for a thumbnail file.
		DLNAImageProfile imageProfile = ImagesUtil.parseImageRequest(filename, DLNAImageProfile.JPEG_TN);
		exchange.getResponseHeaders().set("Content-Type", imageProfile.getMimeType().toString());
		exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
		exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
		exchange.getResponseHeaders().set("Connection", "keep-alive");

		DLNAThumbnailInputStream thumbInputStream;
		if (!CONFIGURATION.isShowCodeThumbs() && !resource.isCodeValid(resource)) {
			thumbInputStream = resource.getGenericThumbnailInputStream(null);
		} else {
			resource.checkThumbnail();
			thumbInputStream = resource.fetchThumbnailInputStream();
		}

		BufferedImageFilterChain filterChain = null;
		if (renderer.isThumbnails() && resource.isFullyPlayedMark()) {
			filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
		}
		filterChain = resource.addFlagFilters(filterChain);
		inputStream = thumbInputStream.transcode(
				imageProfile,
				renderer.isThumbnailPadding(),
				filterChain
		);
		if (contentFeatures != null) {
			if (CONFIGURATION.isUpnpJupnpDidl()) {
				exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG",
					net.pms.network.mediaserver.jupnp.support.contentdirectory.result.DlnaHelper.getDlnaImageContentFeatures(resource, imageProfile, true)
				);
			} else {
				exchange.getResponseHeaders().set("ContentFeatures.DLNA.ORG",
					DlnaHelper.getDlnaImageContentFeatures(resource, imageProfile, true)
				);
			}
		}
		if (inputStream != null && (range.getStart() > 0 || range.getEnd() > 0)) {
			if (range.getStart() > 0) {
				inputStream.skip(range.getStart());
			}
			inputStream = StoreItem.wrap(inputStream, range.getEnd(), range.getStart());
		}

		sendResponse(exchange, renderer, status, inputStream, -2, true);
	}

	private static void sendSubtitlesResponse(final HttpExchange exchange, final Renderer renderer, StoreResource resource) throws IOException {
		// Request to retrieve a subtitles
		TimeRange timeseekrange = getTimeSeekRange(exchange.getRequestHeaders().getFirst("timeseekrange.dlna.org"));
		ByteRange range = getRange(exchange.getRequestHeaders().getFirst("Range"));
		int status = (range.getStart() != 0 || range.getEnd() != 0) ? 206 : 200;

		InputStream inputStream = null;

		if (exchange.getRequestHeaders().containsKey("transfermode.dlna.org")) {
			exchange.getResponseHeaders().set("TransferMode.DLNA.ORG", exchange.getRequestHeaders().getFirst("transfermode.dlna.org"));
		}

		// Only valid if resource is item
		if (resource instanceof StoreItem item &&
				item.isCodeValid(item) &&
				item.getMediaInfo() != null) {
			// This is a mediaServerRequest for a subtitles file
			exchange.getResponseHeaders().set("Content-Type", "text/plain");
			exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
			MediaSubtitle sub = item.getMediaSubtitle();
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
			if (timeseekrange.isStartOffsetAvailable()) {
				// Add timeseek information headers.
				String timeseekValue = StringUtil.formatDLNADuration(timeseekrange.getStartOrZero());
				String timetotalValue = item.getMediaInfo().getDurationString();
				String timeEndValue = timeseekrange.isEndLimitAvailable() ? StringUtil.formatDLNADuration(timeseekrange.getEnd()) : timetotalValue;
				exchange.getResponseHeaders().set("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
				exchange.getResponseHeaders().set("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
			}
			sendResponse(exchange, renderer, status, inputStream, -2, true);
		} else {
			sendErrorResponse(exchange, renderer, 400);
		}
	}

	/**
	 * Returns a date somewhere in the far future.
	 *
	 * @return The {@link String} containing the date
	 */
	private static String getFutureDate() {
		SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
		return SDF.format(new Date(10000000000L + System.currentTimeMillis()));
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
				formattedResponse += "    " + response.replace("\n", "\n    ");
			}
			if (StringUtils.isNotBlank(formattedResponse)) {
				LOGGER.trace(
						"Response sent to {}:\n{}\n{}\n{}\nCONTENT:\n{}\n{}",
						rendererName,
						HTTPSERVER_RESPONSE_BEGIN,
						responseCode,
						header,
						formattedResponse,
						HTTPSERVER_RESPONSE_END
				);
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

	private static void logMessageReceived(HttpExchange exchange, Renderer renderer) {
		StringBuilder header = new StringBuilder();
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
				}
			}
		}

		String rendererName = getRendererName(exchange, renderer);
		LOGGER.trace(
				"Received a request from {}:\n{}\n{}{}\nRenderer UUID={}",
				rendererName,
				HTTPSERVER_REQUEST_BEGIN,
				header,
				HTTPSERVER_REQUEST_END,
				renderer != null ? renderer.getUUID() : "null"
		);
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
