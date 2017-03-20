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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class SearchableMutableTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 8466944555448955118L;

	public SearchableMutableTreeNode(String nodeName) {
		super(nodeName);
	}

	public SearchableMutableTreeNode(String nodeName, boolean allowsChildren) {
		super(nodeName, allowsChildren);
	}

	protected SearchableMutableTreeNode findChild(String searchName, boolean recursive, boolean specialGroupRules) throws IllegalChildException {
		if (getChildCount() > 0) {
			for (int i = 0; i < getChildCount(); i++) {
				TreeNode currentTNChild = getChildAt(i);
				if (currentTNChild instanceof SearchableMutableTreeNode) {
					SearchableMutableTreeNode currentChild = (SearchableMutableTreeNode) currentTNChild;
					if (currentChild.getNodeName().equalsIgnoreCase(searchName)) {
						return currentChild;
					} else if (specialGroupRules && searchName.equalsIgnoreCase(currentChild.getParent().getNodeName() + " " + currentChild.getNodeName())) {
						// Search for the special group rule where grouping is done on the first word (so that the parent has the first word in the name)
						return currentChild;
					}
				} else {
					throw new IllegalChildException("All children must be SearchMutableTreeNode or subclasses thereof");
				}
			}
			// Do recursive search in separate loop to avoid finding a matching subnode before a potential match at the current level
			if (recursive) {
				SearchableMutableTreeNode result = null;
				for (int i = 0; i < getChildCount(); i++) {
					SearchableMutableTreeNode currentChild = (SearchableMutableTreeNode) getChildAt(i);
					if (!currentChild.isLeaf()) {
						result = currentChild.findChild(searchName, true, specialGroupRules);
						if (result != null) {
							return result;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Search the node's immediate children
	 * @param searchObject the object to search for
	 * @return the found node or null
	 * @throws IllegalChildException if a child that's not a SearchableMutableTreeNode or descendant is encountered
	 */
	public SearchableMutableTreeNode findChild(String searchName) throws IllegalChildException {

		return findChild(searchName, false, false);
	}

	/**
	 * Search the node's children recursively
	 * @param searchObject the object to search for
	 * @return the found node or null
	 * @throws IllegalChildException if a child that's not a SearchableMutableTreeNode or descendant is encountered
	 */
	public SearchableMutableTreeNode findInBranch(String searchName, boolean specialGroupRules) throws IllegalChildException {
		return findChild(searchName, true, specialGroupRules);
	}

	public String getNodeName() {
		return (String) super.getUserObject();
	}

    public SearchableMutableTreeNode getParent() {
        return (SearchableMutableTreeNode) parent;
    }

}
