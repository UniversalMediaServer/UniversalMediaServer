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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.MediaMonitor;
import net.pms.dlna.RealFile;
import net.pms.image.ImageIOTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullyPlayed {
	private static final Logger LOGGER = LoggerFactory.getLogger(FullyPlayed.class);
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static final String THUMBNAIL_OVERLAY_RESOURCE_PATH = "/resources/images/icon-fullyplayed.png";
	private static final Color THUMBNAIL_OVERLAY_BACKGROUND_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
	private static final int BLANK_IMAGE_RESOLUTION = 256;
	public static final BufferedImage thumbnailOverlayImage;

	static {
		BufferedImage tmpThumbnailOverlayImage = null;
		try {
			tmpThumbnailOverlayImage = ImageIO.read(FullyPlayed.class.getResourceAsStream(THUMBNAIL_OVERLAY_RESOURCE_PATH));
		} catch (IOException e) {
			LOGGER.error("Error reading fully played overlay image \"{}\": {}", THUMBNAIL_OVERLAY_RESOURCE_PATH, e.getMessage());
			LOGGER.trace("", e);
		}
		thumbnailOverlayImage = tmpThumbnailOverlayImage;
	}

	// Hide the constructor
	private FullyPlayed() {
	}

	/**
	 * Determines if the media thumbnail should have a "fully played" overlay.
	 *
	 * @param file the file representing this media
	 * @return The result
	 */
	public static boolean isFullyPlayedThumbnail(File file) {
		return
			file != null &&
			configuration.getFullyPlayedAction() == FullyPlayedAction.MARK &&
			MediaMonitor.isFullyPlayed(file.getAbsolutePath());
	}

	/**
	 * Determines if the media should be hidden when browsing. Currently only
	 * video files are hidden.
	 *
	 * @param resource the resource the be evaluated
	 * @return The result
	 */
	public static boolean isHideFullyPlayed(DLNAResource resource) {
		return
			resource != null &&
			configuration.getFullyPlayedAction() == FullyPlayedAction.HIDE_VIDEO &&
			resource.getMedia() != null &&
			resource.getMedia().isVideo() &&
			MediaMonitor.isFullyPlayed(resource.getSystemName());
	}

	/**
	 * Adds a text overlay to the given thumbnail and returns it.
	 * <p>
	 * <b>This method either consumes and closes {@code thumb} or it returns it
	 * in a reset state ({@code position = 0}).</b>
	 *
	 * @param thumb the source thumbnail to add the overlay to.
	 * @return The processed thumbnail.
	 */
	public static DLNAThumbnailInputStream addFullyPlayedOverlay(DLNAThumbnailInputStream thumb) {
		if (thumbnailOverlayImage == null) {
			return thumb;
		}

		ImageIO.setUseCache(false);
		BufferedImage image = null;
		if (thumb != null) {
			try {
				image = ImageIO.read(thumb);
			} catch (IOException e) {
				LOGGER.error("Could not read thumbnail input stream: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}

		// Create a blank image if input is missing
		if (image == null) {
			image = new BufferedImage(BLANK_IMAGE_RESOLUTION, BLANK_IMAGE_RESOLUTION, BufferedImage.TYPE_3BYTE_BGR);
		}

		int overlayResolution = (int) Math.round((Math.min(image.getWidth(), image.getHeight())) * 0.6);
		int overlayHorizontalPosition = (image.getWidth() - overlayResolution) / 2;
		int overlayVerticalPosition = (image.getHeight() - overlayResolution) / 2;

		Graphics2D g = image.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.setPaint(THUMBNAIL_OVERLAY_BACKGROUND_COLOR);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.drawImage(thumbnailOverlayImage, overlayHorizontalPosition, overlayVerticalPosition, overlayResolution, overlayResolution, null);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIOTools.imageIOWrite(image, thumb != null ? thumb.getFormat().toString() : "jpg", out);
			if (thumb != null) {
				thumb.close();
			}
			return DLNAThumbnailInputStream.toThumbnailInputStream(out.toByteArray());
		} catch (IOException e) {
			LOGGER.error("Could not write thumbnail byte array: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		if (thumb != null) {
			thumb.fullReset();
		}
		return thumb;
	}

	/**
	 * Prefixes displayName with a "Fully played" prefix if conditions are med
	 *
	 * @param displayName the {@link String} to prefix
	 * @param resource the {@link RealFile} representing the media
	 * @param renderer the current {@link RendererConfiguration}
	 * @return The prefixed {@link String} if conditions are met, or the
	 *         unmodified {@link String}.
	 */
	public static String prefixDisplayName(String displayName, RealFile resource, RendererConfiguration renderer) {
		if (
			renderer != null &&
			!renderer.isThumbnails() &&
			configuration.getFullyPlayedAction() == FullyPlayedAction.MARK &&
			MediaMonitor.isFullyPlayed(resource.getFile().getAbsolutePath())
		) {
			DLNAMediaInfo media = resource.getMedia();
			if (media != null) {
				if (media.isVideo()) {
					displayName = String.format("[%s]%s", Messages.getString("DLNAResource.4"), displayName);
				} else if (media.isAudio()) {
					displayName = String.format("[%s]%s", Messages.getString("DLNAResource.5"), displayName);
				} else if (media.isImage()) {
					displayName = String.format("[%s]%s", Messages.getString("DLNAResource.6"), displayName);
				}
			}
		}
		return displayName;
	}
}
