package net.pms.dlna;

import net.pms.network.DbIdResourceLocator.DbidMediaType;

public class DbidTypeAndIdent {

	/**
	 * Media type
	 */
	public final DbidMediaType type;

	/**
	 * resource to identify
	 */
	public final String ident;

	public DbidTypeAndIdent(DbidMediaType type, String ident) {
		super();
		this.type = type;
		if (ident == null) {
			this.ident = "";
		} else {
			this.ident = ident;
		}
	}
}
