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

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.dlna.DLNAImage;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.ParseException;

/**
 * This class is simply a byte array for holding an {@link ImageIO} supported
 * image with some additional metadata.
 *
 * @author Nadahar
 */
public class Image implements Serializable {

	private static final long serialVersionUID = 6878185988106188499L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Image.class);
	protected final byte[] bytes;
	protected final ImageInfo imageInfo;

	/**
	 * Creates a new {@link Image} instance.
	 *
	 * @param image the source {@link Image} in a supported format. Format
	 *            support is limited to that of {@link ImageIO}.
	 * @param copy whether this instance should be copied or shared.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Image(Image image, boolean copy) {
		this.bytes = image.getBytes(copy);
		this.imageInfo = copy && image.getImageInfo() != null ? image.getImageInfo().copy() : image.getImageInfo();
	}

	/**
	 * Creates a new {@link Image} instance.
	 *
	 * @param bytes the source image in a supported format. Format support is
	 *            limited to that of {@link ImageIO}.
	 * @param imageInfo the {@link ImageInfo} to store with this {@link Image}.
	 * @param copy whether this instance should be copied or shared.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Image(byte[] bytes, ImageInfo imageInfo, boolean copy) {
		if (copy && bytes != null) {
			this.bytes = new byte[bytes.length];
			System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
		} else {
			this.bytes = bytes;
		}
		this.imageInfo = copy && imageInfo != null ? imageInfo.copy() : imageInfo;
	}

	/**
	 * Creates a new {@link Image} instance.
	 *
	 * @param bytes the image in a supported format. Format support is limited
	 *            to that of {@link ImageIO}.
	 * @param width the width of the image.
	 * @param height the height of the image.
	 * @param format the {@link ImageFormat} of the image.
	 * @param colorModel the {@link ColorModel} of the image.
	 * @param metadata the {@link Metadata} instance describing the image. Will
	 *            be created if {@code null}.
	 * @param imageIOSupport whether or not {@link ImageIO} can read/parse this
	 *            image.
	 * @param copy whether this instance should be copied or shared.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Image(
		byte[] bytes,
		int width,
		int height,
		ImageFormat format,
		ColorModel colorModel,
		Metadata metadata,
		boolean imageIOSupport,
		boolean copy
	) throws ParseException {
		if (copy && bytes != null) {
			this.bytes = new byte[bytes.length];
			System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
		} else {
			this.bytes = bytes;
		}
		if (metadata == null) {
			try {
				metadata = ImagesUtil.getMetadata(this.bytes, format);
			} catch (ImageProcessingException | IOException e) {
				LOGGER.error("Error reading image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
		}
		this.imageInfo = ImageInfo.create(
			width,
			height,
			format,
			bytes != null ? bytes.length : 0,
			colorModel,
			metadata,
			false,
			imageIOSupport
		);
	}

	/**
	 * Creates a new {@link Image} instance.
	 *
	 * @param bytes the image in a supported format. Format support is limited
	 *            to that of {@link ImageIO}.
	 * @param format the {@link ImageFormat} of the image.
	 * @param bufferedImage the {@link BufferedImage} to get non-
	 *            {@link Metadata} metadata from.
	 * @param metadata the {@link Metadata} instance describing the image. Will
	 *            be created if {@code null}.
	 * @param copy whether this instance should be copied or shared.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Image(
		byte[] bytes,
		ImageFormat format,
		BufferedImage bufferedImage,
		Metadata metadata,
		boolean copy
	) throws ParseException {
		if (bufferedImage == null) {
			throw new IllegalArgumentException("bufferedImage cannot be null");
		}

		if (copy && bytes != null) {
			this.bytes = new byte[bytes.length];
			System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
		} else {
			this.bytes = bytes;
		}

		if (metadata == null) {
			try {
				metadata = ImagesUtil.getMetadata(this.bytes, format);
			} catch (ImageProcessingException | IOException e) {
				LOGGER.error("Error while reading image metadata: {}", e.getMessage());
				LOGGER.trace("", e);
				metadata = new Metadata();
			}
		}
		this.imageInfo = ImageInfo.create(
			bufferedImage.getWidth(),
			bufferedImage.getHeight(),
			format,
			bytes != null ? bytes.length : 0,
			bufferedImage.getColorModel(),
			metadata,
			false,
			true
		);
	}

	/**
	 * Converts an image to an {@link Image}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation. Format support is
	 * limited to that of {@link ImageIO}.
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link Image} or {@code null} if the source image
	 *         is {@code null}.
	 * @throws IOException
	 */
	public static Image toImage(InputStream inputStream) throws IOException {
		return toImage(inputStream, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an image to an {@link Image}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @return The populated {@link Image} or {@code null} if the source image
	 *         is {@code null}.
	 * @throws IOException
	 */
	public static Image toImage(byte[] imageByteArray) throws IOException {
		return toImage(imageByteArray, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an {@link Image} to another {@link Image}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation. Format
	 * support is limited to that of {@link ImageIO}.
	 *
	 * @param inputImage the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link Image} or {@code null} if the source image
	 *         is {@code null}.
	 * @throws IOException
	 */
	public static Image toImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		return ImagesUtil.transcodeImage(
			inputImage,
			width,
			height,
			scaleType,
			outputFormat,
			false,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to an {@link Image}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation. Format support is
	 * limited to that of {@link ImageIO}.
	 *
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
	 * @return The populated {@link Image} or {@code null} if the source image
	 *         is {@code null}.
	 * @throws IOException
	 */
	public static Image toImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		return ImagesUtil.transcodeImage(
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			false,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to an {@link Image}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param imageByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link Image} or {@code null} if the source image
	 *         is {@code null}.
	 * @throws IOException
	 */
	public static Image toImage(
			byte[] imageByteArray,
			int width,
			int height,
			ScaleType scaleType,
			ImageFormat outputFormat,
			boolean padToSize
		) throws IOException {
		return ImagesUtil.transcodeImage(
			imageByteArray,
			width,
			height,
			scaleType,
			outputFormat,
			false,
			false,
			padToSize);
	}

	/**
	 * Scales the {@link Image}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled {@link Image} or {@code null} if the source is
	 *         {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image scale(
		int width,
		int height,
		ScaleType scaleType,
		boolean updateMetadata,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return transcode(
			width,
			height,
			scaleType,
			ImageFormat.SOURCE,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts the {@link Image}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 *            Overridden by {@code outputProfile}.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @return The converted {@link Image} or {@code null} if the source is
	 *         {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image transcode(
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail
	) throws IOException {
		return transcode(
			0,
			0,
			null,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			false
		);
	}

	/**
	 * Converts and scales the {@link Image}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to convert to or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 *            Overridden by {@code outputProfile}.
	 * @param dlnaCompliant whether or not the output image should be restricted
	 *            to DLNA compliance. This also means that the output can be
	 *            safely cast to {@link DLNAImage}.
	 * @param dlnaThumbnail whether or not the output image should be restricted
	 *            to DLNA thumbnail compliance. This also means that the output
	 *            can be safely cast to {@link DLNAThumbnail}.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The scaled and/or converted {@link Image} or {@code null} if the
	 *         source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image transcode(
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return ImagesUtil.transcodeImage(
			this.getBytes(false),
			width,
			height,
			scaleType,
			outputFormat,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * Converts and scales the {@link Image} according to the given
	 * {@link DLNAImageProfile}. Preserves aspect ratio and rotates/flips the
	 * image according to Exif orientation. Format support is limited to that of
	 * {@link ImageIO}.
	 *
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
	 * @return The scaled and/or converted {@link Image} or {@code null} if the
	 *         source is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public Image transcode(
		DLNAImageProfile outputProfile,
		boolean dlnaCompliant,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return ImagesUtil.transcodeImage(
			this.getBytes(false),
			0,
			0,
			ScaleType.MAX,
			outputProfile,
			dlnaCompliant,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * @param copy whether or not a new array or a reference to the underlying
	 *             buffer should be returned. If a reference is returned,
	 *             <b>NO MODIFICATIONS must be done to the array!</b>
	 * @return The bytes of this image.
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
	 * @return The {@link ImageInfo} for this image.
	 */
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	/**
	 * @return The width of this image.
	 */
	public int getWidth() {
		return imageInfo != null ? imageInfo.getWidth() : -1;
	}


	/**
	 * @return The height of this image.
	 */
	public int getHeight() {
		return imageInfo != null ? imageInfo.getHeight() : -1;
	}


	/**
	 * @return The {@link ImageFormat} for this image.
	 */
	public ImageFormat getFormat() {
		return imageInfo != null ? imageInfo.getFormat() : null;
	}

	/**
	 * @return The size of this image in bytes.
	 */
	public long getSize() {
		return bytes != null ? bytes.length : 0;
	}

	/**
	 * @return The {@link ColorSpace} for this image.
	 */
	public ColorSpace getColorSpace() {
		return imageInfo != null ? imageInfo.getColorSpace() : null;
	}

	/**
	 * @return The {@link ColorSpaceType} for this image.
	 */
	public ColorSpaceType getColorSpaceType() {
		return imageInfo != null ? imageInfo.getColorSpaceType() : null;
	}

	/**
	 * @return The bits per pixel for this image.
	 *
	 * @see #getBitDepth()
	 */
	public int getBitPerPixel() {
		return imageInfo != null ? imageInfo.getBitsPerPixel() : -1;
	}

	/**
	 * The number of color components describe how many "channels" the color
	 * model has. A grayscale image without alpha has 1, a RGB image without
	 * alpha has 3, a RGB image with alpha has 4 etc.
	 *
	 * @return The number of color components for this image.
	 */
	public int getNumComponents() {
		return imageInfo != null ? imageInfo.getNumComponents() : -1;
	}

	/**
	 * @return The number of bits per color "channel" for this image.
	 *
	 * @see #getBitPerPixel()
	 * @see #getNumColorComponents()
	 */
	public int getBitDepth() {
		return imageInfo != null ? imageInfo.getBitDepth() : -1;
	}

	/**
	 * @return Whether or not {@link ImageIO} can read/parse this image.
	 */
	public boolean isImageIOSupported() {
		return imageInfo != null ? imageInfo.isImageIOSupported() : false;
	}

	/**
	 * @return A copy of this image. The buffer is copied and the metadata recreated.
	 */
	public Image copy() {
		return new Image(bytes, imageInfo, true);
	}

	/**
	 * Override this to add information to {@link #toString} from subclasses.
	 *
	 * @param sb the {@link StringBuilder} to add information to.
	 */
	protected void buildToString(StringBuilder sb) {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(80);
		sb.append(getClass().getSimpleName())
			.append(": [Format = ").append(imageInfo.getFormat())
			.append(", Resolution = ").append(imageInfo.getWidth())
			.append("×").append(imageInfo.getHeight());
		if (getSize() > 0) {
			sb.append(", Size = ").append(bytes != null ? bytes.length : 0);
		}
		buildToString(sb);
		sb.append("]");
		return sb.toString();
	}
}
