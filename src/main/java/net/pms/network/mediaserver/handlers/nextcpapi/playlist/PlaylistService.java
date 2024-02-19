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
package net.pms.network.mediaserver.handlers.nextcpapi.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.SQLException;
import net.pms.Messages;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponse;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME : this should be implemented under upnp, CreateObject() | UpdateObject() | DestroyObject()
public class PlaylistService implements NextcpApiResponseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistService.class.getName());
	public static final String PATH_MATCH = "playlist";
	private final PlaylistManager pm = new PlaylistManager();
	private final ObjectMapper om = new ObjectMapper();

	@Override
	public NextcpApiResponse handleRequest(String uri, String content) {
		NextcpApiResponse response = new NextcpApiResponse();
		response.setConnection("keep-alive");
		response.setStatusCode(200);

		String uriLower = uri.toLowerCase();
		try {
			if (!pm.isServiceEnabled()) {
				throw new RuntimeException(Messages.getString("PlaylistServiceDisabled"));
			}
			switch (uriLower) {
				case "getallplaylists" -> {
					LOGGER.trace("getallplaylists");
					String playlists = om.writeValueAsString(pm.getAvailablePlaylistNames());
					response.setContentType("application/json; charset=UTF-8");
					response.setResponse(playlists);
					return response;
				}
				case "getserverplaylists" -> {
					LOGGER.trace("getserverplaylists");
					String serverPlaylists = om.writeValueAsString(pm.getServerAccessiblePlaylists());
					response.setContentType("application/json; charset=UTF-8");
					response.setResponse(serverPlaylists);
					return response;
				}
				case "addsongtoplaylist" -> {
					LOGGER.trace("addsongtoplaylist");
					AudioPlaylistVO add = getParamsFromContent(content);
					pm.addSongToPlaylist(add.getSongObjectId(), add.getPlaylistObjectId());
					response.setResponse(Messages.getString("SongAddedToPlaylist"));
					return response;
				}
				case "removesongfromplaylist" -> {
					LOGGER.trace("removesongfromplaylist");
					AudioPlaylistVO remove = getParamsFromContent(content);
					pm.removeSongFromPlaylist(remove.getSongObjectId(), remove.getPlaylistObjectId());
					response.setResponse(Messages.getString("SongRemovedFromPlaylist"));
					return response;
				}
				case "createplaylist" -> {
					LOGGER.trace("createplaylist");
					pm.createPlaylist(content);
					response.setResponse(Messages.getString("PlaylistHasBeenCreated"));
					return response;
				}
				default -> {
					LOGGER.trace("default");
					response.setStatusCode(404);
					response.setResponse(Messages.getString("NoServiceAvailableForPath") + " : " + uri);
					return response;
				}
			}
		} catch (IOException | RuntimeException | SQLException e) {
			response.setStatusCode(503);
			response.setResponse(e.getMessage());
			return response;
		}
	}

	private AudioPlaylistVO getParamsFromContent(String content) {
		try {
			String[] contentArray = content.split("/");
			return new AudioPlaylistVO(contentArray[0], contentArray[1]);
		} catch (NumberFormatException e) {
			throw new RuntimeException("incorrect input parameter supplied to method");
		}
	}

}
