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
package net.pms.encoders;

import net.pms.TestHelper;
import net.pms.media.MediaInfo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Issue1278 {

	@BeforeEach
    public void setUp() {
        TestHelper.SetLoggingOff();
	}

	@Test
	public void dlnaMediaInfoDoubleParseWithDot() {
		MediaInfo info = new MediaInfo();
		info.setFrameRate(23.976);
		String validFps = Engine.getValidFps(info.getFrameRate(), true);
		assertNotNull(validFps, "validFps");
		assertEquals("24000/1001", validFps, "proper ratio");
		validFps = Engine.getValidFps(info.getFrameRate(), false);
		assertNotNull(validFps, "validFps");
		assertEquals("23.976", validFps, "proper ratio");
	}

	@Test
	public void testNullFrameRate() {
		MediaInfo info = new MediaInfo();
		assertNull(Engine.getValidFps(info.getFrameRate(), true), "valid fps");
	}

}
