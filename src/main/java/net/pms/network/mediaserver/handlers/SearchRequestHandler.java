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
import net.pms.database.MediaDatabase;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.DbIdTypeAndIdent;
import net.pms.dlna.RealFileDbId;
import net.pms.dlna.api.MusicBrainzAlbum;
import net.pms.dlna.virtual.VirtualFolderDbId;
import net.pms.formats.Format;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	protected static DbIdMediaType getRequestType(String searchCriteria) {
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

	public StringBuilder createSearchResponse(SearchRequest requestMessage, Renderer renderer) {
		int numberReturned = 0;
		StringBuilder dlnaItems = new StringBuilder();
		DbIdMediaType requestType = getRequestType(requestMessage.getSearchCriteria());

		int totalMatches = getDLNAResourceCountFromSQL(convertToCountSql(requestMessage.getSearchCriteria(), requestType));

		VirtualFolderDbId folder = new VirtualFolderDbId("Search Result", new DbIdTypeAndIdent(requestType, ""), "");
		String sqlFiles = convertToFilesSql(requestMessage, requestType);
		for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles, requestType)) {
			folder.addChild(resource);
		}

		folder.discoverChildren();
		for (DLNAResource uf : folder.getChildren()) {
			numberReturned++;
			uf.resolve();
			uf.setFakeParentId("0");
			dlnaItems.append(uf.getDidlString(renderer));
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, updateID.getAndIncrement(), dlnaItems);
		return createResponse(response.toString());
	}

	public BrowseResult createSearchResponse(
		String containerId,
		String searchCriteria,
		String filter,
		long startingIndex,
		long requestedCount,
		SortCriterion[] orderBy,
		Renderer renderer
	) {
		int numberReturned = 0;
		StringBuilder dlnaItems = new StringBuilder();
		DbIdMediaType requestType = getRequestType(searchCriteria);

		int totalMatches = getDLNAResourceCountFromSQL(convertToCountSql(searchCriteria, requestType));

		VirtualFolderDbId folder = new VirtualFolderDbId("Search Result", new DbIdTypeAndIdent(requestType, ""), "");
		String sqlFiles = convertToFilesSql(searchCriteria, startingIndex, requestedCount, orderBy, requestType);
		for (DLNAResource resource : getDLNAResourceFromSQL(sqlFiles, requestType)) {
			folder.addChild(resource);
		}

		folder.discoverChildren();
		for (DLNAResource uf : folder.getChildren()) {
			numberReturned++;
			uf.resolve();
			uf.setFakeParentId("0");
			dlnaItems.append(uf.getDidlString(renderer));
		}

		return new BrowseResult(dlnaItems.toString(), numberReturned, totalMatches, updateID.getAndIncrement());
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private static String addSqlSelectByType(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				return "select A.RATING, FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
			}
			case TYPE_PERSON -> {
				return "select DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST) as FILENAME, A.ID as oid from AUDIOTRACKS as A where ";
			}
			case TYPE_ALBUM -> {
				return "select DISTINCT mbid_release as liked, MBID_RECORD, album, artist, media_year, ALBUM as FILENAME, A.ID as oid, A.MBID_RECORD from MUSIC_BRAINZ_RELEASE_LIKE as m right outer join AUDIOTRACKS as a on m.mbid_release = A.mbid_record where ";
			}
			case TYPE_PLAYLIST -> {
				return "select DISTINCT FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private static String addSqlSelectCountByType(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				return "select count(DISTINCT F.id) from FILES as F left outer join AUDIOTRACKS as A on F.ID = A.FILEID where ";
			}
			case TYPE_PERSON -> {
				return "select count (DISTINCT COALESCE(A.ALBUMARTIST, A.ARTIST)) from AUDIOTRACKS as A where ";
			}
			case TYPE_ALBUM -> {
				return "select count(DISTINCT A.id) from AUDIOTRACKS as A where ";
			}
			case TYPE_PLAYLIST -> {
				return "select count(DISTINCT F.id) from FILES as F where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return "select count(DISTINCT F.id) from FILES as F where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	protected static String convertToFilesSql(SearchRequest requestMessage, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectByType(requestType));
		addSqlWherePart(requestMessage.getSearchCriteria(), requestType, sb);
		addOrderBy(requestMessage.getSortCriteria(), requestType, sb);
		addLimit(requestMessage.getStartingIndex(), requestMessage.getRequestedCount(), sb);
		LOGGER.trace(sb.toString());
		return sb.toString();
	}

	private static String convertToFilesSql(String searchCriteria, long startingIndex, long requestedCount, SortCriterion[] orderBy, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectByType(requestType));
		addSqlWherePart(searchCriteria, requestType, sb);
		addOrderBy(orderBy, requestType, sb);
		addLimit(startingIndex, requestedCount, sb);
		LOGGER.trace(sb.toString());
		return sb.toString();
	}

	private static void addOrderBy(SortCriterion[] orderBy, DbIdMediaType requestType, StringBuilder sb) {
		sb.append(" ORDER BY ");
		try {
			for (SortCriterion sort : orderBy) {
				if (!StringUtils.isAllBlank(sort.getPropertyName())) {
					String field = getField(sort.getPropertyName(), requestType);
					if (!StringUtils.isAllBlank(field)) {
						sb.append(field);
						sb.append(sort.isAscending() ? " ASC " : " DESC ");
						sb.append(", ");
					}
				}
			}
		} catch (Exception e) {
			LOGGER.trace("ERROR while processing 'addOrderBy'");
		}
		sb.append(String.format(" oid "));
	}

	private static void addOrderBy(String sortCriteria, DbIdMediaType requestType, StringBuilder sb) {
		sb.append(" ORDER BY ");
		if (!StringUtils.isAllBlank(sortCriteria)) {
			String[] sortElements = sortCriteria.split("[;, ]");
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

	private static String sortOrder(String order) {
		if ("+".equals(order)) {
			return " ASC ";
		} else if ("-".equals(order)) {
			return " DESC ";
		}
		return "";
	}

	private static void addLimit(long startingIndex, long requestedCount, StringBuilder sb) {
		long limit = requestedCount;
		if (limit == 0) {
			limit = 999; // performance issue: do only deliver top 999 items
		}
		sb.append(String.format(" LIMIT %d OFFSET %d ", limit, startingIndex));
	}

	private static String convertToCountSql(String upnpSearch, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectCountByType(requestType));
		addSqlWherePart(upnpSearch, requestType, sb);
		return sb.toString();
	}

	private static void addSqlWherePart(String searchCriteria, DbIdMediaType requestType, StringBuilder sb) {
		int lastIndex = 0;
		Matcher matcher = TOKENIZER_PATTERN.matcher(searchCriteria);
		while (matcher.find()) {
			sb.append(searchCriteria, lastIndex, matcher.start());
			if ("upnp:class".equalsIgnoreCase(matcher.group("property"))) {
				acquireDatabaseType(sb, matcher.group("op"), matcher.group("val"), requestType);
			} else if (matcher.group("property").startsWith("upnp:") || matcher.group("property").startsWith("dc:")) {
				appendProperty(sb, matcher.group("property"), matcher.group("op"), matcher.group("val"), requestType);
			}
			sb.append("");
			lastIndex = matcher.end();
		}
		if (lastIndex < searchCriteria.length()) {
			sb.append(searchCriteria, lastIndex, searchCriteria.length());
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
	private static void appendProperty(StringBuilder sb, String property, String op, String val, DbIdMediaType requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("LOWER(%s) LIKE '%%%s%%'", getField(property, requestType), val.toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private static String getField(String property, DbIdMediaType requestType) {
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

	private static String getTitlePropertyMapping(DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				return " A.SONGNAME ";
			}
			case TYPE_ALBUM -> {
				return " A.ALBUM ";
			}
			case TYPE_PERSON -> {
				return " COALESCE(A.ALBUMARTIST, A.ARTIST) ";
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				return " F.FILENAME ";
			}
			default -> {
				//nothing to do
			}
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	private static void acquireDatabaseType(StringBuilder sb, String op, String val, DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_ALBUM, TYPE_PERSON -> {
				sb.append(" 1=1 ");
				return;
			}
			case TYPE_AUDIO, TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
					sb.append(String.format(" F.FORMAT_TYPE = %d ", getFileType(requestType)));
				}
				return;
			}
			default -> {
				//nothing to do
			}
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	/**
	 * unpn:class filetype mapping
	 *
	 * @param val
	 * @return
	 */
	private static int getFileType(DbIdMediaType mediaFolderType) {
		// album and persons titles are stored within the RealFile and have
		// therefore no unique id.
		switch (mediaFolderType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PERSON -> {
				return Format.AUDIO;
			}
			case TYPE_VIDEO -> {
				return Format.VIDEO;
			}
			case TYPE_IMAGE -> {
				return Format.IMAGE;
			}
			case TYPE_PLAYLIST -> {
				return Format.PLAYLIST;
			}
			default -> {
				//nothing to do
			}
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	private static int getDLNAResourceCountFromSQL(String query) {
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
	private static List<DLNAResource> getDLNAResourceFromSQL(String query, DbIdMediaType type) {
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
								case TYPE_ALBUM -> {
									String mbid = resultSet.getString("MBID_RECORD");
									if (StringUtils.isAllBlank(mbid)) {
										filesList.add(new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent(type, filenameField), ""));
									} else {
										if (!foundMbidAlbums.contains(mbid)) {
											VirtualFolderDbId albumFolder = new VirtualFolderDbId(filenameField,
													new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid), "");
											MusicBrainzAlbum album = new MusicBrainzAlbum(resultSet.getString("MBID_RECORD"),
													resultSet.getString("album"), resultSet.getString("artist"), resultSet.getInt("media_year"));
											DbIdResourceLocator.appendAlbumInformation(album, albumFolder);
											filesList.add(albumFolder);
											foundMbidAlbums.add(mbid);
										}
									}
								}
								case TYPE_PERSON -> filesList.add(new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent(type, filenameField), ""));
								case TYPE_PLAYLIST -> filesList.add(new VirtualFolderDbId(filenameField, new DbIdTypeAndIdent(type, resultSet.getString("FID")), ""));
								default -> {
									String realFileName = resultSet.getString("FILENAME");
									if (realFileName != null) {
										filesList.add(new RealFileDbId(new DbIdTypeAndIdent(type, resultSet.getString("FID")),
											new File(realFileName)));
									}
								}
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
	private static StringBuilder createResponse(String payload) {
		StringBuilder response = new StringBuilder();
		response.append(HTTPXMLHelper.XML_HEADER).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER).append(CRLF);
		response.append(payload).append(CRLF);
		response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER).append(CRLF);
		return response;
	}

	private static StringBuilder buildEnvelope(int foundNumberReturned, int totalMatches, int updateID, StringBuilder dlnaItems) {
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
