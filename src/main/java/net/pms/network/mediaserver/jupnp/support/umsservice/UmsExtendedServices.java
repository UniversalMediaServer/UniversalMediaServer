package net.pms.network.mediaserver.jupnp.support.umsservice;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.model.types.ErrorCode;
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
	@UpnpStateVariable(name = "A_ARG_TYPE_MusicBraizReleaseID", sendEvents = false, datatype = "string"),
	@UpnpStateVariable(name = "A_ARG_TYPE_AlbumLikedValue", sendEvents = false, datatype = "boolean")
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

	@UpnpStateVariable(name = "AudioAddictUser", defaultValue = "", sendEvents = true)
	public String audioAddictUser = "";

	@UpnpStateVariable(name = "AudioAddictPass", defaultValue = "", sendEvents = true)
	public String audioAddictPass = "";

	@UpnpStateVariable(name = "AudioAddictEurope", defaultValue = "false", sendEvents = true)
	public boolean audioAddictEurope = false;


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
		this.audioUpdateRatingTag = PMS.getConfiguration().isAudioUpdateTag();
		this.audioLikesVisibleRoot = PMS.getConfiguration().displayAudioLikesInRootFolder();
		this.upnpCdsWrite = PMS.getConfiguration().isUpnpCdsWrite();
		this.anonymousDevicesWrite = PMS.getConfiguration().isAnonymousDevicesWrite();
		this.audioAddictUser = PMS.getConfiguration().getAudioAddictUser();
		this.audioAddictPass = PMS.getConfiguration().getAudioAddictPassword();
		this.audioAddictEurope = PMS.getConfiguration().isAudioAddictEuropeanServer();
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
	public void setAudioAddictUser(@UpnpInputArgument(name = "AudioAddictUser") String newAudioAddictUser) {
		this.audioAddictUser = newAudioAddictUser;
		PMS.getConfiguration().setAudioAddictUser(newAudioAddictUser);
		LOG.debug("updating audioAddictUser to {}. Value changed : {}", newAudioAddictUser);
	}

	@UpnpAction
	public void setAudioAddictPass(@UpnpInputArgument(name = "AudioAddictPass") String newAudioAddictPass) {
		this.audioAddictPass = newAudioAddictPass;
		PMS.getConfiguration().setAudioAddictPassword(newAudioAddictPass);
		LOG.debug("updating audioAddictPass to {}.", newAudioAddictPass);
	}

	@UpnpAction
	public void setAudioAddictEurope(@UpnpInputArgument(name = "AudioAddictEurope") boolean newAudioAddictEurope) {
		this.audioAddictEurope = newAudioAddictEurope;
		PMS.getConfiguration().setAudioAddictEuropeanServer(newAudioAddictEurope);
		LOG.debug("updating audioAddictEurope to {}. Value changed : {}", newAudioAddictEurope);
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

	@UpnpAction(out = @UpnpOutputArgument(name = "AlbumLikedValue"))
	public boolean isAlbumLiked(@UpnpInputArgument(name = "MusicBraizReleaseID") String musicBrainzReleaseId) throws UmsExtendedServicesException {
		LOG.debug("check album liked for musicbrainz release id of {} ", musicBrainzReleaseId);
		return likeMusic.isAlbumLiked(musicBrainzReleaseId);
	}

	@UpnpAction
	public void likeAlbum(@UpnpInputArgument(name = "MusicBraizReleaseID") String musicBrainzReleaseId) throws UmsExtendedServicesException {
		LOG.debug("like album with musicbrainz release id {} ", musicBrainzReleaseId);
		likeMusic.likeAlbum(musicBrainzReleaseId);
	}

	@UpnpAction
	public void dislikeAlbum(@UpnpInputArgument(name = "MusicBraizReleaseID") String musicBrainzReleaseId) throws UmsExtendedServicesException {
		LOG.debug("dislike album with musicbrainz release id {} ", musicBrainzReleaseId);
		likeMusic.dislikeAlbum(musicBrainzReleaseId);
	}

	@UpnpAction
	public void backupRatings() throws UmsExtendedServicesException {
		LOG.debug("backing up ratings ... ");
		try {
			RatingBackupManager.backupRatings();
		} catch (Exception e) {
			LOG.error("failed backup ratings", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		}
	}

	@UpnpAction
	public void restoreRatings() throws UmsExtendedServicesException {
		LOG.debug("restoring ratings ... ");
		try {
			RatingBackupManager.restoreRating();
		} catch (Exception e) {
			LOG.warn("restore failed", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, e.getMessage());
		}
	}

}
