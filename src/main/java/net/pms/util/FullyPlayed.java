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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.ImageIO;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.MediaMonitor;
import net.pms.dlna.MediaType;
import net.pms.dlna.RealFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullyPlayed {
	private static final Logger LOGGER = LoggerFactory.getLogger(FullyPlayed.class);
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static int thumbnailOverlayResolution = 108;
	private static int thumbnailOverlayHorizontalPositionVideo;
	private static int thumbnailOverlayVerticalPositionVideo;
	private static int thumbnailOverlayHorizontalPositionAudio;
	private static int thumbnailOverlayVerticalPositionAudio;
	private static int thumbnailOverlayHorizontalPositionImage;
	private static int thumbnailOverlayVerticalPositionImage;
	private static final Color THUMBNAIL_OVERLAY_BACKGROUND_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);

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

	private static void initializeThumbnails(BufferedImage image, MediaType mediaType) {
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

		// Calculate the overlay resolution and position
		double maximumOverlayResolution = (Math.min(image.getWidth(), image.getHeight())) * 0.6;
		if (thumbnailOverlayResolution > maximumOverlayResolution) {
			thumbnailOverlayResolution = (int) maximumOverlayResolution;
		}
		int thumbnailOverlayHorizontalPosition = (image.getWidth() - thumbnailOverlayResolution) / 2;
		int thumbnailOverlayVerticalPosition = (int) (image.getHeight() - thumbnailOverlayResolution) / 2;

		// Store the results
		switch (mediaType.toInt()) {
			case MediaType.AUDIO_INT:
				audioThumbnailsLock.writeLock().lock();
				try{
					thumbnailOverlayHorizontalPositionAudio = thumbnailOverlayHorizontalPosition;
					thumbnailOverlayVerticalPositionAudio = thumbnailOverlayVerticalPosition;
					audioThumbnailsInitialized = true;
				} finally {
					audioThumbnailsLock.writeLock().unlock();
				}
				break;
			case MediaType.IMAGE_INT:
				imageThumbnailsLock.writeLock().lock();
				try{
					thumbnailOverlayHorizontalPositionImage = thumbnailOverlayHorizontalPosition;
					thumbnailOverlayVerticalPositionImage = thumbnailOverlayVerticalPosition;
					imageThumbnailsInitialized = true;
				} finally {
					imageThumbnailsLock.writeLock().unlock();
				}
				break;
			case MediaType.VIDEO_INT:
				videoThumbnailsLock.writeLock().lock();
				try{
					thumbnailOverlayHorizontalPositionVideo = thumbnailOverlayHorizontalPosition;
					thumbnailOverlayVerticalPositionVideo = thumbnailOverlayVerticalPosition;
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
			initializeThumbnails(image, mediaType);

			final int thumbnailOverlayHorizontalPosition;
			final int thumbnailOverlayVerticalPosition;
			switch (mediaType.toInt()) {
				case MediaType.AUDIO_INT:
					audioThumbnailsLock.readLock().lock();
					try {
						thumbnailOverlayHorizontalPosition = thumbnailOverlayHorizontalPositionAudio;
						thumbnailOverlayVerticalPosition = thumbnailOverlayVerticalPositionAudio;
					} finally {
						audioThumbnailsLock.readLock().unlock();
					}
					break;
				case MediaType.IMAGE_INT:
					imageThumbnailsLock.readLock().lock();
					try {
						thumbnailOverlayHorizontalPosition = thumbnailOverlayHorizontalPositionImage;
						thumbnailOverlayVerticalPosition = thumbnailOverlayVerticalPositionImage;
					} finally {
						imageThumbnailsLock.readLock().unlock();
					}
					break;
				case MediaType.VIDEO_INT:
					videoThumbnailsLock.readLock().lock();
					try {
						thumbnailOverlayHorizontalPosition = thumbnailOverlayHorizontalPositionVideo;
						thumbnailOverlayVerticalPosition = thumbnailOverlayVerticalPositionVideo;
					} finally {
						videoThumbnailsLock.readLock().unlock();
					}
					break;
				default:
					throw new IllegalStateException("Should not get here");
			}

			Graphics2D g = image.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.setPaint(THUMBNAIL_OVERLAY_BACKGROUND_COLOR);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			if (PMS.thumbnailOverlayImage != null) {
				g.drawImage(PMS.thumbnailOverlayImage, thumbnailOverlayHorizontalPosition, thumbnailOverlayVerticalPosition, thumbnailOverlayResolution, thumbnailOverlayResolution, null);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "jpeg", out);
				thumb = out.toByteArray();
			} catch (IOException e) {
				LOGGER.error("Could not write thumbnail byte array: {}", e.getMessage());
				LOGGER.trace("", e);
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
