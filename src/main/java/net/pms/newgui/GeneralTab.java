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
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.NetworkConfiguration;
import net.pms.util.FormLayoutUtil;
import net.pms.util.KeyedComboBoxModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTab.class);

	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu , p, 3dlu, p, 3dlu, pref:grow";
	private static final String ROW_SPEC = "p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p";

	public static JCheckBox smcheckBox;
	private JCheckBox autoStart;
	private JCheckBox autoUpdateCheckBox;
	private JCheckBox hideAdvancedOptions;
	private JCheckBox newHTTPEngine;
	private JCheckBox preventSleep;
	private JTextField host;
	private JTextField port;
	private JComboBox langs;
	private JComboBox networkinterfacesCBX;
	private JTextField ip_filter;
	public static JTextField maxbitrate;
	private JComboBox renderers;
	private final PmsConfiguration configuration;
	private JCheckBox extNetBox;

	GeneralTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	public JComponent build() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);

		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		smcheckBox = new JCheckBox(Messages.getString("NetworkTab.3"));
		smcheckBox.setContentAreaFilled(false);
		smcheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setMinimized((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		if (configuration.isMinimized()) {
			smcheckBox.setSelected(true);
		}

		autoStart = new JCheckBox(Messages.getString("NetworkTab.57"));
		autoStart.setContentAreaFilled(false);
		autoStart.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAutoStart((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		if (configuration.isAutoStart()) {
			autoStart.setSelected(true);
		}

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(1, 1, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		builder.addLabel(Messages.getString("NetworkTab.0"), FormLayoutUtil.flip(cc.xy(1, 7), colSpec, orientation));
		final KeyedComboBoxModel kcbm = new KeyedComboBoxModel(new Object[] {
				"ar", "bg", "ca", "zhs", "zht", "cz", "da", "nl", "en", "en_uk", "fi", "fr",
				"de", "el", "iw", "is", "it", "ja", "ko", "no", "pl", "pt", "br",
				"ro", "ru", "sl", "es", "sv", "tr"}, new Object[] {
				"Arabic", "Bulgarian", "Catalan", "Chinese (Simplified)",
				"Chinese (Traditional)", "Czech", "Danish", "Dutch", "English (US)", "English (UK)",
				"Finnish", "French", "German", "Greek", "Hebrew", "Icelandic", "Italian",
				"Japanese", "Korean", "Norwegian", "Polish", "Portuguese",
				"Portuguese (Brazilian)", "Romanian", "Russian", "Slovenian",
				"Spanish", "Swedish", "Turkish"});
		langs = new JComboBox(kcbm);
		langs.setEditable(false);

		String defaultLang;
		if (configuration.getLanguage() != null && configuration.getLanguage().length() > 0) {
			defaultLang = configuration.getLanguage();
		} else {
			defaultLang = Locale.getDefault().getLanguage();
		}

		if (defaultLang == null) {
			defaultLang = "en";
		}

		kcbm.setSelectedKey(defaultLang);

		if (langs.getSelectedIndex() == -1) {
			langs.setSelectedIndex(0);
		}

		langs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setLanguage((String) kcbm.getSelectedKey());
				}
			}
		});

		builder.add(langs, FormLayoutUtil.flip(cc.xyw(3, 7, 7), colSpec, orientation));

		builder.add(smcheckBox, FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));

		if (Platform.isWindows()) {
			builder.add(autoStart, FormLayoutUtil.flip(cc.xyw(3, 9, 7), colSpec, orientation));
		}

		if (!configuration.isHideAdvancedOptions()) {
			CustomJButton service = new CustomJButton(Messages.getString("NetworkTab.4"));
			service.setToolTipText(Messages.getString("NetworkTab.63"));
			service.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (PMS.get().installWin32Service()) {
						LOGGER.info(Messages.getString("PMS.41"));
						JOptionPane.showMessageDialog(
							(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
							Messages.getString("NetworkTab.11") +
							Messages.getString("NetworkTab.12"),
							Messages.getString("Dialog.Information"),
							JOptionPane.INFORMATION_MESSAGE
						);
					} else {
						JOptionPane.showMessageDialog(
							(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
							Messages.getString("NetworkTab.14"),
							Messages.getString("Dialog.Error"),
							JOptionPane.ERROR_MESSAGE
						);
					}
				}
			});
			builder.add(service, FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
			if (System.getProperty(LooksFrame.START_SERVICE) != null || !Platform.isWindows()) {
				service.setEnabled(false);
			}

			CustomJButton serviceUninstall = new CustomJButton(Messages.getString("GeneralTab.2"));
			serviceUninstall.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					PMS.get().uninstallWin32Service();
					LOGGER.info(Messages.getString("GeneralTab.3"));
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("GeneralTab.3"),
						Messages.getString("Dialog.Information"),
						JOptionPane.INFORMATION_MESSAGE
					);
				}
			});
			builder.add(serviceUninstall, FormLayoutUtil.flip(cc.xy(3, 11), colSpec, orientation));
			if (System.getProperty(LooksFrame.START_SERVICE) != null || !Platform.isWindows()) {
				serviceUninstall.setEnabled(false);
			}
		}

		CustomJButton checkForUpdates = new CustomJButton(Messages.getString("NetworkTab.8"));
		checkForUpdates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LooksFrame frame = (LooksFrame) PMS.get().getFrame();
				frame.checkForUpdates();
			}
		});
		if (configuration.isHideAdvancedOptions()) {
			builder.add(checkForUpdates, FormLayoutUtil.flip(cc.xy(1, 11), colSpec, orientation));
		} else {
			builder.add(checkForUpdates, FormLayoutUtil.flip(cc.xy(1, 13), colSpec, orientation));
		}

		autoUpdateCheckBox = new JCheckBox(Messages.getString("NetworkTab.9"));
		autoUpdateCheckBox.setContentAreaFilled(false);
		autoUpdateCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAutoUpdate((e.getStateChange() == ItemEvent.SELECTED));
			}
		});
		if (configuration.isAutoUpdate()) {
			autoUpdateCheckBox.setSelected(true);
		}
		if (configuration.isHideAdvancedOptions()) {
			builder.add(autoUpdateCheckBox, FormLayoutUtil.flip(cc.xyw(3, 11, 7), colSpec, orientation));
		} else {
			builder.add(autoUpdateCheckBox, FormLayoutUtil.flip(cc.xyw(3, 13, 7), colSpec, orientation));
		}
		if (!Build.isUpdatable()) {
			checkForUpdates.setEnabled(false);
			autoUpdateCheckBox.setEnabled(false);
		}

		hideAdvancedOptions = new JCheckBox(Messages.getString("NetworkTab.61"));
		hideAdvancedOptions.setContentAreaFilled(false);
		hideAdvancedOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setHideAdvancedOptions(hideAdvancedOptions.isSelected());
			}
		});
		if (configuration.isHideAdvancedOptions()) {
			hideAdvancedOptions.setSelected(true);
			builder.add(hideAdvancedOptions, FormLayoutUtil.flip(cc.xyw(1, 13, 9), colSpec, orientation));
		} else {
			builder.add(hideAdvancedOptions, FormLayoutUtil.flip(cc.xyw(1, 15, 9), colSpec, orientation));
		}

		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getEnabledRenderersConfigurations();
		ArrayList<Object> keyValues = new ArrayList<>();
		ArrayList<Object> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));

		if (allConfs != null) {
			sortRendererConfiurationsByName(allConfs);			
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel renderersKcbm = new KeyedComboBoxModel(
			(Object[]) keyValues.toArray(new Object[keyValues.size()]),
			(Object[]) nameValues.toArray(new Object[nameValues.size()]));
		renderers = new JComboBox(renderersKcbm);
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
					scrollPane.setPreferredSize(new java.awt.Dimension(900, 450));

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
					} catch (Exception e1) {
						return;
					}

					tPanel.add(scrollPane, BorderLayout.NORTH);
					Object[] options = {Messages.getString("LooksFrame.9"), Messages.getString("NetworkTab.45")};

					if (JOptionPane.showOptionDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
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
							JOptionPane.showMessageDialog((JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
								Messages.getString("NetworkTab.52") + e1.toString());
						}
					}
				}
			});
			builder.add(confEdit, FormLayoutUtil.flip(cc.xy(1, 17), colSpec, orientation));

			host = new JTextField(configuration.getServerHostname());
			host.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setHostname(host.getText());
				}
			});

			port = new JTextField(configuration.getServerPort() != 5001 ? "" + configuration.getServerPort() : "");
			port.setToolTipText(Messages.getString("NetworkTab.64"));
			port.addKeyListener(new KeyAdapter() {
				@Override
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
			});

			cmp = builder.addSeparator(Messages.getString("NetworkTab.22"), FormLayoutUtil.flip(cc.xyw(1, 19, 9), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			final KeyedComboBoxModel networkInterfaces = createNetworkInterfacesModel();
			networkinterfacesCBX = new JComboBox(networkInterfaces);
			networkInterfaces.setSelectedKey(configuration.getNetworkInterface());
			networkinterfacesCBX.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						configuration.setNetworkInterface((String) networkInterfaces.getSelectedKey());
					}
				}
			});

			ip_filter = new JTextField(configuration.getIpFilter());
			ip_filter.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setIpFilter(ip_filter.getText());
				}
			});

			maxbitrate = new JTextField(configuration.getMaximumBitrateDisplay());
			maxbitrate.setToolTipText(Messages.getString("NetworkTab.65"));
			maxbitrate.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setMaximumBitrate(maxbitrate.getText());
				}
			});

			builder.addLabel(Messages.getString("NetworkTab.20"), FormLayoutUtil.flip(cc.xy(1, 21), colSpec, orientation));
			builder.add(networkinterfacesCBX, FormLayoutUtil.flip(cc.xyw(3, 21, 7), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.23"), FormLayoutUtil.flip(cc.xy(1, 23), colSpec, orientation));
			builder.add(host, FormLayoutUtil.flip(cc.xyw(3, 23, 7), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.24"), FormLayoutUtil.flip(cc.xy(1, 25), colSpec, orientation));
			builder.add(port, FormLayoutUtil.flip(cc.xyw(3, 25, 7), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.30"), FormLayoutUtil.flip(cc.xy(1, 27), colSpec, orientation));
			builder.add(ip_filter, FormLayoutUtil.flip(cc.xyw(3, 27, 7), colSpec, orientation));
			builder.addLabel(Messages.getString("NetworkTab.35"), FormLayoutUtil.flip(cc.xy(1, 29), colSpec, orientation));
			builder.add(maxbitrate, FormLayoutUtil.flip(cc.xyw(3, 29, 7), colSpec, orientation));

			cmp = builder.addSeparator(Messages.getString("NetworkTab.31"), FormLayoutUtil.flip(cc.xyw(1, 31, 9), colSpec, orientation));
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			newHTTPEngine = new JCheckBox(Messages.getString("NetworkTab.32"));
			newHTTPEngine.setSelected(configuration.isHTTPEngineV2());
			newHTTPEngine.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setHTTPEngineV2((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(newHTTPEngine, FormLayoutUtil.flip(cc.xy(1, 33), colSpec, orientation));

			preventSleep = new JCheckBox(Messages.getString("NetworkTab.33"));
			preventSleep.setSelected(configuration.isPreventsSleep());
			preventSleep.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setPreventsSleep((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			builder.add(preventSleep, FormLayoutUtil.flip(cc.xy(1, 35), colSpec, orientation));

			JCheckBox fdCheckBox = new JCheckBox(Messages.getString("NetworkTab.38"));
			fdCheckBox.setContentAreaFilled(false);
			fdCheckBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setRendererForceDefault((e.getStateChange() == ItemEvent.SELECTED));
				}
			});

			if (configuration.isRendererForceDefault()) {
				fdCheckBox.setSelected(true);
			}

			builder.addLabel(Messages.getString("NetworkTab.62"), FormLayoutUtil.flip(cc.xy(1, 37), colSpec, orientation));
			final CustomJButton setRenderers = new CustomJButton(Messages.getString("GeneralTab.5"));
			setRenderers.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SelectRenderers.showDialog();
				}
			});

			builder.add(setRenderers, FormLayoutUtil.flip(cc.xy(3, 37), colSpec, orientation));

			builder.addLabel(Messages.getString("NetworkTab.36"), FormLayoutUtil.flip(cc.xy(1, 39), colSpec, orientation));

			builder.add(renderers, FormLayoutUtil.flip(cc.xyw(3, 39, 7), colSpec, orientation));

			// External network box
			extNetBox = new JCheckBox(Messages.getString("NetworkTab.56"));
			extNetBox.setToolTipText(Messages.getString("NetworkTab.67"));
			extNetBox.setContentAreaFilled(false);
			extNetBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					configuration.setExternalNetwork((e.getStateChange() == ItemEvent.SELECTED));
				}
			});
			extNetBox.setSelected(configuration.getExternalNetwork());
			builder.add(extNetBox, FormLayoutUtil.flip(cc.xy(1, 43), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
	}

	private KeyedComboBoxModel createNetworkInterfacesModel() {
		List<String> keys = NetworkConfiguration.getInstance().getKeys();
		List<String> names = NetworkConfiguration.getInstance().getDisplayNames();
		keys.add(0, "");
		names.add(0, "");
		final KeyedComboBoxModel networkInterfaces = new KeyedComboBoxModel(keys.toArray(), names.toArray());
		return networkInterfaces;
	}

	/**
	 * Add the renderer configuration selection after they have been
	 * initialized.
	 */
	public void addRenderers() {
		ArrayList<RendererConfiguration> allConfs = RendererConfiguration.getEnabledRenderersConfigurations();
		ArrayList<Object> keyValues = new ArrayList<>();
		ArrayList<Object> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("NetworkTab.37"));

		if (allConfs != null) {
			sortRendererConfiurationsByName(allConfs);
			for (RendererConfiguration renderer : allConfs) {
				if (renderer != null) {
					keyValues.add(renderer.getRendererName());
					nameValues.add(renderer.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel renderersKcbm = new KeyedComboBoxModel(
			(Object[]) keyValues.toArray(new Object[keyValues.size()]),
			(Object[]) nameValues.toArray(new Object[nameValues.size()])
		);
		renderers.setModel(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedKey(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		renderers.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					LOGGER.info("Setting renderer default: \"" + renderersKcbm.getSelectedKey() + "\"");
					configuration.setRendererDefault((String) renderersKcbm.getSelectedKey());
				}
			}
		});
	}
	
	private void sortRendererConfiurationsByName(ArrayList<RendererConfiguration> rendererConfigurations){
		Collections.sort(rendererConfigurations , new Comparator<RendererConfiguration>() {

			@Override
			public int compare(RendererConfiguration o1, RendererConfiguration o2) {
				if(o1 == null && o2 == null){
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
