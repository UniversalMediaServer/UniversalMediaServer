/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.Format;
import net.pms.image.ImageFormat;
import net.pms.image.ImageIOTools;
import net.pms.image.ImagesUtil.ScaleType;


/**
 * This is an {@code enum} used as a singleton class for providing and caching
 * generic file extension icons. Thread-safe.
 */
public enum GenericIcons {

	/** The static singleton instance. */
	INSTANCE;

	private final BufferedImage genericAudioIcon = readBufferedImage("formats/audio.png");
	private final BufferedImage genericImageIcon = readBufferedImage("formats/image.png");
	private final BufferedImage genericVideoIcon = readBufferedImage("formats/video.png");
	private final BufferedImage genericUnknownIcon = readBufferedImage("formats/unknown.png");
	private final DLNAThumbnail genericFolderThumbnail;
	private final ReentrantLock cacheLock = new ReentrantLock();
	/**
	 * All access to {@link #cache} must be protected with {@link #cacheLock}.
	 */
	private final Map<ImageFormat, Map<IconType, Map<String, DLNAThumbnail>>> cache = new HashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericIcons.class);

	private GenericIcons() {
		DLNAThumbnail thumbnail;
		try {
			thumbnail = DLNAThumbnail.toThumbnail(getResourceAsStream("thumbnail-folder-256.png"));
		} catch (IOException e) {
			thumbnail = null;
		}
		genericFolderThumbnail = thumbnail;
	}

	/**
	 * Retrieves or creates the appropriate generic icon/thumbnail for
	 * {@code resource}.
	 *
	 * @param resource the {@link DLNAResource} the return a generic icon for.
	 * @return The appropriate {@link DLNAThumbnailInputStream} or {@code null}
	 *         if one couldn't be generated.
	 */
	public DLNAThumbnailInputStream getGenericIcon(DLNAResource resource) {
		/*
		 * This should be the same format as the source images since OpenJDK
		 * will fail to write JPEGs if the cached BufferedImage has 4 color
		 * components. Alternatively the color model of the cached
		 * BufferedImages would have to be converted to 3 components first.
		 */
		ImageFormat imageFormat = ImageFormat.PNG;

		if (resource == null) {
			ImageIO.setUseCache(false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				ImageIOTools.imageIOWrite(genericUnknownIcon, imageFormat.toString(), out);
				return DLNAThumbnailInputStream.toThumbnailInputStream(out.toByteArray());
			} catch (IOException e) {
				LOGGER.warn(
					"Unexpected error while generating generic thumbnail for null resource: {}",
					e.getMessage()
				);
				LOGGER.trace("", e);
				return null;
			}
		}

		IconType iconType = IconType.UNKNOWN;
		if (resource.getMedia() != null) {
			if (resource.getMedia().isAudio()) {
				iconType = IconType.AUDIO;
			} else if (resource.getMedia().isImage()) {
				iconType = IconType.IMAGE;
			} else if (resource.getMedia().isVideo()) {
				// FFmpeg parses images as video, try to rectify
				if (resource.getFormat() != null && resource.getFormat().isImage()) {
					iconType = IconType.IMAGE;
				} else {
					iconType = IconType.VIDEO;
				}
			}
		} else if (resource.getFormat() != null) {
			if (resource.getFormat().isAudio()) {
				iconType = IconType.AUDIO;
			} else if (resource.getFormat().isImage()) {
				iconType = IconType.IMAGE;
			} else if (resource.getFormat().isVideo()) {
				iconType = IconType.VIDEO;
			}
		}

		DLNAThumbnail image = null;
		cacheLock.lock();
		try {
			if (!cache.containsKey(imageFormat)) {
				cache.put(imageFormat, new HashMap<IconType, Map<String, DLNAThumbnail>>());
			}
			Map<IconType, Map<String, DLNAThumbnail>> typeCache = cache.get(imageFormat);

			if (!typeCache.containsKey(iconType)) {
				typeCache.put(iconType, new HashMap<String, DLNAThumbnail>());
			}
			Map<String, DLNAThumbnail> imageCache = typeCache.get(iconType);

			String label = getLabelFromImageFormat(resource.getMedia());
			if (label == null) {
				label = getLabelFromFormat(resource.getFormat());
			}
			if (label == null) {
				label = getLabelFromContainer(resource.getMedia());
			}
			if (label != null && label.length() < 5) {
				label = label.toUpperCase(Locale.ROOT);
			} else if (label != null && label.toLowerCase(Locale.ROOT).equals(label)) {
				label = StringUtils.capitalize(label);
			}

			if (isBlank(label)) {
				label = Messages.getString("General.Unknown");
			}

			if (imageCache.containsKey(label)) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(imageCache.get(label));
			}

			if (LOGGER.isTraceEnabled()) {
				if (label != null) {
					LOGGER.trace(
						"Creating generic {} {} thumbnail for {}",
						imageFormat,
						iconType.toString().toLowerCase(),
						label.toUpperCase()
					);
				} else {
					LOGGER.warn(
						"Creating generic {} {} thumbnail without label",
						imageFormat,
						iconType.toString().toLowerCase()
					);
				}
			}

			try {
				image = createGenericIcon(label, imageFormat, iconType);
			} catch (IOException e) {
				LOGGER.warn(
					"Unexpected error while generating generic thumbnail for \"{}\": {}",
					resource.getName(),
					e.getMessage()
				);
				LOGGER.trace("", e);
			}

			imageCache.put(label, image);
		} finally {
			cacheLock.unlock();
		}

		return DLNAThumbnailInputStream.toThumbnailInputStream(image);
	}

	/**
	 * @return The generic folder icon/thumbnail.
	 */
	public DLNAThumbnailInputStream getGenericFolderIcon() {
		return DLNAThumbnailInputStream.toThumbnailInputStream(genericFolderThumbnail);
	}

	private static String getLabelFromImageFormat(DLNAMediaInfo mediaInfo) {
		return
			mediaInfo != null && mediaInfo.isImage() &&
			mediaInfo.getImageInfo() != null &&
			mediaInfo.getImageInfo().getFormat() != null ?
				mediaInfo.getImageInfo().getFormat().toString() : null;
	}

	private static String getLabelFromFormat(Format format) {
		if (format == null || format.getIdentifier() == null) {
			return null;
		}
		// Replace some Identifier names with prettier ones
		switch (format.getIdentifier()) {
			case AUDIO_AS_VIDEO:
				return "Audio as Video";
			case MICRODVD:
				return "MicroDVD";
			case SUBRIP:
				return "SubRip";
			case THREEG2A:
				return "3G2A";
			case THREEGA:
				return "3GA";
			case WEBVTT:
				return "WebVTT";
			case ISOVOB:
				return Messages.getString("GenericIcons.DVDVideo");
			default:
				return format.getIdentifier().toString();
		}
	}

	private static String getLabelFromContainer(DLNAMediaInfo mediaInfo) {
		return mediaInfo != null ? mediaInfo.getContainer() : null;
	}

	/**
	 * Creates a generic icon image using the given arguments.
	 *
	 * @param label The label to use for the new generic icon.
	 * @param imageFormat the {@link ImageFormat} to create the new generic icon
	 *            as.
	 * @param iconType the {@link IconType} to use for the new generic icon.
	 *
	 * @return the generic icon with the container label added and scaled in
	 *         accordance with renderer setting
	 * @throws IOException If an error occurs during creation.
	 */
	private DLNAThumbnail createGenericIcon(String label, ImageFormat imageFormat, IconType iconType) throws IOException {

		BufferedImage image;
		switch (iconType) {
			case AUDIO:
				image = genericAudioIcon;
				break;
			case IMAGE:
				image = genericImageIcon;
				break;
			case VIDEO:
				image = genericVideoIcon;
				break;
			default:
				image = genericUnknownIcon;
		}

		if (image != null) {
			// Make a copy
			ColorModel colorModel = image.getColorModel();
			image = new BufferedImage(colorModel, image.copyData(null), colorModel.isAlphaPremultiplied(), null);
		}

		ByteArrayOutputStream out = null;

		if (label != null && image != null) {
			out = new ByteArrayOutputStream();
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			try {
				int size = 40;
				Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
				FontMetrics metrics = g.getFontMetrics(font);
				while (size > 7 && metrics.stringWidth(label) > 135) {
					size--;
					font = new Font(Font.SANS_SERIF, Font.BOLD, size);
					metrics = g.getFontMetrics(font);
				}
				// Text center point 127x, 49y - calculate centering coordinates
				int x = 127 - metrics.stringWidth(label) / 2;
				int y = 46 + metrics.getAscent() / 2;
				g.drawImage(image, 0, 0, null);
				g.setColor(Color.WHITE);
				g.setFont(font);
				g.drawString(label, x, y);

				ImageIO.setUseCache(false);
				ImageIOTools.imageIOWrite(image, imageFormat.toString(), out);
			} finally {
				g.dispose();
			}
		} else if (image != null) {
			out = new ByteArrayOutputStream();
			ImageIOTools.imageIOWrite(image, imageFormat.toString(), out);
		}
		return out != null ? DLNAThumbnail.toThumbnail(out.toByteArray(), 0, 0, ScaleType.MAX, imageFormat, false) : null;
	}

	/**
	 * Reads a resource from a given resource path into a
	 * {@link BufferedImage}. {@code /resources/images/} is already
	 * prepended to the path and only the rest should be specified.
	 *
	 * @param resourcePath the path to the resource relative to
	 * {@code /resources/images/}}

	 * @return The {@link BufferedImage} created from the specified resource or
	 *         {@code null} if the path is invalid.
	 */
	protected static BufferedImage readBufferedImage(String resourcePath) {
		InputStream inputStream = getResourceAsStream(resourcePath);
		if (inputStream != null) {
			try {
				return ImageIO.read(inputStream);
			} catch (IOException e) {
				LOGGER.error("Could not read resource \"{}\": {}", resourcePath, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
		}
		return null;
	}

	/**
	 * Convenience method for retrieving an image resource.
	 *
	 * @param resourcePath the image resource path relative to "
	 *            {@code /resources/images}".
	 * @return The {@link InputStream} for the specified resource.
	 */
	protected static InputStream getResourceAsStream(String resourcePath) {
		return PMS.class.getResourceAsStream("/resources/images/" + resourcePath);
	}

	/**
	 * An {@code enum} representing the type of generic icon/thumbnail.
	 */
	protected static enum IconType {

		/** Audio */
		AUDIO,

		/** Image */
		IMAGE,

		/** Unknown type */
		UNKNOWN,

		/** Video */
		VIDEO
	}
}
