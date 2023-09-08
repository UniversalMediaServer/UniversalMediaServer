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
import java.io.FileNotFoundException;
import java.net.http.HttpResponse;

public class FileBodyHandler implements HttpResponse.BodyHandler<Void> {

	private final File file;
	private final String uri;
	private final ProgressCallback progressCallback;

	public FileBodyHandler(File file, String uri, ProgressCallback progressCallback) throws FileNotFoundException {
		if (file == null) {
			throw new FileNotFoundException("File may nor be null");
		}
		this.file = file;
		this.uri = uri;
		this.progressCallback = progressCallback;
	}

	@Override
	public HttpResponse.BodySubscriber<Void> apply(HttpResponse.ResponseInfo responseInfo) {
		long contentLength = responseInfo.headers().firstValueAsLong("Content-Length").orElse(-1);
		FileBodySubscriber subscriber = new FileBodySubscriber(file, contentLength, uri, progressCallback);
		return HttpResponse.BodySubscribers.fromSubscriber(subscriber, s -> null);
	}

}
