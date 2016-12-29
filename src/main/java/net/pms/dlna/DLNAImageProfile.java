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

import java.awt.Dimension;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.imaging.png.PngColorType;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jfif.JfifDirectory;
import com.drew.metadata.png.PngDirectory;
import net.pms.formats.ImageFormat;


/**
 * Definition and validation of the different DLNA media profiles for images.
 * If more are added, corresponding changes need to be made in
 * {@link ImageFormat#fromImageProfile}.
 *
 * Please note: Only the profile constant (e.g {@code JPEG_TN} is taken into
 * consideration in {@link #equals(Object)} and {@link #hashCode()}, metadata
 * like {@code H} and {@code V} aren't compared. This means that:
 * <p>
 * {@code DLNAImageProfile.JPEG_RES_H_V.equals(DLNAImageProfile.JPEG_RES_H_V)}
 * <p>
 * is true even if {@code H} and {@code V} are different in the two profiles.
 */
public class DLNAImageProfile implements Serializable{

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAImageProfile.class);
	public static final int GIF_LRG_INT = 1;
	public static final int JPEG_LRG_INT = 2;
	public static final int JPEG_MED_INT = 3;
	public static final int JPEG_RES_H_V_INT = 4;
	public static final int JPEG_SM_INT = 5;
	public static final int JPEG_TN_INT = 6;
	public static final int PNG_LRG_INT = 7;
	public static final int PNG_TN_INT = 8;

	public static final Integer GIF_LRG_INTEGER = GIF_LRG_INT;
	public static final Integer JPEG_LRG_INTEGER = JPEG_LRG_INT;
	public static final Integer JPEG_MED_INTEGER = JPEG_MED_INT;
	public static final Integer JPEG_RES_H_V_INTEGER = JPEG_RES_H_V_INT;
	public static final Integer JPEG_SM_INTEGER = JPEG_SM_INT;
	public static final Integer JPEG_TN_INTEGER = JPEG_TN_INT;
	public static final Integer PNG_LRG_INTEGER = PNG_LRG_INT;
	public static final Integer PNG_TN_INTEGER = PNG_TN_INT;

	/**
	 * {@code GIF_LRG} maximum resolution 1600 x 1200.
	 */
	public static final DLNAImageProfile GIF_LRG = new DLNAImageProfile(GIF_LRG_INT, "GIF_LRG", 1600, 1200);

	/**
	 * {@code JPEG_LRG} maximum resolution 4096 x 4096.
	 */
	public static final DLNAImageProfile JPEG_LRG = new DLNAImageProfile(JPEG_LRG_INT, "JPEG_LRG", 4096, 4096);

	/**
	 * {@code JPEG_MED} maximum resolution 1024 x 768.
	 */
	public static final DLNAImageProfile JPEG_MED = new DLNAImageProfile(JPEG_MED_INT, "JPEG_MED", 1024, 768);

	/**
	 * {@code JPEG_RES_H_V} exact resolution H x V.<br>
	 * <br>
	 * <b>This constant creates an invalid profile, only for use with
	 * {@link #equals(Object)}.</b><br>
	 * <br>
	 * To create a valid profile, use {@link #createJPEG_RES_H_V(int, int)} instead.
	 */
	public static final DLNAImageProfile JPEG_RES_H_V = new DLNAImageProfile(JPEG_RES_H_V_INT, "JPEG_RES_H_V", -1, -1);

	/**
	 * {@code JPEG_SM} maximum resolution 640 x 480.
	 */
	public static final DLNAImageProfile JPEG_SM = new DLNAImageProfile(JPEG_SM_INT, "JPEG_SM", 640, 480);

	/**
	 * {@code JPEG_TN} maximum resolution 160 x 160.
	 */
	public static final DLNAImageProfile JPEG_TN = new DLNAImageProfile(JPEG_TN_INT, "JPEG_TN", 160, 160);

	/**
	 * {@code PNG_LRG} maximum resolution 4096 x 4096.
	 */
	public static final DLNAImageProfile PNG_LRG = new DLNAImageProfile(PNG_LRG_INT, "PNG_LRG", 4096, 4096);

	/**
	 * {@code PNG_TN} maximum resolution 160 x 160.
	 */
	public static final DLNAImageProfile PNG_TN = new DLNAImageProfile(PNG_TN_INT, "PNG_TN", 160, 160);

	/**
	 * Creates a new {@link #JPEG_RES_H_V} profile instance with the exact
	 * resolution H x V. Set {@code H} and {@code V} for this instance. Not
	 * applicable for other profiles.
	 *
	 * @param horizontal the {@code H} value.
	 * @param vertical the {@code V} value.
	 * @return The new {@link #JPEG_RES_H_V} instance.
	 */
	public static DLNAImageProfile createJPEG_RES_H_V(int horizontal, int vertical) {
		return new DLNAImageProfile(
			JPEG_RES_H_V_INT,
			"JPEG_RES_" + Integer.toString(horizontal) + "_" + Integer.toString(vertical),
			horizontal,
			vertical
		);
	}

	public final int imageProfileInt;
	public final String imageProfileStr;
	private final int horizontal;
	private final int vertical;

	/**
	 * Instantiate a {@link DLNAImageProfile} object.
	 */
	private DLNAImageProfile(int imageProfileInt, String imageProfileStr, int horizontal, int vertical) {
		this.imageProfileInt = imageProfileInt;
		this.imageProfileStr = imageProfileStr;
		this.horizontal = horizontal;
		this.vertical = vertical;
	}

	/**
	 * Returns the string representation of this {@link DLNAImageProfile}.
	 */
	@Override
	public String toString() {
		return imageProfileStr;
	}

	/**
	 * Returns the integer representation of this {@link DLNAImageProfile}.
	 */
	public int toInt() {
		return imageProfileInt;
	}

	/**
	 * Converts a {@link DLNAImageProfile} to an {@link Integer} object.
	 *
	 * @return This {@link DLNAImageProfile}'s {@link Integer} mapping.
	 */
	public Integer toInteger() {
		switch (imageProfileInt) {
			case GIF_LRG_INT:
				return GIF_LRG_INTEGER;
			case JPEG_LRG_INT:
				return JPEG_LRG_INTEGER;
			case JPEG_MED_INT:
				return JPEG_MED_INTEGER;
			case JPEG_RES_H_V_INT:
				return JPEG_RES_H_V_INTEGER;
			case JPEG_SM_INT:
				return JPEG_SM_INTEGER;
			case JPEG_TN_INT:
				return JPEG_TN_INTEGER;
			case PNG_LRG_INT:
				return PNG_LRG_INTEGER;
			case PNG_TN_INT:
				return PNG_TN_INTEGER;
			default:
				throw new IllegalStateException("DLNAImageProfile " + imageProfileStr + ", " + imageProfileInt + " is unknown.");
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link DLNAImageProfile}. If the conversion fails, this method returns
	 * {@code null}.
	 */
	public static DLNAImageProfile toDLNAImageProfile(String argument) {
		return toDLNAImageProfile(argument, null);
	}

	/**
	 * Converts the integer passed as argument to a {@link DLNAImageProfile}.
	 * If the conversion fails, this method returns {@code null}.
	 */
	public static DLNAImageProfile toDLNAImageProfile(int value) {
		return toDLNAImageProfile(value, null);
	}

	/**
	 * Converts the integer passed as argument to a {@link DLNAImageProfile}. If the
	 * conversion fails, this method returns the specified default.
	 */
	public static DLNAImageProfile toDLNAImageProfile(int value, DLNAImageProfile defaultImageProfile) {
		switch (value) {
			case GIF_LRG_INT:
				return GIF_LRG;
			case JPEG_LRG_INT:
				return JPEG_LRG;
			case JPEG_MED_INT:
				return JPEG_MED;
			case JPEG_RES_H_V_INT:
				return JPEG_RES_H_V;
			case JPEG_SM_INT:
				return JPEG_SM;
			case JPEG_TN_INT:
				return JPEG_TN;
			case PNG_LRG_INT:
				return PNG_LRG;
			case PNG_TN_INT:
				return PNG_TN;
			default:
				return defaultImageProfile;
		}
	}

	/**
	 * Converts the {@link String} passed as argument to a
	 * {@link DLNAImageProfile}. If the conversion fails, this method returns
	 * the specified default.
	 */
	public static DLNAImageProfile toDLNAImageProfile(String argument, DLNAImageProfile defaultImageProfile) {
		if (argument == null) {
			return defaultImageProfile;
		}

		argument = argument.toUpperCase(Locale.ROOT);
		switch (argument) {
			case "GIF_LRG":
				return DLNAImageProfile.GIF_LRG;
			case "JPEG_LRG":
				return DLNAImageProfile.JPEG_LRG;
			case "JPEG_MED":
				return DLNAImageProfile.JPEG_MED;
			case "JPEG_RES_H_V":
				return DLNAImageProfile.JPEG_RES_H_V;
			case "JPEG_SM":
				return DLNAImageProfile.JPEG_SM;
			case "JPEG_TN":
				return DLNAImageProfile.JPEG_TN;
			case "PNG_LRG":
				return DLNAImageProfile.PNG_LRG;
			case "PNG_TN":
				return DLNAImageProfile.PNG_TN;
			default:
				if (argument.startsWith("JPEG_RES")) {
					Matcher matcher = Pattern.compile("^JPEG_RES_?(\\d+)[X_](\\d+)").matcher(argument);
					if (matcher.find()) {
						return new DLNAImageProfile(
							JPEG_RES_H_V_INT,
							"JPEG_RES_" + matcher.group(1) + "_" + matcher.group(2),
							Integer.parseInt(matcher.group(1)),
							Integer.parseInt(matcher.group(2))
						);
					}
				}
				return defaultImageProfile;
		}
	}

	/**
	 * @return The mime type for the given {@link DLNAImageProfile}.
	 */
	public String getMimeType() {
		switch (imageProfileInt) {
			case GIF_LRG_INT:
				return "image/gif";
			case JPEG_LRG_INT:
			case JPEG_MED_INT:
			case JPEG_RES_H_V_INT:
			case JPEG_SM_INT:
			case JPEG_TN_INT:
				return "image/jpeg";
			case PNG_LRG_INT:
			case PNG_TN_INT:
				return "image/png";
		}
		throw new IllegalStateException("Unknown image profile: " + this);
	}

	/**
	 * Gets the maximum image width for the given {@link DLNAImageProfile}.
	 *
	 * @return The value in pixels.
	 */
	public int getMaxWidth() {
		return horizontal;
	}

	/**
	 * Get {@code H} for the {@code JPEG_RES_H_V} profile.
	 *
	 * @return The {@code H} value in pixels.
	 */
	public int getH() {
		return horizontal;
	}

	/**
	 * Gets the maximum image height for the given {@link DLNAImageProfile}.
	 *
	 * @return The value in pixels.
	 */
	public int getMaxHeight() {
		return vertical;
	}

	/**
	 * Get {@code V} for the {@code JPEG_RES_H_V} profile.
	 *
	 * @return The {@code V} value in pixels.
	 */
	public int getV() {
		return vertical;
	}

	/**
	 * Get the {@link Dimension} of the maximum image resolution for the given
	 * {@link DLNAImageProfile}.
	 *
	 * @return The {@link Dimension}.
	 */
	public Dimension getMaxResolution() {
		if (horizontal >= 0 && vertical >= 0) {
			return new Dimension(horizontal, vertical);
		}
		return new Dimension();
	}

	/**
	 * @return The version number multiplied with 100 (last two digits are decimals).
	 */
	protected static int parseExifVersion(byte[] bytes) {
		if (bytes == null) {
			return -1;
		}
		StringBuilder stringBuilder = new StringBuilder(4);
		for (byte b : bytes) {
			stringBuilder.append((char) b);
		}
		while (stringBuilder.length() < 4) {
			stringBuilder.append("0");
		}
		return Integer.parseInt(stringBuilder.toString());
	}

	/**
	 * Performs GIF compliance checks. The result is stored in
	 * {@code complianceResult}.
	 */
	protected static void checkGIF(ImageInfo imageInfo, InternalComplianceResult complianceResult) throws MetadataException {
		if (imageInfo == null || imageInfo.getFormat() != ImageFormat.GIF || imageInfo.getMetadata() == null) {
			return;
		}
		complianceResult.formatCorrect = true;
		GifHeaderDirectory directory = imageInfo.getMetadata() != null ?
			imageInfo.getMetadata().getFirstDirectoryOfType(GifHeaderDirectory.class) : null
		;
		if (directory != null) {
			if (
				directory.containsTag(GifHeaderDirectory.TAG_BITS_PER_PIXEL) &&
				directory.getInt(GifHeaderDirectory.TAG_BITS_PER_PIXEL) <= 8
			) {
				complianceResult.colorsCorrect = true;
			}
		}
	}

	/**
	 * Performs JPEG compliance checks. The result is stored in
	 * {@code complianceResult}.
	 */
	protected static void checkJPEG(ImageInfo imageInfo, InternalComplianceResult complianceResult) throws MetadataException {
		if (imageInfo == null || imageInfo.getFormat() != ImageFormat.JPEG || imageInfo.getMetadata() == null) {
			return;
		}
		complianceResult.colorsCorrect = true;
		JfifDirectory jfifDirectory = imageInfo.getMetadata() != null ?
			imageInfo.getMetadata().getFirstDirectoryOfType(JfifDirectory.class) : null;
		ExifSubIFDDirectory exifSubIFDirectory = imageInfo.getMetadata() != null ?
			imageInfo.getMetadata().getFirstDirectoryOfType(ExifSubIFDDirectory.class) : null;
		if (jfifDirectory != null) {
			complianceResult.formatCorrect = jfifDirectory.containsTag(JfifDirectory.TAG_VERSION) && jfifDirectory.getVersion() >= 0x102;
		}
		if (!complianceResult.formatCorrect && exifSubIFDirectory != null) {
			complianceResult.formatCorrect =
				exifSubIFDirectory.containsTag(ExifDirectoryBase.TAG_EXIF_VERSION) &&
				parseExifVersion(exifSubIFDirectory.getByteArray(ExifDirectoryBase.TAG_EXIF_VERSION)) >= 100;
		}
	}

	/**
	 * Performs PNG compliance checks. The result is stored in
	 * {@code complianceResult}.
	 */
	protected static void checkPNG(ImageInfo imageInfo, InternalComplianceResult complianceResult) throws MetadataException {
		if (imageInfo == null || imageInfo.getFormat() != ImageFormat.PNG || imageInfo.getMetadata() == null) {
			return;
		}
		Collection<PngDirectory> pngDirectories = imageInfo.getMetadata().getDirectoriesOfType(PngDirectory.class);
		for (PngDirectory pngDirectory : pngDirectories) {
			if (pngDirectory.getPngChunkType().getIdentifier().equals("IHDR")) {
				if (pngDirectory.containsTag(PngDirectory.TAG_COLOR_TYPE)) {
					switch (PngColorType.fromNumericValue(pngDirectory.getInt(PngDirectory.TAG_COLOR_TYPE))) {
						case Greyscale:
						case GreyscaleWithAlpha:
						case IndexedColor:
						case TrueColor:
						case TrueColorWithAlpha:
							complianceResult.colorsCorrect = true;
							break;
						default:
					}
				}
				complianceResult.formatCorrect =
					pngDirectory.containsTag(PngDirectory.TAG_INTERLACE_METHOD) &&
					pngDirectory.getInt(PngDirectory.TAG_INTERLACE_METHOD) == 0;
			}
		}
	}

	/**
	 * Validates DLNA compliance for {@code imageInfo} against the largest
	 * {@link DLNAImageProfile} for the given {@link ImageFormat}. Validation
	 * is performed on resolution, format, format version, format compression
	 * and color coding. The validation result is returned in a
	 * {@link DLNAComplianceResult} data structure.
	 *
	 * @param imageInfo the {@link ImageInfo} for the image to check for DLNA
	 *                  compliance.
	 * @param format the {@link ImageFormat} to check for DLNA compliance
	 *               against.
	 * @return The validation result.
	 */
	public static DLNAComplianceResult checkCompliance(ImageInfo imageInfo, ImageFormat format) {
		InternalComplianceResult complianceResult = new InternalComplianceResult();
		if (imageInfo == null || format == null) {
			return DLNAComplianceResult.toDLNAComplianceResult(complianceResult);
		}
		try {
			DLNAImageProfile largestProfile;
			switch (format) {
				case JPEG:
					// Since JPEG_RES_H_V has no upper limit, any resolution is ok
					complianceResult.maxWidth = Integer.MAX_VALUE;
					complianceResult.maxHeight = Integer.MAX_VALUE;
					complianceResult.resolutionCorrect = true;
					checkJPEG(imageInfo, complianceResult);
					break;
				case GIF:
					largestProfile = DLNAImageProfile.GIF_LRG;
					complianceResult.maxWidth = largestProfile.getMaxWidth();
					complianceResult.maxHeight = largestProfile.getMaxHeight();
					complianceResult.resolutionCorrect =
						largestProfile.isResolutionCorrect(imageInfo);
					checkGIF(imageInfo, complianceResult);
					break;
				case PNG:
					largestProfile = DLNAImageProfile.PNG_LRG;
					complianceResult.maxWidth = largestProfile.getMaxWidth();
					complianceResult.maxHeight = largestProfile.getMaxHeight();
					complianceResult.resolutionCorrect =
						largestProfile.isResolutionCorrect(imageInfo);
					checkPNG(imageInfo, complianceResult);
					break;
				default:
					// No other formats are compliant
			}
		} catch (MetadataException e) {
			complianceResult.formatCorrect = false;
			complianceResult.colorsCorrect = false;
			LOGGER.debug("MetadataException in checkCompliance: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return DLNAComplianceResult.toDLNAComplianceResult(complianceResult);
	}

	/**
	 * Validates DLNA compliance for {@code imageInfo} against a specific
	 * {@link DLNAImageProfile}. Validation is performed on resolution, format,
	 * format version, format compression and color coding. The validation
	 * result is returned in a {@link DLNAComplianceResult} data structure.
	 *
	 * @param imageInfo the {@link ImageInfo} for the image to check for DLNA
	 *                  compliance.
	 * @param profile the {@link DLNAImageProfile} to check for DLNA compliance
	 *               against.
	 * @return The validation result.
	 */
	public static DLNAComplianceResult checkCompliance(ImageInfo imageInfo, DLNAImageProfile profile) {
		if (imageInfo == null || profile == null) {
			return DLNAComplianceResult.toDLNAComplianceResult(new InternalComplianceResult());
		}
		return profile.checkCompliance(imageInfo);
	}

	/**
	 * Determines whether the resolution specified in {@code imageInfo} is
	 * valid for this {@link DLNAImageProfile}.
	 * @param imageInfo the {@link ImageInfo} to evaluate.
	 * @return {@code true} if the resolution is valid, {@code false} otherwise.
	 */
	public boolean isResolutionCorrect(ImageInfo imageInfo) {
		if (imageInfo == null || imageInfo.getWidth() < 1 || imageInfo.getHeight() < 1) {
			return false;
		}
		if (equals(DLNAImageProfile.JPEG_RES_H_V)) {
			return
				imageInfo.getWidth() == horizontal &&
				imageInfo.getHeight() == vertical;
		} else {
			return
				imageInfo.getWidth() <= horizontal &&
				imageInfo.getHeight() <= vertical;
		}
	}

	/**
	 * Validates DLNA compliance for {@code image} against this
	 * {@link DLNAImageProfile}. Validation is performed on resolution, format,
	 * format version, format compression and color coding. The validation
	 * result is returned in a {@link DLNAComplianceResult} data structure.
	 *
	 * @param imageInfo the {@link ImageInfo} for the image to check for DLNA
	 *                  compliance.
	 * @return The validation result.
	 */
	public DLNAComplianceResult checkCompliance(ImageInfo imageInfo) {
		InternalComplianceResult complianceResult = new InternalComplianceResult();
		complianceResult.maxWidth = horizontal;
		complianceResult.maxHeight = vertical;
		if (imageInfo == null) {
			return DLNAComplianceResult.toDLNAComplianceResult(complianceResult);
		}
		complianceResult.resolutionCorrect = isResolutionCorrect(imageInfo);

		try {
			switch (this.toInt()) {
				case DLNAImageProfile.GIF_LRG_INT:
					checkGIF(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.JPEG_LRG_INT:
					checkJPEG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.JPEG_MED_INT:
					checkJPEG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.JPEG_RES_H_V_INT:
					checkJPEG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.JPEG_SM_INT:
					checkJPEG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.JPEG_TN_INT:
					checkJPEG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.PNG_LRG_INT:
					checkPNG(imageInfo, complianceResult);
					break;
				case DLNAImageProfile.PNG_TN_INT:
					checkPNG(imageInfo, complianceResult);
					break;
				default:
					throw new IllegalStateException("Illegal DLNA media profile");
			}
		} catch (MetadataException e) {
			complianceResult.formatCorrect = false;
			complianceResult.colorsCorrect = false;
			LOGGER.debug("MetadataException in checkCompliance: {}", e.getMessage());
			LOGGER.trace("", e);
		}

		return DLNAComplianceResult.toDLNAComplianceResult(complianceResult);
	}

	/**
	 * Tries to calculate the would-be resolution and size of {@code imageInfo}
	 * if that image were to be converted into this profile. Size cannot be
	 * calculated if any actual conversion is needed (finding that would
	 * require an actual encoding to be done), and will return {@code null}
	 * unless the image can be used as-is.
	 *
	 * @param imageInfo the {@link ImageInfo} for the image in question.
	 *
	 * @return The result in a {@link HypotheticalResult} data structure.
	 */
	public HypotheticalResult calculateHypotheticalProperties(ImageInfo imageInfo) {
		if (imageInfo == null) {
			throw new IllegalArgumentException("calculateHypotheticalProperties: imageInfo cannot be null");
		}
		DLNAComplianceResult complianceResult = checkCompliance(imageInfo);
		if (complianceResult.isAllCorrect()) {
			return new HypotheticalResult(
				imageInfo.getWidth(),
				imageInfo.getHeight(),
				imageInfo.getSize(),
				false
			);
		}
		if (complianceResult.resolutionCorrect) {
			return new HypotheticalResult(imageInfo.getWidth(), imageInfo.getHeight(), null, true);
		}
		double widthScale = imageInfo.width < 1 ? 0 : (double) horizontal / imageInfo.getWidth();
		double heightScale = imageInfo.height < 1 ? 0 : (double) vertical / imageInfo.getHeight();
		double scale = Math.min(Math.min(widthScale, heightScale), 1);
		return new HypotheticalResult(
			(int) Math.round(imageInfo.width * scale),
			(int) Math.round(imageInfo.height * scale),
			null,
			true
		);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + imageProfileInt;
		return result;
	}

	/**
	 * Only the profile constant is considered when comparing, metadata like
	 * {@code H} and {@code V} aren't taken into account.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DLNAImageProfile)) {
			return false;
		}
		DLNAImageProfile other = (DLNAImageProfile) obj;
		if (imageProfileInt != other.imageProfileInt) {
			return false;
		}
		return true;
	}

	/**
	 * Immutable {@link DLNAImageProfile#calculateHypotheticalProperties(ImageInfo)}
	 * result data structure. Please note that {@code size} might be {@code null}.
	 */
	public static class HypotheticalResult {
		/**
		 * The calculated width or {@code 0}.
		 */
		public final int width;
		/**
		 * The calculated height or {@code 0}.
		 */
		public final int height;
		/**
		 * The calculated size or {@code null}.
		 */
		public final Long size;
		/**
		 * Whether conversion is needed.
		 */
		public final boolean conversionNeeded;

		public HypotheticalResult(int width, int height, Long size, boolean conversionNeeded) {
			this.width = width;
			this.height = height;
			this.size = size;
			this.conversionNeeded = conversionNeeded;
		}
	}

	/**
	 * Internal mutable compliance result data structure.
	 */
	protected static class InternalComplianceResult {
		public boolean resolutionCorrect = false;
		public boolean formatCorrect = false;
		public boolean colorsCorrect = false;
		public int maxWidth;
		public int maxHeight;
	}

	/**
	 * Immutable compliance result data structure.
	 */
	public static class DLNAComplianceResult {
		private final boolean resolutionCorrect;
		private final boolean formatCorrect;
		private final boolean colorsCorrect;
		private final boolean allCorrect;
		private final int maxWidth;
		private final int maxHeight;

		public DLNAComplianceResult(boolean resolutionCorrect, boolean formatCorrect, boolean colorsCorrect, int maxWidth, int maxHeight) {
			this.resolutionCorrect = resolutionCorrect;
			this.formatCorrect = formatCorrect;
			this.colorsCorrect = colorsCorrect;
			this.allCorrect = resolutionCorrect && formatCorrect && colorsCorrect;
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
		}

		protected static DLNAComplianceResult toDLNAComplianceResult(InternalComplianceResult result) {
			return new DLNAComplianceResult(result.resolutionCorrect, result.formatCorrect, result.colorsCorrect, result.maxWidth, result.maxHeight);
		}

		/**
		 * @return Whether the resolution is within DLNA limits for the given
		 *         profile.
		 */
		public boolean isResolutionCorrect() {
			return resolutionCorrect;
		}

		/**
		 * @return Whether the format/compression is compliant for the given
		 *         profile.
		 */
		public boolean isFormatCorrect() {
			return formatCorrect;
		}

		/**
		 * @return Whether the color coding is compliant for the given profile.
		 */
		public boolean isColorsCorrect() {
			return colorsCorrect;
		}

		/**
		 * @return Whether all tests show compliance.
		 */
		public boolean isAllCorrect() {
			return allCorrect;
		}

		/**
		 * @return The maximum width.
		 */
		public int getMaxWidth() {
			return maxWidth;
		}

		/**
		 * @return the maximum height.
		 */
		public int getMaxHeight() {
			return maxHeight;
		}

	}
}
