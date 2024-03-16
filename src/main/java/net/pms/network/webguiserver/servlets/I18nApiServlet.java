/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.util.Languages;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "I18nApiServlet", urlPatterns = {"/v1/api/i18n"}, displayName = "I18n Api Servlet")
public class I18nApiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(I18nApiServlet.class);
	private static final String LANGUAGE_MEMBER_NAME = "language";
	private static final String VERSION_MEMBER_NAME = "version";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/" -> {
					String language = req.getParameter(LANGUAGE_MEMBER_NAME);
					String version = req.getParameter(VERSION_MEMBER_NAME);
					if (language != null) {
						Locale locale = Languages.toLocale(language);
						if (locale == null) {
							locale = PMS.getLocale();
						}
						JsonObject i18n = new JsonObject();
						i18n.add("i18n", Messages.getStringsAsJsonObject(locale));
						i18n.add("languages", Languages.getLanguagesAsJsonArray(locale));
						i18n.add("isRtl", new JsonPrimitive(Languages.getLanguageIsRtl(locale)));
						resp.setHeader("Cache-Control", "public, max-age=604800");
						respond(req, resp, i18n.toString(), 200, "application/json");
					} else {
						respondBadRequest(req, resp);
					}
				}
				default -> {
					LOGGER.trace("I18nApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			respondInternalServerError(req, resp);
		} catch (IOException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in I18nApiServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/" -> {
					JsonObject version = new JsonObject();
					version.addProperty(VERSION_MEMBER_NAME, PropertiesUtil.getProjectProperties().get("git.commit.id"));
					respond(req, resp, version.toString(), 200, "application/json");
				}
				default -> {
					LOGGER.trace("I18nApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			respondInternalServerError(req, resp);
		} catch (IOException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in I18nApiServlet.doPost(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
