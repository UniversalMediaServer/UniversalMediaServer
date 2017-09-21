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

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;


/**
 * This interface represents an implementation that is able to perform an
 * operation (filter) a {@link BufferedImage}. In addition to extending
 * {@link BufferedImageOp}, it requires additional methods for further control
 * and information.
 * <p>
 * {@link NonGeometricBufferedImageOp} can be used as an "implementation helper"
 * for implementations that don't change the geometry/resolution of the image.
 *
 * @author Nadahar
 */
public interface BufferedImageFilter extends BufferedImageOp {

	/**
	 * @return The textual representation of this filter, should be the same as
	 *         returned by {@link #toString()}.
	 */
	public String getDescription();

	/**
	 * Applies this filter to the source {@link BufferedImage}, performing a
	 * color conversion if needed.
	 *
	 * @param source the {@link BufferedImage} to modify.
	 * @return The {@link BufferedImageFilterResult}.
	 */
	public BufferedImageFilterResult filter(BufferedImage source);

	/**
	 * Applies this filter to the source {@link BufferedImage}, performing a
	 * color conversion if needed.
	 *
	 * @param source the {@link BufferedImage} to modify.
	 * @param destination the {@link BufferedImage} in which to store the
	 *            filtered result. If {@code null}, {@code source} will be used
	 *            if possible.
	 * @return The resulting {@link BufferedImage}.
	 */
	@Override
	public BufferedImage filter(BufferedImage source, BufferedImage destination);

	/**
	 * Applies this filter to the source {@link BufferedImage}, performing a
	 * color conversion if needed.
	 *
	 * @param source the {@link BufferedImage} to modify.
	 * @param destination the {@link BufferedImage} in which to store the
	 *            filtered result. If {@code null}, {@code source} will be used
	 *            or a new created, depending on the {@code modifySource}
	 *            argument and if reuse is possible.
	 * @param modifySource if {@code destination} is {@code null}, determines if
	 *            {@code source} will be used as the destination or if a new
	 *            destination will be created.
	 * @return The {@link BufferedImageFilterResult}.
	 */
	public BufferedImageFilterResult filter(BufferedImage source, BufferedImage destination, boolean modifySource);

	/**
	 * An immutable container for {@link BufferedImageFilter} the resulting
	 * {@link BufferedImage} and some information about the process.
	 *
	 * @author Nadahar
	 */
	public class BufferedImageFilterResult {

		private final BufferedImage bufferedImage;
		private final boolean modified;
		private final boolean originalInstance;

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param bufferedImage the {@link BufferedImage}.
		 * @param modified {@code true} if the {@link BufferedImage} was
		 *            modified during the operation, {@code false} otherwise.
		 * @param originalInstance {@code true} if the {@link BufferedImage}
		 *            instance is the same as the input, {@code false} if a new
		 *            was created.
		 */
		public BufferedImageFilterResult(BufferedImage bufferedImage, boolean modified, boolean originalInstance) {
			this.bufferedImage = bufferedImage;
			this.modified = modified;
			this.originalInstance = originalInstance;
		}

		/**
		 * @return The resulting {@link BufferedImage}.
		 */
		public BufferedImage getBufferedImage() {
			return bufferedImage;
		}

		/**
		 * @return {@code true} if the {@link BufferedImage} was modified during
		 *         the operation, {@code false} otherwise.
		 */
		public boolean isModified() {
			return modified;
		}

		/**
		 * @return {@code true} if the {@link BufferedImage} instance is the
		 *         same as the input, {@code false} if a new was created.
		 */
		public boolean isOriginalInstance() {
			return originalInstance;
		}

		@Override
		public String toString() {
			return
				"OriginalInstance: " + (originalInstance ? "true"  : "false") +
				", Modified: " + (modified ? "true" : "false") +
				", " + bufferedImage;
		}
	}
}
