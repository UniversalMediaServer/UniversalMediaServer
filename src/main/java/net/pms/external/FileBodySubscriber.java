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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileBodySubscriber implements Flow.Subscriber<List<ByteBuffer>> {

	private final File file;
	private final long fileSize;
	private final String uri;
	private final ProgressCallback progressCallback;
	private FileChannel fileChannel;
	private long writtenBytes = 0;
	private Flow.Subscription subscription;
	private final AtomicBoolean subscribed = new AtomicBoolean();
	private final CompletableFuture<File> result = new CompletableFuture<>();

	public FileBodySubscriber(File file, long fileSize, String uri, ProgressCallback progressCallback) {
		this.file = file;
		this.fileSize = fileSize;
		this.uri = uri;
		this.progressCallback = progressCallback;
	}

	private void releaseResources() {
		if (fileChannel != null) {
			try {
				fileChannel.close();
			} catch (IOException ignore) {
				//ignore
			}
		}
	}

	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		Objects.requireNonNull(subscription);
		if (!subscribed.compareAndSet(false, true)) {
			subscription.cancel();
			return;
		}
		try {
			this.fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		} catch (IOException ex) {
			result.completeExceptionally(ex);
			subscription.cancel();
			return;
		}
		this.subscription = subscription;
		subscription.request(1);
	}

	@Override
	public void onNext(List<ByteBuffer> items) {
		Objects.requireNonNull(items);
		try {
			writtenBytes += fileChannel.write(items.toArray(ByteBuffer[]::new));
		} catch (IOException ex) {
			releaseResources();
			subscription.cancel();
			result.completeExceptionally(ex);
		}
		if (progressCallback != null) {
			progressCallback.progress(uri, writtenBytes, fileSize);
			if (progressCallback.isCancelled()) {
				subscription.cancel();
			}
		}
		subscription.request(1);
	}

	@Override
	public void onError(Throwable throwable) {
		releaseResources();
		result.completeExceptionally(throwable);
	}

	@Override
	public void onComplete() {
		releaseResources();
		result.complete(file);
	}

}
