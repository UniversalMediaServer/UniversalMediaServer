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
package net.pms.formats.v2;

import ch.qos.logback.classic.LoggerContext;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class AudioPropertiesTest {
	private AudioProperties properties;

	@BeforeEach
	public void setUp() {
		// Silence all log messages from the PMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		properties = new AudioProperties();
	}

	@Test
	public void testDefaultValues() {
		assertEquals(properties.getNumberOfChannels(), 2);
		assertEquals(properties.getAudioDelay(), 0);
		assertEquals(properties.getSampleFrequency(), 48000);
	}

	@Test
	public void testSetNumberOfChannels_withIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> {
			properties.setNumberOfChannels(0);
		});
	}

	@Test
	public void testSampleFrequency_withIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> {
			properties.setSampleFrequency(0);
		});
	}

	@Test
	public void testGetAttribute_shouldReturnValueForAllPossibleAttributes() {
		for (AudioAttribute attribute : AudioAttribute.values()) {
			properties.getAttribute(attribute);
		}
	}

	@Test
	public void testSetNumberOfChannels() {
		properties.setNumberOfChannels(5);
		assertEquals(properties.getNumberOfChannels(), 5);
		properties.setNumberOfChannels("2 channels / 4 channel / 3 channel");
		assertEquals(properties.getNumberOfChannels(), 4);
		properties.setNumberOfChannels("-3 channel");
		assertEquals(properties.getNumberOfChannels(), 2);
	}

	@Test
	public void testSetAudioDelay() {
		properties.setAudioDelay(5);
		assertEquals(properties.getAudioDelay(), 5);
		properties.setAudioDelay("2 ms");
		assertEquals(properties.getAudioDelay(), 2);
		properties.setAudioDelay("-3");
		assertEquals(properties.getAudioDelay(), -3);
	}

	@Test
	public void testSetSampleFrequency() {
		properties.setSampleFrequency(22050);
		assertEquals(properties.getSampleFrequency(), 22050);
		properties.setSampleFrequency("22050 / 44100");
		assertEquals(properties.getSampleFrequency(), 44100);
		properties.setSampleFrequency("-3");
		assertEquals(properties.getSampleFrequency(), 48000);
	}

	@Test
	public void testGetChannelsNumberFromLibMediaInfo_withNullEmptyOrNegativeValue() {
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo(null), 2);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo(""), 2);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("-2chan"), 2);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("0"), 2);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("zero number"), 2);
	}

	@Test
	public void testGetChannelsNumberFromLibMediaInfo() {
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("1 channel"), 1);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("3 channels"), 3);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("   3 ch ls 21"), 21);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("6 channels"), 6);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("2 channels / 1 channel / 1 channel"), 2);
		assertEquals(AudioProperties.getChannelsNumberFromLibMediaInfo("2 channels / 4 channel / 3 channel"), 4);
	}

	@Test
	public void testGetAudioDelayFromLibMediaInfo_withNullEmpty() {
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo(null), 0);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo(""), 0);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("zero number"), 0);
	}

	@Test
	public void testGetAudioDelayFromLibMediaInfo() {
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("1"), 1);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("5 msec"), 5);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("5.4 milli"), 5);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("0"), 0);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("-7"), -7);
		assertEquals(AudioProperties.getAudioDelayFromLibMediaInfo("delay -15 ms"), -15);
	}

	@Test
	public void testGetSampleFrequencyFromLibMediaInfo_withNullEmpty() {
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo(null), 48000);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo(""), 48000);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("freq unknown"), 48000);
	}

	@Test
	public void testGetSampleFrequencyFromLibMediaInfo() {
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("1"), 1);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("5 Hz"), 5);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("48000"), 48000);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("44100 Hz"), 44100);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("44100 / 22050"), 44100);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("22050 / 44100 Hz"), 44100);
		assertEquals(AudioProperties.getSampleFrequencyFromLibMediaInfo("-7 kHz"), 48000);
	}
}
