package net.pms.network.mediaserver.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableStoreIds;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.renderers.Renderer;
import net.pms.store.MediaScanner;
import net.pms.store.MediaStoreId;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;

/**
 * How Lucene searched work:
 *
 * A Lucene Proximity Search is a search method that allows you to find terms located within a specific distance of each other in
 * the text. It is more flexible than an exact phrase search, as it allows for additional words between the search terms or even a
 * different word order.
 *
 * A proximity search is initiated by putting " around the words you want to search for.
 * Those " have to be escaped as "" in the UPnP search criteria. So instead of searching for dc:title contains "Dark moon" the search
 * criteria for a proximity search would be dc:title contains """Dark Moon""".
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchRequestDatabaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestDatabaseTest.class.getName());

	private static Renderer renderer = null;
	private Path testMusicFolder;

	private StoreContainer root = null;
	private StoreContainer musicDir = null;
	private StoreContainer dir1 = null;
	private StoreContainer dir2 = null;
	private StoreContainer dir3 = null;
	private StoreContainer searchDir = null;

	// Order:
	// 0=album, 1=artist, 2=albumArtist, 3=songname, 4=genre, 5=year, 6=track, 7=disc, 8=composer, 9=conductor, 10=folder
	private static final List<String[]> AUDIO_METADATA_PRESETS = List.of(
		// Bucket 1
		new String[] {"The Wall", "Pink Floyd", "Pink Floyd", "In the Flesh", "Rock", "1979", "1", "1", "Roger Waters", null, "/music/1970/In the Flesh"},
		new String[] {"The Dark Side of the Moon", "Pink Floyd", "Pink Floyd", "Time", "Rock", "1973", "4", "1", "Roger Waters", null, "/music/1970/Time"},
		new String[] {"Rumours", "Fleetwood Mac", "Fleetwood Mac", "Dreams", "Rock", "1977", "2", "1", "Stevie Nicks", null, "/music/1970/Dreams"},
		new String[] {"Led Zeppelin IV", "Led Zeppelin", "Led Zeppelin", "Black Dog", "Rock", "1971", "1", "1", "Jimmy Page", null, "/music/1970/Black Dog"},
		new String[] {"A Night at the Opera", "Queen", "Queen", "Bohemian Rhapsody", "Rock", "1975", "11", "1", "Freddie Mercury", null, "/music/1970/Bohemian Rhapsody"},

		// Bucket 2
		new String[] {"Back in Black", "AC/DC", "AC/DC", "Hells Bells", "Hard Rock", "1980", "1", "1", "Angus Young", null, "/music/1980/Hells Bells"},
		new String[] {"Thriller", "Michael Jackson", "Michael Jackson", "Billie Jean", "Pop", "1982", "6", "1", "Michael Jackson", null, "/music/1980/Billie Jean"},
		new String[] {"Kind of Blue", "Miles Davis", "Miles Davis", "So What", "Jazz", "1959", "1", "1", "Miles Davis", null, "/music/1950/So What"},
		new String[] {"Blue Train", "John Coltrane", "John Coltrane", "Blue Train", "Jazz", "1957", "1", "1", "John Coltrane", null, "/music/1950/Blue Train"},
		new String[] {"The Köln Concert", "Keith Jarrett", "Keith Jarrett", "Part I", "Jazz", "1975", "1", "1", "Keith Jarrett", null, "/music/1970/Part I"},

		// Bucket 3
		new String[] {"Random Access Memories", "Daft Punk", "Daft Punk", "Get Lucky", "Electronic", "2013", "8", "1", "Thomas Bangalter", null, "/music/2010/Get Lucky"},
		new String[] {"Discovery", "Daft Punk", "Daft Punk", "Harder Better Faster Stronger", "Electronic", "2001", "4", "1", "Thomas Bangalter", null, "/music/2000/Harder Better Faster Stronger"},
		new String[] {"Nevermind", "Nirvana", "Nirvana", "Smells Like Teen Spirit", "Grunge", "1991", "1", "1", "Kurt Cobain", null, "/music/1990/Smells Like Teen Spirit"},
		new String[] {"OK Computer", "Radiohead", "Radiohead", "Paranoid don't Android", "Alternative", "1997", "2", "1", "Thom Yorke", null, "/music/1990/Paranoid don't Android"},
		new String[] {"In Rainbows", "Radiohead", "Radiohead", "Nude", "Alternative", "2007", "3", "1", "Thom Yorke", null, "/music/2000/Nude"},
		new String[] {"Requiem", "W. A. Mozart", "Various Artists", "Lacrimosa", "Classical", "1791", "8", "1", "W. A. Mozart", "Herbert von Karajan", "/music/1790/Lacrimosa"},
		new String[] {"Symphony No. 5", "Ludwig van Beethoven", "Various Artists", "I. Allegro con brio", "Classical", "1808", "1", "1", "Ludwig van Beethoven", "Carlos Kleiber", "/music/1800/I. Allegro con brio"},
		new String[] {"The Four Seasons", "Antonio Vivaldi", "Various Artists", "Spring Allegro", "Classical", "1725", "1", "1", "Antonio Vivaldi", "Claudio Abbado", "/music/1720/Spring Allegro"},
		new String[] {"Appetite for Destruction", "Guns N' Roses", "Guns N' Roses", "Welcome to the Jungle", "Hard Rock", "1987", "1", "1", "Slash", null, "/music/1980/Welcome to the Jungle"},
		new String[] {"Master of Puppets", "Metallica", "Metallica", "Battery", "Metal", "1986", "1", "1", "James Hetfield", null, "/music/1980/Battery"}
	);

	@AfterAll
	public void tearDown() {
		if (testMusicFolder != null) {
			try {
				deleteDirectoryRecursively(testMusicFolder);
				LOG.info("Deleted test music folder at path {}", testMusicFolder.toAbsolutePath());
			} catch (IOException e) {
				LOG.error("Failed to delete test music folder at path {}", testMusicFolder.toAbsolutePath(), e);
			}
		}
	}

	@BeforeAll
	public void setupTestDatabase() throws Exception {
		System.setProperty("surefire.real.class.path", "/tmp");

		testMusicFolder = Files.createTempDirectory("music");
		testMusicFolder.toFile().deleteOnExit();

		LOG.info("Setting up test music folder at path {}", testMusicFolder.toAbsolutePath());
		Path subDir1 = testMusicFolder.resolve("1");
		Path subDir2 = testMusicFolder.resolve("2");
		Path subDir3 = testMusicFolder.resolve("3");
		Path subSearchDir = subDir1.resolve("search_for_me");

		Files.createDirectories(subDir1);
		Files.createDirectories(subDir2);
		Files.createDirectories(subDir3);
		Files.createDirectories(subSearchDir);

		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		MediaDatabase.dropAllTables(database.getConnection());
		database.checkTables(true);
		try (Connection connection = database.getConnection()) {
			for (int i = 0; i < AUDIO_METADATA_PRESETS.size(); i++) {
				String[] selected = pickPreset(i);
				MediaInfo media = createMediaInfoFromPreset(i);
				Path filePath = null;
				if (i < 5) {
					filePath = subDir1.resolve(valueAt(selected, 3) + ".flac");
				} else if (i < 10) {
					filePath = subDir2.resolve(valueAt(selected, 3) + ".flac");
				} else {
					filePath = subDir3.resolve(valueAt(selected, 3) + ".flac");
				}
				LOG.info("Creating test file for '{}' at path {}", valueAt(selected, 3), filePath.toAbsolutePath());
				Files.createFile(filePath);
				Files.writeString(filePath, "TEST_CONENTEXT.");
				MediaTableFiles.insertOrUpdateData(connection, filePath.toAbsolutePath().toString(), System.currentTimeMillis(), Format.AUDIO, media);
			}
		}
		RendererConfigurations.loadRendererConfigurations();
		renderer = RendererConfigurations.getDefaultRenderer();

		Path playlist = subDir1.resolve("Jazz.m3u8");
		Files.createFile(playlist);
		Files.writeString(playlist, "#EXTM3U\n\nTime.flac");
		MediaTableFiles.insertOrUpdateData(database.getConnection(), playlist.toAbsolutePath().toString(), System.currentTimeMillis(), Format.PLAYLIST, null);

		MediaScanner.init();
		SharedContentConfiguration.updateSharedContent(new SharedContentArray(), true);
		SharedContentConfiguration.addFolderShared(testMusicFolder.toFile());
		MediaScanner.backgroundScanFileOrFolder(testMusicFolder.toAbsolutePath().toString());

		root = (StoreContainer) renderer.getMediaStore().getResource("0");
		musicDir = (StoreContainer) root.getChildren().stream()
			.filter(child -> testMusicFolder.getFileName().toString().equals(child.getName()))
			.findFirst()
			.orElse(null);
		musicDir.discoverChildren();
		dir1 = (StoreContainer) musicDir.getChildren().stream()
			.filter(child -> "1".equals(child.getName()))
			.findFirst()
			.orElse(null);
		dir2 = (StoreContainer) musicDir.getChildren().stream()
			.filter(child -> "2".equals(child.getName()))
			.findFirst()
			.orElse(null);
		dir3 = (StoreContainer) musicDir.getChildren().stream()
			.filter(child -> "3".equals(child.getName()))
			.findFirst()
			.orElse(null);

		Path video = subDir1.resolve("Spider-Man.mkv");
		Files.createFile(video);
		Files.writeString(video, "SPIDERMAN_MOVIE");
		MediaTableFiles.insertOrUpdateData(database.getConnection(), video.toAbsolutePath().toString(), System.currentTimeMillis(), Format.VIDEO, null);
		StoreResource sr = renderer.getMediaStore().createResourceFromFile(video.toFile());
		dir1.addChild(sr);
		MediaStoreId id = MediaTableStoreIds.getResourceMediaStoreId(database.getConnection(), sr);

		dir1.discoverChildren();
		dir2.discoverChildren();
		dir3.discoverChildren();

		searchDir = (StoreContainer) dir1.getChildren().stream()
			.filter(child -> "search_for_me".equals(child.getName()))
			.findFirst()
			.orElse(null);
		searchDir.discoverChildren();
	}

	@Test
	public void testGlobalLinnTitleSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and ( dc:title contains \"blue\" or upnp:album contains \"blue\" or upnp:artist contains \"blue\" or upnp:genre contains \"blue\" )");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(results, 2);
		// We can not create the Store Resources, since the files do not exist.
	}

	/**
	 * Eleemnts with blue in title are both located in dir "2"
	 */
	@Test
	public void testTreeLinnTitleSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and ( dc:title contains \"blue\" or upnp:album contains \"blue\" or upnp:artist contains \"blue\" or upnp:genre contains \"blue\" )");
		sr.setContainerId(dir2.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(results, 2);

		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and ( dc:title contains \"blue\" or upnp:album contains \"blue\" or upnp:artist contains \"blue\" or upnp:genre contains \"blue\" )");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(results, 0);
	}

	/**
	 * This test checks a Lucene proximity search. Default distance is 2, so we should find "The Dark Side of the Moon" when searching for
	 * "Dark the" but not when searching for "Dark Moon".
	 */
	@Test
	public void testGlobalProximitySearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("( upnp:class derivedfrom \"object.container.album\" and dc:title contains \"\"\"Dark the\"\"\")");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("The Dark Side of the Moon", foundResource.getName());


		sr.setSearchCriteria("( upnp:class derivedfrom \"object.container.album\" and dc:title contains \"\"\"Dark moon\"\"\")");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());
	}

	@Test
	public void testGlobalAlbumArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"Various\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Various Artists", foundResource.getName());
	}

	/**
	 * Various Artists are in dir "3"
	 */
	@Test
	public void testTreeAlbumArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"Various\"");
		sr.setContainerId(dir3.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Various Artists", foundResource.getName());

		// Fault
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"Various\"");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());

		// musicDir contains dir1, dir2 and dir3, so we should find the result when searching in musicDir
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"Various\"");
		sr.setContainerId(musicDir.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
	}

	@Test
	public void testGlobalLinnAppSpecialCharSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"don't\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		// We can not create the Store Resources, since the files do not exist.
	}

	@Test
	public void testGlobalAlbumSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"discovery\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Discovery", foundResource.getName());
	}

	/**
	 * Album Discovery is in folder "3".
	 */
	@Test
	public void testTreeAlbumSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"discovery\"");
		sr.setContainerId(dir3.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Discovery", foundResource.getName());

		// now we search in dir "2". This should not return any result, since the album is in dir "3".
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"discovery\"");
		sr.setContainerId(dir2.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());
	}

	@Test
	public void testGlobalArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"punk\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(results, resources.size());

		List<String> names = resources.stream().map(StoreResource::getName).collect(Collectors.toList());
		assertTrue(names.contains("Pink Floyd"), "Expected 'Pink Floyd' in results.");
		assertTrue(names.contains("Daft Punk"), "Expected 'Daft Punk' in results.");
		assertTrue(names.contains("Guns N' Roses"), "Expected 'Guns N' Roses' in results.");
	}

	/**
	 * Same parameter as global search but in directory "1". Only Pink Floyd is in the first bucket with 5 entries.
	 */
	@Test
	public void testTreeArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"punk\"");
		LOG.info("Container ID for search: {}", dir1.getId());
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(results, resources.size());

		List<String> names = resources.stream().map(StoreResource::getName).collect(Collectors.toList());
		assertTrue(names.contains("Pink Floyd"), "Expected 'Pink Floyd' in results.");
	}

	@Test
	public void testGlobalComposerSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"Beethoven\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Ludwig van Beethoven", foundResource.getName());
	}

	/**
	 * Beethoven is located in dir "3"
	 */
	@Test
	public void testTreeComposerSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"Beethoven\"");
		sr.setContainerId(dir3.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Ludwig van Beethoven", foundResource.getName());

		// dir 2 has no composer with name Beethoven, so this should return no result.
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"Beethoven\"");
		sr.setContainerId(dir2.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());
	}

	@Test
	public void testGlobalConductorSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"Karajan\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Herbert von Karajan", foundResource.getName());
	}

	/**
	 * Conductor Karajan is located in dir "3"
	 */
	@Test
	public void testTreeConductorSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"Karajan\"");
		sr.setContainerId(dir3.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Herbert von Karajan", foundResource.getName());

		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"Karajan\"");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());
	}

	@Test
	public void testGlobalExactAlbumSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"rumors\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Rumours", foundResource.getName());
	}

	@Test
	public void testGlobalFuzzyAlbumSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"pupets\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Master of Puppets", foundResource.getName());
	}

	@Test
	public void testGlobalPlaylistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"Jazz\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Jazz.m3u8", foundResource.getName());
	}

	/**
	 * Playlist is located in dir "1"
	 */
	@Test
	public void testTreePlaylistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.playlistContainer\" and dc:title contains \"Jazz\"");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Jazz.m3u8", foundResource.getName());
	}


	@Test
	public void testGlobalVideoSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.videoItem\" and dc:title contains \"Spider\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
	}

	@Test
	public void testTreeVideoSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.item.videoItem\" and dc:title contains \"Spider\"");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);

		sr.setSearchCriteria("upnp:class = \"object.item.videoItem\" and dc:title contains \"Spider\"");
		sr.setContainerId(dir2.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);
	}

	@Test
	public void testGlobalFolderSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.storageFolder\" and dc:title contains \"search_for_me\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);

		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("search_for_me", foundResource.getName());
	}

	@Test
	public void testTreeFolderSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.storageFolder\" and dc:title contains \"search_for_me\"");
		sr.setContainerId(dir1.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LuceneSearchRequestHandler searchRequestHandler = new LuceneSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);

		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("search_for_me", foundResource.getName());

		sr.setSearchCriteria("upnp:class = \"object.container.storageFolder\" and dc:title contains \"search_for_me\"");
		sr.setContainerId(dir2.getId());
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		searchRequestHandler = new LuceneSearchRequestHandler(sr);
		results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(0, results);

		resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(0, resources.size());
	}

	public MediaInfo createMediaInfo() {
		return createMediaInfoFromPreset(0);
	}

	public MediaInfo createMediaInfoFromPreset(int presetIndex) {
		String[] selected = pickPreset(presetIndex);
		MediaInfo media = new MediaInfo();
		media.setAudioMetadata(createMediaAudioMetadataFromArray(selected));
		return media;
	}

	public MediaInfo createMediaInfoFromRandomPreset() {
		int index = ThreadLocalRandom.current().nextInt(AUDIO_METADATA_PRESETS.size());
		return createMediaInfoFromPreset(index);
	}

	public int getPresetCount() {
		return AUDIO_METADATA_PRESETS.size();
	}

	private String[] pickPreset(int presetIndex) {
		if (presetIndex < 0 || presetIndex >= AUDIO_METADATA_PRESETS.size()) {
			throw new IllegalArgumentException(
				"Invalid preset index " + presetIndex + ", valid range is 0.." + (AUDIO_METADATA_PRESETS.size() - 1)
			);
		}
		return AUDIO_METADATA_PRESETS.get(presetIndex);
	}

	private MediaAudioMetadata createMediaAudioMetadataFromArray(String[] values) {
		MediaAudioMetadata metadata = new MediaAudioMetadata();
		if (values == null) {
			return metadata;
		}

		metadata.setAlbum(valueAt(values, 0));
		metadata.setArtist(valueAt(values, 1));
		metadata.setAlbumArtist(valueAt(values, 2));
		metadata.setSongname(valueAt(values, 3));
		metadata.setGenre(valueAt(values, 4));
		metadata.setYear(parseIntOrZero(valueAt(values, 5)));
		metadata.setTrack(parseIntOrZero(valueAt(values, 6)));
		metadata.setDisc(parseIntOrZero(valueAt(values, 7)));
		metadata.setComposer(valueAt(values, 8));
		metadata.setConductor(valueAt(values, 9));

		return metadata;
	}

	private static String valueAt(String[] values, int index) {
		return index >= 0 && index < values.length ? values[index] : null;
	}

	private int parseIntOrZero(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			LOG.debug("Invalid integer value '{}', defaulting to 0", value);
			return 0;
		}
	}

	public void deleteDirectoryRecursively(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
