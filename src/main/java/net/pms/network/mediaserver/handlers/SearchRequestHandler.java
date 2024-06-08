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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableAudioMetadata;
import net.pms.dlna.DidlHelper;
import net.pms.formats.Format;
import net.pms.media.audio.metadata.MusicBrainzAlbum;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdLibrary;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreResource;
import net.pms.store.container.MusicBrainzAlbumFolder;
import net.pms.store.container.MusicBrainzPersonFolder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
	private static final Pattern ARTIST_ROLE = Pattern.compile("upnp:class.*role\\s*=\\s*\"(?<val>.*?)\".*", Pattern.CASE_INSENSITIVE);

	private static final Pattern TOKENIZER_PATTERN = Pattern.compile(
		"(?<property>((\\bdc\\b)|(\\bupnp\\b)):[A-Za-z@\\[\\]\"=]+)\\s+(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>.*?)\"", Pattern.CASE_INSENSITIVE);

	public static DbIdMediaType getRequestType(String searchCriteria) {
		LOGGER.debug("search criteria : {}", searchCriteria);
		Matcher matcher = CLASS_PATTERN.matcher(searchCriteria);
		if (matcher.find()) {
			String propertyValue = matcher.group("val");
			LOGGER.trace("upnp:class is {}", propertyValue);
			if (propertyValue != null) {
				propertyValue = propertyValue.toLowerCase();
				// More specific types must be checked first
				if (propertyValue.startsWith("object.item.audioitem")) {
					return DbIdMediaType.TYPE_AUDIO;
				} else if (propertyValue.startsWith("object.item.videoitem")) {
					return DbIdMediaType.TYPE_VIDEO;
				} else if (propertyValue.startsWith("object.item.imageitem")) {
					return DbIdMediaType.TYPE_IMAGE;
				} else if (propertyValue.startsWith("object.container.person")) {
					return resolveRolePerson(searchCriteria);
				} else if (propertyValue.startsWith("object.container.album")) {
					return DbIdMediaType.TYPE_ALBUM;
				} else if (propertyValue.startsWith("object.container.playlistcontainer")) {
					return DbIdMediaType.TYPE_PLAYLIST;
				} else if (propertyValue.startsWith("object.container")) {
					return DbIdMediaType.TYPE_FOLDER;
				}
			}
		}
		throw new RuntimeException("Unknown type : " + (searchCriteria != null ? searchCriteria : "NULL"));
	}

	private static DbIdMediaType resolveRolePerson(String searchCriteria) {
		Matcher matcher = ARTIST_ROLE.matcher(searchCriteria);
		if (matcher.find()) {
			String roleValue = matcher.group("val");
			if ("composer".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist composer");
				return DbIdMediaType.TYPE_PERSON_COMPOSER;
			} else if ("conductor".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist conductor");
				return DbIdMediaType.TYPE_PERSON_CONDUCTOR;
			} else if ("AlbumArtist".equalsIgnoreCase(roleValue)) {
				LOGGER.debug("looking up artist AlbumArtist");
				return DbIdMediaType.TYPE_PERSON_ALBUMARTIST;
			}
			LOGGER.warn("unknown artist role {}. Fallback to artist search ... ", roleValue);
			return DbIdMediaType.TYPE_PERSON;
		} else {
			LOGGER.trace("artist without role. Regular artist search.");
			return DbIdMediaType.TYPE_PERSON;
		}
	}

	public StringBuilder createSearchResponse(SearchRequest requestMessage, Renderer renderer) {
		int numberReturned = 0;
		StringBuilder dlnaItems = new StringBuilder();
		DbIdMediaType requestType = getRequestType(requestMessage.getSearchCriteria());

		int totalMatches = getLibraryResourceCountFromSQL(convertToCountSql(requestMessage.getSearchCriteria(), requestType));

		String sqlFiles = convertToFilesSql(requestMessage, requestType);
		for (StoreResource resource : getLibraryResourceFromSQL(renderer, sqlFiles, requestType)) {
			numberReturned++;
			dlnaItems.append(DidlHelper.getDidlString(resource));
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, MediaStoreIds.getSystemUpdateId().getValue(), dlnaItems);
		return createResponse(response.toString());
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
				return "select A.RATING, A.GENRE, FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F left outer join AUDIO_METADATA as A on F.ID = A.FILEID where ";
			}
			case TYPE_PERSON -> {
				return "select DISTINCT ON (FILENAME) A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return "select DISTINCT ON (FILENAME) A.CONDUCTOR as FILENAME, A.A.AUDIOTRACK_ID as oid from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_COMPOSER -> {
				return "select DISTINCT ON (FILENAME) A.COMPOSER as FILENAME, A.AUDIOTRACK_ID as oid from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return "select DISTINCT ON (FILENAME) A.ALBUMARTIST as FILENAME, A.AUDIOTRACK_ID as oid from AUDIO_METADATA as A where ";
			}
			case TYPE_ALBUM -> {
				return "select DISTINCT ON (album) mbid_release as liked, MBID_RECORD, album, artist, media_year, genre, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid, A.MBID_RECORD from MUSIC_BRAINZ_RELEASE_LIKE as m right outer join AUDIO_METADATA as a on m.mbid_release = A.mbid_record where ";
			}
			case TYPE_PLAYLIST -> {
				return "select DISTINCT ON (FILENAME) FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			}
			case TYPE_FOLDER -> {
				return "select DISTINCT ON (child.NAME) child.NAME, child.ID as FID, child.ID as oid, parent.ID as parent_id from STORE_IDS child, STORE_IDS parent where ";
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
				return "select count(DISTINCT F.id) from FILES as F left outer join AUDIO_METADATA as A on F.ID = A.FILEID where ";
			}
			case TYPE_PERSON -> {
				return "select count (DISTINCT A.ARTIST) from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return "select count (DISTINCT A.CONDUCTOR) from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_COMPOSER -> {
				return "select count (DISTINCT A.COMPOSER) from AUDIO_METADATA as A where ";
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return "select count (DISTINCT A.ALBUMARTIST) from AUDIO_METADATA as A where ";
			}
			case TYPE_ALBUM -> {
				return "select count(DISTINCT A.AUDIOTRACK_ID) from AUDIO_METADATA as A where ";
			}
			case TYPE_PLAYLIST -> {
				return "select count(DISTINCT F.id) from FILES as F where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return "select count(DISTINCT F.id) from FILES as F where ";
			}
			case TYPE_FOLDER -> {
				return "select count(DISTINCT child.NAME) from STORE_IDS child, STORE_IDS parent where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	public static String convertToFilesSql(SearchRequest requestMessage, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		sb.append(addSqlSelectByType(requestType));
		addSqlWherePart(requestMessage.getSearchCriteria(), requestType, sb);
		addOrderBy(requestMessage.getSortCriteria(), requestType, sb);
		addLimit(requestMessage.getStartingIndex(), requestMessage.getRequestedCount(), sb);
		LOGGER.debug(sb.toString());
		return sb.toString();
	}

	public static String convertToFilesSql(String searchCriteria, long startingIndex, long requestedCount, SortCriterion[] orderBy,
		DbIdMediaType requestType) {
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

	public static String convertToCountSql(String upnpSearch, DbIdMediaType requestType) {
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
		if (requestType.equals(DbIdMediaType.TYPE_FOLDER)) {
			sb.append(" AND child.parent_id = parent.id and child.object_type = 'RealFolder' and parent.object_type = 'RealFolder'");
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
			sb.append(String.format("LOWER(%s) LIKE '%%%s%%'", getField(property, requestType), escapeH2dbSql(val).toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}

	private static String escapeH2dbSql(String val) {
		val = val.replaceAll("'", "''");

		// Unicode #2018 is send by iOS (since iOS11) if "Smart Punctuation" is
		// active.
		val = val.replaceAll("‘", "''");
		return val;
	}

	private static String getField(String prop, DbIdMediaType requestType) {
		String property = prop.toLowerCase();
		if ("dc:title".equalsIgnoreCase(property)) {
			// handle title by return type.
			return getTitlePropertyMapping(requestType);
		} else if (property.startsWith("upnp:artist")) {
			// check for @role=composer, @role=conductor or @role=albumartist
			if (property.contains("albumartist")) {
				return " A.ALBUMARTIST ";
			} else if (property.contains("composer")) {
				return " A." + MediaTableAudioMetadata.COL_COMPOSER + " ";
			} else if (property.contains("conductor")) {
				return " A." + MediaTableAudioMetadata.COL_CONDUCTOR + " ";
			}
			// no role, just the artist
			return " A.ARTIST ";
		} else if ("upnp:genre".equals(property)) {
			return " A.GENRE ";
		} else if ("dc:creator".equals(property)) {
			return " A.ALBUMARTIST ";
		} else if ("upnp:album".equals(property)) {
			return " A.ALBUM ";
		} else if ("upnp:rating".equals(property)) {
			return " rating ";
		} else if ("ums:likedalbum".equals(property)) {
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
				return " A.ARTIST ";
			}
			case TYPE_PERSON_COMPOSER -> {
				return " A.COMPOSER ";
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return " A.ALBUMARTIST ";
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return " A.CONDUCTOR ";
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				return " F.FILENAME ";
			}
			case TYPE_FOLDER -> {
				return " child.name ";
			}
			default -> {
				// nothing to do
			}
		}
		throw new RuntimeException("Unknown type : " + requestType);
	}

	private static void acquireDatabaseType(StringBuilder sb, String op, String val, DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_ALBUM, TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST, TYPE_FOLDER -> {
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
				// nothing to do
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
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST -> {
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
			case TYPE_FOLDER -> {
				// do nothing, where not in the FILES table, but in STORE_IDS
			}
			default -> {
				// nothing to do
			}
		}
		throw new RuntimeException("unknown or unimplemented mediafolder type : >" + mediaFolderType + "<");
	}

	public static int getLibraryResourceCountFromSQL(String query) {
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
					LOGGER.trace("getLibraryResourceCountFromSQL", e);
				}
			}
		} finally {
			MediaDatabase.close(connection);
		}
		return 0;
	}

	/**
	 * Converts SQL statements having 'FILENAME' as a result identifier. Makes a logical DB search and converts
	 * result set to items and container.
	 *
	 * @param query
	 * @return
	 *
	 * List of discovered CDS items and containers from the database.
	 */
	public static List<StoreResource> getLibraryResourceFromSQL(Renderer renderer, String query, DbIdMediaType type) {
		ArrayList<StoreResource> result = new ArrayList<>();

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
							String filenameField = extractDisplayName(resultSet, type);
							switch (type) {
								case TYPE_ALBUM -> {
									String mbid = resultSet.getString("MBID_RECORD");
									if (StringUtils.isAllBlank(mbid)) {
										// Regular albums can be discovered in the media library
										StoreResource sr = DbIdResourceLocator.getAlbumFromMediaLibrary(renderer, filenameField);
										if (sr != null) {
											result.add(sr);
										}
									} else {
										if (!foundMbidAlbums.contains(mbid)) {
											MusicBrainzAlbumFolder folder = DbIdResourceLocator.getLibraryResourceMusicBrainzFolder(
												renderer, new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid));
											if (folder == null) {
												MusicBrainzAlbum album = new MusicBrainzAlbum(mbid, resultSet.getString("album"), resultSet.getString("artist"),
													Integer.toString(resultSet.getInt("media_year")), resultSet.getString("genre"));
												folder = DbIdLibrary.addLibraryResourceMusicBrainzAlbum(renderer, album);
											}
											result.add(folder);
											foundMbidAlbums.add(mbid);
										}
									}
								}
								case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR, TYPE_PERSON_ALBUMARTIST -> {
									DbIdTypeAndIdent ti = new DbIdTypeAndIdent(type, filenameField);
									MusicBrainzPersonFolder personFolder = DbIdResourceLocator.getLibraryResourcePersonFolder(renderer, ti);
									if (personFolder == null) {
										personFolder = DbIdLibrary.addLibraryResourcePerson(renderer, ti);
									}
									result.add(personFolder);
								}
								case TYPE_PLAYLIST -> {
									String realFileName = resultSet.getString("FILENAME");
									if (realFileName != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourcePlaylist(renderer, realFileName);
										if (res != null) {
											result.add(res);
										}
									}
								}
								case TYPE_FOLDER -> {
									if (filenameField != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourceFolder(renderer, filenameField);
										if (res != null) {
											result.add(res);
										}
									}
								}
								default -> {
									String realFileName = resultSet.getString("FILENAME");
									if (realFileName != null) {
										StoreResource res = DbIdResourceLocator.getLibraryResourceRealFile(renderer, realFileName);
										if (res != null) {
											result.add(res);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("getLibraryResourceFromSQL", e);
		} finally {
			MediaDatabase.close(connection);
		}
		return result;
	}

	private static String extractDisplayName(ResultSet resultSet, DbIdMediaType type) throws SQLException {
		switch (type) {
			case TYPE_VIDEO, TYPE_PLAYLIST, TYPE_IMAGE, TYPE_AUDIO -> {
				return FilenameUtils.getBaseName(resultSet.getString("FILENAME"));
			}
			case TYPE_FOLDER -> {
				return resultSet.getString("name");
			}
			default -> {
				// artificial field 'filename' of a person or similar type is
				// already the final display name.
				return resultSet.getString("FILENAME");
			}
		}
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

	private static StringBuilder buildEnvelope(int foundNumberReturned, int totalMatches, long updateID, StringBuilder dlnaItems) {
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
