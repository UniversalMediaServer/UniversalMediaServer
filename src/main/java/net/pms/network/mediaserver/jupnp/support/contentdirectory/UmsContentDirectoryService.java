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
import java.util.List;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.PlaylistFolder;
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.network.mediaserver.handlers.SearchRequestHandler;
import net.pms.network.mediaserver.jupnp.model.meta.UmsRemoteClientInfo;
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
			name = "A_ARG_TYPE_URI",
			sendEvents = false,
			datatype = "uri"),
	@UpnpStateVariable(
			name = "A_ARG_TYPE_SearchCriteria",
			sendEvents = false,
			datatype = "string"),
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

	@UpnpStateVariable(sendEvents = false)
	private final CSV<String> searchCapabilities = new CSVString();

	@UpnpStateVariable(sendEvents = false)
	private final CSV<String> sortCapabilities = new CSVString();

	@UpnpStateVariable(
			sendEvents = true,
			defaultValue = "0",
			eventMaximumRateMilliseconds = 200
	)
	private final UnsignedIntegerFourBytes systemUpdateID = new UnsignedIntegerFourBytes(0);

	protected final PropertyChangeSupport propertyChangeSupport;

	public UmsContentDirectoryService() {
		this.searchCapabilities.addAll(CAPS_SEARCH);
		this.sortCapabilities.addAll(CAPS_SORT);
		propertyChangeSupport = new PropertyChangeSupport(this);
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
		//maybe use the provided systemUpdateID ?
		return new UnsignedIntegerFourBytes(DLNAResource.getSystemUpdateId());
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}

	/**
	 * Call this method after making changes to your content directory.
	 * <p>
	 * This will notify clients that their view of the content directory is
	 * potentially outdated and has to be refreshed.
	 * </p>
	 */
	protected synchronized void changeSystemUpdateID() {
		Long oldUpdateID = getSystemUpdateID().getValue();
		systemUpdateID.increment(true);
		getPropertyChangeSupport().firePropertyChange(
				"SystemUpdateID",
				oldUpdateID,
				getSystemUpdateID().getValue()
		);
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

		boolean browseDirectChildren = browseFlag == BrowseFlag.DIRECT_CHILDREN;

		List<DLNAResource> files = PMS.get().getRootFolder(renderer).getDLNAResources(
				objectID,
				browseDirectChildren,
				(int) startingIndex,
				(int) requestedCount,
				renderer,
				null
		);

		long minus = 0;
		//we may use DIDL object directly when ready
		StringBuilder filesData = new StringBuilder();
		filesData.append(HTTPXMLHelper.DIDL_HEADER);
		if (files != null) {
			for (DLNAResource uf : files) {
				if (uf instanceof PlaylistFolder playlistFolder) {
					File f = new File(uf.getFileName());
					if (uf.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (uf != null &&
					uf.isCompatible(renderer) &&
					(uf.getEngine() == null || uf.getEngine().isEngineCompatible(renderer)) ||
					// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
					// all possible combination not only those supported by renderer because the renderer setting could be wrong.
					uf != null && files.get(0).isInsideTranscodeFolder()
				) {
					filesData.append(uf.getDidlString(renderer));
				} else {
					minus++;
				}
			}
		}
		filesData.append(HTTPXMLHelper.DIDL_FOOTER);

		long filessize = 0;
		if (files != null) {
			filessize = files.size();
		}
		long count = filessize - minus;

		long totalMatches;
		if (browseDirectChildren && renderer != null && renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, files are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular files, plus the #transcode folder, maybe the #imdb one, also files can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			totalMatches = startingIndex + requestedCount + 1L; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (filessize - minus <= 0) {
				totalMatches = startingIndex;
			}
		} else if (browseDirectChildren) {
			DLNAResource parentFolder;
			if (files != null && filessize > 0) {
				parentFolder = files.get(0).getParent();
			} else {
				parentFolder = PMS.get().getRootFolder(renderer).getDLNAResource(objectID, renderer);
			}
			if (parentFolder != null) {
				totalMatches = parentFolder.childrenNumber() - minus;
			} else {
				totalMatches = filessize - minus;
			}
		} else {
			// From upnp spec: If BrowseMetadata is specified in the BrowseFlags then TotalMatches = 1
			totalMatches = 1;
		}

		long containerUpdateID = DLNAResource.getSystemUpdateId();
		//jupnp will escape DIDL result itself
		//this will not be necessary when UMS will build results from DIDL objects
		String result = StringEscapeUtils.unescapeXml(filesData.toString());
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
		if (xbox360 && PMS.getConfiguration().getUseCache() && PMS.get().getLibrary().isEnabled() && xboxId != null) {
			searchCriteria = null;
			if (xboxId.equals("7") && PMS.get().getLibrary().getAlbumFolder() != null) {
				containerId = PMS.get().getLibrary().getAlbumFolder().getResourceId();
			} else if (xboxId.equals("6") && PMS.get().getLibrary().getArtistFolder() != null) {
				containerId = PMS.get().getLibrary().getArtistFolder().getResourceId();
			} else if (xboxId.equals("5") && PMS.get().getLibrary().getGenreFolder() != null) {
				containerId = PMS.get().getLibrary().getGenreFolder().getResourceId();
			} else if (xboxId.equals("F") && PMS.get().getLibrary().getPlaylistFolder() != null) {
				containerId = PMS.get().getLibrary().getPlaylistFolder().getResourceId();
			} else if (xboxId.equals("4") && PMS.get().getLibrary().getAllFolder() != null) {
				containerId = PMS.get().getLibrary().getAllFolder().getResourceId();
			} else if (xboxId.equals("1")) {
				String artist = getEnclosingValue(searchCriteria, "upnp:artist = &quot;", "&quot;)");
				if (artist != null) {
					containerId = PMS.get().getLibrary().getArtistFolder().getResourceId();
					searchCriteria = artist;
				}
			}
		}

		List<DLNAResource> files = PMS.get().getRootFolder(renderer).getDLNAResources(
			containerId,
			true,
			(int) startingIndex,
			(int) requestedCount,
			renderer,
			searchCriteria
		);

		if (searchCriteria != null && files != null) {
			UMSUtils.filterResourcesByName(files, searchCriteria, false, false);
			if (xbox360 && !files.isEmpty()) {
				files = files.get(0).getChildren();
			}
		}

		long minus = 0;
		StringBuilder filesData = new StringBuilder();
		filesData.append(HTTPXMLHelper.DIDL_HEADER);
		if (files != null) {
			for (DLNAResource uf : files) {
				if (uf instanceof PlaylistFolder playlistFolder) {
					File f = new File(uf.getFileName());
					if (uf.getLastModified() < f.lastModified()) {
						playlistFolder.resolve();
					}
				}

				if (xbox360 && xboxId != null && uf != null) {
					uf.setFakeParentId(xboxId);
				}

				if (
					uf != null &&
					(uf.isCompatible(renderer) &&
					(uf.getEngine() == null || uf.getEngine().isEngineCompatible(renderer)) ||
					// do not check compatibility of the media for items in the FileTranscodeVirtualFolder because we need
					// all possible combination not only those supported by renderer because the renderer setting could be wrong.
					files.get(0).isInsideTranscodeFolder())
				) {
					filesData.append(uf.getDidlString(renderer));
				} else {
					minus++;
				}
			}
		}
		filesData.append(HTTPXMLHelper.DIDL_FOOTER);

		int filessize = 0;
		if (files != null) {
			filessize = files.size();
		}
		long count = filessize - minus;

		long totalMatches;
		if (renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
			// with the new parser, files are parsed and analyzed *before*
			// creating the DLNA tree, every 10 items (the ps3 asks 10 by 10),
			// so we do not know exactly the total number of items in the DLNA folder to send
			// (regular files, plus the #transcode folder, maybe the #imdb one, also files can be
			// invalidated and hidden if format is broken or encrypted, etc.).
			// let's send a fake total size to force the renderer to ask following items
			totalMatches = startingIndex + requestedCount + 1; // returns 11 when 10 asked

			// If no more elements, send the startingIndex
			if (filessize - minus <= 0) {
				totalMatches = startingIndex;
			}
		} else {
			DLNAResource parentFolder;
			if (files != null && filessize > 0) {
				parentFolder = files.get(0).getParent();
			} else {
				parentFolder = PMS.get().getRootFolder(renderer).getDLNAResource(containerId, renderer);
			}
			if (parentFolder != null) {
				totalMatches = parentFolder.childrenNumber() - minus;
			} else {
				totalMatches = filessize - minus;
			}
		}

		long containerUpdateID = DLNAResource.getSystemUpdateId();
		//jupnp will escape DIDL result itself
		//this will not be necessary when UMS will build results from DIDL objects
		String result = StringEscapeUtils.unescapeXml(filesData.toString());
		return new BrowseResult(result, count, totalMatches, containerUpdateID);
	}

	private String samsungSetBookmark(
		String objectID,
		long posSecond,
		String categoryType,
		String rId,
		RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		if (posSecond == 0) {
			// Sometimes when Samsung device is starting to play the video
			// it sends X_SetBookmark message immediately with the position=0.
			// No need to update database in such case.
			LOGGER.debug("Skipping \"set bookmark\". Position=0");
		} else {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					DLNAResource dlna = PMS.get().getRootFolder(renderer).getDLNAResource(objectID, renderer);
					File file = new File(dlna.getFileName());
					String path = file.getCanonicalPath();
					MediaTableFilesStatus.setBookmark(connection, path, (int) posSecond);
				}
			} catch (IOException e) {
				LOGGER.error("Cannot set bookmark", e);
			} finally {
				MediaDatabase.close(connection);
			}
		}
		return "";
	}

	private String samsungGetFeaturesList(
		RemoteClientInfo remoteClientInfo
	) throws ContentDirectoryException {
		UmsRemoteClientInfo info = new UmsRemoteClientInfo(remoteClientInfo);
		Renderer renderer = info.renderer;
		StringBuilder features = new StringBuilder();
		String rootFolderId = PMS.get().getRootFolder(renderer).getResourceId();
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
}
