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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result;

import com.google.common.primitives.UnsignedInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAImageResElement;
import net.pms.encoders.Engine;
import net.pms.formats.Format;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.library.DVDISOFile;
import net.pms.library.LibraryResource;
import net.pms.library.PlaylistFolder;
import net.pms.library.RealFile;
import net.pms.library.virtual.VirtualFolderDbId;
import net.pms.media.MediaInfo;
import net.pms.media.MediaStatus;
import net.pms.media.MediaType;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicAlbum;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicArtist;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicGenre;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.PlaylistContainer;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.StorageFolder;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Movie;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.MusicTrack;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Photo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.VideoItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.sec.SEC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.renderers.Renderer;
import net.pms.util.FullyPlayed;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.model.ProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryResourceHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryResourceHelper.class);
	private static final SimpleDateFormat DIDL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	public static final BaseObject getBaseObject(LibraryResource resource) {
		try {
			if (resource.isFolder()) {
				return getContainer(resource);
			} else {
				return getItem(resource);
			}
		} catch (Exception ex) {
			LOGGER.debug("", ex);
		}
		return null;
	}

	public static final Container getContainer(LibraryResource resource) {
		final Renderer renderer = resource.getDefaultRenderer();
		final MediaInfo mediaInfo = resource.getMediaInfo();
		final MediaType mediaType = mediaInfo != null ? mediaInfo.getMediaType() : MediaType.UNKNOWN;
		boolean xbox360 = renderer.isXbox360();
		String resourceId = resource.getResourceId();
		Container result = null;
		if (xbox360 && resource.getFakeParentId() != null) {
			result = switch (resource.getFakeParentId()) {
				case "7" -> new MusicAlbum();
				case "6" -> new MusicArtist();
				case "5" -> new MusicGenre();
				case "F" -> new PlaylistContainer();
				default -> null;
			};
		}
		if (result == null) {
			if (resource instanceof PlaylistFolder) {
				result = new PlaylistContainer();
			} else if (resource instanceof VirtualFolderDbId virtualFolderDbId) {
				result = newContainerFromUpnpClass(virtualFolderDbId.getMediaTypeUclass());
			} else {
				result = new StorageFolder();
			}
		}
		if (xbox360) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}
		result.setId(resourceId);
		if (!resource.isDiscovered() && resource.childrenCount() == 0) {
			// When a folder has not been scanned for resources, it will
			// automatically have zero children.
			// Some renderers like XBMC will assume a folder is empty when
			// encountering childCount="0" and
			// will not display the folder. By returning childCount="1"
			// these renderers will still display
			// the folder. When it is opened, its children will be
			// discovered and childrenCount() will be
			// set to the right value.
			result.setChildCount(1L);
		} else {
			result.setChildCount((long) resource.childrenCount());
		}
		resourceId = resource.getParentId();
		if (xbox360 && resource.getFakeParentId() == null) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}
		result.setParentID(resourceId);
		result.setRestricted(true);
		String title = resource.getDisplayName(false);
		if (
			!renderer.isThumbnails() &&
			resource instanceof RealFile realfile &&
			realfile.isFullyPlayedMark()
		) {
			title = FullyPlayed.addFullyPlayedNamePrefix(title, resource);
		}
		if (resource instanceof VirtualFolderDbId) {
			title = resource.getName();
		}

		title = resource.resumeStr(title);
		result.setTitle(renderer.getDcTitle(title, resource.getDisplayNameSuffix(), resource));
		if (renderer.isSendFolderThumbnails() || resource instanceof DVDISOFile) {
			for (Res res : getThumbnailRes(resource, mediaType)) {
				result.addResource(res);
			}
		}
		return result;
	}

	public static final Item getItem(LibraryResource resource) {
		final Renderer renderer = resource.getDefaultRenderer();
		final Engine engine = resource.getEngine();
		final MediaInfo mediaInfo = resource.getMediaInfo();
		final MediaStatus mediaStatus = resource.getMediaStatus();
		final MediaSubtitle mediaSubtitle = resource.getMediaSubtitle();
		final Format format = resource.getFormat();
		final MediaType mediaType = mediaInfo != null ? mediaInfo.getMediaType() : MediaType.UNKNOWN;
		boolean subsAreValidForStreaming = false;
		boolean xbox360 = renderer.isXbox360();
		Item result;
		if (resource.getPrimaryResource() != null && mediaInfo != null && !mediaInfo.isSecondaryFormatValid()) {
			result = new Item();
		} else if (mediaType == MediaType.IMAGE || mediaType == MediaType.UNKNOWN && format != null && format.isImage()) {
			result = new Photo();
		} else if (mediaType == MediaType.AUDIO || mediaType == MediaType.UNKNOWN && format != null && format.isAudio()) {
			result = new MusicTrack();
		} else if (mediaInfo != null && mediaInfo.hasVideoMetadata() && (mediaInfo.getVideoMetadata().isTVEpisode() || StringUtils.isNotBlank(mediaInfo.getVideoMetadata().getYear()))) {
			// videoItem.movie is used for TV episodes and movies
			result = new Movie();
		} else {
			/**
			 * videoItem is used for recorded videos but does not support
			 * properties like episodeNumber, seriesTitle, etc.
			 *
			 * @see page 251 of
			 *      http://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service.pdf
			 */
			result = new VideoItem();
		}
		if (mediaInfo != null && mediaInfo.isVideo() && mediaSubtitle != null) {
			if (
				!renderer.getUmsConfiguration().isDisableSubtitles() &&
				(
					engine == null ||
					renderer.streamSubsForTranscodedVideo()
				) &&
				mediaSubtitle.isExternal() &&
				renderer.isExternalSubtitlesFormatSupported(mediaSubtitle, resource)
			) {
				subsAreValidForStreaming = true;
				LOGGER.trace("External subtitles \"{}\" can be streamed to {}", mediaSubtitle.getName(), renderer);
			} else if (LOGGER.isTraceEnabled()) {
				if (renderer.getUmsConfiguration().isDisableSubtitles()) {
					LOGGER.trace("Subtitles are disabled");
				} else if (mediaSubtitle.isEmbedded()) {
					LOGGER.trace("Subtitles track {} cannot be streamed because it is internal/embedded", mediaSubtitle.getId());
				} else if (engine != null && !renderer.streamSubsForTranscodedVideo()) {
					LOGGER.trace("Subtitles \"{}\" aren't supported while transcoding to {}", mediaSubtitle.getName(), renderer);
				} else {
					LOGGER.trace("Subtitles \"{}\" aren't valid for streaming to {}", mediaSubtitle.getName(), renderer);
				}
			}
		}

		String resourceId = resource.getResourceId();
		if (xbox360) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}
		result.setId(resourceId);

		resourceId = resource.getParentId();
		if (xbox360 && resource.getFakeParentId() == null) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}

		result.setParentID(resourceId);
		result.setRestricted(true);
		final MediaAudioMetadata audioMetadata = mediaInfo != null ? mediaInfo.getAudioMetadata() : null;

		/*
		 * Use the track title for audio files, otherwise use the filename.
		 */
		String title;
		if (audioMetadata != null && mediaInfo != null && mediaInfo.isAudio() && StringUtils.isNotBlank(audioMetadata.getSongname())) {
			title = "";
			if (renderer.isPrependTrackNumbers() && audioMetadata.getTrack() > 0) {
				// zero pad for proper numeric sorting on all devices
				title += String.format("%03d - ", audioMetadata.getTrack());
			}
			title += audioMetadata.getSongname();
		} else if (subsAreValidForStreaming) {
			title = resource.getDisplayName(false);
		} else {
			title = renderer.getUseSameExtension(resource.getDisplayName(false));
		}
		if (
			!renderer.isThumbnails() &&
			resource instanceof RealFile realfile &&
			realfile.isFullyPlayedMark()
		) {
			title = FullyPlayed.addFullyPlayedNamePrefix(title, resource);
		}

		title = resource.resumeStr(title);
		result.setTitle(renderer.getDcTitle(title, resource.getDisplayNameSuffix(), resource));

		if (renderer.isSamsung() && resource instanceof RealFile && resource.getMediaStatus() != null) {
			LOGGER.debug("Setting bookmark for {} => {}", title, resource.getMediaStatus().getBookmark());
			result.addProperty(new SEC.DcmInfo(String.format("CREATIONDATE=0,FOLDER=%s,BM=%d", title, resource.getMediaStatus().getBookmark())));
		}

		if (audioMetadata != null && renderer.isSendDateMetadataYearForAudioTags() && audioMetadata.getYear() > 1000 && result instanceof MusicTrack musicTrack) {
			musicTrack.setDate(Integer.toString(audioMetadata.getYear()));
		} else if (resource.getLastModified() > 0 && renderer.isSendDateMetadata()) {
			//hacked ?
			result.getProperties().set(new DC.Date(formatDate(new Date(resource.getLastModified()))));
		}

		if (mediaInfo != null && audioMetadata != null && result instanceof MusicTrack musicTrack) {
			if (StringUtils.isNotBlank(audioMetadata.getAlbum())) {
				musicTrack.setAlbum(audioMetadata.getAlbum());
			}

			if (StringUtils.isNotBlank(audioMetadata.getArtist())) {
				musicTrack.setCreator(audioMetadata.getArtist());
				musicTrack.addArtist(new UPNP.Artist(audioMetadata.getArtist()));
			}

			if (StringUtils.isNotBlank(audioMetadata.getComposer())) {
				musicTrack.addArtist(new UPNP.Artist(audioMetadata.getComposer(), "Composer"));
			}

			if (StringUtils.isNotBlank(audioMetadata.getConductor())) {
				musicTrack.addArtist(new UPNP.Artist(audioMetadata.getConductor(), "Conductor"));
			}

			if (StringUtils.isNotBlank(audioMetadata.getGenre())) {
				musicTrack.setGenres(new String[] {audioMetadata.getGenre()});
			}

			if (audioMetadata.getTrack() > 0) {
				musicTrack.setOriginalTrackNumber(audioMetadata.getTrack());
			}

			if (audioMetadata.getRating() != null) {
				//not upnp
				result.addProperty(new UPNP.Rating(audioMetadata.getRating().toString()));
			}
		}

		if (mediaInfo != null && mediaInfo.hasVideoMetadata() && result instanceof Movie movie) {
			MediaVideoMetadata videoMetadata = mediaInfo.getVideoMetadata();
			if (videoMetadata.isTVEpisode()) {
				if (StringUtils.isNotBlank(videoMetadata.getTVSeason())) {
					movie.setEpisodeSeason(UnsignedInteger.valueOf(videoMetadata.getTVSeason()));
				}
				if (StringUtils.isNotBlank(videoMetadata.getTVEpisodeNumber())) {
					movie.setEpisodeNumber(UnsignedInteger.valueOf(videoMetadata.getTVEpisodeNumber()));
				}
				if (StringUtils.isNotBlank(videoMetadata.getMovieOrShowName())) {
					movie.setSeriesTitle(videoMetadata.getMovieOrShowName());
				}
				if (StringUtils.isNotBlank(videoMetadata.getTVEpisodeName())) {
					movie.setProgramTitle(videoMetadata.getTVEpisodeName());
				}
			}
			if (mediaStatus != null) {
				movie.setPlaybackCount(mediaStatus.getPlaybackCount());
				if (StringUtils.isNotBlank(mediaStatus.getLastPlaybackTime())) {
					movie.setLastPlaybackTime(mediaStatus.getLastPlaybackTime());
				}
				if (StringUtils.isNotBlank(mediaStatus.getLastPlaybackPositionForUPnP())) {
					movie.setLastPlaybackPosition(mediaStatus.getLastPlaybackPositionForUPnP());
				}
			}
		}
		if (mediaType != MediaType.IMAGE) {
			for (Res res : getThumbnailRes(resource, mediaType)) {
				result.addResource(res);
			}
		}
		return result;
	}

	private static List<Res> getThumbnailRes(LibraryResource resource, MediaType mediaType) {

		/*
		 * JPEG_TN = Max 160 x 160; EXIF Ver.1.x or later or JFIF 1.02; SRGB or
		 * uncalibrated JPEG_SM = Max 640 x 480; EXIF Ver.1.x or later or JFIF
		 * 1.02; SRGB or uncalibrated PNG_TN = Max 160 x 160; Greyscale 8/16
		 * bit, Truecolor 24 bit, Indexed-color 24 bit, Greyscale with alpha
		 * 8/16 bit or Truecolor with alpha 24 bit; PNG_SM doesn't exist!
		 *
		 * The standard dictates that thumbnails for images and videos should be
		 * given as a <res> element: > If a UPnP AV MediaServer exposes a CDS
		 * object with a <upnp:class> > designation of object.item.imageItem or
		 * object.item.videoItem (or > any class derived from them), then the
		 * UPnP AV MediaServer should > provide a <res> element for the
		 * thumbnail resource. (Multiple > thumbnail <res> elements are also
		 * allowed.)
		 *
		 * It also dictates that if a <res> thumbnail is available, it HAS to be
		 * offered as JPEG_TN (although not exclusively): > If a UPnP AV
		 * MediaServer exposes thumbnail images for image or video > content,
		 * then a UPnP AV MediaServer shall provide a thumbnail that > conforms
		 * to guideline 7.1.7 (GUN 6SXDY) in IEC 62481-2:2013 mediaInfo > format
		 * profile and be declared with the JPEG_TN designation in the > fourth
		 * field of the res@protocolInfo attribute. > > When thumbnails are
		 * provided, the minimal expectation is to provide > JPEG thumbnails.
		 * However, vendors can also provide additional > thumbnails using the
		 * JPEG_TN or PNG_TN profiles.
		 *
		 * For videos content additional related images can be offered: > UPnP
		 * AV MediaServers that expose a video item can include in the > <item>
		 * element zero or more <res> elements referencing companion > images
		 * that provide additional descriptive information. Examples > of
		 * companion images include larger versions of thumbnails, posters >
		 * describing a movie, and others.
		 *
		 * For audio content, and ONLY for audio content, >upnp:albumArtURI>
		 * should be used: > If a UPnP AV MediaServer exposes a CDS object with
		 * a <upnp:class> > designation of object.item.audioItem or
		 * object.container.album.musicAlbum > (or any class derived from either
		 * class), then the UPnP AV MediaServer > should provide a
		 * <upnp:albumArtURI> element to present the URI for > the album art > >
		 * Unlike image or video content, thumbnails for audio content will >
		 * preferably be presented through the <upnp:albumArtURI> element.
		 *
		 * There's a difference between a thumbnail and album art. A thumbnail
		 * is a miniature still image of visual content, since audio isn't
		 * visual the concept is invalid. Album art is an image "tied to" that
		 * audio, but doesn't represent the audio itself.
		 *
		 * The same requirement of always providing a JPEG_TN applies to
		 * <upnp:albumArtURI> although formulated somewhat vaguer: > If album
		 * art thumbnails are provided, the desired expectation is > to have
		 * JPEG thumbnails. Additional thumbnails can also be provided.
		 */

		// Images add thumbnail resources together with the image resources in
		// appendImage()

		final Renderer renderer = resource.getDefaultRenderer();
		final MediaInfo mediaInfo = resource.getMediaInfo();
		List<Res> resources = new ArrayList<>();
		if (MediaType.IMAGE != mediaType) {
			ImageInfo imageInfo = null;
			if (resource.getThumbnailImageInfo() != null) {
				imageInfo = resource.getThumbnailImageInfo();
			} else if (mediaInfo != null && mediaInfo.getThumb() != null && mediaInfo.getThumb().getImageInfo() != null) {
				imageInfo = mediaInfo.getThumb().getImageInfo();
			}

			// Only include GIF elements if the source is a GIF and it's
			// supported by the renderer.
			boolean includeGIF = imageInfo != null && imageInfo.getFormat() == ImageFormat.GIF &&
				DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.GIF_LRG, renderer);

			// Add elements in any order, it's sorted by priority later
			List<DLNAImageResElement> resElements = new ArrayList<>();

			// Always include JPEG_TN as per DLNA standard
			resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_TN, imageInfo, true));
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_SM, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_SM, imageInfo, true));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_TN, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_TN, imageInfo, true));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_LRG, imageInfo, true));
			}

			if (imageInfo != null) {
				if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_RES_H_V, renderer) && imageInfo.getWidth() > 0 &&
					imageInfo.getHeight() > 0) {
					// Offer the exact resolution as JPEG_RES_H_V
					DLNAImageProfile exactResolution = DLNAImageProfile.createJPEG_RES_H_V(imageInfo.getWidth(), imageInfo.getHeight());
					resElements.add(new DLNAImageResElement(exactResolution, imageInfo, true));
				}
				if (includeGIF) {
					resElements.add(new DLNAImageResElement(DLNAImageProfile.GIF_LRG, imageInfo, true));
				}
				if (!DLNAImageProfile.JPEG_SM.isResolutionCorrect(imageInfo)) {
					if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_MED, renderer)) {
						resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_MED, imageInfo, true));
					}
					if (!DLNAImageProfile.JPEG_MED.isResolutionCorrect(imageInfo) &&
						(DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer))
					) {
						resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_LRG, imageInfo, true));
					}
				}
			}

			// Sort the elements by priority
			Collections.sort(resElements, DLNAImageResElement.getComparator(imageInfo != null ? imageInfo.getFormat() : ImageFormat.JPEG));

			for (DLNAImageResElement resElement : resElements) {
				Res res = getImageRes(resource, resElement);
				resources.add(res);
			}

			for (DLNAImageResElement resElement : resElements) {
				// Offering AlbumArt for video breaks the standard, but some
				// renderers need it
				switch (resElement.getProfile().toInt()) {
					case DLNAImageProfile.GIF_LRG_INT,
						DLNAImageProfile.JPEG_SM_INT,
						DLNAImageProfile.JPEG_TN_INT,
						DLNAImageProfile.PNG_LRG_INT,
						DLNAImageProfile.PNG_TN_INT
					-> {
						//should add
						//addAlbumArt(resource, sb, resElement.getProfile());
					}
				}
			}
		}
		return resources;
	}

	private static Res getImageRes(LibraryResource resource, DLNAImageResElement resElement) {
		if (resElement == null) {
			throw new NullPointerException("resElement cannot be null");
		}
		if (!resElement.isResolutionKnown() && DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile())) {
			throw new IllegalArgumentException("Resolution cannot be unknown for DLNAImageProfile.JPEG_RES_H_V");
		}
		String url;
		if (resElement.isThumbnail()) {
			url = resource.getThumbnailURL(resElement.getProfile());
		} else {
			url = resource.getURL((DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile()) ?
				"JPEG_RES" + resElement.getWidth() + "x" + resElement.getHeight() :
				resElement.getProfile().toString()) + "_");
		}
		if (StringUtils.isNotBlank(url)) {
			String ciFlag;
			/*
			 * Some Panasonic TV's can't handle if the thumbnails have the CI
			 * flag set to 0 while the main resource doesn't have a CI flag.
			 * DLNA dictates that a missing CI flag should be interpreted as if
			 * it were 0, so the result should be the same.
			 */
			if (resElement.getCiFlag() == null || resElement.getCiFlag() == 0) {
				ciFlag = "";
			} else {
				ciFlag = ";DLNA.ORG_CI=" + resElement.getCiFlag().toString();
			}
			Res res = new Res();
			if (resElement.getSize() != null && resElement.getSize() > 0) {
				res.setSize(resElement.getSize());
			}
			if (resElement.isResolutionKnown()) {
				res.setResolution(resElement.getWidth(), resElement.getHeight());
			}
			String protocolInfoStr = "http-get:*:" + resElement.getProfile().getMimeType() + ":DLNA.ORG_PN=" +
				resElement.getProfile() + ciFlag + ";DLNA.ORG_FLAGS=00900000000000000000000000000000";
			ProtocolInfo protocolInfo = new ProtocolInfo(protocolInfoStr);
			res.setProtocolInfo(protocolInfo);
			try {
				res.setValue(new URI(url));
			} catch (URISyntaxException ex) {
				LOGGER.trace("Res fail with url: {}", url);
			}
			return res;
		}
		return null;
	}

	private static Container newContainerFromUpnpClass(String upnpClass) {
		return switch (upnpClass) {
			case "object.container.album.musicAlbum" -> new MusicAlbum();
			case "object.container.person.musicArtist" -> new MusicArtist();
			case "object.container.playlistContainer" -> new PlaylistContainer();
			case "object.container.storageFolder" -> new StorageFolder();
			default -> new Container();
		};
	}

	private static String formatDate(Date data) {
		synchronized (DIDL_DATE_FORMAT) {
			return DIDL_DATE_FORMAT.format(data);
		}
	}

}
