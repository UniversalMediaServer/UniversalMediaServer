package net.pms.network.mediaserver.handlers.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class LikeMusic implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(LikeMusic.class.getName());
	private MediaDatabase db = PMS.get().getMediaDatabase();

	@Override
	public void handleRequest(String uri, String content, HttpResponse output) {
		try (Connection connection = db.getConnection()) {
			if (connection == null) {
				return;
			}
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
					}
					break;
				default:
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					break;
			}
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
	}
}
