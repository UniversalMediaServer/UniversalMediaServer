/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.logging;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger that debounces every 300ms.
 * Useful for things that get spammed many times but never change, like
 * configuration state logging.
 */
public class DebounceTraceLogger {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebounceTraceLogger.class);

	private Timer timer = new Timer();
	private long debounceDelay = 300; // 300ms

	public void log(String logMessage) {
		try {
			timer.cancel(); // Cancel any existing timer
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					LOGGER.trace(logMessage);
				}
			}, debounceDelay);
		} catch (Exception e) {
			// don't log that the timer is already cancelled
		}
	}
}