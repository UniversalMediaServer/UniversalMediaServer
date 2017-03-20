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
package net.pms.io;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.IPushOutput;

public class OutputParams {
	@Deprecated
	public File outputFile; // XXX no longer used

	// TODO: Refactor all public variables to private with public getters and setters.
	public File workDir;
	public Map<String,String> env;
	public double minFileSize;
	public double minBufferSize;
	public double maxBufferSize;
	public double timeseek;
	public double timeend;
	public int fromFrame;
	public int toFrame;
	public int waitbeforestart;
	public PipeProcess[] input_pipes = new PipeProcess[2];
	public PipeProcess[] output_pipes = new PipeProcess[2];
	public DLNAMediaAudio aid;
	public DLNAMediaSubtitle sid;
	public int secondread_minsize;
	public boolean noexitcheck;
	public boolean log;
	public boolean lossyaudio;
	public boolean losslessaudio;
	public boolean no_videoencode;
	public String forceFps;
	public String forceType;
	public RendererConfiguration mediaRenderer;
	public boolean hidebuffer;
	public byte header[];
	public IPushOutput stdin;
	public boolean avidemux;
	public boolean shift_scr;
	public boolean cleanup;

	public OutputParams(PmsConfiguration configuration) {
		if (configuration != null) {
			waitbeforestart = configuration.getVideoTranscodeStartDelay() * 1000;
		} else {
			waitbeforestart = 6000;
		}
		fromFrame = -1;
		toFrame = -1;
		secondread_minsize = 1000000;
		if (configuration != null) {
			minFileSize = configuration.getMinStreamBuffer();
			minBufferSize = configuration.getMinMemoryBufferSize();
			maxBufferSize = configuration.getMaxMemoryBufferSize();
		}
		if (maxBufferSize < 100) {
			maxBufferSize = 100;
		}
		timeseek = 0;
		env = null;
	}

	/**
	 * Set some values to allow fast streaming start of transcoded videos
	 */
	public void manageFastStart() {
		if (mediaRenderer != null && mediaRenderer.isTranscodeFastStart()) {
			waitbeforestart = 0; // no delay when the transcode is starting
			minBufferSize = 1; // 1Mb of minimum buffer before sending the file
		}
	}

	@Override
	public String toString() {
		return "OutputParams [aid=" + aid +
			", avidemux=" + avidemux +
			", cleanup=" + cleanup +
			", forceFps=" + forceFps +
			", forceType=" + forceType +
			", fromFrame=" + fromFrame +
			", header=" + Arrays.toString(header) +
			", hidebuffer=" + hidebuffer +
			", input_pipes=" + Arrays.toString(input_pipes) +
			", log=" + log +
			", losslessaudio=" + losslessaudio +
			", lossyaudio=" + lossyaudio +
			", maxBufferSize=" + maxBufferSize +
			", mediaRenderer=" + mediaRenderer +
			", minBufferSize=" + minBufferSize +
			", minFileSize=" + minFileSize +
			", no_videoencode=" + no_videoencode +
			", noexitcheck=" + noexitcheck +
			", output_pipes=" + Arrays.toString(output_pipes) +
			", secondread_minsize=" + secondread_minsize +
			", shift_scr=" + shift_scr +
			", sid=" + sid +
			", stdin=" + stdin +
			", timeend=" + timeend +
			", timeseek=" + timeseek +
			", toFrame=" + toFrame +
			", waitbeforestart=" + waitbeforestart +
			", workDir=" + workDir +
			", env=" + env + "]";
	}
}
