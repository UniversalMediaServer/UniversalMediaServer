package net.pms.util.artistImageProvider;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;


public class UmsArtistImageProvider implements IArtistImageProvider, ConfigurationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsArtistImageProvider.class.getName());
	private String objectId;

	public UmsArtistImageProvider() {
		PMS.getConfiguration().addConfigurationListener(this);
		objectId = PMS.getConfiguration().getAudioArtistDir();
		LOGGER.debug("artist folder objectId: " + objectId);
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

	@Override
	public void configurationChanged(ConfigurationEvent event) {
		if (!event.isBeforeUpdate()) {
			if (UmsConfiguration.KEY_AUDIO_ARTIST_DIR.equals(event.getPropertyName())) {
				objectId = event.getPropertyValue().toString();
				LOGGER.info("New artist folder objectId: " + event.getPropertyValue());
			}
		}
	}

	@Override
	public String getIdent() {
		return "UMS Artist Image Provider for objectId %s.".formatted(objectId);
	}

}
