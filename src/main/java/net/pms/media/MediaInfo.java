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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAMediaInfo.ScanOrder;
import net.pms.dlna.DLNAMediaInfo.ScanType;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.InputFile;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.StandardEngineId;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.formats.audio.*;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ExifInfo;
import net.pms.image.ExifOrientation;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.audio.MediaAudio;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.metadata.MediaVideoMetadata;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo.Mode3D;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.handlers.api.starrating.StarRating;
import net.pms.parsers.FFmpegParser;
import net.pms.renderers.Renderer;
import net.pms.util.APIUtils;
import net.pms.util.CoverSupplier;
import net.pms.util.CoverUtil;
import net.pms.util.FileUtil;
import net.pms.util.MpegUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import net.pms.util.UnknownFormatException;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.lang3.math.NumberUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of media file metadata scanned by the MediaInfo library.
 */
public class MediaInfo implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfo.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Gson GSON = new Gson();
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
	private Double durationSec;
	private int bitrate;
	private int width;
	private int height;
	private long size;
	private String codecV;
	private String frameRate;
	private String frameRateMode;
	private String pixelAspectRatio;
	private ScanType scanType;
	private ScanOrder scanOrder;

	/**
	 * The frame rate mode as read from the parser
	 */
	private String frameRateModeRaw;
	private String frameRateOriginal;
	private String aspectRatioDvdIso;
	private String aspectRatioContainer;
	private String aspectRatioVideoTrack;
	private int videoBitDepth = 8;
	private String videoHDRFormat;
	private String videoHDRFormatCompatibility;

	private volatile DLNAThumbnail thumb = null;

	private MediaVideoMetadata videoMetadata;

	private volatile ImageInfo imageInfo = null;
	private String mimeType;
	private final ReentrantReadWriteLock referenceFrameCountLock = new ReentrantReadWriteLock();
	private byte referenceFrameCount = -1;

	private final ReentrantReadWriteLock avcLevelLock = new ReentrantReadWriteLock();
	private String avcLevel = null;

	private final Object h264ProfileLock = new Object();
	private String h264Profile = null;

	private List<MediaAudio> audioTracks = new ArrayList<>();
	private List<MediaSubtitle> subtitleTracks = new ArrayList<>();
	private List<MediaChapter> chapters = new ArrayList<>();

	private String muxingMode;
	private String muxingModeAudio;
	private String container;

	private final Object h264AnnexBLock = new Object();
	private boolean h264AnnexBParsed;
	private byte[] h264AnnexB;

	/**
	 * Not stored in database.
	 */
	private volatile boolean mediaparsed;

	private boolean ffmpegparsed;

	/**
	 * isUseMediaInfo-related, used to manage thumbnail management separated
	 * from the main parsing process.
	 */
	private volatile boolean thumbready;

	private int dvdtrack;
	private boolean secondaryFormatValid = true;

	private final Object parsingLock = new Object();
	private boolean parsing = false;

	private final Object ffmpegFailureLock = new Object();
	private boolean ffmpegFailure = false;

	private Map<String, String> extras;
	private boolean encrypted;
	private String matrixCoefficients;
	private String stereoscopy;
	private String fileTitleFromMetadata;
	private String videoTrackTitleFromMetadata;

	private int videoTrackCount = 0;
	private int imageCount = 0;

	public int getVideoTrackCount() {
		return videoTrackCount;
	}

	public void setVideoTrackCount(int value) {
		videoTrackCount = value;
	}

	public int getAudioTrackCount() {
		return audioTracks.size();
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
		return !audioTracks.isEmpty();
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
		if (audioTracks.size() != 2) {
			return false;
		}

		return
			(
				audioTracks.get(0).isAACLC() ||
				audioTracks.get(0).isERBSAC()
			) &&
			audioTracks.get(1).isSLS();
	}

	public MediaType getMediaType() {
		if (videoTrackCount > 0) {
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
	 * Used to determine whether tsMuxeR can mux the file to the renderer
	 * instead of transcoding.
	 * Also used by DLNAResource to help determine the DLNA.ORG_PN (file type)
	 * value to send to the renderer.
	 *
	 * Some of this code is repeated in isVideoWithinH264LevelLimits(), and since
	 * both functions are sometimes (but not always) used together, this is
	 * not an efficient use of code.
	 *
	 * TODO: Fix the above situation.
	 * TODO: Now that FFmpeg is muxing without tsMuxeR, we should make a separate
	 *       function for that, or even better, re-think this whole approach.
	 *
	 * @param renderer The renderer we might mux to
	 *
	 * @return
	 */
	public boolean isMuxable(Renderer renderer) {
		// Make sure the file is H.264 video
		boolean muxable = (isH264() || isH265());

		// Check if the renderer supports the resolution of the video
		if (
			(
				renderer.isMaximumResolutionSpecified() &&
				(
					width > renderer.getMaxVideoWidth() ||
					height > renderer.getMaxVideoHeight()
				)
			) ||
			(
				!renderer.isMuxNonMod4Resolution() &&
				!isMod4()
			)
		) {
			muxable = false;
		}

		// Bravia does not support AVC video at less than 288px high
		if (renderer.isBRAVIA() && isH264() && height < 288) {
			muxable = false;
		}

		return muxable;
	}

	/**
	 * Whether a file is a WEB-DL release.
	 *
	 * It's important for some devices like PS3 because WEB-DL files often have
	 * some difference (possibly not starting on a keyframe or something to do with
	 * SEI output from MEncoder, possibly something else) that makes the PS3 not
	 * accept them when output from tsMuxeR via MEncoder.
	 *
	 * The above statement may not be applicable when using tsMuxeR via FFmpeg
	 * so we should reappraise the situation if we make that change.
	 *
	 * It is unlikely it will return false-positives but it will return
	 * false-negatives.
	 *
	 * @param filename the filename
	 * @param params the file properties
	 *
	 * @return whether a file is a WEB-DL release
	 */
	public boolean isWebDl(String filename, OutputParams params) {
		// Check the filename
		if (filename.toLowerCase().replaceAll("\\-", "").contains("webdl")) {
			return true;
		}

		// Check the metadata
		return (
			(
				getFileTitleFromMetadata() != null &&
				getFileTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				getVideoTrackTitleFromMetadata() != null &&
				getVideoTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				params.getAid() != null &&
				params.getAid().getAudioTrackTitleFromMetadata() != null &&
				params.getAid().getAudioTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				params.getSid() != null &&
				params.getSid().getSubtitlesTrackTitleFromMetadata() != null &&
				params.getSid().getSubtitlesTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			)
		);
	}

	public Map<String, String> getExtras() {
		return extras;
	}

	public void putExtra(String key, String value) {
		if (extras == null) {
			extras = new HashMap<>();
		}

		extras.put(key, value);
	}

	public void generateThumbnail(InputFile input, Format ext, int type, Double seekPosition, boolean resume) {
		waitMediaParsing(5);
		setParsing(true);
		MediaInfo forThumbnail = new MediaInfo();
		forThumbnail.setMediaparsed(mediaparsed);  // check if file was already parsed by MediaInfo
		forThumbnail.setImageInfo(imageInfo);
		forThumbnail.durationSec = getDurationInSeconds();
		if (seekPosition <= forThumbnail.durationSec) {
			forThumbnail.durationSec = seekPosition;
		} else {
			forThumbnail.durationSec /= 2;
		}
		forThumbnail.setVideoMetadata(videoMetadata);
		forThumbnail.parseThumbnailOnly(input, ext, type, resume);
		thumb = forThumbnail.thumb;
		thumbready = true;
		setParsing(false);
	}

	private ProcessWrapperImpl getFFmpegThumbnail(InputFile media, boolean resume) {
		/*
		 * Note: The text output from FFmpeg is used by renderers that do
		 * not use MediaInfo, so do not make any changes that remove or
		 * minimize the amount of text given by FFmpeg here
		 */
		ArrayList<String> args = new ArrayList<>();
		boolean generateThumbnail = CONFIGURATION.isThumbnailGenerationEnabled() && !CONFIGURATION.isUseMplayerForVideoThumbs();

		args.add(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO));
		if (args.get(0) == null) {
			LOGGER.warn("Cannot generate thumbnail for {} since the FFmpeg executable is undefined");
			return null;
		}

		if (generateThumbnail) {
			args.add("-ss");
			if (resume) {
				args.add(Integer.toString((int) getDurationInSeconds()));
			} else {
				args.add(Integer.toString((int) Math.min(CONFIGURATION.getThumbnailSeekPos(), getDurationInSeconds())));
			}
		}

		args.add("-i");

		if (media.getFile() != null) {
			args.add(ProcessUtil.getShortFileNameIfWideChars(media.getFile().getAbsolutePath()));
		} else {
			args.add("-");
		}

		args.add("-an");
		args.add("-dn");
		args.add("-sn");
		if (generateThumbnail) {
			args.add("-vf");
			args.add("scale=320:-2");
			args.add("-vframes");
			args.add("1");
			args.add("-f");
			args.add("image2");
			args.add("pipe:");
		}

		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setStdIn(media.getPush());
		params.setNoExitCheck(true); // not serious if anything happens during the thumbnailer

		// true: consume stderr on behalf of the caller i.e. parse()
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params, false, true);

		// FAILSAFE
		waitMediaParsing(5);
		setParsing(true);
		Runnable r = () -> {
			try {
				Thread.sleep(10000);
				synchronized (ffmpegFailureLock) {
					ffmpegFailure = true;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			pw.stopProcess();
			setParsing(false);
		};

		Thread failsafe = new Thread(r, "FFmpeg Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		setParsing(false);
		return pw;
	}

	private ProcessWrapperImpl getMplayerThumbnail(InputFile media, boolean resume) throws IOException {
		File file = media.getFile();
		String[] args = new String[14];
		args[0] = CONFIGURATION.getMPlayerPath();
		args[1] = "-ss";
		if (resume) {
			args[2] = "" + (int) getDurationInSeconds();
		} else {
			args[2] = "" + CONFIGURATION.getThumbnailSeekPos();
		}

		args[3] = "-quiet";

		if (file != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-msglevel";
		args[6] = "all=4";
		args[7] = "-vf";
		args[8] = "scale=320:-2";
		args[9] = "-frames";
		args[10] = "1";
		args[11] = "-vo";
		String frameName = "" + media.hashCode();
		frameName = "mplayer_thumbs:subdirs=\"" + frameName + "\"";
		frameName = frameName.replace(',', '_');
		args[12] = "jpeg:outdir=" + frameName;
		args[13] = "-nosound";
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setWorkDir(CONFIGURATION.getTempFolder());
		params.setMaxBufferSize(1);
		params.setStdIn(media.getPush());
		params.setLog(true);
		params.setNoExitCheck(true); // not serious if anything happens during the thumbnailer
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args, true, params);

		// FAILSAFE
		waitMediaParsing(5);
		setParsing(true);
		Runnable r = () -> {
			UMSUtils.sleep(3000);

			pw.stopProcess();
			setParsing(false);
		};

		Thread failsafe = new Thread(r, "MPlayer Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		setParsing(false);
		return pw;
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

	/**
	 * Parse media without using MediaInfo.
	 */
	public void parse(InputFile inputFile, Format ext, int type, boolean resume) {
		if (!waitMediaParsing(5)) {
			mediaparsed = true;
		}

		if (isMediaparsed()) {
			return;
		}

		if (inputFile != null) {
			File file = inputFile.getFile();
			if (file != null) {
				size = file.length();
			} else {
				size = inputFile.getSize();
			}

			ProcessWrapperImpl pw = null;
			boolean ffmpegParsing = true;

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				ffmpegParsing = false;
				MediaAudio audio = new MediaAudio();

				if (file != null) {
					try {
						AudioFile af;
						if ("mp2".equalsIgnoreCase(FileUtil.getExtension(file))) {
							af = AudioFileIO.readAs(file, "mp3");
						} else {
							af = AudioFileIO.read(file);
						}
						AudioHeader ah = af.getAudioHeader();

						if (ah != null) {
							int length = ah.getTrackLength();
							int rate = ah.getSampleRateAsNumber();

							if (ah.getEncodingType() != null && ah.getEncodingType().toLowerCase().contains("flac 24")) {
								audio.setBitsperSample(24);
							}

							audio.setSampleFrequency("" + rate);
							durationSec = (double) length;
							bitrate = (int) ah.getBitRateAsNumber();

							audio.getAudioProperties().setNumberOfChannels(2); // set default value of channels to 2
							String channels = ah.getChannels().toLowerCase(Locale.ROOT);
							if (StringUtils.isNotBlank(channels)) {
								if (channels.equals("1") || channels.contains("mono")) { // parse value "1" or "Mono"
									audio.getAudioProperties().setNumberOfChannels(1);
								} else if (!(channels.equals("2") || channels.equals("0") || channels.contains("stereo"))) {
									// No need to parse stereo as it's set as default
									try {
										audio.getAudioProperties().setNumberOfChannels(Integer.parseInt(channels));
									} catch (IllegalArgumentException e) {
										LOGGER.debug("Could not parse number of audio channels from \"{}\"", channels);
									}
								}
							}

							if (StringUtils.isNotBlank(ah.getEncodingType())) {
								audio.setCodecA(ah.getEncodingType());
							}

							if (audio.getCodecA() != null && audio.getCodecA().contains("(windows media")) {
								audio.setCodecA(audio.getCodecA().substring(0, audio.getCodecA().indexOf("(windows media")).trim());
							}
						}

						Tag t = af.getTag();

						if (t != null) {
							if (!t.getArtworkList().isEmpty()) {
								thumb = DLNAThumbnail.toThumbnail(
									t.getArtworkList().get(0).getBinaryData(),
									640,
									480,
									ScaleType.MAX,
									ImageFormat.SOURCE,
									false
								);
							} else if (!CONFIGURATION.getAudioThumbnailMethod().equals(CoverSupplier.NONE)) {
								thumb = DLNAThumbnail.toThumbnail(
									CoverUtil.get().getThumbnail(t),
									640,
									480,
									ScaleType.MAX,
									ImageFormat.SOURCE,
									false
								);
							}
							if (thumb != null) {
								thumbready = true;
							}

							audio.setAlbum(extractAudioTagKeyValue(t, FieldKey.ALBUM));
							audio.setArtist(extractAudioTagKeyValue(t, FieldKey.ARTIST));
							audio.setComposer(extractAudioTagKeyValue(t, FieldKey.COMPOSER));
							audio.setConductor(extractAudioTagKeyValue(t, FieldKey.CONDUCTOR));
							audio.setSongname(extractAudioTagKeyValue(t, FieldKey.TITLE));
							audio.setMbidRecord(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_RELEASEID));
							audio.setMbidTrack(extractAudioTagKeyValue(t, FieldKey.MUSICBRAINZ_TRACK_ID));
							audio.setRating(StarRating.convertTagRatingToStar(t));
							audio.setGenre(extractAudioTagKeyValue(t, FieldKey.GENRE));

							String keyyear = extractAudioTagKeyValue(t, FieldKey.YEAR);
							if (keyyear != null) {
								if (keyyear.length() > 4) {
									// Extract just the year, skipping  '-month-day'
									keyyear = keyyear.substring(0, 4);
								}
								if (NumberUtils.isParsable(keyyear)) {
									audio.setYear(Integer.parseInt(keyyear));
								}
							}

							Integer trackNum = extractAudioTagKeyIntegerValue(t, FieldKey.TRACK, 1);
							audio.setTrack(trackNum);
						}
					} catch (CannotReadException e) {
						if (e.getMessage().startsWith(
							ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().substring(0, ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().indexOf("{"))
						)) {
							LOGGER.debug("No audio tag support for audio file \"{}\"", file.getName());
						} else {
							LOGGER.error("Error reading audio tag for \"{}\": {}", file.getName(), e.getMessage());
							LOGGER.trace("", e);
						}
					} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | NumberFormatException | KeyNotFoundException e) {
						LOGGER.debug("Error parsing audio file tag for \"{}\": {}", file.getName(), e.getMessage());
						LOGGER.trace("", e);
						ffmpegParsing = false;
					}

					// Set container for formats that the normal parsing fails to do from Format
					if (StringUtils.isBlank(container) && ext != null && ext.getIdentifier() != null) {
						switch (ext.getIdentifier()) {
							case ADPCM -> audio.setCodecA(FormatConfiguration.ADPCM);
							case DSF -> audio.setCodecA(FormatConfiguration.DSF);
							case DFF -> audio.setCodecA(FormatConfiguration.DFF);
							default -> {
								//nothing to do
							}
						}
					}

					if (StringUtils.isBlank(audio.getSongname())) {
						audio.setSongname(file.getName());
					}

					if (!ffmpegParsing) {
						audioTracks.add(audio);
					}
				}
				if (StringUtils.isBlank(container)) {
					container = audio.getCodecA();
				}
			}

			if (type == Format.IMAGE && file != null) {
				try {
					ffmpegParsing = false;
					ImagesUtil.parseImage(file, this);
					imageCount++;
				} catch (IOException e) {
					LOGGER.debug("Error parsing image \"{}\", switching to FFmpeg: {}", file.getAbsolutePath(), e.getMessage());
					LOGGER.trace("", e);
					ffmpegParsing = true;
				}
			}

			if (ffmpegParsing) {
				if ((type != Format.VIDEO || !CONFIGURATION.isUseMplayerForVideoThumbs())) {
					pw = getFFmpegThumbnail(inputFile, resume);
				}

				String input = "-";

				if (file != null) {
					input = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
				}

				synchronized (ffmpegFailureLock) {
					if (pw != null && !ffmpegFailure) {
						parseFFmpegInfo(pw.getResults(), input);
					}
				}

				if (
					container != null &&
					file != null &&
					container.equals("mpegts") &&
					isH264() &&
					getDurationInSeconds() == 0
				) {
					// Parse the duration
					try {
						int length = MpegUtil.getDurationFromMpeg(file);
						if (length > 0) {
							durationSec = (double) length;
						}
					} catch (IOException e) {
						LOGGER.trace("Error retrieving length: " + e.getMessage());
					}
				}

				if (CONFIGURATION.isUseMplayerForVideoThumbs() && type == Format.VIDEO) {
					try {
						getMplayerThumbnail(inputFile, resume);
						String frameName = "" + inputFile.hashCode();
						frameName = CONFIGURATION.getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
						frameName = frameName.replace(',', '_');
						File jpg = new File(frameName);

						if (jpg.exists()) {
							try (InputStream is = new FileInputStream(jpg)) {
								int sz = is.available();

								if (sz > 0) {
									byte[] bytes = new byte[sz];
									is.read(bytes);
									thumb = DLNAThumbnail.toThumbnail(
										bytes,
										640,
										480,
										ScaleType.MAX,
										ImageFormat.SOURCE,
										false
									);
									thumbready = true;
								}
							}

							if (!jpg.delete()) {
								jpg.deleteOnExit();
							}

							// Try and retry
							if (!jpg.getParentFile().delete() && !jpg.getParentFile().delete()) {
								LOGGER.debug("Failed to delete \"" + jpg.getParentFile().getAbsolutePath() + "\"");
							}
						}
					} catch (IOException e) {
						LOGGER.debug("Caught exception", e);
					}
				}

				if (type == Format.VIDEO && pw != null && thumb == null && pw.getOutputByteArray() != null) {
					byte[] bytes = pw.getOutputByteArray().toByteArray();
					if (bytes != null && bytes.length > 0) {
						try {
							thumb = DLNAThumbnail.toThumbnail(bytes);
						} catch (IOException e) {
							LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
							LOGGER.trace("", e);
						}
						thumbready = true;
					}
				}
			}

			postParse(type, inputFile);
			mediaparsed = true;
		}
	}

	/**
	 * Parse media for ThumbOnly.
	 */
	public void parseThumbnailOnly(InputFile inputFile, Format ext, int type, boolean resume) {
		if (inputFile != null) {
			File file = inputFile.getFile();

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				if (file != null) {
					try {
						AudioFile af;
						if ("mp2".equalsIgnoreCase(FileUtil.getExtension(file))) {
							af = AudioFileIO.readAs(file, "mp3");
						} else {
							af = AudioFileIO.read(file);
						}
						Tag t = af.getTag();
						if (t != null) {
							if (!t.getArtworkList().isEmpty()) {
								thumb = DLNAThumbnail.toThumbnail(
									t.getArtworkList().get(0).getBinaryData(),
									640,
									480,
									ScaleType.MAX,
									ImageFormat.SOURCE,
									false
								);
							} else if (!CONFIGURATION.getAudioThumbnailMethod().equals(CoverSupplier.NONE)) {
								thumb = DLNAThumbnail.toThumbnail(
									CoverUtil.get().getThumbnail(t),
									640,
									480,
									ScaleType.MAX,
									ImageFormat.SOURCE,
									false
								);
							}
							if (thumb != null) {
								thumbready = true;
							}
						}
					} catch (CannotReadException e) {
						if (e.getMessage().startsWith(
							ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().substring(0, ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg().indexOf("{"))
						)) {
							LOGGER.debug("No audio tag support for audio file \"{}\"", file.getName());
						} else {
							LOGGER.error("Error reading audio tag for \"{}\": {}", file.getName(), e.getMessage());
							LOGGER.trace("", e);
						}
					} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | NumberFormatException | KeyNotFoundException e) {
						LOGGER.debug("Error parsing audio file tag for \"{}\": {}", file.getName(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}

			if (type == Format.IMAGE && file != null) {
				if (CONFIGURATION.isThumbnailGenerationEnabled() && CONFIGURATION.getImageThumbnailsEnabled()) {
					LOGGER.trace("Creating thumbnail for \"{}\"", file.getName());

					// Create the thumbnail image
					try {
						if (imageInfo instanceof ExifInfo && ((ExifInfo) imageInfo).hasExifThumbnail() && !imageInfo.isImageIOSupported()) {
							/*
							 * XXX Extraction of thumbnails was removed in version
							 * 2.10.0 of metadata-extractor because of a bug in
							 * related code. This section is deactivated while
							 * waiting for this to be made available again.
							 *
							 * Images supported by ImageIO or DCRaw aren't affected,
							 * so this only applied to very few images anyway.
							 * It could extract thumbnails for some "raw" images
							 * if DCRaw was disabled.
							 *
							 */
						} else {
							// This will fail with UnknownFormatException for any image formats not supported by ImageIO
							thumb = DLNAThumbnail.toThumbnail(
								Files.newInputStream(file.toPath()),
								320,
								320,
								ScaleType.MAX,
								ImageFormat.SOURCE,
								false
							);
						}
						thumbready = true;
					} catch (EOFException e) {
						LOGGER.debug(
							"Error generating thumbnail for \"{}\": Unexpected end of file, probably corrupt file or read error.",
							file.getName()
						);
					} catch (UnknownFormatException e) {
						LOGGER.debug("Could not generate thumbnail for \"{}\" because the format is unknown: {}", file.getName(), e.getMessage());
					} catch (IOException e) {
						LOGGER.debug("Error generating thumbnail for \"{}\": {}", file.getName(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}
			if (type == Format.VIDEO) {
				if (!resume && hasVideoMetadata() && videoMetadata.getPoster() != null) {
					//API Poster
					setThumb(APIUtils.getThumbnailFromUri(videoMetadata.getPoster()));
				}
				if (!thumbready) {
					if (CONFIGURATION.isUseMplayerForVideoThumbs()) {
						//Mplayer parsing
						try {
							getMplayerThumbnail(inputFile, resume);
							String frameName = "" + inputFile.hashCode();
							frameName = CONFIGURATION.getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
							frameName = frameName.replace(',', '_');
							File jpg = new File(frameName);

							if (jpg.exists()) {
								try (InputStream is = new FileInputStream(jpg)) {
									int sz = is.available();
									if (sz > 0) {
										byte[] bytes = new byte[sz];
										is.read(bytes);
										setThumb(DLNAThumbnail.toThumbnail(
											bytes,
											640,
											480,
											ScaleType.MAX,
											ImageFormat.SOURCE,
											false
										));
									}
								}

								if (!jpg.delete()) {
									jpg.deleteOnExit();
								}

								// Try and retry
								if (!jpg.getParentFile().delete() && !jpg.getParentFile().delete()) {
									LOGGER.debug("Failed to delete \"" + jpg.getParentFile().getAbsolutePath() + "\"");
								}
							}
						} catch (IOException e) {
							LOGGER.debug("Caught exception", e);
						}
					} else {
						//FFmpeg parsing
						ProcessWrapperImpl pw = getFFmpegThumbnail(inputFile, resume);
						if (pw != null && thumb == null && pw.getOutputByteArray() != null) {
							byte[] bytes = pw.getOutputByteArray().toByteArray();
							if (bytes != null && bytes.length > 0) {
								try {
									setThumb(DLNAThumbnail.toThumbnail(bytes));
								} catch (IOException e) {
									LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
									LOGGER.trace("", e);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Parses media info from FFmpeg's stderr output
	 *
	 * @param lines The stderr output
	 * @param input The FFmpeg input (-i) argument used
	 */
	public void parseFFmpegInfo(List<String> lines, String input) {
		if (lines != null) {
			if ("-".equals(input)) {
				input = "pipe:";
			}

			boolean matches = false;
			int langId = 0;
			int subId = 0;
			ListIterator<String> fFmpegMetaData = lines.listIterator();

			for (String line : lines) {
				fFmpegMetaData.next();
				line = line.trim();
				if (line.startsWith("Output")) {
					matches = false;
				} else if (line.startsWith("Input")) {
					if (line.contains(input)) {
						matches = true;
						container = line.substring(10, line.indexOf(',', 11)).trim();

						/**
						 * This method is very inaccurate because the Input line in the FFmpeg output
						 * returns "mov,mp4,m4a,3gp,3g2,mj2" for all 6 of those formats, meaning that
						 * we think they are all "mov".
						 *
						 * Here we workaround it by using the file extension, but the best idea is to
						 * prevent using this method by using MediaInfo=true in renderer configs.
						 */
						if ("mov".equals(container)) {
							container = line.substring(line.lastIndexOf('.') + 1, line.lastIndexOf('\'')).trim();
							LOGGER.trace("Setting container to " + container + " from the filename. To prevent false-positives, use MediaInfo=true in the renderer config.");
						}
					} else {
						matches = false;
					}
				} else if (matches) {
					if (line.contains("Duration")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Duration: ")) {
								String durationStr = token.substring(10);
								int l = durationStr.substring(durationStr.indexOf('.') + 1).length();
								if (l < 4) {
									durationStr += "00".substring(0, 3 - l);
								}
								if (durationStr.contains("N/A")) {
									durationSec = null;
								} else {
									durationSec = parseDurationString(durationStr);
								}
							} else if (token.startsWith("bitrate: ")) {
								String bitr = token.substring(9);
								int spacepos = bitr.indexOf(' ');
								if (spacepos > -1) {
									String value = bitr.substring(0, spacepos);
									String unit = bitr.substring(spacepos + 1);
									bitrate = Integer.parseInt(value);
									if (unit.equals("kb/s")) {
										bitrate = 1024 * bitrate;
									}
									if (unit.equals("mb/s")) {
										bitrate = 1048576 * bitrate;
									}
								}
							}
						}
					} else if (line.contains("Audio:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						int a = line.indexOf('(');
						int b = line.indexOf("):", a);
						MediaAudio audio = new MediaAudio();
						audio.setId(langId++);
						if (a > -1 && b > a) {
							audio.setLang(line.substring(a + 1, b));
						} else {
							audio.setLang(MediaLang.UND);
						}

						// Get TS IDs
						a = line.indexOf("[0x");
						b = line.indexOf(']', a);
						if (a > -1 && b > a + 3) {
							String idString = line.substring(a + 3, b);
							try {
								audio.setId(Integer.parseInt(idString, 16));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Error parsing Stream ID: " + idString);
							}
						}

						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								String audioString = "Audio: ";
								int positionAfterAudioString = token.indexOf(audioString) + audioString.length();
								String codec;

								/**
								 * Check whether there are more details after the audio string.
								 * e.g. "Audio: aac (LC)"
								 */
								if (token.indexOf(" ", positionAfterAudioString) != -1) {
									codec = token.substring(positionAfterAudioString, token.indexOf(" ", positionAfterAudioString)).trim();

									// workaround for AAC audio formats
									if (codec.equals("aac")) {
										if (token.contains("(LC)")) {
											codec = FormatConfiguration.AAC_LC;
										} else if (token.contains("(HE-AAC)")) {
											codec = FormatConfiguration.HE_AAC;
										}
									}
								} else {
									codec = token.substring(positionAfterAudioString);

									// workaround for AAC audio formats
									if (codec.equals("aac")) {
										codec = FormatConfiguration.AAC_LC;
									}
								}

								audio.setCodecA(codec);
							} else if (token.endsWith("Hz")) {
								audio.setSampleFrequency(token.substring(0, token.indexOf("Hz")).trim());
							} else if (token.equals("mono")) {
								audio.getAudioProperties().setNumberOfChannels(1);
							} else if (token.equals("stereo")) {
								audio.getAudioProperties().setNumberOfChannels(2);
							} else if (token.equals("5:1") || token.equals("5.1") || token.equals("6 channels")) {
								audio.getAudioProperties().setNumberOfChannels(6);
							} else if (token.equals("5 channels")) {
								audio.getAudioProperties().setNumberOfChannels(5);
							} else if (token.equals("4 channels")) {
								audio.getAudioProperties().setNumberOfChannels(4);
							} else if (token.equals("2 channels")) {
								audio.getAudioProperties().setNumberOfChannels(2);
							} else if (token.equals("s32")) {
								audio.setBitsperSample(32);
							} else if (token.equals("s24")) {
								audio.setBitsperSample(24);
							} else if (token.equals("s16")) {
								audio.setBitsperSample(16);
							}
						}
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();

						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}

						if (line.contains("Metadata:")) {
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										audio.setAudioTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}

						audioTracks.add(audio);
					} else if (line.contains("Video:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								String videoString = "Video: ";
								int positionAfterVideoString = token.indexOf(videoString) + videoString.length();
								String codec;

								// Check whether there are more details after the video string
								if (token.indexOf(" ", positionAfterVideoString) != -1) {
									codec = token.substring(positionAfterVideoString, token.indexOf(" ", positionAfterVideoString)).trim();
								} else {
									codec = token.substring(positionAfterVideoString);
								}

								codecV = codec;
								videoTrackCount++;
							} else if ((token.contains("tbc") || token.contains("tb(c)"))) {
								// A/V sync issues with newest FFmpeg, due to the new tbr/tbn/tbc outputs
								// Priority to tb(c)
								String frameRateDoubleString = token.substring(0, token.indexOf("tb")).trim();
								try {
									// tbc taken into account only if different than tbr
									if (!frameRateDoubleString.equals(frameRate)) {
										Double frameRateDouble = Double.valueOf(frameRateDoubleString);
										frameRate = String.format(Locale.ENGLISH, "%.2f", frameRateDouble / 2);
									}
								} catch (NumberFormatException nfe) {
									// Could happen if tbc is "1k" or something like that, no big deal
									LOGGER.debug("Could not parse frame rate \"" + frameRateDoubleString + "\"");
								}

							} else if ((token.contains("tbr") || token.contains("tb(r)")) && frameRate == null) {
								frameRate = token.substring(0, token.indexOf("tb")).trim();
							} else if ((token.contains("fps") || token.contains("fps(r)")) && frameRate == null) { // dvr-ms ?
								frameRate = token.substring(0, token.indexOf("fps")).trim();
							} else if (token.indexOf('x') > -1 && !token.contains("max")) {
								String resolution = token.trim();
								if (resolution.contains(" [")) {
									resolution = resolution.substring(0, resolution.indexOf(" ["));
								}
								try {
									width = Integer.parseInt(resolution.substring(0, resolution.indexOf('x')));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse width from \"" + resolution.substring(0, resolution.indexOf('x')) + "\"");
								}
								try {
									height = Integer.parseInt(resolution.substring(resolution.indexOf('x') + 1));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse height from \"" + resolution.substring(resolution.indexOf('x') + 1) + "\"");
								}
							}
						}
					} else if (line.contains("Subtitle:")) {
						MediaSubtitle subtitle = new MediaSubtitle();
						// $ ffmpeg -codecs | grep "^...S"
						// ..S... = Subtitle codec
						// DES... ass                  ASS (Advanced SSA) subtitle
						// DES... dvb_subtitle         DVB subtitles (decoders: dvbsub ) (encoders: dvbsub )
						// ..S... dvb_teletext         DVB teletext
						// DES... dvd_subtitle         DVD subtitles (decoders: dvdsub ) (encoders: dvdsub )
						// ..S... eia_608              EIA-608 closed captions
						// D.S... hdmv_pgs_subtitle    HDMV Presentation Graphic Stream subtitles (decoders: pgssub )
						// D.S... jacosub              JACOsub subtitle
						// D.S... microdvd             MicroDVD subtitle
						// DES... mov_text             MOV text
						// D.S... mpl2                 MPL2 subtitle
						// D.S... pjs                  PJS (Phoenix Japanimation Society) subtitle
						// D.S... realtext             RealText subtitle
						// D.S... sami                 SAMI subtitle
						// DES... srt                  SubRip subtitle with embedded timing
						// DES... ssa                  SSA (SubStation Alpha) subtitle
						// DES... subrip               SubRip subtitle
						// D.S... subviewer            SubViewer subtitle
						// D.S... subviewer1           SubViewer v1 subtitle
						// D.S... text                 raw UTF-8 text
						// D.S... vplayer              VPlayer subtitle
						// D.S... webvtt               WebVTT subtitle
						// DES... xsub                 XSUB
						if (line.contains("srt") || line.contains("subrip")) {
							subtitle.setType(SubtitleType.SUBRIP);
						} else if (line.contains(" text")) {
							// excludes dvb_teletext, mov_text, realtext
							subtitle.setType(SubtitleType.TEXT);
						} else if (line.contains("microdvd")) {
							subtitle.setType(SubtitleType.MICRODVD);
						} else if (line.contains("sami")) {
							subtitle.setType(SubtitleType.SAMI);
						} else if (line.contains("ass") || line.contains("ssa")) {
							subtitle.setType(SubtitleType.ASS);
						} else if (line.contains("dvd_subtitle")) {
							subtitle.setType(SubtitleType.VOBSUB);
						} else if (line.contains("xsub")) {
							subtitle.setType(SubtitleType.DIVX);
						} else if (line.contains("mov_text")) {
							subtitle.setType(SubtitleType.TX3G);
						} else if (line.contains("webvtt")) {
							subtitle.setType(SubtitleType.WEBVTT);
						} else if (line.contains("eia_608")) {
							subtitle.setType(SubtitleType.EIA608);
						} else if (line.contains("dvb_subtitle")) {
							subtitle.setType(SubtitleType.DVBSUB);
						} else {
							subtitle.setType(SubtitleType.UNKNOWN);
						}
						int a = line.indexOf('(');
						int b = line.indexOf("):", a);
						if (a > -1 && b > a) {
							subtitle.setLang(line.substring(a + 1, b));
						} else {
							subtitle.setLang(MediaLang.UND);
						}
						subtitle.setId(subId++);
						subtitle.setDefault(line.contains("(default)"));
						subtitle.setForced(line.contains("(forced)"));
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();
						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}
						if (line.contains("Metadata:")) {
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										subtitle.setSubtitlesTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}
						subtitleTracks.add(subtitle);
					} else if (line.contains("Chapters:")) {
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();
						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}
						List<MediaChapter> ffmpegChapters = new ArrayList<>();
						while (line.contains("Chapter #")) {
							MediaChapter chapter = new MediaChapter();
							//set chapter id
							String idStr = line.substring(line.indexOf("Chapter #") + 9);
							if (idStr.contains(" ")) {
								idStr = idStr.substring(0, idStr.indexOf(" "));
							}
							String[] ids = idStr.split(":");
							if (ids.length > 1) {
								chapter.setId(Integer.parseInt(ids[1]));
							} else {
								chapter.setId(Integer.parseInt(ids[0]));
							}
							//set chapter start
							if (line.contains("start ")) {
								String startStr = line.substring(line.indexOf("start ") + 6);
								if (startStr.contains(" ")) {
									startStr = startStr.substring(0, startStr.indexOf(" "));
								}
								if (startStr.endsWith(",")) {
									startStr = startStr.substring(0, startStr.length() - 1);
								}
								chapter.setStart(Double.parseDouble(startStr));
							}
							//set chapter end
							if (line.contains(" end ")) {
								String endStr = line.substring(line.indexOf(" end ") + 5);
								if (endStr.contains(" ")) {
									endStr = endStr.substring(0, endStr.indexOf(" "));
								}
								chapter.setEnd(Double.parseDouble(endStr));
							}
							chapter.setLang(MediaLang.UND);
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							if (line.contains("Metadata:")) {
								fFmpegMetaDataNr += 1;
								line = lines.get(fFmpegMetaDataNr);
								while (line.indexOf("      ") == 0) {
									if (line.contains(": ")) {
										int aa = line.indexOf(": ");
										String key = line.substring(0, aa).trim();
										String value = line.substring(aa + 2);
										if ("title".equals(key)) {
											//do not set title if it is default, it will be filled automatically later
											if (!MediaChapter.isTitleDefault(value)) {
												chapter.setTitle(value);
											}
										} else {
											LOGGER.debug("New chapter metadata not handled \"" + key + "\" : \"" + value + "\"");
										}
										break;
									} else {
										fFmpegMetaDataNr += 1;
										line = lines.get(fFmpegMetaDataNr);
									}
								}
							}
							ffmpegChapters.add(chapter);
						}
						setChapters(ffmpegChapters);
					}
				}
			}
		}
		ffmpegparsed = true;
	}

	/**
	 * Whether the file contains H.264 (AVC) video.
	 *
	 * @return {boolean}
	 */
	public boolean isH264() {
		return codecV != null && codecV.startsWith(FormatConfiguration.H264);
	}

	/**
	 * Whether the file contains H.265 (HEVC) video.
	 *
	 * @return {boolean}
	 */
	public boolean isH265() {
		return codecV != null && codecV.startsWith(FormatConfiguration.H265);
	}

	/**
	 * Disable LPCM transcoding for MP4 container with non-H264 video as workaround for MEncoder's A/V sync bug.
	 * @return isValidForLPCMTranscoding
	 */
	public boolean isValidForLPCMTranscoding() {
		if (container != null) {
			if (container.equals("mp4")) {
				return isH264();
			}
			return true;
		}
		return false;
	}

	public int getFrameNumbers() {
		double fr = Double.parseDouble(frameRate);
		return (int) (getDurationInSeconds() * fr);
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

	public void postParse(int type, InputFile f) {
		String codecA = null;
		if (getFirstAudioTrack() != null) {
			codecA = getFirstAudioTrack().getCodecA();
		}

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
				} else if (codecV.equals("mjpeg") || "jpg".equals(container)) {
					mimeType = HTTPResource.JPEG_TYPEMIME;
				} else if ("png".equals(codecV) || "png".equals(container)) {
					mimeType = HTTPResource.PNG_TYPEMIME;
				} else if ("gif".equals(codecV) || "gif".equals(container)) {
					mimeType = HTTPResource.GIF_TYPEMIME;
				} else if ("tiff".equals(codecV) || "tiff".equals(container)) {
					mimeType = HTTPResource.TIFF_TYPEMIME;
				} else if ("bmp".equals(codecV) || "bmp".equals(container)) {
					mimeType = HTTPResource.BMP_TYPEMIME;
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

		if (getFirstAudioTrack() == null || !(type == Format.AUDIO && getFirstAudioTrack().getBitsperSample() == 24 && getFirstAudioTrack().getSampleRate() > 48000)) {
			secondaryFormatValid = false;
		}
	}

	/**
	 * Checks whether the video has too many reference frames per pixels for the renderer.
	 *
	 * TODO move to PlayerUtil
	 * @param f
	 * @param renderer
	 * @return
	 */
	public boolean isVideoWithinH264LevelLimits(InputFile f, Renderer renderer) {
		if (isH264()) {
			boolean videoWithinH264LevelLimits = true;
			if (
				container != null &&
				(
					container.equals("matroska") ||
					container.equals("mkv") ||
					container.equals("mov") ||
					container.equals("mp4")
				)
			) {
				avcLevelLock.readLock().lock();
				referenceFrameCountLock.readLock().lock();
				try {
					if (
						referenceFrameCount > -1 &&
						(
							"4.1".equals(avcLevel) ||
							"4.2".equals(avcLevel) ||
							"5".equals(avcLevel) ||
							"5.0".equals(avcLevel) ||
							"5.1".equals(avcLevel) ||
							"5.2".equals(avcLevel)
						) &&
						width > 0 &&
						height > 0
					) {
						int maxref;
						if (renderer == null || renderer.isPS3()) {
							/**
							 * 2013-01-25: Confirmed maximum reference frames on PS3:
							 *    - 4 for 1920x1080
							 *    - 11 for 1280x720
							 * Meaning this math is correct
							 */
							maxref = (int) Math.floor(10252743 / (double) (width * height));
						} else {
							/**
							 * This is the math for level 4.1, which results in:
							 *    - 4 for 1920x1080
							 *    - 9 for 1280x720
							 */
							maxref = (int) Math.floor(8388608 / (double) (width * height));
						}

						if (referenceFrameCount > maxref) {
							LOGGER.debug(
								"The file \"{}\" is not compatible with this renderer because it " +
								"can only take {} reference frames at this resolution while this " +
								"file has {} reference frames",
								f.getFilename(),
								maxref, referenceFrameCount
							);
							videoWithinH264LevelLimits = false;
						} else if (referenceFrameCount == -1) {
							LOGGER.debug(
								"The file \"{}\" may not be compatible with this renderer because " +
								"we can't get its number of reference frames",
								f.getFilename()
							);
							videoWithinH264LevelLimits = false;
						}
					} else {
						LOGGER.debug(
							"The H.264 stream inside the file \"{}\" is not compatible with this renderer",
							f.getFilename()
						);
						videoWithinH264LevelLimits = false;
					}
				} finally {
					referenceFrameCountLock.readLock().unlock();
					avcLevelLock.readLock().unlock();
				}
			}
			return videoWithinH264LevelLimits;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (getContainer() != null) {
			result.append("Container: ").append(getContainer().toUpperCase(Locale.ROOT)).append(", ");
		}
		result.append("Size: ").append(getSize());
		result.append(", Overall Bitrate: ").append(getBitrate());
		if (isVideo()) {
			result.append(", Video Tracks: ").append(getVideoTrackCount());
			result.append(", Video Codec: ").append(getCodecV());
			result.append(", Duration: ").append(getDurationString());
			result.append(", Video Resolution: ").append(getWidth()).append(" x ").append(getHeight());
			if (aspectRatioContainer != null) {
				result.append(", Display Aspect Ratio: ").append(getAspectRatioContainer());
			}
			if (!"1.000".equals(getPixelAspectRatio())) {
				result.append(", Pixel Aspect Ratio: ").append(getPixelAspectRatio());
			}
			if (scanType != null) {
				result.append(", Scan Type: ").append(getScanType());
			}
			if (scanOrder != null) {
				result.append(", Scan Order: ").append(getScanOrder());
			}
			if (isNotBlank(getFrameRate())) {
				result.append(", Frame Rate: ").append(getFrameRate());
			}
			if (isNotBlank(getFrameRateOriginal())) {
				result.append(", Original Frame Rate: ").append(getFrameRateOriginal());
			}
			if (isNotBlank(getFrameRateMode())) {
				result.append(", Frame Rate Mode: ");
				result.append(getFrameRateMode());
				if (isNotBlank(getFrameRateModeRaw())) {
					result.append(" (").append(getFrameRateModeRaw()).append(")");
				}
			}
			if (isNotBlank(getFrameRateModeRaw())) {
				result.append(", Frame Rate Mode Raw: ");
				result.append(getFrameRateModeRaw());
			}
			if (isNotBlank(getMuxingMode())) {
				result.append(", Muxing Mode: ").append(getMuxingMode());
			}
			if (isNotBlank(getMatrixCoefficients())) {
				result.append(", Matrix Coefficients: ").append(getMatrixCoefficients());
			}
			if (getReferenceFrameCount() > -1) {
				result.append(", Reference Frame Count: ").append(getReferenceFrameCount());
			}
			if (isNotBlank(avcLevel)) {
				result.append(", AVC Level: ").append(getAvcLevel());
			}
			if (isNotBlank(h264Profile)) {
				result.append(", AVC Profile: ").append(getH264Profile());
			}
//			if (isNotBlank(getHevcLevel())) {
//				result.append(", HEVC Level: ");
//				result.append(getHevcLevel());
			if (getVideoBitDepth() != 8) {
				result.append(", Video Bit Depth: ").append(getVideoBitDepth());
			}
			if (isNotBlank(getVideoHDRFormat())) {
				result.append(", Video HDR Format: ").append(getVideoHDRFormat());
			}
			if (isNotBlank(getVideoHDRFormatForRenderer())) {
				result.append(" (").append(getVideoHDRFormatForRenderer()).append(")");
			}
			if (isNotBlank(getVideoHDRFormatCompatibility())) {
				result.append(", Video HDR Format Compatibility: ").append(getVideoHDRFormatCompatibility());
			}
			if (isNotBlank(getVideoHDRFormatCompatibilityForRenderer())) {
				result.append(" (").append(getVideoHDRFormatCompatibilityForRenderer()).append(")");
			}
			if (isNotBlank(getFileTitleFromMetadata())) {
				result.append(", File Title from Metadata: ").append(getFileTitleFromMetadata());
			}
			if (isNotBlank(getVideoTrackTitleFromMetadata())) {
				result.append(", Video Track Title from Metadata: ").append(getVideoTrackTitleFromMetadata());
			}

			if (getAudioTrackCount() > 0) {
				appendAudioTracks(result);
			}

			if (subtitleTracks != null && !subtitleTracks.isEmpty()) {
				appendSubtitleTracks(result);
			}
			if (hasVideoMetadata()) {
				if (isNotBlank(videoMetadata.getIMDbID())) {
					result.append(", IMDb ID: ").append(videoMetadata.getIMDbID());
				}
				if (isNotBlank(videoMetadata.getYear())) {
					result.append(", Year: ").append(videoMetadata.getYear());
				}
				if (isNotBlank(videoMetadata.getMovieOrShowName())) {
					result.append(", Movie/TV series name: ").append(videoMetadata.getMovieOrShowName());
				}
				if (videoMetadata.isTVEpisode()) {
					result.append(", TV season: ").append(videoMetadata.getTVSeason());
					result.append(", TV episode number: ").append(videoMetadata.getTVEpisodeNumber());
					if (isNotBlank(getVideoTrackTitleFromMetadata())) {
						result.append(", TV episode name: ").append(videoMetadata.getTVEpisodeName());
					}
				}
			}
		} else if (getAudioTrackCount() > 0) {
			result.append(", Bitrate: ").append(getBitrate());
			result.append(", Duration: ").append(getDurationString());
			appendAudioTracks(result);
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

	public void appendAudioTracks(StringBuilder sb) {
		sb.append(", Audio Tracks: ").append(getAudioTrackCount());
		for (MediaAudio audio : audioTracks) {
			if (!audio.equals(audioTracks.get(0))) {
				sb.append(",");
			}
			sb.append(" [").append(audio).append("]");
		}
	}

	public void appendSubtitleTracks(StringBuilder sb) {
		sb.append(", Subtitle Tracks: ").append(getSubTrackCount());
		for (MediaSubtitle subtitleTrack : subtitleTracks) {
			if (!subtitleTrack.equals(subtitleTracks.get(0))) {
				sb.append(",");
			}
			sb.append(" [").append(subtitleTrack).append("]");
		}
	}

	public DLNAThumbnailInputStream getThumbnailInputStream() {
		return thumb != null ? new DLNAThumbnailInputStream(thumb) : null;
	}

	public String getValidFps(boolean ratios) {
		return getValidFps(frameRate, ratios);
	}

	public MediaAudio getFirstAudioTrack() {
		if (!audioTracks.isEmpty()) {
			return audioTracks.get(0);
		}
		return null;
	}

	/**
	 * Converts the result of getAspectRatioDvdIso() to provide
	 * MEncoderVideo with a valid value for the "vaspect" option in the
	 * "-mpegopts" command.
	 *
	 * Note: Our code never uses a false value for "ratios", so unless any
	 * plugins rely on it we can simplify things by removing that parameter.
	 *
	 * @param ratios
	 * @return
	 */
	public String getAspectRatioMencoderMpegopts(boolean ratios) {
		return getAspectRatioMencoderMpegopts(aspectRatioDvdIso, ratios);
	}

	public String getResolution() {
		if (width > 0 && height > 0) {
			return width + "x" + height;
		}

		return null;
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

	public boolean isHDVideo() {
		return (width > 864 || height > 576);
	}

	public boolean isMpegTS() {
		return container != null && container.equals("mpegts");
	}

	@Override
	public MediaInfo clone() throws CloneNotSupportedException {
		MediaInfo mediaCloned = (MediaInfo) super.clone();
		mediaCloned.setAudioTracks(new ArrayList<>());
		for (MediaAudio audio : audioTracks) {
			mediaCloned.getAudioTracksList().add((MediaAudio) audio.clone());
		}

		mediaCloned.setSubtitlesTracks(new ArrayList<>());
		for (MediaSubtitle sub : subtitleTracks) {
			mediaCloned.addSubtitlesTrack((MediaSubtitle) sub.clone());
		}

		return mediaCloned;
	}

	/**
	 * @return the bitrate
	 * @since 1.50.0
	 */
	public int getBitrate() {
		return bitrate;
	}

	/**
	 * @param bitrate the bitrate to set
	 * @since 1.50.0
	 */
	public void setBitrate(int bitrate) {
		this.bitrate = bitrate;
	}

	/**
	 * @return the width
	 * @since 1.50.0
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 * @since 1.50.0
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height
	 * @since 1.50.0
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 * @since 1.50.0
	 */
	public void setHeight(int height) {
		this.height = height;
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
	 * @return the codecV
	 * @since 1.50.0
	 */
	public String getCodecV() {
		return codecV;
	}

	/**
	 * @param codecV the codecV to set
	 * @since 1.50.0
	 */
	public void setCodecV(String codecV) {
		this.codecV = codecV != null ? codecV.toLowerCase(Locale.ROOT) : null;
	}

	/**
	 * @return the frame rate
	 * @since 1.50.0
	 */
	public String getFrameRate() {
		return frameRate;
	}

	/**
	 * @return the frame rate in DLNA format
	 */
	public String getFrameRateDLNA() {
		int framerateDLNA = (int) Math.round(Double.parseDouble(frameRate));
		String framerateDLNAString = String.valueOf(framerateDLNA);
		if (scanType != null && scanType == ScanType.INTERLACED) {
			framerateDLNAString += "i";
		} else {
			framerateDLNAString += "p";
		}
		return framerateDLNAString;
	}

	/**
	 * @param frameRate the frame rate to set
	 * @since 1.50.0
	 */
	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
	}

	/**
	 * @return the frameRateOriginal
	 */
	public String getFrameRateOriginal() {
		return frameRateOriginal;
	}

	/**
	 * @param frameRateOriginal the frameRateOriginal to set
	 */
	public void setFrameRateOriginal(String frameRateOriginal) {
		this.frameRateOriginal = frameRateOriginal;
	}

	/**
	 * @return the frameRateMode
	 * @since 1.55.0
	 */
	public String getFrameRateMode() {
		return frameRateMode;
	}

	/**
	 * @param frameRateMode the frameRateMode to set
	 * @since 1.55.0
	 */
	public void setFrameRateMode(String frameRateMode) {
		this.frameRateMode = frameRateMode;
	}

	/**
	 * @return The unaltered frame rate mode
	 */
	public String getFrameRateModeRaw() {
		return frameRateModeRaw;
	}

	/**
	 * @param frameRateModeRaw the unaltered frame rate mode to set
	 */
	public void setFrameRateModeRaw(String frameRateModeRaw) {
		this.frameRateModeRaw = frameRateModeRaw;
	}

	/**
	 * @return the video bit depth
	 */
	public int getVideoBitDepth() {
		return videoBitDepth;
	}

	/**
	 * @param value the video bit depth to set
	 */
	public void setVideoBitDepth(int value) {
		this.videoBitDepth = value;
	}

	public String getVideoHDRFormat() {
		return videoHDRFormat;
	}

	public void setVideoHDRFormat(String value) {
		this.videoHDRFormat = value;
	}

	public String getVideoHDRFormatCompatibility() {
		return videoHDRFormatCompatibility;
	}

	public void setVideoHDRFormatCompatibility(String value) {
		this.videoHDRFormatCompatibility = value;
	}

	/**
	 * Uses the HDR format compatibility information to return
	 * a string that the renderer config can match if the format
	 * failed to match.
	 *
	 * Note: Sometimes HDR files have a "SDR" compatibility, this means
	 * that any player can play them, so we return null for that just like
	 * any other SDR video.
	 */
	public String getVideoHDRFormatCompatibilityForRenderer() {
		if (StringUtils.isBlank(videoHDRFormatCompatibility)) {
			return null;
		}

		String hdrFormatCompatibilityInRendererFormat = null;
		if (StringUtils.isNotBlank(videoHDRFormatCompatibility)) {
			if (videoHDRFormatCompatibility.startsWith("Dolby Vision")) {
				hdrFormatCompatibilityInRendererFormat = "dolbyvision";
			} else if (
				(
					videoHDRFormatCompatibility.startsWith("HDR10") &&
					!videoHDRFormatCompatibility.startsWith("HDR10+")
				) ||
				videoHDRFormatCompatibility.endsWith("HDR10") // match "HDR10+ Profile A / HDR10"
			) {
				hdrFormatCompatibilityInRendererFormat = "hdr10";
			} else if (videoHDRFormatCompatibility.startsWith("HDR10+")) {
				hdrFormatCompatibilityInRendererFormat = "hdr10+";
			} else if (videoHDRFormatCompatibility.startsWith("HLG")) {
				hdrFormatCompatibilityInRendererFormat = "hlg";
			}
		}

		return hdrFormatCompatibilityInRendererFormat;
	}

	/**
	 * Uses the HDR format and HDR format information
	 * to return a string that the renderer config can match.
	 */
	public String getVideoHDRFormatForRenderer() {
		if (StringUtils.isBlank(videoHDRFormat)) {
			return null;
		}

		String hdrFormatInRendererFormat = null;
		if (StringUtils.isNotBlank(videoHDRFormat)) {
			if (videoHDRFormat.startsWith("Dolby Vision")) {
				hdrFormatInRendererFormat = "dolbyvision";
			} else if (videoHDRFormat.startsWith("HDR10+")) {
				hdrFormatInRendererFormat = "hdr10+";
			} else if (videoHDRFormat.startsWith("HDR10")) {
				hdrFormatInRendererFormat = "hdr10";
			} else if (videoHDRFormat.startsWith("HLG")) {
				hdrFormatInRendererFormat = "hlg";
			}
		}

		return hdrFormatInRendererFormat;
	}

/*
	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String value) {
		this.year = value;
	}

	public String getTVSeriesStartYear() {
		return tvSeriesStartYear;
	}

	public void setTVSeriesStartYear(String value) {
		this.tvSeriesStartYear = value;
	}

	public String getMovieOrShowName() {
		return tvShowName;
	}

	public void setMovieOrShowName(String value) {
		this.tvShowName = value;
	}

	public String getSimplifiedMovieOrShowName() {
		return simplifiedTvShowName;
	}

	public void setSimplifiedMovieOrShowName(String value) {
		this.simplifiedTvShowName = value;
	}

	public String getTVSeason() {
		return tvSeason;
	}

	public void setTVSeason(String value) {
		this.tvSeason = value;
	}

	public String getTVEpisodeNumber() {
		return tvEpisodeNumber;
	}

	public String getTVEpisodeNumberUnpadded() {
		if (isNotBlank(tvEpisodeNumber) && tvEpisodeNumber.length() > 1 && tvEpisodeNumber.startsWith("0")) {
			return tvEpisodeNumber.substring(1);
		}
		return tvEpisodeNumber;
	}

	public void setTVEpisodeNumber(String value) {
		this.tvEpisodeNumber = value;
	}

	public String getTVEpisodeName() {
		return tvEpisodeName;
	}

	public void setTVEpisodeName(String value) {
		this.tvEpisodeName = value;
	}

	public boolean isTVEpisode() {
		return isTVEpisode;
	}

	public void setIsTVEpisode(boolean value) {
		this.isTVEpisode = value;
	}
*/

	public boolean hasVideoMetadata() {
		return videoMetadata != null;
	}

	public MediaVideoMetadata getVideoMetadata() {
		return videoMetadata;
	}

	public void setVideoMetadata(MediaVideoMetadata value) {
		videoMetadata = value;
	}

	/**
	 * @return The pixel aspect ratio.
	 */
	public String getPixelAspectRatio() {
		return pixelAspectRatio;
	}

	/**
	 * Sets the pixel aspect ratio.
	 *
	 * @param pixelAspectRatio the pixel aspect ratio to set.
	 */
	public void setPixelAspectRatio(String pixelAspectRatio) {
		this.pixelAspectRatio = pixelAspectRatio;
	}

	/**
	 * @return the {@link ScanType}.
	 */
	@Nullable
	public ScanType getScanType() {
		return scanType;
	}

	/**
	 * Sets the {@link ScanType}.
	 *
	 * @param scanType the {@link ScanType} to set.
	 */
	public void setScanType(@Nullable ScanType scanType) {
		this.scanType = scanType;
	}

	/**
	 * Sets the {@link ScanType} by parsing the specified {@link String}.
	 *
	 * @param scanType the {@link String} to parse.
	 */
	public void setScanType(@Nullable String scanType) {
		this.scanType = ScanType.typeOf(scanType);
	}

	/**
	 * @return the {@link ScanOrder}.
	 */
	@Nullable
	public ScanOrder getScanOrder() {
		return scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder}.
	 *
	 * @param scanOrder the {@link ScanOrder} to set.
	 */
	public void setScanOrder(@Nullable ScanOrder scanOrder) {
		this.scanOrder = scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder} by parsing the specified {@link String}.
	 *
	 * @param scanOrder the {@link String} to parse.
	 */
	public void setScanOrder(@Nullable String scanOrder) {
		this.scanOrder = ScanOrder.typeOf(scanOrder);
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
	 * Get the aspect ratio reported by the file/container.
	 * This is the aspect ratio that the renderer should display the video
	 * at, and is usually the same as the video track aspect ratio.
	 *
	 * @return the aspect ratio reported by the file/container
	 */
	public String getAspectRatioContainer() {
		return aspectRatioContainer;
	}

	/**
	 * Sets the aspect ratio reported by the file/container.
	 *
	 * @param aspectRatio the aspect ratio to set.
	 */
	public void setAspectRatioContainer(String aspectRatio) {
		this.aspectRatioContainer = getFormattedAspectRatio(aspectRatio);
	}

	/**
	 * Get the aspect ratio of the video track. This is the actual aspect ratio
	 * of the pixels, which is not always the aspect ratio that the renderer
	 * should display or that we should output; that is
	 * {@link #getAspectRatioContainer()}
	 *
	 * @return the aspect ratio of the video track
	 */
	public String getAspectRatioVideoTrack() {
		return aspectRatioVideoTrack;
	}

	/**
	 * @param aspectRatio the aspect ratio to set
	 */
	public void setAspectRatioVideoTrack(String aspectRatio) {
		this.aspectRatioVideoTrack = getFormattedAspectRatio(aspectRatio);
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

	public String getMatrixCoefficients() {
		return matrixCoefficients;
	}

	public void setMatrixCoefficients(String matrixCoefficients) {
		this.matrixCoefficients = matrixCoefficients;
	}

	public String getFileTitleFromMetadata() {
		return fileTitleFromMetadata;
	}

	public void setFileTitleFromMetadata(String value) {
		this.fileTitleFromMetadata = value;
	}

	public String getVideoTrackTitleFromMetadata() {
		return videoTrackTitleFromMetadata;
	}

	public void setVideoTrackTitleFromMetadata(String value) {
		this.videoTrackTitleFromMetadata = value;
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
		if (imageInfo != null && imageInfo.getWidth() > 0 && imageInfo.getHeight() > 0) {
			setWidth(imageInfo.getWidth());
			setHeight(imageInfo.getHeight());
		}
	}

	/**
	 * @return reference frame count for video stream or {@code -1} if not parsed.
	 */
	public byte getReferenceFrameCount() {
		referenceFrameCountLock.readLock().lock();
		try {
			return referenceFrameCount;
		} finally {
			referenceFrameCountLock.readLock().unlock();
		}
	}

	/**
	 * Sets reference frame count for video stream or {@code -1} if not parsed.
	 *
	 * @param referenceFrameCount reference frame count.
	 */
	public void setReferenceFrameCount(byte referenceFrameCount) {
		if (referenceFrameCount < -1) {
			throw new IllegalArgumentException("referenceFrameCount must be >= -1.");
		}
		referenceFrameCountLock.writeLock().lock();
		try {
			this.referenceFrameCount = referenceFrameCount;
		} finally {
			referenceFrameCountLock.writeLock().unlock();
		}
	}

	/**
	 * @return AVC level for video stream or {@code null} if not parsed.
	 */
	public String getAvcLevel() {
		avcLevelLock.readLock().lock();
		try {
			return avcLevel;
		} finally {
			avcLevelLock.readLock().unlock();
		}
	}

	/**
	 * Sets AVC level for video stream or {@code null} if not parsed.
	 *
	 * @param avcLevel AVC level.
	 */
	public void setAvcLevel(String avcLevel) {
		avcLevelLock.writeLock().lock();
		try {
			this.avcLevel = avcLevel;
		} finally {
			avcLevelLock.writeLock().unlock();
		}
	}

	public int getAvcAsInt() {
		try {
			return Integer.parseInt(getAvcLevel().replaceAll("\\.", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getH264Profile() {
		synchronized (h264ProfileLock) {
			return h264Profile;
		}
	}

	public void setH264Profile(String s) {
		synchronized (h264ProfileLock) {
			h264Profile = s;
		}
	}

	/**
	 * @return the audioTracks
	 * @since 1.60.0
	 */
	// TODO (breaking change): rename to getAudioTracks
	public List<MediaAudio> getAudioTracksList() {
		return audioTracks;
	}

	/**
	 * @param audioTracks the audioTracks to set
	 * @since 1.60.0
	 */
	public void setAudioTracks(List<MediaAudio> audioTracks) {
		this.audioTracks = audioTracks;
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
	public void setChapters(List<MediaChapter> chapters) {
		this.chapters = chapters;
	}

	/**
	 *
	 * @param chapter the chapter to add
	 */
	public void addChapter(MediaChapter chapter) {
		this.chapters.add(chapter);
	}

	/**
	 *
	 * @return true if has chapter
	 */
	public boolean hasChapters() {
		return !chapters.isEmpty();
	}

	/**
	 *
	 * @return the json string representation of chapters
	 */
	public String getChaptersToJson() {
		return GSON.toJson(chapters);
	}

	/**
	 *
	 * @param json the json string representation of chapters to set
	 */
	public void setChaptersFromJson(String json) {
		if (StringUtils.isNotBlank(json)) {
			try {
				MediaChapter[] jsonChapters = new MediaChapter[0];
				jsonChapters = GSON.fromJson(json, jsonChapters.getClass());
				if (jsonChapters.length > 0) {
					chapters = Arrays.asList(jsonChapters);
				}
			} catch (JsonIOException e) {
				LOGGER.debug("Could not parsejson string representation of chapters");
			}
		}
	}

	/**
	 * @return The Exif orientation or {@code 1} if unknown.
	 * @since 1.50.0
	 */
	public ExifOrientation getExifOrientation() {
		return imageInfo != null ? imageInfo.getExifOrientation() : ExifOrientation.TOP_LEFT;
	}

	/**
	 * @return the muxingMode
	 * @since 1.50.0
	 */
	public String getMuxingMode() {
		return muxingMode;
	}

	/**
	 * @param muxingMode the muxingMode to set
	 * @since 1.50.0
	 */
	public void setMuxingMode(String muxingMode) {
		this.muxingMode = muxingMode;
	}

	/**
	 * @return the muxingModeAudio
	 * @since 1.50.0
	 */
	public String getMuxingModeAudio() {
		return muxingModeAudio;
	}

	/**
	 * @param muxingModeAudio the muxingModeAudio to set
	 * @since 1.50.0
	 */
	public void setMuxingModeAudio(String muxingModeAudio) {
		this.muxingModeAudio = muxingModeAudio;
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

	/**
	 * @return the h264_annexB
	 * @since 1.50.0
	 */
	public byte[] getH264AnnexB(InputFile f) {
		synchronized (h264AnnexBLock) {
			if (!h264AnnexBParsed) {
				byte[][] headers = FFmpegParser.getAnnexBFrameHeader(f);
				if (headers != null) {
					h264AnnexB = headers[1];
				}
				h264AnnexBParsed = true;
			}
			if (h264AnnexB == null) {
				return null;
			}
			byte[] result = new byte[h264AnnexB.length];
			System.arraycopy(h264AnnexB, 0, result, 0, h264AnnexB.length);
			return result;
		}
	}

	/**
	 * @param h264AnnexB the h264_annexB to set
	 * @since 1.50.0
	 */
	public void setH264AnnexB(byte[] h264AnnexB) {
		synchronized (h264AnnexBLock) {
			if (h264AnnexB == null) {
				this.h264AnnexB = null;
			} else {
				this.h264AnnexB = new byte[h264AnnexB.length];
				System.arraycopy(h264AnnexB, 0, this.h264AnnexB, 0, h264AnnexB.length);
			}
		}
	}

	/**
	 * @return the mediaparsed
	 * @since 1.50.0
	 */
	public boolean isMediaparsed() {
		return mediaparsed;
	}

	/**
	 * @param mediaparsed the mediaparsed to set
	 * @since 1.50.0
	 */
	public void setMediaparsed(boolean mediaparsed) {
		this.mediaparsed = mediaparsed;
	}

	public boolean isFFmpegparsed() {
		return ffmpegparsed;
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

	/**
	 * @return the dvdtrack
	 * @since 1.50.0
	 */
	public int getDvdtrack() {
		return dvdtrack;
	}

	/**
	 * @param dvdtrack the dvdtrack to set
	 * @since 1.50.0
	 */
	public void setDvdtrack(int dvdtrack) {
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
	 * @return the encrypted
	 * @since 1.50.0
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * @param encrypted the encrypted to set
	 * @since 1.50.0
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public boolean isMod4() {
		return (height % 4 == 0 && width % 4 == 0);
	}

	/**
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @return whether the video track is 3D
	 */
	public boolean is3d() {
		return isNotBlank(stereoscopy);
	}

	/**
	 * The significance of this is that the aspect ratio should not be kept
	 * in this case when transcoding.
	 * Example: 3840x1080 should be resized to 1920x1080, not 1920x540.
	 *
	 * @return whether the video track is full SBS or OU 3D
	 */
	public boolean is3dFullSbsOrOu() {
		if (!is3d()) {
			return false;
		}

		return switch (stereoscopy.toLowerCase()) {
			case "overunderrt",
				"oulf",
				"ourf",
				"sbslf",
				"sbsrf",
				"top-bottom (left eye first)",
				"top-bottom (right eye first)",
				"side by side (left eye first)",
				"side by side (right eye first)" -> true;
			default -> false;
		};
	}

	/**
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @return the type of stereoscopy (3D) of the video track
	 */
	public String getStereoscopy() {
		return stereoscopy;
	}

	/**
	 * Sets the type of stereoscopy (3D) of the video track.
	 *
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @param stereoscopy the type of stereoscopy (3D) of the video track
	 */
	public void setStereoscopy(String stereoscopy) {
		this.stereoscopy = stereoscopy;
	}

	public Mode3D get3DLayout() {
		if (!is3d()) {
			return null;
		}

		isAnaglyph = true;
		switch (stereoscopy.toLowerCase()) {
			case "overunderrt", "oulf", "top-bottom (left eye first)" -> {
				isAnaglyph = false;
				return Mode3D.ABL;
			}
			case "ourf", "top-bottom (right eye first)" -> {
				isAnaglyph = false;
				return Mode3D.ABR;
			}
			case "sbslf", "side by side (left eye first)" -> {
				isAnaglyph = false;
				return Mode3D.SBSL;
			}
			case "sbsrf", "side by side (right eye first)" -> {
				isAnaglyph = false;
				return Mode3D.SBSR;
			}
			case "half top-bottom (left eye first)" -> {
				isAnaglyph = false;
				return Mode3D.AB2L;
			}
			case "half side by side (left eye first)" -> {
				isAnaglyph = false;
				return Mode3D.SBS2L;
			}
			case "arcg" -> {
				return Mode3D.ARCG;
			}
			case "arch" -> {
				return Mode3D.ARCH;
			}
			case "arcc" -> {
				return Mode3D.ARCC;
			}
			case "arcd" -> {
				return Mode3D.ARCD;
			}
			case "agmg" -> {
				return Mode3D.AGMG;
			}
			case "agmh" -> {
				return Mode3D.AGMH;
			}
			case "agmc" -> {
				return Mode3D.AGMC;
			}
			case "agmd" -> {
				return Mode3D.AGMD;
			}
			case "aybg" -> {
				return Mode3D.AYBG;
			}
			case "aybh" -> {
				return Mode3D.AYBH;
			}
			case "aybc" -> {
				return Mode3D.AYBC;
			}
			case "aybd" -> {
				return Mode3D.AYBD;
			}
			default -> {
				return null;
			}
		}
	}

	private boolean isAnaglyph;

	public boolean stereoscopyIsAnaglyph() {
		get3DLayout();
		return isAnaglyph;
	}

	public boolean isDVDResolution() {
		return (width == 720 && height == 576) || (width == 720 && height == 480);
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



	private static Double parseDurationString(String duration) {
		return duration != null ? StringUtil.convertStringToTime(duration) : null;
	}

	/**
	 * Extracts key value.
	 *
	 * @param key
	 * @return If key is not available or blanc, NULL will be returned, otherwise string key value
	 */
	private static String extractAudioTagKeyValue(Tag t, FieldKey key) {
		try {
			String value = t.getFirst(key);
			if (StringUtils.isAllBlank(value)) {
				LOGGER.trace("tag field is blanc");
				return null;
			}
			return value;
		} catch (KeyNotFoundException e) {
			LOGGER.trace("tag field not found", e);
			return null;
		}
	}

	/**
	 * Extracts key value and converts it to Integer.
	 *
	 * @param t
	 * @param key
	 * @param defaultValue
	 * @return	If key is not available or blanc, defaultValue will be returned
	 */
	private static Integer extractAudioTagKeyIntegerValue(Tag t, FieldKey key, Integer defaultValue) {
		String value = extractAudioTagKeyValue(t, key);
		if (value != null) {
			try {
				return Integer.valueOf(value);
			} catch (NumberFormatException e) {
				LOGGER.trace("no int value available for key ", e);
			}
		}
		return defaultValue;
	}

	public static boolean isMuxable(String filename, String codecA) {
		return codecA != null && (codecA.startsWith("dts") || codecA.equals("dca"));
	}

	public static boolean isLossless(String codecA) {
		return codecA != null && (codecA.contains("pcm") || codecA.startsWith("dts") || codecA.equals("dca") || codecA.contains("flac")) && !codecA.contains("pcm_u8") && !codecA.contains("pcm_s8");
	}

	private static String getValidFps(String frameRate, boolean ratios) {
		if (frameRate != null && frameRate.length() > 0) {
			try {
				double fr = Double.parseDouble(frameRate.replace(',', '.'));

				if (fr >= 14.99 && fr < 15.1) {
					return "15";
				} else if (fr > 23.9 && fr < 23.99) {
					return ratios ? "24000/1001" : "23.976";
				} else if (fr > 23.99 && fr < 24.1) {
					return "24";
				} else if (fr >= 24.99 && fr < 25.1) {
					return "25";
				} else if (fr > 29.9 && fr < 29.99) {
					return ratios ? "30000/1001" : "29.97";
				} else if (fr >= 29.99 && fr < 30.1) {
					return "30";
				} else if (fr > 47.9 && fr < 47.99) {
					return ratios ? "48000/1001" : "47.952";
				} else if (fr > 49.9 && fr < 50.1) {
					return "50";
				} else if (fr > 59.8 && fr < 59.99) {
					return ratios ? "60000/1001" : "59.94";
				} else if (fr >= 59.99 && fr < 60.1) {
					return "60";
				}
			} catch (NumberFormatException nfe) {
				LOGGER.error(null, nfe);
			}
		}

		return null;
	}

	private static String getAspectRatioMencoderMpegopts(String aspectRatioDvdIso, boolean ratios) {
		if (aspectRatioDvdIso != null) {
			double aspectRatio = Double.parseDouble(aspectRatioDvdIso);

			if (aspectRatio > 1.7 && aspectRatio < 1.8) {
				return ratios ? "16/9" : "1.777777777777777";
			}

			if (aspectRatio > 1.3 && aspectRatio < 1.4) {
				return ratios ? "4/3" : "1.333333333333333";
			}
		}
		return null;
	}

	/**
	 * This takes an exact aspect ratio, and returns the closest common aspect
	 * ratio to that, so that e.g. 720x416 and 720x420 are the same.
	 *
	 * @param aspect
	 * @return an approximate aspect ratio
	 */
	private static String getFormattedAspectRatio(String aspect) {
		if (isBlank(aspect)) {
			return null;
		}

		if (aspect.contains(":")) {
			return aspect;
		}

		double exactAspectRatio = Double.parseDouble(aspect);
		if (exactAspectRatio >= 11.9 && exactAspectRatio <= 12.1) {
			return "12.00:1";
		} else if (exactAspectRatio >= 3.9 && exactAspectRatio <= 4.1) {
			return "4.00:1";
		} else if (exactAspectRatio >= 2.75 && exactAspectRatio <= 2.77) {
			return "2.76:1";
		} else if (exactAspectRatio >= 2.65 && exactAspectRatio <= 2.67) {
			return "24:9";
		} else if (exactAspectRatio >= 2.58 && exactAspectRatio <= 2.6) {
			return "2.59:1";
		} else if (exactAspectRatio >= 2.54  && exactAspectRatio <= 2.56) {
			return "2.55:1";
		} else if (exactAspectRatio >= 2.38 && exactAspectRatio <= 2.41) {
			return "2.39:1";
		} else if (exactAspectRatio > 2.36 && exactAspectRatio < 2.38) {
			return "2.37:1";
		} else if (exactAspectRatio >= 2.34 && exactAspectRatio <= 2.36) {
			return "2.35:1";
		} else if (exactAspectRatio >= 2.33 && exactAspectRatio < 2.34) {
			return "21:9";
		} else if (exactAspectRatio > 2.1  && exactAspectRatio < 2.3) {
			return "11:5";
		} else if (exactAspectRatio > 1.9 && exactAspectRatio < 2.1) {
			return "2.00:1";
		} else if (exactAspectRatio > 1.87  && exactAspectRatio <= 1.9) {
			return "1.896:1";
		} else if (exactAspectRatio >= 1.83 && exactAspectRatio <= 1.87) {
			return "1.85:1";
		} else if (exactAspectRatio >= 1.7 && exactAspectRatio <= 1.8) {
			return "16:9";
		} else if (exactAspectRatio >= 1.65 && exactAspectRatio <= 1.67) {
			return "15:9";
		} else if (exactAspectRatio >= 1.59 && exactAspectRatio <= 1.61) {
			return "16:10";
		} else if (exactAspectRatio >= 1.54 && exactAspectRatio <= 1.56) {
			return "14:9";
		} else if (exactAspectRatio >= 1.49 && exactAspectRatio <= 1.51) {
			return "3:2";
		} else if (exactAspectRatio > 1.42 && exactAspectRatio < 1.44) {
			return "1.43:1";
		} else if (exactAspectRatio > 1.372 && exactAspectRatio < 1.4) {
			return "11:8";
		} else if (exactAspectRatio > 1.35 && exactAspectRatio <= 1.372) {
			return "1.37:1";
		} else if (exactAspectRatio >= 1.3 && exactAspectRatio <= 1.35) {
			return "4:3";
		} else if (exactAspectRatio > 1.2 && exactAspectRatio < 1.3) {
			return "5:4";
		} else if (exactAspectRatio >= 1.18 && exactAspectRatio <= 1.195) {
			return "19:16";
		} else if (exactAspectRatio > 0.99 && exactAspectRatio < 1.1) {
			return "1:1";
		} else if (exactAspectRatio > 0.7 && exactAspectRatio < 0.9) {
			return "4:5";
		} else if (exactAspectRatio > 0.6 && exactAspectRatio < 0.7) {
			return "2:3";
		} else if (exactAspectRatio > 0.5 && exactAspectRatio < 0.6) {
			return "9:16";
		} else {
			return aspect;
		}
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
