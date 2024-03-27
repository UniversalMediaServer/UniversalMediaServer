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

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

	private static final int ANCHO_FOCO_H = 4;

	public static ComponentUI createUI(JComponent c) {
		return new CustomTabbedPaneUI();
	}

	@Override
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
		if (isSelected) {
			g.setColor(new Color(0, 0, 0));
		}

		super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	}

	@Override
	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
		//nothing to do
	}

	@Override
	protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
		return 10 + super.calculateTabWidth(tabPlacement, tabIndex, metrics);
	}

	@Override
	protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
		if (tabPlacement == LEFT || tabPlacement == RIGHT) {
			return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
		} else {
			return ANCHO_FOCO_H + super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
		}
	}

}
