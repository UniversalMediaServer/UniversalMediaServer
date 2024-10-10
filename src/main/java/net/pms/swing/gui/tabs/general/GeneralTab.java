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
package net.pms.swing.gui.tabs.general;

import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.Build;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.platform.windows.WindowsUtils;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import net.pms.swing.LanguageSelection;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.CustomJButton;
import net.pms.swing.components.KeyedComboBoxModel;
import net.pms.swing.gui.FormLayoutUtil;
import net.pms.swing.gui.JavaGui;
import net.pms.swing.gui.UmsFormBuilder;
import net.pms.swing.gui.ViewLevel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralTab {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneralTab.class);
	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu , p, 3dlu, p, 3dlu, pref:grow";
	private static final String ROW_SPEC = "p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 15dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p";

	private final JavaGui looksFrame;
	private final UmsConfiguration configuration;
	private final JTextField currentLanguage = new JTextField();

	private JComboBox<String> renderers;
	private JTextField host;
	private JTextField port;
	private JTextField serverName;
	private JTextField ipFilter;
	private JTextField maxbitrate;
	private CustomJButton installService;
	private JCheckBox isUseInfoFromAPI;
	private JCheckBox useInfoFromTMDB;
	private JLabel tmdbApiKeyLabel;
	private JTextField tmdbApiKey;

	public GeneralTab(UmsConfiguration configuration, JavaGui looksFrame) {
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
		UmsFormBuilder builder = UmsFormBuilder.create().layout(layout);
		builder.border(Paddings.DLU4);
		builder.opaque(true);

		CellConstraints cc = new CellConstraints();

		JCheckBox smcheckBox = new JCheckBox(Messages.getGuiString("StartMinimizedSystemTray"), configuration.isMinimized());
		smcheckBox.setContentAreaFilled(false);
		smcheckBox.addItemListener((ItemEvent e) -> configuration.setMinimized((e.getStateChange() == ItemEvent.SELECTED)));

		builder.addSeparator(Messages.getGuiString("GeneralSettings_SentenceCase")).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos = 7; // we hardcode here (promise last time)
		builder.addLabel(Messages.getGuiString("Language")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new BoxLayout(languagePanel, BoxLayout.LINE_AXIS));
		currentLanguage.setEnabled(false);
		currentLanguage.setText(Messages.getGuiString("Language." + configuration.getLanguageTag()));
		languagePanel.add(currentLanguage);
		CustomJButton selectLanguage = new CustomJButton("    ...    ");
		selectLanguage.addActionListener((ActionEvent e) -> {
			LanguageSelection selectionDialog = new LanguageSelection(looksFrame, configuration.getLanguageLocale(), true);
			selectionDialog.show();
			if (!selectionDialog.isAborted()) {
				currentLanguage.setText(Messages.getGuiString("Language." + configuration.getLanguageTag()));
				looksFrame.applyLanguage();
			}
		});
		languagePanel.add(selectLanguage);
		builder.add(languagePanel).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			serverName = new JTextField(configuration.getServerName());
			serverName.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setServerName(serverName.getText());
				}
			});
			builder.addLabel(Messages.getGuiString("ServerName")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(serverName).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			JCheckBox appendProfileName = new JCheckBox(Messages.getGuiString("AppendProfileName"), configuration.isAppendProfileName());
			appendProfileName.setToolTipText(Messages.getGuiString("WhenEnabledUmsProfileName"));
			appendProfileName.setContentAreaFilled(false);
			appendProfileName.addItemListener((ItemEvent e) -> configuration.setAppendProfileName((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(SwingUtil.getPreferredSizeComponent(appendProfileName)).at(FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
			ypos += 2;
		}

		int xpos = 1;
		builder.add(smcheckBox).at(FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		xpos += 2;

		if (Platform.isWindows()) {
			JCheckBox autoStart = new JCheckBox(Messages.getGuiString("StartWithWindows"), configuration.isAutoStart());
			autoStart.setContentAreaFilled(false);
			autoStart.addItemListener((ItemEvent e) -> configuration.setAutoStart((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(SwingUtil.getPreferredSizeComponent(autoStart)).at(FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
			xpos += 2;
		}

		JCheckBox showSplashScreen = new JCheckBox(Messages.getGuiString("EnableSplashScreen"), configuration.isShowSplashScreen());
		showSplashScreen.setContentAreaFilled(false);
		showSplashScreen.addItemListener((ItemEvent e) -> configuration.setShowSplashScreen((e.getStateChange() == ItemEvent.SELECTED)));

		builder.add(SwingUtil.getPreferredSizeComponent(showSplashScreen)).at(FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		ypos += 2;
		xpos += 2;

		if (!configuration.isHideAdvancedOptions() && Platform.isWindows()) {
			installService = new CustomJButton();
			refreshInstallServiceButtonState();

			builder.add(installService).at(FormLayoutUtil.flip(cc.xy(xpos, ypos), colSpec, orientation));
		}

		CustomJButton checkForUpdates = new CustomJButton(Messages.getGuiString("CheckForUpdates"));
		checkForUpdates.addActionListener((ActionEvent e) -> looksFrame.checkForUpdates(false));
		builder.add(checkForUpdates).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

		JCheckBox autoUpdateCheckBox = new JCheckBox(Messages.getGuiString("CheckAutomaticallyForUpdates"), configuration.isAutoUpdate());
		autoUpdateCheckBox.setContentAreaFilled(false);
		autoUpdateCheckBox.addItemListener((ItemEvent e) -> configuration.setAutoUpdate((e.getStateChange() == ItemEvent.SELECTED)));
		builder.add(SwingUtil.getPreferredSizeComponent(autoUpdateCheckBox)).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
		ypos += 2;
		if (!Build.isUpdatable()) {
			checkForUpdates.setEnabled(false);
			autoUpdateCheckBox.setEnabled(false);
		}

		JCheckBox showAdvancedOptions = new JCheckBox(Messages.getGuiString("ShowAdvancedSettings"), !configuration.isHideAdvancedOptions());
		showAdvancedOptions.setContentAreaFilled(false);
		showAdvancedOptions.addActionListener((ActionEvent e) -> {
			configuration.setHideAdvancedOptions(!showAdvancedOptions.isSelected());
			if (!showAdvancedOptions.isSelected()) {
				looksFrame.setViewLevel(ViewLevel.NORMAL);
			} else {
				looksFrame.setViewLevel(ViewLevel.ADVANCED);
			}
		});
		builder.add(SwingUtil.getPreferredSizeComponent(showAdvancedOptions)).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		JCheckBox runWizardOnProgramStartup = new JCheckBox(Messages.getGuiString("RunTheConfigurationWizard"), configuration.isRunWizard());
		runWizardOnProgramStartup.setContentAreaFilled(false);
		runWizardOnProgramStartup.addActionListener((ActionEvent e) -> configuration.setRunWizard(runWizardOnProgramStartup.isSelected()));
		builder.add(SwingUtil.getPreferredSizeComponent(runWizardOnProgramStartup)).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
		ypos += 2;

		if (!configuration.isHideAdvancedOptions()) {
			JCheckBox singleInstance = new JCheckBox(Messages.getGuiString("OnlyRunSingleInstance"), configuration.isRunSingleInstance());
			singleInstance.setContentAreaFilled(false);
			singleInstance.setToolTipText(Messages.getGuiString("UmsRunAdministratorSingleInstance"));
			singleInstance.addActionListener((ActionEvent e) -> configuration.setRunSingleInstance(singleInstance.isSelected()));
			builder.add(SwingUtil.getPreferredSizeComponent(singleInstance)).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
		}

		List<RendererConfiguration> allConfs = RendererConfigurations.getEnabledRenderersConfigurations();
		List<Object> keyValues = new ArrayList<>();
		List<Object> nameValues = new ArrayList<>();
		keyValues.add("");
		nameValues.add(Messages.getGuiString("UnknownRenderer"));

		if (!allConfs.isEmpty()) {
			RendererConfigurations.sortRendererConfigurationsByName(allConfs);
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
			CustomJButton confEdit = new CustomJButton(Messages.getGuiString("EditUmsConfigurationFileManually"));
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
				Object[] options = {Messages.getGuiString("Save"), Messages.getGuiString("Cancel")};

				if (JOptionPane.showOptionDialog(looksFrame,
					tPanel, Messages.getGuiString("EditUmsConfigurationFileManually"),
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
						JOptionPane.showMessageDialog(looksFrame, Messages.getGuiString("ErrorSavingConfigFile") + e1.toString());
					}
				}
			});
			builder.add(confEdit).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;

			host = new JTextField(configuration.getServerHostname());
			host.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setHostname(host.getText());
				}
			});

			port = new JTextField(configuration.getMediaServerPort() != 5001 ? "" + configuration.getMediaServerPort() : "");
			port.setToolTipText(Messages.getGuiString("IfServerCantFindRenderer"));
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

			builder.addSeparator(Messages.getGuiString("NetworkSettingsAdvanced")).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;

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

			ipFilter = new JTextField(configuration.getNetworkDevicesFilter());
			ipFilter.setToolTipText(Messages.getGuiString((configuration.isNetworkDevicesBlockedByDefault() ? "NetworkDevicesBlockedByDefault" : "NetworkDevicesAllowedByDefault")));
			ipFilter.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setNetworkDevicesFilter(ipFilter.getText());
				}
			});

			maxbitrate = new JTextField(configuration.getMaximumBitrateDisplay());
			maxbitrate.setToolTipText(Messages.getGuiString("AValue90Recommended"));
			maxbitrate.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setMaximumBitrate(maxbitrate.getText());
				}
			});
			maxbitrate.setEnabled(!configuration.isAutomaticMaximumBitrate());

			JCheckBox adaptBitrate = new JCheckBox(Messages.getGuiString("UseAutomaticMaximumBandwidth"), configuration.isAutomaticMaximumBitrate());
			adaptBitrate.setToolTipText(Messages.getGuiString("ItSetsOptimalBandwidth"));
			adaptBitrate.setContentAreaFilled(false);
			adaptBitrate.addActionListener((ActionEvent e) -> {
				configuration.setAutomaticMaximumBitrate(adaptBitrate.isSelected());
				maxbitrate.setEnabled(!configuration.isAutomaticMaximumBitrate());
				looksFrame.getTranscodingTab().enableVideoQualitySettings(configuration.isAutomaticMaximumBitrate());
			});

			builder.addLabel(Messages.getGuiString("ForceNetworkingInterface")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(networkinterfacesCBX).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getGuiString("ForceIpServer")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(host).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getGuiString("ForcePortServer")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(port).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getGuiString((configuration.isNetworkDevicesBlockedByDefault() ? "AllowedNetworkDevices" : "BlockedNetworkDevices"))).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(ipFilter).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 7), colSpec, orientation));
			ypos += 2;
			builder.addLabel(Messages.getGuiString("MaximumBandwidthMbs")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			builder.add(maxbitrate).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));
			builder.add(SwingUtil.getPreferredSizeComponent(adaptBitrate)).at(FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));
			ypos += 2;

			builder.addSeparator(Messages.getGuiString("AdvancedHttpSystemSettings")).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;

			boolean preventSleepSupported = SleepManager.isPreventSleepSupported();
			if (preventSleepSupported) {
				builder.addLabel(Messages.getGuiString("PreventSleep")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
				final KeyedComboBoxModel<PreventSleepMode, String> preventSleepModel = createPreventSleepModel();
				JComboBox<String> preventSleep = new JComboBox<>(preventSleepModel);
				preventSleep.setToolTipText(Messages.getGuiString("DuringPlaybackPreventOperating"));
				preventSleepModel.setSelectedKey(configuration.getPreventSleep());
				preventSleep.addItemListener((ItemEvent e) -> {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						configuration.setPreventSleep(preventSleepModel.getSelectedKey());
					}
				});
				builder.add(preventSleep).at(FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
				ypos += 2;
			}

			final SelectRenderers selectRenderers = new SelectRenderers();

			builder.addLabel(Messages.getGuiString("EnabledRenderers")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			final CustomJButton setRenderers = new CustomJButton(Messages.getGuiString("SelectRenderers"));
			setRenderers.addActionListener((ActionEvent e) -> selectRenderers.showDialog());

			builder.add(setRenderers).at(FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			ypos += 2;

			builder.addLabel(Messages.getGuiString("DefaultRendererWhenAutoFails")).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));

			builder.add(renderers).at(FormLayoutUtil.flip(cc.xyw(3, ypos, 3), colSpec, orientation));

			JCheckBox forceDefaultRenderer = new JCheckBox(Messages.getGuiString("ForceDefaultRenderer"), configuration.isRendererForceDefault());
			forceDefaultRenderer.setToolTipText(Messages.getGuiString("DisablesAutomaticDetection"));
			forceDefaultRenderer.setContentAreaFilled(false);
			forceDefaultRenderer.addItemListener((ItemEvent e) -> configuration.setRendererForceDefault((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(forceDefaultRenderer).at(FormLayoutUtil.flip(cc.xy(7, ypos), colSpec, orientation));

			ypos += 2;

			// External network
			builder.addSeparator(Messages.getGuiString("ExternalOutgoingTraffic")).at(FormLayoutUtil.flip(cc.xyw(1, ypos, 9), colSpec, orientation));
			ypos += 2;
			JCheckBox extNetBox = new JCheckBox(Messages.getGuiString("EnableExternalNetwork"), configuration.getExternalNetwork());
			extNetBox.setToolTipText(Messages.getGuiString("ThisControlsWhetherUmsTry"));
			extNetBox.setContentAreaFilled(false);
			extNetBox.addItemListener((ItemEvent e) -> {
				boolean checked = (e.getStateChange() == ItemEvent.SELECTED);
				configuration.setExternalNetwork(checked);
				isUseInfoFromAPI.setEnabled(checked);
				useInfoFromTMDB.setEnabled(checked);
				tmdbApiKey.setEnabled(checked && configuration.isUseInfoFromTMDB());
				tmdbApiKeyLabel.setEnabled(checked && configuration.isUseInfoFromTMDB());
			});
			builder.add(extNetBox).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;
			isUseInfoFromAPI = new JCheckBox(Messages.getGuiString("UseInfoFromOurApi"), configuration.isUseInfoFromIMDb());
			isUseInfoFromAPI.setToolTipText(Messages.getGuiString("UsesInformationApiAllowBrowsing"));
			isUseInfoFromAPI.setContentAreaFilled(false);
			isUseInfoFromAPI.setEnabled(configuration.getExternalNetwork());
			isUseInfoFromAPI.addItemListener((ItemEvent e) -> configuration.setUseInfoFromIMDb((e.getStateChange() == ItemEvent.SELECTED)));
			builder.add(isUseInfoFromAPI).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			ypos += 2;
			useInfoFromTMDB = new JCheckBox(Messages.getGuiString("UseInfoFromTMDB"), configuration.isUseInfoFromTMDB());
			useInfoFromTMDB.setContentAreaFilled(false);
			useInfoFromTMDB.setEnabled(configuration.getExternalNetwork());
			useInfoFromTMDB.addItemListener((ItemEvent e) -> {
				boolean checked = (e.getStateChange() == ItemEvent.SELECTED);
				configuration.setUseInfoFromTMDB(checked);
				tmdbApiKey.setEnabled(checked);
				tmdbApiKeyLabel.setEnabled(checked);
			});
			builder.add(useInfoFromTMDB).at(FormLayoutUtil.flip(cc.xy(1, ypos), colSpec, orientation));
			tmdbApiKeyLabel = new JLabel(Messages.getGuiString("TMDBApiKey"), SwingConstants.TRAILING);
			tmdbApiKeyLabel.setToolTipText(Messages.getGuiString("ToRegisterTmdbApiKey"));
			tmdbApiKeyLabel.setEnabled(configuration.getExternalNetwork() && configuration.isUseInfoFromTMDB());
			builder.add(tmdbApiKeyLabel).at(FormLayoutUtil.flip(cc.xy(3, ypos), colSpec, orientation));
			tmdbApiKey = new JTextField(configuration.getTmdbApiKey());
			tmdbApiKey.setEnabled(configuration.getExternalNetwork() && configuration.isUseInfoFromTMDB());
			tmdbApiKey.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					configuration.setTmdbApiKey(tmdbApiKey.getText());
				}
			});
			builder.add(tmdbApiKey).at(FormLayoutUtil.flip(cc.xy(5, ypos), colSpec, orientation));
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
		if (System.getProperty(JavaGui.START_SERVICE) != null || !Platform.isWindows()) {
			installService.setEnabled(false);
			installService.setText(Messages.getGuiString("InstallAsWindowsService"));
		} else {
			installService.setEnabled(true);

			boolean isUmsServiceInstalled = WindowsUtils.isUmsServiceInstalled();

			if (isUmsServiceInstalled) {
				// Update button text and tooltip
				installService.setText(Messages.getGuiString("UninstallWindowsService"));
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

					JOptionPane.showMessageDialog(looksFrame,
						Messages.getGuiString("UninstalledWindowsService"),
						Messages.getGuiString("Information"),
						JOptionPane.INFORMATION_MESSAGE
					);
				});
			} else {
				// Update button text and tooltip
				installService.setText(Messages.getGuiString("InstallAsWindowsService"));
				installService.setToolTipText(Messages.getGuiString("NotRecommendedJustStartMinimized"));

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

						JOptionPane.showMessageDialog(looksFrame,
							Messages.getGuiString("YouHaveInstalledWindowsService") +
								Messages.getGuiString("ThenStartServiceWindows"),
							Messages.getGuiString("Information"),
							JOptionPane.INFORMATION_MESSAGE
						);
					} else {
						JOptionPane.showMessageDialog(looksFrame,
							Messages.getGuiString("CouldNotInstallWindowsService"),
							Messages.getGuiString("Error"),
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
		nameValues.add(Messages.getGuiString("UnknownRenderer"));

		if (!allConfs.isEmpty()) {
			RendererConfigurations.sortRendererConfigurationsByName(allConfs);
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
					renderersKcbm.getSelectedKey().isEmpty() ? "Unknown renderer" :
						renderersKcbm.getSelectedKey()
				);
			}
		});
	}

}
