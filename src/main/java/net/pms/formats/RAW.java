package net.pms.formats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import net.coobird.thumbnailator.Thumbnails;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.encoders.RAWThumbnailer;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAW extends JPG {
	private static final Logger LOGGER = LoggerFactory.getLogger(RAW.class);

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.RAW;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"3fr",
			"ari",
			"arw",
			"bay",
			"cap",
			"cr2",
			"crw",
			"dcr",
			"dcs",
			"dng",
			"drf",
			"eip",
			"erf",
			"fff",
			"iiq",
			"k25",
			"kdc",
			"mdc",
			"mef",
			"mos",
			"mrw",
			"nef",
			"nrw",
			"obm",
			"orf",
			"pef",
			"ptx",
			"pxn",
			"r3d",
			"raf",
			"raw",
			"rw2",
			"rwl",
			"rwz",
			"sr2",
			"srf",
			"srw",
			"x3f"
		};
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	@Override
	public void parse(DLNAMediaInfo media, InputFile file, int type, RendererConfiguration renderer) {
		PmsConfiguration configuration = PMS.getConfiguration(renderer);
		try {
			OutputParams params = new OutputParams(configuration);
			params.waitbeforestart = 1;
			params.minBufferSize = 1;
			params.maxBufferSize = 6;
			params.hidebuffer = true;

			String cmdArray[] = new String[4];
			cmdArray[0] = configuration.getDCRawPath();
			cmdArray[1] = "-i";
			cmdArray[2] = "-v";
			if (file.getFile() != null) {
				cmdArray[3] = file.getFile().getAbsolutePath();
			}

			params.log = true;
			ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params, true, false);
			pw.runInSameThread();

			List<String> list = pw.getOtherResults();
			for (String s : list) {
				if (s.startsWith("Thumb size:  ")) {
					String sz = s.substring(13);
					media.setWidth(Integer.parseInt(sz.substring(0, sz.indexOf('x')).trim()));
					media.setHeight(Integer.parseInt(sz.substring(sz.indexOf('x') + 1).trim()));
					break;
				}
			}

			if (media.getWidth() > 0) {
				byte[] image = RAWThumbnailer.getThumbnail(params, file.getFile().getAbsolutePath());
				media.setSize(image.length);
				// XXX why the image size is set to thumbnail size and the codecV and container is set to RAW when thumbnail is in the JPEG format
				media.setCodecV("raw");
				media.setContainer("raw");

				if (configuration.getImageThumbnailsEnabled()) {
					// Resize the thumbnail image using the Thumbnailator library
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					Thumbnails.of(new ByteArrayInputStream(image))
								.size(320, 180)
								.outputFormat("JPEG")
								.outputQuality(1.0f)
								.toOutputStream(out);
					media.setThumb(out.toByteArray());
				}
			}

			media.finalize(type, file);
			media.setMediaparsed(true);
		} catch (Exception e) {
			LOGGER.debug("Caught exception", e);
		}
	}
}
