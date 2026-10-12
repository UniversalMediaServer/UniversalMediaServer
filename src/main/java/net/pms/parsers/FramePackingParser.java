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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the 3D layout of a video that signals it in the stream.
 *
 * MediaInfo reports the layout from the container, for example Matroska's
 * {@code StereoMode}, but not the frame packing arrangement SEI that H.264
 * and H.265 carry in the stream (ITU-T H.264 section D.2.26, H.265 section
 * D.3.16), which is how the 3D files of Blu-ray rips and cameras mark it.
 * FFmpeg's decoders turn the SEI into Stereo 3D side data, and its
 * {@code showinfo} filter prints it for the first frame.
 */
public final class FramePackingParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(FramePackingParser.class);

	// The shortest video that can hold two half-resolution views
	private static final int MINIMUM_WIDTH = 1280;
	private static final long TIMEOUT_MS = 5000;
	private static final Pattern STEREO_3D = Pattern.compile(
		"Stereo 3D: type - (side by side|top and bottom)( \\(inverted\\))?, view - packed"
	);

	/**
	 * This class is not meant to be instantiated.
	 */
	private FramePackingParser() {
	}

	/**
	 * Sets the 3D layout of the default video track from the frame packing
	 * arrangement SEI of the first frame, when the track has none yet.
	 *
	 * @param file the video file
	 * @param media the parsed media of the file
	 */
	public static void parse(File file, MediaInfo media) {
		MediaVideo video = media.getDefaultVideoTrack();
		if (
			video == null ||
			video.is3d() ||
			(!video.isH264() && !video.isH265()) ||
			video.getWidth() < MINIMUM_WIDTH
		) {
			return;
		}

		String ffmpegPath = PMS.getConfiguration().getFFmpegPath();
		if (ffmpegPath == null) {
			return;
		}

		String output = ProcessUtil.runWithTimeout(
			TIMEOUT_MS,
			ffmpegPath,
			"-hide_banner",
			"-nostdin",
			"-loglevel", "info",
			"-i", file.getAbsolutePath(),
			"-an", "-sn",
			"-frames:v", "1",
			"-vf", "showinfo",
			"-f", "null",
			"-"
		);
		String layout = getMultiViewLayout(output, video.getWidth(), video.getHeight());
		if (layout != null) {
			LOGGER.debug("3D format \"{}\" found in the frame packing arrangement SEI of {}", layout, file.getName());
			video.setMultiViewLayout(layout);
		}
	}

	/**
	 * Returns the multi-view layout of the Stereo 3D side data in the output
	 * of FFmpeg's {@code showinfo} filter, in the words of MediaInfo.
	 *
	 * Half or full resolution is not in the SEI, so it is told from the size
	 * of the picture the way it is from the file name: a side by side video
	 * wider than 1920 and a top and bottom video higher than 1080 are full.
	 *
	 * @param showinfoOutput the log of FFmpeg
	 * @param width the width of the video
	 * @param height the height of the video
	 * @return the layout, or {@code null} if the first frame is not
	 *         side by side or top and bottom
	 */
	public static String getMultiViewLayout(String showinfoOutput, int width, int height) {
		if (showinfoOutput == null) {
			return null;
		}

		Matcher matcher = STEREO_3D.matcher(showinfoOutput);
		if (!matcher.find()) {
			return null;
		}

		String eye = matcher.group(2) == null ? "left" : "right";
		if (matcher.group(1).startsWith("side")) {
			return (width > 1920 ? "" : "half ") + "side by side (" + eye + " eye first)";
		}

		return (height > 1080 ? "" : "half ") + "top-bottom (" + eye + " eye first)";
	}
}
