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
package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.parsers.MediaInfoParser;
import net.pms.parsers.ParserTest;
import org.junit.jupiter.api.io.TempDir;

class MediaParseRetryTest {
	@BeforeAll
	static void configure() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@TempDir
	Path directory;

	@Test
	void containerOnlyMemoryCacheIsReparsedByMediaInfo() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(MediaInfoParser.isValid());
		var path = directory.resolve("cached.mp4");
		Files.copy(ParserTest.getTestFile("video-h264-aac.mp4").toPath(), path);
		var cached = new MediaInfo();
		cached.setContainer("mp4");
		cached.setMediaParser("MediaInfo");
		MediaInfoStore.storeMediaInfo(path.toString(), cached);
		var result = MediaInfoStore.getMediaInfo(path.toString(), path.toFile(),
			FormatFactory.getAssociatedFormat(path.toString()), Format.VIDEO);
		assertNotNull(result.getDefaultVideoTrack());
		assertTrue(result.getWidth() > 0);
		assertEquals(MediaInfoParser.PARSER_NAME, result.getMediaParser());
	}
	@Test
	void unchangedFileIsRetriedOnlyAfterCooldown() throws Exception {
		var now = new AtomicLong();
		var policy = new MediaParseRetry(now::get);
		var file = Files.writeString(directory.resolve("broken.wtv"), "broken").toFile();
		assertTrue(policy.acquire(file));
		assertFalse(policy.acquire(file));
		now.set(TimeUnit.MINUTES.toNanos(5));
		assertTrue(policy.acquire(file));
		assertFalse(policy.acquire(file));
	}

	@Test
	void changedFileCanBeRetriedImmediately() throws Exception {
		var policy = new MediaParseRetry(() -> 0);
		var path = Files.writeString(directory.resolve("recording.wtv"), "partial");
		assertTrue(policy.acquire(path.toFile()));
		Files.writeString(path, "more complete recording");
		assertTrue(policy.acquire(path.toFile()));
		assertFalse(policy.acquire(path.toFile()));
	}
}