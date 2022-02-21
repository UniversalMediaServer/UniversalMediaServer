package net.pms.network.mediaserver.handlers.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class LikeMusic implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LikeMusic.class.getName());
	public static final String PATH_MATCH = "like";
	private MediaDatabase db = PMS.get().getMediaDatabase();

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
						ps.setString(1, content);
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "likealbum":
					sql = "MERGE INTO MUSIC_BRAINZ_RELEASE_LIKE KEY (MBID_RELEASE) values (?)";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setString(1, content);
						ps.executeUpdate();
					} catch (SQLException e) {
						LOG.warn("error preparing statement", e);
						return "ERROR:" + e.getMessage();
					}
					break;
				case "dislikesong":
					sql = "UPDATE AUDIOTRACKS set LIKESONG = false where MBID_TRACK = ?";
					try {
						PreparedStatement ps = connection.prepareStatement(sql);
						ps.setString(1, content);
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
						ps.setString(1, content);
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
				default:
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "ERROR";
			}

			return "OK";
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
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
}
