/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util;

import java.io.File;

public class InputFile {

	private File file;
	private IPushOutput push;
	private String filename;
	private Long size = null;

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

}
