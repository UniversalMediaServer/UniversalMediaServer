package net.pms.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import mediautil.gen.Log;
import mediautil.image.jpeg.LLJTran;
import mediautil.image.jpeg.LLJTranException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
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
	 * Creates a black background with the exact dimensions specified, then
	 * centers the image on the background, preserving the aspect ratio.
	 *
	 * @param image
	 * @param width
	 * @param height
	 * @param outputBlank whether to return null or a black image when the
	 *                    image parameter is null.
	 * @param renderer the {@link RendererConfiguration} for which to scale.
	 *
	 * @return The scaled image
	 */
	public static byte[] scaleImage(byte[] image, int width, int height, boolean outputBlank, RendererConfiguration renderer) {
		ByteArrayInputStream in = null;
		if (image == null && !outputBlank) {
			return null;
		} else if (image != null) {
			in = new ByteArrayInputStream(image);
		}

		try {
			BufferedImage img;
			if (in != null) {
				img = ImageIO.read(in);
			} else {
				img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			}

			if (img == null) { // ImageIO doesn't support the image format
				return null;
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (renderer != null && renderer.isThumbnailPadding()) {
				Thumbnails.of(img)
					.size(width, height)
					.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
					.outputFormat("JPEG")
					.outputQuality(1.0f)
					.toOutputStream(out);
			} else {
				Thumbnails.of(img)
					.size(width, height)
					.outputFormat("JPEG")
					.outputQuality(1.0f)
					.toOutputStream(out);
			}

			return out.toByteArray();
		} catch (IOException e) {
			LOGGER.debug("Failed to resize image: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}
}
