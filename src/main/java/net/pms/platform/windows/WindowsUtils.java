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
package net.pms.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.VerRsrc;
import com.sun.jna.platform.win32.VersionUtil;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.LongByReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.pms.PMS;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.platform.PlatformProgramPaths;
import net.pms.platform.PlatformUtils;
import net.pms.service.process.ProcessManager;
import net.pms.service.process.AbstractProcessTerminator;
import net.pms.service.sleep.AbstractSleepWorker;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import net.pms.util.FilePermissions;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.Version;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Windows specific platform code.
 * Only to be instantiated by {@link PlatformUtils#createInstance()}.
 *
 * Contains the Windows specific native functionality.
 * Do not try to instantiate on Linux/Mac !
 */
public class WindowsUtils extends PlatformUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsUtils.class);
	private final Charset consoleCharset;

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
	 * The Filters directory for K-Lite Codec Pack, which contains vsfilter.dll.
	 * @return K-Lite Codec directory
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
			byte[] b1 = longPathName.getBytes(StandardCharsets.UTF_8);
			byte[] b2 = longPathName.getBytes("cp1252");
			unicodeChars = b1.length != b2.length;
		} catch (UnsupportedEncodingException e) {
			return longPathName;
		}

		if (unicodeChars) {
			try {
				WString pathname = new WString(longPathName);

				char[] test = new char[2 + pathname.length() * 2];
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

	private static String getWindowsDirectory() {
		char[] test = new char[2 + 256 * 2];
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

			char[] lpRootPathNameChars = new char[4];
			for (int i = 0; i < 3; i++) {
				lpRootPathNameChars[i] = driveName.charAt(i);
			}
			lpRootPathNameChars[3] = '\0';
			int nVolumeNameSize = 256;
			CharBuffer lpVolumeNameBufferChar = CharBuffer.allocate(nVolumeNameSize);
			LongByReference lpVolumeSerialNumber = new LongByReference();
			LongByReference lpMaximumComponentLength = new LongByReference();
			LongByReference lpFileSystemFlags = new LongByReference();
			int nFileSystemNameSize = 256;
			CharBuffer lpFileSystemNameBufferChar = CharBuffer.allocate(nFileSystemNameSize);

			boolean result2 = Kernel32.INSTANCE.GetVolumeInformationW(
				lpRootPathNameChars,
				lpVolumeNameBufferChar,
				nVolumeNameSize,
				lpVolumeSerialNumber,
				lpMaximumComponentLength,
				lpFileSystemFlags,
				lpFileSystemNameBufferChar,
				nFileSystemNameSize);
			if (!result2) {
				return null;
			}
			return charString2String(lpVolumeNameBufferChar);
		} catch (IOException e) {
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

	/** *  Only to be instantiated by {@link PlatformUtils#createInstance()}. */
	public WindowsUtils() {
		Charset windowsConsole;
		try {
			windowsConsole = Charset.forName("cp" + getOEMCP());
		} catch (Exception e) {
			windowsConsole = Charset.defaultCharset();
		}
		consoleCharset = windowsConsole;
		setVLCRegistryInfo();
		avsPluginsFolder = getAviSynthPluginsFolder();
		aviSynth = avsPluginsFolder != null;
		kLiteFiltersDir = getKLiteFiltersFolder();
		kerio = isKerioInstalled();
		psPing = findPsPing();
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
	public boolean isAdmin() {
		synchronized (IS_ADMIN_LOCK) {
			if (isAdmin != null) {
				return isAdmin;
			}
			if (OS_VERSION.isGreaterThanOrEqualTo("5.1.0")) {
				try {
					String command = "reg query \"HKU\\S-1-5-19\"";
					Process p = Runtime.getRuntime().exec(command);
					p.waitFor();
					int exitValue = p.exitValue();

					if (0 == exitValue) {
						isAdmin = true;
						return true;
					}

					isAdmin = false;
				} catch (IOException | InterruptedException e) {
					isAdmin = false;
					LOGGER.error("An error prevented UMS from checking Windows permissions: {}", e.getMessage());
				}
			} else {
				isAdmin = true;
			}
			return isAdmin;
		}
	}

	@Override
	public List<Path> getDefaultFolders() {
		synchronized (DEFAULT_FOLDERS_LOCK) {
			if (defaultFolders == null) {
				// Lazy initialization
				List<Path> result = new ArrayList<>();
				if (OS_VERSION.isGreaterThanOrEqualTo("6.0.0")) {
					List<GUID> knownFolders = List.of(
						KnownFolders.FOLDERID_MUSIC,
						KnownFolders.FOLDERID_PICTURES,
						KnownFolders.FOLDERID_VIDEOS
					);
					for (GUID guid : knownFolders) {
						Path folder = getWindowsKnownFolder(guid);
						if (folder != null) {
							result.add(folder);
						}
					}
				} else {
					CSIDL[] csidls = {
						CSIDL.CSIDL_MYMUSIC,
						CSIDL.CSIDL_MYPICTURES,
						CSIDL.CSIDL_MYVIDEO
					};
					for (CSIDL csidl : csidls) {
						Path folder = getWindowsFolder(csidl);
						if (folder != null) {
							result.add(folder);
						}
					}
				}
				defaultFolders = Collections.unmodifiableList(result);
			}
			return defaultFolders;
		}
	}

	@Override
	public Version getFileVersionInfo(String filePath) {
		try {
			VerRsrc.VS_FIXEDFILEINFO version = VersionUtil.getFileVersionInfo(filePath);
			Version fileVersion = new Version(version.getFileVersionMajor() + "." + version.getFileVersionMinor() + "." + version.getFileVersionRevision() + "." + version.getFileVersionBuild());
			if (fileVersion.isGreaterThan(new Version("0.0.0.0"))) {
				return fileVersion;
			}
			return new Version(version.getProductVersionMajor() + "." + version.getProductVersionMinor() + "." + version.getProductVersionRevision() + "." + version.getProductVersionBuild());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getiTunesFile() throws IOException {
		Process process = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v \"My Music\"");
		String location = null;
		//TODO The encoding of the output from reg query is unclear, this must be investigated further
		try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = in.readLine()) != null) {
				final String lookFor = "REG_SZ";
				if (line.contains(lookFor)) {
					location = line.substring(line.indexOf(lookFor) + lookFor.length()).trim();
				}
			}
		}

		if (location != null) {
			// Add the iTunes folder to the end
			location += "\\iTunes\\iTunes Music Library.xml";
			return location;
		} else {
			LOGGER.info("Could not find the My Music folder");
		}
		return null;
	}

	@Override
	public Charset getDefaultCharset() {
		return consoleCharset;
	}

	@Override
	public String getDefaultFontPath() {
		String font = null;
		String winDir = getWindowsDirectory();
		if (winDir != null) {
			File winDirFile = new File(winDir);
			if (winDirFile.exists()) {
				File fontsDir = new File(winDirFile, "Fonts");
				if (fontsDir.exists()) {
					File arialDir = new File(fontsDir, "Arial.ttf");
					if (arialDir.exists()) {
						font = arialDir.getAbsolutePath();
					} else {
						arialDir = new File(fontsDir, "arial.ttf");
						if (arialDir.exists()) {
							font = arialDir.getAbsolutePath();
						}
					}
				}
			}
		}
		if (font == null) {
			font = getAbsolutePath("C:\\Windows\\Fonts", "Arial.ttf");
		}
		if (font == null) {
			font = getAbsolutePath("C:\\WINNT\\Fonts", "Arial.ttf");
		}
		if (font == null) {
			font = getAbsolutePath("D:\\Windows\\Fonts", "Arial.ttf");
		}
		if (font == null) {
			font = getAbsolutePath(".\\bin\\mplayer\\", "subfont.ttf");
		}
		return font;
	}

	@Override
	public boolean isPreventSleepSupported() {
		return true;
	}

	@Override
	public AbstractSleepWorker getSleepWorker(SleepManager owner, PreventSleepMode mode) {
		return new WindowsSleepWorker(owner, mode);
	}

	@Override
	public AbstractProcessTerminator getProcessTerminator(ProcessManager processManager) {
		return new WindowsProcessTerminator(processManager);
	}

	@Override
	public IPipeProcess getPipeProcess(String pipeName, OutputParams params, String... extras) {
		return new WindowsPipeProcess(pipeName, params, extras);
	}

	@Override
	public void appendErrorString(StringBuilder sb, int exitCode) {
		NTStatus ntStatus = null;
		if (exitCode > 10) {
			ntStatus = NTStatus.typeOf(exitCode);
		}
		if (ntStatus != null) {
			sb.append("Process exited with error ").append(ntStatus).append("\n");
		} else {
			sb.append("Process exited with code ").append(exitCode).append(":\n");
		}
	}

	@Override
	protected String getTrayIcon() {
		return "icon-16.png";
	}

	private void setVLCRegistryInfo() {
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

	@Override
	public String getShutdownCommand() {
		return "shutdown.exe -s -t 0";
	}

	@Override
	public String getJvmExecutableName() {
		return System.console() == null ? "javaw.exe" : "java.exe";
	}

	private static String getAviSynthPluginsFolder() {
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

	private static String getKLiteFiltersFolder() {
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

	private static boolean isKerioInstalled() {
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

	private static Path findPsPing() {
		// PsPing
		Path tmpPsPing = FileUtil.findExecutableInOSPath(Paths.get("psping64.exe"));
		if (tmpPsPing == null) {
			tmpPsPing = FileUtil.findExecutableInOSPath(Paths.get("psping.exe"));
		}
		return tmpPsPing;
	}

	@Nullable
	private static Path getWindowsKnownFolder(GUID guid) {
		try {
			String folderPath = Shell32Util.getKnownFolderPath(guid);
			if (StringUtils.isNotBlank(folderPath)) {
				Path folder = Paths.get(folderPath);
				try {
					FilePermissions permissions = new FilePermissions(folder);
					if (permissions.isBrowsable()) {
						return folder;
					}
					LOGGER.warn("Insufficient permissions to read default folder \"{}\"", guid);
				} catch (FileNotFoundException e) {
					LOGGER.debug("Default folder \"{}\" not found", folder);
				}
			}
		} catch (Win32Exception e) {
			LOGGER.debug("Default folder \"{}\" not found: {}", guid, e.getMessage());
		} catch (InvalidPathException e) {
			LOGGER.error("Unexpected error while resolving default Windows folder with GUID {}: {}", guid, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	@Nullable
	private static Path getWindowsFolder(@Nullable CSIDL csidl) {
		if (csidl == null) {
			return null;
		}
		try {
			String folderPath = Shell32Util.getFolderPath(csidl.getValue());
			if (StringUtils.isNotBlank(folderPath)) {
				Path folder = Paths.get(folderPath);
				FilePermissions permissions;
				try {
					permissions = new FilePermissions(folder);
					if (permissions.isBrowsable()) {
						return folder;
					}
					LOGGER.warn("Insufficient permissions to read default folder \"{}\"", csidl);
				} catch (FileNotFoundException e) {
					LOGGER.debug("Default folder \"{}\" not found", folder);
				}
			}
		} catch (Win32Exception e) {
			LOGGER.debug("Default folder \"{}\" not found: {}", csidl, e.getMessage());
		} catch (InvalidPathException e) {
			LOGGER.error("Unexpected error while resolving default Windows folder with id {}: {}", csidl, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * @return whether a service named Universal Media Server is installed
	 */
	public static boolean isUmsServiceInstalled() {
		String[] commands = new String[] {"sc", "query", "\"Universal Media Server\""};
		int[] expectedExitCodes = {0, 1060};
		String response = ProcessUtil.run(expectedExitCodes, commands);
		return response.contains("TYPE");
	}

	/**
	 * Executes the needed commands in order to install the Windows service that
	 * starts whenever the machine is started. This function is called from the
	 * General tab.
	 *
	 * @return true if UMS could be installed as a Windows service.
	 * @see net.pms.newgui.GeneralTab#build()
	 */
	public static boolean installWin32Service() {
		Path wrapper = PlatformProgramPaths.resolve("service/wrapper.exe");
		if (wrapper == null || !Files.exists(wrapper)) {
			return false;
		}
		String[] cmdArray = new String[] {wrapper.toFile().getAbsolutePath(), "-i", "wrapper.conf"};
		ProcessWrapperImpl pwinstall = new ProcessWrapperImpl(cmdArray, true, new OutputParams(PMS.getConfiguration()));
		pwinstall.runInSameThread();
		return pwinstall.isSuccess();
	}

	/**
	 * Executes the needed commands in order to remove the Windows service. This
	 * function is called from the General tab.
	 *
	 * TODO: Make it detect if the uninstallation was successful
	 *
	 * @return true
	 * @see net.pms.newgui.GeneralTab#build()
	 */
	public static boolean uninstallWin32Service() {
		Path wrapper = PlatformProgramPaths.resolve("service/wrapper.exe");
		if (wrapper == null || !Files.exists(wrapper)) {
			return false;
		}
		String[] cmdArray = new String[] {wrapper.toFile().getAbsolutePath(), "-r", "wrapper.conf"};
		OutputParams output = new OutputParams(PMS.getConfiguration());
		output.setNoExitCheck(true);
		ProcessWrapperImpl pwuninstall = new ProcessWrapperImpl(cmdArray, true, output);
		pwuninstall.runInSameThread();
		return true;
	}

	/**
	 * Windows has changed its sleep strategy in version 11 to
	 * sleep immediately after we release the sleep lock, instead
	 * of respecting the timer.
	 *
	 * @see https://learn.microsoft.com/en-us/answers/questions/999348/setthreadexecutionstate-without-es-continuous-does
	 */
	public static boolean isVersionThatSleepsImmediately() {
		return StringUtils.equals(System.getProperty("os.name"), "Windows 11");
	}
}
