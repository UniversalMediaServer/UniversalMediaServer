package net.pms.util;


import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Pattern;

public class CodeDb implements DbHandler{

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeDb.class);
	public static final String MASTER = "MASTER_CODE";

	private FileDb db;

	public CodeDb() {
		db = new FileDb(PMS.getConfiguration().getProfileDirectory() + File.separator + name(), this);
		db.setMinCnt(2);
		db.init();
	}

	public String getCode(String obj) {
		for (String key : db.keys()) {
			if(key.equals(MASTER)) {
				continue;
			}
			if (Pattern.matches(key, obj)) {
				 return key;
			}
		}
		return null;
	}

	public String getCode(DLNAResource r) {
		String res = getCode(r.getName());
		if(StringUtils.isEmpty(res)) {
			res = getCode(r.getSystemName());
		}
		return res;
	}

	public String lookup(String key) {
		return (String)db.get(key);
	}


	@Override
	public Object create(String[] args) {
		return args[1];
	}

	@Override
	public String[] format(Object obj) {
		return new String[] { (String)obj };
	}

	@Override
	public String name() {
		return "UMS.code";
	}
}
