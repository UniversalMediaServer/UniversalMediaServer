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
 * MM op-param (Operations Parameter – Common guidelines).
 *
 * The op-value is a string composed of two characters: a-val and b-val. The
 * meaning of these values is depending on whether the context is for the HTTP
 * Media Transport or RTP Media Transport.
 *
 * The DLNA.ORG_OP parameter indicates support for transport layer headers
 * responsible that facilitate random access operations on content binaries,
 * under the "Full Random Access Data Availability" model.
 *
 * If the op-param is present and if either a-val or b-val is "1", then the
 * "Full Random Access Data Availability" model shall be the data access model
 * that applies in the context of the protocolInfo value.
 *
 * Specifically, this means that the transport operation (that is indicated by
 * the a-val or b-val) shall be supported for the entire content binary.
 *
 * If the flags-param token is included in the 4th field, then the
 * s0-increasing, lop-npt, and lop-bytes bits of the primary-flags token shall
 * be set to false.
 *
 * If the associated HTTP Server Endpoint always returns 406 (Not Acceptable) in
 * response to requests that use either HTTP header for the context of the
 * protocolInfo, then the 4th field shall omit the op-param.
 *
 * If the Content Source assigns "0" to both a-val and b-val of the op-param,
 * then the op-param shall be omitted from the 4th field.
 *
 * @author Surf@ceS
 */
@SuppressWarnings({ "checkstyle:TypeName" })
public class ORG_OP {

	private static final String CI_PARAM_DELIM = ";";
	private static final String CI_PARAM_TOKEN = "DLNA.ORG_OP";
	private boolean a;
	private boolean b;

	@Override
	public String toString() {
		return CI_PARAM_TOKEN + "=" + (a ? "1" : "0") + (b ? "1" : "0");
	}

	public String getParam() {
		return a || b ? CI_PARAM_DELIM + toString() : "";
	}

	public boolean isHttpTimeSeekRangeHeaderAccepted() {
		return a;
	}

	/**
	 * For HTTP : indicates support of the TimeSeekRange.dlna.org HTTP header
	 * for the context of the protocolInfo under the "Full Random Access Data
	 * Availability" model.
	 *
	 * @param value
	 */
	public void setHttpTimeSeekRangeHeaderAccepted(boolean value) {
		a = value;
	}

	/**
	 * For HTTP : indicates support of the Range HTTP header for the context of
	 * the protocolInfo under the "Full Random Access Data Availability" model.
	 *
	 * @param value
	 */
	public void setHttpRangeHeaderAccepted(boolean value) {
		b = value;
	}

	/**
	 * For RTP Media Transport : indicates support of the Range header for the
	 * context of the protocolInfo under the "Full Random Access Data
	 * Availability" model.
	 *
	 * the b-val shall have a value of "0".
	 *
	 * @param value
	 */
	public void setRtpRangeHeaderAccepted(boolean value) {
		a = value;
		b = false;
	}

}
