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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.entity.AbstractBinDataConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Asserts;

public class FileDataAsyncResponseConsumer extends AbstractBinDataConsumer implements AsyncEntityConsumer<File>, AsyncResponseConsumer<File> {

	private final File file;
	private final String uri;
	private final ProgressCallback progressCallback;
	private RandomAccessFile accessfile;
	private FileChannel fileChannel;
	private long writtenBytes = 0;
	private long fileSize;
	private volatile FutureCallback<File> resultCallback;

	public FileDataAsyncResponseConsumer(File file, String uri, ProgressCallback progressCallback) throws FileNotFoundException {
		super();
		if (file == null) {
			throw new IllegalArgumentException("File may nor be null");
		}
		this.file = file;
		this.uri = uri;
		this.progressCallback = progressCallback;
	}

	@Override
	public final void streamStart(
			final EntityDetails entityDetails,
			final FutureCallback<File> resultCallback) throws IOException, HttpException {
		//nothing to do
	}

	@Override
	protected int capacityIncrement() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		Asserts.notNull(this.fileChannel, "File channel");
		writtenBytes += fileChannel.write(src);
		if (endOfStream) {
			releaseResources();
		}
		if (progressCallback != null) {
			progressCallback.progress(uri, writtenBytes, fileSize);
			if (progressCallback.isCancelled()) {
				resultCallback.cancelled();
			}
		}
	}

	@Override
	protected final void completed() throws IOException {
		releaseResources();
		if (resultCallback != null) {
			resultCallback.completed(file);
		}
	}

	@Override
	public void releaseResources() {
		if (accessfile != null) {
			try {
				accessfile.close();
			} catch (IOException ignore) {
				//ignore
			}
		}
	}

	@Override
	public final void failed(final Exception cause) {
		if (resultCallback != null) {
			resultCallback.failed(cause);
		}
		releaseResources();
	}

	@Override
	public final File getContent() {
		return file;
	}

	@Override
	public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
		//nothing to do
	}

	@Override
	public void consumeResponse(HttpResponse response, EntityDetails entityDetails, HttpContext context, FutureCallback<File> resultCallback) throws HttpException, IOException {
		if (response.getCode() != HttpStatus.SC_OK) {
			throw new ClientProtocolException("Connection to host failed: " + new StatusLine(response));
		}
		this.resultCallback = resultCallback;
		if (this.accessfile == null) {
			this.accessfile = new RandomAccessFile(this.file, "rw");
			this.fileChannel = this.accessfile.getChannel();
		}
		if (fileSize == 0) {
			Header h = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
			if (h != null && h.getValue() != null) {
				fileSize = Long.parseLong(h.getValue());
			} else {
				fileSize = entityDetails.getContentLength();
			}
		}
	}

}
