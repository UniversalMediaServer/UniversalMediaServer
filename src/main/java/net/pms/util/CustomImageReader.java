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
        if (result.bufferedImage == null) {
            stream.close();
        }
        return result;
    }

    public static ImageReaderResult read(ImageInputStream stream)
        throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        Iterator<?> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }

        ImageInputFormat inputFormat = null;
        ImageReader reader = (ImageReader) iter.next();
        String formatName = reader.getFormatName().toUpperCase(Locale.ROOT);
        // Store the parsing result
        if (formatName != null) {
        	if (formatName.contains("BMP")) {
        		inputFormat = ImageInputFormat.BMP;
        	} else if (formatName.contains("GIF")) {
        		inputFormat = ImageInputFormat.GIF;
        	} else if (formatName.contains("CUR")) {
        		inputFormat = ImageInputFormat.CUR;
        	} else if (formatName.contains("ICO")) {
        		inputFormat = ImageInputFormat.ICO;
        	} else if (formatName.contains("JPEG")) {
        		inputFormat = ImageInputFormat.JPEG;
        	} else if (formatName.contains("TIFF")) {
        		inputFormat = ImageInputFormat.TIFF;
        	} else if (formatName.contains("PNG")) {
        		inputFormat = ImageInputFormat.PNG;
        	} else if (formatName.contains("WBMP")) {
        		inputFormat = ImageInputFormat.WBMP;
        	}
        }

        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, true);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return new ImageReaderResult(bi, inputFormat);
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
    	public final ImageInputFormat imageFormat;

    	public ImageReaderResult(BufferedImage bufferedImage, ImageInputFormat imageFormat) {
    		this.bufferedImage = bufferedImage;
    		this.imageFormat = imageFormat;
    	}
    }

    /**
     * Definition of the different image format supported by the ImageIO parser
     * with the currently installed plugins. If more plugins are added, more
     * entries should be added here and in {@link CustomImageReader#read(ImageInputStream)}.
     */
    public enum ImageInputFormat {
    	BMP, GIF, CUR, ICO, JPEG, TIFF, PNG, WBMP
    }
}
