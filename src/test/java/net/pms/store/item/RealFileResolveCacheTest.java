package net.pms.store.item;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RealFileResolveCacheTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	private static final class CountingFile extends File {
		private int checks;
		private CountingFile() {
			super("cached-video.mp4");
		}
		@Override
		public long lastModified() {
			return 1;
		}
		@Override
		public boolean isFile() {
			checks++;
			return false;
		}
	}

	@Test
	public void resolvedMetadataSkipsFileChecks() {
		CountingFile file = new CountingFile();
		RealFile item = new RealFile(null, file);
		MediaInfo media = new MediaInfo();
		media.setMediaParser("test");
		item.setMediaInfo(media);
		for (int i = 0; i < 100; i++) {
			item.resolve();
		}
		assertEquals(0, file.checks);
		assertSame(media, item.getMediaInfo());
	}

	@Test
	public void unresolvedMetadataStillChecksFile() {
		CountingFile file = new CountingFile();
		RealFile item = new RealFile(null, file);
		item.resolve();
		assertEquals(1, file.checks);
		item.setMediaInfo(new MediaInfo());
		item.resolve();
		assertEquals(2, file.checks);
	}
}
