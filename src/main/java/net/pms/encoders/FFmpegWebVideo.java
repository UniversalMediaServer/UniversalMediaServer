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
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	public static PatternMap<Object> excludes = new PatternMap<Object>();
	public static PatternMap<ArrayList> autoOptions = new PatternMap<ArrayList>() {
		@Override
		public ArrayList add(String key, Object value) {
			return put(key, (ArrayList)parseOptions((String)value));
		}
	};
	private static boolean init = readWebFilters("ffmpeg.webfilters");

	// see http://ffmpeg.org/ffmpeg-protocols.html
	
	private static final List<String> protocols = Arrays.asList(
		"bluray",
		"concat",
		"data",
		"file",
		"gopher",
		"http",
		"https", // ?
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
	
	private static final List<String> http_options = Arrays.asList(
		"-seekable",
		"-chunked_post",
		"-headers",
		"-content_type",
		"-user-agent",
		"-multiple_requests",
		"-post_data",
		"-timeout",
		"-mime_type",
		"-cookies"
	);
	
	private static final List<String> rtmp_options = Arrays.asList(
		"-rtmp_app",
		"-rtmp_playpath"
	);

	private static final List<String> srtp_options = Arrays.asList(
		"-srtp_out_suite",
		"-srtp_out_params",
		"-srtp_in_suite",
		"-srtp_in_params" 
	);

	private static final List<String> tcp_options = Arrays.asList(
		"-listen",
		"-timeout",
		"-listen_timeout"
	);

	private static final List<String> udp_options = Arrays.asList(
		"-buffer_size",
		"-localport",
		"-localaddr",
		"-pkt_size",
		"-reuse",
		"-ttl",
		"-connect",
		"-fifo_size",
		"-overrun_nonfatal",
		"-timeout"
	);

	private static final List<String> crypto_options = Arrays.asList(
		"-key",
		"-iv"
	);

	private static final List<String> file_options = Arrays.asList(
		"-truncate"
	);

	private static final List<String> bluray_options = Arrays.asList(
		"-playlist",
		"-angle",
		"-chapter"
	);

	static List<String> ffmpegOptions(String group) {
		if (group.startsWith("http")) {
			return http_options;
		} else if (group.startsWith("rtmp")) {
			return rtmp_options;
		} else if (group.equals("tcp")) {
			return tcp_options;
		} else if (group.equals("srtp")) {
			return srtp_options;
		} else if (group.equals("udp")) {
			return udp_options;
		} else if (group.equals("bluray")) {
			return bluray_options;
		} else if (group.equals("file")) {
			return file_options;
		}
		return null;
	}
		
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

		// XXX work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (fileName.startsWith("mms:")) {
			fileName = "mmsh:" + fileName.substring(4);
		}

		String protocol = fileName.split(":")[0];
		
		optionsHashMap custom_options = new optionsHashMap();
		
		// Gather custom options (ascending priority):
		// - automatic options
		String match = autoOptions.match(fileName);
		if (match != null) {
			List<String> opts = autoOptions.get(match);
			if (opts != null) {
				custom_options.addAll(opts);
			}
		}
		// - (http) header options
		if (params.header != null && params.header.length > 0) {
			String hdr = new String(params.header);
			custom_options.addAll(parseOptions(hdr));
		}
		// - attached options
		String attached = (String)dlna.getAttachment("ffmpeg");
		if (attached != null) {
			custom_options.addAll(parseOptions(attached));
		}
		// - renderer options
		if (StringUtils.isNotEmpty(renderer.getCustomFFmpegOptions())) {
			custom_options.addAll(parseOptions(renderer.getCustomFFmpegOptions()));
		}

		// check for CRLF in header fields
		if (custom_options.containsKey("-headers")) {
			String headers = custom_options.get("-headers");
			// if it already has CRLF we assume it's ok
			if (! headers.contains("\r\n")) {
				// otherwise we try to fix it
				headers = headers.replace("User-Agent: ", "\r\nUser-Agent: ")
					.replace("Cookie: ", "\r\nCookie: ")
					.replace("Referer: ", "\r\nReferer: ")
					.replace("Accept: ", "\r\nAccept: ")
					.replace("Range: ", "\r\nRange: ")
					.replace("Connection: ", "\r\nConnection: ")
					.replace("Content-Length: ", "\r\nContent-Length: ")
					.replace("Content-Type: ", "\r\nContent-Type: ")
					.replace("\r\n\r\n", "\r\n")
					.trim() + "\r\n";
				custom_options.put("-headers", headers);
			}
		}
		
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

		// Add custom protocol options, if any
		List<String> protocolOptions = ffmpegOptions(protocol);
		if (protocolOptions != null) {
			custom_options.transferAny(protocolOptions, cmdList);
		}

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
		
		// Add any remaining custom options
		custom_options.transferAll(cmdList);

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
					&& excludes.match(url) == null;
			}
		}

		return false;
	}

	public static boolean readWebFilters(String filename) {
		PatternMap filter = null;
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
					} else if (line.equals("OPTIONS")) {
						filter = autoOptions;
					} else if (filter != null) {
						String[] var = line.split(" \\| ", 2);
						filter.add(var[0], var.length > 1 ? var[1] : null);
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
}


