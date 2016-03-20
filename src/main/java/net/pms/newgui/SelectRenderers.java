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

package net.pms.newgui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.newgui.components.IllegalChildException;
import net.pms.newgui.components.SearchableMutableTreeNode;
import net.pms.util.tree.CheckTreeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectRenderers extends JPanel {
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectRenderers.class);
	private static final long serialVersionUID = -2724796596060834064L;
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static List<String> selectedRenderers = configuration.getSelectedRenderers();
	private CheckTreeManager checkTreeManager;
	private JTree SrvTree;
	private SearchableMutableTreeNode allRenderers;
	private static final String allRenderersTreeName = configuration.ALL_RENDERERS;
	private boolean init = false;

	public SelectRenderers() {
		super(new BorderLayout());
	}

	public void build() {
		JPanel checkPanel = new JPanel();
		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(PMS.getLocale()));
		add(checkPanel, BorderLayout.LINE_START);
		allRenderers = new SearchableMutableTreeNode(Messages.getString("GeneralTab.13"));

		Pattern pattern = Pattern.compile("^\\s*([^\\s]*) ?([^\\s].*?)?\\s*$");
		for (String renderer : RendererConfiguration.getAllRenderersNames()) {
			Matcher match = pattern.matcher(renderer);
			if (match.find()) {
				// Find or create group or single name renderer
				SearchableMutableTreeNode node = null;
				try {
					 node = allRenderers.findChild(match.group(1));
				} catch (IllegalChildException e) {}
				if (node == null) {
					node = new SearchableMutableTreeNode(match.group(1));
					allRenderers.add(node);
				}
				// Find or create subgroup/name
				if (match.groupCount() > 1 && match.group(2) != null) {
					SearchableMutableTreeNode subNode = null;
					try {
						subNode = node.findChild(match.group(2));
					} catch (IllegalChildException e) {}
					if (subNode != null) {
						LOGGER.warn("Renderer {} found twice, ignoring repeated entry", renderer);
					} else {
						subNode = new SearchableMutableTreeNode(match.group(2));
						node.add(subNode);
					}
				}
			} else {
				LOGGER.warn("Can't parse renderer name \"{}\"", renderer);
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
		SrvTree.validate();
		// Refresh setting if modified
		selectedRenderers = configuration.getSelectedRenderers();
		TreePath root = new TreePath(allRenderers);
		if (selectedRenderers.isEmpty() || (selectedRenderers.size() == 1 && selectedRenderers.get(0) == null)) {
			checkTreeManager.getSelectionModel().clearSelection();
		} else if (selectedRenderers.size() == 1 && selectedRenderers.get(0).equals(allRenderersTreeName)) {
			checkTreeManager.getSelectionModel().setSelectionPath(root);
		} else {
			if (root.getLastPathComponent() instanceof SearchableMutableTreeNode) {
				SearchableMutableTreeNode rootNode = (SearchableMutableTreeNode) root.getLastPathComponent();
				SearchableMutableTreeNode node = null;
				List<TreePath> selectedRenderersPath = new ArrayList<>(selectedRenderers.size());
				for (String selectedRenderer : selectedRenderers) {
					try {
						node = rootNode.findInBranch(selectedRenderer, true);
					} catch (IllegalChildException e) {}
					if (node != null) {
						selectedRenderersPath.add(new TreePath(node.getPath()));
					}
				}
				checkTreeManager.getSelectionModel().setSelectionPaths(selectedRenderersPath.toArray(new TreePath[selectedRenderersPath.size()]));
			} else {
				LOGGER.error("Illegal node class in SelectRenderers.showDialog(): {}", root.getLastPathComponent().getClass().getSimpleName());
			}
		}

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
			TreePath[] selected = checkTreeManager.getSelectionModel().getSelectionPaths();
			if (selected.length == 0) {
				configuration.setSelectedRenderers("");
			} else if (
				selected.length == 1 && selected[0].getLastPathComponent() instanceof SearchableMutableTreeNode &&
				((SearchableMutableTreeNode) selected[0].getLastPathComponent()).getNodeName().equals(allRenderers.getNodeName())
			) {
				configuration.setSelectedRenderers(allRenderersTreeName);
			} else {
				List<String> selectedRenderers = new ArrayList<>();
				for (TreePath path : selected) {
					String rendererName = "";
					if (path.getPathComponent(0).equals(allRenderers)) {
						for (int i = 1; i < path.getPathCount(); i++) {
							if (path.getPathComponent(i) instanceof SearchableMutableTreeNode) {
								if (!rendererName.isEmpty()) {
									rendererName += " ";
								}
								rendererName += ((SearchableMutableTreeNode) path.getPathComponent(i)).getNodeName();
							} else {
								LOGGER.error("Invalid tree node component class {}", path.getPathComponent(i).getClass().getSimpleName());
							}
						}
						if (!rendererName.isEmpty()) {
							selectedRenderers.add(rendererName);
						}
					} else {
						LOGGER.warn("Invalid renderer treepath encountered: {}", path.toString());
					}
				}
				configuration.setSelectedRenderers(selectedRenderers);
			}
		}
	}
}
