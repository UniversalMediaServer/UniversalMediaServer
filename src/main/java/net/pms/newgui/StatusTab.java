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
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.FormLayoutUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusTab.class);

	private static final int MAX_RENDERERS = 10;
	private ImagePanel imagePanel;
	private PmsConfiguration configuration;
	private ImagePanel renderers[] = new ImagePanel[MAX_RENDERERS];
	private JLabel rendererLabels[] = new JLabel[MAX_RENDERERS];
	private int numRenderers;
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
			"p, 9dlu, p, 9dlu, p, 3dlu, p, 15dlu, p, 3dlu, 63dlu, 3dlu, p, 3dlu, p, 15dlu, p, 9dlu, p"
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
		jpb.setString(Messages.getString("StatusTab.5"));

		builder.addLabel(Messages.getString("StatusTab.6"), FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
		builder.add(jpb, FormLayoutUtil.flip(cc.xyw(1, 15, 2), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("StatusTab.9"), FormLayoutUtil.flip(cc.xyw(1, 17, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		FormLayout layoutRenderer = new FormLayout(
			"0:grow, pref, pref, pref, pref, pref, pref, pref, pref, pref, pref, 0:grow",
			"pref, 3dlu, pref"
		);
		PanelBuilder rendererBuilder = new PanelBuilder(layoutRenderer);
		rendererBuilder.opaque(true);
		for (int i = 0; i < MAX_RENDERERS; i++) {
			renderers[i] = buildImagePanel(null);
			rendererBuilder.add(renderers[i], cc.xy(3 + i, 1));
			rendererLabels[i] = new JLabel("");
			rendererBuilder.add(rendererLabels[i], cc.xy(3 + i, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
		}

		builder.add(rendererBuilder.getPanel(), cc.xyw(1, 19, 5));

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

	public void addRendererIcon(int code, String msg, String icon) {
		BufferedImage bi = null;

		if (icon != null) {
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

		if (bi != null) {
			renderers[numRenderers].set(bi);
		}

        if (msg.contains("\n")) {
            msg = "<html>" + msg.replaceAll("\n", "<br>") + "</html>";
        }
		rendererLabels[numRenderers].setText(msg);
		numRenderers++;
	}
}
