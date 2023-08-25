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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class SharedContent implements Serializable {
	private boolean active = true;
	private List<Integer> groups = new ArrayList<>();

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean value) {
		active = value;
	}

	public List<Integer> getGroups() {
		return groups;
	}

	public void setGroups(List<Integer> value) {
		groups = value;
	}

	public void clearGroups() {
		groups.clear();
	}

	public void addGroup(int value) {
		if (!groups.contains(value)) {
			groups.add(value);
		}
	}

	public void removeGroup(int value) {
		if (isGroupRestricted()) {
			groups.remove(value);
		}
	}

	public boolean isGroupAllowed(int value) {
		return !isGroupRestricted() || value == Integer.MAX_VALUE || groups.contains(value);
	}

	public boolean isGroupRestricted() {
		return groups != null && !groups.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof SharedContent other && active == other.active) {
			if (groups == null) {
				if (other.groups != null) {
					return false;
				}
			} else {
				if (other.groups == null || other.groups.size() != groups.size()) {
					return false;
				}
				for (int group : groups) {
					if (!other.groups.contains(group)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + (this.active ? 1 : 0);
		hash = 83 * hash + (Objects.hashCode(this.groups));
		return hash;
	}

	public abstract String getType();

	public abstract boolean isExternalContent();
}
