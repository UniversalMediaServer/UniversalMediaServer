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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.FormLayoutUtil;
import net.pms.util.PlayerUtil;
import net.pms.util.ProcessUtil;
import org.apache.commons.lang3.StringUtils;
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
	private static final String ROW_SPEC = "p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p";
	public static final String ID = "vlctranscoder";
	protected JTextField scale;
	protected JCheckBox experimentalCodecs;
	protected JCheckBox audioSyncEnabled;
	protected JTextField sampleRate;
	protected JCheckBox sampleRateOverride;
	protected JTextField extraParams;

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

		if (renderer.isTranscodeToWMV()) {
			// Assume WMV = Xbox = all media renderers with this flag
			LOGGER.debug("Using XBox WMV codecs");
			codecConfig.videoCodec = "wmv2";
			codecConfig.audioCodec = "wma";
			codecConfig.container = "asf";
		} else if (renderer.isTranscodeToH264TSAC3()) {
			LOGGER.debug("Using H.264 and MP2 with MPEG-TS container");
			codecConfig.videoCodec = "h264";

			/**
			 * XXX a52 (AC-3) causes the audio to cut out after
			 * a while (5, 10, and 45 minutes have been spotted)
			 * with versions as recent as 2.0.5. MP2 works without
			 * issue, so we use that as a workaround for now.
			 * codecConfig.audioCodec = "a52";
			 */
			codecConfig.audioCodec = "mp2a";

			codecConfig.container = "ts";

			videoRemux = true;
		} else {
			codecConfig.videoCodec = "mp2v";
			codecConfig.audioCodec = "mp2a";

			if (renderer.isTranscodeToMPEGTSAC3()) {
				LOGGER.debug("Using standard DLNA codecs with an MPEG-TS container");
				codecConfig.container = "ts";
			} else {
				LOGGER.debug("Using standard DLNA codecs with an MPEG-PS (default) container");
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
		args.put("ab", configuration.getAudioBitrate());

		// Video scaling
		args.put("scale", "1.0");

		// Audio Channels
		int channels = 2;

		/**
		 * Uncomment this block when we use a52 instead of mp2a
		if (params.aid.getAudioProperties().getNumberOfChannels() > 2 && configuration.getAudioChannelCount() == 6) {
			channels = 6;
		}
		 */
		args.put("channels", channels);

		// Static sample rate
		// TODO: Does WMA still need a sample rate of 41000 for Xbox compatibility?
		args.put("samplerate", "48000");

		// Recommended on VLC DVD encoding page
		//args.put("keyint", 16);

		// Recommended on VLC DVD encoding page
		//args.put("strict-rc", null);

		// Stream subtitles to client
		// args.add("scodec=dvbs");
		// args.add("senc=dvbsub");

		// Enable multi-threading
		args.put("threads", "" + configuration.getNumberOfCpuCores());

		// Hardcode subtitles into video
		args.put("soverlay", null);

		// Add extra args
		args.putAll(codecConfig.extraTrans);

		return args;
	}

	@Override
	public ProcessWrapper launchTranscode(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		final String filename = dlna.getSystemName();
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

		// Disable hardware acceleration which is enabled by default,
		// but for hardware acceleration, user must enable it in "VLC Preferences",
		// until they release documentation for new functionalities introduced in 2.1.4+
		if (!configuration.isGPUAcceleration()) {
			cmdList.add("--avcodec-hw=disabled");
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
			if (params.sid.isExternal()) {
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

			String x264CRF = configuration.getx264ConstantRateFactor();

			// Remove comment from the value
			if (x264CRF.contains("/*")) {
				x264CRF = x264CRF.substring(x264CRF.indexOf("/*"));
			}

			// Determine a good quality setting based on video attributes
			if (x264CRF.contains("Automatic")) {
				x264CRF = "16";

				// Lower CRF for 720p+ content
				if (media.getWidth() > 720) {
					x264CRF = "19";
				}
			}

			cmdList.add("--sout-x264-crf");
			cmdList.add(x264CRF);
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
				encodingArgsBuilder.append("=");
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

		// Add any extra parameters
		if (!extraParams.getText().trim().isEmpty()) { // Add each part as a new item
			cmdList.addAll(Arrays.asList(StringUtils.split(extraParams.getText().trim(), " ")));
		}

		// Pass to process wrapper
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);
		cmdArray = finalizeTranscoderArgs(filename, dlna, media, params, cmdArray);
		LOGGER.trace("Finalized args: " + StringUtils.join(cmdArray, " "));
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(pipe_process);

		// TODO: Why is this here?
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
		}

		pw.runInNewThread();
		return pw;
	}

	@Override
	public JComponent config() {
		// Apply the orientation for the locale
		Locale locale = new Locale(configuration.getLanguage());
		ComponentOrientation orientation = ComponentOrientation.getOrientation(locale);
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
		builder.add(experimentalCodecs, FormLayoutUtil.flip(cc.xy(1, 3), colSpec, orientation));

		audioSyncEnabled = new JCheckBox(Messages.getString("MEncoderVideo.2"), configuration.isVlcAudioSyncEnabled());
		audioSyncEnabled.setContentAreaFilled(false);
		audioSyncEnabled.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setVlcAudioSyncEnabled(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(audioSyncEnabled, FormLayoutUtil.flip(cc.xy(1, 5), colSpec, orientation));

		// Developer stuff. Theoretically temporary
		cmp = builder.addSeparator(Messages.getString("VlcTrans.10"), FormLayoutUtil.flip(cc.xyw(1, 7, 5), colSpec, orientation));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		// Add scale as a subpanel because it has an awkward layout
		/**
		mainPanel.append(Messages.getString("VlcTrans.11"));
		FormLayout scaleLayout = new FormLayout("pref,3dlu,pref", "");
		DefaultFormBuilder scalePanel = new DefaultFormBuilder(scaleLayout);
		double startingScale = Double.valueOf(configuration.getVlcScale());
		scalePanel.append(scale = new JTextField(String.valueOf(startingScale)));
		final JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 10, (int) (startingScale * 10));
		scalePanel.append(scaleSlider);
		scaleSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				String value = String.valueOf((double) scaleSlider.getValue() / 10);
				scale.setText(value);
				configuration.setVlcScale(value);
			}
		});
		scale.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String typed = scale.getText();
				if (!typed.matches("\\d\\.\\d")) {
					return;
				}
				double value = Double.parseDouble(typed);
				scaleSlider.setValue((int) (value * 10));
				configuration.setVlcScale(String.valueOf(value));
			}
		});
		mainPanel.append(scalePanel.getPanel(), 3);

		// Audio sample rate
		FormLayout sampleRateLayout = new FormLayout("right:pref, 3dlu, right:pref, 3dlu, right:pref, 3dlu, left:pref", "");
		DefaultFormBuilder sampleRatePanel = new DefaultFormBuilder(sampleRateLayout);
		sampleRateOverride = new JCheckBox(Messages.getString("VlcTrans.17"), configuration.getVlcSampleRateOverride());
		sampleRatePanel.append(Messages.getString("VlcTrans.18"), sampleRateOverride);
		sampleRate = new JTextField(configuration.getVlcSampleRate(), 8);
		sampleRate.setEnabled(configuration.getVlcSampleRateOverride());
		sampleRate.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setVlcSampleRate(sampleRate.getText());
			}
		});
		sampleRatePanel.append(Messages.getString("VlcTrans.19"), sampleRate);
		sampleRateOverride.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean checked = e.getStateChange() == ItemEvent.SELECTED;
				configuration.setVlcSampleRateOverride(checked);
				sampleRate.setEnabled(checked);
			}
		});

		mainPanel.nextLine();
		mainPanel.append(sampleRatePanel.getPanel(), 7);

		// Extra options
		mainPanel.nextLine();
		*/
		builder.addLabel(Messages.getString("VlcTrans.20"), FormLayoutUtil.flip(cc.xy(1, 9), colSpec, orientation));
		extraParams = new JTextField(configuration.getFont());
		extraParams.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setFont(extraParams.getText());
			}
		});
		builder.add(extraParams, FormLayoutUtil.flip(cc.xyw(3, 9, 3), colSpec, orientation));

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
			PlayerUtil.isVideo(resource, Format.Identifier.MPG)
		) {
			return true;
		}

		return false;
	}
}
