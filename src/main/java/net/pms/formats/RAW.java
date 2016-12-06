package net.pms.formats;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.InputFile;
import net.pms.encoders.RAWThumbnailer;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAW extends JPG {
	private static final Logger LOGGER = LoggerFactory.getLogger(RAW.class);
	private boolean hasEmbeddedThumbnail = false;

	public boolean isEmbeddedThumbnail() {
		return hasEmbeddedThumbnail;
	}

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

	/**
	 * @deprecated Use {@link #isCompatible(DLNAMediaInfo, RendererConfiguration)} instead.
	 * <p>
	 * Returns whether or not a format can be handled by the PS3 natively.
	 * This means the format can be streamed to PS3 instead of having to be
	 * transcoded.
	 * 
	 * @return True if the format can be handled by PS3, false otherwise.
	 */
	@Deprecated
	@Override
	public boolean ps3compatible() {
		return false;
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
			params.waitbeforestart = 0;
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
					hasEmbeddedThumbnail = true;
					break;
				}
			}

			InputStream image;
			if (hasEmbeddedThumbnail) {
				image = RAWThumbnailer.getThumbnail(params, file.getFile().getAbsolutePath());
			} else {
				image = RAWThumbnailer.convertRAWtoPPM(params, file.getFile().getAbsolutePath());
			}

			if (image != null) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				BufferedImage bi;
				if (!hasEmbeddedThumbnail) {
					// resize the converted RAW image and convert to the JPEG format with the max resolution supported by renderer
					out = (ByteArrayOutputStream) UMSUtils.scaleImage(image, configuration.getMaxVideoWidth(), configuration.getMaxVideoHeight(), false, configuration); //convert image to JPEG
					bi = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
				} else {
					bi = ImageIO.read(image);
				}
				// Set the image resolution of the embedded thumbnail or the converted and resized PPM image
				media.setWidth(bi.getWidth());
				media.setHeight(bi.getHeight());
				media.setCodecV("raw");
				media.setContainer("raw");

				if (configuration.getImageThumbnailsEnabled()) {
					// Resize the thumbnail image using the Thumbnailator library
					Thumbnails.of(bi)
								.size(configuration.getThumbnailWidth(), configuration.getThumbnailHeight())
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
