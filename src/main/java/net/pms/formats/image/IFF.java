package net.pms.formats.image;

/**
 * A representation of the Interchange File Format.
 *
 * @author Nadahar
 */
public class IFF extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.IFF;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"iff"
		};
	}

	@Override
	public String mimeType() {
		/*
		 * application/iff
		 * application/x-iff
		 * image/iff
		 * image/x-iff
		 */
		return "image/iff";
	}
}
