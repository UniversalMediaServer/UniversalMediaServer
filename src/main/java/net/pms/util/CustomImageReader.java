package net.pms.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import net.pms.util.ImagesUtil.ImageFormat;

/**
 * This is a hack of a utility class created because whoever wrote {@link ImageIO}
 * decided to make it {@code final}. The sole purpose of these methods is to
 * return the image format of the source image along with the {@link BufferedImage}.
 *
 * There should be no reason to have to do this twice, and thus the result from
 * the analysis done when reading the image is kept.
 */
public class CustomImageReader {

    protected static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();

	// Not to be instantiated
	private CustomImageReader() {
	}

	/**
	 * A copy of {@link ImageIO#read(InputStream)} that calls {{@link #read(ImageInputStream)}
	 * instead of {@link ImageIO#read(ImageInputStream)}.
	 *
	 * @see ImageIO#read(InputStream)
	 */
    public static ImageReaderResult read(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }

        ImageInputStream stream = createImageInputStream(input);
        ImageReaderResult result = read(stream);
        return result;
    }

    protected static ImageFormat parseFormatName(String formatName) {
    	ImageFormat result = null;
        if (formatName != null) {
        	if (formatName.contains("BMP")) {
        		result = ImageFormat.BMP;
        	} else if (formatName.contains("GIF")) {
        		result = ImageFormat.GIF;
        	} else if (formatName.contains("CUR")) {
        		result = ImageFormat.CUR;
        	} else if (formatName.contains("ICO")) {
        		result = ImageFormat.ICO;
        	} else if (formatName.contains("JPEG")) {
        		result = ImageFormat.JPEG;
        	} else if (formatName.contains("TIFF")) {
        		result = ImageFormat.TIFF;
        	} else if (formatName.contains("PNG")) {
        		result = ImageFormat.PNG;
        	} else if (formatName.contains("WBMP")) {
        		result = ImageFormat.WBMP;
        	}
        }
        return result;
    }

    /**
     * <b>Closes {@code stream}</b>
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

	        BufferedImage bi;
	        ImageReadParam param = reader.getDefaultReadParam();
	        reader.setInput(stream, true, true);
	        try {
	            bi = reader.read(0, param);
	        } finally {
	            reader.dispose();
	        }
	        return new ImageReaderResult(bi, inputFormat);
        } finally {
        	stream.close();
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

    public static class UnknownFormatException extends IOException {
		private static final long serialVersionUID = -3779357403392039811L;

		public UnknownFormatException() {
            super();
        }

        public UnknownFormatException(String message) {
            super(message);
        }

        public UnknownFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnknownFormatException(Throwable cause) {
            super(cause);
        }
    }
}
