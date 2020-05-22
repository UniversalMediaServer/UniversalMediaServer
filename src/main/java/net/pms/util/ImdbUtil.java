package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;
import net.pms.io.WinUtils;

/**
 * This is a utility class for IMDB related operations.
 */
public class ImdbUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImdbUtil.class);
	private static final String FILENAME_HASH = "_os([^_]+)_";
	private static final String FILENAME_IMDB_ID = "_imdb([^_]+)_";
	private static final Pattern NFO_IMDB_ID = Pattern.compile("imdb\\.[^\\/]+\\/title\\/tt(\\d+)", Pattern.CASE_INSENSITIVE);

	/**
	 * Not to be instantiated.
	private ImdbUtil() {
	}

	/**
	 * Extracts the OpenSubtitle file hash from the filename if the file has "
	 * {@code _os<hash>_}" in it.
	 *
	 * @param file the {@link File} whose filename to extract from.
	 * @return The extracted OpenSubtitle file hash or {@code null}.
	 */
	public static String extractOSHash(Path file) {
		return extractFromFileName(file, FILENAME_HASH);
	}

	public static String extractImdbId(Path file, boolean scanNfo) {
		String imdbId = extractFromFileName(file, FILENAME_IMDB_ID);
		if (isNotBlank(imdbId)) {
			return removeTT(imdbId);
		}
		return scanNfo ? extractImdbIdFromNfo(file) : null;
	}

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
			if (isNotBlank(nfoFileName) && parent != null) {
				HashMap<Path, Double> candidates = new HashMap<>();
				try {
					DirectoryStream<Path> nfoFiles =  Files.newDirectoryStream(parent, new DirectoryStream.Filter<Path>() {
						@Override
						public boolean accept(Path entry) throws IOException {
							String extension = FileUtil.getExtension(entry.getFileName());
							return "nfo".equals(extension) || "NFO".equals(extension);
						}
					});
					for (Path entry : nfoFiles) {
						Path entryFileNamePath = entry.getFileName();
						String entryName = entryFileNamePath == null ? null : entryFileNamePath.toString();
						if (isBlank(entryName)) {
							continue;
						}
						double score = new JaroWinklerDistance().apply(nfoFileName, entryName);
						if (score <= 0.85) { // threshold to add candidate to map. XXX it should be adjusted
							candidates.put(entry, Double.valueOf(score));
						}
					}
					if (!candidates.isEmpty()) {
						ArrayList<Entry<Path, Double>> candidatesList = new ArrayList<>(candidates.entrySet());
						if (candidatesList.size() > 1) {
							// Sort by score
							Collections.sort(candidatesList, new Comparator<Entry<Path, Double>>() {
								@Override
								public int compare(Entry<Path, Double> o1, Entry<Path, Double> o2) {
									return o2.getValue().compareTo(o1.getValue());
								}
							});
						}
						nfoFile = candidatesList.get(0).getKey();
					}
				} catch (IOException e) {
					LOGGER.error(
						"An error occured when trying to scan for nfo files belonging to \"{}\": {}",
						file,
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
			}
		}
		if (!Files.exists(nfoFile)) {
			LOGGER.debug("Didn't find a matching nfo file for \"{}\" to search for IMDB ID", file);
			return null;
		}
		try {
			if (Files.size(nfoFile) > 100000) {
				LOGGER.debug(
					"Skipping search for IMDB ID in \"{}\" since it's too large ({})",
					nfoFile,
					StringUtil.formatBytes(Files.size(nfoFile), true)
				);
				return null;
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to get size of \"{}\", skipping search for IMDB ID: {}", nfoFile, e.getMessage());
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
			if (Platform.isWindows()) {
				// Because UMS is configured to erroneously always set the default charset to UTF-8, this is useless for Windows
				charset = WinUtils.getOEMCharset();
				if (charset == null) {
					charset = StandardCharsets.US_ASCII;
				}
			} else {
				charset = Charset.defaultCharset();
			}
			LOGGER.debug(
				"Failed to detect the character encoding of \"{}\", falling back to the default charset \"{}\"",
				nfoFile,
				charset
			);
		}
		LOGGER.trace("Scanning \"{}\" for IMDB ID for \"{}\"", nfoFile, file);
		try (BufferedReader reader = Files.newBufferedReader(nfoFile, charset)) {
			String line = reader.readLine();
			while (line != null) {
				Matcher matcher = NFO_IMDB_ID.matcher(line);
				if (matcher.find()) {
					LOGGER.trace("Found IMDB ID {} for \"{}\" in \"{}\"", matcher.group(1), file, nfoFile);
					return matcher.group(1);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			LOGGER.error("An error occurred while scanning \"{}\" for IMDB ID: {}", nfoFile, e.getMessage());
			LOGGER.trace("", e);
		}
		LOGGER.trace("Failed to find IMDB ID for \"{}\" in \"{}\"", file, nfoFile);
		return null;
	}

	/**
	 * Makes sure the IMDB ID starts with "{@code tt}" by prefixing it if
	 * needed.
	 *
	 * @param imdbId the IMDB ID to make sure starts with "{@code tt}".
	 * @return The "{@code tt}" prefixed IMDB ID.
	 */
	public static String ensureTT(String imdbId) {
		if (isBlank(imdbId)) {
			return imdbId;
		}
		imdbId = imdbId.trim().toLowerCase(Locale.ROOT);
		return (imdbId.startsWith("tt") ? imdbId : "tt" + imdbId);
	}

	/**
	 * Makes sure the IMDB doesn't start with "{@code tt}" by removing it if
	 * present.
	 *
	 * @param imdbId the IMDB ID to make sure doesn't start with "{@code tt}".
	 * @return The IMDB ID without a "{@code tt}" prefix.
	 */
	public static String removeTT(String imdbId) {
		if (isBlank(imdbId)) {
			return imdbId;
		}
		imdbId = imdbId.trim().toLowerCase(Locale.ROOT);
		return imdbId.startsWith("tt") ? imdbId.substring(2) : imdbId;
	}
}
