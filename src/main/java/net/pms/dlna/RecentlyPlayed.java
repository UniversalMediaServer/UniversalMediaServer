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
import java.util.Date;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecentlyPlayed extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecentlyPlayed.class);
	private static final int MAX_LIST_SIZE = 250;
	private static final int DEF_LIST_SIZE = 50;
	private List<DLNAResource> list;

	public RecentlyPlayed() {
		super(Messages.getString("VirtualFolder.1"), "images/thumbnail-folder-256.png");
		list = Collections.synchronizedList(new ArrayList<DLNAResource>());
		parseLastFile();
	}

	private File lastFile() {
		return new File(PMS.getConfiguration().getDataFile("UMS.last"));
	}

	private ExternalListener findLastPlayedParent(String className) {
		for (ExternalListener l : ExternalFactory.getExternalListeners()) {
			if (className.equals(l.getClass().getName())) {
				return l;
			}
		}
		return null;
	}

	private Player findLastPlayer(String playerName) {
		for (Player p : PlayerFactory.getPlayers()) {
			if (playerName.equals(p.name())) {
				return p;
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
		if (res instanceof VirtualVideoAction) {
			// don't add these
			return;
		}
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
				res1.setMediaSubtitle(res.getMediaSubtitle());
				res1.setResume(res.getResume());
			} else {
				res1 = res.clone();
				res1.setMediaSubtitle(res.getMediaSubtitle());
				res1.setResume(res.getResume());
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
			// addchild might clear the masterparent
			// so fetch it first and readd
			ExternalListener master = r.getMasterParent();
			addChild(r);
			r.setMasterParent(master);
			if (r.isResume()) {
				// add this non resume after
				DLNAResource clone = r.clone();
				clone.setResume(null);
				addChild(clone);
				clone.setMasterParent(master);
			}
		}
	}

	public List<DLNAResource> getList() {
		return list;
	}

	public void update() {
		try {
			getChildren().clear();
			setDiscovered(false);
			dumpFile();
			getChildren().clear();
			setDiscovered(false);
		} catch (IOException e) {
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
					int pos = str.indexOf(';');
					if (pos == -1) {
						continue;
					}
					String master = str.substring(0, pos);
					str = str.substring(pos + 1);
					pos = str.indexOf(';');
					String subData = null;
					String resData = null;
					DLNAResource res = null;
					Player player = null;
					while (pos != -1) {
						if (str.startsWith("player:")) {
							// find last player
							player = findLastPlayer(str.substring(7, pos));
						}
						if (str.startsWith("resume")) {
							// resume data
							resData = str.substring(6, pos);
						}
						if (str.startsWith("sub")) {
							// subs data
							subData = str.substring(3, pos);
						}
						str = str.substring(pos + 1);
						pos = str.indexOf(';');
					}
					LOGGER.debug("master is " + master + " str " + str);
					ExternalListener lpp;
					if (master.startsWith("internal:")) {
						res = parseInternal(master.substring(9), str);
					} else {
						lpp = findLastPlayedParent(master);
						if (lpp != null) {
							res = resolveCreateMethod(lpp, str);
							if (res != null) {
								LOGGER.debug("set masterparent for " + res + " to " + lpp);
								res.setMasterParent(lpp);
							}
						}
					}
					if (res != null) {
						if (resData != null) {
							ResumeObj r = new ResumeObj(new File(resData));
							if (!r.isDone()) {
								r.read();
								res.setResume(r);
							}
						}
						res.setPlayer(player);
						if (subData != null) {
							DLNAMediaSubtitle s = res.getMediaSubtitle();
							if (s == null) {
								s = new DLNAMediaSubtitle();
								res.setMediaSubtitle(s);
							}
							String[] tmp = subData.split(",");
							s.setLang(tmp[0]);
							subData = tmp[1];
							if (subData.startsWith("file:")) {
								String sFile = subData.substring(5);
								s.setExternalFile(new File(sFile));
								s.setId(100);
								SubtitleType t = SubtitleType.valueOfFileExtension(FileUtil.getExtension(sFile));
								s.setType(t);
							} else if (subData.startsWith("id:")) {
								s.setId(Integer.parseInt(subData.substring(3)));
							}
						}
						list.add(res);
					}
				}
			}
			dumpFile();
		} catch (IOException | NumberFormatException e) {
		}
	}

	private void dumpFile() throws IOException {
		File f = lastFile();
		Date now = new Date();
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			sb.append("## Generated: ");
			sb.append(now.toString());
			sb.append("\n");
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
					if (r.getPlayer() != null) {
						sb.append("player:").append(r.getPlayer().toString()).append(";");
					}
					if (r.isResume()) {
						sb.append("resume");
						sb.append(r.getResume().getResumeFile().getAbsolutePath());
						sb.append(";");
					}
					if (r.getMediaSubtitle() != null) {
						DLNAMediaSubtitle sub = r.getMediaSubtitle();
						if (sub.getLang() != null
							&& sub.getId() != -1) {
							sb.append("sub");
							sb.append(sub.getLang());
							sb.append(",");
							if (sub.isExternal()) {
								sb.append("file:");
								sb.append(sub.getExternalFile().getAbsolutePath());
							} else {
								sb.append("id:");
								sb.append("").append(sub.getId());
							}
							sb.append(";");
						}
					}
					sb.append(data);
					sb.append("\n");
				}
			}
			out.write(sb.toString());
			out.flush();
		}
	}

	private DLNAResource parseInternal(String clazz, String data) {
		boolean error = false;
		if (clazz.contains("RealFile")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				return new RealFile(new File(tmp[1]), tmp[0]);
			}
			error = true;
		}
		if (clazz.contains("SevenZipEntry")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				long len = Long.parseLong(tmp[2]);
				return new SevenZipEntry(new File(tmp[1]), tmp[0], len);
			}
			error = true;
		}
		if (clazz.contains("ZippedEntry")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				long len = Long.parseLong(tmp[2]);
				return new ZippedEntry(new File(tmp[1]), tmp[0], len);
			}
			error = true;
		}
		if (clazz.contains("WebStream")) {
			if (data.contains(">")) {
				String[] tmp = data.split(">");
				int type;
				try {
					type = Integer.parseInt(tmp[3]);
				} catch (NumberFormatException e) {
					type = Format.UNKNOWN;
				}
				return new WebStream(tmp[0], tmp[1], tmp[2], type);
			}
			error = true;
		}

		if (error) {
			LOGGER.debug("parseInternal() received some bad data:");
			LOGGER.debug("clazz: " + clazz);
			LOGGER.debug("data:" + data);
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
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
		}
		return null;
	}
}
