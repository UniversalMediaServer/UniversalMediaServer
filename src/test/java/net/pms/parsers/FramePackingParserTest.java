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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.media.video.MediaVideo.Mode3D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FramePackingParserTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FramePackingParserTest.class);

	@BeforeAll
	static void setUpClass() {
		ParserTest.setUpClass();
	}

	private static MediaVideo getTestFileVideo(String testFile) {
		File file = ParserTest.getTestFile(testFile);
		Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
		MediaInfo mediaInfo = new MediaInfo();
		MediaInfoParser.parse(mediaInfo, file, format.getType());
		return mediaInfo.getDefaultVideoTrack();
	}

	private static void assertLayout(String multiViewLayout, Mode3D layout, String testFile) {
		MediaVideo video = getTestFileVideo(testFile);
		assertEquals(multiViewLayout, video.getMultiViewLayout(), testFile);
		assertEquals(layout, video.get3DLayout(), testFile);
	}

	private static void assertNot3d(String testFile) {
		assertFalse(getTestFileVideo(testFile).is3d(), testFile);
	}

	private static String showinfo(String stereo3d) {
		return "[Parsed_showinfo_0 @ 0x55d4c8a0a2c0] config in time_base: 1/25, frame_rate: 25/1\n" +
			"[Parsed_showinfo_0 @ 0x55d4c8a0a2c0] n:   0 pts:      0 pts_time:0 duration: 1\n" +
			"[Parsed_showinfo_0 @ 0x55d4c8a0a2c0]   side data - " + stereo3d + "\n";
	}

	@Test
	void sideBySideLeftFirst() {
		String output = showinfo("Stereo 3D: type - side by side, view - packed, primary_eye - none");
		assertEquals("side by side (left eye first)", FramePackingParser.getMultiViewLayout(output, 3840, 1080));
	}

	@Test
	void sideBySideRightFirst() {
		String output = showinfo("Stereo 3D: type - side by side (inverted), view - packed, primary_eye - none");
		assertEquals("side by side (right eye first)", FramePackingParser.getMultiViewLayout(output, 3840, 1080));
	}

	@Test
	void halfSideBySideIsToldFromTheWidth() {
		String output = showinfo("Stereo 3D: type - side by side, view - packed, primary_eye - none");
		assertEquals("half side by side (left eye first)", FramePackingParser.getMultiViewLayout(output, 1920, 1080));
		assertEquals("half side by side (left eye first)", FramePackingParser.getMultiViewLayout(output, 1280, 720));
	}

	@Test
	void topAndBottom() {
		String left = showinfo("Stereo 3D: type - top and bottom, view - packed, primary_eye - none");
		String right = showinfo("Stereo 3D: type - top and bottom (inverted), view - packed, primary_eye - none");
		assertEquals("top-bottom (left eye first)", FramePackingParser.getMultiViewLayout(left, 1920, 2160));
		assertEquals("top-bottom (right eye first)", FramePackingParser.getMultiViewLayout(right, 1920, 2160));
		assertEquals("half top-bottom (left eye first)", FramePackingParser.getMultiViewLayout(left, 1920, 1080));
	}

	@Test
	void olderFFmpegOutput() {
		// FFmpeg 6.0 to 7.0
		assertEquals("side by side (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - side by side (inverted)"), 3840, 1080));
		assertEquals("half top-bottom (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - top and bottom"), 1920, 1080));
		// FFmpeg 5.1 and older
		assertEquals("half side by side (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("stereoscopic information: type - side by side"), 1920, 1080));
		assertEquals("top-bottom (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("stereoscopic information: type - top and bottom (inverted)"), 1920, 2160));
		assertEquals("checkerboard (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("stereoscopic information: type - checkerboard"), 1920, 1080));
	}

	@Test
	void checkerboardRowsColumnsAndFramesAreStereo() {
		assertEquals("checkerboard (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - checkerboard, view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("checkerboard (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - checkerboard (inverted), view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("row interleaved (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - interleaved lines, view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("row interleaved (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - interleaved lines (inverted), view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("column interleaved (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - interleaved columns, view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("column interleaved (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("stereoscopic information: type - interleaved columns (inverted)"), 1920, 1080));
		assertEquals("both eyes laced in one block (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - frame alternate, view - left, primary_eye - none"), 1920, 1080));
		assertEquals("both eyes laced in one block (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - frame alternate (inverted), view - left, primary_eye - none"), 1920, 1080));
	}

	@Test
	void quincunxIsNamedSoThatItIsRefusedAloud() {
		assertEquals("side by side quincunx (left eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - side by side (quincunx subsampling), view - packed, primary_eye - none"), 1920, 1080));
		assertEquals("side by side quincunx (right eye first)", FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - side by side (quincunx subsampling) (inverted), view - packed, primary_eye - none"), 1920, 1080));
	}

	@Test
	void twoDIsNotStereo() {
		assertNull(FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - 2D, view - packed, primary_eye - none"), 1920, 1080));
	}

	@Test
	void mediaInfoWordsMapToTheModesOfTheViewsTheyMix() {
		String[][] words = {
			{"Row Interleaved (left eye first)", "IRL"}, {"Row Interleaved (right eye first)", "IRR"},
			{"Column Interleaved (left eye first)", "ICL"}, {"Column Interleaved (right eye first)", "ICR"},
			{"Both Eyes laced in one block (left eye first)", "AL"}, {"Both Eyes laced in one block (right eye first)", "AR"},
			// MediaInfo spells it "Checkboard"
			{"Checkboard (left eye first)", "CHL"}, {"Checkboard (right eye first)", "CHR"},
			{"checkerboard (left eye first)", "CHL"}, {"checkerboard (right eye first)", "CHR"}
		};
		for (String[] word : words) {
			MediaVideo video = new MediaVideo();
			video.setMultiViewLayout(word[0]);
			assertTrue(video.is3d(), word[0]);
			assertFalse(video.multiViewIsAnaglyph(), word[0]);
			assertEquals(Mode3D.valueOf(word[1]), video.get3DLayout(), word[0]);
		}
		for (String word : new String[] {"Anaglyph (cyan/red)", "Anaglyph (green/magenta)", "side by side quincunx (left eye first)"}) {
			MediaVideo video = new MediaVideo();
			video.setMultiViewLayout(word);
			assertTrue(video.is3d(), word);
			assertNull(video.get3DLayout(), word);
		}
	}

	@Test
	void testFiles() {
		if (!MediaInfoParser.isValid() || !FFmpegParser.isValid()) {
			LOGGER.info("FramePackingParser test on files skipped");
			return;
		}

		// The frame packing arrangement SEI, found by FFmpeg
		assertLayout("side by side (left eye first)", Mode3D.SBSL, "video-h264_frame_packing_side_by_side_left_first.mp4");
		assertLayout("half side by side (right eye first)", Mode3D.SBS2R, "video-h264_frame_packing_side_by_side_right_first.ts");
		assertLayout("half top-bottom (left eye first)", Mode3D.AB2L, "video-h264_frame_packing_top_bottom_left_first.mp4");
		assertLayout("top-bottom (right eye first)", Mode3D.ABR, "video-h264_frame_packing_top_bottom_right_first.mp4");
		assertLayout("half side by side (left eye first)", Mode3D.SBS2L, "video-h265_frame_packing_side_by_side_left_first.ts");
		assertLayout("side by side (right eye first)", Mode3D.SBSR, "video-h265_frame_packing_side_by_side_right_first.mp4");
		assertLayout("top-bottom (left eye first)", Mode3D.ABL, "video-h265_frame_packing_top_bottom_left_first.mp4");
		assertLayout("half top-bottom (right eye first)", Mode3D.AB2R, "video-h265_frame_packing_top_bottom_right_first.ts");

		// Checkerboard, columns, rows and temporal interleaving (H.264 D.2.26, table D-9 values 0, 1, 2 and 5)
		assertLayout("checkerboard (left eye first)", Mode3D.CHL, "video-h264_frame_packing_checkerboard.mp4");
		assertLayout("column interleaved (left eye first)", Mode3D.ICL, "video-h264_frame_packing_column_interleaving.mp4");
		assertLayout("row interleaved (left eye first)", Mode3D.IRL, "video-h264_frame_packing_row_interleaving.mp4");
		assertLayout("both eyes laced in one block (left eye first)", Mode3D.AL, "video-h264_frame_packing_temporal_interleaving.mp4");
		assertLayout("both eyes laced in one block (left eye first)", Mode3D.AL, "video-h265_frame_packing_temporal_interleaving.mp4");

		// 2D is not stereo
		assertNot3d("video-h264_frame_packing_2d.mp4");

		// Matroska's StereoMode, found by MediaInfo
		assertLayout("Side by Side (left eye first)", Mode3D.SBSL, "video-h264_stereomode_side_by_side_left_first.mkv");
		assertLayout("Top-Bottom (right eye first)", Mode3D.ABR, "video-h264_stereomode_top_bottom_right_first.mkv");
		assertLayout("Top-Bottom (left eye first)", Mode3D.ABL, "video-h265_stereomode_top_bottom_left_first.mkv");
		assertLayout("Side by Side (right eye first)", Mode3D.SBSR, "video-h265_stereomode_side_by_side_right_first.mkv");
	}

	@Test
	void noSideDataMeansNoLayout() {
		assertNull(FramePackingParser.getMultiViewLayout("[Parsed_showinfo_0 @ 0x1] n:   0 pts: 0\n", 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout("", 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout(null, 1920, 1080));
	}
}
