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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.pms.dlna.DLNAResource;

/**
 * HlsConfiguration Helper.
 *
 * video :
 * avc1.XXXXYY			-> h264			XXXX -> profile (42001 = main, 4D40 = main, 6400 = high) and YY -> hex of level
 * profiles hex :
 * 4200 -> Baseline
 * 4240 -> Constrained Baseline
 * 4D00 -> Main
 * 4D40 -> Constrained main
 * 5800 -> Extended
 * 6400 -> High
 * 6408 -> High Progressive
 * 6E00 -> High 10
 * 6E10 -> High 10 Intra
 * 7A00 -> High 4:2:2
 *
 * hvc1.X.4.LYYY.B01	-> h265			X -> profile		YYY -> level
 *
 * audio :
 * mp4a.40.2			-> AAC-LC
 * mp4a.40.5			-> HE-AAC
 * mp4a.40.29			-> HE-AACv2
 * mp4a.40.33			-> MP2
 * mp4a.40.34			-> MP3
 * ac-3					-> AC3
 * ec-3					-> EAC3
 */
public enum HlsConfiguration {
	ULD("ULD", "Ultra-Low Definition", 426, 240, 25, 350000, "avc1.4D401e", 350000, "mp4a.40.2", 128000),
	LD("LD", "Low Definition", 640, 360, 25, 800000, "avc1.4D401e", 600000, "mp4a.40.2", 128000),
	SD("SD", "Standard Definition", 842, 480, 25, 1400000, "avc1.64001e", 1200000, "mp4a.40.2", 160000),
	HD("HD", "High Definition", 1280, 720, 25, 2800000, "avc1.64001e", 2400000, "mp4a.40.2", 160000),
	FHD("FHD", "Full High Definition", 1920, 1080, 25, 5000000, "avc1.64001e", 4800000, "mp4a.40.2", 192000),
	QHD("QHD", "Quad High Definition", 2560, 1440, 0, 8000000, "avc1.64001e", 9600000, "mp4a.40.2", 192000),
	UHD1("UHD1", "Ultra High Definition 4K", 3840, 2160, 25, 18000000, "avc1.64001e", 16000000, "mp4a.40.2", 192000),
	UHD2("UHD2", "Ultra High Definition 8K", 7680, 4320, 25, 72000000, "avc1.64001e", 64000000, "mp4a.40.2", 192000);

	public final String label;
	public final String description;
	public final int resolutionWidth;
	public final int resolutionHeight;
	public final int framesPerSecond;
	public final int bandwidth;
	public final String videoCodec;
	public final int videoBitRate;
	public final String audioCodec;
	public final int audioBitRate;
	private static final Map<String, HlsConfiguration> BY_LABEL = new LinkedHashMap<>();
	static {
		for (HlsConfiguration e: values()) {
			BY_LABEL.put(e.label, e);
		}
	}

	private HlsConfiguration(String label, String description, int resolutionWidth, int resolutionHeight, int framesPerSecond, int bandwidth, String videoCodec, int videoBitRate, String audioCodec, int audioBitRate) {
		this.label = label;
		this.description = description;
		this.resolutionWidth = resolutionWidth;
		this.resolutionHeight = resolutionHeight;
		this.framesPerSecond = framesPerSecond;
		this.bandwidth = bandwidth;
		this.videoCodec = videoCodec;
		this.videoBitRate = videoBitRate;
		this.audioCodec = audioCodec;
		this.audioBitRate = audioBitRate;
	}

	@Override
	public String toString() {
		return "BANDWIDTH=" + bandwidth + ",RESOLUTION=" + resolutionWidth + "x" + resolutionHeight + ",CODECS=\"" + videoCodec + "," + audioCodec + "\"";
	}

	public static HlsConfiguration getByKey(String label) {
		return BY_LABEL.get(label);
	}

	public static String[] getKeys() {
		return BY_LABEL.keySet().toArray(new String[0]);
	}

	public static HlsConfiguration[] getValues() {
		return BY_LABEL.values().toArray(new HlsConfiguration[0]);
	}

	public static String getHLSm3u8(DLNAResource dlna, String baseUrl) {
		if (dlna.getMedia() != null) {
			// add 5% to handle cropped borders
			int maxHeight = (int) (dlna.getMedia().getHeight() * 1.05);
			String id = dlna.getResourceId();
			StringBuilder sb = new StringBuilder();
			sb.append("#EXTM3U\n");
			sb.append("#EXT-X-VERSION:3\n");
			//HlsConfiguration was ordered by Height
			boolean atLeastOneAdded = false;
			for (HlsConfiguration hlsConf : HlsConfiguration.getValues()) {
				if (maxHeight >= hlsConf.resolutionHeight || !atLeastOneAdded) {
					sb.append("#EXT-X-STREAM-INF:").append(hlsConf.toString()).append("\n");
					sb.append(baseUrl).append(id).append("/hls/").append(hlsConf.label).append(".m3u8\n");
					atLeastOneAdded = true;
				}
			}
			return sb.toString();
		}
		return null;
	}

	public static final double DEFAULT_TARGETDURATION = 6;
	public static String getHLSm3u8ForRendition(DLNAResource dlna, String baseUrl, String rendition) {
		if (dlna.getMedia() != null) {
			Double duration = dlna.getMedia().getDuration();
			double partLen = duration;
			String id = dlna.getResourceId();
			String defaultDurationStr = String.format(Locale.ENGLISH, "%.6f", DEFAULT_TARGETDURATION);
			String targetDurationStr = String.valueOf(Double.valueOf(Math.ceil(DEFAULT_TARGETDURATION)).intValue());
			StringBuilder sb = new StringBuilder();
			sb.append("#EXTM3U\n");
			sb.append("#EXT-X-VERSION:6\n");
			sb.append("#EXT-X-TARGETDURATION:").append(targetDurationStr).append("\n");
			sb.append("#EXT-X-MEDIA-SEQUENCE:0\n");
			sb.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
			sb.append("#EXT-X-INDEPENDENT-SEGMENTS\n");
			int partCount = 0;
			while (partLen > 0) {
				if (partLen >= DEFAULT_TARGETDURATION) {
					sb.append("#EXTINF:").append(defaultDurationStr).append(",\n");
				} else {
					sb.append("#EXTINF:").append(String.format(Locale.ENGLISH, "%.6f", partLen)).append(",\n");
				}
				sb.append(baseUrl).append(id).append("/hls/").append(rendition).append("/").append(partCount).append(".ts\n");
				partLen -= DEFAULT_TARGETDURATION;
				partCount++;
			}
			sb.append("#EXT-X-ENDLIST\n");
			return sb.toString();
		}
		return null;
	}

}
