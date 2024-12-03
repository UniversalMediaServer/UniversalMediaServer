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
package net.pms.dlna;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.pms.PMS;
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
import net.pms.network.mediaserver.HTTPXMLHelper;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreItem;
import net.pms.store.StoreResource;
import net.pms.store.container.DVDISOFile;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.container.VirtualFolderDbId;
import net.pms.store.item.RealFile;
import net.pms.util.FullyPlayed;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DidlHelper extends DlnaHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DidlHelper.class);
	private static final SimpleDateFormat DIDL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	/**
	 * This class is not meant to be instantiated.
	 */
	protected DidlHelper() {
	}

	public static final String getDidlResults(List<StoreResource> resultResources) {
		StringBuilder filesData = new StringBuilder();
		filesData.append(HTTPXMLHelper.DIDL_HEADER);
		for (StoreResource resource : resultResources) {
			filesData.append(getDidlString(resource));
		}
		filesData.append(HTTPXMLHelper.DIDL_FOOTER);
		return StringEscapeUtils.unescapeXml(filesData.toString());
	}

	/**
	 * Returns an XML (DIDL) representation of the DLNA node. It gives a
	 * complete representation of the item, with as many tags as available.
	 * Recommendations as per UPNP specification are followed where possible.
	 *
	 * @return String representing the item. An example would start like this:
	 *         {@code <container id="0$1" childCount="1" parentID="0" restricted
	 *         ="1">}
	 */
	public static final String getDidlString(StoreResource resource) {
		final Renderer renderer = resource.getDefaultRenderer();
		final MediaInfo mediaInfo = resource.getMediaInfo();
		final MediaStatus mediaStatus = resource.getMediaStatus();
		final StoreContainer container = resource instanceof StoreContainer storeContainer ? storeContainer : null;
		final StoreItem item = resource instanceof StoreItem storeItem ? storeItem : null;
		final EncodingFormat encodingFormat = item != null && item.isTranscoded() ? item.getTranscodingSettings().getEncodingFormat() : null;
		final Format format = item != null ? item.getFormat() : null;
		final MediaSubtitle mediaSubtitle = item != null ? item.getMediaSubtitle() : null;

		StringBuilder sb = new StringBuilder();
		boolean subsAreValidForStreaming = false;
		boolean xbox360 = renderer.isXbox360();
		if (item != null) {
			if (mediaInfo != null && mediaInfo.isVideo()) {
				if (
					!renderer.getUmsConfiguration().isDisableSubtitles() &&
					(
						!item.isTranscoded() ||
						renderer.streamSubsForTranscodedVideo()
					) &&
					mediaSubtitle != null &&
					mediaSubtitle.isExternal() &&
					renderer.isExternalSubtitlesFormatSupported(mediaSubtitle, item)
				) {
					subsAreValidForStreaming = true;
					LOGGER.trace("External subtitles \"{}\" can be streamed to {}", mediaSubtitle.getName(), renderer);
				} else if (mediaSubtitle != null && LOGGER.isTraceEnabled()) {
					if (renderer.getUmsConfiguration().isDisableSubtitles()) {
						LOGGER.trace("Subtitles are disabled");
					} else if (mediaSubtitle.isEmbedded()) {
						LOGGER.trace("Subtitles track {} cannot be streamed because it is internal/embedded", mediaSubtitle.getId());
					} else if (item.isTranscoded() && !renderer.streamSubsForTranscodedVideo()) {
						LOGGER.trace("Subtitles \"{}\" aren't supported while transcoding to {}", mediaSubtitle.getName(), renderer);
					} else {
						LOGGER.trace("Subtitles \"{}\" aren't valid for streaming to {}", mediaSubtitle.getName(), renderer);
					}
				}
			}

			openTag(sb, "item");
		} else {
			openTag(sb, "container");
		}

		String resourceId = resource.getResourceId();
		if (xbox360) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}

		if (item != null && renderer.needVersionedObjectId()) {
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(item.getLongId());
			if (updateId != null) {
				resourceId += "#" + updateId;
			}
		}

		addAttribute(sb, "id", resourceId);
		if (container != null) {
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
				addAttribute(sb, "childCount", 1);
			} else {
				addAttribute(sb, "childCount", container.childrenCount());
			}
		}

		resourceId = resource.getParentId();
		if (xbox360 && resource.getFakeParentId() == null) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual
			// folder ids.
			resourceId += "$";
		}

		addAttribute(sb, "parentID", resourceId);
		addAttribute(sb, "restricted", "1");
		endTag(sb);
		final MediaVideo defaultVideoTrack = mediaInfo != null ? mediaInfo.getDefaultVideoTrack() : null;
		final MediaAudio defaultAudioTrack = mediaInfo != null ? mediaInfo.getDefaultAudioTrack() : null;
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
		} else if (container != null || subsAreValidForStreaming) {
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

		if (item != null) {
			title = item.resumeStr(title);
		}
		addXMLTagAndAttribute(sb, "dc:title",
			encodeXML(renderer.getDcTitle(title, resource.getDisplayNameSuffix(), resource)));

		if (renderer.isSamsung() && resource instanceof RealFile) {
			addBookmark(resource, sb, renderer.getDcTitle(title, resource.getDisplayNameSuffix(), resource));
		}

		if (audioMetadata != null && renderer.isSendDateMetadataYearForAudioTags() && audioMetadata.getYear() > 1000) {
			addXMLTagAndAttribute(sb, "dc:date", Integer.toString(audioMetadata.getYear()));
		} else if (resource.getLastModified() > 0 && renderer.isSendDateMetadata()) {
			addXMLTagAndAttribute(sb, "dc:date", formatDate(new Date(resource.getLastModified())));
		}

		if (mediaInfo != null && audioMetadata != null) {
			if (StringUtils.isNotBlank(audioMetadata.getAlbum())) {
				addXMLTagAndAttribute(sb, "upnp:album", encodeXML(audioMetadata.getAlbum()));
			}

			// TODO maciekberry: check whether it makes sense to use Album
			// Artist
			if (StringUtils.isNotBlank(audioMetadata.getArtist())) {
				addXMLTagAndAttribute(sb, "upnp:artist", encodeXML(audioMetadata.getArtist()));
				addXMLTagAndAttribute(sb, "dc:creator", encodeXML(audioMetadata.getArtist()));
			}

			if (StringUtils.isNotBlank(audioMetadata.getComposer())) {
				addXMLTagAndAttributeWithRole(sb, "upnp:artist role=\"Composer\"", encodeXML(audioMetadata.getComposer()));
				addXMLTagAndAttributeWithRole(sb, "upnp:author role=\"Composer\"", encodeXML(audioMetadata.getComposer()));
				//FIXME : it break upnp standard (non existant)
				addXMLTagAndAttribute(sb, "upnp:composer", encodeXML(audioMetadata.getComposer()));
			}

			if (StringUtils.isNotBlank(audioMetadata.getConductor())) {
				addXMLTagAndAttributeWithRole(sb, "upnp:artist role=\"Conductor\"", encodeXML(audioMetadata.getConductor()));
				//FIXME : it break upnp standard (non existant)
				addXMLTagAndAttribute(sb, "upnp:conductor", encodeXML(audioMetadata.getConductor()));
			}

			if (StringUtils.isNotBlank(audioMetadata.getGenre())) {
				addXMLTagAndAttribute(sb, "upnp:genre", encodeXML(audioMetadata.getGenre()));
			}

			if (audioMetadata.getTrack() > 0) {
				addXMLTagAndAttribute(sb, "upnp:originalTrackNumber", "" + audioMetadata.getTrack());
			}

			if (audioMetadata.getRating() != null) {
				addXMLTagAndAttribute(sb, "upnp:rating", "" + audioMetadata.getRating());
			}
		}

		if (mediaInfo != null && mediaInfo.hasVideoMetadata()) {
			MediaVideoMetadata videoMetadata = mediaInfo.getVideoMetadata();
			if (videoMetadata.isTvEpisode()) {
				if (videoMetadata.getTvSeason() != null) {
					addXMLTagAndAttribute(sb, "upnp:episodeSeason", videoMetadata.getTvSeason());
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvEpisodeNumber())) {
					addXMLTagAndAttribute(sb, "upnp:episodeNumber", videoMetadata.getTvEpisodeNumberUnpadded());
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvSeriesTitle())) {
					addXMLTagAndAttribute(sb, "upnp:seriesTitle", encodeXML(videoMetadata.getTvSeriesTitle(null)));
				}
				if (StringUtils.isNotBlank(videoMetadata.getTvEpisodeName())) {
					addXMLTagAndAttribute(sb, "upnp:programTitle", encodeXML(videoMetadata.getTvEpisodeName(null)));
				}
			}
			if (mediaStatus != null) {
				addXMLTagAndAttribute(sb, "upnp:playbackCount", mediaStatus.getPlaybackCount());
				if (StringUtils.isNotBlank(mediaStatus.getLastPlaybackTime())) {
					addXMLTagAndAttribute(sb, "upnp:lastPlaybackTime", encodeXML(mediaStatus.getLastPlaybackTime()));
				}
				if (StringUtils.isNotBlank(mediaStatus.getLastPlaybackPositionForUPnP())) {
					addXMLTagAndAttribute(sb, "upnp:lastPlaybackPosition", encodeXML(mediaStatus.getLastPlaybackPositionForUPnP()));
				}
			}
		}

		MediaType mediaType = mediaInfo != null ? mediaInfo.getMediaType() : MediaType.UNKNOWN;
		if (item != null && mediaType == MediaType.IMAGE) {
			appendImage(item, sb);
		} else if (item != null) {
			int indexCount = 1;
			if (renderer.isDLNALocalizationRequired()) {
				indexCount = getDLNALocalesCount();
			}

			for (int c = 0; c < indexCount; c++) {
				openTag(sb, "res");
				addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
				String dlnaOrgPnFlags = getDlnaOrgPnFlags(item, c);
				String dlnaOrgFlags = "*";
				if (renderer.isSendDLNAOrgFlags()) {
					dlnaOrgFlags = (dlnaOrgPnFlags != null ? (dlnaOrgPnFlags + ";") : "") + getDlnaOrgOpFlags(item);
				}
				String tempString = "http-get:*:" + item.getRendererMimeType() + ":" + dlnaOrgFlags;
				addAttribute(sb, "protocolInfo", tempString);
				if (subsAreValidForStreaming && mediaSubtitle != null && renderer.offerSubtitlesByProtocolInfo() && !renderer.useClosedCaption()) {
					addAttribute(sb, "pv:subtitleFileType", mediaSubtitle.getType().getExtension().toUpperCase());
					addAttribute(sb, "pv:subtitleFileUri", resource.getSubsURL(mediaSubtitle));
				}
				if (item.getRendererMimeType().toLowerCase().startsWith("audio") || item.getRendererMimeType().toLowerCase().startsWith("video")) {
					NetworkInterfaceAssociation ia = NetworkConfiguration.getNetworkInterfaceAssociationFromConfig();
					if (ia != null) {
						int port = PMS.getConfiguration().getMediaServerPort();
						String hostname = ia.getAddr().getHostAddress();
						addAttribute(sb, "importUri", String.format("http://%s:%d/import?id=%s", hostname, port, resource.getId()));
					}
				}

				if (format != null && format.isVideo() && mediaInfo != null && mediaInfo.isMediaParsed()) {
					long transcodedSize = renderer.getTranscodedSize();
					if (!item.isTranscoded()) {
						addAttribute(sb, "size", mediaInfo.getSize());
					} else if (transcodedSize != 0) {
						addAttribute(sb, "size", transcodedSize);
					}

					if (mediaInfo.getDuration() != null) {
						if (item.isResume()) {
							long offset = item.getResume().getTimeOffset() / 1000;
							double duration = mediaInfo.getDuration() - offset;
							addAttribute(sb, "duration", StringUtil.formatDLNADuration(duration));
						} else if (item.getSplitRange().isEndLimitAvailable()) {
							addAttribute(sb, "duration", StringUtil.formatDLNADuration(item.getSplitRange().getDuration()));
						} else {
							addAttribute(sb, "duration", mediaInfo.getDurationString());
						}
					}

					if (defaultVideoTrack != null && defaultVideoTrack.getResolution() != null) {
						if (item.isTranscoded() && (renderer.isKeepAspectRatio() || renderer.isKeepAspectRatioTranscoding())) {
							addAttribute(sb, "resolution", item.getResolutionForKeepAR(defaultVideoTrack.getWidth(), defaultVideoTrack.getHeight()));
						} else {
							addAttribute(sb, "resolution", defaultVideoTrack.getResolution());
						}
					}

					if (mediaInfo.getFrameRate() != null) {
						addAttribute(sb, "framerate", mediaInfo.getFrameRate());
					}

					addAttribute(sb, "bitrate", mediaInfo.getRealVideoBitrate());

					if (defaultAudioTrack != null) {
						if (defaultAudioTrack.getNumberOfChannels() > 0) {
							if (!item.isTranscoded()) {
								addAttribute(sb, "nrAudioChannels", defaultAudioTrack.getNumberOfChannels());
							} else {
								addAttribute(sb, "nrAudioChannels", renderer.getUmsConfiguration().getAudioChannelCount());
							}
						}

						if (defaultAudioTrack.getSampleRate() > 1) {
							addAttribute(sb, "sampleFrequency", defaultAudioTrack.getSampleRate());
						}
					}
					if (defaultVideoTrack != null && defaultVideoTrack.getBitDepth() > 0) {
						addAttribute(sb, "colorDepth", defaultVideoTrack.getBitDepth());
					}
				} else if (format != null && format.isImage()) {
					if (mediaInfo != null && mediaInfo.isMediaParsed()) {
						addAttribute(sb, "size", mediaInfo.getSize());
						if (mediaInfo.getImageInfo() != null && mediaInfo.getImageInfo().getResolution() != null) {
							addAttribute(sb, "resolution", mediaInfo.getImageInfo().getResolution());
						}
					} else {
						addAttribute(sb, "size", resource.length());
					}
				} else if (format != null && format.isAudio()) {
					if (mediaInfo != null && mediaInfo.isMediaParsed()) {
						if (mediaInfo.getBitRate() > 0) {
							addAttribute(sb, "bitrate", mediaInfo.getBitRate());
						}
						if (mediaInfo.getDuration() != null && mediaInfo.getDuration() != 0.0) {
							addAttribute(sb, "duration", StringUtil.formatDLNADuration(mediaInfo.getDuration()));
						}

						int transcodeFrequency = -1;
						int transcodeNumberOfChannels = -1;
						if (defaultAudioTrack != null) {
							if (!item.isTranscoded()) {
								if (defaultAudioTrack.getSampleRate() > 1) {
									addAttribute(sb, "sampleFrequency", defaultAudioTrack.getSampleRate());
								}
								if (defaultAudioTrack.getNumberOfChannels() > 0) {
									addAttribute(sb, "nrAudioChannels", defaultAudioTrack.getNumberOfChannels());
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
									addAttribute(sb, "sampleFrequency", transcodeFrequency);
								}
								if (transcodeNumberOfChannels > 0) {
									addAttribute(sb, "nrAudioChannels", transcodeNumberOfChannels);
								}
							}
							addAttribute(sb, "bitsPerSample", defaultAudioTrack.getBitDepth());
						}

						if (!item.isTranscoded()) {
							if (mediaInfo.getSize() != 0) {
								addAttribute(sb, "size", mediaInfo.getSize());
							}
						} else {
							// Calculate WAV size
							if (defaultAudioTrack != null && mediaInfo.getDurationInSeconds() > 0.0 && transcodeFrequency > 0 &&
								transcodeNumberOfChannels > 0) {
								int finalSize = (int) (mediaInfo.getDurationInSeconds() * transcodeFrequency * 2 * transcodeNumberOfChannels);
								LOGGER.trace("Calculated transcoded size for {}: {}", resource.getFileName(), finalSize);
								addAttribute(sb, "size", finalSize);
							} else if (mediaInfo.getSize() > 0) {
								LOGGER.trace("Could not calculate transcoded size for {}, using file size: {}", resource.getFileName(),
									mediaInfo.getSize());
								addAttribute(sb, "size", mediaInfo.getSize());
							}
						}
					} else {
						addAttribute(sb, "size", resource.length());
					}
				} else {
					addAttribute(sb, "size", StoreResource.TRANS_SIZE);
					addAttribute(sb, "duration", "09:59:59");
					addAttribute(sb, "bitrate", "1000000");
				}

				endTag(sb);
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

				sb.append(item.getMediaURL()).append(transcodedExtension);
				closeTag(sb, "res");
			}

			// DESC Metadata support: add ability for control point to identify
			// songs by MusicBrainz TrackID or audiotrack-id
			if (mediaInfo != null && audioMetadata != null && mediaInfo.isAudio()) {
				openTag(sb, "desc");
				addAttribute(sb, "id", "2");
				// TODO add real namespace
				addAttribute(sb, "nameSpace", "http://ums/tags");
				addAttribute(sb, "type", "ums-tags");
				endTag(sb);
				addXMLTagAndAttribute(sb, "musicbrainztrackid", audioMetadata.getMbidTrack());
				addXMLTagAndAttribute(sb, "musicbrainzreleaseid", audioMetadata.getMbidRecord());
				addXMLTagAndAttribute(sb, "audiotrackid", Integer.toString(audioMetadata.getAudiotrackId()));
				if (audioMetadata.getDisc() > 0) {
					addXMLTagAndAttribute(sb, "numberOfThisDisc", Integer.toString(audioMetadata.getDisc()));
				}
				if (audioMetadata.getRating() != null) {
					addXMLTagAndAttribute(sb, "rating", Integer.toString(audioMetadata.getRating()));
				}
				closeTag(sb, "desc");
			}

			if (subsAreValidForStreaming && mediaSubtitle != null) {
				String subsURL = resource.getSubsURL(mediaSubtitle);
				if (renderer.useClosedCaption()) {
					openTag(sb, "sec:CaptionInfoEx");
					addAttribute(sb, "sec:type", "srt");
					endTag(sb);
					sb.append(subsURL);
					closeTag(sb, "sec:CaptionInfoEx");
					LOGGER.trace("Network debugger: sec:CaptionInfoEx: sec:type=srt " + subsURL);
				} else if (renderer.offerSubtitlesAsResource()) {
					openTag(sb, "res");
					String subtitlesFormat = mediaSubtitle.getType().getExtension();
					if (StringUtils.isBlank(subtitlesFormat)) {
						subtitlesFormat = "plain";
					}

					addAttribute(sb, "protocolInfo", "http-get:*:text/" + subtitlesFormat + ":*");
					endTag(sb);
					sb.append(subsURL);
					closeTag(sb, "res");
					LOGGER.trace("Network debugger: http-get:*:text/" + subtitlesFormat + ":*" + subsURL);
				}
			}
		}

		String uclass;
		if (resource.getPrimaryResource() != null && mediaInfo != null && !mediaInfo.isSecondaryFormatValid()) {
			uclass = "dummy";
		} else if (container != null) {
			if (resource instanceof PlaylistFolder) {
				uclass = "object.container.playlistContainer";
			} else if (resource instanceof VirtualFolderDbId virtualFolderDbId) {
				uclass = virtualFolderDbId.getMediaTypeUclass();
			} else {
				//FIXME : it break upnp standard
				//object.container.storageFolder require the upnp:storageUsed property set
				//so either set it, or use the object.container class
				uclass = "object.container.storageFolder";
			}
			if (xbox360 && resource.getFakeParentId() != null) {
				uclass = switch (resource.getFakeParentId()) {
					case "7" -> "object.container.album.musicAlbum";
					case "6" -> "object.container.person.musicArtist";
					case "5" -> "object.container.genre.musicGenre";
					case "F" -> "object.container.playlistContainer";
					default -> uclass;
				};
			}
		} else if (mediaType == MediaType.IMAGE || mediaType == MediaType.UNKNOWN && format != null && format.isImage()) {
			uclass = "object.item.imageItem.photo";
		} else if (mediaType == MediaType.AUDIO || mediaType == MediaType.UNKNOWN && format != null && format.isAudio()) {
			uclass = "object.item.audioItem.musicTrack";
		} else if (mediaInfo != null && mediaInfo.hasVideoMetadata() && (mediaInfo.getVideoMetadata().isTvEpisode() || mediaInfo.getVideoMetadata().getYear() != null)) {
			// videoItem.movie is used for TV episodes and movies
			uclass = "object.item.videoItem.movie";
		} else {
			/**
			 * videoItem is used for recorded videos but does not support
			 * properties like episodeNumber, seriesTitle, etc.
			 *
			 * @see page 251 of
			 *      http://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service.pdf
			 */
			uclass = "object.item.videoItem";
		}

		if (mediaType != MediaType.IMAGE && (container == null || uclass.startsWith("object.container.album") || renderer.isSendFolderThumbnails() || resource instanceof DVDISOFile)) {
			appendThumbnail(resource, sb, mediaType, uclass.startsWith("object.container.album"));
		}

		addXMLTagAndAttribute(sb, "upnp:class", uclass);
		if (item != null) {
			closeTag(sb, "item");
		} else {
			closeTag(sb, "container");
		}

		return sb.toString();
	}

	/**
	 * Generate and append image and thumbnail {@code res} and
	 * {@code upnp:albumArtURI} entries for the image.
	 *
	 * @param sb The {@link StringBuilder} to append the elements to.
	 */
	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	private static void appendImage(StoreItem item, StringBuilder sb) {
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
		final Renderer renderer = item.getDefaultRenderer();
		final MediaInfo mediaInfo = item.getMediaInfo();

		ImageInfo imageInfo = mediaInfo != null ? mediaInfo.getImageInfo() : null;
		ImageInfo thumbnailImageInf = null;
		if (item.getThumbnailImageInfo() != null) {
			thumbnailImageInf = item.getThumbnailImageInfo();
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
			LOGGER.debug("Warning: Image \"{}\" wasn't parsed when DIDL-Lite was generated", item.getName());
		}

		// Sort the elements by priority
		Collections.sort(resElements, DLNAImageResElement.getComparator(imageInfo != null ? imageInfo.getFormat() : ImageFormat.JPEG));

		for (DLNAImageResElement resElement : resElements) {
			addImageResource(item, sb, resElement);
		}

		for (DLNAImageResElement resElement : resElements) {
			// Offering AlbumArt here breaks the standard, but some renderers
			// need it
			switch (resElement.getProfile().toInt()) {
				case DLNAImageProfile.GIF_LRG_INT,
					DLNAImageProfile.JPEG_SM_INT,
					DLNAImageProfile.JPEG_TN_INT,
					DLNAImageProfile.PNG_LRG_INT,
					DLNAImageProfile.PNG_TN_INT
					-> addAlbumArt(item, sb, resElement.getProfile());
			}
		}
	}

	/**
	 * Generate and append the thumbnail {@code res} and
	 * {@code upnp:albumArtURI} entries for the thumbnail.
	 *
	 * @param sb the {@link StringBuilder} to append the response to.
	 * @param mediaType the {@link MediaType} of this {@link StoreResource}.
	 */
	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	private static void appendThumbnail(StoreResource resource, StringBuilder sb, MediaType mediaType, boolean isAlbum) {

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
				addImageResource(resource, sb, resElement);
			}

			if (isAlbum || renderer.needAlbumArtHack()) {
				for (DLNAImageResElement resElement : resElements) {
					// Offering AlbumArt for object other than Album container
					// breaks the standard, but some renderers need it.
					switch (resElement.getProfile().toInt()) {
						case DLNAImageProfile.GIF_LRG_INT, DLNAImageProfile.JPEG_SM_INT, DLNAImageProfile.JPEG_TN_INT, DLNAImageProfile.PNG_LRG_INT, DLNAImageProfile.PNG_TN_INT -> addAlbumArt(resource, sb, resElement.getProfile());
					}
				}
			}
		}
	}

	private static void addImageResource(StoreResource resource, StringBuilder sb, DLNAImageResElement resElement) {
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
			openTag(sb, "res");
			if (resElement.getSize() != null && resElement.getSize() > 0) {
				addAttribute(sb, "size", resElement.getSize());
			}
			if (resElement.isResolutionKnown()) {
				addAttribute(sb, "resolution", Integer.toString(resElement.getWidth()) + "x" + Integer.toString(resElement.getHeight()));
			}

			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			addAttribute(sb, "protocolInfo", "http-get:*:" + resElement.getProfile().getMimeType() + ":DLNA.ORG_PN=" +
				resElement.getProfile() + ciFlag + ";DLNA.ORG_FLAGS=00900000000000000000000000000000");
			endTag(sb);
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
			if (updateId != null && url != null) {
				if (url.contains("?")) {
					url += "&update=" + updateId;
				} else {
					url += "?update=" + updateId;
				}
			}
			sb.append(url);
			closeTag(sb, "res");
		}
	}

	private static void addAlbumArt(StoreResource resource, StringBuilder sb, DLNAImageProfile thumbnailProfile) {
		String rendererProfile = resource.getDefaultRenderer().getAlbumArtProfile();
		if (StringUtils.isNotBlank(rendererProfile) && !rendererProfile.equalsIgnoreCase(thumbnailProfile.toString())) {
			return;
		}
		String albumArtURL = resource.getThumbnailURL(thumbnailProfile);
		if (StringUtils.isNotBlank(albumArtURL)) {
			String updateId = MediaStoreIds.getObjectUpdateIdAsString(resource.getLongId());
			if (updateId != null && albumArtURL != null) {
				if (albumArtURL.contains("?")) {
					albumArtURL += "&update=" + updateId;
				} else {
					albumArtURL += "?update=" + updateId;
				}
			}
			openTag(sb, "upnp:albumArtURI");
			addAttribute(sb, "dlna:profileID", thumbnailProfile);
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			endTag(sb);
			sb.append(albumArtURL);
			closeTag(sb, "upnp:albumArtURI");
		}
	}

	private static void addBookmark(StoreResource resource, StringBuilder sb, String title) {
		if (resource.getMediaStatus() != null) {
			LOGGER.debug("Setting bookmark for {} => {}", title, resource.getMediaStatus().getBookmark());
			addXMLTagAndAttribute(sb, "sec:dcmInfo", encodeXML(String.format("CREATIONDATE=0,FOLDER=%s,BM=%d", title, resource.getMediaStatus().getBookmark())));
		}
	}

	private static String formatDate(Date data) {
		synchronized (DIDL_DATE_FORMAT) {
			return DIDL_DATE_FORMAT.format(data);
		}
	}

	/**
	 * Appends "&lt;<u>tag</u> " to the StringBuilder. This is a typical
	 * HTML/DIDL/XML tag opening.
	 *
	 * @param sb String to append the tag beginning to.
	 * @param tag String that represents the tag
	 */
	private static void openTag(StringBuilder sb, String tag) {
		sb.append("&lt;");
		sb.append(tag);
	}

	/**
	 * Appends the closing symbol &gt; to the StringBuilder. This is a typical
	 * HTML/DIDL/XML tag closing.
	 *
	 * @param sb String to append the ending character of a tag.
	 */
	private static void endTag(StringBuilder sb) {
		sb.append("&gt;");
	}

	/**
	 * Appends "&lt;/<u>tag</u>&gt;" to the StringBuilder. This is a typical
	 * closing HTML/DIDL/XML tag.
	 *
	 * @param sb
	 * @param tag
	 */
	private static void closeTag(StringBuilder sb, String tag) {
		sb.append("&lt;/");
		sb.append(tag);
		sb.append("&gt;");
	}

	private static void addAttribute(StringBuilder sb, String attribute, Object value) {
		sb.append(' ');
		sb.append(attribute);
		sb.append("=\"");
		sb.append(value);
		sb.append("\"");
	}

	private static void addXMLTagAndAttribute(StringBuilder sb, String tag, Object value) {
		sb.append("&lt;");
		sb.append(tag);
		sb.append("&gt;");
		sb.append(value);
		sb.append("&lt;/");
		sb.append(tag);
		sb.append("&gt;");
	}

	private static void addXMLTagAndAttributeWithRole(StringBuilder sb, String tag, Object value) {
		String myTagWithoutRole = tag.split(" ")[0];
		sb.append("&lt;");
		sb.append(tag);
		sb.append("&gt;");
		sb.append(value);
		sb.append("&lt;/");
		sb.append(myTagWithoutRole);
		sb.append("&gt;");
	}

	/**
	 * Does double transformations between &<> characters and their XML
	 * representation with ampersands.
	 *
	 * @param s String to be encoded
	 * @return Encoded String
	 */
	private static String encodeXML(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		/*
		 * Skip encoding/escaping ' and " for compatibility with some renderers
		 * This might need to be made into a renderer option if some renderers
		 * require them to be encoded s = s.replace("\"", "&quot;"); s =
		 * s.replace("'", "&apos;");
		 */

		// The second encoding/escaping of & is not a bug, it's what effectively
		// adds the second layer of encoding/escaping
		s = s.replace("&", "&amp;");
		return s;
	}

}
