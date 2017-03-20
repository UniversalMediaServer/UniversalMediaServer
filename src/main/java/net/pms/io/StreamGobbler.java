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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use one of the constructors to create a thread (non-blocking) that will
 * consume an {@link InputStream} and then die.
 *
 * Use the static methods to consume an {@link InputStream} in a blocking
 * manner in the calling thread's context.
 */
public class StreamGobbler extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamGobbler.class);
	BufferedReader in;
	private boolean logging;

	/**
	 * Create a new thread that when started will read and discard the {@link InputStream}.
	 * Use this when the stream has to be consumed in a non-blocking fashion.
	 *
	 * @param in the {@link InputStream} to be consumed
	 * @param enableLogging true if the stream content should be logged to TRACE level
	 */
	public StreamGobbler(InputStream in, boolean enableLogging) {
		this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		this.logging = enableLogging;
	}

	/**
	 * Create a new thread that when started will read and discard the {@link InputStream}.
	 * Use this when the stream has to be consumed in a non-blocking fashion.
	 *
	 * @param in the {@link InputStream} to be consumed
	 */
	public StreamGobbler(InputStream in) {
		this(in, false);
	}

	@Override
	public void run() {
		try {
			doGobble(in, logging);
		} catch (IOException e) {
			LOGGER.debug("Caught exception while gobbling stream: {}", e.getMessage());
			LOGGER.trace("", e);
		}

	}

	/**
	 * Read and discard the {@link InputStream} in the caller's context
	 * (blocking). Use this when you want to wait for the stream to be consumed.
	 * Note that you cannot handle more than one stream at a time with this,
	 * and thus any process output to be gobbled should be created with
	 * {@link ProcessBuilder} and it's error stream redirected.
	 *
	 * @param in the {@link InputStream} to be consumed
	 * @param enableLogging true if the stream content should be logged to TRACE level
	 *
	 * @throws IOException if any problems occur while consuming the stream
	 */
	public static void consumeThrow(InputStream in, boolean enableLogging) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		doGobble(reader, enableLogging);
	}

	/**
	 * Read and discard the {@link InputStream} in the caller's context
	 * (blocking). Use this when you want to wait for the stream to be consumed.
	 * Note that you cannot handle more than one stream at a time with this,
	 * and thus any process output to be gobbled should be created with
	 * {@link ProcessBuilder} and it's error stream redirected.
	 *
	 * @param in the {@link InputStream} to be consumed
	 * @param enableLogging true if the stream content should be logged to TRACE level
	 */
	public static void consume(InputStream in, boolean enableLogging) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		try {
			doGobble(reader, enableLogging);
		} catch (IOException e) {
			LOGGER.debug("Caught exception while gobbling stream: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Read and discard the {@link InputStream} in the caller's context
	 * (blocking). Use this when you want to wait for the stream to be consumed.
	 * Note that you cannot handle more than one stream at a time with this,
	 * and thus any process output to be gobbled should be created with
	 * {@link ProcessBuilder} and it's error stream redirected.
	 *
	 * @param in the {@link InputStream} to be consumed
	 */
	public static void consume(InputStream in) {
		consume(in, false);
	}

	private static void doGobble(BufferedReader reader, boolean enableLogging) throws IOException {
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				if (enableLogging && !line.startsWith("100")) {
					LOGGER.trace(line);
				}
			}
		} finally {
			reader.close();
		}
	}
}
