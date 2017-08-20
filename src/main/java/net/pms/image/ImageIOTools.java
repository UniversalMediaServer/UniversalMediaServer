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
package net.pms.image;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import net.pms.image.ImageIORuntimeException;
import net.pms.util.UnknownFormatException;
import com.drew.metadata.Metadata;

/**
 * This is a utility class for use with {@link ImageIO}, which mainly contains
 * modified versions of static {@link ImageIO} methods.
 *
 * @author Nadahar
 */
public class ImageIOTools {

	protected static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

	// Not to be instantiated
	private ImageIOTools() {
	}

	/**
	 * A copy of {@link ImageIO#read(InputStream)} that calls
	 * {{@link #read(ImageInputStream)} instead of
	 * {@link ImageIO#read(ImageInputStream)} and that returns
	 * {@link ImageReaderResult} instead of {@link BufferedImage}. This lets
	 * information about the detected format be retained.
	 *
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 *
	 * @param inputStream an {@link InputStream} to read from.
	 *
	 * @see ImageIO#read(InputStream)
	 */
	public static ImageReaderResult read(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("input == null!");
		}

		ImageInputStream stream = createImageInputStream(inputStream);
		try {
			ImageReaderResult result = read(stream);
			if (result == null) {
				inputStream.close();
			}
			return result;
		} catch (RuntimeException | IOException e) {
			try {
				inputStream.close();
			} catch (Exception e2) {
				//Do nothing
			}
			if (e instanceof RuntimeException) {
				throw new ImageIORuntimeException(
					"An error occurred while trying to read image: " + e.getMessage(),
					(RuntimeException) e
				);
			}
			throw e;
		}
	}

	/**
	 * A copy of {@link ImageIO#read(ImageInputStream)} that returns
	 * {@link ImageReaderResult} instead of {@link BufferedImage}. This lets
	 * information about the detected format be retained.
	 *
	 * <b>
	 * This method consumes and closes {@code stream}.
	 * </b>
	 *
	 * @param stream an {@link ImageInputStream} to read from.
	 *
	 * @see ImageIO#read(ImageInputStream)
	 */
	public static ImageReaderResult read(ImageInputStream stream) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException("stream == null!");
		}

		try {
			Iterator<?> iter = ImageIO.getImageReaders(stream);
			if (!iter.hasNext()) {
				throw new UnknownFormatException("Unable to find a suitable image reader");
			}

			ImageFormat inputFormat = null;
			BufferedImage bufferedImage = null;
			ImageReader reader = (ImageReader) iter.next();
			try {
				// Store the parsing result
				inputFormat = ImageFormat.toImageFormat(reader.getFormatName());

				reader.setInput(stream, true, true);
				bufferedImage = reader.read(0, reader.getDefaultReadParam());
			} finally {
				reader.dispose();
			}
			return bufferedImage != null ? new ImageReaderResult(bufferedImage, inputFormat) : null;
		} catch (RuntimeException e) {
			throw new ImageIORuntimeException("An error occurred while trying to read image: " + e.getMessage(), e);
		} finally {
			stream.close();
		}
	}

	/**
	 * Tries to detect the input image file format using {@link ImageIO} and
	 * returns the result.
	 * <p>
	 * This method does not close {@code inputStream}.
	 *
	 * @param inputStream the image whose format to detect.
	 * @return The {@link ImageFormat} for the input.
	 * @throws UnknownFormatException if the format could not be determined.
	 * @throws IOException if an IO error occurred.
	 */
	public static ImageFormat detectFileFormat(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("input == null!");
		}

		try (ImageInputStream stream = createImageInputStream(inputStream)) {
			Iterator<?> iter = ImageIO.getImageReaders(stream);
			if (!iter.hasNext()) {
				throw new UnknownFormatException("Unable to find a suitable image reader");
			}

			ImageReader reader = (ImageReader) iter.next();
			ImageFormat format = ImageFormat.toImageFormat(reader.getFormatName());
			if (format == null) {
				throw new UnknownFormatException("Unable to determine image format");
			}
			return format;
		} catch (RuntimeException e) {
			throw new ImageIORuntimeException("An error occurred while trying to detect image format: " + e.getMessage(), e);
		}
	}

	/**
	 * Tries to gather the data needed to populate a {@link ImageInfo} instance
	 * describing the input image.
	 *
	 * <p>
	 * This method does not close {@code inputStream}.
	 *
	 * @param inputStream the image whose information to gather.
	 * @param size the size of the image in bytes or
	 *             {@link ImageInfo#SIZE_UNKNOWN} if it can't be determined.
	 * @param metadata the {@link Metadata} instance to embed in the resulting
	 *                 {@link ImageInfo} instance.
	 * @param applyExifOrientation whether or not Exif orientation should be
	 *            compensated for when setting width and height. This will also
	 *            reset the Exif orientation information. <b>Changes will be
	 *            applied to the {@code metadata} argument instance</b>.
	 * @return An {@link ImageInfo} instance describing the input image.
	 * @throws UnknownFormatException if the format could not be determined.
	 * @throws IOException if an IO error occurred.
	 */
	public static ImageInfo readImageInfo(InputStream inputStream, long size, Metadata metadata, boolean applyExifOrientation) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("input == null!");
		}

		try (ImageInputStream stream = createImageInputStream(inputStream)) {
			Iterator<?> iter = ImageIO.getImageReaders(stream);
			if (!iter.hasNext()) {
				throw new UnknownFormatException("Unable to find a suitable image reader");
			}

			ImageReader reader = (ImageReader) iter.next();
			try {
				int width = -1;
				int height = -1;
				ImageFormat format = ImageFormat.toImageFormat(reader.getFormatName());
				if (format == null) {
					throw new UnknownFormatException("Unable to determine image format");
				}

				ColorModel colorModel = null;
				try {
					reader.setInput(stream, true, true);
					Iterator<ImageTypeSpecifier> iterator = reader.getImageTypes(0);
					if (iterator.hasNext()) {
						colorModel = iterator.next().getColorModel();
					}
					width = reader.getWidth(0);
					height = reader.getHeight(0);
				} catch (RuntimeException e) {
					throw new ImageIORuntimeException("Error reading image information: " + e.getMessage(), e);
				}

				boolean imageIOSupport;
				if (format == ImageFormat.TIFF) {
					// ImageIO thinks that it can read some "TIFF like" RAW formats,
					// but fails when it actually tries, so we have to test it.
					try {
						ImageReadParam param = reader.getDefaultReadParam();
						param.setSourceRegion(new Rectangle(1, 1));
						reader.read(0, param);
						imageIOSupport = true;
					} catch (Exception e) {
						// Catch anything here, we simply want to test if it fails.
						imageIOSupport = false;
					}
				} else {
					imageIOSupport = true;
				}

				ImageInfo imageInfo = ImageInfo.create(
					width,
					height,
					format,
					size,
					colorModel,
					metadata,
					applyExifOrientation,
					imageIOSupport
				);
				return imageInfo;
			} finally {
				reader.dispose();
			}
		}
	}

	/**
	 * A copy of {@link ImageIO#createImageInputStream(Object)} that ignores
	 * {@link ImageIO} configuration and never caches to disk. This is intended
	 * used on relatively small images and caching to disk is very expensive
	 * compared to keeping a copy in memory while doing the source analysis.
	 *
	 * @see ImageIO#createImageInputStream(Object)
	 */
	public static ImageInputStream createImageInputStream(Object input)
		throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input == null!");
		}

		Iterator<ImageInputStreamSpi> iter;
		// Ensure category is present
		try {
			iter = theRegistry.getServiceProviders(ImageInputStreamSpi.class, true);
		} catch (IllegalArgumentException e) {
			return null;
		}

		while (iter.hasNext()) {
			ImageInputStreamSpi spi = (ImageInputStreamSpi)iter.next();
			if (spi.getInputClass().isInstance(input)) {
				try {
					return spi.createInputStreamInstance(input, false, null);
				} catch (IOException e) {
					throw new IIOException("Can't create cache file!", e);
				}
			}
		}

		return null;
	}

	/**
	 * This is a wrapper around
	 * {@link ImageIO#write(RenderedImage, String, OutputStream)}
	 * that translate any thrown {@link RuntimeException} to an
	 * {@link ImageIORuntimeException} because {@link ImageIO} has the nasty
	 * habit of throwing {@link RuntimeException}s when something goes wrong.
	 *
	 * @see ImageIO#write(RenderedImage, String, OutputStream)
	 */
	public static boolean imageIOWrite(RenderedImage im, String formatName, OutputStream output) throws IOException {
		try {
			return ImageIO.write(im, formatName, output);
		} catch (RuntimeException e) {
			throw new ImageIORuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * A simple container for more than one return value.
	 */
	public static class ImageReaderResult {
		public final BufferedImage bufferedImage;
		public final ImageFormat imageFormat;
		public final int width;
		public final int height;

		public ImageReaderResult(BufferedImage bufferedImage, ImageFormat imageFormat) {
			this.bufferedImage = bufferedImage;
			this.imageFormat = imageFormat;
			this.width = bufferedImage == null ? -1 : bufferedImage.getWidth();
			this.height = bufferedImage == null ? -1 : bufferedImage.getHeight();
		}
	}
}
