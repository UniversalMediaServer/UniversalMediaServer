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

/**
 * Interface to easily subclass different implementations while keeping most of
 * the other PMS code the same. The original code has been moved to
 * {@link BufferedOutputFileImpl}.
 */
public interface BufferedOutputFile {

	public abstract void close() throws IOException;

	public abstract WaitBufferedInputStream getCurrentInputStream();

	public abstract InputStream getInputStream(long newReadPosition);

	public abstract long getWriteCount();

	public abstract void write(byte b[], int off, int len) throws IOException;

	public abstract void write(int b) throws IOException;

	public abstract void attachThread(ProcessWrapper thread);

	public abstract void reset();

	public abstract int read(boolean firstRead, long readCount);

	public abstract int read(boolean firstRead, long readCount, byte[] b,
			int off, int len);

	public abstract void removeInputStream(
			WaitBufferedInputStream waitBufferedInputStream);

	public abstract void detachInputStream();

	public abstract void write(byte[] byteArray) throws IOException;

}