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
package net.pms.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.Iterator;
import java.util.LinkedList;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special LogBack cacher that administers {@link CacheAppender}.
 *
 * @author Nadahar
 */
public class CacheLogger {

	private static Logger LOGGER = LoggerFactory.getLogger(CacheLogger.class);
	private static LinkedList<Appender<ILoggingEvent>> appenderList = new LinkedList<>();
	private static volatile CacheAppender<ILoggingEvent> cacheAppender = null;
	private static LoggerContext loggerContext = null;
	private static ch.qos.logback.classic.Logger rootLogger;

	private static void detachRootAppenders() {
		Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
		while (it.hasNext()) {
			Appender<ILoggingEvent> appender = it.next();
			if (appender != cacheAppender) {
				appenderList.add(appender);
				rootLogger.detachAppender(appender);
			}
		}
	}

	private static void attachRootAppenders() {
		while (!appenderList.isEmpty()) {
			Appender<ILoggingEvent> appender = appenderList.poll();
			rootLogger.addAppender(appender);
		}
	}

	private static void disposeOfAppenders() {
		appenderList.clear();
	}

	/**
	 * @return whether or not CacheLogger is currently running
	 */
	public static boolean isActive() {
		return cacheAppender != null;
	}

	/**
	 * Sets references to the LoggerContext. Must be called whenever logging
	 * configuration changes between {@link #startCaching()} and {@link #stopAndFlush()}
	 */
	public static synchronized void initContext() {
		ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
		if (!(iLoggerFactory instanceof LoggerContext)) {
			// Not using LogBack, CacheAppender not applicable
			LOGGER.debug("Not using LogBack, aborting CacheLogger");
			loggerContext = null;
			return;
		} else if (!isActive()) {
			LOGGER.error("initContext() cannot be called while isActive() is false");
			return;
		}

		loggerContext = (LoggerContext) iLoggerFactory;
		rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		disposeOfAppenders();
		detachRootAppenders();
		if (!rootLogger.isAttached(cacheAppender)) {
			rootLogger.addAppender(cacheAppender);
		}
		cacheAppender.setContext(loggerContext);
		cacheAppender.setName("CacheAppender");
		cacheAppender.start();
	}

	public static synchronized void startCaching() {
		if (isActive()) {
			LOGGER.debug("StartCaching() failed: Caching already started");
		} else {
			cacheAppender = new CacheAppender<>();
			initContext();
		}
	}

	public static synchronized void stopAndFlush() {
		if (loggerContext == null) {
			LOGGER.debug("Not using LogBack, aborting CacheLogger.stopAndFlush()");
			return;
		} else if (!isActive()) {
			LOGGER.error("stopAndFlush() cannot be called while isActive() is false");
			return;
		}

		cacheAppender.stop();
		rootLogger.detachAppender(cacheAppender);
		attachRootAppenders();
		cacheAppender.flush(rootLogger);
		cacheAppender = null;
	}

	public static synchronized Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
		return appenderList.iterator();
	}

	public static synchronized void addAppender(Appender<ILoggingEvent> newAppender) {
		appenderList.add(newAppender);
	}

	public static synchronized boolean removeAppender(Appender<ILoggingEvent> appender) {
		return appenderList.remove(appender);
	}
}
