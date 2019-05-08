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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;

/**
 * Copy of the {@code org.apache.http.ZeroCopyConsumer}.
 * Added the {@code invokeCallback} method to inform
 * the calling process about the progress of downloading the file.
 * 
 * @author valib
 * 
 */
public abstract class ZeroCopyConsumerWithCallback<T> extends AbstractAsyncResponseConsumer<T> {

    private final File file;
    private final RandomAccessFile accessfile;
    private UriRetrieverCallback callback;
    private String uri;
    private HttpResponse response;
    private ContentType contentType;
    private Header contentEncoding;
    private FileChannel fileChannel;
    private long idx = -1;
    private long fileSize;

    public ZeroCopyConsumerWithCallback(final File file, String uri, UriRetrieverCallback callback) throws FileNotFoundException {
        super();
        if (file == null) {
            throw new IllegalArgumentException("File may nor be null");
        }
        this.file = file;
        this.uri = uri;
        this.callback = callback;
        this.accessfile = new RandomAccessFile(this.file, "rw");
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) {
        this.response = response;
    }

    @Override
    protected void onEntityEnclosed(
            final HttpEntity entity, final ContentType contentType) throws IOException {
        this.contentType = contentType;
        this.contentEncoding = entity.getContentEncoding();
        this.fileChannel = this.accessfile.getChannel();
        this.fileSize = this.fileChannel.size();
        this.idx = 0;
    }

    @Override
    protected void onContentReceived(
            final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
        Asserts.notNull(this.fileChannel, "File channel");
        final long transferred;
        if (decoder instanceof FileContentDecoder) {
            transferred = ((FileContentDecoder)decoder).transfer(
                    this.fileChannel, this.idx, Integer.MAX_VALUE);
        } else {
            transferred = this.fileChannel.transferFrom(
                    new ContentDecoderChannel(decoder), this.idx, Integer.MAX_VALUE);
        }
        if (transferred > 0) {
            this.idx += transferred;
        }
        if (decoder.isCompleted()) {
            this.fileChannel.close();
        }

		invokeCallback(this.uri, this.callback, (int) this.fileSize, (int) this.idx);
    }

    /**
     * Invoked to process received file.
     *
     * @param response original response head.
     * @param file file containing response content.
     * @param contentType the cotnent type.
     * @return result of the response processing
     */
    protected abstract T process(
            HttpResponse response, File file, ContentType contentType) throws Exception;

    @Override
    protected T buildResult(final HttpContext context) throws Exception {
        final FileEntity entity = new FileEntity(this.file, this.contentType);
        entity.setContentEncoding(this.contentEncoding);
        this.response.setEntity(entity);
        return process(this.response, this.file, this.contentType);
    }

    @Override
    protected void releaseResources() {
        try {
            this.accessfile.close();
        } catch (final IOException ignore) {
        }
    }

	static void invokeCallback(String uri, UriRetrieverCallback callback, int totalBytes, int bytesWritten) throws IOException {
		try {
			callback.progressMade(uri, bytesWritten, totalBytes);
		} catch (UriRetrieverCallback.CancelDownloadException e) {
			throw new IOException("Download was cancelled");
		}
	}
}
