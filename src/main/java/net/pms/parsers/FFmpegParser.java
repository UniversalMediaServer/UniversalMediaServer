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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.StandardEngineId;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.FailSafeProcessWrapper;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.audio.MediaAudio;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.store.ThumbnailSource;
import net.pms.util.InputFile;
import net.pms.util.MpegUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegParser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final String PARSER_NAME = "FFmpeg";

	private static String version;

	/**
	 * This class is not meant to be instantiated.
	 */
	private FFmpegParser() {
	}

	/**
	 * Parse media without using MediaInfo.
	 */
	public static void parse(MediaInfo media, InputFile inputFile, Format ext, int type) {
		if (!media.waitMediaParsing(5) || media.isMediaParsed()) {
			return;
		}
		if (inputFile != null) {
			File file = inputFile.getFile();
			if (file != null) {
				media.setSize(file.length());
			} else {
				media.setSize(inputFile.getSize());
			}

			boolean ffmpegParsing = true;

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				ffmpegParsing = false;
				JaudiotaggerParser.parse(media, file, ext);
			}

			if (type == Format.IMAGE && file != null) {
				try {
					ffmpegParsing = false;
					MetadataExtractorParser.parse(file, media);
					media.setImageCount(media.getImageCount() + 1);
				} catch (IOException e) {
					LOGGER.debug("Error parsing image \"{}\", switching to FFmpeg: {}", file.getAbsolutePath(), e.getMessage());
					LOGGER.trace("", e);
					ffmpegParsing = true;
				}
			}

			if (ffmpegParsing) {
				parse(media, inputFile);
				if (
					file != null &&
					"mpegts".equals(media.getContainer()) &&
					media.getDefaultVideoTrack() != null &&
					media.getDefaultVideoTrack().isH264() &&
					media.getDurationInSeconds() == 0
				) {
					// Parse the duration
					try {
						int length = MpegUtil.getDurationFromMpeg(file);
						if (length > 0) {
							media.setDuration((double) length);
						}
					} catch (IOException e) {
						LOGGER.trace("Error retrieving length: " + e.getMessage());
					}
				}
			}
			Parser.postParse(media, type);
		}
	}

	private static void parse(MediaInfo media, InputFile inputFile) {
		/*
		 * Note: The text output from FFmpeg is used by renderers that do
		 * not use MediaInfo, so do not make any changes that remove or
		 * minimize the amount of text given by FFmpeg here
		 */
		String engine = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (engine == null) {
			LOGGER.warn("Cannot parse since the FFmpeg executable is undefined");
			return;
		}

		ArrayList<String> args = new ArrayList<>();
		args.add(engine);
		args.add("-hide_banner");
		args.add("-i");

		String input;
		if (inputFile.getFile() != null) {
			input = ProcessUtil.getShortFileNameIfWideChars(inputFile.getFile().getAbsolutePath());
		} else {
			input = "-";
		}
		args.add(input);

		args.add("-vn");
		args.add("-an");
		args.add("-dn");
		args.add("-sn");

		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setStdIn(inputFile.getPush());
		params.setNoExitCheck(true); // not serious if anything happens during the thumbnailer

		// true: consume stderr on behalf of the caller i.e. parse()
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params, false, true);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 10000);
		media.setParsing(true);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the file: " + input);
		} else {
			parseFFmpegInfo(media, pw.getResults(), input);
		}

		media.setParsing(false);
	}

	public static void parseUrl(MediaInfo media, String url) {
		String engine = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (engine == null) {
			LOGGER.warn("Cannot parse since the FFmpeg executable is undefined");
			return;
		}

		ArrayList<String> args = new ArrayList<>();
		args.add(engine);
		args.add("-hide_banner");
		args.add("-i");
		args.add(url);
		args.add("-t");
		args.add("1");
		args.add("-f");
		args.add("null");
		args.add("-");

		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);

		// true: consume stderr on behalf of the caller i.e. parse()
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params, false, true);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 10000);
		media.setParsing(true);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the url: " + url);
		} else {
			parseFFmpegInfo(media, pw.getResults(), url);
		}

		media.setParsing(false);
	}

	public static DLNAThumbnail getThumbnail(MediaInfo media, InputFile inputFile, Double seekPosition) {
		/*
		 * Note: The text output from FFmpeg is used by renderers that do
		 * not use MediaInfo, so do not make any changes that remove or
		 * minimize the amount of text given by FFmpeg here
		 */
		String engine = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (engine == null) {
			LOGGER.warn("Cannot generate thumbnail since the FFmpeg executable is undefined");
			return null;
		}
		ArrayList<String> args = new ArrayList<>();
		args.add(engine);
		DLNAThumbnail thumbnail = null;
		args.add("-ss");
		double thumbnailSeekPos = seekPosition != null ? seekPosition : CONFIGURATION.getThumbnailSeekPos();
		thumbnailSeekPos = Math.min(thumbnailSeekPos, media.getDurationInSeconds());
		args.add(Integer.toString((int) thumbnailSeekPos));

		args.add("-i");

		if (inputFile.getFile() != null) {
			args.add(ProcessUtil.getShortFileNameIfWideChars(inputFile.getFile().getAbsolutePath()));
		} else {
			args.add("-");
		}

		args.add("-an");
		args.add("-dn");
		args.add("-sn");
		args.add("-vf");
		args.add("scale=320:-2");
		args.add("-vframes");
		args.add("1");
		args.add("-f");
		args.add("image2");
		args.add("pipe:");

		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setStdIn(inputFile.getPush());
		params.setNoExitCheck(true); // not serious if anything happens during the thumbnailer

		// true: consume stderr on behalf of the caller i.e. parse()
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params);

		// FAILSAFE
		media.waitMediaParsing(5);
		media.setParsing(true);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 3000);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error generating thumbnail from the file: " + inputFile.getFile());
			media.setParsing(false);
			return null;
		}

		if (pw.getOutputByteArray() != null) {
			byte[] bytes = pw.getOutputByteArray().toByteArray();
			if (bytes != null && bytes.length > 0) {
				try {
					thumbnail = DLNAThumbnail.toThumbnail(bytes);
					if (thumbnail != null) {
						media.setThumbnailSource(ThumbnailSource.FFMPEG_SEEK);
					}
				} catch (IOException e) {
					LOGGER.debug("Error while decoding thumbnail: " + e.getMessage());
					LOGGER.trace("", e);
				}
			}
		}
		media.setParsing(false);
		return thumbnail;
	}

	/**
	 * Parses media info from FFmpeg's stderr output
	 *
	 * @param lines The stderr output
	 * @param input The FFmpeg input (-i) argument used
	 */
	public static void parseFFmpegInfo(MediaInfo media, List<String> lines, String input) {
		if (lines != null) {
			if ("-".equals(input)) {
				input = "pipe:";
			}

			boolean matches = false;
			int videoId = 0;
			int audioId = 0;
			int subtitleId = 0;
			ListIterator<String> fFmpegMetaData = lines.listIterator();

			for (String line : lines) {
				fFmpegMetaData.next();
				line = line.trim();
				if (line.startsWith("Output")) {
					matches = false;
				} else if (line.startsWith("Input")) {
					if (line.contains(input)) {
						matches = true;
						media.setContainer(line.substring(10, line.indexOf(',', 11)).trim());

						/**
						 * This method is very inaccurate because the Input line in the FFmpeg output
						 * returns "mov,mp4,m4a,3gp,3g2,mj2" for all 6 of those formats, meaning that
						 * we think they are all "mov".
						 *
						 * Here we workaround it by using the file extension, but the best idea is to
						 * prevent using this method by using MediaInfo=true in renderer configs.
						 */
						if ("mov".equals(media.getContainer())) {
							media.setContainer(line.substring(line.lastIndexOf('.') + 1, line.lastIndexOf('\'')).trim());
							LOGGER.trace("Setting container to " + media.getContainer() + " from the filename. To prevent false-positives, use MediaInfo=true in the renderer config.");
						}
						if ("matroska".equals(media.getContainer())) {
							media.setContainer(FormatConfiguration.MKV);
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
									media.setDuration(null);
								} else {
									media.setDuration(parseDurationString(durationStr));
								}
							} else if (token.startsWith("bitrate: ")) {
								String bitr = token.substring(9);
								int spacepos = bitr.indexOf(' ');
								if (spacepos > -1) {
									String value = bitr.substring(0, spacepos);
									String unit = bitr.substring(spacepos + 1);
									int bitrate = Integer.parseInt(value);
									if (unit.equals("kb/s")) {
										bitrate = 1024 * bitrate;
									}
									if (unit.equals("mb/s")) {
										bitrate = 1048576 * bitrate;
									}
									media.setBitRate(bitrate);
								}
							}
						}
					} else if (line.contains("Audio:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						MediaAudio audio = new MediaAudio();
						audio.setId(audioId++);
						audio.setStreamOrder(getStreamOrder(line));
						audio.setLang(getLanguage(line));
						audio.setDefault(line.contains("(default)"));
						audio.setForced(line.contains("(forced)"));

						// Get TS IDs
						int a = line.indexOf("[0x");
						int b = line.indexOf(']', a);
						if (a > -1 && b > a + 3) {
							String idString = line.substring(a + 3, b);
							try {
								audio.setOptionalId(Long.valueOf(idString, 16));
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
								audio.setCodec(codec);
							} else if (token.endsWith("Hz")) {
								String sampleRate = token.substring(0, token.indexOf("Hz")).trim();
								try {
									audio.setSampleRate(Integer.parseInt(sampleRate));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse sample rate \"" + sampleRate + "\"");
								}
							} else if (token.equals("mono")) {
								audio.setNumberOfChannels(1);
							} else if (token.equals("stereo")) {
								audio.setNumberOfChannels(2);
							} else if (token.equals("5:1") || token.equals("5.1") || token.equals("6 channels")) {
								audio.setNumberOfChannels(6);
							} else if (token.equals("5 channels")) {
								audio.setNumberOfChannels(5);
							} else if (token.equals("4 channels")) {
								audio.setNumberOfChannels(4);
							} else if (token.equals("2 channels")) {
								audio.setNumberOfChannels(2);
							} else if (token.equals("s32")) {
								audio.setBitDepth(32);
							} else if (token.equals("s24")) {
								audio.setBitDepth(24);
							} else if (token.equals("s16")) {
								audio.setBitDepth(16);
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
										audio.setTitle(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}
						media.addAudioTrack(audio);
					} else if (line.contains("Video:")) {
						MediaVideo video = new MediaVideo();
						video.setId(videoId++);
						video.setStreamOrder(getStreamOrder(line));
						video.setLang(getLanguage(line));
						video.setDefault(line.contains("(default)"));
						video.setForced(line.contains("(forced)"));
						StringTokenizer st = new StringTokenizer(line, ",");
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								String videoString = "Video: ";
								//get the codec
								int positionAfterVideoString = token.indexOf(videoString) + videoString.length();
								String codec = token.substring(positionAfterVideoString);
								// Check whether there are more details after the video string
								int profilePos = codec.indexOf(" (");
								if (profilePos > -1) {
									video.setFormatProfile(getFormatProfile(codec));
									codec = codec.substring(0, profilePos).trim();
								}
								video.setCodec(codec);
							} else if ((token.contains("tbc") || token.contains("tb(c)"))) {
								// A/V sync issues with newest FFmpeg, due to the new tbr/tbn/tbc outputs
								// Priority to tb(c)
								String frameRate = token.substring(0, token.indexOf("tb")).trim();
								try {
									Double frameRateDouble = Double.valueOf(frameRate);
									// tbc taken into account only if different than tbr
									if (!frameRateDouble.equals(video.getFrameRate())) {
										video.setFrameRate(frameRateDouble / 2);
									}
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse frame rate \"" + frameRate + "\"");
								}
							} else if ((token.contains("tbr") || token.contains("tb(r)")) && video.getFrameRate() == null) {
								String frameRate = token.substring(0, token.indexOf("tb")).trim();
								try {
									video.setFrameRate(Double.valueOf(frameRate));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse frame rate \"" + frameRate + "\"");
								}
							} else if ((token.contains("fps") || token.contains("fps(r)")) && video.getFrameRate() == null) { // dvr-ms ?
								String frameRate = token.substring(0, token.indexOf("fps")).trim();
								try {
									video.setFrameRate(Double.valueOf(frameRate));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse frame rate \"" + frameRate + "\"");
								}
							} else if (token.indexOf('x') > -1 && !token.contains("max")) {
								String resolution = token.trim();
								if (resolution.contains(" [")) {
									resolution = resolution.substring(0, resolution.indexOf(" ["));
								}
								try {
									video.setWidth(Integer.parseInt(resolution.substring(0, resolution.indexOf('x'))));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse width from \"" + resolution.substring(0, resolution.indexOf('x')) + "\"");
								}
								try {
									video.setHeight(Integer.parseInt(resolution.substring(resolution.indexOf('x') + 1)));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse height from \"" + resolution.substring(resolution.indexOf('x') + 1) + "\"");
								}
							}
						}
						media.addVideoTrack(video);
					} else if (line.contains("Subtitle:")) {
						MediaSubtitle subtitle = new MediaSubtitle();
						subtitle.setId(subtitleId++);
						subtitle.setStreamOrder(getStreamOrder(line));
						subtitle.setDefault(line.contains("(default)"));
						subtitle.setForced(line.contains("(forced)"));
						subtitle.setLang(getLanguage(line));
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
										subtitle.setTitle(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}
						media.addSubtitlesTrack(subtitle);
					} else if (line.contains("Chapters:")) {
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();
						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}
						List<MediaChapter> ffmpegChapters = new ArrayList<>();
						while (line.contains("Chapter #")) {
							MediaChapter chapter = new MediaChapter();
							//set chapter id
							chapter.setId(getStreamOrder(line));
							//set language if any
							chapter.setLang(getLanguage(line));
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
						media.setChapters(ffmpegChapters);
					}
				}
			}
		}
		media.setMediaParser(PARSER_NAME);
	}

	private static int getStreamOrder(String line) {
		String idStr = line.substring(line.indexOf("#") + 1);
		int pos = idStr.indexOf(' ');
		if (pos != -1) {
			idStr = idStr.substring(0, pos);
		}
		pos = idStr.indexOf('(');
		if (pos != -1) {
			idStr = idStr.substring(0, pos);
		}
		pos = idStr.indexOf('[');
		if (pos != -1) {
			idStr = idStr.substring(0, pos);
		}
		pos = idStr.lastIndexOf(':');
		if (pos == idStr.length() - 1) {
			idStr = idStr.substring(0, idStr.length() - 2);
		}
		String[] ids = idStr.split(":");
		try {
			if (ids.length > 1) {
				return (Integer.parseInt(ids[1]));
			} else {
				return (Integer.parseInt(ids[0]));
			}
		} catch (NumberFormatException e) {
			LOGGER.info("Error parsing stream index from the line: " + line);
			return 0;
		}
	}

	private static String getLanguage(String line) {
		int a = line.indexOf('(');
		int b = line.indexOf("):", a);
		if (a > -1 && b > a) {
			return line.substring(a + 1, b);
		} else {
			return MediaLang.UND;
		}
	}

	private static String getFormatProfile(String codec) {
		int profilePos = codec.indexOf('(');
		int profilePosEnd = codec.indexOf(')');
		if (profilePos > -1 && profilePosEnd > profilePos) {
			return codec.substring(profilePos + 1, profilePosEnd).toLowerCase();
		}
		return null;
	}

	private static Double parseDurationString(String duration) {
		return duration != null ? StringUtil.convertStringToTime(duration) : null;
	}

	public static byte[][] getAnnexBFrameHeader(InputFile f) {
		String[] cmdArray = new String[14];
		cmdArray[0] = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (cmdArray[0] == null) {
			LOGGER.warn("Cannot process Annex B Frame Header is FFmpeg executable is undefined");
			return null;
		}
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
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setStdIn(f.getPush());

		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 3000);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the file: " + f.getFilename());
			return null;
		}

		byte[] data = pw.getOutputByteArray().toByteArray();

		returnData[0] = data;
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
			byte[] header = new byte[kf - st];
			System.arraycopy(data, st, header, 0, kf - st);
			returnData[1] = header;
		}

		return returnData;
	}

	protected static String getVersion() {
		if (version != null) {
			return version;
		}
		String[] cmdArray = new String[2];
		cmdArray[0] = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (cmdArray[0] == null) {
			LOGGER.warn("Cannot check version if FFmpeg executable is undefined");
			return null;
		}
		cmdArray[1] = "-version";

		OutputParams params = new OutputParams(null);
		params.setLog(true);
		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params, true, false);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 3000);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error checking version");
			return null;
		}

		List<String> lines = pw.getOtherResults();
		for (String line : lines) {
			if (line.startsWith("ffmpeg version")) {
				int index = line.indexOf(" ", 15);
				if (index > 0) {
					version = line.substring(15, index);
				} else {
					version = line.substring(15);
				}
				return version;
			}
		}
		return version;
	}

	public static boolean isValid() {
		return EngineFactory.isEngineAvailable(StandardEngineId.FFMPEG_VIDEO);
	}

}
