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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.InputFile;
import net.pms.formats.AudioAsVideo;
import net.pms.formats.Format;
import net.pms.image.ExifInfo;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil;
import net.pms.media.MediaInfo;
import net.pms.util.APIUtils;
import net.pms.util.UnknownFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final String MANUAL_PARSER = "NO_PARSER";

	/**
	 * This class is not meant to be instantiated.
	 */
	protected Parser() {
	}

	/**
	 * Chooses which parsing method to parse the file with.
	 */
	public static void parse(MediaInfo media, InputFile file, Format ext, int type) {
		//ensure media is not already parsing or is parsed
		media.waitMediaParsing(5);
		if (media.isMediaParsed()) {
			return;
		}
		media.resetParser();
		if (file.getFile() != null) {
			// Special parsing for RealAudio 1.0 and 2.0 which isn't handled by MediaInfo or JAudioTagger
			if (ext.getIdentifier() == Format.Identifier.RA && RealAudioParser.parse(media, file, type)) {
				return;
			}
			// Special parsing for raw image
			if (ext.getIdentifier() == Format.Identifier.RAW && DCRawParser.parse(media, file, type)) {
				return;
			}
			// MediaInfo can't correctly parse ADPCM, DFF, DSF or PNM
			if (MediaInfoParser.isValid() &&
					ext.getIdentifier() != Format.Identifier.ADPCM &&
					ext.getIdentifier() != Format.Identifier.DFF &&
					ext.getIdentifier() != Format.Identifier.DSF &&
					ext.getIdentifier() != Format.Identifier.PNM) {
				MediaInfoParser.parse(media, file.getFile(), type);
			} else if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				JaudiotaggerParser.parse(media, file.getFile(), ext);
			} else {
				FFmpegParser.parse(media, file, ext, type);
			}
		} else {
			FFmpegParser.parse(media, file, ext, type);
		}
	}

	/**
	 * Parse media for Thumb.
	 */
	public static DLNAThumbnail getThumbnail(MediaInfo media, InputFile inputFile, Format ext, int type, Double seekPosition) {
		if (inputFile != null) {
			File file = inputFile.getFile();

			if (type == Format.AUDIO || ext instanceof AudioAsVideo) {
				return JaudiotaggerParser.getThumbnail(file);
			}

			if (type == Format.IMAGE && file != null) {
				// Create the thumbnail image
				try {
					if (media.getImageInfo() instanceof ExifInfo exifInfo && exifInfo.hasExifThumbnail() && !exifInfo.isImageIOSupported()) {
						/*
						 * XXX Extraction of thumbnails was removed in version
						 * 2.10.0 of metadata-extractor because of a bug in
						 * related code. This section is deactivated while
						 * waiting for this to be made available again.
						 *
						 * Images supported by ImageIO or DCRaw aren't affected,
						 * so this only applied to very few images anyway.
						 * It could extract thumbnails for some "raw" images
						 * if DCRaw was disabled.
						 *
						 */
					} else {
						// This will fail with UnknownFormatException for any image formats not supported by ImageIO
						return DLNAThumbnail.toThumbnail(
								Files.newInputStream(file.toPath()),
								320,
								320,
								ImagesUtil.ScaleType.MAX,
								ImageFormat.SOURCE,
								false
						);
					}
				} catch (EOFException e) {
					LOGGER.debug(
							"Error generating thumbnail for \"{}\": Unexpected end of file, probably corrupt file or read error.",
							file.getName()
					);
				} catch (UnknownFormatException e) {
					LOGGER.debug("Could not generate thumbnail for \"{}\" because the format is unknown: {}", file.getName(), e.getMessage());
				} catch (IOException e) {
					LOGGER.debug("Error generating thumbnail for \"{}\": {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
			}
			if (type == Format.VIDEO) {
				DLNAThumbnail thumb = null;
				if (seekPosition == null && media.hasVideoMetadata() && media.getVideoMetadata().getPoster() != null) {
					//API Poster
					thumb = APIUtils.getThumbnailFromUri(media.getVideoMetadata().getPoster());
				}
				if (thumb == null && CONFIGURATION.isUseMplayerForVideoThumbs()) {
					//Mplayer parsing
					thumb = MPlayerParser.getThumbnail(media, inputFile, seekPosition);
				}
				if (thumb == null) {
					//FFmpeg parsing
					thumb = FFmpegParser.getThumbnail(media, inputFile, seekPosition);
				}
				return thumb;
			}
		}
		return null;
	}

}
