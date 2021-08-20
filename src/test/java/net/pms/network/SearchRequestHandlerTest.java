package net.pms.network;

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
import net.pms.network.message.SearchRequest;

/**
 *
 */
public class SearchRequestHandlerTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestHandlerTest.class.getName());

	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);
		PMS.forceHeadless();
		PMS.setConfiguration(new PmsConfiguration(false));
		PMS.get();
	}

	@Test
	public void testVideoFileSqlStatement() {
		SearchRequestHandler sr = new SearchRequestHandler();
		String s = "( upnp:class derivedfrom \"object.item.videoItem\" )";
		String result = sr.convertToFilesSql(s, sr.getRequestType(s)).toString();
		LOG.info(result);  //  \\s+
		assertTrue(result.matches("select\\s+FILENAME,\\s+MODIFIED,\\s+F\\.ID\\s+as\\s+FID\\s+from\\s+FILES\\s+as\\s+F\\s+where\\s+\\(\\s+F\\.TYPE\\s+=\\s+4\\s+\\)"));
	}

	@Test
	public void testVideoFileUpnpSearch() {
		SearchRequestHandler srh = new SearchRequestHandler();
		SearchRequest sr = new SearchRequest();
		RendererConfiguration rc = RendererConfiguration.getDefaultConf();
		sr.setContainerId("0");
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.videoItem\"");
		sr.setFilter("dc:title,av:mediaClass,dc:date,@childCount,av:chapterInfo,res,upnp:rating,upnp:rating@type,upnp:class,av:soundPhoto,res@resolution,res@av:mpfEntries,upnp:album,upnp:genre,upnp:albumArtURI,upnp:albumArtURI@dlna:profileID,dc:creator,res@size,res@duration,res@bitrate,res@protocolInfo");
		sr.setStartingIndex(0);
		sr.setRequestedCount(14);
		sr.setSortCriteria("");
		StringBuilder response = srh.createSearchResponse(sr, rc);
		LOG.info("");
		LOG.info("testVideoFileUpnpSearch");
		LOG.info("===================================================================");
		LOG.info("\r\n" + response.toString());
	}
}
