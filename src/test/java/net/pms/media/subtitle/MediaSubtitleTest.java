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
package net.pms.media.subtitle;

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.io.FileNotFoundException;
import static net.pms.formats.v2.SubtitleType.*;
import static net.pms.util.Constants.*;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class MediaSubtitleTest {
	private final Class<?> CLASS = MediaSubtitleTest.class;

	/**
	 * Set up testing conditions before running the tests.
	 */
	@BeforeEach
	public final void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	@Test
	public void testDefaultSubtitleType() {
		MediaSubtitle dlnaMediaSubtitle = new MediaSubtitle();
		assertEquals(dlnaMediaSubtitle.getType(), UNKNOWN);
	}

	@Test
	public void testSetType_withNullSubtitleType() {
		assertThrows(IllegalArgumentException.class, () -> {
			MediaSubtitle dlnaMediaSubtitle = new MediaSubtitle();
			dlnaMediaSubtitle.setType(null);
		});
	}

	@Test
	public void testSetExternalFile_noFile() throws Exception {
		assertThrows(FileNotFoundException.class, () -> {
			MediaSubtitle dlnaMediaSubtitle = new MediaSubtitle();
			dlnaMediaSubtitle.setExternalFile(new File("no-such-file.xyz"));
		});
	}

	@Test
	public void testSetExternalFile_UTF() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setExternalFile(file_utf8);
		assertEquals(sub1.getSubCharacterSet(), CHARSET_UTF_8);
		assertTrue(sub1.isExternalFileUtf8());
		assertFalse(sub1.isExternalFileUtf16());
		assertTrue(sub1.isExternalFileUtf());

		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setExternalFile(file_utf8_2);
		assertEquals(sub2.getSubCharacterSet(), CHARSET_UTF_8);
		assertTrue(sub2.isExternalFileUtf8());
		assertFalse(sub2.isExternalFileUtf16());
		assertTrue(sub2.isExternalFileUtf());

		File file_utf16_le = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		MediaSubtitle sub3 = new MediaSubtitle();
		sub3.setExternalFile(file_utf16_le);
		assertEquals(sub3.getSubCharacterSet(), CHARSET_UTF_16LE);
		assertFalse(sub3.isExternalFileUtf8());
		assertTrue(sub3.isExternalFileUtf16());
		assertTrue(sub3.isExternalFileUtf());

		File file_utf16_be = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		MediaSubtitle sub4 = new MediaSubtitle();
		sub4.setExternalFile(file_utf16_be);
		assertEquals(sub4.getSubCharacterSet(), CHARSET_UTF_16BE);
		assertFalse(sub4.isExternalFileUtf8());
		assertTrue(sub4.isExternalFileUtf16());
		assertTrue(sub4.isExternalFileUtf());

		File file_utf32_le = FileUtils.toFile(CLASS.getResource("russian-utf32-le.srt"));
		MediaSubtitle sub5 = new MediaSubtitle();
		sub5.setExternalFile(file_utf32_le);
		assertEquals(sub5.getSubCharacterSet(), CHARSET_UTF_32LE);
		assertFalse(sub5.isExternalFileUtf8());
		assertFalse(sub5.isExternalFileUtf16());
		assertTrue(sub5.isExternalFileUtf());

		File file_utf32_be = FileUtils.toFile(CLASS.getResource("russian-utf32-be.srt"));
		MediaSubtitle sub6 = new MediaSubtitle();
		sub6.setExternalFile(file_utf32_be);
		assertEquals(sub6.getSubCharacterSet(), CHARSET_UTF_32BE);
		assertFalse(sub6.isExternalFileUtf8());
		assertFalse(sub6.isExternalFileUtf16());
		assertTrue(sub6.isExternalFileUtf());

		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt"));
		MediaSubtitle sub7 = new MediaSubtitle();
		sub7.setExternalFile(file_utf8_3);
		assertEquals(sub7.getSubCharacterSet(), CHARSET_UTF_8);
		assertTrue(sub7.isExternalFileUtf8());
		assertFalse(sub7.isExternalFileUtf16());
		assertTrue(sub7.isExternalFileUtf());
	}

	@Test
	public void testSetExternalFile_charset() throws Exception {
		File file_big5 = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setExternalFile(file_big5);
		assertEquals(sub1.getSubCharacterSet(), CHARSET_BIG5);
		assertFalse(sub1.isExternalFileUtf8());
		assertFalse(sub1.isExternalFileUtf16());
		assertFalse(sub1.isExternalFileUtf());
		assertEquals(sub1.getLang(), "zh");

		File file_gb18030 = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setExternalFile(file_gb18030);
		assertEquals(sub2.getSubCharacterSet(), CHARSET_GB18030);
		assertFalse(sub2.isExternalFileUtf8());
		assertFalse(sub2.isExternalFileUtf16());
		assertFalse(sub2.isExternalFileUtf());
		assertEquals(sub2.getLang(), "zh");

		File file_cp1250 = FileUtils.toFile(CLASS.getResource("czech-cp1250.srt"));
		MediaSubtitle sub3 = new MediaSubtitle();
		sub3.setExternalFile(file_cp1250);
		assertEquals(sub3.getSubCharacterSet(), CHARSET_WINDOWS_1250);
		assertFalse(sub3.isExternalFileUtf8());
		assertFalse(sub3.isExternalFileUtf16());
		assertFalse(sub3.isExternalFileUtf());
		assertEquals(sub3.getLang(), "cs");

		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		MediaSubtitle sub4 = new MediaSubtitle();
		sub4.setExternalFile(file_cp1251);
		assertEquals(sub4.getSubCharacterSet(), CHARSET_WINDOWS_1251);
		assertFalse(sub4.isExternalFileUtf8());
		assertFalse(sub4.isExternalFileUtf16());
		assertFalse(sub4.isExternalFileUtf());
		assertEquals(sub4.getLang(), "ru");

		File file_iso_8859_2 = FileUtils.toFile(CLASS.getResource("hungarian-iso-8859-2.srt"));
		MediaSubtitle sub5 = new MediaSubtitle();
		sub5.setExternalFile(file_iso_8859_2);
		assertEquals(sub5.getSubCharacterSet(), CHARSET_ISO_8859_2);
		assertFalse(sub5.isExternalFileUtf8());
		assertFalse(sub5.isExternalFileUtf16());
		assertFalse(sub5.isExternalFileUtf());
		assertEquals(sub5.getLang(), "hu");

		File file_koi8_r = FileUtils.toFile(CLASS.getResource("russian-koi8-r.srt"));
		MediaSubtitle sub6 = new MediaSubtitle();
		sub6.setExternalFile(file_koi8_r);
		assertEquals(sub6.getSubCharacterSet(), CHARSET_KOI8_R);
		assertFalse(sub6.isExternalFileUtf8());
		assertFalse(sub6.isExternalFileUtf16());
		assertFalse(sub6.isExternalFileUtf());
		assertEquals(sub6.getLang(), "ru");

		File file_iso_8859_8 = FileUtils.toFile(CLASS.getResource("hebrew-iso-8859-8.srt"));
		MediaSubtitle sub7 = new MediaSubtitle();
		sub7.setExternalFile(file_iso_8859_8);
		assertEquals(sub7.getSubCharacterSet(), CHARSET_ISO_8859_8);
		assertFalse(sub7.isExternalFileUtf8());
		assertFalse(sub7.isExternalFileUtf16());
		assertFalse(sub7.isExternalFileUtf());
		assertEquals(sub7.getLang(), "he");
	}

	@Test
	public void testSetExternalFile_bitmapSubs() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));

		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setType(VOBSUB);
		sub1.setExternalFile(file_cp1251);
		assertNull(sub1.getSubCharacterSet());

		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setType(BMP);
		sub2.setExternalFile(file_cp1251);
		assertNull(sub2.getSubCharacterSet());

		MediaSubtitle sub3 = new MediaSubtitle();
		sub3.setType(DIVX);
		sub3.setExternalFile(file_cp1251);
		assertNull(sub3.getSubCharacterSet());

		MediaSubtitle sub4 = new MediaSubtitle();
		sub4.setType(PGS);
		sub4.setExternalFile(file_cp1251);
		assertNull(sub4.getSubCharacterSet());
	}

	@Test
	public void testSetExternalFile_textSubs() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));

		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setType(SUBRIP);
		sub1.setExternalFile(file_cp1251);
		assertEquals(sub1.getSubCharacterSet(), CHARSET_WINDOWS_1251);

		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setType(ASS);
		sub2.setExternalFile(file_cp1251);
		assertEquals(sub2.getSubCharacterSet(), CHARSET_WINDOWS_1251);
	}

	@Test
	public void testIsEmbedded() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setType(SUBRIP);
		sub1.setExternalFile(file_cp1251);
		assertFalse(sub1.isEmbedded());
		assertTrue(sub1.isExternal());

		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setType(SUBRIP);
		assertTrue(sub2.isEmbedded());
		assertFalse(sub2.isExternal());
	}
}
