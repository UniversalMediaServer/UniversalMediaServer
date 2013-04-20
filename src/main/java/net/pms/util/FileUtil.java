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

	public static String getFileNameWithRewriting(String f) {
		String formattedName;
		int point = f.lastIndexOf(".");

		if (point == -1) {
			point = f.length();
		}

		// Remove file extension
		formattedName = f.substring(0, point);

		String commonFileEnds = "[\\s\\.]AC3.*|[\\s\\.]REPACK.*|[\\s\\.]480p.*|[\\s\\.]720p.*|[\\s\\.]m-720p.*|[\\s\\.]900p.*|[\\s\\.]1080p.*|[\\s\\.]HDTV.*|[\\s\\.]DSR.*|[\\s\\.]PDTV.*|[\\s\\.]WS.*|[\\s\\.]HQ.*|[\\s\\.]DVDRip.*|[\\s\\.]TVRiP.*|[\\s\\.]BDRip.*|[\\s\\.]BluRay.*|[\\s\\.]SUBBED.*|[\\s\\.]x264.*|[\\s\\.]Dual[\\s\\.]Audio.*|[\\s\\.]HSBS.*|[\\s\\.]H-SBS.*";
		String commonFileEndsMatch = ".*[\\s\\.]AC3.*|.*[\\s\\.]REPACK.*|.*[\\s\\.]480p.*|.*[\\s\\.]720p.*|.*[\\s\\.]m-720p.*|.*[\\s\\.]900p.*|.*[\\s\\.]1080p.*|.*[\\s\\.]HDTV.*|.*[\\s\\.]DSR.*|.*[\\s\\.]PDTV.*|.*[\\s\\.]WS.*|.*[\\s\\.]HQ.*|.*[\\s\\.]DVDRip.*|.*[\\s\\.]TVRiP.*|.*[\\s\\.]BDRip.*|.*[\\s\\.]BluRay.*|.*[\\s\\.]SUBBED.*|.*[\\s\\.]x264.*|.*[\\s\\.]Dual[\\s\\.]Audio.*|.*[\\s\\.]HSBS.*|.*[\\s\\.]H-SBS.*";
		String commonFileEndsCaseSensitive = "[\\s\\.]PROPER.*|[\\s\\.]iNTERNAL.*|[\\s\\.]LIMITED.*|[\\s\\.]FESTiVAL.*|[\\s\\.]NORDIC.*";

		if (formattedName.matches(".*[sS]0\\d[eE]\\d\\d.*")) {
			// This matches scene and most p2p TV episodes within the first 9 seasons

			// Rename the season/episode numbers. For example, "S01E01" changes to " - 101"
			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(?i)[\\s\\.]S0(\\d)E(\\d)(\\d)(" + commonFileEnds + ")", " - $1$2$3");

			// If it matches this then it didn't match the previous one, which means there is probably an episode title in the filename
			formattedName = formattedName.replaceAll("(?i)[\\s\\.]S0(\\d)E(\\d)(\\d)[\\s\\.]", " - $1$2$3 - ");

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(".*[sS][1-9]\\d[eE]\\d\\d.*")) {
			// This matches scene and most p2p TV episodes after their first 9 seasons

			// Rename the season/episode numbers. For example, "S11E01" changes to " - 1101"
			formattedName = formattedName.replaceAll("(?i)[\\s\\.]S([1-9]\\d)E(\\d)(\\d)(" + commonFileEnds + ")", " - $1$2$3");

			// If it matches this then it didn't match the previous one, which means there is probably an episode title in the filename
			formattedName = formattedName.replaceAll("(?i)[\\s\\.]S([1-9]\\d)E(\\d)(\\d)[\\s\\.]", " - $1$2$3 - ");

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(".*\\.(19|20)\\d\\d\\.[0-1]\\d\\.[0-3]\\d\\..*")) {
			// This matches scene and most p2p TV episodes that release several times per week

			// Rename the date. For example, "2013.03.18" changes to " - 2013/03/18"
			formattedName = formattedName.replaceAll("(?i)\\.(19|20)(\\d\\d)\\.([0-1]\\d)\\.([0-3]\\d)(" + commonFileEnds + ")", " - $1$2/$3/$4");

			// If it matches this then it didn't match the previous one, which means there is probably an episode title in the filename
			formattedName = formattedName.replaceAll("(?i)\\.(19|20)(\\d\\d)\\.([0-1]\\d)\\.([0-3]\\d)\\.", " - $1$2/$3/$4 - ");

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(".*\\.(19|20)\\d\\d\\..*")) {
			// This matches scene and most p2p movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("\\.(19|20)(\\d\\d)", " ($1$2)");

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");

			// Change "3D" to " (3D)", "CD1" to "(1)", etc.
			formattedName = formattedName.replaceAll("(?i)\\.(CD[1-3])\\.|\\.(3D)\\.|\\.(Special.Edition)\\.|\\.(Unrated)\\.|\\.(Final.Cut)\\.|\\.(Remastered)\\.|\\.(Extended.Cut)\\.", " ($1)");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(commonFileEndsMatch)) {
			// This matches files that partially follow the scene format

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");

			// Change "3D" to " (3D)", "CD1" to "(1)", etc.
			formattedName = formattedName.replaceAll("(?i)\\.(CD[1-3])\\.|\\.(3D)\\.|\\.(Special.Edition)\\.|\\.(Unrated)\\.|\\.(Final.Cut)\\.|\\.(Remastered)\\.|\\.(Extended.Cut)\\.", " ($1)");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(".*\\[(19|20)\\d\\d\\].*")) {
			// This matches rarer types of movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("(?i)\\[(19|20)(\\d\\d)\\].*", " ($1$2)");

			// Replace periods with spaces
			formattedName = formattedName.replaceAll("\\.", " ");
		} else if (formattedName.matches(".*\\((19|20)\\d\\d\\).*")) {
			// This matches rarer types of movies

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");
		} else if (formattedName.matches(".*\\((19|20)\\d\\d\\).*")) {
			// This matches rarer types of movies

			// Remove stuff at the end of the filename like release group, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)" + commonFileEnds, "");
			formattedName = formattedName.replaceAll(commonFileEndsCaseSensitive, "");
		} else if (formattedName.matches(".*\\[[0-9a-zA-Z]{8}\\]$")) {
			// This matches anime with a hash at the end of the name

			// Remove underscores
			formattedName = formattedName.replaceAll("_", " ");

			// Remove stuff at the end of the filename like hash, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)\\s\\(1280x720.*|\\s\\(1920x1080.*|\\s\\(720x400.*|\\s\\(BD.*|\\s\\[Blu-Ray.*|\\s\\[DVD.*|\\.DVD.*|\\[[0-9a-zA-Z]{8}\\]$|\\[h264.*|R1DVD.*|\\[BD.*", "");

			// Remove group name from the beginning of the filename
			if (formattedName.substring(0, 1).matches("\\[")) {
				int closingBracketIndex = formattedName.indexOf("]");
				if (closingBracketIndex != -1) {
					formattedName = formattedName.substring(closingBracketIndex + 1);
				}

				if (formattedName.substring(0, 1).matches("\\s")) {
					formattedName = formattedName.substring(1);
				}
			}
		} else if (formattedName.matches(".*\\[BD\\].*|.*\\[720p\\].*|.*\\[1080p\\].*|.*\\[480p\\].*|.*\\[Blu-Ray.*|.*\\[h264.*")) {
			// This matches anime without a hash in the name

			// Remove underscores
			formattedName = formattedName.replaceAll("_", " ");

			// Remove stuff at the end of the filename like hash, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)\\[BD\\].*|\\[720p.*|\\[1080p.*|\\[480p.*|\\[Blu-Ray.*\\[h264.*", "");

			// Remove group name from the beginning of the filename
			if (formattedName.substring(0, 1).matches("\\[")) {
				int closingBracketIndex = formattedName.indexOf("]");
				if (closingBracketIndex != -1) {
					formattedName = formattedName.substring(closingBracketIndex + 1);
				}

				if (formattedName.substring(0, 1).matches("\\s")) {
					formattedName = formattedName.substring(1);
				}
			}
		}

		return formattedName;
	}

	public static File getFileNameWithNewExtension(File parent, File file, String ext) {
		File ff = isFileExists(new File(parent, file.getName()), ext);

		if (ff != null && ff.exists()) {
			return ff;
		}

		return null;
	}

	/**
	 * @deprecated Use {@link #getFileNameWithNewExtension(File, File, String)}.
	 */
	@Deprecated
	public static File getFileNameWitNewExtension(File parent, File f, String ext) {
		return getFileNameWithNewExtension(parent, f, ext);
	}

	public static File getFileNameWithAddedExtension(File parent, File f, String ext) {
		File ff = new File(parent, f.getName() + ext);

		if (ff.exists()) {
			return ff;
		}

		return null;
	}

	/**
	 * @deprecated Use {@link #getFileNameWithAddedExtension(File, File, String)}.
	 */
	@Deprecated
	public static File getFileNameWitAddedExtension(File parent, File file, String ext) {
		return getFileNameWithAddedExtension(parent, file, ext);
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

	/**
	 * @deprecated Use {@link #isSubtitlesExists(File file, DLNAMediaInfo media)} instead.
	 */
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media) {
		return isSubtitlesExists(file, media);
	}

	public static boolean isSubtitlesExists(File file, DLNAMediaInfo media) {
		return isSubtitlesExists(file, media, true);
	}

	/**
	 * @deprecated Use {@link #isSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache)} instead.
	 */
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
		return isSubtitlesExists(file, media, usecache);
	}

	public static boolean isSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
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
			cache = new HashMap<>();
		}

		File[] allSubs = cache.get(subFolder);
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
	 *
	 * @param file File to detect charset/encoding
	 * @return file's charset {@link org.mozilla.universalchardet.Constants}
	 *         or null if not detected
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
	 *
	 * @param file File to test
	 * @return true if file is UTF-8 encoded with or without BOM, false otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF8(File file) throws IOException {
		return isCharsetUTF8(getFileCharset(file));
	}

	/**
	 * Tests if charset is UTF-8 encoded with or without BOM.
	 *
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

	/**
	 * Determine whether a file is readable by trying to read it. This works around JDK bugs which
	 * return the wrong results for {@link java.io.File#canRead()} on Windows, and in some cases, on Unix.
	 * <p>
	 * Note: since this method accesses the filesystem, it should not be used in contexts in which performance is critical.
	 * Note: this method changes the file access time.
	 *
	 * @since 1.71.0
	 * @param file the File whose permissions are to be determined
	 * @return <code>true</code> if the file is not null, exists, is a file and can be read, <code>false</code> otherwise
	 */
	// based on the workaround posted here:
	// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4993360
	// XXX why isn't this in Apache Commons?
	public static boolean isFileReadable(File file) {
		boolean isReadable = false;

		if ((file != null) && file.isFile()) {
			try {
				new FileInputStream(file).close();
				isReadable = true;
			} catch (IOException ioe) { }
		}

		return isReadable;
	}

	/**
	 * Determine whether a file is writable by trying to write it. This works around JDK bugs which
	 * return the wrong results for {@link java.io.File#canWrite()} on Windows and, in some cases, on Unix.
	 * <p>
	 * Note: since this method accesses the filesystem, it should not be used in contexts in which performance is critical.
	 * Note: this method changes the file access time and may change the file modification time.
	 *
	 * @since 1.71.0
	 * @param file the File whose permissions are to be determined
	 * @return <code>true</code> if the file is not null and either a) exists, is a file and can be written to or b) doesn't
	 * exist and can be created; otherwise returns <code>false</code>
	 */
	// Loosely based on the workaround posted here:
	// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4993360
	// XXX why isn't this in Apache Commons?
	public static boolean isFileWritable(File file) {
		boolean isWritable = false;

		if (file != null) {
			boolean fileAlreadyExists = file.isFile(); // i.e. exists and is a File

			if (fileAlreadyExists || !file.exists()) {
				try {
					// true: open for append: make sure the open
					// doesn't clobber the file
					new FileOutputStream(file, true).close();
					isWritable = true;

					if (!fileAlreadyExists) { // a new file has been "touch"ed; try to remove it
						try {
							if (!file.delete()) {
								LOGGER.warn("Can't delete temporary test file: {}", file.getAbsolutePath());
							}
						} catch (SecurityException se) {
							LOGGER.error("Error deleting temporary test file: " + file.getAbsolutePath(), se);
						}
					}
				} catch (IOException | SecurityException ioe) {
				}
			}
		}

		return isWritable;
	}

	/**
	 * Determines whether the supplied directory is readable by trying to
	 * read its contents.
	 * This works around JDK bugs which return the wrong results for
	 * {@link java.io.File#canRead()} on Windows and possibly on Unix.
	 *
	 * Note: since this method accesses the filesystem, it should not be
	 * used in contexts in which performance is critical.
	 * Note: this method changes the file access time.
	 *
	 * @since 1.71.0
	 * @param dir the File whose permissions are to be determined
	 * @return <code>true</code> if the File is not null, exists, is a
	 *         directory and can be read, <code>false</code> otherwise
	 */
	// XXX dir.canRead() has issues on Windows, so verify it directly:
	// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6203387
	public static boolean isDirectoryReadable(File dir) {
		boolean isReadable = false;

		if (dir != null) {
			// new File("").isDirectory() is false, even though getAbsolutePath() returns the right path.
			// this resolves it
			dir = dir.getAbsoluteFile();

			if (dir.isDirectory()) {
				try {
					File[] files = dir.listFiles(); // null if an I/O error occurs
					isReadable = files != null;
				} catch (SecurityException se) { }
			}
		}

		return isReadable;
	}

	/**
	 * Determines whether the supplied directory is writable by trying to
	 * write a file to it.
	 * This works around JDK bugs which return the wrong results for
	 * {@link java.io.File#canWrite()} on Windows and possibly on Unix.
	 *
	 * Note: since this method accesses the filesystem, it should not be
	 * used in contexts in which performance is critical.
	 * Note: this method changes the file access time and may change the
	 * file modification time.
	 *
	 * @since 1.71.0
	 * @param dir the File whose permissions are to be determined
	 * @return <code>true</code> if the File is not null, exists, is a
	 *         directory and can be written to, <code>false</code> otherwise
	 */
	// XXX dir.canWrite() has issues on Windows, so verify it directly:
	// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6203387
	public static boolean isDirectoryWritable(File dir) {
		boolean isWritable = false;

		if (dir != null) {
			// new File("").isDirectory() is false, even though getAbsolutePath() returns the right path.
			// this resolves it
			dir = dir.getAbsoluteFile();

			if (dir.isDirectory()) {
				File file = new File(
					dir,
					String.format(
						"pms_directory_write_test_%d_%d.tmp",
						System.currentTimeMillis(),
						Thread.currentThread().getId()
					)
				);

				try {
					if (file.createNewFile()) {
						if (isFileWritable(file)) {
							isWritable = true;
						}

						if (!file.delete()) {
							LOGGER.warn("Can't delete temporary test file: {}", file.getAbsolutePath());
						}
					}
				} catch (IOException | SecurityException ioe) {
				}
			}
		}

		return isWritable;
	}
}
