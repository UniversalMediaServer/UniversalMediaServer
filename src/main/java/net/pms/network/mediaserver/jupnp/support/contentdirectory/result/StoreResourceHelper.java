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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.pms.PMS;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAImageResElement;
import net.pms.encoders.EncodingFormat;
import net.pms.formats.Format;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
import net.pms.media.MediaStatus;
import net.pms.media.MediaType;
import net.pms.media.audio.MediaAudio;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.configuration.NetworkInterfaceAssociation;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Desc;
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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dlna.DlnaProtocolInfo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.pv.PV;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.sec.SEC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP.AlbumArtURI;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.DVDISOFile;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.container.RealFolder;
import net.pms.store.container.VirtualFolderDbId;
import net.pms.store.item.RealFile;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreResourceHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceHelper.class);
	private static final SimpleDateFormat DIDL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	/**
	 * This class is not meant to be instantiated.
	 */
	private StoreResourceHelper() {
	}

	public static final BaseObject getBaseObject(StoreResource resource, String filter) {
		try {
			if (resource instanceof StoreContainer container) {
				return getContainer(container, filter);
			} else if (resource instanceof StoreItem item) {
				return getItem(item, filter);
			}
		} catch (Exception ex) {
			LOGGER.debug("", ex);
		}
		return null;
	}

	public static final Container getContainer(StoreContainer container, String filter) {
		final Renderer renderer = container.getDefaultRenderer();
		final MediaInfo mediaInfo = container.getMediaInfo();
		final MediaType mediaType = mediaInfo != null ? mediaInfo.getMediaType() : MediaType.UNKNOWN;
		boolean xbox360 = renderer.isXbox360();
		String resourceId = container.getResourceId();
		Container result = null;
		if (xbox360 && container.getFakeParentId() != null) {
			result = switch (container.getFakeParentId()) {
				case "7" -> new MusicAlbum();
				case "6" -> new MusicArtist();
				case "5" -> new MusicGenre();
				case "F" -> new PlaylistContainer();
				default -> null;
			};
		}
		if (result == null) {
			if (container instanceof PlaylistFolder) {
				result = new PlaylistContainer();
			} else if (container instanceof VirtualFolderDbId virtualFolderDbId) {
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
		if (!container.isDiscovered() && container.childrenCount() == 0) {
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
			result.setChildCount((long) container.childrenCount());
		}
		resourceId = container.getParentId();
		if (xbox360 && container.getFakeParentId() == null) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}
		result.setParentID(resourceId);
		result.setRestricted(true);
		String title = container.getDisplayName(false);
		if (
			!renderer.isThumbnails() &&
			container instanceof RealFolder realfolder &&
			realfolder.isFullyPlayedMark()
		) {
			title = FullyPlayed.addFullyPlayedNamePrefix(title, container);
		}

		result.setTitle(renderer.getDcTitle(title, container.getDisplayNameSuffix(), container));
		if (renderer.isSendFolderThumbnails() || container instanceof DVDISOFile) {
			for (DLNAImageResElement resElement : getThumbnailResElements(container, mediaType)) {
				result.addResource(getImageRes(container, resElement));
				// Offering AlbumArt here breaks the standard, but some renderers
				// need it
				if (renderer.needAlbumArtHack() || result instanceof MusicAlbum) {
					AlbumArtURI albumArtURI = getAlbumArtURI(container, resElement);
					if (albumArtURI != null) {
						result.addProperty(albumArtURI);
					}
				}
			}
		} else if (result instanceof MusicAlbum) {
			for (DLNAImageResElement resElement : getThumbnailResElements(container, mediaType)) {
				AlbumArtURI albumArtURI = getAlbumArtURI(container, resElement);
				if (albumArtURI != null) {
					result.addProperty(albumArtURI);
				}
			}
		}
		return result;
	}

	public static final Item getItem(StoreItem item, String filter) {
		final Renderer renderer = item.getDefaultRenderer();
		final boolean isTrancoded = item.isTranscoded();
		final MediaInfo mediaInfo = item.getMediaInfo();
		final MediaStatus mediaStatus = item.getMediaStatus();
		final MediaSubtitle mediaSubtitle = item.getMediaSubtitle();
		final Format format = item.getFormat();
		final MediaType mediaType = mediaInfo != null ? mediaInfo.getMediaType() : MediaType.UNKNOWN;
		final EncodingFormat encodingFormat = isTrancoded ? item.getTranscodingSettings().getEncodingFormat() : null;
		boolean subsAreValidForStreaming = false;
		boolean xbox360 = renderer.isXbox360();
		Item result;
		if (item.getPrimaryResource() != null && mediaInfo != null && !mediaInfo.isSecondaryFormatValid()) {
			result = new Item();
		} else if (mediaType == MediaType.IMAGE || mediaType == MediaType.UNKNOWN && format != null && format.isImage()) {
			result = new Photo();
		} else if (mediaType == MediaType.AUDIO || mediaType == MediaType.UNKNOWN && format != null && format.isAudio()) {
			result = new MusicTrack();
		} else if (mediaInfo != null && mediaInfo.hasVideoMetadata() && (mediaInfo.getVideoMetadata().isTvEpisode() || mediaInfo.getVideoMetadata().getYear() != null)) {
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
					!isTrancoded ||
					renderer.streamSubsForTranscodedVideo()
				) &&
				mediaSubtitle.isExternal() &&
				renderer.isExternalSubtitlesFormatSupported(mediaSubtitle, item)
			) {
				subsAreValidForStreaming = true;
				LOGGER.trace("External subtitles \"{}\" can be streamed to {}", mediaSubtitle.getName(), renderer);
			} else if (LOGGER.isTraceEnabled()) {
				if (renderer.getUmsConfiguration().isDisableSubtitles()) {
					LOGGER.trace("Subtitles are disabled");
				} else if (mediaSubtitle.isEmbedded()) {
					LOGGER.trace("Subtitles track {} cannot be streamed because it is internal/embedded", mediaSubtitle.getId());
				} else if (isTrancoded && !renderer.streamSubsForTranscodedVideo()) {
					LOGGER.trace("Subtitles \"{}\" aren't supported while transcoding to {}", mediaSubtitle.getName(), renderer);
				} else {
					LOGGER.trace("Subtitles \"{}\" aren't valid for streaming to {}", mediaSubtitle.getName(), renderer);
				}
			}
		}

		String resourceId = item.getResourceId();
		if (xbox360) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}
		if (renderer.needVersionedObjectId()) {
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(item.getLongId());
			if (updateId != null) {
				resourceId += "#" + updateId;
			}
		}
		result.setId(resourceId);

		resourceId = item.getParentId();
		if (xbox360 && item.getFakeParentId() == null) {
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
			title = item.getDisplayName(false);
		} else {
			title = renderer.getUseSameExtension(item.getDisplayName(false));
		}
		if (
			!renderer.isThumbnails() &&
			item instanceof RealFile realfile &&
			realfile.isFullyPlayedMark()
		) {
			title = FullyPlayed.addFullyPlayedNamePrefix(title, item);
		}

		title = item.resumeStr(title);
		result.setTitle(renderer.getDcTitle(title, item.getDisplayNameSuffix(), item));

		if (renderer.isSamsung() && item instanceof RealFile && item.getMediaStatus() != null) {
			LOGGER.debug("Setting bookmark for {} => {}", title, item.getMediaStatus().getBookmark());
			result.addProperty(new SEC.DcmInfo(String.format("CREATIONDATE=0,FOLDER=%s,BM=%d", title, item.getMediaStatus().getBookmark())));
		}

		if (audioMetadata != null && renderer.isSendDateMetadataYearForAudioTags() && audioMetadata.getYear() > 1000 && result instanceof MusicTrack musicTrack) {
			musicTrack.setDate(Integer.toString(audioMetadata.getYear()));
		} else if (item.getLastModified() > 0 && renderer.isSendDateMetadata()) {
			//hacked ?
			result.getProperties().set(new DC.Date(formatDate(new Date(item.getLastModified()))));
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
			if (videoMetadata.isTvEpisode()) {
				if (videoMetadata.getTvSeason() != null) {
					movie.setEpisodeSeason(UnsignedInteger.valueOf(videoMetadata.getTvSeason()));
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvEpisodeNumber())) {
					movie.setEpisodeNumber(UnsignedInteger.valueOf(videoMetadata.getTvEpisodeNumber()));
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvSeriesTitle())) {
					movie.setSeriesTitle(videoMetadata.getTvSeriesTitle());
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvEpisodeName())) {
					movie.setProgramTitle(videoMetadata.getTvEpisodeName());
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
			int indexCount = 1;
			if (renderer.isDLNALocalizationRequired()) {
				indexCount = DlnaHelper.getDlnaLocalesCount();
			}

			for (int c = 0; c < indexCount; c++) {
				Res res = new Res();
				DlnaProtocolInfo protocolInfo = new DlnaProtocolInfo();
				protocolInfo.setProtocol(Protocol.HTTP_GET);
				protocolInfo.setContentFormat(item.getRendererMimeType());
				if (renderer.isSendDLNAOrgFlags()) {
					String additionalInfo = DlnaHelper.getDlnaAdditionalInfo(item, c);
					protocolInfo.setAdditionalInfo(additionalInfo);
				}
				res.setProtocolInfo(protocolInfo);
				if (subsAreValidForStreaming && mediaSubtitle != null && renderer.offerSubtitlesByProtocolInfo() && !renderer.useClosedCaption()) {
					res.getDependentProperties().add(new PV.SubtitleFileType(mediaSubtitle.getType().getExtension().toUpperCase()));
					res.getDependentProperties().add(new PV.SubtitleFileUri(item.getSubsURL(mediaSubtitle)));
				}
				if (item.getRendererMimeType().toLowerCase().startsWith("audio") || item.getRendererMimeType().toLowerCase().startsWith("video")) {
					NetworkInterfaceAssociation ia = NetworkConfiguration.getNetworkInterfaceAssociationFromConfig();
					if (ia != null) {
						int port = PMS.getConfiguration().getMediaServerPort();
						String hostname = ia.getAddr().getHostAddress();
						res.getDependentProperties().add(new Res.ImportUri(URI.create(String.format("http://%s:%d/import?id=%s", hostname, port, item.getId()))));
					}
				}

				if (format != null && format.isVideo() && mediaInfo != null && mediaInfo.isMediaParsed()) {
					MediaVideo defaultVideoTrack = mediaInfo.getDefaultVideoTrack();
					MediaAudio defaultAudioTrack = mediaInfo.getDefaultAudioTrack();
					long transcodedSize = renderer.getTranscodedSize();
					if (!isTrancoded) {
						res.setSize(mediaInfo.getSize());
					} else if (transcodedSize != 0) {
						res.setSize(transcodedSize);
					}

					if (mediaInfo.getDuration() != null) {
						if (item.isResume()) {
							long offset = item.getResume().getTimeOffset() / 1000;
							double duration = mediaInfo.getDuration() - offset;
							res.setDuration(StringUtil.formatDLNADuration(duration));
						} else if (item.getSplitRange().isEndLimitAvailable()) {
							res.setDuration(StringUtil.formatDLNADuration(item.getSplitRange().getDuration()));
						} else {
							res.setDuration(mediaInfo.getDurationString());
						}
					}

					if (defaultVideoTrack != null && defaultVideoTrack.getResolution() != null) {
						if (isTrancoded && (renderer.isKeepAspectRatio() || renderer.isKeepAspectRatioTranscoding())) {
							res.setResolution(item.getResolutionForKeepAR(defaultVideoTrack.getWidth(), defaultVideoTrack.getHeight()));
						} else {
							res.setResolution(defaultVideoTrack.getResolution());
						}
					}

					if (mediaInfo.getFrameRate() != null) {
						res.setFramerate(mediaInfo.getFrameRate().toString());
					}

					res.setBitrate(mediaInfo.getRealVideoBitrate());

					if (defaultAudioTrack != null) {
						if (defaultAudioTrack.getNumberOfChannels() > 0) {
							if (!isTrancoded) {
								res.setNrAudioChannels(defaultAudioTrack.getNumberOfChannels());
							} else {
								res.setNrAudioChannels(renderer.getUmsConfiguration().getAudioChannelCount());
							}
						}

						if (defaultAudioTrack.getSampleRate() > 1) {
							res.setSampleFrequency(defaultAudioTrack.getSampleRate());
						}
					}
					if (defaultVideoTrack != null && defaultVideoTrack.getBitDepth() > 0) {
						res.setColorDepth(defaultVideoTrack.getBitDepth());
					}
				} else if (format != null && format.isImage()) {
					if (mediaInfo != null && mediaInfo.isMediaParsed()) {
						res.setSize(mediaInfo.getSize());
						if (mediaInfo.getImageInfo() != null && mediaInfo.getImageInfo().getResolution() != null) {
							res.setResolution(mediaInfo.getImageInfo().getResolution());
						}
					} else {
						res.setSize(item.length());
					}
				} else if (format != null && format.isAudio()) {
					if (mediaInfo != null && mediaInfo.isMediaParsed()) {
						MediaAudio defaultAudioTrack = mediaInfo.getDefaultAudioTrack();
						if (mediaInfo.getBitRate() > 0) {
							res.setBitrate(mediaInfo.getBitRate());
						}
						if (mediaInfo.getDuration() != null && mediaInfo.getDuration() != 0.0) {
							res.setDuration(StringUtil.formatDLNADuration(mediaInfo.getDuration()));
						}

						int transcodeFrequency = -1;
						int transcodeNumberOfChannels = -1;
						if (defaultAudioTrack != null) {
							if (!isTrancoded) {
								if (defaultAudioTrack.getSampleRate() > 1) {
									res.setSampleFrequency(defaultAudioTrack.getSampleRate());
								}
								if (defaultAudioTrack.getNumberOfChannels() > 0) {
									res.setNrAudioChannels(defaultAudioTrack.getNumberOfChannels());
								}
							} else {
								if (renderer.getUmsConfiguration().isAudioResample()) {
									transcodeFrequency = renderer.isTranscodeAudioTo441() ? 44100 : 48000;
									transcodeNumberOfChannels = 2;
								} else {
									transcodeFrequency = defaultAudioTrack.getSampleRate();
									transcodeNumberOfChannels = defaultAudioTrack.getNumberOfChannels();
								}
								if (transcodeFrequency > 0) {
									res.setSampleFrequency(transcodeFrequency);
								}
								if (transcodeNumberOfChannels > 0) {
									res.setNrAudioChannels(transcodeNumberOfChannels);
								}
							}
							res.setBitsPerSample(defaultAudioTrack.getBitDepth());
						}

						if (!isTrancoded) {
							if (mediaInfo.getSize() != 0) {
								res.setSize(mediaInfo.getSize());
							}
						} else {
							// Calculate WAV size
							if (defaultAudioTrack != null && mediaInfo.getDurationInSeconds() > 0.0 && transcodeFrequency > 0 &&
								transcodeNumberOfChannels > 0) {
								int finalSize = (int) (mediaInfo.getDurationInSeconds() * transcodeFrequency * 2 * transcodeNumberOfChannels);
								LOGGER.trace("Calculated transcoded size for {}: {}", item.getFileName(), finalSize);
								res.setSize(finalSize);
							} else if (mediaInfo.getSize() > 0) {
								LOGGER.trace("Could not calculate transcoded size for {}, using file size: {}", item.getFileName(),
									mediaInfo.getSize());
								res.setSize(mediaInfo.getSize());
							}
						}
					} else {
						res.setSize(item.length());
					}
				} else {
					res.setSize(StoreResource.TRANS_SIZE);
					res.setDuration("09:59:59");
					res.setBitrate(1000000);
				}

				// Add transcoded format extension to the output stream URL.
				String transcodedExtension = "";
				if (encodingFormat != null && mediaInfo != null) {
					// Note: Can't use instanceof below because the audio
					// classes inherit the corresponding video class
					if (mediaInfo.isVideo()) {
						if (renderer.getCustomFFmpegOptions().contains("-f avi")) {
							transcodedExtension = "_transcoded_to.avi";
						} else if (renderer.getCustomFFmpegOptions().contains("-f flv")) {
							transcodedExtension = "_transcoded_to.flv";
						} else if (renderer.getCustomFFmpegOptions().contains("-f matroska")) {
							transcodedExtension = "_transcoded_to.mkv";
						} else if (renderer.getCustomFFmpegOptions().contains("-f mov")) {
							transcodedExtension = "_transcoded_to.mov";
						} else if (renderer.getCustomFFmpegOptions().contains("-f webm")) {
							transcodedExtension = "_transcoded_to.webm";
						} else if (encodingFormat.isTranscodeToHLS()) {
							transcodedExtension = "_transcoded_to.m3u8";
						} else if (encodingFormat.isTranscodeToMPEGTS()) {
							transcodedExtension = "_transcoded_to.ts";
						} else if (encodingFormat.isTranscodeToMP4()) {
							transcodedExtension = "_transcoded_to.mp4";
						} else if (encodingFormat.isTranscodeToWMV() && !xbox360) {
							transcodedExtension = "_transcoded_to.wmv";
						} else {
							transcodedExtension = "_transcoded_to.mpg";
						}
					} else if (mediaInfo.isAudio()) {
						if (renderer.getCustomFFmpegAudioOptions().contains("-f mp3")) {
							transcodedExtension = "_transcoded_to.mp3";
						} else if (renderer.getCustomFFmpegAudioOptions().contains("-f wav")) {
							transcodedExtension = "_transcoded_to.wav";
						} else if (renderer.getCustomFFmpegAudioOptions().contains("-f s16be")) {
							transcodedExtension = "_transcoded_to.pcm";
						} else if (encodingFormat.isTranscodeToMP3()) {
							transcodedExtension = "_transcoded_to.mp3";
						} else if (encodingFormat.isTranscodeToWAV()) {
							transcodedExtension = "_transcoded_to.wav";
						} else {
							transcodedExtension = "_transcoded_to.pcm";
						}
					}
				}

				res.setValue(URI.create(item.getMediaURL() + transcodedExtension));
				result.addResource(res);
			}

			// DESC Metadata support: add ability for control point to identify
			// songs by MusicBrainz TrackID or audiotrack-id
			if (mediaInfo != null && audioMetadata != null && mediaInfo.isAudio()) {
				// TODO add real namespace
				Desc desc = new Desc("http://ums/tags");
				desc.setId("2");
				desc.setType("ums-tags");
				desc.addMetadata("musicbrainztrackid", audioMetadata.getMbidTrack());
				desc.addMetadata("musicbrainzreleaseid", audioMetadata.getMbidRecord());
				desc.addMetadata("audiotrackid", Integer.toString(audioMetadata.getAudiotrackId()));
				if (audioMetadata.getDisc() > 0) {
					desc.addMetadata("numberOfThisDisc", Integer.toString(audioMetadata.getDisc()));
				}
				if (audioMetadata.getRating() != null) {
					desc.addMetadata("rating", Integer.toString(audioMetadata.getRating()));
				}
				result.addDescription(desc);
			}
			for (DLNAImageResElement resElement : getThumbnailResElements(item, mediaType)) {
				result.addResource(getImageRes(item, resElement));
				if (renderer.needAlbumArtHack()) {
					// Offering AlbumArt here breaks the standard, but some renderers
					// need it
					AlbumArtURI albumArtURI = getAlbumArtURI(item, resElement);
					if (albumArtURI != null) {
						result.addProperty(albumArtURI);
					}
				}
			}
		} else {
			for (DLNAImageResElement resElement : getImageResElements(item)) {
				result.addResource(getImageRes(item, resElement));
				// Offering AlbumArt here breaks the standard, but some renderers
				// need it
				if (renderer.needAlbumArtHack()) {
					AlbumArtURI albumArtURI = getAlbumArtURI(item, resElement);
					if (albumArtURI != null) {
						result.addProperty(albumArtURI);
					}
				}
			}
		}
		return result;
	}

	private static List<DLNAImageResElement> getImageResElements(StoreResource resource) {
		/*
		 * There's no technical difference between the image itself and the
		 * thumbnail for an object.item.imageItem, they are all simply listed as
		 * <res> entries. To UMS there is a difference since the thumbnail is
		 * cached while the image itself is not. The idea here is therefore to
		 * offer any size smaller than or equal to the cached thumbnail using
		 * the cached thumbnail as the source, and offer anything bigger using
		 * the image itself as the source.
		 *
		 * If the thumbnail isn't parsed yet, we don't know the size of the
		 * thumbnail. In those situations we simply use the thumbnail for the
		 * _TN entries and the image for all others.
		 */
		final Renderer renderer = resource.getDefaultRenderer();
		final MediaInfo mediaInfo = resource.getMediaInfo();
		ImageInfo imageInfo = mediaInfo != null ? mediaInfo.getImageInfo() : null;
		ImageInfo thumbnailImageInf = null;
		if (resource.getThumbnailImageInfo() != null) {
			thumbnailImageInf = resource.getThumbnailImageInfo();
		} else if (mediaInfo != null && mediaInfo.getThumbnail() != null && mediaInfo.getThumbnail().getImageInfo() != null) {
			thumbnailImageInf = mediaInfo.getThumbnail().getImageInfo();
		}

		// Only include GIF elements if the source is a GIF and it's supported
		// by the renderer.
		boolean includeGIF = imageInfo != null && imageInfo.getFormat() == ImageFormat.GIF &&
			DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.GIF_LRG, renderer);

		// Add elements in any order, it's sorted by priority later
		List<DLNAImageResElement> resElements = new ArrayList<>();

		// Always offer JPEG_TN as per DLNA standard
		resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_TN, thumbnailImageInf != null ? thumbnailImageInf : imageInfo, true));
		if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_TN, renderer)) {
			resElements
				.add(new DLNAImageResElement(DLNAImageProfile.PNG_TN, thumbnailImageInf != null ? thumbnailImageInf : imageInfo, true));
		}
		if (imageInfo != null) {
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_RES_H_V, renderer) && imageInfo.getWidth() > 0 &&
				imageInfo.getHeight() > 0) {
				// Offer the exact resolution as JPEG_RES_H_V
				DLNAImageProfile exactResolution = DLNAImageProfile.createJPEG_RES_H_V(imageInfo.getWidth(), imageInfo.getHeight());
				resElements.add(
					new DLNAImageResElement(exactResolution, imageInfo, exactResolution.useThumbnailSource(imageInfo, thumbnailImageInf)));
			}
			// Always offer JPEG_SM for images as per DLNA standard
			resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_SM, imageInfo,
				DLNAImageProfile.JPEG_SM.useThumbnailSource(imageInfo, thumbnailImageInf)));
			if (!DLNAImageProfile.PNG_TN.isResolutionCorrect(imageInfo)) {
				if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
					resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_LRG, imageInfo,
						DLNAImageProfile.PNG_LRG.useThumbnailSource(imageInfo, thumbnailImageInf)));
				}
				if (includeGIF) {
					resElements.add(new DLNAImageResElement(DLNAImageProfile.GIF_LRG, imageInfo,
						DLNAImageProfile.GIF_LRG.useThumbnailSource(imageInfo, thumbnailImageInf)));
				}
				if (!DLNAImageProfile.JPEG_SM.isResolutionCorrect(imageInfo)) {
					if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_MED, renderer)) {
						resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_MED, imageInfo,
							DLNAImageProfile.JPEG_MED.useThumbnailSource(imageInfo, thumbnailImageInf)));
					}
					if (!DLNAImageProfile.JPEG_MED.isResolutionCorrect(imageInfo) &&
						(DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer))
					) {
						resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_LRG, imageInfo,
							DLNAImageProfile.JPEG_LRG.useThumbnailSource(imageInfo, thumbnailImageInf)));
					}
				}
			}
		} else {
			// This shouldn't normally be the case, parsing must have failed or
			// isn't finished yet so we just make a generic offer.

			// Always offer JPEG_SM for images as per DLNA standard
			resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_SM, null, false));
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_LRG, null, false));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_LRG, null, false));
			}
			LOGGER.debug("Warning: Image \"{}\" wasn't parsed when DIDL-Lite was generated", resource.getName());
		}

		// Sort the elements by priority
		Collections.sort(resElements, DLNAImageResElement.getComparator(imageInfo != null ? imageInfo.getFormat() : ImageFormat.JPEG));
		return resElements;
	}

	private static List<DLNAImageResElement> getThumbnailResElements(StoreResource resource, MediaType mediaType) {

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
		List<DLNAImageResElement> resElements = new ArrayList<>();
		if (MediaType.IMAGE != mediaType) {
			ImageInfo imageInfo = null;
			if (resource.getThumbnailImageInfo() != null) {
				imageInfo = resource.getThumbnailImageInfo();
			} else if (mediaInfo != null && mediaInfo.getThumbnail() != null && mediaInfo.getThumbnail().getImageInfo() != null) {
				imageInfo = mediaInfo.getThumbnail().getImageInfo();
			}

			// Only include GIF elements if the source is a GIF and it's
			// supported by the renderer.
			boolean includeGIF = imageInfo != null && imageInfo.getFormat() == ImageFormat.GIF &&
				DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.GIF_LRG, renderer);

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
		}
		return resElements;
	}

	private static Res getImageRes(StoreResource resource, DLNAImageResElement resElement) {
		if (resource == null) {
			throw new NullPointerException("resource cannot be null");
		}
		if (resElement == null) {
			throw new NullPointerException("resElement cannot be null");
		}
		if (!resElement.isResolutionKnown() && DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile())) {
			throw new IllegalArgumentException("Resolution cannot be unknown for DLNAImageProfile.JPEG_RES_H_V");
		}
		String url = null;
		if (resElement.isThumbnail()) {
			url = resource.getThumbnailURL(resElement.getProfile());
		} else if (resource instanceof StoreItem item) {
			url = item.getMediaURL((DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile()) ?
				"JPEG_RES" + resElement.getWidth() + "x" + resElement.getHeight() :
				resElement.getProfile().toString()) + "_");
		}
		if (StringUtils.isNotBlank(url)) {
			String ciFlag;
			/*
			 * Some Panasonic TV's can't handle if the thumbnails have the CI
			 * flag set to 0 while the main item doesn't have a CI flag.
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
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
			if (updateId != null && url != null) {
				if (url.contains("?")) {
					url += "&update=" + updateId;
				} else {
					url += "?update=" + updateId;
				}
			}
			res.setValue(URI.create(url));
			return res;
		}
		return null;
	}

	private static AlbumArtURI getAlbumArtURI(StoreResource resource, DLNAImageResElement resElement) {
		DLNAImageProfile imageProfile = resElement.getProfile();
		String rendererProfile = resource.getDefaultRenderer().getAlbumArtProfile();
		if (StringUtils.isNotBlank(rendererProfile) && !rendererProfile.equalsIgnoreCase(imageProfile.toString())) {
			return null;
		}
		switch (imageProfile.toInt()) {
			case DLNAImageProfile.GIF_LRG_INT,
				DLNAImageProfile.JPEG_SM_INT,
				DLNAImageProfile.JPEG_TN_INT,
				DLNAImageProfile.PNG_LRG_INT,
				DLNAImageProfile.PNG_TN_INT
				-> {
					return getAlbumArtURI(resource, imageProfile);
				}
		}
		return null;
	}

	private static AlbumArtURI getAlbumArtURI(StoreResource resource, DLNAImageProfile imageProfile) {
		String albumArtURL = resource.getThumbnailURL(imageProfile);
		if (StringUtils.isNotBlank(albumArtURL)) {
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
			if (updateId != null && albumArtURL != null) {
				if (albumArtURL.contains("?")) {
					albumArtURL += "&update=" + updateId;
				} else {
					albumArtURL += "?update=" + updateId;
				}
			}
			UPNP.AlbumArtURI albumArtURI = new UPNP.AlbumArtURI(URI.create(albumArtURL));
			albumArtURI.setProfileID(imageProfile.toString());
			return albumArtURI;
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
