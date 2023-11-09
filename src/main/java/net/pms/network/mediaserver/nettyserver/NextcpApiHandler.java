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
package net.pms.network.mediaserver.nettyserver;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.network.mediaserver.handlers.nextcpapi.AbstractNextcpApiHandler;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponse;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponseHandler;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * This class handles calls to the Nextcp API.
 */
public class NextcpApiHandler extends AbstractNextcpApiHandler {

	/**
	 * Handle API calls (netty)
	 *
	 * @param output Response to request
	 * @param event The request
	 * @param uri
	 *
	 * @return response String
	 */
	public static String handleApiRequest(HttpResponse output, String uri, MessageEvent event) {
		String serverApiKey = PMS.getConfiguration().getNextcpApiKey();
		if (serverApiKey.length() < 12) {
			LOGGER.warn("Weak nextcp API key configured. UMS.conf nextcp_api_key should have at least 12 characters.");
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return Messages.getString("WeakOrNoServerApiKey");
		}
		HttpRequest nettyRequest = (HttpRequest) event.getMessage();
		try {
			if (validApiKeyPresent(serverApiKey, extractApiKey(nettyRequest))) {
				int pathSepPosition = uri.indexOf('/');
				String apiType = uri.substring(0, pathSepPosition);
				if (!StringUtils.isAllBlank(apiType)) {
					uri = uri.substring(pathSepPosition + 1);
					String body = nettyRequest.getContent().toString(StandardCharsets.UTF_8);
					NextcpApiResponseHandler responseHandler = getApiResponseHandler(apiType);
					NextcpApiResponse response = responseHandler.handleRequest(uri, body);
					if (response.getConnection() != null) {
						output.headers().set(HttpHeaders.Names.CONNECTION, response.getConnection());
					}
					if (response.getContentType() != null) {
						output.headers().set(HttpHeaders.Names.CONTENT_TYPE, response.getContentType());
					}
					if (response.getStatusCode() != null) {
						output.setStatus(HttpResponseStatus.valueOf(response.getStatusCode()));
					}
					return response.getResponse();
				} else {
					LOGGER.warn("Invalid API call. Unknown path : " + uri);
					output.setStatus(HttpResponseStatus.NOT_FOUND);
				}
			} else {
				LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf nextcp_api_key value.");
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
	 * Extracts API key from header (netty).
	 *
	 * @param event
	 * @return
	 */
	private static String extractApiKey(HttpRequest request) {
		for (Entry<String, String> entry : request.headers().entries()) {
			if (entry.getKey().equalsIgnoreCase("api-key")) {
				return entry.getValue();
			}
		}
		throw new RuntimeException("no 'api-key' provided in header.");
	}

}
