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

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an immutable class for holding a rational without loss of precision.
 * As a consequence, a new instance is generated for every mathematical
 * operation. The reduced rationale and most string values are generated during
 * construction and cached for later retrieval.
 * <p>
 * This class has methods for doing basic operations mathematical operations,
 * and constructors for various number formats. This class implements
 * {@link Number} for easy conversion to other number formats, and
 * {@link Comparable} so two values can be compared and the values can be
 * sorted. In addition to {@link Comparable#compareTo}, there are
 * {@link #compareTo} methods that accepts most primitive number types and any
 * class implementing {@link Number}.
 * <p>
 * All the {@link #compareTo} can be used as a "replacement" for the six boolean comparison operators ({@literal <}, ==, {@literal >},
 * {@literal >=}, !=, {@literal <=}) since these are not supported for "custom" number types.
 * The suggested idiom for performing
 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
 * operators.
 * <p>
 * Static methods for finding the greatest common divisor and the least common
 * multiple for two integers are also provided with
 * {@link #getGreatestCommonDivisor} and {@link #getLeastCommonMultiple}.
 *
 * This class is "inspired" by the following:
 * <ul>
 * <li>http://introcs.cs.princeton.edu/java/32class/Rational.java.html</li>
 * <li>com.drew.lang.Rational</li>
 * <li>org.apache.commons.lang.math.Fraction</li>
 * <li>com.twelvemonkeys.imageio.metadata.exif.Rational</li>
 * <li>java.math.BigInteger</li>
 * <li>java.math.BigDecimal</li>
 * <li>java.math.MutableBigInteger</li>
 * </ul>
 *
 * @author Nadahar
 */
public class Rational extends Number implements Comparable<Rational> {

	/** The static instance representing the value 0 */
	public static final Rational ZERO = new Rational();

	/** The static instance representing the value 1 */
	public static final Rational ONE = new Rational(1L);

	/** Internal static string for 0 */
	protected static final String STRING_ZERO = "0";

	/** Internal static string for 1 */
	protected static final String STRING_ONE = "1";

	/** Internal regex pattern for validation of strings for parsing */
	protected static final Pattern STRING_PATTERN = Pattern.compile("^\\s*(-?\\d+)\\s*(?:/\\s*(-?\\d+))?\\s*$");

	/**
	 * The locale-insensitive {@link DecimalFormat} used for
	 * {@link #toDecimalString} and {@link #toDebugString}.
	 */
	protected static final DecimalFormat DECIMALFORMAT = new DecimalFormat(
		"#0.####################",
		DecimalFormatSymbols.getInstance(Locale.ROOT)
	);

	private static final long serialVersionUID = 1L;

	/** The numerator, which also holds the sign of this {@link Rational}. */
	protected final BigInteger numerator;

	/** The denominator, which is positive by definition. */
	protected final BigInteger denominator;

	/**
	 * The numerator of the reduced {@link Rational}, which also holds the sign.
	 */
	protected final BigInteger reducedNumerator;

	/** The denominator of the reduced {@link Rational}, always positive. */
	protected final BigInteger reducedDenominator;

	/**
	 * The greatest common divisor of {@code numerator} and {@code denominator}.
	 * This is the number by which {@code numerator} and {@code denominator} are
	 * divided to produce {@code reducedNumerator} and {@code reducedDenominator}
	 */
	protected final BigInteger greatestCommonDivisor;

	/** The cached string value */
	protected final String stringValue;

	/** The cached reduced string value */
	protected final String reducedStringValue;

	/** The cached decimal string value */
	protected final String decimalStringValue;

	/** The cached hashCode */
	protected final int hashCode;

	/**
	 * Creates a new instance that represents the value zero. Use {@link #ZERO}
	 * instead.
	 */
	protected Rational() {
		numerator = BigInteger.ZERO;
		denominator = BigInteger.ONE;
		greatestCommonDivisor = BigInteger.ONE;
		reducedNumerator = numerator;
		reducedDenominator = denominator;
		stringValue = STRING_ZERO;
		reducedStringValue = STRING_ZERO;
		decimalStringValue = STRING_ZERO;
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 */
	public Rational(double value) {
		this(BigDecimal.valueOf(value));
	}

	/**
	 * Creates a new instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 */
	public Rational(BigDecimal value) {
		if (value.signum() == 0) {
			this.numerator = BigInteger.ZERO;
			denominator = BigInteger.ONE;
		} else {
			if (value.scale() > 0) {
				BigInteger unscaled = value.unscaledValue();
				BigInteger tmpDenominator = BigInteger.TEN.pow(value.scale());
				BigInteger tmpGreatestCommonDivisor = unscaled.gcd(tmpDenominator);
				numerator = unscaled.divide(tmpGreatestCommonDivisor);
				denominator = tmpDenominator.divide(tmpGreatestCommonDivisor);
			} else {
				numerator = value.toBigIntegerExact();
				denominator = BigInteger.ONE;
			}
		}
		greatestCommonDivisor = BigInteger.ONE;
		reducedNumerator = numerator;
		reducedDenominator = denominator;
		stringValue = generateRationalString(numerator, denominator);
		reducedStringValue = stringValue;
		decimalStringValue = generateDecimalString(numerator, denominator);
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance by parsing {@code value}. The format must be
	 * either {@code integer numerator/integer denominator} or
	 * {@code integer numerator} for the parsing to succeed. Signs are
	 * understood for both numerator and denominator. If {@code value} is blank
	 * or {@code null}, a {@link Rational} instance representing zero is
	 * created. If {@code value} can't be parsed, a
	 * {@link NumberFormatException} is thrown.
	 *
	 * @param value the {@link String} value to parse.
	 * @throws NumberFormatException If {@code value} cannot be parsed.
	 */
	public Rational(String value) {
		if (isBlank(value)) {
			numerator = BigInteger.ZERO;
			denominator = BigInteger.ONE;
			greatestCommonDivisor = BigInteger.ONE;
			reducedNumerator = numerator;
			reducedDenominator = denominator;
			stringValue = STRING_ZERO;
			reducedStringValue = STRING_ZERO;
			decimalStringValue = STRING_ZERO;
		} else {
			Matcher matcher = STRING_PATTERN.matcher(value);
			if (!matcher.find()) {
				throw new NumberFormatException(
					"Invalid value \"" + value +
					"\". The value must be either \"integer/integer\" or \"integer\""
				);
			}
			if (value.indexOf("/") > 0) {
				if (matcher.group(2).startsWith("-")) {
					// Keep the signum in the numerator
					numerator = new BigInteger(matcher.group(1)).negate();
					denominator = new BigInteger(matcher.group(2)).negate();
				} else {
					numerator = new BigInteger(matcher.group(1));
					denominator = new BigInteger(matcher.group(2));
				}
				greatestCommonDivisor = calculateGreatestCommonDivisor(numerator, denominator);
				reducedNumerator = numerator.divide(greatestCommonDivisor);
				reducedDenominator = denominator.divide(greatestCommonDivisor);
				stringValue = generateRationalString(numerator, denominator);
				reducedStringValue = generateRationalString(reducedNumerator, reducedDenominator);
			} else {
				numerator = new BigInteger(matcher.group(1));
				denominator = BigInteger.ONE;
				greatestCommonDivisor = BigInteger.ONE;
				reducedNumerator = numerator;
				reducedDenominator = denominator;
				stringValue = generateRationalString(numerator, denominator);
				reducedStringValue = stringValue;
			}
			decimalStringValue = generateDecimalString(reducedNumerator, reducedDenominator);
		}
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 */
	public Rational(int value) {
		this((long) value);
	}

	/**
	 * Creates a new instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 */
	public Rational(long value) {
		numerator = BigInteger.valueOf(value);
		denominator = BigInteger.ONE;
		greatestCommonDivisor = BigInteger.ONE;
		reducedNumerator = numerator;
		reducedDenominator = denominator;
		stringValue = Long.toString(value);
		reducedStringValue = stringValue;
		decimalStringValue = stringValue;
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 */
	public Rational(BigInteger value) {
		numerator = value;
		denominator = BigInteger.ONE;
		greatestCommonDivisor = BigInteger.ONE;
		reducedNumerator = numerator;
		reducedDenominator = denominator;
		stringValue = generateRationalString(numerator, denominator);
		reducedStringValue = stringValue;
		decimalStringValue = stringValue;
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 */
	public Rational(int numerator, int denominator) {
		this((long) numerator, (long) denominator);
	}

	/**
	 * Creates a new instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 */
	public Rational(long numerator, long denominator) {
		if (denominator == 0) {
			throw new IllegalArgumentException("denominator can't be zero");
		}
		if (numerator == 0) {
			this.numerator = BigInteger.ZERO;
			this.denominator = BigInteger.ONE;
		} else if (denominator < 0) {
			this.numerator = BigInteger.valueOf(-numerator);
			this.denominator = BigInteger.valueOf(-denominator);
		} else {
			this.numerator = BigInteger.valueOf(numerator);
			this.denominator = BigInteger.valueOf(denominator);
		}
		stringValue = generateRationalString(this.numerator, this.denominator);
		long gcd = calculateGreatestCommonDivisor(numerator, denominator);
		this.greatestCommonDivisor = BigInteger.valueOf(gcd);
		if (gcd == 1) {
			this.reducedNumerator = this.numerator;
			this.reducedDenominator = this.denominator;
			reducedStringValue = stringValue;
		} else {
			this.reducedNumerator = this.numerator.divide(this.greatestCommonDivisor);
			this.reducedDenominator = this.denominator.divide(this.greatestCommonDivisor);
			reducedStringValue = generateRationalString(this.reducedNumerator, this.reducedDenominator);
		}
		decimalStringValue = generateDecimalString(this.reducedNumerator, this.reducedDenominator);
		hashCode = calculateHashCode();
	}

	/**
	 * Creates a new instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 */
	public Rational(BigInteger numerator, BigInteger denominator) {
		if (denominator == null || denominator.signum() == 0) {
			throw new IllegalArgumentException("denominator can't be zero");
		}
		if (numerator == null || numerator.signum() == 0) {
			this.numerator = BigInteger.ZERO;
			this.denominator = BigInteger.ONE;
		} else if (denominator.signum() < 0) {
			this.numerator = numerator.negate();
			this.denominator = denominator.negate();
		} else {
			this.numerator = numerator;
			this.denominator = denominator;
		}
		stringValue = generateRationalString(this.numerator, this.denominator);

		this.greatestCommonDivisor = calculateGreatestCommonDivisor(this.numerator, this.denominator);
		if (BigInteger.ONE.equals(this.greatestCommonDivisor)) {
			this.reducedNumerator = this.numerator;
			this.reducedDenominator = this.denominator;
			reducedStringValue = stringValue;
		} else {
			this.reducedNumerator = this.numerator.divide(this.greatestCommonDivisor);
			this.reducedDenominator = this.denominator.divide(this.greatestCommonDivisor);
			reducedStringValue = generateRationalString(this.reducedNumerator, this.reducedDenominator);
		}
		decimalStringValue = generateDecimalString(this.reducedNumerator, this.reducedDenominator);
		hashCode = calculateHashCode();
	}


	// Operations


	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(int value) {
		return multiply(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(long value) {
		return multiply(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(BigInteger value) {
		if (value == null || value.signum() == 0) {
			return ZERO;
		}
		if (BigInteger.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		return new Rational(reducedNumerator.multiply(value), reducedDenominator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(double value) {
		return multiply(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(BigDecimal value) {
		return multiply(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	public Rational multiply(Rational value) {
		if (value == null || value.numerator.signum() == 0) {
			return ZERO;
		}
		if (value.numerator.equals(value.denominator)) {
			return value.numerator.signum() < 0 ? this.negate() : this;
		}

		BigInteger newNumerator = reducedNumerator.multiply(value.reducedNumerator);
		BigInteger newDenominator = reducedDenominator.multiply(value.reducedDenominator);
		BigInteger gcd = newNumerator.gcd(newDenominator);
		return new Rational(newNumerator.divide(gcd), newDenominator.divide(gcd));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(int value) {
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(long value) {
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(BigInteger value) {
		return add(value.negate());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(double value) {
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(BigDecimal value) {
		return add(value.negate());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	public Rational subtract(Rational value) {
		if (value == null || value.numerator.signum() == 0) {
			return this;
		}
		return add(value.negate());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(int value) {
		return add((long) value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(long value) {
		return add(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(BigInteger value) {
		if (value == null || value.signum() == 0) {
			return this;
		}
		if (BigInteger.ONE.equals(denominator)) {
			return new Rational(numerator.add(value), denominator);
		}
		return new Rational(numerator.add(value.multiply(denominator)), denominator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(double value) {
		return add(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(BigDecimal value) {
		return add(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	public Rational add(Rational value) {
		if (value == null || value.numerator.signum() == 0) {
			return this;
		}
		if (this.numerator.signum() == 0) {
			return value;
		}
		BigInteger lcm = calculateLeastCommonMultiple(denominator, value.denominator);
		return new Rational(numerator.multiply(lcm.divide(denominator)).add(
			value.numerator.multiply(lcm.divide(value.denominator))), lcm);
	}

	/**
	 * Returns a {@link Rational} whose value is the reciprocal of this
	 * {@code (1 / this)}.
	 *
	 * @return The reciprocal result.
	 */
	public Rational reciprocal() {
		return numerator.signum() == 0 ? this : new Rational(denominator, numerator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(int value) {
		return divide((long) value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(long value) {
		if (value == 0) {
			throw new IllegalArgumentException("value cannot be zero/divison by zero");
		}
		if (numerator.signum() == 0 || value == 1) {
			return this;
		}
		if (value == -1) {
			return negate();
		}

		// Keep the sign in the numerator and the denominator positive
		if (value < 0) {
			return new Rational(reducedNumerator.negate(), reducedDenominator.multiply(BigInteger.valueOf(-value)));
		}
		return new Rational(reducedNumerator, reducedDenominator.multiply(BigInteger.valueOf(value)));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(BigInteger value) {
		if (value == null || value.signum() == 0) {
			throw new IllegalArgumentException("value cannot be zero/divison by zero");
		}
		if (numerator.signum() == 0) {
			return this;
		}
		if (BigInteger.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		// Keep the sign in the numerator and the denominator positive
		if (value.signum() < 0) {
			return new Rational(reducedNumerator.negate(), reducedDenominator.multiply(value.negate()));
		}
		return new Rational(reducedNumerator, reducedDenominator.multiply(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(double value) {
		if (value == 0) {
			throw new IllegalArgumentException("value cannot be zero/divison by zero");
		}
		if (numerator.signum() == 0 || value == 1) {
			return this;
		}
		if (value == -1) {
			return negate();
		}
		return divide(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(BigDecimal value) {
		if (value == null || value.signum() == 0) {
			throw new IllegalArgumentException("value cannot be zero/divison by zero");
		}
		if (numerator.signum() == 0 ||  BigDecimal.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}
		return divide(new Rational(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws IllegalArgumentException if {@code value} is zero.
	 */
	public Rational divide(Rational value) {
		if (value == null || value.numerator.signum() == 0) {
			throw new IllegalArgumentException("value cannot be zero/divison by zero");
		}
		return numerator.signum() == 0 ? this : multiply(value.reciprocal());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (-this)}.
	 *
	 * @return The negated result.
	 */
	public Rational negate() {
		if (numerator.signum() == 0) {
			return this;
		}
		return new Rational(numerator.negate(), denominator);
	}

	/**
	 * Returns a {@link Rational} whose value is the absolute value of this
	 * {@code (abs(this))}.
	 *
	 * @return The absolute result.
	 */
	public Rational abs() {
		return numerator.signum() < 0 ? new Rational(numerator.negate(), denominator) : this;
	}

	/**
	 * Returns a {@link Rational} whose value is
	 * <tt>(this<sup>exponent</sup>)</tt>.
	 *
	 * @param exponent exponent to which this {@link Rational} is to be raised.
	 * @return <tt>this<sup>exponent</sup></tt>
	 */
	public Rational pow(int exponent) {
		if (exponent == 0) {
			return ONE;
		}
		if (exponent == 1) {
			return this;
		}
		if (exponent < 0) {
			if (exponent == Integer.MIN_VALUE) {
				return this.reciprocal().pow(2).pow(-(exponent / 2));
			}
			return this.reciprocal().pow(-exponent);
		}
		Rational result = multiply(this);
		if ((exponent % 2) == 0) {
			return result.pow(exponent / 2);
		}
		return result.pow(exponent / 2).multiply(this);
	}

	/**
	 * Returns the signum function of this {@link Rational}.
	 *
	 * @return -1, 0 or 1 as the value of this {@link Rational} is negative,
	 *         zero or positive.
	 */
	public int signum() {
		return numerator.signum();
	}

	/**
	 * @return Whether the value of this {@link Rational} can be expressed as an
	 *         integer value.
	 */
	public boolean isInteger() {
		return BigInteger.ONE.equals(reducedDenominator);
	}


	// Getters


	/**
	 * @return The numerator.
	 */
	public BigInteger getNumerator() {
		return numerator;
	}

	/**
	 * @return The denominator.
	 */
	public BigInteger getDenominator() {
		return denominator;
	}

	/**
	 * The reduced numerator is the numerator divided by
	 * {@link #getGreatestCommonDivisor}.
	 *
	 * @return The reduced numerator.
	 */
	public BigInteger getReducedNumerator() {
		return reducedNumerator;
	}

	/**
	 * The reduced denominator is the denominator divided by
	 * {@link #getGreatestCommonDivisor}.
	 *
	 * @return The reduced denominator.
	 */
	public BigInteger getReducedDenominator() {
		return reducedDenominator;
	}

	/**
	 * @return the greatest common divisor of the numerator and the denominator.
	 */
	public BigInteger getGreatestCommonDivisor() {
		return greatestCommonDivisor;
	}


	// String methods


	/**
	 * Returns a string representation of this {@link Rational} in it's rational
	 * form {@code (1/2, 4 or 16/9)}.
	 *
	 * @return The {@link String} representation.
	 */
	@Override
	public String toString() {
		return stringValue;
	}


	/**
	 * Returns a string representation of this {@link Rational} in it's reduced
	 * rational form {@code (1/2, 4 or 16/9)}. The reduced form is when both
	 * numerator and denominator have been divided by the greatest common
	 * divisor.
	 *
	 * @return The reduced {@link String} representation.
	 */
	public String toReducedString() {
		return reducedStringValue;
	}

	/**
	 * Returns a decimal representation of this {@link Rational}. The decimal
	 * representation is limited to 20 decimals using
	 * {@link RoundingMode#HALF_EVEN} and is formatted with {@link Locale#ROOT}
	 * without grouping.
	 *
	 * @return The decimal {@link String} representation.
	 */
	public String toDecimalString() {
		return decimalStringValue;
	}

	/**
	 * Returns a debug string representation of this {@link Rational}. The debug
	 * representation is a combination of {@link #toString},
	 * {@link #toReducedString} and {@link #toDecimalString}.
	 *
	 * @return The debug {@link String} representation.
	 */
	public String toDebugString() {
		StringBuilder sb = new StringBuilder("Value: ");
		sb.append(stringValue).append(", Reduced: ").append(reducedStringValue).append(", Decimal: ").append(decimalStringValue);
		return sb.toString();
	}

	/**
	 * Returns a hexadecimal string representation of this {@link Rational} in
	 * it's rational form {@code (a/2, ff or 16/c)}.
	 *
	 * @return The hexadecimal {@link String} representation.
	 */
	public String toHexString() {
		return generateRationalHexString(numerator, denominator);
	}

	/**
	 * Returns a hexadecimal string representation of this {@link Rational} in it's reduced
	 * rational form {@code (5, ff or 16/c)}. The reduced form is when both
	 * numerator and denominator have been divided by the greatest common
	 * divisor.
	 *
	 * @return The reduced hexadecimal {@link String} representation.
	 */
	public String toReducedHexString() {
		return generateRationalHexString(reducedNumerator, reducedDenominator);
	}


	// Conversion to other Number formats


	/**
	 * Converts this {@link Rational} to an {@code int}. This conversion is
	 * analogous to the <i>narrowing primitive conversion</i> from
	 * {@code double} to {@code int} as defined in section 5.1.3 of <cite>The
	 * Java&trade; Language Specification</cite>: any fractional part of this
	 * {@link Rational} will be discarded, and if the resulting "
	 * {@link BigInteger}" is too big to fit in an {@code int}, only the
	 * low-order 32 bits are returned.
	 * <p>
	 * Note that this conversion can lose information about the overall
	 * magnitude and precision of this {@link Rational} value as well as return
	 * a result with the opposite sign.
	 *
	 * @return This {@link Rational} converted to an {@code int}.
	 */
	@Override
	public int intValue() {
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), RoundingMode.DOWN).intValue();
	}

	/**
	 *
	 * Converts this {@link Rational} to a {@code long}. This conversion is
	 * analogous to the <i>narrowing primitive conversion</i> from
	 * {@code double} to {@code long} as defined in section 5.1.3 of <cite>The
	 * Java&trade; Language Specification</cite>: any fractional part of this
	 * {@link Rational} will be discarded, and if the resulting "
	 * {@link BigInteger}" is too big to fit in a {@code long}, only the
	 * low-order 64 bits are returned.
	 * <p>
	 * Note that this conversion can lose information about the overall
	 * magnitude and precision of this {@link Rational} value as well as return
	 * a result with the opposite sign.
	 *
	 * @return This {@link Rational} converted to a {@code long}.
	 */
	@Override
	public long longValue() {
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), RoundingMode.DOWN).longValue();
	}

	/**
	 * Converts this {@link Rational} to a {@link BigInteger}. This conversion
	 * is analogous to the <i>narrowing primitive conversion</i> from
	 * {@code double} to {@code long} as defined in section 5.1.3 of <cite>The
	 * Java&trade; Language Specification</cite>: any fractional part of this
	 * {@link Rational} will be discarded.
	 *
	 * @return This {@link Rational} converted to a {@link BigInteger}.
	 */
	public BigInteger bigIntegerValue() {
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), RoundingMode.DOWN).toBigInteger();
	}

	/**
	 * Converts this {@link Rational} to a {@code float}. This conversion is
	 * similar to the <i>narrowing primitive conversion</i> from {@code double}
	 * to {@code float} as defined in section 5.1.3 of <cite>The Java&trade;
	 * Language Specification</cite>: if this {@link Rational} has too great a
	 * magnitude to represent as a {@code float}, it will be converted to
	 * {@link Float#NEGATIVE_INFINITY} or {@link Float#POSITIVE_INFINITY} as
	 * appropriate.
	 * <p>
	 * Note that even when the return value is finite, this conversion can lose
	 * information about the precision of the {@link Rational} value.
	 *
	 * @return This {@link Rational} converted to a {@code float}.
	 */
	@Override
	public float floatValue() {
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), MathContext.DECIMAL32).floatValue();
	}

	/**
	 * Converts this {@link Rational} to a {@code double}. This conversion is
	 * similar to the <i>narrowing primitive conversion</i> from {@code double}
	 * to {@code float} as defined in section 5.1.3 of <cite>The Java&trade;
	 * Language Specification</cite>: if this {@link Rational} has too great a
	 * magnitude to represent as a {@code double}, it will be converted to
	 * {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY} as
	 * appropriate.
	 * <p>
	 * Note that even when the return value is finite, this conversion can lose
	 * information about the precision of the {@link Rational} value.
	 *
	 * @return This {@link Rational} converted to a {@code double}.
	 */
	@Override
	public double doubleValue() {
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), MathContext.DECIMAL64).doubleValue();
	}

	/**
	 * Converts this {@link Rational} to a {@link BigDecimal}. This may involve
	 * rounding. The conversion is limited to 100 decimals and uses
	 * {@link RoundingMode#HALF_EVEN}.
	 * <p>
	 * For explicit control over the conversion, use one of the overloaded
	 * methods.
	 *
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(RoundingMode)
	 * @see #bigDecimalValue(int, RoundingMode)
	 *
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 */
	public BigDecimal bigDecimalValue() {
		if (BigInteger.ONE.equals(reducedDenominator)) {
			return new BigDecimal(reducedNumerator);
		}
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), 100, RoundingMode.HALF_EVEN);
	}

	/**
	 * Converts this {@link Rational} to a {@link BigDecimal}. This may involve
	 * rounding. The conversion is limited to 100 decimals and uses
	 * {@code roundingMode}.
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(int, RoundingMode)
	 *
	 * @param roundingMode the {@link RoundingMode} to apply.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 */
	public BigDecimal bigDecimalValue(RoundingMode roundingMode) {
		if (BigInteger.ONE.equals(reducedDenominator)) {
			return new BigDecimal(reducedNumerator);
		}
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), 100, roundingMode);
	}

	/**
	 * Converts this {@link Rational} to a {@link BigDecimal}. This may involve
	 * rounding.
	 * <p>
	 * Use {@code scale == 0} and
	 * {@code roundingMode == RoundingMode.UNNECESSARY} to achieve absolute
	 * precision. This will throw an {@link ArithmeticException} if the exact
	 * quotient cannot be represented (because it has a non-terminating decimal
	 * expansion).
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(RoundingMode)
	 *
	 * @param scale the scale of the {@link BigDecimal} quotient to be returned.
	 * @param roundingMode the {@link RoundingMode} to apply.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If
	 *             {@code roundingMode == RoundingMode.UNNECESSARY} and the
	 *             specified scale is insufficient to represent the result of
	 *             the division exactly.
	 */
	public BigDecimal bigDecimalValue(int scale, RoundingMode roundingMode) {
		if (BigInteger.ONE.equals(reducedDenominator)) {
			return new BigDecimal(reducedNumerator);
		}
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), scale, roundingMode);
	}

	/**
	 * Converts this {@link Rational} to a {@link BigDecimal} using the given
	 * {@link MathContext}. This may involve rounding.
	 * <p>
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(RoundingMode)
	 * @see #bigDecimalValue(int, RoundingMode)
	 *
	 * @param mathContext the {@link MathContext} to use.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If the result is inexact but the rounding
	 *             mode is {@code UNNECESSARY} or
	 *             {@code mathContext.precision == 0} and the quotient has a
	 *             non-terminating decimal expansion.
	 */
	public BigDecimal bigDecimalValue(MathContext mathContext) {
		if (BigInteger.ONE.equals(reducedDenominator)) {
			return new BigDecimal(reducedNumerator);
		}
		return new BigDecimal(reducedNumerator).divide(new BigDecimal(reducedDenominator), mathContext);
	}


	// Comparison methods


	/**
	 * Compares this {@link Rational} with {@code other}. Two {@link Rational}
	 * instances that are equal in value but have different numerators and
	 * denominators are considered equal by this methods (like {@code 1/2} and
	 * {@code 2/4}).
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param other the {@link Rational} to which this {@link Rational} is to be
	 *            compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code other}.
	 */
	@Override
	public int compareTo(Rational other) {
		if (signum() != other.signum()) {
			return signum() - other.signum();
		}
		BigInteger[] multipliers = getMultipliers(other);
		return reducedNumerator.multiply(multipliers[0]).compareTo(other.reducedNumerator.multiply(multipliers[1]));
	}

	/**
	 * Compares this {@link Rational} by value with a {@code int}.
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param value the {@code int} to which this {@link Rational}'s value is to
	 *            be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code value}.
	 */
	public int compareTo(int value) {
		return compareTo(Integer.valueOf(value));
	}

	/**
	 * Compares this {@link Rational} by value with a {@code long}.
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param value the {@code long} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code value}.
	 */
	public int compareTo(long value) {
		return compareTo(Long.valueOf(value));
	}

	/**
	 * Compares this {@link Rational} by value with a {@code float}.
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param value the {@code float} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code value}.
	 */
	public int compareTo(float value) {
		return compareTo(Float.valueOf(value));
	}

	/**
	 * Compares this {@link Rational} by value with a {@code double}.
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param value the {@code double} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code value}.
	 */
	public int compareTo(double value) {
		return compareTo(Double.valueOf(value));
	}

	/**
	 * Compares this {@link Rational} by value with any class implementing
	 * {@link Number}.
	 * <p>
	 * This method is provided in preference to individual methods for each of
	 * the six boolean comparison operators ({@literal <}, ==, {@literal >},
	 * {@literal >=}, !=, {@literal <=}). The suggested idiom for performing
	 * these comparisons is: {@code (x.compareTo(y)} &lt;<i>op</i>&gt;
	 * {@code 0)}, where &lt;<i>op</i>&gt; is one of the six comparison
	 * operators.
	 *
	 * @param number the {@link Number} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code number}.
	 */
	public int compareTo(Number number) {
		// List known integer types for faster and more accurate comparison
		if (number instanceof BigInteger) {
			if (isInteger()) {
				return bigIntegerValue().compareTo((BigInteger) number);
			}
			return bigDecimalValue(2, RoundingMode.HALF_EVEN).compareTo(new BigDecimal((BigInteger) number));
		}
		if (
			number instanceof AtomicInteger ||
			number instanceof AtomicLong ||
			number instanceof Byte ||
			number instanceof Integer ||
			number instanceof Long ||
			number instanceof Short
		) {
			if (isInteger()) {
				return bigIntegerValue().compareTo(BigInteger.valueOf(number.longValue()));
			}
			return bigDecimalValue(2, RoundingMode.HALF_EVEN).compareTo(new BigDecimal(number.longValue()));
		}
		if (number instanceof BigDecimal) {
			Rational other = new Rational((BigDecimal) number);
			return compareTo(other);
		}
		return bigDecimalValue().compareTo(new BigDecimal(number.doubleValue()));
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Used internally to calculate the {@code hashCode} for caching.
	 *
	 * @return The calculated {@code hashCode}.
	 */
	protected int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((reducedDenominator == null) ? 0 : reducedDenominator.hashCode());
		result = prime * result + ((reducedNumerator == null) ? 0 : reducedNumerator.hashCode());
		return result;
	}


	/**
	 * Indicates whether this instance and {@code object} are mathematically
	 * equivalent, given that {@code other} implements {@link Number}.
	 * <p>
	 * 1/2 and 4/8 are considered equal by this method, as are 4/2 and 2.
	 * <p>
	 * Equality is determined by equality of {@code reducedNumerator} and
	 * {@code reducedDenominator} if {@code object} is another {@link Rational}.
	 * If not, but {@code object} implements {@link Number},
	 * {@code compareTo(Number) == 0} is used to determine equality.
	 * <p>
	 * To test equal representation, use {@link #equalsExact}.
	 *
	 * @param object the reference object with which to compare.
	 * @return {@code true} if {@code object} is a {@link Rational} and are
	 *         mathematically equivalent, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof Number)) {
			return false;
		}
		if (object instanceof Rational) {
			Rational other = (Rational) object;
			if (reducedDenominator == null) {
				if (other.reducedDenominator != null) {
					return false;
				}
			} else if (!reducedDenominator.equals(other.reducedDenominator)) {
				return false;
			}
			if (reducedNumerator == null) {
				if (other.reducedNumerator != null) {
					return false;
				}
			} else if (!reducedNumerator.equals(other.reducedNumerator)) {
				return false;
			}
		} else {
			if (reducedNumerator == null || reducedDenominator == null) {
				// Should be impossible
				return false;
			}
			return compareTo((Number) object) == 0;
		}
		return true;
	}

	/**
	 * Indicates whether this instance and {@code other} have identical
	 * numerator and denominator.
	 * <p>
	 * 1/2, 4/8, 4/2 and 2 are all considered non-equal by this method.
	 * <p>
	 * To test mathematically equivalence, use {@link #equals}.
	 *
	 * @param other the {@link Rational} with which to compare.
	 * @return {@code true} if this instance and {@code other} have an identical
	 *         representation.
	 */
	public boolean equalsExact(Rational other) {
		return other != null && numerator.equals(other.numerator) && denominator.equals(other.denominator);
	}

	/**
	 * Used internally to find by which factor to multiply the reduced
	 * numerators when comparing two {@link Rational}s.
	 *
	 * @param other the {@link Rational} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return An array of {@link BigInteger} multipliers.
	 */
	protected BigInteger[] getMultipliers(Rational other) {
		BigInteger[] result = new BigInteger[2];
		BigInteger lcm = calculateLeastCommonMultiple(reducedDenominator, other.reducedDenominator);
		result[0] = lcm.divide(reducedDenominator);
		result[1] = lcm.divide(other.reducedDenominator);
		return result;
	}


	// Static methods


	/**
	 * Calculates the greatest common divisor for two {@link Long}s using
	 * "binary GDC" with some optimizations borrowed from
	 * {@link org.apache.commons.lang.math.Fraction#greatestCommonDivisor}.
	 *
	 * @param u the first number.
	 * @param v the second number.
	 * @return The GDC, always 1 or greater.
	 */
	public static long calculateGreatestCommonDivisor(long u, long v) {
		if (Math.abs(u) <= 1 || Math.abs(v) <= 1) {
			return 1;
		}
		// keep u and v negative, as negative integers range down to
		// -2^63, while positive numbers can only be as large as 2^63-1.
		if (u > 0) {
			u = -u;
		}
		if (v > 0) {
			v = -v;
		}
		int k = 0;
		while ((u & 1) == 0 && (v & 1) == 0 && k < 63) {
			u >>= 1;
			v >>= 1;
			k++;
		}
		if (k == 63) {
			throw new ArithmeticException("Overflow: gcd is 2^63");
		}
		long t = ((u & 1) == 1) ? v : -(u >> 1);
		do {
			while ((t & 1) == 0) {
				t >>= 1;
			}
			if (t > 0) {
				u = -t;
			} else {
				v = t;
			}
			t = (v - u) >> 1;
		} while (t != 0);

		return -u * (1 << k);
	}

	/**
	 * Calculates the greatest common divisor for two {@link BigInteger}s using
	 * {@link BigInteger#gcd}.
	 *
	 * @param u the first number.
	 * @param v the second number.
	 * @return The GDC, always 1 or greater.
	 */
	public static BigInteger calculateGreatestCommonDivisor(BigInteger u, BigInteger v) {
		if (u.abs().compareTo(BigInteger.ONE) <= 0 || v.abs().compareTo(BigInteger.ONE) <= 0) {
			return BigInteger.ONE;
		}
		return u.gcd(v);
	}

	/**
	 * Calculates the least common multiple for two {@link BigInteger}s using the formula
	 * {@code u * v / gcd(u, v)} where {@code gcd} is the greatest common divisor for the two.
	 *
	 * @param u the first number.
	 * @param v the second number.
	 * @return The LCM, always 1 or greater.
	 */
	public static BigInteger calculateLeastCommonMultiple(BigInteger u, BigInteger v) {
		if (u == null) {
			u = BigInteger.ZERO;
		}
		if (v == null) {
			v = BigInteger.ZERO;
		}
		if (u.signum() == 0 && v.signum() == 0) {
			return BigInteger.ONE;
		}
		u = u.abs();
		v = v.abs();
		if (u.signum() == 0) {
			return v;
		}
		if (v.signum() == 0) {
			return u;
		}
		return u.divide(calculateGreatestCommonDivisor(u, v)).multiply(v);
	}

	/**
	 * Used internally to generate a hexadecimal rational string representation
	 * from two {@link BigInteger}s.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return The hexadecimal rational string representation.
	 */
	protected static String generateRationalHexString(BigInteger numerator, BigInteger denominator) {
		if (BigInteger.ONE.equals(denominator)) {
			return numerator.toString(16);
		}
		return numerator.toString(16) + "/" + denominator.toString(16);
	}

	/**
	 * Used internally to generate a rational string representation from two
	 * {@link BigInteger}s.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return The rational string representation.
	 */
	protected static String generateRationalString(BigInteger numerator, BigInteger denominator) {
		if (BigInteger.ONE.equals(denominator)) {
			return numerator.toString();
		}
		return numerator.toString() + "/" + denominator.toString();
	}

	/**
	 * Used internally to generate a decimal string representation from two
	 * {@link BigInteger}s. The decimal representation is limited to 20
	 * decimals.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return The string representation.
	 */
	protected static String generateDecimalString(BigInteger numerator, BigInteger denominator) {
		if (BigInteger.ONE.equals(denominator)) {
			return numerator.toString();
		}
		BigDecimal decimalValue = new BigDecimal(numerator).divide(new BigDecimal(denominator), 20, RoundingMode.HALF_EVEN);
		return DECIMALFORMAT.format(decimalValue);
	}
}
