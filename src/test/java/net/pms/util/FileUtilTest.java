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

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mozilla.universalchardet.Constants;

import java.io.File;
import java.io.FileNotFoundException;

import static org.fest.assertions.Assertions.assertThat;

public class FileUtilTest {
	private final Class<?> CLASS = FileUtilTest.class;

	@Test
	public void testGetFileCharset_WINDOWS_1251() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_WINDOWS_1251);
	}

	@Test
	public void testGetFileCharset_IBM866() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-ibm866.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_IBM866);
	}

	@Test
	public void testGetFileCharset_KOI8_R() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-koi8-r.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_KOI8_R);
	}

	@Test
	public void testGetFileCharset_UTF8_without_BOM() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF8_with_BOM() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF16_LE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_16LE);
	}

	@Test
	public void testGetFileCharset_UTF16_BE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_16BE);
	}

	@Test
	public void testGetFileCharset_UTF32_LE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf32-le.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_32LE);
	}

	@Test
	public void testGetFileCharset_UTF32_BE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf32-be.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_UTF_32BE);
	}

	@Test
	public void testGetFileCharset_BIG5() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_GB18030);
	}

	@Test
	public void testGetFileCharset_GB2312() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		assertThat(FileUtil.getFileCharset(file)).isEqualTo(Constants.CHARSET_BIG5);
	}

	@Test
	public void testIsFileUTF8() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		assertThat(FileUtil.isFileUTF8(file_utf8)).isTrue();
		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		assertThat(FileUtil.isFileUTF8(file_utf8_2)).isTrue();
		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt"));
		assertThat(FileUtil.isFileUTF8(file_utf8_3)).isTrue();
		File file_utf_16 = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		assertThat(FileUtil.isFileUTF8(file_utf_16)).isFalse();
		File file_utf_16_2 = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		assertThat(FileUtil.isFileUTF8(file_utf_16_2)).isFalse();
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		assertThat(FileUtil.isFileUTF8(file_cp1251)).isFalse();
		File file_ch = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		assertThat(FileUtil.isFileUTF8(file_ch)).isFalse();
		File file_ch_2 = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		assertThat(FileUtil.isFileUTF8(file_ch_2)).isFalse();
	}

	@Test
	public void testIsCharsetUTF8() throws Exception {
		assertThat(FileUtil.isCharsetUTF8("UTF-8")).isTrue();
		assertThat(FileUtil.isCharsetUTF8("uTf-8")).isTrue();
		assertThat(FileUtil.isCharsetUTF8("uTf-88")).isFalse();
	}

	@Test
	public void testIsCharsetUTF18_withNullOrEmptyCharset() throws Exception {
		assertThat(FileUtil.isCharsetUTF8(null)).isFalse();
		assertThat(FileUtil.isCharsetUTF8("")).isFalse();
	}

	@Test
	public void testIsFileUTF16() throws Exception {
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		assertThat(FileUtil.isFileUTF16(file_utf8)).isFalse();
		File file_utf8_2 = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		assertThat(FileUtil.isFileUTF16(file_utf8_2)).isFalse();
		File file_utf8_3 = FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt"));
		assertThat(FileUtil.isFileUTF16(file_utf8_3)).isFalse();
		File file_utf_16 = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		assertThat(FileUtil.isFileUTF16(file_utf_16)).isTrue();
		File file_utf_16_2 = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		assertThat(FileUtil.isFileUTF16(file_utf_16_2)).isTrue();
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		assertThat(FileUtil.isFileUTF16(file_cp1251)).isFalse();
		File file_ch = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		assertThat(FileUtil.isFileUTF16(file_ch)).isFalse();
		File file_ch_2 = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		assertThat(FileUtil.isFileUTF16(file_ch_2)).isFalse();
	}

	@Test
	public void testIsCharsetUTF16() throws Exception {
		assertThat(FileUtil.isCharsetUTF16("UTF-8")).isFalse();
		assertThat(FileUtil.isCharsetUTF16("UTF-16BE")).isTrue();
		assertThat(FileUtil.isCharsetUTF16("UTF-16LE")).isTrue();
		assertThat(FileUtil.isCharsetUTF16("utF-16le")).isTrue();
		assertThat(FileUtil.isCharsetUTF16(" utF-16le")).isFalse();
	}

	@Test
	public void testIsCharsetUTF16_withNullOrEmptyCharset() throws Exception {
		assertThat(FileUtil.isCharsetUTF16(null)).isFalse();
		assertThat(FileUtil.isCharsetUTF16("")).isFalse();
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_inputFileIsUTF16LE() throws Exception {
		File file_utf8le = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		File outputFile = new File(file_utf8le.getParentFile(), "output-utf8-from-utf16-le.srt");
		outputFile.delete();
		FileUtil.convertFileFromUtf16ToUtf8(file_utf8le, outputFile);
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		assertThat(FileUtils.contentEquals(outputFile, file_utf8)).isTrue();
		outputFile.delete();
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_inputFileIsUTF16BE() throws Exception {
		File file_utf8be = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		File outputFile = new File(file_utf8be.getParentFile(), "output-utf8-from-utf16-be.srt");
		outputFile.delete();
		FileUtil.convertFileFromUtf16ToUtf8(file_utf8be, outputFile);
		File file_utf8 = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		assertThat(FileUtils.contentEquals(outputFile, file_utf8)).isTrue();
		outputFile.delete();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFileFromUtf16ToUtf8_notUtf16InputFile() throws Exception {
		File file_cp1251 = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		FileUtil.convertFileFromUtf16ToUtf8(file_cp1251, new File("output.srt"));
	}

	@Test(expected = FileNotFoundException.class)
	public void testConvertFileFromUtf16ToUtf8_inputFileNotFound() throws Exception {
		FileUtil.convertFileFromUtf16ToUtf8(new File("no-such-file.xyz"), new File("output.srt"));
	}
}
