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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import net.coobird.thumbnailator.builders.BufferedImageBuilder;
import net.coobird.thumbnailator.filters.ImageFilter;

/**
 * This class is a bugfix of {@link net.coobird.thumbnailator.filters.Flip}.
 *
 * When the original class <a
 * href="https://github.com/coobird/thumbnailator/pull/92">is fixed</a>, this
 * class can be removed.
 */
public class Flip {
	/**
	 * This class is not meant to be instantiated.
	 */
	private Flip() {
	}

	/**
	 * An image filter which performs a horizontal flip of the image.
	 */
	public static final ImageFilter HORIZONTAL = (BufferedImage img) -> {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage newImage =
				new BufferedImageBuilder(width, height, img.getType()).build();

		Graphics g = newImage.getGraphics();
		g.drawImage(img, width, 0, 0, height, 0, 0, width, height, null);
		g.dispose();

		return newImage;
	};

	/**
	 * An image filter which performs a vertical flip of the image.
	 */
	public static final ImageFilter VERTICAL = (BufferedImage img) -> {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage newImage =
				new BufferedImageBuilder(width, height, img.getType()).build();

		Graphics g = newImage.getGraphics();
		g.drawImage(img, 0, height, width, 0, 0, 0, width, height, null);
		g.dispose();

		return newImage;
	};
}
