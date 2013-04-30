package net.pms.configuration;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.dlna.LibMediaInfoParser;
import net.pms.formats.Format;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormatConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatConfiguration.class);
	private ArrayList<SupportSpec> supportSpecs;
	// Use old parser for JPEG files (MediaInfo does not support EXIF)
	private static final String[] PARSER_V1_EXTENSIONS = new String[]{".jpg", ".jpe", ".jpeg"};
	public static final String AAC = "aac";
	public static final String AC3 = "ac3";
	public static final String AIFF = "aiff";
	public static final String ALAC = "alac";
	public static final String APE = "ape";
	public static final String ATRAC = "atrac";
	public static final String AVI = "avi";
	public static final String BMP = "bmp";
	public static final String DIVX = "divx";
	public static final String DTS = "dts";
	public static final String DTSHD = "dtshd";
	public static final String DV = "dv";
	public static final String EAC3 = "eac3";
	public static final String FLAC = "flac";
	public static final String FLV = "flv";
	public static final String GIF = "gif";
	public static final String H264 = "h264";
	public static final String JPG = "jpg";
	public static final String LPCM = "lpcm";
	public static final String MATROSKA = "mkv";
	public static final String MI_GMC = "gmc";
	public static final String MI_QPEL = "qpel";
	public static final String MJPEG = "mjpeg";
	public static final String MLP = "mlp";
	public static final String MOV = "mov";
	public static final String MP3 = "mp3";
	public static final String MP4 = "mp4";
	public static final String MPA = "mpa";
	public static final String MPC = "mpc";
	public static final String MPEG1 = "mpeg1";
	public static final String MPEG2 = "mpeg2";
	public static final String MPEGPS = "mpegps";
	public static final String MPEGTS = "mpegts";
	public static final String OGG = "ogg";
	public static final String PNG = "png";
	public static final String RA = "ra";
	public static final String RM = "rm";
	public static final String SHORTEN = "shn";
	public static final String TIFF = "tiff";
	public static final String TRUEHD = "truehd";
	public static final String VC1 = "vc1";
	public static final String WAVPACK = "wavpack";
	public static final String WAV = "wav";
	public static final String WEBM = "WebM";
	public static final String WMA = "wma";
	public static final String WMV = "wmv";
	public static final String MIMETYPE_AUTO = "MIMETYPE_AUTO";
	public static final String und = "und";

	private class SupportSpec {
		private int iMaxBitrate = Integer.MAX_VALUE;
		private int iMaxFrequency = Integer.MAX_VALUE;
		private int iMaxNbChannels = Integer.MAX_VALUE;
		private int iMaxVideoHeight = Integer.MAX_VALUE;
		private int iMaxVideoWidth = Integer.MAX_VALUE;
		private Map<String, Pattern> miExtras;
		private Pattern pAudioCodec;
		private Pattern pFormat;
		private Pattern pVideoCodec;
		private String audioCodec;
		private String format;
		private String maxBitrate;
		private String maxFrequency;
		private String maxNbChannels;
		private String maxVideoHeight;
		private String maxVideoWidth;
		private String mimeType;
		private String videoCodec;

		SupportSpec() {
			this.mimeType = MIMETYPE_AUTO;
		}

		boolean isValid() {
			if (StringUtils.isBlank(format)) { // required
				LOGGER.warn("No format supplied");
				return false;
			} else {
				try {
					pFormat = Pattern.compile(format);
				} catch (PatternSyntaxException pse) {
					LOGGER.error("Error parsing format: " + format, pse);
					return false;
				}
			}

			if (videoCodec != null) {
				try {
					pVideoCodec = Pattern.compile(videoCodec);
				} catch (PatternSyntaxException pse) {
					LOGGER.error("Error parsing video codec: " + videoCodec, pse);
					return false;
				}
			}

			if (audioCodec != null) {
				try {
					pAudioCodec = Pattern.compile(audioCodec);
				} catch (PatternSyntaxException pse) {
					LOGGER.error("Error parsing audio codec: " + audioCodec, pse);
					return false;
				}
			}

			if (maxNbChannels != null) {
				try {
					iMaxNbChannels = Integer.parseInt(maxNbChannels);
				} catch (NumberFormatException nfe) {
					LOGGER.error("Error parsing number of channels: " + maxNbChannels, nfe);
					return false;
				}
			}

			if (maxFrequency != null) {
				try {
					iMaxFrequency = Integer.parseInt(maxFrequency);
				} catch (NumberFormatException nfe) {
					LOGGER.error("Error parsing maximum frequency: " + maxFrequency, nfe);
					return false;
				}
			}

			if (maxBitrate != null) {
				try {
					iMaxBitrate = Integer.parseInt(maxBitrate);
				} catch (NumberFormatException nfe) {
					LOGGER.error("Error parsing maximum bitrate: " + maxBitrate, nfe);
					return false;
				}
			}

			if (maxVideoWidth != null) {
				try {
					iMaxVideoWidth = Integer.parseInt(maxVideoWidth);
				} catch (Exception nfe) {
					LOGGER.error("Error parsing maximum video width: " + maxVideoWidth, nfe);
					return false;
				}
			}

			if (maxVideoHeight != null) {
				try {
					iMaxVideoHeight = Integer.parseInt(maxVideoHeight);
				} catch (NumberFormatException nfe) {
					LOGGER.error("Error parsing maximum video height: " + maxVideoHeight, nfe);
					return false;
				}
			}

			return true;
		}

		public boolean match(String container, String videoCodec, String audioCodec) {
			return match(container, videoCodec, audioCodec, 0, 0, 0, 0, 0, null);
		}

		public boolean match(
			String format,
			String videoCodec,
			String audioCodec,
			int nbAudioChannels,
			int frequency,
			int bitrate,
			int videoWidth,
			int videoHeight,
			Map<String, String> extras) {
			boolean matched = false;

			if (format != null && !(matched = pFormat.matcher(format).matches())) {
				return false;
			}

			if (matched && videoCodec != null && pVideoCodec != null && !(matched = pVideoCodec.matcher(videoCodec).matches())) {
				return false;
			}

			if (matched && audioCodec != null && pAudioCodec != null && !(matched = pAudioCodec.matcher(audioCodec).matches())) {
				return false;
			}

			if (matched && nbAudioChannels > 0 && iMaxNbChannels > 0 && nbAudioChannels > iMaxNbChannels) {
				return false;
			}

			if (matched && frequency > 0 && iMaxFrequency > 0 && frequency > iMaxFrequency) {
				return false;
			}

			if (matched && bitrate > 0 && iMaxBitrate > 0 && bitrate > iMaxBitrate) {
				return false;
			}

			if (matched && videoWidth > 0 && iMaxVideoWidth > 0 && videoWidth > iMaxVideoWidth) {
				return false;
			}

			if (matched && videoHeight > 0 && iMaxVideoHeight > 0 && videoHeight > iMaxVideoHeight) {
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

	public FormatConfiguration(List<?> lines) {
		supportSpecs = new ArrayList<SupportSpec>();

		for (Object line : lines) {
			if (line != null) {
				SupportSpec supportSpec = parseSupportLine(line.toString());

				if (supportSpec.isValid()) {
					supportSpecs.add(supportSpec);
				} else {
					LOGGER.warn("Invalid configuration line: " + line);
				}
			}
		}
	}

	public void parse(DLNAMediaInfo media, InputFile file, Format ext, int type) {
		boolean forceV1 = false;

		if (file.getFile() != null) {
			String fName = file.getFile().getName().toLowerCase();

			for (String e : PARSER_V1_EXTENSIONS) {
				if (fName.endsWith(e)) {
					forceV1 = true;
					break;
				}
			}

			if (forceV1) {
				// XXX this path generates thumbnails
				media.parse(file, ext, type, false);
			} else {
				// XXX this path doesn't generate thumbnails
				LibMediaInfoParser.parse(media, file, type);
			}
		} else {
			media.parse(file, ext, type, false);
		}
	}

	// XXX Unused
	@Deprecated
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

	// XXX Unused
	@Deprecated
	public boolean isHiFiMusicFileSupported() {
		return match(WAV, null, null, 0, 96000, 0, 0, 0, null) != null || match(MP3, null, null, 0, 96000, 0, 0, 0, null) != null;
	}

	public String getPrimaryVideoTranscoder() {
		for (SupportSpec supportSpec : supportSpecs) {
			if (supportSpec.match(MPEGPS, MPEG2, AC3)) {
				return MPEGPS;
			}

			if (supportSpec.match(MPEGTS, MPEG2, AC3)) {
				return MPEGTS;
			}

			if (supportSpec.match(WMV, WMV, WMA)) {
				return WMV;
			}
		}

		return null;
	}

	// XXX Unused
	@Deprecated
	public String getPrimaryAudioTranscoder() {
		for (SupportSpec supportSpec : supportSpecs) {
			if (supportSpec.match(WAV, null, null)) {
				return WAV;
			}

			if (supportSpec.match(MP3, null, null)) {
				return MP3;
			}

			// FIXME LPCM?
		}

		return null;
	}

	/**
	 * Match media information to audio codecs supported by the renderer and
	 * return its MIME-type if the match is successful. Returns null if the
	 * media is not natively supported by the renderer, which means it has
	 * to be transcoded.
	 *
	 * @param media The MediaInfo metadata
	 * @return The MIME type or null if no match was found.
	 */
	public String match(DLNAMediaInfo media) {
		if (media.getFirstAudioTrack() == null) {
			// no sound
			return match(
				media.getContainer(),
				media.getCodecV(),
				null,
				0,
				0,
				media.getBitrate(),
				media.getWidth(),
				media.getHeight(),
				media.getExtras()
			);
		} else {
			String finalMimeType = null;

			for (DLNAMediaAudio audio : media.getAudioTracksList()) {
				String mimeType = match(
					media.getContainer(),
					media.getCodecV(),
					audio.getCodecA(),
					audio.getAudioProperties().getNumberOfChannels(),
					audio.getSampleRate(),
					media.getBitrate(),
					media.getWidth(),
					media.getHeight(),
					media.getExtras()
				);

				finalMimeType = mimeType;

				if (mimeType == null) { // if at least one audio track is not compatible, the file must be transcoded.
					return null;
				}
			}

			return finalMimeType;
		}
	}

	public String match(String container, String videoCodec, String audioCodec) {
		return match(
			container,
			videoCodec,
			audioCodec,
			0,
			0,
			0,
			0,
			0,
			null
		);
	}

	public String match(
		String container,
		String videoCodec,
		String audioCodec,
		int nbAudioChannels,
		int frequency,
		int bitrate,
		int videoWidth,
		int videoHeight,
		Map<String, String> extras
	) {
		String matchedMimeType = null;

		for (SupportSpec supportSpec : supportSpecs) {
			if (supportSpec.match(
				container,
				videoCodec,
				audioCodec,
				nbAudioChannels,
				frequency,
				bitrate,
				videoWidth,
				videoHeight,
				extras)
			) {
				matchedMimeType = supportSpec.mimeType;
				break;
			}
		}

		return matchedMimeType;
	}

	private SupportSpec parseSupportLine(String line) {
		StringTokenizer st = new StringTokenizer(line, "\t ");
		SupportSpec supportSpec = new SupportSpec();

		while (st.hasMoreTokens()) {
			String token = st.nextToken();

			if (token.startsWith("f:")) {
				supportSpec.format = token.substring(2).trim();
			} else if (token.startsWith("v:")) {
				supportSpec.videoCodec = token.substring(2).trim();
			} else if (token.startsWith("a:")) {
				supportSpec.audioCodec = token.substring(2).trim();
			} else if (token.startsWith("n:")) {
				supportSpec.maxNbChannels = token.substring(2).trim();
			} else if (token.startsWith("s:")) {
				supportSpec.maxFrequency = token.substring(2).trim();
			} else if (token.startsWith("w:")) {
				supportSpec.maxVideoWidth = token.substring(2).trim();
			} else if (token.startsWith("h:")) {
				supportSpec.maxVideoHeight = token.substring(2).trim();
			} else if (token.startsWith("m:")) {
				supportSpec.mimeType = token.substring(2).trim();
			} else if (token.startsWith("b:")) {
				supportSpec.maxBitrate = token.substring(2).trim();
			} else if (token.contains(":")) {
				// Extra MediaInfo stuff
				if (supportSpec.miExtras == null) {
					supportSpec.miExtras = new HashMap<String, Pattern>();
				}

				String key = token.substring(0, token.indexOf(":"));
				String value = token.substring(token.indexOf(":") + 1);
				supportSpec.miExtras.put(key, Pattern.compile(value));
			}
		}

		return supportSpec;
	}
}
