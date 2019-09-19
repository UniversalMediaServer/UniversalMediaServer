package net.pms.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UMSUtilsTest {
	/**
	 * Set up testing conditions before running the tests.
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("static-method")
	@Before
	public final void setUp() throws ConfigurationException, InterruptedException {
		// Silence all log messages from the DMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

	@Test
	public void testPlayedDurationStr() throws Exception {
		assertThat(UMSUtils.playedDurationStr("01:23:45:67.89", "01:23:45:67")).isEqualTo("23:45:67 / 23:45:67");
		assertThat(UMSUtils.playedDurationStr("01:23", "01:23:45")).isEqualTo("1:23 / 1:23:45");
		assertThat(UMSUtils.playedDurationStr(":12", "59")).isEqualTo("0:12 / 0:59");
	}
}
