package net.pms.dlna;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.RendererConfiguration;
import net.pms.util.OpenSubtitle;

public class PlaySub extends DLNAResource  {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaySub.class);
	private String lang;
	private String url;
	private RealFile real;
	private String name;
	private File subFile;
	
	public PlaySub(String name,String lang,RealFile real,String url) {
		this.name=name;
		this.real=real;
		this.url=url;
		this.lang=lang;
		this.subFile=new File(OpenSubtitle.subFile(name+"_"+lang));
	}
	
	private void getSubFile() throws FileNotFoundException, IOException {
		if(subFile.exists())
			return;
		OpenSubtitle.fetchSubs(url,subFile.getAbsolutePath());
	}
	
	public void setSub()  {
		try {
			getSubFile();
		} catch (FileNotFoundException e) {
			LOGGER.info("Failed to get subs for "+real.getDisplayName());
			return;
		} catch (IOException e) {
			LOGGER.info("Failed to get subs for "+real.getDisplayName());
			return;
		}
		DLNAMediaSubtitle sub=new DLNAMediaSubtitle();
		sub.setFile(subFile);
		sub.setType(DLNAMediaSubtitle.SUBRIP);
		sub.setId(1);
		sub.setLang(lang);
		real.setMediaSubtitle(sub);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		setSub();
		return real.getInputStream();
	}
	
	 public InputStream getInputStream(Range range, RendererConfiguration mediarenderer) throws IOException {
		 setSub();
		 return real.getInputStream(range, mediarenderer);
	 }
	
	public InputStream getThumbnailInputStream() throws IOException {
		try {
			return getResourceInputStream("images/codes/"+lang+".png");
		}
		catch (Exception e) {
		}
		return super.getThumbnailInputStream();
	}

	@Override
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return name;
	}

	@Override
	public String getSystemName() {
		return real.getSystemName();
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public long length() {
		return real.length();
	}
	
	public boolean isStrFile() {
		return true;
	}
}
