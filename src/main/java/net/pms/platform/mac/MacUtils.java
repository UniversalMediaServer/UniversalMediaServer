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
package net.pms.platform.mac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.platform.PlatformUtils;
import net.pms.service.sleep.AbstractSleepWorker;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mac specific platform code.
 * Only to be instantiated by {@link PlatformUtils#createInstance()}.
 */
public class MacUtils extends PlatformUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacUtils.class);

	@Override
	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		return false;
	}

	/**
	 * Fetch the hardware address for a network interface.
	 *
	 * @param ni Interface to fetch the MAC address for
	 * @return the MAC address as bytes, or null if it couldn't be fetched.
	 * @throws SocketException
	 *         This won't happen on OS X, since the NetworkInterface is
	 *         only used to get a name.
	 */
	@Override
	public byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
		// On Mac OS X, fetch the hardware address from the command line tool "ifconfig".
		byte[] aHardwareAddress = null;

		try {
			Process process = Runtime.getRuntime().exec(new String[] {"ifconfig", ni.getName(), "ether" });
			List<String> lines;
			try (InputStream inputStream = process.getInputStream()) {
				lines = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
			}
			String aMacStr = null;
			Pattern aMacPattern = Pattern.compile("\\s*ether\\s*([a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2})");

			if (lines != null) {
				for (String line : lines) {
					Matcher aMacMatcher = aMacPattern.matcher(line);

					if (aMacMatcher.find()) {
						aMacStr = aMacMatcher.group(1);
						break;
					}
				}
			}

			if (aMacStr != null) {
				String[] aComps = aMacStr.split(":");
				aHardwareAddress = new byte[aComps.length];

				for (int i = 0; i < aComps.length; i++) {
					String aComp = aComps[i];
					aHardwareAddress[i] = (byte) Short.valueOf(aComp, 16).shortValue();
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to execute ifconfig", e);
		}

		return aHardwareAddress;
	}

	/**
	 * Return the platform specific ping command for the given host address,
	 * ping count and packet size. macOS has a maximum UDP packet size of ~8400
	 * bytes, so this method will divide packets of larger sizes into multiple
	 * packets to "simulate" the effect.
	 *
	 * @param hostAddress the host address.
	 * @param count the ping count.
	 * @param packetSize the packet size.
	 * @return The ping command.
	 */
	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		if (packetSize > 8000) {
			int divisor = getPingPacketDivisor(packetSize);
			packetSize /= divisor;
			count *= divisor;
		}
		return new String[] {
			"ping", /* count */ "-c", Integer.toString(count),
			/* delay */ "-i", "0.1",
			/* size */ "-s", Integer.toString(packetSize),
			hostAddress
		};
	}

	@Override
	public String parsePingLine(String line) {
		int msPos = line.indexOf("ms");
		String timeString = null;

		if (msPos > -1) {
			if (line.lastIndexOf('<', msPos) > -1) {
				timeString = "0.5";
			} else {
				timeString = line.substring(line.lastIndexOf('=', msPos) + 1, msPos).trim();

			}
		}
		// Avoid returning the macOS ping statistics
		return timeString != null && timeString.contains("/") ? null : timeString;
	}

	@Override
	public int getPingPacketFragments(int packetSize) {
		if (packetSize > 8000) {
			packetSize /= getPingPacketDivisor(packetSize);
		}
		return ((packetSize + 8) / 1500) + 1;
	}

	@Override
	public boolean isTsMuxeRCompatible() {
		// no tsMuxeR for 10.4 (yet?)
		return !(System.getProperty("os.version") != null && System.getProperty("os.version").contains("10.4."));
	}

	@Override
	public String getiTunesFile() throws IOException, URISyntaxException {
		// the second line should contain a quoted file URL e.g.:
		// "file://localhost/Users/MyUser/Music/iTunes/iTunes%20Music%20Library.xml"
		Process process = Runtime.getRuntime().exec("defaults read com.apple.iApps iTunesRecentDatabases");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			// we want the 2nd line
			String line1 = in.readLine();
			String line2 = in.readLine();
			if (line1 != null && line2 != null) {
				String line = line2.trim(); // remove extra spaces
				line = line.substring(1, line.length() - 1); // remove quotes and spaces
				URI tURI = new URI(line);
				return URLDecoder.decode(tURI.toURL().getFile(), "UTF8");
			}
		}
		return null;
	}

	@Override
	public List<Path> getDefaultFolders() {
		synchronized (DEFAULT_FOLDERS_LOCK) {
			if (defaultFolders == null) {
				// Lazy initialization
				List<Path> result = new ArrayList<>();
				result.addAll(NSFoundation.nsSearchPathForDirectoriesInDomains(
					NSFoundation.NSSearchPathDirectory.NSMoviesDirectory,
					NSFoundation.NSSearchPathDomainMask.NSAllDomainsMask,
					true
				));
				result.addAll(NSFoundation.nsSearchPathForDirectoriesInDomains(
					NSFoundation.NSSearchPathDirectory.NSMusicDirectory,
					NSFoundation.NSSearchPathDomainMask.NSAllDomainsMask,
					true
				));
				result.addAll(NSFoundation.nsSearchPathForDirectoriesInDomains(
					NSFoundation.NSSearchPathDirectory.NSPicturesDirectory,
					NSFoundation.NSSearchPathDomainMask.NSAllDomainsMask,
					true
				));
				defaultFolders = Collections.unmodifiableList(result);
			}
			return defaultFolders;
		}
	}

	@Override
	public boolean isAdmin() {
		synchronized (IS_ADMIN_LOCK) {
			if (isAdmin != null) {
				return isAdmin;
			}
			try {
				final String command = "id -Gn";
				LOGGER.trace("isAdmin: Executing \"{}\"", command);
				Process p = Runtime.getRuntime().exec(command);
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII);
				int exitValue;
				String exitLine;
				try (BufferedReader br = new BufferedReader(isr)) {
					p.waitFor();
					exitValue = p.exitValue();
					exitLine = br.readLine();
				}

				if (exitValue != 0 || exitLine == null || exitLine.isEmpty()) {
					LOGGER.error("Could not determine admin privileges, \"{}\" ended with exit code: {}", command, exitValue);
					isAdmin = false;
					return false;
				}

				LOGGER.trace("isAdmin: \"{}\" returned {}", command, exitLine);
				if (exitLine.matches(".*\\badmin\\b.*")) {
					LOGGER.trace("isAdmin: UMS has admin privileges");
					isAdmin = true;
					return true;
				}

				LOGGER.trace("isAdmin: UMS does not have admin privileges");
				isAdmin = false;
				return false;
			} catch (IOException | InterruptedException e) {
				LOGGER.error(
					"An error prevented UMS from checking macOS permissions: {}",
					e.getMessage()
				);
			}
			isAdmin = false;
			return false;
		}
	}

	@Override
	public String getDefaultFontPath() {
		// get default osx font
		return getAbsolutePath("/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/fonts/", "LucidaSansRegular.ttf");
	}

	@Override
	public boolean isPreventSleepSupported() {
		return isMacOsVersionEqualOrGreater("10.5.0");
	}

	@Override
	public AbstractSleepWorker getSleepWorker(SleepManager owner, PreventSleepMode mode) {
		return new MacSleepWorker(owner, mode);
	}

	@Override
	protected String getTrayIcon() {
		if (isDarkMode()) {
			return "icon-darkmode.png";
		} else {
			return "icon-22.png";
		}
	}

	@Override
	public List<String> getRestartCommand(boolean hasOptions) {
		String libraryPath = ManagementFactory.getRuntimeMXBean().getLibraryPath();
		if (StringUtils.isNotBlank(libraryPath)) {
			Pattern pattern = Pattern.compile("(.+?\\.app)/Contents/MacOS");
			Matcher matcher = pattern.matcher(libraryPath);
			if (matcher.find()) {
				String macAppPath = matcher.group(1);
				if (StringUtils.isNotBlank(macAppPath)) {
					List<String> restart = new ArrayList<>();
					restart.add("open");
					restart.add("-n");
					restart.add("-a");
					restart.add(macAppPath);
					if (hasOptions) {
						restart.add("--args");
					}
					return restart;
				}
			}
		}
		return getUMSCommand();
	}

	@Override
	public String getShutdownCommand() {
		return "shutdown -h now";
	}

	/**
	 * @return whether the computer is running in dark mode
	 */
	private static boolean isDarkMode() {
		try {
			// check for exit status only. Once there are more modes than "dark" and "default", we might need to analyze string contents..
			final Process proc = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
			proc.waitFor(100, TimeUnit.MILLISECONDS);
			return proc.exitValue() == 0;
		} catch (IOException | InterruptedException | IllegalThreadStateException ex) {
			// IllegalThreadStateException thrown by proc.exitValue(), if process didn't terminate
			LOGGER.warn("Could not determine whether 'dark mode' is being used. Falling back to default (light) mode.");
			LOGGER.debug("" + ex);
			return false;
		}
	}

	/**
	 * macOS has a default maximum UDP packet size of ~8400 bytes. This method
	 * will divide packets of larger size into multiple packets to "simulate"
	 * the effect.
	 *
	 * @param packetSize the packet size.
	 * @return The divisor with which to device the packet size and multiply the
	 *         count.
	 */
	private static int getPingPacketDivisor(int packetSize) {
		return (int) Math.ceil(packetSize / 8000.0);
	}

	/**
	 * Determines if the current macOS version is of a version equal or greater
	 * to the argument.
	 *
	 * @param version the version to evaluate.
	 * @return whether the current version is at least the specified version.
	 */
	public static boolean isMacOsVersionEqualOrGreater(String version) {
		return OS_VERSION.isGreaterThanOrEqualTo(version);
	}

}
