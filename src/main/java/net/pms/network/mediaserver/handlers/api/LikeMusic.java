package net.pms.network.mediaserver.handlers.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.syntax.DbTypes;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class LikeMusic implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LikeMusic.class.getName());
	public static final String PATH_MATCH = "like";
	private MediaDatabase db = PMS.get().getMediaDatabase();
	private final String backupFilename;

	private DbTypes dbTypes = MediaDatabase.get().getDbType();

	public LikeMusic() {
		String dir = FilenameUtils.concat(PMS.getConfiguration().getProfileDirectory(), "database_backup");
		backupFilename = FilenameUtils.concat(dir, "MUSIC_BRAINZ_RELEASE_LIKE");
	}

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		try (Connection connection = db.getConnection()) {
			if (connection == null) {
				return null;
			}
			output.setStatus(HttpResponseStatus.OK);
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

			String sql = null;
			switch (uri) {
				case "likesong":
					sql = "UPDATE AUDIOTRACKS set LIKESONG = true where MBID_TRACK = ?";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setObject(1, UUID.fromString(content));
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "likealbum":
					try {
						dbTypes.mergeLikedAlbum(connection, content);
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "dislikesong":
					sql = "UPDATE AUDIOTRACKS set LIKESONG = false where MBID_TRACK = ?";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setObject(1, UUID.fromString(content));
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "dislikealbum":
					sql = "DELETE FROM MUSIC_BRAINZ_RELEASE_LIKE where MBID_RELEASE = ?";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setObject(1, UUID.fromString(content));
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "isalbumliked":
					sql = "SELECT COUNT(*) FROM MUSIC_BRAINZ_RELEASE_LIKE where MBID_RELEASE = ?";
					return Boolean.toString(isCountGreaterZero(sql, connection, content));
				case "issongliked":
					sql = "SELECT COUNT(*) FROM AUDIOTRACKS where MBID_TRACK = ?";
					return Boolean.toString(isCountGreaterZero(sql, connection, content));
				case "backupLikedAlbums":
					backupLikedAlbums();
					return "OK";
				case "restoreLikedAlbums":
					restoreLikedAlbums();
					return "OK";
				default:
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "ERROR";
			}

			return "ERROR";
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("backup file not found.", e);
		}
	}

	private boolean isCountGreaterZero(String sql, Connection connection, String key) {
		try (PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setObject(1, UUID.fromString(key));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong(1) > 0;
			}
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
		return false;
	}

	public void backupLikedAlbums() throws SQLException {
		try (Connection connection = db.getConnection()) {
			Script.process(connection, backupFilename, "", "TABLE MUSIC_BRAINZ_RELEASE_LIKE");
		}
	}

	public void restoreLikedAlbums() throws SQLException, FileNotFoundException {
		File backupFile = new File(backupFilename);
		if (backupFile.exists() && backupFile.isFile()) {
			try (Connection connection = db.getConnection(); Statement stmt = connection.createStatement()) {
				String sql;
				sql = "DROP TABLE MUSIC_BRAINZ_RELEASE_LIKE";
				stmt.execute(sql);
				try {
					RunScript.execute(connection, new FileReader(backupFilename));
				} catch (Exception e) {
					LOG.error("restoring MUSIC_BRAINZ_RELEASE_LIKE table : failed");
					throw new RuntimeException("restoring MUSIC_BRAINZ_RELEASE_LIKE table failed", e);
				}
				connection.commit();
				LOG.trace("restoring MUSIC_BRAINZ_RELEASE_LIKE table : success");
			}
		} else {
			if (!StringUtils.isEmpty(backupFilename)) {
				LOG.trace("Backup file doesn't exist : " + backupFilename);
				throw new RuntimeException("Backup file doesn't exist : " + backupFilename);
			} else {
				throw new RuntimeException("Backup filename not set !");
			}
		}
	}
}
