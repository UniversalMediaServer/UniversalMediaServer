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

import static org.apache.commons.lang3.StringUtils.getJaroWinklerDistance;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.*;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;
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
import net.pms.util.OpenSubtitle.MovieGuess.GuessItem;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
	private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
	private static final String SUB_DIR = "subs";
	private static final String UA = "Universal Media Server v1"; //
	private static final long TOKEN_EXPIRATION_TIME = 10 * 60 * 1000; // 10 minutes

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
	 * Posts the specified {@link Document} using the specified
	 * {@link HttpURLConnection}, and returns the reply as another
	 * {@link Document}.
	 *
	 * @param url the HTTP {@link URL} to use.
	 * @param document the {@link Document} to {@code POST}.
	 * @return The reply {@link Document} or {@code null}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	private static Document postXMLDocument(URL url, Document document) throws IOException {
		HTTPResponseCode responseCode = null;
		HttpURLConnection connection = null;
		int retries = 5;
		do {
			URLConnection urlConnection = url.openConnection();
			if (!(urlConnection instanceof HttpURLConnection)) {
				throw new OpenSubtitlesException("Invalid URL: " + url);
			}

			connection = (HttpURLConnection) urlConnection;
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(5000);

			DOMSource source = new DOMSource(document);
			try (OutputStream out = connection.getOutputStream()) {
				StreamResult streamResult = new StreamResult(out);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(source, streamResult);
				out.flush();
			} catch (TransformerFactoryConfigurationError | TransformerException e) {
				LOGGER.error("An error occurred while generating OpenSubtitles XML request: {}", e.getMessage());
				LOGGER.trace("", e);
			}

			responseCode = HTTPResponseCode.typeOf(connection.getResponseCode());
			if (responseCode == null) {
				throw new OpenSubtitlesException(
					"OpenSubtitles replied with an unknown response code: " +
					connection.getResponseCode() + " " +
					connection.getResponseMessage()
				);
			}
			if (responseCode == HTTPResponseCode.SERVICE_UNAVAILABLE) {
				retries--;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return null;
				}
			} else {
				HTTPResponseCode.handleResponseCode(responseCode);
			}
		} while (responseCode == HTTPResponseCode.SERVICE_UNAVAILABLE && retries > -1);

		try {
			return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(connection.getInputStream());
		} catch (SAXException | ParserConfigurationException e) {
			LOGGER.error("An error occurred while posting to OpenSubtitles: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
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
			boolean debug = LOGGER.isDebugEnabled();
			if (debug) {
				LOGGER.debug("Trying to log in to OpenSubtitles");
			}

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

			DocumentBuilder documentBuilder;
			try {
				documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				LOGGER.error(
					"Couldn't aquire a document builder instance for OpenSubtitles login: {}",
					e.getMessage()
				);
				LOGGER.trace("", e);
				return null;
			}

			Document request = documentBuilder.newDocument();
			Element methodCall = addPath(request, request, "methodCall");
			Element methodNameNode = addPath(request, methodCall, "methodName");
			methodNameNode.appendChild(request.createTextNode("LogIn"));
			Element params = addPath(request, methodCall, "params");
			Element usernameElement = addPath(request, params, new String[]{"param", "value", "string"}, 0);
			usernameElement.appendChild(request.createTextNode(username));
			Element passwordElement = addPath(request, params, new String[]{"param", "value", "string"}, 0);
			passwordElement.appendChild(request.createTextNode(pword));
			addPath(request, params, new String[]{"param", "value", "string"}, 0);
			Element userAgent = addPath(request, params, new String[]{"param", "value", "string"}, 0);
			userAgent.appendChild(request.createTextNode(UA));

			URL url;
			try {
				url = new URL(OPENSUBS_URL);
			} catch (MalformedURLException e) {
				throw new AssertionError("OpenSubtitles URL \"" + OPENSUBS_URL + "\" is invalid");
			}

			Document response = postXMLDocument(url, request);

			token = parseLogIn(response, url);
			if (token == null || !token.isValid()) {
				LOGGER.error("Failed to log in to OpenSubtitles");
				LOGGER.trace("The OpenSubtitles login reply was:\n{}", toLogString(response));
				return null;
			}
			if (debug) {
				if (token.getUser() != null) {
					//XXX If log anonymization is ever implemented, hide the nickname.
					LOGGER.debug("Successfully logged in to OpenSubtitles as {}", token.getUser().getUserNickName());
				} else {
					LOGGER.debug("Successfully logged in to OpenSubtitles anonymously");
				}
			}
			return token.getURL();
		} catch (IOException e) {
			LOGGER.error("An error occurred during OpenSubtitles login: {}", e.getMessage());
			LOGGER.trace("", e);
			token = Token.createInvalidToken();
			return null;
		} finally {
			TOKEN_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Adds the specified {@link String} to the specified {@code XML-RPC Struct}
	 * {@link Element} in the specified {@link Document} as a
	 * {@code XML-RPC member}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@code XML-RPC Struct} {@link Element} to add the
	 *            {@code string} to.
	 * @param name the name of the {@code string}.
	 * @param value the {@code string} value.
	 */
	private static void addStructString(Document document, Element parent, String name, String value) {
		addStructMember(document, parent, name, "string", value);
	}

	/**
	 * Adds the specified {@code double} to the specified {@code XML-RPC Struct}
	 * {@link Element} in the specified {@link Document} as a
	 * {@code XML-RPC member}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@code XML-RPC Struct} {@link Element} to add the
	 *            {@code double} to.
	 * @param name the name of the {@code double}.
	 * @param value the {@code double} value.
	 */
	@SuppressWarnings("unused")
	private static void addStructDouble(Document document, Element parent, String name, double value) {
		addStructMember(document, parent, name, "double", Double.toString(value));
	}

	/**
	 * Adds the specified {@code int} to the specified {@code XML-RPC Struct}
	 * {@link Element} in the specified {@link Document} as a
	 * {@code XML-RPC member}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@code XML-RPC Struct} {@link Element} to add the
	 *            {@code int} to.
	 * @param name the name of the {@code int}.
	 * @param value the {@code int} value.
	 */
	private static void addStructInt(Document document, Element parent, String name, int value) {
		addStructMember(document, parent, name, "int", Integer.toString(value));
	}

	/**
	 * Adds the specified {@code member} of the specified data type to the
	 * specified {@code XML-RPC Struct} {@link Element} in the specified
	 * {@link Document}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@code XML-RPC Struct} {@link Element} to add the
	 *            {@code member} to.
	 * @param name the name of the {@code member}.
	 * @param dataType the {@code XML-RPC data type} of the {@code member}.
	 * @param value the {@code member} value as a {@link String}.
	 */
	private static void addStructMember(Document document, Element parent, String name, String dataType, String value) {
		Element member = document.createElement("member");
		parent.appendChild(member);
		addType(document, member, name, dataType, value);
	}

	/**
	 * Adds the specified {@link String} to the specified {@link Element} in the
	 * specified {@link Document} as a {@code XML-RPC string}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Element} to add the {@code string} to.
	 * @param name the name of the {@code string}.
	 * @param value the {@code string} value.
	 */
	private static void addString(Document document, Element parent, String name, String value) {
		addType(document, parent, name, "string", value);
	}

	/**
	 * Adds the specified {@code double} to the specified {@link Element} in the
	 * specified {@link Document} as a {@code XML-RPC double}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Element} to add the {@code double} to.
	 * @param name the name of the {@code double}.
	 * @param value the {@code double} value.
	 */
	@SuppressWarnings("unused")
	private static void addDouble(Document document, Element parent, String name, double value) {
		addType(document, parent, name, "double", Double.toString(value));
	}

	/**
	 * Adds the specified {@code int} to the specified {@link Element} in the
	 * specified {@link Document} as a {@code XML-RPC int}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Element} to add the {@code int} to.
	 * @param name the name of the {@code int}.
	 * @param value the {@code int} value.
	 */
	@SuppressWarnings("unused")
	private static void addInt(Document document, Element parent, String name, int value) {
		addType(document, parent, name, "int", Integer.toString(value));
	}

	/**
	 * Adds the specified {@code XML-RPC data type} to the specified
	 * {@link Element} in the specified {@link Document}.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Element} to add the {@code XML-RPC value} to.
	 * @param name the name of the {@code XML-RPC value}.
	 * @param dataType the {@code XML-RPC data type} of the
	 *            {@code XML-RPC value}.
	 * @param value the {@code XML-RPC value} as a {@link String}.
	 */
	private static void addType(Document document, Element parent, String name, String dataType, String value) {
		if (isNotBlank(name)) {
			Element nameElement = document.createElement("name");
			nameElement.appendChild(document.createTextNode(name));
			parent.appendChild(nameElement);
		}
		Element valueElement = document.createElement("value");
		parent.appendChild(valueElement);
		Element string = document.createElement(dataType);
		string.appendChild(document.createTextNode(value));
		valueElement.appendChild(string);
	}

	/**
	 * Adds a new {@link Node} to the specified {@link Node} with the specified
	 * tag.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Node} to add to.
	 * @param tag the name of the new {@link Node}.
	 * @return the new {@link Node}.
	 */
	private static Element addPath(Document document, Node parent, String tag) {
		Element child = document.createElement(tag);
		parent.appendChild(child);
		return child;
	}

	/**
	 * Adds a path of new {@link Node}s to the specified {@link Node} with the
	 * specified tags recursively.
	 *
	 * @param document the {@link Document}.
	 * @param parent the {@link Node} to add to.
	 * @param tags the array of {@link Node} names with the top of the hierarchy
	 *            first.
	 * @param tagIdx the index of the first tag to add. This is used by the
	 *            recursive calls and should normally be 0 when called
	 *            non-recursively.
	 * @return The last {@link Node} added.
	 */
	private static Element addPath(Document document, Node parent, String[] tags, int tagIdx) {
		if (tags.length - tagIdx < 1 || tagIdx < 0 || tagIdx > tags.length - 1) {
			throw new IllegalArgumentException("tagIdx " + tagIdx + " is invalid for an tag array of length " + tags.length);
		}

		Element child = document.createElement(tags[tagIdx]);
		parent.appendChild(child);
		if (tags.length - tagIdx == 1) {
			return child;
		}

		return addPath(document, child, tags, ++tagIdx);
	}

	/**
	 * Initializes a new {@link Document} for an OpenSubtitles method. It
	 * handles {@link Document} creation, login and builds the basic XML
	 * document structure including the token.
	 * <p>
	 * The returned {@link Document} will look like this:
	 *
	 * <pre>
	 * {@code
	 * <methodCall>
	 *   <methodName>"methodName"</methodName>
	 *   <params>
	 *     <param>
	 *       <value>
	 *         <string>"token"</string>
	 *       </value>
	 *     </param>
	 *   </params>
	 * </methodCall>
	 * }
	 * </pre>
	 *
	 * @param url the {@link URL} to use for login.
	 * @param methodName the name of the OpenSubtitles method.
	 * @return The new {@link MethodDocument} containing both the new
	 *         {@link Document} and the {@code params} {@link Element}.
	 */
	private static MethodDocument initializeMethod(String methodName) {
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.error(
				"Couldn't acquire a document builder instance, aborting OpenSubtitles \"{}\" method initialization: {}",
				methodName,
				e.getMessage()
			);
			LOGGER.trace("", e);
			return null;
		}

		Document document = documentBuilder.newDocument();
		Element methodCall = addPath(document, document, "methodCall");
		Element methodNameNode = addPath(document, methodCall, "methodName");
		methodNameNode.appendChild(document.createTextNode(methodName));
		Element params = addPath(document, methodCall, "params");
		Element tokenString = addPath(document, params, new String[]{"param", "value", "string"}, 0);
		TOKEN_LOCK.readLock().lock();
		try {
			if (token.isValid()) {
				tokenString.appendChild(document.createTextNode(token.getValue()));
			}
		} finally {
			TOKEN_LOCK.readLock().unlock();
		}
		return new MethodDocument(document, params);
	}

	private static Token parseLogIn(Document xmlDocument, URL url) {
		if (xmlDocument == null || url == null) {
			return null;
		}
		LOGGER.trace("Parsing OpenSubtitles login response");
		String tokenString = null;
		User tokenUser = null;
		XPath xPath = X_PATH_FACTORY.newXPath();
		try {
			DeadNodeList members = new DeadNodeList(xPath.evaluate(
				"/methodResponse/params/param/value/struct/member",
				xmlDocument,
				XPathConstants.NODE
			));
			if (!members.isEmpty()) {
				XPathExpression nameExpression = xPath.compile("name");
				XPathExpression valueExpression = xPath.compile("value");
				for (Node member : members) {
					Node name = (Node) nameExpression.evaluate(member, XPathConstants.NODE);
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", member);
						return null;
					}
					Node valueNode = ((Node) valueExpression.evaluate(member, XPathConstants.NODE));
					if (valueNode == null || valueNode.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", member);
						return null;
					}
					String nameString = name.getTextContent();
					if (nameString != null) {
						switch (nameString) {
							case "status":
								Node statusValue = valueNode.getFirstChild();
								if (statusValue == null) {
									LOGGER.error("OpenSubtitles response status has no value, aborting");
									return null;
								}
								StatusCode statusCode = StatusCode.typeOf(statusValue.getTextContent());
								try {
									StatusCode.handleStatusCode(statusCode);
								} catch (OpenSubtitlesException e) {
									LOGGER.error("OpenSubtitles replied with an error, aborting: {}", statusValue.getTextContent());
									LOGGER.trace("", e);
									return Token.createInvalidToken();
								}
								break;
							case "token":
								tokenString = valueNode.getFirstChild().getTextContent();
								break;
							case "data":
								Node dataValueType = valueNode.getFirstChild();
								if (dataValueType != null) {
									if ("struct".equals(dataValueType.getNodeName())) {
										tokenUser = User.createFromStructNode(dataValueType);
									}
								}
								break;
							default:
								break;
						}
					}
				}
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.warn("Received an unexpected response from OpenSubtitles:\n{}", toLogString(xmlDocument));
				} else {
					LOGGER.warn("Received an unexpected response from OpenSubtitles");
				}
				return Token.createInvalidToken();
			}
		} catch (XPathExpressionException e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.error(
					"An error occurred while trying to parse the login response from OpenSubtitles: {}\nReply:\n{}",
					e.getMessage(),
					toLogString(xmlDocument)
				);
				LOGGER.trace("", e);
			} else {
				LOGGER.error(
					"An error occurred while trying to parse the login response from OpenSubtitles: {}",
					e.getMessage()
				);
			}
			return null;
		}
		if (tokenString == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.error("Failed to parse OpenSubtitles login response:\n{}", toLogString(xmlDocument));
			} else {
				LOGGER.error("Failed to parse OpenSubtitles login response");
			}
			return null;
		}
		Token result = new Token(tokenString, tokenUser, url);
		LOGGER.trace("Successfully parsed OpenSubtitles login response. Resulting token: {}", result);
		return result;
	}

	private static ArrayList<SubtitleItem> parseSubtitles(
		Document xmlDocument,
		FileNamePrettifier prettifier,
		DLNAMediaInfo media
	) {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (xmlDocument == null) {
			return result;
		}
		XPath xPath = X_PATH_FACTORY.newXPath();
		try {
			DeadNodeList members = new DeadNodeList(xPath.evaluate(
				"/methodResponse/params/param/value/struct/member",
				xmlDocument,
				XPathConstants.NODE
			));
			if (!members.isEmpty()) {
				XPathExpression nameExpression = xPath.compile("name");
				XPathExpression valueExpression = xPath.compile("value");
				XPathExpression dataValuesExpression = xPath.compile("array/data/value");
				for (Node member : members) {
					Node name = (Node) nameExpression.evaluate(member, XPathConstants.NODE);
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", member);
						return result;
					}
					Node valueNode = ((Node) valueExpression.evaluate(member, XPathConstants.NODE));
					if (valueNode == null || valueNode.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", member);
						return result;
					}
					String nameString = name.getTextContent();
					if (isNotBlank(nameString)) {
						switch (nameString) {
							case "status":
								Node statusValue = valueNode.getFirstChild();
								if (statusValue == null) {
									LOGGER.error("OpenSubtitles response status has no value, aborting");
									return result;
								}
								StatusCode statusCode = StatusCode.typeOf(statusValue.getTextContent());
								try {
									StatusCode.handleStatusCode(statusCode);
								} catch (OpenSubtitlesException e) {
									LOGGER.error("OpenSubtitles replied with an error, aborting: {}", statusValue.getTextContent());
									LOGGER.trace("", e);
								}
								break;
							case "data":
								DeadNodeList values = new DeadNodeList(dataValuesExpression.evaluate(valueNode, XPathConstants.NODE));
								for (Node dataValue : values) {
									Node dataValueType = dataValue.getFirstChild();
									if (dataValueType != null) {
										if ("struct".equals(dataValueType.getNodeName())) {
											SubtitleItem item = SubtitleItem.createFromStructNode(dataValueType, prettifier, media);
											if (item != null) {
												result.add(item);
											}
										} // If anything other than "struct" is ever available, it should be handled here.
									}
								}
								break;
							default:
								break;
						}
					}
				}
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.warn("Received an unexpected response from OpenSubtitles:\n{}", toLogString(xmlDocument));
				} else {
					LOGGER.warn("Received an unexpected response from OpenSubtitles");
				}
			}
		} catch (XPathExpressionException e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.error(
					"An error occurred while trying to parse the response from OpenSubtitles: {}\nReply:\n{}",
					e.getMessage(),
					toLogString(xmlDocument)
				);
				LOGGER.trace("", e);
			} else {
				LOGGER.error(
					"An error occurred while trying to parse the response from OpenSubtitles: {}",
					e.getMessage()
				);
			}
		}
		return result;
	}

	private static Map<String, MovieGuess> parseMovieGuess(Document xmlDocument) {
		Map<String, MovieGuess> result = new HashMap<>();
		if (xmlDocument == null) {
			return result;
		}
		XPath xPath = X_PATH_FACTORY.newXPath();
		try {
			DeadNodeList members = new DeadNodeList(xPath.evaluate(
				"/methodResponse/params/param/value/struct/member",
				xmlDocument,
				XPathConstants.NODE
			));
			if (!members.isEmpty()) {
				XPathExpression nameExpression = xPath.compile("name");
				XPathExpression valueExpression = xPath.compile("value");
				XPathExpression structExpression = xPath.compile("value/struct");
				for (Node member : members) {
					Node name = (Node) nameExpression.evaluate(member, XPathConstants.NODE);
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", member);
						return result;
					}
					Node valueNode = ((Node) valueExpression.evaluate(member, XPathConstants.NODE));
					if (valueNode == null || valueNode.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", member);
						return result;
					}
					String nameString = name.getTextContent();
					if (isNotBlank(nameString)) {
						switch (nameString) {
							case "status":
								Node statusValue = valueNode.getFirstChild();
								if (statusValue == null) {
									LOGGER.error("OpenSubtitles response status has no value, aborting");
									return result;
								}
								StatusCode statusCode = StatusCode.typeOf(statusValue.getTextContent());
								try {
									StatusCode.handleStatusCode(statusCode);
								} catch (OpenSubtitlesException e) {
									LOGGER.error(
										"OpenSubtitles replied with an error, aborting: {}",
										statusValue.getTextContent()
									);
									LOGGER.trace("", e);
								}
								break;
							case "data":
								DeadNodeList dataMembers = new DeadNodeList(xPath.evaluate(
									"struct/member",
									valueNode,
									XPathConstants.NODE
								));
								for (Node dataMember : dataMembers) {
									Node dataName = (Node) nameExpression.evaluate(dataMember, XPathConstants.NODE);
									if (dataName == null) {
										continue;
									}
									String dataNameString = dataName.getTextContent();
									if (isBlank(dataNameString)) {
										continue;
									}

									Node dataStruct = (Node) structExpression.evaluate(dataMember, XPathConstants.NODE);
									if (dataStruct == null) {
										continue;
									}

									result.put(dataNameString, MovieGuess.createFromStructNode(dataStruct));
								}
								break;
							default:
								break;
						}
					}
				}
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.error("Received an unexpected response from OpenSubtitles:\n{}", toLogString(xmlDocument));
				} else {
					LOGGER.error("Received an unexpected response from OpenSubtitles");
				}
			}
		} catch (XPathExpressionException e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.error(
					"An error occurred while trying to parse the response from OpenSubtitles: {}\nReply:\n{}",
					e.getMessage(),
					toLogString(xmlDocument)
				);
				LOGGER.trace("", e);
			} else {
				LOGGER.error(
					"An error occurred while trying to parse the response from OpenSubtitles: {}",
					e.getMessage()
				);
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

			if (!satisfactory) {
				imdbId = ImdbUtil.extractImdbId(file, true);
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
	 * @param resource the {@link DLNAResource} for which subtitles are searched
	 *            for.
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
		URL url = login();
		if (url == null) {
			return new ArrayList<>();
		}
		MethodDocument methodRequest = initializeMethod("SearchSubtitles");
		if (methodRequest == null) {
			return new ArrayList<>();
		}
		Document request = methodRequest.getDocument();
		Element params = methodRequest.getParams();

		Element struct = addPath(request, params, new String[]{"param", "value", "array", "data", "value", "struct"}, 0);
		addStructString(request, struct, "moviehash", fileHash);
		addStructString(request, struct, "moviebytesize", Long.toString(fileSize));
		if (isNotBlank(languageCodes)) {
			addStructString(request, struct, "sublanguageid", languageCodes);
		}
		if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
			addStructInt(request, struct, "season", prettifier.getSeason());
			addStructInt(request, struct, "episode", prettifier.getEpisode());
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Querying OpenSubtitles for subtitles for \"{}\" using file hash:\n{}",
				resource.getName(),
				toLogString(request)
			);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
				"Querying OpenSubtitles for subtitles for \"{}\" using file hash {}",
				resource.getName(),
				fileHash
			);
		}

		try {
			Document reply = postXMLDocument(url, request);
			ArrayList<SubtitleItem> results = parseSubtitles(reply, prettifier, resource.getMedia());
			if (LOGGER.isDebugEnabled()) {
				if (results.isEmpty()) {
					LOGGER.debug(
						"OpenSubtitles search for subtitles for \"{}\" using file hash {} gave no results",
						resource.getName(),
						fileHash
					);
				} else if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Found {} OpenSubtitles subtitles for \"{}\" using file hash {}:\n{}",
						results.size(),
						resource.getName(),
						fileHash,
						toLogString(results, 2)
					);
				} else {
					LOGGER.debug(
						"Found {} OpenSubtitles subtitles for \"{}\" using file hash {}",
						results.size(),
						resource.getName(),
						fileHash
					);
				}
			}
			return results;
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles file hash query results for \"{}\": {}",
				resource.getName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		return new ArrayList<>();
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * IMDB ID.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are searched
	 *            for.
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
		URL url = login();
		if (url == null) {
			return new ArrayList<>();
		}
		MethodDocument methodRequest = initializeMethod("SearchSubtitles");
		if (methodRequest == null) {
			return new ArrayList<>();
		}
		Document request = methodRequest.getDocument();
		Element params = methodRequest.getParams();

		Element struct = addPath(request, params, new String[]{"param", "value", "array", "data", "value", "struct"}, 0);
		addStructString(request, struct, "imdbid", imdbId);
		if (isNotBlank(languageCodes)) {
			addStructString(request, struct, "sublanguageid", languageCodes);
		}
		if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
			addStructInt(request, struct, "season", prettifier.getSeason());
			addStructInt(request, struct, "episode", prettifier.getEpisode());
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Querying OpenSubtitles for subtitles for \"{}\" using IMDB ID:\n{}",
				resource.getName(),
				toLogString(request)
			);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
				"Querying OpenSubtitles for subtitles for \"{}\" using IMDB ID {}",
				resource.getName(),
				imdbId
			);
		}

		try {
			Document reply = postXMLDocument(url, request);
			ArrayList<SubtitleItem> results = parseSubtitles(reply, prettifier, resource.getMedia());
			if (LOGGER.isDebugEnabled()) {
				if (results.isEmpty()) {
					LOGGER.debug(
						"OpenSubtitles search for subtitles for \"{}\" using IMDB ID {} gave no results",
						resource.getName(),
						imdbId
					);
				} else if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Found {} OpenSubtitles subtitles for \"{}\" using IMDB ID {}:\n{}",
						results.size(),
						resource.getName(),
						imdbId,
						toLogString(results, 2)
					);
				} else {
					LOGGER.debug(
						"Found {} OpenSubtitles subtitles for \"{}\" using IMDB ID {}",
						results.size(),
						resource.getName(),
						imdbId
					);
				}
			}
			return results;
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles IMDB ID query results for \"{}\": {}",
				resource.getName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		return new ArrayList<>();
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * name.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are searched
	 *            for.
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
		if (isBlank(fileName) && (prettifier == null || isBlank(prettifier.getName()))) {
			return new ArrayList<>();
		}

		URL url = login();
		if (url == null) {
			return new ArrayList<>();
		}

		MethodDocument methodRequest = initializeMethod("SearchSubtitles");
		if (methodRequest == null) {
			return new ArrayList<>();
		}
		Document request = methodRequest.getDocument();
		Element params = methodRequest.getParams();

		Element data = addPath(request, params, new String[]{"param", "value", "array", "data"}, 0);
		if (isNotBlank(fileName)) {
			Element fileNameStruct = addPath(request, data, new String[]{"value", "struct"}, 0);
			addStructString(request, fileNameStruct, "tag", fileName);
			if (isNotBlank(languageCodes)) {
				addStructString(request, fileNameStruct, "sublanguageid", languageCodes);
			}
			if (prettifier != null && prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
				addStructInt(request, fileNameStruct, "season", prettifier.getSeason());
				addStructInt(request, fileNameStruct, "episode", prettifier.getEpisode());
			}
		}
		if (prettifier != null && isNotBlank(prettifier.getName())) {
			Element nameStruct = addPath(request, data, new String[]{"value", "struct"}, 0);
			addStructString(request, nameStruct, "query", prettifier.getName());
			if (isNotBlank(languageCodes)) {
				addStructString(request, nameStruct, "sublanguageid", languageCodes);
			}
			if (prettifier.getSeason() > 0 && prettifier.getEpisode() > 0) {
				addStructInt(request, nameStruct, "season", prettifier.getSeason());
				addStructInt(request, nameStruct, "episode", prettifier.getEpisode());
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Querying OpenSubtitles for subtitles for \"{}\" using filename:\n{}",
				resource.getName(),
				toLogString(request)
			);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
				"Querying OpenSubtitles for subtitles for \"{}\" using filename {}",
				resource.getName(),
				fileName
			);
		}

		try {
			Document reply = postXMLDocument(url, request);
			ArrayList<SubtitleItem> results = parseSubtitles(reply, prettifier, resource.getMedia());
			if (LOGGER.isDebugEnabled()) {
				if (results.isEmpty()) {
					LOGGER.debug(
						"OpenSubtitles search for subtitles for \"{}\" using filename gave no results",
						resource.getName()
					);
				} else if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Found {} OpenSubtitles subtitles for \"{}\" using filename:\n{}",
						results.size(),
						resource.getName(),
						toLogString(results, 2)
					);
				} else {
					LOGGER.debug(
						"Found {} OpenSubtitles subtitles for \"{}\" using filename",
						results.size(),
						resource.getName()
					);
				}
			}
			return results;
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles filename query results for \"{}\": {}",
				resource.getName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		return new ArrayList<>();
	}

	private static void addGuesses(
		Map<Double, GuessItem> candidates,
		Collection<? extends GuessItem> guesses,
		FileNamePrettifier prettifier,
		VideoClassification classification,
		boolean bestGuess
	) {
		// The score calculation isn't extensively tested and might need to be tweaked
		for (GuessItem guess : guesses) {
			double score = 0.0;
			if (prettifier.getYear() > 0) {
				int guessYear = StringUtil.getYear(guess.getYear());
				if (prettifier.getYear() == guessYear) {
					score += 0.5;
				}
			}
			if (classification != null && classification == guess.getVideoClassification()) {
				score += 0.7;
			}
			if (isNotBlank(prettifier.getName()) && isNotBlank(guess.getTitle())) {
				score += getJaroWinklerDistance(prettifier.getName(), guess.getTitle());
			}
			if (bestGuess) {
				score += 0.3;
			}
			candidates.put(score, guess);
		}
	}

	/**
	 * Queries OpenSubtitles for IMDB IDs matching a filename.
	 *
	 * @param resource the {@link DLNAResource} for which to find the IMDB ID.
	 * @param prettifier the {@link FileNamePrettifier} to use.
	 * @return The IMDB ID or {@code null}.
	 */
	protected static String guessImdbIdByFileName(
		DLNAResource resource,
		FileNamePrettifier prettifier
	) {
		if (resource == null) {
			return null;
		}
		String fileName;
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
		MethodDocument methodRequest = initializeMethod("GuessMovieFromString");
		if (methodRequest == null) {
			return null;
		}
		URL url = login();
		if (url == null) {
			return null;
		}
		Document request = methodRequest.getDocument();
		Element params = methodRequest.getParams();

		Element data = addPath(request, params, new String[]{"param", "value", "array", "data"}, 0);
		addString(request, data, null, fileName);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Querying OpenSubtitles for IMDB ID for \"{}\":\n{}",
				fileName,
				toLogString(request)
			);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
				"Querying OpenSubtitles for IMDB ID for \"{}\"",
				fileName
			);
		}

		Map<String, MovieGuess> movieGuesses;
		try {
			Document reply = postXMLDocument(url, request);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("OpenSubtitles Reply:\n{}", toLogString(reply));
			}
			movieGuesses = parseMovieGuess(reply);
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles file hash query results for \"{}\": {}",
				resource.getName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
			return null;
		}

		MovieGuess movieGuess = movieGuesses.get(fileName);
		if (movieGuess != null) {
			VideoClassification classification;
			if (
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

			TreeMap<Double, GuessItem> candidates = new TreeMap<>();
			if (movieGuess.getGuessesFromString().size() > 0) {
				addGuesses(candidates, movieGuess.getGuessesFromString().values(), prettifier, classification, false);
			}
			if (movieGuess.getImdbSuggestions().size() > 0) {
				addGuesses(candidates, movieGuess.getImdbSuggestions().values(), prettifier, classification, false);
			}
			if (movieGuess.getBestGuess() != null) {
				addGuesses(candidates, Collections.singletonList(movieGuess.getBestGuess()), prettifier, classification, true);
			}
			if (candidates.size() > 0) {
				if (LOGGER.isTraceEnabled()) {
					StringBuilder sb = new StringBuilder();
					for (Entry<Double, GuessItem> entry : candidates.entrySet()) {
						sb.append("  Score: ").append(entry.getKey())
						.append(", Candidate: ").append(entry.getValue()).append("\n");
					}
					LOGGER.trace(
						"guessImdbIdByFileName candidates for \"{}\":\n{}",
						resource.getName(),
						sb.toString()
					);
				}
				LOGGER.debug(
					"guessImdbIdByFileName() picked {} as the most likely candidate for \"{}\"",
					candidates.lastEntry().getValue(),
					resource.getName()
				);
				return candidates.lastEntry().getValue().getImdbId();
			}
		}

		LOGGER.debug("guessImdbIdByFileName() failed to find a candidate for \"{}\"", resource.getName());
		return null;
	}

	/**
	 * Creates a {@link String} where each {@link SubtitleItem} in
	 * {@code subtitleItems} is on its own line and indented with the specified
	 * number of spaces.
	 *
	 * @param subtitleItems the {@link List} of {@link SubtitleItem}s to format
	 *            for logging.
	 * @param indent the number of leading spaces on each line.
	 * @return The log friendly {@link String}.
	 */
	public static String toLogString(List<SubtitleItem> subtitleItems, int indent) {
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

	private static String toLogString(Document xmlDocument) {
		try {
			return StringUtil.prettifyXML(xmlDocument, 2);
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
			LOGGER.error("Failed to prettify XML reply {}", e.getMessage());
			LOGGER.trace("", e);
			return "Unable to pretty print XML document: " + e.getMessage();
		}
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
				return user.getContentLocation();
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
	public static class User extends StructElement {
		private final String idUser;
		private final String userNickName;
		private final String userRank;
		private final String[] userPreferredLanguages;
		private final boolean isVIP;
		private final URL contentLocation;

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
			URL contentLocation
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
		public URL getContentLocation() {
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
		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append(" [");
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

		/**
		 * Parses an OpenSubtitles {@code struct} node describing a user and
		 * returns the resulting {@link User}.
		 *
		 * @param structNode the {@link Node} containing the information about
		 *            the user.
		 * @return The resulting {@link User} or {@code null}.
		 * @throws XPathExpressionException If a {@link XPath} error occurs
		 *             during the operation.
		 */
		public static User createFromStructNode(Node structNode) throws XPathExpressionException {
			XPath xPath = X_PATH_FACTORY.newXPath();
			XPathMapVariableResolver resolver = new XPathMapVariableResolver();
			xPath.setXPathVariableResolver(resolver);
			XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");

			String urlString = getString(structNode, "Content-Location", stringExpression, resolver);
			URL url = null;
			if (isNotBlank(urlString)) {
				try {
					urlString = urlString.replaceFirst("https://", "http://");
					url = new URL(urlString);
				} catch (MalformedURLException e) {
					LOGGER.debug("OpenSubtitles: Ignoring invalid URL \"{}\": {}", urlString, e.getMessage());
					LOGGER.trace("", e);
				}
			}
			return new User(
				getString(structNode, "IDUser", stringExpression, resolver),
				getString(structNode, "UserNickName", stringExpression, resolver),
				getString(structNode, "UserRank", stringExpression, resolver),
				getString(structNode, "UserPreferedLanguages", stringExpression, resolver),
				getBoolean(structNode, "IsVIP", stringExpression, resolver),
				url
			);
		}
	}

	/**
	 * A class representing an OpenSubtitles {@code GuessMovieFromString}
	 * result.
	 *
	 * @author Nadahar
	 */
	public static class MovieGuess extends StructElement {
		private final GuessIt guessIt;
		private final Map<String, GuessFromString> guessesFromString;
		private final Map<String, GuessItem> imdbSuggestions;
		private final BestGuess bestGuess;

		/**
		 * A class representing an OpenSubtitles {@code GuessIt} structure.
		 */
		public static class GuessIt extends StructElement {
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

			@Override
			public String toString(boolean includeName) {
				StringBuilder sb = new StringBuilder();
				if (includeName) {
					sb.append(getClass().getSimpleName()).append(" [");
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
					sb.append(getClass().getSimpleName()).append(" [");
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

			/**
			 * Parses an OpenSubtitles {@code struct} node describing a
			 * {@code GuessMovieFromString} data structure and returns the
			 * resulting {@link List} of {@link GuessFromString}s.
			 *
			 * @param structNode the {@link Node} containing the
			 *            {@code GuessMovieFromString} data structure.
			 * @return The resulting {@link GuessFromString}s or an empty
			 *         {@link Map}.
			 * @throws XPathExpressionException If a {@link XPath} error occurs
			 *             during the operation.
			 */
			public static Map<String, GuessFromString> createGuessesFromStringFromStructNode(
				Node structNode
			) throws XPathExpressionException {
				Map<String, GuessFromString> result = new HashMap<>();
				XPath xPath = X_PATH_FACTORY.newXPath();
				XPathMapVariableResolver resolver = new XPathMapVariableResolver();
				xPath.setXPathVariableResolver(resolver);
				XPathExpression structExpression = xPath.compile("value/struct");
				XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");
				XPathExpression nameExpression = xPath.compile("name");

				DeadNodeList members = new DeadNodeList(xPath.evaluate("member", structNode, XPathConstants.NODE));
				for (Node member : members) {
					String name = (String) nameExpression.evaluate(member, XPathConstants.STRING);
					Node memberStruct = (Node) structExpression.evaluate(member, XPathConstants.NODE);
					if (memberStruct == null || isBlank(name)) {
						continue;
					}
					result.put(name, new GuessFromString(
						getString(memberStruct, "MovieName", stringExpression, resolver),
						getString(memberStruct, "MovieYear", stringExpression, resolver),
						getString(memberStruct, "MovieKind", stringExpression, resolver),
						getString(memberStruct, "IDMovieIMDB", stringExpression, resolver),
						getString(memberStruct, "score", stringExpression, resolver)
					));
				}
				return result;
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
					sb.append(getClass().getSimpleName()).append(" [");
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

			/**
			 * Parses an OpenSubtitles {@code struct} node describing a
			 * {@code BestGuess} data structure and returns the resulting
			 * {@link BestGuess}.
			 *
			 * @param structNode the {@link Node} containing the
			 *            {@code BestGuess} data structure.
			 * @return The resulting {@link BestGuess} or {@code null}.
			 * @throws XPathExpressionException If a {@link XPath} error occurs
			 *             during the operation.
			 */
			public static BestGuess createBestGuessFromStructNode(Node structNode) throws XPathExpressionException {
				XPath xPath = X_PATH_FACTORY.newXPath();
				XPathMapVariableResolver resolver = new XPathMapVariableResolver();
				xPath.setXPathVariableResolver(resolver);
				XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");

				if (structNode != null) {
					return new BestGuess(
						getString(structNode, "MovieName", stringExpression, resolver),
						getString(structNode, "MovieYear", stringExpression, resolver),
						getString(structNode, "MovieKind", stringExpression, resolver),
						getString(structNode, "IDMovieIMDB", stringExpression, resolver),
						getString(structNode, "Reason", stringExpression, resolver)
					);
				}
				return null;
			}
		}

		/**
		 * A class representing an OpenSubtitles guess item structure.
		 */
		public static class GuessItem extends StructElement {

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

			@Override
			public String toString(boolean includeName) {
				StringBuilder sb = new StringBuilder();
				if (includeName) {
					sb.append(getClass().getSimpleName()).append(" [");
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

			/**
			 * Parses an OpenSubtitles {@code struct} node describing a
			 * {@code GetIMDBSuggest} data structure and returns the resulting
			 * {@link List} of {@link GuessItem}s.
			 *
			 * @param structNode the {@link Node} containing the
			 *            {@code GetIMDBSuggest} data structure.
			 * @return The resulting {@link GuessItem}s or an empty {@link Map}.
			 * @throws XPathExpressionException If a {@link XPath} error occurs
			 *             during the operation.
			 */
			public static Map<String, GuessItem> createFromStructNode(Node structNode) throws XPathExpressionException {
				Map<String, GuessItem> result = new HashMap<>();
				XPath xPath = X_PATH_FACTORY.newXPath();
				XPathMapVariableResolver resolver = new XPathMapVariableResolver();
				xPath.setXPathVariableResolver(resolver);
				XPathExpression structExpression = xPath.compile("value/struct");
				XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");
				XPathExpression nameExpression = xPath.compile("name");

				DeadNodeList members = new DeadNodeList(xPath.evaluate("member", structNode, XPathConstants.NODE));
				for (Node member : members) {
					String name = (String) nameExpression.evaluate(member, XPathConstants.STRING);
					Node memberStruct = (Node) structExpression.evaluate(member, XPathConstants.NODE);
					if (memberStruct == null || isBlank(name)) {
						continue;
					}
					result.put(name, new GuessItem(
						getString(memberStruct, "MovieName", stringExpression, resolver),
						getString(memberStruct, "MovieYear", stringExpression, resolver),
						getString(memberStruct, "MovieKind", stringExpression, resolver),
						getString(memberStruct, "IDMovieIMDB", stringExpression, resolver)
					));
				}
				return result;
			}
		}

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
		 * @return The {@link GuessIt}.
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
		 * @return The {@link BestGuess}.
		 */
		public BestGuess getBestGuess() {
			return bestGuess;
		}

		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append(" [");
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
				first &= !addFieldToStringBuilder(first, sb, "GuessesFromString", innerSB, false, false, true);
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
				first &= !addFieldToStringBuilder(first, sb, "IMDBSuggestions", innerSB, false, false, true);
			}
			if (bestGuess != null) {
				addFieldToStringBuilder(first, sb, "BestGuess", bestGuess.toString(false), false, false, true);
			}
			sb.append("]");
			return sb.toString();
		}

		/**
		 * Parses an OpenSubtitles {@code struct} node describing a
		 * {@code GuessMovieFromString} result and returns the resulting
		 * {@link MovieGuess}.
		 *
		 * @param structNode the {@link Node} containing the
		 *            {@code GuessMovieFromString} data.
		 * @return The resulting {@link MovieGuess} or {@code null}.
		 * @throws XPathExpressionException If a {@link XPath} error occurs
		 *             during the operation.
		 */
		public static MovieGuess createFromStructNode(Node structNode) throws XPathExpressionException {
			XPath xPath = X_PATH_FACTORY.newXPath();
			XPathMapVariableResolver resolver = new XPathMapVariableResolver();
			xPath.setXPathVariableResolver(resolver);
			XPathExpression structExpression = xPath.compile("member[name=$name]/value/struct");
			XPathExpression stringExpression = xPath.compile("member[name=$name]/value/string");

			GuessIt guessIt = null;
			resolver.getMap().put("name", "GuessIt");
			Node guessItStruct = (Node) structExpression.evaluate(structNode, XPathConstants.NODE);
			if (guessItStruct != null) {
				guessItStruct = guessItStruct.cloneNode(true);
				guessIt = new GuessIt(
					getString(guessItStruct, "mimetype", stringExpression, resolver),
					getString(guessItStruct, "videoCodec", stringExpression, resolver),
					getString(guessItStruct, "container", stringExpression, resolver),
					getString(guessItStruct, "title", stringExpression, resolver),
					getString(guessItStruct, "format", stringExpression, resolver),
					getString(guessItStruct, "releaseGroup", stringExpression, resolver),
					getString(guessItStruct, "screenSize", stringExpression, resolver),
					getString(guessItStruct, "type", stringExpression, resolver),
					getString(guessItStruct, "audioCodec", stringExpression, resolver)
				);
			}

			Map<String, GuessFromString> guessesFromString;
			resolver.getMap().put("name", "GuessMovieFromString");
			Node guessesFromStringStruct = (Node) structExpression.evaluate(structNode, XPathConstants.NODE);
			if (guessesFromStringStruct != null) {
				guessesFromStringStruct = guessesFromStringStruct.cloneNode(true);
				guessesFromString = GuessFromString.createGuessesFromStringFromStructNode(guessesFromStringStruct);
			} else {
				guessesFromString = new HashMap<>();
			}

			Map<String, GuessItem> imdbSuggestions;
			resolver.getMap().put("name", "GetIMDBSuggest");
			Node imdbSuggestionsStruct = (Node) structExpression.evaluate(structNode, XPathConstants.NODE);
			if (imdbSuggestionsStruct != null) {
				imdbSuggestionsStruct = imdbSuggestionsStruct.cloneNode(true);
				imdbSuggestions = GuessFromString.createFromStructNode(imdbSuggestionsStruct);
			} else {
				imdbSuggestions = new HashMap<>();
			}

			BestGuess bestGuess = null;
			resolver.getMap().put("name", "BestGuess");
			Node bestGuessStruct = (Node) structExpression.evaluate(structNode, XPathConstants.NODE);
			if (bestGuessStruct != null) {
				bestGuess = BestGuess.createBestGuessFromStructNode(bestGuessStruct.cloneNode(true));
			}

			return new MovieGuess(guessIt, guessesFromString, imdbSuggestions, bestGuess);
		}
	}

	/**
	 * A class holding information about an OpenSubtitles subtitles item.
	 *
	 * @author Nadahar
	 */
	public static class SubtitleItem extends StructElement {
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
				if (isNotBlank(prettifier.getFileNameWithoutExtension())) {
					String subFileNameWithoutExtension = FileUtil.getFileNameWithoutExtension(subFileName);
					if (isNotBlank(subFileNameWithoutExtension)) {
						// 0.6 and below gives a score of 0, 1.0 give a score of 40.
						tmpScore += 40d * 2.5 * Math.max(
							getJaroWinklerDistance(prettifier.getFileNameWithoutExtension(), subFileNameWithoutExtension) - 0.6,
							0
						);
					}
				}
				if (isNotBlank(prettifier.getName()) && (isNotBlank(movieName) || isNotBlank(movieNameEng))) {
					double nameScore = isBlank(movieName) ?
						0.0 :
						getJaroWinklerDistance(prettifier.getName(), movieName);
					nameScore = Math.max(
						nameScore,
						isBlank(movieNameEng) ? 0.0 : getJaroWinklerDistance(prettifier.getName(), movieNameEng)
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
					LOGGER.debug("Warning, unknown subtitles type \"{}\"", subFormat);
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

		@Override
		public String toString(boolean includeName) {
			StringBuilder sb = new StringBuilder();
			if (includeName) {
				sb.append(getClass().getSimpleName()).append(" [");
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

		/**
		 * Parses a OpenSubtitles {@code struct} node describing a set of
		 * subtitles and returns the resulting {@link SubtitleItem}.
		 *
		 * @param structNode the {@link Node} containing the information about
		 *            the set of subtitles.
		 * @param prettifier the {@link FileNamePrettifier} for the video item.
		 * @param media the {@link DLNAMediaInfo} instance for the video.
		 * @return The resulting {@link SubtitleItem} or {@code null}.
		 * @throws XPathExpressionException If a {@link XPath} error occurs
		 *             during the operation.
		 */
		public static SubtitleItem createFromStructNode(
			Node structNode,
			FileNamePrettifier prettifier,
			DLNAMediaInfo media
		) throws XPathExpressionException {
			XPath xPath = X_PATH_FACTORY.newXPath();
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

			long lastTS;
			String lastTSString = getString(structNode, "SubLastTS", stringExpression, resolver);
			try {
				lastTS = new SimpleDateFormat("HH:mm:ss").parse(lastTSString).getTime();
			} catch (java.text.ParseException e) {
				try {
					lastTS = Long.parseLong(lastTSString);
				} catch (NumberFormatException nfe) {
					lastTS = -1;
				}
			}
			return new SubtitleItem(
				getString(structNode, "MatchedBy", stringExpression, resolver),
				getString(structNode, "IDSubtitleFile", stringExpression, resolver),
				getString(structNode, "SubFileName", stringExpression, resolver),
				getString(structNode, "SubHash", stringExpression, resolver),
				lastTS,
				getString(structNode, "IDSubtitle", stringExpression, resolver),
				languageCode,
				subFormatToSubtitleType(getString(structNode, "SubFormat", stringExpression, resolver)),
				getBoolean(structNode, "SubBad", stringExpression, resolver),
				getStringDouble(structNode, "SubRating", stringExpression, resolver),
				getStringInt(structNode, "SubDownloadsCnt", stringExpression, resolver),
				getString(structNode, "MovieFPS", stringExpression, resolver),
				getString(structNode, "IDMovieImdb", stringExpression, resolver),
				getString(structNode, "MovieName", stringExpression, resolver),
				getString(structNode, "MovieNameEng", stringExpression, resolver),
				getStringInt(structNode, "MovieYear", stringExpression, resolver),
				getString(structNode, "UserRank", stringExpression, resolver),
				getStringInt(structNode, "SeriesSeason", stringExpression, resolver),
				getStringInt(structNode, "SeriesEpisode", stringExpression, resolver),
				VideoClassification.typeOf(getString(structNode, "MovieKind", stringExpression, resolver)),
				getString(structNode, "SubEncoding", stringExpression, resolver),
				getBoolean(structNode, "SubFromTrusted", stringExpression, resolver),
				url,
				getDouble(structNode, "Score", xPath, resolver),
				prettifier,
				media
			);
		}
	}

	/**
	 * An abstract class for parsing OpenSubtitles "{@code struct}" elements.
	 *
	 * @author Nadahar
	 */
	public abstract static class StructElement {

		/**
		 * Parses a {@code double} value with the specified name from the
		 * specified {@code string struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param stringExpression the pre-compiled {@link XPathExpression} to
		 *            use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code double} value or {@link Double#NaN} if the
		 *         parsing failed.
		 */
		protected static double getStringDouble(
			Node node,
			String name,
			XPathExpression stringExpression,
			XPathMapVariableResolver resolver
		) {
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

		/**
		 * Parses a {@code int} value with the specified name from the specified
		 * {@code string struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param stringExpression the pre-compiled {@link XPathExpression} to
		 *            use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code int} value or {@code -1} if the parsing
		 *         failed.
		 */
		protected static int getStringInt(
			Node node,
			String name,
			XPathExpression stringExpression,
			XPathMapVariableResolver resolver
		) {
			resolver.getMap().put("name", name);
			try {
				String result = (String) stringExpression.evaluate(node, XPathConstants.STRING);
				if (isBlank(result)) {
					return -1;
				}
				try {
					return Integer.parseInt(result);
				} catch (NumberFormatException e) {
					LOGGER.debug("OpenSubtitles: Invalid int value \"{}\" for name \"{}\": {}", result, name, e.getMessage());
					return -1;
				}
			} catch (XPathExpressionException e) {
				LOGGER.debug("OpenSubtitles: XPath expression error for name \"{}\": {}", name, e.getMessage());
				LOGGER.trace("", e);
				return -1;
			}
		}

		/**
		 * Parses a {@code double} value with the specified name from the
		 * specified {@code double struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param xPath the {@link XPath} instance to use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code double} value or {@link Double#NaN} if the
		 *         parsing failed.
		 */
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

		/**
		 * Parses a {@code boolean} value with the specified name from the
		 * specified {@code string struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param stringExpression the pre-compiled {@link XPathExpression} to
		 *            use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code boolean} value or {@code false} if the
		 *         parsing failed.
		 */
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

		/**
		 * Parses a {@link String} value with the specified name from the
		 * specified {@code string struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param stringExpression the pre-compiled {@link XPathExpression} to
		 *            use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@link String} value or {@code null} if the
		 *         parsing failed.
		 */
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
		protected boolean addFieldToStringBuilder(
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
		public abstract String toString(boolean includeName);
	}

	/**
	 * A {@link XPathVariableResolver} implementation that gets the value to use
	 * from a {@link HashMap} by using the value of the key with the
	 * corresponding variable name.
	 *
	 * @author Nadahar
	 */
	public static class XPathMapVariableResolver implements XPathVariableResolver {

		/** The {@link HashMap} containing the variable name and value pairs */
		protected final HashMap<String, Object> variables = new HashMap<>();

		@Override
		public Object resolveVariable(QName variableName) {
			return variables.get(variableName.getLocalPart());
		}

		/**
		 * @return the {@link HashMap} containing the variable name and value
		 *         pairs used when resolving variables.
		 */
		public HashMap<String, Object> getMap() {
			return variables;
		}
	}

	private static class MethodDocument {
		private final Document document;
		private final Element params;

		/**
		 * Creates a new instance with the specified parameters.
		 *
		 * @param document the {@link Document}.
		 * @param params the {@code params} {@link Element}.
		 */
		public MethodDocument(Document document, Element params) {
			this.document = document;
			this.params = params;
		}

		/**
		 * @return The {@link Document}.
		 */
		public Document getDocument() {
			return document;
		}

		/**
		 * @return The {@code params} {@link Element}.
		 */
		public Element getParams() {
			return params;
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
		SERVICE_UNAVAILABLE(503, true);

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
			switch (responseCode) {
				case OK:
					return;
				case SERVICE_UNAVAILABLE:
				case TOO_MANY_REQUESTS:
					throw new OpenSubtitlesException(responseCode.toString());
				default:
					throw new AssertionError("Unimplemented responseCode \"" + responseCode + "\"");
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
