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
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultTreeModel;
import static javax.swing.ScrollPaneConstants.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.NetworkConfiguration;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.components.DefaultTextField;
import net.pms.newgui.components.IntegerDocumentFilter;
import net.pms.newgui.components.NICTreeManager;
import net.pms.newgui.components.NICTreeNode;
import net.pms.newgui.components.SearchableMutableTreeNode;
import net.pms.util.FormLayoutUtil;
import net.pms.util.KeyedComboBoxModel;
import net.pms.util.WindowsUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTab.class);

	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu , p, 3dlu, p, 3dlu, pref:grow";
	private static final String ROW_SPEC = "p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p";

	public JCheckBox smcheckBox;
	private JCheckBox autoStart;
	private JCheckBox autoUpdateCheckBox;
	private JCheckBox hideAdvancedOptions;
	private JCheckBox newHTTPEngine;
	private JCheckBox preventSleep;
	private JCheckBox networkNameResolution;
	private DefaultTextField hostname;
	private DefaultTextField port;
	private DefaultTextField webPort;
	private JTextField serverName;
	private JTextField ip_filter;
	public JTextField maxbitrate;
	private JCheckBox adaptBitrate;
	private JComboBox<String> renderers;
	private final PmsConfiguration configuration;
	private JCheckBox fdCheckBox;
	private JCheckBox extNetBox;
	private JCheckBox appendProfileName;
	private JCheckBox runWizardOnProgramStartup;
	private LooksFrame looksFrame;
	private JCheckBox singleInstance;
	private CustomJButton installService;
	private JCheckBox showSplashScreen;
	private JTextField currentLanguage = new JTextField();

	GeneralTab(PmsConfiguration configuration, LooksFrame looksFrame) {
		this.configuration = configuration;
		this.looksFrame = looksFrame;
	}

	public JComponent build() {
		// count the lines easier to add new ones
		int ypos = 1;
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		smcheckBox = new JCheckBox(Messages.getString("NetworkTab.3"), configuration.isMinimized());
		smcheckBox.setContentAreaFilled(false);
		smcheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setMinimized((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		ypos = 7; // we hardcode here (promise last time)
		builder.addLabel(Messages.getString("GeneralTab.14"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new BoxLayout(languagePanel,BoxLayout.LINE_AXIS));
		currentLanguage.setEnabled(false);
		currentLanguage.setText(Messages.getString("Language." + configuration.getLanguageTag()));
		languagePanel.add(currentLanguage);
		CustomJButton selectLanguage = new CustomJButton("    ...    ");
		selectLanguage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageSelection selectionDialog = new LanguageSelection(looksFrame, configuration.getLanguageLocale(), true);
				if (selectionDialog != null) {
					selectionDialog.show();
					if (!selectionDialog.isAborted()) {
						currentLanguage.setText(Messages.getString("Language." + configuration.getLanguageTag()));
					}
				}
			}
		});
		languagePanel.add(selectLanguage);
		builder.add(languagePanel, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			serverName = new JTextField(configuration.getServerName());
			serverName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setServerName(serverName.getText());
				}
			});
			builder.addLabel(Messages.getString("NetworkTab.71"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(serverName, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			appendProfileName = new JCheckBox(Messages.getString("NetworkTab.72"), configuration.isAppendProfileName());
			appendProfileName.setToolTipText(Messages.getString("NetworkTab.73"));
			appendProfileName.setContentAreaFilled(false);
			appendProfileName.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setAppendProfileName((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(GuiUtil.getPreferredSizeComponent(appendProfileName), FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
			ypos += 2;
		}

		builder.add(smcheckBox, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		if (Platform.isWindows()) {
			autoStart = new JCheckBox(Messages.getString("NetworkTab.57"), configuration.isAutoStart());
			autoStart.setContentAreaFilled(false);
			autoStart.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setAutoStart((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(GuiUtil.getPreferredSizeComponent(autoStart), FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
		}

		showSplashScreen = new JCheckBox(Messages.getString("NetworkTab.74"), configuration.isShowSplashScreen());
		showSplashScreen.setContentAreaFilled(false);
		showSplashScreen.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setShowSplashScreen((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builder.add(GuiUtil.getPreferredSizeComponent(showSplashScreen), FormLayoutUtil.flip(cc.xy(5, ypos), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			installService = new CustomJButton();
			refreshInstallServiceButtonState();

			builder.add(installService, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;
		}

		CustomJButton checkForUpdates = new CustomJButton(Messages.getString("NetworkTab.8"));
		checkForUpdates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				looksFrame.checkForUpdates(false);
			}
		});
		builder.add(checkForUpdates, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		autoUpdateCheckBox = new JCheckBox(Messages.getString("NetworkTab.9"), configuration.isAutoUpdate());
		autoUpdateCheckBox.setContentAreaFilled(false);
		autoUpdateCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAutoUpdate((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(autoUpdateCheckBox), FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		if (!Build.isUpdatable()) {
			checkForUpdates.setEnabled(false);
			autoUpdateCheckBox.setEnabled(false);
		}

		hideAdvancedOptions = new JCheckBox(Messages.getString("NetworkTab.61"), configuration.isHideAdvancedOptions());
		hideAdvancedOptions.setContentAreaFilled(false);
		hideAdvancedOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setHideAdvancedOptions(hideAdvancedOptions.isSelected());
				if (hideAdvancedOptions.isSelected()) {
					looksFrame.setViewLevel(ViewLevel.NORMAL);
				} else {
					looksFrame.setViewLevel(ViewLevel.ADVANCED);
				}
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(hideAdvancedOptions), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		runWizardOnProgramStartup = new JCheckBox(Messages.getString("GeneralTab.9"), configuration.isRunWizard());
		runWizardOnProgramStartup.setContentAreaFilled(false);
		runWizardOnProgramStartup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setRunWizard(runWizardOnProgramStartup.isSelected());
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(runWizardOnProgramStartup), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			singleInstance = new JCheckBox(Messages.getString("GeneralTab.10"), configuration.isRunSingleInstance());
			singleInstance.setContentAreaFilled(false);
			singleInstance.setToolTipText(Messages.getString("GeneralTab.11"));
			singleInstance.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					configuration.setRunSingleInstance(singleInstance.isSelected());
				}
			});
			builder.add(GuiUtil.getPreferredSizeComponent(singleInstance), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
		}

		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getEnabledRenderersConfigurations();
		ArrayList<Object> keyValues = new ArrayList<>();
		ArrayList<Object> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));

		if (allConfs != null) {
			sortRendererConfigurationsByName(allConfs);
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel<String, String> renderersKcbm = new KeyedComboBoxModel<>(
			keyValues.toArray(new String[keyValues.size()]),
			nameValues.toArray(new String[nameValues.size()]));
		renderers = new JComboBox<>(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedKey(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		if (!configuration.isHideAdvancedOptions()) {
			// Edit UMS configuration file manually
			CustomJButton confEdit = new CustomJButton(Messages.getString("NetworkTab.51"));
			confEdit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JPanel tPanel = new JPanel(new BorderLayout());
					final File conf = new File(configuration.getProfilePath());
					final JTextArea textArea = new JTextArea();
					textArea.setFont(new Font("Courier", Font.PLAIN, 12));
					JScrollPane scrollPane = new JScrollPane(textArea);
					scrollPane.setPreferredSize(new Dimension(900, 450));

					try {
						try (FileInputStream fis = new FileInputStream(conf); BufferedReader in = new BufferedReader(new InputStreamReader(fis))) {
							String line;
							StringBuilder sb = new StringBuilder();

							while ((line = in.readLine()) != null) {
								sb.append(line);
								sb.append("\n");
							}
							textArea.setText(sb.toString());
						}
					} catch (IOException e1) {
						return;
					}

					tPanel.add(scrollPane, BorderLayout.NORTH);
					Object[] options = {Messages.getString("LooksFrame.9"), Messages.getString("NetworkTab.45")};

					if (JOptionPane.showOptionDialog(looksFrame,
						tPanel, Messages.getString("NetworkTab.51"),
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, options, null) == JOptionPane.OK_OPTION) {
						String text = textArea.getText();

						try {
							try (FileOutputStream fos = new FileOutputStream(conf)) {
								fos.write(text.getBytes());
								fos.flush();
							}
							configuration.reload();
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(looksFrame, Messages.getString("NetworkTab.52") + e1.toString());
						}
					}
				}
			});
			builder.add(confEdit, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;

			ypos = buildNetworkConfiguration(builder, ypos, colSpec, orientation);
			cmp = builder.addSeparator(Messages.getString("NetworkTab.31"), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			newHTTPEngine = new JCheckBox(Messages.getString("NetworkTab.32"), configuration.isHTTPEngineV2());
			newHTTPEngine.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setHTTPEngineV2((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(newHTTPEngine, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

			if (Platform.isWindows()) {
				preventSleep = new JCheckBox(Messages.getString("NetworkTab.33"), configuration.isPreventsSleep());
				preventSleep.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						configuration.setPreventsSleep((e.getStateChange() == ItemEvent.SELECTED));
					}
				});
				builder.add(preventSleep, FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			}
			ypos += 2;

			final SelectRenderers selectRenderers = new SelectRenderers();

			builder.addLabel(Messages.getString("NetworkTab.62"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			final CustomJButton setRenderers = new CustomJButton(Messages.getString("GeneralTab.5"));
			setRenderers.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectRenderers.showDialog();
				}
			});

			builder.add(setRenderers, FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			ypos += 2;

			builder.addLabel(Messages.getString("NetworkTab.36"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

			builder.add(renderers, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			fdCheckBox = new JCheckBox(Messages.getString("NetworkTab.38"), configuration.isRendererForceDefault());
			fdCheckBox.setContentAreaFilled(false);
			fdCheckBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setRendererForceDefault((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(fdCheckBox, FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));

			ypos += 2;

			// External network box
			extNetBox = new JCheckBox(Messages.getString("NetworkTab.56"), configuration.getExternalNetwork());
			extNetBox.setToolTipText(Messages.getString("NetworkTab.67"));
			extNetBox.setContentAreaFilled(false);
			extNetBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setExternalNetwork((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(extNetBox, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		// Make "Enter" move to the next field
		Set<AWTKeyStroke> forwardKeys = new HashSet<>(panel.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
		forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		panel.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private int buildNetworkConfiguration(PanelBuilder builder, int ypos, String colSpec, ComponentOrientation orientation) {
		CellConstraints cc = new CellConstraints();

		JPanel networkPanel = new JPanel(new FormLayout(
			FormLayoutUtil.getColSpec("fill:p:noGrow, fill:4dlu:noGrow, fill:p:grow", orientation),
			"pref, fill:3dlu:noGrow, fill:min:grow"
		));
		TitledBorder titledBorder = new TitledBorder(new EtchedBorder(), Messages.getString("NetworkTab.22"), TitledBorder.LEADING, TitledBorder.CENTER);
		titledBorder.setTitleFont(networkPanel.getFont().deriveFont(Font.BOLD));
		CompoundBorder compoundBorder = new CompoundBorder(titledBorder, new EmptyBorder(5, 5, 5, 5));
		networkPanel.setBorder(compoundBorder);

		NICTreeNode rootNode = new NICTreeNode(Messages.getString("General.All"));

		for (NetworkInterface networkInterface : NetworkConfiguration.get().getRelevantNetworkInterfaces()) {
			SearchableMutableTreeNode node = rootNode.findChild(networkInterface.getName());
			if (node == null) {
				NICTreeNode newNode = new NICTreeNode(networkInterface);
				rootNode.add(newNode);
				// Add IP address leaves
				/*for (InetAddress address : NetworkConfiguration.get().getRelevantInterfaceAddresses(networkInterface)) {
					newNode.add(new NICTreeNode(address, false));
				}*/

				//TEMP Test
				for (InetAddress address : NetworkConfiguration.get().getRelevantInterfaceAddresses(networkInterface)) {
					NICTreeNode newSubNode = new NICTreeNode(address, true);
					newNode.add(newSubNode);
					int to = 1 + (int) Math.round(Math.random() * 4);
					for (int i = 0; i < to; i++) {
						try {
							newSubNode.add(new NICTreeNode(InetAddress.getByName("192.168.31." + Integer.toString(1 + (int) Math.round(Math.random() * 254))), false));
						} catch (UnknownHostException e1) {
						}
					}
				}
				//
			} else {
				LOGGER.warn("Network interface \"{}\" found twice, ignoring repeated entry", networkInterface.getDisplayName());
			}
		}

		JTree addressTree = new JTree(new DefaultTreeModel(rootNode));
		addressTree.setRowHeight(0);
		addressTree.setShowsRootHandles(true);
		NICTreeManager nicTreeManager = new NICTreeManager(addressTree);
		nicTreeManager.getSelectionModel().clearSelection();
		for (int i = 0; i < addressTree.getRowCount(); i++) {
			addressTree.expandRow(i);
		}
		addressTree.validate();

		JPanel networkLeftPanel = new JPanel();
		networkLeftPanel.setLayout(new FormLayout(
			FormLayoutUtil.getColSpec("fill:p:noGrow, fill:3dlu:noGrow, left:[50dlu,p]:noGrow", orientation),
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow, center:3dlu:noGrow, " +
			"center:p:noGrow"
		));

		// Use name instead of IP?
		JLabel networkNameResolutionLabel = new JLabel(Messages.getString("NetworkConfiguration.UseNameResolution"));
		networkNameResolutionLabel.setToolTipText(Messages.getString("NetworkConfiguration.UseNameResolutionHint"));
		networkLeftPanel.add(networkNameResolutionLabel, "1, 1");
		networkNameResolution = new JCheckBox();
		networkNameResolution.setToolTipText(Messages.getString("NetworkConfiguration.UseNameResolutionHint"));
		networkNameResolution.setSelected(configuration.isNetworkNameResolution()); //TODO: handle in method
		//TODO: Listener
		networkLeftPanel.add(networkNameResolution, "3, 1");

		// Override hostname
		networkLeftPanel.add(new JLabel(Messages.getString("NetworkConfiguration.Hostname")), "1, 3");
		hostname = new DefaultTextField(configuration.getServerHostname(), NetworkConfiguration.getDefaultHostName());
		hostname.setColumns(10);
		hostname.setEnabled(configuration.isNetworkNameResolution()); //TODO: handle in method
		//TODO: ChangeListener?
		hostname.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setHostname(hostname.getText());
			}
		});
		networkLeftPanel.add(hostname, "3, 3");

		// Override port
		JLabel portLabel = new JLabel(Messages.getString("NetworkConfiguration.Port"));
		//portLabel.setToolTipText(Messages.getString(""));
		networkLeftPanel.add(portLabel, "1, 5");
		port = new DefaultTextField(configuration.getServerPortString(), 10, Integer.toString(configuration.getServerDefaultPort()), true);
		AbstractDocument document = (AbstractDocument) port.getDocument();
		document.setDocumentFilter(new IntegerDocumentFilter(1, 65536, true, port));
		port.setToolTipText(Messages.getString("NetworkTab.64"));
		/*port.addKeyListener(new KeyAdapter() {
			@Override
			//TODO: ChangeListener?
			public void keyReleased(KeyEvent e) {
				try {
					String p = port.getText();
					if (StringUtils.isEmpty(p)) {
						p = "5001";
					}
					int ab = Integer.parseInt(p);
					configuration.setServerPort(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse port from \"" + port.getText() + "\"");
				}

			}
		});*/
		port.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				LOGGER.error("Text changed to: {}", ((DefaultTextField) e.getSource()).getText());

			}
		});
		networkLeftPanel.add(port, "3, 5");

		// Override web interface port
		JLabel webPortLabel = new JLabel(Messages.getString("NetworkConfiguration.WebPort"));
		//webPortLabel.setToolTipText(Messages.getString(""));
		networkLeftPanel.add(webPortLabel, "1, 7");
		webPort = new DefaultTextField(configuration.getWebPortString(), Integer.toString(configuration.getWebDefaultPort()));
		webPort.setColumns(10);
		document = (AbstractDocument) webPort.getDocument();
		document.setDocumentFilter(new IntegerDocumentFilter(1, 65536, true));
		document.addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				LOGGER.info("Update: {}", e.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				LOGGER.info("Update: {}", e.getDocument());

			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				LOGGER.info("Update: {}", e.getDocument());

			}
		});
		//webPort.setToolTipText(Messages.getString("NetworkTab.64"));
		webPort.addKeyListener(new KeyAdapter() {
			@Override
			//TODO: ChangeListener?
			public void keyReleased(KeyEvent e) {
				try {
					String p = webPort.getText();
					if (StringUtils.isEmpty(p)) {
						p = "9001";
					}
					int ab = Integer.parseInt(p);
					//configuration.setWebPort(ab);
				} catch (NumberFormatException nfe) {
					LOGGER.debug("Could not parse web interface port from \"{}\"", webPort.getText());
				}

			}
		});
		networkLeftPanel.add(webPort, "3, 7");

		networkLeftPanel.add(new JLabel(Messages.getString("GeneralTab.12")), "1, 9");
		adaptBitrate = new JCheckBox();
		adaptBitrate.setSelected(configuration.isAutomaticMaximumBitrate());
		adaptBitrate.setContentAreaFilled(false);
		adaptBitrate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setAutomaticMaximumBitrate(adaptBitrate.isSelected());
				maxbitrate.setEnabled(!configuration.isAutomaticMaximumBitrate());
			}
		});
		networkLeftPanel.add(adaptBitrate, "3, 9");

		networkLeftPanel.add(new JLabel(Messages.getString("NetworkTab.35")), "1, 11");
		maxbitrate = new JTextField(configuration.getMaximumBitrateDisplay());
		maxbitrate.setColumns(10);
		maxbitrate.setToolTipText(Messages.getString("NetworkTab.65"));
		maxbitrate.addKeyListener(new KeyAdapter() {
			//TODO: ChangeListener?
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setMaximumBitrate(maxbitrate.getText());
			}
		});
		if (configuration.isAutomaticMaximumBitrate()) {
			maxbitrate.setEnabled(false);
		} else {
			maxbitrate.setEnabled(true);
		}
		networkLeftPanel.add(maxbitrate, "3, 11");

		networkLeftPanel.add(new JLabel(Messages.getString("NetworkTab.30")), "1, 13");
		ip_filter = new JTextField(configuration.getIpFilter());
		ip_filter.setColumns(10);
		ip_filter.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setIpFilter(ip_filter.getText());
			}
		});
		networkLeftPanel.add(ip_filter, "3, 13");

		networkPanel.add(networkLeftPanel, "1, 1, 1, 3");
		networkPanel.add(new JLabel(Messages.getString("NetworkConfiguration.BindTo")), "3, 1");

		JScrollPane addressPane = new JScrollPane(addressTree, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		networkPanel.add(addressPane, "3, 3");

		builder.add(networkPanel, cc.xyw(1, ypos, 9));

		//builder.addLabel(Messages.getString("NetworkTab.23"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
		//builder.add(hostname, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		//builder.addLabel(Messages.getString("NetworkTab.24"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
		//builder.add(port, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		//builder.addLabel(Messages.getString("NetworkTab.30"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
		//builder.add(ip_filter, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		//builder.addLabel(Messages.getString("NetworkTab.35"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
		//builder.add(maxbitrate, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));
		//builder.add(GuiUtil.getPreferredSizeComponent(adaptBitrate), FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
		return ypos + 2;
	}

	/**
	 * Refreshes the state of the button to install/uninstall the Windows service for UMS
	 * depending if the service has been installed or not.
	 *  - Set the button and tooltip text
	 *  - Add the correct action listener
	 */
	private void refreshInstallServiceButtonState() {
		if (System.getProperty(LooksFrame.START_SERVICE) != null || !Platform.isWindows()) {
			installService.setEnabled(false);
			installService.setText(Messages.getString("NetworkTab.4"));
		} else {
			installService.setEnabled(true);

			boolean isUmsServiceInstalled = WindowsUtil.isUmsServiceInstalled();

			if (isUmsServiceInstalled) {
				// Update button text and tooltip
				installService.setText(Messages.getString("GeneralTab.2"));
				installService.setToolTipText(null);

				// Remove all attached action listeners
				for (ActionListener al : installService.getActionListeners()) {
					installService.removeActionListener(al);
				}

				// Attach the button clicked action listener
				installService.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						WindowsUtil.uninstallWin32Service();
						LOGGER.info("Uninstalled UMS Windows service");

						// Refresh the button state after it has been clicked
						refreshInstallServiceButtonState();

						JOptionPane.showMessageDialog(
							looksFrame,
							Messages.getString("GeneralTab.3"),
							Messages.getString("Dialog.Information"),
							JOptionPane.INFORMATION_MESSAGE
						);
					}
				});
			} else {
				// Update button text and tooltip
				installService.setText(Messages.getString("NetworkTab.4"));
				installService.setToolTipText(Messages.getString("NetworkTab.63"));

				// Remove all attached action listeners
				for (ActionListener al : installService.getActionListeners()) {
					installService.removeActionListener(al);
				}

				// Attach the button clicked action listener
				installService.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (WindowsUtil.installWin32Service()) {
							LOGGER.info("Installed UMS Windows service");

							// Refresh the button state after it has been clicked
							refreshInstallServiceButtonState();

							JOptionPane.showMessageDialog(
								looksFrame,
								Messages.getString("NetworkTab.11") +
								Messages.getString("NetworkTab.12"),
								Messages.getString("Dialog.Information"),
								JOptionPane.INFORMATION_MESSAGE
							);
						} else {
							JOptionPane.showMessageDialog(
								looksFrame,
								Messages.getString("NetworkTab.14"),
								Messages.getString("Dialog.Error"),
								JOptionPane.ERROR_MESSAGE
							);
						}
					}
				});
			}
		}
	}

	/**
	 * Add the renderer configuration selection after they have been
	 * initialized.
	 */
	public void addRenderers() {
		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getEnabledRenderersConfigurations();
		ArrayList<String> keyValues = new ArrayList<>();
		ArrayList<String> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));

		if (allConfs != null) {
			sortRendererConfigurationsByName(allConfs);
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel<String, String> renderersKcbm = new KeyedComboBoxModel<>(
			keyValues.toArray(new String[keyValues.size()]),
			nameValues.toArray(new String[nameValues.size()])
		);
		renderers.setModel(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedValue(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		renderers.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setRendererDefault((String) renderersKcbm.getSelectedKey());
					LOGGER.info(
						"Setting default renderer to \"{}\"",
						renderersKcbm.getSelectedKey().equals("") ? Messages.getRootString("NetworkTab.37") :
						renderersKcbm.getSelectedKey()
					);
				}
			}
		});
	}

	private void sortRendererConfigurationsByName(ArrayList<RendererConfiguration> rendererConfigurations){
		Collections.sort(rendererConfigurations , new Comparator<RendererConfiguration>() {

			@Override
			public int compare(RendererConfiguration o1, RendererConfiguration o2) {
				if(o1 == null && o2 == null) {
					return 0;
				}
				if(o1 == null) {
					return 1;
				}
				if(o2 == null) {
					return -1;
				}
				return o1.getRendererName().toLowerCase().compareTo(o2.getRendererName().toLowerCase());
			}
		});
	}
}
