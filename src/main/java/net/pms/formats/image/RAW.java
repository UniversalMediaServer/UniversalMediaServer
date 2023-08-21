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
package net.pms.formats.image;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.InputFile;
import net.pms.encoders.DCRaw;
import net.pms.encoders.EngineFactory;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.media.MediaInfo;
import net.pms.renderers.Renderer;
import net.pms.util.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAW extends ImageBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(RAW.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.RAW;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"3fr",
			"ari",
			"arw",
			"bay",
			"cap",
			"cr2",
			"crw",
			"dcr",
			"dcs",
			"dng",
			"drf",
			"eip",
			"erf",
			"fff",
			"iiq",
			"k25",
			"kdc",
			"mdc",
			"mef",
			"mos",
			"mrw",
			"nef",
			"nrw",
			"obm",
			"orf",
			"pef",
			"ptx",
			"pxn",
			"r3d",
			"raf",
			"raw",
			"rw2",
			"rwl",
			"rwz",
			"sr2",
			"srf",
			"srw",
			"x3f"
		};
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	@Override
	public void parse(MediaInfo media, InputFile file, int type, Renderer renderer) {
		boolean trace = LOGGER.isTraceEnabled();
		if (media == null || file == null || file.getFile() == null) {
			// Parsing is impossible
			if (trace) {
				if (file != null && file.getFile() != null) {
					LOGGER.trace("Not parsing RAW file \"{}\" because media is null", file.getFile().getName());
				} else {
					LOGGER.error("Not parsing RAW file because file is null");
				}
			}
			return;
		}

		UmsConfiguration configuration = PMS.getConfiguration(renderer);
		try {
			// Only parse using DCRaw if it is enabled
			DCRaw dcraw = (DCRaw) EngineFactory.getActiveEngine(DCRaw.ID);
			if (dcraw != null) {
				if (trace) {
					LOGGER.trace("Parsing RAW image \"{}\" with DCRaw", file.getFile().getName());
				}
				dcraw.parse(media, file.getFile());

				media.setCodecV(FormatConfiguration.RAW);
				media.setContainer(FormatConfiguration.RAW);

				ImageInfo imageInfo = null;
				Metadata metadata = null;
				FileType fileType = null;
				try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.getFile().toPath()))) {
					fileType = FileTypeDetector.detectFileType(inputStream);
					metadata = ImagesUtil.getMetadata(inputStream, fileType);
				} catch (IOException e) {
					metadata = new Metadata();
					LOGGER.debug("Error reading \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
					LOGGER.trace("", e);
				} catch (ImageProcessingException e) {
					metadata = new Metadata();
					LOGGER.debug(
						"Error parsing {} metadata for \"{}\": {}",
						fileType.toString().toUpperCase(Locale.ROOT),
						file.getFile().getAbsolutePath(),
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
				if (fileType == FileType.Arw && !ImagesUtil.isARW(metadata)) {
					fileType = FileType.Tiff;
				}
				ImageFormat format = ImageFormat.toImageFormat(fileType);
				if (format == null || format == ImageFormat.TIFF) {
					format = ImageFormat.toImageFormat(metadata);
					if (format == null || format == ImageFormat.TIFF) {
						format = ImageFormat.RAW;
					}
				}
				try {
					imageInfo = ImageInfo.create(
						media.getWidth(),
						media.getHeight(),
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
				}

				media.setImageInfo(imageInfo);

				if (media.getWidth() > 0 && media.getHeight() > 0 && configuration.getImageThumbnailsEnabled()) {
					byte[] image = dcraw.getThumbnail(null, file.getFile().getAbsolutePath(), imageInfo);
					media.setThumb(DLNAThumbnail.toThumbnail(image, 320, 320, ScaleType.MAX, ImageFormat.JPEG, false));
				}
			} else {
				if (trace) {
					LOGGER.trace(
						"Parsing RAW image \"{}\" as a regular image because DCRaw is disabled",
						file.getFile().getName()
					);
				}
				ImagesUtil.parseImage(file.getFile(), media);
			}
			media.setSize(file.getSize());
			media.setImageCount(1);
			media.postParse(type, file);
			media.setMediaparsed(true);

		} catch (IOException e) {
			LOGGER.error(
				"Error parsing RAW file \"{}\": {}",
				file.getFile().getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
