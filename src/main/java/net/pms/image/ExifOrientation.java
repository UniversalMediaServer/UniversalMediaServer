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

import net.coobird.thumbnailator.util.exif.Orientation;

/**
 * Exif orientation tag definition, where {@link #TOP_LEFT} is the default/normal state:
 *
 * <ol>
 *   <li>{@link #TOP_LEFT} - The 0th row is at the visual top of the image, and the 0th column is the visual left-hand side.</li>
 *   <li>{@link #TOP_RIGHT} - The 0th row is at the visual top of the image, and the 0th column is the visual right-hand side.</li>
 *   <li>{@link #BOTTOM_RIGHT} - The 0th row is at the visual bottom of the image, and the 0th column is the visual right-hand side.</li>
 *   <li>{@link #BOTTOM_LEFT} - The 0th row is at the visual bottom of the image, and the 0th column is the visual left-hand side.</li>
 *   <li>{@link #LEFT_TOP} - The 0th row is the visual left-hand side of the image, and the 0th column is the visual top.</li>
 *   <li>{@link #RIGHT_TOP} - The 0th row is the visual right-hand side of the image, and the 0th column is the visual top.</li>
 *   <li>{@link #RIGHT_BOTTOM} - The 0th row is the visual right-hand side of the image, and the 0th column is the visual bottom.</li>
 *   <li>{@link #LEFT_BOTTOM} - The 0th row is the visual left-hand side of the image, and the 0th column is the visual bottom.</li>
 * </ol>
 * Exif orientation tag visualized (by Adam M. Costello):<pre>
 *
 *  Here is what the letter F would look like if it were tagged correctly and displayed
 *  by a program that ignores the orientation tag (thus showing the stored image):
 *
 *        1        2       3      4         5            6           7          8
 *
 *      888888  888888      88  88      8888888888  88                  88  8888888888
 *      88          88      88  88      88  88      88  88          88  88      88  88
 *      8888      8888    8888  8888    88          8888888888  8888888888          88
 *      88          88      88  88
 *      88          88  888888  888888
 *
 *  DCRaw flip tag values:
 *
 *        0        1       3      2         4            6           7          5
 *
 * </pre>
 * @author Nadahar
 */
public enum ExifOrientation {
	/**
	 * <b>The default/normal value where no transformation is needed</b>.
	 * <p>
	 * <b> 1 </b> - The 0th row is at the visual top of the image, and the 0th
	 * column is the visual left-hand side. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	TOP_LEFT(1),

	/**
	 * <b> 2 </b> - The 0th row is at the visual top of the image, and the 0th
	 * column is the visual right-hand side. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	TOP_RIGHT(2),

	/**
	 * <b> 3 </b> - The 0th row is at the visual bottom of the image, and the
	 * 0th column is the visual right-hand side. See {@link ExifOrientation} for
	 * a visualized representation.
	 */
	BOTTOM_RIGHT(3),

	/**
	 * <b> 4 </b> - The 0th row is at the visual bottom of the image, and the
	 * 0th column is the visual left-hand side. See {@link ExifOrientation} for
	 * a visualized representation.
	 */
	BOTTOM_LEFT(4),

	/**
	 * <b> 5 </b> - The 0th row is the visual left-hand side of the image, and
	 * the 0th column is the visual top. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	LEFT_TOP(5),

	/**
	 * <b> 6 </b> - The 0th row is the visual right-hand side of the image, and
	 * the 0th column is the visual top. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	RIGHT_TOP(6),

	/**
	 * <b> 7 </b> - The 0th row is the visual right-hand side of the image, and
	 * the 0th column is the visual bottom. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	RIGHT_BOTTOM(7),

	/**
	 * <b> 8 </b> - The 0th row is the visual left-hand side of the image, and
	 * the 0th column is the visual bottom. See {@link ExifOrientation} for a
	 * visualized representation.
	 */
	LEFT_BOTTOM(8),
	;

	private int value;
	private ExifOrientation(int value)
	{
		this.value = value;
	}

	/**
	 * @param value The Exif orientation integer.
	 * @return The {@link ExifOrientation} corresponding to the Exif orientation
	 *         value or {@link #TOP_LEFT} if invalid.
	 */
	public static ExifOrientation typeOf(int value)
	{
		for (ExifOrientation orientation : ExifOrientation.values())
		{
			if (orientation.value == value)
			{
				return orientation;
			}
		}
		return TOP_LEFT;
	}

	/**
	 * @param value The Exif orientation {@link Integer}.
	 * @return The {@link ExifOrientation} corresponding to the Exif orientation
	 *         value or {@link #TOP_LEFT} if invalid or {@code null}.
	 */
	public static ExifOrientation typeOf(Integer value)
	{
		if (value == null) {
			return TOP_LEFT;
		}

		for (ExifOrientation orientation : ExifOrientation.values())
		{
			if (orientation.value == value.intValue())
			{
				return orientation;
			}
		}
		return TOP_LEFT;
	}

	/**
	 *
	 * @param dcRawOrientation the DCRaw "flip" value
	 * @return The {@link ExifOrientation} corresponding to the DCRaw flip
	 *         value.
	 */
	public static ExifOrientation fromDCRawOrientation(int dcRawOrientation) {
		switch (dcRawOrientation & 7) {
			case 0:
				return TOP_LEFT;
			case 1:
				return TOP_RIGHT;
			case 2:
				return BOTTOM_LEFT;
			case 3:
				return BOTTOM_RIGHT;
			case 4:
				return LEFT_TOP;
			case 5:
				return LEFT_BOTTOM;
			case 6:
				return RIGHT_TOP;
			case 7:
				return RIGHT_BOTTOM;
			default:
				return TOP_LEFT;
		}
	}

	/**
	 * @return The Exif orientation value for this {@link ExifOrientation}.
	 */
	public int getValue() {
		return value;
	}

	@Override
	public String toString()
	{
		return "Orientation " + this.name() + " ("+ value + ")";
	}

	/**
	 * @return The DCRaw "flip" value for the {@code -t} argument for this
	 *         {@link ExifOrientation}.
	 */
	public int getDCRawOrientation() {
		switch (this) {
			case TOP_LEFT:
				return 0;
			case TOP_RIGHT:
				return 1;
			case BOTTOM_RIGHT:
				return 3;
			case BOTTOM_LEFT:
				return 2;
			case LEFT_TOP:
				return 4;
			case RIGHT_TOP:
				return 6;
			case RIGHT_BOTTOM:
				return 7;
			case LEFT_BOTTOM:
				return 5;
			default:
				return 0;
		}
	}

	/**
	 * Converts this {@link ExifOrientation} to a corresponding
	 * {@link Orientation}.
	 *
	 * @return The corresponding {@link Orientation}.
	 */
	public Orientation getThumbnailatorOrientation() {
		return Orientation.typeOf(getValue());
	}
}

