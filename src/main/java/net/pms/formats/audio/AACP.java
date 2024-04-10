package net.pms.formats.audio;

public class AACP extends AudioBase {

	public AACP() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.AACP;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] {"aacp"};
	}

	@Override
	public boolean transcodable() {
		return false;
	}
}
