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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.dlna.DidlHelper;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.jupnp.model.meta.UmsRemoteClientInfo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Parser;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Result;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.StoreResourceHelper;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject.IUpdateObjectHandler;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject.UpdateObjectFactory;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.MediaScanner;
import net.pms.store.MediaStatusStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.PlaylistManager;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.MediaLibrary;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.utils.StoreResourceSorter;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
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
import org.jupnp.support.model.SearchResult;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@UpnpService(
		serviceId =
		@UpnpServiceId("ContentDirectory"),
		serviceType =
		@UpnpServiceType(value = "ContentDirectory", version = 1)
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
			datatype = "string"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_FeatureList",
			sendEvents = false,
			datatype = "string")
})
public class UmsContentDirectoryService {

	public final static String EMPTY_FILE_CONTENT = "<UPLOAD RESOURCE>";

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsContentDirectoryService.class);
	private static final List<String> CAPS_SEARCH = List.of();
	private static final List<String> CAPS_SORT = List.of("upnp:class", "dc:title", "dc:creator", "upnp:artist", "upnp:album", "upnp:genre");
	private static final String CRLF = "\r\n";

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
		//Todo : this may not be necessary if we follow the upnp rules.
		MediaStoreIds.incrementSystemUpdateId();
		systemUpdateID = new UnsignedIntegerFourBytes(MediaStoreIds.getSystemUpdateId().getValue());
		propertyChangeSupport = new PropertyChangeSupport(this);
		systemUpdateIdTimer.schedule(systemUpdateIdTask, 0, 200);
	}

	@UpnpAction(out =
			@UpnpOutputArgument(name = "SearchCaps")
	)
	public CSV<String> getSearchCapabilities() {
		return searchCapabilities;
	}

	@UpnpAction(out =
			@UpnpOutputArgument(name = "SortCaps")
	)
	public CSV<String> getSortCapabilities() {
		return sortCapabilities;
	}

	@UpnpAction(out =
			@UpnpOutputArgument(name = "Id")
	)
	public synchronized UnsignedIntegerFourBytes getSystemUpdateID() {
		return MediaStoreIds.getSystemUpdateId();
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}

	private void systemUpdateIdChanged() {
		long oldValue = systemUpdateID.getValue();
		long newValue = MediaStoreIds.getSystemUpdateId().getValue();
		if (oldValue != newValue) {
			getPropertyChangeSupport().firePropertyChange(
					"SystemUpdateID",
					oldValue,
					newValue
			);
			systemUpdateID = new UnsignedIntegerFourBytes(newValue);
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
			LOGGER.debug("Trying to sort on a browse action with '{}' !", orderBy);
			throw new ContentDirectoryException(ContentDirectoryErrorCode.UNSUPPORTED_SORT_CRITERIA, ex.toString());
		}

		try {
			return browse(
					objectId,
					BrowseFlag.valueOrNullOf(browseFlag),
					filter,
					firstResult.getValue(),
					maxResults.getValue(),
					orderByCriteria,
					remoteClientInfo
			);
		} catch (ContentDirectoryException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Exception in result creation \"{}\"", ex.getMessage(), ex);
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
	public SearchResult search(
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
			LOGGER.debug("Trying to sort on a search action with '{}' !", orderBy);
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

	@UpnpAction(out = {
		@UpnpOutputArgument(name = "Result",
				stateVariable = "A_ARG_TYPE_Result",
				getterName = "getResult"),
		@UpnpOutputArgument(name = "ObjectID",
				stateVariable = "A_ARG_TYPE_ObjectID",
				getterName = "getObjectID")
		})
	public CreateObjectResult createObject(
			@UpnpInputArgument(name = "ContainerID", stateVariable = "A_ARG_TYPE_ObjectID") String containerId,
			@UpnpInputArgument(name = "Elements", stateVariable = "A_ARG_TYPE_Result") String elements,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		try {
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

			StoreResource parentContainer = renderer.getMediaStore().getResource(containerId);
			if (parentContainer instanceof StoreContainer storeContainer) {
				Parser parser = new Parser();
				Result modelObjectToAdd = parser.parse(elements);

				checkInput(modelObjectToAdd);

				StoreResource resource = null;
				if (modelObjectToAdd.getItems().size() > 0) {
					resource = createItemResource(storeContainer, modelObjectToAdd.getItems().get(0), resource);
				}
				if (modelObjectToAdd.getContainers().size() > 0) {
					resource = createContainerResource(storeContainer, modelObjectToAdd.getContainers().get(0), resource);
				}
				if (resource != null) {
					MediaScanner.backgroundScanFileOrFolder(resource.getFileName());
					return createObjectResult(renderer, resource);
				}
				throw new ContentDirectoryException(712, "The specified Elements argument is not supported or is invalid.");
			} else {
				throw new ContentDirectoryException(710, "The specified ContainerID is invalid or identifies an object that is not a container.");
			}
		} catch (Exception e) {
			if (e instanceof ContentDirectoryException cde) {
				throw cde;
			} else {
				LOGGER.error("createObject failed", e);
				throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
			}
		}
	}

	private void checkInput(Result modelObjectToAdd) {
		if (modelObjectToAdd.getItems().size() > 1) {
			LOGGER.trace("more than 1 item ... using first found.");
		}
		if (modelObjectToAdd.getContainers().size() > 1) {
			LOGGER.trace("more than 1 container ... using first found.");
		}
		if (modelObjectToAdd.getContainers().size() > 0 && modelObjectToAdd.getItems().size() > 0) {
			LOGGER.trace("found items and container ... using container object ...");
		}
	}

	private StoreResource createContainerResource(StoreContainer storeContainer, Container containerToCreate, StoreResource resource) throws Exception {
		if (containerToCreate != null) {
			if ("object.container.storageFolder".equalsIgnoreCase(containerToCreate.getUpnpClassName())) {
				resource = createFolder(storeContainer, containerToCreate.getTitle());
			}
		}
		return resource;
	}

	private StoreResource createItemResource(StoreContainer storeContainer, Item itemToCreate, StoreResource resource) throws Exception {
		if (itemToCreate != null) {
			if ("object.item.playlistItem".equalsIgnoreCase(itemToCreate.getUpnpClassName())) {
				resource = PlaylistManager.createPlaylist(storeContainer, itemToCreate.getTitle());
			} else if ("object.item".equalsIgnoreCase(itemToCreate.getUpnpClassName())) {
				resource = createEmptyItem(storeContainer, itemToCreate.getTitle());
			} else {
				LOGGER.error("CreateObject of unknown upnp:class : " + itemToCreate.getUpnpClassName());
			}
		}
		return resource;
	}

	private CreateObjectResult createObjectResult(Renderer renderer, StoreResource resource) {
		LOGGER.debug("createObjectResult for objectID {}", resource.getId());
		String result;
		result = getJUPnPDidlResults(List.of(resource), null);
		if (renderer.getUmsConfiguration().isUpnpJupnpDidl()) {
			result = getJUPnPDidlResults(List.of(resource), null);
		} else {
			result = DidlHelper.getDidlResults(List.of(resource));
		}
		if (renderer.getUmsConfiguration().isUpnpDebug()) {
			logDidlLiteResult(result);
		}
		return new CreateObjectResult(result, resource.getId());
	}

	private StoreResource createEmptyItem(StoreContainer storeContainer, String title) {
		File newItem = new File(storeContainer.getFileName(), title);
		if (!newItem.exists()) {
			try {
				newItem.createNewFile();
				FileWriter fileWriter = new FileWriter(newItem);
				fileWriter.write(EMPTY_FILE_CONTENT);
				fileWriter.close();

				StoreResource newResource = storeContainer.getDefaultRenderer().getMediaStore().createResourceFromFile(newItem);
				storeContainer.addChild(newResource);
				if (newResource.getId() != null) {
					LOGGER.error("created resource at {} got a NULL id!", newResource.getFileName());
				}
				return newResource;
			} catch (IOException e) {
				LOGGER.warn("cannot create object item", e);
				return null;
			}
		} else {
			LOGGER.warn("Folder or file already exists for path {}", newItem.getAbsolutePath());
			return null;
		}
	}

	private StoreResource createFolder(StoreContainer storeContainer, String title) {
		File newContainer = new File(storeContainer.getFileName(), title);
		if (!newContainer.exists()) {
			newContainer.mkdir();
			StoreResource newResource = storeContainer.getDefaultRenderer().getMediaStore().createResourceFromFile(newContainer);
			storeContainer.addChild(newResource);
			return newResource;
		} else {
			LOGGER.warn("file system resource already exists for path {}", newContainer.getAbsolutePath());
			throw new RuntimeException(String.format("file system resource already exists for path : %s", newContainer.getAbsolutePath()));
		}
	}

	@UpnpAction(out =
			@UpnpOutputArgument(name = "NewID", stateVariable = "A_ARG_TYPE_ObjectID"))
	public String createReference(
			@UpnpInputArgument(name = "ContainerID", stateVariable = "A_ARG_TYPE_ObjectID") String containerId,
			@UpnpInputArgument(name = "ObjectID", stateVariable = "A_ARG_TYPE_ObjectID") String objectId,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		try {
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

			StoreResource objectResource = renderer.getMediaStore().getResource(objectId);
			StoreResource containerResource = renderer.getMediaStore().getResource(containerId);
			if (objectResource == null) {
				throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
			}
			if (containerResource instanceof StoreContainer storeContainer) {
				if (storeContainer instanceof PlaylistFolder playlistFolder) {
					String newID = PlaylistManager.addEntryToPlaylist(objectResource, playlistFolder);
					if (newID != null) {
						MediaScanner.backgroundScanFileOrFolder(playlistFolder.getFileName());
						return newID;
					}
					throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, "entry already in Playlist");
				} else {
					//this object create reference is not yet implemented
					throw new ContentDirectoryException(ErrorCode.OPTIONAL_ACTION);
				}
			} else {
				throw new ContentDirectoryException(710, "the ContainerID argument is invalid or identifies an object that is not a container.");
			}
		} catch (Exception e) {
			if (e instanceof ContentDirectoryException cde) {
				throw cde;
			} else {
				LOGGER.error("createReference failed", e);
				throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
			}
		}
	}

	@UpnpAction()
	public void updateObject(
		@UpnpInputArgument(name = "ObjectID", stateVariable = "A_ARG_TYPE_ObjectID") String objectId,
		@UpnpInputArgument(name = "CurrentTagValue", stateVariable = "A_ARG_TYPE_TagValueList") String currentTagValue,
		@UpnpInputArgument(name = "NewTagValue", stateVariable = "A_ARG_TYPE_TagValueList") String newTagValue,
		RemoteClientInfo remoteClientInfo
		) throws ContentDirectoryException {
		try {
			UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
			Renderer renderer = info.renderer;
			if (renderer == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Unrecognized media renderer");
				}
				return;
			}
			if (!renderer.isAllowed()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
				}
				return;
			}

			StoreResource objectResource = renderer.getMediaStore().getResource(objectId);
			if (objectResource == null) {
				throw new ContentDirectoryException(701, "no such object");
			}

			String[] currentFragments = UpdateObjectFactory.getFragments(currentTagValue);
			String[] newFragments = UpdateObjectFactory.getFragments(newTagValue);
			if (currentFragments.length != newFragments.length) {
				throw new ContentDirectoryException(706, "UpdateObject() failed because the number of entries (including empty" +
					" entries) in the CurrentTagValue and NewTagValue arguments do not match.");
			}

			for (int i = 0; i < currentFragments.length; i++) {
				IUpdateObjectHandler handler = UpdateObjectFactory.getUpdateObjectHandler(objectResource, currentFragments[i], newFragments[i]);
				if (handler != null) {
					handler.handle();
				}
			}
		} catch (Exception e) {
			if (e instanceof ContentDirectoryException cde) {
				throw cde;
			} else {
				LOGGER.error("updateObject failed", e);
				throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
			}
		}
	}

	@UpnpAction(name = "DestroyObject")
	public void destroyObject(
			@UpnpInputArgument(name = "ObjectID", stateVariable = "A_ARG_TYPE_ObjectID") String objectId,
			RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		try {
			UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
			Renderer renderer = info.renderer;
			if (renderer == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Unrecognized media renderer");
				}
				throw new ContentDirectoryException(714, "No such resource");
			}
			if (!renderer.isAllowed()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Recognized media renderer \"{}\" is not allowed", renderer.getRendererName());
				}
				throw new ContentDirectoryException(715, "Source resource access denied");
			}

			StoreResource objectResource = renderer.getMediaStore().getResource(objectId);
			if (objectResource == null) {
				throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
			} else if (objectResource.getParent() instanceof PlaylistFolder playlistFolder) {
				LOGGER.info("removing entry {} from playlist {} ...", objectResource.getDisplayName(), playlistFolder.getDisplayName());
				if (!PlaylistManager.removeEntryFromPlaylist(objectResource, playlistFolder)) {
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
				}
			} else if (objectResource instanceof PlaylistFolder playlistFolder) {
				LOGGER.info("removing playlist {} ...", playlistFolder.getDisplayName());
				if (!PlaylistManager.deletePlaylistFromDisk(playlistFolder)) {
					throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, "failed deleting playlist file");
				} else {
					MediaScanner.backgroundScanFileOrFolder(playlistFolder.getFileName());
				}
			} else {
				//this object destroy is not yet implemented
				throw new ContentDirectoryException(ErrorCode.OPTIONAL_ACTION);
			}
		} catch (Exception e) {
			if (e instanceof ContentDirectoryException cde) {
				throw cde;
			} else {
				LOGGER.error("destroyObject failed", e);
				throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
			}
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

	@UpnpAction(name = "X_GetFeatureList",
			out =
			@UpnpOutputArgument(name = "FeatureList", stateVariable = "A_ARG_TYPE_FeatureList"))
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

		if (objectID == null || objectID.length() == 0) {
			objectID = "0";
		}

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
				} else if (resource instanceof StoreItem item && item.isCompatible()) {
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

		long containerUpdateID = MediaStoreIds.getSystemUpdateId().getValue();
		LOGGER.trace("Creating DIDL result");
		String result;
		if (renderer.getUmsConfiguration().isUpnpJupnpDidl()) {
			result = getJUPnPDidlResults(resultResources.subList(fromIndex, toIndex), filter);
		} else {
			result = DidlHelper.getDidlResults(resultResources.subList(fromIndex, toIndex));
		}
		LOGGER.trace("DIDL result created");
		if (renderer.getUmsConfiguration().isUpnpDebug()) {
			logDidlLiteResult(result);
		}
		LOGGER.trace("Returning browse result");
		return new BrowseResult(result, count, totalMatches, containerUpdateID);
	}

	private SearchResult search(
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

		try {
			DbIdMediaType requestType = SearchRequestHandler.getRequestType(searchCriteria);
			List<StoreResource> resultResources = null;
			int totalMatches = 0;
			totalMatches = SearchRequestHandler.getLibraryResourceCountFromSQL(SearchRequestHandler.convertToCountSql(searchCriteria, requestType));
			String sqlFiles = SearchRequestHandler.convertToFilesSql(searchCriteria, startingIndex, requestedCount, orderBy, requestType);
			resultResources = SearchRequestHandler.getLibraryResourceFromSQL(renderer, sqlFiles, requestType);

			long containerUpdateID = MediaStoreIds.getSystemUpdateId().getValue();
			LOGGER.trace("Creating DIDL result");
			String result;
			if (renderer.getUmsConfiguration().isUpnpJupnpDidl()) {
				result = getJUPnPDidlResults(resultResources, filter);
			} else {
				result = DidlHelper.getDidlResults(resultResources);
			}
			LOGGER.trace("DIDL result created");
			if (renderer.getUmsConfiguration().isUpnpDebug()) {
				logDidlLiteResult(result);
			}
			LOGGER.trace("Returning search result");
			return new SearchResult(result, resultResources.size(), totalMatches, containerUpdateID);
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

	private SearchResult searchToBrowse(
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
		if (xbox360 && xboxId != null) {
			searchCriteria = null;
			MediaLibrary library = renderer.getMediaStore().getMediaLibrary();
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

		List<StoreResource> resources = renderer.getMediaStore().getResources(
				containerId,
				true
		);

		List<StoreResource> resultResources = new ArrayList<>();
		long resourceCount = 0;
		long badResourceCount = 0;

		//keep only compatible resources
		if (resources != null) {
			if (searchCriteria != null) {
				UMSUtils.filterResourcesByName(resources, searchCriteria, false, false);
				if (xbox360 && !resources.isEmpty() && resources.get(0) instanceof StoreContainer storeContainer) {
					resources = storeContainer.getChildren();
				}
			}
			resourceCount = resources.size();
			for (StoreResource resource : resources) {
				if (resource instanceof PlaylistFolder playlistFolder) {
					File f = new File(resource.getFileName());
					if (resource.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (xbox360 && xboxId != null && resource != null) {
					resource.setFakeParentId(xboxId);
				}

				if (resource instanceof StoreContainer container) {
					resultResources.add(container);
				} else if (resource instanceof StoreItem item && item.isCompatible()) {
					resultResources.add(item);
				} else {
					badResourceCount++;
				}
			}
		}

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
			StoreContainer parentFolder;
			if (resources != null && resourceCount > 0) {
				parentFolder = resources.get(0).getParent();
			} else {
				StoreResource resource = renderer.getMediaStore().getResource(containerId);
				if (resource instanceof StoreContainer storeContainer) {
					parentFolder = storeContainer;
				} else {
					if (resource instanceof StoreItem) {
						LOGGER.debug("Trying to search on a store item for containerId '{}' !", containerId);
					} else {
						LOGGER.debug("Trying to search on a null object for containerId '{}' !", containerId);
					}
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT);
				}
			}
			if (parentFolder != null) {
				totalMatches = parentFolder.childrenCount() - badResourceCount;
			} else {
				totalMatches = resourceCount - badResourceCount;
			}
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

		long containerUpdateID = MediaStoreIds.getSystemUpdateId().getValue();
		String result;
		LOGGER.trace("Creating DIDL result");
		if (renderer.getUmsConfiguration().isUpnpJupnpDidl()) {
			result = getJUPnPDidlResults(resultResources.subList(fromIndex, toIndex), filter);
		} else {
			result = DidlHelper.getDidlResults(resultResources.subList(fromIndex, toIndex));
		}
		LOGGER.trace("DIDL result created");
		if (renderer.getUmsConfiguration().isUpnpDebug()) {
			logDidlLiteResult(result);
		}
		LOGGER.trace("Returning search result");
		return new SearchResult(result, count, totalMatches, containerUpdateID);
	}

	private static void logDidlLiteResult(String result) {
		if (LOGGER.isTraceEnabled()) {
			String formattedResult;
			try {
				formattedResult = "DIDL-Lite result:\n";
				formattedResult += StringUtil.prettifyXML(result, StandardCharsets.UTF_8, 4);
			} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
				formattedResult = "DIDL-Lite result isn't valid XML, using text formatting: " + e.getMessage() + "\n";
				formattedResult += "    " + result.replace("\n", "\n    ");
			}
			LOGGER.trace(formattedResult);
		}
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
				StoreResource resource = renderer.getMediaStore().getResource(objectID);
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
		String mediaStoreId = renderer.getMediaStore().getResourceId();
		features.append(CRLF);
		features.append("<Features xmlns=\"urn:schemas-upnp-org:av:avs\"");
		features.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		features.append(" xsi:schemaLocation=\"urn:schemas-upnp-org:av:avs http://www.upnp.org/schemas/av/avs.xsd\">").append(CRLF);
		features.append("<Feature name=\"samsung.com_BASICVIEW\" version=\"1\">").append(CRLF);
		// we may use here different container IDs in the future
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.audioItem\"/>").append(CRLF);
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.videoItem\"/>").append(CRLF);
		features.append("<container id=\"").append(mediaStoreId).append("\" type=\"object.item.imageItem\"/>").append(CRLF);
		features.append("</Feature>").append(CRLF);
		features.append("</Features>").append(CRLF);
		return features.toString();
	}

	private static String getJUPnPDidlResults(List<StoreResource> resultResources, String filter) {
		Result didlResult = new Result();
		for (StoreResource resource : resultResources) {
			didlResult.addObject(StoreResourceHelper.getBaseObject(resource, filter));
		}
		return didlResult.toString();
	}

}
