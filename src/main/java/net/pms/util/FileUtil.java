package net.pms.util;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.sun.jna.Platform;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.PMS;
import static net.pms.PMS.getConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.formats.FormatFactory;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.StringUtil.LetterCase;
import static net.pms.util.Constants.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.WordUtils;
import static org.apache.commons.lang3.StringUtils.*;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
	private static final ReentrantLock subtitleCacheLock = new ReentrantLock();
	private static final Map<File, File[]> subtitleCache = new HashMap<>();
	private static final int S_ISVTX = 512; // Unix sticky bit mask

	// Signal an invalid parameter in getFileLocation() without raising an exception or returning null
	private static final String DEFAULT_BASENAME = "NO_DEFAULT_BASENAME_SUPPLIED.conf";

	// This class is not instantiable
	private FileUtil() { }

	/**
	 * A helper class used by {@link #getFileLocation(String, String, String)}
	 * which provides access to a file's absolute path and that of its directory.
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
	 * Returns a {@link FileLocation} object which provides access to the directory
	 * and file paths of the specified file as normalised, absolute paths.
	 *
	 * This determines the directory and file path of a file according to the rules
	 * outlined here: http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&amp;t=3507&amp;p=49895#p49895
	 *
	 * @param customPath an optional user-defined path for the resource
	 * @param defaultDirectory a default directory path used if no custom path is provided
	 * @param defaultBasename a default filename used if a) no custom path is provided
	 *                        or b) the custom path is a directory
	 * @return a {@link FileLocation} object providing access to the file's directory and file paths
	 * @since 1.90.0
	 */
	// this is called from a static initialiser, where errors aren't clearly reported,
	// so do everything possible to return a valid reponse, even if the parameters
	// aren't sane
	static public FileLocation getFileLocation(
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

	public final static class InvalidFileSystemException extends Exception {

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
	 * A simple type holding mount point information for Unix file systems.
	 *
	 * @author Nadahar
	 */
	public static final class UnixMountPoint {
		public String device;
		public String folder;

		@Override
	    public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (this == obj) {
				return true;
			}
	    	if (!(obj instanceof UnixMountPoint)) {
	    		return false;
	    	}
	    	return
	    		this.device.equals(((UnixMountPoint) obj).device) &&
	    		this.folder.equals(((UnixMountPoint) obj).folder);
	    }

		@Override
		public int hashCode() {
			return device.hashCode() + folder.hashCode();
		}

		@Override
		public String toString() {
			return String.format("Device: \"%s\", folder: \"%s\"", device, folder);
		}
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
		return getExtension(substringBefore(u, "?"));
	}

	/**
	 * Returns the file extension from the specified {@link File} or
	 * {@code null} if it has no extension.
	 *
	 * @param file the {@link File} from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	public static String getExtension(File file) {
		return getExtension(file, null, null);
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
	public static String getExtension(File file, LetterCase convertTo, Locale locale) {
		if (file == null || file.getName() == null) {
			return null;
		}
		return getExtension(file.getName(), convertTo, locale);
	}

	/**
	 * Returns the file extension from {@code fileName} or {@code null} if
	 * {@code fileName} has no extension.
	 *
	 * @param fileName the file name from which to extract the extension.
	 * @return The extracted extension or {@code null}.
	 */
	public static String getExtension(String fileName) {
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
	public static String getExtension(String fileName, LetterCase convertTo, Locale locale) {
		if (fileName == null) {
			return null;
		}

		int point = fileName.lastIndexOf('.');
		if (point == -1) {
			return null;
		}
		if (convertTo != null && locale == null) {
			locale = Locale.ROOT;
		}

		String extension = fileName.substring(point + 1);
		if (convertTo == LetterCase.UPPER) {
			return extension.toUpperCase(locale);
		}
		if (convertTo == LetterCase.LOWER) {
			return extension.toLowerCase(locale);
		}
		return extension;
	}

	public static String getFileNameWithoutExtension(String f) {
		int point = f.lastIndexOf('.');

		if (point == -1) {
			point = f.length();
		}

		return f.substring(0, point);
	}

	private static final class FormattedNameAndEdition {
		public String formattedName;
		public String edition;

		public FormattedNameAndEdition(String formattedName, String edition) {
			this.formattedName = formattedName;
			this.edition = edition;
		}
	}

	/**
	 * Remove and save edition information to be added later
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
	 * Capitalize the first letter of each word if the string contains no capital letters
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
	 * Capitalize the first letter of each word if the string contains no capital letters
	 */
	private static String convertFormattedNameToTitleCase(String formattedName) {
		if (formattedName.equals(formattedName.toLowerCase())) {
			formattedName = convertLowerCaseStringToTitleCase(formattedName);
		}
		return formattedName;
	}

	/**
	 * Remove group name from the beginning of the filename
	 *
	 * @param fileNameWithoutExtension
	 */
	private static String removeGroupNameFromBeginning(String formattedName) {
		if (!"".equals(formattedName) && formattedName.startsWith("[")) {
			Pattern pattern = Pattern.compile("^\\[[^\\]]{0,20}\\][^\\w]*(\\w.*?)\\s*$");
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
	 * Remove stuff at the end of the filename like release group, quality, source, etc.
	 */
	private static String removeFilenameEndMetadata(String formattedName) {
		formattedName = formattedName.replaceAll(COMMON_FILE_ENDS_CASE_SENSITIVE, "");
		formattedName = formattedName.replaceAll("(?i)" + COMMON_FILE_ENDS, "");
		return formattedName;
	}

	/**
	 * Strings that only occur after all useful information.
	 * When we encounter one of these strings, the string and everything after
	 * them will be removed.
	 */
	private static final String COMMON_FILE_ENDS = "\\sAC3.*|\\sREPACK.*|\\s480p.*|\\s720p.*|\\sm-720p.*|\\s900p.*|\\s1080p.*|\\s2160p.*|\\sWEB-DL.*|\\sHDTV.*|\\sDSR.*|\\sPDTV.*|\\sWS.*|\\sHQ.*|\\sDVDRip.*|\\sTVRiP.*|\\sBDRip.*|\\sBRRip.*|\\sWEBRip.*|\\sBluRay.*|\\sBlu-ray.*|\\sSUBBED.*|\\sx264.*|\\sDual\\sAudio.*|\\sHSBS.*|\\sH-SBS.*|\\sRERiP.*|\\sDIRFIX.*|\\sREADNFO.*|\\s60FPS.*";
	private static final String COMMON_FILE_ENDS_MATCH = ".*\\sAC3.*|.*\\sREPACK.*|.*\\s480p.*|.*\\s720p.*|.*\\sm-720p.*|.*\\s900p.*|.*\\s1080p.*|.*\\s2160p.*|.*\\sWEB-DL.*|.*\\sHDTV.*|.*\\sDSR.*|.*\\sPDTV.*|.*\\sWS.*|.*\\sHQ.*|.*\\sDVDRip.*|.*\\sTVRiP.*|.*\\sBDRip.*|.*\\sBRRip.*|.*\\sWEBRip.*|.*\\sBluRay.*|.*\\sBlu-ray.*|.*\\sSUBBED.*|.*\\sx264.*|.*\\sDual\\sAudio.*|.*\\sHSBS.*|.*\\sH-SBS.*|.*\\sRERiP.*|.*\\sDIRFIX.*|.*\\sREADNFO.*|.*\\s60FPS.*";

	/**
	 * Same as above, but they are common words so we reduce the chances of a
	 * false-positive by being case-sensitive.
	 */
	private static final String COMMON_FILE_ENDS_CASE_SENSITIVE = "\\sPROPER\\s.*|\\siNTERNAL\\s.*|\\sLIMITED\\s.*|\\sLiMiTED\\s.*|\\sFESTiVAL\\s.*|\\sNORDIC\\s.*|\\sREAL\\s.*|\\sSUBBED\\s.*|\\sRETAIL\\s.*|\\sEXTENDED\\s.*|\\sNEWEDIT\\s.*|\\sWEB\\s.*";

	/**
	 * Editions to be added to the end of the prettified name
	 */
	private static final String COMMON_FILE_EDITIONS = "(?i)(?!\\()(Special\\sEdition|Unrated|Final\\sCut|Remastered|Extended\\sCut|IMAX\\sEdition|Uncensored|Directors\\sCut|Uncut)(?!\\))";
	private static final Pattern COMMON_FILE_EDITIONS_PATTERN = Pattern.compile(COMMON_FILE_EDITIONS);

	private static final String COMMON_ANIME_EPISODE_NUMBERS = "(?:[\\s']|S1\\sEP)(\\d\\d)(?:[\\s']|v\\d)";
	private static final Pattern COMMON_ANIME_EPISODE_NUMBERS_PATTERN = Pattern.compile(COMMON_ANIME_EPISODE_NUMBERS);

	private static final String COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS = "(?:[\\s']|S1\\sEP)(\\d\\d-\\d\\d)(?:[\\s']|v\\d)";
	private static final Pattern COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS_PATTERN = Pattern.compile(COMMON_ANIME_MULTIPLE_EPISODES_NUMBERS);

	public static String getFileNamePrettified(String f) {
		return getFileNamePrettified(f, null, null);
	}

	public static String getFileNamePrettified(String f, File file) {
		return getFileNamePrettified(f, file, null);
	}

	/**
	 * Returns the filename after being "prettified", which involves
	 * attempting to strip away certain things like information about the
	 * quality, resolution, codecs, release groups, fansubbers, etc.,
	 * replacing periods with spaces, and various other things to produce a
	 * more "pretty" and standardized filename.
	 *
	 * @param f The filename
	 * @param file The file to possibly be used by the InfoDb
	 * @param media The DLNAMediaInfo for database access
	 *
	 * @return The prettified filename
	 */
	public static String getFileNamePrettified(String f, File file, DLNAMediaInfo media) {
		String formattedName = null;

		String title;
		String year;
		String extraInformation;
		String tvSeason;
		String tvEpisodeNumber;
		String tvEpisodeName;
		boolean isTVEpisode = false;

		// Populate the variables from the data if we can, otherwise from the filename
		if (media != null && getConfiguration().getUseCache() && StringUtils.isNotBlank(media.getMovieOrShowName())) {
			title = media.getMovieOrShowName();

			year             = StringUtils.isNotBlank(media.getYear())             ? media.getYear()             : "";
			extraInformation = StringUtils.isNotBlank(media.getExtraInformation()) ? media.getExtraInformation() : "";
			tvSeason         = StringUtils.isNotBlank(media.getTVSeason())         ? media.getTVSeason()         : "";
			tvEpisodeNumber  = StringUtils.isNotBlank(media.getTVEpisodeNumber())  ? media.getTVEpisodeNumber()  : "";
			tvEpisodeName    = StringUtils.isNotBlank(media.getTVEpisodeName())    ? media.getTVEpisodeName()    : "";
			isTVEpisode      = StringUtils.isNotBlank(media.getTVSeason());
		} else {
			String[] metadataFromFilename = getFileNameMetadata(f);

			title            = StringUtils.isNotBlank(metadataFromFilename[0]) ? metadataFromFilename[0] : "";
			year             = StringUtils.isNotBlank(metadataFromFilename[1]) ? metadataFromFilename[1] : "";
			extraInformation = StringUtils.isNotBlank(metadataFromFilename[2]) ? metadataFromFilename[2] : "";
			tvSeason         = StringUtils.isNotBlank(metadataFromFilename[3]) ? metadataFromFilename[3] : "";
			tvEpisodeNumber  = StringUtils.isNotBlank(metadataFromFilename[4]) ? metadataFromFilename[4] : "";
			tvEpisodeName    = StringUtils.isNotBlank(metadataFromFilename[5]) ? metadataFromFilename[5] : "";

			if (StringUtils.isNotBlank(tvSeason)) {
				isTVEpisode = true;
			}
		}

		if (StringUtils.isBlank(title)) {
			return basicPrettify(f);
		}

		// Build the prettified filename from the metadata
		if (isTVEpisode) {
			// Make sure the episode number has a leading zero
			if (tvEpisodeNumber.length() == 1) {
				tvEpisodeNumber = "0" + tvEpisodeNumber;
			}

			// If the season is a year, anticipate a "/" for a date
			if (StringUtils.isNotBlank(tvSeason) && StringUtils.isNotBlank(tvEpisodeNumber)) {
				if (tvSeason.matches("(19|20)\\d{2}")) {
					tvSeason += "/";
				}
				formattedName = title + " - " + tvSeason + tvEpisodeNumber;
			}

			if (isNotBlank(tvEpisodeName)) {
				formattedName += " - " + tvEpisodeName;
			}
		} else {
			formattedName = title;
			if (StringUtils.isNotBlank(year)) {
				formattedName += " (" + year + ")";
			}
		}

		if (StringUtils.isNotBlank(extraInformation)) {
			formattedName += " " + extraInformation;
		}

		return formattedName;
	}

	/**
	 * Returns metadata from the filename which we will use to check the
	 * validity of online lookups.
	 *
	 * @param filename The filename to extract metadata from
	 *
	 * @return The metadata
	 */
	public static String[] getFileNameMetadata(String filename) {
		if (filename == null) {
			return new String[] { null, null, null, null, null, null };
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
		String extraInformation = null;

		Pattern pattern;
		Matcher matcher;

		formattedName = basicPrettify(filename);

		if (formattedName.toLowerCase(Locale.ENGLISH).endsWith("sample")) {
			isSample = true;
		}

		if (formattedName.matches(".*[sS]\\d\\d[eE]\\d\\d([eE]|-[eE])\\d\\d.*")) {
			// This matches scene and most p2p TV episodes within the first 9 seasons that are more than one episode
			pattern = Pattern.compile("[sS](\\d\\d)[eE](\\d\\d)(?:[eE]|-[eE])(\\d\\d)");
			matcher = pattern.matcher(formattedName);

			if (matcher.find()) {
				tvSeason = matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
				tvEpisodeNumber += "-" + matcher.group(3);
			}

			// Rename the season/episode numbers. For example, "S01E01" changes to " - 101"
			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\sS(\\d\\d)E(\\d)(\\d)([eE]|-[eE])(\\d)(\\d)\\s", " - $1$2$3-$5$6 - ");
			formattedName = formattedName.replaceAll("(?i)\\sS(\\d\\d)E(\\d)(\\d)([eE]|-[eE])(\\d)(\\d)", " - $1$2$3-$5$6");
			formattedName = formattedName.replaceAll("\\sS(\\d\\d)E(\\d)(\\d)([eE]|-[eE])(\\d)(\\d)", " - $1$2$3-$5$6");
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*[sS]\\d\\d[eE]\\d\\d.*")) {
			// This matches scene and most p2p TV episodes within the first 9 seasons
			pattern = Pattern.compile("[sS](\\d\\d)[eE](\\d\\d)");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			// Rename the season/episode numbers. For example, "S01E01" changes to " - 101"
			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\sS(\\d\\d)E(\\d)(\\d)\\s", " - $1$2$3 - ");
			formattedName = formattedName.replaceAll("(?i)\\sS(\\d\\d)E(\\d)(\\d)", " - $1$2$3");
			formattedName = formattedName.replaceAll("\\sS(\\d\\d)E(\\d)(\\d)", " - $1$2$3");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s\\d[xX]\\d\\d.*")) {
			// This matches older scene and most p2p TV episodes within the first 9 seasons
			pattern = Pattern.compile("\\s(\\d)[xX](\\d\\d)");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = "0" + matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			// Rename the season/episode numbers. For example, "1x01" changes to " - 101"
			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s(\\d)x(\\d)(\\d)\\s", " - $1$2$3 - ");
			formattedName = formattedName.replaceAll("(?i)\\s(\\d)x(\\d)(\\d)", " - $1$2$3");
			formattedName = formattedName.replaceAll("\\s(\\d)x(\\d)(\\d)", " - $1$2$3");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s\\d\\d[xX]\\d\\d.*")) {
			// This matches older scene and most p2p TV episodes after the first 9 seasons
			pattern = Pattern.compile("\\s(\\d\\d)[xX](\\d\\d)");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
			}

			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			// Rename the season/episode numbers. For example, "12x01" changes to " - 1201"
			// Then strip the end of the episode if it does not have the episode name in the title
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s(\\d\\d)x(\\d)(\\d)\\s", " - $1$2$3 - ");
			formattedName = formattedName.replaceAll("(?i)\\s(\\d\\d)x(\\d)(\\d)", " - $1$2$3");
			formattedName = formattedName.replaceAll("\\s(\\d\\d)x(\\d)(\\d)", " - $1$2$3");
			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s(19|20)\\d\\d\\s[0-1]\\d\\s[0-3]\\d\\s.*")) {
			// This matches scene and most p2p TV episodes that release several times per week
			pattern = Pattern.compile("\\s((?:19|20)\\d\\d)\\s([0-1]\\d)\\s([0-3]\\d)\\s");
			matcher = pattern.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = matcher.group(1);
				tvEpisodeNumber = matcher.group(2);
				tvEpisodeNumber += "/" + matcher.group(3);
			}

			// Rename the date. For example, "2013.03.18" changes to " - 2013/03/18"
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS_CASE_SENSITIVE + ")", "");
			formattedName = formattedName.replaceAll("(" + COMMON_FILE_ENDS + ")", "");
			formattedName = formattedName.replaceAll("(?i)\\s(19|20)(\\d\\d)\\s([0-1]\\d)\\s([0-3]\\d)\\s", " - $1$2/$3/$4 - ");
			formattedName = formattedName.replaceAll("(?i)\\s(19|20)(\\d\\d)\\s([0-1]\\d)\\s([0-3]\\d)", " - $1$2/$3/$4");
			formattedName = formattedName.replaceAll("\\s(19|20)(\\d\\d)\\s([0-1]\\d)\\s([0-3]\\d)", " - $1$2/$3/$4");
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			formattedName = removeFilenameEndMetadata(formattedName);
			formattedName = convertFormattedNameToTitleCaseParts(formattedName);
		} else if (formattedName.matches(".*\\s(19|20)\\d\\d\\s.*")) {
			// This matches scene and most p2p movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("\\s(19|20)(\\d\\d)", " ($1$2)");
			formattedName = removeFilenameEndMetadata(formattedName);
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
			}

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\[(19|20)\\d\\d\\].*")) {
			// This matches rarer types of movies

			// Rename the year. For example, "2013" changes to " (2013)"
			formattedName = formattedName.replaceAll("(?i)\\[(19|20)(\\d\\d)\\].*", " ($1$2)");
			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\((19|20)\\d\\d\\).*")) {
			// This matches rarer types of movies
			formattedName = removeFilenameEndMetadata(formattedName);

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\[[0-9a-zA-Z]{8}\\]$")) {
			// This matches a single episode anime with a hash at the end of the name
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

			// Remove stuff at the end of the filename like hash, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)\\s\\(1280x720.*|\\s\\(1920x1080.*|\\s\\(720x400.*|\\[720p.*|\\[1080p.*|\\[480p.*|\\s\\(BD.*|\\s\\[Blu-Ray.*|\\s\\[DVD.*|\\.DVD.*|\\[[0-9a-zA-Z]{8}\\]$|\\[h264.*|R1DVD.*|\\[BD.*", "");

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(".*\\[BD\\].*|.*\\[720p\\].*|.*\\[1080p\\].*|.*\\[480p\\].*|.*\\[Blu-Ray.*|.*\\[h264.*")) {
			// This matches anime without a hash in the name
			matcher = COMMON_ANIME_EPISODE_NUMBERS_PATTERN.matcher(formattedName);
			if (matcher.find()) {
				tvSeason = "1";
				tvEpisodeNumber = matcher.group(1);

				int showNameIndex = indexOf(COMMON_ANIME_EPISODE_NUMBERS_PATTERN, formattedName);
				if (showNameIndex != -1) {
					movieOrShowName = formattedName.substring(0, showNameIndex);
				}
			}

			// Remove stuff at the end of the filename like hash, quality, source, etc.
			formattedName = formattedName.replaceAll("(?i)\\[BD\\].*|\\[720p.*|\\[1080p.*|\\[480p.*|\\[Blu-Ray.*|\\[h264.*", "");

			formattedName = convertFormattedNameToTitleCase(formattedName);
		} else if (formattedName.matches(COMMON_FILE_ENDS_MATCH)) {
			// This is probably a movie that doesn't specify a year
			isMovieWithoutYear = true;
			formattedName = removeFilenameEndMetadata(formattedName);
			FormattedNameAndEdition result = removeAndSaveEditionToBeAddedLater(formattedName);
			formattedName = result.formattedName;
			if (result.edition != null) {
				edition = result.edition;
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
			tvSeason = StringUtils.stripStart(tvSeason, "0");

			pattern = Pattern.compile("(?i) - (\\d{2}|\\d{4}|\\d{4}/\\d{2}/\\d{2}) - (.*)");
			int showNameIndex = indexOf(pattern, formattedName);
			if (StringUtils.isEmpty(movieOrShowName)) {
				if (showNameIndex != -1) {
					movieOrShowName = formattedName.substring(0, showNameIndex);

					matcher = pattern.matcher(formattedName);
					if (matcher.find()) {
						tvEpisodeName = matcher.group(2).trim();
						if (StringUtils.isEmpty(tvEpisodeName)) {
							tvEpisodeName = null;
						}
					}
				} else {
					showNameIndex = indexOf(Pattern.compile("(?i) - (\\d{2}|\\d{4}|\\d{4}/\\d{2}/\\d{2})"), formattedName);
					if (showNameIndex != -1) {
						movieOrShowName = formattedName.substring(0, showNameIndex);
					}
				}
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

		return new String[] { movieOrShowName, year, extraInformation, tvSeason, tvEpisodeNumber, tvEpisodeName };
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
		String convertedValue = "";
		boolean loopedOnce = false;

		for (String word : value.split("\\s+")) {
			if (loopedOnce) {
				switch (word) {
					case "a":
					case "an":
					case "and":
					case "in":
					case "it":
					case "for":
					case "of":
					case "on":
					case "the":
					case "to":
					case "vs":
						convertedValue += ' ' + word;
						break;
					default:
						convertedValue += ' ' + word.substring(0, 1).toUpperCase() + word.substring(1);
				}
			} else {
				// Always capitalize the first letter of the string
				convertedValue += word.substring(0, 1).toUpperCase() + word.substring(1);
				loopedOnce = true;
			}
		}

		return convertedValue;
	}

	public static int indexOf(Pattern pattern, String s) {
		Matcher matcher = pattern.matcher(s);
		return matcher.find() ? matcher.start() : -1;
	}

	/**
	 * @deprecated Use {@link #replaceExtension} instead.
	 */
	@Deprecated
	public static File getFileNameWithNewExtension(File parent, File file, String ext) {
		return replaceExtension(parent, file, ext, true, true);
	}

	/**
	 * @deprecated Use {@link #replaceExtension} instead.
	 */
	@Deprecated
	public static File getFileNameWitNewExtension(File parent, File f, String ext) {
		return replaceExtension(parent, f, ext, true, true);
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

	/**
	 * @deprecated Use {@link #replaceExtension} instead.
	 */
	@Deprecated
	public static File isFileExists(String f, String ext) {
		return replaceExtension(new File(f), ext, true, true);
	}

	/**
	 * @deprecated Use {@link #replaceExtension} instead.
	 */
	@Deprecated
	public static File isFileExists(File f, String ext) {
		return replaceExtension(f, ext, true, true);
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

		int point = fileName.lastIndexOf('.');

		String baseFileName;
		if (point == -1) {
			baseFileName = fileName;
		} else {
			baseFileName = fileName.substring(0, point);
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
	 * @deprecated Use {@link #isSubtitlesExists(File file, DLNAMediaInfo media)} instead.
	 */
	@Deprecated
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media) {
		return isSubtitlesExists(file, media);
	}

	public static boolean isSubtitlesExists(File file, DLNAMediaInfo media) {
		return isSubtitlesExists(file, media, true);
	}

	/**
	 * @deprecated Use {@link #isSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache)} instead.
	 */
	@Deprecated
	public static boolean doesSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
		return isSubtitlesExists(file, media, usecache);
	}

	public static boolean isSubtitlesExists(File file, DLNAMediaInfo media, boolean usecache) {
		if (media != null && media.isExternalSubsParsed()) {
			return media.isExternalSubsExist();
		}

		boolean found = false;
		if (file.exists()) {
			found = browseFolderForSubtitles(file.getAbsoluteFile().getParentFile(), file, media, usecache);
		}
		String alternate = PMS.getConfiguration().getAlternateSubtitlesFolder();

		if (isNotBlank(alternate)) { // https://code.google.com/p/ps3mediaserver/issues/detail?id=737#c5
			File subFolder = new File(alternate);

			if (!subFolder.isAbsolute()) {
				subFolder = new File(file.getParent(), alternate);
				try {
					subFolder = subFolder.getCanonicalFile();
				} catch (IOException e) {
					LOGGER.warn("Could not resolve alternative subtitles folder: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			}

			if (subFolder.exists()) {
				found = browseFolderForSubtitles(subFolder, file, media, usecache) || found;
			}
		}

		if (media != null) {
			media.setExternalSubsExist(found);
			media.setExternalSubsParsed(true);
		}

		return found;
	}

	private static boolean browseFolderForSubtitles(File subFolder, File file, DLNAMediaInfo media, final boolean useCache) {
		boolean found = false;
		final Set<String> supported = SubtitleType.getSupportedFileExtensions();

		File[] allSubs = null;
		// TODO This caching scheme is very restrictive locking the whole cache
		// while populating a single folder. A more effective solution should
		// be implemented.
		subtitleCacheLock.lock();
		try {
			if (useCache) {
				allSubs = subtitleCache.get(subFolder);
			}

			if (allSubs == null) {
				allSubs = subFolder.listFiles(
					new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							String ext = FilenameUtils.getExtension(name).toLowerCase();
							if ("sub".equals(ext)) {
								// Avoid microdvd/vobsub confusion by ignoring sub+idx pairs here since
								// they'll come in unambiguously as vobsub via the idx file anyway
								return replaceExtension(new File(dir, name), "idx", true, true) == null;
							}
							return supported.contains(ext);
						}
					}
				);

				if (allSubs != null) {
					subtitleCache.put(subFolder, allSubs);
				}
			}
		} finally {
			subtitleCacheLock.unlock();
		}

		String fileName = getFileNameWithoutExtension(file.getName()).toLowerCase();
		if (allSubs != null) {
			for (File f : allSubs) {
				if (f.isFile() && !f.isHidden()) {
					String fName = f.getName().toLowerCase();
					for (String ext : supported) {
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
											sub.setExternalFile(f, null);
										} catch (FileNotFoundException ex) {
											LOGGER.warn("File not found during external subtitles scan: {}", ex.getMessage());
											LOGGER.trace("", ex);
										}

										exists = true;
									}
								}
							}

							if (!exists) {
								String forcedLang = null;
								DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
								sub.setId(100 + (media == null ? 0 : media.getSubtitleTracksList().size())); // fake id, not used
								if (code.length() == 0 || !Iso639.codeIsValid(code)) {
									sub.setLang(DLNAMediaSubtitle.UND);
									sub.setType(SubtitleType.valueOfFileExtension(ext));
									if (code.length() > 0) {
										sub.setSubtitlesTrackTitleFromMetadata(code);
										if (sub.getSubtitlesTrackTitleFromMetadata().contains("-")) {
											String flavorLang = sub.getSubtitlesTrackTitleFromMetadata().substring(0, sub.getSubtitlesTrackTitleFromMetadata().indexOf('-'));
											String flavorTitle = sub.getSubtitlesTrackTitleFromMetadata().substring(sub.getSubtitlesTrackTitleFromMetadata().indexOf('-') + 1);
											if (Iso639.codeIsValid(flavorLang)) {
												sub.setLang(flavorLang);
												sub.setSubtitlesTrackTitleFromMetadata(flavorTitle);
												forcedLang = flavorLang;
											}
										}
									}
								} else {
									sub.setLang(code);
									sub.setType(SubtitleType.valueOfFileExtension(ext));
									forcedLang = code;
								}

								try {
									sub.setExternalFile(f, forcedLang);
								} catch (FileNotFoundException ex) {
									LOGGER.warn("File not found during external subtitles scan: {}", ex.getMessage());
									LOGGER.trace("", ex);
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
	 * @param file the file for which to detect charset/encoding
	 * @return The match object form the detection process or <code>null</code> if no match was found
	 * @throws IOException
	 */
	public static CharsetMatch getFileCharsetMatch(File file) throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		CharsetDetector detector = new CharsetDetector();
		detector.setText(in);
		// Results are sorted on descending confidence, so we're only after the first one.
		return detector.detectAll()[0];
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 *
	 * @param file the file for which to detect charset/encoding
	 * @return The detected <code>Charset</code> or <code>null</code> if not detected
	 * @throws IOException
	 */
	public static Charset getFileCharset(File file) throws IOException {
		CharsetMatch match = getFileCharsetMatch(file);
		if (match != null) {
			try {
				if (Charset.isSupported(match.getName())) {
					LOGGER.debug("Detected charset \"{}\" in file {}", match.getName(), file.getAbsolutePath());
					return Charset.forName(match.getName());
				}
				LOGGER.debug(
					"Detected charset \"{}\" in file {}, but cannot use it because it's not supported by the Java Virual Machine",
					match.getName(),
					file.getAbsolutePath()
				);
				return null;
			} catch (IllegalCharsetNameException e) {
				LOGGER.debug("Illegal charset deteceted \"{}\" in file {}", match.getName(), file.getAbsolutePath());
			}
		}
		LOGGER.debug("Found no matching charset for file {}", file.getAbsolutePath());
		return null;
	}

	/**
	 * Detects charset/encoding for given file. Not 100% accurate for
	 * non-Unicode files.
	 *
	 * @param file the file for which to detect charset/encoding
	 * @return The name of the detected charset or <code>null</code> if not detected
	 * @throws IOException
	 */
	public static String getFileCharsetName(File file) throws IOException {
		CharsetMatch match = getFileCharsetMatch(file);
		if (match != null) {
			LOGGER.debug("Detected charset \"{}\" in file {}", match.getName(), file.getAbsolutePath());
			return match.getName().toUpperCase(PMS.getLocale());
		}
		LOGGER.debug("Found no matching charset for file {}", file.getAbsolutePath());
		return null;
	}

	/**
	 * Tests if file is UTF-8 encoded with or without BOM.
	 *
	 * @param file File to test
	 * @return True if file is UTF-8 encoded with or without BOM, false otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF8(File file) throws IOException {
		return isCharsetUTF8(getFileCharset(file));
	}

	/**
	 * Tests if charset is UTF-8.
	 *
	 * @param charset <code>Charset</code> to test
	 * @return True if charset is UTF-8, false otherwise.
	 */
	public static boolean isCharsetUTF8(Charset charset) {
		return charset != null && charset.equals(StandardCharsets.UTF_8);
	}

	/**
	 * Tests if charset is UTF-8.
	 *
	 * @param charset charset name to test
	 * @return True if charset is UTF-8, false otherwise.
	 */
	public static boolean isCharsetUTF8(String charsetName) {
		return equalsIgnoreCase(charsetName, CHARSET_UTF_8);
	}

	/**
	 * Tests if file is UTF-16 encoded.
	 *
	 * @param file File to test
	 * @return True if file is UTF-16 encoded, false otherwise.
	 * @throws IOException
	 */
	public static boolean isFileUTF16(File file) throws IOException {
		return isCharsetUTF16(getFileCharset(file));
	}

	/**
	 * Tests if charset is UTF-16.
	 *
	 * @param charset <code>Charset</code> to test
	 * @return True if charset is UTF-16, false otherwise.
	 */
	public static boolean isCharsetUTF16(Charset charset) {
		return charset != null && (charset.equals(StandardCharsets.UTF_16) || charset.equals(StandardCharsets.UTF_16BE) || charset.equals(StandardCharsets.UTF_16LE));
	}

	/**
	 * Tests if charset is UTF-16.
	 *
	 * @param charset charset name to test
	 * @return True if charset is UTF-16, false otherwise.
	 */
	public static boolean isCharsetUTF16(String charsetName) {
		return (equalsIgnoreCase(charsetName, CHARSET_UTF_16LE) || equalsIgnoreCase(charsetName, CHARSET_UTF_16BE));
	}

	/**
	 * Tests if charset is UTF-32.
	 *
	 * @param charsetName charset name to test
	 * @return True if charset is UTF-32, false otherwise.
	 */
	public static boolean isCharsetUTF32(String charsetName) {
		return (equalsIgnoreCase(charsetName, CHARSET_UTF_32LE) || equalsIgnoreCase(charsetName, CHARSET_UTF_32BE));
	}

	/**
	 * Converts UTF-16 inputFile to UTF-8 outputFile. Does not overwrite existing outputFile file.
	 *
	 * @param inputFile UTF-16 file
	 * @param outputFile UTF-8 file after conversion
	 * @throws IOException
	 */
	public static void convertFileFromUtf16ToUtf8(File inputFile, File outputFile) throws IOException {
		Charset charset;
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
				/*
				 * This is a strange hack, and I'm not sure if it's needed. I
				 * did it this way to conform to the tests, which dictates that
				 * UTF-16LE should produce UTF-8 without BOM while UTF-16BE
				 * should produce UTF-8 with BOM.
				 *
				 * For some reason creating a FileInputStream with UTF_16 produces
				 * an UTF-8 outputfile without BOM, while using UTF_16LE or
				 * UTF_16BE produces an UTF-8 outputfile with BOM.
				 * @author Nadahar
				 */
				if (charset.equals(StandardCharsets.UTF_16LE)) {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_16));
				} else {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), charset));
				}

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
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
	 * Return a file or folder's permissions.<br><br>
	 *
	 * This should <b>NOT</b> be used for checking e.g. read permissions before
	 * trying to open a file, because you can't assume that the same is true
	 * when you actually open the file. Other threads or processes could have
	 * locked the file (or changed it's permissions) in the meanwhile. Instead,
	 * use e.g <code>FileNotFoundException</code> like this:
	 * <pre><code>
	 * } catch (FileNotFoundException e) {
	 * 	LOGGER.debug("Can't read xxx {}", e.getMessage());
	 * }
	 * </code></pre>
	 * <code>e.getMessage()</code> will contain both the full path to the file
	 * the reason it couldn't be read (e.g. no permission).
	 *
	 * @param file The file or folder to check permissions for
	 * @return A <code>FilePermissions</code> object holding the permissions
	 * @throws FileNotFoundException
	 * @see {@link #getFilePermissions(String)}
	 */
	public static FilePermissions getFilePermissions(File file) throws FileNotFoundException {
		return new FilePermissions(file);
	}

	/**
	 * Like {@link #getFilePermissions(File)} but returns <code>null</code>
	 * instead of throwing <code>FileNotFoundException</code> if the file or
	 * folder isn't found.
	 */
	public static FilePermissions getFilePermissionsNoThrow(File file) {
		try {
			return new FilePermissions(file);
		} catch (FileNotFoundException | IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Return a file or folder's permissions.<br><br>
	 *
	 * This should <b>NOT</b> be used for checking e.g. read permissions before
	 * trying to open a file, because you can't assume that the same is true
	 * when you actually open the file. Other threads or processes could have
	 * locked the file (or changed it's permissions) in the meanwhile. Instead,
	 * use e.g <code>FileNotFoundException</code> like this:
	 * <pre><code>
	 * } catch (FileNotFoundException e) {
	 * 	LOGGER.debug("Can't read xxx {}", e.getMessage());
	 * }
	 * </code></pre>
	 * <code>e.getMessage()</code> will contain both the full path to the file
	 * the reason it couldn't be read (e.g. no permission).
	 *
	 * @param path The file or folder name to check permissions for
	 * @return A <code>FilePermissions</code> object holding the permissions
	 * @throws FileNotFoundException
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
	 * Like {@link #getFilePermissions(String)} but returns <code>null</code>
	 * instead of throwing <code>FileNotFoundException</code> if the file or
	 * folder isn't found.
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

	public static boolean isFileRelevant(File f, PmsConfiguration configuration) {
		String fileName = f.getName().toLowerCase();
		if (
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
		) {
			return true;
		}

		return false;
	}

	public static boolean isFolderRelevant(File f, PmsConfiguration configuration) {
		return isFolderRelevant(f, configuration, Collections.<String>emptySet());
	}

	public static boolean isFolderRelevant(File f, PmsConfiguration configuration, Set<String> ignoreFiles) {
		if (f.isDirectory() && configuration.isHideEmptyFolders()) {
			File[] children = f.listFiles();

			/**
			 * listFiles() returns null if "this abstract pathname does not denote a directory, or if an I/O error occurs".
			 * in this case (since we've already confirmed that it's a directory), this seems to mean the directory is non-readable
			 * http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15135
			 * http://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
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
		if (PMS.getConfiguration().isPrettifyFilenames()) {
			filename = basicPrettify(filename);
		}

		if (PMS.getConfiguration().isIgnoreTheWordAandThe()) {
			// Remove "a" and "the" from filename
			filename = filename.replaceAll("^(?i)A[ .]|The[ .]", "");

			// Replace multiple whitespaces with space
			filename = filename.replaceAll("\\s{2,}"," ");
		}

		return filename;
	}

	/**
	 * @deprecated Use {@link #createBufferedReaderDetectCharset(File, Charset)} instead.
	 */
	@Deprecated
	public static BufferedReader bufferedReaderWithCorrectCharset(File file) throws IOException {
		return createBufferedReaderDetectCharset(file, StandardCharsets.UTF_8).getBufferedReader();
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
	public static BufferedReaderDetectCharsetResult createBufferedReaderDetectCharset(File file, Charset defaultCharset) throws IOException {
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
	 * @return The corrected path or {@code null} of {@code path} is
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

	private static Boolean isAdmin = null;
	private static Object isAdminLock = new Object();

	/**
	 * Determines whether or not the program has admin/root permissions.
	 */
	public static boolean isAdmin() {
		synchronized(isAdminLock) {
			if (isAdmin != null) {
				return isAdmin;
			}
			if (Platform.isWindows()) {
				Double version = PMS.get().getRegistry().getWindowsVersion();
				if (version == null) {
					LOGGER.error(
						"Could not determine Windows version from {}. Administrator privileges is undetermined.",
						System.getProperty("os.version")
					);
					isAdmin = false;
					return false;
				}
				if (version >= 5.1) {
					try {
						String command = "reg query \"HKU\\S-1-5-19\"";
						Process p = Runtime.getRuntime().exec(command);
						p.waitFor();
						int exitValue = p.exitValue();

						if (0 == exitValue) {
							isAdmin = true;
							return true;
						}
						isAdmin = false;
						return false;
					} catch (IOException | InterruptedException e) {
						LOGGER.error("An error prevented UMS from checking Windows permissions: {}", e.getMessage());
					}
				} else {
					isAdmin = true;
					return true;
				}
			} else if (Platform.isLinux() || Platform.isMac()) {
				try {
					final String command = "id -Gn";
					LOGGER.trace("isAdmin: Executing \"{}\"", command);
					Process p = Runtime.getRuntime().exec(command);
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII);
					int exitValue;
					String exitLine;
					try (BufferedReader br = new BufferedReader(isr)) {
						p.waitFor();
						exitValue = p.exitValue();
						exitLine = br.readLine();
					}
					if (exitValue != 0 || exitLine == null || exitLine.isEmpty()) {
						LOGGER.error("Could not determine root privileges, \"{}\" ended with exit code: {}", command, exitValue);
						isAdmin = false;
						return false;
					}
					LOGGER.trace("isAdmin: \"{}\" returned {}", command, exitLine);
					if
						((Platform.isLinux() && exitLine.matches(".*\\broot\\b.*")) ||
						(Platform.isMac() && exitLine.matches(".*\\badmin\\b.*")))
					{
						LOGGER.trace("isAdmin: UMS has {} privileges", Platform.isLinux() ? "root" : "admin");
						isAdmin = true;
						return true;
					}
					LOGGER.trace("isAdmin: UMS does not have {} privileges", Platform.isLinux() ? "root" : "admin");
					isAdmin = false;
					return false;
				} catch (IOException | InterruptedException e) {
					LOGGER.error("An error prevented UMS from checking {} permissions: {}", Platform.isMac() ? "OS X" : "Linux" ,e.getMessage());
				}
			}
			isAdmin = false;
			return false;
		}
	}

	/**
	 * Finds the {@link UnixMountPoint} for a {@link java.nio.file.Path} given
	 * that the file resides on a Unix file system.
	 *
	 * @param path the {@link java.nio.file.Path} for which to find the Unix mount point.
	 * @return The {@link UnixMountPoint} for the given path.
	 *
	 * @throws InvalidFileSystemException
	 */
	public static UnixMountPoint getMountPoint(Path path) throws InvalidFileSystemException {
		UnixMountPoint mountPoint = new UnixMountPoint();
		FileStore store;
		try {
			store = Files.getFileStore(path);
		} catch (IOException e) {
			throw new InvalidFileSystemException(
				String.format("Could not get Unix mount point for file \"%s\": %s", path.toAbsolutePath(), e.getMessage()),
				e
			);
		}

		try {
			Field entryField = store.getClass().getSuperclass().getDeclaredField("entry");
			Field nameField = entryField.getType().getDeclaredField("name");
			Field dirField = entryField.getType().getDeclaredField("dir");
			entryField.setAccessible(true);
			nameField.setAccessible(true);
			dirField.setAccessible(true);
			mountPoint.device = new String((byte[]) nameField.get(entryField.get(store)), StandardCharsets.UTF_8);
			mountPoint.folder = new String((byte[]) dirField.get(entryField.get(store)), StandardCharsets.UTF_8);
			return mountPoint;
		} catch (NoSuchFieldException e) {
			throw new InvalidFileSystemException(String.format("File \"%s\" is not on a Unix file system", path.isAbsolute()), e);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new InvalidFileSystemException(
				String.format("An error occurred while trying to find mount point for file \"%s\": %s", path.toAbsolutePath(), e.getMessage()),
				e
			);
		}
	}

	/**
	 * Finds the {@link UnixMountPoint} for a {@link java.io.File} given
	 * that the file resides on a Unix file system.
	 *
	 * @param file the {@link java.io.File} for which to find the Unix mount point.
	 * @return The {@link UnixMountPoint} for the given path.
	 *
	 * @throws InvalidFileSystemException
	 */
	public static UnixMountPoint getMountPoint(File file) throws InvalidFileSystemException {
		return getMountPoint(file.toPath());
	}

	public static boolean isUnixStickyBit(Path path) throws IOException, InvalidFileSystemException {
		PosixFileAttributes attr = Files.readAttributes(path, PosixFileAttributes.class);
		try {
			Field st_modeField = attr.getClass().getDeclaredField("st_mode");
			st_modeField.setAccessible(true);
			int st_mode = st_modeField.getInt(attr);
			return (st_mode & S_ISVTX) > 0;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new InvalidFileSystemException("File is not on a Unix file system: " + e.getMessage(), e);
		}
	}

	private static int unixUID = Integer.MIN_VALUE;
	private static Object unixUIDLock = new Object();

	/**
	 * Gets the user ID on Unix based systems. This should not change during a
	 * session and the lookup is expensive, so we cache the result.
	 *
	 * @return The Unix user ID
	 * @throws IOException
	 */
	public static int getUnixUID() throws IOException {
		if (
			Platform.isAIX() || Platform.isFreeBSD() || Platform.isGNU() || Platform.iskFreeBSD() ||
			Platform.isLinux() || Platform.isMac() || Platform.isNetBSD() || Platform.isOpenBSD() ||
			Platform.isSolaris()
		) {
			synchronized (unixUIDLock) {
				if (unixUID < 0) {
					String response;
				    Process id;
					id = Runtime.getRuntime().exec("id -u");
				    try (BufferedReader reader = new BufferedReader(new InputStreamReader(id.getInputStream(), Charset.defaultCharset()))) {
				    	response = reader.readLine();
				    }
				    try {
				    	unixUID = Integer.parseInt(response);
				    } catch (NumberFormatException e) {
				    	throw new UnsupportedOperationException("Unexpected response from OS: " + response, e);
				    }
				}
				return unixUID;
			}
		}
		throw new UnsupportedOperationException("getUnixUID can only be called on Unix based OS'es");
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
}
