/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
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

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.io.FileNotFoundException;
import static net.pms.formats.v2.SubtitleType.*;
import org.apache.commons.io.FileUtils;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mozilla.universalchardet.Constants.*;
import org.slf4j.LoggerFactory;

public class DLNAMediaSubtitleTest {
	private final Class<?> CLASS = DLNAMediaSubtitleTest.class;

	/**
	 * Set up testing conditions before running the tests.
	 */
	@Before
	public final void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	@Test
	public void testDefaultSubtitleType() {
		DLNAMediaSubtitle dlnaMediaSubtitle = new DLNAMediaSubtitle();
		assertThat(dlnaMediaSubtitle.getType()).isEqualTo(UNKNOWN);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetType_withNullSubtitleType() {
		DLNAMediaSubtitle dlnaMediaSubtitle = new DLNAMediaSubtitle();
		dlnaMediaSubtitle.setType(null);
	}

	@Test(expected = FileNotFoundException.class)
	public void testSetExternalFile_noFile() throws Exception {
		DLNAMediaSubtitle dlnaMediaSubtitle = new DLNAMediaSubtitle();
		dlnaMediaSubtitle.setExternalFile(new File("no-such-file.xyz"));
	}

	@Test
	public void testSetExternalFile_UTF() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("../util/russian-utf8-without-bom.srt"));
		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setExternalFile(file_utf8);
		assertThat(sub1.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_8);
		assertThat(sub1.isExternalFileUtf8()).isTrue();
		assertThat(sub1.isExternalFileUtf16()).isFalse();
		assertThat(sub1.isExternalFileUtf()).isTrue();

		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("../util/russian-utf8-with-bom.srt"));
		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setExternalFile(file_utf8_2);
		assertThat(sub2.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_8);
		assertThat(sub2.isExternalFileUtf8()).isTrue();
		assertThat(sub2.isExternalFileUtf16()).isFalse();
		assertThat(sub2.isExternalFileUtf()).isTrue();

		File file_utf16_le = FileUtils.toFile(CLASS.getResource("../util/russian-utf16-le.srt"));
		DLNAMediaSubtitle sub3 = new DLNAMediaSubtitle();
		sub3.setExternalFile(file_utf16_le);
		assertThat(sub3.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_16LE);
		assertThat(sub3.isExternalFileUtf8()).isFalse();
		assertThat(sub3.isExternalFileUtf16()).isTrue();
		assertThat(sub3.isExternalFileUtf()).isTrue();

		File file_utf16_be = FileUtils.toFile(CLASS.getResource("../util/russian-utf16-be.srt"));
		DLNAMediaSubtitle sub4 = new DLNAMediaSubtitle();
		sub4.setExternalFile(file_utf16_be);
		assertThat(sub4.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_16BE);
		assertThat(sub4.isExternalFileUtf8()).isFalse();
		assertThat(sub4.isExternalFileUtf16()).isTrue();
		assertThat(sub4.isExternalFileUtf()).isTrue();

		File file_utf32_le = FileUtils.toFile(CLASS.getResource("../util/russian-utf32-le.srt"));
		DLNAMediaSubtitle sub5 = new DLNAMediaSubtitle();
		sub5.setExternalFile(file_utf32_le);
		assertThat(sub5.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_32LE);
		assertThat(sub5.isExternalFileUtf8()).isFalse();
		assertThat(sub5.isExternalFileUtf16()).isFalse();
		assertThat(sub5.isExternalFileUtf()).isTrue();

		File file_utf32_be = FileUtils.toFile(CLASS.getResource("../util/russian-utf32-be.srt"));
		DLNAMediaSubtitle sub6 = new DLNAMediaSubtitle();
		sub6.setExternalFile(file_utf32_be);
		assertThat(sub6.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_32BE);
		assertThat(sub6.isExternalFileUtf8()).isFalse();
		assertThat(sub6.isExternalFileUtf16()).isFalse();
		assertThat(sub6.isExternalFileUtf()).isTrue();

		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("../util/english-utf8-with-bom.srt"));
		DLNAMediaSubtitle sub7 = new DLNAMediaSubtitle();
		sub7.setExternalFile(file_utf8_3);
		assertThat(sub7.getExternalFileCharacterSet()).isEqualTo(CHARSET_UTF_8);
		assertThat(sub7.isExternalFileUtf8()).isTrue();
		assertThat(sub7.isExternalFileUtf16()).isFalse();
		assertThat(sub7.isExternalFileUtf()).isTrue();
	}

	@Test
	public void testSetExternalFile_charset() throws Exception {
		File file_big5 = FileUtils.toFile(CLASS.getResource("../util/chinese-big5.srt"));
		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setExternalFile(file_big5);
		assertThat(sub1.getExternalFileCharacterSet()).isEqualTo(CHARSET_BIG5);
		assertThat(sub1.isExternalFileUtf8()).isFalse();
		assertThat(sub1.isExternalFileUtf16()).isFalse();
		assertThat(sub1.isExternalFileUtf()).isFalse();

		File file_gb18030 = FileUtils.toFile(CLASS.getResource("../util/chinese-gb18030.srt"));
		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setExternalFile(file_gb18030);
		assertThat(sub2.getExternalFileCharacterSet()).isEqualTo(CHARSET_GB18030);
		assertThat(sub2.isExternalFileUtf8()).isFalse();
		assertThat(sub2.isExternalFileUtf16()).isFalse();
		assertThat(sub2.isExternalFileUtf()).isFalse();

		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../util/russian-cp1251.srt"));
		DLNAMediaSubtitle sub3 = new DLNAMediaSubtitle();
		sub3.setExternalFile(file_cp1251);
		assertThat(sub3.getExternalFileCharacterSet()).isEqualTo(CHARSET_WINDOWS_1251);
		assertThat(sub3.isExternalFileUtf8()).isFalse();
		assertThat(sub3.isExternalFileUtf16()).isFalse();
		assertThat(sub3.isExternalFileUtf()).isFalse();

		File file_ibm866 = FileUtils.toFile(CLASS.getResource("../util/russian-ibm866.srt"));
		DLNAMediaSubtitle sub4 = new DLNAMediaSubtitle();
		sub4.setExternalFile(file_ibm866);
		assertThat(sub4.getExternalFileCharacterSet()).isEqualTo(CHARSET_IBM866);
		assertThat(sub4.isExternalFileUtf8()).isFalse();
		assertThat(sub4.isExternalFileUtf16()).isFalse();
		assertThat(sub4.isExternalFileUtf()).isFalse();

		File file_koi8_r = FileUtils.toFile(CLASS.getResource("../util/russian-koi8-r.srt"));
		DLNAMediaSubtitle sub5 = new DLNAMediaSubtitle();
		sub5.setExternalFile(file_koi8_r);
		assertThat(sub5.getExternalFileCharacterSet()).isEqualTo(CHARSET_KOI8_R);
		assertThat(sub5.isExternalFileUtf8()).isFalse();
		assertThat(sub5.isExternalFileUtf16()).isFalse();
		assertThat(sub5.isExternalFileUtf()).isFalse();
	}

	@Test
	public void testSetExternalFile_bitmapSubs() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../util/russian-cp1251.srt"));

		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setType(VOBSUB);
		sub1.setExternalFile(file_cp1251);
		assertThat(sub1.getExternalFileCharacterSet()).isNull();

		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setType(BMP);
		sub2.setExternalFile(file_cp1251);
		assertThat(sub2.getExternalFileCharacterSet()).isNull();

		DLNAMediaSubtitle sub3 = new DLNAMediaSubtitle();
		sub3.setType(DIVX);
		sub3.setExternalFile(file_cp1251);
		assertThat(sub3.getExternalFileCharacterSet()).isNull();

		DLNAMediaSubtitle sub4 = new DLNAMediaSubtitle();
		sub4.setType(PGS);
		sub4.setExternalFile(file_cp1251);
		assertThat(sub4.getExternalFileCharacterSet()).isNull();
	}

	@Test
	public void testSetExternalFile_textSubs() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../util/russian-cp1251.srt"));

		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setType(SUBRIP);
		sub1.setExternalFile(file_cp1251);
		assertThat(sub1.getExternalFileCharacterSet()).isEqualTo(CHARSET_WINDOWS_1251);

		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setType(ASS);
		sub2.setExternalFile(file_cp1251);
		assertThat(sub2.getExternalFileCharacterSet()).isEqualTo(CHARSET_WINDOWS_1251);
	}

	@Test
	public void testIsEmbedded() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../util/russian-cp1251.srt"));
		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setType(SUBRIP);
		sub1.setExternalFile(file_cp1251);
		assertThat(sub1.isEmbedded()).isFalse();
		assertThat(sub1.isExternal()).isTrue();

		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setType(SUBRIP);
		assertThat(sub2.isEmbedded()).isTrue();
		assertThat(sub2.isExternal()).isFalse();
	}
}
