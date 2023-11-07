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

import java.nio.charset.StandardCharsets;
import net.pms.network.mediaserver.handlers.api.playlist.PlaylistService;
import net.pms.network.mediaserver.handlers.api.starrating.StarRating;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public abstract class AbstractApiHandler {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiHandler.class);

	protected AbstractApiHandler() {
	}

	/**
	 * checks if the given api-key equals to the provided api key.
	 *
	 * @param serverApiKey server API key
	 * @param givenApiKey given API key from client
	 * @return TRUE if keys match.
	 */
	protected static boolean validApiKeyPresent(String serverApiKey, String givenApiKey) {
		boolean result = true;
		try {
			byte[] givenApiKeyHash = DigestUtils.sha256(givenApiKey.getBytes(StandardCharsets.UTF_8));
			byte[] serverApiKeyHash = DigestUtils.sha256(serverApiKey.getBytes(StandardCharsets.UTF_8));
			int pos = 0;
			for (byte b : serverApiKeyHash) {
				result = result && (b == givenApiKeyHash[pos++]);
			}
			LOGGER.debug("validApiKeyPresent : " + result);
			return result;
		} catch (RuntimeException e) {
			LOGGER.error("cannot hash api key", e);
			return false;
		}
	}

	protected static ApiResponseHandler getApiResponseHandler(String apiType) {
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
