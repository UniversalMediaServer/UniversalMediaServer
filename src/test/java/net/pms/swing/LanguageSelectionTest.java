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
package net.pms.swing;

import java.util.Locale;
import net.pms.TestHelper;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LanguageSelectionTest {

	@BeforeEach
	public void setUp() throws ConfigurationException {
		TestHelper.SetLoggingOff();
	}

	@Test
	public void LanguageSelectionConstructorThrowTest() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LanguageSelection(null, null, false);
		});
	}

	@Test
	public void LanguageSelectionClassTest() {
		LanguageSelection languageSelection;
		languageSelection = new LanguageSelection(null, Locale.US, false);
		assertFalse(languageSelection.isAborted(), "isAbortedIsFalseByDefault");
	}

}
