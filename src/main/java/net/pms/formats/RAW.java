package net.pms.formats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.ImageInfo;
import net.pms.dlna.InputFile;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.RAWThumbnailer;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapperImpl;
import net.pms.util.ImagesUtil;
import net.pms.util.ImagesUtil.ScaleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;

public class RAW extends ImageBase {
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
			// Only parse using DCRaw if it is enabled
			if (PlayerFactory.getEnabledPlayer(RAWThumbnailer.class, this) != null) {
				OutputParams params = new OutputParams(configuration);
				params.waitbeforestart = 0;
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

				media.setCodecV(FormatConfiguration.RAW);
				media.setContainer(FormatConfiguration.RAW);

				ImageInfo imageInfo = null;
				if (file.getFile() != null) {
					Metadata metadata = null;
					FileType fileType = null;
					try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.getFile().toPath()))) {
						try {
							fileType = FileTypeDetector.detectFileType(inputStream);
							metadata = ImagesUtil.getMetadata(inputStream, fileType);
						} catch (IOException e) {
							metadata = new Metadata();
							LOGGER.debug("Error reading \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
							LOGGER.trace("", e);
						} catch (ImageProcessingException e) {
							metadata = new Metadata();
							LOGGER.debug(
								"Error parsing {} metadata for \"{}\": {}",
								fileType.toString().toUpperCase(Locale.ROOT),
								file.getFile().getAbsolutePath(),
								e.getMessage()
							);
							LOGGER.trace("", e);
						}
					}
					try {
						imageInfo = new ImageInfo(
							media.getWidth(),
							media.getHeight(),
							metadata,
							ImageFormat.toImageFormat(fileType),
							file.getSize(),
							true,
							false
						);
					} catch (MetadataException e) {
						LOGGER.warn("Unable to parse metadata for \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
						LOGGER.trace("", e);
					}
				}
				media.setImageInfo(imageInfo);

				if (media.getWidth() > 0 && media.getHeight() > 0 && configuration.getImageThumbnailsEnabled()) {
					byte[] image = RAWThumbnailer.getThumbnail(params, file.getFile().getAbsolutePath(), imageInfo);
					media.setThumb(DLNAThumbnail.toThumbnail(image, 320, 320, ScaleType.MAX, ImageFormat.JPEG, false));
				}
			} else {
				ImagesUtil.parseImage(file.getFile(), media);
			}
			media.setSize(file.getSize());
			media.setImageCount(1);
			media.finalize(type, file);
			media.setMediaparsed(true);

		} catch (IOException e) {
			LOGGER.error(
				"Error parsing RAW file \"{}\": {}",
				file.getFile() != null ? file.getFile().getAbsolutePath() : "Unknown",
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
