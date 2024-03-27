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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JComponent;

/**
 * A simple flat borderless progress bar painter, required for Windows where
 * setBorderPainted etc are ignored by the default laf.
 */
public class SimpleProgressUI extends javax.swing.plaf.basic.BasicProgressBarUI {

	Color fg;
	Color bg;

	public SimpleProgressUI() {
		this(null, null);
	}

	public SimpleProgressUI(Color fg, Color bg) {
		this.fg = fg != null ? fg : super.getSelectionForeground();
		this.bg = bg != null ? bg : super.getSelectionBackground();
	}

	@Override
	protected void paintDeterminate(Graphics g, JComponent c) {
		Insets b = progressBar.getInsets();
		int w = progressBar.getWidth() - (b.right + b.left);
		int h = progressBar.getHeight() - (b.top + b.bottom);
		if (w < 1 || h < 1) {
			return;
		}
		// Draw a continuous horizontal left-to-right bar
		int filled = getAmountFull(b, w, h);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(progressBar.getForeground());
		g2.setStroke(new BasicStroke(h, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		g2.drawLine(b.left, (h / 2) + b.top, filled + b.left, (h / 2) + b.top);
		// Draw the string, if any
		if (progressBar.isStringPainted()) {
			paintString(g, b.left, b.top, w, h, filled, b);
		}
	}

	@Override
	protected Color getSelectionForeground() {
		return fg;
	}

	@Override
	protected Color getSelectionBackground() {
		return bg;
	}
}
