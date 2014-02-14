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
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JComponent;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.IPushOutput;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegWebAudio extends FFmpegAudio {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWebAudio.class);
	private static List<String> protocols;

	private static boolean init = false;

	// FIXME we have an id() accessor for this; no need for the field to be public
	@Deprecated
	public static final String ID = "ffmpegwebaudio";

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
		return AUDIO_WEBSTREAM_PLAYER;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public String name() {
		return "FFmpeg Web Audio";
	}
	
	@Deprecated
	public FFmpegWebAudio(PmsConfiguration configuration) {
		this();
	}
	
	public FFmpegWebAudio() {
		if (!init) {
			protocols = FFmpegOptions.getSupportedProtocols(configuration);
			if (protocols.contains("mmsh")) {
				// see XXX workaround below
				protocols.add("mms");
			}
			LOGGER.debug("FFmpeg supported protocols: " + protocols);
			init = true;
		}
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.maxBufferSize = configuration.getMaxAudioBuffer();
		params.waitbeforestart = 5000;
		params.manageFastStart();

		String filename = dlna.getSystemName();

		// XXX work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (filename.startsWith("mms:")) {
			filename = "mmsh:" + filename.substring(4);
		}
		
		// basename of the named pipe:
		String fifoName = String.format(
			"ffmpegwebaudio_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		
		ProcessWrapper mkfifo_process = pipe.getPipeProcess();
		mkfifo_process.runInSameThread();

		params.input_pipes[0] = pipe;

		
		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(executable());
		cmdList.add("-y");
		
		cmdList.add("-loglevel");
		
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		//XXX nThread removed because audio transcoding doesn't produce high CPU load...
		
		
		// Simple (.pls|.m3u) parser
		// XXX ffmpeg doesn't support streaming from (*.pls|*.m3u) files (Shoutcast default)
		// select random entry (stream url) form playlist url
		if (filename.startsWith("http") && (filename.endsWith("pls") || filename.endsWith("m3u"))) {

			final int PLAYLIST_TYPE_PLS = 0;
			final int PLAYLIST_TYPE_M3U = 1;

			final int type = filename.endsWith("pls") ? PLAYLIST_TYPE_PLS : PLAYLIST_TYPE_M3U;

			// http get playlist file
			@SuppressWarnings("resource")
			String pls = new Scanner(new URL(filename).openStream(), "UTF-8").useDelimiter("\\A").next();

			final String[] lines = pls.split("[\\r\\n]+");
			
			if (lines.length > 0) {

				// push the newline-containing input arg via stdin
				params.stdin = new IPushOutput() {
					@Override
					public void push(OutputStream out) throws IOException {
						for (String line : lines) {
							switch (type) {
							case PLAYLIST_TYPE_PLS:// .pls
								if (line.contains("File")) {
									out.write(("file '" + line.substring(line.indexOf("=") + 1) + "'\n").getBytes());
								}
								break;

							case PLAYLIST_TYPE_M3U:// .m3u
								if (!line.startsWith("#")) {
									out.write(("file '" + line + "'\n").getBytes());
								}
								break;
							}
						}

						out.close();
					}

					@Override
					public boolean isUnderlyingSeekSupported() {
						return false;
					}
				};

				cmdList.add("-f");
				cmdList.add("concat");
				
				filename = "-";
			}
		}
		

		cmdList.add("-i");
		cmdList.add(filename);

		if (params.mediaRenderer.isTranscodeToMP3()) {
			cmdList.add("-f");
			cmdList.add("mp3");
			cmdList.add("-ab");
			cmdList.add("320000");
		} else if (params.mediaRenderer.isTranscodeToWAV()) {
			cmdList.add("-f");
			cmdList.add("wav");
		} else { // default: LPCM
			cmdList.add("-f");
			cmdList.add("s16be");
		}

		if (configuration.isAudioResample()) {
			if (params.mediaRenderer.isTranscodeAudioTo441()) {
				cmdList.add("-ar");
				cmdList.add("44100");
			} else {
				cmdList.add("-ar");
				cmdList.add("48000");
			}
		}

		// Output file
		cmdList.add(pipe.getInputPipe());

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Hook to allow plugins to customize this command line
		cmdArray = finalizeTranscoderArgs(
			filename,
			dlna,
			media,
			params,
			cmdArray
		);

		// Now launch FFmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		pw.attachProcess(mkfifo_process); // Clean up the mkfifo process when the transcode ends

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

		return pw;
	}


	// TODO remove this when it's removed from Player
	@Deprecated
	@Override
	public String[] args() {
		return null;
	}


	@Override
	public boolean isCompatible(DLNAResource resource) {

		if( PlayerUtil.isWebAudio(resource) ) {
			return protocols.contains(resource.getSystemName().split(":")[0]);
		}
		
		return false;
	}

}


