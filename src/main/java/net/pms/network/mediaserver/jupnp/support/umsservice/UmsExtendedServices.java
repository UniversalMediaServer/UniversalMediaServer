package net.pms.network.mediaserver.jupnp.support.umsservice;

import java.io.FileNotFoundException;
import java.sql.SQLException;
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
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.mediaserver.jupnp.support.umsservice.impl.LikeMusic;
import net.pms.network.mediaserver.jupnp.support.umsservice.impl.RatingBackupManager;
import net.pms.store.MediaScanner;
import net.pms.store.StoreResource;

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
	@UpnpStateVariable(name = "A_ARG_TYPE_AudioAddictPass", sendEvents = false, datatype = "string")
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
			LOG.debug("prefer european srevers has changed to {} ", PMS.getConfiguration().isAudioAddictEuropeanServer());
			this.anonymousDevicesWrite = PMS.getConfiguration().isAudioAddictEuropeanServer();
		}
	}

	@UpnpAction
	public void setPreferEuropeanServer(@UpnpInputArgument(name = "PreferEuropeanServer") boolean preferEuropeanServer) {
		LOG.debug("updating preferEuropeanServer to {}. Value changed from : {}", preferEuropeanServer, this.preferEuropeanServer);
		PMS.getConfiguration().setAudioAddictEuropeanServer(preferEuropeanServer);
		this.preferEuropeanServer = preferEuropeanServer;
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

	@UpnpAction
	public void setAudioAddictUser(@UpnpInputArgument(name = "AudioAddictUser") String audioAddictUser) {
		PMS.getConfiguration().setAudioAddictUser(audioAddictUser);
		LOG.debug("updated AudioAddict user");
	}

	@UpnpAction
	public void setAudioAddictPass(@UpnpInputArgument(name = "AudioAddictPass") String audioAddictPass) {
		PMS.getConfiguration().setAudioAddictPassword(audioAddictPass);
		LOG.debug("updated AudioAddict password");
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
	public void rescanMediaStoreFolder(@UpnpInputArgument(name = "ObjectID") String objectId) {
		LOG.debug("updating object with ID {} ", objectId);
		StoreResource sr = RendererConfigurations.getDefaultRenderer().getMediaStore().getResource(objectId);
		LOG.debug("object with ID has path of {} ", sr.getFileName());
		MediaScanner.backgroundScanFileOrFolder(sr.getFileName());
	}

	@UpnpAction
	public void backupAudioLikes() throws UmsExtendedServicesException {
		LOG.debug("backing up audio likes table ... ");
		try {
			likeMusic.backupLikedAlbums();
		} catch (SQLException e) {
			LOG.error("failed backup audio likes table", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		}
	}

	@UpnpAction
	public void restoreAudioLikes() throws UmsExtendedServicesException {
		LOG.debug("restoring audio likes table ... ");
		try {
			likeMusic.restoreLikedAlbums();
		} catch (SQLException e) {
			LOG.error("failed backup audio likes table", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		} catch (FileNotFoundException e) {
			LOG.error("Backup file couldn't be found", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Backup file not found : " + e.getMessage());
		}
	}

	/**
	 * Check if an album is liked using MusicBrainz and/or Discogs IDs.
	 * At least one of the IDs must be provided.
	 *
	 * @return true if the album is liked via either MusicBrainz or Discogs
	 * @throws UmsExtendedServicesException if both IDs are missing or invalid
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
	 * Like an album using MusicBrainz and/or Discogs IDs.
	 * If both IDs are provided, both will be liked.
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
