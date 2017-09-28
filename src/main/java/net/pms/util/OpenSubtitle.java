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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaLang;
import net.pms.formats.v2.SubtitleType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final String SUB_DIR = "subs";
	private static final String UA = "Universal Media Server v1";
	private static final long TOKEN_AGE_TIME = 10 * 60 * 1000; // 10 mins
	//private static final long SUB_FILE_AGE = 14 * 24 * 60 * 60 * 1000; // two weeks
	public static final Path SUBTITLES_FOLDER = Paths.get(PMS.getConfiguration().getDataFile(SUB_DIR));


	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;

	private static final String OPENSUBS_URL = "http://api.opensubtitles.org/xml-rpc";
	private static final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
	private static String token = null;
	private static long tokenAge;

	public static String computeHash(File file) throws IOException {
		long size = file.length();
		FileInputStream fis = new FileInputStream(file);
		return computeHash(fis, size);
	}

	public static String computeHash(InputStream stream, long length) throws IOException {
		int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, length);

		// Buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
		byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, length)];
		long head;
		long tail;
		try (DataInputStream in = new DataInputStream(stream)) {
			// First chunk
			in.readFully(chunkBytes, 0, chunkSizeForFile);

			long position = chunkSizeForFile;
			long tailChunkPosition = length - chunkSizeForFile;

			// Seek to position of the tail chunk, or not at all if length is smaller than two chunks
			while (position < tailChunkPosition && (position += in.skip(tailChunkPosition - position)) >= 0) {
				;
			}

			// Second chunk, or the rest of the data if length is smaller than two chunks
			in.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);

			head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
			tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));
		}

		return String.format("%016x", length + head + tail);
	}

	private static long computeHashForChunk(ByteBuffer buffer) {
		LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
		long hash = 0;

		while (longBuffer.hasRemaining()) {
			hash += longBuffer.get();
		}

		return hash;
	}

	public static String postPage(URLConnection connection, String query) throws IOException {
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("Content-Length", "" + query.length());
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

	public static Document postPageXML(URLConnection connection, String query) throws IOException {
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("Content-Length", "" + query.length());
		((HttpURLConnection) connection).setRequestMethod("POST");

		// open up the output stream of the connection
		if (!StringUtils.isEmpty(query)) {
			try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
				output.writeBytes(query);
				output.flush();
			}
		}

		Document xmlDocument;
		try {
			xmlDocument = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder()
				.parse(connection.getInputStream());
		} catch (SAXException | ParserConfigurationException e) {
			xmlDocument = null;
			LOGGER.error("An error occured while posting to OpenSubtitles: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return xmlDocument;
	}

	/*
	 * This MUST be called with a lock on tokenLock
	 */
	private static boolean tokenIsYoung() {
		long now = System.currentTimeMillis();
		return ((now - tokenAge) < TOKEN_AGE_TIME);
	}

	private static boolean login() throws IOException {
		tokenLock.writeLock().lock();
		try {
			if (token != null && tokenIsYoung()) {
				return true;
			}

			URL url = new URL(OPENSUBS_URL);
			CredMgr.Cred cred = PMS.getCred("opensubtitles");
			String pwd = "";
			String usr = "";
			if(cred != null) {
				// if we got credentials use them
				if (!StringUtils.isEmpty(cred.password)) {
					pwd = DigestUtils.md5Hex(cred.password);
				}

				usr = cred.username;
			}

			String req =
				"<methodCall>\n" +
					"<methodName>LogIn</methodName>\n" +
					"<params>\n" +
						"<param>\n" +
							"<value>" +
								"<string>" + usr + "</string>" +
							"</value>\n" +
						"</param>\n" +
						"<param>\n" +
							"<value>" +
								"<string>" + pwd + "</string>" +
							"</value>\n" +
						"</param>\n" +
						"<param>\n" +
							"<value><string/></value>\n" +
						"</param>\n" +
						"<param>\n" +
							"<value>" +
								"<string>" + UA + "</string>" +
							"</value>\n" +
						"</param>\n" +
					"</params>\n" +
				"</methodCall>\n";
			Pattern re = Pattern.compile("token.*?<string>([^<]+)</string>", Pattern.DOTALL);
			Matcher m = re.matcher(postPage(url.openConnection(), req));
			if (m.find()) {
				token = m.group(1);
				tokenAge = System.currentTimeMillis();
			}

			return token != null;
		} finally {
			tokenLock.writeLock().unlock();
		}
	}

	public static String fetchImdbId(File f) throws IOException {
		return fetchImdbId(getHash(f));
	}

	public static String fetchImdbId(String hash) throws IOException {
		LOGGER.debug("fetch imdbid for hash " + hash);
		Pattern re = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		String info = checkMovieHash(hash);
		LOGGER.debug("info is " + info);
		Matcher m = re.matcher(info);
		if (m.find()) {
			return m.group(1);
		}

		return "";
	}

	private static String checkMovieHash(String hash) throws IOException {
		if (!login()) {
			return "";
		}

		URL url = new URL(OPENSUBS_URL);
		tokenLock.readLock().lock();
		String req = null;
		try {
			req =
				"<methodCall>\n" +
					"<methodName>CheckMovieHash</methodName>\n" +
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
												"<string>" + hash + "</string>" +
											"</value>\n" +
										"</data>\n" +
									"</array>\n" +
								"</value>\n" +
							"</param>" +
						"</params>\n" +
					"</methodCall>\n";
		} finally {
			tokenLock.readLock().unlock();
		}

		LOGGER.debug("req " + req);
		return postPage(url.openConnection(), req);
	}

	public static String getMovieInfo(File f) throws IOException {
		String info = checkMovieHash(getHash(f));
		if (StringUtils.isEmpty(info)) {
			return "";
		}
		//Pattern re = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		LOGGER.debug("info is {}", info);
		return info;
	}

	public static String getHash(File f) throws IOException {
		LOGGER.debug("get hash of " + f);
		String hash = ImdbUtil.extractOSHash(f);
		if (!StringUtils.isEmpty(hash)) {
			return hash;
		}

		return computeHash(f);
	}

	public static ArrayList<SubtitleItem> findSubs(File f) throws IOException {
		return findSubs(f, null);
	}

	public static ArrayList<SubtitleItem> findSubs(File f, RendererConfiguration r) throws IOException {
		ArrayList<SubtitleItem> res = findSubs(getHash(f), f.length(), null, null, r);
		if (res.isEmpty()) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdb(f);
			if (StringUtils.isEmpty(imdb)) {
				imdb = fetchImdbId(f);
			}

			res = findSubs(null, 0, imdb, null, r);
		}

		if (res.isEmpty()) { // final try, use the name
			res = querySubs(f.getName(), r);
		}

		return res;
	}

	public static ArrayList<SubtitleItem> findSubs(String hash, long size) throws IOException {
		return findSubs(hash, size, null, null, null);
	}

	public static ArrayList<SubtitleItem> findSubs(String imdb) throws IOException {
		return findSubs(null, 0, imdb, null, null);
	}

	public static ArrayList<SubtitleItem> querySubs(String query) throws IOException {
		return querySubs(query, null);
	}

	public static ArrayList<SubtitleItem> querySubs(String query, RendererConfiguration r) throws IOException {
		return findSubs(null, 0, null, query, r);
	}

	private static void logReplyDocument(String logMessage, Document xmlDocument, boolean trace) {
		try {
			if (trace) {
				LOGGER.trace(logMessage, StringUtil.prettifyXML(xmlDocument, 2));
			} else {
				LOGGER.debug(logMessage, StringUtil.prettifyXML(xmlDocument, 2));
			}
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
			LOGGER.error("Couldn't log reply document: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static ArrayList<SubtitleItem> findSubs(String hash, long size, String imdb, String query, RendererConfiguration r) throws IOException {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (!login()) {
			return result;
		}

		String lang = UMSUtils.getLangList(r, true);
		URL url = new URL(OPENSUBS_URL);
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
			return result;
		}

		String req = null;
		tokenLock.readLock().lock();
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
			tokenLock.readLock().unlock();
		}
		LOGGER.trace("Sending query to OpenSubtitles: {}", req);
		Document xmlDocument = postPageXML(url.openConnection(), req);
		if (xmlDocument == null) {
			return result;
		}
		LOGGER.debug("Parsing OpenSubtitles reply");
		XPath xPath = XPathFactory.newInstance().newXPath();
		try {
			NodeList members = (NodeList) xPath.evaluate(
				"/methodResponse/params/param/value/struct/member",
				xmlDocument,
				XPathConstants.NODESET
			);
			int membersLength = members.getLength();
			if (membersLength > 0) {
				XPathExpression nameExpression = xPath.compile("name");
				XPathExpression valueExpression = xPath.compile("value");
				XPathExpression dataValuesExpression = xPath.compile("array/data/value");
				for (int i = 0; i < membersLength; i++) {
					Node name = (Node) nameExpression.evaluate(members.item(i), XPathConstants.NODE);
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", members.item(i));
						return result;
					}
					Node value = (Node) valueExpression.evaluate(members.item(i), XPathConstants.NODE);
					if (value == null || value.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", members.item(i));
						return result;
					}
					String nameString = name.getTextContent();
					if (isNotBlank(nameString)) {
						switch (nameString) {
							case "status":
								Node statusValue = value.getFirstChild();
								if (statusValue == null) {
									LOGGER.error("OpenSubtitles reply status has no value, aborting");
									return result;
								}
								if (!"200 OK".equals(statusValue.getTextContent())) {
									LOGGER.error("OpenSubtitles replied with status \"{}\", aborting", statusValue.getTextContent());
								}
								break;
							case "data":
								NodeList values = (NodeList) dataValuesExpression.evaluate(value, XPathConstants.NODESET);
								if (values != null) {
									int valuesLength = values.getLength();
									for (int j = 0; j < valuesLength; j++) {
										Node dataValueType = values.item(j).getFirstChild();
										if (dataValueType != null) {
											if ("struct".equals(dataValueType.getNodeName())) {
												SubtitleItem item = SubtitleItem.createFromStructNode(dataValueType);
												if (item != null) {
													result.add(item);
												}
											} // If anything other than "struct" is ever available, it should be handled here.
										}
									}
								}
								break;
							default:
								break;
						}
					}
				}
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Received an unexpected reply from OpenSubtitles:");
				logReplyDocument("\n{}", xmlDocument, false);
			}
		} catch (XPathExpressionException e) {
			LOGGER.error("An error occurred while trying to parse the reply from OpenSubtitles: {}", e.getMessage());
			if (LOGGER.isTraceEnabled()) {
				logReplyDocument("Reply:\n{}", xmlDocument, true);
				LOGGER.trace("", e);
			}
		}



