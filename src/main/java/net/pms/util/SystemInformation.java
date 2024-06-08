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
package net.pms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.pms.PMS;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

/**
 * This class doubles as a utility class for gathering and logging system
 * information and a {@link Thread} that will do the gathering and logging once
 * when started.
 *
 * @author Nadahar
 */
public class SystemInformation extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemInformation.class);

	/**
	 * Creates a new system information logger thread.
	 */
	public SystemInformation() {
		super("System Information Logger");
	}

	/**
	 * Collects and returns system information.
	 *
	 * @return A {@link List} of {@link String}s containing the collected system
	 *         information.
	 */
	public static List<String> getSystemInfo() {
		List<String> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		long jvmMemory = Runtime.getRuntime().maxMemory();
		OperatingSystem os = null;
		CentralProcessor processor = null;
		ProcessorIdentifier processorIdentifier = null;
		GlobalMemory memory = null;
		List<NetworkIF> networkInterfaces = null;
		try {
			SystemInfo systemInfo = new SystemInfo();
			HardwareAbstractionLayer hardware = systemInfo.getHardware();
			os = systemInfo.getOperatingSystem();
			processor = hardware.getProcessor();
			processorIdentifier = processor.getProcessorIdentifier();
			memory = hardware.getMemory();
			networkInterfaces = hardware.getNetworkIFs();
		} catch (Error e) {
			LOGGER.debug("Could not retrieve system information: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		sb.append("OS: ");
		if (os != null && StringUtils.isNotBlank(os.toString())) {
			sb.append(os.toString()).append(" ").append(os.getBitness()).append("-bit");
		} else {
			sb.append(System.getProperty("os.name")).append(" ").append(PlatformUtils.getOSBitness()).append("-bit ");
			sb.append(System.getProperty("os.version"));
		}
		result.add(sb.toString());
		sb.setLength(0);
		sb.append("JVM: ").append(System.getProperty("java.vm.name")).append(" ")
			.append(System.getProperty("java.version")).append(" (")
			.append(System.getProperty("sun.arch.data.model")).append("-bit) by ")
			.append(System.getProperty("java.vendor"));
		result.add(sb.toString());
		sb.setLength(0);
		sb.append("Language: ")
		.append(WordUtils.capitalize(PMS.getLocale().getDisplayName(Locale.ENGLISH)));
		result.add(sb.toString());
		sb.setLength(0);
		sb.append("Encoding: ")
		.append(System.getProperty("file.encoding"));
		result.add(sb.toString());
		sb.setLength(0);
		if (processor != null && processorIdentifier != null) {
			sb.append("CPU: ").append(processorIdentifier.getName()).append(" with ").append(processor.getPhysicalProcessorCount());
			if (processor.getPhysicalProcessorCount() > 1) {
				sb.append(" cores");
			} else {
				sb.append(" core");
			}
			if (processor.getLogicalProcessorCount() != processor.getPhysicalProcessorCount()) {
				sb.append(" (").append(processor.getLogicalProcessorCount());
				if (processor.getLogicalProcessorCount() > 1) {
					sb.append(" virtual cores)");
				} else {
					sb.append(" virtual core)");
				}
			}
			if (processorIdentifier.getMicroarchitecture() != null) {
				sb.append(" (").append(processorIdentifier.getMicroarchitecture()).append(")");
			}
			result.add(sb.toString());
			sb.setLength(0);
		}
		if (memory != null) {
			sb.append("Physical Memory: ").append(StringUtil.formatBytes(memory.getTotal(), true));
			result.add(sb.toString());
			sb.setLength(0);
			sb.append("Free Memory: ").append(StringUtil.formatBytes(memory.getAvailable(), true));
			result.add(sb.toString());
			sb.setLength(0);

		}
		sb.append("Maximum JVM Memory: ");
		if (jvmMemory == Long.MAX_VALUE) {
			sb.append("Unlimited");
		} else {
			sb.append(StringUtil.formatBytes(jvmMemory, true));
		}
		result.add(sb.toString());
		if (networkInterfaces != null) {
			result.add("Used network interfaces:");
			// count only real interfaces whose received some bytes not the logical ones
			for (NetworkIF net : networkInterfaces) {
				if (net.getBytesRecv() > 0) {
					sb.setLength(0);
					sb.append(net.getDisplayName())
					.append(", speed ")
					.append(net.getSpeed() / 1000000)
					.append(" Mb/s");
					result.add(sb.toString());
				}
			}
		}

		return result;
	}

	/**
	 * Logs relevant system information with the specified {@code logLevel}.
	 *
	 * @param logLevel the {@link ch.qos.logback.classic.Level} to use when
	 *            logging.
	 */
	public static void logSystemInfo(ch.qos.logback.classic.Level logLevel) {
		if (logLevel == null) {
			throw new IllegalArgumentException("logLevel cannot be null");
		}
		logSystemInfo(ch.qos.logback.classic.Level.toLocationAwareLoggerInteger(logLevel));
	}

	/**
	 * Logs relevant system information with the specified {@code logLevel}.
	 *
	 * @param logLevel the {@link Level} to use when logging.
	 */
	public static void logSystemInfo(Level logLevel) {
		if (logLevel == null) {
			throw new IllegalArgumentException("logLevel cannot be null");
		}
		logSystemInfo(logLevel.toInt());
	}

	/**
	 * Logs relevant system information with the specified {@code logLevel}.
	 *
	 * @param locationAwareLogLevel the {@link LocationAwareLogger} integer
	 *            constant to use when logging.
	 */
	public static void logSystemInfo(int locationAwareLogLevel) {
		StringBuilder systemInfo = new StringBuilder("System information:\n");
		for (String s : getSystemInfo()) {
			systemInfo.append("  ").append(s).append("\n");
		}
		((ch.qos.logback.classic.Logger) LOGGER).log(
			null,
			SystemInformation.class.getName(),
			locationAwareLogLevel,
			systemInfo.toString(),
			null,
			null
		);
	}

	@Override
	public void run() {
		LOGGER.trace("Starting gathering of system information");
		logSystemInfo(Level.INFO);
		LOGGER.trace("Done logging system information, shutting down thread");
	}
}
