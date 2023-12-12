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
package net.pms;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class TestHelper {

	public static Logger getRootLogger() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		return context.getLogger(Logger.ROOT_LOGGER_NAME);
	}

	private static void SetLogging(Level level) {
		getRootLogger().setLevel(level);
	}

	/**
	 * Silence all log messages from the UMS code that are being tested.
	 */
	public static void SetLoggingOff() {
		SetLogging(Level.OFF);
	}

	public static void SetLoggingWarn() {
		SetLogging(Level.WARN);
	}

	public static void setLoggingTrace() {
		SetLogging(Level.TRACE);
	}

}
