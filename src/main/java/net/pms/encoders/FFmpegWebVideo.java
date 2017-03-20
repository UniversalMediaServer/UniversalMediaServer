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

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.external.ExternalFactory;
import net.pms.external.URLResolver.URLResult;
import net.pms.io.OutputParams;
import net.pms.io.OutputTextLogger;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegWebVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWebVideo.class);
	private static List<String> protocols;
	public static final PatternMap<Object> excludes = new PatternMap<>();

	public static final PatternMap<ArrayList> autoOptions = new PatternMap<ArrayList>() {
		private static final long serialVersionUID = 5225786297932747007L;

		@Override
		public ArrayList add(String key, Object value) {
			return put(key, (ArrayList) parseOptions((String) value));
		}
	};

	public static final PatternMap<String> replacements = new PatternMap<>();
	private static boolean init = false;

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

	@Deprecated
	public FFmpegWebVideo(PmsConfiguration configuration) {
		this();
	}

	public FFmpegWebVideo() {
		if (!init) {
			readWebFilters(configuration.getProfileDirectory() + File.separator + "ffmpeg.webfilters");
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
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.mediaRenderer;
		RendererConfiguration renderer = params.mediaRenderer;
		String filename = dlna.getSystemName();
		setAudioAndSubs(filename, media, params);

		// XXX work around an ffmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		if (filename.startsWith("mms:")) {
			filename = "mmsh:" + filename.substring(4);
		}

		// check if we have modifier for this url
		String r = replacements.match(filename);
		if (r != null) {
			filename = filename.replaceAll(r, replacements.get(r));
			LOGGER.debug("modified url: " + filename);
		}

		FFmpegOptions customOptions = new FFmpegOptions();

		// Gather custom options from various sources in ascending priority:
		// - automatic options
		String match = autoOptions.match(filename);
		if (match != null) {
			List<String> opts = autoOptions.get(match);
			if (opts != null) {
				customOptions.addAll(opts);
			}
		}
		// - (http) header options
		if (params.header != null && params.header.length > 0) {
			String hdr = new String(params.header);
			customOptions.addAll(parseOptions(hdr));
		}
		// - attached options
		String attached = (String) dlna.getAttachment(ID);
		if (attached != null) {
			customOptions.addAll(parseOptions(attached));
		}
		// - renderer options
		String ffmpegOptions = renderer.getCustomFFmpegOptions();
		if (StringUtils.isNotEmpty(ffmpegOptions)) {
			customOptions.addAll(parseOptions(ffmpegOptions));
		}

		// Build the command line
		List<String> cmdList = new ArrayList<>();
		if (!dlna.isURLResolved()) {
			URLResult r1 = ExternalFactory.resolveURL(filename);
			if (r1 != null) {
				if (r1.precoder != null) {
					filename = "-";
					if (Platform.isWindows()) {
						cmdList.add("cmd.exe");
						cmdList.add("/C");
					}
					cmdList.addAll(r1.precoder);
					cmdList.add("|");
				} else {
					if (StringUtils.isNotEmpty(r1.url)) {
						filename = r1.url;
					}
				}
				if (r1.args != null && r1.args.size() > 0) {
					customOptions.addAll(r1.args);
				}
			}
		}

		cmdList.add(executable());

		// XXX squashed bug - without this, ffmpeg hangs waiting for a confirmation
		// that it can write to a file that already exists i.e. the named pipe
		cmdList.add("-y");

		cmdList.add("-loglevel");

		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		/*
		 * FFmpeg uses multithreading by default, so provided that the
		 * user has not disabled FFmpeg multithreading and has not
		 * chosen to use more or less threads than are available, do not
		 * specify how many cores to use.
		 */
		int nThreads = 1;
		if (configuration.isFfmpegMultithreading()) {
			if (Runtime.getRuntime().availableProcessors() == configuration.getNumberOfCpuCores()) {
				nThreads = 0;
			} else {
				nThreads = configuration.getNumberOfCpuCores();
			}
		}

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		// Add global and input-file custom options, if any
		if (!customOptions.isEmpty()) {
			customOptions.transferGlobals(cmdList);
			customOptions.transferInputFileOptions(cmdList);
		}

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.timeseek);
		}

		cmdList.add("-i");
		cmdList.add(filename);

		cmdList.addAll(getVideoFilterOptions(dlna, media, params));

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		// Add the output options (-f, -c:a, -c:v, etc.)

		// Now that inputs and filtering are complete, see if we should
		// give the renderer the final say on the command
		boolean override = false;
		if (renderer instanceof RendererConfiguration.OutputOverride) {
			override = ((RendererConfiguration.OutputOverride) renderer).getOutputOptions(cmdList, dlna, this, params);
		}

		if (!override) {
			cmdList.addAll(getVideoTranscodeOptions(dlna, media, params));

			// Add video bitrate options
			cmdList.addAll(getVideoBitrateOptions(dlna, media, params));

			// Add audio bitrate options
			cmdList.addAll(getAudioBitrateOptions(dlna, media, params));

			// Add any remaining custom options
			if (!customOptions.isEmpty()) {
				customOptions.transferAll(cmdList);
			}
		}

		// Set up the process

		// basename of the named pipe:
		String fifoName = String.format(
			"ffmpegwebvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		ProcessWrapper mkfifo_process = pipe.getPipeProcess();

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifo_process.runInSameThread();

		params.input_pipes[0] = pipe;

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
		parseMediaInfo(filename, dlna, pw); // Better late than never
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

		configuration = prev;
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
		if (PlayerUtil.isWebVideo(resource)) {
			String url = resource.getSystemName();
			return protocols.contains(url.split(":")[0]) && excludes.match(url) == null;
		}

		return false;
	}

	public boolean readWebFilters(String filename) {
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
					} else if (line.equals("REPLACE")) {
						filter = replacements;
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

	static final Matcher endOfHeader = Pattern.compile("Press \\[q\\]|A-V:|At least|Invalid").matcher("");

	/**
	 * Parse media info from ffmpeg headers during playback
	 */
	public void parseMediaInfo(String filename, final DLNAResource dlna, final ProcessWrapperImpl pw) {
		if (dlna.getMedia() == null) {
			dlna.setMedia(new DLNAMediaInfo());
		} else if (dlna.getMedia().isFFmpegparsed()) {
			return;
		}
		final ArrayList<String> lines = new ArrayList<>();
		final String input = filename.length() > 200 ? filename.substring(0, 199) : filename;
		OutputTextLogger ffParser = new OutputTextLogger(null) {
			@Override
			public boolean filter(String line) {
				if (endOfHeader.reset(line).find()) {
					dlna.getMedia().parseFFmpegInfo(lines, input);
					LOGGER.trace("[{}] parsed media from headers: {}", ID, dlna.getMedia());
					dlna.getParent().updateChild(dlna);
					return false; // done, stop filtering
				}
				lines.add(line);
				return true; // keep filtering
			}
		};
		ffParser.setFiltered(true);
		pw.setStderrConsumer(ffParser);
	}
}

