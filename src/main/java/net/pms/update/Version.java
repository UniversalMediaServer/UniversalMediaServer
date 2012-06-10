package net.pms.update;

class Version {
	private final int[] elements;

	Version(String versionNumberAsString) {
		elements = parseNumbers(separateElements(versionNumberAsString));
	}

	private static int[] parseNumbers(String[] elements) {
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

	private static String[] separateElements(String versionNumberAsString) {
		if (versionNumberAsString != null) {
			return versionNumberAsString.split("\\.");
		} else {
			return new String[0];
		}
	}

	public boolean isGreaterThan(Version other) {
		for (int i = 0; i < Math.min(elements.length, other.elements.length); i++) {
			if (elements[i] > other.elements[i]) {
				return true;
			} else if (elements[i] < other.elements[i]) {
				return false;
			}
		}
		return elements.length > other.elements.length;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buf.append(elements[i]);
			if (i != elements.length - 1) {
				buf.append(".");
			}
		}
		return buf.toString();
	}
}
