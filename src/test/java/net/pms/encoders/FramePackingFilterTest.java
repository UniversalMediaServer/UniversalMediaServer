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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.media.video.MediaVideo.Mode3D;
import net.pms.parsers.FFmpegParser;
import net.pms.parsers.MediaInfoParser;
import net.pms.parsers.ParserTest;
import net.pms.util.ProcessUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FramePackingFilterTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FramePackingFilterTest.class);

	@TempDir
	File tempDir;

	@BeforeAll
	static void setUpClass() {
		ParserTest.setUpClass();
	}

	private static MediaVideo getVideo(File file) {
		MediaInfo mediaInfo = new MediaInfo();
		MediaInfoParser.parse(mediaInfo, file, Format.VIDEO);
		return mediaInfo.getDefaultVideoTrack();
	}

	/**
	 * Copies the video of a test file to MPEG-TS the way UMS does, and
	 * returns the layout UMS finds in the copy.
	 */
	private String getCopyLayout(String testFile) {
		String ffmpegPath = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		File file = ParserTest.getTestFile(testFile);
		String filter = FFMpegVideo.getFramePackingBitstreamFilter(getVideo(file), ffmpegPath);
		if (filter == null) {
			return null;
		}

		File copy = new File(tempDir, testFile + ".ts");
		String output = ProcessUtil.run(ffmpegPath, "-hide_banner", "-nostdin", "-y", "-i", file.getAbsolutePath(), "-map", "0:v", "-c:v", "copy", "-bsf:v", filter, "-f", "mpegts", copy.getAbsolutePath());
		assertTrue(copy.length() > 0, output);
		return getVideo(copy).getMultiViewLayout();
	}

	@Test
	void sideBySideKeepsTheEyeOrderAndIgnoresHalfOrFull() {
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBSL));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBS2L));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBSR));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBS2R));
	}

	@Test
	void topAndBottomKeepsTheEyeOrderAndIgnoresHalfOrFull() {
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.ABL));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.AB2L));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.ABR));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.AB2R));
	}

	@Test
	void everyOtherLayoutHasNoFrameOfItsOwn() {
		assertNull(FFMpegVideo.getFramePackingFilterOptions(null));
		for (Mode3D mode : Mode3D.values()) {
			if (!mode.name().startsWith("SBS") && !mode.name().startsWith("AB")) {
				assertNull(FFMpegVideo.getFramePackingFilterOptions(mode), mode.name());
			}
		}
	}

	@Test
	void copyKeepsTheLayoutOfTheContainer() {
		if (!MediaInfoParser.isValid() || !FFmpegParser.isValid()) {
			LOGGER.info("FramePackingFilter test on files skipped");
			return;
		}
		String ffmpegPath = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (!FFmpegOptions.isFramePackingSupported(ffmpegPath, "h264_metadata")) {
			LOGGER.info("FramePackingFilter test on files skipped, {} has no frame_packing option", ffmpegPath);
			return;
		}

		// Matroska's StereoMode does not survive the copy, the frame packing arrangement SEI does
		assertEquals("half side by side (left eye first)", getCopyLayout("video-h264_stereomode_side_by_side_left_first.mkv"));
		assertEquals("half top-bottom (right eye first)", getCopyLayout("video-h264_stereomode_top_bottom_right_first.mkv"));
		assertEquals("top-bottom (left eye first)", getCopyLayout("video-h265_stereomode_top_bottom_left_first.mkv"));
		assertEquals("side by side (right eye first)", getCopyLayout("video-h265_stereomode_side_by_side_right_first.mkv"));
	}
}
