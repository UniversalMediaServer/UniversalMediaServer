package net.pms.store.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
	static void initTest() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");

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
	public void testInternetPlaylist() throws Exception {
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
				assertTrue(false, "Failed to get thumbnail input stream for child: " + child.getName());
			}
		});
	}

	private static PlaylistFolder playlistOf(Path file) {
		return new PlaylistFolder(RendererConfigurations.getDefaultRenderer(), file.toFile());
	}

	private static Path tempPlaylist(String suffix, String content) throws IOException {
		Path file = Files.createTempFile("playlist_", suffix);
		file.toFile().deleteOnExit();
		Files.writeString(file, content);
		return file;
	}

	/**
	 * The url has to be the last line of the block : directives are collected until a line that is
	 * neither a comment nor blank, and that line is what closes the entry.
	 */
	@Test
	public void testAddWebEntryWritesTheDirectiveBlockInOrder() throws Exception {
		Path playlist = tempPlaylist(".m3u8", "#EXTM3U\n");
		assertTrue(playlistOf(playlist).addWebEntry("http://stream.example/live", "Radio Example",
			"http://img.example/logo.png", "aaaabbbb-cccc-dddd-eeee-ffff00001111"));

		List<String> lines = Files.readAllLines(playlist).stream().filter(l -> !l.isBlank()).toList();
		assertEquals("#EXTM3U", lines.get(0));
		assertEquals("#EXTINF:-1,Radio Example", lines.get(1));
		assertEquals("#RADIOBROWSERUUID:aaaabbbb-cccc-dddd-eeee-ffff00001111", lines.get(2));
		assertEquals("#EXTIMG:http://img.example/logo.png", lines.get(3));
		assertEquals("http://stream.example/live", lines.get(4));
		assertEquals(5, lines.size());
	}

	@Test
	public void testAddWebEntryRejectsAnUrlThatIsAlreadyThere() throws Exception {
		Path playlist = tempPlaylist(".m3u8", "#EXTM3U\n\n#EXTINF:-1,Already here\nhttp://stream.example/live\n");
		assertFalse(playlistOf(playlist).addWebEntry("http://stream.example/live", "Second try", null, null));
		assertEquals(1, Files.readAllLines(playlist).stream().filter(l -> l.startsWith("http")).count());
	}

	/**
	 * Adding must not disturb what is already in the file, directives of other entries included.
	 */
	@Test
	public void testAddWebEntryKeepsTheExistingEntries() throws Exception {
		Path playlist = tempPlaylist(".m3u8",
			"#EXTM3U\n\n#EXTINF:-1,First\n#EXTIMG:http://img.example/first.png\n#EXTRATING:4\nhttp://stream.example/first\n");
		assertTrue(playlistOf(playlist).addWebEntry("http://stream.example/second", "Second", null, null));

		String written = Files.readString(playlist);
		assertTrue(written.contains("#EXTIMG:http://img.example/first.png"), written);
		assertTrue(written.contains("#EXTRATING:4"), written);
		assertTrue(written.contains("http://stream.example/first"), written);
		assertTrue(written.indexOf("http://stream.example/first") < written.indexOf("http://stream.example/second"), written);
	}

	/**
	 * The ICY order of a station lives next to its url, and going back to the automatic detection
	 * takes the line out again instead of leaving "auto" behind.
	 */
	@Test
	public void testIcyOrderDirectiveIsWrittenAndRemoved() throws Exception {
		Path playlist = tempPlaylist(".m3u8",
			"#EXTM3U\n\n#EXTINF:-1,First\nhttp://stream.example/first\n#EXTINF:-1,Second\nhttp://stream.example/second\n");

		playlistOf(playlist).updateIcyOrderDirective("http://stream.example/second", "title-first");
		List<String> lines = Files.readAllLines(playlist).stream().filter(l -> !l.isBlank()).toList();
		assertEquals("#EXTINF:-1,Second", lines.get(3));
		assertEquals("#EXTICYORDER:title-first", lines.get(4));
		assertEquals("http://stream.example/second", lines.get(5));
		assertEquals(6, lines.size());

		playlistOf(playlist).updateIcyOrderDirective("http://stream.example/second", null);
		assertFalse(Files.readString(playlist).contains("#EXTICYORDER"), Files.readString(playlist));
		assertEquals(2, Files.readAllLines(playlist).stream().filter(l -> l.startsWith("http")).count());
	}

	/**
	 * A plain m3u is read as ISO-8859-1. Writing it back with the platform default used to turn every
	 * accented character into mojibake.
	 */
	@Test
	public void testRewritingALatin1PlaylistKeepsItsAccents() throws Exception {
		Path playlist = Files.createTempFile("playlist_", ".m3u");
		playlist.toFile().deleteOnExit();
		Files.write(playlist, "#EXTM3U\n\n#EXTINF:-1,Radio Köln Grüße\nhttp://stream.example/koeln\n"
			.getBytes(StandardCharsets.ISO_8859_1));

		assertTrue(playlistOf(playlist).addWebEntry("http://stream.example/second", "Second", null, null));

		String reread = new String(Files.readAllBytes(playlist), StandardCharsets.ISO_8859_1);
		assertTrue(reread.contains("Radio Köln Grüße"), reread);
	}

}
