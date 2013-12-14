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
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.util.FileUtil;
import static org.apache.commons.lang3.StringUtils.*;
import static org.mozilla.universalchardet.Constants.*;

public class SubtitleUtils {
	private final static PmsConfiguration configuration = PMS.getConfiguration();
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
	
	private static int SBS_MIDDLE_LEFT_1920 = 480;
	private static int SBS_MIDDLE_LEFT_3840 = SBS_MIDDLE_LEFT_1920 * 2;
	private static int SBS_MIDDLE_RIGHT_1920 = 1440;
	private static int SBS_MIDDLE_RIGHT_3840 = SBS_MIDDLE_RIGHT_1920 * 2;
	private static int TB_MIDDLE_1920 = 960;
	private static int TB_MIDDLE_3840 = TB_MIDDLE_1920 * 2;
	public enum Mode3D {SBS, TB};

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
	 * Converts ASS/SSA subtitles to 3D ASS/SSA subtitles 
	 *
	 * @param tempSubs Subtitles file to convert
	 * @param mode3D
	 * @param depth3D
	 * @param fullres 
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File convertASSToASS3D(File tempSubs, Mode3D mode3D , int depth3D, boolean fullres) throws IOException {
		File outputSubs = new File(FilenameUtils.getFullPath(tempSubs.getPath()), FilenameUtils.getBaseName(tempSubs.getName()) + ".ass3D");
		StringBuilder outputString = new StringBuilder();
		String subsFileCharset = FileUtil.getFileCharset(tempSubs);
		BufferedWriter output;
		Pattern timePattern = Pattern.compile("Dialogue: [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3},[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3},");
		try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(tempSubs), Charset.forName(subsFileCharset)))) {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), Charset.forName(CHARSET_UTF_8)));
			String line;
			while ((line = input.readLine()) != null) {
				outputString.setLength(0);
				if (line.startsWith("[Script Info]")) {
					outputString.append(line).append("\n");
					output.write(outputString.toString());
					while ((line = input.readLine()) != null) {
						outputString.setLength(0);
						if (!line.isEmpty()) {
							outputString.append(line).append("\n");
							output.write(outputString.toString());
						} else {
							if (!fullres) {
								outputString.append("PlayResX: 1920\n");
								outputString.append("PlayResY: 1080\n");
								output.write(outputString.toString());
							} else {
								if (mode3D != Mode3D.TB) {
									outputString.append("PlayResX: 3840\n");
									outputString.append("PlayResY: 1080\n");
								} else {
									outputString.append("PlayResX: 1920\n");
									outputString.append("PlayResY: 2160\n");
								}
								output.write(outputString.toString());
							}
							break;
						}
					}
				}
				
				Matcher timeMatcher = timePattern.matcher(line);
				if (timeMatcher.find()) {
					if (mode3D != Mode3D.TB) {
						outputString.append(timeMatcher.group(1)).append(timeMatcher.group(2)).append("{\\pos(").append(fullres ? SBS_MIDDLE_LEFT_1920 : SBS_MIDDLE_LEFT_3840 - depth3D).append("1060)}");
						outputString.append(timeMatcher.group(1)).append(timeMatcher.group(2)).append("{\\pos(").append(fullres ? SBS_MIDDLE_RIGHT_1920 : SBS_MIDDLE_RIGHT_3840 + depth3D).append("1060)}");
					} else {
						outputString.append(timeMatcher.group(1)).append(timeMatcher.group(2)).append("{\\pos(").append(fullres ? TB_MIDDLE_1920 : TB_MIDDLE_3840 - depth3D).append("1070)}");
						outputString.append(timeMatcher.group(1)).append(timeMatcher.group(2)).append("{\\pos(").append(fullres ? TB_MIDDLE_1920 : TB_MIDDLE_3840 + depth3D).append("530)}");
					}

					outputString.append("\n"); // continue
					output.write(outputString.toString());
					continue;
				}

				outputString.append(line).append("\n");
				output.write(outputString.toString());
			}
		}

		output.flush();
		output.close();
		return outputSubs;
	}
}
