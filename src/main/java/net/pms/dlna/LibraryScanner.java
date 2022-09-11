/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryScanner {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryScanner.class);
	private static Thread scanner;

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
				} catch (Exception e) {
					LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			};
			scanner = new Thread(scan, "Library Scanner");
			scanner.setPriority(Thread.MIN_PRIORITY);
			scanner.start();
		}
	}

	public static void stopScanLibrary() {
		if (isScanLibraryRunning()) {
			PMS.get().getRootFolder(null).stopScan();
		}
	}
}
