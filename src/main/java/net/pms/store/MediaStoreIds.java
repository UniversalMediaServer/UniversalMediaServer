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
package net.pms.store;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableStoreIds;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get same ids for objects.
 *
 * The ContentDirectory service is recommended to ensure the persistence of
 * the object’s @id property values.
 */
public class MediaStoreIds {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaStoreIds.class);
	private static final Map<Long, UnsignedIntegerFourBytes> UPDATE_IDS = new HashMap<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private MediaStoreIds() {
	}

	public static synchronized Long getMediaStoreResourceId(StoreResource resource) {
		if (resource == null) {
			return null;
		}
		//parse db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				//bump systemUpdateId
				MediaStoreId mediaStoreId = MediaTableStoreIds.getResourceMediaStoreId(connection, resource);
				if (mediaStoreId != null) {
					long id = mediaStoreId.getId();
					resource.setLongId(id);
					if (mediaStoreId.getUpdateId() == 0) {
						//brand new object : set its updateid to next systemUpdateId
						long updateId = incrementUpdateId(id);
						mediaStoreId.setUpdateId(updateId);
					}
					UPDATE_IDS.put(id, new UnsignedIntegerFourBytes(mediaStoreId.getUpdateId()));
					return id;
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return null;
	}

	public static List<MediaStoreId> getMediaStoreResourceTree(long id) {
		List<MediaStoreId> mediaStoreIds = new ArrayList<>();
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaStoreId mediaStoreId = MediaTableStoreIds.getMediaStoreId(connection, id);
				if (mediaStoreId != null) {
					mediaStoreIds.add(mediaStoreId);
					while (mediaStoreId.getParentId() != 0) {
						mediaStoreId = MediaTableStoreIds.getMediaStoreId(connection, mediaStoreId.getParentId());
						mediaStoreIds.add(mediaStoreId);
						if (mediaStoreIds.size() > 100) {
							LOGGER.trace("MediaStore path is more than 100 entries, something was wrong");
							return new ArrayList<>();
						}
					}
					Collections.reverse(mediaStoreIds);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return mediaStoreIds;
	}

	public static List<Long> getMediaStoreIdsForName(String name) {
		List<Long> ids = new ArrayList<>();
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				ids = MediaTableStoreIds.getMediaStoreIdsForName(connection, name);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return ids;
	}

	public static String getMediaStoreNameForId(String id) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				return MediaTableStoreIds.getMediaStoreNameForId(connection, id);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return null;
	}

	public static List<Long> getMediaStoreIdsForName(String name, String objectType) {
		List<Long> ids = new ArrayList<>();
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				ids = MediaTableStoreIds.getMediaStoreIdsForName(connection, name, objectType);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return ids;
	}

	public static List<Long> getMediaStoreIdsForName(String name, Class<? extends StoreResource> storeResourceClass) {
		return getMediaStoreIdsForName(name, storeResourceClass.getSimpleName());
	}

	public static List<Long> getMediaStoreIdsForName(String name, String objectType, String parentType) {
		List<Long> ids = new ArrayList<>();
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				ids = MediaTableStoreIds.getMediaStoreIdsForName(connection, name, objectType, parentType);
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return ids;
	}

	public static List<Long> getMediaStoreIdsForName(String name, Class<? extends StoreResource> storeResourceClass, Class<? extends StoreContainer> parentResourceClass) {
		return getMediaStoreIdsForName(name, storeResourceClass.getSimpleName(), parentResourceClass.getSimpleName());
	}

	public static void incrementUpdateIdForFilename(Connection connection, String filename) {
		List<Long> ids = MediaTableStoreIds.getMediaStoreIdsForName(connection, filename);
		for (Long id : ids) {
			incrementUpdateId(connection, id);
		}
	}

	public static void incrementUpdateIdForFilename(String filename) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				incrementUpdateIdForFilename(connection, filename);
			}
		} finally {
			MediaDatabase.close(connection);
		}
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
					MediaStoreId mediaStoreId = MediaTableStoreIds.getMediaStoreId(connection, -1L);
					if (mediaStoreId != null) {
						value = new UnsignedIntegerFourBytes(mediaStoreId.getUpdateId());
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
	 * Returns the updates id for an object.
	 *
	 * @return The object updated id.
	 */
	private static synchronized UnsignedIntegerFourBytes getObjectUpdateId(Long id) {
		if (id == null || id == -1) {
			return getSystemUpdateId();
		}
		if (!UPDATE_IDS.containsKey(id)) {
			UnsignedIntegerFourBytes value = null;
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaStoreId mediaStoreId = MediaTableStoreIds.getMediaStoreId(connection, id);
					if (mediaStoreId != null && mediaStoreId.getUpdateId() != 0) {
						value = new UnsignedIntegerFourBytes(mediaStoreId.getUpdateId());
					}
				}
				if (value == null) {
					value = getSystemUpdateId();
				}
				UPDATE_IDS.put(id, value);
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return UPDATE_IDS.get(id);
	}

	/**
	 * Returns the updates id for an object as string.
	 *
	 * @return The object updated id as string.
	 */
	public static String getObjectUpdateIdAsString(Long id) {
		UnsignedIntegerFourBytes result = getObjectUpdateId(id);
		if (result == null) {
			return null;
		}
		return result.toString();
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
				MediaTableStoreIds.setMediaStoreUpdateId(connection, -1, updateId);
				if (id != null && id != -1) {
					MediaTableStoreIds.setMediaStoreUpdateId(connection, id, updateId);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return updateId;
	}

	private static synchronized void incrementUpdateId(Connection connection, Long id) {
		if (id != null && id != -1) {
			long updateId = getSystemUpdateId().increment(false).getValue();
			if (UPDATE_IDS.containsKey(id)) {
				UPDATE_IDS.put(id, new UnsignedIntegerFourBytes(updateId));
			}
			if (connection != null) {
				MediaTableStoreIds.setMediaStoreUpdateId(connection, -1, updateId);
				MediaTableStoreIds.setMediaStoreUpdateId(connection, id, updateId);
			}
		}
	}

}
