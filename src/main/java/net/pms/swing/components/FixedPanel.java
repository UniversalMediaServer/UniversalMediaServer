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

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

// A fixed-size horizontal content-centering panel.
public class FixedPanel extends JPanel {

	private static final long serialVersionUID = 8295684215937548109L;

	public FixedPanel(int w, int h) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setSize(w, h);
		super.add(Box.createGlue());
	}

	@Override
	public final void setSize(int w, int h) {
		Dimension d = new Dimension(w, h);
		setMaximumSize(d);
		setPreferredSize(d);
	}

	@Override
	public Component add(Component comp) {
		super.add(comp);
		super.add(Box.createGlue());
		return comp;
	}
}
