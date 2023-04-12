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

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;

public class SearchRequestHandlerTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestHandlerTest.class.getName());

	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
		// No need to setup anything
	}

	@Test
	public void testVideoFileSqlStatement() {
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(s);
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		SearchRequestHandler h = new SearchRequestHandler();
		String result = h.convertToFilesSql(sr, h.getRequestType(s));
		LOG.info(result);  // \\s+
		assertTrue(result.matches(
			"select\\s+FILENAME\\s*,\\s*MODIFIED\\s*,\\s*F\\.ID\\s+as\\s+FID\\s*,\\s*F\\.ID\\s+as\\s+oid\\s+from\\s+FILES\\s+as\\s+F\\s+where\\s*\\(\\s*F\\.FORMAT_TYPE\\s*=\\s*4\\s*\\)\\s*ORDER\\s+BY\\s+oid\\s+LIMIT\\s+999\\s+OFFSET\\s+0\\s*"));
	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS) for Composer
	 */
	@Test
	public void testLinnAppComposerSearch() {
		String searchCriteria = "upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"tchaikovsky\"";
		SearchRequestHandler h = new SearchRequestHandler();
		String countSQL = h.convertToCountSql(searchCriteria, h.getRequestType(searchCriteria));
		LOG.info(countSQL);
		assertTrue(countSQL.matches(
			"select\\s+count\\s+\\(\\s*DISTINCT\\s+A.COMPOSER\\s*\\)\\s+from\\s+AUDIOTRACKS\\s+as\\s+A\\s+where\\s+1\\s*=\\s*1\\s+and\\s+LOWER\\s*\\(\\s*A.COMPOSER\\s*\\)\\s+LIKE\\s+'%tchaikovsky%'"));
	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS) for Composer
	 */
	@Test
	public void testLinnAppConductorSearch() {
		String searchCriteria = "upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"bernstein\"";
		SearchRequestHandler h = new SearchRequestHandler();
		String countSQL = h.convertToCountSql(searchCriteria, h.getRequestType(searchCriteria));
		LOG.info(countSQL);
		assertTrue(countSQL.matches(
			"select\\s+count\\s+\\(\\s*DISTINCT\\s+A.CONDUCTOR\\s*\\)\\s+from\\s+AUDIOTRACKS\\s+as\\s+A\\s+where\\s+1\\s*=\\s*1\\s+and\\s+LOWER\\s*\\(\\s*A.CONDUCTOR\\s*\\)\\s+LIKE\\s+'%bernstein%'"));
	}

	@Test
	public void testAlbumArtistSearch() {
		String searchCriteria = "upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"tchaikovsky\"";
		SearchRequestHandler h = new SearchRequestHandler();
		String countSQL = h.convertToCountSql(searchCriteria, h.getRequestType(searchCriteria));
		LOG.info(countSQL);
		assertTrue(countSQL.matches(
			"select\\s+count\\s+\\(\\s*DISTINCT\\s+A.ALBUMARTIST\\s*\\)\\s+from\\s+AUDIOTRACKS\\s+as\\s+A\\s+where\\s+1\\s*=\\s*1\\s+and\\s+LOWER\\s*\\(\\s*A.ALBUMARTIST\\s*\\)\\s+LIKE\\s+'%tchaikovsky%'"));
	}

	@Test
	public void testArtistSearch() {
		String searchCriteria = "upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist contains \"tchaikovsky\"";
		SearchRequestHandler h = new SearchRequestHandler();
		String countSQL = h.convertToCountSql(searchCriteria, h.getRequestType(searchCriteria));
		LOG.info(countSQL);
		assertTrue(countSQL.matches(
			"select\\s+count\\s+\\(\\s*DISTINCT\\s+A.ARTIST\\s*\\)\\s+from\\s+AUDIOTRACKS\\s+as\\s+A\\s+where\\s+1\\s*=\\s*1\\s+and\\s+LOWER\\s*\\(\\s*A.ARTIST\\s*\\)\\s+LIKE\\s+'%tchaikovsky%'"));
	}

	/**
	 * Tests SearchCriteria issued by LINN app (iOS) for Composer
	 */
	@Test
	public void testLinnAppSpecialCharSearch() {
		String searchCriteria = "upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"love don't\"";
		SearchRequestHandler h = new SearchRequestHandler();
		String countSQL = h.convertToCountSql(searchCriteria, h.getRequestType(searchCriteria));
		LOG.info(countSQL);
		assertTrue(countSQL.matches(
			"select\\s+count\\s*\\(\\s*DISTINCT\\s+F.id\\s*\\)\\s+from\\s+FILES\\s+as\\s+F\\s+left\\s+outer\\s+join\\s+AUDIOTRACKS\\s+as\\s+A\\s+on\\s+F.ID\\s*=\\s*A.FILEID\\s+where\\s+F.FORMAT_TYPE\\s*=\\s*1\\s+and\\s+LOWER\\s*\\(\\s*A.SONGNAME\\s*\\)\\s+LIKE\\s+'%love don''t%'"));
	}

	@Test
	public void testVideoFileUpnpSearch() {
		SearchRequestHandler srh = new SearchRequestHandler();
		SearchRequest sr = new SearchRequest();
		Renderer renderer = RendererConfigurations.getDefaultRenderer();
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		sr.setContainerId("0");
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.videoItem\"");
		sr.setFilter(
			"dc:title,av:mediaClass,dc:date,@childCount,av:chapterInfo,res,upnp:rating,upnp:rating@type,upnp:class,av:soundPhoto,res@resolution,res@av:mpfEntries,upnp:album,upnp:genre,upnp:albumArtURI,upnp:albumArtURI@dlna:profileID,dc:creator,res@size,res@duration,res@bitrate,res@protocolInfo");
		sr.setSortCriteria("");
		StringBuilder response = srh.createSearchResponse(sr, renderer);
		LOG.info("");
		LOG.info("testVideoFileUpnpSearch");
		LOG.info("===================================================================");
		LOG.info("\r\n" + response.toString());
	}
}
