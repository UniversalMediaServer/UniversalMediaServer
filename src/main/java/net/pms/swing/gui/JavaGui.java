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
package net.pms.swing.gui;

import com.jgoodies.looks.Options;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.FontUIResource;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.GuiConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.external.update.AutoUpdater;
import net.pms.gui.EConnectionState;
import net.pms.gui.IGui;
import net.pms.network.mediaserver.MediaServer;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.swing.AutoUpdateDialog;
import net.pms.swing.SwingUtil;
import net.pms.swing.components.AnimatedIcon;
import net.pms.swing.components.AnimatedIcon.AnimatedIconStage;
import net.pms.swing.components.AnimatedIcon.AnimatedIconType;
import net.pms.swing.components.CustomTabbedPaneUI;
import net.pms.swing.components.JAnimatedButton;
import net.pms.swing.components.JImageButton;
import net.pms.swing.gui.tabs.about.AboutTab;
import net.pms.swing.gui.tabs.general.GeneralTab;
import net.pms.swing.gui.tabs.help.HelpTab;
import net.pms.swing.gui.tabs.navigation.NavigationShareTab;
import net.pms.swing.gui.tabs.shared.SharedContentTab;
import net.pms.swing.gui.tabs.status.StatusTab;
import net.pms.swing.gui.tabs.traces.TracesTab;
import net.pms.swing.gui.tabs.transcoding.TranscodingTab;
import net.pms.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaGui extends JFrame implements IGui {

	private static final long serialVersionUID = 8723727186288427690L;
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaGui.class);
	private static final String ICON_BUTTON_QUIT = "button-quit." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_BUTTON_RESTART = "button-restart." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_BUTTON_RESTART_REQUIRED = "button-restart-requiredF%d." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_BUTTON_WIF = "button-wif." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final Dimension STANDARD_SIZE = new Dimension(1000, 750);
	private static final Dimension MINIMUM_SIZE = new Dimension(640, 480);
	public static final String START_SERVICE = "start.service";

	private final AutoUpdater autoUpdater;
	private final UmsConfiguration configuration;
	private final GuiConfiguration guiConfiguration;
	private final JAnimatedButton reload = createAnimatedToolBarButton(Messages.getGuiString("RestartServer"), ICON_BUTTON_RESTART);
	private final AnimatedIcon restartRequiredIcon = new AnimatedIcon(
			reload, true, AnimatedIcon.buildAnimation(ICON_BUTTON_RESTART_REQUIRED, 0, 25, true, 800, 300, 15)
	);

	private JTabbedPane tabbedPane;
	private GeneralTab generalTab;
	private NavigationShareTab navigationSettingsTab;
	private StatusTab statusTab;
	private TracesTab tracesTab;
	private SharedContentTab sharedContentTab;
	private TranscodingTab transcodingTab;
	private AboutTab aboutTab;
	private AnimatedIcon restartIcon;
	private AbstractButton webinterface;
	private JLabel status;
	private ViewLevel viewLevel = ViewLevel.UNKNOWN;
	private String statusLine = null;
	private Rectangle effectiveScreenBounds;
	private AbstractButton quit;

	/**
	 * Constructs a <code>SwingGui</code>, configures the UI, and builds the
	 * content.
	 */
	public JavaGui(@Nonnull UmsConfiguration configuration, @Nonnull GuiConfiguration guiConfiguration) {
		super(guiConfiguration.getGraphicsConfiguration());
		if (configuration == null) {
			throw new IllegalArgumentException("configuration can't be null");
		}
		setResizable(true);
		this.guiConfiguration = guiConfiguration;
		setGuiConfiguration();
		this.autoUpdater = PMS.getAutoUpdater();
		this.configuration = configuration;
		assert this.configuration != null;
		Options.setDefaultIconSize(new Dimension(18, 18));
		Options.setUseNarrowButtons(true);

		// Set view level, can be omitted if ViewLevel is implemented in configuration
		// by setting the view level as variable initialization
		if (configuration.isHideAdvancedOptions()) {
			viewLevel = ViewLevel.NORMAL;
		} else {
			viewLevel = ViewLevel.ADVANCED;
		}

		// Global options
		Options.setTabIconsEnabled(true);
		UIManager.put(Options.POPUP_DROP_SHADOW_ENABLED_KEY, null);

		// Swing Settings
		SwingUtil.initializeLookAndFeel();

		// wait until the look and feel has been initialized before (possibly) displaying the update notification dialog
		if (autoUpdater != null) {
			autoUpdater.pollServer();
		}

		// Shared Fonts
		final Integer twelve = 12;
		final Integer fontPlain = Font.PLAIN;
		final Integer fontBold = Font.BOLD;

		LazyValue dialogPlain12 = (UIDefaults t) -> new FontUIResource(Font.DIALOG, fontPlain, twelve);

		LazyValue sansSerifPlain12 = (UIDefaults t) -> new FontUIResource(Font.SANS_SERIF, fontPlain, twelve);

		LazyValue monospacedPlain12 = (UIDefaults t) -> new FontUIResource(Font.MONOSPACED, fontPlain, twelve);

		LazyValue dialogBold12 = (UIDefaults t) -> new FontUIResource(Font.DIALOG, fontBold, twelve);

		Object menuFont = dialogPlain12;
		Object fixedControlFont = monospacedPlain12;
		Object controlFont = dialogPlain12;
		Object messageFont = dialogPlain12;
		Object windowFont = dialogBold12;
		Object toolTipFont = sansSerifPlain12;
		Object iconFont = controlFont;

		// Override our fonts with a unicode font for languages with special characters
		final String language = configuration.getLanguageTag();
		if (language != null && (language.equals("ja") || language.startsWith("zh") || language.equals("ko"))) {
			// http://propedit.sourceforge.jp/propertieseditor.jnlp
			menuFont = sansSerifPlain12;
			fixedControlFont = sansSerifPlain12;
			controlFont = sansSerifPlain12;
			messageFont = sansSerifPlain12;
			windowFont = sansSerifPlain12;
			iconFont = sansSerifPlain12;
		}

		UIManager.put("Button.font", controlFont);
		UIManager.put("CheckBox.font", controlFont);
		UIManager.put("CheckBoxMenuItem.font", menuFont);
		UIManager.put("ComboBox.font", controlFont);
		UIManager.put("EditorPane.font", controlFont);
		UIManager.put("FileChooser.listFont", iconFont);
		UIManager.put("FormattedTextField.font", controlFont);
		UIManager.put("InternalFrame.titleFont", windowFont);
		UIManager.put("Label.font", controlFont);
		UIManager.put("List.font", controlFont);
		UIManager.put("PopupMenu.font", menuFont);
		UIManager.put("Menu.font", menuFont);
		UIManager.put("MenuBar.font", menuFont);
		UIManager.put("MenuItem.font", menuFont);
		UIManager.put("MenuItem.acceleratorFont", menuFont);
		UIManager.put("RadioButton.font", controlFont);
		UIManager.put("RadioButtonMenuItem.font", menuFont);
		UIManager.put("OptionPane.font", messageFont);
		UIManager.put("OptionPane.messageFont", messageFont);
		UIManager.put("OptionPane.buttonFont", messageFont);
		UIManager.put("Panel.font", controlFont);
		UIManager.put("PasswordField.font", controlFont);
		UIManager.put("ProgressBar.font", controlFont);
		UIManager.put("ScrollPane.font", controlFont);
		UIManager.put("Slider.font", controlFont);
		UIManager.put("Spinner.font", controlFont);
		UIManager.put("TabbedPane.font", controlFont);
		UIManager.put("Table.font", controlFont);
		UIManager.put("TableHeader.font", controlFont);
		UIManager.put("TextArea.font", fixedControlFont);
		UIManager.put("TextField.font", controlFont);
		UIManager.put("TextPane.font", controlFont);
		UIManager.put("TitledBorder.font", controlFont);
		UIManager.put("ToggleButton.font", controlFont);
		UIManager.put("ToolBar.font", menuFont);
		UIManager.put("ToolTip.font", toolTipFont);
		UIManager.put("Tree.font", controlFont);
		UIManager.put("Viewport.font", controlFont);

		setTitle(PMS.NAME);
		Image image = SwingUtil.getAppIconImage();
		if (image != null) {
			setIconImage(image);
		}

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JComponent jp = buildContent();
		String showScrollbars = System.getProperty("scrollbars", "").toLowerCase();

		/**
		 * Handle scrollbars:
		 *
		 * 1) forced scrollbars (-Dscrollbars=true): always display them 2)
		 * optional scrollbars (-Dscrollbars=optional): display them as needed
		 * 3) otherwise (default): don't display them
		 */
		switch (showScrollbars) {
			case "true" ->
				setContentPane(
						new JScrollPane(
								jp,
								ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
								ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
						)
				);
			case "optional" ->
				setContentPane(
						new JScrollPane(
								jp,
								ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
								ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
						)
				);
			default ->
				setContentPane(jp);
		}

		setTitle();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		if (Platform.isMac()) {
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					setExtendedState(Frame.ICONIFIED);
				}
			});
		}

		// Display tooltips immediately and for a long time
		ToolTipManager.sharedInstance().setInitialDelay(400);
		ToolTipManager.sharedInstance().setDismissDelay(60000);
		ToolTipManager.sharedInstance().setReshowDelay(400);

		if (!configuration.isMinimized() && System.getProperty(START_SERVICE) == null) {
			setVisible(true);
		}

		if (configuration.isMinimized() && Platform.isMac()) {
			// setVisible is required to iconify the frame
			setVisible(true);
			setExtendedState(Frame.ICONIFIED);
		}

		if (configuration.isAutoUpdate()) {
			// give the GUI 5 seconds to start before checking for updates
			Timer t = new Timer();
			t.schedule(
					new java.util.TimerTask() {
				@Override
				public void run() {
					checkForUpdates(true);
				}
			},
					5000
			);
		}
	}

	private void setTitle() {
		String projectName = PropertiesUtil.getProjectProperties().get("project.name");
		String projectVersion = PropertiesUtil.getProjectProperties().get("project.version");
		String title = projectName + " " + projectVersion;

		// If the version contains a "-" (e.g. "1.50.1-SNAPSHOT" or "1.50.1-beta1"), add a warning message
		if (projectVersion.indexOf('-') > -1) {
			title = title + " - " + Messages.getGuiString("ForTestingOnly");
		}

		if (PMS.getTraceMode() == 2) {
			// Forced trace mode
			title = title + "  [" + Messages.getGuiString("Trace").toUpperCase() + "]";
		}

		setTitle(title);
	}

	private void setGuiConfiguration() {
		setMinimumSize(MINIMUM_SIZE);
		if (guiConfiguration != null) {
			Rectangle screenBounds = guiConfiguration.getScreenBounds();
			Rectangle windowBounds = guiConfiguration.getWindowBounds();
			String graphicsDevice = guiConfiguration.getGraphicsDevice();
			updateEffectiveScreenBounds(screenBounds, guiConfiguration.getScreenInsets());
			GraphicsConfiguration windowGraphicsConfiguration = getGraphicsConfiguration();
			if (windowBounds != null &&
					effectiveScreenBounds != null &&
					graphicsDevice != null &&
					graphicsDevice.equals(windowGraphicsConfiguration.getDevice().getIDstring()) &&
					screenBounds != null &&
					screenBounds.equals(windowGraphicsConfiguration.getBounds())) {
				setWindowBounds(windowBounds);
			} else {
				Rectangle screen = effectiveScreenBounds != null ? effectiveScreenBounds : windowGraphicsConfiguration.getBounds();
				if (screen.width >= STANDARD_SIZE.width && screen.height >= STANDARD_SIZE.height) {
					setSize(STANDARD_SIZE);
				} else if (getWidth() < MINIMUM_SIZE.width || getHeight() < MINIMUM_SIZE.getHeight()) {
					setSize(MINIMUM_SIZE);
				}
				setLocationByPlatform(true);
			}
			// Set maximized state
			int maximizedState = guiConfiguration.getWindowState() & Frame.MAXIMIZED_BOTH;
			if (maximizedState != 0) {
				setExtendedState(getExtendedState() | maximizedState);
			}
			//save the window config on change
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					if (e.getSource() instanceof JavaGui) {
						updateGuiConfiguration();
					}
				}
			});
			addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {
						if (e.getSource() instanceof JavaGui) {
							updateGuiConfiguration();
						}
					}
					@Override
					public void componentMoved(ComponentEvent e) {
						if (e.getSource() instanceof JavaGui) {
							updateGuiConfiguration();
						}
					}
				});
		}
	}

	public ViewLevel getViewLevel() {
		return viewLevel;
	}

	public void setViewLevel(ViewLevel viewLevel) {
		if (viewLevel != ViewLevel.UNKNOWN) {
			this.viewLevel = viewLevel;
			applyViewLevel();
		}
	}

	public TranscodingTab getTranscodingTab() {
		return transcodingTab;
	}

	@Override
	public void enableWebUiButton() {
		if (PMS.getConfiguration().useWebPlayerServer()) {
			webinterface.setEnabled(true);
		}
	}

	public static URL getImageResource(String filename) {
		return SwingUtil.getImageResource("swing/" + filename);
	}

	public static ImageIcon readImageIcon(String filename) {
		return SwingUtil.getImageIcon("swing/" + filename);
	}

	public final JComponent buildContent() {
		JPanel panel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		toolBar.add(new JPanel());

		if (PMS.getConfiguration().useWebPlayerServer()) {
			webinterface = createToolBarButton(Messages.getGuiString("WebSettings"), ICON_BUTTON_WIF, Messages.getGuiString("ThisLaunchesOurWebSettings"));
			webinterface.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			webinterface.addActionListener((ActionEvent e) -> {
				String error = null;
				if (PMS.get().getGuiServer() != null && StringUtils.isNotBlank(PMS.get().getGuiServer().getUrl())) {
					try {
						URI uri = new URI(PMS.get().getGuiServer().getUrl());
						if (!PlatformUtils.INSTANCE.browseURI(uri.toString())) {
							error = Messages.getGuiString("ErrorOccurredTryingLaunchBrowser");
						}
					} catch (URISyntaxException se) {
						LOGGER.error(
								"Could not form a valid web gui server URI from \"{}\": {}",
								PMS.get().getGuiServer().getUrl(),
								se.getMessage()
						);
						LOGGER.trace("", se);
						error = Messages.getGuiString("CouldNotFormValidUrl");
					}
				} else {
					error = Messages.getGuiString("CouldNotFormValidUrl");
				}
				if (error != null) {
					JOptionPane.showMessageDialog(null, error, Messages.getGuiString("Error"), JOptionPane.ERROR_MESSAGE);
				}
			});
			webinterface.setEnabled(false);
			toolBar.add(webinterface);
			toolBar.addSeparator(new Dimension(20, 1));
		}

		restartIcon = (AnimatedIcon) reload.getIcon();
		restartRequiredIcon.start();
		setReloadable(false);
		reload.addActionListener((ActionEvent e) -> {
			reload.setEnabled(false);
			PMS.get().resetMediaServer();
		});
		reload.setToolTipText(Messages.getGuiString("ThisRestartsMediaServices"));
		reload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toolBar.add(reload);

		toolBar.addSeparator(new Dimension(20, 1));
		quit = createToolBarButton(Messages.getGuiString("Quit"), ICON_BUTTON_QUIT);
		quit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		quit.addActionListener((ActionEvent e) -> PMS.quit());
		quit.getSize();
		toolBar.add(quit);
		if (System.getProperty(START_SERVICE) != null) {
			quit.setEnabled(false);
		}
		toolBar.add(new JPanel());

		// Apply the orientation to the toolbar and all components in it
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		toolBar.applyComponentOrientation(orientation);
		toolBar.setBorder(new EmptyBorder(new Insets(8, 0, 0, 0)));

		panel.add(toolBar, BorderLayout.NORTH);
		panel.add(buildMain(), BorderLayout.CENTER);
		status = new JLabel("");
		status.setComponentOrientation(orientation);
		status.setBorder(BorderFactory.createEmptyBorder(0, 9, 8, 0));

		// Calling applyComponentOrientation() here would be ideal.
		// Alas it horribly mutilates the layout of several tabs.
		//panel.applyComponentOrientation(orientation);
		panel.add(status, BorderLayout.SOUTH);

		return panel;
	}

	public JComponent buildMain() {
		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		tabbedPane.setUI(new CustomTabbedPaneUI());

		statusTab = new StatusTab();
		tracesTab = new TracesTab(configuration, this);
		generalTab = new GeneralTab(configuration, this);
		navigationSettingsTab = new NavigationShareTab(configuration, this);
		sharedContentTab = new SharedContentTab(configuration, this);
		transcodingTab = new TranscodingTab(configuration, this);
		HelpTab helpTab = new HelpTab();
		aboutTab = new AboutTab();

		tabbedPane.addTab(Messages.getGuiString("Status"), statusTab.build());
		tabbedPane.addTab(Messages.getGuiString("Logs"), tracesTab.build());
		tabbedPane.addTab(Messages.getGuiString("GeneralSettings"), generalTab.build());
		if (!configuration.isHideAdvancedOptions()) {
			tabbedPane.addTab(Messages.getGuiString("NavigationSettings"), navigationSettingsTab.build());
		}
		tabbedPane.addTab(Messages.getGuiString("SharedContent"), sharedContentTab.build());
		tabbedPane.addTab(Messages.getGuiString("TranscodingSettings"), transcodingTab.build());
		tabbedPane.addTab(Messages.getGuiString("Help"), helpTab.build());
		tabbedPane.addTab(Messages.getGuiString("About"), aboutTab.build());
		tabbedPane.addChangeListener((ChangeEvent e) -> {
			int helpIndex = tabbedPane.getSelectedIndex();
			if (configuration.isHideAdvancedOptions() && helpIndex > 2) {
				helpIndex++;
			}
			helpTab.setTabIndex(helpIndex);
		});

		tabbedPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		/*
		 * Set the orientation of the tabbedPane.
		 * Note: Do not use applyComponentOrientation() here because it
		 * messes with the layout of several tabs.
		 */
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		tabbedPane.setComponentOrientation(orientation);
		return tabbedPane;
	}

	private void applyViewLevel() {
		//status tab is not impacted
		tracesTab.applyViewLevel();
		tabbedPane.setComponentAt(2, generalTab.build());
		if (configuration.isHideAdvancedOptions()) {
			//remove navigationSettingsTab
			if (tabbedPane.getComponentAt(3) == navigationSettingsTab.getComponent()) {
				tabbedPane.remove(3);
			}
			tabbedPane.setComponentAt(4, transcodingTab.build());
		} else {
			//add navigationSettingsTab
			if (tabbedPane.getComponentAt(3) != navigationSettingsTab.getComponent()) {
				tabbedPane.insertTab(Messages.getGuiString("NavigationSettings"), null, navigationSettingsTab.build(), null, 3);
			}
			tabbedPane.setComponentAt(5, transcodingTab.build());
		}
	}

	public void applyLanguage() {
		setTitle();
		webinterface.setText(Messages.getGuiString("WebSettings"));
		webinterface.setToolTipText(Messages.getGuiString("ThisLaunchesOurWebSettings"));
		reload.setText(Messages.getGuiString("RestartServer"));
		if (reload.getIcon() == restartRequiredIcon) {
			reload.setToolTipText(Messages.getGuiString("TheServerHasToRestarted"));
		} else {
			reload.setToolTipText(Messages.getGuiString("ThisRestartsMediaServices"));
		}
		quit.setText(Messages.getGuiString("Quit"));
		int tabIndex = 0;
		tabbedPane.setTitleAt(tabIndex, Messages.getGuiString("Status"));
		statusTab.applyLanguage();
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("Logs"));
		tracesTab.applyLanguage();
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("GeneralSettings"));
		tabbedPane.setComponentAt(tabIndex, generalTab.build());
		if (!configuration.isHideAdvancedOptions()) {
			tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("NavigationSettings"));
			tabbedPane.setComponentAt(tabIndex, navigationSettingsTab.build());
		}
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("SharedContent"));
		tabbedPane.setComponentAt(tabIndex, sharedContentTab.build());
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("TranscodingSettings"));
		tabbedPane.setComponentAt(tabIndex, transcodingTab.build());
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("Help"));
		//help is not translated
		tabbedPane.setTitleAt(++tabIndex, Messages.getGuiString("About"));
		tabbedPane.setComponentAt(tabIndex, aboutTab.build());
	}

	protected JImageButton createToolBarButton(String text, String iconName) {
		JImageButton button = new JImageButton(text, iconName);
		button.setFocusable(false);
		return button;
	}

	protected JAnimatedButton createAnimatedToolBarButton(String text, String iconName) {
		JAnimatedButton button = new JAnimatedButton(text, iconName);
		button.setFocusable(false);
		return button;
	}

	protected JImageButton createToolBarButton(String text, String iconName, String toolTipText) {
		JImageButton button = createToolBarButton(text, iconName);
		button.setToolTipText(toolTipText);
		button.setBorderPainted(false);
		return button;
	}

	@Override
	public void appendLog(final String msg) {
		SwingUtilities.invokeLater(() -> tracesTab.append(msg));
	}

	@Override
	public void setConnectionState(final EConnectionState connectionState) {
		SwingUtilities.invokeLater(() -> statusTab.setConnectionState(connectionState));
	}

	@Override
	public void setCurrentBitrate(int sizeinMb) {
		statusTab.setCurrentBitrate(sizeinMb);
	}

	@Override
	public void setPeakBitrate(int sizeinMb) {
		statusTab.setPeakBitrate(sizeinMb);
	}

	/**
	 * This method is being called when a configuration change requiring a
	 * restart of the HTTP server has been done by the user. It should notify
	 * the user to restart the server.<br>
	 * Currently the icon as well as the tool tip text of the restart button is
	 * being changed.<br>
	 * The actions requiring a server restart are defined by
	 * {@link UmsConfiguration#NEED_RELOAD_FLAGS}
	 *
	 * @param required true if the server has to be restarted, false otherwise
	 */
	@Override
	public void setReloadable(final boolean required) {
		SwingUtilities.invokeLater(() -> {
			if (required) {
				if (reload.getIcon() == restartIcon) {
					restartIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, restartRequiredIcon, false));
					reload.setToolTipText(Messages.getGuiString("TheServerHasToRestarted"));
				}
			} else {
				if (restartRequiredIcon == reload.getIcon()) {
					reload.setToolTipText(Messages.getGuiString("ThisRestartsMediaServices"));
					restartRequiredIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, restartIcon, false));
				}
			}
			reload.setEnabled(true);
		});
	}

	@Override
	public void addEngines() {
		transcodingTab.addEngines();
	}

	/**
	 * Start the process of checking for updates.
	 *
	 * @param isStartup whether this is being called via startup or button
	 */
	public void checkForUpdates(boolean isStartup) {
		if (autoUpdater != null) {
			try {
				if (!isStartup) {
					autoUpdater.pollServer();
				}
				AutoUpdateDialog.showIfNecessary(this, autoUpdater, isStartup);
			} catch (NoClassDefFoundError ncdfe) {
				LOGGER.error("Error displaying AutoUpdateDialog", ncdfe);
			}
		}
	}

	@Override
	public void setStatusLine(String line) {
		statusLine = line;

		if (line == null) {
			line = " ";
		}

		status.setBorder(BorderFactory.createEmptyBorder(0, 12, 9, 0));
		status.setText(line);
	}

	/**
	 * Sets a secondary status line. If it receives null, it will try to set the
	 * primary status line if it exists, otherwise clear it.
	 *
	 * @param line
	 */
	@Override
	public void setSecondaryStatusLine(String line) {
		if (line == null) {
			if (statusLine != null) {
				line = statusLine;
			} else {
				line = " ";
			}
		}

		status.setBorder(BorderFactory.createEmptyBorder(0, 12, 9, 0));
		status.setText(line);
	}

	@Override
	public void addRenderer(Renderer renderer) {
		statusTab.addRenderer(renderer);
	}

	@Override
	public void serverReady() {
		generalTab.addRenderers();
	}

	@Override
	public void updateServerStatus() {
		if (MediaServer.isStarted()) {
			statusTab.setMediaServerBind(MediaServer.getAddress());
		} else {
			statusTab.setMediaServerBind("-");
		}
		if (PMS.get().getWebPlayerServer() != null && PMS.get().getWebPlayerServer().getServer() != null) {
			statusTab.setInterfaceServerBind(PMS.get().getWebPlayerServer().getAddress());
		} else {
			statusTab.setInterfaceServerBind("-");
		}
	}

	@Override
	public void setMediaScanStatus(boolean running) {
		sharedContentTab.setMediaScanEnabled(running);
	}

	/**
	 * Show error message with swing
	 *
	 * @param message the message to display
	 * @param title the title string for the dialog
	 */
	@Override
	public void showErrorMessage(String message, String title) {
		JOptionPane.showMessageDialog(
				(SwingUtilities.getWindowAncestor(this)),
				message,
				title,
				JOptionPane.ERROR_MESSAGE
		);
	}

	@Override
	public void setConfigurationChanged(String key) {
		//nothing to do
	}

	@Override
	public void setMemoryUsage(int maxMemory, int usedMemory, int dbCacheMemory, int bufferMemory) {
		statusTab.setMemoryUsage(maxMemory, usedMemory, dbCacheMemory, bufferMemory);
	}

	private void updateGuiConfiguration() {
		if (guiConfiguration == null) {
			return;
		}
		//update window state
		int state = getExtendedState();
		guiConfiguration.setWindowState(state);
		//update window bound
		if (state == 0) {
			guiConfiguration.setWindowBounds(getBounds());
		} else if ((state & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {
			Rectangle windowBounds = getBounds();
			Rectangle storedBounds = guiConfiguration.getWindowBounds();
			// Don't store maximized dimensions
			if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
				windowBounds.x = storedBounds.x;
				windowBounds.width = storedBounds.width;
			} else if ((state & Frame.MAXIMIZED_VERT) != 0) {
				windowBounds.y = storedBounds.y;
				windowBounds.height = storedBounds.height;
			}
			guiConfiguration.setWindowBounds(windowBounds);
		}

		//update screen bounds
		Rectangle screenBounds = getGraphicsConfiguration().getBounds();
		guiConfiguration.setScreenBounds(screenBounds);
		//update screen insets
		Insets screenInsets = getToolkit().getScreenInsets(getGraphicsConfiguration());
		guiConfiguration.setScreenInsets(screenInsets);
		if (guiConfiguration.isDirty() && updateEffectiveScreenBounds(screenBounds, screenInsets)) {
			Rectangle windowBounds = guiConfiguration.getWindowBounds();
			if (windowBounds != null && effectiveScreenBounds != null && !effectiveScreenBounds.contains(windowBounds)) {
				setWindowBounds(windowBounds);
			}
		}

		//update device
		String deviceString = getGraphicsConfiguration().getDevice().getIDstring();
		guiConfiguration.setGraphicsDevice(deviceString);

		//save if needed
		guiConfiguration.save();
	}

	private boolean updateEffectiveScreenBounds(Rectangle screenBounds, Insets screenInsets) {
		if (screenBounds == null) {
			return false;
		}
		Insets tmpInsets = screenInsets == null ? new Insets(0, 0, 0, 0) : screenInsets;
		Rectangle newEffectiveScreenBounds = new Rectangle(
				screenBounds.x,
				screenBounds.y,
				screenBounds.width - tmpInsets.left - tmpInsets.right,
				screenBounds.height - tmpInsets.top - tmpInsets.bottom
		);
		if (!newEffectiveScreenBounds.equals(effectiveScreenBounds)) {
			effectiveScreenBounds = newEffectiveScreenBounds;
			return true;
		}
		return false;
	}

	private void setWindowBounds(Rectangle windowBounds) {
		if (windowBounds == null) {
			return;
		}
		if (effectiveScreenBounds == null) {
			setBounds(windowBounds);
			return;
		}
		int deltaX = 0;
		if (windowBounds.x + windowBounds.width > effectiveScreenBounds.x + effectiveScreenBounds.width) {
			deltaX = effectiveScreenBounds.x + effectiveScreenBounds.width - windowBounds.x - windowBounds.width;
		}
		if (windowBounds.x < effectiveScreenBounds.x) {
			deltaX = effectiveScreenBounds.x - windowBounds.x;
		}
		int deltaY = 0;
		if (windowBounds.y + windowBounds.height > effectiveScreenBounds.y + effectiveScreenBounds.height) {
			deltaY = effectiveScreenBounds.y + effectiveScreenBounds.height - windowBounds.y - windowBounds.height;
		}
		if (windowBounds.y < effectiveScreenBounds.y) {
			deltaY = effectiveScreenBounds.y - windowBounds.y;
		}
		if (deltaX != 0 || deltaY != 0) {
			windowBounds.translate(deltaX, deltaY);
		}
		if (!effectiveScreenBounds.contains(windowBounds)) {
			Rectangle newWindowBounds = windowBounds.intersection(effectiveScreenBounds);
			if (newWindowBounds.width < MINIMUM_SIZE.width) {
				newWindowBounds.width = MINIMUM_SIZE.width;
			}
			if (newWindowBounds.height < MINIMUM_SIZE.height) {
				newWindowBounds.height = MINIMUM_SIZE.height;
			}
			windowBounds = newWindowBounds;
		}
		setBounds(windowBounds);
	}

}
