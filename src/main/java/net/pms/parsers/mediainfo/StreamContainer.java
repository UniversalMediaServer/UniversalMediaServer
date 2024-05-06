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
 * StreamContainer (was General but to do is moving to Container)
 */
public class StreamContainer {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamContainer() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material, taken from Tag metadata.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material, taken from Tag metadata (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "MenuID/String");
	}

	/**
	 * Total number of General streams in this file.
	 */
	public static final Long getGeneralCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "GeneralCount");
	}

	/**
	 * Total number of Video streams in this file.
	 */
	public static final Long getVideoCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "VideoCount");
	}

	/**
	 * Total number of Audio streams in this file.
	 */
	public static final Long getAudioCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "AudioCount");
	}

	/**
	 * Total number of Text streams in this file.
	 */
	public static final Long getTextCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "TextCount");
	}

	/**
	 * Total number of Other streams in this file.
	 */
	public static final Long getOtherCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "OtherCount");
	}

	/**
	 * Total number of Image streams in this file.
	 */
	public static final Long getImageCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "ImageCount");
	}

	/**
	 * Total number of Menu streams in this file.
	 */
	public static final Long getMenuCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "MenuCount");
	}

	/**
	 * Video codecs found in this file, separated by forward slash ("/").
	 */
	public static final String getVideoFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Video_Format_List");
	}

	/**
	 * Video codecs found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getVideoFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Video_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideoCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Video_Codec_List");
	}

	/**
	 * List of video stream languages in this file separated by forward slash ("/").
	 */
	public static final String getVideoLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Video_Language_List");
	}

	/**
	 * Audio codecs found in this file, separated by forward slash ("/").
	 */
	public static final String getAudioFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Audio_Format_List");
	}

	/**
	 * Audio codecs found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getAudioFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Audio_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getAudioCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Audio_Codec_List");
	}

	/**
	 * List of audio stream languages in this file separated by forward slash ("/").
	 */
	public static final String getAudioLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Audio_Language_List");
	}

	/**
	 * Total count of channels in all audio streams.
	 */
	public static final Long getAudioChannelsTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Audio_Channels_Total");
	}

	/**
	 * Text codecs found in this file, separated by forward slash ("/").
	 */
	public static final String getTextFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Text_Format_List");
	}

	/**
	 * Text codecs found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getTextFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Text_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getTextCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Text_Codec_List");
	}

	/**
	 * List of text stream languages in this file separated by forward slash ("/").
	 */
	public static final String getTextLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Text_Language_List");
	}

	/**
	 * Other data formats found in this file, separated by forward slash ("/").
	 */
	public static final String getOtherFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Other_Format_List");
	}

	/**
	 * Other data formats found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getOtherFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Other_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getOtherCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Other_Codec_List");
	}

	/**
	 * List of other stream languages (typically Chapters) in this file separated by forward slash ("/").
	 */
	public static final String getOtherLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Other_Language_List");
	}

	/**
	 * Image codecs found in this file, separated by forward slash ("/").
	 */
	public static final String getImageFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Image_Format_List");
	}

	/**
	 * Image codecs found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getImageFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Image_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getImageCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Image_Codec_List");
	}

	/**
	 * List of image stream languages in this file separated by forward slash ("/").
	 */
	public static final String getImageLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Image_Language_List");
	}

	/**
	 * Menu formats found in this file, separated by forward slash ("/").
	 */
	public static final String getMenuFormatList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Menu_Format_List");
	}

	/**
	 * Menu formats found in this file, separated by forward slash ("/") and including common alternative codec names.
	 */
	public static final String getMenuFormatWithHintList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Menu_Format_WithHint_List");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getMenuCodecList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Menu_Codec_List");
	}

	/**
	 * List of menu stream languages in this file separated by forward slash ("/").
	 */
	public static final String getMenuLanguageList(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Menu_Language_List");
	}

	/**
	 * Full path for this file (Folder+Name+Extension).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCompleteName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CompleteName");
	}

	/**
	 * Folder name for this file.
	 */
	public static final String getFolderName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FolderName");
	}

	/**
	 * File name and extension.
	 */
	public static final String getFileNameExtension(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileNameExtension");
	}

	/**
	 * File name only.
	 */
	public static final String getFileName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileName");
	}

	/**
	 * File extension only.
	 */
	public static final String getFileExtension(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileExtension");
	}

	/**
	 * Complete name (Folder+Name+Extension) of the last file (in the case of a sequence of files).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCompleteNameLast(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CompleteName_Last");
	}

	/**
	 * Folder name only of the last file (in the case of a sequence of files).
	 */
	public static final String getFolderNameLast(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FolderName_Last");
	}

	/**
	 * File name and extension of the last file (in the case of a sequence of files).
	 */
	public static final String getFileNameExtensionLast(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileNameExtension_Last");
	}

	/**
	 * File name only of the last file (in the case of a sequence of files).
	 */
	public static final String getFileNameLast(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileName_Last");
	}

	/**
	 * File extension only of the last file (in the case of a sequence of files).
	 */
	public static final String getFileExtensionLast(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileExtension_Last");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format");
	}

	/**
	 * Format used + additional features.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format/Url");
	}

	/**
	 * Known extensions for the identified format.
	 */
	public static final String getFormatExtensions(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format/Extensions");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings if there is one.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Profile");
	}

	/**
	 * Level of the Format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatLevel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Level");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Compression");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_Settings");
	}

	/**
	 * Features required to fully support the file content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Internet Media Type (aka MIME Type, Content-Type).
	 * Shown in Info_Capacities()
	 */
	public static final String getInternetMediaType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "InternetMediaType");
	}

	/**
	 * Codec ID, if defined by the container.
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID");
	}

	/**
	 * Codec ID, if defined by the container (String format).
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description, as defined by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID_Description");
	}

	/**
	 * Version of the CodecID.
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID_Version");
	}

	/**
	 * List of codecs that are compatible with the identified container.
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDCompatible(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CodecID_Compatible");
	}

	/**
	 * If Audio and video are muxed.
	 * Shown in Info_Capacities()
	 */
	public static final String getInterleaved(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Interleaved");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec/Url");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecExtensions(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec/Extensions");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec_Settings");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsAutomatic(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Codec_Settings_Automatic");
	}

	/**
	 * File size, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final String getFileSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize");
	}

	/**
	 * File size with measurement (measured in powers of 1024).
	 * Shown in inform()
	 */
	public static final String getFileSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize/String");
	}

	/**
	 * File size with measurement (measured in powers of 1024) rounded to the nearest integer.
	 */
	public static final String getFileSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize/String1");
	}

	/**
	 * File size with measurement (measured in powers of 1024) rounded to the two most significant digits.
	 */
	public static final String getFileSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize/String2");
	}

	/**
	 * File size with measurement (measured in powers of 1024) rounded to the three most significant digits.
	 */
	public static final String getFileSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize/String3");
	}

	/**
	 * File size with measurement (measured in powers of 1024) rounded to the four most significant digits.
	 */
	public static final String getFileSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FileSize/String4");
	}

	/**
	 * Play time of the content, in s (ms for text output).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "Duration");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (e.g. 01:40:00.000).
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration/String5");
	}

	/**
	 * Start time of stream, in UTC.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationStart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "Duration_Start");
	}

	public static final String getDurationStartString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String");
	}

	public static final String getDurationStartString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String1");
	}

	public static final String getDurationStartString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String2");
	}

	public static final String getDurationStartString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String3");
	}

	public static final String getDurationStartString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String4");
	}

	public static final String getDurationStartString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_Start/String5");
	}

	/**
	 * End time of stream, in UTC.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "Duration_End");
	}

	public static final String getDurationEndString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String");
	}

	public static final String getDurationEndString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String1");
	}

	public static final String getDurationEndString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String2");
	}

	public static final String getDurationEndString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String3");
	}

	public static final String getDurationEndString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String4");
	}

	public static final String getDurationEndString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Duration_End/String5");
	}

	/**
	 * Bit rate mode of all streams (CBR, VBR).
	 * Shown in Info_Capacities()
	 */
	public static final String getOverallBitRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate_Mode");
	}

	/**
	 * Bit rate mode of all streams, as word (Variable, Constant).
	 * Shown in inform()
	 */
	public static final String getOverallBitRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate_Mode/String");
	}

	/**
	 * Bit rate of all streams, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getOverallBitRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "OverallBitRate");
	}

	/**
	 * Bit rate of all streams, with measurement (e.g. 14.2 kb/s).
	 * Shown in inform()
	 */
	public static final String getOverallBitRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate/String");
	}

	/**
	 * Minimum total bit rate of all streams, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getOverallBitRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "OverallBitRate_Minimum");
	}

	/**
	 * Minimum bit rate of all streams, with measurement.
	 * Shown in inform()
	 */
	public static final String getOverallBitRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate_Minimum/String");
	}

	/**
	 * Nominal bit rate of all streams, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getOverallBitRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "OverallBitRate_Nominal");
	}

	/**
	 * Nominal bit rate of all streams, with measurement.
	 * Shown in inform()
	 */
	public static final String getOverallBitRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate_Nominal/String");
	}

	/**
	 * Maximum bit rate of all streams, in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getOverallBitRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "OverallBitRate_Maximum");
	}

	/**
	 * Maximum bit rate of all streams, with measurement.
	 * Shown in inform()
	 */
	public static final String getOverallBitRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OverallBitRate_Maximum/String");
	}

	/**
	 * Frames per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.GENERAL, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second, with measurement.
	 * Shown in inform()
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "FrameRate/String");
	}

	/**
	 * Frames per second, numerator.
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "FrameRate_Num");
	}

	/**
	 * Frames per second, denominator.
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "FrameRate_Den");
	}

	/**
	 * Frame count, if a stream has the same frame rate everywhere.
	 * Shown in Info_Capacities()
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "FrameCount");
	}

	/**
	 * Delay fixed in the stream (relative), is s (ms for text output).
	 * Shown in Info_Capacities()
	 */
	public static final Long getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Delay");
	}

	/**
	 * Delay with measurement and rounded to integer (e.g. 213 ms).
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String");
	}

	/**
	 * Delay with measurement and rounded to integer (e.g. 213 ms).
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String1");
	}

	/**
	 * Delay with measurement and rounded to integer (e.g. 213 ms).
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String2");
	}

	/**
	 * Delay in format HH:MM:SS.mmm.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with the last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (in case of timecode, for example).
	 * Shown in Info_Capacities()
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame.
	 * Shown in Info_Capacities()
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Delay source (Container, Stream, or empty).
	 * Shown in Info_Capacities()
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay_Source");
	}

	/**
	 * Delay source (Container, Stream, or empty).
	 */
	public static final String getDelaySourceString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Delay_Source/String");
	}

	/**
	 * Size of this stream, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamSize");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String1");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String2");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String3");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String4");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * Size of this stream after demuxing, in bytes.
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Header field size, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeaderSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "HeaderSize");
	}

	/**
	 * Data field size, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getDataSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "DataSize");
	}

	/**
	 * Footer field size, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getFooterSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "FooterSize");
	}

	/**
	 * Set if this file is streamable or not (Yes, No).
	 * Shown in Info_Capacities()
	 */
	public static final String getIsStreamable(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "IsStreamable");
	}

	/**
	 * The gain to apply to reach 89dB SPL on playback.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumReplayGainGain(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album_ReplayGain_Gain");
	}

	/**
	 * The gain to apply to reach 89dB SPL on playback.
	 * Shown in inform()
	 */
	public static final String getAlbumReplayGainGainString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album_ReplayGain_Gain/String");
	}

	/**
	 * The maximum absolute peak value of the item.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumReplayGainPeak(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album_ReplayGain_Peak");
	}

	/**
	 * Encryption.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryption(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption");
	}

	/**
	 * Encryption format.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_Format");
	}

	/**
	 * Encryption length (128, 192 or 256 bits).
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionLength(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_Length");
	}

	/**
	 * Encryption method.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionMethod(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_Method");
	}

	/**
	 * Encryption mode.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_Mode");
	}

	/**
	 * Encryption padding.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionPadding(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_Padding");
	}

	/**
	 * Encryption initialization vector.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryptionInitializationVector(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encryption_InitializationVector");
	}

	/**
	 * Universal Ad-ID, see https://ad-id.org for more information.
	 * Shown in inform()
	 */
	public static final String getUniversalAdIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UniversalAdID/String");
	}

	/**
	 * Universal Ad-ID registry.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniversalAdIDRegistry(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UniversalAdID_Registry");
	}

	/**
	 * Universal Ad-ID value.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniversalAdIDValue(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UniversalAdID_Value");
	}

	/**
	 * Title of file.
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Title");
	}

	/**
	 * More title information.
	 * Shown in Info_Capacities()
	 */
	public static final String getTitleMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Title_More");
	}

	/**
	 * URL.
	 */
	public static final String getTitleUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Title/Url");
	}

	/**
	 * Universe that the file's contents belong to (e.g. Star Wars, Stargate, Buffy, Dragonball).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDomain(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Domain");
	}

	/**
	 * Name of the series (e.g. Star Wars movies, Stargate SG-1, Angel).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCollection(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Collection");
	}

	/**
	 * Name of the season (e.g. first Star Wars Trilogy, Season 1).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSeason(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Season");
	}

	/**
	 * Number of the Season.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getSeasonPosition(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Season_Position");
	}

	/**
	 * Total number of seasons.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getSeasonPositionTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Season_Position_Total");
	}

	/**
	 * Name of the movie (e.g. Star Wars: A New Hope).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMovie(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Movie");
	}

	/**
	 * More information about the Movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMovieMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Movie_More");
	}

	/**
	 * Country where the movie was produced.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMovieCountry(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Movie/Country");
	}

	/**
	 * Homepage for the movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMovieUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Movie/Url");
	}

	/**
	 * Name of the album (e.g. The Joshua Tree).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album");
	}

	/**
	 * More information about the Album.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album_More");
	}

	/**
	 * Alternate name of the album, optimized for sorting purposes (e.g. Joshua Tree, The).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumSort(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album/Sort");
	}

	/**
	 * Album performer/artist of this file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumPerformer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album/Performer");
	}

	/**
	 * Alternate name for the performer, optimized for sorting purposes (e.g. Beatles, The).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumPerformerSort(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album/Performer/Sort");
	}

	/**
	 * Homepage of the album performer/artist.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAlbumPerformerUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Album/Performer/Url");
	}

	/**
	 * Name of the comic book series.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComic(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Comic");
	}

	/**
	 * More information about the comic book series.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComicMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Comic_More");
	}

	/**
	 * Total number of comics.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getComicPositionTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Comic/Position_Total");
	}

	/**
	 * Name of the part (e.g. CD1, CD2).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Part");
	}

	/**
	 * Number of the part.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getPartPosition(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Part/Position");
	}

	/**
	 * Total number of parts.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getPartPositionTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Part/Position_Total");
	}

	/**
	 * Name of the reel.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getReel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Reel");
	}

	/**
	 * Number of the reel.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getReelPosition(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Reel/Position");
	}

	/**
	 * Total number of reel.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getReelPositionTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Reel/Position_Total");
	}

	/**
	 * Name of the track (e.g. track 1, track 2).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTrack(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Track");
	}

	/**
	 * More information about the Track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTrackMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Track_More");
	}

	/**
	 * Link to a site about this Track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTrackUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Track/Url");
	}

	/**
	 * Alternate name for the track, optimized for sorting purposes.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTrackSort(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Track/Sort");
	}

	/**
	 * Number of this Track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getTrackPosition(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Track/Position");
	}

	/**
	 * Total number of tracks.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getTrackPositionTotal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Track/Position_Total");
	}

	/**
	 * MXF package name.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPackageName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "PackageName");
	}

	/**
	 * iTunes grouping.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getGrouping(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Grouping");
	}

	/**
	 * Name of the Chapter.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChapter(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Chapter");
	}

	/**
	 * Name of the Subtrack.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSubTrack(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "SubTrack");
	}

	/**
	 * Original name of the Album.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalAlbum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Album");
	}

	/**
	 * Original name of the Movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalMovie(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Movie");
	}

	/**
	 * Original name of the Part.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalPart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Part");
	}

	/**
	 * Original name of the Track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalTrack(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Track");
	}

	/**
	 * iTunes compilation.
	 * Shown in Info_Capacities()
	 */
	public static final String getCompilation(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Compilation");
	}

	/**
	 * iTunes compilation.
	 * Shown in inform()
	 */
	public static final String getCompilationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Compilation/String");
	}

	/**
	 * Main performer(s)/artist(s).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPerformer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Performer");
	}

	/**
	 * Alternate name for the performer, optimized for sorting purposes (e.g. Beatles, The).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPerformerSort(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Performer/Sort");
	}

	/**
	 * Homepage of the performer/artist.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPerformerUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Performer/Url");
	}

	/**
	 * Original artist(s)/performer(s).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalPerformer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Performer");
	}

	/**
	 * Band/orchestra/accompaniment/musician.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAccompaniment(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Accompaniment");
	}

	/**
	 * Name of the original composer.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComposer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Composer");
	}

	/**
	 * Nationality of the primary composer of the piece.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComposerNationality(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Composer/Nationality");
	}

	/**
	 * Nationality of the primary composer of the piece (e.g. Mozart, Wolfgang Amadeus).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComposerSort(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Composer/Sort");
	}

	/**
	 * The person who arranged the piece (e.g. Ravel).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getArranger(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Arranger");
	}

	/**
	 * The person who wrote the lyrics for the piece.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLyricist(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Lyricist");
	}

	/**
	 * Original lyricist(s)/text writer(s).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalLyricist(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Lyricist");
	}

	/**
	 * The artist(s) who performed the work.
	 * In classical music this would be the conductor, orchestra, soloists, etc
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getConductor(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Conductor");
	}

	/**
	 * Name of the director.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDirector(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Director");
	}

	/**
	 * Name of the codirector.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCoDirector(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CoDirector");
	}

	/**
	 * Name of the assistant director.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAssistantDirector(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "AssistantDirector");
	}

	/**
	 * Name of the director of photography, also known as cinematographer.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDirectorOfPhotography(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "DirectorOfPhotography");
	}

	/**
	 * Name of the sound engineer or sound recordist.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSoundEngineer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "SoundEngineer");
	}

	/**
	 * Name of the person who oversees the artists and craftspeople who build the sets.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getArtDirector(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ArtDirector");
	}

	/**
	 * Name of the person responsible for designing the overall visual appearance of a movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getProductionDesigner(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ProductionDesigner");
	}

	/**
	 * Name of the choreographer.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChoreographer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Choreographer");
	}

	/**
	 * Name of the costume designer.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCostumeDesigner(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CostumeDesigner");
	}

	/**
	 * Real name of an actor/actress playing a role in the movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getActor(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Actor");
	}

	/**
	 * Name of the character an actor or actress plays in this movie.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getActorCharacter(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Actor_Character");
	}

	/**
	 * Author of the story or script.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getWrittenBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "WrittenBy");
	}

	/**
	 * Author of the screenplay or scenario (used for movies and TV shows).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getScreenplayBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ScreenplayBy");
	}

	/**
	 * Editors name.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEditedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "EditedBy");
	}

	/**
	 * Name of the person or organization that commissioned the subject of the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCommissionedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CommissionedBy");
	}

	/**
	 * Name of the producer of the media.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getProducer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Producer");
	}

	/**
	 * Name of a co-producer of the media.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCoProducer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CoProducer");
	}

	/**
	 * Name of an executive producer of the media.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getExecutiveProducer(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ExecutiveProducer");
	}

	/**
	 * Main musical artist for the media.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMusicBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "MusicBy");
	}

	/**
	 * Company responsible for distribution of the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDistributedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "DistributedBy");
	}

	/**
	 * Name of the person or organization who supplied the original subject.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceFormDistributedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm/DistributedBy");
	}

	/**
	 * The engineer who mastered the content for a physical medium or for digital distribution.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteredBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "MasteredBy");
	}

	/**
	 * Name of the person/organisation that encoded/ripped the audio file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "EncodedBy");
	}

	/**
	 * Name of the artist(s) that interpreted, remixed, or otherwise modified the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getRemixedBy(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "RemixedBy");
	}

	/**
	 * Main production studio of the media.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getProductionStudio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ProductionStudio");
	}

	/**
	 * A very general metadata tag for everyone else that wants to be listed.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getThanksTo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ThanksTo");
	}

	/**
	 * Name of the organization publishing the media (i.e.
	 * the record label)
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPublisher(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Publisher");
	}

	/**
	 * Publisher's official webpage.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPublisherURL(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Publisher/URL");
	}

	/**
	 * Brand or trademark associated with the marketing of music recordings and music videos.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLabel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Label");
	}

	/**
	 * Main genre of the media (e.g. classical, ambient-house, synthpop, sci-fi, drama, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getGenre(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Genre");
	}

	/**
	 * Podcast category.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPodcastCategory(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "PodcastCategory");
	}

	/**
	 * Intended to reflect the mood of the item with a few keywords (e.g. Romantic, Sad, Uplifting, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMood(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Mood");
	}

	/**
	 * The type or genre of the content (e.g. Documentary, Feature Film, Cartoon, Music Video, Music, Sound FX, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getContentType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ContentType");
	}

	/**
	 * Describes the topic of the file (e.g. "Aerial view of Seattle.").
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSubject(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Subject");
	}

	/**
	 * A short description of the contents (e.g. "Two birds flying.").
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Description");
	}

	/**
	 * Keywords for the content separated by a comma, used for searching.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getKeywords(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Keywords");
	}

	/**
	 * Plot outline or a summary of the story.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSummary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Summary");
	}

	/**
	 * Description of the story line of the item.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getSynopsis(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Synopsis");
	}

	/**
	 * Describes the period that the piece is from or about (e.g. Renaissance).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPeriod(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Period");
	}

	/**
	 * Legal rating of a movie.
	 * Format depends on country of origin (e.g.PG, 16)
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLawRating(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "LawRating");
	}

	/**
	 * Reason for the law rating.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLawRatingReason(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "LawRating_Reason");
	}

	/**
	 * The ICRA rating (previously RSACi).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getICRA(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ICRA");
	}

	/**
	 * Date/year that the content was released.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getReleasedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Released_Date");
	}

	/**
	 * Date/year that the content was originally released.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalReleasedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Original/Released_Date");
	}

	/**
	 * Time/date/year that the recording began.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getRecordedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Recorded_Date");
	}

	/**
	 * Time/date/year that the encoding of this content was completed.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Date");
	}

	/**
	 * Time/date/year that the tags were added to this content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Tagged_Date");
	}

	/**
	 * Time/date/year that the composition of the music/script began.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getWrittenDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Written_Date");
	}

	/**
	 * Time/date/year that the content was digitally mastered.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteredDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Mastered_Date");
	}

	/**
	 * Time that the file was created on the file system.
	 * Shown in Info_Capacities()
	 */
	public static final String getFileCreatedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "File_Created_Date");
	}

	/**
	 * Local time that the file was created on the file system (not to be used in an international database).
	 * Shown in Info_Capacities()
	 */
	public static final String getFileCreatedDateLocal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "File_Created_Date_Local");
	}

	/**
	 * Time that the file was last modified on the file system.
	 */
	public static final String getFileModifiedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "File_Modified_Date");
	}

	/**
	 * Local time that the file was last modified on the file system (not to be used in an international database).
	 */
	public static final String getFileModifiedDateLocal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "File_Modified_Date_Local");
	}

	/**
	 * Location where track was recorded, as Longitude+Latitude.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getRecordedLocation(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Recorded_Location");
	}

	/**
	 * Location that the item was originally designed/written.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getWrittenLocation(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Written_Location");
	}

	/**
	 * Location where an item is archived (e.g. Louvre, Paris, France).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getArchivalLocation(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Archival_Location");
	}

	/**
	 * Name of the software package used to create the file (e.g. Microsoft WaveEdiTY).
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplication(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application");
	}

	/**
	 * Name of the software package used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedApplicationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application/String");
	}

	/**
	 * Name of the company of the encoding application.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application_CompanyName");
	}

	/**
	 * Name of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application_Name");
	}

	/**
	 * Version of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application_Version");
	}

	/**
	 * URL associated with the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Application_Url");
	}

	/**
	 * Software used to create the file.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library");
	}

	/**
	 * Software used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedLibraryString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library/String");
	}

	/**
	 * Name of the encoding software company.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library_CompanyName");
	}

	/**
	 * Name of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library_Name");
	}

	/**
	 * Version of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library_Version");
	}

	/**
	 * Release date of the encoding software, in UTC.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library_Date");
	}

	/**
	 * Parameters used by the encoding software.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrarySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_Library_Settings");
	}

	/**
	 * Operating System of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedOperatingSystem(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Encoded_OperatingSystem");
	}

	/**
	 * Describes whether an image has been cropped and, if so, how it was cropped.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCropped(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cropped");
	}

	/**
	 * Specifies the size of the original subject of the file (e.g. 8.5 in h, 11 in w).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDimensions(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Dimensions");
	}

	/**
	 * Stores dots per inch setting of the digitization mechanism used to produce the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDotsPerInch(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "DotsPerInch");
	}

	/**
	 * Describes the changes in lightness settings on the digitization mechanism made during the production of the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLightness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Lightness");
	}

	/**
	 * Original medium of the material (e.g. vinyl, Audio-CD, Super8 or BetaMax).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMedium(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceMedium");
	}

	/**
	 * Original form of the material (e.g. slide, paper, map).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceForm(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm");
	}

	/**
	 * Number of colors requested when digitizing (e.g. 256 for images or 32 bit RGB for video).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceFormNumColors(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm/NumColors");
	}

	/**
	 * Name of the product the file was originally intended for.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceFormName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm/Name");
	}

	/**
	 * Describes whether the original image has been cropped and, if so, how it was cropped (e.g. 16:9 to 4:3, top and bottom).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceFormCropped(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm/Cropped");
	}

	/**
	 * Identifies changes in sharpness the digitization mechanism made during the production of the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceFormSharpness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalSourceForm/Sharpness");
	}

	/**
	 * Software used to tag the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedApplication(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Tagged_Application");
	}

	/**
	 * Average number of beats per minute.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getBPM(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "BPM");
	}

	/**
	 * International Standard Recording Code, excluding the ISRC prefix and including hyphens.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getISRC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ISRC");
	}

	/**
	 * International Standard Book Number.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getISBN(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ISBN");
	}

	/**
	 * International Standard Audiovisual Number.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getISAN(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ISAN");
	}

	/**
	 * EAN-13 (13-digit European Article Numbering) or UPC-A (12-digit Universal Product Code) bar code identifier.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getBarCode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "BarCode");
	}

	/**
	 * Library of Congress Control Number.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLCCN(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "LCCN");
	}

	/**
	 * Universal Media Identifier.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getUMID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "UMID");
	}

	/**
	 * A label-specific catalogue number used to identify the release (e.g. TIC 01).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCatalogNumber(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "CatalogNumber");
	}

	/**
	 * Label code (e.g. 12345, meaning LC-12345).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLabelCode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "LabelCode");
	}

	/**
	 * Owner of the file.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOwner(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Owner");
	}

	/**
	 * Copyright attribution.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCopyright(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Copyright");
	}

	/**
	 * Link to a site with copyright/legal information.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCopyrightUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Copyright/Url");
	}

	/**
	 * Copyright information as per the production copyright holder.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getProducerCopyright(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Producer_Copyright");
	}

	/**
	 * License information (e.g. All Rights Reserved, Any Use Permitted).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTermsOfUse(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "TermsOfUse");
	}

	/**
	 * Name of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ServiceName");
	}

	/**
	 * Channel of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceChannel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ServiceChannel");
	}

	/**
	 * URL of of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Service/Url");
	}

	/**
	 * Provider of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceProvider(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ServiceProvider");
	}

	/**
	 * URL of provider of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceProviderUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ServiceProvider/Url");
	}

	/**
	 * Type of assisted service.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "ServiceType");
	}

	/**
	 * Television network name.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getNetworkName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "NetworkName");
	}

	/**
	 * Television network name of original broadcast.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalNetworkName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "OriginalNetworkName");
	}

	/**
	 * Country information of the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCountry(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Country");
	}

	/**
	 * Time zone information of the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeZone(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "TimeZone");
	}

	/**
	 * Is there a cover? Result will be "Yes" if present, empty if not.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCover(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cover");
	}

	/**
	 * Short description of cover image file (e.g. Earth in space).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCoverDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cover_Description");
	}

	/**
	 * Cover type (e.g. "Cover (front)").
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCoverType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cover_Type");
	}

	/**
	 * MIME type of cover file (e.g. image/png).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCoverMime(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cover_Mime");
	}

	/**
	 * Cover, in binary format, encoded as Base64.
	 * Shown in Info_Capacities()
	 */
	public static final String getCoverData(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Cover_Data");
	}

	/**
	 * Text of a song.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLyrics(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Lyrics");
	}

	/**
	 * Any comment related to the content.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getComment(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Comment");
	}

	/**
	 * A numeric value defining how much a person likes the song/movie, 1 to 5 (e.g. 2, 5.0).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getRating(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Rating");
	}

	/**
	 * Date/year the item was added to the owners collection.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getAddedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Added_Date");
	}

	/**
	 * Date the owner first played an item.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPlayedFirstDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Played_First_Date");
	}

	/**
	 * Date the owner last played an item.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getPlayedLastDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.GENERAL, streamNumber, "Played_Last_Date");
	}

	/**
	 * Number of times an item was played.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Long getPlayedCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "Played_Count");
	}

	/**
	 * Beginning position for Electronic Program Guide.
	 * Shown in Info_Capacities()
	 */
	public static final Long getEPGPositionsBegin(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "EPG_Positions_Begin");
	}

	/**
	 * Ending position for Electronic Program Guide.
	 * Shown in Info_Capacities()
	 */
	public static final Long getEPGPositionsEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.GENERAL, streamNumber, "EPG_Positions_End");
	}

}
