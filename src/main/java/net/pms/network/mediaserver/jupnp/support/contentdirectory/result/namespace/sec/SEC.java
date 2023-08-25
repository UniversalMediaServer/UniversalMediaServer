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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.sec;

import java.net.URI;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.XmlNamespace;

public class SEC {

	public static final String NAMESPACE_URI = "http://www.sec.co.kr/";

	@SuppressWarnings({ "checkstyle:InterfaceIsType" })
	public interface NAMESPACE extends XmlNamespace {
	}

	public static class CaptionInfoEx extends Property<URI> implements NAMESPACE {

		public CaptionInfoEx() {
			this(null);
		}

		public CaptionInfoEx(URI value) {
			super(value, "CaptionInfoEx", NAMESPACE_URI);
		}

		public CaptionInfoEx(URI value, String type) {
			super(value, "CaptionInfoEx", NAMESPACE_URI);
			setType(type);
		}

		public String getType() {
			return dependentProperties.getValue(Type.class);
		}

		public final CaptionInfoEx setType(String value) {
			dependentProperties.set(new Type(value));
			return this;
		}
	}

	public static class CaptionInfo extends Property<URI> implements NAMESPACE {

		public CaptionInfo() {
			this(null);
		}

		public CaptionInfo(URI value) {
			super(value, "CaptionInfo", NAMESPACE_URI);
		}

		public CaptionInfo(URI value, String type) {
			super(value, "CaptionInfo", NAMESPACE_URI);
			setType(type);
		}

		public String getType() {
			return dependentProperties.getValue(Type.class);
		}

		public final CaptionInfo setType(String value) {
			dependentProperties.set(new Type(value));
			return this;
		}
	}

	public static class DcmInfo extends Property<String> implements NAMESPACE {

		public DcmInfo() {
			this(null);
		}

		public DcmInfo(String value) {
			super(value, "dcmInfo", NAMESPACE_URI);
		}
	}

	public static class Preference extends Property<String> implements NAMESPACE {

		public Preference() {
			this(null);
		}

		public Preference(String value) {
			super(value, "preference", NAMESPACE_URI);
		}
	}

	public static class ModifiationDate extends Property<String> implements NAMESPACE {

		public ModifiationDate() {
			this(null);
		}

		public ModifiationDate(String value) {
			super(value, "modifiationDate", NAMESPACE_URI);
		}
	}

	public static class Type extends Property<String> implements NAMESPACE {

		public Type() {
			this(null);
		}

		public Type(String value) {
			super(value, "type", NAMESPACE_URI);
		}
	}

}
