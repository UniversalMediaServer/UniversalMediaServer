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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Downloads URLs
 *
 * @author Tim Cox (mail@tcox.org)
 */
public class UriRetriever {
	private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
	private static final int BUFFER_SIZE = 1024;
	private HttpClient client = new HttpClient();

	public byte[] get(String uri) throws IOException {
		HttpMethod method = new GetMethod(uri);
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				throw new IOException("HTTP response not OK");
			}
			return method.getResponseBody();
		} catch (HttpException e) {
			throw new IOException("Unable to download by HTTP" + e.getMessage());
		} finally {
			method.releaseConnection();
		}
	}

	public byte[] getWithCallback(String uri, UriRetrieverCallback callback) throws IOException {
		HttpMethod getMethod = null;

		try {
			getMethod = startGetRequest(uri, callback);
			int totalBytes = getContentSize(uri, getMethod);
			byte[] data = pullData(uri, getMethod, callback, totalBytes);
			return data;
		} catch (HttpException e) {
			throw new IOException("Unable to download via HTTP: " + uri + ": " + e.getMessage());
		} catch (IOException e) {
			throw new IOException("Unable to download via HTTP: " + uri + ": " + e.getMessage());
		} finally {
			if (getMethod != null) {
				getMethod.releaseConnection();
			}
		}
	}

	private HttpMethod startGetRequest(String uri, UriRetrieverCallback callback) throws HttpException, IOException {
		int statusCode = -1;
		HttpMethod method = new GetMethod(uri);
		configureMethod(method);
		statusCode = client.executeMethod(method);
		if (statusCode != HttpStatus.SC_OK) {
			throw new IOException("HTTP result code was not OK");
		}
		return method;
	}

	private void configureMethod(HttpMethod method) {
		method.setRequestHeader("User-Agent", "UMS");
		method.setFollowRedirects(true);
	}

	private static byte[] pullData(String uri, HttpMethod method, UriRetrieverCallback callback, int totalBytes) throws IOException {
		int bytesWritten = 0;
		InputStream input = method.getResponseBodyAsStream();
		ByteArrayOutputStream output = new ByteArrayOutputStream(totalBytes);
		byte[] buffer = new byte[BUFFER_SIZE];
		int count = -1;
		while ((count = input.read(buffer)) != -1) {
			output.write(buffer, 0, count);
			bytesWritten += count;
			invokeCallback(uri, callback, totalBytes, bytesWritten);
		}
		output.flush();
		output.close();
		return output.toByteArray();
	}

	private static void invokeCallback(String uri, UriRetrieverCallback callback, int totalBytes, int bytesWritten) throws IOException {
		try {
			callback.progressMade(uri, bytesWritten, totalBytes);
		} catch (UriRetrieverCallback.CancelDownloadException e) {
			throw new IOException("Download was cancelled");
		}
	}

	private int getContentSize(String uri, HttpMethod method) {
		Header header = method.getResponseHeader(HTTP_HEADER_CONTENT_LENGTH);
		if (header != null) {
			String value = "" + header.getValue();
			int totalBytes = -1;
			try {
				totalBytes = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return 0;
			}
			return totalBytes;
		} else {
			return 0;
		}
	}
}
