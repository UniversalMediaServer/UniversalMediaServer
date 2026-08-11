package net.pms.database;

import java.sql.Connection;
import java.util.List;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableResourceRatings.ResourceRating;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdTypeAndIdent;
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
	 * The rating key of an album container is its DBID system name, which embeds
	 * the MusicBrainz release id. It must not contain any generated id, otherwise
	 * album likes would not survive a database rebuild.
	 */
	@Test
	public void testAlbumLikeIsKeyedOnReleaseId() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String mbid = "11111111-2222-3333-4444-555555555555";
			String expectedKey = "$DBID$MUSICBRAINZALBUM$" + mbid;
			assertEquals(expectedKey, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID.getResourceKey(mbid));
			//the key must be exactly what the album container reports as system name
			assertEquals(expectedKey, new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid).toString());

			assertFalse(MediaTableResourceRatings.isAlbumLiked(connection, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid));

			MediaTableResourceRatings.setAlbumLiked(connection, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid, true);
			assertTrue(MediaTableResourceRatings.isAlbumLiked(connection, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid));
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, expectedKey));

			//unliking removes the rating, it does not store a dislike
			MediaTableResourceRatings.setAlbumLiked(connection, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid, false);
			assertFalse(MediaTableResourceRatings.isAlbumLiked(connection, DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbid));
			assertNull(MediaTableResourceRatings.getRating(connection, expectedKey));
		}
	}

	/**
	 * A dislike is a stored 0 and must be distinguishable from "not rated".
	 */
	@Test
	public void testDislikeIsNotTheSameAsUnrated() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String key = "/media/music/testDislikeIsNotTheSameAsUnrated";
			MediaTableResourceRatings.setRating(connection, key, "RealFolder", MediaTableResourceRatings.RATING_DISLIKED);
			assertEquals(Integer.valueOf(0), MediaTableResourceRatings.getRating(connection, key));

			MediaTableResourceRatings.setRating(connection, key, "RealFolder", null);
			assertNull(MediaTableResourceRatings.getRating(connection, key));
		}
	}

	/**
	 * Existing likes of the legacy tables have to end up in this table, otherwise
	 * the My Albums folder would look empty after the upgrade.
	 */
	@Test
	public void testLegacyAlbumLikesAreMigrated() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String mbid = "99999999-8888-7777-6666-555555555555";
			long discogs = 424242L;
			String mbKey = DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID.getResourceKey(mbid);
			String discogsKey = DbIdMediaType.TYPE_DISCOGS_RELEASEID.getResourceKey(Long.toString(discogs));

			MediaTableResourceRatings.deleteRating(connection, mbKey);
			MediaTableResourceRatings.deleteRating(connection, discogsKey);
			MediaDatabase.execute(connection,
				"MERGE INTO " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " KEY (MBID_RELEASE) VALUES ('" + mbid + "')",
				"MERGE INTO " + MediaTableDiscogsReleaseLike.TABLE_NAME + " KEY (DISCOGS_RELEASE_ID) VALUES (" + discogs + ")"
			);

			MediaTableResourceRatings.migrateAlbumLikes(connection);

			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, mbKey));
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, discogsKey));

			//running it twice must not fail on the unique key
			MediaTableResourceRatings.migrateAlbumLikes(connection);
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, mbKey));

			//and the export direction has to reproduce the legacy content
			MediaTableResourceRatings.exportAlbumLikes(connection);
			MediaTableResourceRatings.deleteRating(connection, mbKey);
			MediaTableResourceRatings.migrateAlbumLikes(connection);
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, mbKey));

			MediaTableResourceRatings.deleteRating(connection, mbKey);
			MediaTableResourceRatings.deleteRating(connection, discogsKey);
		}
	}

	/**
	 * Test album rating, no matter whether it was rated, so we write always the same object type.
	 */
	@Test
	public void testAlbumRowsCarryTheAlbumObjectType() throws Exception {
		MediaDatabase.init();
		MediaDatabase database = MediaDatabase.get();
		try (Connection connection = database.getConnection()) {
			String mbKey = DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID.getResourceKey("dcada7c8-5e32-427e-bb83-0711e9f24c03");
			String discogsKey = DbIdMediaType.TYPE_DISCOGS_RELEASEID.getResourceKey("13804609");
			String fileKey = "/media/music/testAlbumRowsCarryTheAlbumObjectType/01 - Dark.flac";

			//an album folder rated in the file tree is stored as the album it holds
			MediaTableResourceRatings.setRating(connection, mbKey, "RealFolder", 5);
			MediaTableResourceRatings.setRating(connection, discogsKey, "MusicAlbumFolder", 5);
			MediaTableResourceRatings.setRating(connection, fileKey, "RealFile", 5);

			List<ResourceRating> all = MediaTableResourceRatings.getAllRatings(connection);
			assertEquals(MediaTableResourceRatings.MUSIC_ALBUM_OBJECT_TYPE, objectTypeOf(all, mbKey));
			assertEquals(MediaTableResourceRatings.MUSIC_ALBUM_OBJECT_TYPE, objectTypeOf(all, discogsKey));
			//rows that are not albums keep their type
			assertEquals("RealFile", objectTypeOf(all, fileKey));

			MediaTableResourceRatings.deleteRating(connection, mbKey);
			MediaTableResourceRatings.deleteRating(connection, discogsKey);
			MediaTableResourceRatings.deleteRating(connection, fileKey);
		}
	}

	private static String objectTypeOf(List<ResourceRating> ratings, String resourceKey) {
		return ratings.stream()
			.filter(rating -> resourceKey.equals(rating.resourceKey()))
			.map(ResourceRating::objectType)
			.findFirst()
			.orElse(null);
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
