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
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Splash extends JFrame {
	private static final long serialVersionUID = 2357524127613134620L;
	private JLabel imglabel;
	private ImageIcon img;
	Thread t = null;

	/**
	 * Show the splash screen before the application GUI starts.
	 * <p>
	 * When the GUI started call the {@link Splash.dispose} to release all resources used by this
     * {@code Splash} and return all memory they consume to the OS.
	 * 
	 * @param showProgressBar Set true when the progress bar should be displayed
	 */
	public Splash() {
		img = new ImageIcon(getClass().getResource("/resources/images/splash.png"));
		imglabel = new JLabel(img);
		imglabel.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
		setSize(imglabel.getWidth(), imglabel.getHeight() + 20);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setUndecorated(true);
		setBackground(new Color(1.0f,1.0f,1.0f,0.0f));
		setLocationRelativeTo(null);
		setLayout(null);
		add(imglabel);
		if (System.getProperty("console") == null) {
			setVisible(true);
		}
	}
}