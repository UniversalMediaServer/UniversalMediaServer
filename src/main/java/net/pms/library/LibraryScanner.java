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
package net.pms.library;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import net.pms.database.MediaDatabase;
import net.pms.gui.GuiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryScanner {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryScanner.class);
	private static Thread scannerThread;
	private static MediaScanner mediaScanner;

	private LibraryScanner() {
		//should not be instantiated
	}

	public static boolean isScanLibraryRunning() {
		return scannerThread != null && scannerThread.isAlive();
	}

	public static void scanLibrary() {
		if (isScanLibraryRunning()) {
			LOGGER.info("Cannot start library scanner: A scan is already in progress");
		} else {
			Runnable scan = () -> {
				try {
					if (mediaScanner == null || mediaScanner.getDefaultRenderer() == null) {
						mediaScanner = new MediaScanner();
					}
					if (mediaScanner.getDefaultRenderer() != null) {
						LOGGER.info("Library scan started");
						long start = System.currentTimeMillis();
						mediaScanner.startScan();
						LOGGER.info("Library scan completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
						analyzeDb();
					}
				} catch (Exception e) {
					LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			};
			scannerThread = new Thread(scan, "Library Scanner");
			scannerThread.setPriority(Thread.MIN_PRIORITY);
			scannerThread.start();
			GuiManager.setScanLibraryStatus(true, true);
		}
	}

	public static void scanFileOrFolder(String filename) {
		if (!LibraryScanner.isScanLibraryRunning()) {
			Runnable scan = () -> {
				if (mediaScanner == null || mediaScanner.getDefaultRenderer() == null) {
					mediaScanner = new MediaScanner();
				}
				if (mediaScanner.getDefaultRenderer() != null) {
					mediaScanner.scanFileOrFolder(filename);
				}
			};
			Thread scanThread = new Thread(scan, "rescanLibraryFileOrFolder");
			scanThread.start();
		}
	}

	public static void stopScanLibrary() {
		if (isScanLibraryRunning() && mediaScanner != null) {
			mediaScanner.stopScan();
		}
	}

	private static void analyzeDb() {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				try (Statement stmt = connection.createStatement()) {
					stmt.execute("ANALYZE SAMPLE_SIZE 0");
				}
			}
		} catch (SQLException e) {
			LOGGER.warn("Error analyzing database", e);
		}
	}

}
