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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.pms.dlna.DLNAImageProfile.HypotheticalResult;
import net.pms.dlna.protocolinfo.ProtocolInfo;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.network.UPNPControl.Renderer;

/**
 * This class is used to represent a {@code <res>} element representing an image
 * (including thumbnail) in a {@code DIDL-Lite} document.
 *
 * @author Nadahar
 */
public class DLNAImageResElement {
	private final DLNAImageProfile profile;
	private final Integer ciFlag;
	private final boolean thumbnail;
	private final HypotheticalResult hypotheticalResult;

	/**
	 * Instantiates a new DLNA image {@code <res>} element.
	 *
	 * @param profile the {@link DLNAImageProfile} for this {@code <res>}
	 *            element.
	 * @param imageInfo the {@link ImageInfo} for the image represented by this
	 *            {@code <res>} element.
	 * @param thumbnail whether the source for this {@code <res>} element is a
	 *            thumbnail.
	 *
	 * @see #isThumbnail()
	 */
	public DLNAImageResElement(DLNAImageProfile profile, ImageInfo imageInfo, boolean thumbnail) {
		this(profile, imageInfo, thumbnail, null);
	}

	/**
	 * Instantiates a new DLNA image {@code <res>} element.
	 *
	 * @param profile the {@link DLNAImageProfile} for this {@code <res>}
	 *            element.
	 * @param imageInfo the {@link ImageInfo} for the image represented by this
	 *            {@code <res>} element.
	 * @param thumbnail whether the source for this {@code <res>} element is a
	 *            thumbnail.
	 * @param overrideCIFlag The overridden CI flag for this {@code <res>}
	 *            element. Pass {@code null} for automatic setting of the CI
	 *            flag.
	 *
	 * @see #isThumbnail()
	 */
	public DLNAImageResElement(DLNAImageProfile profile, ImageInfo imageInfo, boolean thumbnail, Integer overrideCIFlag) {
		this.profile = profile;
		if (profile != null && imageInfo != null) {
			hypotheticalResult = profile.calculateHypotheticalProperties(imageInfo);
			ciFlag = overrideCIFlag == null ?
				(hypotheticalResult.conversionNeeded ?
					Integer.valueOf(1) :
					Integer.valueOf(0)
				) :
				overrideCIFlag;
		} else {
			hypotheticalResult = null;
			ciFlag = overrideCIFlag;
		}
		this.thumbnail = thumbnail;
	}

	/**
	 * @return The {@link DLNAImageProfile}.
	 */
	public DLNAImageProfile getProfile() {
		return profile;
	}

	/**
	 * @return The CI flag value or {@code null}.
	 */
	public Integer getCiFlag() {
		return ciFlag;
	}

	/**
	 * Note: This can be confusing. This doesn't indicate whether the res
	 * element is <b>used</b> as a thumbnail, but if the res element's source is
	 * a thumbnail from UMS' point of view. For low resolution images (where the
	 * resolution is equal or smaller than the cached thumbnail), the thumbnail
	 * source can be used also for the image itself for increased performance.
	 *
	 * @return Whether this element has a thumbnail source.
	 */
	public boolean isThumbnail() {
		return thumbnail;
	}

	/**
	 * @return Whether the resolution for this image is known.
	 */
	public boolean isResolutionKnown() {
		return hypotheticalResult != null && hypotheticalResult.width > 0 && hypotheticalResult.height > 0;
	}

	/**
	 * @return The calculated image width or {@link ImageInfo#UNKNOWN} if
	 *         unknown.
	 */
	public int getWidth() {
		return hypotheticalResult != null ? hypotheticalResult.width : ImageInfo.UNKNOWN;
	}

	/**
	 * @return The calculated image height or {@link ImageInfo#UNKNOWN} if
	 *         unknown.
	 */
	public int getHeight() {
		return hypotheticalResult != null ? hypotheticalResult.height : ImageInfo.UNKNOWN;
	}

