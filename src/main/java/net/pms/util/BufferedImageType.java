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
package net.pms.util;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an {@code enum} mirroring the integer constants in
 * {@link BufferedImage}. Use this for better type-safety.
 */
public enum BufferedImageType {

	/**
	 * Image type is not recognized so it must be a customized image. This type
	 * is only used as a return value for the getType() method.
	 */
	TYPE_CUSTOM(0),

	/**
	 * Represents an image with 8-bit RGB color components packed into integer
	 * pixels. The image has a {@link DirectColorModel} without alpha. When data
	 * with non-opaque alpha is stored in an image of this type, the color data
	 * must be adjusted to a non-premultiplied form and the alpha discarded, as
	 * described in the {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_INT_RGB(1),

	/**
	 * Represents an image with 8-bit RGBA color components packed into integer
	 * pixels. The image has a <code>DirectColorModel</code> with alpha. The
	 * color data in this image is considered not to be premultiplied with
	 * alpha. When this type is used as the <code>imageType</code> argument to a
	 * <code>BufferedImage</code> constructor, the created image is consistent
	 * with images created in the JDK1.1 and earlier releases.
	 */
	TYPE_INT_ARGB(2),

	/**
	 * Represents an image with 8-bit RGBA color components packed into integer
	 * pixels. The image has a <code>DirectColorModel</code> with alpha. The
	 * color data in this image is considered to be premultiplied with alpha.
	 */
	TYPE_INT_ARGB_PRE(3),

	/**
	 * Represents an image with 8-bit RGB color components, corresponding to a
	 * Windows- or Solaris- style BGR color model, with the colors Blue, Green,
	 * and Red packed into integer pixels. There is no alpha. The image has a
	 * {@link DirectColorModel}. When data with non-opaque alpha is stored in an
	 * image of this type, the color data must be adjusted to a
	 * non-premultiplied form and the alpha discarded, as described in the
	 * {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_INT_BGR(4),

	/**
	 * Represents an image with 8-bit RGB color components, corresponding to a
	 * Windows-style BGR color model) with the colors Blue, Green, and Red
	 * stored in 3 bytes. There is no alpha. The image has a
	 * <code>ComponentColorModel</code>. When data with non-opaque alpha is
	 * stored in an image of this type, the color data must be adjusted to a
	 * non-premultiplied form and the alpha discarded, as described in the
	 * {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_3BYTE_BGR(5),

	/**
	 * Represents an image with 8-bit RGBA color components with the colors
	 * Blue, Green, and Red stored in 3 bytes and 1 byte of alpha. The image has
	 * a <code>ComponentColorModel</code> with alpha. The color data in this
	 * image is considered not to be premultiplied with alpha. The byte data is
	 * interleaved in a single byte array in the order A, B, G, R from lower to
	 * higher byte addresses within each pixel.
	 */
	TYPE_4BYTE_ABGR(6),

	/**
	 * Represents an image with 8-bit RGBA color components with the colors
	 * Blue, Green, and Red stored in 3 bytes and 1 byte of alpha. The image has
	 * a <code>ComponentColorModel</code> with alpha. The color data in this
	 * image is considered to be premultiplied with alpha. The byte data is
	 * interleaved in a single byte array in the order A, B, G, R from lower to
	 * higher byte addresses within each pixel.
	 */
	TYPE_4BYTE_ABGR_PRE(7),

