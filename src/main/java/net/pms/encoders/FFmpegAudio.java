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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.PlayerUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegAudio extends FFMpegVideo {

	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudio.class);
	public static final EngineId ID = StandardEngineId.FFMPEG_AUDIO;

	/** The {@link Configuration} key for the FFmpeg Audio executable type. */
	public static final String KEY_FFMPEG_AUDIO_EXECUTABLE_TYPE = "ffmpeg_audio_executable_type";
	public static final String NAME = "FFmpeg Audio";

	// Not to be instantiated by anything but PlayerFactory
	FFmpegAudio() {
	}

	@Override
	public int purpose() {
		return AUDIO_SIMPLEFILE_ENGINE;
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_FFMPEG_AUDIO_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public boolean isAviSynthEngine() {
		return false;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.AUDIO;
	}

	@Override
	public String getMimeType() {
		return HTTPResource.AUDIO_TRANSCODE;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		StoreItem item,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		final String filename = item.getFileName();
		final EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		params.setMaxBufferSize(configuration.getMaxAudioBuffer());
		params.setWaitBeforeStart(1);
		params.manageFastStart();

		/*
		 * FFmpeg uses multithreading by default, so provided that the
		 * user has not disabled FFmpeg multithreading and has not
		 * chosen to use more or less threads than are available, do not
		 * specify how many cores to use.
		 */
		int nThreads = 1;
		if (configuration.isFfmpegMultithreading()) {
			if (Runtime.getRuntime().availableProcessors() == configuration.getNumberOfCpuCores()) {
				nThreads = 0;
			} else {
				nThreads = configuration.getNumberOfCpuCores();
			}
		}

		List<String> cmdList = new ArrayList<>();

		cmdList.add(getExecutable());

		cmdList.add("-loglevel");
		FFmpegLogLevels askedLogLevel = FFmpegLogLevels.valueOfLabel(configuration.getFFmpegLoggingLevel());
		if (LOGGER.isTraceEnabled()) {
			// Set -loglevel in accordance with LOGGER setting
			if (FFmpegLogLevels.INFO.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("info");
			} else {
				cmdList.add(askedLogLevel.label);
			}
		} else {
			if (FFmpegLogLevels.WARNING.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("warning");
			} else {
				cmdList.add(askedLogLevel.label);
			}
		}

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + params.getTimeSeek());
		}

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		cmdList.add("-i");
		cmdList.add(filename);

		// Make sure FFmpeg doesn't try to encode embedded images into the stream
		cmdList.add("-vn");

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		if (params.getTimeEnd() > 0) {
			cmdList.add("-t");
			cmdList.add("" + params.getTimeEnd());
		}

		String customFFmpegAudioOptions = renderer.getCustomFFmpegAudioOptions();

		// Add audio options (-af, -filter_complex, -ab, -ar, -ac, -c:a, -f, -apre, -fpre, -pre, etc.)
		if (StringUtils.isNotBlank(customFFmpegAudioOptions)) {
			parseOptions(customFFmpegAudioOptions, cmdList);
		}

		if (encodingFormat.isTranscodeToMP3()) {
			if (!customFFmpegAudioOptions.contains("-ab ")) {
				cmdList.add("-ab");
				cmdList.add("320000");
			}
			if (!customFFmpegAudioOptions.contains("-f ")) {
				cmdList.add("-f");
				cmdList.add("mp3");
			}
		} else if (encodingFormat.isTranscodeToWAV()) {
			if (!customFFmpegAudioOptions.contains("-f ")) {
				cmdList.add("-f");
				cmdList.add("wav");
			}
		} else { // default: LPCM
			if (!customFFmpegAudioOptions.contains("-f ")) {
				cmdList.add("-f");
				cmdList.add("s16be");
			}
		}

		if (configuration.isAudioResample()) {
			if (renderer.isTranscodeAudioTo441()) {
				if (!customFFmpegAudioOptions.contains("-ar ")) {
					cmdList.add("-ar");
					cmdList.add("44100");
				}
				if (!customFFmpegAudioOptions.contains("-ac ")) {
					cmdList.add("-ac");
					cmdList.add("2");
				}
			} else {
				if (!customFFmpegAudioOptions.contains("-ar ")) {
					cmdList.add("-ar");
					cmdList.add("48000");
				}
				if (!customFFmpegAudioOptions.contains("-ac ")) {
					cmdList.add("-ac");
					cmdList.add("2");
				}
			}
		}

		cmdList.add("pipe:");

		String[] cmdArray = new String[ cmdList.size() ];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		return pw;
	}

	@Override
	public boolean isCompatible(StoreItem resource) {
		// XXX Matching on file format isn't really enough, codec should also be evaluated
		return (
			PlayerUtil.isAudio(resource, Format.Identifier.AC3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ADPCM) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ADTS) ||
			PlayerUtil.isAudio(resource, Format.Identifier.AIFF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.APE) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ATRAC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.AU) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DFF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DSF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DTS) ||
			PlayerUtil.isAudio(resource, Format.Identifier.EAC3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.FLAC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.M4A) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MKA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MLP) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MP3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MPA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MPC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.OGA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.RA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.SHN) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THREEGA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THREEG2A) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THD) ||
			PlayerUtil.isAudio(resource, Format.Identifier.TTA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WAV) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WMA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WV) ||
			PlayerUtil.isWebAudio(resource)
		);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isAudioFormat();
	}

}
