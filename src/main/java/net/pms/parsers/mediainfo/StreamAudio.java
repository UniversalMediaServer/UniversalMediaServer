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

public class StreamAudio {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamAudio() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: General, Video, Audio, Text, Image, Menu, or Other
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: General, Video, Audio, Text, Image, Menu, or Other
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "MenuID/String");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format");
	}

	/**
	 * Format used and any additional features or settings.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings, if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format (old XML: 'Profile@Level' format).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Profile");
	}

	/**
	 * Level of the Format (MIXML only).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatLevel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Level");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Compression");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings");
	}

	/**
	 * Whether Spectral band replication settings used in encoding.
	 * Options are Yes (NBC)/No (Explicit)
	 * Note: NBC stands for "Not Backwards Compatable"
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsSBR(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_SBR");
	}

	/**
	 * Whether Spectral band replication settings used in encoding.
	 * Options are Yes (NBC)/No (Explicit)
	 * Note: NBC stands for "Not Backwards Compatable"
	 */
	public static final String getFormatSettingsSBRString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_SBR/String");
	}

	/**
	 * Whether Parametric Stereo settings used in encoding.
	 * Options are Yes (NBC)/No (Explicit)
	 * Note: NBC stands for "Not Backwards Compatable"
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsPS(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_PS");
	}

	/**
	 * Whether Parametric Stereo settings used in encoding.
	 * Options are Yes (NBC)/No (Explicit)
	 * Note: NBC stands for "Not Backwards Compatable"
	 */
	public static final String getFormatSettingsPSString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_PS/String");
	}

	/**
	 * Profile for format settings used in encoding (e.g. Joint Stereo).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Mode");
	}

	/**
	 * Extended format settings profile for Joint Stereo, derived from header data (e.g. Intensity Stereo + MS Stereo).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsModeExtension(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_ModeExtension");
	}

	/**
	 * Emphasis format settings for MPEG audio, derived from header data (e.g. 50/15ms).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsEmphasis(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Emphasis");
	}

	/**
	 * Settings for Vorbis spectral "floor" (a low-resolution representation of the audio spectrum for the given channel in the current frame) vector (e.g. Floor0).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsFloor(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Floor");
	}

	/**
	 * Agency or company responsible for format settings used in encoding (e.g. Microsoft).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsFirm(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Firm");
	}

	/**
	 * Order of bytes required for decoding.
	 * Options are Big/Little
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsEndianness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Endianness");
	}

	/**
	 * How numbers are stored in stream's encoding.
	 * Options are Signed/Unsigned
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsSign(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Sign");
	}

	/**
	 * U-law or A-law.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsLaw(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Law");
	}

	/**
	 * ITU Telecommunication Standardization Sector compression standard used in encoding (e.g. G.726).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsITU(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_ITU");
	}

	/**
	 * Wrapping mode set for format (e.g. Frame, Clip).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsWrapping(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_Settings_Wrapping");
	}

	/**
	 * Features from the format that are required to fully support the file content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Matrix format used in encoding (e.g. DTS Neural Audio).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMatrixFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Matrix_Format");
	}

	/**
	 * Internet Media Type (aka MIME Type, Content-Type).
	 * Shown in Info_Capacities()
	 */
	public static final String getInternetMediaType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "InternetMediaType");
	}

	/**
	 * How this file is muxed in the container (e.g. Muxed in Video #1).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMuxingMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "MuxingMode");
	}

	/**
	 * More information about MuxingMode.
	 * Shown in inform()
	 */
	public static final String getMuxingModeMoreInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "MuxingMode_MoreInfo");
	}

	/**
	 * Codec identifier as indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID");
	}

	/**
	 * Codec identifier, as indicated by the container.
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec identifier.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "CodecID_Description");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecFamily(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec/Family");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec/Url");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecCC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec/CC");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Description");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Profile");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsAutomatic(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Automatic");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsFloor(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Floor");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsFirm(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Firm");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsEndianness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Endianness");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsSign(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Sign");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsLaw(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_Law");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsITU(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Codec_Settings_ITU");
	}

	/**
	 * Play time, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Duration");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (e.g. 01:40:00.000).
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration/String5");
	}

	/**
	 * Duration of the first frame (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Duration_LastFrame");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Duration_LastFrame/String5");
	}

	/**
	 * Duration of content stored in the file, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Source_Duration");
	}

	/**
	 * Duration of content stored in the file, in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String");
	}

	/**
	 * Duration of content stored in the file, in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String1");
	}

	/**
	 * Duration of content stored in the file, in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String2");
	}

	/**
	 * Duration of content stored in the file, in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String3");
	}

	/**
	 * Duration of content stored in the file, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String4");
	}

	/**
	 * Duration of content stored in the file, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration/String5");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame of content stored in the file (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_Duration_LastFrame/String5");
	}

	/**
	 * Bit rate mode of this stream, as acronym (VBR, CBR).
	 * Shown in Info_Capacities()
	 */
	public static final String getBitRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Mode");
	}

	/**
	 * Bit rate mode of this stream, as word (Variable, Constant).
	 * Shown in inform()
	 */
	public static final String getBitRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Mode/String");
	}

	/**
	 * Bit rate of this stream, in bits per second (e.g. 128026).
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "BitRate");
	}

	/**
	 * Bit rate of this stream, with measurement (e.g. 128 kb/s).
	 * Shown in inform()
	 */
	public static final String getBitRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate/String");
	}

	/**
	 * Minimum bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "BitRate_Minimum");
	}

	/**
	 * Minimum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Minimum/String");
	}

	/**
	 * Nominal bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "BitRate_Nominal");
	}

	/**
	 * Nominal bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Nominal/String");
	}

	/**
	 * Maximum bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "BitRate_Maximum");
	}

	/**
	 * Maximum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Maximum/String");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "BitRate_Encoded");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bits per second.
	 */
	public static final String getBitRateEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitRate_Encoded/String");
	}

	/**
	 * Number of channels (e.g. 2).
	 * Shown in Info_Capacities()
	 */
	public static final Long getChannels(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Channel(s)");
	}

	/**
	 * Number of channels (with measurement).
	 * Shown in inform()
	 */
	public static final String getChannelsString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Channel(s)/String");
	}

	/**
	 * Number of channels, considering data only from the codec (e.g. 6).
	 * Shown in Info_Capacities()
	 */
	public static final Long getChannelsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Channel(s)_Original");
	}

	/**
	 * Number of channels, considering data only from the codec, with measurement (e.g. 6 channels).
	 * Shown in inform()
	 */
	public static final String getChannelsOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Channel(s)_Original/String");
	}

	/**
	 * Number of channels after matrix decoding.
	 * Shown in Info_Capacities()
	 */
	public static final Long getMatrixChannels(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Matrix_Channel(s)");
	}

	/**
	 * Number of channels after matrix decoding, with measurement.
	 * Shown in inform()
	 */
	public static final String getMatrixChannelsString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Matrix_Channel(s)/String");
	}

	/**
	 * Position of channels (e.g. Front: L C R, Side: L R, Back: L R, LFE).
	 * Shown in Info_Capacities()
	 */
	public static final String getChannelPositions(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelPositions");
	}

	/**
	 * Position of channels, considering data only from the codec (e.g. Front: L C R, Side: L R, Back: L R, LFE).
	 * Shown in Info_Capacities()
	 */
	public static final String getChannelPositionsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelPositions_Original");
	}

	/**
	 * Position of channels in x/y.z format (e.g. 3/2/0.1).
	 */
	public static final String getChannelPositionsString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelPositions/String2");
	}

	/**
	 * Position of channels in x/y.z format, considering data only from the codec (e.g. 3/2/0.1).
	 */
	public static final String getChannelPositionsOriginalString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelPositions_Original/String2");
	}

	/**
	 * Position of channels after matrix decoding.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMatrixChannelPositions(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Matrix_ChannelPositions");
	}

	/**
	 * Position of channels after matrix decoding in x/y.z format.
	 */
	public static final String getMatrixChannelPositionsString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Matrix_ChannelPositions/String2");
	}

	/**
	 * Layout of channels (e.g. L R C LFE Ls Rs Lb Rb).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChannelLayout(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelLayout");
	}

	/**
	 * Layout of channels, considering data only from the codec (e.g. L R C LFE Ls Rs Lb Rb).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChannelLayoutOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelLayout_Original");
	}

	/**
	 * ID of layout of channels (e.g. MXF descriptor channel assignment).
	 * Warning, sometimes this is not enough for uniquely identifying a layout (e.g.MXF descriptor channel assignment is SMPTE 377-4)
	 * For AC-3, the form is x,y with x=acmod and y=lfeon
	 * Shown in Info_Capacities()
	 */
	public static final String getChannelLayoutID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ChannelLayoutID");
	}

	/**
	 * Samples per frame (e.g. 1536).
	 * Shown in Info_Capacities()
	 */
	public static final Double getSamplesPerFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "SamplesPerFrame");
	}

	/**
	 * Sampling rate, in Hertz (e.g. 48000).
	 * Shown in Info_Capacities()
	 */
	public static final Double getSamplingRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "SamplingRate");
	}

	/**
	 * Sampling rate, in Hertz, with measurement (e.g. 48.0 KHz).
	 * Shown in inform()
	 */
	public static final String getSamplingRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "SamplingRate/String");
	}

	/**
	 * Sample count (based on sampling rate).
	 */
	public static final Long getSamplingCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "SamplingCount");
	}

	/**
	 * Source Sample count (based on sampling rate), with information derived from header metadata.
	 */
	public static final Long getSourceSamplingCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Source_SamplingCount");
	}

	/**
	 * Frames per second, as integer (e.g. 29.970).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second, with measurement (e.g. 29.970 (29970/1000) FPS).
	 * Shown in inform()
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "FrameRate/String");
	}

	/**
	 * Numerator for determined frames per second (e.g. 29970).
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "FrameRate_Num");
	}

	/**
	 * Denominator for determined frames per second (e.g. 1000).
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "FrameRate_Den");
	}

	/**
	 * Frame count (a frame contains a count of samples depends of the format).
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "FrameCount");
	}

	/**
	 * Source Frame count (a frame contains a count of samples depends of the format).
	 */
	public static final Long getSourceFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Source_FrameCount");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getResolution(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Resolution");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getResolutionString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Resolution/String");
	}

	/**
	 * Number of bits in each sample (resolution) of stream (e.g. 16).
	 * This field will show the significant bits if the stored bit depth is different
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "BitDepth");
	}

	/**
	 * Number of bits in each sample (resolution) of stream (e.g. 16).
	 * This field will show the significant bits if the stored bit depth is different
	 * Shown in inform()
	 */
	public static final String getBitDepthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitDepth/String");
	}

	/**
	 * Number of bits in each sample (resolution), as detected during scan of the input by the muxer, in bits (e.g. 24).
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepthDetected(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "BitDepth_Detected");
	}

	/**
	 * Number of bits in each sample (resolution), as detected during scan of the input by the muxer, in bits (e.g. 24).
	 * Shown in inform()
	 */
	public static final String getBitDepthDetectedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitDepth_Detected/String");
	}

	/**
	 * Stored number of bits in each sample (resolution), in bits (e.g. 24).
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepthStored(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "BitDepth_Stored");
	}

	/**
	 * Stored number of bits in each sample (resolution), in bits (e.g. 24).
	 * Shown in inform()
	 */
	public static final String getBitDepthStoredString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "BitDepth_Stored/String");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in Info_Capacities()
	 */
	public static final String getCompressionMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Compression_Mode");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in inform()
	 */
	public static final String getCompressionModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Compression_Mode/String");
	}

	/**
	 * Stream size divided by uncompressed stream size.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Double getCompressionRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Compression_Ratio");
	}

	/**
	 * Delay fixed in the stream (relative), in milliseconds.
	 */
	public static final Double getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Delay");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String1");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String2");
	}

	/**
	 * Delay fixed in the stream (relative) in format HH:MM:SS.mmm, with measurement.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (i.e.
	 * in case of timecode)
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame information.
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Source");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySourceString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Source/String");
	}

	/**
	 * Delay, considering data only from the codec, in milliseconds.
	 */
	public static final Long getDelayOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Delay_Original");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String1");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String2");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS.mmm.
	 */
	public static final String getDelayOriginalString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String3");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayOriginalString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String4");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayOriginalString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original/String5");
	}

	/**
	 * Delay settings, considering data only from the codec (i.e.
	 * in case of timecode)
	 */
	public static final String getDelayOriginalSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original_Settings");
	}

	/**
	 * Delay drop frame information, considering data only from the codec.
	 */
	public static final String getDelayOriginalDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original_DropFrame");
	}

	/**
	 * Delay source (e.g. Container, Stream, empty).
	 */
	public static final String getDelayOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Delay_Original_Source");
	}

	/**
	 * Delay fixed in the stream relative to video, in milliseconds (e.g. -80).
	 */
	public static final Double getVideoDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Video_Delay");
	}

	/**
	 * Delay fixed in the stream relative to video, in milliseconds, with measurement (e.g. -80 ms).
	 * Shown in inform()
	 */
	public static final String getVideoDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String");
	}

	/**
	 * Delay fixed in the stream relative to video, in milliseconds, with measurement (e.g. -80 ms).
	 */
	public static final String getVideoDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String1");
	}

	/**
	 * Delay fixed in the stream relative to video, in milliseconds, with measurement (e.g. -80 ms).
	 */
	public static final String getVideoDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String2");
	}

	/**
	 * Delay fixed in the stream relative to video, in format HH:MM:SS.mmm (e.g. -00:00:00.080).
	 */
	public static final String getVideoDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with the last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getVideoDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String4");
	}

	/**
	 * Delay fixed in the stream relative to video, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getVideoDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video_Delay/String5");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getVideo0Delay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Video0_Delay");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String1");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String2");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String3");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String4");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Video0_Delay/String5");
	}

	/**
	 * Time code for first frame in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "TimeCode_FirstFrame");
	}

	/**
	 * Time code for last frame (excluding the duration of the last frame) in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "TimeCode_LastFrame");
	}

	/**
	 * Time code drop frame.
	 */
	public static final String getTimeCodeDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "TimeCode_DropFrame");
	}

	/**
	 * Additional time code settings.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "TimeCode_Settings");
	}

	/**
	 * Time code source (Container, Stream, SystemScheme1, SDTI, ANC, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "TimeCode_Source");
	}

	/**
	 * The gain to apply to reach 89dB SPL on playback.
	 * Shown in Info_Capacities()
	 */
	public static final String getReplayGainGain(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ReplayGain_Gain");
	}

	/**
	 * The gain to apply to reach 89dB SPL on playback.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getReplayGainGainString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ReplayGain_Gain/String");
	}

	/**
	 * The maximum absolute peak value of the item.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getReplayGainPeak(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ReplayGain_Peak");
	}

	/**
	 * Size of this stream, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamSize");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String1");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String2");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String3");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String4");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * Size of this stream after demuxing, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Size of content stored in the file, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Source_StreamSize");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getSourceStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String1");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String2");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String3");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String4");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getSourceStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Proportion");
	}

	/**
	 * Size of this stream when encoded, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String1");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String2");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String3");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String4");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded/String5");
	}

	/**
	 * Encoded Stream size divided by file size.
	 */
	public static final String getStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "StreamSize_Encoded_Proportion");
	}

	/**
	 * Size of content stored in the file when encoded, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String1");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String2");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String3");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String4");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded/String5");
	}

	/**
	 * Source Encoded Stream size divided by file size.
	 */
	public static final String getSourceStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Source_StreamSize_Encoded_Proportion");
	}

	/**
	 * How this stream is aligned in the container (e.g. Aligned, Split).
	 * Shown in Info_Capacities()
	 */
	public static final String getAlignment(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Alignment");
	}

	/**
	 * How this stream is aligned in the container (e.g. Aligned, Split).
	 * Shown in inform()
	 */
	public static final String getAlignmentString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Alignment/String");
	}

	/**
	 * For interleaved video, between how many video frames this stream is inserted (e.g. 0.51 video frame).
	 * Shown in Info_Capacities()
	 */
	public static final Double getInterleaveVideoFrames(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Interleave_VideoFrames");
	}

	/**
	 * For interleaved video, between how much time, in milliseconds, this stream is inserted (e.g. 21 ms).
	 * Shown in Info_Capacities()
	 */
	public static final Double getInterleaveDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Interleave_Duration");
	}

	/**
	 * For interleaved video, between how many video frames this stream is inserted, as duration with measurement and amount of frame(s) in parenthesis (e.g. 21 ms (0.51 video frame)).
	 * Shown in inform()
	 */
	public static final String getInterleaveDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Interleave_Duration/String");
	}

	/**
	 * How much time is buffered before the first video frame, in milliseconds (e.g. 500).
	 * Shown in Info_Capacities()
	 */
	public static final Double getInterleavePreload(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.AUDIO, streamNumber, "Interleave_Preload");
	}

	/**
	 * How much time is buffered before the first video frame, in milliseconds with measurement (e.g. 500 ms).
	 * Shown in inform()
	 */
	public static final String getInterleavePreloadString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Interleave_Preload/String");
	}

	/**
	 * Title of track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Title");
	}

	/**
	 * Name of the software package used to create the file (e.g. Microsoft WaveEdiTY).
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplication(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application");
	}

	/**
	 * Name of the software package used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedApplicationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application/String");
	}

	/**
	 * Name of the company of the encoding application.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application_CompanyName");
	}

	/**
	 * Name of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application_Name");
	}

	/**
	 * Version of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application_Version");
	}

	/**
	 * URL associated with the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Application_Url");
	}

	/**
	 * Software used to create the file.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library");
	}

	/**
	 * Software used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedLibraryString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library/String");
	}

	/**
	 * Name of the encoding software company.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library_CompanyName");
	}

	/**
	 * Name of the encoding software.
	 */
	public static final String getEncodedLibraryName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library_Name");
	}

	/**
	 * Version of the encoding software.
	 */
	public static final String getEncodedLibraryVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library_Version");
	}

	/**
	 * Release date of the encoding software, in UTC.
	 */
	public static final String getEncodedLibraryDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library_Date");
	}

	/**
	 * Parameters used by the encoding software.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrarySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Library_Settings");
	}

	/**
	 * Operating System of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedOperatingSystem(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_OperatingSystem");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available (e.g. en, en-US, en-CN).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language");
	}

	/**
	 * Language, as full name (e.g. English).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language/String");
	}

	/**
	 * Language, as full name (e.g. English).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language/String1");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists (e.g. en).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language/String2");
	}

	/**
	 * Language, formatted as 3-letter ISO 639-2, if exists (e.g. eng).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language/String3");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists, with optional ISO 3166-1 country separated by a dash if available (e.g. en-US).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language/String4");
	}

	/**
	 * More information about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Language_More");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ServiceKind");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "ServiceKind/String");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Disabled");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Disabled/String");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Default");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Default/String");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Forced");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "AlternateGroup/String");
	}

	/**
	 * Time that the encoding of this item was completed, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encoded_Date");
	}

	/**
	 * Time that the tags were added to this item, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Tagged_Date");
	}

	/**
	 * Whether this stream is encrypted and, if available, how it is encrypted.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryption(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.AUDIO, streamNumber, "Encryption");
	}

}
