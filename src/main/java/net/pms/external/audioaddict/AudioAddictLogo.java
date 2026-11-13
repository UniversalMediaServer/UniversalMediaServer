package net.pms.external.audioaddict;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.function.LongSupplier;
import net.openhft.hashing.LongHashFunction;
import net.pms.dlna.DLNAThumbnail;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves and caches a network's current logo for all renderers.
 */
public final class AudioAddictLogo {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictLogo.class);
	private static final long REFRESH_INTERVAL = Duration.ofDays(1).toMillis();
	private static final long RETRY_INTERVAL = Duration.ofMinutes(5).toMillis();
	private static final int REQUEST_TIMEOUT = 5_000;
	private static final int MAX_RESPONSE_BYTES = 1_000_000;
	private static final LongHashFunction IMAGE_HASH = LongHashFunction.xx3();

	private final URI metadataUri;
	private final String websiteHost;
	private final LongSupplier clock;
	private long nextCheck;
	private Logo cachedLogo;

	AudioAddictLogo(String domain) {
		this(URI.create("https://cdn.audioaddict.com/" + domain + "/assets/logo-opengraph.png"), "www." + domain, System::currentTimeMillis);
	}

	AudioAddictLogo(URI metadataUri, String websiteHost, LongSupplier clock) {
		this.metadataUri = metadataUri;
		this.websiteHost = websiteHost;
		this.clock = clock;
	}

	public record Logo(DLNAThumbnail thumbnail, String etag) { }

	public synchronized Logo get() {
		if (clock.getAsLong() >= nextCheck) {
			long interval = RETRY_INTERVAL;
			try {
				DLNAThumbnail thumbnail = downloadLogo();
				if (thumbnail != null) {
					String etag = "\"aa-" + Long.toHexString(IMAGE_HASH.hashBytes(thumbnail.getBytes(false))) + "\"";
					cachedLogo = new Logo(thumbnail, etag);
					interval = REFRESH_INTERVAL;
				}
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.debug("Could not refresh the AudioAddict logo for {}: {}", websiteHost, e.getMessage());
			}
			nextCheck = clock.getAsLong() + interval;
		}
		return cachedLogo;
	}

	private DLNAThumbnail downloadLogo() throws IOException {
		// The unhashed CDN path returns the site's 404 page with up-to-date Open Graph metadata.
		Connection.Response metadata = connect(metadataUri).ignoreHttpErrors(true).execute();
		if (metadata.statusCode() != 200 && metadata.statusCode() != 404) {
			return null;
		}
		Document document = metadata.parse();
		Element image = document.selectFirst("meta[property=og:image:secure_url]");
		if (image == null) {
			image = document.selectFirst("meta[property=og:image]");
		}
		if (image == null) {
			return null;
		}
		URI imageUri = URI.create(image.absUrl("content"));
		String host = imageUri.getHost();
		if (!metadataUri.getScheme().equalsIgnoreCase(imageUri.getScheme()) ||
			(!websiteHost.equalsIgnoreCase(host) && !metadataUri.getHost().equalsIgnoreCase(host))) {
			return null;
		}
		Connection.Response response = connect(imageUri).ignoreContentType(true).execute();
		String contentType = response.contentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			return null;
		}
		return DLNAThumbnail.toThumbnail(response.bodyAsBytes());
	}

	private static Connection connect(URI uri) {
		return Jsoup.connect(uri.toString()).timeout(REQUEST_TIMEOUT).maxBodySize(MAX_RESPONSE_BYTES).followRedirects(false);
	}
}
