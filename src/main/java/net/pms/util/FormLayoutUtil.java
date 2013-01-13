/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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

import java.awt.ComponentOrientation;
import java.awt.Insets;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpec.DefaultAlignment;

/**
 * Provides some convenience behavior for flipping side in column
 * specifications, arrays of column specifications and encoded column specs.
 * This can be used to flip the FormLayout from the default left-to-right
 * definition to right-to-left.
 * 
 * Based on the jgoodies ComponenOrientationExample tutorial code.
 */
public class FormLayoutUtil {

	/**
	 * Flips the default alignment of the given column specification and returns
	 * a new column specification object with the flipped alignment and the same
	 * size and growing behavior as the original.
	 * 
	 * @param spec
	 *            the original column specification
	 * @return the column specification with flipped default alignment
	 */
	private static ColumnSpec flipped(ColumnSpec spec) {
		DefaultAlignment alignment = spec.getDefaultAlignment();

		if (alignment == ColumnSpec.LEFT) {
			alignment = ColumnSpec.RIGHT;
		} else {
			if (alignment == ColumnSpec.RIGHT) {
				alignment = ColumnSpec.LEFT;
			}
		}

		return new ColumnSpec(alignment, spec.getSize(), spec.getResizeWeight());
	}

	/**
	 * Returns an array of column specifications that is built from the given
	 * array by flipping each column spec and reversing their order.
	 * 
	 * @param original
	 *            the original array of column specifications
	 * @return an array of flipped column specs in reversed order
	 */
	private static ColumnSpec[] flipped(ColumnSpec[] original) {
		int length = original.length;
		ColumnSpec[] flipped = new ColumnSpec[length];

		for (int i = 0; i < length; i++) {
			flipped[i] = flipped(original[length - 1 - i]);
		}
		return flipped;
	}

	/**
	 * Returns an array of column specifications that is built from the given
	 * encoded column specifications by flipping each column spec and reversing
	 * their order.
	 * 
	 * @param encodedColumnSpecs
	 *            the original comma-separated encoded column specifications
	 * @return an array of flipped column specs in reversed order
	 */
	private static ColumnSpec[] flipped(String encodedColumnSpecs) {
		return flipped(ColumnSpec.decodeSpecs(encodedColumnSpecs));
	}

	/**
	 * Returns an array of column specifications that is built from the given
	 * encoded column specifications by flipping each column spec and reversing
	 * their order.
	 * 
	 * @param encodedColumnSpecs
	 *            the original comma-separated encoded column specifications
	 * @return an array of flipped column specs in reversed order
	 */
	public static String flip(String encodedColumnSpecs) {
		StringBuffer result = new StringBuffer();
		String separator = "";
		ColumnSpec[] flippedSpecs = flipped(ColumnSpec.decodeSpecs(encodedColumnSpecs));
		
		for (ColumnSpec spec : flippedSpecs) {
			result.append(separator);
			result.append(spec.encode());
			separator = ", ";
		}

		return result.toString();
	}

	/**
	 * Creates and returns a horizontally flipped clone of the given cell
	 * constraints object. Flips the horizontal alignment and the left and right
	 * insets.
	 * 
	 * @param cc
	 *            the original cell constraints object
	 * @return the flipped cell constraints with flipped horizontal alignment,
	 *         and flipped left and right insets - if any
	 */
	private static CellConstraints flipHorizontally(CellConstraints cc) {
		CellConstraints.Alignment flippedHAlign = cc.hAlign;

		if (flippedHAlign == CellConstraints.LEFT) {
			flippedHAlign = CellConstraints.RIGHT;
		} else {
			if (flippedHAlign == CellConstraints.RIGHT) {
				flippedHAlign = CellConstraints.LEFT;
			}
		}

		CellConstraints flipped = new CellConstraints(cc.gridX, cc.gridY, cc.gridWidth, cc.gridHeight, flippedHAlign, cc.vAlign);

		if (cc.insets != null) {
			flipped.insets = new Insets(cc.insets.top, cc.insets.right, cc.insets.bottom, cc.insets.left);
		}

		return flipped;
	}

	/**
	 * Creates and returns a horizontally flipped clone of the given cell
	 * constraints object with the grid position adjusted to the given column
	 * count. Flips the horizontal alignment and the left and right insets. And
	 * swaps the left and right cell positions according to the specified column
	 * count.
	 * 
	 * @param cc
	 *            the original cell constraints object
	 * @param columnCount
	 *            the number of columns; used to swap the left and right cell
	 *            bounds
	 * @return the flipped cell constraints with flipped horizontal alignment,
	 *         and flipped left and right insets - if any
	 */
	private static CellConstraints flipHorizontally(CellConstraints cc, int columnCount) {
		CellConstraints flipped = flipHorizontally(cc);
		flipped.gridX = columnCount + 1 - cc.gridX - cc.gridWidth + 1;

		return flipped;
	}

	/**
	 * Returns the proper column specification for the given component
	 * orientation. The column specification is provided in left-to-right
	 * format. When the orientation is left-to-right, the string is returned as
	 * is. Otherwise the string is reworked to represent right-to-left.
	 * 
	 * @param leftToRightColSpec
	 *            The column specification for a left-to-right orientation.
	 * @param orientation
	 * 			  The orientation of the locale.
	 * @return The proper column specification.
	 */
	public static String getColSpec(String leftToRightColSpec, ComponentOrientation orientation) {
		if (orientation.isLeftToRight()) {
			return leftToRightColSpec;
		} else {
			return FormLayoutUtil.flip(leftToRightColSpec);
		}
	}

	/**
	 * Returns the proper cell constraints for the given column specification
	 * and orientation. This means that when the orientation is left-to-right,
	 * the cell constraints are returned as is. When it is right-to-left, the
	 * cell constraints are flipped horizontally.
	 * 
	 * @param cc
	 *            The cell constraints defined in left-to-right format.
	 * @param colSpec
	 *            The column specification for which the cell constraints are
	 *            designed.
	 * @param orientation
	 *            The orientation of the locale.
	 * @return The proper cell constraints.
	 */
	public static CellConstraints flip(CellConstraints cc, String colSpec, ComponentOrientation orientation) {
		if (orientation.isLeftToRight()) {
			return cc;
		} else {
			int columnCount = colSpec.split(",").length;
			return flipHorizontally(cc, columnCount);
		}
	}
}
