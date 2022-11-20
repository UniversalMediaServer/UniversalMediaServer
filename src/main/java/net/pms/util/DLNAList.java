/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
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
