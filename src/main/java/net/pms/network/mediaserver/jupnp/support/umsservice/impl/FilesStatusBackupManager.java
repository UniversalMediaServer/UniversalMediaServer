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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;

public class FilesStatusBackupManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilesStatusBackupManager.class.getName());
	private static ObjectMapper om = new ObjectMapper();

	private final static String STATUS_READ = "SELECT * FROM FILES_STATUS";


	public FilesStatusBackupManager() {
		om.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static void backupRatings() {
		List<FilesStatusDto> backList = new ArrayList<>();
		Connection c = MediaDatabase.getConnectionIfAvailable();
		int items = 0;
		try (PreparedStatement selectStatement = c.prepareStatement(STATUS_READ)) {
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
					ObjectWriter writer = om.writer();
					String value = writer.withDefaultPrettyPrinter().writeValueAsString(backList);
					om.writeValue(file, value);
				} catch (Exception e) {
					LOGGER.error("could not write backup file", e);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("backup rating failed", e);
		}
		LOGGER.info("save {} items into backup file {} ", items, getBackupFilename());
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
