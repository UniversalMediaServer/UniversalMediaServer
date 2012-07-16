package net.pms.dlna;

import java.io.File;
import java.util.StringTokenizer;
import net.pms.configuration.FormatConfiguration;
import net.pms.formats.v2.SubtitleType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibMediaInfoParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibMediaInfoParser.class);
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

	public synchronized static void parse(DLNAMediaInfo media, InputFile file, int type) {
		File filename = file.getFile();
		if (!media.isMediaparsed() && filename != null && MI.isValid() && MI.Open(filename.getAbsolutePath()) > 0) {
			try {
				String info = MI.Inform();
				MediaInfo.StreamKind step = MediaInfo.StreamKind.General;
				DLNAMediaAudio currentAudioTrack = new DLNAMediaAudio();
				boolean audioPrepped = false;
				DLNAMediaSubtitle currentSubTrack = new DLNAMediaSubtitle();
				boolean subPrepped = false;
				if (StringUtils.isNotBlank(info)) {
					media.setSize(filename.length());
					StringTokenizer st = new StringTokenizer(info, "\n\r");
					while (st.hasMoreTokens()) {
						String line = st.nextToken().trim();

						if (line.equals("Video") || line.startsWith("Video #")) {
							step = MediaInfo.StreamKind.Video;
						} else if (line.equals("Audio") || line.startsWith("Audio #")) {
							if (audioPrepped) {
								addAudio(currentAudioTrack, media);
								currentAudioTrack = new DLNAMediaAudio();
							}
							audioPrepped = true;
							step = MediaInfo.StreamKind.Audio;
						} else if (line.equals("Text") || line.startsWith("Text #")) {
							if (subPrepped) {
								addSub(currentSubTrack, media);
								currentSubTrack = new DLNAMediaSubtitle();
							}
							subPrepped = true;
							step = MediaInfo.StreamKind.Text;
						} else if (line.equals("Menu") || line.startsWith("Menu #")) {
							step = MediaInfo.StreamKind.Menu;
						} else if (line.equals("Chapters")) {
							step = MediaInfo.StreamKind.Chapters;
						}
						int point = line.indexOf(":");
						if (point > -1) {
							String key = line.substring(0, point).trim();
							String ovalue = line.substring(point + 1).trim();
							String value = ovalue.toLowerCase();
							if (key.equals("Format") || key.startsWith("Format_Version") || key.startsWith("Format_Profile")) {
								if (step == MediaInfo.StreamKind.Text) {
									// First attempt to detect subtitle track format
									currentSubTrack.setType(SubtitleType.getSubtitleTypeByLibMediaInfoCodec(value));
								} else {
									getFormat(step, media, currentAudioTrack, value);
								}
							} else if (key.equals("Duration/String1") && step == MediaInfo.StreamKind.General) {
								media.setDuration(getDuration(value));
							} else if (key.equals("Codec_Settings_QPel") && step == MediaInfo.StreamKind.Video) {
								media.putExtra(FormatConfiguration.MI_QPEL, value);
							} else if (key.equals("Codec_Settings_GMC") && step == MediaInfo.StreamKind.Video) {
								media.putExtra(FormatConfiguration.MI_GMC, value);
							} else if (key.equals("MuxingMode") && step == MediaInfo.StreamKind.Video) {
								media.setMuxingMode(ovalue);
							} else if (key.equals("CodecID")) {
								if (step == MediaInfo.StreamKind.Text) {
									// Second attempt to detect subtitle track format (CodecID usually is more accurate)
									currentSubTrack.setType(SubtitleType.getSubtitleTypeByLibMediaInfoCodec(value));
								} else {
									getFormat(step, media, currentAudioTrack, value);
								}
							} else if (key.equals("Language/String")) {
								if (step == MediaInfo.StreamKind.Audio) {
									currentAudioTrack.setLang(getLang(value));
								} else if (step == MediaInfo.StreamKind.Text) {
									currentSubTrack.setLang(getLang(value));
								}
							} else if (key.equals("Title")) {
								if (step == MediaInfo.StreamKind.Audio) {
									currentAudioTrack.setFlavor(getFlavor(value));
								} else if (step == MediaInfo.StreamKind.Text) {
									currentSubTrack.setFlavor(getFlavor(value));
								}
							} else if (key.equals("Width")) {
								media.setWidth(getPixelValue(value));
							} else if (key.equals("Encryption") && !media.isEncrypted()) {
								media.setEncrypted("encrypted".equals(value));
							} else if (key.equals("Height")) {
								media.setHeight(getPixelValue(value));
							} else if (key.equals("FrameRate")) {
								media.setFrameRate(getFPSValue(value));
							} else if (key.equals("FrameRateMode")) {
								media.setFrameRateMode(getFrameRateModeValue(value));
							} else if (key.equals("OverallBitRate")) {
								if (step == MediaInfo.StreamKind.General) {
									media.setBitrate(getBitrate(value));
								}
							} else if (key.equals("Channel(s)")) {
								if (step == MediaInfo.StreamKind.Audio) {
									currentAudioTrack.getAudioProperties().setNumberOfChannels(value);
								}
							} else if (key.equals("BitRate")) {
								if (step == MediaInfo.StreamKind.Audio) {
									currentAudioTrack.setBitRate(getBitrate(value));
								}
							} else if (key.equals("SamplingRate")) {
								if (step == MediaInfo.StreamKind.Audio) {
									currentAudioTrack.setSampleFrequency(getSampleFrequency(value));
								}
							} else if (key.equals("ID/String")) {
								// Special check for OGM: MediaInfo reports specific Audio/Subs IDs (0xn) while mencoder does not
								if (value.contains("(0x") && !FormatConfiguration.OGG.equals(media.getContainer())) {
									if (step == MediaInfo.StreamKind.Audio) {
										currentAudioTrack.setId(getSpecificID(value));
									} else if (step == MediaInfo.StreamKind.Text) {
										currentSubTrack.setId(getSpecificID(value));
									}
								} else {
									if (step == MediaInfo.StreamKind.Audio) {
										currentAudioTrack.setId(media.getAudioTracksList().size());
									} else if (step == MediaInfo.StreamKind.Text) {
										currentSubTrack.setId(media.getSubtitleTracksList().size());
									}
								}
							} else if (key.equals("Cover_Data") && step == MediaInfo.StreamKind.General) {
								media.setThumb(getCover(ovalue));
							} else if (key.equals("Track") && step == MediaInfo.StreamKind.General) {
								currentAudioTrack.setSongname(ovalue);
							} else if (key.equals("Album") && step == MediaInfo.StreamKind.General) {
								currentAudioTrack.setAlbum(ovalue);
							} else if (key.equals("Performer") && step == MediaInfo.StreamKind.General) {
								currentAudioTrack.setArtist(ovalue);
							} else if (key.equals("Genre") && step == MediaInfo.StreamKind.General) {
								currentAudioTrack.setGenre(ovalue);
							} else if (key.equals("Recorded_Date") && step == MediaInfo.StreamKind.General) {
								try {
									currentAudioTrack.setYear(Integer.parseInt(value));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse year \"" + value + "\"");
								}
							} else if (key.equals("Track/Position") && step == MediaInfo.StreamKind.General) {
								try {
									currentAudioTrack.setTrack(Integer.parseInt(value));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse track \"" + value + "\"");
								}
							} else if (key.equals("Resolution") && step == MediaInfo.StreamKind.Audio) {
								try {
									currentAudioTrack.setBitsperSample(Integer.parseInt(value));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse bits per sample \"" + value + "\"");
								}
							} else if (key.equals("Video_Delay") && step == MediaInfo.StreamKind.Audio) {
								try {
									currentAudioTrack.getAudioProperties().setAudioDelay(value);
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse delay \"" + value + "\"");
								}
							}
						}
					}
				}
				if (audioPrepped) {
					addAudio(currentAudioTrack, media);
				}
				if (subPrepped) {
					addSub(currentSubTrack, media);
				}
				media.finalize(type, file);
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
		if (currentAudioTrack.getLang() == null) {
			currentAudioTrack.setLang(DLNAMediaLang.UND);
		}
		if (currentAudioTrack.getCodecA() == null) {
			currentAudioTrack.setCodecA(DLNAMediaLang.UND);
		}
		media.getAudioTracksList().add(currentAudioTrack);
	}

	public static void addSub(DLNAMediaSubtitle currentSubTrack, DLNAMediaInfo media) {
		if (currentSubTrack.getType() == SubtitleType.UNSUPPORTED) {
			return;
		}
		if (currentSubTrack.getLang() == null) {
			currentSubTrack.setLang(DLNAMediaLang.UND);
		}
		media.getSubtitleTracksList().add(currentSubTrack);
	}

	public static void getFormat(MediaInfo.StreamKind current, DLNAMediaInfo media, DLNAMediaAudio audio, String value) {
		String format = null;
		if (value.equals("matroska")) {
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
		} else if (value.startsWith("avc") || value.contains("h264")) {
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
		} else if (value.equals("version 1")) {
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
		} else if (value.equals("m4a") || value.equals("40") || value.equals("a_aac") || value.equals("aac")) {
			format = FormatConfiguration.AAC;
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
		}

		if (format != null) {
			if (current == MediaInfo.StreamKind.General) {
				media.setContainer(format);
			} else if (current == MediaInfo.StreamKind.Video) {
				media.setCodecV(format);
			} else if (current == MediaInfo.StreamKind.Audio) {
				audio.setCodecA(format);
			}
		}
	}

	public static int getPixelValue(String value) {
		if (value.indexOf("pixel") > -1) {
			value = value.substring(0, value.indexOf("pixel"));
		}
		value = value.trim();

		// Value can look like "512 / 512" at this point
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf("/")).trim();
		}

		int pixels = Integer.parseInt(value);
		return pixels;
	}

	public static int getBitrate(String value) {
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf("/")).trim();
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			LOGGER.info("Unknown bitrate detected. Returning 0.");
			return 0;
		}
	}

	public static int getSpecificID(String value) {
		if (value.indexOf("(0x") > -1) {
			value = value.substring(0, value.indexOf("(0x"));
		}
		value = value.trim();
		int id = Integer.parseInt(value);
		return id;
	}

	public static String getSampleFrequency(String value) {
		// some tracks show several values like "48000 / 48000 / 24000" for HE-AAC
		// store only first value
		if (value.indexOf("/") > -1) {
			value = value.substring(0, value.indexOf("/"));
		}
		if (value.indexOf("khz") > -1) {
			value = value.substring(0, value.indexOf("khz"));
		}
		value = value.trim();
		return value;
	}

	public static String getFPSValue(String value) {
		if (value.indexOf("fps") > -1) {
			value = value.substring(0, value.indexOf("fps"));
		}
		value = value.trim();
		return value;
	}

	public static String getFrameRateModeValue(String value) {
		if (value.indexOf("/") > -1) {
			value = value.substring(0, value.indexOf("/"));
		}

		value = value.trim();
		return value;
	}

	public static String getLang(String value) {
		if (value.indexOf("(") > -1) {
			value = value.substring(0, value.indexOf("("));
		}
		if (value.indexOf("/") > -1) {
			value = value.substring(0, value.indexOf("/"));
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
			int hl = token.indexOf("h");
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
				int sl = token.indexOf("s");
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
