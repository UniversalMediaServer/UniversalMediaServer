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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.tree.CheckTreeManager;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectRenderers extends JPanel {
	private static final long serialVersionUID = -2724796596060834064L;
	private static PmsConfiguration configuration = PMS.getConfiguration();
	private static ArrayList<String> allRenderersNames = RendererConfiguration.getAllRenderersNames();
	private static List<String> selectedRenderers = configuration.getSelectedRenderers();
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectRenderers.class);
	private CheckTreeManager checkTreeManager;
	private final JTree SrvTree;
	private final DefaultMutableTreeNode allRenderers;
	private String rootName = Messages.getString("GeneralTab.13");

	public SelectRenderers() {
		super(new BorderLayout());
		JPanel checkPanel = new JPanel();
		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(new Locale(configuration.getLanguage())));
		add(checkPanel, BorderLayout.LINE_START);
		checkPanel.applyComponentOrientation(ComponentOrientation.getOrientation(new Locale(configuration.getLanguage())));
		add(checkPanel, BorderLayout.LINE_START);
		allRenderers = new DefaultMutableTreeNode(rootName);
		for (String renderer : allRenderersNames) {
			allRenderers.add(new DefaultMutableTreeNode(renderer));
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
		// Refresh setting if modified
		selectedRenderers = configuration.getSelectedRenderers();
		TreePath[] renderersPath = new TreePath[selectedRenderers.size()];
		TreePath root = new TreePath(allRenderers);
		int i = 0;
		for (String renderer : selectedRenderers) {
			if (selectedRenderers.size() == 1) {
				renderersPath[i] = root;
			} else {
				int childNumber = allRenderers.getChildCount();
				for (int j = 0; j < childNumber; j++) {
					if (allRenderers.getChildAt(j).toString().matches(renderer)) {
						renderersPath[i] = root.pathByAddingChild(allRenderers.getChildAt(j));
						break;
					}
				}
			}

			i++;
		}

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
			StringBuilder nameStr = new StringBuilder();
			for (TreePath render : selected) {
				String[] treePathString = render.toString().split(",");
				nameStr.setLength(0);
				if (treePathString.length > 1) {
					nameStr.append(treePathString[1].substring(0, treePathString[1].indexOf("]")).trim());
					buildSelectedRenders.append(nameStr).append(",");
				} else if (selected.length == 1) {
					buildSelectedRenders.append(configuration.ALL_RENDERERS);
				}
			}

			configuration.setSelectedRenderers(buildSelectedRenders.toString());
			try {
				configuration.save();
			} catch (ConfigurationException e) {
				LOGGER.error("Could not save configuration", e);
			}

		}
	}
}
