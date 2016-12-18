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
package net.pms.dlna;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.coobird.thumbnailator.Thumbnails;
import net.pms.util.CustomImageReader;
import net.pms.util.CustomImageReader.ImageReaderResult;
import net.pms.util.ImagesUtil;
import net.pms.util.ImagesUtil.ImageFormat;
import net.pms.util.ImagesUtil.ScaleType;

/**
 * This class is simply a byte array for holding an {@link ImageIO} supported
 * image with some additional metadata.
 *
 * @see DLNAThumbnailInputStream
 */
public class DLNAThumbnail implements Serializable {

	private static final long serialVersionUID = 8014825365045944137L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAThumbnail.class);
	private final byte[] bytes;
	private final int width;
	private final int height;
	private final ImageFormat format;

	/**
	 * Creates a new {@link DLNAThumbnail} instance.
	 *
	 * @param bytes the source image in a supported format. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param width the width of the source image.
	 * @param height the height of the source image.
	 * @param format the {@link ImageFormat} of the source image.
	 * @param copy whether the source image should be copied or shared.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public DLNAThumbnail(byte[] bytes, int width, int height, ImageFormat format, boolean copy) {
		if (copy && bytes != null) {
			this.bytes = new byte[bytes.length];
			System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
		} else {
			this.bytes = bytes;
		}
		this.width = width;
		this.height = height;
		this.format = format;
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the
	 *         source image could not be parsed.
	 */
	public static DLNAThumbnail toThumbnail(byte[] imageByteArray) {
		return toThumbnail(imageByteArray, 0, 0, null, ImageFormat.SOURCE);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *                     {@link ImageFormat#SOURCE} to preserve source format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the
	 *         source image could not be parsed.
	 */
	public static DLNAThumbnail toThumbnail(byte[] imageByteArray, int width, int height, ScaleType scaleType, ImageFormat outputFormat) {
		return imageByteArray != null ? toThumbnail(new ByteArrayInputStream(imageByteArray), width, height, scaleType, outputFormat) : null;
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}.
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the
	 *         source image could not be parsed.
	 */
	public static DLNAThumbnail toThumbnail(InputStream inputStream) {
		return toThumbnail(inputStream, 0, 0, null, ImageFormat.SOURCE);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}.
	 *
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *                     {@link ImageFormat#SOURCE} to preserve source format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the
	 *         source image could not be parsed.
	 */
	public static DLNAThumbnail toThumbnail(InputStream inputStream, int width, int height, ScaleType scaleType, ImageFormat outputFormat) {
		if (inputStream == null) {
			return null;
		}
		try {
			// A little optimization that saves us from recreating an image that
			// is already in the correct format/size and is a byte array.
			byte[] bytes = null;
			if (inputStream instanceof ByteArrayInputStream) {
				bytes = ImagesUtil.toByteArray(inputStream);
				inputStream = new ByteArrayInputStream(bytes);
			}

			ImageReaderResult imageResult = CustomImageReader.read(inputStream);
			inputStream.close();

			if (imageResult.bufferedImage == null || imageResult.imageFormat == null) { // ImageIO doesn't support the image format
				LOGGER.error("Failed to create thumbnail because the source format is unknown");
				return null;
			}

			if (outputFormat == null || outputFormat.equals(ImageFormat.SOURCE)) {
				outputFormat = imageResult.imageFormat;
			}

			// Thumbnails aren't useful in all formats, restrict them
			if (!ImageFormat.JPEG.equals(outputFormat) && !ImageFormat.PNG.equals(outputFormat)) {
				outputFormat = ImageFormat.JPEG;
			}

			if (width == 0 ||
				height == 0 ||
				(
					ScaleType.MAX.equals(scaleType) &&
					imageResult.bufferedImage.getWidth() <= width &&
					imageResult.bufferedImage.getHeight() <= height
				)
			) {
				//No resize, just convert
				if (imageResult.imageFormat.equals(outputFormat) && bytes != null) {
					// Return source
					return new DLNAThumbnail(bytes, imageResult.bufferedImage.getWidth(), imageResult.bufferedImage.getHeight(), imageResult.imageFormat, false);
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.setUseCache(false);
				ImagesUtil.imageIOWrite(imageResult.bufferedImage, outputFormat.toString(), out);
				return new DLNAThumbnail(out.toByteArray(), imageResult.bufferedImage.getWidth(), imageResult.bufferedImage.getHeight(), outputFormat, false);
			} else {
				BufferedImage bufferedImage =
				Thumbnails.of(imageResult.bufferedImage)
				.size(width, height)
				.outputFormat(outputFormat.toString())
				.outputQuality(1.0f)
				.asBufferedImage();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.setUseCache(false);
				ImagesUtil.imageIOWrite(bufferedImage, outputFormat.toString(), out);
				return new DLNAThumbnail(out.toByteArray(), bufferedImage.getWidth(), bufferedImage.getHeight(), outputFormat, false);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to create thumbnail: {}", e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * @param copy whether or not a new array or a reference to the underlying
	 *             buffer should be returned. If a reference is returned,
	 *             <b>NO MODIFICATIONS must be done to the array!</b>
	 * @return the bytes of this thumbnail.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public byte[] getBytes(boolean copy) {
		if (copy) {
			byte[] result = new byte[bytes.length];
			System.arraycopy(bytes, 0, result, 0, bytes.length);
			return result;
		}
		return bytes;
	}

	/**
	 * @return the width of this thumbnail.
	 */
	public int getWidth() {
		return width;
	}


	/**
	 * @return the height of this thumbnail.
	 */
	public int getHeight() {
		return height;
	}


	/**
	 * @return the {@link ImageFormat} for this thumbnail.
	 */
	public ImageFormat getFormat() {
		return format;
	}

	/**
	 * @return the size of this thumbnail in bytes.
	 */
	public int getSize() {
		return bytes != null ? bytes.length : 0;
	}

	public DLNAThumbnail copy() {
		return new DLNAThumbnail(bytes, width, height, format, true);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(80);
		sb.append("DLNAThumbnail: Format = ").append(format)
		.append(", Width = ").append(width)
		.append(", Height = ").append(height)
		.append(", Size = ").append(bytes.length);
		return sb.toString();
	}
}
