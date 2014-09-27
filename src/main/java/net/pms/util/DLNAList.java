package net.pms.util;


import net.pms.PMS;
import net.pms.dlna.DLNAResource;

import java.util.ArrayList;

public class DLNAList extends ArrayList<DLNAResource> {
	private static final long serialVersionUID = -5775968769790761576L;

	public DLNAResource remove(int index) {
		PMS.getGlobalRepo().remove(get(index));
		return super.remove(index);
	}

	public boolean remove(DLNAResource r) {
		PMS.getGlobalRepo().remove(r);
		return super.remove(r);
	}

	public void clear() {
		for(int i = 0; i < size(); i++) {
			PMS.getGlobalRepo().remove(get(i));
		}
		super.clear();
	}
}
