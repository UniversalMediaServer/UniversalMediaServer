package net.pms.encoders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.ImageInfo;
import net.pms.formats.Format;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
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
		params.waitbeforestart = 0;
		params.minBufferSize = 1;
		params.maxBufferSize = 6;
		params.hidebuffer = true;
		params.outputByteArrayStreamBufferSize =
			media != null &&
			media.getImageInfo() != null &&
			media.getImageInfo().getSize() != ImageInfo.SIZE_UNKNOWN ?
				(int) media.getImageInfo().getSize() / 2 : 500000;
		final String filename = dlna.getSystemName();

		if (media == null) {
			return null;
		}

		byte[] image = null;
		try {
			image = getThumbnail(params, filename, null);
		} catch (IOException e) {
			LOGGER.error("Error extracting thumbnail: {}", e.getMessage());
			LOGGER.trace("", e);
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

	public static byte[] getThumbnail(OutputParams params, String fileName, ImageInfo imageInfo) throws IOException {
		// Use device-specific pms conf
		PmsConfiguration configuration = PMS.getConfiguration(params);
		params.log = false;

		// This is a wild guess at a decent buffer size for an embedded thumbnail.
		// Every time the buffer has to grow, the whole buffer must be copied in memory.
		params.outputByteArrayStreamBufferSize = 300000;

		// First try to get the embedded thumbnail
		String cmdArray[] = new String[6];
		cmdArray[0] = configuration.getDCRawPath();
		cmdArray[1] = "-e";
		cmdArray[2] = "-c";
		cmdArray[3] = "-M";
		cmdArray[4] = "-w";
		cmdArray[5] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params, false, true);
		pw.runInSameThread();
		byte[] b = pw.getOutputByteArray().toByteArray();
		List<String> results = pw.getResults();

		// No embedded thumbnail retrieved, generate thumbnail from the actual file
		if (b.length == 0 && !results.isEmpty() && results.get(0).contains("has no thumbnail")) {
			params.outputByteArrayStreamBufferSize =
				imageInfo != null &&
					imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ?
					(int) imageInfo.getSize() / 4 : 500000;
			cmdArray[1] = "-h";
			pw = new ProcessWrapperImpl(cmdArray, true, params);

			pw.runInSameThread();
			b = pw.getOutputByteArray().toByteArray();
		}
		return b != null && b.length > 0 ? b : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return resource != null && resource.getFormat() != null && resource.getFormat().getIdentifier() == Format.Identifier.RAW;
	}

	/**
	 * Converts Exif orientation to DCRaw flip parameter for valid values.
	 *
	 * @param exifOrientation the Exif orientation to convert.
	 * @return The converted value or {@code 0}.
	 */
	public static int exifOrientationToDCRaw(int exifOrientation) {
		switch (exifOrientation) {
			case 3:
				return 3;
			case 6:
				return 6;
			case 8:
				return 5;
			default:
				return 0;
		}
	}
}
