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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.BasicPlayer;
import net.pms.util.FormLayoutUtil;
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
		public GuiUtil.FixedPanel playing;
		public JLabel time;
		public JFrame frame;
		public GuiUtil.SmoothProgressBar jpb;
		public RendererPanel panel;
		public String name = " ";

		public RendererItem(RendererConfiguration r) {
			icon = addRendererIcon(r.getRendererIcon());
			icon.enableRollover();
			label = new JLabel(r.getRendererName());
			int w = icon.getSource().getWidth() - 20;
			playingLabel = new GuiUtil.MarqueeLabel(" ", w);
			playingLabel.setForeground(Color.gray);
			int h = (int) playingLabel.getSize().getHeight();
			playing = new GuiUtil.FixedPanel(w, h);
			playing.add(playingLabel);
			time = new JLabel(" ");
			time.setForeground(Color.gray);
			jpb = new GuiUtil.SmoothProgressBar(0, 100);
			jpb.setUI(new GuiUtil.SimpleProgressUI(Color.gray, Color.gray));
			jpb.setStringPainted(true);
			jpb.setBorderPainted(false);
			jpb.setString(r.getAddress().getHostAddress());
			jpb.setForeground(bufColor);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			BasicPlayer.State state = ((BasicPlayer) e.getSource()).getState();
			time.setText(state.playback == BasicPlayer.STOPPED ? " " :
				UMSUtils.playedDurationStr(state.position, state.duration));
			jpb.setValue((int) (100 * state.buffer / bufferSize));
			String n = (state.playback == BasicPlayer.STOPPED || StringUtils.isBlank(state.name)) ? " " : state.name;
			if (!name.equals(n)) {
				name = n;
				playingLabel.setText(name);
			}
		}
	}

	private PanelBuilder rendererBuilder;
	private FormLayout layoutRenderer;
	private ImagePanel imagePanel;
	private PmsConfiguration configuration;
	private int rendererCount;
	private JLabel jl;
	private JProgressBar jpb;
	private GuiUtil.SegmentedProgressBarUI memBarUI;
	private JLabel currentBitrate;
	private JLabel currentBitrateLabel;
	private JLabel peakBitrate;
	private JLabel peakBitrateLabel;
	private long rc = 0;
	private long peak;
	private static DecimalFormat formatter = new DecimalFormat("#,###");
	private static int bufferSize;

	StatusTab(PmsConfiguration configuration) {
		this.configuration = configuration;
		rendererCount = 0;
		bufferSize = configuration.getMaxMemoryBufferSize();
	}

	public JProgressBar getJpb() {
		return jpb;
	}

	public void updateCurrentBitrate() {
		long total = 0;
		for (RendererConfiguration r : PMS.get().getRenders()) {
			total += r.getBuffer();
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
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 320dlu, 30dlu, pref, 0:grow", orientation);

		FormLayout layout = new FormLayout(
			colSpec,
			"p, 9dlu, p, 9dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 15dlu, p, 15dlu, p, 15dlu, p, 15dlu, p, p"
		);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DIALOG);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("StatusTab.2"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		jl = new JLabel(Messages.getString("StatusTab.3"));
		builder.add(jl, FormLayoutUtil.flip(cc.xy(1, 3,  "center, top"), colSpec, orientation));
		jl.setFont(new Font("Dialog", 1, 18));
		jl.setForeground(new Color(68, 68, 68));

		imagePanel = buildImagePanel("/resources/images/icon-status-connecting.png");
		builder.add(imagePanel, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JSeparator x = new JSeparator(SwingConstants.VERTICAL);
		x.setPreferredSize(new Dimension(3, 215));
		builder.add(x, FormLayoutUtil.flip(cc.xywh(3, 3, 1, 10, "center, top"), colSpec, orientation));

		currentBitrateLabel = new JLabel(Messages.getString("StatusTab.8") + " (" + Messages.getString("StatusTab.11") + ")");
		builder.add(currentBitrateLabel, FormLayoutUtil.flip(cc.xyw(4, 3, 2, "left, top"), colSpec, orientation));

		currentBitrate = new JLabel("0");
		currentBitrate.setFont(new Font("Dialog", 1, 50));
		currentBitrate.setForeground(new Color(68, 68, 68));
		builder.add(currentBitrate, FormLayoutUtil.flip(cc.xyw(4, 5, 2, "left, top"), colSpec, orientation));

		peakBitrateLabel = new JLabel(Messages.getString("StatusTab.10") + " (" + Messages.getString("StatusTab.11") + ")");
		builder.add(peakBitrateLabel, FormLayoutUtil.flip(cc.xyw(4, 7, 2, "left, top"), colSpec, orientation));

		peakBitrate = new JLabel("0");
		peakBitrate.setFont(new Font("Dialog", 1, 50));
		peakBitrate.setForeground(new Color(68, 68, 68));
		builder.add(peakBitrate, FormLayoutUtil.flip(cc.xyw(4, 9, 2, "left, top"), colSpec, orientation));

		jpb = new JProgressBar(0, 100);
		jpb.setStringPainted(true);
		jpb.setString(Messages.getString("StatusTab.5"));
		memBarUI = new GuiUtil.SegmentedProgressBarUI(Color.white, Color.gray);
		memBarUI.addSegment("", memColor);
		memBarUI.addSegment("", bufColor);
		memBarUI.setTickMarks(100, "{}" + Messages.getString("StatusTab.12"));
		jpb.setUI(memBarUI);

		builder.addLabel(Messages.getString("StatusTab.6"), FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
		builder.add(jpb, FormLayoutUtil.flip(cc.xyw(1, 7, 2), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("StatusTab.9"), FormLayoutUtil.flip(cc.xyw(1, 11, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

//		FormLayout layoutRenderer = new FormLayout(
//			"0:grow, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, 0:grow",
//			"pref, 3dlu, pref"
//		);
		layoutRenderer = new FormLayout(
			"pref",
			"pref, 3dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"
		);
		rendererBuilder = new PanelBuilder(layoutRenderer);
		rendererBuilder.opaque(true);

		JScrollPane rsp = new JScrollPane(
			rendererBuilder.getPanel(),
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//			//	JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		rsp.setBorder(BorderFactory.createEmptyBorder());
		rsp.setPreferredSize(new Dimension(0, 260));
		rsp.getHorizontalScrollBar().setLocation(0,250);

		builder.add(rsp, cc.xyw(1, 13, 5));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			//JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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

		layoutRenderer.appendColumn(ColumnSpec.decode("center:pref"));

		final RendererItem r = new RendererItem(renderer);
		CellConstraints cc = new CellConstraints();
		int i = rendererCount++;
		rendererBuilder.add(r.icon, cc.xy(i + 2, 1));
		rendererBuilder.add(r.label, cc.xy(i + 2, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
		rendererBuilder.add(r.jpb, cc.xy(i + 2, 5));
		rendererBuilder.add(r.playing, cc.xy(i + 2, 7, CellConstraints.CENTER, CellConstraints.DEFAULT));
		rendererBuilder.add(r.time, cc.xy(i + 2, 9));

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
							r.frame = new JFrame(renderer.getRendererName() + (renderer.isOffline() ? "  [offline]" : ""));
							r.panel = new RendererPanel(renderer);
							r.frame.add(r.panel);
							r.frame.setResizable(false);
							r.frame.setIconImage(((JFrame) PMS.get().getFrame()).getIconImage());
							r.frame.pack();
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
				renderer.gui.icon.set(getRendererIcon(renderer.getRendererIcon()));
				renderer.gui.label.setText(renderer.getRendererName());
				// Update the popup panel if it's been opened
				if (renderer.gui.panel != null) {
					renderer.gui.panel.update();
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
					return ImageIO.read(new URL(icon));
				} catch (Exception e) {
					LOGGER.debug("Failed to read icon url: " + e);
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

				if (is != null) {
					bi = ImageIO.read(is);
				}
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
		return bi;
	}

	public void updateMemoryUsage() {
		long max = Runtime.getRuntime().maxMemory() / 1048576;
		long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
		long buf = 0;
		for (RendererConfiguration r : PMS.get().getRenders()) {
			buf += (r.getBuffer());
		}
		memBarUI.setValues(0, (int) max, (int) (used - buf), (int) buf);
	}

	private void startMemoryUpdater() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				for(;;) {
					updateMemoryUsage();
					try {
						Thread.sleep(2000);
					} catch (Exception e) {
						return;
					}
				}
			}
		};
		new Thread(r).start();
	}
}
