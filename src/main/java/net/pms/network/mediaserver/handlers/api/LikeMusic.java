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
package net.pms.network.mediaserver.handlers.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import net.pms.database.MediaTableAudioMetadata;
import net.pms.database.MediaTableMusicBrainzReleaseLike;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class LikeMusic implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LikeMusic.class.getName());
	public static final String PATH_MATCH = "like";
	private final String backupFilename;

	public LikeMusic() {
		String dir = FilenameUtils.concat(PMS.getConfiguration().getProfileDirectory(), "database_backup");
		backupFilename = FilenameUtils.concat(dir, "MUSIC_BRAINZ_RELEASE_LIKE");
	}

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				return null;
			}
			output.setStatus(HttpResponseStatus.OK);
			output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

			String sql;
			switch (uri) {
				case "likealbum" -> {
					sql = "MERGE INTO " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " KEY (MBID_RELEASE) values (?)";
					try (PreparedStatement ps = connection.prepareStatement(sql)) {
						ps.setString(1, content);
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
				}
				case "dislikealbum" -> {
					sql = "DELETE FROM " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " WHERE " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = ?";
					try (PreparedStatement ps = connection.prepareStatement(sql)) {
						ps.setString(1, content);
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
				}
				case "isalbumliked" -> {
					sql = "SELECT COUNT(*) FROM " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " WHERE " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = ?";
					return Boolean.toString(isCountGreaterZero(sql, connection, content));
				}
				case "issongliked" -> {
					sql = "SELECT COUNT(*) FROM " + MediaTableAudioMetadata.TABLE_NAME + " WHERE " + MediaTableAudioMetadata.TABLE_COL_MBID_TRACK + " = ?";
					return Boolean.toString(isCountGreaterZero(sql, connection, content));
				}
				case "backupLikedAlbums" -> {
					backupLikedAlbums();
					return "OK";
				}
				case "restoreLikedAlbums" -> {
					restoreLikedAlbums();
					return "OK";
				}
				default -> {
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "ERROR";
				}
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
			ps.setString(1, key);
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
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			Script.process(connection, backupFilename, "", "TABLE " + MediaTableMusicBrainzReleaseLike.TABLE_NAME);
		}
	}

	public void restoreLikedAlbums() throws SQLException, FileNotFoundException {
		File backupFile = new File(backupFilename);
		if (backupFile.exists() && backupFile.isFile()) {
			try (Connection connection = MediaDatabase.getConnectionIfAvailable(); Statement stmt = connection.createStatement()) {
				String sql;
				sql = "DROP TABLE " + MediaTableMusicBrainzReleaseLike.TABLE_NAME;
				stmt.execute(sql);
				try {
					RunScript.execute(connection, new FileReader(backupFilename));
				} catch (FileNotFoundException | SQLException e) {
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
