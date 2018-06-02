package net.pms.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredMgr {
	public static class Credential {
		public String username;
		public String password;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CredMgr.class);
	private HashMap<MultiKey,ArrayList<Credential>> credentials;
	private HashMap<MultiKey,String> tags;
	private File credFile;

	public CredMgr(File f) {
		credentials = new HashMap<>();
		tags = new HashMap<>();
		credFile = f;
		FileWatcher.add(new FileWatcher.Watch(f.getPath(), reloader, this));
		try {
			readFile();
		} catch (IOException e) {
			LOGGER.debug("Error during credfile init " + e);
		}
	}

	public static final FileWatcher.Listener reloader = new FileWatcher.Listener() {
		@Override
		public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir) {
			try {
				((CredMgr)watch.getItem()).readFile();
			} catch (IOException e) {
				LOGGER.debug("Error during credfile init " + e);
			}
		}
	};

	private void readFile() throws IOException{
		// clear all data first, if file is gone so are all creds
		credentials.clear();
		tags.clear();

		if(!credFile.exists()) {
			return;
		}

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(credFile), StandardCharsets.UTF_8))) {
			String str;
			while ((str = in.readLine()) != null) {
				str = str.trim();
				if (StringUtils.isEmpty(str) || str.startsWith("#")) {
					continue;
				}
				String[] s = str.split("\\s*=\\s*", 2);
				if (s.length < 2) {
					continue;
				}
				String[] s1 = s[0].split("\\.", 2);
				String[] s2 = s[1].split(",", 2);
				if (s2.length < 2) {
					continue;
				}
				// s2[0] == usr s2[1] == pwd s1[0] == owner s1[1] == tag
				String tag = "";
				if (s1.length > 1) {
					// there is a tag here
					tag = s1[1];
					// we have a reverse map here for owner+user -> tags
					MultiKey tkey = new MultiKey(s1[0], s2[0]);
					tags.put(tkey, tag);
				}
				MultiKey key = new MultiKey(s1[0], tag);
				Credential val = new Credential();
				val.username = s2[0];
				val.password = s2[1];
				ArrayList<Credential> old = credentials.get(key);
				if(old == null) {
					old = new ArrayList<>();
				}
				old.add(val);
				credentials.put(key, old);
			}
		}
	}

	private MultiKey createKey(String owner, String tag) {
		// ensure that tag is empty string rather than null
		return new MultiKey(owner, (tag == null ? "" : tag));
	}

	public Credential getCred(String owner) {
		return getCred(owner, "");
	}

	public Credential getCred(String owner, String tag) {
		MultiKey key = createKey(owner, tag);
		// this asks for first!!
		ArrayList<Credential> list = credentials.get(key);
		return (list == null ? null : list.get(0));
	}

	public String getTag(String owner, String username) {
		MultiKey key =  new MultiKey(owner, username);
		return tags.get(key);
	}

	public boolean verify(String owner, String user, String pwd) {
		return verify(owner, "", user, pwd);
	}

	public boolean verify(String owner, String tag, String user, String pwd) {
		MultiKey key = createKey(owner, tag);
		ArrayList<Credential> list = credentials.get(key);
		if(list == null) {
			return false;
		}
		for(Credential c : list) {
			if(user.equals(c.username)) {
				// found user compare pwd
				return pwd.equals(c.password);
			}
		}
		// nothing found
		return false;
	}
}
