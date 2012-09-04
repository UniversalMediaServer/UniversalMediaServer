/*
 * PS3 Media Server, for streaming media to your PS3.
 * Copyright (C) 2012 chocolateboy
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
package net.pms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class VersionTest {
	private final Version v(String version) {
		return new Version(version);
	}

	private void assertVersionIsGreaterThan(Version v1, Version v2) {
		assertTrue(v1.isGreaterThan(v2));
		assertTrue(v1.isGreaterThanOrEqualTo(v2));
		assertTrue(v2.isLessThan(v1));
		assertTrue(v2.isLessThanOrEqualTo(v1));

		assertFalse(v1.isLessThan(v2));
		assertFalse(v1.isLessThanOrEqualTo(v2));
		assertFalse(v2.isGreaterThan(v1));
		assertFalse(v2.isGreaterThanOrEqualTo(v1));

		assertFalse(v1.equals(v2));
		assertFalse(v2.equals(v1));
	}

	private void assertVersionEquals(Version v1, Version v2) {
		assertTrue(v1.equals(v2));
		assertTrue(v2.equals(v1));

		assertTrue(v1.isGreaterThanOrEqualTo(v2));
		assertTrue(v2.isGreaterThanOrEqualTo(v1));

		assertTrue(v1.isLessThanOrEqualTo(v2));
		assertTrue(v2.isLessThanOrEqualTo(v1));

		assertFalse(v1.isGreaterThan(v2));
		assertFalse(v2.isGreaterThan(v1));

		assertFalse(v1.isLessThan(v2));
		assertFalse(v2.isLessThan(v1));
	}

	private void assertIsPmsUpdatable(Version v1, Version v2) {
		assertVersionIsGreaterThan(v2, v1);
		assertTrue(Version.isPmsUpdatable(v1, v2));
		assertFalse(Version.isPmsUpdatable(v2, v1));
	}

	private void assertIsNotPmsUpdatable(Version v1, Version v2) {
		assertFalse(Version.isPmsUpdatable(v1, v2));
		assertFalse(Version.isPmsUpdatable(v2, v1));
	}

	private void assertVersionToStringEquals(Version v, String s) {
		assertEquals(v.toString(), s);
	}

	@Test
	public void testToString() {

	}

	@Test
	public void testEquals() {

	}

	@Test
	public void testIsGreaterThan() {

	}

	@Test
	public void testMajor() {

	}

	@Test
	public void testMinor() {

	}

	@Test
	public void testPatch() {

	}

	@Test
	public void testIsPmsUpdatable() {

	}

	@Test
	public void testIsNotPmsUpdatable() {

	}
}
