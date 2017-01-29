/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.metadata.Metadata;
import net.pms.dlna.DLNAImageProfile.DLNAComplianceResult;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.ParseException;

/**
 * This class is simply a byte array for holding an {@link ImageIO} supported
 * image with some additional metadata restricted to valid DLNA image media
 * format profiles.
 *
 * @see DLNAThumbnail
 *
 * @author Nadahar
 */
public class DLNAImage extends Image {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAImage.class);
	protected final DLNAImageProfile profile;

	/**
	 * Creates a new {@link DLNAImage} instance.
	 *
	 * @param image the source {@link Image} in either GIF, JPEG or PNG format
	 *            adhering to the DLNA restrictions for color space and
	 *            compression.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 */
	public DLNAImage(
		Image image,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException {
		super(image, copy);
		this.profile = profile != null ? profile : findMatchingProfile(this instanceof DLNAThumbnail);
		if (this.profile == null) {
			throw new NullPointerException("DLNAImage: profile cannot be null");
		}
		checkCompliance();
	}

	/**
	 * Creates a new {@link DLNAImage} instance.
	 *
	 * @param bytes the source image in either GIF, JPEG or PNG format adhering
	 *            to the DLNA restrictions for color space and compression.
	 * @param imageInfo the {@link ImageInfo} to store with this
	 *            {@link DLNAImage}.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 */
	public DLNAImage(
		byte[] bytes,
		ImageInfo imageInfo,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException {
		super(bytes, imageInfo, copy);
		this.profile = profile != null ? profile : findMatchingProfile(this instanceof DLNAThumbnail);
		if (this.profile == null) {
			throw new NullPointerException("DLNAImage: profile cannot be null");
		}
		checkCompliance();
	}

	/**
	 * Creates a new {@link DLNAImage} instance.
	 *
	 * @param bytes the source image in either GIF, JPEG or PNG format adhering
	 *            to the DLNA restrictions for color space and compression.
	 * @param width the width of the source image.
	 * @param height the height of the source image.
	 * @param format the {@link ImageFormat} of the source image.
	 * @param colorModel the {@link ColorModel} of the image.
	 * @param metadata the {@link Metadata} instance describing the image.
	 * @param profile the {@link DLNAImageProfile} this {@link DLNAImage}
	 *            adheres to.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	public DLNAImage(
		byte[] bytes,
		int width,
		int height,
		ImageFormat format,
		ColorModel colorModel,
		Metadata metadata,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException, ParseException {
		super(bytes, width, height, format, colorModel, metadata, true, copy);
		this.profile = profile != null ? profile : findMatchingProfile(this instanceof DLNAThumbnail);
		if (this.profile == null) {
			throw new NullPointerException("DLNAImage: profile cannot be null");
		}
		checkCompliance();
	}

	/**
	 * Creates a new {@link DLNAImage} instance.
	 *
	 * @param inputByteArray the source image in either GIF, JPEG or PNG format
	 *            adhering to the DLNA restrictions for color space and
	 *            compression.
	 * @param format the {@link ImageFormat} the source image is in.
	 * @param bufferedImage the {@link BufferedImage} to get non-
	 *            {@link Metadata} metadata from.
	 * @param metadata the {@link Metadata} instance describing the source
	 *            image.
	 * @param copy whether this instance should be copied or shared.
	 * @throws DLNAProfileException if the profile compliance check fails.
	 * @throws ParseException if {@code format} is {@code null} and parsing the
	 *             format from {@code metadata} fails.
	 */
	public DLNAImage(
		byte[] bytes,
		ImageFormat format,
		BufferedImage bufferedImage,
		Metadata metadata,
		DLNAImageProfile profile,
		boolean copy
	) throws DLNAProfileException, ParseException {
		super(bytes, format, bufferedImage, metadata, copy);
		this.profile = profile != null ? profile : findMatchingProfile(this instanceof DLNAThumbnail);
		if (this.profile == null) {
			throw new NullPointerException("DLNAImage: profile cannot be null");
		}
		checkCompliance();
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAImage}. Output format will be
	 * the same as the source if the source is either GIF, JPEG or PNG. Further
	 * restrictions on color space and compression is imposed and conversion
	 * done if necessary. All other formats will be converted to a DLNA
	 * compliant JPEG.
	 *
	 * @param inputImage the source {@link Image}.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(Image inputImage) throws IOException {
		return toDLNAImage(inputImage, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an image to a {@link DLNAImage}. Format support is limited to
	 * that of {@link ImageIO}. Output format will be the same as the source if
	 * the source is either GIF, JPEG or PNG. Further restrictions on color
	 * space and compression is imposed and conversion done if necessary. All
	 * other formats will be converted to a DLNA compliant JPEG. Preserves
	 * aspect ratio and rotates/flips the image according to Exif orientation.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(InputStream inputStream) throws IOException {
		return toDLNAImage(inputStream, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an image to a {@link DLNAImage}. Format support is limited to
	 * that of {@link ImageIO}. Output format will be the same as the source if
	 * the source is either GIF, JPEG or PNG. Further restrictions on color
	 * space and compression is imposed and conversion done if necessary. All
	 * other formats will be converted to a DLNA compliant JPEG. Preserves
	 * aspect ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param sourceByteArray the source image in a supported format.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(byte[] sourceByteArray) throws IOException {
		return toDLNAImage(sourceByteArray, 0, 0, null, ImageFormat.SOURCE, false);
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. Output format will be the same as the source if
	 * the source is either GIF, JPEG or PNG. Further restrictions on color
	 * space and compression is imposed and conversion done if necessary. All
	 * other formats will be converted to a DLNA compliant JPEG.
	 *
	 * @param inputImage the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		Image inputImage,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputImage == null) {
			return null;
		}

		return (DLNAImage) ImagesUtil.transcodeImage(
			inputImage,
			outputProfile,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. Format support is limited to that of
	 * {@link ImageIO}. Output format will be the same as the source if the
	 * source is either GIF, JPEG or PNG. Further restrictions on color space
	 * and compression is imposed and conversion done if necessary. All other
	 * formats will be converted to a DLNA compliant JPEG. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		InputStream inputStream,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputStream == null) {
			return null;
		}

		return (DLNAImage) ImagesUtil.transcodeImage(
			inputStream,
			outputProfile,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAThumbnail} adhering to
	 * {@code outputProfile}. Format support is limited to that of
	 * {@link ImageIO}. Output format will be the same as the source if the
	 * source is either GIF, JPEG or PNG. Further restrictions on color space
	 * and compression is imposed and conversion done if necessary. All other
	 * formats will be converted to a DLNA compliant JPEG. Preserves aspect
	 * ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *            output.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		byte[] inputByteArray,
		DLNAImageProfile outputProfile,
		boolean padToSize
	) throws IOException {
		if (inputByteArray == null) {
			return null;
		}

		return (DLNAImage) ImagesUtil.transcodeImage(
			inputByteArray,
			outputProfile,
			false,
			padToSize
		);
	}

	/**
	 * Converts an {@link Image} to a {@link DLNAImage}. Output format will be
	 * the same as the source if the source is either GIF, JPEG or PNG. Further
	 * restrictions on color space and compression is imposed and conversion
	 * done if necessary. All other formats will be converted to a DLNA
	 * compliant JPEG.
	 *
	 * @param inputImage the source {@link Image}.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		Image inputImage,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		if (inputImage == null) {
			return null;
		}

		return (DLNAImage) ImagesUtil.transcodeImage(
			inputImage,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAImage}. Format support is limited to
	 * that of {@link ImageIO}. Output format will be the same as the source if
	 * the source is either GIF, JPEG or PNG. Further restrictions on color
	 * space and compression is imposed and conversion done if necessary. All
	 * other formats will be converted to a DLNA compliant JPEG. Preserves
	 * aspect ratio and rotates/flips the image according to Exif orientation.
	 *
	 * <p>
	 * <b> This method consumes and closes {@code inputStream}. </b>
	 *
	 * @param inputStream the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		InputStream inputStream,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize
	) throws IOException {
		if (inputStream == null) {
			return null;
		}

		return (DLNAImage) ImagesUtil.transcodeImage(
			inputStream,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			false,
			padToSize
		);
	}

	/**
	 * Converts an image to a {@link DLNAImage}. Format support is limited to
	 * that of {@link ImageIO}. Output format will be the same as the source if
	 * the source is either GIF, JPEG or PNG. Further restrictions on color
	 * space and compression is imposed and conversion done if necessary. All
	 * other formats will be converted to a DLNA compliant JPEG. Preserves
	 * aspect ratio and rotates/flips the image according to Exif orientation.
	 *
	 * @param inputByteArray the source image in a supported format.
	 * @param width the new width or 0 to disable scaling.
	 * @param height the new height or 0 to disable scaling.
	 * @param scaleType the {@link ScaleType} to use when scaling.
	 * @param outputFormat the {@link ImageFormat} to generate or
	 *            {@link ImageFormat#SOURCE} to preserve source format.
	 * @param padToSize whether padding should be used if source aspect doesn't
	 *            match target aspect.
	 * @return The populated {@link DLNAImage} or {@code null} if the source
	 *         image is {@code null}.
	 * @throws IOException if the operation fails.
	 */
	public static DLNAImage toDLNAImage(
		byte[] inputByteArray,
		int width,
		int height,
		ScaleType scaleType,
		ImageFormat outputFormat,
		boolean padToSize) throws IOException {
		return (DLNAImage) ImagesUtil.transcodeImage(
			inputByteArray,
			width,
			height,
			scaleType,
			outputFormat,
			true,
			false,
			padToSize
		);
	}

	/**
	 * Converts and scales the image according to the given
	 * {@link DLNAImageProfile}. Preserves aspect ratio. Format support is
	 * limited to that of {@link ImageIO}.
	 *
	 * @param outputProfile the {@link DLNAImageProfile} to adhere to for the
	 *                      output.
	 * @param dlnaThumbnail whether or not the output image should be
	 *                      restricted to DLNA thumbnail compliance. This also
	 *                      means that the output can be safely cast to
	 *                      {@link DLNAThumbnail}.
	 * @param padToSize Whether padding should be used if source aspect doesn't
	 *                  match target aspect.
	 * @return The scaled and/or converted thumbnail, {@code null} if the
	 *         source is {@code null}.
	 * @exception IOException if the operation fails.
	 */
	public DLNAImage transcode(
		DLNAImageProfile outputProfile,
		boolean dlnaThumbnail,
		boolean padToSize
	) throws IOException {
		return (DLNAImage) ImagesUtil.transcodeImage(
			this,
			outputProfile,
			dlnaThumbnail,
			padToSize
		);
	}

	/**
	 * @return The {@link DLNAImageProfile} this {@link DLNAImage} adheres to.
	 */
	public DLNAImageProfile getDLNAImageProfile() {
		return profile;
	}

	@Override
	public DLNAImage copy() {
		try {
			return new DLNAImage(bytes, imageInfo, profile, true);
		} catch (DLNAProfileException e) {
			// Should be impossible
			LOGGER.error("Impossible situation in DLNAImage.copy(): {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}
	}

	protected DLNAImageProfile findMatchingProfile(boolean dlnaThumbnail) {
		if (
			imageInfo == null || imageInfo.getFormat() == null ||
			imageInfo.getWidth() < 1 || imageInfo.getHeight() < 1
		) {
			return null;
		}
		DLNAComplianceResult result;
		switch (imageInfo.getFormat()) {
			case GIF:
				if (dlnaThumbnail) {
					return null;
				}
				if (DLNAImageProfile.GIF_LRG.checkCompliance(imageInfo).isAllCorrect()) {
					return DLNAImageProfile.GIF_LRG;
				}
				return null;
			case JPEG:
				result = DLNAImageProfile.JPEG_TN.checkCompliance(imageInfo);
				if (result.isAllCorrect()) {
					return DLNAImageProfile.JPEG_TN;
				} else if (result.isColorsCorrect() && result.isFormatCorrect()) {
					if (DLNAImageProfile.JPEG_SM.isResolutionCorrect(imageInfo)) {
						return DLNAImageProfile.JPEG_SM;
					}
					if (DLNAImageProfile.JPEG_MED.isResolutionCorrect(imageInfo)) {
						return DLNAImageProfile.JPEG_MED;
					}
					if (DLNAImageProfile.JPEG_LRG.isResolutionCorrect(imageInfo)) {
						return DLNAImageProfile.JPEG_LRG;
					}
					return DLNAImageProfile.createJPEG_RES_H_V(imageInfo.getWidth(), imageInfo.getHeight());
				}
				return null;
			case PNG:
				result = DLNAImageProfile.PNG_TN.checkCompliance(imageInfo);
				if (result.isAllCorrect()) {
					return DLNAImageProfile.PNG_TN;
				} else if (
					result.isColorsCorrect() &&
					result.isFormatCorrect() &&
					DLNAImageProfile.PNG_LRG.isResolutionCorrect(imageInfo)
				) {
					return DLNAImageProfile.PNG_LRG;
				}
				return null;
			default:

		}
		return null;
	}

	protected void checkCompliance() throws DLNAProfileException {
		DLNAComplianceResult result = profile.checkCompliance(imageInfo);
		if (result.isAllCorrect()) {
			return;
		}
		StringBuilder sb = new StringBuilder(150);
		sb.append(this.getClass().getSimpleName()).
			append(": Compliance check failed for ").append(profile);

		List<String> failures = result.getFailures();
		if (!failures.isEmpty()) {
			if (LOGGER.isDebugEnabled()) {
				sb.append(" with the following failures:\n").
				append(StringUtils.join(failures, "\n"));
			} else {
				sb.append(" for: ").
				append(StringUtils.join(failures, ", "));
			}
		}
		throw new DLNAProfileException(sb.toString());
	}

	@Override
	protected void buildToString(StringBuilder sb) {
		if (profile != null) {
			sb.append(", DLNA Profile = ").append(profile);
		}
	}
}
