package net.pms.formats;

public class BMP extends JPG {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.MPO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] { "mpo" };
	}

	@Override
	public String mimeType() {
		return "image/mpo";
	}
}
