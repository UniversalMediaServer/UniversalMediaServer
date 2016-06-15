package net.pms.util;

import java.util.ArrayList;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;

public class DLNAList extends ArrayList<DLNAResource> {
	private static final long serialVersionUID = -5775968769790761576L;

	@Override
	public DLNAResource remove(int index) {
		PMS.getGlobalRepo().remove(get(index));
		return super.remove(index);
	}

	public boolean remove(DLNAResource r) {
		PMS.getGlobalRepo().remove(r);
		return super.remove(r);
	}

	@Override
	public void clear() {
		for (DLNAResource my : this) {
			PMS.getGlobalRepo().remove(my);
		}
		super.clear();
	}
}
