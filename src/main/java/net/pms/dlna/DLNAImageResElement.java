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
import net.pms.dlna.DLNAImageProfile.CalculatedImage;
import net.pms.dlna.protocolinfo.DLNAOrgConversionIndicator;
import net.pms.dlna.protocolinfo.ProtocolInfo;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.network.UPNPControl.Renderer;
import static net.pms.util.StringUtil.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * This class is used to represent a {@code <res>} element representing an image
 * (including thumbnail) in a {@code DIDL-Lite} document.
 *
 * @author Nadahar
 */
public class DLNAImageResElement extends UPnPImageResElement {

	private static final long serialVersionUID = 1L;
	protected final CalculatedImage calculatedImage;

	/**
	 * For internal use only, use {@link DLNAImageResElement#create} instead.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} to use.
	 * @param format the {@link ImageFormat} to use.
	 * @param url the URL to use.
	 * @param sourceImageInfo the {@link ImageInfo} describing this {@link Image}.
	 * @param convert Whether or not the source image must be converted.
	 * @param thumbnailSource Whether or not the source is a thumbnail, see
	 *            {@link #thumbnailSource}.
	 * @param calculatedImage the {@link CalculatedImage} to use.
	 */
	protected DLNAImageResElement(
		ProtocolInfo protocolInfo,
		ImageFormat format,
		String url,
		ImageInfo sourceImageInfo,
		boolean convert,
		boolean thumbnailSource,
		CalculatedImage calculatedImage
	) {
		super(protocolInfo, format, url, sourceImageInfo, convert, thumbnailSource);
		this.calculatedImage = calculatedImage;
	}

	/**
	 * Creates a new image {@code <res@dlna>} element instance.
	 * <p>
	 * A static factory method must be used instead of a constructor here
	 * because of Java's flawed constructor design. A constructor isn't allow to
	 * generate appropriate super arguments before calling the super
	 * constructor.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} for this {@code <res>}
	 *            element.
	 * @param sourceImageInfo the {@link ImageInfo} for the image represented by this
	 *            {@code <res>} element.
	 * @param thumbnail whether the source for this {@code <res>} element is a
	 *            thumbnail.
	 */
	public static DLNAImageResElement create(ProtocolInfo protocolInfo, ImageInfo sourceImageInfo, boolean thumbnail) {
		return create(protocolInfo, sourceImageInfo, thumbnail, null);
	}

	/**
	 * Creates a new image {@code <res@dlna>} element instance.
	 * <p>
	 * A static factory method must be used instead of a constructor here
	 * because of Java's flawed constructor design. A constructor isn't allow to
	 * generate appropriate super arguments before calling the super
	 * constructor.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} for this {@code <res>}
	 *            element.
	 * @param sourceImageInfo the {@link ImageInfo} for the image represented by this
	 *            {@code <res>} element.
	 * @param thumbnailSource whether the source for this {@code <res@dlna>}
	 *            element is a thumbnail.
	 * @param overrideCIFlag The overridden CI flag for this {@code <res>}
	 *            element. Pass {@code null} for automatic setting of the CI
	 *            flag.
	 */
	public static DLNAImageResElement create(
		ProtocolInfo protocolInfo,
		DLNAResource resource,
		boolean thumbnailSource,
		Integer overrideCIFlag
	) {
		if (resource == null) {
			return null;
		}
		if (!(protocolInfo.getDLNAProfileName() instanceof DLNAImageProfile)) {
			throw new IllegalArgumentException("protocolInfo must be a DLNAImageProfile instance");
		}

		CalculatedImage calculatedImage = null;
		ImageInfo imageInfo; //TODO: (Nad) ...
		Integer ciFlag = null;
		if (imageInfo != null) {
			DLNAImageProfile profile = (DLNAImageProfile) protocolInfo.getDLNAProfileName();
			calculatedImage = profile.calculateHypotheticalProperties(imageInfo);
			if (overrideCIFlag == null) {
				ciFlag = calculatedImage.conversionNeeded ? Integer.valueOf(1) : Integer.valueOf(0);
			} else {
				ciFlag = overrideCIFlag;
			}
		} else if (overrideCIFlag != null) {
			ciFlag = overrideCIFlag;
		}
		if (ciFlag != null && protocolInfo.getDLNAConversionIndicator().getIntValue() != ciFlag.intValue()) {
			protocolInfo = protocolInfo.modify(
				DLNAOrgConversionIndicator.FACTORY.getConversionIndicator(ciFlag)
			);
		}
		String url;
		if (thumbnailSource) {
			url = resource.getThumbnailURL(prefix, extension);
		} else {
			url = resource.getURL(prefix, extension);
		}
	}

