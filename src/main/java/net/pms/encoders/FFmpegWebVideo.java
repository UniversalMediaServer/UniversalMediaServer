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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.ExecutableInfo;
import net.pms.configuration.FFmpegExecutableInfo;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
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
	public static final PlayerId ID = StandardPlayerId.FFMPEG_WEB_VIDEO;

	public static final String KEY_FFMPEG_WEB_EXECUTABLE_TYPE = "ffmpeg_web_executable_type";
	public static final String NAME = "FFmpeg Web Video";

	// Not to be instantiated by anything but PlayerFactory
	FFmpegWebVideo() {
	}

	/**
	 * Must be used to protect all access to {@link #excludes}, {@link #autoOptions} and {@link #replacements}
	 */
	protected static final ReentrantReadWriteLock FILTERS_LOCK = new ReentrantReadWriteLock();

	static {
		readWebFilters(CONFIGURATION.getProfileDirectory() + File.separator + "ffmpeg.webfilters");
	}

	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	protected static final PatternMap<Object> EXCLUDES = new PatternMap<>();

	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	protected static final PatternMap<ArrayList> AUTO_OPTIONS = new PatternMap<ArrayList>() {
		private static final long serialVersionUID = 5225786297932747007L;

		@Override
		public ArrayList add(String key, Object value) {
			return put(key, (ArrayList) parseOptions((String) value));
		}
	};

	/**
	 * All access must be protected with {@link #filtersLock}
	 */
	protected static final PatternMap<String> REPLACEMENTS = new PatternMap<>();

	protected static boolean readWebFilters(String filename) {
		String line;
		try {
			LineIterator it = FileUtils.lineIterator(new File(filename));
			FILTERS_LOCK.writeLock().lock();
			try {
				PatternMap<?> filter = null;
				while (it.hasNext()) {
					line = it.nextLine().trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					} else if (line.equals("EXCLUDE")) {
						filter = EXCLUDES;
					} else if (line.equals("OPTIONS")) {
						filter = AUTO_OPTIONS;
					} else if (line.equals("REPLACE")) {
						filter = REPLACEMENTS;
					} else if (filter != null) {
						String[] var = line.split(" \\| ", 2);
						filter.add(var[0], var.length > 1 ? var[1] : null);
					}
				}
				return true;
			} finally {
				FILTERS_LOCK.writeLock().unlock();
				it.close();
			}
		} catch (FileNotFoundException e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.info("FFmpeg web filters \"{}\" not found, web filters ignored: {}", filename, e.getMessage());
			} else {
				LOGGER.info("FFmpeg web filters \"{}\" not found, web filters ignored", filename);
			}
		} catch (IOException e) {
			LOGGER.debug("Error reading ffmpeg web filters from file \"{}\": {}", filename, e.getMessage());
			LOGGER.trace("", e);
		}
		return false;
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

		// Backup the existing configuration, to be restored at the end
		// TODO: stop doing that
		PmsConfiguration existingConfiguration = configuration;

		configuration = (DeviceConfiguration) params.getMediaRenderer();
		RendererConfiguration renderer = params.getMediaRenderer();
		String filename = dlna.getFileName();
		setAudioAndSubs(dlna, params);

		// Workaround an FFmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		// Also see static init
		if (filename.startsWith("mms:")) {
			filename = "mmsh:" + filename.substring(4);
		}

		// check if we have modifier for this url
		FILTERS_LOCK.readLock().lock();
		try {
			String r = REPLACEMENTS.match(filename);
			if (r != null) {
				filename = filename.replaceAll(r, REPLACEMENTS.get(r));
				LOGGER.debug("Modified url: {}", filename);
			}
		} finally {
			FILTERS_LOCK.readLock().unlock();
		}

		FFmpegOptions customOptions = new FFmpegOptions();

		// Gather custom options from various sources in ascending priority:
		// - automatic options
		FILTERS_LOCK.readLock().lock();
		try {
			String match = AUTO_OPTIONS.match(filename);
			if (match != null) {
				List<String> opts = AUTO_OPTIONS.get(match);
				if (opts != null) {
					customOptions.addAll(opts);
				}
			}
		} finally {
			FILTERS_LOCK.readLock().unlock();
		}
		// - (http) header options
		if (params.getHeader() != null && params.getHeader().length > 0) {
			String hdr = new String(params.getHeader());
			customOptions.addAll(parseOptions(hdr));
		}
		// - attached options
		String attached = (String) dlna.getAttachment(ID.toString());
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

		cmdList.add(getExecutable());

		// this stops FFmpeg waiting to write to a file that already exists i.e. the named pipe
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

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.getTimeSeek());
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
			// TODO: See if that last boolean can be set more carefully to disable unnecessary transcoding
			cmdList.addAll(getVideoTranscodeOptions(dlna, media, params, false));

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
		ProcessWrapper mkfifoProcess = pipe.getPipeProcess();

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifoProcess.runInSameThread();

		params.getInputPipes()[0] = pipe;

		// Output file
		cmdList.add(pipe.getInputPipe());

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Now launch FFmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		parseMediaInfo(filename, dlna, pw); // Better late than never
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

		configuration = existingConfiguration;
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
		if (PlayerUtil.isWebVideo(resource)) {
			String url = resource.getFileName();

			ExecutableInfo executableInfo = programInfo.getExecutableInfo(currentExecutableType);
			if (executableInfo instanceof FFmpegExecutableInfo) {
				List<String> protocols = FFmpegOptions.getSupportedProtocols(executableInfo.getPath());
				if (protocols == null || !protocols.contains(url.split(":")[0])) {
					return false;
				}
			} else {
				LOGGER.warn(
					"Couldn't check {} protocol compatibility for \"{}\", reporting as not compatible",
					getClass().getSimpleName(),
					url.split(":")[0]
				);
				return false;
			}

			// FFmpeg does not natively support YouTube videos
			if (isYouTubeURL(url)) {
				return false;
			}

			FILTERS_LOCK.readLock().lock();
			try {
				if (EXCLUDES.match(url) == null) {
					return true;
				}
			} finally {
				FILTERS_LOCK.readLock().unlock();
			}
		}

		return false;
	}

	static final Matcher END_OF_HEADER = Pattern.compile("Press \\[q\\]|A-V:|At least|Invalid").matcher("");

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
				if (END_OF_HEADER.reset(line).find()) {
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

	public static boolean isYouTubeURL(String youTubeUrl) {
		String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(youTubeUrl);
		if (matcher.find()) {
			return true;
		}
		return false;
	}
}

// A self-combining map of regexes that recompiles if modified
class PatternMap<T> extends ModAwareHashMap<String, T> {
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
class ModAwareHashMap<K, V> extends HashMap<K, V> {
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
