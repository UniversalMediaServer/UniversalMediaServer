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

import java.io.IOException;
import java.io.InputStream;

class WaitBufferedInputStream extends InputStream {
	private BufferedOutputFile outputStream;
	private long readCount;
	private boolean firstRead;

	public void setReadCount(long readCount) {
		this.readCount = readCount;
	}

	public long getReadCount() {
		return readCount;
	}
	
	WaitBufferedInputStream(BufferedOutputFile outputStream) {
		this.outputStream = outputStream;
		firstRead = true;
	}

	public int read() throws IOException {
		int r = outputStream.read(firstRead, getReadCount());
		if (r != -1) {
			setReadCount(getReadCount() + 1);
		}
		firstRead = false;
		return r;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int returned = outputStream.read(firstRead, getReadCount(), b, off, len);
		if (returned != -1) {
			setReadCount(getReadCount() + returned);
		}
		firstRead = false;
		return returned;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int available() throws IOException {
		return (int) outputStream.getWriteCount();
	}

	public void close() throws IOException {
		outputStream.removeInputStream(this);
		outputStream.detachInputStream();
	}
}

