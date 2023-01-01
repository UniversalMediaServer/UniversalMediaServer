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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import net.pms.dlna.*;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.network.HTTPResource;
import net.pms.platform.PlatformUtils;
import net.pms.platform.windows.NTStatus;
import net.pms.renderers.Renderer;
import net.pms.util.CodecUtil;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.util.PlayerUtil;
import net.pms.util.UMSUtils;
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsMuxeRVideo extends Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(TsMuxeRVideo.class);
	public static final EngineId ID = StandardEngineId.TSMUXER_VIDEO;

	/** The {@link Configuration} key for the custom tsMuxeR path. */
	public static final String KEY_TSMUXER_PATH     = "tsmuxer_path";

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
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		// Use device-specific pms conf
		UmsConfiguration prev = configuration;
		configuration = params.getMediaRenderer().getUmsConfiguration();
		final String filename = dlna.getFileName();
		setAudioAndSubs(dlna, params);

		PipeIPCProcess ffVideoPipe;
		ProcessWrapperImpl ffVideo;

		PipeIPCProcess[] ffAudioPipe = null;
		ProcessWrapperImpl[] ffAudio = null;

		String fps = media.getValidFps(false);

		int width  = media.getWidth();
		int height = media.getHeight();
		if (width < 320 || height < 240) {
			width  = -1;
			height = -1;
		}

		String videoType = "V_MPEG4/ISO/AVC";
		String codecV = media.getCodecV();
		if (codecV != null) {
			if (codecV.equals(FormatConfiguration.MPEG2)) {
				videoType = "V_MPEG-2";
			} else if (codecV.equals(FormatConfiguration.H265)) {
				videoType = "V_MPEGH/ISO/HEVC";
			}
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

			if (
				filename.toLowerCase().endsWith(".flac") &&
				media.getFirstAudioTrack().getBitsperSample() >= 24 &&
				media.getFirstAudioTrack().getSampleRate() % 48000 == 0
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

				if (media.getFirstAudioTrack().getBitsperSample() >= 24) {
					depth = "pcm_s24le";
				}

				if (media.getFirstAudioTrack().getSampleRate() > 48000) {
					rate = "" + media.getFirstAudioTrack().getSampleRate();
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
		} else {
			params.setWaitBeforeStart(5000);
			params.manageFastStart();

			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			ffmpegCommands = new String[] {
				EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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
						if (!params.getMediaRenderer().isMuxDTSToMpeg()) {
							ffAudioPipe[0].setModifier(sm);
						}
					} else if (!ac3Remux && params.getMediaRenderer().isTranscodeToAAC()) {
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
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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
								EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO),
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

		File f = new File(CONFIGURATION.getTempFolder(), "ums-tsmuxer.meta");
		params.setLog(false);
		try (PrintWriter pw = new PrintWriter(f)) {
			pw.print("MUXOPT --no-pcr-on-video-pid");
			pw.print(" --new-audio-pes");
			pw.print(" --no-asyncio");
			pw.print(" --vbr");
			pw.println(" --vbv-len=500");

			String videoparams = "";
			if (this instanceof TsMuxeRAudio) {
				videoparams = "track=224";
			} else if (params.getMediaRenderer().isTranscodeToH264()) {
				String sei = "insertSEI";
				if (
					params.getMediaRenderer().isPS3() &&
					media.isWebDl(filename, params)
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

		if (ffAudioPipe != null && params.getAid() != null) {
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
		configuration = prev;
		return p;
	}

	@Override
	public String mimeType() {
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
	public boolean isEngineCompatible(Renderer renderer) {
		return renderer != null && renderer.isMuxH264MpegTS();
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized DLNAMediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The resource needs a subtitle, but we do not support subtitles for tsMuxeR.
			// @todo add subtitles support for tsMuxeR
			return false;
		}

		try {
			String audioTrackName = resource.getMediaAudio().toString();
			String defaultAudioTrackName = resource.getMedia().getAudioTracksList().get(0).toString();

			if (!audioTrackName.equals(defaultAudioTrackName)) {
				// We only support playback of the default audio track for tsMuxeR
				return false;
			}
		} catch (NullPointerException e) {
			LOGGER.trace("tsMuxeR cannot determine compatibility based on audio track for " + resource.getSystemName());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.trace("tsMuxeR cannot determine compatibility based on default audio track for " + resource.getSystemName());
		}

		return (
			PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(resource, Format.Identifier.OGG)
		);
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
			return null;
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
