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
package net.pms.util;

import java.io.File;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;
import org.mozilla.universalchardet.Constants;

public class FileUtilTest {
	@Test
	public void testGetFileCharset_WINDOWS_1251() throws Exception {
		File file = new File(this.getClass().getResource("russian-cp1251.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_WINDOWS_1251);
	}

	@Test
	public void testGetFileCharset_IBM866() throws Exception {
		File file = new File(this.getClass().getResource("russian-ibm866.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_IBM866);
	}

	@Test
	public void testGetFileCharset_KOI8_R() throws Exception {
		File file = new File(this.getClass().getResource("russian-koi8-r.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_KOI8_R);
	}

	@Test
	public void testGetFileCharset_UTF8_without_BOM() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf8-without-bom.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF8_with_BOM() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf8-with-bom.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF16_LE() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf16-le.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_16LE);
	}

	@Test
	public void testGetFileCharset_UTF16_BE() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf16-be.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_16BE);
	}

	@Test
	public void testGetFileCharset_UTF32_LE() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf32-le.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_32LE);
	}

	@Test
	public void testGetFileCharset_UTF32_BE() throws Exception {
		File file = new File(this.getClass().getResource("russian-utf32-be.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_32BE);
	}

	@Test
	public void testGetFileCharset_BIG5() throws Exception {
		File file = new File(this.getClass().getResource("chinese-gb18030.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_GB18030);
	}

	@Test
	public void testGetFileCharset_GB2312() throws Exception {
		File file = new File(this.getClass().getResource("chinese-big5.srt").getFile());
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_BIG5);
	}

	@Test
	public void testIsFileUTF8() throws Exception {
		File file_utf8 = new File(this.getClass().getResource("russian-utf8-without-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_utf8)).isTrue();
		File file_utf8_2 = new File(this.getClass().getResource("russian-utf8-with-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_utf8_2)).isTrue();
		File file_utf8_3 = new File(this.getClass().getResource("english-utf8-with-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_utf8_3)).isTrue();
		File file_utf_16 = new File(this.getClass().getResource("russian-utf16-le.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_utf_16)).isFalse();
		File file_utf_16_2 = new File(this.getClass().getResource("russian-utf16-be.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_utf_16_2)).isFalse();
		File file_cp1251 = new File(this.getClass().getResource("russian-cp1251.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_cp1251)).isFalse();
		File file_ch = new File(this.getClass().getResource("chinese-gb18030.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_ch)).isFalse();
		File file_ch_2 = new File(this.getClass().getResource("chinese-big5.srt").getFile());
		assertThat(FileUtil.isFileUTF8(file_ch_2)).isFalse();
	}

	@Test
	public void testIsFileUTF16() throws Exception {
		File file_utf8 = new File(this.getClass().getResource("russian-utf8-without-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_utf8)).isFalse();
		File file_utf8_2 = new File(this.getClass().getResource("russian-utf8-with-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_utf8_2)).isFalse();
		File file_utf8_3 = new File(this.getClass().getResource("english-utf8-with-bom.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_utf8_3)).isFalse();
		File file_utf_16 = new File(this.getClass().getResource("russian-utf16-le.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_utf_16)).isTrue();
		File file_utf_16_2 = new File(this.getClass().getResource("russian-utf16-be.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_utf_16_2)).isTrue();
		File file_cp1251 = new File(this.getClass().getResource("russian-cp1251.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_cp1251)).isFalse();
		File file_ch = new File(this.getClass().getResource("chinese-gb18030.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_ch)).isFalse();
		File file_ch_2 = new File(this.getClass().getResource("chinese-big5.srt").getFile());
		assertThat(FileUtil.isFileUTF16(file_ch_2)).isFalse();
	}
}
