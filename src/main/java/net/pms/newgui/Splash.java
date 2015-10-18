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
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Splash extends JFrame {
	private static final long serialVersionUID = 2357524127613134620L;
	private static final Logger LOGGER = LoggerFactory.getLogger(Splash.class);
	private JLabel imglabel;
	private ImageIcon img;
	private static JProgressBar pbar;
	Thread t = null;

	/**
	 * Show the splash screen before the application GUI starts.
	 * <p>
	 * When the GUI started call the {@link Splash.dispose} to release all resources used by this
     * {@code Splash} and return all memory they consume to the OS.
	 * 
	 * @param image The Image displayed
	 * @param showProgressBar Set true when the progress bar should be displayed
	 */
	public Splash(ImageIcon image, boolean showProgressBar) {
		img = image;
		imglabel = new JLabel(img);
		imglabel.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
		setSize(imglabel.getWidth(), imglabel.getHeight() + 20);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setUndecorated(true);
		setBackground(new Color(1.0f,1.0f,1.0f,0.0f));
		setLocationRelativeTo(null);
		setLayout(null);
		add(imglabel);
		if (showProgressBar) {
			pbar = new JProgressBar();
			pbar.setMinimum(0);
			pbar.setMaximum(100);
			pbar.setStringPainted(true);
			pbar.setForeground(Color.LIGHT_GRAY);
			add(pbar);
			pbar.setPreferredSize(new Dimension(imglabel.getWidth(), 20));
			pbar.setBounds(0, imglabel.getHeight(), imglabel.getWidth(), 20);
			Thread t = new Thread() {
				public void run() {
					int i = 0;
					while (i <= 100) {
						pbar.setValue(i);
						try {
							sleep(100);
						} catch (InterruptedException ex) {
							LOGGER.error("Error:", ex);
						}
						i++;
					}
				}
			};
			t.start();
		}
		
	}
}