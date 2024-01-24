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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.gui.IRendererGuiListener;
import net.pms.newgui.components.RendererImagePanel;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererPanel implements ActionListener, IRendererGuiListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererPanel.class);
	private static final int MINIMUM_FILENAME_DISPLAY_SIZE = 200;
	private static final Color BUF_COLOR = new Color(255, 128, 0, 128);

	private final RendererImagePanel icon;
	private final JLabel label;
	private final GuiUtil.MarqueeLabel playingLabel;
	private final GuiUtil.FixedPanel playing;
	private final JLabel time;
	private final int bufferSize;
	private final GuiUtil.SmoothProgressBar rendererProgressBar;

	private RendererFrame rendererFrame;
	private String name = " ";
	private JPanel panel = null;

	public RendererPanel(Renderer renderer) {
		icon = addRendererIcon(renderer.getRendererIcon(), renderer.getRendererIconOverlays());
		label = new JLabel(renderer.getRendererName());
		playingLabel = new GuiUtil.MarqueeLabel(" ");
		playingLabel.setForeground(Color.gray);
		int h = (int) playingLabel.getSize().getHeight();
		playing = new GuiUtil.FixedPanel(200, h);
		playing.add(playingLabel);
		time = new JLabel(" ");
		time.setForeground(Color.gray);
		bufferSize = renderer.getUmsConfiguration().getMaxMemoryBufferSize();
		rendererProgressBar = new GuiUtil.SmoothProgressBar(0, 100, new GuiUtil.SimpleProgressUI(Color.gray, Color.gray));
		rendererProgressBar.setStringPainted(true);
		rendererProgressBar.setBorderPainted(false);
		if (renderer.getAddress() != null) {
			rendererProgressBar.setString(renderer.getAddress().getHostAddress());
		}
		rendererProgressBar.setForeground(BUF_COLOR);
		if (icon != null) {
			icon.enableRollover();
			icon.setAction(new AbstractAction() {
				private static final long serialVersionUID = -6316055325551243347L;

				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(() -> {
						if (rendererFrame == null) {
							JFrame top = (JFrame) SwingUtilities.getWindowAncestor(getPanel());
							// We're using JFrame instead of JDialog here so as to
							// have a minimize button. Since the player panel
							// is intrinsically a freestanding module this approach
							// seems valid to me but some frown on it: see
							// http://stackoverflow.com/questions/9554636/the-use-of-multiple-jframes-good-bad-practice
							rendererFrame = new RendererFrame(top, renderer);
						} else {
							rendererFrame.setExtendedState(Frame.NORMAL);
							rendererFrame.setVisible(true);
							rendererFrame.toFront();
						}
					});
				}
			});
		}
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		PlayerState state = ((BasicPlayer) e.getSource()).getState();
		time.setText((state.isStopped() || StringUtil.isZeroTime(state.getPosition())) ? " " :
				UMSUtils.playedDurationStr(state.getPosition(), state.getDuration()));
		rendererProgressBar.setValue((int) (100 * state.getBuffer() / bufferSize));
		String n = (state.isStopped() || StringUtils.isBlank(state.getName())) ? " " : state.getName();
		if (!name.equals(n)) {
			name = n;
			playingLabel.setText(name);
		}
		// Maximize the playing label width if not already done
		if (playing.getSize().width == 0) {
			int w = panel.getWidth() - panel.getInsets().left - panel.getInsets().right;
			if (w < MINIMUM_FILENAME_DISPLAY_SIZE) {
				w = MINIMUM_FILENAME_DISPLAY_SIZE;
			}
			playing.setSize(w, (int) playingLabel.getSize().getHeight());
			playingLabel.setMaxWidth(w);
		}
	}

	@Override
	public void refreshPlayerState(final PlayerState state) {
		time.setText((state.isStopped() || StringUtil.isZeroTime(state.getPosition())) ? " " :
				UMSUtils.playedDurationStr(state.getPosition(), state.getDuration()));
		rendererProgressBar.setValue((int) (100 * state.getBuffer() / bufferSize));
		String n = (state.isStopped() || StringUtils.isBlank(state.getName())) ? " " : state.getName();
		if (!name.equals(n)) {
			name = n;
			playingLabel.setText(name);
		}
	}

	public void addTo(Container parent) {
		parent.add(getPanel());
		parent.validate();
		// Maximize the playing label width
		int w = panel.getWidth() - panel.getInsets().left - panel.getInsets().right;
		playing.setSize(w, (int) playingLabel.getSize().getHeight());
		playingLabel.setMaxWidth(w);
	}

	@Override
	public void delete() {
		SwingUtilities.invokeLater(() -> {
			try {
				// Delete the popup if open
				if (rendererFrame != null) {
					rendererFrame.dispose();
					rendererFrame = null;
				}
				Container parent = panel.getParent();
				parent.remove(panel);
				parent.revalidate();
				parent.repaint();
			} catch (Exception e) {
				//nothing to do
			}
		});
	}

	public JPanel getPanel() {
		if (panel == null) {
			UmsFormBuilder b = UmsFormBuilder.create().layout(new FormLayout(
					"center:pref",
					"max(140px;pref), 3dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"
			));
			b.opaque(true);
			CellConstraints cc = new CellConstraints();
			b.add(icon).at(cc.xy(1, 1));
			b.add(label).at(cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
			b.add(rendererProgressBar).at(cc.xy(1, 5));
			b.add(playing).at(cc.xy(1, 7, CellConstraints.CENTER, CellConstraints.DEFAULT));
			b.add(time).at(cc.xy(1, 9));
			panel = b.getPanel();
		}
		return panel;
	}

	@Override
	public void updateRenderer(final Renderer renderer) {
		SwingUtilities.invokeLater(() -> {
			icon.set(getRendererIcon(renderer.getRendererIcon(), renderer.getRendererIconOverlays()));
			label.setText(renderer.getRendererName());
			// Update the popup panel if it's been opened
			if (rendererFrame != null) {
				rendererFrame.update();
			}
		});
	}

	@Override
	public void setActive(final boolean active) {
		icon.setGrey(!active);
	}

	@Override
	public void setAllowed(boolean allowed) {
		// not implemented on Java GUI
	}

	@Override
	public void setUserId(int userId) {
		// not implemented on Java GUI
	}

	private static RendererImagePanel addRendererIcon(String icon, String overlays) {
		BufferedImage bi = getRendererIcon(icon, overlays);
		return bi != null ? new RendererImagePanel(bi) : null;
	}

	private static BufferedImage getRendererIcon(String icon) {
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
				 * The file can be a) the name of a file in the renderers directory b) a path relative
				 * to the UMS working directory or c) an absolute path. If no file is found,
				 * the built-in resource (if any) is used instead.
				 *
				 * The File constructor does the right thing for the relative and absolute path cases,
				 * so we only need to detect the bare filename case.
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
					is = LooksFrame.class.getResourceAsStream("/resources/images/clients/" + icon);
				}

				if (is == null) {
					is = LooksFrame.class.getResourceAsStream("/renderers/" + icon);
				}

				if (is == null) {
					LOGGER.debug("Unable to read icon \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					is = LooksFrame.class.getResourceAsStream("/resources/images/clients/" + RendererConfiguration.UNKNOWN_ICON);
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

	private static BufferedImage getRendererIcon(String icon, String overlays) {
		BufferedImage bi = getRendererIcon(icon);
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
