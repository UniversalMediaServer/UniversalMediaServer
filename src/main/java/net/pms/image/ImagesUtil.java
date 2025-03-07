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

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.filters.Canvas;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.pms.PMS;
import net.pms.dlna.DLNAImage;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAImageProfile.DLNAComplianceResult;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.BufferedImageFilter.BufferedImageFilterResult;
import net.pms.image.ImageIOTools.ImageReaderResult;
import net.pms.parsers.MetadataExtractorParser;
import net.pms.util.BufferedImageType;
import net.pms.util.InvalidStateException;
import net.pms.util.Iso639;
import net.pms.util.UnknownFormatException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagesUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImagesUtil.class);

	/** A constant for use for constructing a path to the language "flags" */
	public static final String LANGUAGE_FLAGS_PATH = "/resources/images/store/flags/%s.png";

	private static final HashMap<String, WeakReference<BufferedImage>> LANGUAGE_FLAGS_CACHE = new HashMap<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private ImagesUtil() { }

	/**
	 * Converts a raw Exif version byte array to an integer value.
	 *
	 * @param bytes the raw Exif version bytes.
	 * @return The version number multiplied with 100 (last two digits are
	 *         decimals).
	 */
	public static int parseExifVersion(byte[] bytes) {
		if (bytes == null) {
			return ImageInfo.UNKNOWN;
		}
		StringBuilder stringBuilder = new StringBuilder(4);
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] > 47 && bytes[i] < 58) {
				stringBuilder.append((char) bytes[i]);
			} else if (bytes[i] == 0 && i == bytes.length - 1) {
				// Some buggy C/C++ software doesn't properly format the string
				// so we end up with a null-terminated string without the leading zero.
				stringBuilder.insert(0, '0');
			}
		}
		while (stringBuilder.length() < 4) {
			stringBuilder.append("0");
		}
		try {
			return Integer.parseInt(stringBuilder.toString());
		} catch (NumberFormatException e) {
			LOGGER.debug("Failed to parse Exif version number from: {}", Arrays.toString(bytes));
			return 0;
		}
	}

	/**
	 * Checks if the resolution axes must be swapped if the image is rotated
	 * according to the given Exif orientation.
	 *
	 * @param imageInfo the {@link ImageInfo} whose Exif orientation to evaluate.
	 * @return {@code true} if the axes should be swapped, {@code false}
	 *         otherwise.
	 */
	public static boolean isExifAxesSwapNeeded(ImageInfo imageInfo) {
		return imageInfo != null && isExifAxesSwapNeeded(imageInfo.getExifOrientation());
	}

	/**
	 * Checks if the resolution axes must be swapped if the image is rotated
	 * according to the given Exif orientation.
	 *
	 * @param orientation the Exif orientation to evaluate.
	 * @return {@code true} if the axes should be swapped, {@code false}
	 *         otherwise.
	 */

	public static boolean isExifAxesSwapNeeded(ExifOrientation orientation) {
		if (orientation == null) {
			return false;
		}
		return switch (orientation) {
			case LEFT_TOP, RIGHT_TOP, RIGHT_BOTTOM, LEFT_BOTTOM -> true;
			default -> false;
		};
	}

	/**
	 * Calculates the resolution for the image described by {@code imageInfo} if
	 * it is scaled to {@code scaleWidth} width and {@code scaleHeight} height
	 * while preserving aspect ratio.
	 *
	 * @param imageInfo the {@link ImageInfo} instance describing the image.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param scaleWidth the width to scale to.
	 * @param scaleHeight the height to scale to.
	 * @return A {@link Dimension} with the resulting resolution.
	 */
	public static Dimension calculateScaledResolution(
		ImageInfo imageInfo,
		ScaleType scaleType,
		int scaleWidth,
		int scaleHeight
	) {
		return calculateScaledResolution(
			imageInfo.getWidth(),
			imageInfo.getHeight(),
			scaleType,
			scaleWidth,
			scaleHeight
		);
	}

	/**
	 * Calculates the resolution for the image with {@code actualWidth} width
	 * and {@code actualHeight}) height if it is scaled to {@code scaleWidth}
	 * width and {@code scaleHeight} height while preserving aspect ratio.
	 *
	 * @param actualWidth the width of the source image.
	 * @param actualHeight the height of the source image.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param scaleWidth the width to scale to.
	 * @param scaleHeight the height to scale to.
	 * @return A {@link Dimension} with the resulting resolution.
	 */
	public static Dimension calculateScaledResolution(
		int actualWidth,
		int actualHeight,
		ScaleType scaleType,
		int scaleWidth,
		int scaleHeight
	) {
		if (scaleType == null) {
			throw new NullPointerException("scaleType cannot be null");
		}
		if (actualWidth < 1 || actualHeight < 1) {
			throw new IllegalArgumentException(String.format(
				"actualWidth (%d) and actualHeight (%d) must be positive", actualWidth, actualHeight
			));
		}
		if (scaleWidth < 1 || scaleHeight < 1) {
			throw new IllegalArgumentException(String.format(
				"scaleWidth (%d) and scaleHeight (%d) must be positive", scaleWidth, scaleHeight
			));
		}

		double scale = Math.min((double) scaleWidth / actualWidth, (double) scaleHeight / actualHeight);
		if (scaleType == ScaleType.MAX && scale > 1) {
			// Never scale up for ScaleType.MAX
			scale = 1;
		}
		return new Dimension(
			(int) Math.max(Math.round(actualWidth * scale), 1),
			(int) Math.max(Math.round(actualHeight * scale), 1)
		);
	}

	/**
	 * Retrieves a reference to the underlying byte array from a
	 * {@link ByteArrayInputStream} using reflection. Keep in mind that this
	 * this byte array is shared with the {@link ByteArrayInputStream}.
	 *
	 * @param inputStream the {@link ByteArrayInputStream} whose buffer to retrieve.
	 * @return The byte array or {@code null} if retrieval failed.
	 */
	public static byte[] retrieveByteArray(ByteArrayInputStream inputStream) {
		Field f;
		try {
			f = ByteArrayInputStream.class.getDeclaredField("buf");
			f.setAccessible(true);
			return (byte[]) f.get(inputStream);
		} catch (RuntimeException | NoSuchFieldException | IllegalAccessException e) {
			LOGGER.debug("Unexpected reflection failure in retrieveByteArray(): {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
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
			return IOUtils.EMPTY_BYTE_ARRAY;
		}

		// Avoid copying the data if it's already a byte array
		try (inputStream) {
			if (inputStream instanceof ByteArrayInputStream byteArrayInputStream) {
				byte[] bytes = retrieveByteArray(byteArrayInputStream);
				if (bytes != null) {
					return bytes;
				}
				// Reflection failed, use IOUtils to read the stream instead
			}
			return IOUtils.toByteArray(inputStream);
		}
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		Image inputImage,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputImage,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false,
			filterChain
		);
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		InputStream inputStream,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputStream,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false,
			filterChain
		);
	}

	/**
	 * Converts an image to a different {@link ImageFormat}. Rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputFormat the {@link ImageFormat} to convert to. If this is
	 *            {@link ImageFormat#SOURCE} or {@code null} this has no effect.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image convertImage(
		byte[] inputByteArray,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false,
			filterChain
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image scaleImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputImage,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image scaleImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputStream,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Scales an image to the given dimensions. Scaling can be with or without
	 * padding. Preserves aspect ratio and rotates/flips the image according to
	 * Exif orientation. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width.
	 * @param height the new height.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */

	public static Image scaleImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputImage,
			0,
			0,
			null,
			outputProfile,
			true,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputStream,
			0,
			0,
			null,
			outputProfile,
			true,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and if necessary scales an image to comply with a
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The converted image or {@code null} if the source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			0,
			0,
			null,
			outputProfile,
			true,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			null,
			inputImage,
			null,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			null,
			null,
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			null,
			null,
			width,
			height,
			scaleType,
			outputFormat,
			null,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			null,
			inputImage,
			null,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			null,
			null,
			inputStream,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static Image transcodeImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		return transcodeImage(
			inputByteArray,
			null,
			null,
			width,
			height,
			scaleType,
			null,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize,
			filterChain
		);
	}

	/**
	 * Converts and scales an image in one operation. Scaling can be with or
	 * without padding. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}. Only one of the three input arguments may be used in any
	 * given call. Note that {@code outputProfile} overrides
	 * {@code outputFormat}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param inputImage the source {@link Image}.
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 *            Overridden by {@code outputProfile}.
	 * @param outputProfile the {@link DLNAImageProfile} to convert to. This
	 *            overrides {@code outputFormat}.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @param filterChain a {@link BufferedImageFilterChain} to apply during the
	 *            operation or {@code null}.
	 * @return The scaled and/or converted image or {@code null} if the source
	 *         is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	protected static Image transcodeImage(
		byte[] inputByteArray,
		Image inputImage,
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize,
		BufferedImageFilterChain filterChain
	) throws IOException {
		if (inputByteArray == null && inputStream == null && inputImage == null) {
			return null;
		}
		if (
			(inputByteArray != null && inputImage != null) ||
			(inputByteArray != null && inputStream != null) ||
			(inputImage != null && inputStream != null)
		) {
			throw new IllegalArgumentException("Use either inputByteArray, inputImage or inputStream");
		}

		boolean trace = LOGGER.isTraceEnabled();
		if (trace) {
			StringBuilder sb = new StringBuilder();
			if (scaleType != null) {
				sb.append("ScaleType = ").append(scaleType);
			}
			if (width > 0 && height > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("Width = ").append(width).append(", Height = ").append(height);
			}
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("PadToSize = ").append(padToSize ? "True" : "False");
			if (filterChain != null && !filterChain.isEmpty()) {
				sb.append(", Filters: ");
				boolean first = true;
				for (BufferedImageFilter filter : filterChain) {
					if (first) {
						first = false;
					} else {
						sb.append(", ");
					}
					sb.append(filter);
				}
			}
			LOGGER.trace(
				"Converting {} image source to {} format and type {} using the following parameters: {}",
				inputByteArray != null ? "byte array" : inputImage != null ? "Image" : "input stream",
				outputProfile != null ? outputProfile : outputFormat,
				dlnaThumbnail ? "DLNAThumbnail" : dlnaCompliant ? "DLNAImage" : "Image",
				sb
			);
		}

		ImageIO.setUseCache(false);
		dlnaCompliant = dlnaCompliant || dlnaThumbnail;

		if (inputImage != null) {
			inputByteArray = inputImage.getBytes(false);
		} else if (inputStream != null) {
			inputByteArray = ImagesUtil.toByteArray(inputStream);
		}

		// outputProfile overrides outputFormat
		if (outputProfile != null) {
			if (dlnaThumbnail && outputProfile.equals(DLNAImageProfile.GIF_LRG)) {
				outputProfile = DLNAImageProfile.JPEG_LRG;
			}
			// Default to correct ScaleType for the profile
			if (scaleType == null) {
				if (DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile)) {
					scaleType = ScaleType.EXACT;
				} else {
					scaleType = ScaleType.MAX;
				}
			}
			outputFormat = ImageFormat.toImageFormat(outputProfile);
			width = width > 0 ? width : outputProfile.getMaxWidth();
			height = height > 0 ? height : outputProfile.getMaxHeight();
		} else if (scaleType == null) {
			scaleType = ScaleType.MAX;
		}

		ImageReaderResult inputResult;
		try {
			inputResult = ImageIOTools.read(new ByteArrayInputStream(inputByteArray));
		} catch (IIOException e) {
			throw new UnknownFormatException("Unable to read image format", e);
		}

		if (inputResult.bufferedImage == null || inputResult.imageFormat == null) { // ImageIO doesn't support the image format
			throw new UnknownFormatException("Failed to transform image because the source format is unknown");
		}

		if (outputFormat == null || outputFormat == ImageFormat.SOURCE) {
			outputFormat = inputResult.imageFormat;
		}

		BufferedImage bufferedImage = inputResult.bufferedImage;
		boolean reencode = filterChain != null && !filterChain.isEmpty();

		if (outputProfile == null && dlnaCompliant) {
			// Override output format to one valid for DLNA, defaulting to PNG
			// if the source image has alpha and JPEG if not.
			switch (outputFormat) {
				case GIF -> {
					if (dlnaThumbnail) {
						outputFormat = ImageFormat.JPEG;
					}
				}
				case JPEG, PNG -> {
					//nothing to do
				}
				default -> {
					if (bufferedImage.getColorModel().hasAlpha()) {
						outputFormat = ImageFormat.PNG;
					} else {
						outputFormat = ImageFormat.JPEG;
					}
				}
			}
		}

		Metadata metadata = null;
		ExifOrientation orientation;
		if (inputImage != null && inputImage.getImageInfo() != null) {
			orientation = inputImage.getImageInfo().getExifOrientation();
		} else {
			try {
				metadata = MetadataExtractorParser.getMetadata(inputByteArray, inputResult.imageFormat);
			} catch (IOException | ImageProcessingException e) {
				LOGGER.error("Failed to read input image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
			if (metadata == null) {
				metadata = new Metadata();
			}
			orientation = MetadataExtractorParser.parseExifOrientation(metadata);
		}

		if (orientation != ExifOrientation.TOP_LEFT) {
			// Rotate the image before doing all the other checks
			BufferedImage oldBufferedImage = bufferedImage;
			bufferedImage = Thumbnails.of(bufferedImage)
				.scale(1.0d)
				.addFilter(ExifFilterUtils.getFilterForOrientation(orientation.getThumbnailatorOrientation()))
				.asBufferedImage();
			oldBufferedImage.flush();
			// Re-parse the metadata after rotation as these are newly generated.
			ByteArrayOutputStream tmpOutputStream = new ByteArrayOutputStream(inputByteArray.length);
			Thumbnails.of(bufferedImage).scale(1.0d).outputFormat(outputFormat.toString()).toOutputStream(tmpOutputStream);
			try {
				metadata = MetadataExtractorParser.getMetadata(tmpOutputStream.toByteArray(), outputFormat);
			} catch (IOException | ImageProcessingException e) {
				LOGGER.debug("Failed to read rotated image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
			if (metadata == null) {
				metadata = new Metadata();
			}
			reencode = true;
		}

		if (outputProfile == null && dlnaCompliant) {
			// Set a suitable output profile.
			if (width < 1 || height < 1) {
				outputProfile = DLNAImageProfile.getClosestDLNAProfile(
					bufferedImage.getWidth(),
					bufferedImage.getHeight(),
					outputFormat,
					true
				);
				width = outputProfile.getMaxWidth();
				height = outputProfile.getMaxHeight();
			} else {
				outputProfile = DLNAImageProfile.getClosestDLNAProfile(
					calculateScaledResolution(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						scaleType,
						width,
						height
					),
					outputFormat,
					true
				);
				width = Math.min(width, outputProfile.getMaxWidth());
				height = Math.min(height, outputProfile.getMaxHeight());
			}
			if (DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile)) {
				scaleType = ScaleType.EXACT;
			}
		}

		boolean convertColors =
			bufferedImage.getType() == BufferedImageType.TYPE_CUSTOM.getTypeId() ||
			bufferedImage.getType() == BufferedImageType.TYPE_BYTE_BINARY.getTypeId() ||
			bufferedImage.getColorModel().getColorSpace().getType() != ColorSpaceType.TYPE_RGB.getTypeId();

		// Impose DLNA format restrictions
		if (!reencode && outputFormat == inputResult.imageFormat && outputProfile != null) {
			DLNAComplianceResult complianceResult;
			switch (outputFormat) {
				case GIF, JPEG, PNG -> {
					ImageInfo imageInfo;
					// metadata is only null at this stage if inputImage != null and no rotation was necessary
					if (metadata == null) {
						// TODO: why imageInfo is assign and reassing just after
						imageInfo = inputImage.getImageInfo();
					}
					imageInfo = ImageInfo.create(
							bufferedImage.getWidth(),
							bufferedImage.getHeight(),
							inputResult.imageFormat,
							ImageInfo.SIZE_UNKNOWN,
							bufferedImage.getColorModel(),
							metadata,
							false,
							true
					);
					complianceResult = DLNAImageProfile.checkCompliance(imageInfo, outputProfile);
				}
				default -> throw new IllegalStateException("Unexpected image format: " + outputFormat);
			}
			reencode = reencode || convertColors || !complianceResult.isFormatCorrect() || !complianceResult.isColorsCorrect();
			if (!complianceResult.isResolutionCorrect()) {
				width = width > 0 && complianceResult.getMaxWidth() > 0 ?
					Math.min(width, complianceResult.getMaxWidth()) :
						width > 0 ? width : complianceResult.getMaxWidth();
				height = height > 0 && complianceResult.getMaxHeight() > 0 ?
					Math.min(height, complianceResult.getMaxHeight()) :
						height > 0 ? height : complianceResult.getMaxHeight();
			}
			if (trace) {
				if (complianceResult.isAllCorrect()) {
					LOGGER.trace("Image conversion DLNA compliance check: The source image is compliant");
				} else {
					LOGGER.trace(
						"Image conversion DLNA compliance check for {}: " +
						"The source image colors are {}, format is {} and resolution ({} x {}) is {}.\nFailures:\n  {}",
						outputProfile,
						complianceResult.isColorsCorrect() ? "compliant" : "non-compliant",
						complianceResult.isFormatCorrect() ? "compliant" : "non-compliant",
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						complianceResult.isResolutionCorrect() ? "compliant" : "non-compliant",
						StringUtils.join(complianceResult.getFailures(), "\n  ")
					);
				}
			}
		}

		if (convertColors) {
			// Preserve alpha channel if the output format supports it
			BufferedImageType outputImageType;
			if (
				(outputFormat == ImageFormat.PNG || outputFormat == ImageFormat.PSD) &&
				bufferedImage.getColorModel().getNumComponents() == 4
			) {
				outputImageType = bufferedImage.isAlphaPremultiplied() ?
					BufferedImageType.TYPE_4BYTE_ABGR_PRE :
					BufferedImageType.TYPE_4BYTE_ABGR;
			} else {
				outputImageType = BufferedImageType.TYPE_3BYTE_BGR;
			}

			BufferedImage convertedImage = new BufferedImage(
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				outputImageType.getTypeId()
			);
			ColorConvertOp colorConvertOp = new ColorConvertOp(null);
			colorConvertOp.filter(bufferedImage, convertedImage);
			bufferedImage.flush();
			bufferedImage = convertedImage;
			reencode = true;
		}

		if (width < 1 ||
			height < 1 ||
			(
				scaleType == ScaleType.MAX &&
				bufferedImage.getWidth() <= width &&
				bufferedImage.getHeight() <= height
			) ||
			(
				scaleType == ScaleType.EXACT &&
				bufferedImage.getWidth() == width &&
				bufferedImage.getHeight() == height
			)
		) {
			//No resize, just convert
			if (!reencode && inputResult.imageFormat == outputFormat) {
				// Nothing to do, just return source

				// metadata is only null at this stage if inputImage != null
				Image result;
				if (dlnaThumbnail) {
					result = metadata == null ?
						new DLNAThumbnail(inputImage, outputProfile, false) :
						new DLNAThumbnail(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else if (dlnaCompliant) {
					result = metadata == null ?
						new DLNAImage(inputImage, outputProfile, false) :
						new DLNAImage(inputByteArray, outputFormat, bufferedImage, metadata, outputProfile, false);
				} else {
					result = metadata == null ?
						new Image(inputImage, false) :
						new Image(inputByteArray, outputFormat, bufferedImage, metadata, false);
				}
				bufferedImage.flush();

				if (trace) {
					LOGGER.trace(
						"No conversion is needed, returning source image with width = {}, height = {} and output {}.",
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						dlnaCompliant && outputProfile != null ? "profile: " + outputProfile : "format: " + outputFormat
					);
				}
				return result;
			} else if (!reencode) {
				// Convert format
				reencode = true;
			}
		} else {
			boolean force = DLNAImageProfile.JPEG_RES_H_V.equals(outputProfile);
			BufferedImage oldBufferedImage = bufferedImage;
			if (padToSize && force) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.forceSize(width, height)
					.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
					.asBufferedImage();
			} else if (padToSize) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.size(width, height)
					.addFilter(new Canvas(width, height, Positions.CENTER, Color.BLACK))
					.asBufferedImage();
			} else if (force) {
				bufferedImage = Thumbnails.of(bufferedImage)
					.forceSize(width, height)
					.asBufferedImage();
			} else {
				bufferedImage = Thumbnails.of(bufferedImage)
					.size(width, height)
					.asBufferedImage();
			}
			oldBufferedImage.flush();
		}

		// Apply filters
		if (filterChain != null && !filterChain.isEmpty()) {
			BufferedImageFilterResult filterResult = filterChain.filter(bufferedImage);
			if (!filterResult.isOriginalInstance()) {
				bufferedImage.flush();
			}
			bufferedImage = filterResult.getBufferedImage();
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Thumbnails.of(bufferedImage)
			.scale(1.0d)
			.outputFormat(outputFormat.toString())
			.outputQuality(0.8f)
			.toOutputStream(outputStream);

		byte[] outputByteArray = outputStream.toByteArray();

		Image result;
		if (dlnaThumbnail) {
			result = new DLNAThumbnail(
				outputByteArray,
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				outputFormat,
				null,
				null,
				outputProfile,
				false
			);
		} else if (dlnaCompliant) {
			result = new DLNAImage(
				outputByteArray,
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				outputFormat,
				null,
				null,
				outputProfile,
				false
			);
		} else {
			result = new Image(
				outputByteArray,
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				outputFormat,
				null,
				null,
				true,
				false
			);
		}

		if (trace) {
			StringBuilder sb = new StringBuilder();
			sb.append("Convert colors = ").append(convertColors ? "True" : "False")
			.append(", Re-encode = ").append(reencode ? "True" : "False");

			LOGGER.trace(
				"Finished converting {}x{} {} image{}. Output image resolution: {}x{}, {}. Flags: {}",
				inputResult.width,
				inputResult.height,
				inputResult.imageFormat,
				orientation != ExifOrientation.TOP_LEFT ? " with orientation " + orientation : "",
				bufferedImage.getWidth(),
				bufferedImage.getHeight(),
				dlnaCompliant && outputProfile != null ? "profile: " + outputProfile : "format: " + outputFormat,
				sb
			);
		}

		bufferedImage.flush();
		return result;
	}

	/**
	 * @param fileName the "file name" part of the HTTP request.
	 * @return The "decoded" {@link ImageProfile} or
	 *         {@link ImageProfile#JPEG_TN} if the parsing fails.
	 */
	public static DLNAImageProfile parseThumbRequest(String fileName) {
		if (fileName.startsWith("thumbnail0000")) {
			fileName = fileName.substring(13);

			return parseImageRequest(fileName, DLNAImageProfile.JPEG_TN);
		}
		LOGGER.warn("Could not parse thumbnail DLNAImageProfile from \"{}\"");
		return DLNAImageProfile.JPEG_TN;
	}

	/**
	 * @param fileName the "file name" part of the HTTP request.
	 * @param defaultProfile the {@link DLNAImageProfile} to return if parsing
	 *            fails.
	 * @return The "decoded" {@link ImageProfile} or {@code defaultProfile} if
	 *         the parsing fails.
	 */
	public static DLNAImageProfile parseImageRequest(String fileName, DLNAImageProfile defaultProfile) {
		Matcher matcher = Pattern.compile("^([A-Z]+_(?:(?!R)[A-Z]+|RES_?\\d+[Xx_]\\d+))_").matcher(fileName);
		if (matcher.find()) {
			DLNAImageProfile imageProfile = DLNAImageProfile.toDLNAImageProfile(matcher.group(1));
			if (imageProfile == null) {
				LOGGER.warn("Could not parse DLNAImageProfile from \"{}\"", matcher.group(1));
			} else {
				return imageProfile;
			}
		} else {
			LOGGER.debug("Embedded DLNAImageProfile not found in \"{}\"", fileName);
		}
		return defaultProfile;
	}

	/**
	 * Retrieves the bit depth from an array of bit depths for all components.
	 * The last component's bit depth is allowed to be different from the rest,
	 * but the others must be equal or an {@link InvalidStateException} is
	 * thrown.
	 *
	 * @param bitDepthArray the array of bit depths for all components.
	 * @return The bit depth value if it's constant for all but the last
	 *         component.
	 * @throws InvalidStateException If the bit depth values vary or
	 *             {@code bitDepthArray} is null or empty.
	 */
	public static int getBitDepthFromArray(int[] bitDepthArray) throws InvalidStateException {
		if (bitDepthArray == null || bitDepthArray.length == 0) {
			throw new InvalidStateException("The bit depth array cannot be null or empty");
		}

		try {
			return getConstantIntArrayValue(bitDepthArray);
		} catch (InvalidStateException e) {
			// Allow the last value to be different in it's the alpha
			if (bitDepthArray.length > 1) {
				int[] tmpArray = new int[bitDepthArray.length - 1];
				System.arraycopy(bitDepthArray, 0, tmpArray, 0, bitDepthArray.length - 1);
				return getConstantIntArrayValue(tmpArray);
			}
			throw e;
		}
	}

	/**
	 * Retrieves the bit depth from an array of bit depths for all components.
	 * The last component's bit depth is allowed to be different from the rest,
	 * but the others must be equal or an {@link InvalidStateException} is
	 * thrown.
	 *
	 * @param bitDepthArray the array of bit depths for all components.
	 * @return The bit depth value if it's constant for all but the last
	 *         component.
	 * @throws InvalidStateException If the bit depth values vary or
	 *             {@code bitDepthArray} is null or empty.
	 */
	public static byte getBitDepthFromArray(byte[] bitDepthArray) throws InvalidStateException {
		if (bitDepthArray == null || bitDepthArray.length == 0) {
			throw new InvalidStateException("The bit depth array cannot be null or empty");
		}

		try {
			return getConstantByteArrayValue(bitDepthArray);
		} catch (InvalidStateException e) {
			// Allow the last value to be different in it's the alpha
			if (bitDepthArray.length > 1) {
				byte[] tmpArray = new byte[bitDepthArray.length - 1];
				System.arraycopy(bitDepthArray, 0, tmpArray, 0, bitDepthArray.length - 1);
				return getConstantByteArrayValue(tmpArray);
			}
			throw e;
		}
	}

	/**
	 * Verifies and retrieves a constant integer value from an integer array. If
	 * the {@code intArray}'s values aren't all the same or {@code intArray} is
	 * {@code null} or empty, an {@link InvalidStateException} is thrown.
	 *
	 * @param intArray the integer array from which to extract the constant
	 *            integer value.
	 * @return The constant integer value.
	 * @throws InvalidStateException if the values aren't equal or the array is
	 *             {@code null} or empty.
	 */
	@SuppressWarnings("null")
	public static int getConstantIntArrayValue(int[] intArray) throws InvalidStateException {
		if (intArray == null || intArray.length == 0) {
			throw new InvalidStateException("The array cannot be null or empty");
		}

		if (intArray.length == 1) {
			return intArray[0];
		}

		Integer result = null;
		for (int i : intArray) {
			if (result == null) {
				result = i;
			} else {
				if (result != i) {
					throw new InvalidStateException("The array doesn't have a constant value: " + Arrays.toString(intArray));
				}
			}
		}
		return result;
	}

	/**
	 * Verifies and retrieves a constant byte value from a byte array. If
	 * the {@code byteArray}'s values aren't all the same or {@code byteArray} is
	 * {@code null} or empty, an {@link InvalidStateException} is thrown.
	 *
	 * @param byteArray the byte array from which to extract the constant
	 *            byte value.
	 * @return The constant byte value.
	 * @throws InvalidStateException if the values aren't equal or the array is
	 *             {@code null} or empty.
	 */
	@SuppressWarnings("null")
	public static byte getConstantByteArrayValue(byte[] byteArray) throws InvalidStateException {
		if (byteArray == null || byteArray.length == 0) {
			throw new InvalidStateException("The array cannot be null or empty");
		}

		if (byteArray.length == 1) {
			return byteArray[0];
		}

		Byte result = null;
		for (byte b : byteArray) {
			if (result == null) {
				result = b;
			} else {
				if (result != b) {
					throw new InvalidStateException("The array doesn't have a constant value: " + Arrays.toString(byteArray));
				}
			}
		}
		return result;
	}

	/**
	 * Returns the "flag" {@link BufferedImage} for the specified language code
	 * if available. Also handles caching of the "flags" in as
	 * {@link WeakReference}s. The requested {@link BufferedImage} is retrieved
	 * from cache if available. If not, the image is read from disk and the
	 * result cached and returned. Invalid language codes return {@code null}.
	 * Valid language codes where the "flag" isn't found or can't be read, is
	 * permanently cached as not available and {@code null} is returned. This
	 * means that images not found or with read errors won't be attempted read
	 * more than once.
	 *
	 * @param languageCode the language code (2 or 3 letter) whose "flag" to
	 *            get.
	 * @return The {@link BufferedImage} with the "flag" representing this
	 *         language code or {@code null}.
	 */
	public static BufferedImage getLanguageFlag(String languageCode) {
		if (StringUtils.isBlank(languageCode)) {
			return null;
		}
		languageCode = Iso639.getISO639_2Code(languageCode.toLowerCase(Locale.ROOT));
		if (languageCode == null) {
			return null;
		}

		synchronized (LANGUAGE_FLAGS_CACHE) {
			// Remove stale entries from the cache, leave null entries
			// to indicate that the underlying file doesn't exist.
			Iterator<Entry<String, WeakReference<BufferedImage>>> cacheIterator = LANGUAGE_FLAGS_CACHE.entrySet().iterator();
			while (cacheIterator.hasNext()) {
				Entry<String, WeakReference<BufferedImage>> entry = cacheIterator.next();
				if (entry.getValue() != null && entry.getValue().get() == null) {
					cacheIterator.remove();
				}
			}

			if (LANGUAGE_FLAGS_CACHE.containsKey(languageCode)) {
				WeakReference<BufferedImage> reference = LANGUAGE_FLAGS_CACHE.get(languageCode);
				if (reference == null) {
					// The file doesn't exist.
					return null;
				}
				BufferedImage result = reference.get();
				if (result != null) {
					// Cached instance found
					return result;
				}
				// Cached instance must have been GC'ed since the cleanup above, remove the stale record.
				LANGUAGE_FLAGS_CACHE.remove(languageCode);
			}

			InputStream inputStream = PMS.class.getResourceAsStream(String.format(LANGUAGE_FLAGS_PATH, languageCode));
			if (inputStream != null) {
				try {
					BufferedImage result = ImageIO.read(inputStream);
					LANGUAGE_FLAGS_CACHE.put(languageCode, new WeakReference<>(result));
					return result;
				} catch (Exception e) {
					// Catch Exception (instead of IOException) because ImageIO has the
					// nasty habit of throwing RuntimeExceptions if something goes wrong.
					LOGGER.warn(
						"An error occurred while trying to read the language flag for \"{}\" (\"{}\"): {}",
						languageCode,
						String.format(LANGUAGE_FLAGS_PATH, languageCode),
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
			} else {
				LOGGER.debug(
					"Warning: Failed to find the language flag for \"{}\" (\"{}\")",
					languageCode,
					String.format(LANGUAGE_FLAGS_PATH, languageCode)
				);
			}

			// The language flag was not found or failed to read, cache "null"
			LANGUAGE_FLAGS_CACHE.put(languageCode, null);
			return null;
		}
	}

	/**
	 * Defines how image scaling is done. {@link #EXACT} will scale the image
	 * to the given resolution if the aspect ratio is the same, or scale the
	 * image so that it fills the given resolution as much as possible while
	 * keeping the aspect ratio if the aspect radio doesn't match. {@link #MAX}
	 * will make sure that the output image doesn't exceed the given
	 * resolution. {@link #MAX} will never upscale, only downscale if required.
	 */
	public enum ScaleType {
		/**
		 * Scale the image to the given resolution while keeping the aspect
		 * ratio. If the aspect ratio mismatch, one axis will be scaled to the
		 * given resolution while the other will be smaller.
		 */
		EXACT,
		/**
		 * Scale the image so that it is no bigger than the given resolution
		 * while keeping aspect ratio. Will never upscale.
		 */
		MAX
	}

	/**
	 * A {@link BufferedImageFilter} implementation that adds an audio language
	 * flag to a {@link BufferedImage}.
	 *
	 * @author Nadahar
	 */
	public static class AudioFlagFilter extends NonGeometricBufferedImageOp implements BufferedImageFilter {

		private final String languageCode;

		/**
		 * Creates a new filter instance for the specified language.
		 *
		 * @param languageCode the 2 or 3 letter language code for the language.
		 * @param hints the {@link RenderingHints} or {@code null}.
		 */
		public AudioFlagFilter(String languageCode, RenderingHints hints) {
			super(hints);
			this.languageCode = languageCode;
		}

		@Override
		public String getDescription() {
			return toString();
		}

		@Override
		public String toString() {
			return "Audio language flag for \"" + languageCode + "\"";
		}

		/**
		 * @return The language code specified in the constructor.
		 */
		public String getLanguageCode() {
			return languageCode;
		}

		@Override
		public BufferedImage filter(BufferedImage source, BufferedImage destination) {
			return filter(source, destination, true).getBufferedImage();
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source) {
			return filter(source, null, true);
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source, BufferedImage destination, boolean modifySource) {
			BufferedImage flag = getLanguageFlag(languageCode);
			if (flag == null) {
				return new BufferedImageFilterResult(source, false, true);
			}

			boolean sameInstance = true;
			boolean nullSource = source == null;
			// Use the flag image if the input is missing
			if (source == null) {
				source = flag;
				sameInstance = false;
			}

			// Create new destination or reuse source according to modifySource
			if (destination == null) {
				if (modifySource) {
					destination = source;
				} else {
					destination = createCompatibleDestImage(source, null);
					sameInstance = false;
				}
			} else {
				sameInstance = source == destination;
			}

			// Return the flag itself
			if (nullSource && source == destination) {
				return new BufferedImageFilterResult(flag, true, false);
			}

			int flagHorizontalResolution;
			int flagVerticalResolution;
			int flagHorizontalPosition;
			int flagVerticalPosition;
			if (nullSource) {
				flagHorizontalResolution = source.getWidth();
				flagVerticalResolution = source.getHeight();
				flagHorizontalPosition = 0;
				flagVerticalPosition = 0;
			} else {
				double scale = Math.min(
					(double) source.getWidth() / flag.getWidth(),
					(double) source.getHeight() / flag.getHeight()
				);
				flagHorizontalResolution = (int) Math.round(flag.getWidth() * scale);
				flagVerticalResolution = (int) Math.round(flag.getHeight() * scale);
				flagHorizontalPosition = (source.getWidth() - flagHorizontalResolution) / 2;
				flagVerticalPosition = (source.getHeight() - flagVerticalResolution) / 2;
			}

			Graphics2D g2d = source.createGraphics();
			try {
				if (hints != null) {
					g2d.setRenderingHints(hints);
				}
				if (source != destination) {
					g2d.drawImage(source, 0, 0, null);
				}
				g2d.drawImage(
					flag,
					flagHorizontalPosition,
					flagVerticalPosition,
					flagHorizontalResolution,
					flagVerticalResolution,
					null
				);
			} finally {
				g2d.dispose();
			}

			return new BufferedImageFilterResult(source, true, sameInstance);
		}
	}

	/**
	 * A {@link BufferedImageFilter} implementation that adds a subtitles
	 * language flag to a {@link BufferedImage}.
	 *
	 * @author Nadahar
	 */
	public static class SubtitlesFlagFilter extends NonGeometricBufferedImageOp implements BufferedImageFilter {

		private final String languageCode;

		/**
		 * Creates a new filter instance for the specified language.
		 *
		 * @param languageCode the 2 or 3 letter language code for the language.
		 * @param hints the {@link RenderingHints} or {@code null}.
		 */
		public SubtitlesFlagFilter(String languageCode, RenderingHints hints) {
			super(hints);
			this.languageCode = languageCode;
		}

		@Override
		public String getDescription() {
			return toString();
		}

		@Override
		public String toString() {
			return "Subtitles language flag for \"" + languageCode + "\"";
		}

		/**
		 * @return The language code specified in the constructor.
		 */
		public String getLanguageCode() {
			return languageCode;
		}

		@Override
		public BufferedImage filter(BufferedImage source, BufferedImage destination) {
			return filter(source, destination, true).getBufferedImage();
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source) {
			return filter(source, null, true);
		}

		@Override
		public BufferedImageFilterResult filter(BufferedImage source, BufferedImage destination, boolean modifySource) {
			BufferedImage flag = getLanguageFlag(languageCode);
			if (flag == null) {
				return new BufferedImageFilterResult(source, false, true);
			}

			boolean sameInstance = true;
			// Create a blank image if the input is missing
			if (source == null) {
				source = createCompatibleDestImage(flag, null);
				sameInstance = false;
			}

			// Create new destination or reuse source according to modifySource
			if (destination == null) {
				if (modifySource) {
					destination = source;
				} else {
					destination = createCompatibleDestImage(source, null);
					sameInstance = false;
				}
			} else {
				sameInstance = source == destination;
			}

			double scale = Math.min(
				(double) source.getWidth() / (flag.getWidth() * 2),
				(double) source.getHeight() / (flag.getHeight() * 2)
			);
			// Never let the subtitle flag be bigger than 75% of the audio flag on small images
			int maxRelativeHorizontalResolution = (int) Math.round(flag.getWidth() * scale * 1.5);
			int maxRelativeVerticalResolution = (int) Math.round(flag.getHeight() * scale * 1.5);

			// Reduce the downscaling so it won't shrink below 64 x 64 pixels,
			// unless that is bigger than 75% of the audio flag's size, in which
			// case 75% is used.
			int flagHorizontalResolution = (int) Math.max(Math.round(flag.getWidth() * scale), Math.min(64, maxRelativeHorizontalResolution));
			int flagVerticalResolution = (int) Math.max(Math.round(flag.getHeight() * scale), Math.min(64, maxRelativeVerticalResolution));
			int flagHorizontalPosition = source.getWidth() - flagHorizontalResolution;
			int flagVerticalPosition = source.getHeight() - flagVerticalResolution;

			Graphics2D g2d = source.createGraphics();
			try {
				if (hints != null) {
					g2d.setRenderingHints(hints);
				}
				if (source != destination) {
					g2d.drawImage(source, 0, 0, null);
				}
				g2d.drawImage(
					flag,
					flagHorizontalPosition,
					flagVerticalPosition,
					flagHorizontalResolution,
					flagVerticalResolution,
					null
				);
			} finally {
				g2d.dispose();
			}

			return new BufferedImageFilterResult(source, true, sameInstance);
		}
	}
}