// A self-combining map of regexes that recompiles if modified
class PatternMap<T> extends modAwareHashMap<String, T> {
	private static final long serialVersionUID = 3096452459003158959L;
	Matcher combo;
	List<String> groupmap = new ArrayList<>();

	public T add(String key, Object value) {
		return put(key, (T) value);
	}

	// Returns the first matching regex
	String match(String str) {
		if (!isEmpty()) {
			if (modified) {
				compile();
			}
			if (combo.reset(str).find()) {
				for (int i = 0; i < combo.groupCount(); i++) {
					if (combo.group(i + 1) != null) {
						return groupmap.get(i);
					}
				}
			}
		}
		return null;
	}

	void compile() {
		StringBuilder joined = new StringBuilder();
		groupmap.clear();
		for (String regex : this.keySet()) {
			// add each regex as a capture group
			joined.append("|(").append(regex).append(')');
			// map all subgroups to the parent
			for (int i = 0; i < Pattern.compile(regex).matcher("").groupCount() + 1; i++) {
				groupmap.add(regex);
			}
		}
		// compile the combined regex
		combo = Pattern.compile(joined.substring(1)).matcher("");
		modified = false;
	}
}

// A HashMap that reports whether it's been modified
// (necessary because 'modCount' isn't accessible outside java.util)
class modAwareHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = -5334451082377480129L;
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
