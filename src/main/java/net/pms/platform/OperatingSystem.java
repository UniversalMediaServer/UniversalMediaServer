/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.platform;

public class OperatingSystem {
	private static final String PLATFORM_NAME = detectPlatform();
	private static final String PLATFORM_ARCHITECTURE = detectArchitecture();

	private static String detectPlatform() {
		String fullPlatform = System.getProperty("os.name", "unknown");
		return fullPlatform.split(" ")[0].toLowerCase();
	}

	/**
	 * Detects the operating system architecture.
	 * Logic copied from https://github.com/bytedeco/javacpp/blob/abe3cf003b8edfca0802e9869028cf5065c02874/src/main/java/org/bytedeco/javacpp/Loader.java#L93
	 *
	 * @return architecture in a format that matches our release packages
	 */
	private static String detectArchitecture() {
		String osArch  = System.getProperty("os.arch", "").toLowerCase();
		String abiType = System.getProperty("sun.arch.abi", "").toLowerCase();
		String libPath = System.getProperty("sun.boot.library.path", "").toLowerCase();

		if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
			osArch = "x86";
		} else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
			osArch = "x86_64";
		} else if (osArch.startsWith("aarch64") || osArch.startsWith("armv8") || osArch.startsWith("arm64")) {
			osArch = "arm64";
		} else if ((osArch.startsWith("arm")) && ((abiType.equals("gnueabihf")) || (libPath.contains("openjdk-armhf")))) {
			osArch = "armhf";
		} else if (osArch.startsWith("arm")) {
			osArch = "armel";
		}

		return osArch;
	}

	public String getPlatformName() {
		assert PLATFORM_NAME != null;
		return PLATFORM_NAME;
	}

	public String getPlatformArchitecture() {
		assert PLATFORM_ARCHITECTURE != null;
		return PLATFORM_ARCHITECTURE;
	}

	@Override
	public String toString() {
		return getPlatformName() + "-" + getPlatformArchitecture();
	}
}
