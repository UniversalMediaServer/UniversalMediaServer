/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008-2012 A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.encoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeDl extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeDl.class);
	public static final PlayerId ID = StandardPlayerId.YOUTUBE_DL;

	public static final String KEY_FFMPEG_WEB_EXECUTABLE_TYPE = "ffmpeg_web_executable_type";
	public static final String NAME = "youtube-dl";

	// Not to be instantiated by anything but PlayerFactory
	YoutubeDl() {
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public PlayerId id() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_FFMPEG_WEB_EXECUTABLE_TYPE;
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_PLAYER;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);
		// Use device-specific conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.getMediaRenderer();
		String filename = dlna.getFileName();
		setAudioAndSubs(dlna, params);

		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(configuration.getYoutubeDlPath());

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.getTimeSeek());
		}

		cmdList.add("-f");
		cmdList.add("best");

		// Set up the process

		// basename of the named pipe:
		String fifoName = String.format(
			"youtubedl_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		ProcessWrapper mkfifoProcess = pipe.getPipeProcess();

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifoProcess.runInSameThread();

		params.getInputPipes()[0] = pipe;

		// Output file
		cmdList.add("-o");
		cmdList.add(pipe.getInputPipe());
		cmdList.add(filename);

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Now launch FFmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifoProcess); // Clean up the mkfifo process when the transcode ends

		// Give the mkfifo process a little time
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for named pipe to be created", e);
		}

		// Launch the transcode command...
		pw.runInNewThread();
		// ...and wait briefly to allow it to start
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e);
		}

		configuration = prev;
		return pw;
	}

	@Override
	public String name() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isWebVideo(resource);
	}

	static final Matcher END_OF_HEADER = Pattern.compile("Press \\[q\\]|A-V:|At least|Invalid").matcher("");
}
