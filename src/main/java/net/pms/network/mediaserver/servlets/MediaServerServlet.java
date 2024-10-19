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
package net.pms.network.mediaserver.servlets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
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
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.MediaServerRequest;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.DlnaHelper;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.service.Services;
import net.pms.service.sleep.SleepManager;
import net.pms.store.MediaStoreIds;
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
@WebServlet(name = "MEDIA HTTP SERVER", urlPatterns = {"/ums"}, displayName = "Media Server Servlet")
public class MediaServerServlet extends MediaServerHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerServlet.class);
	private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static final String GET = "GET";
	private static final String HEAD = "HEAD";
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final String HTTP_HEADER_RANGE_PREFIX = "bytes=";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doGetHead(req, resp);
	}

	protected void doGetHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Renderer renderer = null;
		try {
			String uri = req.getRequestURI();
			MediaServerRequest mediaServerRequest = new MediaServerRequest(uri);
			if (mediaServerRequest.isBadRequest()) {
				//Bad Request
				if (LOGGER.isTraceEnabled()) {
					logHttpServletRequest(req, "");
				}
				respondBadRequest(req, resp);
				return;
			}

			//find renderer by uuid
			renderer = ConnectedRenderers.getUuidRenderer(mediaServerRequest.getUuid());

			if (renderer == null) {
				//find renderer by uuid and ip for non registred upnp devices
				renderer = ConnectedRenderers.getRendererBySocketAddress(getInetAddress(req));
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
				if (LOGGER.isTraceEnabled()) {
					logHttpServletRequest(req, "");
				}
				//Forbidden
				respondForbidden(req, resp);
				return;
			}

			if (!renderer.isAllowed()) {
				if (LOGGER.isTraceEnabled()) {
					logHttpServletRequest(req, "", getRendererName(req, renderer));
					LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
				}
				//Unauthorized
				respondUnauthorized(req, resp);
				return;
			}

			if (req.getHeader("X-PANASONIC-DMP-Profile") != null) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(req.getHeader("X-PANASONIC-DMP-Profile"), renderer);
			}

			if (LOGGER.isTraceEnabled()) {
				logHttpServletRequest(req, null, getRendererName(req, renderer));
			}

			String method = req.getMethod().toUpperCase();

			if (GET.equals(method) || HEAD.equals(method)) {
				// Get resource
				StoreResource resource = renderer.getMediaStore().getResource(mediaServerRequest.getResourceId());
				if (resource == null) {
					// resource not found
					respondNotFound(req, resp);
					return;
				}
				switch (mediaServerRequest.getRequestType()) {
					case MEDIA -> {
						sendMediaResponse(req, resp, renderer, resource, mediaServerRequest.getOptionalPath());
					}
					case THUMBNAIL -> {
						sendThumbnailResponse(req, resp, renderer, resource, mediaServerRequest.getOptionalPath());
					}
					case SUBTITLES -> {
						sendSubtitlesResponse(req, resp, renderer, resource);
					}
					default -> {
						//Bad Request
						respondBadRequest(req, resp);
					}
				}
			} else {
				//Method Not Allowed
				respondNotAllowed(req, resp);
			}
		} catch (IOException e) {
			String message = e.getMessage();
			if (message != null) {
				if (message.equals("Connection reset by peer")) {
					LOGGER.trace("Http request from {}: {}", getRendererName(req, renderer), message);
				}
			} else {
				LOGGER.error("Http request error:", e);
			}
		}
	}

	private static void sendResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, int code, String message, String contentType) throws IOException {
		resp.setHeader("Server", MediaServer.getServerName());
		resp.setContentType(contentType);
		if (StringUtils.isEmpty(message)) {
			// No response data. Seems we are merely serving up headers.
			resp.setContentLength(0);
			resp.setStatus(204);
			if (LOGGER.isTraceEnabled()) {
				logHttpServletResponse(req, resp, null, false, getRendererName(req, renderer));
			}
			return;
		}
		// A response message was constructed; convert it to data ready to be sent.
		byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
		resp.setContentLength(bytes.length);
		resp.setStatus(code);
		// HEAD requests only require headers to be set, no need to set contents.
		if (!HEAD.equalsIgnoreCase(req.getMethod())) {
			// Not a HEAD request, so set the contents of the response.
			try (OutputStream os = resp.getOutputStream()) {
				os.write(bytes);
			} catch (Exception e) {
				LOGGER.debug("Error sending response: " + e);
			}
		}
		if (LOGGER.isTraceEnabled()) {
			logHttpServletResponse(req, resp, message, false, getRendererName(req, renderer));
		}
	}

	private static void sendResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, int code, InputStream inputStream) throws IOException {
		sendResponse(req, resp, renderer, code, inputStream, -2, true, null);
	}

	private static void sendResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, int code, InputStream inputStream, long cLoverride, boolean writeStream, StartStopListener startStopListener) throws IOException {
		// There is an input stream to send as a response.
		resp.setHeader("Server", MediaServer.getServerName());
		AsyncContext async = req.startAsync();
		if (inputStream == null) {
			// No input stream. Seems we are merely serving up headers.
			resp.setContentLength(0);
			resp.setStatus(204);
			if (LOGGER.isTraceEnabled()) {
				logHttpServletResponse(req, resp, null, false, getRendererName(req, renderer));
			}
			async.complete();
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
			resp.setContentLengthLong(contentLength);
		}
		// Send the response headers to the client.
		resp.setStatus(code);
		if (LOGGER.isTraceEnabled()) {
			logHttpServletResponse(req, resp, null, true, getRendererName(req, renderer));
		}
		// send only if no HEAD method is being used.
		if (writeStream && !HEAD.equalsIgnoreCase(req.getMethod())) {
			// Send the response body to the client in chunks.
			OutputStream os = new BufferedOutputStream(resp.getOutputStream(), BUFFER_SIZE);
			copyStreamAsync(inputStream, os, async, startStopListener);
		} else {
			if (HEAD.equalsIgnoreCase(req.getMethod()) && contentLength < 1) {
				resp.flushBuffer();
			}
			try {
				inputStream.close();
			} catch (IOException ioe) {
				LOGGER.error("Caught exception", ioe);
			}
			async.complete();
		}
	}

	private static void sendMediaResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, StoreResource resource, String filename) throws IOException {
		// Request to retrieve a file
		if (resource instanceof StoreItem item) {
			TimeRange timeseekrange = getTimeSeekRange(req.getHeader("timeseekrange.dlna.org"));
			ByteRange range = getRange(req.getHeader("Range"), item.length());
			int status = (range.getStart() != 0 || range.getEnd() != 0) ? 206 : 200;
			StartStopListener startStopListener = null;
			InputStream inputStream = null;
			long cLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit

			if (req.getHeader("transfermode.dlna.org") != null) {
				resp.setHeader("TransferMode.DLNA.ORG", req.getHeader("transfermode.dlna.org"));
			}
			String contentFeatures = req.getHeader("getcontentfeatures.dlna.org");
			String samsungMediaInfo = req.getHeader("getmediainfo.sec");

			// LibraryResource was found.
			if (filename.endsWith("/chapters.vtt")) {
				respond(req, resp, HlsHelper.getChaptersWebVtt(item), 200, HTTPResource.WEBVTT_TYPEMIME);
				return;
			} else if (filename.endsWith("/chapters.json")) {
				respond(req, resp, HlsHelper.getChaptersHls(item), 200, HTTPResource.WEBVTT_TYPEMIME);
				return;
			} else if (filename.startsWith("hls/")) {
				//HLS
				if (filename.endsWith(".m3u8")) {
					//HLS rendition m3u8 file
					String rendition = filename.replace("hls/", "").replace(".m3u8", "");
					if (HlsHelper.getByKey(rendition) != null) {
						String baseUrl = MediaServerRequest.getMediaURL(renderer.getUUID()).toString();
						respond(req, resp, HlsHelper.getHLSm3u8ForRendition(item, renderer, baseUrl, rendition), 200, HTTPResource.HLS_TYPEMIME);
					} else {
						respondNotFound(req, resp);
					}
				} else {
					//HLS stream request
					inputStream = HlsHelper.getInputStream("/" + filename, item);
					if (inputStream != null) {
						if (filename.endsWith(".ts")) {
							resp.setContentType(HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							startStopListener = new StartStopListener(req.getRemoteHost(), item);
							LOGGER.trace("Sending inputstream for " + filename);
							sendResponse(req, resp, renderer, 200, inputStream, StoreResource.TRANS_SIZE, true, startStopListener);
						} else if (filename.endsWith(".vtt")) {
							resp.setContentType(HTTPResource.WEBVTT_TYPEMIME);
							LOGGER.trace("Sending inputstream for " + filename);
							sendResponse(req, resp, renderer, 200, inputStream, StoreResource.TRANS_SIZE, true, null);
						}
					} else {
						LOGGER.error("No inputstream for " + filename);
						sendResponse(req, resp, renderer, 404, null);
					}
				}
				return;
			} else if (filename.endsWith("_transcoded_to.m3u8")) {
				//HLS start m3u8 file
				if (contentFeatures != null) {
					//only time seek, transcoded
					resp.setHeader("ContentFeatures.DLNA.ORG", "DLNA.ORG_OP=10;DLNA.ORG_CI=01;DLNA.ORG_FLAGS=01700000000000000000000000000000");

					if (item.getMediaInfo().getDurationInSeconds() > 0) {
						String durationStr = String.format(Locale.ENGLISH, "%.3f", item.getMediaInfo().getDurationInSeconds());
						resp.setHeader("TimeSeekRange.dlna.org", "npt=0-" + durationStr + "/" + durationStr);
						resp.setHeader("X-AvailableSeekRange", "npt=0-" + durationStr);
					}
				}
				if (samsungMediaInfo != null && item.getMediaInfo().getDurationInSeconds() > 0) {
					resp.setHeader("MediaInfo.sec", "SEC_Duration=" + (long) (item.getMediaInfo().getDurationInSeconds() * 1000));
				}

				String baseUrl = MediaServerRequest.getMediaURL(renderer.getUUID()).toString();
				sendResponse(req, resp, renderer, 200, HlsHelper.getHLSm3u8(item, renderer, baseUrl), HTTPResource.HLS_TYPEMIME);
				return;
			} else if (item.getMediaInfo() != null && item.getMediaInfo().getMediaType() == MediaType.IMAGE && item.isCodeValid(item)) {
				// This is a request for an image
				SleepManager sleepManager = Services.sleepManager();
				if (sleepManager != null) {
					sleepManager.postponeSleep();
				}
				String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
				String etag = req.getHeader("If-None-Match");
				if (etag != null && etag.equals(updateId)) {
					respondNotModified(req, resp);
					return;
				}
				if (updateId != null) {
					resp.setHeader("etag", updateId);
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
				resp.setContentType(imageProfile.getMimeType().toString());
				resp.setHeader("Accept-Ranges", "bytes");
				if (isHttp10(req)) {
					resp.setHeader("Expires", getFutureDate() + " GMT");
				} else {
					resp.setHeader("Cache-Control", "max-age=86400");
				}
				try {
					InputStream imageInputStream;
					if (item.isTranscoded() && item.getTranscodingSettings().getEngine() instanceof ImageEngine) {
						ProcessWrapper transcodeProcess = item.getTranscodingSettings().getEngine().launchTranscode(item,
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
							resp.setHeader("ContentFeatures.DLNA.ORG", DlnaHelper.getDlnaImageContentFeatures(item, imageProfile, false));
						}
						if (inputStream != null && (range.getStart() > 0 || range.getEnd() > 0)) {
							if (range.getStart() > 0) {
								inputStream.skip(range.getStart());
							}
							inputStream = StoreItem.wrap(inputStream, range.getEnd(), range.getStart());
						}
					}
				} catch (IOException ie) {
					respondUnsupportedMediaType(req, resp);
					LOGGER.debug("Could not send image \"{}\": {}", item.getName(), ie.getMessage() != null ? ie.getMessage() : ie.getClass().getSimpleName());
					LOGGER.trace("", ie);
					return;
				}
			} else if (item.isCodeValid(item)) {
				// This is a request for a regular file.

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
				String userAgentString = req.getHeader("User-Agent");
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
							if (StringUtils.isNotBlank(subtitleHttpHeader) && (!item.isTranscoded() || renderer.streamSubsForTranscodedVideo())) {
								// Device allows a custom subtitle HTTP header; construct it
								MediaSubtitle sub = item.getMediaSubtitle();
								String subtitleUrl = item.getSubsURL(sub);
								resp.setHeader(subtitleHttpHeader, subtitleUrl);
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
					if (!isVideoThumbnailRequest && GET.equals(req.getMethod().toUpperCase())) {
						startStopListener = new StartStopListener(req.getRemoteHost(), item);
					}

					// Try to determine the content type of the file
					String rendererMimeType = item.getMimeType();

					if (rendererMimeType != null && !"".equals(rendererMimeType)) {
						resp.setContentType(rendererMimeType);
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

						resp.setHeader("Content-Range", "bytes " + range.getStart() + "-" + (range.getEnd() > -1 ? range.getEnd() : "*") + "/" + (totalsize > -1 ? totalsize : "*"));

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
						resp.setHeader("ContentFeatures.DLNA.ORG", DlnaHelper.getDlnaContentFeatures(item));
					}

					if (samsungMediaInfo != null && item.getMediaInfo().getDurationInSeconds() > 0) {
						resp.setHeader("MediaInfo.sec", "SEC_Duration=" + (long) (item.getMediaInfo().getDurationInSeconds() * 1000));
					}

					resp.setHeader("Accept-Ranges", "bytes");
					if (GET.equals(req.getMethod().toUpperCase())) {
						resp.setHeader("Connection", "keep-alive");
					}
				}
			}

			if (timeseekrange.isStartOffsetAvailable()) {
				// Add timeseek information headers.
				String timeseekValue = StringUtil.formatDLNADuration(timeseekrange.getStartOrZero());
				String timetotalValue = item.getMediaInfo().getDurationString();
				String timeEndValue = timeseekrange.isEndLimitAvailable() ? StringUtil.formatDLNADuration(timeseekrange.getEnd()) : timetotalValue;
				resp.setHeader("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
				resp.setHeader("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
			}

			sendResponse(req, resp, renderer, status, inputStream, cLoverride, (range.getStart() != MediaInfo.ENDFILE_POS), startStopListener);
		} else {
			respondBadRequest(req, resp);
		}
	}

	private static ByteRange getRange(String rangeStr, long streamLength) {
		List<ByteRange> ranges = parseRanges(rangeStr, streamLength);
		if (ranges.isEmpty()) {
			return new ByteRange(0L, 0L);
		} else {
			return ranges.get(0);
		}
	}

	private static List<ByteRange> parseRanges(String rangesStr, long streamLength) {
		List<ByteRange> ranges = new ArrayList<>();
		if (rangesStr == null || StringUtils.isEmpty(rangesStr)) {
			return ranges;
		}
		long streamEnd = streamLength - 1;
		rangesStr = rangesStr.toLowerCase().trim();
		if (!rangesStr.startsWith(HTTP_HEADER_RANGE_PREFIX)) {
			LOGGER.warn("Range '{}' does not start with '{}'", rangesStr, HTTP_HEADER_RANGE_PREFIX);
			return ranges;
		}
		for (String rangeStr : rangesStr.split(",")) {
			try {
				rangeStr = rangeStr.trim();
				if (rangeStr.startsWith(HTTP_HEADER_RANGE_PREFIX)) {
					rangeStr = rangeStr.substring(HTTP_HEADER_RANGE_PREFIX.length());
				}
				long start = -1;
				long end = -1;
				int dash = rangeStr.indexOf('-');
				if (dash < 0 || rangeStr.indexOf("-", dash + 1) >= 0) {
					LOGGER.warn("Range header '{}' is not well formed on '{}'", rangesStr, rangeStr);
					break;
				}
				if (dash > 0) {
					start = Long.parseLong(rangeStr.substring(0, dash).trim());
				}
				if (dash < (rangeStr.length() - 1)) {
					end = Long.parseLong(rangeStr.substring(dash + 1).trim());
				}
				if (start == -1) {
					if (end == 0) {
						continue;
					}
					if (end == -1) {
						LOGGER.warn("Range header '{}' is not well formed on '{}'", rangesStr, rangeStr);
						break;
					}

					start = Math.max(0, streamEnd - end + 1);
					end = streamEnd;
				} else {
					if (start > streamEnd) {
						continue;
					}
					if (end == -1 || end > streamEnd) {
						end = streamEnd;
					}
				}
				if (end < start) {
					LOGGER.warn("Range header '{}' is not well formed on '{}'", rangesStr, rangeStr);
					break;
				}
				ranges.add(new ByteRange(start, end));
			} catch (NumberFormatException x) {
				LOGGER.warn("Range header '{}' is not well formed on '{}'", rangesStr, rangeStr);
			}
		}
		return ranges;
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

	private static void sendThumbnailResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, StoreResource resource, String filename) throws IOException {
		// Request to retrieve a thumbnail
		String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
		String etag = req.getHeader("If-None-Match");
		if (etag != null && etag.equals(updateId)) {
			respondNotModified(req, resp);
			return;
		}
		if (updateId != null) {
			resp.setHeader("etag", updateId);
		}

		InputStream inputStream;

		if (req.getHeader("transfermode.dlna.org") != null) {
			resp.setHeader("TransferMode.DLNA.ORG", req.getHeader("transfermode.dlna.org"));
		}
		String contentFeatures = req.getHeader("getcontentfeatures.dlna.org");

		// This is a request for a thumbnail file.
		DLNAImageProfile imageProfile = ImagesUtil.parseImageRequest(filename, DLNAImageProfile.JPEG_TN);
		resp.setContentType(imageProfile.getMimeType().toString());
		resp.setHeader("Accept-Ranges", "bytes");
		if (isHttp10(req)) {
			resp.setHeader("Expires", getFutureDate() + " GMT");
		} else {
			resp.setHeader("Cache-Control", "max-age=86400");
		}

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
			resp.setHeader("ContentFeatures.DLNA.ORG", DlnaHelper.getDlnaImageContentFeatures(resource, imageProfile, true));
		}
		int status = 200;
		if (inputStream != null) {
			ByteRange range = getRange(req.getHeader("Range"), inputStream.available());
			if (range.getStart() > 0 || range.getEnd() > 0) {
				if (range.getStart() > 0) {
					inputStream.skip(range.getStart());
				}
				inputStream = StoreItem.wrap(inputStream, range.getEnd(), range.getStart());
				status = 206;
			}
		}
		sendResponse(req, resp, renderer, status, inputStream);
	}

	private static void sendSubtitlesResponse(HttpServletRequest req, HttpServletResponse resp, final Renderer renderer, StoreResource resource) throws IOException {
		// Request to retrieve a subtitles
		TimeRange timeseekrange = getTimeSeekRange(req.getHeader("timeseekrange.dlna.org"));
		int status = 200;
		InputStream inputStream = null;

		if (req.getHeader("transfermode.dlna.org") != null) {
			resp.setHeader("TransferMode.DLNA.ORG", req.getHeader("transfermode.dlna.org"));
		}

		// Only valid if resource is item
		if (resource instanceof StoreItem item &&
				item.isCodeValid(item) &&
				item.getMediaInfo() != null) {
			// This is a request for a subtitles file
			resp.setContentType("text/plain");
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
			String etag = req.getHeader("If-None-Match");
			if (etag != null && etag.equals(updateId)) {
				respondNotModified(req, resp);
				return;
			}
			if (updateId != null) {
				resp.setHeader("etag", updateId);
			}
			if (isHttp10(req)) {
				resp.setHeader("Expires", getFutureDate() + " GMT");
			} else {
				resp.setHeader("Cache-Control", "max-age=86400");
			}
			MediaSubtitle sub = item.getMediaSubtitle();
			if (sub != null) {
				// XXX external file is null if the first subtitle track is embedded
				if (sub.isExternal()) {
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
							ByteRange range = getRange(req.getHeader("Range"), inputStream.available());
							if (range.getStart() != 0 || range.getEnd() != 0) {
								status = 206;
							}
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
				resp.setHeader("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
				resp.setHeader("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
			}
			sendResponse(req, resp, renderer, status, inputStream);
		} else {
			respondBadRequest(req, resp);
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

	private static String getRendererName(HttpServletRequest req, Renderer renderer) {
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
		if (req != null) {
			rendererName +=
					" (" + req.getRemoteHost() +
					":" + req.getRemotePort() + ")";
		}
		return rendererName;
	}

}
