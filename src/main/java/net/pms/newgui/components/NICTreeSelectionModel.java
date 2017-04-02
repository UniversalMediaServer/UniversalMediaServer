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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NICTreeSelectionModel extends DefaultTreeSelectionModel {
	private static final long serialVersionUID = 6785975582865715495L;
	private static final Logger LOGGER = LoggerFactory.getLogger(NICTreeSelectionModel.class);
	protected final TreeModel model;

	public NICTreeSelectionModel(TreeModel model) {
		this.model = model;
		setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	/**
	 * @return Whether there is any unselected node in the subtree of given path
	 */
	public boolean isPartiallySelected(TreePath path) {
		TreePath[] selectionPaths = getSelectionPaths();
		for (TreePath selectionPath : selectionPaths) {
			if (isDescendant(selectionPath, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This is used to show IP addresses as partially selected if none of them
	 * are selected but their parent network interface is.
	 * <p>
	 * The conditions for this to be true are:
	 * <li> The node examined is an address node.
	 * <li> The (first) ancestor interface is selected.
	 * <li> None of the (first) ancestor interface's descendant is selected
	 *
	 * @param path the {@link TreePath} to check.
	 * @return The evaluation result.
	 */
	public boolean isShowPartiallySelectedWhenNot(TreePath path) {
		Object node = path.getLastPathComponent();
		if (
			node == null ||
			!(node instanceof NICTreeNode) ||
			!((NICTreeNode) node).isAddress()
		) {
			return false;
		}

		NICTreeNode parentInterface = (NICTreeNode) node;
		while (parentInterface != null && !parentInterface.isInterface()) {
			parentInterface = (NICTreeNode) parentInterface.getParent();
		}
		if (parentInterface == null) {
			LOGGER.error("Enexpected error while traversing network configuration tree: No parent interface found");
			return false;
		}

		if (!isPathSelected(new TreePath(parentInterface.getPath()))) {
			return false;
		}
		return !isAnyDescendantSelected(parentInterface, null);
	}

	/**
	 * Adds all child nodes recursively of the given {@link TreePath} to the
	 * passed {@link List} of {@link NICTreeNode}s. All nodes must be
	 * {@link NICTreeNode} or a subclass.
	 *
	 * @param nodes the {@link List} to manipulate.
	 * @param path the {@link TreePath} for which to add descendant nodes.
	 */
	public void addDescendant(List<NICTreeNode> nodes, TreePath path) {
		if (nodes == null || path == null) {
			return;
		}
		addDescendant(nodes, (TreeNode) path.getLastPathComponent());
	}

	/**
	 * Adds all child nodes recursively of the given {@link TreeNode} to the
	 * passed {@link List} of {@link NICTreeNode}s. All nodes must be
	 * {@link NICTreeNode} or a subclass.
	 *
	 * @param nodes the {@link List} to manipulate.
	 * @param node the {@link TreeNode} for which to add descendant nodes.
	 */
	public void addDescendant(List<NICTreeNode> nodes, TreeNode node) {
		if (nodes == null || node == null) {
			return;
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			TreeNode subNode = node.getChildAt(i);
			nodes.add((NICTreeNode) subNode);
			if (subNode.getChildCount() > 0) {
				addDescendant(nodes, subNode);
			}
		}
	}

	/**
	 * @return Whether all siblings of given path are selected.
	 */
	protected boolean areSiblingsSelected(TreePath path) {
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return true;
		}
		Object node = path.getLastPathComponent();
		Object parentNode = parent.getLastPathComponent();

		for (int i = 0; i < model.getChildCount(parentNode); i++) {
			Object childNode = model.getChild(parentNode, i);
			if (childNode == node) {
				continue;
			}
			if (!isPathSelected(parent.pathByAddingChild(childNode))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines if any descendant node, except {@code excluded}, is selected.
	 *
	 * @param node the {@link TreeNode} for which to examine children.
	 * @param exclude exclude this {@link TreeNode} and its descendant from
	 *                evaluation. Can be {@code null}.
	 * @return The evaluation result.
	 */
	protected boolean isAnyDescendantSelected(TreeNode node, List<NICTreeNode> excluded) {
		if (node == null) {
			return false;
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			if (!(node.getChildAt(i) instanceof DefaultMutableTreeNode)) {
				throw new IllegalStateException("All nodes must be DefaultMutableTreeNode or subclass");
			}
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
			if ((excluded == null || !excluded.contains(childNode)) && isPathSelected(new TreePath(childNode.getPath()))) {
				return true;
			}
			if (childNode.getChildCount() > 0) {
				if (isAnyDescendantSelected(childNode, excluded)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if any other {@link TreeNode} belonging to the same network
	 * interface parent as the given node is selected.
	 *
	 * @param path the {@link TreePath} to the {@link TreeNode} to examine.
	 * @return The evaluation result.
	 */
	protected boolean isAnyAddressUnderParentInterfaceSelected(TreePath path, List<NICTreeNode> excluded) {
		if (!(path.getLastPathComponent() instanceof NICTreeNode)) {
			throw new IllegalStateException("All nodes must be NICTreeNode or subclass");
		}
		NICTreeNode node = (NICTreeNode) path.getLastPathComponent();
		if (!node.isAddress()) {
			return false;
		}
		NICTreeNode parentInterface = node;
		while (parentInterface != null && !parentInterface.isInterface()) {
			parentInterface = (NICTreeNode) parentInterface.getParent();
		}
		if (parentInterface == null) {
			LOGGER.error("Enexpected error while traversing network configuration tree: No parent interface found");
			return false;
		}

		return isAnyDescendantSelected(parentInterface, excluded);
	}

	/**
	 * @return Whether {@code path1} is a descendant of {@code path2}.
	 */
	protected boolean isDescendant(TreePath path1, TreePath path2) {
		Object[] obj1 = path1.getPath();
		Object[] obj2 = path2.getPath();
		if (obj1.length <= obj2.length) {
			return false;
		}
		for (int i = 0; i < obj2.length; i++) {
			if (obj1[i] != obj2[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Used to manipulate internal selection {@link List}.
	 */
	protected void addListSelectionPath(List<TreePath> selection, TreePath path) {
		addListSelectionPath(selection, path, true, true);
	}

	/**
	 * Used to manipulate internal selection {@link List}.
	 */
	protected void addListSelectionPath(List<TreePath> selection, TreePath path, boolean addParents, boolean addChildren) {
		if (selection == null || path == null) {
			return;
		}

		if (path.getLastPathComponent() instanceof NICTreeNode) {
			NICTreeNode node = (NICTreeNode) path.getLastPathComponent();

			if (addParents && path.getParentPath() != null) {
				if (path.getParentPath().getLastPathComponent() instanceof NICTreeNode) {
					NICTreeNode parentNode = (NICTreeNode) path.getParentPath().getLastPathComponent();
					if (areSiblingsSelected(path)) {
						addListSelectionPath(selection, path.getParentPath(), true, false);
					} else if (node.isAddress()) {
						while (parentNode != null && !parentNode.isInterface()) {
							parentNode = (NICTreeNode) parentNode.getParent();
						}
						if (parentNode != null) {
							addListSelectionPath(selection, new TreePath(parentNode.getPath()), true, false);
						}
					}
				} else {
					throw new IllegalStateException("All nodes must be NICTreeNode or subclass");
				}
			}

			if (!selection.contains(path)) {
				selection.add(path);
			}

			if (addChildren) {
				if (node.isInterface()) {
					// Special for this class is that if all children are addresses
					// they shouldn't automatically be selected if the current node
					// is an interface.
					boolean allChildrenAreAddresses = true, someChildrenAreSelected = false;
					for (int i = 0; i < node.getChildCount(); i++) {
						if (allChildrenAreAddresses && (!(node.getChildAt(i) instanceof NICTreeNode) || !((NICTreeNode) node.getChildAt(i)).isAddress())) {
							// Found a non-address child, which means that normal "propagate to child" rules apply.
							allChildrenAreAddresses = false;
						}
						if (!someChildrenAreSelected && selection.contains(path.pathByAddingChild(node.getChildAt(i)))) {
							someChildrenAreSelected = true;
						}
					}

					if (allChildrenAreAddresses) {
						// Remove the selection for any child addresses recursively
						if (someChildrenAreSelected) {
							for (int i = 0; i < node.getChildCount(); i++) {
								TreePath childPath = path.pathByAddingChild(node.getChildAt(i));
								removeListSelectionPath(selection, childPath, false, true, null);
							}
						}
					} else {
						for (int i = 0; i < node.getChildCount(); i++) {
							addListSelectionPath(selection, path.pathByAddingChild(node.getChildAt(i)), false, true);
						}
					}
				} else {
					for (int i = 0; i < node.getChildCount(); i++) {
						addListSelectionPath(selection, path.pathByAddingChild(node.getChildAt(i)), false, true);
					}
				}
			}
		} else {
			throw new IllegalStateException("All nodes must be NICTreeNode or subclass");
		}
	}

	/**
	 * Used to manipulate internal selection {@link List}.
	 */
	protected void removeListSelectionPath(List<TreePath> selection, TreePath path) {
		removeListSelectionPath(selection, path, true, true, new ArrayList<NICTreeNode>());
	}

	/**
	 * Used to manipulate internal selection {@link List}.
	 *
	 * @param selection the {@link List} of selection {@link TreePath}s to manipulate.
	 * @param path the {@link TreePath} to unselect.
	 * @param removeParents whether removal should traverse upwards.
	 * @param removeChildren whether removal should traverse downwards.
	 * @param excluded {@link NICTreeNode}s to exclude when evaluating if any
	 *                 other address belonging to the same network interface
	 *                 is selected when traversing upwards. Can be null. Has no
	 *                 effect unless {@code removeParents} is {@code true}.
	 */
	protected void removeListSelectionPath(List<TreePath> selection, TreePath path, boolean removeParents, boolean removeChildren, List<NICTreeNode> excluded) {
		if (selection == null || path == null) {
			return;
		}

		NICTreeNode node;
		if (path.getLastPathComponent() instanceof NICTreeNode) {
			node = (NICTreeNode) path.getLastPathComponent();
		} else {
			throw new IllegalStateException("All nodes must be NICTreeNode or subclass");
		}

		TreePath parent = path.getParentPath();
		if (removeParents && parent != null) {
			if (parent.getLastPathComponent() instanceof NICTreeNode) {
				NICTreeNode parentNode = (NICTreeNode) parent.getLastPathComponent();
				if (excluded != null) {
					excluded.add(node);
					if (removeChildren) {
						addDescendant(excluded, node);
					}
				}
				if (!node.isAddress() || parentNode.isAddress() || !isAnyAddressUnderParentInterfaceSelected(path, excluded)) {
					removeListSelectionPath(selection, parent, true, false, excluded);
				}
			} else {
				throw new IllegalStateException("All nodes must be NICTreeNode or subclass");
			}
		}

		if (selection.contains(path)) {
			selection.remove(path);
		}

		if (removeChildren) {
			for (int i = 0; i < node.getChildCount(); i++) {
				removeListSelectionPath(selection, path.pathByAddingChild(node.getChildAt(i)), false, true, null);
			}
		}
	}

	/**
	 * Adds or removes the given array of {@link TreePath} to/from the
	 * selection.
	 *
	 * @param paths the {@link TreePath}s to add or remove.
	 * @param add the operation to perform, {@code true} means add,
	 *            {@code false} means remove.
	 */
	public void modifySelectionPaths(TreePath[] paths, boolean add) {
		if (paths == null || paths.length == 0) {
			return;
		}

		List<TreePath> selection = new ArrayList<>(Arrays.asList(getSelectionPaths()));

		for (TreePath path : paths) {
			if (add) {
				addListSelectionPath(selection, path);
			} else {
				removeListSelectionPath(selection, path);
			}
		}

		Collections.sort(selection, new Comparator<TreePath>() {

			@Override
			public int compare(TreePath path1, TreePath path2) {
				return path2.toString().compareTo(path1.toString());
			}
		});

		setSelectionPaths(selection.toArray(new TreePath[selection.size()]));
	}

	@Override
	public void addSelectionPaths(TreePath[] paths) {
		modifySelectionPaths(paths, true);
	}

	@Override
	public void removeSelectionPaths(TreePath[] paths) {
		modifySelectionPaths(paths, false);
	}
}
