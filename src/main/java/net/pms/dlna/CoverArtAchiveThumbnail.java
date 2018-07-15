package net.pms.dlna;

import java.awt.color.ColorSpace;
import net.pms.image.ColorSpaceType;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;


public class CoverArtAchiveThumbnail implements DLNAThumbnail {

	private static final long serialVersionUID = 1L;

	/** The {@link ImageInfo} describing this {@link CoverArtAchiveThumbnail} */
	protected final ImageInfo imageInfo;

	/** The {@link DLNAImageProfile} for this {@link CoverArtAchiveThumbnail} */
	protected final DLNAImageProfile profile;

	public CoverArtAchiveThumbnail() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public DLNAImageProfile getDLNAImageProfile() {
		return profile;
	}

	@Override
	public byte[] getBytes(boolean copy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	@Override
	public int getWidth() {
		return imageInfo != null ? imageInfo.getWidth() : -1;
	}

	@Override
	public int getHeight() {
		return imageInfo != null ? imageInfo.getHeight() : -1;
	}

	@Override
	public ImageFormat getFormat() {
		return imageInfo != null ? imageInfo.getFormat() : null;
	}

	@Override
	public ColorSpace getColorSpace() {
		return imageInfo != null ? imageInfo.getColorSpace() : null;
	}

	@Override
	public ColorSpaceType getColorSpaceType() {
		return imageInfo != null ? imageInfo.getColorSpaceType() : null;
	}

	@Override
	public int getBitPerPixel() {
		return imageInfo != null ? imageInfo.getBitsPerPixel() : -1;
	}

	@Override
	public int getNumComponents() {
		return imageInfo != null ? imageInfo.getNumComponents() : -1;
	}

	@Override
	public int getBitDepth() {
		return imageInfo != null ? imageInfo.getBitDepth() : -1;
	}

	@Override
	public boolean isImageIOSupported() {
		return imageInfo != null ? imageInfo.isImageIOSupported() : false;
	}
}
