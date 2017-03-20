package net.pms.util.tree;

import java.util.ArrayList;
import java.util.Stack;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

// @author Santhosh Kumar T - santhosh@in.fiorano.com 
public class CheckTreeSelectionModel extends DefaultTreeSelectionModel {
	private static final long serialVersionUID = 6785975582865715495L;
	private TreeModel model;

	public CheckTreeSelectionModel(TreeModel model) {
		this.model = model;
		setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	/**
	 * Whether there is any unselected node in the subtree of given path.
	 */
	public boolean isPartiallySelected(TreePath path) {
		if (isPathSelected(path, true)) {
			return false;
		}
		TreePath[] selectionPaths = getSelectionPaths();
		for (TreePath selectionPath : selectionPaths) {
			if (isDescendant(selectionPath, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether given path is selected. 
	 * If dig is true, then a path is assumed to be selected, if one of its
	 * ancestors is selected.
	 */
	public boolean isPathSelected(TreePath path, boolean dig) {
		if (!dig) {
			return super.isPathSelected(path);
		}
		while (path != null && !super.isPathSelected(path)) {
			path = path.getParentPath();
		}
		return path != null;
	}

	/**
	 * Whether path1 is a descendant of path2.
	 */
	private boolean isDescendant(TreePath path1, TreePath path2) {
		Object obj1[] = path1.getPath();
		Object obj2[] = path2.getPath();
		for (int i = 0; i < obj2.length; i++) {
			if (obj1[i] != obj2[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setSelectionPaths(TreePath[] pPaths) {
		clearSelection();
		for (TreePath pPath : pPaths) {
			addSelectionPath(pPath);
		}
	}

	@Override
	public void addSelectionPaths(TreePath[] paths) {
		// unselect all descendants of paths[]
		for (TreePath path : paths) {
			TreePath[] selectionPaths = getSelectionPaths();
			if (selectionPaths == null) {
				break;
			}
			ArrayList<TreePath> toBeRemoved = new ArrayList<>();
			for (TreePath selectionPath : selectionPaths) {
				if (isDescendant(selectionPath, path)) {
					toBeRemoved.add(selectionPath);
				}
			}
			super.removeSelectionPaths(toBeRemoved.toArray(new TreePath[0]));
		}

		/**
		 * If all siblings are selected then unselect them and select parent
		 * recursively, otherwise just select that path.
		 */
		for (TreePath path : paths) {
			TreePath temp = null;
			while (areSiblingsSelected(path)) {
				temp = path;
				if (path.getParentPath() == null) {
					break;
				}
				path = path.getParentPath();
			}
			if (temp != null) {
				if (temp.getParentPath() != null) {
					addSelectionPath(temp.getParentPath());
				} else {
					if (!isSelectionEmpty()) {
						removeSelectionPaths(getSelectionPaths());
					}
					super.addSelectionPaths(new TreePath[]{temp});
				}
			} else {
				super.addSelectionPaths(new TreePath[]{path});
			}
		}
	}

	/**
	 * Whether all siblings of given path are selected.
	 */
	private boolean areSiblingsSelected(TreePath path) {
		TreePath parent = path.getParentPath();
		if (parent == null) {
			return true;
		}
		Object node = path.getLastPathComponent();
		Object parentNode = parent.getLastPathComponent();

		int childCount = model.getChildCount(parentNode);
		for (int i = 0; i < childCount; i++) {
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

	@Override
	public void removeSelectionPaths(TreePath[] paths) {
		for (TreePath path : paths) {
			if (path.getPathCount() == 1) {
				super.removeSelectionPaths(new TreePath[]{path});
			} else {
				toggleRemoveSelection(path);
			}
		}
	}

	/**
	 * If any ancestor node of given path is selected then unselect it 
	 * and selection all its descendants except given path and descendants. 
	 * Otherwise just unselect the given path.
	 */
	private void toggleRemoveSelection(TreePath path) {
		Stack<TreePath> stack = new Stack<>();
		TreePath parent = path.getParentPath();
		while (parent != null && !isPathSelected(parent)) {
			stack.push(parent);
			parent = parent.getParentPath();
		}
		if (parent != null) {
			stack.push(parent);
		} else {
			super.removeSelectionPaths(new TreePath[]{path});
			return;
		}

		while (!stack.isEmpty()) {
			TreePath temp = stack.pop();
			TreePath peekPath = stack.isEmpty() ? path : (TreePath) stack.peek();
			Object node = temp.getLastPathComponent();
			Object peekNode = peekPath.getLastPathComponent();
			int childCount = model.getChildCount(node);
			for (int i = 0; i < childCount; i++) {
				Object childNode = model.getChild(node, i);
				if (childNode != peekNode) {
					super.addSelectionPaths(new TreePath[]{temp.pathByAddingChild(childNode)});
				}
			}
		}
		super.removeSelectionPaths(new TreePath[]{parent});
	}
}
