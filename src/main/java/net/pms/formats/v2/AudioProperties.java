/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
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
package net.pms.formats.v2;

import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.LibMediaInfoParser;

/**
 * Deprecated class for storing and parsing from libmediainfo output audio
 * track's properties (bitrate, channels number, etc) and meta information
 * (title, album, etc).
 *
 * This class is not thread-safe.
 *
 * @deprecated Use {@link DLNAMediaAudio} to store values,
 *             {@link LibMediaInfoParser} to parse them.
 * @since 1.60.0
 */
@Deprecated
public class AudioProperties implements Cloneable {

	private int numberOfChannels;
	private int audioDelay;
	private int sampleFrequency;

	public int getAttribute(AudioAttribute attribute) {
		switch (attribute) {
			case CHANNELS_NUMBER:
				return getNumberOfChannels();
			case DELAY:
				return getAudioDelay();
			case SAMPLE_FREQUENCY:
				return getSampleFrequency();
			default:
				throw new IllegalArgumentException("Unimplemented attribute");
		}
	}

	/**
	 * Get number of channels for this audio track.
	 *
	 * @param returnDefault Whether to return the default value if not known.
	 * @return The number of channels.
	 * @deprecated Use {@link DLNAMediaAudio#isNumberOfChannelsKnown()} instead.
	 */
	@Deprecated
	public int getNumberOfChannels(boolean returnDefault) {
		if (returnDefault) {
			return getNumberOfChannels();
		}
		return numberOfChannels;
	}

	/**
	 * Get number of channels for this audio track.
	 *
	 * @return The number of channels, or {@link #NUMBEROFCHANNELS_DEFAULT} if
	 *         {@code numberOfChannels} is invalid.
	 * @deprecated Use {@link DLNAMediaAudio#getNumberOfChannels()} instead.
	 */
	@Deprecated
	public int getNumberOfChannels() {
		return numberOfChannels > 0 ? numberOfChannels : DLNAMediaAudio.NUMBEROFCHANNELS_DEFAULT;
	}

	/**
	 * Set number of channels for this audio track.
	 *
	 * @param numberOfChannels number of channels to set.
	 * @deprecated Use {@link DLNAMediaAudio#setNumberOfChannels(int)} instead.
	 */
	@Deprecated
	public void setNumberOfChannels(int numberOfChannels) {
		this.numberOfChannels = numberOfChannels;
	}

	/**
	 * Get delay for this audio track.
	 *
	 * @return The audio delay in milliseconds.
	 * @deprecated Use {@link DLNAMediaAudio#getDelay()} instead.
	 */
	@Deprecated
	public int getAudioDelay() {
		return audioDelay;
	}

	/**
	 * Set delay for this audio track.
	 *
	 * @param audioDelay audio delay in milliseconds to set. May be negative.
	 * @deprecated Use {@link DLNAMediaAudio#setDelay(int)} instead.
	 */
	@Deprecated
	public void setAudioDelay(int audioDelay) {
		this.audioDelay = audioDelay;
	}

	/**
	 * Get sample frequency for this audio track.
	 *
	 * @param returnDefault Whether to return the default value if not known.
	 * @return The sample frequency in Hz.
	 * @deprecated Use {@link DLNAMediaAudio#isSampleRateKnown()} instead.
	 */
	@Deprecated
	public int getSampleFrequency(boolean returnDefault) {
		if (returnDefault) {
			return getSampleFrequency();
		}
		return sampleFrequency;
	}

	/**
	 * Get sample frequency for this audio track.
	 *
	 * @return The sample frequency in Hz, or
	 *         {@link DLNAMediaAudio#SAMPLERATE_DEFAULT} if
	 *         {@code sampleFrequency} is invalid.
	 * @deprecated Use {@link DLNAMediaAudio#getSampleRatesArray()} or
	 *             {@link DLNAMediaAudio#getSampleRate()} instead.
	 */
	@Deprecated
	public int getSampleFrequency() {
		return sampleFrequency > 0 ? sampleFrequency : DLNAMediaAudio.SAMPLERATE_DEFAULT;
	}

	/**
	 * Set sample frequency for this audio track.
	 *
	 * @param sampleFrequency sample frequency in Hz.
	 * @deprecated Use {@link DLNAMediaAudio#setSampleRates(int[])} or
	 *             {@link DLNAMediaAudio#setSampleRate(int)} instead.
	 */
	@Deprecated
	public void setSampleFrequency(int sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (getNumberOfChannels() == 1) {
			result.append(", Channel: ").append(getNumberOfChannels());
		} else {
			result.append(", Channels: ").append(getNumberOfChannels());
		}
		result.append(", Sample Frequency: ").append(getSampleFrequency()).append(" Hz");
		if (getAudioDelay() != 0) {
			result.append(", Delay: ").append(getAudioDelay()).append(" ms");
		}

		return result.toString();
	}

	@Override
	public AudioProperties clone() throws CloneNotSupportedException {
		return (AudioProperties) super.clone();
	}
}
