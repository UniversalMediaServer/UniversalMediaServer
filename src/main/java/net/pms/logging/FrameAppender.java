/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2010  A.Brochard
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
package net.pms.logging;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.ErrorStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import net.pms.newgui.LooksFrame;
import net.pms.newgui.LooksFrame.LooksFrameUpdater;

/**
 * Special Logback appender to 'print' log messages on the PMS GUI.
 *
 * @author thomas@innot.de
 */
public class FrameAppender<E> extends UnsynchronizedAppenderBase<E> {
	private Encoder<E> encoder;
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
	private static final ReentrantLock lock = new ReentrantLock();
	private final List<String> buffer = new ArrayList<>();
	private static boolean looksFrameInitialized = false;

	/**
	 * Checks that the required parameters are set and if everything is in
	 * order, activates this appender.
	 */
	@Override
	public void start() {
		boolean error = true;

		lock.lock();
		try {
			if (this.encoder == null) {
				addStatus(
					new ErrorStatus(
						"No encoder set for the appender named [" + name + "].",
						this
					)
				);
			} else {
				try {
					encoder.init(outputStream);
					error = false;
				} catch (IOException ioe) {
					addStatus(
						new ErrorStatus(
							"Failed to initialize encoder for appender named [" + name + "].",
							this,
							ioe
						)
					);
				}
			}

			if (!error) {
				super.start();
			}
		} finally {
			lock.unlock();
		}
	}

	// Callback called by LooksFrame when the GUI has been initialized.
	public static void setFrameInitialized() {
		lock.lock();
		try {
			looksFrameInitialized = true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Queues the message for appending by the event dispatching thread
	 * @param msg the {@link String} to append
	 */
	private void doAppend(final String msg) {
		LooksFrame.updateGUI(new LooksFrameUpdater() {

			@Override
			protected Class<?> getLoggerClass() {
				return FrameAppender.class;
			}

			@Override
			protected String getCallerName() {
				return "doAppend";
			}

			@Override
			protected void doRun() {
				LooksFrame.get().append(msg);
			}
		});
	}

	/* (non-Javadoc)
	 * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
	 */
	@Override
	protected void append(E eventObject) {
		lock.lock();
		try {
			// if the frame hasn't been initialized yet,
			// buffer the messages until it's available
			encoder.doEncode(eventObject);
			String msg = outputStream.toString("UTF-8");
			outputStream.reset();

			if (!looksFrameInitialized) {
				buffer.add(msg);
			} else {
				if (!buffer.isEmpty()) {
					for (String buffered : buffer) { // drain the buffer
						doAppend(buffered);
					}
					buffer.clear();
				}

				doAppend(msg);
			}
		} catch (IOException ioe) {
			addStatus(new ErrorStatus("IO failure in appender", this, ioe));
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return The encoder associated with this appender, or <code>null</code>
	 *         if no encoder has been set.
	 */
	public Encoder<E> getEncoder() {
		lock.lock();
		try {
			return encoder;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Set the logback Encoder for the appender.
	 *
	 * Needs to be called (via the <encoder class="..."> element in the
	 * logback.xml config file) before the appender can be started.
	 *
	 * @param encoder
	 */
	public void setEncoder(Encoder<E> encoder) {
		lock.lock();
		try {
			this.encoder = encoder;
		} finally {
			lock.unlock();
		}
	}
}
