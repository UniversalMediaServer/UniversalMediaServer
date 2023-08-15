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
package net.pms.network.mediaserver.handlers.api.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.SQLException;
import net.pms.Messages;
import net.pms.network.mediaserver.handlers.ApiResponseHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistService implements ApiResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistService.class.getName());
	public static final String PATH_MATCH = "playlist";
	private final PlaylistManager pm = new PlaylistManager();
	private final ObjectMapper om = new ObjectMapper();

	@Override
	public String handleRequest(String uri, String content, HttpResponse output) {
		output.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		output.setStatus(HttpResponseStatus.OK);

		String uriLower = uri.toLowerCase();
		try {
			if (!pm.isServiceEnabled()) {
				throw new RuntimeException(Messages.getString("PlaylistServiceDisabled"));
			}
			switch (uriLower) {
				case "getallplaylists" -> {
					LOGGER.trace("getallplaylists");
					String playlists = om.writeValueAsString(pm.getAvailablePlaylistNames());
					output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");
					return playlists;
				}
				case "getserverplaylists" -> {
					LOGGER.trace("getserverplaylists");
					String serverPlaylists = om.writeValueAsString(pm.getServerAccessiblePlaylists());
					output.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=UTF-8");
					return serverPlaylists;
				}
				case "addsongtoplaylist" -> {
					LOGGER.trace("addsongtoplaylist");
					AudioPlaylistVO add = getParamsFromContent(content);
					pm.addSongToPlaylist(add.getAudiotrackId(), add.getPlaylistName());
					return Messages.getString("SongAddedToPlaylist");
				}
				case "removesongfromplaylist" -> {
					LOGGER.trace("removesongfromplaylist");
					AudioPlaylistVO remove = getParamsFromContent(content);
					pm.removeSongFromPlaylist(remove.getAudiotrackId(), remove.getPlaylistName());
					return Messages.getString("SongRemovedFromPlaylist");
				}
				case "createplaylist" -> {
					LOGGER.trace("createplaylist");
					pm.createPlaylist(content);
					return Messages.getString("PlaylistHasBeenCreated");
				}
				default -> {
					LOGGER.trace("default");
					output.setStatus(HttpResponseStatus.NOT_FOUND);
					return Messages.getString("NoServiceAvailableForPath") + " : " + uri;
				}
			}
		} catch (IOException | RuntimeException | SQLException e) {
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return e.getMessage();
		}
	}

	private AudioPlaylistVO getParamsFromContent(String content) {
		try {
			String[] contentArray = content.split("/");
			return new AudioPlaylistVO(Integer.valueOf(contentArray[0]), contentArray[1]);
		} catch (NumberFormatException e) {
			throw new RuntimeException("incorrect input parameter supplied to method");
		}
	}
}
