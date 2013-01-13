/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockerFileInputStream extends UnusedInputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(BlockerFileInputStream.class);
	private static final int CHECK_INTERVAL = 1000;
	private long readCount;
	private long waitSize;
	private File file;
	private boolean firstRead;

	public BlockerFileInputStream(ProcessWrapper pw, File file, double waitSize) throws IOException {
		super(new FileInputStream(file), pw, 2000);
		this.file = file;
		this.waitSize = (long) (waitSize * 1048576);
		firstRead = true;
	}

	@Override
	public int read() throws IOException {
		if (checkAvailability()) {
			readCount++;
			int r = super.read();
			firstRead = false;
			return r;
		} else {
			return -1;
		}
	}

	private boolean checkAvailability() throws IOException {
		if (readCount > file.length()) {
			LOGGER.debug("File " + file.getAbsolutePath() + " is not that long!: " + readCount);
			return false;
		}
		int c = 0;
		long writeCount = file.length();
		long wait = firstRead ? waitSize : 100000;
		while (writeCount - readCount <= wait && c < 15) {
			if (c == 0) {
				LOGGER.trace("Suspend File Read: readCount=" + readCount + " / writeCount=" + writeCount);
			}
			c++;
			try {
				Thread.sleep(CHECK_INTERVAL);
			} catch (InterruptedException e) {
			}
			writeCount = file.length();
		}

		if (c > 0) {
			LOGGER.trace("Resume Read: readCount=" + readCount + " / writeCount=" + file.length());
		}
		return true;
	}

	public int available() throws IOException {
		return super.available();
	}

	public void close() throws IOException {
		super.close();
	}

	public long skip(long n) throws IOException {
		long l = super.skip(n);
		readCount += l;
		return l;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (checkAvailability()) {
			int r = super.read(b, off, len);
			firstRead = false;
			readCount += r;
			return r;
		} else {
			return -1;
		}
	}

	@Override
	public void unusedStreamSignal() {
		if (!file.delete()) {
			LOGGER.debug("Failed to delete \"" + file.getAbsolutePath() + "\"");
		}
	}
}
