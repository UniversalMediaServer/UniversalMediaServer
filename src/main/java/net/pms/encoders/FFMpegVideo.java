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

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JTextField;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeIPCProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import net.pms.util.ProcessUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class FFMpegVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	public  static final String ID     = "avsffmpeg";

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_PLAYER;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public boolean avisynth() {
		return true;
	}
	private String overriddenArgs[];

	public FFMpegVideo() {
		if (PMS.getConfiguration().getFfmpegSettings() != null) {
			StringTokenizer st = new StringTokenizer(PMS.getConfiguration().getFfmpegSettings() + " -ab " + PMS.getConfiguration().getAudioBitrate() + "k -threads " + PMS.getConfiguration().getNumberOfCpuCores(), " ");
			overriddenArgs = new String[st.countTokens()];
			int i = 0;
			while (st.hasMoreTokens()) {
				overriddenArgs[i++] = st.nextToken();
			}
		}
	}

	@Override
	public String name() {
		return "AviSynth/FFmpeg";
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	protected String[] getDefaultArgs() {
		return new String[]{"-vcodec", "mpeg2video", "-f", "vob", "-acodec", "ac3"};
	}

	@Override
	public String[] args() {
		String args[] = null;
		String defaultArgs[] = getDefaultArgs();
		if (overriddenArgs != null) {
			args = new String[defaultArgs.length + overriddenArgs.length];
			for (int i = 0; i < defaultArgs.length; i++) {
				args[i] = defaultArgs[i];
			}
			for (int i = 0; i < overriddenArgs.length; i++) {
				if (overriddenArgs[i].equals("-f") || overriddenArgs[i].equals("-acodec") || overriddenArgs[i].equals("-vcodec")) {
					LOGGER.info("FFmpeg encoder settings: You cannot change Muxer, Video Codec or Audio Codec");
					overriddenArgs[i] = "-title";
					if (i + 1 < overriddenArgs.length) {
						overriddenArgs[i + 1] = "NewTitle";
					}
				}
				args[i + defaultArgs.length] = overriddenArgs[i];
			}
		} else {
			args = defaultArgs;
		}
		return args;

	}

	public boolean mplayer() {
		return false;
	}

	@Override
	public String mimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public String executable() {
		return PMS.getConfiguration().getFfmpegPath();
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		return getFFMpegTranscode(fileName, dlna, media, params, args());
	}

	protected ProcessWrapperImpl getFFMpegTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params,
		String args[]) throws IOException {
		setAudioAndSubs(fileName, media, params, PMS.getConfiguration());

		PipeIPCProcess videoP = null;
		PipeIPCProcess audioP = null;
		if (mplayer()) {
			videoP = new PipeIPCProcess("mplayer_vid1" + System.currentTimeMillis(), "mplayer_vid2" + System.currentTimeMillis(), false, false);
			audioP = new PipeIPCProcess("mplayer_aud1" + System.currentTimeMillis(), "mplayer_aud2" + System.currentTimeMillis(), false, false);
		}

		String cmdArray[] = new String[14 + args.length];
		cmdArray[0] = executable();
		cmdArray[1] = "-sn";
		cmdArray[2] = "-sn";
		if (params.timeseek > 0 && !mplayer()) {
			cmdArray[1] = "-ss";
			cmdArray[2] = "" + params.timeseek;
		}
		cmdArray[3] = "-sn";
		cmdArray[4] = "-sn";
		cmdArray[5] = "-sn";
		cmdArray[6] = "-sn";
		if (type() == Format.VIDEO) {
			cmdArray[5] = "-i";
			cmdArray[6] = fileName;
			if (mplayer()) {
				cmdArray[3] = "-f";
				cmdArray[4] = "yuv4mpegpipe";
				//cmdArray[6] = pipeprefix + videoPipe + (PMS.get().isWindows()?".2":"");
				cmdArray[6] = videoP.getOutputPipe();
			} else if (avisynth()) {
				File avsFile = getAVSScript(fileName, params.sid, params.fromFrame, params.toFrame);
				cmdArray[6] = ProcessUtil.getShortFileNameIfWideChars(avsFile.getAbsolutePath());
			}
		}
		cmdArray[7] = "-sn";
		cmdArray[8] = "-sn";
		cmdArray[9] = "-sn";
		cmdArray[10] = "-sn";
		if (type() == Format.VIDEO || type() == Format.AUDIO) {
			if (type() == Format.VIDEO && (mplayer())) {
				cmdArray[7] = "-f";
				cmdArray[8] = "wav";
				cmdArray[9] = "-i";
				//cmdArray[10] = pipeprefix + audioPipe + (PMS.get().isWindows()?".2":"");
				cmdArray[10] = audioP.getOutputPipe();
			} else if (type() == Format.AUDIO) {
				cmdArray[7] = "-i";
				cmdArray[8] = fileName;
			}
		}
		if (params.timeend > 0) {
			cmdArray[9] = "-t";
			cmdArray[10] = "" + params.timeend;
		}
		for (int i = 0; i < args.length; i++) {
			cmdArray[11 + i] = args[i];
		}

		cmdArray[cmdArray.length - 3] = "-muxpreload";
		cmdArray[cmdArray.length - 2] = "0";

		if (PMS.getConfiguration().isFileBuffer()) {
			File m = new File(PMS.getConfiguration().getTempFolder(), "pms-transcode.tmp");
			if (m.exists() && !m.delete()) {
				LOGGER.info("Temp file currently used.. Waiting 3 seconds");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				if (m.exists() && !m.delete()) {
					LOGGER.info("Temp file cannot be deleted... Serious ERROR");
				}
			}
			params.outputFile = m;
			params.minFileSize = params.minBufferSize;
			m.deleteOnExit();
			cmdArray[cmdArray.length - 1] = m.getAbsolutePath();
		} else {
			cmdArray[cmdArray.length - 1] = "pipe:";
		}

		cmdArray = finalizeTranscoderArgs(
			this,
			fileName,
			dlna,
			media,
			params,
			cmdArray
		);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		if (type() != Format.AUDIO && (mplayer())) {
			ProcessWrapper mkfifo_vid_process = videoP.getPipeProcess();
			ProcessWrapper mkfifo_aud_process = audioP.getPipeProcess();

			String seek_param = "-quiet";
			String seek_value = "-quiet";
			if (params.timeseek > 0) {
				seek_param = "-ss";
				seek_value = "" + params.timeseek;
			}

			String overiddenMPlayerArgs[] = null;

			overiddenMPlayerArgs = new String[0];


			String mPlayerdefaultVideoArgs[] = new String[]{fileName, seek_param, seek_value, "-vo", "yuv4mpeg:file=" + videoP.getInputPipe(), "-ao", "pcm:waveheader:file=" + audioP.getInputPipe(), "-benchmark", "-noframedrop", "-speed", "100"/*, "-quiet"*/};
			OutputParams mplayer_vid_params = new OutputParams(PMS.getConfiguration());
			mplayer_vid_params.maxBufferSize = 1;

			String videoArgs[] = new String[1 + overiddenMPlayerArgs.length + mPlayerdefaultVideoArgs.length];
			videoArgs[0] = PMS.getConfiguration().getMplayerPath();
			System.arraycopy(overiddenMPlayerArgs, 0, videoArgs, 1, overiddenMPlayerArgs.length);
			System.arraycopy(mPlayerdefaultVideoArgs, 0, videoArgs, 1 + overiddenMPlayerArgs.length, mPlayerdefaultVideoArgs.length);
			ProcessWrapperImpl mplayer_vid_process = new ProcessWrapperImpl(videoArgs, mplayer_vid_params);

			if (type() == Format.VIDEO) {
				pw.attachProcess(mkfifo_vid_process);
			}
			if (type() == Format.VIDEO || type() == Format.AUDIO) {
				pw.attachProcess(mkfifo_aud_process);
			}
			if (type() == Format.VIDEO) {
				pw.attachProcess(mplayer_vid_process);
			}

			if (type() == Format.VIDEO) {
				mkfifo_vid_process.runInNewThread();
			}
			if (type() == Format.VIDEO || type() == Format.AUDIO) {
				mkfifo_aud_process.runInNewThread();
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
			if (type() == Format.VIDEO) {
				videoP.deleteLater();
				mplayer_vid_process.runInNewThread();
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
		}

		pw.runInNewThread();
		return pw;
	}

	public static File getAVSScript(String fileName, DLNAMediaSubtitle subTrack) throws IOException {
		return getAVSScript(fileName, subTrack, -1, -1);
	}

	public static File getAVSScript(String fileName, DLNAMediaSubtitle subTrack, int fromFrame, int toFrame) throws IOException {
		String onlyFileName = fileName.substring(1 + fileName.lastIndexOf("\\"));
		File file = new File(PMS.getConfiguration().getTempFolder(), "pms-avs-" + onlyFileName + ".avs");
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));

		String convertfps = "";
		if (PMS.getConfiguration().getAvisynthConvertFps()) {
			convertfps = ", convertfps=true";
		}
		File f = new File(fileName);
		if (f.exists()) {
			fileName = ProcessUtil.getShortFileNameIfWideChars(fileName);
		}

		String movieLine       = "DirectShowSource(\"" + fileName + "\"" + convertfps + ")";
		String mtLine1         = "";
		String mtLine2         = "";
		String mtLine3         = "";
		String interframeLines = null;
		String interframePath  = PMS.getConfiguration().getInterFramePath();

		int Cores = 1;
		if (PMS.getConfiguration().getAvisynthMultiThreading()) {
			Cores = PMS.getConfiguration().getNumberOfCpuCores();

			// Goes at the start of the file to initiate multithreading
			mtLine1 = "SetMemoryMax(512)\nSetMTMode(3," + Cores + ")\n";

			// Goes after the input line to make multithreading more efficient
			mtLine2 = "SetMTMode(2)";

			// Goes at the end of the file to allow the multithreading to work with MEncoder
			mtLine3 = "SetMTMode(1)\nGetMTMode(false) > 0 ? distributor() : last";
		}

		// True Motion
		if (PMS.getConfiguration().getAvisynthInterFrame()) {
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
			LOGGER.trace("Avisynth script: Using sub track: " + subTrack);
			if (subTrack.getFile() != null) {
				String function = "TextSub";
				if (subTrack.getType() == DLNAMediaSubtitle.VOBSUB) {
					function = "VobSub";
				}
				subLine = function + "(\"" + ProcessUtil.getShortFileNameIfWideChars(subTrack.getFile().getAbsolutePath()) + "\")";
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

		if (PMS.getConfiguration().getAvisynthInterFrame()) {
			lines.add(interframeLines);
		}

		lines.add(mtLine3);

		if (fullyManaged) {
			for (String s : lines) {
				s = s.replace("<moviefilename>", fileName);
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
	private JTextField ffmpeg;

	@Override
	public JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("FFMpegVideo.0"), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		ffmpeg = new JTextField(PMS.getConfiguration().getFfmpegSettings());
		ffmpeg.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				PMS.getConfiguration().setFfmpegSettings(ffmpeg.getText());
			}
		});
		builder.add(ffmpeg, cc.xy(2, 3));

		return builder.getPanel();
	}
}
