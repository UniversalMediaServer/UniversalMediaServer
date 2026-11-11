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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.pms.media.video.MediaVideo.Mode3D;
import net.pms.parsers.ParserTest;
import net.pms.util.ProcessUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The filters that turn rows, columns, a checkerboard and alternating frames
 * into the layout of a renderer, and a run of FFmpeg that tells the views
 * apart sample by sample.
 */
class StereoConversionFilterTest {

	// The views are 64x32; the value of a sample tells its view and its place
	private static final String LEFT = "mod((%1$s)*3+(%2$s)*5,256)";
	private static final String RIGHT = "255-mod((%1$s)*3+(%2$s)*5,256)";

	@TempDir
	File tempDir;

	@BeforeAll
	static void setUpClass() {
		ParserTest.setUpClass();
	}

	@Test
	void rowsColumnsAndFramesGoToSideBySideFirst() {
		assertEquals(List.of("stereo3d=irl:sbsl", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.IRL, "sbsl"));
		assertEquals(List.of("stereo3d=irr:sbsl", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.IRR, "sbsl"));
		assertEquals(List.of("stereo3d=icl:sbsl", "stereo3d=sbsl:abl", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.ICL, "abl"));
		assertEquals(List.of("stereo3d=al:sbsl", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.AL, "sbsl"));
		assertEquals(List.of("stereo3d=ar:sbsl", "stereo3d=sbsl:ml", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.AR, "ml"));
	}

	@Test
	void columnsWithTheRightEyeFirstAvoidFFmpegsInputThatSwapsTheViews() {
		assertEquals(List.of("stereo3d=icl:sbsr", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.ICR, "sbsl"));
		assertEquals(List.of("stereo3d=icl:sbsr", "stereo3d=sbsl:arcd", "sidedata=mode=delete:type=STEREO3D"), FFMpegVideo.getStereoConversionFilters(Mode3D.ICR, "arcd"));
	}

	@Test
	void checkerboardIsUnpackedByItsOwnFilterThenConverted() {
		List<String> left = FFMpegVideo.getStereoConversionFilters(Mode3D.CHL, "sbsl");
		assertEquals(2, left.size());
		assertEquals("sidedata=mode=delete:type=STEREO3D", left.get(1));
		assertTrue(left.get(0).startsWith("geq=interpolation=n:lum='if(lt(X,W/2),p(2*X+mod(Y+0,2),Y),"), left.get(0));
		List<String> right = FFMpegVideo.getStereoConversionFilters(Mode3D.CHR, "ml");
		assertEquals(3, right.size());
		assertTrue(right.get(0).contains("mod(Y+1,2)"), right.get(0));
		assertEquals("stereo3d=sbsl:ml", right.get(1));
		assertEquals("sidedata=mode=delete:type=STEREO3D", right.get(2));
	}

	@Test
	void onlyTheLayoutsThatMixViewsAreUnpackedBeforeScaling() {
		List<Mode3D> mixed = List.of(Mode3D.IRL, Mode3D.IRR, Mode3D.ICL, Mode3D.ICR, Mode3D.AL, Mode3D.AR, Mode3D.CHL, Mode3D.CHR);
		for (Mode3D mode : Mode3D.values()) {
			assertEquals(mixed.contains(mode), FFMpegVideo.isUnpackedBeforeScaling(mode), mode.name());
		}
		assertFalse(FFMpegVideo.isUnpackedBeforeScaling(null));
	}

	@Test
	void theFramePackingWrittenFollowsTheOutputLayout() {
		// ITU-T H.264 D.2.26, table D-9: the x264 frame-packing values
		assertEquals("0", FFMpegVideo.getFramePackingArrangementForOutput("chl"));
		assertEquals("1", FFMpegVideo.getFramePackingArrangementForOutput("icr"));
		assertEquals("2", FFMpegVideo.getFramePackingArrangementForOutput("irl"));
		assertEquals("3", FFMpegVideo.getFramePackingArrangementForOutput("sbsl"));
		assertEquals("4", FFMpegVideo.getFramePackingArrangementForOutput("ab2r"));
		assertEquals("5", FFMpegVideo.getFramePackingArrangementForOutput("al"));
		assertNull(FFMpegVideo.getFramePackingArrangementForOutput("ml"));
		assertNull(FFMpegVideo.getFramePackingArrangementForOutput("arcd"));
	}

