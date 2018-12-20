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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.sun.jna.Platform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.ExecutableInfo;
import net.pms.configuration.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.configuration.ExternalProgramInfo;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.BasicSystemUtils;
import net.pms.io.ListProcessWrapperResult;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SimpleProcessWrapper;
import net.pms.util.PlayerUtil;
import net.pms.util.Version;

/* XXX this is the old/obsolete VLC web video streaming engine */
public class VideoLanVideoStreaming extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(VideoLanVideoStreaming.class);
	public static final PlayerId ID = StandardPlayerId.VLC_VIDEO_STREAMING;

	/** The {@link Configuration} key for the custom VLC path. */
	public static final String KEY_VLC_PATH = "vlc_path";

	/** The {@link Configuration} key for the VLC Legacy Web executable type. */
	public static final String KEY_VLC_LEGACY_EXECUTABLE_TYPE = "vlc_legacy_executable_type";
	public static final String NAME = "VLC Web Video (Legacy)";

	// Not to be instantiated by anything but PlayerFactory
	VideoLanVideoStreaming() {
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_PLAYER;
	}

	@Override
	public PlayerId id() {
		return ID;
	}

	@Override
	public String getConfigurablePathKey() {
		return KEY_VLC_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_VLC_LEGACY_EXECUTABLE_TYPE;
	}

	@Override
	public String[] args() {
		return new String[]{};
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public String mimeType() {
		return "video/mpeg";
	}

	@Override
	protected ExternalProgramInfo programInfo() {
		return configuration.getVLCPaths();
	}

	protected String getEncodingArgs() {
		/*
			FIXME (or, rather, FIXVLC): channels=2 causes various recent VLCs (from 1.1.4 to 1.1.7)
			to segfault on both Windows and Linux.

			Similar issue (the workaround doesn't work here):

			https://forum.videolan.org/viewtopic.php?f=13&t=83154&p=275196#p275034

			Reproduce:

			vlc -vv -I dummy --sout \
			#transcode{vcodec=mp2v,vb=4096,fps=25,scale=1,acodec=mpga,ab=128,channels=2} \
			:standard{access=file,mux=ts,dst="deleteme.tmp"} \
			http://feedproxy.google.com/~r/TEDTalks_video/~5/wdul2VS10rw/BillGates_2011U.mp4 vlc://quit
		 */

		return "vcodec=mp2v,vb=4096,scale=1,acodec=mp2a,ab=128,channels=2";
	}

	protected String getMux() {
		return "ts";
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.mediaRenderer;
		boolean isWindows = Platform.isWindows();
		final String filename = dlna.getFileName();
		PipeProcess tsPipe = new PipeProcess("VLC" + System.currentTimeMillis() + "." + getMux());
		ProcessWrapper pipe_process = tsPipe.getPipeProcess();

		// XXX it can take a long time for Windows to create a named pipe
		// (and mkfifo can be slow if /tmp isn't memory-mapped), so start this as early as possible
		pipe_process.runInNewThread();
		tsPipe.deleteLater();

		params.input_pipes[0] = tsPipe;
		params.minBufferSize = params.minFileSize;
		params.secondread_minsize = 100000;

		List<String> cmdList = new ArrayList<>();
		cmdList.add(getExecutable());
		cmdList.add("-I");
		cmdList.add("dummy");

		// TODO: either
		// 1) add this automatically if enabled (probe)
		// 2) add a GUI option to "enable GPU acceleration"
		// 3) document it as an option the user can enable themselves in the vlc GUI (saved to a config file used by cvlc)
		// XXX: it's still experimental (i.e. unstable), causing (consistent) segfaults on Windows and Linux,
		// so don't even document it for now
		// cmdList.add("--ffmpeg-hw");

		String transcodeSpec = String.format(
			"#transcode{%s}:standard{access=file,mux=%s,dst=\"%s%s\"}",
			getEncodingArgs(),
			getMux(),
			(isWindows ? "\\\\" : ""),
			tsPipe.getInputPipe()
		);

		// XXX there's precious little documentation on how (if at all) VLC
		// treats colons and hyphens (and :name= and --name=) differently
		// so we just have to test it ourselves
		// these work fine on Windows and Linux with VLC 1.1.x

		if (isWindows) {
			cmdList.add("--dummy-quiet");
		}
		if (isWindows || Platform.isMac()) {
			cmdList.add("--sout=" + transcodeSpec);
		} else {
			cmdList.add("--sout");
			cmdList.add(transcodeSpec);
		}

		// FIXME: cargo-culted from here:
		// via: https://code.google.com/p/ps3mediaserver/issues/detail?id=711
		if (Platform.isMac()) {
			cmdList.add("");
		}
		cmdList.add(filename);
		cmdList.add("vlc://quit");

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(pipe_process);

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
		return null;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isWebVideo(resource);
	}

	@Override
	public boolean excludeFormat(Format extension) {
		return false;
	}

	@Override
	public boolean isPlayerCompatible(RendererConfiguration renderer) {
		return true;
	}

	@Override
	public @Nullable ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		ExecutableInfoBuilder result = executableInfo.modify();
		if (Platform.isWindows()) {
			if (executableInfo.getPath().isAbsolute() && executableInfo.getPath().equals(BasicSystemUtils.INSTANCE.getVlcPath())) {
				result.version(BasicSystemUtils.INSTANCE.getVlcVersion());
			}
			result.available(Boolean.TRUE);
		} else {
			final String arg = "--version";
			try {
				ListProcessWrapperResult output = SimpleProcessWrapper.runProcessListOutput(
					30000,
					1000,
					executableInfo.getPath().toString(),
					arg
				);
				if (output.getError() != null) {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("Engine.Error"), this) + " \n" + output.getError().getMessage());
					result.available(Boolean.FALSE);
					LOGGER.debug("\"{} {}\" failed with error: {}", executableInfo.getPath(), arg, output.getError().getMessage());
					return result.build();
				}
				if (output.getExitCode() == 0) {
					if (output.getOutput() != null && output.getOutput().size() > 0) {
						Pattern pattern = Pattern.compile("VLC version\\s+[^\\(]*\\(([^\\)]*)", Pattern.CASE_INSENSITIVE);
						Matcher matcher = pattern.matcher(output.getOutput().get(0));
						if (matcher.find() && isNotBlank(matcher.group(1))) {
							result.version(new Version(matcher.group(1)));
						}
					}
					result.available(Boolean.TRUE);
				} else {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("Engine.ExitCode"), this, output.getExitCode()));
					result.available(Boolean.FALSE);
				}
			} catch (InterruptedException e) {
				return null;
			}
		}
		if (result.version() != null) {
			Version requiredVersion = new Version("2.0.2");
			if (result.version().compareTo(requiredVersion) <= 0) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("Engine.VersionTooLow"), requiredVersion, this));
				result.available(Boolean.FALSE);
				LOGGER.warn(String.format(Messages.getRootString("Engine.VersionTooLow"), requiredVersion, this));
			}
		} else if (result.available() != null && result.available().booleanValue()) {
			LOGGER.warn("Could not parse VLC version, the version might be too low (< 2.0.2)");
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
