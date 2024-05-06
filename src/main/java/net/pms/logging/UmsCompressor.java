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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * <code>UmsCompressor</code> extends {@link Compressor} to allow compressor
 * to compress the rolled log file into a single zip file.
 * Use CompressionMode.ZIP only.
 * Allow only 9999 files.
 * Better to use this with {@link SizeBasedTriggeringPolicy} set to 100MB or more
 */
public class UmsCompressor extends Compressor {
	static final int BUFFER_SIZE = 8192;

	/**
	 * All synchronization in this class is done via the lock object.
	 */
	protected final ReentrantLock lock = new ReentrantLock(false);
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
		zipCompress(nameOfFile2Compress, nameOfCompressedFile, innerEntryName);
	}

	/**
	 * Same code from Logback, except it will append to zip if zip exist.
	 * @param nameOfFile2zip
	 * @param nameOfZippedFile
	 * @param innerEntryName
	 */
	private void zipCompress(String nameOfFile2zip, String nameOfZippedFile, String innerEntryName) {
		File file2zip = new File(nameOfFile2zip);

		if (!file2zip.exists()) {
			addWarn("The file to compress named [" + nameOfFile2zip + "] does not exist.");
			return;
		}

		if (innerEntryName == null) {
			addWarn("The innerEntryName parameter cannot be null");
			return;
		}

		if (!nameOfZippedFile.endsWith(".zip")) {
			nameOfZippedFile = nameOfZippedFile + ".zip";
		}
		lock.lock();
		Instant start = Instant.now();
		File zippedFile = new File(nameOfZippedFile);
		File appendZippedFile = new File(nameOfZippedFile + ".tmp");
		if (appendZippedFile.exists()) {
			//something went bad on last zipCompress ?
			FileUtils.deleteQuietly(appendZippedFile);
		}
		if (zippedFile.exists()) {
			//set to append
			try {
				if (zippedFile.renameTo(appendZippedFile)) {
					addInfo("The target compressed file named [" + nameOfZippedFile + "] will be appended.");
				} else {
					addWarn("Error occurred while renaming [" + nameOfZippedFile + "]");
					return;
				}
			} catch (SecurityException e) {
				addWarn("Error occurred while renaming [" + nameOfZippedFile + "]");
				return;
			}
		}

		addInfo("ZIP compressing [" + file2zip + "] as [" + zippedFile + "]");
		createMissingTargetDirsIfNecessary(zippedFile);

		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(nameOfZippedFile))) {
			byte[] inbuf = new byte[BUFFER_SIZE];
			int n;

			// first, copy contents from existing zip
			int index = 0;
			if (appendZippedFile.exists()) {
				try (ZipFile appendZip = new ZipFile(appendZippedFile)) {
					Enumeration<? extends ZipEntry> entries = appendZip.entries();
					while (entries.hasMoreElements()) {
						ZipEntry zipEntry = entries.nextElement();
						zos.putNextEntry(zipEntry);
						if (!zipEntry.isDirectory()) {
							index++;
							InputStream zipEntryStream = appendZip.getInputStream(zipEntry);
							while ((n = zipEntryStream.read(inbuf)) != -1) {
								zos.write(inbuf, 0, n);
							}
						}
						zos.closeEntry();
					}
				}
				FileUtils.deleteQuietly(appendZippedFile);
			}
			//add askef file
			try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(nameOfFile2zip))) {
				//try to keep the extension
				String innerEntryExt = "";
				if (innerEntryName.contains(".")) {
					int dotIndex = innerEntryName.lastIndexOf(".");
					innerEntryExt = innerEntryName.substring(dotIndex);
					innerEntryName = innerEntryName.substring(0, dotIndex);
				}
				//let say we will have no more than 9999 files in the zip
				innerEntryName += "." + String.format("%04d", index) + innerEntryExt;
				ZipEntry zipEntry = computeZipEntry(innerEntryName);
				zos.putNextEntry(zipEntry);

				while ((n = bis.read(inbuf)) != -1) {
					zos.write(inbuf, 0, n);
				}
			}

			if (!file2zip.delete()) {
				addWarn("Could not delete [" + nameOfFile2zip + "].");
			}
			Instant end = Instant.now();
			addInfo("Compressing [" + nameOfFile2zip + "] take " + Duration.between(start, end).toString());
		} catch (IOException e) {
			addError("Error occurred while compressing [" + nameOfFile2zip + "] into [" + nameOfZippedFile + "].", e);
		} finally {
			lock.unlock();
		}
	}

	// http://jira.qos.ch/browse/LBCORE-98
	// The name of the compressed file as nested within the zip archive
	//
	// Case 1: RawFile = null, Pattern = foo-%d.zip
	// nestedFilename = foo-${current-date}
	//
	// Case 2: RawFile = hello.txt, Pattern = = foo-%d.zip
	// nestedFilename = foo-${current-date}
	//
	// in both cases, the strategy consisting of removing the compression
	// suffix of zip file works reasonably well. The alternative strategy
	// whereby the nested file name was based on the value of the raw file name
	// (applicable to case 2 only) has the disadvantage of the nested files
	// all having the same name, which could make it harder for the user
	// to unzip the file without collisions
	ZipEntry computeZipEntry(File zippedFile) {
		return computeZipEntry(zippedFile.getName());
	}

	ZipEntry computeZipEntry(String filename) {
		String nameOfFileNestedWithinArchive = computeFileNameStrWithoutCompSuffix(filename, CompressionMode.ZIP);
		return new ZipEntry(nameOfFileNestedWithinArchive);
	}

	void createMissingTargetDirsIfNecessary(File file) {
		boolean result = FileUtil.createMissingParentDirectories(file);
		if (!result) {
			addError("Failed to create parent directories for [" + file.getAbsolutePath() + "]");
		}
	}
}
