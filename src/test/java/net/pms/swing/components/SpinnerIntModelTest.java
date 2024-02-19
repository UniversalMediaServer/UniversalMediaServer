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
package net.pms.swing.components;

import net.pms.TestHelper;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpinnerIntModelTest {

	@BeforeEach
	public void setUp() {
		TestHelper.SetLoggingOff();
	}

	@Test
	public void testSpinnerIntModel() {
		SpinnerIntModel intModel = new SpinnerIntModel(65, 50, 250, 100);

		assertEquals(intModel.getIntValue(), 65, "InitValue");
		assertEquals(intModel.getMinimum(), 50, "LowerLimit");
		assertEquals(intModel.getMaximum(), 250, "UpperLimit");
		assertEquals(intModel.getStepSize(), 100, "StepSize");
		assertEquals(intModel.getNextValue(), 165, "NextValue");
		assertEquals(intModel.getPreviousValue(), 50, "PrevValue");
		intModel.setIntValue(50);
		assertEquals(intModel.getNextValue(), 100, "NextValue");
		assertEquals(intModel.getPreviousValue(), 50, "PrevValue");
		assertEquals(intModel.getValue(), 50, "CurrValue");
		intModel.setValue(intModel.getNextValue());
		intModel.setValue(intModel.getNextValue());
		intModel.setValue(intModel.getNextValue());
		assertEquals(intModel.getNextValue(), 250, "NextValue");
		assertEquals(intModel.getPreviousValue(), 200, "PrevValue");
		assertEquals(intModel.getValue(), 250, "CurrValue");
	}
}
