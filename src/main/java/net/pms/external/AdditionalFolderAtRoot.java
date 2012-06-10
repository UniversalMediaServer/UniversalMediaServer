package net.pms.external;

import net.pms.dlna.DLNAResource;

/**
 * Classes implementing this interface and packaged as pms plugins will show one
 * additional folder at the root level when the DLNA tree is being browsed on
 * the renderer
 */
public interface AdditionalFolderAtRoot extends ExternalListener {
	/**
	 * Gets the DLNAResource that will be added to the root folder. If it is
	 * functional, it will show up after the default folders.
	 * 
	 * @return the DLNAResource that will be added to the root folder
	 */
	public DLNAResource getChild();
}