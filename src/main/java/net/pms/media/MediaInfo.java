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
package net.pms.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.formats.audio.*;
import net.pms.image.ImageInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of media file metadata scanned by the MediaInfo library.
 */
public class MediaInfo implements Cloneable {

	public static final long ENDFILE_POS = 99999475712L;

	/**
	 * Containers that can represent audio or video media is by default
	 * considered to be video. This {@link Map} maps such containers to the type
	 * to use if they represent audio media.
	 */
	protected static final Map<String, AudioVariantInfo> AUDIO_OR_VIDEO_CONTAINERS = getAudioOrVideoContainers();

	private final AtomicBoolean parsing = new AtomicBoolean(false);

	// Stored in database
	private Long fileId;
	private String lastParser;
	private Double durationSec;
	private int bitrate;
	private long size;
	private Double frameRate;
	private String container;
	private String title;
	private String aspectRatioDvdIso;

	private Long thumbnailId = null;
	private ThumbnailSource thumbnailSource = ThumbnailSource.UNKNOWN;

	private MediaVideoMetadata videoMetadata;
	private MediaAudioMetadata audioMetadata;

	private int imageCount = 0;
	private volatile ImageInfo imageInfo = null;
	private MediaVideo defaultVideoTrack;
	private MediaAudio defaultAudioTrack;
	private List<MediaVideo> videoTracks = new ArrayList<>();
	private List<MediaAudio> audioTracks = new ArrayList<>();
	private List<MediaSubtitle> subtitleTracks = new ArrayList<>();
	private List<MediaChapter> chapters = new ArrayList<>();
	private String mimeType;

	/**
	 * Not stored in database.
	 */
	private Integer dvdtrack;
	private long lastExternalLookup = 0;

	public void resetParser() {
		this.lastParser = null;
		this.defaultAudioTrack = null;
		this.defaultVideoTrack = null;
		this.videoTracks.clear();
		this.audioTracks.clear();
		this.subtitleTracks.clear();
		this.chapters.clear();
		this.imageInfo = null;
		this.imageCount = 0;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long value) {
		fileId = value;
	}

	public int getVideoTrackCount() {
		return videoTracks.size();
	}

	public int getAudioTrackCount() {
		return audioTracks.size();
	}

