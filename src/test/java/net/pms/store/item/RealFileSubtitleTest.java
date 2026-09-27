package net.pms.store.item;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RealFileSubtitleTest {
	@BeforeAll
	public static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void standaloneSubtitlesDoNotResolveMediaMetadata() throws Exception {
		for (String extension : new String[] {"ass", "sub", "idx", "srt", "vtt"}) {
			Path path = Files.createTempFile("ums-subtitle-", "." + extension);
			try {
				Files.writeString(path, "1\n00:00:00,000 --> 00:00:01,000\nTest\n");
				// Rejected standalone subtitles should not even need a renderer to parse metadata.
				RealFile file = new RealFile(null, path.toFile());
				assertDoesNotThrow(file::resolve, extension);
				assertEquals(Format.SUBTITLE, file.getType(), extension);
				assertNull(file.getMediaInfo(), extension);
				assertFalse(file.isValid(), extension);
				assertTrue(Files.exists(path), "External subtitle must remain available to its video");
			} finally {
				Files.deleteIfExists(path);
			}
		}
	}
}
