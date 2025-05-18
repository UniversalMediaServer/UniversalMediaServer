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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.FailSafeProcessWrapper;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.parsers.ParserTest;
import net.pms.platform.PlatformUtils;

public class FFmpegVideoTest {
	public List<String> getFFmpegOutput(String filename, ArrayList<String> ffmpegArgs) {
		ArrayList<String> args = new ArrayList<>();
		UmsConfiguration umsConfiguration;
		try {
			umsConfiguration = new UmsConfiguration(false);
			if (umsConfiguration.getFFmpegPath() == null) {
				throw new Exception("no ffmpeg");
			}
			args.add(umsConfiguration.getFFmpegPath());
		} catch (Exception e) {
			System.out.println("did not find ffmpeg, falling back manually");
			// this is a lazy workaround for an error, but the error has nothing to do with the purpose of the test
			if (PlatformUtils.isMac()) {
				args.add("/Users/runner/work/UniversalMediaServer/UniversalMediaServer/target/bin/osx/ffmpeg");
			} else {
				args.add("/home/runner/work/UniversalMediaServer/UniversalMediaServer/target/bin/linux/ffmpeg");
			}
		}

		args.add("-analyzeduration");
		args.add("100M");
		args.add("-probesize");
		args.add("100M");

		args.add("-i");
		File file = ParserTest.getTestFile(filename);
		args.add(file.getAbsolutePath());

		args.addAll(ffmpegArgs);

		args.add("-");

		UmsConfiguration configuration;

		try {
			configuration = new UmsConfiguration(false);

			OutputParams params = new OutputParams(configuration);
			params.setMaxBufferSize(1);
			params.setNoExitCheck(true);

			final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params, false, true);
			FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 10000);
			fspw.runInSameThread();

			if (!fspw.hasFail()) {
				return pw.getResults();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Test
	public void testDolbyVisionOutput() {
		ArrayList<String> args = new ArrayList<>();

		// args.add("-filter_complex");
		// args.add("[0:v][0:s:0]overlay");
		args.add("-maxrate");
		args.add("89000k");
		args.add("-crf");
		args.add("16");
		args.add("-c:a");
		args.add("copy");
		args.add("-c:v");
		args.add("libx265");
		args.add("-preset");
		args.add("superfast");
		args.add("-f");
		args.add("mpegts");

		args.add("-x265-params");
		args.add("vbv-maxrate=30000:vbv-bufsize=30000");
		args.add("-dolbyvision");
		args.add("1");

		List<String> ffmpegOutput = getFFmpegOutput("video-h265_dolbyvision_p08.06-eac3_dolby_surround_ex-hdmv_pgs.mkv", args);

		boolean hasDolbyVisionOutput = false;
		boolean hasLoopedPastOutputLine = false;
		for (String line : ffmpegOutput) {
			line = line.trim();

			// System.out.println("line: " + line);
			if (line.startsWith("Output #0, mpegts, to 'pipe:':")) {
				hasLoopedPastOutputLine = true;
			}

			if (hasLoopedPastOutputLine) {
				if (line.startsWith("DOVI configuration record:")) {
					hasDolbyVisionOutput = true;
					break;
				}
			}
		}

		assertEquals(hasDolbyVisionOutput, true);
	}

	@Test
	public void testHardcodingSubtitles() {
		ArrayList<String> args = new ArrayList<>();

		args.add("-filter_complex");
		args.add("[0:v][0:s:0]overlay");
		args.add("-maxrate");
		args.add("89000k");
		args.add("-crf");
		args.add("16");
		args.add("-c:a");
		args.add("copy");
		args.add("-c:v");
		args.add("libx265");
		args.add("-preset");
		args.add("superfast");
		args.add("-f");
		args.add("mpegts");

		List<String> ffmpegOutput = getFFmpegOutput("video-h265_dolbyvision_p08.06-eac3_dolby_surround_ex-hdmv_pgs.mkv", args);

		boolean finishedEncoding = false;
		for (String line : ffmpegOutput) {
			line = line.trim();

			System.out.println("line: " + line);
			if (line.startsWith("encoded 334 frames in")) {
				finishedEncoding = true;
			}
		}

		assertEquals(finishedEncoding, true);
	}
}
