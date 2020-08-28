/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.io;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.LongByReference;
import java.io.File;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import net.pms.util.FileUtil;
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the Windows specific native functionality. Do not try to instantiate on Linux/MacOS X !
 *
 * @author zsombor
 */
public class WinUtils extends BasicSystemUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(WinUtils.class);

	public interface Kernel32 extends Library {
		Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
		Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

		int GetShortPathNameW(WString lpszLongPath, char[] lpdzShortPath, int cchBuffer);

		int GetWindowsDirectoryW(char[] lpdzShortPath, int uSize);

		boolean GetVolumeInformationW(
			char[] lpRootPathName,
			CharBuffer lpVolumeNameBuffer,
			int nVolumeNameSize,
			LongByReference lpVolumeSerialNumber,
			LongByReference lpMaximumComponentLength,
			LongByReference lpFileSystemFlags,
			CharBuffer lpFileSystemNameBuffer,
			int nFileSystemNameSize
		);

		int SetThreadExecutionState(int EXECUTION_STATE);
		int ES_CONTINUOUS        = 0x80000000;
		int ES_SYSTEM_REQUIRED   = 0x00000001;
		int ES_DISPLAY_REQUIRED  = 0x00000002;
		int ES_AWAYMODE_REQUIRED = 0x00000040;

		int GetACP();
		int GetOEMCP();
		int GetConsoleOutputCP();
	}

	private final boolean kerio;
	protected final Path psPing;
	protected final String avsPluginsFolder;
	protected final String kLiteFiltersDir;

	@Override
	public File getAvsPluginsDir() {
		if (avsPluginsFolder == null) {
			return null;
		}
		File pluginsDir = new File(avsPluginsFolder);
		if (!pluginsDir.exists()) {
			pluginsDir = null;
		}
		return pluginsDir;
	}

	/**
	 * The Filters directory for K-Lite Codec Pack, which contains vsfilter.dll
	 */
	@Override
	public File getKLiteFiltersDir() {
		if (kLiteFiltersDir == null) {
			return null;
		}
		File filtersDir = new File(kLiteFiltersDir + "\\Filters");
		if (!filtersDir.exists()) {
			filtersDir = null;
		}
		return filtersDir;
	}

	@Override
	public String getShortPathNameW(String longPathName) {
		if (longPathName == null) {
			return null;
		}
		boolean unicodeChars;
		try {
			byte b1[] = longPathName.getBytes("UTF-8");
			byte b2[] = longPathName.getBytes("cp1252");
			unicodeChars = b1.length != b2.length;
		} catch (Exception e) {
			return longPathName;
		}

		if (unicodeChars) {
			try {
				WString pathname = new WString(longPathName);

				char test[] = new char[2 + pathname.length() * 2];
				int r = Kernel32.INSTANCE.GetShortPathNameW(pathname, test, test.length);
				if (r > 0) {
					String result = Native.toString(test);
					LOGGER.trace("Using short path name of \"{}\": \"{}\"", pathname, result);
					return result;
				}
				LOGGER.debug("Can't find \"{}\"", pathname);
				return null;

			} catch (Exception e) {
				return longPathName;
			}
		}
		return longPathName;
	}

	@Override
	public String getWindowsDirectory() {
		char test[] = new char[2 + 256 * 2];
		int r = Kernel32.INSTANCE.GetWindowsDirectoryW(test, 256);
		if (r > 0) {
			return Native.toString(test);
		}
		return null;
	}

	@Override
	public String getDiskLabel(File f) {
		String driveName;
		try {
			driveName = f.getCanonicalPath().substring(0, 2) + "\\";

			char[] lpRootPathName_chars = new char[4];
			for (int i = 0; i < 3; i++) {
				lpRootPathName_chars[i] = driveName.charAt(i);
			}
			lpRootPathName_chars[3] = '\0';
			int nVolumeNameSize = 256;
			CharBuffer lpVolumeNameBuffer_char = CharBuffer.allocate(nVolumeNameSize);
			LongByReference lpVolumeSerialNumber = new LongByReference();
			LongByReference lpMaximumComponentLength = new LongByReference();
			LongByReference lpFileSystemFlags = new LongByReference();
			int nFileSystemNameSize = 256;
			CharBuffer lpFileSystemNameBuffer_char = CharBuffer.allocate(nFileSystemNameSize);

			boolean result2 = Kernel32.INSTANCE.GetVolumeInformationW(
				lpRootPathName_chars,
				lpVolumeNameBuffer_char,
				nVolumeNameSize,
				lpVolumeSerialNumber,
				lpMaximumComponentLength,
				lpFileSystemFlags,
				lpFileSystemNameBuffer_char,
				nFileSystemNameSize);
			if (!result2) {
				return null;
			}
			String diskLabel = charString2String(lpVolumeNameBuffer_char);
			return diskLabel;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * For description see <a HREF=
	 * "https://msdn.microsoft.com/en-us/library/windows/desktop/dd318070%28v=vs.85%29.aspx"
	 * >MSDN GetACP</a>
	 *
	 * @return the value from Windows API GetACP()
	 */
	public static int getACP() {
		return Kernel32.INSTANCE.GetACP();
	}

	/**
	 * For description see <a HREF=
	 * "https://msdn.microsoft.com/en-us/library/windows/desktop/dd318114%28v=vs.85%29.aspx"
	 * >MSDN GetOEMCP</a>
	 *
	 * @return the value from Windows API GetOEMCP()
	 */
	public static int getOEMCP() {
		return Kernel32.INSTANCE.GetOEMCP();
	}

	/**
	 * @return The result of {@link #getOEMCP()} converted to a {@link Charset}
	 *         or {@code null} if it couldn't be converted.
	 */
	public static Charset getOEMCharset() {
		int codepage = Kernel32.INSTANCE.GetOEMCP();
		Charset result = null;
		String[] aliases = {"cp" + codepage, "MS" + codepage};
		for (String alias : aliases) {
			try {
				result = Charset.forName(alias);
				break;
			} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
				result = null;
			}
		}
		return result;
	}

	/**
	 * @return The result from the Windows API {@code GetConsoleOutputCP()}.
	 */
	public static int getConsoleOutputCP() {
		return Kernel32.INSTANCE.GetConsoleOutputCP();
	}

	private static String charString2String(CharBuffer buf) {
		char[] chars = buf.array();
		int i;
		for (i = 0; i < chars.length; i++) {
			if (chars[i] == '\0') {
				break;
			}
		}
		return new String(chars, 0, i);
	}

	/** Only to be instantiated by {@link BasicSystemUtils#createInstance()}. */
	protected WinUtils() {
		getVLCRegistryInfo();
		avsPluginsFolder = getAviSynthPluginsFolder();
		aviSynth = avsPluginsFolder != null;
		kLiteFiltersDir = getKLiteFiltersFolder();
		kerio = isKerioInstalled();
		psPing = findPsPing();
	}

	protected void getVLCRegistryInfo() {
		String key = "SOFTWARE\\VideoLAN\\VLC";
		try {
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
				key = "SOFTWARE\\Wow6432Node\\VideoLAN\\VLC";
				if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
					return;
				}
			}
			vlcPath = Paths.get(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, ""));
			vlcVersion = new Version(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, "Version"));
		} catch (Win32Exception e) {
			LOGGER.debug("Could not get VLC information from Windows registry: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	protected String getAviSynthPluginsFolder() {
		String key = "SOFTWARE\\AviSynth";
		try {
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
				key = "SOFTWARE\\Wow6432Node\\AviSynth";
				if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
					return null;
				}
			}
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, "plugindir2_5");
		} catch (Win32Exception e) {
			LOGGER.debug("Could not get AviSynth information from Windows registry: {}", e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	protected String getKLiteFiltersFolder() {
		String key = "SOFTWARE\\Wow6432Node\\KLCodecPack";
		try {
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
				key = "SOFTWARE\\KLCodecPack";
				if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
					return null;
				}
			}
			return Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, key, "installdir");
		} catch (Win32Exception e) {
			LOGGER.debug("Could not get K-Lite Codec Pack information from Windows registry: {}", e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	protected boolean isKerioInstalled() {
		try {
			String key = "SOFTWARE\\Kerio";
			if (!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key)) {
				key = "SOFTWARE\\Wow6432Node\\Kerio";
				return Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, key);
			}
			return true;
		} catch (Win32Exception e) {
			LOGGER.debug("Could not get Kerio information from Windows registry: {}", e.getMessage());
			LOGGER.trace("", e);
			return false;
		}
	}

	protected Path findPsPing() {
		// PsPing
		Path tmpPsPing = null;
		tmpPsPing = FileUtil.findExecutableInOSPath(Paths.get("psping64.exe"));
		if (tmpPsPing == null) {
			tmpPsPing = FileUtil.findExecutableInOSPath(Paths.get("psping.exe"));
		}
		return tmpPsPing;
	}

	@Override
	public boolean isKerioFirewall() {
		return kerio;
	}

	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		if (psPing != null) {
			return new String[] {
				psPing.toString(),
				"-w", // warmup
				"0",
				"-i", // interval
				"0",
				"-n", // count
				Integer.toString(count),
				"-l", // size
				Integer.toString(packetSize),
				hostAddress
			};
		}
		return new String[] {
			"ping",
			"-n", // count
			Integer.toString(count),
			"-l", // size
			Integer.toString(packetSize),
			hostAddress
		};
	}

	@Override
	public String parsePingLine(String line) {
		if (psPing != null) {
			int msPos = line.indexOf("ms");

			if (msPos == -1) {
				return null;
			}
			return line.substring(line.lastIndexOf(':', msPos) + 1, msPos).trim();
		}
		return super.parsePingLine(line);
	}

	@Override
	@Nullable
	public Double getWindowsVersion() {
		if (!Platform.isWindows()) {
			return null;
		}
		try {
			return Double.valueOf(System.getProperty("os.version"));
		} catch (NullPointerException | NumberFormatException e) {
			return null;
		}
	}
}
