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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Folders extends SharedContent {
	protected static final String TYPE = "Folders";
	private String parent;
	private String name;
	private List<Folder> childs;
	private boolean addToMediaLibrary;

	public Folders(String name, List<Folder> childs) {
		this(null, name, childs, true);
	}

	public Folders(String parent, String name, List<Folder> childs) {
		this(parent, name, childs, true);
	}

	public Folders(String parent, String name, List<Folder> childs, boolean addToMediaLibrary) {
		this.parent = parent;
		this.name = name;
		this.childs = childs;
		this.addToMediaLibrary = addToMediaLibrary;
	}

	public void setParent(String value) {
		parent = value;
	}

	public String getParent() {
		return parent;
	}

	public void setName(String value) {
		name = value;
	}

	public String getName() {
		return name;
	}

	public void setFolders(List<Folder> value) {
		childs = value;
	}

	public List<Folder> getFolders() {
		return childs;
	}

	public void setAddToMediaLibrary(boolean value) {
		addToMediaLibrary = value;
	}

	public boolean isAddToMediaLibrary() {
		return addToMediaLibrary;
	}

	public List<File> getFiles() {
		List<File> result = new ArrayList<>();
		if (childs != null) {
			for (Folder folder : childs) {
				if (folder != null && folder.getFile() != null) {
					result.add(folder.getFile());
				}
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof Folders other) {
			if (childs == null) {
				if (other.childs != null) {
					return false;
				}
			} else {
				if (other.childs == null || other.childs.size() != childs.size()) {
					return false;
				}
				for (int i = 0; i < childs.size(); i++) {
					if (!childs.get(i).equals(other.childs.get(i))) {
						return false;
					}
				}
			}
			return (active == other.active &&
				addToMediaLibrary == other.addToMediaLibrary &&
				((parent == null && other.parent == null) ||
				parent != null && parent.equals(other.parent)) &&
				((name == null && other.name == null) ||
				name != null && name.equals(other.name)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + (this.active ? 1 : 0);
		hash = 83 * hash + (Objects.hashCode(this.name));
		hash = 83 * hash + (Objects.hashCode(this.parent));
		hash = 83 * hash + (Objects.hashCode(this.childs));
		hash = 83 * hash + (this.addToMediaLibrary ? 1 : 0);
		return hash;
	}
}
