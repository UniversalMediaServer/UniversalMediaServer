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

public class StreamOther {

	/**
	 * This class is not meant to be instantiated.
	 */
	private StreamOther() {
	}

	/**
	 * Count of objects available in this stream.
	 */
	public static final Long getCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Count");
	}

	/**
	 * bit field (0=IsAccepted, 1=IsFilled, 2=IsUpdated, 3=IsFinished).
	 */
	public static final Long getStatus(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Status");
	}

	/**
	 * Count of streams of that kind available.
	 */
	public static final Long getStreamCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamCount");
	}

	/**
	 * Stream type name.
	 */
	public static final String getStreamKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamKind");
	}

	/**
	 * Stream type name.
	 */
	public static final String getStreamKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamKind/String");
	}

	/**
	 * Number of the stream (base=0).
	 */
	public static final Long getStreamKindID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamKindID");
	}

	/**
	 * When multiple streams, number of the stream (base=1).
	 */
	public static final Long getStreamKindPos(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamKindPos");
	}

	/**
	 * Stream order in the file, whatever is the kind of stream (base=0).
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamOrder");
	}

	/**
	 * Order of the first fully decodable packet met in the file, whatever is the kind of stream (base=0).
	 */
	public static final Long getFirstPacketOrder(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "FirstPacketOrder");
	}

	/**
	 * Last **Inform** call.
	 */
	public static final String getInform(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Inform");
	}

	/**
	 * The ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "ID");
	}

	/**
	 * The ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "ID/String");
	}

	/**
	 * The ID for this stream in the original medium of the material.
	 * Shown in Info_Capacities()
	 */
	public static final String getOriginalSourceMediumID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "OriginalSourceMedium_ID");
	}

	/**
	 * The ID for this stream in the original medium of the material.
	 * Shown in inform()
	 */
	public static final String getOriginalSourceMediumIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "OriginalSourceMedium_ID/String");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in Info_Capacities()
	 */
	public static final String getUniqueID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "UniqueID");
	}

	/**
	 * The unique ID for this stream, should be copied with stream copy.
	 * Shown in inform()
	 */
	public static final String getUniqueIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "UniqueID/String");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in Info_Capacities()
	 */
	public static final String getMenuID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "MenuID");
	}

	/**
	 * The menu ID for this stream in this file.
	 * Shown in inform()
	 */
	public static final String getMenuIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "MenuID/String");
	}

	/**
	 * Type.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getType(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Type");
	}

	/**
	 * Format used.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormat(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format");
	}

	/**
	 * Format used + additional features.
	 * Shown in inform()
	 */
	public static final String getFormatString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format/String");
	}

	/**
	 * Info about Format.
	 * Shown in inform()
	 */
	public static final String getFormatInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format/Info");
	}

	/**
	 * Link.
	 */
	public static final String getFormatUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format/Url");
	}

	/**
	 * Commercial name used by vendor for theses setings or Format field if there is no difference.
	 */
	public static final String getFormatCommercial(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Commercial");
	}

	/**
	 * Commercial name used by vendor for theses setings if there is one.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatCommercialIfAny(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Commercial_IfAny");
	}

	/**
	 * Version of this format.
	 * Shown in inform()
	 */
	public static final String getFormatVersion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Version");
	}

	/**
	 * Profile of the Format.
	 * Shown in inform()
	 */
	public static final String getFormatProfile(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Profile");
	}

	/**
	 * Compression method used.
	 * Shown in inform()
	 */
	public static final String getFormatCompression(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Compression");
	}

	/**
	 * Settings needed for decoder used.
	 * Shown in inform()
	 */
	public static final String getFormatSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_Settings");
	}

	/**
	 * Format features needed for fully supporting the content.
	 * Shown in Info_Capacities()
	 */
	public static final String getFormatAdditionalFeatures(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Format_AdditionalFeatures");
	}

	/**
	 * How this file is muxed in the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getMuxingMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "MuxingMode");
	}

	/**
	 * Codec ID (found in some containers).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecID(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID");
	}

	/**
	 * Codec ID (found in some containers).
	 * Shown in inform()
	 */
	public static final String getCodecIDString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID/String");
	}

	/**
	 * Info about this codec.
	 * Shown in inform()
	 */
	public static final String getCodecIDInfo(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID/Info");
	}

	/**
	 * A hint/popular name for this codec.
	 * Shown in inform()
	 */
	public static final String getCodecIDHint(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID/Hint");
	}

	/**
	 * A link to more details about this codec ID.
	 */
	public static final String getCodecIDUrl(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID/Url");
	}

	/**
	 * Manual description given by the container.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getCodecIDDescription(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "CodecID_Description");
	}

	/**
	 * Play time of the stream in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Duration");
	}

	/**
	 * Play time in format : XXx YYy only, YYy omited if zero.
	 * Shown in inform()
	 */
	public static final String getDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String");
	}

	/**
	 * Play time in format : HHh MMmn SSs MMMms, XX omited if zero.
	 */
	public static final String getDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String1");
	}

	/**
	 * Play time in format : XXx YYy only, YYy omited if zero.
	 */
	public static final String getDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String2");
	}

	/**
	 * Play time in format : HH:MM:SS.MMM.
	 */
	public static final String getDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String3");
	}

	/**
	 * Play time in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String4");
	}

	/**
	 * Play time in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration/String5");
	}

	/**
	 * .
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDurationStart(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration_Start");
	}

	/**
	 * .
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getDurationEnd(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Duration_End");
	}

	/**
	 * Source Play time of the stream, in ms.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDuration(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Source_Duration");
	}

	/**
	 * Source Play time in format : XXx YYy only, YYy omited if zero.
	 * Shown in inform()
	 */
	public static final String getSourceDurationString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String");
	}

	/**
	 * Source Play time in format : HHh MMmn SSs MMMms, XX omited if zero.
	 */
	public static final String getSourceDurationString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String1");
	}

	/**
	 * Source Play time in format : XXx YYy only, YYy omited if zero.
	 */
	public static final String getSourceDurationString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String2");
	}

	/**
	 * Source Play time in format : HH:MM:SS.MMM.
	 */
	public static final String getSourceDurationString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String3");
	}

	/**
	 * Play time in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String4");
	}

	/**
	 * Play time in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration/String5");
	}

	/**
	 * Source Duration of the first frame if it is longer than others, in ms;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame");
	}

	/**
	 * Source Duration of the first frame if it is longer than others, in format : XXx YYy only, YYy omited if zero;.
	 * Shown in inform()
	 */
	public static final String getSourceDurationFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String");
	}

	/**
	 * Source Duration of the first frame if it is longer than others, in format : HHh MMmn SSs MMMms, XX omited if zero;.
	 */
	public static final String getSourceDurationFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String1");
	}

	/**
	 * Source Duration of the first frame if it is longer than others, in format : XXx YYy only, YYy omited if zero;.
	 */
	public static final String getSourceDurationFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String2");
	}

	/**
	 * Source Duration of the first frame if it is longer than others, in format : HH:MM:SS.MMM;.
	 */
	public static final String getSourceDurationFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String3");
	}

	/**
	 * Play time in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String4");
	}

	/**
	 * Play time in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_FirstFrame/String5");
	}

	/**
	 * Source Duration of the last frame if it is longer than others, in ms;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getSourceDurationLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame");
	}

	/**
	 * Source Duration of the last frame if it is longer than others, in format : XXx YYy only, YYy omited if zero;.
	 * Shown in inform()
	 */
	public static final String getSourceDurationLastFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String");
	}

	/**
	 * Source Duration of the last frame if it is longer than others, in format : HHh MMmn SSs MMMms, XX omited if zero;.
	 */
	public static final String getSourceDurationLastFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String1");
	}

	/**
	 * Source Duration of the last frame if it is longer than others, in format : XXx YYy only, YYy omited if zero;.
	 */
	public static final String getSourceDurationLastFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String2");
	}

	/**
	 * Source Duration of the last frame if it is longer than others, in format : HH:MM:SS.MMM;.
	 */
	public static final String getSourceDurationLastFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String3");
	}

	/**
	 * Play time in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getSourceDurationLastFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String4");
	}

	/**
	 * Play time in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getSourceDurationLastFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_Duration_LastFrame/String5");
	}

	/**
	 * Bit rate mode (VBR, CBR);.
	 * Shown in Info_Capacities()
	 */
	public static final String getBitRateMode(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Mode");
	}

	/**
	 * Bit rate mode (Variable, Cconstant);.
	 * Shown in inform()
	 */
	public static final String getBitRateModeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Mode/String");
	}

	/**
	 * Bit rate in bps;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "BitRate");
	}

	/**
	 * Bit rate (with measurement);.
	 * Shown in inform()
	 */
	public static final String getBitRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate/String");
	}

	/**
	 * Minimum Bit rate in bps;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMinimum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "BitRate_Minimum");
	}

	/**
	 * Minimum Bit rate (with measurement).
	 * Shown in inform()
	 */
	public static final String getBitRateMinimumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Minimum/String");
	}

	/**
	 * Nominal Bit rate in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateNominal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "BitRate_Nominal");
	}

	/**
	 * Nominal Bit rate (with measurement).
	 * Shown in inform()
	 */
	public static final String getBitRateNominalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Nominal/String");
	}

	/**
	 * Maximum Bit rate in bps.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateMaximum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "BitRate_Maximum");
	}

	/**
	 * Maximum Bit rate (with measurement).
	 * Shown in inform()
	 */
	public static final String getBitRateMaximumString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Maximum/String");
	}

	/**
	 * Encoded (with forced padding) bit rate in bps, if some container padding is present.
	 * Shown in Info_Capacities()
	 */
	public static final Double getBitRateEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "BitRate_Encoded");
	}

	/**
	 * Encoded (with forced padding) bit rate (with measurement), if some container padding is present.
	 */
	public static final String getBitRateEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "BitRate_Encoded/String");
	}

	/**
	 * Frames per second.
	 * Shown in Info_Capacities()
	 */
	public static final Double getFrameRate(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "FrameRate");
	}

	/**
	 * Frames per second (with measurement).
	 * Shown in inform()
	 */
	public static final String getFrameRateString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "FrameRate/String");
	}

	/**
	 * Frames per second, numerator.
	 */
	public static final Long getFrameRateNum(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "FrameRate_Num");
	}

	/**
	 * Frames per second, denominator.
	 */
	public static final Long getFrameRateDen(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "FrameRate_Den");
	}

	/**
	 * Number of frames.
	 */
	public static final Long getFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "FrameCount");
	}

	/**
	 * Source Number of frames.
	 */
	public static final Long getSourceFrameCount(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Source_FrameCount");
	}

	/**
	 * Delay fixed in the stream (relative) IN MS;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Delay");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String1");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String2");
	}

	/**
	 * Delay in format : HH:MM:SS.MMM;.
	 */
	public static final String getDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String3");
	}

	/**
	 * Delay in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String4");
	}

	/**
	 * Delay in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay/String5");
	}

	/**
	 * Delay settings (in case of timecode for example);.
	 */
	public static final String getDelaySettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Settings");
	}

	/**
	 * Delay drop frame;.
	 */
	public static final String getDelayDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_DropFrame");
	}

	/**
	 * Delay source (Container or Stream or empty);.
	 */
	public static final String getDelaySource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Source");
	}

	/**
	 * Delay source (Container or Stream or empty);.
	 */
	public static final String getDelaySourceString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Source/String");
	}

	/**
	 * Delay fixed in the raw stream (relative) IN MS;.
	 */
	public static final Long getDelayOriginal(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Delay_Original");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayOriginalString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayOriginalString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String1");
	}

	/**
	 * Delay with measurement;.
	 */
	public static final String getDelayOriginalString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String2");
	}

	/**
	 * Delay in format: HH:MM:SS.MMM.
	 */
	public static final String getDelayOriginalString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String3");
	}

	/**
	 * Delay in format: HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available;.
	 */
	public static final String getDelayOriginalString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String4");
	}

	/**
	 * Delay in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getDelayOriginalString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original/String5");
	}

	/**
	 * Delay settings (in case of timecode for example).
	 */
	public static final String getDelayOriginalSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original_Settings");
	}

	/**
	 * Delay drop frame info;.
	 */
	public static final String getDelayOriginalDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original_DropFrame");
	}

	/**
	 * Delay source (Stream or empty);.
	 */
	public static final String getDelayOriginalSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Delay_Original_Source");
	}

	/**
	 * Delay fixed in the stream (absolute / video).
	 */
	public static final Double getVideoDelay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "Video_Delay");
	}

	/**
	 * .
	 * Shown in inform()
	 */
	public static final String getVideoDelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String");
	}

	/**
	 * .
	 */
	public static final String getVideoDelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String1");
	}

	/**
	 * .
	 */
	public static final String getVideoDelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String2");
	}

	/**
	 * .
	 */
	public static final String getVideoDelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String3");
	}

	/**
	 * .
	 */
	public static final String getVideoDelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String4");
	}

	/**
	 * .
	 */
	public static final String getVideoDelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video_Delay/String5");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final Long getVideo0Delay(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Video0_Delay");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String1");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String2");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String3");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String4");
	}

	/**
	 * Deprecated, do not use in new projects.
	 */
	@Deprecated
	public static final String getVideo0DelayString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Video0_Delay/String5");
	}

	/**
	 * TimeStamp fixed in the stream (relative) IN MS;.
	 * Shown in Info_Capacities()
	 */
	public static final Double getTimeStampFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getDouble(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame");
	}

	/**
	 * TimeStamp with measurement;.
	 * Shown in inform()
	 */
	public static final String getTimeStampFirstFrameString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String");
	}

	/**
	 * TimeStamp with measurement;.
	 */
	public static final String getTimeStampFirstFrameString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String1");
	}

	/**
	 * TimeStamp with measurement;.
	 */
	public static final String getTimeStampFirstFrameString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String2");
	}

	/**
	 * TimeStamp in format : HH:MM:SS.MMM;.
	 */
	public static final String getTimeStampFirstFrameString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String3");
	}

	/**
	 * TimeStamp in format : HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available.
	 */
	public static final String getTimeStampFirstFrameString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String4");
	}

	/**
	 * TimeStamp in format : HH:MM:SS.mmm (HH:MM:SS:FF).
	 */
	public static final String getTimeStampFirstFrameString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeStamp_FirstFrame/String5");
	}

	/**
	 * Time code in HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeFirstFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_FirstFrame");
	}

	/**
	 * Time code of the last frame (excluding the duration of the last frame) in HH:MM:SS:FF, last colon replaced by semicolon for drop frame if available format.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeLastFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_LastFrame");
	}

	/**
	 * Delay drop frame;.
	 */
	public static final String getTimeCodeDropFrame(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_DropFrame");
	}

	/**
	 * Time code settings.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSettings(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_Settings");
	}

	/**
	 * Time code is Stripped (only 1st time code, no discontinuity).
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeStripped(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_Stripped");
	}

	/**
	 * Time code is Stripped (only 1st time code, no discontinuity).
	 * Shown in inform()
	 */
	public static final String getTimeCodeStrippedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_Stripped/String");
	}

	/**
	 * Time code source (Container, Stream, SystemScheme1, SDTI, ANC...).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTimeCodeSource(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "TimeCode_Source");
	}

	/**
	 * Streamsize in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamSize");
	}

	/**
	 * Streamsize in with percentage value.
	 * Shown in inform()
	 */
	public static final String getStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String1");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String2");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String3");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String4");
	}

	/**
	 * Streamsize in with percentage value.
	 */
	public static final String getStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize/String5");
	}

	/**
	 * Stream size divided by file size.
	 */
	public static final String getStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Proportion");
	}

	/**
	 * StreamSize in bytes of hte stream after demux.
	 */
	public static final Long getStreamSizeDemuxed(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed");
	}

	/**
	 * StreamSize_Demuxed in with percentage value.
	 */
	public static final String getStreamSizeDemuxedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeDemuxedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String1");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeDemuxedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String2");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeDemuxedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String3");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeDemuxedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String4");
	}

	/**
	 * StreamSize_Demuxed in with percentage value (note: theoritical value, not for real use).
	 */
	public static final String getStreamSizeDemuxedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Demuxed/String5");
	}

	/**
	 * Source Streamsize in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSize(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Source_StreamSize");
	}

	/**
	 * Source Streamsize in with percentage value.
	 * Shown in inform()
	 */
	public static final String getSourceStreamSizeString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String1");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String2");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String3");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String4");
	}

	/**
	 * Source Streamsize in with percentage value.
	 */
	public static final String getSourceStreamSizeString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize/String5");
	}

	/**
	 * Source Stream size divided by file size.
	 */
	public static final String getSourceStreamSizeProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Proportion");
	}

	/**
	 * Encoded Streamsize in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "StreamSize_Encoded");
	}

	/**
	 * Encoded Streamsize in with percentage value.
	 */
	public static final String getStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String1");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String2");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String3");
	}

	/**
	 * .
	 */
	public static final String getStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String4");
	}

	/**
	 * Encoded Streamsize in with percentage value.
	 */
	public static final String getStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded/String5");
	}

	/**
	 * Encoded Stream size divided by file size.
	 */
	public static final String getStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "StreamSize_Encoded_Proportion");
	}

	/**
	 * Source Encoded Streamsize in bytes.
	 * Shown in Info_Capacities()
	 */
	public static final Long getSourceStreamSizeEncoded(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.getLong(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded");
	}

	/**
	 * Source Encoded Streamsize in with percentage value.
	 */
	public static final String getSourceStreamSizeEncodedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeEncodedString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String1");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeEncodedString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String2");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeEncodedString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String3");
	}

	/**
	 * .
	 */
	public static final String getSourceStreamSizeEncodedString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String4");
	}

	/**
	 * Source Encoded Streamsize in with percentage value.
	 */
	public static final String getSourceStreamSizeEncodedString5(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded/String5");
	}

	/**
	 * Source Encoded Stream size divided by file size.
	 */
	public static final String getSourceStreamSizeEncodedProportion(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Source_StreamSize_Encoded_Proportion");
	}

	/**
	 * Name of this menu.
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getTitle(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Title");
	}

	/**
	 * Language (2-letter ISO 639-1 if exists, else 3-letter ISO 639-2, and with optional ISO 3166-1 country separated by a dash if available, e.g. en, en-us, zh-cn).
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguage(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language");
	}

	/**
	 * Language (full).
	 * Shown in inform()
	 */
	public static final String getLanguageString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language/String");
	}

	/**
	 * Language (full).
	 */
	public static final String getLanguageString1(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language/String1");
	}

	/**
	 * Language (2-letter ISO 639-1 if exists, else empty).
	 */
	public static final String getLanguageString2(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language/String2");
	}

	/**
	 * Language (3-letter ISO 639-2 if exists, else empty).
	 */
	public static final String getLanguageString3(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language/String3");
	}

	/**
	 * Language (2-letter ISO 639-1 if exists with optional ISO 3166-1 country separated by a dash if available, e.g. en, en-us, zh-cn, else empty).
	 */
	public static final String getLanguageString4(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language/String4");
	}

	/**
	 * More info about Language (e.g. Director's Comment).
	 * Shown in inform()
	 * Shown in Info_Capacities()
	 */
	public static final String getLanguageMore(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Language_More");
	}

	/**
	 * Service kind, e.g. visually impaired, commentary, voice over.
	 * Shown in Info_Capacities()
	 */
	public static final String getServiceKind(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "ServiceKind");
	}

	/**
	 * Service kind (full).
	 * Shown in inform()
	 */
	public static final String getServiceKindString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "ServiceKind/String");
	}

	/**
	 * Set if that track should not be used.
	 * Shown in Info_Capacities()
	 */
	public static final String getDisabled(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Disabled");
	}

	/**
	 * Set if that track should not be used.
	 * Shown in inform()
	 */
	public static final String getDisabledString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Disabled/String");
	}

	/**
	 * Set if that track should be used if no language found matches the user preference.
	 * Shown in Info_Capacities()
	 */
	public static final String getDefault(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Default");
	}

	/**
	 * Set if that track should be used if no language found matches the user preference.
	 * Shown in inform()
	 */
	public static final String getDefaultString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Default/String");
	}

	/**
	 * Set if that track should be used if no language found matches the user preference.
	 * Shown in Info_Capacities()
	 */
	public static final String getForced(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Forced");
	}

	/**
	 * Set if that track should be used if no language found matches the user preference.
	 * Shown in inform()
	 */
	public static final String getForcedString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "Forced/String");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in Info_Capacities()
	 */
	public static final String getAlternateGroup(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "AlternateGroup");
	}

	/**
	 * Number of a group in order to provide versions of the same track.
	 * Shown in inform()
	 */
	public static final String getAlternateGroupString(MediaInfoHelper mediaInfo, int streamNumber) {
		return mediaInfo.get(StreamKind.OTHER, streamNumber, "AlternateGroup/String");
	}

}
