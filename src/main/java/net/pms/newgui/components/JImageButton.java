/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.newgui.components;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.UIManager;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconStage;
import net.pms.util.FileUtil;

public class JImageButton extends JButton implements AnimatedIconCallback {

	private static final long serialVersionUID = 8120596501408171329L;

	public JImageButton(String text, String iconName) {
		super(text, null);
		setProperites();
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
		setProperites();
	}

	public JImageButton(Icon icon) {
		super(icon);
		setProperites();
	}

	private void setProperites() {
		setRequestFocusEnabled(false);
		setBorderPainted(false);
		setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		setContentAreaFilled(false);
		setOpaque(false);
	}

	/**
	 * Set icons from standard naming convention based on a base image name.
	 * @param defaultIconName the base image resource name used when the
	 *                        button is in the normal state and which
	 *                        the other state names are derived from.
	 *
	 * @see JAnimatedButton#setIcons(String)
	 */
	protected void setIcons(String defaultIconName) {
		if (defaultIconName == null) {
			return;
		}

		ImageIcon icon = LooksFrame.readImageIcon(defaultIconName);
		if (icon == null) {
			setIcon(UIManager.getIcon("OptionPane.warningIcon"));
			return;
		}
		setIcon(icon);

		icon = LooksFrame.readImageIcon(FileUtil.appendToFileName(defaultIconName, "_pressed"));
		if (icon != null) {
			setPressedIcon(icon);
		}

		icon = LooksFrame.readImageIcon(FileUtil.appendToFileName(defaultIconName, "_disabled"));
		if (icon != null) {
			setDisabledIcon(icon);
		}

		icon = LooksFrame.readImageIcon(FileUtil.appendToFileName(defaultIconName, "_mouseover"));
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
			case PRESSEDICON:
				setPressedIcon(stage.icon);
				break;
			case DISABLEDICON:
				setDisabledIcon(stage.icon);
				break;
			case SELECTEDICON:
				setSelectedIcon(stage.icon);
				break;
			case DISABLEDSELECTEDICON:
				setDisabledSelectedIcon(stage.icon);
				break;
			case ROLLOVERICON:
				setRolloverIcon(stage.icon);
				break;
			case ROLLOVERSELECTEDICON:
				setRolloverSelectedIcon(stage.icon);
				break;
			default:
				setIcon(stage.icon);
		}
	}
}
