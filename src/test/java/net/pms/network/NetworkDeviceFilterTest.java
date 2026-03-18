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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NetworkDeviceFilterTest {

	@BeforeEach
	void setUp() throws Exception {
		TestHelper.setLoggingOff();
		PMS.get();
		UmsConfiguration configuration = new UmsConfiguration(false);
		// Keep reverse-DNS bounded so `isAllowed(...)` can't hang in logs (issue 6047).
		configuration.setDnsResolutionTimeoutEnabled(true);
		configuration.setDnsResolutionTimeoutMs(200);
		PMS.setConfiguration(configuration);
		NetworkDeviceFilter.reset();
	}

	private static InetAddress addr(int a, int b, int c, int d) throws Exception {
		return InetAddress.getByAddress(new byte[] {(byte) a, (byte) b, (byte) c, (byte) d});
	}

	@Test
	public void testHostNamePredicateMatchesEmbeddedHostname() throws Exception {
		InetAddress address = InetAddress.getByAddress("living-room-tv", new byte[] {(byte) 192, 0, 2, 27});
		NetworkDeviceFilter.HostNamePredicate predicate = new NetworkDeviceFilter.HostNamePredicate("room");
		NetworkDeviceFilter.HostNamePredicate nonMatchingPredicate = new NetworkDeviceFilter.HostNamePredicate("kitchen");

		assertTrue(predicate.match(address));
		assertFalse(nonMatchingPredicate.match(address));
	}

	@Test
	public void testHostNamePredicateWithIpOnlyAddressCompletesWithinTimeout() throws Exception {
		InetAddress ipOnly = InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 1});
		NetworkDeviceFilter.HostNamePredicate predicate = new NetworkDeviceFilter.HostNamePredicate("192");
		assertTimeoutPreemptively(
				java.time.Duration.ofMillis(5000),
				() -> predicate.match(ipOnly),
				"HostNamePredicate.match() must not block indefinitely on reverse-DNS (issue 6047)"
		);
	}

	@Test
	public void testLoopbackIsAlwaysAllowed() throws Exception {
		NetworkDeviceFilter.setBlockedByDefault(true);
		assertTrue(NetworkDeviceFilter.isAllowed(InetAddress.getLoopbackAddress()));
		NetworkDeviceFilter.setBlockedByDefault(false);
		assertTrue(NetworkDeviceFilter.isAllowed(InetAddress.getLoopbackAddress()));
	}

	@Test
	public void testIpPredicateAllowlist() throws Exception {
		NetworkDeviceFilter.setBlockedByDefault(true); // deny by default
		NetworkDeviceFilter.setAllowed("192.0.2.25", true); // allow specific IP
		assertTrue(NetworkDeviceFilter.isAllowed(addr(192, 0, 2, 25)));
		assertFalse(NetworkDeviceFilter.isAllowed(addr(192, 0, 2, 26)));
	}

	@Test
	public void testIpPredicateDenylist() throws Exception {
		NetworkDeviceFilter.setBlockedByDefault(false); // allow by default
		NetworkDeviceFilter.setAllowed("192.0.2.25", false); // deny specific IP
		assertFalse(NetworkDeviceFilter.isAllowed(addr(192, 0, 2, 25)));
		assertTrue(NetworkDeviceFilter.isAllowed(addr(192, 0, 2, 26)));
	}

	@Test
	public void testIpPredicateRangeAndWildcardMatch() throws Exception {
		NetworkDeviceFilter.setBlockedByDefault(true); // deny by default
		NetworkDeviceFilter.setAllowed("192.0.2-5.*", true);
		assertTrue(NetworkDeviceFilter.isAllowed(addr(192, 0, 4, 10)));
		assertFalse(NetworkDeviceFilter.isAllowed(addr(192, 0, 6, 10)));
	}

	@Test
	public void testHostNamePredicateUsedByFilterCaseInsensitive() throws Exception {
		NetworkDeviceFilter.setBlockedByDefault(false); // allow by default
		NetworkDeviceFilter.setAllowed("ROOM", false); // deny hostnames containing "room"
		InetAddress matching = InetAddress.getByAddress("living-room-tv", new byte[] {(byte) 192, 0, 2, 27});
		InetAddress nonMatching = InetAddress.getByAddress("kitchen-tv", new byte[] {(byte) 192, 0, 2, 28});
		assertNotNull(matching.getHostName());
		assertNotNull(nonMatching.getHostName());
		assertFalse(NetworkDeviceFilter.isAllowed(matching));
		assertTrue(NetworkDeviceFilter.isAllowed(nonMatching));
	}

}
