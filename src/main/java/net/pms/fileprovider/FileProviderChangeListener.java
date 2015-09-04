package net.pms.fileprovider;

/**
 * Interface used to notify file provider changes.
 */
public interface FileProviderChangeListener {
	
	/**
	 * Called when the active file provider has changed.
	 *
	 * @param previousFileProvider the previous file provider
	 * @param newFileProvider the new file provider
	 */
	public void activeFileProviderChanged(FileProvider previousFileProvider, FileProvider newFileProvider);
}
