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

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import net.pms.configuration.PmsConfiguration;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.Messages;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.components.CustomJCheckBox;
import net.pms.newgui.components.CustomJLabel;
import net.pms.newgui.components.CustomJTextField;
import net.pms.newgui.components.OrientedPanelBuilder;
import net.pms.newgui.components.OrientedSpanBuilder;
import net.pms.PMS;
import net.pms.util.SubtitleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranscodingTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(TranscodingTab.class);
	private static final String COMMON_COL_SPEC = "left:min(150dlu;pref), 3dlu, p:grow";
	private static final String COMMON_ROW_SPEC = "4*(pref, 3dlu), pref, 9dlu, pref, 9dlu:grow, pref";
	private static final String EMPTY_COL_SPEC = "left:pref, 3dlu, pref:grow";
	private static final String EMPTY_ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p , 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 20dlu, p, 3dlu, p, 3dlu, p";
	private static final String LEFT_COL_SPEC = "left:pref, pref, pref, pref, 0:grow";
	private static final String LEFT_ROW_SPEC = "fill:10:grow, 3dlu, p, 3dlu, p, 3dlu, p";
	private static final String MAIN_COL_SPEC = "left:min(150dlu;pref), pref, 7dlu, pref, pref, fill:10:grow";
	private static final String MAIN_ROW_SPEC = "fill:10:grow";

	private final PmsConfiguration configuration;
	private LooksFrame looksFrame;

	TranscodingTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	private CustomJCheckBox disableSubs;
	private JTextField forcetranscode;
	private JTextField notranscode;
	private JTextField maxbuffer;
	private JComboBox nbcores;
	private DefaultMutableTreeNode parent[];
	private JPanel tabbedPanel;
	private CardLayout cl;
	private JTextField abitrate;
	private JTree tree;
	private CustomJCheckBox forcePCM;
	private CustomJCheckBox encodedAudioPassthrough;
	public static CustomJCheckBox forceDTSinPCM;
	private JComboBox channels;
	private JComboBox vq;
	private JComboBox x264Quality;
	private CustomJCheckBox ac3remux;
	private CustomJCheckBox mpeg2remux;
	private CustomJCheckBox chapter_support;
	private JTextField chapter_interval;
	private CustomJCheckBox videoHWacceleration;
	private JTextField langs;
	private JTextField defaultsubs;
	private JTextField forcedsub;
	private JTextField forcedtags;
	private JTextField alternateSubFolder;
	private JButton folderSelectButton;
	private CustomJCheckBox autoloadExternalSubtitles;
	private JTextField defaultaudiosubs;
	private JComboBox subtitleCodePage;
	private JTextField defaultfont;
	private JButton fontselect;
	private CustomJCheckBox fribidi;
	private JTextField ass_scale;
	private JTextField ass_outline;
	private JTextField ass_shadow;
	private JTextField ass_margin;
	private JButton subColor;
	private CustomJCheckBox forceExternalSubtitles;
	private CustomJCheckBox useEmbeddedSubtitlesStyle;
	private JTextField depth3D;

	/*
	 * 16 cores is the maximum allowed by MEncoder as of MPlayer r34863.
	 * Revisions before that allowed only 8.
	 */
	private static final int MAX_CORES = 16;

	private void updateEngineModel() {
		ArrayList<String> engines = new ArrayList<>();
		Object root = tree.getModel().getRoot();
		for (int i = 0; i < tree.getModel().getChildCount(root); i++) {
			Object firstChild = tree.getModel().getChild(root, i);
			if (!tree.getModel().isLeaf(firstChild)) {
				for (int j = 0; j < tree.getModel().getChildCount(firstChild); j++) {
					Object secondChild = tree.getModel().getChild(firstChild, j);
					if (secondChild instanceof TreeNodeSettings) {
						TreeNodeSettings tns = (TreeNodeSettings) secondChild;
						if (tns.isEnable() && tns.getPlayer() != null) {
							engines.add(tns.getPlayer().id());
						}
					}
				}
			}
		}
		configuration.setEnginesAsList(engines);
	}

	private void handleCardComponentChange(Component component) {
		tabbedPanel.setPreferredSize(component.getPreferredSize());
		tabbedPanel.getParent().invalidate();
		tabbedPanel.getParent().validate();
		tabbedPanel.getParent().repaint();
	}

	public JComponent build() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(MAIN_COL_SPEC, MAIN_ROW_SPEC);
		builder.border(Borders.DLU4);

		builder.opaque(true);

		CellConstraints cc = builder.getCellConstraints();

		if (!configuration.isHideAdvancedOptions()) {
			builder.add(buildRightTabbedPanel(), cc.xyw(4, 1, 3));
			builder.add(buildLeft(), cc.xy(2, 1));
		} else {
			builder.add(buildRightTabbedPanel(), cc.xyw(2, 1, 5));
			builder.add(buildLeft(), cc.xy(2, 1));
		}

		return builder._getPanel();
	}

	private JComponent buildRightTabbedPanel() {
		cl = new CardLayout();
		tabbedPanel = new JPanel(cl);
		tabbedPanel.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane scrollPane = new JScrollPane(tabbedPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	public JComponent buildLeft() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(LEFT_COL_SPEC, LEFT_ROW_SPEC);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = builder.getCellConstraints();

		CustomJButton but = new CustomJButton(LooksFrame.readImageIcon("button-arrow-down.png"));
		but.setToolTipText(Messages.getString("TrTab2.6"));
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings node = ((TreeNodeSettings) path.getLastPathComponent());
					if (node.getPlayer() != null) {
						DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();   // get the tree model
						//now get the index of the selected node in the DefaultTreeModel
						int index = dtm.getIndexOfChild(node.getParent(), node);
						// if selected node is first, return (can't move it up)
						if (index < node.getParent().getChildCount() - 1) {
							dtm.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index + 1);   // move the node
							dtm.reload();
							for (int i = 0; i < tree.getRowCount(); i++) {
								tree.expandRow(i);
							}
							tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
							updateEngineModel();
						}
					}
				}
			}
		});
		builder.add(but, cc.xy(2, 3));

		CustomJButton but2 = new CustomJButton(LooksFrame.readImageIcon("button-arrow-up.png"));
		but2.setToolTipText(Messages.getString("TrTab2.6"));
		but2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings node = ((TreeNodeSettings) path.getLastPathComponent());
					if (node.getPlayer() != null) {
						DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();   // get the tree model
						//now get the index of the selected node in the DefaultTreeModel
						int index = dtm.getIndexOfChild(node.getParent(), node);
						// if selected node is first, return (can't move it up)
						if (index != 0) {
							dtm.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index - 1);   // move the node
							dtm.reload();
							for (int i = 0; i < tree.getRowCount(); i++) {
								tree.expandRow(i);
							}
							tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
							updateEngineModel();
						}
					}
				}
			}
		});
		builder.add(but2, cc.xy(3, 3));

		CustomJButton but3 = new CustomJButton(LooksFrame.readImageIcon("button-toggleengine.png"));
		but3.setToolTipText(Messages.getString("TrTab2.0"));
		but3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings && ((TreeNodeSettings) path.getLastPathComponent()).getPlayer() != null) {
					((TreeNodeSettings) path.getLastPathComponent()).setEnable(!((TreeNodeSettings) path.getLastPathComponent()).isEnable());
					updateEngineModel();
					tree.updateUI();
				}
			}
		});
		builder.add(but3, cc.xy(4, 3));

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(Messages.getString("TrTab2.11"));
		TreeNodeSettings commonEnc = new TreeNodeSettings(Messages.getString("TrTab2.5"), null, buildCommon());
		commonEnc.getConfigPanel().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				handleCardComponentChange(e.getComponent());
			}
		});
		tabbedPanel.add(commonEnc.id(), commonEnc.getConfigPanel());
		root.add(commonEnc);

		parent = new DefaultMutableTreeNode[5];
		parent[0] = new DefaultMutableTreeNode(Messages.getString("TrTab2.14"));
		parent[1] = new DefaultMutableTreeNode(Messages.getString("TrTab2.15"));
		parent[2] = new DefaultMutableTreeNode(Messages.getString("TrTab2.16"));
		parent[3] = new DefaultMutableTreeNode(Messages.getString("TrTab2.17"));
		parent[4] = new DefaultMutableTreeNode(Messages.getString("TrTab2.18"));
		root.add(parent[0]);
		root.add(parent[1]);
		root.add(parent[2]);
		root.add(parent[3]);
		root.add(parent[4]);

		tree = new JTree(new DefaultTreeModel(root)) {
			private static final long serialVersionUID = -6703434752606636290L;
		};
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings tns = (TreeNodeSettings) e.getNewLeadSelectionPath().getLastPathComponent();
					cl.show(tabbedPanel, tns.id());
				}
			}
		});

		tree.setRequestFocusEnabled(false);
		tree.setCellRenderer(new TreeRenderer());
		JScrollPane pane = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		builder.add(pane, cc.xyw(2, 1, 4));

		builder._addLabel(Messages.getString("TrTab2.19"), cc.xyw(2, 5, 4));
		builder._addLabel(Messages.getString("TrTab2.20"), cc.xyw(2, 7, 4));

		return builder._getPanel();
	}

	public void addEngines() {
		ArrayList<Player> disPlayers = new ArrayList<>();
		ArrayList<Player> ordPlayers = new ArrayList<>();
		PMS r = PMS.get();

		for (String id : configuration.getEnginesAsList(r.getRegistry())) {
			//boolean matched = false;
			for (Player p : PlayerFactory.getAllPlayers()) {
				if (p.id().equals(id)) {
					ordPlayers.add(p);
					if (p.isGPUAccelerationReady()) {
						videoHWacceleration.setEnabled(true);
						videoHWacceleration.setSelected(configuration.isGPUAcceleration());
					}
					//matched = true;
				}
			}
		}

		for (Player p : PlayerFactory.getAllPlayers()) {
			if (!ordPlayers.contains(p)) {
				ordPlayers.add(p);
				disPlayers.add(p);
			}
		}

		for (Player p : ordPlayers) {
			TreeNodeSettings engine = new TreeNodeSettings(p.name(), p, null);

			if (disPlayers.contains(p)) {
				engine.setEnable(false);
			}

			JComponent jc = engine.getConfigPanel();
			if (jc == null) {
				jc = buildEmpty();
			}

			jc.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					handleCardComponentChange(e.getComponent());
				}
			});

			tabbedPanel.add(engine.id(), jc);
			parent[p.purpose()].add(engine);
		}

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		tree.setSelectionRow(0);
	}

	public JComponent buildEmpty() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(EMPTY_COL_SPEC, EMPTY_ROW_SPEC);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = builder.getCellConstraints();

		builder._addSeparator(Messages.getString("TrTab2.1"), cc.xyw(1, 1, 3));

		return builder._getPanel();
	}

	public JComponent buildCommon() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(COMMON_COL_SPEC, COMMON_ROW_SPEC);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = builder.getCellConstraints();

		JComponent cmp = builder._addSeparator(Messages.getString("NetworkTab.5"), cc.xyw(1, 1, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		disableSubs = new CustomJCheckBox(Messages.getString("TrTab2.51"), configuration.isDisableSubtitles());
		disableSubs.setContentAreaFilled(false);
 		disableSubs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setDisableSubtitles((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		if (!configuration.isHideAdvancedOptions()) {
			builder._addLabel(Messages.getString("TrTab2.23"), cc.xy(1, 3));
			maxbuffer = new JTextField("" + configuration.getMaxMemoryBufferSize());
			maxbuffer.setToolTipText(Messages.getString("TrTab2.73"));
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
			builder.add(maxbuffer, cc.xy(3, 3));

			String nCpusLabel = Messages.getString("TrTab2.24", Runtime.getRuntime().availableProcessors());
			builder._addLabel(nCpusLabel, cc.xy(1, 5));

			String[] guiCores = new String[MAX_CORES];
			for (int i = 0; i < MAX_CORES; i++) {
				guiCores[i] = Integer.toString(i + 1);
			}
			nbcores = new JComboBox(guiCores);
			nbcores.setEditable(false);
			builder.orientLabelRenderer((JLabel)nbcores.getRenderer());
			int nbConfCores = configuration.getNumberOfCpuCores();
			if (nbConfCores > 0 && nbConfCores <= MAX_CORES) {
				nbcores.setSelectedItem(Integer.toString(nbConfCores));
			} else {
				nbcores.setSelectedIndex(0);
			}

			nbcores.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setNumberOfCpuCores(Integer.parseInt(e.getItem().toString()));
				}
			});
			builder.add(nbcores, cc.xy(3, 5));

			chapter_support = new CustomJCheckBox(Messages.getString("TrTab2.52"), configuration.isChapterSupport());
			chapter_support.setContentAreaFilled(false);
			chapter_support.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setChapterSupport((e.getStateChange() == ItemEvent.SELECTED));
					chapter_interval.setEnabled(configuration.isChapterSupport());
				}
			});
			builder.add(chapter_support, cc.xy(1, 7));

			chapter_interval = new JTextField("" + configuration.getChapterInterval());
			chapter_interval.setEnabled(configuration.isChapterSupport());
			chapter_interval.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					try {
						int ab = Integer.parseInt(chapter_interval.getText());
						configuration.setChapterInterval(ab);
					} catch (NumberFormatException nfe) {
						LOGGER.debug("Could not parse chapter interval from \"" + chapter_interval.getText() + "\"");
					}
				}
			});
			builder.add(chapter_interval, cc.xy(3, 7));
			builder.add(disableSubs, cc.xy(1, 9));
		} else {
			builder.add(disableSubs, cc.xy(1, 3));
		}

		JTabbedPane setupTabbedPanel = new JTabbedPane();
		setupTabbedPanel.setUI(new CustomTabbedPaneUI());

		setupTabbedPanel.addTab(Messages.getString("TrTab2.67"), buildVideoSetupPanel());
		setupTabbedPanel.addTab(Messages.getString("TrTab2.68"), buildAudioSetupPanel());
		setupTabbedPanel.addTab(Messages.getString("MEncoderVideo.8"), buildSubtitlesSetupPanel());

		if (!configuration.isHideAdvancedOptions()) {
			builder.add(setupTabbedPanel, cc.xywh(1, 11, 3, 3));
		}

		return builder._getPanel();
	}

	private JComponent buildVideoSetupPanel() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(
			"left:min(150dlu;pref), 3dlu, p:grow",
			"$lgap, 2*(pref, 3dlu), 10dlu, 10dlu, 4*(pref, 3dlu), pref"
		);
		builder.border(Borders.DLU4);
		CellConstraints cc = builder.getCellConstraints();

		videoHWacceleration = new CustomJCheckBox(Messages.getString("TrTab2.70"), configuration.isGPUAcceleration());
		videoHWacceleration.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setGPUAcceleration((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(videoHWacceleration, cc.xy(1, 2));
		videoHWacceleration.setEnabled(false);

		mpeg2remux = new CustomJCheckBox(Messages.getString("MEncoderVideo.39"), configuration.isMencoderRemuxMPEG2());
		mpeg2remux.setToolTipText(Messages.getString("TrTab2.82") + (Platform.isWindows() ? " " + Messages.getString("TrTab2.21") : "") + "</html>");
		mpeg2remux.setContentAreaFilled(false);
		mpeg2remux.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderRemuxMPEG2((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(mpeg2remux, cc.xy(1, 6));

		JComponent cmp = builder._addSeparator(Messages.getString("TrTab2.7"), cc.xyw(1, 8, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.add(new CustomJLabel(Messages.getString("TrTab2.32")), cc.xy(1, 10));
		Object data[] = new Object[] {
			configuration.getMPEG2MainSettings(),                                                   /* Current setting */
			String.format("Automatic (Wired)  /* %s */",          Messages.getString("TrTab2.71")), /* Recommended for wired networks */
			String.format("Automatic (Wireless)  /* %s */",       Messages.getString("TrTab2.72")), /* Recommended for wireless networks */
			String.format("keyint=5:vqscale=1:vqmin=2  /* %s */", Messages.getString("TrTab2.60")), /* Great */
			String.format("keyint=5:vqscale=1:vqmin=1  /* %s */", Messages.getString("TrTab2.61")), /* Lossless */
			String.format("keyint=5:vqscale=2:vqmin=3  /* %s */", Messages.getString("TrTab2.62")), /* Good (wired) */
			String.format("keyint=25:vqmax=5:vqmin=2  /* %s */",  Messages.getString("TrTab2.63")), /* Good (wireless) */
			String.format("keyint=25:vqmax=7:vqmin=2  /* %s */",  Messages.getString("TrTab2.64")), /* Medium (wireless) */
			String.format("keyint=25:vqmax=8:vqmin=3  /* %s */",  Messages.getString("TrTab2.65"))  /* Low */
		};

		GuiUtil.MyComboBoxModel cbm = new GuiUtil.MyComboBoxModel(data);
		vq = new JComboBox(cbm);
		vq.setToolTipText(Messages.getString("TrTab2.74"));
		vq.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String s = (String) e.getItem();
					if (s.contains("/*")) {
						s = s.substring(0, s.indexOf("/*")).trim();
					}
					configuration.setMPEG2MainSettings(s);
				}
			}
		});
		vq.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				vq.getItemListeners()[0].itemStateChanged(new ItemEvent(vq, 0, vq.getEditor().getItem(), ItemEvent.SELECTED));
			}
		});
		vq.setEditable(true);
		builder.orientLabelRenderer((JLabel)vq.getRenderer());
		builder.add(vq, cc.xy(3, 10));

		builder.add(new CustomJLabel(Messages.getString("TrTab2.79")), cc.xy(1, 12));
		Object x264QualityOptions[] = new Object[] {
			configuration.getx264ConstantRateFactor(),                                        /* Current setting */
			String.format("Automatic (Wired)  /* %s */", Messages.getString("TrTab2.71")),    /* Recommended for wired networks */
			String.format("Automatic (Wireless)  /* %s */", Messages.getString("TrTab2.72")), /* Recommended for wireless networks */
			String.format("16  /* %s */", Messages.getString("TrTab2.61"))                    /* Lossless */
		};

		GuiUtil.MyComboBoxModel cbm2 = new GuiUtil.MyComboBoxModel(x264QualityOptions);
		x264Quality = new JComboBox(cbm2);
		x264Quality.setToolTipText(Messages.getString("TrTab2.81"));
		x264Quality.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String s = (String) e.getItem();
					if (s.contains("/*")) {
						s = s.substring(0, s.indexOf("/*")).trim();
					}
					configuration.setx264ConstantRateFactor(s);
				}
			}
		});
		x264Quality.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				x264Quality.getItemListeners()[0].itemStateChanged(new ItemEvent(x264Quality, 0, x264Quality.getEditor().getItem(), ItemEvent.SELECTED));
			}
		});
		x264Quality.setEditable(true);
		builder.orientLabelRenderer((JLabel)x264Quality.getRenderer());
		builder.add(x264Quality, cc.xy(3, 12));

		builder.add(new CustomJLabel(Messages.getString("TrTab2.8")), cc.xy(1, 14));
		notranscode = new JTextField(configuration.getDisableTranscodeForExtensions());
		notranscode.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setDisableTranscodeForExtensions(notranscode.getText());
			}
		});
		builder.add(notranscode, cc.xy(3, 14));

		builder._addLabel(Messages.getString("TrTab2.9"), cc.xy(1, 16));
		forcetranscode = new JTextField(configuration.getForceTranscodeForExtensions());
		forcetranscode.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForceTranscodeForExtensions(forcetranscode.getText());
			}
		});
		builder.add(forcetranscode, cc.xy(3, 16));

		return builder._getPanel();
	}

	private JComponent buildAudioSetupPanel() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(
//			"left:pref, 3dlu, pref:grow",
			"left:min(150dlu;pref), 3dlu, p:grow",
			"$lgap, pref, 3dlu, 5*(pref, 3dlu), pref, 12dlu, 3*(pref, 3dlu), pref:grow"
		);
		builder.border(Borders.DLU4);
		CellConstraints cc = builder.getCellConstraints();

		builder._addLabel(Messages.getString("TrTab2.50"), cc.xy(1, 2));

		channels = new JComboBox(new Object[]{Messages.getString("TrTab2.55"),  Messages.getString("TrTab2.56") /*, "8 channels 7.1" */}); // 7.1 not supported by Mplayer :/
		channels.setEditable(false);
		builder.orientLabelRenderer((JLabel)channels.getRenderer());
		if (configuration.getAudioChannelCount() == 2) {
			channels.setSelectedIndex(0);
		} else {
			channels.setSelectedIndex(1);
		}
		channels.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioChannelCount(Integer.parseInt(e.getItem().toString().substring(0, 1)));
			}
		});
		builder.add(channels, cc.xy(3, 2));

		forcePCM = new CustomJCheckBox(Messages.getString("TrTab2.27"), configuration.isAudioUsePCM());
		forcePCM.setToolTipText(Messages.getString("TrTab2.83"));
		forcePCM.setContentAreaFilled(false);
		forcePCM.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioUsePCM(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(forcePCM, cc.xy(1, 4));

		ac3remux = new CustomJCheckBox(Messages.getString("TrTab2.26"), configuration.isAudioRemuxAC3());
		ac3remux.setToolTipText(Messages.getString("TrTab2.84") + (Platform.isWindows() ? " " + Messages.getString("TrTab2.21") : "") + "</html>");
		ac3remux.setEnabled(!configuration.isEncodedAudioPassthrough());
		ac3remux.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioRemuxAC3((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(ac3remux, cc.xy(1, 6));

		forceDTSinPCM = new CustomJCheckBox(Messages.getString("TrTab2.28"), configuration.isAudioEmbedDtsInPcm());
		forceDTSinPCM.setToolTipText(Messages.getString("TrTab2.85") + (Platform.isWindows() ? " " + Messages.getString("TrTab2.21") : "") + "</html>");
		forceDTSinPCM.setEnabled(!configuration.isEncodedAudioPassthrough());
		forceDTSinPCM.setContentAreaFilled(false);
		forceDTSinPCM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setAudioEmbedDtsInPcm(forceDTSinPCM.isSelected());
			}
		});
		builder.add(forceDTSinPCM, cc.xy(1, 8));

		encodedAudioPassthrough = new CustomJCheckBox(Messages.getString("TrTab2.53"), configuration.isEncodedAudioPassthrough());
		encodedAudioPassthrough.setToolTipText(Messages.getString("TrTab2.86") + (Platform.isWindows() ? " " + Messages.getString("TrTab2.21") : "") + "</html>");
		encodedAudioPassthrough.setContentAreaFilled(false);
		encodedAudioPassthrough.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setEncodedAudioPassthrough((e.getStateChange() == ItemEvent.SELECTED));
				ac3remux.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
				forceDTSinPCM.setEnabled((e.getStateChange() != ItemEvent.SELECTED));
			}
		});
		builder.add(encodedAudioPassthrough, cc.xyw(1, 10, 3));

		builder._addLabel(Messages.getString("TrTab2.29"), cc.xy(1, 12));
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
		builder.add(abitrate, cc.xy(3, 12));

		builder._addLabel(Messages.getString("MEncoderVideo.7"), cc.xy(1, 14));
		langs = new JTextField(configuration.getAudioLanguages());
		langs.setToolTipText(Messages.getString("TrTab2.75"));
		langs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAudioLanguages(langs.getText());
			}
		});
		builder.add(langs, cc.xy(3, 14));

		return builder._getPanel();
	}

	private JComponent buildSubtitlesSetupPanel() {
		OrientedPanelBuilder builder = new OrientedPanelBuilder(
			"left:min(150dlu;pref), 3dlu, p:grow",
			"$lgap, 11*(pref, 3dlu), pref"
		);
		builder.border(Borders.DLU4);
		CellConstraints cc = builder.getCellConstraints();

		builder._addLabel(Messages.getString("MEncoderVideo.9"), cc.xy(1, 2));

		OrientedSpanBuilder subs = new OrientedSpanBuilder("p:grow, 6dlu, p, 2dlu, p:grow, 6dlu, p, 2dlu, p:grow");

		defaultsubs = new CustomJTextField(configuration.getSubtitlesLanguages());
		defaultsubs.setToolTipText(Messages.getString("TrTab2.76"));
		defaultsubs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setSubtitlesLanguages(defaultsubs.getText());
			}
		});
		subs.append(defaultsubs);

		forcedsub = new JTextField(configuration.getForcedSubtitleLanguage());
		forcedsub.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForcedSubtitleLanguage(forcedsub.getText());
			}
		});
		subs.append(Messages.getString("MEncoderVideo.94"));
		subs.append(forcedsub);

		forcedtags = new JTextField(configuration.getForcedSubtitleTags());
		forcedtags.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForcedSubtitleTags(forcedtags.getText());
			}
		});
		subs.append(Messages.getString("MEncoderVideo.95"));
		subs.append(forcedtags);

		builder.add(subs._getPanel(), cc.xy(3, 2));

		builder._addLabel(Messages.getString("MEncoderVideo.10"), cc.xy(1, 4));
		defaultaudiosubs = new JTextField(configuration.getAudioSubLanguages());
		defaultaudiosubs.setToolTipText(Messages.getString("TrTab2.77"));
		defaultaudiosubs.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAudioSubLanguages(defaultaudiosubs.getText());
			}
		});
		builder.add(defaultaudiosubs, cc.xy(3, 4));

		OrientedSpanBuilder subsfolder = new OrientedSpanBuilder("p:grow, 1dlu, p");

		builder._addLabel(Messages.getString("MEncoderVideo.37"), cc.xyw(1, 6, 2));
		alternateSubFolder = new JTextField(configuration.getAlternateSubtitlesFolder());
		alternateSubFolder.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAlternateSubtitlesFolder(alternateSubFolder.getText());
			}
		});
		subsfolder.append(alternateSubFolder);

		folderSelectButton = new JButton("...");
		folderSelectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser;
				try {
					chooser = new JFileChooser();
				} catch (Exception ee) {
					chooser = new JFileChooser(new RestrictedFileSystemView());
				}
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("FoldTab.28"));
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					alternateSubFolder.setText(chooser.getSelectedFile().getAbsolutePath());
					configuration.setAlternateSubtitlesFolder(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		subsfolder.append(folderSelectButton);

		builder.add(subsfolder._getPanel(), cc.xy(3, 6));

		OrientedSpanBuilder codepage = new OrientedSpanBuilder("p:grow, 6dlu, p");

		builder._addLabel(Messages.getString("MEncoderVideo.11"), cc.xy(1, 8));
		Object data[] = new Object[]{
			configuration.getSubtitlesCodepage(),
			Messages.getString("MEncoderVideo.96"),
			Messages.getString("MEncoderVideo.97"),
			Messages.getString("MEncoderVideo.98"),
			Messages.getString("MEncoderVideo.99"),
			Messages.getString("MEncoderVideo.100"),
			Messages.getString("MEncoderVideo.101"),
			Messages.getString("MEncoderVideo.102"),
			Messages.getString("MEncoderVideo.103"),
			Messages.getString("MEncoderVideo.104"),
			Messages.getString("MEncoderVideo.105"),
			Messages.getString("MEncoderVideo.106"),
			Messages.getString("MEncoderVideo.107"),
			Messages.getString("MEncoderVideo.108"),
			Messages.getString("MEncoderVideo.109"),
			Messages.getString("MEncoderVideo.110"),
			Messages.getString("MEncoderVideo.111"),
			Messages.getString("MEncoderVideo.112"),
			Messages.getString("MEncoderVideo.113"),
			Messages.getString("MEncoderVideo.114"),
			Messages.getString("MEncoderVideo.115"),
			Messages.getString("MEncoderVideo.116"),
			Messages.getString("MEncoderVideo.117"),
			Messages.getString("MEncoderVideo.118"),
			Messages.getString("MEncoderVideo.119"),
			Messages.getString("MEncoderVideo.120"),
			Messages.getString("MEncoderVideo.121"),
			Messages.getString("MEncoderVideo.122"),
			Messages.getString("MEncoderVideo.123"),
			Messages.getString("MEncoderVideo.124")
		};

		GuiUtil.MyComboBoxModel cbm = new GuiUtil.MyComboBoxModel(data);
		subtitleCodePage = new JComboBox(cbm);
		subtitleCodePage.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String s = (String) e.getItem();
					int offset = s.indexOf("/*");

					if (offset > -1) {
						s = s.substring(0, offset).trim();
					}

					configuration.setSubtitlesCodepage(s);
				}
			}
		});

		subtitleCodePage.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				subtitleCodePage.getItemListeners()[0].itemStateChanged(new ItemEvent(subtitleCodePage, 0, subtitleCodePage.getEditor().getItem(), ItemEvent.SELECTED));
			}
		});
		subtitleCodePage.setEditable(true);
		builder.orientLabelRenderer((JLabel)subtitleCodePage.getRenderer());
		codepage.append(subtitleCodePage);

		fribidi = new CustomJCheckBox(Messages.getString("MEncoderVideo.23"), configuration.isMencoderSubFribidi());
		fribidi.setContentAreaFilled(false);
		fribidi.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderSubFribidi(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		codepage.append(fribidi);

		builder.add(codepage._getPanel(), cc.xy(3, 8));

		OrientedSpanBuilder font = new OrientedSpanBuilder("p:grow, 1dlu, p");

		builder._addLabel(Messages.getString("MEncoderVideo.24"), cc.xy(1, 10));
		defaultfont = new JTextField(configuration.getFont());
		defaultfont.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setFont(defaultfont.getText());
			}
		});
		font.append(defaultfont);

		fontselect = new CustomJButton("...");
		fontselect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FontFileFilter());
				int returnVal = chooser.showDialog((Component) e.getSource(), Messages.getString("MEncoderVideo.25"));
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					defaultfont.setText(chooser.getSelectedFile().getAbsolutePath());
					configuration.setFont(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		font.append(fontselect);

		builder.add(font._getPanel(), cc.xy(3, 10));

		builder._addLabel(Messages.getString("MEncoderVideo.12"), cc.xy(1, 12));

		OrientedSpanBuilder assvars = new OrientedSpanBuilder("3*(p, 3dlu, p:grow, 6dlu), p, 3dlu, p:grow");

		ass_scale = new JTextField(configuration.getAssScale());
		ass_scale.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAssScale(ass_scale.getText());
			}
		});
		assvars.append(Messages.getString("MEncoderVideo.133"));
		assvars.append(ass_scale);

		ass_outline = new JTextField(configuration.getAssOutline());
		ass_outline.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAssOutline(ass_outline.getText());
			}
		});
		assvars.append(Messages.getString("MEncoderVideo.13"));
		assvars.append(ass_outline);

		ass_shadow = new JTextField(configuration.getAssShadow());
		ass_shadow.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAssShadow(ass_shadow.getText());
			}
		});
		assvars.append(Messages.getString("MEncoderVideo.14"));
		assvars.append(ass_shadow);

		ass_margin = new JTextField(configuration.getAssMargin());
		ass_margin.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setAssMargin(ass_margin.getText());
			}
		});
		assvars.append(Messages.getString("MEncoderVideo.15"));
		assvars.append(ass_margin);

		builder.add(assvars._getPanel(), cc.xy(3, 12/*, CellConstraints.FILL, CellConstraints.CENTER*/));

		autoloadExternalSubtitles = new CustomJCheckBox(Messages.getString("MEncoderVideo.22"), configuration.isAutoloadExternalSubtitles());
		autoloadExternalSubtitles.setToolTipText(Messages.getString("TrTab2.78"));
		autoloadExternalSubtitles.setContentAreaFilled(false);
		autoloadExternalSubtitles.setEnabled(!configuration.isForceExternalSubtitles());
		autoloadExternalSubtitles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAutoloadExternalSubtitles((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(autoloadExternalSubtitles, cc.xy(1, 14));

		subColor = new JButton();
		subColor.setText(Messages.getString("MEncoderVideo.31"));
		subColor.setBackground(new Color(configuration.getSubsColor()));
		subColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(
					looksFrame,
					Messages.getString("MEncoderVideo.125"),
					subColor.getBackground()
				);

				if (newColor != null) {
					subColor.setBackground(newColor);
					configuration.setSubsColor(newColor.getRGB());
					SubtitleUtils.deleteSubs(); // Color has been changed so all temporary subs will be deleted and make new
				}
			}
		});
		builder.add(subColor, cc.xy(3, 14));

		forceExternalSubtitles = new CustomJCheckBox(Messages.getString("TrTab2.87"), configuration.isForceExternalSubtitles());
		forceExternalSubtitles.setToolTipText(Messages.getString("TrTab2.88"));
		forceExternalSubtitles.setContentAreaFilled(false);
		forceExternalSubtitles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setForceExternalSubtitles((e.getStateChange() == ItemEvent.SELECTED));
				if (configuration.isForceExternalSubtitles()) {
					autoloadExternalSubtitles.setSelected(true);
				}
				autoloadExternalSubtitles.setEnabled(!configuration.isForceExternalSubtitles());
			}
		});
		builder.add(forceExternalSubtitles, cc.xy(1, 16));

		useEmbeddedSubtitlesStyle = new CustomJCheckBox(Messages.getString("MEncoderVideo.36"), configuration.isUseEmbeddedSubtitlesStyle());
		useEmbeddedSubtitlesStyle.setToolTipText(Messages.getString("TrTab2.89"));
		useEmbeddedSubtitlesStyle.setContentAreaFilled(false);
		useEmbeddedSubtitlesStyle.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setUseEmbeddedSubtitlesStyle(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(useEmbeddedSubtitlesStyle, cc.xy(1, 18));

		builder._addLabel(Messages.getString("TrTab2.90"), cc.xy(1, 20));
		depth3D = new JTextField(configuration.getDepth3D());
		depth3D.setToolTipText(Messages.getString("TrTab2.91"));
		depth3D.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setDepth3D(depth3D.getText());
			}
		});
		builder.add(depth3D, cc.xy(3, 20));

		final JPanel panel = builder._getPanel();

		GuiUtil.enableContainer(panel, !configuration.isDisableSubtitles());
		disableSubs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// If "Disable Subtitles" is not selected, subtitles are enabled
				GuiUtil.enableContainer(panel, e.getStateChange() != ItemEvent.SELECTED);
			}
		});

		return panel;
	}
}
