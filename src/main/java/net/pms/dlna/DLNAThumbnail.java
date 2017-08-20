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
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.metadata.Metadata;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.ParseException;

/**
 * This class is simply a byte array for holding an {@link ImageIO} supported
 * image with some additional metadata restricted to the DLNA image media
 * format profiles {@code JPEG_*} and {@code PNG_*}.
 *
 * @see DLNAThumbnailInputStream
 *
 * @author Nadahar
 */
public class DLNAThumbnail extends DLNAImage {

	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAImage.class);
	/*
	 * Please note: This class is packed and stored in the database. Any changes
	 * to the data structure (fields) will invalidate any instances already
	 * stored, and will require a wipe of all rows with a stored instance. The
	 * serialVersionUID value below should also be bumped.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link DLNAThumbnail} instance.
	 *
	 * @param image the source {@link Image} in either JPEG or PNG format
	 *            adhering to the DLNA restrictions for color space and
	 *            compression.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 */
	public DLNAThumbnail(
		Image image,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException {
		super(image, profile, copy);
	}

	/**
	 * Creates a new {@link DLNAThumbnail} instance.
	 *
	 * @param bytes the source image in either JPEG or PNG format adhering to
	 *            the DLNA restrictions for color space and compression.
	 * @param imageInfo the {@link ImageInfo} to store with this
	 *            {@link DLNAThumbnail}.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 */
	public DLNAThumbnail(
		byte[] bytes,
		ImageInfo imageInfo,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException {
		super(bytes, imageInfo, profile, copy);
	}

	/**
	 * Creates a new {@link DLNAThumbnail} instance.
	 *
	 * @param bytes the source image in either JPEG or PNG format adhering to
	 *            the DLNA restrictions for color space and compression.
	 * @param width the width of the image.
	 * @param height the height of the image.
	 * @param format the {@link ImageFormat} of the image.
	 * @param colorModel the {@link ColorModel} of the image.
	 * @param metadata the {@link Metadata} instance describing the image.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	public DLNAThumbnail(
		byte[] bytes,
		int width,
		int height,
		ImageFormat format,
		ColorModel colorModel,
		Metadata metadata,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException, ParseException {
		super(bytes, width, height, format, colorModel, metadata, profile, copy);
	}

	/**
	 * Creates a new {@link DLNAThumbnail} instance.
	 *
	 * @param bytes the source image in either JPEG or PNG format adhering to
	 *            the DLNA restrictions for color space and compression.
	 * @param format the {@link ImageFormat} of the image.
	 * @param bufferedImage the {@link BufferedImage} to get non-
	 *            {@link Metadata} metadata from.
	 * @param metadata the {@link Metadata} instance describing the image.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	public DLNAThumbnail(
		byte[] bytes,
		ImageFormat format,
		BufferedImage bufferedImage,
		Metadata metadata,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException, ParseException {
		super(bytes, format, bufferedImage, metadata, profile, copy);
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAThumbnail}. Output format will
	 * be the same as the source if the source is either JPEG or PNG. Further
	 * restrictions on color space and compression is imposed and conversion
	 * done if necessary. All other formats will be converted to a DLNA
	 * compliant JPEG.
	 *
	 * @param inputImage the source {@link Image}.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(Image inputImage) throws IOException {
		return toThumbnail(inputImage, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}. Output format will be the same as source if
	 * the source is either JPEG or PNG. Further restrictions on color space and
	 * compression is imposed and conversion done if necessary. All other
	 * formats will be converted to a DLNA compliant JPEG. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(InputStream inputStream) throws IOException {
		return toThumbnail(inputStream, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}. Output format will be the same as source if
	 * the source is either JPEG or PNG. Further restrictions on color space and
	 * compression is imposed and conversion done if necessary. All other
	 * formats will be converted to a DLNA compliant JPEG. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(byte[] inputByteArray) throws IOException {
		return toThumbnail(inputByteArray, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. {@code outputProfile} is limited to JPEG or PNG
	 * profiles. If {@code outputProfile} is a GIF profile, the image will be
	 * converted to {@link DLNAImageProfile#JPEG_LRG}.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		Image inputImage,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputImage == null) {
			return null;
		}

		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputImage,
			outputProfile,
			true,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. Format support is limited to that of
	 * {@link ImageIO}. {@code outputProfile} is limited to JPEG or PNG
	 * profiles. If {@code outputProfile} is a GIF profile, the image will be
	 * converted to {@link DLNAImageProfile#JPEG_LRG}. Preserves aspect ratio
	 * and rotates/flips the image according to Exif orientation.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		InputStream inputStream,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputStream == null) {
			return null;
		}

		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputStream,
			outputProfile,
			true,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. Format support is limited to that of
	 * {@link ImageIO}. {@code outputProfile} is limited to JPEG or PNG
	 * profiles. If {@code outputProfile} is a GIF profile, the image will be
	 * converted to {@link DLNAImageProfile#JPEG_LRG}. Preserves aspect ratio
	 * and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		byte[] inputByteArray,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputByteArray == null) {
			return null;
		}

		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputByteArray,
			outputProfile,
			true,
			padToSize
		);
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAThumbnail}. Format support is
	 * limited to that of {@link ImageIO}. {@code outputFormat} is limited to
	 * JPEG or PNG format adhering to the DLNA restrictions for color space and
	 * compression. If {@code outputFormat} doesn't qualify, the image will be
	 * converted to a DLNA compliant JPEG.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		if (inputImage == null) {
			return null;
		}

		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputImage,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			true,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}. {@code outputFormat} is limited to JPEG or
	 * PNG format adhering to the DLNA restrictions for color space and
	 * compression. If {@code outputFormat} doesn't qualify, the image will be
	 * converted to a DLNA compliant JPEG. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		if (inputStream == null) {
			return null;
		}

		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			true,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail}. Format support is limited
	 * to that of {@link ImageIO}. {@code outputFormat} is limited to JPEG or
	 * PNG format adhering to the DLNA restrictions for color space and
	 * compression. If {@code outputFormat} doesn't qualify, the image will be
	 * converted to a DLNA compliant JPEG. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAThumbnail} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnail toThumbnail(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize) throws IOException {
		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			inputByteArray,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			true,
			padToSize
		);
	}

	/**
	 * Converts and scales the thumbnail according to the given
	 * {@link DLNAImageProfile}. Preserves aspect ratio. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the output.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled and/or converted thumbnail, {@code null} if the
	 *         source is {@code null}.
	 * @exception IOException if the operation fails.
	 */
	public DLNAThumbnail transcode(
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		return (DLNAThumbnail) ImagesUtil.transcodeImage(
			this,
			outputProfile,
			true,
			padToSize);
	}

	@Override
	public DLNAThumbnail copy() {
		try {
			return new DLNAThumbnail(bytes, imageInfo, profile, true);
		} catch (DLNAProfileException e) {
			// Should be impossible
			LOGGER.error("Impossible situation in DLNAImage.copy(): {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}
	}
}
