/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

package net.pms.configuration;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.dlna.LibMediaInfoParser;
import net.pms.formats.Format;
import net.pms.formats.Format.Identifier;
import net.pms.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormatConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatConfiguration.class);
	private ArrayList<SupportSpec> supportSpecs;
	public static final String THREEGPP = "3gp";
	public static final String THREEGPP2 = "3g2";
	public static final String THREEGA = "3ga";
	public static final String AAC_LC = "aac-lc";
	public static final String AAC_LTP = "aac-ltp";
	public static final String AAC_MAIN = "aac-main";
	public static final String AAC_SSR = "aac-ssr";
	public static final String AC3 = "ac3";
	public static final String ACELP = "acelp";
	public static final String ADPCM = "adpcm";
	public static final String ADTS = "adts";
	public static final String AIFF = "aiff";
	public static final String ALAC = "alac";
	public static final String ALS = "als";
	public static final String AMR = "amr";
	public static final String ASF = "asf";
	public static final String ATMOS = "atmos";
	public static final String ATRAC = "atrac";
	public static final String AU = "au";
	public static final String AVI = "avi";
	public static final String BMP = "bmp";
	public static final String CAF = "caf";
	public static final String CELP = "celp";
	public static final String CINEPAK = "cvid";
	public static final String COOK = "cook";
	public static final String CUR = "cur";
	public static final String DIVX = "divx";
	/** Direct Stream Digital / Super Audio CD tracks */
	public static final String DFF = "dff";
	public static final String DSF = "dsf";
	public static final String DOLBYE = "dolbye";
	public static final String DTS = "dts";
	public static final String DTSHD = "dtshd";
	public static final String DV = "dv";
	public static final String EAC3 = "eac3";
	public static final String ER_BSAC = "erbsac";
	public static final String FFV1 = "ffv1";
	public static final String FLAC = "flac";
	public static final String FLV = "flv";
	public static final String G729 = "g729";
	public static final String GIF = "gif";
	public static final String H261 = "h261";
	public static final String H263 = "h263";
	public static final String H264 = "h264";
	public static final String H265 = "h265";
	public static final String HE_AAC = "he-aac";
	public static final String ICNS = "icns";
	public static final String ICO = "ico";
	public static final String INDEO = "indeo";
	public static final String ISO = "iso";
	public static final String JPG = "jpg";
	public static final String JPEG = "jpeg";
	public static final String JPEG2000 = "jpeg2000";
	public static final String LPCM = "lpcm";
	public static final String M4A = "m4a";
	public static final String MKV = "mkv";
	public static final String MI_GMC = "gmc";
	public static final String MI_GOP = "gop";
	public static final String MI_QPEL = "qpel";
	public static final String MACE3 = "mace3";
	public static final String MACE6 = "mace6";
	public static final String MJPEG = "mjpeg";
	public static final String MKA = "mka";
	public static final String MLP = "mlp";
	public static final String MONKEYS_AUDIO = "ape";
	public static final String MOV = "mov";
	public static final String MP2 = "mp2";
	public static final String MP3 = "mp3";
	public static final String MP4 = "mp4";
	public static final String MPA = "mpa";
	public static final String MPC = "mpc";
	public static final String MPEG1 = "mpeg1";
	public static final String MPEG2 = "mpeg2";
	public static final String MPEGPS = "mpegps";
	public static final String MPEGTS = "mpegts";
	public static final String NELLYMOSER = "nellymoser";
	public static final String OGG = "ogg";
	/** OGG container with only audio track */
	public static final String OGA = "oga";
	public static final String OPUS = "opus";
	public static final String PCX = "pcx";
	public static final String PNG = "png";
	public static final String PNM = "pnm";
	public static final String PSD = "psd";
	public static final String QCELP = "qcelp";
	public static final String QDESIGN = "qdmc";
	/** This is the RealAudio file format, not one of the codecs */
	public static final String RA = "ra";
	public static final String RALF = "ralf";
	public static final String RAW = "raw";
	public static final String REALAUDIO_14_4 = "ra14.4";
	public static final String REALAUDIO_28_8 = "ra28.8";
	/** Used as a "video codec" when sequences of raw, uncompressed RGB or RGBA is used as a video stream in AVI, MP4 or MOV files */
	public static final String RGB = "rgb";
	public static final String RLE = "rle";
	public static final String RM = "rm";
	public static final String SHORTEN = "shn";
	public static final String SIPRO = "sipro";
	public static final String SLS = "sls";
	public static final String SORENSON = "sor";
	public static final String TGA = "tga";
	public static final String THEORA = "theora";
	public static final String TIFF = "tiff";
	public static final String TRUEHD = "truehd";
	public static final String TTA = "tta";
	public static final String VC1 = "vc1";
	public static final String VORBIS = "vorbis";
	public static final String VP6 = "vp6";
	public static final String VP7 = "vp7";
	public static final String VP8 = "vp8";
	public static final String VP9 = "vp9";
	public static final String WAV = "wav";
	public static final String WAVPACK = "wavpack";
	public static final String WBMP = "wbmp";
	/** Webm with only one track */
	public static final String WEBA = "weba";
	public static final String WEBM = "webm";
	public static final String WEBP = "webp";
	public static final String WMA = "wma";
	public static final String WMA10 = "wma10";
	public static final String WMALOSSLESS = "wmalossless";
	public static final String WMAPRO = "wmapro";
	public static final String WMAVOICE = "wmavoice";
	public static final String WMV = "wmv";
	/** Used as a "video codec" when sequences of raw, uncompressed YUV is used as a video stream in AVI, MP4 or MOV files */
	public static final String YUV = "yuv";
	public static final String MIMETYPE_AUTO = "MIMETYPE_AUTO";
	public static final String und = "und";

	private static class SupportSpec {
		private int iMaxBitrate = Integer.MAX_VALUE;
		private int iMaxFramerate = Integer.MAX_VALUE;
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
		private String maxFramerate;
		private String maxFrequency;
		private String maxNbChannels;
		private String maxVideoHeight;
		private String maxVideoWidth;
		private String mimeType;
		private String videoCodec;
		private String supportLine;

		SupportSpec() {
			this.mimeType = MIMETYPE_AUTO;
		}

		boolean isValid() {
			if (isBlank(format)) { // required
				LOGGER.error("No format specified on line \"{}\"", supportLine);
				return false;
			}
			try {
				pFormat = Pattern.compile(format);
			} catch (PatternSyntaxException pse) {
				LOGGER.error(
					"Error parsing format \"{}\" from line \"{}\": {}",
					format,
					supportLine,
					pse.getMessage()
				);
				LOGGER.trace("", pse);
				return false;
			}

			if (isNotBlank(videoCodec)) {
				try {
					pVideoCodec = Pattern.compile(videoCodec);
				} catch (PatternSyntaxException pse) {
					LOGGER.error(
						"Error parsing video codec \"{}\" from line \"{}\": {}",
						videoCodec,
						supportLine,
						pse.getMessage()
					);
					LOGGER.trace("", pse);
					return false;
				}
			}

			if (isNotBlank(audioCodec)) {
				try {
					pAudioCodec = Pattern.compile(audioCodec);
				} catch (PatternSyntaxException pse) {
					LOGGER.error(
						"Error parsing audio codec \"{}\" from line \"{}\": {}",
						audioCodec,
						supportLine,
						pse.getMessage()
					);
					LOGGER.trace("", pse);
					return false;
				}
			}

			if (isNotBlank(maxNbChannels)) {
				try {
					iMaxNbChannels = Integer.parseInt(maxNbChannels);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing number of channels \"{}\" from line \"{}\": {}",
						maxNbChannels,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			if (isNotBlank(maxFramerate)) {
				try {
					iMaxFramerate = Integer.parseInt(maxFramerate);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing maximum framerate \"{}\" from line \"{}\": {}",
						maxFramerate,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			if (isNotBlank(maxFrequency)) {
				try {
					iMaxFrequency = Integer.parseInt(maxFrequency);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing maximum frequency \"{}\" from line \"{}\": {}",
						maxFrequency,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			if (isNotBlank(maxBitrate)) {
				try {
					iMaxBitrate = Integer.parseInt(maxBitrate);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing maximum bitrate \"{}\" from line \"{}\": {}",
						maxBitrate,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			if (isNotBlank(maxVideoWidth)) {
				try {
					iMaxVideoWidth = Integer.parseInt(maxVideoWidth);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing maximum video width \"{}\" from line \"{}\": {}",
						maxVideoWidth,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			if (isNotBlank(maxVideoHeight)) {
				try {
					iMaxVideoHeight = Integer.parseInt(maxVideoHeight);
				} catch (NumberFormatException nfe) {
					LOGGER.error(
						"Error parsing maximum video height \"{}\" from line \"{}\": {}",
						maxVideoHeight,
						supportLine,
						nfe.getMessage()
					);
					LOGGER.trace("", nfe);
					return false;
				}
			}

			return true;
		}

		/**
		 * Determine whether or not the provided parameters match the
		 * "Supported" lines for this configuration. If a parameter is null
		 * or 0, its value is skipped for making the match. If any of the
		 * non-null parameters does not match, false is returned. For example,
		 * assume a configuration that contains only the following line:
		 *
		 * Supported = f:mp4 n:2
		 *
		 * match("mp4", null, null, 2, 0, 0, 0, 0, 0, null) = true
		 * match("mp4", null, null, 6, 0, 0, 0, 0, 0, null) = false
		 * match("wav", null, null, 2, 0, 0, 0, 0, 0, null) = false
		 *
		 * @param format
		 * @param videoCodec
		 * @param audioCodec
		 * @param nbAudioChannels
		 * @param frequency
		 * @param bitrate
		 * @param framerate
		 * @param videoWidth
		 * @param videoHeight
		 * @param extras
		 * @return False if any of the provided non-null parameters is not a
		 * 			match, true otherwise.
		 */
		public boolean match(
			String format,
			String videoCodec,
			String audioCodec,
			int nbAudioChannels,
			int frequency,
			int bitrate,
			int framerate,
			int videoWidth,
			int videoHeight,
			Map<String, String> extras
		) {

			// Satisfy a minimum threshold
			if (format == null && videoCodec == null && audioCodec == null) {
				// We have no matchable info. This can happen with unparsed
				// mediainfo objects (e.g. from WEB.conf or plugins).
				return false;
			}

			// Assume a match, until proven otherwise
			if (format != null && !pFormat.matcher(format).matches()) {
				LOGGER.trace("Format \"{}\" failed to match supported line {}", format, supportLine);
				return false;
			}

			if (videoCodec != null && pVideoCodec != null && !pVideoCodec.matcher(videoCodec).matches()) {
				LOGGER.trace("Video codec \"{}\" failed to match support line {}", videoCodec, supportLine);
				return false;
			}

			if (audioCodec != null && pAudioCodec != null && !pAudioCodec.matcher(audioCodec).matches()) {
				LOGGER.trace("Audio codec \"{}\" failed to match support line {}", audioCodec, supportLine);
				return false;
			}

			if (nbAudioChannels > 0 && iMaxNbChannels > 0 && nbAudioChannels > iMaxNbChannels) {
				LOGGER.trace("Number of channels \"{}\" failed to match support line {}", nbAudioChannels, supportLine);
				return false;
			}

			if (framerate > 0 && iMaxFramerate > 0 && framerate > iMaxFramerate) {
				LOGGER.trace("Framerate \"{}\" failed to match support line {}", framerate, supportLine);
				return false;
			}

			if (frequency > 0 && iMaxFrequency > 0 && frequency > iMaxFrequency) {
				LOGGER.trace("Frequency \"{}\" failed to match support line {}", frequency, supportLine);
				return false;
			}

			if (bitrate > 0 && iMaxBitrate > 0 && bitrate > iMaxBitrate) {
				LOGGER.trace("Bit rate \"{}\" failed to match support line {}", bitrate, supportLine);
				return false;
			}

			if (videoWidth > 0 && iMaxVideoWidth > 0 && videoWidth > iMaxVideoWidth) {
				LOGGER.trace("Video width \"{}\" failed to match support line {}", videoWidth, supportLine);
				return false;
			}

			if (videoHeight > 0 && iMaxVideoHeight > 0 && videoHeight > iMaxVideoHeight) {
				LOGGER.trace("Video height \"{}\" failed to match support line {}", videoHeight, supportLine);
				return false;
			}

			if (extras != null && miExtras != null) {
				Iterator<Entry<String, String>> keyIt = extras.entrySet().iterator();
				while (keyIt.hasNext()) {
					String key = keyIt.next().getKey();
					String value = extras.get(key).toLowerCase();

					if (key.equals(MI_QPEL) && miExtras.get(MI_QPEL) != null && !miExtras.get(MI_QPEL).matcher(value).matches()) {
						LOGGER.trace("QPel value \"{}\" failed to match support line {}", miExtras.get(MI_QPEL), supportLine);
						return false;
					}

					if (key.equals(MI_GMC) && miExtras.get(MI_GMC) != null && !miExtras.get(MI_GMC).matcher(value).matches()) {
						LOGGER.trace("GMC value \"{}\" failed to match support line {}", miExtras.get(MI_GMC), supportLine);
						return false;
					}

					if (
						key.equals(MI_GOP) &&
						miExtras.get(MI_GOP) != null &&
						miExtras.get(MI_GOP).matcher("static").matches() &&
						value.equals("variable")
					) {
						LOGGER.trace("GOP value \"{}\" failed to match support line {}", value, supportLine);
						return false;
					}
				}
			}

			LOGGER.trace("Matched support line {}", supportLine);
			return true;
		}
	}

	public FormatConfiguration(List<?> lines) {
		supportSpecs = new ArrayList<>();

		for (Object line : lines) {
			if (line != null) {
				SupportSpec supportSpec = parseSupportLine(line.toString());

				if (supportSpec.isValid()) {
					supportSpecs.add(supportSpec);
				} else {
					LOGGER.warn("Invalid configuration line: {}", line);
				}
			}
		}
	}

	/**
	 * Chooses which parsing method to parse the file with.
	 */
	public void parse(DLNAMediaInfo media, InputFile file, Format ext, int type, RendererConfiguration renderer) {
		if (file.getFile() != null) {
			if (ext.getIdentifier() == Identifier.RA) {
				// Special parsing for RealAudio 1.0 and 2.0 which isn't handled by MediaInfo or JAudioTagger
				FileChannel channel;
				try {
					channel = FileChannel.open(file.getFile().toPath(), StandardOpenOption.READ);
					if (AudioUtils.parseRealAudio(channel, media)) {
						// If successful parsing is done, if not continue parsing the standard way
						media.postParse(type, file);
						return;
					}
				} catch (IOException e) {
					LOGGER.warn("An error occurred when trying to open \"{}\" for reading: {}", file, e.getMessage());
					LOGGER.trace("", e);
				}
			}

			// MediaInfo can't correctly parse ADPCM, DFF, DSF or PNM
			if (
				renderer.isUseMediaInfo() &&
				ext.getIdentifier() != Identifier.ADPCM &&
				ext.getIdentifier() != Identifier.DFF &&
				ext.getIdentifier() != Identifier.DSF &&
				ext.getIdentifier() != Identifier.PNM
			) {
				LibMediaInfoParser.parse(media, file, type, renderer);
			} else {
				media.parse(file, ext, type, false, false, renderer);
			}
		} else {
			media.parse(file, ext, type, false, false, renderer);
		}
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
		if (media == null) {
			return null;
		}
		int frameRate = 0;
		if (isNotBlank(media.getFrameRate())) {
			try {
				frameRate = (int) Math.round(Double.parseDouble(media.getFrameRate()));
			} catch (NumberFormatException e) {
				LOGGER.debug(
					"Could not parse framerate \"{}\" for media {}: {}",
					media.getFrameRate(),
					media,
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
		}
		if (media.getFirstAudioTrack() == null) {
			// no sound
			return match(
				media.getContainer(),
				media.getCodecV(),
				null,
				0,
				0,
				media.getBitrate(),
				frameRate,
				media.getWidth(),
				media.getHeight(),
				media.getExtras()
			);
		}

		if (media.isSLS()) {
			/*
			 * MPEG-4 SLS is a special case and must be treated differently. It
			 * consists of a MPEG-4 ISO container with two audio tracks, the
			 * first is the lossy "core" stream and the second is the SLS
			 * correction stream. When the SLS stream is applied to the core
			 * stream the result is lossless. It is arranged this way so that
			 * players that can't play SLS can still play the (lossy) core
			 * stream. Because of this, only compatibility for the first audio
			 * track needs to be checked.
			 */
			DLNAMediaAudio audio = media.getFirstAudioTrack();
			return match(
				media.getContainer(),
				media.getCodecV(),
				audio.getCodecA(),
				audio.getAudioProperties().getNumberOfChannels(),
				audio.getSampleRate(),
				audio.getBitRate(),
				frameRate,
				media.getWidth(),
				media.getHeight(),
				media.getExtras()
			);
		}

		String finalMimeType = null;

		for (DLNAMediaAudio audio : media.getAudioTracksList()) {
			String mimeType = match(
				media.getContainer(),
				media.getCodecV(),
				audio.getCodecA(),
				audio.getAudioProperties().getNumberOfChannels(),
				audio.getSampleRate(),
				media.getBitrate(),
				frameRate,
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
		int framerate,
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
				framerate,
				videoWidth,
				videoHeight,
				extras
			)) {
				matchedMimeType = supportSpec.mimeType;
				break;
			}
		}

		return matchedMimeType;
	}

	private static SupportSpec parseSupportLine(String line) {
		StringTokenizer st = new StringTokenizer(line, "\t ");
		SupportSpec supportSpec = new SupportSpec();

		supportSpec.supportLine = line;

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
			} else if (token.startsWith("fps:")) {
				supportSpec.maxFramerate = token.substring(4).trim();
			} else if (token.contains(":")) {
				// Extra MediaInfo stuff
				if (supportSpec.miExtras == null) {
					supportSpec.miExtras = new HashMap<>();
				}

				String key = token.substring(0, token.indexOf(':'));
				String value = token.substring(token.indexOf(':') + 1);
				supportSpec.miExtras.put(key, Pattern.compile(value));
			}
		}

		return supportSpec;
	}
}
