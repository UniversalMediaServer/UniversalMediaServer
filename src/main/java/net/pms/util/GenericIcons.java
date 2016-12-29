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
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.formats.ImageFormat;

/**
 * This is an singleton class for providing and caching generic file extension
 * icons. Thread-safe.
 */

public enum GenericIcons {
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

	public DLNAThumbnailInputStream getGenericIcon(DLNAMediaInfo media) {
		ImageFormat imageFormat = ImageFormat.PNG;

		IconType iconType = IconType.UNKNOWN;
		if (media != null) {
			if (media.isAudio()) {
				iconType = IconType.AUDIO;
			} else if (media.isImage()) {
				iconType = IconType.IMAGE;
			} else if (media.isVideo()) {
				iconType = IconType.VIDEO;
			}
		}

		DLNAThumbnail image = null;
		cacheLock.lock();
		try {
			if (!cache.containsKey(imageFormat)) {
				cache.put(imageFormat, new HashMap<IconType, Map<String,DLNAThumbnail>>());
			}
			Map<IconType, Map<String,DLNAThumbnail>> typeCache = cache.get(imageFormat);

			if (!typeCache.containsKey(iconType)) {
				typeCache.put(iconType, new HashMap<String, DLNAThumbnail>());
			}
			Map<String, DLNAThumbnail> imageCache = typeCache.get(iconType);

			String label = media != null ?
				media.isImage() && media.getImageInfo() != null && media.getImageInfo().getFormat() != null ?
					media.getImageInfo().getFormat().toString() :
					media.getContainer() :
				null;
			if (label != null && label.length() < 5) {
				label = label.toUpperCase(Locale.ROOT);
			} else if (label != null && label.toLowerCase(Locale.ROOT).equals(label)) {
				label = StringUtils.capitalize(label);
			}
			if (imageCache.containsKey(label)) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(imageCache.get(label));
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Creating generic {} thumbnail for {} ({})", iconType.toString().toLowerCase(), label.toUpperCase(), imageFormat);
			}

			try {
				image = addFormatLabelToImage(label, imageFormat, iconType);
			} catch (IOException e) {
				LOGGER.warn("Unexpected error while generating generic thumbnail for \"{}\": {}", media, e.getMessage());
				LOGGER.trace("", e);
			}

			imageCache.put(label, image);
		} finally {
			cacheLock.unlock();
		}

		return DLNAThumbnailInputStream.toThumbnailInputStream(image);
	}

	public DLNAThumbnailInputStream getGenericFolderIcon() {
		return DLNAThumbnailInputStream.toThumbnailInputStream(genericFolderThumbnail);
	}

	/**
	 * Add the format(container) name of the media to the generic icon image.
	 *
	 * @param image BufferdImage to be the label added
	 * @param label the media container name to be added as a label
	 * @param renderer the renderer configuration
	 *
	 * @return the generic icon with the container label added and scaled in accordance with renderer setting
	 */
	private DLNAThumbnail addFormatLabelToImage(String label, ImageFormat imageFormat, IconType iconType) throws IOException {

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
				ImagesUtil.imageIOWrite(image, imageFormat.toString(), out);
			} finally {
				g.dispose();
			}
		}
		return out != null ? new DLNAThumbnail(out.toByteArray(), imageFormat, image, null, null, false)  : null;
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
	protected BufferedImage readBufferedImage(String resourcePath) {
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

	protected InputStream getResourceAsStream(String resourcePath) {
		return PMS.class.getResourceAsStream("/resources/images/" + resourcePath);
	}

	protected static enum IconType {
		AUDIO, IMAGE, UNKNOWN, VIDEO
	}
}
