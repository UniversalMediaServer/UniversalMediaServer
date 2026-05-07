package net.pms.util.artistImageProvider;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.ControlPoint;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;


public class UmsArtistImageProvider implements IArtistImageProvider, ConfigurationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsArtistImageProvider.class.getName());
	private String objectId;

	public UmsArtistImageProvider() {
		PMS.getConfiguration().addConfigurationListener(this);
		File dir = new File(PMS.getConfiguration().getAudioArtistDir());
		if (dir.exists() && dir.isDirectory()) {
			LOGGER.info("Artist directory found: " + dir.getAbsolutePath());
			StoreResource sr = DbIdResourceLocator.getLibraryResourceFolder(ControlPoint.getRenderer(), dir.getAbsolutePath());
			if (sr != null) {
				objectId = sr.getId();
				LOGGER.info("Artist folder objectId set to: " + objectId);
			} else {
				LOGGER.warn("Artist directory not found in media store: " + dir.getAbsolutePath());
				objectId = null;
			}
			LOGGER.debug("artist folder objectId: " + objectId);
		} else {
			LOGGER.warn("Artist directory not found or is not a directory: " + dir.getAbsolutePath());
		}
	}

	public DLNAThumbnailInputStream getThumbnail(Renderer renderer, String artistName) {
		LOGGER.info("Getting thumbnail for artist: " + artistName);
		DLNAThumbnailInputStream thumb = null;
		if (!StringUtils.isBlank(artistName)) {
			try {
				StoreContainer folder = (StoreContainer) renderer.getMediaStore().getResource(objectId);
				if (folder != null) {
					var match = folder.getChildren().stream()
							.filter(child -> child.getName().equalsIgnoreCase(artistName))
							.findFirst();
					if (match.isPresent()) {
						try {
							MediaStoreIds.incrementUpdateId(match.get().getLongId());
							thumb = match.get().getThumbnailInputStream();
							LOGGER.info("Thumbnail retrieved for artist: " + artistName);
						} catch (Exception e) {
							LOGGER.error("Error retrieving thumbnail for artist: " + artistName, e);
						}
					} else {
						LOGGER.trace("Artist not found in folder: " + artistName);
					}
				} else {
					LOGGER.warn("Artist folder not found for objectId: " + objectId);
				}
			} catch (Exception e) {
				LOGGER.error("Error getting thumbnail for artist: " + artistName, e);
			}
		} else {
			LOGGER.trace("Artist name is blank");
		}
		return thumb;
	}

	public void updateArtistDir(String objectId) {
		StoreResource sr = ControlPoint.getRenderer().getMediaStore().getResource(objectId);
		if (sr != null) {
			LOGGER.debug("object with ID {} has path of {} ", objectId, sr.getFileName());
			PMS.getConfiguration().setAudioArtistDir(sr.getFileName());
			LOGGER.debug("updated AudioArtistDir to {}", sr.getFileName());
			saveUmsConfig();
		} else {
			LOGGER.warn("object with ID {} not found in media store", objectId);
		}
	}

	@Override
	public void configurationChanged(ConfigurationEvent event) {
		if (!event.isBeforeUpdate()) {
			if (UmsConfiguration.KEY_AUDIO_ARTIST_DIR.equals(event.getPropertyName())) {
				String dir = event.getPropertyValue().toString();
				if (new File(dir).exists()) {
					LOGGER.info("Audio Artist directory updated to: " + dir);
					StoreResource sr = DbIdResourceLocator.getLibraryResourceFolder(ControlPoint.getRenderer(), dir);
					if (sr != null) {
						objectId = sr.getId();
						LOGGER.debug("object with ID {} has path of {} ", objectId, dir);
					} else {
						LOGGER.warn("object with path {} not found in media store", dir);
					}
				} else {
					LOGGER.warn("Audio Artist directory does not exist: " + dir);
				}
			}
		}
	}

	@Override
	public String getIdent() {
		return "UMS Artist Image Provider for objectId %s.".formatted(objectId);
	}

	private void saveUmsConfig() {
		try {
			PMS.getConfiguration().save();
		} catch (ConfigurationException e) {
			LOGGER.error("Failed to save UMS configuration", e);
		}
	}
}