// a self-combining map of regexes that recompiles if modified

class PatternMap<T> extends modAwareHashMap<String,T> {
	Pattern combo;
	List<String> groupmap = new ArrayList<String>();
	
	public T add(String key, Object value) {
		return put(key, (T)value);
	}
	
	// returns the first matching regex
	String match(String str) {
		if (! isEmpty()) {
			if (modified) {
				compile();
			}
			Matcher matcher = combo.matcher(str);
			if (matcher.find()) {
				for (int i=0; i < matcher.groupCount(); i++) {
					if (matcher.group(i+1) != null) {
						return groupmap.get(i);
					}
				}
			}
		}
		return null;
	}

	void compile() {
		String joined = "";
		groupmap.clear();
		for (String regex : this.keySet()) {
			// add each regex as a capture group
			joined += "|(" + regex + ")";
			// map all subgroups to the parent
			for (int i=0; i<Pattern.compile(regex).matcher("").groupCount()+1; i++) {
				groupmap.add(regex);
			}
		}
		// compile the combined regex
		combo = Pattern.compile(joined.substring(1));
		modified = false;
	}	
}


// A HashMap that reports whether it's been modified
// (necessary because 'modCount' isn't accessible outside java.util)

class modAwareHashMap<K,V> extends HashMap<K,V> {
	public boolean modified = false;
	@Override
	public void clear() {
		modified = true;
		super.clear();
	}
	@Override
	public V put(K key, V value) {
		modified = true;
		return super.put(key, value);
	}
	@Override
	public V remove(Object key) {
		modified = true;
		return super.remove(key);
	}
}


// A HashMap of options and args (if any)
// which preserves insertion order

class optionsHashMap extends LinkedHashMap<String,String> {

	public void addAll(List<String> args) {
		String opt = null, optarg = null;
		args.add("-NULL"); 
		for (String arg : args) {
			if (arg.startsWith("-")) {
				// flush
				if (opt != null) {
					put(opt, optarg);
				}
				opt = arg;
				optarg = null;
			} else {
				optarg = arg;
			}
		}
		args.remove("-NULL"); 
	}
	
	public void transfer(String opt, List<String> list) {
		if (containsKey(opt)) {
			_transfer(opt, list);
		}
	}
	
	public void transferAny(List<String> opts, List<String> list) {
		for (String opt : opts) {
			transfer(opt, list);
		}
	}
	
	public void transferAll(List<String> list) {
		for (String opt : keySet()) {
			_transfer(opt, list);
		}
	}

	private void _transfer(String opt, List<String> list) {
		list.add(opt);
		String optarg = remove(opt);
		if (optarg != null) {
			list.add(optarg);
		}
	}
}


