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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;
import net.pms.swing.components.AnimatedIcon.AnimatedIconStage;
import net.pms.swing.gui.JavaGui;
import net.pms.util.FileUtil;

public class JImageButton extends JButton implements AnimatedIconCallback {

	private static final long serialVersionUID = 8120596501408171329L;

	public JImageButton(String text, String iconName) {
		super(text, null);
		setProperties();
		setIcons(iconName);
	}

	public JImageButton(String iconName) {
		this(null, iconName);
	}

	public JImageButton() {
		this(null, (String) null);
	}

	public JImageButton(String text, Icon icon) {
		super(text, icon);
		setProperties();
	}

	public JImageButton(Icon icon) {
		super(icon);
		setProperties();
	}

	private void setProperties() {
		setRequestFocusEnabled(false);
		setBorderPainted(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setContentAreaFilled(false);
		setOpaque(false);
	}

	protected Icon readIcon(String filename) {
		return JavaGui.readImageIcon(filename);
	}

	/**
	 * Set icons from standard naming convention based on a base image name.
	 * @param defaultIconName the base image resource name used when the
	 *                        button is in the normal state and which
	 *                        the other state names are derived from.
	 */
	private void setIcons(String defaultIconName) {
		if (defaultIconName == null) {
			return;
		}

		Icon icon = readIcon(defaultIconName);
		if (icon == null) {
			setIcon(UIManager.getIcon("OptionPane.warningIcon"));
			return;
		}
		setIcon(icon);

		icon = readIcon(FileUtil.appendToFileName(defaultIconName, "_pressed"));
		if (icon != null) {
			setPressedIcon(icon);
		}

		icon = readIcon(FileUtil.appendToFileName(defaultIconName, "_disabled"));
		if (icon != null) {
			setDisabledIcon(icon);
		}

		icon = readIcon(FileUtil.appendToFileName(defaultIconName, "_mouseover"));
		if (icon != null) {
			setRolloverIcon(icon);
		}
	}


	/**
	 * Set icons based on standard naming convention from a base image name.
	 *
	 * @param iconName the base image resource name used when the button is in
	 *                 the normal state and which the other state names are
	 *                 derived from.
	 */

	public void setIconName(String iconName) {
		setIcons(iconName);
	}

	@Override
	public void setNextIcon(AnimatedIconStage stage) {
		switch (stage.iconType) {
			case PRESSEDICON -> setPressedIcon(stage.icon);
			case DISABLEDICON -> setDisabledIcon(stage.icon);
			case SELECTEDICON -> setSelectedIcon(stage.icon);
			case DISABLEDSELECTEDICON -> setDisabledSelectedIcon(stage.icon);
			case ROLLOVERICON -> setRolloverIcon(stage.icon);
			case ROLLOVERSELECTEDICON -> setRolloverSelectedIcon(stage.icon);
			default -> setIcon(stage.icon);
		}
	}
}
