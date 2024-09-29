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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.subtitle.MediaSubtitleTest;
import static net.pms.util.Constants.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class FileUtilTest {

	private final Class<?> CLASS = FileUtilTest.class;
	private final Class<?> SUBTITLE_CLASS = MediaSubtitleTest.class;

	@BeforeEach
	public void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.SetLoggingWarn();
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
		} catch (InterruptedException | ConfigurationException ex) {
			throw new AssertionError(ex);
		}
	}

	@Test
	public void testIsUrl() throws Exception {
		assertFalse(FileUtil.isUrl("universalmediaserver.com"));
		assertTrue(FileUtil.isUrl("http://www.universalmediaserver.com"));
	}

	@Test
	public void testGetProtocol() throws Exception {
		assertNull(FileUtil.getProtocol("universalmediaserver.com"));
		assertEquals(FileUtil.getProtocol("http://www.universalmediaserver.com"), "http");
	}

	@Test
	public void testUrlJoin() throws Exception {
		assertEquals(FileUtil.urlJoin("", "http://www.universalmediaserver.com"), "http://www.universalmediaserver.com");
		assertEquals(FileUtil.urlJoin("http://www.universalmediaserver.com", "index.php"), "http://www.universalmediaserver.com/index.php");
	}

	@Test
	public void testGetUrlExtension() throws Exception {
		assertNull(FileUtil.getUrlExtension("filename"));
		assertEquals(FileUtil.getUrlExtension("http://www.universalmediaserver.com/file.html?foo=bar"), "html");
	}

	@Test
	public void testGetFileNameWithoutExtension() throws Exception {
		assertEquals(FileUtil.getFileNameWithoutExtension("filename.mkv"), "filename");
	}

	/**
	 * Note: The method this is testing handles numerous inputs, so this test
	 * could get very large. It should get much larger than it is now.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testGetFileNameWithRewriting() throws Exception {
		try {
			JsonElement tree = JsonParser.parseReader(
					new java.io.FileReader(
							FileUtils.toFile(
									CLASS.getResource("prettified_filenames_metadata.json")
							)
					)
			);

			JsonArray tests = tree.getAsJsonArray();
			for (JsonElement test : tests) {
				JsonObject o = test.getAsJsonObject();
				String original = o.get("filename").getAsString();
				String absolutePath = null;
				if (o.get("absolutepath") != null) {
					absolutePath = o.get("absolutepath").getAsString();
				}
				String expectedOutput = o.get("prettified").getAsString();
				String fileNamePrettified = FileUtil.getFileNamePrettified(original, absolutePath);
				assertEquals(fileNamePrettified, expectedOutput, o.get("comment").getAsString());
			}
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException ex) {
			throw (new AssertionError(ex));
		}
	}

	/**
	 * Note: The method this is testing handles numerous inputs, so this test
	 * could get very large. It should get much larger than it is now.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testGetFileNameMetadata() throws Exception {
		Logger logger = TestHelper.getRootLogger();
		JsonElement tree = JsonParser.parseReader(
				new java.io.FileReader(
						FileUtils.toFile(
								CLASS.getResource("prettified_filenames_metadata.json")
						)
				)
		);

		JsonArray tests = tree.getAsJsonArray();
		for (JsonElement test : tests) {
			JsonObject o = test.getAsJsonObject();
			String original = o.get("filename").getAsString();
			String absolutePath = null;
			if (o.get("absolutepath") != null) {
				absolutePath = o.get("absolutepath").getAsString();
			}
			JsonObject metadata = o.get("metadata").getAsJsonObject();
			boolean todo = false;
			if (o.has("todo")) {
				todo = o.get("todo").getAsBoolean();
			}

			FileNameMetadata extracted_metadata = FileUtil.getFileNameMetadata(original, absolutePath);
			String movieOrShowName = extracted_metadata.getMovieOrShowName();
			int year = -1;
			try {
				if (extracted_metadata.getYear() != null) {
					year = extracted_metadata.getYear();
				}
			} catch (NumberFormatException ex) {
				throw (new AssertionError(ex));
			}
			String extraInformation = extracted_metadata.getExtraInformation();
			int tvSeason = -1;
			try {
				if (extracted_metadata.getTvSeasonNumber() != null) {
					tvSeason = extracted_metadata.getTvSeasonNumber();
				}
			} catch (NumberFormatException ex) {
				throw (new AssertionError(ex));
			}
			// tvEpisodeNumber might be a single episode, but might also be
			// a hyphen-separated range, so cannot always parse as int
			String tvEpisodeNumber = extracted_metadata.getTvEpisodeNumber();
			String tvEpisodeName = extracted_metadata.getTvEpisodeName();

			JsonElement elem;

			elem = metadata.get("extra");
			if (elem != null) {
				for (JsonElement extra : elem.getAsJsonArray()) {
					try {
						assertTrue(extraInformation.contains(extra.getAsString()));
					} catch (NullPointerException ex) {
						// There is no extraInformation extracted
						if (todo) {
							logger.warn("testGetFileNameMetadata/extra would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						// extraInformation is extracted, but is wrong
						if (todo) {
							logger.warn("testGetFileNameMetadata/extra would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
			}
			if ("tv-series-episode".equals(metadata.get("type").getAsString())) {
				logger.debug("Doing tv-series-episode " + original);
				// A single episode, might have episode title and date
				elem = metadata.get("series");
				if (elem != null) {
					try {
						assertEquals(movieOrShowName, elem.getAsString());
					} catch (NullPointerException ex) {
						// There is no movieOrShowName extracted
						if (todo) {
							logger.warn("testGetFileNameMetadata/series would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						// movieOrShowName is extracted, but is wrong
						if (todo) {
							logger.warn("testGetFileNameMetadata/series would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
				elem = metadata.get("season");
				if (elem != null) {
					try {
						assertEquals(tvSeason, elem.getAsInt());
					} catch (NullPointerException ex) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/season would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/season would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
				elem = metadata.get("episode");
				if (elem != null) {
					try {
						assertEquals(tvEpisodeNumber, String.format("%02d", elem.getAsInt()));
					} catch (NumberFormatException | NullPointerException ex) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/episode would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/episode would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
				if (metadata.has("released")) {
					JsonObject metadata_rel = metadata.get("released").getAsJsonObject();
					String rel_date = Integer.toString(metadata_rel.get("year").getAsInt());
					if (metadata_rel.has("month")) {
						rel_date = rel_date + "-" + Integer.toString(metadata_rel.get("month").getAsInt());
					}
					if (metadata_rel.has("date")) {
						rel_date = rel_date + "-" + Integer.toString(metadata_rel.get("date").getAsInt());
					}
				}
				elem = metadata.get("title");
				if (elem != null) {
					try {
						assertEquals(tvEpisodeName, elem.getAsString());
					} catch (NullPointerException ex) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/title would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/title would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
			} else if ("tv-series-episodes".equals(metadata.get("type").getAsString())) {
				logger.debug("Doing tv-series-episodes " + original);
				// A single episode or an episode range, cannot have episode title or date
				elem = metadata.get("series");
				if (elem != null) {
					try {
						assertEquals(movieOrShowName, elem.getAsString());
					} catch (NullPointerException ex) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/series would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						if (todo) {
							logger.warn("testGetFileNameMetadata/series would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
				elem = metadata.get("season");
				if (elem != null) {
					assertEquals(tvSeason, elem.getAsInt());
				}
				elem = metadata.get("episodes");
				if (elem != null) {
					String range = "";
					for (JsonElement elem2 : elem.getAsJsonArray()) {
						String paddedString = elem2.getAsString();
						if (paddedString.length() == 1) {
							paddedString = "0" + paddedString;
						}
						range = range + "-" + paddedString;
					}
					if (isNotBlank(range)) {
						try {
							assertEquals(tvEpisodeNumber, range.substring(1));
						} catch (AssertionError err) {
							if (todo) {
								logger.warn("testGetFileNameMetadata/episodes would fail for TODO test " + original);
							} else {
								throw err;
							}
						}
					}
				}
			} else if ("movie".equals(metadata.get("type").getAsString())) {
				logger.debug("Doing movie " + original);
				elem = metadata.get("title");
				if (elem != null) {
					try {
						assertEquals(elem.getAsString(), movieOrShowName);
					} catch (NullPointerException ex) {
						// There is no movieOrShowName extracted
						if (todo) {
							logger.warn("testGetFileNameMetadata/title would fail for TODO test " + original);
						} else {
							throw (new AssertionError(ex));
						}
					} catch (AssertionError err) {
						// movieOrShowName is extracted, but is wrong
						if (todo) {
							logger.warn("testGetFileNameMetadata/title would fail for TODO test " + original);
						} else {
							throw (err);
						}
					}
				}
				if (metadata.has("released")) {
					JsonObject metadata_rel = metadata.get("released").getAsJsonObject();
					elem = metadata_rel.get("year");
					if (elem != null) {
						try {
							assertEquals(year, elem.getAsInt());
						} catch (AssertionError err) {
							if (todo) {
								logger.warn("testGetFileNameMetadata/released would fail for TODO test " + original);
							} else {
								throw (err);
							}
						}
					}
				}
			} else if ("sport".equals(metadata.get("type").getAsString())) {
				// We do not do anything with sport videos at the moment, so here we make sure it does NOT match
				logger.debug("Doing sport " + original);
				if (movieOrShowName != null) {
					throw (new AssertionError("Sport videos should not match: " + metadata));
				}
			} else {
				logger.error("Unknown content type in " + original);
			}
		}
	}

	@Test
	public void testGetFileCharset_WINDOWS_1251() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-cp1251.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_WINDOWS_1251);
	}

	@Test
	public void testGetFileCharset_KOI8_R() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-koi8-r.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_KOI8_R);
	}

	@Test
	public void testGetFileCharset_UTF8_without_BOM() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-without-bom.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF8_with_BOM() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-with-bom.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_8);
	}

	@Test
	public void testGetFileCharset_UTF16_LE() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-le.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_16LE);
	}

	@Test
	public void testGetFileCharset_UTF16_BE() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-be.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_16BE);
	}

	@Test
	public void testGetFileCharset_UTF32_LE() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf32-le.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_32LE);
	}

	@Test
	public void testGetFileCharset_UTF32_BE() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf32-be.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_UTF_32BE);
	}

	@Test
	public void testGetFileCharset_BIG5() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-gb18030.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_GB18030);
	}

	@Test
	public void testGetFileCharset_GB2312() throws Exception {
		File file = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-big5.srt"));
		assertEquals(FileUtil.getFileCharsetName(file), CHARSET_BIG5);
	}

	@Test
	public void testIsFileUTF8() throws Exception {
		File file_utf8 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-without-bom.srt"));
		assertTrue(FileUtil.isFileUTF8(file_utf8));
		File file_utf8_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-with-bom.srt"));
		assertTrue(FileUtil.isFileUTF8(file_utf8_2));
		File file_utf8_3 = FileUtils.toFile(SUBTITLE_CLASS.getResource("english-utf8-with-bom.srt"));
		assertTrue(FileUtil.isFileUTF8(file_utf8_3));
		File file_utf_16 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-le.srt"));
		assertFalse(FileUtil.isFileUTF8(file_utf_16));
		File file_utf_16_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-be.srt"));
		assertFalse(FileUtil.isFileUTF8(file_utf_16_2));
		File file_cp1251 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-cp1251.srt"));
		assertFalse(FileUtil.isFileUTF8(file_cp1251));
		File file_ch = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-gb18030.srt"));
		assertFalse(FileUtil.isFileUTF8(file_ch));
		File file_ch_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-big5.srt"));
		assertFalse(FileUtil.isFileUTF8(file_ch_2));
	}

	@Test
	public void testIsCharsetUTF8() throws Exception {
		assertTrue(FileUtil.isCharsetUTF8(StandardCharsets.UTF_8));
		assertTrue(FileUtil.isCharsetUTF8("uTf-8"));
		assertFalse(FileUtil.isCharsetUTF8("uTf-88"));
	}

	@Test
	public void testIsCharsetUTF18_withNullOrEmptyCharset() throws Exception {
		String s = null;
		assertFalse(FileUtil.isCharsetUTF8(s));
		Charset c = null;
		assertFalse(FileUtil.isCharsetUTF8(c));
		assertFalse(FileUtil.isCharsetUTF8(""));
	}

	@Test
	public void testIsFileUTF16() throws Exception {
		File file_utf8 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-without-bom.srt"));
		assertFalse(FileUtil.isFileUTF16(file_utf8));
		File file_utf8_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-with-bom.srt"));
		assertFalse(FileUtil.isFileUTF16(file_utf8_2));
		File file_utf8_3 = FileUtils.toFile(SUBTITLE_CLASS.getResource("english-utf8-with-bom.srt"));
		assertFalse(FileUtil.isFileUTF16(file_utf8_3));
		File file_utf_16 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-le.srt"));
		assertTrue(FileUtil.isFileUTF16(file_utf_16));
		File file_utf_16_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-be.srt"));
		assertTrue(FileUtil.isFileUTF16(file_utf_16_2));
		File file_cp1251 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-cp1251.srt"));
		assertFalse(FileUtil.isFileUTF16(file_cp1251));
		File file_ch = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-gb18030.srt"));
		assertFalse(FileUtil.isFileUTF16(file_ch));
		File file_ch_2 = FileUtils.toFile(SUBTITLE_CLASS.getResource("chinese-big5.srt"));
		assertFalse(FileUtil.isFileUTF16(file_ch_2));
	}

	@Test
	public void testIsCharsetUTF16() throws Exception {
		assertFalse(FileUtil.isCharsetUTF16(StandardCharsets.UTF_8));
		assertTrue(FileUtil.isCharsetUTF16(StandardCharsets.UTF_16BE));
		assertTrue(FileUtil.isCharsetUTF16(StandardCharsets.UTF_16LE));
		assertTrue(FileUtil.isCharsetUTF16("utF-16le"));
		assertFalse(FileUtil.isCharsetUTF16(" utF-16le"));
	}

	@Test
	public void testIsCharsetUTF16_withNullOrEmptyCharset() throws Exception {
		String s = null;
		assertFalse(FileUtil.isCharsetUTF16(s));
		Charset c = null;
		assertFalse(FileUtil.isCharsetUTF16(c));
		assertFalse(FileUtil.isCharsetUTF16(""));
	}

	@Test
	public void testIsCharsetUTF32() throws Exception {
		assertFalse(FileUtil.isCharsetUTF32("UTF-8"));
		assertFalse(FileUtil.isCharsetUTF32("UTF-16BE"));
		assertTrue(FileUtil.isCharsetUTF32("UTF-32BE"));
		assertTrue(FileUtil.isCharsetUTF32("UTF-32BE"));
		assertTrue(FileUtil.isCharsetUTF32("utF-32Be"));
		assertFalse(FileUtil.isCharsetUTF32("utF-332Be"));
	}

	@Test
	public void testIsCharsetUTF32_withNullOrEmptyCharset() throws Exception {
		assertFalse(FileUtil.isCharsetUTF32(null));
		assertFalse(FileUtil.isCharsetUTF32(""));
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_inputFileIsUTF16LE() throws Exception {
		File file_utf8le = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-le.srt"));
		File outputFile = new File(file_utf8le.getParentFile(), "output-utf8-from-utf16-le.srt");
		outputFile.delete();
		FileUtil.convertFileFromUtf16ToUtf8(file_utf8le, outputFile);
		File file_utf8 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-without-bom.srt"));
		assertTrue(FileUtils.contentEquals(outputFile, file_utf8));
		outputFile.delete();
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_inputFileIsUTF16BE() throws Exception {
		File file_utf8be = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf16-be.srt"));
		File outputFile = new File(file_utf8be.getParentFile(), "output-utf8-from-utf16-be.srt");
		outputFile.delete();
		FileUtil.convertFileFromUtf16ToUtf8(file_utf8be, outputFile);
		File file_utf8 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-utf8-with-bom.srt"));
		assertTrue(FileUtils.contentEquals(outputFile, file_utf8));
		outputFile.delete();
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_notUtf16InputFile() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			File file_cp1251 = FileUtils.toFile(SUBTITLE_CLASS.getResource("russian-cp1251.srt"));
			FileUtil.convertFileFromUtf16ToUtf8(file_cp1251, new File("output.srt"));
		});
	}

	@Test
	public void testConvertFileFromUtf16ToUtf8_inputFileNotFound() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			FileUtil.convertFileFromUtf16ToUtf8(new File("no-such-file.xyz"), new File("output.srt"));
		});
	}

	@Test
	public void testGetFilePermissions() throws FileNotFoundException {
		File file = null;
		String path = null;
		try {
			FileUtil.getFilePermissions(file);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
		try {
			FileUtil.getFilePermissions(path);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
		assertNull(FileUtil.getFilePermissionsNoThrow(file), "NullIsNull");
		assertNull(FileUtil.getFilePermissionsNoThrow(path), "NullIsNull");
		assertTrue(FileUtil.getFilePermissions(new File("")).isFolder(), "CurrentFolderIsFolder");
		assertTrue(FileUtil.getFilePermissions(new File("")).isReadable(), "CurrentFolderIsReadable");
		assertTrue(FileUtil.getFilePermissions(new File("")).isBrowsable(), "CurrentFolderIsBrowsable");
		assertTrue(FileUtil.getFilePermissions(new File(System.getProperty("user.dir"))).isFolder(), "user.dirFolderIsFolder");
		try {
			FileUtil.getFilePermissions("No such file");
			fail("Expected FileNotFoundException");
		} catch (FileNotFoundException e) {
			// As expected
		}
		assertNull(FileUtil.getFilePermissionsNoThrow("No such file"), "NoSuchFileIsNull");

		file = FileUtils.toFile(SUBTITLE_CLASS.getResource("english-utf8-with-bom.srt"));
		assertTrue(FileUtil.getFilePermissions(file).isReadable(), "FileIsReadable");
		assertTrue(FileUtil.getFilePermissions(file).isWritable(), "FileIsWritable");
		assertFalse(FileUtil.getFilePermissions(file).isFolder(), "FileIsNotFolder");
		assertFalse(FileUtil.getFilePermissions(file).isBrowsable(), "FileIsNotBrowsable");
		assertTrue(FileUtil.getFilePermissions(file.getParentFile()).isFolder(), "ParentIsFolder");
		assertTrue(FileUtil.getFilePermissions(file.getParentFile()).isBrowsable(), "ParentIsBrowsable");
		try {
			FileUtil.getFilePermissions(new File(file.getParentFile(), "No such file"));
			fail("Expected FileNotFoundException");
		} catch (FileNotFoundException e) {
			// As expected
		}
		assertNull(FileUtil.getFilePermissionsNoThrow(new File(file.getParentFile(), "No such file")), "NoSuchFileIsNull");

		path = String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000));
		file = new File(System.getProperty("java.io.tmpdir"), path);
		try {
			if (file.createNewFile()) {
				try {
					assertTrue(FileUtil.getFilePermissions(file).isReadable(), "TempFileIsReadable");
					assertTrue(FileUtil.getFilePermissions(file).isWritable(), "TempFileIsWritable");
					assertFalse(FileUtil.getFilePermissions(file).isFolder(), "TempFileIsNotFolder");
					assertFalse(FileUtil.getFilePermissions(file).isBrowsable(), "TempFileIsNotBrowsable");
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
		assertFalse(FileUtil.isValidFileName("debug:log"), "ColonIsInvalid");
		assertFalse(FileUtil.isValidFileName("foo/bar"), "SlashIsInvalid");
		assertTrue(FileUtil.isValidFileName("debug.log"), "debug.logIsValid");
	}

	@Test
	public void testAppendPathSeparator() {
		assertEquals(File.separator, FileUtil.appendPathSeparator(""), "AppendEmptyString");
		assertEquals("/", FileUtil.appendPathSeparator("/"), "AppendSlash");
		assertEquals("foo\\bar\\", FileUtil.appendPathSeparator("foo\\bar"), "AppendMissingBackslash");
		assertEquals("foo\\bar\\", FileUtil.appendPathSeparator("foo\\bar\\"), "DontAppendBackslash");
		assertEquals("foo/bar/", FileUtil.appendPathSeparator("foo/bar"), "AppendMissingSlash");
		assertEquals("foo/bar/", FileUtil.appendPathSeparator("foo/bar/"), "DontAppendSlash");
	}
}
