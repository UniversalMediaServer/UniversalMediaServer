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
package net.pms.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class IteratorsTest {

	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	@Test
	public void testIterators() {
		List<Integer> list1 = new ArrayList<>();
		list1.add(0);
		list1.add(1);
		List<Integer> list2 = new ArrayList<>();
		list2.add(0);
		list2.add(1);
		list2.add(2);
		Iterators<Integer> iterators = new Iterators<>();
		iterators.addList(list1);
		assertEquals("IteratorSize", iterators.size(), 2);
		Iterator<Integer> iterator = iterators.combinedIterator();
		int i = 0;
		while (iterator.hasNext()) {
			assertEquals("ListItem", iterator.next(), Integer.valueOf(i));
			i++;
		}
		iterators.addIterator(list2.iterator());
		iterator = iterators.combinedIterator();
		i = 0;
		boolean first = true;
		while (iterator.hasNext()) {
			assertEquals("ListItem", iterator.next(), Integer.valueOf(i));
			if (i == 1 && first) {
				i = 0;
				first = false;
			} else {
				i++;
			}
		}
		assertEquals("IteratorSize", iterators.size(), 5);
		iterators.clear();
		assertEquals("IteratorSize", iterators.size(), 0);
		iterators.addIterator(list2.iterator());
		iterators.addIterator(list1.iterator());
		assertEquals("IteratorSize", iterators.size(), 5);
		i = 0;
		while (iterator.hasNext()) {
			assertEquals("ListItem", iterator.next(), Integer.valueOf(i));
			if (i == 2) {
				i = 0;
			} else {
				i++;
			}
		}
	}
}
