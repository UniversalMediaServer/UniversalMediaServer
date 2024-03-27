/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.swing.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 *
 * Source: http://www.camick.com/java/source/WrapLayout.java Blog:
 * tips4java.wordpress.com/2008/11/06/wrap-layout/
 */
public class WrapLayout extends FlowLayout {

	private static final long serialVersionUID = 8423910716184289166L;

	/**
	 * Constructs a new <code>WrapLayout</code> with a centered alignment and a
	 * default 5-unit horizontal and vertical gap.
	 */
	public WrapLayout() {
		super();
	}

	/**
	 * Constructs a new <code>WrapLayout</code> with the specified alignment and
	 * a default 5-unit horizontal and vertical gap.
	 * <p>
	 * The value of the alignment argument must be one of
	 * <code>FlowLayout.LEFT</code>, <code>FlowLayout.CENTER</code>,
	 * <code>FlowLayout.RIGHT</code>, <code>FlowLayout.LEADING</code>, or
	 * <code>FlowLayout.TRAILING</code>.
	 *
	 * @param align the alignment value
	 */
	public WrapLayout(int align) {
		super(align);
	}

	/**
	 * Creates a new wrap layout manager with the indicated alignment and the
	 * indicated horizontal and vertical gaps.
	 *
	 * @param align the alignment value
	 * @param hgap the horizontal gap between components
	 * @param vgap the vertical gap between components
	 */
	public WrapLayout(int align, int hgap, int vgap) {
		super(align, hgap, vgap);
	}

	/**
	 * Returns the preferred dimensions for this layout given the
	 * <i>visible</i> components in the specified target container.
	 *
	 * @param target the component which needs to be laid out
	 * @return the preferred dimensions to lay out the subcomponents of the
	 * specified container
	 */
	@Override
	public Dimension preferredLayoutSize(Container target) {
		return layoutSize(target, true);
	}

	/**
	 * Returns the minimum dimensions needed to layout the <i>visible</i>
	 * components contained in the specified target container.
	 *
	 * @param target the component which needs to be laid out
	 * @return the minimum dimensions to lay out the subcomponents of the
	 * specified container
	 */
	@Override
	public Dimension minimumLayoutSize(Container target) {
		Dimension minimum = layoutSize(target, false);
		minimum.width -= (getHgap() + 1);
		return minimum;
	}

	/**
	 * Returns the minimum or preferred dimension needed to layout the target
	 * container.
	 *
	 * @param target target to get layout size for
	 * @param preferred should preferred size be calculated
	 * @return the dimension to layout the target container
	 */
	private Dimension layoutSize(Container target, boolean preferred) {
		synchronized (target.getTreeLock()) {
			//  Each row must fit with the width allocated to the container.
			//  When the container width = 0, the preferred width of the container
			//  has not yet been calculated so lets ask for the maximum.

			int targetWidth = target.getSize().width;

			if (targetWidth == 0) {
				targetWidth = Integer.MAX_VALUE;
			}

			int hgap = getHgap();
			int vgap = getVgap();
			Insets insets = target.getInsets();
			int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
			int maxWidth = targetWidth - horizontalInsetsAndGap;

			//  Fit components into the allowed width
			Dimension dim = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;

			int nmembers = target.getComponentCount();

			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);

				if (m.isVisible()) {
					Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

					//  Can't add the component to current row. Start a new row.
					if (rowWidth + d.width > maxWidth) {
						addRow(dim, rowWidth, rowHeight);
						rowWidth = 0;
						rowHeight = 0;
					}

					//  Add a horizontal gap for all components after the first
					if (rowWidth != 0) {
						rowWidth += hgap;
					}

					rowWidth += d.width;
					rowHeight = Math.max(rowHeight, d.height);
				}
			}

			addRow(dim, rowWidth, rowHeight);

			dim.width += horizontalInsetsAndGap;
			dim.height += insets.top + insets.bottom + vgap * 2;

			//	When using a scroll pane or the DecoratedLookAndFeel we need to
			//  make sure the preferred size is less than the size of the
			//  target container so shrinking the container size works
			//  correctly. Removing the horizontal gap is an easy way to do this.
			Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

			if (scrollPane != null && target.isValid()) {
				dim.width -= (hgap + 1);
			}

			return dim;
		}
	}

	/*
		 *  A new row has been completed. Use the dimensions of this row
		 *  to update the preferred size for the container.
		 *
		 *  @param dim update the width and height when appropriate
		 *  @param rowWidth the width of the row to add
		 *  @param rowHeight the height of the row to add
	 */
	private void addRow(Dimension dim, int rowWidth, int rowHeight) {
		dim.width = Math.max(dim.width, rowWidth);

		if (dim.height > 0) {
			dim.height += getVgap();
		}

		dim.height += rowHeight;
	}
}
