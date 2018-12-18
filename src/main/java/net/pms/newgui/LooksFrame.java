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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.io.BasicSystemUtils;
import net.pms.io.WindowsNamedPipe;
import net.pms.newgui.StatusTab.ConnectionState;
import net.pms.newgui.components.AnimatedIcon;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconStage;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconType;
import net.pms.newgui.components.WindowProperties.WindowPropertiesConfiguration;
import net.pms.newgui.components.JAnimatedButton;
import net.pms.newgui.components.JImageButton;
import net.pms.newgui.components.WindowProperties;
import net.pms.newgui.update.AutoUpdateDialog;
import net.pms.update.AutoUpdater;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LooksFrame extends JFrame implements IFrame, Observer {
	private static final Logger LOGGER = LoggerFactory.getLogger(LooksFrame.class);

	private final AutoUpdater autoUpdater;
	private final PmsConfiguration configuration;
	public static final String START_SERVICE = "start.service";
	private final WindowProperties windowProperties;
	private static final long serialVersionUID = 8723727186288427690L;
	protected static final Dimension STANDARD_SIZE = new Dimension(1000, 750);
	protected static final Dimension MINIMUM_SIZE = new Dimension(640, 480);

	/**
	 * List of context sensitive help pages URLs. These URLs should be
	 * relative to the documentation directory and in the same order as the
	 * tabs. The value <code>null</code> means "don't care", activating the
	 * tab will not change the help page.
	 */
	protected static final String[] HELP_PAGES = {
		"index.html",
		null,
		"general_configuration.html",
		null,
		"navigation_share.html",
		"transcoding.html",
		null,
		null
	};

	private NavigationShareTab nt;
	private StatusTab st;
	private TracesTab tt;
	private TranscodingTab tr;
	private GeneralTab gt;
	private HelpTab ht;
	private final JAnimatedButton reload = createAnimatedToolBarButton(Messages.getString("LooksFrame.12"), "button-restart.png");;
	private final AnimatedIcon restartRequredIcon = new AnimatedIcon(
		reload, true, AnimatedIcon.buildAnimation("button-restart-requiredF%d.png", 0, 24, true, 800, 300, 15)
	);
	private AnimatedIcon restartIcon;
	private AbstractButton webinterface;
	private JLabel status;
	private static Object lookAndFeelInitializedLock = new Object();
	private static boolean lookAndFeelInitialized = false;
	private ViewLevel viewLevel = ViewLevel.UNKNOWN;

	/**
	 * Class name of Windows L&F provided in Sun JDK.
	 */
	public static final String WINDOWS_LNF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

	/**
	 * Class name of PlasticXP L&F.
	 */
	public static final String PLASTICXP_LNF = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";

	/**
	 * Class name of Metal L&F.
	 */
	public static final String METAL_LNF = "javax.swing.plaf.metal.MetalLookAndFeel";

	public ViewLevel getViewLevel() {
		return viewLevel;
	}

	public void setViewLevel(ViewLevel viewLevel) {
		if (viewLevel != ViewLevel.UNKNOWN){
			this.viewLevel = viewLevel;
			tt.applyViewLevel();
		}
	}

	public TracesTab getTt() {
		return tt;
	}

	public NavigationShareTab getNt() {
		return nt;
	}

	public TranscodingTab getTr() {
		return tr;
	}

	public GeneralTab getGt() {
		return gt;
	}

	public static void initializeLookAndFeel() {
		synchronized (lookAndFeelInitializedLock) {
			if (lookAndFeelInitialized) {
				return;
			}

			if (Platform.isWindows()) {
				try {
					UIManager.setLookAndFeel(WINDOWS_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Windows look and feel: ", e);
				}
			} else if (System.getProperty("nativelook") == null && !Platform.isMac()) {
				try {
					UIManager.setLookAndFeel(PLASTICXP_LNF);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
					LOGGER.error("Error while setting Plastic XP look and feel: ", e);
				}
			} else {
				try {
					String systemClassName = UIManager.getSystemLookAndFeelClassName();
					// Workaround for Gnome
					try {
						String gtkLAF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
						Class.forName(gtkLAF);

						if (systemClassName.equals("javax.swing.plaf.metal.MetalLookAndFeel")) {
							systemClassName = gtkLAF;
						}
					} catch (ClassNotFoundException ce) {
						LOGGER.error("Error loading GTK look and feel: ", ce);
					}

					LOGGER.trace("Choosing Java look and feel: " + systemClassName);
					UIManager.setLookAndFeel(systemClassName);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
					try {
						UIManager.setLookAndFeel(PLASTICXP_LNF);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
						LOGGER.error("Error while setting Plastic XP look and feel: ", e);
					}
					LOGGER.error("Error while setting native look and feel: ", e1);
				}
			}

			if (isParticularLaFSet(UIManager.getLookAndFeel(), PLASTICXP_LNF)) {
				PlasticLookAndFeel.setPlasticTheme(PlasticLookAndFeel.createMyDefaultTheme());
				PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
				PlasticLookAndFeel.setHighContrastFocusColorsEnabled(false);
			} else if (isParticularLaFSet(UIManager.getLookAndFeel(), METAL_LNF)) {
				MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
			}

			// Work around caching in MetalRadioButtonUI
			JRadioButton radio = new JRadioButton();
			radio.getUI().uninstallUI(radio);
			JCheckBox checkBox = new JCheckBox();
			checkBox.getUI().uninstallUI(checkBox);

			// Workaround for JDK-8179014: JFileChooser with Windows look and feel crashes on win 10
			// https://bugs.openjdk.java.net/browse/JDK-8179014
			if (isParticularLaFSet(UIManager.getLookAndFeel(), WINDOWS_LNF)) {
				UIManager.put("FileChooser.useSystemExtensionHiding", false);
			}

			lookAndFeelInitialized = true;
		}
	}

	/**
	 * Safely checks whether a particular look and feel class is set.
	 *
	 * @param lnf
	 * @param lookAndFeelClassPath
	 * @return whether the incoming look and feel class is set
	 */
	private static boolean isParticularLaFSet(LookAndFeel lnf, String lookAndFeelClassPath) {
		// as of Java 10, com.sun.java.swing.plaf.windows.WindowsLookAndFeel
		// is no longer available on macOS
		// thus "instanceof WindowsLookAndFeel" directives will result
		// in a NoClassDefFoundError during runtime
		if (lnf == null) {
			return false;
		} else {
			try {
				Class c = Class.forName(lookAndFeelClassPath);
				return c.isInstance(lnf);
			} catch (ClassNotFoundException cnfe) {
				// if it is not possible to load the Windows LnF class, the
				// given lnf instance cannot be an instance of the Windows
				// LnF class
				return false;
			}
		}
	}

	/**
	 * Constructs a <code>DemoFrame</code>, configures the UI,
	 * and builds the content.
	 */
	public LooksFrame(AutoUpdater autoUpdater, @Nonnull PmsConfiguration configuration, @Nonnull WindowPropertiesConfiguration windowConfiguration) {
		super(windowConfiguration.getGraphicsConfiguration());
		if (configuration == null) {
			throw new IllegalArgumentException("configuration can't be null");
		}
		setResizable(true);
		windowProperties = new WindowProperties(this, STANDARD_SIZE, MINIMUM_SIZE, windowConfiguration);
		this.autoUpdater = autoUpdater;
		this.configuration = configuration;
		assert this.configuration != null;
		setMinimumSize(MINIMUM_SIZE);
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
		initializeLookAndFeel();

		// wait till the look and feel has been initialized before (possibly) displaying the update notification dialog
		if (autoUpdater != null) {
			autoUpdater.addObserver(this);
			autoUpdater.pollServer();
		}

		// Shared Fonts
		final Integer twelve = Integer.valueOf(12);
		final Integer fontPlain = Integer.valueOf(Font.PLAIN);
		final Integer fontBold = Integer.valueOf(Font.BOLD);

		LazyValue dialogPlain12 = new LazyValue() {
			@Override
			public Object createValue(UIDefaults t) {
				return new FontUIResource(Font.DIALOG, fontPlain, twelve);
			}
		};

		LazyValue sansSerifPlain12 =  new LazyValue() {
			@Override
			public Object createValue(UIDefaults t) {
				return new FontUIResource(Font.SANS_SERIF, fontPlain, twelve);
			}
		};

		LazyValue monospacedPlain12 = new LazyValue() {
			@Override
			public Object createValue(UIDefaults t) {
				return new FontUIResource(Font.MONOSPACED, fontPlain, twelve);
			}
		};

		LazyValue dialogBold12 = new LazyValue() {
			@Override
			public Object createValue(UIDefaults t) {
				return new FontUIResource(Font.DIALOG, fontBold, twelve);
			}
		};

		Object MenuFont = dialogPlain12;
		Object FixedControlFont = monospacedPlain12;
		Object ControlFont = dialogPlain12;
		Object MessageFont = dialogPlain12;
		Object WindowFont = dialogBold12;
		Object ToolTipFont = sansSerifPlain12;
		Object IconFont = ControlFont;

		// Override our fonts with a unicode font for languages with special characters
		final String language = configuration.getLanguageTag();
		if (language != null && (language.equals("ja") || language.startsWith("zh") || language.equals("ko"))) {
			// http://propedit.sourceforge.jp/propertieseditor.jnlp
			MenuFont = sansSerifPlain12;
			FixedControlFont = sansSerifPlain12;
			ControlFont = sansSerifPlain12;
			MessageFont = sansSerifPlain12;
			WindowFont = sansSerifPlain12;
			IconFont = sansSerifPlain12;
		}

		UIManager.put("Button.font", ControlFont);
		UIManager.put("CheckBox.font", ControlFont);
		UIManager.put("CheckBoxMenuItem.font", MenuFont);
		UIManager.put("ComboBox.font", ControlFont);
		UIManager.put("EditorPane.font", ControlFont);
		UIManager.put("FileChooser.listFont", IconFont);
		UIManager.put("FormattedTextField.font", ControlFont);
		UIManager.put("InternalFrame.titleFont", WindowFont);
		UIManager.put("Label.font", ControlFont);
		UIManager.put("List.font", ControlFont);
		UIManager.put("PopupMenu.font", MenuFont);
		UIManager.put("Menu.font", MenuFont);
		UIManager.put("MenuBar.font", MenuFont);
		UIManager.put("MenuItem.font", MenuFont);
		UIManager.put("MenuItem.acceleratorFont", MenuFont);
		UIManager.put("RadioButton.font", ControlFont);
		UIManager.put("RadioButtonMenuItem.font", MenuFont);
		UIManager.put("OptionPane.font", MessageFont);
		UIManager.put("OptionPane.messageFont", MessageFont);
		UIManager.put("OptionPane.buttonFont", MessageFont);
		UIManager.put("Panel.font", ControlFont);
		UIManager.put("PasswordField.font", ControlFont);
		UIManager.put("ProgressBar.font", ControlFont);
		UIManager.put("ScrollPane.font", ControlFont);
		UIManager.put("Slider.font", ControlFont);
		UIManager.put("Spinner.font", ControlFont);
		UIManager.put("TabbedPane.font", ControlFont);
		UIManager.put("Table.font", ControlFont);
		UIManager.put("TableHeader.font", ControlFont);
		UIManager.put("TextArea.font", FixedControlFont);
		UIManager.put("TextField.font", ControlFont);
		UIManager.put("TextPane.font", ControlFont);
		UIManager.put("TitledBorder.font", ControlFont);
		UIManager.put("ToggleButton.font", ControlFont);
		UIManager.put("ToolBar.font", MenuFont);
		UIManager.put("ToolTip.font", ToolTipFont);
		UIManager.put("Tree.font", ControlFont);
		UIManager.put("Viewport.font", ControlFont);

		setTitle("Test");
		setIconImage(readImageIcon("icon-32.png").getImage());

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JComponent jp = buildContent();
		String showScrollbars = System.getProperty("scrollbars", "").toLowerCase();

		/**
		 * Handle scrollbars:
		 *
		 * 1) forced scrollbars (-Dscrollbars=true): always display them
		 * 2) optional scrollbars (-Dscrollbars=optional): display them as needed
		 * 3) otherwise (default): don't display them
		 */
		switch (showScrollbars) {
			case "true":
				setContentPane(
					new JScrollPane(
						jp,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
					)
				);
				break;
			case "optional":
				setContentPane(
					new JScrollPane(
						jp,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
					)
				);
				break;
			default:
				setContentPane(jp);
				break;
		}

		String projectName = PropertiesUtil.getProjectProperties().get("project.name");
		String projectVersion = PropertiesUtil.getProjectProperties().get("project.version");
		String title = projectName + " " + projectVersion;

		// If the version contains a "-" (e.g. "1.50.1-SNAPSHOT" or "1.50.1-beta1"), add a warning message
		if (projectVersion.indexOf('-') > -1) {
			title = title + " - " + Messages.getString("LooksFrame.26");
		}

		if (PMS.getTraceMode() == 2) {
			// Forced trace mode
			title = title + "  [" + Messages.getString("TracesTab.10").toUpperCase() + "]";
		}

		setTitle(title);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		// Display tooltips immediately and for a long time
		ToolTipManager.sharedInstance().setInitialDelay(400);
		ToolTipManager.sharedInstance().setDismissDelay(60000);
		ToolTipManager.sharedInstance().setReshowDelay(400);

		if (!configuration.isMinimized() && System.getProperty(START_SERVICE) == null) {
			setVisible(true);
		}
		BasicSystemUtils.INSTANCE.addSystemTray(this);
	}

	public static ImageIcon readImageIcon(String filename) {
		URL url = LooksFrame.class.getResource("/resources/images/" + filename);
		return url == null ? null : new ImageIcon(url);
	}

	public JComponent buildContent() {
		JPanel panel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		toolBar.add(new JPanel());

		if (PMS.getConfiguration().useWebInterface()) {
			webinterface = createToolBarButton(Messages.getString("LooksFrame.29"), "button-wif.png");
			webinterface.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String error = null;
					if (PMS.get().getWebInterface() != null && isNotBlank(PMS.get().getWebInterface().getUrl())) {
						try {
							URI uri = new URI(PMS.get().getWebInterface().getUrl());
							try {
								Desktop.getDesktop().browse(uri);
							} catch (RuntimeException | IOException be) {
								LOGGER.error("Cound not open the default web browser: {}", be.getMessage());
								LOGGER.trace("", be);
								error = Messages.getString("LooksFrame.BrowserError") + "\n" + be.getMessage();
							}
						} catch (URISyntaxException se) {
							LOGGER.error(
								"Could not form a valid web interface URI from \"{}\": {}",
								PMS.get().getWebInterface().getUrl(),
								se.getMessage()
							);
							LOGGER.trace("", se);
							error = Messages.getString("LooksFrame.URIError");
						}
					}
					else {
						error = Messages.getString("LooksFrame.URIError");
					}
					if (error != null) {
						JOptionPane.showMessageDialog(null, error, Messages.getString("Dialog.Error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			webinterface.setToolTipText(Messages.getString("LooksFrame.30"));
			webinterface.setEnabled(configuration.useWebInterface());
			toolBar.add(webinterface);
			toolBar.addSeparator(new Dimension(20, 1));
		}

		restartIcon = (AnimatedIcon) reload.getIcon();
		restartRequredIcon.start();
		setReloadable(false);
		reload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reload.setEnabled(false);
				PMS.get().reset();
			}
		});
		reload.setToolTipText(Messages.getString("LooksFrame.28"));
		toolBar.add(reload);

		toolBar.addSeparator(new Dimension(20, 1));
		AbstractButton quit = createToolBarButton(Messages.getString("LooksFrame.5"), "button-quit.png");
		quit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});
		toolBar.add(quit);
		if (System.getProperty(START_SERVICE) != null) {
			quit.setEnabled(false);
		}
		toolBar.add(new JPanel());

		// Apply the orientation to the toolbar and all components in it
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		toolBar.applyComponentOrientation(orientation);
		toolBar.setBorder(new EmptyBorder(new Insets(8,0,0,0)));

		panel.add(toolBar, BorderLayout.NORTH);
		panel.add(buildMain(), BorderLayout.CENTER);
		status = new JLabel("");
		status.setBorder(BorderFactory.createEmptyBorder());
		status.setComponentOrientation(orientation);

		// Calling applyComponentOrientation() here would be ideal.
		// Alas it horribly mutilates the layout of several tabs.
		//panel.applyComponentOrientation(orientation);
		panel.add(status, BorderLayout.SOUTH);

		return panel;
	}

	public JComponent buildMain() {
		final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

		tabbedPane.setUI(new CustomTabbedPaneUI());

		st = new StatusTab(configuration);
		tt = new TracesTab(configuration, this);
		gt = new GeneralTab(configuration, this);
		nt = new NavigationShareTab(configuration, this);
		tr = new TranscodingTab(configuration, this);
		ht = new HelpTab();

		tabbedPane.addTab(Messages.getString("LooksFrame.18"), st.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.19"), tt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.20"), gt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.22"), nt.build());
		if (!configuration.isDisableTranscoding()) {
			tabbedPane.addTab(Messages.getString("LooksFrame.21"), tr.build());
		} else {
			tr.build();
		}
		tabbedPane.addTab(Messages.getString("LooksFrame.24"), new HelpTab().build());
		tabbedPane.addTab(Messages.getString("LooksFrame.25"), new AboutTab().build());

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int selectedIndex = tabbedPane.getSelectedIndex();

				if (HELP_PAGES[selectedIndex] != null) {
					PMS.setHelpPage(HELP_PAGES[selectedIndex]);

					// Update the contents of the help tab itself
					ht.updateContents();
				}
			}
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
		JImageButton button = new JImageButton(text, iconName);
		button.setToolTipText(toolTipText);
		button.setFocusable(false);
		button.setBorderPainted(false);
		return button;
	}

	public void quit() {
		WindowsNamedPipe.setLoop(false);
		windowProperties.dispose();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted during shutdown: {}", e);
		}

		System.exit(0);
	}

	@Override
	public void dispose() {
		windowProperties.dispose();
		super.dispose();
	}

	@Override
	public void append(final String msg) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				tt.append(msg);
			}
		});
	}

	@Override
	public void setReadValue(long v, String msg) {
		st.setReadValue(v, msg);
	}

	@Override
	public void setConnectionState(final ConnectionState connectionState) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				st.setConnectionState(connectionState);
			}
		});
	}

	@Override
	public void updateBuffer() {
		st.updateCurrentBitrate();
	}

	/**
	 * This method is being called when a configuration change requiring
	 * a restart of the HTTP server has been done by the user. It should notify the user
	 * to restart the server.<br>
	 * Currently the icon as well as the tool tip text of the restart button is being
	 * changed.<br>
	 * The actions requiring a server restart are defined by {@link PmsConfiguration#NEED_RELOAD_FLAGS}
	 *
	 * @param required true if the server has to be restarted, false otherwise
	 */
	@Override
	public void setReloadable(final boolean required) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (required) {
					if (reload.getIcon() == restartIcon) {
						restartIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, restartRequredIcon, false));
						reload.setToolTipText(Messages.getString("LooksFrame.13"));
					}
				} else {
					reload.setEnabled(true);
					if (restartRequredIcon == reload.getIcon()) {
						reload.setToolTipText(Messages.getString("LooksFrame.28"));
						restartRequredIcon.setNextStage(new AnimatedIconStage(AnimatedIconType.DEFAULTICON, restartIcon, false));
					}
				}
			}
		});
	}

	@Override
	public void addEngines() {
		tr.addEngines();
	}

	// Fired on AutoUpdater state changes
	@Override
	public void update(Observable o, Object arg) {
		if (configuration.isAutoUpdate()) {
			checkForUpdates(true);
		}
	}

	/**
	 * Start the process of checking for updates.
	 *
	 * @param isStartup whether this is being called via startup or button
	 */
	public void checkForUpdates(boolean isStartup) {
		if (autoUpdater != null) {
			try {
				AutoUpdateDialog.showIfNecessary(this, autoUpdater, isStartup);
			} catch (NoClassDefFoundError ncdfe) {
				LOGGER.error("Error displaying AutoUpdateDialog", ncdfe);
			}
		}
	}

	@Override
	public void setStatusLine(String line) {
		if (line == null || "".equals(line)) {
			line = "";
			status.setBorder(BorderFactory.createEmptyBorder());
		} else {
			status.setBorder(BorderFactory.createEmptyBorder(0, 9, 8, 0));
		}

		status.setText(line);
	}

	@Override
	public void addRenderer(RendererConfiguration renderer) {
		st.addRenderer(renderer);
	}

	@Override
	public void updateRenderer(RendererConfiguration renderer) {
		StatusTab.updateRenderer(renderer);
	}

	@Override
	public void serverReady() {
		st.updateMemoryUsage();
		gt.addRenderers();
	}

	@Override
	public void setScanLibraryEnabled(boolean flag) {
		getNt().setScanLibraryEnabled(flag);
	}

	@Override
	public String getLog() {
		return getTt().getList().getText();
	}
}
