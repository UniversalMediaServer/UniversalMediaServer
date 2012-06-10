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

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

public class MEncoderWebVideo extends Player {
	public static final String ID = "mencoderwebvideo";
	private final PmsConfiguration configuration;

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

	public MEncoderWebVideo(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;

		PipeProcess pipe = new PipeProcess("mencoder" + System.currentTimeMillis());
		params.input_pipes[0] = pipe;

		String cmdArray[] = new String[args().length + 4];
		cmdArray[0] = executable();
		cmdArray[1] = fileName;
		for (int i = 0; i < args().length; i++) {
			cmdArray[i + 2] = args()[i];
		}
		cmdArray[cmdArray.length - 2] = "-o";
		cmdArray[cmdArray.length - 1] = pipe.getInputPipe();

		ProcessWrapper mkfifo_process = pipe.getPipeProcess();

		cmdArray = finalizeTranscoderArgs(
			this,
			fileName,
			dlna,
			media,
			params,
			cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifo_process);
		mkfifo_process.runInNewThread();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		pipe.deleteLater();

		pw.runInNewThread();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
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
}
