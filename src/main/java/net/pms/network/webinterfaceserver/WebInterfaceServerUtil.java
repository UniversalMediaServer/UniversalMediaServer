/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.network.webinterfaceserver;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.IpFilter;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableTVSeries;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.network.HTTPResource;
import net.pms.newgui.LooksFrame;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import net.pms.util.Languages;
import net.pms.util.UMSUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WebInterfaceServerUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebInterfaceServerUtil.class);

	//public static final String MIME_TRANS = MIME_MP4;
	public static final String MIME_TRANS = HTTPResource.OGG_TYPEMIME;
	//public static final String MIME_TRANS = MIME_WEBM;

	private static final String HTTPSERVER_REQUEST_BEGIN =  "============================= INTERFACE HTTPSERVER REQUEST BEGIN ================================";
	private static final String HTTPSERVER_REQUEST_END =    "============================= INTERFACE HTTPSERVER REQUEST END ==================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "============================= INTERFACE HTTPSERVER RESPONSE BEGIN ===============================";
	private static final String HTTPSERVER_RESPONSE_END =   "============================= INTERFACE HTTPSERVER RESPONSE END =================================";

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
				exchange.getResponseHeaders().getFirst("Transfer-Encoding").toLowerCase().equals("chunked");
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

	private static Range.Byte nullRange(long len) {
		return new Range.Byte(0L, len);
	}

	public static Range.Byte parseRange(Headers hdr, long len) {
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

	public static Range.Byte parseRange(String range, long len) {
		if (range == null || "".equals(range)) {
			return nullRange(len);
		}
		String[] tmp = range.split("=")[1].split("-");
		long start = Long.parseLong(tmp[0]);
		long end = tmp.length == 1 ? len : Long.parseLong(tmp[1]);
		return new Range.Byte(start, end);
	}

	public static void sendLogo(HttpExchange t) throws IOException {
		InputStream in = LooksFrame.class.getResourceAsStream("/resources/images/logo.png");
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
	 * @todo refactor to be more specific
	 */
	public static boolean directmime(String mime) {
		if (
			mime != null &&
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
			)
		) {
			return true;
		}

		return false;
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

	public static WebRender matchRenderer(String user, HttpExchange t) {
		int browser = WebRender.getBrowser(t.getRequestHeaders().getFirst("User-agent"));
		String confName = WebRender.getBrowserName(browser);
		RendererConfiguration r = RendererConfiguration.find(confName, t.getRemoteAddress().getAddress());
		return ((r instanceof WebRender) && (StringUtils.isBlank(user) || user.equals(((WebRender) r).getUser()))) ?
			(WebRender) r : null;
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
		return mime.equals(HTTPResource.MP4_TYPEMIME) && (PMS.getConfiguration().isWebMp4Trans() || media.getAvcAsInt() >= 40);
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

	public static LinkedHashSet<String> getLangs(HttpExchange t) {
		String hdr = t.getRequestHeaders().getFirst("Accept-language");
		LinkedHashSet<String> result = new LinkedHashSet<>();
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
		LinkedHashSet<String> languages = getLangs(t);
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
		private final HashSet<File> files;
		private final HashMap<String, Template> templates;

		public ResourceManager(String... urls) {
			super(new URL[]{}, null);
			try {
				for (String url : urls) {
					super.addURL(new URL(url));
				}
			} catch (MalformedURLException e) {
				LOGGER.debug("Error adding resource url: " + e);
			}
			files = new HashSet<>();
			templates = new HashMap<>();
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
		 * @return its hashcode (for use as a 'filename' in an http path)
		 */
		public int add(File f) {
			files.add(f);
			return f.hashCode();
		}

		/**
		 * Retrieve a servable file by its hashcode.
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
					PMS.getFileWatcher().add(new FileWatcher.Watch(url.getFile(), recompiler));
				} else {
					LOGGER.warn("Couldn't find web template \"{}\"", filename);
				}
			}
			return t;
		}

		/**
		 * Automatic recompiling
		 */
		FileWatcher.Listener recompiler = new FileWatcher.Listener() {
			@Override
			public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir) {
				String path = watch.fspec.startsWith("web/") ? watch.fspec.substring(4) : watch.fspec;
				if (templates.containsKey(path)) {
					templates.put(path, compile(getInputStream(path)));
					LOGGER.info("Recompiling template: {}", path);
				}
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
	 * @return a JavaScript string to be used by a web browser which includes
	 *         metadata names and when applicable, associated IDs, or null
	 *         when there is no metadata
	 */
	public static String getAPIMetadataAsJavaScriptVars(DLNAResource resource, String language, boolean isTVSeries, RootFolder rootFolder) {
		String javascriptVarsScript = "";

		try {
			List<HashMap<String, Object>> resourceMetadataFromDatabase = null;

			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					if (isTVSeries) {
						String simplifiedTitle = resource.getDisplayName() != null ? FileUtil.getSimplifiedShowName(resource.getDisplayName()) : resource.getName();
						resourceMetadataFromDatabase = MediaTableTVSeries.getAPIResultsBySimplifiedTitleIncludingExternalTables(connection, simplifiedTitle);
					} else {
						resourceMetadataFromDatabase = MediaTableFiles.getAPIResultsByFilenameIncludingExternalTables(connection, resource.getFileName());
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error while getting metadata for web interface");
				LOGGER.debug("", e);
			} finally {
				MediaDatabase.close(connection);
			}

			if (resourceMetadataFromDatabase == null) {
				return null;
			}

			HashSet<String> actors = new HashSet();
			String startYear = "";
			String awards = "";
			String country = "{}";
			String director = "{}";
			HashSet<String> genres = new HashSet();
			String imdbID = "";
			String rated = "{}";
			List<HashMap<String, String>> ratings = new ArrayList<>();
			String plot = "";
			String poster = "";
			Double totalSeasons = null;

			// TMDB metadata added in V11
			String createdBy = "";
			String credits = "";
			String externalIDs = "";
			String firstAirDate = "";
			String homepage = "";
			String images = "[]";
			Boolean inProduction = null;
			String languages = "";
			String lastAirDate = "";
			String networks = "";
			Double numberOfEpisodes = null;
			String numberOfSeasons = "";
			String originCountry = "";
			String originalLanguage = "";
			String originalTitle = "";
			String productionCompanies = "";
			String productionCountries = "";
			String seasons = "";
			String seriesImages = "[]";
			String seriesType = "";
			String spokenLanguages = "";
			String status = "";
			String tagline = "";


			Boolean hasAPIMetadata = false;

			DLNAResource actorsFolder = null;
			DLNAResource countryFolder = null;
			DLNAResource directorFolder = null;
			DLNAResource genresFolder = null;
			DLNAResource ratedFolder = null;

			List<DLNAResource> actorsChildren = null;
			List<DLNAResource> genresChildren = null;

			Iterator<HashMap<String, Object>> i = resourceMetadataFromDatabase.iterator();
			while (i.hasNext()) {
				if (genresFolder == null) {
					// prepare to get IDs of certain metadata resources, to make them clickable
					List<DLNAResource> rootFolderChildren = rootFolder.getDLNAResources("0", true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("PMS.MediaLibrary"));
					UMSUtils.filterResourcesByName(rootFolderChildren, Messages.getString("PMS.MediaLibrary"), true, true);
					if (rootFolderChildren.isEmpty()) {
						return null;
					}
					DLNAResource mediaLibraryFolder = rootFolderChildren.get(0);

					List<DLNAResource> mediaLibraryChildren = mediaLibraryFolder.getDLNAResources(mediaLibraryFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("PMS.34"));
					UMSUtils.filterResourcesByName(mediaLibraryChildren, Messages.getString("PMS.34"), true, true);
					DLNAResource videoFolder = mediaLibraryChildren.get(0);

					boolean isRelatedToTV = isTVSeries || resource.isEpisodeWithinSeasonFolder() || resource.isEpisodeWithinTVSeriesFolder();
					String folderName = isRelatedToTV ? Messages.getString("VirtualFolder.4") : Messages.getString("VirtualFolder.5");
					List<DLNAResource> videoFolderChildren = videoFolder.getDLNAResources(videoFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), folderName);
					UMSUtils.filterResourcesByName(videoFolderChildren, folderName, true, true);
					DLNAResource tvShowsOrMoviesFolder = videoFolderChildren.get(0);

					List<DLNAResource> tvShowsOrMoviesChildren = tvShowsOrMoviesFolder.getDLNAResources(tvShowsOrMoviesFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("VirtualFolder.FilterByInformation"));
					UMSUtils.filterResourcesByName(tvShowsOrMoviesChildren, Messages.getString("VirtualFolder.FilterByInformation"), true, true);
					DLNAResource filterByInformationFolder = tvShowsOrMoviesChildren.get(0);

					List<DLNAResource> filterByInformationChildren = filterByInformationFolder.getDLNAResources(filterByInformationFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), Messages.getString("VirtualFolder.Genres"));

					for (int filterByInformationChildrenIterator = 0; filterByInformationChildrenIterator < filterByInformationChildren.size(); filterByInformationChildrenIterator++) {
						DLNAResource filterByInformationChild = filterByInformationChildren.get(filterByInformationChildrenIterator);
						if (filterByInformationChild.getDisplayName().equals(Messages.getString("VirtualFolder.Actors"))) {
							actorsFolder = filterByInformationChild;
						} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("VirtualFolder.Country"))) {
							countryFolder = filterByInformationChild;
						} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("VirtualFolder.Director"))) {
							directorFolder = filterByInformationChild;
						} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("VirtualFolder.Genres"))) {
							genresFolder = filterByInformationChild;
						} else if (filterByInformationChild.getDisplayName().equals(Messages.getString("VirtualFolder.Rated"))) {
							ratedFolder = filterByInformationChild;
						}
					}

					hasAPIMetadata = true;
				}

				HashMap<String, Object> row = i.next();
				if (StringUtils.isNotBlank((String) row.get("AWARD"))) {
					awards = (String) row.get("AWARD");
				}
				if (StringUtils.isNotBlank((String) row.get("COUNTRY")) && "{}".equals(country) && countryFolder != null) {
					String countryValue = (String) row.get("COUNTRY");
					List<DLNAResource> countriesChildren = countryFolder.getDLNAResources(countryFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), countryValue);
					UMSUtils.filterResourcesByName(countriesChildren, countryValue, true, true);
					if (countriesChildren.isEmpty()) {
						country = "{ }";
					} else {
						DLNAResource filteredCountryFolder = countriesChildren.get(0);

						String countryId = filteredCountryFolder.getId();
						String countryIdForWeb = URLEncoder.encode(countryId, "UTF-8");

						country = "{ id: \"" + countryIdForWeb + "\", name: \"" + StringEscapeUtils.escapeEcmaScript(countryValue) + "\" }";
					}
				}
				if (StringUtils.isNotBlank((String) row.get("DIRECTOR")) && "{}".equals(director) && directorFolder != null) {
					String directorValue = (String) row.get("DIRECTOR");
					List<DLNAResource> directorsChildren = directorFolder.getDLNAResources(directorFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), directorValue);
					UMSUtils.filterResourcesByName(directorsChildren, directorValue, true, true);
					if (directorsChildren.isEmpty()) {
						/**
						* This is usually caused by TV episodes that have a director saved
						* that is not the director of the TV series. One possible fix for
						* that is to populate the TV series data with the episode data in
						* that case, which would mean we need to support multiple directors
						* as we do for actors and genres.
						*
						* For now we stop the code from erroring by ignoring the mismatch.
						*
						* @todo do the above fix, and the same fix for other similar folders.
						*/
						director = "{ }";
					} else {
						DLNAResource filteredDirectorFolder = directorsChildren.get(0);

						String directorId = filteredDirectorFolder.getId();
						String directorIdForWeb = URLEncoder.encode(directorId, "UTF-8");

						director = "{ id: \"" + directorIdForWeb + "\", name: \"" + StringEscapeUtils.escapeEcmaScript(directorValue) + "\" }";
					}
				}
				if (StringUtils.isNotBlank((String) row.get("IMDBID"))) {
					imdbID = (String) row.get("IMDBID");
				}
				if (StringUtils.isNotBlank((String) row.get("PLOT"))) {
					plot = (String) row.get("PLOT");
				}
				if (StringUtils.isNotBlank((String) row.get("POSTER"))) {
					poster = (String) row.get("POSTER");
				}
				if (StringUtils.isNotBlank((String) row.get("RATING")) && "{}".equals(rated) && ratedFolder != null) {
					String ratedValue = (String) row.get("RATING");
					List<DLNAResource> ratedChildren = ratedFolder.getDLNAResources(ratedFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), ratedValue);
					UMSUtils.filterResourcesByName(ratedChildren, ratedValue, true, true);
					if (ratedChildren.isEmpty()) {
						rated = "{ }";
					} else {
						DLNAResource filteredRatedFolder = ratedChildren.get(0);

						String ratedId = filteredRatedFolder.getId();
						String ratedIdForWeb = URLEncoder.encode(ratedId, "UTF-8");

						rated = "{ id: \"" + ratedIdForWeb + "\", name: \"" + StringEscapeUtils.escapeEcmaScript(ratedValue) + "\" }";
					}
				}
				if (StringUtils.isNotBlank((String) row.get("RATINGVALUE")) && StringUtils.isNotBlank((String) row.get("RATINGSOURCE"))) {
					HashMap<String, String> ratingToInsert = new HashMap();
					ratingToInsert.put("source", (String) row.get("RATINGSOURCE"));
					ratingToInsert.put("value", (String) row.get("RATINGVALUE"));
					if (!ratings.contains(ratingToInsert)) {
						ratings.add(ratingToInsert);
					}
				}
				if (StringUtils.isNotBlank((String) row.get("STARTYEAR"))) {
					startYear = (String) row.get("STARTYEAR");
				}
				if (row.get("TOTALSEASONS") != null && (Double) row.get("TOTALSEASONS") != 0.0) {
					totalSeasons = (Double) row.get("TOTALSEASONS");
				}

				// TMDB metadata added in V11
				if (StringUtils.isNotBlank((String) row.get("CREATEDBY"))) {
					createdBy = (String) row.get("CREATEDBY");
				}
				if (StringUtils.isNotBlank((String) row.get("CREDITS"))) {
					credits = (String) row.get("CREDITS");
				}
				if (StringUtils.isNotBlank((String) row.get("EXTERNALIDS"))) {
					externalIDs = (String) row.get("EXTERNALIDS");
				}
				if (StringUtils.isNotBlank((String) row.get("FIRSTAIRDATE"))) {
					firstAirDate = (String) row.get("FIRSTAIRDATE");
				}
				if (StringUtils.isNotBlank((String) row.get("HOMEPAGE"))) {
					homepage = (String) row.get("HOMEPAGE");
				}

				if (row.get("ISTVEPISODE") != null && (Boolean) row.get("ISTVEPISODE") && StringUtils.isNotBlank((String) row.get("MOVIEORSHOWNAME"))) {
					connection = null;
					try {
						connection = MediaDatabase.getConnectionIfAvailable();
						if (connection != null) {
							String simplifiedShowName = FileUtil.getSimplifiedShowName((String) row.get("MOVIEORSHOWNAME"));
							List<HashMap<String, Object>> optionalSeriesMetadataFromDatabase = MediaTableTVSeries.getAPIResultsBySimplifiedTitleIncludingExternalTables(connection, simplifiedShowName);
							Iterator<HashMap<String, Object>> seriesIterator = optionalSeriesMetadataFromDatabase.iterator();
							if (optionalSeriesMetadataFromDatabase != null) {
								HashMap<String, Object> seriesRow = seriesIterator.next();
								if (StringUtils.isNotBlank((String) seriesRow.get("IMAGES"))) {
									seriesImages = (String) seriesRow.get("IMAGES");
								}
							}
						}
					} catch (Exception e) {
						LOGGER.error("Error while getting series metadata for web interface");
						LOGGER.debug("", e);
					} finally {
						MediaDatabase.close(connection);
					}
				}

				if (StringUtils.isNotBlank((String) row.get("IMAGES"))) {
					images = (String) row.get("IMAGES");
				}
				if (row.get("INPRODUCTION") != null) {
					inProduction = (Boolean) row.get("INPRODUCTION");
				}
				if (StringUtils.isNotBlank((String) row.get("LANGUAGES"))) {
					languages = (String) row.get("LANGUAGES");
				}
				if (StringUtils.isNotBlank((String) row.get("LASTAIRDATE"))) {
					lastAirDate = (String) row.get("LASTAIRDATE");
				}
				if (StringUtils.isNotBlank((String) row.get("NETWORKS"))) {
					networks = (String) row.get("NETWORKS");
				}
				if (row.get("NUMBEROFEPISODES") != null) {
					numberOfEpisodes = (Double) row.get("NUMBEROFEPISODES");
				}
				if (StringUtils.isNotBlank((String) row.get("NUMBEROFSEASONS"))) {
					numberOfSeasons = (String) row.get("NUMBEROFSEASONS");
				}
				if (StringUtils.isNotBlank((String) row.get("ORIGINCOUNTRY"))) {
					originCountry = (String) row.get("ORIGINCOUNTRY");
				}
				if (StringUtils.isNotBlank((String) row.get("ORIGINALLANGUAGE"))) {
					originalLanguage = (String) row.get("ORIGINALLANGUAGE");
				}
				if (StringUtils.isNotBlank((String) row.get("ORIGINALTITLE"))) {
					originalTitle = (String) row.get("ORIGINALTITLE");
				}
				if (StringUtils.isNotBlank((String) row.get("PRODUCTIONCOMPANIES"))) {
					productionCompanies = (String) row.get("PRODUCTIONCOMPANIES");
				}
				if (StringUtils.isNotBlank((String) row.get("PRODUCTIONCOUNTRIES"))) {
					productionCountries = (String) row.get("PRODUCTIONCOUNTRIES");
				}
				if (StringUtils.isNotBlank((String) row.get("SEASONS"))) {
					seasons = (String) row.get("SEASONS");
				}
				if (StringUtils.isNotBlank((String) row.get("SERIESTYPE"))) {
					seriesType = (String) row.get("SERIESTYPE");
				}
				if (StringUtils.isNotBlank((String) row.get("SPOKENLANGUAGES"))) {
					spokenLanguages = (String) row.get("SPOKENLANGUAGES");
				}
				if (StringUtils.isNotBlank((String) row.get("STATUS"))) {
					status = (String) row.get("STATUS");
				}
				if (StringUtils.isNotBlank((String) row.get("TAGLINE"))) {
					tagline = (String) row.get("TAGLINE");
				}

				// These are for records that can have multiple results
				if (StringUtils.isNotBlank((String) row.get("ACTOR")) && actorsFolder != null) {
					String actor = (String) row.get("ACTOR");
					String namePartOfJSObject = ", name: \"" + StringEscapeUtils.escapeEcmaScript(actor) + "\"";
					if (!actors.contains(namePartOfJSObject)) {
						if (actorsChildren == null) {
							actorsChildren = actorsFolder.getDLNAResources(actorsFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), actor);
						}
						for (int actorsIterator = 0; actorsIterator < actorsChildren.size(); actorsIterator++) {
							DLNAResource filterByInformationChild = actorsChildren.get(actorsIterator);
							if (filterByInformationChild.getDisplayName().equals(actor)) {
								DLNAResource actorFolder = filterByInformationChild;

								String actorId = actorFolder.getId();
								String actorIdForWeb = URLEncoder.encode(actorId, "UTF-8");

								actors.add("{ id: \"" + actorIdForWeb + "\"" + namePartOfJSObject + " }");
								break;
							}
						}
					}
				}
				if (StringUtils.isNotBlank((String) row.get("GENRE")) && genresFolder != null) {
					String genre = (String) row.get("GENRE");
					String namePartOfJSObject = ", name: \"" + StringEscapeUtils.escapeEcmaScript(genre) + "\"";
					if (!genres.contains(namePartOfJSObject)) {
						if (genresChildren == null) {
							genresChildren = genresFolder.getDLNAResources(genresFolder.getId(), true, 0, 0, rootFolder.getDefaultRenderer(), genre);
						}
						for (int genresIterator = 0; genresIterator < genresChildren.size(); genresIterator++) {
							DLNAResource filterByInformationChild = genresChildren.get(genresIterator);
							if (filterByInformationChild.getDisplayName().equals(genre)) {
								DLNAResource genreFolder = filterByInformationChild;

								String genreId = genreFolder.getId();
								String genreIdForWeb = URLEncoder.encode(genreId, "UTF-8");

								genres.add("{ id: \"" + genreIdForWeb + "\"" + namePartOfJSObject + " }");
								break;
							}
						}
					}
				}
			}

			if (!hasAPIMetadata) {
				return null;
			}

			javascriptVarsScript = "";
			javascriptVarsScript += "var awards = \"" + StringEscapeUtils.escapeEcmaScript(awards) + "\";";
			javascriptVarsScript += "var awardsTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Awards", language) + "\";";
			javascriptVarsScript += "var country = " + country + ";";
			javascriptVarsScript += "var countryTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Country", language) + "\";";
			javascriptVarsScript += "var director = " + director + ";";
			javascriptVarsScript += "var directorTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Director", language) + "\";";
			javascriptVarsScript += "var imdbID = \"" + StringEscapeUtils.escapeEcmaScript(imdbID) + "\";";
			javascriptVarsScript += "var plot = \"" + StringEscapeUtils.escapeEcmaScript(plot) + "\";";
			javascriptVarsScript += "var plotTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Plot", language) + "\";";
			javascriptVarsScript += "var poster = \"" + StringEscapeUtils.escapeEcmaScript(poster) + "\";";
			javascriptVarsScript += "var rated = " + rated + ";";
			javascriptVarsScript += "var ratedTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Rated", language) + "\";";
			javascriptVarsScript += "var startYear = \"" + StringEscapeUtils.escapeEcmaScript(startYear) + "\";";
			javascriptVarsScript += "var yearStartedTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.YearStarted", language) + "\";";
			javascriptVarsScript += "var totalSeasons = " + totalSeasons + ";";
			javascriptVarsScript += "var totalSeasonsTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.TotalSeasons", language) + "\";";

			// TMDB metadata added in V11
			javascriptVarsScript += "var createdBy = \"" + StringEscapeUtils.escapeEcmaScript(createdBy) + "\";";
			javascriptVarsScript += "var credits = \"" + StringEscapeUtils.escapeEcmaScript(credits) + "\";";
			javascriptVarsScript += "var externalIDs = \"" + StringEscapeUtils.escapeEcmaScript(externalIDs) + "\";";
			javascriptVarsScript += "var firstAirDate = \"" + StringEscapeUtils.escapeEcmaScript(firstAirDate) + "\";";
			javascriptVarsScript += "var homepage = \"" + StringEscapeUtils.escapeEcmaScript(homepage) + "\";";
			javascriptVarsScript += "var imageBaseURL = \"" + APIUtils.getApiImageBaseURL() + "\";";
			javascriptVarsScript += "var images = " + images + ";";
			javascriptVarsScript += "var inProduction = " + inProduction + ";";
			javascriptVarsScript += "var languages = \"" + StringEscapeUtils.escapeEcmaScript(languages) + "\";";
			javascriptVarsScript += "var lastAirDate = \"" + StringEscapeUtils.escapeEcmaScript(lastAirDate) + "\";";
			javascriptVarsScript += "var networks = \"" + StringEscapeUtils.escapeEcmaScript(networks) + "\";";
			javascriptVarsScript += "var numberOfEpisodes = " + numberOfEpisodes + ";";
			javascriptVarsScript += "var numberOfSeasons = \"" + StringEscapeUtils.escapeEcmaScript(numberOfSeasons) + "\";";
			javascriptVarsScript += "var originCountry = \"" + StringEscapeUtils.escapeEcmaScript(originCountry) + "\";";
			javascriptVarsScript += "var originalLanguage = \"" + StringEscapeUtils.escapeEcmaScript(originalLanguage) + "\";";
			javascriptVarsScript += "var originalTitle = \"" + StringEscapeUtils.escapeEcmaScript(originalTitle) + "\";";
			javascriptVarsScript += "var productionCompanies = \"" + StringEscapeUtils.escapeEcmaScript(productionCompanies) + "\";";
			javascriptVarsScript += "var productionCountries = \"" + StringEscapeUtils.escapeEcmaScript(productionCountries) + "\";";
			javascriptVarsScript += "var seasons = \"" + StringEscapeUtils.escapeEcmaScript(seasons) + "\";";
			javascriptVarsScript += "var seriesImages = " + seriesImages + ";";
			javascriptVarsScript += "var seriesType = \"" + StringEscapeUtils.escapeEcmaScript(seriesType) + "\";";
			javascriptVarsScript += "var spokenLanguages = \"" + StringEscapeUtils.escapeEcmaScript(spokenLanguages) + "\";";
			javascriptVarsScript += "var status = \"" + StringEscapeUtils.escapeEcmaScript(status) + "\";";
			javascriptVarsScript += "var tagline = \"" + StringEscapeUtils.escapeEcmaScript(tagline) + "\";";
			javascriptVarsScript += "var tagline = \"" + StringEscapeUtils.escapeEcmaScript(tagline) + "\";";

			javascriptVarsScript += "var actorsTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Actors", language) + "\";";
			String actorsArrayJavaScript = "var actors = [";
			for (String actor : actors) {
				actorsArrayJavaScript += actor + ",";
			}
			actorsArrayJavaScript += "];";
			javascriptVarsScript += actorsArrayJavaScript;

			javascriptVarsScript += "var genresTranslation = \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Genres", language) + "\";";
			String genresArrayJavaScript = "var genres = [";
			for (String genre : genres) {
				genresArrayJavaScript += genre + ",";
			}
			genresArrayJavaScript += "];";
			javascriptVarsScript += genresArrayJavaScript;

			String ratingsArrayJavaScript = "var ratings = [";
			javascriptVarsScript += "var ratingsTranslation= \"" + WebInterfaceServerUtil.getMsgString("VirtualFolder.Ratings", language) + "\";";
			if (!ratings.isEmpty()) {
				Iterator<HashMap<String, String>> ratingsIterator = ratings.iterator();
				while (ratingsIterator.hasNext()) {
					HashMap<String, String> rating = ratingsIterator.next();
					ratingsArrayJavaScript += "{ \"source\": \"" + StringEscapeUtils.escapeEcmaScript(rating.get("source")) + "\", \"value\": \"" + StringEscapeUtils.escapeEcmaScript(rating.get("value")) + "\"},";
				}
			}
			ratingsArrayJavaScript += "];";
			javascriptVarsScript += ratingsArrayJavaScript;
		} catch (Exception e) {
			LOGGER.error("Caught exception in getAPIMetadataAsJavaScriptVars: {}", e.getMessage());
			LOGGER.debug("{}", e);
		}

		return javascriptVarsScript;
	}
}
