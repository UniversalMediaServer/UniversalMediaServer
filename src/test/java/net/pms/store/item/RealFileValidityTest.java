package net.pms.store.item;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

public class RealFileValidityTest {
	@org.junit.jupiter.api.BeforeAll
	public static void configure() throws Exception {
		System.setProperty(net.pms.PMS.PROPERTY_RUNNING_TESTS, "true");
		net.pms.PMS.setConfiguration(new net.pms.configuration.UmsConfiguration(false));
	}

	@Test
	public void regularFileUsesAttributesAndNotRepeatedFileQueries() throws Exception {
		var path = Files.createTempFile("ums-validity-", ".mp4");
		try {
			File file = new File(path.toString()) {
				@Override public boolean exists() { throw new AssertionError("duplicate exists query"); }
				@Override public boolean isFile() { throw new AssertionError("duplicate isFile query"); }
				@Override public long length() { throw new AssertionError("duplicate size query"); }
			};
			assertTrue(new RealFile(null, file).isValid());
		} finally {
			Files.deleteIfExists(path);
		}
	}

	@Test
	public void deletedFileDoesNotRemainValid() throws Exception {
		var path = Files.createTempFile("ums-validity-", ".mp4");
		try {
			var item = new RealFile(null, path.toFile());
			assertTrue(item.isValid());
			Files.delete(path);
			assertFalse(item.isValid());
		} finally {
			Files.deleteIfExists(path);
		}
	}

	@Test
	public void directoriesAndSubtitlesAreRejected() throws Exception {
		var directory = Files.createTempDirectory("ums-validity-");
		var subtitle = Files.createTempFile(directory, "subtitle-", ".srt");
		try {
			assertFalse(new RealFile(null, directory.toFile()).isValid());
			assertFalse(new RealFile(null, subtitle.toFile()).isValid());
		} finally {
			Files.deleteIfExists(subtitle);
			Files.deleteIfExists(directory);
		}
	}
}
