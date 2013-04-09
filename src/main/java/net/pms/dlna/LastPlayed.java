package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.formats.Format;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LastPlayed extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(LastPlayed.class);
	private static final int MAX_LIST_SIZE = 250;
	private static final int DEF_LIST_SIZE = 50;
	private List<DLNAResource> list;

	public LastPlayed() {
		super("Last Played", null);
		list = Collections.synchronizedList(new ArrayList<DLNAResource>());
		parseLastFile();
	}

	private File lastFile() {
		return new File("UMS.last");
	}

	private ExternalListener findLastPlayedParent(String className) {
		for (ExternalListener l : ExternalFactory.getExternalListeners()) {
			if (className.equals(l.getClass().getName())) {
				return l;
			}
		}
		return null;
	}

	private int getMax() {
		String str = (String) PMS.getConfiguration().getCustomProperty("last_play_limit");
		if (str != null) {
			if (StringUtils.isEmpty(str)) {
				return DEF_LIST_SIZE;
			}
			int tmp = 0;
			try {
				tmp = Integer.parseInt(str);
			} catch (Exception e) {
			}
			if (tmp <= 0) { // use default
				return DEF_LIST_SIZE;
			}
			if (tmp > MAX_LIST_SIZE) {
				return MAX_LIST_SIZE;
			}
			// value seems to be ok, return it
			return tmp;
		}
		return DEF_LIST_SIZE;
	}

	public void add(DLNAResource res) {
		DLNAResource res1;
		LOGGER.debug("add " + res + " to last played " + res.getParent() + " " + this);
		if (res.getParent() == this) {
			res1 = res; // best guess
			for (DLNAResource r : list) {
				if (r.getName().equals(res.getName())
					&& r.getSystemName().equals(res.getSystemName())) {
					res1 = r;
					break;
				}
			}
		} else {
			String data = res.write();
			if (!StringUtils.isEmpty(data)
				&& res.getMasterParent() != null) {
				res1 = resolveCreateMethod(res.getMasterParent(), data);
				res1.setMasterParent(res.getMasterParent());
			} else {
				res1 = res.clone();
			}
		}
		int max = getMax();
		list.remove(res1);
		if (list.size() == max) {
			list.remove(max - 1);
		}
		list.add(0, res1);
		setDiscovered(false);
		getChildren().clear();
		try {
			dumpFile();
		} catch (IOException e) {
			LOGGER.debug("error dumping last file " + e);
		}
	}

	@Override
	public void discoverChildren() {
		for (DLNAResource r : list) {
			addChild(r);
		}
	}

	private void parseLastFile() {
		File f = lastFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new FileReader(f))) {
				String str;

				while ((str = in.readLine()) != null) {
					if (StringUtils.isEmpty(str)) {
						continue;
					}
					if (str.startsWith("#")) {
						continue;
					}
					str = str.trim();
					if (!str.startsWith("master:")) {
						continue;
					}
					str = str.substring(7);
					int pos = str.indexOf(";");
					if (pos == -1) {
						continue;
					}
					String master = str.substring(0, pos);
					str = str.substring(pos + 1);
					LOGGER.debug("master is " + master + " str " + str);
					DLNAResource res = null;
					ExternalListener lpp = null;
					if (master.startsWith("internal:")) {
						res = parseInternal(master.substring(9), str);
					} else {
						lpp = findLastPlayedParent(master);
						if (lpp != null) {
							res = resolveCreateMethod(lpp, str);
						}
					}
					if (res != null) {
						LOGGER.debug("set masterparent for " + res + " to " + lpp);
						res.setMasterParent(lpp);
						list.add(res);
					}
				}
			}
			dumpFile();
		} catch (Exception e) {
		}
	}

	private void dumpFile() throws IOException {
		File f = lastFile();
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			for (DLNAResource r : list) {
				String data = r.write();
				if (!StringUtils.isEmpty(data)) {
					ExternalListener parent = r.getMasterParent();
					String id;
					if (parent != null) {
						id = parent.getClass().getName();
					} else {
						id = "internal:" + r.getClass().getName();
					}
					sb.append("master:").append(id).append(";");
					sb.append(data);
					sb.append("\n");
				}
			}
			out.write(sb.toString());
			out.flush();
		}
	}

	private DLNAResource parseInternal(String clazz, String data) {
		if (clazz.contains("RealFile")) {
			String[] tmp = data.split(">");
			return new RealFile(new File(tmp[1]), tmp[0]);
		}
		if (clazz.contains("SevenZipEntry")) {
			String[] tmp = data.split(">");
			long len = Long.parseLong(tmp[2]);
			return new SevenZipEntry(new File(tmp[1]), tmp[0], len);
		}
		if (clazz.contains("ZippedEntry")) {
			String[] tmp = data.split(">");
			long len = Long.parseLong(tmp[2]);
			return new ZippedEntry(new File(tmp[1]), tmp[0], len);
		}
		if (clazz.contains("WebStream")) {
			String[] tmp = data.split(">");
			int type;
			try {
				type = Integer.parseInt(tmp[3]);
			} catch (NumberFormatException e) {
				type = Format.UNKNOWN;
			}
			return new WebStream(tmp[0], tmp[1], tmp[2], type);
		}
		return null;
	}

	private DLNAResource resolveCreateMethod(ExternalListener l, String arg) {
		Method create;
		try {
			Class<?> clazz = l.getClass();
			create = clazz.getDeclaredMethod("create", String.class);
			return (DLNAResource) create.invoke(l, arg);
		// Ignore all errors
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) { }
		return null;
	}
}