	private String run(String name, List<String> inputOptions, String filters) throws IOException {
		String ffmpeg = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		File output = new File(tempDir, name + ".yuv");
		List<String> command = new java.util.ArrayList<>(List.of(ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error", "-y"));
		command.addAll(inputOptions);
		command.addAll(List.of("-vf", filters, "-frames:v", "1", "-pix_fmt", "yuv444p", "-f", "rawvideo", output.getAbsolutePath()));
		String log = ProcessUtil.run(command.toArray(new String[0]));
		assertTrue(output.length() > 0, log);
		return output.getAbsolutePath();
	}

	/**
	 * A picture made from a formula of X and Y, grey so that the luma plane tells every sample.
	 */
	private File picture(String name, int width, int height, String luma) throws IOException {
		String path = run(name, List.of("-f", "lavfi", "-i", "color=c=black:size=" + width + "x" + height + ":rate=1"),
			"format=yuv444p,geq=interpolation=n:lum='" + luma + "':cb=128:cr=128");
		return new File(path);
	}

	private byte[] convert(String name, File packed, int width, int height, Mode3D layout) throws IOException {
		return convert(name, packed, width, height, layout, "sbsl");
	}

	private byte[] convert(String name, File packed, int width, int height, Mode3D layout, String output) throws IOException {
		String path = run(name,
			List.of("-f", "rawvideo", "-pix_fmt", "yuv444p", "-s", width + "x" + height, "-i", packed.getAbsolutePath()),
			String.join(",", FFMpegVideo.getStereoConversionFilters(layout, output)));
		return Files.readAllBytes(new File(path).toPath());
	}

	@Test
	void everySampleOfEachViewEndsWhereItsViewBelongs() throws IOException {
		assumeTrue(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO) != null, "FFmpeg is required for sample comparisons");

		// The reference, made without any conversion: left view on the left, right view on the right
		byte[] reference = Files.readAllBytes(picture("reference", 128, 32,
			"if(lt(X,64)," + String.format(LEFT, "X", "Y") + "," + String.format(RIGHT, "X-64", "Y") + ")").toPath());

		String left = String.format(LEFT, "floor(X/2)", "Y");
		String right = String.format(RIGHT, "floor(X/2)", "Y");
		// Checkerboard: the first view owns the samples where column and row add up to an even number (H.264 D.2.26, Figure D-1)
		assertArrayEquals(reference, convert("chl", picture("chl-in", 128, 32, "if(eq(mod(X+Y,2),0)," + left + "," + right + ")"), 128, 32, Mode3D.CHL), "checkerboard, left first");
		assertArrayEquals(reference, convert("chr", picture("chr-in", 128, 32, "if(eq(mod(X+Y,2),1)," + left + "," + right + ")"), 128, 32, Mode3D.CHR), "checkerboard, right first");
		// Columns: the first view owns the even columns (Figure D-2)
		assertArrayEquals(reference, convert("icl", picture("icl-in", 128, 32, "if(eq(mod(X,2),0)," + left + "," + right + ")"), 128, 32, Mode3D.ICL), "columns, left first");
		assertArrayEquals(reference, convert("icr", picture("icr-in", 128, 32, "if(eq(mod(X,2),1)," + left + "," + right + ")"), 128, 32, Mode3D.ICR), "columns, right first");
		// Rows: 64x64 from two 64x32 views, the first view owns the even rows (Figure D-4)
		String leftRows = String.format(LEFT, "X", "floor(Y/2)");
		String rightRows = String.format(RIGHT, "X", "floor(Y/2)");
		assertArrayEquals(reference, convert("irl", picture("irl-in", 64, 64, "if(eq(mod(Y,2),0)," + leftRows + "," + rightRows + ")"), 64, 64, Mode3D.IRL), "rows, left first");
		assertArrayEquals(reference, convert("irr", picture("irr-in", 64, 64, "if(eq(mod(Y,2),1)," + leftRows + "," + rightRows + ")"), 64, 64, Mode3D.IRR), "rows, right first");
	}

	@Test
	void monoOutputIsTheLeftOrTheRightViewWhateverTheOrderOfTheSource() throws IOException {
		assumeTrue(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO) != null, "FFmpeg is required for sample comparisons");

		byte[] leftView = Files.readAllBytes(picture("left-view", 64, 32, String.format(LEFT, "X", "Y")).toPath());
		byte[] rightView = Files.readAllBytes(picture("right-view", 64, 32, String.format(RIGHT, "X", "Y")).toPath());
		String left = String.format(LEFT, "floor(X/2)", "Y");
		String right = String.format(RIGHT, "floor(X/2)", "Y");
		String leftRows = String.format(LEFT, "X", "floor(Y/2)");
		String rightRows = String.format(RIGHT, "X", "floor(Y/2)");
		Object[][] sources = {
			{Mode3D.CHL, 128, 32, "if(eq(mod(X+Y,2),0)," + left + "," + right + ")"},
			{Mode3D.CHR, 128, 32, "if(eq(mod(X+Y,2),1)," + left + "," + right + ")"},
			{Mode3D.ICL, 128, 32, "if(eq(mod(X,2),0)," + left + "," + right + ")"},
			{Mode3D.ICR, 128, 32, "if(eq(mod(X,2),1)," + left + "," + right + ")"},
			{Mode3D.IRL, 64, 64, "if(eq(mod(Y,2),0)," + leftRows + "," + rightRows + ")"},
			{Mode3D.IRR, 64, 64, "if(eq(mod(Y,2),1)," + leftRows + "," + rightRows + ")"}
		};
		for (Object[] source : sources) {
			Mode3D layout = (Mode3D) source[0];
			File packed = picture(layout + "-mono-in", (int) source[1], (int) source[2], (String) source[3]);
			assertArrayEquals(leftView, convert(layout + "-ml", packed, (int) source[1], (int) source[2], layout, "ml"), layout + " to the left view");
			assertArrayEquals(rightView, convert(layout + "-mr", packed, (int) source[1], (int) source[2], layout, "mr"), layout + " to the right view");
		}
	}

