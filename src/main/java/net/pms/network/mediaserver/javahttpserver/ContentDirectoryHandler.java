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
package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DidlHelper;
import net.pms.dlna.protocolinfo.PanasonicDmpProfiles;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.handlers.message.BrowseRequest;
import net.pms.network.mediaserver.handlers.message.BrowseSearchRequest;
import net.pms.network.mediaserver.handlers.message.SamsungBookmark;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStatusStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.MediaLibrary;
import net.pms.store.container.PlaylistFolder;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This is the UPnP ContentDirectory http server.
 *
 * It should be replaced by the JUPnP one.
 */
public class ContentDirectoryHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryHandler.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final SimpleDateFormat SDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	private static final String GET = "GET";
	private static final String HEAD = "HEAD";
	private static final String POST = "POST";
	private static final String SUBSCRIBE = "SUBSCRIBE";
	private static final String NOTIFY = "NOTIFY";
	private static final String CRLF = "\r\n";
	private static final String CONTENT_TYPE_XML_UTF8 = "text/xml; charset=\"utf-8\"";
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final Pattern DIDL_PATTERN = Pattern.compile("<Result>(&lt;DIDL-Lite.*?)</Result>");

	private static final String HTTPSERVER_REQUEST_BEGIN = "================================== UPNPSERVER REQUEST BEGIN =====================================";
	private static final String HTTPSERVER_REQUEST_END = "================================== UPNPSERVER REQUEST END =======================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "================================== UPNPSERVER RESPONSE BEGIN ====================================";
	private static final String HTTPSERVER_RESPONSE_END = "================================== UPNPSERVER RESPONSE END ======================================";

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		Renderer renderer = null;
		try {
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			String userAgentString = exchange.getRequestHeaders().getFirst("User-Agent");
			// Is the request from our own JUPnP service, i.e. self-originating?
			boolean isSelf = ia.getHostAddress().equals(MediaServer.getHost()) &&
					userAgentString != null &&
					userAgentString.contains("UMS/");
			// Filter if required
			if (isSelf || !NetworkDeviceFilter.isAllowed(ia)) {
				exchange.close();
				return;
			}

			Collection<Map.Entry<String, String>> headers = getHeaders(exchange);
			renderer = ConnectedRenderers.getRenderer(ia, userAgentString, headers);
			if (renderer == null) {
				// If RendererConfiguration.resolve() didn't return the default renderer
				// it means we know via upnp that it's not really a renderer.
				exchange.close();
				return;
			}

			if (!renderer.isAllowed()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
				}
				return;
			}

			if (exchange.getRequestHeaders().containsKey("X-PANASONIC-DMP-Profile")) {
				PanasonicDmpProfiles.parsePanasonicDmpProfiles(exchange.getRequestHeaders().getFirst("X-PANASONIC-DMP-Profile"), renderer);
			}

			String requestBody = null;
			if (exchange.getRequestHeaders().containsKey("Content-Length")) {
				int contentLength = 0;
				try {
					contentLength = Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));
				} catch (NumberFormatException e) {
				}
				if (contentLength > 0) {
					byte[] data = new byte[contentLength];
					exchange.getRequestBody().read(data);
					requestBody = new String(data, StandardCharsets.UTF_8);
				} else {
					requestBody = "";
				}
			}
			if (LOGGER.isTraceEnabled()) {
				logMessageReceived(exchange, requestBody, renderer);
			}

			String soapaction = null;
			if (exchange.getRequestHeaders().containsKey("SOAPACTION")) {
				soapaction = exchange.getRequestHeaders().getFirst("SOAPACTION");
			} else if (exchange.getRequestHeaders().containsKey("CALLBACK")) {
				soapaction = exchange.getRequestHeaders().getFirst("CALLBACK");
			}

			String method = exchange.getRequestMethod().toUpperCase();

			String uri = exchange.getRequestURI().getPath();
			// Samsung 2012 TVs have a problematic preceding slash that needs to be removed.
			if (uri.startsWith("/")) {
				LOGGER.trace("Stripping preceding slash from: " + uri);
				uri = uri.substring(1);
			}

			if ((GET.equals(method) || HEAD.equals(method)) && (uri.toLowerCase().endsWith(".png") || uri.toLowerCase().endsWith(".jpg") || uri.toLowerCase().endsWith(".jpeg"))) {
				sendResponse(exchange, renderer, 200, imageHandler(exchange, uri));

				//------------------------- START ContentDirectory -------------------------
			} else if (GET.equals(method) && uri.endsWith("/ContentDirectory/desc")) {
				sendResponse(exchange, renderer, 200, contentDirectorySpec(exchange), CONTENT_TYPE_XML_UTF8);
			} else if (POST.equals(method) && uri.endsWith("/ContentDirectory/action")) {
				if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSystemUpdateID")) {
					sendResponse(exchange, renderer, 200, getSystemUpdateIdHandler(), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_SetBookmark")) {
					sendResponse(exchange, renderer, 200, samsungSetBookmarkHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#X_GetFeatureList")) { // Added for Samsung 2012 TVs
					sendResponse(exchange, renderer, 200, samsungGetFeaturesListHandler(renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSortCapabilities")) {
					sendResponse(exchange, renderer, 200, getSortCapabilitiesHandler(), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#GetSearchCapabilities")) {
					sendResponse(exchange, renderer, 200, getSearchCapabilitiesHandler(renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Browse")) {
					sendResponse(exchange, renderer, 200, browseHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else if (soapaction != null && soapaction.contains("ContentDirectory:1#Search")) {
					sendResponse(exchange, renderer, 200, searchHandler(requestBody, renderer), CONTENT_TYPE_XML_UTF8);
				} else {
					LOGGER.debug("Unsupported action received: " + requestBody);
					sendResponse(exchange, renderer, 200, notifyHandler(exchange), "text/xml");
				}
			} else if (SUBSCRIBE.equals(method)) {
				sendResponse(exchange, renderer, 200, subscribeHandler(exchange, uri, soapaction), CONTENT_TYPE_XML_UTF8);
			} else if (NOTIFY.equals(method)) {
				sendResponse(exchange, renderer, 200, notifyHandler(exchange), CONTENT_TYPE_XML_UTF8);
				//------------------------- END ContentDirectory -------------------------
			}
		} catch (ContentDirectoryException ex) {
			sendErrorResponse(exchange, renderer, ex.getErrorCode());
		} catch (IOException e) {
			String message = e.getMessage();
			if (message != null) {
				if (message.equals("Connection reset by peer")) {
					LOGGER.trace("Http request from {}: {}", getRendererName(exchange, renderer), message);
				}
			} else {
				LOGGER.error("Http request error:", e);
			}
		}
	}

	private static void sendErrorResponse(final HttpExchange exchange, final Renderer renderer, int code) throws IOException {
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
		exchange.sendResponseHeaders(code, 0);
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, null, null, renderer);
		}
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, String message, String contentType) throws IOException {
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
		exchange.getResponseHeaders().set("Content-Type", contentType);
		if (message == null || message.length() == 0) {
			// No response data. Seems we are merely serving up headers.
			exchange.sendResponseHeaders(204, 0);
			if (LOGGER.isTraceEnabled()) {
				logMessageSent(exchange, null, null, renderer);
			}
			return;
		}
		// A response message was constructed; convert it to data ready to be sent.
		byte[] responseData = message.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(code, responseData.length);
		// HEAD requests only require headers to be set, no need to set contents.
		if (!HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			// Not a HEAD request, so set the contents of the response.
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(responseData);
				os.flush();
			}
		}
		exchange.close();
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, message, null, renderer);
		}
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, InputStream inputStream) throws IOException {
		sendResponse(exchange, renderer, code, inputStream, -2, true);
	}

	private static void sendResponse(final HttpExchange exchange, final Renderer renderer, int code, InputStream inputStream, long cLoverride, boolean writeStream) throws IOException {
		// There is an input stream to send as a response.
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
		if (inputStream == null) {
			// No input stream. Seems we are merely serving up headers.
			exchange.sendResponseHeaders(204, 0);
			if (LOGGER.isTraceEnabled()) {
				logMessageSent(exchange, null, null, renderer);
			}
			return;
		}
		long contentLength = 0;
		if (cLoverride > -2) {
			// Content-Length override has been set, send or omit as appropriate
			if (cLoverride == 0) {
				//mean no content, HttpExchange use the -1 value for it.
				contentLength = -1;
			} else if (cLoverride > -1 && cLoverride != StoreResource.TRANS_SIZE) {
				// Since PS3 firmware 2.50, it is wiser not to send an arbitrary Content-Length,
				// as the PS3 will display a network error and request the last seconds of the
				// transcoded video. Better to send no Content-Length at all.
				contentLength = cLoverride;
			} else if (cLoverride == -1) {
				//chunked, HttpExchange use the 0 value for it.
				contentLength = 0;
			}
		} else {
			contentLength = inputStream.available();
			LOGGER.trace("Available Content-Length: {}", contentLength);
		}
		if (contentLength > 0) {
			exchange.getResponseHeaders().set("Content-length", Long.toString(contentLength));
		}

		// Send the response headers to the client.
		exchange.sendResponseHeaders(code, contentLength);
		if (LOGGER.isTraceEnabled()) {
			logMessageSent(exchange, null, inputStream, renderer);
		}
		// send only if no HEAD method is being used.
		if (writeStream && !HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			// Send the response body to the client in chunks.
			byte[] buf = new byte[BUFFER_SIZE];
			int length;
			try (OutputStream outputStream = exchange.getResponseBody()) {
				int lengthSent = 0;
				try {
					while ((length = inputStream.read(buf)) > 0) {
						outputStream.write(buf, 0, length);
						outputStream.flush();
						lengthSent += length;
					}
				} catch (IOException ioe) {
					//client close the connection
				}
				try {
					outputStream.close();
				} catch (IOException ioe) {
					//client close the connection and insufficient bytes written to stream
				}
				LOGGER.trace("OutputStream({}) - bytes sent: {}/{}", outputStream.getClass().getName(), lengthSent, contentLength);
			}
		}
		try {
			inputStream.close();
		} catch (IOException ioe) {
			LOGGER.error("Caught exception", ioe);
		}
		exchange.close();
	}

	private static Collection<Map.Entry<String, String>> getHeaders(HttpExchange exchange) {
		HashMap<String, String> headers = new HashMap<>();
		for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
			String name = header.getKey();
			for (String value : header.getValue()) {
				headers.put(name, value);
			}
		}
		return headers.entrySet();
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 *
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null
	 * otherwise.
	 */
	private static InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replace("//", "/");
		ClassLoader cll = ContentDirectoryHandler.class.getClassLoader();
		InputStream is = cll.getResourceAsStream(fileName.substring(1));

		while (is == null && cll.getParent() != null) {
			cll = cll.getParent();
			is = cll.getResourceAsStream(fileName.substring(1));
		}

		return is;
	}

	private static InputStream imageHandler(HttpExchange exchange, String uri) {
		if (uri.toLowerCase().endsWith(".png")) {
			exchange.getResponseHeaders().set("Content-Type", "image/png");
		} else {
			exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
		}
		exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		exchange.getResponseHeaders().set("Expires", getFutureDate() + " GMT");
		return getResourceInputStream(uri);
	}

	//------------------------- START ContentDirectory -------------------------
	private static String contentDirectorySpec(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Cache-Control", "no-cache");
		exchange.getResponseHeaders().set("Expires", "0");
		exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		InputStream iStream = getResourceInputStream("UPnP_AV_ContentDirectory_1.0.xml");

		byte[] b = new byte[iStream.available()];
		iStream.read(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/**
	 * Wraps the payload around soap Envelope / Body tags.
	 *
	 * @param payload Soap body as a XML String
	 * @return Soap message as a XML string
	 */
	private static StringBuilder createResponse(String payload) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.XML_HEADER).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER).append(CRLF);
		response.append(payload).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER).append(CRLF);
		return response;
	}

	private static String getSystemUpdateIdHandler() {
		StringBuilder payload = new StringBuilder();
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_HEADER).append(CRLF);
		payload.append("<Id>").append(MediaStoreIds.getSystemUpdateId()).append("</Id>").append(CRLF);
		payload.append(HTTPXMLHelper.GETSYSTEMUPDATEID_FOOTER);
		return createResponse(payload.toString()).toString();
	}

	private static String samsungSetBookmarkHandler(String requestBody, Renderer renderer) {
		LOGGER.debug("Setting bookmark");
		SamsungBookmark payload = getPayload(SamsungBookmark.class, requestBody);
		if (payload != null) {
			if (payload.getPosSecond() == 0) {
				// Sometimes when Samsung device is starting to play the video
				// it sends X_SetBookmark message immediatelly with the position=0.
				// No need to update database in such case.
				LOGGER.debug("Skipping \"set bookmark\". Position=0");
			} else {
				try {
					StoreResource resource = renderer.getMediaStore().getResource(payload.getObjectId());
					File file = new File(resource.getFileName());
					String path = file.getCanonicalPath();
					MediaStatusStore.setBookmark(path, renderer.getAccountUserId(), payload.getPosSecond());
				} catch (IOException e) {
					LOGGER.error("Cannot set bookmark", e);
				}
			}
		}
		return createResponse(HTTPXMLHelper.SETBOOKMARK_RESPONSE).toString();
	}

	private static <T> T getPayload(Class<T> clazz, String requestBody) {
		try {
			SOAPMessage message = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(requestBody.getBytes()));
			JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Document body = message.getSOAPBody().extractContentAsDocument();
			return unmarshaller.unmarshal(body, clazz).getValue();
		} catch (JAXBException | SOAPException | IOException e) {
			LOGGER.error("Unmarshalling error", e);
			return null;
		}
	}

	private static String samsungGetFeaturesListHandler(Renderer renderer) {
		StringBuilder features = new StringBuilder();
		String mediaStoreId = renderer.getMediaStore().getResourceId();
		features.append("<Features xmlns=\"urn:schemas-upnp-org:av:avs\"");
		features.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		features.append(" xsi:schemaLocation=\"urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd\">").append(CRLF);
		features.append("<Feature name=\"samsung.com_BASICVIEW\" version=\"1\">").append(CRLF);
		// we may use here different container IDs in the future
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.audioItem\"/>").append(CRLF);
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.videoItem\"/>").append(CRLF);
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.imageItem\"/>").append(CRLF);
		features.append("</Feature>").append(CRLF);
		features.append("</Features>").append(CRLF);

		StringBuilder response = new StringBuilder();
		response.append("<u:X_GetFeatureListResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">").append(CRLF);
		response.append("<FeatureList>").append(CRLF);

		response.append(StringEscapeUtils.escapeXml10(features.toString()));

		response.append("</FeatureList>").append(CRLF);
		response.append("</u:X_GetFeatureListResponse>");
		return createResponse(response.toString()).toString();
	}

	private static String getSortCapabilitiesHandler() {
		return createResponse(HTTPXMLHelper.SORTCAPS_RESPONSE).toString();
	}

	private static String getSearchCapabilitiesHandler(Renderer mediaRenderer) {
		if (mediaRenderer.isUpnpSearchCapsEnabled()) {
			return createResponse(HTTPXMLHelper.SEARCHCAPS_RESPONSE).toString();
		} else {
			return createResponse(HTTPXMLHelper.SEARCHCAPS_RESPONSE_SEARCH_DEACTIVATED).toString();
		}
	}

	private static String browseHandler(String requestBody, Renderer renderer) throws ContentDirectoryException {
		BrowseRequest requestMessage = getPayload(BrowseRequest.class, requestBody);
		return browseSearchHandler(requestMessage, requestBody, renderer).toString();
	}

	private static String searchHandler(String requestBody, Renderer renderer) throws ContentDirectoryException {
		SearchRequest requestMessage = getPayload(SearchRequest.class, requestBody);
		try {
			return new SearchRequestHandler().createSearchResponse(requestMessage, renderer).toString();
		} catch (Exception e) {
			LOGGER.trace("error transforming searchCriteria to SQL. Fallback to content browsing ...", e);
			return browseHandler(requestBody, renderer);
		}
	}

	/**
	 * Hybrid handler for Browse and Search requests.
	 *
	 * @param requestMessage parsed message
	 * @return Soap response as a XML string
	 */
	private static StringBuilder browseSearchHandler(BrowseSearchRequest requestMessage, String requestBody, Renderer renderer) throws ContentDirectoryException {
		int startingIndex = 0;
		int requestCount = 0;
		boolean xbox360 = renderer.isXbox360();
		String objectID = requestMessage.getObjectId();
		String containerID = null;
		if ((objectID == null || objectID.length() == 0)) {
			containerID = requestMessage.getContainerId();
			if (containerID == null || (xbox360 && !containerID.contains("$"))) {
				objectID = "0";
			} else {
				objectID = containerID;
				containerID = null;
			}
		}
		Integer sI = requestMessage.getStartingIndex();
		Integer rC = requestMessage.getRequestedCount();
		String browseFlag = requestMessage.getBrowseFlag();

		if (sI != null) {
			startingIndex = sI;
		}

		if (rC != null) {
			requestCount = rC;
		}

		boolean browseDirectChildren = browseFlag != null && browseFlag.equals("BrowseDirectChildren");

		if (requestMessage instanceof SearchRequest) {
			browseDirectChildren = true;
		}

		// Xbox 360 virtual containers ... d'oh!
		String searchCriteria = null;
		if (xbox360 && containerID != null) {
			MediaLibrary library = renderer.getMediaStore().getMediaLibrary();
			if (containerID.equals("7") && library.getAlbumFolder() != null) {
				objectID = library.getAlbumFolder().getResourceId();
			} else if (containerID.equals("6") && library.getArtistFolder() != null) {
				objectID = library.getArtistFolder().getResourceId();
			} else if (containerID.equals("5") && library.getGenreFolder() != null) {
				objectID = library.getGenreFolder().getResourceId();
			} else if (containerID.equals("F") && library.getPlaylistFolder() != null) {
				objectID = library.getPlaylistFolder().getResourceId();
			} else if (containerID.equals("4") && library.getAllFolder() != null) {
				objectID = library.getAllFolder().getResourceId();
			} else if (containerID.equals("1")) {
				String artist = getEnclosingValue(requestBody, "upnp:artist = &quot;", "&quot;)");
				if (artist != null) {
					objectID = library.getArtistFolder().getResourceId();
					searchCriteria = artist;
				}
			}
		} else if (requestMessage instanceof SearchRequest) {
			searchCriteria = requestMessage.getSearchCriteria();
		}

		List<StoreResource> resources = renderer.getMediaStore().getResources(
				objectID,
				browseDirectChildren
		);

		if (resources != null) {
			//handle searchCriteria
			if (searchCriteria != null) {
				UMSUtils.filterResourcesByName(resources, searchCriteria, false, false);
				if (xbox360 && !resources.isEmpty() && resources.get(0) instanceof StoreContainer libraryContainer) {
					resources = libraryContainer.getChildren();
				}
			}
			//handle startingIndex and requestedCount
			if (startingIndex != 0 || requestCount != 0) {
				int toIndex;
				if (requestCount == 0) {
					toIndex = resources.size();
				} else {
					toIndex = Math.min(startingIndex + requestCount, resources.size());
				}
				resources = resources.subList(startingIndex, toIndex);
			}
		}

		int minus = 0;
		StringBuilder filesData = new StringBuilder();
		if (resources != null) {
			for (StoreResource resource : resources) {
				if (resource instanceof PlaylistFolder playlistFolder) {
					File f = new File(resource.getFileName());
					if (resource.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (xbox360 && containerID != null && resource != null) {
					resource.setFakeParentId(containerID);
				}

				if (resource instanceof StoreContainer) {
					filesData.append(DidlHelper.getDidlString(resource));
				} else if (resource instanceof StoreItem item && (item.isCompatible() &&
						(item.getEngine() == null || item.getEngine().isEngineCompatible(renderer)) ||
						// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
						// all possible combination not only those supported by renderer because the renderer setting could be wrong.
						resources.get(0).isInsideTranscodeFolder())) {
					filesData.append(DidlHelper.getDidlString(resource));
				} else {
					minus++;
				}
			}
		}

		StringBuilder response = new StringBuilder();

		if (requestMessage instanceof SearchRequest) {
			response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
		} else {
			response.append(HTTPXMLHelper.BROWSERESPONSE_HEADER);
		}

		response.append(CRLF);
		response.append(HTTPXMLHelper.RESULT_HEADER);
		response.append(HTTPXMLHelper.DIDL_HEADER);
		response.append(filesData);
		response.append(HTTPXMLHelper.DIDL_FOOTER);
		response.append(HTTPXMLHelper.RESULT_FOOTER);
		response.append(CRLF);

		int filessize = 0;
		if (resources != null) {
			filessize = resources.size();
		}

		response.append("<NumberReturned>").append(filessize - minus).append("</NumberReturned>");
		response.append(CRLF);

		if (browseDirectChildren && renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, resources are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular resources, plus the #transcode folder, maybe the #imdb one, also resources can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			int totalCount = startingIndex + requestCount + 1; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (filessize - minus <= 0) {
				totalCount = startingIndex;
			}

			response.append("<TotalMatches>").append(totalCount).append("</TotalMatches>");
		} else if (browseDirectChildren) {
			StoreContainer parentFolder;
			if (resources != null && filessize > 0) {
				parentFolder = resources.get(0).getParent();
			} else {
				StoreResource resource = renderer.getMediaStore().getResource(objectID);
				if (resource instanceof StoreContainer libraryContainer) {
					parentFolder = libraryContainer;
				} else {
					if (resource instanceof StoreItem) {
						LOGGER.debug("Trying to browse direct children on a store item for objectID '{}' !", objectID);
					} else {
						LOGGER.debug("Trying to browse direct children on a null object for objectID '{}' !", objectID);
					}
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
				}
			}
			response.append("<TotalMatches>").append(((parentFolder != null) ? parentFolder.childrenCount() : filessize) - minus).append("</TotalMatches>");
		} else {
			// From upnp spec: If BrowseMetadata is specified in the BrowseFlags then TotalMatches = 1
			response.append("<TotalMatches>1</TotalMatches>");
		}
		response.append(CRLF);

		/**
		 * From page 95, section 5.5.8.2 of
		 * http://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service.pdf
		 *
		 * UpdateID: The value returned in the UpdateID argument shall be the
		 * SystemUpdateID state variable value at the time the Browse() response
		 * was generated. If a control point finds that the current
		 * SystemUpdateID state variable value is not equal to the value
		 * returned in the UpdateID argument, then a change within the
		 * ContentDirectory service has occurred between the time the result was
		 * generated and the time that the control point is processing the
		 * result. The control point might therefore want to re-invoke the
		 * Browse() action to ensure that it has the latest property values.
		 * Note however that the change in the value of the SystemUpdateID state
		 * variable could have been caused by a change that occurred in a
		 * location in the ContentDirectory tree hierarchy that is not part of
		 * the returned result. In this case, the re-invocation of the Browse()
		 * action will return the exact same result. Note: This definition is
		 * not backwards compatible with previous versions of this
		 * specification. However, the previous definition did not indicate
		 * changes to properties of child containers. Therefore the control
		 * point would not have been aware that it had stale data.
		 */
		response.append("<UpdateID>");
		response.append(MediaStoreIds.getSystemUpdateId());
		response.append("</UpdateID>");
		response.append(CRLF);

		if (requestMessage instanceof SearchRequest) {
			response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
		} else {
			response.append(HTTPXMLHelper.BROWSERESPONSE_FOOTER);
		}

		return createResponse(response.toString());
	}

	/**
	 * Returns the string value that is enclosed by the left and right tag in a
	 * content string. Only the first match of each tag is used to determine
	 * positions. If either of the tags cannot be found, null is returned.
	 *
	 * @param content The entire {@link String} that needs to be searched for
	 * the left and right tag.
	 * @param leftTag The {@link String} determining the match for the left tag.
	 * @param rightTag The {@link String} determining the match for the right
	 * tag.
	 * @return The {@link String} that was enclosed by the left and right tag.
	 */
	private static String getEnclosingValue(String content, String leftTag, String rightTag) {
		String result = null;
		int leftTagPos = content.indexOf(leftTag);
		int leftTagStop = content.indexOf('>', leftTagPos + 1);
		int rightTagPos = content.indexOf(rightTag, leftTagStop + 1);

		if (leftTagPos > -1 && rightTagPos > leftTagPos) {
			result = content.substring(leftTagStop + 1, rightTagPos);
		}

		return result;
	}

	private String subscribeHandler(HttpExchange exchange, String uri, String soapaction) throws IOException {
		exchange.getResponseHeaders().set("SID", MediaServer.getUniqueDeviceName());

		/**
		 * Requirement [7.2.22.1]: UPnP devices must send events to all properly
		 * subscribed UPnP control points. The device must enforce a
		 * subscription TIMEOUT value of 5 minutes. The UPnP device behavior of
		 * enforcing this 5 minutes TIMEOUT value is implemented by specifying
		 * "TIMEOUT: second-300" as an HTTP header/value pair.
		 */
		exchange.getResponseHeaders().set("TIMEOUT", "Second-300");

		if (soapaction != null) {
			String cb = soapaction.replace("<", "").replace(">", "");

			try {
				URL soapActionUrl = URI.create(cb).toURL();
				String addr = soapActionUrl.getHost();
				int port = soapActionUrl.getPort();
				try (
						Socket sock = new Socket(addr, port); OutputStream out = sock.getOutputStream()) {
					out.write(("NOTIFY /" + uri + " HTTP/1.1").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("SID: " + MediaServer.getUniqueDeviceName()).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("SEQ: " + 0).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("NT: upnp:event").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("NTS: upnp:propchange").getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.write(("HOST: " + addr + ":" + port).getBytes(StandardCharsets.UTF_8));
					out.write(CRLF.getBytes(StandardCharsets.UTF_8));
					out.flush();
				}
			} catch (IllegalArgumentException | MalformedURLException ex) {
				LOGGER.debug("Cannot parse address and port from soap action \"" + soapaction + "\"", ex);
			}
		} else {
			LOGGER.debug("Expected soap action in request");
		}

		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.eventHeader("urn:schemas-upnp-org:service:ContentDirectory:1"));
		response.append(HTTPXMLHelper.eventProp("TransferIDs"));
		response.append(HTTPXMLHelper.eventProp("ContainerUpdateIDs"));
		response.append(HTTPXMLHelper.eventProp("SystemUpdateID", "" + MediaStoreIds.getSystemUpdateId()));
		response.append(HTTPXMLHelper.EVENT_FOOTER);
		return response.toString();
	}

	private static String notifyHandler(HttpExchange exchange) {
		exchange.getResponseHeaders().set("NT", "upnp:event");
		exchange.getResponseHeaders().set("NTS", "upnp:propchange");
		exchange.getResponseHeaders().set("SID", MediaServer.getUniqueDeviceName());
		exchange.getResponseHeaders().set("SEQ", "0");
		StringBuilder response = new StringBuilder();
		response.append("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">");
		response.append("<e:property>");
		response.append("<TransferIDs></TransferIDs>");
		response.append("</e:property>");
		response.append("<e:property>");
		response.append("<ContainerUpdateIDs></ContainerUpdateIDs>");
		response.append("</e:property>");
		response.append("<e:property>");
		response.append("<SystemUpdateID>").append(MediaStoreIds.getSystemUpdateId()).append("</SystemUpdateID>");
		response.append("</e:property>");
		response.append("</e:propertyset>");
		return response.toString();
	}

	//------------------------- END ContentDirectory -------------------------
	/**
	 * Returns a date somewhere in the far future.
	 *
	 * @return The {@link String} containing the date
	 */
	private static String getFutureDate() {
		SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
		return SDF.format(new Date(10000000000L + System.currentTimeMillis()));
	}

	private static void logMessageSent(HttpExchange exchange, String response, InputStream iStream, Renderer renderer) {
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
		String rendererName = getRendererName(exchange, renderer);

		if (HEAD.equalsIgnoreCase(exchange.getRequestMethod())) {
			LOGGER.trace(
					"HEAD only response sent to {}:\n{}\n{}\n{}{}",
					rendererName,
					HTTPSERVER_RESPONSE_BEGIN,
					responseCode,
					header,
					HTTPSERVER_RESPONSE_END
			);
		} else {
			String formattedResponse = null;
			if (StringUtils.isNotBlank(response)) {
				try {
					formattedResponse = StringUtil.prettifyXML(response, StandardCharsets.UTF_8, 4);
				} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
					formattedResponse = "  Content isn't valid XML, using text formatting: " + e.getMessage() + "\n";
					formattedResponse += "    " + response.replace("\n", "\n    ");
				}
			}
			if (StringUtils.isNotBlank(formattedResponse)) {
				LOGGER.trace(
						"Response sent to {}:\n{}\n{}\n{}\nCONTENT:\n{}\n{}",
						rendererName,
						HTTPSERVER_RESPONSE_BEGIN,
						responseCode,
						header,
						formattedResponse,
						HTTPSERVER_RESPONSE_END
				);
				Matcher matcher = DIDL_PATTERN.matcher(response);
				if (matcher.find()) {
					try {
						LOGGER.trace(
								"The unescaped <Result> sent to {} is:\n{}",
								rendererName,
								StringUtil.prettifyXML(StringEscapeUtils.unescapeXml(matcher.group(1)), StandardCharsets.UTF_8, 2)
						);
					} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
						LOGGER.warn("Failed to prettify DIDL-Lite document: {}", e.getMessage());
						LOGGER.trace("", e);
					}
				}
			} else if (iStream != null && !"0".equals(exchange.getResponseHeaders().getFirst("Content-Length"))) {
				LOGGER.trace(
						"Transfer response sent to {}:\n{}\n{} ({})\n{}{}",
						rendererName,
						HTTPSERVER_RESPONSE_BEGIN,
						responseCode,
						getResponseIsChunked(exchange) ? "chunked" : "non-chunked",
						header,
						HTTPSERVER_RESPONSE_END
				);
			} else {
				LOGGER.trace(
						"Empty response sent to {}:\n{}\n{}\n{}{}",
						rendererName,
						HTTPSERVER_RESPONSE_BEGIN,
						responseCode,
						header,
						HTTPSERVER_RESPONSE_END
				);
			}
		}
	}

	private static void logMessageReceived(HttpExchange exchange, String content, Renderer renderer) {
		StringBuilder header = new StringBuilder();
		String soapAction = null;
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
					if ("SOAPACTION".equalsIgnoreCase(name)) {
						soapAction = value.toUpperCase(Locale.ROOT);
					}
				}
			}
		}
		String formattedContent = null;
		if (StringUtils.isNotBlank(content)) {
			try {
				formattedContent = StringUtil.prettifyXML(content, StandardCharsets.UTF_8, 2);
			} catch (XPathExpressionException | SAXException | ParserConfigurationException | TransformerException e) {
				LOGGER.trace("XML parsing failed with:\n{}", e);
				formattedContent = "  Content isn't valid XML, using text formatting: " + e.getMessage() + "\n";
				formattedContent += "    " + content.replace("\n", "\n    ") + "\n";
			}
		}
		String requestType = "";
		// Map known requests to request type
		if (soapAction != null) {
			if (soapAction.contains("CONTENTDIRECTORY:1#BROWSE")) {
				requestType = "browse ";
			} else if (soapAction.contains("CONTENTDIRECTORY:1#SEARCH")) {
				requestType = "search ";
			}
		} else {
			soapAction = "";
		}

		String rendererName = getRendererName(exchange, renderer);
		formattedContent = StringUtils.isNotBlank(formattedContent) ? "\nCONTENT:\n" + formattedContent : "";
		if (StringUtils.isNotBlank(requestType)) {
			LOGGER.trace(
					"Received a {}request from {}:\n{}\n{}{}{}",
					requestType,
					rendererName,
					HTTPSERVER_REQUEST_BEGIN,
					header,
					formattedContent,
					HTTPSERVER_REQUEST_END
			);
		} else { // Trace not supported request type
			LOGGER.trace(
					"Received a {}request from {}:\n{}\n{}{}{}\nRenderer UUID={}",
					soapAction,
					rendererName,
					HTTPSERVER_REQUEST_BEGIN,
					header,
					formattedContent,
					HTTPSERVER_REQUEST_END,
					renderer != null ? renderer.getUUID() : "null"
			);
		}
	}

	private static boolean getResponseIsChunked(HttpExchange exchange) {
		return exchange.getResponseHeaders().containsKey("Transfer-Encoding") &&
				exchange.getResponseHeaders().getFirst("Transfer-Encoding").equalsIgnoreCase("chunked");
	}

	private static String getRendererName(HttpExchange exchange, Renderer renderer) {
		String rendererName;
		if (renderer != null) {
			if (StringUtils.isNotBlank(renderer.getRendererName())) {
				if (StringUtils.isBlank(renderer.getConfName()) || renderer.getRendererName().equals(renderer.getConfName())) {
					rendererName = renderer.getRendererName();
				} else {
					rendererName = renderer.getRendererName() + " [" + renderer.getConfName() + "]";
				}
			} else if (StringUtils.isNotBlank(renderer.getConfName())) {
				rendererName = renderer.getConfName();
			} else {
				rendererName = "Unnamed";
			}
		} else {
			rendererName = "Unknown";
		}
		if (exchange != null && exchange.getRemoteAddress() instanceof InetSocketAddress) {
			rendererName +=
					" (" + exchange.getRemoteAddress().getAddress().getHostAddress() +
					":" + exchange.getRemoteAddress().getPort() + ")";
		}
		return rendererName;
	}
}
