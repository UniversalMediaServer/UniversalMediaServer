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

import static net.pms.formats.v2.AudioAttribute.*;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

public class AudioAttributeTest {
	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_matchingKeyValuePairs() throws Exception {
		assertThat(getAudioAttributeByLibMediaInfoKeyValuePair("Channel(s)                       : 6")).isEqualTo(CHANNELS_NUMBER);
		assertThat(getAudioAttributeByLibMediaInfoKeyValuePair(" Channel(s):6")).isEqualTo(CHANNELS_NUMBER);
		assertThat(getAudioAttributeByLibMediaInfoKeyValuePair("Video_Delay                      : 0")).isEqualTo(DELAY);
		assertThat(getAudioAttributeByLibMediaInfoKeyValuePair("SamplingRate                     : 48000")).isEqualTo(SAMPLE_FREQUENCY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_emptyKeyValuePair() throws Exception {
		getAudioAttributeByLibMediaInfoKeyValuePair("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_nullKeyValuePair() throws Exception {
		getAudioAttributeByLibMediaInfoKeyValuePair(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_noMatchingKeyValuePair() throws Exception {
		getAudioAttributeByLibMediaInfoKeyValuePair("Temperature                       : 36");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_noKeyInKeyValuePair() throws Exception {
		getAudioAttributeByLibMediaInfoKeyValuePair("C3PO/R2D2");
	}
}
