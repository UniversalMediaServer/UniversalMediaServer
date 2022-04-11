package net.pms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;


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
