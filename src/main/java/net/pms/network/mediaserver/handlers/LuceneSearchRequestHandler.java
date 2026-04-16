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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.store.DbIdMediaType;

/**
 * This implementation of SearchRequestHandler uses the FTL search capabilities of the underlying H2 database to execute the search.
 *
 * The search criteria is parsed into an AST and then converted into a Lucene query string, which is then passed to the FTL_SEARCH_DATA
 * function in the SQL query.
 *
 * Lucene searched is implemented as follows:
 *
 * For a <code>contains</code> search like <code>dc:title contains "dark moon"</code> and configuration key
 *
 * <code>search_lucene_contains_fuzzy</code> is set to :
 *   - <code>TRUE  :</code> A fuzzy search is being done by converting it to dark~2 moon~2 (default OR syntax by default)
 *   - <code>FALSE :</code> A wildcard search is being done by converting it to dark* moon* (default OR syntax by default)
 *
 * For a <code>contains</code> search like <code>dc:title contains """dark moon"""</code> and configuration key
 * <code>search_lucene_contains_fuzzy</code> is set to :
 *   - <code>TRUE  :</code> A proximity search is being done by converting it to "dark moon"~2
 *   - <code>FALSE :</code> A wildcard search is being done by converting it to "dark moon"*
 *
 * For a <code>=</code> search like <code>dc:title = "dark moon"</code> and configuration key
 *   - <code>TRUE  :</code> A fuzzy search is being done by converting it to dark~2 moon~2 (default OR syntax by default)
 *   - <code>FALSE :</code> A exact search is being done by converting it to dark moon (default OR syntax by default)
 *
 * Proximity Search
 * ===================================================
 *
 * A Lucene Proximity Search is a search method that allows you to find terms located within a specific distance of each other in
 * the text. It is more flexible than an exact phrase search, as it allows for additional words between the search terms or even a
 * different word order.
 *
 * A proximity search is initiated by putting " around the words you want to search for.
 * Those " have to be escaped as "" in the UPnP search criteria. So instead of searching for dc:title contains "Dark moon" the search
 * criteria for a proximity search would be dc:title contains """Dark Moon""".
 *
 *
 * Fuzzy Search
 * ===================================================
 *
 * A Lucene Fuzzy Search is a search method that allows you to find terms that are similar to the search terms, even if they are not an
 * exact match. It is useful for finding results that may have spelling variations or typos.
 *
 * The above logic is implemented in the TermNode class.
 */
