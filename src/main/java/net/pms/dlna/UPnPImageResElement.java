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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import net.pms.dlna.protocolinfo.ProtocolInfo;
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

	/** The {@link ImageInfo} describing the {@link Image}. */
	protected final ImageInfo imageInfo;

	public UPnPImageResElement(
		ProtocolInfo protocolInfo,
		ImageFormat format,
		DLNAResource resource,
		boolean thumbnailSource
	) {
		this(
			protocolInfo,
			getURL(protocolInfo, format, resource, thumbnailSource),
			resource != null && resource.getMedia() != null ? resource.getMedia().getImageInfo() : null,
			thumbnailSource
		);
	}

	/**
	 * Creates a new instance using the given information.
	 *
	 * @param protocolInfo the {@link ProtocolInfo} to use.
	 * @param url the URL to use.
	 * @param imageInfo the {@link ImageInfo} describing this {@link Image}.
	 * @param thumbnailSource Whether or not the source is a thumbnail, see
	 *            {@link #thumbnailSource}.
	 */
	public UPnPImageResElement(ProtocolInfo protocolInfo, String url, ImageInfo imageInfo, boolean thumbnailSource) {
		super(protocolInfo, url);
		if (!"image".equals(protocolInfo.getMimeType().getType())) {
			throw new IllegalArgumentException("protocolInfo must be an image type");
		}
		this.imageInfo = imageInfo;
		this.thumbnailSource = thumbnailSource;
	}

	@Override
	public void appendResString(StringBuilder sb) {
		if (isNotBlank(url)) { //TODO: (Nad) fix?
			openTag(sb, "res");
			appendResAttributes(sb);
			addAttribute(sb, "protocolInfo", protocolInfo);
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			endTag(sb);
			sb.append(url);
			closeTag(sb, "res");
		}
	}

	/**
	 * This is only an extension of the constructor caused by Java's stupid
	 * requirement that calls to other constructors must be done on the first
	 * line.
	 * <p>
	 * As static methods aren't inheritable, something similar will have to
	 * exist in subclasses.
	 *
	 * @param resource the {@link DLNAResource}.
	 * @param thumbnailSource whether the thumbnail should be used as source.
	 * @return The URL.
	 */
	protected static String getURL(
		ProtocolInfo protocolInfo,
		ImageFormat format,
		DLNAResource resource,
		boolean thumbnailSource
	) {
		if (format == null || format == ImageFormat.SOURCE) {
			if (resource != null && resource.getMedia() != null) {
				if (thumbnailSource) {
					if (resource.getMedia().getThumb() != null && resource.getMedia().getThumb().getFormat() != null) {
						format = resource.getMedia().getThumb().getFormat();
					}
				} else {
					if (resource.getMedia().getImageInfo() != null && resource.getMedia().getImageInfo().getFormat() != null) {
						format = resource.getMedia().getImageInfo().getFormat();
					}
				}
			}
			if (format == null || format == ImageFormat.SOURCE) {
				// Default to JPEG
				format = ImageFormat.JPEG;
			}
		}
		String prefix = "UPnP_" + format.toString();
		String extension = format.getDefaultExtension();
		String result;
		if (thumbnailSource) {
			return resource.getThumbnailURL(prefix, extension);
		} else {
			result = resource.getURL(//TODO: (Nad) Override CI, override extension, set JPEG_RES_H_V
				(DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile()) ?
					"JPEG_RES" + resElement.getWidth() + "x" + resElement.getHeight() :
					resElement.getProfile().getValue()
				) + "_"
			);
		}
	}

	@Override
	protected void appendResAttributes(StringBuilder sb) {
		//TODO: (Nad) Implement
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
	 * @return The {@link ImageInfo} describing the image represented by this
	 *         instance.
	 */
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	@Override
	public Long getSize() {
		if (imageInfo == null || imageInfo.getSize() < 1) {
			return (long) UNKNOWN;
		}
		return imageInfo.getSize();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((imageInfo == null) ? 0 : imageInfo.hashCode());
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
		if (imageInfo == null) {
			if (other.imageInfo != null) {
				return false;
			}
		} else if (!imageInfo.equals(other.imageInfo)) {
			return false;
		}
		if (thumbnailSource != other.thumbnailSource) {
			return false;
		}
		return true;
	}
}
