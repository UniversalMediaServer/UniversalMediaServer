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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.ExecutableInfo;
import net.pms.configuration.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.configuration.ExternalProgramInfo;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.*;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.newgui.GuiUtil;
import net.pms.platform.windows.NTStatus;
import net.pms.util.CodecUtil;
import net.pms.util.FormLayoutUtil;
import net.pms.util.PlayerUtil;
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsMuxeRVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(TsMuxeRVideo.class);
	public static final PlayerId ID = StandardPlayerId.TSMUXER_VIDEO;

	/** The {@link Configuration} key for the custom tsMuxeR path. */
	public static final String KEY_TSMUXER_PATH     = "tsmuxer_path";

	/** The {@link Configuration} key for the tsMuxeR executable type. */
	public static final String KEY_TSMUXER_EXECUTABLE_TYPE = "tsmuxer_executable_type";
	public static final String NAME = "tsMuxeR Video";

	private static final String COL_SPEC = "left:pref, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow";

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
	public PlayerId id() {
		return ID;
	}

	@Override
	public String getConfigurablePathKey() {
		return KEY_TSMUXER_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_TSMUXER_EXECUTABLE_TYPE;
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
	protected ExternalProgramInfo programInfo() {
		return configuration.getTsMuxeRPaths();
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.getMediaRenderer();
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
			String timeEndValue2 = "" + params.getTimeEnd();
			if (params.getTimeEnd() < 1) {
				timeEndValue1 = "-y";
				timeEndValue2 = "-y";
			}

			ffmpegCommands = new String[] {
				configuration.getFFmpegPath(),
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
			ffparams.setMaxBufferSize(1);
			ffVideo = new ProcessWrapperImpl(ffmpegCommands, ffparams);

			if (
				filename.toLowerCase().endsWith(".flac") &&
				media.getFirstAudioTrack().getBitsperSample() >= 24 &&
				media.getFirstAudioTrack().getSampleRate() % 48000 == 0
			) {
				ffAudioPipe = new PipeIPCProcess[1];
				ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "flacaudio", System.currentTimeMillis() + "audioout", false, true);

				String[] flacCmd = new String[] {
					configuration.getFLACDefaultPath(),
					"--output-name=" + ffAudioPipe[0].getInputPipe(),
					"-d",
					"-f",
					"-F",
					filename
				};

				ffparams = new OutputParams(configuration);
				ffparams.setMaxBufferSize(1);
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
					configuration.getFFmpegPath(),
					"-i", filename,
					"-ar", rate,
					"-f", "wav",
					"-acodec", depth,
					"-y",
					ffAudioPipe[0].getInputPipe()
				};

				ffparams = new OutputParams(configuration);
				ffparams.setMaxBufferSize(1);
				ffAudio = new ProcessWrapperImpl[1];
				ffAudio[0] = new ProcessWrapperImpl(flacCmd, ffparams);
			}
		} else {
			params.setWaitBeforeStart(5000);
			params.manageFastStart();

			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			ffmpegCommands = new String[] {
				configuration.getFFmpegPath(),
				"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
				"-i", filename,
				"-c", "copy",
				"-f", "rawvideo",
				"-y",
				ffVideoPipe.getInputPipe()
			};

			InputFile newInput = new InputFile();
			newInput.setFilename(filename);
			newInput.setPush(params.getStdIn());

			/**
			 * Note: This logic is weird; on one hand we check if the renderer requires videos to be Level 4.1 or below, but then
			 * the other function allows the video to exceed those limits.
			 * In reality this won't cause problems since renderers typically don't support above 4.1 anyway - nor are many
			 * videos encoded higher than that either - but it's worth acknowledging the logic discrepancy.
			 */
			if (!media.isVideoWithinH264LevelLimits(newInput, params.getMediaRenderer()) && params.getMediaRenderer().isH264Level41Limited()) {
				LOGGER.info("The video will not play or will show a black screen");
			}

			if (media.getH264AnnexB() != null && media.getH264AnnexB().length > 0) {
				StreamModifier sm = new StreamModifier();
				sm.setHeader(media.getH264AnnexB());
				sm.setH264AnnexB(true);
				ffVideoPipe.setModifier(sm);
			}

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.setMaxBufferSize(1);
			ffparams.setStdIn(params.getStdIn());
			ffVideo = new ProcessWrapperImpl(ffmpegCommands, ffparams);

			int numAudioTracks = 1;

			if (media.getAudioTracksList() != null && media.getAudioTracksList().size() > 1 && configuration.isMuxAllAudioTracks()) {
				numAudioTracks = media.getAudioTracksList().size();
			}

			boolean singleMediaAudio = media.getAudioTracksList().size() <= 1;

			if (params.getAid() != null) {
				boolean ac3Remux;
				boolean dtsRemux;
				boolean encodedAudioPassthrough;
				boolean pcm;

				if (numAudioTracks <= 1) {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.getAid().isNonPCMEncodedAudio() && params.getMediaRenderer().isWrapEncodedAudioIntoPCM();
					ac3Remux = params.getAid().isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.getMediaRenderer().isTranscodeToAAC();
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && params.getAid().isDTS() && params.getMediaRenderer().isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						media.isValidForLPCMTranscoding() &&
						(
							params.getAid().isLossless() ||
							(params.getAid().isDTS() && params.getAid().getAudioProperties().getNumberOfChannels() <= 6) ||
							params.getAid().isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									params.getAid().isAC3() ||
									params.getAid().isMP3() ||
									params.getAid().isAAC() ||
									params.getAid().isVorbis() ||
									// params.aid.isWMA() ||
									params.getAid().isMpegAudio()
								)
							)
						) && params.getMediaRenderer().isLPCMPlayable();

					int channels;
					if (ac3Remux) {
						channels = params.getAid().getAudioProperties().getNumberOfChannels(); // AC-3 remux
					} else if (dtsRemux || encodedAudioPassthrough) {
						channels = 2;
					} else if (pcm) {
						channels = params.getAid().getAudioProperties().getNumberOfChannels();
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
						sm.setSampleFrequency(params.getAid().getSampleRate() < 48000 ? 48000 : params.getAid().getSampleRate());
						sm.setBitsPerSample(16);

						ffmpegCommands = new String[] {
							configuration.getFFmpegPath(),
							"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
							"-i", filename,
							"-ac", "" + sm.getNbChannels(),
							"-f", "ac3",
							"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};

						// Use PCM trick when media renderer does not support DTS in MPEG
						if (!params.getMediaRenderer().isMuxDTSToMpeg()) {
							ffAudioPipe[0].setModifier(sm);
						}
					} else if (!ac3Remux && params.getMediaRenderer().isTranscodeToAAC()) {
						// AAC audio
						ffmpegCommands = new String[] {
							configuration.getFFmpegPath(),
							"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
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
							configuration.getFFmpegPath(),
							"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
							"-i", filename,
							"-ac", "" + channels,
							"-f", "ac3",
							"-c:a", (ac3Remux) ? "copy" : "ac3",
							"-ab", String.valueOf(CodecUtil.getAC3Bitrate(configuration, params.getAid())) + "k",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};
					}

					ffparams = new OutputParams(configuration);
					ffparams.setMaxBufferSize(1);
					ffparams.setStdIn(params.getStdIn());
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					ffAudio[0] = new ProcessWrapperImpl(ffmpegCommands, ffparams);
				} else {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					for (int i = 0; i < media.getAudioTracksList().size(); i++) {
						DLNAMediaAudio audio = media.getAudioTracksList().get(i);
						ffAudioPipe[i] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpeg" + i, System.currentTimeMillis() + "audioout" + i, false, true);

						encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.getAid().isNonPCMEncodedAudio() && params.getMediaRenderer().isWrapEncodedAudioIntoPCM();
						ac3Remux = audio.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.getMediaRenderer().isTranscodeToAAC();
						dtsRemux = configuration.isAudioEmbedDtsInPcm() && audio.isDTS() && params.getMediaRenderer().isDTSPlayable() && !encodedAudioPassthrough;

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
							) && params.getMediaRenderer().isLPCMPlayable();

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
							if (!params.getMediaRenderer().isMuxDTSToMpeg()) {
								ffAudioPipe[i].setModifier(sm);
							}

							ffmpegCommands = new String[] {
								configuration.getFFmpegPath(),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
								"-i", filename,
								"-ac", "" + sm.getNbChannels(),
								"-f", "ac3",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracksList().indexOf(audio))),
								"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
						} else if (!ac3Remux && params.getMediaRenderer().isTranscodeToAAC()) {
							// AAC audio
							ffmpegCommands = new String[] {
								configuration.getFFmpegPath(),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
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
								configuration.getFFmpegPath(),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
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
						ffparams.setMaxBufferSize(1);
						ffparams.setStdIn(params.getStdIn());
						ffAudio[i] = new ProcessWrapperImpl(ffmpegCommands, ffparams);
					}
				}
			}
		}

		File f = new File(configuration.getTempFolder(), "pms-tsmuxer.meta");
		params.setLog(false);
		try (PrintWriter pw = new PrintWriter(f)) {
			pw.print("MUXOPT --no-pcr-on-video-pid");
			pw.print(" --new-audio-pes");
			pw.print(" --no-asyncio");
			pw.print(" --vbr");
			pw.println(" --vbv-len=500");

			String sei = "insertSEI";
			if (
				params.getMediaRenderer().isPS3() &&
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

				encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.getAid().isNonPCMEncodedAudio() && params.getMediaRenderer().isWrapEncodedAudioIntoPCM();
				ac3Remux = params.getAid().isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !params.getMediaRenderer().isTranscodeToAAC();
				dtsRemux = configuration.isAudioEmbedDtsInPcm() && params.getAid().isDTS() && params.getMediaRenderer().isDTSPlayable() && !encodedAudioPassthrough;

				pcm = configuration.isAudioUsePCM() &&
					media.isValidForLPCMTranscoding() &&
					(
						params.getAid().isLossless() ||
						(params.getAid().isDTS() && params.getAid().getAudioProperties().getNumberOfChannels() <= 6) ||
						params.getAid().isTrueHD() ||
						(
							!configuration.isMencoderUsePcmForHQAudioOnly() &&
							(
								params.getAid().isAC3() ||
								params.getAid().isMP3() ||
								params.getAid().isAAC() ||
								params.getAid().isVorbis() ||
								// params.aid.isWMA() ||
								params.getAid().isMpegAudio()
							)
						)
					) && params.getMediaRenderer().isLPCMPlayable();
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
						if (params.getMediaRenderer().isMuxDTSToMpeg()) {
							type = "A_DTS";
						}
					}
				}
				if (params.getAid() != null && params.getAid().getAudioProperties().getAudioDelay() != 0 && params.getTimeSeek() == 0) {
					timeshift = "timeshift=" + params.getAid().getAudioProperties().getAudioDelay() + "ms, ";
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

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && params.getAid().isNonPCMEncodedAudio() && params.getMediaRenderer().isWrapEncodedAudioIntoPCM();
					ac3Remux = lang.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough;
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && lang.isDTS() && params.getMediaRenderer().isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						media.isValidForLPCMTranscoding() &&
						(
							lang.isLossless() ||
							(lang.isDTS() && lang.getAudioProperties().getNumberOfChannels() <= 6) ||
							lang.isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									params.getAid().isAC3() ||
									params.getAid().isMP3() ||
									params.getAid().isAAC() ||
									params.getAid().isVorbis() ||
									// params.aid.isWMA() ||
									params.getAid().isMpegAudio()
								)
							)
						) && params.getMediaRenderer().isLPCMPlayable();
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
							if (params.getMediaRenderer().isMuxDTSToMpeg()) {
								type = "A_DTS";
							}
						}
					}
					if (lang.getAudioProperties().getAudioDelay() != 0 && params.getTimeSeek() == 0) {
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
		String executable = getExecutable();
		if (params.getMediaRenderer().isPS3()) {
			executable = configuration.getTsMuxeRNewPath();
		}

		String[] cmdArray = new String[]{
			executable,
			f.getAbsolutePath(),
			tsPipe.getInputPipe()
		};

		ProcessWrapperImpl p = new ProcessWrapperImpl(cmdArray, params);
		params.setMaxBufferSize(100);
		params.getInputPipes()[0] = tsPipe;
		params.setStdIn(null);
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

		if (ffAudioPipe != null && params.getAid() != null) {
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
		return NAME;
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

	@Override
	public @Nullable ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		final String arg = "-v";
		ExecutableInfoBuilder result = executableInfo.modify();
		try {
			ListProcessWrapperResult output = SimpleProcessWrapper.runProcessListOutput(
				30000,
				1000,
				executableInfo.getPath().toString(),
				arg
			);
			if (output.getError() != null) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("Engine.Error"), this) + " \n" + output.getError().getMessage());
				result.available(Boolean.FALSE);
				LOGGER.debug("\"{} {}\" failed with error: {}", executableInfo.getPath(), arg, output.getError().getMessage());
				return result.build();
			}
			if (output.getExitCode() == 0) {
				if (output.getOutput() != null && output.getOutput().size() > 0) {
					Pattern pattern = Pattern.compile("tsMuxeR\\.\\s+Version\\s(\\S+)\\s+", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(output.getOutput().get(0));
					if (matcher.find() && isNotBlank(matcher.group(1))) {
						result.version(new Version(matcher.group(1)));
					}
				}
				result.available(Boolean.TRUE);
			} else {
				NTStatus ntStatus = Platform.isWindows() ? NTStatus.typeOf(output.getExitCode()) : null;
				if (ntStatus != null) {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("Engine.Error"), this) + "\n\n" + ntStatus);
				} else {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("Engine.ExitCode"), this, output.getExitCode()));
					if (Platform.isLinux() && Platform.is64Bit()) {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(result.errorText() + ". \n" + Messages.getString("Engine.tsMuxerErrorLinux"));
					}
					result.available(Boolean.FALSE);
				}
			}
		} catch (InterruptedException e) {
			return null;
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
