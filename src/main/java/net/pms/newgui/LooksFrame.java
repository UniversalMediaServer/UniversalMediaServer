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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.gui.IFrame;
import net.pms.io.WindowsNamedPipe;
import net.pms.newgui.update.AutoUpdateDialog;
import net.pms.update.AutoUpdater;
import net.pms.util.PropertiesUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.looks.Options;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.sun.jna.Platform;

public class LooksFrame extends JFrame implements IFrame, Observer {
	private static final Logger LOGGER = LoggerFactory.getLogger(LooksFrame.class);

	private final AutoUpdater autoUpdater;
	private final PmsConfiguration configuration;
	public static final String START_SERVICE = "start.service";
	private static final long serialVersionUID = 8723727186288427690L;
	private NavigationShareTab ft;
	private StatusTab st;
	private TracesTab tt;
	private TranscodingTab tr;
	private GeneralTab nt;
	private AbstractButton reload;
	private JLabel status;
	protected static final Dimension PREFERRED_SIZE = new Dimension(1000, 750);
	// https://code.google.com/p/ps3mediaserver/issues/detail?id=949
	protected static final Dimension MINIMUM_SIZE = new Dimension(800, 480);
	private static boolean lookAndFeelInitialized = false;

	public TracesTab getTt() {
		return tt;
	}

	public NavigationShareTab getFt() {
		return ft;
	}

	public TranscodingTab getTr() {
		return tr;
	}

	public AbstractButton getReload() {
		return reload;
	}

	static void initializeLookAndFeel() {
		if (lookAndFeelInitialized) {
			return;
		}

		LookAndFeel selectedLaf = null;
		if (Platform.isWindows()) {
			try {
				selectedLaf = (LookAndFeel) Class.forName("com.jgoodies.looks.windows.WindowsLookAndFeel").newInstance();
				//selectedLaf = (LookAndFeel) Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel").newInstance();
			} catch (Exception e) {
				selectedLaf = new PlasticLookAndFeel();
			}
		} else if (System.getProperty("nativelook") == null && !Platform.isMac()) {
			selectedLaf = new PlasticLookAndFeel();
		} else {
			try {
				String systemClassName = UIManager.getSystemLookAndFeelClassName();
				// workaround for gnome
				try {
					String gtkLAF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
					Class.forName(gtkLAF);
					if (systemClassName.equals("javax.swing.plaf.metal.MetalLookAndFeel")) {
						systemClassName = gtkLAF;
					}
				} catch (ClassNotFoundException ce) {
					LOGGER.debug("Caught exception", ce);
				}

				LOGGER.debug("Choosing java look and feel: " + systemClassName);
				UIManager.setLookAndFeel(systemClassName);
			} catch (Exception e1) {
				selectedLaf = new PlasticLookAndFeel();
				LOGGER.error("Error while setting native look and feel: ", e1);
			}
		}

		if (selectedLaf instanceof PlasticLookAndFeel) {
			PlasticLookAndFeel.setPlasticTheme(PlasticLookAndFeel.createMyDefaultTheme());
			PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
			PlasticLookAndFeel.setHighContrastFocusColorsEnabled(false);
		} else if (selectedLaf != null && selectedLaf.getClass() == MetalLookAndFeel.class) {
			MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
		}

		// Work around caching in MetalRadioButtonUI
		JRadioButton radio = new JRadioButton();
		radio.getUI().uninstallUI(radio);
		JCheckBox checkBox = new JCheckBox();
		checkBox.getUI().uninstallUI(checkBox);

		if (selectedLaf != null) {
			try {
				UIManager.setLookAndFeel(selectedLaf);
			} catch (UnsupportedLookAndFeelException e) {
				LOGGER.debug("Cannot change look and feel", e);
			}
		}

		lookAndFeelInitialized = true;
	}

