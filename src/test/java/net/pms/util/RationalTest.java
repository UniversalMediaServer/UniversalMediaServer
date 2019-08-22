/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RationalTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testStaticInstances() {
		assertTrue(Rational.ZERO.equals(BigInteger.ZERO));
		assertEquals(BigInteger.ZERO, Rational.ZERO.numerator);
		assertEquals(BigInteger.ONE, Rational.ZERO.denominator);
		assertEquals(BigInteger.ONE, Rational.ZERO.greatestCommonDivisor);
		assertEquals(BigInteger.ZERO, Rational.ZERO.reducedNumerator);
		assertEquals(BigInteger.ONE, Rational.ZERO.reducedDenominator);
		assertEquals("0", Rational.ZERO.toUnreducedString());
		assertEquals("0", Rational.ZERO.toString());
		assertEquals("0", Rational.ZERO.toDecimalString());
		assertEquals("0", Rational.ZERO.toUnreducedHexString());
		assertEquals("0", Rational.ZERO.toHexString());
		assertEquals("1:1", Rational.ZERO.toAspectRatio());
		assertEquals(Rational.ZERO, Rational.ZERO);

		assertTrue(Rational.ONE.equals(BigInteger.ONE));
		assertEquals(BigInteger.ONE, Rational.ONE.numerator);
		assertEquals(BigInteger.ONE, Rational.ONE.denominator);
		assertEquals(BigInteger.ONE, Rational.ONE.greatestCommonDivisor);
		assertEquals(BigInteger.ONE, Rational.ONE.reducedNumerator);
		assertEquals(BigInteger.ONE, Rational.ONE.reducedDenominator);
		assertEquals("1", Rational.ONE.toUnreducedString());
		assertEquals("1", Rational.ONE.toString());
		assertEquals("1", Rational.ONE.toDecimalString());
		assertEquals("1", Rational.ONE.toUnreducedHexString());
		assertEquals("1", Rational.ONE.toHexString());
		assertEquals("1:1", Rational.ONE.toAspectRatio());
		assertEquals(Rational.ONE, Rational.ONE);

		assertTrue(Rational.POSITIVE_INFINITY.equals(Double.POSITIVE_INFINITY));
		assertTrue(Rational.POSITIVE_INFINITY.equals(Float.POSITIVE_INFINITY));
		assertEquals(BigInteger.ONE, Rational.POSITIVE_INFINITY.numerator);
		assertEquals(BigInteger.ZERO, Rational.POSITIVE_INFINITY.denominator);
		assertEquals(BigInteger.ZERO, Rational.POSITIVE_INFINITY.greatestCommonDivisor);
		assertEquals(BigInteger.ONE, Rational.POSITIVE_INFINITY.reducedNumerator);
		assertEquals(BigInteger.ZERO, Rational.POSITIVE_INFINITY.reducedDenominator);
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toUnreducedString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toDecimalString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toUnreducedHexString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toHexString());
		assertEquals("1:1", Rational.POSITIVE_INFINITY.toAspectRatio());
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY);

		assertTrue(Rational.NEGATIVE_INFINITY.equals(Double.NEGATIVE_INFINITY));
		assertTrue(Rational.NEGATIVE_INFINITY.equals(Float.NEGATIVE_INFINITY));
		assertEquals(BigInteger.ONE.negate(), Rational.NEGATIVE_INFINITY.numerator);
		assertEquals(BigInteger.ZERO, Rational.NEGATIVE_INFINITY.denominator);
		assertEquals(BigInteger.ZERO, Rational.NEGATIVE_INFINITY.greatestCommonDivisor);
		assertEquals(BigInteger.ONE.negate(), Rational.NEGATIVE_INFINITY.reducedNumerator);
		assertEquals(BigInteger.ZERO, Rational.NEGATIVE_INFINITY.reducedDenominator);
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toUnreducedString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toDecimalString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toUnreducedHexString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toHexString());
		assertEquals("1:1", Rational.NEGATIVE_INFINITY.toAspectRatio());
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY);

		assertTrue(Rational.NaN.equals(Double.NaN));
		assertTrue(Rational.NaN.equals(Float.NaN));
		assertEquals(BigInteger.ZERO, Rational.NaN.numerator);
		assertEquals(BigInteger.ZERO, Rational.NaN.denominator);
		assertEquals(BigInteger.ZERO, Rational.NaN.greatestCommonDivisor);
		assertEquals(BigInteger.ZERO, Rational.NaN.reducedNumerator);
		assertEquals(BigInteger.ZERO, Rational.NaN.reducedDenominator);
		assertEquals("NaN", Rational.NaN.toUnreducedString());
		assertEquals("NaN", Rational.NaN.toString());
		assertEquals("NaN", Rational.NaN.toDecimalString());
		assertEquals("NaN", Rational.NaN.toUnreducedHexString());
		assertEquals("NaN", Rational.NaN.toHexString());
		assertEquals("1:1", Rational.NaN.toAspectRatio());
		assertEquals(Rational.NaN, Rational.NaN);
	}

	@Test
	public void testConstructor() {
		Rational rational = new Rational(
			BigInteger.valueOf(1),
			BigInteger.valueOf(2),
			BigInteger.valueOf(3),
			BigInteger.valueOf(4),
			BigInteger.valueOf(5)
		);

		assertTrue(rational.equals(BigDecimal.valueOf(0.8)));
		assertEquals(BigInteger.ONE, rational.numerator);
		assertEquals(BigInteger.valueOf(2), rational.denominator);
		assertEquals(BigInteger.valueOf(3), rational.greatestCommonDivisor);
		assertEquals(BigInteger.valueOf(4), rational.reducedNumerator);
		assertEquals(BigInteger.valueOf(5), rational.reducedDenominator);
		assertEquals("1/2", rational.toUnreducedString());
		assertEquals("4/5", rational.toString());
		assertEquals("0.8", rational.toDecimalString());
		assertEquals("1/2", rational.toUnreducedHexString());
		assertEquals("4/5", rational.toHexString());
		assertEquals("4:5", rational.toAspectRatio());

		Rational rational2 = new Rational(
			BigInteger.valueOf(20),
			BigInteger.valueOf(21),
			BigInteger.valueOf(22),
			BigInteger.valueOf(23),
			BigInteger.valueOf(25)
		);

		assertTrue(rational2.equals(BigDecimal.valueOf(0.92)));
		assertEquals(BigInteger.valueOf(20), rational2.numerator);
		assertEquals(BigInteger.valueOf(21), rational2.denominator);
		assertEquals(BigInteger.valueOf(22), rational2.greatestCommonDivisor);
		assertEquals(BigInteger.valueOf(23), rational2.reducedNumerator);
		assertEquals(BigInteger.valueOf(25), rational2.reducedDenominator);
		assertEquals("20/21", rational2.toUnreducedString());
		assertEquals("23/25", rational2.toString());
		assertEquals("0.92", rational2.toDecimalString());
		assertEquals("14/15", rational2.toUnreducedHexString());
		assertEquals("17/19", rational2.toHexString());
		assertEquals("23:25", rational2.toAspectRatio());

		assertNotEquals(rational, rational2);
		assertEquals(0, rational.compareTo(rational));
		assertEquals(0, rational2.compareTo(rational2));
		assertEquals(-1, rational.compareTo(rational2));
		assertEquals(1, rational2.compareTo(rational));
	}

	@Test
	public void testFloatFactory() {
		assertEquals(0.342f, Rational.valueOf(0.342f).floatValue(), 0.0);
		assertEquals(10000f, Rational.valueOf(10000f).floatValue(), 0.0);
		assertEquals(1f/5, Rational.valueOf(1f/5).floatValue(), 0.0);
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.valueOf(Float.NaN));
	}

	@Test
	public void testDoubleFactory() {
		assertEquals(0.342, Rational.valueOf(0.342).doubleValue(), 0.0);
		assertEquals(10000d, Rational.valueOf(10000d).doubleValue(), 0.0);
		assertEquals(1d/3, Rational.valueOf(1d/3).doubleValue(), 0.0);
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.valueOf(Double.NaN));
	}

	@Test
	public void testBigDecimalFactory() {
		assertEquals(0, Rational.valueOf(BigDecimal.ZERO).intValue());
		assertEquals(Rational.ZERO, Rational.valueOf(BigDecimal.ZERO));
		assertEquals(BigDecimal.ONE, Rational.valueOf(BigDecimal.ONE).bigDecimalValue());
		assertTrue(Rational.valueOf(BigDecimal.ONE).equals(Rational.ONE));
		assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), Rational.valueOf(BigDecimal.valueOf(Long.MAX_VALUE)).bigDecimalValue());
		assertEquals(BigDecimal.valueOf(Long.MIN_VALUE), Rational.valueOf(BigDecimal.valueOf(Long.MIN_VALUE)).bigDecimalValue());
		assertEquals(
			new BigDecimal("398427545234132213.4398023478543978313289043509400000000000000000000000000000000000000000000000000000000000000000000000"),
			Rational.valueOf(new BigDecimal("398427545234132213.43980234785439783132890435094")).bigDecimalValue()
		);
		assertEquals(
			0,
			new BigDecimal("398427545234132213.43980234785439783132890435094")
				.compareTo(Rational.valueOf(new BigDecimal("398427545234132213.43980234785439783132890435094")).bigDecimalValue())
		);
		assertEquals("86358858831081/250000", Rational.valueOf(new BigDecimal("345435435.324324")).toUnreducedString());
		assertEquals("86358858831081/250000", Rational.valueOf(new BigDecimal("345435435.324324")).toString());
		assertEquals("4e8afd1a94e9/3d090", Rational.valueOf(new BigDecimal("345435435.324324")).toUnreducedHexString());
		assertEquals("4e8afd1a94e9/3d090", Rational.valueOf(new BigDecimal("345435435.324324")).toHexString());
		assertEquals("116753208/125", Rational.valueOf(new BigDecimal("934025.6640")).toUnreducedString());
		assertEquals("116753208/125", Rational.valueOf(new BigDecimal("934025.6640")).toString());
		assertEquals("6f58338/7d", Rational.valueOf(new BigDecimal("934025.6640")).toUnreducedHexString());
		assertEquals("6f58338/7d", Rational.valueOf(new BigDecimal("934025.6640")).toHexString());
		assertEquals("16:9", Rational.valueOf(new BigDecimal("1.7777777777")).toAspectRatio());
	}

	@Test
	public void testStringFactory() {
		assertNull(Rational.valueOf(""));
		assertNull(Rational.valueOf((String) null));
		assertNull(Rational.valueOf("   "));
		assertNull(Rational.valueOf(":-16.153"));
		assertNull(Rational.valueOf("2:"));
		assertNull(Rational.valueOf("  :"));
		assertNull(Rational.valueOf("/  "));
		assertNull(Rational.valueOf("/  : "));
		assertEquals(4, Rational.valueOf("16 /4").intValue());
		assertEquals(5, Rational.valueOf("20/4 ").intValue());
		assertEquals(6d, Rational.valueOf("  24 / 4").doubleValue(), 0.0);
		assertEquals(-6d, Rational.valueOf("-24/4").doubleValue(), 0.0);
		assertEquals(-6d, Rational.valueOf("24/ -4").doubleValue(), 0.0);
		assertEquals(6d, Rational.valueOf("-24/-4").doubleValue(), 0.0);
		assertEquals(0.5625, Rational.valueOf("9/16").doubleValue(), 0.0);
		assertEquals(-2194d, Rational.valueOf("-2194").doubleValue(), 0.0);
		assertEquals(4, Rational.valueOf("16 :4").intValue());
		assertEquals(5, Rational.valueOf("20:4 ").intValue());
		assertEquals(6d, Rational.valueOf("  24 : 4").doubleValue(), 0.0);
		assertEquals(-6d, Rational.valueOf("-24:4").doubleValue(), 0.0);
		assertEquals(-6d, Rational.valueOf("24: -4").doubleValue(), 0.0);
		assertEquals(6d, Rational.valueOf("-24:-4").doubleValue(), 0.0);
		assertEquals(0.5625, Rational.valueOf("9:16").doubleValue(), 0.0);
		assertEquals(0.5875, Rational.valueOf("9.4:16").doubleValue(), 0.0);
		assertEquals(0.5819352442270787, Rational.valueOf("9.4: 16.153").doubleValue(), 0.0);
		assertEquals(2.971584225840401, Rational.valueOf("48 :16.153").doubleValue(), 0.0);
		assertEquals(-2.971584225840401, Rational.valueOf("48: -16.153").doubleValue(), 0.0);
		assertEquals(2.971584225840401, Rational.valueOf("  -48 : -16.153").doubleValue(), 0.0);
		assertEquals(0.1987246951030768, Rational.valueOf("-3.21/-16.153").doubleValue(), 0.0);
		assertNull(Rational.valueOf("/-16.153"));
		assertNull(Rational.valueOf("23:"));
		assertEquals(23, Rational.valueOf("23").doubleValue(), 0.0);
		assertEquals(1.0, Rational.valueOf("23:23").doubleValue(), 0.0);
		assertEquals(0.5, Rational.valueOf("11.5:23").doubleValue(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, Rational.valueOf("20:0").doubleValue(), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, Rational.valueOf(" -3/ 0").doubleValue(), 0.0);
		assertEquals(Double.NaN, Rational.valueOf(" -0/ 0").doubleValue(), 0.0);
		assertEquals(12.48, Rational.valueOf("12.48").doubleValue(), 0.0);
		assertEquals(1936122.48, Rational.valueOf("1936122.48").doubleValue(), 0.0);
		assertEquals(1936122.48, Rational.valueOf("1,936,122.48", Locale.US).doubleValue(), 0.0);
		assertEquals(-1936122.48, Rational.valueOf("-1,936,122.48", NumberFormat.getInstance(Locale.US)).doubleValue(), 0.0);
		assertEquals(1936122.48, Rational.valueOf("1.936.122,48", Locale.GERMANY).doubleValue(), 0.0);
		assertEquals(-1936122.48, Rational.valueOf("-1.936.122,48", NumberFormat.getInstance(Locale.GERMANY)).doubleValue(), 0.0);
		assertEquals(602.3090620625292, Rational.valueOf("-1,936,122.48/-3,214.5", Locale.US).doubleValue(), 0.0);
		assertEquals(-602.3090620625292, Rational.valueOf("1,936,122.48 :-3,214.5", NumberFormat.getInstance(Locale.US)).doubleValue(), 0.0);
		assertEquals(602.3090620625292, Rational.valueOf("1.936.122,48: 3.214,5", Locale.GERMANY).doubleValue(), 0.0);
		assertEquals(-602.3090620625292, Rational.valueOf("-1.936.122,48 / 3.214,5", NumberFormat.getInstance(Locale.GERMANY)).doubleValue(), 0.0);
		exception.expect(NumberFormatException.class);
		assertNull(Rational.valueOf(" - 3/ 0"));
	}

	@Test
	public void testIntFactory() {
		assertEquals(Rational.ZERO, Rational.valueOf(0));
		assertEquals(-4096, Rational.valueOf(-4096).intValue());
		assertEquals(Integer.MAX_VALUE, Rational.valueOf(Integer.MAX_VALUE).intValue());
		assertEquals(Integer.MIN_VALUE, Rational.valueOf(Integer.MIN_VALUE).intValue());
	}

	@Test
	public void testLongFactory() {
		assertEquals(Rational.ZERO, Rational.valueOf(0L));
		assertEquals(-4096, Rational.valueOf(-4096L).intValue());
		assertEquals(Long.MAX_VALUE, Rational.valueOf(Long.MAX_VALUE).longValue());
		assertEquals(Long.MIN_VALUE, Rational.valueOf(Long.MIN_VALUE).longValue());
		assertEquals(BigInteger.ONE, Rational.valueOf(Long.MIN_VALUE).greatestCommonDivisor);
		assertEquals(BigInteger.ONE, Rational.valueOf(Long.MIN_VALUE).denominator);
		assertEquals(BigInteger.ONE, Rational.valueOf(Long.MIN_VALUE).reducedDenominator);
	}

	@Test
	public void testBigIntegerFactory() {
		assertEquals(Rational.ZERO, Rational.valueOf(BigInteger.valueOf(0)));
		assertEquals("987933205489029348556903", Rational.valueOf(new BigInteger("987933205489029348556903")).toUnreducedString());
		assertEquals(Long.MAX_VALUE, Rational.valueOf(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
		assertEquals(Long.MIN_VALUE, Rational.valueOf(BigInteger.valueOf(Long.MIN_VALUE)).longValue());
		assertEquals(BigInteger.ONE, Rational.valueOf(BigInteger.valueOf(Long.MIN_VALUE)).greatestCommonDivisor);
		assertEquals(BigInteger.ONE, Rational.valueOf(BigInteger.valueOf(Long.MIN_VALUE)).denominator);
		assertEquals(BigInteger.ONE, Rational.valueOf(BigInteger.valueOf(Long.MIN_VALUE)).reducedDenominator);
		assertNull(Rational.valueOf((BigInteger) null));
	}

	@Test
	public void testTwoIntsFactory() {
		assertEquals(Rational.ZERO, Rational.valueOf(0, 30));
		assertEquals(-1, Rational.valueOf(-1, 30).signum());
		assertEquals(-1, Rational.valueOf(1, -30).signum());
		assertEquals(1, Rational.valueOf(-4, -30).signum());
		assertEquals(Rational.ONE, Rational.valueOf(Integer.MAX_VALUE, Integer.MAX_VALUE));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(Integer.MAX_VALUE, -Integer.MAX_VALUE));
		assertEquals("18/32", Rational.valueOf(18, 32).toUnreducedString());
		assertEquals("9/16", Rational.valueOf(18, 32).toString());
		assertEquals(0.5625, Rational.valueOf(18, 32).doubleValue(), 0.0);
		assertTrue(Rational.valueOf(0, 0).isNaN());
		assertEquals(Rational.NaN, Rational.valueOf(0, 0));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(10, 0));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-20, 0));
	}

	@Test
	public void testTwoLongsFactory() {
		assertEquals(Rational.ZERO, Rational.valueOf(0L, 30L));
		assertEquals(-1, Rational.valueOf(-1L, 30L).signum());
		assertEquals(-1, Rational.valueOf(1L, -30L).signum());
		assertEquals(1, Rational.valueOf(-1L, -30L).signum());
		assertEquals(Rational.ONE, Rational.valueOf(Long.MAX_VALUE, Long.MAX_VALUE));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(Long.MAX_VALUE, -Long.MAX_VALUE));
		assertEquals("36/64", Rational.valueOf(36L, 64L).toUnreducedString());
		assertEquals("9/16", Rational.valueOf(36L, 64L).toString());
		assertEquals(0.5625, Rational.valueOf(36L, 64L).doubleValue(), 0.0);
		assertTrue(Rational.valueOf(0L, 0L).isNaN());
		assertEquals(Rational.NaN, Rational.valueOf(0L, 0L));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(10L, 0L));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-20L, 0L));
	}

	@Test
	public void testTwoBigIntegersFactory() {
		assertNull(Rational.valueOf(null, BigInteger.TEN));
		assertNull(Rational.valueOf(BigInteger.ZERO, null));
		assertNull(Rational.valueOf((BigInteger) null, null));
		assertEquals(-1, Rational.valueOf(BigInteger.ONE.negate(), BigInteger.valueOf(30)).signum());
		assertEquals(-1, Rational.valueOf(BigInteger.ONE, BigInteger.valueOf(-30)).signum());
		assertEquals(1, Rational.valueOf(BigInteger.ONE.negate(), BigInteger.valueOf(-30)).signum());
		assertEquals(Rational.ONE, Rational.valueOf(
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)),
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE))
		));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)),
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)).negate()
		));
		assertEquals(
			"42391158275216203514294433201/75362059155939917358745659024",
			Rational.valueOf(
				new BigInteger("42391158275216203514294433201"),
				new BigInteger("75362059155939917358745659024")
			).toUnreducedString()
		);
		assertEquals("9/16", Rational.valueOf(
			new BigInteger("42391158275216203514294433201"), new BigInteger("75362059155939917358745659024")
		).toString());
		assertEquals(0.5625, Rational.valueOf(
			new BigInteger("42391158275216203514294433201"), new BigInteger("75362059155939917358745659024")
		).doubleValue(), 0.0);
		assertEquals(Rational.ZERO, Rational.valueOf(BigInteger.ZERO, BigInteger.TEN));
		assertEquals(Rational.ONE, Rational.valueOf(BigInteger.TEN, BigInteger.TEN));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(BigInteger.TEN, BigInteger.TEN.negate()));
		assertEquals(Rational.NaN, Rational.valueOf(BigInteger.ZERO, BigInteger.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(BigInteger.valueOf(100), BigInteger.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(BigInteger.valueOf(33).negate(), BigInteger.ZERO));
	}

	@Test
	public void testTwoBigDecimalsFactory() {
		assertNull(Rational.valueOf(null, BigDecimal.TEN));
		assertNull(Rational.valueOf(BigDecimal.ZERO, null));
		assertNull(Rational.valueOf((BigDecimal) null, null));
		assertEquals(-1, Rational.valueOf(BigDecimal.ONE.negate(), BigDecimal.valueOf(30.45)).signum());
		assertEquals(-1, Rational.valueOf(BigDecimal.ONE, BigDecimal.valueOf(-29.893)).signum());
		assertEquals(1, Rational.valueOf(BigDecimal.ONE.negate(), BigDecimal.valueOf(-30.99)).signum());
		assertEquals(Rational.ONE, Rational.valueOf(
			BigDecimal.valueOf(Long.MAX_VALUE).multiply(BigDecimal.valueOf(Math.PI)),
			BigDecimal.valueOf(Long.MAX_VALUE).multiply(BigDecimal.valueOf(Math.PI))
		));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(
			BigDecimal.valueOf(Long.MIN_VALUE).multiply(BigDecimal.valueOf(Math.PI)),
			BigDecimal.valueOf(Long.MIN_VALUE).multiply(BigDecimal.valueOf(Math.PI)).negate()
		));
		assertEquals(
			"423911582752162035142944332010000/75362059155939917358745659024",
			Rational.valueOf(
				new BigDecimal("4239115827521.6203514294433201"),
				new BigDecimal("753620591.55939917358745659024")
			).toUnreducedString()
		);
		assertEquals("9/16", Rational.valueOf(
			new BigDecimal("423911582752162035142.94433201"), new BigDecimal("753620591559399173587.45659024")
		).toString());
		assertEquals(0.5625, Rational.valueOf(
			new BigDecimal("4239115.8275216203514294433201"), new BigDecimal("7536205.9155939917358745659024")
		).doubleValue(), 0.0);
		assertEquals(Rational.ZERO, Rational.valueOf(BigDecimal.ZERO, BigDecimal.TEN));
		assertEquals(Rational.ONE, Rational.valueOf(BigDecimal.TEN, BigDecimal.TEN));
		assertEquals(Rational.ONE.negate(), Rational.valueOf(BigDecimal.TEN, BigDecimal.TEN.negate()));
		assertEquals(Rational.NaN, Rational.valueOf(BigDecimal.ZERO, BigDecimal.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(BigDecimal.valueOf(0.00001), BigDecimal.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(BigDecimal.valueOf(0.00001).negate(), BigDecimal.ZERO));
	}

	@Test
	public void testMultiplyOperator() {
		assertNull(Rational.ONE.multiply((BigInteger) null));
		assertEquals(Rational.NaN, Rational.NaN.multiply(6));
		assertEquals(Rational.valueOf(30), Rational.valueOf(5).multiply(6));
		assertEquals(Rational.valueOf(60), Rational.valueOf(5).multiply(12L));
		assertEquals(Rational.valueOf(90), Rational.valueOf(5).multiply(BigInteger.valueOf(18)));
		assertEquals(Rational.valueOf("15/2"), Rational.valueOf(5f).multiply(1.5f));
		assertEquals(Rational.valueOf("15/2"), Rational.valueOf(5d).multiply(1.5));
		assertEquals(Rational.valueOf("627989997/125000000"), Rational.valueOf(5).multiply(new BigDecimal("1.00478399520")));
		assertEquals(Rational.valueOf("180/37"), Rational.valueOf("24/37").multiply(Rational.valueOf(15, 2)));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.multiply(BigInteger.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(BigInteger.TEN));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(BigInteger.TEN.negate()));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.multiply(BigInteger.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(BigInteger.TEN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(BigInteger.TEN.negate()));
		assertEquals(Rational.ZERO, Rational.valueOf(4.97).multiply(BigInteger.ZERO));
		assertEquals(Rational.valueOf(4.97), Rational.valueOf(4.97).multiply(BigInteger.ONE));
		assertEquals(Rational.valueOf(4.97).negate(), Rational.valueOf(4.97).multiply(BigInteger.ONE.negate()));

		assertEquals(Rational.NaN, Rational.NaN.multiply(6f));
		assertEquals(Rational.NaN, Rational.ONE.multiply(Float.NaN));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.multiply(0f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(3f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(-2f));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Float.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, 2).multiply(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, -3).multiply(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.multiply(0f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(7.5f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(-5f));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, 2).multiply(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, -3).multiply(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(4.97).multiply(0f));

		assertEquals(Rational.NaN, Rational.NaN.multiply(6d));
		assertEquals(Rational.NaN, Rational.ONE.multiply(Double.NaN));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.multiply(0d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(3d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(-2d));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Double.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, 2).multiply(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, -3).multiply(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.multiply(0d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(7.5d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(-5d));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, 2).multiply(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, -3).multiply(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(4.97).multiply(0d));

		assertNull(Rational.ONE.multiply((BigDecimal) null));
		assertNull(Rational.ONE.multiply((Rational) null));
		assertEquals(Rational.NaN, Rational.NaN.multiply(Rational.ONE));
		assertEquals(Rational.NaN, Rational.ONE.multiply(Rational.NaN));
		assertEquals(Rational.NaN, Rational.NaN.multiply(Rational.NaN));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.multiply(Rational.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(Rational.valueOf(15, 2)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(Rational.valueOf(15, -3)));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, 2).multiply(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, -3).multiply(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.multiply(Rational.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(Rational.valueOf(15, 2)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(Rational.valueOf(15, -3)));
		assertEquals(Rational.NaN, Rational.ZERO.multiply(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(15, 2).multiply(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(15, -3).multiply(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(4.97).multiply(Rational.ZERO));
		assertEquals(Rational.valueOf(4.97), Rational.valueOf(4.97).multiply(Rational.ONE));
		assertEquals(Rational.valueOf(4.97).negate(), Rational.valueOf(4.97).multiply(Rational.ONE.negate()));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.multiply(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.multiply(Rational.NEGATIVE_INFINITY));
	}

	@Test
	public void testSubtractOperator() {
		assertNull(Rational.ONE.subtract((BigInteger) null));
		assertNull(Rational.ONE.subtract((BigDecimal) null));
		assertNull(Rational.ONE.subtract((Rational) null));

		assertEquals(Rational.NaN, Rational.NaN.subtract(BigInteger.TEN));
		assertEquals(Rational.NaN, Rational.NaN.subtract(6f));
		assertEquals(Rational.NaN, Rational.ONE.subtract(Float.NaN));
		assertEquals(Rational.NaN, Rational.NaN.subtract(6d));
		assertEquals(Rational.NaN, Rational.ONE.subtract(Double.NaN));
		assertEquals(Rational.NaN, Rational.NaN.subtract(BigDecimal.TEN));
		assertEquals(Rational.NaN, Rational.NaN.subtract(Rational.ONE));
		assertEquals(Rational.NaN, Rational.ONE.subtract(Rational.NaN));
		assertEquals(Rational.NaN, Rational.NaN.subtract(Rational.NaN));

		assertEquals(Rational.ONE.negate(), Rational.valueOf(5).subtract(6));
		assertEquals(Rational.valueOf(5), Rational.valueOf(5).subtract(Rational.ZERO));
		assertEquals(Rational.valueOf(5).negate(), Rational.valueOf(-5).subtract(Rational.ZERO));
		assertEquals(Rational.valueOf(-7), Rational.valueOf(5).subtract(12L));
		assertEquals(Rational.valueOf(-13), Rational.valueOf(5).subtract(BigInteger.valueOf(18)));
		assertEquals(Rational.valueOf("7/2"), Rational.valueOf(5).subtract(1.5));
		assertEquals(Rational.valueOf("2497010003/625000000"), Rational.valueOf(5).subtract(new BigDecimal("1.00478399520")));
		assertEquals(Rational.valueOf("-507/74"), Rational.valueOf("24/37").subtract(Rational.valueOf(15, 2)));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.subtract(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.subtract(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.subtract(Float.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.subtract(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.subtract(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.subtract(Double.POSITIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.subtract(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.subtract(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.subtract(Rational.POSITIVE_INFINITY));
	}

	@Test
	public void testAddOperator() {
		assertNull(Rational.ONE.add((BigInteger) null));
		assertNull(Rational.ONE.add((BigDecimal) null));
		assertNull(Rational.ONE.add((Rational) null));

		assertEquals(Rational.NaN, Rational.NaN.add(BigInteger.TEN));
		assertEquals(Rational.NaN, Rational.NaN.add(6f));
		assertEquals(Rational.NaN, Rational.ONE.add(Float.NaN));
		assertEquals(Rational.NaN, Rational.NaN.add(6d));
		assertEquals(Rational.NaN, Rational.ONE.add(Double.NaN));
		assertEquals(Rational.NaN, Rational.NaN.add(BigDecimal.TEN));
		assertEquals(Rational.NaN, Rational.NaN.add(Rational.ONE));
		assertEquals(Rational.NaN, Rational.ONE.add(Rational.NaN));
		assertEquals(Rational.NaN, Rational.NaN.add(Rational.NaN));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(BigInteger.valueOf(100000)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(BigInteger.valueOf(100000).negate()));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(BigInteger.valueOf(100000)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(BigInteger.valueOf(100000).negate()));
		assertEquals(Rational.valueOf("10:20"), Rational.valueOf(10, 20).add(BigInteger.ZERO));
		assertEquals(Rational.valueOf(7, 1), Rational.valueOf(5, 1).add(BigInteger.valueOf(2)));
		assertEquals(Rational.valueOf(3, 1), Rational.valueOf(5, 1).add(BigInteger.valueOf(-2)));
		assertEquals(Rational.valueOf(11), Rational.valueOf(5).add(6));
		assertEquals(Rational.valueOf(17), Rational.valueOf(5).add(12L));
		assertEquals(Rational.valueOf(23), Rational.valueOf(5).add(BigInteger.valueOf(18)));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(100000.004f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(-100000.004f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(100000.004f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(-100000.004f));
		assertEquals(Rational.valueOf("10:20"), Rational.valueOf(10, 20).add(0f));
		assertEquals(Rational.valueOf(7, 1), Rational.valueOf(5, 1).add(2f));
		assertEquals(Rational.valueOf(3, 1), Rational.valueOf(5, 1).add(-2f));
		assertEquals(Rational.valueOf(11), Rational.valueOf(5).add(6f));
		assertEquals(Rational.valueOf(17), Rational.valueOf(5).add(12f));
		assertEquals(Rational.valueOf(23), Rational.valueOf(5).add(18f));
		assertEquals(Rational.valueOf("13/2"), Rational.valueOf(5).add(1.5f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.add(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.add(Float.POSITIVE_INFINITY));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(100000.004d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(-100000.004d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(100000.004d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(-100000.004d));
		assertEquals(Rational.valueOf("10:20"), Rational.valueOf(10, 20).add(0d));
		assertEquals(Rational.valueOf(7, 1), Rational.valueOf(5, 1).add(2d));
		assertEquals(Rational.valueOf(3, 1), Rational.valueOf(5, 1).add(-2d));
		assertEquals(Rational.valueOf(11), Rational.valueOf(5).add(6d));
		assertEquals(Rational.valueOf(17), Rational.valueOf(5).add(12d));
		assertEquals(Rational.valueOf(23), Rational.valueOf(5).add(18d));
		assertEquals(Rational.valueOf("13/2"), Rational.valueOf(5).add(1.5d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.add(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.add(Double.POSITIVE_INFINITY));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(new BigDecimal("1.00478399520")));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(new BigDecimal("-1.00478399520")));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(new BigDecimal("1.00478399520")));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(new BigDecimal("-1.00478399520")));
		assertEquals(Rational.valueOf(3, 15), Rational.valueOf(3, 15).add(BigDecimal.ZERO));
		assertEquals(Rational.valueOf(3, 15).negate(), Rational.valueOf(3, -15).add(BigDecimal.ZERO));
		assertEquals(Rational.valueOf("3752989997/625000000"), Rational.valueOf(5).add(new BigDecimal("1.00478399520")));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(Rational.valueOf(10000, 47)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(Rational.valueOf(10000, -47)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(Rational.valueOf(10000, 47)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(Rational.valueOf(-10000, 47)));
		assertEquals(Rational.valueOf("10:20"), Rational.valueOf(10, 20).add(Rational.ZERO));
		assertEquals(Rational.valueOf(7, 1), Rational.valueOf(5, 1).add(Rational.valueOf(4, 2)));
		assertEquals(Rational.valueOf(3, 1), Rational.valueOf(5, 1).add(Rational.valueOf(-32, 16)));
		assertEquals(Rational.valueOf(11), Rational.valueOf(5).add(Rational.valueOf(6.0)));
		assertEquals(Rational.valueOf(17), Rational.valueOf(5).add(Rational.valueOf(12)));
		assertEquals(Rational.valueOf(23), Rational.valueOf(5).add(Rational.valueOf(18.00000)));
		assertEquals(Rational.valueOf("13/2"), Rational.valueOf(5).add(Rational.valueOf(3, 2)));
		assertEquals(Rational.valueOf("603/74"), Rational.valueOf("24/37").add(Rational.valueOf(15, 2)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.add(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.add(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.add(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.add(Rational.POSITIVE_INFINITY));
	}

	@Test
	public void testReciprocalOperator() {
		assertEquals(Rational.valueOf("1/5"), Rational.valueOf(5).reciprocal());
		assertEquals(Rational.valueOf("1/50"), Rational.valueOf(50L).reciprocal());
		assertEquals(Rational.valueOf("1/238903247898923"), Rational.valueOf(new BigInteger("238903247898923")).reciprocal());
		assertEquals(Rational.valueOf("2/13"), Rational.valueOf("13/2").reciprocal());
		assertEquals(Rational.valueOf("625000000/3752989997"), Rational.valueOf("3752989997/625000000").reciprocal());
		assertEquals(Rational.valueOf(37, 24), Rational.valueOf(24, 37).reciprocal());
		assertTrue(Rational.ZERO.reciprocal().isNaN());
		assertEquals(Rational.NaN, Rational.ZERO.reciprocal());
		assertNotEquals(Rational.ZERO, Rational.ZERO.reciprocal());
		assertEquals(Rational.NaN, Rational.NaN.reciprocal());
		assertEquals(Rational.ZERO, Rational.POSITIVE_INFINITY.reciprocal());
		assertEquals(Rational.ZERO, Rational.NEGATIVE_INFINITY.reciprocal());
	}

	@Test
	public void testDivideOperator() {
		assertEquals(Rational.NaN, Rational.NaN.divide(6));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(5).divide(0));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-5).divide(0));
		assertEquals(Rational.NaN, Rational.valueOf(0).divide(0));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(6));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(6));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(-6));
		assertEquals(Rational.valueOf(9, 16), Rational.valueOf(9, 16).divide(1));
		assertEquals(Rational.valueOf(9, 16).negate(), Rational.valueOf(9, 16).divide(-1));
		assertEquals(Rational.valueOf(5, 6), Rational.valueOf(5).divide(6));
		assertEquals(Rational.valueOf(5, 12), Rational.valueOf(5).divide(12L));
		assertEquals(Rational.valueOf(5, 18), Rational.valueOf(5).divide(18));

		assertNull(Rational.ONE.divide((BigInteger) null));
		assertEquals(Rational.NaN, Rational.NaN.divide(BigInteger.valueOf(6)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(5).divide(BigInteger.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-5).divide(BigInteger.ZERO));
		assertEquals(Rational.NaN, Rational.valueOf(0).divide(BigInteger.ZERO));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(BigInteger.valueOf(6)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigInteger.valueOf(6)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigInteger.valueOf(-6)));
		assertEquals(Rational.valueOf(9, 16), Rational.valueOf(9, 16).divide(BigInteger.ONE));
		assertEquals(Rational.valueOf(9, 16).negate(), Rational.valueOf(9, 16).divide(BigInteger.ONE.negate()));
		assertEquals(Rational.valueOf(5, 6), Rational.valueOf(5).divide(BigInteger.valueOf(6)));
		assertEquals(Rational.valueOf(5, 12), Rational.valueOf(5).divide(BigInteger.valueOf(12L)));
		assertEquals(Rational.valueOf(5, 18), Rational.valueOf(5).divide(BigInteger.valueOf(18)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigInteger.TEN));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigInteger.TEN.negate()));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigInteger.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigInteger.TEN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigInteger.TEN.negate()));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(BigInteger.ZERO));
		assertEquals(Rational.valueOf(4.97), Rational.valueOf(4.97).divide(BigInteger.ONE));
		assertEquals(Rational.valueOf(4.97).negate(), Rational.valueOf(4.97).divide(BigInteger.ONE.negate()));

		assertEquals(Rational.NaN, Rational.NaN.divide(6f));
		assertEquals(Rational.NaN, Rational.ONE.divide(Float.NaN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(0f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(0f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(3f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(-2f));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Float.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Float.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(0f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(7.5f));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(-5f));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(0f));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-4.97).divide(0f));
		assertEquals(Rational.valueOf("10/3"), Rational.valueOf(5f).divide(1.5f));
		assertEquals(Rational.NaN, Rational.ZERO.divide(0f));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Float.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Float.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Float.NEGATIVE_INFINITY));

		assertEquals(Rational.NaN, Rational.NaN.divide(6d));
		assertEquals(Rational.NaN, Rational.ONE.divide(Double.NaN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(0d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(0d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(3d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(-2d));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Double.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Double.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(0d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(7.5d));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(-5d));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(0d));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-4.97).divide(0d));
		assertEquals(Rational.valueOf("10/3"), Rational.valueOf(5f).divide(1.5d));
		assertEquals(Rational.NaN, Rational.ZERO.divide(0d));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Double.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Double.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Double.NEGATIVE_INFINITY));

		assertNull(Rational.ONE.divide((BigDecimal) null));
		assertEquals(Rational.NaN, Rational.NaN.divide(BigDecimal.valueOf(6)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(5).divide(BigDecimal.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-5).divide(BigDecimal.ZERO));
		assertEquals(Rational.NaN, Rational.valueOf(0).divide(BigDecimal.ZERO));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(BigDecimal.valueOf(6.3)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigDecimal.valueOf(6.4)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigDecimal.valueOf(-6.9)));
		assertEquals(Rational.valueOf(9, 16), Rational.valueOf(9, 16).divide(BigDecimal.ONE));
		assertEquals(Rational.valueOf(9, 16).negate(), Rational.valueOf(9, 16).divide(BigDecimal.ONE.negate()));
		assertEquals(Rational.valueOf(25, 31), Rational.valueOf(5).divide(BigDecimal.valueOf(6.2)));
		assertEquals(Rational.valueOf(5, 12), Rational.valueOf(5).divide(BigDecimal.valueOf(12L)));
		assertEquals(Rational.valueOf(5, 18), Rational.valueOf(5).divide(BigDecimal.valueOf(18)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigDecimal.TEN));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(BigDecimal.TEN.negate()));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigDecimal.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigDecimal.TEN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(BigDecimal.TEN.negate()));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(BigDecimal.ZERO));
		assertEquals(Rational.valueOf(4.97), Rational.valueOf(4.97).divide(BigDecimal.ONE));
		assertEquals(Rational.valueOf(4.97).negate(), Rational.valueOf(4.97).divide(BigDecimal.ONE.negate()));
		assertEquals(Rational.valueOf("3125000000/627989997"), Rational.valueOf(5).divide(new BigDecimal("1.00478399520")));

		assertNull(Rational.ONE.divide((Rational) null));
		assertEquals(Rational.NaN, Rational.NaN.divide(Rational.valueOf(6)));
		assertEquals(Rational.NaN, Rational.ONE.divide(Rational.NaN));
		assertEquals(Rational.NaN, Rational.NaN.divide(Rational.NaN));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.valueOf(9, 3)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.valueOf(4, -2)));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.valueOf(15, 2)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.valueOf(25, -5)));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(Rational.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.valueOf(-4.97).divide(Rational.ZERO));
		assertEquals(Rational.valueOf("10/3"), Rational.valueOf(5f).divide(Rational.valueOf(6, 4)));

		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.ZERO));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.valueOf(15, 2)));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.divide(Rational.valueOf(15, -3)));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.ZERO));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.valueOf(15, 2)));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.divide(Rational.valueOf(15, -3)));
		assertEquals(Rational.ZERO, Rational.ZERO.divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, 2).divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.ZERO, Rational.valueOf(15, -3).divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.valueOf(4.97).divide(Rational.ZERO));
		assertEquals(Rational.valueOf(4.97), Rational.valueOf(4.97).divide(Rational.ONE));
		assertEquals(Rational.valueOf(4.97).negate(), Rational.valueOf(4.97).divide(Rational.ONE.negate()));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.NEGATIVE_INFINITY.divide(Rational.POSITIVE_INFINITY));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.divide(Rational.NEGATIVE_INFINITY));
		assertEquals(Rational.valueOf("16/185"), Rational.valueOf("24/37").divide(Rational.valueOf(15, 2)));
		assertEquals(Rational.NaN, Rational.ZERO.divide(Rational.ZERO));
	}

	@Test
	public void testNegateOperator() {
		assertEquals(Rational.valueOf("-5"), Rational.valueOf(5).negate());
		assertEquals(Rational.valueOf("-4/5"), Rational.valueOf("32/40").negate());
		assertEquals(Rational.ZERO, Rational.ZERO.negate());
		assertEquals(Rational.NaN, Rational.NaN.negate());
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.POSITIVE_INFINITY.negate());
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.negate());
	}

	@Test
	public void testAbsOperator() {
		assertEquals(Rational.valueOf("5"), Rational.valueOf(5).abs());
		assertEquals(Rational.valueOf("5"), Rational.valueOf(-5).abs());
		assertEquals(Rational.valueOf("32/40"), Rational.valueOf("32/-40").negate());
		assertEquals(Rational.ZERO, Rational.ZERO.abs());
		assertEquals(Rational.NaN, Rational.NaN.abs());
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.abs());
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.abs());
	}

	@Test
	public void testPowOperator() {
		assertEquals(Rational.NaN, Rational.NaN.pow(0));
		assertEquals(Rational.NaN, Rational.NaN.pow(10));
		assertEquals(Rational.NaN, Rational.NaN.pow(-10));
		assertEquals(Rational.NaN, Rational.POSITIVE_INFINITY.pow(0));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(10));
		assertEquals(Rational.ZERO, Rational.POSITIVE_INFINITY.pow(-10));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(10));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(1));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(2));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(3));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(55));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(100));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(10));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(1));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(2));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(3));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(55));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(100));
		assertEquals(Rational.ZERO, Rational.NEGATIVE_INFINITY.pow(-10));
		assertEquals(Rational.NaN, Rational.ZERO.pow(0));
		assertEquals(Rational.NaN, Rational.NaN.pow(1));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.pow(1));
		assertEquals(Rational.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.pow(1));
		assertEquals(Rational.ZERO, Rational.ZERO.pow(1));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.ZERO.pow(-1));
		assertEquals(Rational.POSITIVE_INFINITY, Rational.ZERO.pow(-99));
		assertEquals(Rational.ZERO, Rational.ZERO.pow(100));

		assertEquals(Rational.ONE, Rational.valueOf(5).pow(0));
		assertEquals(Rational.valueOf("-5"), Rational.valueOf(-5).pow(1));
		assertEquals(Rational.valueOf("-1073741824/30517578125"), Rational.valueOf("32/-40").pow(15));
		assertEquals(Rational.valueOf("268435456/6103515625"), Rational.valueOf("32/-40").pow(14));
		assertEquals(Rational.valueOf("1/9765625"), Rational.valueOf(-5).pow(-10));
		assertEquals(Rational.ZERO, Rational.ZERO.pow(4));
	}

	@Test
	public void testSignum() {
		assertEquals(1, Rational.valueOf(5).signum());
		assertEquals(-1, Rational.valueOf(-15).signum());
		assertEquals(0, Rational.ZERO.signum());
		assertEquals(-1, Rational.valueOf("32/-40").pow(15).signum());
		assertEquals(1, Rational.valueOf("32/-40").pow(14).signum());
		assertEquals(0, Rational.NaN.signum());
		assertEquals(1, Rational.POSITIVE_INFINITY.signum());
		assertEquals(-1, Rational.NEGATIVE_INFINITY.signum());
	}

	@Test
	public void testIsInteger() {
		assertFalse(Rational.NaN.isInteger());
		assertFalse(Rational.POSITIVE_INFINITY.isInteger());
		assertFalse(Rational.NEGATIVE_INFINITY.isInteger());
		assertTrue(Rational.valueOf(5).isInteger());
		assertTrue(Rational.valueOf(0).isInteger());
		assertTrue(Rational.valueOf(-5).isInteger());
		assertFalse(Rational.valueOf(5.5).isInteger());
		assertFalse(Rational.valueOf(5, 2).isInteger());
	}

	@Test
	public void testGetters() {
		Rational rational = Rational.valueOf(22, 6);
		assertEquals(22, rational.getNumerator().intValue());
		assertEquals(6, rational.getDenominator().intValue());
		assertEquals(11, rational.getReducedNumerator().intValue());
		assertEquals(3, rational.getReducedDenominator().intValue());
		assertEquals(2, rational.getGreatestCommonDivisor().intValue());
	}

	@Test
	public void testStringOutputs() {
		assertEquals("NaN", Rational.NaN.toUnreducedString());
		assertEquals("NaN", Rational.NaN.toString());
		assertEquals("Value: NaN, Reduced: NaN, Decimal: NaN", Rational.NaN.toDebugString());
		assertEquals("NaN", Rational.NaN.toDecimalString());
		assertEquals("NaN", Rational.NaN.toUnreducedHexString());
		assertEquals("NaN", Rational.NaN.toHexString());

		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toUnreducedString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toString());
		assertEquals("Value: \u221e, Reduced: \u221e, Decimal: \u221e", Rational.POSITIVE_INFINITY.toDebugString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toDecimalString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toUnreducedHexString());
		assertEquals("\u221e", Rational.POSITIVE_INFINITY.toHexString());

		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toUnreducedString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toString());
		assertEquals("Value: -\u221e, Reduced: -\u221e, Decimal: -\u221e", Rational.NEGATIVE_INFINITY.toDebugString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toDecimalString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toUnreducedHexString());
		assertEquals("-\u221e", Rational.NEGATIVE_INFINITY.toHexString());

		Rational rational = Rational.valueOf(22, 6);
		assertEquals("22/6", rational.toUnreducedString());
		assertEquals("11/3", rational.toString());
		assertEquals("3.66666666666666666667", rational.toDecimalString());
		assertEquals("16/6", rational.toUnreducedHexString());
		assertEquals("b/3", rational.toHexString());
		assertEquals("Value: 22/6, Reduced: 11/3, Decimal: 3.66666666666666666667", rational.toDebugString());
		assertEquals("11:3", rational.toAspectRatio());
		assertEquals("19:16", Rational.valueOf(1.18).toAspectRatio());
		assertEquals("19:16", Rational.valueOf(1.195).toAspectRatio());
		assertEquals("5:4", Rational.valueOf(1.24).toAspectRatio());
		assertEquals("5:4", Rational.valueOf(1.26).toAspectRatio());
		assertEquals("4:3", Rational.valueOf(1.31).toAspectRatio());
		assertEquals("4:3", Rational.valueOf(1.35).toAspectRatio());
		assertEquals("1.37:1", Rational.valueOf(1.36).toAspectRatio());
		assertEquals("1.37:1", Rational.valueOf(1.372).toAspectRatio());
		assertEquals("11:8", Rational.valueOf(1.373).toAspectRatio());
		assertEquals("11:8", Rational.valueOf(1.399).toAspectRatio());
		assertEquals("1.43:1", Rational.valueOf(1.420001).toAspectRatio());
		assertEquals("1.43:1", Rational.valueOf(1.439999).toAspectRatio());
		assertEquals("3:2", Rational.valueOf(1.49).toAspectRatio());
		assertEquals("3:2", Rational.valueOf(1.51).toAspectRatio());
		assertEquals("14:9", Rational.valueOf(1.54).toAspectRatio());
		assertEquals("14:9", Rational.valueOf(1.56).toAspectRatio());
		assertEquals("16:10", Rational.valueOf(1.59).toAspectRatio());
		assertEquals("16:10", Rational.valueOf(1.61).toAspectRatio());
		assertEquals("15:9", Rational.valueOf(1.65).toAspectRatio());
		assertEquals("15:9", Rational.valueOf(1.67).toAspectRatio());
		assertEquals("7:4", Rational.valueOf(1.7400001).toAspectRatio());
		assertEquals("7:4", Rational.valueOf(1.7599999).toAspectRatio());
		assertEquals("16:9", Rational.valueOf(1.76).toAspectRatio());
		assertEquals("16:9", Rational.valueOf(1.8).toAspectRatio());
		assertEquals("1.85:1", Rational.valueOf(1.84).toAspectRatio());
		assertEquals("1.85:1", Rational.valueOf(1.86).toAspectRatio());
		assertEquals("1.896:1", Rational.valueOf(1.89).toAspectRatio());
		assertEquals("1.896:1", Rational.valueOf(1.9).toAspectRatio());
		assertEquals("2.00:1", Rational.valueOf(2).toAspectRatio());
		assertEquals("11:5", Rational.valueOf(2.19).toAspectRatio());
		assertEquals("11:5", Rational.valueOf(2.21).toAspectRatio());
		assertEquals("2.35:1", Rational.valueOf(2.34).toAspectRatio());
		assertEquals("2.35:1", Rational.valueOf(2.36).toAspectRatio());
		assertEquals("2.37:1", Rational.valueOf(2.3600001).toAspectRatio());
		assertEquals("2.37:1", Rational.valueOf(2.3799999).toAspectRatio());
		assertEquals("2.39:1", Rational.valueOf(2.38).toAspectRatio());
		assertEquals("2.39:1", Rational.valueOf(2.41).toAspectRatio());
		assertEquals("2.55:1", Rational.valueOf(2.54).toAspectRatio());
		assertEquals("2.55:1", Rational.valueOf(2.56).toAspectRatio());
		assertEquals("2.59:1", Rational.valueOf(2.58).toAspectRatio());
		assertEquals("2.59:1", Rational.valueOf(2.6).toAspectRatio());
		assertEquals("24:9", Rational.valueOf(2.65).toAspectRatio());
		assertEquals("24:9", Rational.valueOf(2.67).toAspectRatio());
		assertEquals("2.76:1", Rational.valueOf(2.75).toAspectRatio());
		assertEquals("2.76:1", Rational.valueOf(2.77).toAspectRatio());
		assertEquals("4.00:1", Rational.valueOf(3.9).toAspectRatio());
		assertEquals("4.00:1", Rational.valueOf(4.1).toAspectRatio());
		assertEquals("12.00:1", Rational.valueOf(11.9).toAspectRatio());
		assertEquals("12.00:1", Rational.valueOf(12.1).toAspectRatio());
		assertEquals("1:1", Rational.NaN.toAspectRatio());
		assertEquals("1:1", Rational.POSITIVE_INFINITY.toAspectRatio());
		assertEquals("1:1", Rational.NEGATIVE_INFINITY.toAspectRatio());
	}

	@Test
	public void testNumberInterface() {
		Rational rational = Rational.valueOf(22, 6);
		assertEquals(3, rational.intValue());
		assertEquals(3L, rational.longValue());
		assertEquals(-3L, rational.negate().longValue());
		assertEquals(BigInteger.valueOf(3), rational.bigIntegerValue());
		assertEquals(3.666667f, rational.floatValue(), 0.0f);
		assertEquals(3.666666666666667, rational.doubleValue(), 0.0);
		assertEquals(
			new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666667"),
			rational.bigDecimalValue()
		);
		assertEquals(
			new BigDecimal("3.6666666666666666666"),
			rational.bigDecimalValue(new MathContext(20, RoundingMode.FLOOR))
		);
		assertEquals(
			new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666"),
			rational.bigDecimalValue(RoundingMode.FLOOR)
		);
		assertEquals(
			new BigDecimal("3.66666666666666666666666666666666666666666666666667"),
			rational.bigDecimalValue(50, RoundingMode.CEILING)
		);

		assertEquals(0, Rational.NaN.intValue());
		assertEquals(0, Rational.NaN.longValue());
		try {
			assertEquals(BigInteger.ZERO, Rational.NaN.bigIntegerValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express NaN as BigInteger");
		}
		assertEquals(Float.NaN, Rational.NaN.floatValue(), 0.0f);
		assertEquals(Double.NaN, Rational.NaN.doubleValue(), 0.0d);
		try {
			assertEquals(BigDecimal.ZERO, Rational.NaN.bigDecimalValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express NaN as BigDecimal");
		}

		assertEquals(Integer.MAX_VALUE, Rational.POSITIVE_INFINITY.intValue());
		assertEquals(Long.MAX_VALUE, Rational.POSITIVE_INFINITY.longValue());
		try {
			assertEquals(BigInteger.ZERO, Rational.POSITIVE_INFINITY.bigIntegerValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express infinity as BigInteger");
		}
		assertEquals(Float.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.floatValue(), 0.0f);
		assertEquals(Double.POSITIVE_INFINITY, Rational.POSITIVE_INFINITY.doubleValue(), 0.0d);
		try {
			assertEquals(BigDecimal.ZERO, Rational.POSITIVE_INFINITY.bigDecimalValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express infinity as BigDecimal");
		}

		assertEquals(Integer.MIN_VALUE, Rational.NEGATIVE_INFINITY.intValue());
		assertEquals(Long.MIN_VALUE, Rational.NEGATIVE_INFINITY.longValue());
		try {
			assertEquals(BigInteger.ZERO, Rational.NEGATIVE_INFINITY.bigIntegerValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express infinity as BigInteger");
		}
		assertEquals(Float.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.floatValue(), 0.0f);
		assertEquals(Double.NEGATIVE_INFINITY, Rational.NEGATIVE_INFINITY.doubleValue(), 0.0d);
		try {
			assertEquals(BigDecimal.ZERO, Rational.NEGATIVE_INFINITY.bigDecimalValue());
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Impossible to express infinity as BigDecimal");
		}

		try {
			rational.bigDecimalValue(MathContext.UNLIMITED);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Non-terminating decimal expansion; no exact representable decimal result.");
		}
	}

	@Test
	public void testComparison() {
		assertEquals(0, Rational.valueOf(22, 6).compareTo(Rational.valueOf(11, 3)));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(Rational.valueOf(11, 4)));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(Rational.valueOf(12, 3)));
		assertEquals(1, Rational.valueOf(22, 6).compareTo((byte) 3));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo((byte) 4));
		assertEquals(1, Rational.valueOf(22, 6).compareTo((short) 3));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo((short) 4));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(4));
		assertEquals(0, Rational.valueOf(24, 6).compareTo(4));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3L));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(4L));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3f));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3.6f));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(3.7f));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3.666666f));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(3.666667f));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(3.666666666666666));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(3.666666666666667));
		assertEquals(1, Rational.valueOf(22, 6).compareTo(new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666")));
		assertEquals(-1, Rational.valueOf(22, 6).compareTo(new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666667")));
		assertTrue(Rational.valueOf(22, 6).equalsExact(Rational.valueOf(22, 6)));
		assertFalse(Rational.valueOf(22, 6).equalsExact(Rational.valueOf(11, 3)));
		assertEquals(1, Rational.NaN.compareTo(Integer.MAX_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Integer.MIN_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Long.MAX_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Long.MIN_VALUE));
		assertEquals(1, Rational.NaN.compareTo(BigInteger.valueOf(Long.MAX_VALUE).pow(3)));
		assertEquals(1, Rational.NaN.compareTo(BigInteger.valueOf(Long.MIN_VALUE).pow(3)));
		assertEquals(1, Rational.NaN.compareTo(Float.MAX_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Float.MIN_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Float.POSITIVE_INFINITY));
		assertEquals(1, Rational.NaN.compareTo(Float.NEGATIVE_INFINITY));
		assertEquals(0, Rational.NaN.compareTo(Float.NaN));
		assertEquals(1, Rational.NaN.compareTo(Double.MAX_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Double.MIN_VALUE));
		assertEquals(1, Rational.NaN.compareTo(Double.POSITIVE_INFINITY));
		assertEquals(1, Rational.NaN.compareTo(Double.NEGATIVE_INFINITY));
		assertEquals(0, Rational.NaN.compareTo(Double.NaN));
		assertEquals(1, Rational.NaN.compareTo(BigDecimal.valueOf(Long.MAX_VALUE).pow(5)));
		assertEquals(1, Rational.NaN.compareTo(BigDecimal.valueOf(Long.MIN_VALUE).pow(5)));
		assertEquals(1, Rational.NaN.compareTo(Rational.POSITIVE_INFINITY));
		assertEquals(1, Rational.NaN.compareTo(Rational.NEGATIVE_INFINITY));
		assertEquals(0, Rational.NaN.compareTo(Rational.NaN));

		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Integer.MAX_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Integer.MIN_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Long.MAX_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Long.MIN_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(BigInteger.valueOf(Long.MAX_VALUE).pow(3)));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(BigInteger.valueOf(Long.MIN_VALUE).pow(3)));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Float.MAX_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Float.MIN_VALUE));
		assertEquals(0, Rational.POSITIVE_INFINITY.compareTo(Float.POSITIVE_INFINITY));
		assertEquals(2, Rational.POSITIVE_INFINITY.compareTo(Float.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.POSITIVE_INFINITY.compareTo(Float.NaN));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Double.MAX_VALUE));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(Double.MIN_VALUE));
		assertEquals(0, Rational.POSITIVE_INFINITY.compareTo(Double.POSITIVE_INFINITY));
		assertEquals(2, Rational.POSITIVE_INFINITY.compareTo(Double.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.POSITIVE_INFINITY.compareTo(Double.NaN));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(BigDecimal.valueOf(Long.MAX_VALUE).pow(5)));
		assertEquals(1, Rational.POSITIVE_INFINITY.compareTo(BigDecimal.valueOf(Long.MIN_VALUE).pow(5)));
		assertEquals(0, Rational.POSITIVE_INFINITY.compareTo(Rational.POSITIVE_INFINITY));
		assertEquals(2, Rational.POSITIVE_INFINITY.compareTo(Rational.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.POSITIVE_INFINITY.compareTo(Rational.NaN));

		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Integer.MAX_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Integer.MIN_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Long.MAX_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Long.MIN_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(BigInteger.valueOf(Long.MAX_VALUE).pow(3)));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(BigInteger.valueOf(Long.MIN_VALUE).pow(3)));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Float.MAX_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Float.MIN_VALUE));
		assertEquals(-2, Rational.NEGATIVE_INFINITY.compareTo(Float.POSITIVE_INFINITY));
		assertEquals(0, Rational.NEGATIVE_INFINITY.compareTo(Float.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Float.NaN));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Double.MAX_VALUE));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Double.MIN_VALUE));
		assertEquals(-2, Rational.NEGATIVE_INFINITY.compareTo(Double.POSITIVE_INFINITY));
		assertEquals(0, Rational.NEGATIVE_INFINITY.compareTo(Double.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Double.NaN));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(BigDecimal.valueOf(Long.MAX_VALUE).pow(5)));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(BigDecimal.valueOf(Long.MIN_VALUE).pow(5)));
		assertEquals(-2, Rational.NEGATIVE_INFINITY.compareTo(Rational.POSITIVE_INFINITY));
		assertEquals(0, Rational.NEGATIVE_INFINITY.compareTo(Rational.NEGATIVE_INFINITY));
		assertEquals(-1, Rational.NEGATIVE_INFINITY.compareTo(Rational.NaN));
	}

	@Test
	public void testGetMultipliers() {
		Rational a = Rational.valueOf(22, 6);
		Rational b = Rational.valueOf(-5, 34);
		BigInteger[] array = a.getMultipliers(b);
		assertArrayEquals(new BigInteger[] {BigInteger.valueOf(34), BigInteger.valueOf(3)}, array);
		assertEquals(a.reducedDenominator.multiply(array[0]), b.reducedDenominator.multiply(array[1]));

		a = Rational.valueOf(4.235);
		b = Rational.valueOf(78, -14);
		array = a.getMultipliers(b);
		assertArrayEquals(new BigInteger[] {BigInteger.valueOf(7), BigInteger.valueOf(200)}, array);
		assertEquals(a.reducedDenominator.multiply(array[0]), b.reducedDenominator.multiply(array[1]));

		a = Rational.NaN;
		b = Rational.valueOf(78, -14);
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}

		a = Rational.valueOf(4.235);
		b = Rational.NaN;
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}

		a = Rational.POSITIVE_INFINITY;
		b = Rational.valueOf(78, -14);
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}

		a = Rational.valueOf(4.235);
		b = Rational.POSITIVE_INFINITY;
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}

		a = Rational.NEGATIVE_INFINITY;
		b = Rational.valueOf(78, -14);
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}

		a = Rational.valueOf(4.235);
		b = Rational.NEGATIVE_INFINITY;
		try {
			array = a.getMultipliers(b);
			Assert.fail("Expected ArithmeticException");
		} catch (ArithmeticException e) {
			assertThat(e)
				.isInstanceOf(ArithmeticException.class)
				.hasMessage("Can't calculate multipliers for NaN or infinity");
		}
	}

	@Test
	public void testCalculateGreatestCommonDivisor() {
		assertEquals(5L, Rational.calculateGreatestCommonDivisor(40, 5));
		assertEquals(1L, Rational.calculateGreatestCommonDivisor(40, 7));
		assertEquals(2L, Rational.calculateGreatestCommonDivisor(-54, 128));
	}

	@Test
	public void testCalculateLeastCommonMultiple() {
		assertEquals(BigInteger.valueOf(40), Rational.calculateLeastCommonMultiple(BigInteger.valueOf(40), BigInteger.valueOf(5)));
		assertEquals(BigInteger.valueOf(65610), Rational.calculateLeastCommonMultiple(BigInteger.valueOf(270), BigInteger.valueOf(6561)));
		assertEquals(BigInteger.valueOf(315), Rational.calculateLeastCommonMultiple(BigInteger.valueOf(63), BigInteger.valueOf(35)));
	}
}
