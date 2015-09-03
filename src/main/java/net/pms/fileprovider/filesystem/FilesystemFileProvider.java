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

package net.pms.fileprovider.filesystem;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.fileprovider.FileProvider;
import net.pms.fileprovider.filesystem.dlna.RootFolder;
import net.pms.fileprovider.filesystem.gui.NavigationShareTab;

/**
 * The FilesystemFileProvider is the default UMS file provider.<br>
 * It exposes files and folders on a renderer the same way as they exist in the file system.
 */
public class FilesystemFileProvider implements FileProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemFileProvider.class);

	private boolean isActivated;

	private RootFolder rootFolder;
	private JComponent configurationPanel;

	@Override
	public JComponent getConfigurationPanel() {
		return configurationPanel;
	}

	@Override
	public DLNAResource getRootFolder() {
		return rootFolder;
	}

	@Override
	public String getName() {
		return Messages.getString("FilesystemFileProvider.1");
	}

	@Override
	public void activate() {
		if (!isActivated) {
			LOGGER.debug(String.format("Start activating FileProvider %s", getName()));

			// Initialize the root folder
			rootFolder = new RootFolder();

			// Initialize the configuration panel
			NavigationShareTab navigationShareTab = new NavigationShareTab(PMS.getConfiguration());
			configurationPanel = navigationShareTab.build();

			isActivated = true;

			LOGGER.info(String.format("FileProvider %s has been activated", getName()));
		} else {
			LOGGER.info(String.format("FileProvider %s has already been activated", getName()));
		}
	}

	@Override
	public void deactivate() {
		if (!isActivated) {
			LOGGER.debug(String.format("Start deactivating FileProvider %s", getName()));

			rootFolder = null;
			configurationPanel = null;

			isActivated = false;

			LOGGER.info(String.format("FileProvider %s has been deactivated", getName()));
		}
	}

	@Override
	public boolean isActivated() {
		return isActivated;
	}
}
