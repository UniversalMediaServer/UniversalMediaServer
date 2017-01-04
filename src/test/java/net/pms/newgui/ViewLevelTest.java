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
package net.pms.newgui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class ViewLevelTest {

	@Before
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	/**
	 * Test ViewLevel class
	 */
	@Test
	public void testViewLevel() {
		// Test level values
		assertEquals("ViewLevelNormalInt", ViewLevel.NORMAL_INT, 0);
		assertEquals("ViewLevelNormalInteger", ViewLevel.NORMAL_INTEGER, Integer.valueOf(0));
		assertEquals("ViewLevelAdvancedInt", ViewLevel.ADVANCED_INT, 200);
		assertEquals("ViewLevelAdvancedInteger", ViewLevel.ADVANCED_INTEGER, Integer.valueOf(200));
		assertEquals("ViewLevelExpertInt", ViewLevel.EXPERT_INT, 500);
		assertEquals("ViewLevelExpertInteger", ViewLevel.EXPERT_INTEGER, Integer.valueOf(500));
		assertEquals("ViewLevelDeveloperInt", ViewLevel.DEVELOPER_INT, 1000);
		assertEquals("ViewLevelDeveloperInteger", ViewLevel.DEVELOPER_INTEGER, Integer.valueOf(1000));
		assertEquals("ViewLevelUnknownInt", ViewLevel.UNKNOWN_INT, Integer.MIN_VALUE);
		assertEquals("ViewLevelUnknownInteger", ViewLevel.UNKNOWN_INTEGER, Integer.valueOf(Integer.MIN_VALUE));

		// Test instance creation
		assertEquals("ViewLevelNormal", ViewLevel.NORMAL.toInt(), ViewLevel.NORMAL_INT);
		assertEquals("ViewLevelAdvanced", ViewLevel.ADVANCED.toInt(), ViewLevel.ADVANCED_INT);
		assertEquals("ViewLevelExpert", ViewLevel.EXPERT.toInt(), ViewLevel.EXPERT_INT);
		assertEquals("ViewLevelDeveloper", ViewLevel.DEVELOPER.toInt(), ViewLevel.DEVELOPER_INT);
		assertEquals("ViewLevelUnknown", ViewLevel.UNKNOWN.toInt(), ViewLevel.UNKNOWN_INT);

		// Test Integer mapping
		assertEquals("ViewLevelNormalIntegerMapping", ViewLevel.NORMAL.toInteger(), Integer.valueOf(ViewLevel.NORMAL_INT));
		assertEquals("ViewLevelAdvancedIntegerMapping", ViewLevel.ADVANCED.toInteger(), Integer.valueOf(ViewLevel.ADVANCED_INT));
		assertEquals("ViewLevelExpertIntegerMapping", ViewLevel.EXPERT.toInteger(), Integer.valueOf(ViewLevel.EXPERT_INT));
		assertEquals("ViewLevelDeveloperIntegerMapping", ViewLevel.DEVELOPER.toInteger(), Integer.valueOf(ViewLevel.DEVELOPER_INT));
		assertEquals("ViewLevelUnknownIntegerMapping", ViewLevel.UNKNOWN.toInteger(), Integer.valueOf(ViewLevel.UNKNOWN_INT));

		// Test String values
		assertEquals("ViewLevelNormalString", ViewLevel.NORMAL.toString(), "Normal");
		assertEquals("ViewLevelAdvancedString", ViewLevel.ADVANCED.toString(), "Advanced");
		assertEquals("ViewLevelExpertString", ViewLevel.EXPERT.toString(), "Expert");
		assertEquals("ViewLevelDeveloperString", ViewLevel.DEVELOPER.toString(), "Developer");
		assertEquals("ViewLevelUnknownString", ViewLevel.UNKNOWN.toString(), "Unknown");

		// Test toViewLevel(int val)
		assertEquals("IntToViewLevelNormal", ViewLevel.toViewLevel(0), ViewLevel.NORMAL);
		assertEquals("IntToViewLevelAdvanced", ViewLevel.toViewLevel(200), ViewLevel.ADVANCED);
		assertEquals("IntToViewLevelExpert", ViewLevel.toViewLevel(500), ViewLevel.EXPERT);
		assertEquals("IntToViewLevelDeveloper", ViewLevel.toViewLevel(1000), ViewLevel.DEVELOPER);
		assertEquals("IntToViewLevelUnknown", ViewLevel.toViewLevel(Integer.MIN_VALUE), ViewLevel.UNKNOWN);
		assertEquals("IllegalIntToViewLevel", ViewLevel.toViewLevel(100), ViewLevel.UNKNOWN);
		assertEquals("IllegalIntToViewLevel", ViewLevel.toViewLevel(-400), ViewLevel.UNKNOWN);
		assertEquals("IllegalIntToViewLevel", ViewLevel.toViewLevel(Integer.MAX_VALUE), ViewLevel.UNKNOWN);

		// Test toViewLevel(String sArg)
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("Normal"), ViewLevel.NORMAL);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("nORMAL"), ViewLevel.NORMAL);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("Advanced"), ViewLevel.ADVANCED);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("aDVANCED"), ViewLevel.ADVANCED);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("Expert"), ViewLevel.EXPERT);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("eXPERT"), ViewLevel.EXPERT);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("Developer"), ViewLevel.DEVELOPER);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("dEVELOPER"), ViewLevel.DEVELOPER);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("Unknown"), ViewLevel.UNKNOWN);
		assertEquals("StringToViewLevelNormal", ViewLevel.toViewLevel("uNKNOWN"), ViewLevel.UNKNOWN);
		assertEquals("IllegalStringToViewLevel", ViewLevel.toViewLevel("Foo"), ViewLevel.UNKNOWN);
		assertEquals("IllegalStringToViewLevel", ViewLevel.toViewLevel("BAR"), ViewLevel.UNKNOWN);
	}
}
