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

import com.sun.jna.Platform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.platform.PlatformUtils;
import net.pms.store.StoreItem;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeDl extends FFMpegVideo {

	private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeDl.class);

	public static final EngineId ID = StandardEngineId.YOUTUBE_DL;
	public static final String KEY_FFMPEG_WEB_EXECUTABLE_TYPE = "ffmpeg_web_executable_type";
	public static final String NAME = "youtube-dl";

	// Not to be instantiated by anything but PlayerFactory
	YoutubeDl() {
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_FFMPEG_WEB_EXECUTABLE_TYPE;
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_ENGINE;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		StoreItem resource,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);
		// Use device-specific conf
		UmsConfiguration configuration = params.getMediaRenderer().getUmsConfiguration();
		String filename = resource.getFileName();
		setAudioAndSubs(resource, params);

		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(configuration.getYoutubeDlPath());

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.getTimeSeek());
		}

		cmdList.add("--verbose");
		cmdList.add("--hls-use-mpegts");

		// This process wraps the command that creates the named pipe
		IPipeProcess pipe = null;

		boolean directPipe = Platform.isWindows();
		if (directPipe) {
			cmdList.add("-o");
			cmdList.add("-");
			params.setInputPipes(new IPipeProcess[2]);
		} else {
			// basename of the named pipe:
			String fifoName = String.format(
				"youtubedl_%d_%d",
				Thread.currentThread().getId(),
				System.currentTimeMillis()
			);
			pipe = PlatformUtils.INSTANCE.getPipeProcess(fifoName);
			params.getInputPipes()[0] = pipe;
			cmdList.add("-o");
			cmdList.add(pipe.getInputPipe());
		}

		// Output file
		cmdList.add(filename);

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Now launch youtube-dl
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		if (!directPipe) {
			ProcessWrapper mkfifoProcess = pipe.getPipeProcess();
			pw.attachProcess(mkfifoProcess); // Clean up the mkfifo process when the transcode ends

			/**
			 * It can take a long time for Windows to create a named pipe (and
			 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
			 * the current thread.
			 */
			mkfifoProcess.runInSameThread();
			pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		}

		// Give the mkfifo process a little time
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for named pipe to be created", e);
			Thread.currentThread().interrupt();
		}

		// Launch the transcode command...
		pw.runInNewThread();
		// ...and wait briefly to allow it to start
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e);
			Thread.currentThread().interrupt();
		}

		return pw;
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(StoreItem item) {
		return PlayerUtil.isWebVideo(item);
	}

}
