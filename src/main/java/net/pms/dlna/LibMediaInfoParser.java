package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.MediaInfo.StreamType;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.FileUtil;
import net.pms.util.ImagesUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.imaging.ImageReadException;
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

	static {
		MI = new MediaInfo();

		if (MI.isValid()) {
			MI.Option("Internet", "No"); // avoid MediaInfoLib to try to connect to an Internet server for availability of newer software, anonymous statistics and retrieving information about a file
			MI.Option("Complete", "1");
			MI.Option("Language", "raw");
			MI.Option("File_TestContinuousFileNames", "0");
			LOGGER.debug("Option 'File_TestContinuousFileNames' is set to: " + MI.Option("File_TestContinuousFileNames_Get"));
			MI.Option("ParseSpeed", "0");
			LOGGER.debug("Option 'ParseSpeed' is set to: " + MI.Option("ParseSpeed_Get"));
//			LOGGER.debug(MI.Option("Info_Parameters_CSV")); // It can be used to export all current MediaInfo parameters
		}
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

	@Deprecated
	public synchronized static void parse(DLNAMediaInfo media, InputFile inputFile, int type) {
		parse(media, inputFile, type, null);
	}

	/**
	 * Parse media via MediaInfo.
	 */
	public synchronized static void parse(DLNAMediaInfo media, InputFile inputFile, int type, RendererConfiguration renderer) {
		File file = inputFile.getFile();
		if (!media.isMediaparsed() && file != null && MI.isValid() && MI.Open(file.getAbsolutePath()) > 0) {
//			try {
				StreamType general = StreamType.General;
				StreamType video = StreamType.Video;
				StreamType audio = StreamType.Audio;
				StreamType image = StreamType.Image;
				StreamType text = StreamType.Text;
				DLNAMediaAudio currentAudioTrack = new DLNAMediaAudio();
				DLNAMediaSubtitle currentSubTrack;
				media.setSize(file.length());
				String value;

				// set General
				getFormat(general, media, currentAudioTrack, MI.Get(general, 0, "Format"), file);
				getFormat(general, media, currentAudioTrack, MI.Get(general, 0, "CodecID").trim(), file);
				media.setDuration(getDuration(MI.Get(general, 0, "Duration/String1")));
				media.setBitrate(getBitrate(MI.Get(general, 0, "OverallBitRate")));
				value = MI.Get(general, 0, "Cover_Data");
				if (!value.isEmpty()) {
					media.setThumb(new Base64().decode(value.getBytes(StandardCharsets.US_ASCII)));
					media.setThumbready(true);
				}
				value = MI.Get(general, 0, "Title");
				if (!value.isEmpty()) {
					media.setFileTitleFromMetadata(value);
				}

				// set Video
				media.setVideoTrackCount(MI.Count_Get(video));
				if (media.getVideoTrackCount() > 0) {
					for (int i = 0; i < media.getVideoTrackCount(); i++) {
						// check for DXSA and DXSB subtitles (subs in video format)
						if (MI.Get(video, i, "Title").startsWith("Subtitle")) {
							currentSubTrack = new DLNAMediaSubtitle();
							// First attempt to detect subtitle track format
							currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(video, i, "Format")));
							// Second attempt to detect subtitle track format (CodecID usually is more accurate)
							currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(video, i, "CodecID")));
							currentSubTrack.setId(media.getSubtitleTracksList().size());
							addSub(currentSubTrack, media);
						} else {
							getFormat(video, media, currentAudioTrack, MI.Get(video, i, "Format"), file);
							getFormat(video, media, currentAudioTrack, MI.Get(video, i, "Format_Version"), file);
							getFormat(video, media, currentAudioTrack, MI.Get(video, i, "CodecID"), file);
							media.setWidth(getPixelValue(MI.Get(video, i, "Width")));
							media.setHeight(getPixelValue(MI.Get(video, i, "Height")));
							media.setMatrixCoefficients(MI.Get(video, i, "matrix_coefficients"));
							media.setStereoscopy(MI.Get(video, i, "MultiView_Layout"));
							media.setAspectRatioContainer(MI.Get(video, i, "DisplayAspectRatio/String"));
							media.setAspectRatioVideoTrack(MI.Get(video, i, "DisplayAspectRatio_Original/String"));
							media.setFrameRate(getFPSValue(MI.Get(video, i, "FrameRate")));
							media.setFrameRateOriginal(MI.Get(video, i, "FrameRate_Original"));
							media.setFrameRateMode(getFrameRateModeValue(MI.Get(video, i, "FrameRate_Mode")));
							media.setFrameRateModeRaw(MI.Get(video, i, "FrameRate_Mode"));
							media.setReferenceFrameCount(getReferenceFrameCount(MI.Get(video, i, "Format_Settings_RefFrames/String")));
							media.setVideoTrackTitleFromMetadata(MI.Get(video, i, "Title"));
							value = MI.Get(video, i, "Format_Settings_QPel");
							if (!value.isEmpty()) {
								media.putExtra(FormatConfiguration.MI_QPEL, value);
							}

							value = MI.Get(video, i, "Format_Settings_GMC");
							if (!value.isEmpty()) {
								media.putExtra(FormatConfiguration.MI_GMC, value);
							}

							value = MI.Get(video, i, "Format_Settings_GOP");
							if (!value.isEmpty()) {
								media.putExtra(FormatConfiguration.MI_GOP, value);
							}

							media.setMuxingMode(MI.Get(video, i, "MuxingMode"));
							if (!media.isEncrypted()) {
								media.setEncrypted("encrypted".equals(MI.Get(video, i, "Encryption")));
							}

							value = MI.Get(video, i, "BitDepth");
							if (!value.isEmpty()) {
								try {
									media.setVideoBitDepth(Integer.parseInt(value));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse bits per sample \"" + value + "\"");
								}
							}
						}

						value = MI.Get(video, i, "Format_Profile");
						if (!value.isEmpty() && media.getCodecV() != null && media.getCodecV().equals(FormatConfiguration.H264)) {
							media.setAvcLevel(getAvcLevel(value));
						}
					}
				}

				// set Audio
				int audioTracks = MI.Count_Get(audio);
				if (audioTracks > 0) {
					for (int i = 0; i < audioTracks; i++) {
						currentAudioTrack = new DLNAMediaAudio();
						getFormat(audio, media, currentAudioTrack, MI.Get(audio, i, "Format"), file);
						getFormat(audio, media, currentAudioTrack, MI.Get(audio, i, "Format_Version"), file);
						getFormat(audio, media, currentAudioTrack, MI.Get(audio, i, "Format_Profile"), file);
						getFormat(audio, media, currentAudioTrack, MI.Get(audio, i, "CodecID"), file);
						currentAudioTrack.setLang(getLang(MI.Get(audio, i, "Language/String")));
						currentAudioTrack.setAudioTrackTitleFromMetadata((MI.Get(audio, i, "Title")).trim());
						currentAudioTrack.getAudioProperties().setNumberOfChannels(MI.Get(audio, i, "Channel(s)"));
						currentAudioTrack.setSampleFrequency(getSampleFrequency(MI.Get(audio, i, "SamplingRate")));
						currentAudioTrack.setBitRate(getBitrate(MI.Get(audio, i, "BitRate")));
						currentAudioTrack.setSongname(MI.Get(general, 0, "Track"));

						if (
							renderer.isPrependTrackNumbers() &&
							currentAudioTrack.getTrack() > 0 &&
							currentAudioTrack.getSongname() != null &&
							currentAudioTrack.getSongname().length() > 0
						) {
							currentAudioTrack.setSongname(currentAudioTrack.getTrack() + ": " + currentAudioTrack.getSongname());
						}

						currentAudioTrack.setAlbum(MI.Get(general, 0, "Album"));
						currentAudioTrack.setArtist(MI.Get(general, 0, "Performer"));
						currentAudioTrack.setGenre(MI.Get(general, 0, "Genre"));
						// Try to parse the year from the stored date
						String recordedDate = MI.Get(general, 0, "Recorded_Date");
						Matcher matcher = yearPattern.matcher(recordedDate);
						if (matcher.matches()) {
							try {
								currentAudioTrack.setYear(Integer.parseInt(matcher.group(1)));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse year from recorded date \"" + recordedDate + "\"");
							}
						}

						// Special check for OGM: MediaInfo reports specific Audio/Subs IDs (0xn) while mencoder does not
						value = MI.Get(audio, i, "ID/String");
						if (!value.isEmpty()) {
							if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
								currentAudioTrack.setId(getSpecificID(value));
							} else {
								currentAudioTrack.setId(media.getAudioTracksList().size());
							}
						}

						value = MI.Get(general, i, "Track/Position");
						if (!value.isEmpty()) {
							try {
								currentAudioTrack.setTrack(Integer.parseInt(value));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Could not parse track \"" + value + "\"");
							}
						}

						value = MI.Get(audio, i, "BitDepth");
						if (!value.isEmpty()) {
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
				media.setImageCount(MI.Count_Get(image));
				if (media.getImageCount() > 0) {
					boolean parseByMediainfo = false;
					// for image parsing use Imaging instead of the MediaInfo which doesn't provide enough information
					try {
						ImagesUtil.parseImageByImaging(file, media);
						media.setContainer(media.getCodecV());
					} catch (ImageReadException | IOException e) {
						LOGGER.debug("Error when parsing image ({}) with Imaging, switching to MediaInfo.", file.getAbsolutePath());
						parseByMediainfo = true;
					}

					if (parseByMediainfo) {
						getFormat(image, media, currentAudioTrack, MI.Get(image, 0, "Format"), file);
						media.setWidth(getPixelValue(MI.Get(image, 0, "Width")));
						media.setHeight(getPixelValue(MI.Get(image, 0, "Height")));
					}
					
//					media.setImageCount(media.getImageCount() + 1);
				}

				// set Subs in text format
				int subTracks = MI.Count_Get(text);
				if (subTracks > 0) {
					for (int i = 0; i < subTracks; i++) {
						currentSubTrack = new DLNAMediaSubtitle();
						currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(text, i, "Format")));
						currentSubTrack.setType(SubtitleType.valueOfLibMediaInfoCodec(MI.Get(text, i, "CodecID")));
						currentSubTrack.setLang(getLang(MI.Get(text, i, "Language/String")));
						currentSubTrack.setSubtitlesTrackTitleFromMetadata((MI.Get(text, i, "Title")).trim());
						// Special check for OGM: MediaInfo reports specific Audio/Subs IDs (0xn) while mencoder does not
						value = MI.Get(text, i, "ID/String");
						if (!value.isEmpty()) {
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
//			} catch (Exception e) {
//				LOGGER.error("Error in MediaInfo parsing:", e);
//			} finally {
				MI.Close();
				if (media.getContainer() == null) {
					media.setContainer(DLNAMediaLang.UND);
				}

				if (media.getCodecV() == null) {
					media.setCodecV(DLNAMediaLang.UND);
				}

				media.setMediaparsed(true);
//			}
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

	/**
	 * Sends the correct information to media.setContainer(),
	 * media.setCodecV() or media.setCodecA, depending on streamType.
	 *
	 * TODO: Rename to something like setFormat - this is not a getter.
	 *
	 * @param streamType
	 * @param media
	 * @param audio
	 * @param value
	 * @param file 
	 */
	private static void getFormat(StreamType streamType, DLNAMediaInfo media, DLNAMediaAudio audio, String value, File file) {
		if (value.isEmpty()) {
			return;
		}

		value = value.toLowerCase();
		String format = null;

		if (isBlank(value)) {
			return;
		} else if (value.startsWith("3g2")) {
			format = FormatConfiguration.THREEGPP2;
		} else if (value.startsWith("3gp")) {
			format = FormatConfiguration.THREEGPP;
		} else if (value.startsWith("matroska")) {
			format = FormatConfiguration.MATROSKA;
		} else if (value.equals("avi") || value.equals("opendml")) {
			format = FormatConfiguration.AVI;
		} else if (value.startsWith("cinepack")) {
			format = FormatConfiguration.CINEPACK;
		} else if (value.startsWith("flash")) {
			format = FormatConfiguration.FLV;
		} else if (value.equals("webm")) {
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
		} else if (value.contains("windows media") || value.equals("wmv1") || value.equals("wmv2") || value.equals("wmv7") || value.equals("wmv8")) {
			format = FormatConfiguration.WMV;
		} else if (value.contains("mjpg") || value.contains("m-jpeg")) {
			format = FormatConfiguration.MJPEG;
		} else if (value.startsWith("h263") || value.startsWith("s263") || value.startsWith("u263")) {
			format = FormatConfiguration.H263;
		} else if (value.startsWith("avc") || value.startsWith("h264")) {
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
		} else if (value.equals("au") || value.equals("uLaw/AU Audio File")) {
			format = FormatConfiguration.AU;
		} else if (value.equals("layer 3")) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.MPA)) {
				format = FormatConfiguration.MP3;
				// special case:
				if (media.getContainer() != null && media.getContainer().equals(FormatConfiguration.MPA)) {
					media.setContainer(FormatConfiguration.MP3);
				}
			}
		} else if (value.equals("layer 2") && audio.getCodecA() != null && media.getContainer() != null &&
				   audio.getCodecA().equals(FormatConfiguration.MPA) && media.getContainer().equals(FormatConfiguration.MPA)) {
			// only for audio files:
			format = FormatConfiguration.MP2;
			media.setContainer(FormatConfiguration.MP2);
		} else if (value.equals ("ma") || value.equals("ma / core") || value.equals("134")) {
			if (audio.getCodecA() != null && audio.getCodecA().equals(FormatConfiguration.DTS)) {
				format = FormatConfiguration.DTSHD;
			}
		} else if (value.equals("vorbis") || value.equals("a_vorbis")) {
			format = FormatConfiguration.VORBIS;
		} else if (value.equals("adts")) {
			format = FormatConfiguration.ADTS;
		} else if (value.startsWith("amr")) {
			format = FormatConfiguration.AMR;
		} else if (value.equals("ac-3") || value.equals("a_ac3") || value.equals("2000")) {
			format = FormatConfiguration.AC3;
		} else if (value.startsWith("cook")) {
			format = FormatConfiguration.COOK;
		} else if (value.startsWith("qdesign")) {
			format = FormatConfiguration.QDESIGN;
		} else if (value.equals("realaudio lossless")) {
			format = FormatConfiguration.REALAUDIO_LOSSLESS;
		} else if (value.equals("e-ac-3")) {
			format = FormatConfiguration.EAC3;
		} else if (value.contains("truehd")) {
			format = FormatConfiguration.TRUEHD;
		} else if (value.equals("tta")) {
			format = FormatConfiguration.TTA;
		} else if (value.equals("55") || value.equals("a_mpeg/l3")) {
			format = FormatConfiguration.MP3;
		} else if (value.equals("lc")) {
			format = FormatConfiguration.AAC;
		} else if (value.contains("he-aac")) {
			format = FormatConfiguration.AAC_HE;
		} else if (value.startsWith("adpcm")) {
			format = FormatConfiguration.ADPCM;
		} else if (value.equals("pcm") || (value.equals("1") && (audio.getCodecA() == null || !audio.getCodecA().equals(FormatConfiguration.DTS)))) {
			format = FormatConfiguration.LPCM;
		} else if (value.equals("alac")) {
			format = FormatConfiguration.ALAC;
		} else if (value.equals("wave")) {
			format = FormatConfiguration.WAV;
		} else if (value.equals("shorten")) {
			format = FormatConfiguration.SHORTEN;
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
		} else if (value.startsWith("wma")) {
			format = FormatConfiguration.WMA;
			if (media.getCodecV() == null) {
				media.setContainer(format);
			}
		} else if (
			streamType == StreamType.Audio && media.getCodecV() == null && audio != null && audio.getCodecA() != null &&
			audio.getCodecA() == FormatConfiguration.WMA &&
			(value.equals("161") || value.equals("162") || value.equals("163") || value.equalsIgnoreCase("A"))
		) {
			if (value.equals("161")) {
				format = FormatConfiguration.WMA;
			} else if (value.equals("162")) {
				format = FormatConfiguration.WMAPRO;
			} else if (value.equals("163")) {
				format = FormatConfiguration.WMALOSSLESS;
			} else if (value.equalsIgnoreCase("A")) {
				format = FormatConfiguration.WMAVOICE;
			}
		} else if (value.equals("flac")) {
			format = FormatConfiguration.FLAC;
		} else if (value.equals("monkey's audio")) {
			format = FormatConfiguration.MONKEYS_AUDIO;
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
		} else if (containsIgnoreCase(value, "@l") && streamType == StreamType.Video) {
			media.setAvcLevel(getAvcLevel(value));
			media.setH264Profile(getAvcProfile(value));
		}

		if (format != null) {
			if (streamType == StreamType.General) {
				media.setContainer(format);
			} else if (streamType == StreamType.Video) {
				media.setCodecV(format);
			} else if (streamType == StreamType.Audio) {
				audio.setCodecA(format);
			}
		// format not found so set container type based on the file extension. It will be overwritten when the correct type will be found
		} else if (streamType == StreamType.General && media.getContainer() == null) {
			media.setContainer(FileUtil.getExtension(file.getAbsolutePath()));
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
			return Byte.parseByte(substringBefore(value, " "));
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
		String profile = substringBefore(lowerCase(value), "@l");
		if (isNotBlank(profile)) {
			return profile;
		} else {
			LOGGER.warn("Could not parse AvcProfile value {}." , value);
			return null;
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

	/**
	 * @deprecated use trim()
	 */
	@Deprecated
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
}
