package net.pms.logging;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class DebounceTraceLoggerTest {
	private static Logger logger(boolean enabled, LinkedBlockingQueue<String> messages) {
		return (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class<?>[]{Logger.class}, (proxy, method, args) -> {
			if (method.getName().equals("isTraceEnabled")) {
				return enabled;
			}
			if (method.getName().equals("trace")) {
				messages.add((String) args[0]);
			}
			return null;
		});
	}

	@Test
	public void disabledTraceSchedulesNothing() {
		var executor = new ScheduledThreadPoolExecutor(1);
		try {
			var messages = new LinkedBlockingQueue<String>();
			var debounce = new DebounceTraceLogger(logger(false, messages), executor, 10);
			for (int i = 0; i < 1000; i++) {
				debounce.log("ignored");
			}
			assertEquals(0, executor.getTaskCount());
			assertEquals(0, executor.getPoolSize());
			assertTrue(messages.isEmpty());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void burstEmitsLastMessageAndLoggerCanBeReused() throws Exception {
		var executor = new ScheduledThreadPoolExecutor(1);
		executor.setRemoveOnCancelPolicy(true);
		var release = new java.util.concurrent.CountDownLatch(1);
		var started = new java.util.concurrent.CountDownLatch(1);
		try {
			executor.execute(() -> {
				started.countDown();
				try {
					release.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(started.await(5, TimeUnit.SECONDS));
			var messages = new LinkedBlockingQueue<String>();
			var debounce = new DebounceTraceLogger(logger(true, messages), executor, 20);
			for (int i = 0; i < 100; i++) {
				debounce.log("message-" + i);
			}
			assertEquals(1, executor.getQueue().size());
			release.countDown();
			assertEquals("message-99", messages.poll(5, TimeUnit.SECONDS));
			debounce.log("next burst");
			assertEquals("next burst", messages.poll(5, TimeUnit.SECONDS));
			assertTrue(messages.isEmpty());
		} finally {
			release.countDown();
			executor.shutdownNow();
		}
	}
}
