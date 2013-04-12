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

import com.sun.jna.Platform;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import net.pms.util.AVCHeader;
import net.pms.util.CoverUtil;
import net.pms.util.FileUtil;
import net.pms.util.MpegUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
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
	private static final String THUMBNAIL_DIRECTORY_NAME = "thumbs";
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public static final long ENDFILE_POS = 99999475712L;
	public static final long TRANS_SIZE = 100000000000L;

	private boolean h264_parsed;

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

	public String aspectRatioContainer;
	public String aspectRatioVideoTrack;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public byte thumb[];

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

	private List<DLNAMediaAudio> audioTracks = new ArrayList<>();
	private List<DLNAMediaSubtitle> subtitleTracks = new ArrayList<>();

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

	/**
	 * @deprecated Use {@link #getH264AnnexB()} and {@link #setH264AnnexB(byte[])} to access this variable.
	 */
	@Deprecated
	public byte[] h264_annexB;

	/**
	 * Not stored in database.
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean mediaparsed;

	/**
	 * isMediaParserV2 related, used to manage thumbnail management separated
	 * from the main parsing process.
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean thumbready;

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

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean parsing = false;

	private boolean ffmpeg_failure;
	private boolean ffmpeg_annexb_failure;
	private boolean muxable;
	private Map<String, String> extras;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public boolean encrypted;

	/**
	 * Used to determine whether tsMuxeR can mux the file instead of transcoding.
	 * Also used by DLNAResource to help determine the DLNA.ORG_PN (file type)
	 * value to send to the renderer, which is confusing.
	 *
	 * Some of this code is repeated in isVideoWithinH264LevelLimits(), and since
	 * both functions are sometimes (but not always) used together, this is
	 * not an efficient use of code.
	 * TODO: Fix the above situation.
	 * TODO: Now that FFmpeg is muxing without tsMuxeR, we should make a separate
	 *       function for that, or even better, re-think this whole approach.
	 */
	public boolean isMuxable(RendererConfiguration mediaRenderer) {
		// Allow the formats in the following list
		if (
			getContainer() != null &&
			(
				getContainer().equals("mkv") ||
				getContainer().equals("mp4") ||
				getContainer().equals("mov") ||
				getContainer().equals("ts") ||
				getContainer().equals("m2ts")
			)
		) {
			if (
				getCodecV() != null &&
				(
					getCodecV().equals("h264") ||
					getCodecV().equals("vc1") ||
					getCodecV().equals("mpeg2")
				)
			) {
				if (getFirstAudioTrack() != null) {
					String codecA;
					codecA = getFirstAudioTrack().getCodecA();
					if (
						codecA != null &&
						(
							codecA.equals("aac") ||
							codecA.equals("ac3") ||
							codecA.equals("dca") ||
							codecA.equals("dts") ||
							codecA.equals("eac3")
						)
					) {
						muxable = true;
					}
				}
			}
		}

		// Temporary fix: MediaInfo support will take care of this in the future
		// For now, http://ps3mediaserver.org/forum/viewtopic.php?f=11&t=6361&start=0
		// Bravia does not support AVC video at less than 288px high
		if (mediaRenderer.isBRAVIA() && getHeight() < 288) {
			muxable = false;
		}

		return muxable;
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
			sb.append("|");
			sb.append(entry.getValue());
			sb.append("|");
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

	public DLNAMediaInfo() {
		setThumbready(true); // this class manages thumbnails by default with the parser_v1 method
	}

	public void generateThumbnail(InputFile input, Format ext, int type) {
		DLNAMediaInfo forThumbnail = new DLNAMediaInfo();
		forThumbnail.durationSec = durationSec;
		forThumbnail.parse(input, ext, type, true);
		setThumb(forThumbnail.getThumb());
	}

	private ProcessWrapperImpl getFFMpegThumbnail(InputFile media) {
		String args[] = new String[14];
		args[0] = getFfmpegPath();
		boolean dvrms = media.getFile() != null && media.getFile().getAbsolutePath().toLowerCase().endsWith("dvr-ms");

		if (dvrms && StringUtils.isNotBlank(configuration.getFfmpegAlternativePath())) {
			args[0] = configuration.getFfmpegAlternativePath();
		}

		args[1] = "-ss";
		args[2] = "" + configuration.getThumbnailSeekPos();
		args[3] = "-i";

		if (media.getFile() != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(media.getFile().getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-an";
		args[6] = "-an";
		args[7] = "-s";
		args[8] = "320x180";
		args[9] = "-vframes";
		args[10] = "1";
		args[11] = "-f";
		args[12] = "image2";
		args[13] = "pipe:";

		// FIXME MPlayer should not be used if thumbnail generation is disabled (and it should be disabled in the GUI)
		if (!configuration.isThumbnailGenerationEnabled() || (configuration.isUseMplayerForVideoThumbs() && !dvrms)) {
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
		setParsing(true);
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
					ffmpeg_failure = true;
				} catch (InterruptedException e) { }

				pw.stopProcess();
				setParsing(false);
			}
		};

		Thread failsafe = new Thread(r, "FFmpeg Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		setParsing(false);
		return pw;
	}

	private ProcessWrapperImpl getMplayerThumbnail(InputFile media) throws IOException {
		String args[] = new String[14];
		args[0] = configuration.getMplayerPath();
		args[1] = "-ss";
		boolean toolong = getDurationInSeconds() < configuration.getThumbnailSeekPos();
		args[2] = "" + (toolong ? (getDurationInSeconds() / 2) : configuration.getThumbnailSeekPos());
		args[3] = "-quiet";

		if (media.getFile() != null) {
			args[4] = ProcessUtil.getShortFileNameIfWideChars(media.getFile().getAbsolutePath());
		} else {
			args[4] = "-";
		}

		args[5] = "-msglevel";
		args[6] = "all=4";
		args[7] = "-vf";
		args[8] = "scale=320:-2,expand=:180";
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
		setParsing(true);
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) { }

				pw.stopProcess();
				setParsing(false);
			}
		};

		Thread failsafe = new Thread(r, "MPlayer Thumbnail Failsafe");
		failsafe.start();
		pw.runInSameThread();
		setParsing(false);
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

	public void parse(InputFile inputFile, Format ext, int type, boolean thumbOnly) {
		int i = 0;

		while (isParsing()) {
			if (i == 5) {
				setMediaparsed(true);
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }

			i++;
		}

		if (isMediaparsed()) {
			return;
		}

		if (inputFile != null) {
			if (inputFile.getFile() != null) {
				setSize(inputFile.getFile().length());
			} else {
				setSize(inputFile.getSize());
			}

			ProcessWrapperImpl pw = null;
			boolean ffmpeg_parsing = true;

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				ffmpeg_parsing = false;
				DLNAMediaAudio audio = new DLNAMediaAudio();

				if (inputFile.getFile() != null) {
					try {
						AudioFile af = AudioFileIO.read(inputFile.getFile());
						AudioHeader ah = af.getAudioHeader();

						if (ah != null && !thumbOnly) {
							int length = ah.getTrackLength();
							int rate = ah.getSampleRateAsNumber();

							if (ah.getEncodingType().toLowerCase().contains("flac 24")) {
								audio.setBitsperSample(24);
							}

							audio.setSampleFrequency("" + rate);
							setDuration((double) length);
							setBitrate((int) ah.getBitRateAsNumber());
							audio.getAudioProperties().setNumberOfChannels(2);

							if (ah.getChannels() != null && ah.getChannels().toLowerCase().contains("mono")) {
								audio.getAudioProperties().setNumberOfChannels(1);
							} else if (ah.getChannels() != null && ah.getChannels().toLowerCase().contains("stereo")) {
								audio.getAudioProperties().setNumberOfChannels(2);
							} else if (ah.getChannels() != null) {
								audio.getAudioProperties().setNumberOfChannels(Integer.parseInt(ah.getChannels()));
							}

							audio.setCodecA(ah.getEncodingType().toLowerCase());

							if (audio.getCodecA().contains("(windows media")) {
								audio.setCodecA(audio.getCodecA().substring(0, audio.getCodecA().indexOf("(windows media")).trim());
							}
						}

						Tag t = af.getTag();

						if (t != null) {
							if (t.getArtworkList().size() > 0) {
								setThumb(t.getArtworkList().get(0).getBinaryData());
							} else {
								if (configuration.getAudioThumbnailMethod() > 0) {
									setThumb(
										CoverUtil.get().getThumbnailFromArtistAlbum(
											configuration.getAudioThumbnailMethod() == 1 ?
												CoverUtil.AUDIO_AMAZON :
												CoverUtil.AUDIO_DISCOGS,
											audio.getArtist(), audio.getAlbum()
										)
									);
								}
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
					} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | NumberFormatException | KeyNotFoundException e) {
						LOGGER.debug("Error parsing audio file: {} - {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");
						ffmpeg_parsing = false;
					}

					if (audio.getSongname() == null || audio.getSongname().length() == 0) {
						audio.setSongname(inputFile.getFile().getName());
					}

					if (!ffmpeg_parsing) {
						getAudioTracksList().add(audio);
					}
				}
			}

			if (type == Format.IMAGE && inputFile.getFile() != null) {
				try {
					ffmpeg_parsing = false;
					ImageInfo info = Sanselan.getImageInfo(inputFile.getFile());
					setWidth(info.getWidth());
					setHeight(info.getHeight());
					setBitsPerPixel(info.getBitsPerPixel());
					String formatName = info.getFormatName();

					if (formatName.startsWith("JPEG")) {
						setCodecV("jpg");
						IImageMetadata meta = Sanselan.getMetadata(inputFile.getFile());

						if (meta != null && meta instanceof JpegImageMetadata) {
							JpegImageMetadata jpegmeta = (JpegImageMetadata) meta;
							TiffField tf = jpegmeta.findEXIFValue(TiffConstants.EXIF_TAG_MODEL);

							if (tf != null) {
								setModel(tf.getStringValue().trim());
							}

							tf = jpegmeta.findEXIFValue(TiffConstants.EXIF_TAG_EXPOSURE_TIME);
							if (tf != null) {
								setExposure((int) (1000 * tf.getDoubleValue()));
							}

							tf = jpegmeta.findEXIFValue(TiffConstants.EXIF_TAG_ORIENTATION);
							if (tf != null) {
								setOrientation(tf.getIntValue());
							}

							tf = jpegmeta.findEXIFValue(TiffConstants.EXIF_TAG_ISO);
							if (tf != null) {
								// Galaxy Nexus jpg pictures may contain multiple values, take the first
								int[] isoValues = tf.getIntArrayValue();
								setIso(isoValues[0]);
							}
						}
					} else if (formatName.startsWith("PNG")) {
						setCodecV("png");
					} else if (formatName.startsWith("GIF")) {
						setCodecV("gif");
					} else if (formatName.startsWith("TIF")) {
						setCodecV("tiff");
					}

					setContainer(getCodecV());
				} catch (ImageReadException | IOException e) {
					LOGGER.info("Error parsing image ({}) with Sanselan, switching to FFmpeg.", inputFile.getFile().getAbsolutePath());
				}
			}

			if (configuration.getImageThumbnailsEnabled() && type != Format.VIDEO) {
				try {
					File thumbDir = new File(configuration.getTempFolder(), THUMBNAIL_DIRECTORY_NAME);

					LOGGER.trace("Generating thumbnail for: {}", inputFile.getFile().getAbsolutePath());

					if (!thumbDir.exists() && !thumbDir.mkdirs()) {
						LOGGER.warn("Could not create thumbnail directory: {}", thumbDir.getAbsolutePath());
					} else {
						File thumbFile = new File(thumbDir, inputFile.getFile().getName() + ".jpg");
						String thumbFilename = thumbFile.getAbsolutePath();

						LOGGER.trace("Creating (temporary) thumbnail: {}", thumbFilename);

						// Create the thumbnail image using the Thumbnailator library
						final Builder<File> thumbnail = Thumbnails.of(inputFile.getFile());
						thumbnail.size(320, 180);
						thumbnail.outputFormat("jpg");
						thumbnail.outputQuality(1.0f);

						try {
							thumbnail.toFile(thumbFilename);
						} catch (IIOException e) {
							LOGGER.debug("Error generating thumbnail for: " + inputFile.getFile().getName());
							LOGGER.debug("The full error was: " + e);
						}

						File jpg = new File(thumbFilename);

						if (jpg.exists()) {
							try (InputStream is = new FileInputStream(jpg)) {
								int sz = is.available();

								if (sz > 0) {
									setThumb(new byte[sz]);
									is.read(getThumb());
								}
							}

							if (!jpg.delete()) {
								jpg.deleteOnExit();
							}
						}
					}
				} catch (UnsupportedFormatException ufe) {
					LOGGER.debug("Thumbnailator does not support the format of {}: {}", inputFile.getFile().getAbsolutePath(), ufe.getMessage());
				} catch (Exception e) {
					LOGGER.debug("Thumbnailator could not generate a thumbnail for: {}", inputFile.getFile().getAbsolutePath(), e);
				}
			}

			if (ffmpeg_parsing) {
				if (!thumbOnly || !configuration.isUseMplayerForVideoThumbs()) {
					pw = getFFMpegThumbnail(inputFile);
				}

				String input = "-";
				boolean dvrms = false;

				if (inputFile.getFile() != null) {
					input = ProcessUtil.getShortFileNameIfWideChars(inputFile.getFile().getAbsolutePath());
					dvrms = inputFile.getFile().getAbsolutePath().toLowerCase().endsWith("dvr-ms");
				}

				if (!ffmpeg_failure && !thumbOnly) {
					if (input.equals("-")) {
						input = "pipe:";
					}

					boolean matchs = false;
					ArrayList<String> lines = (ArrayList<String>) pw.getResults();
					int langId = 0;
					int subId = 0;
					ListIterator<String> FFmpegMetaData = lines.listIterator();

					for (String line : lines) {
						FFmpegMetaData.next();
						line = line.trim();
						if (line.startsWith("Output")) {
							matchs = false;
						} else if (line.startsWith("Input")) {
							if (line.indexOf(input) > -1) {
								matchs = true;
								setContainer(line.substring(10, line.indexOf(",", 11)).trim());
							} else {
								matchs = false;
							}
						} else if (matchs) {
							if (line.indexOf("Duration") > -1) {
								StringTokenizer st = new StringTokenizer(line, ",");
								while (st.hasMoreTokens()) {
									String token = st.nextToken().trim();
									if (token.startsWith("Duration: ")) {
										String durationStr = token.substring(10);
										int l = durationStr.substring(durationStr.indexOf(".") + 1).length();
										if (l < 4) {
											durationStr = durationStr + "00".substring(0, 3 - l);
										}
										if (durationStr.indexOf("N/A") > -1) {
											setDuration(null);
										} else {
											setDuration(StringUtil.convertStringToTime(durationStr));
										}
									} else if (token.startsWith("bitrate: ")) {
										String bitr = token.substring(9);
										int spacepos = bitr.indexOf(" ");
										if (spacepos > -1) {
											String value = bitr.substring(0, spacepos);
											String unit = bitr.substring(spacepos + 1);
											setBitrate(Integer.parseInt(value));
											if (unit.equals("kb/s")) {
												setBitrate(1024 * getBitrate());
											}
											if (unit.equals("mb/s")) {
												setBitrate(1048576 * getBitrate());
											}
										}
									}
								}
							} else if (line.indexOf("Audio:") > -1) {
								StringTokenizer st = new StringTokenizer(line, ",");
								int a = line.indexOf("(");
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
								b = line.indexOf("]", a);
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

								if (line.indexOf("Metadata:") > -1) {
									FFmpegMetaDataNr = FFmpegMetaDataNr + 1;
									line = lines.get(FFmpegMetaDataNr);
									while (line.indexOf("      ") == 0) {
										if (line.toLowerCase().indexOf("title           :") > -1) {
											int aa = line.indexOf(": ");
											int bb = line.length();
											if (aa > -1 && bb > aa) {
												audio.setFlavor(line.substring(aa+2, bb));
												break;
											}
										} else {
											FFmpegMetaDataNr = FFmpegMetaDataNr + 1;
											line = lines.get(FFmpegMetaDataNr);
										}
									}
								}

								getAudioTracksList().add(audio);
							} else if (line.indexOf("Video:") > -1) {
								StringTokenizer st = new StringTokenizer(line, ",");
								while (st.hasMoreTokens()) {
									String token = st.nextToken().trim();
									if (token.startsWith("Stream")) {
										setCodecV(token.substring(token.indexOf("Video: ") + 7));
									} else if ((token.indexOf("tbc") > -1 || token.indexOf("tb(c)") > -1)) {
										// A/V sync issues with newest FFmpeg, due to the new tbr/tbn/tbc outputs
										// Priority to tb(c)
										String frameRateDoubleString = token.substring(0, token.indexOf("tb")).trim();
										try {
											if (!frameRateDoubleString.equals(getFrameRate())) {// tbc taken into account only if different than tbr
												Double frameRateDouble = Double.parseDouble(frameRateDoubleString);
												setFrameRate(String.format(Locale.ENGLISH, "%.2f", frameRateDouble / 2));
											}
										} catch (NumberFormatException nfe) {
											// Could happen if tbc is "1k" or something like that, no big deal
											LOGGER.debug("Could not parse frame rate \"" + frameRateDoubleString + "\"");
										}

									} else if ((token.indexOf("tbr") > -1 || token.indexOf("tb(r)") > -1) && getFrameRate() == null) {
										setFrameRate(token.substring(0, token.indexOf("tb")).trim());
									} else if ((token.indexOf("fps") > -1 || token.indexOf("fps(r)") > -1) && getFrameRate() == null) { // dvr-ms ?
										setFrameRate(token.substring(0, token.indexOf("fps")).trim());
									} else if (token.indexOf("x") > -1) {
										String resolution = token.trim();
										if (resolution.indexOf(" [") > -1) {
											resolution = resolution.substring(0, resolution.indexOf(" ["));
										}
										try {
											setWidth(Integer.parseInt(resolution.substring(0, resolution.indexOf("x"))));
										} catch (NumberFormatException nfe) {
											LOGGER.debug("Could not parse width from \"" + resolution.substring(0, resolution.indexOf("x")) + "\"");
										}
										try {
											setHeight(Integer.parseInt(resolution.substring(resolution.indexOf("x") + 1)));
										} catch (NumberFormatException nfe) {
											LOGGER.debug("Could not parse height from \"" + resolution.substring(resolution.indexOf("x") + 1) + "\"");
										}
									}
								}
							} else if (line.indexOf("Subtitle:") > -1 && !line.contains("tx3g")) {
								DLNAMediaSubtitle lang = new DLNAMediaSubtitle();
								lang.setType((line.contains("dvdsub") && Platform.isWindows() ? SubtitleType.VOBSUB : SubtitleType.UNKNOWN));
								int a = line.indexOf("(");
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

								if (line.indexOf("Metadata:") > -1) {
									FFmpegMetaDataNr = FFmpegMetaDataNr + 1;
									line = lines.get(FFmpegMetaDataNr);

									while (line.indexOf("      ") == 0) {
										if (line.toLowerCase().indexOf("title           :") > -1) {
											int aa = line.indexOf(": ");
											int bb = line.length();
											if (aa > -1 && bb > aa) {
												lang.setFlavor(line.substring(aa+2, bb));
												break;
											}
										} else {
											FFmpegMetaDataNr = FFmpegMetaDataNr + 1;
											line = lines.get(FFmpegMetaDataNr);
										}
									}
								}
								getSubtitleTracksList().add(lang);
							}
						}
					}
				}

				if (
					!thumbOnly &&
					getContainer() != null &&
					inputFile.getFile() != null &&
					getContainer().equals("mpegts") &&
					isH264() &&
					getDurationInSeconds() == 0
				) {
					// Parse the duration
					try {
						int length = MpegUtil.getDurationFromMpeg(inputFile.getFile());
						if (length > 0) {
							setDuration((double) length);
						}
					} catch (IOException e) {
						LOGGER.trace("Error retrieving length: " + e.getMessage());
					}
				}

				if (configuration.isUseMplayerForVideoThumbs() && type == Format.VIDEO && !dvrms) {
					try {
						getMplayerThumbnail(inputFile);
						String frameName = "" + inputFile.hashCode();
						frameName = configuration.getTempFolder() + "/mplayer_thumbs/" + frameName + "00000001/00000001.jpg";
						frameName = frameName.replace(',', '_');
						File jpg = new File(frameName);

						if (jpg.exists()) {
							try (InputStream is = new FileInputStream(jpg)) {
								int sz = is.available();

								if (sz > 0) {
									setThumb(new byte[sz]);
									is.read(getThumb());
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

				if (type == Format.VIDEO && pw != null && getThumb() == null) {
					InputStream is;
					try {
						is = pw.getInputStream(0);
						int sz = is.available();
						if (sz > 0) {
							setThumb(new byte[sz]);
							is.read(getThumb());
						}
						is.close();

						if (sz > 0 && !net.pms.PMS.isHeadless()) {
							BufferedImage image = ImageIO.read(new ByteArrayInputStream(getThumb()));
							if (image != null) {
								Graphics g = image.getGraphics();
								g.setColor(Color.WHITE);
								g.setFont(new Font("Arial", Font.PLAIN, 14));
								int low = 0;
								if (getWidth() > 0) {
									if (getWidth() == 1920 || getWidth() == 1440) {
										g.drawString("1080p", 0, low += 18);
									} else if (getWidth() == 1280) {
										g.drawString("720p", 0, low += 18);
									}
								}
								ByteArrayOutputStream out = new ByteArrayOutputStream();
								ImageIO.write(image, "jpeg", out);
								setThumb(out.toByteArray());
							}
						}
					} catch (IOException e) {
						LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
					}
				}
			}

			finalize(type, inputFile);
			setMediaparsed(true);
		}
	}

	public boolean isH264() {
		return getCodecV() != null && getCodecV().contains("264");
	}

	public int getFrameNumbers() {
		double fr = Double.parseDouble(getFrameRate());
		return (int) (getDurationInSeconds() * fr);
	}

	public void setDuration(Double d) {
		this.durationSec = d;
	}

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
		return durationSec != null ? StringUtil.convertTimeToString(durationSec, true) : null;
	}

	public void finalize(int type, InputFile f) {
		String codecA = null;

		if (getFirstAudioTrack() != null) {
			codecA = getFirstAudioTrack().getCodecA();
		}

		if (getContainer() != null && getContainer().equals("avi")) {
			setMimeType(HTTPResource.AVI_TYPEMIME);
		} else if (getContainer() != null && (getContainer().equals("asf") || getContainer().equals("wmv"))) {
			setMimeType(HTTPResource.WMV_TYPEMIME);
		} else if (getContainer() != null && (getContainer().equals("matroska") || getContainer().equals("mkv"))) {
			setMimeType(HTTPResource.MATROSKA_TYPEMIME);
		} else if (getCodecV() != null && getCodecV().equals("mjpeg")) {
			setMimeType(HTTPResource.JPEG_TYPEMIME);
		} else if ("png".equals(getCodecV()) || "png".equals(getContainer())) {
			setMimeType(HTTPResource.PNG_TYPEMIME);
		} else if ("gif".equals(getCodecV()) || "gif".equals(getContainer())) {
			setMimeType(HTTPResource.GIF_TYPEMIME);
		} else if (getCodecV() != null && (getCodecV().equals("h264") || getCodecV().equals("h263") || getCodecV().toLowerCase().equals("mpeg4") || getCodecV().toLowerCase().equals("mp4"))) {
			setMimeType(HTTPResource.MP4_TYPEMIME);
		} else if (getCodecV() != null && (getCodecV().indexOf("mpeg") > -1 || getCodecV().indexOf("mpg") > -1)) {
			setMimeType(HTTPResource.MPEG_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && codecA.contains("mp3")) {
			setMimeType(HTTPResource.AUDIO_MP3_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && codecA.contains("aac")) {
			setMimeType(HTTPResource.AUDIO_MP4_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && codecA.contains("flac")) {
			setMimeType(HTTPResource.AUDIO_FLAC_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && codecA.contains("vorbis")) {
			setMimeType(HTTPResource.AUDIO_OGG_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && (codecA.contains("asf") || codecA.startsWith("wm"))) {
			setMimeType(HTTPResource.AUDIO_WMA_TYPEMIME);
		} else if (getCodecV() == null && codecA != null && (codecA.startsWith("pcm") || codecA.contains("wav"))) {
			setMimeType(HTTPResource.AUDIO_WAV_TYPEMIME);
		} else {
			setMimeType(HTTPResource.getDefaultMimeType(type));
		}

		if (getFirstAudioTrack() == null || !(type == Format.AUDIO && getFirstAudioTrack().getBitsperSample() == 24 && getFirstAudioTrack().getSampleRate() > 48000)) {
			setSecondaryFormatValid(false);
		}

		// Check for external subs here
		if (f.getFile() != null && type == Format.VIDEO && configuration.isAutoloadSubtitles()) {
			FileUtil.isSubtitlesExists(f.getFile(), this);
		}
	}

	@Deprecated
	public boolean isVideoPS3Compatible(InputFile f) {
		return isVideoWithinH264LevelLimits(f, null);
	}

	/**
	 * Checks whether the video has too many reference frames per pixels for the renderer
	 */
	public boolean isVideoWithinH264LevelLimits(InputFile f, RendererConfiguration mediaRenderer) {
		if (!h264_parsed) {
			if (getCodecV() != null && (getCodecV().equals("h264") || getCodecV().startsWith("mpeg2"))) { // what about VC1 ?
				muxable = true;
				if (
					getCodecV().equals("h264") &&
					getContainer() != null &&
					(
						getContainer().equals("matroska") ||
						getContainer().equals("mkv") ||
						getContainer().equals("mov") ||
						getContainer().equals("mp4")
					)
				) { // Containers without h264_annexB
					byte headers[][] = getAnnexBFrameHeader(f);
					if (ffmpeg_annexb_failure) {
						LOGGER.info("Error parsing information from the file: " + f.getFilename());
					}

					if (headers != null) {
						setH264AnnexB(headers[1]);
						if (getH264AnnexB() != null) {
							int skip = 5;
							if (getH264AnnexB()[2] == 1) {
								skip = 4;
							}
							byte header[] = new byte[getH264AnnexB().length - skip];
							System.arraycopy(getH264AnnexB(), skip, header, 0, header.length);
							AVCHeader avcHeader = new AVCHeader(header);
							avcHeader.parse();
							LOGGER.debug("H.264 file: " + f.getFilename() + ": Profile: " + avcHeader.getProfile() + " / level: " + avcHeader.getLevel() + " / ref frames: " + avcHeader.getRef_frames());
							muxable = true;

							if (avcHeader.getLevel() >= 41 && getWidth() > 0 && getHeight() > 0) {
								int maxref;
								if (mediaRenderer == null || mediaRenderer.isPS3()) {
									/**
									 * 2013-01-25: Confirmed maximum reference frames on PS3:
									 *    - 4 for 1920x1080
									 *    - 11 for 1280x720
									 * Meaning this math is correct
									 */
									maxref = (int) Math.floor(10252743 / (getWidth() * getHeight()));
								} else {
									/**
									 * This is the math for level 4.1, which results in:
									 *    - 4 for 1920x1080
									 *    - 9 for 1280x720
									 */
									maxref = (int) Math.floor(8388608 / (getWidth() * getHeight()));
								}

								if (avcHeader.getRef_frames() > maxref) {
									muxable = false;
									LOGGER.debug("The file " + f.getFilename() + " is not compatible with this renderer because it can only take " + maxref + " reference frames at this resolution while this file has " + avcHeader.getRef_frames() + " reference frames");
								} else if (avcHeader.getRef_frames() == -1) {
									muxable = false;
									LOGGER.debug("The file " + f.getFilename() + " may not be compatible with this renderer because we can't get its number of reference frames");
								}
							}
							if (!muxable) {
								LOGGER.debug("H.264 file: " + f.getFilename() + " is not compatible with this renderer");
							}
						} else {
							muxable = false;
						}
					} else {
						muxable = false;
					}
				}
			}

			h264_parsed = true;
		}

		return muxable;
	}

	public boolean isMuxable(String filename, String codecA) {
		return codecA != null && (codecA.startsWith("dts") || codecA.equals("dca"));
	}

	public boolean isLossless(String codecA) {
		return codecA != null && (codecA.contains("pcm") || codecA.startsWith("dts") || codecA.equals("dca") || codecA.contains("flac")) && !codecA.contains("pcm_u8") && !codecA.contains("pcm_s8");
	}

	@Override
	public String toString() {
		String s = "container: " + getContainer() + " / bitrate: " + getBitrate() + " / size: " + getSize() + " / codecV: " + getCodecV() + " / duration: " + getDurationString() + " / width: " + getWidth() + " / height: " + getHeight() + " / frameRate: " + getFrameRate() + " / thumb size : " + (getThumb() != null ? getThumb().length : 0) + " / muxingMode: " + getMuxingMode();

		for (DLNAMediaAudio audio : getAudioTracksList()) {
			s += "\n\taudio: id=" + audio.getId() + " / lang: " + audio.getLang() + " / flavor: " + audio.getFlavor() + " / codec: " + audio.getCodecA() + " / sf:" + audio.getSampleFrequency() + " / na: " + (audio.getAudioProperties() != null ? audio.getAudioProperties().getNumberOfChannels() : "-") + " / bs: " + audio.getBitsperSample();
			if (audio.getArtist() != null) {
				s += " / " + audio.getArtist() + "|" + audio.getAlbum() + "|" + audio.getSongname() + "|" + audio.getYear() + "|" + audio.getTrack();
			}
		}

		for (DLNAMediaSubtitle sub : getSubtitleTracksList()) {
			s += "\n\tsub: id=" + sub.getId() + " / lang: " + sub.getLang() + " / flavor: " + sub.getFlavor() + " / type: " + (sub.getType() != null ? sub.getType().toString() : "null");
		}

		return s;
	}

	public InputStream getThumbnailInputStream() {
		return new ByteArrayInputStream(getThumb());
	}

	public String getValidFps(boolean ratios) {
		String validFrameRate = null;

		if (getFrameRate() != null && getFrameRate().length() > 0) {
			try {
				double fr = Double.parseDouble(getFrameRate().replace(',', '.'));

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
		if (getAudioTracksList().size() > 0) {
			return getAudioTracksList().get(0);
		}
		return null;
	}

	public String getValidAspect(boolean ratios) {
		String a = null;

		if (getAspect() != null) {
			double ar = Double.parseDouble(getAspect());

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
		if (getWidth() > 0 && getHeight() > 0) {
			return getWidth() + "x" + getHeight();
		}

		return null;
	}

	public int getRealVideoBitrate() {
		if (getBitrate() > 0) {
			return (getBitrate() / 8);
		}

		int realBitrate = 10000000;

		if (getDurationInSeconds() != 0) {
			realBitrate = (int) (getSize() / getDurationInSeconds());
		}

		return realBitrate;
	}

	public boolean isHDVideo() {
		return (getWidth() > 1200 || getHeight() > 700);
	}

	public boolean isMpegTS() {
		return getContainer() != null && getContainer().equals("mpegts");
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
		cmdArray[5] = "-vcodec";
		cmdArray[6] = "copy";
		cmdArray[7] = "-f";
		cmdArray[8] = "h264";
		cmdArray[9] = "-vbsf";
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
					ffmpeg_annexb_failure = true;
				} catch (InterruptedException e) { }
				pw.stopProcess();
			}
		};

		Thread failsafe = new Thread(r, "FFMpeg AnnexB Frame Header Failsafe");
		failsafe.start();
		pw.runInSameThread();

		if (ffmpeg_annexb_failure) {
			return null;
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
	protected Object clone() throws CloneNotSupportedException {
		Object cloned = super.clone();

		if (cloned instanceof DLNAMediaInfo) {
			DLNAMediaInfo mediaCloned = ((DLNAMediaInfo) cloned);
			mediaCloned.setAudioTracksList(new ArrayList<DLNAMediaAudio>());

			for (DLNAMediaAudio audio : getAudioTracksList()) {
				mediaCloned.getAudioTracksList().add((DLNAMediaAudio) audio.clone());
			}

			mediaCloned.setSubtitleTracksList(new ArrayList<DLNAMediaSubtitle>());

			for (DLNAMediaSubtitle sub : getSubtitleTracksList()) {
				mediaCloned.getSubtitleTracksList().add((DLNAMediaSubtitle) sub.clone());
			}
		}

		return cloned;
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
		this.codecV = codecV;
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
	 * @return the aspect
	 * @since 1.50.0
	 */
	public String getAspect() {
		return aspect;
	}

	/**
	 * @param aspect the aspect to set
	 * @since 1.50.0
	 */
	public void setAspect(String aspect) {
		this.aspect = aspect;
	}

	/**
	 * @return the aspect ratio reported by the container
	 */
	public String getAspectRatioContainer() {
		return aspectRatioContainer;
	}

	/**
	 * @param aspect the aspect ratio to set
	 */
	public void setAspectRatioContainer(String aspect) {
		this.aspectRatioContainer = aspect;
	}

	/**
	 * @return the aspect ratio of the video track
	 */
	public String getAspectRatioVideoTrack() {
		return aspectRatioVideoTrack;
	}

	/**
	 * @param aspect the aspect ratio to set
	 */
	public void setAspectRatioVideoTrack(String aspect) {
		this.aspectRatioVideoTrack = aspect;
	}

	/**
	 * @return the thumb
	 * @since 1.50.0
	 */
	public byte[] getThumb() {
		return thumb;
	}

	/**
	 * @param thumb the thumb to set
	 * @since 1.50.0
	 */
	public void setThumb(byte[] thumb) {
		this.thumb = thumb;
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
	 * @return the audioTracks
	 * @since 1.60.0
	 */
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
		return h264_annexB;
	}

	/**
	 * @param h264AnnexB the h264_annexB to set
	 * @since 1.50.0
	 */
	public void setH264AnnexB(byte[] h264AnnexB) {
		this.h264_annexB = h264AnnexB;
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
		return parsing;
	}

	/**
	 * @param parsing the parsing to set
	 * @since 1.50.0
	 */
	public void setParsing(boolean parsing) {
		this.parsing = parsing;
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
}
