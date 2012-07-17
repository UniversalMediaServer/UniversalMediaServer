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
import java.util.Locale;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import static net.pms.formats.v2.AudioUtils.getLPCMChannelMappingForMencoder;
import net.pms.io.*;
import net.pms.util.CodecUtil;
import net.pms.util.FormLayoutUtil;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSMuxerVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(TSMuxerVideo.class);
	private static final String COL_SPEC = "left:pref, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow";

	public static final String ID = "tsmuxer";
	private PmsConfiguration configuration;

	public TSMuxerVideo(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean excludeFormat(Format extension) {
		String m = extension.getMatchedId();
		return m != null && !m.equals("mp4") && !m.equals("mkv") && !m.equals("ts") && !m.equals("tp") && !m.equals("m2ts") && !m.equals("m2t") && !m.equals("mpg") && !m.equals("evo") && !m.equals("mpeg")
			&& !m.equals("vob") && !m.equals("m2v") && !m.equals("mts") && !m.equals("mov");
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
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		setAudioAndSubs(fileName, media, params, configuration);

		PipeIPCProcess ffVideoPipe = null;
		ProcessWrapperImpl ffVideo = null;

		PipeIPCProcess ffAudioPipe[] = null;
		ProcessWrapperImpl ffAudio[] = null;

		String fps = media.getValidFps(false);
		String videoType = "V_MPEG4/ISO/AVC";
		if (media != null && media.getCodecV() != null && media.getCodecV().equals("mpeg2video")) {
			videoType = "V_MPEG-2";
		}

		if (this instanceof TsMuxerAudio && media.getFirstAudioTrack() != null) {
			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "fakevideo", System.currentTimeMillis() + "videoout", false, true);
			String ffmpegLPCMextract[] = new String[]{configuration.getFfmpegPath(), "-t", "" + params.timeend, "-loop_input", "-i", "resources/images/fake.jpg", "-qcomp", "0.6", "-qmin", "10", "-qmax", "51", "-qdiff", "4", "-me_range", "4", "-f", "h264", "-vcodec", "libx264", "-an", "-y", ffVideoPipe.getInputPipe()};
			//videoType = "V_MPEG-2";
			videoType = "V_MPEG4/ISO/AVC";
			if (params.timeend < 1) {
				ffmpegLPCMextract[1] = "-title";
				ffmpegLPCMextract[2] = "dummy";
			}

			OutputParams ffparams = new OutputParams(PMS.getConfiguration());
			ffparams.maxBufferSize = 1;
			ffVideo = new ProcessWrapperImpl(ffmpegLPCMextract, ffparams);

			if (fileName.toLowerCase().endsWith(".flac") && media != null && media.getFirstAudioTrack().getBitsperSample() >= 24 && media.getFirstAudioTrack().getSampleRate() % 48000 == 0) {
				ffAudioPipe = new PipeIPCProcess[1];
				ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "flacaudio", System.currentTimeMillis() + "audioout", false, true);
				String flacCmd[] = new String[]{configuration.getFlacPath(), "--output-name=" + ffAudioPipe[0].getInputPipe(), "-d", "-f", "-F", fileName};

				ffparams = new OutputParams(PMS.getConfiguration());
				ffparams.maxBufferSize = 1;
				ffAudio = new ProcessWrapperImpl[1];
				ffAudio[0] = new ProcessWrapperImpl(flacCmd, ffparams);
			} else {
				ffAudioPipe = new PipeIPCProcess[1];
				ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "mlpaudio", System.currentTimeMillis() + "audioout", false, true);
				String depth = "pcm_s16le";
				String rate = "48000";
				if (media != null && media.getFirstAudioTrack().getBitsperSample() >= 24) {
					depth = "pcm_s24le";
				}
				if (media != null && media.getFirstAudioTrack().getSampleRate() > 48000) {
					rate = "" + media.getFirstAudioTrack().getSampleRate();
				}
				String flacCmd[] = new String[]{configuration.getFfmpegPath(), "-ar", rate, "-i", fileName, "-f", "wav", "-acodec", depth, "-y", ffAudioPipe[0].getInputPipe()};

				ffparams = new OutputParams(PMS.getConfiguration());
				ffparams.maxBufferSize = 1;
				ffAudio = new ProcessWrapperImpl[1];
				ffAudio[0] = new ProcessWrapperImpl(flacCmd, ffparams);
			}
		} else {
			params.waitbeforestart = 5000;
			params.manageFastStart();

			String mencoderPath = configuration.getMencoderPath();

			ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);
			String ffmpegLPCMextract[] = new String[]{
				mencoderPath,
				"-ss", "0",
				fileName,
				"-quiet",
				"-quiet",
				"-really-quiet",
				"-msglevel", "statusline=2",
				"-ovc", "copy",
				"-nosound",
				"-mc", "0",
				"-noskip",
				"-of", "rawvideo",
				"-o", ffVideoPipe.getInputPipe()
			};

			if (fileName.toLowerCase().endsWith(".evo")) {
				ffmpegLPCMextract[4] = "-psprobe";
				ffmpegLPCMextract[5] = "1000000";
			}

			if (params.stdin != null) {
				ffmpegLPCMextract[3] = "-";
			}
			InputFile newInput = new InputFile();
			newInput.setFilename(fileName);
			newInput.setPush(params.stdin);

			if (media != null) {
				boolean compat = (media.isVideoPS3Compatible(newInput) || !params.mediaRenderer.isH264Level41Limited());

				if (!compat && params.mediaRenderer.isPS3()) {
					LOGGER.info("The video will not play or will show a black screen on the PS3");
				}
				if (media.getH264AnnexB() != null && media.getH264AnnexB().length > 0) {
					StreamModifier sm = new StreamModifier();
					sm.setHeader(media.getH264AnnexB());
					sm.setH264_annexb(true);
					ffVideoPipe.setModifier(sm);
				}
			}

			if (params.timeseek > 0) {
				ffmpegLPCMextract[2] = "" + params.timeseek;
			}

			OutputParams ffparams = new OutputParams(PMS.getConfiguration());
			ffparams.maxBufferSize = 1;
			ffparams.stdin = params.stdin;
			ffVideo = new ProcessWrapperImpl(ffmpegLPCMextract, ffparams);

			int numAudioTracks = 1;

			if (media != null && media.getAudioTracksList() != null && media.getAudioTracksList().size() > 1 && configuration.isMuxAllAudioTracks()) {
				numAudioTracks = media.getAudioTracksList().size();
			}

			boolean singleMediaAudio = media != null && media.getAudioTracksList().size() <= 1;

			if (params.aid != null) {
				boolean ac3Remux;
				boolean dtsRemux;
				boolean pcm;

				// Disable LPCM transcoding for MP4 container with non-H264 video as workaround for MEncoder's A/V sync bug
				boolean mp4_with_non_h264 = (media.getContainer().equals("mp4") && !media.getCodecV().equals("h264"));

				if (numAudioTracks <= 1) {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudioPipe[0] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);

					// Disable AC3 remux for stereo tracks with 384 kbits bitrate and PS3 renderer (PS3 FW bug?)
					boolean ps3_and_stereo_and_384_kbits = (params.mediaRenderer.isPS3() && params.aid.getAudioProperties().getNumberOfChannels() == 2) &&
						(params.aid.getBitRate() > 370000 && params.aid.getBitRate() < 400000);
					ac3Remux = (params.aid.isAC3() && !ps3_and_stereo_and_384_kbits && configuration.isRemuxAC3());
					dtsRemux = configuration.isDTSEmbedInPCM() && params.aid.isDTS() && params.mediaRenderer.isDTSPlayable();

					pcm = configuration.isMencoderUsePcm() &&
						!mp4_with_non_h264 &&
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
						channels = params.aid.getAudioProperties().getNumberOfChannels(); // ac3 remux
					} else if (dtsRemux) {
						channels = 2;
					} else if (pcm) {
						channels = params.aid.getAudioProperties().getNumberOfChannels();
					} else {
						channels = configuration.getAudioChannelCount(); // 5.1 max for ac3 encoding
					}

					if (!ac3Remux && (dtsRemux || pcm)) {
						// DTS remux or LPCM
						StreamModifier sm = new StreamModifier();
						sm.setPcm(pcm);
						sm.setDtsembed(dtsRemux);
						sm.setNbchannels(channels);
						sm.setSampleFrequency(params.aid.getSampleRate() < 48000 ? 48000 : params.aid.getSampleRate());
						sm.setBitspersample(16);

						String mixer = null;
						if (pcm && !dtsRemux) {
							mixer = getLPCMChannelMappingForMencoder(params.aid);
						}

						ffmpegLPCMextract = new String[]{
							mencoderPath,
							"-ss", "0",
							fileName,
							"-quiet",
							"-quiet",
							"-really-quiet",
							"-msglevel", "statusline=2",
							"-channels", "" + sm.getNbchannels(),
							"-ovc", "copy",
							"-of", "rawaudio",
							"-mc", sm.isDtsembed() ? "0.1" : "0",
							"-noskip",
							"-oac", sm.isDtsembed() ? "copy" : "pcm",
							isNotBlank(mixer) ? "-af" : "-quiet", isNotBlank(mixer) ? mixer : "-quiet",
							singleMediaAudio ? "-quiet" : "-aid", singleMediaAudio ? "-quiet" : ("" + params.aid.getId()),
							"-srate", "48000",
							"-o", ffAudioPipe[0].getInputPipe()
						};

						// Use PCM trick when media renderer does not support DTS in MPEG
						if (!params.mediaRenderer.isMuxDTSToMpeg()) {
							
							ffAudioPipe[0].setModifier(sm);
						}
					} else {
						ffmpegLPCMextract = new String[]{
							mencoderPath,
							"-ss", "0",
							fileName,
							"-quiet",
							"-quiet",
							"-really-quiet",
							"-msglevel", "statusline=2",
							"-channels", "" + channels,
							"-ovc", "copy",
							"-of", "rawaudio",
							"-mc", "0",
							"-noskip",
							"-oac", (ac3Remux) ? "copy" : "lavc",
							params.aid.isAC3() ? "-fafmttag" : "-quiet", params.aid.isAC3() ? "0x2000" : "-quiet",
							"-lavcopts", "acodec=" + (configuration.isMencoderAc3Fixed() ? "ac3_fixed" : "ac3") + ":abitrate=" + CodecUtil.getAC3Bitrate(configuration, params.aid),
							"-af", "lavcresample=48000",
							"-srate", "48000",
							singleMediaAudio ? "-quiet" : "-aid", singleMediaAudio ? "-quiet" : ("" + params.aid.getId()),
							"-o", ffAudioPipe[0].getInputPipe()
						};
					}

					if (fileName.toLowerCase().endsWith(".evo")) {
						ffmpegLPCMextract[4] = "-psprobe";
						ffmpegLPCMextract[5] = "1000000";
					}

					if (params.stdin != null) {
						ffmpegLPCMextract[3] = "-";
					}

					if (params.timeseek > 0) {
						ffmpegLPCMextract[2] = "" + params.timeseek;
					}

					ffparams = new OutputParams(PMS.getConfiguration());
					ffparams.maxBufferSize = 1;
					ffparams.stdin = params.stdin;
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					ffAudio[0] = new ProcessWrapperImpl(ffmpegLPCMextract, ffparams);
				} else {
					ffAudioPipe = new PipeIPCProcess[numAudioTracks];
					ffAudio = new ProcessWrapperImpl[numAudioTracks];
					for (int i = 0; i < media.getAudioTracksList().size(); i++) {
						DLNAMediaAudio audio = media.getAudioTracksList().get(i);
						ffAudioPipe[i] = new PipeIPCProcess(System.currentTimeMillis() + "ffmpeg" + i, System.currentTimeMillis() + "audioout" + i, false, true);

						// disable AC3 remux for stereo tracks with 384 kbits bitrate and PS3 renderer (PS3 FW bug?)
						boolean ps3_and_stereo_and_384_kbits = (params.mediaRenderer.isPS3() && audio.getAudioProperties().getNumberOfChannels() == 2) &&
							(audio.getBitRate() > 370000 && audio.getBitRate() < 400000);
						ac3Remux = audio.isAC3() && !ps3_and_stereo_and_384_kbits && configuration.isRemuxAC3();
						dtsRemux = configuration.isDTSEmbedInPCM() && audio.isDTS() && params.mediaRenderer.isDTSPlayable();

						pcm = configuration.isMencoderUsePcm() &&
							!mp4_with_non_h264 &&
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
							channels = audio.getAudioProperties().getNumberOfChannels(); // ac3 remux
						} else if (dtsRemux) {
							channels = 2;
						} else if (pcm) {
							channels = audio.getAudioProperties().getNumberOfChannels();
						} else {
							channels = configuration.getAudioChannelCount(); // 5.1 max for ac3 encoding
						}

						if (!ac3Remux && (dtsRemux || pcm)) {
							// DTS remux or LPCM
							StreamModifier sm = new StreamModifier();
							sm.setPcm(pcm);
							sm.setDtsembed(dtsRemux);
							sm.setNbchannels(channels);
							sm.setSampleFrequency(audio.getSampleRate() < 48000 ? 48000 : audio.getSampleRate());
							sm.setBitspersample(16);
							if (!params.mediaRenderer.isMuxDTSToMpeg()) {
								ffAudioPipe[i].setModifier(sm);
							}

							String mixer = null;
							if (pcm && !dtsRemux) {
								mixer = getLPCMChannelMappingForMencoder(audio);
							}

							ffmpegLPCMextract = new String[]{
								mencoderPath,
								"-ss", "0",
								fileName,
								"-quiet",
								"-quiet",
								"-really-quiet",
								"-msglevel", "statusline=2",
								"-channels", "" + sm.getNbchannels(),
								"-ovc", "copy",
								"-of", "rawaudio",
								"-mc", sm.isDtsembed() ? "0.1" : "0",
								"-noskip",
								"-oac", sm.isDtsembed() ? "copy" : "pcm",
								isNotBlank(mixer) ? "-af" : "-quiet", isNotBlank(mixer) ? mixer : "-quiet",
								singleMediaAudio ? "-quiet" : "-aid", singleMediaAudio ? "-quiet" : ("" + audio.getId()),
								"-srate", "48000",
								"-o", ffAudioPipe[i].getInputPipe()
							};
						} else {
							ffmpegLPCMextract = new String[]{
								mencoderPath,
								"-ss", "0",
								fileName,
								"-quiet",
								"-quiet",
								"-really-quiet",
								"-msglevel", "statusline=2",
								"-channels", "" + channels,
								"-ovc", "copy",
								"-of", "rawaudio",
								"-mc", "0",
								"-noskip",
								"-oac", (ac3Remux) ? "copy" : "lavc",
								audio.isAC3() ? "-fafmttag" : "-quiet", audio.isAC3() ? "0x2000" : "-quiet",
								"-lavcopts", "acodec=" + (configuration.isMencoderAc3Fixed() ? "ac3_fixed" : "ac3") + ":abitrate=" + CodecUtil.getAC3Bitrate(configuration, audio),
								"-af", "lavcresample=48000",
								"-srate", "48000",
								singleMediaAudio ? "-quiet" : "-aid", singleMediaAudio ? "-quiet" : ("" + audio.getId()),
								"-o", ffAudioPipe[i].getInputPipe()
							};
						}

						if (fileName.toLowerCase().endsWith(".evo")) {
							ffmpegLPCMextract[4] = "-psprobe";
							ffmpegLPCMextract[5] = "1000000";
						}

						if (params.stdin != null) {
							ffmpegLPCMextract[3] = "-";
						}
						if (params.timeseek > 0) {
							ffmpegLPCMextract[2] = "" + params.timeseek;
						}
						ffparams = new OutputParams(PMS.getConfiguration());
						ffparams.maxBufferSize = 1;
						ffparams.stdin = params.stdin;
						ffAudio[i] = new ProcessWrapperImpl(ffmpegLPCMextract, ffparams);
					}
				}
			}
		}

		File f = new File(configuration.getTempFolder(), "pms-tsmuxer.meta");
		params.log = false;
		PrintWriter pw = new PrintWriter(f);
		pw.print("MUXOPT --no-pcr-on-video-pid");
		pw.print(" --new-audio-pes");
		if (ffVideo != null) {
			pw.print(" --no-asyncio");
		}
		pw.print(" --vbr");
		pw.println(" --vbv-len=500");

		if (ffVideoPipe != null) {
			String videoparams = "level=4.1, insertSEI, contSPS, track=1";
			if (this instanceof TsMuxerAudio) {
				videoparams = "track=224";
			}
			if (configuration.isFix25FPSAvMismatch()) {
				fps = "25";
			}
			pw.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + (fps != null ? ("fps=" + fps + ", ") : "") + videoparams);
		}

		// disable LPCM transcoding for MP4 container with non-H264 video as workaround for mencoder's A/V sync bug
		boolean mp4_with_non_h264 = (media.getContainer().equals("mp4") && !media.getCodecV().equals("h264"));
		if (ffAudioPipe != null && ffAudioPipe.length == 1) {
			String timeshift = "";
			boolean ac3Remux;
			boolean dtsRemux;
			boolean pcm;
			boolean ps3_and_stereo_and_384_kbits = (params.mediaRenderer.isPS3() && params.aid.getAudioProperties().getNumberOfChannels() == 2) &&
				(params.aid.getBitRate() > 370000 && params.aid.getBitRate() < 400000);
			ac3Remux = params.aid.isAC3() && !ps3_and_stereo_and_384_kbits && configuration.isRemuxAC3();
			dtsRemux = configuration.isDTSEmbedInPCM() && params.aid.isDTS() && params.mediaRenderer.isDTSPlayable();

			pcm = configuration.isMencoderUsePcm() &&
				!mp4_with_non_h264 &&
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
				// AC3 remux takes priority
				type = "A_AC3";
			} else {
				if (pcm || this instanceof TsMuxerAudio) {
					type = "A_LPCM";
				}
				if (dtsRemux || this instanceof TsMuxerAudio) {
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
				boolean pcm;
				boolean ps3_and_stereo_and_384_kbits = (params.mediaRenderer.isPS3() && lang.getAudioProperties().getNumberOfChannels() == 2) &&
					(lang.getBitRate() > 370000 && lang.getBitRate() < 400000);
				ac3Remux = lang.isAC3() && !ps3_and_stereo_and_384_kbits && configuration.isRemuxAC3();
				dtsRemux = configuration.isDTSEmbedInPCM() && lang.isDTS() && params.mediaRenderer.isDTSPlayable();

				pcm = configuration.isMencoderUsePcm() &&
					!mp4_with_non_h264 &&
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
					// AC3 remux takes priority
					type = "A_AC3";
				} else {
					if (pcm) {
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
		pw.close();

		PipeProcess tsPipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");
		String[] cmdArray = new String[]{executable(), f.getAbsolutePath(), tsPipe.getInputPipe()};

		cmdArray = finalizeTranscoderArgs(
			this,
			fileName,
			dlna,
			media,
			params,
			cmdArray
		);

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

		if (ffVideoPipe != null) {
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
		}

		if (ffAudioPipe != null && params.aid != null) {
			for (int i = 0; i < ffAudioPipe.length; i++) {
				ProcessWrapper ff_pipe_process = ffAudioPipe[i].getPipeProcess();
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
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();


		JComponent cmp = builder.addSeparator(Messages.getString("TSMuxerVideo.3"), FormLayoutUtil.flip(cc.xyw(2, 1, 1), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		tsmuxerforcefps = new JCheckBox(Messages.getString("TSMuxerVideo.2"));
		tsmuxerforcefps.setContentAreaFilled(false);
		if (configuration.isTsmuxerForceFps()) {
			tsmuxerforcefps.setSelected(true);
		}
		tsmuxerforcefps.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setTsmuxerForceFps(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(tsmuxerforcefps, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		muxallaudiotracks = new JCheckBox(Messages.getString("TSMuxerVideo.19"));
		muxallaudiotracks.setContentAreaFilled(false);
		if (configuration.isMuxAllAudioTracks()) {
			muxallaudiotracks.setSelected(true);
		}

		muxallaudiotracks.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMuxAllAudioTracks(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(muxallaudiotracks, FormLayoutUtil.flip(cc.xy(2, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public boolean isInternalSubtitlesSupported() {
		return false;
	}

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
