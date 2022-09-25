/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.newgui.engines;

import javax.swing.JComponent;

public class Players {

	/**
	 * This class is not meant to be instantiated.
	 */
	private Players() {
	}

	public static JComponent config(String name) {
		return switch (name) {
			case net.pms.encoders.AviSynthFFmpeg.NAME -> AviSynthFFmpeg.config();
			case net.pms.encoders.AviSynthMEncoder.NAME -> AviSynthMEncoder.config();
			case net.pms.encoders.FFMpegVideo.NAME -> FFMpegVideo.config();
			case net.pms.encoders.FFmpegAudio.NAME -> FFmpegAudio.config();
			case net.pms.encoders.MEncoderVideo.NAME -> MEncoderVideo.config();
			case net.pms.encoders.TsMuxeRVideo.NAME -> TsMuxeRVideo.config();
			case net.pms.encoders.VLCVideo.NAME -> VLCVideo.config();
			default -> null;
		};

	}
}
