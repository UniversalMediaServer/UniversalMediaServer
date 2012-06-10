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

import net.pms.configuration.PmsConfiguration;
import net.pms.formats.Format;

public class VideoLanAudioStreaming extends VideoLanVideoStreaming {
	public static final String ID = "vlcaudio";

	public VideoLanAudioStreaming(PmsConfiguration configuration) {
		super(configuration);
	}

	@Override
	public int purpose() {
		return AUDIO_WEBSTREAM_PLAYER;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "VLC Audio Streaming";
	}

	@Override
	public int type() {
		return Format.AUDIO;
	}

	@Override
	public String mimeType() {
		return "audio/wav";
	}

	@Override
	protected String getEncodingArgs() {
		return "acodec=s16l,channels=2";
	}

	@Override
	protected String getMux() {
		return "wav";
	}
}
