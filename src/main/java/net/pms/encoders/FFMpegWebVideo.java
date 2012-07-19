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

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;

public class FFMpegWebVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegWebVideo.class);
	public static final String ID = "ffmpegwebvideo";
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

	public FFMpegWebVideo(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;

		// basename of the named pipe: ffmpeg -y -loglevel warning -threads nThreads -i URL -threads nThreads -target ntsc-dvd /path/to/fifoName
		String fifoName = String.format("ffmpegwebvideo_%d_%d", Thread.currentThread().getId(), System.currentTimeMillis());

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);

		params.input_pipes[0] = pipe;
		int nThreads = configuration.getNumberOfCpuCores();

		// work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (fileName.startsWith("mms:")) {
			fileName = "mmsh:" + fileName.substring(4);
		}

		// build the command line
		String[] cmdArray = new String[] {
			executable(),
			"-y",
			"-loglevel", "warning",
			"-threads", "" + nThreads,
			"-i", fileName,
			"-threads",  "" + nThreads,
			"-target", "ntsc-dvd",
			pipe.getInputPipe()
		};

		// hook to allow plugins to customize this command line
		cmdArray = finalizeTranscoderArgs(
			this,
			fileName,
			dlna,
			media,
			params,
			cmdArray
		);

		// now launch ffmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		ProcessWrapper mkfifo_process = pipe.getPipeProcess();
		pw.attachProcess(mkfifo_process);

		// create the named pipe and wait briefly to allow it to be created
		mkfifo_process.runInNewThread();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("thread interrupted while waiting for named pipe to be created", e);
		}

		pipe.deleteLater();

		// launch transcode command and wait briefly to allow it to start
		pw.runInNewThread();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("thread interrupted while waiting for transcode to start", e);
		}

		return pw;
	}

	@Override
	public String name() {
		return "FFmpeg Web Video";
	}

	@Override
	// TODO remove this when it's removed from Player
	public String[] args() {
		return null;
	}

	@Override
	public String executable() {
		return configuration.getFfmpegPath();
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
		if (resource == null || resource.getFormat().getType() != Format.VIDEO) {
			return false;
		}

		Format format = resource.getFormat();

		if (format != null) {
			Format.Identifier id = format.getIdentifier();

			if (id.equals(Format.Identifier.WEB)) {
				return true;
			}
		}

		return false;
	}
}
