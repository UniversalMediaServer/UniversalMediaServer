package net.pms.database;

import java.sql.Connection;
import java.util.List;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableResourceRatings.ResourceRating;
import org.apache.commons.configuration2.ex.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediaTableResourceRatingsTest {

	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void testSetGetAndClearRating() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String key = "/media/music/testSetGetAndClearRating.mp3";

			assertNull(MediaTableResourceRatings.getRating(connection, key));

			MediaTableResourceRatings.setRating(connection, key, "RealFile", 4);
			assertEquals(Integer.valueOf(4), MediaTableResourceRatings.getRating(connection, key));

			//overwriting must not create a second row
			MediaTableResourceRatings.setRating(connection, key, "RealFile", 2);
			assertEquals(Integer.valueOf(2), MediaTableResourceRatings.getRating(connection, key));

			//a null rating removes the rating
			MediaTableResourceRatings.setRating(connection, key, "RealFile", null);
			assertNull(MediaTableResourceRatings.getRating(connection, key));
		}
	}

	@Test
	public void testRatingIsIndependentOfObjectType() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String key = "/media/video/testRatingIsIndependentOfObjectType.mkv";

			//the same file browsed as a RealFile and as a MediaLibraryTvEpisode
			//must share one rating
			MediaTableResourceRatings.setRating(connection, key, "RealFile", 3);
			MediaTableResourceRatings.setRating(connection, key, "MediaLibraryTvEpisode", 5);

			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, key));

			long rows = MediaTableResourceRatings.getAllRatings(connection).stream()
				.filter(rating -> key.equals(rating.resourceKey()))
				.count();
			assertEquals(1L, rows);

			MediaTableResourceRatings.deleteRating(connection, key);
		}
	}

	@Test
	public void testContainersAreRatable() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String folderKey = "/media/music/testContainersAreRatable";
			String playlistKey = "/media/music/testContainersAreRatable.m3u";
			String virtualKey = "/Media Library/Audio/testContainersAreRatable";

			MediaTableResourceRatings.setRating(connection, folderKey, "RealFolder", 1);
			MediaTableResourceRatings.setRating(connection, playlistKey, "PlaylistFolder", 2);
			MediaTableResourceRatings.setRating(connection, virtualKey, "MediaLibraryFolder", 3);

			assertEquals(Integer.valueOf(1), MediaTableResourceRatings.getRating(connection, folderKey));
			assertEquals(Integer.valueOf(2), MediaTableResourceRatings.getRating(connection, playlistKey));
			assertEquals(Integer.valueOf(3), MediaTableResourceRatings.getRating(connection, virtualKey));

			List<ResourceRating> all = MediaTableResourceRatings.getAllRatings(connection);
			assertTrue(all.stream().anyMatch(rating -> playlistKey.equals(rating.resourceKey()) && "PlaylistFolder".equals(rating.objectType())));

			MediaTableResourceRatings.deleteRating(connection, folderKey);
			MediaTableResourceRatings.deleteRating(connection, playlistKey);
			MediaTableResourceRatings.deleteRating(connection, virtualKey);
		}
	}

	@Test
	public void testUnknownResourceKeyIsHarmless() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			assertNull(MediaTableResourceRatings.getRating(connection, "/does/not/exist"));
			assertNull(MediaTableResourceRatings.getRating(connection, null));
			//must not throw
			MediaTableResourceRatings.deleteRating(connection, "/does/not/exist");
			MediaTableResourceRatings.setRating(connection, null, "RealFile", 3);
		}
	}

	/**
	 * Ratings are user data and must survive the "Reset the cache" action, which
	 * re-initializes every table except the ones holding user data.
	 */
	@Test
	public void testRatingSurvivesCacheReset() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		String key = "/media/music/testRatingSurvivesCacheReset.mp3";
		try (Connection connection = database.getConnection()) {
			MediaTableResourceRatings.setRating(connection, key, "RealFile", 5);
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, key));
		}
		try (Connection connection = database.getConnection()) {
			MediaDatabase.dropAllTablesExceptFilesStatus(connection);
			database.checkTables(true);
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, key));
			MediaTableResourceRatings.deleteRating(connection, key);
		}
	}
}
