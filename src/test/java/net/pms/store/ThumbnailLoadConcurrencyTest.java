package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import org.junit.jupiter.api.Test;

public class ThumbnailLoadConcurrencyTest {
	private static DLNAThumbnail thumbnail() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
		return DLNAThumbnail.toThumbnail(bytes.toByteArray(), 2, 2, ScaleType.MAX, ImageFormat.SOURCE, false);
	}

	@Test
	public void slowMissDoesNotBlockOtherIdsAndSameIdLoadsOnce() throws Exception {
		var pool = Executors.newFixedThreadPool(3);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger calls = new AtomicInteger();
		DLNAThumbnail image = thumbnail();
		long cachedId = ThumbnailStore.getTempId(image);
		long id = -1234567L;
		// These different IDs collided in the former 64-lock array.
		assertEquals(Math.floorMod(Long.hashCode(id), 64), Math.floorMod(Long.hashCode(id + 64), 64));
		try {
			var slow = pool.submit(() -> ThumbnailStore.getThumbnail(id, key -> {
				calls.incrementAndGet();
				started.countDown();
				try {
					if (!release.await(5, TimeUnit.SECONDS)) {
						throw new AssertionError("Timed out waiting for test release");
					}
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				}
				return image;
			}));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			var same = pool.submit(() -> ThumbnailStore.getThumbnail(id, key -> {
				calls.incrementAndGet();
				return image;
			}));
			var independent = pool.submit(() -> {
				assertSame(image, ThumbnailStore.getThumbnail(cachedId, key -> fail("Cache miss")));
				assertSame(image, ThumbnailStore.getThumbnail(id + 64, key -> image));
			});
			independent.get(2, TimeUnit.SECONDS);
			release.countDown();
			assertSame(image, slow.get(5, TimeUnit.SECONDS));
			assertSame(image, same.get(5, TimeUnit.SECONDS));
			assertEquals(1, calls.get());
		} finally {
			release.countDown();
			pool.shutdownNow();
		}
	}

	@Test
	public void failedLoadCanBeRetried() throws Exception {
		long id = -2234567L;
		assertNull(ThumbnailStore.getThumbnail(null, key -> fail("Null ID")));
		assertNull(ThumbnailStore.getThumbnail(id, key -> null));
		assertThrows(IllegalStateException.class, () -> ThumbnailStore.getThumbnail(id, key -> {
			throw new IllegalStateException("unavailable");
		}));
		DLNAThumbnail image = thumbnail();
		assertSame(image, ThumbnailStore.getThumbnail(id, key -> image));
	}
}
