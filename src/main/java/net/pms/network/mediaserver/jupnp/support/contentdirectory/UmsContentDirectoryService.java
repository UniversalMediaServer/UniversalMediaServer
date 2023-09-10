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
package net.pms.network.mediaserver.jupnp.support.contentdirectory;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableMetadata;
import net.pms.dlna.DidlHelper;
import net.pms.library.LibraryContainer;
import net.pms.library.LibraryItem;
import net.pms.library.LibraryResource;
import net.pms.library.container.MediaLibrary;
import net.pms.library.container.PlaylistFolder;
import net.pms.media.MediaStatusStore;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.jupnp.model.meta.UmsRemoteClientInfo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.LibraryResourceHelper;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Result;
import net.pms.renderers.Renderer;
import net.pms.util.UMSUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVString;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UpnpService(
		serviceId = @UpnpServiceId("ContentDirectory"),
		serviceType = @UpnpServiceType(value = "ContentDirectory", version = 1)
)

@UpnpStateVariables({
	@UpnpStateVariable(
			name = "A_ARG_TYPE_ObjectID",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_Result",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_SearchCriteria",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_BrowseFlag",
			sendEvents = false,
			datatype = "string",
			allowedValuesEnum = BrowseFlag.class),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_Filter",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_SortCriteria",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_Index",
			sendEvents = false,
			datatype = "ui4"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_Count",
			sendEvents = false,
			datatype = "ui4"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_UpdateID",
			sendEvents = false,
			datatype = "ui4"),
	@UpnpStateVariable(
			name = "A_ARG_Type_TransferID",
			sendEvents = false,
			datatype = "uri"),
	@UpnpStateVariable(
			name = "A_ARG_Type_TransferStatus",
			sendEvents = false,
			datatype = "string",
			allowedValuesEnum = TransferStatus.class),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_TransferLength",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_TransferTotal",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_TagValueList",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_URI",
			sendEvents = false,
			datatype = "uri"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_PosSecond",
			sendEvents = false,
			datatype = "ui4"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_CategoryType",
			sendEvents = false,
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_RID",
			sendEvents = false,
			datatype = "string")
})
public class UmsContentDirectoryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsContentDirectoryService.class);
	private static final List<String> CAPS_SEARCH = List.of();
	private static final List<String> CAPS_SORT = List.of("upnp:class", "dc:title", "dc:creator", "upnp:artist", "upnp:album", "upnp:genre");
	private static final String CRLF = "\r\n";
	private static final String METADATA_TABLE_KEY_SYSTEMUPDATEID = "SystemUpdateID";
	private static final ReentrantReadWriteLock LOCK_SYSTEM_UPDATE_ID = new ReentrantReadWriteLock();
	private static UnsignedIntegerFourBytes dbSystemUpdateID;

	private final Timer systemUpdateIdTimer = new Timer("jupnp-contentdirectory-service");
	private final TimerTask systemUpdateIdTask;

	@UpnpStateVariable(sendEvents = false)
	private final CSV<String> searchCapabilities = new CSVString();

	@UpnpStateVariable(sendEvents = false)
	private final CSV<String> sortCapabilities = new CSVString();

	@UpnpStateVariable(
			sendEvents = true,
			defaultValue = "0",
			datatype = "ui4",
			eventMaximumRateMilliseconds = 200
	)
	private UnsignedIntegerFourBytes systemUpdateID;

	protected final PropertyChangeSupport propertyChangeSupport;

	public UmsContentDirectoryService() {
		this.systemUpdateIdTask = new TimerTask() {
			@Override
			public void run() {
				systemUpdateIdChanged();
			}
		};
		this.searchCapabilities.addAll(CAPS_SEARCH);
		this.sortCapabilities.addAll(CAPS_SORT);
		/**
		 * Bump the SystemUpdateID state variable because now we will have
		 * different resource IDs than last time UMS ran. It also populates our
		 * in-memory value with the database value if the database is enabled.
		 */
		bumpSystemUpdateId();
		systemUpdateID = new UnsignedIntegerFourBytes(getDbSystemUpdateId().getValue());
		propertyChangeSupport = new PropertyChangeSupport(this);
		systemUpdateIdTimer.schedule(systemUpdateIdTask, 0, 200);
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "SearchCaps"))
	public CSV<String> getSearchCapabilities() {
		return searchCapabilities;
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "SortCaps"))
	public CSV<String> getSortCapabilities() {
		return sortCapabilities;
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "Id"))
	public synchronized UnsignedIntegerFourBytes getSystemUpdateID() {
		return getDbSystemUpdateId();
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}

	private void systemUpdateIdChanged() {
		long oldValue = systemUpdateID.getValue();
		long newValue = getDbSystemUpdateId().getValue();
		if (oldValue != newValue) {
			getPropertyChangeSupport().firePropertyChange(
					"SystemUpdateID",
					oldValue,
					newValue
			);
			systemUpdateID = new UnsignedIntegerFourBytes(newValue);
			storeDbSystemUpdateId();
			LOGGER.trace("Send event \"SystemUpdateID\" update from {} to {}", oldValue, newValue);
		}
	}

	/**
	 * This required action enables the caller to incrementally browse the
	 * native hierarchy of the ContentDirectory service objects exposed by the
	 * ContentDirectory service, including information listing the classes of
	 * objects available in any particular object container.
	 */
	@UpnpAction(out = {
		@UpnpOutputArgument(name = "Result",
				stateVariable = "A_ARG_TYPE_Result",
				getterName = "getResult"),
		@UpnpOutputArgument(name = "NumberReturned",
				stateVariable = "A_ARG_TYPE_Count",
				getterName = "getCount"),
		@UpnpOutputArgument(name = "TotalMatches",
				stateVariable = "A_ARG_TYPE_Count",
				getterName = "getTotalMatches"),
		@UpnpOutputArgument(name = "UpdateID",
				stateVariable = "A_ARG_TYPE_UpdateID",
				getterName = "getContainerUpdateID")
	})
	public BrowseResult browse(
			@UpnpInputArgument(name = "ObjectID", aliases = "ContainerID") String objectId,
			@UpnpInputArgument(name = "BrowseFlag") String browseFlag,
			@UpnpInputArgument(name = "Filter") String filter,
			@UpnpInputArgument(name = "StartingIndex", stateVariable = "A_ARG_TYPE_Index") UnsignedIntegerFourBytes firstResult,
			@UpnpInputArgument(name = "RequestedCount", stateVariable = "A_ARG_TYPE_Count") UnsignedIntegerFourBytes maxResults,
			@UpnpInputArgument(name = "SortCriteria") String orderBy,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {

		SortCriterion[] orderByCriteria;
		try {
			orderByCriteria = SortCriterion.valueOf(orderBy);
		} catch (Exception ex) {
			throw new ContentDirectoryException(ContentDirectoryErrorCode.UNSUPPORTED_SORT_CRITERIA, ex.toString());
		}

		try {
			return browse(
					objectId,
					BrowseFlag.valueOrNullOf(browseFlag),
					filter,
					firstResult.getValue(), maxResults.getValue(),
					orderByCriteria,
					remoteClientInfo
			);
		} catch (ContentDirectoryException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, ex.toString());
		}
	}

	@UpnpAction(out = {
		@UpnpOutputArgument(name = "Result",
				stateVariable = "A_ARG_TYPE_Result",
				getterName = "getResult"),
		@UpnpOutputArgument(name = "NumberReturned",
				stateVariable = "A_ARG_TYPE_Count",
				getterName = "getCount"),
		@UpnpOutputArgument(name = "TotalMatches",
				stateVariable = "A_ARG_TYPE_Count",
				getterName = "getTotalMatches"),
		@UpnpOutputArgument(name = "UpdateID",
				stateVariable = "A_ARG_TYPE_UpdateID",
				getterName = "getContainerUpdateID")
	})
	public BrowseResult search(
			@UpnpInputArgument(name = "ContainerID", stateVariable = "A_ARG_TYPE_ObjectID") String containerId,
			@UpnpInputArgument(name = "SearchCriteria") String searchCriteria,
			@UpnpInputArgument(name = "Filter") String filter,
			@UpnpInputArgument(name = "StartingIndex", stateVariable = "A_ARG_TYPE_Index") UnsignedIntegerFourBytes startingIndex,
			@UpnpInputArgument(name = "RequestedCount", stateVariable = "A_ARG_TYPE_Count") UnsignedIntegerFourBytes requestedCount,
			@UpnpInputArgument(name = "SortCriteria") String orderBy,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {

		SortCriterion[] sortCriteria;
		try {
			sortCriteria = SortCriterion.valueOf(orderBy);
		} catch (Exception ex) {
			throw new ContentDirectoryException(ContentDirectoryErrorCode.UNSUPPORTED_SORT_CRITERIA, ex.toString());
		}

		try {
			return search(
					containerId,
					searchCriteria,
					filter,
					startingIndex.getValue(),
					requestedCount.getValue(),
					sortCriteria,
					remoteClientInfo
			);
		} catch (ContentDirectoryException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, ex.toString());
		}
	}

	@UpnpAction(name = "X_SetBookmark")
	public String samsungSetBookmark(
			@UpnpInputArgument(name = "ObjectID") String objectID,
			@UpnpInputArgument(name = "PosSecond") UnsignedIntegerFourBytes posSecond,
			@UpnpInputArgument(name = "CategoryType") String categoryType,
			@UpnpInputArgument(name = "RID") String rId,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		try {
			return samsungSetBookmark(
					objectID,
					posSecond.getValue(),
					categoryType,
					rId,
					remoteClientInfo
			);
		} catch (ContentDirectoryException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, ex.toString());
		}
	}

	@UpnpAction(name = "X_GetFeatureList")
	public String samsungGetFeatureList(
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		try {
			return samsungGetFeaturesList(
					remoteClientInfo
			);
		} catch (ContentDirectoryException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, ex.toString());
		}
	}

	private BrowseResult browse(
			String objectID,
			BrowseFlag browseFlag,
			String filter,
			long startingIndex,
			long requestedCount,
			SortCriterion[] sortCriteria,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		if (renderer == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Unrecognized media renderer");
			}
			return null;
		}
		if (!renderer.isAllowed()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
			}
			return null;
		}

		boolean browseDirectChildren = browseFlag == BrowseFlag.DIRECT_CHILDREN;

		List<LibraryResource> resources = renderer.getRootFolder().getLibraryResources(
				objectID,
				browseDirectChildren,
				(int) startingIndex,
				(int) requestedCount,
				null
		);

		List<LibraryResource> resultResources = new ArrayList<>();
		long resourcesCount = 0;
		long badResourceCount = 0;

		if (resources != null) {
			resourcesCount = resources.size();
			for (LibraryResource resource : resources) {
				if (resource instanceof PlaylistFolder playlistFolder) {
					File f = new File(resource.getFileName());
					if (resource.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (resource instanceof LibraryContainer container) {
					resultResources.add(container);
				} else if (resource instanceof LibraryItem item && (item.isCompatible() &&
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

		long count = resourcesCount - badResourceCount;
		long totalMatches;
		if (browseDirectChildren && renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, resources are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular resources, plus the #transcode folder, maybe the #imdb one, also resources can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			totalMatches = startingIndex + requestedCount + 1L; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (resourcesCount - badResourceCount <= 0) {
				totalMatches = startingIndex;
			}
		} else if (browseDirectChildren) {
			LibraryContainer parentFolder;
			if (resources != null && resourcesCount > 0) {
				parentFolder = resources.get(0).getParent();
			} else {
				LibraryResource resource = renderer.getRootFolder().getLibraryResource(objectID);
				if (resource instanceof LibraryContainer libraryContainer) {
					parentFolder = libraryContainer;
				} else {
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

		long containerUpdateID = getDbSystemUpdateId().getValue();
		String result = DidlHelper.getDidlResults(resultResources);
		return new BrowseResult(result, count, totalMatches, containerUpdateID);
	}

	private BrowseResult search(
			String containerId,
			String searchCriteria,
			String filter,
			long startingIndex,
			long requestedCount,
			SortCriterion[] orderBy,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		if (renderer != null && !renderer.isAllowed()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
			}
			return null;
		}

		try {
			SearchRequestHandler handler = new SearchRequestHandler();
			return handler.createSearchResponse(
					containerId,
					searchCriteria,
					filter,
					startingIndex,
					requestedCount,
					orderBy,
					renderer
			);
		} catch (Exception e) {
			LOGGER.trace("error transforming searchCriteria to SQL. Fallback to content browsing ...", e);
			return searchToBrowse(
					containerId,
					searchCriteria,
					filter,
					startingIndex,
					requestedCount,
					orderBy,
					renderer
			);
		}
	}

	private BrowseResult searchToBrowse(
			String containerId,
			String searchCriteria,
			String filter,
			long startingIndex,
			long requestedCount,
			SortCriterion[] orderBy,
			Renderer renderer
	) throws ContentDirectoryException {
		boolean xbox360 = renderer.isXbox360();
		String xboxId = null;
		if (containerId == null) {
			containerId = "0";
		} else if (xbox360 && !containerId.contains("$")) {
			xboxId = containerId;
			containerId = "0";
		}

		// Xbox 360 virtual containers ... d'oh!
		if (xbox360 && PMS.getConfiguration().getUseCache() && renderer.getRootFolder().getLibrary().isEnabled() && xboxId != null) {
			searchCriteria = null;
			MediaLibrary library = renderer.getRootFolder().getLibrary();
			if (xboxId.equals("7") && library.getAlbumFolder() != null) {
				containerId = library.getAlbumFolder().getResourceId();
			} else if (xboxId.equals("6") && library.getArtistFolder() != null) {
				containerId = library.getArtistFolder().getResourceId();
			} else if (xboxId.equals("5") && library.getGenreFolder() != null) {
				containerId = library.getGenreFolder().getResourceId();
			} else if (xboxId.equals("F") && library.getPlaylistFolder() != null) {
				containerId = library.getPlaylistFolder().getResourceId();
			} else if (xboxId.equals("4") && library.getAllFolder() != null) {
				containerId = library.getAllFolder().getResourceId();
			} else if (xboxId.equals("1")) {
				String artist = getEnclosingValue(searchCriteria, "upnp:artist = &quot;", "&quot;)");
				if (artist != null) {
					containerId = library.getArtistFolder().getResourceId();
					searchCriteria = artist;
				}
			}
		}

		List<LibraryResource> resources = renderer.getRootFolder().getLibraryResources(
				containerId,
				true,
				(int) startingIndex,
				(int) requestedCount,
				searchCriteria
		);

		List<LibraryResource> resultResources = new ArrayList<>();
		long resourceCount = 0;
		long badResourceCount = 0;

		if (resources != null) {
			if (searchCriteria != null) {
				UMSUtils.filterResourcesByName(resources, searchCriteria, false, false);
				if (xbox360 && !resources.isEmpty() && resources.get(0) instanceof LibraryContainer libraryContainer) {
					resources = libraryContainer.getChildren();
				}
			}
			resourceCount = resources.size();
			for (LibraryResource resource : resources) {
				if (resource instanceof PlaylistFolder playlistFolder) {
					File f = new File(resource.getFileName());
					if (resource.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (xbox360 && xboxId != null && resource != null) {
					resource.setFakeParentId(xboxId);
				}

				if (resource instanceof LibraryContainer container) {
					resultResources.add(container);
				} else if (resource instanceof LibraryItem item && (item.isCompatible() &&
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

		long count = resourceCount - badResourceCount;
		long totalMatches;
		if (renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, resources are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular resources, plus the #transcode folder, maybe the #imdb one, also resources can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			totalMatches = startingIndex + requestedCount + 1; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (resourceCount - badResourceCount <= 0) {
				totalMatches = startingIndex;
			}
		} else {
			LibraryContainer parentFolder;
			if (resources != null && resourceCount > 0) {
				parentFolder = resources.get(0).getParent();
			} else {
				LibraryResource resource = renderer.getRootFolder().getLibraryResource(containerId);
				if (resource instanceof LibraryContainer libraryContainer) {
					parentFolder = libraryContainer;
				} else {
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
				}
			}
			if (parentFolder != null) {
				totalMatches = parentFolder.childrenCount() - badResourceCount;
			} else {
				totalMatches = resourceCount - badResourceCount;
			}
		}

		long containerUpdateID = getDbSystemUpdateId().getValue();
		String result = DidlHelper.getDidlResults(resultResources);
		return new BrowseResult(result, count, totalMatches, containerUpdateID);
	}

	private static String getEnclosingValue(String content, String leftTag, String rightTag) {
		String result = null;
		int leftTagPos = content.indexOf(leftTag);
		int leftTagStop = content.indexOf('>', leftTagPos + 1);
		int rightTagPos = content.indexOf(rightTag, leftTagStop + 1);

		if (leftTagPos > -1 && rightTagPos > leftTagPos) {
			result = content.substring(leftTagStop + 1, rightTagPos);
		}

		return result;
	}

	private static String samsungSetBookmark(
			String objectID,
			long posSecond,
			String categoryType,
			String rId,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		if (renderer == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Unrecognized media renderer");
			}
			return null;
		}
		if (!renderer.isAllowed()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
			}
			return null;
		}

		if (posSecond == 0) {
			// Sometimes when Samsung device is starting to play the video
			// it sends X_SetBookmark message immediatelly with the position=0.
			// No need to update database in such case.
			LOGGER.debug("Skipping \"set bookmark\". Position=0");
		} else {
			try {
				LibraryResource resource = renderer.getRootFolder().getLibraryResource(objectID);
				File file = new File(resource.getFileName());
				String path = file.getCanonicalPath();
				MediaStatusStore.setBookmark(path, renderer.getAccountUserId(), (int) posSecond);
			} catch (IOException e) {
				LOGGER.error("Cannot set bookmark", e);
			}
		}
		return "";
	}

	private static String samsungGetFeaturesList(
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		if (renderer == null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Unrecognized media renderer");
			}
			return null;
		}
		if (!renderer.isAllowed()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
			}
			return null;
		}

		StringBuilder features = new StringBuilder();
		String rootFolderId = renderer.getRootFolder().getResourceId();
		features.append("<Features xmlns=\"urn:schemas-upnp-org:av:avs\"");
		features.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		features.append(" xsi:schemaLocation=\"urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd\">").append(CRLF);
		features.append("<Feature name=\"samsung.com_BASICVIEW\" version=\"1\">").append(CRLF);
		// we may use here different container IDs in the future
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.audioItem\"/>").append(CRLF);
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.videoItem\"/>").append(CRLF);
		features.append("<container id=\"").append(rootFolderId).append("\" type=\"object.item.imageItem\"/>").append(CRLF);
		features.append("</Feature>").append(CRLF);
		features.append("</Features>").append(CRLF);

		StringBuilder response = new StringBuilder();
		response.append("<FeatureList>").append(CRLF);
		response.append(StringEscapeUtils.escapeXml10(features.toString()));
		response.append("</FeatureList>").append(CRLF);
		return response.toString();
	}

	/**
	 * Returns the updates id for all resources.
	 *
	 * When all resources need to be refreshed, this id is updated.
	 *
	 * @return The system updated id.
	 * @since 1.50
	 */
	public static UnsignedIntegerFourBytes getDbSystemUpdateId() {
		LOCK_SYSTEM_UPDATE_ID.readLock().lock();
		try {
			if (dbSystemUpdateID != null) {
				return dbSystemUpdateID;
			}
		} finally {
			LOCK_SYSTEM_UPDATE_ID.readLock().unlock();
		}
		LOCK_SYSTEM_UPDATE_ID.writeLock().lock();
		try {
			if (PMS.getConfiguration().getUseCache()) {
				Connection connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					LOCK_SYSTEM_UPDATE_ID.readLock().lock();
					String systemUpdateIdFromDb = MediaTableMetadata.getMetadataValue(connection, METADATA_TABLE_KEY_SYSTEMUPDATEID);
					if (systemUpdateIdFromDb != null) {
						try {
							dbSystemUpdateID = new UnsignedIntegerFourBytes(systemUpdateIdFromDb);
						} catch (NumberFormatException ex) {
							LOGGER.debug("" + ex);
						}
					}
				}
			}
			if (dbSystemUpdateID == null) {
				dbSystemUpdateID = new UnsignedIntegerFourBytes(0);
			}
			return dbSystemUpdateID;
		} finally {
			LOCK_SYSTEM_UPDATE_ID.writeLock().unlock();
		}
	}

	/**
	 * Returns the updates id for all resources.
	 *
	 * When all resources need to be refreshed, this id is updated.
	 *
	 * @return The system updated id.
	 * @since 1.50
	 */
	private static void storeDbSystemUpdateId() {
		if (PMS.getConfiguration().getUseCache()) {
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			// Persist the new value to the database
			if (connection != null) {
				MediaTableMetadata.setOrUpdateMetadataValue(connection, METADATA_TABLE_KEY_SYSTEMUPDATEID, getDbSystemUpdateId().toString());
			}
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Call this method after making changes to your content directory.
	 * <p>
	 * This will notify clients that their view of the content directory is
	 * potentially outdated and has to be refreshed.
	 * </p>
	 */
	public static synchronized void bumpSystemUpdateId() {
		getDbSystemUpdateId().increment(true);
	}

	private static String getJUPnPDidlResults(List<LibraryResource> resultResources) {
		Result didlResult = new Result();
		for (LibraryResource resource : resultResources) {
			if (resource instanceof LibraryContainer container) {
				didlResult.addObject(LibraryResourceHelper.getContainer(container));
			} else if (resource instanceof LibraryItem item) {
				didlResult.addObject(LibraryResourceHelper.getItem(item));
			}
		}
		return didlResult.toString();
	}

}
