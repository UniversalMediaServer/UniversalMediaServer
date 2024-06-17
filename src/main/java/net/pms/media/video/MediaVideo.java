/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.media.video;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.pms.configuration.FormatConfiguration;
import net.pms.media.MediaLang;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the video properties of media.
 */
public class MediaVideo extends MediaLang implements Cloneable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaVideo.class);

	private Integer streamOrder;
	private Long optionalId;
	private boolean defaultFlag;
	private boolean forcedFlag;
	private int width;
	private int height;
	private Double durationSec;
	private int bitRate;
	private Double frameRate;
	private String frameRateModeOriginal;
	private String frameRateMode;
	private String codec;
	private String formatProfile = null;
	private String formatLevel = null;
	private String formatTier = null;

	/**
	 * The frame rate mode as read from the parser
	 */
	private String frameRateModeRaw;
	private final ReentrantReadWriteLock referenceFrameCountLock = new ReentrantReadWriteLock();
	private byte referenceFrameCount = -1;
	private int bitDepth = 8;
	private String matrixCoefficients;
	private Double pixelAspectRatio;
	private String multiViewLayout;
	private ScanType scanType;
	private ScanOrder scanOrder;
	private String displayAspectRatio;
	private String originalDisplayAspectRatio;
	private String title;
	private String muxingMode;
	private Map<String, String> extras;
	private boolean encrypted;
	private String videoHDRFormat;
	private String videoHDRFormatCompatibility;

	/**
	 * @return the container stream index.
	 */
	public Integer getStreamOrder() {
		return streamOrder;
	}

	/**
	 * @param streamIndex the container stream index to set.
	 */
	public void setStreamOrder(Integer streamIndex) {
		this.streamOrder = streamIndex;
	}

	/**
	 * Returns the optional id for this video stream.
	 *
	 * @return The optional id.
	 */
	public Long getOptionalId() {
		return optionalId;
	}

	/**
	 * Sets an optional id for this video stream.
	 *
	 * @param uid the optional id to set.
	 */
	public void setOptionalId(Long optionalId) {
		this.optionalId = optionalId;
	}

	/**
	 * Whether the stream was flagged to default video stream.
	 *
	 * @return {boolean}
	 */
	public boolean isDefault() {
		return defaultFlag;
	}

	/**
	 * @param defaultFlag the default flag to set
	 */
	public void setDefault(boolean defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * Whether the stream was flagged to forced video stream.
	 *
	 * @return {boolean}
	 */
	public boolean isForced() {
		return forcedFlag;
	}

	/**
	 * @param forcedFlag the forced flag to set
	 */
	public void setForced(boolean forcedFlag) {
		this.forcedFlag = forcedFlag;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	public String getResolution() {
		if (width > 0 && height > 0) {
			return width + "x" + height;
		}
		return null;
	}

	public void setDuration(Double d) {
		this.durationSec = d;
	}

	/**
	 * This is the object {@link Double} and might return <code>null</code>.
	 *
	 * To get <code>0</code> instead of <code>null</code>, use
	 * {@link #getDurationInSeconds()}
	 *
	 * @return duration
	 */
	public Double getDuration() {
		return durationSec;
	}

	/**
	 * @return 0 if nothing is specified, otherwise the duration
	 */
	public double getDurationInSeconds() {
		return Optional.ofNullable(durationSec).orElse(0D);
	}

	public String getDurationString() {
		return durationSec != null ? StringUtil.formatDLNADuration(durationSec) : null;
	}

	/**
	 * @return the video codec
	 */
	public String getCodec() {
		return codec;
	}

	/**
	 * @param codec the video codec to set
	 */
	public void setCodec(String codec) {
		this.codec = codec != null ? codec.toLowerCase(Locale.ROOT) : null;
	}

	public String getMatrixCoefficients() {
		return matrixCoefficients;
	}

	public void setMatrixCoefficients(String matrixCoefficients) {
		this.matrixCoefficients = matrixCoefficients;
	}

	/**
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @return the type of multiViewLayout (3D) of the video track
	 */
	public String getMultiViewLayout() {
		return multiViewLayout;
	}

	/**
	 * Sets the type of stereoscopy (3D) of the video track.
	 *
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @param stereoscopy the type of multiViewLayout (3D) of the video track
	 */
	public void setMultiViewLayout(String stereoscopy) {
		this.multiViewLayout = stereoscopy;
	}

	/**
	 * @return The pixel aspect ratio.
	 */
	public Double getPixelAspectRatio() {
		return pixelAspectRatio;
	}

	/**
	 * Sets the pixel aspect ratio.
	 *
	 * @param pixelAspectRatio the pixel aspect ratio to set.
	 */
	public void setPixelAspectRatio(Double pixelAspectRatio) {
		this.pixelAspectRatio = pixelAspectRatio;
	}

	/**
	 * @return the video bit depth
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * @param value the video bit depth to set
	 */
	public void setBitDepth(int value) {
		this.bitDepth = value;
	}

	/**
	 * @return the {@link ScanType}.
	 */
	@Nullable
	public ScanType getScanType() {
		return scanType;
	}

	/**
	 * Sets the {@link ScanType}.
	 *
	 * @param scanType the {@link ScanType} to set.
	 */
	public void setScanType(@Nullable ScanType scanType) {
		this.scanType = scanType;
	}

	/**
	 * Sets the {@link ScanType} by parsing the specified {@link String}.
	 *
	 * @param scanType the {@link String} to parse.
	 */
	public void setScanType(@Nullable String scanType) {
		this.scanType = ScanType.typeOf(scanType);
	}

	/**
	 * @return the {@link ScanOrder}.
	 */
	@Nullable
	public ScanOrder getScanOrder() {
		return scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder}.
	 *
	 * @param scanOrder the {@link ScanOrder} to set.
	 */
	public void setScanOrder(@Nullable ScanOrder scanOrder) {
		this.scanOrder = scanOrder;
	}

	/**
	 * Sets the {@link ScanOrder} by parsing the specified {@link String}.
	 *
	 * @param scanOrder the {@link String} to parse.
	 */
	public void setScanOrder(@Nullable String scanOrder) {
		this.scanOrder = ScanOrder.typeOf(scanOrder);
	}

	/**
	 * Get the aspect ratio reported by the file/container.
	 * This is the aspect ratio that the renderer should display the video
	 * at, and is usually the same as the codec aspect ratio.
	 *
	 * @return the aspect ratio reported by the file/container
	 */
	public String getDisplayAspectRatio() {
		return displayAspectRatio;
	}

	/**
	 * Sets the aspect ratio reported by the file/container.
	 *
	 * @param aspectRatio the aspect ratio to set.
	 */
	public void setDisplayAspectRatio(String aspectRatio) {
		this.displayAspectRatio = getFormattedAspectRatio(aspectRatio);
	}

	/**
	 * Get the aspect ratio of the video track. This is the codec aspect ratio
	 * of the pixels, which is not always the aspect ratio that the renderer
	 * should display or that we should output; that is
	 * {@link #getDisplayAspectRatio()}
	 *
	 * @return the aspect ratio reported by the codec
	 */
	public String getOriginalDisplayAspectRatio() {
		return originalDisplayAspectRatio;
	}

	/**
	 * Sets the aspect ratio reported by the codec.
	 * @param aspectRatio the aspect ratio to set
	 */
	public void setOriginalDisplayAspectRatio(String aspectRatio) {
		this.originalDisplayAspectRatio = getFormattedAspectRatio(aspectRatio);
	}

	/**
	 * @return the frame rate
	 * @since 1.50.0
	 */
	public Double getFrameRate() {
		return frameRate;
	}

	/**
	 * @return the frame rate in DLNA format
	 */
	public String getFrameRateDLNA() {
		int framerateDLNA = (int) Math.round(frameRate);
		String framerateDLNAString = String.valueOf(framerateDLNA);
		if (scanType != null && scanType == ScanType.INTERLACED) {
			framerateDLNAString += "i";
		} else {
			framerateDLNAString += "p";
		}
		return framerateDLNAString;
	}

	/**
	 * @param frameRate the frame rate to set
	 * @since 1.50.0
	 */
	public void setFrameRate(Double frameRate) {
		this.frameRate = frameRate;
	}

	/**
	 * @return the frameRateModeOriginal
	 */
	public String getFrameRateModeOriginal() {
		return frameRateModeOriginal;
	}

	/**
	 * @param frameRateModeOriginal the frameRateModeOriginal to set
	 */
	public void setFrameRateModeOriginal(String frameRateModeOriginal) {
		this.frameRateModeOriginal = frameRateModeOriginal;
	}

	/**
	 * @return the frameRateMode
	 * @since 1.55.0
	 */
	public String getFrameRateMode() {
		return frameRateMode;
	}

	/**
	 * @param frameRateMode the frameRateMode to set
	 * @since 1.55.0
	 */
	public void setFrameRateMode(String frameRateMode) {
		this.frameRateMode = frameRateMode;
	}

	/**
	 * @return The unaltered frame rate mode
	 */
	public String getFrameRateModeRaw() {
		return frameRateModeRaw;
	}

	/**
	 * @param frameRateModeRaw the unaltered frame rate mode to set
	 */
	public void setFrameRateModeRaw(String frameRateModeRaw) {
		this.frameRateModeRaw = frameRateModeRaw;
	}

	/**
	 * Sets reference frame count for video stream or {@code -1} if not parsed.
	 *
	 * @param referenceFrameCount reference frame count.
	 */
	public void setReferenceFrameCount(byte referenceFrameCount) {
		if (referenceFrameCount < -1) {
			throw new IllegalArgumentException("referenceFrameCount must be >= -1.");
		}
		referenceFrameCountLock.writeLock().lock();
		try {
			this.referenceFrameCount = referenceFrameCount;
		} finally {
			referenceFrameCountLock.writeLock().unlock();
		}
	}

	/**
	 * @return reference frame count for video stream or {@code -1} if not parsed.
	 */
	public byte getReferenceFrameCount() {
		referenceFrameCountLock.readLock().lock();
		try {
			return referenceFrameCount;
		} finally {
			referenceFrameCountLock.readLock().unlock();
		}
	}

	/**
	 * @return the encrypted
	 * @since 1.50.0
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * @param encrypted the encrypted to set
	 * @since 1.50.0
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	/**
	 * @return the muxingMode
	 * @since 1.50.0
	 */
	public String getMuxingMode() {
		return muxingMode;
	}

	/**
	 * @param muxingMode the muxingMode to set
	 * @since 1.50.0
	 */
	public void setMuxingMode(String muxingMode) {
		this.muxingMode = muxingMode;
	}

	/**
	 * @return the bitRate
	 */
	public int getBitRate() {
		return bitRate;
	}

	/**
	 * @param bitrate the bitRate to set
	 */
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	public String getHDRFormat() {
		return videoHDRFormat;
	}

	public void setHDRFormat(String value) {
		this.videoHDRFormat = value;
	}

	public String getHDRFormatCompatibility() {
		return videoHDRFormatCompatibility;
	}

	public void setHDRFormatCompatibility(String value) {
		this.videoHDRFormatCompatibility = value;
	}

	/**
	 * @return Format profile for video stream or {@code null} if not parsed.
	 */
	public String getFormatProfile() {
		return formatProfile;
	}

	/**
	 * Sets Format profile for video stream or {@code null} if not parsed.
	 *
	 * @param formatProfile Format Profile.
	 */
	public void setFormatProfile(String formatProfile) {
		this.formatProfile = formatProfile;
	}

	/**
	 * @return Format level for video stream or {@code null} if not parsed.
	 */
	public String getFormatLevel() {
		return formatLevel;
	}

	/**
	 * @param def the default value to return if null or not number.
	 * @return Format level for video stream or default value if not parsed.
	 */
	public double getFormatLevelAsDouble(double def) {
		if (formatLevel == null) {
			return def;
		}
		try {
			return Double.parseDouble(formatLevel);
		} catch (NumberFormatException e) {
			LOGGER.trace("Could not convert {} to double: {}", formatLevel, e.getMessage());
			return def;
		}
	}

	/**
	 * Sets Format level for video stream or {@code null} if not parsed.
	 *
	 * @param formatLevel Format level.
	 */
	public void setFormatLevel(String formatLevel) {
		this.formatLevel = formatLevel;
	}

	/**
	 * @return Format tier for video stream or {@code null} if not parsed.
	 */
	public String getFormatTier() {
		return formatTier;
	}

	/**
	 * Sets Format tier for video stream or {@code null} if not parsed.
	 *
	 * @param formatLevel Format tier.
	 */
	public void setFormatTier(String formatTier) {
		this.formatTier = formatTier;
	}

	public Map<String, String> getExtras() {
		return extras;
	}

	public void putExtra(String key, String value) {
		if (extras == null) {
			extras = new HashMap<>();
		}

		extras.put(key, value);
	}

	/**
	 * Whether the file contains H.264 (AVC) video.
	 *
	 * @return {boolean}
	 */
	public boolean isH264() {
		return codec != null && codec.startsWith(FormatConfiguration.H264);
	}

	/**
	 * Whether the file contains H.265 (HEVC) video.
	 *
	 * @return {boolean}
	 */
	public boolean isH265() {
		return codec != null && codec.startsWith(FormatConfiguration.H265);
	}

	/**
	 * Whether the file contains MPEG-2 video.
	 *
	 * @return {boolean}
	 */
	public boolean isMpeg2() {
		return codec != null && codec.startsWith(FormatConfiguration.MPEG2);
	}

	public boolean isHDVideo() {
		return (width > 864 || height > 576);
	}

	public boolean isMod4() {
		return (height % 4 == 0 && width % 4 == 0);
	}

	/**
	 * Uses the HDR format compatibility information to return
	 * a string that the renderer config can match if the format
	 * failed to match.
	 *
	 * Note: Sometimes HDR files have a "SDR" compatibility, this means
	 * that any player can play them, so we return null for that just like
	 * any other SDR video.
	 */
	public String getHDRFormatCompatibilityForRenderer() {
		if (StringUtils.isBlank(videoHDRFormatCompatibility)) {
			return null;
		}

		String hdrFormatCompatibilityInRendererFormat = null;
		if (StringUtils.isNotBlank(videoHDRFormatCompatibility)) {
			if (videoHDRFormatCompatibility.startsWith("Dolby Vision")) {
				hdrFormatCompatibilityInRendererFormat = "dolbyvision";
			} else if (
				(
					videoHDRFormatCompatibility.startsWith("HDR10") &&
					!videoHDRFormatCompatibility.startsWith("HDR10+")
				) ||
				videoHDRFormatCompatibility.endsWith("HDR10") // match "HDR10+ Profile A / HDR10"
			) {
				hdrFormatCompatibilityInRendererFormat = "hdr10";
			} else if (videoHDRFormatCompatibility.startsWith("HDR10+")) {
				hdrFormatCompatibilityInRendererFormat = "hdr10+";
			} else if (videoHDRFormatCompatibility.startsWith("HLG")) {
				hdrFormatCompatibilityInRendererFormat = "hlg";
			}
		}

		return hdrFormatCompatibilityInRendererFormat;
	}

	/**
	 * Uses the HDR format and HDR format information
	 * to return a string that the renderer config can match.
	 */
	public String getHDRFormatForRenderer() {
		if (StringUtils.isBlank(videoHDRFormat)) {
			return null;
		}

		String hdrFormatInRendererFormat = null;
		if (StringUtils.isNotBlank(videoHDRFormat)) {
			if (videoHDRFormat.startsWith("Dolby Vision")) {
				hdrFormatInRendererFormat = "dolbyvision";
			} else if (videoHDRFormat.startsWith("HDR10+")) {
				hdrFormatInRendererFormat = "hdr10+";
			} else if (videoHDRFormat.startsWith("HDR10")) {
				hdrFormatInRendererFormat = "hdr10";
			} else if (videoHDRFormat.startsWith("HLG")) {
				hdrFormatInRendererFormat = "hlg";
			}
		}

		return hdrFormatInRendererFormat;
	}

	/*
	 * Check if the analysed aspect ration and the codec report different aspect ratios.
	 */
	public boolean isDisplayAspectRatioFromCodec() {
		return (
			originalDisplayAspectRatio == null ||
			displayAspectRatio == null ||
			displayAspectRatio.equals(originalDisplayAspectRatio)
		);
	}

	/**
	 * Note: This is based on a flag in Matroska files, and as such it is
	 * unreliable; it will be unlikely to find a false-positive but there
	 * will be false-negatives, similar to language flags.
	 *
	 * @return whether the video track is 3D
	 */
	public boolean is3d() {
		return StringUtils.isNotBlank(multiViewLayout);
	}

	/**
	 * The significance of this is that the aspect ratio should not be kept
	 * in this case when transcoding.
	 * Example: 3840x1080 should be resized to 1920x1080, not 1920x540.
	 *
	 * @return whether the video track is full SBS or OU 3D
	 */
	public boolean is3dFullSbsOrOu() {
		if (!is3d()) {
			return false;
		}
		return switch (multiViewLayout.toLowerCase()) {
			case "overunderrt",
				"oulf",
				"ourf",
				"sbslf",
				"sbsrf",
				"top-bottom (left eye first)",
				"top-bottom (right eye first)",
				"side by side (left eye first)",
				"side by side (right eye first)" -> true;
			default -> false;
		};
	}

	public boolean multiViewIsAnaglyph() {
		if (!is3d()) {
			return false;
		}
		return switch (multiViewLayout.toLowerCase()) {
			case "overunderrt",
				"oulf",
				"ourf",
				"sbslf",
				"sbsrf",
				"top-bottom (left eye first)",
				"top-bottom (right eye first)",
				"side by side (left eye first)",
				"side by side (right eye first)",
				"half top-bottom (left eye first)",
				"half side by side (left eye first)" -> false;
			default -> true;
		};
	}

	public Mode3D get3DLayout() {
		if (!is3d()) {
			return null;
		}

		switch (multiViewLayout.toLowerCase()) {
			case "overunderrt", "oulf", "top-bottom (left eye first)" -> {
				return Mode3D.ABL;
			}
			case "ourf", "top-bottom (right eye first)" -> {
				return Mode3D.ABR;
			}
			case "sbslf", "side by side (left eye first)" -> {
				return Mode3D.SBSL;
			}
			case "sbsrf", "side by side (right eye first)" -> {
				return Mode3D.SBSR;
			}
			case "half top-bottom (left eye first)" -> {
				return Mode3D.AB2L;
			}
			case "half side by side (left eye first)" -> {
				return Mode3D.SBS2L;
			}
			case "arcg" -> {
				return Mode3D.ARCG;
			}
			case "arch" -> {
				return Mode3D.ARCH;
			}
			case "arcc" -> {
				return Mode3D.ARCC;
			}
			case "arcd" -> {
				return Mode3D.ARCD;
			}
			case "agmg" -> {
				return Mode3D.AGMG;
			}
			case "agmh" -> {
				return Mode3D.AGMH;
			}
			case "agmc" -> {
				return Mode3D.AGMC;
			}
			case "agmd" -> {
				return Mode3D.AGMD;
			}
			case "aybg" -> {
				return Mode3D.AYBG;
			}
			case "aybh" -> {
				return Mode3D.AYBH;
			}
			case "aybc" -> {
				return Mode3D.AYBC;
			}
			case "aybd" -> {
				return Mode3D.AYBD;
			}
			default -> {
				return null;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Video Id: ").append(getId());
		if (forcedFlag) {
			result.append(", Default");
		}
		if (forcedFlag) {
			result.append(", Forced");
		}
		if (StringUtils.isNotBlank(getLang()) && !UND.equals(getLang())) {
			result.append(", Language Code: ").append(getLang());
		}
		if (StringUtils.isNotBlank(getTitle())) {
			result.append(", Title: ").append(getTitle());
		}
		result.append(", Codec: ").append(getCodec());
		if (StringUtils.isNotBlank(getFormatProfile())) {
			result.append(", Format Profile: ").append(getFormatProfile());
		}
		if (StringUtils.isNotBlank(getFormatLevel())) {
			result.append(", Format Level: ").append(getFormatLevel());
		}
		if (StringUtils.isNotBlank(getFormatTier())) {
			result.append(", Format Tier: ").append(getFormatTier());
		}
		if (getStreamOrder() != null) {
			result.append(", Stream Order: ").append(getStreamOrder());
		}
		if (durationSec != null) {
			result.append(", Duration: ").append(getDurationString());
		}
		result.append(", Resolution: ").append(getWidth()).append(" x ").append(getHeight());
		if (displayAspectRatio != null) {
			result.append(", Display Aspect Ratio: ").append(getDisplayAspectRatio());
		}
		if (pixelAspectRatio != null && pixelAspectRatio != 1) {
			result.append(", Pixel Aspect Ratio: ").append(getPixelAspectRatio());
		}
		if (scanType != null) {
			result.append(", Scan Type: ").append(getScanType());
		}
		if (scanOrder != null) {
			result.append(", Scan Order: ").append(getScanOrder());
		}
		if (frameRate != null) {
			result.append(", Frame Rate: ").append(getFrameRate().doubleValue());
		}
		if (StringUtils.isNotBlank(getFrameRateMode())) {
			result.append(", Frame Rate Mode: ");
			result.append(getFrameRateMode());
			if (StringUtils.isNotBlank(getFrameRateModeRaw())) {
				result.append(" (").append(getFrameRateModeRaw()).append(")");
			}
		} else if (StringUtils.isNotBlank(getFrameRateModeRaw())) {
			result.append(", Frame Rate Mode Raw: ");
			result.append(getFrameRateModeRaw());
		}
		if (StringUtils.isNotBlank(getMuxingMode())) {
			result.append(", Muxing Mode: ").append(getMuxingMode());
		}
		if (StringUtils.isNotBlank(getMatrixCoefficients())) {
			result.append(", Matrix Coefficients: ").append(getMatrixCoefficients());
		}
		if (getReferenceFrameCount() > -1) {
			result.append(", Reference Frame Count: ").append(getReferenceFrameCount());
		}
		if (getBitDepth() != 8) {
			result.append(", Bit Depth: ").append(getBitDepth());
		}
		if (StringUtils.isNotBlank(getHDRFormat())) {
			result.append(", HDR Format: ").append(getHDRFormat());
		}
		if (StringUtils.isNotBlank(getHDRFormatForRenderer())) {
			result.append(" (").append(getHDRFormatForRenderer()).append(")");
		}
		if (StringUtils.isNotBlank(getHDRFormatCompatibility())) {
			result.append(", HDR Format Compatibility: ").append(getHDRFormatCompatibility());
		}
		if (StringUtils.isNotBlank(getHDRFormatCompatibilityForRenderer())) {
			result.append(" (").append(getHDRFormatCompatibilityForRenderer()).append(")");
		}
		return result.toString();
	}

	@Override
	public MediaVideo clone() throws CloneNotSupportedException {
		return (MediaVideo) super.clone();
	}

	/**
	 * This takes an exact aspect ratio, and returns the closest common aspect
	 * ratio to that, so that e.g. 720x416 and 720x420 are the same.
	 *
	 * @param aspect
	 * @return an approximate aspect ratio
	 */
	private static String getFormattedAspectRatio(String aspect) {
		if (StringUtils.isBlank(aspect)) {
			return null;
		}

		if (aspect.contains(":")) {
			return aspect;
		}

		double exactAspectRatio = Double.parseDouble(aspect);
		if (exactAspectRatio >= 11.9 && exactAspectRatio <= 12.1) {
			return "12.00:1";
		} else if (exactAspectRatio >= 3.9 && exactAspectRatio <= 4.1) {
			return "4.00:1";
		} else if (exactAspectRatio >= 2.75 && exactAspectRatio <= 2.77) {
			return "2.76:1";
		} else if (exactAspectRatio >= 2.65 && exactAspectRatio <= 2.67) {
			return "24:9";
		} else if (exactAspectRatio >= 2.58 && exactAspectRatio <= 2.6) {
			return "2.59:1";
		} else if (exactAspectRatio >= 2.54  && exactAspectRatio <= 2.56) {
			return "2.55:1";
		} else if (exactAspectRatio >= 2.38 && exactAspectRatio <= 2.41) {
			return "2.39:1";
		} else if (exactAspectRatio > 2.36 && exactAspectRatio < 2.38) {
			return "2.37:1";
		} else if (exactAspectRatio >= 2.34 && exactAspectRatio <= 2.36) {
			return "2.35:1";
		} else if (exactAspectRatio >= 2.33 && exactAspectRatio < 2.34) {
			return "21:9";
		} else if (exactAspectRatio > 2.1  && exactAspectRatio < 2.3) {
			return "11:5";
		} else if (exactAspectRatio > 1.9 && exactAspectRatio < 2.1) {
			return "2.00:1";
		} else if (exactAspectRatio > 1.87  && exactAspectRatio <= 1.9) {
			return "1.896:1";
		} else if (exactAspectRatio >= 1.83 && exactAspectRatio <= 1.87) {
			return "1.85:1";
		} else if (exactAspectRatio >= 1.7 && exactAspectRatio <= 1.8) {
			return "16:9";
		} else if (exactAspectRatio >= 1.65 && exactAspectRatio <= 1.67) {
			return "15:9";
		} else if (exactAspectRatio >= 1.59 && exactAspectRatio <= 1.61) {
			return "16:10";
		} else if (exactAspectRatio >= 1.54 && exactAspectRatio <= 1.56) {
			return "14:9";
		} else if (exactAspectRatio >= 1.49 && exactAspectRatio <= 1.51) {
			return "3:2";
		} else if (exactAspectRatio > 1.42 && exactAspectRatio < 1.44) {
			return "1.43:1";
		} else if (exactAspectRatio > 1.372 && exactAspectRatio < 1.4) {
			return "11:8";
		} else if (exactAspectRatio > 1.35 && exactAspectRatio <= 1.372) {
			return "1.37:1";
		} else if (exactAspectRatio >= 1.3 && exactAspectRatio <= 1.35) {
			return "4:3";
		} else if (exactAspectRatio > 1.2 && exactAspectRatio < 1.3) {
			return "5:4";
		} else if (exactAspectRatio >= 1.18 && exactAspectRatio <= 1.195) {
			return "19:16";
		} else if (exactAspectRatio > 0.99 && exactAspectRatio < 1.1) {
			return "1:1";
		} else if (exactAspectRatio > 0.7 && exactAspectRatio < 0.9) {
			return "4:5";
		} else if (exactAspectRatio > 0.6 && exactAspectRatio < 0.7) {
			return "2:3";
		} else if (exactAspectRatio > 0.5 && exactAspectRatio < 0.6) {
			return "9:16";
		} else {
			return aspect;
		}
	}

	/**
	 * Used by FFmpeg for 3D video format naming
	 */
	public enum Mode3D {
		ML,
		MR,
		SBSL,
		SBSR,
		SBS2L,
		SBS2R,
		ABL,
		ABR,
		AB2L,
		AB2R,
		ARCG,
		ARCH,
		ARCC,
		ARCD,
		AGMG,
		AGMH,
		AGMC,
		AGMD,
		AYBG,
		AYBH,
		AYBC,
		AYBD
	}

	/**
	 * This {@code enum} represents the different video "scan types".
	 */
	public enum ScanType {

		/** Interlaced scan, any sub-type */
		INTERLACED,

		/** Mixed scan */
		MIXED,

		/** Progressive scan */
		PROGRESSIVE;

		@Override
		public String toString() {
			return switch (this) {
				case INTERLACED -> "Interlaced";
				case MIXED -> "Mixed";
				case PROGRESSIVE -> "Progressive";
				default -> name();
			};
		}

		public static ScanType typeOf(String scanType) {
			if (StringUtils.isBlank(scanType)) {
				return null;
			}
			scanType = scanType.trim().toLowerCase(Locale.ROOT);
			switch (scanType) {
				case "interlaced" -> {
					return INTERLACED;
				}
				case "mixed" -> {
					return MIXED;
				}
				case "progressive" -> {
					return PROGRESSIVE;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanType \"{}\"", scanType);
					return null;
				}
			}
		}
	}

	/**
	 * This {@code enum} represents the video scan order.
	 */
	public enum ScanOrder {

		/** Bottom Field First */
		BFF,

		/** Bottom Field Only */
		BFO,

		/** Pulldown */
		PULLDOWN,

		/** 2:2:2:2:2:2:2:2:2:2:2:3 Pulldown */
		PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3,

		/** 2:3 Pulldown */
		PULLDOWN_2_3,

		/** Top Field First */
		TFF,

		/** Top Field Only */
		TFO;

		@Override
		public String toString() {
			return switch (this) {
				case BFF -> "Bottom Field First";
				case BFO -> "Bottom Field Only";
				case PULLDOWN -> "Pulldown";
				case PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3 -> "2:2:2:2:2:2:2:2:2:2:2:3 Pulldown";
				case PULLDOWN_2_3 -> "2:3 Pulldown";
				case TFF -> "Top Field First";
				case TFO -> "Top Field Only";
				default -> name();
			};
		}

		public static ScanOrder typeOf(String scanOrder) {
			if (StringUtils.isBlank(scanOrder)) {
				return null;
			}
			scanOrder = scanOrder.trim().toLowerCase(Locale.ROOT);
			switch (scanOrder) {
				case "bff", "bottom field first" -> {
					return BFF;
				}
				case "bfo", "bottom field only" -> {
					return BFO;
				}
				case "pulldown" -> {
					return PULLDOWN;
				}
				case "2:2:2:2:2:2:2:2:2:2:2:3 pulldown" -> {
					return PULLDOWN_2_2_2_2_2_2_2_2_2_2_2_3;
				}
				case "2:3 pulldown" -> {
					return PULLDOWN_2_3;
				}
				case "tff", "top field first" -> {
					return TFF;
				}
				case "tfo", "top field only" -> {
					return TFO;
				}
				default -> {
					LOGGER.debug("Warning: Unrecognized ScanOrder \"{}\"", scanOrder);
					if (scanOrder.contains("pulldown")) {
						return PULLDOWN;
					}
					return null;
				}
			}
		}
	}
}
