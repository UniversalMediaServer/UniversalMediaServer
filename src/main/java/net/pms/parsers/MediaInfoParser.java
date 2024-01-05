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
package net.pms.parsers;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.media.audio.MediaAudio;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.mediaserver.handlers.api.starrating.StarRating;
import net.pms.parsers.mediainfo.InfoKind;
import net.pms.parsers.mediainfo.MediaInfoHelper;
import net.pms.parsers.mediainfo.StreamKind;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;
import net.pms.util.StringUtil;
import net.pms.util.UnknownFormatException;
import net.pms.util.Version;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoParser.class);

	// Regular expression to parse a 4 digit year number from a string
	private static final String YEAR_REGEX = ".*([\\d]{4}).*";

	// Pattern to parse the year from a string
	private static final Pattern YEAR_PATTERN = Pattern.compile(YEAR_REGEX);

	private static final MediaInfoHelper MI;
	private static final Version VERSION;

	static {
		MI = new MediaInfoHelper();

		if (MI.isValid()) {
			MI.option("Internet", "No"); // avoid MediaInfoLib to try to connect to an Internet server for availability of newer software, anonymous statistics and retrieving information about a file
			MI.option("Complete", "1");
			MI.option("Language", "en");
			MI.option("File_TestContinuousFileNames", "0");
			Matcher matcher = Pattern.compile("MediaInfoLib - v(\\S+)", Pattern.CASE_INSENSITIVE).matcher(MI.option("Info_Version"));
			if (matcher.find() && isNotBlank(matcher.group(1))) {
				VERSION = new Version(matcher.group(1));
			} else {
				VERSION = null;
			}

			if (VERSION != null)  {
				if (VERSION.isGreaterThan(new Version("18.03"))) {
					MI.option("Language", "raw");
					MI.option("Cover_Data", "base64");
				}

				if (VERSION.isGreaterThan(new Version("18.5"))) {
					MI.option("LegacyStreamDisplay", "1");
					MI.option("File_HighestFormat", "0");
					MI.option("File_ChannelLayout", "1");
					MI.option("Legacy", "1");
				}
			}

//			LOGGER.debug(MI.Option("Info_Parameters_CSV")); // It can be used to export all current MediaInfoHelper parameters
		} else {
			VERSION = null;
		}
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private MediaInfoParser() {
	}

	public static boolean isValid() {
		return MI.isValid();
	}

	public static void close() {
		try {
			MI.close();
		} catch (Throwable e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	/**
	 * @return The {@code LibMediaInfo} {@link Version} or {@code null} if
	 *         unknown.
	 */
	@Nullable
	public static Version getVersion() {
		return VERSION;
	}

	/**
	 * Parse media via MediaInfoHelper.
	 */
	public static synchronized void parse(MediaInfo media, InputFile inputFile, int type) {
		media.waitMediaParsing(5);
		media.setParsing(true);
		File file = inputFile.getFile();
		ParseLogger parseLogger = new ParseLogger();
		Boolean fileOpened = MI.openFile(file.getAbsolutePath()) > 0;
		if (!media.isMediaparsed() && file != null && MI.isValid() && fileOpened) {
			StreamKind general = StreamKind.GENERAL;
			StreamKind video = StreamKind.VIDEO;
			StreamKind audio = StreamKind.AUDIO;
			StreamKind image = StreamKind.IMAGE;
			StreamKind text = StreamKind.TEXT;
			MediaAudio currentAudioTrack = new MediaAudio();
			MediaSubtitle currentSubTrack;
			media.setSize(file.length());
			String value;

			// set General
			setFormat(general, media, currentAudioTrack, MI.get(general, 0, "Format"), file);
			setFormat(general, media, currentAudioTrack, MI.get(general, 0, "CodecID").trim(), file);
			media.setDuration(parseDuration(MI.get(general, 0, "Duration")));
			media.setBitrate(getBitrate(MI.get(general, 0, "OverallBitRate")));
			media.setStereoscopy(MI.get(general, 0, "StereoscopicLayout"));
			// set Chapters
			if (MI.countGet(StreamKind.MENU, 0) > 0) {
				String chaptersPosBeginStr = MI.get(StreamKind.MENU, 0, "Chapters_Pos_Begin", InfoKind.TEXT);
				String chaptersPosEndStr = MI.get(StreamKind.MENU, 0, "Chapters_Pos_End", InfoKind.TEXT);
				if (!chaptersPosBeginStr.isEmpty() && !chaptersPosEndStr.isEmpty()) {
					int chaptersPosBegin = Integer.parseInt(chaptersPosBeginStr);
					int chaptersPosEnd = Integer.parseInt(chaptersPosEndStr);
					List<MediaChapter> chapters = new ArrayList<>();
					for (int i = chaptersPosBegin; i <= chaptersPosEnd; i++) {
						String chapterName = MI.get(StreamKind.MENU, 0, i, InfoKind.NAME);
						String chapterTitle = MI.get(StreamKind.MENU, 0, i, InfoKind.TEXT);
						if (!chapterName.isEmpty()) {
							MediaChapter chapter = new MediaChapter();
							LocalTime lt;
							try {
								lt = LocalTime.parse(chapterName, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
							} catch (DateTimeParseException e) {
								LOGGER.debug("Skip chapter as time cannot be parsed: {}", chapterName);
								continue;
							}
							chapter.setId(i - chaptersPosBegin);
							chapter.setStart(lt.toNanoOfDay() / 1000_000_000D);
							//set end for previous chapter
							if (!chapters.isEmpty()) {
								chapters.get(chapters.size() - 1).setEnd(chapter.getStart());
							}
							if (!chapterTitle.isEmpty()) {
								String lang = MediaLang.UND;
								chapter.setLang(lang);
								if (chapterTitle.startsWith(":")) {
									chapterTitle = chapterTitle.substring(1);
								} else if (chapterTitle.length() > 2 && ':' == chapterTitle.charAt(2) && (chapterTitle.length() < 15 || ':' != chapterTitle.charAt(5) || ':' == chapterTitle.charAt(8))) {
									lang = chapterTitle.substring(0, 2);
									chapterTitle = chapterTitle.substring(3);
								}
								//do not set title if it is default, it will be filled automatically later
								if (!MediaChapter.isTitleDefault(chapterTitle)) {
									chapter.setLang(lang);
									chapter.setTitle(chapterTitle);
								}
							}
							chapters.add(chapter);
						}
					}
					//set end for previous chapter
					if (!chapters.isEmpty()) {
						chapters.get(chapters.size() - 1).setEnd(media.getDurationInSeconds());
					}
					media.setChapters(chapters);
				}
			}
			value = MI.get(general, 0, "Cover_Data");
			if (!value.isEmpty()) {
				try {
					media.setThumb(DLNAThumbnail.toThumbnail(
						new Base64().decode(value.getBytes(StandardCharsets.US_ASCII)),
						640,
						480,
						ScaleType.MAX,
						ImageFormat.SOURCE,
						false
					));
				} catch (EOFException e) {
					LOGGER.debug(
						"Error reading \"{}\" thumbnail from MediaInfo: Unexpected end of stream, probably corrupt or read error.",
						file.getName()
					);
				} catch (UnknownFormatException e) {
					LOGGER.debug("Could not read \"{}\" thumbnail from MediaInfo: {}", file.getName(), e.getMessage());
				} catch (IOException e) {
					LOGGER.error("Error reading \"{}\" thumbnail from MediaInfo: {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			}

			value = MI.get(general, 0, "Title");
			if (!value.isEmpty()) {
				media.setFileTitleFromMetadata(value);
			}

			if (parseLogger != null) {
				parseLogger.logGeneralColumns(file);
			}

			// set Video
			int videoTrackCount = 0;
			value = MI.get(video, 0, "StreamCount");
			if (!value.isEmpty()) {
				videoTrackCount = Integer.parseInt(value);
			}

			media.setVideoTrackCount(videoTrackCount);
			if (videoTrackCount > 0) {
				for (int i = 0; i < videoTrackCount; i++) {
					// check for DXSA and DXSB subtitles (subs in video format)
					if (MI.get(video, i, "Title").startsWith("Subtitle")) {
						currentSubTrack = new MediaSubtitle();
						// First attempt to detect subtitle track format
						currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(MI.get(video, i, "Format")));
						// Second attempt to detect subtitle track format (CodecID usually is more accurate)
						currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(MI.get(video, i, "CodecID"),
							currentSubTrack.getType()
						));
						currentSubTrack.setId(media.getSubtitlesTracks().size());
						addSub(currentSubTrack, media);
					} else {
						setFormat(video, media, currentAudioTrack, MI.get(video, i, "Format"), file);
						setFormat(video, media, currentAudioTrack, MI.get(video, i, "Format_Version"), file);
						setFormat(video, media, currentAudioTrack, MI.get(video, i, "CodecID"), file);
						media.setWidth(getPixelValue(MI.get(video, i, "Width")));
						media.setHeight(getPixelValue(MI.get(video, i, "Height")));
						media.setMatrixCoefficients(MI.get(video, i, "matrix_coefficients"));
						if (!media.is3d()) {
							media.setStereoscopy(MI.get(video, i, "MultiView_Layout"));
						}

						media.setPixelAspectRatio(MI.get(video, i, "PixelAspectRatio"));
						media.setScanType(MI.get(video, i, "ScanType"));
						media.setScanOrder(MI.get(video, i, "ScanOrder"));
						media.setAspectRatioContainer(MI.get(video, i, "DisplayAspectRatio/String"));
						media.setAspectRatioVideoTrack(MI.get(video, i, "DisplayAspectRatio_Original/String"));
						media.setFrameRate(getFPSValue(MI.get(video, i, "FrameRate")));
						media.setFrameRateOriginal(MI.get(video, i, "FrameRate_Original"));
						media.setFrameRateMode(getFrameRateModeValue(MI.get(video, i, "FrameRate_Mode")));
						media.setFrameRateModeRaw(MI.get(video, i, "FrameRate_Mode"));
						media.setReferenceFrameCount(getReferenceFrameCount(MI.get(video, i, "Format_Settings_RefFrames/String")));
						media.setVideoTrackTitleFromMetadata(MI.get(video, i, "Title"));
						value = MI.get(video, i, "Format_Settings_QPel");
						if (!value.isEmpty()) {
							media.putExtra(FormatConfiguration.MI_QPEL, value);
						}

						value = MI.get(video, i, "Format_Settings_GMC");
						if (!value.isEmpty()) {
							media.putExtra(FormatConfiguration.MI_GMC, value);
						}

						value = MI.get(video, i, "Format_Settings_GOP");
						if (!value.isEmpty()) {
							media.putExtra(FormatConfiguration.MI_GOP, value);
						}

						media.setMuxingMode(MI.get(video, i, "MuxingMode"));
						if (!media.isEncrypted()) {
							media.setEncrypted("encrypted".equals(MI.get(video, i, "Encryption")));
						}

						value = MI.get(video, i, "BitDepth");
						if (!value.isEmpty()) {
							try {
								media.setVideoBitDepth(Integer.parseInt(value));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse bits per sample \"" + value + "\"");
							}
						}

						value = MI.get(video, i, "HDR_Format");
						if (!value.isEmpty()) {
							media.setVideoHDRFormat(value);
						}

						value = MI.get(video, i, "HDR_Format_Compatibility");
						if (!value.isEmpty()) {
							media.setVideoHDRFormatCompatibility(value);
						}

						value = MI.get(video, i, "Format_Profile");
						if (!value.isEmpty() && media.getCodecV() != null && media.getCodecV().equals(FormatConfiguration.H264)) {
							media.setAvcLevel(getAvcLevel(value));
							media.setH264Profile(getAvcProfile(value));
						}

						if (parseLogger != null) {
							parseLogger.logVideoTrackColumns(i);
						}
					}
				}
			}

			// set Audio
			int audioTracks = 0;
			value = MI.get(audio, 0, "StreamCount");
			if (!value.isEmpty()) {
				audioTracks = Integer.parseInt(value);
			}

			if (audioTracks > 0) {
				for (int i = 0; i < audioTracks; i++) {
					currentAudioTrack = new MediaAudio();
					setFormat(audio, media, currentAudioTrack, MI.get(audio, i, "Format/String"), file);
					setFormat(audio, media, currentAudioTrack, MI.get(audio, i, "Format_Version"), file);
					setFormat(audio, media, currentAudioTrack, MI.get(audio, i, "Format_Profile"), file);
					setFormat(audio, media, currentAudioTrack, MI.get(audio, i, "CodecID"), file);
					value = MI.get(audio, i, "CodecID_Description");
					if (isNotBlank(value) && value.startsWith("Windows Media Audio 10")) {
						currentAudioTrack.setCodecA(FormatConfiguration.WMA10);
					}

					value = MI.get(audio, i, "Language/String");
					String languageCode = null;
					if (isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value);
						if (languageCode != null) {
							currentAudioTrack.setLang(languageCode);
						}
					}

					value = MI.get(audio, i, "Title").trim();
					currentAudioTrack.setAudioTrackTitleFromMetadata(value);
					// if language code is null try to recognize the language from Title
					if (languageCode == null && isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value, true);
						if (languageCode == null) {
							languageCode = MediaLang.UND;
						}

						currentAudioTrack.setLang(languageCode);
					}

					currentAudioTrack.getAudioProperties().setNumberOfChannels(MI.get(audio, i, "Channel(s)"));
					currentAudioTrack.setSampleFrequency(getSampleFrequency(MI.get(audio, i, "SamplingRate")));
					currentAudioTrack.setBitRate(getBitrate(MI.get(audio, i, "BitRate")));

					currentAudioTrack.setSongname(MI.get(general, 0, "Track"));
					currentAudioTrack.setAlbum(MI.get(general, 0, "Album"));
					currentAudioTrack.setAlbumArtist(MI.get(general, 0, "Album/Performer"));
					currentAudioTrack.setArtist(getArtist());
					currentAudioTrack.setGenre(MI.get(general, 0, "Genre"));
					currentAudioTrack.setComposer(MI.get(general, 0, "Composer"));
					currentAudioTrack.setConductor(MI.get(general, 0, "Conductor"));

					if (videoTrackCount == 0) {
						try {
							AudioFile af;
							if ("mp2".equals(FileUtil.getExtension(file).toLowerCase(Locale.ROOT))) {
								af = AudioFileIO.readAs(file, "mp3");
							} else {
								af = AudioFileIO.read(file);
							}
							addMusicBrainzIDs(af, file, currentAudioTrack);
							addAudioTrackRating(af, file, currentAudioTrack);
						} catch (Exception e) {
							LOGGER.debug("Could not parse audio file");
						}
					}

					value = MI.get(general, 0, "Track/Position");
					if (!value.isEmpty()) {
						try {
							currentAudioTrack.setTrack(Integer.parseInt(value));
						} catch (NumberFormatException nfe) {
							LOGGER.debug("Could not parse track \"" + value + "\"");
						}
					}

					value = MI.get(general, 0, "Part");
					if (!value.isEmpty()) {
						try {
							currentAudioTrack.setDisc(Integer.parseInt(value));
						} catch (NumberFormatException nfe) {
							LOGGER.debug("Could not parse disc \"" + value + "\"");
						}
					}

					// Try to parse the year from the stored date
					String recordedDate = MI.get(general, 0, "Recorded_Date");
					Matcher matcher = YEAR_PATTERN.matcher(recordedDate);
					if (matcher.matches()) {
						try {
							currentAudioTrack.setYear(Integer.parseInt(matcher.group(1)));
						} catch (NumberFormatException nfe) {
							LOGGER.debug("Could not parse year from recorded date \"" + recordedDate + "\"");
						}
					}

					// Special check for OGM: MediaInfoHelper reports specific Audio/Subs IDs (0xn) while MEncoder does not
					value = MI.get(audio, i, "ID/String");
					if (!value.isEmpty()) {
						if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
							currentAudioTrack.setId(getSpecificID(value));
						} else {
							currentAudioTrack.setId(media.getAudioTracksList().size());
						}
					}

					value = MI.get(audio, i, "BitDepth");
					if (!value.isEmpty()) {
						try {
							currentAudioTrack.setBitsperSample(Integer.parseInt(value));
						} catch (NumberFormatException nfe) {
							LOGGER.debug("Could not parse bits per sample \"" + value + "\"");
						}
					}

					addAudio(currentAudioTrack, media);
					if (parseLogger != null) {
						parseLogger.logAudioTrackColumns(i);
					}
				}
			}

			// set Image
			int imageCount = 0;
			value = MI.get(image, 0, "StreamCount");
			if (!value.isEmpty()) {
				imageCount = Integer.parseInt(value);
			}

			media.setImageCount(imageCount);
			if (imageCount > 0 || type == Format.IMAGE) {
				boolean parseByMediainfo = false;
				// For images use our own parser instead of MediaInfoHelper which doesn't provide enough information
				try {
					ImagesUtil.parseImage(file, media);
					// This is a little hack. MediaInfoHelper only recognizes a few image formats
					// so that MI.Count_Get(image) might return 0 even if there is an image.
					if (media.getImageCount() == 0) {
						media.setImageCount(1);
					}
				} catch (IOException e) {
					if (media.getImageCount() > 0) {
						LOGGER.debug("Error parsing image ({}), switching to MediaInfo: {}", file.getAbsolutePath(), e.getMessage());
						LOGGER.trace("", e);
						parseByMediainfo = true;
					} else {
						LOGGER.warn("Image parsing for \"{}\" failed both with MediaInfo and internally: {}", file.getAbsolutePath(), e.getMessage());
						LOGGER.trace("", e);
						media.setImageCount(1);
					}
				}

				if (parseByMediainfo) {
					setFormat(image, media, currentAudioTrack, MI.get(image, 0, "Format"), file);
					media.setWidth(getPixelValue(MI.get(image, 0, "Width")));
					media.setHeight(getPixelValue(MI.get(image, 0, "Height")));
				}

				if (parseLogger != null) {
					parseLogger.logImageColumns(0);
				}
			}

			// set Subs in text format
			int subTracks = 0;
			value = MI.get(text, 0, "StreamCount");
			if (!value.isEmpty()) {
				subTracks = Integer.parseInt(value);
			}

			if (subTracks > 0) {
				for (int i = 0; i < subTracks; i++) {
					currentSubTrack = new MediaSubtitle();
					currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(MI.get(text, i, "CodecID"),
						SubtitleType.valueOfMediaInfoValue(MI.get(text, i, "Format"))
					));

					value = MI.get(text, i, "Language/String");
					String languageCode = null;
					if (isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value.toLowerCase(Locale.ROOT));
						if (languageCode != null) {
							currentSubTrack.setLang(languageCode);
						}
					}

					value = MI.get(text, i, "Title").trim();
					currentSubTrack.setSubtitlesTrackTitleFromMetadata(value);
					// if language code is null try to recognize the language from Title
					if (languageCode == null && isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value, true);
						if (languageCode == null) {
							languageCode = MediaLang.UND;
						}

						currentSubTrack.setLang(languageCode);
					}

					// Special check for OGM: MediaInfoHelper reports specific Audio/Subs IDs (0xn) while mencoder/FFmpeg does not
					value = MI.get(text, i, "ID/String");
					if (isNotBlank(value)) {
						if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
							currentSubTrack.setId(getSpecificID(value));
						} else {
							currentSubTrack.setId(media.getSubtitlesTracks().size());
						}
					}

					value = MI.get(text, i, "Default/String");
					if (isNotBlank(value) && "Yes".equals(value)) {
						currentSubTrack.setDefault(true);
					}

					value = MI.get(text, i, "Forced/String");
					if (isNotBlank(value) && "Yes".equals(value)) {
						currentSubTrack.setForced(true);
					}

					addSub(currentSubTrack, media);
					if (parseLogger != null) {
						parseLogger.logSubtitleTrackColumns(i, false);
					}
				}
			}

			/*
			 * Some container formats (like MP4/M4A) can represent both audio
			 * and video media. UMS initially recognized this as video, but this
			 * is corrected here if the content is only audio.
			 */
			if (media.isAudioOrVideoContainer() && media.isAudio()) {
				media.setContainer(media.getAudioVariantFormatConfigurationString());
			}

			// Separate ASF from WMV
			if (FormatConfiguration.WMV.equals(media.getContainer())) {
				if (
					media.getCodecV() != null &&
					!media.getCodecV().equals(FormatConfiguration.WMV) &&
					!media.getCodecV().equals(FormatConfiguration.VC1)
				) {
					media.setContainer(FormatConfiguration.ASF);
				} else {
					for (MediaAudio audioTrack : media.getAudioTracksList()) {
						if (
							audioTrack.getCodecA() != null &&
							!audioTrack.getCodecA().equals(FormatConfiguration.WMA) &&
							!audioTrack.getCodecA().equals(FormatConfiguration.WMAPRO) &&
							!audioTrack.getCodecA().equals(FormatConfiguration.WMALOSSLESS) &&
							!audioTrack.getCodecA().equals(FormatConfiguration.WMAVOICE) &&
							!audioTrack.getCodecA().equals(FormatConfiguration.WMA10) &&
							!audioTrack.getCodecA().equals(FormatConfiguration.MP3) // up to 128 kbit/s only (WMVSPML_MP3 profile)
						) {
							media.setContainer(FormatConfiguration.ASF);
							break;
						}
					}
				}
			}

			/*
			 * Recognize 3D layout from the filename.
			 *
			 * First we check for our custom naming convention, for which the filename
			 * either has to start with "3DSBSLF" or "3DSBSRF" for side-by-side layout
			 * or "3DOULF" or "3DOURF" for over-under layout.
			 * For anaglyph 3D video can be used following combination:
			 * 		3DARCG 	anaglyph_red_cyan_gray
			 *		3DARCH 	anaglyph_red_cyan_half_color
			 *		3DARCC 	anaglyph_red_cyan_color
			 *		3DARCD 	anaglyph_red_cyan_dubois
			 *		3DAGMG 	anaglyph_green_magenta_gray
			 *		3DAGMH 	anaglyph_green_magenta_half_color
			 *		3DAGMC 	anaglyph_green_magenta_color
			 *		3DAGMD 	anaglyph_green_magenta_dubois
			 *		3DAYBG 	anaglyph_yellow_blue_gray
			 *		3DAYBH 	anaglyph_yellow_blue_half_color
			 *		3DAYBC 	anaglyph_yellow_blue_color
			 *		3DAYBD 	anaglyph_yellow_blue_dubois
			 *
			 * Next we check for common naming conventions.
			 */
			if (!media.is3d()) {
				String upperCaseFileName = file.getName().toUpperCase();
				if (upperCaseFileName.startsWith("3DSBS")) {
					LOGGER.debug("3D format SBS detected for " + file.getName());
					media.setStereoscopy(file.getName().substring(2, 7));
				} else if (upperCaseFileName.startsWith("3DOU")) {
					LOGGER.debug("3D format OU detected for " + file.getName());
					media.setStereoscopy(file.getName().substring(2, 6));
				} else if (upperCaseFileName.startsWith("3DA")) {
					LOGGER.debug("3D format Anaglyph detected for " + file.getName());
					media.setStereoscopy(file.getName().substring(2, 6));
				} else if (upperCaseFileName.matches(".*[\\s\\.](H-|H|HALF-|HALF.)SBS[\\s\\.].*")) {
					LOGGER.debug("3D format HSBS detected for " + file.getName());
					media.setStereoscopy("half side by side (left eye first)");
				} else if (upperCaseFileName.matches(".*[\\s\\.](H-|H|HALF-|HALF.)(OU|TB)[\\s\\.].*")) {
					LOGGER.debug("3D format HOU detected for " + file.getName());
					media.setStereoscopy("half top-bottom (left eye first)");
				} else if (upperCaseFileName.matches(".*[\\s\\.]SBS[\\s\\.].*")) {
					if (media.getWidth() > 1920) {
						LOGGER.debug("3D format SBS detected for " + file.getName());
						media.setStereoscopy("side by side (left eye first)");
					} else {
						LOGGER.debug("3D format HSBS detected based on width for " + file.getName());
						media.setStereoscopy("half side by side (left eye first)");
					}
				} else if (upperCaseFileName.matches(".*[\\s\\.](OU|TB)[\\s\\.].*")) {
					if (media.getHeight() > 1080) {
						LOGGER.debug("3D format OU detected for " + file.getName());
						media.setStereoscopy("top-bottom (left eye first)");
					} else {
						LOGGER.debug("3D format HOU detected based on height for " + file.getName());
						media.setStereoscopy("half top-bottom (left eye first)");
					}
				}
			}
			media.postParse(type, inputFile);
			if (parseLogger != null) {
				LOGGER.trace("{}", parseLogger);
			}

			MI.closeFile();
			if (media.getContainer() == null) {
				media.setContainer(MediaLang.UND);
			}

			if (media.getCodecV() == null) {
				media.setCodecV(MediaLang.UND);
			}

			media.setMediaparsed(true);
		}
		media.setParsing(false);
	}

	/**
	 * Reads artist either from the ARTISTS tag or if empty from the PERFORMER tag.
	 *
	 * @return artist
	 */
	private static String getArtist() {
		StreamKind general = StreamKind.GENERAL;
		String artist = MI.get(general, 0, "ARTISTS");
		if (StringUtils.isBlank(artist)) {
			artist = MI.get(general, 0, "Performer");
		}
		return artist;
	}

	private static void addMusicBrainzIDs(AudioFile af, File file, MediaAudio currentAudioTrack) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				String val = t.getFirst(FieldKey.MUSICBRAINZ_RELEASEID);
				currentAudioTrack.setMbidRecord(val.equals("") ? null : val);
				val = t.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);
				currentAudioTrack.setMbidTrack(val.equals("") ? null : val);
			}
		} catch (Exception e) {
			LOGGER.trace("audio musicBrainz tag not parsed: " + e.getMessage());
		}
	}

	private static void addAudioTrackRating(AudioFile af, File file, MediaAudio currentAudioTrack) {
		try {
			Tag t = af.getTag();
			if (t != null) {
				currentAudioTrack.setRating(StarRating.convertTagRatingToStar(t));
			}
		} catch (Exception e) {
			LOGGER.trace("audio rating tag not parsed: " + e.getMessage());
		}
	}

	public static void addAudio(MediaAudio currentAudioTrack, MediaInfo media) {
		if (isBlank(currentAudioTrack.getLang())) {
			currentAudioTrack.setLang(MediaLang.UND);
		}

		if (isBlank(currentAudioTrack.getCodecA())) {
			currentAudioTrack.setCodecA(MediaLang.UND);
		}

		media.getAudioTracksList().add(currentAudioTrack);
	}

	public static void addSub(MediaSubtitle currentSubTrack, MediaInfo media) {
		if (currentSubTrack.getType() == SubtitleType.UNSUPPORTED) {
			return;
		}

		if (isBlank(currentSubTrack.getLang())) {
			currentSubTrack.setLang(MediaLang.UND);
		}

		media.addSubtitlesTrack(currentSubTrack);
	}

	/**
	 * Sends the correct information to media.setContainer(),
	 * media.setCodecV() or media.setCodecA, depending on streamType.
	 *
	 * Note: A lot of these are types of MPEG-4 Audio and this can be a
	 * good resource to make sense of that:
	 * https://en.wikipedia.org/wiki/MPEG-4_Part_3#MPEG-4_Audio_Object_Types
	 * There are also free samples of most of them at:
	 * http://fileformats.archiveteam.org/wiki/MPEG-4_SLS
	 *
	 * @param streamType
	 * @param media
	 * @param audio
	 * @param value
	 * @param file
	 * @todo Rename to something like setFormat - this is not a getter.
	 * @todo Split the values by streamType to make the logic more clear
	 *       with less negative statements.
	 */
	protected static void setFormat(StreamKind streamType, MediaInfo media, MediaAudio audio, String value, File file) {
		if (isBlank(value)) {
			return;
		}

		value = value.toLowerCase(Locale.ROOT);
		String format = null;

		if (isBlank(value)) {
			return;
		} else if (value.startsWith("3g2")) {
			format = FormatConfiguration.THREEGPP2;
		} else if (value.startsWith("3gp")) {
			format = FormatConfiguration.THREEGPP;
		} else if (value.startsWith("matroska")) {
			format = FormatConfiguration.MKV;
		} else if (value.equals("avi") || value.equals("opendml")) {
			format = FormatConfiguration.AVI;
		} else if (value.startsWith("cinepa")) {
			format = FormatConfiguration.CINEPAK;
		} else if (value.startsWith("flash")) {
			format = FormatConfiguration.FLV;
		} else if (value.equals("webm")) {
			format = FormatConfiguration.WEBM;
		} else if (value.equals("qt") || value.equals("quicktime")) {
			format = FormatConfiguration.MOV;
		} else if (
			value.contains("isom") ||
			(streamType != StreamKind.AUDIO && value.startsWith("mp4") && !value.startsWith("mp4a")) ||
			value.equals("20") ||
			value.equals("isml") ||
			(value.startsWith("m4a") && !value.startsWith("m4ae")) ||
			value.startsWith("m4v") ||
			value.equals("mpeg-4 visual")
		) {
			format = FormatConfiguration.MP4;
		} else if (value.contains("mpeg-ps")) {
			format = FormatConfiguration.MPEGPS;
		} else if (value.contains("mpeg-ts") || value.equals("bdav")) {
			format = FormatConfiguration.MPEGTS;
		} else if (value.equals("caf")) {
			format = FormatConfiguration.CAF;
		} else if (value.contains("aiff")) {
			format = FormatConfiguration.AIFF;
		} else if (value.startsWith("atmos") || value.equals("131")) {
			format = FormatConfiguration.ATMOS;
		} else if (value.contains("ogg")) {
			format = FormatConfiguration.OGG;
		} else if (value.contains("opus")) {
			format = FormatConfiguration.OPUS;
		} else if (value.contains("realmedia") || value.startsWith("rv")) {
			format = FormatConfiguration.RM;
		} else if (value.startsWith("theora")) {
			format = FormatConfiguration.THEORA;
		} else if (
			value.startsWith("windows media") ||
			value.equals("wmv1") ||
			value.equals("wmv2")
		) {
			format = FormatConfiguration.WMV;
		} else if (
			streamType == StreamKind.VIDEO &&
			(
				value.contains("mjpg") ||
				value.contains("mjpeg") ||
				value.equals("mjpa") ||
				value.equals("mjpb") ||
				value.equals("jpeg") ||
				value.equals("jpeg2000")
			)
		) {
			format = FormatConfiguration.MJPEG;
		} else if (value.equals("h261")) {
			format = FormatConfiguration.H261;
		} else if (
			value.equals("h263") ||
			value.equals("s263") ||
			value.equals("u263")
		) {
			format = FormatConfiguration.H263;
		} else if (streamType == StreamKind.VIDEO && (value.startsWith("avc") || value.startsWith("h264"))) {
			format = FormatConfiguration.H264;
		} else if (value.startsWith("hevc")) {
			format = FormatConfiguration.H265;
		} else if (value.startsWith("sorenson")) {
			format = FormatConfiguration.SORENSON;
		} else if (value.startsWith("vp6")) {
			format = FormatConfiguration.VP6;
		} else if (value.startsWith("vp7")) {
			format = FormatConfiguration.VP7;
		} else if (value.startsWith("vp8")) {
			format = FormatConfiguration.VP8;
		} else if (value.startsWith("vp9")) {
			format = FormatConfiguration.VP9;
		} else if (
				value.startsWith("div") ||
				value.startsWith("xvid") ||
				value.equals("dx50") ||
				value.equals("dvx1")
			) {
				format = FormatConfiguration.DIVX;
		} else if (value.startsWith("indeo")) { // Intel Indeo Video: IV31, IV32, IV41 and IV50
			format = FormatConfiguration.INDEO;
		} else if (streamType == StreamKind.VIDEO && value.equals("yuv")) {
			format = FormatConfiguration.YUV;
		} else if (streamType == StreamKind.VIDEO && (value.equals("rgb") || value.equals("rgba"))) {
			format = FormatConfiguration.RGB;
		} else if (streamType == StreamKind.VIDEO && value.equals("rle")) {
			format = FormatConfiguration.RLE;
		} else if (value.equals("mac3")) {
			format = FormatConfiguration.MACE3;
		} else if (value.equals("mac6")) {
			format = FormatConfiguration.MACE6;
		} else if (streamType == StreamKind.VIDEO && value.startsWith("tga")) {
			format = FormatConfiguration.TGA;
		} else if (value.equals("ffv1")) {
			format = FormatConfiguration.FFV1;
		} else if (value.equals("celp")) {
			format = FormatConfiguration.CELP;
		} else if (value.equals("qcelp")) {
			format = FormatConfiguration.QCELP;
		} else if (
			value.matches("(?i)(dv)|(cdv.?)|(dc25)|(dcap)|(dvc.?)|(dvs.?)|(dvrs)|(dv25)|(dv50)|(dvan)|(dvh.?)|(dvis)|(dvl.?)|(dvnm)|(dvp.?)|(mdvf)|(pdvc)|(r411)|(r420)|(sdcc)|(sl25)|(sl50)|(sldv)") &&
			!value.contains("dvhe")
		) {
			format = FormatConfiguration.DV;
		} else if (value.contains("mpeg video")) {
			format = FormatConfiguration.MPEG2;
		} else if (value.startsWith("version 1")) {
			if (media.getCodecV() != null && media.getCodecV().equals(FormatConfiguration.MPEG2) && audio.getCodecA() == null) {
				format = FormatConfiguration.MPEG1;
			}
		} else if (
				value.equals("vc-1") ||
				value.equals("wvc1") ||
				value.equals("wmv3") ||
				value.equals("wmvp") ||
				value.equals("wmva")
			) {
				format = FormatConfiguration.VC1;
		} else if (value.equals("au") || value.equals("ulaw/au audio file")) {
			format = FormatConfiguration.AU;
		} else if (value.equals("av01") || value.contains("av1")) {
			format = FormatConfiguration.AV1;
		} else if (value.equals("layer 3")) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.MPA)) {
				format = FormatConfiguration.MP3;
				// special case:
				if (media.getContainer() != null && media.getContainer().equals(FormatConfiguration.MPA)) {
					media.setContainer(FormatConfiguration.MP3);
				}
			}
		} else if (
			value.equals("layer 2") &&
			audio.getCodecA() != null &&
			media.getContainer() != null &&
			audio.getCodecA().equals(FormatConfiguration.MPA) &&
			media.getContainer().equals(FormatConfiguration.MPA)
		) {
			// only for audio files:
			format = FormatConfiguration.MP2;
			media.setContainer(FormatConfiguration.MP2);
		} else if (
			value.equals("ma") ||
			value.equals("ma / core") ||
			value.equals("x / ma / core") ||
			value.equals("imax / x / ma / core") ||
			value.equals("134")
		) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.DTS)) {
				format = FormatConfiguration.DTSHD;
			}
		} else if (value.equals("vorbis") || value.equals("a_vorbis")) {
			format = FormatConfiguration.VORBIS;
		} else if (value.equals("adts")) {
			format = FormatConfiguration.ADTS;
		} else if (value.startsWith("amr")) {
			format = FormatConfiguration.AMR;
		} else if (value.equals("dolby e")) {
			format = FormatConfiguration.DOLBYE;
		} else if (
			value.equals("ac-3") ||
			value.equals("a_ac3") ||
			value.equals("2000")
		) {
			format = FormatConfiguration.AC3;
		} else if (value.startsWith("cook")) {
			format = FormatConfiguration.COOK;
		} else if (value.startsWith("qdesign")) {
			format = FormatConfiguration.QDESIGN;
		} else if (value.equals("realaudio lossless")) {
			format = FormatConfiguration.RALF;
		} else if (value.equals("e-ac-3")) {
			format = FormatConfiguration.EAC3;
		} else if (value.contains("truehd")) {
			format = FormatConfiguration.TRUEHD;
		} else if (value.equals("tta")) {
			format = FormatConfiguration.TTA;
		} else if (value.equals("55") || value.equals("a_mpeg/l3")) {
			format = FormatConfiguration.MP3;
		} else if (
			value.equals("lc") ||
			value.equals("aac lc") ||
			value.equals("mp4a-40-2") ||
			value.equals("00001000-0000-FF00-8000-00AA00389B71") ||
			(
				value.equals("aac") &&
				FormatConfiguration.AVI.equals(media.getContainer())
			)
		) {
			format = FormatConfiguration.AAC_LC;
		} else if (value.equals("aac lc sbr")) {
			format = FormatConfiguration.HE_AAC;
		} else if (value.equals("ltp")) {
			format = FormatConfiguration.AAC_LTP;
		} else if (value.contains("he-aac")) {
			format = FormatConfiguration.HE_AAC;
		} else if (value.equals("main")) {
			format = FormatConfiguration.AAC_MAIN;
		} else if (value.equals("ssr")) {
			format = FormatConfiguration.AAC_SSR;
		} else if (value.startsWith("a_aac")) {
			if (value.equals("a_aac/mpeg2/main")) {
				format = FormatConfiguration.AAC_MAIN;
			} else if (
				value.equals("a_aac/mpeg2/lc") ||
				value.equals("a_aac-2")
			) {
				format = FormatConfiguration.AAC_LC;
			} else if (value.equals("a_aac/mpeg2/lc/sbr")) {
				format = FormatConfiguration.HE_AAC;
			} else if (value.equals("a_aac/mpeg2/ssr")) {
				format = FormatConfiguration.AAC_SSR;
			} else if (value.equals("a_aac/mpeg4/main")) {
				format = FormatConfiguration.AAC_MAIN;
			} else if (value.equals("a_aac/mpeg4/lc")) {
				format = FormatConfiguration.AAC_LC;
			} else if (value.equals("a_aac/mpeg4/lc/sbr")) {
				format = FormatConfiguration.HE_AAC;
			} else if (value.equals("a_aac/mpeg4/lc/sbr/ps")) { // HE-AACv2
				format = FormatConfiguration.HE_AAC;
			} else if (value.equals("a_aac/mpeg4/ssr")) {
				format = FormatConfiguration.AAC_SSR;
			} else if (value.equals("a_aac/mpeg4/ltp")) {
				format = FormatConfiguration.AAC_LTP;
			} else {
				format = FormatConfiguration.AAC_MAIN;
			}
		} else if (
			value.equals("er bsac") ||
			value.equals("mp4a-40-22")
		) {
			format = FormatConfiguration.ER_BSAC;
		} else if (value.startsWith("adpcm")) {
			format = FormatConfiguration.ADPCM;
		} else if (value.equals("pcm") || (value.equals("1") && (audio.getCodecA() == null || !audio.getCodecA().equals(FormatConfiguration.DTS)))) {
			format = FormatConfiguration.LPCM;
		} else if (value.equals("alac")) {
			format = FormatConfiguration.ALAC;
		} else if (value.equals("als")) {
			format = FormatConfiguration.ALS;
		} else if (value.equals("wave")) {
			format = FormatConfiguration.WAV;
		} else if (value.equals("shorten")) {
			format = FormatConfiguration.SHORTEN;
		} else if (value.equals("sls") || value.equals("SLS non-core")) {
			format = FormatConfiguration.SLS;
		} else if (value.equals("acelp")) {
			format = FormatConfiguration.ACELP;
		} else if (value.equals("g.729") || value.equals("g.729a")) {
			format = FormatConfiguration.G729;
		} else if (value.equals("vselp")) {
			format = FormatConfiguration.REALAUDIO_14_4;
		} else if (value.equals("g.728")) {
			format = FormatConfiguration.REALAUDIO_28_8;
		} else if (value.equals("a_real/sipr") || value.equals("kevin")) {
			format = FormatConfiguration.SIPRO;
		} else if (
			(
				value.equals("dts") ||
				value.equals("a_dts") ||
				value.equals("8")
			) &&
			(
				audio.getCodecA() == null ||
				!audio.getCodecA().equals(FormatConfiguration.DTSHD)
			)
		) {
			format = FormatConfiguration.DTS;
		} else if (value.equals("mpeg audio")) {
			format = FormatConfiguration.MPA;
		} else if (value.equals("wma")) {
			format = FormatConfiguration.WMA;
			if (media.getCodecV() == null) {
				media.setContainer(format);
			}
		} else if (
			streamType == StreamKind.AUDIO &&
			media.getContainer() != null &&
				(
					media.getContainer().equals(FormatConfiguration.WMA) ||
					media.getContainer().equals(FormatConfiguration.WMV)
				)
			) {
			if (value.equals("160") || value.equals("161")) {
				format = FormatConfiguration.WMA;
			} else if (value.equals("162")) {
				format = FormatConfiguration.WMAPRO;
			} else if (value.equals("163")) {
				format = FormatConfiguration.WMALOSSLESS;
			} else if (value.equalsIgnoreCase("A")) {
				format = FormatConfiguration.WMAVOICE;
			} else if (value.equals("wma10")) {
				format = FormatConfiguration.WMA10;
			}
		} else if (value.equals("flac") || "19d".equals(value)) { // https://github.com/MediaArea/MediaInfoLib/issues/594
			format = FormatConfiguration.FLAC;
		} else if (value.equals("monkey's audio")) {
			format = FormatConfiguration.MONKEYS_AUDIO;
		} else if (value.contains("musepack")) {
			format = FormatConfiguration.MPC;
		} else if (value.contains("wavpack")) {
			format = FormatConfiguration.WAVPACK;
		} else if (value.contains("mlp")) {
			format = FormatConfiguration.MLP;
		} else if (value.equals("openmg")) {
			format = FormatConfiguration.ATRAC;
		} else if (value.startsWith("atrac") || value.endsWith("-a119-fffa01e4ce62") || value.endsWith("-88fc-61654f8c836c")) {
			format = FormatConfiguration.ATRAC;
			if (streamType == StreamKind.AUDIO && !FormatConfiguration.ATRAC.equals(media.getContainer())) {
				media.setContainer(FormatConfiguration.ATRAC);
			}
		} else if (value.equals("nellymoser")) {
			format = FormatConfiguration.NELLYMOSER;
		} else if (value.equals("jpeg")) {
			format = FormatConfiguration.JPG;
		} else if (value.equals("png")) {
			format = FormatConfiguration.PNG;
		} else if (value.equals("gif")) {
			format = FormatConfiguration.GIF;
		} else if (value.equals("bitmap")) {
			format = FormatConfiguration.BMP;
		} else if (value.equals("tiff")) {
			format = FormatConfiguration.TIFF;
		} else if (containsIgnoreCase(value, "@l") && streamType == StreamKind.VIDEO) {
			media.setAvcLevel(getAvcLevel(value));
			media.setH264Profile(getAvcProfile(value));
		}

		if (format != null) {
			if (streamType == StreamKind.GENERAL) {
				media.setContainer(format);
			} else if (streamType == StreamKind.VIDEO) {
				media.setCodecV(format);
			} else if (streamType == StreamKind.AUDIO) {
				audio.setCodecA(format);
			}
		// format not found so set container type based on the file extension. It will be overwritten when the correct type will be found
		} else if (streamType == StreamKind.GENERAL && media.getContainer() == null) {
			media.setContainer(FileUtil.getExtension(file.getAbsolutePath()).toLowerCase(Locale.ROOT));
		}
	}

	public static int getPixelValue(String value) {
		if (isBlank(value)) {
			return 0;
		}
		if (value.contains("pixel")) {
			value = value.substring(0, value.indexOf("pixel"));
		}

		value = value.trim();

		// Value can look like "512 / 512" at this point
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf('/')).trim();
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			LOGGER.debug("Could not parse pixels \"{}\": {}", value, e.getMessage());
			LOGGER.trace("", e);
			return 0;
		}
	}

	/**
	 * @param value {@code Format_Settings_RefFrames/String} value to parse.
	 * @return reference frame count or {@code -1} if could not parse.
	 */
	public static byte getReferenceFrameCount(String value) {
		if (isBlank(value)) {
			return -1;
		}

		try {
			// Values like "16 frame3"
			return Byte.parseByte(substringBefore(value, " "));
		} catch (NumberFormatException ex) {
			// Not parsed
			LOGGER.warn("Could not parse ReferenceFrameCount value {}.", value);
			LOGGER.warn("Exception: ", ex);
			return -1;
		}
	}

	/**
	 * @param value {@code Format_Profile} value to parse.
	 * @return AVC level or {@code null} if could not parse.
	 */
	public static String getAvcLevel(String value) {
		// Example values:
		// High@L3.0
		// High@L4.0
		// High@L4.1
		final String avcLevel = substringAfterLast(lowerCase(value), "@l");
		if (isNotBlank(avcLevel)) {
			return avcLevel;
		}
		LOGGER.warn("Could not parse AvcLevel value {}.", value);
		return null;
	}

	public static String getAvcProfile(String value) {
		String profile = substringBefore(lowerCase(value), "@l");
		if (isNotBlank(profile)) {
			return profile;
		}
		LOGGER.warn("Could not parse AvcProfile value {}.", value);
		return null;
	}

	public static int getVideoBitrate(String value) {
		if (isBlank(value)) {
			return 0;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			LOGGER.trace("Could not parse video bitrate \"{}\": ", value, e.getMessage());
			return 0;
		}
	}

	public static int getBitrate(String value) {
		if (value.isEmpty()) {
			return 0;
		}

		if (value.contains("/")) {
			value = value.substring(0, value.indexOf('/')).trim();
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			LOGGER.trace("Could not parse bitrate \"{}\": ", value, e.getMessage());
			return 0;
		}
	}

	public static int getSpecificID(String value) {
		// If ID is given as 'streamID-substreamID' use the second (which is hopefully unique).
		// For example in vob audio ID can be '189 (0xBD)-32 (0x80)' and text ID '189 (0xBD)-128 (0x20)'
		int end = value.lastIndexOf("(0x");
		if (end > -1) {
			int start = value.lastIndexOf('-') + 1;
			value = value.substring(start > end ? 0 : start, end);
		}

		value = value.trim();
		return Integer.parseInt(value);
	}

	public static String getSampleFrequency(String value) {
		/**
		 * Some tracks show several values, e.g. "48000 / 48000 / 24000" for HE-AAC
		 * We store only the first value
		 */
		if (value.indexOf('/') > -1) {
			value = value.substring(0, value.indexOf('/'));
		}

		if (value.contains("khz")) {
			value = value.substring(0, value.indexOf("khz"));
		}

		value = value.trim();
		return value;
	}

	public static String getFPSValue(String value) {
		if (value.contains("fps")) {
			value = value.substring(0, value.indexOf("fps"));
		}

		value = value.trim();
		return value;
	}

	public static String getFrameRateModeValue(String value) {
		if (value.indexOf('/') > -1) {
			value = value.substring(0, value.indexOf('/'));
		}

		value = value.trim();
		return value;
	}

	public static String getLang(String value) {
		if (value.indexOf('(') > -1) {
			value = value.substring(0, value.indexOf('('));
		}

		if (value.indexOf('/') > -1) {
			value = value.substring(0, value.indexOf('/'));
		}

		value = value.trim();
		return value;
	}

	/**
	 * Parses the "Duration" format.
	 */
	@Nullable
	private static Double parseDuration(String value) {
		if (isBlank(value)) {
			return null;
		}
		String[] parts = value.split("\\s*/\\s*");
		value = parts[parts.length - 1];
		int separator = value.indexOf(".");
		if (separator > 0) {
			value = value.substring(0, separator);
		}
		try {
			long longValue = Long.parseLong(value);
			return Double.valueOf(longValue / 1000.0);
		} catch (NumberFormatException e) {
			LOGGER.warn("Could not parse duration from \"{}\"", value);
			return null;
		}
	}

	protected static class ParseLogger {

		private final StringBuilder sb = new StringBuilder();
		private final Columns generalColumns = new Columns(false, 2, 32, 62, 92);
		private final Columns streamColumns = new Columns(false, 4, 34, 64, 94);

		/**
		 * Appends a label and value to the internal {@link StringBuilder} at
		 * the next column using the specified parameters.
		 *
		 * @param columns the {@link Columns} to use.
		 * @param label the label.
		 * @param value the value.
		 * @param quote if {@code true}, {@code value} is wrapped in double
		 *            quotes.
		 * @param notBlank if {@code true}, doesn't append anything if
		 *            {@code value} is {@code null} or only whitespace.
		 * @return {@code true} if something was appended, {@code false}
		 *         otherwise.
		 */
		private boolean appendStringNextColumn(
			Columns columns,
			String label,
			String value,
			boolean quote,
			boolean notBlank
		) {
			if (notBlank && isBlank(value)) {
				return false;
			}
			sb.append(columns.toNextColumnRelative(sb));
			appendString(label, value, true, quote, false);
			return true;
		}

		/**
		 * Appends a label and value to the internal {@link StringBuilder} at
		 * the specified column using the specified parameters.
		 *
		 * @param columns the {@link Columns} to use.
		 * @param column the column number.
		 * @param label the label.
		 * @param value the value.
		 * @param quote if {@code true}, {@code value} is wrapped in double
		 *            quotes.
		 * @param notBlank if {@code true}, doesn't append anything if
		 *            {@code value} is {@code null} or only whitespace.
		 * @return {@code true} if something was appended, {@code false}
		 *         otherwise.
		 */
		private boolean appendStringColumn(
			Columns columns,
			int column,
			String label,
			String value,
			boolean quote,
			boolean notBlank
		) {
			if (notBlank && isBlank(value)) {
				return false;
			}
			sb.append(columns.toColumn(sb, column));
			appendString(label, value, true, quote, false);
			return true;
		}

		/**
		 * Appends a label and value to the internal {@link StringBuilder} using
		 * the specified parameters.
		 *
		 * @param label the label.
		 * @param value the value.
		 * @param first if {@code false}, {@code ", "} is added first.
		 * @param quote if {@code true}, {@code value} is wrapped in double
		 *            quotes.
		 * @param notBlank if {@code true}, doesn't append anything if
		 *            {@code value} is {@code null} or only whitespace.
		 * @return {@code true} if something was appended, {@code false}
		 *         otherwise.
		 */
		private boolean appendString(String label, String value, boolean first, boolean quote, boolean notBlank) {
			if (notBlank && isBlank(value)) {
				return false;
			}
			if (!first) {
				sb.append(", ");
			}
			sb.append(label);
			if (quote) {
				sb.append(": \"");
			} else {
				sb.append(": ");
			}
			sb.append(quote ? value : value.trim());
			if (quote) {
				sb.append("\"");
			}
			return true;
		}

		/**
		 * Appends a label and a boolean value to the internal
		 * {@link StringBuilder} at the next column using the specified
		 * parameters. The boolean value will be {@code "False"} if
		 * {@code value} is {@code null} or only whitespace, {@code "True"}
		 * otherwise.
		 *
		 * @param columns the {@link Columns} to use.
		 * @param label the label.
		 * @param value the value to evaluate.
		 * @param booleanValues if {@code true}, {@code "True"} and
		 *            {@code "False"} will be used. If {@code false},
		 *            {@code "Yes"} and {@code "No"} will be used.
		 * @return Always {@code true}.
		 */
		private boolean appendExistsNextColumn(Columns columns, String label, String value, boolean booleanValues) {
			sb.append(columns.toNextColumnRelative(sb));
			appendExists(label, value, true, booleanValues);
			return true;
		}

		/**
		 * Appends a label and a boolean value to the internal
		 * {@link StringBuilder} at the specified column using the specified
		 * parameters. The boolean value will be {@code "False"} if
		 * {@code value} is {@code null} or only whitespace, {@code "True"}
		 * otherwise.
		 *
		 * @param columns the {@link Columns} to use.
		 * @param column the column number.
		 * @param label the label.
		 * @param value the value to evaluate.
		 * @param booleanValues if {@code true}, {@code "True"} and
		 *            {@code "False"} will be used. If {@code false},
		 *            {@code "Yes"} and {@code "No"} will be used.
		 * @return Always {@code true}.
		 */
		private boolean appendExistsColumn(
			Columns columns,
			int column,
			String label,
			String value,
			boolean booleanValues
		) {
			sb.append(columns.toColumn(sb, column));
			appendExists(label, value, true, booleanValues);
			return true;
		}

		/**
		 * Appends a label and a boolean value to the internal
		 * {@link StringBuilder} using the specified parameters. The boolean
		 * value will be {@code "False"} if {@code value} is {@code null} or
		 * only whitespace, {@code "True"} otherwise.
		 *
		 * @param label the label.
		 * @param value the value to evaluate.
		 * @param first if {@code false}, {@code ", "} is added first.
		 * @param booleanValues if {@code true}, {@code "True"} and
		 *            {@code "False"} will be used. If {@code false},
		 *            {@code "Yes"} and {@code "No"} will be used.
		 * @return Always {@code true}.
		 */
		private boolean appendExists(String label, String value, boolean first, boolean booleanValues) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(label).append(": ");
			if (isBlank(value)) {
				sb.append(booleanValues ? "False" : "No");
			} else {
				sb.append(booleanValues ? "True" : "Yes");
			}
			return true;
		}

		public void logGeneral(File file) {
			if (file == null) {
				sb.append("MediaInfo parsing results for null:\n");
			} else {
				sb.append("MediaInfo parsing results for \"").append(file.getAbsolutePath()).append("\":\n");
			}
			if (MI == null) {
				sb.append("ERROR: LibMediaInfo instance is null");
				return;
			}
			if (!MI.isValid()) {
				sb.append("ERROR: LibMediaInfo instance not valid");
				return;
			}
			sb.append("  ");
			boolean first = true;
			first &= !appendString("Title", MI.get(StreamKind.GENERAL, 0, "Title"), first, true, true);
			first &= !appendString("Format", MI.get(StreamKind.GENERAL, 0, "Format"), first, true, false);
			first &= !appendString("CodecID", MI.get(StreamKind.GENERAL, 0, "CodecID"), first, true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.GENERAL, 0, "Duration"));
			if (durationSec != null) {
				first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
			}
			first &= !appendString("Overall Bitrate Mode", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Mode"), first, false, true);
			first &= !appendString("Overall Bitrate", MI.get(StreamKind.GENERAL, 0, "OverallBitRate"), first, false, true);
			first &= !appendString("Overall Bitrate Nom.", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Nominal"), first, false, true);
			first &= !appendString("Overall Bitrate Max.", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Maximum"), first, false, true);
			first &= !appendString("Stereoscopic", MI.get(StreamKind.GENERAL, 0, "StereoscopicLayout"), first, true, true);
			appendExists("Cover", MI.get(StreamKind.GENERAL, 0, "Cover_Data"), first, false);
			first = false;
			appendString("FPS", MI.get(StreamKind.GENERAL, 0, "FrameRate"), first, false, true);
			appendString("Track", MI.get(StreamKind.GENERAL, 0, "Track"), first, true, true);
			appendString("Album", MI.get(StreamKind.GENERAL, 0, "Album"), first, true, true);
			appendString("Performer", MI.get(StreamKind.GENERAL, 0, "Performer"), first, true, true);
			appendString("Genre", MI.get(StreamKind.GENERAL, 0, "Genre"), first, true, true);
			appendString("Rec Date", MI.get(StreamKind.GENERAL, 0, "Recorded_Date"), first, true, true);
		}

		public void logGeneralColumns(File file) {
			if (file == null) {
				sb.append("MediaInfo parsing results for null:\n");
			} else {
				sb.append("MediaInfo parsing results for \"").append(file.getAbsolutePath()).append("\":\n");
			}
			if (MI == null) {
				sb.append("ERROR: LibMediaInfo instance is null");
				return;
			}
			if (!MI.isValid()) {
				sb.append("ERROR: LibMediaInfo instance not valid");
				return;
			}
			generalColumns.reset();
			appendStringNextColumn(generalColumns, "Title", MI.get(StreamKind.GENERAL, 0, "Title"), true, true);
			appendStringNextColumn(generalColumns, "Format", MI.get(StreamKind.GENERAL, 0, "Format"), true, false);
			appendStringNextColumn(generalColumns, "CodecID", MI.get(StreamKind.GENERAL, 0, "CodecID"), true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.GENERAL, 0, "Duration"));
			if (durationSec != null) {
				appendStringNextColumn(generalColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
			}
			appendStringNextColumn(generalColumns, "Overall Bitrate Mode", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Mode"), false, true);
			appendStringNextColumn(generalColumns, "Overall Bitrate", MI.get(StreamKind.GENERAL, 0, "OverallBitRate"), false, true);
			appendStringNextColumn(generalColumns, "Overall Bitrate Nom.", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Nominal"), false, true);
			appendStringNextColumn(generalColumns, "Overall Bitrate Max.", MI.get(StreamKind.GENERAL, 0, "OverallBitRate_Maximum"), false, true);
			appendStringNextColumn(generalColumns, "Stereoscopic", MI.get(StreamKind.GENERAL, 0, "StereoscopicLayout"), true, true);
			appendExistsNextColumn(generalColumns, "Cover", MI.get(StreamKind.GENERAL, 0, "Cover_Data"), false);
			appendStringNextColumn(generalColumns, "FPS", MI.get(StreamKind.GENERAL, 0, "FrameRate"), false, true);
			appendStringNextColumn(generalColumns, "Track", MI.get(StreamKind.GENERAL, 0, "Track"), true, true);
			appendStringNextColumn(generalColumns, "Album", MI.get(StreamKind.GENERAL, 0, "Album"), true, true);
			appendStringNextColumn(generalColumns, "Performer", MI.get(StreamKind.GENERAL, 0, "Performer"), true, true);
			appendStringNextColumn(generalColumns, "Genre", MI.get(StreamKind.GENERAL, 0, "Genre"), true, true);
			appendStringNextColumn(generalColumns, "Rec Date", MI.get(StreamKind.GENERAL, 0, "Recorded_Date"), true, true);
		}

		public void logVideoTrack(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n    - Video - ");
			boolean first = true;
			first &= !appendString("Format", MI.get(StreamKind.VIDEO, idx, "Format"), first, true, true);
			first &= !appendString("Version", MI.get(StreamKind.VIDEO, idx, "Format_Version"), first, true, true);
			first &= !appendString("Profile", MI.get(StreamKind.VIDEO, idx, "Format_Profile"), first, true, true);
			first &= !appendString("ID", MI.get(StreamKind.VIDEO, idx, "ID"), first, false, true);
			first &= !appendString("CodecID", MI.get(StreamKind.VIDEO, idx, "CodecID"), first, true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.VIDEO, 0, "Duration"));
			if (durationSec != null) {
				first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
			}
			first &= !appendString("BitRate Mode", MI.get(StreamKind.VIDEO, idx, "BitRate_Mode"), first, false, true);
			first &= !appendString("Bitrate", MI.get(StreamKind.VIDEO, idx, "BitRate"), first, false, true);
			first &= !appendString("Bitrate Nominal", MI.get(StreamKind.VIDEO, idx, "BitRate_Nominal"), first, false, true);
			first &= !appendString("BitRate Maximum", MI.get(StreamKind.VIDEO, idx, "BitRate_Maximum"), first, false, true);
			first &= !appendString("Bitrate Encoded", MI.get(StreamKind.VIDEO, idx, "BitRate_Encoded"), first, false, true);
			first &= !appendString("Width", MI.get(StreamKind.VIDEO, idx, "Width"), first, false, true);
			first &= !appendString("Height", MI.get(StreamKind.VIDEO, idx, "Height"), first, false, true);
			first &= !appendString("Colorimetry", MI.get(StreamKind.VIDEO, idx, "Colorimetry"), first, false, true);
			first &= !appendString("Chroma", MI.get(StreamKind.VIDEO, idx, "ChromaSubsampling"), first, false, true);
			first &= !appendString("Matrix Co", MI.get(StreamKind.VIDEO, idx, "matrix_coefficients"), first, false, true);
			first &= !appendString("MultiView Layout", MI.get(StreamKind.VIDEO, idx, "MultiView_Layout"), first, true, true);
			first &= !appendString("PAR", MI.get(StreamKind.VIDEO, idx, "PixelAspectRatio"), first, false, true);
			first &= !appendString("DAR", MI.get(StreamKind.VIDEO, idx, "DisplayAspectRatio/String"), first, false, true);
			first &= !appendString("DAR Orig", MI.get(StreamKind.VIDEO, idx, "DisplayAspectRatio_Original/String"), first, false, true);
			first &= !appendString("Scan Type", MI.get(StreamKind.VIDEO, idx, "ScanType"), first, false, true);
			first &= !appendString("Scan Order", MI.get(StreamKind.VIDEO, idx, "ScanOrder"), first, false, true);
			first &= !appendString("FPS", MI.get(StreamKind.VIDEO, idx, "FrameRate"), first, false, true);
			first &= !appendString("FPS Orig", MI.get(StreamKind.VIDEO, idx, "FrameRate_Original"), first, false, true);
			first &= !appendString("Framerate Mode", MI.get(StreamKind.VIDEO, idx, "FrameRate_Mode"), first, false, true);
			first &= !appendString("RefFrames", MI.get(StreamKind.VIDEO, idx, "Format_Settings_RefFrames"), first, false, true);
			first &= !appendString("QPel", MI.get(StreamKind.VIDEO, idx, "Format_Settings_QPel"), first, true, true);
			first &= !appendString("GMC", MI.get(StreamKind.VIDEO, idx, "Format_Settings_GMC"), first, true, true);
			first &= !appendString("GOP", MI.get(StreamKind.VIDEO, idx, "Format_Settings_GOP"), first, true, true);
			first &= !appendString("Muxing Mode", MI.get(StreamKind.VIDEO, idx, "MuxingMode"), first, true, true);
			first &= !appendString("Encrypt", MI.get(StreamKind.VIDEO, idx, "Encryption"), first, true, true);
			first &= !appendString("Bit Depth", MI.get(StreamKind.VIDEO, idx, "BitDepth"), first, false, true);
			first &= !appendString("Delay", MI.get(StreamKind.VIDEO, idx, "Delay"), first, false, true);
			first &= !appendString("Delay Source", MI.get(StreamKind.VIDEO, idx, "Delay_Source"), first, false, true);
			first &= !appendString("Delay Original", MI.get(StreamKind.VIDEO, idx, "Delay_Original"), first, false, true);
			first &= !appendString("Delay O. Source", MI.get(StreamKind.VIDEO, idx, "Delay_Original_Source"), first, false, true);
			first &= !appendString("TimeStamp_FirstFrame", MI.get(StreamKind.VIDEO, idx, "TimeStamp_FirstFrame"), first, false, true);
		}

		public void logVideoTrackColumns(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n  - Video track ");
			appendString("ID", MI.get(StreamKind.VIDEO, idx, "ID"), true, false, false);
			streamColumns.reset();
			sb.append("\n");
			appendStringNextColumn(streamColumns, "Format", MI.get(StreamKind.VIDEO, idx, "Format"), true, true);
			appendStringNextColumn(streamColumns, "Version", MI.get(StreamKind.VIDEO, idx, "Format_Version"), true, true);
			appendStringNextColumn(streamColumns, "Profile", MI.get(StreamKind.VIDEO, idx, "Format_Profile"), true, true);
			appendStringNextColumn(streamColumns, "CodecID", MI.get(StreamKind.VIDEO, idx, "CodecID"), true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.VIDEO, 0, "Duration"));
			if (durationSec != null) {
				appendStringNextColumn(streamColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
			}
			appendStringNextColumn(streamColumns, "BitRate Mode", MI.get(StreamKind.VIDEO, idx, "BitRate_Mode"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate", MI.get(StreamKind.VIDEO, idx, "BitRate"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate Nominal", MI.get(StreamKind.VIDEO, idx, "BitRate_Nominal"), false, true);
			appendStringNextColumn(streamColumns, "BitRate Maximum", MI.get(StreamKind.VIDEO, idx, "BitRate_Maximum"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate Encoded", MI.get(StreamKind.VIDEO, idx, "BitRate_Encoded"), false, true);
			appendStringNextColumn(streamColumns, "Width", MI.get(StreamKind.VIDEO, idx, "Width"), false, true);
			appendStringNextColumn(streamColumns, "Height", MI.get(StreamKind.VIDEO, idx, "Height"), false, true);
			appendStringNextColumn(streamColumns, "Colorimetry", MI.get(StreamKind.VIDEO, idx, "Colorimetry"), false, true);
			appendStringNextColumn(streamColumns, "Chroma", MI.get(StreamKind.VIDEO, idx, "ChromaSubsampling"), false, true);
			appendStringNextColumn(streamColumns, "Matrix Co", MI.get(StreamKind.VIDEO, idx, "matrix_coefficients"), false, true);
			appendStringNextColumn(streamColumns, "MultiView Layout", MI.get(StreamKind.VIDEO, idx, "MultiView_Layout"), true, true);
			appendStringNextColumn(streamColumns, "PAR", MI.get(StreamKind.VIDEO, idx, "PixelAspectRatio"), false, true);
			appendStringNextColumn(streamColumns, "DAR", MI.get(StreamKind.VIDEO, idx, "DisplayAspectRatio/String"), false, true);
			appendStringNextColumn(streamColumns, "DAR Orig", MI.get(StreamKind.VIDEO, idx, "DisplayAspectRatio_Original/String"), false, true);
			appendStringNextColumn(streamColumns, "Scan Type", MI.get(StreamKind.VIDEO, idx, "ScanType"), false, true);
			appendStringNextColumn(streamColumns, "Scan Order", MI.get(StreamKind.VIDEO, idx, "ScanOrder"), false, true);
			appendStringNextColumn(streamColumns, "FPS", MI.get(StreamKind.VIDEO, idx, "FrameRate"), false, true);
			appendStringNextColumn(streamColumns, "FPS Orig", MI.get(StreamKind.VIDEO, idx, "FrameRate_Original"), false, true);
			appendStringNextColumn(streamColumns, "Framerate Mode", MI.get(StreamKind.VIDEO, idx, "FrameRate_Mode"), false, true);
			appendStringNextColumn(streamColumns, "RefFrames", MI.get(StreamKind.VIDEO, idx, "Format_Settings_RefFrames"), false, true);
			appendStringNextColumn(streamColumns, "QPel", MI.get(StreamKind.VIDEO, idx, "Format_Settings_QPel"), true, true);
			appendStringNextColumn(streamColumns, "GMC", MI.get(StreamKind.VIDEO, idx, "Format_Settings_GMC"), true, true);
			appendStringNextColumn(streamColumns, "GOP", MI.get(StreamKind.VIDEO, idx, "Format_Settings_GOP"), true, true);
			appendStringNextColumn(streamColumns, "Muxing Mode", MI.get(StreamKind.VIDEO, idx, "MuxingMode"), true, true);
			appendStringNextColumn(streamColumns, "Encrypt", MI.get(StreamKind.VIDEO, idx, "Encryption"), true, true);
			appendStringNextColumn(streamColumns, "Bit Depth", MI.get(StreamKind.VIDEO, idx, "BitDepth"), false, true);
			appendStringNextColumn(streamColumns, "Delay", MI.get(StreamKind.VIDEO, idx, "Delay"), false, true);
			appendStringNextColumn(streamColumns, "Delay Source", MI.get(StreamKind.VIDEO, idx, "Delay_Source"), false, true);
			appendStringNextColumn(streamColumns, "Delay Original", MI.get(StreamKind.VIDEO, idx, "Delay_Original"), false, true);
			appendStringNextColumn(streamColumns, "Delay O. Source", MI.get(StreamKind.VIDEO, idx, "Delay_Original_Source"), false, true);
			appendStringNextColumn(streamColumns, "TimeStamp_FirstFrame", MI.get(StreamKind.VIDEO, idx, "TimeStamp_FirstFrame"), false, true);
		}

		public void logAudioTrack(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n    - Audio - ");
			boolean first = true;
			first &= !appendString("Title", MI.get(StreamKind.AUDIO, idx, "Title"), first, true, true);
			first &= !appendString("Format", MI.get(StreamKind.AUDIO, idx, "Format"), first, true, true);
			first &= !appendString("Version", MI.get(StreamKind.AUDIO, idx, "Format_Version"), first, true, true);
			first &= !appendString("Profile", MI.get(StreamKind.AUDIO, idx, "Format_Profile"), first, true, true);
			first &= !appendString("ID", MI.get(StreamKind.AUDIO, idx, "ID"), first, false, true);
			first &= !appendString("CodecID", MI.get(StreamKind.AUDIO, idx, "CodecID"), first, true, true);
			first &= !appendString("CodecID Desc", MI.get(StreamKind.AUDIO, idx, "CodecID_Description"), first, true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.AUDIO, 0, "Duration"));
			if (durationSec != null) {
				first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
			}
			first &= !appendString("BitRate Mode", MI.get(StreamKind.AUDIO, idx, "BitRate_Mode"), first, false, true);
			first &= !appendString("Bitrate", MI.get(StreamKind.AUDIO, idx, "BitRate"), first, false, true);
			first &= !appendString("Bitrate Nominal", MI.get(StreamKind.AUDIO, idx, "BitRate_Nominal"), first, false, true);
			first &= !appendString("BitRate Maximum", MI.get(StreamKind.AUDIO, idx, "BitRate_Maximum"), first, false, true);
			first &= !appendString("Bitrate Encoded", MI.get(StreamKind.AUDIO, idx, "BitRate_Encoded"), first, false, true);
			first &= !appendString("Language", MI.get(StreamKind.AUDIO, idx, "Language"), first, true, true);
			first &= !appendString("Channel(s)", MI.get(StreamKind.AUDIO, idx, "Channel(s)_Original"), first, false, true);
			first &= !appendString("Samplerate", MI.get(StreamKind.AUDIO, idx, "SamplingRate"), first, false, true);
			first &= !appendString("Track", MI.get(StreamKind.GENERAL, idx, "Track/Position"), first, false, true);
			first &= !appendString("Bit Depth", MI.get(StreamKind.AUDIO, idx, "BitDepth"), first, false, true);
			first &= !appendString("Delay", MI.get(StreamKind.AUDIO, idx, "Delay"), first, false, true);
			first &= !appendString("Delay Source", MI.get(StreamKind.AUDIO, idx, "Delay_Source"), first, false, true);
			first &= !appendString("Delay Original", MI.get(StreamKind.AUDIO, idx, "Delay_Original"), first, false, true);
			first &= !appendString("Delay O. Source", MI.get(StreamKind.AUDIO, idx, "Delay_Original_Source"), first, false, true);
		}

		public void logAudioTrackColumns(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n  - Audio track ");
			appendString("ID", MI.get(StreamKind.AUDIO, idx, "ID"), true, false, false);
			appendString("Title", MI.get(StreamKind.AUDIO, idx, "Title"), false, true, true);
			streamColumns.reset();
			sb.append("\n");
			appendStringNextColumn(streamColumns, "Format", MI.get(StreamKind.AUDIO, idx, "Format/String"), true, true);
			appendStringNextColumn(streamColumns, "Version", MI.get(StreamKind.AUDIO, idx, "Format_Version"), true, true);
			appendStringNextColumn(streamColumns, "Profile", MI.get(StreamKind.AUDIO, idx, "Format_Profile"), true, true);
			appendStringNextColumn(streamColumns, "CodecID", MI.get(StreamKind.AUDIO, idx, "CodecID"), true, true);
			appendStringNextColumn(streamColumns, "CodecID Desc", MI.get(StreamKind.AUDIO, idx, "CodecID_Description"), true, true);
			Double durationSec = parseDuration(MI.get(StreamKind.AUDIO, 0, "Duration"));
			if (durationSec != null) {
				appendStringNextColumn(streamColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
			}
			appendStringNextColumn(streamColumns, "BitRate Mode", MI.get(StreamKind.AUDIO, idx, "BitRate_Mode"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate", MI.get(StreamKind.AUDIO, idx, "BitRate"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate Nominal", MI.get(StreamKind.AUDIO, idx, "BitRate_Nominal"), false, true);
			appendStringNextColumn(streamColumns, "BitRate Maximum", MI.get(StreamKind.AUDIO, idx, "BitRate_Maximum"), false, true);
			appendStringNextColumn(streamColumns, "Bitrate Encoded", MI.get(StreamKind.AUDIO, idx, "BitRate_Encoded"), false, true);
			appendStringNextColumn(streamColumns, "Language", MI.get(StreamKind.AUDIO, idx, "Language"), true, true);
			appendStringNextColumn(streamColumns, "Channel(s)", MI.get(StreamKind.AUDIO, idx, "Channel(s)"), false, true);
			appendStringNextColumn(streamColumns, "Samplerate", MI.get(StreamKind.AUDIO, idx, "SamplingRate"), false, true);
			appendStringNextColumn(streamColumns, "Track", MI.get(StreamKind.GENERAL, idx, "Track/Position"), false, true);
			appendStringNextColumn(streamColumns, "Bit Depth", MI.get(StreamKind.AUDIO, idx, "BitDepth"), false, true);
			appendStringNextColumn(streamColumns, "Delay", MI.get(StreamKind.AUDIO, idx, "Delay"), false, true);
			appendStringNextColumn(streamColumns, "Delay Source", MI.get(StreamKind.AUDIO, idx, "Delay_Source"), false, true);
			appendStringNextColumn(streamColumns, "Delay Original", MI.get(StreamKind.AUDIO, idx, "Delay_Original"), false, true);
			appendStringNextColumn(streamColumns, "Delay O. Source", MI.get(StreamKind.AUDIO, idx, "Delay_Original_Source"), false, true);
		}

		public void logImage(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n    - Image - ");
			boolean first = true;
			first &= !appendString("Format", MI.get(StreamKind.IMAGE, idx, "Format"), first, true, true);
			first &= !appendString("Version", MI.get(StreamKind.IMAGE, idx, "Format_Version"), first, true, true);
			first &= !appendString("Profile", MI.get(StreamKind.IMAGE, idx, "Format_Profile"), first, true, true);
			first &= !appendString("ID", MI.get(StreamKind.IMAGE, idx, "ID"), first, false, true);
			first &= !appendString("Width", MI.get(StreamKind.IMAGE, idx, "Width"), first, false, true);
			first &= !appendString("Height", MI.get(StreamKind.IMAGE, idx, "Height"), first, false, true);
		}

		public void logImageColumns(int idx) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n  - Image ");
			appendString("ID", MI.get(StreamKind.IMAGE, idx, "ID"), true, false, false);
			streamColumns.reset();
			sb.append("\n");
			appendStringNextColumn(streamColumns, "Format", MI.get(StreamKind.IMAGE, idx, "Format"), true, true);
			appendStringNextColumn(streamColumns, "Version", MI.get(StreamKind.IMAGE, idx, "Format_Version"), true, true);
			appendStringNextColumn(streamColumns, "Profile", MI.get(StreamKind.IMAGE, idx, "Format_Profile"), true, true);
			appendStringNextColumn(streamColumns, "Width", MI.get(StreamKind.IMAGE, idx, "Width"), false, true);
			appendStringNextColumn(streamColumns, "Height", MI.get(StreamKind.IMAGE, idx, "Height"), false, true);
		}

		public void logSubtitleTrack(int idx, boolean videoSubtitle) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n    - Sub - ");
			boolean first = true;
			if (videoSubtitle) {
				first &= !appendString("Title", MI.get(StreamKind.VIDEO, idx, "Title"), first, true, true);
				first &= !appendString("Format", MI.get(StreamKind.VIDEO, idx, "Format"), first, true, true);
				first &= !appendString("Version", MI.get(StreamKind.VIDEO, idx, "Format_Version"), first, true, true);
				first &= !appendString("Profile", MI.get(StreamKind.VIDEO, idx, "Format_Profile"), first, true, true);
				first &= !appendString("ID", MI.get(StreamKind.VIDEO, idx, "ID"), first, false, true);
			} else {
				first &= !appendString("Title", MI.get(StreamKind.TEXT, idx, "Title"), first, true, true);
				first &= !appendString("Format", MI.get(StreamKind.TEXT, idx, "Format"), first, true, true);
				first &= !appendString("Version", MI.get(StreamKind.TEXT, idx, "Format_Version"), first, true, true);
				first &= !appendString("Profile", MI.get(StreamKind.TEXT, idx, "Format_Profile"), first, true, true);
				first &= !appendString("ID", MI.get(StreamKind.TEXT, idx, "ID"), first, false, true);
				first &= !appendString("Language", MI.get(StreamKind.TEXT, idx, "Language"), first, true, true);
			}
		}

		public void logSubtitleTrackColumns(int idx, boolean videoSubtitle) {
			if (MI == null || !MI.isValid()) {
				return;
			}

			sb.append("\n  - Subtitle ");
			streamColumns.reset();
			if (videoSubtitle) {
				appendString("ID", MI.get(StreamKind.VIDEO, idx, "ID"), true, false, false);
				appendString("Title", MI.get(StreamKind.VIDEO, idx, "Title"), false, true, true);
				sb.append("\n");
				appendStringNextColumn(streamColumns, "Format", MI.get(StreamKind.VIDEO, idx, "Format"), true, true);
				appendStringNextColumn(streamColumns, "Version", MI.get(StreamKind.VIDEO, idx, "Format_Version"), true, true);
				appendStringNextColumn(streamColumns, "Profile", MI.get(StreamKind.VIDEO, idx, "Format_Profile"), true, true);
			} else {
				appendString("ID", MI.get(StreamKind.TEXT, idx, "ID"), true, false, false);
				appendString("Title", MI.get(StreamKind.TEXT, idx, "Title"), false, true, true);
				sb.append("\n");
				appendStringNextColumn(streamColumns, "Format", MI.get(StreamKind.TEXT, idx, "Format"), true, true);
				appendStringNextColumn(streamColumns, "Version", MI.get(StreamKind.TEXT, idx, "Format_Version"), true, true);
				appendStringNextColumn(streamColumns, "Profile", MI.get(StreamKind.TEXT, idx, "Format_Profile"), true, true);
				appendStringNextColumn(streamColumns, "Language", MI.get(StreamKind.TEXT, idx, "Language"), true, true);
			}
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		protected static class Columns {

			private final boolean includeZeroColumn;
			private final int[] columns;
			private int lastColumn = -1;

			public Columns(boolean includeZeroColumn, int... columns) {
				this.includeZeroColumn = includeZeroColumn;
				this.columns = columns;
			}

			public int lastColumn() {
				return lastColumn;
			}

			public int nextColumn() {
				if (lastColumn < 0 || lastColumn >= columns.length) {
					return includeZeroColumn ? 0 : 1;
				}
				return lastColumn + 1;
			}

			public void reset() {
				lastColumn = -1;
			}

			/**
			 * Returns the whitespace needed to jump to the next sequential
			 * column.
			 */
			public String toNextColumnAbsolute(StringBuilder sb) {
				if (sb == null) {
					return "";
				}

				boolean newLine = false;
				int next = nextColumn();
				if (next < lastColumn) {
					newLine = true;
				}
				return newLine ? "\n" + toColumn(0, nextColumn()) : toColumn(sb, nextColumn());
			}

			/**
			 * Returns the whitespace needed to jump to the next available
			 * column.
			 */
			public String toNextColumnRelative(StringBuilder sb) {
				if (sb == null) {
					return "";
				}

				boolean newLine = false;
				int linePosition = getLinePosition(sb);
				int column = -1;
				if (includeZeroColumn && linePosition == 0) {
					column = 0;
				} else {
					for (int i = 0; i < columns.length; i++) {
						if (columns[i] > linePosition) {
							column = i + 1;
							break;
						}
					}
				}
				if (column < 0) {
					column = includeZeroColumn ? 0 : 1;
					newLine = true;
				}
				return newLine ? "\n" + toColumn(0, column) : toColumn(linePosition, column);
			}

			public String toColumn(StringBuilder sb, int column) {
				if (sb == null || column > columns.length) {
					return "";
				}

				return toColumn(getLinePosition(sb), column);
			}

			public String toColumn(int linePosition, int column) {
				if (column > columns.length || linePosition < 0) {
					return "";
				}
				if (column < 1) {
					lastColumn = 0;
					return linePosition > 0 ? " " : "";
				}

				lastColumn = column;
				int fill = columns[column - 1] - linePosition;
				if (fill < 1 && linePosition > 0) {
					fill = 1;
				}
				return fill > 0 ? StringUtil.fillString(" ", fill) : "";
			}

			public static int getLinePosition(StringBuilder sb) {
				if (sb == null) {
					return 0;
				}
				int position = sb.lastIndexOf("\n");
				if (position < 0) {
					position = sb.length();
				} else {
					position = sb.length() - position - 1;
				}
				return position;
			}

			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder("Columns: 0");
				for (int column : columns) {
					sb.append(", ").append(column);
				}
				return sb.toString();
			}
		}
	}
}
