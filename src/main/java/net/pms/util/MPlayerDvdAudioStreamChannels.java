/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import net.pms.configuration.FormatConfiguration;


/**
 * This {@code enum} represents MPlayer's
 * {@code stream/stream_dvd_common.c dvd_audio_stream_channels} array.
 *
 * @author Nadahar
 */
public enum MPlayerDvdAudioStreamChannels {

	/** Mono */
	MONO("mono", 1),

	/** Stereo */
	STEREO("stereo", 2),

	/** Unknown number of channels */
	UNKNOWN("unknown", 2),

	/** Unclear what mode this represents */
	SURROUND_6_1("5.1/6.1", 7),

	/** Surround 5.1 */
	SOURRUND("5.1", 6);

	private final String mPlayerCode;
	private final int numberOfChannels;

	private MPlayerDvdAudioStreamChannels(String mPlayerCode, int numberOfChannels) {
		this.mPlayerCode = mPlayerCode;
		this.numberOfChannels = numberOfChannels;
	}

	/**
	 * @return The MPlayer code for this DVD audio stream.
	 */
	public String getMPlayerCode() {
		return mPlayerCode;
	}

	/**
	 * @return The {@link FormatConfiguration} constant/codec for this DVD audio
	 *         stream type.
	 */
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	/**
	 * Returns the {@link MPlayerDvdAudioStreamChannels} type corresponding to
	 * the given MPlayer audio channels output.
	 *
	 * @param mPlayerCode the MPlayer audio channels string.
	 * @return The corresponding {@link MPlayerDvdAudioStreamChannels} or
	 *         {@code null}.
	 */
	public static MPlayerDvdAudioStreamChannels typeOf(String mPlayerCode) {
		if (isBlank(mPlayerCode)) {
			return null;
		}

		for (MPlayerDvdAudioStreamChannels type : MPlayerDvdAudioStreamChannels.values()) {
			if (mPlayerCode.equals(type.mPlayerCode)) {
				return type;
			}
		}
		return null;
	}
}
