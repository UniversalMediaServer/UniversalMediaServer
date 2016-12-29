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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class can be used as a "drop in replacement" for
 * {@link BufferedInputStream} if you want to alter the buffering behavior.
 * This will fake the behavior of a {@link BufferedInputStream} with its own
 * "mark position" while keeping the real mark at 0. This means that the start
 * position won't be invalidated just because a mark is placed later than at
 * position 0. {@code marklimit} in {@link #mark(int)} is ignored and the value
 * given in the constructor is used instead, meaning that this can be passed to
 * other methods without the danger of them invalidating the start position by
 * specifying a too small {@code marklimit}.<br>
 * <br>
 * This also offers two additional methods {@link #isFullResetAvailable()} and
 * {@link #fullReset()}. A full reset will ignore any marks set and reset the
 * read position to the start.<br>
 * <br>
 * The intended use for this class is to use an {@link InputStream} instance
 * for multiple purposes without having to read the whole stream into memory,
 * for example when parsing information early in the stream with multiple
 * parsers.
 *
 * @author Nadahar
 */
public class ResettableInputStream extends BufferedInputStream {

	protected int overriddenMarkpos = 0;

	/**
     * Creates a {@link ResettableInputStream} and saves its argument, the
     * input stream {@code in}, for later use. An internal buffer array is
     * created and stored in {@code buf}.
     *
     * @param in the underlying input stream.
	 * @param marklimit the maximum size to buffer before the mark/start point
	 *                  gets invalidated. This overrides the parameter sent
	 *                  when calling {@link #mark(int)}.
	 */
	public ResettableInputStream(InputStream in, int marklimit) {
		super(in);
		this.marklimit = marklimit;
		markpos = 0;
	}

    /**
     * Creates a {@link ResettableInputStream} with the specified buffer
     * size, and saves its argument, the input stream {@code in}, for later
     * use.  An internal buffer array of length {@code size} is created and
     * stored in {@code buf}.
     *
     * @param in the underlying input stream.
     * @param size the initial buffer size.
	 * @param marklimit the maximum size to buffer before the mark/start point
	 *                  gets invalidated. This overrides the parameter sent
	 *                  when calling {@link #mark(int)}.
     * @exception IllegalArgumentException if {@code size} <= 0.
     */
	public ResettableInputStream(InputStream in, int size, int marklimit) {
		super(in, size);
		this.marklimit = marklimit;
		markpos = 0;
	}

    /**
     * See the general contract of {@link InputStream#mark(int)}.
     *
     * @param readlimit - ignored.
     * @see ResettableInputStream#reset()
     */
	@Override
	public synchronized void mark(int readlimit) {
		//Ignore readlimit, we adhere to the constructor limit
        overriddenMarkpos = pos;
	}

    /**
     * See the general contract of {@link InputStream#reset()}.
     * <p>
     * If {@code markpos} is {@code -1} (the start position has been
     * invalidated), an {@link IOException} is thrown. Otherwise, {@code pos}
     * is set equal to {@code markpos}.
     *
     * @exception IOException if the mark has been invalidated, or the stream
     *                        has been closed by invoking its {@link #close()}
     *                        method, or an I/O error occurs.
     * @see ResettableInputStream#mark(int)
     */
	@Override
	public synchronized void reset() throws IOException {
		super.reset();
		if (overriddenMarkpos < 0) {
			overriddenMarkpos = 0;
		}
        pos = overriddenMarkpos;
	}

	/**
	 * @return Whether a call to {@link #fullReset()} will succeed.
	 */
	public synchronized boolean isFullResetAvailable() {
		return markpos == 0;
	}

	/**
	 * Resets the stream read position to the start if possible.
	 *
	 * @throws IOException if the full reset fails.
	 */
	public synchronized void fullReset() throws IOException {
		super.reset();
		pos = 0;
	}
}
