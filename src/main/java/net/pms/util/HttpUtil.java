package net.pms.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

	private HttpUtil() {
	}

	/**
	 * Request http body from uri. Result should be UTF-8.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String getStringBody(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).
			headers("Content-Type", "text/plain;charset=UTF-8").GET().build();

		HttpResponse<String> response = HttpClient.newBuilder().
				followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofString());

		return response.body();
	}

	public static HttpHeaders getHeaders(String url) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).
			method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<Void> response;
		try {
			response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build().
				send(request, HttpResponse.BodyHandlers.discarding());
			return response.headers();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("canot read headers HEAD request", e);
		}
		return HttpHeaders.of(new HashMap<String, List<String>>(), null);
	}

	public static HttpHeaders getHeadersFromInputStreamRequest(String url) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).
			headers("Content-Type", "text/plain;charset=UTF-8").GET().build();

		HttpResponse<InputStream> response;
		try {
			response = HttpClient.newBuilder().
					followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());
			response.body().close();
			return response.headers();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("canot read headers GET request (InputStream)", e);
		}
		return HttpHeaders.of(new HashMap<String, List<String>>(), null);
	}

	public static InputStream getHttpResourceInputStream(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		HttpResponse<InputStream> extStream = HttpClient.newBuilder().
			followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());
		return extStream.body();
	}

	public static HttpResponse<InputStream> getHttpResponseInputStream(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		HttpResponse<InputStream> extStream = HttpClient.newBuilder().
			followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());
		return extStream;
	}
}
