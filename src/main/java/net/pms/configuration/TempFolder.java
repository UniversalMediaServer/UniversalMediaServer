package net.pms.configuration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles finding a temporary folder.
 * 
 * @author Tim Cox (mail@tcox.org)
 */
class TempFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(TempFolder.class);
	private static final String DEFAULT_TEMP_FOLDER_NAME = "ps3mediaserver";
	private final String userSpecifiedFolder;
	private File tempFolder;

	/**
	 * userSpecifiedFolder may be null
	 */
	public TempFolder(String userSpecifiedFolder) {
		this.userSpecifiedFolder = userSpecifiedFolder;
	}

	public synchronized File getTempFolder() throws IOException {
		if (tempFolder == null) {
			tempFolder = getTempFolder(userSpecifiedFolder);
		}

		return tempFolder;
	}

	private File getTempFolder(String userSpecifiedFolder) throws IOException {
		if (userSpecifiedFolder == null) {
			return getSystemTempFolder();
		}

		try {
			return getUserSpecifiedTempFolder(userSpecifiedFolder);
		} catch (IOException e) {
			LOGGER.error("Problem with user specified temp directory - using system", e);
			return getSystemTempFolder();
		}
	}

	private File getUserSpecifiedTempFolder(String userSpecifiedFolder) throws IOException {
		if (userSpecifiedFolder != null && userSpecifiedFolder.length() == 0) {
			throw new IOException("temporary folder path must not be empty if specified");
		}

		File folderFile = new File(userSpecifiedFolder);
		FileUtils.forceMkdir(folderFile);
		assertFolderIsValid(folderFile);
		return folderFile;
	}

	private static File getSystemTempFolder() throws IOException {
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		File myTMP = new File(tmp, DEFAULT_TEMP_FOLDER_NAME);
		FileUtils.forceMkdir(myTMP);
		assertFolderIsValid(myTMP);
		return myTMP;
	}

	private static void assertFolderIsValid(File folder) throws IOException {
		if (!folder.isDirectory()) {
			throw new IOException("Temp folder must be a folder: " + folder);
		}

		if (!folder.canWrite()) {
			throw new IOException("Temp folder is not writable: " + folder);
		}
	}
}
