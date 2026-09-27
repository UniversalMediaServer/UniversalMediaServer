package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalizedPosterTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void successfulRequestIsReused() {
		MediaInfo media = media();
		AtomicInteger calls = new AtomicInteger();
		BiFunction<String, String, Long> update = (uri, path) -> {
			calls.incrementAndGet();
			assertEquals("https://example.test/poster.jpg", uri);
			assertEquals("movie.mkv", path);
			return 42L;
		};
		ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", update);
		ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", update);
		assertEquals(1, calls.get());
		assertEquals(42L, media.getThumbnailId());
		assertEquals(ThumbnailSource.TMDB_LOC, media.getThumbnailSource());
	}

	@Test
	public void failedDownloadKeepsOldThumbnailAndAllowsRetry() {
		MediaInfo media = media();
		ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", (uri, path) -> null);
		assertEquals(7L, media.getThumbnailId());
		assertEquals(ThumbnailSource.TMDB, media.getThumbnailSource());
		ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", (uri, path) -> 42L);
		assertEquals(42L, media.getThumbnailId());
	}

	@Test
	public void skipsUserAndAlreadyLocalizedCovers() {
		for (ThumbnailSource source : new ThumbnailSource[] {ThumbnailSource.USER, ThumbnailSource.TMDB_LOC}) {
			MediaInfo media = media();
			media.setThumbnailSource(source);
			ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", (uri, path) -> {
				fail("Existing cover must not be replaced");
				return null;
			});
			assertEquals(7L, media.getThumbnailId());
		}
	}

	@Test
	public void missingOrInvalidPosterDoesNotDownload() {
		BiFunction<String, String, Long> unexpected = (uri, path) -> {
			fail("Must not download");
			return null;
		};
		ThumbnailStore.resolveLocalizedPoster(null, "movie.mkv", unexpected);
		ThumbnailStore.resolveLocalizedPoster(media(), null, unexpected);
		ThumbnailStore.resolveLocalizedPoster(new MediaInfo(), "movie.mkv", unexpected);
		for (String poster : new String[] {null, "", "relative.jpg"}) {
			MediaInfo media = media();
			media.getVideoMetadata().setPoster(poster);
			ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", unexpected);
		}
	}

	@Test
	public void concurrentRequestsShareOneDownload() throws Exception {
		MediaInfo media = media();
		AtomicInteger calls = new AtomicInteger();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(2);
		try {
			BiFunction<String, String, Long> update = (uri, path) -> {
				calls.incrementAndGet();
				started.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				return 42L;
			};
			var first = pool.submit(() -> ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", update));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			var second = pool.submit(() -> ThumbnailStore.resolveLocalizedPoster(media, "movie.mkv", update));
			release.countDown();
			first.get(5, TimeUnit.SECONDS);
			second.get(5, TimeUnit.SECONDS);
			assertEquals(1, calls.get());
		} finally {
			release.countDown();
			pool.shutdownNow();
		}
	}

	private static MediaInfo media() {
		MediaVideoMetadata metadata = new MediaVideoMetadata();
		metadata.setPoster("https://example.test/poster.jpg");
		MediaInfo media = new MediaInfo();
		media.setVideoMetadata(metadata);
		media.setThumbnailId(7L);
		media.setThumbnailSource(ThumbnailSource.TMDB);
		return media;
	}
}
