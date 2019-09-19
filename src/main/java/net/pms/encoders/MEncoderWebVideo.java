/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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

public class MEncoderWebVideo extends MEncoderVideo {
	public static final PlayerId ID = StandardPlayerId.MENCODER_WEB_VIDEO;

	/** The {@link Configuration} key for the DCRaw executable type. */
	public static final String KEY_MENCODER_WEB_EXECUTABLE_TYPE = "mencoder_web_executable_type";
	public static final String NAME = "MEncoder Web Video";

	// Not to be instantiated by anything but PlayerFactory
	MEncoderWebVideo() {
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
		return KEY_MENCODER_WEB_EXECUTABLE_TYPE;
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
	public String mimeType() {
		return "video/mpeg";
	}

	@Override
	protected String[] getDefaultArgs() {
		int nThreads = configuration.getMencoderMaxThreads();
		String acodec = configuration.isMencoderAc3Fixed() ? "ac3_fixed" : "ac3";
		return new String[]{
				"-msglevel", "all=2",
				"-quiet",
				"-prefer-ipv4",
				"-cache", "16384",
				"-oac", "lavc",
				"-of", "lavf",
				"-lavfopts", "format=dvd",
				"-ovc", "lavc",
				"-lavcopts", "vcodec=mpeg2video:vbitrate=4096:threads=" + nThreads + ":acodec=" + acodec + ":abitrate=128",
				"-vf", "harddup",
				"-ofps", "25"
			};
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.mediaRenderer;
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;

		PipeProcess pipe = new PipeProcess("mencoder" + System.currentTimeMillis());
		params.input_pipes[0] = pipe;

		String cmdArray[] = new String[args().length + 4];
		cmdArray[0] = getExecutable();
		final String filename = dlna.getFileName();
		cmdArray[1] = filename;
		System.arraycopy(args(), 0, cmdArray, 2, args().length);
		cmdArray[cmdArray.length - 2] = "-o";
		cmdArray[cmdArray.length - 1] = pipe.getInputPipe();

		ProcessWrapper mkfifo_process = pipe.getPipeProcess();

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifo_process);

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifo_process.runInSameThread();

		pipe.deleteLater();

		pw.runInNewThread();

		// Not sure what good this 50ms wait will do for the calling method.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		configuration = prev;
		return pw;
	}

	@Override
	public boolean avisynth() {
		return false;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String[] args() {
		return getDefaultArgs();
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isWebVideo(resource);
	}
}
