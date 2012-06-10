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

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.Messages;
import net.pms.network.HTTPResource;
import net.pms.PMS;

public class FFMpegAudio extends FFMpegVideo {
	public static final String ID = "ffmpegaudio";
	private final PmsConfiguration configuration;

	public FFMpegAudio(PmsConfiguration configuration) {
		this.configuration = configuration;
	}
	JCheckBox noresample;

	@Override
	public JComponent config() {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 0:grow");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();


		JComponent cmp = builder.addSeparator("Audio settings", cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		noresample = new JCheckBox(Messages.getString("TrTab2.22"));
		noresample.setContentAreaFilled(false);
		noresample.setSelected(configuration.isAudioResample());
		noresample.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioResample(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(noresample, cc.xy(2, 3));

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
	public String[] args() {
		return new String[]{"-f", "s16be", "-ar", "48000"};
	}

	@Override
	public String mimeType() {
		return HTTPResource.AUDIO_TRANSCODE;
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		params.maxBufferSize = PMS.getConfiguration().getMaxAudioBuffer();
		params.waitbeforestart = 2000;
		params.manageFastStart();
		String args[] = args();
		if (params.mediaRenderer.isTranscodeToMP3()) {
			args = new String[]{"-f", "mp3", "-ar", "48000", "-ab", "320000"};
		}
		if (params.mediaRenderer.isTranscodeToWAV()) {
			args = new String[]{"-f", "wav", "-ar", "48000"};
		}
		if (params.mediaRenderer.isTranscodeAudioTo441()) {
			args[3] = "44100";
		}
		if (!configuration.isAudioResample()) {
			args[2] = "-vn";
			args[3] = "-vn";
		}
		if (params.mediaRenderer.isTranscodeAudioTo441()) {
			args[3] = "44100";
		}
		return getFFMpegTranscode(fileName, dlna, media, params, args);
	}
}
