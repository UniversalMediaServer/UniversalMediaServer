package net.pms.external.musicbrainz.api;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class MusicBrainzBackoffTest {
	@Test
	public void defaultPauseExpiresWithoutSleeping() {
		var clock = new AtomicLong(-123456);
		var backoff = new MusicBrainzUtil.ServiceBackoff(clock::get, () -> 0);
		assertFalse(backoff.isPaused());
		assertEquals(60, backoff.pause(null));
		assertTrue(backoff.isPaused());
		clock.addAndGet(TimeUnit.SECONDS.toNanos(59));
		assertTrue(backoff.isPaused());
		clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
		assertFalse(backoff.isPaused());
	}

	@Test
	public void retryAfterSupportsSecondsDatesAndInvalidInput() {
		for (String header : new String[]{null, "bad", "-1", "0", "999999999999999999999999"}) {
			assertEquals(60, new MusicBrainzUtil.ServiceBackoff(() -> 0, () -> 0).pause(header));
		}
		assertEquals(120, new MusicBrainzUtil.ServiceBackoff(() -> 0, () -> 0).pause(" 120 "));
		assertEquals(120, new MusicBrainzUtil.ServiceBackoff(() -> 0, () -> 0).pause("Thu, 1 Jan 1970 00:02:00 GMT"));
		assertEquals(86400, new MusicBrainzUtil.ServiceBackoff(() -> 0, () -> 0).pause(Long.toString(Long.MAX_VALUE)));
	}

	@Test
	public void laterFailuresDoNotShortenPauseAndClockWrapIsSafe() {
		var clock = new AtomicLong(Long.MAX_VALUE - 10);
		var backoff = new MusicBrainzUtil.ServiceBackoff(clock::get, () -> 0);
		backoff.pause("120");
		clock.addAndGet(TimeUnit.SECONDS.toNanos(10));
		assertEquals(110, backoff.pause(null));
		assertTrue(backoff.isPaused());
		clock.addAndGet(TimeUnit.SECONDS.toNanos(110));
		assertFalse(backoff.isPaused());
	}
}
