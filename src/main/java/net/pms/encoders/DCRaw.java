package net.pms.encoders;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.ImageInfo;
import net.pms.formats.Format;
import net.pms.io.InternalJavaProcessImpl;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

public class DCRaw extends ImagePlayer {
	public final static String ID = "dcraw";

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
		if (media == null || dlna == null) {
			return null;
		}

		final String filename = dlna.getSystemName();
		byte[] image = getImage(params, filename, media.getImageInfo());

		ProcessWrapper pw = new InternalJavaProcessImpl(new ByteArrayInputStream(image));
		return pw;
	}

	@Override
	public String mimeType() {
		return "image/jpeg";
	}

	@Override
	public String name() {
		return "DCRaw";
	}

	@Override
	public int purpose() {
		return MISC_PLAYER;
	}

	/**
	 * Converts {@code fileName} into PPM format.
	 *
	 * @param params the {@link OutputParams} to use. Can be {@code null}.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file. Can be {@code null}.
	 * @return A byte array containing the converted image or {@code null}.
	 * @throws IOException if an IO error occurs.
	 */
	public byte[] getImage(OutputParams params, String fileName, ImageInfo imageInfo) {
		if (params == null) {
			params = new OutputParams(PMS.getConfiguration());
		}

		// Use device-specific pms conf
		PmsConfiguration configuration = PMS.getConfiguration(params);

		params.log = false;
		// Setting the buffer to the size of the source file or 5 MB. The
		// output won't be the same size as the input, but it will hopefully
		// give us a somewhat relevant buffer size. Every time the buffer has
		// to grow, the whole buffer must be copied in memory.
		params.outputByteArrayStreamBufferSize =
			imageInfo != null &&
				imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ?
				(int) imageInfo.getSize() : 5000000;

		// First try to get the embedded thumbnail
		String cmdArray[] = new String[5];
		cmdArray[0] = configuration.getDCRawPath();
		cmdArray[1] = "-c";
		cmdArray[2] = "-M";
		cmdArray[3] = "-w";
		cmdArray[4] = fileName;
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, true, params, false, true);
		pw.runInSameThread();
		byte[] bytes = pw.getOutputByteArray().toByteArray();
		return bytes != null && bytes.length > 0 ? bytes : null;
	}

	/**
	 * Extracts or generates a thumbnail for {@code fileName}.
	 *
	 * @param params the {@link OutputParams} to use. Can be {@code null}.
	 * @param fileName the path of the image file to process.
	 * @param imageInfo the {@link ImageInfo} for the image file. Can be
	 *            {@code null}.
	 * @return A byte array containing the thumbnail or {@code null}.
	 * @throws IOException if an IO error occurs.
	 */
	public byte[] getThumbnail(OutputParams params, String fileName, ImageInfo imageInfo) {
		if (params == null) {
			params = new OutputParams(PMS.getConfiguration());
		}

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
		byte[] bytes = pw.getOutputByteArray().toByteArray();
		List<String> results = pw.getResults();

		// No embedded thumbnail retrieved, generate thumbnail from the actual file
		if (bytes.length == 0 && !results.isEmpty() && results.get(0).contains("has no thumbnail")) {
			params.outputByteArrayStreamBufferSize =
				imageInfo != null &&
					imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ?
					(int) imageInfo.getSize() / 4 : 500000;
			cmdArray[1] = "-h";
			pw = new ProcessWrapperImpl(cmdArray, true, params);

			pw.runInSameThread();
			bytes = pw.getOutputByteArray().toByteArray();
		}
		return bytes != null && bytes.length > 0 ? bytes : null;
	}

	/**
	 * Parses {@code file} and stores the result in {@code media}.
	 *
	 * @param media the {@link DLNAMediaInfo} instance to store the parse
	 *            results in.
	 * @param file the {@link File} to parse.
	 */
    public void parse(DLNAMediaInfo media, File file) {
    	if (media == null) {
    		throw new NullPointerException("media cannot be null");
    	}
    	if (file == null) {
    		throw new NullPointerException("file cannot be null");
    	}

		OutputParams params = new OutputParams(configuration);
		params.log = true;

		String cmdArray[] = new String[4];
		cmdArray[0] = configuration.getDCRawPath();
		cmdArray[1] = "-i";
		cmdArray[2] = "-v";
		cmdArray[3] = file.getAbsolutePath();

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params, true, false);
		pw.runInSameThread();

		List<String> list = pw.getOtherResults();
		Pattern pattern = Pattern.compile("^Output size:\\s*(\\d+)\\s*x\\s*(\\d+)");
		Matcher matcher;
		for (String s : list) {
			matcher = pattern.matcher(s);
			if (matcher.find()) {
				media.setWidth(Integer.parseInt(matcher.group(1)));
				media.setHeight(Integer.parseInt(matcher.group(2)));
				break;
			}
		}
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return resource != null && resource.getFormat() != null && resource.getFormat().getIdentifier() == Format.Identifier.RAW;
	}
}
