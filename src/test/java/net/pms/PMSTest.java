package net.pms;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMSTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PMSTest.class);

	@Mock
	PMS mockPMS;

	@Mock
	File file;

	@BeforeEach
	public void setUp() {
		mockPMS = Mockito.mock(PMS.class);
		file = Mockito.mock(File.class);
		System.setProperty("user.dir", "TestUserDir");
		System.setProperty("java.version", "TestJavaVer");
	}

	@AfterEach
	public void tearDown() {
		System.clearProperty("user.dir");
		System.clearProperty("java.version");
	}

	@Test
	public void testCheckCompatibilityWithJavaVersion() {
		try {
			MavenXpp3Reader reader = Mockito.mock(MavenXpp3Reader.class);
			Model mockModel = new Model();
			FileReader fileReader = Mockito.mock(FileReader.class);
			Mockito.when(reader.read(fileReader)).thenReturn(mockModel);
			mockPMS.checkCompatibilityWithJavaVersion();
		} catch (Exception e) {
			LOGGER.error("Exception caught : ", e);
		}
	}
}
