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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

/**
 * This is an immutable class for holding a rational number without loss of
 * precision. As a consequence, a new instance is generated for every
 * mathematical operation. The reduced rationale is generated during
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
 * All the {@link #compareTo} methods can be used as a "replacement" for the six
 * boolean comparison operators ({@literal <}, ==, {@literal >}, {@literal >=},
 * !=, {@literal <=}) since these are not supported for "custom" number types.
 * The suggested idiom for performing these comparisons is:
 * {@code (x.compareTo(y)} &lt;<i>op</i>&gt; {@code 0)}, where &lt;<i>op</i>&gt;
 * is one of the six comparison operators.
 * <p>
 * {@link Rational} supports {@link #NaN} (Not a Number) and positive and
 * negative infinity. Arithmetic operations involving these special values
 * follow the rules defined in <cite>IEEE Standard 754 Floating Point
 * Numbers</cite> for "special operations".
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

	private static final long serialVersionUID = 2L;

	/**
	 * The locale-insensitive {@link DecimalFormat} used for
	 * {@link #toDecimalString} and {@link #toDebugString}.
	 */
	protected static final DecimalFormat DECIMALFORMAT = new DecimalFormat(
		"#0.####################",
		DecimalFormatSymbols.getInstance(Locale.ROOT)
	);

	/** The static instance representing the value 0 */
	public static final Rational ZERO = new Rational(
		BigInteger.ZERO,
		BigInteger.ONE,
		BigInteger.ONE,
		BigInteger.ZERO,
		BigInteger.ONE
	);

	/** The static instance representing the value 1 */
	public static final Rational ONE = new Rational(
		BigInteger.ONE,
		BigInteger.ONE,
		BigInteger.ONE,
		BigInteger.ONE,
		BigInteger.ONE
	);

	/** The static instance representing positive infinity */
	public static final Rational POSITIVE_INFINITY = new Rational(
		BigInteger.ONE,
		BigInteger.ZERO,
		BigInteger.ZERO,
		BigInteger.ONE,
		BigInteger.ZERO
	);

	/** The static instance representing negative infinity */
	public static final Rational NEGATIVE_INFINITY = new Rational(
		BigInteger.ONE.negate(),
		BigInteger.ZERO,
		BigInteger.ZERO,
		BigInteger.ONE.negate(),
		BigInteger.ZERO
	);

	/** The static instance representing negative infinity */
	@SuppressWarnings("checkstyle:ConstantName")
	public static final Rational NaN = new Rational(
		BigInteger.ZERO,
		BigInteger.ZERO,
		BigInteger.ZERO,
		BigInteger.ZERO,
		BigInteger.ZERO
	);

	/** The numerator, which also holds the sign of this {@link Rational}. */
	protected final BigInteger numerator;

	/** The denominator, which is never negative by definition. */
	protected final BigInteger denominator;

	/**
	 * The numerator of the reduced {@link Rational}, which also holds the sign.
	 */
	protected final BigInteger reducedNumerator;

	/**
	 * The denominator of the reduced {@link Rational}, which is never negative
	 * by definition.
	 */
	protected final BigInteger reducedDenominator;

	/**
	 * The greatest common divisor of {@code numerator} and {@code denominator}.
	 * This is the number by which {@code numerator} and {@code denominator} are
	 * divided to produce {@code reducedNumerator} and {@code reducedDenominator}
	 */
	protected final BigInteger greatestCommonDivisor;

	/** The cached hashCode */
	protected final int hashCode;

	/**
	 * Creates a new instance with the specified parameters.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @param greatestCommonDivisor the greatest common divisor of numerator and
	 *            denominator.
	 * @param reducedNumerator the reduced numerator.
	 * @param reducedDenominator the reduced denominator.
	 */
	protected Rational(
		BigInteger numerator,
		BigInteger denominator,
		BigInteger greatestCommonDivisor,
		BigInteger reducedNumerator,
		BigInteger reducedDenominator
	) {
		this.numerator = numerator;
		this.denominator = denominator;
		this.greatestCommonDivisor = greatestCommonDivisor;
		this.reducedNumerator = reducedNumerator;
		this.reducedDenominator = reducedDenominator;
		hashCode = calculateHashCode();
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nonnull
	public static Rational valueOf(double value) {
		if (value == Double.POSITIVE_INFINITY) {
			return POSITIVE_INFINITY;
		}
		if (value == Double.NEGATIVE_INFINITY) {
			return NEGATIVE_INFINITY;
		}
		if (Double.isNaN(value)) {
			return NaN;
		}
		return valueOf(BigDecimal.valueOf(value));
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nonnull
	public static Rational valueOf(float value) {
		if (value == Float.POSITIVE_INFINITY) {
			return POSITIVE_INFINITY;
		}
		if (value == Float.NEGATIVE_INFINITY) {
			return NEGATIVE_INFINITY;
		}
		if (Float.isNaN(value)) {
			return NaN;
		}
		return valueOf(BigDecimal.valueOf(value));
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nullable
	public static Rational valueOf(@Nullable BigDecimal value) {
		if (value == null) {
			return null;
		}
		BigInteger numerator;
		BigInteger denominator;
		if (value.signum() == 0) {
			return ZERO;
		}
		if (BigDecimal.ONE.equals(value)) {
			return ONE;
		}
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
		return new Rational(
			numerator,
			denominator,
			BigInteger.ONE,
			numerator,
			denominator
		);
	}

	/**
	 * Returns an instance by parsing the specified {@link String}. The format
	 * must be either {@code number/number}, {@code number:number} or
	 * {@code number}. Signs are understood for both numerator and denominator.
	 * If {@code value} is blank or {@code null}, {@code null} is returned.
	 * "Standard formatted" numbers are expected (no grouping, {@code .} as
	 * decimal separator etc.). If {@code value} can't be parsed, a
	 * {@link NumberFormatException} is thrown.
	 *
	 * @param value the {@link String} value to parse.
	 * @return An instance that represents the value of {@code value}.
	 * @throws NumberFormatException If {@code value} cannot be parsed.
	 */
	@Nullable
	public static Rational valueOf(@Nullable String value) {
		return valueOf(value, (NumberFormat) null);
	}

	/**
	 * Returns an instance by parsing the specified {@link String}. The format
	 * must be either {@code number/number}, {@code number:number} or
	 * {@code number}. Signs are understood for both numerator and denominator.
	 * If {@code value} is blank or {@code null}, {@code null} is returned. If
	 * {@code value} can't be parsed, a {@link NumberFormatException} is thrown.
	 *
	 * @param value the {@link String} value to parse.
	 * @param locale the {@link Locale} to use when parsing numbers. If
	 *            {@code null}, "standard formatted" numbers are expected (no
	 *            grouping, {@code .} as decimal separator etc.).
	 * @return An instance that represents the value of {@code value}.
	 * @throws NumberFormatException If {@code value} cannot be parsed.
	 */
	@Nullable
	public static Rational valueOf(@Nullable String value, @Nullable Locale locale) {
		return valueOf(value, locale == null ? null : NumberFormat.getInstance(locale));
	}

	/**
	 * Returns an instance by parsing the specified {@link String}. The format
	 * must be either {@code number/number}, {@code number:number} or
	 * {@code number}. Signs are understood for both numerator and denominator.
	 * If {@code value} is blank or {@code null}, {@code null} is returned. If
	 * {@code value} can't be parsed, a {@link NumberFormatException} is thrown.
	 *
	 * @param value the {@link String} value to parse.
	 * @param numberFormat the {@link NumberFormat} to use when parsing numbers.
	 *            If {@code null} or not an instance of {@link DecimalFormat},
	 *            "standard formatted" numbers are expected (no grouping,
	 *            {@code .} as decimal separator etc.).
	 * @return An instance that represents the value of {@code value}.
	 * @throws NumberFormatException If {@code value} cannot be parsed.
	 */
	@Nullable
	public static Rational valueOf(@Nullable String value, @Nullable NumberFormat numberFormat) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String[] numbers = value.trim().split("\\s*(?:/|:)\\s*", 2);
		DecimalFormat decimalFormat = numberFormat instanceof DecimalFormat ? (DecimalFormat) numberFormat : null;

		BigDecimal decimalNumerator = parseBigDecimal(numbers[0], decimalFormat);
		if (decimalNumerator == null) {
			return null;
		}
		BigDecimal decimalDenominator;
		if (numbers.length > 1) {
			decimalDenominator = parseBigDecimal(numbers[1], decimalFormat);
		} else {
			decimalDenominator = BigDecimal.ONE;
		}
		if (decimalDenominator == null) {
			return null;
		}
		return valueOf(decimalNumerator, decimalDenominator);
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nonnull
	public static Rational valueOf(int value) {
		return valueOf((long) value);
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nonnull
	public static Rational valueOf(long value) {
		BigInteger numerator = BigInteger.valueOf(value);
		return new Rational(
			numerator,
			BigInteger.ONE,
			BigInteger.ONE,
			numerator,
			BigInteger.ONE
		);
	}

	/**
	 * Returns an instance that represents the value of {@code value}.
	 *
	 * @param value the value.
	 * @return An instance that represents the value of {@code value}.
	 */
	@Nullable
	public static Rational valueOf(@Nullable BigInteger value) {
		if (value == null) {
			return null;
		}
		return new Rational(value, BigInteger.ONE, BigInteger.ONE, value, BigInteger.ONE);
	}

	/**
	 * Returns an instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return An instance that represents the value of {@code numerator}/
	 *         {@code denominator}.
	 */
	@Nonnull
	public static Rational valueOf(int numerator, int denominator) {
		return valueOf((long) numerator, (long) denominator);
	}

	/**
	 * Returns an instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return An instance that represents the value of {@code numerator}/
	 *         {@code denominator}.
	 */
	@Nonnull
	public static Rational valueOf(long numerator, long denominator) {
		if (numerator == 0 && denominator == 0) {
			return NaN;
		}
		if (denominator == 0) {
			return numerator > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (numerator == 0) {
			return ZERO;
		}
		if (numerator == denominator) {
			return ONE;
		}
		BigInteger biNumerator;
		BigInteger biDenominator;
		if (denominator < 0) {
			biNumerator = BigInteger.valueOf(-numerator);
			biDenominator = BigInteger.valueOf(-denominator);
		} else {
			biNumerator = BigInteger.valueOf(numerator);
			biDenominator = BigInteger.valueOf(denominator);
		}
		long gcd = calculateGreatestCommonDivisor(numerator, denominator);
		BigInteger greatestCommonDivisor = BigInteger.valueOf(gcd);
		BigInteger reducedNumerator;
		BigInteger reducedDenominator;
		if (gcd == 1) {
			reducedNumerator = biNumerator;
			reducedDenominator = biDenominator;
		} else {
			reducedNumerator = biNumerator.divide(greatestCommonDivisor);
			reducedDenominator = biDenominator.divide(greatestCommonDivisor);
		}
		return new Rational(
			biNumerator,
			biDenominator,
			greatestCommonDivisor,
			reducedNumerator,
			reducedDenominator
		);
	}

	/**
	 * Returns an instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return An instance that represents the value of {@code numerator}/
	 *         {@code denominator}.
	 */
	@Nullable
	public static Rational valueOf(@Nullable BigInteger numerator, @Nullable BigInteger denominator) {
		if (numerator == null || denominator == null) {
			return null;
		}
		if (numerator.signum() == 0 && denominator.signum() == 0) {
			return NaN;
		}
		if (denominator.signum() == 0) {
			return numerator.signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (numerator.signum() == 0) {
			return ZERO;
		}
		if (numerator.equals(denominator)) {
			return ONE;
		}
		if (denominator.signum() < 0) {
			numerator = numerator.negate();
			denominator = denominator.negate();
		}

		BigInteger reducedNumerator;
		BigInteger reducedDenominator;
		BigInteger greatestCommonDivisor = calculateGreatestCommonDivisor(numerator, denominator);
		if (BigInteger.ONE.equals(greatestCommonDivisor)) {
			reducedNumerator = numerator;
			reducedDenominator = denominator;
		} else {
			reducedNumerator = numerator.divide(greatestCommonDivisor);
			reducedDenominator = denominator.divide(greatestCommonDivisor);
		}
		return new Rational(
			numerator,
			denominator,
			greatestCommonDivisor,
			reducedNumerator,
			reducedDenominator
		);
	}

	/**
	 * Returns an instance with the given {@code numerator} and
	 * {@code denominator}.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return An instance that represents the value of {@code numerator}/
	 *         {@code denominator}.
	 */
	@Nullable
	public static Rational valueOf(@Nullable BigDecimal numerator, @Nullable BigDecimal denominator) {
		if (numerator == null || denominator == null) {
			return null;
		}
		if (numerator.signum() == 0 && denominator.signum() == 0) {
			return NaN;
		}
		if (denominator.signum() == 0) {
			return numerator.signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (numerator.signum() == 0) {
			return ZERO;
		}
		if (numerator.equals(denominator)) {
			return ONE;
		}
		if (denominator.signum() < 0) {
			numerator = numerator.negate();
			denominator = denominator.negate();
		}

		int scale = Math.max(numerator.scale(), denominator.scale());
		if (scale > 0) {
			numerator = numerator.scaleByPowerOfTen(scale);
			denominator = denominator.scaleByPowerOfTen(scale);
		}
		BigInteger biNumerator = numerator.toBigIntegerExact();
		BigInteger biDenominator = denominator.toBigIntegerExact();

		BigInteger reducedNumerator;
		BigInteger reducedDenominator;
		BigInteger greatestCommonDivisor = calculateGreatestCommonDivisor(biNumerator, biDenominator);
		if (BigInteger.ONE.equals(greatestCommonDivisor)) {
			reducedNumerator = biNumerator;
			reducedDenominator = biDenominator;
		} else {
			reducedNumerator = biNumerator.divide(greatestCommonDivisor);
			reducedDenominator = biDenominator.divide(greatestCommonDivisor);
		}
		return new Rational(
			biNumerator,
			biDenominator,
			greatestCommonDivisor,
			reducedNumerator,
			reducedDenominator
		);
	}


	// Operations


	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nonnull
	public Rational multiply(int value) {
		return multiply(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nonnull
	public Rational multiply(long value) {
		return multiply(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nullable
	public Rational multiply(@Nullable BigInteger value) {
		if (value == null) {
			return null;
		}
		if (isNaN()) {
			return NaN;
		}
		if (isInfinite()) {
			if (value.signum() == 0) {
				return NaN; // Infinity by zero
			}
			return numerator.signum() == value.signum() ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (value.signum() == 0) {
			return ZERO;
		}

		if (BigInteger.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		return valueOf(reducedNumerator.multiply(value), reducedDenominator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nonnull
	public Rational multiply(float value) {
		if (isNaN() || Float.isNaN(value)) {
			return NaN;
		}
		if (isInfinite() || Float.isInfinite(value)) {
			if (signum() == 0 || value == 0f) {
				return NaN; // Infinity by zero
			}
			if (value > 0) {
				return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}
			return signum() < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (value == 0f) {
			return ZERO;
		}

		return multiply(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nonnull
	public Rational multiply(double value) {
		if (isNaN() || Double.isNaN(value)) {
			return NaN;
		}
		if (isInfinite() || Double.isInfinite(value)) {
			if (signum() == 0 || value == 0f) {
				return NaN; // Infinity by zero
			}
			if (value > 0) {
				return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}
			return signum() < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (value == 0f) {
			return ZERO;
		}
		return multiply(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nullable
	public Rational multiply(@Nullable BigDecimal value) {
		if (value == null) {
			return null;
		}
		return multiply(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this * value)}.
	 *
	 * @param value the value to be multiplied by this {@link Rational}.
	 * @return The multiplication result.
	 */
	@Nullable
	public Rational multiply(@Nullable Rational value) {
		if (value == null) {
			return null;
		}
		if (isNaN() || value.isNaN()) {
			return NaN;
		}
		if (isInfinite() || value.isInfinite()) {
			if (signum() == 0 || value.signum() == 0) {
				return NaN; // Infinity by zero
			}
			return numerator.signum() == value.signum() ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}
		if (value.signum() == 0) {
			return ZERO;
		}

		if (value.numerator.abs().equals(value.denominator)) {
			return value.signum() < 0 ? this.negate() : this;
		}

		BigInteger newNumerator = reducedNumerator.multiply(value.reducedNumerator);
		BigInteger newDenominator = reducedDenominator.multiply(value.reducedDenominator);
		BigInteger gcd = newNumerator.gcd(newDenominator);
		return valueOf(newNumerator.divide(gcd), newDenominator.divide(gcd));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nonnull
	public Rational subtract(int value) {
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nonnull
	public Rational subtract(long value) {
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nullable
	public Rational subtract(@Nullable BigInteger value) {
		if (value == null) {
			return null;
		}

		return add(value.negate());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nonnull
	public Rational subtract(float value) {
		if (isNaN() || Float.isNaN(value)) {
			return NaN;
		}

		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nonnull
	public Rational subtract(double value) {
		if (isNaN() || Double.isNaN(value)) {
			return NaN;
		}
		return add(-value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nullable
	public Rational subtract(@Nullable BigDecimal value) {
		if (value == null) {
			return null;
		}
		return add(value.negate());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this - value)}.
	 *
	 * @param value the value to be subtracted from this {@link Rational}.
	 * @return The subtraction result.
	 */
	@Nullable
	public Rational subtract(@Nullable Rational value) {
		if (value == null) {
			return null;
		}

		if (isNaN() || value.isNaN()) {
			return NaN;
		}

		if (value.signum() == 0) {
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
	@Nonnull
	public Rational add(int value) {
		return add((long) value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nonnull
	public Rational add(long value) {
		return add(BigInteger.valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nullable
	public Rational add(@Nullable BigInteger value) {
		if (value == null) {
			return null;
		}

		if (isNaN()) {
			return NaN;
		}
		if (isInfinite() || value.signum() == 0) {
			return this;
		}

		if (BigInteger.ONE.equals(denominator)) {
			return valueOf(numerator.add(value), denominator);
		}
		return valueOf(numerator.add(value.multiply(denominator)), denominator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nonnull
	public Rational add(float value) {
		if (isNaN() || Float.isNaN(value)) {
			return NaN;
		}

		if (value == 0f) {
			return this;
		}

		if (isInfinite()) {
			if (Float.isInfinite(value) && signum() != Math.signum(value)) {
				return NaN; // Infinity minus infinity
			}
			return this;
		}

		return add(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nonnull
	public Rational add(double value) {
		if (isNaN() || Double.isNaN(value)) {
			return NaN;
		}

		if (value == 0d) {
			return this;
		}

		if (isInfinite()) {
			if (Double.isInfinite(value) && signum() != Math.signum(value)) {
				return NaN; // Infinity minus infinity
			}
			return this;
		}
		return add(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nullable
	public Rational add(@Nullable BigDecimal value) {
		if (value == null) {
			return null;
		}
		if (isNaN()) {
			return NaN;
		}
		if (isInfinite() || value.signum() == 0) {
			return this;
		}

		return add(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this + value)}.
	 *
	 * @param value the value to be added to this {@link Rational}.
	 * @return The addition result.
	 */
	@Nullable
	public Rational add(@Nullable Rational value) {
		if (value == null) {
			return null;
		}

		if (isNaN() || value.isNaN()) {
			return NaN;
		}

		if (value.numerator.signum() == 0) {
			return this;
		}

		if (this.numerator.signum() == 0) {
			return value;
		}

		if (isInfinite()) {
			if (value.isInfinite() && signum() != value.signum()) {
				return NaN; // Infinity minus infinity
			}
			return this;
		}

		BigInteger lcm = calculateLeastCommonMultiple(denominator, value.denominator);
		return valueOf(numerator.multiply(lcm.divide(denominator)).add(
			value.numerator.multiply(lcm.divide(value.denominator))), lcm);
	}

	/**
	 * Returns a {@link Rational} whose value is the reciprocal of this
	 * {@code (1 / this)}.
	 *
	 * @return The reciprocal result.
	 */
	@Nonnull
	public Rational reciprocal() {
		if (isNaN()) {
			return NaN;
		}
		if (isInfinite()) {
			return ZERO;
		}
		if (numerator.signum() == 0) {
			return NaN;
		}
		return valueOf(denominator, numerator);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 * @throws ArithmeticException if {@code value} is zero.
	 */
	@Nonnull
	public Rational divide(int value) {
		return divide((long) value);
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nonnull
	public Rational divide(long value) {
		if (isNaN()) {
			return NaN;
		}

		if (value == 0) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (signum() == 0 || isInfinite() || 1 == Math.abs(value)) {
			return value < 0 ? negate() : this;
		}

		// Keep the sign in the numerator and the denominator positive
		if (value < 0) {
			return valueOf(reducedNumerator.negate(), reducedDenominator.multiply(BigInteger.valueOf(-value)));
		}
		return valueOf(reducedNumerator, reducedDenominator.multiply(BigInteger.valueOf(value)));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nullable
	public Rational divide(@Nullable BigInteger value) {
		if (value == null) {
			return null;
		}
		if (isNaN()) {
			return NaN;
		}

		if (value.signum() == 0) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (signum() == 0 || isInfinite() || BigInteger.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		// Keep the sign in the numerator and the denominator positive
		if (value.signum() < 0) {
			return valueOf(reducedNumerator.negate(), reducedDenominator.multiply(value.negate()));
		}
		return valueOf(reducedNumerator, reducedDenominator.multiply(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nonnull
	public Rational divide(float value) {
		if (
			isNaN() ||
			Float.isNaN(value) ||
			isInfinite() && Float.isInfinite(value)
		) {
			return NaN;
		}

		if (value == 0f) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (Float.isInfinite(value)) {
			return ZERO;
		}
		if (signum() == 0 || isInfinite() || 1f == Math.abs(value)) {
			return value < 0f ? negate() : this;
		}

		return divide(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nonnull
	public Rational divide(double value) {
		if (
			isNaN() ||
			Double.isNaN(value) ||
			isInfinite() && Double.isInfinite(value)
		) {
			return NaN;
		}

		if (value == 0d) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (Double.isInfinite(value)) {
			return ZERO;
		}
		if (signum() == 0 || isInfinite() || 1d == Math.abs(value)) {
			return value < 0d ? negate() : this;
		}

		return divide(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nullable
	public Rational divide(@Nullable BigDecimal value) {
		if (value == null) {
			return null;
		}
		if (isNaN()) {
			return NaN;
		}

		if (value.signum() == 0) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (signum() == 0 || isInfinite() || BigDecimal.ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		return divide(valueOf(value));
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (this / value)}.
	 *
	 * @param value the value by which this {@link Rational} is to be divided.
	 * @return The division result.
	 */
	@Nullable
	public Rational divide(@Nullable Rational value) {
		if (value == null) {
			return null;
		}
		if (
			isNaN() ||
			value.isNaN() ||
			isInfinite() && value.isInfinite()
		) {
			return NaN;
		}

		if (value.signum() == 0) {
			if (signum() == 0) {
				return NaN;
			}
			return signum() > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
		}

		if (value.isInfinite()) {
			return ZERO;
		}
		if (signum() == 0 || isInfinite() || ONE.equals(value.abs())) {
			return value.signum() < 0 ? negate() : this;
		}

		return numerator.signum() == 0 ? this : multiply(value.reciprocal());
	}

	/**
	 * Returns a {@link Rational} whose value is {@code (-this)}.
	 *
	 * @return The negated result.
	 */
	@Nonnull
	public Rational negate() {
		if (numerator.signum() == 0) {
			return this;
		}
		return valueOf(numerator.negate(), denominator);
	}

	/**
	 * Returns a {@link Rational} whose value is the absolute value of this
	 * {@code (abs(this))}.
	 *
	 * @return The absolute result.
	 */
	@Nonnull
	public Rational abs() {
		return numerator.signum() < 0 ? valueOf(numerator.negate(), denominator) : this;
	}

	/**
	 * Returns a {@link Rational} whose value is
	 * <tt>(this<sup>exponent</sup>)</tt>.
	 *
	 * @param exponent exponent to which this {@link Rational} is to be raised.
	 * @return <tt>this<sup>exponent</sup></tt>
	 */
	@Nonnull
	public Rational pow(int exponent) {
		if (isNaN()) {
			return NaN;
		}
		if (exponent == 0) {
			return signum() == 0 || denominator.signum() == 0 ? NaN : ONE;
		}
		if (exponent == 1) {
			return this;
		}
		if (isInfinite()) {
			if (exponent < 0) {
				return ZERO;
			}
			return (exponent & 1) == 0 ? abs() : this;
		}
		if (signum() == 0) {
			return exponent > 0 ? ZERO : POSITIVE_INFINITY;
		}

		if (exponent < 0) {
			if (exponent == Integer.MIN_VALUE) {
				return reciprocal().pow(2).pow(-(exponent / 2));
			}
			return reciprocal().pow(-exponent);
		}
		Rational result = multiply(this);
		if ((exponent & 1) == 0) {
			return result.pow(exponent / 2);
		}
		return result.pow(exponent / 2).multiply(this);
	}

	/**
	 * Returns the signum function of this {@link Rational}.
	 *
	 * @return -1, 0 or 1 as the value of this {@link Rational} is negative,
	 *         zero or positive. As a special case, signum also returns 0 for
	 *         {@code NaN} which means that <b>{@link #isNaN()} must always be
	 *         checked before {@link #signum()}</b> is used to determine if the
	 *         value is {@link #ZERO}.
	 */
	public int signum() {
		return numerator.signum();
	}

	/**
	 * Returns {@code true} if this {@link Rational} can be expressed as an
	 * integer value, {@code false} otherwise.
	 *
	 * @return {@code true} if this can be expressed as an integer value,
	 *         {@code false} otherwise.
	 */
	public boolean isInteger() {
		return !isNaN() && !isInfinite() && BigInteger.ONE.equals(reducedDenominator);
	}

	/**
	 * Returns {@code true} if this is a Not-a-Number (NaN) value, {@code false}
	 * otherwise.
	 *
	 * @return {@code true} if this is {@code NaN}, {@code false} otherwise.
	 */
	public boolean isNaN() {
		return numerator.signum() == 0 && denominator.signum() == 0;
	}

	/**
	 * Returns {@code true} if this is infinitely large in magnitude,
	 * {@code false} otherwise.
	 *
	 * @return {@code true} if this is positive infinity or negative infinity,
	 *         {@code false} otherwise.
	 */
	public boolean isInfinite() {
		return numerator.signum() != 0 && denominator.signum() == 0;
	}

	/**
	 * Returns {@code true} if this is infinitely positive, {@code false}
	 * otherwise.
	 *
	 * @return {@code true} if this is positive infinity, {@code false}
	 *         otherwise.
	 */
	public boolean isInfinitePositive() {
		return numerator.signum() > 0 && denominator.signum() == 0;
	}

	/**
	 * Returns {@code true} if this is infinitely negative, {@code false}
	 * otherwise.
	 *
	 * @return {@code true} if this is negative infinity, {@code false}
	 *         otherwise.
	 */
	public boolean isInfiniteNegative() {
		return numerator.signum() < 0 && denominator.signum() == 0;
	}


	// Getters


	/**
	 * @return The numerator.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	public BigInteger getNumerator() {
		if (isNaN() || isInfinite()) {
			throw new ArithmeticException("Numerator is undefined");
		}
		return numerator;
	}

	/**
	 * @return The denominator.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	public BigInteger getDenominator() {
		if (isNaN() || isInfinite()) {
			throw new ArithmeticException("Denominator is undefined");
		}
		return denominator;
	}

	/**
	 * The reduced numerator is the numerator divided by
	 * {@link #getGreatestCommonDivisor}.
	 *
	 * @return The reduced numerator.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	public BigInteger getReducedNumerator() {
		if (isNaN() || isInfinite()) {
			throw new ArithmeticException("Numerator is undefined");
		}
		return reducedNumerator;
	}

	/**
	 * The reduced denominator is the denominator divided by
	 * {@link #getGreatestCommonDivisor}.
	 *
	 * @return The reduced denominator.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	public BigInteger getReducedDenominator() {
		if (isNaN() || isInfinite()) {
			throw new ArithmeticException("Denominator is undefined");
		}
		return reducedDenominator;
	}

	/**
	 * @return The greatest common divisor of the numerator and the denominator.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	public BigInteger getGreatestCommonDivisor() {
		if (isNaN() || isInfinite()) {
			throw new ArithmeticException("Greatest common divisor is undefined");
		}
		return greatestCommonDivisor;
	}


	// String methods


	/**
	 * Returns a string representation of this {@link Rational} in its reduced
	 * rational form {@code (1/2, 4 or 16/9)}. The reduced form is when both
	 * numerator and denominator have been divided by the greatest common
	 * divisor.
	 *
	 * @return The reduced {@link String} representation.
	 */
	@Override
	public String toString() {
		return generateRationalString(reducedNumerator, reducedDenominator);
	}

	/**
	 * Returns a string representation of this {@link Rational} in its original
	 * rational form (not reduced) {@code (2/4, 4 or 32/18)}.
	 *
	 * @return The {@link String} representation.
	 */
	@Nonnull
	public String toUnreducedString() {
		return generateRationalString(numerator, denominator);
	}

	/**
	 * Returns a decimal representation of this {@link Rational}. The decimal
	 * representation is limited to 20 decimals using
	 * {@link RoundingMode#HALF_EVEN} and is formatted with {@link Locale#ROOT}
	 * without grouping.
	 *
	 * @return The decimal {@link String} representation.
	 */
	@Nonnull
	public String toDecimalString() {
		return toDecimalString(null);
	}

	/**
	 * Returns a decimal representation of this {@link Rational}. The decimal
	 * representation is limited to 20 decimals using
	 * {@link RoundingMode#HALF_EVEN} and is formatted with the specified
	 * {@link DecimalFormat}.
	 *
	 * @param decimalFormat the {@link DecimalFormat} to use.
	 * @return The decimal {@link String} representation.
	 */
	@Nonnull
	public String toDecimalString(@Nullable DecimalFormat decimalFormat) {
		if (decimalFormat == null) {
			decimalFormat = DECIMALFORMAT;
		}
		return generateDecimalString(reducedNumerator, reducedDenominator, decimalFormat);
	}

	/**
	 * Returns a debug string representation of this {@link Rational}. The debug
	 * representation is a combination of {@link #toString},
	 * {@link #toReducedString} and {@link #toDecimalString}.
	 *
	 * @return The debug {@link String} representation.
	 */
	@Nonnull
	public String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Value: ").append(generateRationalString(numerator, denominator))
			.append(", Reduced: ").append(generateRationalString(reducedNumerator, reducedDenominator))
			.append(", Decimal: ").append(generateDecimalString(reducedNumerator, reducedDenominator, DECIMALFORMAT));
		return sb.toString();
	}

	/**
	 * Returns a hexadecimal string representation of this {@link Rational} in
	 * its reduced rational form {@code (5, ff or 16/c)}. The reduced form is
	 * when both numerator and denominator have been divided by the greatest
	 * common divisor.
	 *
	 * @return The reduced hexadecimal {@link String} representation.
	 */
	@Nonnull
	public String toHexString() {
		return generateRationalHexString(reducedNumerator, reducedDenominator);
	}

	/**
	 * Returns a hexadecimal string representation of this {@link Rational} in
	 * its original rational form (not reduced) {@code (a/2, ff or 16/c)}.
	 *
	 * @return The hexadecimal {@link String} representation.
	 */
	@Nonnull
	public String toUnreducedHexString() {
		return generateRationalHexString(numerator, denominator);
	}

	/**
	 * Returns a string representation of this {@link Rational} in the form
	 * {@code numerator:denominator} while respecting convention and adjusting
	 * for small inaccuracies.
	 *
	 * @return The aspect ratio {@link String} representation.
	 */
	@Nonnull
	public String toAspectRatio() {
		if (numerator.signum() == 0 || denominator.signum() == 0) {
			// Return 1:1 for undefined values and 0
			return "1:1";
		}
		double value = doubleValue();
		if (value >= 11.9 && value <= 12.1) {
			return "12.00:1";
		} else if (value >= 3.9 && value <= 4.1) {
			return "4.00:1";
		} else if (value >= 2.75 && value <= 2.77) {
			return "2.76:1";
		} else if (value >= 2.65 && value <= 2.67) {
			return "24:9";
		} else if (value >= 2.58 && value <= 2.6) {
			return "2.59:1";
		} else if (value >= 2.54  && value <= 2.56) {
			return "2.55:1";
		} else if (value >= 2.38 && value <= 2.41) {
			return "2.39:1";
		} else if (value > 2.36 && value < 2.38) {
			return "2.37:1";
		} else if (value >= 2.34 && value <= 2.36) {
			return "2.35:1";
		} else if (value >= 2.33 && value < 2.34) {
			return "21:9";
		} else if (value > 2.1  && value < 2.3) {
			return "11:5";
		} else if (value > 1.9 && value < 2.1) {
			return "2.00:1";
		} else if (value > 1.87  && value <= 1.9) {
			return "1.896:1";
		} else if (value >= 1.83 && value <= 1.87) {
			return "1.85:1";
		} else if (value >= 1.76 && value <= 1.8) {
			return "16:9";
		} else if (value > 1.74 && value < 1.76) {
			return "7:4";
		} else if (value >= 1.65 && value <= 1.67) {
			return "15:9";
		} else if (value >= 1.59 && value <= 1.61) {
			return "16:10";
		} else if (value >= 1.54 && value <= 1.56) {
			return "14:9";
		} else if (value >= 1.49 && value <= 1.51) {
			return "3:2";
		} else if (value > 1.42 && value < 1.44) {
			return "1.43:1";
		} else if (value > 1.372 && value < 1.4) {
			return "11:8";
		} else if (value > 1.35 && value <= 1.372) {
			return "1.37:1";
		} else if (value >= 1.3 && value <= 1.35) {
			return "4:3";
		} else if (value > 1.2 && value < 1.3) {
			return "5:4";
		} else if (value >= 1.18 && value <= 1.195) {
			return "19:16";
		} else if (value > 0.99 && value < 1.1) {
			return "1:1";
		} else {
			return reducedNumerator + ":" + reducedDenominator;
		}
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
		if (isNaN()) {
			return 0;
		}
		if (isInfinite()) {
			return numerator.signum() > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		}
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
		if (isNaN()) {
			return 0;
		}
		if (isInfinite()) {
			return numerator.signum() > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
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
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 */
	@Nonnull
	public BigInteger bigIntegerValue() {
		if (isNaN()) {
			throw new ArithmeticException("Impossible to express NaN as BigInteger");
		}
		if (isInfinite()) {
			throw new ArithmeticException("Impossible to express infinity as BigInteger");
		}
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
		if (isNaN()) {
			return Float.NaN;
		}
		if (isInfinitePositive()) {
			return Float.POSITIVE_INFINITY;
		}
		if (isInfiniteNegative()) {
			return Float.NEGATIVE_INFINITY;
		}
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
		if (isNaN()) {
			return Double.NaN;
		}
		if (isInfinitePositive()) {
			return Double.POSITIVE_INFINITY;
		}
		if (isInfiniteNegative()) {
			return Double.NEGATIVE_INFINITY;
		}
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
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If this is {@code NaN} or infinite.
	 *
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(RoundingMode)
	 * @see #bigDecimalValue(int, RoundingMode)
	 */
	@Nonnull
	public BigDecimal bigDecimalValue() {
		if (isNaN()) {
			throw new ArithmeticException("Impossible to express NaN as BigDecimal");
		}
		if (isInfinite()) {
			throw new ArithmeticException("Impossible to express infinity as BigDecimal");
		}

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
	 * @param roundingMode the {@link RoundingMode} to apply.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If this is {@code NaN} or infinite or if
	 *             {@code roundingMode} is {@link RoundingMode#UNNECESSARY} and
	 *             the specified scale is insufficient to represent the result
	 *             of the division exactly.
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(int, RoundingMode)
	 */
	@Nonnull
	public BigDecimal bigDecimalValue(RoundingMode roundingMode) {
		if (isNaN()) {
			throw new ArithmeticException("Impossible to express NaN as BigDecimal");
		}
		if (isInfinite()) {
			throw new ArithmeticException("Impossible to express infinity as BigDecimal");
		}

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
	 * @param scale the scale of the {@link BigDecimal} quotient to be returned.
	 * @param roundingMode the {@link RoundingMode} to apply.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If this is {@code NaN} or infinite or if
	 *             {@code roundingMode} is {@link RoundingMode#UNNECESSARY} and
	 *             the specified scale is insufficient to represent the result
	 *             of the division exactly.
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(MathContext)
	 * @see #bigDecimalValue(RoundingMode)
	 */
	@Nonnull
	public BigDecimal bigDecimalValue(int scale, RoundingMode roundingMode) {
		if (isNaN()) {
			throw new ArithmeticException("Impossible to express NaN as BigDecimal");
		}
		if (isInfinite()) {
			throw new ArithmeticException("Impossible to express infinity as BigDecimal");
		}

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
	 * @param mathContext the {@link MathContext} to use.
	 * @return This {@link Rational} converted to a {@link BigDecimal}.
	 * @throws ArithmeticException If this is {@code NaN} or infinite or if the
	 *             result is inexact but the rounding mode is
	 *             {@code UNNECESSARY} or {@code mathContext.precision == 0} and
	 *             the quotient has a non-terminating decimal expansion.
	 *
	 * @see #bigDecimalValue()
	 * @see #bigDecimalValue(RoundingMode)
	 * @see #bigDecimalValue(int, RoundingMode)
	 */
	@Nonnull
	public BigDecimal bigDecimalValue(MathContext mathContext) {
		if (isNaN()) {
			throw new NumberFormatException("Impossible to express NaN as BigDecimal");
		}
		if (isInfinite()) {
			throw new NumberFormatException("Impossible to express infinity as BigDecimal");
		}

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
	 * <p>
	 * <b>Note:</b> {@code NaN} can't be compared by value and is considered
	 * greater than anything but itself as defined for {@link Double}.
	 *
	 * @param other the {@link Rational} to which this {@link Rational} is to be
	 *            compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code other}.
	 */
	@Override
	public int compareTo(@Nonnull Rational other) {
		// NaN comparison is done according to the rules for Double.
		if (isNaN()) {
			return other.isNaN() ? 0 : 1;
		}
		if (other.isNaN()) {
			return -1;
		}

		if (isInfinite()) {
			if (other.isInfinite()) {
				return signum() - other.signum();
			}
			return this.signum();
		}
		if (other.isInfinite()) {
			return -other.signum();
		}

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
	 * <p>
	 * <b>Note:</b> {@code NaN} can't be compared by value and is considered
	 * greater than anything but itself as defined for {@link Double}.
	 *
	 * @param number the {@link Number} to which this {@link Rational}'s value
	 *            is to be compared.
	 * @return A negative integer, zero, or a positive integer as this
	 *         {@link Rational} is numerically less than, equal to, or greater
	 *         than {@code number}.
	 */
	public int compareTo(@Nonnull Number number) {
		// Establish special cases
		boolean numberIsNaN;
		boolean numberIsInfinite;
		int numberSignum;
		if (number instanceof Rational) {
			numberIsNaN = Rational.isNaN((Rational) number);
			numberIsInfinite = Rational.isInfinite((Rational) number);
			numberSignum = ((Rational) number).numerator.signum();
		} else if (number instanceof Float) {
			numberIsNaN = Float.isNaN(number.floatValue());
			numberIsInfinite = Float.isInfinite(number.floatValue());
			numberSignum = (int) Math.signum(number.floatValue());
		} else if (number instanceof Double) {
			numberIsNaN = Double.isNaN(number.doubleValue());
			numberIsInfinite = Double.isInfinite(number.doubleValue());
			numberSignum = (int) Math.signum(number.doubleValue());
		} else {
			numberIsNaN = false;
			numberIsInfinite = false;
			long l = number.longValue();
			numberSignum = l == 0 ? 0 : l > 0 ? 1 : -1;
		}

		// NaN comparison is done according to the rules for Double.
		if (isNaN()) {
			return numberIsNaN ? 0 : 1;
		}
		if (numberIsNaN) {
			return -1;
		}

		if (isInfinite()) {
			if (numberIsInfinite) {
				return signum() - numberSignum;
			}
			return this.signum();
		}
		if (numberIsInfinite) {
			return -numberSignum;
		}

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
			Rational other = valueOf((BigDecimal) number);
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
				throw new AssertionError("reducedNumerator or reducedDenominator is null");
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
	 * @throws ArithmeticException if either part is {@code NaN} or infinite.
	 */
	@Nonnull
	protected BigInteger[] getMultipliers(@Nonnull Rational other) {
		if (isNaN() || isInfinite() || other.isNaN() || other.isInfinite()) {
			throw new ArithmeticException("Can't calculate multipliers for NaN or infinity");
		}
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
	 * @throws ArithmeticException if GDC is greater than {@link Long#MAX_VALUE}.
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
	@Nullable
	public static BigInteger calculateGreatestCommonDivisor(@Nullable BigInteger u, @Nullable BigInteger v) {
		if (u == null || v == null) {
			return null;
		}
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
	@Nullable
	public static BigInteger calculateLeastCommonMultiple(@Nullable BigInteger u, @Nullable BigInteger v) {
		if (u == null || v == null) {
			return null;
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
	 * Tests if the specified {@link Rational} is {@code null},
	 * {@link Rational#ZERO} or {@link Rational#NaN}.
	 *
	 * @param rational the {@link Rational} to evaluate.
	 * @return {@code true} if {@code rational} is {@code null} or its value is
	 *         zero or {@code NaN}, {@code false} otherwise.
	 */
	public static boolean isBlank(@Nullable Rational rational) {
		return rational == null || rational.signum() == 0;
	}

	/**
	 * Tests if the specified {@link Rational} is not {@code null}, not
	 * {@link Rational#ZERO} and not {@link Rational#NaN}.
	 *
	 * @param rational the {@link Rational} to evaluate.
	 * @return {@code true} if {@code rational} is not {@code null} and its
	 *         value isn't zero or {@code NaN}, {@code false} otherwise.
	 */
	public static boolean isNotBlank(@Nullable Rational rational) {
		return rational != null && rational.numerator.signum() != 0;
	}

	/**
	 * Tests if the specified {@link Rational} is a "regular" number, which
	 * means not {@code null}, not {@code NaN} and not infinite.
	 *
	 * @param rational the {@link Rational} to evaluate.
	 * @return {@code true} if {@code rational} is not {@code null}, not
	 *         {@code NaN} and not infinite; {@code false} otherwise.
	 */
	public static boolean isRegular(@Nullable Rational rational) {
		return rational != null && rational.denominator != null;
	}

	/**
	 * Returns {@code true} if the specified {@link Rational} can be expressed
	 * as an integer value, {@code false} otherwise.
	 *
	 * @param rational the {@link Rational} to evaluate.
	 * @return {@code true} if {@code rational} can be expressed as an integer
	 *         value, {@code false} otherwise.
	 */
	public static boolean isInteger(@Nullable Rational rational) {
		return rational == null ? false : rational.isInteger();
	}

	/**
	 * Returns {@code true} if the specified {@link Rational} has a Not-a-Number
	 * ({@code NaN}) value, {@code false} otherwise.
	 *
	 * @param rational the {@link Rational} to be tested.
	 * @return {@code true} if {@code rational} is {@code NaN}, {@code false}
	 *         otherwise.
	 */
	public static boolean isNaN(@Nullable Rational rational) {
		return rational == null ? false : rational.isNaN();
	}

	/**
	 * Returns {@code true} if the specified {@link Rational} is infinitely
	 * large in magnitude, {@code false} otherwise.
	 *
	 * @param rational the {@link Rational} to be tested.
	 * @return {@code true} if the value of the argument is positive infinity or
	 *         negative infinity, {@code false} otherwise.
	 */
	public static boolean isInfinite(@Nullable Rational rational) {
		return rational == null ? false : rational.isInfinite();
	}

	/**
	 * Returns {@code true} if the specified {@link Rational} is infinitely
	 * positive, {@code false} otherwise.
	 *
	 * @param rational the {@link Rational} to be tested.
	 * @return {@code true} if value of the argument is positive infinity,
	 *         {@code false} otherwise.
	 */
	public static boolean isInfinitePositive(@Nullable Rational rational) {
		return rational == null ? false : rational.isInfinitePositive();
	}

	/**
	 * Returns {@code true} if the specified {@link Rational} is infinitely negative, {@code false}
	 * otherwise.
	 *
	 * @param rational the {@link Rational} to be tested.
	 * @return {@code true} if value of the argument is negative infinity, {@code false}
	 *         otherwise.
	 */
	public static boolean isInfiniteNegative(@Nullable Rational rational) {
		return rational == null ? false : rational.isInfiniteNegative();
	}

	/**
	 * Parses the specified {@link String} and returns a {@link BigDecimal}
	 * using the specified {@link DecimalFormat}. {@code value} is expected to
	 * be without leading or trailing whitespace. If {@code value} is blank,
	 * {@code null} will be returned.
	 *
	 * @param value the {@link String} to parse.
	 * @param decimalFormat the {@link DecimalFormat} to use when parsing.
	 * @return The resulting {@link BigDecimal}.
	 * @throws NumberFormatException If {@code value} cannot be parsed.
	 */
	@Nullable
	public static BigDecimal parseBigDecimal(@Nullable String value, @Nullable DecimalFormat decimalFormat) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		if (decimalFormat != null) {
			decimalFormat.setParseBigDecimal(true);
			try {
				return (BigDecimal) decimalFormat.parseObject(value);
			} catch (ParseException e) {
				throw new NumberFormatException("Unable to parse \"" + value + "\": " + e.getMessage());
			}
		}
		return new BigDecimal(value);
	}

	/**
	 * Used internally to generate a hexadecimal rational string representation
	 * from two {@link BigInteger}s.
	 *
	 * @param numerator the numerator.
	 * @param denominator the denominator.
	 * @return The hexadecimal rational string representation.
	 */
	@Nonnull
	protected static String generateRationalHexString(@Nonnull BigInteger numerator, @Nonnull BigInteger denominator) {
		if (denominator.signum() == 0) {
			if (numerator.signum() == 0) {
				return "NaN";
			}
			return numerator.signum() > 0 ? "\u221e" : "-\u221e";
		}
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
	@Nonnull
	protected static String generateRationalString(@Nonnull BigInteger numerator, @Nonnull BigInteger denominator) {
		if (denominator.signum() == 0) {
			if (numerator.signum() == 0) {
				return "NaN";
			}
			return numerator.signum() > 0 ? "\u221e" : "-\u221e";
		}
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
	 * @param decimalFormat the {@link DecimalFormat} instance to use for
	 *            formatting.
	 * @return The string representation.
	 */
	protected static String generateDecimalString(
		@Nonnull BigInteger numerator,
		@Nonnull BigInteger denominator,
		@Nonnull DecimalFormat decimalFormat
	) {
		if (denominator.signum() == 0) {
			if (numerator.signum() == 0) {
				return "NaN";
			}
			return numerator.signum() > 0 ? "\u221e" : "-\u221e";
		}
		if (BigInteger.ONE.equals(denominator)) {
			return numerator.toString();
		}
		BigDecimal decimalValue = new BigDecimal(numerator).divide(new BigDecimal(denominator), 20, RoundingMode.HALF_EVEN);
		return decimalFormat.format(decimalValue);
	}
}
