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
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;

public class MEncoderWebVideo extends Player {
	public static final String ID = "mencoderwebvideo";

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public String id() {
		return ID;
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

	@Deprecated
	public MEncoderWebVideo(PmsConfiguration configuration) {
		this();
	}

	public MEncoderWebVideo() {
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
		cmdArray[0] = executable();
		final String filename = dlna.getSystemName();
		cmdArray[1] = filename;
		System.arraycopy(args(), 0, cmdArray, 2, args().length);
		cmdArray[cmdArray.length - 2] = "-o";
		cmdArray[cmdArray.length - 1] = pipe.getInputPipe();

		ProcessWrapper mkfifo_process = pipe.getPipeProcess();

		cmdArray = finalizeTranscoderArgs(
			filename,
			dlna,
			media,
			params,
			cmdArray
		);

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
		return "MEncoder Web";
	}

	@Override
	public String[] args() {
		return getDefaultArgs();
	}

	@Override
	public String executable() {
		return configuration.getMencoderPath();
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isWebVideo(resource);
	}
}
