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

package net.pms.fileprovider;

import javax.swing.JComponent;

import net.pms.dlna.DLNAResource;

/**
 * All classes implementing the FileProvider Interface will be loaded when UMS
 * is being started and can be selected as file providers in the
 * 'Navigation/Share Settings' tab.
 */
public interface FileProvider {

	/**
	 * Gets the configuration panel which will be displayed in the
	 * 'Navigation/Share Settings' tab if the file provider is the active one.
	 *
	 * @return the configuration panel
	 */
	public JComponent getConfigurationPanel();

	/**
	 * Gets the root folder.</br></br> All children of the root folder (files
	 * and folders) will be shown on the first level when browsing on the
	 * renderer.
	 *
	 * @return the root folder
	 */
	public DLNAResource getRootFolder();

	/**
	 * Gets the name which will be displayed in the 'Navigation/Share Settings'
	 * tab.</br></br> This method has to be functional before
	 * {@link #activate()} is being called.
	 * 
	 * @return the name of the file provider
	 */
	public String getName();

	/**
	 * Activates the file provider.</br></br> This method is being called when
	 * UMS has been completely initialized and the file provider is the active
	 * one.</br> If the plugin has to access UMS functionality, do it here and
	 * not in the constructor.
	 */
	public void activate();

	/**
	 * Deactivates the file provider.</br></br> This method is being called when
	 * UMS shuts down or when the file provider is currently the active one and
	 * another one is being activated,
	 */
	public void deactivate();
}