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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.dlna.DLNAThumbnail;
import net.pms.util.ImagesUtil.ImageFormat;
import net.pms.util.ImagesUtil.ScaleType;

/**
 * This is an {@link InputStream} implementation of {@link DLNAThumbnail}. It
 * holds the stream's content in an internal buffer and is as such not intended
 * to hold very large streams.
 */

public class DLNAThumbnailInputStream extends ByteArrayInputStream {

private final int width;
private final int height;
private final ImageFormat format;

	/**
     * Creates a {@link DLNAThumbnailInputStream} where it uses
     * {@code imageByteArray} as its buffer array. The buffer array is not
     * copied. The initial value of {@code pos} is {@code 0} and the
     * initial value of {@code count} is the length of
     * {@code imageByteArray}.  Format support is limited to that of
     * {@link ImageIO}.
     *
     * @param imageByteArray the source image in a supported format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null}
	 *         if the source image could not be parsed.
     */
	public static DLNAThumbnailInputStream toThumbnailInputStream(byte[] imageByteArray) {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(imageByteArray, 0, 0, null, ImageFormat.SOURCE);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
     * Creates a {@link DLNAThumbnailInputStream} where it uses
     * {@code imageByteArray} as its buffer array. The buffer is only
     * copied if any conversion is needed. The initial value of {@code pos}
     * is {@code 0} and the initial value of {@code count} is the length of
     * {@code imageByteArray}. Format support is limited to that of
     * {@link ImageIO}.
     *
	 * @param imageByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *                     {@link ImageFormat#SOURCE} to preserve source format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null}
	 *         if the source image could not be parsed.
     */
	public static DLNAThumbnailInputStream toThumbnailInputStream(
		byte[] imageByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat
	) {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(imageByteArray, width, height, scaleType, outputFormat);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
     * Creates a {@link DLNAThumbnailInputStream} from {@code inputStream}.
     * <p>
     * <b>{@code inputStream} is consumed and closed</b>
     * <p>
     * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass
     * the underlying array is used - otherwise the stream is read into
     * memory. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null}
	 *         if the source image could not be parsed.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(InputStream inputStream) {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(inputStream, 0, 0, null, ImageFormat.SOURCE);
		return thumbnail != null ? new DLNAThumbnailInputStream(thumbnail) : null;
	}

	/**
     * Creates a {@link DLNAThumbnailInputStream} from {@code inputStream}.
     * <p>
     * <b>{@code inputStream} is consumed and closed</b>
     * <p>
     * If {@code inputStream} is {@link ByteArrayInputStream} or a subclass
     * the underlying array is used - otherwise the stream is read into
     * memory. Format support is limited to that of {@link ImageIO}.
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *                     {@link ImageFormat#SOURCE} to preserve source format.
	 * @return The populated {@link DLNAThumbnailInputStream} or {@code null}
	 *         if the source image could not be parsed.
	 */
	public static DLNAThumbnailInputStream toThumbnailInputStream(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat
	) {
		DLNAThumbnail thumbnail = DLNAThumbnail.toThumbnail(inputStream, width, height, scaleType, outputFormat);
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
    	width = thumbnail.getWidth();
    	height = thumbnail.getHeight();
    	format = thumbnail.getFormat();
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
	 */
	public DLNAThumbnail getThumbnail() {
		return new DLNAThumbnail(buf, width, height, format, false);
	}

	/**
	 * @return The width of this thumbnail.
	 */
	public int getWidth() {
		return width;
	}


	/**
	 * @return The height of this thumbnail.
	 */
	public int getHeight() {
		return height;
	}


	/**
	 * @return The {@link ImageFormat} for this thumbnail.
	 */
	public ImageFormat getFormat() {
		return format;
	}

	/**
	 * @return the size of this thumbnail in bytes.
	 */
	public int getSize() {
		return buf != null ? buf.length : 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(90);
		sb.append("DLNAThumbnailInputStream: Format = ").append(format)
		.append(", Width = ").append(width)
		.append(", Height = ").append(height)
		.append(", Size = ").append(buf.length);
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
