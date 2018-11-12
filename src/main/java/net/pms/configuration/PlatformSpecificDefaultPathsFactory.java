package net.pms.configuration;

import com.sun.jna.Platform;

/**
 * Determines how we look for a program.
 * 
 * @author Tim Cox (mail@tcox.org)
 */
class PlatformSpecificDefaultPathsFactory {
	ProgramPaths get() {
		if (Platform.isWindows()) {
			return new WindowsDefaultPaths();
		} else if (Platform.isMac()) {
			return new MacDefaultPaths();
		} else {
			return new LinuxDefaultPaths();
		}
	}
}
