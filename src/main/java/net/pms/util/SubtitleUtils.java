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
package net.pms.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.FFmpegLogLevels;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.StandardEngineId;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.subtitle.MediaOnDemandSubtitle;
import net.pms.media.video.MediaVideo.Mode3D;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil.BufferedReaderDetectCharsetResult;
import net.pms.util.StringUtil.LetterCase;
import static net.pms.util.Constants.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubtitleUtils {
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleUtils.class);
	private static final long FOLDER_CACHE_EXPIRATION_TIME = 300000; // Milliseconds
	private static final char[] SUBTITLES_UPPER_CASE;
	private static final char[] SUBTITLES_LOWER_CASE;
	private static final File ALTERNATIVE_SUBTITLES_FOLDER;

	/**
	 * This class is not meant to be instantiated.
	 */
	private SubtitleUtils() {
	}

	static {
		String subtitles = "Subtitles";
		SUBTITLES_UPPER_CASE = new char[subtitles.length()];
		SUBTITLES_LOWER_CASE = new char[subtitles.length()];
		for (int i = 0; i < subtitles.length(); i++) {
			SUBTITLES_UPPER_CASE[i] = Character.toUpperCase(subtitles.charAt(i));
			SUBTITLES_LOWER_CASE[i] = Character.toLowerCase(subtitles.charAt(i));
		}

		if (PMS.getConfiguration() == null || isBlank(PMS.getConfiguration().getAlternateSubtitlesFolder())) {
			ALTERNATIVE_SUBTITLES_FOLDER = null;
		} else {
			File alternativeFolder = new File(PMS.getConfiguration().getAlternateSubtitlesFolder());
			if (alternativeFolder.isAbsolute()) {
				try {
					if (!new FilePermissions(alternativeFolder).isBrowsable()) {
						alternativeFolder = null;
						LOGGER.error("Ignoring alternative subtitles folder \"{}\" because of lacking permissions", alternativeFolder);
					}
				} catch (FileNotFoundException e) {
					alternativeFolder = null;
					LOGGER.error("Alternative subtitles folder \"{}\" not found", alternativeFolder);
				}
				if (alternativeFolder != null && !alternativeFolder.isDirectory()) {
					alternativeFolder = null;
					LOGGER.error("Alternative subtitles folder \"{}\" isn't a folder", alternativeFolder);
				}
				ALTERNATIVE_SUBTITLES_FOLDER = alternativeFolder;
			} else {
				ALTERNATIVE_SUBTITLES_FOLDER = alternativeFolder;
			}
		}
	}

	private static final Map<String, String> FILE_CHARSET_TO_MENCODER_SUBCP_OPTION_MAP = new HashMap<String, String>() {

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
	 * @param dlnaMediaSubtitle MediaSubtitle with external subtitles file.
	 * @return value for mencoder's -subcp option or null if can't determine.
	 */
	public static String getSubCpOptionForMencoder(MediaSubtitle dlnaMediaSubtitle) {
		if (dlnaMediaSubtitle == null) {
			throw new NullPointerException("dlnaMediaSubtitle can't be null.");
		}
		if (isBlank(dlnaMediaSubtitle.getSubCharacterSet())) {
			return null;
		}
		return FILE_CHARSET_TO_MENCODER_SUBCP_OPTION_MAP.get(dlnaMediaSubtitle.getSubCharacterSet());
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
		String cp = CONFIGURATION.getSubtitlesCodepage();
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
	 * Extracts embedded subtitles from video to file in SSA/ASS format,
	 * converts external SRT subtitles file to SSA/ASS format and applies
	 * fontconfig setting to that converted file and applies timeseeking when
	 * required.
	 *
	 * @param dlna DLNAResource
	 * @param media MediaInfo
	 * @param params Output parameters
	 * @param configuration
	 * @param subtitleType
	 * @return Converted subtitle file
	 * @throws IOException
	 */
	public static File getSubtitles(
		DLNAResource dlna,
		MediaInfo media,
		OutputParams params,
		UmsConfiguration configuration,
		SubtitleType subtitleType
	) throws IOException {
		if (
			media == null ||
			params.getSid() == null ||
			params.getSid().getId() == MediaLang.DUMMY_ID ||
			!params.getSid().getType().isText()
		) {
			return null;
		}

		String dir = configuration.getDataFile(SUB_DIR);
		File subsPath = new File(dir);
		if (!subsPath.exists()) {
			if (!subsPath.mkdirs()) {
				LOGGER.error("Could not create subtitles conversion folder \"{}\" - subtitles operation aborted!", dir);
				return null;
			}
		}

		if (params.getSid().isExternal() && params.getSid().getExternalFile() == null) {
			// This happens when for example OpenSubtitles fail to download
			return null;
		}

		boolean applyFontConfig = configuration.isFFmpegFontConfig();
		boolean isEmbeddedSource = params.getSid().isEmbedded();
		boolean is3D = media.is3d() && !media.stereoscopyIsAnaglyph();
		File convertedFile = params.getSid().getConvertedFile();

		if (convertedFile != null && convertedFile.canRead()) {
			// subs are already converted and exists
			params.getSid().setType(SubtitleType.ASS);
			params.getSid().setSubCharacterSet(CHARSET_UTF_8);
			return convertedFile;
		}

		String filename = isEmbeddedSource ? dlna.getSystemName() : params.getSid().getExternalFile().getAbsolutePath();

		String basename;

		long modId = new File(filename).lastModified();
		if (modId != 0) {
			// We have a real file
			basename = FilenameUtils.getBaseName(filename).replaceAll("[<>:\"\\\\/|?*+\\[\\]\n\r ']", "").trim();
		} else {
			// It's something else, e.g. a url or psuedo-url without meaningful
			// lastmodified and (maybe) basename characteristics.
			basename = dlna.getName().replaceAll("[<>:\"\\\\/|?*+\\[\\]\n\r ']", "").trim();
			modId = filename.hashCode();
		}

		File convertedSubs;
		StringBuilder nameBuilder = new StringBuilder(subsPath.getAbsolutePath());
		nameBuilder.append(File.separator).append(basename);
		if (isEmbeddedSource) {
			nameBuilder.append("_ID").append(params.getSid().getId());
		}
		if (applyFontConfig) {
			nameBuilder.append("_FB");
		}
		if (is3D) {
			nameBuilder.append("_3D");
		}
		nameBuilder.append("_").append(modId);
		String extension;
		if (subtitleType != null && isNotBlank(subtitleType.getExtension())) {
			extension = subtitleType.getExtension();
		} else {
			extension = FileUtil.getExtension(basename);
		}
		if (isNotBlank(extension)) {
			nameBuilder.append(".").append(extension);
		}
		convertedSubs = new File(nameBuilder.toString());

		File converted3DSubs = new File(FileUtil.getFileNameWithoutExtension(convertedSubs.getAbsolutePath()) + "_3D.ass");
		if (convertedSubs.canRead() || converted3DSubs.canRead()) {
			// subs are already converted
			if (applyFontConfig || isEmbeddedSource || is3D) {
				params.getSid().setType(SubtitleType.ASS);
				params.getSid().setSubCharacterSet(CHARSET_UTF_8);
				if (converted3DSubs.canRead()) {
					convertedSubs = converted3DSubs;
				}
			}

			params.getSid().setConvertedFile(convertedSubs);
			return convertedSubs;
		}

		boolean isExternalAss = false;
		if (params.getSid().getType() == SubtitleType.ASS && params.getSid().isExternal() && !isEmbeddedSource) {
			isExternalAss = true;
		}

		File tempSubs;
		if (
			isExternalAss ||
			(
				!applyFontConfig &&
				!isEmbeddedSource &&
				(params.getSid().getType() == subtitleType) &&
				(params.getSid().getType() == SubtitleType.SUBRIP || params.getSid().getType() == SubtitleType.WEBVTT) &&
				!is3D
			)
		) {
			tempSubs = params.getSid().getExternalFile();
		} else {
			tempSubs = convertSubsToSubtitleType(filename, media, params, configuration, subtitleType);
		}

		if (tempSubs == null) {
			return null;
		}

		if (!FileUtil.isFileUTF8(tempSubs)) {
			try {
				tempSubs = applyCodepageConversion(tempSubs, convertedSubs);
				params.getSid().setSubCharacterSet(CHARSET_UTF_8);
			} catch (IOException ex) {
				params.getSid().setSubCharacterSet(null);
				LOGGER.warn("Exception during external file charset detection.", ex);
			}
		} else {
			FileUtils.copyFile(tempSubs, convertedSubs);
			tempSubs = convertedSubs;
		}

		// Now we're sure we actually have our own modifiable file
		if (applyFontConfig && !(configuration.isUseEmbeddedSubtitlesStyle() && params.getSid().getType() == SubtitleType.ASS)) {
			try {
				tempSubs = applyFontconfigToASSTempSubsFile(tempSubs, media, configuration);
				params.getSid().setSubCharacterSet(CHARSET_UTF_8);
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
			params.getSid().setType(SubtitleType.ASS);
		}

		PMS.get().addTempFile(tempSubs, 30 * 24 * 3600 * 1000);
		params.getSid().setConvertedFile(tempSubs);
		return tempSubs;
	}

	/**
	 * Converts external subtitles or extract embedded subs to the requested
	 * subtitle type
	 *
	 * @param fileName subtitles file or video file with embedded subs
	 * @param media
	 * @param params output parameters
	 * @param configuration
	 * @param outputSubtitleType requested subtitle type
	 * @return Converted subtitles file in requested type
	 */
	public static File convertSubsToSubtitleType(
		String fileName,
		MediaInfo media,
		OutputParams params,
		UmsConfiguration configuration,
		SubtitleType outputSubtitleType
	) {
		if (!params.getSid().getType().isText()) {
			return null;
		}
		List<String> cmdList = new ArrayList<>();
		File tempSubsFile;
		cmdList.add(EngineFactory.getEngineExecutable(StandardEngineId.FFMPEG_VIDEO));
		cmdList.add("-y");
		cmdList.add("-loglevel");
		FFmpegLogLevels askedLogLevel = FFmpegLogLevels.valueOfLabel(configuration.getFFmpegLoggingLevel());
		if (LOGGER.isTraceEnabled()) {
			// Set -loglevel in accordance with LOGGER setting
			if (FFmpegLogLevels.INFO.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("info");
			} else {
				cmdList.add(askedLogLevel.label);
			}
		} else {
			if (FFmpegLogLevels.FATAL.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("fatal");
			} else {
				cmdList.add(askedLogLevel.label);
			}
		}

		// Try to specify input encoding if we have a non utf-8 external sub
		if (params.getSid().isExternal() && !params.getSid().isExternalFileUtf8()) {
			String encoding = isNotBlank(configuration.getSubtitlesCodepage()) ?
			// Prefer the global user-specified encoding if we have one.
			// Note: likely wrong if the file isn't supplied by the user.
				configuration.getSubtitlesCodepage() : params.getSid().getSubCharacterSet() != null ?
				// Fall back on the actually detected encoding if we have it.
				// Note: accuracy isn't 100% guaranteed.
					params.getSid().getSubCharacterSet() : null; // Otherwise we're out of luck!
			if (encoding != null) {
				cmdList.add("-sub_charenc");
				cmdList.add(encoding);
			}
		}

		cmdList.add("-i");
		cmdList.add(fileName);

		if (params.getSid().isEmbedded()) {
			cmdList.add("-map");
			cmdList.add("0:s:" + (media.getSubtitlesTracks().indexOf(params.getSid())));
		}

		try {
			tempSubsFile = new File(
				CONFIGURATION.getTempFolder(),
				FilenameUtils.getBaseName(fileName) + "." + outputSubtitleType.getExtension()
			);
		} catch (IOException e1) {
			LOGGER.debug("Subtitles conversion finished with error: " + e1);
			return null;
		}
		cmdList.add(tempSubsFile.getAbsolutePath());

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		try {
			pw.join(); // Wait until the conversion is finished
			// Avoid creating a pipe for this process and messing up with buffer progress bar
			pw.stopProcess();
		} catch (InterruptedException e) {
			LOGGER.debug("Subtitles conversion finished with error: " + e);
			Thread.currentThread().interrupt();
			return null;
		}

		tempSubsFile.deleteOnExit();
		return tempSubsFile;
	}

	public static File applyFontconfigToASSTempSubsFile(
		File tempSubs,
		MediaInfo media,
		UmsConfiguration configuration
	) throws IOException {
		LOGGER.debug("Applying fontconfig to subtitles " + tempSubs.getName());
		File outputSubs = tempSubs;
		StringBuilder outputString = new StringBuilder();
		File temp = new File(CONFIGURATION.getTempFolder(), tempSubs.getName() + ".tmp");
		FileUtils.copyFile(tempSubs, temp);
		try (
			BufferedReaderDetectCharsetResult input = FileUtil.createBufferedReaderDetectCharset(temp, StandardCharsets.UTF_8);
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputSubs), input.getCharset()));
		) {
			String line;
			String[] format = null;
			int i;
			// do not apply font size change when video resolution is set
			boolean playResIsSet = false;
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
							case "Fontname" -> {
								if (!configuration.getFont().isEmpty()) {
									params[i] = configuration.getFont();
								}
							}
							case "Fontsize" -> {
								if (!playResIsSet) {
									params[i] = Integer.toString((int) (Integer.parseInt(params[i]) * media.getHeight() / (double) 288 *
											Double.parseDouble(configuration.getAssScale())));
								} else {
									params[i] = Integer
											.toString((int) (Integer.parseInt(params[i]) * Double.parseDouble(configuration.getAssScale())));
								}
							}
							case "PrimaryColour" -> {
								params[i] = configuration.getSubsColor().getASSv4StylesHexValue();
							}
							case "Outline" -> {
								params[i] = configuration.getAssOutline();
							}
							case "Shadow" -> {
								params[i] = configuration.getAssShadow();
							}
							case "MarginV" -> {
								params[i] = configuration.getAssMargin();
							}
							default -> {
								//nothing to do
							}
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
	 * Converts ASS/SSA subtitles to 3D ASS/SSA subtitles.Based on
 https://bitbucket.org/r3pek/srt2ass3d
	 *
	 * @param tempSubs Subtitles file to convert
	 * @param media Information about video
	 * @param params
	 * @return Converted subtitles file
	 * @throws IOException
	 */
	public static File convertASSToASS3D(File tempSubs, MediaInfo media, OutputParams params) throws IOException, NullPointerException {
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

		int depth3D = CONFIGURATION.getDepth3D();
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
			outputString.append(
				"Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
			String fontScaleX = "1";
			String fontScaleY = "1";
			if (isOU) {
				fontScaleX = Double.toString(100 * Double.parseDouble(CONFIGURATION.getAssScale()));
				fontScaleY = Double.toString((100 * Double.parseDouble(CONFIGURATION.getAssScale())) / 2);
			} else if (isSBS) {
				fontScaleX = Double.toString((100 * Double.parseDouble(CONFIGURATION.getAssScale())) / 2);
				fontScaleY = Double.toString(100 * Double.parseDouble(CONFIGURATION.getAssScale()));
			}

			String primaryColour = CONFIGURATION.getSubsColor().getASSv4StylesHexValue();
			String outline = CONFIGURATION.getAssOutline();
			String shadow = CONFIGURATION.getAssShadow();
			outputString.append("Style: Default,Arial,").append("15").append(',').append(primaryColour)
				.append(",&H000000FF,&H00000000,&H00000000,0,0,0,0,").append(fontScaleX).append(',').append(fontScaleY).append(",0,0,1,")
				.append(outline).append(',').append(shadow);
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
				// TODO: For now convert only Default style. For other styles must be position and font size recalculated.
				if (line != null && line.startsWith("Dialogue:") && line.contains("Default")) {
					String[] dialogPattern = line.split(",");
					String text = StringUtils.join(dialogPattern, ",", textPosition, dialogPattern.length);
					Matcher timeMatcher = timePattern.matcher(line);
					if (timeMatcher.find()) {
						if (isOU) {
							outputString.append("Dialogue: 0,").append(timeMatcher.group()).append("Default,,");
							if (depth3D > 0) {
								outputString.append("0000,").append(String.format("%04d,", depth3D));
							} else if (depth3D < 0) {
								outputString.append(String.format("%04d,", -depth3D)).append("0000,");
							} else {
								outputString.append("0000,0000,");
							}

							outputString
								.append(String.format("%04d,,", 159))
								.append(text).append("\n")
								.append("Dialogue: 0,")
								.append(timeMatcher.group())
								.append("Default,,0000,0000,0000,,")
								.append(text).append("\n");
						} else if (isSBS) {
							outputString
								.append("Dialogue: 0,")
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
		FileUtils.deleteQuietly(new File(CONFIGURATION.getDataFile(SUB_DIR)));
	}

	/**
	 * Remove the (HTML) tags: {@code
	 * <b> </b> <i> </i> <u> </u> <s> </s> <font *> </font>
	 * } and any ASS tags <code>
	 * {\*}
	 * </code> from subtitles file for renderers not capable of showing SubRip
	 * tags correctly. * is used as a wildcard in the definition above.
	 *
	 * @param file the source subtitles
	 * @return InputStream with converted subtitles.
	 * @throws java.io.IOException
	 */
	public static InputStream removeSubRipTags(File file) throws IOException {
		if (file == null) {
			return null;
		}
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

	private static final HashMap<File, CacheFolder> FOLDER_CACHE = new HashMap<>();

	private static class CacheFolder {

		private File[] items;
		private final long birth;
		private boolean populated;

		public CacheFolder() {
			birth = System.currentTimeMillis();
		}

		public boolean isPopulated() {
			return populated;
		}

		public long getBirth() {
			return birth;
		}

		public File[] getItems() {
			if (!populated) {
				throw new IllegalStateException("Instance hasn't been populated yet");
			}
			return items;
		}

		public void setItems(List<File> items) {
			setItems(items == null ? null : items.toArray(File[]::new));
		}

		public void setItems(File[] items) {
			populated = true;
			if (items == null) {
				this.items = new File[0];
			} else {
				this.items = items;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append(" [Age=");
			sb.append((System.currentTimeMillis() - birth) / 1000).append(" s");
			sb.append(", Populated=").append(populated ? "Yes" : "No");
			if (items == null || items.length == 0) {
				sb.append(", Empty");
			} else {
				sb.append(", Items: ");
				boolean first = true;
				for (File item : items) {
					if (!first) {
						sb.append(", ");
					}
					sb.append(item.getName());
					first = false;
				}
			}
			sb.append("]");
			return sb.toString();
		}
	}

	/**
	 * Evaluates if the given combination of folder and name represents a
	 * subtitles subfolder.
	 *
	 * @param folder the folder in which this file or folder is located.
	 * @param name the name of the file or folder.
	 * @return The {@link File} representing the subfolder if the name match a
	 *         subtitles subfolder, exists and is a folder, {@code null}
	 *         otherwise.
	 */
	private static File isSubtitlesFolder(File folder, CharSequence name) {
		if (folder == null || name == null) {
			return null;
		}
		if (name.length() == 4 || name.length() == SUBTITLES_LOWER_CASE.length) {
			int lastIdx = name.length() - 1;
			for (int i = 0; i <= lastIdx; i++) {
				char c = name.charAt(i);
				if (i == lastIdx && (c == SUBTITLES_LOWER_CASE[0] || c == SUBTITLES_UPPER_CASE[0])) {
					File subsFolder = new File(folder, name.toString());
					return subsFolder.isDirectory() ? subsFolder : null;
				}
				if (c != SUBTITLES_LOWER_CASE[i] && c != SUBTITLES_UPPER_CASE[i]) {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Evaluates if the specified {@link File} is considered a subtitles file
	 * according to the specified {@link Set} of supported extensions.
	 *
	 * @param file the {@link File} to evaluate.
	 * @param supportedExtensions the {@link Set}of supported subtitles
	 *            extensions.
	 * @return {@code true} if {@code file} is considered a subtitles file,
	 *         {@code false} otherwise.
	 */
	private static boolean isSubtitlesFile(File file, Set<String> supportedExtensions) {
		String extension = FileUtil.getExtension(file.getPath(), LetterCase.LOWER, Locale.ROOT);
		if ("sub".equals(extension)) {
			// Avoid microdvd/vobsub confusion by ignoring sub+idx pairs here
			// since
			// they'll come in unambiguously as vobsub via the idx file anyway
			return FileUtil.replaceExtension(file, "idx", true, true) == null;
		}
		return supportedExtensions.contains(extension);
	}

	/**
	 * Scans for and registers external subtitles for the specified file by
	 * creating {@link MediaSubtitle} instances and attaching them to the
	 * specified {@link MediaInfo} instance.
	 * <p>
	 * A folder cache is used for performance optimization, and the parent
	 * folder of {@code file}, any "subs" or "subtitles" (case insensitive)
	 * subfolders of this folder and an alternative subtitles folder (if
	 * configured) will be scanned for matching subtitles files. Already
	 * "registered" files (files that already has a corresponding
	 * {@link MediaSubtitle} instance in {@code media}) will not be
	 * re-parsed.
	 * <p>
	 * If {@code forceRefresh} is {@code true}, the content of the folder cache
	 * will be ignored and the folder content will be reacquired from disk. The
	 * folder cache will be updated with the fresh results.
	 *
	 * @param file the {@link File} for which to scan for external subtitles
	 *            files. It does not have to point to an existing file, the
	 *            parent folder will be used as a scan destination and the name
	 *            will be used for subtitles file name comparison.
	 * @param media the {@link MediaInfo} to add the resulting
	 *            {@link MediaSubtitle} instances to.
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 */
	public static void searchAndAttachExternalSubtitles(File file, MediaInfo media, boolean forceRefresh) {
		if (file == null || media == null) {
			return;
		}
		File subFolder;
		if (file.isAbsolute()) {
			subFolder = file.getParentFile();
		} else {
			try {
				subFolder = file.getCanonicalFile().getParentFile();
			} catch (IOException e) {
				LOGGER.error("Could not find the folder for \"{}\" when looking for external subtitles", file);
				return;
			}
		}

		if (subFolder == null) {
			return;
		}

		LOGGER.trace("Searching for external subtitles for {}", file.getName());

		ArrayList<File> folders = new ArrayList<>();
		if (subFolder.isDirectory()) {
			folders.add(subFolder);
		}

		if (ALTERNATIVE_SUBTITLES_FOLDER != null) {
			if (ALTERNATIVE_SUBTITLES_FOLDER.isAbsolute()) {
				folders.add(ALTERNATIVE_SUBTITLES_FOLDER);
			} else {
				File tmpFolder = new File(subFolder, ALTERNATIVE_SUBTITLES_FOLDER.toString());
				if (tmpFolder.isDirectory()) {
					folders.add(tmpFolder);
				}
			}
		}

		if (folders.isEmpty()) {
			LOGGER.trace("There are no folders to search for subtitles for {}", file.getName());
			return;
		}

		final Set<String> supportedFileExtensions = SubtitleType.getSupportedFileExtensions();

		boolean cleaned = false;
		List<File> folderSubtitles = new ArrayList<>();
		for (File folder : folders) {
			CacheFolder cacheFolder = null;
			synchronized (FOLDER_CACHE) {
				// Clean cache for expired entries and fetch or insert the entry
				// for the folder under examination
				if (cleaned) {
					if (forceRefresh) {
						FOLDER_CACHE.remove(folder);
					}
					cacheFolder = FOLDER_CACHE.get(folder);
				} else {
					long earliestBirth = System.currentTimeMillis() - FOLDER_CACHE_EXPIRATION_TIME;
					for (Iterator<Entry<File, CacheFolder>> iterator = FOLDER_CACHE.entrySet().iterator(); iterator.hasNext();) {
						Entry<File, CacheFolder> entry = iterator.next();
						if (entry.getValue().getBirth() < earliestBirth) {
							iterator.remove();
						} else if (folder.equals(entry.getKey())) {
							if (forceRefresh) {
								iterator.remove();
							} else {
								cacheFolder = entry.getValue();
							}
						}
					}
					cleaned = true;
				}
				if (cacheFolder == null) {
					cacheFolder = new CacheFolder();
					FOLDER_CACHE.put(folder, cacheFolder);
				}
			}

			// Populate the CacheFolder if it isn't already and get the files
			synchronized (cacheFolder) {
				if (!cacheFolder.isPopulated()) {
					List<File> folderSubtitlesList = new ArrayList<>();
					String[] folderContent = folder.list();
					if (folderContent != null && folderContent.length > 0) {
						for (String fileNameEntry : folderContent) {
							File fileEntry = subFolder.equals(folder) ? isSubtitlesFolder(folder, fileNameEntry) : null;
							if (fileEntry != null) {
								// Subtitles subfolder
								String[] subsFolderContent = fileEntry.list();
								if (subsFolderContent != null && subsFolderContent.length > 0) {
									for (String subsFileNameEntry : subsFolderContent) {
										File subsFileEntry = new File(fileEntry, subsFileNameEntry);
										if (
											isSubtitlesFile(subsFileEntry, supportedFileExtensions) &&
											subsFileEntry.isFile() &&
											!subsFileEntry.isHidden()
										) {
											folderSubtitlesList.add(subsFileEntry);
										}
									}
								}
								continue;
							}
							fileEntry = new File(folder, fileNameEntry);
							if (isSubtitlesFile(fileEntry, supportedFileExtensions) && fileEntry.isFile() && !fileEntry.isHidden()) {
								folderSubtitlesList.add(fileEntry);
							}
						}
					}
					cacheFolder.setItems(folderSubtitlesList);
					folderSubtitles.addAll(folderSubtitlesList);
				} else {
					folderSubtitles.addAll(Arrays.asList(cacheFolder.getItems()));
				}
			}
		}

		// Find already parsed subtitles
		HashSet<File> existingSubtitles = new HashSet<>();
		for (MediaSubtitle subtitle : media.getSubtitlesTracks()) {
			if (!(subtitle instanceof MediaOnDemandSubtitle) && subtitle.getExternalFile() != null) {
				existingSubtitles.add(subtitle.getExternalFile());
			}
		}

		// Parse subtitles that are not in the existing list
		String baseFileName = FileUtil.getFileNameWithoutExtension(file.getName()).toLowerCase(Locale.ROOT);
		for (File subtitlesFile : folderSubtitles) {
			if (existingSubtitles.contains(subtitlesFile)) {
				continue;
			}

			String subtitlesName = subtitlesFile.getName();
			String subtitlesNameLower = subtitlesName.toLowerCase(Locale.ROOT);
			if (subtitlesNameLower.startsWith(baseFileName)) {
				List<String> suffixParts = Arrays
					.asList(FileUtil.getFileNameWithoutExtension(subtitlesNameLower).replace(baseFileName, "").split("[\\s\\.-]+"));
				attachExternalSubtitlesFile(subtitlesFile, media, suffixParts);
			} else if (isSubtitlesFolder(subtitlesFile.getParentFile(), subtitlesName) != null) {
				// Subtitles subfolder that doesn't start with video file name
				List<String> suffixParts = Arrays.asList(FileUtil.getFileNameWithoutExtension(subtitlesNameLower).split("[\\s\\.-]+"));
				for (String suffixPart : suffixParts) {
					if (Iso639.isValid(suffixPart)) {
						attachExternalSubtitlesFile(subtitlesFile, media, suffixParts);
						break;
					}
				}
			}
		}

		// Remove no longer existing external subtitles
		for (Iterator<MediaSubtitle> iterator = media.getSubtitlesTracks().iterator(); iterator.hasNext();) {
			MediaSubtitle subtitles = iterator.next();
			if (
				subtitles.isExternal() &&
				!(subtitles instanceof MediaOnDemandSubtitle) &&
				!folderSubtitles.contains(subtitles.getExternalFile())
			) {
				iterator.remove();
			}
		}
	}

	/**
	 * Creates a new instance of MediaSubtitle, populates it based on the
 incoming subtitlesFile, and attaches it to the incoming MediaInfo so
 it appears on the subtitles tracks list for that media.
	 *
	 * @see MediaInfo#getSubtitleTracksList
	 * @param subtitlesFile
	 * @param media
	 * @param suffixParts contains potential language identifiers, e.g. en or
	 *            eng.
	 */
	private static void attachExternalSubtitlesFile(File subtitlesFile, MediaInfo media, List<String> suffixParts) {
		LOGGER.trace("Attaching external subtitles file for {}", subtitlesFile.getName());
		MediaSubtitle subtitles = new MediaSubtitle();
		subtitles.setType(SubtitleType.valueOfFileExtension(FileUtil.getExtension(subtitlesFile.getPath(), LetterCase.LOWER, Locale.ROOT)));

		String language = null;
		if (suffixParts != null && !suffixParts.isEmpty()) {
			ArrayList<String> modifiableSuffixParts = new ArrayList<>(suffixParts);
			for (Iterator<String> iterator = modifiableSuffixParts.iterator(); iterator.hasNext();) {
				String part = iterator.next();
				if (isBlank(part)) {
					iterator.remove();
				} else if (Iso639.isValid(part)) {
					language = Iso639.getISO639_2Code(part);
					iterator.remove();
				}
			}
			if (!modifiableSuffixParts.isEmpty()) {
				subtitles.setSubtitlesTrackTitleFromMetadata(StringUtils.join(modifiableSuffixParts, '-'));
			}
		}

		try {
			if (isNotBlank(language)) {
				subtitles.setLang(language);
			}
			subtitles.setExternalFile(subtitlesFile);
			if (subtitles.getLang() == null) {
				subtitles.setLang(MediaLang.UND);
			}
			media.addSubtitlesTrack(subtitles);
			LOGGER.trace("Added external subtitles file {} to the media {}", subtitlesFile.getName(), media.toString());
		} catch (FileNotFoundException e) {
			LOGGER.warn("File not found during external subtitles scan: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static int getPriorityIndex(List<String> languagePriorities, String languageCode) {
		if (isBlank(languageCode)) {
			return languagePriorities.size();
		}
		for (int i = 0; i < languagePriorities.size(); i++) {
			String code = languagePriorities.get(i);
			if ("*".equals(code) || MediaLang.UND.equals(code) || Iso639.isCodesMatching(languageCode, code)) {
				return i;
			}
		}
		return languagePriorities.size();
	}

	/**
	 * Finds the {@link MediaSubtitle} with the highest priority based on
	 * the subtitles language and whether the subtitles are external or not.
	 * External subtitles have priority over embedded ones if the language is
	 * the same. Languages are prioritized according to the configured subtitles
	 * language priorities.
	 *
	 * @param candidates the {@link Collection} of {@link MediaSubtitle}
	 *            candidates of which to find the one with the highest priority.
	 * @param renderer the {@link RendererConfiguration} to use to get the
	 *            configures subtitles language priorities.
	 * @param returnNotPrioritized if {@code true} a {@link MediaSubtitle}
	 *            will be returned even if no match to the configured subtitles
	 *            languages priorities is found.
	 * @return The candidate with the highest priority or {@code null}.
	 */
	public static MediaSubtitle findPrioritizedSubtitles(
		Collection<MediaSubtitle> candidates,
		Renderer renderer,
		boolean returnNotPrioritized
	) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}

		final ArrayList<String> languagePriorities = new ArrayList<>();
		for (String language : UMSUtils.getLangList(renderer, false).split(",")) {
			if (isNotBlank(language)) {
				languagePriorities.add(language.trim().toLowerCase(Locale.ROOT));
			}
		}

		LOGGER.trace("Looking for subtitles with the highest priority from {}", StringUtils.join(languagePriorities, ", "));
		ArrayList<MediaSubtitle> candidatesList = new ArrayList<>(candidates);
		Collections.sort(candidatesList, (MediaSubtitle o1, MediaSubtitle o2) -> {
			if (isBlank(o1.getLang()) || isBlank(o2.getLang())) {
				if (isNotBlank(o1.getLang()) || isNotBlank(o2.getLang())) {
					return isBlank(o1.getLang()) ? 1 : -1;
				}
			} else if (!Iso639.isCodesMatching(o1.getLang(), o2.getLang())) {
				int o1Priority = getPriorityIndex(languagePriorities, o1.getLang());
				int o2Priority = getPriorityIndex(languagePriorities, o2.getLang());
				if (o1Priority != o2Priority) {
					return o1Priority - o2Priority;
				}
			}
			if (o1.isExternal() == o2.isExternal()) {
				return 0;
			}
			return o1.isExternal() ? -1 : 1;
		});
		MediaSubtitle result = candidatesList.get(0);
		int priority = getPriorityIndex(languagePriorities, result.getLang());
		if (priority == languagePriorities.size()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("No prioritized subtitles language found, returning: {}", returnNotPrioritized ? result : "null");
			}
			return returnNotPrioritized ? result : null;
		}
		LOGGER.trace("Returning subtitles with priority {}: {}", result);
		return result;
	}
}
