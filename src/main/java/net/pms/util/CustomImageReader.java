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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import com.drew.metadata.Metadata;
import net.pms.dlna.ImageInfo;
import net.pms.formats.ImageFormat;

/**
 * This is a hack of a utility class created because whoever wrote {@link ImageIO}
 * decided to make it {@code final}. The sole purpose of these methods is to
 * return the image format of the source image along with the {@link BufferedImage}.
 *
 * There should be no reason to have to do this twice, and thus the result from
 * the analysis done when reading the image is kept.
 *
 * @author Nadahar
 */
public class CustomImageReader {

    protected static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

	// Not to be instantiated
	private CustomImageReader() {
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
        } catch (IOException e) {
        	try {
        		inputStream.close();
        	} catch (Exception e2) {
        		//Do nothing
        	}
        	throw e;
        }
    }

    protected static ImageFormat parseFormatName(String formatName) {
    	ImageFormat result = null;
        if (formatName != null) {
        	if (formatName.contains("BMP")) {
        		result = ImageFormat.BMP;
        	} else if (formatName.contains("CUR")) {
        		result = ImageFormat.CUR;
        	} else if (formatName.contains("DCX")) {
        		result = ImageFormat.DCX;
        	} else if (formatName.contains("GIF")) {
        		result = ImageFormat.GIF;
        	} else if (formatName.contains("ICNS")) {
        		result = ImageFormat.ICNS;
        	} else if (formatName.contains("ICO")) {
        		result = ImageFormat.ICO;
        	} else if (formatName.contains("JPEG")) {
        		result = ImageFormat.JPEG;
        	} else if (formatName.contains("PCX")) {
        		result = ImageFormat.PCX;
        	} else if (formatName.contains("PNG")) {
        		result = ImageFormat.PNG;
        	} else if (formatName.contains("PNM")) {
        		result = ImageFormat.PNM;
        	} else if (formatName.contains("PSD")) {
        		result = ImageFormat.PSD;
        	} else if (formatName.contains("TIFF")) {
        		result = ImageFormat.TIFF;
        	} else if (formatName.contains("WBMP")) {
        		result = ImageFormat.WBMP;
        	}
        }
        return result;
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
	        	throw new UnknownFormatException("Could not find a suitable reader for: " + stream);
	        }

	        ImageReader reader = (ImageReader) iter.next();
	        // Store the parsing result
	        ImageFormat inputFormat = parseFormatName(reader.getFormatName().toUpperCase(Locale.ROOT));

	        BufferedImage bufferedImage;
	        ImageReadParam param = reader.getDefaultReadParam();
	        reader.setInput(stream, true, true);
	        try {
	            bufferedImage = reader.read(0, param);
	        } finally {
	            reader.dispose();
	        }
	        return bufferedImage != null ? new ImageReaderResult(bufferedImage, inputFormat) : null;
        } finally {
        	stream.close();
        }
    }

    /**
     * Tries to detect the input image file format using {@link ImageIO} and
     * returns the result.
     *
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
	        	throw new UnknownFormatException("Could not find a suitable reader for: " + stream);
	        }

	        ImageReader reader = (ImageReader) iter.next();
	        ImageFormat format = parseFormatName(reader.getFormatName().toUpperCase(Locale.ROOT));
	        if (format == null) {
	        	throw new UnknownFormatException("Could not parse the format information for: " + stream);
	        }
	        return format;
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
     * @return An {@link ImageInfo} instance describing the input image.
	 * @throws UnknownFormatException if the format could not be determined.
     * @throws IOException if an IO error occurred.
     */
    public static ImageInfo readImageInfo(InputStream inputStream, long size, Metadata metadata) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("input == null!");
        }

        try (ImageInputStream stream = createImageInputStream(inputStream)) {
	        Iterator<?> iter = ImageIO.getImageReaders(stream);
	        if (!iter.hasNext()) {
	        	throw new UnknownFormatException("Could not find a suitable reader for: " + stream);
	        }

	        ImageReader reader = (ImageReader) iter.next();
	        try {
	        	int width = -1;
	        	int height = -1;
		        ImageFormat format = parseFormatName(reader.getFormatName().toUpperCase(Locale.ROOT));
		        if (format == null) {
		        	throw new UnknownFormatException("Could not parse the format information for: " + stream);
		        }
		        ColorModel colorModel = null;

		        if (colorModel == null) {
			        reader.setInput(stream, true, true);
			        Iterator<ImageTypeSpecifier> iterator = reader.getImageTypes(0);
			        if (iterator.hasNext()) {
			        	colorModel = iterator.next().getColorModel();
			        }
			        width = reader.getWidth(0);
			        height = reader.getHeight(0);
		        }

		        boolean imageIOSupport;
		        if (format == ImageFormat.TIFF) {
		        	// ImageIO thinks that it can read some "TIFF like" RAW formats,
		        	// but fails when it actually tries, so we have to test it.
			        try {
			        	reader.read(0, reader.getDefaultReadParam());
			        	imageIOSupport = true;
			        } catch (Exception e) {
			        	// Catch anything here, we simply want to test if it fails.
			        	imageIOSupport = false;
			        }
		        } else {
		        	imageIOSupport = true;
		        }

		        ImageInfo imageInfo = new ImageInfo(
		        	width,
		        	height,
		        	format,
		        	size,
		        	colorModel,
		        	metadata,
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
	 * used on relatively small images and caching to disk is extremely expensive
	 * compared to just keeping a copy in memory while doing the source analysis.
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
     * A simple container for more than one return value.
     */
    public static class ImageReaderResult {
    	public final BufferedImage bufferedImage;
    	public final ImageFormat imageFormat;

    	public ImageReaderResult(BufferedImage bufferedImage, ImageFormat imageFormat) {
    		this.bufferedImage = bufferedImage;
    		this.imageFormat = imageFormat;
    	}
    }
}
