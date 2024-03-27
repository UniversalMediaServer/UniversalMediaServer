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
package net.pms.network.mediaserver.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import net.pms.PMS;
import net.pms.network.HttpServletHelper;
import net.pms.network.mediaserver.handlers.nextcpapi.AbstractNextcpApiHandler;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponse;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponseHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the Nextcp API.
 *
 * @author Surf@ceS
 */
@WebServlet(name = "NextcpApiServlet", urlPatterns = {"/api"}, displayName = "Nextcp Api Servlet")
public class NextcpApiServlet extends HttpServletHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(NextcpApiServlet.class);

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (deny(req)) {
			return;
		}
		String serverApiKey = PMS.getConfiguration().getNextcpApiKey();
		String clientApiKey = req.getHeader("api-key");
		try {
			if (serverApiKey.length() < 12) {
				LOGGER.warn("Weak nextcp API key configured. UMS.conf nextcp_api_key should have at least 12 digests.");
				respondServiceUnavailable(req, resp);
			} else if (clientApiKey == null) {
				LOGGER.error("no 'api-key' provided in header.");
				respondForbidden(req, resp);
			} else if (AbstractNextcpApiHandler.validApiKeyPresent(serverApiKey, clientApiKey)) {
				String uri = "";
				String handler = "";
				String call = "";
				int pos = req.getRequestURI().indexOf("api/");
				if (pos != -1) {
					uri = req.getRequestURI().substring(pos + "api/".length());
				}
				pos = uri.indexOf("/");
				if (pos != -1) {
					call = uri.substring(pos + 1);
					handler = uri.substring(0, pos);
				}
				if (!StringUtils.isAllBlank(handler)) {
					NextcpApiResponseHandler responseHandler = AbstractNextcpApiHandler.getApiResponseHandler(handler);
					String body = getBodyAsString(req);
					if (body == null) {
						body = "";
					}
					NextcpApiResponse response = responseHandler.handleRequest(call, body);
					sendResponse(req, resp, response);
				} else {
					LOGGER.warn("Invalid API call. Unknown path : " + uri);
					respondNotFound(req, resp);
				}
			} else {
				LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf nextcp_api_key value.");
				respondUnauthorized(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("comparing api key failed: " + e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

	private static void sendResponse(HttpServletRequest req, HttpServletResponse resp, NextcpApiResponse response) throws IOException {
		if (response.getConnection() != null) {
			resp.setHeader("Connection", response.getConnection());
		}
		if (response.getStatusCode() == null) {
			response.setStatusCode(HttpServletResponse.SC_OK);
		}
		respond(req, resp, response.getResponse(), response.getStatusCode(), response.getContentType());
	}

}