public class LuceneSearchRequestHandler extends BaseSearchRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LuceneSearchRequestHandler.class.getName());
	private String luceneQuery = null;
	public LuceneSearchRequestHandler(SearchRequest requestMessage) {
		super(requestMessage);

		Map<String, String> map = getRequestMapping();
		UpnpToLuceneConverter converter = new UpnpToLuceneConverter(this.getRequestMessage().getSearchCriteria(), map);
		luceneQuery = converter.convert();
		LOGGER.debug("lucene search string is : {}", luceneQuery);
	}

	/**
	 * We can handle all types with this implementation now ... If not do a check and return false here, so that the request is processed by another handler.
	 *
	 * @return
	 */
	public boolean canHandle() {
		return true;
	}

	private int getCount() {
		return getRequestMessage().getRequestedCount() != null ? getRequestMessage().getRequestedCount() : 0;
	}

	private int getStartIndex() {
		return getRequestMessage().getStartingIndex() != null ? getRequestMessage().getStartingIndex() : 0;
	}

	private String getFormattedLuceneString(String luceneQuery, String sql, boolean ignoreCountLimit) {
		try {
			// Don't use Lucene limit & offset feature, because in case we filter the result set additionally in the WHERE part
			// like FILETYPE = 16 for playlist, we can get wrong results.
			return String.format(sql, luceneQuery, 0, 0);
		} catch (Exception e) {
			throw new RuntimeException("Error formatting lucene query into sql", e);
		}
	}

	private String getFormattedLuceneString(String luceneQuery, String sql) {
		return getFormattedLuceneString(luceneQuery, sql, false);
	}

	private Map<String, String> getRequestMapping() {
		Map<String, String> map = new HashMap<>();
		// We ignore upnp:class nodes, since they are handled by "WHERE" clause, not by lucene index.
		map.put("upnp:class", null);
		map.put("upnp:artist", "ARTIST");
		map.put("dc:creator", "ARTIST");
		map.put("upnp:artist[@role=\"AlbumArtist\"]", "ALBUMARTIST");
		map.put("upnp:artist[@role=\"Composer\"]", "COMPOSER");
		map.put("upnp:artist[@role=\"Conductor\"]", "CONDUCTOR");
		map.put("upnp:genre", "GENRE");
		map.put("upnp:album", "ALBUM");


		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				map.put("dc:title", "SONGNAME");
			}
			case TYPE_PERSON -> {
				map.put("dc:title", "ARTIST");
				}
			case TYPE_PERSON_CONDUCTOR -> {
				map.put("dc:title", "CONDUCTOR");
			}
			case TYPE_PERSON_COMPOSER -> {
				map.put("dc:title", "COMPOSER");
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				map.put("dc:title", "ALBUMARTIST");
			}
			case TYPE_ALBUM -> {
				map.put("dc:title", "ALBUM");
			}
			case TYPE_PLAYLIST, TYPE_FOLDER, TYPE_VIDEO, TYPE_IMAGE -> {
				map.put("dc:title", "ONLYFILENAME");
			}
			default -> throw new RuntimeException(
				"not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}

		return map;
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectByType() {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = "SELECT A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, FT.SCORE, F.ID AS OID FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A  ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA'";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA'";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.CONDUCTOR as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.COMPOSER as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ALBUMARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_ALBUM -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, album, album as FILENAME, artist, media_year, genre, DISCOGS_RELEASE_ID, MBID_RECORD, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " + "JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' ";
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_FOLDER -> {
				return "select DISTINCT ON (tree.name) tree.name, tree.ID as FID, tree.ID as oid, parent.ID as parent_id from STORE_IDS tree JOIN STORE_IDS parent ON tree.parent_id = parent.id where ";
			}
			default -> throw new RuntimeException(
				"not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectByType(String subtreeId) {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT FT.SCORE, A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, F.ID AS OID, FT.SCORE " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, A.CONDUCTOR as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, A.COMPOSER as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ALBUMARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_ALBUM -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, album, album as FILENAME, artist, media_year, genre, DISCOGS_RELEASE_ID, MBID_RECORD, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT DISTINCT ON (FILENAME) FT.SCORE, FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s', %d, %d) FT " + "JOIN FILES F ON F.ID = FT.KEYS[1] " +
					getTreeWhereStatement("FILES", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql);
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select DISTINCT ON (tree.NAME) tree.NAME, tree.ID as FID, tree.ID as oid, " +
					"parent.ID as parent_id from tree JOIN STORE_IDS parent ON tree.parent_id = parent.id where ";
			}
			default -> throw new RuntimeException(
				"not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type. We ignore the count and
	 * startIndex parameters, since the totalMatches count should be independent
	 * of the requested subset of data.
	 *
	 * @param requestType
	 * @param subtreeId
	 * @param list
	 * @return
	 */
	private String addSqlSelectCountByType() {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = "SELECT COUNT(DISTINCT A.CONDUCTOR) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = "SELECT COUNT(DISTINCT A.COMPOSER) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = "SELECT COUNT(DISTINCT A.ALBUMARTIST) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_ALBUM -> {
				String sql = "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				String sql = "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' ";
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_FOLDER -> {
				return "select count(DISTINCT tree.name) from STORE_IDS tree JOIN STORE_IDS parent ON tree.parent_id = parent.id where ";
			}
			default -> throw new RuntimeException(
				"not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type. We ignore the count and
	 * startIndex parameters, since the totalMatches count should be independent
	 * of the requested subset of data.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectCountByType(String subtreeId) {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.CONDUCTOR) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.COMPOSER) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = getTreeStatement(subtreeId) +
					"SELECT COUNT(DISTINCT A.ALBUMARTIST) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_ALBUM -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, true);
			}
			case TYPE_PLAYLIST, TYPE_VIDEO, TYPE_IMAGE -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] JOIN tree ON F.FILENAME = tree.name " +
					getTreeWhereStatement("FILES", subtreeId, false);
				return getFormattedLuceneString(luceneQuery, sql, false);
			}
			case TYPE_FOLDER -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT tree.name) from tree JOIN STORE_IDS parent ON tree.parent_id = parent.id where ";
				return sql;
			}
			default -> throw new RuntimeException(
				"not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}


	protected String convertToFilesSql() {
		StringBuilder sb = new StringBuilder();
		String subtreeId = getRequestMessage().getContainerId();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectByType());
		} else {
			sb.append(addSqlSelectByType(subtreeId));
		}
		addSqlWherePart(sb);
		addOrderBy(getRequestMessage().getSortCriteria(), getRequestType(), sb);
		addLimit(getRequestType(), getRequestMessage(), sb);
		LOGGER.debug(sb.toString());
		return sb.toString();
	}

	/**
	 * For lucene searches, be aware, that the result set is already ordered by
	 * relevance (score). By adding an additional order by clause, we might mess
	 * up the relevance ordering.
	 *
	 * @param sortCriteria
	 * @param requestType
	 * @param sb
	 */
	private void addOrderBy(String sortCriteria, DbIdMediaType requestType, StringBuilder sb) {
		sb.append(" ORDER BY ");
		switch (requestType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PLAYLIST, TYPE_PERSON -> {
				sb.append("FT.SCORE DESC, ");
			}
			default -> {
			}
		}
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

	private String sortOrder(String order) {
		if ("+".equals(order)) {
			return " ASC ";
		} else if ("-".equals(order)) {
			return " DESC ";
		}
		return "";
	}

	private void addLimit(DbIdMediaType requestType, SearchRequest requestMessage, StringBuilder sb) {
		long limit = getCount();
		if (limit == 0) {
			limit = 999;
		}
		sb.append(String.format(" LIMIT %d OFFSET %d ", limit, getStartIndex()));
	}

	protected String convertToCountSql() {
		StringBuilder sb = new StringBuilder();
		String subtreeId = getRequestMessage().getContainerId();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectCountByType());
		} else {
			sb.append(addSqlSelectCountByType(subtreeId));
		}
		addSqlWherePart(sb);
		return sb.toString();
	}

	private void addSqlWherePart(StringBuilder sb) {
		switch (getRequestType()) {
			case TYPE_IMAGE, TYPE_VIDEO, TYPE_PLAYLIST -> {
				sb.append(String.format(" AND F.FORMAT_TYPE = %d ", getFileType()));
			}
			case TYPE_FOLDER -> {
				SearchRequestTokenizer tokenizer = new SearchRequestTokenizer(getRequestMessage());
				String title = tokenizer.getDcTitleValue();
				// We could also add a lucene index in the table STORE_IDS, but I think this is sufficient for now, since we expect rather few folders searches
				if (title != null) {
					sb.append(String.format(" tree.name ilike '%%%%%s%%%%' and tree.object_type = 'RealFolder' and parent.object_type = 'RealFolder'",
						title));
				} else {
					sb.append(" tree.object_type = 'RealFolder' and parent.object_type = 'RealFolder'");
				}
			}
			default -> {
			}
		}
	}


	private String getTreeStatement(String subtreeId) {
		String tree = String.format("WITH RECURSIVE tree(id, name, parent_id, object_type) AS (\n" +
			"    SELECT id, name, parent_id, object_type FROM STORE_IDS WHERE id = %s\n" +
			"    UNION ALL\n" + "    SELECT t.id, t.name, t.parent_id, t.object_type FROM STORE_IDS t \n" +
			"    INNER JOIN tree ON t.parent_id = tree.id\n" + ") ",
			subtreeId);

		return tree;
	}

	/**
	 * SubtreeId not used yet on purpose
	 *
	 * @param tableName
	 * @param subtreeId
	 * @param addAnd
	 * @return
	 */
	private String getTreeWhereStatement(String tableName, String subtreeId, boolean addAnd) {
		String tree = String.format(" WHERE EXISTS (\n" +
			"    SELECT 1 FROM tree \n" +
			"    WHERE FILENAME like tree.name || '%%%%' ESCAPE ''\n" +
			")\n" +
			"AND FT.\"TABLE\" = '%s' ", tableName);
		if (addAnd) {
			tree = tree + " AND ";
		}
		return tree;
	}

	@Override
	protected void handleException(Exception e) {
		MediaDatabase.recreateFtlIndex();
	}
}
