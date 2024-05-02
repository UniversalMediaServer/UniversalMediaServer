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
package net.pms.image;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import net.pms.util.UnknownFormatException;

/**
 * This is a utility class for use with {@link ImageIO}, which mainly contains
 * modified versions of static {@link ImageIO} methods.
 *
 * @author Nadahar
 */
public class ImageIOTools {

	protected static final IIORegistry REGISTRY = IIORegistry.getDefaultInstance();

	/**
	 * This class should not be instantiated.
	 */
	private ImageIOTools() {}

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
			} catch (IOException e2) {
				//Do nothing
			}
			if (e instanceof RuntimeException runtimeException) {
				throw new ImageIORuntimeException(
					"An error occurred while trying to read image: " + e.getMessage(), runtimeException);
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

		try (stream) {
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
			iter = REGISTRY.getServiceProviders(ImageInputStreamSpi.class, true);
		} catch (IllegalArgumentException e) {
			return null;
		}

		while (iter.hasNext()) {
			ImageInputStreamSpi spi = iter.next();
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
