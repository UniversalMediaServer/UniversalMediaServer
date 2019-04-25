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

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;


/**
 * This abstract class contains boilerplate methods for {@link BufferedImageOp}
 * implementations that doesn't alter the {@link BufferedImage}'s
 * resolution/geometry.
 *
 * @author Nadahar
 */
public abstract class NonGeometricBufferedImageOp implements BufferedImageOp {

	/** The {@link RenderingHints} to use during the operation. */
	protected final RenderingHints hints;

	/**
	 * Abstract constructor.
	 *
	 * @param hints the {@link RenderingHints} to use during the operation.
	 */
	public NonGeometricBufferedImageOp(RenderingHints hints) {
		this.hints = hints;
	}

	/**
	 * Returns the bounding box of the rescaled destination
	 * {@link java.awt.Image}. Since this is not a geometric operation, the
	 * bounding box does not change.
	 *
	 * @param src the source {@link BufferedImage}.
	 * @return The bounds of the resulting {@link java.awt.Image}.
	 */
	@Override
	public Rectangle2D getBounds2D(BufferedImage src) {
		return getBounds2D(src.getRaster());
	}

	/**
	 * Returns the bounding box of the rescaled destination Raster. Since
	 * this is not a geometric operation, the bounding box does not change.
	 *
	 * @param src the rescaled destination {@link Raster}.
	 * @return The bounds of the specified {@link Raster}.
	 */
	public Rectangle2D getBounds2D(Raster src) {
		return src.getBounds();
	}

	/**
	 * Creates a zeroed destination image with the correct size and number
	 * of bands.
	 *
	 * @param src the source {@link BufferedImage} for the filter operation.
	 * @param destColorModel the {@link ColorModel} of the destination. If
	 *            {@code null}, the {@link ColorModel} of {@code src} will
	 *            be used.
	 * @return The zeroed-destination {@link BufferedImage}.
	 */
	@Override
	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destColorModel) {
		BufferedImage result;
		if (destColorModel == null) {
			ColorModel colorModel = src.getColorModel();
			result = new BufferedImage(
				colorModel,
				src.getRaster().createCompatibleWritableRaster(),
				colorModel.isAlphaPremultiplied(),
				null
			);
		} else {
			int width = src.getWidth();
			int height = src.getHeight();
			result = new BufferedImage(
				destColorModel,
				destColorModel.createCompatibleWritableRaster(width, height),
				destColorModel.isAlphaPremultiplied(),
				null
			);
		}
		return result;
	}

	/**
	 * Returns the location of the destination point given a point in the
	 * source. If {@code dstPt} is non-{@code null}, it will be used to hold
	 * the return value. Since this is not a geometric operation,
	 * {@code srcPt} will equal the {@code dstPt}.
	 *
	 * @param srcPt a {@link Point2D} in the source image.
	 * @param dstPt the destination {@link Point2D} or {@code null}.
	 * @return The location of the destination point.
	 */
	@Override
	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null) {
			dstPt = new Point2D.Float();
		}
		dstPt.setLocation(srcPt.getX(), srcPt.getY());
		return dstPt;
	}

	/**
	 * Returns the rendering hints for this filter operation.
	 *
	 * @return the rendering hints of this filter operation.
	 */
	@Override
	public RenderingHints getRenderingHints() {
		return hints;
	}
}
