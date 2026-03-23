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

import net.pms.configuration.RendererConfigurations;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchRequestHandlerTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestHandlerTest.class.getName());
	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
	}

	@Test
	public void testGlobalPlaylistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
			"FT\\.SCORE,\\s*FILENAME,\\s*ONLYFILENAME,\\s*MODIFIED,\\s*F\\.ID\\s+as\\s+FID,\\s*F\\.ID\\s+as\\s+oid\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ONLYFILENAME:jazz~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'FILES'\\s+" +
			"AND\\s+F\\.FORMAT_TYPE\\s*=\\s*16\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testGlobalPlaylistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+F\\.ID\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ONLYFILENAME:jazz~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'FILES'\\s+AND\\s+F\\.FORMAT_TYPE\\s*=\\s*16\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testTreePlaylistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*WITH\\s+RECURSIVE\\s+tree\\s*\\(id,\\s*name\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id,\\s+name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*" +
			"\\)\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
			"FT\\.SCORE,.*?\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ONLYFILENAME:jazz~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s*" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'FILES'\\s*" +
			".*?" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testTreePlaylistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)WITH\\s+RECURSIVE\\s+tree\\s*\\(\\s*id\\s*,\\s*name\\s*\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id\\s*,\\s*name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id\\s*,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+" +
			"INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*\\)\\s+" +
			"SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+F\\.ID\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ONLYFILENAME:jazz~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+tree\\s+ON\\s+F\\.FILENAME\\s*=\\s*tree\\.name\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s+" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'FILES'\\s+AND\\s+F\\.FORMAT_TYPE\\s*=\\s*16\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testGlobalAlbumSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
				"FT\\.SCORE,.*?\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUM:spirit~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
				"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
				"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
				"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s*" +
				"(?:AND\\s+1\\s*=\\s*1\\s*)*.*?" +
				"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testGlobalAlbumSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ALBUM\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUM:spirit~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testTreeAlbumSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*WITH\\s+RECURSIVE\\s+tree\\s*\\(id,\\s*name\\)\\s+AS\\s*\\(\\s*" +
				"SELECT\\s+id,\\s+name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
				"UNION\\s+ALL\\s+" +
				"SELECT\\s+t\\.id,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*" +
				"\\)\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
				"FT\\.SCORE,.*?\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUM:spirit~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
				"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
				"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
				"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s*" +
				"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'.*?" +
				"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testTreeAlbumSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)WITH\\s+RECURSIVE\\s+tree\\s*\\(\\s*id\\s*,\\s*name\\s*\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id\\s*,\\s*name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id\\s*,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+" +
			"INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*\\)\\s+" +
			"SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ALBUM\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUM:spirit~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s+" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testGlobalArtistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.person\" and dc:title contains \"Rhye\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
			"FT\\.SCORE,\\s*A\\.ARTIST\\s+as\\s+FILENAME,\\s*A\\.AUDIOTRACK_ID\\s+as\\s+oid\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ARTIST:Rhye~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testGlobalArtistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.person\" and dc:title contains \"Rhye\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ARTIST\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ARTIST:Rhye~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testTreeArtistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.person\" and dc:title contains \"Rhye\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*WITH\\s+RECURSIVE\\s+tree\\s*\\(id,\\s*name\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id,\\s+name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*" +
			"\\)\\s*SELECT\\s+DISTINCT\\s+ON\\s*\\(FILENAME\\)\\s+" +
			"FT\\.SCORE,\\s*A\\.ARTIST\\s+as\\s+FILENAME,\\s*A\\.AUDIOTRACK_ID\\s+as\\s+oid\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ARTIST:Rhye~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s*" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}

	@Test
	public void testTreeArtistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.person\" and dc:title contains \"Rhye\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)WITH\\s+RECURSIVE\\s+tree\\s*\\(\\s*id\\s*,\\s*name\\s*\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id\\s*,\\s*name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id\\s*,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+" +
			"INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*\\)\\s+" +
			"SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ARTIST\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ARTIST:Rhye~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s+" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}


	@Test
	public void testTreeMusicItemSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)WITH\\s+RECURSIVE\\s+tree\\s*\\(id,\\s*name\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id,\\s+name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*" +
			"\\)\\s*SELECT\\s+FT\\.SCORE,.*?FT\\.SCORE\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'SONGNAME:Darc~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s*" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'.*?" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*"
		));
	}

	@Test
	public void testTreeMusicItemSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);

		assertTrue(sql.matches(
			"(?is)WITH\\s+RECURSIVE\\s+tree\\s*\\(\\s*id\\s*,\\s*name\\s*\\)\\s+AS\\s*\\(\\s*" +
			"SELECT\\s+id\\s*,\\s*name\\s+FROM\\s+STORE_IDS\\s+WHERE\\s+id\\s*=\\s*140\\s+" +
			"UNION\\s+ALL\\s+" +
			"SELECT\\s+t\\.id\\s*,\\s+t\\.name\\s+FROM\\s+STORE_IDS\\s+t\\s+" +
			"INNER\\s+JOIN\\s+tree\\s+ON\\s+t\\.parent_id\\s*=\\s*tree\\.id\\s*\\)\\s+" +
			"SELECT\\s+COUNT\\s*\\(\\s*\\*\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'SONGNAME:Darc~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+EXISTS\\s*\\(\\s*SELECT\\s+1\\s+FROM\\s+tree\\s+WHERE\\s+F\\.FILENAME\\s+LIKE\\s+tree\\.name\\s*\\|\\|\\s*'%'.*?\\)\\s+" +
			"AND\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+F\\.FORMAT_TYPE\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testGlobalMusicItemSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*\\*\\s*\\)\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'SONGNAME:Darc~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+F\\.FORMAT_TYPE\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testGlobalMusicItemSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+" +
			"FT\\.SCORE,\\s*A\\.RATING,\\s*A\\.GENRE,\\s*F\\.FILENAME,\\s*F\\.MODIFIED,\\s*F\\.ID\\s+AS\\s+FID,\\s*FT\\.SCORE,\\s*F\\.ID\\s+AS\\s+OID\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'SONGNAME:Darc~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+" +
			"AND\\s+F\\.FORMAT_TYPE\\s*=\\s*1\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"ORDER\\s+BY\\s+FT\\.SCORE\\s+DESC,\\s*oid\\s*$"
		));
	}


	@Test
	public void testVideoFileSqlStatement() {
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(s);
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String result = searchRequestHandler.convertToFilesSql();
		LOG.info(result);  // \\s+
		assertTrue(result.matches(
				"select\\s+FILENAME\\s*,\\s*MODIFIED\\s*,\\s*F\\.ID\\s+as\\s+FID\\s*,\\s*F\\.ID\\s+as\\s+oid\\s+from\\s+FILES\\s+as\\s+F\\s+where\\s*\\(\\s*F\\.FORMAT_TYPE\\s*=\\s*4\\s*\\)\\s*ORDER\\s+BY\\s+oid\\s+LIMIT\\s+999\\s+OFFSET\\s+0\\s*"));
	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS) for Composer
	 */
	@Test
	public void testLinnAppComposerSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"tchaikovsky\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.COMPOSER\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'COMPOSER:'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"AND\\s+A\\.COMPOSER\\s+ILIKE\\s+'%tchaikovsky%'\\s*$"
		));	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS) for Composer
	 */
	@Test
	public void testLinnAppConductorSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"bernstein\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.CONDUCTOR\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'CONDUCTOR:.*'\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+" +
			"AND\\s+A\\.CONDUCTOR\\s+ILIKE\\s+'%bernstein%'"
		));
	}

	/**
	 * Test for an album search.
	 */
	@Test
	public void testAlbumSearchGlobal() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.album.musicAlbum\" and dc:title contains \"spirit\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ALBUM\\s*\\)\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUM:spirit~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	@Test
	public void testAlbumArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"tchaikovsky\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?is)^\\s*SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ALBUMARTIST\\s*\\)\\s+" +
			"FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ALBUMARTIST:'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+" +
			"JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+" +
			"JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+" +
			"WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s*" +
			"(?:AND\\s+1\\s*=\\s*1\\s*)*" +
			"AND\\s+A\\.ALBUMARTIST\\s+ILIKE\\s+'%tchaikovsky%'\\s*$"
		));
	}

	@Test
	public void testArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"tchaikovsky\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*DISTINCT\\s+A\\.ARTIST\\s*\\)\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'ARTIST:tchaikovsky~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT\\s+JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A\\.FILEID\\s*=\\s*FT\\.KEYS\\[1\\]\\s+JOIN\\s+FILES\\s+F\\s+ON\\s+F\\.ID\\s*=\\s*A\\.FILEID\\s+WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'\\s+AND\\s+1\\s*=\\s*1\\s+and\\s+1\\s*=\\s*1\\s*"
		));
	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS). Tests if special char "'" is escaped.
	 */
	@Test
	public void testLinnAppSpecialCharSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"love don't\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
		assertTrue(sql.matches(
			"(?i)SELECT\\s+COUNT\\s*\\(\\s*\\*\\s*\\)\\s+FROM\\s+FTL_SEARCH_DATA\\s*\\(\\s*'SONGNAME:love~2\\s+don''t~2'\\s*,\\s*0\\s*,\\s*0\\s*\\)\\s+FT" +
			"\\s+JOIN\\s+AUDIO_METADATA\\s+A\\s+ON\\s+A.FILEID\\s*=\\s*FT.KEYS\\[1\\]" +
			"\\s+JOIN\\s+FILES\\s+F\\s+ON\\s+F.ID\\s*=\\s*A.FILEID" +
			"\\s+WHERE\\s+FT\\.\"TABLE\"\\s*=\\s*'AUDIO_METADATA'" +
			"\\s+AND\\s+F.FORMAT_TYPE\\s*=\\s*1" +
			".*"
		));
	}

	/**
	 * Convinience method to test various SearchCriteria and the resulting SQL statements
	 */
	@Test
	public void testSQL() {
//		String searchCriteria = "upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"minimal\"";
//		String searchCriteria = "upnp:class derivedfrom \"object.container.playlistcontainer\" and dc:title contains \"pop\"";
//		String searchCriteria = "upnp:class derivedfrom \"object.item.videoitem\" and dc:title contains \"\"";
//		String searchCriteria = "upnp:class derivedfrom \"object.item.imageitem\" and dc:title contains \"\"";

		String searchCriteria = "upnp:class derivedfrom \"object.container\" and dc:title contains \"music\"";

		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(searchCriteria);
		sr.setContainerId("134");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql(sr);
		LOG.info(sql);
	}

	@Test
	public void testVideoFileUpnpSearch() {
		SearchRequest sr = new SearchRequest();
		Renderer renderer = RendererConfigurations.getDefaultRenderer();
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		sr.setContainerId("0");
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.videoItem\"");
		sr.setFilter(
				"dc:title,av:mediaClass,dc:date,@childCount,av:chapterInfo,res,upnp:rating,upnp:rating@type,upnp:class,av:soundPhoto,res@resolution,res@av:mpfEntries,upnp:album,upnp:genre,upnp:albumArtURI,upnp:albumArtURI@dlna:profileID,dc:creator,res@size,res@duration,res@bitrate,res@protocolInfo");
		sr.setSortCriteria("");
		SearchRequestHandler searchRequestHandler = new SearchRequestHandler(sr);
		StringBuilder response = searchRequestHandler.createSearchResponse(renderer);
		LOG.info("");
		LOG.info("testVideoFileUpnpSearch");
		LOG.info("===================================================================");
		LOG.info("\r\n" + response.toString());
	}

}