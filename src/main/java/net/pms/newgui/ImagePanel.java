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
package net.pms.newgui;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImagePanel extends JButton {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImagePanel.class);
	private static final long serialVersionUID = -6709086531128513425L;
	protected RenderedImage source, grey;
	protected int originX;
	protected int originY;
	protected boolean isGrey;

	public ImagePanel() {
		this(null);
	}

	public ImagePanel(RenderedImage renderedimage) {
		source = null;
		originX = 0;
		originY = 0;
		setLayout(null);
		if (renderedimage != null) {
			source = renderedimage;
			int i = source.getWidth();
			int j = source.getHeight();
			Insets insets = getInsets();
			Dimension dimension =
				new Dimension(
				i + insets.left + insets.right,
				j + insets.top + insets.bottom);
			setPreferredSize(dimension);
		}
		setOpaque(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
	}

	public void setOrigin(int i, int j) {
		originX = i;
		originY = j;
		repaint();
	}

	public Point getOrigin() {
		return new Point(originX, originY);
	}

	public void set(RenderedImage renderedimage) {
		source = renderedimage;
		int i = 0;
		int j = 0;
		if (renderedimage != null) {
			i = source.getWidth();
			j = source.getHeight();
		}
		Insets insets = getInsets();
		Dimension dimension =
			new Dimension(
			i + insets.left + insets.right,
			j + insets.top + insets.bottom);
		setPreferredSize(dimension);
		revalidate();
		repaint();
	}

	public void set(RenderedImage renderedimage, int i, int j) {
		if (renderedimage == null) {
			originX = 0;
			originY = 0;
		} else {
			originX = i;
			originY = j;
		}
		set(renderedimage);
	}

	public RenderedImage getSource() {
		return source;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		Graphics2D graphics2d = (Graphics2D) g;
		if (source == null) {
			graphics2d.setColor(getBackground());
			graphics2d.fillRect(0, 0, getWidth(), getHeight());
			return;
		}
		Rectangle rectangle = graphics2d.getClipBounds();
		graphics2d.setColor(getBackground());
		graphics2d.fillRect(
			rectangle.x,
			rectangle.y,
			rectangle.width,
			rectangle.height);
		Insets insets = getInsets();
		int i = insets.left + originX;
		int j = insets.top + originY;
		try {
			graphics2d.drawRenderedImage(
				getCurrentSource(),
				AffineTransform.getTranslateInstance(i, j));
		} catch (OutOfMemoryError e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	static final Color background = (Color) UIManager.getLookAndFeelDefaults().get("Button.background");
	static final Color highlight = (Color) UIManager.getLookAndFeelDefaults().get("Button.highlight");

	public void enableRollover() {
		setRolloverEnabled(true);
		getModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (getSource() != null) {
					ButtonModel model = (ButtonModel) e.getSource();
					setBackground(model.isRollover() ? highlight : background);
					//setBorderPainted(model.isRollover()); // some lafs ignore borderPainted
					//setCursor(model.isRollover() ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
	}

	public void setGrey(boolean b) {
		if (isGrey != b) {
			isGrey = b;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					repaint();
				}
			});
		}
	}

	public boolean isGrey() {
		return isGrey;
	}

	public RenderedImage getCurrentSource() {
		if (isGrey && grey == null && source != null) {
			grey = greyed((BufferedImage) source, 60);
		}
		return isGrey ? grey : source;
	}

	public BufferedImage greyed(BufferedImage bi, int pct) {
		ImageFilter filter = new GrayFilter(true, pct);
		ImageProducer producer = new FilteredImageSource(bi.getSource(), filter);
		return toBufferedImage(createImage(producer));
	}

	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}
		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return bi;
	}
}
