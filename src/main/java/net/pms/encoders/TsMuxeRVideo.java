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
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.*;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.newgui.GuiUtil;
import net.pms.util.CodecUtil;
import net.pms.util.FormLayoutUtil;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsMuxeRVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(TsMuxeRVideo.class);
	private static final String COL_SPEC = "left:pref, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow";

	public static final String ID = "TSMuxer";

	@Deprecated
	public TsMuxeRVideo(PmsConfiguration configuration) {
		this();
	}

	public TsMuxeRVideo() {
	}

	@Override
	public boolean excludeFormat(Format format) {
		String extension = format.getMatchedExtension();
		return extension != null
			&& !extension.equals("mp4")
			&& !extension.equals("mkv")
			&& !extension.equals("ts")
			&& !extension.equals("tp")
			&& !extension.equals("m2ts")
			&& !extension.equals("m2t")
			&& !extension.equals("mpg")
			&& !extension.equals("evo")
			&& !extension.equals("mpeg")
			&& !extension.equals("vob")
			&& !extension.equals("m2v")
			&& !extension.equals("mts")
			&& !extension.equals("mov");
	}

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
	public String[] args() {
		return null;
	}

	@Override
	public String executable() {
		return configuration.getTsmuxerPath();
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.mediaRenderer;
		final String filename = dlna.getFileName();
		setAudioAndSubs(filename, media, params);

		PipeIPCProcess ffVideoPipe;
		ProcessWrapperImpl ffVideo;

		PipeIPCProcess ffAudioPipe[] = null;
		ProcessWrapperImpl ffAudio[] = null;

		String fps = media.getValidFps(false);

		int width  = media.getWidth();
		int height = media.getHeight();
		if (width < 320 || height < 240) {
			width  = -1;
			height = -1;
		}

		String videoType = "V_MPEG4/ISO/AVC";
		if (media.getCodecV() != null && media.getCodecV().startsWith("mpeg2")) {
			videoType = "V_MPEG-2";
		}

		boolean aacTranscode = false;

		String[] ffmpegCommands;
		if (this instanceof TsMuxeRAudio && media.getFirstAudioTrack() != null) {
			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "fakevideo", System.currentTimeMillis() + "videoout", false, true);

			String timeEndValue1 = "-t";
			String timeEndValue2 = "" + params.timeend;
			if (params.timeend < 1) {
				timeEndValue1 = "-y";
				timeEndValue2 = "-y";
			}

			ffmpegCommands = new String[] {
				configuration.getFfmpegPath(),
				timeEndValue1, timeEndValue2,
				"-loop", "1",
				"-i", "DummyInput.jpg",
				"-f", "h264",
				"-c:v", "libx264",
				"-level", "31",
				"-tune", "zerolatency",
				"-pix_fmt", "yuv420p",
				"-an",
				"-y",
				ffVideoPipe.getInputPipe()
			};

			videoType = "V_MPEG4/ISO/AVC";

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.maxBufferSize = 1;
			ffVideo = new ProcessWrapperImpl(ffmpegCommands, ffparams);

			if (
				filename.toLowerCase().endsWith(".flac") &&
				media.getFirstAudioTrack().getBitsperSample() >= 24 &&
				media.getFirstAudioTrack().getSampleRate() % 48000 == 0
			) {
				ffAudioPipe = new PipeIPCProcess[1];
				ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "flacaudio", System.currentTimeMillis() + "audioout", false, true);

				String[] flacCmd = new String[] {
					configuration.getFlacPath(),
					"--output-name=" + ffAudioPipe[0].getInputPipe(),
					"-d",
					"-f",
					"-F",
					filename
				};

				ffparams = new OutputParams(configuration);
				ffparams.maxBufferSize = 1;
				ffAudio = new ProcessWrapperImpl[1];
				ffAudio[0] = new ProcessWrapperImpl(flacCmd, ffparams);
			} else {
				ffAudioPipe = new PipeIPCProcess[1];
				ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "mlpaudio", System.currentTimeMillis() + "audioout", false, true);
				String depth = "pcm_s16le";
				String rate = "48000";

				if (media.getFirstAudioTrack().getBitsperSample() >= 24) {
					depth = "pcm_s24le";
				}

				if (media.getFirstAudioTrack().getSampleRate() > 48000) {
					rate = "" + media.getFirstAudioTrack().getSampleRate();
				}

				String[] flacCmd = new String[] {
					configuration.getFfmpegPath(),
					"-i", filename,
					"-ar", rate,
					"-f", "wav",
					"-acodec", depth,
					"-y",
					ffAudioPipe[0].getInputPipe()
				};

				ffparams = new OutputParams(configuration);
				ffparams.maxBufferSize = 1;
				ffAudio = new ProcessWrapperImpl[1];
				ffAudio[0] = new ProcessWrapperImpl(flacCmd, ffparams);
			}
		} else {
			params.waitbeforestart = 5000;
			params.manageFastStart();

			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			ffmpegCommands = new String[] {
				configuration.getFfmpegPath(),
				"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
				"-i", filename,
				"-c", "copy",
				"-f", "rawvideo",
				"-y",
				ffVideoPipe.getInputPipe()
			};

			InputFile newInput = new InputFile();
			newInput.setFilename(filename);
			newInput.setPush(params.stdin);

			/**
			 * Note: This logic is weird; on one hand we check if the renderer requires videos to be Level 4.1 or below, but then
			 * the other function allows the video to exceed those limits.
			 * In reality this won't cause problems since renderers typically don't support above 4.1 anyway - nor are many
			 * videos encoded higher than that either - but it's worth acknowledging the logic discrepancy.
			 */
			if (!media.isVideoWithinH264LevelLimits(newInput, params.mediaRenderer) && params.mediaRenderer.isH264Level41Limited()) {
				LOGGER.info("The video will not play or will show a black screen");
			}

			if (media.getH264AnnexB() != null && media.getH264AnnexB().length > 0) {
				StreamModifier sm = new StreamModifier();
				sm.setHeader(media.getH264AnnexB());
				sm.setH264AnnexB(true);
				ffVideoPipe.setModifier(sm);
			}

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.maxBufferSize = 1;
			ffparams.stdin = params.stdin;
			ffVideo = new ProcessWrapperImpl(ffmpegCommands, ffparams);

			int numAudioTracks = 1;

			if (media.getAudioTracksList() != null && media.getAudioTracksList().size() > 1 && configuration.isMuxAllAudioTracks()) {
				numAudioTracks = media.getAudioTracksList().size();
			}

			boolean singleMediaAudio = media.getAudioTracksList().size() <= 1;

			if (params.aid != null) {
				boolean ac3Remux;
				boolean dtsRemux;
				boolean encodedAudioPassthrough;
				boolean pcm;

				if (numAudioTracks <= 1) {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.aid.isNonPCMEncodedAudio() && params.mediaRenderer.isWrapEncodedAudioIntoPCM();
					ac3Remux = params.aid.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.mediaRenderer.isTranscodeToAAC();
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && params.aid.isDTS() && params.mediaRenderer.isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						media.isValidForLPCMTranscoding() &&
						(
							params.aid.isLossless() ||
							(params.aid.isDTS() && params.aid.getAudioProperties().getNumberOfChannels() <= 6) ||
							params.aid.isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									params.aid.isAC3() ||
									params.aid.isMP3() ||
									params.aid.isAAC() ||
									params.aid.isVorbis() ||
									// params.aid.isWMA() ||
									params.aid.isMpegAudio()
								)
							)
						) && params.mediaRenderer.isLPCMPlayable();

					int channels;
					if (ac3Remux) {
						channels = params.aid.getAudioProperties().getNumberOfChannels(); // AC-3 remux
					} else if (dtsRemux || encodedAudioPassthrough) {
						channels = 2;
					} else if (pcm) {
						channels = params.aid.getAudioProperties().getNumberOfChannels();
					} else {
						channels = configuration.getAudioChannelCount(); // 5.1 max for AC-3 encoding
					}

					if (!ac3Remux && (dtsRemux || pcm || encodedAudioPassthrough)) {
						// DTS remux or LPCM
						StreamModifier sm = new StreamModifier();
						sm.setPcm(pcm);
						sm.setDtsEmbed(dtsRemux);
						sm.setEncodedAudioPassthrough(encodedAudioPassthrough);
						sm.setNbChannels(channels);
						sm.setSampleFrequency(params.aid.getSampleRate() < 48000 ? 48000 : params.aid.getSampleRate());
						sm.setBitsPerSample(16);

						ffmpegCommands = new String[] {
							configuration.getFfmpegPath(),
							"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
							"-i", filename,
							"-ac", "" + sm.getNbChannels(),
							"-f", "ac3",
							"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};

						// Use PCM trick when media renderer does not support DTS in MPEG
						if (!params.mediaRenderer.isMuxDTSToMpeg()) {
							ffAudioPipe[0].setModifier(sm);
						}
					} else if (!ac3Remux && params.mediaRenderer.isTranscodeToAAC()) {
						// AAC audio
						ffmpegCommands = new String[] {
							configuration.getFfmpegPath(),
							"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
							"-i", filename,
							"-ac", "" + channels,
							"-f", "adts",
							"-c:a", "aac",
							"-ab", Math.min(configuration.getAudioBitrate(), 320) + "k",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};
						aacTranscode = true;
					} else {
						// AC-3 audio
						ffmpegCommands = new String[] {
							configuration.getFfmpegPath(),
							"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
							"-i", filename,
							"-ac", "" + channels,
							"-f", "ac3",
							"-c:a", (ac3Remux) ? "copy" : "ac3",
							"-ab", String.valueOf(CodecUtil.getAC3Bitrate(configuration, params.aid)) + "k",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};
					}

					ffparams = new OutputParams(configuration);
					ffparams.maxBufferSize = 1;
					ffparams.stdin = params.stdin;
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					ffAudio[0] = new ProcessWrapperImpl(ffmpegCommands, ffparams);
				} else {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					for (int i = 0; i < media.getAudioTracksList().size(); i++) {
						DLNAMediaAudio audio = media.getAudioTracksList().get(i);
						ffAudioPipe[i] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpeg" + i, System.currentTimeMillis() + "audioout" + i, false, true);

						encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.aid.isNonPCMEncodedAudio() && params.mediaRenderer.isWrapEncodedAudioIntoPCM();
						ac3Remux = audio.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.mediaRenderer.isTranscodeToAAC();
						dtsRemux = configuration.isAudioEmbedDtsInPcm() && audio.isDTS() && params.mediaRenderer.isDTSPlayable() && !encodedAudioPassthrough;

						pcm = configuration.isAudioUsePCM() &&
							media.isValidForLPCMTranscoding() &&
							(
								audio.isLossless() ||
								(audio.isDTS() && audio.getAudioProperties().getNumberOfChannels() <= 6) ||
								audio.isTrueHD() ||
								(
									!configuration.isMencoderUsePcmForHQAudioOnly() &&
									(
										audio.isAC3() ||
										audio.isMP3() ||
										audio.isAAC() ||
										audio.isVorbis() ||
										// audio.isWMA() ||
										audio.isMpegAudio()
									)
								)
							) && params.mediaRenderer.isLPCMPlayable();

						int channels;
						if (ac3Remux) {
							channels = audio.getAudioProperties().getNumberOfChannels(); // AC-3 remux
						} else if (dtsRemux || encodedAudioPassthrough) {
							channels = 2;
						} else if (pcm) {
							channels = audio.getAudioProperties().getNumberOfChannels();
						} else {
							channels = configuration.getAudioChannelCount(); // 5.1 max for AC-3 encoding
						}

						if (!ac3Remux && (dtsRemux || pcm || encodedAudioPassthrough)) {
							// DTS remux or LPCM
							StreamModifier sm = new StreamModifier();
							sm.setPcm(pcm);
							sm.setDtsEmbed(dtsRemux);
							sm.setEncodedAudioPassthrough(encodedAudioPassthrough);
							sm.setNbChannels(channels);
							sm.setSampleFrequency(audio.getSampleRate() < 48000 ? 48000 : audio.getSampleRate());
							sm.setBitsPerSample(16);
							if (!params.mediaRenderer.isMuxDTSToMpeg()) {
								ffAudioPipe[i].setModifier(sm);
							}

							ffmpegCommands = new String[] {
								configuration.getFfmpegPath(),
								"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
								"-i", filename,
								"-ac", "" + sm.getNbChannels(),
								"-f", "ac3",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracksList().indexOf(audio))),
								"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
						} else if (!ac3Remux && params.mediaRenderer.isTranscodeToAAC()) {
							// AAC audio
							ffmpegCommands = new String[] {
								configuration.getFfmpegPath(),
								"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
								"-i", filename,
								"-ac", "" + channels,
								"-f", "adts",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracksList().indexOf(audio))),
								"-c:a", "aac",
								"-ab", Math.min(configuration.getAudioBitrate(), 320) + "k",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
							aacTranscode = true;
						} else {
							// AC-3 remux or encoding
							ffmpegCommands = new String[] {
								configuration.getFfmpegPath(),
								"-ss", params.timeseek > 0 ? "" + params.timeseek : "0",
								"-i", filename,
								"-ac", "" + channels,
								"-f", "ac3",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracksList().indexOf(audio))),
								"-c:a", (ac3Remux) ? "copy" : "ac3",
								"-ab", String.valueOf(CodecUtil.getAC3Bitrate(configuration, audio)) + "k",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
						}

						ffparams = new OutputParams(configuration);
						ffparams.maxBufferSize = 1;
						ffparams.stdin = params.stdin;
						ffAudio[i] = new ProcessWrapperImpl(ffmpegCommands, ffparams);
					}
				}
			}
		}

		File f = new File(configuration.getTempFolder(), "pms-tsmuxer.meta");
		params.log = false;
		try (PrintWriter pw = new PrintWriter(f)) {
			pw.print("MUXOPT --no-pcr-on-video-pid");
			pw.print(" --new-audio-pes");
			pw.print(" --no-asyncio");
			pw.print(" --vbr");
			pw.println(" --vbv-len=500");

			String sei = "insertSEI";
			if (
				params.mediaRenderer.isPS3() &&
				media.isWebDl(filename, params)
			) {
				sei = "forceSEI";
			}
			String videoparams = "level=4.1, " + sei + ", contSPS, track=1";
			if (this instanceof TsMuxeRAudio) {
				videoparams = "track=224";
			}
			if (configuration.isFix25FPSAvMismatch()) {
				fps = "25";
			}
			pw.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + (fps != null ? ("fps=" + fps + ", ") : "") + (width != -1 ? ("video-width=" + width + ", ") : "") + (height != -1 ? ("video-height=" + height + ", ") : "") + videoparams);

			if (ffAudioPipe != null && ffAudioPipe.length == 1) {
				String timeshift = "";
				boolean ac3Remux;
				boolean dtsRemux;
				boolean encodedAudioPassthrough;
				boolean pcm;

				encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.aid.isNonPCMEncodedAudio() && params.mediaRenderer.isWrapEncodedAudioIntoPCM();
				ac3Remux = params.aid.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.mediaRenderer.isTranscodeToAAC();
				dtsRemux = configuration.isAudioEmbedDtsInPcm() && params.aid.isDTS() && params.mediaRenderer.isDTSPlayable() && !encodedAudioPassthrough;

				pcm = configuration.isAudioUsePCM() &&
					media.isValidForLPCMTranscoding() &&
					(
						params.aid.isLossless() ||
						(params.aid.isDTS() && params.aid.getAudioProperties().getNumberOfChannels() <= 6) ||
						params.aid.isTrueHD() ||
						(
							!configuration.isMencoderUsePcmForHQAudioOnly() &&
							(
								params.aid.isAC3() ||
								params.aid.isMP3() ||
								params.aid.isAAC() ||
								params.aid.isVorbis() ||
								// params.aid.isWMA() ||
								params.aid.isMpegAudio()
							)
						)
					) && params.mediaRenderer.isLPCMPlayable();
				String type = "A_AC3";
				if (ac3Remux) {
					// AC-3 remux takes priority
					type = "A_AC3";
				} else if (aacTranscode) {
					type = "A_AAC";
				} else {
					if (pcm || this instanceof TsMuxeRAudio) {
						type = "A_LPCM";
					}
					if (encodedAudioPassthrough || this instanceof TsMuxeRAudio) {
						type = "A_LPCM";
					}
					if (dtsRemux || this instanceof TsMuxeRAudio) {
						type = "A_LPCM";
						if (params.mediaRenderer.isMuxDTSToMpeg()) {
							type = "A_DTS";
						}
					}
				}
				if (params.aid != null && params.aid.getAudioProperties().getAudioDelay() != 0 && params.timeseek == 0) {
					timeshift = "timeshift=" + params.aid.getAudioProperties().getAudioDelay() + "ms, ";
				}
				pw.println(type + ", \"" + ffAudioPipe[0].getOutputPipe() + "\", " + timeshift + "track=2");
			} else if (ffAudioPipe != null) {
				for (int i = 0; i < media.getAudioTracksList().size(); i++) {
					DLNAMediaAudio lang = media.getAudioTracksList().get(i);
					String timeshift = "";
					boolean ac3Remux;
					boolean dtsRemux;
					boolean encodedAudioPassthrough;
					boolean pcm;

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.aid.isNonPCMEncodedAudio() && params.mediaRenderer.isWrapEncodedAudioIntoPCM();
					ac3Remux = lang.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough;
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && lang.isDTS() && params.mediaRenderer.isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						media.isValidForLPCMTranscoding() &&
						(
							lang.isLossless() ||
							(lang.isDTS() && lang.getAudioProperties().getNumberOfChannels() <= 6) ||
							lang.isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									params.aid.isAC3() ||
									params.aid.isMP3() ||
									params.aid.isAAC() ||
									params.aid.isVorbis() ||
									// params.aid.isWMA() ||
									params.aid.isMpegAudio()
								)
							)
						) && params.mediaRenderer.isLPCMPlayable();
					String type = "A_AC3";
					if (ac3Remux) {
						// AC-3 remux takes priority
						type = "A_AC3";
					} else {
						if (pcm) {
							type = "A_LPCM";
						}
						if (encodedAudioPassthrough) {
							type = "A_LPCM";
						}
						if (dtsRemux) {
							type = "A_LPCM";
							if (params.mediaRenderer.isMuxDTSToMpeg()) {
								type = "A_DTS";
							}
						}
					}
					if (lang.getAudioProperties().getAudioDelay() != 0 && params.timeseek == 0) {
						timeshift = "timeshift=" + lang.getAudioProperties().getAudioDelay() + "ms, ";
					}
					pw.println(type + ", \"" + ffAudioPipe[i].getOutputPipe() + "\", " + timeshift + "track=" + (2 + i));
				}
			}
		}

		PipeProcess tsPipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

		/**
		 * Use the newer version of tsMuxeR on PS3 since other renderers
		 * like Panasonic TVs don't always recognize the new output
		 */
		String executable = executable();
		if (params.mediaRenderer.isPS3()) {
			executable = configuration.getTsmuxerNewPath();
		}

		String[] cmdArray = new String[]{
			executable,
			f.getAbsolutePath(),
			tsPipe.getInputPipe()
		};

		ProcessWrapperImpl p = new ProcessWrapperImpl(cmdArray, params);
		params.maxBufferSize = 100;
		params.input_pipes[0] = tsPipe;
		params.stdin = null;
		ProcessWrapper pipe_process = tsPipe.getPipeProcess();
		p.attachProcess(pipe_process);
		pipe_process.runInNewThread();

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		tsPipe.deleteLater();

		ProcessWrapper ff_pipe_process = ffVideoPipe.getPipeProcess();
		p.attachProcess(ff_pipe_process);
		ff_pipe_process.runInNewThread();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		ffVideoPipe.deleteLater();

		p.attachProcess(ffVideo);
		ffVideo.runInNewThread();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}

		if (ffAudioPipe != null && params.aid != null) {
			for (int i = 0; i < ffAudioPipe.length; i++) {
				ff_pipe_process = ffAudioPipe[i].getPipeProcess();
				p.attachProcess(ff_pipe_process);
				ff_pipe_process.runInNewThread();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				ffAudioPipe[i].deleteLater();
				p.attachProcess(ffAudio[i]);
				ffAudio[i].runInNewThread();
			}
		}

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}

		p.runInNewThread();
		configuration = prev;
		return p;
	}

	@Override
	public String mimeType() {
		return "video/mpeg";
	}

	@Override
	public String name() {
		return "tsMuxeR";
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}
	private JCheckBox tsmuxerforcefps;
	private JCheckBox muxallaudiotracks;

	@Override
	public JComponent config() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();


		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(2, 1, 1), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		tsmuxerforcefps = new JCheckBox(Messages.getString("TsMuxeRVideo.2"), configuration.isTsmuxerForceFps());
		tsmuxerforcefps.setContentAreaFilled(false);
		tsmuxerforcefps.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setTsmuxerForceFps(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(tsmuxerforcefps), FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		muxallaudiotracks = new JCheckBox(Messages.getString("TsMuxeRVideo.19"), configuration.isMuxAllAudioTracks());
		muxallaudiotracks.setContentAreaFilled(false);
		muxallaudiotracks.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setMuxAllAudioTracks(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(muxallaudiotracks), FormLayoutUtil.flip(cc.xy(2, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	@Override
	public boolean isInternalSubtitlesSupported() {
		return false;
	}

	@Override
	public boolean isExternalSubtitlesSupported() {
		return false;
	}

	@Override
	public boolean isPlayerCompatible(RendererConfiguration mediaRenderer) {
		return mediaRenderer != null && mediaRenderer.isMuxH264MpegTS();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized DLNAMediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The resource needs a subtitle, but PMS does not support subtitles for tsMuxeR.
			return false;
		}

		try {
			String audioTrackName = resource.getMediaAudio().toString();
			String defaultAudioTrackName = resource.getMedia().getAudioTracksList().get(0).toString();

			if (!audioTrackName.equals(defaultAudioTrackName)) {
				// PMS only supports playback of the default audio track for tsMuxeR
				return false;
			}
		} catch (NullPointerException e) {
			LOGGER.trace("tsMuxeR cannot determine compatibility based on audio track for " + resource.getSystemName());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.trace("tsMuxeR cannot determine compatibility based on default audio track for " + resource.getSystemName());
		}

		if (
			PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(resource, Format.Identifier.OGG)
		) {
			return true;
		}

		return false;
	}
}
