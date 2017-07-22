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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.network.HTTPResource;
import net.pms.newgui.GuiUtil;
import net.pms.util.*;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME (breaking change): VLCWebVideo doesn't customize any of this, so everything should be *private*
// TODO (when transcoding to MPEG-2): handle non-MPEG-2 compatible input framerates

/**
 * Use VLC as a backend transcoder. Note that 0.x and 1.x versions are
 * unsupported (and probably will crash). Only the latest version will be
 * supported
 *
 * @author Leon Blakey <lord.quackstar@gmail.com>
 */
public class VLCVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(VLCVideo.class);
	private static final String COL_SPEC = "left:pref, 3dlu, p, 3dlu, 0:grow";
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p";
	public static final String ID = "VLCTranscoder";
	protected JTextField scale;
	protected JCheckBox experimentalCodecs;
	protected JCheckBox audioSyncEnabled;
	protected JTextField sampleRate;
	protected JCheckBox sampleRateOverride;
	SystemUtils registry = PMS.get().getRegistry();

	protected boolean videoRemux;

	@Deprecated
	public VLCVideo(PmsConfiguration configuration) {
		this();
	}

	public VLCVideo() {
	}

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_PLAYER;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public boolean isGPUAccelerationReady() {
		return true;
	}

	@Override
	public boolean avisynth() {
		return false;
	}

	@Override
	public String[] args() {
		return new String[]{};
	}

	@Override
	public String name() {
		return "VLC";
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public String mimeType() {
		// I think?
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public String executable() {
		return configuration.getVlcPath();
	}

	/**
	 * Pick codecs for VLC based on formats the renderer supports;
	 *
	 * @param renderer The {@link RendererConfiguration}.
	 * @return The codec configuration
	 */
	protected CodecConfig genConfig(RendererConfiguration renderer) {
		CodecConfig codecConfig = new CodecConfig();
		boolean isXboxOneWebVideo = renderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_PLAYER;

		if (
			(
				renderer.isTranscodeToWMV() &&
				!renderer.isXbox360()
			) ||
			isXboxOneWebVideo
		) {
			codecConfig.videoCodec = "wmv2";
			codecConfig.audioCodec = "wma";
			codecConfig.container = "asf";
		} else {
			/**
			 * VLC does not support H.265 encoding as of VLC 2.1.5.
			 */
			if (renderer.isTranscodeToH264() || renderer.isTranscodeToH265()) {
				codecConfig.videoCodec = "h264";
				videoRemux = true;
			} else {
				codecConfig.videoCodec = "mp2v";
			}

			if (renderer.isTranscodeToAC3()) {
				codecConfig.audioCodec = "a52";
			} else {
				codecConfig.audioCodec = "mp4a";
			}

			if (renderer.isTranscodeToMPEGTS()) {
				codecConfig.container = "ts";
			} else {
				codecConfig.container = "ps";
			}
		}
		LOGGER.trace("Using " + codecConfig.videoCodec + ", " + codecConfig.audioCodec + ", " + codecConfig.container);

		/**
		// Audio sample rate handling
		if (sampleRateOverride.isSelected()) {
			codecConfig.sampleRate = Integer.valueOf(sampleRate.getText());
		}
		*/

		// This has caused garbled audio, so only enable when told to
		if (audioSyncEnabled.isSelected()) {
			codecConfig.extraTrans.put("audio-sync", "");
		}
		return codecConfig;
	}

	protected static class CodecConfig {
		String videoCodec;
		String audioCodec;
		String container;
		String extraParams;
		HashMap<String, Object> extraTrans = new HashMap<>();
		int sampleRate;
	}

	protected Map<String, Object> getEncodingArgs(CodecConfig codecConfig, OutputParams params) {
		// See: http://www.videolan.org/doc/streaming-howto/en/ch03.html
		// See: http://wiki.videolan.org/Codec
		Map<String, Object> args = new HashMap<>();

		// Codecs to use
		args.put("vcodec", codecConfig.videoCodec);
		args.put("acodec", codecConfig.audioCodec);

		/**
		 * Bitrate in kbit/s
		 *
		 * TODO: Make this engine smarter with bitrates, see
		 * FFMpegVideo.getVideoBitrateOptions() for our best
		 * implementation of this.
		 */
		if (!videoRemux) {
			args.put("vb", "4096");
		}

		if (codecConfig.audioCodec.equals("mp4a")) {
			args.put("ab", Math.min(configuration.getAudioBitrate(), 320));
		} else {
			args.put("ab", configuration.getAudioBitrate());
		}

		// Video scaling
		args.put("scale", "1.0");

		boolean isXboxOneWebVideo = params.mediaRenderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_PLAYER;

		/**
		 * Only output 6 audio channels for codecs other than AC-3 because as of VLC
		 * 2.1.5, VLC screws up the channel mapping, making a rear channel go through
		 * a front speaker.
		 * Re-evaluate if they ever fix it.
		 */
		int channels = 2;
		if (
			!isXboxOneWebVideo &&
			params.aid.getAudioProperties().getNumberOfChannels() > 2 &&
			configuration.getAudioChannelCount() == 6 &&
			!params.mediaRenderer.isTranscodeToAC3()
		) {
			channels = 6;
		}
		args.put("channels", channels);

		// Static sample rate
		// TODO: Does WMA still need a sample rate of 41000 for Xbox compatibility?
		args.put("samplerate", "48000");

		// Recommended on VLC DVD encoding page
		args.put("strict-rc", null);

		// Enable multi-threading
		args.put("threads", "" + configuration.getNumberOfCpuCores());

		// Hardcode subtitles into video
		args.put("soverlay", null);

		// Add extra args
		args.putAll(codecConfig.extraTrans);

		return args;
	}

	private int[] getVideoBitrateConfig(String bitrate) {
		int bitrates[] = new int[2];

		if (bitrate.contains("(") && bitrate.contains(")")) {
			bitrates[1] = Integer.parseInt(bitrate.substring(bitrate.indexOf('(') + 1, bitrate.indexOf(')')));
		}

		if (bitrate.contains("(")) {
			bitrate = bitrate.substring(0, bitrate.indexOf('(')).trim();
		}

		if (isBlank(bitrate)) {
			bitrate = "0";
		}

		bitrates[0] = (int) Double.parseDouble(bitrate);

		return bitrates;
	}

	/**
	 * Returns the video bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer and the user's settings.
	 *
	 * @param dlna
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the video bitrate options for this transcode
	 */
	public List<String> getVideoBitrateOptions(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) {
		List<String> videoBitrateOptions = new ArrayList<>();

		int defaultMaxBitrates[] = getVideoBitrateConfig(configuration.getMaximumBitrate());
		int rendererMaxBitrates[] = new int[2];

		boolean isXboxOneWebVideo = params.mediaRenderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_PLAYER;

		if (StringUtils.isNotEmpty(params.mediaRenderer.getMaxVideoBitrate())) {
			rendererMaxBitrates = getVideoBitrateConfig(params.mediaRenderer.getMaxVideoBitrate());
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace(
				"Using video bitrate limit from {} configuration ({} Mb/s) because " +
				"it is lower than the general configuration bitrate limit ({} Mb/s)",
				params.mediaRenderer.getRendererName(),
				rendererMaxBitrates[0],
				defaultMaxBitrates[0]
			);
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (params.mediaRenderer.getCBRVideoBitrate() == 0 && params.timeend == 0) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			if (params.mediaRenderer.isHalveBitrate()) {
				defaultMaxBitrates[0] /= 2;
			}

			int bufSize = 1835;
			boolean bitrateLevel41Limited = false;

			/**
			 * Although the maximum bitrate for H.264 Level 4.1 is
			 * officially 50,000 kbit/s, some 4.1-capable renderers
			 * like the PS3 stutter when video exceeds roughly 31,250
			 * kbit/s.
			 *
			 * We also apply the correct buffer size in this section.
			 */
			if (!isXboxOneWebVideo && (params.mediaRenderer.isTranscodeToH264() || params.mediaRenderer.isTranscodeToH265())) {
				if (
					params.mediaRenderer.isH264Level41Limited() &&
					defaultMaxBitrates[0] > 31250
				) {
					defaultMaxBitrates[0] = 31250;
					bitrateLevel41Limited = true;
				}
				bufSize = defaultMaxBitrates[0];
			} else {
				if (media.isHDVideo()) {
					bufSize = defaultMaxBitrates[0] / 3;
				}

				if (bufSize > 7000) {
					bufSize = 7000;
				}

				if (defaultMaxBitrates[1] > 0) {
					bufSize = defaultMaxBitrates[1];
				}

				if (params.mediaRenderer.isDefaultVBVSize() && rendererMaxBitrates[1] == 0) {
					bufSize = 1835;
				}
			}

			if (!bitrateLevel41Limited) {
				// Make room for audio
				// TODO: set correct bitrate when remuxing DTS, like in FFMpegVideo
				if (params.mediaRenderer.isTranscodeToAAC()) {
					defaultMaxBitrates[0] -= Math.min(configuration.getAudioBitrate(), 320);
				} else {
					defaultMaxBitrates[0] -= configuration.getAudioBitrate();
				}

				// Round down to the nearest Mb
				defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;
			}

			videoBitrateOptions.add("--sout-x264-vbv-bufsize");
			videoBitrateOptions.add(String.valueOf(bufSize));

			videoBitrateOptions.add("--sout-x264-vbv-maxrate");
			videoBitrateOptions.add(String.valueOf(defaultMaxBitrates[0]));
		}

		if (isXboxOneWebVideo || (!params.mediaRenderer.isTranscodeToH264() && !params.mediaRenderer.isTranscodeToH265())) {
			// Add MPEG-2 quality settings
			String mpeg2Options = configuration.getMPEG2MainSettingsFFmpeg();
			String mpeg2OptionsRenderer = params.mediaRenderer.getCustomFFmpegMPEG2Options();

			// Renderer settings take priority over user settings
			if (isNotBlank(mpeg2OptionsRenderer)) {
				mpeg2Options = mpeg2OptionsRenderer;
			} else if (mpeg2Options.contains("Automatic")) {
				mpeg2Options = "--sout-x264-keyint 5 --sout-avcodec-qscale 1 --sout-avcodec-qmin 2 --sout-avcodec-qmax 3";

				// It has been reported that non-PS3 renderers prefer keyint 5 but prefer it for PS3 because it lowers the average bitrate
				if (params.mediaRenderer.isPS3()) {
					mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qscale 1 --sout-avcodec-qmin 2 --sout-avcodec-qmax 3";
				}

				if (mpeg2Options.contains("Wireless") || defaultMaxBitrates[0] < 70) {
					// Lower quality for 720p+ content
					if (media.getWidth() > 1280) {
						mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qmin 2 --sout-avcodec-qmax 7";
					} else if (media.getWidth() > 720) {
						mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qmin 2 --sout-avcodec-qmax 5";
					}
				}
			}
			String[] customOptions = StringUtils.split(mpeg2Options);
			videoBitrateOptions.addAll(new ArrayList<>(Arrays.asList(customOptions)));
		} else {
			// Add x264 quality settings
			String x264CRF = configuration.getx264ConstantRateFactor();

			// Remove comment from the value
			if (x264CRF.contains("/*")) {
				x264CRF = x264CRF.substring(x264CRF.indexOf("/*"));
			}

			if (x264CRF.contains("Automatic")) {
				x264CRF = "16";

				// Lower CRF for 720p+ content
				if (media.getWidth() > 720) {
					x264CRF = "19";
				}
			}
			videoBitrateOptions.add("--sout-x264-crf");
			videoBitrateOptions.add(x264CRF);
		}

		return videoBitrateOptions;
	}

	@Override
	public ProcessWrapper launchTranscode(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.mediaRenderer;
		final String filename = dlna.getFileName();
		boolean isWindows = Platform.isWindows();
		setAudioAndSubs(filename, media, params);

		// Make sure we can play this
		CodecConfig config = genConfig(params.mediaRenderer);

		PipeProcess tsPipe = new PipeProcess("VLC" + System.currentTimeMillis() + "." + config.container);
		ProcessWrapper pipe_process = tsPipe.getPipeProcess();

		// XXX it can take a long time for Windows to create a named pipe
		// (and mkfifo can be slow if /tmp isn't memory-mapped), so start this as early as possible
		pipe_process.runInNewThread();
		tsPipe.deleteLater();

		params.input_pipes[0] = tsPipe;
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;

		List<String> cmdList = new ArrayList<>();
		cmdList.add(executable());
		cmdList.add("-I");
		cmdList.add("dummy");

		/**
		 * Disable hardware acceleration which is enabled by default,
		 * but for hardware acceleration, user must enable it in "VLC Preferences",
		 * until they release documentation for new functionalities introduced in 2.1.4+
		 */
		if (registry.getVlcVersion() != null) {
			String vlcVersion = registry.getVlcVersion();
			Version currentVersion = new Version(vlcVersion);
			Version requiredVersion = new Version("2.1.4");

			if (currentVersion.compareTo(requiredVersion) > 0) {
				if (!configuration.isGPUAcceleration()) {
					cmdList.add("--avcodec-hw=disabled");
					LOGGER.trace("Disabled VLC's hardware acceleration.");
				}
			} else if (!configuration.isGPUAcceleration()) {
				LOGGER.trace("Version " + vlcVersion + " of VLC is too low to handle the way we disable hardware acceleration.");
			}
		}

		// Useful for the more esoteric codecs people use
		if (experimentalCodecs.isSelected()) {
			cmdList.add("--sout-avcodec-strict=-2");
		}

		// Stop the DOS box from appearing on windows
		if (isWindows) {
			cmdList.add("--dummy-quiet");
		}

		// File needs to be given before sout, otherwise vlc complains
		cmdList.add(filename);

		String disableSuffix = "track=-1";

		// Handle audio language
		if (params.aid != null) {
			// User specified language at the client, acknowledge it
			if (params.aid.getLang() == null || params.aid.getLang().equals("und")) {
				// VLC doesn't understand "und", so try to get audio track by ID
				cmdList.add("--audio-track=" + params.aid.getId());
			} else {
				cmdList.add("--audio-language=" + params.aid.getLang());
			}
		} else {
			// Not specified, use language from GUI
			// FIXME: VLC does not understand "loc" or "und".
			cmdList.add("--audio-language=" + configuration.getAudioLanguages());
		}

		// Handle subtitle language
		if (params.sid != null) { // User specified language at the client, acknowledge it
			if (params.sid.isExternal() && !params.sid.isStreamable() && !params.mediaRenderer.streamSubsForTranscodedVideo()) {
				String externalSubtitlesFileName;

				// External subtitle file
				if (params.sid.isExternalFileUtf16()) {
					try {
						// Convert UTF-16 -> UTF-8
						File convertedSubtitles = new File(configuration.getTempFolder(), "utf8_" + params.sid.getExternalFile().getName());
						FileUtil.convertFileFromUtf16ToUtf8(params.sid.getExternalFile(), convertedSubtitles);
						externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(convertedSubtitles.getAbsolutePath());
					} catch (IOException e) {
						LOGGER.debug("Error converting file from UTF-16 to UTF-8", e);
						externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.sid.getExternalFile().getAbsolutePath());
					}
				} else {
					externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.sid.getExternalFile().getAbsolutePath());
				}

				if (externalSubtitlesFileName != null) {
					cmdList.add("--sub-file");
					cmdList.add(externalSubtitlesFileName);
				}
			} else if (params.sid.getLang() != null && !params.sid.getLang().equals("und")) { // Load by ID (better)
				cmdList.add("--sub-track=" + params.sid.getId());
			} else { // VLC doesn't understand "und", but does understand a nonexistent track
				cmdList.add("--sub-" + disableSuffix);
			}
		} else {
			cmdList.add("--sub-" + disableSuffix);
		}

		// x264 options
		if (videoRemux) {
			cmdList.add("--sout-x264-preset");
			cmdList.add("superfast");

			/**
			 * This option prevents VLC from speeding up transcoding by disabling certain
			 * codec optimizations if the CPU is struggling to keep up.
			 * It is already disabled by default so there is no reason to specify that here,
			 * plus the option doesn't work on versions of VLC from 2.0.7 to 2.1.5.
			 */
			//cmdList.add("--no-sout-avcodec-hurry-up");

			cmdList.addAll(getVideoBitrateOptions(dlna, media, params));
		}

		// Skip forward if necessary
		if (params.timeseek != 0) {
			cmdList.add("--start-time");
			cmdList.add(String.valueOf(params.timeseek));
		}

		// Generate encoding args
		String separator = "";
		StringBuilder encodingArgsBuilder = new StringBuilder();
		for (Map.Entry<String, Object> curEntry : getEncodingArgs(config, params).entrySet()) {
			encodingArgsBuilder.append(separator);
			encodingArgsBuilder.append(curEntry.getKey());

			if (curEntry.getValue() != null) {
				encodingArgsBuilder.append('=');
				encodingArgsBuilder.append(curEntry.getValue());
			}

			separator = ",";
		}

		// Add our transcode options
		String transcodeSpec = String.format(
			"#transcode{%s}:standard{access=file,mux=%s,dst='%s%s'}",
			encodingArgsBuilder.toString(),
			config.container,
			(isWindows ? "\\\\" : ""),
			tsPipe.getInputPipe()
		);
		cmdList.add("--sout");
		cmdList.add(transcodeSpec);

		// Force VLC to exit when finished
		cmdList.add("vlc://quit");

		// Pass to process wrapper
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(pipe_process);

		// TODO: Why is this here?
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
		}

		pw.runInNewThread();
		configuration = prev;
		return pw;
	}

	@Override
	public JComponent config() {
		// Apply the orientation for the locale
		ComponentOrientation orientation = ComponentOrientation.getOrientation(PMS.getLocale());
		String colSpec = FormLayoutUtil.getColSpec(COL_SPEC, orientation);
		FormLayout layout = new FormLayout(colSpec, ROW_SPEC);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), FormLayoutUtil.flip(cc.xyw(1, 1, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		experimentalCodecs = new JCheckBox(Messages.getString("VlcTrans.3"), configuration.isVlcExperimentalCodecs());
		experimentalCodecs.setContentAreaFilled(false);
		experimentalCodecs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setVlcExperimentalCodecs(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(experimentalCodecs), FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		audioSyncEnabled = new JCheckBox(Messages.getString("MEncoderVideo.2"), configuration.isVlcAudioSyncEnabled());
		audioSyncEnabled.setContentAreaFilled(false);
		audioSyncEnabled.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setVlcAudioSyncEnabled(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(audioSyncEnabled), FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));

		JPanel panel = builder.getPanel();

		// Apply the orientation to the panel and all components in it
		panel.applyComponentOrientation(orientation);

		return panel;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		// Only handle local video - not web video or audio
		if (
			PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(resource, Format.Identifier.OGG)
		) {
			return true;
		}

		return false;
	}
}
