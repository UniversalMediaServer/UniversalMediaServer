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
package net.pms.encoders;

import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.service.Services;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author Surf@ceS
 */
public class TranscodingSettingsTest {

	@BeforeAll
	public static void setUpClass() {
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
			PMS.getConfiguration().setExternalNetwork(false);
			Services.destroy();
			Services.create();
			EngineFactory.initialize();
		} catch (InterruptedException | ConfigurationException ex) {
			throw new AssertionError(ex);
		}
	}

	@Test
	public void testGetTranscodingSettings() {
		TranscodingSettings test = TranscodingSettings.getTranscodingSettings((String)null);
		assertNull(test);
		test = TranscodingSettings.getTranscodingSettings("");
		assertNull(test);
		test = TranscodingSettings.getTranscodingSettings("BadEngine|MP4-H264-AAC");
		assertNull(test, "Bad engine id should return null");
		test = TranscodingSettings.getTranscodingSettings("FFmpegVideo|MP8-H264-AAC");
		assertNull(test, "Bad encoding format should return null");
		test = TranscodingSettings.getTranscodingSettings("FFmpegVideo|MP4-H264-AAC");
		assertNotNull(test);
		Assertions.assertEquals("FFmpegVideo|MP4-H264-AAC", test.getId(), "Good pair should return the good TranscodingSettings");
		test = TranscodingSettings.getTranscodingSettings("ffmpegvideo|MP4-H264-AAC");
		assertNotNull(test);
		Assertions.assertEquals("FFmpegVideo|MP4-H264-AAC", test.getId(), "Good pair should return the good TranscodingSettings");
	}

}
