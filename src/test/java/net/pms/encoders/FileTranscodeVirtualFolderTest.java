package net.pms.encoders;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.MPG;
import net.pms.media.*;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.store.item.RealFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FileTranscodeVirtualFolderTest {

	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		var config = new UmsConfiguration(false);
		config.setChapterSupport(false);
		PMS.setConfiguration(config);
	}

	private static MediaSubtitle subtitle(int id, String lang) {
		var sub = new MediaSubtitle();
		sub.setId(id);
		sub.setLang(lang);
		return sub;
	}

	private static RealFile item(Renderer renderer) {
		var item =
				new RealFile(renderer, new File("test.mkv")) {
					@Override
					public synchronized void syncResolve() {
				// Prevent metadata loading in this folder-generation test.
			}

					@Override
					public void registerExternalSubtitles(boolean forceRefresh) {
				// Use the subtitle tracks supplied by the test.
			}
				};
		var media = new MediaInfo();
		media.addVideoTrack(new MediaVideo());
		item.setMediaInfo(media);
		item.setFormat(new MPG());
		return item;
	}

	@SuppressWarnings("unchecked")
	private static List<MediaSubtitle> choices(StoreItem item) {
		try {
			var method = FileTranscodeVirtualFolder.class.getDeclaredMethod("getSubtitleChoices", StoreItem.class);
			method.setAccessible(true);
			return (List<MediaSubtitle>) method.invoke(null, item);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void selectedTrackDoesNotHideOtherTracksOrOffOption() {
		var item = item(null);
		var first = subtitle(0, "ces");
		var second = subtitle(1, "eng");
		item.getMediaInfo().setSubtitlesTracks(new ArrayList<>(List.of(first, second)));
		item.setMediaSubtitle(first);
		var choices = choices(item);
		assertEquals(3, choices.size());
		assertSame(first, choices.get(0));
		assertSame(second, choices.get(1));
		assertEquals(MediaLang.DUMMY_ID, choices.get(2).getId());
		assertEquals(2, item.getMediaInfo().getSubtitlesTracks().size());
		assertSame(first, item.getMediaSubtitle());
	}

	@Test
	void keepsSeparatelySelectedTrackWithoutDuplicatingOffOption() {
		var item = item(null);
		var original = subtitle(0, "ces");
		var downloaded = subtitle(100, "eng");
		item.getMediaInfo()
				.setSubtitlesTracks(new ArrayList<>(List.of(original, subtitle(MediaLang.DUMMY_ID, null))));
		item.setMediaSubtitle(downloaded);
		var choices = choices(item);
		assertEquals(3, choices.size());
		assertSame(downloaded, choices.get(1));
		item.setMediaSubtitle(subtitle(MediaLang.DUMMY_ID, null));
		choices = choices(item);
		assertEquals(2, choices.size());
		assertSame(original, choices.get(0));
	}

	@Test
	void noSubtitlesStillOffersEngines() {
		var choices = choices(item(null));
		assertEquals(1, choices.size());
		assertNull(choices.get(0));
	}

	private static class TestEngine extends FFMpegVideo {
		private final boolean subtitles;

		TestEngine(boolean subtitles) {
			this.subtitles = subtitles;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public boolean isCompatible(EncodingFormat format) {
			return true;
		}

		@Override
		public boolean isCompatible(StoreItem item) {
			return subtitles ||
					item.getMediaSubtitle() == null ||
					item.getMediaSubtitle().getId() == MediaLang.DUMMY_ID;
		}
	}

	@Test
	void folderContainsEveryCompatibleEngineAudioSubtitleCombination() throws Exception {
		Renderer renderer =
				new Renderer((String) null) {
					@Override
					public List<EncodingFormat> getTranscodingFormats() {
						return List.of(EncodingFormat.VIDEO.get("MPEGTS-H264-AAC"));
					}

					@Override
					public boolean isSubtitlesStreamingSupportedForAllFiletypes() {
						return false;
					}
				};
		var item = item(renderer);
		for (int i = 0; i < 2; i++) {
			var audio = new MediaAudio();
			audio.setId(i);
			audio.setLang(i == 0 ? "ces" : "eng");
			item.getMediaInfo().addAudioTrack(audio);
		}
		var first = subtitle(0, "ces");
		var second = subtitle(1, "eng");
		item.getMediaInfo().setSubtitlesTracks(new ArrayList<>(List.of(first, second)));
		item.setMediaSubtitle(first);
		var all = new TestEngine(true);
		var withoutSubtitles = new TestEngine(false);
		var field = EngineFactory.class.getDeclaredField("ENGINES");
		field.setAccessible(true);
		var lockField = EngineFactory.class.getDeclaredField("ENGINES_LOCK");
		lockField.setAccessible(true);
		var lock = (ReentrantReadWriteLock) lockField.get(null);
		lock.writeLock().lock();
		@SuppressWarnings("unchecked")
		var engines = (List<Engine>) field.get(null);
		var saved = new ArrayList<>(engines);
		try {
			engines.clear();
			engines.add(all);
			engines.add(withoutSubtitles);
			var folder = new FileTranscodeVirtualFolder(renderer, item);
			folder.discoverChildren();
			assertEquals(9, folder.getChildren().size()); // direct + 2 audio * (3 subtitle choices + off-only engine)
			for (var audio : item.getMediaInfo().getAudioTracks()) {
				for (var sub : List.of(first, second)) {
					assertEquals(1, folder.getChildren().stream()
									.filter(
											r -> r instanceof StoreItem copy &&
													copy.getMediaAudio() == audio &&
													copy.getMediaSubtitle() == sub &&
													copy.getTranscodingSettings() != null &&
													copy.getTranscodingSettings().getEngine() == all)
									.count());
				}
				for (var engine : List.of(all, withoutSubtitles)) {
					assertEquals(1, folder.getChildren().stream()
									.filter(
											r -> r instanceof StoreItem copy &&
													copy.getMediaAudio() == audio &&
													copy.getMediaSubtitle() != null &&
													copy.getMediaSubtitle().getId() == MediaLang.DUMMY_ID &&
													copy.getTranscodingSettings().getEngine() == engine)
									.count());
				}
			}
			folder.discoverChildren();
			assertEquals(9, folder.getChildren().size());
			assertSame(first, item.getMediaSubtitle());
		} finally {
			engines.clear();
			engines.addAll(saved);
			lock.writeLock().unlock();
		}
	}
}
