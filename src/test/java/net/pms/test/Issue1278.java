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
package net.pms.test;

import ch.qos.logback.classic.LoggerContext;
import net.pms.media.MediaInfo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class Issue1278 {
	@BeforeEach
    public void setUp() {
        // Silence all log messages from the PMS code that are being tested
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset(); 
	}

	@Test
	public void dlnaMediaInfoDoubleParseWithDot() {
		MediaInfo info = new MediaInfo();
		info.setFrameRate("23.976");
		String validFps = info.getValidFps(true);
		assertNotNull(validFps, "validFps");
		assertEquals("24000/1001", validFps, "proper ratio");
		validFps = info.getValidFps(false);
		assertNotNull(validFps, "validFps");
		assertEquals("23.976", validFps, "proper ratio");
	}

	@Test
	public void dlnaMediaInfoDoubleParseWithComma() {
		MediaInfo info = new MediaInfo();
		info.setFrameRate("23,976");
		String validFps = info.getValidFps(true);
		assertNotNull(validFps, "validFps");
		assertEquals("24000/1001", validFps, "proper ratio");
		validFps = info.getValidFps(false);
		assertNotNull(validFps, "validFps");
		assertEquals("23.976", validFps, "proper ratio");
	}

	@Test
	public void testNullFrameRate() {
		MediaInfo info = new MediaInfo();
		assertNull(info.getValidFps(true), "valid fps");
	}

}
