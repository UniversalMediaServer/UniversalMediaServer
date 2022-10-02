/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class LoggableOutputStreamTest {

	@Test
	public void constructorTest() throws IOException {
		try (LoggableOutputStream los = new LoggableOutputStream(null)) {
			assertEquals(StandardCharsets.ISO_8859_1, los.logCharset);
			assertNull(los.outputStream);
		}
		try (LoggableOutputStream los = new LoggableOutputStream(null, StandardCharsets.UTF_8)) {
			assertEquals(StandardCharsets.UTF_8, los.logCharset);
			assertNull(los.outputStream);
		}
	}

	@Test
	public void writeTest() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (LoggableOutputStream los = new LoggableOutputStream(bos, StandardCharsets.UTF_16BE)) {
			assertEquals("", los.toString());
			assertEquals(los.toString(), bos.toString(StandardCharsets.UTF_16BE.name()));
			los.write(0);
			los.write(65);
			assertEquals("A", los.toString());
			assertEquals(los.toString(), bos.toString(StandardCharsets.UTF_16BE.name()));
			byte[] buf = "testing".getBytes(StandardCharsets.UTF_16BE);
			los.write(buf);
			assertEquals("Atesting", los.toString());
			assertEquals(los.toString(), bos.toString(StandardCharsets.UTF_16BE.name()));
			los.write(buf, 8, 6);
			assertEquals("Atestinging", los.toString());
			assertEquals(los.toString(), bos.toString(StandardCharsets.UTF_16BE.name()));
		}
	}

}
