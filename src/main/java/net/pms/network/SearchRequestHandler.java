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
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.dlna.virtual.VirtualFolderDbId;
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

	final static int TYPE_UNKNOWN = 0;
	final static int TYPE_FILES = 1;
	final static int TYPE_ALBUM = 2;
	final static int TYPE_PERSON = 3;
	final static int TYPE_PLAYLIST = 4;
	final static int TYPE_VIDEO = 5;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchRequestHandler.class);
	private final static String CRLF = "\r\n";

	private static Pattern classPattern = Pattern.compile("upnp:class\\s(\\bderivedfrom\\b|=)\\s+\"(?<val>.*?)\"",
		Pattern.CASE_INSENSITIVE);
	private static Pattern tokenizerPattern = Pattern
		.compile("(?<property>((\\bdc\\b)|(\\bupnp\\b)):[A-Za-z]+)\\s+(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>.*?)\"", Pattern.CASE_INSENSITIVE);

	public SearchRequestHandler() {
		this.database = PMS.get().getDatabase();
	}

	int getRequestType(String searchCriteria) {
		Matcher matcher = classPattern.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			if (propertyValue != null) {
				if (propertyValue.toLowerCase().startsWith("object.item.audioitem")) {
					return TYPE_FILES;
				} else if (propertyValue.toLowerCase().startsWith("object.container.person")) {
					return TYPE_PERSON;
				} else if (propertyValue.toLowerCase().startsWith("object.container.album")) {
					return TYPE_ALBUM;
				} else if (propertyValue.toLowerCase().startsWith("object.container.playlistcontainer")) {
					return TYPE_PLAYLIST;
				} else if (propertyValue.toLowerCase().startsWith("object.item.videoitem")) {
					return TYPE_VIDEO;
				}
			}
		}
		return TYPE_UNKNOWN;
	}

	public StringBuilder createSearchResponse(SearchRequest requestMessage, RendererConfiguration mediaRenderer) {
		int numberReturned = 0;
		int totalMatches = 0;
		int updateID = 1;

		StringBuilder dlnaItems = new StringBuilder();
		try {
			int requestType = getRequestType(requestMessage.getSearchCriteria());

			VirtualFolderDbId folder = new VirtualFolderDbId("Search Result", "");
			int folderType = TYPE_FILES == requestType ? MediaLibraryFolder.FILES : MediaLibraryFolder.PLAYLISTS;
			if (requestType == TYPE_FILES || requestType == TYPE_PLAYLIST) {
				StringBuilder sqlFiles = convertToFilesSql(requestMessage.getSearchCriteria(), requestType, folderType);
				for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles.toString(), requestType)) {
					folder.addChild(resource);
				}
			} else {
				StringBuilder sqlText = new StringBuilder();
				sqlText.append(convertToFilesSql(requestMessage.getSearchCriteria(), requestType, MediaLibraryFolder.TEXTS));

				StringBuilder sqlFiles = new StringBuilder();
				sqlFiles.append(convertToFilesSql(requestMessage.getSearchCriteria(), requestType, MediaLibraryFolder.FILES));
				// folder = new MediaLibraryFolder(Messages.getString("PMS.16"),
				// new String[] {sqlText.toString(), String.format(
				// "select FILENAME, MODIFIED, F.ID as FID from FILES F,
				// AUDIOTRACKS A where F.ID = A.FILEID AND F.TYPE = 1 AND %s =
				// '${0}'",
				// getTitlePropertyMapping(requestType)) },
				// new int[] {MediaLibraryFolder.TEXTS, MediaLibraryFolder.FILES
				// });
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
		} catch (Exception e) {
			LOGGER.warn("error transforming searchCriteria to SQL.", e);
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, updateID, dlnaItems);
		return createResponse(response.toString());
	}

	/**
	 * Adds SELECT part of the sql.
	 *
	 * @param mediaFolderType
	 *
	 * @param sql
	 * @param search
	 */
	private String addSqlSelectByType(int requestType, int mediaFolderType) {
		if (MediaLibraryFolder.FILES == mediaFolderType) {
			switch (requestType) {
				case TYPE_FILES:
				case TYPE_PERSON:
				case TYPE_ALBUM:
					return "select FILENAME, MODIFIED, F.ID as FID from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
				case TYPE_PLAYLIST:
				case TYPE_VIDEO:
					return "select FILENAME, MODIFIED, F.ID as FID from FILES as F where ";
				default:
					throw new RuntimeException("not implemented request type");
			}
		} else if (MediaLibraryFolder.TEXTS == mediaFolderType) {
			return String.format("select %s, F.ID as FID from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ",
				getTitlePropertyMapping(requestType));
		} else if (MediaLibraryFolder.PLAYLISTS == mediaFolderType) {
			return "select FILENAME, MODIFIED, F.ID as FID from FILES F where ";
		}
		throw new RuntimeException("not implemented media folder type");
	}

	StringBuilder convertToFilesSql(String upnpSearch, int requestType, int mediaFolderType) {
		int lastIndex = 0;
		StringBuilder sb = new StringBuilder();

		sb.append(addSqlSelectByType(requestType, mediaFolderType));

		Matcher matcher = tokenizerPattern.matcher(upnpSearch);

		while (matcher.find()) {
			sb.append(upnpSearch, lastIndex, matcher.start());

			if ("upnp:class".equalsIgnoreCase(matcher.group("property"))) {
				interpretUpnpClass(sb, matcher.group("op"), matcher.group("val"), requestType);
			} else if (matcher.group("property").startsWith("upnp:") || matcher.group("property").startsWith("dc:")) {
				appendProperty(sb, matcher.group("property"), matcher.group("op"), matcher.group("val"), requestType);
			}
			sb.append("");
			lastIndex = matcher.end();
		}
		if (lastIndex < upnpSearch.length()) {
			sb.append(upnpSearch, lastIndex, upnpSearch.length());
		}
		if ((TYPE_FILES != requestType) && (MediaLibraryFolder.FILES == mediaFolderType)) {
			sb.append(" AND ").append(getTitlePropertyMapping(requestType));
			sb.append(" = ").append("'${0}'");
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
	private void appendProperty(StringBuilder sb, String property, String op, String val, int requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("LOWER(%s) regexp '.*%s.*'", getField(property, requestType), val.toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private Object getField(String property, int requestType) {
		// handle title by return type.
		if ("dc:title".equalsIgnoreCase(property)) {
			return getTitlePropertyMapping(requestType);
		} else if ("upnp:artist".equalsIgnoreCase(property)) {
			return " A.ARTIST";
		} else if ("upnp:genre".equalsIgnoreCase(property)) {
			return " A.GENRE";
		} else if ("dc:creator".equalsIgnoreCase(property)) {
			return " A.ALBUMARTIST ";
		}
		throw new RuntimeException("unknown or unimplemented property: >" + property + "<");
	}

	private String getTitlePropertyMapping(int requestType) {
		switch (requestType) {
			case TYPE_FILES:
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

	private void interpretUpnpClass(StringBuilder sb, String op, String val, int requestType) {
		if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
			sb.append(String.format(" F.TYPE = %d ", getFileType(requestType)));
		}
	}

	/**
	 * unpn:class filetype mapping
	 *
	 * @param val
	 * @return
	 */
	private int getFileType(int mediaFolderType) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		switch (mediaFolderType) {
			case TYPE_FILES:
			case TYPE_ALBUM:
			case TYPE_PERSON:
				return 1;
			case TYPE_VIDEO:
				return 4;
			case TYPE_PLAYLIST:
				return 16;
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	/**
	 * Converts sql statements with FILENAME to RealFiles
	 *
	 * @param query
	 * @return
	 */
	private List<DLNAResource> getDLNAResourceFromSQL(String query, int type) {
		ArrayList<DLNAResource> filesList = new ArrayList<>();

		try (Connection connection = database.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					while (resultSet.next()) {
						switch (type) {
							case TYPE_PLAYLIST:
								filesList.add(new VirtualFolderDbId(FilenameUtils.getBaseName(resultSet.getString("FILENAME")), "",
									resultSet.getString("FID")));
							default:
								filesList.add(new RealFileDbId(new File(resultSet.getString("FILENAME")), resultSet.getString("FID")));
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
