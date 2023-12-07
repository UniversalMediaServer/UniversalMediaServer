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

/**
 * StreamSubtitle (was Text but to do is moving to Subtitle)
 */
public class StreamSubtitle {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamSubtitle() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "MenuID/String");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format");
	}

	/**
	 * Format used and any additional features or settings.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings, if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Profile");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Compression");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Settings");
	}

	/**
	 * Wrapping mode set for format (e.g. Frame, Clip).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsWrapping(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_Settings_Wrapping");
	}

	/**
	 * Features from the format that are required to fully support the file content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Internet Media Type (aka MIME Type, Content-Type).
	 * Shown in Info_Capacities()
	 */
	public static final String getInternetMediaType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "InternetMediaType");
	}

	/**
	 * How this file is muxed in the container (e.g. Muxed in Video #1).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMuxingMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "MuxingMode");
	}

	/**
	 * More information about MuxingMode.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMuxingModeMoreInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "MuxingMode_MoreInfo");
	}

	/**
	 * Codec identifier as indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID");
	}

	/**
	 * Codec identifier, as indicated by the container.
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec identifier.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description, as defined by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "CodecID_Description");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Codec");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Codec/Url");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecCC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Codec/CC");
	}

	/**
	 * Play time of the stream, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration");
	}

	/**
	 * Play time of the stream in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String");
	}

	/**
	 * Play time of the stream in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String1");
	}

	/**
	 * Play time of the stream in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String2");
	}

	/**
	 * Play time of the stream in format HH:MM:SS.mmm (e.g. 01:40:00.000).
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String3");
	}

	/**
	 * Play time of the stream in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String4");
	}

	/**
	 * Play time  of the streamin format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration/String5");
	}

	/**
	 * Play time from first display to last display, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationStart2End(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_Start2End");
	}

	/**
	 * Play time from first display to last display in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationStart2EndString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String");
	}

	/**
	 * Play time from first display to last display in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationStart2EndString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String1");
	}

	/**
	 * Play time from first display to last display in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationStart2EndString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String2");
	}

	/**
	 * Play time from first display to last display in format HH:MM:SS.mmm.
	 */
	public static final String getDurationStart2EndString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String3");
	}

	/**
	 * Play time from first display to last display in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationStart2EndString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String4");
	}

	/**
	 * Play time from first display to last display in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationStart2EndString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start2End/String5");
	}

	/**
	 * Timestamp of first command, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationStartCommand(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_Start_Command");
	}

	/**
	 * Timestamp of first command in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationStartCommandString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String");
	}

	/**
	 * Timestamp of first command in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationStartCommandString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String1");
	}

	/**
	 * Timestamp of first command in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationStartCommandString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String2");
	}

	/**
	 * Timestamp of first command in format HH:MM:SS.mmm.
	 */
	public static final String getDurationStartCommandString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String3");
	}

	/**
	 * Timestamp of first command in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationStartCommandString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String4");
	}

	/**
	 * Timestamp of first command in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationStartCommandString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start_Command/String5");
	}

	/**
	 * Timestamp of first display, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationStart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_Start");
	}

	/**
	 * Timestamp of first display in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationStartString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String");
	}

	/**
	 * Timestamp of first display in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationStartString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String1");
	}

	/**
	 * Timestamp of first display in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationStartString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String2");
	}

	/**
	 * Timestamp of first display in format HH:MM:SS.mmm.
	 */
	public static final String getDurationStartString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String3");
	}

	/**
	 * Timestamp of first display in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationStartString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String4");
	}

	/**
	 * Timestamp of first display in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationStartString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Start/String5");
	}

	/**
	 * Play time of the stream, in s (ms for text output).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_End");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationEndString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationEndString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationEndString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm.
	 */
	public static final String getDurationEndString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationEndString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationEndString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End/String5");
	}

	/**
	 * Play time of the stream, in s (ms for text output).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationEndCommand(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_End_Command");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationEndCommandString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationEndCommandString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationEndCommandString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm.
	 */
	public static final String getDurationEndCommandString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationEndCommandString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationEndCommandString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_End_Command/String5");
	}

	/**
	 * Duration of the first frame (if different than other frames), in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame (if different than other frames), in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Duration_LastFrame");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_LastFrame/String5");
	}

	/**
	 * Temporal coordinate system used for timestamps.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDurationBase(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Duration_Base");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Source_Duration");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String1");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String2");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String3");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String4");
	}

	/**
	 * Duration of content stored in the file (if different than duration), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration/String5");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames),in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames),in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames),in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_Duration_LastFrame/String5");
	}

	/**
	 * Bit rate mode of this stream (CBR, VBR).
	 * Shown in Info_Capacities()
	 */
	public static final String getBitRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Mode");
	}

	/**
	 * Bit rate mode of this stream, as word (Variable, Constant).
	 * Shown in inform()
	 */
	public static final String getBitRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Mode/String");
	}

	/**
	 * Bit rate of this stream, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "BitRate");
	}

	/**
	 * Bit rate of this stream, with measurement (e.g. 128 kb/s).
	 * Shown in inform()
	 */
	public static final String getBitRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate/String");
	}

	/**
	 * Minimum bit rate of this stream, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "BitRate_Minimum");
	}

	/**
	 * Minimum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Minimum/String");
	}

	/**
	 * Nominal bit rate of this stream, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "BitRate_Nominal");
	}

	/**
	 * Nominal bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Nominal/String");
	}

	/**
	 * Maximum bit rate of this stream, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "BitRate_Maximum");
	}

	/**
	 * Maximum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Maximum/String");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "BitRate_Encoded");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bps.
	 */
	public static final String getBitRateEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitRate_Encoded/String");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in characters.
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Width");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in characters, presented in SI unit digit spacing style, with measurement.
	 * Shown in inform()
	 */
	public static final String getWidthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Width/String");
	}

	/**
	 * Height of frame (including aperture size if present) in characters.
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeight(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Height");
	}

	/**
	 * Height of frame (including aperture size if present) in characters, present in SI unit digit spacing style, with measurement.
	 * Shown in inform()
	 */
	public static final String getHeightString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Height/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "DisplayAspectRatio");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "DisplayAspectRatio/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatioOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "DisplayAspectRatio_Original");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "DisplayAspectRatio_Original/String");
	}

	/**
	 * Frame rate mode, as acronym (e.g. CFR, VFR).
	 * Shown in Info_Capacities()
	 */
	public static final String getFrameRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Mode");
	}

	/**
	 * Frame rate mode, as word (e.g. Constant, Variable).
	 */
	public static final String getFrameRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Mode/String");
	}

	/**
	 * Frame rate mode, as acronym (e.g. CFR, VFR).
	 * Shown in Info_Capacities()
	 */
	public static final String getFrameRateModeOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Mode_Original");
	}

	/**
	 * Frame rate mode, as word (e.g. Constant, Variable).
	 * Shown in inform()
	 */
	public static final String getFrameRateModeOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Mode_Original/String");
	}

	/**
	 * Frames per second, as float (e.g. 29.970).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second, with measurement (e.g. 29.970 (29970/1000) FPS).
	 * Shown in inform()
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate/String");
	}

	/**
	 * Numerator for determined frames per second (e.g. 29970).
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "FrameRate_Num");
	}

	/**
	 * Denominator for determined frames per second (e.g. 1000).
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "FrameRate_Den");
	}

	/**
	 * Minimum frames per second (e.g. 25.000).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Minimum");
	}

	/**
	 * Minimum frames per second, with measurement (e.g. 25.000 FPS).
	 */
	public static final String getFrameRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Minimum/String");
	}

	/**
	 * Frames per second rounded to closest standard (e.g. 29.97).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Nominal");
	}

	/**
	 * Frames per second rounded to closest standard, with measurement (e.g. 29.97 fps).
	 * Shown in inform()
	 */
	public static final String getFrameRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Nominal/String");
	}

	/**
	 * Maximum frames per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Maximum");
	}

	/**
	 * Maximum frames per second, with measurement.
	 * Shown in inform()
	 */
	public static final String getFrameRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Maximum/String");
	}

	/**
	 * Frames per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Original");
	}

	/**
	 * Frames per second, with measurement.
	 * Shown in inform()
	 */
	public static final String getFrameRateOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FrameRate_Original/String");
	}

	/**
	 * Frames per second, numerator.
	 */
	public static final Double getFrameRateOriginalNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Original_Num");
	}

	/**
	 * Frames per second, denominator.
	 */
	public static final Double getFrameRateOriginalDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "FrameRate_Original_Den");
	}

	/**
	 * Numer of frames.
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "FrameCount");
	}

	/**
	 * Number of displayed elements.
	 * Shown in inform()
	 */
	public static final Long getElementCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "ElementCount");
	}

	/**
	 * Number of frames according to media header (media/stts atom) data.
	 */
	public static final Long getSourceFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Source_FrameCount");
	}

	/**
	 * Color profile of the image (e.g. YUV).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getColorSpace(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ColorSpace");
	}

	/**
	 * Ratio of chroma to luma in encoded image (e.g. 4:2:2).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChromaSubsampling(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ChromaSubsampling");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getResolution(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Resolution");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getResolutionString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Resolution/String");
	}

	/**
	 * Color information stored in the video frames, as integer (e.g. 10).
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "BitDepth");
	}

	/**
	 * Color information stored in the video frames, as string (e.g. 10 bits).
	 * Shown in inform()
	 */
	public static final String getBitDepthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "BitDepth/String");
	}

	/**
	 * Compression mode (Lossy, Lossless).
	 * Shown in Info_Capacities()
	 */
	public static final String getCompressionMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Compression_Mode");
	}

	/**
	 * Compression mode (Lossy, Lossless).
	 * Shown in inform()
	 */
	public static final String getCompressionModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Compression_Mode/String");
	}

	/**
	 * Stream size divided by uncompressed stream size.
	 * Shown in inform()
	 */
	public static final Double getCompressionRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Compression_Ratio");
	}

	/**
	 * Delay fixed in the stream (relative), in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Delay");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String1");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String2");
	}

	/**
	 * Delay fixed in the stream (relative) in format HH:MM:SS.mmm, with measurement.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (e.g. in case of timecode).
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame information.
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Source");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySourceString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Source/String");
	}

	/**
	 * Delay, in ms.
	 */
	public static final Long getDelayOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Delay_Original");
	}

	/**
	 * Delay, with measurement.
	 */
	public static final String getDelayOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String");
	}

	/**
	 * Delay, with measurement.
	 */
	public static final String getDelayOriginalString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String1");
	}

	/**
	 * Delay, with measurement.
	 */
	public static final String getDelayOriginalString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String2");
	}

	/**
	 * Delay, in format HH:MM:SS.mmm.
	 */
	public static final String getDelayOriginalString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String3");
	}

	/**
	 * Delay, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayOriginalString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String4");
	}

	/**
	 * Delay, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayOriginalString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original/String5");
	}

	/**
	 * Delay settings (e.g. in case of timecode).
	 */
	public static final String getDelayOriginalSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original_Settings");
	}

	/**
	 * Delay drop frame information.
	 */
	public static final String getDelayOriginalDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original_DropFrame");
	}

	/**
	 * Delay source (e.g. Container, Stream, empty).
	 */
	public static final String getDelayOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Delay_Original_Source");
	}

	/**
	 * Delay fixed in the stream relative to video, in ms (e.g. -80).
	 */
	public static final Double getVideoDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Video_Delay");
	}

	/**
	 * Delay fixed in the stream relative to video, in ms, with measurement (e.g. -80 ms).
	 * Shown in inform()
	 */
	public static final String getVideoDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String");
	}

	/**
	 * Delay fixed in the stream relative to video, in ms, with measurement (e.g. -80 ms).
	 */
	public static final String getVideoDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String1");
	}

	/**
	 * Delay fixed in the stream relative to video, in ms, with measurement (e.g. -80 ms).
	 */
	public static final String getVideoDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String2");
	}

	/**
	 * Delay fixed in the stream relative to video, in format HH:MM:SS.mmm (e.g. -00:00:00.080).
	 */
	public static final String getVideoDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with the last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getVideoDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String4");
	}

	/**
	 * Delay fixed in the stream relative to video, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getVideoDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video_Delay/String5");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getVideo0Delay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Video0_Delay");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String1");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String2");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String3");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String4");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Video0_Delay/String5");
	}

	/**
	 * Time code for first frame in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_FirstFrame");
	}

	/**
	 * Time code for last frame (excluding the duration of the last frame) in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_LastFrame");
	}

	/**
	 * Time code drop frame.
	 */
	public static final String getTimeCodeDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_DropFrame");
	}

	/**
	 * Additional time code settings.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_Settings");
	}

	/**
	 * Time code source (Container, Stream, SystemScheme1, SDTI, ANC, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_Source");
	}

	/**
	 * Maximum frame number in time codes.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeMaxFrameNumber(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_MaxFrameNumber");
	}

	/**
	 * Theoritical maximum frame number in time codes.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeMaxFrameNumberTheory(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "TimeCode_MaxFrameNumber_Theory");
	}

	/**
	 * Size of this stream, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamSize");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String1");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String2");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String3");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String4");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * Size of this stream after demuxing, in bytes.
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Size of content stored in the file, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Source_StreamSize");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getSourceStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String1");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String2");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String3");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String4");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getSourceStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Proportion");
	}

	/**
	 * Size of this stream when encoded, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "StreamSize_Encoded");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String1");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String2");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String3");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String4");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded/String5");
	}

	/**
	 * Encoded Stream size divided by file size.
	 */
	public static final String getStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "StreamSize_Encoded_Proportion");
	}

	/**
	 * Size of content stored in the file when encoded, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String1");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String2");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String3");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String4");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded/String5");
	}

	/**
	 * Source Encoded Stream size divided by file size.
	 */
	public static final String getSourceStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Source_StreamSize_Encoded_Proportion");
	}

	/**
	 * Title of file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Title");
	}

	/**
	 * Name of the software package used to create the file (e.g. Microsoft WaveEdiTY).
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplication(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application");
	}

	/**
	 * Name of the software package used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedApplicationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application/String");
	}

	/**
	 * Name of the company of the encoding application.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application_CompanyName");
	}

	/**
	 * Name of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application_Name");
	}

	/**
	 * Version of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application_Version");
	}

	/**
	 * URL associated with the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Application_Url");
	}

	/**
	 * Software used to create the file.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library");
	}

	/**
	 * Software used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedLibraryString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library/String");
	}

	/**
	 * Name of the encoding software company.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library_CompanyName");
	}

	/**
	 * Name of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library_Name");
	}

	/**
	 * Version of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library_Version");
	}

	/**
	 * Release date of the encoding software, in UTC.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library_Date");
	}

	/**
	 * Parameters used by the encoding software.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrarySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Library_Settings");
	}

	/**
	 * Operating System of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedOperatingSystem(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_OperatingSystem");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available (e.g. en, en-US, en-CN).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language");
	}

	/**
	 * Language, as full name (e.g. English).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language/String");
	}

	/**
	 * Language, as full name (e.g. English).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language/String1");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists (e.g. en).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language/String2");
	}

	/**
	 * Language, formatted as 3-letter ISO 639-2, if exists (e.g. eng).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language/String3");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists, with optional ISO 3166-1 country separated by a dash if available (e.g. en-US).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language/String4");
	}

	/**
	 * More information about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Language_More");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ServiceKind");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "ServiceKind/String");
	}

	/**
	 * Set if this stream should not be used (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Disabled");
	}

	/**
	 * Set if this stream should not be used (Yes, No).
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Disabled/String");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Default");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference (Yes, No).
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Default/String");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Forced");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie (Yes, No).
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "AlternateGroup/String");
	}

	/**
	 * Plot outline or a summary of the story.
	 * Shown in Info_Capacities()
	 */
	public static final String getSummary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Summary");
	}

	/**
	 * Time/date/year that the encoding of this content was completed.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encoded_Date");
	}

	/**
	 * Time/date/year that the tags were added to this content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Tagged_Date");
	}

	/**
	 * Whether this stream is encrypted and, if available, how it is encrypted.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryption(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Encryption");
	}

	public static final String getEventsTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_Total");
	}

	/**
	 * Minimum duration per event, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getEventsMinDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.TEXT, streamNumber, "Events_MinDuration");
	}

	/**
	 * Minimum duration per event in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getEventsMinDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String");
	}

	/**
	 * Minimum duration per event in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getEventsMinDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String1");
	}

	/**
	 * Minimum duration per event in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getEventsMinDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String2");
	}

	/**
	 * Minimum duration per event in format HH:MM:SS.mmm.
	 */
	public static final String getEventsMinDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String3");
	}

	/**
	 * Minimum duration per event in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getEventsMinDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String4");
	}

	/**
	 * Minimum duration per event in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getEventsMinDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_MinDuration/String5");
	}

	public static final String getEventsPopOn(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_PopOn");
	}

	public static final String getEventsRollUp(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_RollUp");
	}

	public static final String getEventsPaintOn(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Events_PaintOn");
	}

	public static final String getLinesCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Lines_Count");
	}

	public static final String getLinesMaxCountPerEvent(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "Lines_MaxCountPerEvent");
	}

	public static final String getFirstDisplayDelayFrames(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FirstDisplay_Delay_Frames");
	}

	public static final String getFirstDisplayType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.TEXT, streamNumber, "FirstDisplay_Type");
	}

}
