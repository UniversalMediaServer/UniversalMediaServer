package net.pms.formats;

import java.util.List;
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
	private static final PmsConfiguration configuration = PMS.getConfiguration();

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
		try {
			OutputParams params = new OutputParams(configuration);
			params.waitbeforestart = 1;
			params.minBufferSize = 1;
			params.maxBufferSize = 5;
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
				}
			}

			if (media.getWidth() > 0) {
				media.setThumb(RAWThumbnailer.getThumbnail(params, file.getFile().getAbsolutePath()));
				if (media.getThumb() != null) {
					media.setSize(media.getThumb().length);
				}

				media.setCodecV("raw");
				media.setContainer("raw");
			}

			media.finalize(type, file);
			media.setMediaparsed(true);
		} catch (Exception e) {
			LOGGER.debug("Caught exception", e);
		}
	}
}
