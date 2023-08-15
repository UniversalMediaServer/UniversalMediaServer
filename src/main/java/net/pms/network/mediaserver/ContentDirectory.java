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
package net.pms.network.mediaserver;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableMetadata;
import net.pms.util.Debouncer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentDirectory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectory.class);
	private static final Debouncer DEBOUNCER = new Debouncer();
	private static final ReentrantReadWriteLock LOCK_SYSTEM_UPDATE_ID = new ReentrantReadWriteLock();
	private static int systemUpdateId = 0;
	private static boolean hasFetchedSystemUpdateIdFromDatabase = false;
	// maximum characters for UI4 (Unsigned Integer 4-bytes)
	protected static final int MAX_UI4_VALUE = 2147483647;

	protected static final String METADATA_TABLE_KEY_SYSTEMUPDATEID = "SystemUpdateID";

	/**
	 * This class is not meant to be instantiated.
	 */
	protected ContentDirectory() {
	}

	/**
	 * Returns the updates id for all resources. When all resources need to be
	 * refreshed, this id is updated.
	 *
	 * @return The system updated id.
	 * @since 1.50
	 */
	public static int getSystemUpdateId() {
		LOCK_SYSTEM_UPDATE_ID.readLock().lock();
		try {
			return systemUpdateId;
		} finally {
			LOCK_SYSTEM_UPDATE_ID.readLock().unlock();
		}
	}

	/**
	 * Bumps the updated id for all resources. When any resources has been
	 * changed this id should be bumped, debounced by 300ms
	 */
	public static void bumpSystemUpdateId() {
		DEBOUNCER.debounce(Void.class, () -> {
			LOCK_SYSTEM_UPDATE_ID.writeLock().lock();
			Connection connection = null;
			try {
				if (PMS.getConfiguration().getUseCache()) {
					connection = MediaDatabase.getConnectionIfAvailable();
				}
				// Get the current value from the database if we haven't yet since UMS was started
				if (connection != null && !hasFetchedSystemUpdateIdFromDatabase) {
					String systemUpdateIdFromDb = MediaTableMetadata.getMetadataValue(connection, METADATA_TABLE_KEY_SYSTEMUPDATEID);
					try {
						systemUpdateId = Integer.parseInt(systemUpdateIdFromDb);
					} catch (NumberFormatException ex) {
						LOGGER.debug("" + ex);
					}
					hasFetchedSystemUpdateIdFromDatabase = true;
				}

				systemUpdateId++;

				// if we exceeded the maximum value for a UI4, start again at 0
				if (systemUpdateId > MAX_UI4_VALUE) {
					systemUpdateId = 0;
				}

				// Persist the new value to the database
				if (connection != null) {
					MediaTableMetadata.setOrUpdateMetadataValue(connection, METADATA_TABLE_KEY_SYSTEMUPDATEID, Integer.toString(systemUpdateId));
				}
			} finally {
				MediaDatabase.close(connection);
				LOCK_SYSTEM_UPDATE_ID.writeLock().unlock();
			}
		}, 300, TimeUnit.MILLISECONDS);
	}

}
