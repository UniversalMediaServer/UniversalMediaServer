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
package net.pms.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaInfo.Mode3D;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.FileUtil.BufferedReaderDetectCharsetResult;
import static net.pms.util.Constants.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
			// Central / Eastern Europe
			put(CHARSET_WINDOWS_1250, "cp1250");
			put(CHARSET_ISO_8859_2, "ISO-8859-2");
			// Western Europe
			put(CHARSET_WINDOWS_1252, "cp1252");
			put(CHARSET_ISO_8859_1, "ISO-8859-1");
			// Greek
			put(CHARSET_WINDOWS_1253, "cp1253");
			put(CHARSET_ISO_8859_7, "ISO-8859-7");
			// Turkish
			put(CHARSET_WINDOWS_1254, "cp1254");
			put(CHARSET_ISO_8859_9, "ISO-8859-9");
			// Hebrew
			put(CHARSET_WINDOWS_1255, "cp1255");
			put(CHARSET_ISO_8859_8, "ISO-8859-8");
			// Arabic
			put(CHARSET_WINDOWS_1256, "cp1256");
			put(CHARSET_ISO_8859_6, "ISO-8859-6");
			// Chinese
			put(CHARSET_ISO_2022_CN, "ISO-2022-CN");
			put(CHARSET_BIG5, "enca:zh:big5");
			put(CHARSET_GB18030, "enca:zh:big5");
			put(CHARSET_EUC_TW, "enca:zh:big5");
			// Korean
			put(CHARSET_ISO_2022_KR, "cp949");
			put(CHARSET_EUC_KR, "euc-kr");
			// Japanese
			put(CHARSET_ISO_2022_JP, "ISO-2022-JP");
			put(CHARSET_EUC_JP, "euc-jp");
			put(CHARSET_SHIFT_JIS, "shift-jis");
			// Thai
			put(CHARSET_WINDOWS_874, "MS874");
			put(CHARSET_ISO_8859_11, "ISO-8859-11");
			put(CHARSET_TIS_620, "TIS-620");
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
		if (isBlank(dlnaMediaSubtitle.getSubCharacterSet())) {
			return null;
		}
		return fileCharsetToMencoderSubcpOptionMap.get(dlnaMediaSubtitle.getSubCharacterSet());
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
		String cp = configuration.getSubtitlesCodepage();
		final boolean isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM = isNotBlank(cp) && Charset.isSupported(cp);

		try (
			BufferedReader reader = isSubtitlesCodepageForcedInConfigurationAndSupportedByJVM ?
				new BufferedReader(new InputStreamReader(new FileInputStream(fileToConvert), Charset.forName(cp))) :
				FileUtil.createBufferedReaderDetectCharset(fileToConvert, null).getBufferedReader();
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), StandardCharsets.UTF_8))
		) {
			while ((line = reader.readLine()) != null) {
				output.write(line + "\n");
			}

			output.flush();
			output.close();
		}
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
		File convertedFile = dlna.getMediaSubtitle().getConvertedFile();

		if (convertedFile != null && convertedFile.canRead()) {
			// subs are already converted and exists
			params.sid.setType(SubtitleType.ASS);
			params.sid.setSubCharacterSet(CHARSET_UTF_8);
			return convertedFile;
		}

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

		File converted3DSubs = new File(FileUtil.getFileNameWithoutExtension(convertedSubs.getAbsolutePath()) + "_3D.ass");
		if (convertedSubs.canRead() || converted3DSubs.canRead()) {
			// subs are already converted
			if (applyFontConfig || isEmbeddedSource || is3D) {
				params.sid.setType(SubtitleType.ASS);
				params.sid.setSubCharacterSet(CHARSET_UTF_8);
				if (converted3DSubs.canRead()) {
					convertedSubs = converted3DSubs;
				}
			}

			params.sid.setConvertedFile(convertedSubs);
			dlna.getMediaSubtitle().setConvertedFile(convertedSubs);
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
				params.sid.setSubCharacterSet(CHARSET_UTF_8);
			} catch (IOException ex) {
				params.sid.setSubCharacterSet(null);
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
				params.sid.setSubCharacterSet(CHARSET_UTF_8);
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
		params.sid.setConvertedFile(tempSubs);
		dlna.getMediaSubtitle().setConvertedFile(tempSubs);
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
				params.sid.getSubCharacterSet() != null ?
					// Fall back on the actually detected encoding if we have it.
					// Note: accuracy isn't 100% guaranteed.
					params.sid.getSubCharacterSet() :
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
		try (
			BufferedReaderDetectCharsetResult input = FileUtil.createBufferedReaderDetectCharset(temp, StandardCharsets.UTF_8);
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), input.getCharset()));
		) {
			String line;
			String[] format = null;
			int i;
			boolean playResIsSet = false; // do not apply font size change when video resolution is set
			BufferedReader reader = input.getBufferedReader();
			while ((line = reader.readLine()) != null) {
				outputString.setLength(0);
				if (line.contains("[Script Info]")) {
					outputString.append(line).append("\n");
					output.write(outputString.toString());
					while ((line = reader.readLine()) != null) {
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
								params[i] = configuration.getSubsColor().getASSv4StylesHexValue();
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
				output.flush();
			}
		} finally {
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
		File outputSubs = new File(FileUtil.getFileNameWithoutExtension(tempSubs.getAbsolutePath()) + "_3D.ass");
		StringBuilder outputString = new StringBuilder();
		Charset subsFileCharset = FileUtil.getFileCharset(tempSubs);
		if (subsFileCharset == null) {
			subsFileCharset = StandardCharsets.UTF_8;
		}
		Mode3D mode3D = media.get3DLayout();
		boolean isOU = mode3D == Mode3D.ABL || mode3D == Mode3D.ABR || mode3D == Mode3D.AB2L;
		boolean isSBS = mode3D == Mode3D.SBSL || mode3D == Mode3D.SBSR || mode3D == Mode3D.SBS2L;
		if (mode3D == null) {
			LOGGER.debug("The 3D layout not recognized for the 3D video");
			throw new NullPointerException("The 3D layout not recognized for the 3D video");
		}

		int depth3D = configuration.getDepth3D();
		Pattern timePattern = Pattern.compile("[0-9]:[0-9]{2}:[0-9]{2}.[0-9]{2},[0-9]:[0-9]{2}:[0-9]{2}.[0-9]{2},");
		try (
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(tempSubs), subsFileCharset));
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), StandardCharsets.UTF_8));
		) {
			String line;
			outputString.append("[Script Info]\n");
			outputString.append("ScriptType: v4.00+\n");
			outputString.append("Collisions: Normal\n");
			outputString.append("PlayResX: ").append("384\n");
			outputString.append("PlayResY: ").append("288\n");
			outputString.append("ScaledBorderAndShadow: yes\n");
			outputString.append("PlayDepth: 0\n");
			outputString.append("Timer: 100.0\n");
			outputString.append("WrapStyle: 0\n\n");
			outputString.append("[V4+ Styles]\n");
			outputString.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
			String fontScaleX = "1";
			String fontScaleY = "1";
			if (isOU) {
				fontScaleX = Double.toString(100 * Double.parseDouble(configuration.getAssScale()));
				fontScaleY = Double.toString((100 * Double.parseDouble(configuration.getAssScale())) / 2);
			} else if (isSBS) {
				fontScaleX = Double.toString((100 * Double.parseDouble(configuration.getAssScale())) / 2);
				fontScaleY = Double.toString(100 * Double.parseDouble(configuration.getAssScale()));
			}

			String primaryColour = configuration.getSubsColor().getASSv4StylesHexValue();
			String outline = configuration.getAssOutline();
			String shadow = configuration.getAssShadow();
			outputString.append("Style: Default,Arial,").append("15").append(',').append(primaryColour).append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScaleX).append(',').append(fontScaleY).append(",0,0,1,").append(outline).append(',').append(shadow);
			if (isOU) {
				outputString.append(",2,15,15,15,0\n\n");
			} else if (isSBS) {
				outputString.append(",2,0,0,15,0\n\n");
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
				if (line != null && line.startsWith("Dialogue:") && line.contains("Default")) { // TODO: For now convert only Default style. For other styles must be position and font size recalculated
					String[] dialogPattern = line.split(",");
					String text = StringUtils.join(dialogPattern, ",", textPosition, dialogPattern.length);
					Matcher timeMatcher = timePattern.matcher(line);
					if (timeMatcher.find()) {
						if (isOU) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("Default,,");
							if (depth3D > 0) {
								outputString.append("0000,")
								.append(String.format("%04d,", depth3D));
							} else if (depth3D < 0) {
								outputString.append(String.format("%04d,", -depth3D))
								.append("0000,");
							} else {
								outputString.append("0000,0000,");
							}

							outputString.append(String.format("%04d,,", 159))
							.append(text).append("\n")
							.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("Default,,0000,0000,0000,,")
							.append(text).append("\n");
						} else if (isSBS) {
							outputString.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("Default,,")
							.append("0000,")
							.append(String.format("%04d,", 192 - depth3D))
							.append("0000,,")
							.append(text).append("\n")
							.append("Dialogue: 0,")
							.append(timeMatcher.group())
							.append("Default,,")
							.append(String.format("%04d,", 192 - depth3D))
							.append("0000,0000,,")
							.append(text).append("\n");
						}
					}

					output.write(outputString.toString());
				}
			}
			output.flush();
		}
		LOGGER.debug("Subtitles converted to 3DASS format and stored in the file: \"{}\"", outputSubs.getName());
		tempSubs.deleteOnExit();
		return outputSubs;
	}

	public static void deleteSubs() {
		FileUtils.deleteQuietly(new File(configuration.getDataFile(SUB_DIR)));
	}

	/**
	 * Remove the (HTML) tags: {@code
	 * <b> </b> <i> </i> <u> </u> <s> </s> <font *> </font>
	 * } and any ASS tags <code>
	 * {\*}
	 * </code>
	 * from subtitles file for renderers not capable of showing SubRip tags
	 * correctly. * is used as a wildcard in the definition above.
	 *
	 * @param file the source subtitles
	 * @return InputStream with converted subtitles.
	 */
	public static InputStream removeSubRipTags(File file) throws IOException {
		Pattern pattern = Pattern.compile("\\</?(?:b|i|s|u|font[^\\>]*)\\>|\\{\\\\.*?}|\\\\h|\\\\N");
		try (
			BufferedReaderDetectCharsetResult input = FileUtil.createBufferedReaderDetectCharset(file, null);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(os, input.getCharset())
		) {
			String line;
			BufferedReader reader = input.getBufferedReader();
			while ((line = reader.readLine()) != null) {
				line = pattern.matcher(line).replaceAll("") + "\n";
				writer.write(line);
			}

			writer.flush();
			LOGGER.trace("Removed tags from subtitles file: \"{}\"", file.getName());
			return new ByteArrayInputStream(os.toByteArray());
		}
	}
}