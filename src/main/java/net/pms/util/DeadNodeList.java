/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.util;

import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is a {@link NodeList} implementation that intentionally breaks the
 * interface requirement that the list is "live" for improved performance. Do
 * <b>not</b> use this class if a live {@link NodeList} is needed.
 *
 * This class also extends {@link ArrayList} for easier access to {@link Node}s.
 *
 * @author Nadahar
 */
public class DeadNodeList extends ArrayList<Node> implements NodeList {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeadNodeList.class);

	/**
	 * Constructs an empty list with an initial capacity of ten.
	 */
	public DeadNodeList() {
		super();
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the list.
	 * @throws IllegalArgumentException if the specified initial capacity is
	 *             negative.
	 */
	public DeadNodeList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new list based on the specified {@link Object}.
	 *
	 * @param nodeList the {@link NodeList} or {@link Collection} of
	 *            {@link Node}s containing the {@link Node}s to populate the new
	 *            instance.
	 */
	public DeadNodeList(Object nodeList) {
		super();
		if (nodeList instanceof Collection<?>) {
			for (Object object : ((Collection<?>) nodeList)) {
				if (object instanceof Node) {
					add((Node) object);
				} else {
					LOGGER.error(
						"Can't add \"{}\" to DeadNodeList since {} doesn't implement Node - skipping",
						object,
						object.getClass().getSimpleName()
					);
				}
			}
		} else if (nodeList instanceof NodeList || nodeList instanceof Node) {
			Node node;
			if (nodeList instanceof Node) {
				node = (Node) nodeList;
			} else {
				node = ((NodeList) nodeList).item(0);
			}
			if (node == null) {
				return;
			}
			do {
				// Clone node to avoid extreme slowness when evaluating expressions
				add(node.cloneNode(true));
				node = node.getNextSibling();
			} while (node != null);
		} else if (nodeList != null) {
			throw new IllegalArgumentException("nodeList must either be a Collection of Nodes or a NodeList");
		}
	}

	@Override
	public Node item(int index) {
		return get(index);
	}

	@Override
	public int getLength() {
		return size();
	}
}
