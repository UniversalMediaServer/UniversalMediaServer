package net.pms.network.mediaserver.jupnp.support.umsservice;

import java.util.Timer;
import java.util.TimerTask;
import jakarta.annotation.Nullable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import net.pms.PMS;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.network.mediaserver.jupnp.support.umsservice.impl.LikeMusic;
import net.pms.network.mediaserver.jupnp.support.umsservice.impl.RadioBrowserSearch;
import net.pms.network.mediaserver.jupnp.support.umsservice.impl.RatingBackupManager;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.store.MediaScanner;
import net.pms.store.StoreResource;
import net.pms.store.container.PlaylistFolder;
import net.pms.util.artistImageProvider.UmsArtistImageProvider;
import net.pms.store.container.audioaddict.AudioAddictPlaylistInputStream;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import de.sfuhrm.radiobrowser4j.Station;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

@UpnpService(
	serviceId = @UpnpServiceId("UmsExtendedServices"),
	serviceType = @UpnpServiceType(value = "UmsExtendedServices",
	version = 1))
@UpnpStateVariables({
	@UpnpStateVariable(name = "A_ARG_TYPE_ObjectID", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_MusicBrainzId", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_DiscogsId", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_MusicBrainzReleaseID", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_DiscogsReleaseID", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_AlbumLikedValue", sendEvents = false, datatype = "boolean"),
	@UpnpStateVariable(name = "A_ARG_TYPE_PreferEuropeanServer", sendEvents = false, datatype = "boolean"),
	@UpnpStateVariable(name = "A_ARG_TYPE_AudioAddictUser", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_AudioAddictPass", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_PlaylistLoop", sendEvents = false, datatype = "boolean"),
	@UpnpStateVariable(name = "A_ARG_TYPE_PlaylistId", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_NowPlaying", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Name", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_CountryCode", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Language", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Tag", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Offset", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Limit", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Kind", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Search", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_StationUuid", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Title", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_Result", sendEvents = false, datatype = "string")
	})
public class UmsExtendedServices {

	private static final Logger LOG = LoggerFactory.getLogger(UmsExtendedServices.class.getName());

	private LikeMusic likeMusic = new LikeMusic();

	private final Timer timer = new Timer("jupnp-umsConfigurationServices-readConfigTimer");
	private TimerTask readConfigTimer = null;

	@UpnpStateVariable(name = "AudioUpdateRating", defaultValue = "false", sendEvents = true)
	public boolean audioUpdateRatingTag = false;

	@UpnpStateVariable(name = "AudioLikesVisibleRoot", defaultValue = "false", sendEvents = true)
	public boolean audioLikesVisibleRoot = false;

	@UpnpStateVariable(name = "UpnpCdsWrite", defaultValue = "false", sendEvents = true)
	public boolean upnpCdsWrite = false;

	@UpnpStateVariable(name = "AnonymousDevicesWrite", defaultValue = "false", sendEvents = true)
	public boolean anonymousDevicesWrite = false;

	@UpnpStateVariable(name = "PreferEuropeanServer", defaultValue = "false", sendEvents = true)
	public boolean preferEuropeanServer = false;

	@UpnpStateVariable(name = "PlaylistLoop", defaultValue = "false", sendEvents = true)
	public boolean playlistLoop = false;

	public UmsExtendedServices() {
		readConfig();
		this.readConfigTimer = new TimerTask() {
			@Override
			public void run() {
				readConfig();
			}
		};
		timer.schedule(readConfigTimer, 0, 60000);
	}

	private void readConfig() {
		if (this.audioUpdateRatingTag != PMS.getConfiguration().isAudioUpdateTag()) {
			LOG.debug("isAudioUpdateTag has changed to {} ", PMS.getConfiguration().isAudioUpdateTag());
			this.audioUpdateRatingTag = PMS.getConfiguration().isAudioUpdateTag();
		}
		if (this.audioLikesVisibleRoot != PMS.getConfiguration().displayAudioLikesInRootFolder()) {
			LOG.debug("audioLikesVisibleRoot has changed to {} ", PMS.getConfiguration().displayAudioLikesInRootFolder());
			this.audioLikesVisibleRoot = PMS.getConfiguration().displayAudioLikesInRootFolder();
		}
		if (this.upnpCdsWrite != PMS.getConfiguration().isUpnpCdsWrite()) {
			LOG.debug("upnpCdsWrite has changed to {} ", PMS.getConfiguration().isUpnpCdsWrite());
			this.upnpCdsWrite = PMS.getConfiguration().isUpnpCdsWrite();
		}
		if (this.anonymousDevicesWrite != PMS.getConfiguration().isAnonymousDevicesWrite()) {
			LOG.debug("anonymousDevicesWrite has changed to {} ", PMS.getConfiguration().isAnonymousDevicesWrite());
			this.anonymousDevicesWrite = PMS.getConfiguration().isAnonymousDevicesWrite();
		}
		if (this.preferEuropeanServer != PMS.getConfiguration().isAudioAddictEuropeanServer()) {
			LOG.debug("prefer european servers has changed to {} ", PMS.getConfiguration().isAudioAddictEuropeanServer());
			this.preferEuropeanServer = PMS.getConfiguration().isAudioAddictEuropeanServer();
		}
		if (this.playlistLoop != PMS.getConfiguration().isAudioAddictPlaylistLoop()) {
			LOG.debug("playlistLoop has changed to {} ", PMS.getConfiguration().isAudioAddictPlaylistLoop());
			this.playlistLoop = PMS.getConfiguration().isAudioAddictPlaylistLoop();
		}
	}

	@UpnpAction
	public void setPreferEuropeanServer(@UpnpInputArgument(name = "PreferEuropeanServer") boolean preferEuropeanServer) {
		LOG.debug("updating preferEuropeanServer to {}. Value changed from : {}", preferEuropeanServer, this.preferEuropeanServer);
		PMS.getConfiguration().setAudioAddictEuropeanServer(preferEuropeanServer);
		this.preferEuropeanServer = preferEuropeanServer;
	}

	@UpnpAction
	public void setPlaylistLoop(@UpnpInputArgument(name = "PlaylistLoop") boolean playlistLoop) {
		LOG.debug("updating playlistLoop to {}. Value changed from : {}", playlistLoop, this.playlistLoop);
		PMS.getConfiguration().setAudioAddictPlaylistLoop(playlistLoop);
		this.playlistLoop = playlistLoop;
	}

	/**
	 * Returns the track currently playing on the given AudioAddict curated playlist as a small JSON
	 * object, or an empty string when that playlist is not being streamed right now. Lets a control point
	 * display the live playlist track, which is UMS-internal playback state.
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "NowPlaying"))
	public String getPlaylistNowPlaying(@UpnpInputArgument(name = "PlaylistId") UnsignedIntegerFourBytes playlistId) {
		AudioAddictTrackDto track = AudioAddictPlaylistInputStream.getCurrentTrack(playlistId.getValue().intValue());
		if (track == null) {
			return "";
		}
		Map<String, String> nowPlaying = new LinkedHashMap<>();
		nowPlaying.put("artist", track.artist != null ? track.artist : "");
		nowPlaying.put("title", track.title != null ? track.title : "");
		nowPlaying.put("artUrl", track.albumArt != null ? track.albumArt : "");
		try {
			return new ObjectMapper().writeValueAsString(nowPlaying);
		} catch (JsonProcessingException e) {
			LOG.warn("cannot serialize playlist now-playing for playlist {}", playlistId, e);
			return "";
		}
	}

	@UpnpAction
	public void setAudioUpdateRatingTag(@UpnpInputArgument(name = "AudioUpdateRating") boolean newAudioUpdateRatingTag) {
		this.audioUpdateRatingTag = newAudioUpdateRatingTag;
		boolean changed = PMS.getConfiguration().setAudioUpdateTag(newAudioUpdateRatingTag);
		LOG.debug("updating audioUpdateRatingTag to {}. Value changed : {}", newAudioUpdateRatingTag, changed);
	}

	@UpnpAction
	public void setAudioLikesVisibleRoot(@UpnpInputArgument(name = "AudioLikesVisibleRoot") boolean newAudioLikesVisibleRoot) {
		this.audioLikesVisibleRoot = newAudioLikesVisibleRoot;
		boolean changed = PMS.getConfiguration().setDisplayAudioLikesInRootFolder(newAudioLikesVisibleRoot);
		LOG.debug("updating audioLikesVisibleRoot to {}. Value changed : {}", newAudioLikesVisibleRoot, changed);
	}

	@UpnpAction
	public void setUpnpCdsWrite(@UpnpInputArgument(name = "UpnpCdsWrite") boolean newUpnpCdsWrite) {
		this.upnpCdsWrite = newUpnpCdsWrite;
		boolean changed = PMS.getConfiguration().setUpnpCdsWrite(newUpnpCdsWrite);
		LOG.debug("updating upnpCdsWrite to {}. Value changed : {}", newUpnpCdsWrite, changed);
	}

	@UpnpAction
	public void setAnonymousDevicesWrite(@UpnpInputArgument(name = "AnonymousDevicesWrite") boolean newAnonymousDevicesWrite) {
		this.anonymousDevicesWrite = newAnonymousDevicesWrite;
		boolean changed = PMS.getConfiguration().setAnonymousDevicesWrite(newAnonymousDevicesWrite);
		LOG.debug("updating anonymousDevicesWrite to {}. Value changed : {}", newAnonymousDevicesWrite, changed);
	}

	/**
	 * Set the AudioAddict username in the UMS configuration.
	 * @param audioAddictUser
	 */
	@UpnpAction
	public void setAudioAddictUser(@UpnpInputArgument(name = "AudioAddictUser") String audioAddictUser) {
		PMS.getConfiguration().setAudioAddictUser(audioAddictUser);
		LOG.debug("updated AudioAddict user");
	}

	/**
	 * Set the AudioAddict password in the UMS configuration. Note that the password is stored in plain text in the configuration file.
	 * @param audioAddictPass
	 */
	@UpnpAction
	public void setAudioAddictPass(@UpnpInputArgument(name = "AudioAddictPass") String audioAddictPass) {
		PMS.getConfiguration().setAudioAddictPassword(audioAddictPass);
		LOG.debug("updated AudioAddict password");
	}

	/**
	 * Sets the "audio artist" directory in the UMS configuration from a media-store object id. The
	 * object id is resolved to its filesystem path and stored; used as the source of artist images.
	 */
	@UpnpAction
	public void setAudioArtistDir(@UpnpInputArgument(name = "ObjectID") String objectId) {
		LOG.debug("updating audio artist directory to object id {}", objectId);
		new UmsArtistImageProvider().updateArtistDir(objectId);
	}

	/**
	 * @return the currently configured audio artist directory, or an empty string when none is set.
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "ObjectID"))
	public String getAudioArtistDir() {
		String dir = PMS.getConfiguration().getAudioArtistDir();
		return dir != null ? dir : "";
	}

	/**
	 * Searches radio-browser.info so a control point can offer a station picker.
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "Result"))
	public String searchRadioStations(
			@Nullable @UpnpInputArgument(name = "Name") String name,
			@Nullable @UpnpInputArgument(name = "CountryCode") String countryCode,
			@Nullable @UpnpInputArgument(name = "Language") String language,
			@Nullable @UpnpInputArgument(name = "Tag") String tag,
			@Nullable @UpnpInputArgument(name = "Offset") UnsignedIntegerFourBytes offset,
			@Nullable @UpnpInputArgument(name = "Limit") UnsignedIntegerFourBytes limit) throws UmsExtendedServicesException {
		return RadioBrowserSearch.searchStations(name, countryCode, language, tag,
				offset != null ? offset.getValue().intValue() : 0,
				limit != null ? limit.getValue().intValue() : 0);
	}

	/**
	 * Values for the filter fields of that picker.
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "Result"))
	public String getRadioFilterValues(
			@UpnpInputArgument(name = "Kind") String kind,
			@Nullable @UpnpInputArgument(name = "Search") String search) throws UmsExtendedServicesException {
		return RadioBrowserSearch.getFilterValues(kind, search);
	}

	/**
	 * Adds a station to a playlist.
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "Result"))
	public String addRadioStationToPlaylist(
			@UpnpInputArgument(name = "ObjectID") String objectId,
			@UpnpInputArgument(name = "StationUuid") String stationUuid,
			@Nullable @UpnpInputArgument(name = "Title") String title,
			RemoteClientInfo remoteClientInfo) throws UmsExtendedServicesException {
		Renderer renderer = UmsContentDirectoryService.getBrowseRenderer(remoteClientInfo);
		if (renderer == null) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "unknown media renderer");
		}
		StoreResource resource = renderer.getMediaStore().getResource(objectId);
		if (!(resource instanceof PlaylistFolder playlist)) {
			throw new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID, "object " + objectId + " is not a playlist");
		}
		Station station = RadioBrowserSearch.getStation(stationUuid);
		String url = RadioBrowserSearch.getStreamUrl(station);
		if (StringUtils.isBlank(url)) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "station " + station.getName() + " has no stream url");
		}
		String entryTitle = StringUtils.isNotBlank(title) ? title.trim() : StringUtils.trimToEmpty(station.getName());
		if (!playlist.addWebEntry(url, entryTitle, station.getFavicon(),
				station.getStationUUID() != null ? station.getStationUUID().toString() : null)) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED,
					"could not add " + entryTitle + " to " + playlist.getName());
		}
		ConnectedRenderers.invalidateRendererCache(new File(playlist.getFileName()));
		LOG.debug("added radio station {} to playlist {}", entryTitle, playlist.getFileName());
		return entryTitle;
	}

	@UpnpAction
	public void rescanMediaStore() throws UmsExtendedServicesException {
		if (!MediaScanner.isMediaScanRunning()) {
			LOG.debug("starting media scan ...");
			MediaScanner.startMediaScan();
		} else {
			LOG.warn("Media scan already in progress");
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Media scan already in progress");
		}
	}

	@UpnpAction
	public void rescanMediaStoreFolder(@UpnpInputArgument(name = "ObjectID") String objectId,
			RemoteClientInfo remoteClientInfo) throws UmsExtendedServicesException {
		LOG.debug("updating object with ID {} ", objectId);
		Renderer renderer = UmsContentDirectoryService.getBrowseRenderer(remoteClientInfo);
		if (renderer == null) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "unknown media renderer");
		}
		StoreResource sr = renderer.getMediaStore().getResource(objectId);
		if (sr == null) {
			throw new UmsExtendedServicesException(ErrorCode.ARGUMENT_VALUE_INVALID, "no object with id " + objectId);
		}
		LOG.debug("object with ID has path of {} ", sr.getFileName());
		MediaScanner.backgroundScanFileOrFolder(sr.getFileName());
	}

	/**
	 * Check if an album is liked using MusicBrainz and/or Discogs IDs.
	 *
	 * @return true if the album is liked via either MusicBrainz or Discogs
	 */
	@UpnpAction(out = @UpnpOutputArgument(name = "AlbumLikedValue"))
	public boolean isAlbumLiked(
			@Nullable @UpnpInputArgument(name = "MusicBrainzId") String musicBrainzId,
			@Nullable @UpnpInputArgument(name = "DiscogsId") UnsignedIntegerFourBytes discogsId) throws UmsExtendedServicesException {

		Long discogsIdLong = discogsId != null ? discogsId.getValue().longValue() : null;
		AlbumId albumId = new AlbumId(musicBrainzId, discogsIdLong);
		LOG.debug("check album liked for albumId: musicBrainzId={}, discogsId={}", albumId.musicBrainzId, albumId.discogsId);

		boolean likedViaMusicBrainz = false;
		boolean likedViaDiscogs = false;

		if (albumId.musicBrainzId != null && !albumId.musicBrainzId.isBlank()) {
			likedViaMusicBrainz = likeMusic.isAlbumLikedMB(albumId.musicBrainzId.trim());
		}

		if (albumId.discogsId != null) {
			likedViaDiscogs = likeMusic.isAlbumLikedDiscogs(albumId.discogsId);
		}

		return likedViaMusicBrainz || likedViaDiscogs;
	}

	/**
	 * Like an album using MusicBrainz and/or Discogs IDs. If both IDs are provided, both will be liked.
	 */
	@UpnpAction
	public void likeAlbum(
			@Nullable @UpnpInputArgument(name = "MusicBrainzId") String musicBrainzId,
			@Nullable @UpnpInputArgument(name = "DiscogsId") UnsignedIntegerFourBytes discogsId) throws UmsExtendedServicesException {

		Long discogsIdLong = discogsId != null ? discogsId.getValue().longValue() : null;
		AlbumId albumId = new AlbumId(musicBrainzId, discogsIdLong);
		LOG.debug("like album for albumId: musicBrainzId={}, discogsId={}", albumId.musicBrainzId, albumId.discogsId);

		if (albumId.musicBrainzId != null && !albumId.musicBrainzId.isBlank()) {
			likeMusic.likeAlbumMB(albumId.musicBrainzId.trim());
			LOG.debug("liked album via MusicBrainz: {}", albumId.musicBrainzId);
		}

		if (albumId.discogsId != null) {
			likeMusic.likeAlbumDiscogs(albumId.discogsId);
			LOG.debug("liked album via Discogs: {}", albumId.discogsId);
		}
	}

	/**
	 * Dislike an album using MusicBrainz and/or Discogs IDs.
	 */
	@UpnpAction
	public void dislikeAlbum(
			@Nullable @UpnpInputArgument(name = "MusicBrainzId") String musicBrainzId,
			@Nullable @UpnpInputArgument(name = "DiscogsId") UnsignedIntegerFourBytes discogsId) throws UmsExtendedServicesException {

		Long discogsIdLong = discogsId != null ? discogsId.getValue().longValue() : null;
		AlbumId albumId = new AlbumId(musicBrainzId, discogsIdLong);
		LOG.debug("dislike album for albumId: musicBrainzId={}, discogsId={}", albumId.musicBrainzId, albumId.discogsId);

		if (albumId.musicBrainzId != null && !albumId.musicBrainzId.isBlank()) {
			likeMusic.dislikeAlbumMB(albumId.musicBrainzId.trim());
			LOG.debug("disliked album via MusicBrainz: {}", albumId.musicBrainzId);
		}

		if (albumId.discogsId != null) {
			likeMusic.dislikeAlbumDiscogs(albumId.discogsId);
			LOG.debug("disliked album via Discogs: {}", albumId.discogsId);
		}
	}

	/**
	 * Backup the ratings of all resource types to a file.
	 */
	@UpnpAction
	public void backupRatings() throws UmsExtendedServicesException {
		LOG.debug("backing up audio ratings ... ");
		try {
			RatingBackupManager.backupRatings();
		} catch (Exception e) {
			LOG.error("failed backup audio ratings", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		}
	}

	/**
	 * Restores the ratings of all resource types from a backup file created by the backupRatings action.
	 */
	@UpnpAction
	public void restoreRatings() throws UmsExtendedServicesException {
		LOG.debug("restoring audio ratings ... ");
		try {
			RatingBackupManager.restoreRating();
		} catch (Exception e) {
			LOG.error("failed restore audio ratings", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		}
	}
}
