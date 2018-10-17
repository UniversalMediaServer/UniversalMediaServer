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
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import net.pms.Messages;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.network.HTTPResource;
import net.pms.newgui.GuiUtil;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegAudio extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudio.class);
	public static final String ID = "FFmpegAudio";

	// should be private
	@Deprecated
	JCheckBox noresample;

	@Deprecated
	public FFmpegAudio(PmsConfiguration configuration) {
		this();
	}

	public FFmpegAudio() {
	}

	@Override
	public JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		noresample = new JCheckBox(Messages.getString("TrTab2.22"), configuration.isAudioResample());
		noresample.setContentAreaFilled(false);
		noresample.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioResample(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(GuiUtil.getPreferredSizeComponent(noresample), cc.xy(2, 3));

		return builder.getPanel();
	}

	@Override
	public int purpose() {
		return AUDIO_SIMPLEFILE_PLAYER;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public boolean avisynth() {
		return false;
	}

	@Override
	public String name() {
		return "FFmpeg Audio";
	}

	@Override
	public int type() {
		return Format.AUDIO;
	}

	@Override
	@Deprecated
	public String[] args() {
		// unused: kept for backwards compatibility
		return new String[] {"-f", "s16be", "-ar", "48000"};
	}

	@Override
	public String mimeType() {
		return HTTPResource.AUDIO_TRANSCODE;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		PmsConfiguration prev = configuration;
		// Use device-specific pms conf
		configuration = (DeviceConfiguration)params.mediaRenderer;
		final String filename = dlna.getFileName();
		params.maxBufferSize = configuration.getMaxAudioBuffer();
		params.waitbeforestart = 2000;
		params.manageFastStart();

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

		List<String> cmdList = new ArrayList<>();

		cmdList.add(executable());

		cmdList.add("-loglevel");

		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + params.timeseek);
		}

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		cmdList.add("-i");
		cmdList.add(filename);

		// Make sure FFmpeg doesn't try to encode embedded images into the stream
		cmdList.add("-vn");

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		if (params.timeend > 0) {
			cmdList.add("-t");
			cmdList.add("" + params.timeend);
		}

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
				cmdList.add("-ac");
				cmdList.add("2");
			} else {
				cmdList.add("-ar");
				cmdList.add("48000");
				cmdList.add("-ac");
				cmdList.add("2");
			}
		}

		cmdList.add("pipe:");

		String[] cmdArray = new String[ cmdList.size() ];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		configuration = prev;
		return pw;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		// XXX Matching on file format isn't really enough, codec should also be evaluated
		if (
			PlayerUtil.isAudio(resource, Format.Identifier.AC3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ADPCM) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ADTS) ||
			PlayerUtil.isAudio(resource, Format.Identifier.AIFF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.APE) ||
			PlayerUtil.isAudio(resource, Format.Identifier.ATRAC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.AU) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DFF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DSF) ||
			PlayerUtil.isAudio(resource, Format.Identifier.DTS) ||
			PlayerUtil.isAudio(resource, Format.Identifier.EAC3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.FLAC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.M4A) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MKA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MLP) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MP3) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MPA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.MPC) ||
			PlayerUtil.isAudio(resource, Format.Identifier.OGA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.RA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.SHN) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THREEGA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THREEG2A) ||
			PlayerUtil.isAudio(resource, Format.Identifier.THD) ||
			PlayerUtil.isAudio(resource, Format.Identifier.TTA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WAV) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WMA) ||
			PlayerUtil.isAudio(resource, Format.Identifier.WV) ||
			PlayerUtil.isWebAudio(resource)
		) {
			return true;
		}

		return false;
	}
}
