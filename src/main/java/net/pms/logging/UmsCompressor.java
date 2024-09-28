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
package net.pms.logging;

import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>UmsCompressor</code> extends {@link Compressor} to allow compressor to
 * compress the rolled log file into a single zip file.
 *
 * Use CompressionMode.ZIP only. Allow only 9999 files. Better to use this with
 * {@link SizeBasedTriggeringPolicy} set to 100MB or more
 */
public class UmsCompressor extends Compressor {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsCompressor.class);

	/**
	 * All synchronization in this class is done via the lock object.
	 */
	private final ReentrantLock lock = new ReentrantLock(false);

	public UmsCompressor() {
		super(CompressionMode.ZIP);
	}

	/**
	 * @param nameOfFile2Compress
	 * @param nameOfCompressedFile
	 * @param innerEntryName The name of the file within the zip file. Use for
	 * ZIP compression.
	 */
	@Override
	public void compress(String nameOfFile2Compress, String nameOfCompressedFile, String innerEntryName) {
		nioZipCompress(nameOfFile2Compress, nameOfCompressedFile, innerEntryName);
	}

	private void nioZipCompress(String nameOfFile2Compress, String nameOfCompressedFile, String innerEntryName) {
		File file2zip = new File(nameOfFile2Compress);

		if (!file2zip.exists()) {
			addWarn("The file to compress named [" + nameOfFile2Compress + "] does not exist.");
			return;
		}

		if (innerEntryName == null) {
			addWarn("The innerEntryName parameter cannot be null");
			return;
		}

		if (!nameOfCompressedFile.endsWith(".zip")) {
			nameOfCompressedFile = nameOfCompressedFile + ".zip";
		}
		lock.lock();
		Instant start = Instant.now();
		File zippedFile = new File(nameOfCompressedFile);
		createMissingTargetDirsIfNecessary(zippedFile);
		URI uri = URI.create("jar:" + zippedFile.toURI());
		addInfo("The target compressed file named [" + nameOfCompressedFile + "] will be appended.");
		try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of("create", "true"))) {
			Path e = fs.getRootDirectories().iterator().next();
			long count;
			try (Stream<Path> paths = Files.find(e.getRoot(), 1, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile())) {
				count = paths.count();
			}
			Path source = file2zip.toPath();
			innerEntryName = getIndexedInnerEntryName(innerEntryName, count);
			Path pathInZipfile = fs.getPath(innerEntryName);
			addInfo("ZIP compressing [" + file2zip + "] as [" + innerEntryName + "]");
			Files.copy(source, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
			Instant end = Instant.now();
			LOGGER.info("Compressing [" + nameOfFile2Compress + "] take " + Duration.between(start, end).toString());
			if (!FileUtils.deleteQuietly(file2zip)) {
				LOGGER.warn("Could not delete [" + nameOfFile2Compress + "].");
			}
		} catch (IOException e) {
			addError("Error occurred while compressing [" + nameOfFile2Compress + "] into [" + nameOfCompressedFile + "].", e);
		} finally {
			lock.unlock();
		}
	}

	private void createMissingTargetDirsIfNecessary(File file) {
		boolean result = FileUtil.createMissingParentDirectories(file);
		if (!result) {
			addError("Failed to create parent directories for [" + file.getAbsolutePath() + "]");
		}
	}

	private static String getIndexedInnerEntryName(String innerEntryName, long index) {
		String innerEntryExt = "";
		if (innerEntryName.contains(".")) {
			int dotIndex = innerEntryName.lastIndexOf(".");
			innerEntryExt = innerEntryName.substring(dotIndex);
			innerEntryName = innerEntryName.substring(0, dotIndex);
		}
		//let say we will have no more than 9999 files in the zip
		innerEntryName += "." + String.format("%04d", index) + innerEntryExt;
		return innerEntryName;
	}

}
