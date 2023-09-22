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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableLibraryIds;
import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * Get same ids for objects.
 *
 * The ContentDirectory service is recommended to ensure the persistence of
 * the object’s @id property values.
 */
public class LibraryIds {

	private static final Map<Long, UnsignedIntegerFourBytes> UPDATE_IDS = new HashMap<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private LibraryIds() {
	}

	public static synchronized Long getLibraryResourceId(LibraryResource resource) {
		//parse db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				//bump systemUpdateId
				LibraryId libraryId = MediaTableLibraryIds.getResourceLibraryId(connection, resource);
				if (libraryId != null) {
					long id = libraryId.getId();
					resource.setLongId(id);
					if (libraryId.getUpdateId() == 0) {
						//brand new object : set its updateid to next systemUpdateId
						long updateId = incrementUpdateId(id);
						libraryId.setUpdateId(updateId);
					}
					UPDATE_IDS.put(id, new UnsignedIntegerFourBytes(libraryId.getUpdateId()));
					return id;
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return null;
	}

	public static List<LibraryId> getLibraryResourceTree(long id) {
		List<LibraryId> libraryIds = new ArrayList<>();
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				LibraryId libraryId = MediaTableLibraryIds.getLibraryId(connection, id);
				if (libraryId != null) {
					libraryIds.add(libraryId);
					while (libraryId.getParentId() != 0) {
						libraryId = MediaTableLibraryIds.getLibraryId(connection, libraryId.getParentId());
						libraryIds.add(libraryId);
					}
					Collections.reverse(libraryIds);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return libraryIds;
	}

	/**
	 * Returns the updates id for all resources.
	 *
	 * When all resources need to be refreshed, this id is updated.
	 *
	 * @return The system updated id.
	 */
	public static synchronized UnsignedIntegerFourBytes getSystemUpdateId() {
		if (!UPDATE_IDS.containsKey(-1L)) {
			UnsignedIntegerFourBytes value = null;
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					LibraryId libraryId = MediaTableLibraryIds.getLibraryId(connection, -1L);
					if (libraryId != null) {
						value = new UnsignedIntegerFourBytes(libraryId.getUpdateId());
					} else {
						value = new UnsignedIntegerFourBytes(0);
					}
				}
				if (value == null) {
					value = new UnsignedIntegerFourBytes(0);
				}
				UPDATE_IDS.put(-1L, value);
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return UPDATE_IDS.get(-1L);
	}

	/**
	 * Call this method after making changes to your content directory.
	 * <p>
	 * This will notify clients that their view of the content directory is
	 * potentially outdated and has to be refreshed.
	 * </p>
	 */
	public static synchronized void incrementSystemUpdateId() {
		incrementUpdateId(null);
	}

	/**
	 * upnp:objectUpdateID or upnp:containerUpdateID
	 *
	 * -1 or null id mean systemUpdateId.
	 *
	 * @param id
	 * @return
	 */
	public static synchronized Long incrementUpdateId(Long id) {
		long updateId = getSystemUpdateId().increment(false).getValue();
		if (id != null && id != -1 && UPDATE_IDS.containsKey(id)) {
			UPDATE_IDS.put(id, new UnsignedIntegerFourBytes(updateId));
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableLibraryIds.setLibraryUpdateId(connection, -1, updateId);
				if (id != null && id != -1) {
					MediaTableLibraryIds.setLibraryUpdateId(connection, id, updateId);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return updateId;
	}

}
