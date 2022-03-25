package net.pms.network.mediaserver.handlers.api.playlist;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.pms.Messages;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;

public class PlaylistService implements ApiResponseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class.getName());
	public static final String PATH_MATCH = "playlist";
	private PlaylistManager pm = new PlaylistManager();
	private ObjectMapper om = new ObjectMapper();

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		output.setStatus(HttpResponseStatus.OK);

		String uriLower = uri.toLowerCase();
		try {
			if (!pm.isServiceEnabled()) {
				throw new RuntimeException(Messages.getString("Api.Playlist.ServiceDisabled"));
			}
			switch (uriLower) {
				case "getallplaylists":
					LOG.trace("getallplaylists");
					String playlists = om.writeValueAsString(pm.getAvailablePlaylistNames());
					output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");
					return playlists;
				case "addsongtoplaylist":
					LOG.trace("addsongtoplaylist");
					AudioPlaylistVO add = getParamsFromContent(content);
					pm.addSongToPlaylist(add.audiotrackId, add.playlistName);
					return Messages.getString("Api.Playlist.SongAdded");
				case "removesongfromplaylist":
					LOG.trace("removesongfromplaylist");
					AudioPlaylistVO remove = getParamsFromContent(content);
					pm.removeSongFromPlaylist(remove.audiotrackId, remove.playlistName);
					return Messages.getString("Api.Playlist.SongRemoved");
				case "createplaylist":
					LOG.trace("createplaylist");
					pm.createPlaylist(content);
					return Messages.getString("Api.Playlist.PlaylistCreated");
				default:
					LOG.trace("default");
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return Messages.getString("Api.Error.UnknownService") + " : " + uri;
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
