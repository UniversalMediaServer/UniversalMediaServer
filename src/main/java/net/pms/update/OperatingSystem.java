package net.pms.update;

public class OperatingSystem {
	private static final String platformName = detectPlatform();

	private static String detectPlatform() {
		String fullPlatform = System.getProperty("os.name", "unknown");
		String platform = fullPlatform.split(" ")[0].toLowerCase();
		return platform;
	}

	public String getPlatformName() {
		assert platformName != null;
		return platformName;
	}

	@Override
	public String toString() {
		return getPlatformName();
	}

	public boolean isWindows() {
		return getPlatformName().equals("windows");
	}
}
