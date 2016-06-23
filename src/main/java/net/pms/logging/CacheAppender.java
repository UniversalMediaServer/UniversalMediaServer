/*
 * Universal Media Server, for streaming any medias to DLNA
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.status.ErrorStatus;
import java.util.LinkedList;

/**
 * Special LogBack appender that simply caches all messages and repost them
 * when told to by {@link#flush} (typically after LogBack configuration is complete).
 *
 * @author Nadahar
 */
public class CacheAppender<E> extends AppenderBase<E> {

	private final Object eventListLock = new Object();
	private LinkedList<E> eventList = new LinkedList<>();

	@Override
	protected void append(E eventObject) {
		try {
			synchronized (eventListLock) {
				eventList.add(eventObject);
			}
		} catch (Exception e) {
			addStatus(new ErrorStatus(
						getName() + " failed to append event: " + e.getLocalizedMessage(), this, e)
			);
		}
	}

	public void flush(Logger rootLogger) {
		synchronized (eventListLock) {
			while (!eventList.isEmpty()) {
				rootLogger.callAppenders((ILoggingEvent) eventList.poll());
			}
		}
	}
}
