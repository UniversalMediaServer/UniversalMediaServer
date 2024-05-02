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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PropertyList {

	private final List<Property<?>> properties = new ArrayList<>();

	/**
	 * Replaces the property list with the
	 * specified property list (optional operation).
	 *
	 * @param property property to be stored
	 * @return the PropertyList
	 */
	public PropertyList set(Collection<Property<?>> properties) {
		this.properties.clear();
		return addAll(properties);
	}

	public List<Property<?>> get() {
		return properties;
	}

	public PropertyList addAll(Collection<Property<?>> properties) {
		if (properties != null && !properties.isEmpty()) {
			this.properties.addAll(properties);
		}
		return this;
	}

	public PropertyList addAll(Property<?>[] properties) {
		if (properties != null && properties.length > 0) {
			for (Property property : properties) {
				add(property);
			}
		}
		return this;
	}

	public PropertyList add(Property<?> property) {
		if (property != null) {
			this.properties.add(property);
		}
		return this;
	}

	public PropertyList remove(Class<?> propertyClass) {
		Iterator<Property<?>> it = this.properties.iterator();
		while (it.hasNext()) {
			Property<?> property = it.next();
			if (propertyClass.equals(property.getClass())) {
				it.remove();
			}
		}
		return this;
	}

	/**
	 * Replaces the property element with the
	 * specified element (optional operation).
	 *
	 * @param property property to be stored
	 * @return the PropertyList
	 */
	public PropertyList set(Property<?> property) {
		if (property != null) {
			remove(property.getClass());
			add(property);
		}
		return this;
	}

	public <T> T get(Class<T> propertyClass) {
		for (Property<?> property : this.properties) {
			if (propertyClass.equals(property.getClass())) {
				return (T) property;
			}
		}
		return null;
	}

	public <T> List<T> getAll(Class<T> propertyClass) {
		List<T> list = new ArrayList<>();
		for (Property<?> property : this.properties) {
			if (propertyClass.equals(property.getClass())) {
				list.add((T) property);
			}
		}
		return list;
	}

	public <V> Property<V>[] getPropertiesInstanceOf(Class<?> baseClass) {
		List<Property<V>> list = new ArrayList<>();
		for (Property property : this.properties) {
			if (baseClass.isInstance(property)) {
				list.add(property);
			}
		}
		return list.toArray(Property[]::new);
	}

	public boolean hasProperty(Class<?> propertyClass) {
		for (Property<?> property : this.properties) {
			if (propertyClass.equals(property.getClass())) {
				return true;
			}
		}
		return false;
	}

	public <V> V getValue(Class<? extends Property<V>> propertyClass) {
		for (Property<?> property : this.properties) {
			if (propertyClass.equals(property.getClass())) {
				return (V) property.getValue();
			}
		}
		return null;
	}

	public <V> List<V> getValues(Class<? extends Property<V>> propertyClass) {
		List<V> list = new ArrayList<>();
		for (Property property : getAll(propertyClass)) {
			list.add((V) property.getValue());
		}
		return list;
	}

}
