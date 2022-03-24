package net.pms.network.mediaserver.handlers.api.playlist;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class PlaylistService implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class.getName());
	public static final String PATH_MATCH = "playlist";
	private PlaylistManager pm = new PlaylistManager();
	private ObjectMapper om = new ObjectMapper();

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		try {
			switch (uri) {
				case "getallplaylists":
					try {
						String playlists = om.writeValueAsString(pm.getAvailablePlaylistNames());
						return playlists;
					} catch (JsonProcessingException e) {
						LOG.warn("getAllPlaylists", e);
						return "ERROR : " + e.getMessage();
					}
				case "addsongtoplaylist":
					try {
						AudioPlaylistVO vo = getParamsFromContent(content);
						pm.addSongToPlaylist(vo.audiotrackId, vo.playlistName);
						return "Song added to playlist";
					} catch (Exception e) {
						LOG.warn("getAllPlaylists", e);
						return "ERROR : " + e.getMessage();
					}
				case "removesongfromplaylist":
					AudioPlaylistVO vo = getParamsFromContent(content);
					pm.removeSongFromPlaylist(vo.audiotrackId, vo.playlistName);
					return "Song removed from playlist";
				default:
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return "ERROR";
			}

		} catch (Exception e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return e.getMessage();
		}
	}

	private AudioPlaylistVO getParamsFromContent(String content) {
		try {
			AudioPlaylistVO vo = new AudioPlaylistVO();
			String[] contentArray = content.split("/");
			vo.audiotrackId = Integer.parseInt(contentArray[0]);
			vo.playlistName = contentArray[1];
			return vo;
		} catch (Exception e) {
			throw new RuntimeException("incorrect input parameter supplied to method");
		}
	}
}
