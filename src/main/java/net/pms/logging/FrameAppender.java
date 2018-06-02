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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.pms.newgui.IFrame;

/**
 * Special Logback appender to 'print' log messages on the UMS GUI.
 *
 * @author thomas@innot.de
 */
public class FrameAppender<E> extends UnsynchronizedAppenderBase<E> {
	private static IFrame frame;
	private Encoder<E> encoder;
	private final List<String> buffer = new ArrayList<>();

	/**
	 * Checks that the required parameters are set and if everything is in
	 * order, activates this appender.
	 */
	@Override
	public void start() {
		boolean error = false;

		if (this.encoder == null) {
			error = true;
			addStatus(
				new ErrorStatus(
					"No encoder set for the appender named [" + name + "].",
					this
				)
			);
		}

		if (!error) {
			super.start();
		}
	}

	// Callback called by UMS when the GUI (or dummy GUI) has been initialised.
	// everywhere else in the codebase accesses the frame via PMS.get().getFrame(),
	// but we can't do that here as UMS is in the process of constructing
	// the instance that PMS.get() returns when this class is instantiated.
	public static void setFrame(IFrame iframe) {
		frame = iframe;
	}

	/* (non-Javadoc)
	 * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
	 */
	@Override
	protected synchronized void append(E eventObject) {
		// if the frame hasn't been initialized yet,
		// buffer the messages until it's available
		String msg = new String(encoder.encode(eventObject), StandardCharsets.UTF_8);

		if (frame == null) {
			buffer.add(msg);
		} else {
			if (!buffer.isEmpty()) {
				for (String buffered : buffer) { // drain the buffer
					frame.append(buffered);
				}

				buffer.clear();
			}

			frame.append(msg);
		}
	}

	/**
	 * @return The encoder associated with this appender, or <code>null</code>
	 *         if no encoder has been set.
	 */
	public Encoder<E> getEncoder() {
		return encoder;
	}

	/**
	 * Set the logback Encoder for the appender.
	 *
	 * Needs to be called (via the <encoder class="..."> element in the
	 * logback.xml config file) before the appender can be started.
	 *
	 * @param encoder the {@link Encoder} to use.
	 */
	public void setEncoder(Encoder<E> encoder) {
		this.encoder = encoder;
	}
}
