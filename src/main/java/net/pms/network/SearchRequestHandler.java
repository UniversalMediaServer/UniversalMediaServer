package net.pms.network;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.network.message.SearchRequest;

/**
 * <pre>
 * This class generates a SearchRequestResponse message. It parses the supplied SearchCriteria string and converts it to H2DB sql grammar.
 *
 * Attention: This is rather a quick (hack) implementation for a general search use-case. Not all properties, op's and val's are being
 * processed or interpreted by the tokenizer.
 *
 * Lookout: A right - now not implemented - but more stable solution could use an EBNF parser like COCO/R or COCO/S using the grammar supplied
 * by the document <b>ContentDirectory:1 Service Template Version 1.01 Section 2.5.5.1</b>.
 * </pre>
 */
public class SearchRequestHandler {

	final static int TYPE_UNKNOWN = 0;
	final static int TYPE_FILES = 1;
	final static int TYPE_ALBUM = 2;
	final static int TYPE_PERSON = 3;
	final static int TYPE_PLAYLIST = 4;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchRequestHandler.class);
	private final static String CRLF = "\r\n";

	private static Pattern classPattern = Pattern.compile("upnp:class\\s(\\bderivedfrom\\b|=)\\s+\"(?<val>.*?)\"",
		Pattern.CASE_INSENSITIVE);
	private static Pattern tokenizerPattern = Pattern
		.compile("(?<property>((\\bdc\\b)|(\\bupnp\\b)):[A-Za-z]+)\\s+(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>.*?)\"", Pattern.CASE_INSENSITIVE);

	int getRequestType(String searchCriteria) {
		Matcher matcher = classPattern.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			if ("object.item.audioItem.musicTrack".equalsIgnoreCase(propertyValue)) {
				return TYPE_FILES;
			} else if ("object.container.person".equalsIgnoreCase(propertyValue)) {
				return TYPE_PERSON;
			} else if ("object.container.album".equalsIgnoreCase(propertyValue)) {
				return TYPE_ALBUM;
			} else if ("object.container.playlistContainer".equalsIgnoreCase(propertyValue)) {
				return TYPE_PLAYLIST;
			}
		}
		return TYPE_UNKNOWN;
	}

	public StringBuilder createSearchResponse(SearchRequest requestMessage, RendererConfiguration mediaRenderer) {
		DLNAMediaDatabase db = PMS.get().getDatabase();
		int foundNumberReturned = 0;
		int totalMatches = 0;
		int updateID = 1;

		StringBuilder dlnaItems = new StringBuilder();
		try {
			generateSql(requestMessage, mediaRenderer, dlnaItems);
		} catch (Exception e) {
			LOGGER.warn("error transforming searchCriteria to SQL.", e);
		}

		// Build response message
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
		response.append(CRLF);
		response.append(HTTPXMLHelper.RESULT_HEADER);
		response.append(HTTPXMLHelper.DIDL_HEADER);
		response.append(dlnaItems.toString());
		response.append(HTTPXMLHelper.DIDL_FOOTER);
		response.append(HTTPXMLHelper.RESULT_FOOTER);
		response.append(CRLF);
		response.append("<NumberReturned>").append(foundNumberReturned).append("</NumberReturned>");
		response.append(CRLF);
		response.append("<TotalMatches>").append(totalMatches).append("</TotalMatches>");
		response.append(CRLF);
		response.append("<UpdateID>");
		response.append(updateID);
		response.append("</UpdateID>");
		response.append(CRLF);
		response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
		return createResponse(response.toString());
	}

	private void generateSql(SearchRequest requestMessage, RendererConfiguration mediaRenderer, StringBuilder dlnaItems) {
		int requestType = getRequestType(requestMessage.getSearchCriteria());

		StringBuilder sql = new StringBuilder();
		sql.append(convertToSql(requestMessage.getSearchCriteria(), requestType));

		MediaLibraryFolder folder = new MediaLibraryFolder("Search result ...", sql.toString(), 0);
		folder.discoverChildren();
		for (DLNAResource uf : folder.getChildren()) {
			uf.resolve();
			System.out.println(uf.getDidlString(mediaRenderer));
			dlnaItems.append(uf.getDidlString(mediaRenderer));
		}
		System.out.println(dlnaItems.toString());
	}

	/**
	 * Adds SELECT part of the sql.
	 *
	 * @param sql
	 * @param search
	 */
	private void addSqlSelectPartByType(StringBuilder sql, int requestType) {
		switch (requestType) {
			case TYPE_FILES:
				sql.append("select FILENAME, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ");
				break;
			case TYPE_ALBUM:
				sql.append("select FILENAME, MODIFIED from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ");
				break;

			default:
				break;
		}
	}

	StringBuilder convertToSql(String upnpSearch, int requestType) {
		int lastIndex = 0;
		StringBuilder sb = new StringBuilder();

		addSqlSelectPartByType(sb, requestType);

		Matcher matcher = tokenizerPattern.matcher(upnpSearch);

		while (matcher.find()) {
			sb.append(upnpSearch, lastIndex, matcher.start());

			if ("upnp:class".equalsIgnoreCase(matcher.group("property"))) {
				interpretUpnpClass(sb, matcher.group("op"), matcher.group("val"), requestType);
			} else if (matcher.group("property").startsWith("dc:title")) {
				appendTitleProperty(sb, matcher.group("property"), matcher.group("op"), matcher.group("val"), requestType);
			}
			sb.append("");
			lastIndex = matcher.end();
		}
		if (lastIndex < upnpSearch.length()) {
			sb.append(upnpSearch, lastIndex, upnpSearch.length());
		}
		return sb;
	}

	private void appendTitleProperty(StringBuilder sb, String property, String op, String val, int requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("%s regexp '.*%s.*'", getField(property, requestType), val));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private Object getField(String property, int requestType) {
		if ("dc:title".equalsIgnoreCase(property)) {
			return getPropertyMapping(requestType);
		}
		throw new RuntimeException("unknown or unimplemented property: >" + property + "<");
	}


	private String getPropertyMapping(int requestType) {
		switch (requestType) {
			case TYPE_FILES:
				return " A.SONGNAME ";
			case TYPE_ALBUM:
				return " A.ALBUM ";
			case TYPE_PERSON:
				return " COALESCE(A.ALBUMARTIST, A.ARTIST) ";
			case TYPE_PLAYLIST:
				return " F.FILENAME ";
			default:
				break;
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	private void interpretUpnpClass(StringBuilder sb, String op, String val, int requestType) {
		if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
			sb.append(String.format(" F.TYPE = %d ", getFileType(val)));
		}
	}

	/**
	 * unpn:class filetype mapping
	 *
	 * @param val
	 * @return
	 */
	private int getFileType(String val) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		if ("object.item.audioItem.musicTrack".equalsIgnoreCase(val)) {
			return 1;
		} else if ("object.container.person".equalsIgnoreCase(val)) {
			return 1;
		} else if ("object.container.album".equalsIgnoreCase(val)) {
			return 1;
		} else if ("object.container.playlistContainer".equalsIgnoreCase(val)) {
			return 16;
		}
		throw new RuntimeException("unknown or unimplemented operator : " + "unknown or unimplemented upnp:class : >" + val + "<");
	}

	/**
	 * Wraps the payload around soap Envelope / Body tags.
	 *
	 * @param payload Soap body as a XML String
	 * @return Soap message as a XML string
	 */
	private StringBuilder createResponse(String payload) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.XML_HEADER).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER).append(CRLF);
		response.append(payload).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER).append(CRLF);
		return response;
	}

}
