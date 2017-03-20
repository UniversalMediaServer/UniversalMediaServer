package net.pms.encoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.PlayerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAWThumbnailer extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(RAWThumbnailer.class);
	public final static String ID = "rawthumbs";

	protected String[] getDefaultArgs() {
		return new String[]{ "-e", "-c" };
	}

	@Override
	public String[] args() {
		return getDefaultArgs();
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public String executable() {
		return configuration.getDCRawPath();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		PmsConfiguration prev = configuration;
		// Use device-specific pms conf
		configuration = (DeviceConfiguration)params.mediaRenderer;
		params.waitbeforestart = 1;
		params.minBufferSize = 1;
		params.maxBufferSize = 6;
		params.hidebuffer = true;
		final String filename = dlna.getSystemName();

		if (media == null) {
			return null;
		}

		byte image[] = null;
		try {
			image = getThumbnail(params, filename);
		} catch (IOException e) {
			LOGGER.error("Error extracting thumbnail", e);
		}

		ProcessWrapper pw = new InternalJavaProcessImpl(new ByteArrayInputStream(image));
		configuration = prev;
		return pw;
	}

	@Override
	public String mimeType() {
		return "image/jpeg";
	}

	@Override
	public String name() {
		return "dcraw Thumbnailer";
	}

	@Override
	public int purpose() {
		return MISC_PLAYER;
	}

	@Override
	public int type() {
		return Format.IMAGE;
	}

	// Called from net.pms.formats.RAW.parse XXX even if the engine is disabled
	// May also be called from launchTranscode
	public static byte[] getThumbnail(OutputParams params, String fileName) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration configuration = PMS.getConfiguration(params);
		params.log = false;

		String cmdArray[] = new String[4];
		cmdArray[0] = configuration.getDCRawPath();
		cmdArray[1] = "-e";
		cmdArray[2] = "-c";
		cmdArray[3] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInSameThread();
		ByteArrayOutputStream baos;
		try (InputStream is = pw.getInputStream(0)) {
			baos = new ByteArrayOutputStream();
			int n = -1;
			byte buffer[] = new byte[4096];
			while ((n = is.read(buffer)) > -1) {
				baos.write(buffer, 0, n);
			}
		}
		byte b[] = baos.toByteArray();
		baos.close();
		return b;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isImage(resource, Format.Identifier.RAW);
	}
}
