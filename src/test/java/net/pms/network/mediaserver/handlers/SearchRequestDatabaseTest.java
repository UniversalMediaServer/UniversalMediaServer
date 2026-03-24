package net.pms.network.mediaserver.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.network.mediaserver.handlers.message.SearchRequest;
import net.pms.store.StoreResource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchRequestDatabaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(SearchRequestDatabaseTest.class.getName());

	// Order:
	// 0=album, 1=artist, 2=albumArtist, 3=songname, 4=genre, 5=year, 6=track, 7=disc, 8=composer, 9=conductor
	private static final List<String[]> AUDIO_METADATA_PRESETS = List.of(
		new String[] {"The Wall", "Pink Floyd", "Pink Floyd", "In the Flesh?", "Rock", "1979", "1", "1", "Roger Waters", null},
		new String[] {"The Dark Side of the Moon", "Pink Floyd", "Pink Floyd", "Time", "Rock", "1973", "4", "1", "Roger Waters", null},
		new String[] {"Rumours", "Fleetwood Mac", "Fleetwood Mac", "Dreams", "Rock", "1977", "2", "1", "Stevie Nicks", null},
		new String[] {"Led Zeppelin IV", "Led Zeppelin", "Led Zeppelin", "Black Dog", "Rock", "1971", "1", "1", "Jimmy Page", null},
		new String[] {"A Night at the Opera", "Queen", "Queen", "Bohemian Rhapsody", "Rock", "1975", "11", "1", "Freddie Mercury", null},
		new String[] {"Back in Black", "AC/DC", "AC/DC", "Hells Bells", "Hard Rock", "1980", "1", "1", "Angus Young", null},
		new String[] {"Thriller", "Michael Jackson", "Michael Jackson", "Billie Jean", "Pop", "1982", "6", "1", "Michael Jackson", null},
		new String[] {"Kind of Blue", "Miles Davis", "Miles Davis", "So What", "Jazz", "1959", "1", "1", "Miles Davis", null},
		new String[] {"Blue Train", "John Coltrane", "John Coltrane", "Blue Train", "Jazz", "1957", "1", "1", "John Coltrane", null},
		new String[] {"The Köln Concert", "Keith Jarrett", "Keith Jarrett", "Part I", "Jazz", "1975", "1", "1", "Keith Jarrett", null},
		new String[] {"Random Access Memories", "Daft Punk", "Daft Punk", "Get Lucky", "Electronic", "2013", "8", "1", "Thomas Bangalter", null},
		new String[] {"Discovery", "Daft Punk", "Daft Punk", "Harder Better Faster Stronger", "Electronic", "2001", "4", "1", "Thomas Bangalter", null},
		new String[] {"Nevermind", "Nirvana", "Nirvana", "Smells Like Teen Spirit", "Grunge", "1991", "1", "1", "Kurt Cobain", null},
		new String[] {"OK Computer", "Radiohead", "Radiohead", "Paranoid don't Android", "Alternative", "1997", "2", "1", "Thom Yorke", null},
		new String[] {"In Rainbows", "Radiohead", "Radiohead", "Nude", "Alternative", "2007", "3", "1", "Thom Yorke", null},
		new String[] {"Requiem", "W. A. Mozart", "Various Artists", "Lacrimosa", "Classical", "1791", "8", "1", "W. A. Mozart", "Herbert von Karajan"},
		new String[] {"Symphony No. 5", "Ludwig van Beethoven", "Various Artists", "I. Allegro con brio", "Classical", "1808", "1", "1", "Ludwig van Beethoven", "Carlos Kleiber"},
		new String[] {"The Four Seasons", "Antonio Vivaldi", "Various Artists", "Spring: Allegro", "Classical", "1725", "1", "1", "Antonio Vivaldi", "Claudio Abbado"},
		new String[] {"Appetite for Destruction", "Guns N' Roses", "Guns N' Roses", "Welcome to the Jungle", "Hard Rock", "1987", "1", "1", "Slash", null},
		new String[] {"Master of Puppets", "Metallica", "Metallica", "Battery", "Metal", "1986", "1", "1", "James Hetfield", null}
	);

	@BeforeAll
	public void setupTestDatabase() throws Exception {
		System.setProperty("surefire.real.class.path", "/tmp");
//		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		database.checkTables(true);
		try (Connection connection = database.getConnection()) {
			for (int i = 0; i < AUDIO_METADATA_PRESETS.size(); i++) {
				MediaInfo media = createMediaInfoFromPreset(i);
				MediaTableFiles.insertOrUpdateData(connection, media.getAudioMetadata().getSongname() + ".flac", System.currentTimeMillis(), Format.AUDIO, media);
			}
		}
		RendererConfigurations.loadRendererConfigurations();
	}

	@Test
	public void testAlbumArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"AlbumArtist\"] contains \"Various\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Various Artists", foundResource.getName());
	}

	@Test
	public void testGlobalLinnAppSpecialCharSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.item.audioItem\" and dc:title contains \"don't\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);
		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
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
		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Discovery", foundResource.getName());
	}

	@Test
	public void testGlobalArtistSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and dc:title contains \"punk\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);

		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(results, resources.size());

		List<String> names = resources.stream().map(StoreResource::getName).collect(Collectors.toList());
		assertTrue(names.contains("Pink Floyd"), "Expected 'Pink Floyd' in results.");
		assertTrue(names.contains("Daft Punk"), "Expected 'Daft Punk' in results.");
		assertTrue(names.contains("Guns N' Roses"), "Expected 'Guns N' Roses' in results.");
	}

	@Test
	public void testGlobalComposerSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Composer\"] contains \"Beethoven\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Ludwig van Beethoven", foundResource.getName());
	}

	@Test
	public void testGlobalConductorSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class derivedfrom \"object.container.person.musicArtist\" and upnp:artist[@role=\"Conductor\"] contains \"Karajan\"");
		sr.setContainerId("0");
		sr.setRequestedCount(900);
		sr.setStartingIndex(0);

		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Herbert von Karajan", foundResource.getName());
	}

	@Test
	public void testGlobalExactAlbumSearch() {
		SearchRequest sr = new SearchRequest();
		sr.setSearchCriteria("upnp:class = \"object.container.album\" and dc:title contains \"rumors\"");
		sr.setContainerId("0");
		sr.setRequestedCount(0);
		sr.setStartingIndex(0);
		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
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
		LucenseSearchRequestHandler searchRequestHandler = new LucenseSearchRequestHandler(sr);
		int results = searchRequestHandler.getSearchCountElements(sr);
		assertEquals(1, results);
		List<StoreResource> resources = searchRequestHandler.getLibraryResourceFromSQL(RendererConfigurations.getDefaultRenderer());
		assertEquals(1, resources.size());
		StoreResource foundResource = resources.get(0);
		assertEquals("Master of Puppets", foundResource.getName());
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
}
