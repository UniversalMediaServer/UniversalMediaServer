/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;


/**
 * Defines identifiers for {@link Player} subclasses.
 *
 * @author Nadahar
 */
@Immutable
@SuppressWarnings("serial")
public class StandardPlayerId extends PlayerId {
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardPlayerId.class);

	/** The identifier for {@link AviSynthFFmpeg} */
	public static final PlayerId AVI_SYNTH_FFMPEG = new StandardPlayerId("AviSynthFFmpeg");

	/** The identifier for {@link AviSynthMEncoder} */
	public static final PlayerId AVI_SYNTH_MENCODER = new StandardPlayerId("AviSynthMEncoder");

	/** The identifier for {@link FFmpegAudio} */
	public static final PlayerId FFMPEG_AUDIO = new StandardPlayerId("FFmpegAudio");

	/** The identifier for {@link FFMpegVideo} */
	public static final PlayerId FFMPEG_VIDEO = new StandardPlayerId("FFmpegVideo");

	/** The identifier for {@link FFmpegWebVideo} */
	public static final PlayerId FFMPEG_WEB_VIDEO = new StandardPlayerId("FFmpegWebVideo");

	/** The identifier for {@link MEncoderVideo} */
	public static final PlayerId MENCODER_VIDEO = new StandardPlayerId("MEncoderVideo");

	/** The identifier for {@link MEncoderWebVideo} */
	public static final PlayerId MENCODER_WEB_VIDEO = new StandardPlayerId("MEncoderWebVideo");

	/** The identifier for {@link DCRaw} */
	public static final PlayerId DCRAW = new StandardPlayerId("DCRaw");

	/** The identifier for {@link TsMuxeRAudio} */
	public static final PlayerId TSMUXER_AUDIO = new StandardPlayerId("tsMuxeRAudio");

	/** The identifier for {@link TsMuxeRVideo} */
	public static final PlayerId TSMUXER_VIDEO = new StandardPlayerId("tsMuxeRVideo");

	/** The identifier for {@link VideoLanAudioStreaming} */
	public static final PlayerId VLC_AUDIO_STREAMING = new StandardPlayerId("VLCAudioStreaming");

	/** The identifier for {@link VideoLanVideoStreaming} */
	public static final PlayerId VLC_VIDEO_STREAMING = new StandardPlayerId("VLCVideoStreaming");

	/** The identifier for {@link VLCVideo} */
	public static final PlayerId VLC_VIDEO = new StandardPlayerId("VLCVideo");

	/** The identifier for {@link VLCWebVideo} */
	public static final PlayerId VLC_WEB_VIDEO = new StandardPlayerId("VLCWebVideo");

	/** A static list of all {@link StandardPlayerId}s */
	public static final List<PlayerId> ALL;

	static {
		boolean windows = Platform.isWindows();
		List<PlayerId> allPlayers = new ArrayList<>(12);
		allPlayers.add(FFMPEG_VIDEO);
		if (windows) {
			allPlayers.add(AVI_SYNTH_FFMPEG);
		}
		allPlayers.add(MENCODER_VIDEO);
		if (windows) {
			allPlayers.add(AVI_SYNTH_MENCODER);
		}
		allPlayers.add(TSMUXER_VIDEO);
		allPlayers.add(VLC_VIDEO);
		allPlayers.add(FFMPEG_AUDIO);
		allPlayers.add(TSMUXER_AUDIO);
		allPlayers.add(FFMPEG_WEB_VIDEO);
		allPlayers.add(VLC_WEB_VIDEO);
		allPlayers.add(VLC_VIDEO_STREAMING);
		allPlayers.add(MENCODER_WEB_VIDEO);
		allPlayers.add(VLC_AUDIO_STREAMING);
		allPlayers.add(DCRAW);
		ALL = Collections.unmodifiableList(allPlayers);
	}

	/** The textual representation of this {@link PlayerId} */
	protected final String playerIdName;

	/**
	 * Not to be instantiated, use the static instances instead.
	 *
	 * @param the name of the {@link PlayerId} used for {@link #getName()} and
	 *            {@link #toString()}.
	 */
	private StandardPlayerId(String playerIdName) {
		this.playerIdName = playerIdName;
	}

	@Override
	public String getName() {
		return playerIdName;
	}

	/**
	 * Attempts to convert the specified {@link String} to a
	 * {@link StandardPlayerId}. If the conversion fails, {@code null} is
	 * returned.
	 *
	 * @param playerIdString the {@link String} to convert.
	 * @return The corresponding {@link StandardPlayerId} or {@code null}.
	 */
	public static PlayerId toPlayerID(String playerIdString) {
		if (isBlank(playerIdString)) {
			return null;
		}
		playerIdString = playerIdString.trim().toUpperCase(Locale.ROOT);
		switch (playerIdString) {
			case "AVISYNTHFFMPEG":
				return AVI_SYNTH_FFMPEG;
			case "AVISYNTHMENCODER":
				return AVI_SYNTH_MENCODER;
			case "FFMPEGAUDIO":
				return FFMPEG_AUDIO;
			case "FFMPEGVIDEO":
				return FFMPEG_VIDEO;
			case "FFMPEGWEBVIDEO":
				return FFMPEG_WEB_VIDEO;
			case "MENCODERVIDEO":
				return MENCODER_VIDEO;
			case "MENCODERWEBVIDEO":
				return MENCODER_WEB_VIDEO;
			case "DCRAW":
				return DCRAW;
			case "TSMUXERAUDIO":
				return TSMUXER_AUDIO;
			case "TSMUXERVIDEO":
				return TSMUXER_VIDEO;
			case "VLCAUDIOSTREAMING":
				return VLC_AUDIO_STREAMING;
			case "VLCVIDEOSTREAMING":
				return VLC_VIDEO_STREAMING;
			case "VLCVIDEO":
				return VLC_VIDEO;
			case "VLCWEBVIDEO":
				return VLC_WEB_VIDEO;
			default:
				LOGGER.warn("Could not parse player id \"{}\"", playerIdString);
				return null;
		}
	}
}
