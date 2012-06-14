/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.pms.PMS;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;
import net.pms.util.ImdbUtil;
import net.pms.util.OpenSubtitle;
import net.pms.util.ProcessUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealFile extends MapFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealFile.class);

	public RealFile(File file) {
		getConf().getFiles().add(file);
		setLastmodified(file.lastModified());
	}

	public RealFile(File file, String name) {
		getConf().getFiles().add(file);
		getConf().setName(name);
		setLastmodified(file.lastModified());
	}

	@Override
	// FIXME: this is called repeatedly for invalid files e.g. files MediaInfo can't parse
	public boolean isValid() {
		File file = this.getFile();
		checktype();
		if (getType() == Format.VIDEO && file.exists() && PMS.getConfiguration().getUseSubtitles() && file.getName().length() > 4) {
			setSrtFile(FileUtil.doesSubtitlesExists(file, null));
		}
		boolean valid = file.exists() && (getExt() != null || file.isDirectory());

		if (valid && getParent().getDefaultRenderer() != null && getParent().getDefaultRenderer().isMediaParserV2()) {
			// we need to resolve the dlna resource now
			run();
			if (getMedia() != null && getMedia().getThumb() == null && getType() != Format.AUDIO) // MediaInfo retrieves cover art now
			{
				getMedia().setThumbready(false);
			}
			if (getMedia() != null && (getMedia().isEncrypted() || getMedia().getContainer() == null || getMedia().getContainer().equals(DLNAMediaLang.UND))) {
				// fine tuning: bad parsing = no file !
				valid = false;
				if (getMedia().isEncrypted()) {
					LOGGER.info("The file " + file.getAbsolutePath() + " is encrypted. It will be hidden");
				} else {
					LOGGER.info("The file " + file.getAbsolutePath() + " was badly parsed. It will be hidden");
				}
			}
			if (getParent().getDefaultRenderer().isMediaParserV2ThumbnailGeneration()) {
				checkThumbnail();
			}
		}
		if(valid)
			resolveImdb();
		return valid;
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new FileInputStream(getFile());
		} catch (FileNotFoundException e) {
			LOGGER.debug("File not found: \"" + getFile().getAbsolutePath() + "\"");
		}
		return null;
	}

	@Override
	public long length() {
		if (getPlayer() != null && getPlayer().type() != Format.IMAGE) {
			return DLNAMediaInfo.TRANS_SIZE;
		} else if (getMedia() != null && getMedia().isMediaparsed()) {
			return getMedia().getSize();
		}
		return getFile().length();
	}

	public boolean isFolder() {
		return getFile().isDirectory();
	}

	public File getFile() {
		return getConf().getFiles().get(0);
	}

	@Override
	public String getName() {
		if (this.getConf().getName() == null) {
			String name = null;
			File file = getFile();
			if (file.getName().trim().equals("")) {
				if (PMS.get().isWindows()) {
					name = PMS.get().getRegistry().getDiskLabel(file);
				}
				if (name != null && name.length() > 0) {
					name = file.getAbsolutePath().substring(0, 1) + ":\\ [" + name + "]";
				} else {
					name = file.getAbsolutePath().substring(0, 1);
				}
			} else {
				name = file.getName();
			}
			this.getConf().setName(name);
		}
		return ImdbUtil.cleanName(this.getConf().getName());
	}

	@Override
	protected void checktype() {
		if (getExt() == null) {
			setExt(FormatFactory.getAssociatedExtension(getFile().getAbsolutePath()));
		}

		super.checktype();
	}

	@Override
	public String getSystemName() {
		return ProcessUtil.getShortFileNameIfWideChars(getFile().getAbsolutePath());
	}

	@Override
	public void resolve() {
		File file = getFile();
		if (file.isFile() && file.exists() && (getMedia() == null || !getMedia().isMediaparsed())) {
			boolean found = false;
			InputFile input = new InputFile();
			input.setFile(file);
			String fileName = file.getAbsolutePath();
			if (getSplitTrack() > 0) {
				fileName += "#SplitTrack" + getSplitTrack();
			}
			
			if (PMS.getConfiguration().getUseCache()) {
				DLNAMediaDatabase database = PMS.get().getDatabase();

				if (database != null) {
					ArrayList<DLNAMediaInfo> medias = database.getData(fileName, file.lastModified());

					if (medias != null && medias.size() == 1) {
						setMedia(medias.get(0));
						getMedia().finalize(getType(), input);
						found = true;
					}
				}
			}

			if (!found) {
				if (getMedia() == null) {
					setMedia(new DLNAMediaInfo());
				}
				found = !getMedia().isMediaparsed() && !getMedia().isParsing();
				if (getExt() != null) {
					getExt().parse(getMedia(), input, getType(), getParent().getDefaultRenderer());
				} else //don't think that will ever happen
				{
					getMedia().parse(input, getExt(), getType(), false);
				}
				if (found && PMS.getConfiguration().getUseCache()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						database.insertData(fileName, file.lastModified(), getType(), getMedia());
					}
				}
			}
		}
		super.resolve();
	}

	@Override
	public String getThumbnailContentType() {
		return super.getThumbnailContentType();
	}

	@Override
	public InputStream getThumbnailInputStream() throws IOException {
		File file = getFile();
		File cachedThumbnail = null;
		if (getParent() != null && getParent() instanceof RealFile) {
			cachedThumbnail = ((RealFile) getParent()).getPotentialCover();
			File thumbFolder = null;
			boolean alternativeCheck = false;
			while (cachedThumbnail == null) {
				if (thumbFolder == null && getType() != Format.IMAGE) {
					thumbFolder = file.getParentFile();
				}
				cachedThumbnail = FileUtil.getFileNameWitNewExtension(thumbFolder, file, "jpg");
				if (cachedThumbnail == null) {
					cachedThumbnail = FileUtil.getFileNameWitNewExtension(thumbFolder, file, "png");
				}
				if (cachedThumbnail == null) {
					cachedThumbnail = FileUtil.getFileNameWitAddedExtension(thumbFolder, file, ".cover.jpg");
				}
				if (cachedThumbnail == null) {
					cachedThumbnail = FileUtil.getFileNameWitAddedExtension(thumbFolder, file, ".cover.png");
				}
				if (alternativeCheck) {
					break;
				}
				if (StringUtils.isNotBlank(PMS.getConfiguration().getAlternateThumbFolder())) {
					thumbFolder = new File(PMS.getConfiguration().getAlternateThumbFolder());
					if (!thumbFolder.exists() || !thumbFolder.isDirectory()) {
						thumbFolder = null;
						break;
					}
				}
				alternativeCheck = true;
			}
			if (file.isDirectory()) {
				cachedThumbnail = FileUtil.getFileNameWitNewExtension(file.getParentFile(), file, "/folder.jpg");
				if (cachedThumbnail == null) {
					cachedThumbnail = FileUtil.getFileNameWitNewExtension(file.getParentFile(), file, "/folder.png");
				}
			}

		}
		boolean hasAlreadyEmbeddedCoverArt = getType() == Format.AUDIO && getMedia() != null && getMedia().getThumb() != null;
		if (cachedThumbnail != null && (!hasAlreadyEmbeddedCoverArt || file.isDirectory())) {
			return new FileInputStream(cachedThumbnail);
		} else if (getMedia() != null && getMedia().getThumb() != null) {
			return getMedia().getThumbnailInputStream();
		} else {
			return super.getThumbnailInputStream();
		}
	}

	@Override
	public void checkThumbnail() {
		InputFile input = new InputFile();
		input.setFile(getFile());
		checkThumbnail(input);
	}

	@Override
	protected String getThumbnailURL() {
		if (getType() == Format.IMAGE && !PMS.getConfiguration().getImageThumbnailsEnabled())
		{
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(PMS.get().getServer().getURL());
		sb.append("/");
		if (getMedia() != null && getMedia().getThumb() != null) {
			return super.getThumbnailURL();
		} else if (getType() == Format.AUDIO) {
			if (getParent() != null && getParent() instanceof RealFile && ((RealFile) getParent()).getPotentialCover() != null) {
				return super.getThumbnailURL();
			}
			return null;
		}
		return super.getThumbnailURL();
	}
	
	public boolean isSubSelectable() {
		return true;
	}
	
	public String getImdbId() {
		return ImdbUtil.extractImdb(getFile());
	}
	
	private File fixPath(String path,String name) {
		if(path==null||StringUtils.isEmpty(path))
			return new File(name);
		return new File(path+File.separator+name);
	}
	
	private void resolveImdb() {
		if(!PMS.getConfiguration().autoImdb())
			return;
		if(!StringUtils.isEmpty(getImdbId())) // alreday got an imdbid or we should'n do this
			return;
		final File f=getFile();
		if(f.isDirectory())
			return;
		Runnable r=new Runnable() {
			public void run() {
				String imdb;
				String hash;
				try {
					hash = OpenSubtitle.getHash(f);
					imdb = OpenSubtitle.fetchImdbId(hash);
				} catch (IOException e) {
					LOGGER.debug("error during opensubs communication "+e);
					return;
				}
				if(StringUtils.isEmpty(imdb)||
				   StringUtils.isEmpty(hash)) // no id found, give up
					return;
				// find the . (if any)
				String name=f.getName();
				int pos=name.lastIndexOf(".");
				String rear;
				if(pos == -1) {
					pos=name.length();
					rear="";
				}
				else
					rear=name.substring(pos);
				// Some string trix
				String front=name.substring(0,pos);
				name=front+"_imdb"+imdb+"_"+"_os"+hash+"_"+rear;
				File dst=fixPath(f.getParent(),name);
				LOGGER.debug((f.renameTo(dst)?"Succeded ":"Failed ")+
						"to add imdb "+imdb+" to file "+name);
			}
		};
		new Thread(r).start();
	}
	
}
