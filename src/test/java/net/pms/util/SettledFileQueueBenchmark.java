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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Opt-in synthetic benchmark: run with -Dtest=SettledFileQueueBenchmark. */
class SettledFileQueueBenchmark {
	@TempDir
	Path directory;

	@Test
	void compareStableFileBatches() throws Exception {
		List<File> files = new ArrayList<>();
		for (int i = 0; i < 64; i++) {
			files.add(Files.writeString(directory.resolve("media-" + i), "stable").toFile());
		}
		measure(files.subList(0, 8), false);
		measure(files.subList(0, 8), true);
		long[] baseline = new long[3];
		long[] current = new long[3];
		for (int i = 0; i < 3; i++) {
			// Alternate ordering to reduce order bias.
			if (i % 2 == 0) {
				baseline[i] = measure(files, false);
				current[i] = measure(files, true);
			} else {
				current[i] = measure(files, true);
				baseline[i] = measure(files, false);
			}
		}
		System.out.println("SCANNER_BENCH baseline_ms=" + Arrays.toString(baseline));
		System.out.println("SCANNER_BENCH current_ms=" + Arrays.toString(current));
		Arrays.sort(baseline);
		Arrays.sort(current);
		System.out.printf(Locale.ROOT, "SCANNER_BENCH median_baseline_ms=%d median_current_ms=%d ratio=%.2f%n",
				baseline[1], current[1], (double) baseline[1] / current[1]);
	}

	private long measure(List<File> files, boolean deferred) throws Exception {
		var workers = Executors.newFixedThreadPool(4);
		var scheduler = Executors.newScheduledThreadPool(2);
		CountDownLatch completed = new CountDownLatch(files.size());
		// Lock contention and real media parsing are deliberately excluded.
		SettledFileQueue queue = new SettledFileQueue(scheduler, workers, f -> false, 500, 120);
		Runnable parse = () -> {
			try {
				Thread.sleep(10); // Same simulated parser cost in both variants.
				completed.countDown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		long start = System.nanoTime();
		try {
			for (File file : files) {
				if (deferred) {
					queue.submit(file, parse);
				} else {
					// Reproduce the previous worker-blocking settlement for stable files.
					workers.execute(() -> {
						try {
							long size = file.length();
							Thread.sleep(500);
							if (file.exists() && file.length() == size) {
								parse.run();
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});
				}
			}
			assertTrue(completed.await(30, TimeUnit.SECONDS), "All files must finish");
			return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
		} finally {
			workers.shutdown();
			assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
			scheduler.shutdownNow();
			assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
		}
	}
}
