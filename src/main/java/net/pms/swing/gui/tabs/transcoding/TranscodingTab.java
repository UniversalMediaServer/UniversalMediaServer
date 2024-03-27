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
package net.pms.swing.gui.tabs.transcoding;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.CustomJButton;
import net.pms.swing.components.CustomJSpinner;
import net.pms.swing.components.CustomTabbedPaneUI;
import net.pms.swing.components.JImageButton;
import net.pms.swing.components.KeyedComboBoxModel;
import net.pms.swing.components.KeyedStringComboBoxModel;
import net.pms.swing.components.RestrictedFileSystemView;
import net.pms.swing.components.SpinnerIntModel;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.JavaGui;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.swing.gui.ViewLevel;
import net.pms.util.SubtitleColor;
import net.pms.util.SubtitleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranscodingTab {

	private static final Logger LOGGER = LoggerFactory.getLogger(TranscodingTab.class);
	private static final String COMMON_COL_SPEC = "left:pref, 3dlu, pref:grow";
	private static final String COMMON_ROW_SPEC = "4*(pref, 3dlu), pref, 10dlu, pref, 10dlu:grow, pref";
	private static final String EMPTY_COL_SPEC = "left:pref, 3dlu, pref:grow";
	private static final String EMPTY_ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p , 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 20dlu, p, 3dlu, p, 3dlu, p";
	private static final String LEFT_COL_SPEC = "left:pref, pref, pref, pref, 0:grow";
	private static final String LEFT_ROW_SPEC = "fill:10:grow, 3dlu, p, 3dlu, p, 3dlu, p";
	private static final String MAIN_COL_SPEC = "left:pref, pref, 7dlu, pref, pref, fill:10:grow";
	private static final String MAIN_ROW_SPEC = "fill:10:grow";
	private static final String EMPTY_PANEL = "empty_panel";

	/*
	 * 16 cores is the maximum allowed by MEncoder as of MPlayer r34863.
	 * Revisions before that allowed only 8.
	 */
	private static final int MAX_CORES = 16;

	private static final String BUTTON_ARROW_DOWN = "button-arrow-down." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_ARROW_UP = "button-arrow-up." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_TOOGLE_OFF = "button-toggle-off." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_TOOGLE_ON_DISABLED = "button-toggle-on_disabled." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String BUTTON_TOOGLE_ON = "button-toggle-on." + (SwingUtil.HDPI_AWARE ? "svg" : "png");

	private final UmsConfiguration configuration;
	private final ComponentOrientation orientation;
	private final JavaGui looksFrame;

	private JCheckBox disableSubs;
	private JTextField forcetranscode;
	private JTextField notranscode;
	private JTextField maxbuffer;
	private DefaultMutableTreeNode[] parent;
	private JPanel tabbedPanel;
	private CardLayout cardLayout;
	private JTextField abitrate;
	private JTree tree;
	private JComboBox<String> vq;
	private JComboBox<String> x264Quality;
	private JCheckBox videoHWacceleration;
	private JTextField langs;
	private JTextField defaultsubs;
	private JTextField forcedsub;
	private JTextField forcedtags;
	private JTextField alternateSubFolder;
	private JTextField defaultaudiosubs;
	private JTextField defaultfont;
	private JTextField assScale;
	private JImageButton arrowDownButton;
	private JImageButton arrowUpButton;
	private JImageButton toggleButton;

	public TranscodingTab(UmsConfiguration configuration, JavaGui looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
		// Apply the orientation for the locale
		orientation = ComponentOrientation.getOrientation(PMS.getLocale());
	}

	private enum ToggleButtonState {
		UNKNOWN(BUTTON_TOOGLE_ON_DISABLED),
		ON(BUTTON_TOOGLE_ON),
		OFF(BUTTON_TOOGLE_OFF);

		private final String iconName;
		private ToggleButtonState(String name) {
			iconName = name;
		}

		public String getIconName() {
			return iconName;
		}
	}

	public JComponent build() {
		String colSpec = FormLayoutUtil.getColSpec(MAIN_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, MAIN_ROW_SPEC);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		if (!configuration.isHideAdvancedOptions()) {
			builder.add(buildRightTabbedPanel()).at(FormLayoutUtil.flip(cc.xyw(4, 1, 3), colSpec, orientation));
			builder.add(buildLeft()).at(FormLayoutUtil.flip(cc.xy(2, 1), colSpec, orientation));
		} else {
			builder.add(buildRightTabbedPanel()).at(FormLayoutUtil.flip(cc.xyw(2, 1, 5), colSpec, orientation));
			builder.add(buildLeft()).at(FormLayoutUtil.flip(cc.xy(2, 1), colSpec, orientation));
		}

		addEngines();

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	private JComponent buildRightTabbedPanel() {
		cardLayout = new CardLayout();
		tabbedPanel = new JPanel(cardLayout);
		return tabbedPanel;
	}

	private void setButtonsState() {
		TreePath path = null;
		if (tree != null) {
			path = tree.getSelectionModel().getSelectionPath();
		}
		if (
			path == null ||
			!(path.getLastPathComponent() instanceof TreeNodeSettings) ||
			((TreeNodeSettings) path.getLastPathComponent()).getPlayer() == null
		) {
			arrowDownButton.setEnabled(false);
			arrowUpButton.setEnabled(false);
			toggleButton.setIconName(ToggleButtonState.UNKNOWN.getIconName());
			toggleButton.setEnabled(false);
		} else {
			TreeNodeSettings node = (TreeNodeSettings) path.getLastPathComponent();
			DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
			int index = treeModel.getIndexOfChild(node.getParent(), node);
			arrowUpButton.setEnabled(index != 0);
			arrowDownButton.setEnabled(index != node.getParent().getChildCount() - 1);
			Engine player = node.getPlayer();
			if (player.isEnabled()) {
				toggleButton.setIconName(ToggleButtonState.ON.getIconName());
				toggleButton.setEnabled(true);
			} else {
				toggleButton.setIconName(ToggleButtonState.OFF.getIconName());
				toggleButton.setEnabled(player.isAvailable());
			}
		}
	}

	public JComponent buildLeft() {
		String colSpec = FormLayoutUtil.getColSpec(LEFT_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, LEFT_ROW_SPEC);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		arrowDownButton = new JImageButton(BUTTON_ARROW_DOWN);
		arrowDownButton.setToolTipText(Messages.getGuiString("ChangePositionSelectedEngine"));
		arrowDownButton.addActionListener((ActionEvent e) -> {
			TreePath path = tree.getSelectionModel().getSelectionPath();
			if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
				TreeNodeSettings node = (TreeNodeSettings) path.getLastPathComponent();
				if (node.getPlayer() != null) {
					DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();   // get the tree model
					//now get the index of the selected node in the DefaultTreeModel
					int index = treeModel.getIndexOfChild(node.getParent(), node);
					// if selected node is last, return (can't move down)
					if (index < node.getParent().getChildCount() - 1) {
						treeModel.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index + 1);   // move the node
						treeModel.reload();
						for (int i = 0; i < tree.getRowCount(); i++) {
							tree.expandRow(i);
						}
						tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
						((TreeNodeSettings) treeModel.getChild(node.getParent(), index)).getPlayer();
						configuration.setEnginePriorityBelow(node.getPlayer(), ((TreeNodeSettings) treeModel.getChild(node.getParent(), index)).getPlayer());
					}
				}
			}
		});
		builder.add(arrowDownButton).at(FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		arrowUpButton = new JImageButton(BUTTON_ARROW_UP);
		arrowUpButton.setToolTipText(Messages.getGuiString("ChangePositionSelectedEngine"));
		arrowUpButton.addActionListener((ActionEvent e) -> {
			TreePath path = tree.getSelectionModel().getSelectionPath();
			if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
				TreeNodeSettings node = (TreeNodeSettings) path.getLastPathComponent();
				if (node.getPlayer() != null) {
					DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();   // get the tree model
					//now get the index of the selected node in the DefaultTreeModel
					int index = treeModel.getIndexOfChild(node.getParent(), node);
					// if selected node is first, return (can't move up)
					if (index != 0) {
						treeModel.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index - 1);   // move the node
						treeModel.reload();
						for (int i = 0; i < tree.getRowCount(); i++) {
							tree.expandRow(i);
						}
						tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
						configuration.setEnginePriorityAbove(node.getPlayer(), ((TreeNodeSettings) treeModel.getChild(node.getParent(), index)).getPlayer());
					}
				}
			}
		});
		builder.add(arrowUpButton).at(FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		toggleButton = new JImageButton();
		toggleButton.setToolTipText(Messages.getGuiString("EnableDisableTranscodingEngine"));
		setButtonsState();
		toggleButton.addActionListener((ActionEvent e) -> {
			TreePath path = tree.getSelectionModel().getSelectionPath();
			if (
					path != null &&
					path.getLastPathComponent() instanceof TreeNodeSettings &&
					((TreeNodeSettings) path.getLastPathComponent()).getPlayer() != null
					) {
				((TreeNodeSettings) path.getLastPathComponent()).getPlayer().toggleEnabled(true);
				tree.updateUI();
				setButtonsState();
			}
		});
		builder.add(toggleButton).at(FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(Messages.getGuiString("Engines"));
		TreeNodeSettings commonEnc = new TreeNodeSettings(Messages.getGuiString("CommonTranscodeSettings"), null, buildCommon());
		tabbedPanel.add(commonEnc.id(), commonEnc.getConfigPanel());
		root.add(commonEnc);

		parent = new DefaultMutableTreeNode[5];
		parent[0] = new DefaultMutableTreeNode(Messages.getGuiString("VideoFilesEngines"));
		parent[1] = new DefaultMutableTreeNode(Messages.getGuiString("AudioFilesEngines"));
		parent[2] = new DefaultMutableTreeNode(Messages.getGuiString("WebVideoStreamingEngines"));
		parent[3] = new DefaultMutableTreeNode(Messages.getGuiString("WebAudioStreamingEngines"));
		parent[4] = new DefaultMutableTreeNode(Messages.getGuiString("MiscEngines"));
		root.add(parent[0]);
		root.add(parent[1]);
		root.add(parent[2]);
		root.add(parent[3]);
		root.add(parent[4]);

		tabbedPanel.add(EMPTY_PANEL, new JPanel());

		tree = new JTree(new DefaultTreeModel(root)) {
			private static final long serialVersionUID = -6703434752606636290L;
		};
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener((TreeSelectionEvent e) -> {
			setButtonsState();
			if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getLastPathComponent() instanceof TreeNodeSettings) {
				TreeNodeSettings tns = (TreeNodeSettings) e.getNewLeadSelectionPath().getLastPathComponent();
				cardLayout.show(tabbedPanel, tns.id());
			} else {
				cardLayout.show(tabbedPanel, EMPTY_PANEL);
			}
		});

		tree.setRequestFocusEnabled(false);
		tree.setCellRenderer(new TreeRenderer());
		JScrollPane pane = new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		builder.add(pane).at(FormLayoutUtil.flip(cc.xyw(2, 1, 4), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("EnginesAreInDescending")).at(FormLayoutUtil.flip(cc.xyw(2, 5, 4), colSpec, orientation));
		builder.addLabel(Messages.getGuiString("OrderTheHighestIsFirst")).at(FormLayoutUtil.flip(cc.xyw(2, 7, 4), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public void addEngines() {
		if (!EngineFactory.isInitialized()) {
			return;
		}
		for (Engine engine : EngineFactory.getEngines(false, true)) {
			if (videoHWacceleration != null && engine.isGPUAccelerationReady()) {
				videoHWacceleration.setEnabled(true);
				videoHWacceleration.setSelected(configuration.isGPUAcceleration());
				break;
			}
		}

		for (Engine engine : EngineFactory.getAllEngines()) {
			TreeNodeSettings engineNode = new TreeNodeSettings(engine.getName(), engine, null);

			JComponent configPanel = engineNode.getConfigPanel();
			if (configPanel == null) {
				configPanel = buildEmpty();
			}

			tabbedPanel.add(engineNode.id(), configPanel);
			parent[engine.purpose()].add(engineNode);
		}

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		tree.setSelectionRow(0);
		tree.updateUI();
	}

	public JComponent buildEmpty() {
		String colSpec = FormLayoutUtil.getColSpec(EMPTY_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, EMPTY_ROW_SPEC);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getGuiString("NoSettingsForNow"), FormLayoutUtil.flip(cc.xyw(1, 1, 3), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public JComponent buildCommon() {
		String colSpec = FormLayoutUtil.getColSpec(COMMON_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, COMMON_ROW_SPEC);
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getGuiString("GeneralSettings_SentenceCase")).at(FormLayoutUtil.flip(cc.xyw(1, 1, 3), colSpec, orientation));

		disableSubs = new JCheckBox(Messages.getGuiString("DisableSubtitles"), configuration.isDisableSubtitles());
		disableSubs.setContentAreaFilled(false);
		disableSubs.addItemListener((ItemEvent e) -> configuration.setDisableSubtitles((e.getStateChange() == ItemEvent.SELECTED)));

		if (!configuration.isHideAdvancedOptions()) {
			JCheckBox disableTranscoding = new JCheckBox(Messages.getGuiString("DisableAllTranscoding"), configuration.isDisableTranscoding());
			disableTranscoding.setContentAreaFilled(false);
			disableTranscoding.addItemListener((ItemEvent e) -> configuration.setDisableTranscoding((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(SwingUtil.getPreferredSizeComponent(disableTranscoding)).at(FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
			builder.add(new JLabel(Messages.getGuiString("SkipTranscodingFollowingExtensions"))).at(FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));
			notranscode = new JTextField(configuration.getDisableTranscodeForExtensions());
			notranscode.setToolTipText(Messages.getGuiString("ThisOverridesRendererConfiguration"));
			notranscode.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setDisableTranscodeForExtensions(notranscode.getText());
				}
			});
			builder.add(notranscode).at(FormLayoutUtil.flip(cc.xy(3, 5), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("ForceTranscodingFollowingExtensions")).at(FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
			forcetranscode = new JTextField(configuration.getForceTranscodeForExtensions());
			forcetranscode.setToolTipText(Messages.getGuiString("ThisOverridesRendererConfiguration"));
			forcetranscode.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setForceTranscodeForExtensions(forcetranscode.getText());
				}
			});
			builder.add(forcetranscode).at(FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));

			builder.addLabel(Messages.getGuiString("MaximumTranscodeBufferSize")).at(FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));
			maxbuffer = new JTextField("" + configuration.getMaxMemoryBufferSize());
			maxbuffer.setToolTipText(Messages.getGuiString("UsingSettingHigherThan200"));
			maxbuffer.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					try {
						int ab = Integer.parseInt(maxbuffer.getText());
						configuration.setMaxMemoryBufferSize(ab);
					} catch (NumberFormatException nfe) {
						LOGGER.debug("Could not parse max memory buffer size from \"" + maxbuffer.getText() + "\"");
					}
				}
			});
			builder.add(maxbuffer).at(FormLayoutUtil.flip(cc.xy(3, 9), colSpec, orientation));

			String nCpusLabel = String.format(Messages.getGuiString("CpuThreadsToUse"), Runtime.getRuntime().availableProcessors());
			builder.addLabel(nCpusLabel).at(FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));

			Integer[] guiCores = new Integer[MAX_CORES];
			for (int i = 0; i < MAX_CORES; i++) {
				guiCores[i] = i + 1;
			}
			JComboBox<Integer> nbcores = new JComboBox<>(guiCores);
			nbcores.setEditable(false);
			int nbConfCores = configuration.getNumberOfCpuCores();
			if (nbConfCores > 0 && nbConfCores <= MAX_CORES) {
				nbcores.setSelectedItem(nbConfCores);
			} else {
				nbcores.setSelectedIndex(0);
			}

			nbcores.addItemListener((ItemEvent e) -> configuration.setNumberOfCpuCores((int) e.getItem()));
			builder.add(nbcores).at(FormLayoutUtil.flip(cc.xy(3, 11), colSpec, orientation));
		} else {
			builder.add(SwingUtil.getPreferredSizeComponent(disableSubs)).at(FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
		}

		if (!configuration.isHideAdvancedOptions()) {
			JTabbedPane setupTabbedPanel = new JTabbedPane();
			setupTabbedPanel.setUI(new CustomTabbedPaneUI());
			setupTabbedPanel.addTab(Messages.getGuiString("VideoSettings"), buildVideoSetupPanel());
			setupTabbedPanel.addTab(Messages.getGuiString("AudioSettings"), buildAudioSetupPanel());
			setupTabbedPanel.addTab(Messages.getGuiString("SubtitlesSettings"), buildSubtitlesSetupPanel());
			builder.add(setupTabbedPanel).at(FormLayoutUtil.flip(cc.xywh(1, 13, 3, 1), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	private JComponent buildVideoSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, pref:grow", orientation);
		FormLayout layout = new FormLayout(colSpec, "$lgap, 2*(pref, 3dlu), 10dlu, 10dlu, 4*(pref, 3dlu), pref");
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		CellConstraints cc = new CellConstraints();

		videoHWacceleration = new JCheckBox(Messages.getGuiString("EnableGpuAcceleration"), configuration.isGPUAcceleration());
		videoHWacceleration.addItemListener((ItemEvent e) -> configuration.setGPUAcceleration((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(videoHWacceleration)).at(FormLayoutUtil.flip(cc.xy(1, 2), colSpec, orientation));
		videoHWacceleration.setEnabled(false);

		JCheckBox mpeg2remux = new JCheckBox(Messages.getGuiString("LosslessDvdVideoPlayback"), configuration.isMencoderRemuxMPEG2());
		mpeg2remux.setToolTipText(Messages.getGuiString("WhenEnabledMuxesDvd") + (Platform.isWindows() ? " " + Messages.getGuiString("AviSynthNotSupported") : "") + "</html>");
		mpeg2remux.setContentAreaFilled(false);
		mpeg2remux.addItemListener((ItemEvent e) -> configuration.setMencoderRemuxMPEG2((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(mpeg2remux)).at(FormLayoutUtil.flip(cc.xy(1, 6), colSpec, orientation));

		builder.addSeparator(Messages.getGuiString("MiscSettings")).at(FormLayoutUtil.flip(cc.xyw(1, 8, 3), colSpec, orientation));

		builder.add(new JLabel(Messages.getGuiString("TranscodingQualityMpeg2"))).at(FormLayoutUtil.flip(cc.xy(1, 10), colSpec, orientation));
		String[] keys = new String[] {
			"Automatic (Wired)",
			"Automatic (Wireless)",
			"keyint=5:vqscale=1:vqmin=1",
			"keyint=5:vqscale=1:vqmin=2",
			"keyint=5:vqscale=2:vqmin=3",
			"keyint=25:vqmax=5:vqmin=2",
			"keyint=25:vqmax=7:vqmin=2",
			"keyint=25:vqmax=8:vqmin=3"
		};
		//TODO Set ViewLevel.EXPERT when ViewLevel is fully implemented
		String[] values = new String[] {
			Messages.getGuiString("AutomaticWiredRecommend"), Messages.getGuiString("AutomaticWirelessRecommend"), // Automatic (Wireless)
String.format(Messages.getGuiString("LosslessQuality") + "%s", // Lossless
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=5:vqscale=1:vqmin=1)" : ""
			),
			String.format(Messages.getGuiString("GreatQuality") + "%s", // Great
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=5:vqscale=1:vqmin=2)" : ""
			),
			String.format(Messages.getGuiString("GoodQuality") + "%s", // Good (wired)
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=5:vqscale=2:vqmin=3)" : ""
			),
			String.format(Messages.getGuiString("GoodQualityHdWifi") + "%s", // Good (wireless)
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=25:vqmax=5:vqmin=2)" : ""
			),
			String.format(Messages.getGuiString("MediumQualityHdWifi") + "%s", // Medium (wireless)
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=25:vqmax=7:vqmin=2)" : ""
			),
			String.format(Messages.getGuiString("LowQualitySlowCpu") + "%s", // Low
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (keyint=25:vqmax=8:vqmin=3)" : ""
			)
		};
		final KeyedStringComboBoxModel mPEG2MainModel = new KeyedStringComboBoxModel(keys, values);

		vq = new JComboBox<>(mPEG2MainModel);
		vq.setPreferredSize(getPreferredHeight(vq));
		vq.setToolTipText(Messages.getGuiString("AutomaticWiredOrWireless"));
		mPEG2MainModel.setSelectedKey(configuration.getMPEG2MainSettings());
		vq.setEnabled(!configuration.isAutomaticMaximumBitrate());
		vq.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setMPEG2MainSettings(mPEG2MainModel.getSelectedKey());
			}
		});
		vq.setEditable(true);
		builder.add(vq).at(FormLayoutUtil.flip(cc.xy(3, 10), colSpec, orientation));

		builder.add(new JLabel(Messages.getGuiString("TranscodingQualityH264"))).at(FormLayoutUtil.flip(cc.xy(1, 12), colSpec, orientation));
		keys = new String[] {
			"Automatic (Wired)",
			"Automatic (Wireless)",
			"16"
		};
		//TODO Set ViewLevel.EXPERT when ViewLevel is fully implemented
		values = new String[] {
			Messages.getGuiString("AutomaticWiredRecommend"),
			Messages.getGuiString("AutomaticWirelessRecommend"),
			String.format(Messages.getGuiString("LosslessQuality") + "%s", // Lossless
				looksFrame.getViewLevel().isGreaterOrEqual(ViewLevel.ADVANCED) ? " (16)" : ""
			)
		};
		final KeyedStringComboBoxModel x264QualityModel = new KeyedStringComboBoxModel(keys, values);

		x264Quality = new JComboBox<>(x264QualityModel);
		x264Quality.setPreferredSize(getPreferredHeight(x264Quality));
		x264Quality.setToolTipText(Messages.getGuiString("AutomaticSettingServeBestQuality"));
		x264QualityModel.setSelectedKey(configuration.getx264ConstantRateFactor());
		x264Quality.setEnabled(!configuration.isAutomaticMaximumBitrate());
		x264Quality.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setx264ConstantRateFactor(x264QualityModel.getSelectedKey());
			}
		});
		x264Quality.setEditable(true);
		builder.add(x264Quality).at(FormLayoutUtil.flip(cc.xy(3, 12), colSpec, orientation));

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	private JComponent buildAudioSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, pref:grow", orientation);
		FormLayout layout = new FormLayout(colSpec, "$lgap, pref, 3dlu, 5*(pref, 3dlu), pref, 12dlu, 3*(pref, 3dlu), pref:grow");
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		CellConstraints cc = new CellConstraints();

		builder.addLabel(Messages.getGuiString("MaximumNumberAudioChannelsOutput")).at(FormLayoutUtil.flip(cc.xy(1, 2), colSpec, orientation));

		Integer[] keys = new Integer[] {2, 6};
		String[] values = new String[] {
			Messages.getGuiString("2ChannelsStereo"),
			Messages.getGuiString("6Channels51"), // 7.1 not supported by Mplayer
		};

		final KeyedComboBoxModel<Integer, String> audioChannelsModel = new KeyedComboBoxModel<>(keys, values);
		JComboBox<String> channels = new JComboBox<>(audioChannelsModel);
		channels.setEditable(false);
		audioChannelsModel.setSelectedKey(configuration.getAudioChannelCount());
		channels.addItemListener((ItemEvent e) -> configuration.setAudioChannelCount(audioChannelsModel.getSelectedKey()));
		builder.add(SwingUtil.getPreferredSizeComponent(channels)).at(FormLayoutUtil.flip(cc.xy(3, 2), colSpec, orientation));

		JCheckBox forcePCM = new JCheckBox(Messages.getGuiString("UseLpcmForAudio"), configuration.isAudioUsePCM());
		forcePCM.setToolTipText(Messages.getGuiString("ThisOptionLosslessNotBest"));
		forcePCM.setContentAreaFilled(false);
		forcePCM.addItemListener((ItemEvent e) -> configuration.setAudioUsePCM(e.getStateChange() == ItemEvent.SELECTED));
		builder.add(SwingUtil.getPreferredSizeComponent(forcePCM)).at(FormLayoutUtil.flip(cc.xy(1, 4), colSpec, orientation));

		JCheckBox ac3remux = new JCheckBox(Messages.getGuiString("KeepAc3Tracks"), configuration.isAudioRemuxAC3());
		ac3remux.setToolTipText(Messages.getGuiString("ThisOptionLosslessVeryStable") + (Platform.isWindows() ? " " + Messages.getGuiString("AviSynthNotSupported") : "") + "</html>");
		ac3remux.setEnabled(!configuration.isEncodedAudioPassthrough());
		ac3remux.addItemListener((ItemEvent e) -> configuration.setAudioRemuxAC3((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(ac3remux)).at(FormLayoutUtil.flip(cc.xy(1, 6), colSpec, orientation));

		JCheckBox forceDTSinPCM = new JCheckBox(Messages.getGuiString("KeepDtsTracks"), configuration.isAudioEmbedDtsInPcm());
		forceDTSinPCM.setToolTipText(Messages.getGuiString("ThisOptionLosslessUnstable") + (Platform.isWindows() ? " " + Messages.getGuiString("AviSynthNotSupported") : "") + "</html>");
		forceDTSinPCM.setEnabled(!configuration.isEncodedAudioPassthrough());
		forceDTSinPCM.setContentAreaFilled(false);
		forceDTSinPCM.addActionListener((ActionEvent e) -> configuration.setAudioEmbedDtsInPcm(forceDTSinPCM.isSelected()));
		builder.add(SwingUtil.getPreferredSizeComponent(forceDTSinPCM)).at(FormLayoutUtil.flip(cc.xy(1, 8), colSpec, orientation));

		JCheckBox encodedAudioPassthrough = new JCheckBox(Messages.getGuiString("EncodedAudioPassthrough"), configuration.isEncodedAudioPassthrough());
		encodedAudioPassthrough.setToolTipText(Messages.getGuiString("ThisOptionLossless") + (Platform.isWindows() ? " " + Messages.getGuiString("AviSynthNotSupported") : "") + "</html>");
		encodedAudioPassthrough.setContentAreaFilled(false);
		encodedAudioPassthrough.addItemListener((ItemEvent e) -> {
			configuration.setEncodedAudioPassthrough((e.getStateChange() == ItemEvent.SELECTED));
			ac3remux.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
			forceDTSinPCM.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
		});
		builder.add(SwingUtil.getPreferredSizeComponent(encodedAudioPassthrough)).at(cc.xyw(1, 10, 3));

		builder.addLabel(Messages.getGuiString("Ac3ReencodingAudioBitrate")).at(FormLayoutUtil.flip(cc.xy(1, 12), colSpec, orientation));
		abitrate = new JTextField("" + configuration.getAudioBitrate());
		abitrate.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(abitrate.getText());
					configuration.setAudioBitrate(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse audio bitrate from \"" + abitrate.getText() + "\"");
				}
			}
		});
		builder.add(abitrate).at(FormLayoutUtil.flip(cc.xy(3, 12), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("AudioLanguagePriority")).at(FormLayoutUtil.flip(cc.xy(1, 14), colSpec, orientation));
		langs = new JTextField(configuration.getAudioLanguages());
		langs.setToolTipText(Messages.getGuiString("YouCanRearrangeOrderAudio"));
		langs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAudioLanguages(langs.getText());
			}
		});
		builder.add(langs).at(FormLayoutUtil.flip(cc.xy(3, 14), colSpec, orientation));

		JPanel panel = builder.getPanel();
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	private JComponent buildSubtitlesSetupPanel() {
		String colSpec = FormLayoutUtil.getColSpec("left:pref, 3dlu, p:grow, 3dlu, right:p:grow, 3dlu, p:grow, 3dlu, right:p:grow, 3dlu, p:grow, 3dlu, right:p:grow, 3dlu, pref:grow", orientation);
		FormLayout layout = new FormLayout(colSpec, "$lgap, 11*(pref, 3dlu), pref");
		final UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		CellConstraints cc = new CellConstraints();

		builder.add(SwingUtil.getPreferredSizeComponent(disableSubs)).at(FormLayoutUtil.flip(cc.xy(1, 2), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("SubtitlesLanguagePriority")).at(FormLayoutUtil.flip(cc.xy(1, 4), colSpec, orientation));
		defaultsubs = new JTextField(configuration.getSubtitlesLanguages());
		defaultsubs.setToolTipText(Messages.getGuiString("YouCanRearrangeOrderSubtitles"));
		defaultsubs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setSubtitlesLanguages(defaultsubs.getText());
			}
		});
		builder.add(defaultsubs).at(FormLayoutUtil.flip(cc.xyw(3, 4, 13), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("ForcedLanguage")).at(FormLayoutUtil.flip(cc.xy(1, 6), colSpec, orientation));
		forcedsub = new JTextField(configuration.getForcedSubtitleLanguage());
		forcedsub.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForcedSubtitleLanguage(forcedsub.getText());
			}
		});
		builder.add(forcedsub).at(FormLayoutUtil.flip(cc.xyw(3, 6, 3), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("ForcedTags")).at(FormLayoutUtil.flip(cc.xyw(7, 6, 5, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		forcedtags = new JTextField(configuration.getForcedSubtitleTags());
		forcedtags.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForcedSubtitleTags(forcedtags.getText());
			}
		});
		builder.add(forcedtags).at(FormLayoutUtil.flip(cc.xyw(13, 6, 3), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("AudioSubtitlesLanguagePriority")).at(FormLayoutUtil.flip(cc.xy(1, 8), colSpec, orientation));
		defaultaudiosubs = new JTextField(configuration.getAudioSubLanguages());
		defaultaudiosubs.setToolTipText(Messages.getGuiString("AnExplanationDefaultValueAudio"));
		defaultaudiosubs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAudioSubLanguages(defaultaudiosubs.getText());
			}
		});
		builder.add(defaultaudiosubs).at(FormLayoutUtil.flip(cc.xyw(3, 8, 13), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("AlternateSubtitlesFolder")).at(FormLayoutUtil.flip(cc.xy(1, 10), colSpec, orientation));
		alternateSubFolder = new JTextField(configuration.getAlternateSubtitlesFolder());
		alternateSubFolder.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAlternateSubtitlesFolder(alternateSubFolder.getText());
			}
		});
		builder.add(alternateSubFolder).at(FormLayoutUtil.flip(cc.xyw(3, 10, 12), colSpec, orientation));

		JButton folderSelectButton = new JButton("...");
		folderSelectButton.addActionListener((ActionEvent e) -> {
			JFileChooser chooser;
			try {
				chooser = new JFileChooser();
			} catch (Exception ee) {
				chooser = new JFileChooser(new RestrictedFileSystemView());
			}
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getGuiString("ChooseAFolder"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				alternateSubFolder.setText(chooser.getSelectedFile().getAbsolutePath());
				configuration.setAlternateSubtitlesFolder(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		builder.add(folderSelectButton).at(FormLayoutUtil.flip(cc.xy(15, 10), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("NonUnicodeSubtitleEncoding")).at(FormLayoutUtil.flip(cc.xy(1, 12), colSpec, orientation));
		String[] keys = new String[]{
			"", "cp874", "cp932", "cp936", "cp949", "cp950", "cp1250",
			"cp1251", "cp1252", "cp1253", "cp1254", "cp1255", "cp1256",
			"cp1257", "cp1258", "ISO-2022-CN", "ISO-2022-JP", "ISO-2022-KR",
			"ISO-8859-1", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4",
			"ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8",
			"ISO-8859-9", "ISO-8859-10", "ISO-8859-11", "ISO-8859-13",
			"ISO-8859-14", "ISO-8859-15", "ISO-8859-16", "Big5", "EUC-JP",
			"EUC-KR", "GB18030", "IBM420", "IBM424", "KOI8-R", "Shift_JIS", "TIS-620"
		};
		String[] values = new String[]{
			Messages.getGuiString("AutoDetect"),
			Messages.getGuiString("CharacterSet.874"),
			Messages.getGuiString("CharacterSet.932"),
			Messages.getGuiString("CharacterSet.936"),
			Messages.getGuiString("CharacterSet.949"),
			Messages.getGuiString("CharacterSet.950"),
			Messages.getGuiString("CharacterSet.1250"),
			Messages.getGuiString("CharacterSet.1251"),
			Messages.getGuiString("CharacterSet.1252"),
			Messages.getGuiString("CharacterSet.1253"),
			Messages.getGuiString("CharacterSet.1254"),
			Messages.getGuiString("CharacterSet.1255"),
			Messages.getGuiString("CharacterSet.1256"),
			Messages.getGuiString("CharacterSet.1257"),
			Messages.getGuiString("CharacterSet.1258"),
			Messages.getGuiString("CharacterSet.2022-CN"),
			Messages.getGuiString("CharacterSet.2022-JP"),
			Messages.getGuiString("CharacterSet.2022-KR"),
			Messages.getGuiString("CharacterSet.8859-1"),
			Messages.getGuiString("CharacterSet.8859-2"),
			Messages.getGuiString("CharacterSet.8859-3"),
			Messages.getGuiString("CharacterSet.8859-4"),
			Messages.getGuiString("CharacterSet.8859-5"),
			Messages.getGuiString("CharacterSet.8859-6"),
			Messages.getGuiString("CharacterSet.8859-7"),
			Messages.getGuiString("CharacterSet.8859-8"),
			Messages.getGuiString("CharacterSet.8859-9"),
			Messages.getGuiString("CharacterSet.8859-10"),
			Messages.getGuiString("CharacterSet.8859-11"),
			Messages.getGuiString("CharacterSet.8859-13"),
			Messages.getGuiString("CharacterSet.8859-14"),
			Messages.getGuiString("CharacterSet.8859-15"),
			Messages.getGuiString("CharacterSet.8859-16"),
			Messages.getGuiString("CharacterSet.Big5"),
			Messages.getGuiString("CharacterSet.EUC-JP"),
			Messages.getGuiString("CharacterSet.EUC-KR"),
			Messages.getGuiString("CharacterSet.GB18030"),
			Messages.getGuiString("CharacterSet.IBM420"),
			Messages.getGuiString("CharacterSet.IBM424"),
			Messages.getGuiString("CharacterSet.KOI8-R"),
			Messages.getGuiString("CharacterSet.ShiftJIS"),
			Messages.getGuiString("CharacterSet.TIS-620")
		};

		final KeyedComboBoxModel<String, String> subtitleCodePageModel = new KeyedComboBoxModel<>(keys, values);
		JComboBox<String> subtitleCodePage = new JComboBox<>(subtitleCodePageModel);
		subtitleCodePage.setPreferredSize(getPreferredHeight(subtitleCodePage));
		subtitleCodePage.setToolTipText(Messages.getGuiString("YouFindListSupportedCharacter"));
		subtitleCodePageModel.setSelectedKey(configuration.getSubtitlesCodepage());
		subtitleCodePage.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setSubtitlesCodepage(subtitleCodePageModel.getSelectedKey());
			}
		});

		subtitleCodePage.setEditable(false);
		builder.add(subtitleCodePage).at(FormLayoutUtil.flip(cc.xyw(3, 12, 7), colSpec, orientation));

		JCheckBox fribidi = new JCheckBox(Messages.getGuiString("FribidiMode"), configuration.isMencoderSubFribidi());
		fribidi.setContentAreaFilled(false);
		fribidi.addItemListener((ItemEvent e) -> configuration.setMencoderSubFribidi(e.getStateChange() == ItemEvent.SELECTED));

		builder.add(fribidi).at(FormLayoutUtil.flip(cc.xyw(11, 12, 5, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("SpecifyTruetypeFont")).at(FormLayoutUtil.flip(cc.xy(1, 14), colSpec, orientation));
		defaultfont = new JTextField(configuration.getFont());
		defaultfont.setToolTipText(Messages.getGuiString("ToUseFontMustBeRegistered"));
		defaultfont.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setFont(defaultfont.getText());
			}
		});
		builder.add(defaultfont).at(FormLayoutUtil.flip(cc.xyw(3, 14, 12), colSpec, orientation));

		JButton fontselect = new CustomJButton("...");
		fontselect.addActionListener((ActionEvent e) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FontFileFilter());
			int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getGuiString("SelectTruetypeFont"));
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				defaultfont.setText(chooser.getSelectedFile().getAbsolutePath());
				configuration.setFont(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		builder.add(fontselect).at(FormLayoutUtil.flip(cc.xy(15, 14), colSpec, orientation));

		builder.addLabel(Messages.getGuiString("StyledSubtitles")).at(FormLayoutUtil.flip(cc.xy(1, 16), colSpec, orientation));

		JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		flowPanel.setComponentOrientation(orientation);
		builder.addLabel(Messages.getGuiString("FontScale")).at(FormLayoutUtil.flip(cc.xy(1, 16, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		assScale = new JTextField(configuration.getAssScale());
		assScale.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAssScale(assScale.getText());
			}
		});
		flowPanel.add(assScale);

		flowPanel.add(new JLabel(Messages.getGuiString("FontOutline")));

		int assOutlineValue;
		try {
			assOutlineValue = Integer.parseInt(configuration.getAssOutline());
		} catch (NumberFormatException e) {
			assOutlineValue = 1;
		}
		final SpinnerIntModel assOutlineModel = new SpinnerIntModel(assOutlineValue, 0, 99, 1);
		CustomJSpinner assOutline = new CustomJSpinner(assOutlineModel, true);
		assOutline.addChangeListener((ChangeEvent e) -> configuration.setAssOutline(assOutlineModel.getValue().toString()));
		flowPanel.add(assOutline);

		flowPanel.add(new JLabel(Messages.getGuiString("FontShadow")));

		int assShadowValue;
		try {
			assShadowValue = Integer.parseInt(configuration.getAssShadow());
		} catch (NumberFormatException e) {
			assShadowValue = 1;
		}
		final SpinnerIntModel assShadowModel = new SpinnerIntModel(assShadowValue, 0, 99, 1);
		CustomJSpinner assShadow = new CustomJSpinner(assShadowModel, true);
		assShadow.addChangeListener((ChangeEvent e) -> configuration.setAssShadow(assShadowModel.getValue().toString()));
		flowPanel.add(assShadow);

		flowPanel.add(new JLabel(Messages.getGuiString("MarginPx")));

		int assMarginValue;
		try {
			assMarginValue = Integer.parseInt(configuration.getAssMargin());
		} catch (NumberFormatException e) {
			assMarginValue = 10;
		}
		final SpinnerIntModel assMarginModel = new SpinnerIntModel(assMarginValue, 0, 999, 5);
		CustomJSpinner assMargin = new CustomJSpinner(assMarginModel, true);
		assMargin.addChangeListener((ChangeEvent e) -> configuration.setAssMargin(assMarginModel.getValue().toString()));

		flowPanel.add(assMargin);
		builder.add(flowPanel).at(FormLayoutUtil.flip(cc.xyw(3, 16, 13), colSpec, orientation));

		JCheckBox autoloadExternalSubtitles = new JCheckBox(Messages.getGuiString("AutomaticallyLoadSrtSubtitles"), configuration.isAutoloadExternalSubtitles());
		autoloadExternalSubtitles.setToolTipText(Messages.getGuiString("IfEnabledExternalSubtitlesPrioritized"));
		autoloadExternalSubtitles.setContentAreaFilled(false);
		autoloadExternalSubtitles.setEnabled(!configuration.isForceExternalSubtitles());
		autoloadExternalSubtitles.addItemListener((ItemEvent e) -> configuration.setAutoloadExternalSubtitles((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(autoloadExternalSubtitles).at(FormLayoutUtil.flip(cc.xyw(1, 18, 10), colSpec, orientation));

		JButton subColor = new JButton();
		subColor.setText(Messages.getGuiString("Color"));
		subColor.setBackground(configuration.getSubsColor());
		subColor.addActionListener((ActionEvent e) -> {
			final JColorChooser jColorChooser = new JColorChooser(subColor.getBackground());
			Locale locale = PMS.getLocale();
			jColorChooser.setLocale(locale);
			jColorChooser.setComponentOrientation(ComponentOrientation.getOrientation(locale));
			JDialog dialog = JColorChooser.createDialog(looksFrame, Messages.getGuiString("ChooseSubtitlesColor"), true, jColorChooser, (ActionEvent e1) -> {
				Color newColor = jColorChooser.getColor();
				if (newColor != null) {
					subColor.setBackground(newColor);
					configuration.setSubsColor(new SubtitleColor(newColor));
					// Subtitle color has been changed so all temporary subtitles must be deleted
					SubtitleUtils.deleteSubs();
				}
			}, null);
			dialog.setVisible(true);
			dialog.dispose();
		});
		builder.add(subColor).at(FormLayoutUtil.flip(cc.xyw(11, 18, 5), colSpec, orientation));

		JCheckBox forceExternalSubtitles = new JCheckBox(Messages.getGuiString("ForceExternalSubtitles"), configuration.isForceExternalSubtitles());
		forceExternalSubtitles.setToolTipText(Messages.getGuiString("IfEnabledExternalSubtitlesAlways"));
		forceExternalSubtitles.setContentAreaFilled(false);
		forceExternalSubtitles.addItemListener((ItemEvent e) -> {
			configuration.setForceExternalSubtitles((e.getStateChange() == ItemEvent.SELECTED));
			if (configuration.isForceExternalSubtitles()) {
				autoloadExternalSubtitles.setSelected(true);
			}
			autoloadExternalSubtitles.setEnabled(!configuration.isForceExternalSubtitles());
		});

		builder.add(SwingUtil.getPreferredSizeComponent(forceExternalSubtitles)).at(FormLayoutUtil.flip(cc.xyw(1, 20, 6), colSpec, orientation));

		JCheckBox deleteDownloadedSubtitles = new JCheckBox(Messages.getGuiString("DeleteDownloadedLiveSubtitlesAfter"), !configuration.isLiveSubtitlesKeep());
		deleteDownloadedSubtitles.setToolTipText(Messages.getGuiString("DeterminesDownloadedLiveSubtitlesDeleted"));
		deleteDownloadedSubtitles.setContentAreaFilled(false);
		deleteDownloadedSubtitles.addItemListener((ItemEvent e) -> configuration.setLiveSubtitlesKeep((e.getStateChange() != ItemEvent.SELECTED)));

		builder.add(SwingUtil.getPreferredSizeComponent(deleteDownloadedSubtitles)).at(FormLayoutUtil.flip(cc.xyw(7, 20, 9, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));

		JCheckBox useEmbeddedSubtitlesStyle = new JCheckBox(Messages.getGuiString("UseEmbeddedStyle"), configuration.isUseEmbeddedSubtitlesStyle());
		useEmbeddedSubtitlesStyle.setToolTipText(Messages.getGuiString("IfEnabledWontModifySubtitlesStyling"));
		useEmbeddedSubtitlesStyle.setContentAreaFilled(false);
		useEmbeddedSubtitlesStyle.addItemListener((ItemEvent e) -> configuration.setUseEmbeddedSubtitlesStyle(e.getStateChange() == ItemEvent.SELECTED));

		builder.add(SwingUtil.getPreferredSizeComponent(useEmbeddedSubtitlesStyle)).at(FormLayoutUtil.flip(cc.xyw(1, 22, 4), colSpec, orientation));


		final SpinnerIntModel liveSubtitlesLimitModel = new SpinnerIntModel(configuration.getLiveSubtitlesLimit(), 1, 999, 1);
		CustomJSpinner liveSubtitlesLimit = new CustomJSpinner(liveSubtitlesLimitModel, true);
		liveSubtitlesLimit.setToolTipText(Messages.getGuiString("SetsMaximumNumberLiveSubtitles"));
		liveSubtitlesLimit.addChangeListener((ChangeEvent e) -> configuration.setLiveSubtitlesLimit(liveSubtitlesLimitModel.getIntValue()));
		JLabel liveSubtitlesLimitLabel = new JLabel(Messages.getGuiString("LimitNumberLiveSubtitlesTo"));
		liveSubtitlesLimitLabel.setLabelFor(liveSubtitlesLimit);
		builder.add(liveSubtitlesLimitLabel).at(FormLayoutUtil.flip(cc.xyw(7, 22, 7, CellConstraints.RIGHT, CellConstraints.CENTER), colSpec, orientation));
		builder.add(liveSubtitlesLimit).at(FormLayoutUtil.flip(cc.xy(15, 22), colSpec, orientation));

		Integer[] depth = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5};

		builder.addLabel(Messages.getGuiString("3dSubtitlesDepth")).at(FormLayoutUtil.flip(cc.xy(1, 24), colSpec, orientation));
		JComboBox<Integer> depth3D = new JComboBox<>(depth);
		depth3D.setSelectedItem(configuration.getDepth3D());
		depth3D.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setDepth3D((int) e.getItem());
			}
		});
		builder.add(depth3D).at(FormLayoutUtil.flip(cc.xyw(3, 24, 13), colSpec, orientation));

		final JPanel panel = builder.getPanel();
		SwingUtil.enableContainer(panel, !configuration.isDisableSubtitles());
		// If "Disable Subtitles" is not selected, subtitles are enabled
		disableSubs.addItemListener((ItemEvent e) -> SwingUtil.enableContainer(panel, e.getStateChange() != ItemEvent.SELECTED));

		panel.applyComponentOrientation(orientation);
		return panel;
	}

	// This is kind of a hack to give combo boxes a small preferred size
	private static Dimension getPreferredHeight(JComponent component) {
		return new Dimension(20, component.getPreferredSize().height);
	}

	/**
	 * Enable the video quality settings for FFMpeg/Mencoder/VLC when the
	 * automatic maximum bitrate is not used.
	 *
	 * @param automatic when it is set <code>true</code> than the video
	 * quality settings are disabled.
	 */
	public void enableVideoQualitySettings(boolean automatic) {
		vq.setEnabled(!automatic);
		x264Quality.setEnabled(!automatic);
	}
}
