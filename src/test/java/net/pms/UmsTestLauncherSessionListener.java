package net.pms;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Marks the JVM as a test run before any test class is discovered or loaded.
 *
 * Registered as a service in
 * {@code src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class UmsTestLauncherSessionListener implements LauncherSessionListener {

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
	}

}
