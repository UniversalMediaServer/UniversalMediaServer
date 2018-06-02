package net.pms.util;

import net.pms.PMS;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;

/**
 * Utility class for Windows
 */
public class WindowsUtil {
	/**
	 * Private constructor to avoid instantiating this class
	 */
	private WindowsUtil() {}

	/**
	 * Checks if is the Universal Media Server service is installed.
	 *
	 * @return true, if a service named Universal Media Server is installed
	 */
	public static boolean isUmsServiceInstalled() {
		String[] commands = new String[]{ "sc", "query", "\"Universal Media Server\"" };
		int[] expectedExitCodes = { 0, 1060 };
		String response = ProcessUtil.run(expectedExitCodes, commands);
		return response.contains("TYPE");
	}

	/**
	 * Executes the needed commands in order to install the Windows service
	 * that starts whenever the machine is started.
	 * This function is called from the General tab.
	 *
	 * @return true if UMS could be installed as a Windows service.
	 * @see net.pms.newgui.GeneralTab#build()
	 */
	public static boolean installWin32Service() {
		String cmdArray[] = new String[]{"win32/service/wrapper.exe", "-i", "wrapper.conf"};
		ProcessWrapperImpl pwinstall = new ProcessWrapperImpl(cmdArray, true, new OutputParams(PMS.getConfiguration()));
		pwinstall.runInSameThread();
		return pwinstall.isSuccess();
	}

	/**
	 * Executes the needed commands in order to remove the Windows service.
	 * This function is called from the General tab.
	 *
	 * TODO: Make it detect if the uninstallation was successful
	 *
	 * @return true
	 * @see net.pms.newgui.GeneralTab#build()
	 */
	public static boolean uninstallWin32Service() {
		String cmdArray[] = new String[]{"win32/service/wrapper.exe", "-r", "wrapper.conf"};
		OutputParams output = new OutputParams(PMS.getConfiguration());
		output.noexitcheck = true;
		ProcessWrapperImpl pwuninstall = new ProcessWrapperImpl(cmdArray, true, output);
		pwuninstall.runInSameThread();
		return true;
	}
}
