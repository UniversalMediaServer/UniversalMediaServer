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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeIPCProcess;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.StreamModifier;
import net.pms.network.HTTPResource;
import net.pms.util.ProcessUtil;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Pure FFmpeg video player. 
 */
public class FFMpegVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	public  static final String ID     = "ffmpegvideo";

	protected boolean dtsRemux;
	protected boolean ac3Remux;

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

	public String initialString() {
		String threads = "";
		if (PMS.getConfiguration().isFfmpegMultithreading()) {
			threads = " -threads " + PMS.getConfiguration().getNumberOfCpuCores();
		}
		return PMS.getConfiguration().getFfmpegSettings() + threads;
	}

	public FFMpegVideo() {
		if (PMS.getConfiguration().getFfmpegSettings() != null) {
			StringTokenizer st = new StringTokenizer(initialString(), " ");
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
		List<String> defaultArgsList = new ArrayList<String>();

		defaultArgsList.add("-loglevel");
		defaultArgsList.add("fatal");

		String[] defaultArgsArray = new String[defaultArgsList.size()];
		defaultArgsList.toArray(defaultArgsArray);

		return defaultArgsArray;
	}

	private int[] getVideoBitrateConfig(String bitrate) {
		int bitrates[] = new int[2];

		if (bitrate.contains("(") && bitrate.contains(")")) {
			bitrates[1] = Integer.parseInt(bitrate.substring(bitrate.indexOf("(") + 1, bitrate.indexOf(")")));
		}

		if (bitrate.contains("(")) {
			bitrate = bitrate.substring(0, bitrate.indexOf("(")).trim();
		}

		if (isBlank(bitrate)) {
			bitrate = "0";
		}

		bitrates[0] = (int) Double.parseDouble(bitrate);

		return bitrates;
	}

	@Override
	public String[] args() {
		String args[];
		String defaultArgs[] = getDefaultArgs();

		if (overriddenArgs != null) {
			args = new String[defaultArgs.length + overriddenArgs.length];
			System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);

			boolean loggedDisallowedFfmpegOptions = false;

			for (int i = 0; i < overriddenArgs.length; i++) {
				if (overriddenArgs[i].equals("-f") || overriddenArgs[i].equals("-c:a") || overriddenArgs[i].equals("-c:v")) {
					// No need to log this for each disallowed option
					if (!loggedDisallowedFfmpegOptions) {
						LOGGER.warn("The following ffmpeg options cannot be changed and will be ignored: -f, -acodec, -vcodec");
						loggedDisallowedFfmpegOptions = true;
					}

					overriddenArgs[i] = "-y";

					if (i + 1 < overriddenArgs.length) {
						overriddenArgs[i + 1] = "-y";
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
		String args[]
	) throws IOException {
		setAudioAndSubs(fileName, media, params, PMS.getConfiguration());

		PipeIPCProcess videoP = null;
		PipeIPCProcess audioP = null;

		if (mplayer()) {
			videoP = new PipeIPCProcess("mplayer_vid1" + System.currentTimeMillis(), "mplayer_vid2" + System.currentTimeMillis(), false, false);
			audioP = new PipeIPCProcess("mplayer_aud1" + System.currentTimeMillis(), "mplayer_aud2" + System.currentTimeMillis(), false, false);
		}

		List<String> cmdList = new ArrayList<String>();
		cmdList.add(executable());

		if (params.timeseek > 0 && !mplayer()) {
			cmdList.add("-ss");
			cmdList.add("" + params.timeseek);
		} else {
			cmdList.add("-sn");
			cmdList.add("-sn");
		}

		String cmd3 = "-sn";
		String cmd4 = "-sn";
		String cmd5 = "-sn";
		String cmd6 = "-sn";

		if (type() == Format.VIDEO) {
			cmd5 = "-i";
			cmd6 = fileName;
			if (mplayer()) {
				cmd3 = "-f";
				cmd4 = "yuv4mpegpipe";
				cmd6 = videoP.getOutputPipe();
			} else if (avisynth()) {
				File avsFile = FFMpegAviSynthVideo.getAVSScript(fileName, params.sid, params.fromFrame, params.toFrame, null, null);
				cmd6 = ProcessUtil.getShortFileNameIfWideChars(avsFile.getAbsolutePath());
			}
		}

		cmdList.add(cmd3);
		cmdList.add(cmd4);
		cmdList.add(cmd5);
		cmdList.add(cmd6);

		final boolean isTSMuxerVideoEngineEnabled = PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry()).contains(TSMuxerVideo.ID);

		ac3Remux = false;
		dtsRemux = false;

		String audioCodecInput1 = "-c:a";
		String audioCodecInput2 = "ac3";
		if (PMS.getConfiguration().isRemuxAC3() && params.aid != null && params.aid.isAC3() && !avisynth() && params.mediaRenderer.isTranscodeToAC3()) {
			// AC-3 remux takes priority
			ac3Remux = true;
			audioCodecInput2 = "copy";
		} else {
			// Now check for DTS remux and LPCM streaming
			dtsRemux = isTSMuxerVideoEngineEnabled &&
				PMS.getConfiguration().isDTSEmbedInPCM() &&
				params.aid != null &&
				params.aid.isDTS() &&
				!avisynth() &&
				params.mediaRenderer.isDTSPlayable();

			if (dtsRemux) {
				audioCodecInput1 = "-an";
				audioCodecInput2 = "-an";
			} else if (type() == Format.AUDIO) {
				audioCodecInput1 = "-sn";
				audioCodecInput2 = "-sn";
			}
		}

		// Set the output format
		String cmd7 = "-f";
		String cmd8 = "vob";
		String cmd9 = "-sn";
		String cmd10 = "-sn";

		if (dtsRemux) {
			params.losslessaudio = true;
			params.forceFps = media.getValidFps(false);
			cmd8 = "mpeg2video";
		}

		if (type() == Format.VIDEO || type() == Format.AUDIO) {
			if (type() == Format.VIDEO && (mplayer())) {
				cmd8 = "wav";
				cmd9 = "-i";
				cmd10 = audioP.getOutputPipe();
			} else if (type() == Format.AUDIO) {
				cmd7 = "-i";
				cmd8 = fileName;
			}
		}

		if (params.timeend > 0) {
			cmd9 = "-t";
			cmd10 = "" + params.timeend;
		}

		cmdList.add(cmd7);
		cmdList.add(cmd8);
		cmdList.add(cmd9);
		cmdList.add(cmd10);

		String cmd11 = "-sn";
		String cmd12 = "-sn";
		String cmd13 = "-sn";
		String cmd14 = "-sn";

		int defaultMaxBitrates[] = getVideoBitrateConfig(PMS.getConfiguration().getMaximumBitrate());
		int rendererMaxBitrates[] = new int[2];

		if (params.mediaRenderer.getMaxVideoBitrate() != null) {
			rendererMaxBitrates = getVideoBitrateConfig(params.mediaRenderer.getMaxVideoBitrate());
		}

		if ((defaultMaxBitrates[0] == 0 && rendererMaxBitrates[0] > 0) || rendererMaxBitrates[0] < defaultMaxBitrates[0] && rendererMaxBitrates[0] > 0) {
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (params.mediaRenderer.getCBRVideoBitrate() == 0 && defaultMaxBitrates[0] > 0) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			// Halve it since it seems to send up to 1 second of video in advance
			defaultMaxBitrates[0] = defaultMaxBitrates[0] / 2;

			int bufSize = 1835;
			if (media.isHDVideo()) {
				bufSize = defaultMaxBitrates[0] / 3;
			}

			if (bufSize > 7000) {
				bufSize = 7000;
			}

			if (defaultMaxBitrates[1] > 0) {
				bufSize = defaultMaxBitrates[1];
			}

			if (params.mediaRenderer.isDefaultVBVSize() && rendererMaxBitrates[1] == 0) {
				bufSize = 1835;
			}

			// Audio is always AC3 right now, so subtract the configured amount (usually 640)
			defaultMaxBitrates[0] = defaultMaxBitrates[0] - PMS.getConfiguration().getAudioBitrate();

			// Round down to the nearest Mb
			defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;

			// FFmpeg uses bytes for inputs instead of kbytes like MEncoder
			bufSize = bufSize * 1000;
			defaultMaxBitrates[0] = defaultMaxBitrates[0] * 1000;

			cmd11 = "-bufsize";
			cmd12 = "" + bufSize;

			cmd13 = "-maxrate";
			cmd14 = "" + defaultMaxBitrates[0];
		}

		cmdList.add(cmd11);
		cmdList.add(cmd12);
		cmdList.add(cmd13);
		cmdList.add(cmd14);

		// Audio codec
		cmdList.add(audioCodecInput1);
		cmdList.add(audioCodecInput2);

		int channels;
		if (ac3Remux) {
			channels = params.aid.getAudioProperties().getNumberOfChannels(); // AC-3 remux
		} else if (dtsRemux) {
			channels = 2;
		} else {
			channels = PMS.getConfiguration().getAudioChannelCount(); // 5.1 max for AC-3 encoding
		}
		LOGGER.trace("channels=" + channels);

		// Audio bitrate
		String cmd17 = ((params.aid.isAC3() && !ac3Remux) || type() == Format.AUDIO) ? "-sn" : "-ab";
		String cmd18 = ((params.aid.isAC3() && !ac3Remux) || type() == Format.AUDIO) ? "-sn" : PMS.getConfiguration().getAudioBitrate() + "k";

		cmdList.add(cmd17);
		cmdList.add(cmd18);

		// Add the arguments being passed from other engines
		cmdList.addAll(Arrays.asList(args));

		if (!dtsRemux) {
			if (PMS.getConfiguration().isFileBuffer()) {
				File m = new File(PMS.getConfiguration().getTempFolder(), "pms-transcode.tmp");
				if (m.exists() && !m.delete()) {
					LOGGER.info("Temp file currently used.. Waiting 3 seconds");

					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) { }

					if (m.exists() && !m.delete()) {
						LOGGER.info("Temp file cannot be deleted... Serious ERROR");
					}
				}

				params.outputFile = m;
				params.minFileSize = params.minBufferSize;
				m.deleteOnExit();
				cmdList.add(m.getAbsolutePath());
			} else {
				cmdList.add("pipe:");
			}
		}

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

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

			String seek_param = "-ss";
			String seek_value = "0";
			if (params.timeseek > 0) {
				seek_value = "" + params.timeseek;
			}

			String overiddenMPlayerArgs[];

			overiddenMPlayerArgs = new String[0];

			String[] mPlayerdefaultVideoArgs = new String[] {
				fileName,
				seek_param,
				seek_value,
				"-vo", "yuv4mpeg:file=" + videoP.getInputPipe(),
				"-ao", "pcm:waveheader:file=" + audioP.getInputPipe(),
				"-benchmark",
				"-noframedrop",
				"-speed", "100"
			};

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
			} catch (InterruptedException e) { }

			if (type() == Format.VIDEO) {
				videoP.deleteLater();
				mplayer_vid_process.runInNewThread();
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) { }
		} else if (dtsRemux) {
			PipeProcess pipe;
			pipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

			TSMuxerVideo ts = new TSMuxerVideo(PMS.getConfiguration());
			File f = new File(PMS.getConfiguration().getTempFolder(), "pms-tsmuxer.meta");
			String cmd[] = new String[]{ ts.executable(), f.getAbsolutePath(), pipe.getInputPipe() };
			pw = new ProcessWrapperImpl(cmd, params);

			PipeIPCProcess ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			cmdList.add(ffVideoPipe.getInputPipe());

			OutputParams ffparams = new OutputParams(PMS.getConfiguration());
			ffparams.maxBufferSize = 1;
			ffparams.stdin = params.stdin;

			String[] cmdArrayDts = new String[cmdList.size()];
			cmdList.toArray(cmdArrayDts);

			cmdArrayDts = finalizeTranscoderArgs(
				this,
				fileName,
				dlna,
				media,
				params,
				cmdArrayDts
			);

			ProcessWrapperImpl ffVideo = new ProcessWrapperImpl(cmdArrayDts, ffparams);

			ProcessWrapper ff_video_pipe_process = ffVideoPipe.getPipeProcess();
			pw.attachProcess(ff_video_pipe_process);
			ff_video_pipe_process.runInNewThread();
			ffVideoPipe.deleteLater();

			pw.attachProcess(ffVideo);
			ffVideo.runInNewThread();

			PipeIPCProcess ffAudioPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);
			StreamModifier sm = new StreamModifier();
			sm.setPcm(false);
			sm.setDtsembed(dtsRemux);
			sm.setSampleFrequency(48000);
			sm.setBitspersample(16);
			sm.setNbchannels(channels);

			String ffmpegLPCMextract[] = new String[]{
				executable(), 
				"-ss", "0",
				"-i", fileName,
				"-ac", "" + channels,
				"-f", "dts",
				"-c:a", "copy",
				ffAudioPipe.getInputPipe()
			};

			if (!params.mediaRenderer.isMuxDTSToMpeg()) { // No need to use the PCM trick when media renderer supports DTS
				ffAudioPipe.setModifier(sm);
			}

			if (params.stdin != null) {
				ffmpegLPCMextract[3] = "-";
			}

			if (params.timeseek > 0) {
				ffmpegLPCMextract[2] = "" + params.timeseek;
			}

			OutputParams ffaudioparams = new OutputParams(PMS.getConfiguration());
			ffaudioparams.maxBufferSize = 1;
			ffaudioparams.stdin = params.stdin;
			ProcessWrapperImpl ffAudio = new ProcessWrapperImpl(ffmpegLPCMextract, ffaudioparams);

			params.stdin = null;

			PrintWriter pwMux = new PrintWriter(f);
			pwMux.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
			String videoType = "V_MPEG-2";

			if (params.no_videoencode && params.forceType != null) {
				videoType = params.forceType;
			}

			String fps = "";
			if (params.forceFps != null) {
				fps = "fps=" + params.forceFps + ", ";
			}

			String audioType = "A_AC3";
			if (dtsRemux) {
				if (params.mediaRenderer.isMuxDTSToMpeg()) {
					// Renderer can play proper DTS track
					audioType = "A_DTS";
				} else {
					// DTS padded in LPCM trick
					audioType = "A_LPCM";
				}
			}

			pwMux.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + fps + "level=4.1, insertSEI, contSPS, track=1");
			pwMux.println(audioType + ", \"" + ffAudioPipe.getOutputPipe() + "\", track=2");
			pwMux.close();

			ProcessWrapper pipe_process = pipe.getPipeProcess();
			pw.attachProcess(pipe_process);
			pipe_process.runInNewThread();

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}

			pipe.deleteLater();
			params.input_pipes[0] = pipe;

			ProcessWrapper ff_pipe_process = ffAudioPipe.getPipeProcess();
			pw.attachProcess(ff_pipe_process);
			ff_pipe_process.runInNewThread();

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}

			ffAudioPipe.deleteLater();
			pw.attachProcess(ffAudio);
			ffAudio.runInNewThread();
		}

		pw.runInNewThread();
		return pw;
	}

	private JCheckBox multithreading;

	@Override
	public JComponent config() {
		return config("NetworkTab.5");
	}

	protected JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		multithreading = new JCheckBox(Messages.getString("MEncoderVideo.35"));
		multithreading.setContentAreaFilled(false);
		if (PMS.getConfiguration().isFfmpegMultithreading()) {
			multithreading.setSelected(true);
		}
		multithreading.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				PMS.getConfiguration().setFfmpegMultithreading(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(multithreading, cc.xy(2, 3));

		return builder.getPanel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (resource == null || resource.getFormat().getType() != Format.VIDEO) {
			return false;
		}

		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized DLNAMediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The resource needs a subtitle, but this engine implementation does not support subtitles yet
			return false;
		}

		try {
			String audioTrackName = resource.getMediaAudio().toString();
			String defaultAudioTrackName = resource.getMedia().getAudioTracksList().get(0).toString();

			if (!audioTrackName.equals(defaultAudioTrackName)) {
				// This engine implementation only supports playback of the default audio track at this time
				return false;
			}
		} catch (NullPointerException e) {
			LOGGER.trace("FFmpeg cannot determine compatibility based on audio track for " + resource.getSystemName());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.trace("FFmpeg cannot determine compatibility based on default audio track for " + resource.getSystemName());
		}

		Format format = resource.getFormat();

		if (format != null) {
			Format.Identifier id = format.getIdentifier();

			if (id.equals(Format.Identifier.MKV) || id.equals(Format.Identifier.MPG)) {
				return true;
			}
		}

		return false;
	}
}
