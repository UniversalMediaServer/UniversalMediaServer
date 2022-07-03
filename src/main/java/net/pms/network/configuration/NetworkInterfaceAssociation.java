/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
import java.net.SocketException;

public class NetworkInterfaceAssociation {
	String parentName;
	InetAddress addr;
	NetworkInterface iface;
	boolean wasUp;

	public NetworkInterfaceAssociation(InetAddress addr, NetworkInterface iface, String parentName) {
		super();
		this.addr = addr;
		this.iface = iface;
		this.parentName = parentName;
		this.wasUp = isUp();
	}

	/**
	 * @return the addr
	 */
	public InetAddress getAddr() {
		return addr;
	}

	/**
	 * @return the iface
	 */
	public NetworkInterface getIface() {
		return iface;
	}

	/**
	 * Returns the name of the parent of the interface association.
	 *
	 * @return The name of the parent.
	 */
	public String getParentName() {
		return parentName;
	}

	/**
	 * Returns the name of the interface association.
	 *
	 * @return The name.
	 */
	public String getShortName() {
		return iface.getName();
	}

	/**
	 * Returns the display name of the interface association
	 * with IP address if exists.
	 *
	 * @return The name.
	 */
	public String getDisplayNameWithAddress() {
		String displayName = iface.getDisplayName();

		if (displayName != null) {
			displayName = displayName.trim();
		} else {
			displayName = iface.getName();
		}

		if (addr != null) {
			displayName += " (" + addr.getHostAddress() + ")";
		}

		return displayName;
	}

	/**
	 * Returns the display name of the interface association.
	 *
	 * @return The name.
	 */
	public String getDisplayName() {
		String displayName = iface.getDisplayName();

		if (displayName != null) {
			displayName = displayName.trim();
		} else {
			displayName = iface.getName();
		}

		return displayName;
	}

	public final boolean wasUp() {
		return wasUp;
	}

	public final boolean statusChanged() {
		return wasUp != isUp();
	}

	public final void updateStatus() {
		wasUp = isUp();
	}

	public final boolean isUp() {
		try {
			return iface.isUp();
		} catch (SocketException ex) {
			return false;
		}
	}

	@Override
	public String toString() {
		return "InterfaceAssociation(addr=" + addr + ", iface=" + iface + ", parent=" + parentName + ')';
	}
}

