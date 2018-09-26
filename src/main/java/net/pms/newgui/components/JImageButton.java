/*
 * Digital Media Server, for streaming digital media to DLNA compatible devices
 * based on www.ps3mediaserver.org and www.universalmediaserver.com.
 * Copyright (C) 2016 Digital Media Server developers.
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
package net.pms.newgui.components;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.components.AnimatedIcon.AnimatedIconStage;

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

	protected String appendToFileName(String fileName, String append) {
		int i = fileName.lastIndexOf(".");
		if (i < 0) {
			return fileName + append;
		}
		return fileName.substring(0, i) + append + fileName.substring(i);
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

		icon = LooksFrame.readImageIcon(appendToFileName(defaultIconName, "_pressed"));
		if (icon != null) {
			setPressedIcon(icon);
		}

		icon = LooksFrame.readImageIcon(appendToFileName(defaultIconName, "_disabled"));
		if (icon != null) {
			setDisabledIcon(icon);
		}

		icon = LooksFrame.readImageIcon(appendToFileName(defaultIconName, "_mouseover"));
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
	public JToolTip createToolTip() {
		JToolTip tip = new HyperLinkToolTip();
		tip.setComponent(this);
		return tip;
	}

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