	@Test
	void theComparisonCanFailWhenTheEyeOrderIsWrong() throws IOException {
		assumeTrue(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO) != null, "FFmpeg is required for sample comparisons");

		byte[] reference = Files.readAllBytes(picture("control-reference", 128, 32,
			"if(lt(X,64)," + String.format(LEFT, "X", "Y") + "," + String.format(RIGHT, "X-64", "Y") + ")").toPath());
		String left = String.format(LEFT, "floor(X/2)", "Y");
		String right = String.format(RIGHT, "floor(X/2)", "Y");
		File leftFirst = picture("control-in", 128, 32, "if(eq(mod(X+Y,2),0)," + left + "," + right + ")");
		assertFalse(java.util.Arrays.equals(reference, convert("control", leftFirst, 128, 32, Mode3D.CHR)), "a checkerboard read with the other eye first must differ");
	}
	@Test
	void alternatingFramesPairTheViewsInEitherOrder() throws IOException {
		assumeTrue(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO) != null, "FFmpeg is required for sample comparisons");
		byte[] left = Files.readAllBytes(picture("temporal-left", 64, 32, String.format(LEFT, "X", "Y")).toPath());
		byte[] right = Files.readAllBytes(picture("temporal-right", 64, 32, String.format(RIGHT, "X", "Y")).toPath());
		byte[] reference = Files.readAllBytes(picture("temporal-reference", 128, 32,
			"if(lt(X,64)," + String.format(LEFT, "X", "Y") + "," + String.format(RIGHT, "X-64", "Y") + ")").toPath());
		for (Mode3D mode : List.of(Mode3D.AL, Mode3D.AR)) {
			File packed = new File(tempDir, mode + "-frames.yuv");
			try (var stream = Files.newOutputStream(packed.toPath())) {
				stream.write(mode == Mode3D.AL ? left : right);
				stream.write(mode == Mode3D.AL ? right : left);
			}
			assertArrayEquals(reference, convert(mode + "-pair", packed, 64, 32, mode), mode.name());
		}
	}

}