//		Element root = xmlDocument.getRootElement();
//		if (root != null && "methodResponse".equals(root.getName())) {
//
//		} else {
//			LOGGER.error(
//				"Received an unexpected reply from OpenSubtitles:\n{}",
//				new XMLOutputter().outputString(xmlDocument)
//			);
//		}


//		Pattern re = Pattern.compile("SubFileName</name>.*?<string>([^<]+)</string>.*?SubLanguageID</name>.*?<string>([^<]+)</string>.*?SubDownloadLink</name>.*?<string>([^<]+)</string>", Pattern.DOTALL);
//		Matcher m = re.matcher(page);
//		while (m.find()) {
//			LOGGER.debug("Found subtitle \"{}\" with language {}: \"{}\"", m.group(2), m.group(1), m.group(3));
//			res.put(m.group(2) + ":" + m.group(1), m.group(3));
//			if (res.size() > PMS.getConfiguration().liveSubtitlesLimit()) {
//				// limit the number of hits somewhat
//				break;
//			}
//		}
		return result;
	}

	/**
	 * Feeds the correct parameters to getInfo below.
	 *
	 * @see #getInfo(java.lang.String, long, java.lang.String, java.lang.String)
	 *
	 * @param f the file to lookup
	 * @param formattedName the name to use in the name search
	 *
	 * @return
	 * @throws IOException
	 */
	public static String[] getInfo(File f, String formattedName) throws IOException {
		return getInfo(f, formattedName, null);
	}

	public static String[] getInfo(File f, String formattedName, RendererConfiguration r) throws IOException {
		String[] res = getInfo(getHash(f), f.length(), null, null, r);
		if (res == null || res.length == 0) { // no good on hash! try imdb
			String imdb = ImdbUtil.extractImdb(f);
			if (StringUtil.hasValue(imdb)) {
				res = getInfo(null, 0, imdb, null, r);
			}
		}

		if (res == null || res.length == 0) { // final try, use the name
			if (StringUtils.isNotEmpty(formattedName)) {
				res = getInfo(null, 0, null, formattedName, r);
			} else {
				res = getInfo(null, 0, null, f.getName(), r);
			}
		}

		return res;
	}

	/**
	 * Attempt to return information from IMDb about the file based on information
	 * from the filename; either the hash, the IMDb ID or the filename itself.
	 *
	 * @param hash  the video hash
	 * @param size  the bytesize to be used with the hash
	 * @param imdb  the IMDb ID
	 * @param query the string to search IMDb for
	 *
	 * @return a string array including the IMDb ID, episode title, season number,
	 *         episode number relative to the season, and the show name, or null
	 *         if we couldn't find it on IMDb.
	 *
	 * @throws IOException
	 */
	private static String[] getInfo(String hash, long size, String imdb, String query, RendererConfiguration r) throws IOException {
		if (!login()) {
			return null;
		}
		String lang = UMSUtils.getLangList(r, true);
		URL url = new URL(OPENSUBS_URL);
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
		tokenLock.readLock().lock();
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
			tokenLock.readLock().unlock();
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

			/**
 			 * Sometimes if OpenSubtitles doesn't have an episode title they call it
 			 * something like "Episode #1.4", so discard that.
 			 */
 			episodeName = StringEscapeUtils.unescapeHtml4(episodeName);
 			if (episodeName.startsWith("Episode #")) {
 				episodeName = "";
 			}

			return new String[]{
				ImdbUtil.ensureTT(m.group(1).trim()),
				episodeName,
				StringEscapeUtils.unescapeHtml4(name),
				m.group(3).trim(), // Season number
				m.group(4).trim(), // Episode number
				m.group(5).trim()  // Year
			};
		}
		return null;
	}

	/**
	 * @deprecated Use {@link #createSubtitlesPath(String)} instead.
	 */
	@Deprecated
	public static String subFile(String name) {
		try {
			Path path = resolveSubtitlesPath(name);
			return path == null ? null : path.toString();
		} catch (IOException e) {
			return null;
		}
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
	 * @deprecated Use {@link #fetchSubs(URI, Path)} instead.
	 */
	@Deprecated
	public static String fetchSubs(String url) throws IOException {
		return fetchSubs(url, null).toString();
	}

	/**
	 * @deprecated Use {@link #fetchSubs(URI, Path)} instead.
	 */
	@Deprecated
	public static Path fetchSubs(String url, Path output) throws IOException {
		if (isBlank(url)) {
			return null;
		}
		try {
			return fetchSubs(new URI(url), output);
		} catch (URISyntaxException e) {
			throw new IOException("Invalid URL \"" + url + "\"", e);
		}
	}

	/**
	 * Downloads the subtitles from the specified {@link URI} to the specified
	 * {@link Path}. It the specifed {@link Path} is {@code null} a temporary
	 * filename is used.
	 *
	 * @param url the {@link URI} from which to download.
	 * @param output the {@link Path} for the target file.
	 * @return {@code null} if {@code url} is {@code null} or OpenSubtitles
	 *         login fails, otherwise the {@link Path} to the downloaded file.
	 * @throws IOException If an error occurs during the operation.
	 */
	public static Path fetchSubs(URI url, Path output) throws IOException {
		if (url == null || !login()) {
			return null;
		}
		if (output == null) {
			output = resolveSubtitlesPath("TempSub" + String.valueOf(System.currentTimeMillis()));
		}
		URLConnection connection = url.toURL().openConnection();
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

	public static class SubtitleItem {
		private final String matchedBy;
		private final String idSubMovieFile;
		private final String idSubtitleFile;
		private final String subFileName;
		private final String subHash;
		private final String idSubtitle;
		private final String languageCode;
		private final SubtitleType subtitleType;
		private final boolean subBad;
		private final double subRating;
		private final String userRank;
		private final String subEncoding;
		private final boolean subFromTrusted;
		private final URI subDownloadLink;
		private final double score;

		public SubtitleItem(
			String matchedBy,
			String idSubMovieFile,
			String idSubtitleFile,
			String subFileName,
			String subHash,
			String idSubtitle,
			String languageCode,
			SubtitleType subtitleType,
			boolean subBad,
			double subRating,
			String userRank,
			String subEncoding,
			boolean subFromTrusted,
			URI subDownloadLink,
			double score
		) {
			this.matchedBy = matchedBy;
			this.idSubMovieFile = idSubMovieFile;
			this.idSubtitleFile = idSubtitleFile;
			this.subFileName = subFileName;
			this.subHash = subHash;
			this.idSubtitle = idSubtitle;
			this.languageCode = languageCode;
			this.subtitleType = subtitleType;
			this.subBad = subBad;
			this.subRating = subRating;
			this.userRank = userRank;
			this.subEncoding = subEncoding;
			this.subFromTrusted = subFromTrusted;
			this.subDownloadLink = subDownloadLink;
			this.score = score;
		}

		protected static double getStringDouble(Node node, String name, XPathExpression stringExpression, XPathMapVariableResolver resolver) {
			resolver.getMap().put("name", name);
			try {
				String result = (String) stringExpression.evaluate(node, XPathConstants.STRING);
				if (isBlank(result)) {
					return Double.NaN;
				}
				try {
					return Double.parseDouble(result);
				} catch (NumberFormatException e) {
					LOGGER.debug("OpenSubtitles: Invalid double value \"{}\" for name \"{}\": {}", result, name, e.getMessage());
					return Double.NaN;
				}
			} catch (XPathExpressionException e) {
				LOGGER.debug("OpenSubtitles: XPath expression error for name \"{}\": {}", name, e.getMessage());
				LOGGER.trace("", e);
				return Double.NaN;
			}
		}

		protected static double getDouble(Node node, String name, XPath xPath, XPathMapVariableResolver resolver) {
			resolver.getMap().put("name", name);
			try {
				return (double) xPath.evaluate("member[name=$name]/value/double", node, XPathConstants.NUMBER);
			} catch (XPathExpressionException e) {
				LOGGER.debug("OpenSubtitles: XPath expression error for name \"{}\": {}", name, e.getMessage());
				LOGGER.trace("", e);
				return Double.NaN;
			}
		}

		protected static boolean getBoolean(Node node, String name, XPathExpression stringExpression, XPathMapVariableResolver resolver) {
			resolver.getMap().put("name", name);
			try {
				String result = (String) stringExpression.evaluate(node, XPathConstants.STRING);
				if (result == null) {
					return false;
				}
				result = result.toLowerCase(Locale.ROOT);
				return  "1".equals(result) || "true".equals(result);
			} catch (XPathExpressionException e) {
				LOGGER.debug("OpenSubtitles: XPath expression error for name \"{}\": {}", name, e.getMessage());
				LOGGER.trace("", e);
				return false;
			}
		}

		protected static String getString(Node node, String name, XPathExpression stringExpression, XPathMapVariableResolver resolver) {
			resolver.getMap().put("name", name);
			try {
				String result = (String) stringExpression.evaluate(node, XPathConstants.STRING);
				return isBlank(result) ? null : result;
			} catch (XPathExpressionException e) {
				LOGGER.debug("OpenSubtitles: XPath expression error for name \"{}\": {}", name, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
		}

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
					LOGGER.debug("Warning, unknown subtitles type \"{}\"", subFormat);
					return SubtitleType.UNKNOWN;
			}
		}

		public static SubtitleItem createFromStructNode(Node structNode) throws XPathExpressionException {
			XPath xPath = XPathFactory.newInstance().newXPath();
			XPathMapVariableResolver resolver = new XPathMapVariableResolver();
			xPath.setXPathVariableResolver(resolver);
			XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");

			String urlString = getString(structNode, "SubDownloadLink", stringExpression, resolver);
			if (isBlank(urlString)) {
				return null;
			}
			URI url;
			try {
				url = new URI(urlString);
			} catch (URISyntaxException e) {
				LOGGER.debug("OpenSubtitles: Invalid URL \"{}\": {}", urlString, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
			String languageCode = getString(structNode, "SubLanguageID", stringExpression, resolver);
			if (Iso639.codeIsValid(languageCode)) {
				languageCode = Iso639.getISO639_2Code(languageCode);
			} else {
				languageCode = getString(structNode, "ISO639", stringExpression, resolver);
				if (Iso639.codeIsValid(languageCode)) {
					languageCode = Iso639.getISO639_2Code(languageCode);
				} else {
					languageCode = DLNAMediaLang.UND;
				}
			}

			return new SubtitleItem(
				getString(structNode, "MatchedBy", stringExpression, resolver),
				getString(structNode, "IDSubMovieFile", stringExpression, resolver),
				getString(structNode, "IDSubtitleFile", stringExpression, resolver),
				getString(structNode, "SubFileName", stringExpression, resolver),
				getString(structNode, "SubHash", stringExpression, resolver),
				getString(structNode, "IDSubtitle", stringExpression, resolver),
				languageCode,
				subFormatToSubtitleType(getString(structNode, "SubFormat", stringExpression, resolver)),
				getBoolean(structNode, "SubBad", stringExpression, resolver),
				getStringDouble(structNode, "SubRating", stringExpression, resolver),
				getString(structNode, "UserRank", stringExpression, resolver),
				getString(structNode, "SubEncoding", stringExpression, resolver),
				getBoolean(structNode, "SubFromTrusted", stringExpression, resolver),
				url,
				getDouble(structNode, "Score", xPath, resolver)
			);
		}

		/**
		 * @return {@code MatchedBy}.
		 */
		public String getMatchedBy() {
			return matchedBy;
		}

		/**
		 * @return {@code IDSubMovieFile}.
		 */
		public String getIDSubMovieFile() {
			return idSubMovieFile;
		}

		/**
		 * @return {@code IDSubtitleFile}.
		 */
		public String getIDSubtitleFile() {
			return idSubtitleFile;
		}

		/**
		 * @return {@code SubFileName}.
		 */
		public String getSubFileName() {
			return subFileName;
		}

		/**
		 * @return {@code SubHash}.
		 */
		public String getSubHash() {
			return subHash;
		}

		/**
		 * @return {@code IDSubtitle}.
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
		 * @return {@code SubBad}.
		 */
		public boolean isSubBad() {
			return subBad;
		}

		/**
		 * @return {@code SubRating}.
		 */
		public double getSubRating() {
			return subRating;
		}

		/**
		 * @return {@code UserRank}.
		 */
		public String getUserRank() {
			return userRank;
		}

		/**
		 * @return {@code SubEncoding}.
		 */
		public String getSubEncoding() {
			return subEncoding;
		}

		/**
		 * @return {@code SubFromTrusted}.
		 */
		public boolean isSubFromTrusted() {
			return subFromTrusted;
		}

		/**
		 * @return {@code SubDownloadLink}.
		 */
		public URI getSubDownloadLink() {
			return subDownloadLink;
		}

		/**
		 * @return {@code Score}.
		 */
		public double getScore() {
			return score;
		}

		@Override
		public String toString() {
			return
				"SubtitleItem [MatchedBy=" + matchedBy + ", IDSubMovieFile=" + idSubMovieFile +
				", IDSubtitleFile=" + idSubtitleFile + ", SubFileName=" + subFileName +
				", SubHash=" + subHash + ", IDSubtitle=" + idSubtitle + ", LanguageCode=" + languageCode +
				", SubFormat=" + subtitleType + ", SubBad=" + subBad + ", SubRating=" + subRating +
				", UserRank=" + userRank + ", SubEncoding=" + subEncoding +
				", SubFromTrusted=" + subFromTrusted + ", SubDownloadLink="+ subDownloadLink +
				", Score=" + score + "]";
		}
	}

	public static class XPathMapVariableResolver implements XPathVariableResolver {

		protected final HashMap<String, Object> variables = new HashMap<>();

		@Override
		public Object resolveVariable(QName variableName) {
			return variables.get(variableName.getLocalPart());
		}

		public HashMap<String, Object> getMap() {
			return variables;
		}
	}
}
