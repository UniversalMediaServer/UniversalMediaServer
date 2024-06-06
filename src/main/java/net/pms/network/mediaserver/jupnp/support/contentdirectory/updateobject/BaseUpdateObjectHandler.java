package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.w3c.dom.NodeList;

public abstract class BaseUpdateObjectHandler implements IUpdateObjectHandler {

	private final StoreResource objectResource;
	private final NodeList currentTagValue;
	private final NodeList newTagValue;

	public BaseUpdateObjectHandler(StoreResource objectResource, NodeList currentTagValue, NodeList newTagValue) {
		super();
		this.objectResource = objectResource;
		this.currentTagValue = currentTagValue;
		this.newTagValue = newTagValue;
	}

	public StoreResource getObjectResource() {
		return objectResource;
	}

	public NodeList getCurrentTagValue() {
		return currentTagValue;
	}

	public NodeList getNewTagValue() {
		return newTagValue;
	}

	@Override
	public void handle() throws ContentDirectoryException {
		if (currentTagValue == null) {
			addNewTag();
		} else if (newTagValue == null) {
			deleteTag();
		} else {
			updateTag();
		}
	}

	/**
	 * Returns text content of node at index.
	 *
	 * @param node
	 * @param index
	 * @return TextContent or NULL if Node is empty, blank or not existent.
	 */
	public String getNodeTextValue(NodeList node, int index) {
		try {
			String s = node.item(index).getTextContent();
			if (StringUtils.isAllBlank(s)) {
				return null;
			}
			return s;
		} catch (Exception e) {
			return null;
		}
	}

	protected void updateTag() throws ContentDirectoryException {
	}

	protected void deleteTag() throws ContentDirectoryException {
	}

	protected void addNewTag() throws ContentDirectoryException {
	}
}
