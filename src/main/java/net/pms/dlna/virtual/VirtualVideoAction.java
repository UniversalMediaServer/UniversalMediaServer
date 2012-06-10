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
package net.pms.dlna.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.FormatFactory;
import net.pms.network.HTTPResource;

/**
 * Implements a container that when browsed, an action will be performed.
 * The class assumes that the action to be performed is to toggle a boolean value.
 * Because of this, the thumbnail is either a green tick mark or a red cross. Equivalent
 * videos are shown after the value is toggled.<p> 
 * However this is just cosmetic. Any action can be performed.
 */
public abstract class VirtualVideoAction extends DLNAResource {
	private boolean enabled;
	protected String name;
	private String thumbnailIconOK;
	private String thumbnailIconKO;
	private String thumbnailContentType;
	private String videoOk;
	private String videoKo;
	private long timer1;

	/**Constructor for this class. Recommended instantation includes overriding the {@link #enable()} function (example shown in the link).
	 * @param name Name that is shown via the UPNP ContentBrowser service. This field cannot be changed after the instantiation.
	 * @param enabled If true, a green tick mark is shown as thumbnail. If false, a red cross is shown. This initial value
	 * is usually changed via the {@link #enable()} function.
	 */
	public VirtualVideoAction(String name, boolean enabled) {
		this.name = name;
		thumbnailContentType = HTTPResource.PNG_TYPEMIME;
		thumbnailIconOK = "images/apply-256.png";
		thumbnailIconKO = "images/button_cancel-256.png";
		this.videoOk = "videos/action_success-512.mpg";
		this.videoKo = "videos/button_cancel-512.mpg";
		timer1 = -1;
		this.enabled = enabled;

		// Create correct mediaInfo for the embedded .mpg videos
		// This is needed by Format.isCompatible()
		DLNAMediaInfo mediaInfo = new DLNAMediaInfo();
		mediaInfo.setContainer("mpegps");
		ArrayList<DLNAMediaAudio> audioCodes = new ArrayList<DLNAMediaAudio>();
		mediaInfo.setAudioCodes(audioCodes);
		mediaInfo.setMimeType("video/mpeg");
		mediaInfo.setCodecV("mpeg2");
		mediaInfo.setMediaparsed(true);
		
		setMedia(mediaInfo);
	}

	/**
	 * Returns <code>false</code> because this virtual video action should not
	 * appear in the transcode folder.
	 *
	 * @return Always returns <code>false</code>
	 */
	@Override
	public boolean isTranscodeFolderAvailable() {
		return false;
	}

	/**This function is called as an action from the UPNP client when
	 * the user tries to play this item. This function calls instead the enable()
	 * function in order to execute an action.
	 * As the client expects to play an item, a really short video (less than 1s) is shown with 
	 * the results of the action. 
	 * @see #enable()
	 * @see net.pms.dlna.DLNAResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (timer1 == -1) {
			timer1 = System.currentTimeMillis();
		} else if (System.currentTimeMillis() - timer1 < 2000) {
			timer1 = -1;
		}
		if (timer1 != -1) {
			enabled = enable();
		}
		return getResourceInputStream(enabled ? videoOk : videoKo);
	}

	/**Prototype. This function is called by {@link #getInputStream()} and is the core of this class.
	 * The main purpose of this function is toggle a boolean variable somewhere. 
	 * The value of that boolean variable is shown then as either a green tick mark or a red cross.
	 * However, this is just a cosmetic thing. Any Java code can be executed in this function, not only toggling a boolean variable.
	 * Recommended way to instantiate this class is as follows:
	 * <pre> VirtualFolder vf;
	 * [...]
	 * vf.addChild(new VirtualVideoAction(Messages.getString("PMS.3"), configuration.isMencoderNoOutOfSync()) {
	 *   public boolean enable() {
	 *   configuration.setMencoderNoOutOfSync(!configuration.isMencoderNoOutOfSync());
	 *   return configuration.isMencoderNoOutOfSync();
	 *   }
	 * }); </pre>
	 * @return If true, a green tick mark is shown as thumbnail. If false, a red cross is shown.
	 */
	public abstract boolean enable();

	@Override
	public String getName() {
		return name;
	}

	/**As this item is not a container, returns false.
	 * @return false
	 * @see net.pms.dlna.DLNAResource#isFolder()
	 */
	@Override
	public boolean isFolder() {
		return false;
	}

	/**Returns an invalid length as this item is not 
	 * TODO: (botijo) VirtualFolder returns 0 instead of -1.
	 * @return -1, an invalid length for an item.
	 * @see net.pms.dlna.DLNAResource#length()
	 */
	@Override
	public long length() {
		return -1; //DLNAMediaInfo.TRANS_SIZE;
	}

	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	/**Returns either a green tick mark or a red cross that represents the actual
	 * value of this item
	 * @see net.pms.dlna.DLNAResource#getThumbnailInputStream()
	 */
	@Override
	public InputStream getThumbnailInputStream() {
		return getResourceInputStream(enabled ? thumbnailIconOK : thumbnailIconKO);
	}

	/**@return PNG type, as the thumbnail can only be either a green tick mark or a red cross.
	 * @see #getThumbnailInputStream()
	 * @see net.pms.dlna.DLNAResource#getThumbnailContentType()
	 */
	@Override
	public String getThumbnailContentType() {
		return thumbnailContentType;
	}

	/**TODO: (botijo) Why is ext being set here?
	 * @return True, as this kind of item is always valid.
	 * @see net.pms.dlna.DLNAResource#isValid()
	 */
	@Override
	public boolean isValid() {
		setExt(FormatFactory.getAssociatedExtension("toto.mpg"));
		return true;
	}
}
