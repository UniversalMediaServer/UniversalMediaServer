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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;
import org.apache.commons.lang3.StringUtils;

/**
 * A progressbar ui with labeled subregions, a progress-sensitive main label,
 * and tickmarks.
 */
public class SegmentedProgressBarUI extends BasicProgressBarUI implements Serializable {

	Color fg;
	Color bg;

	static class Segment {

		String label;
		Color color;
		int val;

		Segment(String l, Color c) {
			label = l;
			color = c;
		}
	}

	private transient ArrayList<Segment> segments;
	private transient ArrayList<Segment> mainLabel;

	private int tickmarks;
	private String tickLabel;

	public SegmentedProgressBarUI() {
		this(null, null);
	}

	public SegmentedProgressBarUI(Color fg, Color bg) {
		segments = new ArrayList<>();
		mainLabel = new ArrayList<>();
		this.fg = fg != null ? fg : super.getSelectionForeground();
		this.bg = bg != null ? bg : super.getSelectionBackground();
		tickmarks = 0;
		tickLabel = "{}";
	}

	public synchronized int addSegment(String label, Color c) {
		// Set color transparency to 50% so tickmarks are visible
		segments.add(new Segment(label, new Color(c.getRed(), c.getGreen(), c.getBlue(), 128)));
		return segments.size() - 1;
	}

	public synchronized void setActiveLabel(String label, Color c, int pct) {
		Segment s = new Segment(label, c);
		// This label will be activated if progress equals or exceeds this percentage
		s.val = pct;
		mainLabel.add(s);
	}

	public synchronized void setTickMarks(int units, String label) {
		tickmarks = units;
		tickLabel = label;
	}

	public synchronized void setValues(int min, int max, int... vals) {
		int total = 0;
		for (int i = 0; i < vals.length; i++) {
			segments.get(i).val = vals[i];
			total += vals[i];
		}
		progressBar.setMinimum(min);
		progressBar.setMaximum(max);
		progressBar.setValue(total);
	}

	private int total() {
		int size = 0;
		for (Segment s : segments) {
			size += s.val;
		}
		return size;
	}

	@Override
	protected synchronized void paintDeterminate(Graphics g, JComponent c) {
		Insets b = progressBar.getInsets();
		int w = progressBar.getWidth() - (b.right + b.left);
		int h = progressBar.getHeight() - (b.top + b.bottom);
		if (w < 1 || h < 1) {
			return;
		}
		int max = progressBar.getMaximum();
		getAmountFull(b, w, h);
		float unit = (float) w / max;
		int total = total();
		int x = b.left;
		Graphics2D g2 = (Graphics2D) g;
		// Draw the tick marks, if any
		if (tickmarks > 0) {
			g2.setStroke(new BasicStroke());
			g2.setColor(Color.gray);
			paintTicks(g, b.left, (int) (unit * tickmarks), b.left + w);
		}
		g2.setStroke(new BasicStroke(h, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		// Draw the segments
		for (Segment s : segments) {
			if (s.val > 0) {
				int segmentWidth = (int) (s.val * unit);
				g2.setColor(s.color);
				g2.drawLine(x, (h / 2) + b.top, x + segmentWidth, (h / 2) + b.top);
				// Draw the segment string, if any
				if (progressBar.isStringPainted() && StringUtils.isNotBlank(s.label)) {
					progressBar.setString(s.label);
					paintString(g, x, b.top, segmentWidth, h, segmentWidth, b);
				}
				x += segmentWidth;
			}
		}
		// Draw the main label, if any
		if (progressBar.isStringPainted() && !mainLabel.isEmpty()) {
			// Find the active label for this percentage
			Segment active = null;
			int pct = total * 100 / max;
			for (Segment s : mainLabel) {
				if (s.val <= pct) {
					active = s;
				}
			}
			if (active != null) {
				g2.setColor(active.color);
				String label = active.label.replace("{}", "" + total);
				int labelWidth = (int) g.getFontMetrics().getStringBounds(label, g).getWidth();
				g2.drawString(label, x - labelWidth - 2, g2.getFontMetrics().getAscent());
			}
		}
	}

	private void paintTicks(Graphics g, int x0, int step, int max) {
		if (step < 1) {
			return;
		}
		int h = progressBar.getHeight();
		Font font = g.getFont();
		// Use a small font
		g.setFont(font.deriveFont((float) 10));
		int x;
		for (x = x0 + step; x < max; x += step) {
			// Draw a half-height tick mark
			g.drawLine(x, h / 2, x, h);
			// And label it, if required
			if (tickLabel != null) {
				String s = tickLabel.replace("{}", "" + ((x - x0) / step) * tickmarks);
				int w = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
				g.drawString(s, x - 3 - w, h - 3);
			}
		}
		// Draw the max value if it's not at a tick mark and we have room
		if (tickLabel != null && ((max - x0) % step != 0)) {
			int availableWidth = max - x + step;
			String s = "" + progressBar.getMaximum();
			int w = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
			if (availableWidth > w + 10) {
				g.drawString(s, max - 3 - w, h - 3);
			}
		}
		// Restore the original font
		g.setFont(font);
	}

	@Override
	protected synchronized Color getSelectionForeground() {
		return fg;
	}

	@Override
	protected synchronized Color getSelectionBackground() {
		return bg;
	}
}
