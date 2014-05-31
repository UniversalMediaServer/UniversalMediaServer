package net.pms.remote;

import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class RemoteServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServer.class);

	public static ArrayList<RemoteServer> parse() throws Exception {
		ArrayList<RemoteServer> res=new ArrayList<RemoteServer>();
		File f=new File((String) PMS.getConfiguration().getProfileDirectory()+File.separator+"UMS.srv");
		BufferedReader in = new BufferedReader(new FileReader(f));
		String str;
		RemoteServer srv=null;
		int line=0;
		while ((str = in.readLine()) != null) {
			line++;
			str=str.trim();
			if(RemoteUtil.ignoreLine(str))
				continue;
			if(str.startsWith("{")) {
				if(srv!=null) // syntax error, throw exception
					throw new Exception("Syntax error in remote srv file @ line "+line);
				srv=new RemoteServer();
			}
			if(str.startsWith("}")) {
				if(srv==null) // error, but we are nice skip this
					continue;
				LOGGER.debug("add "+srv.toString());
				res.add(srv);
				srv=null;
			}
			if(srv==null) // error here skip and hope to get in sync again
				continue;
			if(str.startsWith("name="))
				srv.setDispName(str.substring(5));
			if(str.startsWith("addr="))
				srv.setAddr(str.substring(5));
			if(str.startsWith("port="))
				srv.setPort(str.substring(5));
			if(str.startsWith("user="))
				srv.setUser(str.substring(5));
			if(str.startsWith("pwd="))
				srv.setPwd(str.substring(4));
			if(str.startsWith("https"))
				srv.useHTTPS();
		}
		return res;
	}

	private String usr;
	private String pwd;
	private String addr;
	private int port;
	private DLNAResource root;
	private String name;
	private String scheme;

	public RemoteServer() {
		this(RemoteWeb.DEFAULT_PORT);
	}

	public RemoteServer(int port) {
		if (port == 0) {
			port = RemoteWeb.DEFAULT_PORT;
		}
		this.port = port;
		scheme = "http://";
	}

	public void setDispName(String n) {
		name=n;
	}

	public void setUser(String s) {
		usr=s;
	}

	public void setPwd(String s) {
		pwd=s;
	}

	public void setAddr(String s) {
		addr=s;
	}

	public void setPort(String s) {
		try {
			int rPort=Integer.parseInt(s);
			port=rPort;
		}
		catch (Exception e) {
		}
	}

	public void useHTTPS() {
		scheme = "https://";
	}

	public String getDispName() {
		return name;
	}

	public String dlnaAction(String path) throws Exception {
		URL u=new URL(url(path));
		URLConnection uc=u.openConnection();
		uc.setDoOutput(true);
		uc.setDoInput(true);
		BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		StringBuilder page=new StringBuilder();
		String str;
		LOGGER.trace("start read");
		while ((str = in.readLine()) != null) {
			//      page.append("\n");
			page.append(str.trim());
			page.append("\n");
		}
		in.close();
		LOGGER.trace("got out "+page.toString());
		return page.toString();
	}

	public String streamURL(String id) {
		return url("media/" + id);
	}

	public String url(String id) {
		return scheme+addr+":"+port+"/srv/"+id;
	}

}
