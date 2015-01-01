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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAMediaInfo.Mode3D;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
	private static final String SUB_DIR = "subs";

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
	 * Extracts embedded subtitles from video to file in SSA/ASS format, converts external SRT
	 * subtitles file to SSA/ASS format and applies fontconfig setting to that converted file
	 * and applies timeseeking when required.
	 *
	 * @param dlna DLNAResource
	 * @param media DLNAMediaInfo
	 * @param params Output parameters
	 * @param configuration
	 * @return Converted subtitle file
	 * @throws IOException
	 */
	public static File getSubtitles(DLNAResource dlna, DLNAMediaInfo media, OutputParams params, PmsConfiguration configuration, SubtitleType subtitleType) throws IOException {
		if (media == null || params.sid.getId() == -1 || !params.sid.getType().isText()) {
			return null;
		}

		String dir = configuration.getDataFile(SUB_DIR);
		File subsPath = new File(dir);
		if (!subsPath.exists()) {
			subsPath.mkdirs();
		}

		boolean applyFontConfig = configuration.isFFmpegFontConfig();
		boolean isEmbeddedSource = params.sid.getId() < 100;
		boolean is3D = media.is3d() && !media.stereoscopyIsAnaglyph();

		String filename = isEmbeddedSource ?
			dlna.getSystemName() : params.sid.getExternalFile().getAbsolutePath();

		String basename;

		long modId = new File(filename).lastModified();
		if (modId != 0) {
			// We have a real file
			basename = FilenameUtils.getBaseName(filename);
		} else {
			// It's something else, e.g. a url or psuedo-url without meaningful
			// lastmodified and (maybe) basename characteristics.
			basename = dlna.getName().replaceAll("[<>:\"\\\\/|?*+\\[\\]\n\r ']", "").trim();
			modId = filename.hashCode();
		}

		File convertedSubs;
		if (applyFontConfig || isEmbeddedSource || is3D || params.sid.getType() != subtitleType) {
			convertedSubs = new File(subsPath.getAbsolutePath() + File.separator + basename + "_ID" + params.sid.getId() + "_" + modId + "." + subtitleType.getExtension());
		} else {
			String tmp = params.sid.getExternalFile().getName().replaceAll("[<>:\"\\\\/|?*+\\[\\]\n\r ']", "").trim();
			convertedSubs = new File(subsPath.getAbsolutePath() + File.separator + modId + "_" + tmp);
		}

		if (convertedSubs.canRead()) {
			// subs are already converted
			if (applyFontConfig || isEmbeddedSource || is3D) {
				params.sid.setType(SubtitleType.ASS);
				params.sid.setExternalFileCharacterSet(CHARSET_UTF_8);
				if (is3D) {
					try {
						convertedSubs = convertASSToASS3D(convertedSubs, media, params);
					} catch (IOException | NullPointerException e) {
						LOGGER.debug("Converting to ASS3D format ends with error: " + e);
						return null;
					}
				}
			}

			return convertedSubs;
		}

		boolean isExternalAss = false;
		if (
			params.sid.getType() == SubtitleType.ASS &&
			params.sid.isExternal() &&
			!isEmbeddedSource
		) {
			isExternalAss = true;
		}

		File tempSubs;
		if (
			isExternalAss ||
			(
				!applyFontConfig &&
				!isEmbeddedSource &&
				(params.sid.getType() == subtitleType) &&
				(params.sid.getType() == SubtitleType.SUBRIP || params.sid.getType() == SubtitleType.WEBVTT) &&
				!is3D
			)
		) {
			tempSubs = params.sid.getExternalFile();
		} else {
			tempSubs = convertSubsToSubtitleType(filename, media, params, configuration, subtitleType);
		}

		if (tempSubs == null) {
			return null;
		}

		if (!FileUtil.isFileUTF8(tempSubs)) {
			try {
				tempSubs = applyCodepageConversion(tempSubs, convertedSubs);
				params.sid.setExternalFileCharacterSet(CHARSET_UTF_8);
			} catch (IOException ex) {
				params.sid.setExternalFileCharacterSet(null);
				LOGGER.warn("Exception during external file charset detection.", ex);
			}
		} else {
			FileUtils.copyFile(tempSubs, convertedSubs);
			tempSubs = convertedSubs;
		}

		// Now we're sure we actually have our own modifiable file
		if (
			applyFontConfig &&
			!(
				configuration.isUseEmbeddedSubtitlesStyle() &&
				params.sid.getType() == SubtitleType.ASS
			)
		) {
			try {
				tempSubs = applyFontconfigToASSTempSubsFile(tempSubs, media, configuration);
				params.sid.setExternalFileCharacterSet(CHARSET_UTF_8);
			} catch (IOException e) {
				LOGGER.debug("Applying subs setting ends with error: " + e);
				return null;
			}
		}

		if (is3D) {
			try {
				tempSubs = convertASSToASS3D(tempSubs, media, params);
			} catch (IOException | NullPointerException e) {
				LOGGER.debug("Converting to ASS3D format ends with error: " + e);
				return null;
			}
		}

		if (isEmbeddedSource) {
//			params.sid.setExternalFile(tempSubs);
			params.sid.setType(SubtitleType.ASS);
		}

		PMS.get().addTempFile(tempSubs, 30 * 24 * 3600 * 1000);
		return tempSubs;
	}

	/**
	 * Converts external subtitles or extract embedded subs to the requested subtitle type
	 *
	 * @param fileName subtitles file or video file with embedded subs
	 * @param media
	 * @param params output parameters
	 * @param configuration
	 * @param outputSubtitleType requested subtitle type
	 * @return Converted subtitles file in requested type
	 */
	public static File convertSubsToSubtitleType(String fileName, DLNAMediaInfo media, OutputParams params, PmsConfiguration configuration, SubtitleType outputSubtitleType) {
		if (!params.sid.getType().isText()) {
			return null;
		}
		List<String> cmdList = new ArrayList<>();
		File tempSubsFile;
		cmdList.add(configuration.getFfmpegPath());
		cmdList.add("-y");
		cmdList.add("-loglevel");
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("fatal");
		}

		// Try to specify input encoding if we have a non utf-8 external sub
		if (params.sid.getId() >= 100 && !params.sid.isExternalFileUtf8()) {
			String encoding = isNotBlank(configuration.getSubtitlesCodepage()) ?
					// Prefer the global user-specified encoding if we have one.
					// Note: likely wrong if the file isn't supplied by the user.
					configuration.getSubtitlesCodepage() :
				params.sid.getExternalFileCharacterSet() != null ?
					// Fall back on the actually detected encoding if we have it.
					// Note: accuracy isn't 100% guaranteed.
					params.sid.getExternalFileCharacterSet() :
				null; // Otherwise we're out of luck!
			if (encoding != null) {
				cmdList.add("-sub_charenc");
				cmdList.add(encoding);
			}
		}

		cmdList.add("-i");
		cmdList.add(fileName);

		if (params.sid.isEmbedded()) {
			cmdList.add("-map");
			cmdList.add("0:s:" + (media.getSubtitleTracksList().indexOf(params.sid)));
		}

		try {
			tempSubsFile = new File(configuration.getTempFolder(), FilenameUtils.getBaseName(fileName) + "." + outputSubtitleType.getExtension());
		} catch (IOException e1) {
			LOGGER.debug("Subtitles conversion finished wih error: " + e1);
			return null;
		}
		cmdList.add(tempSubsFile.getAbsolutePath());

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		try {
			pw.join(); // Wait until the conversion is finished
			pw.stopProcess(); // Avoid creating a pipe for this process and messing up with buffer progress bar
		} catch (InterruptedException e) {
			LOGGER.debug("Subtitles conversion finished wih error: " + e);
			return null;
		}

		tempSubsFile.deleteOnExit();
		return tempSubsFile;
	}

	public static File applyFontconfigToASSTempSubsFile(File tempSubs, DLNAMediaInfo media, PmsConfiguration configuration) throws IOException {
		LOGGER.debug("Applying fontconfig to subtitles " + tempSubs.getName());
		File outputSubs = tempSubs;
		StringBuilder outputString = new StringBuilder();
		File temp = new File(configuration.getTempFolder(), tempSubs.getName() + ".tmp");
		FileUtils.copyFile(tempSubs, temp);
		BufferedReader input = FileUtil.bufferedReaderWithCorrectCharset(temp);
		BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), CHARSET_UTF_8));
		try {
			String line;
			String[] format = null;
			int i;
			boolean playResIsSet = false; // do not apply font size change when video resolution is set
			while ((line = input.readLine()) != null) {
				outputString.setLength(0);
				if (line.contains("[Script Info]")) {
					outputString.append(line).append("\n");
					output.write(outputString.toString());
					while ((line = input.readLine()) != null) {
						outputString.setLength(0);
						if (isNotBlank(line)) {
							if (line.contains("PlayResY:") || line.contains("PlayResX:")) {
								playResIsSet = true;
							}
							outputString.append(line).append("\n");
							output.write(outputString.toString());
						} else {
							if (!playResIsSet) {
								outputString.append("PlayResY: ").append(media.getHeight()).append("\n");
								outputString.append("PlayResX: ").append(media.getWidth()).append("\n");
							}
							break;
						}
					}
				}

				if (line != null && line.contains("Format:")) {
					format = line.split(",");
					outputString.append(line).append("\n");
					output.write(outputString.toString());
					continue;
				}

				if (line != null && line.contains("Style: Default")) {
					String[] params = line.split(",");

					for (i = 0; i < format.length; i++) {
						switch (format[i].trim()) {
							case "Fontname":
								if (!configuration.getFont().isEmpty()) {
									params[i] = configuration.getFont();
								}

								break;
							case "Fontsize":
								if (!playResIsSet) {
									params[i] = Integer.toString((int) ((Integer.parseInt(params[i]) * media.getHeight() / (double) 288 * Double.parseDouble(configuration.getAssScale()))));
								} else {
									params[i] = Integer.toString((int) (Integer.parseInt(params[i]) * Double.parseDouble(configuration.getAssScale())));
								}

								break;
							case "PrimaryColour":
								String primaryColour = Integer.toHexString(configuration.getSubsColor());
								params[i] = "&H" + primaryColour.substring(6, 8) + primaryColour.substring(4, 6) + primaryColour.substring(2, 4);
								break;
							case "Outline":
								params[i] = configuration.getAssOutline();
								break;
							case "Shadow":
								params[i] = configuration.getAssShadow();
								break;
							case "MarginV":
								params[i] = configuration.getAssMargin();
								break;
							default:
								break;
						}
					}

					outputString.append(StringUtils.join(params, ",")).append("\n");
					output.write(outputString.toString());
					continue;
				}

				outputString.append(line).append("\n");
				output.write(outputString.toString());
			}
		} finally {
			input.close();
			output.flush();
			output.close();
			temp.deleteOnExit();
		}

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
		if (subsFileCharset == null) {
			subsFileCharset = CHARSET_UTF_8;
		}
		BufferedWriter output;
		Mode3D mode3D = media.get3DLayout();
		if (mode3D == null) {
			LOGGER.debug("The 3D layout not recognized for the 3D video");
			throw new NullPointerException("The 3D layout not recognized for the 3D video");
		}

		int playResX;
		int playResY;
		if (mode3D == Mode3D.OUL || mode3D == Mode3D.OUR) {
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
			String fontSize = Integer.toString((int) (16 * media.getHeight() / (double) 288));
			outputString.append("Style: 3D1,Arial,").append(fontSize).append(",").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,").append(outline).append(",").append(shadow).append(",2,0,0,0,1\n");
			outputString.append("Style: 3D2,Arial,").append(fontSize).append(",").append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScale).append(",").append(fontScale).append(",0,0,1,").append(outline).append(",").append(shadow).append(",2,0,0,0,1\n\n");

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
						if (mode3D == Mode3D.OUL) {
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
						} else if (mode3D == Mode3D.OUR) {
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
		tempSubs.deleteOnExit();
		return outputSubs;
	}

	public static String convertColorToAssHexFormat(Color color) {
		String colour = Integer.toHexString(color.getRGB());
		return "&H" + colour.substring(6, 8) + colour.substring(4, 6) + colour.substring(2, 4);
	}
}
