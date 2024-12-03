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
package net.pms.parsers;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.DCRaw;
import net.pms.encoders.EngineFactory;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.util.InputFile;
import net.pms.util.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DCRawParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(JaudiotaggerParser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String PARSER_NAME = "DCRAW";

	/**
	 * This class is not meant to be instantiated.
	 */
	private DCRawParser() {
	}

	/**
	 * Parses {@code file} and stores the result in {@code media}.
	 *
	 * @param media the {@link MediaInfo} instance to store the parse
	 *            results in.
	 * @param file the {@link File} to parse.
	 */
	public static boolean parse(MediaInfo media, InputFile file, int type) {
		boolean trace = LOGGER.isTraceEnabled();
		if (!isValid()) {
			return false;
		}

		if (trace) {
			LOGGER.trace("Parsing RAW image \"{}\" with DCRaw", file.getFile().getName());
		}
		Dimension dimension = parseDimension(file.getFile());
		if (dimension == null) {
			return false;
		}
		media.setContainer(FormatConfiguration.RAW);

		Metadata metadata;
		FileType fileType = null;
		try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.getFile().toPath()))) {
			fileType = FileTypeDetector.detectFileType(inputStream);
			metadata = MetadataExtractorParser.getMetadata(inputStream, fileType);
		} catch (IOException e) {
			metadata = new Metadata();
			LOGGER.debug("Error reading \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		} catch (ImageProcessingException e) {
			metadata = new Metadata();
			LOGGER.debug(
				"Error parsing {} metadata for \"{}\": {}",
				fileType != null ? fileType.toString().toUpperCase(Locale.ROOT) : "null",
				file.getFile().getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
		if (fileType == FileType.Arw && !MetadataExtractorParser.isARW(metadata)) {
			fileType = FileType.Tiff;
		}
		ImageFormat format = MetadataExtractorParser.toImageFormat(fileType);
		if (format == null || format == ImageFormat.TIFF) {
			format = MetadataExtractorParser.toImageFormat(metadata);
			if (format == null || format == ImageFormat.TIFF) {
				format = ImageFormat.RAW;
			}
		}
		ImageInfo imageInfo;
		try {
			imageInfo = ImageInfo.create(
				(int) dimension.getWidth(),
				(int) dimension.getHeight(),
				metadata,
				format,
				file.getSize(),
				true,
				false
			);
			if (trace) {
				LOGGER.trace("Parsing of RAW image \"{}\" completed: {}", file.getFile().getName(), imageInfo);
			}
		} catch (ParseException e) {
			LOGGER.warn("Unable to parse \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
			return false;
		}
		media.setImageInfo(imageInfo);
		media.setSize(file.getSize());
		media.setImageCount(1);
		Parser.postParse(media, type);
		media.setMediaParser(PARSER_NAME);
		return true;
	}

	private static Dimension parseDimension(File file) {
		if (file == null) {
			throw new NullPointerException("file cannot be null");
		}

		OutputParams params = new OutputParams(CONFIGURATION);
		params.setLog(true);

		String[] cmdArray = new String[4];
		cmdArray[0] = EngineFactory.getEngineExecutable(DCRaw.ID);
		cmdArray[1] = "-i";
		cmdArray[2] = "-v";
		cmdArray[3] = file.getAbsolutePath();

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params, true, false);
		pw.runInSameThread();

		List<String> list = pw.getOtherResults();
		Pattern pattern = Pattern.compile("^Output size:\\s*(\\d+)\\s*x\\s*(\\d+)");
		Matcher matcher;
		for (String s : list) {
			matcher = pattern.matcher(s);
			if (matcher.find()) {
				Dimension result = new Dimension(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(1)));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Parsed resolution {} x {} for image \"{}\" from DCRaw output",
						matcher.group(1),
						matcher.group(2),
						file.getPath()
					);
				}
				return result;
			}
		}
		return null;
	}

	public static boolean isValid() {
		return EngineFactory.isEngineActive(DCRaw.ID);
	}

}
