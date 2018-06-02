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
package net.pms.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OutputConsumer} implementation writing to a byte array. The byte
 * array will grow as needed, but as always: growing is expensive. A reasonable
 * default buffer size should be given in
 * {@code params.outputByteArrayStreamBufferSize}.
 *
 * @author Nadahar
 */

public class ByteArrayOutputStreamConsumer extends OutputConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ByteArrayOutputStreamConsumer.class);
	private final ReentrantReadWriteLock outputBufferLock = new ReentrantReadWriteLock();
	private BufferedOutputByteArrayImpl outputBuffer;
	private final int bufferSize;

	public ByteArrayOutputStreamConsumer(InputStream inputStream, OutputParams params) {
		super(inputStream);
		bufferSize = params.outputByteArrayStreamBufferSize > 512 ? params.outputByteArrayStreamBufferSize : 512;
		outputBuffer = new BufferedOutputByteArrayImpl(bufferSize);
	}

	@Override
	public void run() {
		try {
			byte[] bytes = new byte[Math.min(Math.max(512, bufferSize/8), 64000)];
			int n = 0;
			while ((n = inputStream.read(bytes)) > 0) {
				outputBufferLock.writeLock().lock();
				try {
					outputBuffer.write(bytes, 0, n);
				} finally {
					outputBufferLock.writeLock().unlock();
				}
			}
		} catch (IOException e) {
			LOGGER.debug("IO error while reading process output: {}", e.getMessage());
			LOGGER.trace("", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					LOGGER.debug("Error closing input stream: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			}
		}
	}

	@Override
	public BufferedOutputFile getBuffer() {
		outputBufferLock.readLock().lock();
		try {
			return outputBuffer;
		} finally {
			outputBufferLock.readLock().unlock();
		}
	}

	@Override
	public List<String> getResults() {
		return null;
	}
}
