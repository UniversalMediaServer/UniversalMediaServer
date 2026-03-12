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
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
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
		"(?<property>((dc)|(upnp)):[A-Za-z@\\[\\]\"=]+)\\s+" +
		"(?<op>[A-Za-z=!<>]+)\\s+\"(?<val>([^\"]|\"\")*)\"", Pattern.CASE_INSENSITIVE);

	private static final Pattern LUCENE_PATTERN = Pattern.compile("([-+&|!(){}\\[\\]^\"~*?:/\\\\])");

	public record SearchToken(String attr, String op, String val) { }

	private static UmsConfiguration umsConfiguration;

	static {
		try {
			umsConfiguration = new UmsConfiguration();
		} catch (ConfigurationException | InterruptedException e) {
			LOGGER.error("Error while initializing SearchRequestHandler : ", e);
			throw new RuntimeException(e);
		}
	}

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

		int totalMatches = getLibraryResourceCountFromSQL(convertToCountSql(requestType, requestMessage));

		String sqlFiles = convertToFilesSql(requestMessage, requestType);
		for (StoreResource resource : getLibraryResourceFromSQL(renderer, sqlFiles, requestType)) {
			numberReturned++;
			dlnaItems.append(DidlHelper.getDidlString(resource));
		}

		// Build response message
		StringBuilder response = buildEnvelope(numberReturned, totalMatches, MediaStoreIds.getSystemUpdateId().getValue(), dlnaItems);
		return createResponse(response.toString());
	}

	private static String getFormattedLuceneString(String column, String sql, List<SearchToken> list, SearchRequest requestMessage, boolean ignoreCountLimit) {
		String title = getLuceneTitleMatch(list);
		int count = requestMessage.getRequestedCount() != null ? requestMessage.getRequestedCount() : 0;
		int startIndex = requestMessage.getStartingIndex() != null ? requestMessage.getStartingIndex() : 0;
		if (ignoreCountLimit) {
			return String.format(sql, column, title, 0, 0);
		} else {
			return String.format(sql, column, title, count, startIndex);
		}
	}

	private static String getFormattedLuceneString(String column, String sql, List<SearchToken> list, SearchRequest requestMessage) {
		return getFormattedLuceneString(column, sql, list, requestMessage, false);
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private static String addSqlSelectByType(DbIdMediaType requestType, List<SearchToken> list, SearchRequest requestMessage) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				String sql = "SELECT A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, FT.SCORE, F.ID AS OID FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A  ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("SONGNAME", sql, list, requestMessage);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT DISTINCT ON (FILENAME) A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ARTIST", sql, list, requestMessage);
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
				String sql = "SELECT DISTINCT ON (album) album, artist, media_year, genre, MBID_RECORD, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUM", sql, list, requestMessage);
			}
			case TYPE_PLAYLIST -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' AND ";
				return getFormattedLuceneString("ONLYFILENAME", sql, list, requestMessage);
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
	private static String addSqlSelectByType(DbIdMediaType requestType, List<SearchToken> list, String subtreeId, SearchRequest requestMessage) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				String sql = getTreeStatement(subtreeId) + "SELECT A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, F.ID AS OID, FT.SCORE " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("SONGNAME", sql, list, requestMessage);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ARTIST", sql, list, requestMessage);
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
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (album) album, artist, media_year, genre, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ALBUM", sql, list, requestMessage);
			}
			case TYPE_PLAYLIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ONLYFILENAME", sql, list, requestMessage);
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select DISTINCT ON (child.NAME) child.NAME, child.ID as FID, child.ID as oid, " +
					"parent.ID as parent_id from STORE_IDS parent" +
					getTreeWhereStatement(subtreeId, true);
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid FROM files " +
					getTreeWhereStatement(subtreeId, true);			}
			default -> throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type. We ignore the count and startIndex parameters, since the totalMatches count should
	 * be independent of the requested subset of data.
	 *
	 * @param requestType
	 * @param subtreeId
	 * @param list
	 * @return
	 */
	private static String addSqlSelectCountByType(DbIdMediaType requestType, List<SearchToken> list, SearchRequest requestMessage) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				String sql = "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("SONGNAME", sql, list, requestMessage, true);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ARTIST", sql, list, requestMessage, true);
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
				String sql = "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUM", sql, list, requestMessage, true);
			}
			case TYPE_PLAYLIST -> {
				String sql = "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' AND ";
				return getFormattedLuceneString("ONLYFILENAME", sql, list, requestMessage, true);
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

	/**
	 * Beginning part of SQL statement, by type. We ignore the count and startIndex parameters, since the totalMatches count should
	 * be independent of the requested subset of data.
	 *
	 * @param requestType
	 * @return
	 */
	private static String addSqlSelectCountByType(DbIdMediaType requestType, List<SearchToken> list, String subtreeId, SearchRequest requestMessage) {
		switch (requestType) {
			case TYPE_AUDIO -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("SONGNAME", sql, list, requestMessage, true);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ARTIST", sql, list, requestMessage, true);
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
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ALBUM", sql, list, requestMessage, true);
			}
			case TYPE_PLAYLIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] JOIN tree ON F.FILENAME = tree.name " +
					getTreeWhereStatement(subtreeId, true);
				return getFormattedLuceneString("ONLYFILENAME", sql, list, requestMessage, true);
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT F.id) FROM tree " +
					getTreeWhereStatement(subtreeId, true);
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT child.NAME) from tree JOIN STORE_IDS child on tree.id = child.id, STORE_IDS parent " +
					getTreeWhereStatement(subtreeId, true);
			}
			default -> throw new RuntimeException("not implemented request type : " + (requestType != null ? requestType : "NULL"));
		}
	}

	/**
	 * Attention: If a clients asks for an exact match or proximity search, it should put '"' around the search term.
	 * Example : dc:title contains "dark side of the moon".
	 *
	 * @param list
	 * @return
	 */
	private static String getLuceneTitleMatch(List<SearchToken> list) {
		String upnpClass = list.stream().filter(t -> "upnp:class".equalsIgnoreCase(t.attr)).findFirst().map(t -> t.val).orElse("");
		final String filterAttr;
		switch (upnpClass.toLowerCase()) {
			case "object.container.person.musicartist" -> {
				filterAttr = "dc:title";
			}
			case "object.item.audioitem" -> {
				filterAttr = "dc:title";
			}
			default -> {
				LOGGER.warn("unknown upnp:class {}. Fallback to dc:title search ... ", upnpClass);
				filterAttr = "dc:title";
			}
		}

		String title = list.stream().filter(t -> filterAttr.equalsIgnoreCase(t.attr)).findFirst().map(t -> t.val).orElse("");
		String op = list.stream().filter(t -> filterAttr.equalsIgnoreCase(t.attr)).findFirst().map(t -> t.op).orElse("");
		// Escape lucene special characters
		title = LUCENE_PATTERN.matcher(title).replaceAll("\\\\$1");
		if ("contains".equalsIgnoreCase(op)) {
			if (umsConfiguration.getLuceneContainsFuzzySearch()) {
				title = prepareLuceneSearch(title, "~2");
			} else {
				if (!(title.startsWith("\"") && title.endsWith("\""))) {
					LOGGER.debug("for classical contains logic, title must be between \"\".");
					title = prepareLuceneSearch(title, "*");
				}
			}
		}
		if ("=".equalsIgnoreCase(op)) {
			if (umsConfiguration.getLuceneEqualFuzzySearch()) {
				title = prepareLuceneSearch(title, "~2");
			}
		}
		title = title.replace("'", "''");
		LOGGER.debug("lucene search term is : {}", title);
		return title;
	}

	/**
	 * Lucene doesn't support proximity and fuzzy search at the same time. So we check if the search term is an exact match (between " ").
	 * If so, we use proximity search, else we split the search term into words and add the fuzzy or wildcard operator to each word.
	 *
	 * I think this is what the user expects most.
	 * @param title
	 * @param searchType
	 * @return
	 */
	private static String prepareLuceneSearch(String title, String searchType) {
		if (title.startsWith("\"") && title.endsWith("\"")) {
			LOGGER.debug("search request is for an Proximity Search ...");
			if ("*".equals(searchType)) {
				return title;
			}
			return title + searchType;
		}
		String[] words = title.split("\\s+");
		StringBuilder fuzzyQuery = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				if (fuzzyQuery.length() > 0) {
					fuzzyQuery.append(" ");
				}
				fuzzyQuery.append(word).append(searchType);
			}
		}
		return fuzzyQuery.toString();
	}

	public static String convertToFilesSql(SearchRequest requestMessage, DbIdMediaType requestType) {
		StringBuilder sb = new StringBuilder();
		String subtreeId = requestMessage.getContainerId();
		List<SearchToken> tokens = getSearchTokens(requestMessage.getSearchCriteria());
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectByType(requestType, tokens, requestMessage));
		} else {
			sb.append(addSqlSelectByType(requestType, tokens, subtreeId, requestMessage));
		}
		addSqlWherePart(requestMessage.getSearchCriteria(), requestType, sb);
		addOrderBy(requestMessage.getSortCriteria(), requestType, sb);
		addLimit(requestType, requestMessage, sb);
		LOGGER.debug(sb.toString());
		return sb.toString();
	}

	/**
	 * For lucene searches, be aware, that the result set is already ordered by relevance (score). By adding an additional order by clause,
	 * we might mess up the relevance ordering.
	 *
	 * @param sortCriteria
	 * @param requestType
	 * @param sb
	 */
	private static void addOrderBy(String sortCriteria, DbIdMediaType requestType, StringBuilder sb) {
		switch (requestType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PLAYLIST, TYPE_PERSON -> {
				// do nothing, since the FTL search already delivers the correct subset of data based on the startingIndex and requestedCount parameters.
				}
			default -> {
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
						LOGGER.debug("ERROR while processing 'addOrderBy'", e);
					}
				}
				sb.append(String.format(" oid "));
			}
		}
	}

	private static String sortOrder(String order) {
		if ("+".equals(order)) {
			return " ASC ";
		} else if ("-".equals(order)) {
			return " DESC ";
		}
		return "";
	}

	private static void addLimit(DbIdMediaType requestType, SearchRequest requestMessage, StringBuilder sb) {
		switch (requestType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PERSON, TYPE_PLAYLIST -> {
				// do nothing, since the FTL search already delivers the correct subset of data based on the startingIndex and requestedCount parameters.
				}
			default -> {
				long limit = requestMessage.getRequestedCount();
				if (limit == 0) {
					limit = 999; // performance issue: do only deliver top 999 items
				}
				sb.append(String.format(" LIMIT %d OFFSET %d ", limit, requestMessage.getStartingIndex()));
			}
		}
	}

	public static String convertToCountSql(DbIdMediaType requestType, SearchRequest requestMessage) {
		StringBuilder sb = new StringBuilder();
		String subtreeId = requestMessage.getContainerId();
		String upnpSearch = requestMessage.getSearchCriteria();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectCountByType(requestType, getSearchTokens(upnpSearch), requestMessage));
		} else {
			sb.append(addSqlSelectCountByType(requestType, getSearchTokens(upnpSearch), subtreeId, requestMessage));
		}
		addSqlWherePart(upnpSearch, requestType, sb);
		return sb.toString();
	}

	private static List<SearchToken> getSearchTokens(String searchCriteria) {
		List<SearchToken> result = new ArrayList<>();
		Matcher matcher = TOKENIZER_PATTERN.matcher(searchCriteria);
		while (matcher.find()) {
			result.add(new SearchToken(matcher.group("property"), matcher.group("op"), makeClean(matcher.group("val"))));
		}
		return result;
	}

	private static String makeClean(String val) {
		return val.replace("\"\"", "\"");
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
	 * If we want to support requests like "*searchTerm*" , we would need to add the 'default' logic to the search term instead of escaping it.
	 * However, this would disable the lucene fuzzy and proximity search! This could be counter intuitive for users! That's
	 * why it's not implemented.
	 *
	 * All container lookups should put it's search term in "dc:title". As fallback some musicItem properties are also accepted.
	 *
	 * @param sb
	 * @param property
	 * @param op
	 * @param val
	 * @param requestType
	 */
	private static void appendProperty(StringBuilder sb, String property, String op, String val, DbIdMediaType requestType) {
		switch (requestType) {
			case TYPE_AUDIO, TYPE_PLAYLIST -> {
				if ("dc:title".equalsIgnoreCase(property)) {
					LOGGER.trace("type / property {} is indexed by lucene. Ignore this property for SQL generation.", property);
					sb.append("1 = 1 ");
				}
			}
			case TYPE_PERSON -> {
				if ("dc:title".equalsIgnoreCase(property) || property.toLowerCase().startsWith("upnp:artist")) {
					LOGGER.trace("type / property {} is indexed by lucene. Ignore this property for SQL generation.", property);
					sb.append("1 = 1 ");
				}
			}
			case TYPE_ALBUM -> {
				if ("dc:title".equalsIgnoreCase(property) || "upnp:album".equalsIgnoreCase(property)) {
					LOGGER.trace("type / property {} is indexed by lucene. Ignore this property for SQL generation.", property);
					sb.append("1 = 1 ");
				}
			}
			default -> {
				if ("=".equals(op)) {
					sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
				} else if ("contains".equals(op)) {
					sb.append(String.format("%s ILIKE '%%%s%%'", getField(property, requestType), escapeH2dbSql(val)));
				} else {
					throw new RuntimeException("unknown or unimplemented operator : " + op);
				}
			}
		}
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
			// Makes less sense in a  score based search. Maybe we can use this property in a future implementation to mark albums as
			// liked in the database and then use this information to boost the score of liked albums in the search results.
			return " ";
		} else if ("ums:score".equals(property)) {
			return " score ";
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
		} catch (Exception e) {
			LOGGER.warn("getLibraryResourceCountFromSQL", e);
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
											res.resolve();
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

	private static String getTreeStatement(String subtreeId) {
		String tree = String.format("WITH RECURSIVE tree(id, name) AS (\n" +
			"    SELECT id, name FROM STORE_IDS WHERE id = %s\n" +
			"    UNION ALL\n" +
			"    SELECT t.id, t.name FROM STORE_IDS t \n" +
			"    INNER JOIN tree ON t.parent_id = tree.id\n" +
			") ", subtreeId);

		return tree;
	}

	private static String getTreeWhereStatement(String subtreeId, boolean addAnd) {
		String tree = String.format(" WHERE EXISTS (\n" +
			"    SELECT 1 FROM tree \n" +
			"    WHERE F.FILENAME like tree.name || '%%%%'\n" +
			")\n" +
			"AND FT.\"TABLE\" = 'AUDIO_METADATA' ", subtreeId);
		if (addAnd) {
			tree = tree + " AND ";
		}
		return tree;
	}
}
