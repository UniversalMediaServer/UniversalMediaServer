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
package net.pms.network.webguiserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Paths;
import net.pms.image.Image;
import net.pms.image.ImageTypeAdapter;
import net.pms.network.HttpServletHelper;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiHttpServlet extends HttpServletHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(GuiHttpServlet.class);
	private static final ClassLoader CLASS_LOADER = new URLClassLoader(new URL[]{getUrl(CONFIGURATION.getWebPath() + "/react-client/")});
	protected static final Gson GSON = new GsonBuilder()
			.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
			.registerTypeAdapter(Image.class, new ImageTypeAdapter())
			.create();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (deny(req)) {
			return;
		}
		if (LOGGER.isTraceEnabled()) {
			logHttpServletRequest(req, "");
		}
		super.service(req, resp);
	}

	protected static JsonArray getJsonArrayFromStringArray(String[] array) {
		return GSON.toJsonTree(array).getAsJsonArray();
	}

	protected static JsonObject getJsonObjectFromBody(HttpServletRequest req) {
		String reqBody = getBodyAsString(req);
		return jsonObjectFromString(reqBody);
	}

	/**
	 * Write the given resource as an HttpServletResponse body.
	 *
	 * @param req
	 * @param resp
	 * @param filename
	 * @return
	 * @throws java.io.IOException
	 */
	protected static boolean writeAsync(HttpServletRequest req, HttpServletResponse resp, String filename) throws IOException {
		InputStream stream = CLASS_LOADER.getResourceAsStream(filename);
		if (stream != null) {
			AsyncContext async = req.startAsync();
			if (resp.getContentType() == null) {
				String mime = getMimeType(filename);
				if (mime != null) {
					resp.setContentType(mime);
				}
			}
			// Note: available() isn't officially guaranteed to return the full
			// stream length but effectively seems to do so in our context.
			resp.setContentLength(stream.available());
			resp.setStatus(200);
			logHttpServletResponse(req, resp, null, true);
			copyStreamAsync(stream, resp.getOutputStream(), async);
			return true;
		}
		return false;
	}

	protected static String getMimeType(String file) {
		String extension = FileUtil.getExtension(file);
		if (extension != null) {
			return switch (extension) {
				case "html" ->
					"text/html";
				case "css" ->
					"text/css";
				case "js" ->
					"text/javascript";
				case "ttf" ->
					"font/truetype";
				default ->
					URLConnection.guessContentTypeFromName(file);
			};
		}
		return URLConnection.guessContentTypeFromName(file);
	}

	private static JsonObject jsonObjectFromString(String str) {
		JsonObject jObject = null;
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem != null && jElem.isJsonObject()) {
				jObject = jElem.getAsJsonObject();
			}
		} catch (JsonSyntaxException je) {
		}
		return jObject;
	}

	private static URL getUrl(String url) {
		try {
			return Paths.get(url).toUri().toURL();
		} catch (IllegalArgumentException | MalformedURLException e) {
			return null;
		}
	}

}
