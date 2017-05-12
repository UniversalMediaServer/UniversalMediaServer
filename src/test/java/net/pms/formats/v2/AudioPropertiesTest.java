/*
 * PS3 Media Server, for streaming any media to your PS3.
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

import ch.qos.logback.classic.LoggerContext;
import static org.assertj.core.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class AudioPropertiesTest {
	private AudioProperties properties;

	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		properties = new AudioProperties();
	}

	@Test
	public void testDefaultValues() {
		assertThat(properties.getBitRate()).isEqualTo(AudioProperties.BITRATE_DEFAULT);
		assertThat(properties.getNumberOfChannels()).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		assertThat(properties.getAudioDelay()).isEqualTo(AudioProperties.AUDIODELAY_DEFAULT);
		assertThat(properties.getSampleFrequency()).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
		assertThat(properties.getBitsperSample()).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
	}

	@Test
	public void testGetAttribute_shouldReturnValueForAllPossibleAttributes() {
		for (AudioAttribute attribute : AudioAttribute.values()) {
			properties.getAttribute(attribute);
		}
	}

	@Test
	public void testSetBitRate() {
		properties.setBitRate(5000);
		assertThat(properties.getBitRate()).isEqualTo(5000);
		properties.setBitRate("unknown / unknown / 1509000");
		assertThat(properties.getBitRate()).isEqualTo(1509000);
		properties.setBitRate("0");
		assertThat(properties.getBitRate()).isEqualTo(AudioProperties.BITRATE_DEFAULT);
		properties.setBitRate("-3");
		assertThat(properties.getBitRate()).isEqualTo(AudioProperties.BITRATE_DEFAULT);
	}

	@Test
	public void testSetNumberOfChannels() {
		properties.setNumberOfChannels(5);
		assertThat(properties.getNumberOfChannels()).isEqualTo(5);
		properties.setNumberOfChannels("8 channels / 6 channels / 2 channel");
		assertThat(properties.getNumberOfChannels()).isEqualTo(8);
		properties.setNumberOfChannels("0");
		assertThat(properties.getNumberOfChannels()).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		properties.setNumberOfChannels("-3 channel");
		assertThat(properties.getNumberOfChannels()).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
	}

	@Test
	public void testSetAudioDelay() {
		properties.setAudioDelay(5);
		assertThat(properties.getAudioDelay()).isEqualTo(5);
		properties.setAudioDelay("2 ms");
		assertThat(properties.getAudioDelay()).isEqualTo(2);
		properties.setAudioDelay("-3");
		assertThat(properties.getAudioDelay()).isEqualTo(-3);
	}

	@Test
	public void testSetSampleFrequency() {
		properties.setSampleFrequency(22050);
		assertThat(properties.getSampleFrequency()).isEqualTo(22050);
		properties.setSampleFrequency("44100 / 22050");
		assertThat(properties.getSampleFrequency()).isEqualTo(44100);
		properties.setSampleFrequency("0");
		assertThat(properties.getSampleFrequency()).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
		properties.setSampleFrequency("-3");
		assertThat(properties.getSampleFrequency()).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
	}

	@Test
	public void testSetBitsperSample() {
		properties.setBitsperSample(24);
		assertThat(properties.getBitsperSample()).isEqualTo(24);
		properties.setBitsperSample("24 / 16");
		assertThat(properties.getBitsperSample()).isEqualTo(24);
		properties.setBitsperSample("0");
		assertThat(properties.getBitsperSample()).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
		properties.setBitsperSample("-3");
		assertThat(properties.getBitsperSample()).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
	}

	@Test
	public void testGetChannelsNumberFromLibMediaInfo_withNullEmptyOrNegativeValue() {
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo(null)).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("")).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("-2chan")).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("0")).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("zero number")).isEqualTo(AudioProperties.NUMBEROFCHANNELS_DEFAULT);
	}

	@Test
	public void testGetChannelsNumberFromLibMediaInfo() {
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("1 channel")).isEqualTo(1);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("3 channels")).isEqualTo(3);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("6 channels")).isEqualTo(6);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("2 channels / 1 channel / 1 channel")).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("8 channels / 6 channel / 3 channel")).isEqualTo(8);
	}

	@Test
	public void testGetAudioDelayFromLibMediaInfo_withNullEmpty() {
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo(null)).isEqualTo(AudioProperties.AUDIODELAY_DEFAULT);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("")).isEqualTo(AudioProperties.AUDIODELAY_DEFAULT);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("zero number")).isEqualTo(AudioProperties.AUDIODELAY_DEFAULT);
	}

	@Test
	public void testGetAudioDelayFromLibMediaInfo() {
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("1")).isEqualTo(1);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("5 msec")).isEqualTo(5);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("5.4 milli")).isEqualTo(5);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("0")).isEqualTo(0);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("-7")).isEqualTo(-7);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("delay -15 ms")).isEqualTo(-15);
	}

	@Test
	public void testGetSampleFrequencyFromLibMediaInfo_withNullEmpty() {
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo(null)).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("")).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("freq unknown")).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
	}

	@Test
	public void testGetSampleFrequencyFromLibMediaInfo() {
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("1")).isEqualTo(1);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("5 Hz")).isEqualTo(5);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("48000")).isEqualTo(48000);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("44100 Hz")).isEqualTo(44100);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("44100 / 22050")).isEqualTo(44100);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("44100 / 22050 Hz")).isEqualTo(44100);
		assertThat(AudioProperties.getSampleFrequencyFromLibMediaInfo("-7 kHz")).isEqualTo(AudioProperties.SAMPLEFREQUENCY_DEFAULT);
	}

	@Test
	public void testGetBitRateFromLibMediaInfo_withNullEmpty() {
		assertThat(AudioProperties.getBitRateFromLibMediaInfo(null)).isEqualTo(AudioProperties.BITRATE_DEFAULT);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("")).isEqualTo(AudioProperties.BITRATE_DEFAULT);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("bitrate unknown")).isEqualTo(AudioProperties.BITRATE_DEFAULT);
	}

	@Test
	public void testGetBitRateFromLibMediaInfo() {
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("1")).isEqualTo(1);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("8000")).isEqualTo(8000);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("3018000 / 1509000 / 640000")).isEqualTo(3018000);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("3018000 / 1509000")).isEqualTo(3018000);
		assertThat(AudioProperties.getBitRateFromLibMediaInfo("-3 kb/s")).isEqualTo(AudioProperties.BITRATE_DEFAULT);
	}

	@Test
	public void testGetBitperSampleFromLibMediaInfo_withNullEmpty() {
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo(null)).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("")).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("bitdepth unknown")).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
	}

	@Test
	public void testGetBitperSampleFromLibMediaInfo() {
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("1")).isEqualTo(1);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("16 bits")).isEqualTo(16);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("32")).isEqualTo(32);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("32 bits")).isEqualTo(32);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("32 bits / 24 bits / 16 bits")).isEqualTo(32);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("24 / 16")).isEqualTo(24);
		assertThat(AudioProperties.getBitsperSampleFromLibMediaInfo("-3 bits")).isEqualTo(AudioProperties.BITSPERSAMPLE_DEFAULT);
	}

}
