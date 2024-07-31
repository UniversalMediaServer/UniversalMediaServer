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
package net.pms.encoders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.pms.configuration.FormatConfiguration;

/**
 * @author SurfaceS
 */
public class EncodingFormat {

	/**
	 * audio transcoding options.
	 */
	private static final String LPCM = "LPCM";
	private static final String MP3 = "MP3";
	private static final String WAV = "WAV";

	/**
	 * video transcoding options.
	 */
	private static final String MP4_H264_AAC = "MP4-H264-AAC";
	private static final String MP4_H265_AC3 = "MP4-H265-AC3";
	private static final String MPEGTS_H264_AAC = "MPEGTS-H264-AAC";
	private static final String MPEGTS_H264_AC3 = "MPEGTS-H264-AC3";
	private static final String MPEGTS_H265_AAC = "MPEGTS-H265-AAC";
	private static final String MPEGTS_H265_AC3 = "MPEGTS-H265-AC3";
	private static final String MPEGPS_MPEG2_AC3 = "MPEGPS-MPEG2-AC3";
	private static final String MPEGTS_MPEG2_AC3 = "MPEGTS-MPEG2-AC3";
	private static final String HLS_MPEGTS_H264_AAC = "HLS-MPEGTS-H264-AAC";
	private static final String HLS_MPEGTS_H264_AC3 = "HLS-MPEGTS-H264-AC3";
	private static final String WMV = "WMV";

	public static final Map<String, EncodingFormat> AUDIO = Map.ofEntries(
			Map.entry(LPCM, new EncodingFormat(LPCM, FormatConfiguration.LPCM, FormatConfiguration.LPCM)),
			Map.entry(MP3, new EncodingFormat(MP3, FormatConfiguration.MP3, FormatConfiguration.MP3)),
			Map.entry(WAV, new EncodingFormat(WAV, FormatConfiguration.WAV, FormatConfiguration.WAV))
	);

	public static final Map<String, EncodingFormat> VIDEO = Map.ofEntries(
			Map.entry(MP4_H264_AAC, new EncodingFormat(MP4_H264_AAC, FormatConfiguration.MP4, FormatConfiguration.H264, FormatConfiguration.AAC_LC)),
			Map.entry(MP4_H265_AC3, new EncodingFormat(MP4_H265_AC3, FormatConfiguration.MP4, FormatConfiguration.H265, FormatConfiguration.AC3)),
			Map.entry(MPEGTS_H264_AAC, new EncodingFormat(MPEGTS_H264_AAC, FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AAC_LC)),
			Map.entry(MPEGTS_H264_AC3, new EncodingFormat(MPEGTS_H264_AC3, FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AC3)),
			Map.entry(MPEGTS_H265_AAC, new EncodingFormat(MPEGTS_H265_AAC, FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AAC_LC)),
			Map.entry(MPEGTS_H265_AC3, new EncodingFormat(MPEGTS_H265_AC3, FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AC3)),
			Map.entry(MPEGPS_MPEG2_AC3, new EncodingFormat(MPEGPS_MPEG2_AC3, FormatConfiguration.MPEGPS, FormatConfiguration.MPEG2, FormatConfiguration.AC3)),
			Map.entry(MPEGTS_MPEG2_AC3, new EncodingFormat(MPEGTS_MPEG2_AC3, FormatConfiguration.MPEGTS, FormatConfiguration.MPEG2, FormatConfiguration.AC3)),
			Map.entry(HLS_MPEGTS_H264_AAC, new EncodingFormat(MPEGTS_H264_AAC, FormatConfiguration.MPEGTS_HLS, FormatConfiguration.H264, FormatConfiguration.AAC_LC)),
			Map.entry(HLS_MPEGTS_H264_AC3, new EncodingFormat(MPEGTS_H264_AAC, FormatConfiguration.MPEGTS_HLS, FormatConfiguration.H264, FormatConfiguration.AC3)),
			Map.entry(WMV, new EncodingFormat(WMV, FormatConfiguration.WMV, FormatConfiguration.WMV, FormatConfiguration.WMA))
	);

	private final String name;
	private final String container;
	private final String videoCodec;
	private final String audioCodec;

