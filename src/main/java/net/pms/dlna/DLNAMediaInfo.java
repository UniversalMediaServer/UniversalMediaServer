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
package net.pms.dlna;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import net.pms.util.CoverSupplier;
import net.pms.util.CoverUtil;
import net.pms.util.FileUtil;
import net.pms.util.ImagesUtil;
import net.pms.util.ImagesUtil.ImageFormat;
import net.pms.util.ImagesUtil.ScaleType;
import net.pms.util.MpegUtil;
import net.pms.util.ProcessUtil;
import static net.pms.util.StringUtil.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.imaging.ImageInfo.ColorType;
import org.apache.commons.imaging.ImageReadException;
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
 *
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class DLNAMediaInfo implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAMediaInfo.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public static final long ENDFILE_POS = 99999475712L;

	/**
	 * Maximum size of a stream, taking into account that some renderers (like
	 * the PS3) will convert this <code>long</code> to <code>int</code>.
	 * Truncating this value will still return the maximum value that an
	 * <code>int</code> can contain.
	 */
	public static final long TRANS_SIZE = Long.MAX_VALUE - Integer.MAX_VALUE - 1;

	private final Object videoWithinH264LevelLimitsLock = new Object();
	private Boolean videoWithinH264LevelLimits = null;

	// Stored in database
	private Double durationSec;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int bitrate;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int width;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int height;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public long size;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String codecV;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String frameRate;

	private String frameRateMode;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String aspect;

	public String aspectRatioDvdIso;
	public String aspectRatioContainer;
	public String aspectRatioVideoTrack;
	private int videoBitDepth = 8;

	private volatile DLNAThumbnail thumb = null;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String mimeType;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int bitsPerPixel;

	private final ReentrantReadWriteLock referenceFrameCountLock = new ReentrantReadWriteLock();
	private byte referenceFrameCount = -1;

	private final ReentrantReadWriteLock avcLevelLock = new ReentrantReadWriteLock();
	private String avcLevel = null;

	private final Object h264ProfileLock = new Object();
	private String h264Profile = null;

	private List<DLNAMediaAudio> audioTracks = new ArrayList<>();
	private List<DLNAMediaSubtitle> subtitleTracks = new ArrayList<>();

	private boolean externalSubsExist = false;

	public void setExternalSubsExist(boolean exist) {
		this.externalSubsExist = exist;
	}

	public boolean isExternalSubsExist() {
		return externalSubsExist;
	}

	private boolean externalSubsParsed = false;

	public void setExternalSubsParsed(boolean parsed) {
		this.externalSubsParsed = parsed;
	}

	public boolean isExternalSubsParsed() {
		return externalSubsParsed;
	}

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String model;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int exposure;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int orientation;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int iso;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String muxingMode;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String muxingModeAudio;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String container;

	private final Object h264_annexBLock = new Object();
	private byte[] h264_annexB;

	/**
	 * Not stored in database.
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean mediaparsed;

	public boolean ffmpegparsed;

	/**
	 * isUseMediaInfo-related, used to manage thumbnail management separated
	 * from the main parsing process.
	 *
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public volatile boolean thumbready;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public int dvdtrack;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean secondaryFormatValid = true;

	private final Object parsingLock = new Object();
	private boolean parsing = false;

	private final Object ffmpeg_failureLock = new Object();
	private boolean ffmpeg_failure = false;

	private final Object ffmpeg_annexb_failureLock = new Object();
	private boolean ffmpeg_annexb_failure;
	private boolean muxable;
	private Map<String, String> extras;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean encrypted;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String matrixCoefficients;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String stereoscopy;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String fileTitleFromMetadata;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String videoTrackTitleFromMetadata;

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
		return getMediaType().equals(MediaType.VIDEO);
	}

	public boolean isAudio() {
		return getMediaType().equals(MediaType.AUDIO);
	}

	public boolean hasAudio() {
		return audioTracks.size() > 0;
	}

	public MediaType getMediaType() {
		if (videoTrackCount > 0) {
			return MediaType.VIDEO;
		} else {
			switch (audioTracks.size()) {
				case 1 :
					return MediaType.AUDIO;
				case 0 :
					if (imageCount > 0) {
						return MediaType.IMAGE;
					}
				default :
					return MediaType.UNKNOWN;
			}
		}
	}

	/**
	 * @return true when there are subtitle tracks embedded in the media file.
	 */
	public boolean hasSubtitles() {
		return subtitleTracks.size() > 0;
	}

	public boolean isImage() {
		return getMediaType().equals(MediaType.IMAGE);
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
	 * @param mediaRenderer The renderer we might mux to
	 *
	 * @return
	 */
	public boolean isMuxable(RendererConfiguration mediaRenderer) {
		// Make sure the file is H.264 video
		if (isH264()) {
			muxable = true;
		}

		// Check if the renderer supports the resolution of the video
		if (
			(
				mediaRenderer.isMaximumResolutionSpecified() &&
				(
					width > mediaRenderer.getMaxVideoWidth() ||
					height > mediaRenderer.getMaxVideoHeight()
				)
			) ||
			(
				!mediaRenderer.isMuxNonMod4Resolution() &&
				!isMod4()
			)
		) {
			muxable = false;
		}

		// Temporary fix: MediaInfo support will take care of this in the future
		// For now, http://ps3mediaserver.org/forum/viewtopic.php?f=11&t=6361&start=0
		// Bravia does not support AVC video at less than 288px high
		if (mediaRenderer.isBRAVIA() && height < 288) {
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
		if (
			(
				getFileTitleFromMetadata() != null &&
				getFileTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				getVideoTrackTitleFromMetadata() != null &&
				getVideoTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				params.aid != null &&
				params.aid.getAudioTrackTitleFromMetadata() != null &&
				params.aid.getAudioTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			) ||
			(
				params.sid != null &&
				params.sid.getSubtitlesTrackTitleFromMetadata() != null &&
				params.sid.getSubtitlesTrackTitleFromMetadata().toLowerCase().replaceAll("\\-", "").contains("webdl")
			)
		) {
			return true;
		}

		return false;
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

	public String getExtrasAsString() {
		if (extras == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : extras.entrySet()) {
			sb.append(entry.getKey());
			sb.append('|');
			sb.append(entry.getValue());
			sb.append('|');
		}

		return sb.toString();
	}

	public void setExtrasAsString(String value) {
		if (value != null) {
			StringTokenizer st = new StringTokenizer(value, "|");

			while (st.hasMoreTokens()) {
				try {
					putExtra(st.nextToken(), st.nextToken());
				} catch (NoSuchElementException nsee) {
					LOGGER.debug("Caught exception", nsee);
				}
			}
		}
	}

	@Deprecated
	public void generateThumbnail(InputFile input, Format ext, int type, Double seekPosition, boolean resume) {
		generateThumbnail(input, ext, type, seekPosition, resume, null);
	}

	public void generateThumbnail(InputFile input, Format ext, int type, Double seekPosition, boolean resume, RendererConfiguration renderer) {
		DLNAMediaInfo forThumbnail = new DLNAMediaInfo();
		forThumbnail.setMediaparsed(mediaparsed);  // check if file was already parsed by MediaInfo
		forThumbnail.durationSec = getDurationInSeconds();
		if (seekPosition <= forThumbnail.durationSec) {
			forThumbnail.durationSec = seekPosition;
		} else {
			forThumbnail.durationSec /= 2;
		}

		forThumbnail.parse(input, ext, type, true, resume, renderer);
		thumb = forThumbnail.thumb;
		thumbready = true;
	}

	private ProcessWrapperImpl getFFmpegThumbnail(InputFile media, boolean resume, RendererConfiguration renderer) {
		/**
		 * Note: The text output from FFmpeg is used by renderers that do
		 * not use MediaInfo, so do not make any changes that remove or
		 * minimize the amount of text given by FFmpeg here
		 */
		String args[] = new String[14];
		args[0] = getFfmpegPath();
		File file = media.getFile();
		boolean dvrms = file != null && file.getAbsolutePath().toLowerCase().endsWith("dvr-ms");

		if (dvrms && isNotBlank(configuration.getFfmpegAlternativePath())) {
			args[0] = configuration.getFfmpegAlternativePath();
		}

		args[1] = "-ss";
		if (resume) {
			args[2] = Integer.toString((int) getDurationInSeconds());
		} else {
			args[2] = Integer.toString((int) Math.min(configuration.getThumbnailSeekPos(), getDurationInSeconds()));
		}

		args[3] = "-i";

		if (file != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-an";
		args[6] = "-an";

		// Thumbnail resolution
		int thumbnailWidth  = 320;
		int thumbnailHeight = 180;
		double thumbnailRatio  = 1.78;
		boolean isThumbnailPadding = true;
		if (renderer != null) {
			thumbnailWidth     = renderer.getThumbnailWidth();
			thumbnailHeight    = renderer.getThumbnailHeight();
			thumbnailRatio     = renderer.getThumbnailRatio();
			isThumbnailPadding = renderer.isThumbnailPadding();
		}

		if (isThumbnailPadding) {
			args[7] = "-vf";
			args[8] = "scale='if(gt(a," + thumbnailRatio + ")," + thumbnailWidth + ",-1)':'if(gt(a," + thumbnailRatio + "),-1," + thumbnailHeight + ")', pad=" + thumbnailWidth + ":" + thumbnailHeight + ":(" + thumbnailWidth + "-iw)/2:(" + thumbnailHeight + "-ih)/2";
		} else {
			args[7] = "-vf";
			args[8] = "scale='if(gt(a," + thumbnailRatio + ")," + thumbnailWidth + ",-1)':'if(gt(a," + thumbnailRatio + "),-1," + thumbnailHeight + ")'";
		}

		args[9] = "-vframes";
		args[10] = "1";
		args[11] = "-f";
		args[12] = "image2";
		args[13] = "pipe:";

		// FIXME MPlayer should not be used if thumbnail generation is disabled
		if (!configuration.isThumbnailGenerationEnabled() || !renderer.isThumbnails() || (configuration.isUseMplayerForVideoThumbs() && !dvrms)) {
			args[2] = "0";
			for (int i = 5; i <= 13; i++) {
				args[i] = "-an";
			}
		}

		OutputParams params = new OutputParams(configuration);
		params.maxBufferSize = 1;
		params.stdin = media.getPush();
		params.noexitcheck = true; // not serious if anything happens during the thumbnailer

		// true: consume stderr on behalf of the caller i.e. parse()
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args, params, false, true);

		// FAILSAFE
		synchronized (parsingLock) {
			parsing = true;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
					synchronized (ffmpeg_failureLock) {
						ffmpeg_failure = true;
					}
				} catch (InterruptedException e) { }

				pw.stopProcess();
				synchronized (parsingLock) {
					parsing = false;
				}
			}
		};

		Thread failsafe = new Thread(r, "FFmpeg Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		synchronized (parsingLock) {
			parsing = false;
		}
		return pw;
	}

	private ProcessWrapperImpl getMplayerThumbnail(InputFile media, boolean resume, RendererConfiguration renderer) throws IOException {
		File file = media.getFile();
		String args[] = new String[14];
		args[0] = configuration.getMplayerPath();
		args[1] = "-ss";
		if (resume) {
			args[2] = "" + (int) getDurationInSeconds();
		} else {
			args[2] = "" + configuration.getThumbnailSeekPos();
		}

		args[3] = "-quiet";

		if (file != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-msglevel";
		args[6] = "all=4";

		int thumbnailWidth  = 320;
		int thumbnailHeight = 180;
		boolean isThumbnailPadding = true;
		if (renderer != null) {
			thumbnailWidth     = renderer.getThumbnailWidth();
			thumbnailHeight    = renderer.getThumbnailHeight();
			isThumbnailPadding = renderer.isThumbnailPadding();
		}
		if (isThumbnailPadding) {
			args[7] = "-vf";
			args[8] = "scale=" + thumbnailWidth + ":-2,expand=:" + thumbnailHeight;
		} else {
			args[7] = "-vf";
			args[8] = "scale=" + thumbnailWidth + ":-2";
		}

		args[9] = "-frames";
		args[10] = "1";
		args[11] = "-vo";
		String frameName = "" + media.hashCode();
		frameName = "mplayer_thumbs:subdirs=\"" + frameName + "\"";
		frameName = frameName.replace(',', '_');
		args[12] = "jpeg:outdir=" + frameName;
		args[13] = "-nosound";
		OutputParams params = new OutputParams(configuration);
		params.workDir = configuration.getTempFolder();
		params.maxBufferSize = 1;
		params.stdin = media.getPush();
		params.log = true;
		params.noexitcheck = true; // not serious if anything happens during the thumbnailer
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args, params);

		// FAILSAFE
		synchronized (parsingLock) {
			parsing = true;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) { }

				pw.stopProcess();
				synchronized (parsingLock) {
					parsing = false;
				}
			}
		};

		Thread failsafe = new Thread(r, "MPlayer Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		synchronized (parsingLock) {
			parsing = false;
		}
		return pw;
	}

	private String getFfmpegPath() {
		String value = configuration.getFfmpegPath();

		if (value == null) {
			LOGGER.info("No FFmpeg - unable to thumbnail");
			throw new RuntimeException("No FFmpeg - unable to thumbnail");
		} else {
			return value;
		}
	}

	@Deprecated
	public void parse(InputFile inputFile, Format ext, int type, boolean thumbOnly, boolean resume) {
		parse(inputFile, ext, type, thumbOnly, resume, null);
	}

	/**
	 * Parse media without using MediaInfo.
	 */
	public void parse(InputFile inputFile, Format ext, int type, boolean thumbOnly, boolean resume, RendererConfiguration renderer) {
		int i = 0;

		while (isParsing()) {
			if (i == 5) {
				mediaparsed = true;
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }

			i++;
		}

		if (isMediaparsed() && !thumbOnly) { // file could be already parsed by MediaInfo and we need only thumbnail
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
			boolean ffmpeg_parsing = true;

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				ffmpeg_parsing = false;
				DLNAMediaAudio audio = new DLNAMediaAudio();

				if (file != null) {
					try {
						AudioFile af;
						if ("mp2".equals(FileUtil.getExtension(file).toLowerCase(Locale.ROOT))) {
							af = AudioFileIO.readAs(file, "mp3");
						} else {
							af = AudioFileIO.read(file);
						}
						AudioHeader ah = af.getAudioHeader();

						if (ah != null && !thumbOnly) {
							int length = ah.getTrackLength();
							int rate = ah.getSampleRateAsNumber();

							if (ah.getEncodingType().toLowerCase().contains("flac 24")) {
								audio.setBitsperSample(24);
							}

							audio.setSampleFrequency("" + rate);
							durationSec = (double) length;
							bitrate = (int) ah.getBitRateAsNumber();
							audio.getAudioProperties().setNumberOfChannels(2); // set default value of channels to 2
							String channels = ah.getChannels().toLowerCase();
							if (!channels.isEmpty()) {
								if (channels.equals("1") || channels.contains("mono")) { // parse value "1" or "Mono"
									audio.getAudioProperties().setNumberOfChannels(1);
								} else if (!(channels.equals("2") || channels.contains("stereo"))){ // this value is default so try to parse the unknown value
									try {
										audio.getAudioProperties().setNumberOfChannels(Integer.parseInt(channels));
									} catch (NumberFormatException e) {
										LOGGER.debug("Could not parse number of audio channels from \"{}\"", channels);
									}
								}
							}

							audio.setCodecA(ah.getEncodingType());

							if (audio.getCodecA().contains("(windows media")) {
								audio.setCodecA(audio.getCodecA().substring(0, audio.getCodecA().indexOf("(windows media")).trim());
							}
						}

						Tag t = af.getTag();

						if (t != null) {
							if (t.getArtworkList().size() > 0) {
								thumb = DLNAThumbnail.toThumbnail(t.getArtworkList().get(0).getBinaryData());
							} else if (!configuration.getAudioThumbnailMethod().equals(CoverSupplier.NONE)) {
								thumb = DLNAThumbnail.toThumbnail(CoverUtil.get().getThumbnail(t));
							}
							if (thumb != null) {
								thumbready = true;
							}

							if (!thumbOnly) {
								audio.setAlbum(t.getFirst(FieldKey.ALBUM));
								audio.setArtist(t.getFirst(FieldKey.ARTIST));
								audio.setSongname(t.getFirst(FieldKey.TITLE));
								String y = t.getFirst(FieldKey.YEAR);

								try {
									if (y.length() > 4) {
										y = y.substring(0, 4);
									}
									audio.setYear(Integer.parseInt(((y != null && y.length() > 0) ? y : "0")));
									y = t.getFirst(FieldKey.TRACK);
									audio.setTrack(Integer.parseInt(((y != null && y.length() > 0) ? y : "1")));
									audio.setGenre(t.getFirst(FieldKey.GENRE));
								} catch (NumberFormatException | KeyNotFoundException e) {
									LOGGER.debug("Error parsing unimportant metadata: " + e.getMessage());
								}
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
						ffmpeg_parsing = false;
					}

					if (audio.getSongname() != null && audio.getSongname().length() > 0) {
						if (renderer != null && renderer.isPrependTrackNumbers() && audio.getTrack() > 0) {
							audio.setSongname(audio.getTrack() + ": " + audio.getSongname());
						}
					} else {
						audio.setSongname(file.getName());
					}

					if (!ffmpeg_parsing) {
						audioTracks.add(audio);
					}
				}
			}

			if (type == Format.IMAGE && file != null) {
				if (!thumbOnly) {
					try {
						ffmpeg_parsing = false;
						ImagesUtil.parseImageByImaging(file, this);
						container = codecV;
						imageCount++;
					} catch (IOException | ImageReadException e) {
						LOGGER.debug("Error when parsing image ({}) with Imaging, switching to FFmpeg.", file.getAbsolutePath());
						ffmpeg_parsing = true;
					}
				}

				if (configuration.isThumbnailGenerationEnabled() && configuration.getImageThumbnailsEnabled() && thumbOnly) {
					LOGGER.trace("Creating (temporary) thumbnail: {}", file.getName());

					// Create the thumbnail image
					try {
						// This will fail for any image formats not supported by ImageIO
						thumb = DLNAThumbnail.toThumbnail(Files.newInputStream(file.toPath()), 320, 320, ScaleType.MAX, ImageFormat.SOURCE);
						thumbready = true;
					} catch (IOException e) {
						LOGGER.debug("Error generating thumbnail for \"{}\": {}", file.getName(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}

			if (ffmpeg_parsing) {
				if (!thumbOnly || (type == Format.VIDEO && !configuration.isUseMplayerForVideoThumbs())) {
					pw = getFFmpegThumbnail(inputFile, resume, renderer);
				}

				boolean dvrms = false;
				String input = "-";

				if (file != null) {
					input = ProcessUtil.getShortFileNameIfWideChars(file.getAbsolutePath());
					dvrms = file.getAbsolutePath().toLowerCase().endsWith("dvr-ms");
				}

				synchronized (ffmpeg_failureLock) {
					if (pw != null && !ffmpeg_failure && !thumbOnly) {
						parseFFmpegInfo(pw.getResults(), input);
					}
				}

				if (
					!thumbOnly &&
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

				if (configuration.isUseMplayerForVideoThumbs() && type == Format.VIDEO && !dvrms) {
					try {
						getMplayerThumbnail(inputFile, resume, renderer);
						String frameName = "" + inputFile.hashCode();
						frameName = configuration.getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
						frameName = frameName.replace(',', '_');
						File jpg = new File(frameName);

						if (jpg.exists()) {
							try (InputStream is = new FileInputStream(jpg)) {
								int sz = is.available();

								if (sz > 0) {
									byte[] bytes = new byte[sz];
									is.read(bytes);
									thumb = DLNAThumbnail.toThumbnail(bytes);
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

				if (type == Format.VIDEO && pw != null && thumb == null) {
					InputStream is;
					int sz = 0;
					try {
						is = pw.getInputStream(0);
						try {
							if (is != null) {
								sz = is.available();
								if (sz > 0) {
									byte[] bytes = new byte[sz];
									is.read(bytes);
									thumb = DLNAThumbnail.toThumbnail(bytes);
									thumbready = true;
								}
							}
						} finally {
							if (is != null) {
								is.close();
							}
						}
					} catch (IOException e) {
						LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
						LOGGER.trace("", e);
					}
				}
			}

			finalize(type, inputFile);
			mediaparsed = true;
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
			ListIterator<String> FFmpegMetaData = lines.listIterator();

			for (String line : lines) {
				FFmpegMetaData.next();
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
						DLNAMediaAudio audio = new DLNAMediaAudio();
						audio.setId(langId++);
						if (a > -1 && b > a) {
							audio.setLang(line.substring(a + 1, b));
						} else {
							audio.setLang(DLNAMediaLang.UND);
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
								audio.setCodecA(token.substring(token.indexOf("Audio: ") + 7));
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
						int FFmpegMetaDataNr = FFmpegMetaData.nextIndex();

						if (FFmpegMetaDataNr > -1) {
							line = lines.get(FFmpegMetaDataNr);
						}

						if (line.contains("Metadata:")) {
							FFmpegMetaDataNr += 1;
							line = lines.get(FFmpegMetaDataNr);
							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										audio.setAudioTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									FFmpegMetaDataNr += 1;
									line = lines.get(FFmpegMetaDataNr);
								}
							}
						}

						audioTracks.add(audio);
					} else if (line.contains("Video:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								codecV = token.substring(token.indexOf("Video: ") + 7);
								videoTrackCount++;
							} else if ((token.contains("tbc") || token.contains("tb(c)"))) {
								// A/V sync issues with newest FFmpeg, due to the new tbr/tbn/tbc outputs
								// Priority to tb(c)
								String frameRateDoubleString = token.substring(0, token.indexOf("tb")).trim();
								try {
									if (!frameRateDoubleString.equals(frameRate)) {// tbc taken into account only if different than tbr
										Double frameRateDouble = Double.parseDouble(frameRateDoubleString);
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
						DLNAMediaSubtitle lang = new DLNAMediaSubtitle();

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
							lang.setType(SubtitleType.SUBRIP);
						} else if (line.contains(" text")) {
							// excludes dvb_teletext, mov_text, realtext
							lang.setType(SubtitleType.TEXT);
						} else if (line.contains("microdvd")) {
							lang.setType(SubtitleType.MICRODVD);
						} else if (line.contains("sami")) {
							lang.setType(SubtitleType.SAMI);
						} else if (line.contains("ass") || line.contains("ssa")) {
							lang.setType(SubtitleType.ASS);
						} else if (line.contains("dvd_subtitle")) {
							lang.setType(SubtitleType.VOBSUB);
						} else if (line.contains("xsub")) {
							lang.setType(SubtitleType.DIVX);
						} else if (line.contains("mov_text")) {
							lang.setType(SubtitleType.TX3G);
						} else if (line.contains("webvtt")) {
							lang.setType(SubtitleType.WEBVTT);
						} else {
							lang.setType(SubtitleType.UNKNOWN);
						}

						int a = line.indexOf('(');
						int b = line.indexOf("):", a);
						if (a > -1 && b > a) {
							lang.setLang(line.substring(a + 1, b));
						} else {
							lang.setLang(DLNAMediaLang.UND);
						}

						lang.setId(subId++);
						int FFmpegMetaDataNr = FFmpegMetaData.nextIndex();

						if (FFmpegMetaDataNr > -1) {
							line = lines.get(FFmpegMetaDataNr);
						}

						if (line.contains("Metadata:")) {
							FFmpegMetaDataNr += 1;
							line = lines.get(FFmpegMetaDataNr);

							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										lang.setSubtitlesTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									FFmpegMetaDataNr += 1;
									line = lines.get(FFmpegMetaDataNr);
								}
							}
						}
						subtitleTracks.add(lang);
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
		return codecV != null && codecV.startsWith("h264");
	}

	/**
	 * Disable LPCM transcoding for MP4 container with non-H264 video as workaround for MEncoder's A/V sync bug
	 */
	public boolean isValidForLPCMTranscoding() {
		if (container != null) {
			if (container.equals("mp4")) {
				return isH264();
			} else {
				return true;
			}
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
	 * To get <code>0</code> instead of <code>null</code>, use
	 * {@link #getDurationInSeconds()}
	 */
	public Double getDuration() {
		return durationSec;
	}

	/**
	 * @return 0 if nothing is specified, otherwise the duration
	 */
	public double getDurationInSeconds() {
		return durationSec != null ? durationSec : 0;
	}

	public String getDurationString() {
		return durationSec != null ? convertTimeToString(durationSec, DURATION_TIME_FORMAT) : null;
	}

	/**
	 * @deprecated Use {@link #StringUtil.convertTimeToString(durationSec, StringUtil.DURATION_TIME_FORMAT)} instead.
	 */
	public static String getDurationString(double d) {
		return convertTimeToString(d, DURATION_TIME_FORMAT);
	}

	public static Double parseDurationString(String duration) {
		return duration != null ? convertStringToTime(duration) : null;
	}

	public void finalize(int type, InputFile f) {
		String codecA = null;
		if (getFirstAudioTrack() != null) {
			codecA = getFirstAudioTrack().getCodecA();
		}

		if (container != null) {
			switch (container) {
				case "avi":
					mimeType = HTTPResource.AVI_TYPEMIME;
					break;
				case "asf":
				case "wmv":
					mimeType = HTTPResource.WMV_TYPEMIME;
					break;
				case "mov":
					mimeType = HTTPResource.MOV_TYPEMIME;
					break;
			}
		}

		if (mimeType == null) {
			if (codecV != null) {
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
			} else if (codecV == null && codecA != null) {
				if ("ogg".equals(container)) {
					mimeType = HTTPResource.AUDIO_OGG_TYPEMIME;
				} else if ("3gp".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPPA_TYPEMIME;
				} else if ("3g2".equals(container)) {
					mimeType = HTTPResource.AUDIO_THREEGPP2A_TYPEMIME;
				} else if ("adts".equals(container)) {
					mimeType = HTTPResource.AUDIO_ADTS_TYPEMIME;
				} else if ("matroska".equals(container) || "mkv".equals(container)) {
					mimeType = HTTPResource.AUDIO_MATROSKA_TYPEMIME;
				} else if ("webm".equals(container)) {
					mimeType = HTTPResource.AUDIO_WEBM_TYPEMIME;
				} else if (codecA.contains("mp3")) {
					mimeType = HTTPResource.AUDIO_MP3_TYPEMIME;
				} else if (codecA.contains("aac")) {
					mimeType = HTTPResource.AUDIO_MP4_TYPEMIME;
				} else if (codecA.contains("flac")) {
					mimeType = HTTPResource.AUDIO_FLAC_TYPEMIME;
				} else if (codecA.contains("asf") || codecA.startsWith("wm")) {
					mimeType = HTTPResource.AUDIO_WMA_TYPEMIME;
				} else if (codecA.contains("pcm") || codecA.contains("wav") || codecA.contains("dts")) {
					mimeType = HTTPResource.AUDIO_WAV_TYPEMIME;
				}
			}

			if (mimeType == null) {
				mimeType = HTTPResource.getDefaultMimeType(type);
			}
		}

		if (getFirstAudioTrack() == null || !(type == Format.AUDIO && getFirstAudioTrack().getBitsperSample() == 24 && getFirstAudioTrack().getSampleRate() > 48000)) {
			secondaryFormatValid = false;
		}

		// Check for external subs here
		if (f.getFile() != null && type == Format.VIDEO && configuration.isAutoloadExternalSubtitles()) {
			FileUtil.isSubtitlesExists(f.getFile(), this);
		}
	}

	/**
	 * Checks whether the video has too many reference frames per pixels for the renderer
	 *
	 * TODO move to PlayerUtil
	 */
	public boolean isVideoWithinH264LevelLimits(InputFile f, RendererConfiguration mediaRenderer) {
		synchronized (videoWithinH264LevelLimitsLock) {
			if (videoWithinH264LevelLimits == null) {
				if (isH264()) {
					videoWithinH264LevelLimits = true;
					if (
						container != null &&
						(
							container.equals("matroska") ||
							container.equals("mkv") ||
							container.equals("mov") ||
							container.equals("mp4")
						)
					) { // Containers without h264_annexB
						byte headers[][] = getAnnexBFrameHeader(f);
						synchronized (ffmpeg_annexb_failureLock) {
							if (ffmpeg_annexb_failure) {
								LOGGER.info("Error parsing information from the file: " + f.getFilename());
							}
						}

						if (headers != null) {
							synchronized (h264_annexBLock) {
								h264_annexB = headers[1];
								if (h264_annexB != null) {
									int skip = 5;
									if (h264_annexB[2] == 1) {
										skip = 4;
									}
									byte header[] = new byte[h264_annexB.length - skip];
									System.arraycopy(h264_annexB, skip, header, 0, header.length);

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
											if (mediaRenderer == null || mediaRenderer.isPS3()) {
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
										}
									} finally {
										referenceFrameCountLock.readLock().unlock();
										avcLevelLock.readLock().unlock();
									}
								} else {
									LOGGER.debug(
										"The H.264 stream inside the file \"{}\" is not compatible with this renderer",
										f.getFilename()
									);
									videoWithinH264LevelLimits = false;
								}
							}
						} else {
							videoWithinH264LevelLimits = false;
						}
					}
				} else {
					videoWithinH264LevelLimits = false;
				}
			}
			return videoWithinH264LevelLimits;
		}
	}

	public boolean isMuxable(String filename, String codecA) {
		return codecA != null && (codecA.startsWith("dts") || codecA.equals("dca"));
	}

	public boolean isLossless(String codecA) {
		return codecA != null && (codecA.contains("pcm") || codecA.startsWith("dts") || codecA.equals("dca") || codecA.contains("flac")) && !codecA.contains("pcm_u8") && !codecA.contains("pcm_s8");
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("container: ");
		result.append(container);
		result.append(", bitrate: ");
		result.append(bitrate);
		result.append(", size: ");
		result.append(size);
		if (videoTrackCount > 0) {
			result.append(", video tracks: ");
			result.append(videoTrackCount);
		}
		if (getAudioTrackCount() > 0) {
			result.append(", audio tracks: ");
			result.append(getAudioTrackCount());
		}
		if (imageCount > 0) {
			result.append(", images: ");
			result.append(imageCount);
		}
		if (getSubTrackCount() > 0) {
			result.append(", subtitle tracks: ");
			result.append(getSubTrackCount());
		}
		result.append(", video codec: ");
		result.append(codecV);
		result.append(", duration: ");
		result.append(getDurationString());
		result.append(", width: ");
		result.append(width);
		result.append(", height: ");
		result.append(height);
		result.append(", frame rate: ");
		result.append(frameRate);

		if (thumb != null && thumb.getBytes(false) != null) {
			result.append(", thumb size: ");
			result.append(thumb.getBytes(false).length);
		}
		if (isNotBlank(muxingMode)) {
			result.append(", muxing mode: ");
			result.append(muxingMode);
		}

		result.append(", mime type: ");
		result.append(mimeType);

		if (isNotBlank(matrixCoefficients)) {
			result.append(", matrix coefficients: ");
			result.append(matrixCoefficients);
		}

		if (isNotBlank(fileTitleFromMetadata)) {
			result.append(", file title from metadata: ");
			result.append(fileTitleFromMetadata);
		}
		if (isNotBlank(videoTrackTitleFromMetadata)) {
			result.append(", video track title from metadata: ");
			result.append(videoTrackTitleFromMetadata);
		}

		for (DLNAMediaAudio audio : audioTracks) {
			result.append("\n\tAudio track ");
			result.append(audio.toString());
		}

		for (DLNAMediaSubtitle sub : subtitleTracks) {
			result.append("\n\tSubtitle track ");
			result.append(sub.toString());
		}

		return result.toString();
	}

	public DLNAThumbnailInputStream getThumbnailInputStream() {
		return thumb != null ? new DLNAThumbnailInputStream(thumb) : null;
	}

	public String getValidFps(boolean ratios) {
		String validFrameRate = null;

		if (frameRate != null && frameRate.length() > 0) {
			try {
				double fr = Double.parseDouble(frameRate.replace(',', '.'));

				if (fr >= 14.99 && fr < 15.1) {
					validFrameRate = "15";
				} else if (fr > 23.9 && fr < 23.99) {
					validFrameRate = ratios ? "24000/1001" : "23.976";
				} else if (fr > 23.99 && fr < 24.1) {
					validFrameRate = "24";
				} else if (fr >= 24.99 && fr < 25.1) {
					validFrameRate = "25";
				} else if (fr > 29.9 && fr < 29.99) {
					validFrameRate = ratios ? "30000/1001" : "29.97";
				} else if (fr >= 29.99 && fr < 30.1) {
					validFrameRate = "30";
				} else if (fr > 47.9 && fr < 47.99) {
					validFrameRate = ratios ? "48000/1001" : "47.952";
				} else if (fr > 49.9 && fr < 50.1) {
					validFrameRate = "50";
				} else if (fr > 59.8 && fr < 59.99) {
					validFrameRate = ratios ? "60000/1001" : "59.94";
				} else if (fr >= 59.99 && fr < 60.1) {
					validFrameRate = "60";
				}
			} catch (NumberFormatException nfe) {
				LOGGER.error(null, nfe);
			}
		}

		return validFrameRate;
	}

	public DLNAMediaAudio getFirstAudioTrack() {
		if (audioTracks.size() > 0) {
			return audioTracks.get(0);
		}
		return null;
	}

	/**
	 * @deprecated use getAspectRatioMencoderMpegopts() for the original
	 * functionality of this method, or use getAspectRatioContainer() for a
	 * better default method to get aspect ratios.
	 */
	@Deprecated
	public String getValidAspect(boolean ratios) {
		return getAspectRatioMencoderMpegopts(ratios);
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
		String a = null;

		if (aspectRatioDvdIso != null) {
			double ar = Double.parseDouble(aspectRatioDvdIso);

			if (ar > 1.7 && ar < 1.8) {
				a = ratios ? "16/9" : "1.777777777777777";
			}

			if (ar > 1.3 && ar < 1.4) {
				a = ratios ? "4/3" : "1.333333333333333";
			}
		}

		return a;
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

		if (getDurationInSeconds() != 0) {
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

	public byte[][] getAnnexBFrameHeader(InputFile f) {
		String[] cmdArray = new String[14];
		cmdArray[0] = configuration.getFfmpegPath();
		cmdArray[1] = "-i";

		if (f.getPush() == null && f.getFilename() != null) {
			cmdArray[2] = f.getFilename();
		} else {
			cmdArray[2] = "-";
		}

		cmdArray[3] = "-vframes";
		cmdArray[4] = "1";
		cmdArray[5] = "-c:v";
		cmdArray[6] = "copy";
		cmdArray[7] = "-f";
		cmdArray[8] = "h264";
		cmdArray[9] = "-bsf";
		cmdArray[10] = "h264_mp4toannexb";
		cmdArray[11] = "-an";
		cmdArray[12] = "-y";
		cmdArray[13] = "pipe:";

		byte[][] returnData = new byte[2][];
		OutputParams params = new OutputParams(configuration);
		params.maxBufferSize = 1;
		params.stdin = f.getPush();

		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					synchronized (ffmpeg_annexb_failureLock) {
						ffmpeg_annexb_failure = true;
					}
				} catch (InterruptedException e) { }
				pw.stopProcess();
			}
		};

		Thread failsafe = new Thread(r, "FFMpeg AnnexB Frame Header Failsafe");
		failsafe.start();
		pw.runInSameThread();

		synchronized (ffmpeg_annexb_failureLock) {
			if (ffmpeg_annexb_failure) {
				return null;
			}
		}

		InputStream is;
		ByteArrayOutputStream baot = new ByteArrayOutputStream();

		try {
			is = pw.getInputStream(0);
			byte b[] = new byte[4096];
			int n;

			while ((n = is.read(b)) > 0) {
				baot.write(b, 0, n);
			}

			byte data[] = baot.toByteArray();
			baot.close();
			returnData[0] = data;
			is.close();
			int kf = 0;

			for (int i = 3; i < data.length; i++) {
				if (data[i - 3] == 1 && (data[i - 2] & 37) == 37 && (data[i - 1] & -120) == -120) {
					kf = i - 2;
					break;
				}
			}

			int st = 0;
			boolean found = false;

			if (kf > 0) {
				for (int i = kf; i >= 5; i--) {
					if (data[i - 5] == 0 && data[i - 4] == 0 && data[i - 3] == 0 && (data[i - 2] & 1) == 1 && (data[i - 1] & 39) == 39) {
						st = i - 5;
						found = true;
						break;
					}
				}
			}

			if (found) {
				byte header[] = new byte[kf - st];
				System.arraycopy(data, st, header, 0, kf - st);
				returnData[1] = header;
			}
		} catch (IOException e) {
			LOGGER.debug("Caught exception", e);
		}

		return returnData;
	}

	@Override
	protected DLNAMediaInfo clone() throws CloneNotSupportedException {
		DLNAMediaInfo mediaCloned = (DLNAMediaInfo) super.clone();
		mediaCloned.setAudioTracksList(new ArrayList<DLNAMediaAudio>());
		for (DLNAMediaAudio audio : audioTracks) {
			mediaCloned.getAudioTracksList().add((DLNAMediaAudio) audio.clone());
		}

		mediaCloned.setSubtitleTracksList(new ArrayList<DLNAMediaSubtitle>());
		for (DLNAMediaSubtitle sub : subtitleTracks) {
			mediaCloned.getSubtitleTracksList().add((DLNAMediaSubtitle) sub.clone());
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
		this.codecV = codecV.toLowerCase();
	}

	/**
	 * @return the frameRate
	 * @since 1.50.0
	 */
	public String getFrameRate() {
		return frameRate;
	}

	/**
	 * @param frameRate the frameRate to set
	 * @since 1.50.0
	 */
	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
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

	/**
	 * @deprecated use getAspectRatioDvdIso() for the original
	 * functionality of this method, or use getAspectRatioContainer() for a
	 * better default method to get aspect ratios.
	 */
	@Deprecated
	public String getAspect() {
		return getAspectRatioDvdIso();
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
	 * @deprecated use setAspectRatioDvdIso() for the original
	 * functionality of this method, or use setAspectRatioContainer() for a
	 * better default method to set aspect ratios.
	 */
	@Deprecated
	public void setAspect(String aspect) {
		setAspectRatioDvdIso(aspect);
	}

	/**
	 * @param aspect the aspect to set
	 * @since 1.50.0
	 */
	public void setAspectRatioDvdIso(String aspect) {
		this.aspectRatioDvdIso = aspect;
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
	 * Set the aspect ratio reported by the file/container.
	 *
	 * @see #getAspectRatioContainer()
	 * @param aspect the aspect ratio to set
	 */
	public void setAspectRatioContainer(String aspect) {
		this.aspectRatioContainer = getFormattedAspectRatio(aspect);
	}

	/**
	 * Get the aspect ratio of the video track.
	 * This is the actual aspect ratio of the pixels, which is not
	 * always the aspect ratio that the renderer should display or that we
	 * should output; that is {@link #getAspectRatioContainer()}
	 *
	 * @return the aspect ratio of the video track
	 */
	public String getAspectRatioVideoTrack() {
		return aspectRatioVideoTrack;
	}

	/**
	 * @param aspect the aspect ratio to set
	 */
	public void setAspectRatioVideoTrack(String aspect) {
		this.aspectRatioVideoTrack = getFormattedAspectRatio(aspect);
	}

	/**
	 * Make sure the aspect ratio is formatted, e.g. 16:9 not 1.78
	 *
	 * @param aspect the possibly-unformatted aspect ratio
	 *
	 * @return the formatted aspect ratio or null
	 */
	public String getFormattedAspectRatio(String aspect) {
		if (isBlank(aspect)) {
			return null;
		} else {
			if (aspect.contains(":")) {
				return aspect;
			} else {
				double exactAspectRatio = Double.parseDouble(aspect);
				if (exactAspectRatio > 1.7 && exactAspectRatio <= 1.8) {
					return "16:9";
				} else if (exactAspectRatio > 1.3 && exactAspectRatio < 1.4) {
					return "4:3";
				} else if (exactAspectRatio > 1.2 && exactAspectRatio < 1.3) {
					return "5:4";
				} else {
					return null;
				}
			}
		}
	}

	/**
	 * @return the thumb
	 * @since 1.50.0
	 */
	public DLNAThumbnail getThumb() {
		return thumb;
	}

	/**
	 * @param thumb the thumb to set
	 * @since 1.50.0
	 */
	public void setThumb(DLNAThumbnail thumb) {
		this.thumb = thumb;
		if (thumb != null) {
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
	 * @return the bitsPerPixel
	 * @since 1.50.0
	 */
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}

	/**
	 * @param bitsPerPixel the bitsPerPixel to set
	 * @since 1.50.0
	 */
	public void setBitsPerPixel(int bitsPerPixel) {
		this.bitsPerPixel = bitsPerPixel;
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
		} catch (Exception e) {
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
	public List<DLNAMediaAudio> getAudioTracksList() {
		return audioTracks;
	}

	/**
	 * @return the audioTracks
	 * @deprecated use getAudioTracksList() instead
	 */
	@Deprecated
	public ArrayList<DLNAMediaAudio> getAudioCodes() {
		if (audioTracks instanceof ArrayList) {
			return (ArrayList<DLNAMediaAudio>) audioTracks;
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * @param audioTracks the audioTracks to set
	 * @since 1.60.0
	 */
	// TODO (breaking change): rename to setAudioTracks
	public void setAudioTracksList(List<DLNAMediaAudio> audioTracks) {
		this.audioTracks = audioTracks;
	}

	/**
	 * @param audioTracks the audioTracks to set
	 * @deprecated use setAudioTracksList(ArrayList<DLNAMediaAudio> audioTracks) instead
	 */
	@Deprecated
	public void setAudioCodes(List<DLNAMediaAudio> audioTracks) {
		setAudioTracksList(audioTracks);
	}

	/**
	 * @return the subtitleTracks
	 * @since 1.60.0
	 */
	// TODO (breaking change): rename to getSubtitleTracks
	public List<DLNAMediaSubtitle> getSubtitleTracksList() {
		return subtitleTracks;
	}

	/**
	 * @return the subtitleTracks
	 * @deprecated use getSubtitleTracksList() instead
	 */
	@Deprecated
	public ArrayList<DLNAMediaSubtitle> getSubtitlesCodes() {
		if (subtitleTracks instanceof ArrayList) {
			return (ArrayList<DLNAMediaSubtitle>) subtitleTracks;
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * @param subtitleTracks the subtitleTracks to set
	 * @since 1.60.0
	 */
	// TODO (breaking change): rename to setSubtitleTracks
	public void setSubtitleTracksList(List<DLNAMediaSubtitle> subtitleTracks) {
		this.subtitleTracks = subtitleTracks;
	}

	/**
	 * @param subtitleTracks the subtitleTracks to set
	 * @deprecated use setSubtitleTracksList(ArrayList<DLNAMediaSubtitle> subtitleTracks) instead
	 */
	@Deprecated
	public void setSubtitlesCodes(List<DLNAMediaSubtitle> subtitleTracks) {
		setSubtitleTracksList(subtitleTracks);
	}

	/**
	 * @return the model
	 * @since 1.50.0
	 */
	public String getModel() {
		return model;
	}

	/**
	 * @param model the model to set
	 * @since 1.50.0
	 */
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * @return the exposure
	 * @since 1.50.0
	 */
	public int getExposure() {
		return exposure;
	}

	/**
	 * @param exposure the exposure to set
	 * @since 1.50.0
	 */
	public void setExposure(int exposure) {
		this.exposure = exposure;
	}

	/**
	 * @return the orientation
	 * @since 1.50.0
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * @param orientation the orientation to set
	 * @since 1.50.0
	 */
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	/**
	 * @return the iso
	 * @since 1.50.0
	 */
	public int getIso() {
		return iso;
	}

	/**
	 * @param iso the iso to set
	 * @since 1.50.0
	 */
	public void setIso(int iso) {
		this.iso = iso;
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
	public byte[] getH264AnnexB() {
		synchronized (h264_annexBLock) {
			if (h264_annexB == null) {
				return null;
			}
			byte[] result = new byte[h264_annexB.length];
			System.arraycopy(h264_annexB, 0, result, 0, h264_annexB.length);
			return result;
		}
	}

	/**
	 * @param h264AnnexB the h264_annexB to set
	 * @since 1.50.0
	 */
	public void setH264AnnexB(byte[] h264AnnexB) {
		synchronized (h264_annexBLock) {
			if (h264AnnexB == null) {
				this.h264_annexB = null;
			} else {
				this.h264_annexB = new byte[h264AnnexB.length];
				System.arraycopy(h264AnnexB, 0, this.h264_annexB, 0, h264AnnexB.length);
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
		if (
			height % 4 != 0 ||
			width % 4 != 0
		) {
			return false;
		}

		return true;
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

		switch (stereoscopy.toLowerCase()) {
			case "overunderrt":
			case "oulf":
			case "ourf":
			case "sbslf":
			case "sbsrf":
			case "top-bottom (left eye first)":
			case "top-bottom (right eye first)":
			case "side by side (left eye first)":
			case "side by side (right eye first)":
				return true;
		}

		return false;
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

	/**
	 * Used by FFmpeg for 3D video format naming
	 */
	public enum Mode3D {
		SBSL,
		SBSR,
		HSBSL,
		OUL,
		OUR,
		HOUL,
		ARCG,
		ARCH,
		ARCC,
		ARCD,
		AGMG,
		AGMH,
		AGMC,
		AGMD,
		AYBG,
		AYBH,
		AYBC,
		AYBD
	};

	public Mode3D get3DLayout() {
		if (!is3d()) {
			return null;
		}

		isAnaglyph = true;
		switch (stereoscopy.toLowerCase()) {
			case "overunderrt":
			case "oulf":
			case "top-bottom (left eye first)":
				isAnaglyph = false;
				return Mode3D.OUL;
			case "ourf":
			case "top-bottom (right eye first)":
				isAnaglyph = false;
				return Mode3D.OUR;
			case "sbslf":
			case "side by side (left eye first)":
				isAnaglyph = false;
				return Mode3D.SBSL;
			case "sbsrf":
			case "side by side (right eye first)":
				isAnaglyph = false;
				return Mode3D.SBSR;
			case "half top-bottom (left eye first)":
				isAnaglyph = false;
				return Mode3D.HOUL;
			case "half side by side (left eye first)":
				isAnaglyph = false;
				return Mode3D.HSBSL;
			case "arcg":
				return Mode3D.ARCG;
			case "arch":
				return Mode3D.ARCH;
			case "arcc":
				return Mode3D.ARCC;
			case "arcd":
				return Mode3D.ARCD;
			case "agmg":
				return Mode3D.AGMG;
			case "agmh":
				return Mode3D.AGMH;
			case "agmc":
				return Mode3D.AGMC;
			case "agmd":
				return Mode3D.AGMD;
			case "aybg":
				return Mode3D.AYBG;
			case "aybh":
				return Mode3D.AYBH;
			case "aybc":
				return Mode3D.AYBC;
			case "aybd":
				return Mode3D.AYBD;
		}

		return null;
	}

	private boolean isAnaglyph;

	public boolean stereoscopyIsAnaglyph() {
		get3DLayout();
		return isAnaglyph;
	}

	public boolean isDVDResolution() {
		return (width == 720 && height == 576) || (width == 720 && height == 480);
	}

	private ColorType colorType;

	public void setColorType(ColorType colorType) {
		this.colorType = colorType;
	}

	public ColorType getColorType() {
		return colorType;
	}
}
