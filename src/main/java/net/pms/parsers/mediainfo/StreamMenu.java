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

public class StreamMenu {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamMenu() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "MenuID/String");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format");
	}

	/**
	 * Format used and any additional features or settings.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings, if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format.
	 * Shown in inform()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Profile");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Compression");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_Settings");
	}

	/**
	 * Features from the format that are required to fully support the file content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Codec identifier as indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID");
	}

	/**
	 * Codec identifier, as indicated by the container.
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec identifier.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description, as defined by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "CodecID_Description");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Codec");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Codec/Url");
	}

	/**
	 * Play time of the stream, in s (ms for text output).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.MENU, streamNumber, "Duration");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (e.g. 01:40:00.000).
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration/String5");
	}

	/**
	 * Start time of stream, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDurationStart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration_Start");
	}

	/**
	 * End time of stream, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDurationEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Duration_End");
	}

	/**
	 * Delay fixed in the stream (relative), in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.MENU, streamNumber, "Delay");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String1");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String2");
	}

	/**
	 * Delay fixed in the stream (relative) in format HH:MM:SS.mmm, with measurement.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (in case of timecode for example).
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame.
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Part of the file where the delay was set (e.g. Container, Stream, or empty).
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Delay_Source");
	}

	/**
	 * Frame rate mode, as acronym (e.g. CFR, VFR).
	 * Shown in Info_Capacities()
	 */
	public static final String getFrameRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "FrameRate_Mode");
	}

	/**
	 * Frame rate mode, as word (e.g. Constant, Variable).
	 */
	public static final String getFrameRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "FrameRate_Mode/String");
	}

	/**
	 * Frames per second, as float (e.g. 29.970).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.MENU, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second, with measurement (e.g. 29.970 (29970/1000) FPS).
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "FrameRate/String");
	}

	/**
	 * Numerator for determined frames per second (e.g. 29970).
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "FrameRate_Num");
	}

	/**
	 * Denominator for determined frames per second (e.g. 1000).
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "FrameRate_Den");
	}

	/**
	 * Numer of frames.
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "FrameCount");
	}

	/**
	 * List of programs available.
	 * Shown in Info_Capacities()
	 */
	public static final String getListStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "List_StreamKind");
	}

	/**
	 * List of programs available.
	 * Shown in Info_Capacities()
	 */
	public static final String getListStreamPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "List_StreamPos");
	}

	/**
	 * List of programs available.
	 * Shown in Info_Capacities()
	 */
	public static final String getList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "List");
	}

	/**
	 * List of programs available.
	 * Shown in inform()
	 */
	public static final String getListString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "List/String");
	}

	/**
	 * Name of this menu.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Title");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available (e.g. en, en-US, en-CN).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language");
	}

	/**
	 * Language, as full name (e.g. English).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language/String");
	}

	/**
	 * Language, as full name (e.g. English).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language/String1");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists (e.g. en).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language/String2");
	}

	/**
	 * Language, formatted as 3-letter ISO 639-2, if exists (e.g. eng).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language/String3");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists, with optional ISO 3166-1 country separated by a dash if available (e.g. en-US).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language/String4");
	}

	/**
	 * More information about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Language_More");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceKind");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceKind/String");
	}

	/**
	 * Name of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceName");
	}

	/**
	 * Channel of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceChannel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceChannel");
	}

	/**
	 * URL of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Service/Url");
	}

	/**
	 * Provider of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceProvider(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceProvider");
	}

	/**
	 * URL of provider of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceProviderUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceProvider/Url");
	}

	/**
	 * Type of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "ServiceType");
	}

	/**
	 * Television network name.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getNetworkName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "NetworkName");
	}

	/**
	 * Television network name of original broadcast.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalNetworkName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Original/NetworkName");
	}

	/**
	 * Country information of the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCountries(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Countries");
	}

	/**
	 * TimeZone information of the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeZones(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "TimeZones");
	}

	/**
	 * Legal rating of a movie.
	 * Format depends on country of origin (e.g.PG, 16)
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLawRating(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "LawRating");
	}

	/**
	 * Reason of the law rating.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLawRatingReason(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "LawRating_Reason");
	}

	/**
	 * Set if this stream should not be used (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Disabled");
	}

	/**
	 * Set if this stream should not be used (Yes, No).
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Disabled/String");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Default");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference (Yes, No).
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Default/String");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Forced");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie (Yes, No).
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.MENU, streamNumber, "AlternateGroup/String");
	}

	/**
	 * Used by third-party developers to know about the beginning of the chapters list, to be used by Get (Stream_Menu, x, Pos), where Pos is an Integer between Chapters_Pos_Begin and Chapters_Pos_End.
	 */
	public static final Long getChaptersPosBegin(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "Chapters_Pos_Begin");
	}

	/**
	 * Used by third-party developers to know about the end of the chapters list (this position excluded).
	 */
	public static final Long getChaptersPosEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.MENU, streamNumber, "Chapters_Pos_End");
	}

}
