package net.pms.formats.image;

/**
 * A representation of the Truevision Targa Graphic file format.
 *
 * @author Nadahar
 */
public class TGA extends ImageBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.TGA;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"tga",
			"icb",
			"vda",
			"vstrle"
		};
	}

	@Override
	public String mimeType() {
		/*
		 * application/tga,
		 * application/x-tga,
		 * application/x-targa,
		 * image/tga,
		 * image/x-tga,
		 * image/targa,
		 * image/x-targa
		 */
		return "image/x-tga";
	}

}
