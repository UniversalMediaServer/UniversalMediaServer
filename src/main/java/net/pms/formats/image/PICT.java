package net.pms.formats.image;


/**
 * A representation of the Apple PICT format.
 *
 * @author Nadahar
 */
public class PICT extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.PICT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"pict",
			"pct",
			"pic"
		};
	}

	@Override
	public String mimeType() {
		return "image/x-pict";
	}
}
