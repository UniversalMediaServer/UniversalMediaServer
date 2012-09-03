package net.pms.util;

import org.apache.commons.lang.StringUtils;

public class Version implements Comparable<Version> {
	private final int[] components;

	public Version(String versionString) {
		if (StringUtils.isBlank(versionString)) {
			versionString = "0";
		}

		components = parseNumbers(versionString.split("\\."));
	}

	private int[] parseNumbers(String[] components) {
		int[] out = new int[components.length];

		for (int i = 0; i < components.length; i++) {
			try {
				out[i] = Integer.parseInt(components[i]);
			} catch (NumberFormatException e) {
				out[i] = 0;
			}
		}

		return out;
	}

	public int compareTo(Version other) {
		final int[] longerComponents, shorterComponents;;
		final int sign;

		if (components.length >= other.components.length) {
			longerComponents = components;
			shorterComponents = other.components;
			sign = 1;
		} else {
			longerComponents = other.components;
			shorterComponents = components;
			sign = -1;
		}

		for (int i = 0; i < longerComponents.length; ++i) {
			int val = i < shorterComponents.length ? shorterComponents[i] : 0;

			if (longerComponents[i] != val) {
				return (longerComponents[i] - val) * sign;
			}
		}

		return 0;
	}

	public boolean equals(Version other) {
		return compareTo(other) == 0;
	}

	public boolean isLessThan(Version other) {
		return compareTo(other) < 0;
	}

	public boolean isLessThanOrEqualTo(Version other) {
		return compareTo(other) <= 0;
	}

	public boolean isGreaterThan(Version other) {
		return compareTo(other) > 0;
	}

	public boolean isGreaterThanOrEqualTo(Version other) {
		return compareTo(other) >= 0;
	}

	static private int getMostSignificantDigit(int i) {
		int msd = 0;
		String msdString = Integer.toString(i).substring(0, 1);

		try {
			msd = Integer.parseInt(msdString);
		} catch (NumberFormatException e) { }

		return msd;
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
	// this should really be a private method in AutoUpdater but it's vital that it's tested, and it's easy to test here
	static public boolean isPmsUpdatable(Version vFrom, Version vTo) {
		return vTo.isGreaterThan(vFrom)
			&& (vFrom.getMajor() == vTo.getMajor())
			&& (getMostSignificantDigit(vFrom.getMinor()) == getMostSignificantDigit(vTo.getMinor()));
	}

	public int getMajor() {
		if (components.length > 0) {
			return components[0];
		} else {
			return 0;
		}
	}

	public int getMinor() {
		if (components.length > 1) {
			return components[1];
		} else {
			return 0;
		}
	}

	public int getPatch() {
		if (components.length > 2) {
			return components[2];
		} else {
			return 0;
		}
	}

	/**
	 * Returns a string representation of the version. The format of the string returned is "major.minor.patch". 
	 *
	 * @return The string representation of the version.
	 */
	// TODO: hashCode
	// XXX guava (1.8 MB): Ints.join(".", components)
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		if (components.length > 0) {
			buf.append(components[0]);
		}

		for (int i = 1; i < components.length; ++i) {
			buf.append(".");
			buf.append(components[i]);
		}

		return buf.toString();
	}
}
