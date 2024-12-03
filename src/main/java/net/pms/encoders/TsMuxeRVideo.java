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

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.Messages;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.parsers.FFmpegParser;
import net.pms.platform.PlatformUtils;
import net.pms.platform.windows.NTStatus;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.CodecUtil;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.util.InputFile;
import net.pms.util.PlayerUtil;
import net.pms.util.UMSUtils;
import net.pms.util.Version;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsMuxeRVideo extends Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(TsMuxeRVideo.class);
	public static final EngineId ID = StandardEngineId.TSMUXER_VIDEO;

	/** The {@link Configuration} key for the custom tsMuxeR path. */
	public static final String KEY_TSMUXER_PATH = "tsmuxer_path";

	/** The {@link Configuration} key for the tsMuxeR executable type. */
	public static final String KEY_TSMUXER_EXECUTABLE_TYPE = "tsmuxer_executable_type";
	public static final String NAME = "tsMuxeR Video";

	// Not to be instantiated by anything but PlayerFactory
	TsMuxeRVideo() {
		super(CONFIGURATION.getTsMuxeRPaths());
	}

	@Override
	public boolean excludeFormat(Format format) {
		String extension = format.getMatchedExtension();
		return extension != null &&
			!extension.equals("mp4") &&
			!extension.equals("mkv") &&
			!extension.equals("ts") &&
			!extension.equals("tp") &&
			!extension.equals("m2ts") &&
			!extension.equals("m2t") &&
			!extension.equals("mpg") &&
			!extension.equals("evo") &&
			!extension.equals("mpeg") &&
			!extension.equals("vob") &&
			!extension.equals("m2v") &&
			!extension.equals("mts") &&
			!extension.equals("mov");
	}

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_ENGINE;
	}

	@Override
	public EngineId getEngineId() {
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
	public ProcessWrapper launchTranscode(
		StoreItem item,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		Renderer renderer = params.getMediaRenderer();
		// Use device-specific ums conf
		UmsConfiguration configuration = renderer.getUmsConfiguration();

		final String filename = item.getFileName();
		final EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		setAudioAndSubs(item, params);

		PipeIPCProcess ffVideoPipe;
		ProcessWrapperImpl ffVideo;

		PipeIPCProcess[] ffAudioPipe = null;
		ProcessWrapperImpl[] ffAudio = null;

		MediaVideo defaultVideoTrack = media.getDefaultVideoTrack();
		MediaAudio audioTrack = params.getAid();
		String fps = getValidFps(media.getFrameRate(), false);

		String videoType = "V_MPEG4/ISO/AVC";
		String codecV = defaultVideoTrack.getCodec();
		if (codecV != null) {
			if (codecV.equals(FormatConfiguration.MPEG2)) {
				videoType = "V_MPEG-2";
			} else if (codecV.equals(FormatConfiguration.H265)) {
				videoType = "V_MPEGH/ISO/HEVC";
			}
		}

		boolean aacTranscode = false;

		String[] ffmpegCommands;
		if (this instanceof TsMuxeRAudio) {
			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "fakevideo", System.currentTimeMillis() + "videoout", false, true);

			String timeEndValue1 = "-t";
			String timeEndValue2 = "" + params.getTimeEnd();
			if (params.getTimeEnd() < 1) {
				timeEndValue1 = "-y";
				timeEndValue2 = "-y";
			}

			ffmpegCommands = new String[] {
				EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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

			if (audioTrack != null) {
				if (
					filename.toLowerCase().endsWith(".flac") &&
					audioTrack.getBitDepth() >= 24 &&
					audioTrack.getSampleRate() % 48000 == 0
				) {
					ffAudioPipe = new PipeIPCProcess[1];
					ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "flacaudio", System.currentTimeMillis() + "audioout", false, true);

					String[] flacCmd = new String[] {
						configuration.getFLACPath(),
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

					if (audioTrack.getBitDepth() >= 24) {
						depth = "pcm_s24le";
					}

					if (audioTrack.getSampleRate() > 48000) {
						rate = "" + audioTrack.getSampleRate();
					}

					String[] flacCmd = new String[] {
						EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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
			}
		} else {
			params.setWaitBeforeStart(5000);
			params.manageFastStart();

			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			ffmpegCommands = new String[] {
				EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
				"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
				"-i", filename,
				"-c", "copy",
				"-f", FormatConfiguration.H265.equals(codecV) ? "hevc" : "rawvideo",
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
			if (defaultVideoTrack.isH264() && !isVideoWithinH264LevelLimits(defaultVideoTrack, renderer)) {
				LOGGER.info("The video will not play or will show a black screen");
			}

			byte[] h264AnnexB = getH264AnnexB(newInput, defaultVideoTrack);
			if (h264AnnexB != null && h264AnnexB.length > 0) {
				StreamModifier sm = new StreamModifier();
				sm.setHeader(h264AnnexB);
				sm.setH264AnnexB(true);
				ffVideoPipe.setModifier(sm);
			}

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.setMaxBufferSize(1);
			ffparams.setStdIn(params.getStdIn());
			ffVideo = new ProcessWrapperImpl(ffmpegCommands, ffparams);

			int numAudioTracks = 1;

			if (media.getAudioTracks() != null && media.getAudioTracks().size() > 1 && configuration.isMuxAllAudioTracks()) {
				numAudioTracks = media.getAudioTracks().size();
			}

			boolean singleMediaAudio = media.getAudioTracks().size() <= 1;

			if (audioTrack != null) {
				boolean ac3Remux;
				boolean dtsRemux;
				boolean encodedAudioPassthrough;
				boolean pcm;

				if (numAudioTracks <= 1) {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && audioTrack.isNonPCMEncodedAudio() && renderer.isWrapEncodedAudioIntoPCM();
					ac3Remux = audioTrack.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !encodingFormat.isTranscodeToAAC();
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && audioTrack.isDTS() && renderer.isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						isValidForLPCMTranscoding(media) &&
						(
							audioTrack.isLossless() ||
							(audioTrack.isDTS() && audioTrack.getNumberOfChannels() <= 6) ||
							audioTrack.isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									audioTrack.isAC3() ||
									audioTrack.isMP3() ||
									audioTrack.isAAC() ||
									audioTrack.isVorbis() ||
									audioTrack.isMpegAudio()
								)
							)
						) && renderer.isLPCMPlayable();

					int channels;
					if (ac3Remux) {
						channels = audioTrack.getNumberOfChannels(); // AC-3 remux
					} else if (dtsRemux || encodedAudioPassthrough) {
						channels = 2;
					} else if (pcm) {
						channels = audioTrack.getNumberOfChannels();
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
						sm.setSampleFrequency(audioTrack.getSampleRate() < 48000 ? 48000 : audioTrack.getSampleRate());
						sm.setBitsPerSample(16);

						ffmpegCommands = new String[] {
							EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
							"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
							"-i", filename,
							"-ac", "" + sm.getNbChannels(),
							"-f", "ac3",
							"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
							"-y",
							ffAudioPipe[0].getInputPipe()
						};

						// Use PCM trick when media renderer does not support DTS in MPEG
						if (!renderer.isMuxDTSToMpeg()) {
							ffAudioPipe[0].setModifier(sm);
						}
					} else if (!ac3Remux && encodingFormat.isTranscodeToAAC()) {
						// AAC audio
						ffmpegCommands = new String[] {
							EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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
							EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
							"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
							"-i", filename,
							"-ac", "" + channels,
							"-f", "ac3",
							"-c:a", (ac3Remux) ? "copy" : "ac3",
							"-ab", String.valueOf(CodecUtil.getAC3Bitrate(configuration, audioTrack)) + "k",
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
					for (int i = 0; i < media.getAudioTracks().size(); i++) {
						MediaAudio audio = media.getAudioTracks().get(i);
						ffAudioPipe[i] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpeg" + i, System.currentTimeMillis() + "audioout" + i, false, true);

						encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && audioTrack.isNonPCMEncodedAudio() && renderer.isWrapEncodedAudioIntoPCM();
						ac3Remux = audio.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !encodingFormat.isTranscodeToAAC();
						dtsRemux = configuration.isAudioEmbedDtsInPcm() && audio.isDTS() && renderer.isDTSPlayable() && !encodedAudioPassthrough;

						pcm = configuration.isAudioUsePCM() &&
							isValidForLPCMTranscoding(media) &&
							(
								audio.isLossless() ||
								(audio.isDTS() && audio.getNumberOfChannels() <= 6) ||
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
							) && renderer.isLPCMPlayable();

						int channels;
						if (ac3Remux) {
							channels = audio.getNumberOfChannels(); // AC-3 remux
						} else if (dtsRemux || encodedAudioPassthrough) {
							channels = 2;
						} else if (pcm) {
							channels = audio.getNumberOfChannels();
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
							if (!renderer.isMuxDTSToMpeg()) {
								ffAudioPipe[i].setModifier(sm);
							}

							ffmpegCommands = new String[] {
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
								"-i", filename,
								"-ac", "" + sm.getNbChannels(),
								"-f", "ac3",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracks().indexOf(audio))),
								"-c:a", sm.isDtsEmbed() || sm.isEncodedAudioPassthrough() ? "copy" : "pcm",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
						} else if (!ac3Remux && encodingFormat.isTranscodeToAAC()) {
							// AAC audio
							ffmpegCommands = new String[] {
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
								"-i", filename,
								"-ac", "" + channels,
								"-f", "adts",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracks().indexOf(audio))),
								"-c:a", "aac",
								"-ab", Math.min(configuration.getAudioBitrate(), 320) + "k",
								"-y",
								ffAudioPipe[i].getInputPipe()
							};
							aacTranscode = true;
						} else {
							// AC-3 remux or encoding
							ffmpegCommands = new String[] {
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
								"-ss", params.getTimeSeek() > 0 ? "" + params.getTimeSeek() : "0",
								"-i", filename,
								"-ac", "" + channels,
								"-f", "ac3",
								singleMediaAudio ? "-y" : "-map", singleMediaAudio ? "-y" : ("0:a:" + (media.getAudioTracks().indexOf(audio))),
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

		File f = new File(CONFIGURATION.getTempFolder(), "ums-tsmuxer.meta");
		params.setLog(false);
		try (PrintWriter pw = new PrintWriter(f)) {
			pw.print("MUXOPT --no-pcr-on-video-pid");
			pw.print(" --new-audio-pes");
			pw.print(" --no-asyncio");
			pw.print(" --vbr");
			pw.println(" --vbv-len=500");

			String videoparams;
			if (this instanceof TsMuxeRAudio) {
				videoparams = "track=224";
			} else if (encodingFormat.isTranscodeToH264()) {
				String sei = "insertSEI";
				if (
					renderer.isPS3() &&
					isWebDl(filename, media, params)
				) {
					sei = "forceSEI";
				}
				videoparams = "level=4.1, " + sei + ", contSPS, track=1";
			} else {
				videoparams = "track=1";
			}
			if (configuration.isFix25FPSAvMismatch()) {
				fps = "25";
			}
			pw.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + (fps != null ? ("fps=" + fps + ", ") : "") + videoparams);

			if (audioTrack != null && ffAudioPipe != null && ffAudioPipe.length == 1) {
				String timeshift = "";
				boolean ac3Remux;
				boolean dtsRemux;
				boolean encodedAudioPassthrough;
				boolean pcm;

				encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && audioTrack.isNonPCMEncodedAudio() && renderer.isWrapEncodedAudioIntoPCM();
				ac3Remux = audioTrack.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough && !encodingFormat.isTranscodeToAAC();
				dtsRemux = configuration.isAudioEmbedDtsInPcm() && audioTrack.isDTS() && renderer.isDTSPlayable() && !encodedAudioPassthrough;

				pcm = configuration.isAudioUsePCM() &&
					isValidForLPCMTranscoding(media) &&
					(
						audioTrack.isLossless() ||
						(audioTrack.isDTS() && audioTrack.getNumberOfChannels() <= 6) ||
						audioTrack.isTrueHD() ||
						(
							!configuration.isMencoderUsePcmForHQAudioOnly() &&
							(
								audioTrack.isAC3() ||
								audioTrack.isMP3() ||
								audioTrack.isAAC() ||
								audioTrack.isVorbis() ||
								// audioTrack.isWMA() ||
								audioTrack.isMpegAudio()
							)
						)
					) && renderer.isLPCMPlayable();
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
						if (renderer.isMuxDTSToMpeg()) {
							type = "A_DTS";
						}
					}
				}
				if (audioTrack.getVideoDelay() != 0 && params.getTimeSeek() == 0) {
					timeshift = "timeshift=" + audioTrack.getVideoDelay() + "ms, ";
				}
				pw.println(type + ", \"" + ffAudioPipe[0].getOutputPipe() + "\", " + timeshift + "track=2");
			} else if (audioTrack != null && ffAudioPipe != null) {
				for (int i = 0; i < media.getAudioTracks().size(); i++) {
					MediaAudio audio = media.getAudioTracks().get(i);
					String timeshift = "";
					boolean ac3Remux;
					boolean dtsRemux;
					boolean encodedAudioPassthrough;
					boolean pcm;

					encodedAudioPassthrough = configuration.isEncodedAudioPassthrough() && audioTrack.isNonPCMEncodedAudio() && renderer.isWrapEncodedAudioIntoPCM();
					ac3Remux = audio.isAC3() && configuration.isAudioRemuxAC3() && !encodedAudioPassthrough;
					dtsRemux = configuration.isAudioEmbedDtsInPcm() && audio.isDTS() && renderer.isDTSPlayable() && !encodedAudioPassthrough;

					pcm = configuration.isAudioUsePCM() &&
						isValidForLPCMTranscoding(media) &&
						(
							audio.isLossless() ||
							(audio.isDTS() && audio.getNumberOfChannels() <= 6) ||
							audio.isTrueHD() ||
							(
								!configuration.isMencoderUsePcmForHQAudioOnly() &&
								(
									audioTrack.isAC3() ||
									audioTrack.isMP3() ||
									audioTrack.isAAC() ||
									audioTrack.isVorbis() ||
									// audioTrack.isWMA() ||
									audioTrack.isMpegAudio()
								)
							)
						) && renderer.isLPCMPlayable();
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
							if (renderer.isMuxDTSToMpeg()) {
								type = "A_DTS";
							}
						}
					}
					if (audio.getVideoDelay() != 0 && params.getTimeSeek() == 0) {
						timeshift = "timeshift=" + audio.getVideoDelay() + "ms, ";
					}
					pw.println(type + ", \"" + ffAudioPipe[i].getOutputPipe() + "\", " + timeshift + "track=" + (2 + i));
				}
			}
		}

		IPipeProcess tsPipe = PlatformUtils.INSTANCE.getPipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

		String executable = getExecutable();
		String[] cmdArray = new String[]{
			executable,
			f.getAbsolutePath(),
			tsPipe.getInputPipe()
		};

		ProcessWrapperImpl p = new ProcessWrapperImpl(cmdArray, params);
		params.setMaxBufferSize(100);
		params.getInputPipes()[0] = tsPipe;
		params.setStdIn(null);
		ProcessWrapper pipeProcess = tsPipe.getPipeProcess();
		p.attachProcess(pipeProcess);
		pipeProcess.runInNewThread();

		UMSUtils.sleep(50);
		tsPipe.deleteLater();

		ProcessWrapper ffPipeProcess = ffVideoPipe.getPipeProcess();
		p.attachProcess(ffPipeProcess);
		ffPipeProcess.runInNewThread();
		UMSUtils.sleep(50);
		ffVideoPipe.deleteLater();

		p.attachProcess(ffVideo);
		ffVideo.runInNewThread();
		UMSUtils.sleep(50);

		if (ffAudioPipe != null && audioTrack != null) {
			for (int i = 0; i < ffAudioPipe.length; i++) {
				ffPipeProcess = ffAudioPipe[i].getPipeProcess();
				p.attachProcess(ffPipeProcess);
				ffPipeProcess.runInNewThread();
				UMSUtils.sleep(50);
				ffAudioPipe[i].deleteLater();
				p.attachProcess(ffAudio[i]);
				ffAudio[i].runInNewThread();
			}
		}

		UMSUtils.sleep(100);

		p.runInNewThread();
		return p;
	}

	@Override
	public String getMimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.VIDEO;
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
	public boolean isCompatible(StoreItem item) {
		MediaSubtitle subtitle = item.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized MediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The item needs a subtitle, but we do not support subtitles for tsMuxeR.
			// @todo add subtitles support for tsMuxeR
			return false;
		}

		MediaAudio audio = item.getMediaAudio();
		if (audio != null) {
			try {
				String audioTrackName = item.getMediaAudio().toString();
				String defaultAudioTrackName = item.getMediaInfo().getDefaultAudioTrack().toString();

				if (!audioTrackName.equals(defaultAudioTrackName)) {
					// We only support playback of the default audio track for tsMuxeR
					return false;
				}
			} catch (NullPointerException e) {
				LOGGER.trace("tsMuxeR cannot determine compatibility based on audio track for " + item.getFileName());
			} catch (IndexOutOfBoundsException e) {
				LOGGER.trace("tsMuxeR cannot determine compatibility based on default audio track for " + item.getFileName());
			}
		}

		return (
			PlayerUtil.isVideo(item, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(item, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(item, Format.Identifier.OGG)
		);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isVideoFormat() && !encodingFormat.isTranscodeToHLS();
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
				result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" + output.getError().getMessage());
				result.available(Boolean.FALSE);
				LOGGER.debug("\"{} {}\" failed with error: {}", executableInfo.getPath(), arg, output.getError().getMessage());
				return result.build();
			}
			if (output.getExitCode() == 0) {
				if (!output.getOutput().isEmpty()) {
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
					result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + "\n\n" + ntStatus);
				} else {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineNotAvailableExitCode"), this, output.getExitCode()));
					if (Platform.isLinux() && Platform.is64Bit()) {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(result.errorText() + ". \n" + Messages.getString("MakeSureNecessary32bitLibraries"));
					}
					result.available(Boolean.FALSE);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}

	/**
	 * Disable LPCM transcoding for MP4 container with non-H264 video as workaround for MEncoder's A/V sync bug.
	 * @return isValidForLPCMTranscoding
	 */
	public static boolean isValidForLPCMTranscoding(MediaInfo media) {
		return (media != null &&
				media.getContainer() != null &&
				media.getDefaultVideoTrack() != null &&
				media.getContainer().equals("mp4") &&
				media.getDefaultVideoTrack().isH264());
	}

	/**
	 * @return the h264_annexB
	 * @since 1.50.0
	 */
	public byte[] getH264AnnexB(InputFile f, MediaVideo video) {
		if (video.isH264()) {
			byte[][] headers = FFmpegParser.getAnnexBFrameHeader(f);
			if (headers != null && headers[1] != null) {
				byte[] result = new byte[headers[1].length];
				System.arraycopy(headers[1], 0, result, 0, headers[1].length);
				return result;
			}
		}
		return null;
	}

}
