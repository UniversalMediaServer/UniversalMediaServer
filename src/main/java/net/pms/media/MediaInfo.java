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
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import net.pms.formats.audio.*;
import net.pms.image.ImageInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.parsers.Parser;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of media file metadata scanned by the MediaInfo library.
 */
public class MediaInfo implements Cloneable {

	public static final long ENDFILE_POS = 99999475712L;

	/**
	 * Maximum size of a stream, taking into account that some renderers (like
	 * the PS3) will convert this <code>long</code> to <code>int</code>.
	 * Truncating this value will still return the maximum value that an
	 * <code>int</code> can contain.
	 */
	public static final long TRANS_SIZE = Long.MAX_VALUE - Integer.MAX_VALUE - 1;

	/**
	 * Containers that can represent audio or video media is by default
	 * considered to be video. This {@link Map} maps such containers to the type
	 * to use if they represent audio media.
	 */
	protected static final Map<String, AudioVariantInfo> AUDIO_OR_VIDEO_CONTAINERS;

	static {
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

		AUDIO_OR_VIDEO_CONTAINERS = Collections.unmodifiableMap(mutableAudioOrVideoContainers);
	}

	// Stored in database
	private String lastParser;
	private Double durationSec;
	private int bitrate;
	private long size;
	private Double frameRate;
	private String container;
	private String fileTitleFromMetadata;
	private String aspectRatioDvdIso;

	private volatile DLNAThumbnail thumb = null;

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

	/**
	 * Not stored in database.
	 */
	private String mimeType;

	/**
	 * isUseMediaInfo-related, used to manage thumbnail management separated
	 * from the main parsing process.
	 */
	private volatile boolean thumbready;

	private Integer dvdtrack;
	private boolean secondaryFormatValid = true;

