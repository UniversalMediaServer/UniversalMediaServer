/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
package net.pms.dlna;

import java.awt.Dimension;
import java.util.TreeMap;
import org.fourthline.cling.support.model.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.protocolinfo.DLNAOrgConversionIndicator;
import net.pms.dlna.protocolinfo.MimeType;
import net.pms.dlna.protocolinfo.ProtocolInfo;
import net.pms.dlna.protocolinfo.ProtocolInfoAttribute;
import net.pms.dlna.protocolinfo.ProtocolInfoAttributeName;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import static net.pms.util.StringUtil.*;

/**
 * This abstract class is used to represent an UPnP {@code <res>} element in a
 * {@code DIDL-Lite} document.
 *
 * @author Nadahar
 */
public class UPnPImageResElement extends UPnPResElement {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(UPnPImageResElement.class);

	/**
	 * Whether or not this image {@code res} element's source is a (cached)
	 * thumbnail.
	 * <p>
	 * Note: <b>This can be confusing</b>: This doesn't indicate whether the
	 * {@code res} element is <b>used</b> as a thumbnail, but if the {@code res}
	 * element's source is a thumbnail from UMS' point of view. For low
	 * resolution images (where the resolution is equal or smaller than the
	 * cached thumbnail), the thumbnail source can be used also for the image
	 * itself for increased performance.
	 */
	protected final boolean thumbnailSource;

	/** The {@link ImageInfo} describing the source {@link Image}. */
	protected final ImageInfo sourceImageInfo;

	/** The {@link ImageFormat} this {@link UPnPImageResElement} represents. */
	protected final ImageFormat format;

	/** Whether or not the source image must be converted. */
	protected final boolean convert;

	/**
	 * For internal use only, use {@link UPnPImageResElement#create} instead.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} to use.
	 * @param format the {@link ImageFormat} to use.
	 * @param url the URL to use.
	 * @param sourceImageInfo the {@link ImageInfo} describing this
	 *            {@link Image}.
	 * @param convert Whether or not the source image must be converted.
	 * @param thumbnailSource Whether or not the source image is a thumbnail,
	 *            see {@link #thumbnailSource}.
	 */
	protected UPnPImageResElement(
		ProtocolInfo protocolInfo,
		ImageFormat format,
		String url,
		ImageInfo sourceImageInfo,
		boolean convert,
		boolean thumbnailSource
	) {
		super(protocolInfo, url);
		if (!"image".equals(protocolInfo.getMimeType().getType())) {
			throw new IllegalArgumentException("protocolInfo must be an image type");
		}
		if (format == null) {
			throw new IllegalArgumentException("format cannot be null");
		}
		this.format = format;
		this.sourceImageInfo = sourceImageInfo;
		this.convert = convert;
		this.thumbnailSource = thumbnailSource;
	}

	/**
	 * Creates a new image {@code <res>} element instance.
	 * <p>
	 * A static factory method must be used instead of a constructor here
	 * because of Java's flawed constructor design. A constructor isn't allow to
	 * generate appropriate super arguments before calling the super
	 * constructor.
	 *
	 * @param protocol the {@link Protocol} to use, or {@code null} for
	 *            {@link Protocol#HTTP_GET}.
	 * @param network the network string to use, or {@code null} for
	 *            {@link ProtocolInfo#WILDCARD}.
	 * @param mimeType the {@link MimeType} to use.
	 * @param resource the {@link DLNAResource}.
	 * @param thumbnailSource whether the thumbnail should be used as source.
	 * @return The new {@link UPnPImageResElement} instance.
	 */
	public static UPnPImageResElement create(
		Protocol protocol,
		String network,
		MimeType mimeType,
		DLNAResource resource,
		boolean thumbnailSource
	) {
		return create(protocol, network, mimeType, resource, thumbnailSource, null);
	}

