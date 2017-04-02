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
package net.pms.newgui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import com.jgoodies.forms.layout.Sizes;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.components.NICTreeNode.NICTreeNodeType;
import net.pms.util.tree.TristateCheckBox;

public class NICTreeCellRenderer extends JPanel implements TreeCellRenderer {

	private static final long serialVersionUID = 3172174288894253285L;
	protected NICTreeSelectionModel selectionModel;
	protected TreeCellRenderer delegate;
	protected TristateCheckBox checkBox = new TristateCheckBox();

	public NICTreeCellRenderer(TreeCellRenderer delegate, NICTreeSelectionModel selectionModel) {
		this.delegate = delegate;
		this.selectionModel = selectionModel;
		setLayout(new BorderLayout(Sizes.dluX(5).getPixelSize(this),0));
		setOpaque(false);
		checkBox.setOpaque(false);
	}

	@Override
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus
	) {
		Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		TreePath path = tree.getPathForRow(row);
		NICTreeNode node = value instanceof NICTreeNode ? (NICTreeNode) value : null;
		if (path != null) {
			if (selectionModel.isPathSelected(path)) {
				checkBox.setState(TristateCheckBox.SELECTED);
			} else if (selectionModel.isPartiallySelected(path)) {
				checkBox.setState(TristateCheckBox.DONT_CARE);
			} else if (node != null && selectionModel.isShowPartiallySelectedWhenNot(path)) {
				checkBox.setState(TristateCheckBox.DONT_CARE);
			} else {
				checkBox.setState(TristateCheckBox.NOT_SELECTED);
			}
		}

		if (node != null) {
			if (node.getNodeType() == NICTreeNodeType.INTERFACE) {
				setIcon(LooksFrame.readImageIcon("icon-treemenu-nic.png"));
				setText(node.getNetworkInterface().getDisplayName());
			} else if (node.getNodeType() == NICTreeNodeType.ADDRESS) {
				setIcon(LooksFrame.readImageIcon("icon-treemenu-ip.png"));
			} else {
				setIcon(LooksFrame.readImageIcon("icon-treemenu-category.png"));
			}
		}

		removeAll();
		add(checkBox, BorderLayout.LINE_START);
		add(renderer, BorderLayout.CENTER);

		return this;
	}

    public void setIcon(Icon icon) {
    	if (delegate instanceof DefaultTreeCellRenderer) {
    		((DefaultTreeCellRenderer) delegate).setIcon(icon);
    	}
    }

    public void setText(String text) {
    	if (delegate instanceof DefaultTreeCellRenderer) {
    		((DefaultTreeCellRenderer) delegate).setText(text);
    	}
    }
}
