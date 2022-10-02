/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.platform.windows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.LongByReference;
import java.nio.CharBuffer;

@SuppressWarnings({
	"checkstyle:ConstantName",
	"checkstyle:MethodName",
	"checkstyle:ParameterName"
})
public interface Kernel32 extends Library {
	Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

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