	@Override
	protected String getURL(DLNAResource resource, boolean isThumbnail) {
		String result;
		if (isThumbnail) {
			result = resource.getThumbnailURL(resElement.getProfile());
		} else {
			result = resource.getURL(
				(DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile()) ?
					"JPEG_RES" + resElement.getWidth() + "x" + resElement.getHeight() :
					resElement.getProfile().getValue()
				) + "_"
			);
		}
	}

	/**
	 * @return The {@link DLNAImageProfile}.
	 */
	public DLNAImageProfile getProfile() {
		return (DLNAImageProfile) protocolInfo.getDLNAProfileName();
	}

	/**
	 * @return The CI flag instance or {@code null}.
	 */
	public DLNAOrgConversionIndicator getCiFlag() {
		return protocolInfo.getDLNAConversionIndicator();
	}

	/**
	 * @return Whether the resolution for this image is known.
	 */
	public boolean isResolutionKnown() {
		return calculatedImage != null && calculatedImage.width > 0 && calculatedImage.height > 0;
	}

	/**
	 * @return The calculated image width or {@link ImageInfo#UNKNOWN} if
	 *         unknown.
	 */
	public int getWidth() {
		return calculatedImage != null ? calculatedImage.width : ImageInfo.UNKNOWN;
	}

	/**
	 * @return The calculated image height or {@link ImageInfo#UNKNOWN} if
	 *         unknown.
	 */
	public int getHeight() {
		return calculatedImage != null ? calculatedImage.height : ImageInfo.UNKNOWN;
	}

	/**
	 * @return The image size or {@code null} if unknown.
	 */
	@Override
	public Long getSize() {
		return calculatedImage != null ? calculatedImage.size : null;
	}

	/**
	 * Only useful for the {@link Comparator}. Use the individual getter to
	 * obtain the actual values.
	 *
	 * @return The {@link CalculatedImage}.
	 */
	private CalculatedImage getCalculatedImage() {
		return calculatedImage;
	}

	@Override
	public String toString() {
		return "DLNAImageResElement [" +
			"protocolInfo=" + protocolInfo +
			", thumbnail=" + thumbnailSource +
			", calculatedImage=" + calculatedImage +
		"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calculatedImage == null) ? 0 : calculatedImage.hashCode());
		result = prime * result + ((protocolInfo == null) ? 0 : protocolInfo.hashCode());
		result = prime * result + (thumbnailSource ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object object) { //TODO: (Nad) Update
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (!(object instanceof DLNAImageResElement)) {
			return false;
		}
		DLNAImageResElement other = (DLNAImageResElement) object;
		if (calculatedImage == null) {
			if (other.calculatedImage != null) {
				return false;
			}
		} else if (!calculatedImage.equals(other.calculatedImage)) {
			return false;
		}
		if (protocolInfo == null) {
			if (other.protocolInfo != null) {
				return false;
			}
		} else if (!protocolInfo.equals(other.protocolInfo)) {
			return false;
		}
		if (thumbnailSource != other.thumbnailSource) {
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
	public static Comparator<DLNAImageResElement> getComparator(ImageFormat sourceFormat) { //TODO: (Nad) Remake
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

				if (o1.isThumbnailSource() != o2.isThumbnailSource()) {
					return (o1.isThumbnailSource() ? 1 : 0) - (o2.isThumbnailSource() ? 1 : 0);
				}

				int i =
					(o1.protocolInfo.getDLNAConversionIndicator() == DLNAOrgConversionIndicator.TRUE ? 1 : 0) -
					(o2.protocolInfo.getDLNAConversionIndicator() == DLNAOrgConversionIndicator.TRUE ? 1 : 0);
				if (i != 0) {
					return i;
				}

				DLNAImageProfile o1Profile = o1.getProfile();
				DLNAImageProfile o2Profile = o2.getProfile();
				ImageFormat o1Format = o1Profile != null ? o1Profile.getFormat() : null;
				ImageFormat o2Format = o2Profile != null ? o2Profile.getFormat() : null;

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
					(
						DLNAImageProfile.JPEG_RES_H_V.equals(o1Profile) ||
						DLNAImageProfile.JPEG_RES_H_V.equals(o2Profile)
					) && (
						!DLNAImageProfile.JPEG_RES_H_V.equals(o1Profile) ||
						!DLNAImageProfile.JPEG_RES_H_V.equals(o2Profile)
					)
				) {
					if (DLNAImageProfile.JPEG_RES_H_V.equals(o1Profile)) {
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

				if (o1Profile != null || o2Profile != null) {
					if (o1Profile == null) {
						return 1;
					}
					if (o2Profile == null) {
						return -1;
					}
					if (!o1Profile.equals(o2Profile)) {
						return o1Profile.toInt() - o2Profile.toInt();
					}
				}

				long l = //TODO: (Nad) super..?
					(o2.getSize() == null ? 0 : o2.getSize()) -
					(o1.getSize() == null ? 0 : o1.getSize());
				if (l != 0) {
					return (int) l;
				}

				if (o1.getCalculatedImage() != null || o2.getCalculatedImage() != null) {
					// This comparison serves no practical purpose other than
					// to fulfill the contract with equals().
					if (o1.getCalculatedImage() == null) {
						return 1;
					}
					if (o2.getCalculatedImage() == null) {
						return -1;
					}
					if (o1.getCalculatedImage().conversionNeeded != o2.getCalculatedImage().conversionNeeded) {
						return
							(o1.getCalculatedImage().conversionNeeded ? 1 : 0) -
							(o2.getCalculatedImage().conversionNeeded ? 1 : 0);
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
			renderer.deviceProtocolInfo.isImageProfilesEmpty() || //TODO: (Nad) Refactor
			renderer.deviceProtocolInfo.imageProfilesContains(profile);
	}

	public String toResString(DLNAResource resource) {
		StringBuilder sb = new StringBuilder();
		appendResString(sb, resource);
		return sb.toString();
	}

	public void appendResString(StringBuilder sb, DLNAResource resource) {
		DLNAImageProfile profile = (DLNAImageProfile) protocolInfo.getDLNAProfileName();
		if (!isResolutionKnown() && DLNAImageProfile.JPEG_RES_H_V.equals(profile)) {
			throw new IllegalArgumentException("Resolution cannot be unknown for DLNAImageProfile.JPEG_RES_H_V");
		}
		String url;
		if (thumbnailSource) {
			url = resource.getThumbnailURL(profile);
		} else if (DLNAImageProfile.JPEG_RES_H_V.equals(profile)) {
			url = resource.getURL("JPEG_RES" + getWidth() + "x" + getHeight() + "_");
		} else {
			url = resource.getURL(profile + "_");
		}
		if (isNotBlank(url)) {
			openTag(sb, "res");
			//TODO: (Nad) dc:date
			if (getSize() != null && getSize() > 0) {
				addAttribute(sb, "size", getSize());
			}
			if (isResolutionKnown()) {
				addAttribute(sb, "resolution", Integer.toString(getWidth()) + "x" + Integer.toString(getHeight()));
			}

			addAttribute(sb, "protocolInfo", protocolInfo);
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			endTag(sb);
			sb.append(url);
			closeTag(sb, "res");
		}
	}
}
