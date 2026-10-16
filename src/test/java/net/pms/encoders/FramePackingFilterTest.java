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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.pms.media.video.MediaVideo.Mode3D;
import org.junit.jupiter.api.Test;

class FramePackingFilterTest {

	@Test
	void sideBySideKeepsTheEyeOrderAndIgnoresHalfOrFull() {
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBSL));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBS2L));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBSR));
		assertEquals("frame_packing_type=side_by_side:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.SBS2R));
	}

	@Test
	void topAndBottomKeepsTheEyeOrderAndIgnoresHalfOrFull() {
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.ABL));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=left_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.AB2L));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.ABR));
		assertEquals("frame_packing_type=top_bottom:frame_packing_view=right_first", FFMpegVideo.getFramePackingFilterOptions(Mode3D.AB2R));
	}

	@Test
	void everyOtherLayoutHasNoFrameOfItsOwn() {
		assertNull(FFMpegVideo.getFramePackingFilterOptions(null));
		for (Mode3D mode : Mode3D.values()) {
			if (!mode.name().startsWith("SBS") && !mode.name().startsWith("AB")) {
				assertNull(FFMpegVideo.getFramePackingFilterOptions(mode), mode.name());
			}
		}
	}
}
