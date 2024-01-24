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
package net.pms.newgui.components;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererImagePanel extends JButton {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererImagePanel.class);
	private static final long serialVersionUID = -6709086531128513425L;
	protected transient RenderedImage source;
	protected transient RenderedImage grey;
	protected boolean isGrey;

	public RendererImagePanel() {
		this(null);
	}

	public RendererImagePanel(RenderedImage renderedimage) {
		source = null;
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
		int i = insets.left;
		int j = insets.top;
		try {
			graphics2d.drawRenderedImage(
				getCurrentSource(),
				AffineTransform.getTranslateInstance(i, j));
		} catch (OutOfMemoryError e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	static final Color BACKGROUND = (Color) UIManager.getLookAndFeelDefaults().get("Button.background");
	static final Color HIGHLIGHT = (Color) UIManager.getLookAndFeelDefaults().get("Button.highlight");

	public void enableRollover() {
		setRolloverEnabled(true);
		getModel().addChangeListener((ChangeEvent e) -> {
			if (getSource() != null) {
				ButtonModel model1 = (ButtonModel) e.getSource();
				setBackground(model1.isRollover() ? HIGHLIGHT : BACKGROUND);
				//setBorderPainted(model.isRollover()); // some lafs ignore borderPainted
				//setCursor(model.isRollover() ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}

	public void setGrey(boolean b) {
		if (isGrey != b) {
			isGrey = b;
			SwingUtilities.invokeLater(() -> repaint());
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

	public BufferedImage greyed(Image bi, int pct) {
		ImageFilter filter = new GrayFilter(true, pct);
		ImageProducer producer = new FilteredImageSource(bi.getSource(), filter);
		return toBufferedImage(createImage(producer));
	}

	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage bufferedImage) {
			return bufferedImage;
		}
		BufferedImage bi = null;
		if (img != null) {
			bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bi.createGraphics();
			g.drawImage(img, 0, 0, null);
			g.dispose();
		}
		return bi;
	}

}
