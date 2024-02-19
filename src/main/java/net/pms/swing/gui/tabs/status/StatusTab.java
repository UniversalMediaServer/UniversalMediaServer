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

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.gui.EConnectionState;
import net.pms.renderers.Renderer;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.AnimatedIcon;
import net.pms.swing.components.AnimatedIcon.AnimatedIconStage;
import net.pms.swing.components.AnimatedIcon.AnimatedIconType;
import net.pms.swing.components.CustomUIProgressBar;
import net.pms.swing.components.JAnimatedButton;
import net.pms.swing.components.SegmentedProgressBarUI;
import net.pms.swing.components.ServerBindMouseListener;
import net.pms.swing.components.WrapLayout;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.UmsFormBuilder;

public class StatusTab {

	private static final Color MEM_COLOR = new Color(119, 119, 119, 128);
	private static final Color DB_COLOR = new Color(75, 140, 181, 128);
	private static final Color BUF_COLOR = new Color(255, 128, 0, 128);
	private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");
	private static final String ICON_STATUS_CONNECTING = "icon-status-connecting." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_STATUS_CONNECTED = "icon-status-connected." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_STATUS_DISCONNECTED = "icon-status-disconnected." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_STATUS_WARNING = "icon-status-warning." + (SwingUtil.HDPI_AWARE ? "svg" : "png");

	private final JAnimatedButton connectionStatus = new JAnimatedButton();
	private final AnimatedIcon searchingIcon;
	private final AnimatedIcon connectedIcon;
	private final AnimatedIcon disconnectedIcon;
	private final AnimatedIcon blockedIcon;

	private JPanel renderersPanel;
	private JComponent detectedMediaRenderersSeparator;
	private JLabel mediaServerLabel;
	private JLabel mediaServerBindLabel;
	private JLabel interfaceServerBindLabel;
	private JLabel memLabel;
	private SegmentedProgressBarUI memBarUI;
	private JLabel currentBitrate;
	private JLabel peakBitrate;
	private JLabel bitrateLabel;
	private JLabel currentBitrateLabel;
	private JLabel peakBitrateLabel;
	private EConnectionState connectionState = EConnectionState.UNKNOWN;

	/**
	 * Shows a simple visual status of the server.
	 */
	public StatusTab() {
		// Build Animations
		searchingIcon = new AnimatedIcon(connectionStatus, ICON_STATUS_CONNECTING);

		connectedIcon = new AnimatedIcon(connectionStatus, ICON_STATUS_CONNECTED);

		disconnectedIcon = new AnimatedIcon(connectionStatus, ICON_STATUS_DISCONNECTED);

		blockedIcon = new AnimatedIcon(connectionStatus, ICON_STATUS_WARNING);
	}

