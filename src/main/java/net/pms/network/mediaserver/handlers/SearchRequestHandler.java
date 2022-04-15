package net.pms.network.mediaserver.handlers;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.DbIdTypeAndIdent2;
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.virtual.VirtualFolderDbId;
import net.pms.formats.Format;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.network.mymusic.MusicBrainzAlbum;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchRequestHandler.class);
	private static final String CRLF = "\r\n";
	private static final Pattern CLASS_PATTERN = Pattern.compile("upnp:class\\s(\\bderivedfrom\\b|=)\\s+\"(?<val>.*?)\"",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern TOKENIZER_PATTERN = Pattern
		.compile("(?<property>((\\bdc\\b)|(\\bupnp\\b)):[A-Za-z]+)\\s+(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>.*?)\"", Pattern.CASE_INSENSITIVE);

	private final AtomicInteger updateID = new AtomicInteger(1);
	private final DbIdResourceLocator dbIdResourceLocator = new DbIdResourceLocator();

	public SearchRequestHandler() {
	}

	DbIdMediaType getRequestType(String searchCriteria) {
		Matcher matcher = CLASS_PATTERN.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			if (propertyValue != null) {
				propertyValue = propertyValue.toLowerCase();
				if (propertyValue.startsWith("object.item.audioitem")) {
					return DbIdMediaType.TYPE_AUDIO;
				} else if (propertyValue.startsWith("object.item.videoitem")) {
					return DbIdMediaType.TYPE_VIDEO;
				} else if (propertyValue.startsWith("object.item.imageitem")) {
					return DbIdMediaType.TYPE_IMAGE;
				} else if (propertyValue.startsWith("object.container.person")) {
					return DbIdMediaType.TYPE_PERSON;
				} else if (propertyValue.startsWith("object.container.album")) {
					return DbIdMediaType.TYPE_ALBUM;
				} else if (propertyValue.startsWith("object.container.playlistcontainer")) {
					return DbIdMediaType.TYPE_PLAYLIST;
				}
			}
		}
		throw new RuntimeException("Unknown type : " + (searchCriteria != null ? searchCriteria : "NULL"));
	}

	public StringBuilder createSearchResponse(SearchRequest requestMessage, RendererConfiguration mediaRenderer) {
		int numberReturned = 0;
		StringBuilder dlnaItems = new StringBuilder();
		DbIdMediaType requestType = getRequestType(requestMessage.getSearchCriteria());

		int totalMatches = getDLNAResourceCountFromSQL(convertToCountSql(requestMessage.getSearchCriteria(), requestType));

		VirtualFolderDbId folder = new VirtualFolderDbId("Search Result", new DbIdTypeAndIdent2(requestType, ""), "");
		String sqlFiles = convertToFilesSql(requestMessage, requestType);
		for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles, requestType)) {
			folder.addChild(resource);
		}

		folder.discoverChildren();
		for (DLNAResource uf : folder.getChildren()) {
			numberReturned++;
			uf.resolve();
			uf.setFakeParentId("0");
			dlnaItems.append(uf.getDidlString(mediaRenderer));
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, updateID.getAndIncrement(), dlnaItems);
		return createResponse(response.toString());
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectByType(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO:
				return "select A.RATING, FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
			case TYPE_PERSON:
				return "select DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as FILENAME, A.ID as oid from AUDIOTRACKS as A where ";
			case TYPE_ALBUM:
				return "select DISTINCT mbid_release as liked, MBID_RECORD, album, artist, media_year, ALBUM as FILENAME, A.ID as oid, A.MBID_RECORD from MUSIC_BRAINZ_RELEASE_LIKE as m right outer join AUDIOTRACKS as a on m.mbid_release = A.mbid_record where ";
			case TYPE_PLAYLIST:
				return "select DISTINCT FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			case TYPE_VIDEO:
			case TYPE_IMAGE:
				return "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			default:
				throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectCountByType(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO:
				return "select count(DISTINCT F.id) from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
			case TYPE_PERSON:
				return "select count (DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST)) from AUDIOTRACKS as A where ";
			case TYPE_ALBUM:
				return "select count(DISTINCT A.id) from AUDIOTRACKS as A where ";
			case TYPE_PLAYLIST:
				return "select count(DISTINCT F.id) from FILES as F where ";
			case TYPE_VIDEO:
			case TYPE_IMAGE:
				return "select count(DISTINCT F.id) from FILES as F where ";
			default:
				throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	String convertToFilesSql(SearchRequest requestMessage, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectByType(requestType));
		addSqlWherePart(requestMessage.getSearchCriteria(), requestType, sb);
		addOrderBy(requestMessage, requestType, sb);
		addLimit(requestMessage, requestType, sb);
		LOGGER.trace(sb.toString());
		return sb.toString();
	}

	private void addOrderBy(SearchRequest requestMessage, DbIdMediaType requestType, StringBuilder sb) {
		sb.append(" ORDER BY ");
		if (!StringUtils.isAllBlank(requestMessage.getSortCriteria())) {
			String[] sortElements = requestMessage.getSortCriteria().split("[;, ]");
			try {
				for (String sort : sortElements) {
					if (!StringUtils.isAllBlank(sort)) {
						String field = getField(sort.substring(1), requestType);
						if (!StringUtils.isAllBlank(field)) {
							sb.append(field);
							sb.append(sortOrder(sort.substring(0, 1)));
							sb.append(", ");
						}
					}
				}
			} catch (Exception e) {
				LOGGER.trace("ERROR while processing 'addOrderBy'");
			}
		}
		sb.append(String.format(" oid "));
	}

	private String sortOrder(String order) {
		if ("+".equals(order)) {
			return " ASC ";
		} else if ("-".equals(order)) {
			return " DESC ";
		}
		return "";
	}

	private void addLimit(SearchRequest requestMessage, DbIdMediaType requestType, StringBuilder sb) {
		int limit = requestMessage.getRequestedCount();
		int offset = requestMessage.getStartingIndex();
		if (limit == 0) {
			limit = 999; // performance issue: do only deliver top 999 items
		}
		sb.append(String.format(" LIMIT %d OFFSET %d ", limit, offset));
	}

	String convertToCountSql(String upnpSearch, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectCountByType(requestType));
		addSqlWherePart(upnpSearch, requestType, sb);
		return sb.toString();
	}

	private void addSqlWherePart(String upnpSearch, DbIdMediaType requestType, StringBuilder sb) {
		int lastIndex = 0;
		Matcher matcher = TOKENIZER_PATTERN.matcher(upnpSearch);
		while (matcher.find()) {
			sb.append(upnpSearch, lastIndex, matcher.start());
			if ("upnp:class".equalsIgnoreCase(matcher.group("property"))) {
				acquireDatabaseType(sb, matcher.group("op"), matcher.group("val"), requestType);
			} else if (matcher.group("property").startsWith("upnp:") || matcher.group("property").startsWith("dc:")) {
				appendProperty(sb, matcher.group("property"), matcher.group("op"), matcher.group("val"), requestType);
			}
			sb.append("");
			lastIndex = matcher.end();
		}
		if (lastIndex < upnpSearch.length()) {
			sb.append(upnpSearch, lastIndex, upnpSearch.length());
		}
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
	private void appendProperty(StringBuilder sb, String property, String op, String val, DbIdMediaType requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("LOWER(%s) regexp '.*%s.*'", getField(property, requestType), val.toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private String getField(String property, DbIdMediaType requestType) {
		// handle title by return type.
		if ("dc:title".equalsIgnoreCase(property)) {
			return getTitlePropertyMapping(requestType);
		} else if ("upnp:artist".equalsIgnoreCase(property)) {
			return " A.ARTIST ";
		} else if ("upnp:genre".equalsIgnoreCase(property)) {
			return " A.GENRE ";
		} else if ("dc:creator".equalsIgnoreCase(property)) {
			return " A.ALBUMARTIST ";
		} else if ("upnp:album".equalsIgnoreCase(property)) {
			return " A.ALBUM ";
		} else if ("upnp:rating".equalsIgnoreCase(property)) {
			return " rating ";
		} else if ("ums:likedAlbum".equalsIgnoreCase(property)) {
			return " liked ";
		}

		throw new RuntimeException("unknown or unimplemented property: >" + property + "<");
	}

	private String getTitlePropertyMapping(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO:
				return " A.SONGNAME ";
			case TYPE_ALBUM:
				return " A.ALBUM ";
			case TYPE_PERSON:
				return " COALESCE(A.ALBUMARTIST, A.ARTIST) ";
			case TYPE_PLAYLIST:
			case TYPE_VIDEO:
			case TYPE_IMAGE:
				return " F.FILENAME ";
			default:
				break;
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	private void acquireDatabaseType(StringBuilder sb, String op, String val, DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_ALBUM:
			case TYPE_PERSON:
				sb.append(" 1=1 ");
				return;
			case TYPE_AUDIO:
			case TYPE_PLAYLIST:
			case TYPE_VIDEO:
			case TYPE_IMAGE:
				if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
					sb.append(String.format(" F.FORMAT_TYPE = %d ", getFileType(requestType)));
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
	private int getFileType(DbIdMediaType mediaFolderType) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		switch (mediaFolderType) {
			case TYPE_AUDIO:
			case TYPE_ALBUM:
			case TYPE_PERSON:
				return Format.AUDIO;
			case TYPE_VIDEO:
				return Format.VIDEO;
			case TYPE_IMAGE:
				return Format.IMAGE;
			case TYPE_PLAYLIST:
				return Format.PLAYLIST;
			default:
				break;
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	private int getDLNAResourceCountFromSQL(String query) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("SQL count : %s", query));
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						return resultSet.getInt(1);
					}
				} catch (SQLException e) {
					LOGGER.trace("getDLNAResourceCountFromSQL", e);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return 0;
	}

	/**
	 * Converts sql statements with FILENAME to RealFiles
	 *
	 * @param query
	 * @return
	 */
	private List<DLNAResource> getDLNAResourceFromSQL(String query, DbIdMediaType type) {
		ArrayList<DLNAResource> filesList = new ArrayList<>();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("SQL %s : %s", type.dbidPrefix, query));
		}
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				try (Statement statement = connection.createStatement()) {
					try (ResultSet resultSet = statement.executeQuery(query)) {
						Set<String> foundMbidAlbums = new HashSet<>();
						while (resultSet.next()) {
							String filenameField = FilenameUtils.getBaseName(resultSet.getString("FILENAME"));
							switch (type) {
								case TYPE_ALBUM:
									String mbid = resultSet.getString("MBID_RECORD");
									if (StringUtils.isAllBlank(mbid)) {
										filesList.add(new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent2(type, filenameField), ""));
									} else {
										if (!foundMbidAlbums.contains(mbid)) {
											VirtualFolderDbId albumFolder = new VirtualFolderDbId(filenameField,
												new DbIdTypeAndIdent2(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid), "");
											MusicBrainzAlbum album = new MusicBrainzAlbum(resultSet.getString("MBID_RECORD"),
												resultSet.getString("album"), resultSet.getString("artist"), resultSet.getInt("media_year"));
											dbIdResourceLocator.appendAlbumInformation(album, albumFolder);
											filesList.add(albumFolder);
											foundMbidAlbums.add(mbid);
										}
									}
									break;
								case TYPE_PERSON:
									filesList.add(new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent2(type, filenameField), ""));
									break;
								case TYPE_PLAYLIST:
									filesList.add(
										new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent2(type, resultSet.getString("FID")), ""));
									break;
								default:
									filesList.add(new RealFileDbId(new DbIdTypeAndIdent2(type, resultSet.getString("FID")),
										new File(resultSet.getString("FILENAME"))));
									break;
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("getDLNAResourceFromSQL", e);
		} finally {
			MediaDatabase.close(connection);
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
