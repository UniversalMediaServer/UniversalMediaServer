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

import java.net.URL;
import javax.swing.Icon;
import net.pms.swing.gui.JavaGui;

public class JAnimatedButton extends JImageButton {

	private static final long serialVersionUID = -8316312033513554308L;

	private AnimatedIcon currentIcon = null;

	public JAnimatedButton(String text, AnimatedIcon icon) {
		super(text, icon);
	}

	public JAnimatedButton(AnimatedIcon icon) {
		super(icon);
	}

	public JAnimatedButton(String text, String iconName) {
		super(text, iconName);
	}

	public JAnimatedButton(String iconName) {
		super(iconName);
	}

	public JAnimatedButton() {
		super();
	}

	/**
	 * Helps {@link AnimatedIcon} instances to stop other instances when the
	 * icon is changed. This is NOT thread safe.
	 *
	 * @return the previously painted {@link AnimatedIcon} or <code>null</code>
	 */
	public AnimatedIcon getCurrentIcon() {
		return currentIcon;
	}

	/**
	 * Sets the currently painted {@link AnimatedIcon}. This is NOT thread safe.
	 *
	 * @param icon the {@link AnimatedIcon} to set.
	 */
	public void setCurrentIcon(AnimatedIcon icon) {
		currentIcon = icon;
	}

	@Override
	protected Icon readIcon(String filename) {
		URL url = JavaGui.getImageResource(filename);
		return url == null ? null : new AnimatedIcon(this, filename);
	}

}
