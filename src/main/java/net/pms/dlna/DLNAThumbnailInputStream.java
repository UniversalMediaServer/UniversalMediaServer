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

import java.awt.color.ColorSpace;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ColorSpaceType;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;

/**
 * This is an {@link InputStream} implementation of {@link DLNAThumbnail}. It
 * holds the stream's content in an internal buffer and is as such not intended
 * to hold very large streams.
 */
public class DLNAThumbnailInputStream extends ByteArrayInputStream {

	protected final ImageInfo imageInfo;
	protected final DLNAImageProfile profile;

	/**
	 * Creates a {@link DLNAThumbnailInputStream} where it uses
	 * {@code imageByteArray} as its buffer array. The buffer array is not
	 * copied. The initial value of {@code pos} is {@code 0} and the initial
	 * value of {@code count} is the length of {@code imageByteArray}. Format
	 * support is limited to that of {@link ImageIO}. Preserves aspect ratio and
	 * rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null} if
	 *         the source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(byte[] inputByteArray) throws IOException {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(inputByteArray);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
	 * Creates a {@link DLNAThumbnailInputStream} from {@code inputStream}.
	 * <p>
	 * <b>{@code inputStream} is consumed and closed</b>
	 * <p>
	 * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass the
	 * underlying array is used - otherwise the stream is read into memory.
	 * Format support is limited to that of {@link ImageIO}. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null} if
	 *         the source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(InputStream inputStream) throws IOException {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(inputStream);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
	 * Creates a {@link DLNAThumbnailInputStream} where it uses
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
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null} if
	 *         the source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(
			inputByteArray,
			width,
			height,
			scaleType,
			outputFormat,
			padToSize
		);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
	 * Creates a {@link DLNAThumbnailInputStream} from {@code inputStream}.
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
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null} if
	 *         the source image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			padToSize
		);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

    /**
     * Creates a {@link DLNAThumbnailInputStream} where it uses
     * {@code thumbnail}'s buffer as its buffer array. The buffer array is
     * not copied.
     *
     * @param thumbnail the input thumbnail.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null}
	 *         if the source thumbnail is {@code null}.
     */

	public static DLNAThumbnailInputStream toThumbnailInputStream(DLNAThumbnail thumbnail) {
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
	 * Don't call this from outside this class or subclass, use
	 * {@link DLNAThumbnailInputStream#toThumbnailInputStream(DLNAThumbnail)}
	 * which handles {@code null} input.
	 *
	 * @param thumbnail the input thumbnail
	 *
	 * @throws NullPointerException if {@code thumbnail} is {@code null}.
	 */
    protected DLNAThumbnailInputStream(DLNAThumbnail thumbnail) {
    	super(thumbnail.getBytes(false));
    	this.imageInfo = thumbnail.getImageInfo();
    	this.profile = thumbnail.getDLNAImageProfile();
    }

	/**
	 * Converts and scales a thumbnail according to the given
	 * {@link DLNAImageProfile}. Preserves aspect ratio. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param outputProfile the DLNA media profile to adhere to for the output.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled and/or converted thumbnail, {@code null} if the
	 *         source is {@code null}.
	 * @exception IOException if the operation fails.
	 */
	public DLNAThumbnailInputStream transcode(
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		DLNAThumbnail thumbnail;
		thumbnail = (DLNAThumbnail) ImagesUtil.transcodeImage(
			this.getBytes(false),
			outputProfile,
			true,
			padToSize);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
	 * @return The bytes of this thumbnail.
	 */
	@SuppressFBWarnings("EI_EXPOSE_REP")
	public byte[] getBytes(boolean copy) {
		if (copy) {
			byte[] result = new byte[buf.length];
			System.arraycopy(buf, 0, result, 0, buf.length);
			return result;
		} else {
			return buf;
		}
	}


	/**
	 * @return A {@link DLNAThumbnail} sharing the the underlying buffer.
	 * @throws DLNAProfileException
	 */
	public DLNAThumbnail getThumbnail() throws DLNAProfileException {
		return new DLNAThumbnail(buf, imageInfo, profile, false);
	}

	/**
	 * @return The {@link ImageInfo} for this thumbnail.
	 */
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	/**
	 * @return The width of this thumbnail.
	 */
	public int getWidth() {
		return imageInfo != null ? imageInfo.getWidth() : -1;
	}


	/**
	 * @return The height of this thumbnail.
	 */
	public int getHeight() {
		return imageInfo != null ? imageInfo.getHeight() : -1;
	}


	/**
	 * @return The {@link ImageFormat} for this thumbnail.
	 */
	public ImageFormat getFormat() {
		return imageInfo != null ? imageInfo.getFormat() : null;
	}

	/**
	 * @return the size of this thumbnail in bytes.
	 */
	public long getSize() {
		return buf != null ? buf.length : 0;
	}

	/**
	 * @return The {@link ColorSpace} for this thumbnail.
	 */
	public ColorSpace getColorSpace() {
		return imageInfo != null ? imageInfo.getColorSpace() : null;
	}

	/**
	 * @return The {@link ColorSpaceType} for this thumbnail.
	 */
	public ColorSpaceType getColorSpaceType() {
		return imageInfo != null ? imageInfo.getColorSpaceType() : null;
	}

	/**
	 * @return The bits per pixel for this thumbnail.
	 *
	 * @see #getBitDepth()
	 */
	public int getBitPerPixel() {
		return imageInfo != null ? imageInfo.getBitsPerPixel() : -1;
	}

	/**
	 * The number of components describe how many "channels" the color model
	 * has. A grayscale image without alpha has 1, a RGB image without alpha
	 * has 3, a RGP image with alpha has 4 etc.
	 *
	 * @return The number of components for this thumbnail.
	 */
	public int getNumComponents() {
		return imageInfo != null ? imageInfo.getNumComponents() : -1;
	}

	/**
	 * @return The number of bits per color "channel" for this thumbnail.
	 *
	 * @see #getBitPerPixel()
	 * @see #getNumColorComponents()
	 */
	public int getBitDepth() {
		return imageInfo != null ? imageInfo.getBitDepth() : -1;
	}

	/**
	 * @return Whether or not {@link ImageIO} can read/parse this thumbnail.
	 */
	public boolean isImageIOSupported() {
		return imageInfo != null ? imageInfo.isImageIOSupported() : false;
	}

	/**
	 * @return The {@link DLNAImageProfile} this thumbnail adheres to.
	 */
	public DLNAImageProfile getDLNAImageProfile() {
		return profile;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(90);
		sb.append("DLNAThumbnailInputStream: Format = ").append(imageInfo.getFormat())
		.append(", Width = ").append(imageInfo.getWidth())
		.append(", Height = ").append(imageInfo.getHeight())
		.append(", Size = ").append(buf != null ? buf.length : 0);
		return sb.toString();
	}

	/**
     * Resets the buffer to the start position and clears any marks.
     */
    public synchronized void fullReset() {
        pos = 0;
        mark = 0;
    }
}
