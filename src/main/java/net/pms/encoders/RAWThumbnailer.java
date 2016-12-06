package net.pms.encoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
import net.pms.util.UMSUtils;

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

		InputStream image = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			image = getThumbnail(params, filename);
			out = (ByteArrayOutputStream) UMSUtils.scaleImage(image, media.getWidth(), media.getHeight(), false, configuration); //convert image to JPEG
		} catch (IOException e) {
			LOGGER.error("Error extracting thumbnail", e);
		}

		byte[] ba = out.toByteArray();
		media.setSize(ba.length);
		ProcessWrapper pw = new InternalJavaProcessImpl(new ByteArrayInputStream(ba));
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
	/**
	 * Extract the embedded thumbnail image from the original raw file
	 *
	 * @param params Parameters to be used for Process Wrapper 
	 * @param fileName Name of the RAW image from which the thumbnail is to be extracted
	 * @return The Byte Array of the embedded thumbnail
	 */
	public static InputStream getThumbnail(OutputParams params, String fileName) throws IOException {
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
		return pw.getInputStream(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return PlayerUtil.isImage(resource, Format.Identifier.RAW);
	}

	/**
	 * Convert the RAW image to PPM format
	 *
	 * @param params Parameters to be used for Process Wrapper 
	 * @param fileName Name of the RAW image to be converted
	 * @return The Byte Array of the PPM format
	 */
	public static InputStream convertRAWtoPPM(OutputParams params, String fileName) throws IOException {
		params.log = false;
		String cmdArray[] = new String[3];
		cmdArray[0] = PMS.getConfiguration().getDCRawPath();
		cmdArray[1] = "-c";
		cmdArray[2] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInSameThread();
		return pw.getInputStream(0);
	}
}
