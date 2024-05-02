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
package net.pms.configuration.sharedcontent;

import java.io.File;
import java.util.Objects;

public class FolderContent extends SharedContent {
	protected static final String TYPE = "Folder";
	private File file;
	private boolean monitored;
	private boolean metadata;

	public FolderContent(File file) {
		this(file, true, true);
	}

	public FolderContent(File file, boolean monitored) {
		this(file, monitored, true);
	}

	public FolderContent(File file, boolean monitored, boolean metadata) {
		this.file = file;
		this.monitored = monitored;
		this.metadata = metadata;
	}

	public void setFile(File value) {
		file = value;
	}

	public File getFile() {
		return file;
	}

	public void setMonitored(boolean value) {
		monitored = value;
	}

	/**
	 * isMonitored mean monitored for changes like file additions/removals, and
	 * fully played status.
	 * @return monitored state.
	 */
	public boolean isMonitored() {
		return monitored;
	}

	public void setMetadataAbility(boolean value) {
		metadata = value;
	}

	public boolean mayHaveMetadata() {
		return metadata;
	}

	@Override
	public boolean isExternalContent() {
		return false;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (super.equals(o) && o instanceof FolderContent other) {
			return (metadata == other.metadata &&
				monitored == other.monitored &&
				((file == null && other.file == null) ||
				file != null && file.equals(other.file)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 83 * hash + (this.file != null ? Objects.hashCode(this.file.getPath()) : 0);
		hash = 83 * hash + (this.monitored ? 1 : 0);
		hash = 83 * hash + (this.metadata ? 1 : 0);
		return hash;
	}

	@Override
	public String toString() {
		return this.file != null ? this.file.getPath() : "Empty Folder";
	}

}
