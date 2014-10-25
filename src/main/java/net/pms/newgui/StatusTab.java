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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DateFormatter;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.util.BasicPlayer;
import net.pms.util.FormLayoutUtil;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusTab.class);

	public static class RendererItem {
		public ImagePanel icon;
		public JLabel label;
		public JLabel ip;
		public JLabel playing;
		public JLabel time;
		public JFrame frame;
		public RendererPanel panel;
		public Thread thread;
	}

	private PanelBuilder rendererBuilder;
	private FormLayout layoutRenderer;
	private ImagePanel imagePanel;
	private PmsConfiguration configuration;
	private int rendererCount;
	private JLabel jl;
	private JProgressBar jpb;
	private JLabel currentBitrate;
	private JLabel currentBitrateLabel;
	private JLabel peakBitrate;
	private JLabel peakBitrateLabel;
	private long rc = 0;
	private long peak;
	private DecimalFormat formatter = new DecimalFormat("#,###");

	StatusTab(PmsConfiguration configuration) {
		this.configuration = configuration;
		rendererCount = 0;
	}

	public JProgressBar getJpb() {
		return jpb;
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
			"p, 9dlu, p, 9dlu, p, 3dlu, p, 15dlu, p, 3dlu, 63dlu, 3dlu, p, 3dlu, p, 15dlu, p"//, 9dlu, p"
		);

		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DIALOG);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("StatusTab.2"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		jl = new JLabel(Messages.getString("StatusTab.3"));
		builder.add(jl, FormLayoutUtil.flip(cc.xyw(1, 3, 2, "center, top"), colSpec, orientation));
		jl.setFont(new Font("Dialog", 1, 18));
		jl.setForeground(new Color(68, 68, 68));

		imagePanel = buildImagePanel("/resources/images/icon-status-connecting.png");
		builder.add(imagePanel, FormLayoutUtil.flip(cc.xywh(1, 5, 2, 8, "center, fill"), colSpec, orientation));

		JSeparator x = new JSeparator(SwingConstants.VERTICAL);
		x.setPreferredSize(new Dimension(3, 215));
		builder.add(x, FormLayoutUtil.flip(cc.xywh(3, 4, 1, 12, "center, top"), colSpec, orientation));

		currentBitrateLabel = new JLabel(Messages.getString("StatusTab.8") + " (" + Messages.getString("StatusTab.11") + ")");
		builder.add(currentBitrateLabel, FormLayoutUtil.flip(cc.xyw(4, 5, 2, "left, top"), colSpec, orientation));

		currentBitrate = new JLabel("0");
		currentBitrate.setFont(new Font("Dialog", 1, 50));
		currentBitrate.setForeground(new Color(68, 68, 68));
		builder.add(currentBitrate, FormLayoutUtil.flip(cc.xyw(4, 7, 2, "left, top"), colSpec, orientation));

		peakBitrateLabel = new JLabel(Messages.getString("StatusTab.10") + " (" + Messages.getString("StatusTab.11") + ")");
		builder.add(peakBitrateLabel, FormLayoutUtil.flip(cc.xyw(4, 9, 2, "left, top"), colSpec, orientation));

		peakBitrate = new JLabel("0");
		peakBitrate.setFont(new Font("Dialog", 1, 50));
		peakBitrate.setForeground(new Color(68, 68, 68));
		builder.add(peakBitrate, FormLayoutUtil.flip(cc.xyw(4, 11, 2, "left, top"), colSpec, orientation));

		jpb = new JProgressBar(0, 100);
		jpb.setStringPainted(true);
		jpb.setForeground(new Color(75, 140, 181));
		jpb.setString(Messages.getString("StatusTab.5"));

		builder.addLabel(Messages.getString("StatusTab.6"), FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
		builder.add(jpb, FormLayoutUtil.flip(cc.xyw(1, 13, 2), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("StatusTab.9"), FormLayoutUtil.flip(cc.xyw(1, 15, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

//		FormLayout layoutRenderer = new FormLayout(
//			"0:grow, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, 0:grow",
//			"pref, 3dlu, pref"
//		);
		layoutRenderer = new FormLayout(
			"pref",
			"pref, 3dlu, pref, 2dlu,pref, 2dlu,pref, 2dlu, pref"
		);
		rendererBuilder = new PanelBuilder(layoutRenderer);
		rendererBuilder.opaque(true);

		JScrollPane rsp = new JScrollPane(
			rendererBuilder.getPanel(),
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rsp.setBorder(BorderFactory.createEmptyBorder());
		rsp.setPreferredSize(new Dimension(0, 220));

		builder.add(rsp, cc.xyw(1, 17, 5));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
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

		final RendererItem r = new RendererItem();
		r.icon = addRendererIcon(renderer.getRendererIcon());
		r.icon.enableRollover();
		CellConstraints cc = new CellConstraints();
		int i = rendererCount++;
		rendererBuilder.add(r.icon, cc.xy(i + 2, 1));
		r.label = new JLabel(renderer.getRendererName());
		rendererBuilder.add(r.label, cc.xy(i + 2, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
		r.ip = new JLabel("");
		rendererBuilder.add(r.ip, cc.xy(i + 2, 5));
		r.playing = new JLabel("");
		rendererBuilder.add(r.playing, cc.xy(i + 2, 7));
		r.time = new JLabel("");
		rendererBuilder.add(r.time, cc.xy(i + 2, 9));

		renderer.setGuiComponents(r);
		updateIP(renderer);
		r.icon.setAction(new AbstractAction() {
			private static final long serialVersionUID = -6316055325551243347L;

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
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
			public void run() {
				renderer.gui.icon.set(getRendererIcon(renderer.getRendererIcon()));
				renderer.gui.label.setText(renderer.getRendererName());
				updateIP(renderer);
				// Update the popup panel if it's been opened
				if (renderer.gui.panel != null) {
					renderer.gui.panel.update();
				}
			}
		});
	}

	private static void clearRenderGui(RendererConfiguration r) {
		r.gui.ip.setText("");
		r.gui.playing.setText("");
		r.gui.time.setText("");
	}

	private static Thread launchThread(final RendererConfiguration render) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				while(render.getPlayingRes() != null) {
					DLNAResource res = render.getPlayingRes();
					long elapsed = System.currentTimeMillis() - res.getStartTime();
					String dur = "????";
					if (res.getMedia() != null) {
						dur = StringUtil.shortTime(res.getMedia().getDurationString(), 4);
					}
					String estr = StringUtil.shortTime(DurationFormatUtils.formatDuration(elapsed, "HH:mm:ss"), 2);
					render.gui.time.setText(UMSUtils.playedDurationStr(estr, dur));
					try {
						Thread.sleep(5000);
					} catch (Exception e) {
					}
				}
				render.gui.thread = null;
			}
		};
		return new Thread(r);
	}

	private static void updateIP(RendererConfiguration renderer) {
		clearRenderGui(renderer);
		InetAddress ip = renderer.getAddress();
		if(ip != null) {
			renderer.gui.ip.setText("<html><font color=gray>" + ip.getHostAddress() + "</font></html>");
		}
		DLNAResource res = renderer.getPlayingRes();
		if(res != null) {
			String title = res.getDisplayName();
			renderer.gui.playing.setText(title.substring(0, title.length() < 25 ? title.length() : 25));
			if(renderer.isUpnpControllable()) {
				renderer.getPlayer().connect(renderer);
			}
			else {
				if(renderer.gui.thread == null) {
					renderer.gui.thread = launchThread(renderer);
					renderer.gui.thread.start();
				}
			}

		}
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
}
