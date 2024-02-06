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
package net.pms.swing.gui.tabs.status;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.renderers.Renderer;
import net.pms.swing.components.SvgMultiResolutionImage;
import net.pms.swing.gui.JavaGui;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererImage extends JButton {

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererImage.class);
	private static final Color BACKGROUND = UIManager.getLookAndFeelDefaults().getColor("Button.background");
	private static final Color HIGHLIGHT = UIManager.getLookAndFeelDefaults().getColor("Button.highlight");

	protected transient Image source;
	protected transient Image grey;
	protected boolean isGrey;

	public RendererImage() {
		this(null);
	}

	public RendererImage(Renderer renderer) {
		setLayout(null);
		setOpaque(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		if (renderer != null) {
			set(renderer);
		}
	}

	public final void set(Renderer renderer) {
		source = getRendererIcon(renderer.getRendererIcon(), renderer.getRendererIconOverlays());
		//check the source size
		//The height is 128 pixels.
		//The width does not exceed 300 pixels.
		int w = 0;
		int h = 0;
		if (source != null) {
			w = source.getWidth(null);
			h = source.getHeight(null);
			if (w > 300 || h != 128) {
				//this icon is not compliant.
				//todo : fix the icon size (size to 128 then crop if needed)
				LOGGER.warn("Renderer icon \"{}\" is not sized correctly", renderer.getRendererIcon());
			}
		}
		Insets insets = getInsets();
		Dimension dimension =
			new Dimension(
			w + insets.left + insets.right,
			h + insets.top + insets.bottom);
		setPreferredSize(dimension);
		revalidate();
		repaint();
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
			graphics2d.drawImage(
				getCurrentSource(),
				AffineTransform.getTranslateInstance(i, j),
				null);
		} catch (OutOfMemoryError e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	public void enableRollover() {
		setRolloverEnabled(true);
		getModel().addChangeListener((ChangeEvent e) -> {
			if (e.getSource() != null) {
				ButtonModel model1 = (ButtonModel) e.getSource();
				setBackground(model1.isRollover() ? HIGHLIGHT : BACKGROUND);
			}
		});
	}

	public void setGrey(boolean b) {
		if (isGrey != b) {
			isGrey = b;
			SwingUtilities.invokeLater(() -> repaint());
		}
	}

	public Image getCurrentSource() {
		if (isGrey && grey == null) {
			grey = greyed(source, 60);
		}
		return isGrey ? grey : source;
	}

	public Image greyed(Image image, int pct) {
		if (image == null) {
			return null;
		}
		ImageFilter filter = new GrayFilter(true, pct);
		if (image instanceof SvgMultiResolutionImage svgImage) {
			return new SvgMultiResolutionImage(svgImage.getSVGDocument(), filter);
		}
		ImageProducer producer = new FilteredImageSource(image.getSource(), filter);
		return createImage(producer);
	}

	private static Image getRendererIcon(String icon) {
		BufferedImage bi = null;

		if (icon != null) {

			if (icon.matches(".*\\S+://.*")) {
				try {
					bi = ImageIO.read(URI.create(icon).toURL());
				} catch (IOException e) {
					LOGGER.debug("Error reading icon url: " + e);
				}
				if (bi != null) {
					return bi;
				} else {
					LOGGER.debug("Unable to read icon url \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					icon = RendererConfiguration.UNKNOWN_ICON;
				}
			}

			try {
				InputStream is = null;

				/**
				 * Check for a custom icon file first
				 *
				 * The file can be a) the name of a file in the renderers
				 * directory b) a path relative to the UMS working directory or
				 * c) an absolute path. If no file is found, the built-in
				 * resource (if any) is used instead.
				 *
				 * The File constructor does the right thing for the relative
				 * and absolute path cases, so we only need to detect the bare
				 * filename case.
				 *
				 * RendererIcon = foo.png // e.g. $UMS/renderers/foo.png
				 * RendererIcon = images/foo.png // e.g. $UMS/images/foo.png
				 * RendererIcon = /path/to/foo.png
				 */
				File f = RendererConfigurations.getRenderersIconFile(icon);
				if (f.isFile() && f.exists()) {
					is = new FileInputStream(f);
				}

				if (is == null) {
					is = JavaGui.class.getResourceAsStream("/resources/images/clients/" + icon);
				}

				if (is == null) {
					is = JavaGui.class.getResourceAsStream("/renderers/" + icon);
				}

				if (is != null && "svg".equalsIgnoreCase(FileUtil.getExtension(icon))) {
					return new SvgMultiResolutionImage(is);
				}
				if (is == null) {
					LOGGER.debug("Unable to read icon \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					is = JavaGui.class.getResourceAsStream("/resources/images/clients/" + RendererConfiguration.UNKNOWN_ICON);
				}

				if (is != null) {
					bi = ImageIO.read(is);
				}

			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
		if (bi == null) {
			LOGGER.debug("Failed to load icon: " + icon);
		}
		return bi;
	}

	private static Image getRendererIcon(String icon, String overlays) {
		Image bi = getRendererIcon(icon);
		if (bi != null && StringUtils.isNotBlank(overlays)) {
			Graphics g = bi.getGraphics();
			g.setColor(Color.DARK_GRAY);
			for (String overlay : overlays.split("[\u007c]")) {
				if (overlay.contains("@")) {
					String text = overlay.substring(0, overlay.indexOf("@"));
					String[] values = overlay.substring(overlay.indexOf("@") + 1).split(",");
					if (values.length == 3) {
						int x = Integer.parseInt(values[0]);
						int y = Integer.parseInt(values[1]);
						int size = Integer.parseInt(values[2]);
						g.setFont(new Font("Courier", Font.BOLD, size));
						g.drawString(text, x, y);
					}
				}
			}
			g.dispose();
		}
		return bi;
	}

}