	/**
	 * Creates a new image {@code <res>} element instance.
	 * <p>
	 * A static factory method must be used instead of a constructor here
	 * because of Java's flawed constructor design. A constructor isn't allow to
	 * generate appropriate super arguments before calling the super
	 * constructor.
	 *
	 * @param protocol the {@link Protocol} to use, or {@code null} for
	 *            {@link Protocol#HTTP_GET}.
	 * @param network the network string to use, or {@code null} for
	 *            {@link ProtocolInfo#WILDCARD}.
	 * @param mimeType the {@link MimeType} to use.
	 * @param resource the {@link DLNAResource}.
	 * @param thumbnailSource whether the thumbnail should be used as source.
	 * @param overrideCIFlag The overridden CI flag for this {@code <res>}
	 *            element. Pass {@code null} for automatic setting of the CI
	 *            flag.
	 * @return The new {@link UPnPImageResElement} instance.
	 */
	public static UPnPImageResElement create(
		Protocol protocol,
		String network,
		MimeType mimeType,
		DLNAResource resource,
		boolean thumbnailSource,
		Integer overrideCIFlag
	) {
		if (resource == null) {
			throw new IllegalArgumentException("resource cannot be null");
		}
		if (mimeType == null) {
			throw new IllegalArgumentException("mimeType cannot be null");
		}
		if (protocol == null) {
			protocol = Protocol.HTTP_GET;
		}

		ImageInfo imageInfo = null;
		ImageFormat sourceFormat = null;
		if (resource.getMedia() != null) {
			if (thumbnailSource) {
				if (resource.getMedia().getThumb() != null) {
					imageInfo = resource.getMedia().getThumb().getImageInfo();
				}
			} else {
				imageInfo = resource.getMedia().getImageInfo();
			}
		}
		if (imageInfo != null) {
			sourceFormat = imageInfo.getFormat();
		}
		if (sourceFormat == null && LOGGER.isDebugEnabled()) {
			LOGGER.debug("Warning: Could not figure out source image format for {}", resource.getSystemName());
			LOGGER.trace(
				"protocol: {}\nmimeType: {}\nthumbnailSource: {}\noverrideCIFlag: {}",
				protocol,
				mimeType,
				thumbnailSource ? "True" : "False",
				overrideCIFlag
			);
		}

		ImageFormat format = ImageFormat.toImageFormat(mimeType);
		if (format == null) {
			throw new IllegalStateException("Unable to resolve an image format from \"" + mimeType + "\"");
		}
		boolean convert = format != sourceFormat;

		// Set protocolInfo attributes
		TreeMap<ProtocolInfoAttributeName, ProtocolInfoAttribute> attributes = ProtocolInfo.createEmptyAttributesMap();
		DLNAOrgConversionIndicator ciFlag;
		if (overrideCIFlag != null) {
			ciFlag = DLNAOrgConversionIndicator.FACTORY.getConversionIndicator(overrideCIFlag.intValue());
		} else {
			ciFlag = DLNAOrgConversionIndicator.FACTORY.getConversionIndicator(convert);
		}
		attributes.put(ciFlag.getName(), ciFlag);

		ProtocolInfo protocolInfo = new ProtocolInfo(protocol, network, mimeType, attributes);
		return create(protocolInfo, format, resource, convert, thumbnailSource);
	}


	/**
	 * Creates a new image {@code <res>} element instance.
	 * <p>
	 * A static factory method must be used instead of a constructor here
	 * because of Java's flawed constructor design. A constructor isn't allow to
	 * generate appropriate super arguments before calling the super
	 * constructor.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} to use.
	 * @param format the {@link ImageFormat} this {@code <res>} element
	 *            represents.
	 * @param resource the {@link DLNAResource}.
	 * @param convert Whether or not the source image format is different from
	 *            {@code format}.
	 * @param thumbnailSource whether the thumbnail should be used as source.
	 * @return The new {@link UPnPImageResElement} instance.
	 */
	public static UPnPImageResElement create(
		ProtocolInfo protocolInfo,
		ImageFormat format,
		DLNAResource resource,
		boolean convert,
		boolean thumbnailSource
	) {
		if (protocolInfo == null) {
			throw new IllegalArgumentException("protocolInfo cannot be null");
		}
		if (resource == null) {
			throw new IllegalArgumentException("resource cannot be null");
		}
		ImageInfo imageInfo = null;
		ImageFormat sourceFormat = null;
		if (resource.getMedia() != null) {
			if (thumbnailSource) {
				if (resource.getMedia().getThumb() != null) {
					imageInfo = resource.getMedia().getThumb().getImageInfo();
				}
			} else {
				imageInfo = resource.getMedia().getImageInfo();
			}
		}
		if (imageInfo != null) {
			sourceFormat = imageInfo.getFormat();
		}
		if (sourceFormat == null && LOGGER.isDebugEnabled()) {
			LOGGER.debug("Warning: Could not figure out source image format for {}", resource.getSystemName());
			LOGGER.trace("protocolInfo: {}\nresource: {}\nimageInfo: {}\nthumbnailSource: {}", protocolInfo, resource, imageInfo,
				thumbnailSource ? "True" : "False");
		}

		String prefix = "UPnP_" + format.toString();
		String extension = convert ? format.getDefaultExtension() : null;
		String url;
		if (thumbnailSource) {
			url = resource.getThumbnailURL(prefix, extension);
		} else {
			url = resource.getURL(prefix, extension);
		}
		return new UPnPImageResElement(protocolInfo, format, url, imageInfo, convert, thumbnailSource);
	}

