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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
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
	 * Default "folder" logic.
	 *
	 * @param folder
	 * @return
	 */
	public static File getFolderThumbnail(File folder) {
		return getFolderThumbnail(folder, "folder");
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
	 * Stores a picture as thumbnail
	 */
	public static File storeThumbnail(File folder, String baseName, DLNAThumbnail thumbnail) throws IOException {
		if (folder == null || StringUtils.isBlank(baseName) || thumbnail == null) {
			return null;
		}
		String extension = thumbnailExtension(thumbnail.getFormat());
		if (extension == null) {
			LOGGER.debug("not storing a cover in {} : {} is no thumbnail format", folder, thumbnail.getFormat());
			return null;
		}
		File cover = new File(folder, baseName + "." + extension);
		Files.write(cover.toPath(), thumbnail.getBytes(false));
		File[] siblings = folder.listFiles();
		if (siblings != null) {
			for (File sibling : siblings) {
				if (sibling.isFile() && !sibling.getName().equalsIgnoreCase(cover.getName()) &&
						isThumbnailOf(sibling.getName(), baseName)) {
					if (!sibling.delete()) {
						LOGGER.warn("cannot remove the previous cover {}", sibling);
					}
				}
			}
		}
		LOGGER.debug("stored a cover as {}", cover);
		return cover;
	}

	/**
	 * Whether the file is a thumbnail spelling of baseName
	 */
	private static boolean isThumbnailOf(String fileName, String baseName) {
		String prefix = baseName + ".";
		if (!fileName.startsWith(prefix)) {
			return false;
		}
		String extension = fileName.substring(prefix.length());
		return THUMBNAIL_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
	}

	/**
	 * The extension a picture of this format may be stored under, or null when no name we could give
	 * it would be recognised as a thumbnail.
	 */
	private static String thumbnailExtension(ImageFormat format) {
		if (format == null) {
			return null;
		}
		return switch (format) {
			case JPEG -> "jpg";
			case PNG -> "png";
			case GIF -> "gif";
			case BMP -> "bmp";
			case WEBP -> "webp";
			default -> null;
		};
	}

	/**
	 * Returns whether or not the specified {@link File} is considered a "folder
	 * thumbnail" by naming convention.
	 *
	 * @param file the {@link File} to evaluate.
	 * @param evaluateExtension if {@code true} the file extension will also be
	 *            evaluated in addition to the file name, if {@code false} only
	 *            the file name will be evaluated.
	 * @return {@code true} if {@code file} name matches the naming convention
	 *         for folder thumbnails, {@code false} otherwise.
	 */
	public static boolean isFolderThumbnail(File file, boolean evaluateExtension) {
		if (file == null || !file.isFile()) {
			return false;
		}

		String fileName = file.getName();
		if (StringUtils.isBlank(fileName)) {
			return false;
		}
		if (evaluateExtension && !isPotentialThumbnail(fileName)) {
			return false;
		}
		fileName = fileName.toLowerCase(Locale.ROOT);
		return fileName.startsWith("folder.") || fileName.contains("albumart");
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
		return THUMBNAIL_EXTENSIONS.contains(FileUtil.getExtension(fileName));
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
