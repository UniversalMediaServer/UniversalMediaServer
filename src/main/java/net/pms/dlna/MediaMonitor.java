package net.pms.dlna;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.dlna.virtual.VirtualFolder;

public class MediaMonitor extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);
	private File[] dirs;
	private ArrayList<String> oldEntries=new ArrayList<String>();
	private ArrayList<String> newEntries=new ArrayList<String>();
	
	public MediaMonitor(File[] dirs) {
		super("New Media",null);
		this.dirs=dirs;
		ArrayList<String> oldEntries=new ArrayList<String>();
		ArrayList<String> newEntries=new ArrayList<String>();
		parseMonitorFile();
		scan();
	}
	
	
	public void scan() {
		for(int i=0;i<dirs.length;i++) {
			File f=dirs[i];
			LOGGER.debug("scan dir "+f.getAbsolutePath());
			scanDir(f.listFiles());
		}
	}
	
	private void scanDir(File[] files) {
		for(int i=0;i<files.length;i++) {
			File f=files[i];
			if(f.isFile()) {
				// regular file
				LOGGER.debug("file "+f+" is old? "+oldEntries.contains(f.getAbsoluteFile()));
				if(!oldEntries.contains(f.getAbsolutePath()))
					newEntries.add(f.getAbsolutePath());
				continue;
			}
			if(f.isDirectory()) {
			
			}
		}
	}
	
	private File monitorFile() {
		return new File("UMS.mon");
	}
	
	private void parseMonitorFile() {
		File f=monitorFile();
		if(!f.exists())
			return;
		try {
			BufferedReader in=new BufferedReader(new FileReader(f));
			String str;    	
			
			while ((str = in.readLine()) != null) {
				if(StringUtils.isEmpty(str))
					continue;
				str=str.trim();	
				if(str.startsWith("#"))
					continue;
				if(str.startsWith("entry=")) {
					String entry=str.substring(6);
					if(!new File(entry.trim()).exists())
						continue;
					if(!oldEntries.contains(entry.trim()))
						oldEntries.add(entry.trim());
				}
			}
			in.close();
			dumpFile();
		} catch (Exception e) {
		}
		
	}
	
	public void discoverChildren() {
		for(String s : newEntries)
			addChild(new RealFile(new File(s)));
	}
	
	public boolean isRefreshNeeded() {
		return true;
	}
	
	public void stopped(DLNAResource res) {
		if(!(res instanceof RealFile)) 
			return;
		RealFile rf=(RealFile)res;
		String file=rf.getFile().getAbsolutePath();
		if(!newEntries.contains(file))
			return;
		newEntries.remove(file);
		oldEntries.add(file);
		setDiscovered(false);
		getChildren().clear();
		try {
			dumpFile();
		} catch (IOException e) {
		}
	}
	
	private void dumpFile() throws IOException {
		File f=monitorFile();
		FileWriter out=new FileWriter(f);
		StringBuffer sb=new StringBuffer();
		sb.append("######\n");
		sb.append("## NOTE!!!!!\n");
		sb.append("## This file is auto generated\n");
		sb.append("## Edit with EXTREME care\n");
		for(String s : oldEntries) { 
			sb.append("entry=");
			sb.append(s);
			sb.append("\n");
		}
		out.write(sb.toString());
		out.flush();
		out.close();
	}

}
