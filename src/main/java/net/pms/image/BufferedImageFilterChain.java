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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.pms.image.BufferedImageFilter.BufferedImageFilterResult;


/**
 * This class is a special {@link List} implementation that can only hold
 * {@link BufferedImageFilter} elements. It can be used to group several
 * {@link BufferedImageFilter}s together and apply them all by calling one of
 * the {@link #filter} methods. The filters will be applied in the order of the
 * elements.
 *
 * @author Nadahar
 */
public class BufferedImageFilterChain extends ArrayList<BufferedImageFilter> {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new empty filter chain.
	 */
	public BufferedImageFilterChain() {
		super();
	}

	/**
	 * Creates a new filter chain containing the specified
	 * {@link BufferedImageFilter}s in the specified order.
	 *
	 * @param filters the {@link BufferedImageFilter} instances.
	 */
	public BufferedImageFilterChain(BufferedImageFilter... filters) {
		super(Arrays.asList(filters));
	}

	/**
	 * Creates an empty filter chain with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the filter chain.
	 */
	public BufferedImageFilterChain(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new filter chain containing the {@link BufferedImageFilter}s of
	 * the specified collection, in the order they are returned by the
	 * collection's iterator.
	 *
	 * @param collection the {@link Collection} of {@link BufferedImageFilter}s.
	 */
	public BufferedImageFilterChain(Collection<? extends BufferedImageFilter> collection) {
		super(collection);
	}

	@Override
	public String toString() {
		if (isEmpty()) {
			return "Empty " + this.getClass().getSimpleName();
		}
		StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(": [");
		boolean first = true;
		int i = 1;
		for (BufferedImageFilter filter : this) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(i++).append(": ").append(filter);
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Applies the {@link BufferedImageFilter}s contained by this
	 * {@link BufferedImageFilterChain} to the source {@link BufferedImage},
	 * performing a color conversion if needed.
	 *
	 * @param source the {@link BufferedImage} to modify.
	 * @return The {@link BufferedImageFilterResult}.
	 */
	public BufferedImageFilterResult filter(BufferedImage source) {
		return filter(source, null, true);
	}

	/**
	 * Applies the {@link BufferedImageFilter}s contained by this
	 * {@link BufferedImageFilterChain} to the source {@link BufferedImage},
	 * performing a color conversion if needed.
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
	public BufferedImageFilterResult filter(BufferedImage source, BufferedImage destination, boolean modifySource) {
		if (isEmpty()) {
			return new BufferedImageFilterResult(destination == null ? source : destination, false, destination == null);
		}
		boolean modified = false;
		boolean originalInstance = true;
		BufferedImage outputImage = source;
		for (BufferedImageFilter filter : this) {
			if (filter != null) {
				BufferedImageFilterResult result = filter.filter(outputImage, destination, modifySource);
				modified |= result.isModified();
				originalInstance &= result.isOriginalInstance();
				outputImage = result.getBufferedImage();
			}
		}
		return new BufferedImageFilterResult(outputImage, modified, originalInstance);
	}
}
