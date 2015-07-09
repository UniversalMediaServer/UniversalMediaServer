/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012  UMS developers.
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

package net.pms.newgui;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.tree.CheckTreeManager;

public class SelectRenderers extends JPanel {
	private static final long serialVersionUID = -2724796596060834064L;
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static List<String> selectedRenderers = configuration.getSelectedRenderers();
	private CheckTreeManager checkTreeManager;
	private JTree SrvTree;
	private DefaultMutableTreeNode allRenderers;
	private static final String allRenderersTreeName = configuration.ALL_RENDERERS;
	private boolean init = false;

	public SelectRenderers() {
		super(new BorderLayout());
	}

	public void build() {
		JPanel checkPanel = new JPanel();
		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(new Locale(configuration.getLanguage())));
		add(checkPanel, BorderLayout.LINE_START);
		allRenderers = new DefaultMutableTreeNode(Messages.getString("GeneralTab.13"));
		DefaultMutableTreeNode renderersGroup = null;
		String lastGroup = null;
		String groupName;
		boolean firstLoop = true;
		for (String renderer : RendererConfiguration.getAllRenderersNames()) {
			if (lastGroup != null && renderer.startsWith(lastGroup)) {
				if (renderer.indexOf(" ") > 0 ) {
					renderersGroup.add(new DefaultMutableTreeNode(renderer.substring(renderer.indexOf(" "))));
				} else {
					renderersGroup.add(new DefaultMutableTreeNode(renderer));
				}

			} else {
				if (!firstLoop) {
					allRenderers.add(renderersGroup);
				}
				
				if (renderer.indexOf(" ") > 0) {
					groupName = renderer.substring(0, renderer.indexOf(" "));
					renderersGroup = new DefaultMutableTreeNode(groupName);
					renderersGroup.add(new DefaultMutableTreeNode(renderer.substring(renderer.indexOf(" "))));
				} else {
					groupName = renderer;
					renderersGroup = new DefaultMutableTreeNode(groupName);
				}
				
				lastGroup = groupName;
				firstLoop = false;
			}
		}

		SrvTree = new JTree(new DefaultTreeModel(allRenderers));
		checkTreeManager = new CheckTreeManager(SrvTree); 
		checkPanel.add(new JScrollPane(SrvTree));
		checkPanel.setSize(400, 500);

	}

	/**
	 * Create the GUI and show it.
	 */
	public void showDialog() {
		if (!init) {
			// Initial call
			build();
			init = true;
		}
		// Refresh setting if modified
		selectedRenderers = configuration.getSelectedRenderers();
		TreePath[] renderersPath = new TreePath[selectedRenderers.size()];
		TreePath root = new TreePath(allRenderers);
		int i = 0;
		for (String renderer : selectedRenderers) {
			if (allRenderersTreeName.equals(renderer)) {
				renderersPath[i] = root;
			} else {
				int childNumber = allRenderers.getChildCount();
				for (int j = 0; j < childNumber; j++) {
					TreeNode node = allRenderers.getChildAt(j); // node represents simple renderer or group of renderers family
					int nodeChildren = node.getChildCount();
					int endIndex = renderer.indexOf(" ");
					if (node.toString().equals(renderer)) { // group has all children selected or renderer is only one word name
						renderersPath[i] = root.pathByAddingChild(node);
						i++;
						break;
					} else {
						if (endIndex > 0 && nodeChildren > 0) {
							if (node.toString().equals(renderer.substring(0, endIndex))) {
								for (int jj = 0; jj < nodeChildren; jj++) {
									if (node.getChildAt(jj).toString().equals(renderer.substring(renderer.indexOf(" ")))) {
										renderersPath[i] = root.pathByAddingChild(node).pathByAddingChild(node.getChildAt(jj));
										i++;
										break;
									}
								}
							
								break;	
							}
						}
						
					}
				}
			}
		}

		SrvTree.validate();
		checkTreeManager.getSelectionModel().setSelectionPaths(renderersPath);
		int selectRenderers = JOptionPane.showOptionDialog(
			(Component) PMS.get().getFrame(),
			this,
			Messages.getString("GeneralTab.5"),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			null
		);

		if (selectRenderers == JOptionPane.OK_OPTION) {
			StringBuilder buildSelectedRenders = new StringBuilder();
			TreePath[] selected = checkTreeManager.getSelectionModel().getSelectionPaths();
			for (TreePath path : selected) {
				String[] treePathString = path.toString().split(",");
				StringBuilder nameStr = new StringBuilder();
				int beginIndex;
				int endIndex;
				if (treePathString.length > 1) {
					if (treePathString[0].contains(allRenderers.toString())) {
						for (i = 1; i < treePathString.length; i++) {
							beginIndex = treePathString[i].lastIndexOf("[") + 1;
							endIndex = treePathString[i].indexOf("]");
							if (endIndex > beginIndex) {
								nameStr.append(treePathString[i].substring(beginIndex, endIndex).trim());
							} else {
								nameStr.append(treePathString[i].trim()).append(" ");
							}
						}
					}
				} else {
					if (selected.length == 1) {
						nameStr.append(allRenderersTreeName);
					}
				}

				buildSelectedRenders.append(nameStr).append(",");
			}

			configuration.setSelectedRenderers(buildSelectedRenders.toString());
		}
	}
}
