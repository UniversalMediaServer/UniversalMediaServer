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
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.dlna.DLNAImageInputStream;
import net.pms.dlna.DLNAImageProfile;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.media.MediaType;
import net.pms.dlna.PlaylistFolder;
import net.pms.dlna.Range;
import net.pms.dlna.RealFile;
import net.pms.dlna.TimeRange;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.ImageEngine;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.handlers.ApiHandler;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.handlers.message.BrowseRequest;
import net.pms.network.mediaserver.handlers.message.BrowseSearchRequest;
import net.pms.network.mediaserver.handlers.message.SamsungBookmark;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import net.pms.service.Services;
import net.pms.service.StartStopListenerDelegate;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.UMSUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpMethod.HEAD;
import static org.jboss.netty.handler.codec.http.HttpMethod.POST;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class handles all forms of incoming HTTP requests by constructing a proper HTTP response.
 */
public class RequestV2 extends HTTPResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestV2.class);
	private static final String CRLF = "\r\n";
	private static final Pattern DIDL_PATTERN = Pattern.compile("<Result>(&lt;DIDL-Lite.*?)</Result>");
	private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final String HTTPSERVER_RESPONSE_BEGIN = "================================== HTTPSERVER RESPONSE BEGIN ====================================";
	private static final String HTTPSERVER_RESPONSE_END =   "================================== HTTPSERVER RESPONSE END ======================================";

	private final HttpMethod method;
	private final SearchRequestHandler searchRequestHandler = new SearchRequestHandler();

	private UmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * A {@link String} that contains the uri with which this {@link RequestV2} was
	 * created.
	 */
	private String uri;
	private String soapaction;
	private String content;
	private int startingIndex;
	private int requestCount;
	private boolean isVideoThumbnailRequest = false;

	/**
	 * When sending an input stream, the lowRange indicates which byte to start from.
	 */
	private long lowRange;
	private Renderer mediaRenderer;
	private String transferMode;
	private String contentFeatures;
	private final TimeRange range = new TimeRange();

	/**
	 * When sending an input stream, the highRange indicates which byte to stop at.
	 */
	private long highRange;

	public Renderer getMediaRenderer() {
		return mediaRenderer;
	}

	public void setMediaRenderer(Renderer renderer) {
		this.mediaRenderer = renderer;
		// Use device-specific pms conf
		configuration = PMS.getConfiguration(renderer);
	}

	/**
	 * When sending an input stream, the lowRange indicates which byte to start from.
	 * @return The byte to start from
	 */
	public long getLowRange() {
		return lowRange;
	}

	/**
	 * Set the byte from which to start when sending an input stream. This value will
	 * be used to send a CONTENT_RANGE header with the response.
	 * @param lowRange The byte to start from.
	 */
	public void setLowRange(long lowRange) {
		// Assume 100GB+ values are errors and ignore them.
		if (lowRange < 100000000000L) {
			this.lowRange = lowRange;
		}
	}

	public String getTransferMode() {
		return transferMode;
	}

	public void setTransferMode(String transferMode) {
		this.transferMode = transferMode;
	}

	public String getContentFeatures() {
		return contentFeatures;
	}

	public void setContentFeatures(String contentFeatures) {
		this.contentFeatures = contentFeatures;
	}

	public void setTimeRangeStart(Double timeseek) {
		this.range.setStart(timeseek);
	}

	public void setTimeRangeStartString(String str) {
		setTimeRangeStart(StringUtil.convertStringToTime(str));
	}

	public void setTimeRangeEnd(Double rangeEnd) {
		this.range.setEnd(rangeEnd);
	}

	public void setTimeRangeEndString(String str) {
		setTimeRangeEnd(StringUtil.convertStringToTime(str));
	}

	/**
	 * When sending an input stream, the highRange indicates which byte to stop at.
	 * @return The byte to stop at.
	 */
	public long getHighRange() {
		return highRange;
	}

	/**
	 * Set the byte at which to stop when sending an input stream. This value will
	 * be used to send a CONTENT_RANGE header with the response.
	 * @param highRange The byte to stop at.
	 */
	public void setHighRange(long highRange) {
		this.highRange = highRange;
	}

	/**
	 * This class will construct and transmit a proper HTTP response to a given HTTP request.Rewritten version of the {@link Request} class.
	 * @param method The {@link String} that defines the HTTP method to be used.
	 * @param uri
	 */
	public RequestV2(HttpMethod method, String uri) {
		this.method = method;
		this.uri = uri;
	}

	public String getSoapaction() {
		return soapaction;
	}

	public void setSoapaction(String soapaction) {
		this.soapaction = soapaction;
	}

	public String getTextContent() {
		return content;
	}

	public void setTextContent(String content) {
		this.content = content;
	}

	/**
	 * LG TVs send us many "play" requests while browsing directories, in order
	 * for them to show dynamic thumbnails. That means we can skip certain things
	 * like searching for subtitles and fully played logic.
	 */
	public boolean isVideoThumbnailRequest() {
		return isVideoThumbnailRequest;
	}

	public void setIsVideoThumbnailRequest(boolean value) {
		this.isVideoThumbnailRequest = value;
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
		HttpResponse output,
		MessageEvent event,
		final boolean close,
		final StartStopListenerDelegate startStopListenerDelegate
	) throws IOException {
		PMS.REALTIME_LOCK.lock();
		long cLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit
		StringBuilder response = new StringBuilder();
		DLNAResource dlna = null;
		InputStream inputStream = null;
		ChannelFuture future = null;
		try {
			// Samsung 2012 TVs have a problematic preceding slash that needs to be removed.
			if (uri.startsWith("/")) {
				LOGGER.trace("Stripping preceding slash from: " + uri);
				uri = uri.substring(1);
			}

			if (uri.startsWith("api/")) {
				ApiHandler api = new ApiHandler();
				response.append(api.handleApiRequest(method, content, output, uri.substring(4), event));
			} else if ((GET.equals(method) || HEAD.equals(method)) && uri.startsWith("get/")) {
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
				if (id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
					try {
						dlna = DbIdResourceLocator.locateResource(id.substring(0, id.indexOf('/')), mediaRenderer);
					} catch (Exception e) {
						LOGGER.error("", e);
					}
				} else {
					dlna = PMS.get().getRootFolder(mediaRenderer).getDLNAResource(id, mediaRenderer);
				}
				String fileName = id.substring(id.indexOf('/') + 1);

				if (transferMode != null) {
					output.headers().set("TransferMode.DLNA.ORG", transferMode);
				}

				if (dlna != null && dlna.isFolder() && !fileName.startsWith("thumbnail0000")) {
					// if we found a folder we MUST be asked for thumbnails
					// otherwise this is not allowed
					dlna = null;
				}

				if (dlna != null) {
					// DLNAresource was found.
					if (fileName.endsWith("/chapters.vtt")) {
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.WEBVTT_TYPEMIME);
						response.append(MediaChapter.getWebVtt(dlna));
					} else if (fileName.endsWith("/chapters.json")) {
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.JSON_TYPEMIME);
						response.append(MediaChapter.getHls(dlna));
					} else if (fileName.startsWith("hls/")) {
						//HLS
						if (fileName.endsWith(".m3u8")) {
							//HLS rendition m3u8 file
							String rendition = fileName.replace("hls/", "").replace(".m3u8", "");
							if (HlsHelper.getByKey(rendition) != null) {
								output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.HLS_TYPEMIME);
								response.append(HlsHelper.getHLSm3u8ForRendition(dlna, mediaRenderer, "/get/", rendition));
							}
						} else {
							//HLS stream request
							cLoverride = MediaInfo.TRANS_SIZE;
							inputStream = HlsHelper.getInputStream("/" + fileName, dlna, mediaRenderer);
							if (fileName.endsWith(".ts")) {
								output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							} else if (fileName.endsWith(".vtt")) {
								output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.WEBVTT_TYPEMIME);
							}
						}
					} else if (fileName.endsWith("_transcoded_to.m3u8")) {
						//HLS start m3u8 file
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, HTTPResource.HLS_TYPEMIME);
						response.append(HlsHelper.getHLSm3u8(dlna, mediaRenderer, "/get/"));
						if (contentFeatures != null) {
							//output.headers().set("transferMode.dlna.org", "Streaming");
							if (dlna.getMedia().getDurationInSeconds() > 0) {
								String durationStr = String.format(Locale.ENGLISH, "%.3f", dlna.getMedia().getDurationInSeconds());
								output.headers().set("TimeSeekRange.dlna.org", "npt=0-" + durationStr + "/" + durationStr);
								output.headers().set("X-AvailableSeekRange", "npt=0-" + durationStr);
								//only time seek, transcoded
								output.headers().set("ContentFeatures.DLNA.ORG", "DLNA.ORG_OP=10;DLNA.ORG_CI=1;DLNA.ORG_FLAGS=41700000000000000000000000000000");
							}
						}
					} else if (fileName.startsWith("thumbnail0000")) {
						// This is a request for a thumbnail file.
						DLNAImageProfile imageProfile = ImagesUtil.parseThumbRequest(fileName);
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
						output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
						output.headers().set(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
						output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

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
							mediaRenderer != null && mediaRenderer.isThumbnailPadding(),
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
						output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
						output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
					} else if (dlna.getMedia() != null && dlna.getMedia().getMediaType() == MediaType.IMAGE && dlna.isCodeValid(dlna)) {
						// This is a request for an image
						Services.sleepManager().postponeSleep();
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
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
						output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
						output.headers().set(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
						output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
						try {
							InputStream imageInputStream;
							if (dlna.getEngine() instanceof ImageEngine) {
								ProcessWrapper transcodeProcess = dlna.getEngine().launchTranscode(
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
									output.headers().set(
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
								output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
								output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
							}
						} catch (IOException ie) {
							output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "0");
							output.setStatus(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);

							// Send the response headers to the client.
							future = event.getChannel().write(output);

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
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
						output.headers().set(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
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
						TimeRange splitRange = dlna.getSplitRange();

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
							totalsize != MediaInfo.TRANS_SIZE ||
							(
								ignoreTranscodeByteRangeRequests &&
								lowRange == 0 &&
								totalsize == MediaInfo.TRANS_SIZE
							)
						) {
							inputStream = dlna.getInputStream(Range.create(lowRange, highRange, range.getStart(), range.getEnd()), mediaRenderer);
							if (dlna.isResume()) {
								// Update range to possibly adjusted resume time
								range.setStart(dlna.getResume().getTimeOffset() / (double) 1000);
							}
						}

						Format format = dlna.getFormat();
						if (!isVideoThumbnailRequest && format != null && format.isVideo()) {
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
									if (isNotBlank(subtitleHttpHeader)  && (dlna.getEngine() == null || mediaRenderer.streamSubsForTranscodedVideo())) {
										// Device allows a custom subtitle HTTP header; construct it
										MediaSubtitle sub = dlna.getMediaSubtitle();
										String subtitleUrl;
										String subExtension = sub.getType().getExtension();
										if (isNotBlank(subExtension)) {
											subExtension = "." + subExtension;
										}
										subtitleUrl = MediaServer.getURL() + "/get/" +
											id.substring(0, id.indexOf('/')) + "/subtitle0000" + subExtension;

											output.headers().set(subtitleHttpHeader, subtitleUrl);
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
							if (startStopListenerDelegate != null) {
								startStopListenerDelegate.start(dlna);
							}

							// Try to determine the content type of the file
							String rendererMimeType = getRendererMimeType(mediaRenderer, dlna);

							if (rendererMimeType != null && !"".equals(rendererMimeType)) {
								output.headers().set(HttpHeaders.Names.CONTENT_TYPE, rendererMimeType);
							}

							// Response generation:
							// We use -1 for arithmetic convenience but don't send it as a value.
							// If Content-Length < 0 we omit it, for Content-Range we use '*' to signify unspecified.
							boolean chunked = mediaRenderer.isChunkedTransfer();

							// Determine the total size. Note: when transcoding the length is
							// not known in advance, so MediaInfo.TRANS_SIZE will be returned instead.
							if (chunked && totalsize == MediaInfo.TRANS_SIZE) {
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

								output.headers().set(HttpHeaders.Names.CONTENT_RANGE, "bytes " + lowRange + "-" + (highRange > -1 ? highRange : "*") + "/" + (totalsize > -1 ? totalsize : "*"));

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
								output.headers().set("ContentFeatures.DLNA.ORG", dlna.getDlnaContentFeatures(mediaRenderer));
							}

							output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
							output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
						}
						if (origRendering != null) {
							// Restore original rendering details
							dlna.updateRendering(origRendering);
						}
					}
				}
			} else if ((GET.equals(method) || HEAD.equals(method)) && (uri.toLowerCase().endsWith(".png") || uri.toLowerCase().endsWith(".jpg") || uri.toLowerCase().endsWith(".jpeg"))) {
				inputStream = imageHandler(output);

			//------------------------- START ContentDirectory -------------------------
			} else if (GET.equals(method) && uri.endsWith("/ContentDirectory/desc")) {
				response.append(contentDirectorySpec(output));
			} else if (POST.equals(method) && uri.endsWith("/ContentDirectory/action")) {
				output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
				if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSystemUpdateID")) {
					response.append(getSystemUpdateIdHandler());
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_SetBookmark")) {
					response.append(samsungSetBookmarkHandler());
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_GetFeatureList")) { // Added for Samsung 2012 TVs
					response.append(samsungGetFeaturesListHandler());
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSortCapabilities")) {
					response.append(getSortCapabilitiesHandler());
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSearchCapabilities")) {
					response.append(getSearchCapabilitiesHandler(mediaRenderer));
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Browse")) {
					response.append(browseHandler());
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Search")) {
					response.append(searchHandler());
				} else {
					LOGGER.debug("Unsupported action received: " + content);
				}
			} else if (method.getName().equals("SUBSCRIBE")) {
				response.append(subscribeHandler(output));
			} else if (method.getName().equals("NOTIFY")) {
				response.append(notifyHandler(output));
			//------------------------- END ContentDirectory -------------------------
			}

			output.headers().set(HttpHeaders.Names.SERVER, PMS.get().getServerName());

			if (response.length() > 0) {
				// A response message was constructed; convert it to data ready to be sent.
				byte[] responseData = response.toString().getBytes(StandardCharsets.UTF_8);
				output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "" + responseData.length);

				// HEAD requests only require headers to be set, no need to set contents.
				if (!HEAD.equals(method)) {
					// Not a HEAD request, so set the contents of the response.
					ChannelBuffer buf = ChannelBuffers.copiedBuffer(responseData);
					output.setContent(buf);
				}

				// Send the response to the client.
				future = event.getChannel().write(output);

				if (close) {
					// Close the channel after the response is sent.
					future.addListener(ChannelFutureListener.CLOSE);
				}
			} else if (inputStream != null) {
				// There is an input stream to send as a response.

				if (cLoverride > -2) {
					// Content-Length override has been set, send or omit as appropriate
					if (cLoverride > -1 && cLoverride != MediaInfo.TRANS_SIZE) {
						// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
						// as the PS3 will display a network error and request the last seconds of the
						// transcoded video. Better to send no Content-Length at all.
						output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "" + cLoverride);
					}
				} else {
					int contentLength = inputStream.available();
					LOGGER.trace("Available Content-Length: {}", contentLength);
					output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "" + contentLength);
				}

				if (range.isStartOffsetAvailable() && dlna != null) {
					// Add timeseek information headers.
					String timeseekValue = StringUtil.formatDLNADuration(range.getStartOrZero());
					String timetotalValue = dlna.getMedia().getDurationString();
					String timeEndValue = range.isEndLimitAvailable() ? StringUtil.formatDLNADuration(range.getEnd()) : timetotalValue;
					output.headers().set("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
					output.headers().set("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
				}

				// Send the response headers to the client.
				future = event.getChannel().write(output);

				if (lowRange != MediaInfo.ENDFILE_POS && !HEAD.equals(method)) {
					// Send the response body to the client in chunks.
					ChannelFuture chunkWriteFuture = event.getChannel().write(new ChunkedStream(inputStream, BUFFER_SIZE));

					// Add a listener to clean up after sending the entire response body.
					final InputStream finalInputStream = inputStream;
					chunkWriteFuture.addListener((ChannelFuture future1) -> {
						try {
							finalInputStream.close();
						} catch (IOException e) {
							LOGGER.error("Caught exception", e);
						}
						// Always close the channel after the response is sent because of
						// a freeze at the end of video when the channel is not closed.
						future1.getChannel().close();
						if (startStopListenerDelegate != null) {
							startStopListenerDelegate.stop();
						}
					});
				} else {
					// HEAD method is being used, so simply clean up after the response was sent.
					try {
						inputStream.close();
					} catch (IOException ioe) {
						LOGGER.error("Caught exception", ioe);
					}

					if (close) {
						// Close the channel after the response is sent
						future.addListener(ChannelFutureListener.CLOSE);
					}

					if (startStopListenerDelegate != null) {
						startStopListenerDelegate.stop();
					}
				}
			} else {
				// No response data and no input stream. Seems we are merely serving up headers.
				output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "0");
				output.setStatus(HttpResponseStatus.NO_CONTENT);

				// Send the response headers to the client.
				future = event.getChannel().write(output);

				if (close) {
					// Close the channel after the response is sent.
					future.addListener(ChannelFutureListener.CLOSE);
				}
			}
		} catch (Exception e) {
			LOGGER.error("error while generating answer", e);
		} finally {
			PMS.REALTIME_LOCK.unlock();
		}
		if (LOGGER.isTraceEnabled()) {
			// Log trace information
			logRequest(output, response, inputStream);
		}
		return future;
	}

	private InputStream imageHandler(HttpResponse output) {
		if (uri.toLowerCase().endsWith(".png")) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/png");
		} else {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
		}

		output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
		output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		output.headers().set(HttpHeaders.Names.EXPIRES, getFutureDate() + " GMT");
		return getResourceInputStream(uri);
	}

	private void logRequest(HttpResponse output, StringBuilder response, InputStream iStream) {
		StringBuilder header = new StringBuilder();
		for (Entry<String, String> entry : output.headers().entries()) {
			if (isNotBlank(entry.getKey())) {
				header.append("  ").append(entry.getKey())
				.append(": ").append(entry.getValue()).append("\n");
			}
		}
		if (header.length() > 0) {
			header.insert(0, "\nHEADER:\n");
		}

		String responseCode = output.getProtocolVersion() + " " + output.getStatus();
		String rendererName = getRendererName();
		String contentType = output.headers().get(HttpHeaders.Names.CONTENT_TYPE);
		if (HEAD.equals(method)) {
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
			if (isNotBlank(response)) {
				if (contentType != null && contentType.startsWith("text/xml")) {
					try {
						formattedResponse = StringUtil.prettifyXML(response.toString(), StandardCharsets.UTF_8, 4);
					} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
						formattedResponse = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
						formattedResponse += "    " + response.toString().replace("\n", "\n    ");
					}
				} else {
					formattedResponse = response.toString();
				}
			}
			if (isNotBlank(formattedResponse)) {
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
			} else if (iStream != null && !"0".equals(output.headers().get(HttpHeaders.Names.CONTENT_LENGTH))) {
				LOGGER.trace(
					"Transfer response sent to {}:\n{}\n{} ({})\n{}{}",
					rendererName,
					HTTPSERVER_RESPONSE_BEGIN,
					responseCode,
					output.isChunked() ? "chunked" : "non-chunked",
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

	private String getRendererName() {
		String rendererName;
		if (mediaRenderer != null) {
			if (isNotBlank(mediaRenderer.getRendererName())) {
				if (
					isBlank(mediaRenderer.getConfName()) ||
					mediaRenderer.getRendererName().equals(mediaRenderer.getConfName())
				) {
					rendererName = mediaRenderer.getRendererName();
				} else {
					rendererName = mediaRenderer.getRendererName() + " [" + mediaRenderer.getConfName() + "]";
				}
			} else if (isNotBlank(mediaRenderer.getConfName())) {
				rendererName = mediaRenderer.getConfName();
			} else {
				rendererName = "Unnamed";
			}
		} else {
			rendererName = "Unknown";
		}
		return rendererName;
	}

	//------------------------- START ContentDirectory -------------------------
	private String contentDirectorySpec(HttpResponse output) throws IOException {
		output.headers().set(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
		output.headers().set(HttpHeaders.Names.EXPIRES, "0");
		output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
		output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		InputStream iStream = getResourceInputStream("UPnP_AV_ContentDirectory_1.0.xml");

		byte[] b = new byte[iStream.available()];
		iStream.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	private static String getSystemUpdateIdHandler() {
		StringBuilder payload = new StringBuilder();
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_HEADER).append(CRLF);
		payload.append("<Id>").append(DLNAResource.getSystemUpdateId()).append("</Id>").append(CRLF);
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_FOOTER);
		return createResponse(payload.toString()).toString();
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

	private StringBuilder samsungGetFeaturesListHandler() {
		StringBuilder features = new StringBuilder();
		String rootFolderId = PMS.get().getRootFolder(mediaRenderer).getResourceId();
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
		return createResponse(response.toString());
	}

	private StringBuilder browseHandler() {
		BrowseRequest requestMessage = getPayload(BrowseRequest.class);
		return this.browseSearchHandler(requestMessage);
	}

	private StringBuilder searchHandler() {
		SearchRequest requestMessage = getPayload(SearchRequest.class);
		try {
			return searchRequestHandler.createSearchResponse(requestMessage, mediaRenderer);
		} catch (Exception e) {
			LOGGER.trace("error transforming searchCriteria to SQL. Fallback to content browsing ...", e);
			return browseHandler();
		}
	}

	/**
	 * Hybrid handler for Browse and Search requests.
	 * FIXME: Should be split up into separate implementations!
	 * New implementation use JUPnP, so forgot this
	 *
	 * @param requestMessage parsed message
	 * @return Soap response as a XML string
	 */
	private StringBuilder browseSearchHandler(BrowseSearchRequest requestMessage) {
		boolean xbox360 = mediaRenderer.isXbox360();
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
		if (xbox360 && configuration.getUseCache() && PMS.get().getLibrary().isEnabled() && containerID != null) {
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
				String artist = getEnclosingValue(content, "upnp:artist = &quot;", "&quot;)");
				if (artist != null) {
					objectID = PMS.get().getLibrary().getArtistFolder().getResourceId();
					searchCriteria = artist;
				}
			}
		} else if (requestMessage instanceof SearchRequest) {
			searchCriteria = requestMessage.getSearchCriteria();
		}

		List<DLNAResource> files = PMS.get().getRootFolder(mediaRenderer).getDLNAResources(
			objectID,
			browseDirectChildren,
			startingIndex,
			requestCount,
			mediaRenderer,
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

				if (
					uf != null &&
					(uf.isCompatible(mediaRenderer) &&
					(uf.getEngine() == null || uf.getEngine().isEngineCompatible(mediaRenderer)) ||
					// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
					// all possible combination not only those supported by renderer because the renderer setting could be wrong.
					files.get(0).isInsideTranscodeFolder())
				) {
					filesData.append(uf.getDidlString(mediaRenderer));
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
			parentFolder = PMS.get().getRootFolder(mediaRenderer).getDLNAResource(objectID, mediaRenderer);
		}

		if (browseDirectChildren && mediaRenderer.isUseMediaInfo() && mediaRenderer.isDLNATreeHack()) {
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
		 * From page 95, section 5.5.8.2 of http://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service.pdf
		 *
		 * UpdateID: The value returned in the UpdateID argument shall be the SystemUpdateID
		 * state variable value at the time the Browse() response was generated. If a control point
		 * finds that the current SystemUpdateID state variable value is not equal to the value
		 * returned in the UpdateID argument, then a change within the ContentDirectory service has
		 * occurred between the time the result was generated and the time that the control point is
		 * processing the result. The control point might therefore want to re-invoke the Browse()
		 * action to ensure that it has the latest property values. Note however that the change in the
		 * value of the SystemUpdateID state variable could have been caused by a change that
		 * occurred in a location in the ContentDirectory tree hierarchy that is not part of the returned
		 * result. In this case, the re-invocation of the Browse() action will return the exact same
		 * result.
		 * Note: This definition is not backwards compatible with previous versions of this
		 * specification. However, the previous definition did not indicate changes to properties
		 * of child containers. Therefore the control point would not have been aware that it had
		 * stale data.
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

	private StringBuilder samsungSetBookmarkHandler() {
		LOGGER.debug("Setting bookmark");
		SamsungBookmark payload = this.getPayload(SamsungBookmark.class);
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
					DLNAResource dlna = PMS.get().getRootFolder(mediaRenderer).getDLNAResource(payload.getObjectId(), mediaRenderer);
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
		return createResponse(HTTPXMLHelper.SETBOOKMARK_RESPONSE);
	}

	private <T> T getPayload(Class<T> clazz) {
		try {
			SOAPMessage message = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(content.getBytes()));
			JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Document body = message.getSOAPBody().extractContentAsDocument();
			return unmarshaller.unmarshal(body, clazz).getValue();
		} catch (JAXBException | SOAPException | IOException e) {
			LOGGER.error("Unmarshalling error", e);
			return null;
		}
	}

	/**
	 * Returns the string value that is enclosed by the left and right tag in a content string.
	 * Only the first match of each tag is used to determine positions. If either of the tags
	 * cannot be found, null is returned.
	 * @param content The entire {@link String} that needs to be searched for the left and right tag.
	 * @param leftTag The {@link String} determining the match for the left tag.
	 * @param rightTag The {@link String} determining the match for the right tag.
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

	private String subscribeHandler(HttpResponse output) throws IOException {
		StringBuilder response = new StringBuilder();
		output.headers().set("SID", PMS.get().usn());

		/**
		 * Requirement [7.2.22.1]: UPnP devices must send events to all properly
		 * subscribed UPnP control points. The device must enforce a subscription
		 * TIMEOUT value of 5 minutes.
		 * The UPnP device behavior of enforcing this 5 minutes TIMEOUT value is
		 * implemented by specifying "TIMEOUT: second-300" as an HTTP header/value pair.
		 */
		output.headers().set("TIMEOUT", "Second-300");

		if (soapaction != null) {
			String cb = soapaction.replace("<", "").replace(">", "");

			try {
				URL soapActionUrl = new URL(cb);
				String addr = soapActionUrl.getHost();
				int port = soapActionUrl.getPort();
				try (
						Socket sock = new Socket(addr, port);
						OutputStream out = sock.getOutputStream()
				) {
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
		response.append(HTTPXMLHelper.eventHeader("urn:schemas-upnp-org:service:ContentDirectory:1"));
		response.append(HTTPXMLHelper.eventProp("TransferIDs"));
		response.append(HTTPXMLHelper.eventProp("ContainerUpdateIDs"));
		response.append(HTTPXMLHelper.eventProp("SystemUpdateID", "" + DLNAResource.getSystemUpdateId()));
		response.append(HTTPXMLHelper.EVENT_FOOTER);
		return response.toString();
	}

	private static String notifyHandler(HttpResponse output) {
		output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
		output.headers().set("NT", "upnp:event");
		output.headers().set("NTS", "upnp:propchange");
		output.headers().set("SID", PMS.get().usn());
		output.headers().set("SEQ", "0");

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
	 * @return The {@link String} containing the date
	 */
	private static String getFutureDate() {
		SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
		return SDF.format(new Date(10000000000L + System.currentTimeMillis()));
	}

	public String getUri() {
		return uri;
	}
}
