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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import static net.pms.util.Constants.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.*;
import org.assertj.core.api.Fail;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilTest {
	private final Class<?> CLASS = FileUtilTest.class;

	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@Before
	public final void setUp() throws ConfigurationException {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);;
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

	@Test
	public void testIsUrl() throws Exception {
		assertThat(FileUtil.isUrl("universalmediaserver.com")).isFalse();
		assertThat(FileUtil.isUrl("http://www.universalmediaserver.com")).isTrue();
	}

	@Test
	public void testGetProtocol() throws Exception {
		assertThat(FileUtil.getProtocol("universalmediaserver.com")).isNull();
		assertThat(FileUtil.getProtocol("http://www.universalmediaserver.com")).isEqualTo("http");
	}

	@Test
	public void testUrlJoin() throws Exception {
		assertThat(FileUtil.urlJoin("", "http://www.universalmediaserver.com")).isEqualTo("http://www.universalmediaserver.com");
		assertThat(FileUtil.urlJoin("http://www.universalmediaserver.com", "index.php")).isEqualTo("http://www.universalmediaserver.com/index.php");
	}

	@Test
	public void testGetUrlExtension() throws Exception {
		assertThat(FileUtil.getUrlExtension("filename")).isNull();
		assertThat(FileUtil.getUrlExtension("http://www.universalmediaserver.com/file.html?foo=bar")).isEqualTo("html");
	}

	@Test
	public void testGetFileNameWithoutExtension() throws Exception {
		assertThat(FileUtil.getFileNameWithoutExtension("filename.mkv")).isEqualTo("filename");
	}

	/**
	 * Note: The method this is testing handles numerous inputs, so this test
	 * could get very large. It should get much larger than it is now.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testGetFileNameWithRewriting() throws Exception {
		JsonParser parser = new JsonParser();
		
		try {
			JsonElement tree = parser.parse(
					new java.io.FileReader(
							FileUtils.toFile(
									CLASS.getResource("prettified_filenames_metadata.json")
									)
							)
					);

	        JsonArray tests = tree.getAsJsonArray();
	        for( JsonElement test: tests ) {
	        	JsonObject o = test.getAsJsonObject();
	        	String original = o.get("filename").getAsString();
	        	String prettified = o.get("prettified").getAsString();
	        	assertThat(FileUtil.getFileNamePrettified(original)).isEqualTo(prettified);
	        }
		}
		catch( Exception ex ) {
			throw(new AssertionError(ex));
		}
	}		


	@Test
	public void testGetFileCharset_WINDOWS_1251() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-cp1251.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_WINDOWS_1251);
	}

	@Test
	public void testGetFileCharset_KOI8_R() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-koi8-r.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_KOI8_R);
	}

	@Test
	public void testGetFileCharset_UTF8_without_BOM() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf8-without-bom.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF8_with_BOM() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf8-with-bom.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF16_LE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf16-le.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_16LE);
	}

	@Test
	public void testGetFileCharset_UTF16_BE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf16-be.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_16BE);
	}

	@Test
	public void testGetFileCharset_UTF32_LE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf32-le.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_32LE);
	}

	@Test
	public void testGetFileCharset_UTF32_BE() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("russian-utf32-be.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_UTF_32BE);
	}

	@Test
	public void testGetFileCharset_BIG5() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("chinese-gb18030.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_GB18030);
	}

	@Test
	public void testGetFileCharset_GB2312() throws Exception {
		File file = FileUtils.toFile(CLASS.getResource("chinese-big5.srt"));
		assertThat(FileUtil.getFileCharsetName(file)).isEqualTo(CHARSET_BIG5);
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
		String s = null;
		assertThat(FileUtil.isCharsetUTF8(s)).isFalse();
		Charset c = null;
		assertThat(FileUtil.isCharsetUTF8(c)).isFalse();
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
		String s = null;
		assertThat(FileUtil.isCharsetUTF16(s)).isFalse();
		Charset c = null;
		assertThat(FileUtil.isCharsetUTF16(c)).isFalse();
		assertThat(FileUtil.isCharsetUTF16("")).isFalse();
	}

	@Test
	public void testIsCharsetUTF32() throws Exception {
		assertThat(FileUtil.isCharsetUTF32("UTF-8")).isFalse();
		assertThat(FileUtil.isCharsetUTF32("UTF-16BE")).isFalse();
		assertThat(FileUtil.isCharsetUTF32("UTF-32BE")).isTrue();
		assertThat(FileUtil.isCharsetUTF32("UTF-32BE")).isTrue();
		assertThat(FileUtil.isCharsetUTF32("utF-32Be")).isTrue();
		assertThat(FileUtil.isCharsetUTF32("utF-332Be")).isFalse();
	}

	@Test
	public void testIsCharsetUTF32_withNullOrEmptyCharset() throws Exception {
		assertThat(FileUtil.isCharsetUTF32(null)).isFalse();
		assertThat(FileUtil.isCharsetUTF32("")).isFalse();
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

	@Test
	public void testGetFilePermissions() throws FileNotFoundException {
		File file = null;
		String path = null;
		try {
			FileUtil.getFilePermissions(file);
			Fail.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
		try {
			FileUtil.getFilePermissions(path);
			Fail.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
		assertNull("NullIsNull", FileUtil.getFilePermissionsNoThrow(file));
		assertNull("NullIsNull", FileUtil.getFilePermissionsNoThrow(path));
		assertTrue("CurrentFolderIsFolder", FileUtil.getFilePermissions(new File("")).isFolder());
		assertTrue("CurrentFolderIsReadable", FileUtil.getFilePermissions(new File("")).isReadable());
		assertTrue("CurrentFolderIsBrowsable", FileUtil.getFilePermissions(new File("")).isBrowsable());
		assertTrue("user.dirFolderIsFolder", FileUtil.getFilePermissions(new File(System.getProperty("user.dir"))).isFolder());
		try {
			FileUtil.getFilePermissions("No such file");
			Fail.fail("Expected FileNotFoundException");
		} catch (FileNotFoundException e) {
			// As expected
		}
		assertNull("NoSuchFileIsNull", FileUtil.getFilePermissionsNoThrow("No such file"));

		file = FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt"));
		assertTrue("FileIsReadable", FileUtil.getFilePermissions(file).isReadable());
		assertTrue("FileIsWritable", FileUtil.getFilePermissions(file).isWritable());
		assertFalse("FileIsNotFolder", FileUtil.getFilePermissions(file).isFolder());
		assertFalse("FileIsNotBrowsable", FileUtil.getFilePermissions(file).isBrowsable());
		assertTrue("ParentIsFolder", FileUtil.getFilePermissions(file.getParentFile()).isFolder());
		assertTrue("ParentIsBrowsable", FileUtil.getFilePermissions(file.getParentFile()).isBrowsable());
		try {
			FileUtil.getFilePermissions(new File(file.getParentFile(),"No such file"));
			Fail.fail("Expected FileNotFoundException");
		} catch (FileNotFoundException e) {
			// As expected
		}
		assertNull("NoSuchFileIsNull", FileUtil.getFilePermissionsNoThrow(new File(file.getParentFile(),"No such file")));

		path = String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000));
		file = new File(System.getProperty("java.io.tmpdir"), path);
		try {
			if (file.createNewFile()) {
				try {
					assertTrue("TempFileIsReadable", FileUtil.getFilePermissions(file).isReadable());
					assertTrue("TempFileIsWritable", FileUtil.getFilePermissions(file).isWritable());
					assertFalse("TempFileIsNotFolder", FileUtil.getFilePermissions(file).isFolder());
					assertFalse("TempFileIsNotBrowsable", FileUtil.getFilePermissions(file).isBrowsable());
				} finally {
					file.delete();
				}
			}
		} catch (IOException e) {
			// Move on
		}
	}

	@Test
	public void testIsValidFileName() {
		assertFalse("ColonIsInvalid", FileUtil.isValidFileName("debug:log"));
		assertFalse("SlashIsInvalid", FileUtil.isValidFileName("foo/bar"));
		assertTrue("debug.logIsValid", FileUtil.isValidFileName("debug.log"));
	}

	@Test
	public void testAppendPathSeparator() {
		assertEquals("AppendEmptyString", File.separator, FileUtil.appendPathSeparator(""));
		assertEquals("AppendSlash", "/", FileUtil.appendPathSeparator("/"));
		assertEquals("AppendMissingBackslash", "foo\\bar\\", FileUtil.appendPathSeparator("foo\\bar"));
		assertEquals("DontAppendBackslash", "foo\\bar\\", FileUtil.appendPathSeparator("foo\\bar\\"));
		assertEquals("AppendMissingSlash", "foo/bar/", FileUtil.appendPathSeparator("foo/bar"));
		assertEquals("DontAppendSlash", "foo/bar/", FileUtil.appendPathSeparator("foo/bar/"));
	}
}
