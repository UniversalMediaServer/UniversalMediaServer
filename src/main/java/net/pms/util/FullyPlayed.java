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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.MediaMonitor;
import net.pms.dlna.MediaType;
import net.pms.formats.Format;
import net.pms.image.BufferedImageFilter;
import net.pms.image.NonGeometricBufferedImageOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An utility class for handling "fully played" functionality.
 */
public class FullyPlayed {
	private static final Logger LOGGER = LoggerFactory.getLogger(FullyPlayed.class);
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static final String THUMBNAIL_OVERLAY_RESOURCE_PATH = "/resources/images/icon-fullyplayed.png";
	private static final Color THUMBNAIL_OVERLAY_BACKGROUND_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
	private static final int BLANK_IMAGE_RESOLUTION = 256;

	/** A static cache of the fully played overlay as a {@link BufferedImage} */
	public static final BufferedImage THUMBNAIL_OVERLAY_IMAGE;

	/** A static cache of the fully played overlay {@link BufferedImageFilter} */
	public static final FullyPlayerOverlayFilter OVERLAY_FILTER_INSTANCE = new FullyPlayerOverlayFilter(DLNAResource.THUMBNAIL_HINTS);

	static {
		BufferedImage tmpThumbnailOverlayImage = null;
		try {
			tmpThumbnailOverlayImage = ImageIO.read(FullyPlayed.class.getResourceAsStream(THUMBNAIL_OVERLAY_RESOURCE_PATH));
		} catch (IOException e) {
			LOGGER.error("Error reading fully played overlay image \"{}\": {}", THUMBNAIL_OVERLAY_RESOURCE_PATH, e.getMessage());
			LOGGER.trace("", e);
		}
		THUMBNAIL_OVERLAY_IMAGE = tmpThumbnailOverlayImage;
	}

	// Hide the constructor
	private FullyPlayed() {
	}

	/**
	 * Determines if the media should be marked as "fully played" either with
	 * text or a "fully played" overlay.
	 *
	 * @param file the file representing this media
	 * @return The result
	 */
	public static boolean isFullyPlayedMark(File file) {
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
	 * @return The cached {@link FullyPlayerOverlayFilter} instance.
	 */
	public static BufferedImageFilter getOverlayFilter() {
		return OVERLAY_FILTER_INSTANCE;
	}

	/**
	 * Prefixes the specified string with a "fully played" text.
	 *
	 * @param displayName the {@link String} to prefix.
	 * @param resource the {@link DLNAResource} representing the media.
	 * @return The prefixed {@link String}.
	 */
	public static String addFullyPlayedNamePrefix(String displayName, DLNAResource resource) {
		MediaType mediaType;
		if (resource.getMedia() != null) {
			mediaType = resource.getMedia().getMediaType();
		} else if (resource.getFormat() != null) {
			switch (resource.getFormat().getType()) {
				case Format.AUDIO:
					mediaType = MediaType.AUDIO;
					break;
				case Format.IMAGE:
					mediaType = MediaType.IMAGE;
					break;
				case Format.VIDEO:
					mediaType = MediaType.VIDEO;
					break;
				case Format.UNKNOWN:
				default:
					mediaType = MediaType.UNKNOWN;
					break;
			}
		} else {
			mediaType = MediaType.UNKNOWN;
		}
		switch (mediaType) {
			case IMAGE:
				return String.format("[%s] %s", Messages.getString("DLNAResource.6"), displayName);
			case VIDEO:
				return String.format("[%s] %s", Messages.getString("DLNAResource.4"), displayName);
			case AUDIO:
			case UNKNOWN:
			default:
				return String.format("[%s] %s", Messages.getString("DLNAResource.5"), displayName);

		}
	}

	/**
	 * A {@link BufferedImageFilter} implementation that applies the
	 * "fully played" overlay on the specified {@link BufferedImage}.
	 *
	 * @author Nadahar
	 */
	public static class FullyPlayerOverlayFilter extends NonGeometricBufferedImageOp implements BufferedImageFilter {

		/**
		 * Creates a new fully played filter instance.
		 *
		 * @param hints the {@link RenderingHints} or {@code null}.
		 */
		public FullyPlayerOverlayFilter(RenderingHints hints) {
			super(hints);
		}

		@Override
		public String getDescription() {
			return toString();
		}

		@Override
		public String toString() {
			return "Fully played image overlay";
		}

		@Override
		public BufferedImage filter(BufferedImage source, BufferedImage destination) {
			return filter(source, destination, true).getBufferedImage();
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source) {
			return filter(source, null, true);
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source, BufferedImage destination, boolean modifySource) {
			if (THUMBNAIL_OVERLAY_IMAGE == null) {
				return new BufferedImageFilterResult(source, false, true);
			}

			boolean sameInstance = true;
			// Create a blank image if the input is missing
			if (source == null) {
				source = new BufferedImage(BLANK_IMAGE_RESOLUTION, BLANK_IMAGE_RESOLUTION, BufferedImage.TYPE_3BYTE_BGR);
				sameInstance = false;
			}

			// Create new destination or reuse source according to modifySource
			if (destination == null) {
				if (modifySource) {
					destination = source;
				} else {
					destination = createCompatibleDestImage(source, null);
					sameInstance = false;
				}
			} else {
				sameInstance = source == destination;
			}

			int overlayResolution = (int) Math.round((Math.min(source.getWidth(), source.getHeight())) * 0.6);
			int overlayHorizontalPosition = (source.getWidth() - overlayResolution) / 2;
			int overlayVerticalPosition = (source.getHeight() - overlayResolution) / 2;

			Graphics2D g2d = destination.createGraphics();
			try {
				if (hints != null) {
					g2d.setRenderingHints(hints);
				}
				if (source != destination) {
					g2d.drawImage(source, 0, 0, null);
				}
				g2d.setPaint(THUMBNAIL_OVERLAY_BACKGROUND_COLOR);
				g2d.fillRect(0, 0, source.getWidth(), source.getHeight());
				g2d.drawImage(
					THUMBNAIL_OVERLAY_IMAGE,
					overlayHorizontalPosition,
					overlayVerticalPosition,
					overlayResolution,
					overlayResolution,
					null
				);
			} finally {
				g2d.dispose();
			}

			return new BufferedImageFilterResult(destination, true, sameInstance);
		}
	}
}
