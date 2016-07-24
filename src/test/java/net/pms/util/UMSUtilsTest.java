package net.pms.util;

import static org.assertj.core.api.Assertions.assertThat;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class UMSUtilsTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@Before
	public final void setUp() throws ConfigurationException {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

//	@Test
	public void testPlayedDurationStr() throws Exception {
		assertThat(UMSUtils.playedDurationStr("01:23:45:67.89", "01:23:45:67")).isEqualTo("23:45:67 / 23:45:67");
		assertThat(UMSUtils.playedDurationStr("01:23", "01:23:45")).isEqualTo("1:23 / 1:23:45");
		assertThat(UMSUtils.playedDurationStr(":12", "59")).isEqualTo("0:12 / 0:59");
	}
	
	@Test
	public void testFiledetection() throws Exception {
		String mimeType = "video/x-ms-wmv";
		String ext = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
		assertThat(ext).isEqualTo(".wmv");
		
		String mime = new Tika().detect("as" + ext);
		assertThat(mime).isEqualTo(mimeType);
		
		mimeType = "audio/vnd.dlna.adts";
		ext = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
		assertThat(ext).isEqualTo("");
		
		mime = new Tika().detect("as" + ext);
		assertThat(mime).isEqualTo("application/octet-stream");
	}
	
	public void testCriteria() throws Exception {
		String str;
		String sql;

		// Web search
		str = "dc:title contains \"cap\"";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("LOWER(FILENAME) like '%cap%'");

		// Xbox
		str = "(upnp:class derivedfrom &quot;object.item.audioItem&quot;)";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1)");
		
		// WMP tests
		str = "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("f.type = 1 and 1=1");
		
		str = "upnp:class derivedfrom \"object.container.playlistContainer\" and @refID exists false";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("f.type = 4 and 1=1");
		
		// BubbleUPnP tests
		str = "(upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"cap\")";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1 and LOWER(FILENAME) like '%cap%')");
		
		str = "(upnp:class derivedfrom \"object.item.videoItem\" and dc:title contains \"cap\")";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 4 and LOWER(FILENAME) like '%cap%')");
		
		str = "(upnp:class = \"object.container.album.musicAlbum\" and dc:title contains \"cap\")";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1 and LOWER(FILENAME) like '%cap%')");
		
		str = "(upnp:class = \"object.container.album.musicAlbum\" and upnp:artist contains \"cap\")";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1 and LOWER(ARTIST) like '%cap%')");
		
		str = "(upnp:class = \"object.container.person.musicArtist\" and dc:title contains \"cap\")";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1 and LOWER(FILENAME) like '%cap%')");
		
		str = "(upnp:class derivedfrom \"object.item.audioItem\" and (dc:creator contains \"cap\" or upnp:artist contains \"cap\"))";
		sql = UMSUtils.getSqlFromCriteria(str);
		assertThat(sql).isEqualTo("(f.type = 1 and (1=1 or LOWER(ARTIST) like '%cap%'))");
	
	}
}
