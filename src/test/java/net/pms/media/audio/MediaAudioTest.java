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
package net.pms.media.audio;

import ch.qos.logback.classic.LoggerContext;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class MediaAudioTest {
	private MediaAudio audio;

	@BeforeEach
	public void setUp() {
		// Silence all log messages from the PMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
		audio = new MediaAudio();
	}

	@Test
	public void testDefaultValues() {
		assertEquals(audio.getNumberOfChannels(), 2);
		assertEquals(audio.getVideoDelay(), 0);
		assertEquals(audio.getSampleRate(), 48000);
	}

	@Test
	public void testSetNumberOfChannels_withIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> {
			audio.setNumberOfChannels(0);
		});
	}

	@Test
	public void testSampleFrequency_withIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> {
			audio.setSampleRate(0);
		});
	}

}
