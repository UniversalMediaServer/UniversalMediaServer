/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * package net.pms.formats;

public class BMP extends JPG {
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public Identifier getIdentifier() {
		return Identifier.BMS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedExtensions() {
		return new String[] { "bms" };
	}

	@Override
	public String mimeType() {
		return "image/x-bms";
	}
}
