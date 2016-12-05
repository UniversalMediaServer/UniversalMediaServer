package net.pms.util.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

 /**
 * Implements a {@link TreeCellRenderer} that support {@link JCheckBox} nodes.
 * <p>
 * Based on <a href="http://www.jroller.com/santhosh/entry/jtree_with_checkboxes">JTree with checkboxes</a>.
 *
 * @author Santhosh Kumar T - santhosh@in.fiorano.com
 * @author valib
 */
public class CheckTreeCellRenderer extends JPanel implements TreeCellRenderer {
	private static final long serialVersionUID = 5762499761493341720L;
	protected CheckTreeSelectionModel selectionModel;
	protected TreeCellRenderer delegate;
	protected TristateCheckBox checkBox = new TristateCheckBox();

	public CheckTreeCellRenderer(TreeCellRenderer delegate, CheckTreeSelectionModel selectionModel) {
		this.delegate = delegate;
		this.selectionModel = selectionModel;
		setLayout(new BorderLayout());
		setOpaque(false);
		checkBox.setOpaque(false);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		TreePath path = tree.getPathForRow(row);
		if (path != null) {
			if (selectionModel.isPathSelected(path, true)) {
				checkBox.setState(TristateCheckBox.SELECTED);
			} else {
				checkBox.setState(selectionModel.isPartiallySelected(path) ? TristateCheckBox.DONT_CARE : TristateCheckBox.NOT_SELECTED);
			}
		}
		removeAll();
		add(checkBox, BorderLayout.LINE_START);
		add(renderer, BorderLayout.CENTER);
		return this;
	}
}
