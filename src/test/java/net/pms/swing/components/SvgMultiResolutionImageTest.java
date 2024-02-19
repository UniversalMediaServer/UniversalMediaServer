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

import java.awt.Image;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Surf@ceS
 */
public class SvgMultiResolutionImageTest {

	public SvgMultiResolutionImageTest() {
	}

	@Test
	public void testSvgMultiResolutionImage() {
		try {
			SvgMultiResolutionImage test = new SvgMultiResolutionImage(SvgMultiResolutionImageTest.class.getResource("/resources/images/icon.svg"));
			Image base = test.getBaseImage();
			Assertions.assertEquals(32, base.getWidth(null), "Base Width");
			Assertions.assertEquals(32, base.getHeight(null), "Base Height");
			Image variant = test.getResolutionVariant(64, 64);
			Assertions.assertEquals(64, variant.getWidth(null), "Variant Width");
			Assertions.assertEquals(64, variant.getHeight(null), "Variant Height");
		} catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

}