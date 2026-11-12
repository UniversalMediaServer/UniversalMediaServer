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
package net.pms.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.pms.PMS;
import net.pms.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlatHeadsetRendererTest {
	private UmsConfiguration previous;

	@BeforeEach
	void configure() {
		PMS.configureJNA();
		TestHelper.setLoggingOff();
		previous = PMS.getConfiguration();
	}

	@AfterEach
	void restore() {
		PMS.setConfiguration(previous);
	}

	@Test
	public void flatHeadsetProfileIsRecognizedAndConfiguredForStereo() throws Exception {
		PMS.setConfiguration(new UmsConfiguration(false));
		RendererConfigurations.loadRendererConfigurations();

		RendererConfiguration renderer = RendererConfigurations.getRendererConfigurationByUPNPDetails(
			"{friendlyName=Flat Headset, address=127.0.0.1, udn=uuid:00000000-0000-0000-0000-000000000000, " +
			"manufacturer=KDE contributors, modelName=Flat Headset, modelDescription=Flat Headset, " +
			"manufacturerURL=http://example.invalid/, modelURL=http://example.invalid/, FlatHeadset/1.0 Panel/3840x2160}"
		);

		assertNotNull(renderer, "Flat Headset UPnP details were not recognized");
		assertEquals("Flat Headset", renderer.getRendererName());
		assertEquals(3840, renderer.getMaxVideoWidth());
		assertEquals(2160, renderer.getMaxVideoHeight());
		assertEquals(2, renderer.getLoadingPriority());
		assertEquals("sbsl", renderer.getOutput3DFormat());

		for (String details : new String[] {
			"manufacturer=kde contributors , modelName=flat headset , modelNumber=FlatHeadset/1.0 Panel/1920x1080",
			"manufacturer=KDE contributors, modelName=Flat Headset, modelNumber=FlatHeadset/1.0 Panel/3840x2160"
		}) {
			assertEquals("Flat Headset", RendererConfigurations.getRendererConfigurationByUPNPDetails(details).getRendererName());
		}

		assertNotEquals(renderer, RendererConfigurations.getRendererConfigurationByUPNPDetails(
			"manufacturer=Other vendor, modelName=Flat Headset, modelNumber=FlatHeadset/1.0 Panel/3840x2160"));
		assertNotEquals(renderer, RendererConfigurations.getRendererConfigurationByUPNPDetails(
			"manufacturer=KDE contributors, modelName=Other viewer, modelNumber=FlatHeadset/1.0 Panel/3840x2160"));
	}

}
