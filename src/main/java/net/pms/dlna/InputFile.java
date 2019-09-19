package net.pms.dlna;

import java.io.File;

public class InputFile {
	/**
	 * TODO: Change all instance variables to private. For backwards compatibility
	 * with external plugin code the variables have all been marked as deprecated
	 * rather than being changed to private, but this will change in the future.
	 * When everything has been changed to private, this note can be removed.
	 */

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public File file;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public IPushOutput push;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public String filename;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public Long size = null;

	/**
	 * Return the string representation of this InputFile.
	 */
	@Override
	public String toString() {
		if (file != null) {
			return file.getName();
		} else {
			if (push != null) {
				return "pipe";
			} else {
				return "";
			}
		}
	}

	/**
	 * @return the file
	 * @since 1.50
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 * @since 1.50
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @return the push
	 * @since 1.50
	 */
	public IPushOutput getPush() {
		return push;
	}

	/**
	 * @param push the push to set
	 * @since 1.50
	 */
	public void setPush(IPushOutput push) {
		this.push = push;
	}

	/**
	 * @return the filename
	 * @since 1.50
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 * @since 1.50
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		if (size == null && file != null) {
			return file.length();
		} else if (size == null) {
			return -1;
		}
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}
}
