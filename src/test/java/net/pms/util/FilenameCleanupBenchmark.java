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
package net.pms.util;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Opt-in microbenchmark: -Dtest=FilenameCleanupBenchmark. */
class FilenameCleanupBenchmark {
	private static String sensitiveExpression;
	private static String insensitiveExpression;
	private static volatile long sink;

	@Test
	void compareTrailingMetadataCleanup() throws Throwable {
		sensitiveExpression = expression("COMMON_FILE_ENDS_CASE_SENSITIVE");
		insensitiveExpression = "(?i)" + expression("COMMON_FILE_ENDS");
		var signature = MethodType.methodType(String.class, String.class);
		MethodHandle before = MethodHandles.lookup().findStatic(FilenameCleanupBenchmark.class, "legacy", signature);
		MethodHandle after = MethodHandles.privateLookupIn(FileUtil.class, MethodHandles.lookup())
			.findStatic(FileUtil.class, "removeFilenameEndMetadata", signature);
		List<String> inputs = new ArrayList<>();
		try (var stream = FileUtilTest.class.getResourceAsStream("prettified_filenames_metadata.json")) {
			assertNotNull(stream);
			try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				for (var entry : JsonParser.parseReader(reader).getAsJsonArray()) {
					// Feed normalized separators to the suffix-cleanup stage.
					inputs.add(entry.getAsJsonObject().get("filename").getAsString().replace('.', ' ').replace('_', ' '));
				}
			}
		}
		int changed = 0;
		for (String input : inputs) {
			String expected = (String) before.invokeExact(input);
			assertEquals(expected, (String) after.invokeExact(input), input);
			if (!expected.equals(input)) {
				changed++;
			}
		}
		assertTrue(changed > 0);
		assertTrue(changed < inputs.size());
		for (int i = 0; i < 2; i++) {
			measure(before, inputs);
			measure(after, inputs);
		}
		long[] oldTimes = new long[5];
		long[] newTimes = new long[5];
		for (int i = 0; i < oldTimes.length; i++) {
			if (i % 2 == 0) {
				oldTimes[i] = measure(before, inputs);
				newTimes[i] = measure(after, inputs);
			} else {
				newTimes[i] = measure(after, inputs);
				oldTimes[i] = measure(before, inputs);
			}
		}
		System.out.printf("FILENAME_BENCH fixtures=%d changed=%d operations=%d before_ns=%s after_ns=%s%n",
			inputs.size(), changed, inputs.size() * 100, Arrays.toString(oldTimes), Arrays.toString(newTimes));
		Arrays.sort(oldTimes);
		Arrays.sort(newTimes);
		System.out.printf("FILENAME_BENCH median_before_ms=%.3f median_after_ms=%.3f checksum=%d%n",
			oldTimes[2] / 1_000_000.0, newTimes[2] / 1_000_000.0, sink);
	}

	private static String expression(String name) throws Exception {
		var field = FileUtil.class.getDeclaredField(name);
		field.setAccessible(true);
		return (String) field.get(null);
	}

	private static String legacy(String input) {
		return input.replaceAll(sensitiveExpression, "").replaceAll(insensitiveExpression, "");
	}

	private static long measure(MethodHandle function, List<String> inputs) throws Throwable {
		long checksum = 0;
		long start = System.nanoTime();
		for (int i = 0; i < 100; i++) {
			for (String input : inputs) {
				String result = (String) function.invokeExact(input);
				checksum += result.length();
			}
		}
		long elapsed = System.nanoTime() - start;
		sink = checksum;
		return elapsed;
	}
}
