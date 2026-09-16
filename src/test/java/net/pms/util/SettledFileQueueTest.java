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
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettledFileQueueTest {
	@TempDir
	Path directory;
	private final ManualScheduler scheduler = new ManualScheduler();
	private final Queue<Runnable> workers = new ArrayDeque<>();
	private final AtomicBoolean locked = new AtomicBoolean();
	private final SettledFileQueue queue = new SettledFileQueue(scheduler, workers::add, f -> locked.get(), 500, 2);

	@AfterEach
	void close() {
		scheduler.shutdownNow();
	}

	@Test
	void waitingFilesDoNotOccupyWorkersAndDuplicatesAreCoalescedThroughParsing() throws Exception {
		File file = Files.createFile(directory.resolve("media")).toFile();
		queue.submit(file, () -> queue.submit(file, () -> fail("Duplicate parser")));
		queue.submit(file, () -> fail("Duplicate event"));
		assertEquals(1, scheduler.tasks.size());
		scheduler.next();
		assertTrue(workers.isEmpty());
		assertEquals(500, scheduler.lastDelay);
		scheduler.next();
		assertEquals(1, workers.size());
		queue.submit(file, () -> fail("Duplicate queued task"));
		workers.remove().run();
		assertTrue(scheduler.tasks.isEmpty());
		queue.submit(file, () -> { });
		assertEquals(1, scheduler.tasks.size(), "File must be accepted after completion");
	}

	@Test
	void changingAndLockedFilesAreRecheckedBeforeParsing() throws Exception {
		Path path = Files.createFile(directory.resolve("media"));
		queue.submit(path.toFile(), () -> { });
		scheduler.next();
		Files.writeString(path, "new data");
		scheduler.next();
		assertTrue(workers.isEmpty());
		locked.set(true);
		scheduler.next();
		assertTrue(workers.isEmpty());
		locked.set(false);
		scheduler.next();
		assertEquals(1, workers.size());
	}

	@Test
	void timedOutAndDeletedFilesCanBeSubmittedAgain() throws Exception {
		Path path = Files.createFile(directory.resolve("media"));
		locked.set(true);
		queue.submit(path.toFile(), () -> fail("Locked file parsed"));
		for (int i = 0; i < 4; i++) {
			scheduler.next();
		}
		assertTrue(workers.isEmpty());
		assertTrue(scheduler.tasks.isEmpty());
		queue.submit(path.toFile(), () -> fail("Deleted file parsed"));
		Files.delete(path);
		scheduler.next();
		Files.createFile(path);
		locked.set(false);
		queue.submit(path.toFile(), () -> { });
		scheduler.next();
		scheduler.next();
		assertEquals(1, workers.size());
	}

	@Test
	void changesWhileQueuedOrParsingAreSettledAgain() throws Exception {
		Path path = Files.createFile(directory.resolve("media"));
		AtomicBoolean parsed = new AtomicBoolean();
		queue.submit(path.toFile(), () -> {
			if (!parsed.getAndSet(true)) {
				try {
					Files.writeString(path, "changed during parsing");
				} catch (java.io.IOException e) {
					throw new java.io.UncheckedIOException(e);
				}
			}
		});
		scheduler.next();
		scheduler.next();
		Files.writeString(path, "changed while queued");
		workers.remove().run();
		assertFalse(parsed.get());
		scheduler.next();
		scheduler.next();
		workers.remove().run();
		assertTrue(parsed.get());
		assertEquals(1, scheduler.tasks.size());
		scheduler.next();
		scheduler.next();
		workers.remove().run();
		assertTrue(scheduler.tasks.isEmpty());
	}

	@Test
	void parserFailureReleasesTheFile() throws Exception {
		File file = Files.createFile(directory.resolve("media")).toFile();
		queue.submit(file, () -> { throw new IllegalStateException("parser failed"); });
		scheduler.next();
		scheduler.next();
		assertThrows(IllegalStateException.class, () -> workers.remove().run());
		queue.submit(file, () -> { });
		assertEquals(1, scheduler.tasks.size());
	}

	@Test
	void rejectedWorkerAndSchedulerReleaseTheFile() throws Exception {
		File file = Files.createFile(directory.resolve("media")).toFile();
		SettledFileQueue rejecting = new SettledFileQueue(scheduler,
				r -> { throw new RejectedExecutionException(); }, f -> false, 500, 2);
		rejecting.submit(file, () -> { });
		scheduler.next();
		assertThrows(RejectedExecutionException.class, scheduler::next);
		rejecting.submit(file, () -> { });
		assertEquals(1, scheduler.tasks.size());
		scheduler.tasks.clear();
		scheduler.reject = true;
		assertThrows(RejectedExecutionException.class, () -> queue.submit(file, () -> { }));
		scheduler.reject = false;
		queue.submit(file, () -> { });
		assertEquals(1, scheduler.tasks.size());
	}

	private static final class ManualScheduler extends ScheduledThreadPoolExecutor {
		private final Queue<Runnable> tasks = new ArrayDeque<>();
		private long lastDelay;
		private boolean reject;

		private ManualScheduler() {
			super(1);
		}

		@Override
		public void execute(Runnable command) {
			if (reject) {
				throw new RejectedExecutionException();
			}
			tasks.add(command);
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
			lastDelay = unit.toMillis(delay);
			execute(command);
			return null;
		}

		private void next() {
			tasks.remove().run();
		}
	}
}
