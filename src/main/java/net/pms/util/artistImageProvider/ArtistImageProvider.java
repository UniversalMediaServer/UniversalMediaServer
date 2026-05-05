package net.pms.util.artistImageProvider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;

public class ArtistImageProvider {

	private static ArtistImageProvider instance = new ArtistImageProvider();

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtistImageProvider.class.getName());

	private static List<IArtistImageProvider> providers = new CopyOnWriteArrayList<>();

	public ArtistImageProvider() {
		providers.add(new UmsArtistImageProvider());
	}

	public static ArtistImageProvider getInstance() {
		return instance;
	}


	public DLNAThumbnailInputStream getThumbnail(Renderer renderer, String artistName) {
		LOGGER.trace("Getting thumbnail for artist: {}", artistName);
		for (IArtistImageProvider artistImageProvider : providers) {
			DLNAThumbnailInputStream thumb = artistImageProvider.getThumbnail(renderer, artistName);
			if (thumb != null) {
				LOGGER.debug("Thumbnail found for artist: {} using provider: {}", artistName, artistImageProvider.getIdent());
				return thumb;
			}
		}
		LOGGER.trace("No thumbnail found for artist: {}", artistName);
		return null;
	}
}
