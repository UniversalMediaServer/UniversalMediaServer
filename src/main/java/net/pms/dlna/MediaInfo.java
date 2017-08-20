/**
 * MediaInfoDLL - All info about media files, for DLL (JNA version)
 *
 * Copyright (C) 2009-2009 Jerome Martinez, Zen@MediaArea.net
 *
 * This library is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 *
 **/

// Note: the original stuff was well packaged with Java style,
// but I (the main developer) prefer to keep an easiest for me
// way to have all sources and example in the same place
// Removed stuff:
// "package net.sourceforge.mediainfo;"
// directory was /net/sourceforge/mediainfo

package net.pms.dlna;

import com.sun.jna.*;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfo.class);
	static String libraryName;

	static {
		if (Platform.isWindows() && Platform.is64Bit()) {
			libraryName = "mediainfo64";
		} else {
			libraryName = "mediainfo";
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
	}

	// Internal stuff
	interface MediaInfoDLL_Internal extends Library {

		/**
		 * A {@link Map} of library options for use with {@link Native#loadLibrary}.
		 */
		Map<String, Object> options = Collections.unmodifiableMap(new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			{
				put(OPTION_FUNCTION_MAPPER, new FunctionMapper() {

					@Override
					public String getFunctionName(NativeLibrary lib, Method method) {
						// e.g. MediaInfo_New(), MediaInfo_Open() ...
						return "MediaInfo_" + method.getName();
					}
				});
				if (!Platform.isWindows()) {
					put(OPTION_STRING_ENCODING, "UTF-8");
				}
			}
		});


		@SuppressWarnings("cast")
		MediaInfoDLL_Internal INSTANCE = (MediaInfoDLL_Internal) Native.loadLibrary(
			libraryName,
			MediaInfoDLL_Internal.class,
			options
		);

		// Constructor/Destructor
		Pointer New();

		void Delete(Pointer Handle);

		// File
		int Open(Pointer Handle, WString file);

		void Close(Pointer Handle);

		// Info
		WString Inform(Pointer Handle);

		WString Get(Pointer Handle, int streamType, int streamNumber, WString parameter, int infoType, int searchType);

		WString GetI(Pointer Handle, int streamType, int streamNumber, int parameterIndex, int infoType);

		int Count_Get(Pointer Handle, int streamType, int streamNumber);

		// Options
		WString Option(Pointer Handle, WString option, WString value);
	}
	private Pointer Handle;

	@Deprecated
	public enum StreamKind {
		General,
		Video,
		Audio,
		Text,
		Chapters,
		Image,
		Menu;
	}

	public enum StreamType {
		General,
		Video,
		Audio,
		Text,
		Chapters,
		Image,
		Menu,
		Other;
	}

	// Enums
	@Deprecated
	public enum InfoKind {
		/**
		 * Unique name of parameter.
		 */
		Name,
		/**
		 * Value of parameter.
		 */
		Text,
		/**
		 * Unique name of measure unit of parameter.
		 */
		Measure,
		Options,
		/**
		 * Translated name of parameter.
		 */
		Name_Text,
		/**
		 * Translated name of measure unit.
		 */
		Measure_Text,
		/**
		 * More information about the parameter.
		 */
		Info,
		/**
		 * How this parameter is supported, could be N (No), B (Beta), R (Read only), W
		 * (Read/Write).
		 */
		HowTo,
		/**
		 * Domain of this piece of information.
		 */
		Domain;
	}

	public enum InfoType {
		/**
		 * Unique name of parameter.
		 */
		Name,
		/**
		 * Value of parameter.
		 */
		Text,
		/**
		 * Unique name of measure unit of parameter.
		 */
		Measure,
		Options,
		/**
		 * Translated name of parameter.
		 */
		Name_Text,
		/**
		 * Translated name of measure unit.
		 */
		Measure_Text,
		/**
		 * More information about the parameter.
		 */
		Info,
		/**
		 * How this parameter is supported, could be N (No), B (Beta), R (Read only), W
		 * (Read/Write).
		 */
		HowTo,
		/**
		 * Domain of this piece of information.
		 */
		Domain;
	}

	// Constructor/Destructor
	public MediaInfo() {
		try {
			LOGGER.info("Loading MediaInfo library");
			Handle = MediaInfoDLL_Internal.INSTANCE.New();
			LOGGER.info("Loaded {}", Option_Static("Info_Version"));

			LOGGER.debug("Setting MediaInfo library characterset to UTF-8");
			if (!Platform.isWindows()) {
				setUTF8();
			}
		} catch (Throwable e) {
			LOGGER.error("Error loading MediaInfo library: {}", e.getMessage());
			LOGGER.trace("", e);
			if (!Platform.isWindows() && !Platform.isMac()) {
				LOGGER.info("Make sure you have libmediainfo and libzen installed");
			}
			LOGGER.info("The server will now use the less accurate FFmpeg parsing method");
		}
	}

	public boolean isValid() {
		return Handle != null;
	}

	public void dispose() {
		if (Handle == null) {
			throw new IllegalStateException();
		}

		MediaInfoDLL_Internal.INSTANCE.Delete(Handle);
		Handle = null;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (Handle != null) {
				dispose();
			}
		} finally {
			super.finalize();
		}
	}

	// File
	/**
	 * Open a file and collect information about it (technical information and tags).
	 *
	 * @param File_Name full name of the file to open
	 * @return 1 if file was opened, 0 if file was not not opened
	 */
	public int Open(String File_Name) {
		return MediaInfoDLL_Internal.INSTANCE.Open(Handle, new WString(File_Name));
	}

	/**
	 * Close a file opened before with Open().
	 *
	 */
	public void Close() {
		MediaInfoDLL_Internal.INSTANCE.Close(Handle);
	}

	// Information
	/**
	 * Get all details about a file.
	 *
	 * @return All details about a file in one string
	 */
	public String Inform() {
		return MediaInfoDLL_Internal.INSTANCE.Inform(Handle).toString();
	}

	/**
	 * Get a piece of information about a file (parameter is a string).
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in Type of Stream (first, second...)
	 * @param parameter Parameter you are looking for in the Stream (Codec, width, bitrate...),
	 *            in string format ("Codec", "Width"...)
	 * @return a string about information you search, an empty string if there is a problem
	 */
	public String Get(StreamType streamType, int streamNumber, String parameter) {
		return Get(streamType, streamNumber, parameter, InfoType.Text, InfoType.Name);
	}

	/**
	 * Get a piece of information about a file (parameter is a string).
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in Type of Stream (first, second...)
	 * @param parameter Parameter you are looking for in the Stream (Codec, width, bitrate...),
	 *            in string format ("Codec", "Width"...)
	 * @param infoType Type of information you want about the parameter (the text, the measure,
	 *            the help...)
	 * @return a string about information you search, an empty string if there is a problem
	 */
	public String Get(StreamType streamType, int streamNumber, String parameter, InfoType infoType) {
		return Get(streamType, streamNumber, parameter, infoType, InfoType.Name);
	}

	/**
	 * Get a piece of information about a file (parameter is a string).
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in Type of Stream (first, second...)
	 * @param parameter Parameter you are looking for in the Stream (Codec, width, bitrate...),
	 *            in string format ("Codec", "Width"...)
	 * @param infoType Type of information you want about the parameter (the text, the measure,
	 *            the help...)
	 * @param searchType Where to look for the parameter
	 * @return a string about information you search, an empty string if there is a problem
	 */
	public String Get(StreamType streamType, int streamNumber, String parameter, InfoType infoType, InfoType searchType) {
		return MediaInfoDLL_Internal.INSTANCE.Get(
			Handle,
			streamType.ordinal(),
			streamNumber,
			new WString(parameter),
			infoType.ordinal(),
			searchType.ordinal()).toString();
	}

	/**
	 * Get a piece of information about a file (parameter is an integer).
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in Type of Stream (first, second...)
	 * @param parameterIndex Parameter you are looking for in the Stream (Codec, width, bitrate...),
	 *            in integer format (first parameter, second parameter...)
	 * @return a string about information you search, an empty string if there is a problem
	 */
	public String get(StreamType streamType, int streamNumber, int parameterIndex) {
		return Get(streamType, streamNumber, parameterIndex, InfoType.Text);
	}

	/**
	 * Get a piece of information about a file (parameter is an integer).
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in Type of Stream (first, second...)
	 * @param parameterIndex Parameter you are looking for in the Stream (Codec, width, bitrate...),
	 *            in integer format (first parameter, second parameter...)
	 * @param infoType Type of information you want about the parameter (the text, the measure,
	 *            the help...)
	 * @return a string about information you search, an empty string if there is a problem
	 */
	public String Get(StreamType streamType, int streamNumber, int parameterIndex, InfoType infoType) {
		return MediaInfoDLL_Internal.INSTANCE.GetI(
			Handle,
			streamType.ordinal(),
			streamNumber,
			parameterIndex,
			infoType.ordinal()).toString();
	}

	/**
	 * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
	 * information in this Stream.
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @return number of Streams of the given Stream kind
	 */
	public int Count_Get(StreamType streamType) {
		return MediaInfoDLL_Internal.INSTANCE.Count_Get(Handle, streamType.ordinal(), -1);
	}

	/**
	 * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
	 * information in this Stream.
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in this kind of Stream (first, second...)
	 * @return number of Streams of the given Stream kind
	 */
	public int Count_Get(StreamType streamType, int streamNumber) {
		return MediaInfoDLL_Internal.INSTANCE.Count_Get(Handle, streamType.ordinal(), streamNumber);
	}

	// Options
	/**
	 * Configure or get information about MediaInfo.
	 *
	 * @param Option The name of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public String Option(String Option) {
		return MediaInfoDLL_Internal.INSTANCE.Option(Handle, new WString(Option), new WString("")).toString();
	}

	/**
	 * Configure or get information about MediaInfo.
	 *
	 * @param Option The name of option
	 * @param Value The value of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public String Option(String Option, String Value) {
		return MediaInfoDLL_Internal.INSTANCE.Option(Handle, new WString(Option), new WString(Value)).toString();
	}

	/**
	 * Sets the MediaInfo library to expect UTF-8 input. This is necessary on
	 * non-Windows platforms for Unicode support.
	 */
	public void setUTF8() {
		MediaInfoDLL_Internal.INSTANCE.Option(Handle, new WString("setlocale_LC_CTYPE"), new WString("UTF-8"));
	}

	/**
	 * Configure or get information about MediaInfo (Static version).
	 *
	 * @param Option The name of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public static String Option_Static(String Option) {
		return MediaInfoDLL_Internal.INSTANCE.Option(
			MediaInfoDLL_Internal.INSTANCE.New(),
			new WString(Option),
			new WString("")).toString();
	}

	/**
	 * Configure or get information about MediaInfo (Static version).
	 *
	 * @param Option The name of option
	 * @param Value The value of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public static String Option_Static(String Option, String Value) {
		return MediaInfoDLL_Internal.INSTANCE.Option(
			MediaInfoDLL_Internal.INSTANCE.New(),
			new WString(Option),
			new WString(Value)).toString();
	}
}
