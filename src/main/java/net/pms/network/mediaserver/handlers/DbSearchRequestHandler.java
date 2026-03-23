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
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
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
public class DbSearchRequestHandler extends BaseSearchRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbSearchRequestHandler.class);


	public DbSearchRequestHandler(SearchRequest requestMessage) {
		super(requestMessage);
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
				return "select DISTINCT ON (album) mbid_release as liked, DISCOGS_RELEASE_ID, MBID_RECORD, album, artist, media_year, genre, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid, A.MBID_RECORD from MUSIC_BRAINZ_RELEASE_LIKE as m right outer join AUDIO_METADATA as a on m.mbid_release = A.mbid_record where ";
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
				return getTreeStatement(subtreeId) + "select A.RATING, A.GENRE, FILENAME, MODIFIED, F.ID as FID, F.ID as oid FROM tree JOIN FILES F ON F.FILENAME = tree.name left outer join AUDIO_METADATA as A on F.ID = A.FILEID where ";
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
				return "select DISTINCT ON (album) mbid_release as liked, DISCOGS_RELEASE_ID, MBID_RECORD, album, artist, media_year, genre, ALBUM as FILENAME, A.AUDIOTRACK_ID as oid, A.MBID_RECORD from MUSIC_BRAINZ_RELEASE_LIKE as m right outer join AUDIO_METADATA as a on m.mbid_release = A.mbid_record where ";
			}
			case TYPE_PLAYLIST -> {
				return getTreeStatement(subtreeId) + "select DISTINCT ON (FILENAME) FILENAME, MODIFIED, F.ID as FID, F.ID as oid FROM tree JOIN FILES F ON F.FILENAME = tree.name where ";
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select DISTINCT ON (child.NAME) child.NAME, child.ID as FID, child.ID as oid, parent.ID as parent_id from tree JOIN STORE_IDS child on tree.name = child.name, STORE_IDS parent where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select FILENAME, MODIFIED, F.ID as FID, F.ID as oid FROM tree JOIN FILES F ON F.FILENAME = tree.name where ";
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
	private  String addSqlSelectCountByType() {
		switch (getRequestType()) {
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
				return "select count(DISTINCT A.ALBUM) from AUDIO_METADATA as A where ";
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
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	/**
	 * Beginning part of SQL statement, by type.
	 *
	 * @param requestType
	 * @return
	 */
	private String addSqlSelectCountByType(String subtreeId) {
		switch (getRequestType()) {
			case TYPE_AUDIO -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT F.id) FROM tree JOIN FILES F ON F.FILENAME = tree.name left outer join AUDIO_METADATA as A on F.ID = A.FILEID where ";
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
				return "select count(DISTINCT A.ALBUM) from AUDIO_METADATA as A where ";
			}
			case TYPE_PLAYLIST -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT F.id) FROM tree JOIN FILES F ON F.FILENAME = tree.name where ";
			}
			case TYPE_VIDEO, TYPE_IMAGE -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT F.id) FROM tree JOIN FILES F ON F.FILENAME = tree.name where ";
			}
			case TYPE_FOLDER -> {
				return getTreeStatement(subtreeId) + "select count(DISTINCT child.NAME) from tree JOIN STORE_IDS child on tree.id = child.id, STORE_IDS parent where ";
			}
			default -> throw new RuntimeException("not implemented request type : " + (getRequestType() != null ? getRequestType() : "NULL"));
		}
	}

	public String convertToFilesSql() {
		StringBuilder sb = new StringBuilder();
		String subtreeId = getRequestMessage().getContainerId();
		if ("0".equals(subtreeId) || StringUtils.isAllBlank(subtreeId)) {
			sb.append(addSqlSelectByType());
		} else {
			sb.append(addSqlSelectByType(subtreeId));
		}
		addSqlWherePart(getRequestMessage().getSearchCriteria(), getRequestType(), sb);
		addOrderBy(getRequestMessage().getSortCriteria(), getRequestType(), sb);
		addLimit(getRequestMessage().getStartingIndex(), getRequestMessage().getRequestedCount(), sb);
		LOGGER.debug(sb.toString());
		return sb.toString();
	}

	private void addOrderBy(String sortCriteria, DbIdMediaType requestType, StringBuilder sb) {
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

	private  String sortOrder(String order) {
		if ("+".equals(order)) {
			return " ASC ";
		} else if ("-".equals(order)) {
			return " DESC ";
		}
		return "";
	}

	private  void addLimit(long startingIndex, long requestedCount, StringBuilder sb) {
		long limit = requestedCount;
		if (limit == 0) {
			limit = 999; // performance issue: do only deliver top 999 items
		}
		sb.append(String.format(" LIMIT %d OFFSET %d ", limit, startingIndex));
	}

	public String convertToCountSql() {
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
	private void appendProperty(StringBuilder sb, String property, String op, String val, DbIdMediaType requestType) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", getField(property, requestType), val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("%s ILIKE '%%%s%%'", getField(property, requestType), escapeH2dbSql(val)));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
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

	public int getLibraryResourceCountFromSQL(String query) {
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


	private String getTreeStatement(String subtreeId) {
		String tree = String.format("WITH RECURSIVE tree(id, parent_id, name) AS (" +
			"    SELECT id, parent_id, name" +
			"    FROM STORE_IDS" +
			"    WHERE id = %s" +
			"\n" +
			"    UNION ALL" +
			"\n" +
			"    SELECT t.id, t.parent_id, t.name" +
			"    FROM STORE_IDS t" +
			"    INNER JOIN tree ON t.parent_id = tree.id " +
			")\n" +
			"", subtreeId);

		return tree;
	}
}
