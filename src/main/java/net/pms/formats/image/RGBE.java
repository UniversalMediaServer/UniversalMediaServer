package net.pms.formats.image;

/**
 * A representation of the former Radiance HDR now known as RGBE format.
 *
 * @author Nadahar
 */
public class RGBE extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.RGBE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"hdr",
			"rad",
			"rgbe",
			"xyze"
		};
	}

	@Override
	public String mimeType() {
		return "image/vnd.radiance";
	}
}
