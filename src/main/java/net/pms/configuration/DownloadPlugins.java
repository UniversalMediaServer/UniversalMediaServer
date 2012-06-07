package net.pms.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import net.pms.PMS;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.newgui.GeneralTab;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadPlugins {
	
	private final static String PLUGIN_LIST_URL="file:///tst.txt";
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadPlugins.class);
	
	private static final int TYPE_JAR=0;
	private static final int TYPE_LIST=1;
	private static final int TYPE_BUNDLE=2;
	
	private String name;
	private String rating;
	private String desc;
	private String url;
	private String author;
	private int type;
	private ArrayList<URL> jars;
	
	public static ArrayList<DownloadPlugins> downloadList() {
		ArrayList<DownloadPlugins> res=new ArrayList<DownloadPlugins>();
		try {
			URL u=new URL(PLUGIN_LIST_URL);
			URLConnection connection=u.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		    String str;
			DownloadPlugins plugin=new DownloadPlugins();
		    while ((str = in.readLine()) != null) {
		    	str=str.trim();
				if(StringUtils.isEmpty(str)) {
					if(plugin.isOk())
						res.add(plugin);
					plugin=new DownloadPlugins();
				}
				String[] keyval=str.split("=",2);
				if(keyval.length<2)
					continue;
				if(keyval[0].equalsIgnoreCase("name"))
					plugin.name=keyval[1];
				if(keyval[0].equalsIgnoreCase("rating"))
					plugin.rating=keyval[1];
				if(keyval[0].equalsIgnoreCase("desc"))
					plugin.desc=keyval[1];
				if(keyval[0].equalsIgnoreCase("url"))
					plugin.url=keyval[1];	
				if(keyval[0].equalsIgnoreCase("author"))
					plugin.author=keyval[1];	
				if(keyval[0].equalsIgnoreCase("type")) {
					if(keyval[1].equalsIgnoreCase("jar"))
						plugin.type=DownloadPlugins.TYPE_JAR;
					if(keyval[1].equalsIgnoreCase("list"))
						plugin.type=DownloadPlugins.TYPE_LIST;
					if(keyval[1].equalsIgnoreCase("bundle"))
						plugin.type=DownloadPlugins.TYPE_BUNDLE;
				}
			}
		    if(plugin.isOk()) // add the last one
				res.add(plugin);
		    in.close();
		} catch (Exception e) {
			LOGGER.debug("bad plugin list "+e);
		}
		return res;
	}
	public DownloadPlugins() {
		type=DownloadPlugins.TYPE_JAR;
		rating="--";
		jars=null;
	}
	
	public String getName() {
		return name;
	}
	
	public String getRating() {
		return rating;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public boolean isOk() {
		// we must have a name and an url
		return (!StringUtils.isEmpty(name))&&(!StringUtils.isEmpty(url));
	}
	
	private	String splitString(String string) {
		StringBuffer buf = new StringBuffer();
		String tempString = string;	

		if (string != null) {
			while(tempString.length() > 60) {  
				String block = tempString.substring(0, 60);                                           
				int index = block.lastIndexOf(' ');                                             
				if(index < 0) {
					index = tempString.indexOf(' ');
				}
				if (index >= 0){
					buf.append(tempString.substring(0, index) + "<BR>");                                        
				} 
				tempString = tempString.substring(index+1);
			}
		}
		else {
			tempString = " ";
		}
		buf.append(tempString);
		return buf.toString();
	}
	
	private String header(String hdr) {
		return "<br><b>"+hdr+":  </b>";
	}
	
	public String htmlString() {
		String res="<html>";
		res+="<b>Name:  </b>"+getName();
		if(!StringUtils.isEmpty(getRating()))
			res+=header("Rating")+getRating();
		if(!StringUtils.isEmpty(getAuthor()))
			res+=header("Author")+getAuthor();
		if(!StringUtils.isEmpty(getDescription()))
			res+=header("Description")+splitString(getDescription());
		return res;
	}
	
	private String extractFileName(String str) {
		int pos=str.lastIndexOf("/");
		if(pos==-1)
			return name;
		return str.substring(pos+1);
	}
	
	private void ensureCreated(String p) {
		File f=new File(p);
		f.mkdirs();
	}
	
	private boolean downloadFile(String url,String dir) throws Exception {
		URL u=new URL(url);
		ensureCreated(dir);
		File f=new File(dir+File.separator+extractFileName(url));
		URLConnection connection=u.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		InputStream in=connection.getInputStream();
		FileOutputStream out=new FileOutputStream(f);
		byte[] buf = new byte[4096];
		int len;
		while((len=in.read(buf))!=-1)
			out.write(buf, 0, len);
		out.flush();
		out.close();
		in.close();
		// if we got down here add the jar to the list (if it is a jar)
		if(f.getAbsolutePath().endsWith(".jar"))	
			jars.add(f.toURI().toURL());
		return true;
	}
	
	private boolean downloadList(String url) throws Exception {
		URL u=new URL(url);
		URLConnection connection=u.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    String str;
	    boolean res=true;
	    while ((str = in.readLine()) != null) {
	    	str=str.trim();
	    	if(StringUtils.isEmpty(str))
	    		continue;
	    	String[] tmp=str.split(",",2);
	    	String dir=PMS.getConfiguration().getPluginDirectory();
	    	if(tmp.length>1) {
	    		String rootDir=new File("").getAbsolutePath();
	    		if(tmp[1].equalsIgnoreCase("root"))
	    			dir=rootDir;
	    		else {
	    			dir=rootDir+File.separator+tmp[1];
	    		}	    		
	    	}
	    	res&=downloadFile(tmp[0],dir);
	    }
	    return res;
	}
	
	private boolean download() throws Exception {
		if(type==DownloadPlugins.TYPE_JAR)
			return downloadFile(url,PMS.getConfiguration().getPluginDirectory());
		if(type==DownloadPlugins.TYPE_LIST)
			return downloadList(url);
		return false;
	}
	
	public boolean install() throws Exception {
		LOGGER.debug("install plugin "+name+" type "+type);
		// init the jar file list
		jars=new ArrayList<URL>();
		// 1st download the
		if(!download()) // download failed, bail out
			return false;
		// 2nd load the jars (if any)
		if(jars.isEmpty())
			return true;
		URL[] jarURLs = new URL[jars.size()];
		jars.toArray(jarURLs);
		ExternalFactory.loadJARs(jarURLs,true);
		// Finally create the instaces of the plugins
		ExternalFactory.instantiateDownloaded();
		return true;
	}
}
