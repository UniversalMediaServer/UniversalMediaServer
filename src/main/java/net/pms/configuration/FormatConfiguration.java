package net.pms.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.dlna.MediaInfoParser;
import net.pms.formats.Format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormatConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatConfiguration.class);

	public static final String MPEGPS = "mpegps";
	public static final String MPEGTS = "mpegts";
	public static final String WMV = "wmv";
	public static final String AVI = "avi";
	public static final String MP4 = "mp4";
	public static final String MOV = "mov";
	public static final String FLV = "flv";
	public static final String RM = "rm";
	public static final String MATROSKA = "mkv";
	public static final String WAV = "wav";
	public static final String WAVPACK = "wavpack";
	public static final String LPCM = "lpcm";
	public static final String AAC = "aac";
	public static final String AC3 = "ac3";
	public static final String MP3 = "mp3";
	public static final String SHORTEN = "shn";
	public static final String MPA = "mpa";
	public static final String OGG = "ogg";
	public static final String WMA = "wma";
	public static final String ALAC = "alac";
	public static final String DTS = "dts";
	public static final String DTSHD = "dtshd";
	public static final String TRUEHD = "truehd";
	public static final String EAC3 = "eac3";
	public static final String ATRAC = "atrac";
	public static final String FLAC = "flac";
	public static final String APE = "ape";
	public static final String RA = "ra";
	public static final String MPC = "mpc";
	public static final String AIFF = "aiff";
	public static final String DV = "dv";
	public static final String MPEG1 = "mpeg1";
	public static final String MPEG2 = "mpeg2";
	public static final String DIVX = "divx";
	public static final String H264 = "h264";
	public static final String MLP = "mlp";
	public static final String MJPEG = "mjpeg";
	public static final String VC1 = "vc1";
	public static final String JPG = "jpg";
	public static final String PNG = "png";
	public static final String GIF = "gif";
	public static final String TIFF = "tiff";
	public static final String BMP = "bmp";
	public static final String und = "und";
	public static final String MI_QPEL = "qpel";
	public static final String MI_GMC = "gmc";

	// Use old parser for Jpeg files (MediaInfo does not support EXIF)
	private static final String PARSER_V1_EXTENSIONS[] = new String[]{".jpg", ".jpe", ".jpeg"};

	public void parse(DLNAMediaInfo media, InputFile file, Format ext, int type) {
		boolean force_v1 = false;
		if (file.getFile() != null) {
			String fName = file.getFile().getName().toLowerCase();
			for (String e : PARSER_V1_EXTENSIONS) {
				if (fName.endsWith(e)) {
					force_v1 = true;
					break;
				}
			}
			if (force_v1) {
				media.parse(file, ext, type, false);
			} else {
				MediaInfoParser.parse(media, file, type);
			}
		} else {
			media.parse(file, ext, type, false);
		}
	}

	public static final String MIMETYPE_AUTO = "MIMETYPE_AUTO";
	ArrayList<Supports> list;

	class Supports {
		String format;
		String videocodec;
		String audiocodec;
		String mimetype;
		String maxnbchannels;
		String maxfrequency;
		String maxbitrate;
		String maxvideowidth;
		String maxvideoheight;
		Map<String, Pattern> miExtras;
		Pattern pformat;
		Pattern pvideocodec;
		Pattern paudiocodec;
		int imaxnbchannels = Integer.MAX_VALUE;
		int imaxfrequency = Integer.MAX_VALUE;
		int imaxbitrate = Integer.MAX_VALUE;
		int imaxvideowidth = Integer.MAX_VALUE;
		int imaxvideoheight = Integer.MAX_VALUE;

		Supports() {
			mimetype = MIMETYPE_AUTO;
		}

		boolean isValid() {
			boolean v = format != null && format.length() > 0;
			if (v) {
				try {
					pformat = Pattern.compile(format);
				} catch (PatternSyntaxException pe) {
					LOGGER.info("Couldn't resolve this pattern: " + format + " / " + pe.getMessage());
					v = false;
				}
				if (videocodec != null) {
					try {
						pvideocodec = Pattern.compile(videocodec);
					} catch (PatternSyntaxException pe) {
						LOGGER.info("Couldn't resolve this pattern: " + videocodec + " / " + pe.getMessage());
						v = false;
					}
				}
				if (audiocodec != null) {
					try {
						paudiocodec = Pattern.compile(audiocodec);
					} catch (PatternSyntaxException pe) {
						LOGGER.info("Couldn't resolve this pattern: " + audiocodec + " / " + pe.getMessage());
						v = false;
					}
				}
				try {
					if (maxnbchannels != null) {
						imaxnbchannels = Integer.parseInt(maxnbchannels);
					}
				} catch (Exception e) {
					LOGGER.info("Error in parsing number: " + maxnbchannels);
					v = false;
				}
				try {
					if (maxfrequency != null) {
						imaxfrequency = Integer.parseInt(maxfrequency);
					}
				} catch (Exception e) {
					LOGGER.info("Error in parsing number: " + maxfrequency);
					v = false;
				}
				try {
					if (maxbitrate != null) {
						imaxbitrate = Integer.parseInt(maxbitrate);
					}
				} catch (Exception e) {
					LOGGER.info("Error in parsing number: " + maxbitrate);
					v = false;
				}
				try {
					if (maxvideowidth != null) {
						imaxvideowidth = Integer.parseInt(maxvideowidth);
					}
				} catch (Exception e) {
					LOGGER.info("Error in parsing number: " + maxvideowidth);
					v = false;
				}
				try {
					if (maxvideoheight != null) {
						imaxvideoheight = Integer.parseInt(maxvideoheight);
					}
				} catch (Exception e) {
					LOGGER.info("Error in parsing number: " + maxvideoheight);
					v = false;
				}
			}
			return v;
		}

		public boolean match(String container, String videocodec, String audiocodec) {
			return match(container, videocodec, audiocodec, 0, 0, 0, 0, 0, null);
		}

		public boolean match(String format, String videocodec, String audiocodec, int nbAudioChannels, int frequency, int bitrate, int videowidth, int videoheight, Map<String, String> extras) {

			boolean matched = false;

			if (format != null && !(matched = pformat.matcher(format).matches())) {
				return false;
			}

			if (matched && videocodec != null && pvideocodec != null && !(matched = pvideocodec.matcher(videocodec).matches())) {
				return false;
			}

			if (matched && audiocodec != null && paudiocodec != null && !(matched = paudiocodec.matcher(audiocodec).matches())) {
				return false;
			}

			if (matched && nbAudioChannels > 0 && imaxnbchannels > 0 && nbAudioChannels > imaxnbchannels) {
				return false;
			}

			if (matched && frequency > 0 && imaxfrequency > 0 && frequency > imaxfrequency) {
				return false;
			}

			if (matched && bitrate > 0 && imaxbitrate > 0 && bitrate > imaxbitrate) {
				return false;
			}

			if (matched && videowidth > 0 && imaxvideowidth > 0 && videowidth > imaxvideowidth) {
				return false;
			}

			if (matched && videoheight > 0 && imaxvideoheight > 0 && videoheight > imaxvideoheight) {
				return false;
			}

			if (matched && extras != null && miExtras != null) {
				Iterator<String> keyIt = extras.keySet().iterator();
				while (keyIt.hasNext()) {
					String key = keyIt.next();
					String value = extras.get(key);
					if (matched && key.equals(MI_QPEL) && miExtras.get(MI_QPEL) != null) {
						matched = miExtras.get(MI_QPEL).matcher(value).matches();
					} else if (matched && key.equals(MI_GMC) && miExtras.get(MI_GMC) != null) {
						matched = miExtras.get(MI_GMC).matcher(value).matches();
					}
				}
			}

			return matched;
		}
	}

	public boolean isDVDVideoRemuxSupported() {
		return match(MPEGPS, MPEG2, null) != null;
	}

	public boolean isFormatSupported(String container) {
		return match(container, null, null) != null;
	}

	public boolean isDTSSupported() {
		return match(MPEGPS, null, DTS) != null || match(MPEGTS, null, DTS) != null;
	}

	public boolean isLPCMSupported() {
		return match(MPEGPS, null, LPCM) != null || match(MPEGTS, null, LPCM) != null;
	}

	public boolean isMpeg2Supported() {
		return match(MPEGPS, MPEG2, null) != null || match(MPEGTS, MPEG2, null) != null;
	}

	public boolean isHiFiMusicFileSupported() {
		return match(WAV, null, null, 0, 96000, 0, 0, 0, null) != null || match(MP3, null, null, 0, 96000, 0, 0, 0, null) != null;
	}

	public String getPrimaryVideoTranscoder() {
		for (Supports conf : list) {
			if (conf.match(MPEGPS, MPEG2, AC3)) {
				return MPEGPS;
			}
			if (conf.match(MPEGTS, MPEG2, AC3)) {
				return MPEGTS;
			}
			if (conf.match(WMV, WMV, WMA)) {
				return WMV;
			}
		}
		return null;
	}

	public String getPrimaryAudioTranscoder() {
		for (Supports conf : list) {
			if (conf.match(WAV, null, null)) {
				return WAV;
			}
			if (conf.match(MP3, null, null)) {
				return MP3;
			}
		}
		return null;
	}

	public FormatConfiguration(List<?> supported) {
		list = new ArrayList<Supports>();
		for (Object line : supported) {
			if (line != null) {
				Supports conf = parseSupportLine(line.toString());
				if (conf.isValid()) {
					list.add(conf);
				} else {
					LOGGER.info("Invalid configuration line: " + line);
				}
			}
		}
	}

	/**
	 * Match a media information to available audio codes for the renderer
	 * and return its mimetype if the match is successful. Returns null if
	 * the media is not natively supported by the renderer, which means it
	 * has to be transcoded.
	 * @param media The media information
	 * @return The mimetype or null if no match was found.
	 */
	public String match(DLNAMediaInfo media) {
		if (media.getFirstAudioTrack() == null) {
			// no sound
			return match(media.getContainer(), media.getCodecV(), null, 0, 0, media.getBitrate(), media.getWidth(), media.getHeight(), media.getExtras());
		} else {
			String finalMimeType = null;
			for (DLNAMediaAudio audio : media.getAudioCodes()) {
				String mimeType = match(media.getContainer(), media.getCodecV(), audio.getCodecA(), audio.getNrAudioChannels(), audio.getSampleRate(), media.getBitrate(), media.getWidth(), media.getHeight(), media.getExtras());
				finalMimeType = mimeType;
				if (mimeType == null) // if at least one audio track is not compatible, the file must be transcoded.
				{
					return null;
				}
			}
			return finalMimeType;
		}
	}

	public String match(String container, String videocodec, String audiocodec) {
		return match(container, videocodec, audiocodec, 0, 0, 0, 0, 0, null);
	}

	public String match(String container, String videocodec, String audiocodec, int nbAudioChannels, int frequency, int bitrate, int videowidth, int videoheight, Map<String, String> extras) {
		String matchedMimeType = null;

		for (Supports conf : list) {
			if (conf.match(container, videocodec, audiocodec, nbAudioChannels, frequency, bitrate, videowidth, videoheight, extras)) {
				matchedMimeType = conf.mimetype;
				break;
			}
		}

		return matchedMimeType;
	}

	private Supports parseSupportLine(String line) {
		StringTokenizer st = new StringTokenizer(line, "\t ");
		Supports conf = new Supports();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.startsWith("f:")) {
				conf.format = token.substring(2).trim();
			} else if (token.startsWith("v:")) {
				conf.videocodec = token.substring(2).trim();
			} else if (token.startsWith("a:")) {
				conf.audiocodec = token.substring(2).trim();
			} else if (token.startsWith("n:")) {
				conf.maxnbchannels = token.substring(2).trim();
			} else if (token.startsWith("s:")) {
				conf.maxfrequency = token.substring(2).trim();
			} else if (token.startsWith("w:")) {
				conf.maxvideowidth = token.substring(2).trim();
			} else if (token.startsWith("h:")) {
				conf.maxvideoheight = token.substring(2).trim();
			} else if (token.startsWith("m:")) {
				conf.mimetype = token.substring(2).trim();
			} else if (token.startsWith("b:")) {
				conf.maxbitrate = token.substring(2).trim();
			} else if (token.contains(":")) {
				// extra mediainfo stuff
				if (conf.miExtras == null) {
					conf.miExtras = new HashMap<String, Pattern>();
				}
				String key = token.substring(0, token.indexOf(":"));
				String value = token.substring(token.indexOf(":") + 1);
				conf.miExtras.put(key, Pattern.compile(value));
			}
		}
		return conf;
	}
}
