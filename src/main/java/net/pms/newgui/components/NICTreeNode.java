/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.newgui.components;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class NICTreeNode extends SearchableMutableTreeNode {
	private static final long serialVersionUID = -8537751660346092874L;
	private final NICTreeNodeType nodeType;
	private final NetworkInterface networkInterface;
	private final InetAddress inetAddress;

	public NICTreeNode(String nodeName) {
		this(nodeName, true);
	}

	public NICTreeNode(String nodeName, boolean allowsChildren) {
		super(nodeName, allowsChildren);
		this.nodeType = NICTreeNodeType.OTHER;
		this.networkInterface = null;
		this.inetAddress = null;
	}

	public NICTreeNode(NetworkInterface networkInterface) {
		this(networkInterface, true);
	}

	public NICTreeNode(NetworkInterface networkInterface, boolean allowsChildren) {
		super(networkInterface.getName(), allowsChildren);
		this.nodeType = NICTreeNodeType.INTERFACE;
		this.networkInterface = networkInterface;
		this.inetAddress = null;
	}

	public NICTreeNode(InetAddress inetAddress) {
		this(inetAddress, true);
	}

	public NICTreeNode(InetAddress inetAddress, boolean allowsChildren) {
		super(inetAddress.getHostAddress(), allowsChildren);
		this.nodeType = NICTreeNodeType.ADDRESS;
		this.inetAddress = inetAddress;
		this.networkInterface = null;
	}

	public NICTreeNodeType getNodeType() {
		return nodeType;
	}

	public boolean isInterface() {
		return nodeType == NICTreeNodeType.INTERFACE;
	}

	public boolean isAddress() {
		return nodeType == NICTreeNodeType.ADDRESS;
	}

	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public enum NICTreeNodeType {
		INTERFACE,
		ADDRESS,
		OTHER
	}
}
