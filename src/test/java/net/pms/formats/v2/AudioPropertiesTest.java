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

import static org.fest.assertions.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

public class AudioPropertiesTest {
	private AudioProperties properties;

	@Before
	public void setUp() {
		properties = new AudioProperties();
	}

	@Test
	public void testDefaultValues() {
		assertThat(properties.getNumberOfChannels()).isEqualTo(2);
		assertThat(properties.getAudioDelay()).isEqualTo(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetNumberOfChannels_withIllegalArgument() {
		AudioProperties attributes = new AudioProperties();
		attributes.setNumberOfChannels(0);
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
		assertThat(properties.getNumberOfChannels()).isEqualTo(5);
		properties.setNumberOfChannels("2 channels / 4 channel / 3 channel");
		assertThat(properties.getNumberOfChannels()).isEqualTo(4);
		properties.setNumberOfChannels("-3 channel");
		assertThat(properties.getNumberOfChannels()).isEqualTo(2);
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
	public void testGetChannelsNumberFromLibMediaInfo_withNullEmptyOrNegativeValue() {
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo(null)).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("")).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("-2chan")).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("0")).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("zero number")).isEqualTo(2);
	}

	@Test
	public void testGetChannelsNumberFromLibMediaInfo() {
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("1 channel")).isEqualTo(1);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("3 channels")).isEqualTo(3);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("   3 ch ls 21")).isEqualTo(21);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("6 channels")).isEqualTo(6);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("2 channels / 1 channel / 1 channel")).isEqualTo(2);
		assertThat(AudioProperties.getChannelsNumberFromLibMediaInfo("2 channels / 4 channel / 3 channel")).isEqualTo(4);
	}

	@Test
	public void testGetAudioDelayFromLibMediaInfo_withNullEmpty() {
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo(null)).isEqualTo(0);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("")).isEqualTo(0);
		assertThat(AudioProperties.getAudioDelayFromLibMediaInfo("zero number")).isEqualTo(0);
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
}
