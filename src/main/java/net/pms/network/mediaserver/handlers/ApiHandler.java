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

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import net.pms.Messages;
import net.pms.PMS;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class ApiHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	private final ApiResponseFactory apiFactory = new ApiResponseFactory();

	/**
	 * Handle API calls
	 *
	 * @param method HTTP method
	 * @param content body content
	 * @param output Response to request
	 * @param event The request
	 * @param uri
	 *
	 * @return response String
	 */
	public String handleApiRequest(HttpMethod method, String content, HttpResponse output, String uri, MessageEvent event) {
		String serverApiKey = PMS.getConfiguration().getApiKey();
		if (serverApiKey.length() < 12) {
			LOGGER.warn("Weak server API key configured. UMS.conf api_key should have at least 12 characters.");
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return Messages.getString("WeakOrNoServerApiKey");
		}

		try {
			if (validApiKeyPresent(serverApiKey, extractApiKey(event))) {
				int pathSepPosition = uri.indexOf('/');
				String apiType = uri.substring(0, pathSepPosition);
				if (!StringUtils.isAllBlank(apiType)) {
					uri = uri.substring(pathSepPosition + 1);
					ApiResponseHandler responseHandler = apiFactory.getApiResponseHandler(apiType);
					return responseHandler.handleRequest(uri, content, output);
				} else {
					LOGGER.warn("Invalid API call. Unknown path : " + uri);
					output.setStatus(HttpResponseStatus.NOT_FOUND);
				}
			} else {
				LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf api_key value.");
				output.setStatus(HttpResponseStatus.UNAUTHORIZED);
			}
		} catch (Exception e) {
			LOGGER.error("handling api request failed failed: ", e);
			output.setStatus(HttpResponseStatus.EXPECTATION_FAILED);
			return "ERROR : " + e.getMessage();
		}
		return "ERROR";
	}

	/**
	 * checks if the given api-key equals to the provided api key.
	 *
	 * @param serverApiKey server API key
	 * @param givenApiKey given API key from client
	 * @return TRUE if keys match.
	 */
	private boolean validApiKeyPresent(String serverApiKey, String givenApiKey) {
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

	/**
	 * Extracts API key from header.
	 *
	 * @param event
	 * @return
	 */
	private String extractApiKey(MessageEvent event) {
		HttpRequest request = (HttpRequest) event.getMessage();
		for (Entry<String, String> entry : request.headers().entries()) {
			if (entry.getKey().equalsIgnoreCase("api-key")) {
				return entry.getValue();
			}
		}
		throw new RuntimeException("no 'api-key' provided in header.");
	}
}
