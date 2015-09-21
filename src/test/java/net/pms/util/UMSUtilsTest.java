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
	@Before
	public final void setUp() throws ConfigurationException {
		// Silence all log messages from the PMS code that is being tested
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
		PMS.get();
		PMS.setConfiguration(new PmsConfiguration(false));
	}

	@Test
	public void testNormalizeDurationString() throws Exception {
		assertThat(UMSUtils.normalizeDurationString("01:23:45:67.89")).isEqualTo("23:45:67");
		assertThat(UMSUtils.normalizeDurationString("01:23:45:67")).isEqualTo("23:45:67");
		assertThat(UMSUtils.normalizeDurationString("01:23:45")).isEqualTo("01:23:45");
		assertThat(UMSUtils.normalizeDurationString("01:23")).isEqualTo("00:01:23");
		assertThat(UMSUtils.normalizeDurationString("0:12")).isEqualTo("00:00:12");
	}
}
