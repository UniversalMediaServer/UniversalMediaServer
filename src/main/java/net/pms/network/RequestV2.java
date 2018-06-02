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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.*;
import net.pms.encoders.ImagePlayer;
import net.pms.external.StartStopListenerDelegate;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import static net.pms.util.StringUtil.convertStringToTime;
import net.pms.util.UMSUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.lang3.StringEscapeUtils;
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

/**
 * This class handles all forms of incoming HTTP requests by constructing a proper HTTP response.
 */
public class RequestV2 extends HTTPResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestV2.class);
	private final static String CRLF = "\r\n";
	private static final Pattern DIDL_PATTERN = Pattern.compile("<Result>(&lt;DIDL-Lite.*?)</Result>");
	private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static int BUFFER_SIZE = 8 * 1024;
	private final String method;
	private PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * A {@link String} that contains the argument with which this {@link RequestV2} was
	 * created. It contains a command, a unique resource id and a resource name, all
	 * separated by slashes. For example: "get/0$0$2$17/big_buck_bunny_1080p_h264.mov" or
	 * "get/0$0$2$13/thumbnail0000Sintel.2010.1080p.mkv"
	 */
	private String argument;
	private String soapaction;
	private String content;
	private String objectID;
	private int startingIndex;
	private int requestCount;
	private String browseFlag;

	/**
	 * When sending an input stream, the lowRange indicates which byte to start from.
	 */
	private long lowRange;
	private InputStream inputStream;
	private RendererConfiguration mediaRenderer;
	private String transferMode;
	private String contentFeatures;
	private final Range.Time range = new Range.Time();

	/**
	 * When sending an input stream, the highRange indicates which byte to stop at.
	 */
	private long highRange;
	private boolean http10;

	public RendererConfiguration getMediaRenderer() {
		return mediaRenderer;
	}

	public void setMediaRenderer(RendererConfiguration mediaRenderer) {
		this.mediaRenderer = mediaRenderer;
		// Use device-specific pms conf
		configuration = PMS.getConfiguration(mediaRenderer);
	}

	public InputStream getInputStream() {
		return inputStream;
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
		setTimeRangeStart(convertStringToTime(str));
	}

	public void setTimeRangeEnd(Double rangeEnd) {
		this.range.setEnd(rangeEnd);
	}

	public void setTimeRangeEndString(String str) {
		setTimeRangeEnd(convertStringToTime(str));
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

	public boolean isHttp10() {
		return http10;
	}

	public void setHttp10(boolean http10) {
		this.http10 = http10;
	}

	/**
	 * This class will construct and transmit a proper HTTP response to a given HTTP request.
	 * Rewritten version of the {@link Request} class.
	 * @param method The {@link String} that defines the HTTP method to be used.
	 * @param argument The {@link String} containing instructions for PMS. It contains a command,
	 * 		a unique resource id and a resource name, all separated by slashes.
	 */
	public RequestV2(String method, String argument) {
		this.method = method;
		this.argument = argument;
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
	 * Retrieves the HTTP method with which this {@link RequestV2} was created.
	 * @return The (@link String} containing the HTTP method.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Retrieves the argument with which this {@link RequestV2} was created. It contains
	 * a command, a unique resource id and a resource name, all separated by slashes. For
	 * example: "get/0$0$2$17/big_buck_bunny_1080p_h264.mov" or "get/0$0$2$13/thumbnail0000Sintel.2010.1080p.mkv"
	 * @return The {@link String} containing the argument.
	 */
	public String getArgument() {
		return argument;
	}

	/**
	 * Construct a proper HTTP response to a received request. After the response has been
	 * created, it is sent and the resulting {@link ChannelFuture} object is returned.
	 * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
	 * for HTTP header field definitions.
	 *
	 * @param ctx
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
		ChannelFuture future = null;
		long CLoverride = -2; // 0 and above are valid Content-Length values, -1 means omit
		StringBuilder response = new StringBuilder();
		DLNAResource dlna = null;
		boolean xbox360 = mediaRenderer.isXbox360();

		// Samsung 2012 TVs have a problematic preceding slash that needs to be removed.
		if (argument.startsWith("/")) {
			LOGGER.trace("Stripping preceding slash from: " + argument);
			argument = argument.substring(1);
		}

		if ((method.equals("GET") || method.equals("HEAD")) && argument.startsWith("console/")) {
			// Request to output a page to the HTML console.
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
			response.append(HTMLConsole.servePage(argument.substring(8)));
		} else if ((method.equals("GET") || method.equals("HEAD")) && argument.startsWith("get/")) {
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
			String id = argument.substring(argument.indexOf("get/") + 4);

			// Some clients escape the separators in their request: unescape them.
			id = id.replace("%24", "$");

			// Retrieve the DLNAresource itself.
			dlna = PMS.get().getRootFolder(mediaRenderer).getDLNAResource(id, mediaRenderer);
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
				if (fileName.startsWith("thumbnail0000")) {
					// This is a request for a thumbnail file.
					DLNAImageProfile imageProfile = ImagesUtil.parseThumbRequest(fileName);
					output.headers().set(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
					output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
					output.headers().set(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
					output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

					DLNAThumbnailInputStream thumbInputStream;
					if (!configuration.isShowCodeThumbs() && !dlna.isCodeValid(dlna)) {
						thumbInputStream = dlna.getGenericThumbnailInputStream(null);
					} else {
						dlna.checkThumbnail();
						thumbInputStream = dlna.fetchThumbnailInputStream();
					}
					if (dlna instanceof RealFile && FullyPlayed.isFullyPlayedThumbnail(((RealFile) dlna).getFile())) {
						thumbInputStream = FullyPlayed.addFullyPlayedOverlay(thumbInputStream);
					}
					inputStream = thumbInputStream.transcode(imageProfile, mediaRenderer != null ? mediaRenderer.isThumbnailPadding() : false);
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
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, imageProfile.getMimeType());
						output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
						output.headers().set(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
						output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
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
					output.headers().set(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
					DLNAMediaSubtitle sub = dlna.getMediaSubtitle();
					if (sub != null) {
						// XXX external file is null if the first subtitle track is embedded:
						// http://www.ps3mediaserver.org/forum/viewtopic.php?f=3&t=15805&p=75534#p75534
						if (sub.isExternal()) {
							try {
								if (sub.getType() == SubtitleType.SUBRIP && mediaRenderer.isRemoveTagsFromSRTsubs()) { // remove tags from .srt subs when renderer doesn't support them
									inputStream = SubtitleUtils.removeSubRipTags(sub.getExternalFile());
								} else {
									inputStream = new FileInputStream(sub.getExternalFile());
								}
								LOGGER.trace("Loading external subtitles file: {}", sub);
							} catch (IOException ioe) {
								LOGGER.debug("Couldn't load external subtitles file: {}\nCause: {}", sub, ioe.getMessage());
								LOGGER.trace("", ioe);
							}
						} else {
							LOGGER.trace("Not loading external subtitles file because it is embedded: {}", sub);
						}
					} else {
						LOGGER.trace("Not loading external subtitles because dlna.getMediaSubtitle() returned null");
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
						if (dlna.getMedia() != null && !configuration.isDisableSubtitles() && dlna.getMediaSubtitle() != null && dlna.getMediaSubtitle().isStreamable()) {
							// Some renderers (like Samsung devices) allow a custom header for a subtitle URL
							String subtitleHttpHeader = mediaRenderer.getSubtitleHttpHeader();
							if (isNotBlank(subtitleHttpHeader)) {
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

								output.headers().set(subtitleHttpHeader, subtitleUrl);
							} else {
								LOGGER.trace(
									"Did not send subtitle headers because mediaRenderer.getSubtitleHttpHeader() returned {}",
									subtitleHttpHeader == null ? "null" : "\"" + subtitleHttpHeader + "\""
								);
							}
						} else if (LOGGER.isTraceEnabled()) {
							ArrayList<String> reasons = new ArrayList<>();
							if (dlna.getMedia() == null) {
								reasons.add("dlna.getMedia() is null");
							}
							if (configuration.isDisableSubtitles()) {
								reasons.add("configuration.isDisabledSubtitles() is true");
							}
							if (dlna.getMediaSubtitle() == null) {
								reasons.add("dlna.getMediaSubtitle() is null");
							} else if (!dlna.getMediaSubtitle().isStreamable()) {
								reasons.add("dlna.getMediaSubtitle().isStreamable() is false");
							}
							LOGGER.trace("Did not send subtitle headers because {}", StringUtil.createReadableCombinedString(reasons));
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
						String rendererMimeType = getRendererMimeType(dlna.mimeType(), mediaRenderer, dlna.getMedia());

						if (rendererMimeType != null && !"".equals(rendererMimeType)) {
							output.headers().set(HttpHeaders.Names.CONTENT_TYPE, rendererMimeType);
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

							output.headers().set(HttpHeaders.Names.CONTENT_RANGE, "bytes " + lowRange + "-" + (highRange > -1 ? highRange : "*") + "/" + (totalsize > -1 ? totalsize : "*"));

							// Content-Length refers to the current chunk size here, though in chunked
							// mode if the request is open-ended and totalsize is unknown we omit it.
							if (chunked && requested < 0 && totalsize < 0) {
								CLoverride = -1;
							} else {
								CLoverride = bytes;
							}
						} else {
							// Content-Length refers to the total remaining size of the stream here.
							CLoverride = remaining;
						}

						// Calculate the corresponding highRange (this is usually redundant).
						highRange = lowRange + CLoverride - (CLoverride > 0 ? 1 : 0);

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
		} else if ((method.equals("GET") || method.equals("HEAD")) && (argument.toLowerCase().endsWith(".png") || argument.toLowerCase().endsWith(".jpg") || argument.toLowerCase().endsWith(".jpeg"))) {
			if (argument.toLowerCase().endsWith(".png")) {
				output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/png");
			} else {
				output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
			}

			output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			output.headers().set(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
			inputStream = getResourceInputStream(argument);
		} else if ((method.equals("GET") || method.equals("HEAD")) && (argument.equals("description/fetch") || argument.endsWith("1.0.xml"))) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
			output.headers().set(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
			output.headers().set(HttpHeaders.Names.EXPIRES, "0");
			output.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			inputStream = getResourceInputStream((argument.equals("description/fetch") ? "PMS.xml" : argument));

			if (argument.equals("description/fetch")) {
				byte b[] = new byte[inputStream.available()];
				inputStream.read(b);
				String s = new String(b, StandardCharsets.UTF_8);
				s = s.replace("[uuid]", PMS.get().usn()); //.substring(0, PMS.get().usn().length()-2));

				if (PMS.get().getServer().getHost() != null) {
					s = s.replace("[host]", PMS.get().getServer().getHost());
					s = s.replace("[port]", "" + PMS.get().getServer().getPort());
				}

				if (xbox360) {
					LOGGER.debug("DLNA changes for Xbox 360");
					s = s.replace("Universal Media Server", configuration.getServerDisplayName() + " : Windows Media Connect");
					s = s.replace("<modelName>UMS</modelName>", "<modelName>Windows Media Connect</modelName>");
					s = s.replace("<serviceList>", "<serviceList>" + CRLF + "<service>" + CRLF +
						"<serviceType>urn:microsoft.com:service:X_MS_MediaReceiverRegistrar:1</serviceType>" + CRLF +
						"<serviceId>urn:microsoft.com:serviceId:X_MS_MediaReceiverRegistrar</serviceId>" + CRLF +
						"<SCPDURL>/upnp/mrr/scpd</SCPDURL>" + CRLF +
						"<controlURL>/upnp/mrr/control</controlURL>" + CRLF +
						"</service>" + CRLF);
				} else {
					s = s.replace("Universal Media Server", configuration.getServerDisplayName());
				}

				response.append(s);
				inputStream = null;
			}
		} else if (method.equals("POST") && (argument.contains("MS_MediaReceiverRegistrar_control") || argument.contains("mrr/control"))) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
			response.append(HTTPXMLHelper.XML_HEADER);
			response.append(CRLF);
			response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
			response.append(CRLF);

			if (soapaction != null && soapaction.contains("IsAuthorized")) {
				response.append(HTTPXMLHelper.XBOX_360_2);
				response.append(CRLF);
			} else if (soapaction != null && soapaction.contains("IsValidated")) {
				response.append(HTTPXMLHelper.XBOX_360_1);
				response.append(CRLF);
			}

			response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
			response.append(CRLF);
		} else if (method.equals("POST") && argument.endsWith("upnp/control/connection_manager")) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

			if (soapaction != null && soapaction.contains("ConnectionManager:1#GetProtocolInfo")) {
				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.PROTOCOLINFO_RESPONSE);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			}
		} else if (method.equals("POST") && argument.endsWith("upnp/control/content_directory")) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");

			if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSystemUpdateID")) {
				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.GETSYSTEMUPDATEID_HEADER);
				response.append(CRLF);
				response.append("<Id>").append(DLNAResource.getSystemUpdateId()).append("</Id>");
				response.append(CRLF);
				response.append(HTTPXMLHelper.GETSYSTEMUPDATEID_FOOTER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_GetFeatureList")) { // Added for Samsung 2012 TVs
				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SAMSUNG_ERROR_RESPONSE);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSortCapabilities")) {
				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SORTCAPS_RESPONSE);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSearchCapabilities")) {
				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SEARCHCAPS_RESPONSE);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			} else if (soapaction != null && (soapaction.contains("ContentDirectory:1#Browse") || soapaction.contains("ContentDirectory:1#Search"))) {
				objectID = getEnclosingValue(content, "<ObjectID", "</ObjectID>");
				String containerID = null;
				if ((objectID == null || objectID.length() == 0)) {
					containerID = getEnclosingValue(content, "<ContainerID", "</ContainerID>");
					if (containerID == null || (xbox360 && !containerID.contains("$"))) {
						objectID = "0";
					} else {
						objectID = containerID;
						containerID = null;
					}
				}
				String sI = getEnclosingValue(content, "<StartingIndex", "</StartingIndex>");
				String rC = getEnclosingValue(content, "<RequestedCount", "</RequestedCount>");
				browseFlag = getEnclosingValue(content, "<BrowseFlag", "</BrowseFlag>");

				if (sI != null) {
					startingIndex = Integer.parseInt(sI);
				}

				if (rC != null) {
					requestCount = Integer.parseInt(rC);
				}

				response.append(HTTPXMLHelper.XML_HEADER);
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
				response.append(CRLF);

				if (soapaction.contains("ContentDirectory:1#Search")) {
					response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
				} else {
					response.append(HTTPXMLHelper.BROWSERESPONSE_HEADER);
				}

				response.append(CRLF);
				response.append(HTTPXMLHelper.RESULT_HEADER);
				response.append(HTTPXMLHelper.DIDL_HEADER);

				boolean browseDirectChildren = browseFlag != null && browseFlag.equals("BrowseDirectChildren");

				if (soapaction.contains("ContentDirectory:1#Search")) {
					browseDirectChildren = true;
				}

				// Xbox 360 virtual containers ... d'oh!
				String searchCriteria = null;
				if (xbox360 && configuration.getUseCache() && PMS.get().getLibrary() != null && containerID != null) {
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
				} else if (soapaction.contains("ContentDirectory:1#Search")) {
					searchCriteria = getEnclosingValue(content, "<SearchCriteria", "</SearchCriteria>");
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
					UMSUtils.postSearch(files, searchCriteria);
					if (xbox360) {
						if (files.size() > 0) {
							files = files.get(0).getChildren();
						}
					}
				}

				int minus = 0;
				if (files != null) {
					for (DLNAResource uf : files) {
						if (xbox360 && containerID != null) {
							uf.setFakeParentId(containerID);
						}

						if (uf.isCompatible(mediaRenderer) && (uf.getPlayer() == null
							|| uf.getPlayer().isPlayerCompatible(mediaRenderer))
							 // do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
							 // all possible combination not only those supported by renderer because the renderer setting could be wrong.
							|| files.get(0).getParent() instanceof FileTranscodeVirtualFolder) {
								response.append(uf.getDidlString(mediaRenderer));
						} else {
							minus++;
						}
					}
				}

				response.append(HTTPXMLHelper.DIDL_FOOTER);
				response.append(HTTPXMLHelper.RESULT_FOOTER);
				response.append(CRLF);

				int filessize = 0;
				if (files != null) {
					filessize = files.size();
				}

				response.append("<NumberReturned>").append(filessize - minus).append("</NumberReturned>");
				response.append(CRLF);
				DLNAResource parentFolder = null;

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
				response.append("<UpdateID>");

				if (parentFolder != null) {
					response.append(parentFolder.getUpdateId());
				} else {
					response.append('1');
				}

				response.append("</UpdateID>");
				response.append(CRLF);
				if (soapaction.contains("ContentDirectory:1#Search")) {
					response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
				} else {
					response.append(HTTPXMLHelper.BROWSERESPONSE_FOOTER);
				}
				response.append(CRLF);
				response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
				response.append(CRLF);
			}
		} else if (method.equals("SUBSCRIBE")) {
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
						out.write(("NOTIFY /" + argument + " HTTP/1.1").getBytes(StandardCharsets.UTF_8));
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

			if (argument.contains("connection_manager")) {
				response.append(HTTPXMLHelper.eventHeader("urn:schemas-upnp-org:service:ConnectionManager:1"));
				response.append(HTTPXMLHelper.eventProp("SinkProtocolInfo"));
				response.append(HTTPXMLHelper.eventProp("SourceProtocolInfo"));
				response.append(HTTPXMLHelper.eventProp("CurrentConnectionIDs"));
				response.append(HTTPXMLHelper.EVENT_FOOTER);
			} else if (argument.contains("content_directory")) {
				response.append(HTTPXMLHelper.eventHeader("urn:schemas-upnp-org:service:ContentDirectory:1"));
				response.append(HTTPXMLHelper.eventProp("TransferIDs"));
				response.append(HTTPXMLHelper.eventProp("ContainerUpdateIDs"));
				response.append(HTTPXMLHelper.eventProp("SystemUpdateID", "" + DLNAResource.getSystemUpdateId()));
				response.append(HTTPXMLHelper.EVENT_FOOTER);
			}
		} else if (method.equals("NOTIFY")) {
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
			output.headers().set("NT", "upnp:event");
			output.headers().set("NTS", "upnp:propchange");
			output.headers().set("SID", PMS.get().usn());
			output.headers().set("SEQ", "0");
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
		}

		output.headers().set(HttpHeaders.Names.SERVER, PMS.get().getServerName());

		if (response.length() > 0) {
			// A response message was constructed; convert it to data ready to be sent.
			byte responseData[] = response.toString().getBytes("UTF-8");
			output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "" + responseData.length);

			// HEAD requests only require headers to be set, no need to set contents.
			if (!method.equals("HEAD")) {
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

			if (CLoverride > -2) {
				// Content-Length override has been set, send or omit as appropriate
				if (CLoverride > -1 && CLoverride != DLNAMediaInfo.TRANS_SIZE) {
					// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
					// as the PS3 will display a network error and request the last seconds of the
					// transcoded video. Better to send no Content-Length at all.
					output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "" + CLoverride);
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

			if (lowRange != DLNAMediaInfo.ENDFILE_POS && !method.equals("HEAD")) {
				// Send the response body to the client in chunks.
				ChannelFuture chunkWriteFuture = event.getChannel().write(new ChunkedStream(inputStream, BUFFER_SIZE));

				// Add a listener to clean up after sending the entire response body.
				chunkWriteFuture.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) {
						try {
							inputStream.close();
						} catch (IOException e) {
							LOGGER.error("Caught exception", e);
						}

						// Always close the channel after the response is sent because of
						// a freeze at the end of video when the channel is not closed.
						future.getChannel().close();
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

				startStopListenerDelegate.stop();
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

		if (LOGGER.isTraceEnabled()) {
			// Log trace information
			StringBuilder header = new StringBuilder();
			for (Entry<String, String> entry : output.headers().entries()) {
				if (isNotBlank(entry.getKey())) {
					header.append("  ").append(entry.getKey())
					.append(": ").append(entry.getValue()).append("\n");
				}
			}
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

			if (method.equals("HEAD")) {
				LOGGER.trace(
					"HEAD only response sent to {}:\n\nHEADER:\n  {} {}\n{}",
					rendererName,
					output.getProtocolVersion(),
					output.getStatus(),
					header
				);
			} else {
				String formattedResponse = null;
				if (isNotBlank(response)) {
					try {
						formattedResponse = StringUtil.prettifyXML(response.toString(), 4);
					} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
						formattedResponse = "  Content isn't valid XML, using text formatting: " + e.getMessage()  + "\n";
						formattedResponse += "    " + response.toString().replaceAll("\n", "\n    ");
					}
				}
				if (isNotBlank(formattedResponse)) {
					LOGGER.trace(
						"Response sent to {}:\n\nHEADER:\n  {} {}\n{}\nCONTENT:\n{}",
						rendererName,
						output.getProtocolVersion(),
						output.getStatus(),
						header,
						formattedResponse
					);
					Matcher matcher = DIDL_PATTERN.matcher(response);
					if (matcher.find()) {
						try {
							LOGGER.trace(
								"The unescaped <Result> sent to {} is:\n{}",
								rendererName,
								StringUtil.prettifyXML(StringEscapeUtils.unescapeXml(matcher.group(1)), 2)
							);
						} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
							LOGGER.warn("Failed to prettify DIDL-Lite document: {}", e.getMessage());
							LOGGER.trace("", e);
						}
					}
				} else if (inputStream != null && !"0".equals(output.headers().get(HttpHeaders.Names.CONTENT_LENGTH))) {
					LOGGER.trace(
						"Transfer response sent to {}:\n\nHEADER:\n  {} {} ({})\n{}",
						rendererName,
						output.getProtocolVersion(),
						output.getStatus(),
						output.isChunked() ? "chunked" : "non-chunked",
						header
					);
				} else {
					LOGGER.trace(
						"Empty response sent to {}:\n\nHEADER:\n  {} {}\n{}",
						rendererName,
						output.getProtocolVersion(),
						output.getStatus(),
						header
					);
				}
			}
		}
		return future;
	}

	/**
	 * Returns a date somewhere in the far future.
	 * @return The {@link String} containing the date
	 */
	private String getFUTUREDATE() {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(new Date(10000000000L + System.currentTimeMillis()));
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
}
