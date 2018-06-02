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
package net.pms.image;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.metadata.Metadata;
import com.drew.metadata.jpeg.JpegComponent;
import com.drew.metadata.jpeg.JpegDirectory;

/**
 * This class is used to hold a subsampling J:a:b notation value. Use
 * {@link #toString()} to get a formatted {@link String}.
 * <p>
 * {@link #calculateJPEGSubsampling(Metadata)} and
 * {@link #calculateJPEGSubsampling(JpegComponent[], int, int)} are factory
 * methods to create {@link JPEGSubsamplingNotation} instances from the
 * {@link Metadata} of a JPEG image or for from a {@link JpegComponent}
 * respectively.
 *
 * @author Nadahar
 */
@SuppressWarnings("serial")
public class JPEGSubsamplingNotation implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(JPEGSubsamplingNotation.class);

	private final double j;
	private final double a;
	private final double b;

	/**
	 * Creates a new {@link JPEGSubsamplingNotation} with the given factors.
	 *
	 * @param j the {@code J} factor.
	 * @param a the {@code a} factor.
	 * @param b the {@code b} factor.
	 */
	public JPEGSubsamplingNotation(int j, int a, int b) {
		this.j = j;
		this.a = a;
		this.b = b;
	}

	public JPEGSubsamplingNotation(double j, double a, double b) {
		this.j = j;
		this.a = a;
		this.b = b;
	}

	// Internal constructor
	protected JPEGSubsamplingNotation(double[] factors) {
		j = factors[0];
		a = factors[1];
		b = factors[2];
	}

	/**
	 * Calculates the J:a:b chroma subsampling notation from a {@link Metadata}
	 * instance generated from a JPEG image. If the non-luminance components
	 * have different subsampling values or the calculation is impossible,
	 * {@link Double#NaN} will be returned for all factors.
	 *
	 * @param metadata the {@link Metadata} instance from which to calculate.
	 * @return A {@link JPEGSubsamplingNotation} with the result.
	 */
	public static JPEGSubsamplingNotation calculateJPEGSubsampling(Metadata metadata) {
		if (metadata == null) {
			throw new NullPointerException("metadata cannot be null");
		}

		JpegDirectory directory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
		if (directory == null) {
			return new JPEGSubsamplingNotation(Double.NaN, Double.NaN, Double.NaN);
		}

		return calculateJPEGSubsampling(directory);
	}

	/**
	 * Calculates the J:a:b chroma subsampling notation from a
	 * {@link JpegDirectory} instance generated from a JPEG image. If the
	 * non-luminance components have different subsampling values or the
	 * calculation is impossible, {@link Double#NaN} will be returned for all
	 * factors.
	 *
	 * @param directory the {@link JpegDirectory} instance from which to
	 *            calculate.
	 * @return A {@link JPEGSubsamplingNotation} with the result.
	 */
	public static JPEGSubsamplingNotation calculateJPEGSubsampling(JpegDirectory directory) {
		if (directory == null) {
			throw new NullPointerException("directory cannot be null");
		}

		if (
			directory.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS) == null ||
			directory.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS).intValue() == 0
		) {
			return new JPEGSubsamplingNotation(Double.NaN, Double.NaN, Double.NaN);
		}

		int numComponents = directory.getInteger(JpegDirectory.TAG_NUMBER_OF_COMPONENTS).intValue();
		int luminanceIdx = -1;
		JpegComponent[] components = new JpegComponent[numComponents];
		for (int i = 0; i < numComponents; i++) {
			components[i] = directory.getComponent(i);
			if (components[i].getComponentId() == 1) {
				luminanceIdx = i;
			}
		}

		if (luminanceIdx < 0) {
			return new JPEGSubsamplingNotation(Double.NaN, Double.NaN, Double.NaN);
		}

		JPEGSubsamplingNotation result = null;
		for (int i = 0; i < numComponents; i++) {
			if (i != luminanceIdx) {
				JPEGSubsamplingNotation componentResult = calculateJPEGSubsampling(components, luminanceIdx, i);
				if (result == null) {
					result = componentResult;
				} else {
					if (!result.equals(componentResult)) {
						LOGGER.trace(
							"Components {} and {} have mismatching chroma subsampling {} and " +
							"{}. Unable to determine an overall chroma subsampling notation",
							components[i - 1].getComponentName(),
							components[i].getComponentName(),
							result,
							componentResult
						);
						return new JPEGSubsamplingNotation(Double.NaN, Double.NaN, Double.NaN);
					}
				}
			}
		}

		return result != null ? result : new JPEGSubsamplingNotation(Double.NaN, Double.NaN, Double.NaN);
	}

	/**
	 * Calculates the J:a:b subsampling notation values for a given
	 * {@link JpegComponent} indicated by {@code componentIdx}.
	 * {@code luminanceIdx} is used to identify the luminance component which is
	 * needed as a reference.
	 *
	 * @param components the array of {@link JpegComponent} to calculate from.
	 * @param luminanceIdx the index of the luminance component within
	 *            {@code components}.
	 * @param componentIdx the index of the component within {@code components}
	 *            for which to calculate the subsampling notation values.
	 * @return A {@link JPEGSubsamplingNotation} with the result.
	 */
	public static JPEGSubsamplingNotation calculateJPEGSubsampling(
		JpegComponent[] components,
		int luminanceIdx,
		int componentIdx
	) {
		double[] result = new double[3];
		result[0] = 4;
		int hMax = 0;
		int vMax = 0;
		for (int i = 0; i < components.length; i++) {
			hMax = Math.max(hMax, components[i].getHorizontalSamplingFactor());
			vMax = Math.max(vMax, components[i].getVerticalSamplingFactor());
		}
		double[] h = new double[components.length];
		double[] v = new double[components.length];
		for (int i = 0; i < components.length; i++) {
			h[i] = (double) components[i].getHorizontalSamplingFactor() / hMax;
			v[i] = (double) components[i].getVerticalSamplingFactor() / vMax;
		}

		result[1] = 4 * h[componentIdx];
		double cHeight = 2 * v[componentIdx];
		result[2] = Double.NaN;
		if (cHeight == 2) {
			result[2] = result[1];
		} else if (cHeight == 1) {
			result[2] = 0;
		} else if (cHeight < 1) {
			result[2] = 1;
		}
		return new JPEGSubsamplingNotation(result);
	}

	/**
	 * @return The J value in J:a:b.
	 */
	public double getJ() {
		return j;
	}

	/**
	 * @return The a value in J:a:b.
	 */
	public double getA() {
		return a;
	}

	/**
	 * @return The b value in J:a:b.
	 */
	public double getB() {
		return b;
	}

	@Override
	public String toString() {
		if (
			(Double.isNaN(j) || Double.isInfinite(j)) &&
			(Double.isNaN(a) || Double.isInfinite(a)) &&
			(Double.isNaN(b) || Double.isInfinite(b))
		) {
			return "N/A";
		}
		DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));
		return String.format(
			Locale.ROOT, "%s:%s:%s",
			j == (long) j ? (long) j : df.format(j),
			a == (long) a ? (long) a : df.format(a),
			b == (long) b ? (long) b : df.format(b)
		);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(a);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(b);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(j);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof JPEGSubsamplingNotation)) {
			return false;
		}
		JPEGSubsamplingNotation other = (JPEGSubsamplingNotation) obj;
		if (Double.doubleToLongBits(a) != Double.doubleToLongBits(other.a)) {
			return false;
		}
		if (Double.doubleToLongBits(b) != Double.doubleToLongBits(other.b)) {
			return false;
		}
		if (Double.doubleToLongBits(j) != Double.doubleToLongBits(other.j)) {
			return false;
		}
		return true;
	}


}
