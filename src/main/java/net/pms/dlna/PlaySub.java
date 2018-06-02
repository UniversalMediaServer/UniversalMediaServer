package net.pms.dlna;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.pms.configuration.RendererConfiguration;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.OpenSubtitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaySub extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlaySub.class);
	private String lang;
	private String url;
	private RealFile real;
	private String name;
	private File subFile;

	public PlaySub(String name, String lang, RealFile real, String url) {
		this.name = name;
		this.real = real;
		this.url = url;
		this.lang = lang;
		this.subFile = new File(OpenSubtitle.subFile(name + "_" + lang));
	}

	private void getSubFile() throws FileNotFoundException, IOException {
		if (subFile.exists()) {
			return;
		}
		OpenSubtitle.fetchSubs(url, subFile.getAbsolutePath());
	}

	public void setSub() {
		try {
			getSubFile();
		} catch (FileNotFoundException e) {
			LOGGER.info("Failed to get subtitles for " + real.getDisplayName());
			return;
		} catch (IOException e) {
			LOGGER.info("Failed to get subtitles for " + real.getDisplayName());
			return;
		}
		DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
		try {
			sub.setExternalFile(subFile, null);
		} catch (FileNotFoundException e) {
			LOGGER.info("Failed to download subtitle file: " + subFile.getName());
			return;
		}
		sub.setType(SubtitleType.SUBRIP);
		sub.setId(1);
		sub.setLang(lang);
		real.setMediaSubtitle(sub);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		setSub();
		return real.getInputStream();
	}

	@Override
	public synchronized InputStream getInputStream(Range range, RendererConfiguration mediarenderer) throws IOException {
		setSub();
		return real.getInputStream(range, mediarenderer);
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		try {
			return DLNAThumbnailInputStream.toThumbnailInputStream(getResourceInputStream("images/codes/" + lang + ".png"));
		} catch (Exception e) {
		}
		return super.getThumbnailInputStream();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
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

	@Override
	public boolean hasExternalSubtitles() {
		return true;
	}
}