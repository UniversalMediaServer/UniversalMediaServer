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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.util.FormLayoutUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;

public class TranscodingTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(TranscodingTab.class);
	private static final String COMMON_COL_SPEC = "left:pref, 2dlu, pref:grow";
	private static final String COMMON_ROW_SPEC = "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p";
	private static final String EMPTY_COL_SPEC = "left:pref, 2dlu, pref:grow";
	private static final String EMPTY_ROW_SPEC = "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p , 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 20dlu, p, 3dlu, p, 3dlu, p";
	private static final String LEFT_COL_SPEC = "left:pref, pref, pref, pref, 0:grow";
	private static final String LEFT_ROW_SPEC = "fill:10:grow, 3dlu, p, 3dlu, p, 3dlu, p";
	private static final String MAIN_COL_SPEC = "left:pref, pref, 7dlu, pref, pref, fill:10:grow";
	private static final String MAIN_ROW_SPEC = "fill:10:grow";

	private final PmsConfiguration configuration;
	private ComponentOrientation orientation;

	TranscodingTab(PmsConfiguration configuration) {
		this.configuration = configuration;
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		orientation = ComponentOrientation.getOrientation(locale);
	}
	private JCheckBox disableSubs;

	public JCheckBox getDisableSubs() {
		return disableSubs;
	}
	private JTextField forcetranscode;
	private JTextField notranscode;
	private JTextField maxbuffer;
	private JComboBox nbcores;
	private DefaultMutableTreeNode parent[];
	private JPanel tabbedPane;
	private CardLayout cl;
	private JTextField abitrate;
	private JTree tree;
	private JCheckBox forcePCM;
	private JCheckBox forceDTSinPCM;
	private JComboBox channels;
	private JComboBox vq;
	private JCheckBox ac3remux;
	private JCheckBox mpeg2remux;
	private JCheckBox chapter_support;
	private JTextField chapter_interval;

	/*
	 * 16 cores is the maximum allowed by MEncoder as of MPlayer r34863.
	 * Revisions before that allowed only 8.
	 */
	private static final int MAX_CORES = 16;

	private void updateEngineModel() {
		ArrayList<String> engines = new ArrayList<String>();
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
		tabbedPane.setPreferredSize(component.getPreferredSize());
		tabbedPane.getParent().invalidate();
		tabbedPane.getParent().validate();
		tabbedPane.getParent().repaint();
	}

	public JComponent build() {
		String colSpec = FormLayoutUtil.getColSpec(MAIN_COL_SPEC, orientation);
		FormLayout mainlayout = new FormLayout(colSpec, MAIN_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(mainlayout);
		builder.setBorder(Borders.DLU4_BORDER);

		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();
		builder.add(buildRightTabbedPane(), FormLayoutUtil.flip(cc.xyw(4, 1, 3), colSpec, orientation));
		builder.add(buildLeft(), FormLayoutUtil.flip(cc.xy(2, 1), colSpec, orientation));

		JPanel panel = builder.getPanel();
		
		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public JComponent buildRightTabbedPane() {
		cl = new CardLayout();
		tabbedPane = new JPanel(cl);
		tabbedPane.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane scrollPane = new JScrollPane(tabbedPane);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	public JComponent buildLeft() {
		String colSpec = FormLayoutUtil.getColSpec(LEFT_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, LEFT_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JButton but = new JButton(LooksFrame.readImageIcon("kdevelop_down-32.png"));
		but.setToolTipText(Messages.getString("TrTab2.6"));
		but.addActionListener(new ActionListener() {
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
		builder.add(but, FormLayoutUtil.flip(cc.xy(2, 3), colSpec, orientation));

		JButton but2 = new JButton(LooksFrame.readImageIcon("up-32.png"));
		but2.setToolTipText(Messages.getString("TrTab2.6"));
		but2.addActionListener(new ActionListener() {
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
		builder.add(but2, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		JButton but3 = new JButton(LooksFrame.readImageIcon("connect_no-32.png"));
		but3.setToolTipText(Messages.getString("TrTab2.0"));
		but3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings && ((TreeNodeSettings) path.getLastPathComponent()).getPlayer() != null) {
					((TreeNodeSettings) path.getLastPathComponent()).setEnable(!((TreeNodeSettings) path.getLastPathComponent()).isEnable());
					updateEngineModel();
					tree.updateUI();
				}
			}
		});
		builder.add(but3, FormLayoutUtil.flip(cc.xy(4, 3), colSpec, orientation));

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(Messages.getString("TrTab2.11"));
		TreeNodeSettings commonEnc = new TreeNodeSettings(Messages.getString("TrTab2.5"), null, buildCommon());
		commonEnc.getConfigPanel().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				handleCardComponentChange(e.getComponent());
			}
		});
		tabbedPane.add(commonEnc.id(), commonEnc.getConfigPanel());
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
					cl.show(tabbedPane, tns.id());
				}
			}
		});

		tree.setCellRenderer(new TreeRenderer());
		JScrollPane pane = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		builder.add(pane, FormLayoutUtil.flip(cc.xyw(2, 1, 4), colSpec, orientation));

		builder.addLabel(Messages.getString("TrTab2.19"), FormLayoutUtil.flip(cc.xyw(2, 5, 4), colSpec, orientation));
		builder.addLabel(Messages.getString("TrTab2.20"), FormLayoutUtil.flip(cc.xyw(2, 7, 4), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public void addEngines() {
		ArrayList<Player> disPlayers = new ArrayList<Player>();
		ArrayList<Player> ordPlayers = new ArrayList<Player>();
		PMS r = PMS.get();

		for (String id : configuration.getEnginesAsList(r.getRegistry())) {
			//boolean matched = false;
			for (Player p : PlayerFactory.getAllPlayers()) {
				if (p.id().equals(id)) {
					ordPlayers.add(p);
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
			TreeNodeSettings en = new TreeNodeSettings(p.name(), p, null);
			if (disPlayers.contains(p)) {
				en.setEnable(false);
			}
			JComponent jc = en.getConfigPanel();
			if (jc == null) {
				jc = buildEmpty();
			}
			jc.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					handleCardComponentChange(e.getComponent());
				}
			});
			tabbedPane.add(en.id(), jc);
			parent[p.purpose()].add(en);
		}

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}


		tree.setSelectionRow(0);
	}

	public JComponent buildEmpty() {
		String colSpec = FormLayoutUtil.getColSpec(EMPTY_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, EMPTY_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getString("TrTab2.1"), FormLayoutUtil.flip(cc.xyw(1, 1, 3), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	public JComponent buildCommon() {
		String colSpec = FormLayoutUtil.getColSpec(COMMON_COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, COMMON_ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		maxbuffer = new JTextField("" + configuration.getMaxMemoryBufferSize());
		maxbuffer.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

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

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(1, 1, 3), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.addLabel(Messages.getString("TrTab2.23").replaceAll("MAX_BUFFER_SIZE", configuration.getMaxMemoryBufferSizeStr()), FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));
		builder.add(maxbuffer, FormLayoutUtil.flip(cc.xy(3, 3), colSpec, orientation));

		builder.addLabel(Messages.getString("TrTab2.24") + Runtime.getRuntime().availableProcessors() + ")", FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));

		String[] guiCores = new String[MAX_CORES];
		for (int i = 0; i < MAX_CORES; i++) {
			guiCores[i] = Integer.toString(i + 1);
		}
		nbcores = new JComboBox(guiCores);
		nbcores.setEditable(false);
		int nbConfCores = configuration.getNumberOfCpuCores();
		if (nbConfCores > 0 && nbConfCores <= MAX_CORES) {
			nbcores.setSelectedItem(Integer.toString(nbConfCores));
		} else {
			nbcores.setSelectedIndex(0);
		}

		nbcores.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
			        configuration.setNumberOfCpuCores(Integer.parseInt(e.getItem().toString()));
			}
		});
		builder.add(nbcores, FormLayoutUtil.flip(cc.xy(3, 5), colSpec, orientation));

		chapter_interval = new JTextField("" + configuration.getChapterInterval());
		chapter_interval.setEnabled(configuration.isChapterSupport());
		chapter_interval.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

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

		chapter_support = new JCheckBox(Messages.getString("TrTab2.52"));
		chapter_support.setContentAreaFilled(false);
		chapter_support.setSelected(configuration.isChapterSupport());

		chapter_support.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setChapterSupport((e.getStateChange() == ItemEvent.SELECTED));
				chapter_interval.setEnabled(configuration.isChapterSupport());
			}
		});

		builder.add(chapter_support, FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));

		builder.add(chapter_interval, FormLayoutUtil.flip(cc.xy(3, 7), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("TrTab2.3"), FormLayoutUtil.flip(cc.xyw(1, 11, 3), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		channels = new JComboBox(new Object[]{Messages.getString("TrTab2.55"),  Messages.getString("TrTab2.56") /*, "8 channels 7.1" */}); // 7.1 not supported by MPlayer :\
		channels.setEditable(false);
		if (configuration.getAudioChannelCount() == 2) {
			channels.setSelectedIndex(0);
		} else {
			channels.setSelectedIndex(1);
		}
		channels.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioChannelCount(Integer.parseInt(e.getItem().toString().substring(0, 1)));
			}
		});

		builder.addLabel(Messages.getString("TrTab2.50"), FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
		builder.add(channels, FormLayoutUtil.flip(cc.xy(3, 13), colSpec, orientation));

		abitrate = new JTextField("" + configuration.getAudioBitrate());
		abitrate.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

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

		builder.addLabel(Messages.getString("TrTab2.29"), FormLayoutUtil.flip(cc.xy(1, 15), colSpec, orientation));
		builder.add(abitrate, FormLayoutUtil.flip(cc.xy(3, 15), colSpec, orientation));

		forceDTSinPCM = new JCheckBox(Messages.getString("TrTab2.28") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		forceDTSinPCM.setContentAreaFilled(false);
		if (configuration.isDTSEmbedInPCM()) {
			forceDTSinPCM.setSelected(true);
		}
		forceDTSinPCM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setDTSEmbedInPCM(forceDTSinPCM.isSelected());
				if (configuration.isDTSEmbedInPCM()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("TrTab2.10"),
						"Information",
						JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});

		builder.add(forceDTSinPCM, FormLayoutUtil.flip(cc.xyw(1, 17, 3), colSpec, orientation));

		forcePCM = new JCheckBox(Messages.getString("TrTab2.27"));
		forcePCM.setContentAreaFilled(false);
		if (configuration.isMencoderUsePcm()) {
			forcePCM.setSelected(true);
		}
		forcePCM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderUsePcm(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		builder.add(forcePCM, FormLayoutUtil.flip(cc.xyw(1, 19, 3), colSpec, orientation));

		ac3remux = new JCheckBox(Messages.getString("MEncoderVideo.32") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		ac3remux.setContentAreaFilled(false);
		if (configuration.isRemuxAC3()) {
			ac3remux.setSelected(true);
		}
		ac3remux.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setRemuxAC3((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builder.add(ac3remux, FormLayoutUtil.flip(cc.xyw(1, 21, 3), colSpec, orientation));

		mpeg2remux = new JCheckBox(Messages.getString("MEncoderVideo.39") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		mpeg2remux.setContentAreaFilled(false);
		if (configuration.isMencoderRemuxMPEG2()) {
			mpeg2remux.setSelected(true);
		}
		mpeg2remux.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderRemuxMPEG2((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builder.add(mpeg2remux, FormLayoutUtil.flip(cc.xyw(1, 23, 3), colSpec, orientation));

		cmp = builder.addSeparator(Messages.getString("TrTab2.4"), FormLayoutUtil.flip(cc.xyw(1, 25, 3), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.addLabel(Messages.getString("TrTab2.32"), FormLayoutUtil.flip(cc.xyw(1, 29, 3), colSpec, orientation));

		Object data[] = new Object[] {
			configuration.getMencoderMainSettings(),                                                /* default */
			String.format("keyint=5:vqscale=1:vqmin=2  /* %s */", Messages.getString("TrTab2.60")), /* great */
			String.format("keyint=5:vqscale=1:vqmin=1  /* %s */", Messages.getString("TrTab2.61")), /* lossless */
			String.format("keyint=5:vqscale=2:vqmin=3  /* %s */", Messages.getString("TrTab2.62")), /* good (wired) */
			String.format("keyint=25:vqmax=5:vqmin=2  /* %s */",  Messages.getString("TrTab2.63")), /* good (wireless) */
			String.format("keyint=25:vqmax=7:vqmin=2  /* %s */",  Messages.getString("TrTab2.64")), /* medium (wireless) */
			String.format("keyint=25:vqmax=8:vqmin=3  /* %s */",  Messages.getString("TrTab2.65"))  /* low */
		};

		MyComboBoxModel cbm = new MyComboBoxModel(data);

		vq = new JComboBox(cbm);
		vq.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String s = (String) e.getItem();
					if (s.indexOf("/*") > -1) {
						s = s.substring(0, s.indexOf("/*")).trim();
					}
					configuration.setMencoderMainSettings(s);
				}
			}
		});
		vq.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				vq.getItemListeners()[0].itemStateChanged(new ItemEvent(vq, 0, vq.getEditor().getItem(), ItemEvent.SELECTED));
			}
		});
		vq.setEditable(true);
		builder.add(vq, FormLayoutUtil.flip(cc.xyw(1, 31, 3), colSpec, orientation));

		String help1 = Messages.getString("TrTab2.39");
		help1 += Messages.getString("TrTab2.40");
		help1 += Messages.getString("TrTab2.41");
		help1 += Messages.getString("TrTab2.42");
		help1 += Messages.getString("TrTab2.43");
		help1 += Messages.getString("TrTab2.44");

		JTextArea decodeTips = new JTextArea(help1);
		decodeTips.setEditable(false);
		decodeTips.setBorder(BorderFactory.createEtchedBorder());
		decodeTips.setBackground(new Color(255, 255, 192));
		builder.add(decodeTips, FormLayoutUtil.flip(cc.xyw(1, 41, 3), colSpec, orientation));

		disableSubs = new JCheckBox(Messages.getString("TrTab2.51"));
		disableSubs.setContentAreaFilled(false);

		cmp = builder.addSeparator(Messages.getString("TrTab2.7"), FormLayoutUtil.flip(cc.xyw(1, 33, 3), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.add(disableSubs, FormLayoutUtil.flip(cc.xy(1, 35), colSpec, orientation));

		builder.addLabel(Messages.getString("TrTab2.8"), FormLayoutUtil.flip(cc.xy(1, 37), colSpec, orientation));

		notranscode = new JTextField(configuration.getNoTranscode());
		notranscode.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setNoTranscode(notranscode.getText());
			}
		});
		builder.add(notranscode, FormLayoutUtil.flip(cc.xy(3, 37), colSpec, orientation));

		builder.addLabel(Messages.getString("TrTab2.9"), FormLayoutUtil.flip(cc.xy(1, 39), colSpec, orientation));

		forcetranscode = new JTextField(configuration.getForceTranscode());
		forcetranscode.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForceTranscode(forcetranscode.getText());
			}
		});
		builder.add(forcetranscode, FormLayoutUtil.flip(cc.xy(3, 39), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}
}
