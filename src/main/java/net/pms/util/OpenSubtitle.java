/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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

package net.pms.util;

import static net.pms.util.XMLRPCUtil.*;
import static org.apache.commons.lang3.StringUtils.getJaroWinklerDistance;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaLang;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.dlna.VideoClassification;
import net.pms.dlna.protocolinfo.MimeType;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.CredMgr.Credential;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final String SUB_DIR = "subs";
	private static final String UA = "Universal Media Server v1";
	private static final long TOKEN_EXPIRATION_TIME = 10 * 60 * 1000; // 10 minutes

	/** The minimum Jaro–Winkler title distance for IMDB guesses to be valid */
	private static final double MIN_IMDB_GUESS_JW_DISTANCE = 0.65;

	/** The {@link Path} where downloaded OpenSubtitles subtitles are stored */
	public static final Path SUBTITLES_FOLDER = Paths.get(PMS.getConfiguration().getDataFile(SUB_DIR));

	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;

	private static final String OPENSUBS_URL = "http://api.opensubtitles.org/xml-rpc";
	private static final ReentrantReadWriteLock TOKEN_LOCK = new ReentrantReadWriteLock();
	private static Token token = null;

	/**
	 * Gets the <a href=
	 * "http://trac.opensubtitles.org/projects/opensubtitles/wiki/HashSourceCodes"
	 * >OpenSubtitles hash</a> for the specified {@link Path} by first trying to
	 * extract it from the filename and if that doesn't work calculate it with
	 * {@link #computeHash(Path)}.
	 *
	 * @param file the {@link Path} for which to get the hash.
	 * @return The OpenSubtitles hash or {@code null}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static String getHash(Path file) throws IOException {
		String hash = ImdbUtil.extractOSHash(file);
		if (isBlank(hash)) {
			hash = computeHash(file);
		}
		LOGGER.debug("OpenSubtitles hash for \"{}\" is {}", file.getFileName(), hash);
		return hash;
	}

	/**
	 * Calculates the <a href=
	 * "http://trac.opensubtitles.org/projects/opensubtitles/wiki/HashSourceCodes"
	 * >OpenSubtitles hash</a> for the specified {@link Path}.
	 *
	 * @param file the {@link Path} for which to calculate the hash.
	 * @return The calculated OpenSubtitles hash or {@code null}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static String computeHash(Path file) throws IOException {
		if (!Files.isRegularFile(file)) {
			return null;
		}

		long size = Files.size(file);
		long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

		try (FileChannel fileChannel = FileChannel.open(file)) {
			long head = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
			long tail = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));

			return String.format("%016x", size + head + tail);
		}
	}

	private static long computeHashForChunk(ByteBuffer buffer) {
		LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
		long hash = 0;

		while (longBuffer.hasRemaining()) {
			hash += longBuffer.get();
		}

		return hash;
	}

	private static String postPage(URLConnection connection, String query) throws IOException {
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("Content-Length", "" + query.length());
		connection.setConnectTimeout(5000);
		((HttpURLConnection) connection).setRequestMethod("POST");
		//LOGGER.debug("opensub query "+query);
		// open up the output stream of the connection
		if (!StringUtils.isEmpty(query)) {
			try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
				output.writeBytes(query);
				output.flush();
			}
		}

		StringBuilder page;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			page = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null) {
				page.append(str.trim()).append("\n");
			}
		}

		//LOGGER.debug("opensubs result page "+page.toString());
		return page.toString();
	}

	/**
	 * Completes the exchange of the specified {@link HttpURLConnection} and
	 * returns the response as an {@link InputStream}. This also handles the
	 * HTTP response code and throws an {@link OpenSubtitlesException} if the
	 * response isn't OK. In the event of a
	 * {@link HTTPResponseCode#SERVICE_UNAVAILABLE} response code, 3 retries
	 * with a 500 millisecond pause between them will be tried before giving up.
	 *
	 * @param connection the {@link HttpURLConnection} to complete the exchange
	 *            for.
	 * @return The {@link InputStream} with the response.
	 * @throws IOException If an error occurs during the operation.
	 */
	private static InputStream sendXMLStream(HttpURLConnection connection) throws IOException {
		return sendXMLStream(connection, 3, 500);
	}

	/**
	 * Completes the exchange of the specified {@link HttpURLConnection} and
	 * returns the response as an {@link InputStream}. This also handles the
	 * HTTP response code and throws an {@link OpenSubtitlesException} if the
	 * response isn't OK.
	 *
	 * @param connection the {@link HttpURLConnection} to complete the exchange
	 *            for.
	 * @param retries the number of times to retry if the response code is
	 *            {@link HTTPResponseCode#SERVICE_UNAVAILABLE} before giving up.
	 * @param retrySleepMS the number of milliseconds to wait between each
	 *            retry.
	 * @return The {@link InputStream} with the response.
	 * @throws IOException If an error occurs during the operation.
	 */
	private static InputStream sendXMLStream(
		HttpURLConnection connection,
		int retries,
		long retrySleepMS
	) throws IOException {
		int remaining = retries;
		HTTPResponseCode responseCode = HTTPResponseCode.typeOf(connection.getResponseCode());
		do {
			if (responseCode == null) {
				throw new OpenSubtitlesException(
					"OpenSubtitles replied with an unknown response code: " +
					connection.getResponseCode() + " " +
					connection.getResponseMessage()
				);
			}
			if (
				responseCode == HTTPResponseCode.SERVICE_UNAVAILABLE ||
				responseCode == HTTPResponseCode.ORIGIN_ERROR
			) {
				remaining--;
				try {
					Thread.sleep(retrySleepMS);
				} catch (InterruptedException e) {
					LOGGER.debug(
						"OpenSubtitles was interrupted while waiting to retry a request to {}",
						connection.getURL().getHost()
					);
					throw new OpenSubtitlesException("Interrupted while waiting to retry", e);
				}
			} else {
				HTTPResponseCode.handleResponseCode(responseCode);
			}
		} while (
			remaining >= 0 &&
			(
				responseCode == HTTPResponseCode.SERVICE_UNAVAILABLE ||
				responseCode == HTTPResponseCode.ORIGIN_ERROR
			)
		);
		if (
			responseCode == HTTPResponseCode.SERVICE_UNAVAILABLE ||
			responseCode == HTTPResponseCode.ORIGIN_ERROR
		) {
			throw new OpenSubtitlesException(
				"OpenSubtitles gave up getting a response from " +
				connection.getURL().getHost() + " after " +
				retries + " attempts (Response code " + responseCode + ")");
		}
		return connection.getInputStream();
	}

	/**
	 * Logs in to OpenSubtitles and stores the result in {@link #token}. Some
	 * users might get a different API address in response, which will be
	 * reflected in the {@link URL} returned by this method.
	 * <p>
	 * <b>All access to {@link #token} must be protected by {@link #TOKEN_LOCK}</b>.
	 *
	 * @param url The API {@link URL} to use for login.
	 * @return The URL to use if the login was a success, {@code null} otherwise.
	 */
	private static URL login() {
		TOKEN_LOCK.writeLock().lock();
		try {
			if (token != null && token.isYoung()) {
				return token.isValid() ? token.getURL() : null;
			}
			LOGGER.debug("Trying to log in to OpenSubtitles");

			Credential credentials = PMS.getCred("opensubtitles");
			String pword = "";
			String username = "";
			if (credentials != null) {
				// if we got credentials use them
				if (isNotBlank(credentials.password)) {
					pword = DigestUtils.md5Hex(credentials.password);
				}
				username = credentials.username;
			}

			// Setup connection
			URL url;
			try {
				url = new URL(OPENSUBS_URL);
			} catch (MalformedURLException e) {
				throw new AssertionError("OpenSubtitles URL \"" + OPENSUBS_URL + "\" is invalid");
			}

			URLConnection urlConnection = url.openConnection();
			if (!(urlConnection instanceof HttpURLConnection)) {
				throw new OpenSubtitlesException("Invalid URL: " + OPENSUBS_URL);
			}
			HttpURLConnection connection = (HttpURLConnection) urlConnection;
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(2000);

			// Create request
			Params params = new Params();
			params.add(new ValueString(username));
			params.add(new ValueString(pword));
			params.add(new ValueString(null));
			params.add(new ValueString(UA));

			// Send request
			try (OutputStream out = LOGGER.isTraceEnabled() ?
				new LoggableOutputStream(connection.getOutputStream(), StandardCharsets.UTF_8) :
				connection.getOutputStream()
			) {
				XMLStreamWriter writer = createWriter(out);
				writeMethod(writer, "LogIn", params);
				writer.flush();
				if (out instanceof LoggableOutputStream) {
					LOGGER.trace("Sending OpenSubtitles login request:\n{}", toLogString((LoggableOutputStream) out));
				}
			} catch (XMLStreamException | FactoryConfigurationError e) {
				LOGGER.error("An error occurred while generating OpenSubtitles login request: {}", e.getMessage());
				LOGGER.trace("", e);
			}

			// Parse reply
			params = null;
			try (InputStream reply = LOGGER.isTraceEnabled() ?
				new LoggableInputStream(sendXMLStream(connection, 5, 500), StandardCharsets.UTF_8) :
				sendXMLStream(connection, 5, 500)
			) {
				LOGGER.trace("Parsing OpenSubtitles login response");
				XMLStreamReader reader = null;
				try {
					reader = XMLRPCUtil.createReader(reply);
					params = readMethodResponse(reader);
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
				if (reply instanceof LoggableInputStream) {
					LOGGER.trace("Received OpenSubtitles login response:\n{}", toLogString((LoggableInputStream) reply));
				}
			}

			if (params == null) {
				LOGGER.error("Failed to parse Opensubtitles login response, aborting");
				token = Token.createInvalidToken();
				return null;
			}
			if (
				params.size() != 1 ||
				!(params.get(0).getValue() instanceof Struct)
			) {
				LOGGER.error("Unexpected reply from OpenSubtitles:\n{}", params);
				token = Token.createInvalidToken();
				return null;
			}

			// Handle status code
			if (!checkStatus(params)) {
				token = Token.createInvalidToken();
				LOGGER.error("OpenSubtitles login was aborted");
				return null;
			}

			Struct members = (Struct) params.get(0).getValue();

			// Check token
			String tokenString;
			Member<?, ?> member = members.get("token");
			if (!(member instanceof MemberString) || isBlank(((MemberString) member).getValue())) {
				LOGGER.error("Failed to parse OpenSubtitles login token: {}", member);
				token = Token.createInvalidToken();
				return null;
			}
			tokenString = ((MemberString) member).getValue();

			// Parse user
			User tokenUser = null;
			member = members.get("data");
			if (member != null) {
				tokenUser = User.createFromStruct((Struct) member.getValue());
			}

			// Create Token
			token = new Token(tokenString, tokenUser, url);
			if (!token.isValid()) {
				LOGGER.error("Failed to log in to OpenSubtitles");
				return null;
			}
			if (LOGGER.isDebugEnabled()) {
				if (token.getUser() != null) {
					//XXX If log anonymization is ever implemented, hide the nickname.
					LOGGER.debug("Successfully logged in to OpenSubtitles as {}", token.getUser().getUserNickName());
				} else {
					LOGGER.debug("Successfully logged in to OpenSubtitles anonymously");
				}
			}
			return token.getURL();
		} catch (XMLStreamException | IOException e) {
			LOGGER.error("An error occurred during OpenSubtitles login: {}", e.getMessage());
			LOGGER.trace("", e);
			token = Token.createInvalidToken();
			return null;
		} finally {
			TOKEN_LOCK.writeLock().unlock();
		}
	}

	private static boolean checkStatus(Params params) {
		if (
			params == null ||
			params.isEmpty() ||
			!(params.get(0).getValue() instanceof Struct) ||
			((Struct) params.get(0).getValue()).get("status") == null
		) {
			LOGGER.error("OpenSubtitles response has no status, aborting");
			return false;
		}

		Member<?, ?> status = ((Struct) params.get(0).getValue()).get("status");
		if (status.getValue() == null) {
			LOGGER.error("OpenSubtitles response has no status, aborting");
			return false;
		}

		StatusCode statusCode = StatusCode.typeOf(status.getValue().toString());
		try {
			StatusCode.handleStatusCode(statusCode);
		} catch (OpenSubtitlesException e) {
			LOGGER.error("OpenSubtitles replied with an error, aborting: {}", statusCode);
			LOGGER.trace("", e);
			return false;
		}

		return true;
	}

	private static ArrayList<SubtitleItem> parseSubtitles(
		Array dataArray,
		FileNamePrettifier prettifier,
		DLNAMediaInfo media
	) throws OpenSubtitlesException {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (dataArray == null) {
			return result;
		}

		for (Value<?> value : dataArray) {
			if (!(value.getValue() instanceof Struct)) {
				throw new OpenSubtitlesException("Unexpected data in OpenSubtitles response array: " + value);
			}
			Struct struct = (Struct) value.getValue();
			SubtitleItem item = SubtitleItem.createFromStruct(struct, prettifier, media);
			if (item != null) {
				result.add(item);
			}
		}
		return result;
	}

	private static Map<String, MovieGuess> parseMovieGuesses(Struct dataStruct) throws OpenSubtitlesException {
		if (dataStruct == null || dataStruct.isEmpty()) {
			return null;
		}
		Map<String, MovieGuess> result = new HashMap<>();
		for (Member<?, ?> member : dataStruct.values()) {
			if (!(member.getValue() instanceof Struct)) {
				throw new OpenSubtitlesException("Received an unexpected MovieGuess entry from OpenSubtitles: " + member);
			}
			result.put(member.getName(), MovieGuess.createFromStruct(((Struct) member.getValue())));
		}
		return result;
	}

	private static Map<String, List<CheckMovieHashItem>> parseCheckMovieHash(Struct dataStruct) throws OpenSubtitlesException {
		HashMap<String, List<CheckMovieHashItem>> result = new HashMap<>();
		if (dataStruct == null) {
			return result;
		}

		for (Entry<String, Member<? extends Value<?>, ?>> entry : dataStruct.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			if (!(entry.getValue().getValue() instanceof Array)) {
				throw new OpenSubtitlesException("Unexpected data in CheckMovieHash(2) response: " + entry);
			}
			Array array = (Array) entry.getValue().getValue();
			if (array.isEmpty()) {
				continue;
			}
			ArrayList<CheckMovieHashItem> items = new ArrayList<>();
			for (Value<?> value : array) {
				if (value == null || !(value.getValue() instanceof Struct)) {
					continue;
				}
				Struct struct = (Struct) value.getValue();
				if (struct.isEmpty()) {
					continue;
				}
				CheckMovieHashItem item = new CheckMovieHashItem(
					Member.getString(struct, "MovieKind"),
					Member.getInt(struct, "SubCount"),
					Member.getInt(struct, "SeenCount"),
					Member.getString(struct, "MovieImdbID"),
					Member.getString(struct, "MovieYear"),
					Member.getString(struct, "MovieHash"),
					Member.getInt(struct, "SeriesEpisode"),
					Member.getString(struct, "MovieName"),
					Member.getInt(struct, "SeriesSeason")
				);
				items.add(item);
			}
			if (!items.isEmpty()) {
				result.put(entry.getKey(), items);
			}
		}
		return result;
	}

	/**
	 * Tries to find relevant OpenSubtitles subtitles for the specified
	 * {@link DLNAResource} for the specified renderer.
	 *
	 * @param resource the {@link DLNAResource} for which to find OpenSubtitles
	 *            subtitles.
	 * @param renderer the {@link RendererConfiguration} or {@code null}.
	 * @return The {@link List} of found {@link SubtitleItem}. If none are
	 *         found, an empty {@link List} is returned.
	 */
	public static ArrayList<SubtitleItem> findSubtitles(DLNAResource resource, RendererConfiguration renderer) {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (resource == null) {
			return new ArrayList<>();
		}
		URL url = login();
		if (url == null) {
			LOGGER.error(
				"Couldn't find any live subtitles for {} since OpenSubtitles login failed",
				resource.getName()
			);
			return new ArrayList<>();
		}

		String languageCodes = getLanguageCodes(renderer);
		String primaryLanguageCode = getPrimaryLanguageCode(languageCodes);
		String imdbId = null;
		FileNamePrettifier prettifier = new FileNamePrettifier(resource);
		boolean satisfactory = false;
		if (resource instanceof RealFile) {
			Path file = ((RealFile) resource).getFile().toPath();
			LOGGER.info("Looking for OpenSubtitles subtitles for \"{}\"", file);

			// Query by hash
			long fileSize;
			try {
				fileSize = Files.size(file);
			} catch (IOException e) {
				LOGGER.error(
					"Can't read the size of \"{}\", please check that it exists and that read permission is granted",
					file.toAbsolutePath()
				);
				LOGGER.trace("", e);
				fileSize = 0L;
			}
			String fileHash;
			try {
				fileHash = getHash(file);
			} catch (IOException e) {
				LOGGER.error("Couldn't calculate OpenSubtitles hash for \"{}\": {}", file.getFileName(), e.getMessage());
				LOGGER.trace("", e);
				fileHash = null;
			}

			if (isNotBlank(fileHash) && fileSize > 0L) {
				result.addAll(findSubtitlesByFileHash(resource, fileHash, fileSize, languageCodes, prettifier));
				satisfactory = isSubtitlesSatisfactory(result, primaryLanguageCode);
			}

			if (!satisfactory && isBlank(imdbId)) {
				imdbId = ImdbUtil.extractImdbId(file, true);
				if (isBlank(imdbId)) {
					imdbId = findImdbIdByFileHash(resource, fileHash, fileSize, prettifier);
				}
			}
		}

		if (!satisfactory) {
			if (isBlank(imdbId)) {
				imdbId = guessImdbIdByFileName(resource, prettifier);
			}
			if (isNotBlank(imdbId)) {
				// Query by IMDB id
				result.addAll(findSubtitlesByImdbId(resource, imdbId, languageCodes, prettifier));
				satisfactory = isSubtitlesSatisfactory(result, primaryLanguageCode);
			}
		}

		if (!satisfactory) {
			// Query by name
			result.addAll(findSubtitlesByName(resource, languageCodes, prettifier));
		}

		if (result.size() > 0) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
					"Found {} OpenSubtitles subtitles ({}) for \"{}\":\n{}",
					result.size(),
					satisfactory ? "satisfied" : "unsatisfied",
					resource.getName(),
					toLogString(result, 2)
				);
			} else {
				LOGGER.info(
					"Found {} OpenSubtitles subtitles for \"{}\"",
					result.size(),
					resource.getName()
				);
			}
		} else {
			LOGGER.info("Couldn't find any OpenSubtitles subtitles for \"{}\"", resource.getName());
		}

		return result;
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * hash and size.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are to be
	 *            searched.
	 * @param fileHash the file hash.
	 * @param fileSize the file size in bytes.
	 * @param languageCodes the comma separated list of subtitle language codes.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return A {@link List} with the found {@link SubtitleItem}s (might be
	 *         empty).
	 */
	protected static ArrayList<SubtitleItem> findSubtitlesByFileHash(
		DLNAResource resource,
		String fileHash,
		long fileSize,
		String languageCodes,
		FileNamePrettifier prettifier
	) {
		if (resource == null || isBlank(fileHash)) {
			return new ArrayList<>();
		}

		Struct queryStruct = new Struct();
		queryStruct.put(new MemberString("moviehash", fileHash));
		queryStruct.put(new MemberString("moviebytesize", Long.toString(fileSize)));
		if (isNotBlank(languageCodes)) {
			queryStruct.put(new MemberString("sublanguageid", languageCodes));
		}
		if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
			queryStruct.put(new MemberInt("season", prettifier.getSeason()));
			queryStruct.put(new MemberInt("episode", prettifier.getEpisode()));
		}
		Array queryArray = new Array();
		queryArray.add(new ValueStruct(queryStruct));

		return searchSubtitles(queryArray, resource, prettifier, "file hash", fileHash, -1);
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * IMDB ID.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are to be
	 *            searched.
	 * @param imdbId the IMDB ID.
	 * @param languageCodes the comma separated list of subtitle language codes.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return A {@link List} with the found {@link SubtitleItem}s (might be
	 *         empty).
	 */
	protected static ArrayList<SubtitleItem> findSubtitlesByImdbId(
		DLNAResource resource,
		String imdbId,
		String languageCodes,
		FileNamePrettifier prettifier
	) {
		if (resource == null || isBlank(imdbId)) {
			return new ArrayList<>();
		}

		Struct queryStruct = new Struct();
		queryStruct.put(new MemberString("imdbid", imdbId));
		if (isNotBlank(languageCodes)) {
			queryStruct.put(new MemberString("sublanguageid", languageCodes));
		}
		if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
			queryStruct.put(new MemberInt("season", prettifier.getSeason()));
			queryStruct.put(new MemberInt("episode", prettifier.getEpisode()));
		}
		Array queryArray = new Array();
		queryArray.add(new ValueStruct(queryStruct));

		return searchSubtitles(queryArray, resource, prettifier, "IMDB ID", imdbId, -1);
	}

	/**
	 * Queries OpenSubtitles for subtitles using the specified query
	 * {@link Array}.
	 *
	 * @param queryArray the {@link Array} containing the query to send.
	 * @param resource the {@link DLNAResource} for which subtitles are searched
	 *            for.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @param logDescription a {@link String} describing the type of search,
	 *            i.e. {@code "file hash"} or {@code "IMDB ID"}.
	 * @param logSearchTerm a {@link String} with the "main" search term, for
	 *            example the file hash, the IMDB ID or the filename.
	 * @param limit the maximum number of returned {@link SubtitleItem}s or
	 *            {@code -1} for no limit.
	 * @return A {@link List} with the found {@link SubtitleItem}s (might be
	 *         empty).
	 */
	protected static ArrayList<SubtitleItem> searchSubtitles(
		Array queryArray,
		DLNAResource resource,
		FileNamePrettifier prettifier,
		String logDescription,
		String logSearchTerm,
		int limit
	) {
		if (resource == null || queryArray == null || queryArray.isEmpty()) {
			return new ArrayList<>();
		}
		URL url = login();
		if (url == null) {
			return new ArrayList<>();
		}

		URLConnection urlConnection;
		try {
			urlConnection = url.openConnection();
			if (!(urlConnection instanceof HttpURLConnection)) {
				throw new OpenSubtitlesException("Invalid URL: " + OPENSUBS_URL);
			}

			HttpURLConnection connection = (HttpURLConnection) urlConnection;
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(3000);

			// Create request
			Params params = new Params();
			TOKEN_LOCK.readLock().lock();
			try {
				if (token.isValid()) {
					params.add(new ValueString(token.getValue()));
				}
			} finally {
				TOKEN_LOCK.readLock().unlock();
			}
			params.add(new ValueArray(queryArray));
			if (limit > 0) {
				Struct struct = new Struct();
				struct.put(new MemberInt("limit", 2));
				params.add(new ValueStruct(struct));
			}

			// Send request
			try (OutputStream out = LOGGER.isTraceEnabled() ?
				new LoggableOutputStream(connection.getOutputStream(), StandardCharsets.UTF_8) :
				connection.getOutputStream()
			) {
				XMLStreamWriter writer = createWriter(out);
				writeMethod(writer, "SearchSubtitles", params);
				writer.flush();
				if (out instanceof LoggableOutputStream) {
					LOGGER.trace(
						"Querying OpenSubtitles for subtitles for \"{}\" using {}:\n{}",
						resource.getName(),
						logDescription,
						toLogString((LoggableOutputStream) out));
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
						"Querying OpenSubtitles for subtitles for \"{}\" using {} \"{}\"",
						resource.getName(),
						logDescription,
						logSearchTerm
					);
				}
			} catch (XMLStreamException | FactoryConfigurationError e) {
				LOGGER.error(
					"An error occurred while generating OpenSubtitles search by {} request: {}",
					logDescription,
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			// Parse reply
			params = null;
			try (InputStream reply = LOGGER.isTraceEnabled() ?
				new LoggableInputStream(sendXMLStream(connection), StandardCharsets.UTF_8) :
				sendXMLStream(connection)
			) {
				LOGGER.trace("Parsing OpenSubtitles search by {} response", logDescription);
				XMLStreamReader reader = null;
				try {
					reader = XMLRPCUtil.createReader(reply);
					params = readMethodResponse(reader);
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
				if (reply instanceof LoggableInputStream) {
					LOGGER.trace(
						"Received OpenSubtitles search by {} response:\n{}",
						logDescription,
						toLogString((LoggableInputStream) reply)
					);
				}
			}

			// Handle status code
			if (!checkStatus(params)) {
				LOGGER.error(
					"OpenSubtitles search using {} \"{}\" was aborted because of an error",
					logDescription,
					logSearchTerm
				);
				return new ArrayList<>();
			}

			// Parse subtitles
			Member<?, ?> dataMember = ((Struct) params.get(0).getValue()).get("data");
			if (
				dataMember == null ||
				!(dataMember.getValue() instanceof Array)
			) {
				// No data
				return new ArrayList<>();
			}
			ArrayList<SubtitleItem> results = parseSubtitles((Array) dataMember.getValue(), prettifier, resource.getMedia());

			if (LOGGER.isDebugEnabled()) {
				if (results.isEmpty()) {
					LOGGER.debug(
						"OpenSubtitles search for subtitles for \"{}\" using {} \"{}\" gave no results",
						resource.getName(),
						logDescription,
						logSearchTerm
					);
				} else if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Found {} OpenSubtitles subtitles for \"{}\" using {} \"{}\":\n{}",
						results.size(),
						resource.getName(),
						logDescription,
						logSearchTerm,
						toLogString(results, 2)
					);
				} else {
					LOGGER.debug(
						"Found {} OpenSubtitles subtitles for \"{}\" using {} \"{}\"",
						results.size(),
						resource.getName(),
						logDescription,
						logSearchTerm
					);
				}
			}
			return results;
		} catch (XMLStreamException | IOException e) {
			LOGGER.error("An error occurred while performing OpenSubtitles search by {}: {}", logDescription, e.getMessage());
			LOGGER.trace("", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Attempts to find an {@code IMDB ID} corresponding to a video file using
	 * {@code SearchSubtitles} and {@code CheckMovieHash2} queries.
	 *
	 * @param resource the {@link DLNAResource} whose IMDB ID to find.
	 * @param fileHash the file hash for the video file.
	 * @param fileSize the file size for the video file.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return The {@code IMDB ID} if one can be determined, {@code null}
	 *         otherwise.
	 */
	protected static String findImdbIdByFileHash(
		DLNAResource resource,
		String fileHash,
		long fileSize,
		FileNamePrettifier prettifier
	) {
		if (resource == null || isBlank(fileHash)) {
			return null;
		}
		LOGGER.trace(
			"Querying OpenSubtitles for IMDB ID for \"{}\" using file hash \"{}\".",
			resource.getName(),
			fileHash
		);
		String result;

		// Try first using searchSubtitles because we can use file size for better accuracy
		Struct queryStruct = new Struct();
		queryStruct.put(new MemberString("moviehash", fileHash));
		queryStruct.put(new MemberString("moviebytesize", Long.toString(fileSize)));
		if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
			queryStruct.put(new MemberInt("season", prettifier.getSeason()));
			queryStruct.put(new MemberInt("episode", prettifier.getEpisode()));
		}
		Array queryArray = new Array();
		queryArray.add(new ValueStruct(queryStruct));

		List<SubtitleItem> subtitles = searchSubtitles(queryArray, resource, prettifier, "file hash", fileHash, 1);
		if (!subtitles.isEmpty()) {
			result = subtitles.get(0).getIdMovieImdb();
			if (isNotBlank(result)) {
				LOGGER.debug(
					"OpenSubtitles SearchSubtitles returned IMDB ID {} for \"{}\" using file hash \"{}\"",
					result,
					resource.getName(),
					fileHash
				);
				return result;
			}
		}

		// Use the less accurate checkMovieHash2 if no subtitles are registered for the file hash
		Map<String, List<CheckMovieHashItem>> results = checkMovieHash2(fileHash);
		List<CheckMovieHashItem> items = results.isEmpty() ? null : results.get(fileHash);
		if (items == null || items.isEmpty()) {
			LOGGER.debug(
				"OpenSubtitles CheckMovieHash2 returned no IMDB ID for \"{}\" using file hash \"{}\"",
				resource.getName(),
				fileHash
			);
			return null;
		}
		if (prettifier == null) {
			LOGGER.warn(
				"OpenSubtitles CheckMovieHash returned {} result{} which can't be verified because the prettifier is null",
				items.size(),
				items.size() > 1 ? "s" : ""
			);
			return null;
		}
		ArrayList<GuessCandidate> candidates = new ArrayList<>();
		addGuesses(candidates, items, prettifier, prettifier.getClassification(), PMS.getLocale());
		if (candidates.isEmpty()) {
			LOGGER.debug(
				"OpenSubtitles CheckMovieHash2 returned no usable IMDB ID for \"{}\" using file hash \"{}\"",
				resource.getName(),
				fileHash
			);
			return null;
		}
		if (candidates.size() == 1) {
			result = candidates.get(0).getGuessItem().getImdbId();
			LOGGER.debug(
				"OpenSubtitles CheckMovieHash2 returned IMDB ID {} for \"{}\" using file hash \"{}\"",
				result,
				resource.getName(),
				fileHash
			);
			return result;
		}
		Collections.sort(candidates);
		if (LOGGER.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (GuessCandidate candidate : candidates) {
				sb.append("  ").append(candidate).append("\n");
			}
			LOGGER.trace(
				"OpenSubtitles: findImdbIdByHash() candidates for \"{}\":\n{}",
				resource.getName(),
				sb.toString()
			);
		}
		result = candidates.get(0).getGuessItem().getImdbId();
		LOGGER.debug(
			"OpenSubtitles: Picked IMDB ID {} as the best match for \"{}\" using file hash \"{}\" with CheckMovieHash2",
			result,
			resource.getName(),
			fileHash
		);

		return result;
	}

	/**
	 * Queries OpenSubtitles for titles matching the specified file hashes.
	 *
	 * @param fileHashes the file hashes to look up.
	 * @return A {@link Map} of the matching [file hash, {@link List} of
	 *         {@link CheckMovieHashItem}s] pairs. If nothing is found, an empty
	 *         {@link Map} is returned.
	 */
	protected static Map<String, List<CheckMovieHashItem>> checkMovieHash2(String... fileHashes) {
		if (fileHashes == null || fileHashes.length == 0) {
			return new HashMap<>();
		}
		URL url = login();
		if (url == null) {
			return new HashMap<>();
		}

		URLConnection urlConnection;
		try {
			urlConnection = url.openConnection();
			if (!(urlConnection instanceof HttpURLConnection)) {
				throw new OpenSubtitlesException("Invalid URL: " + OPENSUBS_URL);
			}

			HttpURLConnection connection = (HttpURLConnection) urlConnection;
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(3000);

			// Create request
			Params params = new Params();
			TOKEN_LOCK.readLock().lock();
			try {
				if (token.isValid()) {
					params.add(new ValueString(token.getValue()));
				}
			} finally {
				TOKEN_LOCK.readLock().unlock();
			}
			Array queryArray = new Array();
			for (String fileHash : fileHashes) {
				queryArray.add(new ValueString(fileHash));
			}
			params.add(new ValueArray(queryArray));

			// Send request
			try (OutputStream out = LOGGER.isTraceEnabled() ?
				new LoggableOutputStream(connection.getOutputStream(), StandardCharsets.UTF_8) :
				connection.getOutputStream()
			) {
				XMLStreamWriter writer = createWriter(out);
				writeMethod(writer, "CheckMovieHash2", params);
				writer.flush();
				if (out instanceof LoggableOutputStream) {
					LOGGER.trace(
						"Querying OpenSubtitles for titles using file hash{}:\n{}",
						fileHashes.length > 1 ? "es" : "",
						toLogString((LoggableOutputStream) out)
					);
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
						"Querying OpenSubtitles for titles with file hash{} {}",
						fileHashes.length > 1 ? "es" : "",
						StringUtil.createReadableCombinedString(fileHashes, true)
					);
				}
			} catch (XMLStreamException | FactoryConfigurationError e) {
				LOGGER.error(
					"An error occurred while generating OpenSubtitles CheckMovieHash2 request for {}: {}",
					StringUtil.createReadableCombinedString(fileHashes, true),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			// Parse reply
			params = null;
			try (InputStream reply = LOGGER.isTraceEnabled() ?
				new LoggableInputStream(sendXMLStream(connection), StandardCharsets.UTF_8) :
				sendXMLStream(connection)
			) {
				LOGGER.trace("Parsing OpenSubtitles CheckMovieHash2 response");
				XMLStreamReader reader = null;
				try {
					reader = XMLRPCUtil.createReader(reply);
					params = readMethodResponse(reader);
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
				if (reply instanceof LoggableInputStream) {
					LOGGER.trace(
						"Received OpenSubtitles CheckMovieHash2 response:\n{}",
						toLogString((LoggableInputStream) reply)
					);
				}
			}

			// Handle status code
			if (!checkStatus(params)) {
				LOGGER.error("OpenSubtitles CheckMovieHash2 was aborted because of an error");
				return new HashMap<>();
			}

			// Parse subtitles
			Member<?, ?> dataMember = ((Struct) params.get(0).getValue()).get("data");
			if (
				dataMember == null ||
				!(dataMember.getValue() instanceof Struct)
			) {
				// No data
				return new HashMap<>();
			}
			Map<String, List<CheckMovieHashItem>> results = parseCheckMovieHash((Struct) dataMember.getValue());

			if (LOGGER.isDebugEnabled()) {
				if (results.isEmpty()) {
					LOGGER.debug(
						"OpenSubtitles CheckMovieHash2 for {} gave no results",
						StringUtil.createReadableCombinedString(fileHashes, true)
					);
				} else {
					int num = 0;
					for (List<CheckMovieHashItem> items : results.values()) {
						if (items != null) {
							num += items.size();
						}
					}
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace(
							"Found {} OpenSubtitles titles for {} using CheckMovieHash2:\n{}",
							num,
							StringUtil.createReadableCombinedString(fileHashes, true),
							toLogString(results, 2)
						);
					} else {
						LOGGER.debug(
							"Found {} OpenSubtitles titles for {} using CheckMovieHash2",
							num,
							StringUtil.createReadableCombinedString(fileHashes, true)
						);
					}
				}
			}
			return results;
		} catch (XMLStreamException | IOException e) {
			LOGGER.error(
				"An error occurred during OpenSubtitles CheckMovieHash2 for {}: {}",
				StringUtil.createReadableCombinedString(fileHashes, true),
				e.getMessage()
			);
			LOGGER.trace("", e);
			return new HashMap<>();
		}
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * name.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are to be
	 *            searched.
	 * @param languageCodes the comma separated list of subtitle language codes.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return A {@link List} with the found {@link SubtitleItem}s (might be
	 *         empty).
	 */
	protected static ArrayList<SubtitleItem> findSubtitlesByName(
		DLNAResource resource,
		String languageCodes,
		FileNamePrettifier prettifier
	) {
		if (resource == null) {
			return new ArrayList<>();
		}
		String fileName = null;
		if (resource instanceof RealFile) {
			File file = ((RealFile) resource).getFile();
			if (file != null) {
				fileName = file.getName();
			}
		}
		if (fileName == null) {
			fileName = resource.getSystemName();
		}

		Array queryArray = new Array();
		if (isNotBlank(fileName)) {
			Struct queryStruct = new Struct();
			queryStruct.put(new MemberString("tag", fileName));
			if (isNotBlank(languageCodes)) {
				queryStruct.put(new MemberString("sublanguageid", languageCodes));
			}
			if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
				queryStruct.put(new MemberInt("season", prettifier.getSeason()));
				queryStruct.put(new MemberInt("episode", prettifier.getEpisode()));
			}
			queryArray.add(new ValueStruct(queryStruct));
		}
		if (prettifier != null && isNotBlank(prettifier.getName())) {
			Struct queryStruct = new Struct();
			queryStruct.put(new MemberString("query", prettifier.getName()));
			if (isNotBlank(languageCodes)) {
				queryStruct.put(new MemberString("sublanguageid", languageCodes));
			}
			if (prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
				queryStruct.put(new MemberInt("season", prettifier.getSeason()));
				queryStruct.put(new MemberInt("episode", prettifier.getEpisode()));
			}
			queryArray.add(new ValueStruct(queryStruct));
		}

		return searchSubtitles(queryArray, resource, prettifier, "filename", fileName, -1);
	}

	private static void addGuesses(
		List<GuessCandidate> candidates,
		Collection<? extends GuessItem> guesses,
		FileNamePrettifier prettifier,
		VideoClassification classification,
		Locale locale
	) {
		// The score calculation isn't extensively tested and might need to be tweaked
		for (GuessItem guess : guesses) { //TODO: (Nad) Guess Scoring
			double score = 0.0;
			if (isBlank(prettifier.getName()) || isBlank(guess.getTitle())) {
				continue;
			}
			score += getJaroWinklerDistance(
				prettifier.getName().toLowerCase(locale),
				guess.getTitle().toLowerCase(locale)
			);
			if (score < MIN_IMDB_GUESS_JW_DISTANCE) { //TODO: (Nad) Parameterize threshold...?
				LOGGER.trace(
					"OpenSubtitles: Excluding IMDB guess because the similarity ({}) is under the threshold ({}): {}",
					score,
					MIN_IMDB_GUESS_JW_DISTANCE,
					guess
				);
				continue;
			}
			if (prettifier.getYear() > 0) {
				int guessYear = StringUtil.getYear(guess.getYear());
				if (prettifier.getYear() == guessYear) {
					score += 0.4;
				}
			}
			if (classification != null && classification == guess.getVideoClassification()) {
				score += 0.5;
				if (classification == VideoClassification.SERIES && guess instanceof CheckMovieHashItem) {
					CheckMovieHashItem item = (CheckMovieHashItem) guess;
					if (
						item.getSeriesSeason() == prettifier.getSeason() &&
						item.getSeriesEpisode() == prettifier.getEpisode()
					) {
						score += 0.3;
					}
				}
			}
			if (guess instanceof BestGuess) {
				score += 0.2;
			}
			candidates.add(new GuessCandidate(score, guess));
		}
	}

	/**
	 * Queries OpenSubtitles for IMDB IDs matching a filename.
	 *
	 * @param resource the {@link DLNAResource} for which to find the IMDB ID.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return The IMDB ID or {@code null}.
	 */
	public static String guessImdbIdByFileName(
		DLNAResource resource,
		FileNamePrettifier prettifier
	) {
		return guessImdbIdByFileName(resource, null, prettifier);
	}

	/**
	 * Queries OpenSubtitles for IMDB IDs matching a filename.
	 *
	 * @param fileName the file name for which to find the IMDB ID.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return The IMDB ID or {@code null}.
	 */
	public static String guessImdbIdByFileName(
		String fileName,
		FileNamePrettifier prettifier
	) {
		return guessImdbIdByFileName(null, fileName, prettifier);
	}

	/**
	 * Queries OpenSubtitles for IMDB IDs matching a filename. Specify
	 * <i>either</i> {@code resource} <i>or</i> {@code fileName}. If both are
	 * specified, only {@code resource} is used.
	 *
	 * @param resource the {@link DLNAResource} for which to find the IMDB ID or
	 *            {@code null}.
	 * @param fileName the file name for which to find the IMDB ID or
	 *            {@code null}.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return The IMDB ID or {@code null}.
	 */
	protected static String guessImdbIdByFileName(
		DLNAResource resource,
		String fileName,
		FileNamePrettifier prettifier
	) {
		if (resource == null && isBlank(fileName)) {
			return null;
		}
		if (resource != null) {
			if (resource instanceof RealFile) {
				File file = ((RealFile) resource).getFile();
				if (file == null) {
					return null;
				}
				fileName = file.getName();
			} else {
				fileName = resource.getSystemName();
			}
			if (isBlank(fileName)) {
				return null;
			}
		}

		URL url = login();
		if (url == null) {
			LOGGER.error(
				"Couldn't guess IMDB ID for {} since OpenSubtitles login failed",
				resource == null ? fileName : resource.getName()
			);
			return null;
		}

		URLConnection urlConnection;
		try {
			urlConnection = url.openConnection();
			if (!(urlConnection instanceof HttpURLConnection)) {
				throw new OpenSubtitlesException("Invalid URL: " + OPENSUBS_URL);
			}

			HttpURLConnection connection = (HttpURLConnection) urlConnection;
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(3000);

			// Create request
			Params params = new Params();
			TOKEN_LOCK.readLock().lock();
			try {
				if (token.isValid()) {
					params.add(new ValueString(token.getValue()));
				}
			} finally {
				TOKEN_LOCK.readLock().unlock();
			}
			Array array = new Array();
			array.add(new ValueString(fileName));
			params.add(new ValueArray(array));

			// Send request
			try (OutputStream out = LOGGER.isTraceEnabled() ?
				new LoggableOutputStream(connection.getOutputStream(), StandardCharsets.UTF_8) :
				connection.getOutputStream()
			) {
				XMLStreamWriter writer = createWriter(out);
				writeMethod(writer, "GuessMovieFromString", params);
				writer.flush();

				if (out instanceof LoggableOutputStream) {
					LOGGER.trace(
						"Querying OpenSubtitles for IMDB ID for \"{}\":\n{}",
						fileName,
						toLogString((LoggableOutputStream) out));
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
						"Querying OpenSubtitles for IMDB ID for \"{}\"",
						fileName
					);
				}
			} catch (XMLStreamException | FactoryConfigurationError e) {
				LOGGER.error(
					"An error occurred while generating OpenSubtitles GuessMovieFromString request: {}",
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			// Parse reply
			params = null;
			try (InputStream reply = LOGGER.isTraceEnabled() ?
				new LoggableInputStream(sendXMLStream(connection), StandardCharsets.UTF_8) :
				sendXMLStream(connection)
			) {
				LOGGER.trace("Parsing OpenSubtitles GuessMovieFromString response");
				XMLStreamReader reader = null;
				try {
					reader = XMLRPCUtil.createReader(reply);
					params = readMethodResponse(reader);
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
				if (reply instanceof LoggableInputStream) {
					LOGGER.trace(
						"Received OpenSubtitles GuessMovieFromString response:\n{}",
						toLogString((LoggableInputStream) reply)
					);
				}
			}

			// Handle status code
			if (!checkStatus(params)) {
				return null;
			}

			// Parse suggestions
			Member<?, ?> dataMember = ((Struct) params.get(0).getValue()).get("data");
			if (
				dataMember == null ||
				!(dataMember.getValue() instanceof Struct)
			) {
				// No data
				return null;
			}

			Map<String, MovieGuess> guesses = parseMovieGuesses((Struct) dataMember.getValue());
			if (guesses == null) {
				return null;
			}

			MovieGuess movieGuess = guesses.get(fileName);
			if (movieGuess != null) {
				VideoClassification classification;
				if (
					movieGuess.getGuessIt() != null &&
					movieGuess.getGuessIt().getType() != null &&
					movieGuess.getGuessIt().getType() != prettifier.getClassification()
				) {
					classification = movieGuess.getGuessIt().getType();
					LOGGER.debug(
						"OpenSubtitles guessed that \"{}\" is a {} while we guessed a {}. Using {}",
						fileName,
						movieGuess.getGuessIt().getType(),
						prettifier.getClassification(),
						classification
					);
				} else {
					classification = prettifier.getClassification();
				}

				Locale locale = PMS.getLocale();
				ArrayList<GuessCandidate> candidates = new ArrayList<>();
				if (movieGuess.getGuessesFromString().size() > 0) {
					addGuesses(candidates, movieGuess.getGuessesFromString().values(), prettifier, classification, locale);
				}
				if (movieGuess.getImdbSuggestions().size() > 0) {
					addGuesses(candidates, movieGuess.getImdbSuggestions().values(), prettifier, classification, locale);
				}
				if (movieGuess.getBestGuess() != null) {
					addGuesses(candidates, Collections.singletonList(movieGuess.getBestGuess()), prettifier, classification, locale);
				}
				if (candidates.size() > 0) {
					Collections.sort(candidates);
					if (LOGGER.isTraceEnabled()) {
						StringBuilder sb = new StringBuilder();
						for (GuessCandidate candidate : candidates) {
							sb.append("  ").append(candidate).append("\n");
						}
						LOGGER.trace(
							"OpenSubtitles: guessImdbIdByFileName() candidates for \"{}\":\n{}",
							resource == null ? fileName : resource.getName(),
							sb.toString()
						);
					}
					LOGGER.debug(
						"OpenSubtitles: guessImdbIdByFileName() picked {} as the most likely OpenSubtitles candidate for \"{}\"",
						candidates.get(0).getGuessItem(),
						resource == null ? fileName : resource.getName()
					);
					return candidates.get(0).getGuessItem().getImdbId();
				}
			}

			LOGGER.debug(
				"OpenSubtitles: guessImdbIdByFileName() failed to find a candidate for \"{}\"",
				resource == null ? fileName : resource.getName()
			);
			return null;
		} catch (XMLStreamException | IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles GuessMovieFromString query results for \"{}\": {}",
				resource == null ? fileName : resource.getName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
			return null;
		}
	}

	/**
	 * Creates a {@link String} from the content of the specified
	 * {@link LoggableInputStream}. If the content isn't to long, the content is
	 * formatted as "prettified" XML.
	 *
	 * @param loggable the {@link LoggableInputStream} to use.
	 * @return The log friendly {@link String}.
	 */
	public static String toLogString(LoggableInputStream loggable) {
		if (loggable == null) {
			return "null";
		}
		String loggableString = loggable.toString();
		if (loggableString.length() > 300000) {
			return "Not logging huge XML document with a length of " + loggableString.length() + " characters";
		}
		if (loggableString.length() > 20000) {
			return
				"Not prettifying XML document with a length of " +
				loggableString.length() +
				", unprettified XML document: " +
				loggableString;
		}
		try {
			return StringUtil.prettifyXML(loggableString, StandardCharsets.UTF_8, 2);
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
			LOGGER.error("Failed to prettify XML reply {}", e.getMessage());
			LOGGER.trace("", e);
			return "Unable to pretty print XML document: " + e.getMessage() + "\nUnprettified XML document: " + loggableString;
		}
	}

	/**
	 * Creates a {@link String} from the content of the specified
	 * {@link LoggableOutputStream}. If the content isn't to long, the content
	 * is formatted as "prettified" XML.
	 *
	 * @param loggable the {@link LoggableOutputStream} to use.
	 * @return The log friendly {@link String}.
	 */
	public static String toLogString(LoggableOutputStream loggable) {
		if (loggable == null) {
			return "null";
		}
		String loggableString = loggable.toString();
		if (loggableString.length() > 100000) {
			return "Not logging huge XML document with a length of " + loggableString.length() + " characters";
		}
		if (loggableString.length() > 10000) {
			return "Unprettified XML document: " + loggableString;
		}
		try {
			return StringUtil.prettifyXML(loggableString, StandardCharsets.UTF_8, 2);
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
			LOGGER.error("Failed to prettify XML reply {}", e.getMessage());
			LOGGER.trace("", e);
			return "Unable to pretty print XML document: " + e.getMessage() + "\nUnprettified XML document: " + loggableString;
		}
	}

	/**
	 * Creates a {@link String} where each {@link SubtitleItem} in
	 * {@code subtitleItems} is on its own line and indented with the specified
	 * number of spaces.
	 *
	 * @param subtitleItems the {@link Collection} of {@link SubtitleItem}s to
	 *            format for logging.
	 * @param indent the number of leading spaces on each line.
	 * @return The log friendly {@link String}.
	 */
	public static String toLogString(Collection<SubtitleItem> subtitleItems, int indent) {
		String indentation = indent > 0 ? StringUtil.fillString(' ', indent) : "";
		if (subtitleItems == null) {
			return indentation + "Null";
		}
		if (subtitleItems.isEmpty()) {
			return indentation + "No matching subtitles";
		}
		StringBuilder sb = new StringBuilder();
		for (SubtitleItem item : subtitleItems) {
			sb.append(indentation).append(item).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Creates a {@link String} where each file hash in {@code titles} is on its
	 * own line and indented with the specified number of spaces. The
	 * {@link CheckMovieHashItem}s for each file hash is listed as sub-items
	 * with double indentation.
	 *
	 * @param titles the {@link Map} of [file hash, {@link CheckMovieHashItem}]
	 *            pairs to format for logging.
	 * @param indent the number of leading spaces for "one indentation".
	 * @return The log friendly {@link String}.
	 */
	public static String toLogString(Map<String, List<CheckMovieHashItem>> titles, int indent) {
		String indentation = indent > 0 ? StringUtil.fillString(' ', indent) : "";
		if (titles == null) {
			return indentation + "Null";
		}
		if (titles.isEmpty()) {
			return indentation + "No matching titles";
		}
		StringBuilder sb = new StringBuilder();
		for (Entry<String, List<CheckMovieHashItem>> entry : titles.entrySet()) {
			sb.append(indentation).append(entry.getKey()).append(":\n");
			sb.append(toLogStringTitles(entry.getValue(), 2 * indent));
		}
		return sb.toString();
	}

	/**
	 * Creates a {@link String} where each {@link CheckMovieHashItem} in
	 * {@code titles} is on its own line and indented with the specified number
	 * of spaces.
	 *
	 * @param titles the {@link Collection} of {@link CheckMovieHashItem}s to
	 *            format for logging.
	 * @param indent the number of leading spaces on each line.
	 * @return The log friendly {@link String}.
	 */
	public static String toLogStringTitles(Collection<CheckMovieHashItem> titles, int indent) {
		String indentation = indent > 0 ? StringUtil.fillString(' ', indent) : "";
		if (titles == null) {
			return indentation + "Null";
		}
		if (titles.isEmpty()) {
			return indentation + "No matching titles";
		}
		StringBuilder sb = new StringBuilder();
		for (CheckMovieHashItem item : titles) {
			sb.append(indentation).append(item).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Evaluates whether the found set of subtitles are satisfactory or if more
	 * searches should be performed.
	 *
	 * @param subtitleItems the currently found {@link SubtitleItem}s.
	 * @param primaryLanguageCode the primary language code.
	 * @return {@code true} if the list of subtitles are considered good enough,
	 *         {@code false} otherwise.
	 */
	protected static boolean isSubtitlesSatisfactory(List<SubtitleItem> subtitleItems, String primaryLanguageCode) {
		if (subtitleItems == null || subtitleItems.isEmpty()) {
			return false;
		}
		if (isBlank(primaryLanguageCode)) {
			return true;
		}
		String languageCode = primaryLanguageCode.trim().toLowerCase(Locale.ROOT);
		for (SubtitleItem item : subtitleItems) {
			String itemLangaugeCode = item.getLanguageCode();
			if (
				isNotBlank(itemLangaugeCode) &&
				languageCode.equals(itemLangaugeCode.trim().toLowerCase(Locale.ROOT))
			) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates the ISO 639-2 (3 letter) language code query string for the
	 * configured subtitle languages.
	 *
	 * @param renderer the {@link RendererConfiguration} for which to the
	 *            generate language codes query.
	 * @return The comma separated list of ISO 639-2 codes or {@code null}.
	 */
	public static String getLanguageCodes(RendererConfiguration renderer) {
		String languages = UMSUtils.getLangList(renderer, false);
		if (isBlank(languages)) {
			return null;
		}
		ArrayList<String> languagesList = new ArrayList<>();
		String[] languagesArray = languages.trim().split("\\s*,\\s*");
		for (String language : languagesArray) {
			if (isNotBlank(language)) {
				String iso6392 = Iso639.getISO639_2Code(language);
				if (isNotBlank(iso6392) && !DLNAMediaLang.UND.equals(iso6392)) {
					languagesList.add(iso6392);
				}
			}
		}
		if (languagesList.size() == 0) {
			return null;
		}
		return StringUtils.join(languagesList, ',');
	}

	/**
	 * Extracts the first language code from a comma separated list of language
	 * codes.
	 *
	 * @param languageCodes The comma separated list of language codes.
	 * @return The primary language code or {@code null}.
	 */
	public static String getPrimaryLanguageCode(String languageCodes) {
		if (isBlank(languageCodes)) {
			return null;
		}
		int firstComma = languageCodes.indexOf(",");
		if (firstComma > 0) {
			return languageCodes.substring(0, firstComma);
		}
		return null;
	}

	/**
	 * Feeds the correct parameters for getInfo below.
	 *
	 * @param file the {@link File} to lookup.
	 * @param formattedName the name to use in the name search
	 * @return The parameter {@link String}.
	 * @throws IOException If an I/O error occurs during the operation.
	 *
	 * @see #getInfo(java.lang.String, long, java.lang.String, java.lang.String)
	 */
	public static String[] getInfo(File file, String formattedName) throws IOException {
		return getInfo(file, formattedName, null);
	}

	public static String[] getInfo(File file, String formattedName, RendererConfiguration r) throws IOException {
		Path path = file.toPath();
		String[] res = getInfo(getHash(path), file.length(), null, null, r);
		if (res == null || res.length == 0) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdbId(path, false);
			if (StringUtil.hasValue(imdb)) {
				res = getInfo(null, 0, imdb, null, r);
			}
		}

		if (res == null || res.length == 0) { // final try, use the name
			if (StringUtils.isNotEmpty(formattedName)) {
				res = getInfo(null, 0, null, formattedName, r);
			} else {
				res = getInfo(null, 0, null, file.getName(), r);
			}
		}

		return res;
	}

	/**
	 * Attempt to return information from OpenSubtitles about the file based
	 * on information from the filename; either the hash, the IMDb ID or the
	 * filename itself.
	 *
	 * @param hash  the video hash
	 * @param size  the byte-size to be used with the hash
	 * @param imdb  the IMDb ID
	 * @param query the string to search OpenSubtitles for
	 * @param renderer the renderer to get subtitle languages from
	 *
	 * @return a string array including the IMDb ID, episode title, season
	 *         number, episode number relative to the season, and the show
	 *         name, or {@code null} if we couldn't find it on OpenSubtitles.
	 *
	 * @throws IOException
	 */
	private static String[] getInfo(String hash, long size, String imdb, String query, RendererConfiguration r) throws IOException {
		URL url = login();
		if (url == null) {
			return null;
		}
		String lang = getLanguageCodes(r);

		String hashStr = "";
		String imdbStr = "";
		String qStr = "";
		if (!StringUtils.isEmpty(hash)) {
			hashStr =
				"<member>" +
					"<name>moviehash</name>" +
					"<value>" +
						"<string>" + hash + "</string>" +
					"</value>" +
				"</member>\n" +
				"<member>" +
					"<name>moviebytesize</name>" +
					"<value>" +
						"<double>" + size + "</double>" +
					"</value>" +
				"</member>\n";
		} else if (!StringUtils.isEmpty(imdb)) {
			imdbStr =
				"<member>" +
					"<name>imdbid</name>" +
					"<value>" +
						"<string>" + imdb + "</string>" +
					"</value>" +
				"</member>\n";
		} else if (!StringUtils.isEmpty(query)) {
			qStr =
				"<member>" +
					"<name>query</name>" +
					"<value>" +
						"<string>" + query + "</string>" +
					"</value>" +
				"</member>\n";
		} else {
			return null;
		}

		String req = null;
		TOKEN_LOCK.readLock().lock();
		try {
			req =
				"<methodCall>\n" +
					"<methodName>SearchSubtitles</methodName>\n" +
					"<params>\n" +
						"<param>\n" +
							"<value>" +
								"<string>" + token + "</string>" +
							"</value>\n" +
						"</param>\n" +
						"<param>\n" +
							"<value>\n" +
								"<array>\n" +
									"<data>\n" +
										"<value>" +
											"<struct>" +
												"<member>" +
													"<name>sublanguageid</name>" +
													"<value>" +
														"<string>" + lang + "</string>" +
													"</value>" +
												"</member>" +
												hashStr +
												imdbStr +
												qStr + "\n" +
											"</struct>" +
										"</value>" +
									"</data>\n" +
								"</array>\n" +
							"</value>\n" +
						"</param>" +
					"</params>\n" +
				"</methodCall>\n";
		} finally {
			TOKEN_LOCK.readLock().unlock();
		}
		Pattern re = Pattern.compile(
			".*IDMovieImdb</name>.*?<string>([^<]+)</string>.*?" +
			"MovieName</name>.*?<string>([^<]+)</string>.*?" +
			"SeriesSeason</name>.*?<string>([^<]+)</string>.*?" +
			"SeriesEpisode</name>.*?<string>([^<]+)</string>.*?" +
			"MovieYear</name>.*?<string>([^<]+)</string>.*?",
			Pattern.DOTALL
		);
		String page = postPage(url.openConnection(), req);

		// LOGGER.trace("opensubs page: " + page);

		Matcher m = re.matcher(page);
		if (m.find()) {
			LOGGER.debug("match {},{},{},{},{}", m.group(1), m.group(2), m.group(3), m.group(4), m.group(5));
			Pattern re1 = Pattern.compile("&#34;([^&]+)&#34;(.*)");
			String name = m.group(2);
			Matcher m1 = re1.matcher(name);
			String episodeName = "";
			if (m1.find()) {
				episodeName = m1.group(2).trim();
				name = m1.group(1).trim();
			}

			String imdbId = ImdbUtil.ensureTT(m.group(1).trim());

			/**
			 * Sometimes if OpenSubtitles doesn't have an episode title they call it
			 * something like "Episode #1.4", so discard that.
			 */
			episodeName = StringEscapeUtils.unescapeHtml4(episodeName);
			if (episodeName.startsWith("Episode #")) {
				episodeName = "";
			}

			return new String[]{
				imdbId,
				episodeName,
				StringEscapeUtils.unescapeHtml4(name),
				m.group(4).trim(), // Season number
				m.group(5).trim(), // Episode number
				m.group(3).trim()  // Year
			};
		}
		return null;
	}

	/**
	 * Resolves the location and full path of the subtitles file with the
	 * specified name.
	 *
	 * @param fileName the file name of the subtitles file.
	 * @return The resulting {@link Path}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static Path resolveSubtitlesPath(String fileName) throws IOException {
		if (isBlank(fileName)) {
			return null;
		}
		if (!Files.exists(SUBTITLES_FOLDER)) {
			Files.createDirectories(SUBTITLES_FOLDER);
		}
		return SUBTITLES_FOLDER.resolve(fileName).toAbsolutePath();
	}

	/**
	 * Downloads the subtitles from the specified {@link URI} to the specified
	 * {@link Path}. It the specified {@link Path} is {@code null} a temporary
	 * filename is used.
	 *
	 * @param url the {@link URL} from which to download.
	 * @param output the {@link Path} for the target file.
	 * @return {@code null} if {@code url} is {@code null} or OpenSubtitles
	 *         login fails, otherwise the {@link Path} to the downloaded file.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Path fetchSubs(URL url, Path output) throws IOException {
		if (url == null || login() == null) {
			return null;
		}
		if (output == null) {
			output = resolveSubtitlesPath("TempSub" + String.valueOf(System.currentTimeMillis()));
		}
		URLConnection connection = url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		InputStream in = connection.getInputStream();
		try (
			GZIPInputStream gzipInputStream = new GZIPInputStream(in);
			OutputStream out = Files.newOutputStream(output);
		) {
			byte[] buf = new byte[4096];
			int len;
			while ((len = gzipInputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		}
		return output.toAbsolutePath();
	}

	/**
	 * Adds a field to the specified {@link StringBuilder} using the
	 * specified parameters. This is a convenience method for building
	 * {@link #toString()} values.
	 *
	 * @param first {@code true} if this is the first field and shouldn't be
	 *            prefixed with "{@code , }", {@code false} otherwise.
	 * @param sb the {@link StringBuilder} to add to.
	 * @param fieldName the name of the field.
	 * @param value the value of the field.
	 * @param quote {@code true} if the field's value should be quoted in
	 *            double quotes, {@code false} otherwise.
	 * @param addBlank {@code true} it the field should be added even if
	 *            {@code value} is {@code null} or if
	 *            {@code value.toString()} is blank, {@code false}
	 *            otherwise.
	 * @param addZero {@code true} if the field should be added if
	 *            {@code value.toString()} is "{@code 0}" or if
	 *            {@code value} implements {@link Number} and its numerical
	 *            value is zero, {@code false} otherwise.
	 * @return {@code true} if the field was added to the
	 *         {@link StringBuilder}, {@code false} otherwise.
	 */
	protected static boolean addFieldToStringBuilder(
		boolean first,
		StringBuilder sb,
		String fieldName,
		Object value,
		boolean quote,
		boolean addBlank,
		boolean addZero
	) {
		if (!addBlank && (value == null || isBlank(value.toString()))) {
			return false;
		}
		if (!addZero && ("0".equals(value.toString()) || value instanceof Number && ((Number) value).intValue() == 0)) {
			return false;
		}

		if (!first) {
			sb.append(", ");
		}
		if (isNotBlank(fieldName)) {
			sb.append(fieldName).append("=");
		}
		if (value == null) {
			sb.append("Null");
			return true;
		}
		if (quote) {
			sb.append("\"").append(value).append("\"");
			return true;
		}
		sb.append(value);
		return true;
	}

	/**
	 * A class representing an OpenSubtitles token.
	 *
	 * @author Nadahar
	 */
	public static class Token {
		private final String value;
		private final long tokenBirth;
		private final User user;
		private final URL defaultUrl;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param value the token value {@link String}.
		 * @param user the {@link User} or {@code null}.
		 * @param url the standard API {@link URL}.
		 */
		public Token(String value, User user, URL url) {
			this.tokenBirth = System.currentTimeMillis();
			this.value = value;
			this.user = user;
			this.defaultUrl = url;
		}

		/**
		 * @return The token {@link String} value.
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @return The {@link Token} age in milliseconds.
		 */
		public long getTokenAge() {
			return System.currentTimeMillis() - tokenBirth;
		}

		/**
		 * @return {@code true} if this {@link Token} is young enough to be
		 *         reused, {@code false} if a new should be obtained.
		 */
		public boolean isYoung() {
			return System.currentTimeMillis() - tokenBirth < TOKEN_EXPIRATION_TIME;
		}

		/**
		 * Evaluates whether this {@link Token} is valid, that is whether it has
		 * a token value, a default {@link URL} and the age isn't more than a
		 * minute past the expiration time.
		 * <p>
		 * The reason for accepting a minute past the expiration time is: The
		 * expiration time is checked during {@link OpenSubtitle#login()} and a
		 * new {@link Token} is requested if it has expired. The expiration
		 * limit isn't defined by OpenSubtitle, it is set shorter than the
		 * actual expiration to be on the safe side. If the {@link Token}
		 * expires after the call to {@link OpenSubtitle#login()} it won't be
		 * renewed until the next time {@link OpenSubtitle#login()} is called.
		 * It is thus possible that a {@link Token} can be in use for a short
		 * while after "expiration". A minute is thus added to allow a
		 * {@link Token} to be used for a while after "expiration" so that
		 * ongoing operations can be completed.
		 *
		 * @return {@code true} if this {@link Token} is considered valid,
		 *         {@code false} otherwise.
		 */
		public boolean isValid() {
			return
				isNotBlank(value) &&
				defaultUrl != null &&
				System.currentTimeMillis() - tokenBirth < TOKEN_EXPIRATION_TIME + 60000;
		}

		/**
		 * @return The {@link User} if credentials were used to log in,
		 *         {@code null} otherwise.
		 */
		public User getUser() {
			return user;
		}

		/**
		 * @return {@code true} if credentials were used to log in,
		 *         {@code false} otherwise.
		 */
		public boolean isUser() {
			return user != null;
		}

		/**
		 * Some OpenSubtitle users like VIPs get a different API address with
		 * better priority. If such an URL was returned during login, it will be
		 * returned {@link URL}. If not, the login {@link URL} will be returned.
		 *
		 * @return The {@link User}'s {@link URL} or {@code null}.
		 */
		public URL getURL() {
			if (user != null && user.getContentLocation() != null) {
				try {
					return user.getContentLocation().toURL();
				} catch (MalformedURLException e) {
					LOGGER.error("OpenSubtitles: Not using user specified API URL: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			}
			return defaultUrl;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append(" [value=").append(value);
			sb.append(", Expired=").append(isYoung() ? "No" : "Yes");
			if (user != null) {
				sb.append(", User=").append(user.toString(false));
			} else {
				sb.append(", Anonymous");
			}
			sb.append("]");
			return sb.toString();
		}

		/**
		 * Creates an invalid {@link Token} indicating that login has failed.
		 *
		 * @return The new invalid {@link Token};
		 */
		public static Token createInvalidToken() {
			return new Token(null, null, null);
		}
	}

	/**
	 * A class holding information about an OpenSubtitles user.
	 *
	 * @author Nadahar
	 */
	public static class User {
		private final String idUser;
		private final String userNickName;
		private final String userRank;
		private final String[] userPreferredLanguages;
		private final boolean isVIP;
		private final URI contentLocation;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param idUser the id.
		 * @param userNickName the nickname.
		 * @param userRank the rank.
		 * @param userPreferredLanguages the comma separated preferred
		 *            languages.
		 * @param isVIP {@code true} if the user is a VIP, {@code false}
		 *            otherwise.
		 * @param contentLocation the user's API {@link URL} or {@code null}.
		 */
		public User(
			String idUser,
			String userNickName,
			String userRank,
			String userPreferredLanguages,
			boolean isVIP,
			URI contentLocation
		) {
			this.idUser = idUser;
			this.userNickName = userNickName;
			this.userRank = userRank;
			if (isNotBlank(userPreferredLanguages)) {
				this.userPreferredLanguages = userPreferredLanguages.trim().split("\\s*,\\s*");
			} else {
				this.userPreferredLanguages = new String[0];
			}
			this.isVIP = isVIP;
			this.contentLocation = contentLocation;
		}

		/**
		 * @return {@code IDUser}.
		 */
		public String getIdUser() {
			return idUser;
		}

		/**
		 * @return {@code UserNickName}.
		 */
		public String getUserNickName() {
			return userNickName;
		}

		/**
		 * @return {@code UserRank}.
		 */
		public String getUserRank() {
			return userRank;
		}

		/**
		 * @return A new array containing the language codes from
		 *         {@code UserPreferredLanguages}.
		 */
		public String[] getUserPreferredLanguages() {
			String[] result = new String[userPreferredLanguages.length];
			System.arraycopy(userPreferredLanguages, 0, result, 0, result.length);
			return result;
		}

		/**
		 * @return {@code IsVIP}.
		 */
		public boolean isVIP() {
			return isVIP;
		}

		/**
		 * @return {@code Content-Location}.
		 */
		public URI getContentLocation() {
			return contentLocation;
		}

		@Override
		public String toString() {
			return toString(true);
		}

		/**
		 * Returns a {@link String} representation of this instance.
		 *
		 * @param includeName {@code true} if the name of this {@link Class}
		 *            should be included, {@code false} otherwise.
		 * @return The {@link String} representation.
		 */
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "IDUser", idUser, false, true, true);
			first &= !addFieldToStringBuilder(first, sb, "UserNickName", userNickName, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "UserRank", userRank, true, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IsVIP", Boolean.valueOf(isVIP), false, false, true);
			if (userPreferredLanguages.length > 0) {
				first &= !addFieldToStringBuilder(
					first,
					sb,
					"UserPreferredLanguages",
					StringUtils.join(userPreferredLanguages, ", "),
					true,
					false,
					true
				);
			}
			addFieldToStringBuilder(first, sb, "Content-Location", contentLocation, true, false, true);
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((contentLocation == null) ? 0 : contentLocation.hashCode());
			result = prime * result + ((idUser == null) ? 0 : idUser.hashCode());
			result = prime * result + (isVIP ? 1231 : 1237);
			result = prime * result + ((userNickName == null) ? 0 : userNickName.hashCode());
			result = prime * result + Arrays.hashCode(userPreferredLanguages);
			result = prime * result + ((userRank == null) ? 0 : userRank.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof User)) {
				return false;
			}
			User other = (User) obj;
			if (contentLocation == null) {
				if (other.contentLocation != null) {
					return false;
				}
			} else if (!contentLocation.equals(other.contentLocation)) {
				return false;
			}
			if (idUser == null) {
				if (other.idUser != null) {
					return false;
				}
			} else if (!idUser.equals(other.idUser)) {
				return false;
			}
			if (isVIP != other.isVIP) {
				return false;
			}
			if (userNickName == null) {
				if (other.userNickName != null) {
					return false;
				}
			} else if (!userNickName.equals(other.userNickName)) {
				return false;
			}
			if (!Arrays.equals(userPreferredLanguages, other.userPreferredLanguages)) {
				return false;
			}
			if (userRank == null) {
				if (other.userRank != null) {
					return false;
				}
			} else if (!userRank.equals(other.userRank)) {
				return false;
			}
			return true;
		}

		/**
		 * Creates a new {@link User} from the specified {@code LogIn} data
		 * structure.
		 *
		 * @param dataStruct the {@link Struct} containing the {@code LogIn}
		 *            user data structure.
		 * @return The resulting {@link User}.
		 */
		public static User createFromStruct(Struct dataStruct) {
			if (dataStruct == null) {
				throw new IllegalArgumentException("struct cannot be null");
			}

			Member<? extends Value<?>, ?> userId = dataStruct.get("IDUser");
			Member<? extends Value<?>, ?> nick = dataStruct.get("UserNickName");
			Member<? extends Value<?>, ?> rank = dataStruct.get("UserRank");
			Member<? extends Value<?>, ?> preferences = dataStruct.get("UserPreferedLanguages");
			Member<? extends Value<?>, ?> vip = dataStruct.get("isVIP");
			boolean isVIP =
				vip != null &&
				vip.getValue() != null &&
				!"0".equals(vip.getValue().toString()) &&
				!"false".equals(vip.getValue().toString().toLowerCase(Locale.ROOT));
			Member<? extends Value<?>, ?> contentLocation = dataStruct.get("Content-Location");

			String urlString = contentLocation == null || contentLocation.getValue() == null ?
				null :
				contentLocation.getValue().toString();
			URI url = null;
			if (urlString != null && isNotBlank(urlString)) {
				try {
					urlString = urlString.replaceFirst("https://", "http://");
					url = new URI(urlString);
				} catch (URISyntaxException e) {
					LOGGER.debug("OpenSubtitles: Ignoring invalid URL \"{}\": {}", urlString, e.getMessage());
					LOGGER.trace("", e);
				}
			}

			return new User(
				userId == null || userId.getValue() == null ? null : userId.getValue().toString(),
				nick == null || nick.getValue() == null ? null : nick.getValue().toString(),
				rank == null || rank.getValue() == null ? null : rank.getValue().toString(),
				preferences == null || preferences.getValue() == null ? null : preferences.getValue().toString(),
				isVIP,
				url
			);
		}
	}

	/**
	 * A class representing an OpenSubtitles guess item.
	 */
	public static class GuessItem {

		/** The movie name/title */
		protected final String title;

		/** The release year */
		protected final String year;

		/** The {@link VideoClassification} */
		protected final VideoClassification videoClassification;

		/** The IMDB ID */
		protected final String imdbId;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the {@link VideoClassification}.
		 * @param imdbId the IMDB ID.
		 */
		public GuessItem(
			String movieName,
			String movieYear,
			VideoClassification videoClassification,
			String imdbId
		) {
			this.title = movieName;
			this.year = movieYear;
			this.videoClassification = videoClassification;
			this.imdbId = imdbId;
		}

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the video classification.
		 * @param imdbId the IMDB ID.
		 */
		public GuessItem(String movieName, String movieYear, String videoClassification, String imdbId) {
			this.title = movieName;
			this.year = movieYear;
			this.videoClassification = VideoClassification.typeOf(videoClassification);
			this.imdbId = imdbId;
		}

		/**
		 * @return The title.
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * @return The release year.
		 */
		public String getYear() {
			return year;
		}

		/**
		 * @return The {@link VideoClassification}.
		 */
		public VideoClassification getVideoClassification() {
			return videoClassification;
		}

		/**
		 * @return The IMDB ID.
		 */
		public String getImdbId() {
			return imdbId;
		}

		/**
		 * Returns a {@link String} representation of this instance.
		 *
		 * @param includeName {@code true} if the name of this {@link Class}
		 *            should be included, {@code false} otherwise.
		 * @return The {@link String} representation.
		 */
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "MovieName", title, true, true, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieKind", videoClassification, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieYear", year, false, false, true);
			addFieldToStringBuilder(first, sb, "IDMovieIMDB", imdbId, false, false, true);
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((imdbId == null) ? 0 : imdbId.hashCode());
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			result = prime * result + ((videoClassification == null) ? 0 : videoClassification.hashCode());
			result = prime * result + ((year == null) ? 0 : year.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof GuessItem)) {
				return false;
			}
			GuessItem other = (GuessItem) obj;
			if (imdbId == null) {
				if (other.imdbId != null) {
					return false;
				}
			} else if (!imdbId.equals(other.imdbId)) {
				return false;
			}
			if (title == null) {
				if (other.title != null) {
					return false;
				}
			} else if (!title.equals(other.title)) {
				return false;
			}
			if (videoClassification != other.videoClassification) {
				return false;
			}
			if (year == null) {
				if (other.year != null) {
					return false;
				}
			} else if (!year.equals(other.year)) {
				return false;
			}
			return true;
		}

		/**
		 * Creates a new {@link Map} of [IMDB ID, {@link GuessItem}] pairs from
		 * the {@code GetIMDBSuggest} part of an OpenSubtitles
		 * {@code GuessMovieFromString} result.
		 *
		 * @param member the {@link Member} containing the
		 *            {@code GetIMDBSuggest} data structure.
		 * @return The resulting {@link GuessItem}s or an empty {@link Map}.
		 */
		public static Map<String, GuessItem> createFromStruct(Member<?, ?> member) {
			Map<String, GuessItem> result = new HashMap<>();
			if (member == null) {
				return result;
			}
			if (member.getValue() instanceof Struct) {
				Struct struct = (Struct) member.getValue();
				if (struct.isEmpty()) {
					return result;
				}
				for (Member<?, ?> guessMember : struct.values()) {
					if (guessMember == null || !(guessMember.getValue() instanceof Struct)) {
						continue;
					}
					Struct guessStruct = (Struct) guessMember.getValue();
					result.put(guessMember.getName(), new GuessItem(
						Member.getString(guessStruct, "MovieName"),
						Member.getString(guessStruct, "MovieYear"),
						Member.getString(guessStruct, "MovieKind"),
						Member.getString(guessStruct, "IDMovieIMDB")
					));
				}
			} else if (member.getValue() instanceof Array) {
				Array array = (Array) member.getValue();
				if (array.isEmpty()) {
					return result;
				}
				for (Value<?> value : array) {
					if (value == null || !(value.getValue() instanceof Struct)) {
						continue;
					}
					Struct guessStruct = (Struct) value.getValue();
					String imdbId = Member.getString(guessStruct, "IDMovieIMDB");
					result.put(imdbId, new GuessItem(
						Member.getString(guessStruct, "MovieName"),
						Member.getString(guessStruct, "MovieYear"),
						Member.getString(guessStruct, "MovieKind"),
						imdbId
					));
				}
			}
			return result;
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code GuessFromString}
	 * structure.
	 */
	public static class GuessFromString extends GuessItem {
		private final int score;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the video classification.
		 * @param imdbId the IMDB ID.
		 * @param score the score.
		 */
		public GuessFromString(
			String movieName,
			String movieYear,
			String videoClassification,
			String imdbId,
			String score
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			int tmpScore;
			try {
				tmpScore = Integer.parseInt(score);
			} catch (NumberFormatException e) {
				tmpScore = -1;
			}
			this.score = tmpScore;
		}

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the {@link VideoClassification}.
		 * @param imdbId the IMDB ID.
		 * @param score the score.
		 */
		public GuessFromString(
			String movieName,
			String movieYear,
			VideoClassification videoClassification,
			String imdbId,
			int score
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			this.score = score;
		}

		/**
		 * @return The score.
		 */
		public int getScore() {
			return score;
		}

		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "MovieName", title, true, true, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieKind", videoClassification, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieYear", year, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IDMovieIMDB", imdbId, false, false, true);
			addFieldToStringBuilder(first, sb, "score", Integer.valueOf(score), false, false, false);
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + score;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof GuessFromString)) {
				return false;
			}
			GuessFromString other = (GuessFromString) obj;
			if (score != other.score) {
				return false;
			}
			return true;
		}

		/**
		 * Creates a new {@link Map} of [IMDB ID, {@link GuessFromString}] pairs
		 * from {@code GuessMovieFromString} part of an OpenSubtitles
		 * {@code GuessMovieFromString} result.
		 *
		 * @param member the {@link Member} containing the
		 *            {@code GuessMovieFromString} data structure.
		 * @return The resulting {@link GuessFromString}s or an empty
		 *         {@link Map}.
		 */
		public static Map<String, GuessFromString> createGuessesFromStringFromMember(Member<?, ?> member) {
			Map<String, GuessFromString> result = new HashMap<>();
			if (member == null) {
				return result;
			}
			if (member.getValue() instanceof Struct) {
				Struct struct = (Struct) member.getValue();
				if (struct.isEmpty()) {
					return result;
				}
				for (Member<?, ?> guessMember : struct.values()) {
					if (guessMember == null || !(guessMember.getValue() instanceof Struct)) {
						continue;
					}
					Struct guessStruct = (Struct) guessMember.getValue();
					result.put(member.getName(), new GuessFromString(
						Member.getString(guessStruct, "MovieName"),
						Member.getString(guessStruct, "MovieYear"),
						Member.getString(guessStruct, "MovieKind"),
						Member.getString(guessStruct, "IDMovieIMDB"),
						Member.getString(guessStruct, "score")
					));
				}
			} else if (member.getValue() instanceof Array) {
				Array array = (Array) member.getValue();
				if (array.isEmpty()) {
					return result;
				}
				for (Value<?> value : array) {
					if (value == null || !(value.getValue() instanceof Struct)) {
						continue;
					}
					Struct guessStruct = (Struct) value.getValue();
					String imdbId = Member.getString(guessStruct, "IDMovieIMDB");
					result.put(imdbId, new GuessFromString(
						Member.getString(guessStruct, "MovieName"),
						Member.getString(guessStruct, "MovieYear"),
						Member.getString(guessStruct, "MovieKind"),
						Member.getString(guessStruct, "IDMovieIMDB"),
						imdbId
					));
				}
			}
			return result;
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code GuessIt} structure.
	 */
	public static class GuessIt {
		private final MimeType mimeType;
		private final String videoCodec;
		private final String container;
		private final String title;
		private final String originalFormat;
		private final String releaseGroup;
		private final String videoResolutionFormat;
		private final VideoClassification type;
		private final String audioCodec;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param mimeType the MIME-type.
		 * @param videoCodec the video codec.
		 * @param container the container type.
		 * @param title the title
		 * @param originalFormat the original medium/format.
		 * @param releaseGroup the release group.
		 * @param videoResolutionFormat the video resolution format.
		 * @param type the type.
		 * @param audioCodec the audio codec.
		 */
		public GuessIt(
			String mimeType,
			String videoCodec,
			String container,
			String title,
			String originalFormat,
			String releaseGroup,
			String videoResolutionFormat,
			String type,
			String audioCodec
		) {
			if (isBlank(mimeType)) {
				this.mimeType = null;
			} else {
				MimeType tmpMimeType;
				try {
					tmpMimeType = MimeType.valueOf(mimeType);
				} catch (ParseException e) {
					tmpMimeType = null;
				}
				this.mimeType = tmpMimeType;
			}
			this.videoCodec = videoCodec;
			this.container = container;
			this.title = title;
			this.originalFormat = originalFormat;
			this.releaseGroup = releaseGroup;
			this.videoResolutionFormat = videoResolutionFormat;
			this.type = VideoClassification.typeOf(type);
			this.audioCodec = audioCodec;
		}

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param mimeType the {@link MimeType}.
		 * @param videoCodec the video codec.
		 * @param container the container type.
		 * @param title the title
		 * @param originalFormat the original medium/format.
		 * @param releaseGroup the release group.
		 * @param videoResolutionFormat the video resolution format.
		 * @param type the {@link VideoClassification}.
		 * @param audioCodec the audio codec.
		 */
		public GuessIt(
			MimeType mimeType,
			String videoCodec,
			String container,
			String title,
			String originalFormat,
			String releaseGroup,
			String videoResolutionFormat,
			VideoClassification type,
			String audioCodec
		) {
			this.mimeType = mimeType;
			this.videoCodec = videoCodec;
			this.container = container;
			this.title = title;
			this.originalFormat = originalFormat;
			this.releaseGroup = releaseGroup;
			this.videoResolutionFormat = videoResolutionFormat;
			this.type = type;
			this.audioCodec = audioCodec;
		}

		/**
		 * @return the {@link MimeType}.
		 */
		public MimeType getMimeType() {
			return mimeType;
		}

		/**
		 * @return The video codec.
		 */
		public String getVideoCodec() {
			return videoCodec;
		}

		/**
		 * @return The container format.
		 */
		public String getContainer() {
			return container;
		}

		/**
		 * @return The title.
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * @return The original format (BluRay, WebDL etc.).
		 */
		public String getOriginalFormat() {
			return originalFormat;
		}

		/**
		 * @return The release group name.
		 */
		public String getReleaseGroup() {
			return releaseGroup;
		}

		/**
		 * @return The video resolution format (720p, 1080i etc.).
		 */
		public String getVideoResolutionFormat() {
			return videoResolutionFormat;
		}

		/**
		 * @return The {@link VideoClassification}.
		 */
		public VideoClassification getType() {
			return type;
		}

		/**
		 * @return The audio codec.
		 */
		public String getAudioCodec() {
			return audioCodec;
		}

		/**
		 * Returns a {@link String} representation of this instance.
		 *
		 * @param includeName {@code true} if the name of this {@link Class}
		 *            should be included, {@code false} otherwise.
		 * @return The {@link String} representation.
		 */
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "title", title, true, true, true);
			first &= !addFieldToStringBuilder(first, sb, "type", type, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "container", container, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "mimetype", mimeType, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "videoCodec", videoCodec, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "audio", audioCodec, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "format", originalFormat, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "screenSize", videoResolutionFormat, false, false, true);
			addFieldToStringBuilder(first, sb, "releaseGroup", releaseGroup, false, false, false);
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((audioCodec == null) ? 0 : audioCodec.hashCode());
			result = prime * result + ((container == null) ? 0 : container.hashCode());
			result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
			result = prime * result + ((originalFormat == null) ? 0 : originalFormat.hashCode());
			result = prime * result + ((releaseGroup == null) ? 0 : releaseGroup.hashCode());
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((videoCodec == null) ? 0 : videoCodec.hashCode());
			result = prime * result + ((videoResolutionFormat == null) ? 0 : videoResolutionFormat.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof GuessIt)) {
				return false;
			}
			GuessIt other = (GuessIt) obj;
			if (audioCodec == null) {
				if (other.audioCodec != null) {
					return false;
				}
			} else if (!audioCodec.equals(other.audioCodec)) {
				return false;
			}
			if (container == null) {
				if (other.container != null) {
					return false;
				}
			} else if (!container.equals(other.container)) {
				return false;
			}
			if (mimeType == null) {
				if (other.mimeType != null) {
					return false;
				}
			} else if (!mimeType.equals(other.mimeType)) {
				return false;
			}
			if (originalFormat == null) {
				if (other.originalFormat != null) {
					return false;
				}
			} else if (!originalFormat.equals(other.originalFormat)) {
				return false;
			}
			if (releaseGroup == null) {
				if (other.releaseGroup != null) {
					return false;
				}
			} else if (!releaseGroup.equals(other.releaseGroup)) {
				return false;
			}
			if (title == null) {
				if (other.title != null) {
					return false;
				}
			} else if (!title.equals(other.title)) {
				return false;
			}
			if (type != other.type) {
				return false;
			}
			if (videoCodec == null) {
				if (other.videoCodec != null) {
					return false;
				}
			} else if (!videoCodec.equals(other.videoCodec)) {
				return false;
			}
			if (videoResolutionFormat == null) {
				if (other.videoResolutionFormat != null) {
					return false;
				}
			} else if (!videoResolutionFormat.equals(other.videoResolutionFormat)) {
				return false;
			}
			return true;
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code BestGuess} structure.
	 */
	public static class BestGuess extends GuessItem {
		private final String reason;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the video classification.
		 * @param imdbId the IMDB ID.
		 * @param reason the reason for being considered the best guess.
		 */
		public BestGuess(
			String movieName,
			String movieYear,
			String videoClassification,
			String imdbId,
			String reason
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			this.reason = reason;
		}

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param movieName the movie name/title.
		 * @param movieYear the release year.
		 * @param videoClassification the {@link VideoClassification}.
		 * @param imdbId the IMDB ID.
		 * @param reason the reason for being considered the best guess.
		 */
		public BestGuess(
			String movieName,
			String movieYear,
			VideoClassification videoClassification,
			String imdbId,
			String reason
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			this.reason = reason;
		}

		/**
		 * @return The "best" guess reason.
		 */
		public String getReason() {
			return reason;
		}

		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "MovieName", title, true, true, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieKind", videoClassification, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieYear", year, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IDMovieIMDB", imdbId, false, false, true);
			addFieldToStringBuilder(first, sb, "Reason", reason, true, false, true);
			sb.append("]");
			return sb.toString();
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((reason == null) ? 0 : reason.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof BestGuess)) {
				return false;
			}
			BestGuess other = (BestGuess) obj;
			if (reason == null) {
				if (other.reason != null) {
					return false;
				}
			} else if (!reason.equals(other.reason)) {
				return false;
			}
			return true;
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code GuessMovieFromString}
	 * result.
	 *
	 * @author Nadahar
	 */
	public static class MovieGuess {
		private final GuessIt guessIt;
		private final Map<String, GuessFromString> guessesFromString;
		private final Map<String, GuessItem> imdbSuggestions;
		private final BestGuess bestGuess;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param guessIt the {@link GuessIt}.
		 * @param guessesFromString the {@link Map} of IMDB IDs and the
		 *            corresponding {@link GuessFromString}s.
		 * @param imdbSuggestions the {@link Map} of IMDB IDs and the
		 *            corresponding {@link GuessItem}.
		 * @param bestGuess the {@link BestGuess}.
		 */
		public MovieGuess(
			GuessIt guessIt,
			Map<String, GuessFromString> guessesFromString,
			Map<String, GuessItem> imdbSuggestions,
			BestGuess bestGuess
		) {
			this.guessIt = guessIt;
			this.guessesFromString = guessesFromString != null ? guessesFromString : new HashMap<String, GuessFromString>();
			this.imdbSuggestions = imdbSuggestions != null ? imdbSuggestions : new HashMap<String, GuessItem>();
			this.bestGuess = bestGuess;
		}

		/**
		 * @return The {@link GuessIt} or {@code null}.
		 */
		public GuessIt getGuessIt() {
			return guessIt;
		}

		/**
		 * @return The {@link Map} of {@link GuessFromString}s.
		 */
		public Map<String, GuessFromString> getGuessesFromString() {
			return Collections.unmodifiableMap(guessesFromString);
		}


		/**
		 * @return The {@link Map} of IMDB suggestions.
		 */
		public Map<String, GuessItem> getImdbSuggestions() {
			return Collections.unmodifiableMap(imdbSuggestions);
		}


		/**
		 * @return The {@link BestGuess} or {@code null}.
		 */
		public BestGuess getBestGuess() {
			return bestGuess;
		}

		/**
		 * Returns a {@link String} representation of this instance.
		 *
		 * @param includeName {@code true} if the name of this {@link Class}
		 *            should be included, {@code false} otherwise.
		 * @return The {@link String} representation.
		 */
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = true;
			if (guessIt != null) {
				first &= !addFieldToStringBuilder(first, sb, "GuessIt", guessIt.toString(false), false, false, true);
			}
			if (!guessesFromString.isEmpty()) {
				boolean innerFirst = true;
				StringBuilder innerSB = new StringBuilder();
				for (Entry<String, GuessFromString> entry : guessesFromString.entrySet()) {
					innerFirst &= !addFieldToStringBuilder(
						innerFirst,
						innerSB,
						entry.getKey(),
						entry.getValue().toString(false),
						false,
						false,
						true
					);
				}

				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append("GuessesFromString{").append(innerSB).append("}");
			}
			if (!imdbSuggestions.isEmpty()) {
				boolean innerFirst = true;
				StringBuilder innerSB = new StringBuilder();
				for (Entry<String, GuessItem> entry : imdbSuggestions.entrySet()) {
					innerFirst &= !addFieldToStringBuilder(
						innerFirst,
						innerSB,
						entry.getKey(),
						entry.getValue().toString(false),
						false,
						false,
						true
					);
				}
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append("IMDBSuggestions{").append(innerSB).append("}");
			}
			if (bestGuess != null) {
				addFieldToStringBuilder(first, sb, "BestGuess", bestGuess.toString(false), false, false, true);
			}
			sb.append("]");
			return sb.toString();
		}

		/**
		 * Creates a new {@link MovieGuess} from a {@link Struct} from an
		 * OpenSubtitles {@code GuessMovieFromString} result.
		 *
		 * @param guessStruct the {@link Struct} containing the
		 *            {@code GuessMovieFromString} data.
		 * @return The resulting {@link MovieGuess} or {@code null}.
		 */
		public static MovieGuess createFromStruct(Struct guessStruct) {
			if (guessStruct == null || guessStruct.isEmpty()) {
				return null;
			}

			GuessIt guessIt;
			Member<?, ?> member = guessStruct.get("GuessIt");
			if (member != null && member.getValue() instanceof Struct) {
				Struct guessItStruct = (Struct) member.getValue();
				guessIt = new GuessIt(
					Member.getString(guessItStruct, "mimetype"),
					Member.getString(guessItStruct, "videoCodec"),
					Member.getString(guessItStruct, "container"),
					Member.getString(guessItStruct, "title"),
					Member.getString(guessItStruct, "format"),
					Member.getString(guessItStruct, "releaseGroup"),
					Member.getString(guessItStruct, "screenSize"),
					Member.getString(guessItStruct, "type"),
					Member.getString(guessItStruct, "audioCodec")
				);
			} else {
				guessIt = null;
			}

			Map<String, GuessFromString> guessesFromString;
			member = guessStruct.get("GuessMovieFromString");
			if (member != null) {
				guessesFromString = GuessFromString.createGuessesFromStringFromMember(member);
			} else {
				guessesFromString = new HashMap<>();
			}

			Map<String, GuessItem> imdbSuggestions;
			member = guessStruct.get("GetIMDBSuggest");
			if (member != null) {
				imdbSuggestions = GuessItem.createFromStruct(member);
			} else {
				imdbSuggestions = new HashMap<>();
			}

			BestGuess bestGuess;
			member = guessStruct.get("BestGuess");
			if (member != null && member.getValue() instanceof Struct) {
				Struct bestGuessStruct = (Struct) member.getValue();
				bestGuess = new BestGuess(
					Member.getString(bestGuessStruct, "MovieName"),
					Member.getString(bestGuessStruct, "MovieYear"),
					Member.getString(bestGuessStruct, "MovieKind"),
					Member.getString(bestGuessStruct, "IDMovieIMDB"),
					Member.getString(bestGuessStruct, "Reason")
				);
			} else {
				bestGuess = null;
			}

			return new MovieGuess(guessIt, guessesFromString, imdbSuggestions, bestGuess);
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code CheckMovieHash} or
	 * {@code CheckMovieHash2} item.
	 */
	public static class CheckMovieHashItem extends GuessItem {

		/** The {@code SubCount} */
		protected final int subCount;

		/** The {@code SeenCount} */
		protected final int seenCount;

		/** The {@code MovieHash} */
		protected final String movieHash;

		/** The {@code SeriesSeason} */
		protected final int seriesSeason;

		/** The {@code SeriesEpisode} */
		protected final int seriesEpisode;

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param videoClassification the video classification.
		 * @param subCount the {@code SubCount}.
		 * @param seenCount the {@code SeenCount}.
		 * @param imdbId the {@code IMDB ID}.
		 * @param movieYear the release year.
		 * @param movieHash the video file hash.
		 * @param seriesEpisode the episode number or {@code -1} if it doesn't
		 *            apply.
		 * @param movieName the movie name/title.
		 * @param seriesSeason the season number or {@code -1} if it doesn't
		 *            apply.
		 */
		public CheckMovieHashItem(
			String videoClassification,
			int subCount,
			int seenCount,
			String imdbId,
			String movieYear,
			String movieHash,
			int seriesEpisode,
			String movieName,
			int seriesSeason
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			this.subCount = subCount;
			this.seenCount = seenCount;
			this.movieHash = movieHash;
			this.seriesEpisode = seriesEpisode;
			this.seriesSeason = seriesSeason;
		}

		/**
		 * Creates a new instance using the specified values.
		 *
		 * @param videoClassification the {@link VideoClassification}.
		 * @param subCount the {@code SubCount}.
		 * @param seenCount the {@code SeenCount}.
		 * @param imdbId the {@code IMDB ID}.
		 * @param movieYear the release year.
		 * @param movieHash the video file hash.
		 * @param seriesEpisode the episode number or {@code -1} if it doesn't
		 *            apply.
		 * @param movieName the movie name/title.
		 * @param seriesSeason the season number or {@code -1} if it doesn't
		 *            apply.
		 */
		public CheckMovieHashItem(
			VideoClassification videoClassification,
			int subCount,
			int seenCount,
			String imdbId,
			String movieYear,
			String movieHash,
			int seriesEpisode,
			String movieName,
			int seriesSeason
		) {
			super(movieName, movieYear, videoClassification, imdbId);
			this.subCount = subCount;
			this.seenCount = seenCount;
			this.movieHash = movieHash;
			this.seriesEpisode = seriesEpisode;
			this.seriesSeason = seriesSeason;
		}

		/**
		 * @return The {@code SubCount} or {@code -1} if unknown.
		 */
		public int getSubCount() {
			return subCount;
		}

		/**
		 * @return The {@code SeenCount} or {@code -1} if unknown.
		 */
		public int getSeenCount() {
			return seenCount;
		}

		/**
		 * @return The {@code MovieHash}.
		 */
		public String getMovieHash() {
			return movieHash;
		}

		/**
		 * @return The {@code SeriesSeason} or {@code -1} if unknown.
		 */
		public int getSeriesSeason() {
			return seriesSeason;
		}

		/**
		 * @return The {@code SeriesEpisode} or {@code -1} if unknown.
		 */
		public int getSeriesEpisode() {
			return seriesEpisode;
		}

		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "MovieName", title, true, true, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieKind", videoClassification, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieYear", year, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IDMovieIMDB", imdbId, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieHash", movieHash, false, false, true);
			if (subCount >= 0) {
				first &= !addFieldToStringBuilder(first, sb, "SubCount", subCount, false, false, true);
			}
			if (seenCount >= 0) {
				first &= !addFieldToStringBuilder(first, sb, "SeenCount", seenCount, false, false, true);
			}
			if (seriesSeason >= 0) {
				first &= !addFieldToStringBuilder(first, sb, "SeriesSeason", seriesSeason, false, false, true);
			}
			if (seriesEpisode >= 0) {
				addFieldToStringBuilder(first, sb, "SeriesEpisode", seriesSeason, false, false, true);
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((movieHash == null) ? 0 : movieHash.hashCode());
			result = prime * result + seenCount;
			result = prime * result + seriesEpisode;
			result = prime * result + seriesSeason;
			result = prime * result + subCount;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (!(obj instanceof CheckMovieHashItem)) {
				return false;
			}
			CheckMovieHashItem other = (CheckMovieHashItem) obj;
			if (movieHash == null) {
				if (other.movieHash != null) {
					return false;
				}
			} else if (!movieHash.equals(other.movieHash)) {
				return false;
			}
			if (seenCount != other.seenCount) {
				return false;
			}
			if (seriesEpisode != other.seriesEpisode) {
				return false;
			}
			if (seriesSeason != other.seriesSeason) {
				return false;
			}
			if (subCount != other.subCount) {
				return false;
			}
			return true;
		}
	}

	/**
	 * A class holding information about an OpenSubtitles subtitles item.
	 *
	 * @author Nadahar
	 */
	public static class SubtitleItem {
		private final String matchedBy;
		private final String idSubtitleFile;
		private final String subFileName;
		private final String subHash;
		private final long subLastTS;
		private final String idSubtitle;
		private final String languageCode;
		private final SubtitleType subtitleType;
		private final boolean subBad;
		private final double subRating;
		private final int subDownloadsCnt;
		private final String movieFPS;
		private final String idMovieImdb;
		private final String movieName;
		private final String movieNameEng;
		private final int movieYear;
		private final String userRank;
		private final int seriesSeason;
		private final int seriesEpisode;
		private final VideoClassification movieKind;
		private final String subEncoding;
		private final boolean subFromTrusted;
		private final URI subDownloadLink;
		private final double openSubtitlesScore;
		private final double score;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param matchedBy the {@code MatchedBy} {@link String}.
		 * @param idSubtitleFile the {@code IDSubtitleFile} {@link String}.
		 * @param subFileName the {@code SubFileName} {@link String}.
		 * @param subHash the {@code SubHash} {@link String}.
		 * @param subLastTS the last subtitle timestamp in milliseconds.
		 * @param idSubtitle the {@code IDSubtitle} {@link String}.
		 * @param languageCode the ISO 639-2 (3 letter) language code.
		 * @param subtitleType the {@link SubtitleType}.
		 * @param subBad the boolean equivalent of {@code SubBad}.
		 * @param subRating the {@code double} equivalent of {@code SubRating}.
		 * @param subDownloadsCnt the subtitles download count.
		 * @param movieFPS the frames per second for the video.
		 * @param idMovieImdb the IMDB ID.
		 * @param movieName the movie name/title.
		 * @param movieNameEng the English movie name/title if different.
		 * @param movieYear the release year.
		 * @param userRank the {@code UserRank} {@link String}.
		 * @param seriesSeason the season number if relevant or {@code 0}.
		 * @param seriesEpisode the episode number if relevant or {@code 0}.
		 * @param movieKind the {@link VideoClassification}.
		 * @param subEncoding the {@code SubEncoding} {@link String}.
		 * @param subFromTrusted the boolean equivalent of
		 *            {@code SubFromTrusted}.
		 * @param subDownloadLink the {@link URI} equivalent of
		 *            {@code SubDownloadLink}.
		 * @param openSubtitlesScore the {@code Score} {@code double}.
		 * @param prettifier the {@link FileNamePrettifier} for the video item.
		 * @param media the {@link DLNAMediaInfo} for the video media.
		 */
		public SubtitleItem(
			String matchedBy,
			String idSubtitleFile,
			String subFileName,
			String subHash,
			long subLastTS,
			String idSubtitle,
			String languageCode,
			SubtitleType subtitleType,
			boolean subBad,
			double subRating,
			int subDownloadsCnt,
			String movieFPS,
			String idMovieImdb,
			String movieName,
			String movieNameEng,
			int movieYear,
			String userRank,
			int seriesSeason,
			int seriesEpisode,
			VideoClassification movieKind,
			String subEncoding,
			boolean subFromTrusted,
			URI subDownloadLink,
			double openSubtitlesScore,
			FileNamePrettifier prettifier,
			DLNAMediaInfo media
		) {
			this.matchedBy = matchedBy;
			this.idSubtitleFile = idSubtitleFile;
			this.subFileName = subFileName;
			this.subHash = subHash;
			this.subLastTS = subLastTS;
			this.idSubtitle = idSubtitle;
			this.languageCode = languageCode;
			this.subtitleType = subtitleType;
			this.subBad = subBad;
			this.subRating = subRating;
			this.subDownloadsCnt = subDownloadsCnt;
			this.movieFPS = movieFPS;
			this.idMovieImdb = idMovieImdb;
			this.movieName = movieName;
			this.movieNameEng = movieNameEng;
			this.movieYear = movieYear;
			this.userRank = userRank;
			this.seriesSeason = seriesSeason;
			this.seriesEpisode = seriesEpisode;
			this.movieKind = movieKind;
			this.subEncoding = subEncoding;
			this.subFromTrusted = subFromTrusted;
			this.subDownloadLink = subDownloadLink;
			this.openSubtitlesScore = openSubtitlesScore;
			double tmpScore = 0.0;
			if (isNotBlank(matchedBy)) {
				switch (matchedBy.toLowerCase(Locale.ROOT)) {
					case "moviehash":
						tmpScore += 200d;
					case "imdbid":
						tmpScore += 100d;
					case "tag":
						tmpScore += 10d;
				}
			}
			if (prettifier != null) {
				Locale locale = PMS.getLocale();
				if (isNotBlank(prettifier.getFileNameWithoutExtension())) {
					String subFileNameWithoutExtension = FileUtil.getFileNameWithoutExtension(subFileName);
					if (isNotBlank(subFileNameWithoutExtension)) {
						// 0.6 and below gives a score of 0, 1.0 give a score of 40.
						tmpScore += 40d * 2.5 * Math.max(getJaroWinklerDistance(
							prettifier.getFileNameWithoutExtension().toLowerCase(locale),
							subFileNameWithoutExtension.toLowerCase(Locale.ENGLISH)
						) - 0.6, 0);
					}
				}
				if (isNotBlank(prettifier.getName()) && (isNotBlank(movieName) || isNotBlank(movieNameEng))) {
					double nameScore = isBlank(movieName) ?
						0.0 :
						getJaroWinklerDistance(prettifier.getName().toLowerCase(locale), movieName.toLowerCase(locale));
					nameScore = Math.max(
						nameScore,
						isBlank(movieNameEng) ? 0.0 : getJaroWinklerDistance(
							prettifier.getName().toLowerCase(Locale.ENGLISH),
							movieNameEng.toLowerCase(Locale.ENGLISH)
						)
					);
					// 0.5 and below gives a score of 0, 1 give a score of 30
					tmpScore += 30d * 2 * Math.max(nameScore - 0.5, 0);
				}
				if (
					seriesEpisode > 0 &&
					seriesEpisode == prettifier.getEpisode() &&
					(
						(
							seriesSeason < 1 &&
							prettifier.getSeason() < 1
						) ||
						seriesSeason == prettifier.getSeason()
					)
				) {
					tmpScore += 30d;
				}
				if (movieYear > 0 && movieYear == prettifier.getYear()) {
					tmpScore += 20d;
				}
				if (movieKind != null && movieKind == prettifier.getClassification()) {
					tmpScore += 20d;
				}
			}
			if (subLastTS > 0 && media != null && media.getDurationInSeconds() > 0) {
				long mediaDuration = (long) (media.getDurationInSeconds() * 1000);
				// Trying to guess the most likely time for the last subtitle
				long mediaLastTS = mediaDuration - Math.min(Math.max((long) (mediaDuration * 0.02), 2000), 120000);
				if (mediaLastTS > 0) {
					long diff = Math.abs(subLastTS - mediaLastTS);
					tmpScore += 30d * Math.max(0.5 - (double) diff / mediaLastTS, 0);
				}
			}
			this.score = tmpScore;
		}

		/**
		 * Converts OpenSubtitles' {@code SubFormat} to the corresponding
		 * {@link SubtitleType}.
		 *
		 * @param subFormat the {@code SubFormat} {@link String} to convert.
		 * @return The resulting {@link SubtitleType} or {@code null}.
		 */
		public static SubtitleType subFormatToSubtitleType(String subFormat) {
			if (subFormat == null) {
				return null;
			}

			switch (subFormat.toLowerCase(Locale.ROOT)) {
				case "sub":
					return SubtitleType.MICRODVD;
				case "srt":
					return SubtitleType.SUBRIP;
				case "txt":
					return SubtitleType.TEXT;
				case "ssa":
					return SubtitleType.ASS;
				case "smi":
					return SubtitleType.SAMI;
				case "mpl":
				case "tmp":
					return SubtitleType.UNSUPPORTED;
				case "vtt":
					return SubtitleType.WEBVTT;
				default:
					LOGGER.warn("OpenSubtitles: Warning, unknown subtitles type \"{}\"", subFormat);
					return SubtitleType.UNKNOWN;
			}
		}

		/**
		 * @return The {@code MatchedBy}.
		 */
		public String getMatchedBy() {
			return matchedBy;
		}

		/**
		 * @return The {@code IDSubtitleFile}.
		 */
		public String getIDSubtitleFile() {
			return idSubtitleFile;
		}

		/**
		 * @return The {@code SubFileName}.
		 */
		public String getSubFileName() {
			return subFileName;
		}

		/**
		 * @return The {@code SubHash}.
		 */
		public String getSubHash() {
			return subHash;
		}

		/**
		 * @return The last subtitle timestamp in milliseconds.
		 */
		public long getSubLastTS() {
			return subLastTS;
		}

		/**
		 * @return The {@code IDSubtitle}.
		 */
		public String getIDSubtitle() {
			return idSubtitle;
		}

		/**
		 * @return The ISO 639-2 (3 letter) language code.
		 */
		public String getLanguageCode() {
			return languageCode;
		}

		/**
		 * @return The {@link SubtitleType}.
		 */
		public SubtitleType getSubtitleType() {
			return subtitleType;
		}

		/**
		 * @return The {@code SubBad}.
		 */
		public boolean isSubBad() {
			return subBad;
		}

		/**
		 * @return The {@code SubRating}.
		 */
		public double getSubRating() {
			return subRating;
		}


		/**
		 * @return The subtitles download count.
		 */
		public int getSubDownloadsCnt() {
			return subDownloadsCnt;
		}

		/**
		 * @return The frames per second.
		 */
		public String getMovieFPS() {
			return movieFPS;
		}


		/**
		 * @return The IMDB ID.
		 */
		public String getIdMovieImdb() {
			return idMovieImdb;
		}

		/**
		 * @return The movie name.
		 */
		public String getMovieName() {
			return movieName;
		}

		/**
		 * @return The English movie name.
		 */
		public String getMovieNameEng() {
			return movieNameEng;
		}

		/**
		 * @return The movie release year.
		 */
		public int getMovieYear() {
			return movieYear;
		}

		/**
		 * @return The {@code UserRank}.
		 */
		public String getUserRank() {
			return userRank;
		}

		/**
		 * @return The season number.
		 */
		public int getSeriesSeason() {
			return seriesSeason;
		}

		/**
		 * @return The episode number.
		 */
		public int getSeriesEpisode() {
			return seriesEpisode;
		}

		/**
		 * @return The {@link VideoClassification}.
		 */
		public VideoClassification getMovieKind() {
			return movieKind;
		}

		/**
		 * @return The {@code SubEncoding}.
		 */
		public String getSubEncoding() {
			return subEncoding;
		}

		/**
		 * @return The {@code SubFromTrusted}.
		 */
		public boolean isSubFromTrusted() {
			return subFromTrusted;
		}

		/**
		 * @return The {@code SubDownloadLink}.
		 */
		public URI getSubDownloadLink() {
			return subDownloadLink;
		}

		/**
		 * @return The OpenSubtitles {@code Score}.
		 */
		public double getOpenSubtitlesScore() {
			return openSubtitlesScore;
		}

		/**
		 * @return The calculated {@code Score}.
		 */
		public double getScore() {
			return score;
		}

		/**
		 * Returns a {@link String} representation of this instance.
		 *
		 * @param includeName {@code true} if the name of this {@link Class}
		 *            should be included, {@code false} otherwise.
		 * @return The {@link String} representation.
		 */
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append("[");
			} else {
				sb.append("[");
			}
			boolean first = !addFieldToStringBuilder(true, sb, "MatchedBy", matchedBy, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "LanguageCode", languageCode, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "Score", Double.valueOf(score), false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "OSScore", Double.valueOf(openSubtitlesScore), false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IDSubtitleFile", idSubtitleFile, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "SubFileName", subFileName, true, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubHash", subHash, false, false, false);
			if (subLastTS > 0) {
				first &= !addFieldToStringBuilder(
					first,
					sb,
					"SubLastTS",
					new SimpleDateFormat("H:mm:ss").format(new Date(subLastTS)),
					false,
					false,
					false
				);
			}
			first &= !addFieldToStringBuilder(first, sb, "IDSubtitle", idSubtitle, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "SubFormat", subtitleType, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubBad", Boolean.valueOf(subBad), false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubRating", Double.valueOf(subRating), false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "SubDLCnt", Integer.valueOf(subDownloadsCnt), false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "MovieFPS", movieFPS, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IMDB ID", idMovieImdb, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieName", movieName, true, false, true);
			first &= !addFieldToStringBuilder(first, sb, "MovieNameEng", movieNameEng, true, false, true);
			if (movieYear > 0) {
				first &= !addFieldToStringBuilder(first, sb, "MovieYear", Integer.valueOf(movieYear), false, false, false);
			}
			first &= !addFieldToStringBuilder(first, sb, "UserRank", userRank, false, false, false);
			if (seriesSeason > 0) {
				first &= !addFieldToStringBuilder(first, sb, "SeriesSeason", Integer.valueOf(seriesSeason), false, false, false);
			}
			if (seriesEpisode > 0) {
				first &= !addFieldToStringBuilder(first, sb, "SeriesEpisode", Integer.valueOf(seriesEpisode), false, false, false);
			}
			first &= !addFieldToStringBuilder(first, sb, "MovieKind", movieKind, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubEncoding", subEncoding, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubFromTrusted", Boolean.valueOf(subFromTrusted), false, false, true);
			addFieldToStringBuilder(first, sb, "SubDownloadLink", subDownloadLink, true, true, true);
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((idMovieImdb == null) ? 0 : idMovieImdb.hashCode());
			result = prime * result + ((idSubtitle == null) ? 0 : idSubtitle.hashCode());
			result = prime * result + ((idSubtitleFile == null) ? 0 : idSubtitleFile.hashCode());
			result = prime * result + ((languageCode == null) ? 0 : languageCode.hashCode());
			result = prime * result + ((matchedBy == null) ? 0 : matchedBy.hashCode());
			result = prime * result + ((movieFPS == null) ? 0 : movieFPS.hashCode());
			result = prime * result + ((movieKind == null) ? 0 : movieKind.hashCode());
			result = prime * result + ((movieName == null) ? 0 : movieName.hashCode());
			result = prime * result + ((movieNameEng == null) ? 0 : movieNameEng.hashCode());
			result = prime * result + movieYear;
			long temp;
			temp = Double.doubleToLongBits(openSubtitlesScore);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(score);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + seriesEpisode;
			result = prime * result + seriesSeason;
			result = prime * result + (subBad ? 1231 : 1237);
			result = prime * result + ((subDownloadLink == null) ? 0 : subDownloadLink.hashCode());
			result = prime * result + subDownloadsCnt;
			result = prime * result + ((subEncoding == null) ? 0 : subEncoding.hashCode());
			result = prime * result + ((subFileName == null) ? 0 : subFileName.hashCode());
			result = prime * result + (subFromTrusted ? 1231 : 1237);
			result = prime * result + ((subHash == null) ? 0 : subHash.hashCode());
			result = prime * result + (int) (subLastTS ^ (subLastTS >>> 32));
			temp = Double.doubleToLongBits(subRating);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((subtitleType == null) ? 0 : subtitleType.hashCode());
			result = prime * result + ((userRank == null) ? 0 : userRank.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof SubtitleItem)) {
				return false;
			}
			SubtitleItem other = (SubtitleItem) obj;
			if (idMovieImdb == null) {
				if (other.idMovieImdb != null) {
					return false;
				}
			} else if (!idMovieImdb.equals(other.idMovieImdb)) {
				return false;
			}
			if (idSubtitle == null) {
				if (other.idSubtitle != null) {
					return false;
				}
			} else if (!idSubtitle.equals(other.idSubtitle)) {
				return false;
			}
			if (idSubtitleFile == null) {
				if (other.idSubtitleFile != null) {
					return false;
				}
			} else if (!idSubtitleFile.equals(other.idSubtitleFile)) {
				return false;
			}
			if (languageCode == null) {
				if (other.languageCode != null) {
					return false;
				}
			} else if (!languageCode.equals(other.languageCode)) {
				return false;
			}
			if (matchedBy == null) {
				if (other.matchedBy != null) {
					return false;
				}
			} else if (!matchedBy.equals(other.matchedBy)) {
				return false;
			}
			if (movieFPS == null) {
				if (other.movieFPS != null) {
					return false;
				}
			} else if (!movieFPS.equals(other.movieFPS)) {
				return false;
			}
			if (movieKind != other.movieKind) {
				return false;
			}
			if (movieName == null) {
				if (other.movieName != null) {
					return false;
				}
			} else if (!movieName.equals(other.movieName)) {
				return false;
			}
			if (movieNameEng == null) {
				if (other.movieNameEng != null) {
					return false;
				}
			} else if (!movieNameEng.equals(other.movieNameEng)) {
				return false;
			}
			if (movieYear != other.movieYear) {
				return false;
			}
			if (Double.doubleToLongBits(openSubtitlesScore) != Double.doubleToLongBits(other.openSubtitlesScore)) {
				return false;
			}
			if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
				return false;
			}
			if (seriesEpisode != other.seriesEpisode) {
				return false;
			}
			if (seriesSeason != other.seriesSeason) {
				return false;
			}
			if (subBad != other.subBad) {
				return false;
			}
			if (subDownloadLink == null) {
				if (other.subDownloadLink != null) {
					return false;
				}
			} else if (!subDownloadLink.equals(other.subDownloadLink)) {
				return false;
			}
			if (subDownloadsCnt != other.subDownloadsCnt) {
				return false;
			}
			if (subEncoding == null) {
				if (other.subEncoding != null) {
					return false;
				}
			} else if (!subEncoding.equals(other.subEncoding)) {
				return false;
			}
			if (subFileName == null) {
				if (other.subFileName != null) {
					return false;
				}
			} else if (!subFileName.equals(other.subFileName)) {
				return false;
			}
			if (subFromTrusted != other.subFromTrusted) {
				return false;
			}
			if (subHash == null) {
				if (other.subHash != null) {
					return false;
				}
			} else if (!subHash.equals(other.subHash)) {
				return false;
			}
			if (subLastTS != other.subLastTS) {
				return false;
			}
			if (Double.doubleToLongBits(subRating) != Double.doubleToLongBits(other.subRating)) {
				return false;
			}
			if (subtitleType != other.subtitleType) {
				return false;
			}
			if (userRank == null) {
				if (other.userRank != null) {
					return false;
				}
			} else if (!userRank.equals(other.userRank)) {
				return false;
			}
			return true;
		}

		/**
		 * Creates a new {@link SubtitleItem} from a {@link Struct} from an
		 * OpenSubtitles {@code SearchSubtitles} result.
		 *
		 * @param subtitlesStruct the {@link Struct} containing the information
		 *            about the set of subtitles.
		 * @param prettifier the {@link FileNamePrettifier} for the video item.
		 * @param media the {@link DLNAMediaInfo} instance for the video.
		 * @return The resulting {@link SubtitleItem} or {@code null}.
		 */
		public static SubtitleItem createFromStruct(
			Struct subtitlesStruct,
			FileNamePrettifier prettifier,
			DLNAMediaInfo media
		) {
			if (subtitlesStruct == null) {
				return null;
			}

			Member<?, ?> member = subtitlesStruct.get("SubDownloadLink");
			String urlString = member == null ? null : member.getValue().toString();
			if (isBlank(urlString)) {
				return null;
			}
			URI url;
			try {
				url = new URI(urlString);
			} catch (URISyntaxException e) {
				LOGGER.error("OpenSubtitles: Invalid subtitles URL \"{}\": {}", urlString, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}

			member = subtitlesStruct.get("SubLanguageID");
			String languageCode = member == null ? null : member.getValue().toString();
			if (Iso639.codeIsValid(languageCode)) {
				languageCode = Iso639.getISO639_2Code(languageCode);
			} else {
				member = subtitlesStruct.get("ISO639");
				languageCode = member == null ? null : member.getValue().toString();
				if (Iso639.codeIsValid(languageCode)) {
					languageCode = Iso639.getISO639_2Code(languageCode);
				} else {
					languageCode = DLNAMediaLang.UND;
				}
			}

			member = subtitlesStruct.get("SubLastTS");
			long lastTS;
			String lastTSString = member == null ? null : member.getValue().toString();
			if (isBlank(lastTSString)) {
				lastTS = -1;
			} else {
				try {
					lastTS = new SimpleDateFormat("HH:mm:ss").parse(lastTSString).getTime();
				} catch (java.text.ParseException e) {
					try {
						lastTS = Long.parseLong(lastTSString);
					} catch (NumberFormatException nfe) {
						lastTS = -1;
					}
				}
			}

			return new SubtitleItem(
				Member.getString(subtitlesStruct, "MatchedBy"),
				Member.getString(subtitlesStruct, "IDSubtitleFile"),
				Member.getString(subtitlesStruct, "SubFileName"),
				Member.getString(subtitlesStruct, "SubHash"),
				lastTS,
				Member.getString(subtitlesStruct, "IDSubtitle"),
				languageCode,
				subFormatToSubtitleType(Member.getString(subtitlesStruct, "SubFormat")),
				Member.getBoolean(subtitlesStruct, "SubBad"),
				Member.getDouble(subtitlesStruct, "SubRating"),
				Member.getInt(subtitlesStruct, "SubDownloadsCnt"),
				Member.getString(subtitlesStruct, "MovieFPS"),
				Member.getString(subtitlesStruct, "IDMovieImdb"),
				Member.getString(subtitlesStruct, "MovieName"),
				Member.getString(subtitlesStruct, "MovieNameEng"),
				Member.getInt(subtitlesStruct, "MovieYear"),
				Member.getString(subtitlesStruct, "UserRank"),
				Member.getInt(subtitlesStruct, "SeriesSeason"),
				Member.getInt(subtitlesStruct, "SeriesEpisode"),
				VideoClassification.typeOf(Member.getString(subtitlesStruct, "MovieKind")),
				Member.getString(subtitlesStruct, "SubEncoding"),
				Member.getBoolean(subtitlesStruct, "SubFromTrusted"),
				url,
				Member.getDouble(subtitlesStruct, "Score"),
				prettifier,
				media
			);
		}
	}

	/**
	 * A class representing a {@link GuessItem} and a score pair. Its natural
	 * sorting sorts the "best" guesses first.
	 */
	public static class GuessCandidate implements Comparable<GuessCandidate> {
		private final double score;
		private final GuessItem guessItem;

		/**
		 * Creates a new instance with the specified parameters.
		 *
		 * @param score the score for this candidate.
		 * @param guessItem the {@link GuessItem} for this candidate.
		 */
		public GuessCandidate(double score, GuessItem guessItem) {
			this.score = score;
			this.guessItem = guessItem;
		}

		/**
		 * @return The score.
		 */
		public double getScore() {
			return score;
		}

		/**
		 * @return The {@link GuessItem}.
		 */
		public GuessItem getGuessItem() {
			return guessItem;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append("[Score=").append(score);
			sb.append(", GuessItem=").append(guessItem).append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((guessItem == null) ? 0 : guessItem.hashCode());
			long temp;
			temp = Double.doubleToLongBits(score);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof GuessCandidate)) {
				return false;
			}
			GuessCandidate other = (GuessCandidate) obj;
			if (guessItem == null) {
				if (other.guessItem != null) {
					return false;
				}
			} else if (!guessItem.equals(other.guessItem)) {
				return false;
			}
			if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(GuessCandidate o) {
			if (o == null) {
				return -1;
			}
			int result = Double.compare(o.score, score);
			if (result != 0) {
				return result;
			}
			if (guessItem == null) {
				if (o.guessItem == null) {
					return 0;
				}
				return 1;
			}
			if (o.guessItem == null) {
				return -1;
			}
			if (guessItem.getImdbId() == null) {
				if (o.guessItem.getImdbId() != null) {
					return 1;
				}
			} else if (o.guessItem.getImdbId() == null) {
				return -1;
			}
			return guessItem.getImdbId().compareTo(o.guessItem.getImdbId());
		}
	}

	/**
	 * This enum represents the potential OpenSubtitles HTTP response codes.
	 *
	 * @author Nadahar
	 */
	public static enum HTTPResponseCode {

		/** Successful: OK */
		OK(200, false),

		/** Error: Too many requests (<a href="http://forum.opensubtitles.org/viewtopic.php?f=8&t=16072">more information</a>) */
		TOO_MANY_REQUESTS(429, true),

		/** Server Error: Service Unavailable */
		SERVICE_UNAVAILABLE(503, true),

		/** Origin Error: Unclear cause, seems to occur during heavy server load */
		ORIGIN_ERROR(520, true);

		private final int responseCode;
		private final boolean error;

		private HTTPResponseCode(int responseCode, boolean error) {
			this.responseCode = responseCode;
			this.error = error;
		}

		/**
		 * @return The status code.
		 */
		public int getStatusCode() {
			return responseCode;
		}

		/**
		 * @return {@code true} if this status indicates an error, {@code false}
		 *         otherwise.
		 */
		public boolean isError() {
			return error;
		}

		@Override
		public String toString() {
			switch (this) {
				case OK:
					return "Successful: OK";
				case ORIGIN_ERROR:
					return "Origin Error: Unknown cause";
				case TOO_MANY_REQUESTS:
					return "Server Error: Service Unavailable";
				case SERVICE_UNAVAILABLE:
					return "Server Error: Service Unavailable (temporary, retry in 1 second)";
				default:
					return name();
			}
		}

		/**
		 * Converts an integer response code to a {@link HTTPResponseCode}
		 * instance if possible.
		 *
		 * @param responseCode the integer response code to convert.
		 * @return The corresponding {@link HTTPResponseCode} instance or
		 *         {@code null} if no match was found.
		 */
		public static HTTPResponseCode typeOf(int responseCode) {
			if (responseCode < 200 || responseCode > 599) {
				return null;
			}
			for (HTTPResponseCode instance : values()) {
				if (instance.responseCode == responseCode) {
					return instance;
				}
			}
			return null;
		}

		/**
		 * Throws an {@link OpenSubtitlesException} if the response code
		 * indicates a problem.
		 *
		 * @param responseCode the response code to handle.
		 * @throws OpenSubtitlesException If the response code indicates a
		 *             problem.
		 */
		public static void handleResponseCode(int responseCode) throws OpenSubtitlesException {
			HTTPResponseCode instance = typeOf(responseCode);
			if (instance == null) {
				throw new OpenSubtitlesException("Unknown response code " + responseCode);
			}
			handleResponseCode(instance);
		}

		/**
		 * Throws an {@link OpenSubtitlesException} if the response code
		 * indicates a problem.
		 *
		 * @param responseCode the {@link HTTPResponseCode} to handle.
		 * @throws OpenSubtitlesException If the response code indicates a
		 *             problem.
		 */
		public static void handleResponseCode(HTTPResponseCode responseCode) throws OpenSubtitlesException {
			if (responseCode == null) {
				throw new IllegalArgumentException("responseCode cannot be null");
			}
			if (responseCode.isError()) {
				throw new OpenSubtitlesException(responseCode.toString());
			}
		}
	}

	/**
	 * This enum represents the potential OpenSubtitles status codes.
	 *
	 * @author Nadahar
	 */
	public static enum StatusCode {

		/** Successful: OK */
		OK(200, false),

		/** Successful: Partial content; message */
		PARTIAL_CONTENT(206, false),

		/** Moved (host) */
		MOVED(301, true),

		/** Error: Unauthorized */
		UNAUTHORIZED(401, true),

		/** Error: Subtitles has invalid format */
		INVALID_SUBTITLES_FORMAT(402, true),

		/** Error: SubHashes (content and sent subhash) are not same! */
		HASH_MISMATCH(403, true),

		/** Error: Subtitles has invalid language! */
		INVALID_SUBTITLES_LANGUAGE(404, true),

		/** Error: Not all mandatory parameters was specified */
		MISSING_MANDATORY_PARAMETER(405, true),

		/** Error: No session */
		NO_SESSION(406, true),

		/** Error: Download limit reached */
		DOWNLOAD_LIMIT_REACHED(407, true),

		/** Error: Invalid parameters */
		INVALID_PARAMETER(408, true),

		/** Error: Method not found */
		UNKNOWN_METHOD(409, true),

		/** Error: Other or unknown error */
		UNKNOWN_ERROR(410, true),

		/** Error: Empty or invalid useragent */
		INVALID_USERAGENT(411, true),

		/** Error: %s has invalid format (reason) */
		INVALID_FORMAT(412, true),

		/** Error: Invalid ImdbID */
		INVALID_IMDBID(413, true),

		/** Error: Unknown User Agent */
		UNKNOWN_USER_AGENT(414, true),

		/** Error: Disabled user agent */
		DISABLED_USER_AGENT(415, true),

		/** Error: Internal subtitle validation failed */
		INTERNAL_VALIDATION_FAILURE(416, true),

		/** Server Error: Service Unavailable */
		SERVICE_UNAVAILABLE(503, true),

		/** Server Error: Server under maintenance */
		SERVER_MAINTENANCE(506, true);

		private final int statusCode;
		private final boolean error;

		private StatusCode(int statusCode, boolean error) {
			this.statusCode = statusCode;
			this.error = error;
		}

		/**
		 * @return The status code.
		 */
		public int getStatusCode() {
			return statusCode;
		}

		/**
		 * @return {@code true} if this status indicates an error, {@code false}
		 *         otherwise.
		 */
		public boolean isError() {
			return error;
		}

		@Override
		public String toString() {
			switch (this) {
				case DISABLED_USER_AGENT:
					return "Error: Disabled user agent";
				case DOWNLOAD_LIMIT_REACHED:
					return "Error: Download limit reached";
				case INTERNAL_VALIDATION_FAILURE:
					return "Error: Internal subtitle validation failed";
				case INVALID_FORMAT:
					return "Error: %s has invalid format (reason)";
				case INVALID_IMDBID:
					return "Error: Invalid ImdbID";
				case INVALID_PARAMETER:
					return "Error: Invalid parameters";
				case INVALID_SUBTITLES_FORMAT:
					return "Error: Subtitles has invalid format";
				case HASH_MISMATCH:
					return "Error: SubHashes (content and sent subhash) are not same!";
				case INVALID_SUBTITLES_LANGUAGE:
					return "Error: Subtitles has invalid language!";
				case INVALID_USERAGENT:
					return "Error: Empty or invalid useragent";
				case MISSING_MANDATORY_PARAMETER:
					return "Error: Not all mandatory parameters was specified";
				case MOVED:
					return "Moved (host)";
				case NO_SESSION:
					return "Error: No session";
				case OK:
					return "Successful: OK";
				case PARTIAL_CONTENT:
					return "Successful: Partial content; message";
				case SERVER_MAINTENANCE:
					return "Server Error: Server under maintenance";
				case SERVICE_UNAVAILABLE:
					return "Server Error: Service Unavailable (temporary, retry in 1 second)";
				case UNAUTHORIZED:
					return "Error: Unauthorized";
				case UNKNOWN_ERROR:
					return "Error: Other or unknown error";
				case UNKNOWN_METHOD:
					return "Error: Method not found";
				case UNKNOWN_USER_AGENT:
					return "Error: Unknown User Agent";
				default:
					return name();
			}
		}

		/**
		 * Tries to parse a {@link String} into a {@link StatusCode}. The
		 * {@link String} must begin with the status code number.
		 *
		 * @param statusCode the {@link String} to parse.
		 * @return The corresponding {@link StatusCode} instance or {@code null}
		 *         if no match was found.
		 */
		public static StatusCode typeOf(String statusCode) {
			if (isBlank(statusCode)) {
				return null;
			}
			Pattern pattern = Pattern.compile("^(\\d+)\\s");
			Matcher matcher = pattern.matcher(statusCode);
			if (matcher.find()) {
				return typeOf(Integer.parseInt(matcher.group(1)));
			}
			return null;
		}

		/**
		 * Converts an integer status code to a {@link StatusCode}
		 * instance if possible.
		 *
		 * @param statusCode the integer status code to convert.
		 * @return The corresponding {@link StatusCode} instance
		 *         or {@code null} if no match was found.
		 */
		public static StatusCode typeOf(int statusCode) {
			if (statusCode < 200 || statusCode > 599) {
				return null;
			}
			for (StatusCode instance : values()) {
				if (instance.statusCode == statusCode) {
					return instance;
				}
			}
			return null;
		}

		/**
		 * Throws an {@link OpenSubtitlesException} if the status code indicates
		 * a problem.
		 *
		 * @param statusCode the status code to handle.
		 * @throws OpenSubtitlesException If the status code indicates a
		 *             problem.
		 */
		public static void handleStatusCode(int statusCode) throws OpenSubtitlesException {
			StatusCode instance = typeOf(statusCode);
			if (instance == null) {
				throw new OpenSubtitlesException("Unknown OpenSubtitles status code " + statusCode);
			}
			handleStatusCode(instance);
		}

		/**
		 * Throws an {@link OpenSubtitlesException} if the status code indicates
		 * a problem.
		 *
		 * @param statusCode the {@link StatusCode} to handle.
		 * @throws OpenSubtitlesException If the status code indicates a
		 *             problem.
		 */
		public static void handleStatusCode(StatusCode statusCode) throws OpenSubtitlesException {
			if (statusCode == null) {
				throw new IllegalArgumentException("statusCode cannot be null");
			}
			switch (statusCode) {
				case OK:
				case PARTIAL_CONTENT:
					return;
				case DISABLED_USER_AGENT:
				case DOWNLOAD_LIMIT_REACHED:
				case HASH_MISMATCH:
				case INTERNAL_VALIDATION_FAILURE:
				case INVALID_FORMAT:
				case INVALID_IMDBID:
				case INVALID_PARAMETER:
				case INVALID_SUBTITLES_FORMAT:
				case INVALID_SUBTITLES_LANGUAGE:
				case INVALID_USERAGENT:
				case MISSING_MANDATORY_PARAMETER:
				case MOVED:
				case NO_SESSION:
				case SERVER_MAINTENANCE:
				case SERVICE_UNAVAILABLE:
				case UNAUTHORIZED:
				case UNKNOWN_ERROR:
				case UNKNOWN_METHOD:
				case UNKNOWN_USER_AGENT:
					throw new OpenSubtitlesException(statusCode.toString());
				default:
					throw new AssertionError("Unimplemented statusCode \"" + statusCode + "\"");
			}
		}
	}

	/**
	 * Signals that a problem of some sort has occurred while communicating with
	 * OpenSubtitles.
	 *
	 * @author Nadahar
	 */
	public static class OpenSubtitlesException extends IOException {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance with the specified detail message and cause.
		 *
		 * @param message the detail message.
		 * @param cause the {@link Throwable} causing this
		 *            {@link OpenSubtitlesException} if any.
		 */
		public OpenSubtitlesException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Creates a new instance with the specified detail message.
		 *
		 * @param message the detail message.
		 */
		public OpenSubtitlesException(String message) {
			super(message);
		}
	}
}
