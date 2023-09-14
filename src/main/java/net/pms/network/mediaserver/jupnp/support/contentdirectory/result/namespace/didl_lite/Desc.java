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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite;

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;

/**
 * The desc property can appear as a <desc> element anywhere in a valid
 * DIDL-Lite XML Document where an element can appear.
 *
 * The desc property is used to associate blocks of other XML-based metadata
 * with a given ContentDirectory service object. Examples of other XML-based
 * metadata include DIG35, MPEG7, RDF, XrML, etc. The desc property could also
 * be used to contain vendor-specific content ratings information, digitally
 * signed rights descriptions, etc.
 */
public class Desc extends Property<Void> implements DIDL_LITE.NAMESPACE {

	/**
	 * The required @nameSpace property identifies the namespace of the
	 * metadata, contained in the associated independent desc property.
	 *
	 * Since the dependent @nameSpace property can only appear once for its
	 * associated independent desc property, the contents of each desc property
	 * can be associated with only one namespace.
	 */
	public static class NameSpace extends Property<String> implements DIDL_LITE.NAMESPACE {

		public NameSpace() {
		}

		public NameSpace(String value) {
			super(value, "nameSpace");
		}
	}

	public static class Id extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Id() {
		}

		public Id(String value) {
			super(value, "id");
		}
	}

	public static class Type extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Type() {
		}

		public Type(String value) {
			super(value, "type");
		}
	}

	public static class Metadata extends Property<String> implements DIDL_LITE.NAMESPACE {

		public Metadata() {
		}

		public Metadata(String qualifiedName, String value) {
			super(value, qualifiedName);
		}
	}

	public Desc() {
		super("desc");
	}

	public Desc(String nameSpace) {
		this();
		setNameSpace(nameSpace);
	}

	public String getNameSpace() {
		return dependentProperties.getValue(Desc.NameSpace.class);
	}

	public final void setNameSpace(String nameSpace) {
		dependentProperties.set(new Desc.NameSpace(nameSpace));
	}

	public String getId() {
		return dependentProperties.getValue(Desc.Id.class);
	}

	public final void setId(String id) {
		dependentProperties.set(new Desc.Id(id));
	}

	public String getType() {
		return dependentProperties.getValue(Desc.Type.class);
	}

	public final void setType(String type) {
		dependentProperties.set(new Desc.Type(type));
	}

	public Desc addMetadata(Metadata metadata) {
		properties.add(metadata);
		return this;
	}

	public Desc addMetadata(String qualifiedName, String value) {
		properties.add(new Metadata(qualifiedName, value));
		return this;
	}

}