	@Override
	protected void appendResAttributes(StringBuilder sb) {
		if (sourceImageInfo == null) {
			return;
		}
		String s;
		if (!convert) {
			s = sourceImageInfo.getResSize();
			if (s != null) {
				addAttribute(sb, "size", s);
			}
		}
		// No resolution conversion for UPnP elements
		s = sourceImageInfo.getResResolution();
		if (s != null) {
			addAttribute(sb, "resolution", s);
		}
		if (!convert) {
			s = sourceImageInfo.getResColorDepth();
			if (s != null) {
				addAttribute(sb, "colorDepth", s);
			}
		}
	}

	/**
	 * returns whether or not this image {@code res} element's source is a
	 * (cached) thumbnail.
	 * <p>
	 * Note: <b>This can be confusing</b>: This doesn't indicate whether the
	 * {@code res} element is <b>used</b> as a thumbnail, but if the {@code res}
	 * element's source is a thumbnail from UMS' point of view. For low
	 * resolution images (where the resolution is equal or smaller than the
	 * cached thumbnail), the thumbnail source can be used also for the image
	 * itself for increased performance.
	 *
	 * @return {@code true} if the source is a thumbnail, {@code false}
	 *         otherwise.
	 */
	public boolean isThumbnailSource() {
		return thumbnailSource;
	}

	/**
	 * @return The {@link ImageInfo} describing the source {@link Image}
	 *         represented by this instance.
	 */
	public ImageInfo getSourceImageInfo() {
		return sourceImageInfo;
	}

	@Override
	public Long getSize() {
		if (
			convert ||
			sourceImageInfo == null ||
			sourceImageInfo.getSize() < 1
		) {
			return (long) UNKNOWN;
		}
		return sourceImageInfo.getSize();
	}

	/**
	 * @return The resolution of the image represented by this {@code res}
	 *         element or {@code null} if unknown.
	 */
	public Dimension getResolution() {
		if (
			sourceImageInfo == null ||
			sourceImageInfo.getWidth() < 1 ||
			sourceImageInfo.getHeight() < 1
		) {
			return null;
		}
		return new Dimension(sourceImageInfo.getWidth(), sourceImageInfo.getHeight());
	}


	/**
	 * @return The {@link ImageFormat} this {@link UPnPImageResElement}
	 *         represents.
	 */
	public ImageFormat getFormat() {
		return format;
	}


	/**
	 * @return Whether or not the source image must be converted for this
	 *         {@link UPnPImageResElement}.
	 */
	public boolean isConvert() {
		return convert;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((format == null) ? 0 : format.hashCode());
		result = prime * result + ((sourceImageInfo == null) ? 0 : sourceImageInfo.hashCode());
		result = prime * result + (thumbnailSource ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof UPnPImageResElement)) {
			return false;
		}
		UPnPImageResElement other = (UPnPImageResElement) obj;
		if (format != other.format) {
			return false;
		}
		if (sourceImageInfo == null) {
			if (other.sourceImageInfo != null) {
				return false;
			}
		} else if (!sourceImageInfo.equals(other.sourceImageInfo)) {
			return false;
		}
		if (thumbnailSource != other.thumbnailSource) {
			return false;
		}
		return true;
	}
}
