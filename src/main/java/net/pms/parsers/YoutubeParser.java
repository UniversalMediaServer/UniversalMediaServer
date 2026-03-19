package net.pms.parsers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.pms.media.MediaInfo;

public class YoutubeParser {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeParser.class.getName());

	public YoutubeParser() {
		// TODO Auto-generated constructor stub
	}

	public static void parseUrl(MediaInfo mediaInfo, String url) {
		JsonNode youTubeMetadata = getYouTubeMetadata(url);
		if (youTubeMetadata != null) {
			LOGGER.trace("YouTube metadata found for URL: {} \n{}", url, youTubeMetadata.toPrettyString());
			parseFormats(youTubeMetadata, mediaInfo);
		} else {
			LOGGER.trace("No YouTube metadata found for URL: {}", url);
			return;
		}
	}

	private static void parseFormats(JsonNode root, MediaInfo mediaInfo) {
		JsonNode formatsNode = root.path("streamingData").path("adaptiveFormats");

		if (formatsNode.isArray()) {
			for (JsonNode format : formatsNode) {
				String mimeType = format.path("mimeType").asText();
				int bitrate = format.path("bitrate").asInt();
				double duration = format.path("approxDurationMs").asLong() / 1000.0; // convert ms to seconds
				long contentLength = format.path("contentLength").asLong();
				mediaInfo.setBitRate(bitrate);
				mediaInfo.setMimeType(mimeType);
				mediaInfo.setDuration(duration);
				mediaInfo.setSize(contentLength);
				// TODO: Don't know which exact format to parse. We take the first one, but youtube offers so many different formats ...
				break;
			}
		}

		JsonNode videoNode = root.path("videoDetails");
		String title = videoNode.path("title").asText();
		mediaInfo.setTitle(title);
	}

	public static JsonNode getYouTubeMetadata(String videoUrl) {
		try {
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(videoUrl))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
				.header("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7")
				.GET()
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String html = response.body();

			Pattern pattern = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});");
			Matcher matcher = pattern.matcher(html);

			if (matcher.find()) {
				String jsonString = matcher.group(1);
				return MAPPER.readTree(jsonString);
			} else {
				LOGGER.debug("couldn't find 'ytInitialPlayerResponse' inside the YouTube video page HTML for URL: {}", videoUrl);
			}
		} catch (Exception e) {
			LOGGER.debug("Error while fetching YouTube metadata for URL: {}", videoUrl, e);
		}
		return null;
	}
}