	public int getSubtitleTrackCount() {
		return subtitleTracks.size();
	}

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int value) {
		imageCount = value;
	}

	public int getSubTrackCount() {
		return subtitleTracks.size();
	}

	public boolean isVideo() {
		return MediaType.VIDEO == getMediaType();
	}

	public boolean isAudio() {
		return MediaType.AUDIO == getMediaType();
	}

	public boolean hasAudio() {
		return defaultAudioTrack != null;
	}

	/**
	 * Determines whether this media "is" MPEG-4 SLS.
	 * <p>
	 * SLS is MPEG-4's hybrid lossless audio codec. It uses a standard MPEG-4 GA
	 * core layer. Valid cores include AAC-LC, AAC Scalable (without LTP), ER
	 * AAC LC, ER AAC Scalable, and ER BSAC.
	 * <p>
	 * Since UMS currently only implements AAC-LC among the valid core layer
	 * codecs, AAC-LC is the only core layer format "approved" by this test. If
	 * further codecs are added in the future, this test should be modified
	 * accordingly.
	 *
	 * @return {@code true} is this {@link MediaInfo} instance has two audio
	 *         tracks where the first has codec AAC-LC and the second has codec
	 *         SLS, {@code false} otherwise.
	 */
	public boolean isSLS() {
		return
			audioTracks.size() == 2 &&
			(
				audioTracks.get(0).isAACLC() ||
				audioTracks.get(0).isERBSAC()
			) &&
			audioTracks.get(1).isSLS();
	}

	public MediaType getMediaType() {
		if (defaultVideoTrack != null) {
			return MediaType.VIDEO;
		}
		int audioTracksSize = audioTracks.size();
		if (audioTracksSize == 0 && imageCount > 0) {
			return MediaType.IMAGE;
		} else if (audioTracksSize == 1 || isSLS()) {
			return MediaType.AUDIO;
		} else {
			return MediaType.UNKNOWN;
		}
	}

	public boolean isImage() {
		return MediaType.IMAGE == getMediaType();
	}

	/**
	 * Ensure media is not parsing.
	 */
	public boolean waitMediaParsing(int seconds) {
		int i = 0;
		while (isParsing()) {
			if (i == seconds) {
				return false;
			}
			UMSUtils.sleep(1000);
			i++;
		}
		return true;
	}

	public int getFrameNumbers() {
		return (int) (getDurationInSeconds() * frameRate);
	}

	public void setDuration(Double d) {
		this.durationSec = d;
	}

	/**
	 * This is the object {@link Double} and might return <code>null</code>.
	 *
	 * To get <code>0</code> instead of <code>null</code>, use
	 * {@link #getDurationInSeconds()}
	 *
	 * @return duration
	 */
	public Double getDuration() {
		return durationSec;
	}

	/**
	 * @return 0 if nothing is specified, otherwise the duration
	 */
	public double getDurationInSeconds() {
		return Optional.ofNullable(durationSec).orElse(0D);
	}

	public String getDurationString() {
		return durationSec != null ? StringUtil.formatDLNADuration(durationSec) : null;
	}

	/**
	 * @return the bitrate
	 * @since 1.50.0
	 */
	public int getBitRate() {
		return bitrate;
	}

	/**
	 * @param bitrate the bitrate to set
	 * @since 1.50.0
	 */
	public void setBitRate(int bitrate) {
		this.bitrate = bitrate;
	}

	public int getRealVideoBitrate() {
		if (bitrate > 0) {
			return (bitrate / 8);
		}

		int realBitrate = 10000000;

		if (getDurationInSeconds() > 0) {
			realBitrate = (int) (size / getDurationInSeconds());
		}

		return realBitrate;
	}

	/**
	 * @return the size
	 * @since 1.50.0
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 * @since 1.50.0
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @return the frame rate
	 * @since 1.50.0
	 */
	public Double getFrameRate() {
		return frameRate;
	}

	/**
	 * @param frameRate the frame rate to set
	 * @since 1.50.0
	 */
	public void setFrameRate(Double frameRate) {
		this.frameRate = frameRate;
	}

	public boolean hasVideoMetadata() {
		return videoMetadata != null;
	}

	public MediaVideoMetadata getVideoMetadata() {
		return videoMetadata;
	}

	public void setVideoMetadata(MediaVideoMetadata value) {
		videoMetadata = value;
	}

	public boolean hasAudioMetadata() {
		return audioMetadata != null;
	}

	public MediaAudioMetadata getAudioMetadata() {
		return audioMetadata;
	}

	public void setAudioMetadata(MediaAudioMetadata value) {
		audioMetadata = value;
	}

	/**
	 * The aspect ratio for a DVD ISO video track
	 *
	 * @return the aspect
	 * @since 1.50.0
	 */
	public String getAspectRatioDvdIso() {
		return aspectRatioDvdIso;
	}

	/**
	 * @param aspectRatio the aspect to set
	 * @since 1.50.0
	 */
	public void setAspectRatioDvdIso(String aspectRatio) {
		this.aspectRatioDvdIso = aspectRatio;
	}

	/**
	 * @return the thumbnail
	 * @since 1.50.0
	 */
	public DLNAThumbnail getThumbnail() {
		return ThumbnailStore.getThumbnail(thumbnailId);
	}

	public Long getThumbnailId() {
		return thumbnailId;
	}

	public void setThumbnailId(Long thumbnailId) {
		this.thumbnailId = thumbnailId;
	}

	public ThumbnailSource getThumbnailSource() {
		return thumbnailSource;
	}

	public void setThumbnailSource(ThumbnailSource value) {
		this.thumbnailSource = value;
	}

	public void setThumbnailSource(String value) {
		this.thumbnailSource = ThumbnailSource.valueOfName(value);
	}

	/**
	 * @return the thumbnail is ready
	 * @since 1.50.0
	 */
	public boolean isThumbnailReady() {
		return getThumbnail() != null;
	}

	public DLNAThumbnailInputStream getThumbnailInputStream() {
		DLNAThumbnail thumb = getThumbnail();
		return thumb != null ? new DLNAThumbnailInputStream(thumb) : null;
	}

	/**
	 * @return the mimeType
	 * @since 1.50.0
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType the mimeType to set
	 * @since 1.50.0
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * @return The {@link ImageInfo} for this media or {@code null}.
	 */
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	/**
	 * Sets the {@link ImageInfo} for this media.
	 *
	 * @param imageInfo the {@link ImageInfo}.
	 */
	public void setImageInfo(ImageInfo imageInfo) {
		this.imageInfo = imageInfo;
	}

	/**
	 * @return the videoTracks
	 */
	public synchronized List<MediaVideo> getVideoTracks() {
		return videoTracks;
	}

	public synchronized MediaVideo getDefaultVideoTrack() {
		return defaultVideoTrack;
	}

	/**
	 *
	 * @return true if has video tracks
	 */
	public synchronized boolean hasVideoTrack() {
		return !videoTracks.isEmpty();
	}

	/**
	 * @param videoTracks the videoTracks to set
	 * @since 1.60.0
	 */
	public synchronized void setVideoTracks(List<MediaVideo> videoTracks) {
		if (videoTracks == null) {
			this.videoTracks.clear();
		} else {
			this.videoTracks = videoTracks;
		}
		defaultVideoTrack = null;
		for (MediaVideo videoTrack : this.videoTracks) {
			if (defaultVideoTrack == null || (!defaultVideoTrack.isDefault() && videoTrack.isDefault())) {
				defaultVideoTrack = videoTrack;
			}
			if (defaultVideoTrack.isDefault()) {
				break;
			}
		}
	}

	/**
	 * @param videoTrack the videoTrack to add
	 */
	public synchronized void addVideoTrack(MediaVideo videoTrack) {
		this.videoTracks.add(videoTrack);
		if (defaultVideoTrack == null || (!defaultVideoTrack.isDefault() && videoTrack.isDefault())) {
			defaultVideoTrack = videoTrack;
		}
	}

	/**
	 * @return the audioTracks
	 * @since 1.60.0
	 */
	public synchronized List<MediaAudio> getAudioTracks() {
		return audioTracks;
	}

	public synchronized MediaAudio getDefaultAudioTrack() {
		return defaultAudioTrack;
	}

	/**
	 * @param audioTracks the audioTracks to set
	 * @since 1.60.0
	 */
	public synchronized void setAudioTracks(List<MediaAudio> audioTracks) {
		if (audioTracks == null) {
			this.audioTracks.clear();
		} else {
			this.audioTracks = audioTracks;
		}
		defaultAudioTrack = null;
		for (MediaAudio audioTrack : this.audioTracks) {
			if (defaultAudioTrack == null || (!defaultAudioTrack.isDefault() && audioTrack.isDefault())) {
				defaultAudioTrack = audioTrack;
			}
			if (defaultAudioTrack.isDefault()) {
				break;
			}
		}
	}

	/**
	 * @param audioTrack the audioTrack to add
	 */
	public synchronized void addAudioTrack(MediaAudio audioTrack) {
		this.audioTracks.add(audioTrack);
		if (defaultAudioTrack == null || audioTrack.isDefault()) {
			defaultAudioTrack = audioTrack;
		}
	}

	/**
	 * @return the subtitleTracks
	 * @since 1.60.0
	 */
	public synchronized List<MediaSubtitle> getSubtitlesTracks() {
		return subtitleTracks;
	}

	/**
	 * @param subtitlesTracks the subtitlesTracks to set
	 * @since 1.60.0
	 */
	public synchronized void setSubtitlesTracks(List<MediaSubtitle> subtitlesTracks) {
		this.subtitleTracks = subtitlesTracks;
	}

	/**
	 * @param subtitlesTrack the subtitleTrack to add
	 */
	public synchronized void addSubtitlesTrack(MediaSubtitle subtitlesTrack) {
		this.subtitleTracks.add(subtitlesTrack);
	}

	/**
	 * @return the chapters
	 */
	public synchronized List<MediaChapter> getChapters() {
		return chapters;
	}

	/**
	 *
	 * @param chapters the chapters to add
	 */
	public synchronized void setChapters(List<MediaChapter> chapters) {
		this.chapters = chapters;
	}

	/**
	 *
	 * @return true if has chapter
	 */
	public synchronized boolean hasChapters() {
		return !chapters.isEmpty();
	}

	/**
	 * @return the container
	 * @since 1.50.0
	 */
	public String getContainer() {
		return container;
	}

	/**
	 * @param container the container to set
	 * @since 1.50.0
	 */
	public void setContainer(String container) {
		this.container = container;
	}

	public boolean isMpegTS() {
		return container != null && container.equals(FormatConfiguration.MPEGTS);
	}

	/**
	 * @return the last parser.
	 * @since 1.50.0
	 */
	public String getMediaParser() {
		return lastParser;
	}

	/**
	 * @param mediaparsed the mediaparsed to set
	 * @since 1.50.0
	 */
	public void setMediaParser(String lastParser) {
		this.lastParser = lastParser;
	}

	/**
	 * @return the mediaparsed
	 * @since 1.50.0
	 */
	public boolean isMediaParsed() {
		return this.lastParser != null;
	}

	/**
	 * @return the dvdtrack
	 * @since 1.50.0
	 */
	public Integer getDvdtrack() {
		return dvdtrack;
	}

	/**
	 * @param dvdtrack the dvdtrack to set
	 * @since 1.50.0
	 */
	public void setDvdtrack(Integer dvdtrack) {
		this.dvdtrack = dvdtrack;
	}

	/**
	 * @return the secondaryFormatValid
	 * @since 1.50.0
	 */
	public boolean isSecondaryFormatValid() {
		return (isAudio() && defaultAudioTrack.getBitDepth() == 24 && defaultAudioTrack.getSampleRate() > 48000);
	}

	/**
	 * @return the parsing
	 * @since 1.50.0
	 */
	public boolean isParsing() {
		return parsing.get();
	}

	/**
	 * @param parsing the parsing to set
	 * @since 1.50.0
	 */
	public void setParsing(boolean parsing) {
		this.parsing.set(parsing);
	}

	public long getLastExternalLookup() {
		return lastExternalLookup;
	}

	public void setLastExternalLookup(long lastExternalLookup) {
		this.lastExternalLookup = lastExternalLookup;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return switch (getMediaType()) {
			case VIDEO -> defaultVideoTrack.getHeight();
			case IMAGE -> imageInfo.getHeight();
			default -> 0;
		};
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return switch (getMediaType()) {
			case VIDEO -> defaultVideoTrack.getWidth();
			case IMAGE -> imageInfo.getWidth();
			default -> 0;
		};
	}

	/**
	 * Determines if this {@link MediaInfo} instance has a container that is
	 * used both for audio and video media.
	 *
	 * @return {@code true} if the currently set {@code container} can be either
	 *         audio or video, {@code false} otherwise.
	 */
	public boolean isAudioOrVideoContainer() {
		return isAudioOrVideoContainer(container);
	}

	/**
	 * Returns the {@link Format} to use if this {@link MediaInfo} instance
	 * represent an audio media wrapped in a container that can represent both
	 * audio and video media. This returns {@code null} unless
	 * {@link #isAudioOrVideoContainer} is {@code true}.
	 *
	 * @see #isAudioOrVideoContainer()
	 *
	 * @return The "audio variant" {@link Format} for this container, or
	 *         {@code null} if it doesn't apply.
	 */
	public Format getAudioVariantFormat() {
		return getAudioVariantFormat(container);
	}

	/**
	 * Returns the {@link FormatConfiguration} {@link String} constant to use if
	 * this {@link MediaInfo} instance represent an audio media wrapped in a
	 * container that can represent both audio and video media. This returns
	 * {@code null} unless {@link #isAudioOrVideoContainer} is {@code true}.
	 *
	 * @see #isAudioOrVideoContainer()
	 *
	 * @return The "audio variant" {@link FormatConfiguration} {@link String}
	 *         constant for this container, or {@code null} if it doesn't apply.
	 */
	public String getAudioVariantFormatConfigurationString() {
		return getAudioVariantFormatConfigurationString(container);
	}

	/**
	 * Returns the {@link AudioVariantInfo} to use if this {@link MediaInfo}
	 * instance represent an audio media wrapped in a container that can
	 * represent both audio and video media.
	 * This returns {@code null} unless {@link #isAudioOrVideoContainer}
	 * is {@code true}.
	 *
	 *  TODO : seems not used
	 * @see #isAudioOrVideoContainer()
	 *
	 * @return The {@link AudioVariantInfo} for this container, or {@code null}
	 *         if it doesn't apply.
	 */
	private AudioVariantInfo getAudioVariant() {
		return getAudioVariant(container);
	}

	private void appendVideoTracks(StringBuilder sb) {
		sb.append(", Video Tracks: ").append(getVideoTrackCount());
		for (MediaVideo video : videoTracks) {
			if (!video.equals(videoTracks.get(0))) {
				sb.append(",");
			}
			sb.append(" [").append(video).append("]");
		}
	}

	private void appendAudioTracks(StringBuilder sb) {
		sb.append(", Audio Tracks: ").append(getAudioTrackCount());
		for (MediaAudio audio : audioTracks) {
			if (!audio.equals(audioTracks.get(0))) {
				sb.append(",");
			}
			sb.append(" [").append(audio).append("]");
		}
	}

	private void appendSubtitleTracks(StringBuilder sb) {
		sb.append(", Subtitle Tracks: ").append(getSubTrackCount());
		for (MediaSubtitle subtitleTrack : subtitleTracks) {
			if (!subtitleTrack.equals(subtitleTracks.get(0))) {
				sb.append(",");
			}
			sb.append(" [").append(subtitleTrack).append("]");
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (getContainer() != null) {
			result.append("Container: ").append(getContainer().toUpperCase(Locale.ROOT)).append(", ");
		}
		result.append("Size: ").append(getSize());
		if (isVideo()) {
			result.append(", Overall Bitrate: ").append(getBitRate());
			if (StringUtils.isNotBlank(getTitle())) {
				result.append(", File Title from Metadata: ").append(getTitle());
			}
			if (frameRate != null) {
				result.append(", Frame Rate: ").append(getFrameRate());
			}
			result.append(", Duration: ").append(getDurationString());
			appendVideoTracks(result);

			if (getAudioTrackCount() > 0) {
				appendAudioTracks(result);
			}

			if (subtitleTracks != null && !subtitleTracks.isEmpty()) {
				appendSubtitleTracks(result);
			}
			if (hasVideoMetadata()) {
				if (StringUtils.isNotBlank(videoMetadata.getIMDbID())) {
					result.append(", IMDb ID: ").append(videoMetadata.getIMDbID());
				}
				if (videoMetadata.getYear() != null) {
					result.append(", Year: ").append(videoMetadata.getYear());
				}
				if (StringUtils.isNotBlank(videoMetadata.getTitle())) {
					result.append(", Movie/TV series name: ").append(videoMetadata.getTitle());
				}
				if (videoMetadata.isTvEpisode()) {
					result.append(", TV season: ").append(videoMetadata.getTvSeason());
					result.append(", TV episode number: ").append(videoMetadata.getTvEpisodeNumber());
					if (StringUtils.isNotBlank(videoMetadata.getTvEpisodeName())) {
						result.append(", TV episode name: ").append(videoMetadata.getTvEpisodeName());
					}
				}
			}
		} else if (getAudioTrackCount() > 0) {
			result.append(", Overall Bitrate: ").append(getBitRate());
			result.append(", Duration: ").append(getDurationString());
			appendAudioTracks(result);
			if (hasAudioMetadata()) {
				if (StringUtils.isNotBlank(audioMetadata.getArtist())) {
					result.append(", Artist: ").append(audioMetadata.getArtist());
				}
				if (StringUtils.isNotBlank(audioMetadata.getComposer())) {
					result.append(", Composer: ").append(audioMetadata.getComposer());
				}
				if (StringUtils.isNotBlank(audioMetadata.getConductor())) {
					result.append(", Conductor: ").append(audioMetadata.getConductor());
				}
				if (StringUtils.isNotBlank(audioMetadata.getAlbum())) {
					result.append(", Album: ").append(audioMetadata.getAlbum());
				}
				if (StringUtils.isNotBlank(audioMetadata.getAlbumArtist())) {
					result.append(", Album Artist: ").append(audioMetadata.getAlbumArtist());
				}
				if (StringUtils.isNotBlank(audioMetadata.getSongname())) {
					result.append(", Track Name: ").append(audioMetadata.getSongname());
				}
				if (audioMetadata.getYear() != 0) {
					result.append(", Year: ").append(audioMetadata.getYear());
				}
				if (audioMetadata.getTrack() != 0) {
					result.append(", Track: ").append(audioMetadata.getTrack());
				}
				if (StringUtils.isNotBlank(audioMetadata.getGenre())) {
					result.append(", Genre: ").append(audioMetadata.getGenre());
				}
			}
		}
		if (getImageCount() > 0) {
			if (getImageCount() > 1) {
				result.append(", Images: ").append(getImageCount());
			}
			if (getImageInfo() != null) {
				result.append(", ").append(getImageInfo());
			} else {
				result.append(", Image Width: ").append(getWidth());
				result.append(", Image Height: ").append(getHeight());
			}
		}

		if (getThumbnail() != null) {
			result.append(", ").append(getThumbnail());
		}

		if (getMimeType() != null) {
			result.append(", Mime Type: ").append(getMimeType());
		}

		return result.toString();
	}

	@Override
	public MediaInfo clone() throws CloneNotSupportedException {
		MediaInfo mediaCloned = (MediaInfo) super.clone();
		mediaCloned.setVideoTracks(new ArrayList<>());
		for (MediaVideo video : videoTracks) {
			mediaCloned.addVideoTrack((MediaVideo) video.clone());
		}

		mediaCloned.setAudioTracks(new ArrayList<>());
		for (MediaAudio audio : audioTracks) {
			mediaCloned.addAudioTrack((MediaAudio) audio.clone());
		}

		mediaCloned.setSubtitlesTracks(new ArrayList<>());
		for (MediaSubtitle sub : subtitleTracks) {
			mediaCloned.addSubtitlesTrack((MediaSubtitle) sub.clone());
		}

		return mediaCloned;
	}

	public static boolean isLossless(String codecA) {
		return codecA != null && (codecA.contains("pcm") || codecA.startsWith("dts") || codecA.equals("dca") || codecA.contains("flac")) && !codecA.contains("pcm_u8") && !codecA.contains("pcm_s8");
	}

	private static boolean isAudioOrVideoContainer(String container) {
		if (StringUtils.isBlank(container)) {
			return false;
		}
		for (Entry<String, AudioVariantInfo> entry : AUDIO_OR_VIDEO_CONTAINERS.entrySet()) {
			if (
				container.equals(entry.getKey()) ||
				container.equals(entry.getValue().getFormatConfiguration())
			) {
				return true;
			}
		}
		return false;
	}

	private static Format getAudioVariantFormat(String container) {
		if (StringUtils.isBlank(container)) {
			return null;
		}
		for (Entry<String, AudioVariantInfo> entry : AUDIO_OR_VIDEO_CONTAINERS.entrySet()) {
			if (
				container.equals(entry.getKey()) ||
				container.equals(entry.getValue().getFormatConfiguration())
			) {
				return entry.getValue().getFormat();
			}
		}
		return null;
	}

	private static String getAudioVariantFormatConfigurationString(String container) {
		if (StringUtils.isBlank(container)) {
			return null;
		}
		for (Entry<String, AudioVariantInfo> entry : AUDIO_OR_VIDEO_CONTAINERS.entrySet()) {
			if (
				container.equals(entry.getKey()) ||
				container.equals(entry.getValue().getFormatConfiguration())
			) {
				return entry.getValue().getFormatConfiguration();
			}
		}
		return null;
	}

	// TODO : seems not used
	private static AudioVariantInfo getAudioVariant(String container) {
		if (StringUtils.isBlank(container)) {
			return null;
		}
		for (Entry<String, AudioVariantInfo> entry : AUDIO_OR_VIDEO_CONTAINERS.entrySet()) {
			if (
				container.equals(entry.getKey()) ||
				container.equals(entry.getValue().getFormatConfiguration())
			) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * An immutable struct/record for hold information for a particular audio
	 * variant for containers that can constitute multiple "media types".
	 */
	public static class AudioVariantInfo {

		protected final Format format;
		protected final String formatConfiguration;

		/**
		 * Creates a new instance.
		 *
		 * @param format the {@link Format} for this {@link AudioVariantInfo}.
		 * @param formatConfiguration the {@link FormatConfiguration}
		 *            {@link String} constant for this {@link AudioVariantInfo}.
		 */
		public AudioVariantInfo(Format format, String formatConfiguration) {
			this.format = format;
			this.formatConfiguration = formatConfiguration;
		}

		/**
		 * @return the {@link Format}.
		 */
		public Format getFormat() {
			return format;
		}

		/**
		 * @return the {@link FormatConfiguration} {@link String} constant.
		 */
		public String getFormatConfiguration() {
			return formatConfiguration;
		}
	}

	private static Map<String, AudioVariantInfo> getAudioOrVideoContainers() {
		Map<String, AudioVariantInfo> mutableAudioOrVideoContainers = new HashMap<>();

		// Map container formats to their "audio variant".
		mutableAudioOrVideoContainers.put(FormatConfiguration.MP4, new AudioVariantInfo(new M4A(), FormatConfiguration.M4A));
		mutableAudioOrVideoContainers.put(FormatConfiguration.MKV, new AudioVariantInfo(new MKA(), FormatConfiguration.MKA));
		mutableAudioOrVideoContainers.put(FormatConfiguration.OGG, new AudioVariantInfo(new OGA(), FormatConfiguration.OGA));
		mutableAudioOrVideoContainers.put(FormatConfiguration.RM, new AudioVariantInfo(new RA(), FormatConfiguration.RA));
		// XXX Not technically correct, but should work until MPA is implemented
		mutableAudioOrVideoContainers.put(FormatConfiguration.MPEG1, new AudioVariantInfo(new MP3(), FormatConfiguration.MPA));
		// XXX Not technically correct, but should work until MPA is implemented
		mutableAudioOrVideoContainers.put(FormatConfiguration.MPEG2, new AudioVariantInfo(new MP3(), FormatConfiguration.MPA));
		mutableAudioOrVideoContainers.put(FormatConfiguration.THREEGPP, new AudioVariantInfo(new THREEGA(), FormatConfiguration.THREEGA));
		mutableAudioOrVideoContainers.put(FormatConfiguration.THREEGPP2, new AudioVariantInfo(new THREEG2A(), FormatConfiguration.THREEGA));
		// XXX WEBM Audio is NOT MKA, but it will have to stay this way until WEBM Audio is implemented.
		mutableAudioOrVideoContainers.put(FormatConfiguration.WEBM, new AudioVariantInfo(new MKA(), FormatConfiguration.WEBA));
		mutableAudioOrVideoContainers.put(FormatConfiguration.WMV, new AudioVariantInfo(new WMA(), FormatConfiguration.WMA));

		return Collections.unmodifiableMap(mutableAudioOrVideoContainers);
	}

}
