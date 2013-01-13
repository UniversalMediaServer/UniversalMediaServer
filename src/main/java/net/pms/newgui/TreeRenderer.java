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

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;

public class TreeRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 8830634234336247114L;

	public TreeRenderer() {
	}

	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean sel,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus
	) {

		super.getTreeCellRendererComponent(
			tree, value, sel,
			expanded, leaf, row,
			hasFocus);
		if (leaf && value instanceof TreeNodeSettings) {
			if (((TreeNodeSettings) value).getPlayer() == null) {
				setIcon(LooksFrame.readImageIcon("icon_tree_parent-16.png"));
			} else {
				if (((TreeNodeSettings) value).isEnable()) {
					Player p = ((TreeNodeSettings) value).getPlayer();
					if (PlayerFactory.getPlayers().contains(p)) {
						setIcon(LooksFrame.readImageIcon("icon_tree_node-16.png"));
					} else {
						setIcon(LooksFrame.readImageIcon("messagebox_warning-16.png"));
					}
				} else {
					setIcon(LooksFrame.readImageIcon("icon_tree_node_fail-16.png"));
				}
			}

			if (((TreeNodeSettings) value).getPlayer() != null && ((TreeNodeSettings) value).getParent().getIndex((TreeNodeSettings) value) == 0) {
				setFont(getFont().deriveFont(Font.BOLD));
			} else {
				setFont(getFont().deriveFont(Font.PLAIN));
			}
		} else {
			setIcon(LooksFrame.readImageIcon("icon_tree_parent-16.png"));
		}
		return this;
	}
}
