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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dlna;

/**
 * MM ci-param (conversion indicator flag).
 *
 * The ci-param is a conversion indicator parameter.
 *
 * If the context of the protocolInfo involves a content binary that is
 * converted from a different content binary, then ci-value is "1". Otherwise,
 * the ci-value is "0".
 *
 * Conversion include transcoding, system layer conversion, timestamps (e.g.
 * TTS, PCR, PTS), scaling, and decoding.
 *
 * The ci-param is not mandatory, but its use is strongly encouraged, especially
 * for converted content.
 *
 * If a protocolInfo value omits the ci-param, then UPnP MediaServer control
 * points shall infer that the associated content is not converted content.
 *
 * @author Surf@ceS
 */
@SuppressWarnings({ "checkstyle:TypeName" })
public class ORG_CI {

	private static final String CI_PARAM_DELIM = ";";
	private static final String CI_PARAM_TOKEN = "DLNA.ORG_CI";
	private Boolean ci;

	@Override
	public String toString() {
		return CI_PARAM_TOKEN + "=" + (Boolean.TRUE.equals(ci) ? "1" : "0");
	}

	public String getParam() {
		return ci == null ? "" : CI_PARAM_DELIM + toString();
	}

	public boolean isConverted() {
		return Boolean.TRUE.equals(ci);
	}

	public void setConverted(Boolean value) {
		ci = value;
	}

}
