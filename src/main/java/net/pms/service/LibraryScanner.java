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
package net.pms.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.gui.GuiManager;

public class LibraryScanner {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryScanner.class);
	private static Thread scanner;

	private LibraryScanner() {
		//should not be instantiated
	}

	public static boolean isScanLibraryRunning() {
		return scanner != null && scanner.isAlive();
	}

	public static void scanLibrary() {
		if (isScanLibraryRunning()) {
			LOGGER.info("Cannot start library scanner: A scan is already in progress");
		} else {
			Runnable scan = () -> {
				try {
					long start = System.currentTimeMillis();
					PMS.get().getRootFolder(null).startScan();
					LOGGER.info("Library scan completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
					analyzeDb();
				} catch (Exception e) {
					LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			};
			scanner = new Thread(scan, "Library Scanner");
			scanner.setPriority(Thread.MIN_PRIORITY);
			scanner.start();
			GuiManager.setScanLibraryStatus(true, true);
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

	public static void stopScanLibrary() {
		if (isScanLibraryRunning()) {
			PMS.get().getRootFolder(null).stopScan();
		}
	}
}
