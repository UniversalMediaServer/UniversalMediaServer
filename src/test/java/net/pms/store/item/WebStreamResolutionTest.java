/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store.item;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.TranscodingSettings;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.container.FileTranscodeVirtualFolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WebStreamResolutionTest {

	private static Renderer renderer;

	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
		renderer = new Renderer(new RendererConfiguration(new File("src/main/external-resources/renderers/VLC-for-desktop.conf")));
	}

	@Test
	void cachedStreamIsAvailableImmediatelyAndReplacesPrematureTranscoding() {
		String url = newUrl();
		MediaInfo parsed = parsedVideo();
		MediaInfoStore.storeMediaInfo(url, parsed);
		try {
			WebVideoStream stream = new WebVideoStream(renderer, "VLC stream", url, null);
			assertTrue(stream.isValid());
			stream.setTranscodingSettings(new TranscodingSettings(null, null));
			stream.resolveInBackground();
			assertSame(parsed, stream.getMediaInfo());
			assertTrue(stream.isTranscoded(), "The browse decision remains provisional until playback");

			stream.prepareForPlayback();
			assertFalse(stream.isTranscoded(), "VLC supports video/mpeg without transcoding");
		} finally {
			MediaInfoStore.removeMediaEntryFromCache(url);
		}
	}

	@Test
	void everyRendererCopyReceivesAnOngoingProbe() throws Exception {
		checkOngoingProbe(false, false);
	}

	@Test
	void timeoutLeavesSharedProbeAvailableToOtherResources() throws Exception {
		checkOngoingProbe(true, false);
	}

	@Test
	void interruptionPreservesInterruptAndLeavesSharedProbeRunning() throws Exception {
		checkOngoingProbe(false, true);
	}

	private void checkOngoingProbe(boolean timeOut, boolean interrupt) throws Exception {
		String url = newUrl();
		MediaInfo parsed = parsedVideo();
		Object lock = storeMethod("acquireLock", String.class).invoke(null, url);
		CountDownLatch resolved = new CountDownLatch(2);
		WebVideoStream first = observingStream(url, resolved);
		WebVideoStream second = observingStream(url, resolved);
		try {
			// Hold the real per-URL load lock so browsing returns while the probe is still pending.
			synchronized (lock) {
				first.resolveInBackground();
				second.resolveInBackground();
				assertNull(first.getMediaInfo());
				assertNull(second.getMediaInfo());
				if (timeOut) {
					assertFalse(second.awaitMediaInfo(10, TimeUnit.MILLISECONDS));
				} else if (interrupt) {
					Thread.currentThread().interrupt();
					try {
						assertFalse(second.awaitMediaInfo(1, TimeUnit.SECONDS));
						assertTrue(Thread.currentThread().isInterrupted());
					} finally {
						Thread.interrupted();
					}
				}
				// Supply the result before releasing the loader; no external stream is needed in this test.
				MediaInfoStore.storeMediaInfo(url, parsed);
			}
			assertTrue(resolved.await(3, TimeUnit.SECONDS), "Both renderer copies must receive the same probe result");
			assertSame(parsed, first.getMediaInfo());
			assertSame(parsed, second.getMediaInfo());
			assertTrue(second.awaitMediaInfo(1, TimeUnit.SECONDS));
		} finally {
			storeMethod("releaseLock", String.class, lock.getClass()).invoke(null, url, lock);
			MediaInfoStore.removeMediaEntryFromCache(url);
		}
	}

	@Test
	void explicitTranscodeFolderChoiceIsPreserved() {
		WebVideoStream stream = new WebVideoStream(renderer, "VLC stream", newUrl(), null);
		stream.setMediaInfo(parsedVideo());
		stream.setParent(new FileTranscodeVirtualFolder(renderer, stream));
		TranscodingSettings selected = new TranscodingSettings(null, null);
		stream.setTranscodingSettings(selected);
		stream.prepareForPlayback();
		assertSame(selected, stream.getTranscodingSettings());
	}

	@Test
	void cachedProbePreservesStationMetadata() {
		String url = newUrl();
		MediaInfoStore.storeMediaInfo(url, parsedVideo());
		try {
			WebVideoStream stream = new WebVideoStream(renderer, "Station", url, null);
			MediaInfo previous = new MediaInfo();
			MediaAudioMetadata metadata = new MediaAudioMetadata();
			metadata.setAlbum("Station album");
			metadata.setArtist("Station artist");
			previous.setAudioMetadata(metadata);
			stream.setMediaInfo(previous);
			stream.resolveInBackground();
			assertEquals("Station album", stream.getMediaInfo().getAudioMetadata().getAlbum());
			assertEquals("Station artist", stream.getMediaInfo().getAudioMetadata().getArtist());
		} finally {
			MediaInfoStore.removeMediaEntryFromCache(url);
		}
	}

	private static WebVideoStream observingStream(String url, CountDownLatch resolved) {
		return new WebVideoStream(renderer, "VLC stream", url, null) {
			@Override
			public void setMediaInfo(MediaInfo media) {
				super.setMediaInfo(media);
				resolved.countDown();
			}
		};
	}

	private static String newUrl() {
		return "http://127.0.0.1/stream-" + UUID.randomUUID() + ".m2t";
	}

	private static MediaInfo parsedVideo() {
		MediaInfo media = new MediaInfo();
		media.setContainer("mpegts");
		media.setMimeType("video/mpeg");
		media.setMediaParser("WEBSTREAM");
		return media;
	}

	private static Method storeMethod(String name, Class<?>... types) throws Exception {
		Method method = MediaInfoStore.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}
}
