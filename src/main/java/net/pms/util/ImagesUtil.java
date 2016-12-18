package net.pms.util;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import mediautil.gen.Log;
import mediautil.image.jpeg.LLJTran;
import mediautil.image.jpeg.LLJTranException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.util.CustomImageReader.ImageReaderResult;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagesUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImagesUtil.class);

	public static InputStream getAutoRotateInputStreamImage(InputStream input, int exifOrientation) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			auto(input, baos, exifOrientation);
		} catch (IOException | LLJTranException e) {
			LOGGER.error("Error in auto rotate", e);
			return null;
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}

	public static void auto(InputStream input, OutputStream output, int exifOrientation) throws IOException, LLJTranException {
		// convert sanselan exif orientation -> llj operation
		int op;
		switch (exifOrientation) {
			case 1:
				op = 0;
				break;
			case 2:
				op = 1;
				break;
			case 3:
				op = 6;
				break;
			case 4:
				op = 2;
				break;
			case 5:
				op = 3;
				break;
			case 6:
				op = 5;
				break;
			case 7:
				op = 4;
				break;
			case 8:
				op = 7;
				break;
			default:
				op = 0;
		}

		// Raise the Debug Level which is normally LEVEL_INFO. Only Warning
		// messages will be printed by MediaUtil.
		Log.debugLevel = Log.LEVEL_NONE;

		// 1. Initialize LLJTran and Read the entire Image including Appx markers
		LLJTran llj = new LLJTran(input);
		// If you pass the 2nd parameter as false, Exif information is not
		// loaded and hence will not be written.
		llj.read(LLJTran.READ_ALL, true);

		// 2. Transform the image using default options along with
		// transformation of the Orientation tags. Try other combinations of
		// LLJTran_XFORM.. flags. Use a jpeg with partial MCU (partialMCU.jpg)
		// for testing LLJTran.XFORM_TRIM and LLJTran.XFORM_ADJUST_EDGES
		int options = LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_ORIENTATION;
		llj.transform(op, options);

		// 3. Save the Image which is already transformed as specified by the
		//    input transformation in Step 2, along with the Exif header.
		try (OutputStream out = new BufferedOutputStream(output)) {
			llj.save(out, LLJTran.OPT_WRITE_ALL);
		}

		// Cleanup
		input.close();
		llj.freeMemory();
	}

	/**
	 * This method populates the supplied {@link DLNAMediaInfo} object with some of the image data
	 * (WIDTH, HEIGHT, BITSPERPIXEL, COLORTYPE, MODEL, EXPOSURE TIME, ORIENTATION and ISO).
	 *
	 * @param file The image file to be parsed
	 * @param media The Imaging metadata which will be populated
	 * @throws ImageReadException
	 * @throws IOException
	 */
	public static void parseImageByImaging(File file, DLNAMediaInfo media) throws ImageReadException, IOException {
		ImageInfo info = Imaging.getImageInfo(file);
		media.setWidth(info.getWidth());
		media.setHeight(info.getHeight());
		media.setBitsPerPixel(info.getBitsPerPixel());
		media.setColorType(info.getColorType());
		String formatName = info.getFormatName();
		if (formatName.startsWith("JPEG")) {
			media.setCodecV(FormatConfiguration.JPG);
			ImageMetadata meta = Imaging.getMetadata(file);
			if (meta != null && meta instanceof JpegImageMetadata) {
				JpegImageMetadata jpegmeta = (JpegImageMetadata) meta;
				TiffField tf = jpegmeta.findEXIFValue(TiffTagConstants.TIFF_TAG_MODEL);
				if (tf != null) {
					media.setModel(tf.getStringValue().trim());
				}

				tf = jpegmeta.findEXIFValue(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME);
				if (tf != null) {
					media.setExposure((int) (1000 * tf.getDoubleValue()));
				}

				tf = jpegmeta.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION);
				if (tf != null) {
					media.setOrientation(tf.getIntValue());
				}

				tf = jpegmeta.findEXIFValue(ExifTagConstants.EXIF_TAG_ISO);
				if (tf != null) {
					// Galaxy Nexus jpg pictures may contain multiple values, take the first
					int[] isoValues = tf.getIntArrayValue();
					media.setIso(isoValues[0]);
				}
			}

		} else if (formatName.startsWith("PNG")) {
			media.setCodecV(FormatConfiguration.PNG);
		} else if (formatName.startsWith("GIF")) {
			media.setCodecV(FormatConfiguration.GIF);
		} else if (formatName.startsWith("BMP")) {
			media.setCodecV(FormatConfiguration.BMP);
		} else if (formatName.startsWith("TIF")) {
			media.setCodecV(FormatConfiguration.TIFF);
		}
	}

	/**
	 * This attempts to get the underlying byte array directly from the
	 * {@link InputStream} if it is backed by a byte array, otherwise the
	 * {@link InputStream} is copied into a new byte array with
	 * {@link IOUtils#toByteArray(InputStream)}.
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 * @param inputStream the <code>InputStream</code> to read.
     * @return The resulting byte array.
     * @throws IOException if an I/O error occurs
	 */
	public static byte[] toByteArray(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		}

		// Avoid copying the data if it's already a byte array
		if (inputStream instanceof ByteArrayInputStream) {
			Field f;
			try {
				f = ByteArrayInputStream.class.getDeclaredField("buf");
				f.setAccessible(true);
				return (byte[]) f.get(inputStream);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				// Reflection failed, use IOUtils instead
				LOGGER.debug("Unexpected reflection failure in toByteArray(): {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		try {
			return IOUtils.toByteArray(inputStream);
		} finally {
			inputStream.close();
		}
	}

	/**
	 * Converts image to a different {@link ImageFormat}. Format support is
	 * limited to that of {@link ImageIO}.
	 * <p><b>
	 * This method consumes and closes {@code inputStream}.
	 * </b>
	 *
	 * @param imageInputStream the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to generate. If this is
	 *                     {@link ImageFormat#SOURCE} or {@code null} this has
	 *                     no effect.
	 * @return The converted image, {@code null} if the source is {@code null}
	 *         or the source image if the operation fails.
	 */
	public static InputStream convertImage(InputStream imageInputStream, ImageFormat outputFormat) {
		if (imageInputStream == null) {
			return null;
		}

		byte[] imageByteArray = null;
		try {
			imageByteArray = toByteArray(imageInputStream);
		} catch (IOException e) {
			LOGGER.warn("Converting image failed: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		byte[] result = scaleImage(imageByteArray, 0, 0, null, outputFormat, false);
		return result == null ? null : new ByteArrayInputStream(result);
	}

	/**
	 * Converts image to a different {@link ImageFormat}. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to generate. If this is
	 *                     {@link ImageFormat#SOURCE} or {@code null} this has
	 *                     no effect.
	 * @return The converted image, {@code null} if the source is {@code null}
	 *         or the source image if the operation fails.
	 */
	public static byte[] convertImage(byte[] imageByteArray, ImageFormat outputFormat) {
		if (outputFormat == null || outputFormat.equals(ImageFormat.SOURCE)) {
			return imageByteArray;
		}
		return scaleImage(imageByteArray, 0, 0, null, outputFormat, false);
	}

	/**
	 * Convert and scales an image for use as a thumbnail for a specific
	 * {@code RendererConfiguration} .Preserves aspect ratio. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param thumbnailInputStream the source image in a supported format.
	 * @param imageProfile the DLNA media profile to adhere to for the output.
	 * @param renderer the {@link RendererConfiguration} to get output settings
	 *                 from.
	 * @return The scaled and/or converted thumbnail, {@code null} if the
	 *         source is {@code null} or the source image if the operation fails.
	 *
	 * XXX Ideally all internal thumb handling should be done using byte arrays
	 *     instead of streams, and this overload could be removed.
	 */
	public static InputStream scaleThumb(DLNAThumbnailInputStream thumbnailInputStream, DLNAImageProfile imageProfile, RendererConfiguration renderer) {
		if (thumbnailInputStream == null) {
			return null;
		}

		try {
			return new ByteArrayInputStream(scaleThumb(thumbnailInputStream.getThumbnail(), imageProfile, renderer));
		} finally {
			thumbnailInputStream.fullReset();
		}
	}

	/**
	 * Convert and scales a thumbnail for use for a specific
	 * {@code RendererConfiguration} .Preserves aspect ratio. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param thumbnail the source thumbnail in a supported format.
	 * @param imageProfile the DLNA media profile to adhere to for the output.
	 * @param renderer the {@link RendererConfiguration} to get output settings
	 *                 from.
	 * @return The scaled and/or converted thumbnail, {@code null} if the
	 *         source is {@code null} or the source image if the operation fails.
	 */
	public static byte[] scaleThumb(DLNAThumbnail thumbnail, DLNAImageProfile imageProfile, RendererConfiguration renderer) {
		switch (imageProfile.toInt()) {
			case DLNAImageProfile.JPEG_LRG_INT:
				return scaleImage(thumbnail.getBytes(false), 4096, 4096, ScaleType.MAX, ImageFormat.JPEG, renderer != null ? renderer.isThumbnailPadding() : false);
			case DLNAImageProfile.JPEG_MED_INT:
				return scaleImage(thumbnail.getBytes(false), 1024, 768, ScaleType.MAX, ImageFormat.JPEG, renderer != null ? renderer.isThumbnailPadding() : false);
			case DLNAImageProfile.JPEG_RES_H_V_INT:
				return scaleImage(thumbnail.getBytes(false), imageProfile.getH(), imageProfile.getV(), ScaleType.EXACT, ImageFormat.JPEG, false);
			case DLNAImageProfile.JPEG_SM_INT:
				return scaleImage(thumbnail.getBytes(false), 640, 480, ScaleType.MAX, ImageFormat.JPEG, renderer != null ? renderer.isThumbnailPadding() : false);
			case DLNAImageProfile.JPEG_TN_INT:
				return scaleImage(thumbnail.getBytes(false), 160, 160, ScaleType.MAX, ImageFormat.JPEG, renderer != null ? renderer.isThumbnailPadding() : false);
			case DLNAImageProfile.PNG_LRG_INT:
				return scaleImage(thumbnail.getBytes(false), 4096, 4096, ScaleType.MAX, ImageFormat.PNG, renderer != null ? renderer.isThumbnailPadding() : false);
			case DLNAImageProfile.PNG_TN_INT:
				return scaleImage(thumbnail.getBytes(false), 160, 160, ScaleType.MAX, ImageFormat.PNG, renderer != null ? renderer.isThumbnailPadding() : false);
			default:
				return scaleImage(thumbnail.getBytes(false), 160, 160, ScaleType.MAX, ImageFormat.JPEG, renderer != null ? renderer.isThumbnailPadding() : false);
		}
	}

	/**
	 *
	 * @param fileName the "file name" part of the HTTP request.
	 * @return The "decoded" {@link ImageProfile} or {@link ImageProfile#JPEG_TN}
	 *         if the parsing fails.
	 */
	public static DLNAImageProfile parseThumbRequest(String fileName) {
		Matcher matcher = Pattern.compile("^thumbnail0000(\\w+_\\w+)_").matcher(fileName);
		if (matcher.find()) {
			DLNAImageProfile imageProfile = DLNAImageProfile.toDLNAImageProfile(matcher.group(1));
			if (imageProfile == null) {
				LOGGER.warn("Could not parse DLNAImageProfile from \"{}\"", matcher.group(1));
			} else {
				return imageProfile;
			}
		}
		return DLNAImageProfile.JPEG_TN;
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio. Format support is limited to
	 * that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param fitTo the {@link Rectangle} to fit the image to.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled, {@code null} if the source is {@code null} or the
	 *         source image if the operation fails.
	 */
	public static byte[] scaleImage(byte[] imageByteArray, Rectangle fitTo, boolean padToSize) {
		if (fitTo.isEmpty()) {
			return imageByteArray;
		}
		return scaleImage(imageByteArray, fitTo.width, fitTo.height, ScaleType.EXACT, ImageFormat.SOURCE, padToSize);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio. Format support is limited to
	 * that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled, {@code null} if the source is {@code null} or the
	 *         source image if the operation fails.
	 */

	public static byte[] scaleImage(byte[] imageByteArray, int width, int height, boolean padToSize) {
		if (width == 0 || height == 0) {
			return imageByteArray;
		}
		return scaleImage(imageByteArray, width, height, ScaleType.EXACT, ImageFormat.SOURCE, padToSize);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio. Format support is limited to
	 * that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *                     {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled and/or converted image, {@code null} if the source is
	 *         {@code null} or the source image if the operation fails.
	 */
	public static byte[] scaleImage(byte[] imageByteArray, int width, int height, ScaleType scaleType, ImageFormat outputFormat, boolean padToSize) {
		if (imageByteArray == null) {
			return null;
		}

		try {
			ImageReaderResult imageResult = CustomImageReader.read(new ByteArrayInputStream(imageByteArray));

			if (imageResult.bufferedImage == null || imageResult.imageFormat == null) { // ImageIO doesn't support the image format
				LOGGER.warn("Failed to resize image because the source format is unknown");
				return imageByteArray;
			}

			if (outputFormat == null || outputFormat.equals(ImageFormat.SOURCE)) {
				outputFormat = imageResult.imageFormat;
			}

			ImageIO.setUseCache(false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (width == 0 ||
				height == 0 ||
				(
					ScaleType.MAX.equals(scaleType) &&
					imageResult.bufferedImage.getWidth() <= width &&
					imageResult.bufferedImage.getHeight() <= height
				)
			) {
				//No resize, just convert
				if (imageResult.imageFormat.equals(outputFormat)) {
					// Nothing to do, just return source
					return imageByteArray;
				}
				imageIOWrite(imageResult.bufferedImage, outputFormat.toString(), out);
			} else {
				if (padToSize) {
					Thumbnails.of(imageResult.bufferedImage)
						.size(width, height)
						.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
						.outputFormat(outputFormat.toString())
						.outputQuality(1.0f)
						.toOutputStream(out);
				} else {
					Thumbnails.of(imageResult.bufferedImage)
						.size(width, height)
						.outputFormat(outputFormat.toString())
						.outputQuality(1.0f)
						.toOutputStream(out);
				}

			}
			if (out != null) {
				return out.toByteArray();
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to resize image: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return imageByteArray;
	}

	/**
	 * This is a wrapper around
	 * {@link ImageIO#write(RenderedImage, String, OutputStream)}
	 * that translate any thrown {@link RuntimeException} to an
	 * {@link IOUtilsRuntimeException} because {@link IOUtils} has the nasty
	 * habit of throwing {@link RuntimeException}s when something goes wrong.
	 *
	 * @see ImageIO#write(RenderedImage, String, OutputStream)
	 */
	public static boolean imageIOWrite(RenderedImage im, String formatName, OutputStream output) throws IOException {
		try {
			return ImageIO.write(im, formatName, output);
		} catch (RuntimeException e) {
			throw new IOUtilsRuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Defines how image scaling is done, if the given resolution is the exact
	 * resolution of the output or the maximum the output can be, If set to
	 * maximum, no upscale will occur.
	 */
	public enum ScaleType {
		EXACT, MAX
	}

    /**
     * Definition of the different image format supported by the ImageIO parser
     * with the currently installed plugins plus the special value {@code SOURCE}.
     * If more plugins are added, more entries should be added here and in
     * {@link CustomImageReader#read(ImageInputStream)}.
     */
    public enum ImageFormat {
    	BMP, CUR, GIF, ICO, JPEG, PNG, SOURCE, TIFF, WBMP;

    	public static ImageFormat toImageFormat(DLNAImageProfile imageProfile) {
    		switch (imageProfile.toInt()) {
    			case DLNAImageProfile.GIF_LRG_INT:
    				return ImageFormat.GIF;
				case DLNAImageProfile.JPEG_LRG_INT:
				case DLNAImageProfile.JPEG_MED_INT:
				case DLNAImageProfile.JPEG_RES_H_V_INT:
				case DLNAImageProfile.JPEG_SM_INT:
				case DLNAImageProfile.JPEG_TN_INT:
					return ImageFormat.JPEG;
				case DLNAImageProfile.PNG_LRG_INT:
				case DLNAImageProfile.PNG_TN_INT:
					return ImageFormat.PNG;
				default:
					return null;

    		}
    	}
    }

    public static class IOUtilsRuntimeException extends IOException {
		private static final long serialVersionUID = -96661084610576312L;

		public IOUtilsRuntimeException() {
	        super();
	    }

	    public IOUtilsRuntimeException(String message) {
	        super(message);
	    }

	    public IOUtilsRuntimeException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public IOUtilsRuntimeException(Throwable cause) {
	        super(cause);
	    }
    }
}
