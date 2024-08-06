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
package net.pms.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableSubtracks;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.encoders.Engine;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.TranscodingSettings;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.SizeLimitInputStream;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.MediaType;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.MediaServerRequest;
import net.pms.parsers.Parser;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.MediaScannerDevice;
import net.pms.store.container.ChapterFileTranscodeVirtualFolder;
import net.pms.store.item.DVDISOTitle;
import net.pms.store.item.RealFile;
import net.pms.store.item.VirtualVideoAction;
import net.pms.util.ByteRange;
import net.pms.util.FileUtil;
import net.pms.util.IPushOutput;
import net.pms.util.InputFile;
import net.pms.util.Iso639;
import net.pms.util.MpegUtil;
import net.pms.util.Range;
import net.pms.util.SubtitleUtils;
import net.pms.util.TimeRange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an item (media).
 *
 * This is widely used by the UPNP ContentBrowser service.
 */
public abstract class StoreItem extends StoreResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreItem.class);
	private static final int STOP_PLAYING_DELAY = 4000;
	private static final double CONTAINER_OVERHEAD = 1.04;

	/**
	 * Represents the transformation to be used to the file.
	 */
	private TranscodingSettings transcodingSettings;
	private boolean skipTranscode = false;
	private ProcessWrapper externalProcess;

	private Format format;
	private int specificType;
	private MediaAudio mediaAudio;

	/**
	 * The time range for the file containing the start and end time in seconds.
	 */
	private TimeRange splitRange = new TimeRange();

	/**
	 * The system time when the resource was last (re)started by a user.
	 */
	private long lastStartSystemTimeUser;

	/**
	 * The system time when the resource was last (re)started.
	 */
	private long lastStartSystemTime;

	/**
	 * The most recently requested time offset in seconds.
	 */
	private double lastStartPosition;

	private double lastTimeSeek = -1.0;

	private final Map<String, Integer> requestIdToRefcount = new HashMap<>();

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////
	private ResumeObj resume;
	private int resHash = 0;

	private MediaSubtitle mediaSubtitle;
	/**
	 * Used to synchronize access to {@link #hasExternalSubtitles},
	 * {@link #hasSubtitles} and {@link #isExternalSubtitlesParsed}
	 */
	private final Object subtitlesLock = new Object();

	private boolean hasExternalSubtitles;
	private boolean hasSubtitles;
	private boolean isExternalSubtitlesParsed;

	protected StoreItem(Renderer renderer) {
		this(renderer, Format.UNKNOWN);
	}

	protected StoreItem(Renderer renderer, int specificType) {
		super(renderer);
		setSpecificType(specificType);
		setSortable(true);
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	/**
	 * Returns the {@link Format} of this resource, which defines its
	 * capabilities.
	 *
	 * @return The format of this resource.
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * Sets the {@link Format} of this resource, thereby defining its
	 * capabilities.
	 *
	 * @param format The format to set.
	 */
	public void setFormat(Format format) {
		this.format = format;
	}

	/**
	 * Sets the StoreResource's {@link net.pms.formats.Format} according to
	 * its filename if it isn't set already.
	 *
	 * @since 1.90.0
	 */
	protected void resolveFormat() {
		if (format == null) {
			format = FormatFactory.getAssociatedFormat(getFileName());
		}

		if (format != null && format.isUnknown()) {
			format.setType(getSpecificType());
		}
	}

	/**
	 * Returns the from - to time range for this resource.
	 *
	 * @return The time range.
	 */
	public TimeRange getSplitRange() {
		return splitRange;
	}

	/**
	 * Sets the from - to time range for this resource.
	 *
	 * @param splitRange The time range to set.
	 * @since 1.50
	 */
	public void setSplitRange(TimeRange splitRange) {
		this.splitRange = splitRange;
	}

	/**
	 * Returns the {@link TranscodingSettings} object that is used to encode this resource
	 * for the renderer.
	 *
	 * Can be null.
	 *
	 * @return The transcodingSettings object.
	 */
	public TranscodingSettings getTranscodingSettings() {
		return transcodingSettings;
	}

	/**
	 * Sets the {@link TranscodingSettings} object that is to be used to encode this resource
	 * for the renderer.
	 *
	 * The transcodingSettings object can be null.
	 *
	 * @param transcodingSettings The transcodingSettings object to set.
	 * @since 1.50
	 */
	public void setTranscodingSettings(TranscodingSettings transcodingSettings) {
		this.transcodingSettings = transcodingSettings;
	}

	public boolean isTranscoded() {
		return getTranscodingSettings() != null;
	}

	public boolean isTimeSeekable() {
		return isTranscoded() ? getTranscodingSettings().getEngine().isTimeSeekable() : true;
	}

	/**
	 * Returns the engine name.
	 *
	 * @return The engine name.
	 */
	protected String getEngineName() {
		if (isTranscoded()) {
			return getTranscodingSettings().getEngine().getName();
		}
		return Messages.getString("NoTranscoding");
	}

	public int getType() {
		if (getFormat() != null) {
			return getFormat().getType();
		}
		return Format.UNKNOWN;
	}

	/**
	 * Returns the specific type of resource. Valid types are defined in
	 * {@link Format}.
	 *
	 * @return The specific type
	 */
	protected int getSpecificType() {
		return specificType;
	}

	/**
	 * Set the specific type of this resource. Valid types are defined in
	 * {@link Format}.
	 *
	 * @param specificType The specific type to set.
	 */
	private void setSpecificType(int specificType) {
		this.specificType = specificType;
	}

	/**
	 * Returns the {@link MediaAudio} object for this resource that contains the
	 * audio specifics. A resource can have many audio tracks, this method
	 * returns the one that should be played.
	 *
	 * @return The audio object containing detailed information.
	 * @since 1.50
	 */
	public MediaAudio getMediaAudio() {
		return mediaAudio;
	}

	/**
	 * Sets the {@link MediaAudio} object for this resource that contains the
	 * audio specifics. A resource can have many audio tracks, this method
	 * determines the one that should be played.
	 *
	 * @param mediaAudio The audio object containing detailed information.
	 * @since 1.50
	 */
	public void setMediaAudio(MediaAudio mediaAudio) {
		this.mediaAudio = mediaAudio;
	}

	/**
	 * Returns the {@link MediaSubtitle} object for this resource that contains
	 * the specifics for the subtitles. A resource can have many subtitles, this
	 * method returns the one that should be displayed.
	 *
	 * @return The subtitle object containing detailed information.
	 * @since 1.50
	 */
	public MediaSubtitle getMediaSubtitle() {
		return mediaSubtitle;
	}

	/**
	 * Sets the {@link MediaSubtitle} object for this resource that contains the
	 * specifics for the subtitles. A resource can have many subtitles, this
	 * method determines the one that should be used.
	 *
	 * @param mediaSubtitle The subtitle object containing detailed information.
	 * @since 1.50
	 */
	public void setMediaSubtitle(MediaSubtitle mediaSubtitle) {
		this.mediaSubtitle = mediaSubtitle;
	}

	/**
	 * Determines whether this resource has external subtitles.
	 *
	 * @return {@code true} if this resource has external subtitles,
	 * {@code false} otherwise.
	 */
	public boolean hasExternalSubtitles() {
		return hasExternalSubtitles(false);
	}

	/**
	 * Determines whether this resource has external subtitles.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 * subtitles instead of relying on cached information (if it exists).
	 * @return {@code true} if this resource has external subtitles,
	 * {@code false} otherwise.
	 */
	public boolean hasExternalSubtitles(boolean forceRefresh) {
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return false;
		}
		synchronized (subtitlesLock) {
			registerExternalSubtitles(forceRefresh);
			return hasExternalSubtitles;
		}
	}

	/**
	 * Determines whether this resource has subtitles.
	 *
	 * @return {@code true} if this resource has subtitles, {@code false}
	 * otherwise.
	 */
	public boolean hasSubtitles() {
		return hasSubtitles(false);
	}

	/**
	 * Determines whether this resource has subtitles.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 * subtitles instead of relying on cached information (if it exists).
	 * @return {@code true} if this resource has subtitles, {@code false}
	 * otherwise.
	 */
	public boolean hasSubtitles(boolean forceRefresh) {
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return false;
		}
		synchronized (subtitlesLock) {
			registerExternalSubtitles(forceRefresh);
			return hasSubtitles;
		}
	}

	/**
	 * Determines whether this resource has internal/embedded subtitles.
	 *
	 * @return {@code true} if this resource has internal/embedded subtitles,
	 * {@code false} otherwise.
	 */
	public boolean hasEmbeddedSubtitles() {
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return false;
		}

		List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
		if (subtitlesList != null) {
			for (MediaSubtitle subtitles : subtitlesList) {
				if (subtitles.isEmbedded()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return true if the {@link defaultRenderer#renderer} can understand type
	 * of mediaInfo. Also returns true if this StoreResource is a container.
	 */
	public boolean isCompatible() {
		return format == null || format.isUnknown() || (format.isVideo() && renderer.isVideoSupported()) ||
				(format.isAudio() && renderer.isAudioSupported()) || (format.isImage() && renderer.isImageSupported());
	}

	/**
	 * Returns true if transcoding should be skipped for this resource.
	 *
	 * @return True if transcoding should be skipped, false otherwise.
	 * @since 1.50
	 */
	private boolean isSkipTranscode() {
		return skipTranscode;
	}

	/**
	 * Determine whether we are a candidate for streaming or transcoding to the
	 * renderer, and return the relevant TranscodingSettings or null as appropriate.
	 *
	 * @return An TranscodingSettings if transcoding or null if streaming
	 */
	public TranscodingSettings resolveTranscodingSettings() {
		if (renderer instanceof MediaScannerDevice) {
			return null;
		}

		if (renderer.getUmsConfiguration().isDisableTranscoding()) {
			LOGGER.debug("Final verdict: \"{}\" will be streamed since transcoding is disabled", getName());
			return null;
		}

		// Use device-specific conf, if any
		boolean parserV2 = mediaInfo != null && renderer.isUseMediaInfo();
		TranscodingSettings resolvedTranscodingSettings;

		if (mediaInfo == null) {
			mediaInfo = new MediaInfo();
		}

		if (format == null) {
			// Shouldn't happen, this is just a desperate measure
			Format f = FormatFactory.getAssociatedFormat(getSystemName());
			setFormat(f != null ? f : FormatFactory.getAssociatedFormat(".mpg"));
		}

		// Check if we're a transcode folder item
		if (isInsideTranscodeFolder()) {
			// Yes, leave everything as-is
			resolvedTranscodingSettings = getTranscodingSettings();
			LOGGER.trace("Selecting transcodingSettings {} based on transcode item settings", resolvedTranscodingSettings);
			return resolvedTranscodingSettings;
		}

		// Resolve subtitles stream
		if (mediaInfo.isVideo() && !renderer.getUmsConfiguration().isDisableSubtitles() && hasSubtitles(false)) {
			MediaAudio audio = mediaAudio != null ? mediaAudio : resolveAudioStream();
			if (mediaSubtitle == null) {
				mediaSubtitle = resolveSubtitlesStream(audio == null ? null : audio.getLang(), false);
			}
		}

		String rendererForceExtensions = renderer.getTranscodedExtensions();
		String rendererSkipExtensions = renderer.getStreamedExtensions();
		String configurationForceExtensions = renderer.getUmsConfiguration().getForceTranscodeForExtensions();
		String configurationSkipExtensions = renderer.getUmsConfiguration().getDisableTranscodeForExtensions();

		// Should transcoding be skipped for this format?
		skipTranscode = format.skip(configurationSkipExtensions, rendererSkipExtensions);

		if (skipTranscode) {
			LOGGER.debug("Final verdict: \"{}\" will be streamed since it is forced by configuration", getName());
			return null;
		}

		// Should transcoding be forced for this format?
		boolean forceTranscode = format.skip(configurationForceExtensions, rendererForceExtensions);

		// Try to match an engine based on mediaInfo information and format.
		resolvedTranscodingSettings = TranscodingSettings.getBestTranscodingSettings(this);

		boolean isIncompatible = false;
		if (resolvedTranscodingSettings != null) {
			String prependTranscodingReason = "File \"{}\" will not be streamed because ";
			if (forceTranscode) {
				LOGGER.debug(prependTranscodingReason + "transcoding is forced by configuration", getName());
			} else if (this instanceof DVDISOTitle) {
				forceTranscode = true;
				LOGGER.debug(prependTranscodingReason + "streaming of DVD video tracks isn't supported", getName());
			} else if (!format.isCompatible(this, renderer)) {
				isIncompatible = true;
				LOGGER.debug(prependTranscodingReason + "it is not supported by the renderer {}", getName(),
						renderer.getRendererName());
			} else if (renderer.getUmsConfiguration().isEncodedAudioPassthrough()) {
				if (mediaAudio != null && (FormatConfiguration.AC3.equals(mediaAudio.getAudioCodec()) ||
						FormatConfiguration.DTS.equals(mediaAudio.getAudioCodec()))) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
				} else {
					for (MediaAudio audioTrack : mediaInfo.getAudioTracks()) {
						if (audioTrack != null && (FormatConfiguration.AC3.equals(audioTrack.getAudioCodec()) ||
								FormatConfiguration.DTS.equals(audioTrack.getAudioCodec()))) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
							break;
						}
					}
				}
			}

			if (!forceTranscode && !isIncompatible && format.isVideo() && parserV2 && mediaInfo.getDefaultVideoTrack() != null) {
				MediaVideo mediaVideo = mediaInfo.getDefaultVideoTrack();
				int maxBandwidth = renderer.getMaxBandwidth();

				if (renderer.isKeepAspectRatio() && !"16:9".equals(mediaVideo.getDisplayAspectRatio())) {
					isIncompatible = true;
					LOGGER.debug(
							prependTranscodingReason + "the renderer needs us to add borders to change the aspect ratio from {} to 16/9.",
							getName(), mediaVideo.getDisplayAspectRatio());
				} else if (!renderer.isResolutionCompatibleWithRenderer(mediaVideo.getWidth(), mediaVideo.getHeight())) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the resolution is incompatible with the renderer.", getName());
				} else if (mediaInfo.getBitRate() > maxBandwidth) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the bitrate ({} b/s) is too high ({} b/s).", getName(), mediaInfo.getBitRate(),
							maxBandwidth);
				} else if (mediaVideo.isH264()) {
					double h264LevelLimit = renderer.getH264LevelLimit();
					if (mediaVideo.getFormatLevel() != null) {
						double h264Level = mediaVideo.getFormatLevelAsDouble(4.1);
						if (h264Level > h264LevelLimit) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the H.264 level ({}) is not supported by the renderer (limit: {}).", getName(), h264Level, h264LevelLimit);
						}
					} else if (h264LevelLimit < 4.2) {
						isIncompatible = true;
						LOGGER.debug(prependTranscodingReason + "the H.264 level is unknown.", getName());
					}
				} else if (mediaVideo.is3d() && StringUtils.isNotBlank(renderer.getOutput3DFormat()) &&
						(!mediaVideo.get3DLayout().toString().toLowerCase(Locale.ROOT).equals(renderer.getOutput3DFormat()))) {
					forceTranscode = true;
					LOGGER.debug(prependTranscodingReason + "it is 3D and is forced to transcode to the format \"{}\"", getName(),
							renderer.getOutput3DFormat());
				}
			}

			/*
			 * Transcode if: 1) transcoding is forced by configuration, or 2)
			 * transcoding is not prevented by configuration and is needed due
			 * to subtitles or some other renderer incompatibility
			 */
			if (forceTranscode || (isIncompatible && !isSkipTranscode())) {
				if (parserV2) {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with transcodingSettings \"{}\" with mime type \"{}\"", getName(),
							resolvedTranscodingSettings.toString(), getMimeType());
				} else {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with transcodingSettings \"{}\"", getName(), resolvedTranscodingSettings.toString());
				}
			} else {
				resolvedTranscodingSettings = null;
				LOGGER.debug("Final verdict: \"{}\" will be streamed", getName());
			}
		} else {
			LOGGER.debug("Final verdict: \"{}\" will be streamed because no compatible engine was found", getName());
		}
		return resolvedTranscodingSettings;
	}

	/**
	 * Gets the media renderer's mime type if available, returns a default mime
	 * type otherwise.
	 *
	 * @return String representation of the mime type
	 */
	public String getRendererMimeType() {
		String mime = getMimeType();

		// Use our best guess if we have no valid mime type
		if (mime == null || mime.contains("/transcode")) {
			mime = HTTPResource.getDefaultMimeType(getType());
		}

		return mime;
	}

	/**
	 * Plugin implementation. When this item is going to play, it will notify
	 * all the StartStopListener objects available.
	 *
	 * @param rendererId
	 * @param incomingRenderer
	 *
	 * @see StartStopListener
	 */
	public void startPlaying(final String rendererId) {
		final String requestId = getRequestId(rendererId);
		synchronized (requestIdToRefcount) {
			Integer temp = requestIdToRefcount.get(requestId);
			if (temp == null) {
				temp = 0;
			}

			final Integer refCount = temp;
			requestIdToRefcount.put(requestId, refCount + 1);
			if (refCount == 0) {
				final StoreItem self = this;
				Runnable r = () -> {
					String rendererName = "unknown renderer";
					try {
						renderer.setPlayingRes(self);
						rendererName = renderer.getRendererName().replaceAll("\n", "");
					} catch (NullPointerException e) {
					}
					if (isLogPlayEvents()) {
						LOGGER.info("Started playing {} ({}) on your {}", getName(), getEngineName(), rendererName);
						LOGGER.debug(
								"The full filename of which is: " + getFileName() + " and the address of the renderer is: " + rendererId);
					}
					lastStartSystemTime = System.currentTimeMillis();
				};
				new Thread(r, "StartPlaying Event").start();
			}
		}
	}

	/**
	 * Plugin implementation. When this item is going to stop playing, it will
	 * notify all the StartStopListener objects available.
	 *
	 * @see StartStopListener
	 */
	public void stopPlaying(final String rendererId) {
		final StoreResource self = this;
		final String requestId = getRequestId(rendererId);
		Runnable defer = () -> {
			long start = lastStartSystemTime;
			if (isLogPlayEvents()) {
				LOGGER.trace("Stop playing {} on {} if no request under {} ms", getName(), renderer.getRendererName(), STOP_PLAYING_DELAY);
			}
			try {
				Thread.sleep(STOP_PLAYING_DELAY);
			} catch (InterruptedException e) {
				LOGGER.error("stopPlaying sleep interrupted", e);
				Thread.currentThread().interrupt();
			}

			synchronized (requestIdToRefcount) {
				final Integer refCount = requestIdToRefcount.get(requestId);
				assert refCount != null;
				assert refCount > 0;
				requestIdToRefcount.put(requestId, refCount - 1);
				if (start != lastStartSystemTime) {
					if (isLogPlayEvents()) {
						LOGGER.trace("Continue playing {} on {}", getName(), renderer.getRendererName());
					}
					return;
				}

				Runnable r = () -> {
					if (refCount == 1) {
						requestIdToRefcount.put(requestId, 0);
						String rendererName = renderer.getRendererName();
						// Reset only if another item hasn't already
						// begun playing
						if (renderer.getPlayingRes() == self) {
							renderer.setPlayingRes(null);
						}

						if (isLogPlayEvents()) {
							LOGGER.info("Stopped playing {} on {}", getName(), rendererName);
							LOGGER.debug("The full filename of which is \"{}\" and the address of the renderer is {}", getFileName(),
									rendererId);
						}
						internalStop();
					}
				};
				new Thread(r, "StopPlaying Event").start();
			}
		};

		new Thread(defer, "StopPlaying Event Deferrer").start();
	}

	private String getRequestId(String rendererId) {
		return String.format("%s|%x|%s", rendererId, hashCode(), getSystemName());
	}

	/**
	 * @return The system time when the resource was last (re)started
	 */
	public long getLastStartSystemTime() {
		return lastStartSystemTime;
	}

	/**
	 * Sets the system time when the resource was last (re)started.
	 *
	 * @param startTime the system time to set
	 */
	public void setLastStartSystemTime(long startTime) {
		lastStartSystemTime = startTime;

		double fileDuration = 0;
		if (mediaInfo != null && (mediaInfo.isAudio() || mediaInfo.isVideo())) {
			fileDuration = mediaInfo.getDurationInSeconds();
		}

		/**
		 * Do not treat this as a legitimate playback attempt if the start time
		 * was within 2 seconds of the end of the video.
		 */
		if (fileDuration < 2.0 || lastStartPosition < (fileDuration - 2.0)) {
			lastStartSystemTimeUser = startTime;
		}
	}

	/**
	 * Gets the system time when the resource was last (re)started.
	 *
	 * The system time when the resource was last (re)started by a user. This is
	 * a guess, where we disqualify certain playback requests from setting this
	 * value based on how close they were to the end, because some renderers
	 * request the last bytes of a file for processing behind the scenes, and
	 * that does not count as a real user doing it.
	 *
	 * @return The system time when the resource was last (re)started by a user.
	 */
	public long getLastStartSystemTimeUser() {
		return lastStartSystemTimeUser;
	}

	/**
	 * Gets the most recently requested time offset in seconds.
	 *
	 * @return The most recently requested time offset in seconds
	 */
	public double getLastStartPosition() {
		return lastStartPosition;
	}

	public abstract InputStream getInputStream() throws IOException;

	/**
	 * Returns an InputStream of this StoreItem that starts at a given
	 * time, if possible. Very useful if video chapters are being used.
	 *
	 * @param range
	 * @return The inputstream
	 * @throws IOException
	 */
	public InputStream getInputStream(Range range) throws IOException {
		return getInputStream(range, null);
	}

	/**
	 * Returns an InputStream of this StoreItem that starts at a given
	 * time, if possible. Very useful if video chapters are being used.
	 *
	 * @param range
	 * @param hlsConfiguration
	 * @return The inputstream
	 * @throws IOException
	 */
	public synchronized InputStream getInputStream(Range range, HlsHelper.HlsConfiguration hlsConfiguration) throws IOException {
		// Use device-specific UMS conf, if any
		LOGGER.trace("Asked stream chunk : " + range + " of " + getName() + " and engine " + getTranscodingSettings());

		// shagrath: small fix, regression on chapters
		boolean timeseekAuto = false;
		// Ditlew - WDTV Live
		// Ditlew - We convert byteoffset to timeoffset here. This needs the
		// stream to be CBR!
		int cbrVideoBitrate = renderer.getCBRVideoBitrate();
		long low = (range instanceof ByteRange byteRange) ? byteRange.getStartOrZero() : 0;
		long high = (range instanceof ByteRange byteRange && range.isEndLimitAvailable()) ? (long) byteRange.getEnd() : -1;
		TimeRange timeRange = range.createTimeRange();
		if (isTranscoded() && low > 0 && cbrVideoBitrate > 0) {
			int usedBitRated = (int) ((cbrVideoBitrate + 256) * 1024 / (double) 8 * CONTAINER_OVERHEAD);
			if (low > usedBitRated) {
				timeRange.setStart(low / (double) (usedBitRated));
				low = 0;

				// WDTV Live - if set to TS it asks multiple times and ends by
				// asking for an invalid offset which kills MEncoder
				if (timeRange.getStartOrZero() > mediaInfo.getDurationInSeconds()) {
					return null;
				}

				// Should we rewind a little (in case our overhead isn't
				// accurate enough)
				int rewindSecs = renderer.getByteToTimeseekRewindSeconds();
				timeRange.rewindStart(rewindSecs);

				// shagrath:
				timeseekAuto = true;
			}
		}

		if (low > 0 && mediaInfo.getBitRate() > 0) {
			lastStartPosition = (low * 8) / (double) mediaInfo.getBitRate();
			LOGGER.trace("Estimating seek position from byte range:");
			LOGGER.trace("   media.getBitrate: " + mediaInfo.getBitRate());
			LOGGER.trace("   low: " + low);
			LOGGER.trace("   lastStartPosition: " + lastStartPosition);
		} else {
			lastStartPosition = timeRange.getStartOrZero();
			LOGGER.trace("Setting lastStartPosition from time-seeking: " + lastStartPosition);
		}

		// Determine source of the stream
		if (!isTranscoded() && !isResume()) {
			// No transcoding
			if (this instanceof IPushOutput iPushOutput) {
				PipedOutputStream out = new PipedOutputStream();
				InputStream fis = new PipedInputStream(out);
				iPushOutput.push(out);

				if (low > 0) {
					fis.skip(low);
				}

				setLastStartSystemTime(System.currentTimeMillis());
				return wrap(fis, high, low);
			}

			InputStream fis = getInputStream();

			if (fis != null) {
				if (low > 0) {
					fis.skip(low);
				}

				fis = wrap(fis, high, low);
				if (timeRange.getStartOrZero() > 0 && this instanceof RealFile) {
					fis.skip(MpegUtil.getPositionForTimeInMpeg(((RealFile) this).getFile(), (int) timeRange.getStartOrZero()));
				}
			}

			setLastStartSystemTime(System.currentTimeMillis());
			return fis;
		}

		// Pipe transcoding result
		OutputParams params = new OutputParams(renderer.getUmsConfiguration());
		params.setAid(mediaAudio);
		params.setSid(mediaSubtitle);
		params.setMediaRenderer(renderer);
		timeRange.limit(getSplitRange());
		params.setTimeSeek(timeRange.getStartOrZero());
		params.setTimeEnd(timeRange.getEndOrZero());
		params.setShiftScr(timeseekAuto);
		params.setHlsConfiguration(hlsConfiguration);
		if (this instanceof IPushOutput iPushOutput) {
			params.setStdIn(iPushOutput);
		}

		if (resume != null) {
			if (range instanceof TimeRange tRange) {
				resume.update(tRange, this);
			}

			params.setTimeSeek(resume.getTimeOffset() / 1000);
			if (!isTranscoded()) {
				setTranscodingSettings(TranscodingSettings.getBestTranscodingSettings(this));
			}
		}

		if (System.currentTimeMillis() - lastStartSystemTime < 500) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.error(null, e);
				Thread.currentThread().interrupt();
			}
		}

		// (Re)start transcoding process if necessary
		if (externalProcess == null || externalProcess.isDestroyed() || hlsConfiguration != null) {
			// First playback attempt => start new transcoding process
			LOGGER.debug("Starting transcode/remux of " + getName() + " with media info: " + mediaInfo);
			setLastStartSystemTime(System.currentTimeMillis());

			if (params.getTimeSeek() > 0) {
				// This must be a resume - so need to set lastTimeSeek to avoid a restart of the process
				// from a new seek request to the same resume point
				LOGGER.debug("Setting last time seek (from resume) to: " + params.getTimeSeek() + " seconds");
				lastTimeSeek = params.getTimeSeek();
			}

			externalProcess = getTranscodingSettings().getEngine().launchTranscode(this, mediaInfo, params);
			if (params.getWaitBeforeStart() > 0) {
				LOGGER.trace("Sleeping for {} milliseconds", params.getWaitBeforeStart());
				try {
					Thread.sleep(params.getWaitBeforeStart());
				} catch (InterruptedException e) {
					LOGGER.error(null, e);
					Thread.currentThread().interrupt();
				}

				LOGGER.trace("Finished sleeping for " + params.getWaitBeforeStart() + " milliseconds");
			}
		} else if (params.getTimeSeek() > 0 && mediaInfo != null && mediaInfo.isMediaParsed() && mediaInfo.getDurationInSeconds() > 0) {

			// Time seek request => stop running transcode process and start a new one
			LOGGER.debug("Requesting time seek: " + params.getTimeSeek() + " seconds");

			if (lastTimeSeek == params.getTimeSeek()) {
				LOGGER.debug("Duplicate time seek request: " + params.getTimeSeek() + " seconds, ignoring");
			} else {

				LOGGER.debug("Setting last time seek to: " + params.getTimeSeek() + " seconds");
				lastTimeSeek = params.getTimeSeek();

				params.setMinBufferSize(1);

				Runnable r = () -> {
					externalProcess.stopProcess();
				};

				new Thread(r, "External Process Stopper").start();

				setLastStartSystemTime(System.currentTimeMillis());
				ProcessWrapper newExternalProcess = getTranscodingSettings().getEngine().launchTranscode(this, mediaInfo, params);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error(null, e);
					Thread.currentThread().interrupt();
				}

				if (newExternalProcess == null) {
					LOGGER.trace("External process instance is null... sounds not good");
				}

				externalProcess = newExternalProcess;
			}
		}

		if (externalProcess == null) {
			return null;
		}

		InputStream is = null;
		int timer = 0;
		while (is == null && timer < 10) {
			is = externalProcess.getInputStream(low);
			timer++;
			if (is == null) {
				LOGGER.debug("External input stream instance is null... sounds not good, waiting 500ms");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		// fail fast: don't leave a process running indefinitely if it's
		// not producing output after params.waitbeforestart milliseconds + 5
		// seconds
		// this cleans up lingering MEncoder web video transcode processes that
		// hang
		// instead of exiting
		if (is == null && !externalProcess.isDestroyed()) {
			Runnable r = () -> {
				LOGGER.error("External input stream instance is null... stopping process");
				externalProcess.stopProcess();
			};

			new Thread(r, "Hanging External Process Stopper").start();
		}

		return is;
	}

	/**
	 * Wrap an {@link InputStream} in a {@link SizeLimitInputStream} that sets a
	 * limit to the maximum number of bytes to be read from the original input
	 * stream. The number of bytes is determined by the high and low value
	 * (bytes = high - low). If the high value is less than the low value, the
	 * input stream is not wrapped and returned as is.
	 *
	 * @param input The input stream to wrap.
	 * @param high The high value.
	 * @param low The low value.
	 * @return The resulting input stream.
	 */
	public static InputStream wrap(InputStream input, long high, long low) {
		if (input != null && high > low) {
			long bytes = (high - (low < 0 ? 0 : low)) + 1;
			LOGGER.trace("Using size-limiting stream (" + bytes + " bytes)");
			return new SizeLimitInputStream(input, bytes);
		}

		return input;
	}

	public String getMimeType() {
		return getMimeType(getTranscodingSettings());
	}

	private String getMimeType(TranscodingSettings transcodingSettings) {
		if (transcodingSettings != null) {
			// Engines like FFmpegVideo can define placeholder MIME types like
			// video/transcode to be replaced later
			return transcodingSettings.getMimeType(this);
		} else if (mediaInfo != null && mediaInfo.isMediaParsed()) {
			return getPreferredMimeType();
		} else if (getFormat() != null) {
			return getFormat().mimeType();
		} else {
			return HTTPResource.getDefaultMimeType(getSpecificType());
		}
	}

	/**
	 * Get the mimetype for this resource according to the renderer's supported
	 * preferences, if any.
	 *
	 * @return The mimetype renderer supported preferences
	 */
	private String getPreferredMimeType() {
		if (mediaInfo != null && renderer.isUseMediaInfo() && (format == null || !format.isImage())) {
			// See which MIME type the renderer prefers in case it supports the
			// mediaInfo
			String preferred = renderer.getFormatConfiguration().getMatchedMIMEtype(this, renderer);
			if (preferred != null && !FormatConfiguration.MIMETYPE_AUTO.equals(preferred)) {
				// Use the renderer's preferred MIME type for this file
				LOGGER.trace("File \"{}\" will be sent with MIME type \"{}\"", getName(), preferred);
				return preferred;
			}
		}
		return mediaInfo != null ? mediaInfo.getMimeType() : null;
	}

	/**
	 * Prototype for returning URLs.
	 *
	 * @return a URL for a given mediaInfo item.
	 */
	public String getMediaURL() {
		return getMediaURL("");
	}

	/**
	 * @param prefix
	 * @return Returns a URL for a given mediaInfo item.
	 */
	public String getMediaURL(String prefix) {
		return getMediaURL(prefix, false);
	}

	public String getMediaURL(String prefix, boolean useSystemName) {
		return getMediaURL(prefix, useSystemName, true);
	}

	private String getMediaURL(String prefix, boolean useSystemName, boolean urlEncode) {
		StringBuilder sb = MediaServerRequest.getServerMediaURL(renderer.getUUID(), getResourceId());
		String uri = useSystemName ? getSystemName() : getName();
		sb.append(prefix);
		sb.append(urlEncode ? encode(uri) : uri);
		return sb.toString();
	}

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////
	private void internalStop() {
		StoreResource res = resumeStop();
		final MediaStore mediaStore = ((renderer != null) ? renderer.getMediaStore() : null);
		if (mediaStore != null) {
			if (res == null) {
				res = this.clone();
			} else {
				res = res.clone();
			}

			mediaStore.stopPlaying(res);
		}
	}

	public int resumeHash() {
		return resHash;
	}

	public void setResumeHash(int resHash) {
		this.resHash = resHash;
	}

	public ResumeObj getResume() {
		return resume;
	}

	public void setResume(ResumeObj r) {
		resume = r;
	}

	protected boolean isResumeable() {
		if (format != null) {
			// Only resume videos
			return format.isVideo();
		}

		return true;
	}

	private StoreResource resumeStop() {
		if (!CONFIGURATION.isResumeEnabled() || !isResumeable()) {
			return null;
		}

		notifyRefresh();
		if (resume != null) {
			resume.stop(lastStartSystemTime, (long) (mediaInfo.getDurationInSeconds() * 1000));
			if (resume.isDone()) {
				getParent().removeChild(this);
			} else if (getMediaInfo() != null) {
				mediaInfo.setThumbnailId(null);
				mediaInfo.setThumbnailSource(ThumbnailSource.UNKNOWN);
			}
		} else {
			for (StoreResource res : getParent().getChildren()) {
				if (res instanceof StoreItem item && item.isResume() && item.getName().equals(getName())) {
					item.resume.stop(lastStartSystemTime, (long) (mediaInfo.getDurationInSeconds() * 1000));
					if (item.resume.isDone()) {
						getParent().removeChild(res);
						return null;
					}

					if (res.getMediaInfo() != null) {
						res.mediaInfo.setThumbnailId(null);
						res.mediaInfo.setThumbnailSource(ThumbnailSource.UNKNOWN);
					}

					return res;
				}
			}

			ResumeObj r = ResumeObj.store(this, lastStartSystemTime);
			if (r != null) {
				StoreItem clone = this.clone();
				clone.resume = r;
				clone.resHash = resHash;
				if (clone.mediaInfo != null) {
					clone.mediaInfo.setThumbnailId(null);
					clone.mediaInfo.setThumbnailSource(ThumbnailSource.UNKNOWN);
				}

				clone.transcodingSettings = transcodingSettings;
				getParent().addChildInternal(clone);
				return clone;
			}
		}

		return null;
	}

	public final boolean isResume() {
		return resume != null && isResumeable();
	}

	public int minPlayTime() {
		return CONFIGURATION.getMinimumWatchedPlayTime();
	}

	public String resumeStr(String s) {
		if (isResume()) {
			return Messages.getString("Resume") + ": " + s;
		}
		return s;
	}

	public String resumeName() {
		return resumeStr(getDisplayName());
	}

	public String getLocalizedResumeName(String lang) {
		return resumeStr(getLocalizedDisplayName(lang));
	}

	public String getResolutionForKeepAR(int scaleWidth, int scaleHeight) {
		double videoAspectRatio = (double) scaleWidth / (double) scaleHeight;
		double rendererAspectRatio = 1.777777777777778;
		if (videoAspectRatio > rendererAspectRatio) {
			scaleHeight = (int) Math.round(scaleWidth / rendererAspectRatio);
		} else {
			scaleWidth = (int) Math.round(scaleHeight * rendererAspectRatio);
		}

		scaleWidth = Engine.convertToModX(scaleWidth, 4);
		scaleHeight = Engine.convertToModX(scaleHeight, 4);
		return scaleWidth + "x" + scaleHeight;
	}

	/**
	 * Returns the engine part of the display name or {@code null} if none
	 * should be displayed.
	 *
	 * @return The engine display name or {@code null}.
	 */
	protected String getDisplayNameEngine() {
		if (isNoName() ||
			(isTranscoded() && !CONFIGURATION.isHideEngineNames())) {
			return "[" + getEngineName() + "]";
		}
		return null;
	}

	/**
	 * Returns the suffix part of the display name or an empty {@link String} if
	 * none should be displayed.
	 *
	 * @return The display name suffix or {@code ""}.
	 */
	@Override
	public String getDisplayNameSuffix() {
		if (mediaInfo == null) {
			return null;
		}
		MediaType mediaType = mediaInfo.getMediaType();
		switch (mediaType) {
			case VIDEO:
				StringBuilder nameSuffixBuilder = new StringBuilder();
				boolean subsAreValidForStreaming = mediaSubtitle != null && mediaSubtitle.isExternal() &&
						(!isTranscoded() || renderer.streamSubsForTranscodedVideo()) &&
						renderer.isExternalSubtitlesFormatSupported(mediaSubtitle, this);

				if (mediaAudio != null) {
					String audioLanguage = mediaAudio.getLang();
					if (audioLanguage == null || MediaLang.UND.equals(audioLanguage.toLowerCase(Locale.ROOT))) {
						audioLanguage = "";
					} else {
						audioLanguage = Iso639.getFirstName(audioLanguage);
						audioLanguage = audioLanguage == null ? "" : "/" + audioLanguage;
					}

					String audioTrackTitle = "";
					if (mediaAudio.getTitle() != null && !"".equals(mediaAudio.getTitle()) &&
							renderer.isShowAudioMetadata()) {
						audioTrackTitle = " (" + mediaAudio.getTitle() + ")";
					}

					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{Audio: ").append(mediaAudio.getAudioCodec()).append(audioLanguage).append(audioTrackTitle)
							.append("}");
				}

				UmsConfiguration.SubtitlesInfoLevel subsInfoLevel;
				if (getParent() instanceof ChapterFileTranscodeVirtualFolder) {
					subsInfoLevel = UmsConfiguration.SubtitlesInfoLevel.NONE;
				} else if (isInsideTranscodeFolder()) {
					subsInfoLevel = UmsConfiguration.SubtitlesInfoLevel.FULL;
				} else {
					subsInfoLevel = CONFIGURATION.getSubtitlesInfoLevel();
				}
				if (mediaSubtitle != null && mediaSubtitle.getId() != MediaLang.DUMMY_ID && subsInfoLevel != UmsConfiguration.SubtitlesInfoLevel.NONE) {
					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{");
					String subtitleLanguage = mediaSubtitle.getLangFullName();
					if (subsInfoLevel == UmsConfiguration.SubtitlesInfoLevel.BASIC) {
						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Unknown"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}
						nameSuffixBuilder.append(" ").append(Messages.getString("Subtitles_lowercase"));
					} else if (subsInfoLevel == UmsConfiguration.SubtitlesInfoLevel.FULL) {
						if (subsAreValidForStreaming) {
							nameSuffixBuilder.append(Messages.getString("Stream")).append(" ");
						}

						if (mediaSubtitle.isExternal()) {
							nameSuffixBuilder.append(Messages.getString("External_abbr")).append(" ");
						} else if (mediaSubtitle.isEmbedded()) {
							nameSuffixBuilder.append(Messages.getString("Internal_abbr")).append(" ");
						}
						nameSuffixBuilder.append(Messages.getString("Sub"));
						nameSuffixBuilder.append(mediaSubtitle.getType().getShortName()).append("/");

						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Unknown_abbr"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}

						if (mediaSubtitle.getTitle() != null &&
								StringUtils.isNotBlank(mediaSubtitle.getTitle()) && renderer.isShowSubMetadata()) {
							nameSuffixBuilder.append(" (").append(mediaSubtitle.getTitle()).append(")");
						}
					}
					nameSuffixBuilder.append("}");
				}
				return nameSuffixBuilder.toString();
			case AUDIO:
			case IMAGE:
			case UNKNOWN:
			default:
				return null;
		}
	}

	/**
	 * Checks if a thumbnail exists, and, if not, generates one (if possible).
	 *
	 * @param inputFile File to check or generate the thumbnail for.
	 */
	protected void checkThumbnail(InputFile inputFile) {
		// Use device-specific conf, if any
		if (mediaInfo != null &&
				!mediaInfo.isThumbnailReady() &&
				renderer.getUmsConfiguration().isThumbnailGenerationEnabled() &&
				renderer.isThumbnails()) {
			Double seekPosition = null;
			boolean isResume = isResume();
			if (isResume) {
				Double resumePosition = resume.getTimeOffset() / 1000d;

				if (mediaInfo.getDurationInSeconds() > 0 && resumePosition < mediaInfo.getDurationInSeconds()) {
					seekPosition = resumePosition;
				} else {
					seekPosition = (double) renderer.getUmsConfiguration().getThumbnailSeekPos();
				}
			}

			DLNAThumbnail thumbnail = Parser.getThumbnail(mediaInfo, inputFile, getFormat(), getType(), seekPosition);
			if (thumbnail != null) {
				if (!isResume && mediaInfo.getFileId() != null) {
					mediaInfo.setThumbnailId(ThumbnailStore.getId(thumbnail, mediaInfo.getFileId(), mediaInfo.getThumbnailSource()));
				} else {
					mediaInfo.setThumbnailId(ThumbnailStore.getTempId(thumbnail));
				}
			}
		}
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail, which is
	 * the first of:
	 * <li>its Format icon, if any
	 * <li>the fallback image, if any
	 * <li>the {@link GenericIcons} icon <br>
	 * <br>
	 *
	 * @param fallback the fallback image, or {@code null}.
	 *
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	@Override
	protected DLNAThumbnailInputStream getGenericThumbnailInputStreamInternal(String fallback) throws IOException {
		String thumb = fallback;
		if (format != null && format.getIcon() != null) {
			thumb = format.getIcon();
		}

		// Thumb could be:
		if (thumb != null && isCodeValid(this)) {
			// A local file
			if (new File(thumb).exists()) {
				FileInputStream inputStream = new FileInputStream(thumb);
				return DLNAThumbnailInputStream.toThumbnailInputStream(inputStream);
			}

			// A jar resource
			InputStream is = getResourceInputStream(thumb);
			if (is != null) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(is);
			}

			// A URL
			try {
				return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.downloadAndSend(thumb, true));
			} catch (IOException e) {
				//dowwnload fail
			}
		}

		return super.getGenericThumbnailInputStreamInternal(fallback);
	}

	/**
	 * Adds an audio "flag" filter to the specified
	 * {@link BufferedImageFilterChain}. If {@code filterChain} is {@code null}
	 * and a "flag" filter is added, a new {@link BufferedImageFilterChain} is
	 * created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	public BufferedImageFilterChain addAudioFlagFilter(BufferedImageFilterChain filterChain) {
		String audioLanguageCode = mediaAudio != null ? mediaAudio.getLang() : null;
		if (StringUtils.isNotBlank(audioLanguageCode)) {
			if (filterChain == null) {
				filterChain = new BufferedImageFilterChain();
			}
			filterChain.add(new ImagesUtil.AudioFlagFilter(audioLanguageCode, THUMBNAIL_HINTS));
		}
		return filterChain;
	}

	/**
	 * Adds a subtitles "flag" filter to the specified
	 * {@link BufferedImageFilterChain}. If {@code filterChain} is {@code null}
	 * and a "flag" filter is added, a new {@link BufferedImageFilterChain} is
	 * created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	public BufferedImageFilterChain addSubtitlesFlagFilter(BufferedImageFilterChain filterChain) {
		String subsLanguageCode = mediaSubtitle != null && mediaSubtitle.getId() != MediaLang.DUMMY_ID ? mediaSubtitle.getLang() : null;

		if (StringUtils.isNotBlank(subsLanguageCode)) {
			if (filterChain == null) {
				filterChain = new BufferedImageFilterChain();
			}
			filterChain.add(new ImagesUtil.SubtitlesFlagFilter(subsLanguageCode, THUMBNAIL_HINTS));
		}
		return filterChain;
	}

	/**
	 * Adds audio and subtitles "flag" filters to the specified
	 * {@link BufferedImageFilterChain} if they should be applied. If
	 * {@code filterChain} is {@code null} and a "flag" filter is added, a new
	 * {@link BufferedImageFilterChain} is created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	@Override
	public BufferedImageFilterChain addFlagFilters(BufferedImageFilterChain filterChain) {
		// Show audio and subtitles language flags in the TRANSCODE folder only
		// for video files
		if (isInsideTranscodeFolder() &&
				(mediaAudio != null || mediaSubtitle != null) &&
				((mediaInfo != null && mediaInfo.isVideo()) || (mediaInfo == null && format != null && format.isVideo()))) {
			filterChain = addAudioFlagFilter(filterChain);
			filterChain = addSubtitlesFlagFilter(filterChain);
		}
		return filterChain;
	}

	@Override
	public synchronized void syncResolve() {
		resolve();
		if (mediaInfo != null && mediaInfo.isVideo()) {
			registerExternalSubtitles(false);
		}
	}

	/**
	 * Scans for and registers external subtitles if this is a video resource.
	 * Cached information will be used if it exists unless {@code forceRefresh}
	 * is {@code true}, in which case a new scan will always be done. This also
	 * sets the cached subtitles information for this resource after
	 * registration.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 * subtitles instead of relying on cached information (if it exists).
	 */
	public void registerExternalSubtitles(boolean forceRefresh) {
		if (mediaInfo == null || !mediaInfo.isVideo() || this instanceof VirtualVideoAction) {
			return;
		}

		synchronized (subtitlesLock) {
			if (!forceRefresh && isExternalSubtitlesParsed) {
				return;
			}

			File file = this instanceof RealFile ? ((RealFile) this).getFile() : new File(getFileName());
			if (file == null || mediaInfo == null || FileUtil.isUrl(getFileName())) {
				isExternalSubtitlesParsed = true;
				return;
			}

			if (!renderer.getUmsConfiguration().isDisableSubtitles() && renderer.getUmsConfiguration().isAutoloadExternalSubtitles()) {
				boolean changed = SubtitleUtils.searchAndAttachExternalSubtitles(file, mediaInfo, forceRefresh);
				// update the database if enabled
				if (changed && mediaInfo.isMediaParsed() && !mediaInfo.isParsing()) {
					Connection connection = null;
					try {
						connection = MediaDatabase.getConnectionIfAvailable();
						if (connection != null) {
							//handle autocommit
							boolean currentAutoCommit = connection.getAutoCommit();
							if (currentAutoCommit) {
								connection.setAutoCommit(false);
							}
							MediaTableSubtracks.insertOrUpdateSubtitleTracks(connection, mediaInfo.getFileId(), mediaInfo);
							if (currentAutoCommit) {
								connection.commit();
								connection.setAutoCommit(true);
							}
						}
					} catch (SQLException e) {
						LOGGER.error("Database error while trying to add parsed information for \"{}\" to the cache: {}", file,
								e.getMessage());
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("SQL error code: {}", e.getErrorCode());
							if (e.getCause() instanceof SQLException &&
									((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()) {
								LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
							}
							LOGGER.trace("", e);
						}
					} finally {
						MediaDatabase.close(connection);
					}
				}
			}

			List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
			if (subtitlesList != null) {
				hasSubtitles = !subtitlesList.isEmpty();
				for (MediaSubtitle subtitles : subtitlesList) {
					if (subtitles.isExternal()) {
						hasExternalSubtitles = true;
						break;
					}
				}
			}

			isExternalSubtitlesParsed = true;
		}
	}

	/**
	 * Sets external subtitles parsed status to true and sets
	 * {@link #hasSubtitles} and {@link #hasExternalSubtitles} according to the
	 * existing {@link MediaSubtitle} instances.
	 * <p>
	 * <b>WARNING:</b> This should only be called when the subtitles tracks has
	 * been populated by an alternative source like the database. Setting this
	 * under other circumstances will break the implemented automatic parsing
	 * and caching.
	 */
	public void setExternalSubtitlesParsed() {
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return;
		}

		synchronized (subtitlesLock) {
			if (isExternalSubtitlesParsed) {
				return;
			}

			List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
			hasSubtitles = !subtitlesList.isEmpty();
			for (MediaSubtitle subtitles : subtitlesList) {
				if (subtitles.isExternal()) {
					hasExternalSubtitles = true;
					break;
				}
			}
			isExternalSubtitlesParsed = true;
		}
	}

	/**
	 * Resets the cached subtitles information. Mostly useful for cloned
	 * resources.
	 */
	public void resetSubtitlesStatus() {
		synchronized (subtitlesLock) {
			isExternalSubtitlesParsed = false;
			hasExternalSubtitles = false;
			hasSubtitles = false;
		}
	}

	/**
	 * This method figures out which audio track should be used based on
	 * {@link MediaInfo} metadata and configuration settings.
	 *
	 * @return The resolved {@link MediaAudio} or {@code null}.
	 */
	public MediaAudio resolveAudioStream() {
		if (mediaInfo == null || mediaInfo.getAudioTrackCount() == 0) {
			LOGGER.trace("Found no audio track");
			return null;
		}

		// check for preferred audio
		MediaAudio dtsTrack = null;
		StringTokenizer st = new StringTokenizer(renderer.getUmsConfiguration().getAudioLanguages(), ",");
		while (st.hasMoreTokens()) {
			String lang = st.nextToken().trim();
			LOGGER.trace("Looking for an audio track with language \"{}\" for \"{}\"", lang, getName());
			for (MediaAudio audio : mediaInfo.getAudioTracks()) {
				if (audio.matchCode(lang)) {
					LOGGER.trace("Matched audio track: {}", audio);
					return audio;
				}

				if (dtsTrack == null && audio.isDTS()) {
					dtsTrack = audio;
				}
			}
		}

		// preferred audio not found, take a default audio track, dts first if
		// available
		if (dtsTrack != null) {
			LOGGER.trace("Preferring DTS audio track since no language match was found: {}", dtsTrack);
			return dtsTrack;
		}
		MediaAudio result = mediaInfo.getDefaultAudioTrack();
		LOGGER.trace("Using the first available audio track: {}", result);
		return result;
	}

	/**
	 * This method figures out which subtitles track should be used based on
	 * {@link MediaInfo} metadata, chosen audio language and configuration
	 * settings.
	 *
	 * @param audioLanguage the {@code ISO 639} language code for the chosen
	 * audio language or {@code null} if it doesn't apply.
	 * @param forceRefresh if {@code true} forces a new scan for external
	 * subtitles instead of relying on cached information (if it exists).
	 * @return The resolved {@link MediaSubtitle} or {@code null}.
	 */
	public MediaSubtitle resolveSubtitlesStream(String audioLanguage, boolean forceRefresh) {
		if (mediaInfo == null) {
			return null;
		}

		// Use device-specific conf
		if (renderer.getUmsConfiguration().isDisableSubtitles()) {
			LOGGER.trace("Not resolving subtitles since subtitles are disabled");
			return null;
		}

		if (!hasSubtitles(forceRefresh)) {
			return null;
		}

		/*
		 * Check for external and internal subtitles matching the user's
		 * language preferences
		 */
		MediaSubtitle matchedSub;
		boolean useExternal = renderer.getUmsConfiguration().isAutoloadExternalSubtitles();
		boolean forceExternal = renderer.getUmsConfiguration().isForceExternalSubtitles();
		String audioSubLanguages = renderer.getUmsConfiguration().getAudioSubLanguages();

		if (forceExternal) {
			matchedSub = getHighestPriorityExternalSubtitles();
			if (matchedSub == null) {
				LOGGER.trace("No external subtitles candidates were found to force for \"{}\"", getName());
			} else {
				LOGGER.trace("Forcing external subtitles track for \"{}\": {}", getName(), matchedSub);
				return matchedSub;
			}
		}

		if (StringUtils.isBlank(audioLanguage) || StringUtils.isBlank(audioSubLanguages)) {
			// Not enough information to do a full audio/subtitles combination
			// search, only use the preferred subtitles
			LOGGER.trace("Searching for subtitles without considering audio language for \"{}\"", getName());
			ArrayList<MediaSubtitle> candidates = new ArrayList<>();
			for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
				if (subtitles.isExternal()) {
					if (useExternal) {
						candidates.add(subtitles);
					}
				} else {
					candidates.add(subtitles);
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, false);
				if (matchedSub != null) {
					LOGGER.trace("Matched {} subtitles track for \"{}\" with unknown audio language: {}",
							matchedSub.isExternal() ? "external" : "internal", getName(), matchedSub);
					return matchedSub;
				}
			}
		} else {
			// Do a full audio/subtitles combination search
			StringTokenizer st = new StringTokenizer(audioSubLanguages.toLowerCase(Locale.ROOT), ";");
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				int commaPos = pair.indexOf(',');
				if (commaPos > -1) {
					String audio = pair.substring(0, commaPos).trim();
					String sub = pair.substring(commaPos + 1).trim();
					LOGGER.trace("Searching for a match for audio language \"{}\" with audio \"{}\" and subtitles \"{}\" for \"{}\"",
							audioLanguage, audio, sub, getName());

					if ("*".equals(audio) || MediaLang.UND.equals(audio) || Iso639.isCodesMatching(audio, audioLanguage)) {
						boolean anyLanguage = "*".equals(sub) || MediaLang.UND.equals(sub);
						if ("off".equals(sub)) {
							LOGGER.trace("Not looking for non-forced subtitles since they are \"off\" for audio language \"{}\"", audio);
							break;
						} else {
							ArrayList<MediaSubtitle> candidates = new ArrayList<>();
							for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
								if (anyLanguage || subtitles.matchCode(sub)) {
									if (subtitles.isEmbedded()) {
										candidates.add(subtitles);
										LOGGER.trace("Adding internal subtitles candidate: {}", subtitles);
									} else if (useExternal) {
										candidates.add(subtitles);
										LOGGER.trace("Adding external subtitles candidate: {}", subtitles);
									} else {
										LOGGER.trace(
												"Ignoring external subtitles because auto loading of external subtitles is disabled: {}",
												subtitles);
									}
								}
							}
							if (!candidates.isEmpty()) {
								matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, !anyLanguage);
								if (matchedSub != null) {
									LOGGER.trace("Matched {} subtitles track for \"{}\": {}",
											matchedSub.isExternal() ? "external" : "internal", getName(), matchedSub);
									return matchedSub;
								}
							}
						}
					}
				} else {
					LOGGER.warn("Ignoring invalid audio/subtitle language configuration \"{}\"", pair);
				}
			}
		}

		// Check for forced subtitles.
		String forcedTags = renderer.getUmsConfiguration().getForcedSubtitleTags();
		if (StringUtils.isNotBlank(forcedTags)) {
			Locale locale = PMS.getLocale();
			ArrayList<String> forcedTagsList = new ArrayList<>();
			for (String forcedTag : forcedTags.split(",")) {
				if (StringUtils.isNotBlank(forcedTag)) {
					forcedTagsList.add(forcedTag.trim().toLowerCase(locale));
				}
			}
			ArrayList<MediaSubtitle> candidates = new ArrayList<>();
			String forcedLanguage = renderer.getUmsConfiguration().getForcedSubtitleLanguage();
			boolean anyLanguage = StringUtils.isBlank(forcedLanguage) || "*".equals(forcedLanguage) || MediaLang.UND.equals(forcedLanguage);
			for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
				if (!useExternal && subtitles.isExternal()) {
					continue;
				}
				if (anyLanguage || Iso639.isCodesMatching(subtitles.getLang(), forcedLanguage)) {
					if (subtitles.isForced()) {
						candidates.add(subtitles);
						LOGGER.trace("Adding {} forced subtitles candidate that is flagged \"forced\": {}",
								subtitles.isExternal() ? "external" : "internal", subtitles);
					} else {
						//look for forcedTags in title
						String title = subtitles.getTitle();
						if (StringUtils.isNotBlank(title)) {
							title = title.toLowerCase(locale);
							for (String forcedTag : forcedTagsList) {
								if (title.contains(forcedTag)) {
									candidates.add(subtitles);
									LOGGER.trace("Adding {} forced subtitles candidate that matched tag \"{}\": {}",
											subtitles.isExternal() ? "external" : "internal", forcedTag, subtitles);
								}
							}
						}
					}
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, false);
				if (matchedSub != null) {
					LOGGER.trace("Using forced {} subtitles track for \"{}\": {}", matchedSub.isExternal() ? "external" : "internal",
							getName(), matchedSub);
					return matchedSub;
				}
			}
		}

		LOGGER.trace("Found no matching subtitle for \"{}\"", getName());
		return null;
	}

	private MediaSubtitle getHighestPriorityExternalSubtitles() {
		MediaSubtitle matchedSub = null;

		ArrayList<MediaSubtitle> candidates = new ArrayList<>();
		for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
			if (subtitles.isExternal()) {
				candidates.add(subtitles);
			}
		}

		// If external subtitles were found, let findPrioritizedSubtitles return
		// the right one
		if (!candidates.isEmpty()) {
			matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, true);
		}

		// Return either the matched external subtitles, or null if there was no
		// external match
		return matchedSub;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public StoreItem clone() {
		return (StoreItem) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=").append(getId());
		result.append(", name=").append(getName());
		result.append(", full path=");
		result.append(getResourceId());
		result.append(", format=").append(getFormat());
		if (getMediaAudio() != null) {
			result.append(", selected audio=[").append(getMediaAudio()).append("]");
		}
		if (getMediaSubtitle() != null) {
			result.append(", selected subtitles=[").append(getMediaSubtitle()).append("]");
		}
		result.append(", transcoding engine=[").append(getEngineName()).append("]");
		result.append(']');
		return result.toString();
	}

}
