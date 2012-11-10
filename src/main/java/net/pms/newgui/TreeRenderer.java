/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;

public class TreeRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 8830634234336247114L;

	public TreeRenderer() {
	}

	/**
	 * Sets the color to use for the background if node is selected.
	 */
	@Override
	public void setBackgroundSelectionColor(Color newColor) {
		backgroundSelectionColor = new Color(57, 114, 147);
	}

	private Border border = BorderFactory.createEmptyBorder(0, 3, 0, 3);

	@Override
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean sel,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus
	) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

		if (leaf && value instanceof TreeNodeSettings) {
			if (((TreeNodeSettings) value).getPlayer() == null) {
				setIcon(LooksFrame.readImageIcon("icon-treemenu-category.png"));
			} else {
				if (((TreeNodeSettings) value).isEnable()) {
					Player p = ((TreeNodeSettings) value).getPlayer();

					if (PlayerFactory.getPlayers().contains(p)) {
						setIcon(LooksFrame.readImageIcon("icon-treemenu-engineenabled.png"));
					} else {
						setIcon(LooksFrame.readImageIcon("icon-treemenu-enginewarning.png"));
					}
				} else {
					setIcon(LooksFrame.readImageIcon("icon-treemenu-enginedisabled.png"));
				}
			}

			if (((TreeNodeSettings) value).getPlayer() != null && ((TreeNodeSettings) value).getParent().getIndex((TreeNodeSettings) value) == 0) {
				setFont(getFont().deriveFont(Font.BOLD));
			} else {
				setFont(getFont().deriveFont(Font.PLAIN));
			}
		} else {
			setIcon(LooksFrame.readImageIcon("icon-treemenu-category.png"));
		}

		setBorder(border);

		return this;
	}
}
