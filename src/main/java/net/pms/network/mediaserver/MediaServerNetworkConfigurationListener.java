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
package net.pms.network.mediaserver;

import java.net.InetAddress;
import java.net.NetworkInterface;
import net.pms.network.configuration.NetworkConfigurationListenerInterface;
import net.pms.network.configuration.NetworkInterfaceAssociation;

public class MediaServerNetworkConfigurationListener implements NetworkConfigurationListenerInterface {

	@Override
	public void networkInterfaceAdded(NetworkInterface networkInterface, InetAddress address) {
		//nothing for now
	}

	@Override
	public void networkInterfaceRemoved(NetworkInterface networkInterface, InetAddress address) {
		//nothing for now
	}

	@Override
	public void networkInterfaceUp(NetworkInterfaceAssociation networkInterfaceAssociation) {
		//nothing for now
	}

	@Override
	public void networkInterfaceDown(NetworkInterfaceAssociation networkInterfaceAssociation) {
		//nothing for now
	}

	@Override
	public void networkInterfaceAddressAdded(NetworkInterface networkInterface, InetAddress address) {
		//nothing for now
	}

	@Override
	public void networkInterfaceDefaultAddressChanged(NetworkInterface networkInterface, InetAddress address) {
		//nothing for now
	}

	@Override
	public void networkInterfaceWithAddressAdded() {
		MediaServer.checkNetworkConfiguration();
	}

	@Override
	public void networkInterfaceAddressRemoved(NetworkInterface networkInterface, InetAddress address) {
		//nothing for now
	}
}