	public void setConnectionState(EConnectionState connectionState) {
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
				default ->
					connectionStatus.setIcon(null);
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
				"p," + // Detected Media Renderers --------------------//  1
				"9dlu," + //                                              //
				"fill:p:grow," + //                 <renderers>                  //  3
				"3dlu," + //                                              //
				"p," + // ---------------------------------------------//  5
				"10dlu," + //           |                       |          //
				"[10pt,p]," + // Connected |  Memory Usage         |<bitrate> //  7
				"1dlu," + //           |                       |          //
				"[30pt,p]," + //  <icon>   |  <statusbar>          |          //  9
				"3dlu," //           |                       |          //
		//                   //////////////////////////////////////////////////
		);

		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DIALOG);
		builder.opaque(true);
		CellConstraints cc = new CellConstraints();

		// Renderers
		detectedMediaRenderersSeparator = builder.createBoldedSeparator(Messages.getString("DetectedMediaRenderers"));
		builder.add(detectedMediaRenderersSeparator).at(FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		Color fgColor = new Color(68, 68, 68);

		renderersPanel = new JPanel(new WrapLayout(FlowLayout.CENTER, 20, 10));
		JScrollPane rsp = new JScrollPane(
				renderersPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rsp.setBorder(BorderFactory.createEmptyBorder());
		rsp.setPreferredSize(new Dimension(0, 260));
		rsp.getHorizontalScrollBar().setLocation(0, 250);

		builder.add(rsp).at(cc.xyw(1, 3, 5));

		builder.addSeparator(null).at(FormLayoutUtil.flip(cc.xyw(1, 5, 5), colSpec, orientation));

		connectedIcon.start();
		searchingIcon.start();
		disconnectedIcon.start();
		connectionStatus.setFocusable(false);

		// Bitrate
		String conColSpec = "left:pref, 3dlu, right:pref:grow";
		UmsFormBuilder connectionBuilder = UmsFormBuilder.create().layout(new FormLayout(conColSpec, "p, 1dlu, p, 1dlu, p"));
		connectionBuilder.add(connectionStatus).at(FormLayoutUtil.flip(cc.xywh(1, 1, 1, 3, "center, fill"), conColSpec, orientation));
		// Set initial connection state
		setConnectionState(EConnectionState.SEARCHING);

		mediaServerLabel = new JLabel("<html><b>" + Messages.getString("Servers") + "</b></html>");
		mediaServerLabel.setForeground(fgColor);
		connectionBuilder.add(mediaServerLabel).at(FormLayoutUtil.flip(cc.xy(3, 1, "left, top"), conColSpec, orientation));
		mediaServerBindLabel = new JLabel("-");
		mediaServerBindLabel.setForeground(fgColor);
		mediaServerBindLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		mediaServerBindLabel.addMouseListener(new ServerBindMouseListener(mediaServerBindLabel));
		mediaServerBindLabel.setToolTipText(Messages.getString("MediaServerIpAddress"));
		connectionBuilder.add(mediaServerBindLabel).at(FormLayoutUtil.flip(cc.xy(3, 3, "left, top"), conColSpec, orientation));
		interfaceServerBindLabel = new JLabel("-");
		interfaceServerBindLabel.setForeground(fgColor);
		interfaceServerBindLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		interfaceServerBindLabel.addMouseListener(new ServerBindMouseListener(interfaceServerBindLabel));
		interfaceServerBindLabel.setToolTipText(Messages.getString("WebSettingsServerIpAddress"));
		connectionBuilder.add(interfaceServerBindLabel).at(FormLayoutUtil.flip(cc.xy(3, 5, "left, top"), conColSpec, orientation));
		builder.add(connectionBuilder.getPanel()).at(FormLayoutUtil.flip(cc.xywh(1, 7, 1, 3, "left, top"), colSpec, orientation));

		// Memory
		memBarUI = new SegmentedProgressBarUI(Color.white, Color.gray);
		memBarUI.setActiveLabel("{}", Color.white, 0);
		memBarUI.setActiveLabel("{}", Color.red, 90);
		memBarUI.addSegment("", MEM_COLOR);
		memBarUI.addSegment("", DB_COLOR);
		memBarUI.addSegment("", BUF_COLOR);
		memBarUI.setTickMarks(getTickMarks(), "{}");
		JProgressBar memoryProgressBar = new CustomUIProgressBar(0, 100, memBarUI);
		memoryProgressBar.setStringPainted(true);
		memoryProgressBar.setForeground(new Color(75, 140, 181));
		memoryProgressBar.setString(Messages.getString("Empty"));

		memLabel = new JLabel("<html><b>" + Messages.getString("MemoryUsage") + "</b> (" + Messages.getString("Mb") + ")</html>");
		memLabel.setForeground(fgColor);
		builder.add(memLabel).at(FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));
		builder.add(memoryProgressBar).at(FormLayoutUtil.flip(cc.xyw(3, 9, 1), colSpec, orientation));

		// Bitrate
		String bitColSpec = "left:pref, 3dlu, right:pref:grow";
		UmsFormBuilder bitrateBuilder = UmsFormBuilder.create().layout((new FormLayout(bitColSpec, "p, 1dlu, p, 1dlu, p")));

		bitrateLabel = new JLabel("<html><b>" + Messages.getString("Bitrate") + "</b> (" + Messages.getString("Mbs") + ")</html>");
		bitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(bitrateLabel).at(FormLayoutUtil.flip(cc.xy(1, 1), bitColSpec, orientation));

		currentBitrateLabel = new JLabel(Messages.getString("Current"));
		currentBitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(currentBitrateLabel).at(FormLayoutUtil.flip(cc.xy(1, 3), bitColSpec, orientation));

		currentBitrate = new JLabel("0");
		currentBitrate.setForeground(fgColor);
		bitrateBuilder.add(currentBitrate).at(FormLayoutUtil.flip(cc.xy(3, 3), bitColSpec, orientation));

		peakBitrateLabel = new JLabel(Messages.getString("Peak"));
		peakBitrateLabel.setForeground(fgColor);
		bitrateBuilder.add(peakBitrateLabel).at(FormLayoutUtil.flip(cc.xy(1, 5), bitColSpec, orientation));

		peakBitrate = new JLabel("0");
		peakBitrate.setForeground(fgColor);
		bitrateBuilder.add(peakBitrate).at(FormLayoutUtil.flip(cc.xy(3, 5), bitColSpec, orientation));

		builder.add(bitrateBuilder.getPanel()).at(FormLayoutUtil.flip(cc.xywh(5, 7, 1, 3, "left, top"), colSpec, orientation));

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
		SwingUtilities.invokeLater(() -> mediaServerBindLabel.setText(bind));
	}

	public void setInterfaceServerBind(String bind) {
		SwingUtilities.invokeLater(() -> interfaceServerBindLabel.setText(bind));
	}

	public void setCurrentBitrate(int sizeinMb) {
		currentBitrate.setText(FORMATTER.format(sizeinMb));
	}

	public void setPeakBitrate(int sizeinMb) {
		peakBitrate.setText(FORMATTER.format(sizeinMb));
	}

	public void addRenderer(final Renderer renderer) {
		final RendererPanel rendererPanel = new RendererPanel(renderer);
		rendererPanel.addTo(renderersPanel);
		renderer.addGuiListener(rendererPanel);
	}

	private int getTickMarks() {
		int mb = (int) (Runtime.getRuntime().maxMemory() / 1048576);
		return mb < 1000 ? 100 : mb < 2500 ? 250 : mb < 5000 ? 500 : 1000;
	}

	public void setMemoryUsage(int maxMemory, int usedMemory, int dbCacheMemory, int bufferMemory) {
		SwingUtilities.invokeLater(() -> memBarUI.setValues(0, maxMemory, Math.max(0, usedMemory - dbCacheMemory - bufferMemory), dbCacheMemory, bufferMemory));
	}

	public void applyLanguage() {
		if (detectedMediaRenderersSeparator != null &&
				detectedMediaRenderersSeparator.getComponentCount() > 0 &&
				detectedMediaRenderersSeparator.getComponent(0) instanceof JLabel jlabel) {
			jlabel.setText(Messages.getString("DetectedMediaRenderers"));
		}
		switch (connectionState) {
			case SEARCHING -> {
				connectionStatus.setToolTipText(Messages.getString("SearchingForRenderers"));
			}
			case CONNECTED -> {
				connectionStatus.setToolTipText(Messages.getString("Connected"));
			}
			case DISCONNECTED -> {
				connectionStatus.setToolTipText(Messages.getString("NoRenderersWereFound"));
			}
			case BLOCKED -> {
				connectionStatus.setToolTipText(Messages.getString("PortBlockedChangeIt"));
			}
		}
		mediaServerBindLabel.setToolTipText(Messages.getString("MediaServerIpAddress"));
		interfaceServerBindLabel.setToolTipText(Messages.getString("WebSettingsServerIpAddress"));
		mediaServerLabel.setText("<html><b>" + Messages.getString("Servers") + "</b></html>");
		memLabel.setText("<html><b>" + Messages.getString("MemoryUsage") + "</b> (" + Messages.getString("Mb") + ")</html>");
		bitrateLabel.setText("<html><b>" + Messages.getString("Bitrate") + "</b> (" + Messages.getString("Mbs") + ")</html>");
		currentBitrateLabel.setText(Messages.getString("Current"));
		peakBitrateLabel.setText(Messages.getString("Peak"));
	}

}
