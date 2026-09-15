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
package net.pms.formats;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.pms.TestHelper;
import net.pms.util.FileUtil;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Opt-in comparison of the original matching order with the production factory. */
class FormatMatchingBenchmark {
	private static volatile long sink;

	@Test
	void compareFormatMatching() throws Exception {
		TestHelper.setLoggingOff();
		List<Format> formats = FormatFactory.getSupportedFormats().stream().map(Format::duplicate).toList();
		for (Format format : formats) {
			Class<?> owner = format.getClass().getMethod("match", String.class).getDeclaringClass();
			assertTrue(owner == Format.class || owner == WEB.class, "Baseline only supports built-in matchers");
		}
		String[][] corpora = {
			{"C:/Music/Track.mp3", "C:/Video/Film.MKV", "C:/Pictures/Photo.jpg", "C:/Media/readme.bogus", "C:/TV/Series.S01E02.ts", "C:/Media/žluťoučký.WAV"},
			{"http://example.com/test.mp3", "https://example.com/video.mkv", "rtmp://example.com/live playpath=test", "http://example.com/test.asf?format=.wmv", "svn+ssh://example.com/test", "https://example.com/"}
		};
		for (int corpus = 0; corpus < corpora.length; corpus++) {
			String[] inputs = corpora[corpus];
			for (String input : inputs) {
				Format old = legacy(formats, input);
				Format current = FormatFactory.getAssociatedFormat(input);
				if (old == null) {
					assertNull(current, input);
				} else {
					assertNotNull(current, input);
					assertEquals(old.getClass(), current.getClass(), input);
					assertEquals(old.getMatchedExtension(), current.getMatchedExtension(), input);
					assertNotSame(current, FormatFactory.getAssociatedFormat(input));
				}
			}
			for (int i = 0; i < 2; i++) {
				measure(formats, inputs, false);
				measure(formats, inputs, true);
			}
			long[] before = new long[5];
			long[] after = new long[5];
			for (int i = 0; i < 5; i++) {
				if (i % 2 == 0) {
					before[i] = measure(formats, inputs, false);
					after[i] = measure(formats, inputs, true);
				} else {
					after[i] = measure(formats, inputs, true);
					before[i] = measure(formats, inputs, false);
				}
			}
			System.out.printf("FORMAT_BENCH corpus=%d operations=3000 before_ns=%s after_ns=%s%n", corpus, Arrays.toString(before), Arrays.toString(after));
			Arrays.sort(before);
			Arrays.sort(after);
			System.out.printf(Locale.ROOT, "FORMAT_BENCH corpus=%d median_before_ms=%.3f median_after_ms=%.3f checksum=%d%n", corpus, before[2] / 1_000_000.0, after[2] / 1_000_000.0, sink);
		}
	}

	private static Format legacy(List<Format> formats, String filename) {
		for (Format format : formats) {
			boolean matched = format instanceof WEB ? format.match(filename) : legacyMatch(format, filename);
			if (matched) {
				return format.duplicate();
			}
		}
		return null;
	}

	private static boolean legacyMatch(Format format, String filename) {
		if (filename == null) {
			return false;
		}
		filename = filename.toLowerCase(Locale.ROOT);
		String[] extensions = format.getSupportedExtensions();
		if (extensions != null) {
			if (FileUtil.getProtocol(filename) != null) {
				return false;
			}
			for (String extension : extensions) {
				String ext = extension.toLowerCase(Locale.ROOT);
				if (filename.endsWith("." + ext)) {
					format.setMatchedExtension(ext);
					return true;
				}
			}
		}
		return false;
	}

	private static long measure(List<Format> formats, String[] inputs, boolean current) {
		long checksum = 0;
		long start = System.nanoTime();
		for (int i = 0; i < 500; i++) {
			for (String input : inputs) {
				Format result = current ? FormatFactory.getAssociatedFormat(input) : legacy(formats, input);
				if (result != null) {
					checksum += result.getMatchedExtension().length();
				}
			}
		}
		long elapsed = System.nanoTime() - start;
		sink = checksum;
		return elapsed;
	}
}
