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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.store.DbIdMediaType;

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
public class LucenseSearchRequestHandler extends BaseSearchRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LucenseSearchRequestHandler.class.getName());
	private static final Pattern LUCENE_PATTERN = Pattern.compile("([-+&|!(){}\\[\\]^\"~*?:/\\\\])");


	public LucenseSearchRequestHandler(SearchRequest requestMessage) {
		super(requestMessage);
	}


	private String getFormattedLuceneString(String column, String sql, boolean ignoreCountLimit) {
		String title = getLuceneTitleMatch(getTokens());
		int count = getRequestMessage().getRequestedCount() != null ? getRequestMessage().getRequestedCount() : 0;
		int startIndex = getRequestMessage().getStartingIndex() != null ? getRequestMessage().getStartingIndex() : 0;
		if (ignoreCountLimit) {
			return String.format(sql, column, title, 0, 0);
		} else {
			return String.format(sql, column, title, count, startIndex);
		}
	}

	private String getFormattedLuceneString(String column, String sql) {
		return getFormattedLuceneString(column, sql, false);
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
				String sql = "SELECT FT.SCORE, A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, FT.SCORE, F.ID AS OID FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A  ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("SONGNAME", sql);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ARTIST", sql);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.CONDUCTOR as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("CONDUCTOR", sql);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.COMPOSER as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("COMPOSER", sql);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ALBUMARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUMARTIST", sql);
			}
			case TYPE_ALBUM -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, album, album as FILENAME, artist, media_year, genre, DISCOGS_RELEASE_ID, MBID_RECORD, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUM", sql);
			}
			case TYPE_PLAYLIST -> {
				String sql = "SELECT DISTINCT ON (FILENAME) FT.SCORE, FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' AND ";
				return getFormattedLuceneString("ONLYFILENAME", sql);
			}
			case TYPE_FOLDER -> {
				return "select DISTINCT ON (child.NAME) child.NAME, child.ID as FID, child.ID as oid, parent.ID as parent_id from STORE_IDS child, STORE_IDS parent where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid from FILES as F where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
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
				String sql = getTreeStatement(subtreeId) + "SELECT FT.SCORE, A.RATING, A.GENRE, F.FILENAME, F.MODIFIED, F.ID AS FID, F.ID AS OID, FT.SCORE " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("SONGNAME", sql);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ARTIST", sql);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.CONDUCTOR as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("CONDUCTOR", sql);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.COMPOSER as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("COMPOSER", sql);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, A.ALBUMARTIST as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ALBUMARTIST", sql);
			}
			case TYPE_ALBUM -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, album, album as FILENAME, artist, media_year, genre, DISCOGS_RELEASE_ID, MBID_RECORD, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ALBUM", sql);
			}
			case TYPE_PLAYLIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT DISTINCT ON (FILENAME) FT.SCORE, FILENAME, ONLYFILENAME, MODIFIED, F.ID as FID, F.ID as oid " +
					"FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] " +
					getTreeWhereStatement("FILES", subtreeId, true);
				return getFormattedLuceneString("ONLYFILENAME", sql);
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select DISTINCT ON (child.NAME) child.NAME, child.ID as FID, child.ID as oid, " +
					"parent.ID as parent_id from STORE_IDS parent" +
					getTreeWhereStatement("FILES", subtreeId, true);
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid FROM files " +
					getTreeWhereStatement("FILES", subtreeId, true);			}
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
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
	private String addSqlSelectCountByType() {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("SONGNAME", sql, true);
			}
			case TYPE_PERSON -> {
				String sql = "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ARTIST", sql, true);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = "SELECT COUNT(DISTINCT A.CONDUCTOR) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("CONDUCTOR", sql, true);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = "SELECT COUNT(DISTINCT A.COMPOSER) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("COMPOSER", sql, true);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = "SELECT COUNT(DISTINCT A.ALBUMARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUMARTIST", sql, true);
			}
			case TYPE_ALBUM -> {
				String sql = "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] " +
					"JOIN FILES F ON F.ID = A.FILEID WHERE FT.\"TABLE\" = 'AUDIO_METADATA' AND ";
				return getFormattedLuceneString("ALBUM", sql, true);
			}
			case TYPE_PLAYLIST -> {
				String sql = "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] WHERE FT.\"TABLE\" = 'FILES' AND ";
				return getFormattedLuceneString("ONLYFILENAME", sql, true);
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return "select count(DISTINCT F.id) from FILES as F where ";
			}
			case TYPE_FOLDER -> {
				return "select count(DISTINCT child.NAME) from STORE_IDS child, STORE_IDS parent where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type. We ignore the count and startIndex parameters, since the totalMatches count should
	 * be independent of the requested subset of data.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectCountByType(String subtreeId) {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(*) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("SONGNAME", sql, true);
			}
			case TYPE_PERSON -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ARTIST", sql, true);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.CONDUCTOR) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("CONDUCTOR", sql, true);
			}
			case TYPE_PERSON_COMPOSER -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.COMPOSER) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("COMPOSER", sql, true);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ALBUMARTIST) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ALBUMARTIST", sql, true);
			}
			case TYPE_ALBUM -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT A.ALBUM) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN AUDIO_METADATA A ON A.FILEID = FT.KEYS[1] JOIN FILES F ON F.ID = A.FILEID " +
					getTreeWhereStatement("AUDIO_METADATA", subtreeId, true);
				return getFormattedLuceneString("ALBUM", sql, true);
			}
			case TYPE_PLAYLIST -> {
				String sql = getTreeStatement(subtreeId) + "SELECT COUNT(DISTINCT F.ID) FROM FTL_SEARCH_DATA('%s:%s', %d, %d) FT " +
					"JOIN FILES F ON F.ID = FT.KEYS[1] JOIN tree ON F.FILENAME = tree.name " +
					getTreeWhereStatement("FILES", subtreeId, true);
				return getFormattedLuceneString("ONLYFILENAME", sql, true);
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT F.id) FROM tree " +
					getTreeWhereStatement("FILES", subtreeId, true);
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT child.NAME) from tree JOIN STORE_IDS child on tree.id = child.id, STORE_IDS parent " +
					getTreeWhereStatement("FILES", subtreeId, true);
			}
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Attention: If a clients asks for an exact match or proximity search, it should put '"' around the search term.
	 * Example : dc:title contains "dark side of the moon".
	 *
	 * @param list
	 * @return
	 */
	private String getLuceneTitleMatch(List<SearchToken> list) {
		final String filterAttr = "dc:title";
		// Just in case we need to map the request type to a different title property, we can do this here.
		/*
		String upnpClass = list.stream().filter(t -> "upnp:class".equalsIgnoreCase(t.attr)).findFirst().map(t -> t.val).orElse("");
		switch (upnpClass.toLowerCase()) {
			default -> {
				filterAttr = "dc:title";
			}
		}
		*/

		String title = list.stream().filter(t -> filterAttr.equalsIgnoreCase(t.attr())).findFirst().map(t -> t.val()).orElse("");
		String op = list.stream().filter(t -> filterAttr.equalsIgnoreCase(t.attr())).findFirst().map(t -> t.op()).orElse("");
		// Escape lucene special characters
		title = LUCENE_PATTERN.matcher(title).replaceAll("\\\\$1");
		if ("contains".equalsIgnoreCase(op)) {
			if (getUmsConfiguration().getLuceneContainsFuzzySearch()) {
				title = prepareLuceneSearch(title, "~2");
			} else {
				if (!(title.startsWith("\"") && title.endsWith("\""))) {
					LOGGER.debug("for classical contains logic, title must be between \"\".");
					title = prepareLuceneSearch(title, "*");
				}
			}
		}
		if ("=".equalsIgnoreCase(op)) {
			if (getUmsConfiguration().getLuceneEqualFuzzySearch()) {
				title = prepareLuceneSearch(title, "~2");
			}
		}
		title = title.replace("'", "''");
		LOGGER.debug("lucene search term is : {}", title);
		if (StringUtils.isBlank(title)) {
			LOGGER.warn("no search term found for lucene search.");
			for (SearchToken searchToken : list) {
				LOGGER.debug("search token : {} {} {}", searchToken.attr(), searchToken.op(), searchToken.val());
			}
		}
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
	private String prepareLuceneSearch(String title, String searchType) {
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

	protected String convertToFilesSql() {
		StringBuilder sb = new StringBuilder();
		String subtreeId = getRequestMessage().getContainerId();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectByType());
		} else {
			sb.append(addSqlSelectByType(subtreeId));
		}
		addSqlWherePart(getRequestMessage().getSearchCriteria(), getRequestType(), sb);
		addOrderBy(getRequestMessage().getSortCriteria(), getRequestType(), sb);
		addLimit(getRequestType(), getRequestMessage(), sb);
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
	private void addOrderBy(String sortCriteria, DbIdMediaType requestType, StringBuilder sb) {
		sb.append(" ORDER BY ");
		switch (requestType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PLAYLIST, TYPE_PERSON -> {
				sb.append("FT.SCORE DESC, ");
				}
			default -> { }
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
		switch (requestType) {
			case TYPE_AUDIO, TYPE_ALBUM, TYPE_PERSON, TYPE_PLAYLIST -> {
				// do nothing, since the FTL search already delivers the correct subset of data based on the startingIndex and requestedCount parameters.
				}
			default -> {
				long limit = getRequestMessage().getRequestedCount();
				if (limit == 0) {
					limit = 999; // performance issue: do only deliver top 999 items
				}
				sb.append(String.format(" LIMIT %d OFFSET %d ", limit, getRequestMessage().getStartingIndex()));
			}
		}
	}

	protected String convertToCountSql() {
		StringBuilder sb = new StringBuilder();
		String subtreeId = getRequestMessage().getContainerId();
		String upnpSearch = getRequestMessage().getSearchCriteria();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectCountByType());
		} else {
			sb.append(addSqlSelectCountByType(subtreeId));
		}
		addSqlWherePart(upnpSearch, getRequestType(), sb);
		return sb.toString();
	}

	private void addSqlWherePart(String searchCriteria, DbIdMediaType requestType, StringBuilder sb) {
		int lastIndex = 0;
		Matcher matcher = SearchRequestTokenizer.TOKENIZER_PATTERN.matcher(searchCriteria);
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

		switch (requestType) {
			case TYPE_FOLDER -> {
				sb.append(" AND child.parent_id = parent.id and child.object_type = 'RealFolder' and parent.object_type = 'RealFolder'");
			}
			default -> { }
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
	private void appendProperty(StringBuilder sb, String property, String op, String val, DbIdMediaType requestType) {
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

	private void acquireDatabaseType(StringBuilder sb, String op, String val, DbIdMediaType requestType) {
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


	private String getTreeStatement(String subtreeId) {
		String tree = String.format("WITH RECURSIVE tree(id, name) AS (\n" +
			"    SELECT id, name FROM STORE_IDS WHERE id = %s\n" +
			"    UNION ALL\n" +
			"    SELECT t.id, t.name FROM STORE_IDS t \n" +
			"    INNER JOIN tree ON t.parent_id = tree.id\n" +
			") ", subtreeId);

		return tree;
	}

	/**
	 * SubtreeId not used yet on purpose
	 * @param tableName
	 * @param subtreeId
	 * @param addAnd
	 * @return
	 */
	private String getTreeWhereStatement(String tableName, String subtreeId, boolean addAnd) {
		String tree = String.format(" WHERE EXISTS (\n" +
			"    SELECT 1 FROM tree \n" +
			"    WHERE F.FILENAME like tree.name || '%%%%'\n" +
			")\n" +
			"AND FT.\"TABLE\" = '%s' ", tableName);
		if (addAnd) {
			tree = tree + " AND ";
		}
		return tree;
	}
}
