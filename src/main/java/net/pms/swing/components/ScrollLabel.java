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
package net.pms.swing.components;

import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * A lightweight version of a label that automatically scrolls its text if wider
 * than a specified maximum.
 */
public class ScrollLabel extends JLabel {

	private static final long serialVersionUID = 1834361616111946511L;
	String text;

	public ScrollLabel(String text) {
		super(text);
		this.text = text;
		setSize(getPreferredSize());
		scrollTheText();
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		this.text = text;
	}

	private void scrollTheText() {
		new Timer(200, (ActionEvent e) -> {
			text = new StringBuffer(text.substring(1)).append(text.substring(0, 1)).toString();
			setText(text);
		}).start();
	}
}
