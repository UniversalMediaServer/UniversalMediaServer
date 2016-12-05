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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

public class NICTreeManager extends MouseAdapter implements TreeSelectionListener {
	private NICTreeSelectionModel selectionModel;
	private JTree tree = new JTree();
	int hotspot = new JCheckBox().getPreferredSize().width;

	public NICTreeManager(JTree tree) {
		this.tree = tree;
		selectionModel = new NICTreeSelectionModel(tree.getModel());
		tree.setCellRenderer(new NICTreeCellRenderer(tree.getCellRenderer(), selectionModel));
		tree.addMouseListener(this);
		selectionModel.addTreeSelectionListener(this);
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		TreePath path = tree.getPathForLocation(me.getX(), me.getY());
		if (path == null) {
			return;
		}
		if (me.getX() > tree.getPathBounds(path).x + hotspot) {
			return;
		}

		boolean selected = selectionModel.isPathSelected(path);
		selectionModel.removeTreeSelectionListener(this);

		try {
			if (selected) {
				selectionModel.removeSelectionPath(path);
			} else {
				selectionModel.addSelectionPath(path);
			}
		} finally {
			selectionModel.addTreeSelectionListener(this);
			tree.treeDidChange();
		}
	}

	public NICTreeSelectionModel getSelectionModel() {
		return selectionModel;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		tree.treeDidChange();
	}
}
