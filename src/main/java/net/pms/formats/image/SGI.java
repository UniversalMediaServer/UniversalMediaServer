package net.pms.formats.image;

/**
 * A representation of the Silicon Graphics SGI image format, also known as RLE
 * or Haeberli.
 *
 * @author Nadahar
 */
public class SGI extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.SGI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"sgi",
			"rle"
		};
	}

	@Override
	public String mimeType() {
		/*
		 * image/sgi,
		 * image/x-sgi,
		 * image/x-sgi-rgba
		 */
		return "image/sgi";
	}
}
