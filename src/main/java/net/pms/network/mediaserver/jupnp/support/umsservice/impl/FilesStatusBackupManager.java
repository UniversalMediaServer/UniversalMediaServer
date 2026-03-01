package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;

public class FilesStatusBackupManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilesStatusBackupManager.class.getName());
	private static ObjectMapper om = new ObjectMapper();
	private static MediaDatabase mdb = new MediaDatabase();

	private final static String STATUS_READ = "SELECT * FROM FILES_STATUS";
	private final static String STATUS_READ_EXISTING = "SELECT ID FROM FILES_STATUS where FILENAME = ? and USERID = ?";
	private final static String STATUS_MERGE = "MERGE INTO FILES_STATUS (BOOKMARK, FILENAME, ISFULLYPLAYED, LASTPLAYBACKPOSITION, PLAYCOUNT, USERID, ID) VALUES (?,?,?,?,?,?,?)";
	private final static String STATUS_INSERT = "INSERT INTO FILES_STATUS (BOOKMARK, FILENAME, ISFULLYPLAYED, LASTPLAYBACKPOSITION, PLAYCOUNT, USERID) VALUES (?,?,?,?,?,?)";

	public FilesStatusBackupManager() {
		om.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature());
	}

	public static void backup() {
		List<FilesStatusDto> backList = new ArrayList<>();
		int items = 0;
		try (Connection c = mdb.getConnection(); PreparedStatement selectStatement = c.prepareStatement(STATUS_READ)) {
			try (ResultSet rs = selectStatement.executeQuery()) {
				while (rs.next()) {
					FilesStatusDto dto = new FilesStatusDto();
					dto.bookmark = rs.getInt("BOOKMARK");
					dto.filename = rs.getString("FILENAME");
					dto.isFullyPlayed = rs.getBoolean("ISFULLYPLAYED");
					dto.lastPlaybackPos = rs.getDouble("LASTPLAYBACKPOSITION");
					dto.playcount = rs.getInt("PLAYCOUNT");
					dto.userid = rs.getInt("USERID");
					backList.add(dto);
					items++;
				}
				String backupFilename = getBackupFilename();
				File file = new File(backupFilename);
				try {
					om.writeValue(file, backList);
				} catch (Exception e) {
					LOGGER.error("could not write backup file", e);
				}
			}
		} catch (Exception e) {
			LOGGER.error("backup files status failed", e);
		}
		LOGGER.info("save {} items into backup file {} ", items, getBackupFilename());
	}

	private static Integer findExisting(String filename, Integer userid) {
		try (Connection c = mdb.getConnection(); PreparedStatement selectStatement = c.prepareStatement(STATUS_READ_EXISTING)) {
			selectStatement.setString(1, filename);
			selectStatement.setInt(2, userid);
			try (ResultSet rs = selectStatement.executeQuery()) {
				if (rs.next()) {
					Integer id = rs.getInt(1);
					return id;
				}
			}
		} catch (Exception e) {
			LOGGER.error("backup files status failed", e);
		}
		return null;
	}

	public static void restore() {
		int updated = 0;
		try {
			List<FilesStatusDto> dtoList = om.readValue(new File(getBackupFilename()), new TypeReference<List<FilesStatusDto>>() { });
			Connection c = MediaDatabase.getConnectionIfAvailable();
			for (FilesStatusDto dto : dtoList) {
				Integer id = findExisting(dto.filename, dto.userid);
				if (id != null) {
					try (PreparedStatement updateStatement = c.prepareStatement(STATUS_MERGE)) {
						updateStatement.setInt(1, dto.bookmark);
						updateStatement.setString(2, dto.filename);
						updateStatement.setBoolean(3, dto.isFullyPlayed);
						updateStatement.setDouble(4, dto.lastPlaybackPos);
						updateStatement.setInt(5, dto.playcount);
						updateStatement.setInt(6, dto.userid);
						updateStatement.setInt(7, id);

						updateStatement.executeUpdate();
						updated++;
					} catch (SQLException e) {
						LOGGER.warn("restore files status failed for entry {} ", dto.filename, e);
					}
				} else {
					try (PreparedStatement updateStatement = c.prepareStatement(STATUS_INSERT)) {
						updateStatement.setInt(1, dto.bookmark);
						updateStatement.setString(2, dto.filename);
						updateStatement.setBoolean(3, dto.isFullyPlayed);
						updateStatement.setDouble(4, dto.lastPlaybackPos);
						updateStatement.setInt(5, dto.playcount);
						updateStatement.setInt(6, dto.userid);

						updateStatement.executeUpdate();
						updated++;
					} catch (SQLException e) {
						LOGGER.warn("restore files status failed for entry {} ", dto.filename, e);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("restore files status failed", e);
		}
		LOGGER.info("imported {} files status lines", updated);
	}

	private static String getBackupFilename() {
		String dir = FilenameUtils.concat(UmsConfiguration.getProfileDirectory(), "database_backup");
		File mydir = new File(dir);
		if (!mydir.exists()) {
			mydir.mkdirs();
		}
		String backupFilename = FilenameUtils.concat(dir, "files_status_backup.json");
		return backupFilename;
	}
}
