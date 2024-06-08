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

/**
 * Virtual folders allow you to combine real folders into virtual ones and
 * give them custom names.
 *
 * If you add a folder here, it will be scanned and its content added to
 * the Media Store.
 * For each virtual folder you can decide whether you want its content scanned
 * and added to the Media Store.
 */
public class VirtualFolderContent extends SharedContentWithPath {
	protected static final String TYPE = "VirtualFolder";
	private String name;
	private List<SharedContent> childs;
	private boolean addToMediaLibrary;

	public VirtualFolderContent(String name, List<SharedContent> childs) {
		this(null, name, childs, true);
	}

	public VirtualFolderContent(String parent, String name, List<SharedContent> childs) {
		this(parent, name, childs, true);
	}

	public VirtualFolderContent(String parent, String name, List<SharedContent> childs, boolean addToMediaLibrary) {
		setParent(parent);
		this.name = name;
		this.childs = childs;
		this.addToMediaLibrary = addToMediaLibrary;
	}

	public void setName(String value) {
		name = value;
	}

	public String getName() {
		return name;
	}

	public void setChilds(List<SharedContent> value) {
		childs = value;
	}

	public List<SharedContent> getChilds() {
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
			for (SharedContent sharedContent : childs) {
				if (sharedContent instanceof FolderContent folder && folder.getFile() != null) {
					result.add(folder.getFile());
				}
			}
		}
		return result;
	}

	public List<VirtualFolderContent> getVirtualFolders() {
		List<VirtualFolderContent> result = new ArrayList<>();
		if (childs != null) {
			for (SharedContent sharedContent : childs) {
				if (sharedContent instanceof VirtualFolderContent virtualFolder) {
					result.add(virtualFolder);
				}
			}
		}
		return result;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public boolean isExternalContent() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (super.equals(o) && o instanceof VirtualFolderContent other) {
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
			return (addToMediaLibrary == other.addToMediaLibrary &&
				((name == null && other.name == null) ||
				name != null && name.equals(other.name)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 83 * hash + (Objects.hashCode(this.name));
		hash = 83 * hash + (Objects.hashCode(this.childs));
		hash = 83 * hash + (this.addToMediaLibrary ? 1 : 0);
		return hash;
	}

}
