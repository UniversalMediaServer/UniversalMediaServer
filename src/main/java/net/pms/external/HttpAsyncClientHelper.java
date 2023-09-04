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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;

/**
 * Download file from the external server and inform the calling process about
 * the progress of download.
 */
public class HttpAsyncClientHelper {

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
	public static byte[] getBytes(String uri) throws IOException, InterruptedException {
		try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
			httpclient.start();
			SimpleHttpRequest request = new SimpleHttpRequest(Method.GET.name(), uri);
			Future<SimpleHttpResponse> future = httpclient.execute(request, null);
			SimpleHttpResponse response = future.get();
			int statusCode = response.getCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("HTTP response not OK for " + uri);
			}
			return response.getBodyBytes();
		} catch (ExecutionException e) {
			throw new IOException("Unable to download by HTTP" + e.getMessage());
		}
	}

	/**
	 * Download the file from the external server and store it at the defined
	 * path.
	 *
	 * @param uri The URI of the external server file.
	 * @param file The path to store downloaded file.
	 * @param callback The callback which will be informed about the progress of
	 * the file download.
	 *
	 * @throws Exception
	 */
	public static void getFile(URI uri, File file, ProgressCallback callback) throws Exception {
		try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
			httpclient.start();
			FileDataAsyncResponseConsumer responseConsumer = new FileDataAsyncResponseConsumer(file, uri.toString(), callback);
			SimpleRequestProducer requestProducer = SimpleRequestProducer.create(SimpleHttpRequest.create(Method.GET, uri));
			Future<File> future = httpclient.execute(requestProducer, responseConsumer, null);
			future.get();
		}
	}

}
