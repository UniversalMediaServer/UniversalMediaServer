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

import java.io.File;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;

public class CodeDb implements DbHandler {
	public static final String MASTER = "MASTER_CODE";

	private final FileDb db;

	public CodeDb() {
		db = new FileDb(PMS.getConfiguration().getProfileDirectory() + File.separator + name(), this);
		db.setMinCnt(2);
		db.init();
	}

	public String getCode(String obj) {
		for (String key : db.keys()) {
			if (key.equals(MASTER)) {
				continue;
			}
			if (Pattern.matches(key, obj)) {
				return key;
			}
		}
		return null;
	}

	public String getCode(StoreResource r) {
		String res = getCode(r.getName());
		if (StringUtils.isEmpty(res)) {
			res = getCode(r.getSystemName());
		}
		return res;
	}

	public String lookup(String key) {
		return (String) db.get(key);
	}

	@Override
	public Object create(String[] args) {
		return args[1];
	}

	@Override
	public String[] format(Object obj) {
		return new String[]{(String) obj};
	}

	@Override
	public final String name() {
		return "UMS.code";
	}
}
