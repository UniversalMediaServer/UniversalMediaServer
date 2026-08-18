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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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

	/**
	 * Number of bytes taken from the beginning and from the end of a file that is
	 * not hashed completely.
	 */
	private static final int CHUNK_BYTES = 1024 * 256; // 256 kB

	/**
	 * Files up to this size are hashed completely, larger ones by size plus their
	 * head and tail chunk.
	 */
	private static final long WHOLE_FILE_THRESHOLD = 2L * CHUNK_BYTES;

	private static final String LOG_RUID_CREATE = "Creating ruid ({}) for \"{}\"";
	private static final String LOG_RUID_RESULTS = "RUID for \"{}\": {}";
	private static final String LOG_RUID_ERROR = "Error creating ruid ({}) for \"{}\"";

	/**
	 * This class is not meant to be instantiated.
	 */
	private ResourceIdentifier() {
	}

	/**
	 * Calculates the XXH3 hash and returns the value as hex string.
	 *
	 * @param uri the file/url to identify
	 * @return the pseudo unique file identifier.
	 */
	public static String getResourceIdentifier(final String uri) {
		if (StringUtils.isBlank(uri)) {
			return null;
		}
		File file = getFile(uri);
		if (file != null && file.isFile()) {
			return getFileIdentifier(file);
		}
		return getTextIdentifier(uri);
	}

	private static File getFile(final String uri) {
		// The scheme decides how to read it. Trying URI.create() first, as it was done
		// before, threw for every plain path containing a character that is not allowed
		// in a URI - a space for example - which happens for most files of a library.
		if (StringUtils.startsWithIgnoreCase(uri, "file:")) {
			try {
				return new File(URI.create(uri));
			} catch (IllegalArgumentException e) {
				// not a usable file URI
			}
		} else if (!uri.contains("://")) {
			try {
				return Path.of(uri).toFile();
			} catch (InvalidPathException e) {
				// not a path
			}
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash of a file and returns the value as hex string.
	 *
	 * Files larger than {@link #WHOLE_FILE_THRESHOLD} are identified by their size
	 * plus a chunk from the beginning and one from the end. Hashing every byte made
	 * the initial scan read the whole library, which dominated its runtime.
	 *
	 * The content is mapped through the Foreign Function &amp; Memory API, so nothing
	 * is copied to the heap and files larger than 2 GB are handled as well.
	 *
	 * @param file the file to identify
	 * @return the pseudo unique file identifier.
	 */
	private static String getFileIdentifier(final File file) {
		try (Arena arena = Arena.ofConfined(); FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			LongHashFunction xx3 = LongHashFunction.xx3();
			long size = channel.size();
			long hash;
			if (size == 0) {
				hash = xx3.hashVoid();
			} else if (size <= WHOLE_FILE_THRESHOLD) {
				MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, size, arena);
				hash = xx3.hashMemory(segment.address(), size);
			} else {
				MemorySegment head = channel.map(FileChannel.MapMode.READ_ONLY, 0, CHUNK_BYTES, arena);
				MemorySegment tail = channel.map(FileChannel.MapMode.READ_ONLY, size - CHUNK_BYTES, CHUNK_BYTES, arena);
				hash = xx3.hashLongs(new long[] {
					size,
					xx3.hashMemory(head.address(), CHUNK_BYTES),
					xx3.hashMemory(tail.address(), CHUNK_BYTES)
				});
			}
			String ruid = Long.toHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, file.getAbsolutePath(), ruid);
			return ruid;
		} catch (Exception ex) {
			LOGGER.error(LOG_RUID_ERROR, "file", file.getAbsolutePath());
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * Calculates the XXH3 hash and returns the value as 16 character hex string.
	 *
	 * Will process the entire text bytes.
	 *
	 * @param text the text to identify
	 * @return the pseudo unique file identifier.
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
