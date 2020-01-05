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
package net.pms.dlna;

import static org.junit.Assert.*;
import org.junit.Test;
import net.pms.util.H264Level;
import net.pms.util.H265Level;

public class DLNAMediaInfoTest {

	@Test
	public void testH264() throws Exception {
		DLNAMediaInfo media = new DLNAMediaInfo();

		media.setVideoFormatProfile("low@L1b");
		assertEquals(H264Level.L1b, media.getH264Level());
		assertEquals("low",media.getH264Profile());

		media.setVideoFormatProfile("Main@L2.0");
		assertEquals(H264Level.L2, media.getH264Level());
		assertEquals("main",media.getH264Profile());

		media.setVideoFormatProfile("High@L3.0");
		assertEquals(H264Level.L3, media.getH264Level());
		assertEquals("high",media.getH264Profile());

		media.setVideoFormatProfile("high@l4.0");
		assertEquals(H264Level.L4, media.getH264Level());
		assertEquals("high",media.getH264Profile());

		media.setVideoFormatProfile("hIgH@L4.1");
		assertEquals(H264Level.L4_1, media.getH264Level());
		assertEquals("high",media.getH264Profile());

		media.setVideoFormatProfile("5");
		assertEquals(H264Level.L5, media.getH264Level());
		assertEquals("5", media.getH264Profile());

		media.setVideoFormatProfile("LEVEL 5.1");
		assertEquals(H264Level.L5_1, media.getH264Level());
		assertEquals("level 5.1",media.getH264Profile());

		media.setVideoFormatProfile("level5,2");
		assertEquals(H264Level.L5_2, media.getH264Level());
		assertEquals("level5,2",media.getH264Profile());

		media.setVideoFormatProfile("level");
		assertNull(media.getH264Level());
		assertEquals("level",media.getH264Profile());
	}

	@Test
	public void testH265() throws Exception {
		DLNAMediaInfo media = new DLNAMediaInfo();

		media.setVideoFormatProfile("Main@L2.0@High");
		assertEquals(H265Level.L2, media.getH265Level());
		assertEquals("main",media.getH265Profile());

		media.setVideoFormatProfile("High@L3.0");
		assertEquals(H265Level.L3, media.getH265Level());
		assertEquals("high",media.getH265Profile());

		media.setVideoFormatProfile("mAin@l4.0@maIN");
		assertEquals(H265Level.L4, media.getH265Level());
		assertEquals("main",media.getH265Profile());

		media.setVideoFormatProfile("hIgH@L4.1");
		assertEquals(H265Level.L4_1, media.getH265Level());
		assertEquals("high",media.getH265Profile());

		media.setVideoFormatProfile("hIgH@L4.2@loW");
		assertNull(media.getH265Level());
		assertEquals("high",media.getH265Profile());

		media.setVideoFormatProfile("5");
		assertEquals(H265Level.L5, media.getH265Level());
		assertEquals("5", media.getH265Profile());

		media.setVideoFormatProfile("LEVEL 5.1");
		assertEquals(H265Level.L5_1, media.getH265Level());
		assertEquals("level 5.1",media.getH265Profile());

		media.setVideoFormatProfile("level5,2");
		assertEquals(H265Level.L5_2, media.getH265Level());
		assertEquals("level5,2",media.getH265Profile());

		media.setVideoFormatProfile("level");
		assertNull(media.getH265Level());
		assertEquals("level",media.getH265Profile());
	}
}
