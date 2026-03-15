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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DnsResolver} (issue 6047: DNS timeouts, no caching).
 */
class DnsResolverTest {

	@Test
	void resolveReverseNullReturnsEmpty() {
		assertEquals("", DnsResolver.resolveReverse(null));
	}

	@Test
	void resolveReverseLoopbackReturnsLocalhost() throws Exception {
		String result = DnsResolver.resolveReverse(InetAddress.getLoopbackAddress());
		assertNotNull(result);
		assertTrue(result.equals("localhost") || result.equals("127.0.0.1"),
				"loopback reverse-DNS should be localhost or 127.0.0.1, got: " + result);
	}

	@Test
	void resolveReverseIpOnlyReturnsAddressOrHostname() throws Exception {
		InetAddress ipOnly = InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 1});
		String result = DnsResolver.resolveReverse(ipOnly);
		assertNotNull(result);
		assertTrue(result.equals("192.0.2.1") || result.length() > 0,
				"reverse-DNS returns IP or hostname; got " + result);
	}

	@Test
	void resolveByNameNullReturnsNull() {
		assertNull(DnsResolver.resolveByName(null));
	}

	@Test
	void resolveByNameEmptyReturnsNull() {
		assertNull(DnsResolver.resolveByName(""));
	}

	@Test
	void resolveByNameLocalhostReturnsAddress() {
		InetAddress addr = DnsResolver.resolveByName("localhost");
		assertNotNull(addr);
		assertTrue(addr.isLoopbackAddress());
	}

	@Test
	void resolveByNameUnknownHostReturnsNull() {
		InetAddress addr = DnsResolver.resolveByName("nonexistent.invalid.domain.6047");
		assertNull(addr);
	}

	@Test
	void resolveAllByNameNullReturnsEmptyArray() {
		InetAddress[] addrs = DnsResolver.resolveAllByName(null);
		assertNotNull(addrs);
		assertEquals(0, addrs.length);
	}

	@Test
	void resolveAllByNameEmptyReturnsEmptyArray() {
		InetAddress[] addrs = DnsResolver.resolveAllByName("");
		assertNotNull(addrs);
		assertEquals(0, addrs.length);
	}

	@Test
	void resolveAllByNameLocalhostReturnsNonEmpty() {
		InetAddress[] addrs = DnsResolver.resolveAllByName("localhost");
		assertNotNull(addrs);
		assertTrue(addrs.length >= 1);
		assertTrue(addrs[0].isLoopbackAddress());
	}

	@Test
	void resolveAllByNameUnknownHostReturnsEmptyArray() {
		InetAddress[] addrs = DnsResolver.resolveAllByName("nonexistent.invalid.domain.6047");
		assertNotNull(addrs);
		assertArrayEquals(new InetAddress[0], addrs);
	}
}
