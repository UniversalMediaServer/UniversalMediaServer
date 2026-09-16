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
package net.pms.store.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.store.StoreResource;
import net.pms.store.utils.StoreResourceSorterTest.CountingFile;
import net.pms.store.utils.StoreResourceSorterTest.Resource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Opt-in: -Dtest=StoreResourceSorterBenchmark. No timing pass/fail threshold. */
class StoreResourceSorterBenchmark {
	@TempDir
	Path directory;

	@Test
	void compareSortWork() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		UmsConfiguration configuration = new UmsConfiguration(false);
		PMS.setConfiguration(configuration);
		configuration.setIgnoreTheWordAandThe(true);
		configuration.setSortMethod(StoreResourceSorter.SORT_DATE_MOD_ASC);
		List<Resource> titles = new ArrayList<>();
		for (int i = 0; i < 5000; i++) {
			titles.add(new Resource("The Épisode  " + i + " — Ｓｅｒｉｅｓ", null));
		}
		Collections.shuffle(titles, new Random(73));
		compare(titles, false);
		List<Resource> files = new ArrayList<>();
		for (int i = 0; i < 512; i++) {
			Path path = Files.createFile(directory.resolve("file-" + i));
			Files.setLastModifiedTime(path, FileTime.fromMillis(1700000000000L + i * 1000));
			files.add(new Resource("file-" + i, new CountingFile(path)));
		}
		Collections.shuffle(files, new Random(83));
		compare(files, true);
	}

	private void compare(List<Resource> input, boolean date) {
		Sample warmBefore = measure(input, date, false);
		Sample warmAfter = measure(input, date, true);
		assertEquals(warmBefore.result(), warmAfter.result());
		long[] before = new long[3];
		long[] after = new long[3];
		for (int i = 0; i < 3; i++) {
			Sample oldSample;
			Sample newSample;
			if (i % 2 == 0) {
				oldSample = measure(input, date, false);
				newSample = measure(input, date, true);
			} else {
				newSample = measure(input, date, true);
				oldSample = measure(input, date, false);
			}
			assertEquals(oldSample.result(), newSample.result());
			assertEquals(input.size(), newSample.reads());
			before[i] = oldSample.micros();
			after[i] = newSample.micros();
		}
		String kind = date ? "date" : "title";
		System.out.println("SORT_BENCH kind=" + kind + " before_us=" + Arrays.toString(before) + " after_us=" + Arrays.toString(after));
		Arrays.sort(before);
		Arrays.sort(after);
		System.out.println("SORT_BENCH kind=" + kind + " median_before_us=" + before[1] + " median_after_us=" + after[1] +
				" before_reads=" + warmBefore.reads() + " after_reads=" + warmAfter.reads());
	}

	private Sample measure(List<Resource> input, boolean date, boolean current) {
		List<StoreResource> resources = new ArrayList<>(input);
		for (Resource resource : input) {
			resource.nameReads = 0;
			if (date) {
				((CountingFile) resource.file).reads = 0;
			}
		}
		long start = System.nanoTime();
		if (current) {
			if (date) {
				StoreResourceSorter.sortResourcesByDefault(resources);
			} else {
				StoreResourceSorter.sortResourcesByTitle(resources);
			}
		} else {
			resources.sort((a, b) -> {
				if (date) {
					try {
						return Long.compare(
								Files.getLastModifiedTime(((Resource) a).file.toPath()).toMillis(),
								Files.getLastModifiedTime(((Resource) b).file.toPath()).toMillis());
					} catch (java.io.IOException e) {
						throw new java.io.UncheckedIOException(e);
					}
				}
				return StoreResourceSorterTest.legacyTitleCompare(a.getLocalizedDisplayName(null), b.getLocalizedDisplayName(null),
						PMS.getConfiguration().isIgnoreTheWordAandThe());
			});
		}
		long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
		long reads = input.stream().mapToLong(r -> date ? ((CountingFile) r.file).reads : r.nameReads).sum();
		return new Sample(micros, reads, resources);
	}

	private record Sample(long micros, long reads, List<StoreResource> result) { }
}
