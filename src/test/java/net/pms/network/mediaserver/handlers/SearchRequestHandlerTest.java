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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;

public class SearchRequestHandlerTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestHandlerTest.class.getName());


	@Test
	public void testGlobalPlaylistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
	}

	@Test
	public void testGlobalPlaylistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);
	}

	@Test
	public void testTreePlaylistSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
	}

	@Test
	public void testTreePlaylistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"jazz\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);

	}

	@Test
	public void testTreeAlbumSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
	}

	@Test
	public void testTreeAlbumSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"spirit\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);

	}

	@Test
	public void testTreeArtistSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.person\" and dc:title contains \"Rhye\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);

	}


	@Test
	public void testTreeMusicItemSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
	}

	@Test
	public void testTreeMusicItemSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("140");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);

	}

	@Test
	public void testGlobalMusicItemSearchCount() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);
	}

	@Test
	public void testGlobalMusicItemSearchSql() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.audioItem.musicTrack\" and dc:title contains \"Darc\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToFilesSql();
		LOG.info(sql);
	}


	@Test
	public void testVideoFileSqlStatement() {
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(s);
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String result = searchRequestHandler.convertToFilesSql();
		LOG.info(result);  // \\s+
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
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		String sql = searchRequestHandler.convertToCountSql();
		LOG.info(sql);
	}

	/**
	 * A search criteria filtering only by upnp:class (no fulltext term) produces an
	 * empty Lucene query. The LuceneSearchRequestHandler cannot handle it and has to
	 * report this, so that UmsContentDirectoryService falls back to the
	 * DbSearchRequestHandler (see BubbleUPnP audio search regression).
	 */
	@Test
	public void testClassOnlySearchFallsBackToDbHandler() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\"");
		sr.setContainerId("0");
		sr.setRequestedCount(1);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler luceneHandler = new LuceneSearchRequestHandler(sr);
		assertFalse(luceneHandler.canHandle(), "empty lucene query cannot be handled by LuceneSearchRequestHandler");

		DbSearchRequestHandler dbHandler = new DbSearchRequestHandler(sr);
		assertTrue(dbHandler.canHandle());
		String sql = dbHandler.convertToFilesSql();
		LOG.info(sql);
		assertTrue(sql.contains("FORMAT_TYPE = 1"), "db handler should filter audio by FORMAT_TYPE");
		assertTrue(sql.contains("LIMIT 1"), "requested count must be honoured");
	}

	/**
	 * A criteria containing a fulltext term must still be handled by the Lucene handler.
	 */
	@Test
	public void testClassWithTitleSearchIsHandledByLucene() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"jazz\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler luceneHandler = new LuceneSearchRequestHandler(sr);
		assertTrue(luceneHandler.canHandle());
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
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		StringBuilder response = searchRequestHandler.createSearchResponse(renderer);
		LOG.info("");
		LOG.info("testVideoFileUpnpSearch");
		LOG.info("===================================================================");
		LOG.info("\r\n" + response.toString());
	}
}
