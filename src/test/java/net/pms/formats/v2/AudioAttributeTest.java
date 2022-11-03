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

import static net.pms.formats.v2.AudioAttribute.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AudioAttributeTest {
	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_matchingKeyValuePairs() throws Exception {
		assertEquals(getAudioAttributeByLibMediaInfoKeyValuePair("Channel(s)                       : 6"), CHANNELS_NUMBER);
		assertEquals(getAudioAttributeByLibMediaInfoKeyValuePair(" Channel(s):6"), CHANNELS_NUMBER);
		assertEquals(getAudioAttributeByLibMediaInfoKeyValuePair("Video_Delay                      : 0"), DELAY);
		assertEquals(getAudioAttributeByLibMediaInfoKeyValuePair("SamplingRate                     : 48000"), SAMPLE_FREQUENCY);
	}

	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_emptyKeyValuePair() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			getAudioAttributeByLibMediaInfoKeyValuePair("");
		});
	}

	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_nullKeyValuePair() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			getAudioAttributeByLibMediaInfoKeyValuePair(null);
		});
	}

	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_noMatchingKeyValuePair() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			getAudioAttributeByLibMediaInfoKeyValuePair("Temperature                       : 36");
		});
	}

	@Test
	public void testGetAudioAttributeByLibMediaInfoKeyValuePair_noKeyInKeyValuePair() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			getAudioAttributeByLibMediaInfoKeyValuePair("C3PO/R2D2");
		});
	}
}
