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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a utility class for IMDb related operations.
 */
public class ImdbUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImdbUtil.class);
	private static final String FILENAME_HASH = "_os([^_]+)_";
	private static final String FILENAME_IMDB_ID = "_imdb([^_]+)_";
	private static final Pattern NFO_IMDB_ID = Pattern.compile("imdb\\.[^\\/]+\\/title\\/tt(\\d+)", Pattern.CASE_INSENSITIVE);

	/**
	 * This class is not meant to be instantiated.
	 */
	private ImdbUtil() {
	}

	/**
	 * Extracts the OpenSubtitle file hash from the filename if the file has
	 * "{@code FILENAME_HASH}" in it.
	 *
	 * @param file the {@link Path} of the file whose filename to extract from.
	 * @return The extracted OpenSubtitle file hash or {@code null}.
	 */
	public static String extractOSHash(Path file) {
		return extractFromFileName(file, FILENAME_HASH);
	}

	/**
	 * Extracts the IMDb ID from the filename if the file has
	 * "{@code FILENAME_IMDB_ID}" in it.
	 *
	 * @param file the {@link Path} of the file whose IMDb ID to extract from.
	 * @param scanNfo true to try to extract the IMDb ID from the NFO file when failed
	 * to extract it from the file name
	 * @return The extracted IMDb ID or {@code null}.
	 */
	public static String extractImdbId(Path file, boolean scanNfo) {
		String imdbId = extractFromFileName(file, FILENAME_IMDB_ID);
		if (StringUtils.isNotBlank(imdbId)) {
			return removeTT(imdbId);
		}
		return scanNfo ? extractImdbIdFromNfo(file) : null;
	}

	/**
	 * Extracts the OpenSubtitle file hash or the IMDb ID from the filename.
	 *
	 * @param file the {@link Path} of the file whose IMDb ID to extract from.
	 * @param regex the parameter to find
	 * @return The extracted OpenSubtitle file hash or the IMDb ID or {@code null}.
	 */
	private static String extractFromFileName(Path file, String regex) {
		if (file == null || file.getFileName() == null || regex == null) {
			return null;
		}

		Pattern pattern = Pattern.compile(regex);
		String fileName = file.getFileName().toString();
		Matcher matcher = pattern.matcher(fileName);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * Extracts the IMDb ID from the associated NFO file if exists.
	 *
	 * @param file the {@link Path} of the file whose IMDb ID to extract from.
	 * @return The extracted IMDb ID or {@code null}.
	 */
	private static String extractImdbIdFromNfo(Path file) {
		if (file == null) {
			return null;
		}
		Path nfoFile = FileUtil.replaceExtension(file, "nfo", false, true);
		if (nfoFile == null) {
			return null;
		}
		if (!Files.exists(nfoFile)) {
			Path nfoFileNamePath = nfoFile.getFileName();
			String nfoFileName = nfoFileNamePath == null ? null : nfoFileNamePath.toString();
			Path parent = nfoFile.getParent();
			if (StringUtils.isNotBlank(nfoFileName) && parent != null) {
				HashMap<Path, Double> candidates = new HashMap<>();
				try (DirectoryStream<Path> nfoFiles =  Files.newDirectoryStream(parent, (Path entry) -> {
					String extension = FileUtil.getExtension(entry.getFileName());
					return "nfo".equals(extension) || "NFO".equals(extension);
				})) {
					for (Path entry : nfoFiles) {
						Path entryFileNamePath = entry.getFileName();
						String entryName = entryFileNamePath == null ? null : entryFileNamePath.toString();
						if (StringUtils.isBlank(entryName)) {
							continue;
						}
						double score = new JaroWinklerSimilarity().apply(nfoFileName, entryName);
						if (score >= 0.85) {
							candidates.put(entry, score);
						}
					}
					if (!candidates.isEmpty()) {
						ArrayList<Entry<Path, Double>> candidatesList = new ArrayList<>(candidates.entrySet());
						if (candidatesList.size() > 1) {
							// Sort by score
							Collections.sort(candidatesList, (Entry<Path, Double> o1, Entry<Path, Double> o2) -> o2.getValue().compareTo(o1.getValue()));
						}
						nfoFile = candidatesList.get(0).getKey();
					}
				} catch (IOException e) {
					LOGGER.error(
						"An error occurred when trying to scan for nfo files belonging to \"{}\": {}",
						file,
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
			}
		}
		if (!Files.exists(nfoFile)) {
			LOGGER.debug("Didn't find a matching nfo file for \"{}\" to search for IMDb ID", file);
			return null;
		}
		try {
			if (Files.size(nfoFile) > 100000) {
				LOGGER.debug(
					"Skipping search for IMDb ID in \"{}\" since it's too large ({})",
					nfoFile,
					StringUtil.formatBytes(Files.size(nfoFile), true)
				);
				return null;
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to get size of \"{}\", skipping search for IMDb ID: {}", nfoFile, e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		Charset charset = null;
		try {
			charset = FileUtil.detectCharset(nfoFile, 70);
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while trying to detect the character encoding of \"{}\": {}",
				nfoFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		if (charset == null) {
			charset = PlatformUtils.INSTANCE.getDefaultCharset();
			LOGGER.debug(
				"Failed to detect the character encoding of \"{}\", falling back to the default charset \"{}\"",
				nfoFile,
				charset
			);
		}
		LOGGER.trace("Scanning \"{}\" for IMDb ID for \"{}\"", nfoFile, file);
		try (BufferedReader reader = Files.newBufferedReader(nfoFile, charset)) {
			String line = reader.readLine();
			while (line != null) {
				Matcher matcher = NFO_IMDB_ID.matcher(line);
				if (matcher.find()) {
					LOGGER.trace("Found IMDb ID {} for \"{}\" in \"{}\"", matcher.group(1), file, nfoFile);
					return matcher.group(1);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			LOGGER.error("An error occurred while scanning \"{}\" for IMDb ID: {}", nfoFile, e.getMessage());
			LOGGER.trace("", e);
		}
		LOGGER.trace("Failed to find IMDb ID for \"{}\" in \"{}\"", file, nfoFile);
		return null;
	}

	/**
	 * Makes sure the IMDb ID starts with "{@code tt}" by prefixing it if
	 * needed.
	 *
	 * @param imdbId the IMDbB ID to make sure starts with "{@code tt}".
	 * @return The "{@code tt}" prefixed IMDb ID.
	 */
	public static String ensureTT(String imdbId) {
		if (StringUtils.isBlank(imdbId)) {
			return imdbId;
		}
		imdbId = imdbId.trim().toLowerCase(Locale.ROOT);
		return (imdbId.startsWith("tt") ? imdbId : "tt" + imdbId);
	}

	/**
	 * Makes sure the IMDb doesn't start with "{@code tt}" by removing it if
	 * present.
	 *
	 * @param imdbId the IMDb ID to make sure doesn't start with "{@code tt}".
	 * @return The IMDb ID without a "{@code tt}" prefix.
	 */
	public static String removeTT(String imdbId) {
		if (StringUtils.isBlank(imdbId)) {
			return imdbId;
		}
		imdbId = imdbId.trim().toLowerCase(Locale.ROOT);
		return imdbId.startsWith("tt") ? imdbId.substring(2) : imdbId;
	}
}