	/**
	 * Represents an image with 5-6-5 RGB color components (5-bits red, 6-bits
	 * green, 5-bits blue) with no alpha. This image has a
	 * <code>DirectColorModel</code>. When data with non-opaque alpha is stored
	 * in an image of this type, the color data must be adjusted to a
	 * non-premultiplied form and the alpha discarded, as described in the
	 * {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_USHORT_565_RGB(8),

	/**
	 * Represents an image with 5-5-5 RGB color components (5-bits red, 5-bits
	 * green, 5-bits blue) with no alpha. This image has a
	 * <code>DirectColorModel</code>. When data with non-opaque alpha is stored
	 * in an image of this type, the color data must be adjusted to a
	 * non-premultiplied form and the alpha discarded, as described in the
	 * {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_USHORT_555_RGB(9),

	/**
	 * Represents a unsigned byte grayscale image, non-indexed. This image has a
	 * <code>ComponentColorModel</code> with a CS_GRAY {@link ColorSpace}. When
	 * data with non-opaque alpha is stored in an image of this type, the color
	 * data must be adjusted to a non-premultiplied form and the alpha
	 * discarded, as described in the {@link java.awt.AlphaComposite}
	 * documentation.
	 */
	TYPE_BYTE_GRAY(10),

	/**
	 * Represents an unsigned short grayscale image, non-indexed). This image
	 * has a <code>ComponentColorModel</code> with a CS_GRAY
	 * <code>ColorSpace</code>. When data with non-opaque alpha is stored in an
	 * image of this type, the color data must be adjusted to a
	 * non-premultiplied form and the alpha discarded, as described in the
	 * {@link java.awt.AlphaComposite} documentation.
	 */
	TYPE_USHORT_GRAY(11),

	/**
	 * Represents an opaque byte-packed 1, 2, or 4 bit image. The image has an
	 * {@link IndexColorModel} without alpha. When this type is used as the
	 * <code>imageType</code> argument to the <code>BufferedImage</code>
	 * constructor that takes an <code>imageType</code> argument but no
	 * <code>ColorModel</code> argument, a 1-bit image is created with an
	 * <code>IndexColorModel</code> with two colors in the default sRGB
	 * <code>ColorSpace</code>: {0,&nbsp;0,&nbsp;0} and
	 * {255,&nbsp;255,&nbsp;255}.
	 *
	 * <p>
	 * Images with 2 or 4 bits per pixel may be constructed via the
	 * <code>BufferedImage</code> constructor that takes a
	 * <code>ColorModel</code> argument by supplying a <code>ColorModel</code>
	 * with an appropriate map size.
	 *
	 * <p>
	 * Images with 8 bits per pixel should use the image types
	 * <code>TYPE_BYTE_INDEXED</code> or <code>TYPE_BYTE_GRAY</code> depending
	 * on their <code>ColorModel</code>.
	 *
	 * <p>
	 * When color data is stored in an image of this type, the closest color in
	 * the colormap is determined by the <code>IndexColorModel</code> and the
	 * resulting index is stored. Approximation and loss of alpha or color
	 * components can result, depending on the colors in the
	 * <code>IndexColorModel</code> colormap.
	 */
	TYPE_BYTE_BINARY(12),

	/**
	 * Represents an indexed byte image. When this type is used as the
	 * <code>imageType</code> argument to the <code>BufferedImage</code>
	 * constructor that takes an <code>imageType</code> argument but no
	 * <code>ColorModel</code> argument, an <code>IndexColorModel</code> is
	 * created with a 256-color 6/6/6 color cube palette with the rest of the
	 * colors from 216-255 populated by grayscale values in the default sRGB
	 * ColorSpace.
	 *
	 * <p>
	 * When color data is stored in an image of this type, the closest color in
	 * the colormap is determined by the <code>IndexColorModel</code> and the
	 * resulting index is stored. Approximation and loss of alpha or color
	 * components can result, depending on the colors in the
	 * <code>IndexColorModel</code> colormap.
	 */
    TYPE_BYTE_INDEXED(13);

	private static final Map<Integer, BufferedImageType> map = new HashMap<>();

    static {
        for (BufferedImageType bufferedImageType : BufferedImageType.values()) {
            map.put(bufferedImageType.typeId, bufferedImageType);
        }
    }

    public static BufferedImageType toBufferedImageType(int typeId) {
        return map.get(typeId);
    }

	private final int typeId;

    private BufferedImageType(int typeId) {
    	this.typeId = typeId;
	}

    public int getTypeId() {
    	return typeId;
    }
}
