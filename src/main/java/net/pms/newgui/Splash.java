/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Splash extends JFrame implements MouseListener {
	private static final long serialVersionUID = 2357524127613134620L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Splash.class);
	private JLabel imglabel;
	private ImageIcon img;
	private PmsConfiguration configuration;

	/**
	 * Show the splash screen before the application GUI starts.
	 * <p>
	 * When the GUI started call the {@code .dispose()} to release all resources used by this
     * {@code Splash} class and return all memory they consume to the OS.
	 * @return 
	 */
	public Splash(PmsConfiguration config) {
		this.configuration = config;
		if (!configuration.isShowSplashScreen()) {
			return;
		}
			
		img = new ImageIcon(getClass().getResource("/resources/images/splash.png"));
		imglabel = new JLabel(img);
		imglabel.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
		setSize(imglabel.getWidth(), imglabel.getHeight());
		setUndecorated(true);
		setBackground(new Color(1.0f,1.0f,1.0f,0.0f));
		setLocationRelativeTo(null);
		setLayout(null);
		add(imglabel);
		imglabel.addMouseListener(this);
		if (System.getProperty("console") == null) {
			setVisible(true);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int isShowSplashScreen = JOptionPane.showConfirmDialog(
			null,
			Messages.getString("Splash.1"),
			Messages.getString("Splash.2"),
			JOptionPane.YES_NO_OPTION
		);
		if (isShowSplashScreen == 0) {
			configuration.setShowSplashScreen(false);
			try {
				configuration.save();
			} catch (ConfigurationException e1) {
				LOGGER.error("Error when saving the Splash Screen setting", e1);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}