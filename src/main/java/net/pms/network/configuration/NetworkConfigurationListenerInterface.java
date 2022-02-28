/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.configuration;

import java.net.InetAddress;
import java.net.NetworkInterface;

public interface NetworkConfigurationListenerInterface {
	public void networkInterfaceAdded(NetworkInterface networkInterface, InetAddress address);
	public void networkInterfaceRemoved(NetworkInterface networkInterface, InetAddress address);
	public void networkInterfaceUp(NetworkInterfaceAssociation networkInterfaceAssociation);
	public void networkInterfaceDown(NetworkInterfaceAssociation networkInterfaceAssociation);
	public void networkInterfaceAddressAdded(NetworkInterface networkInterface, InetAddress address);
	public void networkInterfaceDefaultAddressChanged(NetworkInterface networkInterface, InetAddress address);
	public void networkInterfaceWithAddressAdded();
	public void networkInterfaceAddressRemoved(NetworkInterface networkInterface, InetAddress address);
}
