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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
		assertVersionToStringEquals(v(""), "0");
		assertVersionToStringEquals(v("foo"), "0");
		assertVersionToStringEquals(v("0"), "0");
		assertVersionToStringEquals(v("0.0"), "0.0");
		assertVersionToStringEquals(v("1"), "1");
		assertVersionToStringEquals(v("1.2"), "1.2");
		assertVersionToStringEquals(v("1.2.3"), "1.2.3");
		assertVersionToStringEquals(v("1.2.3.4"), "1.2.3.4");
		assertVersionToStringEquals(v("foo.1"), "0.1");
		assertVersionToStringEquals(v("1.foo"), "1.0");
		assertVersionToStringEquals(v("1.2-foo"), "1.0");
		assertVersionToStringEquals(v("1.foo-2"), "1.0");
		assertVersionToStringEquals(v("foo.42.bar"), "0.42.0");
		assertVersionToStringEquals(v("foo-1.42.1-bar"), "0.42.0");
	}

	@Test
	public void testEquals() {
		assertVersionEquals(v(""), v(""));
		assertVersionEquals(v(""), v("0"));
		assertVersionEquals(v(""), v("00"));
		assertVersionEquals(v(""), v("00.00"));
		assertVersionEquals(v(""), v("0.0.0"));
		assertVersionEquals(v(""), v("00.00.00"));
		assertVersionEquals(v(""), v("0.0.0.0"));
		assertVersionEquals(v(""), v("00.00.00.00"));

		assertVersionEquals(v("foo"), v("0"));
		assertVersionEquals(v("foo"), v("00"));
		assertVersionEquals(v("foo"), v("0.0"));
		assertVersionEquals(v("foo"), v("00.00"));
		assertVersionEquals(v("foo"), v("0.0.0"));
		assertVersionEquals(v("foo"), v("00.00.00"));
		assertVersionEquals(v("foo"), v("0.0.0.0"));
		assertVersionEquals(v("foo"), v("00.00.00.00"));

		assertVersionEquals(v("1foo2"), v("0"));
		assertVersionEquals(v("1foo2"), v("00"));
		assertVersionEquals(v("1foo2"), v("0.0"));
		assertVersionEquals(v("1foo2"), v("00.00"));
		assertVersionEquals(v("1foo2"), v("0.0.0"));
		assertVersionEquals(v("1foo2"), v("00.00.00"));
		assertVersionEquals(v("1foo2"), v("0.0.0.0"));
		assertVersionEquals(v("1foo2"), v("00.00.00.00"));

		assertVersionEquals(v("2"), v("2"));
		assertVersionEquals(v("2"), v("02"));
		assertVersionEquals(v("2"), v("2.0"));
		assertVersionEquals(v("2"), v("02.00"));
		assertVersionEquals(v("2"), v("2.0.0"));
		assertVersionEquals(v("2"), v("02.00.00"));
		assertVersionEquals(v("2"), v("2.0.0.0"));
		assertVersionEquals(v("2"), v("02.00.00.00"));

		assertVersionEquals(v("2.2"), v("2.2"));
		assertVersionEquals(v("2.2"), v("02.02"));
		assertVersionEquals(v("2.2"), v("2.2.0"));
		assertVersionEquals(v("2.2"), v("02.02.00"));
		assertVersionEquals(v("2.2"), v("2.2.0.0"));
		assertVersionEquals(v("2.2"), v("02.02.00.00"));

		assertVersionEquals(v("2.2.2"), v("2.2.2"));
		assertVersionEquals(v("2.2.2"), v("02.02.02"));
		assertVersionEquals(v("2.2.2"), v("2.2.2.0"));
		assertVersionEquals(v("2.2.2"), v("02.02.02.00"));

		assertVersionEquals(v("2.2.2.2"), v("2.2.2.2"));
		assertVersionEquals(v("2.2.2.2"), v("02.02.02.02"));
	}

	@Test
	public void testIsGreaterThan() {
		assertVersionIsGreaterThan(v("2"), v("1"));
		assertVersionIsGreaterThan(v("2"), v("01"));
		assertVersionIsGreaterThan(v("2"), v("1.0"));
		assertVersionIsGreaterThan(v("2"), v("01.00"));
		assertVersionIsGreaterThan(v("2"), v("1.0.0"));
		assertVersionIsGreaterThan(v("2"), v("01.00.00"));
		assertVersionIsGreaterThan(v("2"), v("1.0.0.0"));
		assertVersionIsGreaterThan(v("2"), v("01.00.00.00"));

		assertVersionIsGreaterThan(v("2.2"), v("2"));
		assertVersionIsGreaterThan(v("2.2"), v("02"));
		assertVersionIsGreaterThan(v("2.2"), v("2.0"));
		assertVersionIsGreaterThan(v("2.2"), v("02.00"));
		assertVersionIsGreaterThan(v("2.2"), v("2.0.0"));
		assertVersionIsGreaterThan(v("2.2"), v("02.00.00"));
		assertVersionIsGreaterThan(v("2.2"), v("2.0.0.0"));
		assertVersionIsGreaterThan(v("2.2"), v("02.00.00.00"));

		assertVersionIsGreaterThan(v("2.2.2"), v("2"));
		assertVersionIsGreaterThan(v("2.2.2"), v("02"));
		assertVersionIsGreaterThan(v("2.2.2"), v("2.0"));
		assertVersionIsGreaterThan(v("2.2.2"), v("02.00"));
		assertVersionIsGreaterThan(v("2.2.2"), v("2.0.0"));
		assertVersionIsGreaterThan(v("2.2.2"), v("02.00.00"));
		assertVersionIsGreaterThan(v("2.2.2"), v("2.0.0.0"));
		assertVersionIsGreaterThan(v("2.2.2"), v("02.00.00.00"));

		assertVersionIsGreaterThan(v("2.2.2.2"), v("2"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("02"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("2.0"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("02.00"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("2.0.0"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("02.00.00"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("2.0.0.0"));
		assertVersionIsGreaterThan(v("2.2.2.2"), v("02.00.00.00"));
	}

	@Test
	public void testMajor() {
		assertThat(v("0").getMajor(), is(0));
		assertThat(v("0.1").getMajor(), is(0));
		assertThat(v("0.1.2").getMajor(), is(0));
		assertThat(v("0.1.2.3").getMajor(), is(0));

		assertThat(v("1").getMajor(), is(1));
		assertThat(v("1.2").getMajor(), is(1));
		assertThat(v("1.2.3").getMajor(), is(1));
		assertThat(v("1.2.3.4").getMajor(), is(1));
	}

	@Test
	public void testMinor() {
		assertThat(v("0").getMinor(), is(0));
		assertThat(v("0.1").getMinor(), is(1));
		assertThat(v("0.1.2").getMinor(), is(1));
		assertThat(v("0.1.2.3").getMinor(), is(1));

		assertThat(v("1").getMinor(), is(0));
		assertThat(v("1.2").getMinor(), is(2));
		assertThat(v("1.2.3").getMinor(), is(2));
		assertThat(v("1.2.3.4").getMinor(), is(2));
	}

	@Test
	public void testPatch() {
		assertThat(v("0").getPatch(), is(0));
		assertThat(v("0.1").getPatch(), is(0));
		assertThat(v("0.1.2").getPatch(), is(2));
		assertThat(v("0.1.2.3").getPatch(), is(2));

		assertThat(v("1").getPatch(), is(0));
		assertThat(v("1.2").getPatch(), is(0));
		assertThat(v("1.2.3").getPatch(), is(3));
		assertThat(v("1.2.3.4").getPatch(), is(3));
	}

	@Test
	public void testIsPmsUpdatable() {
		assertIsPmsUpdatable(v("2"), v("2.0.1"));
		assertIsPmsUpdatable(v("2"), v("02.00.01"));
		assertIsPmsUpdatable(v("2"), v("2.0.1.0"));
		assertIsPmsUpdatable(v("2"), v("02.00.01.00"));

		assertIsPmsUpdatable(v("2.2"), v("2.2.1"));
		assertIsPmsUpdatable(v("2.2"), v("02.02.01"));
		assertIsPmsUpdatable(v("2.2"), v("2.2.0.1"));
		assertIsPmsUpdatable(v("2.2"), v("02.02.00.01"));

		assertIsPmsUpdatable(v("2.2.2"), v("2.2.3"));
		assertIsPmsUpdatable(v("2.2.2"), v("02.02.03"));
		assertIsPmsUpdatable(v("2.2.2"), v("2.2.2.1"));
		assertIsPmsUpdatable(v("2.2.2"), v("02.02.02.01"));

		assertIsPmsUpdatable(v("2.2.2.2"), v("2.2.2.3"));
		assertIsPmsUpdatable(v("2.2.2.2"), v("02.02.02.03"));
		assertIsPmsUpdatable(v("2.2.2.2"), v("2.2.3.0"));
		assertIsPmsUpdatable(v("2.2.2.2"), v("02.02.03.00"));
	}

	@Test
	public void testIsNotPmsUpdatable() {
		assertIsNotPmsUpdatable(v("2"), v("2"));
		assertIsNotPmsUpdatable(v("2"), v("02"));
		assertIsNotPmsUpdatable(v("2"), v("20"));
		assertIsNotPmsUpdatable(v("2"), v("020"));
		assertIsNotPmsUpdatable(v("2"), v("2.0"));
		assertIsNotPmsUpdatable(v("2"), v("02.00"));
		assertIsNotPmsUpdatable(v("2"), v("2.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("02.00.00"));
		assertIsNotPmsUpdatable(v("2"), v("2.0.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("02.00.00.00"));

		assertIsNotPmsUpdatable(v("2"), v("2.1"));
		assertIsNotPmsUpdatable(v("2"), v("02.01"));
		assertIsNotPmsUpdatable(v("2"), v("2.1.0"));
		assertIsNotPmsUpdatable(v("2"), v("02.01.00"));
		assertIsNotPmsUpdatable(v("2"), v("2.1.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("02.01.00.00"));

		assertIsNotPmsUpdatable(v("2"), v("1"));
		assertIsNotPmsUpdatable(v("2"), v("01"));
		assertIsNotPmsUpdatable(v("2"), v("1.0"));
		assertIsNotPmsUpdatable(v("2"), v("01.00"));
		assertIsNotPmsUpdatable(v("2"), v("1.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("01.00.00"));
		assertIsNotPmsUpdatable(v("2"), v("1.0.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("01.00.00.00"));

		assertIsNotPmsUpdatable(v("2"), v("3"));
		assertIsNotPmsUpdatable(v("2"), v("03"));
		assertIsNotPmsUpdatable(v("2"), v("3.0"));
		assertIsNotPmsUpdatable(v("2"), v("03.00"));
		assertIsNotPmsUpdatable(v("2"), v("3.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("03.00.00"));
		assertIsNotPmsUpdatable(v("2"), v("3.0.0.0"));
		assertIsNotPmsUpdatable(v("2"), v("03.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2"), v("2.2"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.02"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.2.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.02.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.2.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.02.00.00"));

		assertIsNotPmsUpdatable(v("2.2"), v("1"));
		assertIsNotPmsUpdatable(v("2.2"), v("01"));
		assertIsNotPmsUpdatable(v("2.2"), v("1.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("01.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("1.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("01.00.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("1.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("01.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2"), v("3"));
		assertIsNotPmsUpdatable(v("2.2"), v("03"));
		assertIsNotPmsUpdatable(v("2.2"), v("3.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("03.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("3.2.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("03.00.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("3.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("03.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2"), v("2.1"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.01"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.1.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.01.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.1.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.01.00.00"));

		assertIsNotPmsUpdatable(v("2.2"), v("2.3"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.03"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.3.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.03.00"));
		assertIsNotPmsUpdatable(v("2.2"), v("2.3.0.0"));
		assertIsNotPmsUpdatable(v("2.2"), v("02.03.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2"), v("2.2.2"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.02.02"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("2.2.2.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.02.02.00"));

		assertIsNotPmsUpdatable(v("2.2.2"), v("1"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("01"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("1.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("01.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("1.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("01.00.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("1.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("01.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2"), v("3"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("03"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("3.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("03.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("3.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("03.00.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("3.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("03.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2"), v("2.1.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.01.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("2.1.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.01.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2"), v("2.3.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.03.00"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("2.3.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2"), v("02.03.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.2.2.2"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.02.02.02"));

		assertIsNotPmsUpdatable(v("2.2.2.2"), v("1"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("01"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("1.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("01.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("1.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("01.00.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("1.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("01.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2.2"), v("3"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("03"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("3.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("03.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("3.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("03.00.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("3.0.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("03.00.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.1"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.01"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.1.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.01.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.1.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.01.00.00"));

		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.3"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.03"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.3.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.03.00"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("2.3.0.0"));
		assertIsNotPmsUpdatable(v("2.2.2.2"), v("02.03.00.00"));
	}
}
