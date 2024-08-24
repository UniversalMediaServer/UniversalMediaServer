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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines identifiers for {@link Engine} subclasses.
 *
 * @author Nadahar
 */
@Immutable
@SuppressWarnings("serial")
public class StandardEngineId extends EngineId {
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardEngineId.class);

	/** The identifier for {@link AviSynthFFmpeg} */
	public static final EngineId AVI_SYNTH_FFMPEG = new StandardEngineId("AviSynthFFmpeg");

	/** The identifier for {@link AviSynthMEncoder} */
	public static final EngineId AVI_SYNTH_MENCODER = new StandardEngineId("AviSynthMEncoder");

	/** The identifier for {@link FFmpegAudio} */
	public static final EngineId FFMPEG_AUDIO = new StandardEngineId("FFmpegAudio");

	/** The identifier for {@link FFMpegVideo} */
	public static final EngineId FFMPEG_VIDEO = new StandardEngineId("FFmpegVideo");

	/** The identifier for {@link FFmpegHlsVideo} */
	public static final EngineId FFMPEG_HLS_VIDEO = new StandardEngineId("FFmpegHlsVideo");

	/** The identifier for {@link FFmpegWebVideo} */
	public static final EngineId FFMPEG_WEB_VIDEO = new StandardEngineId("FFmpegWebVideo");

	/** The identifier for {@link MEncoderVideo} */
	public static final EngineId MENCODER_VIDEO = new StandardEngineId("MEncoderVideo");

	/** The identifier for {@link MEncoderWebVideo} */
	public static final EngineId MENCODER_WEB_VIDEO = new StandardEngineId("MEncoderWebVideo");

	/** The identifier for {@link DCRaw} */
	public static final EngineId DCRAW = new StandardEngineId("DCRaw");

	/** The identifier for {@link TsMuxeRAudio} */
	public static final EngineId TSMUXER_AUDIO = new StandardEngineId("tsMuxeRAudio");

	/** The identifier for {@link TsMuxeRVideo} */
	public static final EngineId TSMUXER_VIDEO = new StandardEngineId("tsMuxeRVideo");

	/** The identifier for {@link VideoLanAudioStreaming} */
	public static final EngineId VLC_AUDIO_STREAMING = new StandardEngineId("VLCAudioStreaming");

	/** The identifier for {@link VideoLanVideoStreaming} */
	public static final EngineId VLC_VIDEO_STREAMING = new StandardEngineId("VLCVideoStreaming");

	/** The identifier for {@link VLCVideo} */
	public static final EngineId VLC_VIDEO = new StandardEngineId("VLCVideo");

	/** The identifier for {@link VLCWebVideo} */
	public static final EngineId VLC_WEB_VIDEO = new StandardEngineId("VLCWebVideo");

	/** The identifier for {@link YoutubeDl} */
	public static final EngineId YOUTUBE_DL = new StandardEngineId("youtubeDl");

	/** *  A static list of all {@link StandardEngineId}s */
	public static final List<EngineId> ALL;

	static {
		List<EngineId> allEngines = new ArrayList<>(12);
		allEngines.add(FFMPEG_VIDEO);
		if (PlatformUtils.isWindows()) {
			allEngines.add(AVI_SYNTH_FFMPEG);
		}
		allEngines.add(MENCODER_VIDEO);
		if (PlatformUtils.isWindows()) {
			allEngines.add(AVI_SYNTH_MENCODER);
		}
		allEngines.add(TSMUXER_VIDEO);
		allEngines.add(VLC_VIDEO);
		allEngines.add(FFMPEG_AUDIO);
		allEngines.add(TSMUXER_AUDIO);
		allEngines.add(FFMPEG_HLS_VIDEO);
		allEngines.add(FFMPEG_WEB_VIDEO);
		allEngines.add(VLC_WEB_VIDEO);
		allEngines.add(VLC_VIDEO_STREAMING);
		allEngines.add(MENCODER_WEB_VIDEO);
		allEngines.add(VLC_AUDIO_STREAMING);
		allEngines.add(DCRAW);
		allEngines.add(YOUTUBE_DL);
		ALL = Collections.unmodifiableList(allEngines);
	}

	/** *  The textual representation of this {@link EngineId} */
	protected final String engineIdName;

	/**
	 * Not to be instantiated, use the static instances instead.
	 *
	 * @param the name of the {@link EngineId} used for {@link #getName()} and
	 *            {@link #toString()}.
	 */
	private StandardEngineId(String engineIdName) {
		this.engineIdName = engineIdName;
	}

	@Override
	public String getName() {
		return engineIdName;
	}

	/**
	 * Attempts to convert the specified {@link String} to a
	 * {@link StandardEngineId}. If the conversion fails, {@code null} is
	 * returned.
	 *
	 * @param engineIdString the {@link String} to convert.
	 * @return The corresponding {@link StandardEngineId} or {@code null}.
	 */
	public static EngineId toEngineID(String engineIdString) {
		if (StringUtils.isBlank(engineIdString)) {
			return null;
		}
		engineIdString = engineIdString.trim().toUpperCase(Locale.ROOT);
		switch (engineIdString) {
			case "AVISYNTHFFMPEG":
				return AVI_SYNTH_FFMPEG;
			case "AVISYNTHMENCODER":
				return AVI_SYNTH_MENCODER;
			case "FFMPEGAUDIO":
				return FFMPEG_AUDIO;
			case "FFMPEGVIDEO":
				return FFMPEG_VIDEO;
			case "FFMPEGHLSVIDEO":
				return FFMPEG_HLS_VIDEO;
			case "FFMPEGWEBVIDEO":
				return FFMPEG_WEB_VIDEO;
			case "MENCODER": // old name
			case "MENCODERVIDEO":
				return MENCODER_VIDEO;
			case "MENCODERWEBVIDEO":
				return MENCODER_WEB_VIDEO;
			case "DCRAW":
				return DCRAW;
			case "TSMUXERAUDIO":
				return TSMUXER_AUDIO;
			case "TSMUXER": // old name
			case "TSMUXERVIDEO":
				return TSMUXER_VIDEO;
			case "VLCAUDIO": // old name
			case "VLCAUDIOSTREAMING":
				return VLC_AUDIO_STREAMING;
			case "VLCVIDEOSTREAMING":
				return VLC_VIDEO_STREAMING;
			case "VLCTRANSCODER": // old name
			case "VLCVIDEO":
				return VLC_VIDEO;
			case "VLCWEBVIDEO":
				return VLC_WEB_VIDEO;
			case "YOUTUBEDL":
				return YOUTUBE_DL;
			default:
				LOGGER.warn("Could not parse engine id \"{}\"", engineIdString);
				return null;
		}
	}
}
