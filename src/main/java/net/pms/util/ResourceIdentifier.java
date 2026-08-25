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
import java.net.URI;
import java.nio.channels.FileChannel;
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

	private static final int SEGMENTS = 2;
	private static final int SEGMENT_BYTES = 1024 * 128;
	private static final long WHOLE_FILE_THRESHOLD = (long) SEGMENTS * SEGMENT_BYTES;

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
			return getFileIdentifier(file);
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
	 * Calculates the XXH3 hash of a file and returns the value as hex string.
	 *
	 * Files larger than WHOLE_FILE_THRESHOLD are identified by their size plus #SEGMENTS, the first one
	 * at the beginning, the last one at the end and the others spread evenly in between.
	 *
	 * @param file the file to identify
	 * @return the pseudo unique file identifier.
	 */
	private static String getFileIdentifier(final File file) {
		String pathname = file.getAbsolutePath();
		try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			LongHashFunction xx3 = LongHashFunction.xx3();
			long size = fileChannel.size();
			long hash;
			if (size == 0) {
				hash = xx3.hashVoid();
			} else if (size <= WHOLE_FILE_THRESHOLD) {
				hash = xx3.hashBytes(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size));
			} else {
				long[] hashes = new long[SEGMENTS + 1];
				hashes[0] = size;
				long lastOffset = size - SEGMENT_BYTES;
				for (int i = 0; i < SEGMENTS; i++) {
					long offset = lastOffset * i / (SEGMENTS - 1);
					hashes[i + 1] = xx3.hashBytes(fileChannel.map(FileChannel.MapMode.READ_ONLY, offset, SEGMENT_BYTES));
				}
				hash = xx3.hashLongs(hashes);
			}
			String ruid = Long.toHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, pathname, ruid);
			return ruid;
		} catch (Exception ex) {
			LOGGER.error(LOG_RUID_ERROR, "file", pathname);
			LOGGER.trace("", ex);
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