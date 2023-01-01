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
package net.pms.network.webinterfaceserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.renderers.devices.WebRender;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.dlna.ByteRange;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.network.HTTPResource;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import net.pms.util.IpFilter;
import net.pms.util.Languages;
import net.pms.util.UMSUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WebInterfaceServerUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebInterfaceServerUtil.class);
	private static final Gson GSON = new Gson();

	//public static final String MIME_TRANS = MIME_MP4;
	public static final String MIME_TRANS = HTTPResource.OGG_TYPEMIME;
	//public static final String MIME_TRANS = MIME_WEBM;

	private static final String HTTPSERVER_REQUEST_BEGIN =  "============================= INTERFACE HTTPSERVER REQUEST BEGIN ================================";
	private static final String HTTPSERVER_REQUEST_END =    "============================= INTERFACE HTTPSERVER REQUEST END ==================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "============================= INTERFACE HTTPSERVER RESPONSE BEGIN ===============================";
	private static final String HTTPSERVER_RESPONSE_END =   "============================= INTERFACE HTTPSERVER RESPONSE END =================================";

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebInterfaceServerUtil() {
	}

	public static void respond(HttpExchange t, String response, int status, String mime) {
		if (response != null) {
			if (mime != null) {
				Headers hdr = t.getResponseHeaders();
				hdr.add("Content-Type", mime);
			}
			byte[] bytes = response.getBytes();
			try (OutputStream os = t.getResponseBody()) {
				t.sendResponseHeaders(status, bytes.length);
				if (LOGGER.isTraceEnabled()) {
					logMessageSent(t, response, null);
				}
				os.write(bytes);
				os.close();
			} catch (Exception e) {
				LOGGER.debug("Error sending response: " + e);
			}
		} else {
			try {
				t.sendResponseHeaders(status, -1);
				if (LOGGER.isTraceEnabled()) {
					logMessageSent(t, "", null);
				}
			} catch (IOException e) {
				LOGGER.debug("Error sending empty response: " + e);
			}
		}
	}

	public static void dumpFile(String file, HttpExchange t) throws IOException {
		File f = new File(file);
		dumpFile(f, t);
	}

	public static void dumpFile(File f, HttpExchange t) throws IOException {
		LOGGER.debug("file " + f + " " + f.length());
		if (!f.exists()) {
			throw new IOException("no file");
		}
		t.sendResponseHeaders(200, f.length());
		InputStream in = new FileInputStream(f);
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(t, "", in);
		}
		dump(in, t.getResponseBody());
		LOGGER.debug("dump of " + f.getName() + " done");
	}

	public static void dump(final InputStream in, final OutputStream os) {
		Runnable r = () -> {
			byte[] buffer = new byte[32 * 1024];
			int bytes;
			int sendBytes = 0;

			try {
				while ((bytes = in.read(buffer)) != -1) {
					sendBytes += bytes;
					os.write(buffer, 0, bytes);
					os.flush();
				}
				LOGGER.trace("Sending stream finished after: " + sendBytes + " bytes.");
			} catch (IOException e) {
				LOGGER.trace("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + e.getMessage());
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}

			try {
				os.close();
			} catch (IOException e) {
			}
		};
		new Thread(r).start();
	}

	public static void logMessageSent(HttpExchange exchange, String response, InputStream iStream) {
		StringBuilder header = new StringBuilder();
		for (Map.Entry<String, List<String>> headers : exchange.getResponseHeaders().entrySet()) {
			String name = headers.getKey();
			if (StringUtils.isNotBlank(name)) {
				for (String value : headers.getValue()) {
					header.append("  ").append(name).append(": ").append(value).append("\n");
				}
			}
		}
		if (header.length() > 0) {
			header.insert(0, "\nHEADER:\n");
		}

		String responseCode = exchange.getProtocol() + " " + exchange.getResponseCode();
		String remoteHost = exchange.getRemoteAddress().getHostString();
		if (StringUtils.isNotBlank(response)) {
			LOGGER.trace(
				"Response sent to {}:\n{}\n{}\n{}\nCONTENT:\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				header,
				response,
				HTTPSERVER_RESPONSE_END
			);
		} else if (iStream != null && !"0".equals(exchange.getResponseHeaders().getFirst("Content-Length"))) {
			LOGGER.trace(
				"Transfer response sent to {}:\n{}\n{} ({})\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				getResponseIsChunked(exchange) ? "chunked" : "non-chunked",
				header,
				HTTPSERVER_RESPONSE_END
			);
		} else {
			LOGGER.trace(
				"Empty response sent to {}:\n{}\n{}\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				header,
				HTTPSERVER_RESPONSE_END
			);
		}
	}

	public static void logMessageReceived(HttpExchange exchange, String content) {
		StringBuilder header = new StringBuilder();
		header.append(exchange.getRequestMethod());
		header.append(" ").append(exchange.getRequestURI());
		if (header.length() > 0) {
			header.append(" ");
		}
		header.append(exchange.getProtocol());
		header.append("\n\n");
		header.append("HEADER:\n");
		for (Map.Entry<String, List<String>> headers : exchange.getRequestHeaders().entrySet()) {
			String name = headers.getKey();
			if (StringUtils.isNotBlank(name)) {
				for (String value : headers.getValue()) {
					header.append("  ").append(name).append(": ").append(value).append("\n");
				}
			}
		}
		String formattedContent = StringUtils.isNotBlank(content) ? "\nCONTENT:\n" + content + "\n" : "";
		String remoteHost = exchange.getRemoteAddress().getHostString();
		LOGGER.trace(
				"Received a request from {}:\n{}\n{}{}{}",
				remoteHost,
				HTTPSERVER_REQUEST_BEGIN,
				header,
				formattedContent,
				HTTPSERVER_REQUEST_END
		);
	}

	private static boolean getResponseIsChunked(HttpExchange exchange) {
		return exchange.getResponseHeaders().containsKey("Transfer-Encoding") &&
				exchange.getResponseHeaders().getFirst("Transfer-Encoding").equalsIgnoreCase("chunked");
	}

	public static String read(File f) {
		try {
			return FileUtils.readFileToString(f, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.debug("Error reading file: " + e);
		}
		return null;
	}

	public static String getId(String path, HttpExchange t) {
		String id = "0";
		int pos = t.getRequestURI().getPath().indexOf(path);
		if (pos != -1) {
			id = t.getRequestURI().getPath().substring(pos + path.length());
		}
		return id;
	}

	public static String getId(String path, URI uri) {
		String id = "0";
		int pos = uri.getPath().indexOf(path);
		if (pos != -1) {
			id = uri.getPath().substring(pos + path.length());
		}
		return id;
	}

	public static String strip(String id) {
		int pos = id.lastIndexOf('.');
		if (pos != -1) {
			return id.substring(0, pos);
		}
		return id;
	}

	public static boolean deny(HttpExchange t) {
		return deny(t.getRemoteAddress().getAddress());
	}

	public static boolean deny(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress) || !PMS.isReady();
	}

	private static ByteRange nullRange(long len) {
		return new ByteRange(0L, len);
	}

	public static ByteRange parseRange(Headers hdr, long len) {
		if (hdr == null) {
			return nullRange(len);
		}
		List<String> r = hdr.get("Range");
		if (r == null) { // no range
			return nullRange(len);
		}
		// assume only one
		return parseRange(r.get(0), len);
	}

	public static ByteRange parseRange(String range, long len) {
		if (range == null || "".equals(range)) {
			return nullRange(len);
		}
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? len : Long.parseLong(tmp[1]);
		return new ByteRange(start, end);
	}

	public static void sendLogo(HttpExchange t) throws IOException {
		InputStream in = PMS.class.getResourceAsStream("/resources/images/logo.png");
		t.sendResponseHeaders(200, 0);
		OutputStream os = t.getResponseBody();
		dump(in, os);
	}

	/**
	 * Whether the MIME type is supported by all browsers.
	 * Note: This is a flawed approach because while browsers
	 * may support the container format, they may not support
	 * the codecs within. For example, most browsers support
	 * MP4 with H.264, but do not support it with H.265 (HEVC)
	 *
	 * @param mime
	 * @return
	 * @todo refactor to be more specific
	 */
	public static boolean directmime(String mime) {
		return mime != null &&
		(
			mime.equals(HTTPResource.MP4_TYPEMIME) ||
			mime.equals(HTTPResource.WEBM_TYPEMIME) ||
			mime.equals(HTTPResource.OGG_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_M4A_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_OGA_TYPEMIME) ||
			mime.equals(HTTPResource.AUDIO_WAV_TYPEMIME) ||
			mime.equals(HTTPResource.BMP_TYPEMIME) ||
			mime.equals(HTTPResource.PNG_TYPEMIME) ||
			mime.equals(HTTPResource.JPEG_TYPEMIME) ||
			mime.equals(HTTPResource.GIF_TYPEMIME)
		);
	}

	public static String userName(HttpExchange t) {
		HttpPrincipal p = t.getPrincipal();
		if (p == null) {
			return "";
		}
		return p.getUsername();
	}

	public static String getQueryVars(String query, String str) {
		if (StringUtils.isEmpty(query)) {
			return null;
		}
		for (String p : query.split("&")) {
			String[] pair = p.split("=");
			if (pair[0].equalsIgnoreCase(str)) {
				if (pair.length > 1 && StringUtils.isNotEmpty(pair[1])) {
					return pair[1];
				}
			}
		}
		return null;
	}

	/**
	 * @param exchange the HTTP exchange
	 * @return Array of query parameters
	 */
	public static Map<String, String> parseQueryString(HttpExchange exchange) {
		return parseQueryString(exchange.getRequestURI().getQuery());
	}

	/**
	 * @param qs the query string
	 * @return Array of query parameters
	 * @see https://stackoverflow.com/a/41610845/2049714
	 */
	public static Map<String, String> parseQueryString(String qs) {
		Map<String, String> result = new HashMap<>();
		if (qs == null) {
			return result;
		}

		int last = 0;
		int next;
		int l = qs.length();
		while (last < l) {
			next = qs.indexOf('&', last);
			if (next == -1) {
				next = l;
			}

			if (next > last) {
				int eqPos = qs.indexOf('=', last);
				try {
					if (eqPos < 0 || eqPos > next) {
						result.put(URLDecoder.decode(qs.substring(last, next), "utf-8"), "");
					} else {
						result.put(URLDecoder.decode(qs.substring(last, eqPos), "utf-8"), URLDecoder.decode(qs.substring(eqPos + 1, next), "utf-8"));
					}
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e); // will never happen, utf-8 support is mandatory for java
				}
			}
			last = next + 1;
		}
		return result;
	}

	public static String getPostString(HttpExchange exchange) {
		try {
			return IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return null;
		}
	}

	public static JsonObject getJsonObjectFromPost(HttpExchange exchange) {
		String reqBody = getPostString(exchange);
		return jsonObjectFromString(reqBody);
	}

	private static JsonObject jsonObjectFromString(String str) {
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem.isJsonObject()) {
				return jElem.getAsJsonObject();
			}
		} catch (JsonSyntaxException je) {
		}
		return null;
	}

	private static JsonArray jsonArrayFromString(String str) {
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem.isJsonArray()) {
				return jElem.getAsJsonArray();
			}
		} catch (JsonSyntaxException je) {
		}
		return null;
	}

	public static WebRender matchRenderer(String user, HttpExchange t) {
		int browser = WebRender.getBrowser(t.getRequestHeaders().getFirst("User-agent"));
		String confName = WebRender.getBrowserName(browser);
		Renderer renderer = ConnectedRenderers.find(confName, t.getRemoteAddress().getAddress());
		return (renderer instanceof WebRender webRender && (StringUtils.isBlank(user) || user.equals(webRender.getUser()))) ?
			webRender : null;
	}

	public static String getCookie(String name, HttpExchange t) {
		String cstr = t.getRequestHeaders().getFirst("Cookie");
		if (!StringUtils.isEmpty(cstr)) {
			name += "=";
			for (String str : cstr.trim().split("\\s*;\\s*")) {
				if (str.startsWith(name)) {
					return str.substring(name.length());
				}
			}
		}
		LOGGER.debug("Cookie '{}' not found: {}", name, t.getRequestHeaders().get("Cookie"));
		return null;
	}

	private static final int WIDTH = 0;
	private static final int HEIGHT = 1;

	private static final int DEFAULT_WIDTH = 720;
	private static final int DEFAULT_HEIGHT = 404;

	private static int getHW(int cfgVal, int id, int def) {
		if (cfgVal != 0) {
			// if we have a value cfg return that
			return cfgVal;
		}
		String s = PMS.getConfiguration().getWebSize();
		if (StringUtils.isEmpty(s)) {
			// no size string return default
			return def;
		}
		String[] tmp = s.split("x", 2);
		if (tmp.length < 2) {
			// bad format resort to default
			return def;
		}
		try {
			// pick whatever we got
			return Integer.parseInt(tmp[id]);
		} catch (NumberFormatException e) {
			// bad format (again) resort to default
			return def;
		}
	}

	public static int getHeight() {
		return getHW(PMS.getConfiguration().getWebHeight(), HEIGHT, DEFAULT_HEIGHT);
	}

	public static int getWidth() {
		return getHW(PMS.getConfiguration().getWebWidth(), WIDTH, DEFAULT_WIDTH);
	}

	public static boolean transMp4(String mime, DLNAMediaInfo media) {
		LOGGER.debug("mp4 profile " + media.getH264Profile());
		return mime.equals(HTTPResource.MP4_TYPEMIME) && (PMS.getConfiguration().isWebPlayerMp4Trans() || media.getAvcAsInt() >= 40);
	}

	private static IpFilter bumpFilter = null;

	public static boolean bumpAllowed(HttpExchange t) {
		return bumpAllowed(t.getRemoteAddress().getAddress());
	}

	public static boolean bumpAllowed(InetAddress inetAddress) {
		if (bumpFilter == null) {
			bumpFilter = new IpFilter(PMS.getConfiguration().getBumpAllowedIps());
		}
		return bumpFilter.allowed(inetAddress);
	}

	public static String transMime() {
		return MIME_TRANS;
	}

	public static String getContentType(String filename) {
		return filename.endsWith(".html") ? "text/html" :
			filename.endsWith(".css") ? "text/css" :
			filename.endsWith(".js") ? "text/javascript" :
			filename.endsWith(".ttf") ? "font/truetype" :
			URLConnection.guessContentTypeFromName(filename);
	}

	public static Template compile(InputStream stream) {
		try {
			return Mustache.compiler().escapeHTML(false).compile(new InputStreamReader(stream));
		} catch (Exception e) {
			LOGGER.debug("Error compiling mustache template: " + e);
		}
		return null;
	}

	public static Set<String> getLangs(HttpExchange t) {
		String hdr = t.getRequestHeaders().getFirst("Accept-language");
		Set<String> result = new LinkedHashSet<>();
		if (StringUtils.isEmpty(hdr)) {
			return result;
		}

		String[] tmp = hdr.split(",");
		for (String language : tmp) {
			String[] l1 = language.split(";");
			result.add(l1[0]);
		}
		return result;
	}

	public static String getFirstSupportedLanguage(HttpExchange t) {
		Set<String> languages = getLangs(t);
		for (String language : languages) {
			String code = Languages.toLanguageTag(language);
			if (code != null) {
				return code;
			}
		}
		return "";
	}

	public static String getMsgString(String key, HttpExchange t) {
		if (PMS.getConfiguration().useWebLang()) {
			String lang = getFirstSupportedLanguage(t);
			if (!lang.isEmpty()) {
				return Messages.getString(key, Locale.forLanguageTag(lang));
			}
		}
		return Messages.getString(key);
	}

	public static String getMsgString(String key, String lang) {
		if (PMS.getConfiguration().useWebLang()) {
			if (!lang.isEmpty()) {
				return Messages.getString(key, Locale.forLanguageTag(lang));
			}
		}
		return Messages.getString(key);
	}

	/**
	 * A web resource manager to act as:
	 *
	 * - A resource finder with native java classpath search behaviour (including in zip files)
	 *   to allow flexible customizing/skinning of the web player server.
	 *
	 * - A file manager to control access to arbitrary non-web resources, i.e. subtitles,
	 *   logs, etc.
	 *
	 * - A template manager.
	 */
	public static class ResourceManager extends URLClassLoader {
		private final Set<File> files = new HashSet<>();
		private final Map<String, Template> templates = new HashMap<>();

		public ResourceManager(String... urls) {
			super(new URL[]{}, null);
			try {
				for (String url : urls) {
					super.addURL(new URL(url));
				}
			} catch (MalformedURLException e) {
				LOGGER.debug("Error adding resource url: " + e);
			}
		}

		public InputStream getInputStream(String filename) {
			InputStream stream = getResourceAsStream(filename);
			if (stream == null) {
				File file = getFile(filename);
				if (file != null && file.exists()) {
					try {
						stream = new FileInputStream(file);
					} catch (FileNotFoundException e) {
						LOGGER.debug("Error opening stream: " + e);
					}
				}
			}
			return stream;
		}

		@Override
		public URL getResource(String name) {
			URL url = super.getResource(name);
			if (url != null) {
				LOGGER.debug("Using resource: " + url);
			}
			return url;
		}

		/**
		 * Register a file as servable.
		 *
		 * @param f
		 * @return its hashcode (for use as a 'filename' in an http path)
		 */
		public int add(File f) {
			files.add(f);
			return f.hashCode();
		}

		/**
		 * Retrieve a servable file by its hashcode.
		 * @param hash
		 * @return
		 */
		public File getFile(String hash) {
			try {
				int h = Integer.parseInt(hash);
				for (File f : files) {
					if (f.hashCode() == h) {
						return f;
					}
				}
			} catch (NumberFormatException e) {
			}
			return null;
		}

		public String read(String filename) {
			try {
				return IOUtils.toString(getInputStream(filename), StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.debug("Error reading resource {}: {}", filename, e);
			}
			return null;
		}

		/**
		 * Write the given resource as an http response body.
		 * @param filename
		 * @param t
		 * @return
		 * @throws java.io.IOException
		 */
		public boolean write(String filename, HttpExchange t) throws IOException {
			InputStream stream = getInputStream(filename);
			if (stream != null) {
				Headers headers = t.getResponseHeaders();
				if (!headers.containsKey("Content-Type")) {
					String mime = getContentType(filename);
					if (mime != null) {
						headers.add("Content-Type", mime);
					}
				}
				//add cache for js and css versionned
				if (filename.startsWith("util/") && t.getRequestURI() != null && t.getRequestURI().getQuery() != null && t.getRequestURI().getQuery().startsWith("v=")) {
					headers.add("Cache-Control", "public, max-age=604800");
				}
				// Note: available() isn't officially guaranteed to return the full
				// stream length but effectively seems to do so in our context.
				t.sendResponseHeaders(200, stream.available());
				dump(stream, t.getResponseBody());
				return true;
			}
			return false;
		}

		/**
		 * Retrieve the given mustache template, compiling as necessary.
		 * @param filename
		 * @return
		 */
		public Template getTemplate(String filename) {
			Template t = null;
			if (templates.containsKey(filename)) {
				t = templates.get(filename);
			} else {
				URL url = findResource(filename);
				if (url != null) {
					t = compile(getInputStream(filename));
					templates.put(filename, t);
					FileWatcher.add(new FileWatcher.Watch(url.getFile(), recompiler));
				} else {
					LOGGER.warn("Couldn't find web template \"{}\"", filename);
				}
			}
			return t;
		}

		/**
		 * Automatic recompiling
		 */
		FileWatcher.Listener recompiler = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
			String path = watch.getFileSpec().startsWith("web/") ? watch.getFileSpec().substring(4) : watch.getFileSpec();
			if (templates.containsKey(path)) {
				templates.put(path, compile(getInputStream(path)));
				LOGGER.info("Recompiling template: {}", path);
			}
		};
	}

	/**
	 * Gets metadata from our database, which may be there from our API, for
	 * this resource, which could be a TV series, TV episode, or movie.
	 *
	 * @param resource
	 * @param language
	 * @param isTVSeries whether this is a TV series, or an episode/movie
	 * @param rootFolder the root folder, used for looking up IDs
	 * @return a JsonObject to be used by a web browser which includes
	 *         metadata names and when applicable, associated IDs, or null
	 *         when there is no metadata
	 */
	public static JsonObject getAPIMetadataAsJsonObject(DLNAResource resource, String language, boolean isTVSeries, RootFolder rootFolder) {
		JsonObject result = getAPIMetadataAsJsonObject(resource, isTVSeries, rootFolder);
		if (result != null) {
			result.addProperty("actorsTranslation", WebInterfaceServerUtil.getMsgString("Actors", language));
			result.addProperty("awardsTranslation", WebInterfaceServerUtil.getMsgString("Awards", language));
			result.addProperty("countryTranslation", WebInterfaceServerUtil.getMsgString("Country", language));
			result.addProperty("directorTranslation", WebInterfaceServerUtil.getMsgString("Director", language));
			result.addProperty("genresTranslation", WebInterfaceServerUtil.getMsgString("Genres", language));
			result.addProperty("ratedTranslation", WebInterfaceServerUtil.getMsgString("Rated", language));
			result.addProperty("ratingsTranslation", WebInterfaceServerUtil.getMsgString("Ratings", language));
			result.addProperty("totalSeasonsTranslation", WebInterfaceServerUtil.getMsgString("TotalSeasons", language));
			result.addProperty("plotTranslation", WebInterfaceServerUtil.getMsgString("Plot", language));
			result.addProperty("yearStartedTranslation", WebInterfaceServerUtil.getMsgString("YearStarted", language));
		}
		return result;
	}

	/**
	 * Gets metadata from our database, which may be there from our API, for
	 * this resource, which could be a TV series, TV episode, or movie.
	 *
	 * @param resource
	 * @param isTVSeries whether this is a TV series, or an episode/movie
	 * @param rootFolder the root folder, used for looking up IDs
	 * @return a JsonObject to be used by a web browser which includes
	 *         metadata names and when applicable, associated IDs, or null
	 *         when there is no metadata
	 */
	public static JsonObject getAPIMetadataAsJsonObject(DLNAResource resource, boolean isTVSeries, RootFolder rootFolder) {
		JsonObject result = null;
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				if (isTVSeries) {
					String simplifiedTitle = resource.getDisplayName() != null ? FileUtil.getSimplifiedShowName(resource.getDisplayName()) : resource.getName();
					result = MediaTableTVSeries.getTvSeriesMetadataAsJsonObject(connection, simplifiedTitle, null);
				} else {
					result = MediaTableVideoMetadata.getVideoMetadataAsJsonObject(connection, resource.getFileName(), null);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting metadata for web interface");
			LOGGER.debug("", e);
		}
		if (result == null) {
			return null;
		}
		DLNAResource actorsFolder = null;
		DLNAResource countriesFolder = null;
		DLNAResource directorsFolder = null;
		DLNAResource genresFolder = null;
		DLNAResource ratedFolder = null;

		// prepare to get IDs of certain metadata resources, to make them clickable
		List<DLNAResource> rootFolderChildren = rootFolder.getDLNAResources("0", true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("MediaLibrary"));
		UMSUtils.filterResourcesByName(rootFolderChildren, Messages.getString("MediaLibrary"), true, true);
		if (rootFolderChildren.isEmpty()) {
			return null;
		}
		DLNAResource mediaLibraryFolder = rootFolderChildren.get(0);
		List<DLNAResource> mediaLibraryChildren = mediaLibraryFolder.getDLNAResources(mediaLibraryFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("Video"));
		UMSUtils.filterResourcesByName(mediaLibraryChildren, Messages.getString("Video"), true, true);
		DLNAResource videoFolder = mediaLibraryChildren.get(0);

		boolean isRelatedToTV = isTVSeries || resource.isEpisodeWithinSeasonFolder() || resource.isEpisodeWithinTVSeriesFolder();
		String folderName = isRelatedToTV ? Messages.getString("TvShows") : Messages.getString("Movies");
		List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), folderName);
		UMSUtils.filterResourcesByName(videoFolderChildren, folderName, true, true);
		DLNAResource tvShowsOrMoviesFolder = videoFolderChildren.get(0);

		List<DLNAResource> tvShowsOrMoviesChildren = tvShowsOrMoviesFolder.getDLNAResources(tvShowsOrMoviesFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("FilterByInformation"));
		UMSUtils.filterResourcesByName(tvShowsOrMoviesChildren, Messages.getString("FilterByInformation"), true, true);
		DLNAResource filterByInformationFolder = tvShowsOrMoviesChildren.get(0);

		List<DLNAResource> filterByInformationChildren = filterByInformationFolder.getDLNAResources(filterByInformationFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("Genres"));

		for (int filterByInformationChildrenIterator = 0; filterByInformationChildrenIterator < filterByInformationChildren.size(); filterByInformationChildrenIterator++) {
			DLNAResource filterByInformationChild = filterByInformationChildren.get(filterByInformationChildrenIterator);
			if (filterByInformationChild.getDisplayName().equals(Messages.getString("Actors"))) {
				actorsFolder = filterByInformationChild;
			} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Country"))) {
				countriesFolder = filterByInformationChild;
			} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Director"))) {
				directorsFolder = filterByInformationChild;
			} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Genres"))) {
				genresFolder = filterByInformationChild;
			} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("Rated"))) {
				ratedFolder = filterByInformationChild;
			}
		}

		addJsonArrayDlnaIds(result, "actors", actorsFolder, rootFolder);
		addJsonArrayDlnaIds(result, "countries", countriesFolder, rootFolder);
		addJsonArrayDlnaIds(result, "directors", directorsFolder, rootFolder);
		addJsonArrayDlnaIds(result, "genres", genresFolder, rootFolder);
		addStringDlnaId(result, "rated", ratedFolder, rootFolder);
		result.addProperty("imageBaseURL", APIUtils.getApiImageBaseURL());

		return result;
	}

	private static void addJsonArrayDlnaIds(final JsonObject object, final String memberName, final DLNAResource folder, final RootFolder rootFolder) {
		if (object.has(memberName)) {
			JsonElement element = object.remove(memberName);
			if (element.isJsonArray()) {
				JsonArray array = element.getAsJsonArray();
				if (!array.isEmpty() && folder != null) {
					JsonArray dlnaChilds = new JsonArray();
					for (JsonElement child : array) {
						if (child.isJsonPrimitive()) {
							String value = child.getAsString();
							List<DLNAResource> folderChildren = folder.getDLNAResources(folder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), value);
							UMSUtils.filterResourcesByName(folderChildren, value, true, true);
							if (!folderChildren.isEmpty()) {
								JsonObject dlnaChild = new JsonObject();
								dlnaChild.addProperty("id", folderChildren.get(0).getId());
								dlnaChild.addProperty("name", value);
								dlnaChilds.add(dlnaChild);
							}
						}
					}
					object.add(memberName, dlnaChilds);
				}
			}
		}
	}

	private static void addStringDlnaId(final JsonObject object, final String memberName, final DLNAResource folder, final RootFolder rootFolder) {
		if (object.has(memberName)) {
			JsonElement element = object.remove(memberName);
			if (element.isJsonPrimitive() && folder != null) {
				String value = element.getAsString();
				List<DLNAResource> folderChildren = folder.getDLNAResources(folder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), value);
				UMSUtils.filterResourcesByName(folderChildren, value, true, true);
				if (!folderChildren.isEmpty()) {
					JsonObject dlnaChild = new JsonObject();
					dlnaChild.addProperty("id", folderChildren.get(0).getId());
					dlnaChild.addProperty("name", value);
					object.add(memberName, dlnaChild);
				}
			}
		}
	}

}
