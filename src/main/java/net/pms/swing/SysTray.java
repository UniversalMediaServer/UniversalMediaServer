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
package net.pms.swing;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.external.update.AutoUpdater;
import net.pms.gui.GuiManager;
import net.pms.platform.PlatformUtils;
import net.pms.swing.components.SvgMultiResolutionImage;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

/**
 * @author Surf@ceS
 */
public class SysTray {

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
			MenuItem quitItem = new MenuItem(Messages.getGuiString("Quit"));
			MenuItem oldGuiItem = new MenuItem(Messages.getGuiString("SettingsOld"));

			quitItem.addActionListener((ActionEvent e) -> PMS.quit());

			oldGuiItem.addActionListener((ActionEvent e) -> GuiManager.showSwingFrame());

			if (PMS.getConfiguration().useWebPlayerServer()) {
				MenuItem webPlayerItem = new MenuItem(Messages.getGuiString("WebPlayer"));
				webPlayerItem.addActionListener((ActionEvent e) -> PlatformUtils.INSTANCE.browseURI(PMS.get().getWebPlayerServer().getUrl()));
				popup.add(webPlayerItem);
			}

			MenuItem webGuiItem = new MenuItem(Messages.getGuiString("Settings"));
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

			AutoUpdater.addChangeListener((ChangeEvent e) -> {
				if (systemTray != null && e.getSource() instanceof AutoUpdater autoUpdater) {
					boolean isUpdateAvailable = autoUpdater.getState() == AutoUpdater.State.UPDATE_AVAILABLE;
					if (updateAvailable != isUpdateAvailable) {
						updateAvailable = isUpdateAvailable;
						SwingUtilities.invokeLater(() -> {
							if (trayIcon != null) {
								Image oldImage = trayIcon.getImage();
								Image trayIconImage1 = resolveTrayIcon(updateAvailable);
								trayIcon.setImage(trayIconImage1);
								oldImage.flush();
							}
						});
					}
				}
			});
		}
	}

	public static void addSystemTray() {
		if (SystemTray.isSupported() && instance == null) {
			instance = new SysTray();
		}
	}

	/**
	 * Return the proper tray icon for the operating system.
	 *
	 * @return The tray icon.
	 */
	private static Image resolveTrayIcon(boolean updateAvailable) {
		String icon = PlatformUtils.INSTANCE.getTrayIcon();
		if (SwingUtil.HDPI_AWARE) {
			SVGDocument document = SvgMultiResolutionImage.getSVGDocument(SwingUtil.getImageResource(icon + ".svg"));
			if (updateAvailable) {
				Element elem = document.getElementById("updatable");
				if (elem != null) {
					elem.setAttribute("opacity", "1");
				}
			}
			return new SvgMultiResolutionImage(document);
		} else {
			String[] iconsPath;
			switch (icon) {
				case "icon-darkmode" -> {
					iconsPath = new String[]{"icon-darkmode.png", "icon-darkmode@2x.png", "icon-darkmode-32.png"};
				}
				case "icon-bw" -> {
					iconsPath = new String[]{"icon-bw-22.png", "icon-bw-32.png"};
				}
				default -> {
					iconsPath = new String[]{"icon-16.png", "icon-20.png", "icon-24.png", "icon-28.png", "icon-32.png", "icon-36.png",
						"icon-40.png", "icon-44.png", "icon-48.png", "icon-52.png", "icon-56.png", "icon-60.png", "icon-64.png"};
				}
			}
			List<BufferedImage> bufferedImages = new ArrayList<>();
			for (String iconPath : iconsPath) {
				try {
					BufferedImage image = ImageIO.read(SysTray.class.getResource("/resources/images/" + iconPath));
					if (updateAvailable) {
						BufferedImage overlay = ImageIO.read(SysTray.class.getResource("/resources/images/systray/icon-updatable-32.png"));
						Graphics2D g = image.createGraphics();
						g.drawImage(overlay.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_DEFAULT), 0, 0, null);
						g.dispose();
					}
					bufferedImages.add(image);
				} catch (IOException e) {
					LOGGER.debug("TrayIcon exception", e);
				}
			}
			if (bufferedImages.size() > 1) {
				return new BaseMultiResolutionImage(bufferedImages.toArray(BufferedImage[]::new));
			} else if (!bufferedImages.isEmpty()) {
				return bufferedImages.get(0);
			}
		}
		return null;
	}

}
