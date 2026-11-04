package net.pms.store.item;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RealFileNameCacheTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void repeatedLanguageReusesPrettifiedName() {
		CountingMetadata metadata = new CountingMetadata();
		RealFile file = file(metadata);
		String first = file.getLocalizedDisplayName("cs-CZ");
		for (int i = 0; i < 100; i++) {
			assertEquals(first, file.getLocalizedDisplayName("cs-cz"));
		}
		assertEquals(1, metadata.builds);
	}

	@Test
	public void changingLanguageRebuildsName() {
		CountingMetadata metadata = new CountingMetadata();
		RealFile file = file(metadata);
		String czech = file.getLocalizedDisplayName("cs");
		String english = file.getLocalizedDisplayName("en");
		assertNotEquals(czech, english);
		assertEquals(2, metadata.builds);
		assertEquals(english, file.getLocalizedDisplayName("en"));
		assertEquals(2, metadata.builds);
		assertEquals(czech, file.getLocalizedDisplayName("cs"));
		assertEquals(3, metadata.builds);
	}

	@Test
	public void defaultLanguageReusesSameCacheAsExplicitLanguage() {
		CountingMetadata metadata = new CountingMetadata();
		RealFile file = file(metadata);
		String initial = file.getLocalizedDisplayName(null);
		String language = PMS.getConfiguration().getTranslationLanguage(null);
		assertEquals(initial, file.getLocalizedDisplayName(language));
		assertEquals(initial, file.getLocalizedDisplayName(null));
		assertEquals(1, metadata.builds);
	}

	@Test
	public void completedTranslationInvalidatesNameCache() {
		CountingMetadata metadata = new CountingMetadata();
		RealFile file = file(metadata);
		file.getLocalizedDisplayName("cs");
		metadata.setTranslations(java.util.Map.of());
		file.getLocalizedDisplayName("cs");
		assertEquals(2, metadata.builds);
		file.getLocalizedDisplayName("cs");
		assertEquals(2, metadata.builds);
	}
	@Test
	public void translatedTitleReplacesFallbackOnNextRead() {
		MediaVideoMetadata metadata = new MediaVideoMetadata();
		metadata.setTitle("Original title");
		RealFile file = file(metadata);
		assertTrue(file.getLocalizedDisplayName("cs").contains("Original title"));
		var translation = new net.pms.media.video.metadata.VideoMetadataLocalized();
		translation.setTitle("Cesky nazev");
		metadata.setTranslations(java.util.Map.of("cs", translation));
		assertTrue(file.getLocalizedDisplayName("cs").contains("Cesky nazev"));
	}
	private static RealFile file(MediaVideoMetadata metadata) {
		RealFile file = new RealFile(null, new File("Movie.2024.mkv")) {
			@Override
			public String getDisplayName() {
				// Exercise the real prettifier without renderer-specific suffixes.
				return getBaseNamePrettified();
			}
		};
		MediaInfo media = new MediaInfo();
		media.setVideoMetadata(metadata);
		file.setMediaInfo(media);
		return file;
	}

	private static class CountingMetadata extends MediaVideoMetadata {
		private int builds;

		@Override
		public String getMovieOrShowName() {
			return "Movie";
		}

		@Override
		public String getMovieOrShowName(String language) {
			return language + " Movie";
		}

		@Override
		public void ensureHavingTranslation(String language) {
			builds++;
		}
	}
}
