package net.pms.util;

import java.util.Arrays;

public final class Version implements Comparable<Version> {
	private final static int MAX_ELEMENTS = 4;
	private final int[] elements;

	public Version(String versionString) {
		if (versionString == null) {
			throw new NullPointerException("Version string can not be null");
		}

		elements = parse(versionString.split("\\.", MAX_ELEMENTS));
	}

	private int[] parse(String[] elements) {
		int[] out = new int[elements.length];

		for (int i = 0; i < elements.length; i++) {
			try {
				out[i] = Integer.parseInt(elements[i]);
			} catch (NumberFormatException e) {
				out[i] = 0;
			}
		}

		return out;
	}

	/**
	 * Compares this version to the supplied version and returns
	 * an <code>int</code> which indicates whether this version is
	 * less than, equal to, or greater than the supplied version.
	 *
	 * @param other version to compare this version to
	 * @return less than zero if this version is lower, 0 if they're
	 * equal, or greater than zero if this version is higher
	 */
	@Override
	public int compareTo(Version other) {
		final int[] longerElements, shorterElements;
		final int sign;

		if (elements.length >= other.elements.length) {
			longerElements = elements;
			shorterElements = other.elements;
			sign = 1;
		} else {
			longerElements = other.elements;
			shorterElements = elements;
			sign = -1;
		}

		for (int i = 0; i < longerElements.length; ++i) {
			int val = i < shorterElements.length ? shorterElements[i] : 0;

			if (longerElements[i] != val) {
				return (longerElements[i] - val) * sign;
			}
		}

		return 0;
	}

	/**
	 * Returns true if this version equals the supplied object,
	 * false otherwise
	 * @param other object to be compared with this version
	 * @return true if this version is equal to the supplied object, false otherwise
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Version) {
			return compareTo((Version)other) == 0;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if this version is less than the supplied
	 * version, false otherwise
	 * @param other version to be compared with this version
	 * @return true if this version is less than the supplied version, false otherwise
	 */
	public boolean isLessThan(Version other) {
		return compareTo(other) < 0;
	}

	/**
	 * Returns true if this version is less than or equal
	 * to the supplied version, false otherwise
	 * @param other version to be compared with this version2yy
	 * @return true if this version is less than or equal to the supplied version, false otherwise
	 */
	public boolean isLessThanOrEqualTo(Version other) {
		return compareTo(other) <= 0;
	}

	/**
	 * Returns true if this version is greater than
	 * the supplied version, false otherwise
	 * @param other version to be compared with this version2yy
	 * @return true if this version is greater than the supplied version, false otherwise
	 */
	public boolean isGreaterThan(Version other) {
		return compareTo(other) > 0;
	}

	/**
	 * Returns true if this version is greater than or
	 * equal to the supplied version, false otherwise
	 * @param other version to be compared with this version2yy
	 * @return true if this version is greater than or equal to the supplied version,false otherwise
	 */
	public boolean isGreaterThanOrEqualTo(Version other) {
		return compareTo(other) >= 0;
	}

	/**
	 * Compares an initial (current) version and a target version of PMS and
	 * returns true if the initial version can be updated
	 * to the target version. See src/main/external-resources/update/README
	 * for the criteria.
	 * @param vFrom The initial version
	 * @param vTo The target version
	 * @return <code>true</code> if the current version can safely be updated, <code>false</code> otherwise.
	 */
	public static boolean isPmsUpdatable(Version vFrom, Version vTo) {
		return vTo.isGreaterThan(vFrom);
	}

	/**
	 * Returns the first integer element of this version string,
	 * or 0 if a first integer was not defined or could not be parsed
	 * @return the major number
	 */
	public int getMajor() {
		if (elements.length > 0) {
			return elements[0];
		} else {
			return 0;
		}
	}

	/**
	 * Returns the second integer element of this version string,
	 * or 0 if a second integer was not defined or could not be parsed
	 * @return the minor number
	 */
	public int getMinor() {
		if (elements.length > 1) {
			return elements[1];
		} else {
			return 0;
		}
	}

	/**
	 * Returns the third integer element of this version string,
	 * or 0 if a third integer was not defined or could not be parsed
	 * @return the revision number
	 */
	public int getRevision() {
		if (elements.length > 2) {
			return elements[2];
		} else {
			return 0;
		}
	}

	/**
	 * Returns the fourth integer element of this version string,
	 * or 0 if a fourth integer was not defined or could not be parsed
	 * @return the build number
	 */
	public int getBuild() {
		if (elements.length > 3) {
			return elements[3];
		} else {
			return 0;
		}
	}

	/**
	 * Returns the element array with trailing zeros
	 * removed - used to ensure e.g. 2.2 and 2.2.0.0
	 * (normalized to [ 2, 2 ]) have the same hash code.
	 *
	 * If there are trailing zeros, a new element
	 * array with the trailing zeros removed is returned,
	 * otherwise the original element array is returned.
	 *
	 * @return the element array with trailing zeros removed
	 */
	private int[] getCanonicalElements() {
		int[] canonicalElements;
		// this default length is used if a) the loop isn't entered
		// (elements.length == 1) or b) if all trailing digits are 0
		int canonicalElementsLength = 1;

		// find the last (rightmost) non-zero element.
		for (int i = elements.length - 1; i > 0; --i) {
			if (elements[i] != 0) {
				canonicalElementsLength = i + 1;
				break;
			}
		}

		if (canonicalElementsLength == elements.length) {
			canonicalElements = elements;
		} else {
			canonicalElements = new int[canonicalElementsLength];
			System.arraycopy(elements, 0, canonicalElements, 0, canonicalElementsLength);
		}

		return canonicalElements;
	}

	/**
	 * Returns this version's hash code
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(getCanonicalElements());
	}

	/**
	 * Returns a string representation of the version. The format of the string returned is "major.minor.revision.build".
	 *
	 * @return The string representation of the version.
	 */
	// XXX guava (1.8 MB): Ints.join(".", elements)
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		if (elements.length > 0) {
			buf.append(elements[0]);
		}

		for (int i = 1; i < elements.length; ++i) {
			buf.append('.').append(elements[i]);
		}

		return buf.toString();
	}
}
