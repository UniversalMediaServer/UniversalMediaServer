package net.pms.network.mediaserver.handlers.api;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import net.pms.configuration.DeviceConfiguration;

public class ApiRenderer extends DeviceConfiguration {

	public ApiRenderer() throws ConfigurationException, InterruptedException {
		super(NOFILE, null);
	}

	@Override
	public boolean load(File f) {
		configuration.addProperty(MEDIAPARSERV2, true);
		configuration.addProperty(MEDIAPARSERV2_THUMB, true);
		configuration.addProperty(SUPPORTED, "f:mpegts v:h264 a:aac-lc|aac-ltp|aac-main|aac-ssr|he-aac|ac3|eac3 m:video/mp2t");
		configuration.addProperty(SUPPORTED, "f:flac  m:audio/x-flac");
		configuration.addProperty(SUPPORTED, "f:m4a|3ga   a:alac          m:audio/x-m4a");
		configuration.addProperty(SUPPORTED, "f:oga a:vorbis|flac m:audio/ogg");
		configuration.addProperty(SUPPORTED, "f:m4a|3ga   a:aac-lc|he-aac m:audio/x-m4a");
		configuration.addProperty(SUPPORTED, "f:wav                       m:audio/wav");
		configuration.addProperty(SUPPORTED, "f:m4a|3ga   a:aac-lc|he-aac m:audio/x-m4a");
		return true;
	}

	public String getRendererName() {
		return "API renderer";
	}

}
