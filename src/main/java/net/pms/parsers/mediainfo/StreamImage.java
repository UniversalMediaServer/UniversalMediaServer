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

public class StreamImage {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamImage() {
	}

	/**
	 * Count of objects available in this stream created by MediaInfo when analyzing file.
	 * This is mostly for internal use
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Count");
	}

	/**
	 * Status of bit field when parsing.
	 * Options are: 0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished
	 * This is mostly for internal use
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Status");
	}

	/**
	 * Total number of streams available for this StreamKind.
	 * Counting starts at 1
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamCount");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamKind");
	}

	/**
	 * Name of stream type.
	 * Options are: Audio, General, Image, Menu, Other, Text, or Video
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamKind/String");
	}

	/**
	 * Identification number for stream, assigned in order of parsing.
	 * Counting starts at 0
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamKindID");
	}

	/**
	 * Identification number for stream when multiple, assigned in order of parsing.
	 * Counting starts at 1
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file for type of stream.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet parsed in the file for stream type.
	 * Counting starts at 0
	 * Shown in Info_Capacities()
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 * This is mostly for internal use
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Inform");
	}

	/**
	 * The identification number for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ID");
	}

	/**
	 * The identification number for this stream in this file (String format).
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ID/String");
	}

	/**
	 * Identification for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * Identification for this stream in the original medium of the material (String format).
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "MenuID/String");
	}

	/**
	 * Title of track.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Title");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format");
	}

	/**
	 * Format used and any additional features or settings.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format/String");
	}

	/**
	 * More details about the identified Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format/Info");
	}

	/**
	 * Link to a description of this format.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for these settings or Format field if there is no difference.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for these settings, if available.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version for the identified format.
	 * Shown in inform()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format (old XML: 'Profile@Level' format).
	 * Shown in inform()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Profile");
	}

	/**
	 * Order of bytes required for decoding.
	 * Options are Big/Little
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsEndianness(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Settings_Endianness");
	}

	/**
	 * Data packing method used in DPX frames (e.g. Packed, Filled A, Filled B).
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsPacking(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Settings_Packing");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Compression");
	}

	/**
	 * Settings used and required by decoder.
	 * Shown in inform()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Settings");
	}

	/**
	 * Wrapping mode set for format (e.g. Frame, Clip).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatSettingsWrapping(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_Settings_Wrapping");
	}

	/**
	 * Format features needed for fully supporting the content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * Internet Media Type (aka MIME Type, Content-Type).
	 * Shown in Info_Capacities()
	 */
	public static final String getInternetMediaType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "InternetMediaType");
	}

	/**
	 * Codec identifier as indicated by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID");
	}

	/**
	 * Codec identifier, as indicated by the container.
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID/String");
	}

	/**
	 * More information about this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID/Info");
	}

	/**
	 * Common alternative names for this codec identifier.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec identifier.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID/Url");
	}

	/**
	 * Codec description, as defined by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "CodecID_Description");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodec(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Codec");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Codec/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecFamily(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Codec/Family");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Codec/Info");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getCodecUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Codec/Url");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, as integer (e.g. 1920).
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Width");
	}

	/**
	 * Width of frame (trimmed to "clean aperture" size if present) in pixels, presented in SI unit digit spacing style, with measurement (e.g. 1 920 pixels).
	 * Shown in inform()
	 */
	public static final String getWidthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Width/String");
	}

	/**
	 * Offset between original width and displayed width, in pixels.
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidthOffset(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Width_Offset");
	}

	/**
	 * Offset between original width and displayed width, in pixels.
	 */
	public static final String getWidthOffsetString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Width_Offset/String");
	}

	/**
	 * Width of frame (not including aperture size if present) in pixels, presented as integer (e.g. 1920).
	 * Shown in Info_Capacities()
	 */
	public static final Long getWidthOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Width_Original");
	}

	/**
	 * Width of frame (not including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 920 pixels).
	 * Shown in inform()
	 */
	public static final String getWidthOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Width_Original/String");
	}

	/**
	 * Height of frame (including aperture size if present) in pixels, presented as integer (e.g. 1080).
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeight(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Height");
	}

	/**
	 * Height of frame (including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 080 pixels).
	 * Shown in inform()
	 */
	public static final String getHeightString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Height/String");
	}

	/**
	 * Offset between original height and displayed height, in pixels.
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeightOffset(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Height_Offset");
	}

	/**
	 * Offset between original height and displayed height, in pixels.
	 */
	public static final String getHeightOffsetString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Height_Offset/String");
	}

	/**
	 * Height of frame (not including aperture size if present) in pixels, presented as integer (e.g. 1080).
	 * Shown in Info_Capacities()
	 */
	public static final Long getHeightOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Height_Original");
	}

	/**
	 * Height of frame (not including aperture size if present) in pixels, present in SI unit digit spacing style, with measurement (e.g. 1 080 pixels).
	 * Shown in inform()
	 */
	public static final String getHeightOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Height_Original/String");
	}

	/**
	 * Pixel Aspect ratio.
	 * Shown in Info_Capacities()
	 */
	public static final Double getPixelAspectRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.IMAGE, streamNumber, "PixelAspectRatio");
	}

	/**
	 * Pixel Aspect ratio.
	 */
	public static final String getPixelAspectRatioString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "PixelAspectRatio/String");
	}

	/**
	 * Original (in the raw stream) Pixel Aspect ratio.
	 * Shown in Info_Capacities()
	 */
	public static final Double getPixelAspectRatioOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.IMAGE, streamNumber, "PixelAspectRatio_Original");
	}

	/**
	 * Original (in the raw stream) Pixel Aspect ratio.
	 */
	public static final String getPixelAspectRatioOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "PixelAspectRatio_Original/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.IMAGE, streamNumber, "DisplayAspectRatio");
	}

	/**
	 * The proportional relationship between the width and height of a frame (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "DisplayAspectRatio/String");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering data only from the codec (e.g. 4:3).
	 * Shown in Info_Capacities()
	 */
	public static final Double getDisplayAspectRatioOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.IMAGE, streamNumber, "DisplayAspectRatio_Original");
	}

	/**
	 * The proportional relationship between the width and height of a frame, considering data only from the codec (e.g. 4:3).
	 * Shown in inform()
	 */
	public static final String getDisplayAspectRatioOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "DisplayAspectRatio_Original/String");
	}

	/**
	 * Color profile of the image (e.g. YUV).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getColorSpace(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ColorSpace");
	}

	/**
	 * Ratio of chroma to luma in encoded image (e.g. 4:2:2).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getChromaSubsampling(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ChromaSubsampling");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getResolution(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "Resolution");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getResolutionString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Resolution/String");
	}

	/**
	 * Color information stored in the frame, as integer (e.g. 10).
	 * Shown in Info_Capacities()
	 */
	public static final Long getBitDepth(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "BitDepth");
	}

	/**
	 * Color information stored in the frame, as string (e.g. 10 bits).
	 * Shown in inform()
	 */
	public static final String getBitDepthString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "BitDepth/String");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in Info_Capacities()
	 */
	public static final String getCompressionMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Compression_Mode");
	}

	/**
	 * Compression mode (Lossy or Lossless).
	 * Shown in inform()
	 */
	public static final String getCompressionModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Compression_Mode/String");
	}

	/**
	 * Stream size divided by uncompressed stream size.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final Double getCompressionRatio(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.IMAGE, streamNumber, "Compression_Ratio");
	}

	/**
	 * Size of this stream, in bytes (e.g. 11010717).
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamSize");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 * Shown in inform()
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String1");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String2");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String3");
	}

	/**
	 * Size of this stream, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String4");
	}

	/**
	 * Size of this stream, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize/String5");
	}

	/**
	 * Size of this stream divided by total file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * Size of this stream after demuxing, in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) rounded to the nearest integer (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the two most significant digits (e.g. 11 MiB).
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the three most significant digits (e.g. 10.5 MiB).
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * Size of this stream after demuxing, measurement (measured in powers of 1024) rounded to the four most significant digits (e.g. 10.50 MiB).
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * Size of this stream after demuxing, with measurement (measured in powers of 1024) and percentage value (e.g. 10.5 MiB (98%)).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Software used to create the file.
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library");
	}

	/**
	 * Software used to create the file, in the format "CompanyName ProductName (OperatingSystem) Version (Date)".
	 * Shown in inform()
	 */
	public static final String getEncodedLibraryString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library/String");
	}

	/**
	 * Name of the encoding software.
	 */
	public static final String getEncodedLibraryName(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library_Name");
	}

	/**
	 * Version of the encoding software.
	 */
	public static final String getEncodedLibraryVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library_Version");
	}

	/**
	 * Release date of the encoding software, in UTC.
	 */
	public static final String getEncodedLibraryDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library_Date");
	}

	/**
	 * Parameters used by the encoding software.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedLibrarySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Library_Settings");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available (e.g. en, en-US, en-CN).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language");
	}

	/**
	 * Language, as full name (e.g. English).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language/String");
	}

	/**
	 * Language, as full name (e.g. English).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language/String1");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists (e.g. en).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language/String2");
	}

	/**
	 * Language, formatted as 3-letter ISO 639-2, if exists (e.g. eng).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language/String3");
	}

	/**
	 * Language, formatted as 2-letter ISO 639-1, if exists, with optional ISO 3166-1 country separated by a dash if available (e.g. en-US).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language/String4");
	}

	/**
	 * More information about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Language_More");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ServiceKind");
	}

	/**
	 * Type of assisted service (e.g. visually impaired, commentary, voice over).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "ServiceKind/String");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Disabled");
	}

	/**
	 * Set if this stream should not be used.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Disabled/String");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Default");
	}

	/**
	 * Flag set if this stream should be used if no language found matches the user preference.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Default/String");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Forced");
	}

	/**
	 * Flag set if this stream should be used regardless of user preferences, often used for sparse subtitle dialogue in an otherwise unsubtitled movie.
	 * Options are Yes/No
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "AlternateGroup/String");
	}

	/**
	 * Plot outline or a summary of the story.
	 */
	public static final String getSummary(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Summary");
	}

	/**
	 * Time that the encoding of this item was completed, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncodedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encoded_Date");
	}

	/**
	 * Time that the tags were added to this item, in UTC.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTaggedDate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Tagged_Date");
	}

	/**
	 * Whether this stream is encrypted and, if available, how it is encrypted.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getEncryption(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "Encryption");
	}

	/**
	 * Presence of color description.
	 * Options are Yes/No
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresent(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "colour_description_present");
	}

	/**
	 * Chromaticity coordinates of the source primaries.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimaries(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "colour_primaries");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristics(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "transfer_characteristics");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficients(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "matrix_coefficients");
	}

	/**
	 * Presence of colour description (if incoherencies).
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourdescriptionpresentOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "colour_description_present_Original");
	}

	/**
	 * Chromaticity coordinates of the source primaries (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getcolourprimariesOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "colour_primaries_Original");
	}

	/**
	 * Opto-electronic transfer characteristic of the source picture (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String gettransfercharacteristicsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "transfer_characteristics_Original");
	}

	/**
	 * Matrix coefficients used in deriving luma and chroma signals from the green, blue, and red primaries (if incoherencies).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getmatrixcoefficientsOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.IMAGE, streamNumber, "matrix_coefficients_Original");
	}

}
