package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableResourceRatings;
import net.pms.database.MediaTableResourceRatings.ResourceRating;
import net.pms.store.StoreResourceRatings;
import net.pms.util.RelativeMediaPath;
import net.pms.util.RelativeMediaPath.Relative;

/**
 * Backs up and restores user ratings to and from a properties file.
 *
 * Add portable rating, so the restore has more than the absolute path to match on :
 *
 * - RESOURCE_PREFIX : the absolute path, as it was at backup time
 * - PORTABLE_PREFIX : the path relative to the shared folder holding it, which survives a changed mount point
 * - CONTENT_PREFIX : the content derived FILES.RUID of a file, which survives a move or a rename inside the library
 *
 * The shared folders in place at backup time are written as SHARED_ROOT_PREFIX entries, so that an unchanged setup
 * resolves back to the very same paths.
 */
public class RatingBackupManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RatingBackupManager.class.getName());

	private final static String RESOURCE_PREFIX = "resource:";
	private final static String PORTABLE_PREFIX = "portable:";
	private final static String CONTENT_PREFIX = "content:";
	private final static String SHARED_ROOT_PREFIX = "sharedroot:";
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
		int portableItems = 0;
		int contentItems = 0;
		List<File> sharedFolders = SharedContentConfiguration.getSharedFolders();
		Set<Integer> usedRoots = new HashSet<>();
		try {
			List<ResourceRating> resourceRatings = MediaTableResourceRatings.getAllRatings(c);
			for (ResourceRating resourceRating : resourceRatings) {
				String objectType = getObjectType(resourceRating.objectType());
				String rating = Integer.toString(resourceRating.rating());
				String resourceKey = resourceRating.resourceKey();
				p.put(RESOURCE_PREFIX + objectType + ":" + resourceKey, rating);
				items++;
				if (!RelativeMediaPath.isFileSystemPath(resourceKey)) {
					//already independent of any mount point
					continue;
				}
				Relative relative = RelativeMediaPath.relativize(resourceKey, sharedFolders);
				if (relative != null) {
					p.put(PORTABLE_PREFIX + objectType + ":" + relative.rootIndex() + ":" + relative.path(), rating);
					usedRoots.add(relative.rootIndex());
					portableItems++;
				} else {
					LOGGER.debug("[backupRatings] \"{}\" is not below a shared folder, its rating is bound to that path", resourceKey);
				}
				String resourceUid = MediaTableFiles.getResourceUidForFilename(c, resourceKey);
				if (resourceUid != null) {
					p.put(CONTENT_PREFIX + objectType + ":" + resourceUid, rating);
					contentItems++;
				}
			}
			for (Integer rootIndex : usedRoots) {
				p.put(SHARED_ROOT_PREFIX + rootIndex, sharedFolders.get(rootIndex).getAbsolutePath());
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
			throw new UncheckedIOException("writing the backup file failed : " + backupFilename, e);
		}
		LOGGER.info("save {} items into backup file {} ", items, backupFilename);
		LOGGER.info("{} ratings can be restored below another mount point, {} after a move of the file", portableItems, contentItems);
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

			Connection c = MediaDatabase.getConnectionIfAvailable();
			if (c == null) {
				throw new IllegalStateException("Database is not available");
			}
			RestoreCounters counters = new RestoreCounters();
			boolean autoCommit = true;
			try {
				autoCommit = c.getAutoCommit();
				c.setAutoCommit(false);
				Set<String> restored;
				try (Writers writers = new Writers(c)) {
					restored = restoreRatings(c, p, counters, writers);
				}
				counters.deleted = MediaTableResourceRatings.deleteRatingsNotIn(c, restored);
				c.commit();
			} catch (SQLException e) {
				rollback(c);
				LOGGER.error("restore rating failed, nothing was changed", e);
				return;
			} finally {
				resetAutoCommit(c, autoCommit);
				MediaDatabase.close(c);
			}
			StoreResourceRatings.clearCache();
			LOGGER.info("Updated {} items. Skipped {} items. Deleted {} items.", counters.updated, counters.skipped, counters.deleted);
			LOGGER.info("Restored {} ratings on their stored path, {} on the content of a moved file, {} below another mount point. " +
				"{} stored paths do not exist any more.",
				counters.byPath, counters.byContent, counters.byRelativePath, counters.missingPaths);
		} catch (IOException e) {
			LOGGER.error("restore rating failed", e);
		}
	}

	/*
	 * the statements of a whole restore, prepared once instead of per entry
	 */
	private static final class Writers implements AutoCloseable {

		private final MediaTableResourceRatings.RatingWriter ratings;

		private Writers(Connection c) throws SQLException {
			ratings = new MediaTableResourceRatings.RatingWriter(c);
		}

		@Override
		public void close() {
			ratings.close();
		}
	}

	private static void rollback(Connection c) {
		try {
			c.rollback();
		} catch (SQLException e) {
			LOGGER.error("rollback failed", e);
		}
	}

	private static void resetAutoCommit(Connection c, boolean autoCommit) {
		try {
			c.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			LOGGER.error("could not reset autoCommit", e);
		}
	}

	/**
	 * Restores every entry of a backup file, absolute paths first.
	 */
	private static Set<String> restoreRatings(Connection c, Properties p, RestoreCounters counters, Writers writers) {
		Map<Integer, String> recordedRoots = new HashMap<>();
		List<Entry> resourceEntries = new ArrayList<>();
		List<Entry> contentEntries = new ArrayList<>();
		List<Entry> portableEntries = new ArrayList<>();

		for (Object okey : p.keySet()) {
			String key = (String) okey;
			if (key.startsWith(SHARED_ROOT_PREFIX)) {
				Integer rootIndex = parseInteger(key.substring(SHARED_ROOT_PREFIX.length()), key);
				if (rootIndex != null) {
					recordedRoots.put(rootIndex, p.getProperty(key));
				}
				continue;
			}
			Integer rating = parseInteger(p.getProperty(key), key);
			if (rating == null) {
				counters.skipped++;
			} else if (key.startsWith(RESOURCE_PREFIX)) {
				addEntry(resourceEntries, key, RESOURCE_PREFIX, rating, counters);
			} else if (key.startsWith(CONTENT_PREFIX)) {
				addEntry(contentEntries, key, CONTENT_PREFIX, rating, counters);
			} else if (key.startsWith(PORTABLE_PREFIX)) {
				addEntry(portableEntries, key, PORTABLE_PREFIX, rating, counters);
			} else {
				counters.obsoleteFormat++;
				counters.skipped++;
			}
		}

		if (counters.obsoleteFormat > 0) {
			LOGGER.warn("[restoreRating] ignored {} entries in the obsolete \"ruid=rating\" format. They were written by a UMS " +
				"that kept ratings in AUDIO_METADATA. {}", counters.obsoleteFormat,
				resourceEntries.isEmpty() && contentEntries.isEmpty() && portableEntries.isEmpty() ?
					"The backup file holds nothing else, so no rating was restored and none was deleted." :
					"The rest of the file describes the same ratings and was restored.");
		}

		//the resource keys written in this run, so a rating is not applied twice
		Set<String> restored = new HashSet<>();
		List<File> sharedFolders = SharedContentConfiguration.getSharedFolders();

		for (Entry entry : resourceEntries) {
			restoreResourceRating(entry, c, restored, counters, writers);
		}
		for (Entry entry : contentEntries) {
			restoreContentRating(entry, c, sharedFolders, restored, counters, writers);
		}
		for (Entry entry : portableEntries) {
			restorePortableRating(entry, c, sharedFolders, recordedRoots, restored, counters, writers);
		}
		//keys of entries whose file is gone belong to the backup as well, they must survive
		for (Entry entry : resourceEntries) {
			restored.add(entry.value());
		}
		return restored;
	}

	private static void restoreResourceRating(Entry entry, Connection c, Set<String> restored, RestoreCounters counters, Writers writers) {
		String resourceKey = entry.value();
		if (RelativeMediaPath.isFileSystemPath(resourceKey) && !new File(resourceKey).exists()) {
			LOGGER.trace("[restoreRating] \"{}\" does not exist, trying to resolve it another way", resourceKey);
			counters.missingPaths++;
			return;
		}
		applyRating(c, resourceKey, entry.objectType(), entry.rating(), restored, counters, writers);
		counters.byPath++;
	}

	/**
	 * Restores the rating of a file on matching FILES.RUID.
	 */
	private static void restoreContentRating(Entry entry, Connection c, List<File> sharedFolders, Set<String> restored, RestoreCounters counters, Writers writers) {
		List<String> filenames = MediaTableFiles.getFilenamesForResourceUid(c, entry.value());
		int applied = 0;
		for (String filename : filenames) {
			if (restored.contains(filename)) {
				continue;
			}
			//only restore into the media library that is shared now
			if (RelativeMediaPath.relativize(filename, sharedFolders) == null) {
				continue;
			}
			applyRating(c, filename, entry.objectType(), entry.rating(), restored, counters, writers);
			applied++;
		}
		if (applied > 1) {
			LOGGER.info("[restoreRating] content '{}' exists {} times on the file system, all of them were rated", entry.value(), applied);
		}
		counters.byContent = counters.byContent + applied;
	}

	/**
	 * Restores the rating relative to its shared folder.
	 */
	private static void restorePortableRating(Entry entry, Connection c, List<File> sharedFolders, Map<Integer, String> recordedRoots,
		Set<String> restored, RestoreCounters counters, Writers writers) {
		String value = entry.value();
		int separator = value.indexOf(':');
		if (separator < 0) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", value);
			counters.skipped++;
			return;
		}
		Integer rootIndex = parseInteger(value.substring(0, separator), value);
		String relativePath = value.substring(separator + 1);
		if (rootIndex == null || relativePath.isEmpty()) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", value);
			counters.skipped++;
			return;
		}
		List<String> resourceKeys = RelativeMediaPath.resolve(recordedRoots.get(rootIndex), relativePath, sharedFolders);
		if (resourceKeys.isEmpty()) {
			LOGGER.debug("[restoreRating] \"{}\" was not found below any shared folder", relativePath);
			return;
		}
		if (resourceKeys.size() > 1) {
			LOGGER.info("[restoreRating] \"{}\" exists below {} shared folders, all of them were rated", relativePath, resourceKeys.size());
		}
		for (String resourceKey : resourceKeys) {
			if (restored.contains(resourceKey)) {
				continue;
			}
			applyRating(c, resourceKey, entry.objectType(), entry.rating(), restored, counters, writers);
			counters.byRelativePath++;
		}
	}

	/**
	 * Writes one rating.
	 */
	private static void applyRating(Connection c, String resourceKey, String objectType, Integer rating, Set<String> restored, RestoreCounters counters, Writers writers) {
		try {
			writers.ratings.write(resourceKey, objectType, rating);
		} catch (SQLException e) {
			LOGGER.warn("restoreRating could not write the rating of {} : {}", resourceKey, e.getMessage());
			counters.skipped++;
			return;
		}
		restored.add(resourceKey);
		counters.updated++;
	}

	/**
	 * Splits an entry key into its object type and its value.
	 */
	private static void addEntry(List<Entry> entries, String key, String prefix, Integer rating, RestoreCounters counters) {
		String value = key.substring(prefix.length());
		int separator = value.indexOf(':');
		if (separator < 0) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", key);
			counters.skipped++;
			return;
		}
		String objectType = value.substring(0, separator);
		String entryValue = value.substring(separator + 1);
		if (entryValue.isEmpty()) {
			LOGGER.warn("restoreRating skipped malformed entry {} ", key);
			counters.skipped++;
			return;
		}
		entries.add(new Entry(UNKNOWN_OBJECT_TYPE.equals(objectType) ? null : objectType, entryValue, rating));
	}

	private static String getObjectType(String objectType) {
		if (objectType == null || objectType.isEmpty()) {
			return UNKNOWN_OBJECT_TYPE;
		}
		return objectType;
	}

	private static Integer parseInteger(String value, String key) {
		if (value == null) {
			LOGGER.warn("restoreRating skipped entry {} without a value", key);
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("restoreRating skipped entry {} with an unparsable value \"{}\"", key, value);
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

	/**
	 * One entry of a backup file.
	 */
	private record Entry(String objectType, String value, Integer rating) {
	}

	/**
	 * What a restore did, for the log.
	 */
	private static class RestoreCounters {
		private int updated;
		private int skipped;
		private int deleted;
		private int byPath;
		private int byContent;
		private int byRelativePath;
		private int missingPaths;
		private int obsoleteFormat;
	}

}