	private final Object parsingLock = new Object();
	private boolean parsing = false;

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
	 * FIXME : This should not set current thumb.
	 * Thumbnail is linked to status, renderer, ...
	 */
	public void generateThumbnail(InputFile input, Format ext, int type, Double seekPosition) {
		thumb = Parser.getThumbnail(this, input, ext, type, seekPosition);
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

	public void postParse(int type) {
		String codecA = defaultAudioTrack != null ? defaultAudioTrack.getCodec() : null;
		String codecV = defaultVideoTrack != null ? defaultVideoTrack.getCodec() : null;

		if (container != null) {
			mimeType = switch (container) {
				case FormatConfiguration.AVI -> HTTPResource.AVI_TYPEMIME;
				case FormatConfiguration.ASF -> HTTPResource.ASF_TYPEMIME;
				case FormatConfiguration.FLV -> HTTPResource.FLV_TYPEMIME;
				case FormatConfiguration.M4V -> HTTPResource.M4V_TYPEMIME;
				case FormatConfiguration.MP4 -> HTTPResource.MP4_TYPEMIME;
				case FormatConfiguration.MPEGPS -> HTTPResource.MPEG_TYPEMIME;
				case FormatConfiguration.MPEGTS -> HTTPResource.MPEGTS_TYPEMIME;
				case FormatConfiguration.MPEGTS_HLS -> HTTPResource.HLS_TYPEMIME;
				case FormatConfiguration.WMV -> HTTPResource.WMV_TYPEMIME;
				case FormatConfiguration.MOV -> HTTPResource.MOV_TYPEMIME;
				case FormatConfiguration.ADPCM -> HTTPResource.AUDIO_ADPCM_TYPEMIME;
				case FormatConfiguration.ADTS -> HTTPResource.AUDIO_ADTS_TYPEMIME;
				case FormatConfiguration.M4A -> HTTPResource.AUDIO_M4A_TYPEMIME;
				case FormatConfiguration.AC3 -> HTTPResource.AUDIO_AC3_TYPEMIME;
				case FormatConfiguration.AU -> HTTPResource.AUDIO_AU_TYPEMIME;
				case FormatConfiguration.DFF -> HTTPResource.AUDIO_DFF_TYPEMIME;
				case FormatConfiguration.DSF -> HTTPResource.AUDIO_DSF_TYPEMIME;
				case FormatConfiguration.EAC3 -> HTTPResource.AUDIO_EAC3_TYPEMIME;
				case FormatConfiguration.MPA -> HTTPResource.AUDIO_MPA_TYPEMIME;
				case FormatConfiguration.MP2 -> HTTPResource.AUDIO_MP2_TYPEMIME;
				case FormatConfiguration.AIFF -> HTTPResource.AUDIO_AIFF_TYPEMIME;
				case FormatConfiguration.ATRAC -> HTTPResource.AUDIO_ATRAC_TYPEMIME;
				case FormatConfiguration.MKA -> HTTPResource.AUDIO_MKA_TYPEMIME;
				case FormatConfiguration.MLP -> HTTPResource.AUDIO_MLP_TYPEMIME;
				case FormatConfiguration.MONKEYS_AUDIO -> HTTPResource.AUDIO_APE_TYPEMIME;
				case FormatConfiguration.MPC -> HTTPResource.AUDIO_MPC_TYPEMIME;
				case FormatConfiguration.OGG -> HTTPResource.OGG_TYPEMIME;
				case FormatConfiguration.OGA -> HTTPResource.AUDIO_OGA_TYPEMIME;
				case FormatConfiguration.RA -> HTTPResource.AUDIO_RA_TYPEMIME;
				case FormatConfiguration.RM -> HTTPResource.RM_TYPEMIME;
				case FormatConfiguration.SHORTEN -> HTTPResource.AUDIO_SHN_TYPEMIME;
				case FormatConfiguration.THREEGA -> HTTPResource.AUDIO_THREEGPPA_TYPEMIME;
				case FormatConfiguration.TRUEHD -> HTTPResource.AUDIO_TRUEHD_TYPEMIME;
				case FormatConfiguration.TTA -> HTTPResource.AUDIO_TTA_TYPEMIME;
				case FormatConfiguration.WAVPACK -> HTTPResource.AUDIO_WV_TYPEMIME;
				case FormatConfiguration.WEBA -> HTTPResource.AUDIO_WEBM_TYPEMIME;
				case FormatConfiguration.WEBP -> HTTPResource.WEBP_TYPEMIME;
				case FormatConfiguration.WMA, FormatConfiguration.WMA10 -> HTTPResource.AUDIO_WMA_TYPEMIME;
				case FormatConfiguration.BMP -> HTTPResource.BMP_TYPEMIME;
				case FormatConfiguration.GIF -> HTTPResource.GIF_TYPEMIME;
				case FormatConfiguration.JPEG -> HTTPResource.JPEG_TYPEMIME;
				case FormatConfiguration.PNG -> HTTPResource.PNG_TYPEMIME;
				case FormatConfiguration.TIFF -> HTTPResource.TIFF_TYPEMIME;
				default -> mimeType;
			};
		}

		if (mimeType == null) {
			if (codecV != null && !codecV.equals(MediaLang.UND)) {
				if ("matroska".equals(container) || "mkv".equals(container)) {
					mimeType = HTTPResource.MATROSKA_TYPEMIME;
				} else if ("ogg".equals(container)) {
					mimeType = HTTPResource.OGG_TYPEMIME;
				} else if ("3gp".equals(container)) {
					mimeType = HTTPResource.THREEGPP_TYPEMIME;
				} else if ("3g2".equals(container)) {
					mimeType = HTTPResource.THREEGPP2_TYPEMIME;
				} else if ("webm".equals(container)) {
					mimeType = HTTPResource.WEBM_TYPEMIME;
				} else if (container.startsWith("flash")) {
					mimeType = HTTPResource.FLV_TYPEMIME;
				} else if (codecV.startsWith("h264") || codecV.equals("h263") || codecV.equals("mpeg4") || codecV.equals("mp4")) {
					mimeType = HTTPResource.MP4_TYPEMIME;
				} else if (codecV.contains("mpeg") || codecV.contains("mpg")) {
					mimeType = HTTPResource.MPEG_TYPEMIME;
				}
			} else if ((codecV == null || codecV.equals(MediaLang.UND)) && codecA != null) {
				if ("ogg".equals(container) || "oga".equals(container)) {
					mimeType = HTTPResource.AUDIO_OGA_TYPEMIME;
				} else if ("3gp".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPPA_TYPEMIME;
				} else if ("3g2".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPP2A_TYPEMIME;
				} else if ("adts".equals(container)) {
					mimeType = HTTPResource.AUDIO_ADTS_TYPEMIME;
				} else if ("matroska".equals(container) || "mkv".equals(container)) {
					mimeType = HTTPResource.AUDIO_MKA_TYPEMIME;
				} else if ("webm".equals(container)) {
					mimeType = HTTPResource.AUDIO_WEBM_TYPEMIME;
				} else if (codecA.contains("mp3")) {
					mimeType = HTTPResource.AUDIO_MP3_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.MPA)) {
					mimeType = HTTPResource.AUDIO_MPA_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.MP2)) {
					mimeType = HTTPResource.AUDIO_MP2_TYPEMIME;
				} else if (codecA.contains("flac")) {
					mimeType = HTTPResource.AUDIO_FLAC_TYPEMIME;
				} else if (codecA.contains("vorbis")) {
					mimeType = HTTPResource.AUDIO_VORBIS_TYPEMIME;
				} else if (codecA.contains("asf") || codecA.startsWith("wm")) {
					mimeType = HTTPResource.AUDIO_WMA_TYPEMIME;
				} else if (codecA.contains("pcm") || codecA.contains("wav") || codecA.contains("dts")) {
					mimeType = HTTPResource.AUDIO_WAV_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.TRUEHD)) {
					mimeType = HTTPResource.AUDIO_TRUEHD_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DTS)) {
					mimeType = HTTPResource.AUDIO_DTS_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DTSHD)) {
					mimeType = HTTPResource.AUDIO_DTSHD_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.EAC3)) {
					mimeType = HTTPResource.AUDIO_EAC3_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.ADPCM)) {
					mimeType = HTTPResource.AUDIO_ADPCM_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DFF)) {
					mimeType = HTTPResource.AUDIO_DFF_TYPEMIME;
				} else if (codecA.equals(FormatConfiguration.DSF)) {
					mimeType = HTTPResource.AUDIO_DSF_TYPEMIME;
				}
			}

			if (mimeType == null) {
				mimeType = HTTPResource.getDefaultMimeType(type);
			}
		}

		if (defaultAudioTrack == null || !(type == Format.AUDIO && defaultAudioTrack.getBitDepth() == 24 && defaultAudioTrack.getSampleRate() > 48000)) {
			secondaryFormatValid = false;
		}
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
	 * @return the thumb
	 * @since 1.50.0
	 */
	public DLNAThumbnail getThumb() {
		return thumb;
	}

	/**
	 * Sets the {@link DLNAThumbnail} instance to use for this {@link MediaInfo} instance.
	 *
	 * @param thumbnail the {@link DLNAThumbnail} to set.
	 */
	public void setThumb(DLNAThumbnail thumbnail) {
		this.thumb = thumbnail;
		if (thumbnail != null) {
			thumbready = true;
		}
	}

	/**
	 * @return the thumbready
	 * @since 1.50.0
	 */
	public boolean isThumbready() {
		return thumbready;
	}

	/**
	 * @param thumbready the thumbready to set
	 * @since 1.50.0
	 */
	public void setThumbready(boolean thumbready) {
		this.thumbready = thumbready;
	}

	public DLNAThumbnailInputStream getThumbnailInputStream() {
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

	public String getFileTitleFromMetadata() {
		return fileTitleFromMetadata;
	}

	public void setTitle(String value) {
		this.fileTitleFromMetadata = value;
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
	 * @param chapter the chapter to add
	 */
	public synchronized void addChapter(MediaChapter chapter) {
		this.chapters.add(chapter);
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
		return container != null && container.equals("mpegts");
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
		return secondaryFormatValid;
	}

	/**
	 * @param secondaryFormatValid the secondaryFormatValid to set
	 * @since 1.50.0
	 */
	public void setSecondaryFormatValid(boolean secondaryFormatValid) {
		this.secondaryFormatValid = secondaryFormatValid;
	}

	/**
	 * @return the parsing
	 * @since 1.50.0
	 */
	public boolean isParsing() {
		synchronized (parsingLock) {
			return parsing;
		}
	}

	/**
	 * @param parsing the parsing to set
	 * @since 1.50.0
	 */
	public void setParsing(boolean parsing) {
		synchronized (parsingLock) {
			this.parsing = parsing;
		}
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
	 * @see #isAudioOrVideoContainer()
	 *
	 * @return The {@link AudioVariantInfo} for this container, or {@code null}
	 *         if it doesn't apply.
	 */
	public AudioVariantInfo getAudioVariant() {
		return getAudioVariant(container);
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
			if (StringUtils.isNotBlank(getFileTitleFromMetadata())) {
				result.append(", File Title from Metadata: ").append(getFileTitleFromMetadata());
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
				if (StringUtils.isNotBlank(videoMetadata.getYear())) {
					result.append(", Year: ").append(videoMetadata.getYear());
				}
				if (StringUtils.isNotBlank(videoMetadata.getMovieOrShowName())) {
					result.append(", Movie/TV series name: ").append(videoMetadata.getMovieOrShowName());
				}
				if (videoMetadata.isTVEpisode()) {
					result.append(", TV season: ").append(videoMetadata.getTVSeason());
					result.append(", TV episode number: ").append(videoMetadata.getTVEpisodeNumber());
					if (StringUtils.isNotBlank(videoMetadata.getTVEpisodeName())) {
						result.append(", TV episode name: ").append(videoMetadata.getTVEpisodeName());
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

		if (getThumb() != null) {
			result.append(", ").append(getThumb());
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

}
