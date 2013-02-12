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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import javax.swing.JComponent;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegWebVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWebVideo.class);
	private final PmsConfiguration configuration;

	public static PatternList excludes = new PatternList();
	private static boolean init = readWebFilters("ffmpeg.webfilters");

	// see http://ffmpeg.org/ffmpeg-protocols.html
	
	private static final List<String> protocols = Arrays.asList(
		"bluray",
		"concat",
		"data",
		"file",
		"gopher",
		"http", // TODO: support -cookies option
//		"https", // ?
		"mms",
		"mmsh",
		"mmst",
		"rtmp", // TODO: first verify whether ffmpeg is --enable-rtmp
		"rtmpe",
		"rtmps",
		"rtmpt",
		"rtmpte",
		"rtmpts",
		"rtp",
		"rtsp",
		"tcp",
		"tls",
		"udp"
	);
		
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

	public FFmpegWebVideo(PmsConfiguration configuration) {
		super(configuration);
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
		RendererConfiguration renderer = params.mediaRenderer;

		// basename of the named pipe:
		// ffmpeg -loglevel warning -threads nThreads -i URL -threads nThreads -transcode-video-options /path/to/fifoName
		String fifoName = String.format(
			"ffmpegwebvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		ProcessWrapper mkfifo_process = pipe.getPipeProcess();
		// start the process as early as possible
		mkfifo_process.runInNewThread();

		params.input_pipes[0] = pipe;
		int nThreads = configuration.getNumberOfCpuCores();

		// XXX work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (fileName.startsWith("mms:")) {
			fileName = "mmsh:" + fileName.substring(4);
		}

		// build the command line
		List<String> cmdList = new ArrayList<String>();

		cmdList.add(executable());

		// XXX squashed bug - without this, ffmpeg hangs waiting for a confirmation
		// that it can write to a file that already exists i.e. the named pipe
		cmdList.add("-y");

		cmdList.add("-loglevel");
		cmdList.add("warning");

		// decoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		cmdList.add("-i");
		cmdList.add(fileName);

		cmdList.addAll(getVideoFilterOptions(renderer, media, params));
		
		// Encoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		// Add the output options (-f, -acodec, -vcodec)
		cmdList.addAll(getTranscodeVideoOptions(renderer, media, params));

		// Add video bitrate options
		cmdList.addAll(getVideoBitrateOptions(renderer, media));

		// Add audio bitrate options
		cmdList.addAll(getAudioBitrateOptions(renderer, media));

		// Add (http) headers
		if (params.header != null && params.header.length > 0) {
			String hdr = new String(params.header);
			parseOptions(hdr, cmdList);
		}

		// Add custom options
		if (StringUtils.isNotEmpty(renderer.getCustomFFmpegOptions())) {
			parseOptions(renderer.getCustomFFmpegOptions(), cmdList);
		}

		// Output file
		cmdList.add(pipe.getInputPipe());

		// Convert the command list to an array
		String[] cmdArray = new String[ cmdList.size() ];
		cmdList.toArray(cmdArray);

		// Hook to allow plugins to customize this command line
		cmdArray = finalizeTranscoderArgs(
			fileName,
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

	public static boolean readWebFilters(String filename) {
		PatternList filter = null;
		String line;
		try {
			LineIterator it = FileUtils.lineIterator(new File(filename));
			try {
				while (it.hasNext()) {
					line = it.nextLine().trim();
					if (line.isEmpty() || line.startsWith("#")) {
						// continue
					} else if (line.equals("EXCLUDE")) {
						filter = excludes;
					} else if (filter != null) {
						filter.add(line);
					}
				}
				return true;
			} finally {
				it.close();
			} 
		} catch (Exception e) {
			LOGGER.debug("Error reading ffmpeg web filters: " + e.getLocalizedMessage());
		}
		return false;
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
				String url = resource.getSystemName();
				return protocols.contains(url.split(":")[0])
					&& ! excludes.match(url);
			}
		}

		return false;
	}
}

// a self-combining list of regexes that recompiles if modified

class PatternList extends ArrayList<String> {
	Pattern pattern;
	int modified = 0;

	boolean match(String str) {
		if (modified != modCount) {
			pattern = Pattern.compile(StringUtils.join(this, "|"));
			modified = modCount;
		}
		return ! isEmpty() && pattern.matcher(str).find();
	}
}


