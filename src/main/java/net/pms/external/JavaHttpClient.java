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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 *
 * @author nicol
 */
public class JavaHttpClient {

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
				throw new IOException("HTTP response not OK for " + uri);
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
				throw new IOException("HTTP response not OK for " + uri);
			}
		} catch (URISyntaxException ex) {
			throw new IOException("Unable to download by HTTP" + ex.getMessage());
		}
	}

}
