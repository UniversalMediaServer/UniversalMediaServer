package net.pms;

import java.util.ArrayList;
import net.pms.dlna.DLNAResource;
import net.pms.newgui.StatusTab;

public class ServingMediaList extends ArrayList<DLNAResource> {
	private static final long serialVersionUID = -3950603478982912864L;

	public boolean remove(DLNAResource r) {
		if (super.remove(r)) {
			StatusTab.refreshServingTable();
			return true;
		} else {
			return false;
		}
	}

	public boolean add(DLNAResource r) {
		if (super.add(r)) {
			StatusTab.refreshServingTable();
			return true;
		} else {
			return false;
		}
	}
}
