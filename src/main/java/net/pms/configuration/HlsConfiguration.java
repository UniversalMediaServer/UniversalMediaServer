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

public enum HlsConfiguration {
	ULD("ULD", "Ultra-Low Definition", 426, 240, 350000),		//Main
	LD("LD", "Low Definition", 640, 360, 800000),				//Main
	SD("SD", "Standard Definition", 842, 480, 1400000),			//High
	HD("HD", "High Definition", 1280, 720, 2800000),			//High
	FHD("FHD", "Full High Definition", 1920, 1080, 5000000),	//High
	QHD("QHD", "Quad High Definition", 2560, 1440, 8000000),
	UHD1("UHD1", "Ultra High Definition 4K", 3840, 2160, 18000000),
	UHD2("UHD2", "Ultra High Definition 8K", 7680, 4320, 72000000);

	public final String label;
	public final String description;
	public final int resolutionWidth;
	public final int resolutionHeight;
	public final int bandwidth;
	private static final Map<String, HlsConfiguration> BY_LABEL = new LinkedHashMap<>();
	static {
		for (HlsConfiguration e: values()) {
			BY_LABEL.put(e.label, e);
		}
	}

	private HlsConfiguration(String label, String description, int resolutionWidth, int resolutionHeight, int bandwidth) {
		this.label = label;
		this.description = description;
		this.resolutionWidth = resolutionWidth;
		this.resolutionHeight = resolutionHeight;
		this.bandwidth = bandwidth;
	}

	@Override
	public String toString() {
		return "BANDWIDTH=" + bandwidth + ",RESOLUTION=" + resolutionWidth + "x" + resolutionHeight;
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
			int maxHeight = dlna.getMedia().getHeight() + Double.valueOf(dlna.getMedia().getHeight() * 0.05).intValue();
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

	public static final double DEFAULT_TARGETDURATION = 10;
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
