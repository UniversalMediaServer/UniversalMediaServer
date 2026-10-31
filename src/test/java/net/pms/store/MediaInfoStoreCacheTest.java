package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.*;
import net.pms.formats.Format;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.parsers.Parser;
import org.junit.jupiter.api.Test;

public class MediaInfoStoreCacheTest {
	private static Method method(String name, Class<?>... types) throws Exception {
		Method method = MediaInfoStore.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}

	private static void publish(String key, MediaInfo media, long modified) throws Exception {
		method("storeMediaInfo", String.class, MediaInfo.class, long.class).invoke(null, key, media, modified);
	}

	@Test
	public void cachedFileDoesNotWaitForLoadLock() throws Exception {
		check(false);
	}

	@Test
	public void changedFileWaitsAndUsesFreshMetadata() throws Exception {
		check(true);
	}

	private static MediaInfo parsedAudio() {
		MediaInfo media = new MediaInfo();
		media.addAudioTrack(new MediaAudio());
		media.setMediaParser("JAudiotagger");
		return media;
	}

	@Test
	public void manuallySuppliedMetadataDoesNotRequireTracks() throws Exception {
		String key = "manual-cache-test-" + UUID.randomUUID();
		MediaInfo media = new MediaInfo();
		media.setMediaParser(Parser.MANUAL_PARSER);
		File file = new File(key);
		publish(key, media, file.lastModified());
		try {
			assertSame(media, MediaInfoStore.getMediaInfo(key, file, null, Format.VIDEO));
		} finally {
			MediaInfoStore.removeMediaEntryFromCache(key);
		}
	}

	private void check(boolean changed) throws Exception {
		String key = "cache-test-" + UUID.randomUUID();
		CountDownLatch statRead = new CountDownLatch(1);
		File file = new File(key) {
			@Override public long lastModified() { statRead.countDown(); return changed ? 2 : 1; }
		};
		MediaInfo cached = parsedAudio();
		MediaInfo fresh = parsedAudio();
		publish(key, cached, 1);
		Object lock = method("acquireLock", String.class).invoke(null, key);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<MediaInfo> result;
		try {
			synchronized (lock) {
				result = executor.submit(() -> MediaInfoStore.getMediaInfo(key, file, null, Format.AUDIO));
				if (changed) {
					assertTrue(statRead.await(2, TimeUnit.SECONDS));
					assertThrows(TimeoutException.class, () -> result.get(150, TimeUnit.MILLISECONDS));
					publish(key, fresh, 2);
				} else {
					assertSame(cached, result.get(2, TimeUnit.SECONDS));
				}
			}
			assertSame(changed ? fresh : cached, result.get(2, TimeUnit.SECONDS));
		} finally {
			method("releaseLock", String.class, lock.getClass()).invoke(null, key, lock);
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
			MediaInfoStore.removeMediaEntryFromCache(key);
		}
	}
}
