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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputBufferConsumer extends OutputConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OutputBufferConsumer.class);
	private BufferedOutputFile outputBuffer;
	
	/**
	 * Size of a buffer in bytes. The buffer is used to copy data from an
	 * {@link java.io.InputStream InputStream} to an {@link java.io.OutputStream OutputStream}
	 * such as {@link net.pms.io.BufferedOutputFile BufferedOutputFile}.
	 * <p>
	 * It is unknown up front how many bytes will be read at once by
	 * {@link java.io.InputStream#read(byte[]) read(byte[])}, but it will never be more than
	 * the buffer size that we define here. Tests show varying numbers between 2048 and 450560
	 * being copied, with 8192 being most commonly used, probably because that is the default
	 * size for {@link org.jboss.netty.channel.Channel Channel} packets.
	 */
	private static final int PIPE_BUFFER_SIZE = 500000;

	public OutputBufferConsumer(InputStream inputStream, OutputParams params) {
		super(inputStream);
		outputBuffer = new BufferedOutputFileImpl(params);
	}

	@Override
	public void run() {
		try {
			//LOGGER.trace("Starting read from pipe");
			byte buf[] = new byte[PIPE_BUFFER_SIZE];
			int n = 0;
			while ((n = inputStream.read(buf)) > 0) {
				//LOGGER.trace("Fetched " + n + " from pipe");
				outputBuffer.write(buf, 0, n);
			}
			//LOGGER.debug("Finished to read");
		} catch (IOException ioe) {
			LOGGER.debug("Error consuming stream of spawned process: " + ioe.getMessage());
		} finally {
			//LOGGER.trace("Closing read from pipe");
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					LOGGER.debug("Caught exception", e);
				}
			}
		}
	}

	@Override
	public BufferedOutputFile getBuffer() {
		return outputBuffer;
	}

	@Override
	public List<String> getResults() {
		return null;
	}
}