	/**
	 * Constructs a <code>DemoFrame</code>, configures the UI,
	 * and builds the content.
	 */
	public LooksFrame(AutoUpdater autoUpdater, PmsConfiguration configuration) {
		this.autoUpdater = autoUpdater;
		this.configuration = configuration;
		assert this.configuration != null;
		Options.setDefaultIconSize(new Dimension(18, 18));
		Options.setUseNarrowButtons(true);

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

		// http://propedit.sourceforge.jp/propertieseditor.jnlp
		Font sf = null;

		// Set an unicode font for testing exotics languages (japanese)
		final String language = configuration.getLanguage();
		if (language != null && (language.equals("ja") || language.startsWith("zh"))) {
			sf = new Font("Serif", Font.PLAIN, 12);
		}
		if (sf != null) {
			UIManager.put("Button.font", sf);
			UIManager.put("ToggleButton.font", sf);
			UIManager.put("RadioButton.font", sf);
			UIManager.put("CheckBox.font", sf);
			UIManager.put("ColorChooser.font", sf);
			UIManager.put("ToggleButton.font", sf);
			UIManager.put("ComboBox.font", sf);
			UIManager.put("ComboBoxItem.font", sf);
			UIManager.put("InternalFrame.titleFont", sf);
			UIManager.put("Label.font", sf);
			UIManager.put("List.font", sf);
			UIManager.put("MenuBar.font", sf);
			UIManager.put("Menu.font", sf);
			UIManager.put("MenuItem.font", sf);
			UIManager.put("RadioButtonMenuItem.font", sf);
			UIManager.put("CheckBoxMenuItem.font", sf);
			UIManager.put("PopupMenu.font", sf);
			UIManager.put("OptionPane.font", sf);
			UIManager.put("Panel.font", sf);
			UIManager.put("ProgressBar.font", sf);
			UIManager.put("ScrollPane.font", sf);
			UIManager.put("Viewport", sf);
			UIManager.put("TabbedPane.font", sf);
			UIManager.put("TableHeader.font", sf);
			UIManager.put("TextField.font", sf);
			UIManager.put("PasswordFiled.font", sf);
			UIManager.put("TextArea.font", sf);
			UIManager.put("TextPane.font", sf);
			UIManager.put("EditorPane.font", sf);
			UIManager.put("TitledBorder.font", sf);
			UIManager.put("ToolBar.font", sf);
			UIManager.put("ToolTip.font", sf);
			UIManager.put("Tree.font", sf);
		}

		setTitle("Test");
		setIconImage(readImageIcon("icon-32.png").getImage());

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JComponent jp = buildContent();
		String showScrollbars = System.getProperty("scrollbars", "").toLowerCase();

		/*
		 * handle scrollbars:
		 *
		 * 1) forced scrollbars (-Dscrollbars=true): always display them
		 * 2) optional scrollbars (-Dscrollbars=optional): display them as needed
		 * 3) otherwise (default): don't display them
		 */
		if (showScrollbars.equals("true")) {
			setContentPane(
				new JScrollPane(
				jp,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
		} else if (showScrollbars.equals("optional")) {
			setContentPane(
				new JScrollPane(
				jp,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		} else {
			setContentPane(jp);
		}

		String projectName = PropertiesUtil.getProjectProperties().get("project.name");
		String projectVersion = PropertiesUtil.getProjectProperties().get("project.version");
		String title = projectName + " " + projectVersion;

		// If the version contains a "-" (e.g. "1.50.1-SNAPSHOT" or "1.50.1-beta1"), add a warning message
		if (projectVersion.indexOf("-") > -1) {
			title = title + " - " + Messages.getString("LooksFrame.26");
		}

		this.setTitle(title);
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		Dimension screenSize = getToolkit().getScreenSize();

		if (screenSize.width < MINIMUM_SIZE.width || screenSize.height < MINIMUM_SIZE.height) {
			setMinimumSize(screenSize);
		} else {
			setMinimumSize(MINIMUM_SIZE);
		}

		if (screenSize.width < PREFERRED_SIZE.width || screenSize.height < PREFERRED_SIZE.height) {
			setSize(screenSize);
		} else {
			setSize(PREFERRED_SIZE);
		}

		setResizable(true);
		Dimension paneSize = getSize();
		setLocation(
			((screenSize.width > paneSize.width) ? ((screenSize.width - paneSize.width) / 2) : 0),
			((screenSize.height > paneSize.height) ? ((screenSize.height - paneSize.height) / 2) : 0)
		);
		if (!configuration.isMinimized() && System.getProperty(START_SERVICE) == null) {
			setVisible(true);
		}
		PMS.get().getRegistry().addSystemTray(this);
	}

	protected static ImageIcon readImageIcon(String filename) {
		URL url = LooksFrame.class.getResource("/resources/images/" + filename);
		return new ImageIcon(url);
	}

	public JComponent buildContent() {
		JPanel panel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		toolBar.add(new JPanel());
		AbstractButton save = createToolBarButton(Messages.getString("LooksFrame.9"), "filesave-48.png", Messages.getString("LooksFrame.9"));
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PMS.get().save();
			}
		});
		toolBar.add(save);
		toolBar.addSeparator();
		reload = createToolBarButton(Messages.getString("LooksFrame.12"), "reload_page-48.png", Messages.getString("LooksFrame.12"));
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PMS.get().reset();
			}
		});
		toolBar.add(reload);
		toolBar.addSeparator();
		AbstractButton quit = createToolBarButton(Messages.getString("LooksFrame.5"), "exit-48.png", Messages.getString("LooksFrame.5"));
		quit.addActionListener(new ActionListener() {
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
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		toolBar.applyComponentOrientation(orientation);

		panel.add(toolBar, BorderLayout.NORTH);
		panel.add(buildMain(), BorderLayout.CENTER);
		status = new JLabel(" ");
		status.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(0, 5, 0, 5)));
		status.setComponentOrientation(orientation);

		// Calling applyComponentOrientation() here would be ideal.
		// Alas it horribly mutilates the layout of several tabs.
		//panel.applyComponentOrientation(orientation);
		panel.add(status, BorderLayout.SOUTH);


		return panel;
	}

	public JComponent buildMain() {
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

		st = new StatusTab(configuration);
		tt = new TracesTab(configuration);
		tr = new TranscodingTab(configuration);
		nt = new GeneralTab(configuration);
		ft = new NavigationShareTab(configuration);

		tabbedPane.addTab(Messages.getString("LooksFrame.18"),/* readImageIcon("server-16.png"),*/ st.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.19"),/* readImageIcon("mail_new-16.png"),*/ tt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.20"),/* readImageIcon("advanced-16.png"),*/ nt.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.22"), /*readImageIcon("bookmark-16.png"),*/ ft.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.21"),/* readImageIcon("player_play-16.png"),*/ tr.build());
		tabbedPane.addTab(Messages.getString("LooksFrame.24"), /* readImageIcon("mail_new-16.png"), */ new HelpTab().build());
		tabbedPane.addTab(Messages.getString("LooksFrame.25"), /*readImageIcon("documentinfo-16.png"),*/ new AboutTab().build());

		tabbedPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Set the orientation of the tabbedPane. Note: not using
		// applyComponentOrientation() here on purpose as it will horribly
		// mutilate the layout of several tabs.
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
		tabbedPane.setComponentOrientation(orientation);

		return tabbedPane;
	}

	protected AbstractButton createToolBarButton(String text, String iconName, String toolTipText) {
		JButton button = new JButton(text, readImageIcon(iconName));
		button.setToolTipText(toolTipText);
		button.setFocusable(false);
		return button;
	}

	public void quit() {
		WindowsNamedPipe.setLoop(false);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			LOGGER.error(null, e);
		}
		System.exit(0);
	}

	@Override
	public void append(String msg) {
		tt.getList().append(msg);
	}

	@Override
	public void setReadValue(long v, String msg) {
		st.setReadValue(v, msg);
	}

	@Override
	public void setStatusCode(int code, String msg, String icon) {
		st.getJl().setText(msg);
		try {
			st.getImagePanel().set(ImageIO.read(LooksFrame.class.getResourceAsStream("/resources/images/" + icon)));
		} catch (IOException e) {
			LOGGER.error(null, e);
		}
	}

	@Override
	public void setValue(int v, String msg) {
		st.getJpb().setValue(v);
		st.getJpb().setString(msg);
	}

	/**
	 * This method is being called when a configuration change requiring
	 * a restart of the HTTP server has been done by the user. It should notify the user
	 * to restart the server.<br>
	 * Currently the icon as well as the tool tip text of the restart button is being 
	 * changed.<br>
	 * The actions requiring a server restart are defined by {@link PmsConfiguration#NEED_RELOAD_FLAGS}
	 * 
	 * @param b true if the server has to be restarted, false otherwise
	 */
	@Override
	public void setReloadable(boolean b) {
		if(b) {
			reload.setIcon(readImageIcon("reload_page_required-48.png"));
			reload.setToolTipText(Messages.getString("LooksFrame.13"));
		} else {
			reload.setIcon(readImageIcon("reload_page-48.png"));
			reload.setToolTipText(Messages.getString("LooksFrame.12"));
		}
	}

	@Override
	public void addEngines() {
		tr.addEngines();
	}

	// fired on AutoUpdater state changes
	public void update(Observable o, Object arg) {
		if (configuration.isAutoUpdate()) {
			checkForUpdates();
		}
	}

	public void checkForUpdates() {
		if (autoUpdater != null) {
			try {
				AutoUpdateDialog.showIfNecessary(this, autoUpdater);
			} catch (NoClassDefFoundError ncdf) {
				LOGGER.info("Class not found: " + ncdf.getMessage());
			}
		}
	}

	public void setStatusLine(String line) {
		if (line == null) {
			line = " ";
		}
		status.setText(line);
	}

	@Override
	public void addRendererIcon(int code, String msg, String icon) {
		st.addRendererIcon(code, msg, icon);
	}

	@Override
	public void serverReady() {
		nt.addRenderers();
		nt.addPlugins();
	}

	@Override
	public void setScanLibraryEnabled(boolean flag) {
		getFt().setScanLibraryEnabled(flag);
	}
}
