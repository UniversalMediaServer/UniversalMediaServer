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
package net.pms.update;

import java.io.IOException;
import net.pms.util.PmsProperties;
import net.pms.util.Version;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

/**
 * Data provided by the server for us to update with.  Must be synchronized externally.
 *
 * @author Tim Cox (mail@tcox.org)
 */
public class AutoUpdaterServerProperties {
	private static final String KEY_LATEST_VERSION = "LatestVersion2";
	private static final String DEFAULT_LATEST_VERSION = "0";
	private static final String KEY_DOWNLOAD_URL = "DownloadUrl2";
	private static final String DEFAULT_DOWNLOAD_URL = "";
	private final PmsProperties properties = new PmsProperties();
	private final OperatingSystem operatingSystem = new OperatingSystem();

	public void loadFrom(byte[] data) throws IOException {
		properties.clear();
		properties.loadFromByteArray(data);
	}

	public boolean isStateValid() {
		return getDownloadUrl().length() > 0 && getLatestVersion().isGreaterThan(new Version("0"));
	}

	public Version getLatestVersion() {
		return new Version(getStringWithDefault(KEY_LATEST_VERSION, DEFAULT_LATEST_VERSION));
	}

	public String getDownloadUrl() {
		return getStringWithDefault(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL);
	}

	private String getStringWithDefault(String key, String defaultValue) {
		String platformSpecificKey = getPlatformSpecificKey(key);
		if (properties.containsKey(platformSpecificKey)) {
			return properties.get(platformSpecificKey);
		} else if (properties.containsKey(key)) {
			return properties.get(key);
		} else {
			return defaultValue;
		}
	}

	private String getPlatformSpecificKey(String key) {
		String os = operatingSystem.toString();
		if (os.startsWith("windows")) {
			os = operatingSystem.getPlatformName();
		} else if (os.startsWith("mac")) {
			os = operatingSystem.getPlatformName();

			String osVersionRaw = System.getProperty("os.version");
			Version osVersion = new Version(osVersionRaw);
			boolean isMacOSPreCatalina = osVersion.isLessThan(new Version("10.15"));

			SystemInfo systemInfo = new SystemInfo();
			HardwareAbstractionLayer hardware = systemInfo.getHardware();
			CentralProcessor processor = hardware.getProcessor();
			ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
			String microarchitecture = processorIdentifier.getMicroarchitecture();
			if (isMacOSPreCatalina) {
				os += "-pre10.15";
			} else if (microarchitecture.startsWith("ARM64")) {
				os += "-arm";
			}
		}

		return key + "." + os;
	}
}
