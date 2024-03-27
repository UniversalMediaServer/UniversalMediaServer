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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc;

import java.net.URI;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.XmlNamespace;

/**
 *
 * @See http://dublincore.org/documents/dces
 */
public class DC {

	public static final String NAMESPACE_URI = "http://purl.org/dc/elements/1.1/";

	@SuppressWarnings({ "checkstyle:InterfaceIsType" })
	public interface NAMESPACE extends XmlNamespace {
	}

	/**
	 * The dc:contributor property indicates the name of a contributor to the
	 * content item.
	 *
	 * It is RECOMMENDED that dc:contributor property includes the name of the
	 * primary content creator or owner (Dublin Core ‘creator’ property).
	 */
	public static class Contributor extends Property<String> implements NAMESPACE {

		public Contributor() {
		}

		public Contributor(String value) {
			super(value, null);
		}
	}

	/**
	 * The dc:creator property indicates an entity that owns the content or is
	 * primarily responsible for creating the content.
	 *
	 * Examples include a person, an organization or a service. Typically, the
	 * name of the creator should be used to indicate the entity.
	 */
	public static class Creator extends Property<String> implements NAMESPACE {

		public Creator() {
		}

		public Creator(String value) {
			super(value, "creator");
		}
	}

	/**
	 * The dc:date property contains the primary date of the content.
	 *
	 * The format MUST be compliant to [ISO 8601] and SHOULD be compliant to
	 * [RFC 3339].
	 */
	public static class Date extends Property<String> implements NAMESPACE {

		public Date() {
		}

		public Date(String value) {
			super(value, "date");
		}
	}

	/**
	 * The dc:description property contains a brief description of the content
	 * item.
	 */
	public static class Description extends Property<String> implements NAMESPACE {

		public Description() {
		}

		public Description(String value) {
			super(value, "description");
		}
	}

	/**
	 * The dc:language property indicates one of the languages used in the
	 * content as defined by RFC 3066.
	 *
	 * for example, “en-US”.
	 */
	public static class Language extends Property<String> implements NAMESPACE {

		public Language() {
		}

		public Language(String value) {
			super(value, "language");
		}
	}

	/**
	 * The dc:publisher property indicates the name of a publisher of the
	 * content.
	 */
	public static class Publisher extends Property<String> implements NAMESPACE {

		public Publisher() {
		}

		public Publisher(String value) {
			super(value, "publisher");
		}
	}

	/**
	 * A related resource.
	 *
	 * See http://dublincore.org/documents/dces. The value MUST be a properly
	 * escaped URI as described in [RFC 2396].
	 */
	public static class Relation extends Property<URI> implements NAMESPACE {

		public Relation() {
		}

		public Relation(URI value) {
			super(value, "relation");
		}
	}

	/**
	 * The upnp:rights property contains some descriptive information about the
	 * legal rights held in or over this resource.
	 */
	public static class Rights extends Property<String> implements NAMESPACE {

		public Rights() {
		}

		public Rights(String value) {
			super(value, "rights");
		}
	}

	/**
	 * The dc:title property is a REQUIRED property and indicates a friendly
	 * name for the object.
	 */
	public static class Title extends Property<String> implements NAMESPACE {

		public Title() {
		}

		public Title(String value) {
			super(value, "title");
		}
	}

}
