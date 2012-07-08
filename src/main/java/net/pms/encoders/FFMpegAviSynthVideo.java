/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.encoders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class handles the Windows-specific FFmpeg/AviSynth player combination. 
 */
public class FFMpegAviSynthVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegAviSynthVideo.class);
	public static final String ID      = "avsffmpeg";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "FFmpeg/AviSynth";
	}

	@Override
	public boolean avisynth() {
		return true;
	}

	@Override
	public JComponent config() {
		return config("FFMpegAviSynthVideo.0");
	}

	public static File getAVSScript(String fileName, DLNAMediaSubtitle subTrack) throws IOException {
		return getAVSScript(fileName, subTrack, -1, -1, null, null, true);
	}

	public static File getAVSScript(String fileName, DLNAMediaSubtitle subTrack, int fromFrame, int toFrame, String frameRateRatio, String frameRateNumber, boolean isFFmpeg) throws IOException {
		String onlyFileName = fileName.substring(1 + fileName.lastIndexOf("\\"));
		File file = new File(PMS.getConfiguration().getTempFolder(), "pms-avs-" + onlyFileName + ".avs");
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));

		/*
		 * Prepare the framerate variables
		 */
		String numerator;
		String denominator;

		if (frameRateRatio != null && frameRateNumber != null) {
			if (frameRateRatio.equals(frameRateNumber)) {
				// No ratio was available
				numerator = frameRateRatio;
				denominator = "1";
			} else {
				String[] frameRateNumDen = frameRateRatio.split("/");
				numerator = frameRateNumDen[0];
				denominator = "1001";
			}
		} else {
			// No framerate was given so we should try the most common one
			numerator = "24000";
			denominator = "1001";
			frameRateNumber = "23.976";
		}

		String assumeFPS = ".AssumeFPS(" + numerator + "," + denominator + ")";

		String directShowFPS = "";
		if (!"0".equals(frameRateNumber)) {
			directShowFPS = ", fps=" + frameRateNumber;
		}

		String convertfps = "";
		if (PMS.getConfiguration().getAvisynthConvertFps()) {
			convertfps = ", convertfps=true";
		}

		File f = new File(fileName);
		if (f.exists()) {
			fileName = ProcessUtil.getShortFileNameIfWideChars(fileName);
		}

		String movieLine       = "DirectShowSource(\"" + fileName + "\"" + directShowFPS + convertfps + ")" + assumeFPS;
		String mtLine1         = "";
		String mtLine2         = "";
		String mtLine3         = "";
		String interframeLines = null;
		String interframePath  = PMS.getConfiguration().getInterFramePath();

		int Cores = 1;
		if (PMS.getConfiguration().getAvisynthMultiThreading() && !isFFmpeg) {
			Cores = PMS.getConfiguration().getNumberOfCpuCores();

			// Goes at the start of the file to initiate multithreading
			mtLine1 = "SetMemoryMax(512)\nSetMTMode(3," + Cores + ")\n";

			// Goes after the input line to make multithreading more efficient
			mtLine2 = "SetMTMode(2)";

			// Goes at the end of the file to allow the multithreading to work with MEncoder
			mtLine3 = "SetMTMode(1)\nGetMTMode(false) > 0 ? distributor() : last";
		}

		// True Motion
		if (PMS.getConfiguration().getAvisynthInterFrame() && !isFFmpeg) {
			String GPU = "";
			movieLine = movieLine + ".ConvertToYV12()";

			// Enable GPU to assist with CPU
			if (PMS.getConfiguration().getAvisynthInterFrameGPU()){
				GPU = ", GPU=true";
			}

			interframeLines = "\n" +
				"PluginPath = \"" + interframePath + "\"\n" +
				"LoadPlugin(PluginPath+\"svpflow1.dll\")\n" +
				"LoadPlugin(PluginPath+\"svpflow2.dll\")\n" +
				"Import(PluginPath+\"InterFrame2.avsi\")\n" +
				"InterFrame(Cores=" + Cores + GPU + ")\n";
		}

		String subLine = null;
		if (subTrack != null && PMS.getConfiguration().getUseSubtitles() && !PMS.getConfiguration().isMencoderDisableSubs()) {
			LOGGER.trace("AviSynth script: Using sub track: " + subTrack);
			if (subTrack.getExternalFile() != null) {
				String function = "TextSub";
				if (subTrack.getType() == DLNAMediaSubtitle.VOBSUB) {
					function = "VobSub";
				}
				subLine = "clip=" + function + "(clip, \"" + ProcessUtil.getShortFileNameIfWideChars(subTrack.getExternalFile().getAbsolutePath()) + "\")";
			}
		}

		ArrayList<String> lines = new ArrayList<String>();

		lines.add(mtLine1);

		boolean fullyManaged = false;
		String script = PMS.getConfiguration().getAvisynthScript();
		StringTokenizer st = new StringTokenizer(script, PMS.AVS_SEPARATOR);
		while (st.hasMoreTokens()) {
			String line = st.nextToken();
			if (line.contains("<movie") || line.contains("<sub")) {
				fullyManaged = true;
			}
			lines.add(line);
		}

		lines.add(mtLine2);

		if (PMS.getConfiguration().getAvisynthInterFrame() && !isFFmpeg) {
			lines.add(interframeLines);
		}

		lines.add(mtLine3);

		if (fullyManaged) {
			for (String s : lines) {
				if (s.contains("<moviefilename>")) {
					s = s.replace("<moviefilename>", fileName);
				}

				if (movieLine != null) {
					s = s.replace("<movie>", movieLine);
				}
				s = s.replace("<sub>", subLine != null ? subLine : "#");
				pw.println(s);
			}
		} else {
			pw.println(movieLine);
			if (subLine != null) {
				pw.println(subLine);
			}
			pw.println("clip");

		}

		pw.close();
		file.deleteOnExit();
		return file;
	}
}
