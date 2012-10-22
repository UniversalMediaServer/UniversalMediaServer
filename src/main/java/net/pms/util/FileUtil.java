package net.pms.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.formats.v2.SubtitleType;
import static org.apache.commons.lang.StringUtils.*;
import static org.mozilla.universalchardet.Constants.*;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

	private static Map<File, File[]> cache;

	public static File isFileExists(String f, String ext) {
		return isFileExists(new File(f), ext);
	}

	public static String getExtension(String f) {
		int point = f.lastIndexOf(".");
		if (point == -1) {
			return null;
		}
		return f.substring(point + 1);
	}

	public static String getFileNameWithoutExtension(String f) {
		int point = f.lastIndexOf(".");
		if (point == -1) {
			point = f.length();
		}
		return f.substring(0, point);
	}

	public static File getFileNameWitNewExtension(File parent, File f, String ext) {
		File ff = isFileExists(new File(parent, f.getName()), ext);
		if (ff != null && ff.exists()) {
			return ff;
		}
		return null;
	}

	public static File getFileNameWitAddedExtension(File parent, File f, String ext) {
		File ff = new File(parent, f.getName() + ext);
		if (ff.exists()) {
			return ff;
		}
		return null;
	}

	public static File isFileExists(File f, String ext) {
		int point = f.getName().lastIndexOf(".");
		if (point == -1) {
			point = f.getName().length();
		}
		File lowerCasedFilename = new File(f.getParentFile(), f.getName().substring(0, point) + "." + ext.toLowerCase());
		if (lowerCasedFilename.exists()) {
			return lowerCasedFilename;
		}

		File upperCasedFilename = new File(f.getParentFile(), f.getName().substring(0, point) + "." + ext.toUpperCase());
		if (upperCasedFilename.exists()) {
			return upperCasedFilename;
		}
		return null;
	}

	// FIXME needs to be renamed e.g. doSubtitlesExist or doesSubtitleExist
	@Deprecated
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media) {
		return doesSubtitlesExists(file, media, true);
	}

	// FIXME needs to be renamed e.g. doSubtitlesExist or doesSubtitleExist
	@Deprecated
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
		boolean found = browseFolderForSubtitles(file.getParentFile(), file, media, usecache);
		String alternate = PMS.getConfiguration().getAlternateSubsFolder();

		if (isNotBlank(alternate)) { // https://code.google.com/p/ps3mediaserver/issues/detail?id=737#c5
			File subFolder = new File(alternate);
			if (!subFolder.isAbsolute()) {
				subFolder = new File(file.getParent() + "/" + alternate);
				try {
					subFolder = subFolder.getCanonicalFile();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}

			if (subFolder.exists()) {
				found = found || browseFolderForSubtitles(subFolder, file, media, usecache);
			}
		}

		return found;
	}

	private synchronized static boolean browseFolderForSubtitles(File subFolder, File file, DLNAMediaInfo media, boolean usecache) {
		boolean found = false;
		if (!usecache) {
			cache = null;
		}

		if (cache == null) {
			cache = new HashMap<File, File[]>();
		}

		File allSubs[] = cache.get(subFolder);
		if (allSubs == null) {
			allSubs = subFolder.listFiles();
			if (allSubs != null) {
				cache.put(subFolder, allSubs);
			}
		}

		String fileName = getFileNameWithoutExtension(file.getName()).toLowerCase();
		if (allSubs != null) {
			for (File f : allSubs) {
				if (f.isFile() && !f.isHidden()) {
					String fName = f.getName().toLowerCase();
					for (String ext : SubtitleType.getSupportedFileExtensions()) {
						if (fName.length() > ext.length() && fName.startsWith(fileName) && endsWithIgnoreCase(fName, "." + ext)) {
							int a = fileName.length();
							int b = fName.length() - ext.length() - 1;
							String code = "";
							if (a <= b) { // handling case with several dots: <video>..<extension>
								code = fName.substring(a, b);
							}

							if (code.startsWith(".")) {
								code = code.substring(1);
							}

							boolean exists = false;
							if (media != null) {
								for (DLNAMediaSubtitle sub : media.getSubtitleTracksList()) {
									if (f.equals(sub.getExternalFile())) {
										exists = true;
									} else if (equalsIgnoreCase(ext, "idx") && sub.getType() == SubtitleType.MICRODVD) { // sub+idx => VOBSUB
										sub.setType(SubtitleType.VOBSUB);
										exists = true;
									} else if (equalsIgnoreCase(ext, "sub") && sub.getType() == SubtitleType.VOBSUB) { // VOBSUB
										try {
											sub.setExternalFile(f);
										} catch (FileNotFoundException ex) {
											LOGGER.warn("Exception during external subtitles scan.", ex);
										}
										exists = true;
									}
								}
							}

							if (!exists) {
								DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
								sub.setId(100 + (media == null ? 0 : media.getSubtitleTracksList().size())); // fake id, not used
								if (code.length() == 0 || !Iso639.getCodeList().contains(code)) {
									sub.setLang(DLNAMediaSubtitle.UND);
									sub.setType(SubtitleType.valueOfFileExtension(ext));
									if (code.length() > 0) {
										sub.setFlavor(code);
										if (sub.getFlavor().contains("-")) {
											String flavorLang = sub.getFlavor().substring(0, sub.getFlavor().indexOf("-"));
											String flavorTitle = sub.getFlavor().substring(sub.getFlavor().indexOf("-") + 1);
											if (Iso639.getCodeList().contains(flavorLang)) {
												sub.setLang(flavorLang);
												sub.setFlavor(flavorTitle);
											}
										}
									}
								} else {
									sub.setLang(code);
									sub.setType(SubtitleType.valueOfFileExtension(ext));
								}

								try {
									sub.setExternalFile(f);
								} catch (FileNotFoundException ex) {
									LOGGER.warn("Exception during external subtitles scan.", ex);
								}

								found = true;
								if (media != null) {
									media.getSubtitleTracksList().add(sub);
								}
							}
						}
					}
				}
			}
		}

		return found;
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 * @param file File to detect charset/encoding
	 * @return file's charset {@link org.mozilla.universalchardet.Constants} or null
	 * if not detected
	 * @throws IOException
	 */
	public static String getFileCharset(File file) throws IOException {
		byte[] buf = new byte[4096];
		BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
		final UniversalDetector universalDetector = new UniversalDetector(null);

		int numberOfBytesRead;
		while ((numberOfBytesRead = bufferedInputStream.read(buf)) > 0 && !universalDetector.isDone()) {
			universalDetector.handleData(buf, 0, numberOfBytesRead);
		}

		universalDetector.dataEnd();
		String encoding = universalDetector.getDetectedCharset();

		if (encoding != null) {
			LOGGER.debug("Detected encoding for {} is {}.", file.getAbsolutePath(), encoding);
		} else {
			LOGGER.debug("No encoding detected for {}.", file.getAbsolutePath());
		}

		universalDetector.reset();

		return encoding;
	}

	/**
	 * Tests if file is UTF-8 encoded with or without BOM.
	 * @param file File to test
	 * @return true if file is UTF-8 encoded with or without BOM, false otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF8(File file) throws IOException {
		return isCharsetUTF8(getFileCharset(file));
	}

	/**
	 * Tests if charset is UTF-8 encoded with or without BOM.
	 * @param charset Charset to test
	 * @return true if charset is UTF-8 encoded with or without BOM, false otherwise.
	 */
	public static boolean isCharsetUTF8(String charset) {
		return equalsIgnoreCase(charset, CHARSET_UTF_8);
	}

	/**
	 * Tests if file is UTF-16 encoded LE or BE.
	 * @param file File to test
	 * @return true if file is UTF-16 encoded LE or BE, false otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF16(File file) throws IOException {
		return isCharsetUTF16(getFileCharset(file));
	}

	/**
	 * Tests if charset is UTF-16 encoded LE or BE.
	 * @param charset Charset to test
	 * @return true if charset is UTF-16 encoded LE or BE, false otherwise.
	 */
	public static boolean isCharsetUTF16(String charset) {
		return (equalsIgnoreCase(charset, CHARSET_UTF_16LE) || equalsIgnoreCase(charset, CHARSET_UTF_16BE));
	}

	/**
	 * Tests if charset is UTF-32 encoded LE or BE.
	 * @param charset Charset to test
	 * @return true if charset is UTF-32 encoded LE or BE, false otherwise.
	 */
	public static boolean isCharsetUTF32(String charset) {
		return (equalsIgnoreCase(charset, CHARSET_UTF_32LE) || equalsIgnoreCase(charset, CHARSET_UTF_32BE));
	}

	/**
	 * Converts UTF-16 inputFile to UTF-8 outputFile. Does not overwrite existing outputFile file.
	 * @param inputFile UTF-16 file
	 * @param outputFile UTF-8 file after conversion
	 * @throws IOException
	 */
	public static void convertFileFromUtf16ToUtf8(File inputFile, File outputFile) throws IOException {
		String charset;
		if (inputFile == null || !inputFile.canRead()) {
			throw new FileNotFoundException("Can't read inputFile.");
		}

		try {
			charset = getFileCharset(inputFile);
		} catch (IOException ex) {
			LOGGER.debug("Exception during charset detection.", ex);
			throw new IllegalArgumentException("Can't confirm inputFile is UTF-16.");
		}

		if (isCharsetUTF16(charset)) {
			if (!outputFile.exists()) {
				BufferedReader reader = null;
				try {
					if (equalsIgnoreCase(charset, CHARSET_UTF_16LE)) {
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-16"));
					} else {
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-16BE"));
					}
				} catch (UnsupportedEncodingException ex) {
					LOGGER.warn("Unsupported exception.", ex);
					throw ex;
				}

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
				int c;
				while ((c = reader.read()) != -1) {
					writer.write(c);
				}

				writer.close();
				reader.close();
			}
		} else {
			throw new IllegalArgumentException("File is not UTF-16");
		}
	}
}
