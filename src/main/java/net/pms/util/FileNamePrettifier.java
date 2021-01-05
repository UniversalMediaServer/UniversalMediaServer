/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.WordUtils;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.VideoClassification;


/**
 * This class "prettifies" video file names.
 */
public class FileNamePrettifier {

	/** Strings that only occur after all useful information. */
	public static final String COMMON_FILE_ENDS =
		"[\\s\\.]AC3.*|[\\s\\.]REPACK.*|[\\s\\.]480p.*|[\\s\\.]720p.*|" +
		"[\\s\\.]m-720p.*|[\\s\\.]900p.*|[\\s\\.]1080p.*|[\\s\\.]2160p.*|" +
		"[\\s\\.]WEB-DL.*|[\\s\\.]HDTV.*|[\\s\\.]DSR.*|[\\s\\.]PDTV.*|" +
		"[\\s\\.]WS.*|[\\s\\.]HQ.*|[\\s\\.]DVDRip.*|[\\s\\.]TVRiP.*|" +
		"[\\s\\.]BDRip.*|[\\s\\.]BRRip.*|[\\s\\.]WEBRip.*|[\\s\\.]BluRay.*|" +
		"[\\s\\.]Blu-ray.*|[\\s\\.]SUBBED.*|[\\s\\.]x264.*|" +
		"[\\s\\.]Dual[\\s\\.]Audio.*|[\\s\\.]HSBS.*|[\\s\\.]H-SBS.*|" +
		"[\\s\\.]RERiP.*|[\\s\\.]DIRFIX.*|[\\s\\.]READNFO.*|[\\s\\.]60FPS.*|" +
		"[\\s\\.]Xvid.*|[\\s\\.]DivX.*|[\\s\\.]DVB.*";

	/** A precompiled {@link Pattern} for {@link #COMMON_FILE_ENDS} */
	protected static final Pattern COMMON_FILE_ENDS_PATTERN = Pattern.compile(COMMON_FILE_ENDS);

	/**
	 * Same as {@link #COMMON_FILE_ENDS}, but they are common words so reduce
	 * the chances of false-positives by being case-sensitive.
	 */
	public static final String COMMON_FILE_ENDS_CASE_SENSITIVE =
		"[\\s\\.]PROPER[\\s\\.].*|[\\s\\.]iNTERNAL[\\s\\.].*|" +
		"[\\s\\.]LIMITED[\\s\\.].*|[\\s\\.]LiMiTED[\\s\\.].*|" +
		"[\\s\\.]FESTiVAL[\\s\\.].*|[\\s\\.]NORDIC[\\s\\.].*|" +
		"[\\s\\.]REAL[\\s\\.].*|[\\s\\.]SUBBED[\\s\\.].*|" +
		"[\\s\\.]RETAIL[\\s\\.].*|[\\s\\.]EXTENDED[\\s\\.].*|" +
		"[\\s\\.]NEWEDIT[\\s\\.].*|[\\s\\.]WEB[\\s\\.].*|" +
		"[\\s\\.]NORDiC[\\s\\.].*|\\[shareprovider[\\s\\.]com\\].*" +
		"|\\[sharethefiles[\\s\\.]com\\].*";

	/** Editions to be added to the end of the prettified name */
	private static final Pattern COMMON_FILE_EDITIONS = Pattern.compile(
		"(?i)(?:\\s+-\\s+)?(?!\\()(Special[\\s\\.]Edition|Unrated|Final[\\s\\.]Cut|" +
		"Remastered|Extended[\\s\\.]Cut|IMAX[\\s\\.]Edition|Uncensored|" +
		"Directors[\\s\\.]Cut|Uncut)(?!\\))"
	);

	private static final Pattern SERIES_EPISODE_EPISODE = Pattern.compile(
		"[sS](\\d{1,3})-?[eE](\\d{1,4})-?[eE](\\d{1,4})"
	);
	private static final Pattern SERIES_EPISODE = Pattern.compile("[sS](\\d{1,3})-?[eE](\\d{1,4})");
	private static final Pattern SERIES_DATE = Pattern.compile("([19|20]\\d{2})[\\s\\.]([0-1]\\d)[\\s\\.]([0-3]\\d)");
	private static final Pattern MOVIE_YEAR = Pattern.compile(
		"[\\(\\[]((?:19|20)\\d{2})[\\)\\]]|((?<![\\d\\(\\[])(?:19|20)\\d{2}(?![\\d\\)\\]]))"
	);

