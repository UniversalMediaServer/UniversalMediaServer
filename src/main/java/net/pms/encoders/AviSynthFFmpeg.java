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

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.annotation.Nonnull;
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
import net.pms.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class handles the Windows-specific AviSynth/FFmpeg player combination.
 */
public class AviSynthFFmpeg extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(AviSynthFFmpeg.class);
	public static final EngineId ID = StandardEngineId.AVI_SYNTH_FFMPEG;
	public static final String NAME = "AviSynth/FFmpeg";
	public static boolean isAviSynthPlus = false;
	/** The final AviSynth {@link ExternalProgramInfo} instance set in the constructor */
	@Nonnull
	private final ExternalProgramInfo aviSynthInfo;

	// Not to be instantiated by anything but PlayerFactory
	AviSynthFFmpeg() {
		aviSynthInfo = CONFIGURATION.getAviSynthPaths();
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
		ExecutableInfo aviSynthExecutableInfo = aviSynthInfo.getExecutableInfo(aviSynthInfo.getDefault());
		if (aviSynthExecutableInfo == null) {
			return executableInfo.modify()
				.available(false)
				.errorType(ExecutableErrorType.GENERAL)
				.errorText(
					"Can't instantiate " + this.getClass().getSimpleName() + " because executables() returns null"
				).build();
		}
		executableInfo = testExecutableFile(aviSynthExecutableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		Version version = PlatformUtils.INSTANCE.getFileVersionInfo(executableInfo.getPath().toString());
		if (version != null) {
			LOGGER.debug(
				"Founded AviSynth version {}",
				version
			);
			if (version.getBuild() > 2) {
				LOGGER.debug(
					"Founded AviSynth+ version {}",
					version
				);
			} else {
				LOGGER.info(
					"Founded AviSynth version {}",
					version
				);
			}
			executableInfo = executableInfo.modify()
				.version(version)
				.build();
		}
		executableInfo = executableInfo.modify()
			.available(true)
			.build();
		return executableInfo;
	}

	/*
	 * Generate the AviSynth script based on the user's settings
	 */
	public static File getAVSScript(String filename, OutputParams params, String frameRateRatio, String frameRateNumber) throws IOException {
		Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		double timeSeek = params.getTimeSeek();
		String onlyFileName = filename.substring(1 + filename.lastIndexOf('\\'));
		File file = new File(configuration.getTempFolder(), "ums-avs-" + onlyFileName + ".avs");
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
			String interframePath  		= configuration.getInterFramePath();
			String ffms2Path 	   		= configuration.getFFMS2Path();
			String convert2dTo3DPath	= configuration.getConvert2dTo3dPath();
			String aviSynthPath	    	= configuration.getAviSynthPath();
			String mvtools2Path	    	= configuration.getMvtools2Path();

			if (configuration.getFfmpegAvisynthUseFFMS2() && ffms2Path != null) {
				// See documentation for FFMS2 here: http://avisynth.nl/index.php/FFmpegSource

				String fpsNum   = "fpsnum=" + numerator;
				String fpsDen   = "fpsden=" + denominator;

				String convertfps = "";

				if (configuration.getFfmpegAvisynthConvertFps()) {
					convertfps = ", " + fpsNum + ", " + fpsDen;
				}

				// atrack:
				// A value of -1 means select the first available audio track. Default (-2) means audio is disabled.

				int audioTrack = -1;

				// seekmode:
				// 1: Safe normal (the default). Bases seeking decisions on the keyframe positions reported by libavformat.
				// 2: Unsafe normal. Same as mode 1, but no error will be thrown if the exact seek destination has to be guessed.

				int seekMode   = 2;

				movieLine += "\n" +
				"PluginPath = \"" + ffms2Path + "\"\n" +
				"LoadPlugin(PluginPath+\"\\ffms2.dll\")\n";

				movieLine += "FFMS2(\"" + filename + "\"" + convertfps + ", atrack=" + audioTrack + ", seekmode=" + seekMode + ")";
			} else {
				String assumeFPS = ".AssumeFPS(" + numerator + "," + denominator + ")";

				String directShowFPS = "";
				if (!"0".equals(frameRateNumber)) {
					directShowFPS = ", fps=" + frameRateNumber;
				}

				String convertfps = "";
				if (configuration.getFfmpegAvisynthConvertFps()) {
					convertfps = ", convertfps=true";
				}

				movieLine       += "DirectShowSource(\"" + filename + "\"" + directShowFPS + convertfps + ")" + assumeFPS;
			}

			int cores = 1;
			if (configuration.isFfmpegAviSynthMultithreading()) {

				cores = configuration.getNumberOfCpuCores();

				if (PlatformUtils.INSTANCE.isAviSynthPlusAvailable()) {
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
			if (configuration.getFfmpegAvisynthInterFrame() && interframePath != null) {
				String gpu = "";
				movieLine += ".ConvertToYV12()";

				// Enable GPU to assist with CPU
				if (configuration.getFfmpegAvisynthInterFrameGPU() && configuration.isGPUAcceleration()) {
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
				configuration.isAutoloadExternalSubtitles() &&
				!configuration.isDisableSubtitles()
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
			if (configuration.isFfmpegAvisynth2Dto3D() && renderer.getAviSynth3DFormat() > 0 && convert2dTo3DPath != null) {

				lines.add("video2d=Last");
				lines.add("seekFrame=int(video2d.FrameRate*" + timeSeek + "+0.5)");
				lines.add("video2dFromSeekPoint=Trim(video2d,seekFrame,0)");

				lines.add("\n" +
				"PluginPath = \"" + convert2dTo3DPath + "\"\n" +
				"LoadPlugin(PluginPath+\"\\mvtools2.dll\")\n" +
				"Import(PluginPath+\"\\convert2dto3d.avsi\")\n\n");

				String frameStretchFactor = configuration.getFfmpegAvisynthFrameStretchFactor();
				String lightOffsetFactor = configuration.getFfmpegAvisynthLightOffsetFactor();

				lines.add("convert2dTo3d(video2dFromSeekPoint, algorithm=" + configuration.getFfmpegAvisynthConversionAlgorithm2Dto3D() + ", outputFormat=" + renderer.getAviSynth3DFormat() + ", resize=" + configuration.isFfmpegAvisynthHorizontalResize() + ", hzTargetSize=" + configuration.getFfmpegAvisynthHorizontalResizeResolution() + ", frameStretchFactor=" + frameStretchFactor + ", lightOffsetFactor=" + lightOffsetFactor + ")");

				// lines.add("subtitle( \"Time Seek (Seconds)=" + timeSeek + ", Frame Rate=\"+String(video2d.FrameRate)+\", Seek Frame=\"+String(seekFrame), align=5, size=64)");
			}

			if (configuration.getFfmpegAvisynthInterFrame() && interframePath != null) {
				lines.add(mtLine2);
				lines.add(interframeLines);
			}

			if (configuration.isFfmpegAviSynthMultithreading() && PlatformUtils.INSTANCE.isAviSynthPlusAvailable()) {
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
