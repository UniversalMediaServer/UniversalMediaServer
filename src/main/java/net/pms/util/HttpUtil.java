package net.pms.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class HttpUtil {

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

	public static HttpHeaders getHeaders(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).
			method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<Void> response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build().
			send(request, HttpResponse.BodyHandlers.discarding());
		return response.headers();
	}

	public static HttpHeaders getHeadersFromInputStreamRequest(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).
			headers("Content-Type", "text/plain;charset=UTF-8").GET().build();

		HttpResponse<InputStream> response = HttpClient.newBuilder().
				followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());
		response.body().close();
		return response.headers();
	}

	public static InputStream getHttpResourceInputStream(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		java.net.http.HttpResponse<InputStream> extStream = HttpClient.newBuilder().
			followRedirects(HttpClient.Redirect.ALWAYS).build().send(request, BodyHandlers.ofInputStream());
		return extStream.body();
	}
}
