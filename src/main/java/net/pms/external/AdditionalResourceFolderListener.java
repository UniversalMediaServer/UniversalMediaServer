package net.pms.external;

import net.pms.dlna.DLNAResource;

/**
 * Classes implementing this interface and packaged as pms plugins will add a
 * single folder to every existing folder
 */
public interface AdditionalResourceFolderListener extends ExternalListener {
	/**
	 * Allows to add a virtual folder resource, similar to the #transcoded
	 * folder to every existing folder
	 * 
	 * @param currentResource
	 *            Parent resource
	 * @param child
	 *            the DLNAResource to add
	 */
	public void addAdditionalFolder(DLNAResource currentResource,
			DLNAResource child);
}