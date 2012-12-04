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
import javax.swing.JComponent;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.FFMpegVideo;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFMpegWebVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegWebVideo.class);
	private final PmsConfiguration configuration;

	// FIXME we have an id() accessor for this; no need for the field to be public
	@Deprecated
	public static final String ID = "ffmpegwebvideo";

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

		// basename of the named pipe:
		// ffmpeg -loglevel warning -threads nThreads -i URL -threads nThreads -transcode-video-options /path/to/fifoName
		String fifoName = String.format(
			"ffmpegwebvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);

		params.input_pipes[0] = pipe;
		int nThreads = configuration.getNumberOfCpuCores();

		// XXX work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (fileName.startsWith("mms:")) {
			fileName = "mmsh:" + fileName.substring(4);
		}

		// build the command line
		List<String> cmdList = new ArrayList<String>();

		cmdList.add(executable());

		cmdList.add("-loglevel");
		cmdList.add("warning");

		// decoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		cmdList.add("-i");
		cmdList.add(fileName);

		// encoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		// preserve the bitrate
		cmdList.add("-sameq");

		// get the TranscodeVideo (output) options (-acodec, -vcodec, -f)
		List<String> transcodeOptions = getTranscodeVideoOptions(params.mediaRenderer);
		cmdList.addAll(transcodeOptions);

		// output file
		cmdList.add(pipe.getInputPipe());

		// convert the command list to an array
		String[] cmdArray = new String[ cmdList.size() ];
		cmdList.toArray(cmdArray);

		// hook to allow plugins to customize this command line
		cmdArray = finalizeTranscoderArgs(
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
			LOGGER.error("Thread interrupted while waiting for named pipe to be created", e);
		}

		pipe.deleteLater();

		// launch transcode command and wait briefly to allow it to start
		pw.runInNewThread();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e);
		}

		return pw;
	}

	@Override
	public String name() {
		return "FFmpeg Web Video";
	}

	// TODO remove this when it's removed from Player
	@Deprecated
	@Override
	public String[] args() {
		return null;
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
