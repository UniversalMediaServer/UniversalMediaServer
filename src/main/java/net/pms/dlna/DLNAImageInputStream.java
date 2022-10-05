/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil.ScaleType;

/**
 * This is an {@link InputStream} implementation of {@link DLNAImage}. It holds
 * the stream's content in an internal buffer and is as such not intended to
 * hold very large streams.
 */
public class DLNAImageInputStream extends ByteArrayInputStream {

	/** The {@link ImageInfo} instance describing this {@link DLNAImageInputStream} */
	protected final ImageInfo imageInfo;

	/** The {@link DLNAImageProfile} for this {@link DLNAImageInputStream} */
	protected final DLNAImageProfile profile;

	/**
	 * Don't call this from outside this class or subclass, use
	 * {@link DLNAImageInputStream#toImageInputStream(DLNAImage)}
	 * which handles {@code null} input.
	 *
	 * @param image the input image
	 *
	 * @throws NullPointerException if {@code image} is {@code null}.
	 */
	protected DLNAImageInputStream(DLNAImage image) {
		super(image.getBytes(false));
		this.imageInfo = image.getImageInfo();
		this.profile = image.getDLNAImageProfile();
	}

	/**
	 * Creates a {@link DLNAImageInputStream} where it uses
	 * {@code imageByteArray} as its buffer array. The buffer array is not
	 * copied. The initial value of {@code pos} is {@code 0} and the initial
	 * value of {@code count} is the length of {@code imageByteArray}. Format
	 * support is limited to that of {@link ImageIO}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(byte[] inputByteArray) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(inputByteArray);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} from {@code inputStream}.
	 * <p>
	 * <b>{@code inputStream} is consumed and closed</b>
	 * <p>
	 * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass the
	 * underlying array is used - otherwise the stream is read into memory.
	 * Format support is limited to that of {@link ImageIO}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if
	 *         the source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(InputStream inputStream) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(inputStream);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} from {@code inputStream}.
	 * <p>
	 * <b>{@code inputStream} is consumed and closed</b>
	 * <p>
	 * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass the
	 * underlying array is used - otherwise the stream is read into memory.
	 * Format support is limited to that of {@link ImageIO}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(
		byte[] inputByteArray,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(inputByteArray, outputProfile, padToSize);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} from {@code inputStream}.
	 * <p>
	 * <b>{@code inputStream} is consumed and closed</b>
	 * <p>
	 * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass the
	 * underlying array is used - otherwise the stream is read into memory.
	 * Format support is limited to that of {@link ImageIO}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(
		InputStream inputStream,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(inputStream, outputProfile, padToSize);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} where it uses
	 * {@code imageByteArray} as its buffer array. The buffer is only copied if
	 * any conversion is needed. The initial value of {@code pos} is {@code 0}
	 * and the initial value of {@code count} is the length of
	 * {@code imageByteArray}. Format support is limited to that of
	 * {@link ImageIO}. Preserves aspect ratio and rotates/flips the image
	 * according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(
			inputByteArray,
			width,
			height,
			scaleType,
			outputFormat,
			padToSize
		);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} from {@code inputStream}.
	 * <p>
	 * <b>{@code inputStream} is consumed and closed</b>
	 * <p>
	 * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass the
	 * underlying array is used - otherwise the stream is read into memory.
	 * Format support is limited to that of {@link ImageIO}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImageInputStream toImageInputStream(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		DLNAImage image = DLNAImage.toDLNAImage(
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			padToSize
		);
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	/**
	 * Creates a {@link DLNAImageInputStream} where it uses {@code image}'s
	 * buffer as its buffer array. The buffer array is not copied.
	 *
	 * @param image the input image.
	 * @return The populated {@link DLNAImageInputStream} or {@code null} if the
	 *         source image is {@code null}.
	 */

	public static DLNAImageInputStream toImageInputStream(DLNAImage image) {
		return image != null ? new DLNAImageInputStream(image) : null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(90);
		sb.append("DLNAImageInputStream: Format = ").append(imageInfo.getFormat())
		.append(", Width = ").append(imageInfo.getWidth())
		.append(", Height = ").append(imageInfo.getHeight())
		.append(", Size = ").append(buf != null ? buf.length : 0);
		return sb.toString();
	}

}
