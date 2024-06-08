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

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// based on MediaInfoDLL - All info about media files, for DLL (JNA version)
// Copyright (C) 2009-2009 Jerome Martinez, Zen@MediaArea.net
// net.sourceforge.mediainfo
public class MediaInfoHelper implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaInfoHelper.class);

	private Pointer handle;

	// Constructor/Destructor
	public MediaInfoHelper(boolean log) {
		try {
			if (log) {
				LOGGER.info("Loading MediaInfo library");
			}
			handle = MediaInfoLibrary.INSTANCE.New();
			if (log) {
				LOGGER.info("Loaded {}", optionStatic("Info_Version"));
			}
			if (!Platform.isWindows()) {
				if (log) {
					LOGGER.debug("Setting MediaInfo library characterset to UTF-8");
				}
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
		return handle != null;
	}

	public void dispose() {
		if (handle == null) {
			throw new IllegalStateException();
		}

		MediaInfoLibrary.INSTANCE.Delete(handle);
		handle = null;
	}

	@Override
	public void close() throws Exception {
		if (handle != null) {
			dispose();
		}
	}

	// File
	/**
	 * Open a file and collect information about it (technical information and tags).
	 *
	 * @param fileName full name of the file to open
	 * @return 1 if file was opened, 0 if file was not not opened
	 */
	public int openFile(String fileName) {
		return MediaInfoLibrary.INSTANCE.Open(handle, new WString(fileName));
	}

	/**
	 * Close a file opened before with Open().
	 *
	 */
	public void closeFile() {
		MediaInfoLibrary.INSTANCE.Close(handle);
	}

	// Information
	/**
	 * Get all details about a file.
	 *
	 * @return All details about a file in one string
	 */
	public String inform() {
		return MediaInfoLibrary.INSTANCE.Inform(handle).toString();
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
	public String get(StreamKind streamType, int streamNumber, String parameter) {
		return get(streamType, streamNumber, parameter, InfoKind.TEXT, InfoKind.NAME);
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
	public String get(StreamKind streamType, int streamNumber, String parameter, InfoKind infoType) {
		return get(streamType, streamNumber, parameter, infoType, InfoKind.NAME);
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
	public String get(StreamKind streamType, int streamNumber, String parameter, InfoKind infoType, InfoKind searchType) {
		return MediaInfoLibrary.INSTANCE.Get(handle,
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
	public String get(StreamKind streamType, int streamNumber, int parameterIndex) {
		return get(streamType, streamNumber, parameterIndex, InfoKind.TEXT);
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
	public String get(StreamKind streamType, int streamNumber, int parameterIndex, InfoKind infoType) {
		return MediaInfoLibrary.INSTANCE.GetI(handle,
			streamType.getValue(),
			streamNumber,
			parameterIndex,
			infoType.getValue()).toString();
	}

	public Long getLong(StreamKind streamType, int streamNumber, String parameter) {
		String result = get(streamType, streamNumber, parameter);
		if (result != null && !"".equals(result)) {
			try {
				return Long.valueOf(result);
			} catch (NumberFormatException e) {
				StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
				if (stackTraceElements.length > 1) {
					LOGGER.debug("Could not parse \"{}\" as Long for \"{}.{}\"", result, stackTraceElements[2].getClassName(), stackTraceElements[2].getMethodName());
				} else {
					LOGGER.debug("Could not parse \"{}\" as Long", result);
				}
			}
		}
		return null;
	}

	public Double getDouble(StreamKind streamType, int streamNumber, String parameter) {
		String result = get(streamType, streamNumber, parameter);
		if (result != null && !"".equals(result)) {
			try {
				return Double.valueOf(result);
			} catch (NumberFormatException e) {
				StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
				if (stackTraceElements.length > 1) {
					LOGGER.debug("Could not parse \"{}\" as Double for \"{}.{}\"", result, stackTraceElements[2].getClassName(), stackTraceElements[2].getMethodName());
				} else {
					LOGGER.debug("Could not parse \"{}\" as Double", result);
				}
			}
		}
		return null;
	}

	/**
	 * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
	 * information in this Stream.
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @return number of Streams of the given Stream kind
	 */
	public int countGet(StreamKind streamType) {
		return MediaInfoLibrary.INSTANCE.Count_Get(handle, streamType.getValue(), -1);
	}

	/**
	 * Count of Streams of a Stream kind (StreamNumber not filled), or count of piece of
	 * information in this Stream.
	 *
	 * @param streamType Type of Stream (general, video, audio...)
	 * @param streamNumber Stream number in this kind of Stream (first, second...)
	 * @return number of Streams of the given Stream kind
	 */
	public int countGet(StreamKind streamType, int streamNumber) {
		return MediaInfoLibrary.INSTANCE.Count_Get(handle, streamType.getValue(), streamNumber);
	}

	// Options
	/**
	 * Configure or get information about MediaInfoHelper.
	 *
	 * @param option The name of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public String option(String option) {
		return MediaInfoLibrary.INSTANCE.Option(handle, new WString(option), new WString("")).toString();
	}

	/**
	 * Configure or get information about MediaInfoHelper.
	 *
	 * @param option The name of option
	 * @param value The value of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public String option(String option, String value) {
		return MediaInfoLibrary.INSTANCE.Option(handle, new WString(option), new WString(value)).toString();
	}

	/**
	 * Sets the MediaInfo library to expect UTF-8 input. This is necessary on
	 * non-Windows platforms for Unicode support.
	 */
	public final void setUTF8() {
		MediaInfoLibrary.INSTANCE.Option(handle, new WString("setlocale_LC_CTYPE"), new WString("UTF-8"));
	}

	/**
	 * Configure or get information about MediaInfoHelper (Static version).
	 *
	 * @param option The name of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public static String optionStatic(String option) {
		return MediaInfoLibrary.INSTANCE.Option(MediaInfoLibrary.INSTANCE.New(),
			new WString(option),
			new WString("")).toString();
	}

	/**
	 * Configure or get information about MediaInfoHelper (Static version).
	 *
	 * @param option The name of option
	 * @param value The value of option
	 * @return Depends on the option: by default "" (nothing) means No, other means Yes
	 */
	public static String optionStatic(String option, String value) {
		return MediaInfoLibrary.INSTANCE.Option(MediaInfoLibrary.INSTANCE.New(),
			new WString(option),
			new WString(value)).toString();
	}
}
