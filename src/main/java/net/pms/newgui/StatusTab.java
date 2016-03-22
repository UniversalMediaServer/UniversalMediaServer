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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.BasicPlayer;
import net.pms.util.FormLayoutUtil;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusTab.class);
	private static final Color memColor = new Color(119, 119, 119, 128);
	private static final Color bufColor = new Color(75, 140, 181, 128);

	public static class RendererItem implements ActionListener {
		public ImagePanel icon;
		public JLabel label;
		public GuiUtil.MarqueeLabel playingLabel;
//		public GuiUtil.ScrollLabel playingLabel;
		public GuiUtil.FixedPanel playing;
		public JLabel time;
		public JFrame frame;
		public GuiUtil.SmoothProgressBar rendererProgressBar;
		public RendererPanel panel;
		public String name = " ";
		private JPanel _panel = null;

		public RendererItem(RendererConfiguration r) {
			icon = addRendererIcon(r.getRendererIcon());
			icon.enableRollover();
			label = new JLabel(r.getRendererName());
			playingLabel = new GuiUtil.MarqueeLabel(" ");
//			playingLabel = new GuiUtil.ScrollLabel(" ");
			playingLabel.setForeground(Color.gray);
			int h = (int) playingLabel.getSize().getHeight();
			playing = new GuiUtil.FixedPanel(0, h);
			playing.add(playingLabel);
			time = new JLabel(" ");
			time.setForeground(Color.gray);
			rendererProgressBar = new GuiUtil.SmoothProgressBar(0, 100, new GuiUtil.SimpleProgressUI(Color.gray, Color.gray));
			rendererProgressBar.setStringPainted(true);
			rendererProgressBar.setBorderPainted(false);
			rendererProgressBar.setString(r.getAddress().getHostAddress());
			rendererProgressBar.setForeground(bufColor);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			BasicPlayer.State state = ((BasicPlayer) e.getSource()).getState();
			time.setText((state.playback == BasicPlayer.STOPPED || StringUtil.isZeroTime(state.position)) ? " " :
				UMSUtils.playedDurationStr(state.position, state.duration));
			rendererProgressBar.setValue((int) (100 * state.buffer / bufferSize));
			String n = (state.playback == BasicPlayer.STOPPED || StringUtils.isBlank(state.name)) ? " " : state.name;
			if (!name.equals(n)) {
				name = n;
				playingLabel.setText(name);
			}
		}

		public void addTo(Container parent) {
			parent.add(getPanel());
			parent.validate();
			// Maximize the playing label width
			int w = _panel.getWidth() - _panel.getInsets().left - _panel.getInsets().right;
			playing.setSize(w, (int) playingLabel.getSize().getHeight());
			playingLabel.setMaxWidth(w);
		}

		public void delete() {
			try {
				// Delete the popup if open
				if (frame != null) {
					frame.dispose();
					frame = null;
				}
				Container parent = _panel.getParent();
				parent.remove(_panel);
				parent.revalidate();
				parent.repaint();
			} catch (Exception e) {
			}
		}

		public JPanel getPanel() {
			if (_panel == null) {
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
				_panel = b.getPanel();
			}
			return _panel;
		}
	}

	private ImagePanel imagePanel;
	private JPanel renderers;
	private JLabel jl;
	private JProgressBar memoryProgressBar;
	private GuiUtil.SegmentedProgressBarUI memBarUI;
	private JLabel bitrateLabel;
	private JLabel currentBitrate;
	private JLabel currentBitrateLabel;
	private JLabel peakBitrate;
	private JLabel peakBitrateLabel;
	private long rc = 0;
	private long peak;
	private static DecimalFormat formatter = new DecimalFormat("#,###");
	private static int bufferSize;

	StatusTab(PmsConfiguration configuration) {
		bufferSize = configuration.getMaxMemoryBufferSize();
	}

	public void updateCurrentBitrate() {
		long total = 0;
		List<RendererConfiguration> foundRenderers = PMS.get().getFoundRenderers();
		synchronized(foundRenderers) {
			for (RendererConfiguration r : foundRenderers) {
				total += r.getBuffer();
			}
		}
		if (total == 0) {
			currentBitrate.setText("0");
		}
	}

	public JLabel getJl() {
		return jl;
	}

	public ImagePanel getImagePanel() {
		return imagePanel;
	}

	public JComponent build() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());

		String colSpec = FormLayoutUtil.getColSpec("pref, 30dlu, fill:pref:grow, 30dlu, pref", orientation);
		//                                             1     2          3           4     5

		FormLayout layout = new FormLayout(colSpec,
			//                          1     2          3            4     5
			//                   //////////////////////////////////////////////////
			  "p,"               // Detected Media Renderers --------------------//  1
			+ "9dlu,"            //                                              //
			+ "fill:p:grow,"     //                 <renderers>                  //  3
			+ "3dlu,"            //                                              //
			+ "p,"               // ---------------------------------------------//  5
			+ "3dlu,"            //           |                       |          //
			+ "p,"               // Connected |  Memory Usage         |<bitrate> //  7
			+ "3dlu,"            //           |                       |          //
			+ "p,"               //  <icon>   |  <statusbar>          |          //  9
			                     //////////////////////////////////////////////////
		);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DIALOG);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		// Renderers
		JComponent cmp = builder.addSeparator(Messages.getString("StatusTab.9"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		Font bold = cmp.getFont().deriveFont(Font.BOLD);
		Color fgColor = new Color(68, 68, 68);
		cmp.setFont(bold);

		renderers = new JPanel(new GuiUtil.WrapLayout(FlowLayout.CENTER, 20, 10));
		JScrollPane rsp = new JScrollPane(
			renderers,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		rsp.setBorder(BorderFactory.createEmptyBorder());
		rsp.setPreferredSize(new Dimension(0, 260));
		rsp.getHorizontalScrollBar().setLocation(0,250);

		builder.add(rsp, cc.xyw(1, 3, 5));

		cmp = builder.addSeparator(null, FormLayoutUtil.flip(cc.xyw(1, 5, 5), colSpec, orientation));

		// Connected
		jl = new JLabel(Messages.getString("StatusTab.3"));
		builder.add(jl, FormLayoutUtil.flip(cc.xy(1, 7,  "center, top"), colSpec, orientation));
		jl.setFont(bold);
		jl.setForeground(fgColor);

		imagePanel = buildImagePanel("/resources/images/icon-status-connecting.png");
		builder.add(imagePanel, FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));

		// Memory
		memBarUI = new GuiUtil.SegmentedProgressBarUI(Color.white, Color.gray);
		memBarUI.setActiveLabel("{}", Color.white, 0);
		memBarUI.setActiveLabel("{}", Color.red, 90);
		memBarUI.addSegment("", memColor);
		memBarUI.addSegment("", bufColor);
		memBarUI.setTickMarks(getTickMarks(), "{}");
		memoryProgressBar = new GuiUtil.CustomUIProgressBar(0, 100, memBarUI);
		memoryProgressBar.setStringPainted(true);
		memoryProgressBar.setForeground(new Color(75, 140, 181));
		memoryProgressBar.setString(Messages.getString("StatusTab.5"));

		JLabel mem = builder.addLabel("<html><b>" + Messages.getString("StatusTab.6") + "</b> (" + Messages.getString("StatusTab.12") + ")</html>", FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
		mem.setForeground(fgColor);
		builder.add(memoryProgressBar, FormLayoutUtil.flip(cc.xyw(3, 9, 1), colSpec, orientation));

		// Bitrate
		String bitColSpec = "left:pref, 3dlu, right:pref:grow";
		PanelBuilder bitrateBuilder = new PanelBuilder(new FormLayout(bitColSpec, "p, 1dlu, p, 1dlu, p"));

		bitrateLabel = new JLabel("<html><b>" + Messages.getString("StatusTab.13") + "</b> (" + Messages.getString("StatusTab.11") + ")</html>");
		bitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(bitrateLabel, FormLayoutUtil.flip(cc.xy(1, 1), bitColSpec, orientation));

		currentBitrateLabel = new JLabel(Messages.getString("StatusTab.14"));
		currentBitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(currentBitrateLabel, FormLayoutUtil.flip(cc.xy(1, 3), bitColSpec, orientation));

		currentBitrate = new JLabel("0");
		currentBitrate.setForeground(fgColor);
		bitrateBuilder.add(currentBitrate, FormLayoutUtil.flip(cc.xy(3, 3), bitColSpec, orientation));

		peakBitrateLabel = new JLabel(Messages.getString("StatusTab.15"));
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
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		startMemoryUpdater();
		return scrollPane;
	}

	public void setReadValue(long v, String msg) {
		if (v < rc) {
			rc = v;
		} else {
			int sizeinMb = (int) ((v - rc) / 125) / 1024;

			if (sizeinMb > peak) {
				peak = sizeinMb;
			}

			currentBitrate.setText(formatter.format(sizeinMb));
			peakBitrate.setText(formatter.format(peak));
			rc = v;
		}
	}

	public ImagePanel buildImagePanel(String url) {
		BufferedImage bi = null;

		if (url != null) {
			try {
				bi = ImageIO.read(LooksFrame.class.getResourceAsStream(url));
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}

		return new ImagePanel(bi);
	}

	public void addRenderer(final RendererConfiguration renderer) {
		final RendererItem r = new RendererItem(renderer);
		r.addTo(renderers);
		renderer.setGuiComponents(r);
		r.icon.setAction(new AbstractAction() {
			private static final long serialVersionUID = -6316055325551243347L;

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (r.frame == null) {
							JFrame top = (JFrame) SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame());
							// We're using JFrame instead of JDialog here so as to have a minimize button. Since the player panel
							// is intrinsically a freestanding module this approach seems valid to me but some frown on it: see
							// http://stackoverflow.com/questions/9554636/the-use-of-multiple-jframes-good-bad-practice
							r.frame = new JFrame();
							r.panel = new RendererPanel(renderer);
							r.frame.add(r.panel);
							r.panel.update();
							r.frame.setResizable(false);
							r.frame.setIconImage(((JFrame) PMS.get().getFrame()).getIconImage());
							r.frame.setLocationRelativeTo(top);
							r.frame.setVisible(true);
						} else {
							r.frame.setVisible(true);
							r.frame.toFront();
						}
					}
				});
			}
		});
	}

	public static void updateRenderer(final RendererConfiguration renderer) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (renderer.gui != null) {
					renderer.gui.icon.set(getRendererIcon(renderer.getRendererIcon()));
					renderer.gui.label.setText(renderer.getRendererName());
					// Update the popup panel if it's been opened
					if (renderer.gui.panel != null) {
						renderer.gui.panel.update();
					}
				}
			}
		});
	}

	public static ImagePanel addRendererIcon(String icon) {
		BufferedImage bi = getRendererIcon(icon);
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

				File f = new File(icon);

				if (!f.isAbsolute() && f.getParent() == null) { // filename
					f = new File("renderers", icon);
				}

				if (f.isFile()) {
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

	private int getTickMarks() {
		int mb = (int) (Runtime.getRuntime().maxMemory() / 1048576);
		return mb < 1000 ? 100 : mb < 2500 ? 250 : mb < 5000 ? 500 : 1000;
	}

	public void updateMemoryUsage() {
		final long max = Runtime.getRuntime().maxMemory() / 1048576;
		final long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
		long buf = 0;
		List<RendererConfiguration> foundRenderers = PMS.get().getFoundRenderers();
		synchronized (foundRenderers) {
			for (RendererConfiguration r : PMS.get().getFoundRenderers()) {
				buf += (r.getBuffer());
			}
		}
		final long buffer = buf;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				memBarUI.setValues(0, (int) max, (int) (used - buffer), (int) buffer);
			}
		});
	}

	private void startMemoryUpdater() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				for(;;) {
					updateMemoryUsage();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		new Thread(r).start();
	}
}
