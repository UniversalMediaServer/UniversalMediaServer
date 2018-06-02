package net.pms.formats;

public class OGG extends Format {

	@Override
	public Identifier getIdentifier() {
		return Identifier.OGG;
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	public OGG() {
		type = VIDEO;
	}

	@Override
	public String[] getSupportedExtensions() {
		return new String[] {
			"ogg",
			"ogm",
			"ogv"
		};
	}
}
