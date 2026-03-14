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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class NetworkDeviceFilterTest {

	@Test
	public void testGetDisplayNameUsesAddressLiteralWhenHostnameIsUnknown() throws Exception {
		InetAddress address = InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 25});
		assertEquals("192.0.2.25", NetworkDeviceFilter.getDisplayName(address));
	}

	@Test
	public void testGetDisplayNameUsesEmbeddedHostnameWithoutLookup() throws Exception {
		InetAddress address = InetAddress.getByAddress("living-room-tv", new byte[] {(byte) 192, 0, 2, 26});
		assertEquals("living-room-tv", NetworkDeviceFilter.getDisplayName(address));
	}

	@Test
	public void testGetDisplayNameNormalizesLoopback() {
		assertEquals("localhost", NetworkDeviceFilter.getDisplayName(InetAddress.getLoopbackAddress()));
	}

	@Test
	public void testHostNamePredicateMatchesEmbeddedHostname() throws Exception {
		InetAddress address = InetAddress.getByAddress("living-room-tv", new byte[] {(byte) 192, 0, 2, 27});
		NetworkDeviceFilter.HostNamePredicate predicate = new NetworkDeviceFilter.HostNamePredicate("room");
		NetworkDeviceFilter.HostNamePredicate nonMatchingPredicate = new NetworkDeviceFilter.HostNamePredicate("kitchen");

		assertTrue(predicate.match(address));
		assertFalse(nonMatchingPredicate.match(address));
	}
}
