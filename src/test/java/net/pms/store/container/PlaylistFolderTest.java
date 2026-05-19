package net.pms.store.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.dlna.DLNAThumbnailInputStream;

public class PlaylistFolderTest {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistFolderTest.class.getName());

	@BeforeAll
	static void initTest() throws SQLException, ConfigurationException, InterruptedException {
		System.setProperty("surefire.real.class.path", "/tmp");

		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		MediaDatabase.dropAllTables(database.getConnection());
		database.checkTables(true);

		RendererConfigurations.loadRendererConfigurations();
	}

	/**
	 * Test that a playlist file with an internet URL can be read and a thumbnail input stream can be obtained.
	 * @throws IOException
	 */
	@Test
	public void testInternetPlaylist() throws IOException {
		Path testPlaylist;
		testPlaylist = Files.createTempFile("playlist_", ".m3u8");
		testPlaylist.toFile().deleteOnExit();
		Files.writeString(testPlaylist, "#EXTM3U\n\nhttps://somafm.com/dronezone256.pls");

		LOG.info("Testing playlist file: {}", testPlaylist.toString());
		PlaylistFolder pf = new PlaylistFolder(RendererConfigurations.getDefaultRenderer(), testPlaylist.toFile());
		pf.resolve();
		DLNAThumbnailInputStream is = pf.getThumbnailInputStream();
		assertNotNull(is);
		assertEquals(1, pf.getChildren().size());
		pf.getChildren().forEach(child -> {
			try {
				assertNotNull(child.getThumbnailInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}