	/**
	 * @return The image size or {@code null} if unknown.
	 */
	public Long getSize() {
		return hypotheticalResult != null ? hypotheticalResult.size : null;
	}

	/**
	 * Only useful for the {@link Comparator}. Use the individual getter to
	 * obtain the actual values.
	 *
	 * @return The {@link HypotheticalResult}.
	 */
	private HypotheticalResult getHypotheticalResult() {
		return hypotheticalResult;
	}

	@Override
	public String toString() {
		return
			"DLNAImageResElement [profile=" + profile + ", ciFlag=" + ciFlag +
			", thumbnail=" + thumbnail + ", hypotheticalResult=" +
			hypotheticalResult + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ciFlag == null) ? 0 : ciFlag.hashCode());
		result = prime * result + ((hypotheticalResult == null) ? 0 : hypotheticalResult.hashCode());
		result = prime * result + ((profile == null) ? 0 : profile.hashCode());
		result = prime * result + (thumbnail ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DLNAImageResElement)) {
			return false;
		}
		DLNAImageResElement other = (DLNAImageResElement) obj;
		if (ciFlag == null) {
			if (other.ciFlag != null) {
				return false;
			}
		} else if (!ciFlag.equals(other.ciFlag)) {
			return false;
		}
		if (hypotheticalResult == null) {
			if (other.hypotheticalResult != null) {
				return false;
			}
		} else if (!hypotheticalResult.equals(other.hypotheticalResult)) {
			return false;
		}
		if (profile == null) {
			if (other.profile != null) {
				return false;
			}
		} else if (!profile.equals(other.profile)) {
			return false;
		}
		if (thumbnail != other.thumbnail) {
			return false;
		}
		return true;
	}

	/**
	 * Constructs a {@link Comparator} for sorting {@link DLNAImageResElement}s
	 * by priority with the highest priority first.
	 *
	 * @param sourceFormat the {@link ImageFormat} of the source image, use to
	 *            decide the preferred {@link DLNAImageProfile}s.
	 * @return The {@link Comparator}.
	 */
	public static Comparator<DLNAImageResElement> getComparator(ImageFormat sourceFormat) {
		// This defines what DLNA format should be preferred for per source format
		final ImageFormat preferredFormat;
		if (sourceFormat != null) {
			switch (sourceFormat) {
				case GIF:
					preferredFormat = ImageFormat.GIF;
					break;
				case CUR:
				case ICNS:
				case ICO:
				case PNG:
				case PSD:
				case TIFF:
				case WEBP:
					preferredFormat = ImageFormat.PNG;
					break;
				case ARW:
				case BMP:
				case CR2:
				case CRW:
				case DCX:
				case JPEG:
				case NEF:
				case ORF:
				case PCX:
				case PNM:
				case RAF:
				case RW2:
				case WBMP:
				default:
					preferredFormat = ImageFormat.JPEG;
					break;

			}
		} else {
			preferredFormat = ImageFormat.JPEG;
		}
		return new Comparator<DLNAImageResElement>() {

			@Override
			public int compare(DLNAImageResElement o1, DLNAImageResElement o2) {
				if (o1 == null && o2 == null) {
					return 0;
				} else if (o1 == null) {
					return 1;
				} else if (o2 == null) {
					return -1;
				}

				if (o1.isThumbnail() != o2.isThumbnail()) {
					return (o1.isThumbnail() ? 1 : 0) - (o2.isThumbnail() ? 1 : 0);
				}

				int i =
					(o1.getCiFlag() == null ? 2 : o1.getCiFlag()) -
					(o2.getCiFlag() == null ? 2 : o2.getCiFlag());
				if (i != 0) {
					return i;
				}

				ImageFormat o1Format = o1.getProfile() != null ? o1.getProfile().getFormat() : null;
				ImageFormat o2Format = o2.getProfile() != null ? o2.getProfile().getFormat() : null;

				if (o1Format != o2Format) {
					if (o1Format == null) {
						return 1;
					} else if (o2Format == null) {
						return -1;
					}
					if (o1Format == preferredFormat) {
						return -1;
					}
					if (o2Format == preferredFormat) {
						return 1;
					}
					return o1Format.compareTo(o2Format);
				}

				if (
					(DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile()) ||
					DLNAImageProfile.JPEG_RES_H_V.equals(o2.getProfile())) &&
					(!DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile()) ||
					!DLNAImageProfile.JPEG_RES_H_V.equals(o2.getProfile()))
				) {
					if (DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile())) {
						return -1;
					}
					return 1;
				}

				if (o1.getWidth() != o2.getWidth()) {
					return o2.getWidth() - o1.getWidth();
				}

				if (o1.getHeight() != o2.getHeight()) {
					return o2.getHeight() - o1.getHeight();
				}

				if (o1.getProfile() != null || o2.getProfile() != null) {
					if (o1.getProfile() == null) {
						return 1;
					}
					if (o2.getProfile() == null) {
						return -1;
					}
					if (!o1.getProfile().equals(o2.getProfile())) {
						return o1.getProfile().toInt() - o2.getProfile().toInt();
					}
				}

				long l =
					(o2.getSize() == null ? 0 : o2.getSize()) -
					(o1.getSize() == null ? 0 : o1.getSize());
				if (l != 0) {
					return (int) l;
				}

				if (o1.getHypotheticalResult() != null || o2.getHypotheticalResult() != null) {
					// This comparison serves no practical purpose other than
					// to fulfill the contract with equals().
					if (o1.getHypotheticalResult() == null) {
						return 1;
					}
					if (o2.getHypotheticalResult() == null) {
						return -1;
					}
					if (o1.getHypotheticalResult().conversionNeeded != o2.getHypotheticalResult().conversionNeeded) {
						return
							(o1.getHypotheticalResult().conversionNeeded ? 1 : 0) -
							(o2.getHypotheticalResult().conversionNeeded ? 1 : 0);
					}
				}
				return 0;
			}
		};
	}

	/**
	 * Filter out {@link DLNAImageResElement}s not supported by {@code renderer}.
	 *
	 * @param resElements the {@link List} of {@link DLNAImageResElement}s to filter.
	 * @param renderer the {@link Renderer} to use for filtering.
	 */
	public static void filterResElements(List<DLNAImageResElement> resElements, Renderer renderer) {
		if (
			renderer == null ||
			renderer.deviceProtocolInfo == null ||
			renderer.deviceProtocolInfo.isImageProfilesEmpty()
		) {
			return;
		}
		Iterator<DLNAImageResElement> iterator = resElements.iterator();
		while (iterator.hasNext()) {
			DLNAImageResElement resElement = iterator.next();
			if (!renderer.deviceProtocolInfo.imageProfilesContains(resElement.getProfile())) {
				iterator.remove();
			}
		}
	}

	/**
	 * Checks whether a given {@link DLNAImageProfile} is supported by
	 * {@code renderer} according to acquired {@link ProtocolInfo} information.
	 * If no information or no supported image profiles are available, it's
	 * considered supported. The reason is that not all renderers provide this
	 * information, which means that all must be assumed supported as opposed to
	 * none.
	 *
	 * @param profile the {@link DLNAImageProfile} whose support to examine.
	 * @param renderer the {@link Renderer} for which to check supported
	 *            {@link DLNAImageProfile}s.
	 * @return {@code true} if {@code profile} is supported or no image profile
	 *         information is available, {@code false} otherwise.
	 */
	public static boolean isImageProfileSupported(DLNAImageProfile profile, Renderer renderer) {
		return
			renderer == null ||
			renderer.deviceProtocolInfo == null ||
			renderer.deviceProtocolInfo.isImageProfilesEmpty() ||
			renderer.deviceProtocolInfo.imageProfilesContains(profile);
	}
}
