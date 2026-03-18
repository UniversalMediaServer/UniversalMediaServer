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
package net.pms.network;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpeedStatsRaceConditionTest {

	private SpeedStats speedStats;

	@BeforeEach
	void setUp() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		UmsConfiguration configuration = new UmsConfiguration(false);
		// Bound reverse-DNS used for cache-key normalization (issue 6047).
		configuration.setDnsResolutionTimeoutEnabled(true);
		configuration.setDnsResolutionTimeoutMs(200);
		PMS.setConfiguration(configuration);
		// Ensure no cross-test pollution in the static cache.
		// Note: SpeedStats cache is internal by design; for tests we use the fact that the
		// in-flight future is cached immediately under the IP key, and use distinct loopback
		// addresses per test to avoid collisions.

		// Inject a fast measurement strategy so this test doesn't spend ~15s pinging.
		speedStats = new SpeedStats((addr, rendererName) -> 42);
	}

	@Test
	void getCachedSpeedReturnsInFlightFutureForIpKey() throws Exception {
		// Use a loopback address with an embedded hostname so reverse-DNS is fast and deterministic.
		InetAddress addr = InetAddress.getByAddress("ums-speedstats-ip-keyed-host", new byte[] {127, 0, 0, 10});

		CompletableFuture<Integer> inFlight = speedStats.calculateSpeedInMBits(addr, "JUnit");
		CompletableFuture<Integer> cached = speedStats.getCachedSpeedInMBits(addr);

		assertNotNull(cached, "getCachedSpeedInMBits should return a non-null future even during in-flight measurement");
		assertSame(inFlight, cached, "Expected in-flight future to be cached immediately under the IP key (avoids race window)");

		// Ensure background measurement completes so non-daemon executor threads don't keep the JVM alive.
		Integer result = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			try {
				return inFlight.get(3, TimeUnit.SECONDS);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		assertNotNull(result);
		assertTrue(result == -1 || result >= 0, "Speed should be -1 or a non-negative Mb/s value");
	}

	@Test
	void hostnameKeyEventuallyPointsAtSameInFlightFuture() throws Exception {
		// Simulate a "host-less" InetAddress by using:
		// 1) one with an embedded hostname (avoid reverse lookup)
		// 2) one where the "hostname" is just the IP literal, so reverse-DNS may be invoked during a cache miss
		//
		// We must trigger the reverse-DNS path during a cache-miss, so we call getCachedSpeedInMBits()
		// *before* starting any measurement.
		InetAddress embeddedHostnameAddr = InetAddress.getByAddress(
			"ums-speedstats-hostname-keyed-host", new byte[] {127, 0, 0, 20}
		);
		InetAddress hostLessAddr = InetAddress.getByAddress(
			"127.0.0.20", new byte[] {127, 0, 0, 20}
		);

		// Cache miss: should resolve reverse-DNS in bounded time and then return -1 (no cached speed yet).
		Integer missValue = assertTimeoutPreemptively(
			Duration.ofSeconds(2),
			() -> speedStats.getCachedSpeedInMBits(hostLessAddr).get(1, TimeUnit.SECONDS)
		);
		assertEquals(-1, missValue, "Cache miss for host-less address should return -1");

		// After measurement starts for the embedded-hostname address, IP cache should now be filled.
		CompletableFuture<Integer> inFlight = speedStats.calculateSpeedInMBits(embeddedHostnameAddr, "JUnit");
		CompletableFuture<Integer> cached = speedStats.getCachedSpeedInMBits(hostLessAddr);
		assertSame(inFlight, cached, "After measurement starts, host-less address should map to the same in-flight future via the IP cache");
	}
}
