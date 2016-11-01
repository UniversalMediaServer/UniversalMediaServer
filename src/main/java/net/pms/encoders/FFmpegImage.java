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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegImage extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegImage.class);
	public static final String ID = "ffmpegimage";

	@Override
	public String name() {
		return "FFmpeg Image";
	}
	
	@Override
	public int type() {
		return Format.IMAGE;
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		PmsConfiguration prev = configuration;
		// Use device-specific pms conf
		configuration = (DeviceConfiguration)params.mediaRenderer;
		final String filename = dlna.getSystemName();
		params.waitbeforestart = 2000;
		params.manageFastStart();
		RendererConfiguration renderer = params.mediaRenderer;

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

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		cmdList.add("-i");
		if (params.stdin == null)
			cmdList.add(filename);
		else
			cmdList.add("-");
		
		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		String scalingFilter = getScalingFilter(dlna, renderer);
		if (scalingFilter != null) {
			cmdList.add("-vf");
			cmdList.add(scalingFilter);
		}
		cmdList.add("-f");
		cmdList.add("image2");
		cmdList.add("-c:v");
		cmdList.add("png");

//		cmdList.add(dlna.getFilename(params.mediaRenderer));
		cmdList.add("pipe:");

		String[] cmdArray = new String[ cmdList.size() ];
		cmdList.toArray(cmdArray);

		cmdArray = finalizeTranscoderArgs(
			filename,
			dlna,
			media,
			params,
			cmdArray
		);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		configuration = prev;
		return pw;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (PlayerUtil.isImage(resource) || resource.getType() == Format.IMAGE) {
			return true;
		}

		return false;
	}
}
