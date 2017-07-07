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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.io.Serializable;
import java.util.Comparator;
import net.pms.dlna.protocolinfo.DeviceProtocolInfoOrigin.ProtocolInfoType;
import net.pms.dlna.protocolinfo.ProtocolInfo;
import net.pms.image.ImageFormat;
import net.pms.network.UPNPControl.Renderer;
import static net.pms.util.StringUtil.*;

/**
 * This abstract class is used to represent an UPnP {@code <res>} element in a
 * {@code DIDL-Lite} document.
 *
 * @author Nadahar
 */
public abstract class UPnPResElement implements Comparable<UPnPResElement>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The "{@code null} value" for {@code int} and {@code long} */
	public static int UNKNOWN = -1;
	protected final ProtocolInfo protocolInfo;
	protected final String url;

	public UPnPResElement(ProtocolInfo protocolInfo, String url) {
		if (protocolInfo == null) {
			throw new IllegalArgumentException("protocolInfo cannot be null");
		}
		this.protocolInfo = protocolInfo;
		this.url = isBlank(url) ? "" : url;
	}

	public boolean isDLNA() {
		return protocolInfo.isDLNA();
	}

	@Override
	public String toString() {
		return toResString();
	}

	public String toResString() {
		StringBuilder sb = new StringBuilder();
		appendResString(sb);
		return sb.toString();
	}

	public void appendResString(StringBuilder sb) {
		if (isNotBlank(url)) {
			openTag(sb, "res");
			appendResAttributes(sb);
			addAttribute(sb, "protocolInfo", protocolInfo);
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			endTag(sb);
			sb.append(url);
			closeTag(sb, "res");
		}
	}

	public ProtocolInfo getProtocolInfo() {
		return protocolInfo;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * @return The size in bytes of the media represented by this {@code res}
	 *         element or {@link #UNKNOWN} if unknown.
	 */
	public abstract Long getSize();

	protected abstract void appendResAttributes(StringBuilder sb);

	/**
	 * Checks whether this is supported by {@code renderer} according to
	 * acquired {@link ProtocolInfo} information. If no information is
	 * available, it's considered supported. The reason is that not all
	 * renderers provide this information, in which case all must be assumed
	 * supported as opposed to none.
	 *
	 * @param renderer the {@link Renderer} for which to check supported
	 *            {@link UPnPResElement}s.
	 * @return {@code true} if this is supported or no information is available,
	 *         {@code false} otherwise.
	 */
	public boolean isSupported(Renderer renderer) { //TODO: (Nad) Carefully verify
		if (renderer == null || renderer.deviceProtocolInfo == null || renderer.deviceProtocolInfo.isEmpty(ProtocolInfoType.SINK)) {
			return true;
		}

		for (ProtocolInfo protocolInfoEntry : renderer.deviceProtocolInfo.toArray(ProtocolInfoType.SINK)) {
			if (protocolInfo.isDLNA() == protocolInfoEntry.isDLNA()) {
				if (protocolInfo.isDLNA()) {
					if (protocolInfo.getDLNAProfileName().getValue().equals(protocolInfoEntry.getDLNAProfileName().getValue())) {
						if (protocolInfo.getMimeType().isCompatible(protocolInfoEntry.getMimeType(), false, true)) {
							return true; //TODO: (Nad) Verify
						}
					}
				} else {
					if (protocolInfo.getMimeType().isCompatible(protocolInfoEntry.getMimeType(), false, false)) {
						return true; //TODO: (Nad) Verify
					}
				}
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getClass().hashCode();
		result = prime * result + ((protocolInfo == null) ? 0 : protocolInfo.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		if (!(obj instanceof UPnPResElement)) {
			return false;
		}
		if (!getClass().equals(obj.getClass())) {
			return false;
		}
		UPnPResElement other = (UPnPResElement) obj;
		if (protocolInfo == null) {
			if (other.protocolInfo != null) {
				return false;
			}
		} else if (!protocolInfo.equals(other.protocolInfo)) {
			return false;
		}
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(UPnPResElement other) {
		if (other == null) { //TODO: (Nad) Implement Comparator here instead, handling all subclasses, use ImageInfo for source.
			return -1;
		}
		if (protocolInfo == null && other.protocolInfo != null) {
			return 1;
		}
		if (protocolInfo != null && other.protocolInfo == null) {
			return -1;
		}
		if (protocolInfo != null) {
			if (protocolInfo.isDLNA() != other.protocolInfo.isDLNA()) {
				if (protocolInfo.isDLNA()) {
					return -1;
				}
				return 1;
			}
			int result = protocolInfo.compareTo(other.protocolInfo);
			if (result != 0) {
				return result;
			}
		}
		return url.compareTo(other.url);
	}

	/**
	 * Constructs a {@link Comparator} for sorting image entries of
	 * {@link UPnPResElement} and subclasses by priority with the highest
	 * priority first.
	 *
	 * <b>This method must handle all subclasses as well</b>.
	 *
	 * @param sourceFormat the {@link ImageFormat} of the source image, used
	 *            when determining priorities.
	 * @return The {@link Comparator}.
	 */
	public static Comparator<UPnPResElement> getComparator(ImageFormat sourceFormat) {
		// This defines what format should be preferred depending on the source format
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
		return new Comparator<UPnPResElement>() { //TODO: (Nad) Change type

			@Override
			public int compare(UPnPResElement o1, UPnPResElement o2) {
				if (o1 == null && o2 == null) {
					return 0;
				} else if (o1 == null) {
					return 1;
				} else if (o2 == null) {
					return -1;
				}

				if (o1.isDLNA() != o2.isDLNA()) {
					// DLNA entries sort before UPnP entries
					if (o1.isDLNA()) {
						return -1;
					}
					return 1;
				}
				if (o1.isDLNA()) {
					// DLNA entries
//					if (o1.isThumbnail() != o2.isThumbnail()) {
//						return (o1.isThumbnail() ? 1 : 0) - (o2.isThumbnail() ? 1 : 0);
//					}
//
//					int i =
//						(o1.getCiFlag() == null ? 2 : o1.getCiFlag()) -
//						(o2.getCiFlag() == null ? 2 : o2.getCiFlag());
//					if (i != 0) {
//						return i;
//					}
//
//					ImageFormat o1Format = o1.getProfile() != null ? o1.getProfile().getFormat() : null;
//					ImageFormat o2Format = o2.getProfile() != null ? o2.getProfile().getFormat() : null;
//
//					if (o1Format != o2Format) {
//						if (o1Format == null) {
//							return 1;
//						} else if (o2Format == null) {
//							return -1;
//						}
//						if (o1Format == preferredFormat) {
//							return -1;
//						}
//						if (o2Format == preferredFormat) {
//							return 1;
//						}
//						return o1Format.compareTo(o2Format);
//					}
//
//					if (
//						(DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile()) ||
//						DLNAImageProfile.JPEG_RES_H_V.equals(o2.getProfile())) &&
//						(!DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile()) ||
//						!DLNAImageProfile.JPEG_RES_H_V.equals(o2.getProfile()))
//					) {
//						if (DLNAImageProfile.JPEG_RES_H_V.equals(o1.getProfile())) {
//							return -1;
//						}
//						return 1;
//					}
//
//					if (o1.getWidth() != o2.getWidth()) {
//						return o2.getWidth() - o1.getWidth();
//					}
//
//					if (o1.getHeight() != o2.getHeight()) {
//						return o2.getHeight() - o1.getHeight();
//					}
//
//					if (o1.getProfile() != null || o2.getProfile() != null) {
//						if (o1.getProfile() == null) {
//							return 1;
//						}
//						if (o2.getProfile() == null) {
//							return -1;
//						}
//						if (!o1.getProfile().equals(o2.getProfile())) {
//							return o1.getProfile().toInt() - o2.getProfile().toInt();
//						}
//					}
//
//					long l =
//						(o2.getSize() == null ? 0 : o2.getSize()) -
//						(o1.getSize() == null ? 0 : o1.getSize());
//					if (l != 0) {
//						return (int) l;
//					}
//
//					if (o1.getHypotheticalResult() != null || o2.getHypotheticalResult() != null) {
//						// This comparison serves no practical purpose other than
//						// to fulfill the contract with equals().
//						if (o1.getHypotheticalResult() == null) {
//							return 1;
//						}
//						if (o2.getHypotheticalResult() == null) {
//							return -1;
//						}
//						if (o1.getHypotheticalResult().conversionNeeded != o2.getHypotheticalResult().conversionNeeded) {
//							return
//								(o1.getHypotheticalResult().conversionNeeded ? 1 : 0) -
//								(o2.getHypotheticalResult().conversionNeeded ? 1 : 0);
//						}
//					}
					return 0;
				} else {
					// UPnP entries
					// TODO: (Nad) Sort
					return 0;
				}
			}
		};
	}
}
