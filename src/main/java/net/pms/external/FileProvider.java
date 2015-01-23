package net.pms.external;

import javax.swing.JPanel;

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
	public JPanel getConfigurationPanel();

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
