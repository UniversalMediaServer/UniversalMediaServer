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
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;

public class RatingBackupManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RatingBackupManager.class.getName());
	private final static String RATINGS_READ = "SELECT FILES.RUID, AUDIO_METADATA.RATING FROM FILES LEFT JOIN AUDIO_METADATA ON FILES.ID = AUDIO_METADATA.FILEID WHERE RATING is not null and RUID is not null";
	private final static String RATINGS_WRITE = "UPDATE AUDIO_METADATA a SET a.RATING = ? WHERE a.FILEID in (SELECT ID from FILES WHERE RUID = ?)";

	public RatingBackupManager() {

	}

	public static void backupRatings() {
		Properties p = new Properties();
		Connection c = MediaDatabase.getConnectionIfAvailable();
		int items = 0;
		try (PreparedStatement selectStatement = c.prepareStatement(RATINGS_READ)) {
			try (ResultSet rs = selectStatement.executeQuery()) {
				while (rs.next()) {
					p.put(rs.getString("ruid"), rs.getString("rating"));
					items++;
				}
				String backupFilename = getBackupFilename();
				try (FileOutputStream fs = new FileOutputStream(new File(backupFilename))) {
					DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
					p.store(fs, "ratings backup from " + formatter.format(LocalDateTime.now()));
				} catch (IOException e) {
					LOGGER.error("backup rating failed", e);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("backup rating failed", e);
		}
		LOGGER.info("save {} items into backup file {} ", items, getBackupFilename());
	}

	public static void restoreRating() {
		String backupFilename = getBackupFilename();
		File f = new File(backupFilename);
		if (!f.exists()) {
			throw new RuntimeException("Backup file not present");
		}
		try (FileInputStream fis = new FileInputStream(f)) {
			Properties p = new Properties();
			p.load(fis);
			LOGGER.debug("[restoreRating] read {} items.", p.size());
			int updated = 0;
			int skipped = 0;

			Connection c = MediaDatabase.getConnectionIfAvailable();

			for (Object oruid : p.keySet()) {
				try (PreparedStatement updateStatement = c.prepareStatement(RATINGS_WRITE)) {
					String ruid = (String) oruid;
					String rating = (String) p.get(oruid);
					updateStatement.setLong(1, Integer.parseInt(rating));
					updateStatement.setString(2, ruid);
					int numUpdates = updateStatement.executeUpdate();
					if (numUpdates == 1) {
						updated++;
					} else if (numUpdates > 1) {
						LOGGER.info("File exists multiple times on file system. RUID : '{}'.", ruid);
						updated = updated + numUpdates;
					} else {
						skipped++;
					}
				} catch (SQLException e) {
					LOGGER.warn("restoreRating failed for entry {} ", oruid, e);
				}
			}
			LOGGER.info("Updated {} items. Skipped {} items.", updated, skipped);
		} catch (IOException e) {
			LOGGER.error("restore rating failed", e);
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
