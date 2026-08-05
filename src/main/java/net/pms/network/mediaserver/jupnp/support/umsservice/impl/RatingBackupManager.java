package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableResourceRatings;
import net.pms.database.MediaTableResourceRatings.ResourceRating;
import net.pms.store.StoreResourceRatings;

/**
 * Backs up and restores user ratings to and from a properties file.
 *
 * Two kinds of entries are written:
 * <ul>
 * <li>legacy entries keyed on the FILES.RUID of an audio file, holding the
 * mirrored AUDIO_METADATA.RATING</li>
 * <li>generic entries keyed on resource:OBJECTTYPE:RESOURCEKEY, holding the
 * rating of any kind of resource : audio and video files as well as folders,
 * playlists and web streams</li>
 * </ul>
 *
 * Backup files written by earlier versions only hold legacy entries and are
 * still restored.
 */
public class RatingBackupManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RatingBackupManager.class.getName());
	private final static String RATINGS_READ = "SELECT FILES.RUID, AUDIO_METADATA.RATING FROM FILES LEFT JOIN AUDIO_METADATA ON FILES.ID = AUDIO_METADATA.FILEID WHERE RATING is not null and RUID is not null";
	private final static String RATINGS_WRITE = "UPDATE AUDIO_METADATA a SET a.RATING = ? WHERE a.FILEID in (SELECT ID from FILES WHERE RUID = ?)";

	/**
	 * Prefix of the generic resource rating entries.
	 */
	private final static String RESOURCE_PREFIX = "resource:";

	/**
	 * Used when the object type of a resource rating is unknown.
	 */
	private final static String UNKNOWN_OBJECT_TYPE = "-";

	public RatingBackupManager() {

	}

	public static void backupRatings() {
		Properties p = new Properties();
		Connection c = MediaDatabase.getConnectionIfAvailable();
		if (c == null) {
			throw new IllegalStateException("Database is not available");
		}
		int items = 0;
		try {
			try (PreparedStatement selectStatement = c.prepareStatement(RATINGS_READ)) {
				try (ResultSet rs = selectStatement.executeQuery()) {
					while (rs.next()) {
						p.put(rs.getString("ruid"), rs.getString("rating"));
						items++;
					}
				}
			} catch (SQLException e) {
				LOGGER.error("backup rating failed", e);
			}

			List<ResourceRating> resourceRatings = MediaTableResourceRatings.getAllRatings(c);
			for (ResourceRating resourceRating : resourceRatings) {
				p.put(getResourceEntryKey(resourceRating), Integer.toString(resourceRating.rating()));
				items++;
			}
		} finally {
			MediaDatabase.close(c);
		}

		String backupFilename = getBackupFilename();
		try (FileOutputStream fs = new FileOutputStream(new File(backupFilename))) {
			DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
			p.store(fs, "ratings backup from " + formatter.format(LocalDateTime.now()));
		} catch (IOException e) {
			LOGGER.error("backup rating failed", e);
		}
		LOGGER.info("save {} items into backup file {} ", items, backupFilename);
	}

	public static void restoreRating() {
		String backupFilename = getBackupFilename();
		File f = new File(backupFilename);
		if (!f.exists()) {
			throw new IllegalStateException("Backup file " + backupFilename + " not present");
		}
		try (FileInputStream fis = new FileInputStream(f)) {
			Properties p = new Properties();
			p.load(fis);
			LOGGER.debug("[restoreRating] read {} items.", p.size());
			int updated = 0;
			int skipped = 0;

			Connection c = MediaDatabase.getConnectionIfAvailable();
			if (c == null) {
				throw new IllegalStateException("Database is not available");
			}
			try {
				for (Object okey : p.keySet()) {
					String key = (String) okey;
					Integer rating = parseRating(p.getProperty(key), key);
					if (rating == null) {
						skipped++;
					} else if (key.startsWith(RESOURCE_PREFIX)) {
						if (restoreResourceRating(c, key, rating)) {
							updated++;
						} else {
							skipped++;
						}
					} else {
						int numUpdates = restoreLegacyRating(c, key, rating);
						if (numUpdates == 1) {
							updated++;
						} else if (numUpdates > 1) {
							LOGGER.info("File exists multiple times on file system. RUID : '{}'.", key);
							updated = updated + numUpdates;
						} else {
							skipped++;
						}
					}
				}
			} finally {
				MediaDatabase.close(c);
			}
			StoreResourceRatings.clearCache();
			LOGGER.info("Updated {} items. Skipped {} items.", updated, skipped);
		} catch (IOException e) {
			LOGGER.error("restore rating failed", e);
		}
	}

	/**
	 * Restores the mirrored rating of an audio file, keyed on its RUID.
	 *
	 * @return the number of updated rows
	 */
	private static int restoreLegacyRating(Connection c, String ruid, Integer rating) {
		try (PreparedStatement updateStatement = c.prepareStatement(RATINGS_WRITE)) {
			updateStatement.setInt(1, rating);
			updateStatement.setString(2, ruid);
			return updateStatement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.warn("restoreRating failed for entry {} ", ruid, e);
			return 0;
		}
	}

	/**
	 * Restores the rating of any kind of resource, keyed on its rating key.
	 *
	 * @return true if the rating was restored
	 */
	private static boolean restoreResourceRating(Connection c, String entryKey, Integer rating) {
		String value = entryKey.substring(RESOURCE_PREFIX.length());
		int separator = value.indexOf(':');
		if (separator < 0) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", entryKey);
			return false;
		}
		String objectType = value.substring(0, separator);
		String resourceKey = value.substring(separator + 1);
		if (resourceKey.isEmpty()) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", entryKey);
			return false;
		}
		MediaTableResourceRatings.setRating(
			c,
			resourceKey,
			UNKNOWN_OBJECT_TYPE.equals(objectType) ? null : objectType,
			rating
		);
		return true;
	}

	private static String getResourceEntryKey(ResourceRating resourceRating) {
		String objectType = resourceRating.objectType();
		if (objectType == null || objectType.isEmpty()) {
			objectType = UNKNOWN_OBJECT_TYPE;
		}
		return RESOURCE_PREFIX + objectType + ":" + resourceRating.resourceKey();
	}

	private static Integer parseRating(String rating, String key) {
		if (rating == null) {
			LOGGER.warn("restoreRating skipped entry {} without a value", key);
			return null;
		}
		try {
			return Integer.valueOf(rating.trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("restoreRating skipped entry {} with an unparsable value \"{}\"", key, rating);
			return null;
		}
	}

	private static String getBackupFilename() {
		String dir = FilenameUtils.concat(UmsConfiguration.getProfileDirectory(), "database_backup");
		File mydir = new File(dir);
		if (!mydir.exists()) {
			mydir.mkdirs();
		}
		String backupFilename = FilenameUtils.concat(dir, "ratings_backup");
		return backupFilename;
	}

}
