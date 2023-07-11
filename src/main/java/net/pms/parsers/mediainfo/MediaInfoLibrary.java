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
package net.pms.parsers.mediainfo;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.pms.platform.PlatformProgramPaths;
import net.pms.platform.windows.WindowsProgramPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({
	"checkstyle:MethodName"
})
interface MediaInfoLibrary extends Library {
	static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoLibrary.class);
	MediaInfoLibrary INSTANCE = createInstance();

	// Constructor/Destructor
	Pointer New();

	void Delete(Pointer handle);

	// File
	int Open(Pointer handle, WString file);

	void Close(Pointer handle);

	// Info
	WString Inform(Pointer handle);

	WString Get(Pointer handle, int streamType, int streamNumber, WString parameter, int infoType, int searchType);

	WString GetI(Pointer handle, int streamType, int streamNumber, int parameterIndex, int infoType);

	int Count_Get(Pointer handle, int streamType, int streamNumber);

	// Options
	WString Option(Pointer handle, WString option, WString value);

	public static MediaInfoLibrary createInstance() {
		String libraryName = "mediainfo";

		//windows
		if (Platform.isWindows() && System.getProperty("jna.library.path") == null && PlatformProgramPaths.get() instanceof WindowsProgramPaths && ((WindowsProgramPaths) PlatformProgramPaths.get()).getMediaInfo() != null) {
			String jnaPath = ((WindowsProgramPaths) PlatformProgramPaths.get()).getMediaInfo().toString();
			LOGGER.info("JNA Library folder set to: \"{}\"", jnaPath);
			System.setProperty("jna.library.path", jnaPath);
		}

		// libmediainfo for Linux depends on libzen
		if (!Platform.isWindows() && !Platform.isMac()) {
			try {
				// We need to load dependencies first, because we know where our native libs are (e.g. Java Web Start Cache).
				// If we do not, the system will look for dependencies, but only in the library path.
				NativeLibrary.getInstance("zen");
			} catch (LinkageError e) {
				LOGGER.warn("Error loading libzen: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		Map<String, Object> options = new HashMap<>();
		// add MediaInfo_ to methods : eg. MediaInfo_New(), MediaInfo_Open() ...
		options.put(Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (NativeLibrary lib, Method method) -> "MediaInfo_" + method.getName());
		if (!Platform.isWindows()) {
			options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
		}
		return Native.load(libraryName,
			MediaInfoLibrary.class,
			options
		);
	}
}

