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
import org.junit.jupiter.api.Test;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.FailSafeProcessWrapper;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.parsers.ParserTest;

public class FFmpegVideoTest {
	@Test
	public void testDolbyVisionOutput() {
		System.out.println("Cannot parse since the FFmpeg executable is undefined");
		ArrayList<String> args = new ArrayList<>();
		UmsConfiguration umsConfiguration;
		try {
			umsConfiguration = new UmsConfiguration(false);
			args.add(umsConfiguration.getFFmpegPath());
			System.out.println("ffmpeg path: " + umsConfiguration.getFFmpegPath());
		} catch (Exception e) {
			e.printStackTrace();
		}

		args.add("-hide_banner");
		args.add("-y");
		args.add("-i");

		File file = ParserTest.getTestFile("video-h265_dolbyvision_p05.05-eac3_atmos.mkv");
		args.add(file.getAbsolutePath());

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

		args.add("file.ts");

		UmsConfiguration configuration;
		boolean hasDolbyVisionOutput = false;

		try {
			configuration = new UmsConfiguration(false);

			OutputParams params = new OutputParams(configuration);
			params.setMaxBufferSize(1);
			params.setNoExitCheck(true);

			final ProcessWrapperImpl pw = new ProcessWrapperImpl(args.toArray(String[]::new), true, params, false, true);
			FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 10000);
			fspw.runInSameThread();

			if (!fspw.hasFail()) {
				boolean hasLoopedPastOutputLine = false;
				for (String line : pw.getResults()) {
					line = line.trim();

					System.out.println("line: " + line);
					if (line.startsWith("Output #0, mpegts, to 'file.ts':")) {
						hasLoopedPastOutputLine = true;
					}

					if (hasLoopedPastOutputLine) {
						if (line.startsWith("DOVI configuration record:")) {
							hasDolbyVisionOutput = true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(hasDolbyVisionOutput, true);
	}
}
