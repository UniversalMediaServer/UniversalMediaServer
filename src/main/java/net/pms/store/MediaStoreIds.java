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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
	private static final Map<Long, UnsignedIntegerFourBytes> UPDATE_IDS = new ConcurrentHashMap<>();

	/**
	 * Containers whose update id changed since the last ContainerUpdateIDs event went out.
	 */
	private static final Map<Long, UnsignedIntegerFourBytes> CHANGED_IDS = new ConcurrentHashMap<>();
	private static final int MAX_CHANGED_IDS = 64;

	/** Guards the shared system update id counter only, never a database call. */
	private static final Object SYSTEM_UPDATE_ID_LOCK = new Object();

	/**
	 * Looking up the store id of a resource inserts the row when it is missing, so it has to be
	 * atomic per resource. Striping keeps unrelated resources from waiting on each other, which is
	 * what serialised the whole media scan before.
	 */
	private static final int STORE_ID_STRIPES = 64;
	private static final Object[] STORE_ID_LOCKS = new Object[STORE_ID_STRIPES];

	static {
		for (int i = 0; i < STORE_ID_STRIPES; i++) {
			STORE_ID_LOCKS[i] = new Object();
		}
	}

	/**
	 * This class is not meant to be instantiated.
	 */
	private MediaStoreIds() {
	}

	public static Long getMediaStoreResourceId(StoreResource resource) {
		if (resource == null) {
			return null;
		}
		// The connection is taken before the monitor on purpose.
		Connection connection = MediaDatabase.getConnectionIfAvailable();
		try {
			synchronized (getStoreIdLock(resource)) {
				if (connection != null) {
					//parse db
					MediaStoreId mediaStoreId = MediaTableStoreIds.getResourceMediaStoreId(connection, resource);
					if (mediaStoreId != null) {
						long id = mediaStoreId.getId();
						resource.setLongId(id);
						if (mediaStoreId.getUpdateId() == 0) {
							//brand new object : set its updateid to next systemUpdateId, reusing this connection
							long updateId = applyUpdateId(connection, id);
							mediaStoreId.setUpdateId(updateId);
						}
						UPDATE_IDS.put(id, new UnsignedIntegerFourBytes(mediaStoreId.getUpdateId()));
						return id;
					}
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return null;
	}

	private static Object getStoreIdLock(StoreResource resource) {
		Long parentId = resource.getParent() != null ? resource.getParent().getLongId() : null;
		int hash = Objects.hash(parentId, resource.getSystemName());
		return STORE_ID_LOCKS[Math.floorMod(hash, STORE_ID_STRIPES)];
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
	public static UnsignedIntegerFourBytes getSystemUpdateId() {
		UnsignedIntegerFourBytes known = UPDATE_IDS.get(-1L);
		if (known != null) {
			return known;
		}
		UnsignedIntegerFourBytes value = null;
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaStoreId mediaStoreId = MediaTableStoreIds.getMediaStoreId(connection, -1L);
				if (mediaStoreId != null) {
					value = new UnsignedIntegerFourBytes(mediaStoreId.getUpdateId());
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		if (value == null) {
			value = new UnsignedIntegerFourBytes(0);
		}
		// Whoever gets there first defines the counter, so every caller shares the same instance.
		UnsignedIntegerFourBytes previous = UPDATE_IDS.putIfAbsent(-1L, value);
		return previous != null ? previous : value;
	}

	/**
	 * Returns the updates id for an object.
	 *
	 * @return The object updated id.
	 */
	private static UnsignedIntegerFourBytes getObjectUpdateId(Long id) {
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
	public static void incrementSystemUpdateId() {
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
	public static Long incrementUpdateId(Long id) {
		// Same reason as in getMediaStoreResourceId: never wait for a connection under the monitor.
		Connection connection = MediaDatabase.getConnectionIfAvailable();
		try {
			long updateId = nextSystemUpdateId();
			if (id != null && id != -1) {
				UPDATE_IDS.computeIfPresent(id, (key, value) -> new UnsignedIntegerFourBytes(updateId));
				trackChangedId(id, updateId);
			}
			if (connection != null) {
				MediaTableStoreIds.setMediaStoreUpdateIdIfHigher(connection, -1, updateId);
				if (id != null && id != -1) {
					MediaTableStoreIds.setMediaStoreUpdateId(connection, id, updateId);
				}
			}
			return updateId;
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Bumps the given store ids in one go.
	 */
	public static void incrementUpdateIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		Set<Long> distinct = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id != null && id != -1) {
				distinct.add(id);
			}
		}
		if (distinct.isEmpty()) {
			return;
		}
		// Same reason as in getMediaStoreResourceId: never wait for a connection under the monitor.
		Connection connection = MediaDatabase.getConnectionIfAvailable();
		try {
			long highestUpdateId = 0;
			for (Long id : distinct) {
				final long updateId = nextSystemUpdateId();
				UPDATE_IDS.computeIfPresent(id, (key, value) -> new UnsignedIntegerFourBytes(updateId));
				trackChangedId(id, updateId);
				if (connection != null) {
					MediaTableStoreIds.setMediaStoreUpdateId(connection, id, updateId);
				}
				highestUpdateId = updateId;
			}
			// The system id only has to end up at the highest value, so it is written once.
			if (connection != null) {
				MediaTableStoreIds.setMediaStoreUpdateIdIfHigher(connection, -1, highestUpdateId);
			}
			LOGGER.trace("Bumped the update id of {} container(s): {}", distinct.size(), distinct);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Bumps the system update id and persists it for the system and the given store id, reusing the
	 * connection the caller already holds.
	 *
	 * Callers pass a valid id.
	 *
	 * @param connection the db connection, may be NULL when the database is unavailable
	 * @param id the store id to bump
	 * @return the new update id
	 */
	private static long applyUpdateId(Connection connection, long id) {
		long updateId = nextSystemUpdateId();
		UPDATE_IDS.computeIfPresent(id, (key, value) -> new UnsignedIntegerFourBytes(updateId));
		trackChangedId(id, updateId);
		if (connection != null) {
			MediaTableStoreIds.setMediaStoreUpdateIdIfHigher(connection, -1, updateId);
			MediaTableStoreIds.setMediaStoreUpdateId(connection, id, updateId);
		}
		return updateId;
	}

	/**
	 * Bumps the shared counter. Only the arithmetic is guarded, so nothing waits on a database call.
	 */
	private static long nextSystemUpdateId() {
		synchronized (SYSTEM_UPDATE_ID_LOCK) {
			return getSystemUpdateId().increment(false).getValue();
		}
	}

	private static void incrementUpdateId(Connection connection, Long id) {
		if (id != null && id != -1) {
			applyUpdateId(connection, id);
		}
	}


	private static void trackChangedId(long id, long updateId) {
		if (CHANGED_IDS.size() >= MAX_CHANGED_IDS && !CHANGED_IDS.containsKey(id)) {
			return;
		}
		CHANGED_IDS.put(id, new UnsignedIntegerFourBytes(updateId));
	}

	/**
	 * Hands over the containers that changed and removes them, so every change is reported once.
	 */
	public static Map<Long, UnsignedIntegerFourBytes> drainChangedIds() {
		if (CHANGED_IDS.isEmpty()) {
			return Map.of();
		}
		Map<Long, UnsignedIntegerFourBytes> drained = new HashMap<>();
		for (Long id : Set.copyOf(CHANGED_IDS.keySet())) {
			UnsignedIntegerFourBytes updateId = CHANGED_IDS.remove(id);
			if (updateId != null) {
				drained.put(id, updateId);
			}
		}
		return drained;
	}

}
