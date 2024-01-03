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

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import static net.pms.formats.v2.SubtitleType.VOBSUB;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.subtitle.MediaSubtitleTest;
import static net.pms.util.SubtitleUtils.getSubCpOptionForMencoder;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class SubtitleUtilsTest {
	private final Class<?> CLASS = MediaSubtitleTest.class;

	/**
	 * Set up testing conditions before running the tests.
	 */
	@BeforeEach
	public final void setUp() {
		// Silence all log messages from the PMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	@Test
	public void testGetSubCpOptionForMencoder_withNullMediaSubtitle() throws Exception {
		assertThrows(NullPointerException.class, () -> {
			getSubCpOptionForMencoder(null);
		});
	}

	@Test
	public void testGetSubCpOptionForMencoder_withoutExternalSubtitles() throws Exception {
		MediaSubtitle subtitle = new MediaSubtitle();
		assertNull(getSubCpOptionForMencoder(subtitle));
	}

	@Test
	public void testGetSubCpOptionForMencoder_withoutDetectedCharset() throws Exception {
		MediaSubtitle subtitle = new MediaSubtitle();
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		subtitle.setType(VOBSUB);
		subtitle.setExternalFile(file_cp1251);
		assertNull(subtitle.getSubCharacterSet());
		assertNull(getSubCpOptionForMencoder(subtitle));
	}

	@Test
	public void testGetSubCpOptionForMencoder() throws Exception {
		File file_big5 = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setExternalFile(file_big5);
		assertEquals(getSubCpOptionForMencoder(sub1), "enca:zh:big5");

		File file_gb18030 = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setExternalFile(file_gb18030);
		assertEquals(getSubCpOptionForMencoder(sub2), "enca:zh:big5");

		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		MediaSubtitle sub3 = new MediaSubtitle();
		sub3.setExternalFile(file_cp1251);
		assertEquals(getSubCpOptionForMencoder(sub3), "enca:ru:cp1251");

//		File file_ibm866 = FileUtils.toFile(CLASS.getResource("russian-ibm866.srt"));
//		MediaSubtitle sub4 = new MediaSubtitle();
//		sub4.setExternalFile(file_ibm866);
//		assertEquals(getSubCpOptionForMencoder(sub4), "enca:ru:cp1251");

		File file_koi8_r = FileUtils.toFile(CLASS.getResource("russian-koi8-r.srt"));
		MediaSubtitle sub5 = new MediaSubtitle();
		sub5.setExternalFile(file_koi8_r);
		assertEquals(getSubCpOptionForMencoder(sub5), "enca:ru:cp1251");

		File file_cp1250 = FileUtils.toFile(CLASS.getResource("czech-cp1250.srt"));
		MediaSubtitle sub6 = new MediaSubtitle();
		sub6.setExternalFile(file_cp1250);
		assertEquals(getSubCpOptionForMencoder(sub6), "cp1250");

		File file_iso_8859_2 = FileUtils.toFile(CLASS.getResource("hungarian-iso-8859-2.srt"));
		MediaSubtitle sub7 = new MediaSubtitle();
		sub7.setExternalFile(file_iso_8859_2);
		assertEquals(getSubCpOptionForMencoder(sub7), "ISO-8859-2");
	}

	@Test
	public void testGetSubCpOptionForMencoder_UTF() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		MediaSubtitle sub1 = new MediaSubtitle();
		sub1.setExternalFile(file_utf8);
		assertNull(getSubCpOptionForMencoder(sub1));

		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		MediaSubtitle sub2 = new MediaSubtitle();
		sub2.setExternalFile(file_utf8_2);
		assertNull(getSubCpOptionForMencoder(sub2));

		File file_utf16_le = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		MediaSubtitle sub3 = new MediaSubtitle();
		sub3.setExternalFile(file_utf16_le);
		assertNull(getSubCpOptionForMencoder(sub3));

		File file_utf16_be = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		MediaSubtitle sub4 = new MediaSubtitle();
		sub4.setExternalFile(file_utf16_be);
		assertNull(getSubCpOptionForMencoder(sub4));

		File file_utf32_le = FileUtils.toFile(CLASS.getResource("russian-utf32-le.srt"));
		MediaSubtitle sub5 = new MediaSubtitle();
		sub5.setExternalFile(file_utf32_le);
		assertNull(getSubCpOptionForMencoder(sub5));

		File file_utf32_be = FileUtils.toFile(CLASS.getResource("russian-utf32-be.srt"));
		MediaSubtitle sub6 = new MediaSubtitle();
		sub6.setExternalFile(file_utf32_be);
		assertNull(getSubCpOptionForMencoder(sub6));

		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt"));
		MediaSubtitle sub7 = new MediaSubtitle();
		sub7.setExternalFile(file_utf8_3);
		assertNull(getSubCpOptionForMencoder(sub7));
	}
}
