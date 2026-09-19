package net.pms.util;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SubtitleFolderCacheTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
	}

	@Test
	public void expirationIsCheckedBetweenGlobalCleanups() {
		File folder = new File("subtitle-cache-" + UUID.randomUUID());
		long now = System.currentTimeMillis();
		var first = SubtitleUtils.getCachedSubtitleFolder(folder, false, now);
		assertSame(first, SubtitleUtils.getCachedSubtitleFolder(folder, false, now + 300000));
		// Only 1 ms after the last sweep, but the requested entry must expire.
		var renewed = SubtitleUtils.getCachedSubtitleFolder(folder, false, now + 300001);
		assertNotSame(first, renewed);
		assertSame(renewed, SubtitleUtils.getCachedSubtitleFolder(folder, false, now + 300002));
		assertNotSame(renewed, SubtitleUtils.getCachedSubtitleFolder(folder, true, now + 300003));
	}

	@Test
	public void concurrentLookupsShareEntryAndRefreshKeepsOldSnapshotIntact() throws Exception {
		File folder = new File("subtitle-cache-" + UUID.randomUUID());
		long now = System.currentTimeMillis();
		var executor = Executors.newFixedThreadPool(4);
		try {
			var futures = new java.util.ArrayList<Future<SubtitleUtils.CacheFolder>>();
			for (int i = 0; i < 20; i++) {
				futures.add(executor.submit(() -> SubtitleUtils.getCachedSubtitleFolder(folder, false, now)));
			}
			var first = futures.get(0).get(2, TimeUnit.SECONDS);
			for (var future : futures) { assertSame(first, future.get(2, TimeUnit.SECONDS)); }
			File subtitle = new File(folder, "movie.srt");
			first.setItems(new File[]{subtitle});
			var fresh = SubtitleUtils.getCachedSubtitleFolder(folder, true, now);
			assertFalse(fresh.isPopulated());
			assertArrayEquals(new File[]{subtitle}, first.getItems());
		} finally {
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
		}
	}
}
