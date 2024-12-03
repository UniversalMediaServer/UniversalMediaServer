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

import java.io.IOException;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.store.StoreItem;
import net.pms.util.PlayerUtil;

public class TsMuxeRAudio extends TsMuxeRVideo {
	public static final EngineId ID = StandardEngineId.TSMUXER_AUDIO;

	/** The {@link Configuration} key for the tsMuxeR Audio executable type. */
	public static final String KEY_TSMUXER_AUDIO_EXECUTABLE_TYPE = "tsmuxer_audio_executable_type";
	public static final String NAME = "tsMuxeR Audio";

	// Not to be instantiated by anything but PlayerFactory
	TsMuxeRAudio() {
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_TSMUXER_AUDIO_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public ProcessWrapper launchTranscode(
		StoreItem resource,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		params.setTimeEnd(media.getDurationInSeconds());
		params.setWaitBeforeStart(2500);
		return super.launchTranscode(resource, media, params);
	}

	@Override
	public String getMimeType() {
		return HTTPResource.AUDIO_TRANSCODE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int purpose() {
		return AUDIO_SIMPLEFILE_ENGINE;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public boolean isCompatible(StoreItem resource) {
		return PlayerUtil.isVideo(resource, Format.Identifier.AUDIO_AS_VIDEO);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isAudioFormat();
	}

}
