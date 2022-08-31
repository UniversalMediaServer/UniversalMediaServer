/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.logging;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import net.pms.gui.GuiManager;

/**
 * Special Logback appender to 'print' log messages on the UMS GUI.
 *
 * @author thomas@innot.de
 * @param <E>
 */
public class GuiManagerAppender<E> extends UnsynchronizedAppenderBase<E> {
	private Encoder<E> encoder;

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

	/* (non-Javadoc)
	 * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
	 */
	@Override
	protected synchronized void append(E eventObject) {
		String msg = new String(encoder.encode(eventObject), StandardCharsets.UTF_8);
		GuiManager.appendLog(msg);
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
