package net.pms.external;

/**
 * Base interface for pms plugins. Classes implementing this interface and
 * packaged as pms plugins will show up in the plugins section of the GUI.
 */
public interface ExternalListener {
	/**
	 * The name of the plugin, as it will be shown in the GUI
	 *
	 * @return name of the plugin
	 */
	public String name();
}
