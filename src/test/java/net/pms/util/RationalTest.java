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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RationalTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testDefaultConstructor() {
		assertEquals(Rational.ZERO, new Rational());
		assertEquals(BigInteger.ZERO, Rational.ZERO.numerator);
		assertEquals(BigInteger.ONE, Rational.ZERO.denominator);
		assertEquals(BigInteger.ONE, Rational.ZERO.greatestCommonDivisor);
		assertEquals(BigInteger.ZERO, Rational.ZERO.reducedNumerator);
		assertEquals(BigInteger.ONE, Rational.ZERO.reducedDenominator);
		assertEquals("0", Rational.ZERO.stringValue);
		assertEquals("0", Rational.ZERO.reducedStringValue);
		assertEquals("0", Rational.ZERO.decimalStringValue);

	}

	@Test
	public void testDoubleConstructor() {
		assertEquals(0.342, new Rational(0.342).doubleValue(), 0.0);
		assertEquals(10000d, new Rational(10000d).doubleValue(), 0.0);
		assertEquals(1d/3, new Rational(1d/3).doubleValue(), 0.0);
	}

	@Test
	public void testBigDecimalConstructor() {
		assertEquals(0, new Rational(BigDecimal.ZERO).intValue());
		assertEquals(BigDecimal.ONE, new Rational(BigDecimal.ONE).bigDecimalValue());
		assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), new Rational(BigDecimal.valueOf(Long.MAX_VALUE)).bigDecimalValue());
		assertEquals(BigDecimal.valueOf(Long.MIN_VALUE), new Rational(BigDecimal.valueOf(Long.MIN_VALUE)).bigDecimalValue());
		assertEquals(
			new BigDecimal("398427545234132213.4398023478543978313289043509400000000000000000000000000000000000000000000000000000000000000000000000"),
			new Rational(new BigDecimal("398427545234132213.43980234785439783132890435094")).bigDecimalValue()
		);
		assertEquals(
			0,
			new BigDecimal("398427545234132213.43980234785439783132890435094")
				.compareTo(new Rational(new BigDecimal("398427545234132213.43980234785439783132890435094")).bigDecimalValue())
		);
	}

	@Test
	public void testStringConstructor() {
		assertEquals(Rational.ZERO, new Rational(""));
		assertEquals(Rational.ZERO, new Rational((String) null));
		assertEquals(4, new Rational("16 /4").intValue());
		assertEquals(5, new Rational("20/4 ").intValue());
		assertEquals(6d, new Rational("  24 / 4").doubleValue(), 0.0);
		assertEquals(-6d, new Rational("-24/4").doubleValue(), 0.0);
		assertEquals(-6d, new Rational("24/ -4").doubleValue(), 0.0);
		assertEquals(6d, new Rational("-24/-4").doubleValue(), 0.0);
		assertEquals(0.5625, new Rational("9/16").doubleValue(), 0.0);
		assertEquals(-2194d, new Rational("-2194").doubleValue(), 0.0);
	}

	@Test
	public void testIntConstructor() {
		assertEquals(Rational.ZERO, new Rational(0));
		assertEquals(-4096, new Rational(-4096).intValue());
		assertEquals(Integer.MAX_VALUE, new Rational(Integer.MAX_VALUE).intValue());
		assertEquals(Integer.MIN_VALUE, new Rational(Integer.MIN_VALUE).intValue());
	}

	@Test
	public void testLongConstructor() {
		assertEquals(Rational.ZERO, new Rational(0L));
		assertEquals(-4096, new Rational(-4096L).intValue());
		assertEquals(Long.MAX_VALUE, new Rational(Long.MAX_VALUE).longValue());
		assertEquals(Long.MIN_VALUE, new Rational(Long.MIN_VALUE).longValue());
		assertEquals(BigInteger.ONE, new Rational(Long.MIN_VALUE).greatestCommonDivisor);
		assertEquals(BigInteger.ONE, new Rational(Long.MIN_VALUE).denominator);
		assertEquals(BigInteger.ONE, new Rational(Long.MIN_VALUE).reducedDenominator);
	}

	@Test
	public void testBigIntegerConstructor() {
		assertEquals(Rational.ZERO, new Rational(BigInteger.valueOf(0)));
		assertEquals("987933205489029348556903", new Rational(new BigInteger("987933205489029348556903")).stringValue);
		assertEquals(Long.MAX_VALUE, new Rational(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
		assertEquals(Long.MIN_VALUE, new Rational(BigInteger.valueOf(Long.MIN_VALUE)).longValue());
		assertEquals(BigInteger.ONE, new Rational(BigInteger.valueOf(Long.MIN_VALUE)).greatestCommonDivisor);
		assertEquals(BigInteger.ONE, new Rational(BigInteger.valueOf(Long.MIN_VALUE)).denominator);
		assertEquals(BigInteger.ONE, new Rational(BigInteger.valueOf(Long.MIN_VALUE)).reducedDenominator);
	}

	@Test
	public void testTwoIntsConstructor() {
		assertEquals(Rational.ZERO, new Rational(0, 30));
		assertEquals(-1, new Rational(-1, 30).signum());
		assertEquals(-1, new Rational(1, -30).signum());
		assertEquals(1, new Rational(-1, -30).signum());
		assertEquals(Rational.ONE, new Rational(Integer.MAX_VALUE, Integer.MAX_VALUE));
		assertEquals(Rational.ONE.negate(), new Rational(Integer.MAX_VALUE, -Integer.MAX_VALUE));
		assertEquals("18/32", new Rational(18, 32).toString());
		assertEquals("9/16", new Rational(18, 32).toReducedString());
		assertEquals(0.5625, new Rational(18, 32).doubleValue(), 0.0);
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("denominator can't be zero");
		assertEquals(Rational.ZERO, new Rational(0, 0));
	}

	@Test
	public void testTwoLongsConstructor() {
		assertEquals(Rational.ZERO, new Rational(0L, 30L));
		assertEquals(-1, new Rational(-1L, 30L).signum());
		assertEquals(-1, new Rational(1L, -30L).signum());
		assertEquals(1, new Rational(-1L, -30L).signum());
		assertEquals(Rational.ONE, new Rational(Long.MAX_VALUE, Long.MAX_VALUE));
		assertEquals(Rational.ONE.negate(), new Rational(Long.MAX_VALUE, -Long.MAX_VALUE));
		assertEquals("36/64", new Rational(36L, 64L).toString());
		assertEquals("9/16", new Rational(36L, 64L).toReducedString());
		assertEquals(0.5625, new Rational(36L, 64L).doubleValue(), 0.0);
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("denominator can't be zero");
		assertEquals(Rational.ZERO, new Rational(0L, 0L));
	}

	@Test
	public void testTwoBigIntegersConstructor() {
		assertEquals(Rational.ZERO, new Rational(null, BigInteger.TEN));
		assertEquals(-1, new Rational(BigInteger.ONE.negate(), BigInteger.valueOf(30)).signum());
		assertEquals(-1, new Rational(BigInteger.ONE, BigInteger.valueOf(-30)).signum());
		assertEquals(1, new Rational(BigInteger.ONE.negate(), BigInteger.valueOf(-30)).signum());
		assertEquals(Rational.ONE, new Rational(
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)),
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE))
		));
		assertEquals(Rational.ONE.negate(), new Rational(
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)),
			BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE)).negate()
		));
		assertEquals(
			"42391158275216203514294433201/75362059155939917358745659024",
			new Rational(new BigInteger("42391158275216203514294433201"), new BigInteger("75362059155939917358745659024")).toString()
		);
		assertEquals("9/16", new Rational(
			new BigInteger("42391158275216203514294433201"), new BigInteger("75362059155939917358745659024")
		).toReducedString());
		assertEquals(0.5625, new Rational(
			new BigInteger("42391158275216203514294433201"), new BigInteger("75362059155939917358745659024")
		).doubleValue(), 0.0);
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("denominator can't be zero");
		assertEquals(Rational.ZERO, new Rational(BigInteger.ZERO, null));
	}

	@Test
	public void testMultiplyOperator() {
		assertEquals(new Rational(30), new Rational(5).multiply(6));
		assertEquals(new Rational(60), new Rational(5).multiply(12L));
		assertEquals(new Rational(90), new Rational(5).multiply(BigInteger.valueOf(18)));
		assertEquals(new Rational("15/2"), new Rational(5).multiply(1.5));
		assertEquals(new Rational("627989997/125000000"), new Rational(5).multiply(new BigDecimal("1.00478399520")));
		assertEquals(new Rational("180/37"), new Rational("24/37").multiply(new Rational(15, 2)));
	}

	@Test
	public void testSubtractOperator() {
		assertEquals(Rational.ONE.negate(), new Rational(5).subtract(6));
		assertEquals(new Rational(-7), new Rational(5).subtract(12L));
		assertEquals(new Rational(-13), new Rational(5).subtract(BigInteger.valueOf(18)));
		assertEquals(new Rational("7/2"), new Rational(5).subtract(1.5));
		assertEquals(new Rational("2497010003/625000000"), new Rational(5).subtract(new BigDecimal("1.00478399520")));
		assertEquals(new Rational("-507/74"), new Rational("24/37").subtract(new Rational(15, 2)));
	}

	@Test
	public void testAddOperator() {
		assertEquals(new Rational(11), new Rational(5).add(6));
		assertEquals(new Rational(17), new Rational(5).add(12L));
		assertEquals(new Rational(23), new Rational(5).add(BigInteger.valueOf(18)));
		assertEquals(new Rational("13/2"), new Rational(5).add(1.5));
		assertEquals(new Rational("3752989997/625000000"), new Rational(5).add(new BigDecimal("1.00478399520")));
		assertEquals(new Rational("603/74"), new Rational("24/37").add(new Rational(15, 2)));
	}

	@Test
	public void testDivideOperator() {
		assertEquals(new Rational(5, 6), new Rational(5).divide(6));
		assertEquals(new Rational("5 / 12"), new Rational(5).divide(12L));
		assertEquals(new Rational("5/18"), new Rational(5).divide(BigInteger.valueOf(18)));
		assertEquals(new Rational("10/3"), new Rational(5).divide(1.5));
		assertEquals(new Rational("3125000000/627989997"), new Rational(5).divide(new BigDecimal("1.00478399520")));
		assertEquals(new Rational("16/185"), new Rational("24/37").divide(new Rational(15, 2)));
	}

	@Test
	public void testReciprocalOperator() {
		assertEquals(new Rational("1/5"), new Rational(5).reciprocal());
		assertEquals(new Rational("1/50"), new Rational(50L).reciprocal());
		assertEquals(new Rational("1/238903247898923"), new Rational(new BigInteger("238903247898923")).reciprocal());
		assertEquals(new Rational("2/13"), new Rational("13/2").reciprocal());
		assertEquals(new Rational("625000000/3752989997"), new Rational("3752989997/625000000").reciprocal());
		assertEquals(new Rational(37, 24), new Rational(24, 37).reciprocal());
		assertEquals(Rational.ZERO, Rational.ZERO.reciprocal());
	}

	@Test
	public void testNegateOperator() {
		assertEquals(new Rational("-5"), new Rational(5).negate());
		assertEquals(new Rational("-4/5"), new Rational("32/40").negate());
		assertEquals(Rational.ZERO, Rational.ZERO.negate());
	}

	@Test
	public void testAbsOperator() {
		assertEquals(new Rational("5"), new Rational(5).abs());
		assertEquals(new Rational("5"), new Rational(-5).abs());
		assertEquals(new Rational("32/40"), new Rational("32/-40").negate());
		assertEquals(Rational.ZERO, Rational.ZERO.abs());
	}

	@Test
	public void testPowOperator() {
		assertEquals(Rational.ONE, new Rational(5).pow(0));
		assertEquals(new Rational("-5"), new Rational(-5).pow(1));
		assertEquals(new Rational("-1073741824/30517578125"), new Rational("32/-40").pow(15));
		assertEquals(new Rational("268435456/6103515625"), new Rational("32/-40").pow(14));
		assertEquals(new Rational("1/9765625"), new Rational(-5).pow(-10));
		assertEquals(Rational.ZERO, Rational.ZERO.pow(4));
	}

	@Test
	public void testSignum() {
		assertEquals(1, new Rational(5).signum());
		assertEquals(-1, new Rational(-15).signum());
		assertEquals(0, new Rational(null, BigInteger.valueOf(3)).signum());
		assertEquals(0, Rational.ZERO.signum());
		assertEquals(-1, new Rational("32/-40").pow(15).signum());
		assertEquals(1, new Rational("32/-40").pow(14).signum());
	}

	@Test
	public void testIsInteger() {
		assertTrue(new Rational(5).isInteger());
		assertTrue(new Rational(0).isInteger());
		assertTrue(new Rational(-5).isInteger());
		assertFalse(new Rational(5.5).isInteger());
		assertFalse(new Rational(5, 2).isInteger());
	}

	@Test
	public void testGetters() {
		Rational rational = new Rational(22, 6);
		assertEquals(22, rational.getNumerator().intValue());
		assertEquals(6, rational.getDenominator().intValue());
		assertEquals(11, rational.getReducedNumerator().intValue());
		assertEquals(3, rational.getReducedDenominator().intValue());
		assertEquals(2, rational.getGreatestCommonDivisor().intValue());
	}

	@Test
	public void testStringOutputs() {
		Rational rational = new Rational(22, 6);
		assertEquals("22/6", rational.toString());
		assertEquals("11/3", rational.toReducedString());
		assertEquals("3.66666666666666666667", rational.toDecimalString());
		assertEquals("16/6", rational.toHexString());
		assertEquals("b/3", rational.toReducedHexString());
		assertEquals("Value: 22/6, Reduced: 11/3, Decimal: 3.66666666666666666667", rational.toDebugString());
	}

	@Test
	public void testNumberInterface() {
		Rational rational = new Rational(22, 6);
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


		exception.expect(ArithmeticException.class);
		exception.expectMessage("Non-terminating decimal expansion; no exact representable decimal result.");
		assertEquals(
			new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666667"),
			rational.bigDecimalValue(MathContext.UNLIMITED)
		);
	}

	@Test
	public void testComparison() {
		assertEquals(0, new Rational(22, 6).compareTo(new Rational(11, 3)));
		assertEquals(1, new Rational(22, 6).compareTo(new Rational(11, 4)));
		assertEquals(-1, new Rational(22, 6).compareTo(new Rational(12, 3)));
		assertEquals(1, new Rational(22, 6).compareTo((byte) 3));
		assertEquals(-1, new Rational(22, 6).compareTo((byte) 4));
		assertEquals(1, new Rational(22, 6).compareTo((short) 3));
		assertEquals(-1, new Rational(22, 6).compareTo((short) 4));
		assertEquals(1, new Rational(22, 6).compareTo(3));
		assertEquals(-1, new Rational(22, 6).compareTo(4));
		assertEquals(0, new Rational(24, 6).compareTo(4));
		assertEquals(1, new Rational(22, 6).compareTo(3L));
		assertEquals(-1, new Rational(22, 6).compareTo(4L));
		assertEquals(1, new Rational(22, 6).compareTo(3f));
		assertEquals(1, new Rational(22, 6).compareTo(3.6f));
		assertEquals(-1, new Rational(22, 6).compareTo(3.7f));
		assertEquals(1, new Rational(22, 6).compareTo(3.666666f));
		assertEquals(-1, new Rational(22, 6).compareTo(3.666667f));
		assertEquals(1, new Rational(22, 6).compareTo(3.666666666666666));
		assertEquals(-1, new Rational(22, 6).compareTo(3.666666666666667));
		assertEquals(1, new Rational(22, 6).compareTo(new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666")));
		assertEquals(-1, new Rational(22, 6).compareTo(new BigDecimal("3.6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666667")));
		assertTrue(new Rational(22, 6).equalsExact(new Rational(22, 6)));
		assertFalse(new Rational(22, 6).equalsExact(new Rational(11, 3)));
	}

	@Test
	public void testGetMultipliers() {
		Rational a = new Rational(22, 6);
		Rational b = new Rational(-5, 34);
		BigInteger[] array = a.getMultipliers(b);
		assertArrayEquals(new BigInteger[] {BigInteger.valueOf(34), BigInteger.valueOf(3)}, array);
		assertEquals(a.reducedDenominator.multiply(array[0]), b.reducedDenominator.multiply(array[1]));

		a = new Rational(4.235);
		b = new Rational(78, -14);
		array = a.getMultipliers(b);
		assertArrayEquals(new BigInteger[] {BigInteger.valueOf(7), BigInteger.valueOf(200)}, array);
		assertEquals(a.reducedDenominator.multiply(array[0]), b.reducedDenominator.multiply(array[1]));
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
