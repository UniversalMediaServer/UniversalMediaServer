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
package net.pms.newgui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.plaf.ProgressBarUI;
import net.pms.Messages;
import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuiUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(GuiUtil.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private GuiUtil() {
	}

	/**
	 * Check swing availability.
	 * It don't use java.awt.GraphicsEnvironment.isHeadless() as some Linux
	 * distributions seem to not handle it properly.
	 * @return true if is headless, false if swing is available
	 */
	public static boolean isHeadless() {
		try {
			JDialog d = new JDialog();
			d.dispose();
			return false;
		} catch (NoClassDefFoundError | HeadlessException | InternalError e) {
			return true;
		}
	}

	/**
	 * Brings up a dialog with a yes/no choice
	 * @param message the message to display
	 * @param title the title string for the dialog
	 * @param defaultValue the default value
	 * @return an boolean indicating the option chosen by the user
	 */
	public static boolean askYesNoMessage(Object message, String title, boolean defaultValue) {
		if (!PMS.isHeadless()) {
			Object[] yesNoOptions = {
				Messages.getString("Yes"),
				Messages.getString("No")
			};
			int result = JOptionPane.showOptionDialog(
				null,
				message,
				title,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				yesNoOptions,
				defaultValue ? yesNoOptions[0] : yesNoOptions[1]
			);
			return result == JOptionPane.YES_OPTION;
		}
		return defaultValue;
	}

	/**
	 * Init the default platform-specific implementation of Toolkit.
	 * @return false if a toolkit could not be found, or if one could not be accessed or instantiated.
	 */
	public static boolean initDefaultToolkit() {
		try {
			Toolkit.getDefaultToolkit();
			return true;
		} catch (AWTError t) {
			LOGGER.error("Toolkit error: " + t.getClass().getName() + ": " + t.getMessage());
			return false;
		}
	}

	/**
	 * Wraps a {@link JComponent} into a {@link JPanel} using a {@link BorderLayout}, adding it to WEST.<br>
	 * If using this method for e.g. a {@link JCheckBox} and adding it to a layout, the {@link JCheckBox} won't
	 * span the entire space and thus, it won't change the checked state if clicking outside of it.
	 *
	 * @param component the component
	 * @return the preferred size component
	 */
	public static JComponent getPreferredSizeComponent(JComponent component) {
		JPanel pWrap = new JPanel(new BorderLayout());
		pWrap.add(component, BorderLayout.WEST);
		return pWrap;
	}

	// A progress bar with smooth transitions
	public static class SmoothProgressBar extends CustomUIProgressBar {
		private static final long serialVersionUID = 4418306779403459913L;

		public SmoothProgressBar(int min, int max) {
			super(min, max, null);
		}

		public SmoothProgressBar(int min, int max, ProgressBarUI ui) {
			super(min, max, ui);
		}

		@Override
		public void setValue(int n) {
			int v = getValue();
			if (n != v) {
				int step = n > v ? 1 : -1;
				n += step;
				try {
					for (; v != n; v += step) {
						super.setValue(v);
						Thread.sleep(10);
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	// A simple flat borderless progress bar painter,
	// required for Windows where setBorderPainted etc are ignored by the default laf
	public static class SimpleProgressUI extends javax.swing.plaf.basic.BasicProgressBarUI {
		Color fg, bg;

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

	// A fixed-size horizontal content-centering panel.
	public static class FixedPanel extends JPanel {
		private static final long serialVersionUID = 8295684215937548109L;

		public FixedPanel(int w, int h) {
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setSize(w, h);
			super.add(Box.createGlue());
		}

		@Override
		public void setSize(int w, int h) {
			Dimension d = new Dimension(w, h);
			setMaximumSize(d);
			setPreferredSize(d);
		}

		@Override
		public Component add(Component comp) {
			super.add(comp);
			super.add(Box.createGlue());
			return comp;
		}
	}

	// A label that automatically scrolls its text if
	// wider than a specified maximum.
	public static class MarqueeLabel extends JLabel {
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

	// A lightweight version of a label that automatically scrolls its text if
	// wider than a specified maximum.
	public static class ScrollLabel extends JLabel {
		private static final long serialVersionUID = 1834361616111946511L;
		String text;

		public ScrollLabel(String text) {
			super(text);
			this.text = text;
			setSize(getPreferredSize());
			scrollTheText();
		}

		@Override
		public void setText(String text) {
			super.setText(text);
			this.text = text;
		}

		private void scrollTheText() {
			new Timer(200, (ActionEvent e) -> {
				text = new StringBuffer(text.substring(1)).append(text.substring(0, 1)).toString();
				setText(text);
			}).start();
		}
	}

	// A progressbar ui with labeled subregions, a progress-sensitive main label, and tickmarks
	public static class SegmentedProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI {
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
		private ArrayList<Segment> segments;
		private ArrayList<Segment> mainLabel;

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
				g2.setColor(active.color);
				String label = active.label.replace("{}", "" + total);
				int labelWidth = (int) g.getFontMetrics().getStringBounds(label, g).getWidth();
				g2.drawString(label, x - labelWidth - 2, g2.getFontMetrics().getAscent());
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

	// A JProgressBar with a persistent custom ProgressBarUI.
	// This is to prevent replacement of the initial custom ui with
	// the laf's default ProgressBarUI as a result of future
	// invocations of JProgressBar.UpdateUI()
	public static class CustomUIProgressBar extends JProgressBar {
		private ProgressBarUI ui;

		public CustomUIProgressBar(int min, int max, ProgressBarUI ui) {
			super(min, max);
			this.ui = ui;
			setUI(ui);
		}

		@Override
		public void setUI(javax.swing.plaf.ProgressBarUI ui) {
			// Always prefer our own ui if we have one
			super.setUI(this.ui != null ? this.ui : ui);
		}
	}

	/**
	 * FlowLayout subclass that fully supports wrapping of components.
	 *
	 * Source: http://www.camick.com/java/source/WrapLayout.java
	 * Blog:   tips4java.wordpress.com/2008/11/06/wrap-layout/
	 */
	public static class WrapLayout extends FlowLayout {
		private static final long serialVersionUID = 8423910716184289166L;

		/**
		* Constructs a new <code>WrapLayout</code> with a centered
		* alignment and a default 5-unit horizontal and vertical gap.
		*/
		public WrapLayout() {
			super();
		}

		/**
		* Constructs a new <code>WrapLayout</code> with the specified
		* alignment and a default 5-unit horizontal and vertical gap.
		* <p>
		* The value of the alignment argument must be one of
		* <code>FlowLayout.LEFT</code>, <code>FlowLayout.CENTER</code>,
		* <code>FlowLayout.RIGHT</code>, <code>FlowLayout.LEADING</code>,
		* or <code>FlowLayout.TRAILING</code>.
		*
		* @param align the alignment value
		*/
		public WrapLayout(int align) {
			super(align);
		}

		/**
		* Creates a new wrap layout manager with the indicated alignment
		* and the indicated horizontal and vertical gaps.
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
		* @param target the component which needs to be laid out
		* @return the preferred dimensions to lay out the
		* subcomponents of the specified container
		*/
		@Override
		public Dimension preferredLayoutSize(Container target) {
			return layoutSize(target, true);
		}

		/**
		* Returns the minimum dimensions needed to layout the <i>visible</i>
		* components contained in the specified target container.
		* @param target the component which needs to be laid out
		* @return the minimum dimensions to lay out the
		* subcomponents of the specified container
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

	public static void enableContainer(Container c, boolean enable) {
		for (Component component : c.getComponents()) {
			component.setEnabled(enable);
			if (component instanceof Container container) {
				enableContainer(container, enable);
			}
		}
	}
}
