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

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.Timer;

// A label that automatically scrolls its text if
// wider than a specified maximum.
public class MarqueeLabel extends JLabel {

	private static final long serialVersionUID = 8600355251271220610L;
	private int speed, spacer, dir, maxWidth, interval = 33;
	private Timer timer = null;

	public MarqueeLabel(String text) {
		this(text, 9999, 30, -1, 10);
	}

	public MarqueeLabel(String text, int width) {
		this(text, width, 30, -1, 10);
	}

	public MarqueeLabel(String text, int width, int speed, int dir, int spacer) {
		super(text);
		this.maxWidth = width;
		this.speed = speed;
		this.dir = dir;
		this.spacer = spacer;
		setSize(getPreferredSize());
	}

	public void setMaxWidth(int width) {
		maxWidth = width;
	}

	@Override
	protected void paintComponent(Graphics g) {
		int w = getWidth();
		if (w <= maxWidth) {
			// Static
			super.paintComponent(g);
		} else {
			// Wraparound scrolling
			w += spacer;
			int offset = (int) ((System.currentTimeMillis() / speed) % w);
			g.translate(dir * offset, 0);
			super.paintComponent(g);
			g.translate(-dir * w, 0);
			super.paintComponent(g);
			if (timer == null) {
				timer = new Timer(interval, (ActionEvent e) -> repaint());
				timer.start();
			}
		}
	}
}
