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
package net.pms.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.pms.media.subtitle.MediaSubtitleTest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class LoggableInputStreamTest {

	public static LoggableInputStream getTestStream() {
		return new LoggableInputStream(MediaSubtitleTest.class.getResourceAsStream("english-utf8-with-bom.srt"), StandardCharsets.UTF_8);
	}

	@Test
	public void constructorTest() throws IOException {
		try (LoggableInputStream lis = new LoggableInputStream(null)) {
			assertEquals(StandardCharsets.ISO_8859_1, lis.logCharset);
			assertNull(lis.inputStream);
		}
		try (LoggableInputStream lis = new LoggableInputStream(null, StandardCharsets.UTF_8)) {
			assertEquals(StandardCharsets.UTF_8, lis.logCharset);
			assertNull(lis.inputStream);
		}
	}

	@Test
	public void readTest() throws IOException {
		byte[] buf = new byte[64];
		try (LoggableInputStream lis = new LoggableInputStream(null)) {
			assertEquals(-1, lis.read());
			assertEquals(0, lis.position);
			assertEquals(0, lis.readPosition);
			assertEquals(-1, lis.read(buf));
			assertEquals(0, lis.position);
			assertEquals(0, lis.readPosition);
			assertEquals(-1, lis.read(buf, 10, 20));
			assertEquals(0, lis.position);
			assertEquals(0, lis.readPosition);
		}
		try (LoggableInputStream lis = getTestStream()) {
			assertEquals(239, lis.read());
			assertEquals(1, lis.position);
			assertEquals(1, lis.readPosition);
			assertEquals("\ufffd", lis.toString());
			assertEquals(64, lis.read(buf));
			assertEquals(65, lis.position);
			assertEquals(65, lis.readPosition);
			assertEquals("﻿1\r\n00:00:02,107 --> 00:00:05,810\r\nAlcide, you sure know\r\nhow t", lis.toString());
			assertEquals(32, lis.read(buf, 24, 32));
			assertEquals(97, lis.position);
			assertEquals(97, lis.readPosition);
			assertEquals("﻿1\r\n00:00:02,107 --> 00:00:05,810\r\nAlcide, you sure know\r\nhow to treat a lady.\r\n\r\n2\r\n00:00:05,8", lis.toString());
			assertEquals("\ufffd\ufffd1\r\n00:00:02,107 --> 00o treat a lady.\r\n\r\n2\r\n00:00:05,8w\r\nhow t", new String(buf, StandardCharsets.UTF_8));
		}
	}

	@Test
	public void skipMarkTest() throws IOException {
		try (LoggableInputStream lis = new LoggableInputStream(null)) {
			assertEquals(0, lis.available());
			lis.mark(100);
			assertEquals(0, lis.markPosition);
			assertFalse(lis.markSupported());
			lis.reset();
			assertEquals(0, lis.skip(196));
			assertEquals(0, lis.position);
			assertEquals(0, lis.readPosition);
			assertEquals("", lis.toString());
		}
		byte[] buf = new byte[64];
		try (LoggableInputStream lis = getTestStream()) {
			assertTrue(lis.markSupported());
			assertEquals(10, lis.skip(10));
			lis.mark(200);
			assertEquals(32, lis.skip(32));
			assertEquals(42, lis.position);
			assertEquals(42, lis.readPosition);
			assertEquals("﻿1\r\n00:00:02,107 --> 00:00:05,810\r\nAlcid", lis.toString());
			lis.reset();
			assertEquals(10, lis.position);
			assertEquals(42, lis.readPosition);
			assertEquals(64, lis.read(buf));
			assertEquals(74, lis.position);
			assertEquals(74, lis.readPosition);
			assertEquals("﻿1\r\n00:00:02,107 --> 00:00:05,810\r\nAlcide, you sure know\r\nhow to treat a", lis.toString());
			lis.reset();
			assertEquals(32, lis.read(buf, 24, 32));
			assertEquals(42, lis.position);
			assertEquals(74, lis.readPosition);
			assertEquals("﻿1\r\n00:00:02,107 --> 00:00:05,810\r\nAlcide, you sure know\r\nhow to treat a", lis.toString());
			assertEquals("0:02,107 --> 00:00:05,810:02,107 --> 00:00:05,810\r\nAlcid treat a", new String(buf, StandardCharsets.UTF_8));
		}
	}

}
