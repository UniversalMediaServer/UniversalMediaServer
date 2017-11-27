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
import java.net.MalformedURLException;
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
import java.util.List;
import java.util.Locale;
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
import net.pms.dlna.DLNAMediaLang;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RealFile;
import net.pms.formats.v2.SubtitleType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OpenSubtitle {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSubtitle.class);
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
	private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
	private static final String SUB_DIR = "subs";
	private static final String UA = "Universal Media Server v1";
	private static final long TOKEN_EXPIRATION_TIME = 10 * 60 * 1000; // 10 mins
	//private static final long SUB_FILE_AGE = 14 * 24 * 60 * 60 * 1000; // two weeks
	public static final Path SUBTITLES_FOLDER = Paths.get(PMS.getConfiguration().getDataFile(SUB_DIR));

	/**
	 * Size of the chunks that will be hashed in bytes (64 KB)
	 */
	private static final int HASH_CHUNK_SIZE = 64 * 1024;

	private static final String OPENSUBS_URL = "http://api.opensubtitles.org/xml-rpc";
	private static final ReentrantReadWriteLock TOKEN_LOCK = new ReentrantReadWriteLock();
	private static volatile Token token = null;

	public static String computeHash(Path file) throws IOException {
		long size = Files.size(file);
		return computeHash(Files.newInputStream(file), size);
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
		connection.setConnectTimeout(5000); //TODO: (Nad) Figure out
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
	 * Posts a query to OpenSubtitles using the specified {@link URLConnection}
	 * and returns the resulting {@link Document}.
	 *
	 * @param connection the {@link URLConnection} to use.
	 * @param query the OpenSubtitles query to post.
	 * @return The resulting {@link Document}.
	 * @throws IOException If an error occurs during the operation.
	 */
	//TODO: (Nad) Remove
	public static Document postPageXMLold(URLConnection connection, String query) throws IOException {
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Content-Type", "text/xml");
		connection.setRequestProperty("Content-Length", "" + query.length());
		((HttpURLConnection) connection).setRequestMethod("POST");

		// open up the output stream of the connection
		if (!StringUtils.isBlank(query)) {
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
	protected static Document postXMLDocument(URL url, Document document) throws IOException {
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

	private static URL getUrl() {
		try {
			return new URL(OPENSUBS_URL);
		} catch (MalformedURLException e) {
			throw new AssertionError("OpenSubtitles URL \"" + OPENSUBS_URL + "\" is invalid");
		}
	}

	/**
	 * Logs in to OpenSubtitles and stores the result in {@link #token}. <b>All
	 * access to {@link #token} must be protected by {@link #TOKEN_LOCK}</b>.
	 *
	 * @param url The API {@link URL} to use for login.
	 * @return {@code true} if the login was a success, {@code false} otherwise.
	 */
	private static boolean login(URL url) {
		if (token != null && token.isYoung()) {
			return true;
		}
		TOKEN_LOCK.writeLock().lock();
		try {
			// Double-checked locking (safe as token is volatile)
			if (token != null && token.isYoung()) {
				return true;
			}
			boolean debug = LOGGER.isDebugEnabled();
			if (debug) {
				LOGGER.debug("Trying to log in to OpenSubtitles");
			}

			CredMgr.Cred credentials = PMS.getCred("opensubtitles");
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
				return false;
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

			try {
				LOGGER.error(StringUtil.prettifyXML(request, 2)); //TODO: (Nad) Temp
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Document response = postXMLDocument(url, request);

			try {
				LOGGER.error(StringUtil.prettifyXML(response, 2)); //TODO: (Nad) Temp
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			token = parseLogIn(response);
			if (token == null) {
				LOGGER.error("Failed to log in to OpenSubtitles");
				return false;
			}
			if (debug) {
				if (token.getUser() != null) {
					//XXX If log anonymization is even implemented, hide the nickname.
					LOGGER.debug("Successfully logged in to OpenSubtitles as {}", token.getUser().getUserNickName());
				} else {
					LOGGER.debug("Successfully logged in to OpenSubtitles anonymously");
				}
			}
			return true;
		} catch (IOException e) {
			LOGGER.error("An error occurred during OpenSubtitles login: {}", e.getMessage());
			LOGGER.trace("", e);
			return false;
		} finally {
			TOKEN_LOCK.writeLock().unlock();
		}
	}

	public static String fetchImdbId(Path file) throws IOException {
		return fetchImdbId(getHash(file));
	}

	public static String fetchImdbId(String hash) throws IOException {
		LOGGER.debug("fetch imdbid for hash " + hash);
		Pattern pattern = Pattern.compile("MovieImdbID.*?<string>([^<]+)</string>", Pattern.DOTALL);
		String info = checkMovieHash(hash);
		LOGGER.debug("info is " + info);
		Matcher m = pattern.matcher(info);
		if (m.find()) {
			return m.group(1);
		}

		return "";
	}

	private static String checkMovieHash(String hash) throws IOException {
		if (!login(getUrl())) { //TODO: (Nad) save URL
			return "";
		}

		URL url = getUrl();
		TOKEN_LOCK.readLock().lock();
		String request = null;
		try {
			request =
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
			TOKEN_LOCK.readLock().unlock();
		}

		if (LOGGER.isTraceEnabled()) {
			String formattedRequest;
			try {
				formattedRequest = StringUtil.prettifyXML(StringEscapeUtils.unescapeXml(request), StandardCharsets.UTF_8, 2);
			} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
				LOGGER.warn("Failed to prettify XML request: {}", e.getMessage());
				LOGGER.trace("", e);
				formattedRequest = request;
			}
			LOGGER.trace("Querying OpenSubtitles for hash {}:\n{}", hash, formattedRequest);
		} else {
			LOGGER.debug("Querying OpenSubtitles for hash {}", hash);
		}
		return postPage(url.openConnection(), request);
	}

	public static String getHash(Path file) throws IOException {
		String hash = ImdbUtil.extractOSHash(file);
		if (isBlank(hash)) {
			hash = computeHash(file);
		}
		LOGGER.debug("OpenSubtitles hash for \"{}\" is {}", file.getFileName(), hash);
		return hash;
	}

	public static ArrayList<SubtitleItem> findSubs(File file) throws IOException {
		return findSubsOld(file, null);
	}

	public static ArrayList<SubtitleItem> querySubs(String query) throws IOException {
		return querySubs(query, null);
	}

	protected static void addParameterString(Document document, Element parent, String name, String value) {
		addParameter(document, parent, name, "string", value);
	}

	protected static void addParameterDouble(Document document, Element parent, String name, double value) {
		addParameter(document, parent, name, "double", Double.toString(value));
	}

	protected static void addParameterInteger(Document document, Element parent, String name, int value) {
		addParameter(document, parent, name, "int", Integer.toString(value));
	}

	// TODO: (Nad) JavaDocs
	protected static void addParameter(Document document, Element parent, String name, String dataType, String value) {
		Element member = document.createElement("member");
		parent.appendChild(member);
		Element nameElement = document.createElement("name");
		member.appendChild(nameElement);
		nameElement.appendChild(document.createTextNode(name));
		Element valueElement = document.createElement("value");
		member.appendChild(valueElement);
		Element string = document.createElement(dataType);
		string.appendChild(document.createTextNode(value));
		valueElement.appendChild(string);

	}

	protected static Element addPath(Document document, Node parent, String tag) {
		Element child = document.createElement(tag);
		parent.appendChild(child);
		return child;
	}

	protected static Element addPath(Document document, Node parent, String[] tags, int tagIdx) {
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
	protected static MethodDocument initializeMethod(URL url, String methodName) {
		if (!login(url)) {
			LOGGER.error("Could not login to OpenSubtitles - please check your credentials");
			return null;
		}
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.error(
				"Couldn't aquire a document builder instance, aborting OpenSubtitles \"{}\" method initialization: {}",
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
			tokenString.appendChild(document.createTextNode(token.getValue()));
		} finally {
			TOKEN_LOCK.readLock().unlock();
		}
		return new MethodDocument(document, params);
	}

	public static Token parseLogIn(Document xmlDocument) {
		if (xmlDocument == null) {
			return null;
		}
		LOGGER.trace("Parsing OpenSubtitles login response");
		String tokenString = null;
		User tokenUser = null;
		XPath xPath = X_PATH_FACTORY.newXPath();
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
				for (int i = 0; i < membersLength; i++) {
					Node name = ((Node) nameExpression.evaluate(members.item(i), XPathConstants.NODE));
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", members.item(i));
						return null;
					}
					// Clone node to avoid extreme slowness when evaluating expressions
					name = name.cloneNode(true);
					Node valueNode = ((Node) valueExpression.evaluate(members.item(i), XPathConstants.NODE));
					if (valueNode == null || valueNode.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", members.item(i));
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
									return null;
								}
								break;
							case "token":
								tokenString = valueNode.getFirstChild().getTextContent();
								break;
							case "data":
								Node dataValueType = valueNode.getFirstChild();
								if (dataValueType != null) {
									// Clone node to avoid extreme slowness when evaluating expressions
									dataValueType = dataValueType.cloneNode(true);
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
				LOGGER.warn("Received an unexpected response from OpenSubtitles:");
				logReplyDocument("\n{}", xmlDocument);
				return null;
			}
		} catch (XPathExpressionException e) {
			LOGGER.error("An error occurred while trying to parse the login response from OpenSubtitles: {}", e.getMessage());
			logReplyDocument("Reply:\n{}", xmlDocument);
			LOGGER.trace("", e);
			return null;
		}
		if (tokenString == null) {
			LOGGER.warn("Failed to parse OpenSubtitles login response");
			logReplyDocument("Reply:\n{}", xmlDocument);
			return null;
		}
		Token result = new Token(tokenString, tokenUser);
		LOGGER.trace("Successfully parsed OpenSubtitles login response. Resulting token: {}", result);
		return result;
	}

	public static ArrayList<SubtitleItem> parseSubtitles(Document xmlDocument) {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (xmlDocument == null) {
			return result;
		}
		XPath xPath = X_PATH_FACTORY.newXPath();
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
					Node name = ((Node) nameExpression.evaluate(members.item(i), XPathConstants.NODE));
					if (name == null || name.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<name> not found in member, aborting: {}", members.item(i));
						return result;
					}
					// Clone node to avoid extreme slowness when evaluating expressions
					name = name.cloneNode(true);
					Node valueNode = ((Node) valueExpression.evaluate(members.item(i), XPathConstants.NODE));
					if (valueNode == null || valueNode.getNodeType() != Node.ELEMENT_NODE) {
						LOGGER.trace("<value> not found in member, aborting: {}", members.item(i));
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
								NodeList values = (NodeList) dataValuesExpression.evaluate(valueNode, XPathConstants.NODESET);
								if (values != null) {
									int valuesLength = values.getLength();
									for (int j = 0; j < valuesLength; j++) {
										Node dataValueType = values.item(j).getFirstChild();
										if (dataValueType != null) {
											// Clone node to avoid extreme slowness when evaluating expressions
											dataValueType = dataValueType.cloneNode(true);
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
			} else {
				LOGGER.warn("Received an unexpected response from OpenSubtitles:");
				logReplyDocument("\n{}", xmlDocument);
			}
		} catch (XPathExpressionException e) {
			LOGGER.error("An error occurred while trying to parse the response from OpenSubtitles: {}", e.getMessage());
			logReplyDocument("Reply:\n{}", xmlDocument);
			LOGGER.trace("", e);
		}
		return result;
	}

	public static ArrayList<SubtitleItem> findSubtitles(DLNAResource resource, RendererConfiguration renderer) {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (resource == null) {
			return new ArrayList<>();
		}

		URL url = getUrl();
		//TODO: (Nad) Login..
		String languageCodes = getLanguageCodes(renderer);
		String primaryLanguageCode = getPrimaryLanguageCode(languageCodes);
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
				result.addAll(findSubtitlesByFileHash(resource, renderer, url, fileHash, fileSize, languageCodes));
				if (isSubtitlesSatisfactory(result, primaryLanguageCode)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Found {} subtitles for \"{}\":\n{}", result.size(), file, toLogString(result));
					} else {
						LOGGER.info("Found {} subtitles for \"{}\"", result.size(), file);
					}
					return result;
				}
			}

			// Query by IMDB id
			String imdbId = ImdbUtil.extractImdbId(file);
			LOGGER.debug("IMDB ID: {}", imdbId);

		}

		return new ArrayList<>(); //TODO: (Nad) Temp
	}

	/**
	 * Queries OpenSubtitles for subtitles matching a file with the specified
	 * hash and size.
	 *
	 * @param resource the {@link DLNAResource} for which subtitles are searched
	 *            for.
	 * @param renderer the current {@link RendererConfiguration}.
	 * @param url the OpenSubtitles {@link URL}.
	 * @param fileHash the file hash.
	 * @param fileSize the file size in bytes.
	 * @param languageCodes the comma separated list of subtitle language codes.
	 * @return A {@link List} with the found {@link SubtitleItem}s (might be
	 *         empty).
	 */
	protected static ArrayList<SubtitleItem> findSubtitlesByFileHash(
		DLNAResource resource,
		RendererConfiguration renderer,
		URL url,
		String fileHash,
		long fileSize,
		String languageCodes
	) {
		MethodDocument methodRequest = initializeMethod(url, "SearchSubtitles");
		if (methodRequest == null) {
			return new ArrayList<>();
		}
		Document request = methodRequest.getDocument();
		Element params = methodRequest.getParams();

		Element struct = addPath(request, params, new String[]{"param", "value", "array", "data", "value", "struct"}, 0);
		addParameterString(request, struct, "moviehash", fileHash);
		addParameterString(request, struct, "moviebytesize", Long.toString(fileSize));
		if (isNotBlank(languageCodes)) {
			addParameterString(request, struct, "sublanguageid", languageCodes);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Querying OpenSubtitles for subtitles for \"{}\" using file hash:\n{}",
				resource.getDisplayName(renderer, false),
				toLogString(request)
			);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
				"Querying OpenSubtitles for subtitles for \"{}\" using file hash {}",
				resource.getDisplayName(renderer, false),
				fileHash
			);
		}

		try {
			Document reply = postXMLDocument(getUrl(), request);
			return parseSubtitles(reply);
		} catch (IOException e) {
			LOGGER.error(
				"An error occurred while processing OpenSubtitles file hash query results for \"{}\": {}",
				resource.getDisplayName(renderer, false),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		return new ArrayList<>();
	}

	private static String toLogString(List<SubtitleItem> subtitleItems) {
		if (subtitleItems == null) {
			return "Null";
		}
		if (subtitleItems.isEmpty()) {
			return "  No matching subtitles";
		}
		StringBuilder sb = new StringBuilder();
		for (SubtitleItem item : subtitleItems) {
			sb.append("  ").append(item).append("\n");
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

	public static ArrayList<SubtitleItem> querySubs(String query, RendererConfiguration renderer) throws IOException {
		return findSubs(null, 0, null, query, renderer);
	}

	public static ArrayList<SubtitleItem> findSubsOld(File file, RendererConfiguration renderer) throws IOException {

		LOGGER.info("Looking for OpenSubtitles subtitles for \"{}\"", file);
		// Query by hash
		ArrayList<SubtitleItem> result = findSubs(getHash(file.toPath()), file.length(), null, null, renderer);

		if (result.isEmpty()) {
		// Query by IMDB id
			String imdbId = ImdbUtil.extractImdbId(file.toPath());
			if (isNotBlank(imdbId)) {
				result = findSubs(null, 0, imdbId, null, renderer);
			}
		}

		if (result.isEmpty()) {
			// Query by name
			result = querySubs(file.getName(), renderer);
		}
		if (result.isEmpty()) {
			LOGGER.info("Found no OpenSubtitles subtitles for \"{}\"", file);
		}
		else if (LOGGER.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (SubtitleItem subtitleItem : result) {
				sb.append(subtitleItem).append("\n");
			}
			LOGGER.info("Found {} OpenSubtitles subtitles for \"{}\":\n{}", result.size(), file, sb.toString());
		} else {
			LOGGER.info("Found {} OpenSubtitles subtitles for \"{}\"", result.size(), file);
		}
		return result;
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
			if (isNotBlank(itemLangaugeCode)) {
				if (languageCode.equals(itemLangaugeCode.trim().toLowerCase(Locale.ROOT))) {
					return true;
				}
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

	public static ArrayList<SubtitleItem> findSubs(
		String hash,
		long size,
		String imdb,
		String query,
		RendererConfiguration renderer
	) throws IOException {
		ArrayList<SubtitleItem> result = new ArrayList<>();
		if (!login(getUrl())) { //TODO: (Nad) save URL
			return result;
		}

		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			documentBuilder = null;
			e.printStackTrace();
		}
		Document document = documentBuilder.newDocument();

		String languageCodes = getLanguageCodes(renderer);
		URL url = getUrl();
		String hashStr = "";
		String imdbStr = "";
		String queryStr = "";
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
			queryStr =
				"<member>" +
					"<name>query</name>" +
					"<value>" +
						"<string>" + query + "</string>" +
					"</value>" +
				"</member>\n";
		} else {
			return result;
		}

		Element methodCall = document.createElement("methodCall");
		document.appendChild(methodCall);

		Element methodName = document.createElement("methodName");
		methodName.appendChild(document.createTextNode("SearchSubtitles"));
		methodCall.appendChild(methodName);
		Element params = document.createElement("params");
		methodCall.appendChild(params);
		Element param = document.createElement("param");
		params.appendChild(param);
		Element value = document.createElement("value");
		param.appendChild(value);
		Element string = document.createElement("string");
		string.appendChild(document.createTextNode(token.getValue()));
		value.appendChild(string);
		Element struct = addPath(document, params, new String[]{"param", "value", "array", "data", "value", "struct"}, 0);
		addParameterString(document, struct, "moviehash", hash);

		try {
			LOGGER.error(StringUtil.prettifyXML(document, 2));
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		postXMLDocument(url, document); //TODO: (Nad) Temp

		String request = null;
		TOKEN_LOCK.readLock().lock();
		try {
			request =
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
														"<string>" + languageCodes + "</string>" +
													"</value>" +
												"</member>" +
													hashStr +
													imdbStr +
													queryStr + "\n" +
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

		String readableQueryArgument = null;
		if (LOGGER.isDebugEnabled()) {
			ArrayList<String> queryArguments = new ArrayList<String>();
			if (!isBlank(hash)) {
				queryArguments.add("hash " + hash);
			}
			if (!isBlank(imdb)) {
				queryArguments.add("IMDB id " + imdb);
			}
			if (!isBlank(query)) {
				queryArguments.add("freetext \"" + query + "\"");
			}
			readableQueryArgument = queryArguments.isEmpty() ? "nothing" : StringUtil.createReadableCombinedString(queryArguments);
			if (LOGGER.isTraceEnabled()) {
				String formattedRequest;
				try {
					formattedRequest = StringUtil.prettifyXML(StringEscapeUtils.unescapeXml(request), StandardCharsets.UTF_8, 2);
				} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
					LOGGER.warn("Failed to prettify XML request: {}", e.getMessage());
					LOGGER.trace("", e);
					formattedRequest = request;
				}
				LOGGER.trace("Querying OpenSubtitles for subtitles using {}:\n{}", readableQueryArgument, formattedRequest);
			} else {
				LOGGER.debug("Querying OpenSubtitles for subtitles using{}", readableQueryArgument);
			}
		}
		Document reply = postPageXMLold(url.openConnection(), request);
		if (reply == null) {
			LOGGER.debug("OpenSubtitles replied with an empty document");
			return result;
		}
		if (LOGGER.isTraceEnabled()) {
			String formattedReply;
			try {
				formattedReply = StringUtil.prettifyXML(reply, 2);
			} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
				LOGGER.warn("Failed to prettify XML reply: {}", e.getMessage());
				LOGGER.trace("", e);
				formattedReply = reply.toString();
			}
			LOGGER.trace(
				"Parsing OpenSubtitles reply for query using {}:\n{}",
				readableQueryArgument,
				formattedReply
			);
		} else {
			LOGGER.debug("Parsing OpenSubtitles reply for query using {}", readableQueryArgument);
		}
		return parseSubtitles(reply);
	}

	private static void logReplyDocument(String logMessage, Document xmlDocument) { //TODO: (Nad) Fix (toLogString())
		try {
			LOGGER.warn(logMessage, StringUtil.prettifyXML(xmlDocument, 2));
		} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
			LOGGER.error("Failed to prettify XML reply {}", e.getMessage());
			LOGGER.trace("", e);
			LOGGER.warn(logMessage, xmlDocument.toString());
		}
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
			String imdb = ImdbUtil.extractImdbId(path);
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
		URL url = getUrl();
		if (!login(url)) {
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
			return fetchSubs(new URL(url), output);
		} catch (MalformedURLException e) {
			throw new IOException("Invalid URL \"" + url + "\"", e);
		}
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
		if (url == null || !login(url)) {
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

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param value the token value {@link String}.
		 * @param user the {@link User} or {@code null}.
		 */
		public Token(String value, User user) {
			this.tokenBirth = System.currentTimeMillis();
			this.value = value;
			this.user = user;
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
		 * returned as an {@link URL}.
		 *
		 * @return The {@link User}'s {@link URL} or {@code null}.
		 */
		public URL getURL() {
			return user == null ? null : user.getContentLocation();
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
		 * @param isVIP {@code true} it the user is a VIP, {@code false}
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
	 * A class holding information about an OpenSubtitles subtitles item.
	 *
	 * @author Nadahar
	 */
	public static class SubtitleItem extends StructElement {
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

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param matchedBy the {@code MatchedBy} {@link String}.
		 * @param idSubMovieFile the {@code IDSubMovieFile} {@link String}.
		 * @param idSubtitleFile the {@code IDSubtitleFile} {@link String}.
		 * @param subFileName the {@code SubFileName} {@link String}.
		 * @param subHash the {@code SubHash} {@link String}.
		 * @param idSubtitle the {@code IDSubtitle} {@link String}.
		 * @param languageCode the ISO 639-2 (3 letter) language code.
		 * @param subtitleType the {@link SubtitleType}.
		 * @param subBad the boolean equivalent of {@code SubBad}.
		 * @param subRating the {@code double} equivalent of {@code SubRating}.
		 * @param userRank the {@code UserRank} {@link String}.
		 * @param subEncoding the {@code SubEncoding} {@link String}.
		 * @param subFromTrusted the boolean equivalent of
		 *            {@code SubFromTrusted}.
		 * @param subDownloadLink the {@link URI} equivalent of
		 *            {@code SubDownloadLink}.
		 * @param score the {@code Score} {@code double}.
		 */
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
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append(" [");
			boolean first = !addFieldToStringBuilder(true, sb, "MatchedBy", matchedBy, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "Score", Double.valueOf(score), false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "IDSubMovieFile", idSubMovieFile, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "IDSubtitleFile", idSubtitleFile, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "SubFileName", subFileName, true, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubHash", subHash, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "IDSubtitle", idSubtitle, false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "LanguageCode", languageCode, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubFormat", subtitleType, false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubBad", Boolean.valueOf(subBad), false, false, true);
			first &= !addFieldToStringBuilder(first, sb, "SubRating", Double.valueOf(subRating), false, false, false);
			first &= !addFieldToStringBuilder(first, sb, "UserRank", userRank, false, false, false);
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
		 * @return The resulting {@link SubtitleItem} or {@code null}.
		 * @throws XPathExpressionException If a {@link XPath} error occurs
		 *             during the operation.
		 */
		public static SubtitleItem createFromStructNode(Node structNode) throws XPathExpressionException {
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
	}

	/**
	 * An abstract class for parsing OpenSubtitles "{@code struct}" elements.
	 *
	 * @author Nadahar
	 */
	public static abstract class StructElement {

		/**
		 * Parses a {@code double} value with the specified name from the
		 * specified {@code string struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param stringExpression the pre-compiled {@link XPathExpression} to
		 *            use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code double} value or {@link Double#NaN} of the
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
		 * Parses a {@code double} value with the specified name from the
		 * specified {@code double struct member} contained in {@link Node}.
		 *
		 * @param node the {@link Node} from which to parse.
		 * @param name the {@code name} value of the {@code member} to parse.
		 * @param xPath the {@link XPath} instance to use.
		 * @param resolver the {@link XPathMapVariableResolver} to use.
		 * @return The parsed {@code double} value or {@link Double#NaN} of the
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
		 * @return The parsed {@code boolean} value or {@code false} of the
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
		 * @return The parsed {@link String} value or {@code null} of the
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
			sb.append(fieldName).append("=");
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

	public static enum HTTPResponseCode {

		/** Successful: OK */
		OK(200, false),

		/** Error: Too many requests (<a href="​http://forum.opensubtitles.org/viewtopic.php?f=8&t=16072">more information</a>) */
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

		public static void handleResponseCode(int responseCode) throws OpenSubtitlesException {
			HTTPResponseCode instance = typeOf(responseCode);
			if (instance == null) {
				throw new OpenSubtitlesException("Unknown response code " + responseCode);
			}
			handleResponseCode(instance);
		}

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

	//TODO: (Nad) Split XML and HTTP status codes; handle
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

		public static void handleStatusCode(int statusCode) throws OpenSubtitlesException {
			StatusCode instance = typeOf(statusCode);
			if (instance == null) {
				throw new OpenSubtitlesException("Unknown OpenSubtitles status code " + statusCode);
			}
			handleStatusCode(instance);
		}

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

	public static class OpenSubtitlesException extends IOException {
		private static final long serialVersionUID = 1L;

		public OpenSubtitlesException(String message, Throwable cause) {
			super(message, cause);
		}

		public OpenSubtitlesException(String message) {
			super(message);
		}
	}
}
