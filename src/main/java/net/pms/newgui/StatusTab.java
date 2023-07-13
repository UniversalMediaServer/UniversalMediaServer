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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.*;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.net.URL;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.gui.EConnectionState;
import net.pms.gui.IRendererGuiListener;
import net.pms.newgui.components.AnimatedIcon;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconStage;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconType;
import net.pms.newgui.components.JAnimatedButton;
import net.pms.newgui.components.ServerBindMouseListener;
import net.pms.newgui.util.FormLayoutUtil;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.players.BasicPlayer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusTab.class);
	private static final Color MEM_COLOR = new Color(119, 119, 119, 128);
	private static final Color BUF_COLOR = new Color(75, 140, 181, 128);
	private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");
	private static final int MINIMUM_FILENAME_DISPLAY_SIZE = 200;

	public static class RendererItem implements ActionListener, IRendererGuiListener {
		private final ImagePanel icon;
		private final JLabel label;
		private final GuiUtil.MarqueeLabel playingLabel;
		private final GuiUtil.FixedPanel playing;
		private final JLabel time;
		private JFrame frame;
		private final GuiUtil.SmoothProgressBar rendererProgressBar;
		private RendererPanel rendererPanel;
		private String name = " ";
		private JPanel panel = null;

		public RendererItem(Renderer renderer) {
			icon = addRendererIcon(renderer.getRendererIcon(), renderer.getRendererIconOverlays());
			icon.enableRollover();
			label = new JLabel(renderer.getRendererName());
			playingLabel = new GuiUtil.MarqueeLabel(" ");
			playingLabel.setForeground(Color.gray);
			int h = (int) playingLabel.getSize().getHeight();
			playing = new GuiUtil.FixedPanel(200, h);
			playing.add(playingLabel);
			time = new JLabel(" ");
			time.setForeground(Color.gray);
			rendererProgressBar = new GuiUtil.SmoothProgressBar(0, 100, new GuiUtil.SimpleProgressUI(Color.gray, Color.gray));
			rendererProgressBar.setStringPainted(true);
			rendererProgressBar.setBorderPainted(false);
			if (renderer.getAddress() != null) {
				rendererProgressBar.setString(renderer.getAddress().getHostAddress());
			}
			rendererProgressBar.setForeground(BUF_COLOR);
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
					if (frame != null) {
						frame.dispose();
						frame = null;
					}
					Container parent = panel.getParent();
					parent.remove(panel);
					parent.revalidate();
					parent.repaint();
				} catch (Exception e) {
				}
			});
		}

		public JPanel getPanel() {
			if (panel == null) {
				PanelBuilder b = new PanelBuilder(new FormLayout(
					"center:pref",
					"max(140px;pref), 3dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"
				));
				b.opaque(true);
				CellConstraints cc = new CellConstraints();
				b.add(icon, cc.xy(1, 1));
				b.add(label, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
				b.add(rendererProgressBar, cc.xy(1, 5));
				b.add(playing, cc.xy(1, 7, CellConstraints.CENTER, CellConstraints.DEFAULT));
				b.add(time, cc.xy(1, 9));
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
				if (rendererPanel != null) {
					rendererPanel.update();
				}
			});
		}

		@Override
		public void setActive(final boolean active) {
			icon.setGrey(!active);
		}
	}

	private JPanel renderers;
	private JLabel mediaServerBindLabel;
	private JLabel interfaceServerBindLabel;
	private GuiUtil.SegmentedProgressBarUI memBarUI;
	private JLabel currentBitrate;
	private JLabel peakBitrate;

	private static int bufferSize;
	private EConnectionState connectionState = EConnectionState.UNKNOWN;
	private final JAnimatedButton connectionStatus = new JAnimatedButton();
	private final AnimatedIcon searchingIcon;
	private final AnimatedIcon connectedIcon;
	private final AnimatedIcon disconnectedIcon;
	private final AnimatedIcon blockedIcon;

	/**
	 * Shows a simple visual status of the server.
	 *
	 * @todo choose better icons for these
	 * @param configuration
	 */
	StatusTab(UmsConfiguration configuration) {
		// Build Animations
		searchingIcon = new AnimatedIcon(connectionStatus, "icon-status-connecting.png");

		connectedIcon = new AnimatedIcon(connectionStatus, "icon-status-connected.png");

		disconnectedIcon = new AnimatedIcon(connectionStatus, "icon-status-disconnected.png");

		blockedIcon = new AnimatedIcon(connectionStatus, "icon-status-warning.png");

		bufferSize = configuration.getMaxMemoryBufferSize();
	}

	void setConnectionState(EConnectionState connectionState) {
		if (connectionState == null) {
			throw new IllegalArgumentException("connectionState cannot be null");
		}
		if (!connectionState.equals(this.connectionState)) {
			this.connectionState = connectionState;
			AnimatedIcon oldIcon = (AnimatedIcon) connectionStatus.getIcon();
			switch (connectionState) {
				case SEARCHING -> {
					connectionStatus.setToolTipText(Messages.getString("SearchingForRenderers"));
					searchingIcon.restartArm();
					if (oldIcon != null) {
						oldIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, searchingIcon, false));
					} else {
						connectionStatus.setIcon(searchingIcon);
					}
				}
				case CONNECTED -> {
					connectionStatus.setToolTipText(Messages.getString("Connected"));
					connectedIcon.restartArm();
					if (oldIcon != null) {
						oldIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, connectedIcon, false));
					} else {
						connectionStatus.setIcon(connectedIcon);
					}
				}
				case DISCONNECTED -> {
					connectionStatus.setToolTipText(Messages.getString("NoRenderersWereFound"));
					disconnectedIcon.restartArm();
					if (oldIcon != null) {
						oldIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, disconnectedIcon, false));
					} else {
						connectionStatus.setIcon(disconnectedIcon);
					}
				}
				case BLOCKED -> {
					connectionStatus.setToolTipText(Messages.getString("PortBlockedChangeIt"));
					blockedIcon.reset();
					if (oldIcon != null) {
						oldIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, blockedIcon, false));
					} else {
						connectionStatus.setIcon(blockedIcon);
					}
				}
				default -> connectionStatus.setIcon(null);
			}
		}
	}

	public JComponent build() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());

		String colSpec = FormLayoutUtil.getColSpec("pref, 30dlu, fill:pref:grow, 30dlu, pref", orientation);
		//                                             1     2          3           4     5

		//RowSpec.decode("bottom:max(50dlu;pref)");
		FormLayout layout = new FormLayout(colSpec,
			//                          1     2          3            4     5
			//                   //////////////////////////////////////////////////
			"p," +               // Detected Media Renderers --------------------//  1
			"9dlu," +            //                                              //
			"fill:p:grow," +     //                 <renderers>                  //  3
			"3dlu," +            //                                              //
			"p," +               // ---------------------------------------------//  5
			"10dlu," +           //           |                       |          //
			"[10pt,p]," +        // Connected |  Memory Usage         |<bitrate> //  7
			"1dlu," +            //           |                       |          //
			"[30pt,p]," +        //  <icon>   |  <statusbar>          |          //  9
			"3dlu,"              //           |                       |          //
			//                   //////////////////////////////////////////////////
		);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DIALOG);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		// Renderers
		JComponent cmp = builder.addSeparator(Messages.getString("DetectedMediaRenderers"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		Font bold = cmp.getFont().deriveFont(Font.BOLD);
		Color fgColor = new Color(68, 68, 68);
		cmp.setFont(bold);

		renderers = new JPanel(new GuiUtil.WrapLayout(FlowLayout.CENTER, 20, 10));
		JScrollPane rsp = new JScrollPane(
			renderers,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rsp.setBorder(BorderFactory.createEmptyBorder());
		rsp.setPreferredSize(new Dimension(0, 260));
		rsp.getHorizontalScrollBar().setLocation(0, 250);

		builder.add(rsp, cc.xyw(1, 3, 5));

		cmp = builder.addSeparator(null, FormLayoutUtil.flip(cc.xyw(1, 5, 5), colSpec, orientation));

		connectedIcon.start();
		searchingIcon.start();
		disconnectedIcon.start();
		connectionStatus.setFocusable(false);

		// Bitrate
		String conColSpec = "left:pref, 3dlu, right:pref:grow";
		PanelBuilder connectionBuilder = new PanelBuilder(new FormLayout(conColSpec, "p, 1dlu, p, 1dlu, p"));
		connectionBuilder.add(connectionStatus, FormLayoutUtil.flip(cc.xywh(1, 1, 1, 3, "center, fill"), conColSpec, orientation));
		// Set initial connection state
		setConnectionState(EConnectionState.SEARCHING);

		JLabel mediaServerLabel = new JLabel("<html><b>" + Messages.getString("Servers") + "</b></html>");
		mediaServerLabel.setForeground(fgColor);
		connectionBuilder.add(mediaServerLabel, FormLayoutUtil.flip(cc.xy(3, 1, "left, top"), conColSpec, orientation));
		mediaServerBindLabel = new JLabel("-");
		mediaServerBindLabel.setForeground(fgColor);
		mediaServerBindLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		mediaServerBindLabel.addMouseListener(new ServerBindMouseListener(mediaServerBindLabel));
		mediaServerBindLabel.setToolTipText(Messages.getString("MediaServerIpAddress"));
		connectionBuilder.add(mediaServerBindLabel, FormLayoutUtil.flip(cc.xy(3, 3, "left, top"), conColSpec, orientation));
		interfaceServerBindLabel = new JLabel("-");
		interfaceServerBindLabel.setForeground(fgColor);
		interfaceServerBindLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		interfaceServerBindLabel.addMouseListener(new ServerBindMouseListener(interfaceServerBindLabel));
		interfaceServerBindLabel.setToolTipText(Messages.getString("WebSettingsServerIpAddress"));
		connectionBuilder.add(interfaceServerBindLabel, FormLayoutUtil.flip(cc.xy(3, 5, "left, top"), conColSpec, orientation));
		builder.add(connectionBuilder.getPanel(), FormLayoutUtil.flip(cc.xywh(1, 7, 1, 3, "left, top"), colSpec, orientation));

		// Memory
		memBarUI = new GuiUtil.SegmentedProgressBarUI(Color.white, Color.gray);
		memBarUI.setActiveLabel("{}", Color.white, 0);
		memBarUI.setActiveLabel("{}", Color.red, 90);
		memBarUI.addSegment("", MEM_COLOR);
		memBarUI.addSegment("", BUF_COLOR);
		memBarUI.setTickMarks(getTickMarks(), "{}");
		JProgressBar memoryProgressBar = new GuiUtil.CustomUIProgressBar(0, 100, memBarUI);
		memoryProgressBar.setStringPainted(true);
		memoryProgressBar.setForeground(new Color(75, 140, 181));
		memoryProgressBar.setString(Messages.getString("Empty"));

		JLabel mem = builder.addLabel("<html><b>" + Messages.getString("MemoryUsage") + "</b> (" + Messages.getString("Mb") + ")</html>", FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
		mem.setForeground(fgColor);
		builder.add(memoryProgressBar, FormLayoutUtil.flip(cc.xyw(3, 9, 1), colSpec, orientation));

		// Bitrate
		String bitColSpec = "left:pref, 3dlu, right:pref:grow";
		PanelBuilder bitrateBuilder = new PanelBuilder(new FormLayout(bitColSpec, "p, 1dlu, p, 1dlu, p"));

		JLabel bitrateLabel = new JLabel("<html><b>" + Messages.getString("Bitrate") + "</b> (" + Messages.getString("Mbs") + ")</html>");
		bitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(bitrateLabel, FormLayoutUtil.flip(cc.xy(1, 1), bitColSpec, orientation));

		JLabel currentBitrateLabel = new JLabel(Messages.getString("Current"));
		currentBitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(currentBitrateLabel, FormLayoutUtil.flip(cc.xy(1, 3), bitColSpec, orientation));

		currentBitrate = new JLabel("0");
		currentBitrate.setForeground(fgColor);
		bitrateBuilder.add(currentBitrate, FormLayoutUtil.flip(cc.xy(3, 3), bitColSpec, orientation));

		JLabel peakBitrateLabel = new JLabel(Messages.getString("Peak"));
		peakBitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(peakBitrateLabel, FormLayoutUtil.flip(cc.xy(1, 5), bitColSpec, orientation));

		peakBitrate = new JLabel("0");
		peakBitrate.setForeground(fgColor);
		bitrateBuilder.add(peakBitrate, FormLayoutUtil.flip(cc.xy(3, 5), bitColSpec, orientation));

		builder.add(bitrateBuilder.getPanel(), FormLayoutUtil.flip(cc.xywh(5, 7, 1, 3, "left, top"), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	public void setMediaServerBind(String bind) {
		SwingUtilities.invokeLater(() -> {
			mediaServerBindLabel.setText(bind);
		});
	}

	public void setInterfaceServerBind(String bind) {
		SwingUtilities.invokeLater(() -> {
			interfaceServerBindLabel.setText(bind);
		});
	}

	public void setCurrentBitrate(int sizeinMb) {
		currentBitrate.setText(FORMATTER.format(sizeinMb));
	}

	public void setPeakBitrate(int sizeinMb) {
		peakBitrate.setText(FORMATTER.format(sizeinMb));
	}

	public void addRenderer(final Renderer renderer) {
		final RendererItem r = new RendererItem(renderer);
		r.addTo(renderers);
		renderer.addGuiListener(r);
		r.icon.setAction(new AbstractAction() {
			private static final long serialVersionUID = -6316055325551243347L;

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					if (r.frame == null) {
						JFrame top = (JFrame) SwingUtilities.getWindowAncestor(r.getPanel());
						// We're using JFrame instead of JDialog here so as to
						// have a minimize button. Since the player panel
						// is intrinsically a freestanding module this approach
						// seems valid to me but some frown on it: see
						// http://stackoverflow.com/questions/9554636/the-use-of-multiple-jframes-good-bad-practice
						r.frame = new JFrame();
						r.rendererPanel = new RendererPanel(renderer);
						r.frame.add(r.rendererPanel);
						r.rendererPanel.update();
						r.frame.setResizable(false);
						r.frame.setIconImage(top.getIconImage());
						r.frame.setLocationRelativeTo(top);
						r.frame.setVisible(true);
					} else {
						r.frame.setExtendedState(Frame.NORMAL);
						r.frame.setVisible(true);
						r.frame.toFront();
					}
				});
			}
		});
	}

	public static ImagePanel addRendererIcon(String icon, String overlays) {
		BufferedImage bi = getRendererIcon(icon, overlays);
		return bi != null ? new ImagePanel(bi) : null;
	}

	public static BufferedImage getRendererIcon(String icon) {
		BufferedImage bi = null;

		if (icon != null) {

			if (icon.matches(".*\\S+://.*")) {
				try {
					bi = ImageIO.read(new URL(icon));
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
				 * to the PMS working directory or c) an absolute path. If no file is found,
				 * the built-in resource (if any) is used instead.
				 *
				 * The File constructor does the right thing for the relative and absolute path cases,
				 * so we only need to detect the bare filename case.
				 *
				 * RendererIcon = foo.png // e.g. $PMS/renderers/foo.png
				 * RendererIcon = images/foo.png // e.g. $PMS/images/foo.png
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

	public static BufferedImage getRendererIcon(String icon, String overlays) {
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

	private int getTickMarks() {
		int mb = (int) (Runtime.getRuntime().maxMemory() / 1048576);
		return mb < 1000 ? 100 : mb < 2500 ? 250 : mb < 5000 ? 500 : 1000;
	}

	public void setMemoryUsage(int maxMemory, int usedMemory, int bufferMemory) {
		SwingUtilities.invokeLater(() -> {
			memBarUI.setValues(0, maxMemory, (usedMemory - bufferMemory), bufferMemory);
		});
	}

}
