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
package net.pms.test;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import net.pms.util.Version;

import org.junit.Before;

public class VersionTest {
	private final Version v(String version) {
		return new Version(version);
	}

	private void assertVersionIsGreaterThan(Version v1, Version v2) {
		assertTrue(v1.isGreaterThan(v2));
	}

	private void assertVersionIsNotGreaterThan(Version v1, Version v2) {
		assertFalse(v1.isGreaterThan(v2));
	}

	private void assertIsPmsCompatible(Version v1, Version v2) {
		assertTrue(v1.isPmsCompatible(v2)); 
	}

	private void assertIsNotPmsCompatible(Version v1, Version v2) {
		assertFalse(v1.isPmsCompatible(v2)); 
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
	public void testIsGreaterThan() {
		assertVersionIsGreaterThan(v("2.3.4"), v("1.3.4")); // major
		assertVersionIsGreaterThan(v("2.3.4"), v("2.2.4")); // minor
		assertVersionIsGreaterThan(v("2.3.4"), v("2.3.3")); // patch

		assertVersionIsGreaterThan(v("2.3.4"), v("01.3.4")); // padded major
		assertVersionIsGreaterThan(v("2.3.4"), v("2.02.4")); // padded minor
		assertVersionIsGreaterThan(v("2.3.4"), v("2.3.03")); // padded patch

		assertVersionIsGreaterThan(v("2.3.4"), v("1.3.4.99")); // major with extra component
		assertVersionIsGreaterThan(v("2.3.4"), v("2.2.4.99")); // minor with extra component
		assertVersionIsGreaterThan(v("2.3.4"), v("2.3.3.99")); // patch with extra component

		assertVersionIsGreaterThan(v("2.3.4"), v("1")); // 1 component
		assertVersionIsGreaterThan(v("2.3.4"), v("2.2")); // 2 components

		assertVersionIsGreaterThan(v("2.3.4"), v("0")); // sanity check zero: 1 component
		assertVersionIsGreaterThan(v("2.3.4"), v("0.0")); // sanity check zero: 2 components
		assertVersionIsGreaterThan(v("2.3.4"), v("0.0.0")); // sanity check zero: 3 components
		assertVersionIsGreaterThan(v("2.3.4"), v("0.0.0.0")); // sanity check zero: 4 components
	}

	@Test
	public void testIsNotGreaterThan() {
		assertVersionIsNotGreaterThan(v("2.3.4"), v("2.3.4")); // self
		assertVersionIsNotGreaterThan(v("2.3.4"), v("2.3.4.0")); // self with extra component
		assertVersionIsNotGreaterThan(v("2.3.4"), v("02.3.4.0")); // self with padded major
		assertVersionIsNotGreaterThan(v("2.3.4"), v("2.03.4.0")); // self with padded minor
		assertVersionIsNotGreaterThan(v("2.3.4"), v("2.3.04")); // self with padded patch

		assertVersionIsNotGreaterThan(v("1.3.4"), v("2.3.4")); // major
		assertVersionIsNotGreaterThan(v("2.2.4"), v("2.3.4")); // minor
		assertVersionIsNotGreaterThan(v("2.3.3"), v("2.3.4")); // patch

		assertVersionIsNotGreaterThan(v("01.3.4"), v("2.3.4")); // padded major
		assertVersionIsNotGreaterThan(v("2.02.4"), v("2.3.4")); // padded minor
		assertVersionIsNotGreaterThan(v("2.3.03"), v("2.3.4")); // padded patch

		assertVersionIsNotGreaterThan(v("1.3.4.99"), v("2.3.4")); // major with extra component
		assertVersionIsNotGreaterThan(v("2.2.4.99"), v("2.3.4")); // minor with extra component
		assertVersionIsNotGreaterThan(v("2.3.3.99"), v("2.3.4")); // patch with extra component

		assertVersionIsNotGreaterThan(v("1"), v("2.3.4")); // 1 component
		assertVersionIsNotGreaterThan(v("2.2"), v("2.3.4")); // 2 components

		assertVersionIsNotGreaterThan(v("0"), v("2.3.4")); // sanity check zero: 1 component
		assertVersionIsNotGreaterThan(v("0.0"), v("2.3.4")); // sanity check zero: 2 components
		assertVersionIsNotGreaterThan(v("0.0.0"), v("2.3.4")); // sanity check zero: 3 components
		assertVersionIsNotGreaterThan(v("0.0.0.0"), v("2.3.4")); // sanity check zero: 4 components
	}

	@Test
	public void testMajor() {
		assertThat(v("").getMajor(), is(0)); // default when no major is defined
		assertThat(v("foo").getMajor(), is(0)); // ditto

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
		assertThat(v("").getMinor(), is(0)); // default when no minor is defined
		assertThat(v("foo").getMinor(), is(0)); // ditto

		assertThat(v("0").getMinor(), is(0)); // default when no minor is defined
		assertThat(v("0.1").getMinor(), is(1));
		assertThat(v("0.1.2").getMinor(), is(1));
		assertThat(v("0.1.2.3").getMinor(), is(1));

		assertThat(v("1").getMinor(), is(0)); // default when no minor is defined
		assertThat(v("1.2").getMinor(), is(2));
		assertThat(v("1.2.3").getMinor(), is(2));
		assertThat(v("1.2.3.4").getMinor(), is(2));
	}

	@Test
	public void testPatch() {
		assertThat(v("").getPatch(), is(0)); // default when no patch is defined
		assertThat(v("foo").getPatch(), is(0)); // ditto

		assertThat(v("0").getPatch(), is(0)); // default when no patch is defined
		assertThat(v("0.1").getPatch(), is(0)); // ditto
		assertThat(v("0.1.2").getPatch(), is(2));
		assertThat(v("0.1.2.3").getPatch(), is(2));

		assertThat(v("1").getPatch(), is(0)); // default when no patch is defined
		assertThat(v("1.2").getPatch(), is(0)); // ditto
		assertThat(v("1.2.3").getPatch(), is(3)); 
		assertThat(v("1.2.3.4").getPatch(), is(3));
	}

	@Test
	public void testIsPmsCompatible() {
		// current -> latest
		assertIsPmsCompatible(v(""), v("")); // defaults to 0
		assertIsPmsCompatible(v(""), v("0"));
		assertIsPmsCompatible(v(""), v("0.0"));
		assertIsPmsCompatible(v(""), v("0.0.0"));
		assertIsPmsCompatible(v(""), v("0.0.0.0"));
		assertIsPmsCompatible(v(""), v("0.0.1.0"));
		assertIsPmsCompatible(v(""), v("0.0.0.1"));
		assertIsPmsCompatible(v(""), v("0.0.1.1"));

		assertIsPmsCompatible(v("0"), v("0"));
		assertIsPmsCompatible(v("0"), v(""));
		assertIsPmsCompatible(v("0"), v("foo"));
		assertIsPmsCompatible(v("0"), v("0.0"));
		assertIsPmsCompatible(v("0"), v("0.0.0"));
		assertIsPmsCompatible(v("0"), v("0.0.0.0"));
		assertIsPmsCompatible(v("0"), v("0.0.0.1"));
		assertIsPmsCompatible(v("0"), v("0.0.1.0"));
		assertIsPmsCompatible(v("0"), v("0.0.1.1"));
		assertIsPmsCompatible(v("0"), v("0.0.99.99"));

		assertIsPmsCompatible(v("foo"), v("foo")); // defaults to 0
		assertIsPmsCompatible(v("foo"), v(""));
		assertIsPmsCompatible(v("foo"), v("0"));
		assertIsPmsCompatible(v("foo"), v("0.0"));
		assertIsPmsCompatible(v("foo"), v("0.0.0"));
		assertIsPmsCompatible(v("foo"), v("0.0.0.0"));
		assertIsPmsCompatible(v("foo"), v("0.0.0.1"));
		assertIsPmsCompatible(v("foo"), v("0.0.1.0"));
		assertIsPmsCompatible(v("foo"), v("0.0.1.1"));
		assertIsPmsCompatible(v("foo"), v("0.0.99.99"));

		assertIsPmsCompatible(v("1"), v("1"));
		assertIsPmsCompatible(v("1"), v("1.0"));
		assertIsPmsCompatible(v("1"), v("1.0.0"));
		assertIsPmsCompatible(v("1"), v("1.0.0.0"));
		assertIsPmsCompatible(v("1"), v("1.0.0.1"));
		assertIsPmsCompatible(v("1"), v("1.0.1.0"));
		assertIsPmsCompatible(v("1"), v("1.0.1.1"));
		assertIsPmsCompatible(v("1"), v("1.0.99.99"));

		assertIsPmsCompatible(v("1.2"), v("1.2"));
		assertIsPmsCompatible(v("1.2"), v("1.2.0"));
		assertIsPmsCompatible(v("1.2"), v("1.2.0.0"));
		assertIsPmsCompatible(v("1.2"), v("1.2.0.1"));
		assertIsPmsCompatible(v("1.2"), v("1.2.1.0"));
		assertIsPmsCompatible(v("1.2"), v("1.2.1.1"));
		assertIsPmsCompatible(v("1.2"), v("1.2.99.99"));

		assertIsPmsCompatible(v("1.2.3"), v("1.2.3"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.0"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.0.0"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.0.1"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.1.0"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.1.1"));
		assertIsPmsCompatible(v("1.2.3"), v("1.2.99.99"));

		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.3.4"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.0"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.0.0"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.0.1"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.1.0"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.1.1"));
		assertIsPmsCompatible(v("1.2.3.4"), v("1.2.99.99"));
	}

	@Test
	public void testIsNotPmsCompatible() {
		// current -> latest
		assertIsNotPmsCompatible(v(""), v("1"));
		assertIsNotPmsCompatible(v(""), v("1.0"));
		assertIsNotPmsCompatible(v(""), v("1.0.0"));
		assertIsNotPmsCompatible(v(""), v("1.0.0.0"));
		assertIsNotPmsCompatible(v(""), v("1.0.0.1"));
		assertIsNotPmsCompatible(v(""), v("1.0.1.0"));
		assertIsNotPmsCompatible(v(""), v("1.0.1.1"));
		assertIsNotPmsCompatible(v(""), v("1.0.99.99"));
		assertIsNotPmsCompatible(v(""), v("0.2"));
		assertIsNotPmsCompatible(v(""), v("0.2.0"));
		assertIsNotPmsCompatible(v(""), v("0.2.0.0"));
		assertIsNotPmsCompatible(v(""), v("0.2.0.1"));
		assertIsNotPmsCompatible(v(""), v("0.2.1.0"));
		assertIsNotPmsCompatible(v(""), v("0.2.1.1"));
		assertIsNotPmsCompatible(v(""), v("0.2.99.99"));

		assertIsNotPmsCompatible(v("0"), v("1"));
		assertIsNotPmsCompatible(v("0"), v("1.0"));
		assertIsNotPmsCompatible(v("0"), v("1.0.0"));
		assertIsNotPmsCompatible(v("0"), v("1.0.0.0"));
		assertIsNotPmsCompatible(v("0"), v("1.0.0.1"));
		assertIsNotPmsCompatible(v("0"), v("1.0.1.0"));
		assertIsNotPmsCompatible(v("0"), v("1.0.1.1"));
		assertIsNotPmsCompatible(v("0"), v("1.0.99.99"));
		assertIsNotPmsCompatible(v("0"), v("0.2"));
		assertIsNotPmsCompatible(v("0"), v("0.2.0"));
		assertIsNotPmsCompatible(v("0"), v("0.2.0.0"));
		assertIsNotPmsCompatible(v("0"), v("0.2.0.1"));
		assertIsNotPmsCompatible(v("0"), v("0.2.1.0"));
		assertIsNotPmsCompatible(v("0"), v("0.2.1.1"));
		assertIsNotPmsCompatible(v("0"), v("0.2.99.99"));

		assertIsNotPmsCompatible(v("foo"), v("1"));
		assertIsNotPmsCompatible(v("foo"), v("1.0"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.0"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.0.0"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.0.1"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.1.0"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.1.1"));
		assertIsNotPmsCompatible(v("foo"), v("1.0.99.99"));
		assertIsNotPmsCompatible(v("foo"), v("0.2"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.0"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.0.0"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.0.1"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.1.0"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.1.1"));
		assertIsNotPmsCompatible(v("foo"), v("0.2.99.99"));

		assertIsNotPmsCompatible(v("1"), v("0"));
		assertIsNotPmsCompatible(v("1"), v("2"));
		assertIsNotPmsCompatible(v("1"), v("0.0"));
		assertIsNotPmsCompatible(v("1"), v("0.0.0"));
		assertIsNotPmsCompatible(v("1"), v("0.0.0.0"));
		assertIsNotPmsCompatible(v("1"), v("0.0.0.1"));
		assertIsNotPmsCompatible(v("1"), v("0.0.1.0"));
		assertIsNotPmsCompatible(v("1"), v("0.0.1.1"));
		assertIsNotPmsCompatible(v("1"), v("0.0.99.99"));
		assertIsNotPmsCompatible(v("1"), v("1.1"));
		assertIsNotPmsCompatible(v("1"), v("1.1.0"));
		assertIsNotPmsCompatible(v("1"), v("1.1.0.0"));
		assertIsNotPmsCompatible(v("1"), v("1.1.0.1"));
		assertIsNotPmsCompatible(v("1"), v("1.1.1.0"));
		assertIsNotPmsCompatible(v("1"), v("1.1.1.1"));
		assertIsNotPmsCompatible(v("1"), v("1.1.99.99"));

		assertIsNotPmsCompatible(v("1.2"), v("0"));
		assertIsNotPmsCompatible(v("1.2"), v("1"));
		assertIsNotPmsCompatible(v("1.2"), v("2"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.0"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.0.0"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.0.1"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.1.0"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.1.1"));
		assertIsNotPmsCompatible(v("1.2"), v("0.2.99.99"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.0.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.0.1"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.1.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.1.1"));
		assertIsNotPmsCompatible(v("1.2"), v("1.1.99.99"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.0.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.0.1"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.1.0"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.1.1"));
		assertIsNotPmsCompatible(v("1.2"), v("1.3.99.99"));

		assertIsNotPmsCompatible(v("1.2.3"), v("0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("2"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.0.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.0.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.1.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.1.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("0.2.99.99"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.0.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.0.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.1.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.1.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.1.99.99"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.0.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.0.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.1.0"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.1.1"));
		assertIsNotPmsCompatible(v("1.2.3"), v("1.3.99.99"));
	}
}
