/*
 * Universal Media Server, for streaming any media to DLNA
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
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	private PmsConfiguration configuration;
	private final Object optionLock = new Object();
	private JLabel imageLabel;

	/**
	 * Creates a new instance and displays it.
	 * <p>
	 * Use {@link #dispose()} to remove the {@link Splash} when the GUI is
	 * initialized.
	 *
	 * @param configuration the {@link PmsConfiguration} to use.
	 * @param graphicsConfiguration the {@link GraphicsConfiguration} to use.
	 */
	public Splash(@Nonnull PmsConfiguration configuration, @Nullable GraphicsConfiguration graphicsConfiguration) {
		super(graphicsConfiguration);
		this.configuration = configuration;
		if (!configuration.isShowSplashScreen() || System.getProperty("console") != null) {
			return;
		}

		URL imageURL = getClass().getResource("/resources/images/splash.png");
		if (imageURL == null) {
			return;
		}
		ImageIcon image = new ImageIcon(imageURL);
		imageLabel = new JLabel(image);
		imageLabel.setBounds(0, 0, image.getIconWidth(), image.getIconHeight());
		setSize(imageLabel.getWidth(), imageLabel.getHeight());
		setUndecorated(true);
		setBackground(new Color(1.0f, 1.0f, 1.0f, 0.0f));
		add(imageLabel);
		imageLabel.addMouseListener(this);
		image = new ImageIcon(getClass().getResource("/resources/images/icon-32.png"));
		setIconImage(image.getImage());
		Rectangle bounds = getGraphicsConfiguration().getBounds();
		setLocation(bounds.x + (bounds.width - getWidth()) / 2, bounds.y + (bounds.height - getHeight()) / 2);
		setVisible(true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		synchronized (optionLock) {
			int isShowSplashScreen = JOptionPane.showConfirmDialog(
				this,
				Messages.getString("Splash.1"),
				Messages.getString("Splash.2"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE
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
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void dispose() {
		if (imageLabel != null) {
			imageLabel.setVisible(false);
		}
		synchronized (optionLock) {
			super.dispose();
		}
	}
}
