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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.Blake3;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class ResourceIdentifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceIdentifier.class);

	/**
	 * The number of buffer block to analyze.
	 *
	 * ex : 1024 will analyze file block of 4194304 bytes (4 MB)
	 */
	private static final int BUFFER_COUNT = 1024;
	/**
	 * The number of file block to analyze.
	 *
	 * Should not be less than 2 (start/end of the file).
	 */
	private static final int BLOCKS_COUNT = 5;

	private static final int BLOCKS_DIVIDER = BLOCKS_COUNT - 1;
	private static final int BUFFER_SIZE = 4096;
	private static final int PROCESS_BYTES = BUFFER_SIZE * BUFFER_COUNT * BLOCKS_COUNT;
	private static final String LOG_RUID_CREATE = "Creating ruid ({}) for \"{}\"";
	private static final String LOG_RUID_RESULTS = "RUID for \"{}\": {}";
	private static final String LOG_RUID_ERROR = "Error creating ruid ({}) for \"{}\"";

	/**
	 * This class is not meant to be instantiated.
	 */
	private ResourceIdentifier() {
	}

	/**
	 * Calculates the Blake3 digest and returns the value as a 32 character hex
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
			if (fileSize > PROCESS_BYTES) {
				return getBigFileIdentifier(file, fileSize);
			} else {
				return getSmallFileIdentifier(file, fileSize);
			}
		}
		return getTextIdentifier(uri);
	}

	private static File getFile(final String uri) {
		try {
			return Path.of(uri).toFile();
		} catch (InvalidPathException e) {
			//not a path
		}
		//try URI
		try {
			return new File(URI.create(uri));
		} catch (IllegalArgumentException es) {
			//not a file URI
		}
		return null;
	}

	/**
	 * Calculates the Blake3 digest and returns the value as a 32 character hex
	 * string.
	 *
	 * Will process 5 blocks of 4Mb including start and end files bytes.
	 *
	 * @param file the file to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	private static String getBigFileIdentifier(final File file, final long fileSize) {
		String pathname = file.getAbsolutePath();
		LOGGER.debug(LOG_RUID_CREATE, "big file", pathname);
		Blake3 blake3 = Blake3.initHash();
		try (InputStream is = Files.newInputStream(file.toPath())) {
			//put file size
			blake3.update(ByteBuffer.wrap(new byte[8]).putLong(fileSize).array());
			//we will hash 4 block of the file
			long skipper = (fileSize - PROCESS_BYTES) / BLOCKS_DIVIDER;
			int count = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while ((read = is.read(buffer)) != -1) {
				blake3.update(buffer, 0, read);
				count++;
				if (count == BUFFER_COUNT) {
					//we proceed 4 Mb, move to next block
					count = 0;
					is.skip(skipper);
				}
			}
			byte[] hash = new byte[32];
			blake3.doFinalize(hash);
			String ruid = Hex.encodeHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, pathname, ruid);
			return ruid;
		} catch (IOException ex) {
			LOGGER.error(LOG_RUID_ERROR, "big file", pathname);
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * Calculates the Blake3 digest and returns the value as a 32 character hex
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
		LOGGER.debug(LOG_RUID_CREATE, "small file", pathname);
		Blake3 blake3 = Blake3.initHash();
		try (InputStream is = Files.newInputStream(file.toPath())) {
			//put file size
			blake3.update(ByteBuffer.wrap(new byte[8]).putLong(fileSize).array());
			byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while ((read = is.read(buffer)) != -1) {
				blake3.update(buffer, 0, read);
			}
			byte[] hash = new byte[32];
			blake3.doFinalize(hash);
			String ruid = Hex.encodeHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, pathname, ruid);
			return ruid;
		} catch (IOException ex) {
			LOGGER.error(LOG_RUID_ERROR, "small file", pathname);
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * Calculates the Blake3 digest and returns the value as a 32 character hex
	 * string.
	 *
	 * Will process the entire text bytes.
	 *
	 * @param text the text to identify
	 * @return the pseudo unique file identifier.
	 * @throws IOException
	 */
	private static String getTextIdentifier(final String text) {
		LOGGER.debug(LOG_RUID_CREATE, "text", text);
		Blake3 blake3 = Blake3.initHash();
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		try (InputStream is = new ByteArrayInputStream(bytes)) {
			//put bytes size
			blake3.update(ByteBuffer.wrap(new byte[8]).putLong(bytes.length).array());
			byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while ((read = is.read(buffer)) != -1) {
				blake3.update(buffer, 0, read);
			}
			byte[] hash = new byte[32];
			blake3.doFinalize(hash);
			String ruid = Hex.encodeHexString(hash);
			LOGGER.trace(LOG_RUID_RESULTS, text, ruid);
			return ruid;
		} catch (IOException ex) {
			LOGGER.error(LOG_RUID_ERROR, "text", text);
			LOGGER.trace("", ex);
		}
		return null;
	}

}
