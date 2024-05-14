package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.w3c.dom.NodeList;
import net.pms.store.StoreResource;

public abstract class BaseUpdateObjectHandler implements IUpdateObjectHandler {

	private StoreResource objectResource;
	private NodeList currentTagValue;
	private NodeList newTagValue;

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

	protected void updateTag() throws ContentDirectoryException {
	}

	protected void deleteTag() throws ContentDirectoryException {
	}

	protected void addNewTag() throws ContentDirectoryException {
	}
}
