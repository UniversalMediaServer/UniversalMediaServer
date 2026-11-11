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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.io.OutputParams;
import net.pms.media.MediaInfo;
import net.pms.media.video.MediaVideo;
import net.pms.parsers.FramePackingParser;
import net.pms.parsers.ParserTest;
import net.pms.renderers.Renderer;
import net.pms.util.ProcessUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StereoConversionMetadataTest {
	@TempDir
	File tempDir;

	@BeforeAll
	static void configure() {
		ParserTest.setUpClass();
	}

	private List<String> filters(String inputLayout, String outputLayout) throws Exception {
		Renderer renderer = new Renderer((String) null) {
			@Override
			public String getOutput3DFormat() {
				return outputLayout;
			}
		};
		PMS.getConfiguration().setDisableSubtitles(true);
		MediaVideo video = new MediaVideo();
		video.setWidth(1280);
		video.setHeight(720);
		video.setMultiViewLayout(inputLayout);
		MediaInfo media = new MediaInfo();
		media.addVideoTrack(video);
		OutputParams params = new OutputParams(PMS.getConfiguration());
		params.setMediaRenderer(renderer);
		return new FFMpegVideo().getVideoFilterOptions(null, media, params, false);
	}

	@Test
	void untouchedVideoKeepsItsSideData() throws Exception {
		assertTrue(filters("side by side (left eye first)", "sbsl").isEmpty());
		assertTrue(filters("top-bottom (left eye first)", "").isEmpty());
		assertTrue(filters(null, "ml").isEmpty());
	}

	private String convertedMark(String outputLayout, String encoder) throws Exception {
		String ffmpeg = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		assumeTrue(ffmpeg != null, "FFmpeg is required to check the encoded layout");
		File input = ParserTest.getTestFile("video-h264_frame_packing_top_bottom_left_first.mp4");
		File output = new File(tempDir, outputLayout + "-" + encoder + ".ts");
		List<String> command = new ArrayList<>(List.of(ffmpeg, "-hide_banner", "-nostdin", "-y", "-i", input.getAbsolutePath()));
		command.addAll(filters("half top-bottom (left eye first)", outputLayout));
		command.addAll(List.of("-an", "-frames:v", "3", "-threads", "1", "-c:v", encoder));
		if ("sbsl".equals(outputLayout)) {
			command.addAll(List.of("-x264-params", "frame-packing=3"));
		}
		command.addAll(List.of("-f", "mpegts", output.getAbsolutePath()));
		String log = ProcessUtil.run(command.toArray(new String[0]));
		assertTrue(output.length() > 0, log);
		return ProcessUtil.run(ffmpeg, "-hide_banner", "-nostdin", "-i", output.getAbsolutePath(),
			"-vf", "showinfo", "-frames:v", "1", "-f", "null", "-");
	}

	@Test
	void sideBySideOutputCarriesSideBySideRatherThanTheOldTopBottomMark() throws Exception {
		String log = convertedMark("sbsl", "libx264");
		assertEquals("half side by side (left eye first)", FramePackingParser.getMultiViewLayout(log, 1280, 360), log);
	}

	@Test
	void monoOutputHasNoStereoMarkInH264OrMpeg2() throws Exception {
		for (String encoder : List.of("libx264", "mpeg2video")) {
			for (String output : List.of("ml", "mr")) {
				String log = convertedMark(output, encoder);
				assertNull(FramePackingParser.getMultiViewLayout(log, 1280, 360), log);
				assertFalse(log.contains("Stereo 3D") || log.contains("stereoscopic information"), log);
			}
		}
	}
}
