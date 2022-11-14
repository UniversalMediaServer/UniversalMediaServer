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
package net.pms.network.mediaserver.handlers;

import net.pms.network.mediaserver.handlers.api.FolderScanner;
import net.pms.network.mediaserver.handlers.api.LikeMusic;
import net.pms.network.mediaserver.handlers.api.playlist.PlaylistService;
import net.pms.network.mediaserver.handlers.api.starrating.StarRating;

public class ApiResponseFactory {

	public ApiResponseHandler getApiResponseHandler(String apiType) {
		switch (apiType) {
			case FolderScanner.PATH_MATCH -> {
				return new FolderScanner();
			}
			case LikeMusic.PATH_MATCH -> {
				return new LikeMusic();
			}
			case StarRating.PATH_MATCH -> {
				return new StarRating();
			}
			case PlaylistService.PATH_MATCH -> {
				return new PlaylistService();
			}
			default -> throw new RuntimeException("No api Handler found");
		}
	}
}
