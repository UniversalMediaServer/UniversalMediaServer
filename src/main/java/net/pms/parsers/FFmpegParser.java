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

import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.InputFile;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.StandardEngineId;
import net.pms.io.FailSafeProcessWrapper;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegParser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * This class is not meant to be instantiated.
	 */
	private FFmpegParser() {
	}

	public static byte[][] getAnnexBFrameHeader(InputFile f) {
		String[] cmdArray = new String[14];
		cmdArray[0] = EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO);
		if (cmdArray[0] == null) {
			LOGGER.warn("Cannot process Annex B Frame Header is FFmpeg executable is undefined");
			return null;
		}
		cmdArray[1] = "-i";

		if (f.getPush() == null && f.getFilename() != null) {
			cmdArray[2] = f.getFilename();
		} else {
			cmdArray[2] = "-";
		}

		cmdArray[3] = "-vframes";
		cmdArray[4] = "1";
		cmdArray[5] = "-c:v";
		cmdArray[6] = "copy";
		cmdArray[7] = "-f";
		cmdArray[8] = "h264";
		cmdArray[9] = "-bsf";
		cmdArray[10] = "h264_mp4toannexb";
		cmdArray[11] = "-an";
		cmdArray[12] = "-y";
		cmdArray[13] = "pipe:";

		byte[][] returnData = new byte[2][];
		OutputParams params = new OutputParams(CONFIGURATION);
		params.setMaxBufferSize(1);
		params.setStdIn(f.getPush());

		final ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params);
		FailSafeProcessWrapper fspw = new FailSafeProcessWrapper(pw, 3000);
		fspw.runInSameThread();

		if (fspw.hasFail()) {
			LOGGER.info("Error parsing information from the file: " + f.getFilename());
			return null;
		}

		byte[] data = pw.getOutputByteArray().toByteArray();

		returnData[0] = data;
		int kf = 0;

		for (int i = 3; i < data.length; i++) {
			if (data[i - 3] == 1 && (data[i - 2] & 37) == 37 && (data[i - 1] & -120) == -120) {
				kf = i - 2;
				break;
			}
		}

		int st = 0;
		boolean found = false;

		if (kf > 0) {
			for (int i = kf; i >= 5; i--) {
				if (data[i - 5] == 0 && data[i - 4] == 0 && data[i - 3] == 0 && (data[i - 2] & 1) == 1 && (data[i - 1] & 39) == 39) {
					st = i - 5;
					found = true;
					break;
				}
			}
		}

		if (found) {
			byte[] header = new byte[kf - st];
			System.arraycopy(data, st, header, 0, kf - st);
			returnData[1] = header;
		}

		return returnData;
	}

}
