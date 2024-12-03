/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.encoders;

import java.io.IOException;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.platform.PlatformUtils;
import net.pms.store.StoreItem;
import net.pms.util.PlayerUtil;
import net.pms.util.UMSUtils;

public class MEncoderWebVideo extends MEncoderVideo {
	public static final EngineId ID = StandardEngineId.MENCODER_WEB_VIDEO;

	/** The {@link Configuration} key for the DCRaw executable type. */
	public static final String KEY_MENCODER_WEB_EXECUTABLE_TYPE = "mencoder_web_executable_type";
	public static final String NAME = "MEncoder Web Video";

	// Not to be instantiated by anything but PlayerFactory
	MEncoderWebVideo() {
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_MENCODER_WEB_EXECUTABLE_TYPE;
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_ENGINE;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public String getMimeType() {
		return HTTPResource.MPEG_TYPEMIME;
	}

	protected String[] getDefaultArgs(UmsConfiguration configuration) {
		int nThreads = configuration.getMencoderMaxThreads();
		String acodec = configuration.isMencoderAc3Fixed() ? "ac3_fixed" : "ac3";
		return new String[]{
				"-msglevel", "all=2",
				"-quiet",
				"-prefer-ipv4",
				"-cache", "16384",
				"-oac", "lavc",
				"-of", "lavf",
				"-lavfopts", "format=dvd",
				"-ovc", "lavc",
				"-lavcopts", "vcodec=mpeg2video:vbitrate=4096:threads=" + nThreads + ":acodec=" + acodec + ":abitrate=128",
				"-vf", "harddup",
				"-ofps", "25"
			};
	}

	@Override
	public ProcessWrapper launchTranscode(
		StoreItem resource,
		MediaInfo media,
		OutputParams params) throws IOException {
		// Use device-specific pms conf
		UmsConfiguration configuration = params.getMediaRenderer().getUmsConfiguration();
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);

		IPipeProcess pipe = PlatformUtils.INSTANCE.getPipeProcess("mencoder" + System.currentTimeMillis());
		params.getInputPipes()[0] = pipe;

		String[] defaultArgs = getDefaultArgs(configuration);
		String[] cmdArray = new String[defaultArgs.length + 4];
		cmdArray[0] = getExecutable();
		final String filename = resource.getFileName();
		cmdArray[1] = filename;
		System.arraycopy(defaultArgs, 0, cmdArray, 2, defaultArgs.length);
		cmdArray[cmdArray.length - 2] = "-o";
		cmdArray[cmdArray.length - 1] = pipe.getInputPipe();

		ProcessWrapper mkfifoProcess = pipe.getPipeProcess();

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifoProcess);

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifoProcess.runInSameThread();

		pipe.deleteLater();

		pw.runInNewThread();

		// Not sure what good this 50ms wait will do for the calling method.
		UMSUtils.sleep(50);
		return pw;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isCompatible(StoreItem item) {
		return PlayerUtil.isWebVideo(item);
	}

}
