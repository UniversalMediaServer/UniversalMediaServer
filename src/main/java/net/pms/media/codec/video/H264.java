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
package net.pms.media.codec.video;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advanced Video Coding (AVC).
 *
 * Also referred to as H.264 or MPEG-4 Part 10
 *
 * @author Surf@ceS
 */
public class H264 {

	private static final Map<Double, H264> H264_LEVEL = new LinkedHashMap<>();

	static {
		H264_LEVEL.put(1D, new H264(1, 1485, 99, 64, 396));
		H264_LEVEL.put(1.1, new H264(1.1, 3000, 396, 192, 900));
		H264_LEVEL.put(1.2, new H264(1.2, 6000, 396, 384, 2376));
		H264_LEVEL.put(1.3, new H264(1.3, 11880, 396, 768, 2376));
		H264_LEVEL.put(2D, new H264(2, 11880, 396, 2000, 2376));
		H264_LEVEL.put(2.1, new H264(2.1, 19800, 792, 4000, 4752));
		H264_LEVEL.put(2.2, new H264(2.2, 20250, 1620, 4000, 8100));
		H264_LEVEL.put(3D, new H264(3, 40500, 1620, 10000, 8100));
		H264_LEVEL.put(3.1, new H264(3.1, 108000, 3600, 14000, 18000));
		H264_LEVEL.put(3.2, new H264(3.2, 216000, 5120, 20000, 20480));
		H264_LEVEL.put(4D, new H264(4, 245760, 8192, 20000, 32768));
		H264_LEVEL.put(4.1, new H264(4.1, 245760, 8192, 50000, 32768));
		H264_LEVEL.put(4.2, new H264(4.2, 522240, 8704, 50000, 34816));
		H264_LEVEL.put(5D, new H264(5, 589824, 22080, 135000, 110400));
		H264_LEVEL.put(5.1, new H264(5.1, 983040, 36864, 240000, 184320));
		H264_LEVEL.put(5.2, new H264(5.2, 2073600, 36864, 240000, 184320));
		H264_LEVEL.put(6D, new H264(6, 4177920, 139264, 240000, 696320));
		H264_LEVEL.put(6.1, new H264(6.1, 8355840, 139264, 480000, 696320));
		H264_LEVEL.put(6.2, new H264(6.2, 16711680, 139264, 800000, 696320));
	}

	public final double level;
	/**
	 * Maximum decoding speed in macroblocks/s
	 */
	public final int maximumDecodingSpeed;
	/**
	 * Maximum frame size in macroblocks
	 */
	public final int maximumFrameSize;
	/**
	 * Maximum video bit rate for video coding layer (VCL) in kbits/s
	 *
	 * (Constrained Baseline, Baseline, Extended and Main Profiles)
	 */
	public final int maximumVideoBitRate;
	/**
	 * Maximum decoded picture buffer size in units of macroblocks
	 *
	 * (Constrained Baseline, Baseline, Extended and Main Profiles)
	 */
	public final int maximumDpbMbs;

	public H264(double level, int maximumDecodingSpeed, int maximumFrameSize, int maximumVideoBitRate, int maximumDpbMbs) {
		this.level = level;
		this.maximumDecodingSpeed = maximumDecodingSpeed;
		this.maximumFrameSize = maximumFrameSize;
		this.maximumVideoBitRate = maximumVideoBitRate;
		this.maximumDpbMbs = maximumDpbMbs;
	}

	public static int getMaximumStoredFrames(double level, int width, int height) {
		if (!H264_LEVEL.containsKey(level)) {
			return 0;
		}
		int maxDpbMbs = H264_LEVEL.get(level).maximumDpbMbs;
		int picWidthInMbs = (int) Math.ceil(width / 16D);
		int frameHeightInMbs = (int) Math.ceil(height / 16D);
		return Math.min((int) Math.floor(maxDpbMbs / (double) (picWidthInMbs * frameHeightInMbs)), 16);
	}

	private static int getAvcProfileId(String profile) {
		if (profile == null) {
			return 255;
		}
		return switch (profile.toLowerCase()) {
			case "cavlc 4:4:4 intra" -> 44;
			case "baseline" -> 66;
			case "main" -> 77;
			case "scalable baseline" -> 83;
			case "scalable high" -> 86;
			case "extended" -> 88;
			case "high" -> 100;
			case "high 10" -> 110;
			case "multiview high" -> 118;
			case "high 4:2:2" -> 122;
			case "stereo high" -> 128;
			case "multiview depth high" -> 138;
			case "high 4:4:4" -> 144;
			case "high 4:4:4 predictive" -> 244;
			default -> 255;
		};
	}

	public static String getAvcProfileHex(String profile) {
		int h264ProfileId = H264.getAvcProfileId(profile);
		if (h264ProfileId != 255) {
			return String.format("%02X", h264ProfileId) + "00";
		}
		return null;
	}

	public static String getAvcLevelHex(double level) {
		int h264IntLevel = (int) (level * 10);
		return String.format("%02X", h264IntLevel);
	}

}
