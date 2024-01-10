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

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.pms.Messages;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Splash extends JFrame implements MouseListener {

	private static final long serialVersionUID = 2357524127613134620L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Splash.class);
	private final UmsConfiguration configuration;
	private final transient Object optionLock = new Object();
	private JLabel imageLabel;
	private JLabel statusLabel;
	private String status;
	private String dots;
	private Timer timer;

	/**
	 * Creates a new instance and displays it.
	 * <p>
	 * Use {@link #dispose()} to remove the {@link Splash} when the GUI is
	 * initialized.
	 *
	 * @param configuration the {@link UmsConfiguration} to use.
	 * @param graphicsConfiguration the {@link GraphicsConfiguration} to use.
	 */
	public Splash(@Nonnull UmsConfiguration configuration, @Nullable GraphicsConfiguration graphicsConfiguration) {
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
		statusLabel = new JLabel();
		statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		statusLabel.setSize(imageLabel.getWidth(), 12);
		statusLabel.setBackground(new Color(1.0f, 1.0f, 1.0f, 0.0f));
		statusLabel.setForeground(Color.WHITE);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setLocation(0, (int) (imageLabel.getHeight() / 1.20));
		imageLabel.add(statusLabel);
		Rectangle bounds = getGraphicsConfiguration().getBounds();
		setLocation(bounds.x + (bounds.width - getWidth()) / 2, bounds.y + (bounds.height - getHeight()) / 2);
		setVisible(true);
		timer = new Timer(500,  (ActionEvent e) -> {
			if (status != null) {
				if (dots.length() == 3) {
					dots = "";
				} else {
					dots += ".";
				}
				statusLabel.setText(status + dots);
			}
		});
		timer.setInitialDelay(500);
		timer.start();
	}

	public void setText(String text) {
		status = text;
		dots = "";
		statusLabel.setText(status);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		synchronized (optionLock) {
			int isShowSplashScreen = JOptionPane.showConfirmDialog(
				this,
				Messages.getString("DisableSplashScreenDuringStartup"),
				Messages.getString("SplashScreenSetting"),
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
		//nothing to do
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		//nothing to do
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		//nothing to do
	}

	@Override
	public void mouseExited(MouseEvent e) {
		//nothing to do
	}

	@Override
	public void dispose() {
		if (imageLabel != null) {
			imageLabel.setVisible(false);
		}
		if (statusLabel != null) {
			statusLabel.setVisible(false);
		}
		if (timer != null) {
			timer.stop();
		}
		synchronized (optionLock) {
			super.dispose();
		}
	}

}
