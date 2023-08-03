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

public class StreamVideo {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamVideo() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MenuID/String");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format");
	}

	/**
	 * Format used and any additional features or settings.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings, if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format (old XML: 'Profile@Level' format).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Profile");
	}

	/**
	 * Level of the Format (MIXML only).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatLevel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Level");
	}

	/**
	 * Tier of the Format (MIXML only).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatTier(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Tier");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Compression");
	}

	/**
	 * Features from the format that are required to fully support the file content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Profile of the base stream for Multiview Video Coding.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMultiViewBaseProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MultiView_BaseProfile");
	}

	/**
	 * View count for Multiview Video Coding.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMultiViewCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MultiView_Count");
	}

	/**
	 * How views are muxed in the container (when not muxing in the stream) for Multiview Video Coding.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMultiViewLayout(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MultiView_Layout");
	}

	/**
	 * High Dynamic Range Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format");
	}

	/**
	 * HDR Format used, along with version, profile, level, layers, settings, and compatibility information.
	 * Shown in inform()
	 */
	public static final String getHDRFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format/String");
	}

	/**
	 * Commercial name used by vendor for these HDR settings or HDR Format field if there is no difference.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Commercial");
	}

	/**
	 * Version of HDR Format.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Version");
	}

	/**
	 * Profile of HDR Format.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Profile");
	}

	/**
	 * Level of HDR Format.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatLevel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Level");
	}

	/**
	 * HDR Format settings.
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Settings");
	}

	/**
	 * HDR Format compatibility with commercial products (e.g. HDR10).
	 * Shown in Info_Capacities()
	 */
	public static final String getHDRFormatCompatibility(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "HDR_Format_Compatibility");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings");
	}

	/**
	 * Whether BVOP (Bidirectional Interpolated Video Object Plane) settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsBVOP(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_BVOP");
	}

	/**
	 * Whether BVOP (Bidirectional Interpolated Video Object Plane) settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsBVOPString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_BVOP/String");
	}

	/**
	 * Whether Quarter-pixel motion settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsQPel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_QPel");
	}

	/**
	 * Whether Quarter-pixel motion settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsQPelString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_QPel/String");
	}

	/**
	 * Whether Global Motion Compensation settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final Long getFormatSettingsGMC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Format_Settings_GMC");
	}

	/**
	 * Whether Global Motion Compensation settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsGMCString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_GMC/String");
	}

	/**
	 * Whether Matrix settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsMatrix(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Matrix");
	}

	/**
	 * Whether Matrix settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsMatrixString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Matrix/String");
	}

	/**
	 * Matrix data, in base64-encoded binary format.
	 * Order: intra, non-intra, gray intra, gray non-intra
	 */
	public static final String getFormatSettingsMatrixData(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Matrix_Data");
	}

	/**
	 * Whether Context-adaptive binary arithmetic coding support is required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsCABAC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_CABAC");
	}

	/**
	 * Whether Context-adaptive binary arithmetic coding settings are required for decoding MPEG.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsCABACString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_CABAC/String");
	}

	/**
	 * Whether reference frames settings are required for decoding AVC.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final Long getFormatSettingsRefFrames(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Format_Settings_RefFrames");
	}

	/**
	 * Whether reference frames settings are required for decoding AVC.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getFormatSettingsRefFramesString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_RefFrames/String");
	}

	/**
	 * Pulldown method (for film transferred to video).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsPulldown(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Pulldown");
	}

	/**
	 * Order of bytes required for decoding.
	 * Options are Big/Little
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsEndianness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Endianness");
	}

	/**
	 * Data packing method used in DPX frames (e.g. Packed, Filled A, Filled B).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsPacking(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Packing");
	}

	/**
	 * Frame mode for decoding AVC.
	 * Options are "Frame doubling" or "Frame tripling"
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsFrameMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_FrameMode");
	}

	/**
	 * GOP method set for format (e.g. N=1, Variable).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsGOP(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_GOP");
	}

	/**
	 * Picture structure method set for format (e.g. Frame, Field).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsPictureStructure(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_PictureStructure");
	}

	/**
	 * Wrapping mode set for format (e.g. Frame, Clip).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsWrapping(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Format_Settings_Wrapping");
	}

	/**
	 * Internet Media Type (aka MIME Type, Content-Type).
	 * Shown in Info_Capacities()
	 */
	public static final String getInternetMediaType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "InternetMediaType");
	}

	/**
	 * How this file is muxed in the container (e.g. Muxed in Video #1).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMuxingMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MuxingMode");
	}

	/**
	 * Codec identifier as indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID");
	}

	/**
	 * Codec identifier, as indicated by the container.
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec identifier.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description, as defined by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "CodecID_Description");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecFamily(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec/Family");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec/Url");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecCC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec/CC");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Profile");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Description");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsPacketBitStream(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_PacketBitStream");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsBVOP(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_BVOP");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsQPel(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_QPel");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsGMC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_GMC");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsGMCString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_GMC/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsMatrix(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_Matrix");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsMatrixData(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_Matrix_Data");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsCABAC(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_CABAC");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecSettingsRefFrames(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Codec_Settings_RefFrames");
	}

	/**
	 * Play time, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Duration");
	}

	/**
	 * Play time in format XXx YYy, with YYy value omitted if zero (e.g. 1 h 40 min).
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String");
	}

	/**
	 * Play time in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String1");
	}

	/**
	 * Play time in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String2");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (e.g. 01:40:00.000).
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String3");
	}

	/**
	 * Play time in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String4");
	}

	/**
	 * Play time in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration/String5");
	}

	/**
	 * Duration of the first frame (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame (if different than other frames), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Duration_LastFrame");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm.
	 */
	public static final String getDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame (if different than other frames), in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Duration_LastFrame/String5");
	}

	/**
	 * Duration of the file, according to media header data, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Source_Duration");
	}

	/**
	 * Duration of the file, according to media header data, in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String");
	}

	/**
	 * Duration of the file, according to media header data, in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String1");
	}

	/**
	 * Duration of the file, according to media header data, in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String2");
	}

	/**
	 * Duration of the file, according to media header data, in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String3");
	}

	/**
	 * Duration of the file, according to media header data, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String4");
	}

	/**
	 * Duration of the file, according to media header data, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration/String5");
	}

	/**
	 * Duration of the first frame, according to media header data, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame");
	}

	/**
	 * Duration of the first frame, according to media header data, in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String");
	}

	/**
	 * Duration of the first frame, according to media header data, in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String1");
	}

	/**
	 * Duration of the first frame, according to media header data, in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String2");
	}

	/**
	 * Duration of the first frame, according to media header data, in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String3");
	}

	/**
	 * Duration of the first frame, according to media header data, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String4");
	}

	/**
	 * Duration of the first frame, according to media header data, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_FirstFrame/String5");
	}

	/**
	 * Duration of the last frame, according to media header data, in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame");
	}

	/**
	 * Duration of the last frame, according to media header data, in format XXx YYy, with YYy value omitted if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String");
	}

	/**
	 * Duration of the last frame, according to media header data, in format HHh MMmn SSs MMMms, with any fields omitted if zero.
	 */
	public static final String getSourceDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String1");
	}

	/**
	 * Duration of the last frame, according to media header data, in format XXx YYy, with YYy omitted if value is zero.
	 */
	public static final String getSourceDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String2");
	}

	/**
	 * Duration of the last frame, according to media header data, in format HH:MM:SS.mmm.
	 */
	public static final String getSourceDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String3");
	}

	/**
	 * Duration of the last frame, according to media header data, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String4");
	}

	/**
	 * Duration of the last frame, according to media header data, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_Duration_LastFrame/String5");
	}

	/**
	 * Bit rate mode of this stream, as acronym (VBR, CBR).
	 * Shown in Info_Capacities()
	 */
	public static final String getBitRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Mode");
	}

	/**
	 * Bit rate mode of this stream, as word (Variable, Constant).
	 * Shown in inform()
	 */
	public static final String getBitRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Mode/String");
	}

	/**
	 * Bit rate of this stream, in bits per second (e.g. 128026).
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "BitRate");
	}

	/**
	 * Bit rate of this stream, with measurement (e.g. 128 kb/s).
	 * Shown in inform()
	 */
	public static final String getBitRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate/String");
	}

	/**
	 * Minimum bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "BitRate_Minimum");
	}

	/**
	 * Minimum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Minimum/String");
	}

	/**
	 * Nominal bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "BitRate_Nominal");
	}

	/**
	 * Nominal bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Nominal/String");
	}

	/**
	 * Maximum bit rate of this stream, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "BitRate_Maximum");
	}

	/**
	 * Maximum bit rate of this stream, with measurement.
	 * Shown in inform()
	 */
	public static final String getBitRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Maximum/String");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bits per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "BitRate_Encoded");
	}

	/**
	 * Encoded bit rate (with forced padding), if container padding is present, in bits per second.
	 */
	public static final String getBitRateEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitRate_Encoded/String");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, as integer (e.g. 1920).
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Width");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, presented in SI unit digit spacing style, with measurement (e.g. 1 920 pixels).
	 * Shown in inform()
	 */
	public static final String getWidthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Width/String");
	}

	/**
	 * Offset between original width and displayed width, in pixels.
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidthOffset(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Width_Offset");
	}

	/**
	 * Offset between original width and displayed width, in pixels.
	 */
	public static final String getWidthOffsetString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Width_Offset/String");
	}

	/**
	 * Width of frame (not including aperture size if present) in pixels, presented as integer (e.g. 1920).
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidthOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Width_Original");
	}

	/**
	 * Width of frame (not including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 920 pixels).
	 * Shown in inform()
	 */
	public static final String getWidthOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Width_Original/String");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, presented as integer (e.g. 1920).
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidthCleanAperture(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Width_CleanAperture");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 920 pixels).
	 * Shown in inform()
	 */
	public static final String getWidthCleanApertureString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Width_CleanAperture/String");
	}

	/**
	 * Height of frame (including aperture size if present) in pixels, presented as integer (e.g. 1080).
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeight(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Height");
	}

	/**
	 * Height of frame (including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 080 pixels).
	 * Shown in inform()
	 */
	public static final String getHeightString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Height/String");
	}

	/**
	 * Offset between original height and displayed height, in pixels.
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeightOffset(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Height_Offset");
	}

	/**
	 * Offset between original height and displayed height, in pixels.
	 */
	public static final String getHeightOffsetString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Height_Offset/String");
	}

	/**
	 * Height of frame (not including aperture size if present) in pixels, presented as integer (e.g. 1080).
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeightOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Height_Original");
	}

	/**
	 * Height of frame (not including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 080 pixels).
	 * Shown in inform()
	 */
	public static final String getHeightOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Height_Original/String");
	}

	/**
	 * Height of frame (trimmed to "clean aperture" size if present) in pixels, presented as integer (e.g. 1080).
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeightCleanAperture(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Height_CleanAperture");
	}

	/**
	 * Height of frame (trimmed to "clean aperture" size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 800 pixels).
	 * Shown in inform()
	 */
	public static final String getHeightCleanApertureString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Height_CleanAperture/String");
	}

	/**
	 * Width of frame, considering data stored in the codec.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStoredWidth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Stored_Width");
	}

	/**
	 * Height of frame, considering data stored in the codec.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStoredHeight(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Stored_Height");
	}

	/**
	 * Width of frame, from data derived from video stream.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSampledWidth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Sampled_Width");
	}

	/**
	 * Height of frame, from data derived from video stream.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSampledHeight(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Sampled_Height");
	}

	/**
	 * Width of a pixel as compared to the height, considering data from both the container and codec (e.g. 1.422).
	 * Shown in Info_Capacities()
	 */
	public static final Double getPixelAspectRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "PixelAspectRatio");
	}

	/**
	 * Width of a pixel as compared to the height, considering data from both the container and codec (e.g. 1.422).
	 */
	public static final String getPixelAspectRatioString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "PixelAspectRatio/String");
	}

	/**
	 * Width of a pixel as compared to the height, considering data only from the codec (e.g. 1.422).
	 * This field is only shown if the container and codec values are different
	 * Shown in Info_Capacities()
	 */
	public static final Double getPixelAspectRatioOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "PixelAspectRatio_Original");
	}

	/**
	 * Width of a pixel as compared to the height, considering data only from the codec (e.g. 1.422).
	 * This field is only shown if the container and codec values are different
	 */
	public static final String getPixelAspectRatioOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "PixelAspectRatio_Original/String");
	}

	/**
	 * Width of a pixel as compared to the height, considering "clean aperture" dimensions (e.g. 1.422).
	 * This field is only shown if the values are different
	 * Shown in Info_Capacities()
	 */
	public static final Double getPixelAspectRatioCleanAperture(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "PixelAspectRatio_CleanAperture");
	}

	/**
	 * Width of a pixel as compared to the height, considering "clean aperture" dimensions (e.g. 1.422).
	 * This field is only shown if the values are different
	 */
	public static final String getPixelAspectRatioCleanApertureString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "PixelAspectRatio_CleanAperture/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering data only from the codec (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatioOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio_Original");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering data only from the codec (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio_Original/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering "clean aperture" dimensions (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatioCleanAperture(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio_CleanAperture");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering "clean aperture" dimensions (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioCleanApertureString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "DisplayAspectRatio_CleanAperture/String");
	}

	/**
	 * Active Format Description, as value code (e.g. 001).
	 * Shown in Info_Capacities()
	 */
	public static final String getActiveFormatDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ActiveFormatDescription");
	}

	/**
	 * Active Format Description, as text (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getActiveFormatDescriptionString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ActiveFormatDescription/String");
	}

	/**
	 * Muxing mode used for Active Format Description (AFD value).
	 * Options are A/53 (for Raw) or SMPTE ST 2016-3 (for Ancillary)
	 * Shown in Info_Capacities()
	 */
	public static final String getActiveFormatDescriptionMuxingMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ActiveFormatDescription_MuxingMode");
	}

	/**
	 * Rotation of video, derived from track header data, in degrees.
	 * Shown in Info_Capacities()
	 */
	public static final String getRotation(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Rotation");
	}

	/**
	 * Rotation of video, derived from track header data, in degrees.
	 * Shown in inform()
	 */
	public static final String getRotationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Rotation/String");
	}

	/**
	 * Frame rate mode, considering data from both the container and codec, as acronym (e.g. CFR, VFR).
	 * Shown in Info_Capacities()
	 */
	public static final String getFrameRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Mode");
	}

	/**
	 * Frame rate mode, considering data from both the container and codec, as word (e.g. Constant, Variable).
	 * Shown in inform()
	 */
	public static final String getFrameRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Mode/String");
	}

	/**
	 * Original frame rate mode, considering data only from the codec, as acronym (e.g. CFR, VFR).
	 * Shown in Info_Capacities()
	 */
	public static final String getFrameRateModeOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Mode_Original");
	}

	/**
	 * Original frame rate mode, considering data only from the codec, as word (Constant, Variable).
	 * Shown in inform()
	 */
	public static final String getFrameRateModeOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Mode_Original/String");
	}

	/**
	 * Frames per second, as integer (e.g. 29.970).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second, with measurement (e.g. 29.970 (29970/1000) FPS).
	 * Shown in inform()
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate/String");
	}

	/**
	 * Numerator for determined frames per second (e.g. 29970).
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "FrameRate_Num");
	}

	/**
	 * Denominator for determined frames per second (e.g. 1000).
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "FrameRate_Den");
	}

	/**
	 * Minimum frames per second (e.g. 25.000).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Minimum");
	}

	/**
	 * Minimum frames per second, with measurement (e.g. 25.000 FPS).
	 * Shown in inform()
	 */
	public static final String getFrameRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Minimum/String");
	}

	/**
	 * Frames per second rounded to closest standard (e.g. 24.98).
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Nominal");
	}

	/**
	 * Frames per second rounded to closest standard, with measurement (e.g. 29.97 fps).
	 * Shown in inform()
	 */
	public static final String getFrameRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Nominal/String");
	}

	/**
	 * Maximum frames per second, considering data from both the container and codec.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Maximum");
	}

	/**
	 * Maximum frames per second, considering data from both the container and codec, with measurement.
	 * Shown in inform()
	 */
	public static final String getFrameRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Maximum/String");
	}

	/**
	 * Frames per second, considering data only from the codec.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Original");
	}

	/**
	 * Frames per second, considering data only from the codec, with measurement.
	 * Shown in inform()
	 */
	public static final String getFrameRateOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Original/String");
	}

	/**
	 * Numerator for determined frames per second, considering data only from the codec (e.g. 29970).
	 */
	public static final Double getFrameRateOriginalNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Original_Num");
	}

	/**
	 * Denominator for determined frames per second, considering data only from the codec (e.g. 1000).
	 */
	public static final Double getFrameRateOriginalDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Original_Den");
	}

	/**
	 * Real (capture) frames per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRateReal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "FrameRate_Real");
	}

	/**
	 * Real (capture) frames per second (with measurement).
	 * Shown in inform()
	 */
	public static final String getFrameRateRealString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "FrameRate_Real/String");
	}

	/**
	 * Numer of frames (e.g. 78112).
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "FrameCount");
	}

	/**
	 * Number of frames according to media header (media/stts atom) data (e.g. 78112).
	 */
	public static final Long getSourceFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Source_FrameCount");
	}

	/**
	 * Either the NTSC or PAL color encoding system, determined by other video characteristics.
	 * Shown in inform()
	 */
	public static final String getStandard(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Standard");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getResolution(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Resolution");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getResolutionString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Resolution/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getColorimetry(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Colorimetry");
	}

	/**
	 * Color profile of the image (e.g. YUV).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getColorSpace(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ColorSpace");
	}

	/**
	 * Ratio of chroma to luma in encoded image (e.g. 4:2:2).
	 * Shown in Info_Capacities()
	 */
	public static final String getChromaSubsampling(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ChromaSubsampling");
	}

	/**
	 * Ratio of chroma to luma in encoded image (e.g. 4:2:2).
	 * Shown in inform()
	 */
	public static final String getChromaSubsamplingString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ChromaSubsampling/String");
	}

	/**
	 * Position type of chroma subsampling.
	 * Shown in Info_Capacities()
	 */
	public static final String getChromaSubsamplingPosition(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ChromaSubsampling_Position");
	}

	/**
	 * Color information stored in the video frames, as integer (e.g. 10).
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "BitDepth");
	}

	/**
	 * Color information stored in the video frames, as string (e.g. 10 bits).
	 * Shown in inform()
	 */
	public static final String getBitDepthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BitDepth/String");
	}

	/**
	 * Way in which lines of video are displayed, considering data from both the container and codec (e.g. Progressive).
	 * Shown in Info_Capacities()
	 */
	public static final String getScanType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType");
	}

	/**
	 * Way in which lines of video are displayed, considering data from both the container and codec (e.g. Progressive).
	 * Shown in inform()
	 */
	public static final String getScanTypeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType/String");
	}

	/**
	 * Way in which lines of video are encoded, considering data only from the codec (e.g. Progressive).
	 * Shown in Info_Capacities()
	 */
	public static final String getScanTypeOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType_Original");
	}

	/**
	 * Way in which lines of video are encoded, considering data only from the codec (e.g. Progressive).
	 * Shown in inform()
	 */
	public static final String getScanTypeOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType_Original/String");
	}

	/**
	 * Whether the video's ScanType is stored with fields separated or interleaved.
	 * Shown in Info_Capacities()
	 */
	public static final String getScanTypeStoreMethod(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType_StoreMethod");
	}

	/**
	 * Count of fields per container block.
	 * Shown in Info_Capacities()
	 */
	public static final String getScanTypeStoreMethodFieldsPerBlock(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType_StoreMethod_FieldsPerBlock");
	}

	/**
	 * Whether the video's ScanType is stored with fields separated or interleaved.
	 * Shown in inform()
	 */
	public static final String getScanTypeStoreMethodString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanType_StoreMethod/String");
	}

	/**
	 * Order in which lines are encoded, as acronym (e.g. TFF).
	 * Shown in Info_Capacities()
	 */
	public static final String getScanOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder");
	}

	/**
	 * Order in which lines are encoded, as acronym (e.g. TFF).
	 * Shown in inform()
	 */
	public static final String getScanOrderString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder/String");
	}

	/**
	 * Stored ScanOrder, displayed when the stored order is not same as the display order.
	 * Shown in Info_Capacities()
	 */
	public static final String getScanOrderStored(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder_Stored");
	}

	/**
	 * Stored ScanOrder, displayed when the stored order is not same as the display order.
	 * Shown in inform()
	 */
	public static final String getScanOrderStoredString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder_Stored/String");
	}

	/**
	 * Field is set to "Yes" when display and stored orders are inverted.
	 */
	public static final String getScanOrderStoredDisplayedInverted(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder_StoredDisplayedInverted");
	}

	/**
	 * Whether the video's ScanType is stored with fields separated or interleaved, considering data only from the codec.
	 * Shown in Info_Capacities()
	 */
	public static final String getScanOrderOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder_Original");
	}

	/**
	 * Whether the video's ScanType is stored with fields separated or interleaved, considering data only from the codec.
	 * Shown in inform()
	 */
	public static final String getScanOrderOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ScanOrder_Original/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getInterlacement(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Interlacement");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getInterlacementString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Interlacement/String");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in Info_Capacities()
	 */
	public static final String getCompressionMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Compression_Mode");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in inform()
	 */
	public static final String getCompressionModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Compression_Mode/String");
	}

	/**
	 * Stream size divided by uncompressed stream size.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Double getCompressionRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Compression_Ratio");
	}

	/**
	 * Bits divided by Pixel multiplied by Frame.
	 * Shown in inform()
	 */
	public static final Double getBitsPixelFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Bits-(Pixel*Frame)");
	}

	/**
	 * Delay fixed in the stream (relative), in milliseconds.
	 */
	public static final Double getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "Delay");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String1");
	}

	/**
	 * Delay fixed in the stream (relative), with measurement.
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String2");
	}

	/**
	 * Delay fixed in the stream (relative) in format HH:MM:SS.mmm, with measurement.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (i.e.
	 * in case of timecode)
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame information.
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Source");
	}

	/**
	 * Source location of the Delay (e.g. Container, Stream, empty).
	 */
	public static final String getDelaySourceString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Source/String");
	}

	/**
	 * Delay, considering data only from the codec, in milliseconds.
	 */
	public static final Long getDelayOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Delay_Original");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String1");
	}

	/**
	 * Delay, considering data only from the codec, with measurement.
	 */
	public static final String getDelayOriginalString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String2");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS.mmm.
	 */
	public static final String getDelayOriginalString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String3");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayOriginalString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String4");
	}

	/**
	 * Delay, considering data only from the codec, in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayOriginalString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original/String5");
	}

	/**
	 * Delay settings, considering data only from the codec (i.e.
	 * in case of timecode)
	 */
	public static final String getDelayOriginalSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original_Settings");
	}

	/**
	 * Delay drop frame information, considering data only from the codec.
	 */
	public static final String getDelayOriginalDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original_DropFrame");
	}

	/**
	 * Delay source (e.g. Container, Stream, empty).
	 */
	public static final String getDelayOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Delay_Original_Source");
	}

	/**
	 * Timestamp fixed in the stream (relative), in milliseconds.
	 * Shown in Info_Capacities()
	 */
	public static final Double getTimeStampFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame");
	}

	/**
	 * Timestamp fixed in the stream (relative), with measurement.
	 * Shown in inform()
	 */
	public static final String getTimeStampFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String");
	}

	/**
	 * Timestamp fixed in the stream (relative), with measurement.
	 */
	public static final String getTimeStampFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String1");
	}

	/**
	 * Timestamp fixed in the stream (relative), with measurement.
	 */
	public static final String getTimeStampFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String2");
	}

	/**
	 * Timestamp in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getTimeStampFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String3");
	}

	/**
	 * Timestamp in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getTimeStampFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String4");
	}

	/**
	 * Timestamp in format HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getTimeStampFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeStamp_FirstFrame/String5");
	}

	/**
	 * Time code for first frame in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeCode_FirstFrame");
	}

	/**
	 * Time code for last frame (excluding the duration of the last frame) in format HH:MM:SS:FF, with last colon replaced by semicolon for drop frame if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeCode_LastFrame");
	}

	/**
	 * Time code drop frame.
	 */
	public static final String getTimeCodeDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeCode_DropFrame");
	}

	/**
	 * Additional time code settings.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeCode_Settings");
	}

	/**
	 * Time code source (Container, Stream, SystemScheme1, SDTI, ANC, etc.).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "TimeCode_Source");
	}

	/**
	 * Time code information about Open/Closed GOP.
	 * Shown in Info_Capacities()
	 */
	public static final String getGopOpenClosed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Gop_OpenClosed");
	}

	/**
	 * Time code information about Open/Closed GOP.
	 * Shown in inform()
	 */
	public static final String getGopOpenClosedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Gop_OpenClosed/String");
	}

	/**
	 * Time code information about Open/Closed of first frame if GOP is Open for the other GOPs.
	 * Shown in Info_Capacities()
	 */
	public static final String getGopOpenClosedFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Gop_OpenClosed_FirstFrame");
	}

	/**
	 * Time code information about Open/Closed of first frame if GOP is Open for the other GOPs.
	 * Shown in inform()
	 */
	public static final String getGopOpenClosedFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Gop_OpenClosed_FirstFrame/String");
	}

	/**
	 * Size of this stream, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamSize");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String1");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String2");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String3");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String4");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * Size of this stream after demuxing, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Size of content stored in the file, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Source_StreamSize");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getSourceStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String1");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String2");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String3");
	}

	/**
	 * Size of content stored in the file, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String4");
	}

	/**
	 * Size of content stored in the file, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getSourceStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Proportion");
	}

	/**
	 * Size of this stream when encoded, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String1");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String2");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String3");
	}

	/**
	 * Size of this stream when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String4");
	}

	/**
	 * Size of this stream when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded/String5");
	}

	/**
	 * Encoded Stream size divided by file size.
	 */
	public static final String getStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "StreamSize_Encoded_Proportion");
	}

	/**
	 * Size of content stored in the file when encoded, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String1");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String2");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String3");
	}

	/**
	 * Size of content stored in the file when encoded, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getSourceStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String4");
	}

	/**
	 * Size of content stored in the file when encoded, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getSourceStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded/String5");
	}

	/**
	 * Source Encoded Stream size divided by file size.
	 */
	public static final String getSourceStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Source_StreamSize_Encoded_Proportion");
	}

	/**
	 * How this stream is aligned in the container (e.g. Aligned, Split).
	 * Shown in Info_Capacities()
	 */
	public static final String getAlignment(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Alignment");
	}

	/**
	 * How this stream is aligned in the container (e.g. Aligned, Split).
	 * Shown in inform()
	 */
	public static final String getAlignmentString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Alignment/String");
	}

	/**
	 * Title of track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Title");
	}

	/**
	 * Name of the software package used to create the file (e.g. Microsoft WaveEdiTY).
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplication(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application");
	}

	/**
	 * Name of the software package used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedApplicationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application/String");
	}

	/**
	 * Name of the company of the encoding application.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application_CompanyName");
	}

	/**
	 * Name of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application_Name");
	}

	/**
	 * Version of the encoding product.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application_Version");
	}

	/**
	 * URL associated with the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedApplicationUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Application_Url");
	}

	/**
	 * Software used to create the file.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library");
	}

	/**
	 * Software used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedLibraryString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library/String");
	}

	/**
	 * Name of the encoding software company.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibraryCompanyName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library_CompanyName");
	}

	/**
	 * Name of the encoding software.
	 */
	public static final String getEncodedLibraryName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library_Name");
	}

	/**
	 * Version of the encoding software.
	 */
	public static final String getEncodedLibraryVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library_Version");
	}

	/**
	 * Release date of the encoding software, in UTC.
	 */
	public static final String getEncodedLibraryDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library_Date");
	}

	/**
	 * Parameters used by the encoding software.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrarySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Library_Settings");
	}

	/**
	 * Operating System of the encoding software.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedOperatingSystem(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_OperatingSystem");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available (e.g. en, en-US, en-CN).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language");
	}

	/**
	 * Language, as full name (e.g. English).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language/String");
	}

	/**
	 * Language, as full name (e.g. English).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language/String1");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists (e.g. en).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language/String2");
	}

	/**
	 * Language, formatted as 3-letter ISO 639-2, if exists (e.g. eng).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language/String3");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists, with optional ISO 3166-1 country separated by a dash if available (e.g. en-US).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language/String4");
	}

	/**
	 * More information about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Language_More");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ServiceKind");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "ServiceKind/String");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Disabled");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Disabled/String");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Default");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Default/String");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Forced");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "AlternateGroup/String");
	}

	/**
	 * Time that the encoding of this item was completed, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encoded_Date");
	}

	/**
	 * Time that the tags were added to this item, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Tagged_Date");
	}

	/**
	 * Whether this stream is encrypted and, if available, how it is encrypted.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryption(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "Encryption");
	}

	/**
	 * The minimum size of the buffer needed to decode the sequence.
	 * Shown in Info_Capacities()
	 */
	public static final String getBufferSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "BufferSize");
	}

	/**
	 * Presence of color description.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresent(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_description_present");
	}

	/**
	 * Presence of colour description (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresentSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_description_present_Source");
	}

	/**
	 * Presence of colour description (if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresentOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_description_present_Original");
	}

	/**
	 * Presence of colour description (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresentOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_description_present_Original_Source");
	}

	/**
	 * Color range for YUV color space.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourrange(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_range");
	}

	/**
	 * Colour range for YUV colour space (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourrangeSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_range_Source");
	}

	/**
	 * Colour range for YUV colour space (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourrangeOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_range_Original");
	}

	/**
	 * Colour range for YUV colour space (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourrangeOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_range_Original_Source");
	}

	/**
	 * Chromaticity coordinates of the source primaries.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimaries(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_primaries");
	}

	/**
	 * Chromaticity coordinates of the source primaries (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimariesSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_primaries_Source");
	}

	/**
	 * Chromaticity coordinates of the source primaries (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimariesOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_primaries_Original");
	}

	/**
	 * Chromaticity coordinates of the source primaries (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimariesOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "colour_primaries_Original_Source");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristics(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "transfer_characteristics");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture (source).
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristicsSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "transfer_characteristics_Source");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristicsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "transfer_characteristics_Original");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristicsOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "transfer_characteristics_Original_Source");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficients(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "matrix_coefficients");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficientsSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "matrix_coefficients_Source");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficientsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "matrix_coefficients_Original");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficientsOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "matrix_coefficients_Original_Source");
	}

	/**
	 * Chromaticity coordinates of the source primaries of the mastering display.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayColorPrimaries(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_ColorPrimaries");
	}

	/**
	 * Chromaticity coordinates of the source primaries of the mastering display (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayColorPrimariesSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_ColorPrimaries_Source");
	}

	/**
	 * Chromaticity coordinates of the source primaries of the mastering display (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayColorPrimariesOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_ColorPrimaries_Original");
	}

	/**
	 * Chromaticity coordinates of the source primaries of the mastering display (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayColorPrimariesOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_ColorPrimaries_Original_Source");
	}

	/**
	 * Luminance of the mastering display.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayLuminance(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_Luminance");
	}

	/**
	 * Luminance of the mastering display (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayLuminanceSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_Luminance_Source");
	}

	/**
	 * Luminance of the mastering display (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayLuminanceOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_Luminance_Original");
	}

	/**
	 * Luminance of the mastering display (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getMasteringDisplayLuminanceOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MasteringDisplay_Luminance_Original_Source");
	}

	/**
	 * Maximum content light level.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxCLL(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxCLL");
	}

	/**
	 * Maximum content light level (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxCLLSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxCLL_Source");
	}

	/**
	 * Maximum content light level (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxCLLOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxCLL_Original");
	}

	/**
	 * Maximum content light level (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxCLLOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxCLL_Original_Source");
	}

	/**
	 * Maximum frame average light level.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxFALL(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxFALL");
	}

	/**
	 * Maximum frame average light level (source).
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxFALLSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxFALL_Source");
	}

	/**
	 * Maximum frame average light level (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxFALLOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxFALL_Original");
	}

	/**
	 * Maximum frame average light level (source if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getMaxFALLOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.VIDEO, streamNumber, "MaxFALL_Original_Source");
	}

}
