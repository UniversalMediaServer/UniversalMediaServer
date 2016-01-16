/*
 * Universal Media Server, for streaming any medias to DLNA
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.MediaMonitor;
import net.pms.dlna.MediaType;
import net.pms.dlna.RealFile;

public class FullyPlayed {

	private static final Logger LOGGER = LoggerFactory.getLogger(FullyPlayed.class);
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static int thumbnailFontSizeVideo;
	private static int thumbnailTextHorizontalPositionVideo;
	private static int thumbnailTextVerticalPositionVideo;
	private static int thumbnailFontSizeAudio;
	private static int thumbnailTextHorizontalPositionAudio;
	private static int thumbnailTextVerticalPositionAudio;
	private static int thumbnailFontSizeImage;
	private static int thumbnailTextHorizontalPositionImage;
	private static int thumbnailTextVerticalPositionImage;
	private static final String THUMBNAIL_TEXT_VIDEO = Messages.getString("DLNAResource.4");
	private static final String THUMBNAIL_TEXT_AUDIO = Messages.getString("DLNAResource.5");
	private static final String THUMBNAIL_TEXT_IMAGE = Messages.getString("DLNAResource.6");
	private static final Color THUMBNAIL_OVERLAY_BACKGROUND_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.6f);
	private static final Color THUMBNAIL_OVERLAY_TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);

	/**
	 * This lock is responsible for all audio class variables.
	 */
	private static ReadWriteLock audioThumbnailsLock = new ReentrantReadWriteLock();
	private static boolean audioThumbnailsInitialized = false;
	/**
	 * This lock is responsible for all image class variables.
	 */
	private static ReadWriteLock imageThumbnailsLock = new ReentrantReadWriteLock();
	private static boolean imageThumbnailsInitialized = false;
	/**
	 * This lock is responsible for all video class variables.
	 */
	private static ReadWriteLock videoThumbnailsLock = new ReentrantReadWriteLock();
	private static boolean videoThumbnailsInitialized = false;


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

	private static void initializeThumbnails(BufferedImage image, MediaType mediaType, String fontName, int fontStyle) {
		switch (mediaType.toInt()) {
			case MediaType.AUDIO_INT:
				audioThumbnailsLock.readLock().lock();
				try{
					if (audioThumbnailsInitialized) {
						return;
					}
				} finally {
					audioThumbnailsLock.readLock().unlock();
				}
				break;
			case MediaType.IMAGE_INT:
				imageThumbnailsLock.readLock().lock();
				try{
					if (imageThumbnailsInitialized) {
						return;
					}
				} finally {
					imageThumbnailsLock.readLock().unlock();
				}
				break;
			case MediaType.VIDEO_INT:
				videoThumbnailsLock.readLock().lock();
				try{
					if (videoThumbnailsInitialized) {
						return;
					}
				} finally {
					videoThumbnailsLock.readLock().unlock();
				}
				break;
			default:
				throw new IllegalArgumentException("mediaType cannot be of type unknown");
		}

		// Calculate thumbnail text size and position
		Graphics2D graphics = image.createGraphics();
		int thumbnailFontSize = image.getWidth() / 6;
		FontMetrics metrics = graphics.getFontMetrics(new Font(fontName, fontStyle, thumbnailFontSize));
		final String thumbnailText;
		switch (mediaType.toInt()) {
			case MediaType.AUDIO_INT:
				thumbnailText = THUMBNAIL_TEXT_AUDIO;
				break;
			case MediaType.IMAGE_INT:
				thumbnailText = THUMBNAIL_TEXT_IMAGE;
				break;
			case MediaType.VIDEO_INT:
				thumbnailText = THUMBNAIL_TEXT_VIDEO;
				break;
			default:
				throw new IllegalStateException("Should not get here");
		}
		Rectangle2D textSize = metrics.getStringBounds(thumbnailText, graphics);
		int textWidth = (int) textSize.getWidth();
		int thumbnailTextHorizontalPosition = (image.getWidth() - textWidth) / 2;
		int maxTextWidth = (int) Math.round(image.getWidth() * 0.9);

		// Use a smaller font size if there isn't enough room
		if (textWidth > maxTextWidth) {
			for (int divider = 7; divider < 99; divider++) {
				thumbnailFontSize = image.getWidth() / divider;
				metrics = graphics.getFontMetrics(new Font(fontName, fontStyle, thumbnailFontSize));
				textSize = metrics.getStringBounds(thumbnailText, graphics);
				textWidth = (int) textSize.getWidth();
				if (textWidth <= maxTextWidth) {
					thumbnailTextHorizontalPosition = (image.getWidth() - textWidth) / 2;
					break;
				}
			}
		}
		int thumbnailTextVerticalPosition = (int) (image.getHeight() - textSize.getHeight()) / 2 + metrics.getAscent();

		// Store the results
		switch (mediaType.toInt()) {
			case MediaType.AUDIO_INT:
				audioThumbnailsLock.writeLock().lock();
				try{
					thumbnailFontSizeAudio = thumbnailFontSize;
					thumbnailTextHorizontalPositionAudio = thumbnailTextHorizontalPosition;
					thumbnailTextVerticalPositionAudio = thumbnailTextVerticalPosition;
					audioThumbnailsInitialized = true;
				} finally {
					audioThumbnailsLock.writeLock().unlock();
				}
				break;
			case MediaType.IMAGE_INT:
				imageThumbnailsLock.writeLock().lock();
				try{
					thumbnailFontSizeImage = thumbnailFontSize;
					thumbnailTextHorizontalPositionImage = thumbnailTextHorizontalPosition;
					thumbnailTextVerticalPositionImage = thumbnailTextVerticalPosition;
					imageThumbnailsInitialized = true;
				} finally {
					imageThumbnailsLock.writeLock().unlock();
				}
				break;
			case MediaType.VIDEO_INT:
				videoThumbnailsLock.writeLock().lock();
				try{
					thumbnailFontSizeVideo = thumbnailFontSize;
					thumbnailTextHorizontalPositionVideo = thumbnailTextHorizontalPosition;
					thumbnailTextVerticalPositionVideo = thumbnailTextVerticalPosition;
					videoThumbnailsInitialized = true;
				} finally {
					videoThumbnailsLock.writeLock().unlock();
				}
				break;
			default:
				throw new IllegalStateException("Should not get here");
		}
	}

	/**
	 * Adds a text overlay to the given thumbnail and returns it.
	 *
	 * @param thumb the source thumb to add the overlay to
	 * @param mediaType the type of media the thumbnail is for
	 * @return The modified thumbnail
	 */
	public static byte[] addFullyPlayedOverlay(byte[] thumb, MediaType mediaType) {
		if (thumb == null) {
			return null;
		}
		if (mediaType == MediaType.UNKNOWN) {
			throw new IllegalArgumentException("Can't generate fully played overlay for unknown media type");
		}

		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(thumb));
		} catch (IOException e) {
			LOGGER.error("Could not read thumbnail byte array: {}", e.getMessage());
			LOGGER.trace("",e);
			image = null;
		}
		if (image != null) {
			/*
			 * TODO: Include and use a custom font
			 */
			String fontName = "Arial";
			int fontStyle = Font.PLAIN;

			initializeThumbnails(image, mediaType, fontName, fontStyle);

			final int thumbnailFontSize;
			final int thumbnailTextHorizontalPosition;
			final int thumbnailTextVerticalPosition;
			final String thumbnailText;
			switch (mediaType.toInt()) {
				case MediaType.AUDIO_INT:
					audioThumbnailsLock.readLock().lock();
					try {
						thumbnailFontSize = thumbnailFontSizeAudio;
						thumbnailTextHorizontalPosition = thumbnailTextHorizontalPositionAudio;
						thumbnailTextVerticalPosition = thumbnailTextVerticalPositionAudio;
					} finally {
						audioThumbnailsLock.readLock().unlock();
					}
					thumbnailText = THUMBNAIL_TEXT_AUDIO;
					break;
				case MediaType.IMAGE_INT:
					imageThumbnailsLock.readLock().lock();
					try {
						thumbnailFontSize = thumbnailFontSizeImage;
						thumbnailTextHorizontalPosition = thumbnailTextHorizontalPositionImage;
						thumbnailTextVerticalPosition = thumbnailTextVerticalPositionImage;
					} finally {
						imageThumbnailsLock.readLock().unlock();
					}
					thumbnailText = THUMBNAIL_TEXT_IMAGE;
					break;
				case MediaType.VIDEO_INT:
					videoThumbnailsLock.readLock().lock();
					try {
						thumbnailFontSize = thumbnailFontSizeVideo;
						thumbnailTextHorizontalPosition = thumbnailTextHorizontalPositionVideo;
						thumbnailTextVerticalPosition = thumbnailTextVerticalPositionVideo;
					} finally {
						videoThumbnailsLock.readLock().unlock();
					}
					thumbnailText = THUMBNAIL_TEXT_VIDEO;
					break;
				default:
					throw new IllegalStateException("Should not get here");
			}

			Graphics2D g = image.createGraphics();
			g.setPaint(THUMBNAIL_OVERLAY_BACKGROUND_COLOR);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.setColor(THUMBNAIL_OVERLAY_TEXT_COLOR);
			g.setFont(new Font(fontName, fontStyle, thumbnailFontSize));
			g.drawString(thumbnailText, thumbnailTextHorizontalPosition, thumbnailTextVerticalPosition);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "jpeg", out);
				thumb = out.toByteArray();
			} catch (IOException e) {
				LOGGER.error("Could not write thumbnail byte array: {}", e.getMessage());
				LOGGER.trace("",e);
			}
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
