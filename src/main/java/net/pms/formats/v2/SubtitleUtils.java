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

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaInfo.Mode3D;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.util.FileUtil;
import static org.apache.commons.lang3.StringUtils.*;
import static org.mozilla.universalchardet.Constants.*;

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
		String subsFileCharset = FileUtil.getFileCharset(fileToConvert);
		final boolean isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM = isNotBlank(cp) && Charset.isSupported(cp);
		final boolean isSubtitlesCodepageAutoDetectedAndSupportedByJVM = isNotBlank(subsFileCharset) && Charset.isSupported(subsFileCharset);
		if (isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToConvert), Charset.forName(cp)));
		} else if (isSubtitlesCodepageAutoDetectedAndSupportedByJVM) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToConvert), Charset.forName(subsFileCharset)));
		} else {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToConvert)));
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
					line = line.substring(line.indexOf("}") + 1);
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
	public static File convertASSToASS3D(File tempSubs, DLNAMediaInfo media) throws IOException, NullPointerException {
		File outputSubs = new File(FilenameUtils.getFullPath(tempSubs.getPath()), FilenameUtils.getBaseName(tempSubs.getName()) + "_3D.ass");
		StringBuilder outputString = new StringBuilder();
		String subsFileCharset = FileUtil.getFileCharset(tempSubs);
		BufferedWriter output;
		// First try to calculate subtitles position and depth
		int depth3Dsbs = (int) (((media.getWidth() / 2) / 100) * Double.valueOf(configuration.getDepth3D())); 
		int depth3Dtb = (int) ((media.getWidth() / 100) * Double.valueOf(configuration.getDepth3D()));
		// Max depth - 5% ... + 5%
		int sbsOffset = ((media.getWidth() / 2) / 100) * 5;
		int tbOffset = (media.getWidth() / 100) * 5;
		int bottomSubsPositionSbs = (int) (media.getHeight() / 100 * Double.valueOf(configuration.get3DbottomSubsPosition()));
		int bottomSubsPositionTb = (int) ((media.getHeight() / 2) / 100 * Double.valueOf(configuration.get3DbottomSubsPosition()));
		int topSubsPosition = (media.getHeight() / 2) + bottomSubsPositionTb;
		int middle = media.getWidth() / 2;
		Mode3D mode3D = media.get3DLayout();
		if (mode3D == null) {
			LOGGER.debug("The 3D layout not recognized for the 3D video");
			throw new NullPointerException("The 3D layout not recognized for the 3D video");
		}
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
			String fontSize = "16";
			if (mode3D == Mode3D.SBSLF || mode3D == Mode3D.SBSRF) {
				fontSize = Integer.toString((int) ((16 * media.getHeight() / 288 * Double.parseDouble(configuration.getFontSize3D()))));
			} else {
				fontSize = Integer.toString((int) ((16 * ((media.getHeight() / 2) / 288) * Double.parseDouble(configuration.getFontSize3D()))));
			}

			if (mode3D == Mode3D.SBSLF || mode3D == Mode3D.SBSRF) { // TODO: recalculate font size accordingly to the video size
				outputString.append("Style: 3D1,Cube Modern Rounded Thin,").append(fontSize).append(",&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,6,0,2,0,0,0,1\n");
				outputString.append("Style: 3D2,Cube Modern Rounded Thin,").append(fontSize).append(",&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,6,0,2,0,0,0,1\n\n");
			} else {
				outputString.append("Style: 3D1,Cube Modern Rounded Short,").append(fontSize).append(",&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,6,0,2,0,0,0,1\n");
				outputString.append("Style: 3D2,Cube Modern Rounded Short,").append(fontSize).append(",&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,6,0,2,0,0,0,1\n\n");
			}

			outputString.append("[Events]\n");
			outputString.append("Format: Layer, Start, End, Style, Name, ScaleX, ScaleY, MarginL, MarginR, MarginV, Effect, Text\n\n");
			output.write(outputString.toString());
			int textPosition = 0;
			while ((line = input.readLine()) != null) {
				if (line.startsWith("[Events]")) {
					line = input.readLine();
					if (line.startsWith("Format:")) {
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
						if (mode3D == Mode3D.TBLF) {
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D1,,100,100,").append(String.format("%04d,", tbOffset - depth3Dtb)).append(String.format("%04d,", tbOffset + depth3Dtb)).append(String.format("%04d,,", topSubsPosition)).append(text).append("\n");
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D2,,100,100,").append(String.format("%04d,", tbOffset + depth3Dtb)).append(String.format("%04d,", tbOffset - depth3Dtb)).append(String.format("%04d,,", bottomSubsPositionTb)).append(text).append("\n");
						} else if (mode3D == Mode3D.TBRF) {
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D1,,100,100,").append(String.format("%04d,", tbOffset + depth3Dtb)).append(String.format("%04d,", tbOffset - depth3Dtb)).append(String.format("%04d,,", topSubsPosition)).append(text).append("\n");
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D2,,100,100,").append(String.format("%04d,", tbOffset - depth3Dtb)).append(String.format("%04d,", tbOffset + depth3Dtb)).append(String.format("%04d,,", bottomSubsPositionTb)).append(text).append("\n");
						} else if (mode3D == Mode3D.SBSLF) {
							int marginR1 = (media.getWidth() / 2) + sbsOffset + depth3Dsbs;
							int marginL2 = (media.getWidth() / 2) + sbsOffset + depth3Dsbs;
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D1,,100,100,").append(String.format("%04d,", sbsOffset - depth3Dsbs)).append(String.format("%04d,", marginR1)).append(String.format("%04d,,", bottomSubsPositionSbs)).append(text).append("\n");
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D2,,100,100,").append(String.format("%04d,", marginL2)).append(String.format("%04d,", sbsOffset - depth3Dsbs)).append(String.format("%04d,,", bottomSubsPositionSbs)).append(text).append("\n");
						} else if (mode3D == Mode3D.SBSRF) {
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D1,,100,100,").append(String.format("%04d,", sbsOffset - depth3Dsbs)).append(String.format("%04d,", middle - sbsOffset + depth3Dsbs)).append(String.format("%04d,,", bottomSubsPositionSbs)).append(text).append("\n");
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("3D2,,100,100,").append(String.format("%04d,", middle - sbsOffset + depth3Dsbs)).append(String.format("%04d,", sbsOffset - depth3Dsbs)).append(String.format("%04d,,", bottomSubsPositionSbs)).append(text).append("\n");
						}

						output.write(outputString.toString());
					}
				}
			}
		}

		output.flush();
		output.close();
		return outputSubs;
	}
}
