package net.pms.util;

import org.apache.commons.lang.StringUtils;

public class Version {
	private final int[] components;
	private final String versionString;

	public Version(String versionString) {
		if (StringUtils.isBlank(versionString)) {
			versionString = "0";
		}

		this.versionString = versionString;
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

	public boolean isGreaterThan(Version other) {
		for (int i = 0; i < Math.min(components.length, other.components.length); i++) {
			if (components[i] > other.components[i]) {
				return true;
			} else if (components[i] < other.components[i]) {
				return false;
			}
		}

		return components.length > other.components.length;
	}

	private Integer getMostSignificantDigit(int i) {
		int msd = 0;
		String msdString = Integer.toString(i).substring(0, 1);

		try {
			msd = Integer.parseInt(msdString);
		} catch (NumberFormatException e) { }

		return msd;
	}

	/**
     * Compares this Version object, representing the current version of PMS, to
	 * the supplied Version object, representing the latest version of PMS according
	 * to the update file. Returns true if the current version can be upgraded
	 * to the latest version. See src/main/external-resources/update/README
	 * for the criteria.
     * @param other The latest version
     * @return <code>true</code> if this version can safely be upgraded, <code>false</code> otherwise.
     */
	// this should really be a private method in AutoUpdater but it's vital that it's tested, and it's easy to test here
	public boolean isPmsCompatible(Version other) {
		if ((this.getMajor() == other.getMajor()) && (getMostSignificantDigit(this.getMinor()) == getMostSignificantDigit(other.getMinor()))) {
			return true;
		} else {
			return false;
		}
	}

	public Integer getMajor() {
		if (components.length > 0) {
			return components[0];
		} else {
			return 0;
		}
	}

	public Integer getMinor() {
		if (components.length > 1) {
			return components[1];
		} else {
			return 0;
		}
	}

	public Integer getPatch() {
		if (components.length > 2) {
			return components[2];
		} else {
			return 0;
		}
	}

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
