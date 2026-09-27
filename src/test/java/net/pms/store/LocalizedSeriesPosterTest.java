package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.video.metadata.TvSeriesMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalizedSeriesPosterTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void successfulRequestIsReused() {
		TvSeriesMetadata media = media();
		AtomicInteger calls = new AtomicInteger();
		BiFunction<String, Long, Long> update = (uri, path) -> {
			calls.incrementAndGet();
			assertEquals("https://example.test/poster.jpg", uri);
			assertEquals(12L, path);
			return 42L;
		};
		ThumbnailStore.resolveLocalizedSeriesPoster(media, update);
		ThumbnailStore.resolveLocalizedSeriesPoster(media, update);
		assertEquals(1, calls.get());
		assertEquals(42L, media.getThumbnailId());
		assertEquals(ThumbnailSource.TMDB_LOC, media.getThumbnailSource());
	}

	@Test
	public void failedDownloadKeepsOldThumbnailAndAllowsRetry() {
		TvSeriesMetadata media = media();
		ThumbnailStore.resolveLocalizedSeriesPoster(media, (uri, path) -> null);
		assertEquals(7L, media.getThumbnailId());
		assertEquals(ThumbnailSource.TMDB, media.getThumbnailSource());
		ThumbnailStore.resolveLocalizedSeriesPoster(media, (uri, path) -> 42L);
		assertEquals(42L, media.getThumbnailId());
	}

	@Test
	public void skipsUserAndAlreadyLocalizedCovers() {
		for (ThumbnailSource source : new ThumbnailSource[] {ThumbnailSource.USER, ThumbnailSource.TMDB_LOC}) {
			TvSeriesMetadata media = media();
			media.setThumbnailSource(source);
			ThumbnailStore.resolveLocalizedSeriesPoster(media, (uri, path) -> {
				fail("Existing cover must not be replaced");
				return null;
			});
			assertEquals(7L, media.getThumbnailId());
		}
	}

	@Test
	public void missingOrInvalidPosterDoesNotDownload() {
		BiFunction<String, Long, Long> unexpected = (uri, path) -> {
			fail("Must not download");
			return null;
		};
		ThumbnailStore.resolveLocalizedSeriesPoster(null, unexpected);
		TvSeriesMetadata invalid = media();
		invalid.setTvSeriesId(-1L);
		ThumbnailStore.resolveLocalizedSeriesPoster(invalid, unexpected);
		ThumbnailStore.resolveLocalizedSeriesPoster(new TvSeriesMetadata(), unexpected);
		for (String poster : new String[] {null, "", "relative.jpg"}) {
			TvSeriesMetadata media = media();
			media.setPoster(poster);
			ThumbnailStore.resolveLocalizedSeriesPoster(media, unexpected);
		}
	}

	@Test
	public void concurrentRequestsShareOneDownload() throws Exception {
		TvSeriesMetadata media = media();
		AtomicInteger calls = new AtomicInteger();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(2);
		try {
			BiFunction<String, Long, Long> update = (uri, path) -> {
				calls.incrementAndGet();
				started.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				return 42L;
			};
			var first = pool.submit(() -> ThumbnailStore.resolveLocalizedSeriesPoster(media, update));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			var second = pool.submit(() -> ThumbnailStore.resolveLocalizedSeriesPoster(media, update));
			release.countDown();
			first.get(5, TimeUnit.SECONDS);
			second.get(5, TimeUnit.SECONDS);
			assertEquals(1, calls.get());
		} finally {
			release.countDown();
			pool.shutdownNow();
		}
	}

	private static TvSeriesMetadata media() {
		TvSeriesMetadata metadata = new TvSeriesMetadata();
		metadata.setPoster("https://example.test/poster.jpg");
		TvSeriesMetadata media = metadata;
		media.setTvSeriesId(12L);
		media.setThumbnailId(7L);
		media.setThumbnailSource(ThumbnailSource.TMDB);
		return media;
	}
}
