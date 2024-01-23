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
package net.pms.gui.systray;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.external.update.AutoUpdater;
import net.pms.gui.GuiManager;
import net.pms.newgui.components.SvgMultiResolutionImage;
import net.pms.platform.PlatformUtils;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

/**
 * @author Surf@ceS
 */
public class SysTray implements ChangeListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(SysTray.class);

	private static SysTray instance = null;

	private boolean updateAvailable = false;
	private SystemTray systemTray;
	private TrayIcon trayIcon;

	private SysTray() {
		if (SystemTray.isSupported()) {
			systemTray = SystemTray.getSystemTray();

			Image trayIconImage = resolveTrayIcon(updateAvailable);

			PopupMenu popup = new PopupMenu();
			MenuItem quitItem = new MenuItem(Messages.getString("Quit"));
			MenuItem oldGuiItem = new MenuItem(Messages.getString("SettingsOld"));

			quitItem.addActionListener((ActionEvent e) -> PMS.quit());

			oldGuiItem.addActionListener((ActionEvent e) -> GuiManager.showSwingFrame());

			if (PMS.getConfiguration().useWebPlayerServer()) {
				MenuItem webPlayerItem = new MenuItem(Messages.getString("WebPlayer"));
				webPlayerItem.addActionListener((ActionEvent e) -> PlatformUtils.INSTANCE.browseURI(PMS.get().getWebPlayerServer().getUrl()));
				popup.add(webPlayerItem);
			}

			MenuItem webGuiItem = new MenuItem(Messages.getString("Settings"));
			webGuiItem.addActionListener((ActionEvent e) -> PlatformUtils.INSTANCE.browseURI(PMS.get().getGuiServer().getUrl()));
			popup.add(webGuiItem);
			popup.add(oldGuiItem);
			popup.add(quitItem);

			trayIcon = new TrayIcon(trayIconImage, PropertiesUtil.getProjectProperties().get("project.name"), popup);

			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener((ActionEvent e) -> PlatformUtils.INSTANCE.browseURI(PMS.get().getGuiServer().getUrl()));
			try {
				if (systemTray.getTrayIcons().length > 0) {
					systemTray.remove(systemTray.getTrayIcons()[0]);
				}
				systemTray.add(trayIcon);
			} catch (AWTException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (systemTray != null && e.getSource() instanceof AutoUpdater autoUpdater) {
			boolean isUpdateAvailable = autoUpdater.getState() == AutoUpdater.State.UPDATE_AVAILABLE;
			if (updateAvailable != isUpdateAvailable) {
				updateAvailable = isUpdateAvailable;
				SwingUtilities.invokeLater(() -> {
					if (trayIcon != null) {
						Image oldImage = trayIcon.getImage();
						Image trayIconImage = resolveTrayIcon(updateAvailable);
						trayIcon.setImage(trayIconImage);
						oldImage.flush();
					}
				});
			}
		}
	}

	public static void addSystemTray() {
		if (SystemTray.isSupported() && instance == null) {
			instance = new SysTray();
			AutoUpdater.addChangeListener(instance);
		}
	}

	/**
	 * Return the proper tray icon for the operating system.
	 *
	 * @return The tray icon.
	 */
	private static Image resolveTrayIcon(boolean updateAvailable) {
		String icon = PlatformUtils.INSTANCE.getTrayIcon();
		SVGDocument document = SvgMultiResolutionImage.getSVGDocument(SysTray.class.getResource("/resources/images/" + icon + ".svg"));
		if (updateAvailable) {
			Element elem = document.getElementById("Updatable");
			if (elem != null) {
				elem.setAttribute("opacity", "1");
			}
		}
		return new SvgMultiResolutionImage(document);
	}

}
