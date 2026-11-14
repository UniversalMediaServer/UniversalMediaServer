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
package net.pms.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.util.FileUtil;

public class SystemFilesHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemFilesHelper.class);

	/**
	 * An array of {@link String}s that defines the lower-case representation of
	 * the file extensions that are eligible for evaluation as file or folder
	 * thumbnails.
	 */
	public static final Set<String> THUMBNAIL_EXTENSIONS = Set.of("webp", "png", "jpeg", "jpg", "bmp", "gif");

	/**
	 * An array of {@link String}s that defines the file extensions that are
	 * never media so we should not attempt to parse.
	 */
	public static final Set<String> EXTENSIONS_DENYLIST = Set.of("!qB", "!ut", "1", "dmg", "exe");

	/**
	 * This class is not meant to be instantiated.
	 */
	private SystemFilesHelper() {
	}

	/**
	 * Default "folder" logic, using the configured folder thumbnail names.
	 *
	 * @param folder
	 * @return
	 */
	public static File getFolderThumbnail(File folder) {
		return getFolderThumbnail(folder, PMS.getConfiguration().getFolderThumbnailNames());
	}

	/**
	 * Returns the best {@link File} in a specified folder that is considered a
	 * "folder thumbnail" by naming convention.
	 * <p>
	 * A file is a candidate if it has an image extension and its name is one
	 * of {@code thumbnailNames} followed by an extension, or contains
	 * "albumart". Names are matched case-insensitively and earlier names win
	 * over later ones. "albumart" files are used only if no named file exists.
	 * If several files share the same priority, the first by file name is
	 * returned so the result does not depend on the file system order.
	 *
	 * @param folder the folder to search for a folder thumbnail.
	 * @param thumbnailNames the file names, without extension, to look for in
	 *            order of priority.
	 * @return The best "folder thumbnail" file in {@code folder} or
	 *         {@code null} if none was found.
	 */
	public static File getFolderThumbnail(File folder, List<String> thumbnailNames) {
		if (folder == null || !folder.isDirectory()) {
			return null;
		}

		List<String> names = normalizeThumbnailNames(thumbnailNames);
		File best = null;
		int bestPriority = Integer.MAX_VALUE;
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder.toPath())) {
			for (Path entry : entries) {
				Path fileNamePath = entry.getFileName();
				if (fileNamePath == null) {
					continue;
				}
				String fileName = fileNamePath.toString();
				int priority = getFolderThumbnailPriority(fileName, names);
				if (priority < 0 || !isPotentialThumbnail(fileName.toLowerCase(Locale.ROOT))) {
					continue;
				}
				if (best == null || priority < bestPriority ||
						(priority == bestPriority && fileName.compareToIgnoreCase(best.getName()) < 0)) {
					best = entry.toFile();
					bestPriority = priority;
				}
			}
		} catch (IOException e) {
			LOGGER.warn("An error occurred while trying to browse folder \"{}\": {}", folder.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
		return best;
	}

	/**
	 * Returns the first {@link File} in a specified folder that is considered a
	 * "folder thumbnail" by naming convention.
	 *
	 * @param folder the folder to search for a folder thumbnail.
	 * @return The first "folder thumbnail" file in {@code folder} or
	 *         {@code null} if none was found.
	 */
	public static File getFolderThumbnail(File folder, String thumbFilename) {
		if (folder == null || !folder.isDirectory()) {
			return null;
		}

		String thumbPrefix = Paths.get(thumbFilename).getFileName().toString() + ".";

		try (DirectoryStream<Path> folderThumbnails = Files.newDirectoryStream(folder.toPath(), (Path entry) -> {
			Path fileNamePath = entry.getFileName();
			if (fileNamePath == null) {
				return false;
			}
			String fileName = fileNamePath.toString();
			String fileNameLower = fileName.toLowerCase(Locale.ROOT);
			if (fileName.startsWith(thumbPrefix) || fileNameLower.contains("albumart")) {
				return isPotentialThumbnail(fileNameLower);
			}
			return false;
		})) {
			for (Path folderThumbnail : folderThumbnails) {
				return folderThumbnail.toFile();
			}
		} catch (IOException e) {
			LOGGER.warn("An error occurred while trying to browse folder \"{}\": {}", folder.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Returns whether or not the specified {@link File} is considered a "folder
	 * thumbnail" by naming convention, using the configured folder thumbnail
	 * names.
	 *
	 * @param file the {@link File} to evaluate.
	 * @param evaluateExtension if {@code true} the file extension will also be
	 *            evaluated in addition to the file name, if {@code false} only
	 *            the file name will be evaluated.
	 * @return {@code true} if {@code file} name matches the naming convention
	 *         for folder thumbnails, {@code false} otherwise.
	 */
	public static boolean isFolderThumbnail(File file, boolean evaluateExtension) {
		return isFolderThumbnail(file, evaluateExtension, PMS.getConfiguration().getFolderThumbnailNames());
	}

	/**
	 * Returns whether or not the specified {@link File} is considered a "folder
	 * thumbnail" by naming convention.
	 *
	 * @param file the {@link File} to evaluate.
	 * @param evaluateExtension if {@code true} the file extension will also be
	 *            evaluated in addition to the file name, if {@code false} only
	 *            the file name will be evaluated.
	 * @param thumbnailNames the file names, without extension, that are
	 *            considered folder thumbnails.
	 * @return {@code true} if {@code file} name matches the naming convention
	 *         for folder thumbnails, {@code false} otherwise.
	 */
	public static boolean isFolderThumbnail(File file, boolean evaluateExtension, List<String> thumbnailNames) {
		if (file == null || !file.isFile()) {
			return false;
		}

		String fileName = file.getName();
		if (StringUtils.isBlank(fileName)) {
			return false;
		}
		if (evaluateExtension && !isPotentialThumbnail(fileName.toLowerCase(Locale.ROOT))) {
			return false;
		}
		return getFolderThumbnailPriority(fileName, thumbnailNames) >= 0;
	}

	/**
	 * Returns how well {@code fileName} matches the folder thumbnail naming
	 * convention. A lower value is a better match.
	 *
	 * @param fileName the file name to evaluate.
	 * @param thumbnailNames the file names, without extension, to look for in
	 *            order of priority.
	 * @return the index in {@code thumbnailNames} that {@code fileName}
	 *         matches, the number of names if it only contains "albumart", or
	 *         {@code -1} if it is not a folder thumbnail.
	 */
	public static int getFolderThumbnailPriority(String fileName, List<String> thumbnailNames) {
		if (StringUtils.isBlank(fileName)) {
			return -1;
		}
		List<String> names = normalizeThumbnailNames(thumbnailNames);
		String fileNameLower = fileName.toLowerCase(Locale.ROOT);
		for (int i = 0; i < names.size(); i++) {
			if (fileNameLower.startsWith(names.get(i) + ".")) {
				return i;
			}
		}
		return fileNameLower.contains("albumart") ? names.size() : -1;
	}

	/**
	 * Trims and lower-cases the names and drops blank and duplicate entries,
	 * keeping the order.
	 */
	private static List<String> normalizeThumbnailNames(List<String> thumbnailNames) {
		List<String> result = new ArrayList<>();
		if (thumbnailNames != null) {
			for (String name : thumbnailNames) {
				String normalized = StringUtils.trimToEmpty(name).toLowerCase(Locale.ROOT);
				if (!normalized.isEmpty() && !result.contains(normalized)) {
					result.add(normalized);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the potential thumbnail resources for the specified audio or
	 * video file as a {@link Set} of {@link File}s.
	 *
	 * @param audioVideoFile the {@link File} for which to enumerate potential
	 *            thumbnail files.
	 * @param existingOnly if {@code true}, files will only be added to the
	 *            returned {@link Set} if they {@link File#exists()}.
	 * @return The {@link Set} of {@link File}s.
	 */
	public static Set<File> getPotentialFileThumbnails(File audioVideoFile, boolean existingOnly) {
		File file;
		Set<File> potentialMatches = new HashSet<>(THUMBNAIL_EXTENSIONS.size() * 2);
		for (String extension : THUMBNAIL_EXTENSIONS) {
			file = FileUtil.replaceExtension(audioVideoFile, extension, false, true);
			if (!existingOnly || file.exists()) {
				potentialMatches.add(file);
			}
			file = new File(audioVideoFile.toString() + ".cover." + extension);
			if (!existingOnly || file.exists()) {
				potentialMatches.add(file);
			}
		}
		return potentialMatches;
	}

	/**
	 * Returns whether or not the specified {@link File} has an extension that
	 * makes it a possible thumbnail file that should be considered a thumbnail
	 * resource belonging to another file or folder.
	 *
	 * @param file the {@link File} to evaluate.
	 * @return {@code true} if {@code file} has one of the predefined
	 *         {@link VirtualFile#THUMBNAIL_EXTENSIONS} extensions,
	 *         {@code false} otherwise.
	 */
	public static boolean isPotentialThumbnail(File file) {
		return file != null && file.isFile() && isPotentialThumbnail(file.getName());
	}

	/**
	 * Returns whether or not {@code fileName} has an extension that makes it a
	 * possible thumbnail file that should be considered a thumbnail resource
	 * belonging to another file or folder.
	 *
	 * @param fileName the file name to evaluate.
	 * @return {@code true} if {@code fileName} has one of the predefined
	 *         {@link VirtualFile#THUMBNAIL_EXTENSIONS} extensions,
	 *         {@code false} otherwise.
	 */
	public static boolean isPotentialThumbnail(String fileName) {
		String extension = FileUtil.getExtension(fileName);
		return extension != null && THUMBNAIL_EXTENSIONS.contains(extension);
	}

	/**
	 * Returns whether {@code fileName} has an extension that is not on our list
	 * of extensions that can't be media files.
	 *
	 * @param fileName the file name to evaluate.
	 * @return {@code true} if {@code fileName} has not the one of the
	 *         predefined {@link VirtualFile#EXTENSIONS_DENYLIST} extensions,
	 *         {@code false} otherwise.
	 */
	public static boolean isPotentialMediaFile(String fileName) {
		String ext = FileUtil.getExtension(fileName);
		if (ext == null) {
			return false;
		}
		return !EXTENSIONS_DENYLIST.contains(ext);
	}

}
