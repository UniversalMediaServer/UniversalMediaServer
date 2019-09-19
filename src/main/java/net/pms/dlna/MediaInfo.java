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

	/**
	 * An enum representing the C++ enum {@code stream_t} defined in
	 * {@code MediaInfoDLL.h}, "Kinds of Stream".
	 *
	 * @author Nadahar
	 */
	public enum StreamType {

		/** StreamKind = General */
		General(0),

		/** StreamKind = Video */
		Video(1),

		/** StreamKind = Audio */
		Audio(2),

		/** StreamKind = Text */
		Text(3),

		/** StreamKind = Other */
		Other(4),

		/** StreamKind = Image */
		Image(5),

		/** StreamKind = Menu */
		Menu(6);

		private final int value;

		private StreamType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/**
	 * An enum representing the C++ enum {@code info_t} defined in
	 * {@code MediaInfoDLL.h}, "Kind of information".
	 *
	 * @author Nadahar
	 */
	public enum InfoType {

		/** InfoKind = Unique name of parameter */
		Name(0),

		/** InfoKind = Value of parameter */
		Text(1),

		/** InfoKind = Unique name of measure unit of parameter */
		Measure(2),

		/** InfoKind = See {@link InfoOptionsType} */
		Options(3),

		/** InfoKind = Translated name of parameter */
		Name_Text(4),

		/** InfoKind = Translated name of measure unit */
		Measure_Text(5),

		/** InfoKind = More information about the parameter */
		Info(6),

		/** InfoKind = Information : how data is found */
		HowTo(7);

		private final int value;

		private InfoType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/**
	 * An enum representing the C++ enum {@code infooptions_t} defined in
	 * {@code MediaInfoDLL.h}, "Option if InfoKind = Info_Options".
	 * <p>
	 * Get(...)[infooptions_t] return a string like "YNYN...". Use this
	 * {@code enum} to know at what correspond the {@code Y} (Yes) or {@code N}
	 * (No).
	 * <p>
	 * If {@code Get(...)[0]==Y, then : }
	 *
	 * @author Nadahar
	 */
	public enum InfoOptionsType {

		/** Show this parameter in {@link MediaInfo#Inform()} */
		ShowInInform(0),

		/** Reserved for future use */
		Reserved(1),

		/**
		 * Internal use only (info : Must be showed in
		 * {@code Info_Capacities()})
		 */
		ShowInSupported(2),

		/**
		 * Value return by a standard {@link MediaInfo#get}() can be : {@code T}
		 * (Text), {@code I} (Integer, warning up to 64 bits), {@code F}
		 * (Float), {@code D} (Date), {@code B} (Binary datas coded Base64)
		 * (Numbers are in Base 10)
		 */
		TypeOfValue(3);

		private final int value;

		private InfoOptionsType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}


	/**
	 * An enum representing the C++ enum {@code fileoptions_t} defined in
	 * {@code MediaInfoDLL.h}, "File opening options".
	 *
	 * @author Nadahar
	 */
	public enum FileOptionsType {

		/** No options */
		Nothing(0x00),

		/** Do not browse folders recursively */
		NoRecursive(0x01),

		/** Close all files before open */
		CloseAll(0x02);

		private final int value;

		private FileOptionsType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
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
			streamType.getValue(),
			streamNumber,
			new WString(parameter),
			infoType.getValue(),
			searchType.getValue()).toString();
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
			streamType.getValue(),
			streamNumber,
			parameterIndex,
			infoType.getValue()).toString();
	}

	/**
	 * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
	 * information in this Stream.
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @return number of Streams of the given Stream kind
	 */
	public int Count_Get(StreamType streamType) {
		return MediaInfoDLL_Internal.INSTANCE.Count_Get(Handle, streamType.getValue(), -1);
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
		return MediaInfoDLL_Internal.INSTANCE.Count_Get(Handle, streamType.getValue(), streamNumber);
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