	public EncodingFormat(String name, String container, String audioCodec) {
		this(name, container, null, audioCodec);
	}

	public EncodingFormat(String name, String container, String videoCodec, String audioCodec) {
		this.name = name;
		this.container = container;
		this.videoCodec = videoCodec;
		this.audioCodec = audioCodec;
	}

	/**
	 * @return the container
	 */
	public String getTranscodingContainer() {
		return container;
	}

	public String getTranscodingVideoCodec() {
		return videoCodec;
	}

	public String getTranscodingAudioCodec() {
		return audioCodec;
	}

	public boolean isAudioFormat() {
		return audioCodec != null && videoCodec == null;
	}

	public boolean isVideoFormat() {
		return videoCodec != null;
	}

	public boolean isTranscodeToMP3() {
		return MP3.equals(name);
	}

	public boolean isTranscodeToLPCM() {
		return LPCM.equals(name);
	}

	public boolean isTranscodeToWAV() {
		return WAV.equals(name);
	}

	public boolean isTranscodeToWMV() {
		return WMV.equals(name);
	}

	public boolean isTranscodeToMP4H265AC3() {
		return MP4_H265_AC3.equals(name);
	}

	/**
	 * @return whether to use the AC-3 audio codec for transcoded video
	 */
	public boolean isTranscodeToAC3() {
		return FormatConfiguration.AC3.equals(getTranscodingAudioCodec());
	}

	/**
	 * @return whether to use the AAC audio codec for transcoded video
	 */
	public boolean isTranscodeToAAC() {
		return FormatConfiguration.AAC_LC.equals(getTranscodingAudioCodec());
	}

	/**
	 * @return whether to use the H.264 video codec for transcoded video
	 */
	public boolean isTranscodeToH264() {
		return FormatConfiguration.H264.equals(getTranscodingVideoCodec());
	}

	/**
	 * @return whether to use the H.265 video codec for transcoded video
	 */
	public boolean isTranscodeToH265() {
		return FormatConfiguration.H265.equals(getTranscodingVideoCodec());
	}

	/**
	 * @return whether to use the MPEG-2 video codec for transcoded video
	 */
	public boolean isTranscodeToMPEG2() {
		return FormatConfiguration.MPEG2.equals(getTranscodingVideoCodec());
	}

	/**
	 * @return whether to use the MPEG-TS container for transcoded video
	 */
	public boolean isTranscodeToMPEGTS() {
		return FormatConfiguration.MPEGTS.equals(getTranscodingContainer());
	}

	/**
	 * @return whether to use the MP4 container for transcoded video
	 */
	public boolean isTranscodeToMP4() {
		return FormatConfiguration.MP4.equals(getTranscodingContainer());
	}

	/**
	 * @return whether to use the HLS format for transcoded video
	 */
	public boolean isTranscodeToHLS() {
		return FormatConfiguration.MPEGTS_HLS.equals(getTranscodingContainer());
	}

	@Override
	public String toString() {
		return name;
	}

	public static EncodingFormat getAudioEncodingFormat(String encodingFormat) {
		if (encodingFormat == null || !AUDIO.containsKey(encodingFormat)) {
			return null;
		}
		return AUDIO.get(encodingFormat);
	}

	public static List<EncodingFormat> getVideoEncodingFormats(List<String> encodingFormats) {
		List<EncodingFormat> result = new ArrayList<>();
		if (encodingFormats != null && !encodingFormats.isEmpty()) {
			for (String encodingFormat : encodingFormats) {
				EncodingFormat format = getVideoEncodingFormat(encodingFormat);
				if (format != null) {
					result.add(format);
				}
			}
		}
		return result;
	}

	public static EncodingFormat getVideoEncodingFormat(String encodingFormat) {
		if (encodingFormat == null || !VIDEO.containsKey(encodingFormat)) {
			return null;
		}
		return VIDEO.get(encodingFormat);
	}

	public static EncodingFormat getEncodingFormat(String encodingFormat) {
		if (encodingFormat != null) {
			if (AUDIO.containsKey(encodingFormat)) {
				return AUDIO.get(encodingFormat);
			}
			if (VIDEO.containsKey(encodingFormat)) {
				return VIDEO.get(encodingFormat);
			}
		}
		return null;
	}

}
