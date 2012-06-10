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

import java.io.File;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.prefs.Preferences;

import net.pms.PMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.LongByReference;

/**
 * Contains the Windows specific native functionality. Do not try to instantiate on Linux/MacOSX !
 * @author zsombor
 *
 */
public class WinUtils extends BasicSystemUtils implements SystemUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(WinUtils.class);

	public interface Kernel32 extends Library {
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32",
			Kernel32.class);
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
		int ES_DISPLAY_REQUIRED = 0x00000002;
		int ES_SYSTEM_REQUIRED = 0x00000001;
		int ES_CONTINUOUS = 0x80000000;
	}
	private static final int KEY_READ = 0x20019;
	private boolean kerio;
	private String avsPluginsDir;
	public long lastDontSleepCall = 0;
	public long lastGoToSleepCall = 0;

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#disableGoToSleep()
	 */
	@Override
	public void disableGoToSleep() {
		// Disable go to sleep (every 40s)
		if (PMS.getConfiguration().isPreventsSleep()
				&& System.currentTimeMillis() - lastDontSleepCall > 40000) {
			LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);
			lastDontSleepCall = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#reenableGoToSleep()
	 */
	@Override
	public void reenableGoToSleep() {
		// Reenable go to sleep
		if (PMS.getConfiguration().isPreventsSleep()
				&& System.currentTimeMillis() - lastGoToSleepCall > 40000) {
			LOGGER.trace("Calling SetThreadExecutionState ES_CONTINUOUS");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
			lastGoToSleepCall = System.currentTimeMillis();
		}
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#getAvsPluginsDir()
	 */
	@Override
	public File getAvsPluginsDir() {
		if (avsPluginsDir == null) {
			return null;
		}
		File pluginsDir = new File(avsPluginsDir);
		if (!pluginsDir.exists()) {
			pluginsDir = null;
		}
		return pluginsDir;
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#getShortPathNameW(java.lang.String)
	 */
	@Override
	public String getShortPathNameW(String longPathName) {
		boolean unicodeChars = false;
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
					LOGGER.debug("Forcing short path name on " + pathname);
					return Native.toString(test);
				} else {
					LOGGER.info("File does not exist? " + pathname);
					return null;
				}

			} catch (Exception e) {
				return longPathName;
			}
		}
		return longPathName;
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#getWindowsDirectory()
	 */
	@Override
	public String getWindowsDirectory() {
		char test[] = new char[2 + 256 * 2];
		int r = Kernel32.INSTANCE.GetWindowsDirectoryW(test, 256);
		if (r > 0) {
			return Native.toString(test);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#getDiskLabel(java.io.File)
	 */
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

	private String charString2String(CharBuffer buf) {
		char[] chars = buf.array();
		int i;
		for (i = 0; i < chars.length; i++) {
			if (chars[i] == '\0') {
				break;
			}
		}
		return new String(chars, 0, i);
	}

	public WinUtils() {
		start();
	}

	private void start() {
		final Preferences userRoot = Preferences.userRoot();
		final Preferences systemRoot = Preferences.systemRoot();
		final Class<? extends Preferences> clz = userRoot.getClass();
		try {
			if (clz.getName().endsWith("WindowsPreferences")) {
				final Method openKey = clz.getDeclaredMethod("WindowsRegOpenKey", int.class,
					byte[].class, int.class);
				openKey.setAccessible(true);
				final Method closeKey = clz.getDeclaredMethod(
					"WindowsRegCloseKey", int.class);
				closeKey.setAccessible(true);
				final Method winRegQueryValue = clz.getDeclaredMethod(
					"WindowsRegQueryValueEx", int.class, byte[].class);
				winRegQueryValue.setAccessible(true);
				byte[] valb = null;
				String key = null;
				key = "SOFTWARE\\VideoLAN\\VLC";
				int handles[] = (int[]) openKey.invoke(systemRoot, -2147483646,
					toCstr(key), KEY_READ);
				if (!(handles.length == 2 && handles[0] != 0 && handles[1] == 0)) {
					key = "SOFTWARE\\Wow6432Node\\VideoLAN\\VLC";
					handles = (int[]) openKey.invoke(systemRoot, -2147483646,
						toCstr(key), KEY_READ);
				}
				if (handles.length == 2 && handles[0] != 0 && handles[1] == 0) {
					valb = (byte[]) winRegQueryValue.invoke(systemRoot,
						handles[0], toCstr(""));
					vlcp = (valb != null ? new String(valb).trim() : null);
					valb = (byte[]) winRegQueryValue.invoke(systemRoot,
						handles[0], toCstr("Version"));
					vlcv = (valb != null ? new String(valb).trim() : null);
					closeKey.invoke(systemRoot, handles[0]);
				}
				key = "SOFTWARE\\AviSynth";
				handles = (int[]) openKey.invoke(systemRoot, -2147483646,
					toCstr(key), KEY_READ);
				if (!(handles.length == 2 && handles[0] != 0 && handles[1] == 0)) {
					key = "SOFTWARE\\Wow6432Node\\AviSynth";
					handles = (int[]) openKey.invoke(systemRoot, -2147483646,
						toCstr(key), KEY_READ);
				}
				if (handles.length == 2 && handles[0] != 0 && handles[1] == 0) {
					avis = true;
					valb = (byte[]) winRegQueryValue.invoke(systemRoot,
						handles[0], toCstr("plugindir2_5"));
					avsPluginsDir = (valb != null ? new String(valb).trim() : null);
					closeKey.invoke(systemRoot, handles[0]);
				}
				key = "SOFTWARE\\Kerio";
				handles = (int[]) openKey.invoke(systemRoot, -2147483646,
					toCstr(key), KEY_READ);
				if (handles.length == 2 && handles[0] != 0 && handles[1] == 0) {
					kerio = true;
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	/* (non-Javadoc)
	 * @see net.pms.io.SystemUtils#isKerioFirewall()
	 */
	@Override
	public boolean isKerioFirewall() {
		return kerio;
	}

	private static byte[] toCstr(String str) {
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}

	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		return new String[] { "ping", /* count */ "-n" , Integer.toString(count), /* size */ "-l", Integer.toString(packetSize), hostAddress };
	}
}
