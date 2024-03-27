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

import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;

public class DlnaProtocolInfo extends ProtocolInfo {

	public DlnaProtocolInfo() {
		super(Protocol.ALL, WILDCARD, WILDCARD, WILDCARD);
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public void setContentFormat(String contentFormat) {
		this.contentFormat = contentFormat;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

}
