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
package net.pms.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

/**
 * The default UMS thread factory.
 */
public class SimpleThreadFactory implements ThreadFactory {
	private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final ThreadGroup group;

	public SimpleThreadFactory(String name) {
		this(name, null, Thread.MAX_PRIORITY, false);
	}

	public SimpleThreadFactory(String name, String groupName) {
		this(name, groupName, Thread.MAX_PRIORITY, false);
	}

	public SimpleThreadFactory(String name, String groupName, int maxPriority) {
		this(name, groupName, maxPriority, false);
	}

	public SimpleThreadFactory(String name, boolean logPoolNumber) {
		this(name, null, Thread.MAX_PRIORITY, logPoolNumber);
	}

	public SimpleThreadFactory(String name, String groupName, boolean logPoolNumber) {
		this(name, groupName, Thread.MAX_PRIORITY, logPoolNumber);
	}

	public SimpleThreadFactory(String name, String groupName, int maxPriority, boolean logPoolNumber) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("name cannot be blank");
		}
		if (StringUtils.isNotBlank(groupName)) {
			group = new ThreadGroup(groupName);
			group.setMaxPriority(maxPriority);
		} else {
			group = null;
		}

		if (logPoolNumber) {
			namePrefix = name + " " + POOL_NUMBER.getAndIncrement() + "-";
		} else {
			namePrefix = name + " ";
		}
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(
			group,
			r,
			namePrefix + threadNumber.getAndIncrement(),
			0
		);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}
}
