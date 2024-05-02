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

import org.w3c.dom.Element;

/**
 * A property in the ContentDirectory service represents a characteristic of an
 * object.
 *
 * @see Properties are distinguished by their names. The ContentDirectory
 * service defines two kinds of properties – independent and dependent. Each
 * independent property has zero or more dependent properties associated with
 * it.
 */
public class Property<V> {

	private V value;
	private final String qualifiedName;
	private final String namespaceURI;
	protected final PropertyList properties = new PropertyList();
	protected final PropertyList dependentProperties = new PropertyList();

	protected Property() {
		this(null, null);
	}

	protected Property(String qualifiedName) {
		this(null, qualifiedName, null);
	}

	protected Property(V value, String qualifiedName) {
		this(value, qualifiedName, null);
	}

	protected Property(V value, String qualifiedName, String namespaceURI) {
		this.value = value;
		this.qualifiedName = qualifiedName;
		this.namespaceURI = namespaceURI;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	/**
     * @return the qualifiedName
     */
	public String getQualifiedName() {
		return qualifiedName;
	}

	/**
	 * @return the namespaceURI
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	public void setOnElement(Element element) {
		element.setTextContent(toString());
		for (Property<?> attr : dependentProperties.get()) {
			element.setAttributeNS(
					attr.getNamespaceURI(),
					attr.getQualifiedName(),
					attr.toString());
		}
	}

	public PropertyList getDependentProperties() {
		return dependentProperties;
	}

	public PropertyList getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		if (value == null) {
			return "";
		}
		if (value instanceof Boolean) {
			return Boolean.TRUE.equals(value) ? "1" : "0";
		}
		return getValue().toString();
	}

}
