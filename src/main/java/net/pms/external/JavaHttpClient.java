/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.external;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.UnknownFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class JavaHttpClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaHttpClient.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private JavaHttpClient() {
	}

	/**
	 * Download file from the external server and return the content of it in
	 * the ByteArray.
	 *
	 * @param uri The URI of the external server file.
	 *
	 * @return the content of the downloaded file.
	 *
	 * @throws IOException
	 */
	public static byte[] getBytes(String uri) throws IOException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(uri))
					.GET()
					.build();
			HttpResponse<byte[]> response = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build()
					.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
					.join();
			int statusCode = response.statusCode();
			if (statusCode != 200) {
				String contentType = response.headers().firstValue("content-type").orElse(null);
				Long contentLength = response.headers().firstValueAsLong("content-length").orElse(0);
				if (contentType != null && contentType.startsWith("text") && contentLength != 0) {
					String body = new String(response.body(), StandardCharsets.UTF_8);
					throw new IOException("HTTP response not OK (" + statusCode + ") for " + uri + ":\n" + body);
				}
				throw new IOException("HTTP response not OK (" + statusCode + ") for " + uri);
			}
			return response.body();
		} catch (URISyntaxException ex) {
			throw new IOException("Unable to download by HTTP" + ex.getMessage());
		}
	}

	public static void getFile(File file, String uri, ProgressCallback callback) throws IOException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(uri))
					.GET()
					.build();
			FileBodyHandler responseBodyHandler = new FileBodyHandler(file, uri, callback);
			HttpResponse<Void> response = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build()
					.sendAsync(request, responseBodyHandler)
					.join();
			int statusCode = response.statusCode();
			if (statusCode != 200) {
				throw new IOException("HTTP response not OK (" + statusCode + ") for " + uri);
			}
		} catch (URISyntaxException ex) {
			throw new IOException("Unable to download by HTTP" + ex.getMessage());
		}
	}

	/**
	 * Request http body from uri. Result should be UTF-8.
	 *
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public static String getStringBody(String uri) throws IOException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(uri))
					.headers("Content-Type", "text/plain;charset=UTF-8")
					.GET()
					.build();
			HttpResponse<String> response = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build()
					.sendAsync(request, HttpResponse.BodyHandlers.ofString())
					.join();
			int statusCode = response.statusCode();
			if (statusCode != 200) {
				throw new IOException("HTTP response not OK (" + statusCode + ") for " + uri);
			}
			return response.body();
		} catch (IllegalArgumentException ex) {
			throw new IOException("Unable to get string by HTTP:" + ex.getMessage());
		}
	}

	public static HttpHeaders getHeaders(String uri) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(uri))
					.method("HEAD", HttpRequest.BodyPublishers.noBody())
					.build();
			HttpResponse<Void> response = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build()
					.sendAsync(request, HttpResponse.BodyHandlers.discarding())
					.join();
			return response.headers();
		} catch (IllegalArgumentException ex) {
			LOGGER.error("Unable to read headers for " + uri);
			return HttpHeaders.of(Map.of(), null);
		}
	}

	public static HttpHeaders getHeadersFromInputStreamRequest(String uri) {
		try {
			HttpResponse<InputStream> response = getHttpResponseInputStream(uri);
			response.body().close();
			return response.headers();
		} catch (IOException | IllegalArgumentException ex) {
			LOGGER.error("Unable to read headers for request (InputStream) " + uri);
			return HttpHeaders.of(Map.of(), null);
		}
	}

	public static HttpResponse<InputStream> getHttpResponseInputStream(String uri) throws IOException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(uri))
					.GET()
					.build();
			return HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.ALWAYS)
					.build()
					.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
					.join();
		} catch (IllegalArgumentException ex) {
			throw new IOException("Unable to GET InputStream for request " + uri + ":" + ex.getMessage());
		}
	}

	public static DLNAThumbnail getThumbnail(String uri) {
		try {
			LOGGER.trace("Downloading image from {}", uri);
			byte[] image = getBytes(uri);
			return DLNAThumbnail.toThumbnail(image, 640, 480, ScaleType.MAX, ImageFormat.JPEG, false);
		} catch (EOFException e) {
			LOGGER.debug(
					"Error reading thumbnail from uri \"{}\": Unexpected end of stream, probably corrupt or read error.",
					uri
			);
		} catch (UnknownFormatException e) {
			LOGGER.debug("Could not read thumbnail from uri \"{}\": {}", uri, e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Error reading thumbnail from uri \"{}\": {}", uri, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

}
