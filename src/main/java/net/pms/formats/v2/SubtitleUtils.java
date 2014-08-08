/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.formats.v2;

import java.awt.Color;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaInfo.Mode3D;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.io.OutputParams;
import net.pms.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.*;
import static org.mozilla.universalchardet.Constants.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubtitleUtils {
	private final static PmsConfiguration configuration = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleUtils.class);
	private final static Map<String, String> fileCharsetToMencoderSubcpOptionMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			// Cyrillic / Russian
			put(CHARSET_IBM855, "enca:ru:cp1251");
			put(CHARSET_ISO_8859_5, "enca:ru:cp1251");
			put(CHARSET_KOI8_R, "enca:ru:cp1251");
			put(CHARSET_MACCYRILLIC, "enca:ru:cp1251");
			put(CHARSET_WINDOWS_1251, "enca:ru:cp1251");
			put(CHARSET_IBM866, "enca:ru:cp1251");
			// Greek
			put(CHARSET_WINDOWS_1253, "cp1253");
			put(CHARSET_ISO_8859_7, "ISO-8859-7");
			// Western Europe
			put(CHARSET_WINDOWS_1252, "cp1252");
			// Hebrew
			put(CHARSET_WINDOWS_1255, "cp1255");
			put(CHARSET_ISO_8859_8, "ISO-8859-8");
			// Chinese
			put(CHARSET_ISO_2022_CN, "ISO-2022-CN");
			put(CHARSET_BIG5, "enca:zh:big5");
			put(CHARSET_GB18030, "enca:zh:big5");
			put(CHARSET_EUC_TW, "enca:zh:big5");
			put(CHARSET_HZ_GB_2312, "enca:zh:big5");
			// Korean
			put(CHARSET_ISO_2022_KR, "cp949");
			put(CHARSET_EUC_KR, "euc-kr");
			// Japanese
			put(CHARSET_ISO_2022_JP, "ISO-2022-JP");
			put(CHARSET_EUC_JP, "euc-jp");
			put(CHARSET_SHIFT_JIS, "shift-jis");
		}
	};

	/**
	 * Returns value for -subcp option for non UTF-8 external subtitles based on
	 * detected charset.
	 *
	 * @param dlnaMediaSubtitle DLNAMediaSubtitle with external subtitles file.
	 * @return value for mencoder's -subcp option or null if can't determine.
	 */
	public static String getSubCpOptionForMencoder(DLNAMediaSubtitle dlnaMediaSubtitle) {
		if (dlnaMediaSubtitle == null) {
			throw new NullPointerException("dlnaMediaSubtitle can't be null.");
		}
		if (isBlank(dlnaMediaSubtitle.getExternalFileCharacterSet())) {
			return null;
		}
		return fileCharsetToMencoderSubcpOptionMap.get(dlnaMediaSubtitle.getExternalFileCharacterSet());
	}

	/**
	 * Applies codepage conversion to subtitles file
	 *
	 * @param fileToConvert Subtitles file to convert
	 * @param outputSubs Converted subtitles file
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File applyCodepageConversion(File fileToConvert, File outputSubs) throws IOException {
		String line;
		BufferedReader reader;
		String cp = configuration.getSubtitlesCodepage();
		final boolean isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM = isNotBlank(cp) && Charset.isSupported(cp);
		if (isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToConvert), Charset.forName(cp)));
		} else {
			reader = FileUtil.bufferedReaderWithCorrectCharset(fileToConvert);
		}

		try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), Charset.forName(CHARSET_UTF_8)))) {
			while ((line = reader.readLine()) != null) {
				output.write(line + "\n");
			}

			output.flush();
			output.close();
		}

		reader.close();
		return outputSubs;
	}

	/**
	 * Converts subtitles from the SUBRIP format to the WebVTT format
	 *
	 * @param tempSubs Subtitles file to convert
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File convertSubripToWebVTT(File tempSubs) throws IOException {
		File outputSubs = new File(FilenameUtils.getFullPath(tempSubs.getPath()), FilenameUtils.getBaseName(tempSubs.getName()) + ".vtt");
		StringBuilder outputString = new StringBuilder();
		String subsFileCharset = FileUtil.getFileCharset(tempSubs);
		BufferedWriter output;
		Pattern timePattern = Pattern.compile("([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) --> ([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3})");
		try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(tempSubs), Charset.forName(subsFileCharset)))) {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), Charset.forName(CHARSET_UTF_8)));
			String line;
			outputString.append("WEBVTT\n\n");
			output.write(outputString.toString());
			while ((line = input.readLine()) != null) {
				outputString.setLength(0);
				Matcher timeMatcher = timePattern.matcher(line);
				if (timeMatcher.find()) {
					outputString.append(timeMatcher.group().replace(",", ".")).append("\n");
					output.write(outputString.toString());
					continue;
				}

				line = line.replace("&", "&amp;");
				if (countMatches(line, "<") == 1) {
					line = line.replace("<", "&lt;");
				}

				if (countMatches(line, ">") == 1) {
					line = line.replace(">", "&gt;");
				}

				if (line.startsWith("{") && line.contains("}")) {
					line = line.substring(line.indexOf('}') + 1);
				}

				outputString.append(line).append("\n");
				output.write(outputString.toString());
			}
		}

		output.flush();
		output.close();
		return outputSubs;
	}

	/**
	 * Converts ASS/SSA subtitles to 3D ASS/SSA subtitles.
	 * Based on https://bitbucket.org/r3pek/srt2ass3d
	 *
	 * @param tempSubs Subtitles file to convert
	 * @param media Information about video
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File convertASSToASS3D(File tempSubs, DLNAMediaInfo media, OutputParams params) throws IOException, NullPointerException {
		File outputSubs = new File(FilenameUtils.getFullPath(tempSubs.getPath()), FilenameUtils.getBaseName(tempSubs.getName()) + "_3D.ass");
		StringBuilder outputString = new StringBuilder();
		String subsFileCharset = FileUtil.getFileCharset(tempSubs);
		BufferedWriter output;
		Mode3D mode3D = media.get3DLayout();
		if (mode3D == null) {
			LOGGER.debug("The 3D layout not recognized for the 3D video");
			throw new NullPointerException("The 3D layout not recognized for the 3D video");
		}

		boolean isAnaglyph = media.stereoscopyIsAnaglyph();
		int playResX;
		int playResY;
		if (mode3D == Mode3D.ABL || mode3D == Mode3D.ABR) {
			playResX = media.getWidth();
			playResY = media.getHeight() / 2;
		} else {
			playResX = media.getWidth() / 2;
			playResY = media.getHeight();
		}

		// First try to calculate subtitles position and depth
		// Max depth - 2% ... + 2%
		int depth3D = (int) - (((double) playResX /(double) 100) * Double.valueOf(configuration.getDepth3D()));
		int offset = (playResX / 100) * 2;
		int bottomSubsPosition = (int) ((playResY /(double) 100) * Double.valueOf(configuration.getAssMargin()));
		int topSubsPositionTb = playResY + bottomSubsPosition;
		int middleSbs = media.getWidth() / 2;
		Pattern timePattern = Pattern.compile("[0-9]:[0-9]{2}:[0-9]{2}.[0-9]{2},[0-9]:[0-9]{2}:[0-9]{2}.[0-9]{2},");
		try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(tempSubs), Charset.forName(subsFileCharset)))) {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), Charset.forName(CHARSET_UTF_8)));
			String line;
			outputString.append("[Script Info]\n");
			outputString.append("ScriptType: v4.00+\n");
			outputString.append("WrapStyle: 0\n");
			outputString.append("PlayResX: ").append(media.getWidth()).append("\n");
			outputString.append("PlayResY: ").append(media.getHeight()).append("\n");
			outputString.append("ScaledBorderAndShadow: yes\n\n");
			outputString.append("[V4+ Styles]\n");
			outputString.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
			String fontScale = Double.toString(100 * Double.parseDouble(configuration.getAssScale()));
			String primaryColour = convertColorToAssHexFormat(new Color(configuration.getSubsColor()));
			String outline = configuration.getAssOutline();
			String shadow = configuration.getAssShadow();
			if (isAnaglyph) {
				outputString.append("Style: 3D1,Arial,32,").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,6,0,2,0,0,0,1\n");
				outputString.append("Style: 3D2,Arial,32,").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,6,0,2,0,0,0,1\n\n");
			} else {
				outputString.append("Style: 3D1,Arial,16,").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,").append(outline).append(",").append(shadow).append(",2,0,0,0,1\n");
				outputString.append("Style: 3D2,Arial,16,").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,").append(outline).append(",").append(shadow).append(",2,0,0,0,1\n\n");
			}

			outputString.append("[Events]\n");
			outputString.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n\n");
			output.write(outputString.toString());
			int textPosition = 0;
			while ((line = input.readLine()) != null) {
				if (line.startsWith("[Events]")) {
					line = input.readLine();
					if (line != null && line.startsWith("Format:")) {
						String[] formatPattern = line.split(",");
						int i = 0;
						for (String component : formatPattern) {
							if (component.trim().equals("Text")) {
								textPosition = i;
							}
							i++;
						}
					}
				}

				outputString.setLength(0);
				if (line.startsWith("Dialogue:") && line.contains("Default")) { // TODO: For now convert only Default style. For other styles must be position and font size recalculated
					String[] dialogPattern = line.split(",");
					String text = StringUtils.join(dialogPattern, ",", textPosition, dialogPattern.length);
					Matcher timeMatcher = timePattern.matcher(line);
					if (timeMatcher.find()) {	
						if (isAnaglyph) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D1,,")
							.append(String.format("%04d,", 0))
							.append(String.format("%04d,", 0))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
						} else if (mode3D == Mode3D.ABL) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D1,,")
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,", offset + depth3D))
							.append(String.format("%04d,,", topSubsPositionTb))
							.append(text).append("\n");
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D2,,")
							.append(String.format("%04d,", offset + depth3D))
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
						} else if (mode3D == Mode3D.ABR) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D1,,")
							.append(String.format("%04d,", offset + depth3D))
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,,", topSubsPositionTb))
							.append(text).append("\n");
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D2,,")
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,", offset + depth3D))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
						} else if (mode3D == Mode3D.SBSL) {
							int marginR1 = playResX + offset + depth3D;
							int marginL2 = playResX + offset + depth3D;
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D1,,")
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,", marginR1))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D2,,")
							.append(String.format("%04d,", marginL2))
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
						} else if (mode3D == Mode3D.SBSR) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D1,,")
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,", middleSbs - offset + depth3D))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("3D2,,")
							.append(String.format("%04d,", middleSbs - offset + depth3D))
							.append(String.format("%04d,", offset - depth3D))
							.append(String.format("%04d,,", bottomSubsPosition))
							.append(text).append("\n");
						}
					}
						
					output.write(outputString.toString());
				}
			}
		}

		LOGGER.debug("Subtitles converted to 3DASS format and stored in the file: " + outputSubs.getName());
		output.flush();
		output.close();
		return outputSubs;
	}

	public static String convertColorToAssHexFormat(Color color) {
		String colour = Integer.toHexString(color.getRGB());
		return "&H" + colour.substring(6, 8) + colour.substring(4, 6) + colour.substring(2, 4);
	}
}



