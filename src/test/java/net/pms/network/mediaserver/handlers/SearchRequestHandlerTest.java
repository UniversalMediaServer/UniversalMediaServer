package net.pms.network.mediaserver.handlers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.service.Services;

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
		PMS.setConfiguration(new PmsConfiguration(false));
		PMS.getConfiguration().setAutomaticMaximumBitrate(false); // do not test the network speed.

		if (PMS.getConfiguration().isRunSingleInstance()) {
			PMS.killOld();
		}
		Services.create();

		// Create a new instance
		PMS.getNewInstance();
	}

	@Test
	public void testVideoFileSqlStatement() {
		SearchRequestHandler srh = new SearchRequestHandler();
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria(s);
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		String result = srh.convertToFilesSql(sr, srh.getRequestType(s)).toString();
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
