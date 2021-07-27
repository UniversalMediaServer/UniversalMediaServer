package net.pms.dlna;

import net.pms.network.DbIdResourceLocator.DbidMediaType;

public class DbidTypeAndIdent {

	public final DbidMediaType type;
	public final String ident;

	public DbidTypeAndIdent(DbidMediaType type, String ident) {
		super();
		this.type = type;
		this.ident = ident;
	}
}
