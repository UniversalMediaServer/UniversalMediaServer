package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDb {
	private Map<String, Object> db;
	private int minCnt;
	private String sep;
	private File file;
	private DbHandler handler;
	private boolean autoSync;
	private boolean overwrite;

	public FileDb(DbHandler h) {
		this(PMS.getConfiguration().getDataFile(h.name()), h);
	}

	public FileDb(String f, DbHandler h) {
		if (StringUtils.isEmpty(f)) {
			f = "UMS.db";
		}
		file = new File(f);
		handler = h;
		minCnt = 2;
		sep = ",";
		autoSync = true;
		overwrite = false;
		db = new HashMap<>();
	}

	public void setSep(String s) {
		sep = s;
	}

	public void setMinCnt(int c) {
		minCnt = c;
	}

	public void setAutoSync(boolean b) {
		autoSync = b;
	}

	public void setOverwrite(boolean b) {
		overwrite = b;
	}

	public void init() {
		if (!file.exists()) {
			return;
		}
		try {
			BufferedReader in;
			in = new BufferedReader(new FileReader(file));
			String str;
			while ((str = in.readLine()) != null) {
				str = str.trim();
				if (StringUtils.isEmpty(str) || str.startsWith("#")) {
					continue;
				}
				String tmp[] = str.split(sep);
				if (tmp.length < minCnt) {
					continue;
				}
				db.put(tmp[0], handler.create(tmp));
			}
			in.close();
		} catch (Exception e) {
		}
	}

	public void addNoSync(String key, Object obj) {
		if (!overwrite) {
			if (get(key) != null) {
				return;
			}
		}
		db.put(key, obj);
	}

	public void removeNoSync(String key) {
		db.remove(key);
	}

	public void add(String key, Object obj) {
		addNoSync(key, obj);
		if (autoSync) {
			sync();
		}
	}

	public void remove(String key) {
		db.remove(key);
		if (autoSync) {
			sync();
		}
	}

	public Object get(String key) {
		return db.get(key);
	}

	public void sync() {
		try {
			// write a dummy line to make sure the file exists
			try (FileOutputStream out = new FileOutputStream(file)) {
				// write a dummy line to make sure the file exists
				Date now = new Date();
				String data = "#########################\n#### Db file generated " + now.toString() + "\n" +
						"#### Edit with care\n#########################\n";
				out.write(data.getBytes(), 0, data.length());
				for (String key : db.keySet()) {
					Object obj = db.get(key);
					if (obj == null) {
						data = key;
						for (int i = 1; i < minCnt; i++) {
							data = data + sep;
						}
						data = data + "\n";
					} else {
						String[] data1 = handler.format(obj);
						data = key + sep + StringUtils.join(data1, sep) + "\n";
					}
					out.write(data.getBytes(), 0, data.length());
				}
				out.flush();
			}
		} catch (Exception e) {
		}
	}
}
