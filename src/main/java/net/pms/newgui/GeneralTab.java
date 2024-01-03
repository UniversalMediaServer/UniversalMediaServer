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
package net.pms.newgui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.newgui.components.CustomJButton;
import net.pms.newgui.util.FormLayoutUtil;
import net.pms.newgui.util.KeyedComboBoxModel;
import net.pms.platform.windows.WindowsUtils;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralTab {
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTab.class);

	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu , p, 3dlu, p, 3dlu, pref:grow";
	private static final String ROW_SPEC = "p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p";

	private final LooksFrame looksFrame;
	private final JTextField currentLanguage = new JTextField();

	private JComboBox<String> renderers;
	private JTextField host;
	private JTextField port;
	private JTextField serverName;
	private JTextField ipFilter;
	private JTextField maxbitrate;
	private final UmsConfiguration configuration;
	private CustomJButton installService;

	GeneralTab(UmsConfiguration configuration, LooksFrame looksFrame) {
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

		JCheckBox smcheckBox = new JCheckBox(Messages.getString("StartMinimizedSystemTray"), configuration.isMinimized());
		smcheckBox.setContentAreaFilled(false);
		smcheckBox.addItemListener((ItemEvent e) -> configuration.setMinimized((e.getStateChange() == ItemEvent.SELECTED)));

		JComponent cmp = builder.addSeparator(Messages.getString("GeneralSettings_SentenceCase"), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		ypos = 7; // we hardcode here (promise last time)
		builder.addLabel(Messages.getString("Language"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new BoxLayout(languagePanel, BoxLayout.LINE_AXIS));
		currentLanguage.setEnabled(false);
		currentLanguage.setText(Messages.getString("Language." + configuration.getLanguageTag()));
		languagePanel.add(currentLanguage);
		CustomJButton selectLanguage = new CustomJButton("    ...    ");
		selectLanguage.addActionListener((ActionEvent e) -> {
			LanguageSelection selectionDialog = new LanguageSelection(looksFrame, configuration.getLanguageLocale(), true);
			selectionDialog.show();
			if (!selectionDialog.isAborted()) {
				currentLanguage.setText(Messages.getString("Language." + configuration.getLanguageTag()));
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
			builder.addLabel(Messages.getString("ServerName"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(serverName, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			JCheckBox appendProfileName = new JCheckBox(Messages.getString("AppendProfileName"), configuration.isAppendProfileName());
			appendProfileName.setToolTipText(Messages.getString("WhenEnabledUmsProfileName"));
			appendProfileName.setContentAreaFilled(false);
			appendProfileName.addItemListener((ItemEvent e) -> configuration.setAppendProfileName((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(GuiUtil.getPreferredSizeComponent(appendProfileName), FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
			ypos += 2;
		}

		int xpos = 1;
		builder.add(smcheckBox, FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		xpos += 2;

		if (Platform.isWindows()) {
			JCheckBox autoStart = new JCheckBox(Messages.getString("StartWithWindows"), configuration.isAutoStart());
			autoStart.setContentAreaFilled(false);
			autoStart.addItemListener((ItemEvent e) -> configuration.setAutoStart((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(GuiUtil.getPreferredSizeComponent(autoStart), FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
			xpos += 2;
		}

		JCheckBox showSplashScreen = new JCheckBox(Messages.getString("EnableSplashScreen"), configuration.isShowSplashScreen());
		showSplashScreen.setContentAreaFilled(false);
		showSplashScreen.addItemListener((ItemEvent e) -> configuration.setShowSplashScreen((e.getStateChange() == ItemEvent.SELECTED)));

		builder.add(GuiUtil.getPreferredSizeComponent(showSplashScreen), FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		ypos += 2;
		xpos += 2;

		if (!configuration.isHideAdvancedOptions() && Platform.isWindows()) {
			installService = new CustomJButton();
			refreshInstallServiceButtonState();

			builder.add(installService, FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		}

		CustomJButton checkForUpdates = new CustomJButton(Messages.getString("CheckForUpdates"));
		checkForUpdates.addActionListener((ActionEvent e) -> looksFrame.checkForUpdates(false));
		builder.add(checkForUpdates, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		JCheckBox autoUpdateCheckBox = new JCheckBox(Messages.getString("CheckAutomaticallyForUpdates"), configuration.isAutoUpdate());
		autoUpdateCheckBox.setContentAreaFilled(false);
		autoUpdateCheckBox.addItemListener((ItemEvent e) -> configuration.setAutoUpdate((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(GuiUtil.getPreferredSizeComponent(autoUpdateCheckBox), FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		if (!Build.isUpdatable()) {
			checkForUpdates.setEnabled(false);
			autoUpdateCheckBox.setEnabled(false);
		}

		JCheckBox hideAdvancedOptions = new JCheckBox(Messages.getString("HideAdvancedOptions"), configuration.isHideAdvancedOptions());
		hideAdvancedOptions.setContentAreaFilled(false);
		hideAdvancedOptions.addActionListener((ActionEvent e) -> {
			configuration.setHideAdvancedOptions(hideAdvancedOptions.isSelected());
			if (hideAdvancedOptions.isSelected()) {
				looksFrame.setViewLevel(ViewLevel.NORMAL);
			} else {
				looksFrame.setViewLevel(ViewLevel.ADVANCED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(hideAdvancedOptions), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		JCheckBox runWizardOnProgramStartup = new JCheckBox(Messages.getString("RunTheConfigurationWizard"), configuration.isRunWizard());
		runWizardOnProgramStartup.setContentAreaFilled(false);
		runWizardOnProgramStartup.addActionListener((ActionEvent e) -> configuration.setRunWizard(runWizardOnProgramStartup.isSelected()));
		builder.add(GuiUtil.getPreferredSizeComponent(runWizardOnProgramStartup), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			JCheckBox singleInstance = new JCheckBox(Messages.getString("OnlyRunSingleInstance"), configuration.isRunSingleInstance());
			singleInstance.setContentAreaFilled(false);
			singleInstance.setToolTipText(Messages.getString("UmsRunAdministratorSingleInstance"));
			singleInstance.addActionListener((ActionEvent e) -> configuration.setRunSingleInstance(singleInstance.isSelected()));
			builder.add(GuiUtil.getPreferredSizeComponent(singleInstance), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
		}

		List<RendererConfiguration> allConfs = RendererConfigurations.getEnabledRenderersConfigurations();
		List<Object> keyValues = new ArrayList<>();
		List<Object> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("UnknownRenderer"));

		if (!allConfs.isEmpty()) {
			sortRendererConfigurationsByName(allConfs);
			for (RendererConfiguration rendererConf : allConfs) {
				if (rendererConf != null) {
					keyValues.add(rendererConf.getRendererName());
					nameValues.add(rendererConf.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel<String, String> renderersKcbm = new KeyedComboBoxModel<>(
			keyValues.toArray(String[]::new),
			nameValues.toArray(String[]::new));
		renderers = new JComboBox<>(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedKey(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		if (!configuration.isHideAdvancedOptions()) {
			// Edit UMS configuration file manually
			CustomJButton confEdit = new CustomJButton(Messages.getString("EditUmsConfigurationFileManually"));
			confEdit.setToolTipText(configuration.getProfilePath());
			confEdit.addActionListener((ActionEvent e) -> {
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
				Object[] options = {Messages.getString("Save"), Messages.getString("Cancel")};

				if (JOptionPane.showOptionDialog(looksFrame,
					tPanel, Messages.getString("EditUmsConfigurationFileManually"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, null) == JOptionPane.OK_OPTION) {
					String text = textArea.getText();

					try {
						try (FileOutputStream fos = new FileOutputStream(conf)) {
							fos.write(text.getBytes());
							fos.flush();
						}
						configuration.reload();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(looksFrame, Messages.getString("ErrorSavingConfigFile") + e1.toString());
					}
				}
			});
			builder.add(confEdit, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;

			host = new JTextField(configuration.getServerHostname());
			host.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setHostname(host.getText());
				}
			});

			port = new JTextField(configuration.getMediaServerPort() != 5001 ? "" + configuration.getMediaServerPort() : "");
			port.setToolTipText(Messages.getString("IfServerCantFindRenderer"));
			port.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					try {
						String p = port.getText();
						if (StringUtils.isEmpty(p)) {
							p = "5001";
						}
						int ab = Integer.parseInt(p);
						configuration.setMediaServerPort(ab);
					} catch (NumberFormatException nfe) {
						LOGGER.debug("Could not parse port from \"" + port.getText() + "\"");
					}

				}
			});

			cmp = builder.addSeparator(Messages.getString("NetworkSettingsAdvanced"), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			final KeyedComboBoxModel<String, String> networkInterfaces = createNetworkInterfacesModel();
			JComboBox<String> networkinterfacesCBX = new JComboBox<>(networkInterfaces);
			String savedNetworkInterface = configuration.getNetworkInterface();
			// for backwards-compatibility check if the short network interface name is used
			savedNetworkInterface = NetworkConfiguration.replaceShortInterfaceNameByDisplayName(savedNetworkInterface);
			networkInterfaces.setSelectedKey(savedNetworkInterface);
			networkinterfacesCBX.addItemListener((ItemEvent e) -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setNetworkInterface(networkInterfaces.getSelectedKey());
				}
			});

			ipFilter = new JTextField(configuration.getIpFilter());
			ipFilter.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setIpFilter(ipFilter.getText());
				}
			});

			maxbitrate = new JTextField(configuration.getMaximumBitrateDisplay());
			maxbitrate.setToolTipText(Messages.getString("AValue90Recommended"));
			maxbitrate.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setMaximumBitrate(maxbitrate.getText());
				}
			});
			maxbitrate.setEnabled(!configuration.isAutomaticMaximumBitrate());

			JCheckBox adaptBitrate = new JCheckBox(Messages.getString("UseAutomaticMaximumBandwidth"), configuration.isAutomaticMaximumBitrate());
			adaptBitrate.setToolTipText(Messages.getString("ItSetsOptimalBandwidth"));
			adaptBitrate.setContentAreaFilled(false);
			adaptBitrate.addActionListener((ActionEvent e) -> {
				configuration.setAutomaticMaximumBitrate(adaptBitrate.isSelected());
				maxbitrate.setEnabled(!configuration.isAutomaticMaximumBitrate());
				looksFrame.getTr().enableVideoQualitySettings(configuration.isAutomaticMaximumBitrate());
			});

			builder.addLabel(Messages.getString("ForceNetworkingInterface"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(networkinterfacesCBX, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getString("ForceIpServer"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(host, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getString("ForcePortServer"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(port, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getString("UseIpFilter"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(ipFilter, FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getString("MaximumBandwidthMbs"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(maxbitrate, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));
			builder.add(GuiUtil.getPreferredSizeComponent(adaptBitrate), FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
			ypos += 2;

			cmp = builder.addSeparator(Messages.getString("AdvancedHttpSystemSettings"), FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
			cmp = (JComponent) cmp.getComponent(0);
			cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

			builder.addLabel(Messages.getString("MediaServerEngine"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			final KeyedComboBoxModel<Integer, String> mediaServerEngineKcbm = new KeyedComboBoxModel<>();
			mediaServerEngineKcbm.add(0, Messages.getString("Default"));
			for (Entry<Integer, String> upnpEngineVersion : MediaServer.VERSIONS.entrySet()) {
				mediaServerEngineKcbm.add(upnpEngineVersion.getKey(), upnpEngineVersion.getValue());
			}
			JComboBox<String> serverEngine = new JComboBox<>(mediaServerEngineKcbm);
			serverEngine.setToolTipText(Messages.getString("DefaultOptionIsHighlyRecommended"));
			serverEngine.setEditable(false);
			mediaServerEngineKcbm.setSelectedKey(configuration.getServerEngine());
			if (serverEngine.getSelectedIndex() == -1) {
				serverEngine.setSelectedIndex(0);
			}
			serverEngine.addItemListener((ItemEvent e) -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					configuration.setServerEngine((int) mediaServerEngineKcbm.getSelectedKey());
					LOGGER.info(
						"Setting default media server engine version to \"{}\"",
						mediaServerEngineKcbm.getSelectedValue()
					);
				}
			});
			builder.add(serverEngine, FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			ypos += 2;

			boolean preventSleepSupported = SleepManager.isPreventSleepSupported();
			if (preventSleepSupported) {
				builder.addLabel(Messages.getString("PreventSleep"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
				final KeyedComboBoxModel<PreventSleepMode, String> preventSleepModel = createPreventSleepModel();
				JComboBox<String> preventSleep = new JComboBox<>(preventSleepModel);
				preventSleep.setToolTipText(Messages.getString("DuringPlaybackPreventOperating"));
				preventSleepModel.setSelectedKey(configuration.getPreventSleep());
				preventSleep.addItemListener((ItemEvent e) -> {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						configuration.setPreventSleep(preventSleepModel.getSelectedKey());
					}
				});
				builder.add(preventSleep, FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
				ypos += 2;
			}

			final SelectRenderers selectRenderers = new SelectRenderers();

			builder.addLabel(Messages.getString("EnabledRenderers"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			final CustomJButton setRenderers = new CustomJButton(Messages.getString("SelectRenderers"));
			setRenderers.addActionListener((ActionEvent e) -> {
				selectRenderers.showDialog();
			});

			builder.add(setRenderers, FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			ypos += 2;

			builder.addLabel(Messages.getString("DefaultRendererWhenAutoFails"), FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

			builder.add(renderers, FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			JCheckBox forceDefaultRenderer = new JCheckBox(Messages.getString("ForceDefaultRenderer"), configuration.isRendererForceDefault());
			forceDefaultRenderer.setToolTipText(Messages.getString("DisablesAutomaticDetection"));
			forceDefaultRenderer.setContentAreaFilled(false);
			forceDefaultRenderer.addItemListener((ItemEvent e) -> configuration.setRendererForceDefault((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(forceDefaultRenderer, FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));

			ypos += 2;

			// External network box
			JCheckBox extNetBox = new JCheckBox(Messages.getString("EnableExternalNetwork"), configuration.getExternalNetwork());
			extNetBox.setToolTipText(Messages.getString("ThisControlsWhetherUmsTry"));
			extNetBox.setContentAreaFilled(false);
			extNetBox.addItemListener((ItemEvent e) -> configuration.setExternalNetwork((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(extNetBox, FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
		}

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		JScrollPane scrollPane = new JScrollPane(
			panel,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		return scrollPane;
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
			installService.setText(Messages.getString("InstallAsWindowsService"));
		} else {
			installService.setEnabled(true);

			boolean isUmsServiceInstalled = WindowsUtils.isUmsServiceInstalled();

			if (isUmsServiceInstalled) {
				// Update button text and tooltip
				installService.setText(Messages.getString("UninstallWindowsService"));
				installService.setToolTipText(null);

				// Remove all attached action listeners
				for (ActionListener al : installService.getActionListeners()) {
					installService.removeActionListener(al);
				}

				// Attach the button clicked action listener
				installService.addActionListener((ActionEvent e) -> {
					WindowsUtils.uninstallWin32Service();
					LOGGER.info("Uninstalled UMS Windows service");

					// Refresh the button state after it has been clicked
					refreshInstallServiceButtonState();

					JOptionPane.showMessageDialog(
						looksFrame,
						Messages.getString("UninstalledWindowsService"),
						Messages.getString("Information"),
						JOptionPane.INFORMATION_MESSAGE
					);
				});
			} else {
				// Update button text and tooltip
				installService.setText(Messages.getString("InstallAsWindowsService"));
				installService.setToolTipText(Messages.getString("NotRecommendedJustStartMinimized"));

				// Remove all attached action listeners
				for (ActionListener al : installService.getActionListeners()) {
					installService.removeActionListener(al);
				}

				// Attach the button clicked action listener
				installService.addActionListener((ActionEvent e) -> {
					if (WindowsUtils.installWin32Service()) {
						LOGGER.info("Installed UMS Windows service");

						// Refresh the button state after it has been clicked
						refreshInstallServiceButtonState();

						JOptionPane.showMessageDialog(
							looksFrame,
							Messages.getString("YouHaveInstalledWindowsService") +
								Messages.getString("ThenStartServiceWindows"),
							Messages.getString("Information"),
							JOptionPane.INFORMATION_MESSAGE
						);
					} else {
						JOptionPane.showMessageDialog(
							looksFrame,
							Messages.getString("CouldNotInstallWindowsService"),
							Messages.getString("Error"),
							JOptionPane.ERROR_MESSAGE
						);
					}
				});
			}
		}
	}

	private KeyedComboBoxModel<String, String> createNetworkInterfacesModel() {
		List<String> keys = NetworkConfiguration.getDisplayNames();
		List<String> names = NetworkConfiguration.getDisplayNamesWithAddress();
		keys.add(0, "");
		names.add(0, "");
		return new KeyedComboBoxModel<>(
			keys.toArray(String[]::new),
			names.toArray(String[]::new)
		);
	}

	private KeyedComboBoxModel<PreventSleepMode, String> createPreventSleepModel() {
		PreventSleepMode[] modes = PreventSleepMode.values();
		String[] descriptions = new String[modes.length];
		for (int i = 0; i < modes.length; i++) {
			descriptions[i] = modes[i].toString();
		}
		return new KeyedComboBoxModel<>(
			modes,
			descriptions
		);
	}

	/**
	 * Add the renderer configuration selection after they have been
	 * initialized.
	 */
	public void addRenderers() {
		List<RendererConfiguration> allConfs = RendererConfigurations.getEnabledRenderersConfigurations();
		List<String> keyValues = new ArrayList<>();
		List<String> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getString("UnknownRenderer"));

		if (!allConfs.isEmpty()) {
			sortRendererConfigurationsByName(allConfs);
			for (RendererConfiguration rendererConf : allConfs) {
				if (rendererConf != null) {
					keyValues.add(rendererConf.getRendererName());
					nameValues.add(rendererConf.getRendererName());
				}
			}
		}

		final KeyedComboBoxModel<String, String> renderersKcbm = new KeyedComboBoxModel<>(
			keyValues.toArray(String[]::new),
			nameValues.toArray(String[]::new)
		);
		renderers.setModel(renderersKcbm);
		renderers.setEditable(false);
		String defaultRenderer = configuration.getRendererDefault();
		renderersKcbm.setSelectedValue(defaultRenderer);

		if (renderers.getSelectedIndex() == -1) {
			renderers.setSelectedIndex(0);
		}

		renderers.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				configuration.setRendererDefault((String) renderersKcbm.getSelectedKey());
				LOGGER.info(
					"Setting default renderer to \"{}\"",
					renderersKcbm.getSelectedKey().equals("") ? Messages.getRootString("NetworkTab.37") :
						renderersKcbm.getSelectedKey()
				);
			}
		});
	}

	public static void sortRendererConfigurationsByName(List<RendererConfiguration> rendererConfigurations) {
		Collections.sort(rendererConfigurations, (RendererConfiguration o1, RendererConfiguration o2) -> {
			if (o1 == null && o2 == null) {
				return 0;
			}

			if (o1 == null) {
				return 1;
			}

			if (o2 == null) {
				return -1;
			}

			return o1.getRendererName().toLowerCase().compareTo(o2.getRendererName().toLowerCase());
		});
	}
}
