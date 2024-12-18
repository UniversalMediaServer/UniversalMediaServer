package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import net.pms.store.StoreResource;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;

/**
 * Updates the Thumbnail AlbumArtURI of a resource.
 */
public class AlbumArtUriHandler extends BaseUpdateObjectHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlbumArtUriHandler.class.getName());

	public AlbumArtUriHandler(StoreResource objectResource, NodeList currentTagValue, NodeList newTagValue) {
		super(objectResource, currentTagValue, newTagValue);
	}

	@Override
	public void handle() throws ContentDirectoryException {
		LOGGER.debug("UpdateObject : AlbumArtURI ... ");
		try {
			String currentValue = getNodeTextValue(getCurrentTagValue(), 0);
			String newValue = getNodeTextValue(getNewTagValue(), 0);

			if (!isModelValueEqual(currentValue)) {
				throw new ContentDirectoryException(702, "UpdateObject() failed because upnp:albumArtURI value listed in " +
					"the CurrentTagValue argument do not match the current state of the ContentDirectory service. " +
					"The specified data is likely out of date.");
			}

			ThumbnailStore.updateThumbnailByURI(newValue, getObjectResource().getFileName(), ThumbnailSource.USER);
		} catch (Exception e) {
			LOGGER.debug("UpdateObject : failed", e);
		}
	}

	private boolean isModelValueEqual(String currentValue) {
		// check URL
		return true;
	}
}
