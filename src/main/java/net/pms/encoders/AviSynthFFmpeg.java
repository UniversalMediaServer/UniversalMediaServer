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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExternalProgramInfo;
import net.pms.util.PlayerUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.ProgramExecutableType;
import net.pms.util.Version;

/*
 * This class handles the Windows-specific AviSynth/FFmpeg player combination.
 */
public class AviSynthFFmpeg extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(AviSynthFFmpeg.class);
	public static final EngineId ID = StandardEngineId.AVI_SYNTH_FFMPEG;
	public static final String NAME = "AviSynth/FFmpeg";

	/** The final AviSynth {@link ExternalProgramInfo} instance set in the constructor */
	@Nonnull
	private final ExternalProgramInfo aviSynthInfo;
	private final ExternalProgramInfo ffms2Info;
	private final ExternalProgramInfo directShowSourceInfo;
	private final ExternalProgramInfo mvtools2Info;
	private final ExternalProgramInfo masktools2Info;
	private final ExternalProgramInfo convert2dTo3dInfo;
	private boolean isAviSynthPlus = false;
	private Path ffms2Path;
	private Path directShowSourcePath;
	private Path mvtools2Path;
	private Path masktools2Path;
	private Path convert2dTo3dPath;

	// Not to be instantiated by anything but PlayerFactory
	AviSynthFFmpeg() {
		aviSynthInfo = CONFIGURATION.getAviSynthPaths();
		ffms2Info = CONFIGURATION.getFFMS2Paths();
		directShowSourceInfo = CONFIGURATION.getDirectShowSourcePaths();
		mvtools2Info = CONFIGURATION.getMvtools2Paths();
		masktools2Info = CONFIGURATION.getMasktools2Paths();
		convert2dTo3dInfo = CONFIGURATION.getConvert2dTo3dPaths();
		if (aviSynthInfo == null) {
			throw new IllegalStateException(
				"Can't instantiate " + this.getClass().getSimpleName() + "because executables() returns null"
			);
		}
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isAviSynthEngine() {
		return true;
	}

	@Override
	public String initialString() {
		String threads = "";
		if (configuration.isFfmpegAviSynthMultithreading()) {
			threads = " -threads " + configuration.getNumberOfCpuCores();
		}
		return configuration.getMPEG2MainSettingsFFmpeg() + " -ab " + configuration.getAudioBitrate() + "k" + threads;
	}

	@Override
	public boolean isGPUAccelerationReady() {
		return true;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		Format format = resource.getFormat();

		if (format != null && format.getIdentifier() == Format.Identifier.WEB) {
			return false;
		}

		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized DLNAMediaSubtitle objects have a null language.
		if (subtitle != null && subtitle.getLang() != null) {
			// The resource needs a subtitle, but this engine implementation does not support subtitles yet
			return false;
		}

		try {
			String audioTrackName = resource.getMediaAudio().toString();
			String defaultAudioTrackName = resource.getMedia().getAudioTracksList().get(0).toString();

			if (!audioTrackName.equals(defaultAudioTrackName)) {
				// This engine implementation only supports playback of the default audio track at this time
				return false;
			}
		} catch (NullPointerException e) {
			LOGGER.trace("AviSynth/FFmpeg cannot determine compatibility based on audio track for " + resource.getSystemName());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.trace("AviSynth/FFmpeg cannot determine compatibility based on default audio track for " + resource.getSystemName());
		}

		return PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(resource, Format.Identifier.OGG);
	}

	@Override
	public ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		//check FFMpeg
		executableInfo = super.testExecutable(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		//TODO: adapt for linux/Mac, then remove this
		if (!Platform.isWindows()) {
			return executableInfo.modify()
				.available(false)
				.errorType(ExecutableErrorType.GENERAL)
				.errorText(
					String.format("Skipping transcoding engine %s as it's not compatible with this platform", this)
				).build();
		}
		//check AviSynth
		if (aviSynthInfo != null) {
			for (ProgramExecutableType executableType : aviSynthInfo.getExecutableTypes()) {
				Path aviSynthPath = aviSynthInfo.getPath(executableType);
				if (aviSynthPath != null && Files.exists(aviSynthPath)) {
					//here we know that avisynth is available
					executableInfo = executableInfo.modify()
						.available(true)
						.build();
					Version version = PlatformUtils.INSTANCE.getFileVersionInfo(aviSynthPath.toString());
					if (version != null) {
						if (version.getMajor() > 2) {
							isAviSynthPlus = true;
							LOGGER.debug(
								"Found AviSynth+ version {}",
								version
							);
						} else {
							LOGGER.info(
								"Found AviSynth version {}",
								version
							);
						}
					}
					break;
				}
			}
		}
		if (!Boolean.TRUE.equals(executableInfo.getAvailable())) {
			executableInfo = executableInfo.modify()
				.available(Boolean.FALSE)
				.errorType(ExecutableErrorType.GENERAL)
				.errorText(
					String.format(Messages.getString("ExecutableXTranscodingEngineNotFound"), executableInfo.getPath(), this)
				).build();
			return executableInfo;
		}
		//check optionals libraries
		if (ffms2Info != null) {
			for (ProgramExecutableType executableType : ffms2Info.getExecutableTypes()) {
				Path ffms2TestPath = ffms2Info.getPath(executableType);
				if (Files.exists(ffms2TestPath)) {
					ffms2Path = ffms2TestPath;
					LOGGER.info("Found AviSynth FFmpegSource2 library");
					break;
				}
			}
		}
		if (directShowSourceInfo != null) {
			for (ProgramExecutableType executableType : directShowSourceInfo.getExecutableTypes()) {
				Path directShowSourceTestPath = directShowSourceInfo.getPath(executableType);
				if (Files.exists(directShowSourceTestPath)) {
					directShowSourcePath = directShowSourceTestPath;
					LOGGER.info("Found AviSynth DirectShowSource library");
					break;
				}
			}
		}
		if (ffms2Path == null && directShowSourcePath == null) {
			executableInfo = executableInfo.modify()
				.available(Boolean.FALSE)
				.errorType(ExecutableErrorType.GENERAL)
				.errorText(
					String.format(Messages.getString("ExecutableXTranscodingEngineNotFound"), executableInfo.getPath(), this)
				).build();
			return executableInfo;
		}
		if (mvtools2Info != null) {
			for (ProgramExecutableType executableType : mvtools2Info.getExecutableTypes()) {
				Path mvtools2TestPath = mvtools2Info.getPath(executableType);
				if (Files.exists(mvtools2TestPath)) {
					mvtools2Path = mvtools2TestPath;
					LOGGER.info("Found AviSynth mvtools2 library");
					break;
				}
			}
		}
		if (masktools2Info != null) {
			for (ProgramExecutableType executableType : masktools2Info.getExecutableTypes()) {
				Path masktools2TestPath = masktools2Info.getPath(executableType);
				if (Files.exists(masktools2TestPath)) {
					masktools2Path = masktools2TestPath;
					LOGGER.info("Found AviSynth masktools2 library");
					break;
				}
			}
		}
		if (convert2dTo3dInfo != null) {
			for (ProgramExecutableType executableType : convert2dTo3dInfo.getExecutableTypes()) {
				Path convert2dTo3dTestPath = convert2dTo3dInfo.getPath(executableType);
				if (Files.exists(convert2dTo3dTestPath)) {
					convert2dTo3dPath = convert2dTo3dTestPath;
					LOGGER.info("Found AviSynth convert2dTo3d script");
					break;
				}
			}
		}

		return executableInfo;
	}

	/*
	 * Generate the AviSynth script based on the user's settings
	 */
	public File getAVSScript(String filename, OutputParams params, String frameRateRatio, String frameRateNumber) throws IOException {
		Renderer renderer = params.getMediaRenderer();
		UmsConfiguration customConfiguration = renderer.getUmsConfiguration();
		double timeSeek = params.getTimeSeek();
		String onlyFileName = filename.substring(1 + filename.lastIndexOf('\\'));
		File file = new File(CONFIGURATION.getTempFolder(), "ums-avs-" + onlyFileName + ".avs");

		try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
			String numerator;
			String denominator;

			if (frameRateRatio != null && frameRateNumber != null) {
				if (frameRateRatio.equals(frameRateNumber)) {
					// No ratio was available
					numerator = frameRateRatio;
					denominator = "1";
				} else {
					String[] frameRateNumDen = frameRateRatio.split("/");
					numerator = frameRateNumDen[0];
					denominator = "1001";
				}
			} else {
				// No framerate was given so we should try the most common one
				numerator = "24000";
				denominator = "1001";
				frameRateNumber = "23.976";
			}

			File f = new File(filename);
			if (f.exists()) {
				filename = ProcessUtil.getShortFileNameIfWideChars(filename);
			}

			String movieLine       		= "";
			String mtLine1         		= "";
			String mtLine2		   		= "";
			String mtPrefetchLine  		= "";
			String interframeLines 		= null;
			String interframePath  		= customConfiguration.getInterFramePath();

			//fallback to FFMS2 if DirectShowSource not found
			if ((directShowSourcePath == null || customConfiguration.getFfmpegAvisynthUseFFMS2()) && ffms2Path != null) {
				// See documentation for FFMS2 here: http://avisynth.nl/index.php/FFmpegSource

				String fpsNum   = "fpsnum=" + numerator;
				String fpsDen   = "fpsden=" + denominator;

				String convertfps = "";

				if (customConfiguration.getFfmpegAvisynthConvertFps()) {
					convertfps = ", " + fpsNum + ", " + fpsDen;
				}

				// cachefile:
				// The filename of the index file (where the indexing data is saved).
				// Defaults to sourcefilename.ffindex, what we don't want as it mess up media folder.
				File cachefile = new File(CONFIGURATION.getTempFolder(), "ums-avs-" + onlyFileName + ".ffindex");
				cachefile.deleteOnExit();

				// atrack:
				// A value of -1 means select the first available audio track. Default (-2) means audio is disabled.

				int audioTrack = -1;

				// seekmode:
				// 1: Safe normal (the default). Bases seeking decisions on the keyframe positions reported by libavformat.
				// 2: Unsafe normal. Same as mode 1, but no error will be thrown if the exact seek destination has to be guessed.

				int seekMode   = 2;

				movieLine += "\n";
				movieLine += "LoadPlugin(\"" + ffms2Path + "\")\n";

				movieLine += "FFMS2(\"" + filename + "\"" + convertfps + ", cachefile=\"" + cachefile.getAbsolutePath() + "\", atrack=" + audioTrack + ", seekmode=" + seekMode + ")";
			} else {
				String assumeFPS = ".AssumeFPS(" + numerator + "," + denominator + ")";

				String directShowFPS = "";
				if (!"0".equals(frameRateNumber)) {
					directShowFPS = ", fps=" + frameRateNumber;
				}

				String convertfps = "";
				if (customConfiguration.getFfmpegAvisynthConvertFps()) {
					convertfps = ", convertfps=true";
				}

				movieLine += "\n";
				movieLine += "LoadPlugin(\"" + directShowSourcePath + "\")\n";
				movieLine       += "DirectShowSource(\"" + filename + "\"" + directShowFPS + convertfps + ")" + assumeFPS;
			}

			int cores = 1;
			if (customConfiguration.isFfmpegAviSynthMultithreading()) {

				cores = customConfiguration.getNumberOfCpuCores();

				if (isAviSynthPlus) {
					// AviSynth+ multi-threading

					// Goes at the start of the file to initiate multithreading
					mtLine1 = "SetFilterMTMode(\"DEFAULT_MT_MODE\", 2)\n";

					// Goes at the end of the script file to support AviSynth+ multithreading
					mtPrefetchLine = "Prefetch(" + cores + ")\n";
				} else {
					// AviSynth multi-threading

					// Goes at the start of the file to initiate multithreading
					mtLine1 = "SetMemoryMax(512)\nSetMTMode(3," + cores + ")\n";
					// Goes after the input line to make multithreading more efficient
					mtLine2 = "SetMTMode(2)";
				}
			}

			// True Motion
			if (customConfiguration.getFfmpegAvisynthInterFrame() && interframePath != null) {
				String gpu = "";
				movieLine += ".ConvertToYV12()";

				// Enable GPU to assist with CPU
				if (customConfiguration.getFfmpegAvisynthInterFrameGPU() && customConfiguration.isGPUAcceleration()) {
					gpu = ", GPU=true";
				}

				interframeLines = "\n" +
					"PluginPath = \"" + interframePath + "\"\n" +
					"LoadPlugin(PluginPath+\"svpflow1.dll\")\n" +
					"LoadPlugin(PluginPath+\"svpflow2.dll\")\n" +
					"Import(PluginPath+\"InterFrame2.avsi\")\n" +
					"InterFrame(Cores=" + cores + gpu + ", Preset=\"Faster\")\n";
			}

			String subLine = null;
			DLNAMediaSubtitle subTrack = params.getSid();
			if (
				subTrack != null &&
				subTrack.isExternal() &&
				customConfiguration.isAutoloadExternalSubtitles() &&
				!customConfiguration.isDisableSubtitles()
			) {
				if (subTrack.getExternalFile() != null) {
					LOGGER.info("AviSynth script: Using subtitle track: {}", subTrack);
					String function = "TextSub";
					if (subTrack.getType() == SubtitleType.VOBSUB) {
						function = "VobSub";
					}
					subLine = function + "(\"" + ProcessUtil.getShortFileNameIfWideChars(subTrack.getExternalFile()) + "\")";
				} else {
					LOGGER.error("External subtitles file \"{}\" is unavailable", subTrack.getName());
				}
			}

			ArrayList<String> lines = new ArrayList<>();

			lines.add(mtLine1);

			boolean fullyManaged = false;
			String script = "<movie>\n<sub>\n";
			StringTokenizer st = new StringTokenizer(script, PMS.AVS_SEPARATOR);
			while (st.hasMoreTokens()) {
				String line = st.nextToken();
				if (line.contains("<movie") || line.contains("<sub")) {
					fullyManaged = true;
				}
				lines.add(line);
			}
			if (customConfiguration.isFfmpegAvisynth2Dto3D() && renderer.getAviSynth2Dto3D() && mvtools2Path != null && masktools2Path != null && convert2dTo3dPath != null) {

				LOGGER.debug("AviSynth will seek to time index: " + timeSeek + " seconds");

				lines.add("video2d=Last");
				lines.add("seekFrame=int(video2d.FrameRate*" + timeSeek + "+0.5)");
				lines.add("video2dFromSeekPoint=Trim(video2d,seekFrame,0)");

				lines.add("\n" +
				"LoadPlugin(\"" + mvtools2Path + "\")\n" +
				"LoadPlugin(\"" + masktools2Path + "\")\n" +
				"Import(\"" + convert2dTo3dPath + "\")\n\n");

				int frameStretchFactor = customConfiguration.getFfmpegAvisynthFrameStretchFactor();
				int lightOffsetFactor = customConfiguration.getFfmpegAvisynthLightOffsetFactor();

				lines.add("convert2dTo3d(video2dFromSeekPoint, algorithm=" + customConfiguration.getFfmpegAvisynthConversionAlgorithm2Dto3D() + ", outputFormat=" + customConfiguration.getFfmpegAvisynthOutputFormat3D() + ", resize=" + customConfiguration.isFfmpegAvisynthHorizontalResize() + ", hzTargetSize=" + customConfiguration.getFfmpegAvisynthHorizontalResizeResolution() + ", frameStretchFactor=" + frameStretchFactor + ", lightOffsetFactor=" + lightOffsetFactor + ")");
			}

			if (customConfiguration.getFfmpegAvisynthInterFrame() && interframePath != null) {
				lines.add(mtLine2);
				lines.add(interframeLines);
			}

			if (customConfiguration.isFfmpegAviSynthMultithreading() && isAviSynthPlus) {
				lines.add(mtPrefetchLine);
			}

			if (fullyManaged) {
				for (String s : lines) {
					if (s.contains("<moviefilename>")) {
						s = s.replace("<moviefilename>", filename);
					}

					s = s.replace("<movie>", movieLine);
					s = s.replace("<sub>", subLine != null ? subLine : "#");
					pw.println(s);
				}
			} else {
				pw.println(movieLine);
				if (subLine != null) {
					pw.println(subLine);
				}
				pw.println("clip");

			}
		}
		file.deleteOnExit();
		return file;
	}

}
