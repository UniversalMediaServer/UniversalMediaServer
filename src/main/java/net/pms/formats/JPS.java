package net.pms.formats;

public class BMP extends JPG {
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.JPS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] { "jps" };
	}

	@Override
	public String mimeType() {
		return "image/jps";
	}
}
