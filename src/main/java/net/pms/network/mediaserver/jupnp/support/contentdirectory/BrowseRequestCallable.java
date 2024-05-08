package net.pms.network.mediaserver.jupnp.support.contentdirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.utils.StoreResourceSorter;

public class BrowseRequestCallable implements Callable<BrowseRequestCallResult> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrowseRequestCallable.class);

	private String objectID;
	private BrowseFlag browseFlag;
	private String filter;
	private long startingIndex;
	private long requestedCount;
	private SortCriterion[] sortCriteria;
	private Renderer renderer;

	public BrowseRequestCallable(String objectID,
		BrowseFlag browseFlag,
		String filter,
		long startingIndex,
		long requestedCount,
		SortCriterion[] sortCriteria,
		Renderer renderer) {
		this.objectID = objectID;
		this.browseFlag = browseFlag;
		this.filter = filter;
		this.startingIndex = startingIndex;
		this.requestedCount = requestedCount;
		this.sortCriteria = sortCriteria;
		this.renderer = renderer;
	}

	@Override
	public BrowseRequestCallResult call() throws Exception {
		BrowseRequestCallResult result = new BrowseRequestCallResult();

		boolean browseDirectChildren = browseFlag == BrowseFlag.DIRECT_CHILDREN;

		List<StoreResource> resources = renderer.getMediaStore().getResources(
				objectID,
				browseDirectChildren
		);

		List<StoreResource> resultResources = new ArrayList<>();
		long resourcesCount = 0;
		long badResourceCount = 0;

		//keep only compatible resources
		if (resources != null) {
			resourcesCount = resources.size();
			for (StoreResource resource : resources) {
				if (resource instanceof PlaylistFolder playlistFolder) {
					File f = new File(resource.getFileName());
					if (resource.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (resource instanceof StoreContainer container) {
					resultResources.add(container);
				} else if (resource instanceof StoreItem item && (item.isCompatible() &&
						(item.getEngine() == null || item.getEngine().isEngineCompatible(renderer)) ||
						// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
						// all possible combination not only those supported by renderer because the renderer setting could be wrong.
						resources.get(0).isInsideTranscodeFolder())) {
					resultResources.add(item);
				} else {
					badResourceCount++;
				}
			}
		}

		//sort
		StoreResourceSorter.sortResources(resultResources, sortCriteria);

		long totalMatches;
		if (browseDirectChildren) {
			StoreContainer parentFolder;
			if (resources != null && resourcesCount > 0) {
				parentFolder = resources.get(0).getParent();
			} else {
				StoreResource resource = renderer.getMediaStore().getResource(objectID);
				if (resource instanceof StoreContainer storeContainer) {
					parentFolder = storeContainer;
				} else {
					if (resource instanceof StoreItem) {
						LOGGER.debug("Trying to browse direct children on a store item for objectID '{}' !", objectID);
					} else {
						LOGGER.debug("Trying to browse direct children on a null object for objectID '{}' !", objectID);
					}
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
				}
			}
			if (parentFolder != null) {
				totalMatches = parentFolder.childrenCount() - badResourceCount;
			} else {
				totalMatches = resourcesCount - badResourceCount;
			}
		} else {
			// From upnp spec: If BrowseMetadata is specified in the BrowseFlags then TotalMatches = 1
			totalMatches = 1;
		}

		//handle startingIndex and requestedCount
		int fromIndex = (int) startingIndex;
		int toIndex;
		if (requestedCount == 0) {
			toIndex = resultResources.size();
		} else {
			toIndex = Math.min(fromIndex + (int) requestedCount, resultResources.size());
		}
		long count = (long) toIndex - fromIndex;
		if (count < 0) {
			LOGGER.debug("requested objects out of range.");
			fromIndex = 0;
			toIndex = 0;
			count = 0;
		}

		// fill up result struct
		result.count = count;
		result.fromIndex = fromIndex;
		result.resultList = resultResources;
		result.toIndex = toIndex;
		result.totalMatches = totalMatches;
		return result;
	}
}
