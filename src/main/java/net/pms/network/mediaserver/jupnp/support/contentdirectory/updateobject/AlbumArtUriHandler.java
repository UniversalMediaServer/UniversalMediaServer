package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import net.pms.store.StoreResource;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.item.WebStream;

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
		String currentValue = getNodeTextValue(getCurrentTagValue(), 0);
		String newValue = getNodeTextValue(getNewTagValue(), 0);

		if (!isModelValueEqual(currentValue)) {
			throw new ContentDirectoryException(702, "UpdateObject() failed because upnp:albumArtURI value listed in " +
				"the CurrentTagValue argument do not match the current state of the ContentDirectory service. " +
				"The specified data is likely out of date.");
		}
		if (newValue == null) {
			throw new ContentDirectoryException(703, "UpdateObject() failed because no upnp:albumArtURI value was supplied. " +
				"Removing the album art is not supported.");
		}
		if (!ThumbnailStore.isUsableThumbnailUri(newValue)) {
			throw new ContentDirectoryException(703, "UpdateObject() failed because \"" + newValue +
				"\" is not an absolute URI.");
		}
		// done in the calling thread : the control point shall learn whether the image could be read.
		String fileName = getObjectResource().getFileName();
		if (ThumbnailStore.updateThumbnailByURI(newValue, fileName, ThumbnailSource.USER) == null) {
			throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, "the album art could not be read from " + newValue);
		}
		if (getObjectResource() instanceof WebStream ws && getObjectResource().getParent() instanceof PlaylistFolder pls) {
			// This entry is a webResource from a Playlist. We can try to update the album art uri
			try {
				pls.updateAlbumArtUriDirective(ws.getUrl(), newValue);
			} catch (Exception e) {
				throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, "the playlist file could not be updated : " + e.getMessage());
			}
		}
	}

	private boolean isModelValueEqual(String currentValue) {
		return true;
	}
}
