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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JTextField;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeIPCProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Pure FFmpeg video player. 
 */
public class FFMpegVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	public  static final String ID     = "ffmpegvideo";

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
		return false;
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
		return "FFmpeg";
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	protected String[] getDefaultArgs() {
		return new String[]{"-c:v", "mpeg2video", "-f", "vob", "-c:a", "ac3", "-loglevel", "fatal", "-max_delay", "0"};
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
				if (overriddenArgs[i].equals("-f") || overriddenArgs[i].equals("-c:a") || overriddenArgs[i].equals("-c:v")) {
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

	private JTextField ffmpeg;

	@Override
	public JComponent config() {
		return config("FFMpegVideo.1");
	}

	protected JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAMediaInfo mediaInfo) {
		if (mediaInfo != null) {
			// TODO: Determine compatibility based on mediaInfo
			return false;
		} else {
			// No information available
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(Format format) {
		if (format != null) {
			Format.Identifier id = format.getIdentifier();

			if (id.equals(Format.Identifier.MKV)
					|| id.equals(Format.Identifier.MPG)
					) {
				return true;
			}
		}

		return false;
	}
}