	private static final Pattern ANIME_SERIES_EPISODE = Pattern.compile(
		"(?:[\\s\\.']|S(\\d{1,2})[\\s\\.]?EP)(\\d{1,3})(?:[\\s\\.']|v\\d)",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern ANIME_SERIES_EPISODE_EPISODE = Pattern.compile(
		"(?:[\\s\\.']|S(\\d{1,2})[\\s\\.]?EP)(\\d{1,3})-(\\d{1,3})(?:[\\s\\.']|v\\d)",
		Pattern.CASE_INSENSITIVE
	);

	private static final Pattern LEADING_TRAILING_HYPHEN = Pattern.compile("^-\\s*|\\s*-$");

	private static final Pattern ANIME_HASH = Pattern.compile("\\[[0-9a-zA-Z]{8}\\]$");

	private final String fileName;
	private final String fileNameWithoutExtension;
	private final Locale locale;
	private VideoClassification classification;
	private String name;
	private int year = -1;
	private String edition;
	private int season = -1;
	private int episode = -1;
	private String episodeName;

	/**
	 * Creates a new instance for the specified {@link DLNAResource}.
	 *
	 * @param resource the {@link DLNAResource} whose name to prettify.
	 */
	public FileNamePrettifier(DLNAResource resource) {
		if (resource == null) {
			throw new IllegalArgumentException("resource cannot be null");
		}
		String tmpName = null;
		if (resource instanceof RealFile) {
			tmpName = ((RealFile) resource).getFile().getName();
		}
		if (isBlank(tmpName)) {
			tmpName = resource.getSystemName();
		}
		if (isBlank(tmpName)) {
			tmpName = resource.getName();
		}
		fileName = tmpName;
		fileNameWithoutExtension = FileUtil.getFileNameWithoutExtension(tmpName);
		locale = PMS.getLocale();
		parse();
	}

	/**
	 * Creates a new instance for the specified {@link File}.
	 * {@link Locale#ROOT} is used for case conversion.
	 *
	 * @param file the {@link File} whose name to prettify.
	 */
	public FileNamePrettifier(File file) {
		this(file.getName(), null);
	}

	/**
	 * Creates a new instance for the specified {@link File}.
	 *
	 * @param file the {@link File} whose name to prettify.
	 * @param locale the {@link Locale} to use for case conversion or
	 *            {@code null} for {@link Locale#ROOT}.
	 */
	public FileNamePrettifier(File file, Locale locale) {
		this(file.getName(), locale);
	}

	/**
	 * Creates a new instance with the specified {@link String}.
	 * {@link Locale#ROOT} is used for case conversion.
	 *
	 * @param fileName the {@link String} to prettify.
	 */
	public FileNamePrettifier(String fileName) {
		this(fileName, null);
	}

	/**
	 * Creates a new instance with the specified {@link String}.
	 *
	 * @param fileName the {@link String} to prettify.
	 * @param locale the {@link Locale} to use for case conversion or
	 *            {@code null} for {@link Locale#ROOT}.
	 */
	public FileNamePrettifier(String fileName, Locale locale) {
		if (fileName == null) {
			throw new IllegalArgumentException("fileName cannot be null");
		}
		if (locale == null) {
			locale = Locale.ROOT;
		}
		this.fileName = fileName;
		this.fileNameWithoutExtension = FileUtil.getFileNameWithoutExtension(fileName);
		this.locale = locale;
		parse();
	}

	/**
	 * @return The filename.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return The filename with the extension, if any, stripped off.
	 */
	public String getFileNameWithoutExtension() {
		return fileNameWithoutExtension;
	}

	/**
	 * @return The {@link VideoClassification}.
	 */
	public VideoClassification getClassification() {
		return classification;
	}

	/**
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The year.
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @return The edition.
	 */
	public String getEdition() {
		return edition;
	}

	/**
	 * @return The season.
	 */
	public int getSeason() {
		return season;
	}

	/**
	 * @return The episode.
	 */
	public int getEpisode() {
		return episode;
	}

	/**
	 * @return The episode name.
	 */
	public String getEpisodeName() {
		return episodeName;
	}

	/**
	 * @return The prettified name.
	 */
	public String getPrettifiedName() {
		StringBuilder sb = new StringBuilder();
		if (isBlank(name)) {
			sb.append("Unknown");
		} else {
			sb.append(name);
		}
		if (season > 0 && episode > 0) {
			sb.append(" ").append(season).append("-").append(episode);
		} else if (episode > 0) {
			sb.append(" ").append(episode);
		}
		if (episode > 0 && isNotBlank(episodeName)) {
			sb.append(" ").append(episodeName);
		}
		if (year > 0 && year != season) {
			sb.append(" (").append(year).append(")");
		}
		if (isNotBlank(edition)) {
			sb.append(" ").append(edition);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return getPrettifiedName();
	}

	/**
	 * Parses the media resource.
	 */
	protected void parse() {
		if (isBlank(fileNameWithoutExtension)) {
			return;
		}

		String tmpName = removeGroupNameFromBeginning(fileNameWithoutExtension);
		tmpName = removeFilenameEndMetadata(tmpName);
		tmpName = tmpName.replaceAll("_", " ");
		if (isBlank(fileName)) {
			return;
		}

		Matcher matcher = ANIME_HASH.matcher(tmpName);
		if (matcher.find()) {
			// Anime with a hash at the end of the name, remove it
			tmpName = matcher.replaceAll("");
		}

		matcher = SERIES_EPISODE_EPISODE.matcher(tmpName);
		if (matcher.find()) {
			// Series containing more than one episode
			classification = VideoClassification.SERIES;
			season = Integer.parseInt(matcher.group(1));
			// Using first episode only
			episode = Integer.parseInt(matcher.group(2));
			String[] splitName = SERIES_EPISODE_EPISODE.split(tmpName);
			if (splitName.length > 1) {
				String tmpEpisodeName = splitName[1];
				tmpEpisodeName = normalizeSpaces(tmpEpisodeName);
				if (tmpEpisodeName.length() > 0) {
					episodeName = convertFormattedNameToTitleCase(tmpEpisodeName);
				}
			}
			name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(splitName[0])));
			return;
		}
		matcher = SERIES_EPISODE.matcher(tmpName);
		if (matcher.find()) {
			// A series episode
			classification = VideoClassification.SERIES;
			season = Integer.parseInt(matcher.group(1));
			// Using first episode only
			episode = Integer.parseInt(matcher.group(2));
			String[] splitName = SERIES_EPISODE.split(tmpName);
			if (splitName.length > 1) {
				String tmpEpisodeName = splitName[1];
				tmpEpisodeName = normalizeSpaces(tmpEpisodeName);
				if (tmpEpisodeName.length() > 0) {
					episodeName = convertFormattedNameToTitleCase(tmpEpisodeName);
				}
			}
			name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(splitName[0])));
			return;
		}
		matcher = ANIME_SERIES_EPISODE.matcher(tmpName);
		matcher = SERIES_DATE.matcher(tmpName);
		if (matcher.find()) {
			// A series episode with date based episode naming
			classification = VideoClassification.SERIES;
			season = Integer.parseInt(matcher.group(1));
			year = season;
			// Combining month and day into an "episode number"
			episode = Integer.parseInt(matcher.group(2) + matcher.group(3));
			String[] splitName = SERIES_DATE.split(tmpName);
			if (splitName.length > 1) {
				String tmpEpisodeName = splitName[1];
				tmpEpisodeName = normalizeSpaces(tmpEpisodeName);
				if (tmpEpisodeName.length() > 0) {
					episodeName = convertFormattedNameToTitleCase(tmpEpisodeName);
				}
			}
			name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(splitName[0])));
			return;
		}
		matcher = MOVIE_YEAR.matcher(tmpName);
		if (matcher.find()) {
			// A movie
			classification = VideoClassification.MOVIE;
			tmpName = extractEdition(tmpName);
			// The year can be in either group
			String yearString = matcher.group(2);
			if (isBlank(yearString)) {
				yearString = matcher.group(1);
			}
			year = Integer.parseInt(yearString);
			name = convertFormattedNameToTitleCase(splitClean(MOVIE_YEAR, tmpName));
			return;
		}
		matcher = ANIME_SERIES_EPISODE_EPISODE.matcher(tmpName);
		if (matcher.find()) {
			// Anime containing more than one episode
			classification = VideoClassification.SERIES;
			season = isBlank(matcher.group(1)) ? 1 : Integer.parseInt(matcher.group(1));
			// Using first episode only
			episode = Integer.parseInt(matcher.group(2));
			String[] splitName = ANIME_SERIES_EPISODE_EPISODE.split(tmpName);
			if (splitName.length > 1) {
				String tmpEpisodeName = splitName[1];
				tmpEpisodeName = normalizeSpaces(tmpEpisodeName);
				if (tmpEpisodeName.length() > 0) {
					episodeName = convertFormattedNameToTitleCase(tmpEpisodeName);
				}
			}
			name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(splitName[0])));
			return;
		}
		if (matcher.find()) {
			// An anime episode
			classification = VideoClassification.SERIES;
			season = isBlank(matcher.group(1)) ? 1 : Integer.parseInt(matcher.group(1));
			// Using first episode only
			episode = Integer.parseInt(matcher.group(2));
			String[] splitName = ANIME_SERIES_EPISODE.split(tmpName);
			if (splitName.length > 1) {
				String tmpEpisodeName = splitName[1];
				tmpEpisodeName = normalizeSpaces(tmpEpisodeName);
				if (tmpEpisodeName.length() > 0) {
					episodeName = convertFormattedNameToTitleCase(tmpEpisodeName);
				}
			}
			name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(splitName[0])));
			return;
		}
		// Probably a movie without a year
		classification = VideoClassification.MOVIE;
		tmpName = extractEdition(tmpName);
		name = convertFormattedNameToTitleCase(normalizeSpaces(removeFilenameEndMetadata(tmpName)));
	}

	/**
	 * Converts all dots to spaces, reduces sequences of whitespace to a single
	 * space and removes leading and trailing whitespace.
	 *
	 * @param name the name to normalize.
	 * @return The normalized {@link String}.
	 */
	protected String normalizeSpaces(String name) {
		if (isEmpty(name)) {
			return name;
		}
		String result = name.replaceAll("\\.", " ").replaceAll("\\s+", " ").trim();
		Matcher matcher = LEADING_TRAILING_HYPHEN.matcher(result);
		if (matcher.find()) {
			result = matcher.replaceAll("");
		}
		return result;
	}

	/**
	 * Extracts edition information.
	 *
	 * @param name the name to process.
	 * @return The name with the edition (if any) removed.
	 */
	protected String extractEdition(String name) {
		Matcher matcher = COMMON_FILE_EDITIONS.matcher(name);
		if (matcher.find()) {
			String tmpEdition = matcher.group().replaceAll("\\.", " ");
			tmpEdition = "(" + WordUtils.capitalizeFully(tmpEdition) + ")";
			if (isNotBlank(tmpEdition)) {
				edition = tmpEdition;
			}
			return matcher.replaceAll("");
		}
		return name;
	}

	/**
	 * Remove stuff from the end of the filename like release group, quality,
	 * source, etc.
	 *
	 * @param name the name to process;
	 * @return The name with common file endings removed.
	 */
	protected String removeFilenameEndMetadata(String name) {
		return name.replaceAll(COMMON_FILE_ENDS_CASE_SENSITIVE, "").replaceAll("(?i)" + COMMON_FILE_ENDS, "");
	}


	/**
	 * Remove the group name from the beginning of the name.
	 *
	 * @param name the name to process.
	 * @return The name with the prefixed group name (if any) removed.
	 */
	protected String removeGroupNameFromBeginning(String name) {
		if (isBlank(name)) {
			return name;
		}
		if (name.startsWith("[")) {
			Pattern pattern = Pattern.compile("^\\[[^\\]]{0,20}\\][^\\w]*(\\w.*?)\\s*$");
			Matcher matcher = pattern.matcher(name);
			if (matcher.find()) {
				name = matcher.group(1);
			} else if (name.endsWith("]")) {
				pattern = Pattern.compile("^\\[([^\\[\\]]+)\\]\\s*$");
				matcher = pattern.matcher(name);
				if (matcher.find()) {
					name = matcher.group(1);
				}
			}
		}

		return name;
	}

	/**
	 * Capitalize the first letter of each word if the name is in all
	 * lower-case.
	 *
	 * @param name the name to process.
	 * @return the converted name.
	 */
	protected String convertFormattedNameToTitleCase(String name) {
		if (isBlank(name)) {
			return name;
		}
		if (name.equals(name.toLowerCase(locale))) {
			return FileUtil.convertLowerCaseStringToTitleCase(name);
		}
		return name;
	}

	/**
	 * Splits the specified {@link String} using the specified {@link Pattern},
	 * cleans each part and returns the seemingly most relevant part after
	 * cleaning.
	 *
	 * @param pattern the {@link Pattern} to use.
	 * @param name the {@link String} to process.
	 * @return The part after processing.
	 */
	protected String splitClean(Pattern pattern, String name) {
		if (pattern == null || isBlank(name)) {
			return name;
		}
		String[] splitName = pattern.split(name);
		for (int i = 0; i < splitName.length; i++) {
			splitName[i] = normalizeSpaces(removeFilenameEndMetadata(splitName[i]));
		}
		if (splitName[0].length() < 3 && splitName[1].length() > 3) {
			return splitName[1];
		}
		return splitName[0];
	}
}
