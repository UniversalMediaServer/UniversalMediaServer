package net.pms.newgui;

import java.awt.*;
import javax.swing.*;

public final class GuiUtil {

	public static class CustomJButton extends JButton {
		private static final long serialVersionUID = -528428545289132331L;

		public CustomJButton(String string) {
			super(string);
			this.setRequestFocusEnabled(false);
		}

		public CustomJButton(Icon icon) {
			super(null, icon);
			this.setRequestFocusEnabled(false);
		}

		public CustomJButton(String string, Icon icon) {
			super(string, icon);
			this.setRequestFocusEnabled(false);
		}
	}

	public static class MyComboBoxModel extends DefaultComboBoxModel<Object> {
		private static final long serialVersionUID = -9094365556516842551L;

		public MyComboBoxModel() {
			super();
		}

		public MyComboBoxModel(Object[] items) {
			super(items);
		}

		@Override
		public Object getElementAt(int index) {
			String s = (String) super.getElementAt(index);
			return s;
		}
	}

	// A progresss bar with smooth transitions
	public static class SmoothProgressBar extends JProgressBar {
		public SmoothProgressBar(int min, int max) {
			super(min, max);
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
			Graphics2D g2 = (Graphics2D)g;
			g2.setColor(progressBar.getForeground());
			g2.setStroke(new BasicStroke((float)h, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			g2.drawLine(b.left, (h/2) + b.top, filled + b.left, (h/2) + b.top);
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
		public FixedPanel(int w, int h) {
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setMaximumSize(new Dimension(w, h));
			setPreferredSize(new Dimension(w, h));
			super.add(Box.createGlue());
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
		public int speed, spacer, dir, max_w, interval = 30;

		public MarqueeLabel(String text, int width) {
			this(text, width, 30, -1, 10);
		}

		public MarqueeLabel(String text, int width, int speed, int dir, int spacer) {
			super(text);
			this.max_w = width;
			this.speed = speed;
			this.dir = dir;
			this.spacer = spacer;
			setSize(getPreferredSize());
		}

		@Override
		protected void paintComponent(Graphics g) {
			int w = getWidth();
			if (w <= max_w) {
				// Static
				super.paintComponent(g);
			} else {
				// Wraparound scrolling
				w += spacer;
				int offset = (int)((System.currentTimeMillis() / speed) % w);
				g.translate(dir * offset, 0);
				super.paintComponent(g);
				g.translate(-dir * w, 0);
				super.paintComponent(g);
				repaint(interval);
			}
		}
	}
}
