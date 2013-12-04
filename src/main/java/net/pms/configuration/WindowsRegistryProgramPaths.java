package net.pms.configuration;

import java.io.File;
import net.pms.PMS;
import net.pms.io.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WindowsRegistryProgramPaths implements ProgramPaths {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsRegistryProgramPaths.class);
	private final ProgramPaths defaults;

	WindowsRegistryProgramPaths(ProgramPaths defaults) {
		this.defaults = defaults;
	}

	@Override
	public String getFfmpegPath() {
		return defaults.getFfmpegPath();
	}

	@Override
	public String getFlacPath() {
		return defaults.getFlacPath();
	}

	@Override
	public String getMencoderPath() {
		return defaults.getMencoderPath();
	}

	@Override
	public String getMplayerPath() {
		return defaults.getMplayerPath();
	}

	@Override
	public String getTsmuxerPath() {
		return defaults.getTsmuxerPath();
	}

	@Override
	public String getTsmuxerNewPath() {
		return defaults.getTsmuxerNewPath();
	}

	@Override
	public String getVlcPath() {
		SystemUtils registry = PMS.get().getRegistry();
		if (registry.getVlcPath() != null) {
			String vlc = registry.getVlcPath();
			String version = registry.getVlcVersion();
			if (new File(vlc).exists() && version != null) {
				LOGGER.debug("Found VLC version " + version + " in Windows Registry: " + vlc);
				return vlc;
			}
		}

		return defaults.getVlcPath();
	}

	@Override
	public String getDCRaw() {
		return defaults.getDCRaw();
	}

	@Override
	public String getInterFramePath() {
		return defaults.getInterFramePath();
	}
}
