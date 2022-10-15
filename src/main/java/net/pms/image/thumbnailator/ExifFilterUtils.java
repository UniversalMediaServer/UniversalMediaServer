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
package net.pms.image.thumbnailator;

import net.coobird.thumbnailator.filters.ImageFilter;
import net.coobird.thumbnailator.filters.Pipeline;
import net.coobird.thumbnailator.util.exif.Orientation;

/**
 * This class is a copy of
 * {@link net.coobird.thumbnailator.util.exif.ExifFilterUtils} that use bugfixed
 * versions of {@link net.coobird.thumbnailator.filters.Rotation} and
 * {@link net.coobird.thumbnailator.filters.Rotation}.
 *
 * When the original classes <a
 * href="https://github.com/coobird/thumbnailator/pull/92">are fixed</a>, this
 * class can be removed.
 */
public final class ExifFilterUtils {

	/**
	 * This class should not be instantiated.
	 */
	private ExifFilterUtils() {}

	/**
	 * Returns a {@link ImageFilter} which will perform the transformations
	 * required to properly orient the thumbnail according to the Exif
	 * orientation.
	 *
	 * @param orientation	The Exif orientation
	 * @return				{@link ImageFilter}s required to properly
	 * 						orient the image.
	 */
	public static ImageFilter getFilterForOrientation(Orientation orientation) {
		Pipeline filters = new Pipeline();
		if (null != orientation) {
			switch (orientation) {
				case TOP_RIGHT -> filters.add(Flip.HORIZONTAL);
				case BOTTOM_RIGHT -> filters.add(Rotation.ROTATE_180_DEGREES);
				case BOTTOM_LEFT -> {
					filters.add(Rotation.ROTATE_180_DEGREES);
					filters.add(Flip.HORIZONTAL);
				}
				case LEFT_TOP -> {
					filters.add(Rotation.RIGHT_90_DEGREES);
					filters.add(Flip.HORIZONTAL);
				}
				case RIGHT_TOP -> filters.add(Rotation.RIGHT_90_DEGREES);
				case RIGHT_BOTTOM -> {
					filters.add(Rotation.LEFT_90_DEGREES);
					filters.add(Flip.HORIZONTAL);
				}
				case LEFT_BOTTOM -> filters.add(Rotation.LEFT_90_DEGREES);
				default -> {
					//nothing to do
				}
			}
		}

		return filters;
	}
}
