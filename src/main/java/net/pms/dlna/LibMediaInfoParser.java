package net.pms.dlna;

import java.io.File;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.MediaInfo.InfoType;
import net.pms.dlna.MediaInfo.StreamType;
import net.pms.formats.v2.SubtitleType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibMediaInfoParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibMediaInfoParser.class);

	// Regular expression to parse a 4 digit year number from a string
	private static final String YEAR_REGEX = ".*([\\d]{4}).*";

	// Pattern to parse the year from a string
	private static final Pattern yearPattern = Pattern.compile(YEAR_REGEX);

	private static MediaInfo MI;
	private static Base64 base64;

	static {
		MI = new MediaInfo();

		if (MI.isValid()) {
			MI.Option("Complete", "1");
			MI.Option("Language", "raw");
		}

		base64 = new Base64();
	}

	public static boolean isValid() {
		return MI.isValid();
	}

	public static void close() {
		try {
			MI.finalize();
		} catch (Throwable e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	public synchronized static void parse(DLNAMediaInfo media, InputFile inputFile, int type) {
		File file = inputFile.getFile();
		if (!media.isMediaparsed() && file != null && MI.isValid() && MI.Open(file.getAbsolutePath()) > 0) {
			try {
				DLNAMediaAudio currentAudioTrack = new DLNAMediaAudio();
				DLNAMediaSubtitle currentSubTrack = new DLNAMediaSubtitle();
				media.setSize(file.length());
				String value;

				// set General
				getFormat(StreamType.General, media, currentAudioTrack, MI.Get(StreamType.General, 0, "Format").toLowerCase(), file);
				media.setDuration(getDuration(MI.Get(StreamType.General, 0, "Duration/String1")));
				media.setBitrate(getBitrate(MI.Get(StreamType.General, 0, "OverallBitRate")));
				value = MI.Get(StreamType.General, 0, "Cover_Data");
				if (isNotBlank(value)) {
					media.setThumb(getCover(value));
				}

				// set Video
				int videos = MI.Count_Get(StreamType.Video);
				if (videos > 0) {
					for (int i = 0; i < videos; i++) {
						// check for DXSA and DXSB subtitles (subs in video format)
						if (MI.Get(StreamType.Video, i, "Title").startsWith("Subtitle")) {
							currentSubTrack = new DLNAMediaSubtitle();
							// First attempt to detect subtitle track format
							currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(StreamType.Video, i, "Format")));
							// Second attempt to detect subtitle track format (CodecID usually is more accurate)
							currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(StreamType.Video, i, "CodecID")));
							currentSubTrack.setId(media.getSubtitleTracksList().size());
							addSub(currentSubTrack, media);
						} else {
							getFormat(StreamType.Video, media, currentAudioTrack, MI.Get(StreamType.Video, i, "CodecID").toLowerCase(), file);
							media.setWidth(getPixelValue(MI.Get(StreamType.Video, i, "Width")));
							media.setHeight(getPixelValue(MI.Get(StreamType.Video, i, "Height")));
							media.setFrameRate(getFPSValue(MI.Get(StreamType.Video, i, "FrameRate")));
							media.setStereoscopy(MI.Get(StreamType.Video, i, "MultiView_Layout"));
							media.setAspectRatioContainer(MI.Get(StreamType.Video, i, "DisplayAspectRatio/String"));
							media.setAspectRatioVideoTrack(MI.Get(StreamType.Video, i, "DisplayAspectRatio_Original/Stri"));
							media.setFrameRate(getFPSValue(MI.Get(StreamType.Video, i, "FrameRate")));
							media.setFrameRateMode(getFrameRateModeValue(MI.Get(StreamType.Video, i, "FrameRateMode")));
							media.setReferenceFrameCount(getReferenceFrameCount(MI.Get(StreamType.Video, i, "Format_Settings_RefFrames/String")));
							media.putExtra(FormatConfiguration.MI_QPEL, MI.Get(StreamType.Video, i, "Format_Settings_QPel", InfoType.Text, InfoType.Name));
							media.putExtra(FormatConfiguration.MI_GMC, MI.Get(StreamType.Video, i, "Format_Settings_GMC", InfoType.Text, InfoType.Name));
							media.putExtra(FormatConfiguration.MI_GOP, MI.Get(StreamType.Video, i, "Format_Settings_GMC", InfoType.Text, InfoType.Name));
							media.setMuxingMode(MI.Get(StreamType.Video, i, "MuxingMode", InfoType.Text, InfoType.Name));
							if (!media.isEncrypted()) {
								media.setEncrypted("encrypted".equals(MI.Get(StreamType.Video, i, "Encryption")));
							}
						}
					}
				}

				// set Audio
				int audioTracks = MI.Count_Get(StreamType.Audio);
				if (audioTracks > 0) {
					for (int i = 0; i < audioTracks; i++) {
						currentAudioTrack = new DLNAMediaAudio();
						getFormat(StreamType.Audio, media, currentAudioTrack, MI.Get(StreamType.Audio, i, "Format").toLowerCase(), file);
						getFormat(StreamType.Audio, media, currentAudioTrack, MI.Get(StreamType.Audio, i, "Format_Version").toLowerCase(), file);
						getFormat(StreamType.Audio, media, currentAudioTrack, MI.Get(StreamType.Audio, i, "Format_Profile").toLowerCase(), file);
						getFormat(StreamType.Audio, media, currentAudioTrack, MI.Get(StreamType.Audio, i, "CodecID").toLowerCase(), file);
						currentAudioTrack.setLang(getLang(MI.Get(StreamType.Audio, i, "Language/String")));
						currentAudioTrack.setFlavor(getFlavor(MI.Get(StreamType.Audio, i, "Title")));
						currentAudioTrack.getAudioProperties().setNumberOfChannels(MI.Get(StreamType.Audio, i, "Channel(s)"));
						currentAudioTrack.setSampleFrequency(getSampleFrequency(MI.Get(StreamType.Audio, i, "SamplingRate")));
						currentAudioTrack.setBitRate(getBitrate(MI.Get(StreamType.Audio, i, "BitRate")));
						currentAudioTrack.setSongname(MI.Get(StreamType.General, 0, "Track"));
						currentAudioTrack.setAlbum(MI.Get(StreamType.General, 0, "Album"));
						currentAudioTrack.setArtist(MI.Get(StreamType.General, 0, "Performer"));
						currentAudioTrack.setGenre(MI.Get(StreamType.General, 0, "Genre"));
						// Try to parse the year from the stored date
						String recordedDate = MI.Get(StreamType.General, 0, "Recorded_Date");
						Matcher matcher = yearPattern.matcher(recordedDate);
						if (matcher.matches()) {
							try {
								currentAudioTrack.setYear(Integer.parseInt(matcher.group(1)));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse year from recorded date \"" + recordedDate + "\"");
							}
						}

						// Special check for OGM: MediaInfo reports specific Audio/Subs IDs (0xn) while mencoder does not
						value = MI.Get(StreamType.Audio, i, "ID/String");
						if (isNotBlank(value)) {
							if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
								currentAudioTrack.setId(getSpecificID(value));
							} else {
								currentAudioTrack.setId(media.getAudioTracksList().size());
							}
						}

						value = MI.Get(StreamType.General, i, "Track/Position");
						if (isNotBlank(value)) {
							try {
								currentAudioTrack.setTrack(Integer.parseInt(value));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse track \"" + value + "\"");
							}
						}

						value = MI.Get(StreamType.Audio, i, "BitDepth");
						if (isNotBlank(value)) {
							try {
								currentAudioTrack.setBitsperSample(Integer.parseInt(value));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse bits per sample \"" + value + "\"");
							}
						}

						addAudio(currentAudioTrack, media);
					}
				}

				// set Image
				if (MI.Count_Get(StreamType.Image) > 0) {
					getFormat(StreamType.Image, media, currentAudioTrack, MI.Get(StreamType.Image, 0, "Format").toLowerCase(), file);
					media.setWidth(getPixelValue(MI.Get(StreamType.Image, 0, "Width")));
					media.setHeight(getPixelValue(MI.Get(StreamType.Image, 0, "Height")));
				}

				// set Subs in text format
				int subTracks = MI.Count_Get(StreamType.Text);
				if (subTracks > 0) {
					for (int i = 0; i < subTracks; i++) {
						currentSubTrack = new DLNAMediaSubtitle();
						currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(StreamType.Text, i, "Format")));
						currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(StreamType.Text, i, "CodecID")));
						currentSubTrack.setLang(getLang(MI.Get(StreamType.Text, i, "Language/String")));
						currentSubTrack.setFlavor(getFlavor(MI.Get(StreamType.Text, i, "Title")));
						// Special check for OGM: MediaInfo reports specific Audio/Subs IDs (0xn) while mencoder does not
						value = MI.Get(StreamType.Text, i, "ID/String");
						if (isNotBlank(value)) {
							if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
								currentSubTrack.setId(getSpecificID(value));
							} else {
								currentSubTrack.setId(media.getSubtitleTracksList().size());
							}
						}

						addSub(currentSubTrack, media);
					}
				}

				/**
				 * Native M4A/AAC streaming bug: http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=16691
				 * Some M4A files have generic codec id "mp42" instead of "M4A". For example:
				 *
				 * General
				 * Format                                   : MPEG-4
				 * Format profile                           : Apple audio with iTunes info
				 * Codec ID                                 : M4A
				 *
				 * vs
				 *
				 * General
				 * Format                                   : MPEG-4
				 * Format profile                           : Base Media / Version 2
				 * Codec ID                                 : mp42
				 *
				 * As a workaround, set container type to AAC for MP4 files that have a single AAC audio track and no video.
				 */
				if (
					FormatConfiguration.MP4.equals(media.getContainer()) &&
					isBlank(media.getCodecV()) &&
					media.getAudioTracksList() != null &&
					media.getAudioTracksList().size() == 1 &&
					FormatConfiguration.AAC.equals(media.getAudioTracksList().get(0).getCodecA())
				) {
					media.setContainer(FormatConfiguration.AAC);
				}

				/**
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

				media.finalize(type, inputFile);
			} catch (Exception e) {
				LOGGER.error("Error in MediaInfo parsing:", e);
			} finally {
				MI.Close();
				if (media.getContainer() == null) {
					media.setContainer(DLNAMediaLang.UND);
				}

				if (media.getCodecV() == null) {
					media.setCodecV(DLNAMediaLang.UND);
				}

				media.setMediaparsed(true);
			}
		}
	}

	public static void addAudio(DLNAMediaAudio currentAudioTrack, DLNAMediaInfo media) {
		if (isBlank(currentAudioTrack.getLang())) {
			currentAudioTrack.setLang(DLNAMediaLang.UND);
		}

		if (isBlank(currentAudioTrack.getCodecA())) {
			currentAudioTrack.setCodecA(DLNAMediaLang.UND);
		}

		media.getAudioTracksList().add(currentAudioTrack);
	}

	public static void addSub(DLNAMediaSubtitle currentSubTrack, DLNAMediaInfo media) {
		if (currentSubTrack.getType() == SubtitleType.UNSUPPORTED) {
			return;
		}

		if (isBlank(currentSubTrack.getLang())) {
			currentSubTrack.setLang(DLNAMediaLang.UND);
		}

		media.getSubtitleTracksList().add(currentSubTrack);
	}

	@Deprecated
	// FIXME this is obsolete (replaced by the private method below) and isn't called from anywhere outside this class
	public static void getFormat(MediaInfo.StreamType streamType, DLNAMediaInfo media, DLNAMediaAudio audio, String value) {
		getFormat(streamType, media, audio, value, null);
	}

	private static void getFormat(MediaInfo.StreamType streamType, DLNAMediaInfo media, DLNAMediaAudio audio, String value, File file) {
		String format = null;

		if (value.startsWith("matroska")) {
			format = FormatConfiguration.MATROSKA;
		} else if (value.equals("avi") || value.equals("opendml")) {
			format = FormatConfiguration.AVI;
		} else if (value.startsWith("flash")) {
			format = FormatConfiguration.FLV;
		} else if (value.toLowerCase().equals("webm")) {
			format = FormatConfiguration.WEBM;
		} else if (value.equals("qt") || value.equals("quicktime")) {
			format = FormatConfiguration.MOV;
		} else if (value.equals("isom") || value.startsWith("mp4") || value.equals("20") || value.equals("m4v") || value.startsWith("mpeg-4")) {
			format = FormatConfiguration.MP4;
		} else if (value.contains("mpeg-ps")) {
			format = FormatConfiguration.MPEGPS;
		} else if (value.contains("mpeg-ts") || value.equals("bdav")) {
			format = FormatConfiguration.MPEGTS;
		} else if (value.contains("aiff")) {
			format = FormatConfiguration.AIFF;
		} else if (value.contains("ogg")) {
			format = FormatConfiguration.OGG;
		} else if (value.contains("realmedia") || value.startsWith("rv") || value.startsWith("cook")) {
			format = FormatConfiguration.RM;
		} else if (value.contains("windows media") || value.equals("wmv1") || value.equals("wmv2") || value.equals("wmv7") || value.equals("wmv8")) {
			format = FormatConfiguration.WMV;
		} else if (value.contains("mjpg") || value.contains("m-jpeg")) {
			format = FormatConfiguration.MJPEG;
		} else if (value.startsWith("avc") || value.startsWith("h264")) {
			format = FormatConfiguration.H264;
		} else if (value.contains("xvid")) {
			format = FormatConfiguration.MP4;
		} else if (value.contains("mjpg") || value.contains("m-jpeg")) {
			format = FormatConfiguration.MJPEG;
		} else if (value.contains("div") || value.contains("dx")) {
			format = FormatConfiguration.DIVX;
		} else if (value.matches("(?i)(dv)|(cdv.?)|(dc25)|(dcap)|(dvc.?)|(dvs.?)|(dvrs)|(dv25)|(dv50)|(dvan)|(dvh.?)|(dvis)|(dvl.?)|(dvnm)|(dvp.?)|(mdvf)|(pdvc)|(r411)|(r420)|(sdcc)|(sl25)|(sl50)|(sldv)")) {
			format = FormatConfiguration.DV;
		} else if (value.contains("mpeg video")) {
			format = FormatConfiguration.MPEG2;
		} else if (value.equals("vc-1") || value.equals("vc1") || value.equals("wvc1") || value.equals("wmv3") || value.equals("wmv9") || value.equals("wmva")) {
			format = FormatConfiguration.VC1;
		} else if (value.startsWith("version 1")) {
			if (media.getCodecV() != null && media.getCodecV().equals(FormatConfiguration.MPEG2) && audio.getCodecA() == null) {
				format = FormatConfiguration.MPEG1;
			}
		} else if (value.equals("layer 3")) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.MPA)) {
				format = FormatConfiguration.MP3;
				// special case:
				if (media.getContainer() != null && media.getContainer().equals(FormatConfiguration.MPA)) {
					media.setContainer(FormatConfiguration.MP3);
				}
			}
		} else if (value.equals("ma")) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.DTS)) {
				format = FormatConfiguration.DTSHD;
			}
		} else if (value.equals("vorbis") || value.equals("a_vorbis")) {
			format = FormatConfiguration.OGG;
		} else if (value.equals("ac-3") || value.equals("a_ac3") || value.equals("2000")) {
			format = FormatConfiguration.AC3;
		} else if (value.equals("e-ac-3")) {
			format = FormatConfiguration.EAC3;
		} else if (value.contains("truehd")) {
			format = FormatConfiguration.TRUEHD;
		} else if (value.equals("55") || value.equals("a_mpeg/l3")) {
			format = FormatConfiguration.MP3;
		} else if (value.equals("lc")) {
			format = FormatConfiguration.AAC;
		} else if (value.contains("he-aac")) {
			format = FormatConfiguration.AAC_HE;
		} else if (value.equals("pcm") || (value.equals("1") && (audio.getCodecA() == null || !audio.getCodecA().equals(FormatConfiguration.DTS)))) {
			format = FormatConfiguration.LPCM;
		} else if (value.equals("alac")) {
			format = FormatConfiguration.ALAC;
		} else if (value.equals("wave")) {
			format = FormatConfiguration.WAV;
		} else if (value.equals("shorten")) {
			format = FormatConfiguration.SHORTEN;
		} else if (value.equals("dts") || value.equals("a_dts") || value.equals("8")) {
			format = FormatConfiguration.DTS;
		} else if (value.equals("mpeg audio")) {
			format = FormatConfiguration.MPA;
		} else if (value.equals("161") || value.startsWith("wma")) {
			format = FormatConfiguration.WMA;
			if (media.getCodecV() == null) {
				media.setContainer(FormatConfiguration.WMA);
			}
		} else if (value.equals("flac")) {
			format = FormatConfiguration.FLAC;
		} else if (value.equals("monkey's audio")) {
			format = FormatConfiguration.APE;
		} else if (value.contains("musepack")) {
			format = FormatConfiguration.MPC;
		} else if (value.contains("wavpack")) {
			format = FormatConfiguration.WAVPACK;
		} else if (value.contains("mlp")) {
			format = FormatConfiguration.MLP;
		} else if (value.contains("atrac3")) {
			format = FormatConfiguration.ATRAC;
			if (media.getCodecV() == null) {
				media.setContainer(FormatConfiguration.ATRAC);
			}
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
		} else if (StringUtils.containsIgnoreCase(value, "@l") && streamType == MediaInfo.StreamType.Video) {
			media.setAvcLevel(getAvcLevel(value));
			media.setH264Profile(getAvcProfile(value));
		}

		if (format != null) {
			if (streamType == MediaInfo.StreamType.General) {
				media.setContainer(format);
			} else if (streamType == MediaInfo.StreamType.Video) {
				media.setCodecV(format);
			} else if (streamType == MediaInfo.StreamType.Audio) {
				audio.setCodecA(format);
			}
		}
	}

	public static int getPixelValue(String value) {
		if (value.contains("pixel")) {
			value = value.substring(0, value.indexOf("pixel"));
		}

		value = value.trim();

		// Value can look like "512 / 512" at this point
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf('/')).trim();
		}

		int pixels = Integer.parseInt(value);
		return pixels;
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
			return Byte.parseByte(StringUtils.substringBefore(value, " "));
		} catch (NumberFormatException ex) {
			// Not parsed
			LOGGER.warn("Could not parse ReferenceFrameCount value {}." , value);
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
		} else {
			LOGGER.warn("Could not parse AvcLevel value {}." , value);
			return null;
		}
	}

	public static String getAvcProfile(String value) {
		String profile = StringUtils.substringBefore(lowerCase(value), "@l");
		if (isNotBlank(profile)) {
			return profile;
		} else {
			LOGGER.warn("Could not parse AvcProfile value {}." , value);
			return null;
		}
	}

	public static int getBitrate(String value) {
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf('/')).trim();
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			LOGGER.trace("Could not parse bitrate from: " + value);
			LOGGER.trace("The full error was: " + e);

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
		int id = Integer.parseInt(value);
		return id;
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

	public static String getFlavor(String value) {
		value = value.trim();
		return value;
	}

	private static double getDuration(String value) {
		int h = 0, m = 0, s = 0;
		StringTokenizer st = new StringTokenizer(value, " ");

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			int hl = token.indexOf('h');

			if (hl > -1) {
				h = Integer.parseInt(token.substring(0, hl).trim());
			}

			int mnl = token.indexOf("mn");

			if (mnl > -1) {
				m = Integer.parseInt(token.substring(0, mnl).trim());
			}

			int msl = token.indexOf("ms");

			if (msl == -1) {
				// Only check if ms was not found
				int sl = token.indexOf('s');

				if (sl > -1) {
					s = Integer.parseInt(token.substring(0, sl).trim());
				}
			}
		}

		return (h * 3600) + (m * 60) + s;
	}

	public static byte[] getCover(String based64Value) {
		try {
			if (base64 != null) {
				return base64.decode(based64Value.getBytes());
			}
		} catch (Exception e) {
			LOGGER.error("Error in decoding thumbnail data", e);
		}

		return null;
	}
}
