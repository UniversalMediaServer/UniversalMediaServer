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

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.sun.jna.Platform;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.PMS;
import static net.pms.PMS.getConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.metadata.MediaVideoMetadata;
import net.pms.platform.windows.WindowsProgramPaths;
import static net.pms.util.Constants.*;
import net.pms.util.FilePermissions.FileFlag;
import net.pms.util.StringUtil.LetterCase;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

	/**
	 * An array of chars that qualifies as file path separators. For Windows
	 * filesystems that is forward slash in addition to
	 * {@link File#separatorChar}, for other filesystems it is only
	 * {@link File#separatorChar}. This is to be compatible with Java's behavior
	 * of accepting forward slash as a separator also on Windows.
	 */
	private static final char[] FILE_SEPARATORS;

	// Signal an invalid parameter in getFileLocation() without raising an exception or returning null
	private static final String DEFAULT_BASENAME = "NO_DEFAULT_BASENAME_SUPPLIED.conf";

	static {
		char separator = File.separatorChar;
		if (separator == '\\') {
			FILE_SEPARATORS = new char[2];
			FILE_SEPARATORS[0] = separator;
			FILE_SEPARATORS[1] = '/';
		} else {
			FILE_SEPARATORS = new char[1];
			FILE_SEPARATORS[0] = separator;
		}
	}

	// This class is not instantiable
	private FileUtil() {
	}

	/**
	 * A helper class used by {@link #getFileLocation(String, String, String)}
	 * which provides access to a file's absolute path and that of its
	 * directory.
	 *
	 * @since 1.90.0
	 */
	public static final class FileLocation {
		private String directoryPath;
		private String filePath;

		FileLocation(File directory, File file) {
			this.directoryPath = FilenameUtils.normalize(directory.getAbsolutePath());
			this.filePath = FilenameUtils.normalize(file.getAbsolutePath());
		}

		public String getDirectoryPath() {
			return directoryPath;
		}

		public String getFilePath() {
			return filePath;
		}
	}

	/**
	 * Returns a {@link FileLocation} object which provides access to the
	 * directory and file paths of the specified file as normalised, absolute
	 * paths.
	 *
	 * This determines the directory and file path of a file according to the
	 * profile rules.
	 *
	 * @param customPath an optional user-defined path for the resource
	 * @param defaultDirectory a default directory path used if no custom path
	 *            is provided
	 * @param defaultBasename a default filename used if a) no custom path is
	 *            provided or b) the custom path is a directory
	 * @return a {@link FileLocation} object providing access to the file's
	 *         directory and file paths
	 * @since 1.90.0
	 */
	// this is called from a static initialiser, where errors aren't clearly reported,
	// so do everything possible to return a valid response, even if the parameters aren't sane
	public static FileLocation getFileLocation(
		String customPath,
		String defaultDirectory,
		String defaultBasename
	) {
		File customFile = null;
		File directory = null;
		File file = null;

		if (isBlank(defaultBasename)) {
			// shouldn't get here
			defaultBasename = DEFAULT_BASENAME;
		}

		if (defaultDirectory == null) {
			defaultDirectory = ""; // current directory
		}

		if (customPath != null) {
			customFile = new File(customPath).getAbsoluteFile();
		}

		if (customFile != null) {
			if (customFile.exists()) {
				if (customFile.isDirectory()) {
					directory = customFile;
					file = new File(customFile, defaultBasename).getAbsoluteFile();
				} else {
					directory = customFile.getParentFile();
					file = customFile;
				}
			} else {
				File parentDirectoryFile = customFile.getParentFile();
				if (parentDirectoryFile != null && parentDirectoryFile.exists()) {
					// parent directory exists: the file can be created
					directory = parentDirectoryFile;
					file = customFile;
				}
			}
		}

		if (directory == null || file == null) {
			directory = new File(defaultDirectory).getAbsoluteFile();
			file = new File(directory, defaultBasename).getAbsoluteFile();
		}

		return new FileLocation(directory, file);
	}

	public static final class InvalidFileSystemException extends Exception {

		private static final long serialVersionUID = -4545843729375389876L;

		public InvalidFileSystemException() {
			super();
		}

		public InvalidFileSystemException(String message) {
			super(message);
		}

		public InvalidFileSystemException(Throwable cause) {
			super(cause);
		}

		public InvalidFileSystemException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Checks if the specified {@link String} is the file path separator. On
	 * Windows filesystems both {@code "\"} and {@code "/"} is considered as
	 * such by this method.
	 *
	 * @param character the character to check.
	 * @return {@code true} if {@code character} is the file path separator,
	 *         {@code false} otherwise.
	 */
	public static boolean isSeparator(String character) {
		if (character == null || character.length() != 1) {
			return false;
		}

		return isSeparator(character.charAt(0));
	}

	/**
	 * Checks if the specified {@code char} is the file path separator. On
	 * Windows filesystems both {@code "\"} and {@code "/"} is considered as
	 * such by this method.
	 *
	 * @param character the character to check.
	 * @return {@code true} if {@code character} is the file path separator,
	 *         {@code false} otherwise.
	 */
	public static boolean isSeparator(char character) {
		for (char entry : FILE_SEPARATORS) {
			if (character == entry) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the last index position of the file path separator in the same
	 * way as {@link String#lastIndexOf(String)} does. On Windows filesystems
	 * both {@code "\"} and {@code "/"} is considered a file path separator by
	 * this method.
	 *
	 * @param fileName the filename for which to find the last file path
	 *            separator.
	 * @return The index of the last file path separator or {@code -1} if not
	 *         found.
	 */
	public static int getIndexOfLastSeparator(String fileName) {
		if (fileName == null) {
			return -1;
		}

		if (FILE_SEPARATORS.length == 2) {
			return Math.max(fileName.lastIndexOf(FILE_SEPARATORS[0]), fileName.lastIndexOf(FILE_SEPARATORS[1]));
		}

		return fileName.lastIndexOf(FILE_SEPARATORS[0]);
	}

	public static boolean isUrl(String filename) {
		// We're intentionally avoiding stricter URI() methods, which can throw
		// URISyntaxException for psuedo-urls (e.g. librtmp-style urls containing spaces)
		return filename != null && filename.matches("\\S+://.*");
	}

	public static String getProtocol(String filename) {
		// Intentionally avoids URI.getScheme(), see above
		if (isUrl(filename)) {
			return filename.split("://")[0].toLowerCase(Locale.ROOT);
		}

		return null;
	}

	public static String urlJoin(String base, String filename) {
		if (isUrl(filename)) {
			return filename;
		}

		try {
			return new URL(new URL(base), filename).toString();
		} catch (MalformedURLException e) {
			return filename;
		}
	}

	public static String getUrlExtension(String u) {
		// Omit the query string, if any
		return getExtension(substringBefore(u, "?"), null, null);
	}


	/**
	 * Returns the file extension from the specified {@link File} or
	 * {@code null} if it has no extension.
	 *
	 * @param file the {@link File} from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable File file) {
		if (file == null || file.getName() == null) {
			return null;
		}

		return getExtension(file.getName(), null, null);
	}

	/**
	 * Returns the file extension from the specified {@link File} or
	 * {@code null} if it has no extension.
	 *
	 * @param file the {@link File} from which to extract the extension.
	 * @param convertTo if {@code null} makes no letter case change to the
	 *            returned {@link String}, otherwise converts the extracted
	 *            extension (if any) to the corresponding letter case.
	 * @param locale the {@link Locale} to use for letter case conversion.
	 *            Defaults to {@link Locale#ROOT} if {@code null}.
	 * @return The extracted and potentially letter case converted extension or
	 *         {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable File file, LetterCase convertTo, Locale locale) {
		if (file == null || file.getName() == null) {
			return null;
		}

		return getExtension(file.getName(), convertTo, locale);
	}

	/**
	 * Returns the file extension from the specified {@link Path} or
	 * {@code null} if it has no extension.
	 *
	 * @param file the {@link Path} from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable Path file) {
		return getExtension(file, null, null);
	}

	/**
	 * Returns the file extension from the specified {@link Path} or
	 * {@code null} if it has no extension.
	 *
	 * @param file the {@link Path} from which to extract the extension.
	 * @param convertTo if {@code null} makes no letter case change to the
	 *            returned {@link String}, otherwise converts the extracted
	 *            extension (if any) to the corresponding letter case.
	 * @param locale the {@link Locale} to use for letter case conversion.
	 *            Defaults to {@link Locale#ROOT} if {@code null}.
	 * @return The extracted and potentially letter case converted extension or
	 *         {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable Path file, LetterCase convertTo, Locale locale) {
		if (file == null) {
			return null;
		}

		Path fileName = file.getFileName();
		if (fileName == null || isBlank(fileName.toString())) {
			return null;
		}

		return getExtension(fileName.toString(), convertTo, locale);
	}

	/**
	 * Returns the file extension from {@code fileName} or {@code null} if
	 * {@code fileName} has no extension.
	 *
	 * @param fileName the file name from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable String fileName) {
		return getExtension(fileName, null, null);
	}

	/**
	 * Returns the file extension from {@code fileName} or {@code null} if
	 * {@code fileName} has no extension.
	 *
	 * @param fileName the file name from which to extract the extension.
	 * @param convertTo if {@code null} makes no letter case change to the
	 *            returned {@link String}, otherwise converts the extracted
	 *            extension (if any) to the corresponding letter case.
	 * @param locale the {@link Locale} to use for letter case conversion.
	 *            Defaults to {@link Locale#ROOT} if {@code null}.
	 * @return The extracted and potentially letter case converted extension or
	 *         {@code null}.
	 */
	@Nullable
	public static String getExtension(@Nullable String fileName, LetterCase convertTo, Locale locale) {
		if (isBlank(fileName)) {
			return null;
		}

		int dot = fileName.lastIndexOf('.');
		if (dot == -1 || getIndexOfLastSeparator(fileName) > dot) {
			return null;
		}

		if (convertTo != null && locale == null) {
			locale = Locale.ROOT;
		}

		String extension = fileName.substring(dot + 1);
		if (convertTo == LetterCase.UPPER) {
			return extension.toUpperCase(locale);
		}

		if (convertTo == LetterCase.LOWER) {
			return extension.toLowerCase(locale);
		}

		return extension;
	}

	/**
	 * Returns a filename with the extension (if any) stripped off.
	 *
	 * @param file the {@link File}.
	 * @return The extensionless filename.
	 */
	public static String getFileNameWithoutExtension(File file) {
		if (file == null) {
			return null;
		}

		return getFileNameWithoutExtension(file.getName());
	}

	/**
	 * Returns a filename with the extension (if any) stripped off.
	 *
	 * @param file the {@link Path}.
	 * @return The extensionless filename.
	 */
	public static String getFileNameWithoutExtension(Path file) {
		if (file == null) {
			return null;
		}

		Path fileName = file.getFileName();
		if (fileName == null) {
			return null;
		}

		return getFileNameWithoutExtension(fileName.toString());
	}

	/**
	 * Returns a filename with the extension (if any) stripped off.
	 *
	 * @param fileName the filename.
	 * @return The extensionless filename.
	 */
	public static String getFileNameWithoutExtension(String fileName) {
		if (isBlank(fileName)) {
			return fileName;
		}

		int point = fileName.lastIndexOf('.');
		if (point == -1 || getIndexOfLastSeparator(fileName) > point) {
			return fileName;
		}

		return fileName.substring(0, point);
	}

	private static final class FormattedNameAndEdition {
		private final String formattedName;
		private final String edition;

		public FormattedNameAndEdition(String formattedName, String edition) {
			this.formattedName = formattedName;
			this.edition = edition;
		}

		public String getEdition() {
			return edition;
		}

		public String getFormattedName() {
			return formattedName;
		}
	}

	/**
	 * Remove and save edition information to be added later.
	 */
	private static FormattedNameAndEdition removeAndSaveEditionToBeAddedLater(String formattedName) {
		String edition = null;
		Matcher m = COMMON_FILE_EDITIONS_PATTERN.matcher(formattedName);
		if (m.find()) {
			edition = m.group().replaceAll("\\.", " ");
			edition = "(" + WordUtils.capitalizeFully(edition) + ")";
			formattedName = formattedName.replaceAll(" - " + COMMON_FILE_EDITIONS, "");
			formattedName = formattedName.replaceAll(COMMON_FILE_EDITIONS, "");
		}

		return new FormattedNameAndEdition(formattedName, edition);
	}

	/**
	 * Capitalize the first letter of each word if the string contains no
	 * capital letters.
	 */
	private static String convertFormattedNameToTitleCaseParts(String formattedName) {
		if (formattedName.equals(formattedName.toLowerCase())) {
			StringBuilder formattedNameBuilder = new StringBuilder();
			for (String part : formattedName.split(" - ")) {
				if (formattedNameBuilder.length() > 0) {
					formattedNameBuilder.append(" - ");
				}

				formattedNameBuilder.append(convertLowerCaseStringToTitleCase(part));
			}

			formattedName = formattedNameBuilder.toString();
		}

		return formattedName;
	}

	/**
	 * Capitalize the first letter of each word if the string contains no
	 * capital letters.
	 */
	private static String convertFormattedNameToTitleCase(String formattedName) {
		if (formattedName.equals(formattedName.toLowerCase())) {
			formattedName = convertLowerCaseStringToTitleCase(formattedName);
		}

		return formattedName;
	}

	/**
	 * Remove group name from the beginning of the filename.
	 */
	private static String removeGroupNameFromBeginning(String formattedName) {
		if (!"".equals(formattedName) && (formattedName.startsWith("[") || formattedName.startsWith("("))) {
			Pattern pattern = Pattern.compile("^[\\[\\(][^\\]]{0,25}[\\]\\)][^\\w]*(\\w.*?)\\s*$");
			Matcher matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				formattedName = matcher.group(1);
			} else if (formattedName.endsWith("]")) {
				pattern = Pattern.compile("^\\[([^\\[\\]]+)\\]\\s*$");
				matcher = pattern.matcher(formattedName);
				if (matcher.find()) {
					formattedName = matcher.group(1);
				}
			}
		}

		return formattedName;
	}

	/**
	 * Remove stuff at the end of the filename like release group, quality,
	 * source, etc.
	 */
	private static String removeFilenameEndMetadata(String formattedName) {
		formattedName = formattedName.replaceAll(COMMON_FILE_ENDS_CASE_SENSITIVE, "");
		formattedName = formattedName.replaceAll("(?i)" + COMMON_FILE_ENDS, "");
		return formattedName;
	}

	/**
	 * Strings that only occur after all useful information. When we encounter
	 * one of these strings, the string and everything after them will be
	 * removed.
	 */
	private static final String COMMON_FILE_ENDS = "\\s\\[BD.*|[\\s\\(]DUBBED.*|[\\s\\(]AC3.*|[\\s\\(]NTSC.*|[\\s\\(]TVNZ\\s.*|[\\s\\(]FP\\s.*|[\\s\\(]AAC.*|[\\s\\(]REPACK.*|[\\s\\(]480p.*|[\\s\\(]720p.*|[\\s\\(]m-720p.*|[\\s\\(]900p.*|[\\s\\(]1080i.*|[\\s\\(]1080p.*|[\\s\\(]2160p.*|[\\s\\(]WEB-DL.*|[\\s\\(]HDTV.*|[\\s\\(]DSR.*|[\\s\\(]PDTV.*|[\\s\\(]SDTV.*|[\\s\\(]WS.*|[\\s\\(]HQ.*|[\\s\\(]DVDRip.*|[\\s\\(]TVRiP.*|[\\s\\(]BDRip.*|[\\s\\(]BRRip.*|[\\s\\(]WEBRip.*|[\\s\\(]BluRay.*|[\\s\\(]Blu-ray.*|[\\s\\(]SUBBED.*|[\\s\\(]x264.*|[\\s\\(]x265.*|[\\s\\(]XviD.*|[\\s\\(]Dual\\sAudio.*|[\\s\\(]HSBS.*|[\\s\\(]H-SBS.*|[\\s\\(]RERiP.*|[\\s\\(]DIRFIX.*|[\\s\\(]READNFO.*|[\\s\\(]60FPS.*|[\\s\\(]HDR.*|[\\s\\(]DV[\\s\\(].*";
	private static final String COMMON_FILE_ENDS_MATCH = ".*\\s\\[BD.*|.*[\\s\\(]DUBBED.*|.*[\\s\\(]AC3.*|.*[\\s\\(]NTSC.*|.*[\\s\\(]TVNZ.*|.*[\\s\\(]FP.*|.*[\\s\\(]AAC.*|.*[\\s\\(]REPACK.*|.*[\\s\\(]480p.*|.*[\\s\\(]720p.*|.*[\\s\\(]m-720p.*|.*[\\s\\(]900p.*|.*[\\s\\(]1080i.*|.*[\\s\\(]1080p.*|.*[\\s\\(]2160p.*|.*[\\s\\(]WEB-DL.*|.*[\\s\\(]HDTV.*|.*[\\s\\(]DSR.*|.*[\\s\\(]PDTV.*|.*[\\s\\(]SDTV.*|.*[\\s\\(]WS.*|.*[\\s\\(]HQ.*|.*[\\s\\(]DVDRip.*|.*[\\s\\(]TVRiP.*|.*[\\s\\(]BDRip.*|.*[\\s\\(]BRRip.*|.*[\\s\\(]WEBRip.*|.*[\\s\\(]BluRay.*|.*[\\s\\(]Blu-ray.*|.*[\\s\\(]SUBBED.*|.*[\\s\\(]x264.*|.*[\\s\\(]x265.*|.*[\\s\\(]XviD.*|.*[\\s\\(]Dual\\sAudio.*|.*[\\s\\(]HSBS.*|.*[\\s\\(]H-SBS.*|.*[\\s\\(]RERiP.*|.*[\\s\\(]DIRFIX.*|.*[\\s\\(]READNFO.*|.*[\\s\\(]60FPS.*|.*[\\s\\(]HDR.*|.*[\\s\\(]DV[\\s\\(].*";

	private static final String COMMON_ANIME_FILE_ENDS = "(?i)\\s\\(1280x720.*|\\s\\(1920x1080.*|\\s\\(720x400.*|\\s[\\[\\(]\\d{3,4}p.*|\\s\\(BD.*|\\s\\[Blu-Ray.*|\\s\\[DVD.*|\\.DVD.*|\\[[0-9a-zA-Z]{8}\\]$|\\[h264.*|R1DVD.*|\\[BD.*|[\\s_]\\(Dual\\sAudio.*|\\s\\[VOSTFR\\].*|\\s\\[HD_\\d{3,4}x\\d{3,4}\\].*";
	private static final String COMMON_ANIME_FILE_ENDS_MATCH = ".*\\s\\(1280x720.*|.*\\s\\(1920x1080.*|.*\\s\\(720x400.*|.*\\s[\\[\\(]\\d{3,4}p.*|.*\\s\\(BD.*|.*\\s\\[Blu-Ray.*|.*\\s\\[DVD.*|\\.DVD.*|.*\\s\\[[0-9a-zA-Z]{8}\\]$|.*\\s\\[h264.*|.*\\sR1DVD.*|.*\\s\\[BD.*|.*[\\s_]\\(Dual\\sAudio.*|.*\\s\\[VOSTFR\\].*|.*\\s\\[HD_\\d{3,4}x\\d{3,4}\\].*|.*\\s\\(\\d{1,2}bit.*";

	private static final String SCENE_P2P_EPISODE_REGEX = "[sS](\\d{1,2})(?:\\s|)[eE](\\d{1,}\\w{1}|\\d{1,})";
	private static final String SCENE_P2P_EPISODE_SPECIAL_REGEX = "[sS](\\d{2})\\s(\\w{3,})";
	private static final String MIXED_EPISODE_CONVENTION = "\\s(?:Ep|e)(?:\\s{1,2}|)(\\d{1,4})(?:\\s|$)";
	private static final String MIXED_EPISODE_CONVENTION_MATCH = ".*" + MIXED_EPISODE_CONVENTION + ".*";
	private static final Pattern MIXED_EPISODE_CONVENTION_PATTERN = Pattern.compile(MIXED_EPISODE_CONVENTION);

	private static final String MINISERIES_CONVENTION = "\\s(\\d{1,2})of\\d{1,2}\\s";
	private static final String MINISERIES_CONVENTION_MATCH = ".*" + MINISERIES_CONVENTION + ".*";
	private static final Pattern MINISERIES_CONVENTION_PATTERN = Pattern.compile(MINISERIES_CONVENTION);

	private static final String SCENE_MULTI_EPISODE_CONVENTION = "[sS](\\d{1,2})[eE](\\d{1,})([eE]|-[eE])(\\d{1,})";
	private static final String SCENE_MULTI_EPISODE_CONVENTION_MATCH = ".*" + SCENE_MULTI_EPISODE_CONVENTION + ".*";
	private static final Pattern SCENE_MULTI_EPISODE_CONVENTION_PATTERN = Pattern.compile(SCENE_MULTI_EPISODE_CONVENTION);

	private static final String SHOW_NAME_INDEX_MATCHER = "(?i) (S\\d{2}E\\d{2}\\w{1}|S\\d{2}E\\d{2}|S\\d{2}|S\\d{2}E\\d{2}-\\d{2}|\\d{4}/\\d{2}/\\d{2})";
	private static final Pattern SHOW_NAME_INDEX_PATTERN = Pattern.compile(SHOW_NAME_INDEX_MATCHER + " - (.*)");
	private static final Pattern SHOW_NAME_INDEX_FALLBACK_PATTERN = Pattern.compile(SHOW_NAME_INDEX_MATCHER);

	/**
	 * Same as above, but they are common words so we reduce the chances of a
	 * false-positive by being case-sensitive.
	 */
	private static final String COMMON_FILE_ENDS_CASE_SENSITIVE = "\\sPROPER\\s.*|\\siNTERNAL\\s.*|\\sLIMITED\\s.*|\\sLiMiTED\\s.*|\\sFESTiVAL\\s.*|\\sNORDIC\\s.*|\\sREAL\\s.*|\\sSUBBED\\s.*|\\sDUBBED\\s.*|\\sRETAIL\\s.*|\\sEXTENDED\\s.*|\\sNEWEDIT\\s.*|\\sWEB\\s.*";

	/**
	 * Editions to be added to the end of the prettified name
	 */
	private static final String COMMON_FILE_EDITIONS = "(?i)(?!\\()(Special\\sEdition|Unrated|Final\\sCut|Remastered|Extended\\sCut|IMAX\\sEdition|Uncensored|Directors\\sCut|Uncut)(?!\\))";
	private static final Pattern COMMON_FILE_EDITIONS_PATTERN = Pattern.compile(COMMON_FILE_EDITIONS);

	private static final String COMMON_ANIME_EPISODE_NUMBERS = "(?:[\\s']|S\\d{1,2}\\sE|\\s-\\s)(?:[eE]|)(?:[pP]|)(\\d{1,4}\\s\\d{1}|\\d{1,4})(?:\\s|'|v\\d|)?";
	private static final Pattern COMMON_ANIME_EPISODE_NUMBERS_PATTERN = Pattern.compile(COMMON_ANIME_EPISODE_NUMBERS);

	private static final Pattern SEASON_NUMBER_IN_SERIES_TITLE_PATTERN = Pattern.compile(".*\\sS(\\d)$");

	private static final String COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS = "(?:[\\s']|S\\d{1,2}\\sE)(?:[pP]|)(\\d{1,4}-\\d{1,4})(?:[\\s']|v\\d)";
	private static final Pattern COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS_PATTERN = Pattern.compile(COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS);

	/**
	 * Attempts to resolve short filenames by using their parent directory.
	 * For example, it can be common for files to have a filename like:
	 *    groupname-moviename.1080p-x264
	 * Which is very hard to parse, while the directory name is much better:
	 *    Movie.Name.2001.1080p.BluRay.x264-GROUPNAME
	 *
	 * @return the parent directory or the original filename if there was no match
	 */
	public static String replaceShortFilenameWithParentDirectoryName(String filename, String absolutePath) {
		if (absolutePath == null || filename == null) {
			return filename;
		}

		// With this naming convention, the filename is always lower case
		if (!filename.toLowerCase().equals(filename)) {
			return filename;
		}

		String pathWithoutFilename = substringBeforeLast(absolutePath, "\\");
		if (pathWithoutFilename == null) {
			return filename;
		}
		String parentDirectory = substringAfterLast(pathWithoutFilename, "\\");
		if (parentDirectory == null) {
			return filename;
		}

		String groupNameFromDirectory = substringAfterLast(parentDirectory, "-");
		if (groupNameFromDirectory == null) {
			return filename;
		}

		// Remove any host information from the group name, e.g. Movie.Name.2001.1080p.BluRay.x264-GROUPNAME [HostName]
		String groupNameFromDirectoryWithoutHost = substringBefore(groupNameFromDirectory, " ");
		if (groupNameFromDirectoryWithoutHost != null) {
			groupNameFromDirectory = groupNameFromDirectoryWithoutHost;
		}

		String groupNameFromFilename = substringBefore(filename, "-");
		if (groupNameFromFilename == null) {
			return filename;
		}

		if (!groupNameFromFilename.equals(lowerCase(groupNameFromDirectory))) {
			// We didn't match the group name exactly, but let's try a partial match
			String groupNameFromDirectoryWithoutNumbers = lowerCase(groupNameFromDirectory).replaceAll("\\d", "");

			/*
			 * Sometimes the release group will have a number in it, and that will
			 * be in the directory but not the filename, which is really stupid but
			 * that's how it is. Here we remove that character from both strings
			 * before attempting to match.
			 */
			Pattern pattern = Pattern.compile("\\d");
			Matcher matcher = pattern.matcher(groupNameFromDirectory);
			if (matcher.find()) {
				Integer numberIndex = matcher.start();
				groupNameFromFilename = new StringBuilder(groupNameFromFilename).deleteCharAt(numberIndex).toString();
			}

			if (!groupNameFromDirectoryWithoutNumbers.startsWith(groupNameFromFilename)) {
				return filename;
			}
		}

		return parentDirectory;
	}

	public static String getFileNamePrettified(String f, String absolutePath) {
		return getFileNamePrettified(f, null, false, false, absolutePath);
	}

	/**
	 * Returns the filename after being "prettified", which involves attempting
	 * to strip away certain things like information about the quality,
	 * resolution, codecs, release groups, fansubbers, etc., replacing periods
	 * with spaces, and various other things to produce a more "pretty" and
	 * standardized filename.
	 *
	 * @param f The filename
	 * @param media
	 * @param isEpisodeWithinSeasonFolder whether this is an episode within
	 *                                    a season folder in the Media Library
	 * @param isEpisodeWithinTVSeriesFolder whether this is an episode within
	 *                                      a TV series folder in the Media Library
	 * @param absolutePath the full path to the file
	 *
	 * @return The prettified filename
	 */
	public static String getFileNamePrettified(String f, MediaInfo media, boolean isEpisodeWithinSeasonFolder, boolean isEpisodeWithinTVSeriesFolder, String absolutePath) {
		String formattedName;

		String title;
		String year = "";
		String extraInformation;
		String tvSeason;
		String tvSeasonPadded;
		String tvEpisodeNumber;
		String tvEpisodeName;
		String tvSeriesStartYear = "";
		boolean isTVEpisode = false;

		// Attempt to get API metadata from the database if it wasn't passed via the media parameter
		if (media == null && absolutePath != null && getConfiguration().getUseCache()) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					media = MediaTableFiles.getFileMetadata(connection, absolutePath);
				}
			} catch (IOException | SQLException e) {
				LOGGER.debug("Error while fetching metadata from database for prettifying: {}", e);
			} finally {
				MediaDatabase.close(connection);
			}
		}

		// Populate the variables from the data if we can, otherwise from the filename
		if (media != null && getConfiguration().getUseCache() && media.hasVideoMetadata() && isNotBlank(media.getVideoMetadata().getMovieOrShowName())) {
			MediaVideoMetadata videoMetadata = media.getVideoMetadata();
			title             = videoMetadata.getMovieOrShowName();
			year              = isNotBlank(videoMetadata.getYear())              ? videoMetadata.getYear()              : "";
			extraInformation  = isNotBlank(videoMetadata.getExtraInformation())  ? videoMetadata.getExtraInformation()  : "";
			tvSeason          = isNotBlank(videoMetadata.getTVSeason())          ? videoMetadata.getTVSeason()          : "";
			tvEpisodeNumber   = isNotBlank(videoMetadata.getTVEpisodeNumber())   ? videoMetadata.getTVEpisodeNumber()   : "";
			tvEpisodeName     = isNotBlank(videoMetadata.getTVEpisodeName())     ? videoMetadata.getTVEpisodeName()     : "";
			isTVEpisode       = isNotBlank(videoMetadata.getTVSeason());
			tvSeriesStartYear = isNotBlank(videoMetadata.getTVSeriesStartYear()) ? videoMetadata.getTVSeriesStartYear() : "";
		} else {
			String[] metadataFromFilename = getFileNameMetadata(f, absolutePath);

			title            = isNotBlank(metadataFromFilename[0]) ? metadataFromFilename[0] : "";
			extraInformation = isNotBlank(metadataFromFilename[2]) ? metadataFromFilename[2] : "";
			tvSeason         = isNotBlank(metadataFromFilename[3]) ? metadataFromFilename[3] : "";
			tvEpisodeNumber  = isNotBlank(metadataFromFilename[4]) ? metadataFromFilename[4] : "";
			tvEpisodeName    = isNotBlank(metadataFromFilename[5]) ? metadataFromFilename[5] : "";

			if (isNotBlank(tvSeason)) {
				isTVEpisode = true;
				tvSeriesStartYear = isNotBlank(metadataFromFilename[1]) ? metadataFromFilename[1] : "";
			} else {
				year = isNotBlank(metadataFromFilename[1]) ? metadataFromFilename[1] : "";
			}
		}

		if (isBlank(title)) {
			return basicPrettify(f);
		}

		// Build the prettified filename from the metadata
		if (isTVEpisode) {
			tvSeasonPadded = tvSeason;
			boolean isEpisodeWithDate = false;
			if (tvSeason.matches("(19|20)\\d{2}")) {
				// If the season is a year, anticipate a "/" for a date
				tvSeasonPadded += "/";
				isEpisodeWithDate = true;
			} else {
				if (tvSeason.length() == 1) {
					tvSeasonPadded = "0" + tvSeasonPadded;
				}
				tvSeasonPadded = "S" + tvSeasonPadded;
			}

			// Make sure the episode number has a leading zero
			if (tvEpisodeNumber.length() == 1) {
				tvEpisodeNumber = "0" + tvEpisodeNumber;
			}

			/*
			 * If we are accessing this file via a Season folder within
			 * the Media Library, we already have the show name and the
			 * season in the preceding folders, so we only show the episode
			 * number and title.
			 */
			if (isEpisodeWithinSeasonFolder) {
				formattedName = "";
				if (isNotBlank(tvEpisodeNumber)) {
					formattedName = tvEpisodeNumber + " - ";
				}

				if (isBlank(tvEpisodeName)) {
					formattedName += "Episode " + tvEpisodeNumber;
				} else {
					formattedName += tvEpisodeName;
				}
			} else if (isEpisodeWithinTVSeriesFolder) {
				formattedName = tvSeasonPadded;
				if (!isEpisodeWithDate) {
					formattedName += "E";
				}
				formattedName += tvEpisodeNumber + " - ";

				if (isBlank(tvEpisodeName)) {
					formattedName += "Episode " + tvEpisodeNumber;
				} else {
					formattedName += tvEpisodeName;
				}
			} else {
				formattedName = title;
				if (isNotBlank(tvSeasonPadded)) {
					formattedName += " " + tvSeasonPadded;
				}
				if (isNotBlank(tvEpisodeNumber)) {
					if (!isEpisodeWithDate) {
						formattedName += "E";
					}
					formattedName += tvEpisodeNumber;
				}

				if (isNotBlank(tvEpisodeName)) {
					formattedName += " - " + tvEpisodeName;
				}
			}
		} else {
			formattedName = title;
			if (year != null && isNotBlank(year)) {
				formattedName += " (" + year + ")";
			} else if (isNotBlank(tvSeriesStartYear)) {
				formattedName += " (" + tvSeriesStartYear + ")";
			}
		}

		if (isNotBlank(extraInformation)) {
			formattedName += " " + extraInformation;
		}

		return formattedName;
	}

	/**
	 * Returns metadata from the filename which we will use to check the
	 * validity of online lookups.
	 *
	 * @param filename The filename to extract metadata from
	 * @param absolutePath
	 *
	 * @return The metadata
	 */
	public static String[] getFileNameMetadata(String filename, String absolutePath) {
		if (filename == null) {
			return new String[] {null, null, null, null, null, null};
		}

		String formattedName;

		// These are false unless we recognize that we could use some info on the video from IMDb
		boolean isMovieWithoutYear = false;
		boolean isSample = false;

		String movieOrShowName = null;
		String year            = null;
		String tvSeason        = null;
		String tvEpisodeName   = null;
		String tvEpisodeNumber = null;
		String edition         = null;

		// This can contain editions and "Sample" for now
		String extraInformation;

		Pattern pattern;
		Matcher matcher;

		filename = replaceShortFilenameWithParentDirectoryName(filename, absolutePath);

		formattedName = basicPrettify(filename);

		if (formattedName.toLowerCase(Locale.ENGLISH).endsWith("sample")) {
			isSample = true;
		}

		if (formattedName.matches(SCENE_MULTI_EPISODE_CONVENTION_MATCH)) {
			// This matches scene and most p2p TV episodes that are more than one episode
			matcher = SCENE_MULTI_EPISODE_CONVENTION_PATTERN.matcher(formattedName);

			if (matcher.find()) {
				tvSeason = matcher.group(1);
				if (tvSeason.length() == 1) {
					tvSeason = "0" + tvSeason;
				}
				tvEpisodeNumber = matcher.group(2);
				tvEpisodeNumber += "-" + matcher.group(4);
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("\\s" + SCENE_MULTI_EPISODE_CONVENTION + "\\s", " S" + tvSeason + "E$2-$3 - ");
			formattedName = formattedName.replaceAll("\\s" + SCENE_MULTI_EPISODE_CONVENTION, " S" + tvSeason + "E$2-$3");
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*" + SCENE_P2P_EPISODE_REGEX + ".*")) {
			// This matches scene and most p2p TV episodes
			pattern = Pattern.compile(SCENE_P2P_EPISODE_REGEX);
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				if (tvSeason.length() == 1) {
					tvSeason = "0" + tvSeason;
				}
				tvEpisodeNumber = matcher.group(2);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s" + SCENE_P2P_EPISODE_REGEX + "\\s", " S" + tvSeason + "E$2 - ");
			formattedName = formattedName.replaceAll("(?i)\\s" + SCENE_P2P_EPISODE_REGEX, " S" + tvSeason + "E$2");
			formattedName = formattedName.replaceAll(SCENE_P2P_EPISODE_REGEX, " S" + tvSeason + "E$2");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*" + SCENE_P2P_EPISODE_SPECIAL_REGEX + ".*")) {
			// This matches scene and most p2p TV special episodes, e.g. episodes that have no episode number in the filename
			pattern = Pattern.compile(SCENE_P2P_EPISODE_SPECIAL_REGEX);
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				if (tvSeason.length() == 1) {
					tvSeason = "0" + tvSeason;
				}
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s" + SCENE_P2P_EPISODE_SPECIAL_REGEX, " S" + tvSeason + " - $2");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*[\\s-\\.](\\d{1,2})[xX]\\d\\d.*")) {
			// This matches older scene (like .avi releases) and some p2p TV episodes
			// e.g. Universal Media Server - 1x02 - Mysterious Wordplay.mkv
			pattern = Pattern.compile("[\\s-\\.](\\d{1,2})[xX](\\d\\d)");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				if (tvSeason.length() == 1) {
					tvSeason = "0" + tvSeason;
				}
				tvEpisodeNumber = matcher.group(2);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)[\\s-\\.](\\d{1,2})[xX](\\d{1,})[\\s-\\.]", " S" + tvSeason + "E$2 - ");
			formattedName = formattedName.replaceAll("(?i)[\\s-\\.](\\d{1,2})[xX](\\d{1,})", " S" + tvSeason + "E$2");
			formattedName = formattedName.replaceAll("[\\s-\\.](\\d{1,2})[xX](\\d{1,})", " S" + tvSeason + "E$2");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s-\\s(\\d{3})\\s-\\s.*")) {
			// This matches other older scene (like .avi releases) and some p2p TV episodes
			// e.g. Universal Media Server - 102 - Mysterious Wordplay.mkv
			pattern = Pattern.compile("\\s-\\s(\\d{3})\\s-\\s");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				String tvSeasonAndEpisode = matcher.group(1);
				tvSeason = "0" + tvSeasonAndEpisode.substring(0, 1);
				tvEpisodeNumber = tvSeasonAndEpisode.substring(1, 3);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s-\\s(\\d{3})\\s-\\s", " S" + tvSeason + "E" + tvEpisodeNumber + " - ");
			formattedName = formattedName.replaceAll("(?i)\\s(\\d{3})", " S" + tvSeason + "E" + tvEpisodeNumber);
			formattedName = formattedName.replaceAll("\\s(\\d{3})", " S" + tvSeason + "E" + tvEpisodeNumber);
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(MINISERIES_CONVENTION_MATCH)) {
			// This matches some episodes in miniseries, like:
			// e.g. Universal.Media.Server.2of6.Mysterious.Wordplay.HDTV.1080i.groupname.mkv[website].mkv
			matcher = MINISERIES_CONVENTION_PATTERN.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = "01";
				tvEpisodeNumber = matcher.group(1);
				if (isNotBlank(tvEpisodeNumber) && tvEpisodeNumber.length() == 1) {
					tvEpisodeNumber = "0" + tvEpisodeNumber;
				}
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");

			// Here we match existing case, otherwise we risk breaking the Title Case conversion later
			String seasonLetterReplace = "S";
			String episodeLetterReplace = "E";
			if (formattedName.equals(formattedName.toLowerCase())) {
				seasonLetterReplace = "s";
				episodeLetterReplace = "e";
			}
			formattedName = formattedName.replaceAll(MINISERIES_CONVENTION, " " + seasonLetterReplace + tvSeason + episodeLetterReplace + tvEpisodeNumber + " - ");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(MIXED_EPISODE_CONVENTION_MATCH)) {
			// This matches another mixed convention, like:
			// e.g. Universal Media Server - Ep. 02 - Mysterious Wordplay.mp4
			matcher = MIXED_EPISODE_CONVENTION_PATTERN.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = "01";
				tvEpisodeNumber = matcher.group(1);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");

			// Here we match existing case, otherwise we risk breaking the Title Case conversion later
			String seasonLetterReplace = "S";
			String episodeLetterReplace = "E";
			if (formattedName.equals(formattedName.toLowerCase())) {
				seasonLetterReplace = "s";
				episodeLetterReplace = "e";
			}
			formattedName = formattedName.replaceAll(MIXED_EPISODE_CONVENTION, " " + seasonLetterReplace + tvSeason + episodeLetterReplace + tvEpisodeNumber + " - ");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s(19|20)\\d{2}\\s[0-1]\\d\\s[0-3]\\d\\s.*")) {
			// This matches scene and most p2p TV episodes that release several times per week
			pattern = Pattern.compile("\\s((?:19|20)\\d{2})\\s([0-1]\\d)\\s([0-3]\\d)\\s");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
				tvEpisodeNumber += "/" + matcher.group(3);
			}

			// Rename the date. For example, "2013.03.18" changes to "2013/03/18"
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s(19|20)(\\d{2})\\s([0-1]\\d)\\s([0-3]\\d)\\s", " $1$2/$3/$4 - ");
			formattedName = formattedName.replaceAll("(?i)\\s(19|20)(\\d{2})\\s([0-1]\\d)\\s([0-3]\\d)", " $1$2/$3/$4");
			formattedName = formattedName.replaceAll("\\s(19|20)(\\d{2})\\s([0-1]\\d)\\s([0-3]\\d)", " $1$2/$3/$4");
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches("^(?!.*\\d{1,3}[\\s:][\\s-]).*\\s(?:19|20)\\d{2}([1-9]|1[0-2])([1-9]|[12][0-9]|3[01]).*")) {
			// This matches some sports releases

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches("^(?!.*\\d{1,3}[\\s:][\\s-]).*\\s(?:19|20)\\d{2}.*")) {
			// This matches scene and most p2p movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("\\s(19|20)(\\d{2})", " ($1$2)");
			formattedName = removeFilenameEndMetadata(formattedName);
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\[(19|20)\\d{2}\\].*")) {
			// This matches rarer types of movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("(?i)\\[(19|20)(\\d{2})\\].*", " ($1$2)");
			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\((19|20)\\d{2}\\).*")) {
			// This matches rarer types of movies
			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\[[0-9a-zA-Z]{8}\\]$") || formattedName.matches(".*\\s-\\s\\d{1,3}$") || formattedName.matches(COMMON_ANIME_FILE_ENDS_MATCH)) {
			/*
			 * This matches anime episodes that end with a hash or an episode number, or no quality/resolution.
			 * It is quite messy because there is so much variation out there.
			 */

			// Remove stuff at the end of the filename like hash, quality, source, etc.
			formattedName = formattedName.replaceAll(COMMON_ANIME_FILE_ENDS, "");

			matcher = COMMON_ANIME_EPISODE_NUMBERS_PATTERN.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = "1";
				tvEpisodeNumber = matcher.group(1);

				int showNameIndex = indexOf(COMMON_ANIME_EPISODE_NUMBERS_PATTERN, formattedName);
				if (showNameIndex != -1) {
					movieOrShowName = formattedName.substring(0, showNameIndex);
				}
			} else {
				// This matches a multiple episode anime with a hash at the end of the name
				matcher = COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS_PATTERN.matcher(formattedName);
				if (matcher.find()) {
					tvSeason = "1";
					tvEpisodeNumber = matcher.group(1);

					int showNameIndex = indexOf(COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS_PATTERN, formattedName);
					if (showNameIndex != -1) {
						movieOrShowName = formattedName.substring(0, showNameIndex);
					}
				}
			}

			if (tvEpisodeNumber != null && tvEpisodeNumber.length() > 2 && tvEpisodeNumber.charAt(0) == '0') {
				if (tvEpisodeNumber.contains(" ")) {
					// Restores the decimal point we stripped from the episode number
					tvEpisodeNumber = tvEpisodeNumber.replace(" ", ".");
				} else {
					// Strips a leading zero from a 3+ digit episode number
					tvEpisodeNumber = tvEpisodeNumber.substring(1);
				}
			}

			// Attempt to extract the season number from the series title
			if (movieOrShowName != null && isNotBlank(movieOrShowName)) {
				movieOrShowName = movieOrShowName.trim();
				matcher = SEASON_NUMBER_IN_SERIES_TITLE_PATTERN.matcher(movieOrShowName);
				if (matcher.find()) {
					tvSeason = matcher.group(1);
					movieOrShowName = movieOrShowName.substring(0, movieOrShowName.length() - 3);
				}
			}

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(COMMON_FILE_ENDS_MATCH)) {
			// This is probably a movie that doesn't specify a year
			isMovieWithoutYear = true;
			formattedName = removeFilenameEndMetadata(formattedName);
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.getFormattedName();
			if (result.getEdition() != null) {
				edition = result.getEdition();
			}

			formattedName = convertFormattedNameToTitleCase(formattedName);
		}

		// Remove extra spaces
		formattedName = formattedName.replaceAll("\\s+", " ");
		formattedName = formattedName.trim();
		if (movieOrShowName != null) {
			movieOrShowName = movieOrShowName.trim();
			String substr = movieOrShowName.substring(Math.max(0, movieOrShowName.length() - 2));
			if (" -".equals(substr)) {
				movieOrShowName = movieOrShowName.substring(0, movieOrShowName.length() - 2);
			}
		}

		if (tvSeason != null) {
			// Remove leading 0 from the season if it exists
			if (tvSeason.length() > 1 && tvSeason.startsWith("0")) {
				tvSeason = tvSeason.substring(1);
			}
			if (isEmpty(movieOrShowName)) {
				int showNameIndex = indexOf(SHOW_NAME_INDEX_PATTERN, formattedName);
				if (showNameIndex != -1) {
					movieOrShowName = formattedName.substring(0, showNameIndex);

					matcher = SHOW_NAME_INDEX_PATTERN.matcher(formattedName);
					if (matcher.find()) {
						tvEpisodeName = matcher.group(2).trim();
						if (isEmpty(tvEpisodeName)) {
							tvEpisodeName = null;
						} else {
							if (tvEpisodeName.startsWith("- ")) {
								tvEpisodeName = tvEpisodeName.substring(2);
							}
						}
					}

					if (!isEmpty(tvEpisodeName)) {
						tvEpisodeName = convertFormattedNameToTitleCase(tvEpisodeName);
					}
				} else {
					showNameIndex = indexOf(SHOW_NAME_INDEX_FALLBACK_PATTERN, formattedName);
					if (showNameIndex != -1) {
						movieOrShowName = formattedName.substring(0, showNameIndex);
					}
				}
			}
			if (movieOrShowName != null) {
				movieOrShowName = movieOrShowName.trim();
			}
			int yearIndex = indexOf(Pattern.compile("(?:\\(|\\s)(?:19|20)\\d{2}"), movieOrShowName);
			if (yearIndex > -1) {
				year = formattedName.substring(yearIndex + 1, yearIndex + 5);
				movieOrShowName = formattedName.substring(0, yearIndex);
				movieOrShowName = movieOrShowName.trim();
				movieOrShowName += " (" + year + ")";
			}
		} else {
			if (isMovieWithoutYear) {
				movieOrShowName = formattedName;
			} else {
				int yearIndex = indexOf(Pattern.compile("\\s\\((?:19|20)\\d{2}\\)"), formattedName);
				if (yearIndex > -1) {
					movieOrShowName = formattedName.substring(0, yearIndex);
					year = formattedName.substring(yearIndex + 2, yearIndex + 6);
				}
			}
		}

		// Clean up any unexpected ugliness
		if (movieOrShowName != null) {
			movieOrShowName = movieOrShowName.trim();
			if (movieOrShowName.endsWith(" -")) {
				movieOrShowName = movieOrShowName.substring(0, movieOrShowName.length() - 2);
			}
		}

		// Retain the fact it is a sample clip
		if (isSample) {
			if (edition == null) {
				extraInformation = "";
			} else {
				extraInformation = edition + " ";
			}
			extraInformation += "(Sample)";
		} else {
			extraInformation = edition;
		}

		return new String[] {movieOrShowName, year, extraInformation, tvSeason, tvEpisodeNumber, tvEpisodeName};
	}

	/**
	 * Converts a lower case string to title case.
	 *
	 * It is not very smart right now so it can be expanded to be more reliable.
	 *
	 * @param value the string to convert
	 *
	 * @return the converted string
	 */
	public static String convertLowerCaseStringToTitleCase(String value) {
		value = value.trim();
		StringBuilder convertedValue = new StringBuilder();
		boolean loopedOnce = false;

		for (String word : value.split("\\s+")) {
			if (loopedOnce) {
				switch (word) {
					case "a", "an", "and", "in", "it", "for", "of", "on", "the", "to", "vs" -> convertedValue.append(' ').append(word);
					default -> convertedValue.append(' ').append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
				}
			} else {
				// Always capitalize the first letter of the string
				convertedValue.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
				loopedOnce = true;
			}
		}

		return convertedValue.toString();
	}

	public static int indexOf(Pattern pattern, String s) {
		if (isBlank(s)) {
			return -1;
		}

		Matcher matcher = pattern.matcher(s);
		return matcher.find() ? matcher.start() : -1;
	}

	public static File getFileNameWithAddedExtension(File parent, File f, String ext) {
		File ff = new File(parent, f.getName() + ext);

		if (ff.exists()) {
			return ff;
		}

		return null;
	}

	/**
	 * Returns a new {@link File} instance where the file extension has been
	 * replaced.
	 *
	 * @param file the {@link File} for which to replace the extension.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link File} instance if the constructed
	 *            {@link File} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link File} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link File} instance will be returned.
	 * @return The constructed {@link File} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static File replaceExtension(
		File file,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (file == null) {
			return null;
		}
		return replaceExtension(
			file.getParentFile(),
			file.getName(),
			extension,
			nullIfNonExisting,
			adjustExtensionCase
		);
	}

	/**
	 * Returns a new {@link File} instance where the file extension has been
	 * replaced.
	 *
	 * @param folder the {@link File} instance representing the folder for the
	 *            constructed {@link File}. Use {@code null} or an empty string
	 *            for the current folder.
	 * @param file the {@link File} for which to replace the extension. Only the
	 *            file name will be used, its path will be discarded.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link File} instance if the constructed
	 *            {@link File} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link File} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link File} instance will be returned.
	 * @return The constructed {@link File} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static File replaceExtension(
		File folder,
		File file,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (file == null) {
			return null;
		}
		return replaceExtension(
			folder,
			file.getName(),
			extension,
			nullIfNonExisting,
			adjustExtensionCase
		);
	}

	/**
	 * Returns a new {@link File} instance where the file extension has been
	 * replaced.
	 *
	 * @param folder the {@link File} instance representing the folder for the
	 *            constructed {@link File}. Use {@code null} or an empty string
	 *            for the current folder.
	 * @param fileName the {@link String} for which to replace the extension.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link File} instance if the constructed
	 *            {@link File} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link File} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link File} instance will be returned.
	 * @return The constructed {@link File} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static File replaceExtension(
		File folder,
		String fileName,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (isBlank(fileName)) {
			return null;
		}

		int dot = fileName.lastIndexOf('.');

		String baseFileName;
		if (dot == -1 || getIndexOfLastSeparator(fileName) > dot) {
			baseFileName = fileName;
		} else {
			baseFileName = fileName.substring(0, dot);
		}

		if (isBlank(extension)) {
			File result = new File(folder, baseFileName);
			return !nullIfNonExisting || result.exists() ? result : null;
		}

		File result = new File(folder, baseFileName + "." + extension);
		if (result.exists() || !nullIfNonExisting && !adjustExtensionCase) {
			return result;
		}

		if (!Platform.isWindows() && adjustExtensionCase) {
			File adjustedResult = new File(folder, baseFileName + "." + extension.toLowerCase(Locale.ROOT));
			if (adjustedResult.exists()) {
				return adjustedResult;
			}
			adjustedResult = new File(folder, baseFileName + "." + extension.toUpperCase(Locale.ROOT));
			if (adjustedResult.exists()) {
				return adjustedResult;
			}
		}

		return nullIfNonExisting ? null : result;
	}

	/**
	 * Returns a new {@link Path} instance where the file extension has been
	 * replaced.
	 *
	 * @param file the {@link Path} for which to replace the extension.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link Path} instance if the constructed
	 *            {@link Path} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link Path} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link Path} instance will be returned.
	 * @return The constructed {@link Path} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static Path replaceExtension(
		Path file,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (file == null) {
			return null;
		}
		Path fileName = file.getFileName();
		if (fileName == null) {
			return null;
		}
		return replaceExtension(
			file.getParent(),
			fileName.toString(),
			extension,
			nullIfNonExisting,
			adjustExtensionCase
		);
	}

	/**
	 * Returns a new {@link Path} instance where the file extension has been
	 * replaced.
	 *
	 * @param folder the {@link Path} instance representing the folder for the
	 *            constructed {@link Path}. Use {@code null} for the current
	 *            folder.
	 * @param file the {@link Path} for which to replace the extension. Only the
	 *            file name will be used, its path will be discarded.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link Path} instance if the constructed
	 *            {@link Path} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link Path} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link Path} instance will be returned.
	 * @return The constructed {@link Path} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static Path replaceExtension(
		Path folder,
		Path file,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (file == null) {
			return null;
		}
		Path fileName = file.getFileName();
		if (fileName == null) {
			return null;
		}
		return replaceExtension(
			folder,
			fileName.toString(),
			extension,
			nullIfNonExisting,
			adjustExtensionCase
		);
	}

	/**
	 * Returns a new {@link Path} instance where the file extension has been
	 * replaced.
	 *
	 * @param folder the {@link Path} instance representing the folder for the
	 *            constructed {@link Path}. Use {@code null} for the current
	 *            folder.
	 * @param fileName the {@link String} for which to replace the extension.
	 * @param extension the new file extension.
	 * @param nullIfNonExisting whether or not to return {@code null} or a
	 *            non-existing {@link Path} instance if the constructed
	 *            {@link Path} doesn't exist.
	 * @param adjustExtensionCase whether or not to try upper- and lower-case
	 *            variants of the extension. If {@code true} and the constructed
	 *            {@link Path} doesn't exist with the given case but does exist
	 *            with an either upper or lower case version of the extension,
	 *            the existing {@link Path} instance will be returned.
	 * @return The constructed {@link Path} instance or {@code null} if the
	 *         target file doesn't exist and {@code nullIfNonExisting} is true.
	 */
	public static Path replaceExtension(
		Path folder,
		String fileName,
		String extension,
		boolean nullIfNonExisting,
		boolean adjustExtensionCase
	) {
		if (isBlank(fileName)) {
			return null;
		}

		int point = fileName.lastIndexOf('.');

		String baseFileName;
		if (point == -1) {
			baseFileName = fileName;
		} else {
			baseFileName = fileName.substring(0, point);
		}

		try {
			if (folder == null) {
				folder = Paths.get("");
			}
			if (isBlank(extension)) {
				Path result = folder.resolve(baseFileName);
				return !nullIfNonExisting || Files.exists(result) ? result : null;
			}

			Path result = folder.resolve(baseFileName + "." + extension);
			if (Files.exists(result) || !nullIfNonExisting && !adjustExtensionCase) {
				return result;
			}

			if (!Platform.isWindows() && adjustExtensionCase) {
				Path adjustedResult = folder.resolve(baseFileName + "." + extension.toLowerCase(Locale.ROOT));
				if (Files.exists(adjustedResult)) {
					return adjustedResult;
				}
				adjustedResult = folder.resolve(baseFileName + "." + extension.toUpperCase(Locale.ROOT));
				if (Files.exists(adjustedResult)) {
					return adjustedResult;
				}
			}
			return nullIfNonExisting ? null : result;
		} catch (InvalidPathException e) {
			LOGGER.error(
				"Unexpected error during replaceExtension for folder \"{}\", file \"{}\" and extension \"{}\": {}",
				folder,
				fileName,
				extension,
				e.getMessage()
			);
			LOGGER.trace("", e);
			return null;
		}
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 *
	 * @param file the file for which to detect charset/encoding
	 * @return The match object form the detection process or {@code null} if no
	 *         match was found
	 * @throws IOException
	 */
	@Nonnull
	public static CharsetMatch getFileCharsetMatch(@Nonnull File file) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		CharsetDetector detector = new CharsetDetector();
		detector.setText(in);
		// Get best match only.
		return detector.detect();
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 *
	 * @param file the file for which to detect charset/encoding
	 * @return The detected {@link Charset} or {@code null} if not detected
	 * @throws IOException
	 */
	@Nullable
	public static Charset getFileCharset(@Nullable File file) throws IOException {
		if (file == null) {
			return null;
		}
		CharsetMatch match = getFileCharsetMatch(file);
		try {
			if (Charset.isSupported(match.getName())) {
				LOGGER.debug("Detected charset \"{}\" in file \"{}\"", match.getName(), file.getAbsolutePath());
				return Charset.forName(match.getName());
			}
			LOGGER.debug(
				"Detected charset \"{}\" in file \"{}\", but cannot use it because it's not supported by the Java Virtual Machine",
				match.getName(),
				file.getAbsolutePath()
			);
			return null;
		} catch (IllegalCharsetNameException e) {
			LOGGER.debug("Illegal charset \"{}\" deteceted in file \"{}\"", match.getName(), file.getAbsolutePath());
		}
		LOGGER.debug("Found no matching charset for file \"{}\"", file.getAbsolutePath());
		return null;
	}

	/**
	 * Tries to detect the {@link Charset}/encoding for the specified file.If
	 * no valid {@link Charset} is detected or the confidence of the best match
	 * is below the threshold, {@code null}will be returned.
	 *
	 * @param file the text file whose {@link Charset} to detect.
	 * @param confidenceThreshold
	 * @return The most confidently detected {@link Charset} or {@code null}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static Charset detectCharset(Path file, int confidenceThreshold) throws IOException {
		if (file == null || !Files.exists(file)) {
			return null;
		}
		CharsetDetector detector = new CharsetDetector();
		CharsetMatch[] matches;
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file))) {
			detector.setText(bis);
			detector.enableInputFilter(true);
			matches = detector.detectAll();
		}
		for (CharsetMatch match : matches) {
			if (match.getConfidence() < confidenceThreshold) {
				LOGGER.debug(
					"Detected charset \"{}\" in \"{}\" but not with enough confidence ({} < {})",
					match.getName(),
					file,
					match.getConfidence(),
					confidenceThreshold
				);
				break;
			}
			try {
				if (Charset.isSupported(match.getName())) {
					return Charset.forName(match.getName());
				}
				LOGGER.debug(
					"The detected charset \"{}\" in \"{}\" isn't supported by the JVM - skipping",
					match.getName(),
					file
				);
			} catch (IllegalCharsetNameException e) {
				LOGGER.debug(
					"Detected an illegal charset \"{}\" in \"{}\" - skipping: {}",
					match.getName(),
					file,
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
		}

		if (matches.length == 0) {
			LOGGER.debug("Found no matching charset for \"{}\"", file);
		} else {
			LOGGER.debug(
				"Found {} matching charset{} for \"{}\", but {} both supported by the JVM and {} a high enough confidence ({})",
				matches.length,
				matches.length > 1 ? "s" : "",
				file,
				matches.length > 1 ? "none are" : "it isn't",
				matches.length > 1 ? "have" : "has",
				confidenceThreshold
			);
		}
		return null;
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 *
	 * @param file the {@link File} for which to detect charset/encoding
	 * @return The name of the detected {@link Charset} or {@code null} if not
	 *         detected
	 * @throws IOException
	 */
	@Nullable
	public static String getFileCharsetName(@Nullable File file) throws IOException {
		if (file == null) {
			return null;
		}
		CharsetMatch match = getFileCharsetMatch(file);
		try {
			if (Charset.isSupported(match.getName())) {
				LOGGER.debug("Detected charset \"{}\" in file \"{}\"", match.getName(), file.getAbsolutePath());
				return match.getName().toUpperCase(Locale.ROOT);
			}
			LOGGER.debug(
				"Detected charset \"{}\" in file \"{}\", but cannot use it because it's not supported by the Java Virtual Machine",
				match.getName(),
				file.getAbsolutePath()
			);
			return null;
		} catch (IllegalCharsetNameException e) {
			LOGGER.debug("Illegal charset \"{}\" deteceted in file \"{}\"", match.getName(), file.getAbsolutePath());
		}
		LOGGER.debug("Found no matching charset for file \"{}\"", file.getAbsolutePath());
		return null;
	}

	/**
	 * Tests if the {@link File} is UTF-8 encoded with or without BOM.
	 *
	 * @param file {@link File} to test.
	 * @return {@code true} if {@link File} is UTF-8 encoded with or without
	 *         BOM, {@code false} otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF8(File file) throws IOException {
		return isCharsetUTF8(getFileCharset(file));
	}

	/**
	 * Tests if the {@link Charset} is UTF-8.
	 *
	 * @param charset the {@link Charset} to test.
	 * @return {@code true} if {@link Charset} is UTF-8, {@code false}
	 *         otherwise.
	 */
	public static boolean isCharsetUTF8(Charset charset) {
		return charset != null && charset.equals(StandardCharsets.UTF_8);
	}

	/**
	 * Tests if the charset name is UTF-8.
	 *
	 * @param charsetName the charset name to test.
	 * @return {@code true} if charset name is UTF-8, {@code false} otherwise.
	 */
	public static boolean isCharsetUTF8(String charsetName) {
		return equalsIgnoreCase(charsetName, CHARSET_UTF_8);
	}

	/**
	 * Tests if the {@link File} is UTF-16 encoded.
	 *
	 * @param file the {@link File} to test.
	 * @return {@code true} if {@link File} is UTF-16 encoded, {@code false}
	 *         otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF16(File file) throws IOException {
		return isCharsetUTF16(getFileCharset(file));
	}

	/**
	 * Tests if {@code charset} is {@code UTF-16}.
	 *
	 * @param charset the {@link Charset} to test.
	 * @return {@code true} if {@code charset} is {@code UTF-16}, {@code false}
	 *         otherwise.
	 */
	public static boolean isCharsetUTF16(Charset charset) {
		return
			charset != null &&
			(
				charset.equals(StandardCharsets.UTF_16) ||
				charset.equals(StandardCharsets.UTF_16BE) ||
				charset.equals(StandardCharsets.UTF_16LE)
			);
	}

	/**
	 * Tests if {@code charset} is {@code UTF-16}.
	 *
	 * @param charsetName the charset name to test
	 * @return {@code true} if {@code charsetName} is {@code UTF-16},
	 *         {@code false} otherwise.
	 */
	public static boolean isCharsetUTF16(String charsetName) {
		return (equalsIgnoreCase(charsetName, CHARSET_UTF_16LE) || equalsIgnoreCase(charsetName, CHARSET_UTF_16BE));
	}

	/**
	 * Tests if {@code charsetName} is {@code UTF-32}.
	 *
	 * @param charsetName the charset name to test.
	 * @return {@code true} if {@code charsetName} is {@code UTF-32},
	 *         {@code false} otherwise.
	 */
	public static boolean isCharsetUTF32(String charsetName) {
		return (equalsIgnoreCase(charsetName, CHARSET_UTF_32LE) || equalsIgnoreCase(charsetName, CHARSET_UTF_32BE));
	}

	/**
	 * Converts an {@code UTF-16} input file to an {@code UTF-8} output file.
	 * Does not overwrite an existing output file.
	 *
	 * @param inputFile an {@code UTF-16} {@link File}.
	 * @param outputFile the {@code UTF-8} {@link File} after conversion.
	 * @throws IOException If an IO error occurs during the operation.
	 */
	public static void convertFileFromUtf16ToUtf8(File inputFile, File outputFile) throws IOException {
		Charset charset;
		if (inputFile == null) {
			throw new IllegalArgumentException("inputFile cannot be null");
		}

		try {
			charset = getFileCharset(inputFile);
		} catch (IOException ex) {
			LOGGER.debug("Exception during charset detection.", ex);
			throw new IllegalArgumentException("Can't confirm that inputFile is UTF-16.");
		}

		if (isCharsetUTF16(charset)) {
			if (!outputFile.exists()) {
				/*
				 * This is a strange hack, and I'm not sure if it's needed. I
				 * did it this way to conform to the tests, which dictates that
				 * UTF-16LE should produce UTF-8 without BOM while UTF-16BE
				 * should produce UTF-8 with BOM.
				 *
				 * For some reason creating a FileInputStream with UTF_16
				 * produces an UTF-8 outputfile without BOM, while using
				 * UTF_16LE or UTF_16BE produces an UTF-8 outputfile with BOM.
				 *
				 * @author Nadahar
				 */
				try (BufferedReader reader =
					StandardCharsets.UTF_16LE.equals(charset) ?
						new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_16)) :
						new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), charset))
				) {
					try (BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))
					) {
						int c;

						while ((c = reader.read()) != -1) {
							writer.write(c);
						}
					}
				}
			}
		} else {
			throw new IllegalArgumentException("File is not UTF-16");
		}
	}

	/**
	 * Return a file or folder's permissions.
	 * <p>
	 * This should <b>NOT</b> be used for checking e.g. read permissions before
	 * trying to open a file, because you can't assume that the same is true
	 * when you actually open the file. Other threads or processes could have
	 * locked the file (or changed it's permissions) in the meanwhile. Instead,
	 * use e.g {@link FileNotFoundException} like this:
	 *
	 * <pre>
	 * <code>
	 * } catch (FileNotFoundException e) {
	 *     LOGGER.debug("Can't read xxx {}", e.getMessage());
	 * }
	 * </code>
	 * </pre>
	 *
	 * {@code e.getMessage()} will contain both the full path to the file the
	 * reason it couldn't be read (e.g. no permission).
	 *
	 * @param file the {@link File} to check permissions for.
	 * @return A {@link FilePermissions} object holding the permissions.
	 * @throws FileNotFoundException If {@code file} can't be found/read.
	 * @see {@link #getFilePermissions(String)}
	 */
	public static FilePermissions getFilePermissions(File file) throws FileNotFoundException {
		return new FilePermissions(file);
	}

	/**
	 * Like {@link #getFilePermissions(File)} but returns {@code null} instead
	 * of throwing {@link FileNotFoundException} if the file or folder isn't
	 * found.
	 *
	 * @param file the file to check permissions for.
	 * @return A {@link FilePermissions} object holding the permissions.
	 */
	public static FilePermissions getFilePermissionsNoThrow(File file) {
		try {
			return new FilePermissions(file);
		} catch (FileNotFoundException | IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Return a file or folder's permissions.
	 * <p>
	 * This should <b>NOT</b> be used for checking e.g. read permissions before
	 * trying to open a file, because you can't assume that the same is true
	 * when you actually open the file. Other threads or processes could have
	 * locked the file (or changed it's permissions) in the meanwhile. Instead,
	 * use e.g {@link FileNotFoundException} like this:
	 *
	 * <pre>
	 * <code>
	 * } catch (FileNotFoundException e) {
	 *     LOGGER.debug("Can't read xxx {}", e.getMessage());
	 * }
	 * </code>
	 * </pre>
	 *
	 * {@code e.getMessage()} will contain both the full path to the file the
	 * reason it couldn't be read (e.g. no permission).
	 *
	 * @param path the file or folder name to check permissions for.
	 * @return A {@link FilePermissions} object holding the permissions.
	 * @throws FileNotFoundException If {@code path} can't be found/read.
	 * @see {@link #getFilePermissions(File)}
	 */
	public static FilePermissions getFilePermissions(String path) throws FileNotFoundException {
		if (path != null) {
			return new FilePermissions(new File(path));
		}

		File file = null;
		return new FilePermissions(file);
	}

	/**
	 * Like {@link #getFilePermissions(String)} but returns {@code null} instead
	 * of throwing {@link FileNotFoundException} if the file or folder isn't
	 * found.
	 *
	 * @param path the file or folder name to check permissions for.
	 * @return A {@link FilePermissions} object holding the permissions.
	 */
	public static FilePermissions getFilePermissionsNoThrow(String path) {
		if (path != null) {
			try {
				return new FilePermissions(new File(path));
			} catch (FileNotFoundException | IllegalArgumentException e) {
				return null;
			}
		}

		return null;
	}

	public static boolean isFileRelevant(File f, UmsConfiguration configuration) {
		String fileName = f.getName().toLowerCase();
		return (
			(
				configuration.isArchiveBrowsing() &&
				(
					fileName.endsWith(".zip") ||
					fileName.endsWith(".cbz") ||
					fileName.endsWith(".rar") ||
					fileName.endsWith(".cbr")
				)
			) ||
			fileName.endsWith(".iso") ||
			fileName.endsWith(".img") ||
			fileName.endsWith(".m3u") ||
			fileName.endsWith(".m3u8") ||
			fileName.endsWith(".pls") ||
			fileName.endsWith(".cue")
		);
	}

	public static boolean isFolderRelevant(File f, UmsConfiguration configuration) {
		return isFolderRelevant(f, configuration, Collections.<String>emptySet());
	}

	public static boolean isFolderRelevant(File f, UmsConfiguration configuration, Set<String> ignoreFiles) {
		if (f.isDirectory() && configuration.isHideEmptyFolders()) {
			File[] children = f.listFiles();

			/*
			 * listFiles() returns null if "this abstract pathname does not denote a directory, or if an I/O error occurs".
			 * in this case (since we've already confirmed that it's a directory), this seems to mean the directory is non-readable
			 * https://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
			 */
			if (children == null) {
				LOGGER.warn("Can't list files in non-readable directory: {}", f.getAbsolutePath());
			} else {
				for (File child : children) {
					if (ignoreFiles != null && ignoreFiles.contains(child.getAbsolutePath())) {
						continue;
					}

					if (child.isFile()) {
						if (FormatFactory.getAssociatedFormat(child.getName()) != null || isFileRelevant(child, configuration)) {
							return true;
						}
					} else {
						if (isFolderRelevant(child, configuration, ignoreFiles)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Removes the file extension, fansub group name, and replaces periods
	 * and underscores with spaces.
	 *
	 * @param filename
	 * @return prettified filename
	 */
	public static String basicPrettify(String filename) {
		// Remove file extension
		filename = getFileNameWithoutExtension(filename);

		// Remove possible fansub group name
		filename = removeGroupNameFromBeginning(filename);

		// Replace periods and underscores with spaces
		return  filename.replaceAll("\\.|_", " ");
	}

	public static String renameForSorting(String filename) {
		return renameForSorting(filename, false, null);
	}

	public static String renameForSorting(String filename, boolean isEpisodeWithinTVSeriesFolder, String absolutePath) {
		if (PMS.getConfiguration().isPrettifyFilenames()) {
			filename = getFileNamePrettified(filename, null, false, isEpisodeWithinTVSeriesFolder, absolutePath);
		}

		if (PMS.getConfiguration().isIgnoreTheWordAandThe()) {
			// Remove "a" and "the" from filename
			filename = filename.replaceAll("^(?i)A[ .]|The[ .]", "");

			// Replace multiple whitespaces with space
			filename = filename.replaceAll("\\s{2,}", " ");
		}

		return filename;
	}

	/**
	 * Attempts to detect the {@link Charset} used in the specified {@link File}
	 * and creates a {@link BufferedReader} using that {@link Charset}. If the
	 * {@link Charset} detection fails, the specified default {@link Charset}
	 * will be used.
	 *
	 * @param file the {@link File} to use.
	 * @param defaultCharset the fallback {@link Charset} it automatic detection
	 *            fails. If {@code null}, the JVM default {@link Charset} will
	 *            be used.
	 * @return The resulting {@link BufferedReaderDetectCharsetResult}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static BufferedReaderDetectCharsetResult createBufferedReaderDetectCharset(
		File file,
		Charset defaultCharset
	) throws IOException {
		BufferedReader reader;
		Charset fileCharset = getFileCharset(file);
		if (fileCharset != null) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), fileCharset));
			return new BufferedReaderDetectCharsetResult(reader, fileCharset, true);
		}

		if (defaultCharset == null) {
			defaultCharset = Charset.defaultCharset();
		}

		reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), defaultCharset));
		LOGGER.warn(
			"Could not detect character encoding for file \"{}\"; using the default charset \"{}\"",
			file.getAbsolutePath(),
			defaultCharset
		);

		return new BufferedReaderDetectCharsetResult(reader, defaultCharset, false);
	}

	/**
	 * Checks for valid file name syntax. Path is not allowed.
	 *
	 * @param fileName the file name to be verified
	 * @return whether or not the file name is valid
	 */
	public static boolean isValidFileName(String fileName) {
		if (Platform.isWindows()) {
			if (fileName.matches("^[^\"*:<>?/\\\\]+$")) {
				return true;
			}
		} else if (Platform.isMac()) {
			if (fileName.matches("^[^:/]+$")) {
				return true;
			}
		} else {
			// Assuming POSIX
			if (fileName.matches("^[A-Za-z0-9._][A-Za-z0-9._-]*$")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Appends a path separator of the same type last in the string if it's not
	 * already there.
	 *
	 * @param path the path to be modified.
	 * @return The corrected path or {@code null} if {@code path} is
	 *         {@code null}.
	 */
	public static String appendPathSeparator(String path) {
		if (path == null) {
			return null;
		}

		if (!path.endsWith("\\") && !path.endsWith("/")) {
			if (path.contains("\\")) {
				path += "\\";
			} else if (path.contains("/")) {
				path += "/";
			} else {
				path += File.separator;
			}
		}

		return path;
	}

	/**
	 * Appends a suffix to a filename before the last {@code "."} if there is
	 * one. If not, simply appends the suffix to the filename.
	 *
	 * @param fileName the filename to append to.
	 * @param suffix the suffix to append.
	 * @return The modified filename.
	 */
	@Nonnull
	public static String appendToFileName(@Nonnull String fileName, @Nullable String suffix) {
		if (fileName == null) {
			throw new IllegalArgumentException("fileName cannot be null");
		}

		if (isBlank(suffix)) {
			return fileName;
		}

		int i = fileName.lastIndexOf(".");
		if (i < 0) {
			return fileName + suffix;
		}

		return fileName.substring(0, i) + suffix + fileName.substring(i);
	}

	/**
	 * @return The OS {@code PATH} environment variable as a {@link List} of
	 *         {@link Path}s.
	 */
	@Nonnull
	public static List<Path> getOSPath() {
		List<Path> result = new ArrayList<>();
		String osPath = System.getenv("PATH");
		if (isBlank(osPath)) {
			return result;
		}

		String[] paths = osPath.split(File.pathSeparator);
		for (String path : paths) {
			try {
				Path pathToAdd = Paths.get(path);
				result.add(pathToAdd);
			} catch (InvalidPathException e) {
				LOGGER.debug("Skipping invalid path {}", e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Tries to find the specified relative file that is both executable and
	 * readable using the system {@code PATH} environment variable while
	 * following symbolic links. Returns the first match in the order of the
	 * system {@code PATH} or {@code null} is no match was found.
	 *
	 * @param relativePath the relative {@link Path} describing the file or
	 *            folder to return.
	 * @return The matched {@link Path} or {@code null} if no match was found.
	 * @throws IllegalArgumentException if {@code relativePath} is absolute.
	 */
	@Nullable
	public static Path findExecutableInOSPath(@Nullable Path relativePath) {
		return findInOSPath(relativePath, true, FileFlag.FILE, FileFlag.READ, FileFlag.EXECUTE);
	}

	/**
	 * Tries to find the specified relative file or folder using the system
	 * {@code PATH} environment variable. Returns the first match in the order
	 * of the system {@code PATH} or {@code null} is no match was found.
	 *
	 * @param relativePath the relative {@link Path} describing the file or
	 *            folder to return.
	 * @param followSymlinks whether or not to follow symbolic links (NIO
	 *            default is {@code true}).
	 * @param requiredFlags zero or more {@link FileFlag}s that specify
	 *            permissions or properties that must be met for a file object
	 *            to match. Use for example {@link FileFlag#FILE} to only find
	 *            files or {@link FileFlag#FOLDER} to only find folders.
	 * @return The matched {@link Path} or {@code null} if no match was found.
	 * @throws IllegalArgumentException if {@code relativePath} is absolute.
	 */
	@Nullable
	public static Path findInOSPath(
		@Nullable Path relativePath,
		boolean followSymlinks,
		FileFlag... requiredFlags
	) {
		if (relativePath == null) {
			return null;
		}

		if (relativePath.isAbsolute()) {
			throw new IllegalArgumentException("relativePath must be relative");
		}

		LinkOption[] options = followSymlinks ? new LinkOption[] {} : new LinkOption[] {LinkOption.NOFOLLOW_LINKS};
		List<Path> osPath = new ArrayList<>();
		osPath.add(null);
		osPath.addAll(getOSPath());
		Path result;
		List<String> extensions = new ArrayList<>();
		extensions.add(null);
		if (Platform.isWindows() && getExtension(relativePath) == null) {
			for (String s : WindowsProgramPaths.getWindowsPathExtensions()) {
				if (isNotBlank(s)) {
					extensions.add("." + s);
				}
			}
		}

		for (String extension : extensions) {
			for (Path path : osPath) {
				if (path == null) {
					path = Paths.get("").toAbsolutePath();
				}

				if (extension == null) {
					result = path.resolve(relativePath);
				} else {
					result = path.resolve(relativePath.toString() + extension);
				}

				if (Files.exists(result, options)) {
					if (requiredFlags.length == 0) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Resolved \"{}\" from \"{}\" using OS path", result, relativePath);
						}

						try {
							return result.toRealPath(options);
						} catch (IOException e) {
							LOGGER.warn("Could not get the real path of \"{}\": {}", result, e.getMessage());
							LOGGER.trace("", e);
							return result;
						}
					}

					try {
						FilePermissions permissions = new FilePermissions(result, options);
						Set<FileFlag> flags = permissions.getFlags(requiredFlags);
						if (flags.containsAll(Arrays.asList(requiredFlags))) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Resolved \"{}\" from \"{}\" using OS path", result, relativePath);
							}

							try {
								return result.toRealPath(options);
							} catch (IOException e) {
								LOGGER.warn("Could not get the real path of \"{}\": {}", result, e.getMessage());
								LOGGER.trace("", e);
								return result;
							}
						}
					} catch (FileNotFoundException e) {
						//will continue;
					}
				}
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Failed to resolve \"{}\" using OS path", relativePath);
		}

		return null;
	}

	/**
	 * This class holds the results from
	 * {@link FileUtil#createBufferedReaderDetectCharset}.
	 *
	 * @author Nadahar
	 */
	public static class BufferedReaderDetectCharsetResult implements Closeable {
		private final BufferedReader reader;
		private final Charset charset;
		private final boolean successfulDetection;

		/**
		 * Creates a new instance with the given parameters.
		 *
		 * @param reader the {@link BufferedReader}.
		 * @param charset the {@link Charset}.
		 * @param successfulDetection {@code true} is {@link Charset} detection
		 *            was successful, {@code false} otherwise.
		 */
		public BufferedReaderDetectCharsetResult(BufferedReader reader, Charset charset, boolean successfulDetection) {
			this.reader = reader;
			this.charset = charset;
			this.successfulDetection = successfulDetection;
		}

		/**
		 * @return The {@link BufferedReader}.
		 */
		public BufferedReader getBufferedReader() {
			return reader;
		}

		/**
		 * @return The {@link Charset} used for the {@link BufferedReader}.
		 */
		public Charset getCharset() {
			return charset;
		}

		/**
		 * @return {@code true} if {@link Charset} detection was successful,
		 *         {@code false} if the default was used..
		 */
		public boolean isSuccessfulDetection() {
			return successfulDetection;
		}

		@Override
		public void close() throws IOException {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * This reduces the incoming title to a lowercase, alphanumeric string
	 * for searching in order to prevent titles like "Word of the Word" and
	 * "Word Of The Word!" from being seen as different shows.
	 *
	 * @param title
	 * @return
	 */
	public static String getSimplifiedShowName(String title) {
		if (title == null) {
			return null;
		}

		return title.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	/**
	 * Check if the provided {@code filename} string can be a directory
	 * by checking if the string contains extension.
	 *
	 * @param filename the string represented the directory
	 * @return {@code true} if the string doesn't contain the extension
	 * {@code false} otherwise.
	 */
	public static boolean isDirectory(String filename) {
		return FileUtil.getExtension(filename) == null;
	}

	/**
	 * Check if the {@code file} is a symbolic link.
	 *
	 * @param file the file to check
	 * @return {@code true} if the file is a symbolic link
	 * {@code false} otherwise.
	 */
	public static boolean isSymbolicLink(File file) {
		return Files.isSymbolicLink(file.toPath());
	}

	/**
	 * Return the file from the real path of the provided {@code file}
	 * that is a symbolic link.
	 *
	 * @param file the symbolic link file
	 * @return a File from the <em>real</em> path of the symbolic link file
	 */
	public static File getRealFile(File file) {
		Path target = file.toPath();
		while (Files.isSymbolicLink(target)) {
			try {
				target = target.toRealPath();
			} catch (IOException ex) {
				return target.toFile();
			}
		}
		return target.toFile();
	}

	/**
	 * Whether the file is locked. Useful for checking whether filesystem
	 * operations are in progress.
	 *
	 * @param file
	 * @return whether the file is locked
	 */
	public static final boolean isLocked(File file) {
		try (RandomAccessFile srcFile = new RandomAccessFile(file, "rw")) {
			return false;
		} catch (Exception e) {
			return true;
		}
	}
}
