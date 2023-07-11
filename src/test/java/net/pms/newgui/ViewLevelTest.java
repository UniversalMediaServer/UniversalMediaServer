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
package net.pms.newgui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class ViewLevelTest {

	@BeforeEach
	public void setUp() throws ConfigurationException {
		// Silence all log messages from the UMS code that are being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
	}

	/**
	 * Test ViewLevel class
	 */
	@Test
	public void testViewLevel() {
		// Test level values
		assertEquals(ViewLevel.NORMAL_INT, 0, "ViewLevelNormalInt");
		assertEquals(ViewLevel.NORMAL_INTEGER, Integer.valueOf(0), "ViewLevelNormalInteger");
		assertEquals(ViewLevel.ADVANCED_INT, 200, "ViewLevelAdvancedInt");
		assertEquals(ViewLevel.ADVANCED_INTEGER, Integer.valueOf(200), "ViewLevelAdvancedInteger");
		assertEquals(ViewLevel.EXPERT_INT, 500, "ViewLevelExpertInt");
		assertEquals(ViewLevel.EXPERT_INTEGER, Integer.valueOf(500), "ViewLevelExpertInteger");
		assertEquals(ViewLevel.DEVELOPER_INT, 1000, "ViewLevelDeveloperInt");
		assertEquals(ViewLevel.DEVELOPER_INTEGER, Integer.valueOf(1000), "ViewLevelDeveloperInteger");
		assertEquals(ViewLevel.UNKNOWN_INT, Integer.MIN_VALUE, "ViewLevelUnknownInt");
		assertEquals(ViewLevel.UNKNOWN_INTEGER, Integer.valueOf(Integer.MIN_VALUE), "ViewLevelUnknownInteger");

		// Test instance creation
		assertEquals(ViewLevel.NORMAL.toInt(), ViewLevel.NORMAL_INT, "ViewLevelNormal");
		assertEquals(ViewLevel.ADVANCED.toInt(), ViewLevel.ADVANCED_INT, "ViewLevelAdvanced");
		assertEquals(ViewLevel.EXPERT.toInt(), ViewLevel.EXPERT_INT, "ViewLevelExpert");
		assertEquals(ViewLevel.DEVELOPER.toInt(), ViewLevel.DEVELOPER_INT, "ViewLevelDeveloper");
		assertEquals(ViewLevel.UNKNOWN.toInt(), ViewLevel.UNKNOWN_INT, "ViewLevelUnknown");

		// Test Integer mapping
		assertEquals(ViewLevel.NORMAL.toInteger(), Integer.valueOf(ViewLevel.NORMAL_INT), "ViewLevelNormalIntegerMapping");
		assertEquals(ViewLevel.ADVANCED.toInteger(), Integer.valueOf(ViewLevel.ADVANCED_INT), "ViewLevelAdvancedIntegerMapping");
		assertEquals(ViewLevel.EXPERT.toInteger(), Integer.valueOf(ViewLevel.EXPERT_INT), "ViewLevelExpertIntegerMapping");
		assertEquals(ViewLevel.DEVELOPER.toInteger(), Integer.valueOf(ViewLevel.DEVELOPER_INT), "ViewLevelDeveloperIntegerMapping");
		assertEquals(ViewLevel.UNKNOWN.toInteger(), Integer.valueOf(ViewLevel.UNKNOWN_INT), "ViewLevelUnknownIntegerMapping");

		// Test String values
		assertEquals(ViewLevel.NORMAL.toString(), "Normal", "ViewLevelNormalString");
		assertEquals(ViewLevel.ADVANCED.toString(), "Advanced", "ViewLevelAdvancedString");
		assertEquals(ViewLevel.EXPERT.toString(), "Expert", "ViewLevelExpertString");
		assertEquals(ViewLevel.DEVELOPER.toString(), "Developer", "ViewLevelDeveloperString");
		assertEquals(ViewLevel.UNKNOWN.toString(), "Unknown", "ViewLevelUnknownString");

		// Test toViewLevel(int val)
		assertEquals(ViewLevel.toViewLevel(0), ViewLevel.NORMAL, "IntToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel(200), ViewLevel.ADVANCED, "IntToViewLevelAdvanced");
		assertEquals(ViewLevel.toViewLevel(500), ViewLevel.EXPERT, "IntToViewLevelExpert");
		assertEquals(ViewLevel.toViewLevel(1000), ViewLevel.DEVELOPER, "IntToViewLevelDeveloper");
		assertEquals(ViewLevel.toViewLevel(Integer.MIN_VALUE), ViewLevel.UNKNOWN, "IntToViewLevelUnknown");
		assertEquals(ViewLevel.toViewLevel(100), ViewLevel.UNKNOWN, "IllegalIntToViewLevel");
		assertEquals(ViewLevel.toViewLevel(-400), ViewLevel.UNKNOWN, "IllegalIntToViewLevel");
		assertEquals(ViewLevel.toViewLevel(Integer.MAX_VALUE), ViewLevel.UNKNOWN, "IllegalIntToViewLevel");

		// Test toViewLevel(String sArg)
		assertEquals(ViewLevel.toViewLevel("Normal"), ViewLevel.NORMAL, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("nORMAL"), ViewLevel.NORMAL, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("Advanced"), ViewLevel.ADVANCED, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("aDVANCED"), ViewLevel.ADVANCED, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("Expert"), ViewLevel.EXPERT, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("eXPERT"), ViewLevel.EXPERT, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("Developer"), ViewLevel.DEVELOPER, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("dEVELOPER"), ViewLevel.DEVELOPER, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("Unknown"), ViewLevel.UNKNOWN, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("uNKNOWN"), ViewLevel.UNKNOWN, "StringToViewLevelNormal");
		assertEquals(ViewLevel.toViewLevel("Foo"), ViewLevel.UNKNOWN, "IllegalStringToViewLevel");
		assertEquals(ViewLevel.toViewLevel("BAR"), ViewLevel.UNKNOWN, "IllegalStringToViewLevel");
	}
}
