/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011 G.Zsombor
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
package net.pms.io;

import com.sun.jna.Platform;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.newgui.LooksFrame;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for the SystemUtils class for the generic cases.
 * @author zsombor
 *
 */
public class BasicSystemUtils implements SystemUtils {
	private final static Logger LOGGER = LoggerFactory.getLogger(BasicSystemUtils.class); 

	protected String vlcp;
	protected String vlcv;
	protected boolean avis;

	@Override
	public void disableGoToSleep() {

	}

	@Override
	public void reenableGoToSleep() {

	}

	@Override
	public File getAvsPluginsDir() {
		return null;
	}

	@Override
	public File getKLiteFiltersDir() {
		return null;
	}

	@Override
	public String getShortPathNameW(String longPathName) {
		return longPathName;
	}

	@Override
	public String getWindowsDirectory() {
		return null;
	}

	@Override
	public String getDiskLabel(File f) {
		return null;
	}

	@Override
	public boolean isKerioFirewall() {
		return false;
	}

	@Override
	@Deprecated
	public String getVlcp() {
		return getVlcPath();
	}

	@Override
	@Deprecated
	public String getVlcv() {
		return getVlcVersion();
	}

	@Override
	public String getVlcPath() {
		return vlcp;
	}

	@Override
	public String getVlcVersion() {
		return vlcv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.io.SystemUtils#isAvis()
	 */
	@Override
	public boolean isAvis() {
		return avis;
	}

	@Override
	public void browseURI(String uri) {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch (IOException | URISyntaxException e) {
			LOGGER.trace("Unable to open the given URI: " + uri + ".");
		}
	}

	@Override
	public boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		return ni.isLoopback();
	}

	@Override
	public void addSystemTray(final LooksFrame frame) {
		if (SystemTray.isSupported()) {
			SystemTray tray = SystemTray.getSystemTray();

			Image trayIconImage = resolveTrayIcon();

			PopupMenu popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem(Messages.getString("LooksFrame.5"));
			MenuItem traceItem = new MenuItem(Messages.getString("LooksFrame.6"));

			defaultItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.quit();
				}
			});

			traceItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.setVisible(true);
				}
			});

			if (PMS.getConfiguration().useWebInterface()) {
				MenuItem webInterfaceItem = new MenuItem(Messages.getString("LooksFrame.29"));
				webInterfaceItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						browseURI(PMS.get().getWebInterface().getUrl());
					}
				});
				popup.add(webInterfaceItem);
			}
			popup.add(traceItem);
			popup.add(defaultItem);

			final TrayIcon trayIcon = new TrayIcon(trayIconImage, PropertiesUtil.getProjectProperties().get("project.name"), popup);

			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.setVisible(true);
					frame.setFocusable(true);
				}
			});
			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
	}

	/**
	 * Fetch the hardware address for a network interface.
	 *
	 * @param ni Interface to fetch the mac address for
	 * @return the mac address as bytes, or null if it couldn't be fetched.
	 * @throws SocketException
	 *             This won't happen on Mac OS, since the NetworkInterface is
	 *             only used to get a name.
	 */
	@Override
	public byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
		return ni.getHardwareAddress();
	}

	/**
	 * Return the platform specific ping command for the given host address,
	 * ping count and packet size.
	 *
	 * @param hostAddress The host address.
	 * @param count The ping count.
	 * @param packetSize The packet size.
	 * @return The ping command.
	 */
	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		return new String[] { "ping", /* count */"-c", Integer.toString(count), /* size */
				"-s", Integer.toString(packetSize), hostAddress };
	}

	public String parsePingLine(String line) {
		int msPos = line.indexOf("ms");
		String timeString = null;

		if (msPos > -1) {
			if (line.lastIndexOf('<', msPos) > -1){
				timeString = "0.5";
			} else {
				timeString = line.substring(line.lastIndexOf('=', msPos) + 1, msPos).trim();
			}
		}
		return timeString;
	}

	/**
	 * Return the proper tray icon for the operating system.
	 *
	 * @return The tray icon.
	 */
	private Image resolveTrayIcon() {
		String icon = "icon-16.png";

		if (Platform.isMac()) {
			icon = "icon-22.png";
		}
		return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/images/" + icon));
	}
}
