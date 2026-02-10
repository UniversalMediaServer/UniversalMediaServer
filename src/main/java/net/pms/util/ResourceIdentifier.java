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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.openhft.hashing.LongHashFunction;

/**
 * @author Surf@ceS
 */
public class ResourceIdentifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceIdentifier.class);

	private static final int SMALL_BYTES_THRESHOLD = 1024 * 1024 * 100; // 100 MB
	private static final int BIG_BYTES_THRESHOLD = Integer.MAX_VALUE; 	// 2 GB
	private static final long VERY_BIG_FILES_SEGMENT_SIZE = 1024 * 1024 * 256; // 256 MB

	private static final String LOG_RUID_CREATE = "Creating ruid ({}) for \"{}\"";
	private static final String LOG_RUID_RESULTS = "RUID for \"{}\": {}";
	private static final String LOG_RUID_ERROR = "Error creating ruid ({}) for \"{}\"";

	/**
	 * This class is not meant to be instantiated.
	 */
	private ResourceIdentifier() {
	}

	/**
	 * Calculates the XXH3 hash and returns the value as a 16 character hex
	 * string.
	 *
	 * @param uri the file/url to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	public static String getResourceIdentifier(final String uri) {
		if (StringUtils.isBlank(uri)) {
			return null;
		}
		File file = getFile(uri);
		if (file != null && file.exists() && file.isFile()) {
			long fileSize = file.length();
			if (fileSize < SMALL_BYTES_THRESHOLD) {
				return getSmallFileIdentifier(file, fileSize);
			}
			if (fileSize < BIG_BYTES_THRESHOLD) {
				return getBigFileIdentifier(file);
			} else {
				return getVeryBigFileIdentifier(file);
			}
		}
		return getTextIdentifier(uri);
	}

	private static File getFile(final String uri) {
		// try URI
		try {
			return new File(URI.create(uri));
		} catch (IllegalArgumentException es) {
			// not a file URI
		}
		// try path
		try {
			return Path.of(uri).toFile();
		} catch (InvalidPathException e) {
			// not a path
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash and returns the value as a 16 character hex
	 * string. File has to be smaller than 2 GB for correct hashing.
	 *
	 * @param file the file to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	private static String getBigFileIdentifier(final File file) {
		try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			String ruid = Long.toHexString(LongHashFunction.xx3().hashBytes(buffer));
			LOGGER.trace(LOG_RUID_RESULTS, file.getAbsolutePath(), ruid);
			return ruid;
		} catch (IOException ex) {
			LOGGER.error(LOG_RUID_ERROR, "big file", file.getAbsolutePath());
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash for files > 2GB by processing them in segments.
	 * @param file
	 * @return
	 */
	private static String getVeryBigFileIdentifier(final File file) {
		LongHashFunction xxh3 = LongHashFunction.xx3();
		try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			long fileSize = channel.size();

			int numSegments = (int) Math.ceil((double) fileSize / VERY_BIG_FILES_SEGMENT_SIZE);
			LOGGER.trace("getVeryBigFileIdentifier : {}  segments for file: {}", numSegments, file.getAbsolutePath());
			long[] segmentHashes = new long[numSegments];

			for (int i = 0; i < numSegments; i++) {
				long position = i * VERY_BIG_FILES_SEGMENT_SIZE;
				long sizeToRead = Math.min(VERY_BIG_FILES_SEGMENT_SIZE, fileSize - position);

				// Segment mappen
				var buffer = channel.map(FileChannel.MapMode.READ_ONLY, position, sizeToRead);
				segmentHashes[i] = xxh3.hashBytes(buffer);
			}

			String ruid = Long.toHexString(LongHashFunction.xx3().hashLongs(segmentHashes));
			LOGGER.trace(LOG_RUID_RESULTS, file.getAbsolutePath(), ruid);
			return ruid;
		} catch (Exception ex) {
			LOGGER.error(LOG_RUID_ERROR, "very big file", file.getAbsolutePath());
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash and returns the value as a 16 character hex
	 * string.
	 *
	 * Will process the entire file bytes.
	 *
	 * @param file the file to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	private static String getSmallFileIdentifier(final File file, final long fileSize) {
		String pathname = file.getAbsolutePath();
		try {
			String ruid = Long.toHexString(LongHashFunction.xx3().hashBytes(Files.readAllBytes(file.toPath())));
			LOGGER.trace(LOG_RUID_RESULTS, pathname, ruid);
			return ruid;
		} catch (Exception e) {
			LOGGER.error(LOG_RUID_ERROR, "small file", pathname);
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash and returns the value as a 16 character hex
	 * string.
	 *
	 * Will process the entire text bytes.
	 *
	 * @param text the text to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	private static String getTextIdentifier(final String text) {
		try {
			LOGGER.debug(LOG_RUID_CREATE, "text", text);
			long hash = LongHashFunction.xx3().hashChars(text);
			String ruid = Long.toHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, text, ruid);
			return ruid;
		} catch (Exception e) {
			LOGGER.error(LOG_RUID_ERROR, "text", text);
			LOGGER.trace("", e);
		}
		return null;
	}

}
