package net.pms.network;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbidTypeAndIdent;
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.virtual.VirtualFolderDbId;
import net.pms.network.DbIdResourceLocator.DbidMediaType;
import net.pms.network.message.SearchRequest;

/**
 * <pre>
 * This class generates a SearchRequestResponse message. It parses the supplied SearchCriteria string and converts it to H2DB sql grammar.
 *
 * Attention: This is rather a quick (hack) implementation for a general search use-case. Not all properties, op's and val's are being
 * processed or interpreted by the tokenizer.
 *
 * Lookout: A more robust but here now not implemented solution could use an EBNF parser like COCO/R or COCO/S using the grammar supplied
 * by the document <b>ContentDirectory:1 Service Template Version 1.01 Section 2.5.5.1</b>.
 * </pre>
 */
public class SearchRequestHandler {

	private DLNAMediaDatabase database;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchRequestHandler.class);
	private final static String CRLF = "\r\n";

	private static Pattern classPattern = Pattern.compile("upnp:class\\s(\\bderivedfrom\\b|=)\\s+\"(?<val>.*?)\"",
		Pattern.CASE_INSENSITIVE);
	private static Pattern tokenizerPattern = Pattern
		.compile("(?<property>((\\bdc\\b)|(\\bupnp\\b)):[A-Za-z]+)\\s+(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>.*?)\"", Pattern.CASE_INSENSITIVE);

	public SearchRequestHandler() {
		this.database = PMS.get().getDatabase();
	}

	private DbidMediaType getRequestType(String searchCriteria) {
		Matcher matcher = classPattern.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			if (propertyValue != null) {
				if (propertyValue.toLowerCase().startsWith("object.item.audioitem")) {
					return DbidMediaType.TYPE_AUDIO;
				} else if (propertyValue.toLowerCase().startsWith("object.container.person")) {
					return DbidMediaType.TYPE_PERSON;
				} else if (propertyValue.toLowerCase().startsWith("object.container.album")) {
					return DbidMediaType.TYPE_ALBUM;
				} else if (propertyValue.toLowerCase().startsWith("object.container.playlistcontainer")) {
					return DbidMediaType.TYPE_PLAYLIST;
				} else if (propertyValue.toLowerCase().startsWith("object.item.videoitem")) {
					return DbidMediaType.TYPE_VIDEO;
				}
			}
		}
		throw new RuntimeException("Unknown type : ");
	}

	public StringBuilder createSearchResponse(SearchRequest requestMessage, RendererConfiguration mediaRenderer) {
		int numberReturned = 0;
		int totalMatches = 0;
		int updateID = 1;

		StringBuilder dlnaItems = new StringBuilder();
		DbidMediaType requestType = getRequestType(requestMessage.getSearchCriteria());

		VirtualFolderDbId folder = new VirtualFolderDbId("SearchResult", new DbidTypeAndIdent(requestType, ""), "");
		if (requestType == DbidMediaType.TYPE_AUDIO || requestType == DbidMediaType.TYPE_PLAYLIST) {
			StringBuilder sqlFiles = convertToFilesSql(requestMessage.getSearchCriteria(), requestType);
			for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles.toString(), requestType)) {
				folder.addChild(resource);
			}
		} else {
			StringBuilder sqlFiles = new StringBuilder();
			sqlFiles.append(convertToFilesSql(requestMessage.getSearchCriteria(), requestType));
			for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles.toString(), requestType)) {
				folder.addChild(resource);
			}
		}

		folder.discoverChildren();
		for (DLNAResource uf : folder.getChildren()) {
			if (totalMatches >= requestMessage.getStartingIndex()) {
				totalMatches++;
				if (numberReturned < requestMessage.getRequestedCount()) {
					numberReturned++;
					uf.resolve();
					uf.setFakeParentId("0");
					dlnaItems.append(uf.getDidlString(mediaRenderer));
				}
			}
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, updateID, dlnaItems);
		return createResponse(response.toString());
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectByType(DbidMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO:
				return "select FILENAME, MODIFIED, F.ID as FID from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
			case TYPE_PERSON:
				return "select DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as FILENAME from AUDIOTRACKS as A where ";
			case TYPE_ALBUM:
				return "select DISTINCT ALBUM as FILENAME from AUDIOTRACKS as A where ";
			case TYPE_PLAYLIST:
				return "select DISTINCT FILENAME, MODIFIED, F.ID as FID from FILES as F where ";
			case TYPE_VIDEO:
				return "select FILENAME, MODIFIED, F.ID as FID from FILES as F where ";
			default:
				throw new RuntimeException("not implemented request type");
		}
	}

	StringBuilder convertToFilesSql(String upnpSearch, DbidMediaType requestType) {
		int lastIndex = 0;
		StringBuilder sb = new StringBuilder();

		sb.append(addSqlSelectByType(requestType));

		Matcher matcher = tokenizerPattern.matcher(upnpSearch);
		while (matcher.find()) {
			sb.append(upnpSearch, lastIndex, matcher.start());
			if ("upnp:class".equalsIgnoreCase(matcher.group("property"))) {
				aquireDatabaseType(sb, matcher.group("op"), matcher.group("val"), requestType);
			} else if (matcher.group("property").startsWith("upnp:") || matcher.group("property").startsWith("dc:")) {
				appendProperty(sb, matcher.group("property"), matcher.group("op"), matcher.group("val"), requestType);
			}
			sb.append("");
			lastIndex = matcher.end();
		}
		if (lastIndex < upnpSearch.length()) {
			sb.append(upnpSearch, lastIndex, upnpSearch.length());
		}
		return sb;
	}

	/**
	 * Title property depends on what Result type is being searched for.
	 *
	 * @param sb
	 * @param property
	 * @param op
	 * @param val
	 * @param requestType
	 */
	private void appendProperty(StringBuilder sb, String property, String op, String val, DbidMediaType requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("LOWER(%s) regexp '.*%s.*'", getField(property, requestType), val.toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private Object getField(String property, DbidMediaType requestType) {
		// handle title by return type.
		if ("dc:title".equalsIgnoreCase(property)) {
			return getTitlePropertyMapping(requestType);
		} else if ("upnp:artist".equalsIgnoreCase(property)) {
			return " A.ARTIST";
		} else if ("upnp:genre".equalsIgnoreCase(property)) {
			return " A.GENRE";
		} else if ("dc:creator".equalsIgnoreCase(property)) {
			return " A.ALBUMARTIST ";
		} else if ("upnp:album".equalsIgnoreCase(property)) {
			return " A.ALBUM ";
		}
		throw new RuntimeException("unknown or unimplemented property: >" + property + "<");
	}

	private String getTitlePropertyMapping(DbidMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO:
				return " A.SONGNAME ";
			case TYPE_ALBUM:
				return " A.ALBUM ";
			case TYPE_PERSON:
				return " COALESCE(A.ALBUMARTIST, A.ARTIST) ";
			case TYPE_PLAYLIST:
			case TYPE_VIDEO:
				return " F.FILENAME ";
			default:
				break;
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	private void aquireDatabaseType(StringBuilder sb, String op, String val, DbidMediaType requestType) {
		switch (requestType) {
			case TYPE_ALBUM:
			case TYPE_PERSON:
				sb.append(" 1=1 ");
				return;
			case TYPE_AUDIO:
			case TYPE_PLAYLIST:
			case TYPE_VIDEO:
				if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
					sb.append(String.format(" F.TYPE = %d ", getFileType(requestType)));
				}
				return;
			default:
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	/**
	 * unpn:class filetype mapping
	 *
	 * @param val
	 * @return
	 */
	private int getFileType(DbidMediaType mediaFolderType) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		switch (mediaFolderType) {
			case TYPE_AUDIO:
			case TYPE_ALBUM:
			case TYPE_PERSON:
				return 1;
			case TYPE_VIDEO:
				return 4;
			case TYPE_PLAYLIST:
				return 16;
			default:
				break;
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	/**
	 * Converts sql statements with FILENAME to RealFiles
	 *
	 * @param query
	 * @return
	 */
	private List<DLNAResource> getDLNAResourceFromSQL(String query, DbidMediaType type) {
		ArrayList<DLNAResource> filesList = new ArrayList<>();

		try (Connection connection = database.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					while (resultSet.next()) {
						String filenameField = FilenameUtils.getBaseName(resultSet.getString("FILENAME"));
						switch (type) {
							case TYPE_ALBUM:
							case TYPE_PERSON:
								filesList.add(new VirtualFolderDbId(filenameField, new DbidTypeAndIdent(type, filenameField), ""));
								break;
							case TYPE_PLAYLIST:
								filesList
									.add(new VirtualFolderDbId(filenameField, new DbidTypeAndIdent(type, resultSet.getString("FID")), ""));
								break;
							default:
								filesList.add(new RealFileDbId(new DbidTypeAndIdent(type, resultSet.getString("FID")),
									new File(resultSet.getString("FILENAME"))));
								break;
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		}
		return filesList;
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

	private StringBuilder buildEnvelope(int foundNumberReturned, int totalMatches, int updateID, StringBuilder dlnaItems) {
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
		return response;
	}

}
