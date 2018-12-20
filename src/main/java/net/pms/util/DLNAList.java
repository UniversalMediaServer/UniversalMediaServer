package net.pms.util;

import java.util.ArrayList;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;

public class DLNAList extends ArrayList<DLNAResource> {
	private static final long serialVersionUID = -5775968769790761576L;

	@Override
	public boolean add(DLNAResource r) {
		PMS.getGlobalRepo().setScope(r, true);
		return super.add(r);
	}

	@Override
	public void add(int index, DLNAResource r) {
		PMS.getGlobalRepo().setScope(r, true);
		super.add(index, r);
	}

	@Override
	public DLNAResource remove(int index) {
		PMS.getGlobalRepo().setScope(get(index), false);
		return super.remove(index);
	}

	public boolean remove(DLNAResource r) {
		PMS.getGlobalRepo().setScope(r, false);
		return super.remove(r);
	}

	@Override
	public void clear() {
		for (DLNAResource my : this) {
			PMS.getGlobalRepo().setScope(my, false);
		}
		super.clear();
	}
}
