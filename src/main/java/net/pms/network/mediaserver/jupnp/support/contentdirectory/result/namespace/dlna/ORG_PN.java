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
 * MM pn-param (DLNA.ORG_PN parameter).
 *
 * The pn-value shall identify the DLNA Media Format Profile ID that is
 * applicable for the context of the protocolInfo.
 *
 * The pn-param is reserved for use with contexts where content conforms to a
 * DLNA Media Format Profile. Use of pn-param for content not conformant with a
 * DLNA Media Format Profile is expressly prohibited.
 *
 * @author Surf@ceS
 */
@SuppressWarnings({ "checkstyle:TypeName" })
public class ORG_PN {

	private static final String CI_PARAM_TOKEN = "DLNA.ORG_PN";
	private String profileId;

	@Override
	public String toString() {
		return profileId == null ? "" : CI_PARAM_TOKEN + "=" + profileId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String value) {
		profileId = value;
	}

}
