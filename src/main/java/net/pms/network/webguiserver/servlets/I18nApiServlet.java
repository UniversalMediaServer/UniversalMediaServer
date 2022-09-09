/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.util.Languages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "I18nApiServlet", urlPatterns = {"/v1/api/i18n"}, displayName = "I18n Api Servlet")
public class I18nApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(I18nApiServlet.class);
	private static final String LANGUAGE_MEMBER_NAME = "language";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/" -> {
					JsonObject post = WebGuiServletHelper.getJsonObjectFromBody(req);
					if (post == null || !post.has(LANGUAGE_MEMBER_NAME) || !post.get(LANGUAGE_MEMBER_NAME).isJsonPrimitive()) {
						WebGuiServletHelper.respondBadRequest(req, resp);
						return;
					}
					Locale locale = Languages.toLocale(post.get(LANGUAGE_MEMBER_NAME).getAsString());
					if (locale == null) {
						locale = PMS.getLocale();
					}
					JsonObject i18n = new JsonObject();
					i18n.add("i18n", Messages.getStringsAsJsonObject(locale));
					i18n.add("languages", Languages.getLanguagesAsJsonArray(locale));
					i18n.add("isRtl", new JsonPrimitive(Languages.getLanguageIsRtl(locale)));
					WebGuiServletHelper.respond(req, resp, i18n.toString(), 200, "application/json");
				}
				default -> {
					LOGGER.trace("I18nApiServlet request not available : {}", path);
					WebGuiServletHelper.respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			WebGuiServletHelper.respondInternalServerError(req, resp);
		} catch (IOException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in I18nApiServlet.doPost(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
