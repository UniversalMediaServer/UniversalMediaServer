package net.pms.external;

import javax.swing.JComponent;

/**
 * Base interface for pms plugins. Classes implementing this interface and
 * packaged as pms plugins will show up in the plugins section of the GUI.
 */
public interface ExternalListener {
	/**
	 * Gets the graphical component to configure the plugin. If no configuration
	 * is required, return null.
	 * 
	 * @return JComponent for plugin configuration
	 */
	public JComponent config();

	/**
	 * The name of the plugin, as it will be shown in the GUI
	 * 
	 * @return name of the plugin
	 */
	public String name();

	/**
	 * Called when pms is being closed. When this method returns, the plugin
	 * must guarantee to have closed all threads it might have created and
	 * released all resources.
	 */
	public void shutdown();
}