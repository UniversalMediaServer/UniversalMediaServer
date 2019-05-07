/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;

/**
 * Download file from the external server.
 *
 * @author valib
 */
public class UriFileRetriever {
	
	private URI uri;
	private File target;
	private UriRetrieverCallback callback;

	/**
	 * Download file from the external server.
	 *
	 * @param uri The URI of the external server.
	 * @param target The path the received file is stored in the system.
	 * @param callback The calling process to be informed about the download progress.
	 */
	public UriFileRetriever(URI uri, File target, UriRetrieverCallback callback) {
		this.uri = uri;
		this.target = target;
		this.callback = callback;
	}

	public UriFileRetriever() {
	}

	/**
	 * Download file from the external server and return the
	 * content of it in the ByteArray.
	 *
	 * @param uri The URI of the external server file.
	 * 
	 * @return the content of the downloaded file.
	 * 
	 * @throws IOException
	 */
	public byte[] get(String uri) throws IOException {
		CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        try {
            httpclient.start();
            HttpGet request = new HttpGet(uri);
            Future<HttpResponse> future = httpclient.execute(request, null);
            HttpResponse response = future.get();
            int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("HTTP response not OK");
			}

			return IOUtils.toByteArray(response.getEntity().getContent());
		} catch (InterruptedException|ExecutionException e) {
			throw new IOException("Unable to download by HTTP" + e.getMessage());
		} finally {
			httpclient.close();
		}
	}

	/**
	 * Download the file from the external server and store it at the defined path.
	 * 
	 * @return the downloaded file.
	 * 
	 * @throws Exception 
	 */
	public File executeGetFile() throws Exception {
		CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
		File result;
		try  {
			httpclient.start();
			ZeroCopyConsumerWithCallback<File> consumer = new ZeroCopyConsumerWithCallback<File>(target, uri.toString(), callback) {

				@Override
				protected File process(
					final HttpResponse response,
					final File file,
					final ContentType contentType) throws Exception {
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						throw new ClientProtocolException("Connection to host failed: " + response.getStatusLine());
					}

					return file;
				}
			};

			Future<File> future = httpclient.execute(HttpAsyncMethods.createGet(uri), consumer, null, null);
			result = future.get();
		} finally {
			httpclient.close();
		}

		return result;
	}
}
