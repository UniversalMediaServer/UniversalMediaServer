package net.pms.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.MediaDatabase;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

public class HashCacheTest {

    @BeforeEach
    public final void setUp() throws ConfigurationException, InterruptedException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
        PMS.get();
        PMS.setConfiguration(new PmsConfiguration(false));
        MediaDatabase.init();
    }

    @Test
    public void testCanAddDummyHashToCache() throws URISyntaxException, IOException {
        String dummyHash = "abcdef123456";
        Path path = Paths.get("../resources/project.properties");
        HashCacheUtil.addHashToCache(path, dummyHash);
        var result = HashCacheUtil.getHashFromCache(path);
        assertEquals(dummyHash, result);
    }
}
