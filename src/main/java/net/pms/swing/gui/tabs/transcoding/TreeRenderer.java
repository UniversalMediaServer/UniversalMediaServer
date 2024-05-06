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
package net.pms.swing.gui.tabs.transcoding;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
import net.pms.encoders.Engine;
import net.pms.swing.SwingUtil;
import net.pms.swing.gui.JavaGui;

public class TreeRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 8830634234336247114L;
	private static final String ICON_TREEMENU_CATEGORY = "icon-treemenu-category." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_TREEMENU_ENGINE_DISABLED = "icon-treemenu-enginedisabled." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_TREEMENU_ENGINE_ENABLED = "icon-treemenu-engineenabled." + (SwingUtil.HDPI_AWARE ? "svg" : "png");
	private static final String ICON_TREEMENU_ENGINE_WARNING = "icon-treemenu-enginewarning." + (SwingUtil.HDPI_AWARE ? "svg" : "png");

	private final transient Border border = BorderFactory.createEmptyBorder(0, 3, 0, 3);

	/**
	 * Sets the color to use for the background if node is selected.
	 */
	@Override
	public void setBackgroundSelectionColor(Color newColor) {
		backgroundSelectionColor = new Color(57, 114, 147);
	}

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
			Engine player = ((TreeNodeSettings) value).getPlayer();
			if (player == null) {
				setIcon(JavaGui.readImageIcon(ICON_TREEMENU_CATEGORY));
				setToolTipText(null);
			} else {
				if (player.isEnabled()) {
					if (player.isAvailable()) {
						setIcon(JavaGui.readImageIcon(ICON_TREEMENU_ENGINE_ENABLED));
					} else {
						setIcon(JavaGui.readImageIcon(ICON_TREEMENU_ENGINE_WARNING));
					}
				} else {
					setIcon(JavaGui.readImageIcon(ICON_TREEMENU_ENGINE_DISABLED));
				}
				setToolTipText(player.getStatusText());
			}

			if (player != null && ((TreeNodeSettings) value).getParent().getIndex((TreeNodeSettings) value) == 0) {
				setFont(getFont().deriveFont(Font.BOLD));
			} else {
				setFont(getFont().deriveFont(Font.PLAIN));
			}
		} else {
			setIcon(JavaGui.readImageIcon(ICON_TREEMENU_CATEGORY));
		}

		setBorder(border);

		return this;
	}

}
