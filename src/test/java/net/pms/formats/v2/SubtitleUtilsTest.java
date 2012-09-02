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
package net.pms.formats.v2;

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import net.pms.dlna.DLNAMediaSubtitle;
import static net.pms.formats.v2.SubtitleType.VOBSUB;
import static net.pms.formats.v2.SubtitleUtils.getSubCpOptionForMencoder;
import org.apache.commons.io.FileUtils;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class SubtitleUtilsTest {
	private final Class<?> CLASS = SubtitleUtilsTest.class;

	/**
	 * Set up testing conditions before running the tests.
	 */
	@Before
	public final void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	@Test(expected = NullPointerException.class)
	public void testGetSubCpOptionForMencoder_withNullDLNAMediaSubtitle() throws Exception {
		getSubCpOptionForMencoder(null);
	}

	@Test
	public void testGetSubCpOptionForMencoder_withoutExternalSubtitles() throws Exception {
		DLNAMediaSubtitle subtitle = new DLNAMediaSubtitle();
		assertThat(getSubCpOptionForMencoder(subtitle)).isNull();
	}

	@Test
	public void testGetSubCpOptionForMencoder_withoutDetectedCharset() throws Exception {
		DLNAMediaSubtitle subtitle = new DLNAMediaSubtitle();
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../../util/russian-cp1251.srt"));
		subtitle.setType(VOBSUB);
		subtitle.setExternalFile(file_cp1251);
		assertThat(subtitle.getExternalFileCharacterSet()).isNull();
		assertThat(getSubCpOptionForMencoder(subtitle)).isNull();
	}

	@Test
	public void testGetSubCpOptionForMencoder() throws Exception {
		File file_big5 = FileUtils.toFile(CLASS.getResource("../../util/chinese-big5.srt"));
		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setExternalFile(file_big5);
		assertThat(getSubCpOptionForMencoder(sub1)).isEqualTo("enca:zh:big5");

		File file_gb18030 = FileUtils.toFile(CLASS.getResource("../../util/chinese-gb18030.srt"));
		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setExternalFile(file_gb18030);
		assertThat(getSubCpOptionForMencoder(sub2)).isEqualTo("enca:zh:big5");

		File file_cp1251 = FileUtils.toFile(CLASS.getResource("../../util/russian-cp1251.srt"));
		DLNAMediaSubtitle sub3 = new DLNAMediaSubtitle();
		sub3.setExternalFile(file_cp1251);
		assertThat(getSubCpOptionForMencoder(sub3)).isEqualTo("enca:ru:cp1251");

		File file_ibm866 = FileUtils.toFile(CLASS.getResource("../../util/russian-ibm866.srt"));
		DLNAMediaSubtitle sub4 = new DLNAMediaSubtitle();
		sub4.setExternalFile(file_ibm866);
		assertThat(getSubCpOptionForMencoder(sub4)).isEqualTo("enca:ru:cp1251");

		File file_koi8_r = FileUtils.toFile(CLASS.getResource("../../util/russian-koi8-r.srt"));
		DLNAMediaSubtitle sub5 = new DLNAMediaSubtitle();
		sub5.setExternalFile(file_koi8_r);
		assertThat(getSubCpOptionForMencoder(sub5)).isEqualTo("enca:ru:cp1251");
	}

	@Test
	public void testGetSubCpOptionForMencoder_UTF() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("../../util/russian-utf8-without-bom.srt"));
		DLNAMediaSubtitle sub1 = new DLNAMediaSubtitle();
		sub1.setExternalFile(file_utf8);
		assertThat(getSubCpOptionForMencoder(sub1)).isNull();

		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("../../util/russian-utf8-with-bom.srt"));
		DLNAMediaSubtitle sub2 = new DLNAMediaSubtitle();
		sub2.setExternalFile(file_utf8_2);
		assertThat(getSubCpOptionForMencoder(sub2)).isNull();

		File file_utf16_le = FileUtils.toFile(CLASS.getResource("../../util/russian-utf16-le.srt"));
		DLNAMediaSubtitle sub3 = new DLNAMediaSubtitle();
		sub3.setExternalFile(file_utf16_le);
		assertThat(getSubCpOptionForMencoder(sub3)).isNull();

		File file_utf16_be = FileUtils.toFile(CLASS.getResource("../../util/russian-utf16-be.srt"));
		DLNAMediaSubtitle sub4 = new DLNAMediaSubtitle();
		sub4.setExternalFile(file_utf16_be);
		assertThat(getSubCpOptionForMencoder(sub4)).isNull();

		File file_utf32_le = FileUtils.toFile(CLASS.getResource("../../util/russian-utf32-le.srt"));
		DLNAMediaSubtitle sub5 = new DLNAMediaSubtitle();
		sub5.setExternalFile(file_utf32_le);
		assertThat(getSubCpOptionForMencoder(sub5)).isNull();

		File file_utf32_be = FileUtils.toFile(CLASS.getResource("../../util/russian-utf32-be.srt"));
		DLNAMediaSubtitle sub6 = new DLNAMediaSubtitle();
		sub6.setExternalFile(file_utf32_be);
		assertThat(getSubCpOptionForMencoder(sub6)).isNull();

		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("../../util/english-utf8-with-bom.srt"));
		DLNAMediaSubtitle sub7 = new DLNAMediaSubtitle();
		sub7.setExternalFile(file_utf8_3);
		assertThat(getSubCpOptionForMencoder(sub7)).isNull();
	}
}
