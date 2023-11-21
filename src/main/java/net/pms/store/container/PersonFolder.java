package net.pms.store.container;

import net.pms.renderers.Renderer;
import net.pms.store.DbIdTypeAndIdent;

public class PersonFolder extends VirtualFolderDbIdNamed {

	public PersonFolder(Renderer renderer, String personName, DbIdTypeAndIdent typeIdent) {
		super(renderer, personName, typeIdent);
	}
}
