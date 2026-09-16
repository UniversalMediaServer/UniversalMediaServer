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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Opt-in comparison benchmark: -Dtest=LibraryListBenchmark. */
class LibraryListBenchmark {
	@Test
	void compareLibrarySnapshots() {
		List<String> snapshot = new ArrayList<>();
		for (int i = 0; i < 100000; i++) {
			snapshot.add("C:/Media/Library/Video-" + i + ".mkv");
		}
		Collections.shuffle(snapshot, new Random(42));
		for (boolean reordered : List.of(false, true)) {
			List<String> queryOrder = new ArrayList<>(snapshot);
			if (reordered) {
				Collections.shuffle(queryOrder, new Random(19));
			}
			measure(snapshot, queryOrder, false);
			measure(snapshot, queryOrder, true);
			long[] before = new long[3];
			long[] after = new long[3];
			for (int i = 0; i < 3; i++) {
				if (i % 2 == 0) {
					before[i] = measure(snapshot, queryOrder, false);
					after[i] = measure(snapshot, queryOrder, true);
				} else {
					after[i] = measure(snapshot, queryOrder, true);
					before[i] = measure(snapshot, queryOrder, false);
				}
			}
			System.out.println("LIST_BENCH reordered=" + reordered + " before_us=" + Arrays.toString(before) + " after_us=" + Arrays.toString(after));
			Arrays.sort(before);
			Arrays.sort(after);
			System.out.println("LIST_BENCH reordered=" + reordered + " median_before_us=" + before[1] + " median_after_us=" + after[1]);
		}
	}

	private long measure(List<String> source, List<String> queryOrder, boolean current) {
		List<String> snapshot = new ArrayList<>(source);
		long elapsed = 0;
		for (int i = 0; i < 5; i++) {
			// Model a fresh query result. Exclude construction and SQL execution.
			List<String> query = new ArrayList<>(queryOrder.size());
			for (String value : queryOrder) {
				query.add(new String(value));
			}
			long start = System.nanoTime();
			boolean equal;
			if (current) {
				equal = UMSUtils.isListsEqual(snapshot, query);
			} else {
				Collections.sort(snapshot);
				Collections.sort(query);
				equal = snapshot.equals(query);
			}
			elapsed += System.nanoTime() - start;
			assertTrue(equal);
		}
		return TimeUnit.NANOSECONDS.toMicros(elapsed / 5);
	}
}
