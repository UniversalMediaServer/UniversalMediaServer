/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011  G.Zsombor
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.test;

import ch.qos.logback.classic.LoggerContext;
import java.util.concurrent.TimeUnit;
import net.pms.util.TaskRunner;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TaskRunnerTest {
	@Before
	public void setUp() {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
	}

	class Counter {
		int x;

		synchronized void incr() {
			x++;
		}
	}

	@Test
	public void simpleScheduledTasks() throws InterruptedException {
		TaskRunner tk = new TaskRunner();

		final Counter c = new Counter();

		for (int i = 0; i < 3; i++) {
			tk.submitNamed("myTask", new Runnable() {
				@Override
				public void run() {
					c.incr();
				}
			});
		}
		tk.shutdown();
		tk.awaitTermination(1, TimeUnit.DAYS);
		assertEquals("all 3 task is executed", 3, c.x);
	}

	@Test
	public void singletonTasks() throws InterruptedException {
		TaskRunner tk = new TaskRunner();

		final Counter c = new Counter();

		for (int i = 0; i < 5; i++) {
			tk.submitNamed("myTask", true, new Runnable() {
				@Override
				public void run() {
					sleep();
					c.incr();
				}
			});
		}
		tk.shutdown();
		tk.awaitTermination(1, TimeUnit.DAYS);
		assertEquals("only one task is executed", 1, c.x);
	}

	protected void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
