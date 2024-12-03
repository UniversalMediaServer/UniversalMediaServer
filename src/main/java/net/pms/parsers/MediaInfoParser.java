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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.mediainfo.InfoKind;
import net.pms.parsers.mediainfo.MediaInfoHelper;
import net.pms.parsers.mediainfo.MediaInfoParseLogger;
import net.pms.parsers.mediainfo.StreamAudio;
import net.pms.parsers.mediainfo.StreamContainer;
import net.pms.parsers.mediainfo.StreamImage;
import net.pms.parsers.mediainfo.StreamKind;
import net.pms.parsers.mediainfo.StreamMenu;
import net.pms.parsers.mediainfo.StreamSubtitle;
import net.pms.parsers.mediainfo.StreamVideo;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;
import net.pms.util.UnknownFormatException;
import net.pms.util.Version;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoParser.class);
	// Regular expression to parse a 4 digit year number from a string
	private static final String YEAR_REGEX = ".*([\\d]{4}).*";
	// Pattern to parse the year from a string
	private static final Pattern YEAR_PATTERN = Pattern.compile(YEAR_REGEX);
	private static final Version VERSION;
	private static final boolean IS_VALID;
	public static final String PARSER_NAME;

	private static boolean blocked;

	static {
		MediaInfoHelper mediaInfoHelper = getMediaInfoHelper(true);
		IS_VALID = mediaInfoHelper.isValid();
		if (IS_VALID) {
			Matcher matcher = Pattern.compile("MediaInfoLib - v(\\S+)", Pattern.CASE_INSENSITIVE).matcher(mediaInfoHelper.option("Info_Version"));
			if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
				VERSION = new Version(matcher.group(1));
			} else {
				VERSION = null;
			}
			PARSER_NAME = "MI_" + VERSION;
		} else {
			VERSION = null;
			PARSER_NAME = null;
		}
		try {
			mediaInfoHelper.close();
		} catch (Exception ex) {
			LOGGER.warn("MediaInfoHelper error on close: ", ex);
		}
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private MediaInfoParser() {
	}

	public static boolean isValid() {
		return IS_VALID && !blocked;
	}

	protected static void block() {
		blocked = true;
	}

	protected static void unblock() {
		blocked = false;
	}

	/**
	 * @return The {@code LibMediaInfo} {@link Version} or {@code null} if
	 *         unknown.
	 */
	@Nullable
	public static Version getVersion() {
		return VERSION;
	}

	private static MediaInfoHelper getMediaInfoHelper(boolean log) {
		MediaInfoHelper mediaInfoHelper = new MediaInfoHelper(log);
		if (mediaInfoHelper.isValid()) {
			//by default, MediaInfo will ignore not known option, so do not check for version.
			mediaInfoHelper.option("Internet", "No"); // avoid MediaInfoLib to try to connect to an Internet server for availability of newer software, anonymous statistics and retrieving information about a file
			mediaInfoHelper.option("Complete", "1");
			mediaInfoHelper.option("Language", "en");
			mediaInfoHelper.option("File_TestContinuousFileNames", "0");
			mediaInfoHelper.option("ReadByHuman", "0");
			mediaInfoHelper.option("Cover_Data", "base64");
			mediaInfoHelper.option("File_HighestFormat", "0");
		}
		return mediaInfoHelper;
	}

	/**
	 * Parse media via MediaInfoHelper.
	 */
	public static void parse(MediaInfo media, File file, int type) {
		media.waitMediaParsing(5);
		media.setParsing(true);
		if (file == null || media.isMediaParsed()) {
			media.setParsing(false);
			return;
		}
		MediaInfoHelper mediaInfoHelper = getMediaInfoHelper(false);
		if (!mediaInfoHelper.isValid()) {
			media.setParsing(false);
			return;
		}

		MediaInfoParseLogger parseLogger = LOGGER.isTraceEnabled() ? new MediaInfoParseLogger(mediaInfoHelper) : null;
		boolean fileOpened = mediaInfoHelper.openFile(file.getAbsolutePath()) > 0;
		if (fileOpened) {
			MediaAudio currentAudioTrack = new MediaAudio();
			MediaVideo currentVideoTrack = new MediaVideo();
			MediaSubtitle currentSubTrack;
			media.resetParser();
			media.setSize(file.length());
			String value;
			Double doubleValue;
			Long longValue;

			// set Container
			setFormat(StreamKind.GENERAL, media, currentVideoTrack, currentAudioTrack, StreamContainer.getFormat(mediaInfoHelper, 0), file);
			setFormat(StreamKind.GENERAL, media, currentVideoTrack, currentAudioTrack, StreamContainer.getCodecID(mediaInfoHelper, 0).trim(), file);
			doubleValue = StreamContainer.getDuration(mediaInfoHelper, 0);
			if (doubleValue != null) {
				//for some reason UMS store only seconds.
				media.setDuration(doubleValue / 1000);
			}
			media.setBitRate(getIntValue(StreamContainer.getOverallBitRate(mediaInfoHelper, 0), 0));
			media.setTitle(StreamContainer.getTitle(mediaInfoHelper, 0));

			if (parseLogger != null) {
				parseLogger.logGeneralColumns(file);
			}

			// set cover
			value = StreamContainer.getCoverData(mediaInfoHelper, 0);
			if (!value.isEmpty()) {
				String[] thumbs = value.split(" / ");
				try {
					thumbs[0] = thumbs[0].trim();
					DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(
						Base64.getDecoder().decode(thumbs[0]),
						640,
						480,
						ScaleType.MAX,
						ImageFormat.SOURCE,
						false
					);
					if (thumbnail != null) {
						Long thumbId = ThumbnailStore.getId(thumbnail);
						media.setThumbnailId(thumbId);
						media.setThumbnailSource(ThumbnailSource.EMBEDDED);
					}
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

			// set Chapters
			if (mediaInfoHelper.countGet(StreamKind.MENU, 0) > 0) {
				Long chaptersPosBeginLong = StreamMenu.getChaptersPosBegin(mediaInfoHelper, 0);
				Long chaptersPosEndLong = StreamMenu.getChaptersPosEnd(mediaInfoHelper, 0);
				if (chaptersPosBeginLong != null && chaptersPosEndLong != null) {
					int chaptersPosBegin = chaptersPosBeginLong.intValue();
					int chaptersPosEnd = chaptersPosEndLong.intValue();
					List<MediaChapter> chapters = new ArrayList<>();
					for (int i = chaptersPosBegin; i <= chaptersPosEnd; i++) {
						String chapterName = mediaInfoHelper.get(StreamKind.MENU, 0, i, InfoKind.NAME);
						String chapterTitle = mediaInfoHelper.get(StreamKind.MENU, 0, i, InfoKind.TEXT);
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

			// set Video
			Long videoTrackCount = StreamVideo.getStreamCount(mediaInfoHelper, 0);
			if (videoTrackCount != null && videoTrackCount > 0) {
				for (int i = 0; i < videoTrackCount; i++) {
					// check for DXSA and DXSB subtitles (subs in video format)
					if (StreamVideo.getTitle(mediaInfoHelper, i).startsWith("Subtitle")) {
						currentSubTrack = new MediaSubtitle();
						// First attempt to detect subtitle track format
						currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(StreamVideo.getFormat(mediaInfoHelper, i)));
						// Second attempt to detect subtitle track format (CodecID usually is more accurate)
						currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(StreamVideo.getCodecID(mediaInfoHelper, i),
							currentSubTrack.getType()
						));
						currentSubTrack.setId(media.getSubtitlesTracks().size());
						longValue = StreamVideo.getStreamOrder(mediaInfoHelper, i);
						if (longValue != null) {
							currentSubTrack.setStreamOrder(longValue.intValue());
						}
						currentSubTrack.setDefault("Yes".equals(StreamVideo.getDefault(mediaInfoHelper, i)));
						currentSubTrack.setForced("Yes".equals(StreamVideo.getForced(mediaInfoHelper, i)));
						addSubtitlesTrack(currentSubTrack, media);
					} else {
						currentVideoTrack = new MediaVideo();
						currentVideoTrack.setId(i);
						setFormat(StreamKind.VIDEO, media, currentVideoTrack, currentAudioTrack, StreamVideo.getFormat(mediaInfoHelper, i), file);
						setFormat(StreamKind.VIDEO, media, currentVideoTrack, currentAudioTrack, StreamVideo.getFormatVersion(mediaInfoHelper, i), file);
						setFormat(StreamKind.VIDEO, media, currentVideoTrack, currentAudioTrack, StreamVideo.getCodecID(mediaInfoHelper, i), file);
						longValue = StreamVideo.getStreamOrder(mediaInfoHelper, i);
						if (longValue != null) {
							currentVideoTrack.setStreamOrder(longValue.intValue());
						}
						currentVideoTrack.setDefault("Yes".equals(StreamVideo.getDefault(mediaInfoHelper, i)));
						currentVideoTrack.setForced("Yes".equals(StreamVideo.getForced(mediaInfoHelper, i)));
						currentVideoTrack.setWidth(StreamVideo.getWidth(mediaInfoHelper, i).intValue());
						currentVideoTrack.setHeight(StreamVideo.getHeight(mediaInfoHelper, i).intValue());
						doubleValue = StreamVideo.getDuration(mediaInfoHelper, i);
						if (doubleValue == null) {
							doubleValue = media.getDuration();
						} else {
							//for some reason UMS store only seconds.
							doubleValue = doubleValue / 1000;
						}
						currentVideoTrack.setDuration(doubleValue);
						currentVideoTrack.setBitRate(getIntValue(StreamVideo.getBitRate(mediaInfoHelper, i), 0));
						value = StreamVideo.getFormatProfile(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							String[] profile = getFormatProfile(value);
							if (profile[0] != null) {
								currentVideoTrack.setFormatProfile(profile[0]);
							}
							if (profile[1] != null) {
								currentVideoTrack.setFormatLevel(profile[1]);
							}
							if (profile[2] != null) {
								currentVideoTrack.setFormatTier(profile[2]);
							}
						}
						currentVideoTrack.setMatrixCoefficients(StreamVideo.getmatrixcoefficients(mediaInfoHelper, i));
						currentVideoTrack.setMultiViewLayout(StreamVideo.getMultiViewLayout(mediaInfoHelper, i));
						currentVideoTrack.setPixelAspectRatio(StreamVideo.getPixelAspectRatio(mediaInfoHelper, i));
						currentVideoTrack.setScanType(StreamVideo.getScanType(mediaInfoHelper, i));
						currentVideoTrack.setScanOrder(StreamVideo.getScanOrder(mediaInfoHelper, i));
						currentVideoTrack.setDisplayAspectRatio(StreamVideo.getDisplayAspectRatioString(mediaInfoHelper, i));
						currentVideoTrack.setOriginalDisplayAspectRatio(StreamVideo.getDisplayAspectRatioOriginalString(mediaInfoHelper, i));
						currentVideoTrack.setFrameRate(StreamVideo.getFrameRate(mediaInfoHelper, i));
						// for some reason, this is not store in DB.
						currentVideoTrack.setFrameRateModeOriginal(StreamVideo.getFrameRateModeOriginal(mediaInfoHelper, i));
						// for some reason, this is not store in DB.
						currentVideoTrack.setFrameRateMode(getFrameRateModeValue(StreamVideo.getFrameRateMode(mediaInfoHelper, i)));
						// for some reason, this is not store in DB.
						currentVideoTrack.setFrameRateModeRaw(StreamVideo.getFrameRateMode(mediaInfoHelper, i));
						currentVideoTrack.setReferenceFrameCount(getByteValue(StreamVideo.getFormatSettingsRefFrames(mediaInfoHelper, i), (byte) -1));
						currentVideoTrack.setTitle(StreamVideo.getTitle(mediaInfoHelper, i));
						// for some reason, this is not store in DB.
						value = StreamVideo.getFormatSettingsQPel(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							currentVideoTrack.putExtra(FormatConfiguration.MI_QPEL, value);
						}
						// for some reason, this is not store in DB.
						value = StreamVideo.getFormatSettingsGMCString(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							currentVideoTrack.putExtra(FormatConfiguration.MI_GMC, value);
						}
						// for some reason, this is not store in DB.
						value = StreamVideo.getFormatSettingsGOP(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							currentVideoTrack.putExtra(FormatConfiguration.MI_GOP, value);
						}

						currentVideoTrack.setMuxingMode(StreamVideo.getMuxingMode(mediaInfoHelper, i));
						// for some reason, this is not store in DB.
						currentVideoTrack.setEncrypted("encrypted".equals(StreamVideo.getEncryption(mediaInfoHelper, i)));

						longValue = StreamVideo.getBitDepth(mediaInfoHelper, i);
						if (longValue != null) {
							currentVideoTrack.setBitDepth(longValue.intValue());
						}

						value = StreamVideo.getHDRFormat(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							currentVideoTrack.setHDRFormat(value);
						}

						value = StreamVideo.getHDRFormatCompatibility(mediaInfoHelper, i);
						if (!value.isEmpty()) {
							currentVideoTrack.setHDRFormatCompatibility(value);
						}

						value = StreamVideo.getLanguageString3(mediaInfoHelper, i);
						if (StringUtils.isNotBlank(value)) {
							currentVideoTrack.setLang(Iso639.getISO639_2Code(value));
						}

						value = StreamVideo.getID(mediaInfoHelper, i);
						if (StringUtils.isNotBlank(value)) {
							currentVideoTrack.setOptionalId(getSpecificID(value));
						}

						addVideoTrack(currentVideoTrack, media);
						if (parseLogger != null) {
							parseLogger.logVideoTrackColumns(i);
						}
					}
				}
			}

			// set Audio
			Long audioTracks = StreamAudio.getStreamCount(mediaInfoHelper, 0);
			if (audioTracks != null && audioTracks > 0) {
				for (int i = 0; i < audioTracks; i++) {
					currentAudioTrack = new MediaAudio();
					currentAudioTrack.setId(i);
					longValue = StreamAudio.getStreamOrder(mediaInfoHelper, i);
					if (longValue != null) {
						currentAudioTrack.setStreamOrder(longValue.intValue());
					}
					currentAudioTrack.setDefault("Yes".equals(StreamAudio.getDefault(mediaInfoHelper, i)));
					currentAudioTrack.setForced("Yes".equals(StreamAudio.getForced(mediaInfoHelper, i)));
					setFormat(StreamKind.AUDIO, media, currentVideoTrack, currentAudioTrack, StreamAudio.getFormat(mediaInfoHelper, i), file);
					setFormat(StreamKind.AUDIO, media, currentVideoTrack, currentAudioTrack, StreamAudio.getFormatVersion(mediaInfoHelper, i), file);
					setFormat(StreamKind.AUDIO, media, currentVideoTrack, currentAudioTrack, StreamAudio.getFormatProfile(mediaInfoHelper, i), file);
					setFormat(StreamKind.AUDIO, media, currentVideoTrack, currentAudioTrack, StreamAudio.getCodecID(mediaInfoHelper, i), file);
					value = StreamAudio.getCodecIDDescription(mediaInfoHelper, i);
					if (StringUtils.isNotBlank(value) && value.startsWith("Windows Media Audio 10")) {
						currentAudioTrack.setCodec(FormatConfiguration.WMA10);
					}

					String languageCode = null;
					value = StreamAudio.getLanguageString3(mediaInfoHelper, i);
					if (StringUtils.isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value);
						currentAudioTrack.setLang(languageCode);
					}

					value = StreamAudio.getTitle(mediaInfoHelper, i).trim();
					currentAudioTrack.setTitle(value);
					// if language code is null try to recognize the language from Title
					if (languageCode == null && StringUtils.isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value, true);
						currentAudioTrack.setLang(languageCode);
					}
					currentAudioTrack.setNumberOfChannels(getIntValue(StreamAudio.getChannels(mediaInfoHelper, i), MediaAudio.DEFAULT_NUMBER_OF_CHANNELS));
					currentAudioTrack.setSampleRate(getIntValue(StreamAudio.getSamplingRate(mediaInfoHelper, i), MediaAudio.DEFAULT_SAMPLE_RATE));
					currentAudioTrack.setBitRate(getIntValue(StreamAudio.getBitRate(mediaInfoHelper, i), 0));
					currentAudioTrack.setVideoDelay(getIntValue(StreamAudio.getVideoDelay(mediaInfoHelper, i), 0));
					currentAudioTrack.setBitDepth(getIntValue(StreamAudio.getBitDepth(mediaInfoHelper, i), MediaAudio.DEFAULT_BIT_DEPTH));
					value = StreamAudio.getID(mediaInfoHelper, i);
					if (StringUtils.isNotBlank(value)) {
						currentAudioTrack.setOptionalId(getSpecificID(value));
					}
					addAudioTrack(currentAudioTrack, media);
					if (parseLogger != null) {
						parseLogger.logAudioTrackColumns(i);
					}
				}
			}

			// set Image
			Long imageCount = StreamImage.getStreamCount(mediaInfoHelper, 0);
			if (imageCount != null) {
				media.setImageCount(imageCount.intValue());
			}

			if (media.getImageCount() > 0 || type == Format.IMAGE) {
				boolean parseByMediainfo = false;
				// For images use our own parser instead of MediaInfoHelper which doesn't provide enough information
				try {
					MetadataExtractorParser.parse(file, media);
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
					setFormat(StreamKind.IMAGE, media, currentVideoTrack, currentAudioTrack, StreamImage.getFormat(mediaInfoHelper, 0), file);
				}

				if (parseLogger != null) {
					parseLogger.logImageColumns(0);
				}
			}

			// set Subs in text format
			Long subTrackCount = StreamSubtitle.getStreamCount(mediaInfoHelper, 0);
			if (subTrackCount != null && subTrackCount > 0) {
				for (int i = 0; i < subTrackCount; i++) {
					currentSubTrack = new MediaSubtitle();
					currentSubTrack.setType(SubtitleType.valueOfMediaInfoValue(StreamSubtitle.getCodecID(mediaInfoHelper, i),
						SubtitleType.valueOfMediaInfoValue(StreamSubtitle.getFormat(mediaInfoHelper, i))
					));
					currentSubTrack.setId(media.getSubtitlesTracks().size());
					String languageCode = null;
					value = StreamSubtitle.getLanguageString3(mediaInfoHelper, i);
					if (StringUtils.isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value);
						currentSubTrack.setLang(languageCode);
					}

					value = StreamSubtitle.getTitle(mediaInfoHelper, i).trim();
					currentSubTrack.setTitle(value);
					// if language code is null try to recognize the language from Title
					if (languageCode == null && StringUtils.isNotBlank(value)) {
						languageCode = Iso639.getISO639_2Code(value, true);
						currentSubTrack.setLang(languageCode);
					}

					// Special check for OGM: MediaInfoHelper reports specific Audio/Subs IDs (0xn) while mencoder/FFmpeg does not
					value = StreamSubtitle.getID(mediaInfoHelper, i);
					if (StringUtils.isNotBlank(value)) {
						currentSubTrack.setOptionalId(getSpecificID(value));
					}
					currentSubTrack.setDefault("Yes".equals(StreamSubtitle.getDefault(mediaInfoHelper, i)));
					currentSubTrack.setForced("Yes".equals(StreamSubtitle.getForced(mediaInfoHelper, i)));

					addSubtitlesTrack(currentSubTrack, media);
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
				for (MediaAudio audioTrack : media.getAudioTracks()) {
					if (
						audioTrack.getCodec() != null &&
						!audioTrack.getCodec().equals(FormatConfiguration.WMA) &&
						!audioTrack.getCodec().equals(FormatConfiguration.WMAPRO) &&
						!audioTrack.getCodec().equals(FormatConfiguration.WMALOSSLESS) &&
						!audioTrack.getCodec().equals(FormatConfiguration.WMAVOICE) &&
						!audioTrack.getCodec().equals(FormatConfiguration.WMA10) &&
						!audioTrack.getCodec().equals(FormatConfiguration.MP3) // up to 128 kbit/s only (WMVSPML_MP3 profile)
					) {
						media.setContainer(FormatConfiguration.ASF);
						break;
					}
				}
			}
			if (FormatConfiguration.WMV.equals(media.getContainer())) {
				for (MediaVideo videoTrack : media.getVideoTracks()) {
					if (
						videoTrack.getCodec() != null &&
						!videoTrack.getCodec().equals(FormatConfiguration.WMV) &&
						!videoTrack.getCodec().equals(FormatConfiguration.VC1)
					) {
						media.setContainer(FormatConfiguration.ASF);
						break;
					}
				}
			}

			if (media.hasVideoTrack() && !media.getDefaultVideoTrack().is3d()) {
				parseFilenameForMultiViewLayout(file, media);
			}

			if (media.isAudio()) {
				media.setAudioMetadata(parseFileForAudioMetadata(mediaInfoHelper, file, media));
			}

			Parser.postParse(media, type);
			if (parseLogger != null) {
				LOGGER.trace("{}", parseLogger);
			}

			mediaInfoHelper.closeFile();
			if (media.getContainer() == null) {
				media.setContainer(MediaLang.UND);
			}
			if (!media.isImage() || !media.isMediaParsed()) {
				media.setMediaParser(PARSER_NAME);
			}
		}
		try {
			mediaInfoHelper.close();
		} catch (Exception ex) {
			LOGGER.warn("MediaInfoHelper on close: ", ex);
		}
		media.setParsing(false);
	}

	public static void addVideoTrack(MediaVideo currentVideoTrack, MediaInfo media) {
		if (StringUtils.isBlank(currentVideoTrack.getLang())) {
			currentVideoTrack.setLang(MediaLang.UND);
		}

		if (StringUtils.isBlank(currentVideoTrack.getCodec())) {
			currentVideoTrack.setCodec(MediaLang.UND);
		}

		media.addVideoTrack(currentVideoTrack);
	}

	public static void addAudioTrack(MediaAudio currentAudioTrack, MediaInfo media) {
		if (StringUtils.isBlank(currentAudioTrack.getLang())) {
			currentAudioTrack.setLang(MediaLang.UND);
		}

		if (StringUtils.isBlank(currentAudioTrack.getCodec())) {
			currentAudioTrack.setCodec(MediaLang.UND);
		}

		media.addAudioTrack(currentAudioTrack);
	}

	public static void addSubtitlesTrack(MediaSubtitle currentSubTrack, MediaInfo media) {
		if (currentSubTrack.getType() == SubtitleType.UNSUPPORTED) {
			return;
		}

		if (StringUtils.isBlank(currentSubTrack.getLang())) {
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
	 * @todo Split the values by streamType to make the logic more clear
	 *       with less negative statements.
	 */
	protected static void setFormat(StreamKind streamType, MediaInfo media, MediaVideo video, MediaAudio audio, String value, File file) {
		if (StringUtils.isBlank(value) || streamType == null) {
			return;
		}

		value = value.toLowerCase(Locale.ROOT);
		String format = null;

		if (StringUtils.isBlank(value)) {
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
			!value.contains("dvhe") &&
			!value.contains("dvh1")
		) {
			format = FormatConfiguration.DV;
		} else if (value.contains("mpeg video")) {
			format = FormatConfiguration.MPEG2;
		} else if (value.startsWith("version 1")) {
			if (video.getCodec() != null && video.getCodec().equals(FormatConfiguration.MPEG2) && audio.getCodec() == null) {
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
			if (audio.getCodec() != null && audio.getCodec().equals(FormatConfiguration.MPA)) {
				format = FormatConfiguration.MP3;
				// special case:
				if (media.getContainer() != null && media.getContainer().equals(FormatConfiguration.MPA)) {
					media.setContainer(FormatConfiguration.MP3);
				}
			}
		} else if (
			value.equals("layer 2") &&
			audio.getCodec() != null &&
			media.getContainer() != null &&
			audio.getCodec().equals(FormatConfiguration.MPA) &&
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
			if (audio.getCodec() != null && audio.getCodec().equals(FormatConfiguration.DTS)) {
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
		} else if (value.equals("ac-4")) {
			format = FormatConfiguration.AC4;
		} else if (value.startsWith("cook")) {
			format = FormatConfiguration.COOK;
		} else if (value.startsWith("qdesign")) {
			format = FormatConfiguration.QDESIGN;
		} else if (value.equals("realaudio lossless")) {
			format = FormatConfiguration.RALF;
		} else if (value.contains("e-ac-3")) {
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
			format = switch (value) {
				case "a_aac/mpeg2/main" -> FormatConfiguration.AAC_MAIN;
				case "a_aac/mpeg2/lc", "a_aac-2" -> FormatConfiguration.AAC_LC;
				case "a_aac/mpeg2/lc/sbr" -> FormatConfiguration.HE_AAC;
				case "a_aac/mpeg2/ssr" -> FormatConfiguration.AAC_SSR;
				case "a_aac/mpeg4/main" -> FormatConfiguration.AAC_MAIN;
				case "a_aac/mpeg4/lc" -> FormatConfiguration.AAC_LC;
				case "a_aac/mpeg4/lc/sbr" -> FormatConfiguration.HE_AAC;
				case "a_aac/mpeg4/lc/sbr/ps" -> FormatConfiguration.HE_AAC;
				case "a_aac/mpeg4/ssr" -> FormatConfiguration.AAC_SSR;
				case "a_aac/mpeg4/ltp" -> FormatConfiguration.AAC_LTP;
				// HE-AACv2
				default -> FormatConfiguration.AAC_MAIN;
			};
		} else if (
			value.equals("er bsac") ||
			value.equals("mp4a-40-22")
		) {
			format = FormatConfiguration.ER_BSAC;
		} else if (value.startsWith("adpcm")) {
			format = FormatConfiguration.ADPCM;
		} else if (value.equals("pcm") || (value.equals("1") && (audio.getCodec() == null || !audio.getCodec().equals(FormatConfiguration.DTS)))) {
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
				audio.getCodec() == null ||
				!audio.getCodec().equals(FormatConfiguration.DTSHD)
			)
		) {
			format = FormatConfiguration.DTS;
		} else if (value.equals("mpeg audio")) {
			format = FormatConfiguration.MPA;
		} else if (value.equals("wma")) {
			format = FormatConfiguration.WMA;
			if (video.getCodec() == null) {
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
		}

		if (format != null) {
			switch (streamType) {
				case GENERAL -> media.setContainer(format);
				case VIDEO -> video.setCodec(format);
				case AUDIO -> audio.setCodec(format);
				default -> {
					//should not
				}
			}
			// format not found so set container type based on the file extension. It will be overwritten when the correct type will be found
		} else if (streamType == StreamKind.GENERAL && media.getContainer() == null && file.getAbsolutePath() != null) {
			String ext = FileUtil.getExtension(file.getAbsolutePath());
			if (ext != null) {
				media.setContainer(ext.toLowerCase(Locale.ROOT));
			}
		}
	}

	public static int getPixelValue(String value) {
		if (StringUtils.isBlank(value)) {
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
		if (StringUtils.isBlank(value)) {
			return -1;
		}

		try {
			// Values like "16 frame3"
			return Byte.parseByte(StringUtils.substringBefore(value, " "));
		} catch (NumberFormatException ex) {
			// Not parsed
			LOGGER.warn("Could not parse ReferenceFrameCount value {}.", value);
			LOGGER.warn("Exception: ", ex);
			return -1;
		}
	}

	/**
	 * @param value {@code Format_Profile} value to parse.
	 * @return Array of string with Format Profile, Format Level and Format Tier.
	 */
	public static String[] getFormatProfile(String value) {
		String[] result = new String[3];
		String profile = StringUtils.substringBefore(value, "@");
		if (StringUtils.isNotBlank(profile)) {
			result[0] = StringUtils.lowerCase(profile);
		}
		//do the same way as MediaInfoLib do.
		String profileMore = StringUtils.substringAfter(value, "@");
		if (StringUtils.isNotBlank(profileMore)) {
			if (profileMore.length() > 1 && profileMore.charAt(0) == 'L' && profileMore.charAt(1) >= '0' && profileMore.charAt(1) <= '9') {
				profileMore = profileMore.substring(1);
			}
			int separatorPos = profileMore.indexOf('@');
			if (separatorPos != -1) {
				result[1] = StringUtils.lowerCase(profileMore.substring(0, separatorPos));
				result[2] = StringUtils.lowerCase(profileMore.substring(separatorPos + 1));
			} else {
				result[1] = StringUtils.lowerCase(profileMore);
			}
		}
		return result;
	}

	public static int getVideoBitrate(String value) {
		if (StringUtils.isBlank(value)) {
			return 0;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			LOGGER.trace("Could not parse video bitrate \"{}\": ", value, e.getMessage());
			return 0;
		}
	}

	public static Long getSpecificID(String value) {
		// If ID is given as 'streamID-substreamID' use the second (which is hopefully unique).
		// For example in vob audio ID can be '189 (0xBD)-32 (0x80)' and text ID '189 (0xBD)-128 (0x20)'
		int end = value.lastIndexOf("(0x");
		if (end > -1) {
			int start = value.lastIndexOf('-') + 1;
			value = value.substring(start > end ? 0 : start, end);
		} else if (value.lastIndexOf('-') > -1) { // value could be '189-128'
			value = value.substring(value.lastIndexOf('-') + 1);
		}

		value = value.trim();
		return Long.valueOf(value);
	}

	public static String getFrameRateModeValue(String value) {
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
	public static Double parseDuration(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String[] parts = value.split("\\s*/\\s*");
		value = parts[parts.length - 1];
		int separator = value.indexOf(".");
		if (separator > 0) {
			value = value.substring(0, separator);
		}
		try {
			double longValue = Long.parseLong(value);
			return longValue / 1000.0;
		} catch (NumberFormatException e) {
			LOGGER.warn("Could not parse duration from \"{}\"", value);
			return null;
		}
	}

	private static Double getDoubleValue(String value) {
		if (StringUtils.isEmpty(value)) {
			return null;
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException ex) {
			LOGGER.warn("NumberFormatException during parsing double from value {}", value);
			return null;
		}
	}

	private static int getIntValue(Double value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value.intValue();
	}

	private static int getIntValue(Long value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value.intValue();
	}

	private static byte getByteValue(Long value, byte defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value.byteValue();
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
	private static void parseFilenameForMultiViewLayout(File file, MediaInfo media) {
		String upperCaseFileName = file.getName().toUpperCase();
		if (upperCaseFileName.startsWith("3DSBS")) {
			LOGGER.debug("3D format SBS detected for " + file.getName());
			media.getDefaultVideoTrack().setMultiViewLayout(file.getName().substring(2, 7));
		} else if (upperCaseFileName.startsWith("3DOU")) {
			LOGGER.debug("3D format OU detected for " + file.getName());
			media.getDefaultVideoTrack().setMultiViewLayout(file.getName().substring(2, 6));
		} else if (upperCaseFileName.startsWith("3DA")) {
			LOGGER.debug("3D format Anaglyph detected for " + file.getName());
			media.getDefaultVideoTrack().setMultiViewLayout(file.getName().substring(2, 6));
		} else if (upperCaseFileName.matches(".*[\\s\\.](H-|H|HALF-|HALF.)SBS[\\s\\.].*")) {
			LOGGER.debug("3D format HSBS detected for " + file.getName());
			media.getDefaultVideoTrack().setMultiViewLayout("half side by side (left eye first)");
		} else if (upperCaseFileName.matches(".*[\\s\\.](H-|H|HALF-|HALF.)(OU|TB)[\\s\\.].*")) {
			LOGGER.debug("3D format HOU detected for " + file.getName());
			media.getDefaultVideoTrack().setMultiViewLayout("half top-bottom (left eye first)");
		} else if (upperCaseFileName.matches(".*[\\s\\.]SBS[\\s\\.].*")) {
			if (media.getWidth() > 1920) {
				LOGGER.debug("3D format SBS detected for " + file.getName());
				media.getDefaultVideoTrack().setMultiViewLayout("side by side (left eye first)");
			} else {
				LOGGER.debug("3D format HSBS detected based on width for " + file.getName());
				media.getDefaultVideoTrack().setMultiViewLayout("half side by side (left eye first)");
			}
		} else if (upperCaseFileName.matches(".*[\\s\\.](OU|TB)[\\s\\.].*")) {
			if (media.getHeight() > 1080) {
				LOGGER.debug("3D format OU detected for " + file.getName());
				media.getDefaultVideoTrack().setMultiViewLayout("top-bottom (left eye first)");
			} else {
				LOGGER.debug("3D format HOU detected based on height for " + file.getName());
				media.getDefaultVideoTrack().setMultiViewLayout("half top-bottom (left eye first)");
			}
		}
	}

	private static MediaAudioMetadata parseFileForAudioMetadata(MediaInfoHelper mediaInfoHelper, File file, MediaInfo media) {
		MediaAudioMetadata audioMetadata = new MediaAudioMetadata();
		audioMetadata.setSongname(StreamContainer.getTrack(mediaInfoHelper, 0));
		audioMetadata.setAlbum(StreamContainer.getAlbum(mediaInfoHelper, 0));
		String albumArtists = mediaInfoHelper.get(StreamKind.GENERAL, 0, "ALBUM_ARTISTS");
		if (StringUtils.isAllBlank(albumArtists)) {
			albumArtists = StreamContainer.getAlbumPerformer(mediaInfoHelper, 0);
		}
		audioMetadata.setAlbumArtist(albumArtists);
		String artists = mediaInfoHelper.get(StreamKind.GENERAL, 0, "ARTISTS");
		if (StringUtils.isAllBlank(artists)) {
			artists = StreamContainer.getPerformer(mediaInfoHelper, 0);
		}
		audioMetadata.setArtist(artists);
		audioMetadata.setGenre(StreamContainer.getGenre(mediaInfoHelper, 0));
		audioMetadata.setComposer(StreamContainer.getComposer(mediaInfoHelper, 0));
		audioMetadata.setConductor(StreamContainer.getConductor(mediaInfoHelper, 0));
		Long longValue = StreamContainer.getTrackPosition(mediaInfoHelper, 0);
		if (longValue != null) {
			audioMetadata.setTrack(longValue.intValue());
		}

		String value = StreamContainer.getPart(mediaInfoHelper, 0);
		if (!value.isEmpty()) {
			try {
				audioMetadata.setDisc(Integer.parseInt(value));
			} catch (NumberFormatException nfe) {
				LOGGER.debug("Could not parse disc \"" + value + "\"");
			}
		}

		// Try to parse the year from the stored date
		String recordedDate = StreamContainer.getRecordedDate(mediaInfoHelper, 0);
		Matcher matcher = YEAR_PATTERN.matcher(recordedDate);
		if (matcher.matches()) {
			try {
				audioMetadata.setYear(Integer.parseInt(matcher.group(1)));
			} catch (NumberFormatException nfe) {
				LOGGER.debug("Could not parse year from recorded date \"" + recordedDate + "\"");
			}
		}

		if (!media.hasVideoTrack()) {
			JaudiotaggerParser.parse(file, audioMetadata);
		}
		return audioMetadata;
	}

}
