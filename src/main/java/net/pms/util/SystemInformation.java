/*
 * Universal Media Server, for streaming any medias to DLNA
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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LocationAwareLogger;
import com.sun.jna.Platform;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
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
		GlobalMemory memory = null;
		try {
			SystemInfo systemInfo = new SystemInfo();
			HardwareAbstractionLayer hardware = systemInfo.getHardware();
			os = systemInfo.getOperatingSystem();
			processor = hardware.getProcessor();
			memory = hardware.getMemory();
		} catch (Error e) {
			LOGGER.debug("Could not retrieve system information: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		sb.append("JVM: ").append(System.getProperty("java.vm.name")).append(" ")
			.append(System.getProperty("java.version")).append(" (")
			.append(System.getProperty("sun.arch.data.model")).append("-bit) by ")
			.append(System.getProperty("java.vendor"));
		result.add(sb.toString());
		sb.setLength(0);
		sb.append("OS: ");
		if (os != null && isNotBlank(os.toString())) {
			sb.append(os.toString()).append(" ").append(getOSBitness()).append("-bit");
		} else {
			sb.append(System.getProperty("os.name")).append(" ").append(getOSBitness()).append("-bit ");
			sb.append(System.getProperty("os.version"));
		}
		result.add(sb.toString());
		sb.setLength(0);
		if (processor != null) {
			sb.append("CPU: ").append(processor.getName()).append(" with ")
				.append(processor.getPhysicalProcessorCount());
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

	/**
	 * Determines whether the operating system is 64-bit or 32-bit.
	 *
	 * XXX This will work with Windows and OS X but not necessarily with Linux
	 * as we're relying on Java's {@code os.arch} which only detects the bitness
	 * of the JVM, not of the operating system. If <a
	 * href="https://github.com/oshi/oshi/issues/377">OSHI #377</a> is
	 * implemented, it could be a reliable source for all OSes.
	 *
	 * @return The bitness of the operating system.
	 */
	public static int getOSBitness() {
		if (Platform.isWindows()) {
			return System.getenv("ProgramFiles(x86)") != null ? 64 : 32;
		}
		return Platform.is64Bit() ? 64 : 32;
	}

	@Override
	public void run() {
		LOGGER.trace("Starting gathering of system information");
		logSystemInfo(Level.INFO);
		LOGGER.trace("Done logging system information, shutting down thread");
	}
}
