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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FramePackingParserTest {

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
		assertNull(FramePackingParser.getMultiViewLayout(showinfo("stereoscopic information: type - checkerboard"), 1920, 1080));
	}

	@Test
	void layoutsWithoutAModeAreIgnored() {
		assertNull(FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - checkerboard, view - packed, primary_eye - none"), 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - frame alternate, view - left, primary_eye - none"), 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout(showinfo("Stereo 3D: type - 2D, view - packed, primary_eye - none"), 1920, 1080));
	}

	@Test
	void noSideDataMeansNoLayout() {
		assertNull(FramePackingParser.getMultiViewLayout("[Parsed_showinfo_0 @ 0x1] n:   0 pts: 0\n", 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout("", 1920, 1080));
		assertNull(FramePackingParser.getMultiViewLayout(null, 1920, 1080));
	}
}
