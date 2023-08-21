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

import java.io.File;
import net.pms.parsers.MediaInfoParser;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class MediaInfoParseLogger {

	private final StringBuilder sb = new StringBuilder();
	private final Columns generalColumns = new Columns(false, 2, 32, 62, 92);
	private final Columns streamColumns = new Columns(false, 4, 34, 64, 94);
	private final MediaInfoHelper mi;

	public MediaInfoParseLogger(MediaInfoHelper mi) {
		this.mi = mi;
	}

	/**
	 * Appends a label and value to the internal {@link StringBuilder} at the
	 * next column using the specified parameters.
	 *
	 * @param columns the {@link Columns} to use.
	 * @param label the label.
	 * @param value the value.
	 * @param quote if {@code true}, {@code value} is wrapped in double quotes.
	 * @param notBlank if {@code true}, doesn't append anything if {@code value}
	 * is {@code null} or only whitespace.
	 * @return {@code true} if something was appended, {@code false} otherwise.
	 */
	private boolean appendStringNextColumn(
			Columns columns,
			String label,
			String value,
			boolean quote,
			boolean notBlank
	) {
		if (notBlank && StringUtils.isBlank(value)) {
			return false;
		}
		sb.append(columns.toNextColumnRelative(sb));
		appendString(label, value, true, quote, false);
		return true;
	}

	/**
	 * Appends a label and value to the internal {@link StringBuilder} at the
	 * specified column using the specified parameters.
	 *
	 * @param columns the {@link Columns} to use.
	 * @param column the column number.
	 * @param label the label.
	 * @param value the value.
	 * @param quote if {@code true}, {@code value} is wrapped in double quotes.
	 * @param notBlank if {@code true}, doesn't append anything if {@code value}
	 * is {@code null} or only whitespace.
	 * @return {@code true} if something was appended, {@code false} otherwise.
	 */
	private boolean appendStringColumn(
			Columns columns,
			int column,
			String label,
			String value,
			boolean quote,
			boolean notBlank
	) {
		if (notBlank && StringUtils.isBlank(value)) {
			return false;
		}
		sb.append(columns.toColumn(sb, column));
		appendString(label, value, true, quote, false);
		return true;
	}

	/**
	 * Appends a label and value to the internal {@link StringBuilder} using the
	 * specified parameters.
	 *
	 * @param label the label.
	 * @param value the value.
	 * @param first if {@code false}, {@code ", "} is added first.
	 * @param quote if {@code true}, {@code value} is wrapped in double quotes.
	 * @param notBlank if {@code true}, doesn't append anything if {@code value}
	 * is {@code null} or only whitespace.
	 * @return {@code true} if something was appended, {@code false} otherwise.
	 */
	private boolean appendString(String label, String value, boolean first, boolean quote, boolean notBlank) {
		if (notBlank && StringUtils.isBlank(value)) {
			return false;
		}
		if (!first) {
			sb.append(", ");
		}
		sb.append(label);
		if (quote) {
			sb.append(": \"");
		} else {
			sb.append(": ");
		}
		sb.append(quote ? value : value.trim());
		if (quote) {
			sb.append("\"");
		}
		return true;
	}

	/**
	 * Appends a label and a boolean value to the internal {@link StringBuilder}
	 * at the next column using the specified parameters. The boolean value will
	 * be {@code "False"} if {@code value} is {@code null} or only whitespace,
	 * {@code "True"} otherwise.
	 *
	 * @param columns the {@link Columns} to use.
	 * @param label the label.
	 * @param value the value to evaluate.
	 * @param booleanValues if {@code true}, {@code "True"} and {@code "False"}
	 * will be used. If {@code false}, {@code "Yes"} and {@code "No"} will be
	 * used.
	 * @return Always {@code true}.
	 */
	private boolean appendExistsNextColumn(Columns columns, String label, String value, boolean booleanValues) {
		sb.append(columns.toNextColumnRelative(sb));
		appendExists(label, value, true, booleanValues);
		return true;
	}

	/**
	 * Appends a label and a boolean value to the internal {@link StringBuilder}
	 * at the specified column using the specified parameters. The boolean value
	 * will be {@code "False"} if {@code value} is {@code null} or only
	 * whitespace, {@code "True"} otherwise.
	 *
	 * @param columns the {@link Columns} to use.
	 * @param column the column number.
	 * @param label the label.
	 * @param value the value to evaluate.
	 * @param booleanValues if {@code true}, {@code "True"} and {@code "False"}
	 * will be used. If {@code false}, {@code "Yes"} and {@code "No"} will be
	 * used.
	 * @return Always {@code true}.
	 */
	private boolean appendExistsColumn(
			Columns columns,
			int column,
			String label,
			String value,
			boolean booleanValues
	) {
		sb.append(columns.toColumn(sb, column));
		appendExists(label, value, true, booleanValues);
		return true;
	}

	/**
	 * Appends a label and a boolean value to the internal {@link StringBuilder}
	 * using the specified parameters. The boolean value will be {@code "False"}
	 * if {@code value} is {@code null} or only whitespace, {@code "True"}
	 * otherwise.
	 *
	 * @param label the label.
	 * @param value the value to evaluate.
	 * @param first if {@code false}, {@code ", "} is added first.
	 * @param booleanValues if {@code true}, {@code "True"} and {@code "False"}
	 * will be used. If {@code false}, {@code "Yes"} and {@code "No"} will be
	 * used.
	 * @return Always {@code true}.
	 */
	private boolean appendExists(String label, String value, boolean first, boolean booleanValues) {
		if (!first) {
			sb.append(", ");
		}
		sb.append(label).append(": ");
		if (StringUtils.isBlank(value)) {
			sb.append(booleanValues ? "False" : "No");
		} else {
			sb.append(booleanValues ? "True" : "Yes");
		}
		return true;
	}

	public void logGeneral(File file) {
		if (file == null) {
			sb.append("MediaInfo parsing results for null:\n");
		} else {
			sb.append("MediaInfo parsing results for \"").append(file.getAbsolutePath()).append("\":\n");
		}
		if (mi == null) {
			sb.append("ERROR: LibMediaInfo instance is null");
			return;
		}
		if (!mi.isValid()) {
			sb.append("ERROR: LibMediaInfo instance not valid");
			return;
		}
		sb.append("  ");
		boolean first = true;
		first &= !appendString("Title", mi.get(StreamKind.GENERAL, 0, "Title"), first, true, true);
		first &= !appendString("Format", mi.get(StreamKind.GENERAL, 0, "Format"), first, true, false);
		first &= !appendString("CodecID", mi.get(StreamKind.GENERAL, 0, "CodecID"), first, true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.GENERAL, 0, "Duration"));
		if (durationSec != null) {
			first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
		}
		first &= !appendString("Overall Bitrate Mode", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Mode"), first, false, true);
		first &= !appendString("Overall Bitrate", mi.get(StreamKind.GENERAL, 0, "OverallBitRate"), first, false, true);
		first &= !appendString("Overall Bitrate Nom.", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Nominal"), first, false, true);
		first &= !appendString("Overall Bitrate Max.", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Maximum"), first, false, true);
		first &= !appendString("Stereoscopic", mi.get(StreamKind.GENERAL, 0, "StereoscopicLayout"), first, true, true);
		appendExists("Cover", mi.get(StreamKind.GENERAL, 0, "Cover_Data"), first, false);
		first = false;
		appendString("FPS", mi.get(StreamKind.GENERAL, 0, "FrameRate"), first, false, true);
		appendString("Track", mi.get(StreamKind.GENERAL, 0, "Track"), first, true, true);
		appendString("Album", mi.get(StreamKind.GENERAL, 0, "Album"), first, true, true);
		appendString("Performer", mi.get(StreamKind.GENERAL, 0, "Performer"), first, true, true);
		appendString("Genre", mi.get(StreamKind.GENERAL, 0, "Genre"), first, true, true);
		appendString("Rec Date", mi.get(StreamKind.GENERAL, 0, "Recorded_Date"), first, true, true);
	}

	public void logGeneralColumns(File file) {
		if (file == null) {
			sb.append("MediaInfo parsing results for null:\n");
		} else {
			sb.append("MediaInfo parsing results for \"").append(file.getAbsolutePath()).append("\":\n");
		}
		if (mi == null) {
			sb.append("ERROR: LibMediaInfo instance is null");
			return;
		}
		if (!mi.isValid()) {
			sb.append("ERROR: LibMediaInfo instance not valid");
			return;
		}
		generalColumns.reset();
		appendStringNextColumn(generalColumns, "Title", mi.get(StreamKind.GENERAL, 0, "Title"), true, true);
		appendStringNextColumn(generalColumns, "Format", mi.get(StreamKind.GENERAL, 0, "Format"), true, false);
		appendStringNextColumn(generalColumns, "CodecID", mi.get(StreamKind.GENERAL, 0, "CodecID"), true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.GENERAL, 0, "Duration"));
		if (durationSec != null) {
			appendStringNextColumn(generalColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
		}
		appendStringNextColumn(generalColumns, "Overall Bitrate Mode", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Mode"), false, true);
		appendStringNextColumn(generalColumns, "Overall Bitrate", mi.get(StreamKind.GENERAL, 0, "OverallBitRate"), false, true);
		appendStringNextColumn(generalColumns, "Overall Bitrate Nom.", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Nominal"), false, true);
		appendStringNextColumn(generalColumns, "Overall Bitrate Max.", mi.get(StreamKind.GENERAL, 0, "OverallBitRate_Maximum"), false, true);
		appendStringNextColumn(generalColumns, "Stereoscopic", mi.get(StreamKind.GENERAL, 0, "StereoscopicLayout"), true, true);
		appendExistsNextColumn(generalColumns, "Cover", mi.get(StreamKind.GENERAL, 0, "Cover_Data"), false);
		appendStringNextColumn(generalColumns, "FPS", mi.get(StreamKind.GENERAL, 0, "FrameRate"), false, true);
		appendStringNextColumn(generalColumns, "Track", mi.get(StreamKind.GENERAL, 0, "Track"), true, true);
		appendStringNextColumn(generalColumns, "Album", mi.get(StreamKind.GENERAL, 0, "Album"), true, true);
		appendStringNextColumn(generalColumns, "Performer", mi.get(StreamKind.GENERAL, 0, "Performer"), true, true);
		appendStringNextColumn(generalColumns, "Genre", mi.get(StreamKind.GENERAL, 0, "Genre"), true, true);
		appendStringNextColumn(generalColumns, "Rec Date", mi.get(StreamKind.GENERAL, 0, "Recorded_Date"), true, true);
	}

	public void logVideoTrack(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n    - Video - ");
		boolean first = true;
		first &= !appendString("Format", mi.get(StreamKind.VIDEO, idx, "Format"), first, true, true);
		first &= !appendString("Version", mi.get(StreamKind.VIDEO, idx, "Format_Version"), first, true, true);
		first &= !appendString("Profile", mi.get(StreamKind.VIDEO, idx, "Format_Profile"), first, true, true);
		first &= !appendString("ID", mi.get(StreamKind.VIDEO, idx, "ID"), first, false, true);
		first &= !appendString("CodecID", mi.get(StreamKind.VIDEO, idx, "CodecID"), first, true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.VIDEO, 0, "Duration"));
		if (durationSec != null) {
			first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
		}
		first &= !appendString("BitRate Mode", mi.get(StreamKind.VIDEO, idx, "BitRate_Mode"), first, false, true);
		first &= !appendString("Bitrate", mi.get(StreamKind.VIDEO, idx, "BitRate"), first, false, true);
		first &= !appendString("Bitrate Nominal", mi.get(StreamKind.VIDEO, idx, "BitRate_Nominal"), first, false, true);
		first &= !appendString("BitRate Maximum", mi.get(StreamKind.VIDEO, idx, "BitRate_Maximum"), first, false, true);
		first &= !appendString("Bitrate Encoded", mi.get(StreamKind.VIDEO, idx, "BitRate_Encoded"), first, false, true);
		first &= !appendString("Width", mi.get(StreamKind.VIDEO, idx, "Width"), first, false, true);
		first &= !appendString("Height", mi.get(StreamKind.VIDEO, idx, "Height"), first, false, true);
		first &= !appendString("Colorimetry", mi.get(StreamKind.VIDEO, idx, "Colorimetry"), first, false, true);
		first &= !appendString("Chroma", mi.get(StreamKind.VIDEO, idx, "ChromaSubsampling"), first, false, true);
		first &= !appendString("Matrix Co", mi.get(StreamKind.VIDEO, idx, "matrix_coefficients"), first, false, true);
		first &= !appendString("MultiView Layout", mi.get(StreamKind.VIDEO, idx, "MultiView_Layout"), first, true, true);
		first &= !appendString("PAR", mi.get(StreamKind.VIDEO, idx, "PixelAspectRatio"), first, false, true);
		first &= !appendString("DAR", mi.get(StreamKind.VIDEO, idx, "DisplayAspectRatio/String"), first, false, true);
		first &= !appendString("DAR Orig", mi.get(StreamKind.VIDEO, idx, "DisplayAspectRatio_Original/String"), first, false, true);
		first &= !appendString("Scan Type", mi.get(StreamKind.VIDEO, idx, "ScanType"), first, false, true);
		first &= !appendString("Scan Order", mi.get(StreamKind.VIDEO, idx, "ScanOrder"), first, false, true);
		first &= !appendString("FPS", mi.get(StreamKind.VIDEO, idx, "FrameRate"), first, false, true);
		first &= !appendString("FPS Orig", mi.get(StreamKind.VIDEO, idx, "FrameRate_Original"), first, false, true);
		first &= !appendString("Framerate Mode", mi.get(StreamKind.VIDEO, idx, "FrameRate_Mode"), first, false, true);
		first &= !appendString("RefFrames", mi.get(StreamKind.VIDEO, idx, "Format_Settings_RefFrames"), first, false, true);
		first &= !appendString("QPel", mi.get(StreamKind.VIDEO, idx, "Format_Settings_QPel"), first, true, true);
		first &= !appendString("GMC", mi.get(StreamKind.VIDEO, idx, "Format_Settings_GMC"), first, true, true);
		first &= !appendString("GOP", mi.get(StreamKind.VIDEO, idx, "Format_Settings_GOP"), first, true, true);
		first &= !appendString("Muxing Mode", mi.get(StreamKind.VIDEO, idx, "MuxingMode"), first, true, true);
		first &= !appendString("Encrypt", mi.get(StreamKind.VIDEO, idx, "Encryption"), first, true, true);
		first &= !appendString("Bit Depth", mi.get(StreamKind.VIDEO, idx, "BitDepth"), first, false, true);
		first &= !appendString("Delay", mi.get(StreamKind.VIDEO, idx, "Delay"), first, false, true);
		first &= !appendString("Delay Source", mi.get(StreamKind.VIDEO, idx, "Delay_Source"), first, false, true);
		first &= !appendString("Delay Original", mi.get(StreamKind.VIDEO, idx, "Delay_Original"), first, false, true);
		first &= !appendString("Delay O. Source", mi.get(StreamKind.VIDEO, idx, "Delay_Original_Source"), first, false, true);
		appendString("TimeStamp_FirstFrame", mi.get(StreamKind.VIDEO, idx, "TimeStamp_FirstFrame"), first, false, true);
	}

	public void logVideoTrackColumns(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n  - Video track ");
		appendString("ID", mi.get(StreamKind.VIDEO, idx, "ID"), true, false, false);
		streamColumns.reset();
		sb.append("\n");
		appendStringNextColumn(streamColumns, "Format", mi.get(StreamKind.VIDEO, idx, "Format"), true, true);
		appendStringNextColumn(streamColumns, "Version", mi.get(StreamKind.VIDEO, idx, "Format_Version"), true, true);
		appendStringNextColumn(streamColumns, "Profile", mi.get(StreamKind.VIDEO, idx, "Format_Profile"), true, true);
		appendStringNextColumn(streamColumns, "CodecID", mi.get(StreamKind.VIDEO, idx, "CodecID"), true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.VIDEO, 0, "Duration"));
		if (durationSec != null) {
			appendStringNextColumn(streamColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
		}
		appendStringNextColumn(streamColumns, "BitRate Mode", mi.get(StreamKind.VIDEO, idx, "BitRate_Mode"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate", mi.get(StreamKind.VIDEO, idx, "BitRate"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate Nominal", mi.get(StreamKind.VIDEO, idx, "BitRate_Nominal"), false, true);
		appendStringNextColumn(streamColumns, "BitRate Maximum", mi.get(StreamKind.VIDEO, idx, "BitRate_Maximum"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate Encoded", mi.get(StreamKind.VIDEO, idx, "BitRate_Encoded"), false, true);
		appendStringNextColumn(streamColumns, "Width", mi.get(StreamKind.VIDEO, idx, "Width"), false, true);
		appendStringNextColumn(streamColumns, "Height", mi.get(StreamKind.VIDEO, idx, "Height"), false, true);
		appendStringNextColumn(streamColumns, "Colorimetry", mi.get(StreamKind.VIDEO, idx, "Colorimetry"), false, true);
		appendStringNextColumn(streamColumns, "Chroma", mi.get(StreamKind.VIDEO, idx, "ChromaSubsampling"), false, true);
		appendStringNextColumn(streamColumns, "Matrix Co", mi.get(StreamKind.VIDEO, idx, "matrix_coefficients"), false, true);
		appendStringNextColumn(streamColumns, "MultiView Layout", mi.get(StreamKind.VIDEO, idx, "MultiView_Layout"), true, true);
		appendStringNextColumn(streamColumns, "PAR", mi.get(StreamKind.VIDEO, idx, "PixelAspectRatio"), false, true);
		appendStringNextColumn(streamColumns, "DAR", mi.get(StreamKind.VIDEO, idx, "DisplayAspectRatio/String"), false, true);
		appendStringNextColumn(streamColumns, "DAR Orig", mi.get(StreamKind.VIDEO, idx, "DisplayAspectRatio_Original/String"), false, true);
		appendStringNextColumn(streamColumns, "Scan Type", mi.get(StreamKind.VIDEO, idx, "ScanType"), false, true);
		appendStringNextColumn(streamColumns, "Scan Order", mi.get(StreamKind.VIDEO, idx, "ScanOrder"), false, true);
		appendStringNextColumn(streamColumns, "FPS", mi.get(StreamKind.VIDEO, idx, "FrameRate"), false, true);
		appendStringNextColumn(streamColumns, "FPS Orig", mi.get(StreamKind.VIDEO, idx, "FrameRate_Original"), false, true);
		appendStringNextColumn(streamColumns, "Framerate Mode", mi.get(StreamKind.VIDEO, idx, "FrameRate_Mode"), false, true);
		appendStringNextColumn(streamColumns, "RefFrames", mi.get(StreamKind.VIDEO, idx, "Format_Settings_RefFrames"), false, true);
		appendStringNextColumn(streamColumns, "QPel", mi.get(StreamKind.VIDEO, idx, "Format_Settings_QPel"), true, true);
		appendStringNextColumn(streamColumns, "GMC", mi.get(StreamKind.VIDEO, idx, "Format_Settings_GMC"), true, true);
		appendStringNextColumn(streamColumns, "GOP", mi.get(StreamKind.VIDEO, idx, "Format_Settings_GOP"), true, true);
		appendStringNextColumn(streamColumns, "Muxing Mode", mi.get(StreamKind.VIDEO, idx, "MuxingMode"), true, true);
		appendStringNextColumn(streamColumns, "Encrypt", mi.get(StreamKind.VIDEO, idx, "Encryption"), true, true);
		appendStringNextColumn(streamColumns, "Bit Depth", mi.get(StreamKind.VIDEO, idx, "BitDepth"), false, true);
		appendStringNextColumn(streamColumns, "Delay", mi.get(StreamKind.VIDEO, idx, "Delay"), false, true);
		appendStringNextColumn(streamColumns, "Delay Source", mi.get(StreamKind.VIDEO, idx, "Delay_Source"), false, true);
		appendStringNextColumn(streamColumns, "Delay Original", mi.get(StreamKind.VIDEO, idx, "Delay_Original"), false, true);
		appendStringNextColumn(streamColumns, "Delay O. Source", mi.get(StreamKind.VIDEO, idx, "Delay_Original_Source"), false, true);
		appendStringNextColumn(streamColumns, "TimeStamp_FirstFrame", mi.get(StreamKind.VIDEO, idx, "TimeStamp_FirstFrame"), false, true);
	}

	public void logAudioTrack(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n    - Audio - ");
		boolean first = true;
		first &= !appendString("Title", mi.get(StreamKind.AUDIO, idx, "Title"), first, true, true);
		first &= !appendString("Format", mi.get(StreamKind.AUDIO, idx, "Format"), first, true, true);
		first &= !appendString("Version", mi.get(StreamKind.AUDIO, idx, "Format_Version"), first, true, true);
		first &= !appendString("Profile", mi.get(StreamKind.AUDIO, idx, "Format_Profile"), first, true, true);
		first &= !appendString("ID", mi.get(StreamKind.AUDIO, idx, "ID"), first, false, true);
		first &= !appendString("CodecID", mi.get(StreamKind.AUDIO, idx, "CodecID"), first, true, true);
		first &= !appendString("CodecID Desc", mi.get(StreamKind.AUDIO, idx, "CodecID_Description"), first, true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.AUDIO, 0, "Duration"));
		if (durationSec != null) {
			first &= !appendString("Duration", StringUtil.formatDLNADuration(durationSec), first, false, true);
		}
		first &= !appendString("BitRate Mode", mi.get(StreamKind.AUDIO, idx, "BitRate_Mode"), first, false, true);
		first &= !appendString("Bitrate", mi.get(StreamKind.AUDIO, idx, "BitRate"), first, false, true);
		first &= !appendString("Bitrate Nominal", mi.get(StreamKind.AUDIO, idx, "BitRate_Nominal"), first, false, true);
		first &= !appendString("BitRate Maximum", mi.get(StreamKind.AUDIO, idx, "BitRate_Maximum"), first, false, true);
		first &= !appendString("Bitrate Encoded", mi.get(StreamKind.AUDIO, idx, "BitRate_Encoded"), first, false, true);
		first &= !appendString("Language", mi.get(StreamKind.AUDIO, idx, "Language"), first, true, true);
		first &= !appendString("Channel(s)", mi.get(StreamKind.AUDIO, idx, "Channel(s)_Original"), first, false, true);
		first &= !appendString("Samplerate", mi.get(StreamKind.AUDIO, idx, "SamplingRate"), first, false, true);
		first &= !appendString("Track", mi.get(StreamKind.GENERAL, idx, "Track/Position"), first, false, true);
		first &= !appendString("Bit Depth", mi.get(StreamKind.AUDIO, idx, "BitDepth"), first, false, true);
		first &= !appendString("Delay", mi.get(StreamKind.AUDIO, idx, "Delay"), first, false, true);
		first &= !appendString("Delay Source", mi.get(StreamKind.AUDIO, idx, "Delay_Source"), first, false, true);
		first &= !appendString("Delay Original", mi.get(StreamKind.AUDIO, idx, "Delay_Original"), first, false, true);
		appendString("Delay O. Source", mi.get(StreamKind.AUDIO, idx, "Delay_Original_Source"), first, false, true);
	}

	public void logAudioTrackColumns(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n  - Audio track ");
		appendString("ID", mi.get(StreamKind.AUDIO, idx, "ID"), true, false, false);
		appendString("Title", mi.get(StreamKind.AUDIO, idx, "Title"), false, true, true);
		streamColumns.reset();
		sb.append("\n");
		appendStringNextColumn(streamColumns, "Format", mi.get(StreamKind.AUDIO, idx, "Format/String"), true, true);
		appendStringNextColumn(streamColumns, "Version", mi.get(StreamKind.AUDIO, idx, "Format_Version"), true, true);
		appendStringNextColumn(streamColumns, "Profile", mi.get(StreamKind.AUDIO, idx, "Format_Profile"), true, true);
		appendStringNextColumn(streamColumns, "CodecID", mi.get(StreamKind.AUDIO, idx, "CodecID"), true, true);
		appendStringNextColumn(streamColumns, "CodecID Desc", mi.get(StreamKind.AUDIO, idx, "CodecID_Description"), true, true);
		Double durationSec = MediaInfoParser.parseDuration(mi.get(StreamKind.AUDIO, 0, "Duration"));
		if (durationSec != null) {
			appendStringNextColumn(streamColumns, "Duration", StringUtil.formatDLNADuration(durationSec), false, true);
		}
		appendStringNextColumn(streamColumns, "BitRate Mode", mi.get(StreamKind.AUDIO, idx, "BitRate_Mode"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate", mi.get(StreamKind.AUDIO, idx, "BitRate"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate Nominal", mi.get(StreamKind.AUDIO, idx, "BitRate_Nominal"), false, true);
		appendStringNextColumn(streamColumns, "BitRate Maximum", mi.get(StreamKind.AUDIO, idx, "BitRate_Maximum"), false, true);
		appendStringNextColumn(streamColumns, "Bitrate Encoded", mi.get(StreamKind.AUDIO, idx, "BitRate_Encoded"), false, true);
		appendStringNextColumn(streamColumns, "Language", mi.get(StreamKind.AUDIO, idx, "Language"), true, true);
		appendStringNextColumn(streamColumns, "Channel(s)", mi.get(StreamKind.AUDIO, idx, "Channel(s)"), false, true);
		appendStringNextColumn(streamColumns, "Samplerate", mi.get(StreamKind.AUDIO, idx, "SamplingRate"), false, true);
		appendStringNextColumn(streamColumns, "Track", mi.get(StreamKind.GENERAL, idx, "Track/Position"), false, true);
		appendStringNextColumn(streamColumns, "Bit Depth", mi.get(StreamKind.AUDIO, idx, "BitDepth"), false, true);
		appendStringNextColumn(streamColumns, "Delay", mi.get(StreamKind.AUDIO, idx, "Delay"), false, true);
		appendStringNextColumn(streamColumns, "Delay Source", mi.get(StreamKind.AUDIO, idx, "Delay_Source"), false, true);
		appendStringNextColumn(streamColumns, "Delay Original", mi.get(StreamKind.AUDIO, idx, "Delay_Original"), false, true);
		appendStringNextColumn(streamColumns, "Delay O. Source", mi.get(StreamKind.AUDIO, idx, "Delay_Original_Source"), false, true);
	}

	public void logImage(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n    - Image - ");
		boolean first = true;
		first &= !appendString("Format", mi.get(StreamKind.IMAGE, idx, "Format"), first, true, true);
		first &= !appendString("Version", mi.get(StreamKind.IMAGE, idx, "Format_Version"), first, true, true);
		first &= !appendString("Profile", mi.get(StreamKind.IMAGE, idx, "Format_Profile"), first, true, true);
		first &= !appendString("ID", mi.get(StreamKind.IMAGE, idx, "ID"), first, false, true);
		first &= !appendString("Width", mi.get(StreamKind.IMAGE, idx, "Width"), first, false, true);
		appendString("Height", mi.get(StreamKind.IMAGE, idx, "Height"), first, false, true);
	}

	public void logImageColumns(int idx) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n  - Image ");
		appendString("ID", mi.get(StreamKind.IMAGE, idx, "ID"), true, false, false);
		streamColumns.reset();
		sb.append("\n");
		appendStringNextColumn(streamColumns, "Format", mi.get(StreamKind.IMAGE, idx, "Format"), true, true);
		appendStringNextColumn(streamColumns, "Version", mi.get(StreamKind.IMAGE, idx, "Format_Version"), true, true);
		appendStringNextColumn(streamColumns, "Profile", mi.get(StreamKind.IMAGE, idx, "Format_Profile"), true, true);
		appendStringNextColumn(streamColumns, "Width", mi.get(StreamKind.IMAGE, idx, "Width"), false, true);
		appendStringNextColumn(streamColumns, "Height", mi.get(StreamKind.IMAGE, idx, "Height"), false, true);
	}

	public void logSubtitleTrack(int idx, boolean videoSubtitle) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n    - Sub - ");
		boolean first = true;
		if (videoSubtitle) {
			first &= !appendString("Title", mi.get(StreamKind.VIDEO, idx, "Title"), first, true, true);
			first &= !appendString("Format", mi.get(StreamKind.VIDEO, idx, "Format"), first, true, true);
			first &= !appendString("Version", mi.get(StreamKind.VIDEO, idx, "Format_Version"), first, true, true);
			first &= !appendString("Profile", mi.get(StreamKind.VIDEO, idx, "Format_Profile"), first, true, true);
			appendString("ID", mi.get(StreamKind.VIDEO, idx, "ID"), first, false, true);
		} else {
			first &= !appendString("Title", mi.get(StreamKind.TEXT, idx, "Title"), first, true, true);
			first &= !appendString("Format", mi.get(StreamKind.TEXT, idx, "Format"), first, true, true);
			first &= !appendString("Version", mi.get(StreamKind.TEXT, idx, "Format_Version"), first, true, true);
			first &= !appendString("Profile", mi.get(StreamKind.TEXT, idx, "Format_Profile"), first, true, true);
			first &= !appendString("ID", mi.get(StreamKind.TEXT, idx, "ID"), first, false, true);
			appendString("Language", mi.get(StreamKind.TEXT, idx, "Language"), first, true, true);
		}
	}

	public void logSubtitleTrackColumns(int idx, boolean videoSubtitle) {
		if (mi == null || !mi.isValid()) {
			return;
		}

		sb.append("\n  - Subtitle ");
		streamColumns.reset();
		if (videoSubtitle) {
			appendString("ID", mi.get(StreamKind.VIDEO, idx, "ID"), true, false, false);
			appendString("Title", mi.get(StreamKind.VIDEO, idx, "Title"), false, true, true);
			sb.append("\n");
			appendStringNextColumn(streamColumns, "Format", mi.get(StreamKind.VIDEO, idx, "Format"), true, true);
			appendStringNextColumn(streamColumns, "Version", mi.get(StreamKind.VIDEO, idx, "Format_Version"), true, true);
			appendStringNextColumn(streamColumns, "Profile", mi.get(StreamKind.VIDEO, idx, "Format_Profile"), true, true);
		} else {
			appendString("ID", mi.get(StreamKind.TEXT, idx, "ID"), true, false, false);
			appendString("Title", mi.get(StreamKind.TEXT, idx, "Title"), false, true, true);
			sb.append("\n");
			appendStringNextColumn(streamColumns, "Format", mi.get(StreamKind.TEXT, idx, "Format"), true, true);
			appendStringNextColumn(streamColumns, "Version", mi.get(StreamKind.TEXT, idx, "Format_Version"), true, true);
			appendStringNextColumn(streamColumns, "Profile", mi.get(StreamKind.TEXT, idx, "Format_Profile"), true, true);
			appendStringNextColumn(streamColumns, "Language", mi.get(StreamKind.TEXT, idx, "Language"), true, true);
		}
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	protected static class Columns {

		private final boolean includeZeroColumn;
		private final int[] columns;
		private int lastColumn = -1;

		public Columns(boolean includeZeroColumn, int... columns) {
			this.includeZeroColumn = includeZeroColumn;
			this.columns = columns;
		}

		public int lastColumn() {
			return lastColumn;
		}

		public int nextColumn() {
			if (lastColumn < 0 || lastColumn >= columns.length) {
				return includeZeroColumn ? 0 : 1;
			}
			return lastColumn + 1;
		}

		public void reset() {
			lastColumn = -1;
		}

		/**
		 * Returns the whitespace needed to jump to the next sequential column.
		 */
		public String toNextColumnAbsolute(StringBuilder sb) {
			if (sb == null) {
				return "";
			}

			boolean newLine = false;
			int next = nextColumn();
			if (next < lastColumn) {
				newLine = true;
			}
			return newLine ? "\n" + toColumn(0, nextColumn()) : toColumn(sb, nextColumn());
		}

		/**
		 * Returns the whitespace needed to jump to the next available column.
		 */
		public String toNextColumnRelative(StringBuilder sb) {
			if (sb == null) {
				return "";
			}

			boolean newLine = false;
			int linePosition = getLinePosition(sb);
			int column = -1;
			if (includeZeroColumn && linePosition == 0) {
				column = 0;
			} else {
				for (int i = 0; i < columns.length; i++) {
					if (columns[i] > linePosition) {
						column = i + 1;
						break;
					}
				}
			}
			if (column < 0) {
				column = includeZeroColumn ? 0 : 1;
				newLine = true;
			}
			return newLine ? "\n" + toColumn(0, column) : toColumn(linePosition, column);
		}

		public String toColumn(StringBuilder sb, int column) {
			if (sb == null || column > columns.length) {
				return "";
			}

			return toColumn(getLinePosition(sb), column);
		}

		public String toColumn(int linePosition, int column) {
			if (column > columns.length || linePosition < 0) {
				return "";
			}
			if (column < 1) {
				lastColumn = 0;
				return linePosition > 0 ? " " : "";
			}

			lastColumn = column;
			int fill = columns[column - 1] - linePosition;
			if (fill < 1 && linePosition > 0) {
				fill = 1;
			}
			return fill > 0 ? StringUtil.fillString(" ", fill) : "";
		}

		public static int getLinePosition(StringBuilder sb) {
			if (sb == null) {
				return 0;
			}
			int position = sb.lastIndexOf("\n");
			if (position < 0) {
				position = sb.length();
			} else {
				position = sb.length() - position - 1;
			}
			return position;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Columns: 0");
			for (int column : columns) {
				sb.append(", ").append(column);
			}
			return sb.toString();
		}
	}
}
