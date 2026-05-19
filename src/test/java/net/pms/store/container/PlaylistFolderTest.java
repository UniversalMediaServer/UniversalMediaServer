package net.pms.store.container;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.RendererConfigurations;
import net.pms.dlna.DLNAThumbnailInputStream;

public class PlaylistFolderTest {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistFolderTest.class.getName());

	/**
	 * Test that a playlist file with an internet URL can be read and a thumbnail input stream can be obtained.
	 * @throws IOException
	 */
	@Test
	public void testInternetPlaylist() throws IOException {
		System.setProperty("surefire.real.class.path", "/tmp");
		Path testPlaylist;
		testPlaylist = Files.createTempFile("playlist_", ".m3u8");
		testPlaylist.toFile().deleteOnExit();
		Files.writeString(testPlaylist, "#EXTM3U\n\nhttps://somafm.com/dronezone256.pls");

		LOG.info("Testing playlist file: {}", testPlaylist.toString());
		PlaylistFolder pf = new PlaylistFolder(RendererConfigurations.getDefaultRenderer(), testPlaylist.toFile());
		DLNAThumbnailInputStream is = pf.getThumbnailInputStream();
		assertNotNull(is);
	}

}
