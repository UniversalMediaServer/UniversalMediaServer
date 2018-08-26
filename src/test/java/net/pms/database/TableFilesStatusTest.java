/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2012  I. Sokolov
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
package net.pms.database;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableFilesStatusTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@Before
	public final void setUp() throws ConfigurationException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

	/**
	 * This should get more specific, since for now it
	 * just makes sure the code completes without errors but
	 * doesn't really do anything.
	 */
	@Test
	public void testUpgrade() throws Exception {
		Tables.checkTables();
	}

	@Test
	public void testIsFullyPlayed() throws Exception {
		TableFilesStatus.setFullyPlayed("FileThatHasBeenPlayed", true);
		TableFilesStatus.setFullyPlayed("FileThatHasBeenMarkedNotPlayed", false);
		assertThat(TableFilesStatus.isFullyPlayed("FileThatDoesntExist")).isNull();
		assertThat(TableFilesStatus.isFullyPlayed("FileThatHasBeenPlayed")).isTrue();
		assertThat(TableFilesStatus.isFullyPlayed("FileThatHasBeenMarkedNotPlayed")).isFalse();
	}
}
