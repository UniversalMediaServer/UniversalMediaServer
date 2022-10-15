/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.mediaserver.handlers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.service.Services;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchRequestHandlerTest {
	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestHandlerTest.class.getName());

	/**
	 * Set up testing conditions before running the tests.
	 *
	 * @throws ConfigurationException
	 */
	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
		PMS.forceHeadless();
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
		assert PMS.getConfiguration() != null;
		PMS.getConfiguration().setAutomaticMaximumBitrate(false); // do not test the network speed.
		PMS.getConfiguration().setSharedFolders(null);
		PMS.getConfiguration().setScanSharedFoldersOnStartup(false);
		PMS.getConfiguration().setUseCache(false);

		Services.destroy();

		try {
			PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			LOG.warn("Failed to write credentials configuration", ex);
		}

		// Create a new PMS instance
		PMS.getNewInstance();
	}

	@Test
	public void testVideoFileSqlStatement() {
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(s);
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		String result = SearchRequestHandler.convertToFilesSql(sr, SearchRequestHandler.getRequestType(s));
		LOG.info(result);  // \\s+
		assertTrue(result.matches(
			"select\\s+FILENAME\\s*,\\s*MODIFIED\\s*,\\s*F\\.ID\\s+as\\s+FID\\s*,\\s*F\\.ID\\s+as\\s+oid\\s+from\\s+FILES\\s+as\\s+F\\s+where\\s*\\(\\s*F\\.FORMAT_TYPE\\s*=\\s*4\\s*\\)\\s*ORDER\\s+BY\\s+oid\\s+LIMIT\\s+999\\s+OFFSET\\s+0\\s*"));
	}

	@Test
	public void testVideoFileUpnpSearch() {
		SearchRequestHandler srh = new SearchRequestHandler();
		SearchRequest sr = new SearchRequest();
		RendererConfiguration rc = RendererConfiguration.getDefaultConf();
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		sr.setContainerId("0");
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.videoItem\"");
		sr.setFilter(
			"dc:title,av:mediaClass,dc:date,@childCount,av:chapterInfo,res,upnp:rating,upnp:rating@type,upnp:class,av:soundPhoto,res@resolution,res@av:mpfEntries,upnp:album,upnp:genre,upnp:albumArtURI,upnp:albumArtURI@dlna:profileID,dc:creator,res@size,res@duration,res@bitrate,res@protocolInfo");
		sr.setSortCriteria("");
		StringBuilder response = srh.createSearchResponse(sr, rc);
		LOG.info("");
		LOG.info("testVideoFileUpnpSearch");
		LOG.info("===================================================================");
		LOG.info("\r\n" + response.toString());
	}

}
